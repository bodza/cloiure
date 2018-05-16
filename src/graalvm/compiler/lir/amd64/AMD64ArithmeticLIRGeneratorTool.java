package graalvm.compiler.lir.amd64;

import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.Value;

/**
 * This interface can be used to generate AMD64 LIR for arithmetic operations.
 */
public interface AMD64ArithmeticLIRGeneratorTool extends ArithmeticLIRGeneratorTool
{
    Value emitCountLeadingZeros(Value value);

    Value emitCountTrailingZeros(Value value);

    enum RoundingMode
    {
        NEAREST(0),
        DOWN(1),
        UP(2),
        TRUNCATE(3);

        public final int encoding;

        RoundingMode(int encoding)
        {
            this.encoding = encoding;
        }
    }

    Value emitRound(Value value, RoundingMode mode);

    void emitCompareOp(AMD64Kind cmpKind, Variable left, Value right);
}
