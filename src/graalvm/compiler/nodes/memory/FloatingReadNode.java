package graalvm.compiler.nodes.memory;

import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNodeUtil;
import graalvm.compiler.nodes.ValuePhiNode;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

/**
 * A floating read of a value from memory specified in terms of an object base and an object
 * relative location. This node does not null check the object.
 */
@NodeInfo(nameTemplate = "Read#{p#location/s}", cycles = CYCLES_2, size = SIZE_1)
public final class FloatingReadNode extends FloatingAccessNode implements LIRLowerableAccess, Canonicalizable {
    public static final NodeClass<FloatingReadNode> TYPE = NodeClass.create(FloatingReadNode.class);

    @OptionalInput(Memory) MemoryNode lastLocationAccess;

    public FloatingReadNode(AddressNode address, LocationIdentity location, MemoryNode lastLocationAccess, Stamp stamp) {
        this(address, location, lastLocationAccess, stamp, null, BarrierType.NONE);
    }

    public FloatingReadNode(AddressNode address, LocationIdentity location, MemoryNode lastLocationAccess, Stamp stamp, GuardingNode guard) {
        this(address, location, lastLocationAccess, stamp, guard, BarrierType.NONE);
    }

    public FloatingReadNode(AddressNode address, LocationIdentity location, MemoryNode lastLocationAccess, Stamp stamp, GuardingNode guard, BarrierType barrierType) {
        super(TYPE, address, location, stamp, guard, barrierType);
        this.lastLocationAccess = lastLocationAccess;

        // The input to floating reads must be always non-null or have at least a guard.
        assert guard != null || !(address.getBase().stamp(NodeView.DEFAULT) instanceof ObjectStamp) || address.getBase() instanceof ValuePhiNode ||
                        ((ObjectStamp) address.getBase().stamp(NodeView.DEFAULT)).nonNull() : address.getBase();
    }

    @Override
    public MemoryNode getLastLocationAccess() {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode newlla) {
        updateUsages(ValueNodeUtil.asNode(lastLocationAccess), ValueNodeUtil.asNode(newlla));
        lastLocationAccess = newlla;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        LIRKind readKind = gen.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        gen.setResult(this, gen.getLIRGeneratorTool().getArithmetic().emitLoad(readKind, gen.operand(address), null));
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        Node result = ReadNode.canonicalizeRead(this, getAddress(), getLocationIdentity(), tool);
        if (result != this) {
            return result;
        }
        if (tool.canonicalizeReads() && getAddress().hasMoreThanOneUsage() && lastLocationAccess instanceof WriteNode) {
            WriteNode write = (WriteNode) lastLocationAccess;
            if (write.getAddress() == getAddress() && write.getAccessStamp().isCompatible(getAccessStamp())) {
                // Same memory location with no intervening write
                return write.value();
            }
        }
        return this;
    }

    @SuppressWarnings("try")
    @Override
    public FixedAccessNode asFixedNode() {
        try (DebugCloseable position = withNodeSourcePosition()) {
            ReadNode result = graph().add(new ReadNode(getAddress(), getLocationIdentity(), stamp(NodeView.DEFAULT), getBarrierType()));
            result.setGuard(getGuard());
            return result;
        }
    }

    @Override
    public boolean verify() {
        MemoryNode lla = getLastLocationAccess();
        assert lla != null || getLocationIdentity().isImmutable() : "lastLocationAccess of " + this + " shouldn't be null for mutable location identity " + getLocationIdentity();
        return super.verify();
    }

    @Override
    public Stamp getAccessStamp() {
        return stamp(NodeView.DEFAULT);
    }
}
