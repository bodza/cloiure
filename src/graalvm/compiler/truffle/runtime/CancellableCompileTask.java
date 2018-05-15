package graalvm.compiler.truffle.runtime;

import java.util.concurrent.Future;

import graalvm.compiler.nodes.Cancellable;

public class CancellableCompileTask implements Cancellable {
    Future<?> future = null;
    boolean cancelled = false;

    // This cannot be done in the constructor because the CancellableCompileTask needs to be
    // passed down to the compiler through a Runnable inner class.
    // This means it must be final and initialized before the future can be set.
    public synchronized void setFuture(Future<?> future) {
        if (this.future == null) {
            this.future = future;
        } else {
            throw new IllegalStateException("The future should not be re-set.");
        }
    }

    public synchronized Future<?> getFuture() {
        return future;
    }

    @Override
    public synchronized boolean isCancelled() {
        assert future != null;
        assert !cancelled || future.isCancelled();
        return cancelled;
    }

    public synchronized void cancel() {
        if (!cancelled) {
            assert future != null;
            cancelled = true;
            if (future != null) {
                assert !future.isCancelled();
                // should assert future.cancel(false)=true but future might already finished between
                // the cancelled=true write and the call to cancel(false)
                future.cancel(false);
            }
        }
    }

    public boolean isRunning() {
        assert future != null;
        return !(future.isDone() || future.isCancelled());
    }
}
