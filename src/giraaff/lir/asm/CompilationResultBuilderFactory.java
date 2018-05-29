package giraaff.lir.asm;

import jdk.vm.ci.code.CodeCacheProvider;

import giraaff.asm.Assembler;
import giraaff.code.CompilationResult;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.lir.framemap.FrameMap;
import giraaff.options.OptionValues;

/**
 * Factory class for creating {@link CompilationResultBuilder}s.
 */
// @iface CompilationResultBuilderFactory
public interface CompilationResultBuilderFactory
{
    /**
     * Creates a new {@link CompilationResultBuilder}.
     */
    CompilationResultBuilder createBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder, FrameContext frameContext, OptionValues options, CompilationResult compilationResult);

    /**
     * The default factory creates a standard {@link CompilationResultBuilder}.
     */
    CompilationResultBuilderFactory Default = new CompilationResultBuilderFactory()
    {
        @Override
        public CompilationResultBuilder createBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder, FrameContext frameContext, OptionValues options, CompilationResult compilationResult)
        {
            return new CompilationResultBuilder(codeCache, foreignCalls, frameMap, asm, dataBuilder, frameContext, options, compilationResult);
        }
    };
}
