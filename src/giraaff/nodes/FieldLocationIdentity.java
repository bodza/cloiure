package giraaff.nodes;

import jdk.vm.ci.meta.JavaKind.FormatWithToString;
import jdk.vm.ci.meta.ResolvedJavaField;

import org.graalvm.word.LocationIdentity;

// @class FieldLocationIdentity
public final class FieldLocationIdentity extends LocationIdentity implements FormatWithToString
{
    private final ResolvedJavaField inner;

    // @cons
    public FieldLocationIdentity(ResolvedJavaField inner)
    {
        super();
        this.inner = inner;
    }

    @Override
    public boolean isImmutable()
    {
        return false;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof FieldLocationIdentity)
        {
            FieldLocationIdentity fieldLocationIdentity = (FieldLocationIdentity) obj;
            return inner.equals(fieldLocationIdentity.inner);
        }
        return false;
    }

    public ResolvedJavaField getField()
    {
        return inner;
    }

    @Override
    public int hashCode()
    {
        return inner.hashCode();
    }
}
