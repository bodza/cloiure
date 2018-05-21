package graalvm.compiler.core.common;

import graalvm.compiler.options.OptionKey;

/**
 * This class encapsulates options that control the behavior of the Graal compiler.
 */
public final class GraalOptions
{
    // "Use compiler intrinsifications."
    public static final OptionKey<Boolean> Intrinsify = new OptionKey<>(true);

    // "Inline calls with monomorphic type profile."
    public static final OptionKey<Boolean> InlineMonomorphicCalls = new OptionKey<>(true);

    // "Inline calls with polymorphic type profile."
    public static final OptionKey<Boolean> InlinePolymorphicCalls = new OptionKey<>(true);

    // "Inline calls with megamorphic type profile (i.e., not all types could be recorded)."
    public static final OptionKey<Boolean> InlineMegamorphicCalls = new OptionKey<>(true);

    // "Maximum desired size of the compiler graph in nodes."
    public static final OptionKey<Integer> MaximumDesiredSize = new OptionKey<>(20000);

    // "Minimum probability for methods to be inlined for megamorphic type profiles."
    public static final OptionKey<Double> MegamorphicInliningMinMethodProbability = new OptionKey<>(0.33D);

    // "Maximum level of recursive inlining."
    public static final OptionKey<Integer> MaximumRecursiveInlining = new OptionKey<>(5);

    // "Graphs with less than this number of nodes are trivial and therefore always inlined."
    public static final OptionKey<Integer> TrivialInliningSize = new OptionKey<>(10);

    // "Inlining is explored up to this number of nodes in the graph for each call site."
    public static final OptionKey<Integer> MaximumInliningSize = new OptionKey<>(300);

    // "If the previous low-level graph size of the method exceeds the threshold, it is not inlined."
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

    // "Stress the code emitting explicit exception throwing code."
    public static final OptionKey<Boolean> StressExplicitExceptionCode = new OptionKey<>(false);

    // "Stress the code emitting invokes with explicit exception edges."
    public static final OptionKey<Boolean> StressInvokeWithExceptionNode = new OptionKey<>(false);

    // "Stress the code by emitting reads at earliest instead of latest point."
    public static final OptionKey<Boolean> StressTestEarlyReads = new OptionKey<>(false);

    // Debug settings
    public static final OptionKey<Integer> GCDebugStartCycle = new OptionKey<>(-1);

    // "Perform platform dependent validation of the Java heap at returns."
    public static final OptionKey<Boolean> VerifyHeapAtReturn = new OptionKey<>(false);

    // Register allocator debugging
    // "Comma separated list of registers that register allocation is limited to."
    public static final OptionKey<String> RegisterPressure = new OptionKey<>(null);

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

    // Ahead of time compilation
    // "Try to avoid emitting code where patching is required."
    public static final OptionKey<Boolean> ImmutableCode = new OptionKey<>(false);

    // "Generate position independent code."
    public static final OptionKey<Boolean> GeneratePIC = new OptionKey<>(false);

    public static final OptionKey<Boolean> CallArrayCopy = new OptionKey<>(true);

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

    // "Allow backend to match complex expressions."
    public static final OptionKey<Boolean> MatchExpressions = new OptionKey<>(true);

    // "Enable counters for various paths in snippets."
    public static final OptionKey<Boolean> SnippetCounters = new OptionKey<>(false);

    // "Eagerly construct extra snippet info."
    public static final OptionKey<Boolean> EagerSnippets = new OptionKey<>(false);

    // "Use a cache for snippet graphs."
    public static final OptionKey<Boolean> UseSnippetGraphCache = new OptionKey<>(true);

    // "Enable experimental Trace Register Allocation."
    public static final OptionKey<Boolean> TraceRA = new OptionKey<>(false);
}
