package giraaff.hotspot;

import giraaff.core.phases.CommunityCompilerConfiguration;
import giraaff.phases.tiers.CompilerConfiguration;

/**
 * Factory for creating the default configuration for the community edition of Graal.
 */
public class CommunityCompilerConfigurationFactory extends CompilerConfigurationFactory
{
    public CommunityCompilerConfigurationFactory()
    {
        super("community");
    }

    @Override
    public CompilerConfiguration createCompilerConfiguration()
    {
        return new CommunityCompilerConfiguration();
    }
}
