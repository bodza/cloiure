package graalvm.compiler.virtual.phases.ea;

import java.util.List;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.spi.ArrayOffsetProvider;
import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.calc.UnpackEndianHalfNode;
import graalvm.compiler.nodes.java.MonitorIdNode;
import graalvm.compiler.nodes.spi.LoweringProvider;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualInstanceNode;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.options.OptionValues;

/**
 * Forwards calls from {@link VirtualizerTool} to the actual {@link PartialEscapeBlockState}.
 */
class VirtualizerToolImpl implements VirtualizerTool, CanonicalizerTool
{
    private final MetaAccessProvider metaAccess;
    private final ConstantReflectionProvider constantReflection;
    private final ConstantFieldProvider constantFieldProvider;
    private final PartialEscapeClosure<?> closure;
    private final Assumptions assumptions;
    private final OptionValues options;
    private final LoweringProvider loweringProvider;
    private ConstantNode illegalConstant;

    VirtualizerToolImpl(MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, PartialEscapeClosure<?> closure, Assumptions assumptions, OptionValues options, LoweringProvider loweringProvider)
    {
        this.metaAccess = metaAccess;
        this.constantReflection = constantReflection;
        this.constantFieldProvider = constantFieldProvider;
        this.closure = closure;
        this.assumptions = assumptions;
        this.options = options;
        this.loweringProvider = loweringProvider;
    }

    private boolean deleted;
    private PartialEscapeBlockState<?> state;
    private ValueNode current;
    private FixedNode position;
    private GraphEffectList effects;

    @Override
    public OptionValues getOptions()
    {
        return options;
    }

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

    public void reset(PartialEscapeBlockState<?> newState, ValueNode newCurrent, FixedNode newPosition, GraphEffectList newEffects)
    {
        deleted = false;
        state = newState;
        current = newCurrent;
        position = newPosition;
        effects = newEffects;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    @Override
    public ValueNode getAlias(ValueNode value)
    {
        return closure.getAliasAndResolve(state, value);
    }

    @Override
    public ValueNode getEntry(VirtualObjectNode virtualObject, int index)
    {
        return state.getObjectState(virtualObject).getEntry(index);
    }

    @Override
    public boolean setVirtualEntry(VirtualObjectNode virtual, int index, ValueNode value, JavaKind theAccessKind, long offset)
    {
        ObjectState obj = state.getObjectState(virtual);
        ValueNode newValue;
        JavaKind entryKind = virtual.entryKind(index);
        JavaKind accessKind = theAccessKind != null ? theAccessKind : entryKind;
        if (value == null)
        {
            newValue = null;
        }
        else
        {
            newValue = closure.getAliasAndResolve(state, value);
        }
        ValueNode oldValue = getEntry(virtual, index);
        boolean canVirtualize = entryKind == accessKind || (entryKind == accessKind.getStackKind() && virtual instanceof VirtualInstanceNode);
        if (!canVirtualize)
        {
            if (entryKind == JavaKind.Long && oldValue.getStackKind() == newValue.getStackKind() && oldValue.getStackKind().isPrimitive())
            {
                /*
                 * Special case: If the entryKind is long, allow arbitrary kinds as long as a value
                 * of the same kind is already there. This can only happen if some other node
                 * initialized the entry with a value of a different kind. One example where this
                 * happens is the Truffle NewFrameNode.
                 */
                canVirtualize = true;
            }
            else if (entryKind == JavaKind.Int && (accessKind == JavaKind.Long || accessKind == JavaKind.Double) && offset % 8 == 0)
            {
                /*
                 * Special case: Allow storing a single long or double value into two consecutive
                 * int slots.
                 */
                int nextIndex = virtual.entryIndexForOffset(getArrayOffsetProvider(), offset + 4, JavaKind.Int);
                if (nextIndex != -1)
                {
                    canVirtualize = true;
                }
            }
        }

        if (canVirtualize)
        {
            state.setEntry(virtual.getObjectId(), index, newValue);
            if (entryKind == JavaKind.Int)
            {
                if (accessKind.needsTwoSlots())
                {
                    // Storing double word value two int slots
                    state.setEntry(virtual.getObjectId(), index + 1, getIllegalConstant());
                }
                else if (oldValue.getStackKind() == JavaKind.Double || oldValue.getStackKind() == JavaKind.Long)
                {
                    // Splitting double word constant by storing over it with an int
                    ValueNode secondHalf = UnpackEndianHalfNode.create(oldValue, false, NodeView.DEFAULT);
                    addNode(secondHalf);
                    state.setEntry(virtual.getObjectId(), index + 1, secondHalf);
                }
            }
            if (oldValue.isConstant() && oldValue.asConstant().equals(JavaConstant.forIllegal()))
            {
                // Storing into second half of double, so replace previous value
                ValueNode previous = getEntry(virtual, index - 1);
                ValueNode firstHalf = UnpackEndianHalfNode.create(previous, true, NodeView.DEFAULT);
                addNode(firstHalf);
                state.setEntry(virtual.getObjectId(), index - 1, firstHalf);
            }
            return true;
        }
        // Should only occur if there are mismatches between the entry and access kind
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
    public void setEnsureVirtualized(VirtualObjectNode virtualObject, boolean ensureVirtualized)
    {
        int id = virtualObject.getObjectId();
        state.setEnsureVirtualized(id, ensureVirtualized);
    }

    @Override
    public boolean getEnsureVirtualized(VirtualObjectNode virtualObject)
    {
        return state.getObjectState(virtualObject).getEnsureVirtualized();
    }

    @Override
    public void replaceWithVirtual(VirtualObjectNode virtual)
    {
        closure.addVirtualAlias(virtual, current);
        effects.deleteNode(current);
        deleted = true;
    }

    @Override
    public void replaceWithValue(ValueNode replacement)
    {
        effects.replaceAtUsages(current, closure.getScalarAlias(replacement), position);
        closure.addScalarAlias(current, replacement);
        deleted = true;
    }

    @Override
    public void delete()
    {
        effects.deleteNode(current);
        deleted = true;
    }

    @Override
    public void replaceFirstInput(Node oldInput, Node replacement)
    {
        effects.replaceFirstInput(current, oldInput, replacement);
    }

    @Override
    public void addNode(ValueNode node)
    {
        if (node instanceof FloatingNode)
        {
            effects.addFloatingNode(node, "VirtualizerTool");
        }
        else
        {
            effects.addFixedNodeBefore((FixedWithNextNode) node, position);
        }
    }

    @Override
    public void createVirtualObject(VirtualObjectNode virtualObject, ValueNode[] entryState, List<MonitorIdNode> locks, boolean ensureVirtualized)
    {
        if (!virtualObject.isAlive())
        {
            effects.addFloatingNode(virtualObject, "newVirtualObject");
        }
        for (int i = 0; i < entryState.length; i++)
        {
            ValueNode entry = entryState[i];
            entryState[i] = entry instanceof VirtualObjectNode ? entry : closure.getAliasAndResolve(state, entry);
        }
        int id = virtualObject.getObjectId();
        if (id == -1)
        {
            id = closure.virtualObjects.size();
            closure.virtualObjects.add(virtualObject);
            virtualObject.setObjectId(id);
        }
        state.addObject(id, new ObjectState(entryState, locks, ensureVirtualized));
        closure.addVirtualAlias(virtualObject, virtualObject);
        effects.addVirtualizationDelta(1);
    }

    @Override
    public int getMaximumEntryCount()
    {
        return GraalOptions.MaximumEscapeAnalysisArrayLength.getValue(current.getOptions());
    }

    @Override
    public void replaceWith(ValueNode node)
    {
        if (node instanceof VirtualObjectNode)
        {
            replaceWithVirtual((VirtualObjectNode) node);
        }
        else
        {
            replaceWithValue(node);
        }
    }

    @Override
    public boolean ensureMaterialized(VirtualObjectNode virtualObject)
    {
        return closure.ensureMaterialized(state, virtualObject.getObjectId(), position, effects);
    }

    @Override
    public void addLock(VirtualObjectNode virtualObject, MonitorIdNode monitorId)
    {
        int id = virtualObject.getObjectId();
        state.addLock(id, monitorId);
    }

    @Override
    public MonitorIdNode removeLock(VirtualObjectNode virtualObject)
    {
        int id = virtualObject.getObjectId();
        return state.removeLock(id);
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
