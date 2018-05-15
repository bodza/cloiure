package graalvm.compiler.truffle.compiler;

import org.graalvm.collections.EconomicMap;
import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime.ConstantFieldInfo;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;

public class TruffleConstantFieldProvider implements ConstantFieldProvider {
    private final ConstantFieldProvider graalConstantFieldProvider;
    private final MetaAccessProvider metaAccess;
    private final EconomicMap<ResolvedJavaField, ConstantFieldInfo> cachedConstantFieldInfo;

    public TruffleConstantFieldProvider(ConstantFieldProvider graalConstantFieldProvider, MetaAccessProvider metaAccess) {
        this.graalConstantFieldProvider = graalConstantFieldProvider;
        this.metaAccess = metaAccess;
        this.cachedConstantFieldInfo = EconomicMap.create();
    }

    @Override
    public <T> T readConstantField(ResolvedJavaField field, ConstantFieldTool<T> tool) {
        boolean isStaticField = field.isStatic();
        if (!isStaticField && tool.getReceiver().isNull()) {
            // can't be optimized
            return null;
        }

        boolean isArrayField = field.getType().isArray();
        if (!isArrayField) {
            // The fast way does not require any annotation processing but only covers the most
            // frequent cases. It must not be used for array fields as it might return an incorrect
            // value for the number of stable dimensions.
            T ret = readConstantFieldFast(field, tool);
            if (ret != null) {
                return ret;
            }
        }

        boolean hasObjectKind = field.getType().getJavaKind() == JavaKind.Object;
        ConstantFieldInfo info = getConstantFieldInfo(field);
        if (info != null) {
            if (info.isChildren()) {
                int stableDimensions = isArrayField ? 1 : 0;
                return tool.foldStableArray(verifyFieldValue(field, tool.readValue(), info), stableDimensions, true);
            } else if (!isStaticField && hasObjectKind) {
                if (info.isChild()) {
                    return tool.foldConstant(verifyFieldValue(field, tool.readValue(), info));
                }
            }
            if (isArrayField) {
                int dimensions = info.getDimensions();
                assert dimensions >= 0;
                return tool.foldStableArray(tool.readValue(), dimensions, true);
            } else {
                return tool.foldConstant(tool.readValue());
            }
        }

        if (isArrayField) {
            return readConstantFieldFast(field, tool);
        }
        return null;
    }

    private ConstantFieldInfo getConstantFieldInfo(ResolvedJavaField field) {
        ConstantFieldInfo info = cachedConstantFieldInfo.get(field);
        if (info == null) {
            info = TruffleCompilerRuntime.getRuntime().getConstantFieldInfo(field);
            cachedConstantFieldInfo.put(field, info);
        }
        return info;
    }

    private <T> T readConstantFieldFast(ResolvedJavaField field, ConstantFieldTool<T> tool) {
        T ret = graalConstantFieldProvider.readConstantField(field, tool);
        if (ret == null && field.isFinal()) {
            ret = tool.foldConstant(tool.readValue());
        }
        return ret;
    }

    private JavaConstant verifyFieldValue(ResolvedJavaField field, JavaConstant constant, ConstantFieldInfo info) {
        assert !info.isChild() || constant.isNull() ||
                        TruffleCompilerRuntime.getRuntime().resolveType(metaAccess, "com.oracle.truffle.api.nodes.Node").isAssignableFrom(metaAccess.lookupJavaType(constant)) : String.format(
                                        "@Child field value must be a Node: %s, but was: %s", field, constant);
        assert !info.isChildren() || constant.isNull() ||
                        metaAccess.lookupJavaType(constant).isArray() : String.format("@Children field value must be an array: %s, but was: %s", field, constant);
        return constant;
    }
}
