package giraaff.nodes.virtual;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.spi.ArrayOffsetProvider;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class VirtualObjectNode
public abstract class VirtualObjectNode extends ValueNode implements LIRLowerable, IterableNodeType
{
    public static final NodeClass<VirtualObjectNode> TYPE = NodeClass.create(VirtualObjectNode.class);

    protected boolean hasIdentity;
    private int objectId = -1;

    // @cons
    protected VirtualObjectNode(NodeClass<? extends VirtualObjectNode> c, ResolvedJavaType type, boolean hasIdentity)
    {
        super(c, StampFactory.objectNonNull(TypeReference.createExactTrusted(type)));
        this.hasIdentity = hasIdentity;
    }

    public final int getObjectId()
    {
        return objectId;
    }

    public final void resetObjectId()
    {
        this.objectId = -1;
    }

    public final void setObjectId(int objectId)
    {
        this.objectId = objectId;
    }

    @Override
    protected void afterClone(Node other)
    {
        super.afterClone(other);
        resetObjectId();
    }

    /**
     * The type of object described by this {@link VirtualObjectNode}. In case of arrays, this is
     * the array type (and not the component type).
     */
    public abstract ResolvedJavaType type();

    /**
     * The number of entries this virtual object has. Either the number of fields or the number of
     * array elements.
     */
    public abstract int entryCount();

    /**
     * Returns the name of the entry at the given index. Only used for debugging purposes.
     */
    public abstract String entryName(int i);

    /**
     * If the given index denotes an entry in this virtual object, the index of this entry is
     * returned. If no such entry can be found, this method returns -1.
     *
     * @param constantOffset offset, where the value is placed.
     * @param expectedEntryKind Specifies which type is expected at this offset (Is important when
     *            doing implicit casts, especially on big endian systems.
     */
    public abstract int entryIndexForOffset(ArrayOffsetProvider arrayOffsetProvider, long constantOffset, JavaKind expectedEntryKind);

    /**
     * Returns the {@link JavaKind} of the entry at the given index.
     */
    public abstract JavaKind entryKind(int index);

    /**
     * Returns an exact duplicate of this virtual object node, which has not been added to the graph yet.
     */
    public abstract VirtualObjectNode duplicate();

    /**
     * Specifies whether this virtual object has an object identity. If not, then the result of a
     * comparison of two virtual objects is determined by comparing their contents.
     */
    public boolean hasIdentity()
    {
        return hasIdentity;
    }

    public void setIdentity(boolean identity)
    {
        this.hasIdentity = identity;
    }

    /**
     * Returns a node that can be used to materialize this virtual object. If this returns an
     * {@link AllocatedObjectNode} then this node will be attached to a {@link CommitAllocationNode}
     * , otherwise the node will just be added to the graph.
     */
    public abstract ValueNode getMaterializedRepresentation(FixedNode fixed, ValueNode[] entries, LockState locks);

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        // nothing to do...
    }
}
