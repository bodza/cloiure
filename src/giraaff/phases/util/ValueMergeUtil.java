package giraaff.phases.util;

import java.util.List;
import java.util.function.Function;

import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;

// @class ValueMergeUtil
public class ValueMergeUtil
{
    public static ValueNode mergeReturns(AbstractMergeNode __merge, List<? extends ReturnNode> __returnNodes)
    {
        return mergeValueProducers(__merge, __returnNodes, null, __returnNode -> __returnNode.result());
    }

    public static <T> ValueNode mergeValueProducers(AbstractMergeNode __merge, List<? extends T> __valueProducers, Function<T, FixedWithNextNode> __lastInstrFunction, Function<T, ValueNode> __valueFunction)
    {
        ValueNode __singleResult = null;
        PhiNode __phiResult = null;
        for (T __valueProducer : __valueProducers)
        {
            ValueNode __result = __valueFunction.apply(__valueProducer);
            if (__result != null)
            {
                if (__phiResult == null && (__singleResult == null || __singleResult == __result))
                {
                    // Only one result value, so no need yet for a phi node.
                    __singleResult = __result;
                }
                else if (__phiResult == null)
                {
                    // Found a second result value, so create phi node.
                    __phiResult = __merge.graph().addWithoutUnique(new ValuePhiNode(__result.stamp(NodeView.DEFAULT).unrestricted(), __merge));
                    for (int __i = 0; __i < __merge.forwardEndCount(); __i++)
                    {
                        __phiResult.addInput(__singleResult);
                    }
                    __phiResult.addInput(__result);
                }
                else
                {
                    // Multiple return values, just add to existing phi node.
                    __phiResult.addInput(__result);
                }
            }

            // create and wire up a new EndNode
            EndNode __endNode = __merge.graph().add(new EndNode());
            __merge.addForwardEnd(__endNode);
            if (__lastInstrFunction == null)
            {
                ((ControlSinkNode) __valueProducer).replaceAndDelete(__endNode);
            }
            else
            {
                FixedWithNextNode __lastInstr = __lastInstrFunction.apply(__valueProducer);
                __lastInstr.setNext(__endNode);
            }
        }

        if (__phiResult != null)
        {
            __phiResult.inferStamp();
            return __phiResult;
        }
        else
        {
            return __singleResult;
        }
    }
}
