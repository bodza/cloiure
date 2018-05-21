package graalvm.compiler.core.amd64;

import jdk.vm.ci.code.Register;
import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.CompressionNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

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

    public static class HeapBaseNode extends FloatingNode implements LIRLowerable
    {
        public static final NodeClass<HeapBaseNode> TYPE = NodeClass.create(HeapBaseNode.class);

        private final Register heapBaseRegister;

        public HeapBaseNode(Register heapBaseRegister)
        {
            super(TYPE, StampFactory.pointer());
            this.heapBaseRegister = heapBaseRegister;
        }

        @Override
        public void generate(NodeLIRBuilderTool generator)
        {
            LIRKind kind = generator.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
            generator.setResult(this, heapBaseRegister.asValue(kind));
        }
    }
}
