package giraaff.replacements.nodes.arithmetic;

import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.spi.Lowerable;

public interface IntegerExactArithmeticNode extends Lowerable
{
    IntegerExactArithmeticSplitNode createSplit(AbstractBeginNode next, AbstractBeginNode deopt);
}
