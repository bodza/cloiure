package giraaff.core.common.type;

///
// A pair of stamp with one being the stamp that can be trusted and the other one being a guess that
// needs a dynamic check to be used.
///
// @class StampPair
public final class StampPair
{
    // @field
    private final Stamp ___trustedStamp;
    // @field
    private final Stamp ___uncheckedStamp;

    // @cons
    private StampPair(Stamp __trustedStamp, Stamp __uncheckedStamp)
    {
        super();
        this.___trustedStamp = __trustedStamp;
        this.___uncheckedStamp = __uncheckedStamp;
    }

    public static StampPair create(Stamp __trustedStamp, Stamp __uncheckedStamp)
    {
        return new StampPair(__trustedStamp, __uncheckedStamp);
    }

    public static StampPair createSingle(Stamp __stamp)
    {
        return new StampPair(__stamp, null);
    }

    public Stamp getUncheckedStamp()
    {
        return this.___uncheckedStamp;
    }

    public Stamp getTrustedStamp()
    {
        return this.___trustedStamp;
    }
}
