package giraaff.hotspot.replacements;

import giraaff.options.OptionKey;

/**
 * Options related to HotSpot snippets in this package.
 */
// @class HotspotSnippetsOptions
public final class HotspotSnippetsOptions
{
    // @Option "If the probability that a type check will hit one the profiled types (up to TypeCheckMaxHints) is below this value, the type check will be compiled without profiling info."
    public static final OptionKey<Double> TypeCheckMinProfileHitProbability = new OptionKey<>(0.5);

    // @Option "The maximum number of profiled types that will be used when compiling a profiled type check. Note that TypeCheckMinProfileHitProbability also influences whether profiling info is used in compiled type checks."
    public static final OptionKey<Integer> TypeCheckMaxHints = new OptionKey<>(2);

    // @Option "Use a VM runtime call to load and clear the exception object from the thread at the start of a compiled exception handler."
    public static final OptionKey<Boolean> LoadExceptionObjectInVM = new OptionKey<>(false);

    // @Option "Handle simple cases for inflated monitors in the fast-path."
    public static final OptionKey<Boolean> SimpleFastInflatedLocking = new OptionKey<>(true);

    // @cons
    private HotspotSnippetsOptions()
    {
        super();
    }
}
