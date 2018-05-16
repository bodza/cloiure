package graalvm.compiler.nodes.spi;

public interface LIRLowerable
{
    void generate(NodeLIRBuilderTool generator);
}
