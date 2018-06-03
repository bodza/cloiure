package giraaff.hotspot.replacements;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.word.Word;

///
// Substitutions for java.util.zip.CRC32C.
///
@ClassSubstitution(className = "java.util.zip.CRC32C", optional = true)
// @class CRC32CSubstitutions
public final class CRC32CSubstitutions
{
    // @cons
    private CRC32CSubstitutions()
    {
        super();
    }

    // @def
    public static final ForeignCallDescriptor UPDATE_BYTES_CRC32C = new ForeignCallDescriptor("updateBytesCRC32C", int.class, int.class, WordBase.class, int.class);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native int updateBytesCRC32(@ConstantNodeParameter ForeignCallDescriptor __descriptor, int __crc, WordBase __buf, int __length);

    @MethodSubstitution
    static int updateBytes(int __crc, byte[] __b, int __off, int __end)
    {
        Word __bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__b, HotSpotReplacementsUtil.arrayBaseOffset(JavaKind.Byte) + __off));
        return updateBytesCRC32(UPDATE_BYTES_CRC32C, __crc, __bufAddr, __end - __off);
    }

    @MethodSubstitution
    static int updateDirectByteBuffer(int __crc, long __addr, int __off, int __end)
    {
        WordBase __bufAddr = WordFactory.unsigned(__addr).add(__off);
        return updateBytesCRC32(UPDATE_BYTES_CRC32C, __crc, __bufAddr, __end - __off);
    }
}
