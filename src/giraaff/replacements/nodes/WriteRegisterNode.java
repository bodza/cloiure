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
    public static final NodeClass<WriteRegisterNode> TYPE = NodeClass.create(WriteRegisterNode.class);

    /**
     * The fixed register to access.
     */
    protected final Register register;

    /**
     * The new value assigned to the register.
     */
    @Input ValueNode value;

    // @cons
    public WriteRegisterNode(Register register, ValueNode value)
    {
        super(TYPE, StampFactory.forVoid());
        this.register = register;
        this.value = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value val = gen.operand(value);
        gen.getLIRGeneratorTool().emitMove(register.asValue(val.getValueKind()), val);
    }
}
