package giraaff.nodes.java;

import java.util.ArrayList;
import java.util.Arrays;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.extended.SwitchNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

/**
 * The {@code TypeSwitchNode} performs a lookup based on the type of the input value. The type
 * comparison is an exact type comparison, not an instanceof.
 */
// @class TypeSwitchNode
public final class TypeSwitchNode extends SwitchNode implements LIRLowerable, Simplifiable
{
    // @def
    public static final NodeClass<TypeSwitchNode> TYPE = NodeClass.create(TypeSwitchNode.class);

    // @field
    protected final ResolvedJavaType[] keys;
    // @field
    protected final Constant[] hubs;

    // @cons
    public TypeSwitchNode(ValueNode __value, AbstractBeginNode[] __successors, ResolvedJavaType[] __keys, double[] __keyProbabilities, int[] __keySuccessors, ConstantReflectionProvider __constantReflection)
    {
        super(TYPE, __value, __successors, __keySuccessors, __keyProbabilities);
        this.keys = __keys;

        hubs = new Constant[__keys.length];
        for (int __i = 0; __i < hubs.length; __i++)
        {
            hubs[__i] = __constantReflection.asObjectHub(__keys[__i]);
        }
    }

    /**
     * Don't allow duplicate keys.
     */
    private boolean assertKeys()
    {
        for (int __i = 0; __i < keys.length; __i++)
        {
            for (int __j = 0; __j < keys.length; __j++)
            {
                if (__i == __j)
                {
                    continue;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isSorted()
    {
        return false;
    }

    @Override
    public int keyCount()
    {
        return keys.length;
    }

    @Override
    public Constant keyAt(int __index)
    {
        return hubs[__index];
    }

    @Override
    public boolean equalKeys(SwitchNode __switchNode)
    {
        if (!(__switchNode instanceof TypeSwitchNode))
        {
            return false;
        }
        TypeSwitchNode __other = (TypeSwitchNode) __switchNode;
        return Arrays.equals(keys, __other.keys);
    }

    public ResolvedJavaType typeAt(int __index)
    {
        return keys[__index];
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.emitSwitch(this);
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        if (value() instanceof ConstantNode)
        {
            Constant __constant = value().asConstant();

            int __survivingEdge = keySuccessorIndex(keyCount());
            for (int __i = 0; __i < keyCount(); __i++)
            {
                Constant __typeHub = keyAt(__i);
                Boolean __equal = __tool.getConstantReflection().constantEquals(__constant, __typeHub);
                if (__equal == null)
                {
                    // We don't know if this key is a match or not, so we cannot simplify.
                    return;
                }
                else if (__equal.booleanValue())
                {
                    __survivingEdge = keySuccessorIndex(__i);
                }
            }
            killOtherSuccessors(__tool, __survivingEdge);
        }
        if (value() instanceof LoadHubNode && ((LoadHubNode) value()).getValue().stamp(__view) instanceof ObjectStamp)
        {
            ObjectStamp __objectStamp = (ObjectStamp) ((LoadHubNode) value()).getValue().stamp(__view);
            if (__objectStamp.type() != null)
            {
                int __validKeys = 0;
                for (int __i = 0; __i < keyCount(); __i++)
                {
                    if (__objectStamp.type().isAssignableFrom(keys[__i]))
                    {
                        __validKeys++;
                    }
                }
                if (__validKeys == 0)
                {
                    __tool.addToWorkList(defaultSuccessor());
                    graph().removeSplitPropagate(this, defaultSuccessor());
                }
                else if (__validKeys != keys.length)
                {
                    ArrayList<AbstractBeginNode> __newSuccessors = new ArrayList<>(blockSuccessorCount());
                    ResolvedJavaType[] __newKeys = new ResolvedJavaType[__validKeys];
                    int[] __newKeySuccessors = new int[__validKeys + 1];
                    double[] __newKeyProbabilities = new double[__validKeys + 1];
                    double __totalProbability = 0;
                    int __current = 0;
                    for (int __i = 0; __i < keyCount() + 1; __i++)
                    {
                        if (__i == keyCount() || __objectStamp.type().isAssignableFrom(keys[__i]))
                        {
                            int __index = __newSuccessors.indexOf(keySuccessor(__i));
                            if (__index == -1)
                            {
                                __index = __newSuccessors.size();
                                __newSuccessors.add(keySuccessor(__i));
                            }
                            __newKeySuccessors[__current] = __index;
                            if (__i < keyCount())
                            {
                                __newKeys[__current] = keys[__i];
                            }
                            __newKeyProbabilities[__current] = keyProbability(__i);
                            __totalProbability += keyProbability(__i);
                            __current++;
                        }
                    }
                    if (__totalProbability > 0)
                    {
                        for (int __i = 0; __i < __current; __i++)
                        {
                            __newKeyProbabilities[__i] /= __totalProbability;
                        }
                    }
                    else
                    {
                        for (int __i = 0; __i < __current; __i++)
                        {
                            __newKeyProbabilities[__i] = 1.0 / __current;
                        }
                    }

                    for (int __i = 0; __i < blockSuccessorCount(); __i++)
                    {
                        AbstractBeginNode __successor = blockSuccessor(__i);
                        if (!__newSuccessors.contains(__successor))
                        {
                            __tool.deleteBranch(__successor);
                        }
                        setBlockSuccessor(__i, null);
                    }

                    AbstractBeginNode[] __successorsArray = __newSuccessors.toArray(new AbstractBeginNode[__newSuccessors.size()]);
                    TypeSwitchNode __newSwitch = graph().add(new TypeSwitchNode(value(), __successorsArray, __newKeys, __newKeyProbabilities, __newKeySuccessors, __tool.getConstantReflection()));
                    ((FixedWithNextNode) predecessor()).setNext(__newSwitch);
                    GraphUtil.killWithUnusedFloatingInputs(this);
                }
            }
        }
    }

    @Override
    public Stamp getValueStampForSuccessor(AbstractBeginNode __beginNode)
    {
        Stamp __result = null;
        if (__beginNode != defaultSuccessor())
        {
            for (int __i = 0; __i < keyCount(); __i++)
            {
                if (keySuccessor(__i) == __beginNode)
                {
                    if (__result == null)
                    {
                        __result = StampFactory.objectNonNull(TypeReference.createExactTrusted(typeAt(__i)));
                    }
                    else
                    {
                        __result = __result.meet(StampFactory.objectNonNull(TypeReference.createExactTrusted(typeAt(__i))));
                    }
                }
            }
        }
        return __result;
    }
}
