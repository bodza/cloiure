package giraaff.nodes.spi;

import giraaff.lir.gen.ArithmeticLIRGeneratorTool;

// @iface ArithmeticLIRLowerable
public interface ArithmeticLIRLowerable extends LIRLowerable
{
    @Override
    default void generate(NodeLIRBuilderTool __builder)
    {
        generate(__builder, __builder.getLIRGeneratorTool().getArithmetic());
    }

    void generate(NodeLIRBuilderTool __builder, ArithmeticLIRGeneratorTool __gen);
}
