package graalvm.compiler.hotspot.amd64;

import static graalvm.compiler.hotspot.HotSpotBackend.Options.GraalArithmeticStubs;
import static graalvm.compiler.hotspot.amd64.AMD64HotSpotMathIntrinsicOp.IntrinsicOpcode.COS;
import static graalvm.compiler.hotspot.amd64.AMD64HotSpotMathIntrinsicOp.IntrinsicOpcode.LOG;
import static graalvm.compiler.hotspot.amd64.AMD64HotSpotMathIntrinsicOp.IntrinsicOpcode.LOG10;
import static graalvm.compiler.hotspot.amd64.AMD64HotSpotMathIntrinsicOp.IntrinsicOpcode.SIN;
import static graalvm.compiler.hotspot.amd64.AMD64HotSpotMathIntrinsicOp.IntrinsicOpcode.TAN;

import graalvm.compiler.core.amd64.AMD64ArithmeticLIRGenerator;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.hotspot.HotSpotBackend.Options;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.gen.LIRGenerator;

import jdk.vm.ci.meta.Value;

/**
 * Lowering of selected {@link Math} routines that depends on the value of
 * {@link Options#GraalArithmeticStubs}.
 */
public class AMD64HotSpotMaths implements AMD64ArithmeticLIRGenerator.Maths
{
    @Override
    public Variable emitLog(LIRGenerator gen, Value input, boolean base10)
    {
        if (GraalArithmeticStubs.getValue(gen.getResult().getLIR().getOptions()))
        {
            return null;
        }
        Variable result = gen.newVariable(LIRKind.combine(input));
        gen.append(new AMD64HotSpotMathIntrinsicOp(base10 ? LOG10 : LOG, result, gen.asAllocatable(input)));
        return result;
    }

    @Override
    public Variable emitCos(LIRGenerator gen, Value input)
    {
        if (GraalArithmeticStubs.getValue(gen.getResult().getLIR().getOptions()))
        {
            return null;
        }
        Variable result = gen.newVariable(LIRKind.combine(input));
        gen.append(new AMD64HotSpotMathIntrinsicOp(COS, result, gen.asAllocatable(input)));
        return result;
    }

    @Override
    public Variable emitSin(LIRGenerator gen, Value input)
    {
        if (GraalArithmeticStubs.getValue(gen.getResult().getLIR().getOptions()))
        {
            return null;
        }
        Variable result = gen.newVariable(LIRKind.combine(input));
        gen.append(new AMD64HotSpotMathIntrinsicOp(SIN, result, gen.asAllocatable(input)));
        return result;
    }

    @Override
    public Variable emitTan(LIRGenerator gen, Value input)
    {
        if (GraalArithmeticStubs.getValue(gen.getResult().getLIR().getOptions()))
        {
            return null;
        }
        Variable result = gen.newVariable(LIRKind.combine(input));
        gen.append(new AMD64HotSpotMathIntrinsicOp(TAN, result, gen.asAllocatable(input)));
        return result;
    }
}
