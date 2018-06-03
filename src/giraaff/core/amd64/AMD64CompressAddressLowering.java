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
    protected final boolean improve(StructuredGraph __graph, AMD64AddressNode __addr, boolean __isBaseNegated, boolean __isIndexNegated)
    {
        if (super.improve(__graph, __addr, __isBaseNegated, __isIndexNegated))
        {
            return true;
        }

        if (!__isBaseNegated && !__isIndexNegated && __addr.getScale() == AMD64Address.Scale.Times1)
        {
            ValueNode __base = __addr.getBase();
            ValueNode __index = __addr.getIndex();

            if (tryToImproveUncompression(__addr, __index, __base) || tryToImproveUncompression(__addr, __base, __index))
            {
                return true;
            }
        }

        return false;
    }

    private boolean tryToImproveUncompression(AMD64AddressNode __addr, ValueNode __value, ValueNode __other)
    {
        if (__value instanceof CompressionNode)
        {
            CompressionNode __compression = (CompressionNode) __value;
            if (__compression.getOp() == CompressionNode.CompressionOp.Uncompress && improveUncompression(__addr, __compression, __other))
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
        // @def
        public static final NodeClass<HeapBaseNode> TYPE = NodeClass.create(HeapBaseNode.class);

        // @field
        private final Register heapBaseRegister;

        // @cons
        public HeapBaseNode(Register __heapBaseRegister)
        {
            super(TYPE, StampFactory.pointer());
            this.heapBaseRegister = __heapBaseRegister;
        }

        @Override
        public void generate(NodeLIRBuilderTool __gen)
        {
            LIRKind __kind = __gen.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
            __gen.setResult(this, heapBaseRegister.asValue(__kind));
        }
    }
}
