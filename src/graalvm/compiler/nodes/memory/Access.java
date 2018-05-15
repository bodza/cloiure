package graalvm.compiler.nodes.memory;

import graalvm.compiler.nodes.extended.GuardedNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.word.LocationIdentity;

public interface Access extends GuardedNode, HeapAccess {

    AddressNode getAddress();

    void setAddress(AddressNode address);

    LocationIdentity getLocationIdentity();

    boolean canNullCheck();

}
