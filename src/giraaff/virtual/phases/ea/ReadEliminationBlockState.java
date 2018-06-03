package giraaff.virtual.phases.ea;

import java.util.Iterator;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.word.LocationIdentity;

import giraaff.nodes.ValueNode;

/**
 * This class maintains a set of known values, identified by base object, locations and offset.
 */
// @class ReadEliminationBlockState
public final class ReadEliminationBlockState extends EffectsBlockState<ReadEliminationBlockState>
{
    // @field
    final EconomicMap<CacheEntry<?>, ValueNode> readCache;

    // @class ReadEliminationBlockState.CacheEntry
    abstract static class CacheEntry<T>
    {
        // @field
        public final ValueNode object;
        // @field
        public final T identity;

        // @cons
        CacheEntry(ValueNode __object, T __identity)
        {
            super();
            this.object = __object;
            this.identity = __identity;
        }

        public abstract CacheEntry<T> duplicateWithObject(ValueNode newObject);

        @Override
        public int hashCode()
        {
            int __result = 31 + ((identity == null) ? 0 : identity.hashCode());
            return 31 * __result + ((object == null) ? 0 : object.hashCode());
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (!(__obj instanceof CacheEntry<?>))
            {
                return false;
            }
            CacheEntry<?> __other = (CacheEntry<?>) __obj;
            return identity.equals(__other.identity) && object == __other.object;
        }

        public abstract boolean conflicts(LocationIdentity other);

        public abstract LocationIdentity getIdentity();
    }

    // @class ReadEliminationBlockState.LoadCacheEntry
    static final class LoadCacheEntry extends CacheEntry<LocationIdentity>
    {
        // @cons
        LoadCacheEntry(ValueNode __object, LocationIdentity __identity)
        {
            super(__object, __identity);
        }

        @Override
        public CacheEntry<LocationIdentity> duplicateWithObject(ValueNode __newObject)
        {
            return new LoadCacheEntry(__newObject, identity);
        }

        @Override
        public boolean conflicts(LocationIdentity __other)
        {
            return identity.equals(__other);
        }

        @Override
        public LocationIdentity getIdentity()
        {
            return identity;
        }
    }

    /**
     * CacheEntry describing an Unsafe memory reference. The memory location and the location
     * identity are separate so both must be considered when looking for optimizable memory accesses.
     */
    // @class ReadEliminationBlockState.UnsafeLoadCacheEntry
    static final class UnsafeLoadCacheEntry extends CacheEntry<ValueNode>
    {
        // @field
        private final LocationIdentity locationIdentity;

        // @cons
        UnsafeLoadCacheEntry(ValueNode __object, ValueNode __location, LocationIdentity __locationIdentity)
        {
            super(__object, __location);
            this.locationIdentity = __locationIdentity;
        }

        @Override
        public CacheEntry<ValueNode> duplicateWithObject(ValueNode __newObject)
        {
            return new UnsafeLoadCacheEntry(__newObject, identity, locationIdentity);
        }

        @Override
        public boolean conflicts(LocationIdentity __other)
        {
            return locationIdentity.equals(__other);
        }

        @Override
        public int hashCode()
        {
            return 31 * super.hashCode() + locationIdentity.hashCode();
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (__obj instanceof UnsafeLoadCacheEntry)
            {
                UnsafeLoadCacheEntry __other = (UnsafeLoadCacheEntry) __obj;
                return super.equals(__other) && locationIdentity.equals(__other.locationIdentity);
            }
            return false;
        }

        @Override
        public LocationIdentity getIdentity()
        {
            return locationIdentity;
        }
    }

    // @cons
    public ReadEliminationBlockState()
    {
        super();
        readCache = EconomicMap.create(Equivalence.DEFAULT);
    }

    // @cons
    public ReadEliminationBlockState(ReadEliminationBlockState __other)
    {
        super();
        readCache = EconomicMap.create(Equivalence.DEFAULT, __other.readCache);
    }

    @Override
    public boolean equivalentTo(ReadEliminationBlockState __other)
    {
        return isSubMapOf(readCache, __other.readCache);
    }

    public void addCacheEntry(CacheEntry<?> __identifier, ValueNode __value)
    {
        readCache.put(__identifier, __value);
    }

    public ValueNode getCacheEntry(CacheEntry<?> __identifier)
    {
        return readCache.get(__identifier);
    }

    public void killReadCache()
    {
        readCache.clear();
    }

    public void killReadCache(LocationIdentity __identity)
    {
        Iterator<CacheEntry<?>> __iterator = readCache.getKeys().iterator();
        while (__iterator.hasNext())
        {
            CacheEntry<?> __entry = __iterator.next();
            if (__entry.conflicts(__identity))
            {
                __iterator.remove();
            }
        }
    }

    public EconomicMap<CacheEntry<?>, ValueNode> getReadCache()
    {
        return readCache;
    }
}
