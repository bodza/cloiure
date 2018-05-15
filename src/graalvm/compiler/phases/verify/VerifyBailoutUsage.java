package graalvm.compiler.phases.verify;

import graalvm.compiler.core.common.PermanentBailoutException;
import graalvm.compiler.core.common.RetryableBailoutException;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.phases.VerifyPhase;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class VerifyBailoutUsage extends VerifyPhase<PhaseContext> {

    private static final String[] AllowedPackagePrefixes;

    private static String getPackageName(Class<?> c) {
        String classNameWithPackage = c.getName();
        String simpleName = c.getSimpleName();
        return classNameWithPackage.substring(0, classNameWithPackage.length() - simpleName.length() - 1);
    }

    static {
        try {
            AllowedPackagePrefixes = new String[]{getPackageName(PermanentBailoutException.class), "jdk.vm.ci"};
        } catch (Throwable t) {
            throw new GraalError(t);
        }
    }

    private static boolean matchesPrefix(String packageName) {
        for (String allowedPackagePrefix : AllowedPackagePrefixes) {
            if (packageName.startsWith(allowedPackagePrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean verify(StructuredGraph graph, PhaseContext context) {
        final ResolvedJavaType bailoutType = context.getMetaAccess().lookupJavaType(BailoutException.class);
        ResolvedJavaMethod caller = graph.method();
        String holderQualified = caller.format("%H");
        String holderUnqualified = caller.format("%h");
        String packageName = holderQualified.substring(0, holderQualified.length() - holderUnqualified.length() - 1);
        if (!matchesPrefix(packageName)) {
            for (MethodCallTargetNode t : graph.getNodes(MethodCallTargetNode.TYPE)) {
                ResolvedJavaMethod callee = t.targetMethod();
                if (callee.getDeclaringClass().equals(bailoutType)) {
                    // we only allow the getter
                    if (!callee.getName().equals("isPermanent")) {
                        throw new VerificationError("Call to %s at callsite %s is prohibited. Consider using %s for permanent bailouts or %s for retryables.", callee.format("%H.%n(%p)"),
                                        caller.format("%H.%n(%p)"), PermanentBailoutException.class.getName(),
                                        RetryableBailoutException.class.getName());
                    }
                }
            }
        }
        return true;
    }

}
