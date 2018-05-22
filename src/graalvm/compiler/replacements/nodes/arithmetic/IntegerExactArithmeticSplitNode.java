package graalvm.compiler.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.BeginNode;
import graalvm.compiler.nodes.ControlSplitNode;
import graalvm.compiler.nodes.DeoptimizeNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public abstract class IntegerExactArithmeticSplitNode extends ControlSplitNode implements Simplifiable, LIRLowerable
{
    public static final NodeClass<IntegerExactArithmeticSplitNode> TYPE = NodeClass.create(IntegerExactArithmeticSplitNode.class);

    @Successor AbstractBeginNode next;
    @Successor AbstractBeginNode overflowSuccessor;
    @Input ValueNode x;
    @Input ValueNode y;

    protected IntegerExactArithmeticSplitNode(NodeClass<? extends IntegerExactArithmeticSplitNode> c, Stamp stamp, ValueNode x, ValueNode y, AbstractBeginNode next, AbstractBeginNode overflowSuccessor)
    {
        super(c, stamp);
        this.x = x;
        this.y = y;
        this.overflowSuccessor = overflowSuccessor;
        this.next = next;
    }

    @Override
    public AbstractBeginNode getPrimarySuccessor()
    {
        return next;
    }

    @Override
    public double probability(AbstractBeginNode successor)
    {
        return successor == next ? 1 : 0;
    }

    @Override
    public boolean setProbability(AbstractBeginNode successor, double value)
    {
        // Successor probabilities for arithmetic split nodes are fixed.
        return false;
    }

    public AbstractBeginNode getNext()
    {
        return next;
    }

    public AbstractBeginNode getOverflowSuccessor()
    {
        return overflowSuccessor;
    }

    public ValueNode getX()
    {
        return x;
    }

    public ValueNode getY()
    {
        return y;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        generator.setResult(this, generateArithmetic(generator));
        generator.emitOverflowCheckBranch(getOverflowSuccessor(), getNext(), stamp, probability(getOverflowSuccessor()));
    }

    protected abstract Value generateArithmetic(NodeLIRBuilderTool generator);

    static void lower(LoweringTool tool, IntegerExactArithmeticNode node)
    {
        if (node.asNode().graph().getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            FloatingNode floatingNode = (FloatingNode) node;
            FixedWithNextNode previous = tool.lastFixedNode();
            FixedNode next = previous.next();
            previous.setNext(null);
            DeoptimizeNode deopt = floatingNode.graph().add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.ArithmeticException));
            AbstractBeginNode normalBegin = floatingNode.graph().add(new BeginNode());
            normalBegin.setNext(next);
            IntegerExactArithmeticSplitNode split = node.createSplit(normalBegin, BeginNode.begin(deopt));
            previous.setNext(split);
            floatingNode.replaceAndDelete(split);
        }
    }

    @Override
    public int getSuccessorCount()
    {
        return 2;
    }
}
