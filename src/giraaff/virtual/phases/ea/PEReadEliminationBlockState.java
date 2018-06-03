package giraaff.virtual.phases.ea;

import java.util.Iterator;
import java.util.List;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.nodes.FieldLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.virtual.AllocatedObjectNode;
import giraaff.nodes.virtual.VirtualInstanceNode;
import giraaff.nodes.virtual.VirtualObjectNode;

// @class PEReadEliminationBlockState
public final class PEReadEliminationBlockState extends PartialEscapeBlockState<PEReadEliminationBlockState>
{
    // @field
    final EconomicMap<ReadCacheEntry, ValueNode> readCache;

    // @class PEReadEliminationBlockState.ReadCacheEntry
    static final class ReadCacheEntry
    {
        // @field
        public final LocationIdentity identity;
        // @field
        public final ValueNode object;
        // @field
        public final int index;
        // @field
        public final JavaKind kind;

        // This flag does not affect hashCode or equals implementations.
        // @field
        public final boolean overflowAccess;

        // @cons
        ReadCacheEntry(LocationIdentity __identity, ValueNode __object, int __index, JavaKind __kind, boolean __overflowAccess)
        {
            super();
            this.identity = __identity;
            this.object = __object;
            this.index = __index;
            this.kind = __kind;
            this.overflowAccess = __overflowAccess;
        }

        @Override
        public int hashCode()
        {
            int __result = 31 + ((identity == null) ? 0 : identity.hashCode());
            __result = 31 * __result + ((object == null) ? 0 : System.identityHashCode(object));
            __result = 31 * __result + kind.ordinal();
            return __result * 31 + index;
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (!(__obj instanceof ReadCacheEntry))
            {
                return false;
            }
            ReadCacheEntry __other = (ReadCacheEntry) __obj;
            return identity.equals(__other.identity) && object == __other.object && index == __other.index && kind == __other.kind;
        }
    }

    // @cons
    public PEReadEliminationBlockState()
    {
        super();
        readCache = EconomicMap.create(Equivalence.DEFAULT);
    }

    // @cons
    public PEReadEliminationBlockState(PEReadEliminationBlockState __other)
    {
        super(__other);
        readCache = EconomicMap.create(Equivalence.DEFAULT, __other.readCache);
    }

    private static JavaKind stampToJavaKind(Stamp __stamp)
    {
        if (__stamp instanceof IntegerStamp)
        {
            switch (((IntegerStamp) __stamp).getBits())
            {
                case 1:
                    return JavaKind.Boolean;
                case 8:
                    return JavaKind.Byte;
                case 16:
                    return ((IntegerStamp) __stamp).isPositive() ? JavaKind.Char : JavaKind.Short;
                case 32:
                    return JavaKind.Int;
                case 64:
                    return JavaKind.Long;
                default:
                    throw new IllegalArgumentException("unexpected IntegerStamp " + __stamp);
            }
        }
        else
        {
            return __stamp.getStackKind();
        }
    }

    @Override
    protected void objectMaterialized(VirtualObjectNode __virtual, AllocatedObjectNode __representation, List<ValueNode> __values)
    {
        if (__virtual instanceof VirtualInstanceNode)
        {
            VirtualInstanceNode __instance = (VirtualInstanceNode) __virtual;
            for (int __i = 0; __i < __instance.entryCount(); __i++)
            {
                JavaKind __declaredKind = __instance.field(__i).getJavaKind();
                if (__declaredKind == stampToJavaKind(__values.get(__i).stamp(NodeView.DEFAULT)))
                {
                    // We won't cache unaligned field writes upon instantiation unless we add
                    // support for non-array objects in PEReadEliminationClosure.processUnsafeLoad.
                    readCache.put(new ReadCacheEntry(new FieldLocationIdentity(__instance.field(__i)), __representation, -1, __declaredKind, false), __values.get(__i));
                }
            }
        }
    }

    @Override
    public boolean equivalentTo(PEReadEliminationBlockState __other)
    {
        if (!isSubMapOf(readCache, __other.readCache))
        {
            return false;
        }
        return super.equivalentTo(__other);
    }

    public void addReadCache(ValueNode __object, LocationIdentity __identity, int __index, JavaKind __kind, boolean __overflowAccess, ValueNode __value, PartialEscapeClosure<?> __closure)
    {
        ValueNode __cacheObject;
        ObjectState __obj = __closure.getObjectState(this, __object);
        if (__obj != null)
        {
            __cacheObject = __obj.getMaterializedValue();
        }
        else
        {
            __cacheObject = __object;
        }
        readCache.put(new ReadCacheEntry(__identity, __cacheObject, __index, __kind, __overflowAccess), __value);
    }

    public ValueNode getReadCache(ValueNode __object, LocationIdentity __identity, int __index, JavaKind __kind, PartialEscapeClosure<?> __closure)
    {
        ValueNode __cacheObject;
        ObjectState __obj = __closure.getObjectState(this, __object);
        if (__obj != null)
        {
            __cacheObject = __obj.getMaterializedValue();
        }
        else
        {
            __cacheObject = __object;
        }
        ValueNode __cacheValue = readCache.get(new ReadCacheEntry(__identity, __cacheObject, __index, __kind, false));
        __obj = __closure.getObjectState(this, __cacheValue);
        if (__obj != null)
        {
            __cacheValue = __obj.getMaterializedValue();
        }
        else
        {
            // assert !scalarAliases.containsKey(cacheValue);
            __cacheValue = __closure.getScalarAlias(__cacheValue);
        }
        return __cacheValue;
    }

    public void killReadCache()
    {
        readCache.clear();
    }

    public void killReadCache(LocationIdentity __identity, int __index)
    {
        Iterator<ReadCacheEntry> __iter = readCache.getKeys().iterator();
        while (__iter.hasNext())
        {
            ReadCacheEntry __entry = __iter.next();
            if (__entry.identity.equals(__identity) && (__index == -1 || __entry.index == -1 || __index == __entry.index || __entry.overflowAccess))
            {
                __iter.remove();
            }
        }
    }

    public EconomicMap<ReadCacheEntry, ValueNode> getReadCache()
    {
        return readCache;
    }
}
