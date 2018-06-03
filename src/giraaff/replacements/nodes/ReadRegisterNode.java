package giraaff.replacements.nodes;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Access the value of a specific register.
 */
// @class ReadRegisterNode
public final class ReadRegisterNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<ReadRegisterNode> TYPE = NodeClass.create(ReadRegisterNode.class);

    /**
     * The fixed register to access.
     */
    // @field
    protected final Register register;

    /**
     * When true, subsequent uses of this node use the fixed register; when false, the value is
     * moved into a new virtual register so that the fixed register is not seen by uses.
     */
    // @field
    protected final boolean directUse;

    /**
     * When true, this node is also an implicit definition of the value for the register allocator,
     * i.e., the register is an implicit incoming value; when false, the register must be defined in
     * the same method or must be an register excluded from register allocation.
     */
    // @field
    protected final boolean incoming;

    // @cons
    public ReadRegisterNode(Register __register, JavaKind __kind, boolean __directUse, boolean __incoming)
    {
        super(TYPE, StampFactory.forKind(__kind));
        this.register = __register;
        this.directUse = __directUse;
        this.incoming = __incoming;
    }

    // @cons
    public ReadRegisterNode(@InjectedNodeParameter Stamp __stamp, Register __register, boolean __directUse, boolean __incoming)
    {
        super(TYPE, __stamp);
        this.register = __register;
        this.directUse = __directUse;
        this.incoming = __incoming;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRKind __kind = __gen.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        Value __result = register.asValue(__kind);
        if (incoming)
        {
            __gen.getLIRGeneratorTool().emitIncomingValues(new Value[] { __result });
        }
        if (!directUse)
        {
            __result = __gen.getLIRGeneratorTool().emitMove(__result);
        }
        __gen.setResult(this, __result);
    }
}
