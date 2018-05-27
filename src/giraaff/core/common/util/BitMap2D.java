package giraaff.core.common.util;

import java.util.BitSet;

/**
 * This class implements a two-dimensional bitmap.
 */
public final class BitMap2D
{
    private BitSet map;
    private final int bitsPerSlot;

    private int bitIndex(int slotIndex, int bitWithinSlotIndex)
    {
        return slotIndex * bitsPerSlot + bitWithinSlotIndex;
    }

    private boolean verifyBitWithinSlotIndex(int index)
    {
        return true;
    }

    public BitMap2D(int sizeInSlots, int bitsPerSlot)
    {
        map = new BitSet(sizeInSlots * bitsPerSlot);
        this.bitsPerSlot = bitsPerSlot;
    }

    public int sizeInBits()
    {
        return map.size();
    }

    // returns number of full slots that have been allocated
    public int sizeInSlots()
    {
        return map.size() / bitsPerSlot;
    }

    public boolean isValidIndex(int slotIndex, int bitWithinSlotIndex)
    {
        return (bitIndex(slotIndex, bitWithinSlotIndex) < sizeInBits());
    }

    public boolean at(int slotIndex, int bitWithinSlotIndex)
    {
        return map.get(bitIndex(slotIndex, bitWithinSlotIndex));
    }

    public void setBit(int slotIndex, int bitWithinSlotIndex)
    {
        map.set(bitIndex(slotIndex, bitWithinSlotIndex));
    }

    public void clearBit(int slotIndex, int bitWithinSlotIndex)
    {
        map.clear(bitIndex(slotIndex, bitWithinSlotIndex));
    }

    public void atPutGrow(int slotIndex, int bitWithinSlotIndex, boolean value)
    {
        int size = sizeInSlots();
        if (size <= slotIndex)
        {
            while (size <= slotIndex)
            {
                size *= 2;
            }
            BitSet newBitMap = new BitSet(size * bitsPerSlot);
            newBitMap.or(map);
            map = newBitMap;
        }

        if (value)
        {
            setBit(slotIndex, bitWithinSlotIndex);
        }
        else
        {
            clearBit(slotIndex, bitWithinSlotIndex);
        }
    }

    public void clear()
    {
        map.clear();
    }
}
