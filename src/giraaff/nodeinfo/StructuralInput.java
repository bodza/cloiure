package giraaff.nodeinfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
// Marker type for describing node inputs in snippets that are not of type {@link InputType#Value}.
///
// @class StructuralInput
public abstract class StructuralInput
{
    // @cons StructuralInput
    private StructuralInput()
    {
        super();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    // @iface StructuralInput.MarkerType
    public @interface MarkerType
    {
        InputType value();
    }

    ///
    // Marker type for {@link InputType#Memory} edges in snippets.
    ///
    @StructuralInput.MarkerType(InputType.Memory)
    // @class StructuralInput.Memory
    public abstract static class Memory extends StructuralInput
    {
    }

    ///
    // Marker type for {@link InputType#ConditionI} edges in snippets.
    ///
    @StructuralInput.MarkerType(InputType.ConditionI)
    // @class StructuralInput.ConditionI
    public abstract static class ConditionI extends StructuralInput
    {
    }

    ///
    // Marker type for {@link InputType#StateI} edges in snippets.
    ///
    @StructuralInput.MarkerType(InputType.StateI)
    // @class StructuralInput.StateI
    public abstract static class StateI extends StructuralInput
    {
    }

    ///
    // Marker type for {@link InputType#Guard} edges in snippets.
    ///
    @StructuralInput.MarkerType(InputType.Guard)
    // @class StructuralInput.Guard
    public abstract static class Guard extends StructuralInput
    {
    }

    ///
    // Marker type for {@link InputType#Anchor} edges in snippets.
    ///
    @StructuralInput.MarkerType(InputType.Anchor)
    // @class StructuralInput.Anchor
    public abstract static class Anchor extends StructuralInput
    {
    }

    ///
    // Marker type for {@link InputType#Association} edges in snippets.
    ///
    @StructuralInput.MarkerType(InputType.Association)
    // @class StructuralInput.Association
    public abstract static class Association extends StructuralInput
    {
    }

    ///
    // Marker type for {@link InputType#Extension} edges in snippets.
    ///
    @StructuralInput.MarkerType(InputType.Extension)
    // @class StructuralInput.Extension
    public abstract static class Extension extends StructuralInput
    {
    }
}
