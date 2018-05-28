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

/**
 * Substitutions for {@link CRC32}.
 */
@ClassSubstitution(CRC32.class)
public class CRC32Substitutions
{
    /**
     * Removed in 9.
     */
    @MethodSubstitution(optional = true)
    static int update(int crc, int b)
    {
        final Pointer crcTableRawAddress = WordFactory.pointer(GraalHotSpotVMConfigNode.crcTableAddress());

        int c = ~crc;
        int index = (b ^ c) & 0xFF;
        int offset = index << 2;
        int result = crcTableRawAddress.readInt(offset);
        result = result ^ (c >>> 8);
        return ~result;
    }

    /**
     * Removed in 9.
     */
    @MethodSubstitution(optional = true)
    static int updateBytes(int crc, byte[] buf, int off, int len)
    {
        Word bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(buf, HotSpotReplacementsUtil.arrayBaseOffset(JavaKind.Byte) + off));
        return updateBytesCRC32(UPDATE_BYTES_CRC32, crc, bufAddr, len);
    }

    /**
     * @since 9
     */
    @MethodSubstitution(optional = true)
    static int updateBytes0(int crc, byte[] buf, int off, int len)
    {
        Word bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(buf, HotSpotReplacementsUtil.arrayBaseOffset(JavaKind.Byte) + off));
        return updateBytesCRC32(UPDATE_BYTES_CRC32, crc, bufAddr, len);
    }

    /**
     * Removed in 9.
     */
    @MethodSubstitution(optional = true)
    static int updateByteBuffer(int crc, long addr, int off, int len)
    {
        WordBase bufAddr = WordFactory.unsigned(addr).add(off);
        return updateBytesCRC32(UPDATE_BYTES_CRC32, crc, bufAddr, len);
    }

    /**
     * @since 9
     */
    @MethodSubstitution(optional = true)
    static int updateByteBuffer0(int crc, long addr, int off, int len)
    {
        WordBase bufAddr = WordFactory.unsigned(addr).add(off);
        return updateBytesCRC32(UPDATE_BYTES_CRC32, crc, bufAddr, len);
    }

    public static final ForeignCallDescriptor UPDATE_BYTES_CRC32 = new ForeignCallDescriptor("updateBytesCRC32", int.class, int.class, WordBase.class, int.class);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native int updateBytesCRC32(@ConstantNodeParameter ForeignCallDescriptor descriptor, int crc, WordBase buf, int length);
}
