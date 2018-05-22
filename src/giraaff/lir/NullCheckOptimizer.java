package giraaff.lir;

import java.util.ArrayList;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.StandardOp.ImplicitNullCheck;
import giraaff.lir.StandardOp.NullCheck;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.PostAllocationOptimizationPhase;

public final class NullCheckOptimizer extends PostAllocationOptimizationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PostAllocationOptimizationContext context)
    {
        LIR ir = lirGenRes.getLIR();
        AbstractBlockBase<?>[] blocks = ir.codeEmittingOrder();
        NullCheckOptimizer.foldNullChecks(ir, blocks, target.implicitNullCheckLimit);
    }

    private static void foldNullChecks(LIR ir, AbstractBlockBase<?>[] blocks, int implicitNullCheckLimit)
    {
        for (AbstractBlockBase<?> block : blocks)
        {
            if (block == null)
            {
                continue;
            }
            ArrayList<LIRInstruction> list = ir.getLIRforBlock(block);

            if (!list.isEmpty())
            {
                LIRInstruction lastInstruction = list.get(0);
                for (int i = 0; i < list.size(); i++)
                {
                    LIRInstruction instruction = list.get(i);

                    if (instruction instanceof ImplicitNullCheck && lastInstruction instanceof NullCheck)
                    {
                        NullCheck nullCheck = (NullCheck) lastInstruction;
                        ImplicitNullCheck implicitNullCheck = (ImplicitNullCheck) instruction;
                        if (implicitNullCheck.makeNullCheckFor(nullCheck.getCheckedValue(), nullCheck.getState(), implicitNullCheckLimit))
                        {
                            list.remove(i - 1);
                            if (i < list.size())
                            {
                                instruction = list.get(i);
                            }
                        }
                    }
                    lastInstruction = instruction;
                }
            }
        }
    }
}
