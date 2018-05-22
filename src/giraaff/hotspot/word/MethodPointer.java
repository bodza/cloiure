package giraaff.hotspot.word;

import org.graalvm.word.Pointer;

import giraaff.hotspot.word.HotSpotOperation.HotspotOpcode;

/**
 * Marker type for a metaspace pointer to a method.
 */
public abstract class MethodPointer extends MetaspacePointer
{
    @HotSpotOperation(opcode = HotspotOpcode.POINTER_EQ)
    public abstract boolean equal(KlassPointer other);

    @HotSpotOperation(opcode = HotspotOpcode.POINTER_NE)
    public abstract boolean notEqual(KlassPointer other);

    @HotSpotOperation(opcode = HotspotOpcode.TO_METHOD_POINTER)
    public static native MethodPointer fromWord(Pointer pointer);
}
