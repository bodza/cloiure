package giraaff.phases.common;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.nodes.NodeView;
import giraaff.nodes.PrefetchAllocateNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.JavaReadNode;
import giraaff.nodes.memory.AbstractWriteNode;
import giraaff.nodes.memory.FloatingReadNode;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;

///
// Created by adinn on 09/05/17.
///
// @class AddressLoweringByUsePhase
public final class AddressLoweringByUsePhase extends Phase
{
    // @class AddressLoweringByUsePhase.AddressLoweringByUse
    public abstract static class AddressLoweringByUse
    {
        public abstract AddressNode lower(ValueNode __use, Stamp __stamp, AddressNode __address);

        public abstract AddressNode lower(AddressNode __address);
    }

    // @field
    private final AddressLoweringByUse ___lowering;

    // @cons
    public AddressLoweringByUsePhase(AddressLoweringByUse __lowering)
    {
        super();
        this.___lowering = __lowering;
    }

    @Override
    protected void run(StructuredGraph __graph)
    {
        // first replace address nodes hanging off known usages
        for (Node __node : __graph.getNodes())
        {
            AddressNode __address;
            AddressNode __lowered;
            if (__node instanceof ReadNode)
            {
                ReadNode __readNode = (ReadNode) __node;
                Stamp __stamp = __readNode.stamp(NodeView.DEFAULT);
                __address = __readNode.getAddress();
                __lowered = this.___lowering.lower(__readNode, __stamp, __address);
            }
            else if (__node instanceof JavaReadNode)
            {
                JavaReadNode __javaReadNode = (JavaReadNode) __node;
                Stamp __stamp = __javaReadNode.stamp(NodeView.DEFAULT);
                __address = __javaReadNode.getAddress();
                __lowered = this.___lowering.lower(__javaReadNode, __stamp, __address);
            }
            else if (__node instanceof FloatingReadNode)
            {
                FloatingReadNode __floatingReadNode = (FloatingReadNode) __node;
                Stamp __stamp = __floatingReadNode.stamp(NodeView.DEFAULT);
                __address = __floatingReadNode.getAddress();
                __lowered = this.___lowering.lower(__floatingReadNode, __stamp, __address);
            }
            else if (__node instanceof AbstractWriteNode)
            {
                AbstractWriteNode __abstractWriteNode = (AbstractWriteNode) __node;
                Stamp __stamp = __abstractWriteNode.value().stamp(NodeView.DEFAULT);
                __address = __abstractWriteNode.getAddress();
                __lowered = this.___lowering.lower(__abstractWriteNode, __stamp, __address);
            }
            else if (__node instanceof PrefetchAllocateNode)
            {
                PrefetchAllocateNode __prefetchAllocateNode = (PrefetchAllocateNode) __node;
                Stamp __stamp = StampFactory.forKind(JavaKind.Object);
                __address = (AddressNode) __prefetchAllocateNode.inputs().first();
                __lowered = this.___lowering.lower(__prefetchAllocateNode, __stamp, __address);
            }
            else
            {
                continue;
            }
            // the lowered address may already be a replacement
            // in which case we want to use it not delete it!
            if (__lowered != __address)
            {
                // replace original with lowered at this usage only
                // n.b. lowered is added unique so repeat lowerings will elide
                __node.replaceFirstInput(__address, __lowered);
                // if that was the last reference we can kill the old (dead) node
                if (__address.hasNoUsages())
                {
                    GraphUtil.killWithUnusedFloatingInputs(__address);
                }
            }
        }

        // now replace any remaining unlowered address nodes
        for (Node __node : __graph.getNodes())
        {
            AddressNode __lowered;
            if (__node instanceof OffsetAddressNode)
            {
                AddressNode __address = (AddressNode) __node;
                __lowered = this.___lowering.lower(__address);
            }
            else
            {
                continue;
            }
            // will always be a new AddresNode
            __node.replaceAtUsages(__lowered);
            GraphUtil.killWithUnusedFloatingInputs(__node);
        }
    }
}
