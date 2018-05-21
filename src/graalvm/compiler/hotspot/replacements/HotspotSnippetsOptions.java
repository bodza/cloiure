package graalvm.compiler.hotspot.replacements;

import graalvm.compiler.options.OptionKey;

/**
 * Options related to HotSpot snippets in this package.
 */
public class HotspotSnippetsOptions
{
    // Option "If the probability that a type check will hit one the profiled types (up to TypeCheckMaxHints) is below this value, the type check will be compiled without profiling info."
    public static final OptionKey<Double> TypeCheckMinProfileHitProbability = new OptionKey<>(0.5);

    // Option "The maximum number of profiled types that will be used when compiling a profiled type check. Note that TypeCheckMinProfileHitProbability also influences whether profiling info is used in compiled type checks."
    public static final OptionKey<Integer> TypeCheckMaxHints = new OptionKey<>(2);

    // Option "Use a VM runtime call to load and clear the exception object from the thread at the start of a compiled exception handler."
    public static final OptionKey<Boolean> LoadExceptionObjectInVM = new OptionKey<>(false);

    // Option "Handle simple cases for inflated monitors in the fast-path."
    public static final OptionKey<Boolean> SimpleFastInflatedLocking = new OptionKey<>(true);

    // Option "Trace monitor operations on objects whose type contains this substring."
    public static final OptionKey<String> TraceMonitorsTypeFilter = new OptionKey<>(null);

    // Option "Trace monitor operations in methods whose fully qualified name contains this substring."
    public static final OptionKey<String> TraceMonitorsMethodFilter = new OptionKey<>(null);

    // Option "Emit extra code to dynamically check monitor operations are balanced."
    public static final OptionKey<Boolean> VerifyBalancedMonitors = new OptionKey<>(false);
}
