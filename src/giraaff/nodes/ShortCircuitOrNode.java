package giraaff.nodes;

import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.Stamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.spi.ValueProxy;

// @class ShortCircuitOrNode
public final class ShortCircuitOrNode extends LogicNode implements IterableNodeType, Canonicalizable.Binary<LogicNode>
{
    // @def
    public static final NodeClass<ShortCircuitOrNode> TYPE = NodeClass.create(ShortCircuitOrNode.class);

    @Input(InputType.Condition)
    // @field
    LogicNode x;
    @Input(InputType.Condition)
    // @field
    LogicNode y;
    // @field
    protected boolean xNegated;
    // @field
    protected boolean yNegated;
    // @field
    protected double shortCircuitProbability;

    // @cons
    public ShortCircuitOrNode(LogicNode __x, boolean __xNegated, LogicNode __y, boolean __yNegated, double __shortCircuitProbability)
    {
        super(TYPE);
        this.x = __x;
        this.xNegated = __xNegated;
        this.y = __y;
        this.yNegated = __yNegated;
        this.shortCircuitProbability = __shortCircuitProbability;
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

    protected ShortCircuitOrNode canonicalizeNegation(LogicNode __forX, LogicNode __forY)
    {
        LogicNode __xCond = __forX;
        boolean __xNeg = xNegated;
        while (__xCond instanceof LogicNegationNode)
        {
            __xCond = ((LogicNegationNode) __xCond).getValue();
            __xNeg = !__xNeg;
        }

        LogicNode __yCond = __forY;
        boolean __yNeg = yNegated;
        while (__yCond instanceof LogicNegationNode)
        {
            __yCond = ((LogicNegationNode) __yCond).getValue();
            __yNeg = !__yNeg;
        }

        if (__xCond != __forX || __yCond != __forY)
        {
            return new ShortCircuitOrNode(__xCond, __xNeg, __yCond, __yNeg, shortCircuitProbability);
        }
        else
        {
            return this;
        }
    }

    @Override
    public LogicNode canonical(CanonicalizerTool __tool, LogicNode __forX, LogicNode __forY)
    {
        ShortCircuitOrNode __ret = canonicalizeNegation(__forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        if (__forX == __forY)
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
                    return LogicNegationNode.create(__forX);
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
                    return __forX;
                }
            }
        }
        if (__forX instanceof LogicConstantNode)
        {
            if (((LogicConstantNode) __forX).getValue() ^ isXNegated())
            {
                return LogicConstantNode.tautology();
            }
            else
            {
                if (isYNegated())
                {
                    return new LogicNegationNode(__forY);
                }
                else
                {
                    return __forY;
                }
            }
        }
        if (__forY instanceof LogicConstantNode)
        {
            if (((LogicConstantNode) __forY).getValue() ^ isYNegated())
            {
                return LogicConstantNode.tautology();
            }
            else
            {
                if (isXNegated())
                {
                    return new LogicNegationNode(__forX);
                }
                else
                {
                    return __forX;
                }
            }
        }

        if (__forX instanceof ShortCircuitOrNode)
        {
            ShortCircuitOrNode __inner = (ShortCircuitOrNode) __forX;
            if (__forY == __inner.getX())
            {
                return optimizeShortCircuit(__inner, this.xNegated, this.yNegated, true);
            }
            else if (__forY == __inner.getY())
            {
                return optimizeShortCircuit(__inner, this.xNegated, this.yNegated, false);
            }
        }
        else if (__forY instanceof ShortCircuitOrNode)
        {
            ShortCircuitOrNode __inner = (ShortCircuitOrNode) __forY;
            if (__inner.getX() == __forX)
            {
                return optimizeShortCircuit(__inner, this.yNegated, this.xNegated, true);
            }
            else if (__inner.getY() == __forX)
            {
                return optimizeShortCircuit(__inner, this.yNegated, this.xNegated, false);
            }
        }

        // check whether !X => Y constant
        if (__forX instanceof UnaryOpLogicNode && __forY instanceof UnaryOpLogicNode)
        {
            UnaryOpLogicNode __unaryX = (UnaryOpLogicNode) __forX;
            UnaryOpLogicNode __unaryY = (UnaryOpLogicNode) __forY;
            if (skipThroughPisAndProxies(__unaryX.getValue()) == skipThroughPisAndProxies(__unaryY.getValue()))
            {
                // !X => Y is constant
                Stamp __succStamp = __unaryX.getSucceedingStampForValue(!isXNegated());
                TriState __fold = __unaryY.tryFold(__succStamp);
                if (__fold.isKnown())
                {
                    boolean __yResult = __fold.toBoolean() ^ isYNegated();
                    return __yResult
                                    ? LogicConstantNode.tautology()
                                    : (isXNegated()
                                                    ? LogicNegationNode.create(__forX)
                                                    : __forX);
                }
            }
        }

        return this;
    }

    private static ValueNode skipThroughPisAndProxies(ValueNode __node)
    {
        for (ValueNode __n = __node; __n != null; )
        {
            if (__n instanceof PiNode)
            {
                __n = ((PiNode) __n).getOriginalNode();
            }
            else if (__n instanceof ValueProxy)
            {
                __n = ((ValueProxy) __n).getOriginalNode();
            }
            else
            {
                return __n;
            }
        }
        return null;
    }

    private static LogicNode optimizeShortCircuit(ShortCircuitOrNode __inner, boolean __innerNegated, boolean __matchNegated, boolean __matchIsInnerX)
    {
        boolean __innerMatchNegated;
        if (__matchIsInnerX)
        {
            __innerMatchNegated = __inner.isXNegated();
        }
        else
        {
            __innerMatchNegated = __inner.isYNegated();
        }
        if (!__innerNegated)
        {
            // The four digit results of the expression used in the 16 subsequent formula
            // comments correspond to results when using the following truth table for
            // inputs a and b and testing all 4 possible input combinations:
            // _ 1234
            // a 1100
            // b 1010
            if (__innerMatchNegated == __matchNegated)
            {
                // ( (!a ||!b) ||!a) => 0111 (!a ||!b)
                // ( (!a || b) ||!a) => 1011 (!a || b)
                // ( ( a ||!b) || a) => 1101 ( a ||!b)
                // ( ( a || b) || a) => 1110 ( a || b)
                // Only the inner or is relevant, the outer or never adds information.
                return __inner;
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
            if (__innerMatchNegated == __matchNegated)
            {
                // (!(!a ||!b) ||!a) => 1011 (!a || b)
                // (!(!a || b) ||!a) => 0111 (!a ||!b)
                // (!( a ||!b) || a) => 1110 ( a || b)
                // (!( a || b) || a) => 1101 ( a ||!b)
                boolean __newInnerXNegated = __inner.isXNegated();
                boolean __newInnerYNegated = __inner.isYNegated();
                double __newProbability = __inner.getShortCircuitProbability();
                if (__matchIsInnerX)
                {
                    __newInnerYNegated = !__newInnerYNegated;
                }
                else
                {
                    __newInnerXNegated = !__newInnerXNegated;
                    __newProbability = 1.0 - __newProbability;
                }
                // The expression can be transformed into a single or.
                return new ShortCircuitOrNode(__inner.getX(), __newInnerXNegated, __inner.getY(), __newInnerYNegated, __newProbability);
            }
            else
            {
                // (!(!a ||!b) || a) => 1100 (a)
                // (!(!a || b) || a) => 1100 (a)
                // (!( a ||!b) ||!a) => 0011 (!a)
                // (!( a || b) ||!a) => 0011 (!a)
                LogicNode __result = __inner.getY();
                if (__matchIsInnerX)
                {
                    __result = __inner.getX();
                }
                // Only the second part of the outer or is relevant.
                if (__matchNegated)
                {
                    return LogicNegationNode.create(__result);
                }
                else
                {
                    return __result;
                }
            }
        }
    }
}
