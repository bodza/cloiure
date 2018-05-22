package graalvm.compiler.hotspot.word;

import graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode;

/**
 * Marker type for a metaspace pointer to a method counters.
 */
public abstract class MethodCountersPointer extends MetaspacePointer
{
    @HotSpotOperation(opcode = HotspotOpcode.POINTER_EQ)
    public abstract boolean equal(MethodCountersPointer other);

    @HotSpotOperation(opcode = HotspotOpcode.POINTER_NE)
    public abstract boolean notEqual(MethodCountersPointer other);
}
