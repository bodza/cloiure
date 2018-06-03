package giraaff.lir.ssa;

import java.util.ArrayList;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.StandardOp.BlockEndOp;
import giraaff.lir.StandardOp.JumpOp;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.ValueConsumer;

/**
 * Utilities for working with Static-Single-Assignment LIR form.
 *
 * <h2>Representation of <code>PHI</code>s</h2>
 *
 * There is no explicit <code>PHI</code> {@linkplain LIRInstruction}. Instead, they are implemented
 * as parallel copy that span across a control-flow edge.
 *
 * The variables introduced by <code>PHI</code>s of a specific {@linkplain AbstractBlockBase merge
 * block} are {@linkplain LabelOp#setIncomingValues attached} to the {@linkplain LabelOp} of the
 * block. The outgoing values from the predecessor are {@link JumpOp#getOutgoingValue input} to the
 * {@linkplain BlockEndOp} of the predecessor. Because there are no critical edges we know that the
 * {@link BlockEndOp} of the predecessor has to be a {@link JumpOp}.
 *
 * <h3>Example:</h3>
 *
 * <pre>
 * B0 -> B1
 *   ...
 *   v0|i = ...
 *   JUMP ~[v0|i, int[0|0x0]] destination: B0 -> B1
 * ________________________________________________
 *
 * B2 -> B1
 *   ...
 *   v1|i = ...
 *   v2|i = ...
 *   JUMP ~[v1|i, v2|i] destination: B2 -> B1
 * ________________________________________________
 *
 * B1 <- B0,B2
 *   [v3|i, v4|i] = LABEL
 *   ...
 * </pre>
 */
// @class SSAUtil
public final class SSAUtil
{
    // @iface SSAUtil.PhiValueVisitor
    public interface PhiValueVisitor
    {
        /**
         * @param phiIn the incoming value at the merge block
         * @param phiOut the outgoing value from the predecessor block
         */
        void visit(Value phiIn, Value phiOut);
    }

    /**
     * Visits each phi value pair of an edge, i.e. the outgoing value from the predecessor and the
     * incoming value to the merge block.
     */
    public static void forEachPhiValuePair(LIR __lir, AbstractBlockBase<?> __merge, AbstractBlockBase<?> __pred, PhiValueVisitor __visitor)
    {
        if (__merge.getPredecessorCount() < 2)
        {
            return;
        }

        JumpOp __jump = phiOut(__lir, __pred);
        LabelOp __label = phiIn(__lir, __merge);

        for (int __i = 0; __i < __label.getPhiSize(); __i++)
        {
            __visitor.visit(__label.getIncomingValue(__i), __jump.getOutgoingValue(__i));
        }
    }

    public static JumpOp phiOut(LIR __lir, AbstractBlockBase<?> __block)
    {
        ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);
        int __index = __instructions.size() - 1;
        LIRInstruction __op = __instructions.get(__index);
        return (JumpOp) __op;
    }

    public static JumpOp phiOutOrNull(LIR __lir, AbstractBlockBase<?> __block)
    {
        if (__block.getSuccessorCount() != 1)
        {
            return null;
        }
        return phiOut(__lir, __block);
    }

    public static int phiOutIndex(LIR __lir, AbstractBlockBase<?> __block)
    {
        ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);
        return __instructions.size() - 1;
    }

    public static LabelOp phiIn(LIR __lir, AbstractBlockBase<?> __block)
    {
        return (LabelOp) __lir.getLIRforBlock(__block).get(0);
    }

    public static void removePhiOut(LIR __lir, AbstractBlockBase<?> __block)
    {
        JumpOp __jump = phiOut(__lir, __block);
        __jump.clearOutgoingValues();
    }

    public static void removePhiIn(LIR __lir, AbstractBlockBase<?> __block)
    {
        LabelOp __label = phiIn(__lir, __block);
        __label.clearIncomingValues();
    }

    public static boolean isMerge(AbstractBlockBase<?> __block)
    {
        return __block.getPredecessorCount() > 1;
    }

    public static void forEachPhiRegisterHint(LIR __lir, AbstractBlockBase<?> __block, LabelOp __label, Value __targetValue, OperandMode __mode, ValueConsumer __valueConsumer)
    {
        if (!__label.isPhiIn())
        {
            return;
        }
        int __idx = indexOfValue(__label, __targetValue);

        for (AbstractBlockBase<?> __pred : __block.getPredecessors())
        {
            JumpOp __jump = phiOut(__lir, __pred);
            Value __sourceValue = __jump.getOutgoingValue(__idx);
            __valueConsumer.visitValue(__jump, __sourceValue, null, null);
        }
    }

    private static int indexOfValue(LabelOp __label, Value __value)
    {
        for (int __i = 0; __i < __label.getIncomingSize(); __i++)
        {
            if (__label.getIncomingValue(__i).equals(__value))
            {
                return __i;
            }
        }
        return -1;
    }

    public static int numPhiOut(LIR __lir, AbstractBlockBase<?> __block)
    {
        if (__block.getSuccessorCount() != 1)
        {
            // cannot be a phi_out block
            return 0;
        }
        return numPhiIn(__lir, __block.getSuccessors()[0]);
    }

    private static int numPhiIn(LIR __lir, AbstractBlockBase<?> __block)
    {
        if (!isMerge(__block))
        {
            return 0;
        }
        return phiIn(__lir, __block).getPhiSize();
    }
}
