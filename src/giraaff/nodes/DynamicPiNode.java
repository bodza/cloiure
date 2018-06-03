package giraaff.nodes;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.extended.GuardingNode;

/**
 * A {@link PiNode} where the type is not yet known. If the type becomes known at a later point in
 * the compilation, this can canonicalize to a regular {@link PiNode}.
 */
// @class DynamicPiNode
public final class DynamicPiNode extends PiNode
{
    // @def
    public static final NodeClass<DynamicPiNode> TYPE = NodeClass.create(DynamicPiNode.class);

    @Input
    // @field
    ValueNode typeMirror;
    // @field
    private final boolean exact;

    // @cons
    protected DynamicPiNode(ValueNode __object, GuardingNode __guard, ValueNode __typeMirror, boolean __exact)
    {
        super(TYPE, __object, StampFactory.object(), __guard);
        this.typeMirror = __typeMirror;
        this.exact = __exact;
    }

    public static ValueNode create(Assumptions __assumptions, ConstantReflectionProvider __constantReflection, ValueNode __object, GuardingNode __guard, ValueNode __typeMirror, boolean __exact)
    {
        ValueNode __synonym = findSynonym(__assumptions, __constantReflection, __object, __guard, __typeMirror, __exact);
        if (__synonym != null)
        {
            return __synonym;
        }
        return new DynamicPiNode(__object, __guard, __typeMirror, __exact);
    }

    public static ValueNode create(Assumptions __assumptions, ConstantReflectionProvider __constantReflection, ValueNode __object, GuardingNode __guard, ValueNode __typeMirror)
    {
        return create(__assumptions, __constantReflection, __object, __guard, __typeMirror, false);
    }

    public boolean isExact()
    {
        return exact;
    }

    private static ValueNode findSynonym(Assumptions __assumptions, ConstantReflectionProvider __constantReflection, ValueNode __object, GuardingNode __guard, ValueNode __typeMirror, boolean __exact)
    {
        if (__typeMirror.isConstant())
        {
            ResolvedJavaType __t = __constantReflection.asJavaType(__typeMirror.asConstant());
            if (__t != null)
            {
                Stamp __staticPiStamp;
                if (__t.isPrimitive())
                {
                    __staticPiStamp = StampFactory.alwaysNull();
                }
                else
                {
                    TypeReference __type = __exact ? TypeReference.createExactTrusted(__t) : TypeReference.createTrusted(__assumptions, __t);
                    __staticPiStamp = StampFactory.object(__type);
                }

                return PiNode.create(__object, __staticPiStamp, (ValueNode) __guard);
            }
        }

        return null;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        ValueNode __synonym = findSynonym(__tool.getAssumptions(), __tool.getConstantReflection(), object, guard, typeMirror, exact);
        if (__synonym != null)
        {
            return __synonym;
        }
        return this;
    }
}
