package graalvm.compiler.options;

/**
 * An interface to a set of {@link OptionDescriptor}s.
 */
public interface OptionDescriptors extends Iterable<OptionDescriptor> {
    /**
     * Gets the {@link OptionDescriptor} matching a given option name or {@code null} if this option
     * descriptor set doesn't contain a matching option name.
     */
    OptionDescriptor get(String value);
}
