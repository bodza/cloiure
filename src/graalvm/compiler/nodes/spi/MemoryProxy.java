package graalvm.compiler.nodes.spi;

import graalvm.compiler.nodes.memory.MemoryNode;
import org.graalvm.word.LocationIdentity;

public interface MemoryProxy extends Proxy, MemoryNode
{
    LocationIdentity getLocationIdentity();

    MemoryNode getOriginalMemoryNode();
}
