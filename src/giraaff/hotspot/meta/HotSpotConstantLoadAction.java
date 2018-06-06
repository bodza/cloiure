package giraaff.hotspot.meta;

// @enum HotSpotConstantLoadAction
public enum HotSpotConstantLoadAction
{
    RESOLVE(0),
    INITIALIZE(1),
    MAKE_NOT_ENTRANT(2),
    LOAD_COUNTERS(3);

    // @field
    private int ___value;

    // @cons HotSpotConstantLoadAction
    HotSpotConstantLoadAction(int __value)
    {
        this.___value = __value;
    }

    public int value()
    {
        return this.___value;
    }
}
