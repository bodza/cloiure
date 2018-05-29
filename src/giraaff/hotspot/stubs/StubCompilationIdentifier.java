package giraaff.hotspot.stubs;

import java.util.concurrent.atomic.AtomicLong;

import giraaff.core.common.CompilationIdentifier;
import giraaff.util.GraalError;

/**
 * {@link CompilationIdentifier} for {@linkplain Stub stub compilations}.
 */
// @class StubCompilationIdentifier
public final class StubCompilationIdentifier implements CompilationIdentifier
{
    private static final AtomicLong uniqueStubIds = new AtomicLong();
    private final long id;
    private final Stub stub;

    // @cons
    public StubCompilationIdentifier(Stub stub)
    {
        super();
        this.id = uniqueStubIds.getAndIncrement();
        this.stub = stub;
    }

    @Override
    public final String toString()
    {
        return toString(Verbosity.DETAILED);
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        switch (verbosity)
        {
            case ID:
                return buildID();
            case NAME:
                return buildName();
            case DETAILED:
                return buildID() + '[' + buildName() + ']';
        }
        throw new GraalError("unknown verbosity: " + verbosity);
    }

    private String buildName()
    {
        return stub.toString();
    }

    private String buildID()
    {
        return "StubCompilation-" + id;
    }
}
