package graalvm.compiler.lir.dfa;

import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.asStackSlot;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.ValueConsumer;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.framemap.ReferenceMapBuilder;
import graalvm.compiler.lir.util.IndexedValueMap;
import graalvm.compiler.lir.util.ValueSet;

import jdk.vm.ci.meta.Value;

final class RegStackValueSet extends ValueSet<RegStackValueSet>
{
    private final FrameMap frameMap;
    private final IndexedValueMap registers;
    private final IndexedValueMap stack;
    private Set<Value> extraStack;

    RegStackValueSet(FrameMap frameMap)
    {
        this.frameMap = frameMap;
        registers = new IndexedValueMap();
        stack = new IndexedValueMap();
    }

    private RegStackValueSet(FrameMap frameMap, RegStackValueSet s)
    {
        this.frameMap = frameMap;
        registers = new IndexedValueMap(s.registers);
        stack = new IndexedValueMap(s.stack);
        if (s.extraStack != null)
        {
            extraStack = new HashSet<>(s.extraStack);
        }
    }

    @Override
    public RegStackValueSet copy()
    {
        return new RegStackValueSet(frameMap, this);
    }

    @Override
    public void put(Value v)
    {
        if (!shouldProcessValue(v))
        {
            return;
        }
        if (isRegister(v))
        {
            int index = asRegister(v).number;
            registers.put(index, v);
        }
        else if (isStackSlot(v))
        {
            int index = frameMap.offsetForStackSlot(asStackSlot(v));
            if (index % 4 == 0)
            {
                stack.put(index / 4, v);
            }
            else
            {
                if (extraStack == null)
                {
                    extraStack = new HashSet<>();
                }
                extraStack.add(v);
            }
        }
    }

    @Override
    public void putAll(RegStackValueSet v)
    {
        registers.putAll(v.registers);
        stack.putAll(v.stack);
        if (v.extraStack != null)
        {
            if (extraStack == null)
            {
                extraStack = new HashSet<>();
            }
            extraStack.addAll(v.extraStack);
        }
    }

    @Override
    public void remove(Value v)
    {
        if (!shouldProcessValue(v))
        {
            return;
        }
        if (isRegister(v))
        {
            int index = asRegister(v).number;
            registers.put(index, null);
        }
        else if (isStackSlot(v))
        {
            int index = frameMap.offsetForStackSlot(asStackSlot(v));
            if (index % 4 == 0)
            {
                stack.put(index / 4, null);
            }
            else if (extraStack != null)
            {
                extraStack.remove(v);
            }
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof RegStackValueSet)
        {
            RegStackValueSet other = (RegStackValueSet) obj;
            return registers.equals(other.registers) && stack.equals(other.stack) && Objects.equals(extraStack, other.extraStack);
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    private static boolean shouldProcessValue(Value v)
    {
        /*
         * We always process registers because we have to track the largest register size that is
         * alive across safepoints in order to save and restore them.
         */
        return isRegister(v) || !LIRKind.isValue(v);
    }

    public void addLiveValues(ReferenceMapBuilder refMap)
    {
        ValueConsumer addLiveValue = new ValueConsumer()
        {
            @Override
            public void visitValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                refMap.addLiveValue(value);
            }
        };
        registers.visitEach(null, null, null, addLiveValue);
        stack.visitEach(null, null, null, addLiveValue);
        if (extraStack != null)
        {
            for (Value v : extraStack)
            {
                refMap.addLiveValue(v);
            }
        }
    }
}
