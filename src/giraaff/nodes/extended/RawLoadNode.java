package giraaff.nodes.extended;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ReinterpretNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.virtual.VirtualObjectNode;

///
// Load of a value from a location specified as an offset relative to an object. No null check is
// performed before the load.
///
// @class RawLoadNode
public class RawLoadNode extends UnsafeAccessNode implements Lowerable, Virtualizable, Canonicalizable
{
    // @def
    public static final NodeClass<RawLoadNode> TYPE = NodeClass.create(RawLoadNode.class);

    ///
    // This constructor exists for node intrinsics that need a stamp based on {@code accessKind}.
    ///
    // @cons
    public RawLoadNode(ValueNode __object, ValueNode __offset, JavaKind __accessKind, LocationIdentity __locationIdentity)
    {
        this(__object, __offset, __accessKind, __locationIdentity, false);
    }

    // @cons
    public RawLoadNode(ValueNode __object, ValueNode __offset, JavaKind __accessKind, LocationIdentity __locationIdentity, boolean __forceAnyLocation)
    {
        super(TYPE, StampFactory.forKind(__accessKind.getStackKind()), __object, __offset, __accessKind, __locationIdentity, __forceAnyLocation);
    }

    ///
    // This constructor exists for node intrinsics that need a stamp based on the return type of the
    // {@link giraaff.graph.Node.NodeIntrinsic} annotated method.
    ///
    // @cons
    public RawLoadNode(@InjectedNodeParameter Stamp __stamp, ValueNode __object, ValueNode __offset, LocationIdentity __locationIdentity, JavaKind __accessKind)
    {
        super(TYPE, __stamp, __object, __offset, __accessKind, __locationIdentity, false);
    }

    // @cons
    protected RawLoadNode(NodeClass<? extends RawLoadNode> __c, ValueNode __object, ValueNode __offset, JavaKind __accessKind, LocationIdentity __locationIdentity)
    {
        super(__c, StampFactory.forKind(__accessKind.getStackKind()), __object, __offset, __accessKind, __locationIdentity, false);
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
            ValueNode __offsetValue = __tool.getAlias(offset());
            if (__offsetValue.isConstant())
            {
                long __off = __offsetValue.asJavaConstant().asLong();
                int __entryIndex = __virtual.entryIndexForOffset(__tool.getArrayOffsetProvider(), __off, accessKind());

                if (__entryIndex != -1)
                {
                    ValueNode __entry = __tool.getEntry(__virtual, __entryIndex);
                    JavaKind __entryKind = __virtual.entryKind(__entryIndex);
                    if (__entry.getStackKind() == getStackKind() || __entryKind == accessKind())
                    {
                        if (!(__entry.stamp(NodeView.DEFAULT).isCompatible(stamp(NodeView.DEFAULT))))
                        {
                            if (__entry.stamp(NodeView.DEFAULT) instanceof PrimitiveStamp && this.___stamp instanceof PrimitiveStamp)
                            {
                                PrimitiveStamp __p1 = (PrimitiveStamp) this.___stamp;
                                PrimitiveStamp __p2 = (PrimitiveStamp) __entry.stamp(NodeView.DEFAULT);
                                int __width1 = __p1.getBits();
                                int __width2 = __p2.getBits();
                                if (__width1 == __width2)
                                {
                                    Node __replacement = ReinterpretNode.create(__p2, __entry, NodeView.DEFAULT);
                                    __tool.replaceWith((ValueNode) __replacement);
                                    return;
                                }
                                else
                                {
                                    // different bit width
                                    return;
                                }
                            }
                            else
                            {
                                // cannot reinterpret for arbitrary objects
                                return;
                            }
                        }
                        __tool.replaceWith(__entry);
                    }
                }
            }
        }
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (!isAnyLocationForced() && getLocationIdentity().isAny())
        {
            ValueNode __targetObject = object();
            if (offset().isConstant() && __targetObject.isConstant() && !__targetObject.isNullConstant())
            {
                ConstantNode __objectConstant = (ConstantNode) __targetObject;
                ResolvedJavaType __type = StampTool.typeOrNull(__objectConstant);
                if (__type != null && __type.isArray())
                {
                    JavaConstant __arrayConstant = __objectConstant.asJavaConstant();
                    if (__arrayConstant != null)
                    {
                        int __stableDimension = __objectConstant.getStableDimension();
                        if (__stableDimension > 0)
                        {
                            NodeView __view = NodeView.from(__tool);
                            long __constantOffset = offset().asJavaConstant().asLong();
                            Constant __constant = stamp(__view).readConstant(__tool.getConstantReflection().getMemoryAccessProvider(), __arrayConstant, __constantOffset);
                            boolean __isDefaultStable = __objectConstant.isDefaultStable();
                            if (__constant != null && (__isDefaultStable || !__constant.isDefaultForKind()))
                            {
                                return ConstantNode.forConstant(stamp(__view), __constant, __stableDimension - 1, __isDefaultStable, __tool.getMetaAccess());
                            }
                        }
                    }
                }
            }
        }
        return super.canonical(__tool);
    }

    @Override
    protected ValueNode cloneAsFieldAccess(Assumptions __assumptions, ResolvedJavaField __field)
    {
        return LoadFieldNode.create(__assumptions, object(), __field);
    }

    @Override
    protected ValueNode cloneAsArrayAccess(ValueNode __location, LocationIdentity __identity)
    {
        return new RawLoadNode(object(), __location, accessKind(), __identity);
    }

    @NodeIntrinsic
    public static native Object load(Object __object, long __offset, @ConstantNodeParameter JavaKind __kind, @ConstantNodeParameter LocationIdentity __locationIdentity);
}
