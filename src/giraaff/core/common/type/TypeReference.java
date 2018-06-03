package giraaff.core.common.type;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ResolvedJavaType;

///
// This class represents a reference to a Java type and whether this reference is referring only to
// the represented type or also to its sub types in the class hierarchy. When creating a type
// reference, the following options have to be considered:
//
// <li>The reference should always only refer to the given concrete type. Use
// {@link #createExactTrusted(ResolvedJavaType)} for this purpose.</li>
// <li>The reference should be created without assumptions about the class hierarchy. The returned
// reference is exact only when the type is a leaf type (i.e., it cannot have subclasses). Depending
// on whether interface types can be trusted for this type reference use
// {@link #createWithoutAssumptions} or {@link #createTrustedWithoutAssumptions}.</li>
// <li>The reference should be created using assumptions about the class hierarchy. The returned
// reference is also exact, when there is only a single concrete sub type for the given type.
// Depending on whether interface types can be trusted for this type reference use {@link #create}
// or {@link #createTrusted}.</li>
//
// For the methods with untrusted interface types, a {@code null} reference will be constructed for
// untrusted interface types. Examples for interface types that cannot be trusted are types for
// parameters, fields, and return values. They are not checked by the Java verifier.
///
// @class TypeReference
public final class TypeReference
{
    // @field
    private final ResolvedJavaType ___type;
    // @field
    private final boolean ___exactReference;

    // @cons
    private TypeReference(ResolvedJavaType __type, boolean __exactReference)
    {
        super();
        this.___type = __type;
        this.___exactReference = __exactReference;
    }

    ///
    // Creates an exact type reference using the given type.
    ///
    public static TypeReference createExactTrusted(ResolvedJavaType __type)
    {
        if (__type == null)
        {
            return null;
        }
        return new TypeReference(__type, true);
    }

    ///
    // Creates a type reference using the given type without assumptions and without trusting interface types.
    ///
    public static TypeReference createWithoutAssumptions(ResolvedJavaType __type)
    {
        return create(null, __type);
    }

    ///
    // Creates a type reference using the given type without assumptions and trusting interface types.
    ///
    public static TypeReference createTrustedWithoutAssumptions(ResolvedJavaType __type)
    {
        return createTrusted(null, __type);
    }

    ///
    // Creates a type reference using the given type with assumptions and without trusting interface types.
    ///
    public static TypeReference create(Assumptions __assumptions, ResolvedJavaType __type)
    {
        return createTrusted(__assumptions, filterInterfaceTypesOut(__type));
    }

    ///
    // Create a type reference using the given type with assumptions and trusting interface types.
    ///
    public static TypeReference createTrusted(Assumptions __assumptions, ResolvedJavaType __type)
    {
        if (__type == null)
        {
            return null;
        }
        ResolvedJavaType __exactType = __type.isLeaf() ? __type : null;
        if (__exactType == null)
        {
            Assumptions.AssumptionResult<ResolvedJavaType> __leafConcreteSubtype = __type.findLeafConcreteSubtype();
            if (__leafConcreteSubtype != null && __leafConcreteSubtype.canRecordTo(__assumptions))
            {
                __leafConcreteSubtype.recordTo(__assumptions);
                __exactType = __leafConcreteSubtype.getResult();
            }
        }
        if (__exactType == null)
        {
            return new TypeReference(__type, false);
        }
        return new TypeReference(__exactType, true);
    }

    ///
    // The type this reference refers to.
    ///
    public ResolvedJavaType getType()
    {
        return this.___type;
    }

    ///
    // @return {@code true} if this reference is exact and only refers to the given type and
    //         {@code false} if it also refers to its sub types.
    ///
    public boolean isExact()
    {
        return this.___exactReference;
    }

    ///
    // @return A new reference that is guaranteed to be exact.
    ///
    public TypeReference asExactReference()
    {
        if (isExact())
        {
            return this;
        }
        return new TypeReference(this.___type, true);
    }

    private static ResolvedJavaType filterInterfaceTypesOut(ResolvedJavaType __type)
    {
        if (__type != null)
        {
            if (__type.isArray())
            {
                ResolvedJavaType __componentType = filterInterfaceTypesOut(__type.getComponentType());
                if (__componentType != null)
                {
                    return __componentType.getArrayClass();
                }
                // returns Object[].class
                return __type.getSuperclass().getArrayClass();
            }
            if (__type.isInterface())
            {
                return null;
            }
        }
        return __type;
    }
}
