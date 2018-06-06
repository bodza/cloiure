package giraaff.core.common.cfg;

import java.util.function.BiConsumer;

///
// A {@linkplain PrintableCFG printable} {@link DominatorOptimizationProblem}.
///
// @class PrintableDominatorOptimizationProblem
public abstract class PrintableDominatorOptimizationProblem<E extends Enum<E>, C extends PropertyConsumable> extends DominatorOptimizationProblem<E, C> implements PrintableCFG
{
    // @cons PrintableDominatorOptimizationProblem
    protected PrintableDominatorOptimizationProblem(Class<E> __keyType, AbstractControlFlowGraph<?> __cfg)
    {
        super(__keyType, __cfg);
    }

    @Override
    public void forEachPropertyPair(AbstractBlockBase<?> __block, BiConsumer<String, String> __action)
    {
        // for each flag
        getFlags().forEach(__flag -> ((BiConsumer<String, Boolean>) (__name, __value) -> __action.accept(__name, __value ? "true" : "false")).accept(getName(__flag), get(__flag, __block)));
        // for each property
        C __cost = getCost(__block);
        if (__cost != null)
        {
            __cost.forEachProperty((__name, __value) -> __action.accept(__name, __value));
        }
    }
}
