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
    public AMD64HotSpotMoveFactory(BackupSlotProvider __backupSlotProvider)
    {
        super(__backupSlotProvider);
    }

    @Override
    public boolean canInlineConstant(Constant __c)
    {
        if (HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(__c))
        {
            return true;
        }
        else if (__c instanceof HotSpotObjectConstant)
        {
            return ((HotSpotObjectConstant) __c).isCompressed();
        }
        else if (__c instanceof HotSpotMetaspaceConstant)
        {
            return ((HotSpotMetaspaceConstant) __c).isCompressed();
        }
        else
        {
            return super.canInlineConstant(__c);
        }
    }

    @Override
    public boolean allowConstantToStackMove(Constant __value)
    {
        if (__value instanceof HotSpotConstant)
        {
            return ((HotSpotConstant) __value).isCompressed();
        }
        return super.allowConstantToStackMove(__value);
    }

    @Override
    public AMD64LIRInstruction createLoad(AllocatableValue __dst, Constant __src)
    {
        if (HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(__src))
        {
            return super.createLoad(__dst, JavaConstant.INT_0);
        }
        else if (__src instanceof HotSpotObjectConstant)
        {
            return new AMD64HotSpotMove.HotSpotLoadObjectConstantOp(__dst, (HotSpotObjectConstant) __src);
        }
        else if (__src instanceof HotSpotMetaspaceConstant)
        {
            return new AMD64HotSpotMove.HotSpotLoadMetaspaceConstantOp(__dst, (HotSpotMetaspaceConstant) __src);
        }
        else
        {
            return super.createLoad(__dst, __src);
        }
    }

    @Override
    public LIRInstruction createStackLoad(AllocatableValue __dst, Constant __src)
    {
        if (HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(__src))
        {
            return super.createStackLoad(__dst, JavaConstant.INT_0);
        }
        else if (__src instanceof HotSpotObjectConstant)
        {
            return new AMD64HotSpotMove.HotSpotLoadObjectConstantOp(__dst, (HotSpotObjectConstant) __src);
        }
        else if (__src instanceof HotSpotMetaspaceConstant)
        {
            return new AMD64HotSpotMove.HotSpotLoadMetaspaceConstantOp(__dst, (HotSpotMetaspaceConstant) __src);
        }
        else
        {
            return super.createStackLoad(__dst, __src);
        }
    }
}
