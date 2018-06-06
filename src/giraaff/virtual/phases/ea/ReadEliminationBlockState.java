package giraaff.virtual.phases.ea;

import java.util.Iterator;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.word.LocationIdentity;

import giraaff.nodes.ValueNode;

///
// This class maintains a set of known values, identified by base object, locations and offset.
///
// @class ReadEliminationBlockState
public final class ReadEliminationBlockState extends EffectsBlockState<ReadEliminationBlockState>
{
    // @field
    final EconomicMap<ReadEliminationBlockState.CacheEntry<?>, ValueNode> ___readCache;

    // @class ReadEliminationBlockState.CacheEntry
    abstract static class CacheEntry<T>
    {
        // @field
        public final ValueNode ___object;
        // @field
        public final T ___identity;

        // @cons ReadEliminationBlockState.CacheEntry
        CacheEntry(ValueNode __object, T __identity)
        {
            super();
            this.___object = __object;
            this.___identity = __identity;
        }

        public abstract ReadEliminationBlockState.CacheEntry<T> duplicateWithObject(ValueNode __newObject);

        @Override
        public int hashCode()
        {
            int __result = 31 + ((this.___identity == null) ? 0 : this.___identity.hashCode());
            return 31 * __result + ((this.___object == null) ? 0 : this.___object.hashCode());
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (!(__obj instanceof ReadEliminationBlockState.CacheEntry<?>))
            {
                return false;
            }
            ReadEliminationBlockState.CacheEntry<?> __other = (ReadEliminationBlockState.CacheEntry<?>) __obj;
            return this.___identity.equals(__other.___identity) && this.___object == __other.___object;
        }

        public abstract boolean conflicts(LocationIdentity __other);

        public abstract LocationIdentity getIdentity();
    }

    // @class ReadEliminationBlockState.LoadCacheEntry
    static final class LoadCacheEntry extends ReadEliminationBlockState.CacheEntry<LocationIdentity>
    {
        // @cons ReadEliminationBlockState.LoadCacheEntry
        LoadCacheEntry(ValueNode __object, LocationIdentity __identity)
        {
            super(__object, __identity);
        }

        @Override
        public ReadEliminationBlockState.CacheEntry<LocationIdentity> duplicateWithObject(ValueNode __newObject)
        {
            return new ReadEliminationBlockState.LoadCacheEntry(__newObject, this.___identity);
        }

        @Override
        public boolean conflicts(LocationIdentity __other)
        {
            return this.___identity.equals(__other);
        }

        @Override
        public LocationIdentity getIdentity()
        {
            return this.___identity;
        }
    }

    ///
    // ReadEliminationBlockState.CacheEntry describing an Unsafe memory reference. The memory location and the
    // location identity are separate so both must be considered when looking for optimizable memory accesses.
    ///
    // @class ReadEliminationBlockState.UnsafeLoadCacheEntry
    static final class UnsafeLoadCacheEntry extends ReadEliminationBlockState.CacheEntry<ValueNode>
    {
        // @field
        private final LocationIdentity ___locationIdentity;

        // @cons ReadEliminationBlockState.UnsafeLoadCacheEntry
        UnsafeLoadCacheEntry(ValueNode __object, ValueNode __location, LocationIdentity __locationIdentity)
        {
            super(__object, __location);
            this.___locationIdentity = __locationIdentity;
        }

        @Override
        public ReadEliminationBlockState.CacheEntry<ValueNode> duplicateWithObject(ValueNode __newObject)
        {
            return new ReadEliminationBlockState.UnsafeLoadCacheEntry(__newObject, this.___identity, this.___locationIdentity);
        }

        @Override
        public boolean conflicts(LocationIdentity __other)
        {
            return this.___locationIdentity.equals(__other);
        }

        @Override
        public int hashCode()
        {
            return 31 * super.hashCode() + this.___locationIdentity.hashCode();
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (__obj instanceof ReadEliminationBlockState.UnsafeLoadCacheEntry)
            {
                ReadEliminationBlockState.UnsafeLoadCacheEntry __other = (ReadEliminationBlockState.UnsafeLoadCacheEntry) __obj;
                return super.equals(__other) && this.___locationIdentity.equals(__other.___locationIdentity);
            }
            return false;
        }

        @Override
        public LocationIdentity getIdentity()
        {
            return this.___locationIdentity;
        }
    }

    // @cons ReadEliminationBlockState
    public ReadEliminationBlockState()
    {
        super();
        this.___readCache = EconomicMap.create(Equivalence.DEFAULT);
    }

    // @cons ReadEliminationBlockState
    public ReadEliminationBlockState(ReadEliminationBlockState __other)
    {
        super();
        this.___readCache = EconomicMap.create(Equivalence.DEFAULT, __other.___readCache);
    }

    @Override
    public boolean equivalentTo(ReadEliminationBlockState __other)
    {
        return isSubMapOf(this.___readCache, __other.___readCache);
    }

    public void addCacheEntry(ReadEliminationBlockState.CacheEntry<?> __identifier, ValueNode __value)
    {
        this.___readCache.put(__identifier, __value);
    }

    public ValueNode getCacheEntry(ReadEliminationBlockState.CacheEntry<?> __identifier)
    {
        return this.___readCache.get(__identifier);
    }

    public void killReadCache()
    {
        this.___readCache.clear();
    }

    public void killReadCache(LocationIdentity __identity)
    {
        Iterator<ReadEliminationBlockState.CacheEntry<?>> __iterator = this.___readCache.getKeys().iterator();
        while (__iterator.hasNext())
        {
            ReadEliminationBlockState.CacheEntry<?> __entry = __iterator.next();
            if (__entry.conflicts(__identity))
            {
                __iterator.remove();
            }
        }
    }

    public EconomicMap<ReadEliminationBlockState.CacheEntry<?>, ValueNode> getReadCache()
    {
        return this.___readCache;
    }
}
