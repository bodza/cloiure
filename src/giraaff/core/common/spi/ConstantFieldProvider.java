package giraaff.core.common.spi;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaField;

///
// Implements the logic that decides whether a field read should be constant folded.
///
// @iface ConstantFieldProvider
public interface ConstantFieldProvider
{
    // @iface ConstantFieldProvider.ConstantFieldTool
    public interface ConstantFieldTool<T>
    {
        JavaConstant readValue();

        JavaConstant getReceiver();

        T foldConstant(JavaConstant __ret);

        T foldStableArray(JavaConstant __ret, int __stableDimensions, boolean __isDefaultStable);
    }

    ///
    // Decide whether a read from the {@code field} should be constant folded. This should return
    // {@link ConstantFieldTool#foldConstant} or {@link ConstantFieldTool#foldStableArray} if the
    // read should be constant folded, or {@code null} otherwise.
    ///
    <T> T readConstantField(ResolvedJavaField __field, ConstantFieldTool<T> __tool);
}
