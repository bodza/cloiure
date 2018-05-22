package giraaff.hotspot.meta;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.debug.GraalError;
import giraaff.hotspot.nodes.aot.InitializeKlassNode;
import giraaff.hotspot.nodes.aot.ResolveConstantNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.ClassInitializationPlugin;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;

public final class HotSpotClassInitializationPlugin implements ClassInitializationPlugin
{
    @Override
    public boolean shouldApply(GraphBuilderContext builder, ResolvedJavaType type)
    {
        if (!builder.parsingIntrinsic())
        {
            if (!type.isArray())
            {
                ResolvedJavaMethod method = builder.getGraph().method();
                ResolvedJavaType methodHolder = method.getDeclaringClass();
                // We can elide initialization nodes if type >=: methodHolder.
                // The type is already initialized by either "new" or "invokestatic".

                // Emit initialization node if type is an interface since:
                // JLS 12.4: Before a class is initialized, its direct superclass must be
                // initialized, but interfaces implemented by the class are not
                // initialized and a class or interface type T will be initialized
                // immediately before the first occurrence of accesses listed
                // in JLS 12.4.1.

                return !type.isAssignableFrom(methodHolder) || type.isInterface();
            }
            else if (!type.getComponentType().isPrimitive())
            {
                // Always apply to object array types
                return true;
            }
        }
        return false;
    }

    @Override
    public ValueNode apply(GraphBuilderContext builder, ResolvedJavaType type, FrameState frameState)
    {
        Stamp hubStamp = builder.getStampProvider().createHubStamp((ObjectStamp) StampFactory.objectNonNull());
        ConstantNode hub = builder.append(ConstantNode.forConstant(hubStamp, ((HotSpotResolvedObjectType) type).klass(), builder.getMetaAccess(), builder.getGraph()));
        DeoptimizingFixedWithNextNode result = builder.append(type.isArray() ? new ResolveConstantNode(hub) : new InitializeKlassNode(hub));
        result.setStateBefore(frameState);
        return result;
    }

    private static final Class<? extends ConstantPool> hscp;
    private static final MethodHandle loadReferencedTypeIIZMH;

    static
    {
        MethodHandle m = null;
        Class<? extends ConstantPool> c = null;
        try
        {
            c = Class.forName("jdk.vm.ci.hotspot.HotSpotConstantPool").asSubclass(ConstantPool.class);
            m = MethodHandles.lookup().findVirtual(c, "loadReferencedType", MethodType.methodType(void.class, int.class, int.class, boolean.class));
        }
        catch (Exception e)
        {
        }
        loadReferencedTypeIIZMH = m;
        hscp = c;
    }

    private static boolean isHotSpotConstantPool(ConstantPool cp)
    {
        // jdk.vm.ci.hotspot.HotSpotConstantPool is final, so we can
        // directly compare Classes.
        return cp.getClass() == hscp;
    }

    @Override
    public boolean supportsLazyInitialization(ConstantPool cp)
    {
        if (loadReferencedTypeIIZMH != null && isHotSpotConstantPool(cp))
        {
            return true;
        }
        return false;
    }

    @Override
    public void loadReferencedType(GraphBuilderContext builder, ConstantPool cp, int cpi, int opcode)
    {
        if (loadReferencedTypeIIZMH != null && isHotSpotConstantPool(cp))
        {
            try
            {
                loadReferencedTypeIIZMH.invoke(cp, cpi, opcode, false);
            }
            catch (Throwable t)
            {
                throw GraalError.shouldNotReachHere(t);
            }
        }
        else
        {
            cp.loadReferencedType(cpi, opcode);
        }
    }
}
