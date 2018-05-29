package giraaff.core.common;

import jdk.vm.ci.code.CompilationRequest;

/**
 * A unique identifier for a compilation. Compiled code can be mapped to a single compilation id.
 * The reverse is not true since the compiler might bailout in which case no code is installed.
 */
// @iface CompilationIdentifier
public interface CompilationIdentifier
{
    // @enum CompilationIdentifier.Verbosity
    enum Verbosity
    {
        /**
         * Only the unique identifier of the compilation.
         */
        ID,
        /**
         * Only the name of the compilation unit.
         */
        NAME,
        /**
         * {@link #ID} + a readable description.
         */
        DETAILED
    }

    CompilationRequestIdentifier INVALID_COMPILATION_ID = new CompilationRequestIdentifier()
    {
        @Override
        public String toString()
        {
            return toString(Verbosity.DETAILED);
        }

        @Override
        public String toString(Verbosity verbosity)
        {
            return "InvalidCompilationID";
        }

        @Override
        public CompilationRequest getRequest()
        {
            return null;
        }
    };

    /**
     * This method is a shortcut for {@link #toString(Verbosity)} with {@link Verbosity#DETAILED}.
     */
    @Override
    String toString();

    /**
     * Creates a String representation for this compilation identifier with a given
     * {@link Verbosity}.
     */
    String toString(Verbosity verbosity);
}
