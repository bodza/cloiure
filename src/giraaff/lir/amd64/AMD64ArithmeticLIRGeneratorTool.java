package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.Value;

import giraaff.lir.Variable;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;

///
// This interface can be used to generate AMD64 LIR for arithmetic operations.
///
// @iface AMD64ArithmeticLIRGeneratorTool
public interface AMD64ArithmeticLIRGeneratorTool extends ArithmeticLIRGeneratorTool
{
    Value emitCountLeadingZeros(Value __value);

    Value emitCountTrailingZeros(Value __value);

    // @enum AMD64ArithmeticLIRGeneratorTool.RoundingMode
    enum RoundingMode
    {
        NEAREST(0),
        DOWN(1),
        UP(2),
        TRUNCATE(3);

        // @field
        public final int ___encoding;

        RoundingMode(int __encoding)
        {
            this.___encoding = __encoding;
        }
    }

    Value emitRound(Value __value, RoundingMode __mode);

    void emitCompareOp(AMD64Kind __cmpKind, Variable __left, Value __right);
}
