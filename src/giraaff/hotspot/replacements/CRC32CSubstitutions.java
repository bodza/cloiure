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

/**
 * Substitutions for java.util.zip.CRC32C.
 */
@ClassSubstitution(className = "java.util.zip.CRC32C", optional = true)
public class CRC32CSubstitutions
{
    @MethodSubstitution
    static int updateBytes(int crc, byte[] b, int off, int end)
    {
        Word bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(b, HotSpotReplacementsUtil.arrayBaseOffset(JavaKind.Byte) + off));
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
