package graalvm.compiler.hotspot.meta;

import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.hotspot.nodes.type.KlassPointerStamp;
import graalvm.compiler.hotspot.nodes.type.MethodPointerStamp;
import graalvm.compiler.nodes.spi.StampProvider;

public class HotSpotStampProvider implements StampProvider
{
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
