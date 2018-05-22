package giraaff.replacements.nodes;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.StampFactory;
import giraaff.debug.GraalError;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Assertion nodes will go away as soon as the value evaluates to true. Compile-time assertions will
 * fail if this has not happened by the time the node is lowered to LIR, while runtime assertions
 * may need to insert a check.
 */
public final class AssertionNode extends FixedWithNextNode implements Lowerable, Canonicalizable, LIRLowerable
{
    public static final NodeClass<AssertionNode> TYPE = NodeClass.create(AssertionNode.class);
    @Input ValueNode condition;

    protected final boolean compileTimeAssertion;
    protected final String message;

    public AssertionNode(boolean compileTimeAssertion, ValueNode condition, String message)
    {
        super(TYPE, StampFactory.forVoid());
        this.condition = condition;
        this.compileTimeAssertion = compileTimeAssertion;
        this.message = message;
    }

    public ValueNode condition()
    {
        return condition;
    }

    public String message()
    {
        return message;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (condition.isConstant() && condition.asJavaConstant().asInt() != 0)
        {
            return null;
        }
        /*
         * Assertions with a constant "false" value do not immediately cause an error, since they
         * may be unreachable and could thus be removed by later optimizations.
         */
        return this;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        if (!compileTimeAssertion)
        {
            tool.getLowerer().lower(this, tool);
        }
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        if (condition.isConstant())
        {
            if (condition.asJavaConstant().asInt() == 0)
            {
                throw new GraalError("%s: failed compile-time assertion: %s", this, message);
            }
        }
        else
        {
            throw new GraalError("%s: failed compile-time assertion (value %s): %s", this, condition, message);
        }
    }

    @NodeIntrinsic
    public static native void assertion(@ConstantNodeParameter boolean compileTimeAssertion, boolean condition, @ConstantNodeParameter String message);
}
