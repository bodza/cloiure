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
    public static final NodeClass<DynamicNewInstanceNode> TYPE = NodeClass.create(DynamicNewInstanceNode.class);

    @Input ValueNode clazz;

    /**
     * Class pointer to class.class needs to be exposed earlier than this node is lowered so that it
     * can be replaced by the AOT machinery. If it's not needed for lowering this input can be ignored.
     */
    @OptionalInput ValueNode classClass;

    // @cons
    public DynamicNewInstanceNode(ValueNode clazz, boolean fillContents)
    {
        this(TYPE, clazz, fillContents, null);
    }

    // @cons
    protected DynamicNewInstanceNode(NodeClass<? extends DynamicNewInstanceNode> c, ValueNode clazz, boolean fillContents, FrameState stateBefore)
    {
        super(c, StampFactory.objectNonNull(), fillContents, stateBefore);
        this.clazz = clazz;
    }

    public ValueNode getInstanceType()
    {
        return clazz;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (clazz.isConstant())
        {
            ResolvedJavaType type = tool.getConstantReflection().asJavaType(clazz.asConstant());
            if (type != null && type.isInitialized() && !throwsInstantiationException(type, tool.getMetaAccess()))
            {
                return createNewInstanceNode(type);
            }
        }
        return this;
    }

    /**
     * Hook for subclasses to instantiate a subclass of {@link NewInstanceNode}.
     */
    protected NewInstanceNode createNewInstanceNode(ResolvedJavaType type)
    {
        return new NewInstanceNode(type, fillContents(), stateBefore());
    }

    public static boolean throwsInstantiationException(Class<?> type, Class<?> classClass)
    {
        return type.isPrimitive() || type.isArray() || type.isInterface() || Modifier.isAbstract(type.getModifiers()) || type == classClass;
    }

    public static boolean throwsInstantiationException(ResolvedJavaType type, MetaAccessProvider metaAccess)
    {
        return type.isPrimitive() || type.isArray() || type.isInterface() || Modifier.isAbstract(type.getModifiers()) || type.equals(metaAccess.lookupJavaType(Class.class));
    }

    public ValueNode getClassClass()
    {
        return classClass;
    }

    public void setClassClass(ValueNode newClassClass)
    {
        updateUsages(classClass, newClassClass);
        classClass = newClassClass;
    }
}
