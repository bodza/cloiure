package graalvm.compiler.nodes.calc;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_1;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.UnaryOp.Not;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.StampInverter;

/**
 * Binary negation of long or integer values.
 */
@NodeInfo(cycles = CYCLES_1, size = SIZE_1)
public final class NotNode extends UnaryArithmeticNode<Not> implements ArithmeticLIRLowerable, NarrowableArithmeticNode, StampInverter {

    public static final NodeClass<NotNode> TYPE = NodeClass.create(NotNode.class);

    protected NotNode(ValueNode x) {
        super(TYPE, ArithmeticOpTable::getNot, x);
    }

    public static ValueNode create(ValueNode x) {
        return canonicalize(null, x);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue) {
        ValueNode ret = super.canonical(tool, forValue);
        if (ret != this) {
            return ret;
        }
        return canonicalize(this, forValue);
    }

    private static ValueNode canonicalize(NotNode node, ValueNode x) {
        if (x instanceof NotNode) {
            return ((NotNode) x).getValue();
        }
        if (node != null) {
            return node;
        }
        return new NotNode(x);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen) {
        nodeValueMap.setResult(this, gen.emitNot(nodeValueMap.operand(getValue())));
    }

    @Override
    public Stamp invertStamp(Stamp outStamp) {
        return getArithmeticOp().foldStamp(outStamp);
    }
}
