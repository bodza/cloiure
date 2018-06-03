package giraaff.hotspot.meta;

import giraaff.nodes.spi.LoweringProvider;

///
// HotSpot implementation of {@link LoweringProvider}.
///
// @iface HotSpotLoweringProvider
public interface HotSpotLoweringProvider extends LoweringProvider
{
    void initialize(HotSpotProviders __providers);
}
