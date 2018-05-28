package giraaff.nodes.extended;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaField;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FrameState;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.StoreFieldNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * Store of a value at a location specified as an offset relative to an object. No null check is
 * performed before the store.
 */
public final class RawStoreNode extends UnsafeAccessNode implements StateSplit, Lowerable, Virtualizable, MemoryCheckpoint.Single
{
    public static final NodeClass<RawStoreNode> TYPE = NodeClass.create(RawStoreNode.class);

    @Input ValueNode value;
    @OptionalInput(InputType.State) FrameState stateAfter;
    private final boolean needsBarrier;

    public RawStoreNode(ValueNode object, ValueNode offset, ValueNode value, JavaKind accessKind, LocationIdentity locationIdentity)
    {
        this(object, offset, value, accessKind, locationIdentity, true);
    }

    public RawStoreNode(ValueNode object, ValueNode offset, ValueNode value, JavaKind accessKind, LocationIdentity locationIdentity, boolean needsBarrier)
    {
        this(object, offset, value, accessKind, locationIdentity, needsBarrier, null, false);
    }

    public RawStoreNode(ValueNode object, ValueNode offset, ValueNode value, JavaKind accessKind, LocationIdentity locationIdentity, boolean needsBarrier, FrameState stateAfter, boolean forceAnyLocation)
    {
        super(TYPE, StampFactory.forVoid(), object, offset, accessKind, locationIdentity, forceAnyLocation);
        this.value = value;
        this.needsBarrier = needsBarrier;
        this.stateAfter = stateAfter;
    }

    @NodeIntrinsic
    public static native Object storeObject(Object object, long offset, Object value, @ConstantNodeParameter JavaKind kind, @ConstantNodeParameter LocationIdentity locationIdentity, @ConstantNodeParameter boolean needsBarrier);

    @NodeIntrinsic
    public static native Object storeChar(Object object, long offset, char value, @ConstantNodeParameter JavaKind kind, @ConstantNodeParameter LocationIdentity locationIdentity);

    public boolean needsBarrier()
    {
        return needsBarrier;
    }

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x)
    {
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    public ValueNode value()
    {
        return value;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(object());
        if (alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode virtual = (VirtualObjectNode) alias;
            ValueNode indexValue = tool.getAlias(offset());
            if (indexValue.isConstant())
            {
                long off = indexValue.asJavaConstant().asLong();
                int entryIndex = virtual.entryIndexForOffset(tool.getArrayOffsetProvider(), off, accessKind());
                if (entryIndex != -1 && tool.setVirtualEntry(virtual, entryIndex, value(), accessKind(), off))
                {
                    tool.delete();
                }
            }
        }
    }

    @Override
    protected ValueNode cloneAsFieldAccess(Assumptions assumptions, ResolvedJavaField field)
    {
        return new StoreFieldNode(object(), field, value(), stateAfter());
    }

    @Override
    protected ValueNode cloneAsArrayAccess(ValueNode location, LocationIdentity identity)
    {
        return new RawStoreNode(object(), location, value, accessKind(), identity, needsBarrier, stateAfter(), isAnyLocationForced());
    }

    public FrameState getState()
    {
        return stateAfter;
    }
}
