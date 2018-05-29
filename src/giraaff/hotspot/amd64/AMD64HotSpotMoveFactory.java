package giraaff.hotspot.amd64;

import jdk.vm.ci.hotspot.HotSpotCompressedNullConstant;
import jdk.vm.ci.hotspot.HotSpotConstant;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.amd64.AMD64MoveFactory;
import giraaff.lir.LIRInstruction;
import giraaff.lir.amd64.AMD64LIRInstruction;

// @class AMD64HotSpotMoveFactory
public final class AMD64HotSpotMoveFactory extends AMD64MoveFactory
{
    // @cons
    public AMD64HotSpotMoveFactory(BackupSlotProvider backupSlotProvider)
    {
        super(backupSlotProvider);
    }

    @Override
    public boolean canInlineConstant(Constant c)
    {
        if (HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(c))
        {
            return true;
        }
        else if (c instanceof HotSpotObjectConstant)
        {
            return ((HotSpotObjectConstant) c).isCompressed();
        }
        else if (c instanceof HotSpotMetaspaceConstant)
        {
            return ((HotSpotMetaspaceConstant) c).isCompressed();
        }
        else
        {
            return super.canInlineConstant(c);
        }
    }

    @Override
    public boolean allowConstantToStackMove(Constant value)
    {
        if (value instanceof HotSpotConstant)
        {
            return ((HotSpotConstant) value).isCompressed();
        }
        return super.allowConstantToStackMove(value);
    }

    @Override
    public AMD64LIRInstruction createLoad(AllocatableValue dst, Constant src)
    {
        if (HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(src))
        {
            return super.createLoad(dst, JavaConstant.INT_0);
        }
        else if (src instanceof HotSpotObjectConstant)
        {
            return new AMD64HotSpotMove.HotSpotLoadObjectConstantOp(dst, (HotSpotObjectConstant) src);
        }
        else if (src instanceof HotSpotMetaspaceConstant)
        {
            return new AMD64HotSpotMove.HotSpotLoadMetaspaceConstantOp(dst, (HotSpotMetaspaceConstant) src);
        }
        else
        {
            return super.createLoad(dst, src);
        }
    }

    @Override
    public LIRInstruction createStackLoad(AllocatableValue dst, Constant src)
    {
        if (HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(src))
        {
            return super.createStackLoad(dst, JavaConstant.INT_0);
        }
        else if (src instanceof HotSpotObjectConstant)
        {
            return new AMD64HotSpotMove.HotSpotLoadObjectConstantOp(dst, (HotSpotObjectConstant) src);
        }
        else if (src instanceof HotSpotMetaspaceConstant)
        {
            return new AMD64HotSpotMove.HotSpotLoadMetaspaceConstantOp(dst, (HotSpotMetaspaceConstant) src);
        }
        else
        {
            return super.createStackLoad(dst, src);
        }
    }
}
