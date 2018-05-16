package graalvm.compiler.phases.verify;

import java.lang.annotation.Annotation;

import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.phases.VerifyPhase;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Verifies a method is annotated with CallerSensitive iff it calls Reflection#getCallerClass().
 */
public class VerifyCallerSensitiveMethods extends VerifyPhase<PhaseContext> {

    Class<? extends Annotation> callerSensitiveClass;
    Class<?> reflectionClass;

    @Override
    public boolean checkContract() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public VerifyCallerSensitiveMethods() {
        try {
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            reflectionClass = classLoader.loadClass("jdk.internal.reflect.Reflection");
            callerSensitiveClass = (Class<? extends Annotation>) classLoader.loadClass("jdk.internal.reflect.ConstantPool");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected boolean verify(StructuredGraph graph, PhaseContext context) {
        Invoke invoke = callsReflectionGetCallerClass(graph, context);
        Annotation annotation = graph.method().getAnnotation(callerSensitiveClass);
        if (invoke != null) {
            if (annotation == null) {
                StackTraceElement e = graph.method().asStackTraceElement(invoke.bci());
                throw new VerificationError(String.format("%s: method that calls Reflection.getCallerClass() must be annotated with @CallerSensitive", e));
            }

        } else if (annotation != null) {
            throw new VerificationError(String.format("%s: method annotated with @CallerSensitive does not call Reflection.getCallerClass()", graph.method().format("%H.%n(%p)")));
        }
        return true;
    }

    private Invoke callsReflectionGetCallerClass(StructuredGraph graph, PhaseContext context) {
        ResolvedJavaType reflectionType = context.getMetaAccess().lookupJavaType(reflectionClass);
        for (MethodCallTargetNode t : graph.getNodes(MethodCallTargetNode.TYPE)) {
            ResolvedJavaMethod callee = t.targetMethod();
            if (callee.getDeclaringClass().equals(reflectionType)) {
                if (callee.getName().equals("getCallerClass")) {
                    return t.invoke();
                }
            }
        }
        return null;
    }
}
