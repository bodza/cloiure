package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayBaseOffset;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.nodes.ComputeObjectAddressNode;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.word.Word;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;

import jdk.vm.ci.meta.JavaKind;

/**
 * Substitutions for java.util.zip.CRC32C.
 */
@ClassSubstitution(className = "java.util.zip.CRC32C", optional = true)
public class CRC32CSubstitutions
{
    @MethodSubstitution
    static int updateBytes(int crc, byte[] b, int off, int end)
    {
        Word bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(b, arrayBaseOffset(JavaKind.Byte) + off));
        return updateBytesCRC32(UPDATE_BYTES_CRC32C, crc, bufAddr, end - off);
    }

    @MethodSubstitution
    static int updateDirectByteBuffer(int crc, long addr, int off, int end)
    {
        WordBase bufAddr = WordFactory.unsigned(addr).add(off);
        return updateBytesCRC32(UPDATE_BYTES_CRC32C, crc, bufAddr, end - off);
    }

    public static final ForeignCallDescriptor UPDATE_BYTES_CRC32C = new ForeignCallDescriptor("updateBytesCRC32C", int.class, int.class, WordBase.class, int.class);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native int updateBytesCRC32(@ConstantNodeParameter ForeignCallDescriptor descriptor, int crc, WordBase buf, int length);
}
