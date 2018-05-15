package graalvm.compiler.hotspot.word;

import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.POINTER_EQ;
import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.POINTER_NE;
import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.TO_METHOD_POINTER;

import org.graalvm.word.Pointer;

/**
 * Marker type for a metaspace pointer to a method.
 */
public abstract class MethodPointer extends MetaspacePointer {

    @HotSpotOperation(opcode = POINTER_EQ)
    public abstract boolean equal(KlassPointer other);

    @HotSpotOperation(opcode = POINTER_NE)
    public abstract boolean notEqual(KlassPointer other);

    @HotSpotOperation(opcode = TO_METHOD_POINTER)
    public static native MethodPointer fromWord(Pointer pointer);
}
