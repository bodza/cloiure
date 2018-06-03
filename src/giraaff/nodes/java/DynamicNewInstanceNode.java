package giraaff.nodes.java;

import java.lang.reflect.Modifier;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;

// @class DynamicNewInstanceNode
public final class DynamicNewInstanceNode extends AbstractNewObjectNode implements Canonicalizable
{
    // @def
    public static final NodeClass<DynamicNewInstanceNode> TYPE = NodeClass.create(DynamicNewInstanceNode.class);

    @Input
    // @field
    ValueNode clazz;

    /**
     * Class pointer to class.class needs to be exposed earlier than this node is lowered so that it
     * can be replaced by the AOT machinery. If it's not needed for lowering this input can be ignored.
     */
    @OptionalInput
    // @field
    ValueNode classClass;

    // @cons
    public DynamicNewInstanceNode(ValueNode __clazz, boolean __fillContents)
    {
        this(TYPE, __clazz, __fillContents, null);
    }

    // @cons
    protected DynamicNewInstanceNode(NodeClass<? extends DynamicNewInstanceNode> __c, ValueNode __clazz, boolean __fillContents, FrameState __stateBefore)
    {
        super(__c, StampFactory.objectNonNull(), __fillContents, __stateBefore);
        this.clazz = __clazz;
    }

    public ValueNode getInstanceType()
    {
        return clazz;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (clazz.isConstant())
        {
            ResolvedJavaType __type = __tool.getConstantReflection().asJavaType(clazz.asConstant());
            if (__type != null && __type.isInitialized() && !throwsInstantiationException(__type, __tool.getMetaAccess()))
            {
                return createNewInstanceNode(__type);
            }
        }
        return this;
    }

    /**
     * Hook for subclasses to instantiate a subclass of {@link NewInstanceNode}.
     */
    protected NewInstanceNode createNewInstanceNode(ResolvedJavaType __type)
    {
        return new NewInstanceNode(__type, fillContents(), stateBefore());
    }

    public static boolean throwsInstantiationException(Class<?> __type, Class<?> __classClass)
    {
        return __type.isPrimitive() || __type.isArray() || __type.isInterface() || Modifier.isAbstract(__type.getModifiers()) || __type == __classClass;
    }

    public static boolean throwsInstantiationException(ResolvedJavaType __type, MetaAccessProvider __metaAccess)
    {
        return __type.isPrimitive() || __type.isArray() || __type.isInterface() || Modifier.isAbstract(__type.getModifiers()) || __type.equals(__metaAccess.lookupJavaType(Class.class));
    }

    public ValueNode getClassClass()
    {
        return classClass;
    }

    public void setClassClass(ValueNode __newClassClass)
    {
        updateUsages(classClass, __newClassClass);
        classClass = __newClassClass;
    }
}
