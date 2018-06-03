package giraaff.nodes;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
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

    @OptionalInput
    // @field
    ValueNode result;
    @OptionalInput(InputType.Extension)
    // @field
    MemoryMapNode memoryMap;

    public ValueNode result()
    {
        return result;
    }

    // @cons
    public ReturnNode(ValueNode __result)
    {
        this(__result, null);
    }

    // @cons
    public ReturnNode(ValueNode __result, MemoryMapNode __memoryMap)
    {
        super(TYPE, StampFactory.forVoid());
        this.result = __result;
        this.memoryMap = __memoryMap;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        if (result == null)
        {
            __gen.getLIRGeneratorTool().emitReturn(JavaKind.Void, null);
        }
        else
        {
            __gen.getLIRGeneratorTool().emitReturn(result.getStackKind(), __gen.operand(result));
        }
    }

    public void setMemoryMap(MemoryMapNode __memoryMap)
    {
        updateUsages(this.memoryMap, __memoryMap);
        this.memoryMap = __memoryMap;
    }

    public MemoryMapNode getMemoryMap()
    {
        return memoryMap;
    }

    private boolean verifyReturn(TargetDescription __target)
    {
        if (graph().method() != null)
        {
            JavaKind __actual = result == null ? JavaKind.Void : result.getStackKind();
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
