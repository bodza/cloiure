package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.StackSlot;

import giraaff.core.common.LIRKind;
import giraaff.core.common.NumUtil;
import giraaff.lir.framemap.FrameMap;

///
// AMD64 specific frame map.
//
// This is the format of an AMD64 stack frame:
//
// <pre>
//   Base       Contents
//
//            :                                :  -----
//   caller   | incoming overflow argument n   |    ^
//   frame    :     ...                        :    | positive
//            | incoming overflow argument 0   |    | offsets
//   ---------+--------------------------------+---------------------
//            | return address                 |    |            ^
//   current  +--------------------------------+    |            |    -----
//   frame    |                                |    |            |      ^
//            : callee save area               :    |            |      |
//            |                                |    |            |      |
//            +--------------------------------+    |            |      |
//            | spill slot 0                   |    | negative   |      |
//            :     ...                        :    v offsets    |      |
//            | spill slot n                   |  -----        total  frame
//            +--------------------------------+               frame  size
//            | alignment padding              |               size     |
//            +--------------------------------+  -----          |      |
//            | outgoing overflow argument n   |    ^            |      |
//            :     ...                        :    | positive   |      |
//            | outgoing overflow argument 0   |    | offsets    v      v
//    %sp--&gt;  +--------------------------------+---------------------------
//
// </pre>
//
// The spill slot area also includes stack allocated memory blocks (ALLOCA blocks). The size of such
// a block may be greater than the size of a normal spill slot or the word size.
//
// A runtime can reserve space at the beginning of the overflow argument area. The calling
// convention can specify that the first overflow stack argument is not at offset 0, but at a
// specified offset. Use {@link CodeCacheProvider#getMinimumOutgoingSize()} to make sure that
// call-free methods also have this space reserved. Then the VM can use the memory at offset 0
// relative to the stack pointer.
///
// @class AMD64FrameMap
public final class AMD64FrameMap extends FrameMap
{
    // @field
    private StackSlot ___rbpSpillSlot;

    // @cons
    public AMD64FrameMap(CodeCacheProvider __codeCache, RegisterConfig __registerConfig)
    {
        super(__codeCache, __registerConfig);
        // (negative) offset relative to sp + total frame size
        this.___initialSpillSize = returnAddressSize();
        this.___spillSize = this.___initialSpillSize;
    }

    @Override
    public int totalFrameSize()
    {
        return frameSize() + returnAddressSize();
    }

    @Override
    public int currentFrameSize()
    {
        return alignFrameSize(this.___outgoingSize + this.___spillSize - returnAddressSize());
    }

    @Override
    protected int alignFrameSize(int __size)
    {
        return NumUtil.roundUp(__size + returnAddressSize(), getTarget().stackAlignment) - returnAddressSize();
    }

    @Override
    public int offsetForStackSlot(StackSlot __slot)
    {
        return super.offsetForStackSlot(__slot);
    }

    ///
    // For non-leaf methods, RBP is preserved in the special stack slot required by the HotSpot
    // runtime for walking/inspecting frames of such methods.
    ///
    StackSlot allocateRBPSpillSlot()
    {
        this.___rbpSpillSlot = allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        return this.___rbpSpillSlot;
    }

    void freeRBPSpillSlot()
    {
        this.___spillSize = this.___initialSpillSize;
    }
}
