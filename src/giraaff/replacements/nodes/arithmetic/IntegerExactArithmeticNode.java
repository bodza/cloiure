package giraaff.replacements.nodes.arithmetic;

import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.spi.Lowerable;

// @iface IntegerExactArithmeticNode
public interface IntegerExactArithmeticNode extends Lowerable
{
    IntegerExactArithmeticSplitNode createSplit(AbstractBeginNode next, AbstractBeginNode deopt);
}
