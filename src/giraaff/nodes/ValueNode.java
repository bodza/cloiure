package giraaff.nodes;

import java.util.function.Predicate;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodePredicate;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.spi.NodeValueMap;

/**
 * This class represents a value within the graph, including local variables, phis, and all other instructions.
 */
// @class ValueNode
public abstract class ValueNode extends giraaff.graph.Node implements ValueNodeInterface
{
    // @def
    public static final NodeClass<ValueNode> TYPE = NodeClass.create(ValueNode.class);

    /**
     * The kind of this value. This is {@link JavaKind#Void} for instructions that produce no value.
     * This kind is guaranteed to be a {@linkplain JavaKind#getStackKind() stack kind}.
     */
    // @field
    protected Stamp stamp;

    // @cons
    public ValueNode(NodeClass<? extends ValueNode> __c, Stamp __stamp)
    {
        super(__c);
        this.stamp = __stamp;
    }

    public final Stamp stamp(NodeView __view)
    {
        return __view.stamp(this);
    }

    public final void setStamp(Stamp __stamp)
    {
        this.stamp = __stamp;
    }

    @Override
    public final StructuredGraph graph()
    {
        return (StructuredGraph) super.graph();
    }

    /**
     * Checks if the given stamp is different than the current one (
     * {@code newStamp.equals(oldStamp) == false}). If it is different then the new stamp will
     * become the current stamp for this node.
     *
     * @return true if the stamp has changed, false otherwise.
     */
    protected final boolean updateStamp(Stamp __newStamp)
    {
        if (__newStamp == null || __newStamp.equals(stamp))
        {
            return false;
        }
        else
        {
            stamp = __newStamp;
            return true;
        }
    }

    /**
     * This method can be overridden by subclasses of {@link ValueNode} if they need to recompute
     * their stamp if their inputs change. A typical implementation will compute the stamp and pass
     * it to {@link #updateStamp(Stamp)}, whose return value can be used as the result of this method.
     *
     * @return true if the stamp has changed, false otherwise.
     */
    public boolean inferStamp()
    {
        return false;
    }

    public final JavaKind getStackKind()
    {
        return stamp(NodeView.DEFAULT).getStackKind();
    }

    /**
     * Checks whether this value is a constant (i.e. it is of type {@link ConstantNode}.
     *
     * @return {@code true} if this value is a constant
     */
    public final boolean isConstant()
    {
        return this instanceof ConstantNode;
    }

    // @closure
    private static final NodePredicate IS_CONSTANT = new NodePredicate()
    {
        @Override
        public boolean apply(Node __n)
        {
            return __n instanceof ConstantNode;
        }
    };

    public static NodePredicate isConstantPredicate()
    {
        return IS_CONSTANT;
    }

    /**
     * Checks whether this value represents the null constant.
     *
     * @return {@code true} if this value represents the null constant
     */
    public final boolean isNullConstant()
    {
        JavaConstant __value = asJavaConstant();
        return __value != null && __value.isNull();
    }

    /**
     * Convert this value to a constant if it is a constant, otherwise return null.
     *
     * @return the {@link JavaConstant} represented by this value if it is a constant; {@code null} otherwise
     */
    public final Constant asConstant()
    {
        if (this instanceof ConstantNode)
        {
            return ((ConstantNode) this).getValue();
        }
        else
        {
            return null;
        }
    }

    public final boolean isJavaConstant()
    {
        return isConstant() && asConstant() instanceof JavaConstant;
    }

    public final JavaConstant asJavaConstant()
    {
        Constant __value = asConstant();
        if (__value instanceof JavaConstant)
        {
            return (JavaConstant) __value;
        }
        else
        {
            return null;
        }
    }

    @Override
    public ValueNode asNode()
    {
        return this;
    }

    @Override
    public boolean isAllowedUsageType(InputType __type)
    {
        if (getStackKind() != JavaKind.Void && __type == InputType.Value)
        {
            return true;
        }
        else
        {
            return super.isAllowedUsageType(__type);
        }
    }

    /**
     * Checks if this node has usages other than the given node {@code node}.
     *
     * @param node node which is ignored when searching for usages
     * @return true if this node has other usages, false otherwise
     */
    public boolean hasUsagesOtherThan(ValueNode __node, NodeValueMap __nodeValueMap)
    {
        for (Node __usage : usages())
        {
            if (__usage != __node && __usage instanceof ValueNode && __nodeValueMap.hasOperand(__usage))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void replaceAtUsages(Node __other, Predicate<Node> __filter, Node __toBeDeleted)
    {
        super.replaceAtUsages(__other, __filter, __toBeDeleted);
    }

    private boolean checkReplaceAtUsagesInvariants(Node __other)
    {
        if (this.hasUsages() && !this.stamp(NodeView.DEFAULT).isEmpty() && !(__other instanceof PhiNode) && __other != null)
        {
            boolean __morePrecise = ((ValueNode) __other).stamp(NodeView.DEFAULT).join(stamp(NodeView.DEFAULT)).equals(((ValueNode) __other).stamp(NodeView.DEFAULT));
        }
        return true;
    }
}
