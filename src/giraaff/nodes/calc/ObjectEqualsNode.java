package giraaff.nodes.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.GetClassNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualBoxingNode;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.util.GraalError;

// @class ObjectEqualsNode
public final class ObjectEqualsNode extends PointerEqualsNode implements Virtualizable
{
    // @def
    public static final NodeClass<ObjectEqualsNode> TYPE = NodeClass.create(ObjectEqualsNode.class);

    // @def
    private static final ObjectEqualsOp OP = new ObjectEqualsOp();

    // @cons
    public ObjectEqualsNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, __x, __y);
    }

    public static LogicNode create(ValueNode __x, ValueNode __y, ConstantReflectionProvider __constantReflection, NodeView __view)
    {
        LogicNode __result = CompareNode.tryConstantFold(CanonicalCondition.EQ, __x, __y, __constantReflection, false);
        if (__result != null)
        {
            return __result;
        }
        else
        {
            __result = findSynonym(__x, __y, __view);
            if (__result != null)
            {
                return __result;
            }
            return new ObjectEqualsNode(__x, __y);
        }
    }

    public static LogicNode create(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __result = OP.canonical(__constantReflection, __metaAccess, null, CanonicalCondition.EQ, false, __x, __y, __view);
        if (__result != null)
        {
            return __result;
        }
        return create(__x, __y, __constantReflection, __view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __value = OP.canonical(__tool.getConstantReflection(), __tool.getMetaAccess(), __tool.smallestCompareWidth(), CanonicalCondition.EQ, false, __forX, __forY, __view);
        if (__value != null)
        {
            return __value;
        }
        return this;
    }

    // @class ObjectEqualsNode.ObjectEqualsOp
    public static final class ObjectEqualsOp extends PointerEqualsOp
    {
        @Override
        protected LogicNode canonicalizeSymmetricConstant(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, Constant __constant, ValueNode __nonConstant, boolean __mirrored, boolean __unorderedIsTrue, NodeView __view)
        {
            ResolvedJavaType __type = __constantReflection.asJavaType(__constant);
            if (__type != null && __nonConstant instanceof GetClassNode)
            {
                GetClassNode __getClassNode = (GetClassNode) __nonConstant;
                ValueNode __object = __getClassNode.getObject();
                if (!__type.isPrimitive() && (__type.isConcrete() || __type.isArray()))
                {
                    return InstanceOfNode.create(TypeReference.createExactTrusted(__type), __object);
                }
                return LogicConstantNode.forBoolean(false);
            }
            return super.canonicalizeSymmetricConstant(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __constant, __nonConstant, __mirrored, __unorderedIsTrue, __view);
        }

        @Override
        protected CompareNode duplicateModified(ValueNode __newX, ValueNode __newY, boolean __unorderedIsTrue, NodeView __view)
        {
            if (__newX.stamp(__view) instanceof ObjectStamp && __newY.stamp(__view) instanceof ObjectStamp)
            {
                return new ObjectEqualsNode(__newX, __newY);
            }
            else if (__newX.stamp(__view) instanceof AbstractPointerStamp && __newY.stamp(__view) instanceof AbstractPointerStamp)
            {
                return new PointerEqualsNode(__newX, __newY);
            }
            throw GraalError.shouldNotReachHere();
        }
    }

    private void virtualizeNonVirtualComparison(VirtualObjectNode __virtual, ValueNode __other, VirtualizerTool __tool)
    {
        if (__virtual instanceof VirtualBoxingNode && __other.isConstant())
        {
            VirtualBoxingNode __virtualBoxingNode = (VirtualBoxingNode) __virtual;
            if (__virtualBoxingNode.getBoxingKind() == JavaKind.Boolean)
            {
                JavaConstant __otherUnboxed = __tool.getConstantReflectionProvider().unboxPrimitive(__other.asJavaConstant());
                if (__otherUnboxed != null && __otherUnboxed.getJavaKind() == JavaKind.Boolean)
                {
                    int __expectedValue = __otherUnboxed.asBoolean() ? 1 : 0;
                    IntegerEqualsNode __equals = new IntegerEqualsNode(__virtualBoxingNode.getBoxedValue(__tool), ConstantNode.forInt(__expectedValue, graph()));
                    __tool.addNode(__equals);
                    __tool.replaceWithValue(__equals);
                }
                else
                {
                    __tool.replaceWithValue(LogicConstantNode.contradiction(graph()));
                }
            }
        }
        if (__virtual.hasIdentity())
        {
            // one of them is virtual: they can never be the same objects
            __tool.replaceWithValue(LogicConstantNode.contradiction(graph()));
        }
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __xAlias = __tool.getAlias(getX());
        ValueNode __yAlias = __tool.getAlias(getY());

        VirtualObjectNode __xVirtual = __xAlias instanceof VirtualObjectNode ? (VirtualObjectNode) __xAlias : null;
        VirtualObjectNode __yVirtual = __yAlias instanceof VirtualObjectNode ? (VirtualObjectNode) __yAlias : null;

        if (__xVirtual != null && __yVirtual == null)
        {
            virtualizeNonVirtualComparison(__xVirtual, __yAlias, __tool);
        }
        else if (__xVirtual == null && __yVirtual != null)
        {
            virtualizeNonVirtualComparison(__yVirtual, __xAlias, __tool);
        }
        else if (__xVirtual != null && __yVirtual != null)
        {
            if (__xVirtual.hasIdentity() ^ __yVirtual.hasIdentity())
            {
                // One of the two objects has identity, the other doesn't. In code, this looks like
                // "Integer.valueOf(a) == new Integer(b)", which is always false.
                //
                // In other words: an object created via valueOf can never be equal to one created
                // by new in the same compilation unit.
                __tool.replaceWithValue(LogicConstantNode.contradiction(graph()));
            }
            else if (!__xVirtual.hasIdentity() && !__yVirtual.hasIdentity())
            {
                ResolvedJavaType __type = __xVirtual.type();
                if (__type.equals(__yVirtual.type()))
                {
                    MetaAccessProvider __metaAccess = __tool.getMetaAccessProvider();
                    if (__type.equals(__metaAccess.lookupJavaType(Integer.class)) || __type.equals(__metaAccess.lookupJavaType(Long.class)))
                    {
                        // both are virtual without identity: check contents
                        IntegerEqualsNode __equals = new IntegerEqualsNode(__tool.getEntry(__xVirtual, 0), __tool.getEntry(__yVirtual, 0));
                        __tool.addNode(__equals);
                        __tool.replaceWithValue(__equals);
                    }
                }
            }
            else
            {
                // both are virtual with identity: check if they refer to the same object
                __tool.replaceWithValue(LogicConstantNode.forBoolean(__xVirtual == __yVirtual, graph()));
            }
        }
    }
}
