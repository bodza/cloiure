package giraaff.lir.gen;

import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.calc.FloatConvert;
import giraaff.lir.LIRFrameState;
import giraaff.lir.Variable;

///
// This interface can be used to generate LIR for arithmetic and simple memory access operations.
//
// The setFlags flag in emitAdd, emitSub and emitMul indicates, that the instruction must set the
// flags register to be used for a later branch. (On AMD64, the condition codes are set in every
// arithmetic instruction, but other architectures optionally set the flags register.) If setFlags
// is set, the instruction must set the flags register; if false, the instruction may or may not
// set the flags register.
///
// @iface ArithmeticLIRGeneratorTool
public interface ArithmeticLIRGeneratorTool
{
    Value emitNegate(Value __input);

    Value emitAdd(Value __a, Value __b, boolean __setFlags);

    Value emitSub(Value __a, Value __b, boolean __setFlags);

    Value emitMul(Value __a, Value __b, boolean __setFlags);

    Value emitMulHigh(Value __a, Value __b);

    Value emitUMulHigh(Value __a, Value __b);

    Value emitDiv(Value __a, Value __b, LIRFrameState __state);

    Value emitRem(Value __a, Value __b, LIRFrameState __state);

    Value emitUDiv(Value __a, Value __b, LIRFrameState __state);

    Value emitURem(Value __a, Value __b, LIRFrameState __state);

    Value emitNot(Value __input);

    Value emitAnd(Value __a, Value __b);

    Value emitOr(Value __a, Value __b);

    Value emitXor(Value __a, Value __b);

    Value emitShl(Value __a, Value __b);

    Value emitShr(Value __a, Value __b);

    Value emitUShr(Value __a, Value __b);

    Value emitFloatConvert(FloatConvert __op, Value __inputVal);

    Value emitReinterpret(LIRKind __to, Value __inputVal);

    Value emitNarrow(Value __inputVal, int __bits);

    Value emitSignExtend(Value __inputVal, int __fromBits, int __toBits);

    Value emitZeroExtend(Value __inputVal, int __fromBits, int __toBits);

    Value emitMathAbs(Value __input);

    Value emitMathSqrt(Value __input);

    Value emitBitCount(Value __operand);

    Value emitBitScanForward(Value __operand);

    Value emitBitScanReverse(Value __operand);

    Variable emitLoad(LIRKind __kind, Value __address, LIRFrameState __state);

    void emitStore(ValueKind<?> __kind, Value __address, Value __input, LIRFrameState __state);
}
