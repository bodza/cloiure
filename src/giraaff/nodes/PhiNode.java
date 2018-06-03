package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;

///
// {@code PhiNode}s represent the merging of edges at a control flow merges (
// {@link AbstractMergeNode} or {@link LoopBeginNode}). For a {@link AbstractMergeNode}, the order
// of the values corresponds to the order of the ends. For {@link LoopBeginNode}s, the first value
// corresponds to the loop's predecessor, while the rest of the values correspond to the
// {@link LoopEndNode}s.
///
// @class PhiNode
public abstract class PhiNode extends FloatingNode implements Canonicalizable
{
    // @def
    public static final NodeClass<PhiNode> TYPE = NodeClass.create(PhiNode.class);

    @Input(InputType.Association)
    // @field
    protected AbstractMergeNode ___merge;

    // @cons
    protected PhiNode(NodeClass<? extends PhiNode> __c, Stamp __stamp, AbstractMergeNode __merge)
    {
        super(__c, __stamp);
        this.___merge = __merge;
    }

    public abstract NodeInputList<ValueNode> values();

    public AbstractMergeNode merge()
    {
        return this.___merge;
    }

    public void setMerge(AbstractMergeNode __x)
    {
        updateUsages(this.___merge, __x);
        this.___merge = __x;
    }

    ///
    // Get the instruction that produces the value associated with the i'th predecessor of the merge.
    //
    // @param i the index of the predecessor
    // @return the instruction that produced the value in the i'th predecessor
    ///
    public ValueNode valueAt(int __i)
    {
        return values().get(__i);
    }

    ///
    // Sets the value at the given index and makes sure that the values list is large enough.
    //
    // @param i the index at which to set the value
    // @param x the new phi input value for the given location
    ///
    public void initializeValueAt(int __i, ValueNode __x)
    {
        while (values().size() <= __i)
        {
            values().add(null);
        }
        values().set(__i, __x);
    }

    public void setValueAt(int __i, ValueNode __x)
    {
        values().set(__i, __x);
    }

    public void setValueAt(AbstractEndNode __end, ValueNode __x)
    {
        setValueAt(merge().phiPredecessorIndex(__end), __x);
    }

    public ValueNode valueAt(AbstractEndNode __pred)
    {
        return valueAt(merge().phiPredecessorIndex(__pred));
    }

    ///
    // Get the number of inputs to this phi (i.e. the number of predecessors to the merge).
    //
    // @return the number of inputs in this phi
    ///
    public int valueCount()
    {
        return values().size();
    }

    public void clearValues()
    {
        values().clear();
    }

    public void addInput(ValueNode __x)
    {
        values().add(__x);
    }

    public void removeInput(int __index)
    {
        values().remove(__index);
    }

    public NodeIterable<ValueNode> backValues()
    {
        return values().subList(merge().forwardEndCount());
    }

    ///
    // If all inputs are the same value, this value is returned, otherwise {@code this}. Note that
    // {@code null} is a valid return value, since {@link GuardPhiNode}s can have {@code null} inputs.
    ///
    public ValueNode singleValueOrThis()
    {
        ValueNode __singleValue = valueAt(0);
        int __count = valueCount();
        for (int __i = 1; __i < __count; ++__i)
        {
            ValueNode __value = valueAt(__i);
            if (__value != this)
            {
                if (__value != __singleValue)
                {
                    return this;
                }
            }
        }
        return __singleValue;
    }

    ///
    // If all inputs (but the first one) are the same value, the value is returned, otherwise
    // {@code this}. Note that {@code null} is a valid return value, since {@link GuardPhiNode}s can
    // have {@code null} inputs.
    ///
    public ValueNode singleBackValueOrThis()
    {
        int __valueCount = valueCount();
        // Skip first value, assume second value as single value.
        ValueNode __singleValue = valueAt(1);
        for (int __i = 2; __i < __valueCount; ++__i)
        {
            ValueNode __value = valueAt(__i);
            if (__value != __singleValue)
            {
                return this;
            }
        }
        return __singleValue;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool)
    {
        if (isLoopPhi())
        {
            int __valueCount = valueCount();
            int __i;
            for (__i = 1; __i < __valueCount; ++__i)
            {
                ValueNode __value = valueAt(__i);
                if (__value != this)
                {
                    break;
                }
            }

            // All back edges are self-references => return forward edge input value.
            if (__i == __valueCount)
            {
                return firstValue();
            }

            boolean __onlySelfUsage = true;
            for (Node __n : this.usages())
            {
                if (__n != this)
                {
                    __onlySelfUsage = false;
                    break;
                }
            }

            if (__onlySelfUsage)
            {
                return null;
            }
        }

        return singleValueOrThis();
    }

    public ValueNode firstValue()
    {
        return valueAt(0);
    }

    public boolean isLoopPhi()
    {
        return merge() instanceof LoopBeginNode;
    }
}
