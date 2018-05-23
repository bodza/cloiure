package giraaff.core.common;

import giraaff.options.OptionKey;

/**
 * This class encapsulates options that control the behavior of the Graal compiler.
 */
public final class GraalOptions
{
    // Option "Use compiler intrinsifications."
    public static final OptionKey<Boolean> Intrinsify = new OptionKey<>(true);

    // Option "Inline calls with monomorphic type profile."
    public static final OptionKey<Boolean> InlineMonomorphicCalls = new OptionKey<>(true);

    // Option "Inline calls with polymorphic type profile."
    public static final OptionKey<Boolean> InlinePolymorphicCalls = new OptionKey<>(true);

    // Option "Inline calls with megamorphic type profile (i.e., not all types could be recorded)."
    public static final OptionKey<Boolean> InlineMegamorphicCalls = new OptionKey<>(true);

    // Option "Maximum desired size of the compiler graph in nodes."
    public static final OptionKey<Integer> MaximumDesiredSize = new OptionKey<>(20000);

    // Option "Minimum probability for methods to be inlined for megamorphic type profiles."
    public static final OptionKey<Double> MegamorphicInliningMinMethodProbability = new OptionKey<>(0.33D);

    // Option "Maximum level of recursive inlining."
    public static final OptionKey<Integer> MaximumRecursiveInlining = new OptionKey<>(5);

    // Option "Graphs with less than this number of nodes are trivial and therefore always inlined."
    public static final OptionKey<Integer> TrivialInliningSize = new OptionKey<>(10);

    // Option "Inlining is explored up to this number of nodes in the graph for each call site."
    public static final OptionKey<Integer> MaximumInliningSize = new OptionKey<>(300);

    // Option "If the previous low-level graph size of the method exceeds the threshold, it is not inlined."
    public static final OptionKey<Integer> SmallCompiledLowLevelGraphSize = new OptionKey<>(300);

    public static final OptionKey<Double> LimitInlinedInvokes = new OptionKey<>(5.0);

    public static final OptionKey<Boolean> InlineEverything = new OptionKey<>(false);

    // escape analysis settings
    public static final OptionKey<Boolean> PartialEscapeAnalysis = new OptionKey<>(true);
    public static final OptionKey<Integer> EscapeAnalysisIterations = new OptionKey<>(2);
    public static final OptionKey<Integer> EscapeAnalysisLoopCutoff = new OptionKey<>(20);
    public static final OptionKey<String> EscapeAnalyzeOnly = new OptionKey<>(null);
    public static final OptionKey<Integer> MaximumEscapeAnalysisArrayLength = new OptionKey<>(32);
    public static final OptionKey<Boolean> PEAInliningHints = new OptionKey<>(false);
    public static final OptionKey<Double> TailDuplicationProbability = new OptionKey<>(0.5);
    public static final OptionKey<Integer> TailDuplicationTrivialSize = new OptionKey<>(1);
    public static final OptionKey<Integer> DeoptsToDisableOptimisticOptimization = new OptionKey<>(40);
    public static final OptionKey<Boolean> LoopPeeling = new OptionKey<>(true);
    public static final OptionKey<Boolean> ReassociateInvariants = new OptionKey<>(true);
    public static final OptionKey<Boolean> FullUnroll = new OptionKey<>(true);
    public static final OptionKey<Boolean> LoopUnswitch = new OptionKey<>(true);
    public static final OptionKey<Boolean> PartialUnroll = new OptionKey<>(true);
    public static final OptionKey<Float> MinimumPeelProbability = new OptionKey<>(0.35f);
    public static final OptionKey<Integer> LoopMaxUnswitch = new OptionKey<>(3);
    public static final OptionKey<Boolean> UseLoopLimitChecks = new OptionKey<>(true);

    // debugging settings
    public static final OptionKey<Boolean> ZapStackOnMethodEntry = new OptionKey<>(false);
    public static final OptionKey<Boolean> DeoptALot = new OptionKey<>(false);

    // Option "Stress the code emitting explicit exception throwing code."
    public static final OptionKey<Boolean> StressExplicitExceptionCode = new OptionKey<>(false);

    // Option "Stress the code emitting invokes with explicit exception edges."
    public static final OptionKey<Boolean> StressInvokeWithExceptionNode = new OptionKey<>(false);

    // Option "Stress the code by emitting reads at earliest instead of latest point."
    public static final OptionKey<Boolean> StressTestEarlyReads = new OptionKey<>(false);

    // Register allocator debugging
    public static final OptionKey<Boolean> ConditionalElimination = new OptionKey<>(true);
    public static final OptionKey<Boolean> RawConditionalElimination = new OptionKey<>(true);
    public static final OptionKey<Boolean> ReplaceInputsWithConstantsBasedOnStamps = new OptionKey<>(true);
    public static final OptionKey<Boolean> RemoveNeverExecutedCode = new OptionKey<>(true);
    public static final OptionKey<Boolean> UseExceptionProbability = new OptionKey<>(true);
    public static final OptionKey<Boolean> OmitHotExceptionStacktrace = new OptionKey<>(false);
    public static final OptionKey<Boolean> GenSafepoints = new OptionKey<>(true);
    public static final OptionKey<Boolean> GenLoopSafepoints = new OptionKey<>(true);
    public static final OptionKey<Boolean> UseTypeCheckHints = new OptionKey<>(true);
    public static final OptionKey<Boolean> InlineVTableStubs = new OptionKey<>(true);
    public static final OptionKey<Boolean> AlwaysInlineVTableStubs = new OptionKey<>(false);
    public static final OptionKey<Boolean> ResolveClassBeforeStaticInvoke = new OptionKey<>(false);
    public static final OptionKey<Boolean> CanOmitFrame = new OptionKey<>(true);

    // Runtime settings
    public static final OptionKey<Boolean> SupportJsrBytecodes = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptAssumptions = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptConvertDeoptsToGuards = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptReadElimination = new OptionKey<>(true);
    public static final OptionKey<Integer> ReadEliminationMaxLoopVisits = new OptionKey<>(5);
    public static final OptionKey<Boolean> OptDeoptimizationGrouping = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptScheduleOutOfLoops = new OptionKey<>(true);
    public static final OptionKey<Boolean> GuardPriorities = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptEliminateGuards = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptImplicitNullChecks = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptClearNonLiveLocals = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptLoopTransform = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptFloatingReads = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptEliminatePartiallyRedundantGuards = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptFilterProfiledTypes = new OptionKey<>(true);
    public static final OptionKey<Boolean> OptDevirtualizeInvokesOptimistically = new OptionKey<>(true);

    // Option "Allow backend to match complex expressions."
    public static final OptionKey<Boolean> MatchExpressions = new OptionKey<>(true);

    // Option "Enable counters for various paths in snippets."
    public static final OptionKey<Boolean> SnippetCounters = new OptionKey<>(false);

    // Option "Eagerly construct extra snippet info."
    public static final OptionKey<Boolean> EagerSnippets = new OptionKey<>(false);

    // Option "Use a cache for snippet graphs."
    public static final OptionKey<Boolean> UseSnippetGraphCache = new OptionKey<>(true);
}
