package graalvm.compiler.truffle.runtime;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.Engine;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * Compiler options that can be configured per {@link Engine engine} instance.
 */
@Option.Group("compiler")
public final class PolyglotCompilerOptions {

    // @formatter:off

    // USER OPTIONS

    // EXPERT OPTIONS

    @Option(help = "Minimum number of invocations or loop iterations needed to compile a guest language root.",
                    category = OptionCategory.EXPERT)
    public static final OptionKey<Integer> CompilationThreshold = new OptionKey<>(1000);

    @Option(help = "Maximum number of milliseconds between the first call and beeing queued for compilation of a guest language root. " +
                    "If the maximum queue time is exceeded then the compilation is deferred, ie. the invocation counter is reset. " +
                    "The queuing is retried after the compilation threshold is reached again.",
                    category = OptionCategory.EXPERT)
    public static final OptionKey<Integer> QueueTimeThreshold = new OptionKey<>(50000);

    /*
     * TODO planned options:
     *
    @Option(help = "Enable automatic inlining of guest language roots.",
                    category = OptionCategory.EXPERT)
    public static final OptionKey<Boolean> InliningEnabled = new OptionKey<>(true);

    @Option(help = "Maximum number of inlined non-trivial AST nodes per compilation unit.",
                    category = OptionCategory.EXPERT)
    public static final OptionKey<Integer> InliningNodeBudget = new OptionKey<>(2250);

    @Option(help = "Maximum depth for recursive inlining.",
                    category = OptionCategory.EXPERT)
    public static final OptionKey<Integer> InliningRecursionDepth = new OptionKey<>(4);

    @Option(help = "Enable automatic duplication of compilation profiles (splitting).",
                    category = OptionCategory.EXPERT)
    public static final OptionKey<Boolean> SplittingEnabled = new OptionKey<>(true);

    @Option(help = "Enable automatic on-stack-replacement of loops.",
                    category = OptionCategory.EXPERT)
    public static final OptionKey<Boolean> OSREnabled = new OptionKey<>(true);

    // DEBUG OPTIONS

    @Option(help = "Trace compilation decisions to the standard output.",
                    category = OptionCategory.DEBUG)
    public static final OptionKey<Boolean> TraceCompilation = new OptionKey<>(false);

    @Option(help = "Trace inlining decisions to the standard output.",
                    category = OptionCategory.DEBUG)
    public static final OptionKey<Boolean> TraceInlining = new OptionKey<>(false);

    @Option(help = "Trace splitting decisions to the standard output.",
                    category = OptionCategory.DEBUG)
    public static final OptionKey<Boolean> TraceSplitting = new OptionKey<>(false);

    @Option(help = "Trace deoptimization of compilation units.",
                    category = OptionCategory.DEBUG)
    public static final OptionKey<Boolean> TraceDeoptimization = new OptionKey<>(false);
    */

    // @formatter:on

    private static final EconomicMap<OptionKey<?>, graalvm.compiler.options.OptionKey<?>> TRUFFLE_TO_GRAAL = EconomicMap.create(Equivalence.IDENTITY);
    static {
        initializePolyglotToGraalMapping();
    }

    private static void initializePolyglotToGraalMapping() {
        TRUFFLE_TO_GRAAL.put(CompilationThreshold, TruffleCompilerOptions.TruffleCompilationThreshold);
        TRUFFLE_TO_GRAAL.put(QueueTimeThreshold, TruffleCompilerOptions.TruffleTimeThreshold);
    }

    @SuppressWarnings("unchecked")
    static <T> graalvm.compiler.options.OptionKey<T> getGraalOption(OptionKey<T> polyglotKey) {
        return (graalvm.compiler.options.OptionKey<T>) TRUFFLE_TO_GRAAL.get(polyglotKey);
    }

    static OptionValues getPolyglotValues(RootNode root) {
        return OptimizedCallTarget.runtime().getTvmci().getCompilerOptionValues(root);
    }

    @SuppressWarnings("unchecked")
    static <T> T getValue(OptionValues polyglotValues, OptionKey<T> key) {
        if (polyglotValues != null && polyglotValues.hasBeenSet(key)) {
            return polyglotValues.get(key);
        } else {
            graalvm.compiler.options.OptionKey<?> graalOption = PolyglotCompilerOptions.getGraalOption(key);
            if (graalOption != null) {
                return (T) graalOption.getValue(TruffleCompilerOptions.getOptions());
            }
        }
        return key.getDefaultValue();
    }

    static <T> T getValue(RootNode rootNode, OptionKey<T> key) {
        OptionValues polyglotValues = OptimizedCallTarget.runtime().getTvmci().getCompilerOptionValues(rootNode);
        return getValue(polyglotValues, key);
    }

    static OptionDescriptors getDescriptors() {
        throw new UnsupportedOperationException("return new PolyglotCompilerOptionsOptionDescriptors();");
    }

}
