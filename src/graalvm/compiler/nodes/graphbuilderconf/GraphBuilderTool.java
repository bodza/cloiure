package graalvm.compiler.nodes.graphbuilderconf;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.StampProvider;
import graalvm.compiler.options.OptionValues;

/**
 * Used by a {@link GraphBuilderPlugin} to interface with an object that builds a graph.
 */
public interface GraphBuilderTool
{
    /**
     * Adds the given node to the graph and also adds recursively all referenced inputs.
     *
     * @param value the node to be added to the graph
     * @return either the node added or an equivalent node
     */
    <T extends ValueNode> T append(T value);

    StampProvider getStampProvider();

    MetaAccessProvider getMetaAccess();

    default Assumptions getAssumptions()
    {
        return getGraph().getAssumptions();
    }

    ConstantReflectionProvider getConstantReflection();

    ConstantFieldProvider getConstantFieldProvider();

    /**
     * Gets the graph being constructed.
     */
    StructuredGraph getGraph();

    default OptionValues getOptions()
    {
        return getGraph().getOptions();
    }

    /**
     * Determines if this parsing context is within the bytecode of an intrinsic or a method inlined
     * by an intrinsic.
     */
    boolean parsingIntrinsic();

    @SuppressWarnings("unused")
    default boolean canDeferPlugin(GeneratedInvocationPlugin plugin)
    {
        // By default generated plugins must be completely processed during parsing.
        return false;
    }
}
