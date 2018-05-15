package graalvm.compiler.truffle.compiler.hotspot;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import graalvm.compiler.truffle.compiler.substitutions.KnownTruffleTypes;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

public final class HotSpotKnownTruffleTypes extends KnownTruffleTypes {

    public final ResolvedJavaType classWeakReference = lookupType(WeakReference.class);
    public final ResolvedJavaType classSoftReference = lookupType(SoftReference.class);
    public final ResolvedJavaField referenceReferent = findField(lookupType(Reference.class), "referent");

    public HotSpotKnownTruffleTypes(MetaAccessProvider metaAccess) {
        super(metaAccess);
    }

}
