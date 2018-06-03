package giraaff.lir.asm;

import jdk.vm.ci.code.CodeCacheProvider;

import giraaff.asm.Assembler;
import giraaff.code.CompilationResult;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.lir.framemap.FrameMap;

/**
 * Factory class for creating {@link CompilationResultBuilder}s.
 */
// @iface CompilationResultBuilderFactory
public interface CompilationResultBuilderFactory
{
    /**
     * Creates a new {@link CompilationResultBuilder}.
     */
    CompilationResultBuilder createBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder, FrameContext frameContext, CompilationResult compilationResult);

    /**
     * The default factory creates a standard {@link CompilationResultBuilder}.
     */
    // @closure
    CompilationResultBuilderFactory DEFAULT = new CompilationResultBuilderFactory()
    {
        @Override
        public CompilationResultBuilder createBuilder(CodeCacheProvider __codeCache, ForeignCallsProvider __foreignCalls, FrameMap __frameMap, Assembler __asm, DataBuilder __dataBuilder, FrameContext __frameContext, CompilationResult __compilationResult)
        {
            return new CompilationResultBuilder(__codeCache, __foreignCalls, __frameMap, __asm, __dataBuilder, __frameContext, __compilationResult);
        }
    };
}
