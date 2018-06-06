package giraaff.nodes;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.memory.MemoryMapNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class ReturnNode
public final class ReturnNode extends ControlSinkNode implements LIRLowerable, IterableNodeType
{
    // @def
    public static final NodeClass<ReturnNode> TYPE = NodeClass.create(ReturnNode.class);

    @Node.OptionalInput
    // @field
    ValueNode ___result;
    @Node.OptionalInput(InputType.Extension)
    // @field
    MemoryMapNode ___memoryMap;

    public ValueNode result()
    {
        return this.___result;
    }

    // @cons ReturnNode
    public ReturnNode(ValueNode __result)
    {
        this(__result, null);
    }

    // @cons ReturnNode
    public ReturnNode(ValueNode __result, MemoryMapNode __memoryMap)
    {
        super(TYPE, StampFactory.forVoid());
        this.___result = __result;
        this.___memoryMap = __memoryMap;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        if (this.___result == null)
        {
            __gen.getLIRGeneratorTool().emitReturn(JavaKind.Void, null);
        }
        else
        {
            __gen.getLIRGeneratorTool().emitReturn(this.___result.getStackKind(), __gen.operand(this.___result));
        }
    }

    public void setMemoryMap(MemoryMapNode __memoryMap)
    {
        updateUsages(this.___memoryMap, __memoryMap);
        this.___memoryMap = __memoryMap;
    }

    public MemoryMapNode getMemoryMap()
    {
        return this.___memoryMap;
    }

    private boolean verifyReturn(TargetDescription __target)
    {
        if (graph().method() != null)
        {
            JavaKind __actual = this.___result == null ? JavaKind.Void : this.___result.getStackKind();
            JavaKind __expected = graph().method().getSignature().getReturnKind().getStackKind();
            if (__actual == __target.wordJavaKind && __expected == JavaKind.Object)
            {
                // OK, we're compiling a snippet that returns a Word
                return true;
            }
        }
        return true;
    }
}
