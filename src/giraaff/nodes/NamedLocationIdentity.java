package giraaff.nodes;

import java.util.EnumMap;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaKind.FormatWithToString;

import org.graalvm.word.LocationIdentity;

///
// A {@link LocationIdentity} with a name.
///
// @class NamedLocationIdentity
public class NamedLocationIdentity extends LocationIdentity implements FormatWithToString
{
    ///
    // Denotes the location of a value that is guaranteed to be unchanging.
    ///
    // @def
    public static final LocationIdentity FINAL_LOCATION = NamedLocationIdentity.immutable("FINAL_LOCATION");

    ///
    // Denotes the location of the length field of a Java array.
    ///
    // @def
    public static final LocationIdentity ARRAY_LENGTH_LOCATION = NamedLocationIdentity.immutable("[].length");

    ///
    // Denotes an off-heap address.
    ///
    // @def
    public static final LocationIdentity OFF_HEAP_LOCATION = NamedLocationIdentity.mutable("OFF_HEAP_LOCATION");

    // @field
    private final String ___name;
    // @field
    private final boolean ___immutable;

    // @cons
    protected NamedLocationIdentity(String __name, boolean __immutable)
    {
        super();
        this.___name = __name;
        this.___immutable = __immutable;
    }

    ///
    // Creates a named unique location identity for read and write operations against mutable memory.
    //
    // @param name the name of the new location identity
    ///
    public static NamedLocationIdentity mutable(String __name)
    {
        return create(__name, false);
    }

    ///
    // Creates a named unique location identity for read operations against immutable memory.
    // Immutable memory will never have a visible write in the graph, which is more restrictive than
    // Java final.
    //
    // @param name the name of the new location identity
    ///
    public static NamedLocationIdentity immutable(String __name)
    {
        return create(__name, true);
    }

    ///
    // Creates a named unique location identity for read and write operations.
    //
    // @param name the name of the new location identity
    // @param immutable true if the location is immutable
    ///
    private static NamedLocationIdentity create(String __name, boolean __immutable)
    {
        return new NamedLocationIdentity(__name, __immutable);
    }

    @Override
    public boolean isImmutable()
    {
        return this.___immutable;
    }

    ///
    // Returns the named location identity for an array of the given element kind. Array accesses of
    // the same kind must have the same location identity unless an alias analysis guarantees that
    // two distinct arrays are accessed.
    ///
    public static LocationIdentity getArrayLocation(JavaKind __elementKind)
    {
        return ARRAY_LOCATIONS.get(__elementKind);
    }

    // @def
    private static final EnumMap<JavaKind, LocationIdentity> ARRAY_LOCATIONS = initArrayLocations();

    private static EnumMap<JavaKind, LocationIdentity> initArrayLocations()
    {
        EnumMap<JavaKind, LocationIdentity> __result = new EnumMap<>(JavaKind.class);
        for (JavaKind __kind : JavaKind.values())
        {
            __result.put(__kind, NamedLocationIdentity.mutable("Array: " + __kind.getJavaName()));
        }
        return __result;
    }
}
