package giraaff.phases;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaType;

// @class ClassTypeSequence
final class ClassTypeSequence implements JavaType, CharSequence
{
    // @field
    private final Class<?> clazz;

    // @cons
    ClassTypeSequence(Class<?> __clazz)
    {
        super();
        this.clazz = __clazz;
    }

    @Override
    public String getName()
    {
        return "L" + clazz.getName().replace('.', '/') + ";";
    }

    @Override
    public String toJavaName()
    {
        return toJavaName(true);
    }

    @Override
    public String toJavaName(boolean __qualified)
    {
        if (__qualified)
        {
            return clazz.getName();
        }
        else
        {
            int __lastDot = clazz.getName().lastIndexOf('.');
            return clazz.getName().substring(__lastDot + 1);
        }
    }

    @Override
    public JavaType getComponentType()
    {
        return null;
    }

    @Override
    public JavaType getArrayClass()
    {
        return null;
    }

    @Override
    public JavaKind getJavaKind()
    {
        return JavaKind.Object;
    }

    @Override
    public ResolvedJavaType resolve(ResolvedJavaType __accessingClass)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int length()
    {
        return clazz.getName().length();
    }

    @Override
    public char charAt(int __index)
    {
        return clazz.getName().charAt(__index);
    }

    @Override
    public CharSequence subSequence(int __start, int __end)
    {
        return clazz.getName().subSequence(__start, __end);
    }
}
