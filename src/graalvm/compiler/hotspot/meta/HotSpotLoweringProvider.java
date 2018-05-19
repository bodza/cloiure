package graalvm.compiler.hotspot.meta;

import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.nodes.spi.LoweringProvider;
import graalvm.compiler.options.OptionValues;

/**
 * HotSpot implementation of {@link LoweringProvider}.
 */
public interface HotSpotLoweringProvider extends LoweringProvider
{
    void initialize(OptionValues options, HotSpotProviders providers, GraalHotSpotVMConfig config);
}
