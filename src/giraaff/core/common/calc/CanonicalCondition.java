package giraaff.core.common.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.PrimitiveConstant;

// @enum CanonicalCondition
public enum CanonicalCondition
{
    EQ(Condition.EQ),
    LT(Condition.LT),
    BT(Condition.BT);

    // @field
    private final Condition ___condition;

    CanonicalCondition(Condition __condition)
    {
        this.___condition = __condition;
    }

    public Condition asCondition()
    {
        return this.___condition;
    }

    public boolean foldCondition(Constant __lt, Constant __rt, ConstantReflectionProvider __constantReflection)
    {
        return asCondition().foldCondition(__lt, __rt, __constantReflection);
    }

    public boolean foldCondition(PrimitiveConstant __lp, PrimitiveConstant __rp)
    {
        return asCondition().foldCondition(__lp, __rp);
    }
}
