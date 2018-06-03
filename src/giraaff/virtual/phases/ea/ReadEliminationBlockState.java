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
    final EconomicMap<CacheEntry<?>, ValueNode> ___readCache;

    // @class ReadEliminationBlockState.CacheEntry
    abstract static class CacheEntry<T>
    {
        // @field
        public final ValueNode ___object;
        // @field
        public final T ___identity;

        // @cons
        CacheEntry(ValueNode __object, T __identity)
        {
            super();
            this.___object = __object;
            this.___identity = __identity;
        }

        public abstract CacheEntry<T> duplicateWithObject(ValueNode __newObject);

        @Override
        public int hashCode()
        {
            int __result = 31 + ((this.___identity == null) ? 0 : this.___identity.hashCode());
            return 31 * __result + ((this.___object == null) ? 0 : this.___object.hashCode());
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (!(__obj instanceof CacheEntry<?>))
            {
                return false;
            }
            CacheEntry<?> __other = (CacheEntry<?>) __obj;
            return this.___identity.equals(__other.___identity) && this.___object == __other.___object;
        }

        public abstract boolean conflicts(LocationIdentity __other);

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
            return new LoadCacheEntry(__newObject, this.___identity);
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
    // CacheEntry describing an Unsafe memory reference. The memory location and the location
    // identity are separate so both must be considered when looking for optimizable memory accesses.
    ///
    // @class ReadEliminationBlockState.UnsafeLoadCacheEntry
    static final class UnsafeLoadCacheEntry extends CacheEntry<ValueNode>
    {
        // @field
        private final LocationIdentity ___locationIdentity;

        // @cons
        UnsafeLoadCacheEntry(ValueNode __object, ValueNode __location, LocationIdentity __locationIdentity)
        {
            super(__object, __location);
            this.___locationIdentity = __locationIdentity;
        }

        @Override
        public CacheEntry<ValueNode> duplicateWithObject(ValueNode __newObject)
        {
            return new UnsafeLoadCacheEntry(__newObject, this.___identity, this.___locationIdentity);
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
            if (__obj instanceof UnsafeLoadCacheEntry)
            {
                UnsafeLoadCacheEntry __other = (UnsafeLoadCacheEntry) __obj;
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

    // @cons
    public ReadEliminationBlockState()
    {
        super();
        this.___readCache = EconomicMap.create(Equivalence.DEFAULT);
    }

    // @cons
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

    public void addCacheEntry(CacheEntry<?> __identifier, ValueNode __value)
    {
        this.___readCache.put(__identifier, __value);
    }

    public ValueNode getCacheEntry(CacheEntry<?> __identifier)
    {
        return this.___readCache.get(__identifier);
    }

    public void killReadCache()
    {
        this.___readCache.clear();
    }

    public void killReadCache(LocationIdentity __identity)
    {
        Iterator<CacheEntry<?>> __iterator = this.___readCache.getKeys().iterator();
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
        return this.___readCache;
    }
}
