package graalvm.compiler.truffle.runtime.hotspot;

import java.util.function.Supplier;

import graalvm.compiler.api.runtime.GraalJVMCICompiler;
import graalvm.compiler.api.runtime.GraalRuntime;
import graalvm.compiler.hotspot.CompilerConfigurationFactory;
import graalvm.compiler.hotspot.HotSpotGraalCompilerFactory;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.serviceprovider.ServiceProvider;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;

import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.TruffleRuntimeAccess;
import com.oracle.truffle.api.impl.DefaultTruffleRuntime;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.hotspot.HotSpotVMConfigAccess;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCICompiler;
import jdk.vm.ci.services.Services;

@ServiceProvider(TruffleRuntimeAccess.class)
public class HotSpotTruffleRuntimeAccess implements TruffleRuntimeAccess {

    static class Options {
        // @formatter:off
        @Option(help = "Select a Graal compiler configuration for Truffle compilation (default: use Graal system compiler configuration).")
        public static final OptionKey<String> TruffleCompilerConfiguration = new OptionKey<>(null);
        // @formatter:on
    }

    @Override
    public TruffleRuntime getRuntime() {
        // initialize JVMCI to make sure the TruffleCompiler option is parsed
        Services.initializeJVMCI();

        HotSpotJVMCIRuntimeProvider hsRuntime = (HotSpotJVMCIRuntimeProvider) JVMCI.getRuntime();
        HotSpotVMConfigAccess config = new HotSpotVMConfigAccess(hsRuntime.getConfigStore());
        boolean useCompiler = config.getFlag("UseCompiler", Boolean.class);
        if (!useCompiler) {
            // This happens, for example, when -Xint is given on the command line
            return new DefaultTruffleRuntime();
        }
        return new HotSpotTruffleRuntime(new LazyGraalRuntime());
    }

    /**
     * A supplier of a {@link GraalRuntime} that retrieves the runtime in a synchronized block on
     * first request and caches it for subsequent requests. This allows delaying initialization of a
     * {@link GraalRuntime} until the first Truffle compilation.
     */
    private static final class LazyGraalRuntime implements Supplier<GraalRuntime> {

        private volatile GraalRuntime graalRuntime;

        @Override
        public GraalRuntime get() {
            if (graalRuntime == null) {
                synchronized (this) {
                    if (graalRuntime == null) {
                        graalRuntime = getCompiler().getGraalRuntime();
                    }
                }
            }
            return graalRuntime;
        }

        private static GraalJVMCICompiler getCompiler() {
            OptionValues options = TruffleCompilerOptions.getOptions();
            if (!Options.TruffleCompilerConfiguration.hasBeenSet(options)) {
                JVMCICompiler compiler = JVMCI.getRuntime().getCompiler();
                if (compiler instanceof GraalJVMCICompiler) {
                    return (GraalJVMCICompiler) compiler;
                }
            }
            CompilerConfigurationFactory compilerConfigurationFactory = CompilerConfigurationFactory.selectFactory(Options.TruffleCompilerConfiguration.getValue(options), options);
            return HotSpotGraalCompilerFactory.createCompiler("Truffle", JVMCI.getRuntime(), options, compilerConfigurationFactory);
        }
    }
}
