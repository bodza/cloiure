package giraaff.replacements.nodes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.BinaryNode;
import giraaff.nodes.calc.FloatDivNode;
import giraaff.nodes.calc.MulNode;
import giraaff.nodes.calc.SqrtNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.util.GraalError;

public final class BinaryMathIntrinsicNode extends BinaryNode implements ArithmeticLIRLowerable, Lowerable
{
    public static final NodeClass<BinaryMathIntrinsicNode> TYPE = NodeClass.create(BinaryMathIntrinsicNode.class);
    protected final BinaryOperation operation;

    public enum BinaryOperation
    {
        POW(new ForeignCallDescriptor("arithmeticPow", double.class, double.class, double.class));

        public final ForeignCallDescriptor foreignCallDescriptor;

        BinaryOperation(ForeignCallDescriptor foreignCallDescriptor)
        {
            this.foreignCallDescriptor = foreignCallDescriptor;
        }
    }

    public BinaryOperation getOperation()
    {
        return operation;
    }

    public static ValueNode create(ValueNode forX, ValueNode forY, BinaryOperation op)
    {
        ValueNode c = tryConstantFold(forX, forY, op);
        if (c != null)
        {
            return c;
        }
        return new BinaryMathIntrinsicNode(forX, forY, op);
    }

    protected static ValueNode tryConstantFold(ValueNode forX, ValueNode forY, BinaryOperation op)
    {
        if (forX.isConstant() && forY.isConstant())
        {
            double ret = doCompute(forX.asJavaConstant().asDouble(), forY.asJavaConstant().asDouble(), op);
            return ConstantNode.forDouble(ret);
        }
        return null;
    }

    @Override
    public Stamp foldStamp(Stamp stampX, Stamp stampY)
    {
        return stamp(NodeView.DEFAULT);
    }

    protected BinaryMathIntrinsicNode(ValueNode forX, ValueNode forY, BinaryOperation op)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Double), forX, forY);
        this.operation = op;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        Value xValue = nodeValueMap.operand(getX());
        Value yValue = nodeValueMap.operand(getY());
        Value result;
        switch (getOperation())
        {
            case POW:
                result = gen.emitMathPow(xValue, yValue);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        nodeValueMap.setResult(this, result);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode c = tryConstantFold(forX, forY, getOperation());
        if (c != null)
        {
            return c;
        }
        if (forY.isConstant())
        {
            double yValue = forY.asJavaConstant().asDouble();
            // If the second argument is positive or negative zero, then the result is 1.0.
            if (yValue == 0.0D)
            {
                return ConstantNode.forDouble(1);
            }

            // If the second argument is 1.0, then the result is the same as the first argument.
            if (yValue == 1.0D)
            {
                return x;
            }

            // If the second argument is NaN, then the result is NaN.
            if (Double.isNaN(yValue))
            {
                return ConstantNode.forDouble(Double.NaN);
            }

            // x**-1 = 1/x
            if (yValue == -1.0D)
            {
                return new FloatDivNode(ConstantNode.forDouble(1), x);
            }

            // x**2 = x*x
            if (yValue == 2.0D)
            {
                return new MulNode(x, x);
            }

            // x**0.5 = sqrt(x)
            if (yValue == 0.5D && x.stamp(view) instanceof FloatStamp && ((FloatStamp) x.stamp(view)).lowerBound() >= 0.0D)
            {
                return SqrtNode.create(x, view);
            }
        }
        return this;
    }

    @NodeIntrinsic
    public static native double compute(double x, double y, @ConstantNodeParameter BinaryOperation op);

    private static double doCompute(double x, double y, BinaryOperation op)
    {
        switch (op)
        {
            case POW:
                return Math.pow(x, y);
            default:
                throw new GraalError("unknown op %s", op);
        }
    }
}