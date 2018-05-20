package graalvm.compiler.core.common.util;

import java.util.Arrays;

/**
 * An expandable and indexable list of {@code int}s.
 *
 * This class avoids the boxing/unboxing incurred by {@code ArrayList<Integer>}.
 */
public final class IntList
{
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private int[] array;
    private int size;

    /**
     * Creates an int list with a specified initial capacity.
     */
    public IntList(int initialCapacity)
    {
        array = new int[initialCapacity];
    }

    /**
     * Creates an int list with a specified initial array.
     *
     * @param array the initial array used for the list (no copy is made)
     * @param initialSize the initial {@linkplain #size() size} of the list (must be less than or
     *            equal to {@code array.length}
     */
    public IntList(int[] array, int initialSize)
    {
        this.array = array;
        this.size = initialSize;
    }

    /**
     * Makes a new int list by copying a range from a given int list.
     *
     * @param other the list from which a range of values is to be copied into the new list
     * @param startIndex the index in {@code other} at which to start copying
     * @param length the number of values to copy from {@code other}
     * @return a new int list whose {@linkplain #size() size} and capacity is {@code length}
     */
    public static IntList copy(IntList other, int startIndex, int length)
    {
        return copy(other, startIndex, length, length);
    }

    /**
     * Makes a new int list by copying a range from a given int list.
     *
     * @param other the list from which a range of values is to be copied into the new list
     * @param startIndex the index in {@code other} at which to start copying
     * @param length the number of values to copy from {@code other}
     * @param initialCapacity the initial capacity of the new int list (must be greater or equal to
     *            {@code length})
     * @return a new int list whose {@linkplain #size() size} is {@code length}
     */
    public static IntList copy(IntList other, int startIndex, int length, int initialCapacity)
    {
        if (initialCapacity == 0)
        {
            return new IntList(EMPTY_INT_ARRAY, 0);
        }
        else
        {
            int[] array = new int[initialCapacity];
            System.arraycopy(other.array, startIndex, array, 0, length);
            return new IntList(array, length);
        }
    }

    public int size()
    {
        return size;
    }

    /**
     * Appends a value to the end of this list, increasing its {@linkplain #size() size} by 1.
     *
     * @param value the value to append
     */
    public void add(int value)
    {
        if (size == array.length)
        {
            int newSize = (size * 3) / 2 + 1;
            array = Arrays.copyOf(array, newSize);
        }
        array[size++] = value;
    }

    /**
     * Gets the value in this list at a given index.
     *
     * @param index the index of the element to return
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public int get(int index)
    {
        if (index >= size)
        {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return array[index];
    }

    /**
     * Sets the size of this list to 0.
     */
    public void clear()
    {
        size = 0;
    }

    /**
     * Sets a value at a given index in this list.
     *
     * @param index the index of the element to update
     * @param value the new value of the element
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public void set(int index, int value)
    {
        if (index >= size)
        {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        array[index] = value;
    }

    /**
     * Adjusts the {@linkplain #size() size} of this int list.
     *
     * If {@code newSize < size()}, the size is changed to {@code newSize}. If
     * {@code newSize > size()}, sufficient 0 elements are {@linkplain #add(int) added} until
     * {@code size() == newSize}.
     *
     * @param newSize the new size of this int list
     */
    public void setSize(int newSize)
    {
        if (newSize < size)
        {
            size = newSize;
        }
        else if (newSize > size)
        {
            array = Arrays.copyOf(array, newSize);
        }
    }

    @Override
    public String toString()
    {
        if (array.length == size)
        {
            return Arrays.toString(array);
        }
        return Arrays.toString(Arrays.copyOf(array, size));
    }
}
