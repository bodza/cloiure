package giraaff.core.gen;

import jdk.vm.ci.meta.Value;

import giraaff.graph.Node;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LabelRef;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.AndNode;
import giraaff.nodes.calc.FloatConvertNode;
import giraaff.nodes.calc.FloatEqualsNode;
import giraaff.nodes.calc.FloatLessThanNode;
import giraaff.nodes.calc.IntegerBelowNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.IntegerLessThanNode;
import giraaff.nodes.calc.IntegerTestNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.calc.MulNode;
import giraaff.nodes.calc.NarrowNode;
import giraaff.nodes.calc.ObjectEqualsNode;
import giraaff.nodes.calc.OrNode;
import giraaff.nodes.calc.PointerEqualsNode;
import giraaff.nodes.calc.ReinterpretNode;
import giraaff.nodes.calc.SignExtendNode;
import giraaff.nodes.calc.SubNode;
import giraaff.nodes.calc.UnsignedRightShiftNode;
import giraaff.nodes.calc.XorNode;
import giraaff.nodes.calc.ZeroExtendNode;
import giraaff.nodes.java.LogicCompareAndSwapNode;
import giraaff.nodes.java.ValueCompareAndSwapNode;
import giraaff.nodes.memory.FloatingReadNode;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.WriteNode;

// MatchableNode nodeClass = ConstantNode.class, shareable = true
// MatchableNode nodeClass = FloatConvertNode.class, inputs = { "value" }
// MatchableNode nodeClass = FloatingReadNode.class, inputs = { "address" }
// MatchableNode nodeClass = IfNode.class, inputs = { "condition" }
// MatchableNode nodeClass = SubNode.class, inputs = { "x", "y" }
// MatchableNode nodeClass = LeftShiftNode.class, inputs = { "x", "y" }
// MatchableNode nodeClass = NarrowNode.class, inputs = { "value" }
// MatchableNode nodeClass = ReadNode.class, inputs = { "address" }
// MatchableNode nodeClass = ReinterpretNode.class, inputs = { "value" }
// MatchableNode nodeClass = SignExtendNode.class, inputs = { "value" }
// MatchableNode nodeClass = UnsignedRightShiftNode.class, inputs = { "x", "y" }
// MatchableNode nodeClass = WriteNode.class, inputs = { "address", "value" }
// MatchableNode nodeClass = ZeroExtendNode.class, inputs = { "value" }
// MatchableNode nodeClass = AndNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = FloatEqualsNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = FloatLessThanNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = PointerEqualsNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = AddNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = IntegerBelowNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = IntegerEqualsNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = IntegerLessThanNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = MulNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = IntegerTestNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = ObjectEqualsNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = OrNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = XorNode.class, inputs = { "x", "y" }, commutative = true
// MatchableNode nodeClass = PiNode.class, inputs = { "object" }
// MatchableNode nodeClass = LogicCompareAndSwapNode.class, inputs = { "address", "expectedValue", "newValue" }
// MatchableNode nodeClass = ValueCompareAndSwapNode.class, inputs = { "address", "expectedValue", "newValue" }
public abstract class NodeMatchRules
{
    NodeLIRBuilder lirBuilder;
    protected final LIRGeneratorTool gen;

    protected NodeMatchRules(LIRGeneratorTool gen)
    {
        this.gen = gen;
    }

    protected LIRGeneratorTool getLIRGeneratorTool()
    {
        return gen;
    }

    /*
     * For now we do not want to expose the full lirBuilder to subclasses, so we delegate the few
     * methods that are actually needed. If the list grows too long, exposing lirBuilder might be
     * the better approach.
     */

    protected final Value operand(Node node)
    {
        return lirBuilder.operand(node);
    }

    protected final LIRFrameState state(DeoptimizingNode deopt)
    {
        return lirBuilder.state(deopt);
    }

    protected final LabelRef getLIRBlock(FixedNode b)
    {
        return lirBuilder.getLIRBlock(b);
    }

    protected final void append(LIRInstruction op)
    {
        lirBuilder.append(op);
    }
}
