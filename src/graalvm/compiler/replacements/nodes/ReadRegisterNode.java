package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

/**
 * Access the value of a specific register.
 */
@NodeInfo(nameTemplate = "ReadRegister %{p#register}", cycles = CYCLES_0, size = SIZE_0)
public final class ReadRegisterNode extends FixedWithNextNode implements LIRLowerable {

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

    public ReadRegisterNode(Register register, JavaKind kind, boolean directUse, boolean incoming) {
        super(TYPE, StampFactory.forKind(kind));
        assert register != null;
        this.register = register;
        this.directUse = directUse;
        this.incoming = incoming;
    }

    public ReadRegisterNode(@InjectedNodeParameter Stamp stamp, Register register, boolean directUse, boolean incoming) {
        super(TYPE, stamp);
        assert register != null;
        this.register = register;
        this.directUse = directUse;
        this.incoming = incoming;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        LIRKind kind = generator.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        Value result = register.asValue(kind);
        if (incoming) {
            generator.getLIRGeneratorTool().emitIncomingValues(new Value[]{result});
        }
        if (!directUse) {
            result = generator.getLIRGeneratorTool().emitMove(result);
        }
        generator.setResult(this, result);
    }

    @Override
    public String toString(Verbosity verbosity) {
        if (verbosity == Verbosity.Name) {
            return super.toString(Verbosity.Name) + "%" + register;
        } else {
            return super.toString(verbosity);
        }
    }
}
