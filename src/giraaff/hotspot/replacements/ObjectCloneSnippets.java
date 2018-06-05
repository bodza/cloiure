package giraaff.hotspot.replacements;

import java.lang.reflect.Method;
import java.util.EnumMap;

import jdk.vm.ci.meta.JavaKind;

import giraaff.api.directives.GraalDirectives;
import giraaff.api.replacements.Snippet;
import giraaff.hotspot.replacements.arraycopy.ArrayCopyCallNode;
import giraaff.nodes.java.DynamicNewArrayNode;
import giraaff.nodes.java.NewArrayNode;
import giraaff.replacements.Snippets;
import giraaff.util.GraalError;

// @class ObjectCloneSnippets
public final class ObjectCloneSnippets implements Snippets
{
    // @def
    public static final EnumMap<JavaKind, Method> arrayCloneMethods = new EnumMap<>(JavaKind.class);

    static
    {
        arrayCloneMethods.put(JavaKind.Boolean, getCloneMethod("booleanArrayClone", boolean[].class));
        arrayCloneMethods.put(JavaKind.Byte, getCloneMethod("byteArrayClone", byte[].class));
        arrayCloneMethods.put(JavaKind.Char, getCloneMethod("charArrayClone", char[].class));
        arrayCloneMethods.put(JavaKind.Short, getCloneMethod("shortArrayClone", short[].class));
        arrayCloneMethods.put(JavaKind.Int, getCloneMethod("intArrayClone", int[].class));
        arrayCloneMethods.put(JavaKind.Long, getCloneMethod("longArrayClone", long[].class));
        arrayCloneMethods.put(JavaKind.Object, getCloneMethod("objectArrayClone", Object[].class));
    }

    private static Method getCloneMethod(String __name, Class<?> __param)
    {
        try
        {
            return ObjectCloneSnippets.class.getDeclaredMethod(__name, __param);
        }
        catch (SecurityException | NoSuchMethodException __e)
        {
            throw new GraalError(__e);
        }
    }

    @Snippet
    public static boolean[] booleanArrayClone(boolean[] __src)
    {
        boolean[] __result = (boolean[]) NewArrayNode.newUninitializedArray(Boolean.TYPE, __src.length);
        ArrayCopyCallNode.disjointArraycopy(__src, 0, __result, 0, __src.length, JavaKind.Boolean);
        return __result;
    }

    @Snippet
    public static byte[] byteArrayClone(byte[] __src)
    {
        byte[] __result = (byte[]) NewArrayNode.newUninitializedArray(Byte.TYPE, __src.length);
        ArrayCopyCallNode.disjointArraycopy(__src, 0, __result, 0, __src.length, JavaKind.Byte);
        return __result;
    }

    @Snippet
    public static short[] shortArrayClone(short[] __src)
    {
        short[] __result = (short[]) NewArrayNode.newUninitializedArray(Short.TYPE, __src.length);
        ArrayCopyCallNode.disjointArraycopy(__src, 0, __result, 0, __src.length, JavaKind.Short);
        return __result;
    }

    @Snippet
    public static char[] charArrayClone(char[] __src)
    {
        char[] __result = (char[]) NewArrayNode.newUninitializedArray(Character.TYPE, __src.length);
        ArrayCopyCallNode.disjointArraycopy(__src, 0, __result, 0, __src.length, JavaKind.Char);
        return __result;
    }

    @Snippet
    public static int[] intArrayClone(int[] __src)
    {
        int[] __result = (int[]) NewArrayNode.newUninitializedArray(Integer.TYPE, __src.length);
        ArrayCopyCallNode.disjointArraycopy(__src, 0, __result, 0, __src.length, JavaKind.Int);
        return __result;
    }

    @Snippet
    public static long[] longArrayClone(long[] __src)
    {
        long[] __result = (long[]) NewArrayNode.newUninitializedArray(Long.TYPE, __src.length);
        ArrayCopyCallNode.disjointArraycopy(__src, 0, __result, 0, __src.length, JavaKind.Long);
        return __result;
    }

    @Snippet
    public static Object[] objectArrayClone(Object[] __src)
    {
        // since this snippet is lowered early, the array must be initialized
        Object[] __result = (Object[]) DynamicNewArrayNode.newArray(GraalDirectives.guardingNonNull(__src.getClass().getComponentType()), __src.length, JavaKind.Object);
        ArrayCopyCallNode.disjointUninitializedArraycopy(__src, 0, __result, 0, __src.length, JavaKind.Object);
        return __result;
    }
}
