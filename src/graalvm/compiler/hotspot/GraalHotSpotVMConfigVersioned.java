package graalvm.compiler.hotspot;

import jdk.vm.ci.hotspot.HotSpotVMConfigAccess;
import jdk.vm.ci.hotspot.HotSpotVMConfigStore;

/**
 * This is a source with different versions for various JDKs. When modifying/adding a field in this
 * class accessed from outside this class, be sure to update the field appropriately in all source
 * files named {@code GraalHotSpotVMConfigVersioned.java}.
 *
 * Fields are grouped according to the most recent JBS issue showing why they are versioned.
 *
 * JDK Version: 9+
 */
final class GraalHotSpotVMConfigVersioned extends HotSpotVMConfigAccess {

    GraalHotSpotVMConfigVersioned(HotSpotVMConfigStore store) {
        super(store);
    }

    // JDK-8073583
    final boolean useCRC32CIntrinsics = getFlag("UseCRC32CIntrinsics", Boolean.class);

    // JDK-8075171
    final boolean inlineNotify = getFlag("InlineNotify", Boolean.class);

    // JDK-8046936
    final int javaThreadReservedStackActivationOffset = getFieldOffset("JavaThread::_reserved_stack_activation", Integer.class, "address");
    final int methodFlagsOffset = getFieldOffset("Method::_flags", Integer.class, "u2");
    final long throwDelayedStackOverflowErrorEntry = getFieldValue("StubRoutines::_throw_delayed_StackOverflowError_entry", Long.class, "address");
    final long enableStackReservedZoneAddress = getAddress("SharedRuntime::enable_stack_reserved_zone");

    // JDK-8135085
    final int methodIntrinsicIdOffset = getFieldOffset("Method::_intrinsic_id", Integer.class, "u2");

    // JDK-8151956
    final int methodCodeOffset = getFieldOffset("Method::_code", Integer.class, "CompiledMethod*");

    // JDK-8059606
    final int invocationCounterIncrement = getConstant("InvocationCounter::count_increment", Integer.class);
    final int invocationCounterShift = getConstant("InvocationCounter::count_shift", Integer.class);

    // JDK-8134994
    final int dirtyCardQueueBufferOffset = getConstant("dirtyCardQueueBufferOffset", Integer.class);
    final int dirtyCardQueueIndexOffset = getConstant("dirtyCardQueueIndexOffset", Integer.class);
    final int satbMarkQueueBufferOffset = getConstant("satbMarkQueueBufferOffset", Integer.class);
    final int satbMarkQueueIndexOffset = getConstant("satbMarkQueueIndexOffset", Integer.class);
    final int satbMarkQueueActiveOffset = getConstant("satbMarkQueueActiveOffset", Integer.class);

    // JDK-8195142
    final byte dirtyCardValue = getConstant("CardTableModRefBS::dirty_card", Byte.class);
    final byte g1YoungCardValue = getConstant("G1SATBCardTableModRefBS::g1_young_gen", Byte.class);

    // JDK-8201318
    final int javaThreadDirtyCardQueueOffset = getFieldOffset("JavaThread::_dirty_card_queue", Integer.class, "DirtyCardQueue");
    final int javaThreadSatbMarkQueueOffset = getFieldOffset("JavaThread::_satb_mark_queue", Integer.class);
    final int g1CardQueueIndexOffset = javaThreadDirtyCardQueueOffset + dirtyCardQueueIndexOffset;
    final int g1CardQueueBufferOffset = javaThreadDirtyCardQueueOffset + dirtyCardQueueBufferOffset;
    final int g1SATBQueueMarkingOffset = javaThreadSatbMarkQueueOffset + satbMarkQueueActiveOffset;
    final int g1SATBQueueIndexOffset = javaThreadSatbMarkQueueOffset + satbMarkQueueIndexOffset;
    final int g1SATBQueueBufferOffset = javaThreadSatbMarkQueueOffset + satbMarkQueueBufferOffset;

    // JDK-8033552
    final long heapTopAddress = getFieldValue("CompilerToVM::Data::_heap_top_addr", Long.class, "HeapWord* volatile*");

    // JDK-8015774
    final long codeCacheLowBound = getFieldValue("CodeCache::_low_bound", Long.class, "address");
    final long codeCacheHighBound = getFieldValue("CodeCache::_high_bound", Long.class, "address");
}
