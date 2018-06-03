package giraaff.phases.common.inlining.info;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.graph.Node;
import giraaff.nodes.Invoke;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.util.Providers;

/**
 * Represents an inlining opportunity where the compiler can statically determine a monomorphic
 * target method and therefore is able to determine the called method exactly.
 */
// @class ExactInlineInfo
public class ExactInlineInfo extends AbstractInlineInfo
{
    // @field
    protected final ResolvedJavaMethod concrete;
    // @field
    private Inlineable inlineableElement;
    // @field
    private boolean suppressNullCheck;

    // @cons
    public ExactInlineInfo(Invoke __invoke, ResolvedJavaMethod __concrete)
    {
        super(__invoke);
        this.concrete = __concrete;
    }

    public void suppressNullCheck()
    {
        suppressNullCheck = true;
    }

    @Override
    public EconomicSet<Node> inline(Providers __providers, String __reason)
    {
        return inline(invoke, concrete, inlineableElement, !suppressNullCheck, __reason);
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers __providers)
    {
        // nothing todo, can already be bound statically
    }

    @Override
    public int numberOfMethods()
    {
        return 1;
    }

    @Override
    public ResolvedJavaMethod methodAt(int __index)
    {
        return concrete;
    }

    @Override
    public double probabilityAt(int __index)
    {
        return 1.0;
    }

    @Override
    public double relevanceAt(int __index)
    {
        return 1.0;
    }

    @Override
    public Inlineable inlineableElementAt(int __index)
    {
        return inlineableElement;
    }

    @Override
    public void setInlinableElement(int __index, Inlineable __inlineableElement)
    {
        this.inlineableElement = __inlineableElement;
    }

    @Override
    public boolean shouldInline()
    {
        return concrete.shouldBeInlined();
    }
}
