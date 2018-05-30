package giraaff.core.common.type;

/**
 * A pair of stamp with one being the stamp that can be trusted and the other one being a guess that
 * needs a dynamic check to be used.
 */
// @class StampPair
public final class StampPair
{
    private final Stamp trustedStamp;
    private final Stamp uncheckedStamp;

    // @cons
    private StampPair(Stamp trustedStamp, Stamp uncheckedStamp)
    {
        super();
        this.trustedStamp = trustedStamp;
        this.uncheckedStamp = uncheckedStamp;
    }

    public static StampPair create(Stamp trustedStamp, Stamp uncheckedStamp)
    {
        return new StampPair(trustedStamp, uncheckedStamp);
    }

    public static StampPair createSingle(Stamp stamp)
    {
        return new StampPair(stamp, null);
    }

    public Stamp getUncheckedStamp()
    {
        return uncheckedStamp;
    }

    public Stamp getTrustedStamp()
    {
        return trustedStamp;
    }
}
