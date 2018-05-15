package graalvm.compiler.hotspot;

import graalvm.compiler.core.phases.CommunityCompilerConfiguration;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.serviceprovider.ServiceProvider;

/**
 * Factory for creating the default configuration for the community edition of Graal.
 */
@ServiceProvider(CompilerConfigurationFactory.class)
public class CommunityCompilerConfigurationFactory extends CompilerConfigurationFactory {

    public static final String NAME = "community";

    /**
     * Must be greater than {@link EconomyCompilerConfigurationFactory#AUTO_SELECTION_PRIORITY}.
     */
    public static final int AUTO_SELECTION_PRIORITY = 2;

    public CommunityCompilerConfigurationFactory() {
        super(NAME, AUTO_SELECTION_PRIORITY);
        assert AUTO_SELECTION_PRIORITY > EconomyCompilerConfigurationFactory.AUTO_SELECTION_PRIORITY;
    }

    @Override
    public CompilerConfiguration createCompilerConfiguration() {
        return new CommunityCompilerConfiguration();
    }
}
