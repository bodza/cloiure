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
    private final Condition condition;

    CanonicalCondition(Condition __condition)
    {
        this.condition = __condition;
    }

    public Condition asCondition()
    {
        return condition;
    }

    public boolean foldCondition(Constant __lt, Constant __rt, ConstantReflectionProvider __constantReflection, boolean __unorderedIsTrue)
    {
        return asCondition().foldCondition(__lt, __rt, __constantReflection, __unorderedIsTrue);
    }

    public boolean foldCondition(PrimitiveConstant __lp, PrimitiveConstant __rp, boolean __unorderedIsTrue)
    {
        return asCondition().foldCondition(__lp, __rp, __unorderedIsTrue);
    }
}
