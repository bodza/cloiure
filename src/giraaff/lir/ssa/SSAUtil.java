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
    public static void forEachPhiValuePair(LIR lir, AbstractBlockBase<?> merge, AbstractBlockBase<?> pred, PhiValueVisitor visitor)
    {
        if (merge.getPredecessorCount() < 2)
        {
            return;
        }

        JumpOp jump = phiOut(lir, pred);
        LabelOp label = phiIn(lir, merge);

        for (int i = 0; i < label.getPhiSize(); i++)
        {
            visitor.visit(label.getIncomingValue(i), jump.getOutgoingValue(i));
        }
    }

    public static JumpOp phiOut(LIR lir, AbstractBlockBase<?> block)
    {
        ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
        int index = instructions.size() - 1;
        LIRInstruction op = instructions.get(index);
        return (JumpOp) op;
    }

    public static JumpOp phiOutOrNull(LIR lir, AbstractBlockBase<?> block)
    {
        if (block.getSuccessorCount() != 1)
        {
            return null;
        }
        return phiOut(lir, block);
    }

    public static int phiOutIndex(LIR lir, AbstractBlockBase<?> block)
    {
        ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
        return instructions.size() - 1;
    }

    public static LabelOp phiIn(LIR lir, AbstractBlockBase<?> block)
    {
        return (LabelOp) lir.getLIRforBlock(block).get(0);
    }

    public static void removePhiOut(LIR lir, AbstractBlockBase<?> block)
    {
        JumpOp jump = phiOut(lir, block);
        jump.clearOutgoingValues();
    }

    public static void removePhiIn(LIR lir, AbstractBlockBase<?> block)
    {
        LabelOp label = phiIn(lir, block);
        label.clearIncomingValues();
    }

    public static boolean isMerge(AbstractBlockBase<?> block)
    {
        return block.getPredecessorCount() > 1;
    }

    public static void forEachPhiRegisterHint(LIR lir, AbstractBlockBase<?> block, LabelOp label, Value targetValue, OperandMode mode, ValueConsumer valueConsumer)
    {
        if (!label.isPhiIn())
        {
            return;
        }
        int idx = indexOfValue(label, targetValue);

        for (AbstractBlockBase<?> pred : block.getPredecessors())
        {
            JumpOp jump = phiOut(lir, pred);
            Value sourceValue = jump.getOutgoingValue(idx);
            valueConsumer.visitValue(jump, sourceValue, null, null);
        }
    }

    private static int indexOfValue(LabelOp label, Value value)
    {
        for (int i = 0; i < label.getIncomingSize(); i++)
        {
            if (label.getIncomingValue(i).equals(value))
            {
                return i;
            }
        }
        return -1;
    }

    public static int numPhiOut(LIR lir, AbstractBlockBase<?> block)
    {
        if (block.getSuccessorCount() != 1)
        {
            // cannot be a phi_out block
            return 0;
        }
        return numPhiIn(lir, block.getSuccessors()[0]);
    }

    private static int numPhiIn(LIR lir, AbstractBlockBase<?> block)
    {
        if (!isMerge(block))
        {
            return 0;
        }
        return phiIn(lir, block).getPhiSize();
    }
}
