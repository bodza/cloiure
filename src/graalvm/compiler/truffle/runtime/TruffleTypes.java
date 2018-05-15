package graalvm.compiler.truffle.runtime;

/**
 * A service for getting types that can be resolved by
 * {@link GraalTruffleRuntime#resolveType(jdk.vm.ci.meta.MetaAccessProvider, String, boolean)}.
 */
public interface TruffleTypes {

    /**
     * Gets the types supplied by this supplier.
     */
    Class<?>[] getTypes();
}
