package giraaff.hotspot.meta;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.Bytecodes;
import giraaff.core.common.type.Stamp;
import giraaff.hotspot.nodes.aot.ResolveDynamicConstantNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InvokeDynamicPlugin;
import giraaff.util.GraalError;

// @class HotSpotInvokeDynamicPlugin
public final class HotSpotInvokeDynamicPlugin implements InvokeDynamicPlugin
{
    // @def
    private static final Class<? extends ConstantPool> hscp;
    // @def
    private static final MethodHandle isResolvedDynamicInvokeMH;

    static
    {
        MethodHandle __m = null;
        Class<? extends ConstantPool> __c = null;
        try
        {
            __c = Class.forName("jdk.vm.ci.hotspot.HotSpotConstantPool").asSubclass(ConstantPool.class);
            __m = MethodHandles.lookup().findVirtual(__c, "isResolvedDynamicInvoke", MethodType.methodType(boolean.class, int.class, int.class));
        }
        catch (Exception __e)
        {
        }
        isResolvedDynamicInvokeMH = __m;
        hscp = __c;
    }

    private static boolean isResolvedDynamicInvoke(ConstantPool __constantPool, int __index, int __opcode)
    {
        if (isResolvedDynamicInvokeMH != null)
        {
            if (!hscp.isInstance(__constantPool))
            {
                return false;
            }
            try
            {
                return (boolean) isResolvedDynamicInvokeMH.invoke(__constantPool, __index, __opcode);
            }
            catch (Throwable __t)
            {
                throw GraalError.shouldNotReachHere(__t);
            }
        }
        throw GraalError.shouldNotReachHere("isResolvedDynamicInvokeMH not set");
    }

    // @field
    private final DynamicTypeStore dynoStore;
    // @field
    private final boolean treatAppendixAsConstant;

    // @cons
    public HotSpotInvokeDynamicPlugin(DynamicTypeStore __dynoStore, boolean __treatAppendixAsConstant)
    {
        super();
        this.dynoStore = __dynoStore;
        this.treatAppendixAsConstant = __treatAppendixAsConstant;
    }

    // @cons
    public HotSpotInvokeDynamicPlugin(DynamicTypeStore __dynoStore)
    {
        this(__dynoStore, true);
    }

    // @cons
    public HotSpotInvokeDynamicPlugin()
    {
        this(null);
    }

    // invokehandle support
    @Override
    public boolean isResolvedDynamicInvoke(GraphBuilderContext __builder, int __index, int __opcode)
    {
        ConstantPool __constantPool = __builder.getCode().getConstantPool();
        if (isResolvedDynamicInvokeMH == null)
        {
            // For older JVMCI, when HotSpotInvokeDynamicPlugin is being used for testing,
            // return true, so that we can continue along the plugin path.
            return true;
        }
        return isResolvedDynamicInvoke(__constantPool, __index, __opcode);
    }

    @Override
    public boolean supportsDynamicInvoke(GraphBuilderContext __builder, int __index, int __opcode)
    {
        return __opcode == Bytecodes.INVOKEDYNAMIC || isResolvedDynamicInvokeMH != null;
    }

    public DynamicTypeStore getDynamicTypeStore()
    {
        return dynoStore;
    }

    @Override
    public void recordDynamicMethod(GraphBuilderContext __builder, int __index, int __opcode, ResolvedJavaMethod __target)
    {
        HotSpotResolvedJavaMethod __method = (HotSpotResolvedJavaMethod) __builder.getMethod();
        HotSpotResolvedObjectType __methodHolder = __method.getDeclaringClass();

        HotSpotResolvedJavaMethod __adapter = (HotSpotResolvedJavaMethod) __target;
        if (dynoStore != null)
        {
            dynoStore.recordAdapter(__opcode, __methodHolder, __index, __adapter);
        }
    }

    @Override
    public ValueNode genAppendixNode(GraphBuilderContext __builder, int __index, int __opcode, JavaConstant __appendixConstant, FrameState __frameState)
    {
        JavaConstant __appendix = __appendixConstant;
        HotSpotResolvedJavaMethod __method = (HotSpotResolvedJavaMethod) __builder.getMethod();
        HotSpotResolvedObjectType __methodHolder = __method.getDeclaringClass();

        if (dynoStore != null)
        {
            __appendix = dynoStore.recordAppendix(__opcode, __methodHolder, __index, __appendix);
        }

        ConstantNode __appendixNode = ConstantNode.forConstant(__appendix, __builder.getMetaAccess(), __builder.getGraph());

        Stamp __appendixStamp = __appendixNode.stamp(NodeView.DEFAULT);
        Stamp __resolveStamp = treatAppendixAsConstant ? __appendixStamp : __appendixStamp.unrestricted();
        ResolveDynamicConstantNode __resolveNode = new ResolveDynamicConstantNode(__resolveStamp, __appendixNode);
        ResolveDynamicConstantNode __added = __builder.append(__resolveNode);
        __added.setStateBefore(__frameState);
        return __resolveNode;
    }

    // @iface HotSpotInvokeDynamicPlugin.DynamicTypeStore
    public interface DynamicTypeStore
    {
        void recordAdapter(int opcode, HotSpotResolvedObjectType holder, int cpi, HotSpotResolvedJavaMethod adapter);

        JavaConstant recordAppendix(int opcode, HotSpotResolvedObjectType holder, int cpi, JavaConstant appendix);
    }
}
