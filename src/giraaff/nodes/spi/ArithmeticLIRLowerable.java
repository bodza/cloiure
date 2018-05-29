package giraaff.nodes.spi;

import giraaff.lir.gen.ArithmeticLIRGeneratorTool;

// @iface ArithmeticLIRLowerable
public interface ArithmeticLIRLowerable extends LIRLowerable
{
    @Override
    default void generate(NodeLIRBuilderTool builder)
    {
        generate(builder, builder.getLIRGeneratorTool().getArithmetic());
    }

    void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen);
}
