package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.Value;

/**
 * Changes the value of a specific register.
 */
@NodeInfo(nameTemplate = "WriteRegister %{p#register}", cycles = CYCLES_2, size = SIZE_1)
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

    public WriteRegisterNode(Register register, ValueNode value)
    {
        super(TYPE, StampFactory.forVoid());
        this.register = register;
        this.value = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        Value val = generator.operand(value);
        generator.getLIRGeneratorTool().emitMove(register.asValue(val.getValueKind()), val);
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Name)
        {
            return super.toString(Verbosity.Name) + "%" + register;
        }
        else
        {
            return super.toString(verbosity);
        }
    }
}
