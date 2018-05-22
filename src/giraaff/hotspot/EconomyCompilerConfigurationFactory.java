package giraaff.hotspot;

import giraaff.core.phases.EconomyCompilerConfiguration;
import giraaff.phases.tiers.CompilerConfiguration;

/**
 * Factory that creates a {@link EconomyCompilerConfiguration}.
 */
public class EconomyCompilerConfigurationFactory extends CompilerConfigurationFactory
{
    public static final String NAME = "economy";

    public static final int AUTO_SELECTION_PRIORITY = 1;

    public EconomyCompilerConfigurationFactory()
    {
        super(NAME, AUTO_SELECTION_PRIORITY);
    }

    @Override
    public CompilerConfiguration createCompilerConfiguration()
    {
        return new EconomyCompilerConfiguration();
    }

    @Override
    public BackendMap createBackendMap()
    {
        // the economy configuration only differs in the frontend, it reuses the "community" backend
        return new DefaultBackendMap(CommunityCompilerConfigurationFactory.NAME);
    }
}
