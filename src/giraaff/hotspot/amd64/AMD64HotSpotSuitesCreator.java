package giraaff.hotspot.amd64;

import giraaff.core.amd64.AMD64SuitesCreator;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.phases.tiers.CompilerConfiguration;

public class AMD64HotSpotSuitesCreator extends AMD64SuitesCreator
{
    public AMD64HotSpotSuitesCreator(CompilerConfiguration compilerConfiguration, Plugins plugins)
    {
        super(compilerConfiguration, plugins);
    }
}
