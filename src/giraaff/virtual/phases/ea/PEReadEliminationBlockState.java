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
import giraaff.options.OptionValues;

// @class PEReadEliminationBlockState
public final class PEReadEliminationBlockState extends PartialEscapeBlockState<PEReadEliminationBlockState>
{
    final EconomicMap<ReadCacheEntry, ValueNode> readCache;

    // @class PEReadEliminationBlockState.ReadCacheEntry
    static final class ReadCacheEntry
    {
        public final LocationIdentity identity;
        public final ValueNode object;
        public final int index;
        public final JavaKind kind;

        // This flag does not affect hashCode or equals implementations.
        public final boolean overflowAccess;

        // @cons
        ReadCacheEntry(LocationIdentity identity, ValueNode object, int index, JavaKind kind, boolean overflowAccess)
        {
            super();
            this.identity = identity;
            this.object = object;
            this.index = index;
            this.kind = kind;
            this.overflowAccess = overflowAccess;
        }

        @Override
        public int hashCode()
        {
            int result = 31 + ((identity == null) ? 0 : identity.hashCode());
            result = 31 * result + ((object == null) ? 0 : System.identityHashCode(object));
            result = 31 * result + kind.ordinal();
            return result * 31 + index;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof ReadCacheEntry))
            {
                return false;
            }
            ReadCacheEntry other = (ReadCacheEntry) obj;
            return identity.equals(other.identity) && object == other.object && index == other.index && kind == other.kind;
        }
    }

    // @cons
    public PEReadEliminationBlockState(OptionValues options)
    {
        super(options);
        readCache = EconomicMap.create(Equivalence.DEFAULT);
    }

    // @cons
    public PEReadEliminationBlockState(PEReadEliminationBlockState other)
    {
        super(other);
        readCache = EconomicMap.create(Equivalence.DEFAULT, other.readCache);
    }

    private static JavaKind stampToJavaKind(Stamp stamp)
    {
        if (stamp instanceof IntegerStamp)
        {
            switch (((IntegerStamp) stamp).getBits())
            {
                case 1:
                    return JavaKind.Boolean;
                case 8:
                    return JavaKind.Byte;
                case 16:
                    return ((IntegerStamp) stamp).isPositive() ? JavaKind.Char : JavaKind.Short;
                case 32:
                    return JavaKind.Int;
                case 64:
                    return JavaKind.Long;
                default:
                    throw new IllegalArgumentException("unexpected IntegerStamp " + stamp);
            }
        }
        else
        {
            return stamp.getStackKind();
        }
    }

    @Override
    protected void objectMaterialized(VirtualObjectNode virtual, AllocatedObjectNode representation, List<ValueNode> values)
    {
        if (virtual instanceof VirtualInstanceNode)
        {
            VirtualInstanceNode instance = (VirtualInstanceNode) virtual;
            for (int i = 0; i < instance.entryCount(); i++)
            {
                JavaKind declaredKind = instance.field(i).getJavaKind();
                if (declaredKind == stampToJavaKind(values.get(i).stamp(NodeView.DEFAULT)))
                {
                    // We won't cache unaligned field writes upon instantiation unless we add
                    // support for non-array objects in PEReadEliminationClosure.processUnsafeLoad.
                    readCache.put(new ReadCacheEntry(new FieldLocationIdentity(instance.field(i)), representation, -1, declaredKind, false), values.get(i));
                }
            }
        }
    }

    @Override
    public boolean equivalentTo(PEReadEliminationBlockState other)
    {
        if (!isSubMapOf(readCache, other.readCache))
        {
            return false;
        }
        return super.equivalentTo(other);
    }

    public void addReadCache(ValueNode object, LocationIdentity identity, int index, JavaKind kind, boolean overflowAccess, ValueNode value, PartialEscapeClosure<?> closure)
    {
        ValueNode cacheObject;
        ObjectState obj = closure.getObjectState(this, object);
        if (obj != null)
        {
            cacheObject = obj.getMaterializedValue();
        }
        else
        {
            cacheObject = object;
        }
        readCache.put(new ReadCacheEntry(identity, cacheObject, index, kind, overflowAccess), value);
    }

    public ValueNode getReadCache(ValueNode object, LocationIdentity identity, int index, JavaKind kind, PartialEscapeClosure<?> closure)
    {
        ValueNode cacheObject;
        ObjectState obj = closure.getObjectState(this, object);
        if (obj != null)
        {
            cacheObject = obj.getMaterializedValue();
        }
        else
        {
            cacheObject = object;
        }
        ValueNode cacheValue = readCache.get(new ReadCacheEntry(identity, cacheObject, index, kind, false));
        obj = closure.getObjectState(this, cacheValue);
        if (obj != null)
        {
            cacheValue = obj.getMaterializedValue();
        }
        else
        {
            // assert !scalarAliases.containsKey(cacheValue);
            cacheValue = closure.getScalarAlias(cacheValue);
        }
        return cacheValue;
    }

    public void killReadCache()
    {
        readCache.clear();
    }

    public void killReadCache(LocationIdentity identity, int index)
    {
        Iterator<ReadCacheEntry> iter = readCache.getKeys().iterator();
        while (iter.hasNext())
        {
            ReadCacheEntry entry = iter.next();
            if (entry.identity.equals(identity) && (index == -1 || entry.index == -1 || index == entry.index || entry.overflowAccess))
            {
                iter.remove();
            }
        }
    }

    public EconomicMap<ReadCacheEntry, ValueNode> getReadCache()
    {
        return readCache;
    }
}
