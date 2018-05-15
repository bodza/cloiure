package graalvm.compiler.hotspot;

import graalvm.compiler.core.phases.EconomyCompilerConfiguration;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.serviceprovider.ServiceProvider;

/**
 * Factory that creates a {@link EconomyCompilerConfiguration}.
 */
@ServiceProvider(CompilerConfigurationFactory.class)
public class EconomyCompilerConfigurationFactory extends CompilerConfigurationFactory {

    public static final String NAME = "economy";

    public static final int AUTO_SELECTION_PRIORITY = 1;

    public EconomyCompilerConfigurationFactory() {
        super(NAME, AUTO_SELECTION_PRIORITY);
    }

    @Override
    public CompilerConfiguration createCompilerConfiguration() {
        return new EconomyCompilerConfiguration();
    }

    @Override
    public BackendMap createBackendMap() {
        // the economy configuration only differs in the frontend, it reuses the "community" backend
        return new DefaultBackendMap(CommunityCompilerConfigurationFactory.NAME);
    }
}
