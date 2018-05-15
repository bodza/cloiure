package graalvm.compiler.truffle.compiler.substitutions;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TruffleUseFrameWithoutBoxing;
import static graalvm.compiler.truffle.common.TruffleCompilerOptions.getValue;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

public class KnownTruffleTypes extends AbstractKnownTruffleTypes {

    public final ResolvedJavaType classFrameClass = getValue(TruffleUseFrameWithoutBoxing) ? //
                    lookupType("graalvm.compiler.truffle.runtime.FrameWithoutBoxing") : //
                    lookupType("graalvm.compiler.truffle.runtime.FrameWithBoxing");
    public final ResolvedJavaType classFrameDescriptor = lookupType("com.oracle.truffle.api.frame.FrameDescriptor");
    public final ResolvedJavaType classFrameSlot = lookupType("com.oracle.truffle.api.frame.FrameSlot");
    public final ResolvedJavaType classFrameSlotKind = lookupType("com.oracle.truffle.api.frame.FrameSlotKind");
    public final ResolvedJavaType classExactMath = lookupType("com.oracle.truffle.api.ExactMath");
    public final ResolvedJavaType classMethodHandle = lookupType(MethodHandle.class);

    public final ResolvedJavaField fieldFrameDescriptorDefaultValue = findField(classFrameDescriptor, "defaultValue");
    public final ResolvedJavaField fieldFrameDescriptorVersion = findField(classFrameDescriptor, "version");
    public final ResolvedJavaField fieldFrameDescriptorMaterializeCalled = findField(classFrameDescriptor, "materializeCalled");
    public final ResolvedJavaField fieldFrameDescriptorSlots = findField(classFrameDescriptor, "slots");

    public final ResolvedJavaField fieldArrayListElementData = findField(lookupType(ArrayList.class), "elementData");

    public final ResolvedJavaField fieldFrameSlotKind = findField(classFrameSlot, "kind");
    public final ResolvedJavaField fieldFrameSlotIndex = findField(classFrameSlot, "index");

    public final ResolvedJavaField fieldFrameSlotKindTag = findField(classFrameSlotKind, "tag");

    public final ResolvedJavaField fieldOptimizedAssumptionIsValid = findField(lookupType("com.oracle.truffle.api.impl.AbstractAssumption"), "isValid");

    public KnownTruffleTypes(MetaAccessProvider metaAccess) {
        super(metaAccess);
    }
}
