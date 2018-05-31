package giraaff.graph.spi;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.graph.Node;

// @iface CanonicalizerTool
public interface CanonicalizerTool
{
    Assumptions getAssumptions();

    MetaAccessProvider getMetaAccess();

    ConstantReflectionProvider getConstantReflection();

    ConstantFieldProvider getConstantFieldProvider();

    boolean canonicalizeReads();

    /**
     * If this method returns false, not all {@link Node#usages() usages of a node} are yet
     * available. So a node must not be canonicalized base on, e.g. information returned from
     * {@link Node#hasNoUsages()}.
     */
    boolean allUsagesAvailable();

    /**
     * Indicates the smallest width for comparing an integer value on the target platform.
     * If this method returns null, then there is no known smallest compare width.
     */
    Integer smallestCompareWidth();
}
