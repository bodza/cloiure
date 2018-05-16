package graalvm.compiler.replacements.amd64;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.replacements.nodes.UnaryMathIntrinsicNode;
import graalvm.compiler.replacements.nodes.UnaryMathIntrinsicNode.UnaryOperation;

/**
 * Substitutions for some {@link java.lang.Math} methods that leverage AMD64 instructions for
 * selected input values.
 */
@ClassSubstitution(Math.class)
public class AMD64MathSubstitutions
{
    private static final double PI_4 = Math.PI / 4;

    // NOTE on snippets below:
    // Math.sin(), .cos() and .tan() guarantee a value within 1 ULP of the
    // exact result, but x87 trigonometric FPU instructions are only that
    // accurate within [-pi/4, pi/4]. Examine the passed value and provide
    // a slow path for inputs outside of that interval.

    @MethodSubstitution
    public static double sin(double x)
    {
        if (Math.abs(x) < PI_4)
        {
            return UnaryMathIntrinsicNode.compute(x, UnaryOperation.SIN);
        }
        else
        {
            return callDouble1(UnaryOperation.SIN.foreignCallDescriptor, x);
        }
    }

    @MethodSubstitution
    public static double cos(double x)
    {
        if (Math.abs(x) < PI_4)
        {
            return UnaryMathIntrinsicNode.compute(x, UnaryOperation.COS);
        }
        else
        {
            return callDouble1(UnaryOperation.COS.foreignCallDescriptor, x);
        }
    }

    @MethodSubstitution
    public static double tan(double x)
    {
        if (Math.abs(x) < PI_4)
        {
            return UnaryMathIntrinsicNode.compute(x, UnaryOperation.TAN);
        }
        else
        {
            return callDouble1(UnaryOperation.TAN.foreignCallDescriptor, x);
        }
    }

    @NodeIntrinsic(value = ForeignCallNode.class)
    private static native double callDouble1(@ConstantNodeParameter ForeignCallDescriptor descriptor, double value);

    @NodeIntrinsic(value = ForeignCallNode.class)
    private static native double callDouble2(@ConstantNodeParameter ForeignCallDescriptor descriptor, double a, double b);
}
