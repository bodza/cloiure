package graalvm.compiler.phases.common.inlining.info;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.phases.common.inlining.info.elem.Inlineable;
import graalvm.compiler.phases.util.Providers;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Represents an inlining opportunity where the compiler can statically determine a monomorphic
 * target method and therefore is able to determine the called method exactly.
 */
public class ExactInlineInfo extends AbstractInlineInfo
{
    protected final ResolvedJavaMethod concrete;
    private Inlineable inlineableElement;
    private boolean suppressNullCheck;

    public ExactInlineInfo(Invoke invoke, ResolvedJavaMethod concrete)
    {
        super(invoke);
        this.concrete = concrete;
    }

    public void suppressNullCheck()
    {
        suppressNullCheck = true;
    }

    @Override
    public EconomicSet<Node> inline(Providers providers, String reason)
    {
        return inline(invoke, concrete, inlineableElement, !suppressNullCheck, reason);
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers providers)
    {
        // nothing todo, can already be bound statically
    }

    @Override
    public int numberOfMethods()
    {
        return 1;
    }

    @Override
    public ResolvedJavaMethod methodAt(int index)
    {
        return concrete;
    }

    @Override
    public double probabilityAt(int index)
    {
        return 1.0;
    }

    @Override
    public double relevanceAt(int index)
    {
        return 1.0;
    }

    @Override
    public String toString()
    {
        return "exact " + concrete.format("%H.%n(%p):%r");
    }

    @Override
    public Inlineable inlineableElementAt(int index)
    {
        return inlineableElement;
    }

    @Override
    public void setInlinableElement(int index, Inlineable inlineableElement)
    {
        this.inlineableElement = inlineableElement;
    }

    @Override
    public boolean shouldInline()
    {
        return concrete.shouldBeInlined();
    }
}
