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
import giraaff.debug.GraalError;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.amd64.AMD64AddressValue;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.amd64.AMD64Move;
import giraaff.lir.amd64.AMD64Move.AMD64StackMove;
import giraaff.lir.amd64.AMD64Move.LeaDataOp;
import giraaff.lir.amd64.AMD64Move.LeaOp;
import giraaff.lir.amd64.AMD64Move.MoveFromConstOp;
import giraaff.lir.amd64.AMD64Move.MoveFromRegOp;
import giraaff.lir.amd64.AMD64Move.MoveToRegOp;

public abstract class AMD64MoveFactory extends AMD64MoveFactoryBase
{
    public AMD64MoveFactory(BackupSlotProvider backupSlotProvider)
    {
        super(backupSlotProvider);
    }

    @Override
    public boolean canInlineConstant(Constant con)
    {
        if (con instanceof JavaConstant)
        {
            JavaConstant c = (JavaConstant) con;
            switch (c.getJavaKind())
            {
                case Long:
                    return NumUtil.isInt(c.asLong());
                case Object:
                    return c.isNull();
                default:
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean allowConstantToStackMove(Constant constant)
    {
        if (constant instanceof DataPointerConstant)
        {
            return false;
        }
        if (constant instanceof JavaConstant && !AMD64Move.canMoveConst2Stack(((JavaConstant) constant)))
        {
            return false;
        }
        return true;
    }

    @Override
    public AMD64LIRInstruction createMove(AllocatableValue dst, Value src)
    {
        if (src instanceof AMD64AddressValue)
        {
            return new LeaOp(dst, (AMD64AddressValue) src, AMD64Assembler.OperandSize.QWORD);
        }
        else if (LIRValueUtil.isConstantValue(src))
        {
            return createLoad(dst, LIRValueUtil.asConstant(src));
        }
        else if (ValueUtil.isRegister(src) || LIRValueUtil.isStackSlotValue(dst))
        {
            return new MoveFromRegOp((AMD64Kind) dst.getPlatformKind(), dst, (AllocatableValue) src);
        }
        else
        {
            return new MoveToRegOp((AMD64Kind) dst.getPlatformKind(), dst, (AllocatableValue) src);
        }
    }

    @Override
    public AMD64LIRInstruction createStackMove(AllocatableValue result, AllocatableValue input, Register scratchRegister, AllocatableValue backupSlot)
    {
        return new AMD64StackMove(result, input, scratchRegister, backupSlot);
    }

    @Override
    public AMD64LIRInstruction createLoad(AllocatableValue dst, Constant src)
    {
        if (src instanceof JavaConstant)
        {
            return new MoveFromConstOp(dst, (JavaConstant) src);
        }
        else if (src instanceof DataPointerConstant)
        {
            return new LeaDataOp(dst, (DataPointerConstant) src);
        }
        else
        {
            throw GraalError.shouldNotReachHere(String.format("unsupported constant: %s", src));
        }
    }

    @Override
    public LIRInstruction createStackLoad(AllocatableValue result, Constant input)
    {
        if (input instanceof JavaConstant)
        {
            return new MoveFromConstOp(result, (JavaConstant) input);
        }
        else
        {
            throw GraalError.shouldNotReachHere(String.format("unsupported constant for stack load: %s", input));
        }
    }
}
