package graalvm.compiler.hotspot.word;

import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.POINTER_EQ;
import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.POINTER_NE;
import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.READ_KLASS_POINTER;
import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.TO_KLASS_POINTER;

import graalvm.compiler.word.Word.Opcode;
import graalvm.compiler.word.Word.Operation;
import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;

/**
 * Marker type for a metaspace pointer to a type.
 */
public abstract class KlassPointer extends MetaspacePointer {

    @HotSpotOperation(opcode = POINTER_EQ)
    public abstract boolean equal(KlassPointer other);

    @HotSpotOperation(opcode = POINTER_NE)
    public abstract boolean notEqual(KlassPointer other);

    @HotSpotOperation(opcode = TO_KLASS_POINTER)
    public static native KlassPointer fromWord(Pointer pointer);

    @HotSpotOperation(opcode = READ_KLASS_POINTER)
    public native KlassPointer readKlassPointer(int offset, LocationIdentity locationIdentity);

    @Operation(opcode = Opcode.WRITE_POINTER)
    public native void writeKlassPointer(int offset, KlassPointer t, LocationIdentity locationIdentity);
}
