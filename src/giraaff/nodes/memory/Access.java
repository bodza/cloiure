package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.nodes.extended.GuardedNode;
import giraaff.nodes.memory.address.AddressNode;

// @iface Access
public interface Access extends GuardedNode, HeapAccess
{
    AddressNode getAddress();

    void setAddress(AddressNode __address);

    LocationIdentity getLocationIdentity();

    boolean canNullCheck();
}
