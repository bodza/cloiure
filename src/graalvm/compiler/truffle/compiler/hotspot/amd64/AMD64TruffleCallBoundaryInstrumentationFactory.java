package graalvm.compiler.truffle.compiler.hotspot.amd64;

import static jdk.vm.ci.hotspot.HotSpotCallingConventionType.JavaCall;

import graalvm.compiler.asm.Assembler;
import graalvm.compiler.asm.Label;
import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64Assembler.ConditionFlag;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.common.CompressEncoding;
import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.hotspot.amd64.AMD64HotSpotBackend;
import graalvm.compiler.lir.amd64.AMD64Move;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.asm.DataBuilder;
import graalvm.compiler.lir.asm.FrameContext;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.serviceprovider.ServiceProvider;
import graalvm.compiler.truffle.compiler.hotspot.TruffleCallBoundaryInstrumentation;
import graalvm.compiler.truffle.compiler.hotspot.TruffleCallBoundaryInstrumentationFactory;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaKind;

@ServiceProvider(TruffleCallBoundaryInstrumentationFactory.class)
public class AMD64TruffleCallBoundaryInstrumentationFactory extends TruffleCallBoundaryInstrumentationFactory {

    @Override
    public CompilationResultBuilder createBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder, FrameContext frameContext,
                    OptionValues options, DebugContext debug, CompilationResult compilationResult) {
        return new TruffleCallBoundaryInstrumentation(metaAccess, codeCache, foreignCalls, frameMap, asm, dataBuilder, frameContext, options, debug, compilationResult, config, registers) {
            @Override
            protected void injectTailCallCode(int installedCodeOffset, int entryPointOffset) {
                AMD64MacroAssembler masm = (AMD64MacroAssembler) this.asm;
                Register thisRegister = codeCache.getRegisterConfig().getCallingConventionRegisters(JavaCall, JavaKind.Object).get(0);
                Register spillRegister = AMD64.r10;
                Label doProlog = new Label();
                int pos = masm.position();

                if (config.useCompressedOops) {
                    // First instruction must be at least 5 bytes long to be safe for patching
                    masm.movl(spillRegister, new AMD64Address(thisRegister, installedCodeOffset), true);
                    assert masm.position() - pos >= AMD64HotSpotBackend.PATCHED_VERIFIED_ENTRY_POINT_INSTRUCTION_SIZE;
                    CompressEncoding encoding = config.getOopEncoding();
                    Register heapBaseRegister = AMD64Move.UncompressPointerOp.hasBase(options, encoding) ? registers.getHeapBaseRegister() : null;
                    AMD64Move.UncompressPointerOp.emitUncompressCode(masm, spillRegister, encoding.getShift(), heapBaseRegister, true);
                } else {
                    // First instruction must be at least 5 bytes long to be safe for patching
                    masm.movq(spillRegister, new AMD64Address(thisRegister, installedCodeOffset), true);
                    assert masm.position() - pos >= AMD64HotSpotBackend.PATCHED_VERIFIED_ENTRY_POINT_INSTRUCTION_SIZE;
                }
                masm.movq(spillRegister, new AMD64Address(spillRegister, entryPointOffset));
                masm.testq(spillRegister, spillRegister);
                masm.jcc(ConditionFlag.Equal, doProlog);
                masm.jmp(spillRegister);
                masm.bind(doProlog);
            }
        };
    }

    @Override
    public String getArchitecture() {
        return "AMD64";
    }
}
