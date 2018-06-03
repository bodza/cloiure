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

///
// Represents a compile-time constant zero-terminated UTF-8 string installed with the generated code.
///
// @class CStringConstant
public final class CStringConstant extends DataPointerConstant
{
    // @def
    private static final Charset UTF8 = Charset.forName("utf8");

    // @field
    private final String ___string;

    // @cons
    public CStringConstant(String __string)
    {
        super(1);
        this.___string = __string;
    }

    @Override
    public int getSerializedSize()
    {
        return this.___string.getBytes(UTF8).length + 1;
    }

    @Override
    public void serialize(ByteBuffer __buffer)
    {
        byte[] __bytes = this.___string.getBytes(UTF8);
        __buffer.put(__bytes);
        __buffer.put((byte) 0);
    }

    @Override
    public String toValueString()
    {
        return "c\"" + this.___string + "\"";
    }

    public static boolean intrinsify(GraphBuilderContext __b, @SuppressWarnings("unused") ResolvedJavaMethod __targetMethod, String __string)
    {
        __b.addPush(JavaKind.Object, new ConstantNode(new CStringConstant(__string), StampFactory.pointer()));
        return true;
    }

    @NodeIntrinsic
    public static native Word cstring(@ConstantNodeParameter String __string);
}
