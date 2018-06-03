package giraaff.replacements.nodes;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Changes the value of a specific register.
///
// @class WriteRegisterNode
public final class WriteRegisterNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<WriteRegisterNode> TYPE = NodeClass.create(WriteRegisterNode.class);

    ///
    // The fixed register to access.
    ///
    // @field
    protected final Register ___register;

    ///
    // The new value assigned to the register.
    ///
    @Input
    // @field
    ValueNode ___value;

    // @cons
    public WriteRegisterNode(Register __register, ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.___register = __register;
        this.___value = __value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __val = __gen.operand(this.___value);
        __gen.getLIRGeneratorTool().emitMove(this.___register.asValue(__val.getValueKind()), __val);
    }
}
