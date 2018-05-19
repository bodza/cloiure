package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_UNKNOWN;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Assertion nodes will go away as soon as the value evaluates to true. Compile-time assertions will
 * fail if this has not happened by the time the node is lowered to LIR, while runtime assertions
 * may need to insert a check.
 */
@NodeInfo(cycles = CYCLES_UNKNOWN, size = SIZE_UNKNOWN)
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
            if (GraalOptions.ImmutableCode.getValue(getOptions()))
            {
                // Snippet assertions are disabled for AOT
                graph().removeFixed(this);
            }
            else
            {
                tool.getLowerer().lower(this, tool);
            }
        }
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        if (GraalOptions.ImmutableCode.getValue(getOptions()))
        {
            // Snippet assertions are disabled for AOT
            return;
        }
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
