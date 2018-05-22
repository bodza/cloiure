package graalvm.compiler.phases.common;

import jdk.vm.ci.meta.JavaKind;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.PrefetchAllocateNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.JavaReadNode;
import graalvm.compiler.nodes.memory.AbstractWriteNode;
import graalvm.compiler.nodes.memory.FloatingReadNode;
import graalvm.compiler.nodes.memory.ReadNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.memory.address.OffsetAddressNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.phases.Phase;

/**
 * Created by adinn on 09/05/17.
 */
public class AddressLoweringByUsePhase extends Phase
{
    public abstract static class AddressLoweringByUse
    {
        public abstract AddressNode lower(ValueNode use, Stamp stamp, AddressNode address);

        public abstract AddressNode lower(AddressNode address);
    }

    private final AddressLoweringByUse lowering;

    public AddressLoweringByUsePhase(AddressLoweringByUse lowering)
    {
        this.lowering = lowering;
    }

    @Override
    protected void run(StructuredGraph graph)
    {
        // first replace address nodes hanging off known usages
        for (Node node : graph.getNodes())
        {
            AddressNode address;
            AddressNode lowered;
            if (node instanceof ReadNode)
            {
                ReadNode readNode = (ReadNode) node;
                Stamp stamp = readNode.stamp(NodeView.DEFAULT);
                address = readNode.getAddress();
                lowered = lowering.lower(readNode, stamp, address);
            }
            else if (node instanceof JavaReadNode)
            {
                JavaReadNode javaReadNode = (JavaReadNode) node;
                Stamp stamp = javaReadNode.stamp(NodeView.DEFAULT);
                address = javaReadNode.getAddress();
                lowered = lowering.lower(javaReadNode, stamp, address);
            }
            else if (node instanceof FloatingReadNode)
            {
                FloatingReadNode floatingReadNode = (FloatingReadNode) node;
                Stamp stamp = floatingReadNode.stamp(NodeView.DEFAULT);
                address = floatingReadNode.getAddress();
                lowered = lowering.lower(floatingReadNode, stamp, address);
            }
            else if (node instanceof AbstractWriteNode)
            {
                AbstractWriteNode abstractWriteNode = (AbstractWriteNode) node;
                Stamp stamp = abstractWriteNode.value().stamp(NodeView.DEFAULT);
                address = abstractWriteNode.getAddress();
                lowered = lowering.lower(abstractWriteNode, stamp, address);
            }
            else if (node instanceof PrefetchAllocateNode)
            {
                PrefetchAllocateNode prefetchAllocateNode = (PrefetchAllocateNode) node;
                Stamp stamp = StampFactory.forKind(JavaKind.Object);
                address = (AddressNode) prefetchAllocateNode.inputs().first();
                lowered = lowering.lower(prefetchAllocateNode, stamp, address);
            }
            else
            {
                continue;
            }
            // the lowered address may already be a replacement
            // in which case we want to use it not delete it!
            if (lowered != address)
            {
                // replace original with lowered at this usage only
                // n.b. lowered is added unique so repeat lowerings will elide
                node.replaceFirstInput(address, lowered);
                // if that was the last reference we can kill the old (dead) node
                if (address.hasNoUsages())
                {
                    GraphUtil.killWithUnusedFloatingInputs(address);
                }
            }
        }

        // now replace any remaining unlowered address nodes
        for (Node node : graph.getNodes())
        {
            AddressNode lowered;
            if (node instanceof OffsetAddressNode)
            {
                AddressNode address = (AddressNode) node;
                lowered = lowering.lower(address);
            }
            else
            {
                continue;
            }
            // will always be a new AddresNode
            node.replaceAtUsages(lowered);
            GraphUtil.killWithUnusedFloatingInputs(node);
        }
    }
}
