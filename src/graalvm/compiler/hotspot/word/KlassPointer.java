package graalvm.compiler.hotspot.word;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;

import graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode;
import graalvm.compiler.word.Word.Opcode;
import graalvm.compiler.word.Word.Operation;

/**
 * Marker type for a metaspace pointer to a type.
 */
public abstract class KlassPointer extends MetaspacePointer
{
    @HotSpotOperation(opcode = HotspotOpcode.POINTER_EQ)
    public abstract boolean equal(KlassPointer other);

    @HotSpotOperation(opcode = HotspotOpcode.POINTER_NE)
    public abstract boolean notEqual(KlassPointer other);

    @HotSpotOperation(opcode = HotspotOpcode.TO_KLASS_POINTER)
    public static native KlassPointer fromWord(Pointer pointer);

    @HotSpotOperation(opcode = HotspotOpcode.READ_KLASS_POINTER)
    public native KlassPointer readKlassPointer(int offset, LocationIdentity locationIdentity);

    @Operation(opcode = Opcode.WRITE_POINTER)
    public native void writeKlassPointer(int offset, KlassPointer t, LocationIdentity locationIdentity);
}
