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
    final EconomicMap<PEReadEliminationBlockState.ReadCacheEntry, ValueNode> ___readCache;

    // @class PEReadEliminationBlockState.ReadCacheEntry
    static final class ReadCacheEntry
    {
        // @field
        public final LocationIdentity ___identity;
        // @field
        public final ValueNode ___object;
        // @field
        public final int ___index;
        // @field
        public final JavaKind ___kind;

        // This flag does not affect hashCode or equals implementations.
        // @field
        public final boolean ___overflowAccess;

        // @cons PEReadEliminationBlockState.ReadCacheEntry
        ReadCacheEntry(LocationIdentity __identity, ValueNode __object, int __index, JavaKind __kind, boolean __overflowAccess)
        {
            super();
            this.___identity = __identity;
            this.___object = __object;
            this.___index = __index;
            this.___kind = __kind;
            this.___overflowAccess = __overflowAccess;
        }

        @Override
        public int hashCode()
        {
            int __result = 31 + ((this.___identity == null) ? 0 : this.___identity.hashCode());
            __result = 31 * __result + ((this.___object == null) ? 0 : System.identityHashCode(this.___object));
            __result = 31 * __result + this.___kind.ordinal();
            return __result * 31 + this.___index;
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (!(__obj instanceof PEReadEliminationBlockState.ReadCacheEntry))
            {
                return false;
            }
            PEReadEliminationBlockState.ReadCacheEntry __other = (PEReadEliminationBlockState.ReadCacheEntry) __obj;
            return this.___identity.equals(__other.___identity) && this.___object == __other.___object && this.___index == __other.___index && this.___kind == __other.___kind;
        }
    }

    // @cons PEReadEliminationBlockState
    public PEReadEliminationBlockState()
    {
        super();
        this.___readCache = EconomicMap.create(Equivalence.DEFAULT);
    }

    // @cons PEReadEliminationBlockState
    public PEReadEliminationBlockState(PEReadEliminationBlockState __other)
    {
        super(__other);
        this.___readCache = EconomicMap.create(Equivalence.DEFAULT, __other.___readCache);
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
                    this.___readCache.put(new PEReadEliminationBlockState.ReadCacheEntry(new FieldLocationIdentity(__instance.field(__i)), __representation, -1, __declaredKind, false), __values.get(__i));
                }
            }
        }
    }

    @Override
    public boolean equivalentTo(PEReadEliminationBlockState __other)
    {
        if (!isSubMapOf(this.___readCache, __other.___readCache))
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
        this.___readCache.put(new PEReadEliminationBlockState.ReadCacheEntry(__identity, __cacheObject, __index, __kind, __overflowAccess), __value);
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
        ValueNode __cacheValue = this.___readCache.get(new PEReadEliminationBlockState.ReadCacheEntry(__identity, __cacheObject, __index, __kind, false));
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
        this.___readCache.clear();
    }

    public void killReadCache(LocationIdentity __identity, int __index)
    {
        Iterator<PEReadEliminationBlockState.ReadCacheEntry> __iter = this.___readCache.getKeys().iterator();
        while (__iter.hasNext())
        {
            PEReadEliminationBlockState.ReadCacheEntry __entry = __iter.next();
            if (__entry.___identity.equals(__identity) && (__index == -1 || __entry.___index == -1 || __index == __entry.___index || __entry.___overflowAccess))
            {
                __iter.remove();
            }
        }
    }

    public EconomicMap<PEReadEliminationBlockState.ReadCacheEntry, ValueNode> getReadCache()
    {
        return this.___readCache;
    }
}
