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
import giraaff.options.OptionValues;
import giraaff.util.GraalError;

public final class ObjectEqualsNode extends PointerEqualsNode implements Virtualizable
{
    public static final NodeClass<ObjectEqualsNode> TYPE = NodeClass.create(ObjectEqualsNode.class);

    private static final ObjectEqualsOp OP = new ObjectEqualsOp();

    public ObjectEqualsNode(ValueNode x, ValueNode y)
    {
        super(TYPE, x, y);
    }

    public static LogicNode create(ValueNode x, ValueNode y, ConstantReflectionProvider constantReflection, NodeView view)
    {
        LogicNode result = CompareNode.tryConstantFold(CanonicalCondition.EQ, x, y, constantReflection, false);
        if (result != null)
        {
            return result;
        }
        else
        {
            result = findSynonym(x, y, view);
            if (result != null)
            {
                return result;
            }
            return new ObjectEqualsNode(x, y);
        }
    }

    public static LogicNode create(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode result = OP.canonical(constantReflection, metaAccess, options, null, CanonicalCondition.EQ, false, x, y, view);
        if (result != null)
        {
            return result;
        }
        return create(x, y, constantReflection, view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode value = OP.canonical(tool.getConstantReflection(), tool.getMetaAccess(), tool.getOptions(), tool.smallestCompareWidth(), CanonicalCondition.EQ, false, forX, forY, view);
        if (value != null)
        {
            return value;
        }
        return this;
    }

    public static class ObjectEqualsOp extends PointerEqualsOp
    {
        @Override
        protected LogicNode canonicalizeSymmetricConstant(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, Constant constant, ValueNode nonConstant, boolean mirrored, boolean unorderedIsTrue, NodeView view)
        {
            ResolvedJavaType type = constantReflection.asJavaType(constant);
            if (type != null && nonConstant instanceof GetClassNode)
            {
                GetClassNode getClassNode = (GetClassNode) nonConstant;
                ValueNode object = getClassNode.getObject();
                if (!type.isPrimitive() && (type.isConcrete() || type.isArray()))
                {
                    return InstanceOfNode.create(TypeReference.createExactTrusted(type), object);
                }
                return LogicConstantNode.forBoolean(false);
            }
            return super.canonicalizeSymmetricConstant(constantReflection, metaAccess, options, smallestCompareWidth, condition, constant, nonConstant, mirrored, unorderedIsTrue, view);
        }

        @Override
        protected CompareNode duplicateModified(ValueNode newX, ValueNode newY, boolean unorderedIsTrue, NodeView view)
        {
            if (newX.stamp(view) instanceof ObjectStamp && newY.stamp(view) instanceof ObjectStamp)
            {
                return new ObjectEqualsNode(newX, newY);
            }
            else if (newX.stamp(view) instanceof AbstractPointerStamp && newY.stamp(view) instanceof AbstractPointerStamp)
            {
                return new PointerEqualsNode(newX, newY);
            }
            throw GraalError.shouldNotReachHere();
        }
    }

    private void virtualizeNonVirtualComparison(VirtualObjectNode virtual, ValueNode other, VirtualizerTool tool)
    {
        if (virtual instanceof VirtualBoxingNode && other.isConstant())
        {
            VirtualBoxingNode virtualBoxingNode = (VirtualBoxingNode) virtual;
            if (virtualBoxingNode.getBoxingKind() == JavaKind.Boolean)
            {
                JavaConstant otherUnboxed = tool.getConstantReflectionProvider().unboxPrimitive(other.asJavaConstant());
                if (otherUnboxed != null && otherUnboxed.getJavaKind() == JavaKind.Boolean)
                {
                    int expectedValue = otherUnboxed.asBoolean() ? 1 : 0;
                    IntegerEqualsNode equals = new IntegerEqualsNode(virtualBoxingNode.getBoxedValue(tool), ConstantNode.forInt(expectedValue, graph()));
                    tool.addNode(equals);
                    tool.replaceWithValue(equals);
                }
                else
                {
                    tool.replaceWithValue(LogicConstantNode.contradiction(graph()));
                }
            }
        }
        if (virtual.hasIdentity())
        {
            // one of them is virtual: they can never be the same objects
            tool.replaceWithValue(LogicConstantNode.contradiction(graph()));
        }
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode xAlias = tool.getAlias(getX());
        ValueNode yAlias = tool.getAlias(getY());

        VirtualObjectNode xVirtual = xAlias instanceof VirtualObjectNode ? (VirtualObjectNode) xAlias : null;
        VirtualObjectNode yVirtual = yAlias instanceof VirtualObjectNode ? (VirtualObjectNode) yAlias : null;

        if (xVirtual != null && yVirtual == null)
        {
            virtualizeNonVirtualComparison(xVirtual, yAlias, tool);
        }
        else if (xVirtual == null && yVirtual != null)
        {
            virtualizeNonVirtualComparison(yVirtual, xAlias, tool);
        }
        else if (xVirtual != null && yVirtual != null)
        {
            if (xVirtual.hasIdentity() ^ yVirtual.hasIdentity())
            {
                /*
                 * One of the two objects has identity, the other doesn't. In code, this looks like
                 * "Integer.valueOf(a) == new Integer(b)", which is always false.
                 *
                 * In other words: an object created via valueOf can never be equal to one created
                 * by new in the same compilation unit.
                 */
                tool.replaceWithValue(LogicConstantNode.contradiction(graph()));
            }
            else if (!xVirtual.hasIdentity() && !yVirtual.hasIdentity())
            {
                ResolvedJavaType type = xVirtual.type();
                if (type.equals(yVirtual.type()))
                {
                    MetaAccessProvider metaAccess = tool.getMetaAccessProvider();
                    if (type.equals(metaAccess.lookupJavaType(Integer.class)) || type.equals(metaAccess.lookupJavaType(Long.class)))
                    {
                        // both are virtual without identity: check contents
                        IntegerEqualsNode equals = new IntegerEqualsNode(tool.getEntry(xVirtual, 0), tool.getEntry(yVirtual, 0));
                        tool.addNode(equals);
                        tool.replaceWithValue(equals);
                    }
                }
            }
            else
            {
                // both are virtual with identity: check if they refer to the same object
                tool.replaceWithValue(LogicConstantNode.forBoolean(xVirtual == yVirtual, graph()));
            }
        }
    }
}
