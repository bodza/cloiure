package graalvm.compiler.core.gen;

import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.match.MatchableNode;
import graalvm.compiler.graph.Node;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LabelRef;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.DeoptimizingNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.calc.AddNode;
import graalvm.compiler.nodes.calc.AndNode;
import graalvm.compiler.nodes.calc.FloatConvertNode;
import graalvm.compiler.nodes.calc.FloatEqualsNode;
import graalvm.compiler.nodes.calc.FloatLessThanNode;
import graalvm.compiler.nodes.calc.IntegerBelowNode;
import graalvm.compiler.nodes.calc.IntegerEqualsNode;
import graalvm.compiler.nodes.calc.IntegerLessThanNode;
import graalvm.compiler.nodes.calc.IntegerTestNode;
import graalvm.compiler.nodes.calc.LeftShiftNode;
import graalvm.compiler.nodes.calc.MulNode;
import graalvm.compiler.nodes.calc.NarrowNode;
import graalvm.compiler.nodes.calc.ObjectEqualsNode;
import graalvm.compiler.nodes.calc.OrNode;
import graalvm.compiler.nodes.calc.PointerEqualsNode;
import graalvm.compiler.nodes.calc.ReinterpretNode;
import graalvm.compiler.nodes.calc.SignExtendNode;
import graalvm.compiler.nodes.calc.SubNode;
import graalvm.compiler.nodes.calc.UnsignedRightShiftNode;
import graalvm.compiler.nodes.calc.XorNode;
import graalvm.compiler.nodes.calc.ZeroExtendNode;
import graalvm.compiler.nodes.java.LogicCompareAndSwapNode;
import graalvm.compiler.nodes.java.ValueCompareAndSwapNode;
import graalvm.compiler.nodes.memory.FloatingReadNode;
import graalvm.compiler.nodes.memory.ReadNode;
import graalvm.compiler.nodes.memory.WriteNode;

@MatchableNode(nodeClass = ConstantNode.class, shareable = true)
@MatchableNode(nodeClass = FloatConvertNode.class, inputs = {"value"})
@MatchableNode(nodeClass = FloatingReadNode.class, inputs = {"address"})
@MatchableNode(nodeClass = IfNode.class, inputs = {"condition"})
@MatchableNode(nodeClass = SubNode.class, inputs = {"x", "y"})
@MatchableNode(nodeClass = LeftShiftNode.class, inputs = {"x", "y"})
@MatchableNode(nodeClass = NarrowNode.class, inputs = {"value"})
@MatchableNode(nodeClass = ReadNode.class, inputs = {"address"})
@MatchableNode(nodeClass = ReinterpretNode.class, inputs = {"value"})
@MatchableNode(nodeClass = SignExtendNode.class, inputs = {"value"})
@MatchableNode(nodeClass = UnsignedRightShiftNode.class, inputs = {"x", "y"})
@MatchableNode(nodeClass = WriteNode.class, inputs = {"address", "value"})
@MatchableNode(nodeClass = ZeroExtendNode.class, inputs = {"value"})
@MatchableNode(nodeClass = AndNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = FloatEqualsNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = FloatLessThanNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = PointerEqualsNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = AddNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = IntegerBelowNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = IntegerEqualsNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = IntegerLessThanNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = MulNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = IntegerTestNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = ObjectEqualsNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = OrNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = XorNode.class, inputs = {"x", "y"}, commutative = true)
@MatchableNode(nodeClass = PiNode.class, inputs = {"object"})
@MatchableNode(nodeClass = LogicCompareAndSwapNode.class, inputs = {"address", "expectedValue", "newValue"})
@MatchableNode(nodeClass = ValueCompareAndSwapNode.class, inputs = {"address", "expectedValue", "newValue"})
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
