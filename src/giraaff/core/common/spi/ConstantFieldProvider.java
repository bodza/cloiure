package giraaff.core.common.spi;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaField;

import giraaff.options.OptionValues;

/**
 * Implements the logic that decides whether a field read should be constant folded.
 */
// @iface ConstantFieldProvider
public interface ConstantFieldProvider
{
    // @iface ConstantFieldProvider.ConstantFieldTool
    public interface ConstantFieldTool<T>
    {
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
