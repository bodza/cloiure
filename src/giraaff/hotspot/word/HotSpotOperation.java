package giraaff.hotspot.word;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// @iface HotSpotOperation
public @interface HotSpotOperation
{
    // @enum HotSpotOperation.HotspotOpcode
    enum HotspotOpcode
    {
        FROM_POINTER,
        TO_KLASS_POINTER,
        TO_METHOD_POINTER,
        POINTER_EQ,
        POINTER_NE,
        IS_NULL,
        READ_KLASS_POINTER
    }

    HotSpotOperation.HotspotOpcode opcode();
}
