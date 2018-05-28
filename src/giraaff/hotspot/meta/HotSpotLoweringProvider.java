package giraaff.hotspot.meta;

import giraaff.nodes.spi.LoweringProvider;
import giraaff.options.OptionValues;

/**
 * HotSpot implementation of {@link LoweringProvider}.
 */
public interface HotSpotLoweringProvider extends LoweringProvider
{
    void initialize(OptionValues options, HotSpotProviders providers);
}
