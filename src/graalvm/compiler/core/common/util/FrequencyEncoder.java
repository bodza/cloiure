package graalvm.compiler.core.common.util;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

/**
 * Creates an array of T objects order by the occurrence frequency of each object. The most
 * frequently used object is the first one, the least frequently used the last one. If {@code null}
 * is added, it is always the first element.
 *
 * Either object {@link #createIdentityEncoder() identity} or object {@link #createEqualityEncoder()
 * equality} can be used to build the array and count the frequency.
 */
public class FrequencyEncoder<T>
{
    static class Entry<T>
    {
        protected final T object;
        protected int frequency;
        protected int index;

        protected Entry(T object)
        {
            this.object = object;
            this.index = -1;
        }
    }

    protected final EconomicMap<T, Entry<T>> map;
    protected boolean containsNull;

    /**
     * Creates an encoder that uses object identity.
     */
    public static <T> FrequencyEncoder<T> createIdentityEncoder()
    {
        return new FrequencyEncoder<>(EconomicMap.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE));
    }

    /**
     * Creates an encoder that uses {@link Object#equals(Object) object equality}.
     */
    public static <T> FrequencyEncoder<T> createEqualityEncoder()
    {
        return new FrequencyEncoder<>(EconomicMap.create(Equivalence.DEFAULT));
    }

    protected FrequencyEncoder(EconomicMap<T, Entry<T>> map)
    {
        this.map = map;
    }

    /**
     * Adds an object to the array.
     */
    public void addObject(T object)
    {
        if (object == null)
        {
            containsNull = true;
            return;
        }

        Entry<T> entry = map.get(object);
        if (entry == null)
        {
            entry = new Entry<>(object);
            map.put(object, entry);
        }
        entry.frequency++;
    }

    /**
     * Returns the index of an object in the array. The object must have been
     * {@link #addObject(Object) added} before.
     */
    public int getIndex(T object)
    {
        if (object == null)
        {
            return 0;
        }
        Entry<T> entry = map.get(object);
        return entry.index;
    }

    /**
     * Returns the number of distinct objects that have been added, i.e., the length of the array.
     */
    public int getLength()
    {
        return map.size() + (containsNull ? 1 : 0);
    }

    /**
     * Fills the provided array with the added objects. The array must have the {@link #getLength()
     * correct length}.
     */
    public T[] encodeAll(T[] allObjects)
    {
        List<Entry<T>> sortedEntries = new ArrayList<>(allObjects.length);
        for (Entry<T> value : map.getValues())
        {
            sortedEntries.add(value);
        }
        sortedEntries.sort((e1, e2) -> -Integer.compare(e1.frequency, e2.frequency));

        int offset = 0;
        if (containsNull)
        {
            allObjects[0] = null;
            offset = 1;
        }
        for (int i = 0; i < sortedEntries.size(); i++)
        {
            Entry<T> entry = sortedEntries.get(i);
            int index = i + offset;
            entry.index = index;
            allObjects[index] = entry.object;
        }
        return allObjects;
    }
}
