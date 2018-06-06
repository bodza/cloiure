package giraaff.core.common.util;

import java.util.Arrays;

///
// An expandable and indexable list of {@code int}s.
//
// This class avoids the boxing/unboxing incurred by {@code ArrayList<Integer>}.
///
// @class IntList
public final class IntList
{
    // @def
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    // @field
    private int[] ___array;
    // @field
    private int ___size;

    ///
    // Creates an int list with a specified initial capacity.
    ///
    // @cons IntList
    public IntList(int __initialCapacity)
    {
        super();
        this.___array = new int[__initialCapacity];
    }

    ///
    // Creates an int list with a specified initial array.
    //
    // @param array the initial array used for the list (no copy is made)
    // @param initialSize the initial {@linkplain #size() size} of the list (must be less than or
    //            equal to {@code array.length}
    ///
    // @cons IntList
    public IntList(int[] __array, int __initialSize)
    {
        super();
        this.___array = __array;
        this.___size = __initialSize;
    }

    ///
    // Makes a new int list by copying a range from a given int list.
    //
    // @param other the list from which a range of values is to be copied into the new list
    // @param startIndex the index in {@code other} at which to start copying
    // @param length the number of values to copy from {@code other}
    // @return a new int list whose {@linkplain #size() size} and capacity is {@code length}
    ///
    public static IntList copy(IntList __other, int __startIndex, int __length)
    {
        return copy(__other, __startIndex, __length, __length);
    }

    ///
    // Makes a new int list by copying a range from a given int list.
    //
    // @param other the list from which a range of values is to be copied into the new list
    // @param startIndex the index in {@code other} at which to start copying
    // @param length the number of values to copy from {@code other}
    // @param initialCapacity the initial capacity of the new int list (must be greater or equal to
    //            {@code length})
    // @return a new int list whose {@linkplain #size() size} is {@code length}
    ///
    public static IntList copy(IntList __other, int __startIndex, int __length, int __initialCapacity)
    {
        if (__initialCapacity == 0)
        {
            return new IntList(EMPTY_INT_ARRAY, 0);
        }
        else
        {
            int[] __array = new int[__initialCapacity];
            System.arraycopy(__other.___array, __startIndex, __array, 0, __length);
            return new IntList(__array, __length);
        }
    }

    public int size()
    {
        return this.___size;
    }

    ///
    // Appends a value to the end of this list, increasing its {@linkplain #size() size} by 1.
    //
    // @param value the value to append
    ///
    public void add(int __value)
    {
        if (this.___size == this.___array.length)
        {
            int __newSize = (this.___size * 3) / 2 + 1;
            this.___array = Arrays.copyOf(this.___array, __newSize);
        }
        this.___array[this.___size++] = __value;
    }

    ///
    // Gets the value in this list at a given index.
    //
    // @param index the index of the element to return
    // @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
    ///
    public int get(int __index)
    {
        if (__index >= this.___size)
        {
            throw new IndexOutOfBoundsException("Index: " + __index + ", Size: " + this.___size);
        }
        return this.___array[__index];
    }

    ///
    // Sets the size of this list to 0.
    ///
    public void clear()
    {
        this.___size = 0;
    }

    ///
    // Sets a value at a given index in this list.
    //
    // @param index the index of the element to update
    // @param value the new value of the element
    // @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
    ///
    public void set(int __index, int __value)
    {
        if (__index >= this.___size)
        {
            throw new IndexOutOfBoundsException("Index: " + __index + ", Size: " + this.___size);
        }
        this.___array[__index] = __value;
    }

    ///
    // Adjusts the {@linkplain #size() size} of this int list.
    //
    // If {@code newSize < size()}, the size is changed to {@code newSize}. If
    // {@code newSize > size()}, sufficient 0 elements are {@linkplain #add(int) added} until
    // {@code size() == newSize}.
    //
    // @param newSize the new size of this int list
    ///
    public void setSize(int __newSize)
    {
        if (__newSize < this.___size)
        {
            this.___size = __newSize;
        }
        else if (__newSize > this.___size)
        {
            this.___array = Arrays.copyOf(this.___array, __newSize);
        }
    }
}
