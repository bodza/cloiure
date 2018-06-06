package giraaff.core.common.util;

import java.util.BitSet;

///
// This class implements a two-dimensional bitmap.
///
// @class BitMap2D
public final class BitMap2D
{
    // @field
    private BitSet ___map;
    // @field
    private final int ___bitsPerSlot;

    private int bitIndex(int __slotIndex, int __bitWithinSlotIndex)
    {
        return __slotIndex * this.___bitsPerSlot + __bitWithinSlotIndex;
    }

    private boolean verifyBitWithinSlotIndex(int __index)
    {
        return true;
    }

    // @cons BitMap2D
    public BitMap2D(int __sizeInSlots, int __bitsPerSlot)
    {
        super();
        this.___map = new BitSet(__sizeInSlots * __bitsPerSlot);
        this.___bitsPerSlot = __bitsPerSlot;
    }

    public int sizeInBits()
    {
        return this.___map.size();
    }

    // returns number of full slots that have been allocated
    public int sizeInSlots()
    {
        return this.___map.size() / this.___bitsPerSlot;
    }

    public boolean isValidIndex(int __slotIndex, int __bitWithinSlotIndex)
    {
        return (bitIndex(__slotIndex, __bitWithinSlotIndex) < sizeInBits());
    }

    public boolean at(int __slotIndex, int __bitWithinSlotIndex)
    {
        return this.___map.get(bitIndex(__slotIndex, __bitWithinSlotIndex));
    }

    public void setBit(int __slotIndex, int __bitWithinSlotIndex)
    {
        this.___map.set(bitIndex(__slotIndex, __bitWithinSlotIndex));
    }

    public void clearBit(int __slotIndex, int __bitWithinSlotIndex)
    {
        this.___map.clear(bitIndex(__slotIndex, __bitWithinSlotIndex));
    }

    public void atPutGrow(int __slotIndex, int __bitWithinSlotIndex, boolean __value)
    {
        int __size = sizeInSlots();
        if (__size <= __slotIndex)
        {
            while (__size <= __slotIndex)
            {
                __size *= 2;
            }
            BitSet __newBitMap = new BitSet(__size * this.___bitsPerSlot);
            __newBitMap.or(this.___map);
            this.___map = __newBitMap;
        }

        if (__value)
        {
            setBit(__slotIndex, __bitWithinSlotIndex);
        }
        else
        {
            clearBit(__slotIndex, __bitWithinSlotIndex);
        }
    }

    public void clear()
    {
        this.___map.clear();
    }
}
