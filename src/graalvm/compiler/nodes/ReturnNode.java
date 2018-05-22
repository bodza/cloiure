package graalvm.compiler.nodes;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.memory.MemoryMapNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public final class ReturnNode extends ControlSinkNode implements LIRLowerable, IterableNodeType
{
    public static final NodeClass<ReturnNode> TYPE = NodeClass.create(ReturnNode.class);
    @OptionalInput ValueNode result;
    @OptionalInput(InputType.Extension) MemoryMapNode memoryMap;

    public ValueNode result()
    {
        return result;
    }

    public ReturnNode(ValueNode result)
    {
        this(result, null);
    }

    public ReturnNode(ValueNode result, MemoryMapNode memoryMap)
    {
        super(TYPE, StampFactory.forVoid());
        this.result = result;
        this.memoryMap = memoryMap;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        if (result == null)
        {
            gen.getLIRGeneratorTool().emitReturn(JavaKind.Void, null);
        }
        else
        {
            gen.getLIRGeneratorTool().emitReturn(result.getStackKind(), gen.operand(result));
        }
    }

    public void setMemoryMap(MemoryMapNode memoryMap)
    {
        updateUsages(this.memoryMap, memoryMap);
        this.memoryMap = memoryMap;
    }

    public MemoryMapNode getMemoryMap()
    {
        return memoryMap;
    }

    private boolean verifyReturn(TargetDescription target)
    {
        if (graph().method() != null)
        {
            JavaKind actual = result == null ? JavaKind.Void : result.getStackKind();
            JavaKind expected = graph().method().getSignature().getReturnKind().getStackKind();
            if (actual == target.wordJavaKind && expected == JavaKind.Object)
            {
                // OK, we're compiling a snippet that returns a Word
                return true;
            }
        }
        return true;
    }
}
