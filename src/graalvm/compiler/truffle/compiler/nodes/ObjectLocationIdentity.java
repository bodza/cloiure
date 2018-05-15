package graalvm.compiler.truffle.compiler.nodes;

import java.util.Objects;

import graalvm.compiler.nodes.util.JavaConstantFormattable;
import graalvm.compiler.nodes.util.JavaConstantFormatter;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

/**
 * A {@link LocationIdentity} wrapping an object.
 */
public final class ObjectLocationIdentity extends LocationIdentity implements JavaConstantFormattable {

    private final JavaConstant object;

    public static LocationIdentity create(JavaConstant object) {
        assert object.getJavaKind() == JavaKind.Object && object.isNonNull();
        return new ObjectLocationIdentity(object);
    }

    private ObjectLocationIdentity(JavaConstant object) {
        this.object = object;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ObjectLocationIdentity) {
            ObjectLocationIdentity that = (ObjectLocationIdentity) obj;
            return Objects.equals(that.object, this.object);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public String toString() {
        return "Identity(" + object + ")";
    }

    @Override
    public String format(JavaConstantFormatter formatter) {
        return "Identity(" + formatter.format(object) + ")";
    }
}
