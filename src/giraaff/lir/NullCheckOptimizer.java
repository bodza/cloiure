package giraaff.lir;

import java.util.ArrayList;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.StandardOp.ImplicitNullCheck;
import giraaff.lir.StandardOp.NullCheck;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.PostAllocationOptimizationPhase;

// @class NullCheckOptimizer
public final class NullCheckOptimizer extends PostAllocationOptimizationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, PostAllocationOptimizationContext __context)
    {
        LIR __ir = __lirGenRes.getLIR();
        AbstractBlockBase<?>[] __blocks = __ir.codeEmittingOrder();
        NullCheckOptimizer.foldNullChecks(__ir, __blocks, __target.implicitNullCheckLimit);
    }

    private static void foldNullChecks(LIR __ir, AbstractBlockBase<?>[] __blocks, int __implicitNullCheckLimit)
    {
        for (AbstractBlockBase<?> __block : __blocks)
        {
            if (__block == null)
            {
                continue;
            }
            ArrayList<LIRInstruction> __list = __ir.getLIRforBlock(__block);

            if (!__list.isEmpty())
            {
                LIRInstruction __lastInstruction = __list.get(0);
                for (int __i = 0; __i < __list.size(); __i++)
                {
                    LIRInstruction __instruction = __list.get(__i);

                    if (__instruction instanceof ImplicitNullCheck && __lastInstruction instanceof NullCheck)
                    {
                        NullCheck __nullCheck = (NullCheck) __lastInstruction;
                        ImplicitNullCheck __implicitNullCheck = (ImplicitNullCheck) __instruction;
                        if (__implicitNullCheck.makeNullCheckFor(__nullCheck.getCheckedValue(), __nullCheck.getState(), __implicitNullCheckLimit))
                        {
                            __list.remove(__i - 1);
                            if (__i < __list.size())
                            {
                                __instruction = __list.get(__i);
                            }
                        }
                    }
                    __lastInstruction = __instruction;
                }
            }
        }
    }
}
