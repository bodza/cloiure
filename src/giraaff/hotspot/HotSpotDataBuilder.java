package giraaff.hotspot;

import java.nio.ByteBuffer;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotCompressedNullConstant;
import jdk.vm.ci.hotspot.HotSpotConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.SerializableConstant;
import jdk.vm.ci.meta.VMConstant;

import giraaff.code.DataSection.Data;
import giraaff.code.DataSection.Patches;
import giraaff.code.DataSection.SerializableData;
import giraaff.code.DataSection.ZeroData;
import giraaff.lir.asm.DataBuilder;
import giraaff.util.GraalError;

// @class HotSpotDataBuilder
public final class HotSpotDataBuilder extends DataBuilder
{
    // @field
    private final TargetDescription target;

    // @cons
    public HotSpotDataBuilder(TargetDescription __target)
    {
        super();
        this.target = __target;
    }

    @Override
    public Data createDataItem(Constant __constant)
    {
        if (JavaConstant.isNull(__constant))
        {
            int __size = HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(__constant) ? 4 : target.wordSize;
            return ZeroData.create(__size, __size);
        }
        else if (__constant instanceof VMConstant)
        {
            VMConstant __vmConstant = (VMConstant) __constant;
            if (!(__constant instanceof HotSpotConstant))
            {
                throw new GraalError(String.valueOf(__constant));
            }

            HotSpotConstant __c = (HotSpotConstant) __vmConstant;
            int __size = __c.isCompressed() ? 4 : target.wordSize;
            // @closure
            return new Data(__size, __size)
            {
                @Override
                protected void emit(ByteBuffer __buffer, Patches __patches)
                {
                    int __position = __buffer.position();
                    if (getSize() == Integer.BYTES)
                    {
                        __buffer.putInt(0xDEADDEAD);
                    }
                    else
                    {
                        __buffer.putLong(0xDEADDEADDEADDEADL);
                    }
                    __patches.registerPatch(__position, __vmConstant);
                }
            };
        }
        else if (__constant instanceof SerializableConstant)
        {
            return new SerializableData((SerializableConstant) __constant);
        }
        else
        {
            throw new GraalError(String.valueOf(__constant));
        }
    }
}
