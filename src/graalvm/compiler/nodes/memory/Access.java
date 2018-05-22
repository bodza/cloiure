package graalvm.compiler.nodes.memory;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.nodes.extended.GuardedNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

public interface Access extends GuardedNode, HeapAccess
{
    AddressNode getAddress();

    void setAddress(AddressNode address);

    LocationIdentity getLocationIdentity();

    boolean canNullCheck();
}
