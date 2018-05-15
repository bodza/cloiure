package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayBaseOffset;

import java.util.zip.CRC32;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.Fold.InjectedParameter;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.nodes.ComputeObjectAddressNode;
import graalvm.compiler.hotspot.nodes.GraalHotSpotVMConfigNode;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.word.Word;
import org.graalvm.word.Pointer;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;

import jdk.vm.ci.meta.JavaKind;

/**
 * Substitutions for {@link CRC32}.
 */
@ClassSubstitution(CRC32.class)
public class CRC32Substitutions {

    /**
     * Gets the address of {@code StubRoutines::x86::_crc_table} in {@code stubRoutines_x86.hpp}.
     */
    @Fold
    static long crcTableAddress(@InjectedParameter GraalHotSpotVMConfig config) {
        return config.crcTableAddress;
    }

    /**
     * Removed in 9.
     */
    @MethodSubstitution(optional = true)
    static int update(int crc, int b) {
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
    static int updateBytes(int crc, byte[] buf, int off, int len) {
        Word bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(buf, arrayBaseOffset(JavaKind.Byte) + off));
        return updateBytesCRC32(UPDATE_BYTES_CRC32, crc, bufAddr, len);
    }

    /**
     * @since 9
     */
    @MethodSubstitution(optional = true)
    static int updateBytes0(int crc, byte[] buf, int off, int len) {
        Word bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(buf, arrayBaseOffset(JavaKind.Byte) + off));
        return updateBytesCRC32(UPDATE_BYTES_CRC32, crc, bufAddr, len);
    }

    /**
     * Removed in 9.
     */
    @MethodSubstitution(optional = true)
    static int updateByteBuffer(int crc, long addr, int off, int len) {
        WordBase bufAddr = WordFactory.unsigned(addr).add(off);
        return updateBytesCRC32(UPDATE_BYTES_CRC32, crc, bufAddr, len);
    }

    /**
     * @since 9
     */
    @MethodSubstitution(optional = true)
    static int updateByteBuffer0(int crc, long addr, int off, int len) {
        WordBase bufAddr = WordFactory.unsigned(addr).add(off);
        return updateBytesCRC32(UPDATE_BYTES_CRC32, crc, bufAddr, len);
    }

    public static final ForeignCallDescriptor UPDATE_BYTES_CRC32 = new ForeignCallDescriptor("updateBytesCRC32", int.class, int.class, WordBase.class, int.class);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native int updateBytesCRC32(@ConstantNodeParameter ForeignCallDescriptor descriptor, int crc, WordBase buf, int length);
}
