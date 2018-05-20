package graalvm.compiler.lir.framemap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.gen.LIRGenerationResult;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ValueKind;

/**
 * A FrameMapBuilder that records allocation.
 */
public class FrameMapBuilderImpl extends FrameMapBuilderTool
{
    private final RegisterConfig registerConfig;
    private final CodeCacheProvider codeCache;
    private final FrameMap frameMap;
    private final List<VirtualStackSlot> stackSlots;
    private final List<CallingConvention> calls;
    private int numStackSlots;

    public FrameMapBuilderImpl(FrameMap frameMap, CodeCacheProvider codeCache, RegisterConfig registerConfig)
    {
        this.registerConfig = registerConfig == null ? codeCache.getRegisterConfig() : registerConfig;
        this.codeCache = codeCache;
        this.frameMap = frameMap;
        this.stackSlots = new ArrayList<>();
        this.calls = new ArrayList<>();
        this.numStackSlots = 0;
    }

    @Override
    public VirtualStackSlot allocateSpillSlot(ValueKind<?> kind)
    {
        SimpleVirtualStackSlot slot = new SimpleVirtualStackSlot(numStackSlots++, kind);
        stackSlots.add(slot);
        return slot;
    }

    @Override
    public VirtualStackSlot allocateStackSlots(int slots, BitSet objects, List<VirtualStackSlot> outObjectStackSlots)
    {
        if (slots == 0)
        {
            return null;
        }
        if (outObjectStackSlots != null)
        {
            throw GraalError.unimplemented();
        }
        VirtualStackSlotRange slot = new VirtualStackSlotRange(numStackSlots++, slots, objects, LIRKind.fromJavaKind(frameMap.getTarget().arch, JavaKind.Object));
        stackSlots.add(slot);
        return slot;
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
    public void callsMethod(CallingConvention cc)
    {
        calls.add(cc);
    }

    @Override
    public FrameMap buildFrameMap(LIRGenerationResult res)
    {
        for (CallingConvention cc : calls)
        {
            frameMap.callsMethod(cc);
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
