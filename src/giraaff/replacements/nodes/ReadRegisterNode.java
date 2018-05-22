package giraaff.replacements.nodes;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Access the value of a specific register.
 */
public final class ReadRegisterNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<ReadRegisterNode> TYPE = NodeClass.create(ReadRegisterNode.class);
    /**
     * The fixed register to access.
     */
    protected final Register register;

    /**
     * When true, subsequent uses of this node use the fixed register; when false, the value is
     * moved into a new virtual register so that the fixed register is not seen by uses.
     */
    protected final boolean directUse;

    /**
     * When true, this node is also an implicit definition of the value for the register allocator,
     * i.e., the register is an implicit incoming value; when false, the register must be defined in
     * the same method or must be an register excluded from register allocation.
     */
    protected final boolean incoming;

    public ReadRegisterNode(Register register, JavaKind kind, boolean directUse, boolean incoming)
    {
        super(TYPE, StampFactory.forKind(kind));
        this.register = register;
        this.directUse = directUse;
        this.incoming = incoming;
    }

    public ReadRegisterNode(@InjectedNodeParameter Stamp stamp, Register register, boolean directUse, boolean incoming)
    {
        super(TYPE, stamp);
        this.register = register;
        this.directUse = directUse;
        this.incoming = incoming;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        LIRKind kind = generator.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        Value result = register.asValue(kind);
        if (incoming)
        {
            generator.getLIRGeneratorTool().emitIncomingValues(new Value[]{result});
        }
        if (!directUse)
        {
            result = generator.getLIRGeneratorTool().emitMove(result);
        }
        generator.setResult(this, result);
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
