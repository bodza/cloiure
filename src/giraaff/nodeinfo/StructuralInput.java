package giraaff.nodeinfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker type for describing node inputs in snippets that are not of type {@link InputType#Value}.
 */
// @class StructuralInput
public abstract class StructuralInput
{
    // @cons
    private StructuralInput()
    {
        super();
        throw new Error("Illegal instance of StructuralInput. This class should be used in snippets only.");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface MarkerType
    {
        InputType value();
    }

    /**
     * Marker type for {@link InputType#Memory} edges in snippets.
     */
    @MarkerType(InputType.Memory)
    // @class StructuralInput.Memory
    public abstract static class Memory extends StructuralInput
    {
    }

    /**
     * Marker type for {@link InputType#Condition} edges in snippets.
     */
    @MarkerType(InputType.Condition)
    // @class StructuralInput.Condition
    public abstract static class Condition extends StructuralInput
    {
    }

    /**
     * Marker type for {@link InputType#State} edges in snippets.
     */
    @MarkerType(InputType.State)
    // @class StructuralInput.State
    public abstract static class State extends StructuralInput
    {
    }

    /**
     * Marker type for {@link InputType#Guard} edges in snippets.
     */
    @MarkerType(InputType.Guard)
    // @class StructuralInput.Guard
    public abstract static class Guard extends StructuralInput
    {
    }

    /**
     * Marker type for {@link InputType#Anchor} edges in snippets.
     */
    @MarkerType(InputType.Anchor)
    // @class StructuralInput.Anchor
    public abstract static class Anchor extends StructuralInput
    {
    }

    /**
     * Marker type for {@link InputType#Association} edges in snippets.
     */
    @MarkerType(InputType.Association)
    // @class StructuralInput.Association
    public abstract static class Association extends StructuralInput
    {
    }

    /**
     * Marker type for {@link InputType#Extension} edges in snippets.
     */
    @MarkerType(InputType.Extension)
    // @class StructuralInput.Extension
    public abstract static class Extension extends StructuralInput
    {
    }
}
