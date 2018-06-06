package giraaff.core.common.spi;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.GraalOptions;
import giraaff.util.GraalError;

///
// Utility for default constant folding semantics for Java fields.
///
// @class JavaConstantFieldProvider
public abstract class JavaConstantFieldProvider implements ConstantFieldProvider
{
    // @cons JavaConstantFieldProvider
    protected JavaConstantFieldProvider(MetaAccessProvider __metaAccess)
    {
        super();
        try
        {
            this.___stringValueField = __metaAccess.lookupJavaField(String.class.getDeclaredField("value"));
            this.___stringHashField = __metaAccess.lookupJavaField(String.class.getDeclaredField("hash"));
        }
        catch (NoSuchFieldException | SecurityException __e)
        {
            throw new GraalError(__e);
        }
    }

    @Override
    public <T> T readConstantField(ResolvedJavaField __field, ConstantFieldProvider.ConstantFieldTool<T> __tool)
    {
        if (isStableField(__field, __tool))
        {
            JavaConstant __value = __tool.readValue();
            if (__value != null && isStableFieldValueConstant(__field, __value, __tool))
            {
                return foldStableArray(__value, __field, __tool);
            }
        }
        if (isFinalField(__field, __tool))
        {
            JavaConstant __value = __tool.readValue();
            if (__value != null && isFinalFieldValueConstant(__field, __value, __tool))
            {
                return __tool.foldConstant(__value);
            }
        }
        return null;
    }

    protected <T> T foldStableArray(JavaConstant __value, ResolvedJavaField __field, ConstantFieldProvider.ConstantFieldTool<T> __tool)
    {
        return __tool.foldStableArray(__value, getArrayDimension(__field.getType()), isDefaultStableField(__field, __tool));
    }

    private static int getArrayDimension(JavaType __type)
    {
        int __dimensions = 0;
        JavaType __componentType = __type;
        while ((__componentType = __componentType.getComponentType()) != null)
        {
            __dimensions++;
        }
        return __dimensions;
    }

    private static boolean isArray(ResolvedJavaField __field)
    {
        JavaType __fieldType = __field.getType();
        return __fieldType instanceof ResolvedJavaType && ((ResolvedJavaType) __fieldType).isArray();
    }

    @SuppressWarnings("unused")
    protected boolean isStableFieldValueConstant(ResolvedJavaField __field, JavaConstant __value, ConstantFieldProvider.ConstantFieldTool<?> __tool)
    {
        return !__value.isDefaultForKind();
    }

    @SuppressWarnings("unused")
    protected boolean isFinalFieldValueConstant(ResolvedJavaField __field, JavaConstant __value, ConstantFieldProvider.ConstantFieldTool<?> __tool)
    {
        return !__value.isDefaultForKind() || GraalOptions.trustFinalDefaultFields;
    }

    @SuppressWarnings("unused")
    protected boolean isStableField(ResolvedJavaField __field, ConstantFieldProvider.ConstantFieldTool<?> __tool)
    {
        if (isSyntheticEnumSwitchMap(__field))
        {
            return true;
        }
        if (isWellKnownImplicitStableField(__field))
        {
            return true;
        }
        if (__field.equals(this.___stringHashField))
        {
            return true;
        }
        return false;
    }

    protected boolean isDefaultStableField(ResolvedJavaField __field, ConstantFieldProvider.ConstantFieldTool<?> __tool)
    {
        if (isSyntheticEnumSwitchMap(__field))
        {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean isFinalField(ResolvedJavaField __field, ConstantFieldProvider.ConstantFieldTool<?> __tool)
    {
        return __field.isFinal();
    }

    protected boolean isSyntheticEnumSwitchMap(ResolvedJavaField __field)
    {
        if (__field.isSynthetic() && __field.isStatic() && isArray(__field))
        {
            String __name = __field.getName();
            if (__field.isFinal() && __name.equals("$VALUES") || __name.equals("ENUM$VALUES"))
            {
                // generated int[] field for EnumClass::values()
                return true;
            }
            else if (__name.startsWith("$SwitchMap$") || __name.startsWith("$SWITCH_TABLE$"))
            {
                // javac and ecj generate a static field in an inner class for a switch on an enum
                // named $SwitchMap$p$k$g$EnumClass and $SWITCH_TABLE$p$k$g$EnumClass, respectively
                return true;
            }
        }
        return false;
    }

    // @field
    private final ResolvedJavaField ___stringValueField;
    // @field
    private final ResolvedJavaField ___stringHashField;

    protected boolean isWellKnownImplicitStableField(ResolvedJavaField __field)
    {
        return __field.equals(this.___stringValueField);
    }
}
