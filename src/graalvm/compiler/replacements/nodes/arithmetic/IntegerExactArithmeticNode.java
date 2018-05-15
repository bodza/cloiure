package graalvm.compiler.replacements.nodes.arithmetic;

import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.spi.Lowerable;

public interface IntegerExactArithmeticNode extends Lowerable {

    IntegerExactArithmeticSplitNode createSplit(AbstractBeginNode next, AbstractBeginNode deopt);
}
