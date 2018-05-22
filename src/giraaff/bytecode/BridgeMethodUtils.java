package giraaff.bytecode;

import java.lang.annotation.Annotation;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.Bytecodes;

/**
 * Utilities for working around the absence of method annotations and parameter annotations on
 * bridge methods where the bridged methods have method annotations or parameter annotations. Not
 * all Java compilers copy method annotations and parameter annotations to bridge methods.
 *
 * @see <a href="http://bugs.java.com/view_bug.do?bug_id=6695379">6695379</a>
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=495396">495396</a>
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=427745">427745</a>
 */
public class BridgeMethodUtils
{
    /**
     * Gets the method bridged to by a {@linkplain ResolvedJavaMethod#isBridge() bridge} method. The
     * value returned is the method called by {@code method} that has the same name as
     * {@code bridge}.
     *
     * @param bridge a bridge method
     * @return the method called by {@code bridge} whose name is the same as
     *         {@code bridge.getName()}
     */
    public static ResolvedJavaMethod getBridgedMethod(ResolvedJavaMethod bridge)
    {
        Bytecode code = new ResolvedJavaMethodBytecode(bridge);
        BytecodeStream stream = new BytecodeStream(code.getCode());
        int opcode = stream.currentBC();
        ResolvedJavaMethod bridged = null;
        boolean calledAbstractMethodErrorConstructor = false;
        while (opcode != Bytecodes.END)
        {
            switch (opcode)
            {
                case Bytecodes.INVOKEVIRTUAL:
                case Bytecodes.INVOKESPECIAL:
                case Bytecodes.INVOKESTATIC:
                case Bytecodes.INVOKEINTERFACE:
                {
                    int cpi = stream.readCPI();
                    ConstantPool cp = code.getConstantPool();
                    cp.loadReferencedType(cpi, opcode);
                    ResolvedJavaMethod method = (ResolvedJavaMethod) cp.lookupMethod(cpi, opcode);
                    if (method.getName().equals(bridge.getName()))
                    {
                        return method;
                    }
                    else if (method.getName().equals("<init>") && method.getDeclaringClass().getName().equals("Ljava/lang/AbstractMethodError;"))
                    {
                        calledAbstractMethodErrorConstructor = true;
                    }
                    break;
                }
                case Bytecodes.ATHROW:
                {
                    if (calledAbstractMethodErrorConstructor)
                    {
                        // This is a miranda method
                        return null;
                    }
                }
            }
            stream.next();
            opcode = stream.currentBC();
        }
        if (bridged == null)
        {
            String dis = new BytecodeDisassembler().disassemble(bridge);
            throw new InternalError(String.format("Couldn't find method bridged by %s:%n%s", bridge.format("%R %H.%n(%P)"), dis));
        }
        return bridged;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getAnnotation(Class)} that handles the absence of
     * annotations on bridge methods where the bridged method has annotations.
     */
    public static <T extends Annotation> T getAnnotation(Class<T> annotationClass, ResolvedJavaMethod method)
    {
        T a = method.getAnnotation(annotationClass);
        if (a == null && method.isBridge())
        {
            ResolvedJavaMethod bridged = getBridgedMethod(method);
            if (bridged != null)
            {
                a = bridged.getAnnotation(annotationClass);
            }
        }
        return a;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getAnnotations()} that handles the absence of
     * annotations on bridge methods where the bridged method has annotations.
     */
    public static Annotation[] getAnnotations(ResolvedJavaMethod method)
    {
        Annotation[] a = method.getAnnotations();
        if (a.length == 0 && method.isBridge())
        {
            ResolvedJavaMethod bridged = getBridgedMethod(method);
            if (bridged != null)
            {
                a = bridged.getAnnotations();
            }
        }
        return a;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getDeclaredAnnotations()} that handles the absence of
     * annotations on bridge methods where the bridged method has annotations.
     */
    public static Annotation[] getDeclaredAnnotations(ResolvedJavaMethod method)
    {
        Annotation[] a = method.getAnnotations();
        if (a.length == 0 && method.isBridge())
        {
            ResolvedJavaMethod bridged = getBridgedMethod(method);
            if (bridged != null)
            {
                a = bridged.getDeclaredAnnotations();
            }
        }
        return a;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getParameterAnnotations()} that handles the absence of
     * parameter annotations on bridge methods where the bridged method has parameter annotations.
     */
    public static Annotation[][] getParameterAnnotations(ResolvedJavaMethod method)
    {
        Annotation[][] a = method.getParameterAnnotations();
        if (a.length == 0 && method.isBridge())
        {
            ResolvedJavaMethod bridged = getBridgedMethod(method);
            if (bridged != null)
            {
                a = bridged.getParameterAnnotations();
            }
        }
        return a;
    }

    /**
     * A helper for {@link ResolvedJavaMethod#getParameterAnnotation(Class, int)} that handles the
     * absence of parameter annotations on bridge methods where the bridged method has parameter
     * annotations.
     */
    public static <T extends Annotation> T getParameterAnnotation(Class<T> annotationClass, int parameterIndex, ResolvedJavaMethod method)
    {
        T a = method.getParameterAnnotation(annotationClass, parameterIndex);
        if (a == null && method.isBridge())
        {
            ResolvedJavaMethod bridged = getBridgedMethod(method);
            if (bridged != null)
            {
                a = bridged.getParameterAnnotation(annotationClass, parameterIndex);
            }
        }
        return a;
    }
}
