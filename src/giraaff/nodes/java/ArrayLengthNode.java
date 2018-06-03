package giraaff.nodes.java;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValueProxyNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.ValueProxy;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.VirtualArrayNode;

/**
 * The {@code ArrayLength} instruction gets the length of an array.
 */
// @class ArrayLengthNode
public final class ArrayLengthNode extends FixedWithNextNode implements Canonicalizable.Unary<ValueNode>, Lowerable, Virtualizable
{
    // @def
    public static final NodeClass<ArrayLengthNode> TYPE = NodeClass.create(ArrayLengthNode.class);

    @Input
    // @field
    ValueNode array;

    public ValueNode array()
    {
        return array;
    }

    @Override
    public ValueNode getValue()
    {
        return array;
    }

    // @cons
    public ArrayLengthNode(ValueNode __array)
    {
        super(TYPE, StampFactory.positiveInt());
        this.array = __array;
    }

    public static ValueNode create(ValueNode __forValue, ConstantReflectionProvider __constantReflection)
    {
        if (__forValue instanceof NewArrayNode)
        {
            NewArrayNode __newArray = (NewArrayNode) __forValue;
            return __newArray.length();
        }

        ValueNode __length = readArrayLengthConstant(__forValue, __constantReflection);
        if (__length != null)
        {
            return __length;
        }
        return new ArrayLengthNode(__forValue);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        ValueNode __length = readArrayLength(__forValue, __tool.getConstantReflection());
        if (__length != null)
        {
            return __length;
        }
        return this;
    }

    /**
     * Replicate the {@link ValueProxyNode}s from {@code originalValue} onto {@code value}.
     *
     * @param originalValue a possibly proxied value
     * @param value a value needing proxies
     * @return proxies wrapping {@code value}
     */
    private static ValueNode reproxyValue(ValueNode __originalValue, ValueNode __value)
    {
        if (__value.isConstant())
        {
            // no proxy needed
            return __value;
        }
        if (__originalValue instanceof ValueProxyNode)
        {
            ValueProxyNode __proxy = (ValueProxyNode) __originalValue;
            return new ValueProxyNode(reproxyValue(__proxy.getOriginalNode(), __value), __proxy.proxyPoint());
        }
        else if (__originalValue instanceof ValueProxy)
        {
            ValueProxy __proxy = (ValueProxy) __originalValue;
            return reproxyValue(__proxy.getOriginalNode(), __value);
        }
        else
        {
            return __value;
        }
    }

    /**
     * Gets the length of an array if possible.
     *
     * @return a node representing the length of {@code array} or null if it is not available
     */
    public static ValueNode readArrayLength(ValueNode __originalArray, ConstantReflectionProvider __constantReflection)
    {
        ValueNode __length = GraphUtil.arrayLength(__originalArray);
        if (__length != null)
        {
            // ensure that any proxies on the original value end up on the length value
            return reproxyValue(__originalArray, __length);
        }
        return readArrayLengthConstant(__originalArray, __constantReflection);
    }

    private static ValueNode readArrayLengthConstant(ValueNode __originalArray, ConstantReflectionProvider __constantReflection)
    {
        ValueNode __array = GraphUtil.unproxify(__originalArray);
        if (__constantReflection != null && __array.isConstant() && !__array.isNullConstant())
        {
            JavaConstant __constantValue = __array.asJavaConstant();
            if (__constantValue != null && __constantValue.isNonNull())
            {
                Integer __constantLength = __constantReflection.readArrayLength(__constantValue);
                if (__constantLength != null)
                {
                    return ConstantNode.forInt(__constantLength);
                }
            }
        }
        return null;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @NodeIntrinsic
    public static native int arrayLength(Object array);

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(array());
        if (__alias instanceof VirtualArrayNode)
        {
            VirtualArrayNode __virtualArray = (VirtualArrayNode) __alias;
            __tool.replaceWithValue(ConstantNode.forInt(__virtualArray.entryCount(), graph()));
        }
    }
}
