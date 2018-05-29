package giraaff.nodes.spi;

import org.graalvm.word.LocationIdentity;

import giraaff.nodes.memory.MemoryNode;

// @iface MemoryProxy
public interface MemoryProxy extends Proxy, MemoryNode
{
    LocationIdentity getLocationIdentity();

    MemoryNode getOriginalMemoryNode();
}
