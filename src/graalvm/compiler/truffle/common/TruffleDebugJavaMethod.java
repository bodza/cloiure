package graalvm.compiler.truffle.common;

import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.JavaMethodContext;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

/**
 * Enables a Truffle compilable to masquerade as a {@link JavaMethod} for use as a context value in
 * {@linkplain DebugContext#scope(Object) debug scopes}.
 */
public class TruffleDebugJavaMethod implements JavaMethod, JavaMethodContext {
    private final CompilableTruffleAST compilable;

    private static final JavaType declaringClass = new JavaType() {

        @Override
        public String getName() {
            return "LTruffleGraal;";
        }

        @Override
        public JavaType getComponentType() {
            return null;
        }

        @Override
        public JavaType getArrayClass() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JavaKind getJavaKind() {
            return JavaKind.Object;
        }

        @Override
        public ResolvedJavaType resolve(ResolvedJavaType accessingClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TruffleDebugJavaMethod;
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    };

    private static final Signature signature = new Signature() {

        @Override
        public JavaType getReturnType(ResolvedJavaType accessingClass) {
            return declaringClass;
        }

        @Override
        public int getParameterCount(boolean receiver) {
            return 0;
        }

        @Override
        public JavaType getParameterType(int index, ResolvedJavaType accessingClass) {
            throw new IndexOutOfBoundsException();
        }
    };

    public TruffleDebugJavaMethod(CompilableTruffleAST compilable) {
        this.compilable = compilable;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TruffleDebugJavaMethod) {
            TruffleDebugJavaMethod other = (TruffleDebugJavaMethod) obj;
            return other.compilable.equals(compilable);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return compilable.hashCode();
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public String getName() {
        return (compilable.toString() + "").replace('.', '_').replace(' ', '_');
    }

    @Override
    public JavaType getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String toString() {
        return format("Truffle<%n(%p)>");
    }

    @Override
    public JavaMethod asJavaMethod() {
        return this;
    }
}
