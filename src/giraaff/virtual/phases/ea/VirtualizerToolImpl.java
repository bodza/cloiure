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

/**
 * Forwards calls from {@link VirtualizerTool} to the actual {@link PartialEscapeBlockState}.
 */
// @class VirtualizerToolImpl
final class VirtualizerToolImpl implements VirtualizerTool, CanonicalizerTool
{
    // @field
    private final MetaAccessProvider metaAccess;
    // @field
    private final ConstantReflectionProvider constantReflection;
    // @field
    private final ConstantFieldProvider constantFieldProvider;
    // @field
    private final PartialEscapeClosure<?> closure;
    // @field
    private final Assumptions assumptions;
    // @field
    private final LoweringProvider loweringProvider;
    // @field
    private ConstantNode illegalConstant;

    // @cons
    VirtualizerToolImpl(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, PartialEscapeClosure<?> __closure, Assumptions __assumptions, LoweringProvider __loweringProvider)
    {
        super();
        this.metaAccess = __metaAccess;
        this.constantReflection = __constantReflection;
        this.constantFieldProvider = __constantFieldProvider;
        this.closure = __closure;
        this.assumptions = __assumptions;
        this.loweringProvider = __loweringProvider;
    }

    // @field
    private boolean deleted;
    // @field
    private PartialEscapeBlockState<?> state;
    // @field
    private ValueNode current;
    // @field
    private FixedNode position;
    // @field
    private GraphEffectList effects;

    @Override
    public MetaAccessProvider getMetaAccessProvider()
    {
        return metaAccess;
    }

    @Override
    public ConstantReflectionProvider getConstantReflectionProvider()
    {
        return constantReflection;
    }

    @Override
    public ConstantFieldProvider getConstantFieldProvider()
    {
        return constantFieldProvider;
    }

    @Override
    public ArrayOffsetProvider getArrayOffsetProvider()
    {
        return loweringProvider;
    }

    public void reset(PartialEscapeBlockState<?> __newState, ValueNode __newCurrent, FixedNode __newPosition, GraphEffectList __newEffects)
    {
        deleted = false;
        state = __newState;
        current = __newCurrent;
        position = __newPosition;
        effects = __newEffects;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    @Override
    public ValueNode getAlias(ValueNode __value)
    {
        return closure.getAliasAndResolve(state, __value);
    }

    @Override
    public ValueNode getEntry(VirtualObjectNode __virtualObject, int __index)
    {
        return state.getObjectState(__virtualObject).getEntry(__index);
    }

    @Override
    public boolean setVirtualEntry(VirtualObjectNode __virtual, int __index, ValueNode __value, JavaKind __theAccessKind, long __offset)
    {
        ObjectState __obj = state.getObjectState(__virtual);
        ValueNode __newValue;
        JavaKind __entryKind = __virtual.entryKind(__index);
        JavaKind __accessKind = __theAccessKind != null ? __theAccessKind : __entryKind;
        if (__value == null)
        {
            __newValue = null;
        }
        else
        {
            __newValue = closure.getAliasAndResolve(state, __value);
        }
        ValueNode __oldValue = getEntry(__virtual, __index);
        boolean __canVirtualize = __entryKind == __accessKind || (__entryKind == __accessKind.getStackKind() && __virtual instanceof VirtualInstanceNode);
        if (!__canVirtualize)
        {
            if (__entryKind == JavaKind.Long && __oldValue.getStackKind() == __newValue.getStackKind() && __oldValue.getStackKind().isPrimitive())
            {
                /*
                 * Special case: If the entryKind is long, allow arbitrary kinds as long as a value
                 * of the same kind is already there. This can only happen if some other node
                 * initialized the entry with a value of a different kind. One example where this
                 * happens is the Truffle NewFrameNode.
                 */
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
            state.setEntry(__virtual.getObjectId(), __index, __newValue);
            if (__entryKind == JavaKind.Int)
            {
                if (__accessKind.needsTwoSlots())
                {
                    // storing double word value two int slots
                    state.setEntry(__virtual.getObjectId(), __index + 1, getIllegalConstant());
                }
                else if (__oldValue.getStackKind() == JavaKind.Double || __oldValue.getStackKind() == JavaKind.Long)
                {
                    // splitting double word constant by storing over it with an int
                    ValueNode __secondHalf = UnpackEndianHalfNode.create(__oldValue, false, NodeView.DEFAULT);
                    addNode(__secondHalf);
                    state.setEntry(__virtual.getObjectId(), __index + 1, __secondHalf);
                }
            }
            if (__oldValue.isConstant() && __oldValue.asConstant().equals(JavaConstant.forIllegal()))
            {
                // storing into second half of double, so replace previous value
                ValueNode __previous = getEntry(__virtual, __index - 1);
                ValueNode __firstHalf = UnpackEndianHalfNode.create(__previous, true, NodeView.DEFAULT);
                addNode(__firstHalf);
                state.setEntry(__virtual.getObjectId(), __index - 1, __firstHalf);
            }
            return true;
        }
        // should only occur if there are mismatches between the entry and access kind
        return false;
    }

    private ValueNode getIllegalConstant()
    {
        if (illegalConstant == null)
        {
            illegalConstant = ConstantNode.forConstant(JavaConstant.forIllegal(), getMetaAccessProvider());
            addNode(illegalConstant);
        }
        return illegalConstant;
    }

    @Override
    public void setEnsureVirtualized(VirtualObjectNode __virtualObject, boolean __ensureVirtualized)
    {
        int __id = __virtualObject.getObjectId();
        state.setEnsureVirtualized(__id, __ensureVirtualized);
    }

    @Override
    public boolean getEnsureVirtualized(VirtualObjectNode __virtualObject)
    {
        return state.getObjectState(__virtualObject).getEnsureVirtualized();
    }

    @Override
    public void replaceWithVirtual(VirtualObjectNode __virtual)
    {
        closure.addVirtualAlias(__virtual, current);
        effects.deleteNode(current);
        deleted = true;
    }

    @Override
    public void replaceWithValue(ValueNode __replacement)
    {
        effects.replaceAtUsages(current, closure.getScalarAlias(__replacement), position);
        closure.addScalarAlias(current, __replacement);
        deleted = true;
    }

    @Override
    public void delete()
    {
        effects.deleteNode(current);
        deleted = true;
    }

    @Override
    public void replaceFirstInput(Node __oldInput, Node __replacement)
    {
        effects.replaceFirstInput(current, __oldInput, __replacement);
    }

    @Override
    public void addNode(ValueNode __node)
    {
        if (__node instanceof FloatingNode)
        {
            effects.addFloatingNode(__node, "VirtualizerTool");
        }
        else
        {
            effects.addFixedNodeBefore((FixedWithNextNode) __node, position);
        }
    }

    @Override
    public void createVirtualObject(VirtualObjectNode __virtualObject, ValueNode[] __entryState, List<MonitorIdNode> __locks, boolean __ensureVirtualized)
    {
        if (!__virtualObject.isAlive())
        {
            effects.addFloatingNode(__virtualObject, "newVirtualObject");
        }
        for (int __i = 0; __i < __entryState.length; __i++)
        {
            ValueNode __entry = __entryState[__i];
            __entryState[__i] = __entry instanceof VirtualObjectNode ? __entry : closure.getAliasAndResolve(state, __entry);
        }
        int __id = __virtualObject.getObjectId();
        if (__id == -1)
        {
            __id = closure.virtualObjects.size();
            closure.virtualObjects.add(__virtualObject);
            __virtualObject.setObjectId(__id);
        }
        state.addObject(__id, new ObjectState(__entryState, __locks, __ensureVirtualized));
        closure.addVirtualAlias(__virtualObject, __virtualObject);
        effects.addVirtualizationDelta(1);
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
        return closure.ensureMaterialized(state, __virtualObject.getObjectId(), position, effects);
    }

    @Override
    public void addLock(VirtualObjectNode __virtualObject, MonitorIdNode __monitorId)
    {
        int __id = __virtualObject.getObjectId();
        state.addLock(__id, __monitorId);
    }

    @Override
    public MonitorIdNode removeLock(VirtualObjectNode __virtualObject)
    {
        int __id = __virtualObject.getObjectId();
        return state.removeLock(__id);
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return metaAccess;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return constantReflection;
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
        return assumptions;
    }

    @Override
    public Integer smallestCompareWidth()
    {
        if (loweringProvider != null)
        {
            return loweringProvider.smallestCompareWidth();
        }
        else
        {
            return null;
        }
    }
}
