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

///
// A FrameMapBuilder that records allocation.
///
// @class FrameMapBuilderImpl
public class FrameMapBuilderImpl extends FrameMapBuilderTool
{
    // @field
    private final RegisterConfig ___registerConfig;
    // @field
    private final CodeCacheProvider ___codeCache;
    // @field
    private final FrameMap ___frameMap;
    // @field
    private final List<VirtualStackSlot> ___stackSlots;
    // @field
    private final List<CallingConvention> ___calls;
    // @field
    private int ___numStackSlots;

    // @cons
    public FrameMapBuilderImpl(FrameMap __frameMap, CodeCacheProvider __codeCache, RegisterConfig __registerConfig)
    {
        super();
        this.___registerConfig = __registerConfig == null ? __codeCache.getRegisterConfig() : __registerConfig;
        this.___codeCache = __codeCache;
        this.___frameMap = __frameMap;
        this.___stackSlots = new ArrayList<>();
        this.___calls = new ArrayList<>();
        this.___numStackSlots = 0;
    }

    @Override
    public VirtualStackSlot allocateSpillSlot(ValueKind<?> __kind)
    {
        SimpleVirtualStackSlot __slot = new SimpleVirtualStackSlot(this.___numStackSlots++, __kind);
        this.___stackSlots.add(__slot);
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
        VirtualStackSlotRange __slot = new VirtualStackSlotRange(this.___numStackSlots++, __slots, __objects, LIRKind.fromJavaKind(this.___frameMap.getTarget().arch, JavaKind.Object));
        this.___stackSlots.add(__slot);
        return __slot;
    }

    @Override
    public RegisterConfig getRegisterConfig()
    {
        return this.___registerConfig;
    }

    @Override
    public CodeCacheProvider getCodeCache()
    {
        return this.___codeCache;
    }

    @Override
    public FrameMap getFrameMap()
    {
        return this.___frameMap;
    }

    @Override
    public int getNumberOfStackSlots()
    {
        return this.___numStackSlots;
    }

    @Override
    public void callsMethod(CallingConvention __cc)
    {
        this.___calls.add(__cc);
    }

    @Override
    public FrameMap buildFrameMap(LIRGenerationResult __res)
    {
        for (CallingConvention __cc : this.___calls)
        {
            this.___frameMap.callsMethod(__cc);
        }
        this.___frameMap.finish();
        return this.___frameMap;
    }

    @Override
    public List<VirtualStackSlot> getStackSlots()
    {
        return this.___stackSlots;
    }
}
