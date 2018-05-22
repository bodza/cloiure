package graalvm.compiler.nodes.spi;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.nodes.memory.MemoryNode;

public interface MemoryProxy extends Proxy, MemoryNode
{
    LocationIdentity getLocationIdentity();

    MemoryNode getOriginalMemoryNode();
}
