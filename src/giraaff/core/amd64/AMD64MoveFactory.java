package giraaff.core.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Assembler;
import giraaff.core.common.NumUtil;
import giraaff.core.common.type.DataPointerConstant;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.amd64.AMD64AddressValue;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.amd64.AMD64Move;
import giraaff.util.GraalError;

// @class AMD64MoveFactory
public abstract class AMD64MoveFactory extends AMD64MoveFactoryBase
{
    // @cons AMD64MoveFactory
    public AMD64MoveFactory(AMD64MoveFactoryBase.BackupSlotProvider __backupSlotProvider)
    {
        super(__backupSlotProvider);
    }

    @Override
    public boolean canInlineConstant(Constant __con)
    {
        if (__con instanceof JavaConstant)
        {
            JavaConstant __c = (JavaConstant) __con;
            switch (__c.getJavaKind())
            {
                case Long:
                    return NumUtil.isInt(__c.asLong());
                case Object:
                    return __c.isNull();
                default:
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean allowConstantToStackMove(Constant __constant)
    {
        if (__constant instanceof DataPointerConstant)
        {
            return false;
        }
        if (__constant instanceof JavaConstant && !AMD64Move.canMoveConst2Stack(((JavaConstant) __constant)))
        {
            return false;
        }
        return true;
    }

    @Override
    public AMD64LIRInstruction createMove(AllocatableValue __dst, Value __src)
    {
        if (__src instanceof AMD64AddressValue)
        {
            return new AMD64Move.LeaOp(__dst, (AMD64AddressValue) __src, AMD64Assembler.OperandSize.QWORD);
        }
        else if (LIRValueUtil.isConstantValue(__src))
        {
            return createLoad(__dst, LIRValueUtil.asConstant(__src));
        }
        else if (ValueUtil.isRegister(__src) || LIRValueUtil.isStackSlotValue(__dst))
        {
            return new AMD64Move.MoveFromRegOp((AMD64Kind) __dst.getPlatformKind(), __dst, (AllocatableValue) __src);
        }
        else
        {
            return new AMD64Move.MoveToRegOp((AMD64Kind) __dst.getPlatformKind(), __dst, (AllocatableValue) __src);
        }
    }

    @Override
    public AMD64LIRInstruction createStackMove(AllocatableValue __result, AllocatableValue __input, Register __scratchRegister, AllocatableValue __backupSlot)
    {
        return new AMD64Move.AMD64StackMove(__result, __input, __scratchRegister, __backupSlot);
    }

    @Override
    public AMD64LIRInstruction createLoad(AllocatableValue __dst, Constant __src)
    {
        if (__src instanceof JavaConstant)
        {
            return new AMD64Move.MoveFromConstOp(__dst, (JavaConstant) __src);
        }
        else if (__src instanceof DataPointerConstant)
        {
            return new AMD64Move.LeaDataOp(__dst, (DataPointerConstant) __src);
        }
        else
        {
            throw GraalError.shouldNotReachHere("unsupported constant: " + __src);
        }
    }

    @Override
    public LIRInstruction createStackLoad(AllocatableValue __result, Constant __input)
    {
        if (__input instanceof JavaConstant)
        {
            return new AMD64Move.MoveFromConstOp(__result, (JavaConstant) __input);
        }
        else
        {
            throw GraalError.shouldNotReachHere("unsupported constant for stack load: " + __input);
        }
    }
}
