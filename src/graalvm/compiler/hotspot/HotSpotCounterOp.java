package graalvm.compiler.hotspot;

import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.asJavaConstant;
import static graalvm.compiler.lir.LIRValueUtil.isJavaConstant;

import java.util.Arrays;

import org.graalvm.collections.EconomicMap;
import graalvm.compiler.asm.Assembler;
import graalvm.compiler.core.common.NumUtil;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.debug.BenchmarkCounters;
import graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstructionClass;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

public abstract class HotSpotCounterOp extends LIRInstruction {
    public static final LIRInstructionClass<HotSpotCounterOp> TYPE = LIRInstructionClass.create(HotSpotCounterOp.class);

    private final String[] names;
    private final String[] groups;
    protected final Register thread;
    protected final GraalHotSpotVMConfig config;
    @Alive({OperandFlag.CONST, OperandFlag.REG}) protected Value[] increments;

    public HotSpotCounterOp(LIRInstructionClass<? extends HotSpotCounterOp> c, String name, String group, Value increment, HotSpotRegistersProvider registers, GraalHotSpotVMConfig config) {
        this(c, new String[]{name}, new String[]{group}, new Value[]{increment}, registers, config);
    }

    public HotSpotCounterOp(LIRInstructionClass<? extends HotSpotCounterOp> c, String[] names, String[] groups, Value[] increments, HotSpotRegistersProvider registers, GraalHotSpotVMConfig config) {
        super(c);

        assert names.length == groups.length;
        assert groups.length == increments.length;

        this.names = names;
        this.groups = groups;
        this.increments = increments;
        this.thread = registers.getThreadRegister();
        this.config = config;
    }

    protected static int getDisplacementForLongIndex(TargetDescription target, long index) {
        long finalDisp = index * target.arch.getPlatformKind(JavaKind.Long).getSizeInBytes();
        if (!NumUtil.isInt(finalDisp)) {
            throw GraalError.unimplemented("cannot deal with indices that big: " + index);
        }
        return (int) finalDisp;
    }

    protected interface CounterProcedure {
        /**
         * Lambda interface for iterating over counters declared in this op.
         *
         * @param counterIndex Index in this CounterOp object.
         * @param increment Value for increment
         * @param displacement Displacement in bytes in the counter array
         */
        void apply(int counterIndex, Value increment, int displacement);
    }

    /**
     * Calls the {@link CounterProcedure} for each counter in ascending order of their displacement
     * in the counter array.
     *
     * @param proc The procedure to be called
     * @param target Target architecture (used to calculate the array displacements)
     */
    protected void forEachCounter(CounterProcedure proc, TargetDescription target) {
        if (names.length == 1) { // fast path
            int arrayIndex = getIndex(names[0], groups[0], increments[0]);
            int displacement = getDisplacementForLongIndex(target, arrayIndex);
            proc.apply(0, increments[0], displacement);
        } else { // Slow path with sort by displacements ascending
            int[] displacements = new int[names.length];
            EconomicMap<Integer, Integer> offsetMap = EconomicMap.create();
            for (int i = 0; i < names.length; i++) {
                int arrayIndex = getIndex(names[i], groups[i], increments[i]);
                displacements[i] = getDisplacementForLongIndex(target, arrayIndex);
                offsetMap.put(displacements[i], i);
            }
            Arrays.sort(displacements);
            // Now apply in order
            for (int offset : displacements) {
                int idx = offsetMap.get(offset);
                proc.apply(idx, increments[idx], displacements[idx]);
            }
        }
    }

    protected int getIndex(String name, String group, Value increment) {
        if (isJavaConstant(increment)) {
            // get index for the counter
            return BenchmarkCounters.getIndexConstantIncrement(name, group, config, asLong(asJavaConstant(increment)));
        }
        assert isRegister(increment) : "Unexpected Value: " + increment;
        // get index for the counter
        return BenchmarkCounters.getIndex(name, group, config);
    }

    /**
     * Patches the increment value in the instruction emitted by this instruction. Use only, if
     * patching is needed after assembly.
     *
     * @param asm
     * @param increment
     */
    public void patchCounterIncrement(Assembler asm, int[] increment) {
        throw GraalError.unimplemented();
    }

    private static long asLong(JavaConstant value) {
        JavaKind kind = value.getJavaKind();
        switch (kind) {
            case Byte:
            case Short:
            case Char:
            case Int:
                return value.asInt();
            case Long:
                return value.asLong();
            default:
                throw new IllegalArgumentException("not an integer kind: " + kind);
        }
    }

    protected static int asInt(JavaConstant value) {
        long l = asLong(value);
        if (!NumUtil.isInt(l)) {
            throw GraalError.shouldNotReachHere("value does not fit into int: " + l);
        }
        return (int) l;
    }

    public String[] getNames() {
        return names;
    }

    public String[] getGroups() {
        return groups;
    }
}
