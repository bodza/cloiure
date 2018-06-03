package giraaff.bytecode;

import java.lang.annotation.Annotation;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.Bytecodes;

/**
 * Utilities for working around the absence of method annotations and parameter annotations on
 * bridge methods where the bridged methods have method annotations or parameter annotations. Not
 * all Java compilers copy method annotations and parameter annotations to bridge methods.
 */
// @class BridgeMethodUtils
public final class BridgeMethodUtils
{
    // @cons
    private BridgeMethodUtils()
    {
        super();
    }

    /**
     * Gets the method bridged to by a {@linkplain ResolvedJavaMethod#isBridge() bridge} method. The
     * value returned is the method called by {@code method} that has the same name as {@code bridge}.
     *
     * @param bridge a bridge method
     * @return the method called by {@code bridge} whose name is the same as {@code bridge.getName()}
     */
    public static ResolvedJavaMethod getBridgedMethod(ResolvedJavaMethod __bridge)
    {
        Bytecode __code = new ResolvedJavaMethodBytecode(__bridge);
        BytecodeStream __stream = new BytecodeStream(__code.getCode());
        int __opcode = __stream.currentBC();
        ResolvedJavaMethod __bridged = null;
        boolean __calledAbstractMethodErrorConstructor = false;
        while (__opcode != Bytecodes.END)
        {
            switch (__opcode)
            {
                case Bytecodes.INVOKEVIRTUAL:
                case Bytecodes.INVOKESPECIAL:
                case Bytecodes.INVOKESTATIC:
                case Bytecodes.INVOKEINTERFACE:
                {
                    int __cpi = __stream.readCPI();
                    ConstantPool __cp = __code.getConstantPool();
                    __cp.loadReferencedType(__cpi, __opcode);
                    ResolvedJavaMethod __method = (ResolvedJavaMethod) __cp.lookupMethod(__cpi, __opcode);
                    if (__method.getName().equals(__bridge.getName()))
                    {
                        return __method;
                    }
                    else if (__method.getName().equals("<init>") && __method.getDeclaringClass().getName().equals("Ljava/lang/AbstractMethodError;"))
                    {
                        __calledAbstractMethodErrorConstructor = true;
                    }
                    break;
                }
                case Bytecodes.ATHROW:
                {
                    if (__calledAbstractMethodErrorConstructor)
                    {
                        // this is a miranda method
                        return null;
                    }
                }
            }
            __stream.next();
            __opcode = __stream.currentBC();
        }
        if (__bridged == null)
        {
            throw new InternalError("Couldn't find method bridged by " + __bridge.format("%R %H.%n(%P)"));
        }
        return __bridged;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getAnnotation(Class)} that handles the absence of
     * annotations on bridge methods where the bridged method has annotations.
     */
    public static <T extends Annotation> T getAnnotation(Class<T> __annotationClass, ResolvedJavaMethod __method)
    {
        T __a = __method.getAnnotation(__annotationClass);
        if (__a == null && __method.isBridge())
        {
            ResolvedJavaMethod __bridged = getBridgedMethod(__method);
            if (__bridged != null)
            {
                __a = __bridged.getAnnotation(__annotationClass);
            }
        }
        return __a;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getAnnotations()} that handles the absence of
     * annotations on bridge methods where the bridged method has annotations.
     */
    public static Annotation[] getAnnotations(ResolvedJavaMethod __method)
    {
        Annotation[] __a = __method.getAnnotations();
        if (__a.length == 0 && __method.isBridge())
        {
            ResolvedJavaMethod __bridged = getBridgedMethod(__method);
            if (__bridged != null)
            {
                __a = __bridged.getAnnotations();
            }
        }
        return __a;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getDeclaredAnnotations()} that handles the absence of
     * annotations on bridge methods where the bridged method has annotations.
     */
    public static Annotation[] getDeclaredAnnotations(ResolvedJavaMethod __method)
    {
        Annotation[] __a = __method.getAnnotations();
        if (__a.length == 0 && __method.isBridge())
        {
            ResolvedJavaMethod __bridged = getBridgedMethod(__method);
            if (__bridged != null)
            {
                __a = __bridged.getDeclaredAnnotations();
            }
        }
        return __a;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getParameterAnnotations()} that handles the absence of
     * parameter annotations on bridge methods where the bridged method has parameter annotations.
     */
    public static Annotation[][] getParameterAnnotations(ResolvedJavaMethod __method)
    {
        Annotation[][] __a = __method.getParameterAnnotations();
        if (__a.length == 0 && __method.isBridge())
        {
            ResolvedJavaMethod __bridged = getBridgedMethod(__method);
            if (__bridged != null)
            {
                __a = __bridged.getParameterAnnotations();
            }
        }
        return __a;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getParameterAnnotation(Class, int)} that handles the
     * absence of parameter annotations on bridge methods where the bridged method has parameter annotations.
     */
    public static <T extends Annotation> T getParameterAnnotation(Class<T> __annotationClass, int __parameterIndex, ResolvedJavaMethod __method)
    {
        T __a = __method.getParameterAnnotation(__annotationClass, __parameterIndex);
        if (__a == null && __method.isBridge())
        {
            ResolvedJavaMethod __bridged = getBridgedMethod(__method);
            if (__bridged != null)
            {
                __a = __bridged.getParameterAnnotation(__annotationClass, __parameterIndex);
            }
        }
        return __a;
    }
}
