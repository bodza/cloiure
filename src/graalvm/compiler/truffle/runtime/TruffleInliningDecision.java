package graalvm.compiler.truffle.runtime;

import static graalvm.compiler.truffle.runtime.OptimizedCallTarget.runtime;

import java.util.Iterator;
import java.util.List;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.truffle.common.TruffleInliningPlan;

import jdk.vm.ci.meta.JavaConstant;

public final class TruffleInliningDecision extends TruffleInlining implements Comparable<TruffleInliningDecision>, TruffleInliningPlan.Decision {

    private final OptimizedCallTarget target;
    private final TruffleInliningProfile profile;
    private boolean inline;

    public TruffleInliningDecision(OptimizedCallTarget target, TruffleInliningProfile profile, List<TruffleInliningDecision> children) {
        super(children);
        this.target = target;
        this.profile = profile;
    }

    @Override
    public String getTargetName() {
        return target.toString();
    }

    public OptimizedCallTarget getTarget() {
        return target;
    }

    void setInline(boolean inline) {
        this.inline = inline;
    }

    @Override
    public boolean shouldInline() {
        return inline;
    }

    public TruffleInliningProfile getProfile() {
        return profile;
    }

    @Override
    public int compareTo(TruffleInliningDecision o) {
        return Double.compare(o.getProfile().getScore(), getProfile().getScore());
    }

    public boolean isSameAs(TruffleInliningDecision other) {
        if (getTarget() != other.getTarget()) {
            return false;
        } else if (shouldInline() != other.shouldInline()) {
            return false;
        } else if (!shouldInline()) {
            assert !other.shouldInline();
            return true;
        } else {
            Iterator<TruffleInliningDecision> i1 = iterator();
            Iterator<TruffleInliningDecision> i2 = other.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                if (!i1.next().isSameAs(i2.next())) {
                    return false;
                }
            }
            return !i1.hasNext() && !i2.hasNext();
        }
    }

    @Override
    public String toString() {
        return String.format("TruffleInliningDecision(callNode=%s, inline=%b)", profile.getCallNode(), inline);
    }

    @Override
    public boolean isTargetStable() {
        return target == getProfile().getCallNode().getCurrentCallTarget();
    }

    @Override
    public JavaConstant getNodeRewritingAssumption() {
        SnippetReflectionProvider snippetReflection = runtime().getGraalRuntime().getRequiredCapability(SnippetReflectionProvider.class);
        return snippetReflection.forObject(target.getNodeRewritingAssumption());
    }
}
