package giraaff.lir.framemap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.LIRKind;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.util.GraalError;

/**
 * A FrameMapBuilder that records allocation.
 */
// @class FrameMapBuilderImpl
public class FrameMapBuilderImpl extends FrameMapBuilderTool
{
    // @field
    private final RegisterConfig registerConfig;
    // @field
    private final CodeCacheProvider codeCache;
    // @field
    private final FrameMap frameMap;
    // @field
    private final List<VirtualStackSlot> stackSlots;
    // @field
    private final List<CallingConvention> calls;
    // @field
    private int numStackSlots;

    // @cons
    public FrameMapBuilderImpl(FrameMap __frameMap, CodeCacheProvider __codeCache, RegisterConfig __registerConfig)
    {
        super();
        this.registerConfig = __registerConfig == null ? __codeCache.getRegisterConfig() : __registerConfig;
        this.codeCache = __codeCache;
        this.frameMap = __frameMap;
        this.stackSlots = new ArrayList<>();
        this.calls = new ArrayList<>();
        this.numStackSlots = 0;
    }

    @Override
    public VirtualStackSlot allocateSpillSlot(ValueKind<?> __kind)
    {
        SimpleVirtualStackSlot __slot = new SimpleVirtualStackSlot(numStackSlots++, __kind);
        stackSlots.add(__slot);
        return __slot;
    }

    @Override
    public VirtualStackSlot allocateStackSlots(int __slots, BitSet __objects, List<VirtualStackSlot> __outObjectStackSlots)
    {
        if (__slots == 0)
        {
            return null;
        }
        if (__outObjectStackSlots != null)
        {
            throw GraalError.unimplemented();
        }
        VirtualStackSlotRange __slot = new VirtualStackSlotRange(numStackSlots++, __slots, __objects, LIRKind.fromJavaKind(frameMap.getTarget().arch, JavaKind.Object));
        stackSlots.add(__slot);
        return __slot;
    }

    @Override
    public RegisterConfig getRegisterConfig()
    {
        return registerConfig;
    }

    @Override
    public CodeCacheProvider getCodeCache()
    {
        return codeCache;
    }

    @Override
    public FrameMap getFrameMap()
    {
        return frameMap;
    }

    @Override
    public int getNumberOfStackSlots()
    {
        return numStackSlots;
    }

    @Override
    public void callsMethod(CallingConvention __cc)
    {
        calls.add(__cc);
    }

    @Override
    public FrameMap buildFrameMap(LIRGenerationResult __res)
    {
        for (CallingConvention __cc : calls)
        {
            frameMap.callsMethod(__cc);
        }
        frameMap.finish();
        return frameMap;
    }

    @Override
    public List<VirtualStackSlot> getStackSlots()
    {
        return stackSlots;
    }
}
