package giraaff.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class IntegerExactArithmeticSplitNode
public abstract class IntegerExactArithmeticSplitNode extends ControlSplitNode implements Simplifiable, LIRLowerable
{
    // @def
    public static final NodeClass<IntegerExactArithmeticSplitNode> TYPE = NodeClass.create(IntegerExactArithmeticSplitNode.class);

    @Successor
    // @field
    AbstractBeginNode ___next;
    @Successor
    // @field
    AbstractBeginNode ___overflowSuccessor;
    @Input
    // @field
    ValueNode ___x;
    @Input
    // @field
    ValueNode ___y;

    // @cons
    protected IntegerExactArithmeticSplitNode(NodeClass<? extends IntegerExactArithmeticSplitNode> __c, Stamp __stamp, ValueNode __x, ValueNode __y, AbstractBeginNode __next, AbstractBeginNode __overflowSuccessor)
    {
        super(__c, __stamp);
        this.___x = __x;
        this.___y = __y;
        this.___overflowSuccessor = __overflowSuccessor;
        this.___next = __next;
    }

    @Override
    public AbstractBeginNode getPrimarySuccessor()
    {
        return this.___next;
    }

    @Override
    public double probability(AbstractBeginNode __successor)
    {
        return __successor == this.___next ? 1 : 0;
    }

    @Override
    public boolean setProbability(AbstractBeginNode __successor, double __value)
    {
        // Successor probabilities for arithmetic split nodes are fixed.
        return false;
    }

    public AbstractBeginNode getNext()
    {
        return this.___next;
    }

    public AbstractBeginNode getOverflowSuccessor()
    {
        return this.___overflowSuccessor;
    }

    public ValueNode getX()
    {
        return this.___x;
    }

    public ValueNode getY()
    {
        return this.___y;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, generateArithmetic(__gen));
        __gen.emitOverflowCheckBranch(getOverflowSuccessor(), getNext(), this.___stamp, probability(getOverflowSuccessor()));
    }

    protected abstract Value generateArithmetic(NodeLIRBuilderTool __gen);

    static void lower(LoweringTool __tool, IntegerExactArithmeticNode __node)
    {
        if (__node.asNode().graph().getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            FloatingNode __floatingNode = (FloatingNode) __node;
            FixedWithNextNode __previous = __tool.lastFixedNode();
            FixedNode __next = __previous.next();
            __previous.setNext(null);
            DeoptimizeNode __deopt = __floatingNode.graph().add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.ArithmeticException));
            AbstractBeginNode __normalBegin = __floatingNode.graph().add(new BeginNode());
            __normalBegin.setNext(__next);
            IntegerExactArithmeticSplitNode __split = __node.createSplit(__normalBegin, BeginNode.begin(__deopt));
            __previous.setNext(__split);
            __floatingNode.replaceAndDelete(__split);
        }
    }

    @Override
    public int getSuccessorCount()
    {
        return 2;
    }
}
