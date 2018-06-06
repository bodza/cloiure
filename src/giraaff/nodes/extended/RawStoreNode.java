package giraaff.nodes.extended;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaField;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
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

///
// Store of a value at a location specified as an offset relative to an object. No null check is
// performed before the store.
///
// @class RawStoreNode
public final class RawStoreNode extends UnsafeAccessNode implements StateSplit, Lowerable, Virtualizable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<RawStoreNode> TYPE = NodeClass.create(RawStoreNode.class);

    @Node.Input
    // @field
    ValueNode ___value;
    @Node.OptionalInput(InputType.StateI)
    // @field
    FrameState ___stateAfter;
    // @field
    private final boolean ___needsBarrier;

    // @cons RawStoreNode
    public RawStoreNode(ValueNode __object, ValueNode __offset, ValueNode __value, JavaKind __accessKind, LocationIdentity __locationIdentity)
    {
        this(__object, __offset, __value, __accessKind, __locationIdentity, true);
    }

    // @cons RawStoreNode
    public RawStoreNode(ValueNode __object, ValueNode __offset, ValueNode __value, JavaKind __accessKind, LocationIdentity __locationIdentity, boolean __needsBarrier)
    {
        this(__object, __offset, __value, __accessKind, __locationIdentity, __needsBarrier, null, false);
    }

    // @cons RawStoreNode
    public RawStoreNode(ValueNode __object, ValueNode __offset, ValueNode __value, JavaKind __accessKind, LocationIdentity __locationIdentity, boolean __needsBarrier, FrameState __stateAfter, boolean __forceAnyLocation)
    {
        super(TYPE, StampFactory.forVoid(), __object, __offset, __accessKind, __locationIdentity, __forceAnyLocation);
        this.___value = __value;
        this.___needsBarrier = __needsBarrier;
        this.___stateAfter = __stateAfter;
    }

    @Node.NodeIntrinsic
    public static native Object storeObject(Object __object, long __offset, Object __value, @Node.ConstantNodeParameter JavaKind __kind, @Node.ConstantNodeParameter LocationIdentity __locationIdentity, @Node.ConstantNodeParameter boolean __needsBarrier);

    @Node.NodeIntrinsic
    public static native Object storeChar(Object __object, long __offset, char __value, @Node.ConstantNodeParameter JavaKind __kind, @Node.ConstantNodeParameter LocationIdentity __locationIdentity);

    public boolean needsBarrier()
    {
        return this.___needsBarrier;
    }

    @Override
    public FrameState stateAfter()
    {
        return this.___stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(this.___stateAfter, __x);
        this.___stateAfter = __x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    public ValueNode value()
    {
        return this.___value;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(object());
        if (__alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
            ValueNode __indexValue = __tool.getAlias(offset());
            if (__indexValue.isConstant())
            {
                long __off = __indexValue.asJavaConstant().asLong();
                int __entryIndex = __virtual.entryIndexForOffset(__tool.getArrayOffsetProvider(), __off, accessKind());
                if (__entryIndex != -1 && __tool.setVirtualEntry(__virtual, __entryIndex, value(), accessKind(), __off))
                {
                    __tool.delete();
                }
            }
        }
    }

    @Override
    protected ValueNode cloneAsFieldAccess(Assumptions __assumptions, ResolvedJavaField __field)
    {
        return new StoreFieldNode(object(), __field, value(), stateAfter());
    }

    @Override
    protected ValueNode cloneAsArrayAccess(ValueNode __location, LocationIdentity __identity)
    {
        return new RawStoreNode(object(), __location, this.___value, accessKind(), __identity, this.___needsBarrier, stateAfter(), isAnyLocationForced());
    }

    public FrameState getState()
    {
        return this.___stateAfter;
    }
}
