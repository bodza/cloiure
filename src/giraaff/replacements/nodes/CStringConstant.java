package giraaff.replacements.nodes;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.DataPointerConstant;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.word.Word;

/**
 * Represents a compile-time constant zero-terminated UTF-8 string installed with the generated
 * code.
 */
public final class CStringConstant extends DataPointerConstant
{
    private static final Charset UTF8 = Charset.forName("utf8");

    private final String string;

    public CStringConstant(String string)
    {
        super(1);
        this.string = string;
    }

    @Override
    public int getSerializedSize()
    {
        return string.getBytes(UTF8).length + 1;
    }

    @Override
    public void serialize(ByteBuffer buffer)
    {
        byte[] bytes = string.getBytes(UTF8);
        buffer.put(bytes);
        buffer.put((byte) 0);
    }

    @Override
    public String toValueString()
    {
        return "c\"" + string + "\"";
    }

    public static boolean intrinsify(GraphBuilderContext b, @SuppressWarnings("unused") ResolvedJavaMethod targetMethod, String string)
    {
        b.addPush(JavaKind.Object, new ConstantNode(new CStringConstant(string), StampFactory.pointer()));
        return true;
    }

    @NodeIntrinsic
    public static native Word cstring(@ConstantNodeParameter String string);
}
