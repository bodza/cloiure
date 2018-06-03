package giraaff.virtual.phases.ea;

import java.util.List;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.spi.ArrayOffsetProvider;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.graph.Node;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.calc.UnpackEndianHalfNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualInstanceNode;
import giraaff.nodes.virtual.VirtualObjectNode;

///
// Forwards calls from {@link VirtualizerTool} to the actual {@link PartialEscapeBlockState}.
///
// @class VirtualizerToolImpl
final class VirtualizerToolImpl implements VirtualizerTool, CanonicalizerTool
{
    // @field
    private final MetaAccessProvider ___metaAccess;
    // @field
    private final ConstantReflectionProvider ___constantReflection;
    // @field
    private final ConstantFieldProvider ___constantFieldProvider;
    // @field
    private final PartialEscapeClosure<?> ___closure;
    // @field
    private final Assumptions ___assumptions;
    // @field
    private final LoweringProvider ___loweringProvider;
    // @field
    private ConstantNode ___illegalConstant;

    // @cons
    VirtualizerToolImpl(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, PartialEscapeClosure<?> __closure, Assumptions __assumptions, LoweringProvider __loweringProvider)
    {
        super();
        this.___metaAccess = __metaAccess;
        this.___constantReflection = __constantReflection;
        this.___constantFieldProvider = __constantFieldProvider;
        this.___closure = __closure;
        this.___assumptions = __assumptions;
        this.___loweringProvider = __loweringProvider;
    }

    // @field
    private boolean ___deleted;
    // @field
    private PartialEscapeBlockState<?> ___state;
    // @field
    private ValueNode ___current;
    // @field
    private FixedNode ___position;
    // @field
    private GraphEffectList ___effects;

    @Override
    public MetaAccessProvider getMetaAccessProvider()
    {
        return this.___metaAccess;
    }

    @Override
    public ConstantReflectionProvider getConstantReflectionProvider()
    {
        return this.___constantReflection;
    }

    @Override
    public ConstantFieldProvider getConstantFieldProvider()
    {
        return this.___constantFieldProvider;
    }

    @Override
    public ArrayOffsetProvider getArrayOffsetProvider()
    {
        return this.___loweringProvider;
    }

    public void reset(PartialEscapeBlockState<?> __newState, ValueNode __newCurrent, FixedNode __newPosition, GraphEffectList __newEffects)
    {
        this.___deleted = false;
        this.___state = __newState;
        this.___current = __newCurrent;
        this.___position = __newPosition;
        this.___effects = __newEffects;
    }

    public boolean isDeleted()
    {
        return this.___deleted;
    }

    @Override
    public ValueNode getAlias(ValueNode __value)
    {
        return this.___closure.getAliasAndResolve(this.___state, __value);
    }

    @Override
    public ValueNode getEntry(VirtualObjectNode __virtualObject, int __index)
    {
        return this.___state.getObjectState(__virtualObject).getEntry(__index);
    }

    @Override
    public boolean setVirtualEntry(VirtualObjectNode __virtual, int __index, ValueNode __value, JavaKind __theAccessKind, long __offset)
    {
        ObjectState __obj = this.___state.getObjectState(__virtual);
        ValueNode __newValue;
        JavaKind __entryKind = __virtual.entryKind(__index);
        JavaKind __accessKind = __theAccessKind != null ? __theAccessKind : __entryKind;
        if (__value == null)
        {
            __newValue = null;
        }
        else
        {
            __newValue = this.___closure.getAliasAndResolve(this.___state, __value);
        }
        ValueNode __oldValue = getEntry(__virtual, __index);
        boolean __canVirtualize = __entryKind == __accessKind || (__entryKind == __accessKind.getStackKind() && __virtual instanceof VirtualInstanceNode);
        if (!__canVirtualize)
        {
            if (__entryKind == JavaKind.Long && __oldValue.getStackKind() == __newValue.getStackKind() && __oldValue.getStackKind().isPrimitive())
            {
                // Special case: If the entryKind is long, allow arbitrary kinds as long as a value
                // of the same kind is already there. This can only happen if some other node
                // initialized the entry with a value of a different kind. One example where this
                // happens is the Truffle NewFrameNode.
                __canVirtualize = true;
            }
            else if (__entryKind == JavaKind.Int && (__accessKind == JavaKind.Long || __accessKind == JavaKind.Double) && __offset % 8 == 0)
            {
                // Special case: Allow storing a single long or double value into two consecutive int slots.
                int __nextIndex = __virtual.entryIndexForOffset(getArrayOffsetProvider(), __offset + 4, JavaKind.Int);
                if (__nextIndex != -1)
                {
                    __canVirtualize = true;
                }
            }
        }

        if (__canVirtualize)
        {
            this.___state.setEntry(__virtual.getObjectId(), __index, __newValue);
            if (__entryKind == JavaKind.Int)
            {
                if (__accessKind.needsTwoSlots())
                {
                    // storing double word value two int slots
                    this.___state.setEntry(__virtual.getObjectId(), __index + 1, getIllegalConstant());
                }
                else if (__oldValue.getStackKind() == JavaKind.Double || __oldValue.getStackKind() == JavaKind.Long)
                {
                    // splitting double word constant by storing over it with an int
                    ValueNode __secondHalf = UnpackEndianHalfNode.create(__oldValue, false, NodeView.DEFAULT);
                    addNode(__secondHalf);
                    this.___state.setEntry(__virtual.getObjectId(), __index + 1, __secondHalf);
                }
            }
            if (__oldValue.isConstant() && __oldValue.asConstant().equals(JavaConstant.forIllegal()))
            {
                // storing into second half of double, so replace previous value
                ValueNode __previous = getEntry(__virtual, __index - 1);
                ValueNode __firstHalf = UnpackEndianHalfNode.create(__previous, true, NodeView.DEFAULT);
                addNode(__firstHalf);
                this.___state.setEntry(__virtual.getObjectId(), __index - 1, __firstHalf);
            }
            return true;
        }
        // should only occur if there are mismatches between the entry and access kind
        return false;
    }

    private ValueNode getIllegalConstant()
    {
        if (this.___illegalConstant == null)
        {
            this.___illegalConstant = ConstantNode.forConstant(JavaConstant.forIllegal(), getMetaAccessProvider());
            addNode(this.___illegalConstant);
        }
        return this.___illegalConstant;
    }

    @Override
    public void setEnsureVirtualized(VirtualObjectNode __virtualObject, boolean __ensureVirtualized)
    {
        int __id = __virtualObject.getObjectId();
        this.___state.setEnsureVirtualized(__id, __ensureVirtualized);
    }

    @Override
    public boolean getEnsureVirtualized(VirtualObjectNode __virtualObject)
    {
        return this.___state.getObjectState(__virtualObject).getEnsureVirtualized();
    }

    @Override
    public void replaceWithVirtual(VirtualObjectNode __virtual)
    {
        this.___closure.addVirtualAlias(__virtual, this.___current);
        this.___effects.deleteNode(this.___current);
        this.___deleted = true;
    }

    @Override
    public void replaceWithValue(ValueNode __replacement)
    {
        this.___effects.replaceAtUsages(this.___current, this.___closure.getScalarAlias(__replacement), this.___position);
        this.___closure.addScalarAlias(this.___current, __replacement);
        this.___deleted = true;
    }

    @Override
    public void delete()
    {
        this.___effects.deleteNode(this.___current);
        this.___deleted = true;
    }

    @Override
    public void replaceFirstInput(Node __oldInput, Node __replacement)
    {
        this.___effects.replaceFirstInput(this.___current, __oldInput, __replacement);
    }

    @Override
    public void addNode(ValueNode __node)
    {
        if (__node instanceof FloatingNode)
        {
            this.___effects.addFloatingNode(__node, "VirtualizerTool");
        }
        else
        {
            this.___effects.addFixedNodeBefore((FixedWithNextNode) __node, this.___position);
        }
    }

    @Override
    public void createVirtualObject(VirtualObjectNode __virtualObject, ValueNode[] __entryState, List<MonitorIdNode> __locks, boolean __ensureVirtualized)
    {
        if (!__virtualObject.isAlive())
        {
            this.___effects.addFloatingNode(__virtualObject, "newVirtualObject");
        }
        for (int __i = 0; __i < __entryState.length; __i++)
        {
            ValueNode __entry = __entryState[__i];
            __entryState[__i] = __entry instanceof VirtualObjectNode ? __entry : this.___closure.getAliasAndResolve(this.___state, __entry);
        }
        int __id = __virtualObject.getObjectId();
        if (__id == -1)
        {
            __id = this.___closure.___virtualObjects.size();
            this.___closure.___virtualObjects.add(__virtualObject);
            __virtualObject.setObjectId(__id);
        }
        this.___state.addObject(__id, new ObjectState(__entryState, __locks, __ensureVirtualized));
        this.___closure.addVirtualAlias(__virtualObject, __virtualObject);
        this.___effects.addVirtualizationDelta(1);
    }

    @Override
    public int getMaximumEntryCount()
    {
        return GraalOptions.maximumEscapeAnalysisArrayLength;
    }

    @Override
    public void replaceWith(ValueNode __node)
    {
        if (__node instanceof VirtualObjectNode)
        {
            replaceWithVirtual((VirtualObjectNode) __node);
        }
        else
        {
            replaceWithValue(__node);
        }
    }

    @Override
    public boolean ensureMaterialized(VirtualObjectNode __virtualObject)
    {
        return this.___closure.ensureMaterialized(this.___state, __virtualObject.getObjectId(), this.___position, this.___effects);
    }

    @Override
    public void addLock(VirtualObjectNode __virtualObject, MonitorIdNode __monitorId)
    {
        int __id = __virtualObject.getObjectId();
        this.___state.addLock(__id, __monitorId);
    }

    @Override
    public MonitorIdNode removeLock(VirtualObjectNode __virtualObject)
    {
        int __id = __virtualObject.getObjectId();
        return this.___state.removeLock(__id);
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return this.___metaAccess;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return this.___constantReflection;
    }

    @Override
    public boolean canonicalizeReads()
    {
        return false;
    }

    @Override
    public boolean allUsagesAvailable()
    {
        return true;
    }

    @Override
    public Assumptions getAssumptions()
    {
        return this.___assumptions;
    }

    @Override
    public Integer smallestCompareWidth()
    {
        if (this.___loweringProvider != null)
        {
            return this.___loweringProvider.smallestCompareWidth();
        }
        else
        {
            return null;
        }
    }
}
