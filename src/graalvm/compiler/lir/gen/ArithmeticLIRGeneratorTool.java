package graalvm.compiler.lir.gen;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.calc.FloatConvert;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.Variable;

import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

/**
 * This interface can be used to generate LIR for arithmetic and simple memory access operations.
 *
 * The setFlags flag in emitAdd, emitSub and emitMul indicates, that the instruction must set the
 * flags register to be used for a later branch. (On AMD64, the condition codes are set in every
 * arithmetic instruction, but other architectures optionally set the flags register) If setFlags is
 * set, the instruction must set the flags register; if false, the instruction may or may not set
 * the flags register.
 */
public interface ArithmeticLIRGeneratorTool {

    Value emitNegate(Value input);

    Value emitAdd(Value a, Value b, boolean setFlags);

    Value emitSub(Value a, Value b, boolean setFlags);

    Value emitMul(Value a, Value b, boolean setFlags);

    Value emitMulHigh(Value a, Value b);

    Value emitUMulHigh(Value a, Value b);

    Value emitDiv(Value a, Value b, LIRFrameState state);

    Value emitRem(Value a, Value b, LIRFrameState state);

    Value emitUDiv(Value a, Value b, LIRFrameState state);

    Value emitURem(Value a, Value b, LIRFrameState state);

    Value emitNot(Value input);

    Value emitAnd(Value a, Value b);

    Value emitOr(Value a, Value b);

    Value emitXor(Value a, Value b);

    Value emitShl(Value a, Value b);

    Value emitShr(Value a, Value b);

    Value emitUShr(Value a, Value b);

    Value emitFloatConvert(FloatConvert op, Value inputVal);

    Value emitReinterpret(LIRKind to, Value inputVal);

    Value emitNarrow(Value inputVal, int bits);

    Value emitSignExtend(Value inputVal, int fromBits, int toBits);

    Value emitZeroExtend(Value inputVal, int fromBits, int toBits);

    Value emitMathAbs(Value input);

    Value emitMathSqrt(Value input);

    Value emitBitCount(Value operand);

    Value emitBitScanForward(Value operand);

    Value emitBitScanReverse(Value operand);

    Variable emitLoad(LIRKind kind, Value address, LIRFrameState state);

    void emitStore(ValueKind<?> kind, Value address, Value input, LIRFrameState state);

    @SuppressWarnings("unused")
    default Value emitMathLog(Value input, boolean base10) {
        throw GraalError.unimplemented("No specialized implementation available");
    }

    @SuppressWarnings("unused")
    default Value emitMathCos(Value input) {
        throw GraalError.unimplemented("No specialized implementation available");
    }

    @SuppressWarnings("unused")
    default Value emitMathSin(Value input) {
        throw GraalError.unimplemented("No specialized implementation available");
    }

    @SuppressWarnings("unused")
    default Value emitMathTan(Value input) {
        throw GraalError.unimplemented("No specialized implementation available");
    }

    @SuppressWarnings("unused")
    default Value emitMathExp(Value input) {
        throw GraalError.unimplemented("No specialized implementation available");
    }

    @SuppressWarnings("unused")
    default Value emitMathPow(Value x, Value y) {
        throw GraalError.unimplemented("No specialized implementation available");
    }

}
