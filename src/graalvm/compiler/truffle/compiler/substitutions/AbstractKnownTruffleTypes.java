package graalvm.compiler.truffle.compiler.substitutions;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Base class for references to well known types and fields.
 */
public class AbstractKnownTruffleTypes {
    protected final TruffleCompilerRuntime runtime = TruffleCompilerRuntime.getRuntime();
    protected final MetaAccessProvider metaAccess;

    protected AbstractKnownTruffleTypes(MetaAccessProvider metaAccess) {
        this.metaAccess = metaAccess;
    }

    protected ResolvedJavaType lookupType(String className) {
        return runtime.resolveType(metaAccess, className);
    }

    protected ResolvedJavaType lookupType(Class<?> c) {
        return metaAccess.lookupJavaType(c);
    }

    static class FieldsCache {
        final ResolvedJavaType declaringClass;
        final ResolvedJavaField[] fields;

        FieldsCache(ResolvedJavaType declaringClass, ResolvedJavaField[] fields) {
            this.declaringClass = declaringClass;
            this.fields = fields;
        }
    }

    private FieldsCache fieldsCache;

    protected ResolvedJavaField findField(ResolvedJavaType declaringClass, String name) {
        FieldsCache fc = fieldsCache;
        if (fc == null || !fc.declaringClass.equals(declaringClass)) {
            fc = new FieldsCache(declaringClass, declaringClass.getInstanceFields(false));
            fieldsCache = fc;
        }
        for (ResolvedJavaField f : fc.fields) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        throw new GraalError("Could not find required field %s.%s", declaringClass.getName(), name);
    }
}
