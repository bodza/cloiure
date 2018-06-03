package giraaff.nodes.extended;

import java.util.Arrays;

import jdk.vm.ci.meta.Constant;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeSuccessorList;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.ValueNode;
import giraaff.util.GraalError;

/**
 * The {@code SwitchNode} class is the base of both lookup and table switches.
 */
// @class SwitchNode
public abstract class SwitchNode extends ControlSplitNode
{
    // @def
    public static final NodeClass<SwitchNode> TYPE = NodeClass.create(SwitchNode.class);

    @Successor
    // @field
    protected NodeSuccessorList<AbstractBeginNode> successors;
    @Input
    // @field
    protected ValueNode value;

    // do not change the contents of these arrays:
    // @field
    protected final double[] keyProbabilities;
    // @field
    protected final int[] keySuccessors;

    /**
     * Constructs a new Switch.
     *
     * @param value the instruction that provides the value to be switched over
     * @param successors the list of successors of this switch
     */
    // @cons
    protected SwitchNode(NodeClass<? extends SwitchNode> __c, ValueNode __value, AbstractBeginNode[] __successors, int[] __keySuccessors, double[] __keyProbabilities)
    {
        super(__c, StampFactory.forVoid());
        this.successors = new NodeSuccessorList<>(this, __successors);
        this.value = __value;
        this.keySuccessors = __keySuccessors;
        this.keyProbabilities = __keyProbabilities;
    }

    private boolean assertProbabilities()
    {
        double __total = 0;
        for (double __d : keyProbabilities)
        {
            __total += __d;
        }
        return true;
    }

    @Override
    public int getSuccessorCount()
    {
        return successors.count();
    }

    @Override
    public double probability(AbstractBeginNode __successor)
    {
        double __sum = 0;
        for (int __i = 0; __i < keySuccessors.length; __i++)
        {
            if (successors.get(keySuccessors[__i]) == __successor)
            {
                __sum += keyProbabilities[__i];
            }
        }
        return __sum;
    }

    @Override
    public boolean setProbability(AbstractBeginNode __successor, double __value)
    {
        double __sum = 0;
        double __otherSum = 0;
        for (int __i = 0; __i < keySuccessors.length; __i++)
        {
            if (successors.get(keySuccessors[__i]) == __successor)
            {
                __sum += keyProbabilities[__i];
            }
            else
            {
                __otherSum += keyProbabilities[__i];
            }
        }

        if (__otherSum == 0 || __sum == 0)
        {
            // Cannot correctly adjust probabilities.
            return false;
        }

        double __delta = __value - __sum;

        for (int __i = 0; __i < keySuccessors.length; __i++)
        {
            if (successors.get(keySuccessors[__i]) == __successor)
            {
                keyProbabilities[__i] = Math.max(0.0, keyProbabilities[__i] + (__delta * keyProbabilities[__i]) / __sum);
            }
            else
            {
                keyProbabilities[__i] = Math.max(0.0, keyProbabilities[__i] - (__delta * keyProbabilities[__i]) / __otherSum);
            }
        }
        return true;
    }

    public ValueNode value()
    {
        return value;
    }

    public abstract boolean isSorted();

    /**
     * The number of distinct keys in this switch.
     */
    public abstract int keyCount();

    /**
     * The key at the specified position, encoded in a Constant.
     */
    public abstract Constant keyAt(int i);

    public boolean structureEquals(SwitchNode __switchNode)
    {
        return Arrays.equals(keySuccessors, __switchNode.keySuccessors) && equalKeys(__switchNode);
    }

    /**
     * Returns true if the switch has the same keys in the same order as this switch.
     */
    public abstract boolean equalKeys(SwitchNode switchNode);

    /**
     * Returns the index of the successor belonging to the key at the specified index.
     */
    public int keySuccessorIndex(int __i)
    {
        return keySuccessors[__i];
    }

    /**
     * Returns the successor for the key at the given index.
     */
    public AbstractBeginNode keySuccessor(int __i)
    {
        return successors.get(keySuccessors[__i]);
    }

    /**
     * Returns the probability of the key at the given index.
     */
    public double keyProbability(int __i)
    {
        return keyProbabilities[__i];
    }

    /**
     * Returns the index of the default (fall through) successor of this switch.
     */
    public int defaultSuccessorIndex()
    {
        return keySuccessors[keySuccessors.length - 1];
    }

    public AbstractBeginNode blockSuccessor(int __i)
    {
        return successors.get(__i);
    }

    public void setBlockSuccessor(int __i, AbstractBeginNode __s)
    {
        successors.set(__i, __s);
    }

    public int blockSuccessorCount()
    {
        return successors.count();
    }

    /**
     * Gets the successor corresponding to the default (fall through) case.
     *
     * @return the default successor
     */
    public AbstractBeginNode defaultSuccessor()
    {
        if (defaultSuccessorIndex() == -1)
        {
            throw new GraalError("unexpected");
        }
        return successors.get(defaultSuccessorIndex());
    }

    @Override
    public AbstractBeginNode getPrimarySuccessor()
    {
        return null;
    }

    /**
     * Delete all other successors except for the one reached by {@code survivingEdge}.
     *
     * @param survivingEdge index of the edge in the {@link SwitchNode#successors} list
     */
    protected void killOtherSuccessors(SimplifierTool __tool, int __survivingEdge)
    {
        for (Node __successor : successors())
        {
            // Deleting a branch change change the successors so reload the surviving successor each time.
            if (__successor != blockSuccessor(__survivingEdge))
            {
                __tool.deleteBranch(__successor);
            }
        }
        __tool.addToWorkList(blockSuccessor(__survivingEdge));
        graph().removeSplit(this, blockSuccessor(__survivingEdge));
    }

    public abstract Stamp getValueStampForSuccessor(AbstractBeginNode beginNode);
}
