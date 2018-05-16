package graalvm.compiler.hotspot.phases;

import static graalvm.compiler.nodes.ConstantNode.getConstantNodes;

import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.phases.VerifyPhase;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.JavaKind;

/**
 * Checks for {@link #isIllegalObjectConstant(ConstantNode) illegal} object constants in a graph
 * processed for AOT compilation.
 *
 * @see LoadJavaMirrorWithKlassPhase
 */
public class AheadOfTimeVerificationPhase extends VerifyPhase<PhaseContext>
{
    @Override
    protected boolean verify(StructuredGraph graph, PhaseContext context)
    {
        for (ConstantNode node : getConstantNodes(graph))
        {
            if (isIllegalObjectConstant(node))
            {
                throw new VerificationError("illegal object constant: " + node);
            }
        }
        return true;
    }

    public static boolean isIllegalObjectConstant(ConstantNode node)
    {
        return isObject(node) && !isNullReference(node) && !isInternedString(node) && !isDirectMethodHandle(node) && !isBoundMethodHandle(node);
    }

    private static boolean isObject(ConstantNode node)
    {
        return node.getStackKind() == JavaKind.Object;
    }

    private static boolean isNullReference(ConstantNode node)
    {
        return isObject(node) && node.isNullConstant();
    }

    private static boolean isDirectMethodHandle(ConstantNode node)
    {
        if (!isObject(node))
        {
            return false;
        }
        return "Ljava/lang/invoke/DirectMethodHandle;".equals(StampTool.typeOrNull(node).getName());
    }

    private static boolean isBoundMethodHandle(ConstantNode node)
    {
        if (!isObject(node))
        {
            return false;
        }
        return StampTool.typeOrNull(node).getName().startsWith("Ljava/lang/invoke/BoundMethodHandle");
    }

    private static boolean isInternedString(ConstantNode node)
    {
        if (!isObject(node))
        {
            return false;
        }

        HotSpotObjectConstant c = (HotSpotObjectConstant) node.asConstant();
        return c.isInternedString();
    }
}
