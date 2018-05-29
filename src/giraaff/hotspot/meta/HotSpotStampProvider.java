package giraaff.hotspot.meta;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.nodes.type.MethodPointerStamp;
import giraaff.nodes.spi.StampProvider;

// @class HotSpotStampProvider
public final class HotSpotStampProvider implements StampProvider
{
    // @cons
    public HotSpotStampProvider()
    {
        super();
    }

    @Override
    public Stamp createHubStamp(ObjectStamp object)
    {
        return KlassPointerStamp.klassNonNull();
    }

    @Override
    public Stamp createMethodStamp()
    {
        return MethodPointerStamp.methodNonNull();
    }
}
