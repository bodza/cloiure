package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.nodes.extended.GuardedNode;
import giraaff.nodes.memory.address.AddressNode;

public interface Access extends GuardedNode, HeapAccess
{
    AddressNode getAddress();

    void setAddress(AddressNode address);

    LocationIdentity getLocationIdentity();

    boolean canNullCheck();
}
