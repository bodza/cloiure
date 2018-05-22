package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.JavaKind;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ValueNode;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.nodes.BasicArrayCopyNode;

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
