package giraaff.nodes.extended;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IntegerBelowNode;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

/**
 * The {@code IntegerSwitchNode} represents a switch on integer keys, with a sorted array of key
 * values. The actual implementation of the switch will be decided by the backend.
 */
// @class IntegerSwitchNode
public final class IntegerSwitchNode extends SwitchNode implements LIRLowerable, Simplifiable
{
    // @def
    public static final NodeClass<IntegerSwitchNode> TYPE = NodeClass.create(IntegerSwitchNode.class);

    // @field
    protected final int[] keys;

    // @cons
    public IntegerSwitchNode(ValueNode __value, AbstractBeginNode[] __successors, int[] __keys, double[] __keyProbabilities, int[] __keySuccessors)
    {
        super(TYPE, __value, __successors, __keySuccessors, __keyProbabilities);
        this.keys = __keys;
    }

    // @cons
    public IntegerSwitchNode(ValueNode __value, int __successorCount, int[] __keys, double[] __keyProbabilities, int[] __keySuccessors)
    {
        this(__value, new AbstractBeginNode[__successorCount], __keys, __keyProbabilities, __keySuccessors);
    }

    @Override
    public boolean isSorted()
    {
        return true;
    }

    /**
     * Gets the key at the specified index.
     *
     * @param i the index
     * @return the key at that index
     */
    @Override
    public JavaConstant keyAt(int __i)
    {
        return JavaConstant.forInt(keys[__i]);
    }

    @Override
    public int keyCount()
    {
        return keys.length;
    }

    @Override
    public boolean equalKeys(SwitchNode __switchNode)
    {
        if (!(__switchNode instanceof IntegerSwitchNode))
        {
            return false;
        }
        IntegerSwitchNode __other = (IntegerSwitchNode) __switchNode;
        return Arrays.equals(keys, __other.keys);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.emitSwitch(this);
    }

    public AbstractBeginNode successorAtKey(int __key)
    {
        return blockSuccessor(successorIndexAtKey(__key));
    }

    public int successorIndexAtKey(int __key)
    {
        for (int __i = 0; __i < keyCount(); __i++)
        {
            if (keys[__i] == __key)
            {
                return keySuccessorIndex(__i);
            }
        }
        return keySuccessorIndex(keyCount());
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        if (blockSuccessorCount() == 1)
        {
            __tool.addToWorkList(defaultSuccessor());
            graph().removeSplitPropagate(this, defaultSuccessor());
        }
        else if (value() instanceof ConstantNode)
        {
            killOtherSuccessors(__tool, successorIndexAtKey(value().asJavaConstant().asInt()));
        }
        else if (tryOptimizeEnumSwitch(__tool))
        {
            return;
        }
        else if (tryRemoveUnreachableKeys(__tool, value().stamp(__view)))
        {
            return;
        }
    }

    // @class IntegerSwitchNode.KeyData
    static final class KeyData
    {
        // @field
        final int key;
        // @field
        final double keyProbability;
        // @field
        final int keySuccessor;

        // @cons
        KeyData(int __key, double __keyProbability, int __keySuccessor)
        {
            super();
            this.key = __key;
            this.keyProbability = __keyProbability;
            this.keySuccessor = __keySuccessor;
        }
    }

    /**
     * Remove unreachable keys from the switch based on the stamp of the value, i.e., based on the
     * known range of the switch value.
     */
    public boolean tryRemoveUnreachableKeys(SimplifierTool __tool, Stamp __valueStamp)
    {
        if (!(__valueStamp instanceof IntegerStamp))
        {
            return false;
        }
        IntegerStamp __integerStamp = (IntegerStamp) __valueStamp;
        if (__integerStamp.isUnrestricted())
        {
            return false;
        }

        List<KeyData> __newKeyDatas = new ArrayList<>(keys.length);
        ArrayList<AbstractBeginNode> __newSuccessors = new ArrayList<>(blockSuccessorCount());
        for (int __i = 0; __i < keys.length; __i++)
        {
            if (__integerStamp.contains(keys[__i]) && keySuccessor(__i) != defaultSuccessor())
            {
                __newKeyDatas.add(new KeyData(keys[__i], keyProbabilities[__i], addNewSuccessor(keySuccessor(__i), __newSuccessors)));
            }
        }

        if (__newKeyDatas.size() == keys.length)
        {
            // All keys are reachable.
            return false;
        }
        else if (__newKeyDatas.size() == 0)
        {
            if (__tool != null)
            {
                __tool.addToWorkList(defaultSuccessor());
            }
            graph().removeSplitPropagate(this, defaultSuccessor());
            return true;
        }
        else
        {
            int __newDefaultSuccessor = addNewSuccessor(defaultSuccessor(), __newSuccessors);
            double __newDefaultProbability = keyProbabilities[keyProbabilities.length - 1];
            doReplace(value(), __newKeyDatas, __newSuccessors, __newDefaultSuccessor, __newDefaultProbability);
            return true;
        }
    }

    /**
     * For switch statements on enum values, the Java compiler has to generate complicated code:
     * because {@link Enum#ordinal()} can change when recompiling an enum, it cannot be used
     * directly as the value that is switched on. An intermediate int[] array, which is initialized
     * once at run time based on the actual {@link Enum#ordinal()} values, is used.
     *
     * The {@link ConstantFieldProvider} of Graal already detects the int[] arrays and marks them as
     * {@link ConstantNode#isDefaultStable() stable}, i.e., the array elements are constant. The
     * code in this method detects array loads from such a stable array and re-wires the switch to
     * use the keys from the array elements, so that the array load is unnecessary.
     */
    private boolean tryOptimizeEnumSwitch(SimplifierTool __tool)
    {
        if (!(value() instanceof LoadIndexedNode))
        {
            // Not the switch pattern we are looking for.
            return false;
        }
        LoadIndexedNode __loadIndexed = (LoadIndexedNode) value();
        if (__loadIndexed.usages().count() > 1)
        {
            // The array load is necessary for other reasons too, so there is no benefit optimizing the switch.
            return false;
        }

        ValueNode __newValue = __loadIndexed.index();
        JavaConstant __arrayConstant = __loadIndexed.array().asJavaConstant();
        if (__arrayConstant == null || ((ConstantNode) __loadIndexed.array()).getStableDimension() != 1 || !((ConstantNode) __loadIndexed.array()).isDefaultStable())
        {
            /*
             * The array is a constant that we can optimize. We require the array elements to be
             * constant too, since we put them as literal constants into the switch keys.
             */
            return false;
        }

        Integer __optionalArrayLength = __tool.getConstantReflection().readArrayLength(__arrayConstant);
        if (__optionalArrayLength == null)
        {
            // Loading a constant value can be denied by the VM.
            return false;
        }
        int __arrayLength = __optionalArrayLength;

        Map<Integer, List<Integer>> __reverseArrayMapping = new HashMap<>();
        for (int __i = 0; __i < __arrayLength; __i++)
        {
            JavaConstant __elementConstant = __tool.getConstantReflection().readArrayElement(__arrayConstant, __i);
            if (__elementConstant == null || __elementConstant.getJavaKind() != JavaKind.Int)
            {
                // Loading a constant value can be denied by the VM.
                return false;
            }
            int __element = __elementConstant.asInt();

            /*
             * The value loaded from the array is the old switch key, the index into the array is
             * the new switch key. We build a mapping from the old switch key to new keys.
             */
            __reverseArrayMapping.computeIfAbsent(__element, __e -> new ArrayList<>()).add(__i);
        }

        // Build high-level representation of new switch keys.
        List<KeyData> __newKeyDatas = new ArrayList<>(__arrayLength);
        ArrayList<AbstractBeginNode> __newSuccessors = new ArrayList<>(blockSuccessorCount());
        for (int __i = 0; __i < keys.length; __i++)
        {
            List<Integer> __newKeys = __reverseArrayMapping.get(keys[__i]);
            if (__newKeys == null || __newKeys.size() == 0)
            {
                // The switch case is unreachable, we can ignore it.
                continue;
            }

            /*
             * We do not have detailed profiling information about the individual new keys, so we
             * have to assume they split the probability of the old key.
             */
            double __newKeyProbability = keyProbabilities[__i] / __newKeys.size();
            int __newKeySuccessor = addNewSuccessor(keySuccessor(__i), __newSuccessors);

            for (int __newKey : __newKeys)
            {
                __newKeyDatas.add(new KeyData(__newKey, __newKeyProbability, __newKeySuccessor));
            }
        }

        int __newDefaultSuccessor = addNewSuccessor(defaultSuccessor(), __newSuccessors);
        double __newDefaultProbability = keyProbabilities[keyProbabilities.length - 1];

        /*
         * We remove the array load, but we still need to preserve exception semantics by keeping
         * the bounds check. Fortunately the array length is a constant.
         */
        LogicNode __boundsCheck = graph().unique(new IntegerBelowNode(__newValue, ConstantNode.forInt(__arrayLength, graph())));
        graph().addBeforeFixed(this, graph().add(new FixedGuardNode(__boundsCheck, DeoptimizationReason.BoundsCheckException, DeoptimizationAction.InvalidateReprofile)));

        // Build the low-level representation of the new switch keys and replace ourself with a new node.
        doReplace(__newValue, __newKeyDatas, __newSuccessors, __newDefaultSuccessor, __newDefaultProbability);

        // The array load is now unnecessary.
        GraphUtil.removeFixedWithUnusedInputs(__loadIndexed);

        return true;
    }

    private static int addNewSuccessor(AbstractBeginNode __newSuccessor, ArrayList<AbstractBeginNode> __newSuccessors)
    {
        int __index = __newSuccessors.indexOf(__newSuccessor);
        if (__index == -1)
        {
            __index = __newSuccessors.size();
            __newSuccessors.add(__newSuccessor);
        }
        return __index;
    }

    private void doReplace(ValueNode __newValue, List<KeyData> __newKeyDatas, ArrayList<AbstractBeginNode> __newSuccessors, int __newDefaultSuccessor, double __newDefaultProbability)
    {
        // Sort the new keys (invariant of the IntegerSwitchNode).
        __newKeyDatas.sort(Comparator.comparingInt(__k -> __k.key));

        // Create the final data arrays.
        int __newKeyCount = __newKeyDatas.size();
        int[] __newKeys = new int[__newKeyCount];
        double[] __newKeyProbabilities = new double[__newKeyCount + 1];
        int[] __newKeySuccessors = new int[__newKeyCount + 1];

        for (int __i = 0; __i < __newKeyCount; __i++)
        {
            KeyData __keyData = __newKeyDatas.get(__i);
            __newKeys[__i] = __keyData.key;
            __newKeyProbabilities[__i] = __keyData.keyProbability;
            __newKeySuccessors[__i] = __keyData.keySuccessor;
        }

        __newKeySuccessors[__newKeyCount] = __newDefaultSuccessor;
        __newKeyProbabilities[__newKeyCount] = __newDefaultProbability;

        // Normalize new probabilities so that they sum up to 1.
        double __totalProbability = 0;
        for (double __probability : __newKeyProbabilities)
        {
            __totalProbability += __probability;
        }
        if (__totalProbability > 0)
        {
            for (int __i = 0; __i < __newKeyProbabilities.length; __i++)
            {
                __newKeyProbabilities[__i] /= __totalProbability;
            }
        }
        else
        {
            for (int __i = 0; __i < __newKeyProbabilities.length; __i++)
            {
                __newKeyProbabilities[__i] = 1.0 / __newKeyProbabilities.length;
            }
        }

        // Collect dead successors. Successors have to be cleaned before adding the new node to the graph.
        List<AbstractBeginNode> __deadSuccessors = successors.filter(__s -> !__newSuccessors.contains(__s)).snapshot();
        successors.clear();

        // Create the new switch node. This is done before removing dead successors as 'killCFG' could edit
        // some of the inputs (e.g. if 'newValue' is a loop-phi of the loop that dies while removing successors).
        AbstractBeginNode[] __successorsArray = __newSuccessors.toArray(new AbstractBeginNode[__newSuccessors.size()]);
        SwitchNode __newSwitch = graph().add(new IntegerSwitchNode(__newValue, __successorsArray, __newKeys, __newKeyProbabilities, __newKeySuccessors));

        // remove dead successors
        for (AbstractBeginNode __successor : __deadSuccessors)
        {
            GraphUtil.killCFG(__successor);
        }

        // replace ourselves with the new switch
        ((FixedWithNextNode) predecessor()).setNext(__newSwitch);
        GraphUtil.killWithUnusedFloatingInputs(this);
    }

    @Override
    public Stamp getValueStampForSuccessor(AbstractBeginNode __beginNode)
    {
        Stamp __result = null;
        if (__beginNode != this.defaultSuccessor())
        {
            for (int __i = 0; __i < keyCount(); __i++)
            {
                if (keySuccessor(__i) == __beginNode)
                {
                    if (__result == null)
                    {
                        __result = StampFactory.forConstant(keyAt(__i));
                    }
                    else
                    {
                        __result = __result.meet(StampFactory.forConstant(keyAt(__i)));
                    }
                }
            }
        }
        return __result;
    }
}
