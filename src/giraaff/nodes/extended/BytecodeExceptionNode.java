package giraaff.nodes.extended;

import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * A node that represents an exception thrown implicitly by a Java bytecode. It can be lowered to
 * either a {@linkplain ForeignCallDescriptor foreign} call or a pre-allocated exception object.
 */
public final class BytecodeExceptionNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<BytecodeExceptionNode> TYPE = NodeClass.create(BytecodeExceptionNode.class);
    protected final Class<? extends Throwable> exceptionClass;
    @Input NodeInputList<ValueNode> arguments;

    public BytecodeExceptionNode(MetaAccessProvider metaAccess, Class<? extends Throwable> exceptionClass, ValueNode... arguments)
    {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createExactTrusted(metaAccess.lookupJavaType(exceptionClass))));
        this.exceptionClass = exceptionClass;
        this.arguments = new NodeInputList<>(this, arguments);
    }

    public Class<? extends Throwable> getExceptionClass()
    {
        return exceptionClass;
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Name)
        {
            return super.toString(verbosity) + "#" + exceptionClass.getSimpleName();
        }
        return super.toString(verbosity);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public NodeInputList<ValueNode> getArguments()
    {
        return arguments;
    }
}
