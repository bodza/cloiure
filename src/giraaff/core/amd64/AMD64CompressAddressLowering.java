package giraaff.core.amd64;

import jdk.vm.ci.code.Register;

import giraaff.asm.amd64.AMD64Address;
import giraaff.core.common.LIRKind;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.CompressionNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class AMD64CompressAddressLowering
public abstract class AMD64CompressAddressLowering extends AMD64AddressLowering
{
    @Override
    protected final boolean improve(StructuredGraph graph, AMD64AddressNode addr, boolean isBaseNegated, boolean isIndexNegated)
    {
        if (super.improve(graph, addr, isBaseNegated, isIndexNegated))
        {
            return true;
        }

        if (!isBaseNegated && !isIndexNegated && addr.getScale() == AMD64Address.Scale.Times1)
        {
            ValueNode base = addr.getBase();
            ValueNode index = addr.getIndex();

            if (tryToImproveUncompression(addr, index, base) || tryToImproveUncompression(addr, base, index))
            {
                return true;
            }
        }

        return false;
    }

    private boolean tryToImproveUncompression(AMD64AddressNode addr, ValueNode value, ValueNode other)
    {
        if (value instanceof CompressionNode)
        {
            CompressionNode compression = (CompressionNode) value;
            if (compression.getOp() == CompressionNode.CompressionOp.Uncompress && improveUncompression(addr, compression, other))
            {
                return true;
            }
        }

        return false;
    }

    protected abstract boolean improveUncompression(AMD64AddressNode addr, CompressionNode compression, ValueNode other);

    // @class AMD64CompressAddressLowering.HeapBaseNode
    public static final class HeapBaseNode extends FloatingNode implements LIRLowerable
    {
        public static final NodeClass<HeapBaseNode> TYPE = NodeClass.create(HeapBaseNode.class);

        private final Register heapBaseRegister;

        // @cons
        public HeapBaseNode(Register heapBaseRegister)
        {
            super(TYPE, StampFactory.pointer());
            this.heapBaseRegister = heapBaseRegister;
        }

        @Override
        public void generate(NodeLIRBuilderTool gen)
        {
            LIRKind kind = gen.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
            gen.setResult(this, heapBaseRegister.asValue(kind));
        }
    }
}
