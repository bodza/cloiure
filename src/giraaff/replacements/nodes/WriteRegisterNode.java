package giraaff.replacements.nodes;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Changes the value of a specific register.
 */
// @class WriteRegisterNode
public final class WriteRegisterNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<WriteRegisterNode> TYPE = NodeClass.create(WriteRegisterNode.class);

    /**
     * The fixed register to access.
     */
    // @field
    protected final Register register;

    /**
     * The new value assigned to the register.
     */
    @Input
    // @field
    ValueNode value;

    // @cons
    public WriteRegisterNode(Register __register, ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.register = __register;
        this.value = __value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __val = __gen.operand(value);
        __gen.getLIRGeneratorTool().emitMove(register.asValue(__val.getValueKind()), __val);
    }
}
