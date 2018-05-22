package giraaff.replacements.nodes;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.MemoryNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.virtual.VirtualArrayNode;
import giraaff.nodes.virtual.VirtualObjectNode;

public class BasicArrayCopyNode extends AbstractMemoryCheckpoint implements Virtualizable, MemoryCheckpoint.Single, MemoryAccess, Lowerable, DeoptimizingNode.DeoptDuring
{
    public static final NodeClass<BasicArrayCopyNode> TYPE = NodeClass.create(BasicArrayCopyNode.class);

    static final int SRC_ARG = 0;
    static final int SRC_POS_ARG = 1;
    static final int DEST_ARG = 2;
    static final int DEST_POS_ARG = 3;
    static final int LENGTH_ARG = 4;

    @Input NodeInputList<ValueNode> args;

    @OptionalInput(InputType.State) FrameState stateDuring;

    @OptionalInput(InputType.Memory) protected MemoryNode lastLocationAccess;

    protected JavaKind elementKind;

    protected int bci;

    public BasicArrayCopyNode(NodeClass<? extends AbstractMemoryCheckpoint> type, ValueNode src, ValueNode srcPos, ValueNode dest, ValueNode destPos, ValueNode length, JavaKind elementKind, int bci)
    {
        super(type, StampFactory.forKind(JavaKind.Void));
        this.bci = bci;
        args = new NodeInputList<>(this, new ValueNode[]{src, srcPos, dest, destPos, length});
        this.elementKind = elementKind != JavaKind.Illegal ? elementKind : null;
    }

    public BasicArrayCopyNode(NodeClass<? extends AbstractMemoryCheckpoint> type, ValueNode src, ValueNode srcPos, ValueNode dest, ValueNode destPos, ValueNode length, JavaKind elementKind)
    {
        super(type, StampFactory.forKind(JavaKind.Void));
        this.bci = BytecodeFrame.INVALID_FRAMESTATE_BCI;
        args = new NodeInputList<>(this, new ValueNode[]{src, srcPos, dest, destPos, length});
        this.elementKind = elementKind != JavaKind.Illegal ? elementKind : null;
    }

    public ValueNode getSource()
    {
        return args.get(SRC_ARG);
    }

    public ValueNode getSourcePosition()
    {
        return args.get(SRC_POS_ARG);
    }

    public ValueNode getDestination()
    {
        return args.get(DEST_ARG);
    }

    public ValueNode getDestinationPosition()
    {
        return args.get(DEST_POS_ARG);
    }

    public ValueNode getLength()
    {
        return args.get(LENGTH_ARG);
    }

    public int getBci()
    {
        return bci;
    }

    public JavaKind getElementKind()
    {
        return elementKind;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        if (elementKind != null)
        {
            return NamedLocationIdentity.getArrayLocation(elementKind);
        }
        return LocationIdentity.any();
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode lla)
    {
        updateUsagesInterface(lastLocationAccess, lla);
        lastLocationAccess = lla;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    private static boolean checkBounds(int position, int length, VirtualObjectNode virtualObject)
    {
        return position >= 0 && position + length <= virtualObject.entryCount();
    }

    private static boolean checkEntryTypes(int srcPos, int length, VirtualObjectNode src, ResolvedJavaType destComponentType, VirtualizerTool tool)
    {
        if (destComponentType.getJavaKind() == JavaKind.Object && !destComponentType.isJavaLangObject())
        {
            for (int i = 0; i < length; i++)
            {
                ValueNode entry = tool.getEntry(src, srcPos + i);
                ResolvedJavaType type = StampTool.typeOrNull(entry);
                if (type == null || !destComponentType.isAssignableFrom(type))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * Returns true if this copy doesn't require store checks. Trivially true for primitive arrays.
     */
    public boolean isExact()
    {
        ResolvedJavaType srcType = StampTool.typeOrNull(getSource().stamp(NodeView.DEFAULT));
        ResolvedJavaType destType = StampTool.typeOrNull(getDestination().stamp(NodeView.DEFAULT));
        if (srcType == null || !srcType.isArray() || destType == null || !destType.isArray())
        {
            return false;
        }
        if ((srcType.getComponentType().getJavaKind().isPrimitive() && destType.getComponentType().equals(srcType.getComponentType())) || getSource() == getDestination())
        {
            return true;
        }

        if (StampTool.isExactType(getDestination().stamp(NodeView.DEFAULT)))
        {
            if (destType != null && destType.isAssignableFrom(srcType))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode sourcePosition = tool.getAlias(getSourcePosition());
        ValueNode destinationPosition = tool.getAlias(getDestinationPosition());
        ValueNode replacedLength = tool.getAlias(getLength());

        if (sourcePosition.isConstant() && destinationPosition.isConstant() && replacedLength.isConstant())
        {
            int srcPosInt = sourcePosition.asJavaConstant().asInt();
            int destPosInt = destinationPosition.asJavaConstant().asInt();
            int len = replacedLength.asJavaConstant().asInt();
            ValueNode destAlias = tool.getAlias(getDestination());

            if (destAlias instanceof VirtualArrayNode)
            {
                VirtualArrayNode destVirtual = (VirtualArrayNode) destAlias;
                if (len < 0 || !checkBounds(destPosInt, len, destVirtual))
                {
                    return;
                }
                ValueNode srcAlias = tool.getAlias(getSource());

                if (srcAlias instanceof VirtualObjectNode)
                {
                    if (!(srcAlias instanceof VirtualArrayNode))
                    {
                        return;
                    }
                    VirtualArrayNode srcVirtual = (VirtualArrayNode) srcAlias;
                    if (destVirtual.componentType().getJavaKind() != srcVirtual.componentType().getJavaKind())
                    {
                        return;
                    }
                    if (!checkBounds(srcPosInt, len, srcVirtual))
                    {
                        return;
                    }
                    if (!checkEntryTypes(srcPosInt, len, srcVirtual, destVirtual.type().getComponentType(), tool))
                    {
                        return;
                    }
                    for (int i = 0; i < len; i++)
                    {
                        tool.setVirtualEntry(destVirtual, destPosInt + i, tool.getEntry(srcVirtual, srcPosInt + i));
                    }
                    tool.delete();
                }
                else
                {
                    ResolvedJavaType sourceType = StampTool.typeOrNull(srcAlias);
                    if (sourceType == null || !sourceType.isArray())
                    {
                        return;
                    }
                    ResolvedJavaType sourceComponentType = sourceType.getComponentType();
                    ResolvedJavaType destComponentType = destVirtual.type().getComponentType();
                    if (!sourceComponentType.equals(destComponentType))
                    {
                        return;
                    }
                    for (int i = 0; i < len; i++)
                    {
                        LoadIndexedNode load = new LoadIndexedNode(graph().getAssumptions(), srcAlias, ConstantNode.forInt(i + srcPosInt, graph()), destComponentType.getJavaKind());
                        tool.addNode(load);
                        tool.setVirtualEntry(destVirtual, destPosInt + i, load);
                    }
                    tool.delete();
                }
            }
        }
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public FrameState stateDuring()
    {
        return stateDuring;
    }

    @Override
    public void setStateDuring(FrameState stateDuring)
    {
        updateUsages(this.stateDuring, stateDuring);
        this.stateDuring = stateDuring;
    }

    @Override
    public void computeStateDuring(FrameState currentStateAfter)
    {
        FrameState newStateDuring = currentStateAfter.duplicateModifiedDuringCall(getBci(), asNode().getStackKind());
        setStateDuring(newStateDuring);
    }
}
