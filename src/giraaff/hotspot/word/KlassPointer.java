package giraaff.hotspot.word;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;

import giraaff.hotspot.word.HotSpotOperation.HotspotOpcode;
import giraaff.word.Word.Opcode;
import giraaff.word.Word.Operation;

///
// Marker type for a metaspace pointer to a type.
///
// @class KlassPointer
public abstract class KlassPointer extends MetaspacePointer
{
    @HotSpotOperation(opcode = HotspotOpcode.POINTER_EQ)
    public abstract boolean equal(KlassPointer __other);

    @HotSpotOperation(opcode = HotspotOpcode.POINTER_NE)
    public abstract boolean notEqual(KlassPointer __other);

    @HotSpotOperation(opcode = HotspotOpcode.TO_KLASS_POINTER)
    public static native KlassPointer fromWord(Pointer __pointer);

    @HotSpotOperation(opcode = HotspotOpcode.READ_KLASS_POINTER)
    public native KlassPointer readKlassPointer(int __offset, LocationIdentity __locationIdentity);

    @Operation(opcode = Opcode.WRITE_POINTER)
    public native void writeKlassPointer(int __offset, KlassPointer __t, LocationIdentity __locationIdentity);
}
