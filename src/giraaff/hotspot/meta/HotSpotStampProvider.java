package giraaff.hotspot.meta;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.nodes.type.MethodPointerStamp;
import giraaff.nodes.spi.StampProvider;

// @class HotSpotStampProvider
public final class HotSpotStampProvider implements StampProvider
{
    // @cons HotSpotStampProvider
    public HotSpotStampProvider()
    {
        super();
    }

    @Override
    public Stamp createHubStamp(ObjectStamp __object)
    {
        return KlassPointerStamp.klassNonNull();
    }

    @Override
    public Stamp createMethodStamp()
    {
        return MethodPointerStamp.methodNonNull();
    }
}
