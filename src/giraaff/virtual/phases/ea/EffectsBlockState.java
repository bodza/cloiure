package giraaff.virtual.phases.ea;

import java.util.Iterator;
import java.util.Map;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableMapCursor;

// @class EffectsBlockState
public abstract class EffectsBlockState<T extends EffectsBlockState<T>>
{
    /**
     * This flag specifies whether this block is unreachable, which can happen during analysis if
     * conditions turn constant or nodes canonicalize to cfg sinks.
     */
    // @field
    private boolean dead;

    // @cons
    public EffectsBlockState()
    {
        super();
        // emtpy
    }

    // @cons
    public EffectsBlockState(EffectsBlockState<T> __other)
    {
        super();
        this.dead = __other.dead;
    }

    protected abstract boolean equivalentTo(T other);

    public boolean isDead()
    {
        return dead;
    }

    public void markAsDead()
    {
        this.dead = true;
    }

    /**
     * Returns true if every value in subMap is also present in the superMap (according to "equals" semantics).
     */
    protected static <K, V> boolean isSubMapOf(EconomicMap<K, V> __superMap, EconomicMap<K, V> __subMap)
    {
        if (__superMap == __subMap)
        {
            return true;
        }
        UnmodifiableMapCursor<K, V> __cursor = __subMap.getEntries();
        while (__cursor.advance())
        {
            K __key = __cursor.getKey();
            V __value = __cursor.getValue();
            V __otherValue = __superMap.get(__key);
            if (__otherValue != __value && !__value.equals(__otherValue))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Modifies target so that only entries that have corresponding entries in source remain.
     */
    protected static <U, V> void meetMaps(Map<U, V> __target, Map<U, V> __source)
    {
        Iterator<Map.Entry<U, V>> __iter = __target.entrySet().iterator();
        while (__iter.hasNext())
        {
            Map.Entry<U, V> __entry = __iter.next();
            if (!__source.containsKey(__entry.getKey()))
            {
                __iter.remove();
            }
        }
    }
}
