package giraaff.nodes.memory;

///
// Encapsulates properties of a node describing how it accesses the heap.
///
// @iface HeapAccess
public interface HeapAccess
{
    ///
    // The types of (write/read) barriers attached to stores.
    ///
    // @enum HeapAccess.BarrierType
    enum BarrierType
    {
        ///
        // Primitive stores which do not necessitate barriers.
        ///
        NONE,
        ///
        // Array object stores which necessitate precise barriers.
        ///
        PRECISE,
        ///
        // Field object stores which necessitate imprecise barriers.
        ///
        IMPRECISE
    }

    ///
    // Gets the write barrier type for that particular access.
    ///
    HeapAccess.BarrierType getBarrierType();
}
