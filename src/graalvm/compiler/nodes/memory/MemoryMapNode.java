package graalvm.compiler.nodes.memory;

import static graalvm.compiler.nodeinfo.InputType.Extension;
import static graalvm.compiler.nodeinfo.InputType.Memory;
import static org.graalvm.word.LocationIdentity.any;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.nodes.StartNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

public final class MemoryMapNode extends FloatingNode implements MemoryMap, MemoryNode, LIRLowerable
{
    public static final NodeClass<MemoryMapNode> TYPE = NodeClass.create(MemoryMapNode.class);
    protected final List<LocationIdentity> locationIdentities;
    @Input(Memory) NodeInputList<ValueNode> nodes;

    public MemoryMapNode(EconomicMap<LocationIdentity, MemoryNode> mmap)
    {
        super(TYPE, StampFactory.forVoid());
        int size = mmap.size();
        locationIdentities = new ArrayList<>(size);
        nodes = new NodeInputList<>(this, size);
        int index = 0;
        MapCursor<LocationIdentity, MemoryNode> cursor = mmap.getEntries();
        while (cursor.advance())
        {
            locationIdentities.add(cursor.getKey());
            nodes.initialize(index, (ValueNode) cursor.getValue());
            index++;
        }
    }

    public boolean isEmpty()
    {
        if (locationIdentities.isEmpty())
        {
            return true;
        }
        if (locationIdentities.size() == 1)
        {
            if (nodes.get(0) instanceof StartNode)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public MemoryNode getLastLocationAccess(LocationIdentity locationIdentity)
    {
        if (locationIdentity.isImmutable())
        {
            return null;
        }
        else
        {
            int index = locationIdentities.indexOf(locationIdentity);
            if (index == -1)
            {
                index = locationIdentities.indexOf(any());
            }
            return (MemoryNode) nodes.get(index);
        }
    }

    @Override
    public Collection<LocationIdentity> getLocations()
    {
        return locationIdentities;
    }

    public EconomicMap<LocationIdentity, MemoryNode> toMap()
    {
        EconomicMap<LocationIdentity, MemoryNode> res = EconomicMap.create(Equivalence.DEFAULT, locationIdentities.size());
        for (int i = 0; i < nodes.size(); i++)
        {
            res.put(locationIdentities.get(i), (MemoryNode) nodes.get(i));
        }
        return res;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        // nothing to do...
    }
}
