package giraaff.nodes;

import jdk.vm.ci.meta.JavaKind.FormatWithToString;
import jdk.vm.ci.meta.ResolvedJavaField;

import org.graalvm.word.LocationIdentity;

// @class FieldLocationIdentity
public final class FieldLocationIdentity extends LocationIdentity implements FormatWithToString
{
    // @field
    private final ResolvedJavaField inner;

    // @cons
    public FieldLocationIdentity(ResolvedJavaField __inner)
    {
        super();
        this.inner = __inner;
    }

    @Override
    public boolean isImmutable()
    {
        return false;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj instanceof FieldLocationIdentity)
        {
            FieldLocationIdentity __fieldLocationIdentity = (FieldLocationIdentity) __obj;
            return inner.equals(__fieldLocationIdentity.inner);
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
