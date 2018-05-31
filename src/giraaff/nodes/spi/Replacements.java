package giraaff.nodes.spi;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.api.replacements.MethodSubstitution;
import giraaff.bytecode.Bytecode;
import giraaff.bytecode.BytecodeProvider;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;

/**
 * Interface for managing replacements.
 */
// @iface Replacements
public interface Replacements
{
    /**
     * Gets the object managing the various graph builder plugins used by this object when parsing
     * bytecode into a graph.
     */
    GraphBuilderConfiguration.Plugins getGraphBuilderPlugins();

    /**
     * Gets the snippet graph derived from a given method.
     *
     * @param args arguments to the snippet if available, otherwise {@code null}
     * @return the snippet graph, if any, that is derived from {@code method}
     */
    StructuredGraph getSnippet(ResolvedJavaMethod method, Object[] args);

    /**
     * Gets the snippet graph derived from a given method.
     *
     * @param recursiveEntry if the snippet contains a call to this method, it's considered as
     *            recursive call and won't be processed for {@linkplain MethodSubstitution substitutions}.
     * @param args arguments to the snippet if available, otherwise {@code null}
     * @return the snippet graph, if any, that is derived from {@code method}
     */
    StructuredGraph getSnippet(ResolvedJavaMethod method, ResolvedJavaMethod recursiveEntry, Object[] args);

    /**
     * Gets a graph that is a substitution for a given method.
     *
     * @param invokeBci the call site BCI if this request is made for inlining a substitute
     *            otherwise {@code -1}
     * @return the graph, if any, that is a substitution for {@code method}
     */
    StructuredGraph getSubstitution(ResolvedJavaMethod method, int invokeBci);

    /**
     * Gets the substitute bytecode for a given method.
     *
     * @return the bytecode to substitute for {@code method} or {@code null} if there is no
     *         substitute bytecode for {@code method}
     */
    Bytecode getSubstitutionBytecode(ResolvedJavaMethod method);

    /**
     * Determines if there may be a
     * {@linkplain #getSubstitution(ResolvedJavaMethod, int)
     * substitution graph} for a given method.
     *
     * A call to {@link #getSubstitution} may still return {@code null} for {@code method} and
     * {@code invokeBci}. A substitution may be based on an {@link InvocationPlugin} that returns
     * {@code false} for {@link InvocationPlugin#execute} making it impossible to create a
     * substitute graph.
     *
     * @param invokeBci the call site BCI if this request is made for inlining a substitute
     *            otherwise {@code -1}
     * @return true iff there may be a substitution graph available for {@code method}
     */
    boolean hasSubstitution(ResolvedJavaMethod method, int invokeBci);

    /**
     * Gets the provider for accessing the bytecode of a substitution method if no other provider is
     * associated with the substitution method.
     */
    BytecodeProvider getDefaultReplacementBytecodeProvider();
}
