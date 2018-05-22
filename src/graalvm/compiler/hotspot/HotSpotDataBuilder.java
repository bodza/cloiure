package graalvm.compiler.hotspot;

import java.nio.ByteBuffer;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotCompressedNullConstant;
import jdk.vm.ci.hotspot.HotSpotConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.SerializableConstant;
import jdk.vm.ci.meta.VMConstant;

import graalvm.compiler.code.DataSection.Data;
import graalvm.compiler.code.DataSection.Patches;
import graalvm.compiler.code.DataSection.SerializableData;
import graalvm.compiler.code.DataSection.ZeroData;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.asm.DataBuilder;

public class HotSpotDataBuilder extends DataBuilder
{
    private final TargetDescription target;

    public HotSpotDataBuilder(TargetDescription target)
    {
        this.target = target;
    }

    @Override
    public boolean needDetailedPatchingInformation()
    {
        /* The HotSpot VM finds operands that need patching by decoding the instruction. */
        return false;
    }

    @Override
    public Data createDataItem(Constant constant)
    {
        if (JavaConstant.isNull(constant))
        {
            boolean compressed = HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(constant);
            int size = compressed ? 4 : target.wordSize;
            return ZeroData.create(size, size);
        }
        else if (constant instanceof VMConstant)
        {
            VMConstant vmConstant = (VMConstant) constant;
            if (!(constant instanceof HotSpotConstant))
            {
                throw new GraalError(String.valueOf(constant));
            }

            HotSpotConstant c = (HotSpotConstant) vmConstant;
            int size = c.isCompressed() ? 4 : target.wordSize;
            return new Data(size, size)
            {
                @Override
                protected void emit(ByteBuffer buffer, Patches patches)
                {
                    int position = buffer.position();
                    if (getSize() == Integer.BYTES)
                    {
                        buffer.putInt(0xDEADDEAD);
                    }
                    else
                    {
                        buffer.putLong(0xDEADDEADDEADDEADL);
                    }
                    patches.registerPatch(position, vmConstant);
                }
            };
        }
        else if (constant instanceof SerializableConstant)
        {
            SerializableConstant s = (SerializableConstant) constant;
            return new SerializableData(s);
        }
        else
        {
            throw new GraalError(String.valueOf(constant));
        }
    }
}
