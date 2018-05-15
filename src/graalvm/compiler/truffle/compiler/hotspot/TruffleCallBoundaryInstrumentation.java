package graalvm.compiler.truffle.compiler.hotspot;

import static graalvm.compiler.truffle.common.TruffleCompilerRuntime.getRuntime;

import graalvm.compiler.asm.Assembler;
import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.asm.DataBuilder;
import graalvm.compiler.lir.asm.FrameContext;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompilerRuntime;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.site.Mark;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaField;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Mechanism for injecting special code into
 * {@linkplain HotSpotTruffleCompilerRuntime#getTruffleCallBoundaryMethods() call boundary methods}.
 */
public abstract class TruffleCallBoundaryInstrumentation extends CompilationResultBuilder {
    protected final GraalHotSpotVMConfig config;
    protected final HotSpotRegistersProvider registers;
    protected final MetaAccessProvider metaAccess;

    public TruffleCallBoundaryInstrumentation(MetaAccessProvider metaAccess, CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder,
                    FrameContext frameContext,
                    OptionValues options, DebugContext debug, CompilationResult compilationResult, GraalHotSpotVMConfig config, HotSpotRegistersProvider registers) {
        super(codeCache, foreignCalls, frameMap, asm, dataBuilder, frameContext, options, debug, compilationResult);
        this.metaAccess = metaAccess;
        this.config = config;
        this.registers = registers;
    }

    @Override
    public Mark recordMark(Object id) {
        Mark mark = super.recordMark(id);
        if ((int) id == config.MARKID_VERIFIED_ENTRY) {
            ResolvedJavaType optimizedCallTargetType = getRuntime().resolveType(metaAccess, "graalvm.compiler.truffle.runtime.hotspot.HotSpotOptimizedCallTarget");
            int installedCodeOffset = getFieldOffset("installedCode", optimizedCallTargetType);
            int entryPointOffset = getFieldOffset("entryPoint", metaAccess.lookupJavaType(InstalledCode.class));
            injectTailCallCode(installedCodeOffset, entryPointOffset);
        }
        return mark;
    }

    private static int getFieldOffset(String name, ResolvedJavaType declaringType) {
        for (ResolvedJavaField field : declaringType.getInstanceFields(false)) {
            if (field.getName().equals(name)) {
                return ((HotSpotResolvedJavaField) field).offset();
            }
        }
        throw new NoSuchFieldError(declaringType.toJavaName() + "." + name);
    }

    /**
     * Injects code into the verified entry point of that makes a tail-call to the target callee.
     *
     * @param entryPointOffset offset of the field {@code HotSpotOptimizedCallTarget.installedCode}
     * @param installedCodeOffset offset of the field {@code InstalledCode.entryPoint}
     */
    protected abstract void injectTailCallCode(int installedCodeOffset, int entryPointOffset);
}
