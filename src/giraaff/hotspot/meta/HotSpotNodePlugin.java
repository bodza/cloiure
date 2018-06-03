package giraaff.hotspot.meta;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampPair;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.GraphBuilderTool;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin;
import giraaff.nodes.graphbuilderconf.NodePlugin;
import giraaff.nodes.graphbuilderconf.TypePlugin;
import giraaff.nodes.util.ConstantFoldUtil;
import giraaff.word.Word;
import giraaff.word.WordOperationPlugin;

///
// This plugin handles the HotSpot-specific customizations of bytecode parsing:
//
// {@link Word}-type rewriting for {@link GraphBuilderContext#parsingIntrinsic intrinsic} functions
// (snippets and method substitutions), by forwarding to the {@link WordOperationPlugin}. Note that
// we forward the {@link NodePlugin} and {@link TypePlugin} methods, but not the
// {@link InlineInvokePlugin} methods implemented by {@link WordOperationPlugin}. The latter is not
// necessary because HotSpot only uses the {@link Word} type in methods that are force-inlined,
// i.e., there are never non-inlined invokes that involve the {@link Word} type.
//
// Constant folding of field loads.
///
// @class HotSpotNodePlugin
public final class HotSpotNodePlugin implements NodePlugin, TypePlugin
{
    // @field
    protected final WordOperationPlugin ___wordOperationPlugin;

    // @cons
    public HotSpotNodePlugin(WordOperationPlugin __wordOperationPlugin)
    {
        super();
        this.___wordOperationPlugin = __wordOperationPlugin;
    }

    @Override
    public boolean canChangeStackKind(GraphBuilderContext __b)
    {
        if (__b.parsingIntrinsic())
        {
            return this.___wordOperationPlugin.canChangeStackKind(__b);
        }
        return false;
    }

    @Override
    public StampPair interceptType(GraphBuilderTool __b, JavaType __declaredType, boolean __nonNull)
    {
        if (__b.parsingIntrinsic())
        {
            return this.___wordOperationPlugin.interceptType(__b, __declaredType, __nonNull);
        }
        return null;
    }

    @Override
    public boolean handleInvoke(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args)
    {
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleInvoke(__b, __method, __args))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleLoadField(GraphBuilderContext __b, ValueNode __object, ResolvedJavaField __field)
    {
        if (__object.isConstant())
        {
            JavaConstant __asJavaConstant = __object.asJavaConstant();
            if (tryReadField(__b, __field, __asJavaConstant))
            {
                return true;
            }
        }
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleLoadField(__b, __object, __field))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleLoadStaticField(GraphBuilderContext __b, ResolvedJavaField __field)
    {
        if (tryReadField(__b, __field, null))
        {
            return true;
        }
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleLoadStaticField(__b, __field))
        {
            return true;
        }
        return false;
    }

    private static boolean tryReadField(GraphBuilderContext __b, ResolvedJavaField __field, JavaConstant __object)
    {
        return tryConstantFold(__b, __field, __object);
    }

    private static boolean tryConstantFold(GraphBuilderContext __b, ResolvedJavaField __field, JavaConstant __object)
    {
        ConstantNode __result = ConstantFoldUtil.tryConstantFold(__b.getConstantFieldProvider(), __b.getConstantReflection(), __b.getMetaAccess(), __field, __object);
        if (__result != null)
        {
            __result = __b.getGraph().unique(__result);
            __b.push(__field.getJavaKind(), __result);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleStoreField(GraphBuilderContext __b, ValueNode __object, ResolvedJavaField __field, ValueNode __value)
    {
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleStoreField(__b, __object, __field, __value))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleStoreStaticField(GraphBuilderContext __b, ResolvedJavaField __field, ValueNode __value)
    {
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleStoreStaticField(__b, __field, __value))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleLoadIndexed(GraphBuilderContext __b, ValueNode __array, ValueNode __index, JavaKind __elementKind)
    {
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleLoadIndexed(__b, __array, __index, __elementKind))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleStoreIndexed(GraphBuilderContext __b, ValueNode __array, ValueNode __index, JavaKind __elementKind, ValueNode __value)
    {
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleStoreIndexed(__b, __array, __index, __elementKind, __value))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleCheckCast(GraphBuilderContext __b, ValueNode __object, ResolvedJavaType __type, JavaTypeProfile __profile)
    {
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleCheckCast(__b, __object, __type, __profile))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleInstanceOf(GraphBuilderContext __b, ValueNode __object, ResolvedJavaType __type, JavaTypeProfile __profile)
    {
        if (__b.parsingIntrinsic() && this.___wordOperationPlugin.handleInstanceOf(__b, __object, __type, __profile))
        {
            return true;
        }
        return false;
    }
}
