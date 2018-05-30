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
    public static <T> T uncheckedCast(@SuppressWarnings("unused") Class<T> type, Object object)
    {
        return (T) object;
    }

    /**
     * Statically cast an object to an arbitrary Object type. Dynamically checked.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object object)
    {
        return (T) object;
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
    public static <T> void atPutGrow(List<T> list, int pos, T x, T filler)
    {
        if (list.size() < pos + 1)
        {
            while (list.size() < pos + 1)
            {
                list.add(filler);
            }
        }

        list.set(pos, x);
    }

    /**
     * Prepends the String {@code indentation} to every line in String {@code lines}, including a
     * possibly non-empty line following the final newline.
     */
    public static String indent(String lines, String indentation)
    {
        if (lines.length() == 0)
        {
            return lines;
        }
        final String newLine = "\n";
        if (lines.endsWith(newLine))
        {
            return indentation + (lines.substring(0, lines.length() - 1)).replace(newLine, newLine + indentation) + newLine;
        }
        return indentation + lines.replace(newLine, newLine + indentation);
    }

    /**
     * Returns the zero value for a given numeric kind.
     */
    public static JavaConstant zero(JavaKind kind)
    {
        switch (kind)
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
    public static JavaConstant one(JavaKind kind)
    {
        switch (kind)
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

    private static String methodName(ResolvedJavaMethod method)
    {
        return method.format("%H.%n(%p):%r") + " (" + method.getCodeSize() + " bytes)";
    }

    /**
     * Calls {@link AccessibleObject#setAccessible(boolean)} on {@code field} with the value
     * {@code flag}.
     */
    public static void setAccessible(Field field, boolean flag)
    {
        field.setAccessible(flag);
    }

    /**
     * Calls {@link AccessibleObject#setAccessible(boolean)} on {@code executable} with the value
     * {@code flag}.
     */
    public static void setAccessible(Executable executable, boolean flag)
    {
        executable.setAccessible(flag);
    }
}
