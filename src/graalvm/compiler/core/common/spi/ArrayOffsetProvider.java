package graalvm.compiler.core.common.spi;

import jdk.vm.ci.meta.JavaKind;

public interface ArrayOffsetProvider {

    int arrayBaseOffset(JavaKind elementKind);

    int arrayScalingFactor(JavaKind elementKind);
}
