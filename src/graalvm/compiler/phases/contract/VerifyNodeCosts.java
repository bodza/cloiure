package graalvm.compiler.phases.contract;

import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeCycles;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodeinfo.NodeSize;
import graalvm.compiler.phases.VerifyPhase;

/**
 * Utility class that verifies that every {@link Class} extending {@link Node} specifies non default
 * values for {@link NodeCycles} and {@link NodeSize} in its {@link NodeInfo} annotation.
 */
public class VerifyNodeCosts
{
    public static void verifyNodeClass(Class<?> clazz)
    {
        Class<?> nodeClass = Node.class;
        if (nodeClass.isAssignableFrom(clazz))
        {
            if (!clazz.isAnnotationPresent(NodeInfo.class))
            {
                throw new VerifyPhase.VerificationError("%s.java extends Node.java but does not specify a NodeInfo annotation.", clazz.getName());
            }

            if (!Modifier.isAbstract(clazz.getModifiers()))
            {
                boolean cyclesSet = walkCHUntil(getType(clazz), getType(nodeClass), cur ->
                {
                    return cur.cycles() != NodeCycles.CYCLES_UNSET;
                });
                boolean sizeSet = walkCHUntil(getType(clazz), getType(nodeClass), cur ->
                {
                    return cur.size() != NodeSize.SIZE_UNSET;
                });
                if (!cyclesSet)
                {
                    throw new VerifyPhase.VerificationError("%s.java does not specify a NodeCycles value in its class hierarchy.", clazz.getName());
                }
                if (!sizeSet)
                {
                    throw new VerifyPhase.VerificationError("%s.java does not specify a NodeSize value in its class hierarchy.", clazz.getName());
                }
            }
        }
    }

    private static NodeClass<?> getType(Class<?> c)
    {
        try
        {
            return NodeClass.get(c);
        }
        catch (Throwable t)
        {
            throw new VerifyPhase.VerificationError("%s.java does not specify a TYPE field.", c.getName());
        }
    }

    private static boolean walkCHUntil(NodeClass<?> start, NodeClass<?> until, Predicate<NodeClass<?>> p)
    {
        NodeClass<?> cur = start;
        while (cur != until && cur != null)
        {
            if (p.test(cur))
            {
                return true;
            }
            cur = cur.getSuperNodeClass();
        }
        return false;
    }
}
