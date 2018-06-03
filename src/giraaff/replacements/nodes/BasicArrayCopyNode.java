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

// @class BasicArrayCopyNode
public class BasicArrayCopyNode extends AbstractMemoryCheckpoint implements Virtualizable, MemoryCheckpoint.Single, MemoryAccess, Lowerable, DeoptimizingNode.DeoptDuring
{
    // @def
    public static final NodeClass<BasicArrayCopyNode> TYPE = NodeClass.create(BasicArrayCopyNode.class);

    // @def
    static final int SRC_ARG = 0;
    // @def
    static final int SRC_POS_ARG = 1;
    // @def
    static final int DEST_ARG = 2;
    // @def
    static final int DEST_POS_ARG = 3;
    // @def
    static final int LENGTH_ARG = 4;

    @Input
    // @field
    NodeInputList<ValueNode> args;

    @OptionalInput(InputType.State)
    // @field
    FrameState stateDuring;

    @OptionalInput(InputType.Memory)
    // @field
    protected MemoryNode lastLocationAccess;

    // @field
    protected JavaKind elementKind;

    // @field
    protected int bci;

    // @cons
    public BasicArrayCopyNode(NodeClass<? extends AbstractMemoryCheckpoint> __type, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, JavaKind __elementKind, int __bci)
    {
        super(__type, StampFactory.forKind(JavaKind.Void));
        this.bci = __bci;
        args = new NodeInputList<>(this, new ValueNode[] { __src, __srcPos, __dest, __destPos, __length });
        this.elementKind = __elementKind != JavaKind.Illegal ? __elementKind : null;
    }

    // @cons
    public BasicArrayCopyNode(NodeClass<? extends AbstractMemoryCheckpoint> __type, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, JavaKind __elementKind)
    {
        super(__type, StampFactory.forKind(JavaKind.Void));
        this.bci = BytecodeFrame.INVALID_FRAMESTATE_BCI;
        args = new NodeInputList<>(this, new ValueNode[] { __src, __srcPos, __dest, __destPos, __length });
        this.elementKind = __elementKind != JavaKind.Illegal ? __elementKind : null;
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
    public void setLastLocationAccess(MemoryNode __lla)
    {
        updateUsagesInterface(lastLocationAccess, __lla);
        lastLocationAccess = __lla;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    private static boolean checkBounds(int __position, int __length, VirtualObjectNode __virtualObject)
    {
        return __position >= 0 && __position + __length <= __virtualObject.entryCount();
    }

    private static boolean checkEntryTypes(int __srcPos, int __length, VirtualObjectNode __src, ResolvedJavaType __destComponentType, VirtualizerTool __tool)
    {
        if (__destComponentType.getJavaKind() == JavaKind.Object && !__destComponentType.isJavaLangObject())
        {
            for (int __i = 0; __i < __length; __i++)
            {
                ValueNode __entry = __tool.getEntry(__src, __srcPos + __i);
                ResolvedJavaType __type = StampTool.typeOrNull(__entry);
                if (__type == null || !__destComponentType.isAssignableFrom(__type))
                {
                    return false;
                }
            }
        }
        return true;
    }

    // Returns true if this copy doesn't require store checks. Trivially true for primitive arrays.
    public boolean isExact()
    {
        ResolvedJavaType __srcType = StampTool.typeOrNull(getSource().stamp(NodeView.DEFAULT));
        ResolvedJavaType __destType = StampTool.typeOrNull(getDestination().stamp(NodeView.DEFAULT));
        if (__srcType == null || !__srcType.isArray() || __destType == null || !__destType.isArray())
        {
            return false;
        }
        if ((__srcType.getComponentType().getJavaKind().isPrimitive() && __destType.getComponentType().equals(__srcType.getComponentType())) || getSource() == getDestination())
        {
            return true;
        }

        if (StampTool.isExactType(getDestination().stamp(NodeView.DEFAULT)))
        {
            if (__destType != null && __destType.isAssignableFrom(__srcType))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __sourcePosition = __tool.getAlias(getSourcePosition());
        ValueNode __destinationPosition = __tool.getAlias(getDestinationPosition());
        ValueNode __replacedLength = __tool.getAlias(getLength());

        if (__sourcePosition.isConstant() && __destinationPosition.isConstant() && __replacedLength.isConstant())
        {
            int __srcPosInt = __sourcePosition.asJavaConstant().asInt();
            int __destPosInt = __destinationPosition.asJavaConstant().asInt();
            int __len = __replacedLength.asJavaConstant().asInt();
            ValueNode __destAlias = __tool.getAlias(getDestination());

            if (__destAlias instanceof VirtualArrayNode)
            {
                VirtualArrayNode __destVirtual = (VirtualArrayNode) __destAlias;
                if (__len < 0 || !checkBounds(__destPosInt, __len, __destVirtual))
                {
                    return;
                }
                ValueNode __srcAlias = __tool.getAlias(getSource());

                if (__srcAlias instanceof VirtualObjectNode)
                {
                    if (!(__srcAlias instanceof VirtualArrayNode))
                    {
                        return;
                    }
                    VirtualArrayNode __srcVirtual = (VirtualArrayNode) __srcAlias;
                    if (__destVirtual.componentType().getJavaKind() != __srcVirtual.componentType().getJavaKind())
                    {
                        return;
                    }
                    if (!checkBounds(__srcPosInt, __len, __srcVirtual))
                    {
                        return;
                    }
                    if (!checkEntryTypes(__srcPosInt, __len, __srcVirtual, __destVirtual.type().getComponentType(), __tool))
                    {
                        return;
                    }
                    for (int __i = 0; __i < __len; __i++)
                    {
                        __tool.setVirtualEntry(__destVirtual, __destPosInt + __i, __tool.getEntry(__srcVirtual, __srcPosInt + __i));
                    }
                    __tool.delete();
                }
                else
                {
                    ResolvedJavaType __sourceType = StampTool.typeOrNull(__srcAlias);
                    if (__sourceType == null || !__sourceType.isArray())
                    {
                        return;
                    }
                    ResolvedJavaType __sourceComponentType = __sourceType.getComponentType();
                    ResolvedJavaType __destComponentType = __destVirtual.type().getComponentType();
                    if (!__sourceComponentType.equals(__destComponentType))
                    {
                        return;
                    }
                    for (int __i = 0; __i < __len; __i++)
                    {
                        LoadIndexedNode __load = new LoadIndexedNode(graph().getAssumptions(), __srcAlias, ConstantNode.forInt(__i + __srcPosInt, graph()), __destComponentType.getJavaKind());
                        __tool.addNode(__load);
                        __tool.setVirtualEntry(__destVirtual, __destPosInt + __i, __load);
                    }
                    __tool.delete();
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
    public void setStateDuring(FrameState __stateDuring)
    {
        updateUsages(this.stateDuring, __stateDuring);
        this.stateDuring = __stateDuring;
    }

    @Override
    public void computeStateDuring(FrameState __currentStateAfter)
    {
        FrameState __newStateDuring = __currentStateAfter.duplicateModifiedDuringCall(getBci(), asNode().getStackKind());
        setStateDuring(__newStateDuring);
    }
}
