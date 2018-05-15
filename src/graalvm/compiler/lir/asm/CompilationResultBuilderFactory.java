package graalvm.compiler.lir.asm;

import graalvm.compiler.asm.Assembler;
import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.CodeCacheProvider;

/**
 * Factory class for creating {@link CompilationResultBuilder}s.
 */
public interface CompilationResultBuilderFactory {

    /**
     * Creates a new {@link CompilationResultBuilder}.
     */
    CompilationResultBuilder createBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder, FrameContext frameContext,
                    OptionValues options, DebugContext debug, CompilationResult compilationResult);

    /**
     * The default factory creates a standard {@link CompilationResultBuilder}.
     */
    CompilationResultBuilderFactory Default = new CompilationResultBuilderFactory() {

        @Override
        public CompilationResultBuilder createBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder,
                        FrameContext frameContext, OptionValues options, DebugContext debug, CompilationResult compilationResult) {
            return new CompilationResultBuilder(codeCache, foreignCalls, frameMap, asm, dataBuilder, frameContext, options, debug, compilationResult);
        }
    };
}
