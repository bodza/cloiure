package graalvm.compiler.truffle.runtime;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleAssumptions;
import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleStackTraceLimit;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

import graalvm.compiler.debug.TTY;
import graalvm.compiler.truffle.common.OptimizedAssumptionDependency;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.impl.AbstractAssumption;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import jdk.vm.ci.meta.JavaKind.FormatWithToString;

/**
 * An assumption that when {@linkplain #invalidate() invalidated} will cause all
 * {@linkplain #registerDependency() registered} dependencies to be invalidated.
 */
public final class OptimizedAssumption extends AbstractAssumption implements FormatWithToString {
    /**
     * Reference to machine code that is dependent on an assumption.
     */
    static class Entry implements Consumer<OptimizedAssumptionDependency> {
        /**
         * A machine code reference that must be kept reachable as long as the machine code itself
         * is valid.
         */
        OptimizedAssumptionDependency dependency;

        /**
         * Machine code that is guaranteed to be invalid once the
         * {@link OptimizedAssumptionDependency} object becomes unreachable.
         */
        WeakReference<OptimizedAssumptionDependency> weakDependency;

        Entry next;

        @Override
        public synchronized void accept(OptimizedAssumptionDependency dep) {
            if (dep == null || dep.reachabilityDeterminesValidity()) {
                this.weakDependency = new WeakReference<>(dep);
            } else {
                this.dependency = dep;
            }
            this.notifyAll();
        }

        synchronized OptimizedAssumptionDependency awaitDependency() {
            while (dependency == null && weakDependency == null) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (dependency != null) {
                return dependency;
            }
            return weakDependency.get();
        }

        synchronized boolean isValid() {
            if (dependency != null) {
                return dependency.isValid();
            }
            if (weakDependency != null) {
                OptimizedAssumptionDependency dep = weakDependency.get();
                return dep != null && dep.isValid();
            }
            // A pending dependency is treated as valid
            return true;
        }

        @Override
        public synchronized String toString() {
            if (dependency != null) {
                return String.format("%x[%s]", hashCode(), dependency);
            }
            if (weakDependency != null) {
                OptimizedAssumptionDependency dep = weakDependency.get();
                return String.format("%x[%s]", hashCode(), dep);
            }
            return String.format("%x", hashCode());
        }
    }

    /**
     * Linked list of registered dependencies.
     */
    private Entry dependencies;

    /**
     * Number of entries in {@link #dependencies}.
     */
    private int size;

    /**
     * Number of entries in {@link #dependencies} after most recent call to
     * {@link #removeInvalidEntries()}.
     */
    private int sizeAfterLastRemove;

    public OptimizedAssumption(String name) {
        super(name);
    }

    @Override
    public void check() throws InvalidAssumptionException {
        if (!this.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new InvalidAssumptionException();
        }
    }

    @Override
    public void invalidate() {
        if (isValid) {
            invalidateImpl("");
        }
    }

    @Override
    public void invalidate(String message) {
        if (isValid) {
            invalidateImpl(message);
        }
    }

    @TruffleBoundary
    private synchronized void invalidateImpl(String message) {
        /*
         * Check again, now that we are holding the lock. Since isValid is defined volatile,
         * double-checked locking is allowed.
         */
        if (!isValid) {
            return;
        }

        boolean invalidatedADependency = false;
        Entry e = dependencies;
        while (e != null) {
            OptimizedAssumptionDependency dependency = e.awaitDependency();
            if (dependency != null) {
                OptimizedCallTarget callTarget = invalidateWithReason(dependency, "assumption invalidated");
                invalidatedADependency = true;
                if (TruffleCompilerOptions.getValue(TraceTruffleAssumptions)) {
                    logInvalidatedDependency(dependency, message);
                }
                if (callTarget != null) {
                    callTarget.getCompilationProfile().reportInvalidated();
                }
            }
            e = e.next;
        }
        dependencies = null;
        size = 0;
        sizeAfterLastRemove = 0;
        isValid = false;

        if (TruffleCompilerOptions.getValue(TraceTruffleAssumptions)) {
            if (invalidatedADependency) {
                logStackTrace();
            }
        }
    }

    private void removeInvalidEntries() {
        Entry last = null;
        Entry e = dependencies;
        dependencies = null;
        while (e != null) {
            if (e.isValid()) {
                if (last == null) {
                    dependencies = e;
                } else {
                    last.next = e;
                }
                last = e;
            } else {
                size--;
            }
            e = e.next;
        }
        if (last != null) {
            last.next = null;
        }
        sizeAfterLastRemove = size;
    }

    /**
     * Removes all {@linkplain OptimizedAssumptionDependency#isValid() invalid} dependencies.
     */
    public synchronized void removeInvalidDependencies() {
        removeInvalidEntries();
    }

    /**
     * Gets the number of dependencies registered with this assumption.
     */
    public synchronized int countDependencies() {
        return size;
    }

    /**
     * Registers some dependent code with this assumption.
     *
     * As the dependent code may not yet be available, a {@link Consumer} is returned that must be
     * {@linkplain Consumer#accept(Object) notified} when the code becomes available. If there is an
     * error while compiling or installing the code, the returned consumer must be called with a
     * {@code null} argument.
     *
     * If this assumption is already invalid, then {@code null} is returned in which case the caller
     * (e.g., the compiler) must ensure the dependent code is never executed.
     */
    public synchronized Consumer<OptimizedAssumptionDependency> registerDependency() {
        if (isValid) {
            if (size >= 2 * sizeAfterLastRemove) {
                removeInvalidEntries();
            }
            Entry e = new Entry();
            e.next = dependencies;
            dependencies = e;
            size++;
            return e;
        } else {
            return null;
        }
    }

    private OptimizedCallTarget invalidateWithReason(OptimizedAssumptionDependency dependency, String reason) {
        if (dependency.getCompilable() != null) {
            OptimizedCallTarget callTarget = (OptimizedCallTarget) dependency.getCompilable();
            callTarget.invalidate(this, reason);
            return callTarget;
        } else {
            dependency.invalidate();
            return null;
        }
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    private void logInvalidatedDependency(OptimizedAssumptionDependency dependency, String message) {
        if (message != null && message.length() > 0) {
            TTY.out().out().printf("assumption '%s' invalidated installed code '%s' with message '%s'\n", name, dependency, message);
        } else {
            TTY.out().out().printf("assumption '%s' invalidated installed code '%s'\n", name, dependency);
        }
    }

    private static void logStackTrace() {
        final int skip = 1;
        final int limit = TruffleCompilerOptions.getValue(TraceTruffleStackTraceLimit);
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        StringBuilder strb = new StringBuilder();
        String sep = "";
        for (int i = skip; i < stackTrace.length && i < skip + limit; i++) {
            strb.append(sep).append("  ").append(stackTrace[i].toString());
            sep = "\n";
        }
        if (stackTrace.length > skip + limit) {
            strb.append("\n    ...");
        }

        TTY.out().out().println(strb);
    }
}
