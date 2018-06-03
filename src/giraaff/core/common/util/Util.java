package giraaff.core.common.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.List;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.util.GraalError;

/**
 * The {@code Util} class contains a motley collection of utility methods used throughout the compiler.
 */
// @class Util
public final class Util
{
    // @cons
    private Util()
    {
        super();
    }

    /**
     * Statically cast an object to an arbitrary Object type. Dynamically checked.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(@SuppressWarnings("unused") Class<T> __type, Object __object)
    {
        return (T) __object;
    }

    /**
     * Statically cast an object to an arbitrary Object type. Dynamically checked.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object __object)
    {
        return (T) __object;
    }

    /**
     * Sets the element at a given position of a list and ensures that this position exists. If the
     * list is current shorter than the position, intermediate positions are filled with a given value.
     *
     * @param list the list to put the element into
     * @param pos the position at which to insert the element
     * @param x the element that should be inserted
     * @param filler the filler element that is used for the intermediate positions in case the list
     *            is shorter than pos
     */
    public static <T> void atPutGrow(List<T> __list, int __pos, T __x, T __filler)
    {
        if (__list.size() < __pos + 1)
        {
            while (__list.size() < __pos + 1)
            {
                __list.add(__filler);
            }
        }

        __list.set(__pos, __x);
    }

    /**
     * Prepends the String {@code indentation} to every line in String {@code lines}, including a
     * possibly non-empty line following the final newline.
     */
    public static String indent(String __lines, String __indentation)
    {
        if (__lines.length() == 0)
        {
            return __lines;
        }
        final String __newLine = "\n";
        if (__lines.endsWith(__newLine))
        {
            return __indentation + (__lines.substring(0, __lines.length() - 1)).replace(__newLine, __newLine + __indentation) + __newLine;
        }
        return __indentation + __lines.replace(__newLine, __newLine + __indentation);
    }

    /**
     * Returns the zero value for a given numeric kind.
     */
    public static JavaConstant zero(JavaKind __kind)
    {
        switch (__kind)
        {
            case Boolean:
                return JavaConstant.FALSE;
            case Byte:
                return JavaConstant.forByte((byte) 0);
            case Char:
                return JavaConstant.forChar((char) 0);
            case Double:
                return JavaConstant.DOUBLE_0;
            case Float:
                return JavaConstant.FLOAT_0;
            case Int:
                return JavaConstant.INT_0;
            case Long:
                return JavaConstant.LONG_0;
            case Short:
                return JavaConstant.forShort((short) 0);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    /**
     * Returns the one value for a given numeric kind.
     */
    public static JavaConstant one(JavaKind __kind)
    {
        switch (__kind)
        {
            case Boolean:
                return JavaConstant.TRUE;
            case Byte:
                return JavaConstant.forByte((byte) 1);
            case Char:
                return JavaConstant.forChar((char) 1);
            case Double:
                return JavaConstant.DOUBLE_1;
            case Float:
                return JavaConstant.FLOAT_1;
            case Int:
                return JavaConstant.INT_1;
            case Long:
                return JavaConstant.LONG_1;
            case Short:
                return JavaConstant.forShort((short) 1);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private static String methodName(ResolvedJavaMethod __method)
    {
        return __method.format("%H.%n(%p):%r") + " (" + __method.getCodeSize() + " bytes)";
    }

    /**
     * Calls {@link AccessibleObject#setAccessible(boolean)} on {@code field} with the value
     * {@code flag}.
     */
    public static void setAccessible(Field __field, boolean __flag)
    {
        __field.setAccessible(__flag);
    }

    /**
     * Calls {@link AccessibleObject#setAccessible(boolean)} on {@code executable} with the value
     * {@code flag}.
     */
    public static void setAccessible(Executable __executable, boolean __flag)
    {
        __executable.setAccessible(__flag);
    }
}
