package giraaff.nodes;

import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.Stamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.spi.ValueProxy;

public final class ShortCircuitOrNode extends LogicNode implements IterableNodeType, Canonicalizable.Binary<LogicNode>
{
    public static final NodeClass<ShortCircuitOrNode> TYPE = NodeClass.create(ShortCircuitOrNode.class);
    @Input(InputType.Condition) LogicNode x;
    @Input(InputType.Condition) LogicNode y;
    protected boolean xNegated;
    protected boolean yNegated;
    protected double shortCircuitProbability;

    public ShortCircuitOrNode(LogicNode x, boolean xNegated, LogicNode y, boolean yNegated, double shortCircuitProbability)
    {
        super(TYPE);
        this.x = x;
        this.xNegated = xNegated;
        this.y = y;
        this.yNegated = yNegated;
        this.shortCircuitProbability = shortCircuitProbability;
    }

    @Override
    public LogicNode getX()
    {
        return x;
    }

    @Override
    public LogicNode getY()
    {
        return y;
    }

    public boolean isXNegated()
    {
        return xNegated;
    }

    public boolean isYNegated()
    {
        return yNegated;
    }

    /**
     * Gets the probability that the {@link #getY() y} part of this binary node is <b>not</b>
     * evaluated. This is the probability that this operator will short-circuit its execution.
     */
    public double getShortCircuitProbability()
    {
        return shortCircuitProbability;
    }

    protected ShortCircuitOrNode canonicalizeNegation(LogicNode forX, LogicNode forY)
    {
        LogicNode xCond = forX;
        boolean xNeg = xNegated;
        while (xCond instanceof LogicNegationNode)
        {
            xCond = ((LogicNegationNode) xCond).getValue();
            xNeg = !xNeg;
        }

        LogicNode yCond = forY;
        boolean yNeg = yNegated;
        while (yCond instanceof LogicNegationNode)
        {
            yCond = ((LogicNegationNode) yCond).getValue();
            yNeg = !yNeg;
        }

        if (xCond != forX || yCond != forY)
        {
            return new ShortCircuitOrNode(xCond, xNeg, yCond, yNeg, shortCircuitProbability);
        }
        else
        {
            return this;
        }
    }

    @Override
    public LogicNode canonical(CanonicalizerTool tool, LogicNode forX, LogicNode forY)
    {
        ShortCircuitOrNode ret = canonicalizeNegation(forX, forY);
        if (ret != this)
        {
            return ret;
        }

        if (forX == forY)
        {
            //  a ||  a = a
            //  a || !a = true
            // !a ||  a = true
            // !a || !a = !a
            if (isXNegated())
            {
                if (isYNegated())
                {
                    // !a || !a = !a
                    return LogicNegationNode.create(forX);
                }
                else
                {
                    // !a || a = true
                    return LogicConstantNode.tautology();
                }
            }
            else
            {
                if (isYNegated())
                {
                    // a || !a = true
                    return LogicConstantNode.tautology();
                }
                else
                {
                    // a || a = a
                    return forX;
                }
            }
        }
        if (forX instanceof LogicConstantNode)
        {
            if (((LogicConstantNode) forX).getValue() ^ isXNegated())
            {
                return LogicConstantNode.tautology();
            }
            else
            {
                if (isYNegated())
                {
                    return new LogicNegationNode(forY);
                }
                else
                {
                    return forY;
                }
            }
        }
        if (forY instanceof LogicConstantNode)
        {
            if (((LogicConstantNode) forY).getValue() ^ isYNegated())
            {
                return LogicConstantNode.tautology();
            }
            else
            {
                if (isXNegated())
                {
                    return new LogicNegationNode(forX);
                }
                else
                {
                    return forX;
                }
            }
        }

        if (forX instanceof ShortCircuitOrNode)
        {
            ShortCircuitOrNode inner = (ShortCircuitOrNode) forX;
            if (forY == inner.getX())
            {
                return optimizeShortCircuit(inner, this.xNegated, this.yNegated, true);
            }
            else if (forY == inner.getY())
            {
                return optimizeShortCircuit(inner, this.xNegated, this.yNegated, false);
            }
        }
        else if (forY instanceof ShortCircuitOrNode)
        {
            ShortCircuitOrNode inner = (ShortCircuitOrNode) forY;
            if (inner.getX() == forX)
            {
                return optimizeShortCircuit(inner, this.yNegated, this.xNegated, true);
            }
            else if (inner.getY() == forX)
            {
                return optimizeShortCircuit(inner, this.yNegated, this.xNegated, false);
            }
        }

        // check whether !X => Y constant
        if (forX instanceof UnaryOpLogicNode && forY instanceof UnaryOpLogicNode)
        {
            UnaryOpLogicNode unaryX = (UnaryOpLogicNode) forX;
            UnaryOpLogicNode unaryY = (UnaryOpLogicNode) forY;
            if (skipThroughPisAndProxies(unaryX.getValue()) == skipThroughPisAndProxies(unaryY.getValue()))
            {
                // !X => Y is constant
                Stamp succStamp = unaryX.getSucceedingStampForValue(!isXNegated());
                TriState fold = unaryY.tryFold(succStamp);
                if (fold.isKnown())
                {
                    boolean yResult = fold.toBoolean() ^ isYNegated();
                    return yResult
                                    ? LogicConstantNode.tautology()
                                    : (isXNegated()
                                                    ? LogicNegationNode.create(forX)
                                                    : forX);
                }
            }
        }

        return this;
    }

    private static ValueNode skipThroughPisAndProxies(ValueNode node)
    {
        for (ValueNode n = node; n != null; )
        {
            if (n instanceof PiNode)
            {
                n = ((PiNode) n).getOriginalNode();
            }
            else if (n instanceof ValueProxy)
            {
                n = ((ValueProxy) n).getOriginalNode();
            }
            else
            {
                return n;
            }
        }
        return null;
    }

    private static LogicNode optimizeShortCircuit(ShortCircuitOrNode inner, boolean innerNegated, boolean matchNegated, boolean matchIsInnerX)
    {
        boolean innerMatchNegated;
        if (matchIsInnerX)
        {
            innerMatchNegated = inner.isXNegated();
        }
        else
        {
            innerMatchNegated = inner.isYNegated();
        }
        if (!innerNegated)
        {
            // The four digit results of the expression used in the 16 subsequent formula comments
            // correspond to results when using the following truth table for inputs a and b
            // and testing all 4 possible input combinations:
            // _ 1234
            // a 1100
            // b 1010
            if (innerMatchNegated == matchNegated)
            {
                // ( (!a ||!b) ||!a) => 0111 (!a ||!b)
                // ( (!a || b) ||!a) => 1011 (!a || b)
                // ( ( a ||!b) || a) => 1101 ( a ||!b)
                // ( ( a || b) || a) => 1110 ( a || b)
                // Only the inner or is relevant, the outer or never adds information.
                return inner;
            }
            else
            {
                // ( ( a || b) ||!a) => 1111 (true)
                // ( (!a ||!b) || a) => 1111 (true)
                // ( (!a || b) || a) => 1111 (true)
                // ( ( a ||!b) ||!a) => 1111 (true)
                // The result of the expression is always true.
                return LogicConstantNode.tautology();
            }
        }
        else
        {
            if (innerMatchNegated == matchNegated)
            {
                // (!(!a ||!b) ||!a) => 1011 (!a || b)
                // (!(!a || b) ||!a) => 0111 (!a ||!b)
                // (!( a ||!b) || a) => 1110 ( a || b)
                // (!( a || b) || a) => 1101 ( a ||!b)
                boolean newInnerXNegated = inner.isXNegated();
                boolean newInnerYNegated = inner.isYNegated();
                double newProbability = inner.getShortCircuitProbability();
                if (matchIsInnerX)
                {
                    newInnerYNegated = !newInnerYNegated;
                }
                else
                {
                    newInnerXNegated = !newInnerXNegated;
                    newProbability = 1.0 - newProbability;
                }
                // The expression can be transformed into a single or.
                return new ShortCircuitOrNode(inner.getX(), newInnerXNegated, inner.getY(), newInnerYNegated, newProbability);
            }
            else
            {
                // (!(!a ||!b) || a) => 1100 (a)
                // (!(!a || b) || a) => 1100 (a)
                // (!( a ||!b) ||!a) => 0011 (!a)
                // (!( a || b) ||!a) => 0011 (!a)
                LogicNode result = inner.getY();
                if (matchIsInnerX)
                {
                    result = inner.getX();
                }
                // Only the second part of the outer or is relevant.
                if (matchNegated)
                {
                    return LogicNegationNode.create(result);
                }
                else
                {
                    return result;
                }
            }
        }
    }
}
