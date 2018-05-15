package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_8;

import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.MetaAccessProvider;

/**
 * A node that represents an exception thrown implicitly by a Java bytecode. It can be lowered to
 * either a {@linkplain ForeignCallDescriptor foreign} call or a pre-allocated exception object.
 */
// @formatter:off
@NodeInfo(cycles = CYCLES_8,
          cyclesRationale = "Node will be lowered to a foreign call.",
          size = SIZE_8)
// @formatter:on
public final class BytecodeExceptionNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single {

    public static final NodeClass<BytecodeExceptionNode> TYPE = NodeClass.create(BytecodeExceptionNode.class);
    protected final Class<? extends Throwable> exceptionClass;
    @Input NodeInputList<ValueNode> arguments;

    public BytecodeExceptionNode(MetaAccessProvider metaAccess, Class<? extends Throwable> exceptionClass, ValueNode... arguments) {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createExactTrusted(metaAccess.lookupJavaType(exceptionClass))));
        this.exceptionClass = exceptionClass;
        this.arguments = new NodeInputList<>(this, arguments);
    }

    public Class<? extends Throwable> getExceptionClass() {
        return exceptionClass;
    }

    @Override
    public String toString(Verbosity verbosity) {
        if (verbosity == Verbosity.Name) {
            return super.toString(verbosity) + "#" + exceptionClass.getSimpleName();
        }
        return super.toString(verbosity);
    }

    @Override
    public LocationIdentity getLocationIdentity() {
        return LocationIdentity.any();
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    public NodeInputList<ValueNode> getArguments() {
        return arguments;
    }
}
