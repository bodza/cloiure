package giraaff.hotspot.replacements;

import java.util.zip.CRC32;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.Pointer;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.hotspot.nodes.GraalHotSpotVMConfigNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.word.Word;

///
// Substitutions for {@link CRC32}.
///
@ClassSubstitution(CRC32.class)
// @class CRC32Substitutions
public final class CRC32Substitutions
{
    // @cons
    private CRC32Substitutions()
    {
        super();
    }

    ///
    // Removed in 9.
    ///
    @MethodSubstitution(optional = true)
    static int update(int __crc, int __b)
    {
        final Pointer __crcTableRawAddress = WordFactory.pointer(GraalHotSpotVMConfigNode.crcTableAddress());

        int __c = ~__crc;
        int __index = (__b ^ __c) & 0xFF;
        int __offset = __index << 2;
        int __result = __crcTableRawAddress.readInt(__offset);
        __result = __result ^ (__c >>> 8);
        return ~__result;
    }

    ///
    // Removed in 9.
    ///
    @MethodSubstitution(optional = true)
    static int updateBytes(int __crc, byte[] __buf, int __off, int __len)
    {
        Word __bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__buf, HotSpotReplacementsUtil.arrayBaseOffset(JavaKind.Byte) + __off));
        return updateBytesCRC32(UPDATE_BYTES_CRC32, __crc, __bufAddr, __len);
    }

    ///
    // @since 9
    ///
    @MethodSubstitution(optional = true)
    static int updateBytes0(int __crc, byte[] __buf, int __off, int __len)
    {
        Word __bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__buf, HotSpotReplacementsUtil.arrayBaseOffset(JavaKind.Byte) + __off));
        return updateBytesCRC32(UPDATE_BYTES_CRC32, __crc, __bufAddr, __len);
    }

    ///
    // Removed in 9.
    ///
    @MethodSubstitution(optional = true)
    static int updateByteBuffer(int __crc, long __addr, int __off, int __len)
    {
        WordBase __bufAddr = WordFactory.unsigned(__addr).add(__off);
        return updateBytesCRC32(UPDATE_BYTES_CRC32, __crc, __bufAddr, __len);
    }

    ///
    // @since 9
    ///
    @MethodSubstitution(optional = true)
    static int updateByteBuffer0(int __crc, long __addr, int __off, int __len)
    {
        WordBase __bufAddr = WordFactory.unsigned(__addr).add(__off);
        return updateBytesCRC32(UPDATE_BYTES_CRC32, __crc, __bufAddr, __len);
    }

    // @def
    public static final ForeignCallDescriptor UPDATE_BYTES_CRC32 = new ForeignCallDescriptor("updateBytesCRC32", int.class, int.class, WordBase.class, int.class);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native int updateBytesCRC32(@ConstantNodeParameter ForeignCallDescriptor __descriptor, int __crc, WordBase __buf, int __length);
}
