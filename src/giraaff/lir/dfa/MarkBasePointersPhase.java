package giraaff.lir.dfa;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.LIRKind;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.Variable;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.util.IndexedValueMap;
import giraaff.lir.util.ValueSet;

/**
 * Record all derived reference base pointers in a frame state.
 */
public final class MarkBasePointersPhase extends AllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        new Marker(lirGenRes.getLIR(), null).build();
    }

    private static final class Marker extends LocationMarker<Marker.BasePointersSet>
    {
        private final class BasePointersSet extends ValueSet<Marker.BasePointersSet>
        {
            private final IndexedValueMap variables;

            BasePointersSet()
            {
                variables = new IndexedValueMap();
            }

            private BasePointersSet(BasePointersSet s)
            {
                variables = new IndexedValueMap(s.variables);
            }

            @Override
            public Marker.BasePointersSet copy()
            {
                return new BasePointersSet(this);
            }

            @Override
            public void put(Value v)
            {
                Variable base = (Variable) v.getValueKind(LIRKind.class).getDerivedReferenceBase();
                variables.put(base.index, base);
            }

            @Override
            public void putAll(BasePointersSet v)
            {
                variables.putAll(v.variables);
            }

            @Override
            public void remove(Value v)
            {
                Variable base = (Variable) v.getValueKind(LIRKind.class).getDerivedReferenceBase();
                variables.put(base.index, null);
            }

            @Override
            public boolean equals(Object obj)
            {
                if (obj instanceof Marker.BasePointersSet)
                {
                    BasePointersSet other = (BasePointersSet) obj;
                    return variables.equals(other.variables);
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
        }

        private Marker(LIR lir, FrameMap frameMap)
        {
            super(lir, frameMap);
        }

        @Override
        protected Marker.BasePointersSet newLiveValueSet()
        {
            return new BasePointersSet();
        }

        @Override
        protected boolean shouldProcessValue(Value operand)
        {
            ValueKind<?> kind = operand.getValueKind();
            if (kind instanceof LIRKind)
            {
                return ((LIRKind) kind).isDerivedReference();
            }
            else
            {
                return false;
            }
        }

        @Override
        protected void processState(LIRInstruction op, LIRFrameState info, BasePointersSet values)
        {
            info.setLiveBasePointers(new IndexedValueMap(values.variables));
        }
    }
}
