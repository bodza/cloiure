package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.JavaKind;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.nodes.BasicArrayCopyNode;

// @NodeInfo.allowedUsageTypes "Memory"
// @class ArrayCopyWithSlowPathNode
public final class ArrayCopyWithSlowPathNode extends BasicArrayCopyNode
{
    // @def
    public static final NodeClass<ArrayCopyWithSlowPathNode> TYPE = NodeClass.create(ArrayCopyWithSlowPathNode.class);

    // @field
    private final SnippetTemplate.SnippetInfo snippet;

    // @cons
    public ArrayCopyWithSlowPathNode(ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, SnippetTemplate.SnippetInfo __snippet, JavaKind __elementKind)
    {
        super(TYPE, __src, __srcPos, __dest, __destPos, __length, __elementKind, BytecodeFrame.INVALID_FRAMESTATE_BCI);
        this.snippet = __snippet;
    }

    @NodeIntrinsic
    public static native void arraycopy(Object nonNullSrc, int srcPos, Object nonNullDest, int destPos, int length, @ConstantNodeParameter SnippetTemplate.SnippetInfo snippet, @ConstantNodeParameter JavaKind elementKind);

    public SnippetTemplate.SnippetInfo getSnippet()
    {
        return snippet;
    }

    public void setBci(int __bci)
    {
        this.bci = __bci;
    }
}
