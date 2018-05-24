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
public final class ArrayLengthNode extends FixedWithNextNode implements Canonicalizable.Unary<ValueNode>, Lowerable, Virtualizable
{
    public static final NodeClass<ArrayLengthNode> TYPE = NodeClass.create(ArrayLengthNode.class);
    @Input ValueNode array;

    public ValueNode array()
    {
        return array;
    }

    @Override
    public ValueNode getValue()
    {
        return array;
    }

    public ArrayLengthNode(ValueNode array)
    {
        super(TYPE, StampFactory.positiveInt());
        this.array = array;
    }

    public static ValueNode create(ValueNode forValue, ConstantReflectionProvider constantReflection)
    {
        if (forValue instanceof NewArrayNode)
        {
            NewArrayNode newArray = (NewArrayNode) forValue;
            return newArray.length();
        }

        ValueNode length = readArrayLengthConstant(forValue, constantReflection);
        if (length != null)
        {
            return length;
        }
        return new ArrayLengthNode(forValue);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        ValueNode length = readArrayLength(forValue, tool.getConstantReflection());
        if (length != null)
        {
            return length;
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
    private static ValueNode reproxyValue(ValueNode originalValue, ValueNode value)
    {
        if (value.isConstant())
        {
            // No proxy needed
            return value;
        }
        if (originalValue instanceof ValueProxyNode)
        {
            ValueProxyNode proxy = (ValueProxyNode) originalValue;
            return new ValueProxyNode(reproxyValue(proxy.getOriginalNode(), value), proxy.proxyPoint());
        }
        else if (originalValue instanceof ValueProxy)
        {
            ValueProxy proxy = (ValueProxy) originalValue;
            return reproxyValue(proxy.getOriginalNode(), value);
        }
        else
        {
            return value;
        }
    }

    /**
     * Gets the length of an array if possible.
     *
     * @return a node representing the length of {@code array} or null if it is not available
     */
    public static ValueNode readArrayLength(ValueNode originalArray, ConstantReflectionProvider constantReflection)
    {
        ValueNode length = GraphUtil.arrayLength(originalArray);
        if (length != null)
        {
            // Ensure that any proxies on the original value end up on the length value
            return reproxyValue(originalArray, length);
        }
        return readArrayLengthConstant(originalArray, constantReflection);
    }

    private static ValueNode readArrayLengthConstant(ValueNode originalArray, ConstantReflectionProvider constantReflection)
    {
        ValueNode array = GraphUtil.unproxify(originalArray);
        if (constantReflection != null && array.isConstant() && !array.isNullConstant())
        {
            JavaConstant constantValue = array.asJavaConstant();
            if (constantValue != null && constantValue.isNonNull())
            {
                Integer constantLength = constantReflection.readArrayLength(constantValue);
                if (constantLength != null)
                {
                    return ConstantNode.forInt(constantLength);
                }
            }
        }
        return null;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @NodeIntrinsic
    public static native int arrayLength(Object array);

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(array());
        if (alias instanceof VirtualArrayNode)
        {
            VirtualArrayNode virtualArray = (VirtualArrayNode) alias;
            tool.replaceWithValue(ConstantNode.forInt(virtualArray.entryCount(), graph()));
        }
    }
}