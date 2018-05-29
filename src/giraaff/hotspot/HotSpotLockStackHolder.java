package giraaff.hotspot;

import giraaff.core.gen.LockStackHolder;

/**
 * Extends {@link LockStackHolder} to allocate the extra debug information required for locks.
 */
// @class HotSpotLockStackHolder
public final class HotSpotLockStackHolder extends LockStackHolder
{
    private final HotSpotLockStack lockStack;

    // @cons
    public HotSpotLockStackHolder(HotSpotLockStack lockStack)
    {
        super();
        this.lockStack = lockStack;
    }

    public HotSpotLockStack lockStack()
    {
        return lockStack;
    }
}
