package giraaff.nodes.extended;

import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// A node that represents an exception thrown implicitly by a Java bytecode. It can be lowered to
// either a {@linkplain ForeignCallDescriptor foreign} call or a pre-allocated exception object.
///
// @class BytecodeExceptionNode
public final class BytecodeExceptionNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<BytecodeExceptionNode> TYPE = NodeClass.create(BytecodeExceptionNode.class);

    // @field
    protected final Class<? extends Throwable> ___exceptionClass;
    @Node.Input
    // @field
    NodeInputList<ValueNode> ___arguments;

    // @cons BytecodeExceptionNode
    public BytecodeExceptionNode(MetaAccessProvider __metaAccess, Class<? extends Throwable> __exceptionClass, ValueNode... __arguments)
    {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createExactTrusted(__metaAccess.lookupJavaType(__exceptionClass))));
        this.___exceptionClass = __exceptionClass;
        this.___arguments = new NodeInputList<>(this, __arguments);
    }

    public Class<? extends Throwable> getExceptionClass()
    {
        return this.___exceptionClass;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public NodeInputList<ValueNode> getArguments()
    {
        return this.___arguments;
    }
}
