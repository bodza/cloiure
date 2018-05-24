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
public abstract class SwitchNode extends ControlSplitNode
{
    public static final NodeClass<SwitchNode> TYPE = NodeClass.create(SwitchNode.class);
    @Successor protected NodeSuccessorList<AbstractBeginNode> successors;
    @Input protected ValueNode value;

    // do not change the contents of these arrays:
    protected final double[] keyProbabilities;
    protected final int[] keySuccessors;

    /**
     * Constructs a new Switch.
     *
     * @param value the instruction that provides the value to be switched over
     * @param successors the list of successors of this switch
     */
    protected SwitchNode(NodeClass<? extends SwitchNode> c, ValueNode value, AbstractBeginNode[] successors, int[] keySuccessors, double[] keyProbabilities)
    {
        super(c, StampFactory.forVoid());
        this.successors = new NodeSuccessorList<>(this, successors);
        this.value = value;
        this.keySuccessors = keySuccessors;
        this.keyProbabilities = keyProbabilities;
    }

    private boolean assertProbabilities()
    {
        double total = 0;
        for (double d : keyProbabilities)
        {
            total += d;
        }
        return true;
    }

    @Override
    public int getSuccessorCount()
    {
        return successors.count();
    }

    @Override
    public double probability(AbstractBeginNode successor)
    {
        double sum = 0;
        for (int i = 0; i < keySuccessors.length; i++)
        {
            if (successors.get(keySuccessors[i]) == successor)
            {
                sum += keyProbabilities[i];
            }
        }
        return sum;
    }

    @Override
    public boolean setProbability(AbstractBeginNode successor, double value)
    {
        double sum = 0;
        double otherSum = 0;
        for (int i = 0; i < keySuccessors.length; i++)
        {
            if (successors.get(keySuccessors[i]) == successor)
            {
                sum += keyProbabilities[i];
            }
            else
            {
                otherSum += keyProbabilities[i];
            }
        }

        if (otherSum == 0 || sum == 0)
        {
            // Cannot correctly adjust probabilities.
            return false;
        }

        double delta = value - sum;

        for (int i = 0; i < keySuccessors.length; i++)
        {
            if (successors.get(keySuccessors[i]) == successor)
            {
                keyProbabilities[i] = Math.max(0.0, keyProbabilities[i] + (delta * keyProbabilities[i]) / sum);
            }
            else
            {
                keyProbabilities[i] = Math.max(0.0, keyProbabilities[i] - (delta * keyProbabilities[i]) / otherSum);
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

    public boolean structureEquals(SwitchNode switchNode)
    {
        return Arrays.equals(keySuccessors, switchNode.keySuccessors) && equalKeys(switchNode);
    }

    /**
     * Returns true if the switch has the same keys in the same order as this switch.
     */
    public abstract boolean equalKeys(SwitchNode switchNode);

    /**
     * Returns the index of the successor belonging to the key at the specified index.
     */
    public int keySuccessorIndex(int i)
    {
        return keySuccessors[i];
    }

    /**
     * Returns the successor for the key at the given index.
     */
    public AbstractBeginNode keySuccessor(int i)
    {
        return successors.get(keySuccessors[i]);
    }

    /**
     * Returns the probability of the key at the given index.
     */
    public double keyProbability(int i)
    {
        return keyProbabilities[i];
    }

    /**
     * Returns the index of the default (fall through) successor of this switch.
     */
    public int defaultSuccessorIndex()
    {
        return keySuccessors[keySuccessors.length - 1];
    }

    public AbstractBeginNode blockSuccessor(int i)
    {
        return successors.get(i);
    }

    public void setBlockSuccessor(int i, AbstractBeginNode s)
    {
        successors.set(i, s);
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
    protected void killOtherSuccessors(SimplifierTool tool, int survivingEdge)
    {
        for (Node successor : successors())
        {
            // Deleting a branch change change the successors so reload the surviving successor each time.
            if (successor != blockSuccessor(survivingEdge))
            {
                tool.deleteBranch(successor);
            }
        }
        tool.addToWorkList(blockSuccessor(survivingEdge));
        graph().removeSplit(this, blockSuccessor(survivingEdge));
    }

    public abstract Stamp getValueStampForSuccessor(AbstractBeginNode beginNode);
}