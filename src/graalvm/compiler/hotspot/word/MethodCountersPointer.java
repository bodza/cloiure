package graalvm.compiler.hotspot.word;

import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.POINTER_EQ;
import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.POINTER_NE;

/**
 * Marker type for a metaspace pointer to a method counters.
 */
public abstract class MethodCountersPointer extends MetaspacePointer {

    @HotSpotOperation(opcode = POINTER_EQ)
    public abstract boolean equal(MethodCountersPointer other);

    @HotSpotOperation(opcode = POINTER_NE)
    public abstract boolean notEqual(MethodCountersPointer other);
}
