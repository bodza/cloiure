package giraaff.hotspot.word;

import giraaff.hotspot.word.HotSpotOperation.HotspotOpcode;

///
// Marker type for a metaspace pointer to a method counters.
///
// @class MethodCountersPointer
public abstract class MethodCountersPointer extends MetaspacePointer
{
    @HotSpotOperation(opcode = HotspotOpcode.POINTER_EQ)
    public abstract boolean equal(MethodCountersPointer __other);

    @HotSpotOperation(opcode = HotspotOpcode.POINTER_NE)
    public abstract boolean notEqual(MethodCountersPointer __other);
}
