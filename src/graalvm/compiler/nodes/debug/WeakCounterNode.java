package graalvm.compiler.nodes.debug;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

/**
 * This is a special version of the dynamic counter node that removes itself as soon as it's the
 * only usage of the associated node. This way it only increments the counter if the node is
 * actually executed.
 */
@NodeInfo
public final class WeakCounterNode extends DynamicCounterNode implements Simplifiable, Virtualizable
{
    public static final NodeClass<WeakCounterNode> TYPE = NodeClass.create(WeakCounterNode.class);
    @Input ValueNode checkedValue;

    public WeakCounterNode(String group, String name, ValueNode increment, boolean addContext, ValueNode checkedValue)
    {
        super(TYPE, group, name, increment, addContext);
        this.checkedValue = checkedValue;
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        if (checkedValue instanceof FloatingNode && checkedValue.getUsageCount() == 1)
        {
            tool.addToWorkList(checkedValue);
            graph().removeFixed(this);
        }
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(checkedValue);
        if (alias instanceof VirtualObjectNode)
        {
            tool.delete();
        }
    }

    public static void addCounterBefore(String group, String name, long increment, boolean addContext, ValueNode checkedValue, FixedNode position)
    {
        StructuredGraph graph = position.graph();
        WeakCounterNode counter = graph.add(new WeakCounterNode(name, group, ConstantNode.forLong(increment, graph), addContext, checkedValue));
        graph.addBeforeFixed(position, counter);
    }
}
