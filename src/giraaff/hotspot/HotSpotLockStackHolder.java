package giraaff.hotspot;

import giraaff.core.gen.LockStackHolder;

/**
 * Extends {@link LockStackHolder} to allocate the extra debug information required for locks.
 */
public class HotSpotLockStackHolder extends LockStackHolder
{
    private final HotSpotLockStack lockStack;

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
