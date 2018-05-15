package graalvm.compiler.lir.dfa;

import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.framemap.ReferenceMapBuilder;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterAttributes;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;

/**
 * Mark all live references for a frame state. The frame state use this information to build the OOP
 * maps.
 */
public final class LocationMarkerPhase extends AllocationPhase {

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context) {
        new Marker(lirGenRes.getLIR(), lirGenRes.getFrameMap()).build();
    }

    static final class Marker extends LocationMarker<RegStackValueSet> {

        private final RegisterAttributes[] registerAttributes;

        private Marker(LIR lir, FrameMap frameMap) {
            super(lir, frameMap);
            this.registerAttributes = frameMap.getRegisterConfig().getAttributesMap();
        }

        @Override
        protected RegStackValueSet newLiveValueSet() {
            return new RegStackValueSet(frameMap);
        }

        @Override
        protected boolean shouldProcessValue(Value operand) {
            if (isRegister(operand)) {
                Register reg = asRegister(operand);
                if (!reg.mayContainReference() || !attributes(reg).isAllocatable()) {
                    // register that's not allocatable or not part of the reference map
                    return false;
                }
            } else if (!isStackSlot(operand)) {
                // neither register nor stack slot
                return false;
            }

            return !operand.getValueKind().equals(LIRKind.Illegal);
        }

        /**
         * This method does the actual marking.
         */
        @Override
        protected void processState(LIRInstruction op, LIRFrameState info, RegStackValueSet values) {
            if (!info.hasDebugInfo()) {
                info.initDebugInfo(frameMap, !op.destroysCallerSavedRegisters() || !frameMap.getRegisterConfig().areAllAllocatableRegistersCallerSaved());
            }

            ReferenceMapBuilder refMap = frameMap.newReferenceMapBuilder();
            frameMap.addLiveValues(refMap);
            values.addLiveValues(refMap);

            info.debugInfo().setReferenceMap(refMap.finish(info));
        }

        /**
         * Gets an object describing the attributes of a given register according to this register
         * configuration.
         */
        private RegisterAttributes attributes(Register reg) {
            return registerAttributes[reg.number];
        }

    }
}
