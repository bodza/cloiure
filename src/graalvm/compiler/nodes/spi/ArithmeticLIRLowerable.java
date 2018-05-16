package graalvm.compiler.nodes.spi;

import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;

public interface ArithmeticLIRLowerable extends LIRLowerable
{
    @Override
    default void generate(NodeLIRBuilderTool builder)
    {
        generate(builder, builder.getLIRGeneratorTool().getArithmetic());
    }

    void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen);
}
