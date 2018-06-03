package giraaff.nodes.memory;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.CanonicalizableLocation;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

///
// Reads an {@linkplain FixedAccessNode accessed} value.
///
// @class ReadNode
public final class ReadNode extends FloatableAccessNode implements LIRLowerableAccess, Canonicalizable, Virtualizable, GuardingNode
{
    // @def
    public static final NodeClass<ReadNode> TYPE = NodeClass.create(ReadNode.class);

    // @cons
    public ReadNode(AddressNode __address, LocationIdentity __location, Stamp __stamp, BarrierType __barrierType)
    {
        this(TYPE, __address, __location, __stamp, null, __barrierType, false, null);
    }

    // @cons
    protected ReadNode(NodeClass<? extends ReadNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp, GuardingNode __guard, BarrierType __barrierType, boolean __nullCheck, FrameState __stateBefore)
    {
        super(__c, __address, __location, __stamp, __guard, __barrierType, __nullCheck, __stateBefore);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRKind __readKind = __gen.getLIRGeneratorTool().getLIRKind(getAccessStamp());
        __gen.setResult(this, __gen.getLIRGeneratorTool().getArithmetic().emitLoad(__readKind, __gen.operand(this.___address), __gen.state(this)));
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (__tool.allUsagesAvailable() && hasNoUsages())
        {
            // Read without usages or guard can be safely removed.
            return null;
        }
        if (!getNullCheck())
        {
            return canonicalizeRead(this, getAddress(), getLocationIdentity(), __tool);
        }
        else
        {
            // if this read is a null check, then replacing it with the value is incorrect for
            // guard-type usages
            return this;
        }
    }

    @Override
    public FloatingAccessNode asFloatingNode(MemoryNode __lastLocationAccess)
    {
        return graph().unique(new FloatingReadNode(getAddress(), getLocationIdentity(), __lastLocationAccess, stamp(NodeView.DEFAULT), getGuard(), getBarrierType()));
    }

    @Override
    public boolean isAllowedUsageType(InputType __type)
    {
        return (getNullCheck() && __type == InputType.Guard) ? true : super.isAllowedUsageType(__type);
    }

    public static ValueNode canonicalizeRead(ValueNode __read, AddressNode __address, LocationIdentity __locationIdentity, CanonicalizerTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        MetaAccessProvider __metaAccess = __tool.getMetaAccess();
        if (__tool.canonicalizeReads() && __address instanceof OffsetAddressNode)
        {
            OffsetAddressNode __objAddress = (OffsetAddressNode) __address;
            ValueNode __object = __objAddress.getBase();
            if (__metaAccess != null && __object.isConstant() && !__object.isNullConstant() && __objAddress.getOffset().isConstant())
            {
                long __displacement = __objAddress.getOffset().asJavaConstant().asLong();
                int __stableDimension = ((ConstantNode) __object).getStableDimension();
                if (__locationIdentity.isImmutable() || __stableDimension > 0)
                {
                    Constant __constant = __read.stamp(__view).readConstant(__tool.getConstantReflection().getMemoryAccessProvider(), __object.asConstant(), __displacement);
                    boolean __isDefaultStable = __locationIdentity.isImmutable() || ((ConstantNode) __object).isDefaultStable();
                    if (__constant != null && (__isDefaultStable || !__constant.isDefaultForKind()))
                    {
                        return ConstantNode.forConstant(__read.stamp(__view), __constant, Math.max(__stableDimension - 1, 0), __isDefaultStable, __metaAccess);
                    }
                }
            }
            if (__locationIdentity.equals(NamedLocationIdentity.ARRAY_LENGTH_LOCATION))
            {
                ValueNode __length = GraphUtil.arrayLength(__object);
                if (__length != null)
                {
                    return __length;
                }
            }
            if (__locationIdentity instanceof CanonicalizableLocation)
            {
                CanonicalizableLocation __canonicalize = (CanonicalizableLocation) __locationIdentity;
                return __canonicalize.canonicalizeRead(__read, __address, __object, __tool);
            }
        }
        return __read;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        throw GraalError.shouldNotReachHere("unexpected ReadNode before PEA");
    }

    @Override
    public boolean canNullCheck()
    {
        return true;
    }

    @Override
    public Stamp getAccessStamp()
    {
        return stamp(NodeView.DEFAULT);
    }
}
