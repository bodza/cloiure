package graalvm.compiler.core.common.spi;

import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaField;

/**
 * Implements the logic that decides whether a field read should be constant folded.
 */
public interface ConstantFieldProvider {

    public interface ConstantFieldTool<T> {

        OptionValues getOptions();

        JavaConstant readValue();

        JavaConstant getReceiver();

        T foldConstant(JavaConstant ret);

        T foldStableArray(JavaConstant ret, int stableDimensions, boolean isDefaultStable);
    }

    /**
     * Decide whether a read from the {@code field} should be constant folded. This should return
     * {@link ConstantFieldTool#foldConstant} or {@link ConstantFieldTool#foldStableArray} if the
     * read should be constant folded, or {@code null} otherwise.
     */
    <T> T readConstantField(ResolvedJavaField field, ConstantFieldTool<T> tool);
}
