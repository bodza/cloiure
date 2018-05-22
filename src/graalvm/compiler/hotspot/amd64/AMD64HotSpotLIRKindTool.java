package graalvm.compiler.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64Kind;

import graalvm.compiler.core.amd64.AMD64LIRKindTool;
import graalvm.compiler.core.common.LIRKind;

public class AMD64HotSpotLIRKindTool extends AMD64LIRKindTool
{
    @Override
    public LIRKind getNarrowOopKind()
    {
        return LIRKind.compressedReference(AMD64Kind.DWORD);
    }

    @Override
    public LIRKind getNarrowPointerKind()
    {
        return LIRKind.value(AMD64Kind.DWORD);
    }
}
