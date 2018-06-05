package giraaff.nodes.calc;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.TriState;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.extended.LoadMethodNode;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;

// @class PointerEqualsNode
public class PointerEqualsNode extends CompareNode implements BinaryCommutative<ValueNode>
{
    // @def
    public static final NodeClass<PointerEqualsNode> TYPE = NodeClass.create(PointerEqualsNode.class);

    // @def
    private static final PointerEqualsOp OP = new PointerEqualsOp();

    // @cons
    public PointerEqualsNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    public static LogicNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __result = findSynonym(__x, __y, __view);
        if (__result != null)
        {
            return __result;
        }
        return new PointerEqualsNode(__x, __y);
    }

    // @cons
    protected PointerEqualsNode(NodeClass<? extends PointerEqualsNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, CanonicalCondition.EQ, __x, __y);
    }

    @Override
    public Node canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __value = OP.canonical(__tool.getConstantReflection(), __tool.getMetaAccess(), __tool.smallestCompareWidth(), CanonicalCondition.EQ, __forX, __forY, __view);
        if (__value != null)
        {
            return __value;
        }
        return this;
    }

    // @class PointerEqualsNode.PointerEqualsOp
    public static class PointerEqualsOp extends CompareOp
    {
        ///
        // Determines if this is a comparison used to determine whether dispatching on a receiver
        // could select a certain method and if so, returns {@code true} if the answer is guaranteed
        // to be false. Otherwise, returns {@code false}.
        ///
        private static boolean isAlwaysFailingVirtualDispatchTest(CanonicalCondition __condition, ValueNode __forX, ValueNode __forY)
        {
            if (__forY.isConstant())
            {
                if (__forX instanceof LoadMethodNode && __condition == CanonicalCondition.EQ)
                {
                    LoadMethodNode __lm = ((LoadMethodNode) __forX);
                    if (__lm.getMethod().getEncoding().equals(__forY.asConstant()))
                    {
                        if (__lm.getHub() instanceof LoadHubNode)
                        {
                            ValueNode __object = ((LoadHubNode) __lm.getHub()).getValue();
                            ResolvedJavaType __type = StampTool.typeOrNull(__object);
                            ResolvedJavaType __declaringClass = __lm.getMethod().getDeclaringClass();
                            if (__type != null && !__type.equals(__declaringClass) && __declaringClass.isAssignableFrom(__type))
                            {
                                ResolvedJavaMethod __override = __type.resolveMethod(__lm.getMethod(), __lm.getCallerType());
                                if (__override != null && !__override.equals(__lm.getMethod()))
                                {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public LogicNode canonical(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, ValueNode __forX, ValueNode __forY, NodeView __view)
        {
            LogicNode __result = findSynonym(__forX, __forY, __view);
            if (__result != null)
            {
                return __result;
            }
            if (isAlwaysFailingVirtualDispatchTest(__condition, __forX, __forY))
            {
                return LogicConstantNode.contradiction();
            }
            return super.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __forX, __forY, __view);
        }

        @Override
        protected CompareNode duplicateModified(ValueNode __newX, ValueNode __newY, NodeView __view)
        {
            return new PointerEqualsNode(__newX, __newY);
        }
    }

    public static LogicNode findSynonym(ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY))
        {
            return LogicConstantNode.tautology();
        }
        else if (__forX.stamp(__view).alwaysDistinct(__forY.stamp(__view)))
        {
            return LogicConstantNode.contradiction();
        }
        else if (((AbstractPointerStamp) __forX.stamp(__view)).alwaysNull())
        {
            return IsNullNode.create(__forY);
        }
        else if (((AbstractPointerStamp) __forY.stamp(__view)).alwaysNull())
        {
            return IsNullNode.create(__forX);
        }
        else
        {
            return null;
        }
    }

    @Override
    public Stamp getSucceedingStampForX(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        if (!__negated)
        {
            Stamp __newStamp = __xStamp.join(__yStamp);
            if (!__newStamp.equals(__xStamp))
            {
                return __newStamp;
            }
        }
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        if (!__negated)
        {
            Stamp __newStamp = __yStamp.join(__xStamp);
            if (!__newStamp.equals(__yStamp))
            {
                return __newStamp;
            }
        }
        return null;
    }

    @Override
    public TriState tryFold(Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        if (__xStampGeneric instanceof ObjectStamp && __yStampGeneric instanceof ObjectStamp)
        {
            ObjectStamp __xStamp = (ObjectStamp) __xStampGeneric;
            ObjectStamp __yStamp = (ObjectStamp) __yStampGeneric;
            if (__xStamp.alwaysDistinct(__yStamp))
            {
                return TriState.FALSE;
            }
            else if (__xStamp.neverDistinct(__yStamp))
            {
                return TriState.TRUE;
            }
        }
        return TriState.UNKNOWN;
    }
}
