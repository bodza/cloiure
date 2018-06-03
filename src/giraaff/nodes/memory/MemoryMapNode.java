package giraaff.nodes.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.StartNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @NodeInfo.allowedUsageTypes "Extension, Memory"
// @class MemoryMapNode
public final class MemoryMapNode extends FloatingNode implements MemoryMap, MemoryNode, LIRLowerable
{
    // @def
    public static final NodeClass<MemoryMapNode> TYPE = NodeClass.create(MemoryMapNode.class);

    // @field
    protected final List<LocationIdentity> ___locationIdentities;
    @Input(InputType.Memory)
    // @field
    NodeInputList<ValueNode> ___nodes;

    // @cons
    public MemoryMapNode(EconomicMap<LocationIdentity, MemoryNode> __mmap)
    {
        super(TYPE, StampFactory.forVoid());
        int __size = __mmap.size();
        this.___locationIdentities = new ArrayList<>(__size);
        this.___nodes = new NodeInputList<>(this, __size);
        int __index = 0;
        MapCursor<LocationIdentity, MemoryNode> __cursor = __mmap.getEntries();
        while (__cursor.advance())
        {
            this.___locationIdentities.add(__cursor.getKey());
            this.___nodes.initialize(__index, (ValueNode) __cursor.getValue());
            __index++;
        }
    }

    public boolean isEmpty()
    {
        if (this.___locationIdentities.isEmpty())
        {
            return true;
        }
        if (this.___locationIdentities.size() == 1)
        {
            if (this.___nodes.get(0) instanceof StartNode)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public MemoryNode getLastLocationAccess(LocationIdentity __locationIdentity)
    {
        if (__locationIdentity.isImmutable())
        {
            return null;
        }
        else
        {
            int __index = this.___locationIdentities.indexOf(__locationIdentity);
            if (__index == -1)
            {
                __index = this.___locationIdentities.indexOf(LocationIdentity.any());
            }
            return (MemoryNode) this.___nodes.get(__index);
        }
    }

    @Override
    public Collection<LocationIdentity> getLocations()
    {
        return this.___locationIdentities;
    }

    public EconomicMap<LocationIdentity, MemoryNode> toMap()
    {
        EconomicMap<LocationIdentity, MemoryNode> __res = EconomicMap.create(Equivalence.DEFAULT, this.___locationIdentities.size());
        for (int __i = 0; __i < this.___nodes.size(); __i++)
        {
            __res.put(this.___locationIdentities.get(__i), (MemoryNode) this.___nodes.get(__i));
        }
        return __res;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // nothing to do...
    }
}
