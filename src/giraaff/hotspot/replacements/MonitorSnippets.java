package giraaff.hotspot.replacements;

import java.util.List;

import jdk.vm.ci.code.MemoryBarriers;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.meta.HotSpotRegistersProvider;
import giraaff.hotspot.nodes.AcquiredCASLockNode;
import giraaff.hotspot.nodes.BeginLockScopeNode;
import giraaff.hotspot.nodes.CurrentLockNode;
import giraaff.hotspot.nodes.EndLockScopeNode;
import giraaff.hotspot.nodes.FastAcquireBiasedLockNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.MembarNode;
import giraaff.nodes.java.MonitorExitNode;
import giraaff.nodes.java.RawMonitorEnterNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.word.Word;

///
// Snippets used for implementing the monitorenter and monitorexit instructions.
//
// The locking algorithm used is described in the paper
// <a href="http://dl.acm.org/citation.cfm?id=1167515.1167496"> Eliminating synchronization-related
// atomic operations with biased locking and bulk rebiasing</a> by Kenneth Russell and David Detlefs.
//
// Comment below is reproduced from {@code markOop.hpp} for convenience:
//
// <pre>
//  Bit-format of an object header (most significant first, big endian layout below):
//  32 bits:
//  --------
//             hash:25 ------------>| age:4    biased_lock:1 lock:2 (normal object)
//             JavaThread*:23 epoch:2 age:4    biased_lock:1 lock:2 (biased object)
//             size:32 ------------------------------------------>| (CMS free block)
//             PromotedObject*:29 ---------->| promo_bits:3 ----->| (CMS promoted object)
//
//  64 bits:
//  --------
//  unused:25 hash:31 -->| unused:1   age:4    biased_lock:1 lock:2 (normal object)
//  JavaThread*:54 epoch:2 unused:1   age:4    biased_lock:1 lock:2 (biased object)
//  PromotedObject*:61 --------------------->| promo_bits:3 ----->| (CMS promoted object)
//  size:64 ----------------------------------------------------->| (CMS free block)
//
//  unused:25 hash:31 -->| cms_free:1 age:4    biased_lock:1 lock:2 (COOPs && normal object)
//  JavaThread*:54 epoch:2 cms_free:1 age:4    biased_lock:1 lock:2 (COOPs && biased object)
//  narrowOop:32 unused:24 cms_free:1 unused:4 promo_bits:3 ----->| (COOPs && CMS promoted object)
//  unused:21 size:35 -->| cms_free:1 unused:7 ------------------>| (COOPs && CMS free block)
//
//  - hash contains the identity hash value: largest value is
//    31 bits, see os::random().  Also, 64-bit vm's require
//    a hash value no bigger than 32 bits because they will not
//    properly generate a mask larger than that: see library_call.cpp
//    and c1_CodePatterns_sparc.cpp.
//
//  - the biased lock pattern is used to bias a lock toward a given
//    thread. When this pattern is set in the low three bits, the lock
//    is either biased toward a given thread or "anonymously" biased,
//    indicating that it is possible for it to be biased. When the
//    lock is biased toward a given thread, locking and unlocking can
//    be performed by that thread without using atomic operations.
//    When a lock's bias is revoked, it reverts back to the normal
//    locking scheme described below.
//
//    Note that we are overloading the meaning of the "unlocked" state
//    of the header. Because we steal a bit from the age we can
//    guarantee that the bias pattern will never be seen for a truly
//    unlocked object.
//
//    Note also that the biased state contains the age bits normally
//    contained in the object header. Large increases in scavenge
//    times were seen when these bits were absent and an arbitrary age
//    assigned to all biased objects, because they tended to consume a
//    significant fraction of the eden semispaces and were not
//    promoted promptly, causing an increase in the amount of copying
//    performed. The runtime system aligns all JavaThread* pointers to
//    a very large value (currently 128 bytes (32bVM) or 256 bytes (64bVM))
//    to make room for the age bits & the epoch bits (used in support of
//    biased locking), and for the CMS "freeness" bit in the 64bVM (+COOPs).
//
//    [JavaThread* | epoch | age | 1 | 01]       lock is biased toward given thread
//    [0           | epoch | age | 1 | 01]       lock is anonymously biased
//
//  - the two lock bits are used to describe three states: locked/unlocked and monitor.
//
//    [ptr             | 00]  locked             ptr points to real header on stack
//    [header      | 0 | 01]  unlocked           regular object header
//    [ptr             | 10]  monitor            inflated lock (header is wapped out)
//    [ptr             | 11]  marked             used by markSweep to mark an object
//                                               not valid at any other time
//
//    We assume that stack/thread pointers have the lowest two bits cleared.
// </pre>
//
// Note that {@code Thread::allocate} enforces {@code JavaThread} objects to be aligned
// appropriately to comply with the layouts above.
///
// @class MonitorSnippets
public final class MonitorSnippets implements Snippets
{
    // @cons
    private MonitorSnippets()
    {
        super();
    }

    @Snippet
    public static void monitorenter(Object __object, KlassPointer __hub, @ConstantParameter int __lockDepth, @ConstantParameter Register __threadRegister, @ConstantParameter Register __stackPointerRegister)
    {
        // load the mark word - this includes a null-check on object
        final Word __mark = HotSpotReplacementsUtil.loadWordFromObject(__object, HotSpotRuntime.markOffset);

        final Word __lock = BeginLockScopeNode.beginLockScope(__lockDepth);

        Pointer __objectPointer = Word.objectToTrackedPointer(__object);

        if (HotSpotRuntime.useBiasedLocking)
        {
            if (tryEnterBiased(__object, __hub, __lock, __mark, __threadRegister))
            {
                return;
            }
            // not biased, fall-through
        }
        if (inlineFastLockSupported() && BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __mark.and(HotSpotRuntime.monitorMask).notEqual(0)))
        {
            // inflated case
            if (tryEnterInflated(__object, __lock, __mark, __threadRegister))
            {
                return;
            }
        }
        else
        {
            // create the unlocked mark word pattern
            Word __unlockedMark = __mark.or(HotSpotRuntime.unlockedMask);

            // copy this unlocked mark word into the lock slot on the stack
            __lock.writeWord(HotSpotRuntime.lockDisplacedMarkOffset, __unlockedMark, HotSpotReplacementsUtil.DISPLACED_MARK_WORD_LOCATION);

            // make sure previous store does not float below compareAndSwap
            MembarNode.memoryBarrier(MemoryBarriers.STORE_STORE);

            // Test if the object's mark word is unlocked, and if so, store the (address of) the lock slot into the object's mark word.
            Word __currentMark = __objectPointer.compareAndSwapWord(HotSpotRuntime.markOffset, __unlockedMark, __lock, HotSpotReplacementsUtil.MARK_WORD_LOCATION);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __currentMark.equal(__unlockedMark)))
            {
                AcquiredCASLockNode.mark(__object);
                return;
            }
            else
            {
                // The mark word in the object header was not the same.
                // Either the object is locked by another thread or is already locked
                // by the current thread. The latter is true if the mark word
                // is a stack pointer into the current thread's stack, i.e.:
                //
                // 1) (currentMark & aligned_mask) == 0
                // 2) rsp <= currentMark
                // 3) currentMark <= rsp + page_size
                //
                // These 3 tests can be done by evaluating the following expression:
                //
                // (currentMark - rsp) & (aligned_mask - page_size)
                //
                // assuming both the stack pointer and page_size have their least
                // significant 2 bits cleared and page_size is a power of 2
                final Word __alignedMask = WordFactory.unsigned(HotSpotReplacementsUtil.wordSize() - 1);
                final Word __stackPointer = HotSpotReplacementsUtil.registerAsWord(__stackPointerRegister).add(HotSpotRuntime.stackBias);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __currentMark.subtract(__stackPointer).and(__alignedMask.subtract(HotSpotReplacementsUtil.pageSize())).equal(0)))
                {
                    // recursively locked => write 0 to the lock slot
                    __lock.writeWord(HotSpotRuntime.lockDisplacedMarkOffset, WordFactory.zero(), HotSpotReplacementsUtil.DISPLACED_MARK_WORD_LOCATION);
                    return;
                }
            }
        }
        // slow-path runtime-call
        monitorenterStubC(MONITORENTER, __object, __lock);
    }

    private static boolean tryEnterBiased(Object __object, KlassPointer __hub, Word __lock, Word __mark, Register __threadRegister)
    {
        // See whether the lock is currently biased toward our thread and whether the epoch is still valid.
        // Note that the runtime guarantees sufficient alignment of JavaThread pointers to allow age to be placed into low bits.
        final Word __biasableLockBits = __mark.and(HotSpotRuntime.biasedLockMaskInPlace);

        // Check whether the bias pattern is present in the object's mark word and the bias owner and the epoch are both still current.
        final Word __prototypeMarkWord = __hub.readWord(HotSpotRuntime.prototypeMarkWordOffset, HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION);
        final Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        final Word __tmp = __prototypeMarkWord.or(__thread).xor(__mark).and(~HotSpotRuntime.ageMaskInPlace);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __tmp.equal(0)))
        {
            // object is already biased to current thread -> done
            FastAcquireBiasedLockNode.mark(__object);
            return true;
        }

        // now check to see whether biasing is enabled for this object
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __biasableLockBits.equal(WordFactory.unsigned(HotSpotRuntime.biasedLockPattern))))
        {
            Pointer __objectPointer = Word.objectToTrackedPointer(__object);
            // At this point we know that the mark word has the bias pattern and that we are not the bias owner in the
            // current epoch. We need to figure out more details about the state of the mark word in order to know what
            // operations can be legally performed on the object's mark word.

            // If the low three bits in the xor result aren't clear, that means the prototype header is no longer biasable
            // and we have to revoke the bias on this object.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __tmp.and(HotSpotRuntime.biasedLockMaskInPlace).equal(0)))
            {
                // Biasing is still enabled for object's type. See whether the epoch of the current bias is still valid,
                // meaning that the epoch bits of the mark word are equal to the epoch bits of the prototype mark word.
                // (Note that the prototype mark word's epoch bits only change at a safepoint.) If not, attempt to rebias
                // the object toward the current thread. Note that we must be absolutely sure that the current epoch is
                // invalid in order to do this, because otherwise the manipulations it performs on the mark word are illegal.
                if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __tmp.and(HotSpotRuntime.epochMaskInPlace).equal(0)))
                {
                    // The epoch of the current bias is still valid but we know nothing about the owner, it might be
                    // set or it might be clear. Try to acquire the bias of the object using an atomic operation. If
                    // this fails we will go in to the runtime to revoke the object's bias. Note that we first construct
                    // the presumed unbiased header so we don't accidentally blow away another thread's valid bias.
                    Word __unbiasedMark = __mark.and(HotSpotRuntime.biasedLockMaskInPlace | HotSpotRuntime.ageMaskInPlace | HotSpotRuntime.epochMaskInPlace);
                    Word __biasedMark = __unbiasedMark.or(__thread);
                    if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_FAST_PATH_PROBABILITY, __objectPointer.logicCompareAndSwapWord(HotSpotRuntime.markOffset, __unbiasedMark, __biasedMark, HotSpotReplacementsUtil.MARK_WORD_LOCATION)))
                    {
                        // object is now biased to current thread -> done
                        return true;
                    }
                    // If the biasing toward our thread failed, this means that another thread owns the bias
                    // and we need to revoke that bias. The revocation will occur in the interpreter runtime.
                }
                else
                {
                    // At this point we know the epoch has expired, meaning that the current bias owner, if any, is
                    // actually invalid. Under these circumstances *only*, are we allowed to use the current mark word
                    // value as the comparison value when doing the CAS to acquire the bias in the current epoch. In
                    // other words, we allow transfer of the bias from one thread to another directly in this situation.
                    Word __biasedMark = __prototypeMarkWord.or(__thread);
                    if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_FAST_PATH_PROBABILITY, __objectPointer.logicCompareAndSwapWord(HotSpotRuntime.markOffset, __mark, __biasedMark, HotSpotReplacementsUtil.MARK_WORD_LOCATION)))
                    {
                        // object is now biased to current thread -> done
                        return true;
                    }
                    // If the biasing toward our thread failed, then another thread succeeded in biasing it toward itself
                    // and we need to revoke that bias. The revocation will occur in the runtime in the slow case.
                }
                // slow-path runtime-call
                monitorenterStubC(MONITORENTER, __object, __lock);
                return true;
            }
            else
            {
                // The prototype mark word doesn't have the bias bit set any more, indicating that objects of this
                // data type are not supposed to be biased any more. We are going to try to reset the mark of this
                // object to the prototype value and fall through to the CAS-based locking scheme. Note that if our
                // CAS fails, it means that another thread raced us for the privilege of revoking the bias of this
                // particular object, so it's okay to continue in the normal locking code.
                Word __result = __objectPointer.compareAndSwapWord(HotSpotRuntime.markOffset, __mark, __prototypeMarkWord, HotSpotReplacementsUtil.MARK_WORD_LOCATION);

                // Fall through to the normal CAS-based lock, because no matter what the result of the above CAS,
                // some thread must have succeeded in removing the bias bit from the object's header.
                return false;
            }
        }
        else
        {
            // biasing not enabled -> fall through to lightweight locking
            return false;
        }
    }

    private static boolean inlineFastLockSupported()
    {
        return GraalOptions.simpleFastInflatedLocking && HotSpotRuntime.monitorMask >= 0 && HotSpotRuntime.objectMonitorOwnerOffset >= 0;
    }

    private static boolean tryEnterInflated(Object __object, Word __lock, Word __mark, Register __threadRegister)
    {
        // write non-zero value to lock slot
        __lock.writeWord(HotSpotRuntime.lockDisplacedMarkOffset, __lock, HotSpotReplacementsUtil.DISPLACED_MARK_WORD_LOCATION);
        // mark is a pointer to the ObjectMonitor + monitorMask
        Word __monitor = __mark.subtract(HotSpotRuntime.monitorMask);
        int __ownerOffset = HotSpotRuntime.objectMonitorOwnerOffset;
        Word __owner = __monitor.readWord(__ownerOffset, HotSpotReplacementsUtil.OBJECT_MONITOR_OWNER_LOCATION);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __owner.equal(0)))
        {
            // it appears unlocked (owner == 0)
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __monitor.logicCompareAndSwapWord(__ownerOffset, __owner, HotSpotReplacementsUtil.registerAsWord(__threadRegister), HotSpotReplacementsUtil.OBJECT_MONITOR_OWNER_LOCATION)))
            {
                // success
                return true;
            }
        }
        return false;
    }

    ///
    // Calls straight out to the monitorenter stub.
    ///
    @Snippet
    public static void monitorenterStub(Object __object, @ConstantParameter int __lockDepth)
    {
        if (__object == null)
        {
            DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.NullCheckException);
        }
        // BeginLockScope nodes do not read from object, so a use of object cannot float about the null check above.
        final Word __lock = BeginLockScopeNode.beginLockScope(__lockDepth);
        monitorenterStubC(MONITORENTER, __object, __lock);
    }

    @Snippet
    public static void monitorexit(Object __object, @ConstantParameter int __lockDepth, @ConstantParameter Register __threadRegister)
    {
        final Word __mark = HotSpotReplacementsUtil.loadWordFromObject(__object, HotSpotRuntime.markOffset);
        if (HotSpotRuntime.useBiasedLocking)
        {
            // Check for biased locking unlock case, which is a no-op.
            // Note: we do not have to check the thread ID for two reasons.
            // First, the interpreter checks for IllegalMonitorStateException at a higher level.
            // Second, if the bias was revoked while we held the lock, the object could not be
            // rebiased toward another thread, so the bias bit would be clear.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __mark.and(HotSpotRuntime.biasedLockMaskInPlace).equal(WordFactory.unsigned(HotSpotRuntime.biasedLockPattern))))
            {
                EndLockScopeNode.endLockScope();
                return;
            }
        }

        final Word __lock = CurrentLockNode.currentLock(__lockDepth);

        // load displaced mark
        final Word __displacedMark = __lock.readWord(HotSpotRuntime.lockDisplacedMarkOffset, HotSpotReplacementsUtil.DISPLACED_MARK_WORD_LOCATION);

        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_LIKELY_PROBABILITY, __displacedMark.equal(0)))
        {
            // recursive locking => done
        }
        else
        {
            if (!tryExitInflated(__object, __mark, __lock, __threadRegister))
            {
                // Test if object's mark word is pointing to the displaced mark word, and if so,
                // restore the displaced mark in the object - if the object's mark word is not
                // pointing to the displaced mark word, do unlocking via runtime call.
                Pointer __objectPointer = Word.objectToTrackedPointer(__object);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_FAST_PATH_PROBABILITY, __objectPointer.logicCompareAndSwapWord(HotSpotRuntime.markOffset, __lock, __displacedMark, HotSpotReplacementsUtil.MARK_WORD_LOCATION)))
                {
                    // ...
                }
                else
                {
                    // the object's mark word was not pointing to the displaced header
                    monitorexitStubC(MONITOREXIT, __object, __lock);
                }
            }
        }
        EndLockScopeNode.endLockScope();
    }

    private static boolean inlineFastUnlockSupported()
    {
        return GraalOptions.simpleFastInflatedLocking
            && HotSpotRuntime.objectMonitorEntryListOffset >= 0
            && HotSpotRuntime.objectMonitorCxqOffset >= 0
            && HotSpotRuntime.monitorMask >= 0
            && HotSpotRuntime.objectMonitorOwnerOffset >= 0
            && HotSpotRuntime.objectMonitorRecursionsOffset >= 0;
    }

    private static boolean tryExitInflated(Object __object, Word __mark, Word __lock, Register __threadRegister)
    {
        if (!inlineFastUnlockSupported())
        {
            return false;
        }
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __mark.and(HotSpotRuntime.monitorMask).notEqual(0)))
        {
            // inflated case
            // mark is a pointer to the ObjectMonitor + monitorMask
            Word __monitor = __mark.subtract(HotSpotRuntime.monitorMask);
            int __ownerOffset = HotSpotRuntime.objectMonitorOwnerOffset;
            Word __owner = __monitor.readWord(__ownerOffset, HotSpotReplacementsUtil.OBJECT_MONITOR_OWNER_LOCATION);
            int __recursionsOffset = HotSpotRuntime.objectMonitorRecursionsOffset;
            Word __recursions = __monitor.readWord(__recursionsOffset, HotSpotReplacementsUtil.OBJECT_MONITOR_RECURSION_LOCATION);
            Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __owner.xor(__thread).or(__recursions).equal(0)))
            {
                // owner == thread && recursions == 0
                int __cxqOffset = HotSpotRuntime.objectMonitorCxqOffset;
                Word __cxq = __monitor.readWord(__cxqOffset, HotSpotReplacementsUtil.OBJECT_MONITOR_CXQ_LOCATION);
                int __entryListOffset = HotSpotRuntime.objectMonitorEntryListOffset;
                Word __entryList = __monitor.readWord(__entryListOffset, HotSpotReplacementsUtil.OBJECT_MONITOR_ENTRY_LIST_LOCATION);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __cxq.or(__entryList).equal(0)))
                {
                    // cxq == 0 && entryList == 0
                    // nobody is waiting, success
                    MembarNode.memoryBarrier(MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_STORE);
                    __monitor.writeWord(__ownerOffset, WordFactory.zero());
                    return true;
                }
            }
            monitorexitStubC(MONITOREXIT, __object, __lock);
            return true;
        }
        return false;
    }

    ///
    // Calls straight out to the monitorexit stub.
    ///
    @Snippet
    public static void monitorexitStub(Object __object, @ConstantParameter int __lockDepth)
    {
        final Word __lock = CurrentLockNode.currentLock(__lockDepth);
        monitorexitStubC(MONITOREXIT, __object, __lock);
        EndLockScopeNode.endLockScope();
    }

    // @class MonitorSnippets.Templates
    public static final class Templates extends AbstractTemplates
    {
        // @field
        private final SnippetInfo ___monitorenter = snippet(MonitorSnippets.class, "monitorenter");
        // @field
        private final SnippetInfo ___monitorexit = snippet(MonitorSnippets.class, "monitorexit");
        // @field
        private final SnippetInfo ___monitorenterStub = snippet(MonitorSnippets.class, "monitorenterStub");
        // @field
        private final SnippetInfo ___monitorexitStub = snippet(MonitorSnippets.class, "monitorexitStub");

        // @field
        private final boolean ___useFastLocking;

        // @cons
        public Templates(HotSpotProviders __providers, TargetDescription __target, boolean __useFastLocking)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
            this.___useFastLocking = __useFastLocking;
        }

        public void lower(RawMonitorEnterNode __monitorenterNode, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            StructuredGraph __graph = __monitorenterNode.graph();

            Arguments __args;
            if (this.___useFastLocking)
            {
                __args = new Arguments(this.___monitorenter, __graph.getGuardsStage(), __tool.getLoweringStage());
                __args.add("object", __monitorenterNode.object());
                __args.add("hub", __monitorenterNode.getHub());
                __args.addConst("lockDepth", __monitorenterNode.getMonitorId().getLockDepth());
                __args.addConst("threadRegister", __registers.getThreadRegister());
                __args.addConst("stackPointerRegister", __registers.getStackPointerRegister());
            }
            else
            {
                __args = new Arguments(this.___monitorenterStub, __graph.getGuardsStage(), __tool.getLoweringStage());
                __args.add("object", __monitorenterNode.object());
                __args.addConst("lockDepth", __monitorenterNode.getMonitorId().getLockDepth());
            }

            template(__monitorenterNode, __args).instantiate(this.___providers.getMetaAccess(), __monitorenterNode, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(MonitorExitNode __monitorexitNode, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            StructuredGraph __graph = __monitorexitNode.graph();

            Arguments __args;
            if (this.___useFastLocking)
            {
                __args = new Arguments(this.___monitorexit, __graph.getGuardsStage(), __tool.getLoweringStage());
            }
            else
            {
                __args = new Arguments(this.___monitorexitStub, __graph.getGuardsStage(), __tool.getLoweringStage());
            }
            __args.add("object", __monitorexitNode.object());
            __args.addConst("lockDepth", __monitorexitNode.getMonitorId().getLockDepth());
            __args.addConst("threadRegister", __registers.getThreadRegister());

            template(__monitorexitNode, __args).instantiate(this.___providers.getMetaAccess(), __monitorexitNode, SnippetTemplate.DEFAULT_REPLACER, __args);
        }
    }

    // @def
    public static final ForeignCallDescriptor MONITORENTER = new ForeignCallDescriptor("monitorenter", void.class, Object.class, Word.class);
    // @def
    public static final ForeignCallDescriptor MONITOREXIT = new ForeignCallDescriptor("monitorexit", void.class, Object.class, Word.class);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void monitorenterStubC(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Object __object, Word __lock);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void monitorexitStubC(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Object __object, Word __lock);
}
