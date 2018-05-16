package graalvm.compiler.hotspot.meta;

import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.code.DisassemblerProvider;
import graalvm.compiler.serviceprovider.ServiceProvider;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;

/**
 * HotSpot implementation of {@link DisassemblerProvider}.
 */
@ServiceProvider(DisassemblerProvider.class)
public class HotSpotDisassemblerProvider implements DisassemblerProvider
{
    @Override
    public String disassembleCompiledCode(CodeCacheProvider codeCache, CompilationResult compResult)
    {
        return null;
    }

    @Override
    public String disassembleInstalledCode(CodeCacheProvider codeCache, CompilationResult compResult, InstalledCode code)
    {
        return ((HotSpotCodeCacheProvider) codeCache).disassemble(code);
    }

    @Override
    public String getName()
    {
        return "hsdis";
    }
}
