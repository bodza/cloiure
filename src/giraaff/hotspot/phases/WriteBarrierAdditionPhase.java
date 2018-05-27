package giraaff.hotspot.phases;

import giraaff.graph.Node;
import giraaff.hotspot.GraalHotSpotVMConfig;
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

public class WriteBarrierAdditionPhase extends Phase
{
    private GraalHotSpotVMConfig config;

    public WriteBarrierAdditionPhase(GraalHotSpotVMConfig config)
    {
        this.config = config;
    }

    @Override
    protected void run(StructuredGraph graph)
    {
        for (Node n : graph.getNodes())
        {
            if (n instanceof ReadNode)
            {
                addReadNodeBarriers((ReadNode) n, graph);
            }
            else if (n instanceof WriteNode)
            {
                addWriteNodeBarriers((WriteNode) n, graph);
            }
            else if (n instanceof LoweredAtomicReadAndWriteNode)
            {
                addAtomicReadWriteNodeBarriers((LoweredAtomicReadAndWriteNode) n, graph);
            }
            else if (n instanceof AbstractCompareAndSwapNode)
            {
                addCASBarriers((AbstractCompareAndSwapNode) n, graph);
            }
            else if (n instanceof ArrayRangeWrite)
            {
                ArrayRangeWrite node = (ArrayRangeWrite) n;
                if (node.writesObjectArray())
                {
                    addArrayRangeBarriers(node, graph);
                }
            }
        }
    }

    private void addReadNodeBarriers(ReadNode node, StructuredGraph graph)
    {
        if (node.getBarrierType() == BarrierType.PRECISE)
        {
            G1ReferentFieldReadBarrier barrier = graph.add(new G1ReferentFieldReadBarrier(node.getAddress(), node, false));
            graph.addAfterFixed(node, barrier);
        }
    }

    protected static void addG1PreWriteBarrier(FixedAccessNode node, AddressNode address, ValueNode value, boolean doLoad, boolean nullCheck, StructuredGraph graph)
    {
        G1PreWriteBarrier preBarrier = graph.add(new G1PreWriteBarrier(address, value, doLoad, nullCheck));
        preBarrier.setStateBefore(node.stateBefore());
        node.setNullCheck(false);
        node.setStateBefore(null);
        graph.addBeforeFixed(node, preBarrier);
    }

    protected void addG1PostWriteBarrier(FixedAccessNode node, AddressNode address, ValueNode value, boolean precise, StructuredGraph graph)
    {
        final boolean alwaysNull = StampTool.isPointerAlwaysNull(value);
        graph.addAfterFixed(node, graph.add(new G1PostWriteBarrier(address, value, precise, alwaysNull)));
    }

    protected void addSerialPostWriteBarrier(FixedAccessNode node, AddressNode address, ValueNode value, boolean precise, StructuredGraph graph)
    {
        final boolean alwaysNull = StampTool.isPointerAlwaysNull(value);
        if (alwaysNull)
        {
            // serial barrier isn't needed for null value
            return;
        }
        graph.addAfterFixed(node, graph.add(new SerialWriteBarrier(address, precise)));
    }

    private void addWriteNodeBarriers(WriteNode node, StructuredGraph graph)
    {
        BarrierType barrierType = node.getBarrierType();
        switch (barrierType)
        {
            case NONE:
                // nothing to do
                break;
            case IMPRECISE:
            case PRECISE:
                boolean precise = barrierType == BarrierType.PRECISE;
                if (config.useG1GC)
                {
                    if (!node.getLocationIdentity().isInit())
                    {
                        addG1PreWriteBarrier(node, node.getAddress(), null, true, node.getNullCheck(), graph);
                    }
                    addG1PostWriteBarrier(node, node.getAddress(), node.value(), precise, graph);
                }
                else
                {
                    addSerialPostWriteBarrier(node, node.getAddress(), node.value(), precise, graph);
                }
                break;
            default:
                throw new GraalError("unexpected barrier type: " + barrierType);
        }
    }

    private void addAtomicReadWriteNodeBarriers(LoweredAtomicReadAndWriteNode node, StructuredGraph graph)
    {
        BarrierType barrierType = node.getBarrierType();
        switch (barrierType)
        {
            case NONE:
                // nothing to do
                break;
            case IMPRECISE:
            case PRECISE:
                boolean precise = barrierType == BarrierType.PRECISE;
                if (config.useG1GC)
                {
                    addG1PreWriteBarrier(node, node.getAddress(), null, true, node.getNullCheck(), graph);
                    addG1PostWriteBarrier(node, node.getAddress(), node.getNewValue(), precise, graph);
                }
                else
                {
                    addSerialPostWriteBarrier(node, node.getAddress(), node.getNewValue(), precise, graph);
                }
                break;
            default:
                throw new GraalError("unexpected barrier type: " + barrierType);
        }
    }

    private void addCASBarriers(AbstractCompareAndSwapNode node, StructuredGraph graph)
    {
        BarrierType barrierType = node.getBarrierType();
        switch (barrierType)
        {
            case NONE:
                // nothing to do
                break;
            case IMPRECISE:
            case PRECISE:
                boolean precise = barrierType == BarrierType.PRECISE;
                if (config.useG1GC)
                {
                    addG1PreWriteBarrier(node, node.getAddress(), node.getExpectedValue(), false, false, graph);
                    addG1PostWriteBarrier(node, node.getAddress(), node.getNewValue(), precise, graph);
                }
                else
                {
                    addSerialPostWriteBarrier(node, node.getAddress(), node.getNewValue(), precise, graph);
                }
                break;
            default:
                throw new GraalError("unexpected barrier type: " + barrierType);
        }
    }

    private void addArrayRangeBarriers(ArrayRangeWrite write, StructuredGraph graph)
    {
        if (config.useG1GC)
        {
            if (!write.isInitialization())
            {
                G1ArrayRangePreWriteBarrier g1ArrayRangePreWriteBarrier = graph.add(new G1ArrayRangePreWriteBarrier(write.getAddress(), write.getLength(), write.getElementStride()));
                graph.addBeforeFixed(write.asNode(), g1ArrayRangePreWriteBarrier);
            }
            G1ArrayRangePostWriteBarrier g1ArrayRangePostWriteBarrier = graph.add(new G1ArrayRangePostWriteBarrier(write.getAddress(), write.getLength(), write.getElementStride()));
            graph.addAfterFixed(write.asNode(), g1ArrayRangePostWriteBarrier);
        }
        else
        {
            SerialArrayRangeWriteBarrier serialArrayRangeWriteBarrier = graph.add(new SerialArrayRangeWriteBarrier(write.getAddress(), write.getLength(), write.getElementStride()));
            graph.addAfterFixed(write.asNode(), serialArrayRangeWriteBarrier);
        }
    }
}
