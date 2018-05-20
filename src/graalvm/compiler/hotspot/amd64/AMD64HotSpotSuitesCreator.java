package graalvm.compiler.hotspot.amd64;

import graalvm.compiler.core.amd64.AMD64SuitesCreator;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.phases.tiers.CompilerConfiguration;

public class AMD64HotSpotSuitesCreator extends AMD64SuitesCreator
{
    public AMD64HotSpotSuitesCreator(CompilerConfiguration compilerConfiguration, Plugins plugins)
    {
        super(compilerConfiguration, plugins);
    }
}
