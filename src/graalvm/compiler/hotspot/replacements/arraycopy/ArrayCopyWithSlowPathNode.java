package graalvm.compiler.hotspot.replacements.arraycopy;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.nodes.BasicArrayCopyNode;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.JavaKind;

@NodeInfo(allowedUsageTypes = InputType.Memory)
public final class ArrayCopyWithSlowPathNode extends BasicArrayCopyNode
{
    public static final NodeClass<ArrayCopyWithSlowPathNode> TYPE = NodeClass.create(ArrayCopyWithSlowPathNode.class);

    private final SnippetTemplate.SnippetInfo snippet;

    public ArrayCopyWithSlowPathNode(ValueNode src, ValueNode srcPos, ValueNode dest, ValueNode destPos, ValueNode length, SnippetTemplate.SnippetInfo snippet, JavaKind elementKind)
    {
        super(TYPE, src, srcPos, dest, destPos, length, elementKind, BytecodeFrame.INVALID_FRAMESTATE_BCI);
        this.snippet = snippet;
    }

    @NodeIntrinsic
    public static native void arraycopy(Object nonNullSrc, int srcPos, Object nonNullDest, int destPos, int length, @ConstantNodeParameter SnippetTemplate.SnippetInfo snippet, @ConstantNodeParameter JavaKind elementKind);

    public SnippetTemplate.SnippetInfo getSnippet()
    {
        return snippet;
    }

    public void setBci(int bci)
    {
        this.bci = bci;
    }
}
