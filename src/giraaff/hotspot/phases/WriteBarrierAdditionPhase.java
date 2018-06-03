package giraaff.hotspot.phases;

import giraaff.graph.Node;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.G1ArrayRangePostWriteBarrier;
import giraaff.hotspot.nodes.G1ArrayRangePreWriteBarrier;
import giraaff.hotspot.nodes.G1PostWriteBarrier;
import giraaff.hotspot.nodes.G1PreWriteBarrier;
import giraaff.hotspot.nodes.G1ReferentFieldReadBarrier;
import giraaff.hotspot.nodes.SerialArrayRangeWriteBarrier;
import giraaff.hotspot.nodes.SerialWriteBarrier;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.ArrayRangeWrite;
import giraaff.nodes.java.AbstractCompareAndSwapNode;
import giraaff.nodes.java.LoweredAtomicReadAndWriteNode;
import giraaff.nodes.memory.FixedAccessNode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.WriteNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.type.StampTool;
import giraaff.phases.Phase;
import giraaff.util.GraalError;

// @class WriteBarrierAdditionPhase
public final class WriteBarrierAdditionPhase extends Phase
{
    // @cons
    public WriteBarrierAdditionPhase()
    {
        super();
    }

    @Override
    protected void run(StructuredGraph __graph)
    {
        for (Node __n : __graph.getNodes())
        {
            if (__n instanceof ReadNode)
            {
                addReadNodeBarriers((ReadNode) __n, __graph);
            }
            else if (__n instanceof WriteNode)
            {
                addWriteNodeBarriers((WriteNode) __n, __graph);
            }
            else if (__n instanceof LoweredAtomicReadAndWriteNode)
            {
                addAtomicReadWriteNodeBarriers((LoweredAtomicReadAndWriteNode) __n, __graph);
            }
            else if (__n instanceof AbstractCompareAndSwapNode)
            {
                addCASBarriers((AbstractCompareAndSwapNode) __n, __graph);
            }
            else if (__n instanceof ArrayRangeWrite)
            {
                ArrayRangeWrite __node = (ArrayRangeWrite) __n;
                if (__node.writesObjectArray())
                {
                    addArrayRangeBarriers(__node, __graph);
                }
            }
        }
    }

    private void addReadNodeBarriers(ReadNode __node, StructuredGraph __graph)
    {
        if (__node.getBarrierType() == BarrierType.PRECISE)
        {
            G1ReferentFieldReadBarrier __barrier = __graph.add(new G1ReferentFieldReadBarrier(__node.getAddress(), __node, false));
            __graph.addAfterFixed(__node, __barrier);
        }
    }

    protected static void addG1PreWriteBarrier(FixedAccessNode __node, AddressNode __address, ValueNode __value, boolean __doLoad, boolean __nullCheck, StructuredGraph __graph)
    {
        G1PreWriteBarrier __preBarrier = __graph.add(new G1PreWriteBarrier(__address, __value, __doLoad, __nullCheck));
        __preBarrier.setStateBefore(__node.stateBefore());
        __node.setNullCheck(false);
        __node.setStateBefore(null);
        __graph.addBeforeFixed(__node, __preBarrier);
    }

    protected void addG1PostWriteBarrier(FixedAccessNode __node, AddressNode __address, ValueNode __value, boolean __precise, StructuredGraph __graph)
    {
        final boolean __alwaysNull = StampTool.isPointerAlwaysNull(__value);
        __graph.addAfterFixed(__node, __graph.add(new G1PostWriteBarrier(__address, __value, __precise, __alwaysNull)));
    }

    protected void addSerialPostWriteBarrier(FixedAccessNode __node, AddressNode __address, ValueNode __value, boolean __precise, StructuredGraph __graph)
    {
        final boolean __alwaysNull = StampTool.isPointerAlwaysNull(__value);
        if (__alwaysNull)
        {
            // serial barrier isn't needed for null value
            return;
        }
        __graph.addAfterFixed(__node, __graph.add(new SerialWriteBarrier(__address, __precise)));
    }

    private void addWriteNodeBarriers(WriteNode __node, StructuredGraph __graph)
    {
        BarrierType __barrierType = __node.getBarrierType();
        switch (__barrierType)
        {
            case NONE:
                // nothing to do
                break;
            case IMPRECISE:
            case PRECISE:
            {
                boolean __precise = __barrierType == BarrierType.PRECISE;
                if (HotSpotRuntime.useG1GC)
                {
                    if (!__node.getLocationIdentity().isInit())
                    {
                        addG1PreWriteBarrier(__node, __node.getAddress(), null, true, __node.getNullCheck(), __graph);
                    }
                    addG1PostWriteBarrier(__node, __node.getAddress(), __node.value(), __precise, __graph);
                }
                else
                {
                    addSerialPostWriteBarrier(__node, __node.getAddress(), __node.value(), __precise, __graph);
                }
                break;
            }
            default:
                throw new GraalError("unexpected barrier type: " + __barrierType);
        }
    }

    private void addAtomicReadWriteNodeBarriers(LoweredAtomicReadAndWriteNode __node, StructuredGraph __graph)
    {
        BarrierType __barrierType = __node.getBarrierType();
        switch (__barrierType)
        {
            case NONE:
                // nothing to do
                break;
            case IMPRECISE:
            case PRECISE:
            {
                boolean __precise = __barrierType == BarrierType.PRECISE;
                if (HotSpotRuntime.useG1GC)
                {
                    addG1PreWriteBarrier(__node, __node.getAddress(), null, true, __node.getNullCheck(), __graph);
                    addG1PostWriteBarrier(__node, __node.getAddress(), __node.getNewValue(), __precise, __graph);
                }
                else
                {
                    addSerialPostWriteBarrier(__node, __node.getAddress(), __node.getNewValue(), __precise, __graph);
                }
                break;
            }
            default:
                throw new GraalError("unexpected barrier type: " + __barrierType);
        }
    }

    private void addCASBarriers(AbstractCompareAndSwapNode __node, StructuredGraph __graph)
    {
        BarrierType __barrierType = __node.getBarrierType();
        switch (__barrierType)
        {
            case NONE:
                // nothing to do
                break;
            case IMPRECISE:
            case PRECISE:
            {
                boolean __precise = __barrierType == BarrierType.PRECISE;
                if (HotSpotRuntime.useG1GC)
                {
                    addG1PreWriteBarrier(__node, __node.getAddress(), __node.getExpectedValue(), false, false, __graph);
                    addG1PostWriteBarrier(__node, __node.getAddress(), __node.getNewValue(), __precise, __graph);
                }
                else
                {
                    addSerialPostWriteBarrier(__node, __node.getAddress(), __node.getNewValue(), __precise, __graph);
                }
                break;
            }
            default:
                throw new GraalError("unexpected barrier type: " + __barrierType);
        }
    }

    private void addArrayRangeBarriers(ArrayRangeWrite __write, StructuredGraph __graph)
    {
        if (HotSpotRuntime.useG1GC)
        {
            if (!__write.isInitialization())
            {
                G1ArrayRangePreWriteBarrier __g1ArrayRangePreWriteBarrier = __graph.add(new G1ArrayRangePreWriteBarrier(__write.getAddress(), __write.getLength(), __write.getElementStride()));
                __graph.addBeforeFixed(__write.asNode(), __g1ArrayRangePreWriteBarrier);
            }
            G1ArrayRangePostWriteBarrier __g1ArrayRangePostWriteBarrier = __graph.add(new G1ArrayRangePostWriteBarrier(__write.getAddress(), __write.getLength(), __write.getElementStride()));
            __graph.addAfterFixed(__write.asNode(), __g1ArrayRangePostWriteBarrier);
        }
        else
        {
            SerialArrayRangeWriteBarrier __serialArrayRangeWriteBarrier = __graph.add(new SerialArrayRangeWriteBarrier(__write.getAddress(), __write.getLength(), __write.getElementStride()));
            __graph.addAfterFixed(__write.asNode(), __serialArrayRangeWriteBarrier);
        }
    }
}
