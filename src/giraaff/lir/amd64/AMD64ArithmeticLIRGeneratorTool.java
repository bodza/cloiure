package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.Value;

import giraaff.lir.Variable;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;

/**
 * This interface can be used to generate AMD64 LIR for arithmetic operations.
 */
// @iface AMD64ArithmeticLIRGeneratorTool
public interface AMD64ArithmeticLIRGeneratorTool extends ArithmeticLIRGeneratorTool
{
    Value emitCountLeadingZeros(Value value);

    Value emitCountTrailingZeros(Value value);

    // @enum AMD64ArithmeticLIRGeneratorTool.RoundingMode
    enum RoundingMode
    {
        NEAREST(0),
        DOWN(1),
        UP(2),
        TRUNCATE(3);

        // @field
        public final int encoding;

        RoundingMode(int __encoding)
        {
            this.encoding = __encoding;
        }
    }

    Value emitRound(Value value, RoundingMode mode);

    void emitCompareOp(AMD64Kind cmpKind, Variable left, Value right);
}
