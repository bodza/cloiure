package graalvm.compiler.truffle.compiler.hotspot;

import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.asm.CompilationResultBuilderFactory;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompilerRuntime;

import jdk.vm.ci.meta.MetaAccessProvider;

/**
 * A service for creating a specialized {@link CompilationResultBuilder} used to inject code into
 * the beginning of a {@linkplain HotSpotTruffleCompilerRuntime#getTruffleCallBoundaryMethods() call
 * boundary method}. The injected code tests the {@code entryPoint} field of the
 * {@code installedCode} field of the receiver and tail calls it if it is non-zero:
 *
 * <pre>
 * long ep = this.installedCode.entryPoint;
 * // post-volatile-read barrier
 * if (ep != null) {
 *     tailcall(ep);
 * }
 * // normal compiled code
 * </pre>
 */
public abstract class TruffleCallBoundaryInstrumentationFactory implements CompilationResultBuilderFactory {

    protected MetaAccessProvider metaAccess;
    protected GraalHotSpotVMConfig config;
    protected HotSpotRegistersProvider registers;

    @SuppressWarnings("hiding")
    public final void init(MetaAccessProvider metaAccess, GraalHotSpotVMConfig config, HotSpotRegistersProvider registers) {
        this.metaAccess = metaAccess;
        this.config = config;
        this.registers = registers;
    }

    /**
     * Gets the architecture supported by this factory.
     */
    public abstract String getArchitecture();
}
