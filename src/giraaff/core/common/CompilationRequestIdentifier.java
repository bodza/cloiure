package giraaff.core.common;

import jdk.vm.ci.code.CompilationRequest;

/**
 * A {@link CompilationIdentifier} based on a {@link CompilationRequest}.
 */
public interface CompilationRequestIdentifier extends CompilationIdentifier
{
    CompilationRequest getRequest();

    /**
     * Returns the {@link CompilationRequestIdentifier#getRequest() request} from a
     * {@link CompilationRequestIdentifier}. Returns {@code null} if the
     * {@link CompilationIdentifier identifier} does not have one.
     */
    static CompilationRequest asCompilationRequest(CompilationIdentifier compilationId)
    {
        if (compilationId instanceof CompilationRequestIdentifier)
        {
            return ((CompilationRequestIdentifier) compilationId).getRequest();
        }
        return null;
    }
}
