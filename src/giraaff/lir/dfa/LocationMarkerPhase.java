package giraaff.lir.dfa;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterAttributes;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.LIRKind;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.framemap.ReferenceMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;

/**
 * Mark all live references for a frame state. The frame state use this information to build the OOP maps.
 */
public final class LocationMarkerPhase extends AllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        new Marker(lirGenRes.getLIR(), lirGenRes.getFrameMap()).build();
    }

    static final class Marker extends LocationMarker<RegStackValueSet>
    {
        private final RegisterAttributes[] registerAttributes;

        private Marker(LIR lir, FrameMap frameMap)
        {
            super(lir, frameMap);
            this.registerAttributes = frameMap.getRegisterConfig().getAttributesMap();
        }

        @Override
        protected RegStackValueSet newLiveValueSet()
        {
            return new RegStackValueSet(frameMap);
        }

        @Override
        protected boolean shouldProcessValue(Value operand)
        {
            if (ValueUtil.isRegister(operand))
            {
                Register reg = ValueUtil.asRegister(operand);
                if (!reg.mayContainReference() || !attributes(reg).isAllocatable())
                {
                    // register that's not allocatable or not part of the reference map
                    return false;
                }
            }
            else if (!ValueUtil.isStackSlot(operand))
            {
                // neither register nor stack slot
                return false;
            }

            return !operand.getValueKind().equals(LIRKind.Illegal);
        }

        /**
         * This method does the actual marking.
         */
        @Override
        protected void processState(LIRInstruction op, LIRFrameState info, RegStackValueSet values)
        {
            if (!info.hasDebugInfo())
            {
                info.initDebugInfo(frameMap, !op.destroysCallerSavedRegisters() || !frameMap.getRegisterConfig().areAllAllocatableRegistersCallerSaved());
            }

            ReferenceMapBuilder refMap = frameMap.newReferenceMapBuilder();
            frameMap.addLiveValues(refMap);
            values.addLiveValues(refMap);

            info.debugInfo().setReferenceMap(refMap.finish(info));
        }

        /**
         * Gets an object describing the attributes of a given register according to this register configuration.
         */
        private RegisterAttributes attributes(Register reg)
        {
            return registerAttributes[reg.number];
        }
    }
}
