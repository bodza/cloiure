package giraaff.core.common;

/**
 * This class encapsulates options that control the behavior of the Graal compiler.
 */
// @class GraalOptions
public final class GraalOptions
{
    // @Option "Use compiler intrinsifications."
    // @def
    public static final boolean intrinsify = true;

    // @Option "Inline calls with monomorphic type profile."
    // @def
    public static final boolean inlineMonomorphicCalls = true;

    // @Option "Inline calls with polymorphic type profile."
    // @def
    public static final boolean inlinePolymorphicCalls = true;

    // @Option "Inline calls with megamorphic type profile (i.e., not all types could be recorded)."
    // @def
    public static final boolean inlineMegamorphicCalls = true;

    // @Option "Maximum desired size of the compiler graph in nodes."
    // @def
    public static final int maximumDesiredSize = 20000;

    // @Option "Minimum probability for methods to be inlined for megamorphic type profiles."
    // @def
    public static final double megamorphicInliningMinMethodProbability = 0.33;

    // @Option "Maximum level of recursive inlining."
    // @def
    public static final int maximumRecursiveInlining = 5;

    // @Option "Graphs with less than this number of nodes are trivial and therefore always inlined."
    // @def
    public static final int trivialInliningSize = 10;

    // @Option "Inlining is explored up to this number of nodes in the graph for each call site."
    // @def
    public static final int maximumInliningSize = 300;

    // @Option "If the previous low-level graph size of the method exceeds the threshold, it is not inlined."
    // @def
    public static final int smallCompiledLowLevelGraphSize = 300;

    // @def
    public static final double limitInlinedInvokes = 5.0;

    // @def
    public static final boolean inlineEverything = false;

    // escape analysis settings
    // @def
    public static final boolean partialEscapeAnalysis = true;
    // @def
    public static final int escapeAnalysisIterations = 2;
    // @def
    public static final int escapeAnalysisLoopCutoff = 20;
    // @def
    public static final int maximumEscapeAnalysisArrayLength = 32;
    // @def
    public static final int deoptsToDisableOptimisticOptimization = 40;
    // @def
    public static final boolean loopPeeling = true;
    // @def
    public static final boolean reassociateInvariants = true;
    // @def
    public static final boolean fullUnroll = true;
    // @def
    public static final boolean loopUnswitch = true;
    // @def
    public static final boolean partialUnroll = true;
    // @def
    public static final float minimumPeelProbability = 0.35f;
    // @def
    public static final int loopMaxUnswitch = 3;
    // @def
    public static final boolean useLoopLimitChecks = true;

    // debugging settings
    // @def
    public static final boolean zapStackOnMethodEntry = false;
    // @def
    public static final boolean deoptALot = false;

    // @Option "Stress the code emitting explicit exception throwing code."
    // @def
    public static final boolean stressExplicitExceptionCode = false;

    // @Option "Stress the code emitting invokes with explicit exception edges."
    // @def
    public static final boolean stressInvokeWithExceptionNode = false;

    // @Option "Stress the code by emitting reads at earliest instead of latest point."
    // @def
    public static final boolean stressTestEarlyReads = false;

    // register allocator debugging
    // @def
    public static final boolean conditionalElimination = true;
    // @def
    public static final boolean rawConditionalElimination = true;
    // @def
    public static final boolean replaceInputsWithConstantsBasedOnStamps = true;
    // @def
    public static final boolean removeNeverExecutedCode = true;
    // @def
    public static final boolean useExceptionProbability = true;
    // @def
    public static final boolean omitHotExceptionStacktrace = false;
    // @def
    public static final boolean genLoopSafepoints = true;
    // @def
    public static final boolean useTypeCheckHints = true;
    // @def
    public static final boolean inlineVTableStubs = true;
    // @def
    public static final boolean alwaysInlineVTableStubs = false;
    // @def
    public static final boolean resolveClassBeforeStaticInvoke = false;
    // @def
    public static final boolean canOmitFrame = true;

    // runtime settings
    // @def
    public static final boolean supportJsrBytecodes = true;
    // @def
    public static final boolean optAssumptions = true;
    // @def
    public static final boolean optConvertDeoptsToGuards = true;
    // @def
    public static final boolean optReadElimination = true;
    // @def
    public static final int readEliminationMaxLoopVisits = 5;
    // @def
    public static final boolean optDeoptimizationGrouping = true;
    // @def
    public static final boolean optScheduleOutOfLoops = true;
    // @def
    public static final boolean guardPriorities = true;
    // @def
    public static final boolean optEliminateGuards = true;
    // @def
    public static final boolean optImplicitNullChecks = true;
    // @def
    public static final boolean optClearNonLiveLocals = true;
    // @def
    public static final boolean optLoopTransform = true;
    // @def
    public static final boolean optFloatingReads = true;
    // @def
    public static final boolean optDevirtualizeInvokesOptimistically = true;

    // @Option "Eagerly construct extra snippet info."
    // @def
    public static final boolean eagerSnippets = false;

    // @Option "Use a cache for snippet graphs."
    // @def
    public static final boolean useSnippetGraphCache = true;

    /**
     * @anno AMD64NodeLIRBuilder.Options
     */
    // @Option "AMD64: Emit lfence instructions at the beginning of basic blocks."
    // @def
    public static final boolean mitigateSpeculativeExecutionAttacks = false;

    /**
     * @anno JavaConstantFieldProvider.Options
     */
    // @Option "Determines whether to treat final fields with default values as constant."
    // @def
    public static final boolean trustFinalDefaultFields = true;

    /**
     * @anno HighTier.Options
     */
    // @Option "Enable inlining."
    // @def
    public static final boolean inline = true;

    /**
     * @anno Graph.Options
     */
    // @Option "Graal graph compression is performed when percent of live nodes falls below this value."
    // @def
    public static final int graphCompressionThreshold = 70;

    /**
     * @anno OnStackReplacementPhase.Options
     */
    // @Option "Deoptimize OSR compiled code when the OSR entry loop is finished if there is no mature profile available for the rest of the method."
    // @def
    public static final boolean deoptAfterOSR = true;

    // @Option "Support OSR compilations with locks. If DeoptAfterOSR is true we can per definition not have unbalaced enter/extis mappings. If DeoptAfterOSR is false insert artificial monitor enters after the OSRStart to have balanced enter/exits in the graph."
    // @def
    public static final boolean supportOSRWithLocks = true;

    /**
     * Options related to HotSpot snippets in this package.
     *
     * @anno HotspotSnippetsOptions
     */
    // @Option "If the probability that a type check will hit one the profiled types (up to TypeCheckMaxHints) is below this value, the type check will be compiled without profiling info."
    // @def
    public static final double typeCheckMinProfileHitProbability = 0.5;

    // @Option "The maximum number of profiled types that will be used when compiling a profiled type check. Note that TypeCheckMinProfileHitProbability also influences whether profiling info is used in compiled type checks."
    // @def
    public static final int typeCheckMaxHints = 2;

    // @Option "Use a VM runtime call to load and clear the exception object from the thread at the start of a compiled exception handler."
    // @def
    public static final boolean loadExceptionObjectInVM = false;

    // @Option "Handle simple cases for inflated monitors in the fast-path."
    // @def
    public static final boolean simpleFastInflatedLocking = true;

    /**
     * Options related to {@link BytecodeParser}.
     *
     * @anno BytecodeParserOptions
     */
    // @Option "Inlines trivial methods during bytecode parsing."
    // @def
    public static final boolean inlineDuringParsing = true;

    // @Option "Inlines partial intrinsic exits during bytecode parsing when possible. A partial intrinsic exit is a call within an intrinsic to the method being intrinsified and denotes semantics of the original method that the intrinsic does not support."
    // @def
    public static final boolean inlinePartialIntrinsicExitDuringParsing = true;

    // @Option "Inlines intrinsic methods during bytecode parsing."
    // @def
    public static final boolean inlineIntrinsicsDuringParsing = true;

    // @Option "Maximum depth when inlining during bytecode parsing."
    // @def
    public static final int inlineDuringParsingMaxDepth = 10;

    // @Option "Use intrinsics guarded by a virtual dispatch test at indirect call sites."
    // @def
    public static final boolean useGuardedIntrinsics = true;

    /**
     * @anno LIRPhase.Options
     */
    // @Option "Enable LIR level optimiztations."
    // @def
    public static final boolean lirOptimization = true;

    /**
     * @anno LinearScan.Options
     */
    // @Option "Enable spill position optimization."
    // @def
    public static final boolean lirOptLSRAOptimizeSpillPosition = lirOptimization && true;

    /**
     * @anno LinearScanEliminateSpillMovePhase.Options
     */
    // @Option "Enable spill move elimination."
    // @def
    public static final boolean lirOptLSRAEliminateSpillMoves = lirOptimization && true;

    /**
     * @anno OptimizingLinearScanWalker.Options
     */
    // @Option "Enable LSRA optimization."
    // @def
    public static final boolean lsraOptimization = false;
    // @Option "LSRA optimization: Only split but do not reassign."
    // @def
    public static final boolean lsraOptSplitOnly = false;

    /**
     * @anno StackMoveOptimizationPhase.Options
     */
    // @def
    public static final boolean lirOptStackMoveOptimizer = lirOptimization && true;

    /**
     * @anno ConstantLoadOptimization.Options
     */
    // @Option "Enable constant load optimization."
    // @def
    public static final boolean lirOptConstantLoadOptimization = lirOptimization && true;

    /**
     * @anno PostAllocationOptimizationStage.Options
     */
    // @def
    public static final boolean lirOptEdgeMoveOptimizer = lirOptimization && true;
    // @def
    public static final boolean lirOptControlFlowOptimizer = lirOptimization && true;
    // @def
    public static final boolean lirOptRedundantMoveElimination = lirOptimization && true;
    // @def
    public static final boolean lirOptNullCheckOptimizer = lirOptimization && true;

    /**
     * @anno LSStackSlotAllocator.Options
     */
    // @Option "Use linear scan stack slot allocation."
    // @def
    public static final boolean lirOptLSStackSlotAllocator = lirOptimization && true;

    /**
     * @anno DefaultLoopPolicies.Options
     */
    // @def
    public static final int loopUnswitchMaxIncrease = 500;
    // @def
    public static final int loopUnswitchTrivial = 10;
    // @def
    public static final double loopUnswitchFrequencyBoost = 10.0;

    // @def
    public static final int fullUnrollMaxNodes = 300;
    // @def
    public static final int fullUnrollMaxIterations = 600;
    // @def
    public static final int exactFullUnrollMaxNodes = 1200;
    // @def
    public static final int exactPartialUnrollMaxNodes = 200;

    // @def
    public static final int unrollMaxIterations = 16;

    /**
     * @anno DeadCodeEliminationPhase.Options
     */
    // @Option "Disable optional dead code eliminations."
    // @def
    public static final boolean reduceDCE = true;

    /**
     * @anno InliningPhase.Options
     */
    // @Option "Unconditionally inline intrinsics."
    // @def
    public static final boolean alwaysInlineIntrinsics = false;

    /**
     * This is a defensive measure against known pathologies of the inliner where the breadth of
     * the inlining call tree exploration can be wide enough to prevent inlining from completing
     * in reasonable time.
     */
    // @Option "Per-compilation method inlining exploration limit before giving up (use 0 to disable)."
    // @def
    public static final int methodInlineBailoutLimit = 5000;

    /**
     * @anno UseTrappingNullChecksPhase.Options
     */
    // @Option "Use traps for null checks instead of explicit null-checks."
    // @def
    public static final boolean useTrappingNullChecks = true;

    /**
     * @anno SnippetTemplate.Options
     */
    // @Option "Use a LRU cache for snippet templates."
    // @def
    public static final boolean useSnippetTemplateCache = true;

    // @def
    public static final int maxTemplatesPerSnippet = 50;

    /**
     * @anno PartialEscapePhase.Options
     */
    // @def
    public static final boolean optEarlyReadElimination = true;
}
