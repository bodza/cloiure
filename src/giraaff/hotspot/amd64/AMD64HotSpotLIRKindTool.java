package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64Kind;

import giraaff.core.amd64.AMD64LIRKindTool;
import giraaff.core.common.LIRKind;

// @class AMD64HotSpotLIRKindTool
public final class AMD64HotSpotLIRKindTool extends AMD64LIRKindTool
{
    // @cons
    public AMD64HotSpotLIRKindTool()
    {
        super();
    }

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
