package giraaff.nodes.graphbuilderconf;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.StampProvider;

///
// Used by a {@link GraphBuilderPlugin} to interface with an object that builds a graph.
///
// @iface GraphBuilderTool
public interface GraphBuilderTool
{
    ///
    // Adds the given node to the graph and also adds recursively all referenced inputs.
    //
    // @param value the node to be added to the graph
    // @return either the node added or an equivalent node
    ///
    <T extends ValueNode> T append(T __value);

    StampProvider getStampProvider();

    MetaAccessProvider getMetaAccess();

    default Assumptions getAssumptions()
    {
        return getGraph().getAssumptions();
    }

    ConstantReflectionProvider getConstantReflection();

    ConstantFieldProvider getConstantFieldProvider();

    ///
    // Gets the graph being constructed.
    ///
    StructuredGraph getGraph();

    ///
    // Determines if this parsing context is within the bytecode of an intrinsic or a method inlined
    // by an intrinsic.
    ///
    boolean parsingIntrinsic();

    @SuppressWarnings("unused")
    default boolean canDeferPlugin(GeneratedInvocationPlugin __plugin)
    {
        // By default generated plugins must be completely processed during parsing.
        return false;
    }
}
