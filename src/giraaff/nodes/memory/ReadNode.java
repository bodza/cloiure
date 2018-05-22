package giraaff.nodes.memory;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.Stamp;
import giraaff.debug.GraalError;
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

/**
 * Reads an {@linkplain FixedAccessNode accessed} value.
 */
public class ReadNode extends FloatableAccessNode implements LIRLowerableAccess, Canonicalizable, Virtualizable, GuardingNode
{
    public static final NodeClass<ReadNode> TYPE = NodeClass.create(ReadNode.class);

    public ReadNode(AddressNode address, LocationIdentity location, Stamp stamp, BarrierType barrierType)
    {
        this(TYPE, address, location, stamp, null, barrierType, false, null);
    }

    protected ReadNode(NodeClass<? extends ReadNode> c, AddressNode address, LocationIdentity location, Stamp stamp, GuardingNode guard, BarrierType barrierType, boolean nullCheck, FrameState stateBefore)
    {
        super(c, address, location, stamp, guard, barrierType, nullCheck, stateBefore);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        LIRKind readKind = gen.getLIRGeneratorTool().getLIRKind(getAccessStamp());
        gen.setResult(this, gen.getLIRGeneratorTool().getArithmetic().emitLoad(readKind, gen.operand(address), gen.state(this)));
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (tool.allUsagesAvailable() && hasNoUsages())
        {
            // Read without usages or guard can be safely removed.
            return null;
        }
        if (!getNullCheck())
        {
            return canonicalizeRead(this, getAddress(), getLocationIdentity(), tool);
        }
        else
        {
            // if this read is a null check, then replacing it with the value is incorrect for
            // guard-type usages
            return this;
        }
    }

    @Override
    public FloatingAccessNode asFloatingNode(MemoryNode lastLocationAccess)
    {
        return graph().unique(new FloatingReadNode(getAddress(), getLocationIdentity(), lastLocationAccess, stamp(NodeView.DEFAULT), getGuard(), getBarrierType()));
    }

    @Override
    public boolean isAllowedUsageType(InputType type)
    {
        return (getNullCheck() && type == InputType.Guard) ? true : super.isAllowedUsageType(type);
    }

    public static ValueNode canonicalizeRead(ValueNode read, AddressNode address, LocationIdentity locationIdentity, CanonicalizerTool tool)
    {
        NodeView view = NodeView.from(tool);
        MetaAccessProvider metaAccess = tool.getMetaAccess();
        if (tool.canonicalizeReads() && address instanceof OffsetAddressNode)
        {
            OffsetAddressNode objAddress = (OffsetAddressNode) address;
            ValueNode object = objAddress.getBase();
            if (metaAccess != null && object.isConstant() && !object.isNullConstant() && objAddress.getOffset().isConstant())
            {
                long displacement = objAddress.getOffset().asJavaConstant().asLong();
                int stableDimension = ((ConstantNode) object).getStableDimension();
                if (locationIdentity.isImmutable() || stableDimension > 0)
                {
                    Constant constant = read.stamp(view).readConstant(tool.getConstantReflection().getMemoryAccessProvider(), object.asConstant(), displacement);
                    boolean isDefaultStable = locationIdentity.isImmutable() || ((ConstantNode) object).isDefaultStable();
                    if (constant != null && (isDefaultStable || !constant.isDefaultForKind()))
                    {
                        return ConstantNode.forConstant(read.stamp(view), constant, Math.max(stableDimension - 1, 0), isDefaultStable, metaAccess);
                    }
                }
            }
            if (locationIdentity.equals(NamedLocationIdentity.ARRAY_LENGTH_LOCATION))
            {
                ValueNode length = GraphUtil.arrayLength(object);
                if (length != null)
                {
                    return length;
                }
            }
            if (locationIdentity instanceof CanonicalizableLocation)
            {
                CanonicalizableLocation canonicalize = (CanonicalizableLocation) locationIdentity;
                ValueNode result = canonicalize.canonicalizeRead(read, address, object, tool);
                return result;
            }
        }
        return read;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
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
