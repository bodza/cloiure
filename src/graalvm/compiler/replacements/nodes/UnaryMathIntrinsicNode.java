package graalvm.compiler.replacements.nodes;

import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.core.common.type.FloatStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.UnaryNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

public final class UnaryMathIntrinsicNode extends UnaryNode implements ArithmeticLIRLowerable, Lowerable
{
    public static final NodeClass<UnaryMathIntrinsicNode> TYPE = NodeClass.create(UnaryMathIntrinsicNode.class);
    protected final UnaryOperation operation;

    public enum UnaryOperation
    {
        LOG(new ForeignCallDescriptor("arithmeticLog", double.class, double.class)),
        LOG10(new ForeignCallDescriptor("arithmeticLog10", double.class, double.class)),
        SIN(new ForeignCallDescriptor("arithmeticSin", double.class, double.class)),
        COS(new ForeignCallDescriptor("arithmeticCos", double.class, double.class)),
        TAN(new ForeignCallDescriptor("arithmeticTan", double.class, double.class)),
        EXP(new ForeignCallDescriptor("arithmeticExp", double.class, double.class));

        public final ForeignCallDescriptor foreignCallDescriptor;

        UnaryOperation(ForeignCallDescriptor foreignCallDescriptor)
        {
            this.foreignCallDescriptor = foreignCallDescriptor;
        }

        public double compute(double value)
        {
            switch (this)
            {
                case LOG:
                    return Math.log(value);
                case LOG10:
                    return Math.log10(value);
                case EXP:
                    return Math.exp(value);
                case SIN:
                    return Math.sin(value);
                case COS:
                    return Math.cos(value);
                case TAN:
                    return Math.tan(value);
                default:
                    throw new GraalError("unknown op %s", this);
            }
        }
    }

    public UnaryOperation getOperation()
    {
        return operation;
    }

    public static ValueNode create(ValueNode value, UnaryOperation op)
    {
        ValueNode c = tryConstantFold(value, op);
        if (c != null)
        {
            return c;
        }
        return new UnaryMathIntrinsicNode(value, op);
    }

    protected static ValueNode tryConstantFold(ValueNode value, UnaryOperation op)
    {
        if (value.isConstant())
        {
            return ConstantNode.forDouble(op.compute(value.asJavaConstant().asDouble()));
        }
        return null;
    }

    protected UnaryMathIntrinsicNode(ValueNode value, UnaryOperation op)
    {
        super(TYPE, computeStamp(value.stamp(NodeView.DEFAULT), op), value);
        this.operation = op;
    }

    @Override
    public Stamp foldStamp(Stamp valueStamp)
    {
        return computeStamp(valueStamp, getOperation());
    }

    static Stamp computeStamp(Stamp valueStamp, UnaryOperation op)
    {
        if (valueStamp instanceof FloatStamp)
        {
            FloatStamp floatStamp = (FloatStamp) valueStamp;
            switch (op)
            {
                case COS:
                case SIN:
                {
                    boolean nonNaN = floatStamp.lowerBound() != Double.NEGATIVE_INFINITY && floatStamp.upperBound() != Double.POSITIVE_INFINITY && floatStamp.isNonNaN();
                    return StampFactory.forFloat(JavaKind.Double, -1.0, 1.0, nonNaN);
                }
                case TAN:
                {
                    boolean nonNaN = floatStamp.lowerBound() != Double.NEGATIVE_INFINITY && floatStamp.upperBound() != Double.POSITIVE_INFINITY && floatStamp.isNonNaN();
                    return StampFactory.forFloat(JavaKind.Double, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, nonNaN);
                }
                case LOG:
                case LOG10:
                {
                    double lowerBound = op.compute(floatStamp.lowerBound());
                    double upperBound = op.compute(floatStamp.upperBound());
                    if (floatStamp.contains(0.0))
                    {
                        // 0.0 and -0.0 infinity produces -Inf
                        lowerBound = Double.NEGATIVE_INFINITY;
                    }
                    boolean nonNaN = floatStamp.lowerBound() >= 0.0 && floatStamp.isNonNaN();
                    return StampFactory.forFloat(JavaKind.Double, lowerBound, upperBound, nonNaN);
                }
                case EXP:
                {
                    double lowerBound = Math.exp(floatStamp.lowerBound());
                    double upperBound = Math.exp(floatStamp.upperBound());
                    boolean nonNaN = floatStamp.isNonNaN();
                    return StampFactory.forFloat(JavaKind.Double, lowerBound, upperBound, nonNaN);
                }
            }
        }
        return StampFactory.forKind(JavaKind.Double);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        Value input = nodeValueMap.operand(getValue());
        Value result;
        switch (getOperation())
        {
            case LOG:
                result = gen.emitMathLog(input, false);
                break;
            case LOG10:
                result = gen.emitMathLog(input, true);
                break;
            case EXP:
                result = gen.emitMathExp(input);
                break;
            case SIN:
                result = gen.emitMathSin(input);
                break;
            case COS:
                result = gen.emitMathCos(input);
                break;
            case TAN:
                result = gen.emitMathTan(input);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        nodeValueMap.setResult(this, result);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        ValueNode c = tryConstantFold(forValue, getOperation());
        if (c != null)
        {
            return c;
        }
        return this;
    }

    @NodeIntrinsic
    public static native double compute(double value, @ConstantNodeParameter UnaryOperation op);
}
