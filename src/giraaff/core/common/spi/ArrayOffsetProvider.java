package giraaff.core.common.spi;

import jdk.vm.ci.meta.JavaKind;

// @iface ArrayOffsetProvider
public interface ArrayOffsetProvider
{
    int arrayBaseOffset(JavaKind elementKind);

    int arrayScalingFactor(JavaKind elementKind);
}
