package giraaff.hotspot;

import giraaff.core.gen.LockStackHolder;

/**
 * Extends {@link LockStackHolder} to allocate the extra debug information required for locks.
 */
// @class HotSpotLockStackHolder
public final class HotSpotLockStackHolder extends LockStackHolder
{
    // @field
    private final HotSpotLockStack lockStack;

    // @cons
    public HotSpotLockStackHolder(HotSpotLockStack __lockStack)
    {
        super();
        this.lockStack = __lockStack;
    }

    public HotSpotLockStack lockStack()
    {
        return lockStack;
    }
}
