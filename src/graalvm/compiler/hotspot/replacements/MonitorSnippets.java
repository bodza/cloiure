package graalvm.compiler.hotspot.replacements;

import static jdk.vm.ci.code.MemoryBarriers.LOAD_STORE;
import static jdk.vm.ci.code.MemoryBarriers.STORE_STORE;
import static graalvm.compiler.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static graalvm.compiler.hotspot.nodes.BeginLockScopeNode.beginLockScope;
import static graalvm.compiler.hotspot.nodes.EndLockScopeNode.endLockScope;
import static graalvm.compiler.hotspot.nodes.VMErrorNode.vmError;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.DISPLACED_MARK_WORD_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.MARK_WORD_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.OBJECT_MONITOR_CXQ_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.OBJECT_MONITOR_ENTRY_LIST_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.OBJECT_MONITOR_OWNER_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.OBJECT_MONITOR_RECURSION_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.ageMaskInPlace;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.biasedLockMaskInPlace;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.biasedLockPattern;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.config;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.epochMaskInPlace;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.loadWordFromObject;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.lockDisplacedMarkOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.markOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.monitorMask;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectMonitorCxqOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectMonitorEntryListOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectMonitorOwnerOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.objectMonitorRecursionsOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.pageSize;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.prototypeMarkWordOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.registerAsWord;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.unlockedMask;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.useBiasedLocking;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.verifyOop;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.wordSize;
import static graalvm.compiler.hotspot.replacements.HotspotSnippetsOptions.SimpleFastInflatedLocking;
import static graalvm.compiler.hotspot.replacements.HotspotSnippetsOptions.TraceMonitorsMethodFilter;
import static graalvm.compiler.hotspot.replacements.HotspotSnippetsOptions.TraceMonitorsTypeFilter;
import static graalvm.compiler.hotspot.replacements.HotspotSnippetsOptions.VerifyBalancedMonitors;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.FAST_PATH_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.FREQUENT_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.NOT_FREQUENT_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.NOT_LIKELY_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.SLOW_PATH_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.VERY_FAST_PATH_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.probability;
import static graalvm.compiler.replacements.SnippetTemplate.DEFAULT_REPLACER;

import java.util.List;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.bytecode.Bytecode;
import graalvm.compiler.bytecode.ResolvedJavaMethodBytecode;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import graalvm.compiler.hotspot.nodes.AcquiredCASLockNode;
import graalvm.compiler.hotspot.nodes.CurrentLockNode;
import graalvm.compiler.hotspot.nodes.FastAcquireBiasedLockNode;
import graalvm.compiler.hotspot.nodes.MonitorCounterNode;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.nodes.BreakpointNode;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.DeoptimizeNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.NamedLocationIdentity;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ReturnNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.nodes.extended.MembarNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.java.MonitorExitNode;
import graalvm.compiler.nodes.java.RawMonitorEnterNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.replacements.Log;
import graalvm.compiler.replacements.SnippetCounter;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.word.Word;
import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Snippets used for implementing the monitorenter and monitorexit instructions.
 *
 * The locking algorithm used is described in the paper
 * <a href="http://dl.acm.org/citation.cfm?id=1167515.1167496"> Eliminating synchronization-related
 * atomic operations with biased locking and bulk rebiasing</a> by Kenneth Russell and David
 * Detlefs.
 *
 * Comment below is reproduced from {@code markOop.hpp} for convenience:
 *
 * <pre>
 *  Bit-format of an object header (most significant first, big endian layout below):
 *  32 bits:
 *  --------
 *             hash:25 ------------>| age:4    biased_lock:1 lock:2 (normal object)
 *             JavaThread*:23 epoch:2 age:4    biased_lock:1 lock:2 (biased object)
 *             size:32 ------------------------------------------>| (CMS free block)
 *             PromotedObject*:29 ---------->| promo_bits:3 ----->| (CMS promoted object)
 *
 *  64 bits:
 *  --------
 *  unused:25 hash:31 -->| unused:1   age:4    biased_lock:1 lock:2 (normal object)
 *  JavaThread*:54 epoch:2 unused:1   age:4    biased_lock:1 lock:2 (biased object)
 *  PromotedObject*:61 --------------------->| promo_bits:3 ----->| (CMS promoted object)
 *  size:64 ----------------------------------------------------->| (CMS free block)
 *
 *  unused:25 hash:31 -->| cms_free:1 age:4    biased_lock:1 lock:2 (COOPs && normal object)
 *  JavaThread*:54 epoch:2 cms_free:1 age:4    biased_lock:1 lock:2 (COOPs && biased object)
 *  narrowOop:32 unused:24 cms_free:1 unused:4 promo_bits:3 ----->| (COOPs && CMS promoted object)
 *  unused:21 size:35 -->| cms_free:1 unused:7 ------------------>| (COOPs && CMS free block)
 *
 *  - hash contains the identity hash value: largest value is
 *    31 bits, see os::random().  Also, 64-bit vm's require
 *    a hash value no bigger than 32 bits because they will not
 *    properly generate a mask larger than that: see library_call.cpp
 *    and c1_CodePatterns_sparc.cpp.
 *
 *  - the biased lock pattern is used to bias a lock toward a given
 *    thread. When this pattern is set in the low three bits, the lock
 *    is either biased toward a given thread or "anonymously" biased,
 *    indicating that it is possible for it to be biased. When the
 *    lock is biased toward a given thread, locking and unlocking can
 *    be performed by that thread without using atomic operations.
 *    When a lock's bias is revoked, it reverts back to the normal
 *    locking scheme described below.
 *
 *    Note that we are overloading the meaning of the "unlocked" state
 *    of the header. Because we steal a bit from the age we can
 *    guarantee that the bias pattern will never be seen for a truly
 *    unlocked object.
 *
 *    Note also that the biased state contains the age bits normally
 *    contained in the object header. Large increases in scavenge
 *    times were seen when these bits were absent and an arbitrary age
 *    assigned to all biased objects, because they tended to consume a
 *    significant fraction of the eden semispaces and were not
 *    promoted promptly, causing an increase in the amount of copying
 *    performed. The runtime system aligns all JavaThread* pointers to
 *    a very large value (currently 128 bytes (32bVM) or 256 bytes (64bVM))
 *    to make room for the age bits & the epoch bits (used in support of
 *    biased locking), and for the CMS "freeness" bit in the 64bVM (+COOPs).
 *
 *    [JavaThread* | epoch | age | 1 | 01]       lock is biased toward given thread
 *    [0           | epoch | age | 1 | 01]       lock is anonymously biased
 *
 *  - the two lock bits are used to describe three states: locked/unlocked and monitor.
 *
 *    [ptr             | 00]  locked             ptr points to real header on stack
 *    [header      | 0 | 01]  unlocked           regular object header
 *    [ptr             | 10]  monitor            inflated lock (header is wapped out)
 *    [ptr             | 11]  marked             used by markSweep to mark an object
 *                                               not valid at any other time
 *
 *    We assume that stack/thread pointers have the lowest two bits cleared.
 * </pre>
 *
 * Note that {@code Thread::allocate} enforces {@code JavaThread} objects to be aligned
 * appropriately to comply with the layouts above.
 */
public class MonitorSnippets implements Snippets
{
    private static final boolean PROFILE_CONTEXT = false;

    @Snippet
    public static void monitorenter(Object object, KlassPointer hub, @ConstantParameter int lockDepth, @ConstantParameter Register threadRegister, @ConstantParameter Register stackPointerRegister, @ConstantParameter boolean trace, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        verifyOop(object);

        // Load the mark word - this includes a null-check on object
        final Word mark = loadWordFromObject(object, markOffset(INJECTED_VMCONFIG));

        final Word lock = beginLockScope(lockDepth);

        Pointer objectPointer = Word.objectToTrackedPointer(object);

        incCounter(options);

        if (useBiasedLocking(INJECTED_VMCONFIG))
        {
            if (tryEnterBiased(object, hub, lock, mark, threadRegister, trace, options, counters))
            {
                return;
            }
            // not biased, fall-through
        }
        if (inlineFastLockSupported(options) && probability(SLOW_PATH_PROBABILITY, mark.and(monitorMask(INJECTED_VMCONFIG)).notEqual(0)))
        {
            // Inflated case
            if (tryEnterInflated(object, lock, mark, threadRegister, trace, options, counters))
            {
                return;
            }
        }
        else
        {
            // Create the unlocked mark word pattern
            Word unlockedMark = mark.or(unlockedMask(INJECTED_VMCONFIG));

            // Copy this unlocked mark word into the lock slot on the stack
            lock.writeWord(lockDisplacedMarkOffset(INJECTED_VMCONFIG), unlockedMark, DISPLACED_MARK_WORD_LOCATION);

            // make sure previous store does not float below compareAndSwap
            MembarNode.memoryBarrier(STORE_STORE);

            // Test if the object's mark word is unlocked, and if so, store the
            // (address of) the lock slot into the object's mark word.
            Word currentMark = objectPointer.compareAndSwapWord(markOffset(INJECTED_VMCONFIG), unlockedMark, lock, MARK_WORD_LOCATION);
            if (probability(FAST_PATH_PROBABILITY, currentMark.equal(unlockedMark)))
            {
                counters.lockCas.inc();
                AcquiredCASLockNode.mark(object);
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
                final Word alignedMask = WordFactory.unsigned(wordSize() - 1);
                final Word stackPointer = registerAsWord(stackPointerRegister).add(config(INJECTED_VMCONFIG).stackBias);
                if (probability(FAST_PATH_PROBABILITY, currentMark.subtract(stackPointer).and(alignedMask.subtract(pageSize())).equal(0)))
                {
                    // Recursively locked => write 0 to the lock slot
                    lock.writeWord(lockDisplacedMarkOffset(INJECTED_VMCONFIG), WordFactory.zero(), DISPLACED_MARK_WORD_LOCATION);
                    counters.lockCasRecursive.inc();
                    return;
                }
                counters.lockStubFailedCas.inc();
            }
        }
        // slow-path runtime-call
        monitorenterStubC(MONITORENTER, object, lock);
    }

    private static boolean tryEnterBiased(Object object, KlassPointer hub, Word lock, Word mark, Register threadRegister, boolean trace, OptionValues options, Counters counters)
    {
        // See whether the lock is currently biased toward our thread and
        // whether the epoch is still valid.
        // Note that the runtime guarantees sufficient alignment of JavaThread
        // pointers to allow age to be placed into low bits.
        final Word biasableLockBits = mark.and(biasedLockMaskInPlace(INJECTED_VMCONFIG));

        // Check whether the bias pattern is present in the object's mark word
        // and the bias owner and the epoch are both still current.
        final Word prototypeMarkWord = hub.readWord(prototypeMarkWordOffset(INJECTED_VMCONFIG), PROTOTYPE_MARK_WORD_LOCATION);
        final Word thread = registerAsWord(threadRegister);
        final Word tmp = prototypeMarkWord.or(thread).xor(mark).and(~ageMaskInPlace(INJECTED_VMCONFIG));
        if (probability(FAST_PATH_PROBABILITY, tmp.equal(0)))
        {
            // Object is already biased to current thread -> done
            counters.lockBiasExisting.inc();
            FastAcquireBiasedLockNode.mark(object);
            return true;
        }

        // Now check to see whether biasing is enabled for this object
        if (probability(NOT_FREQUENT_PROBABILITY, biasableLockBits.equal(WordFactory.unsigned(biasedLockPattern(INJECTED_VMCONFIG)))))
        {
            Pointer objectPointer = Word.objectToTrackedPointer(object);
            // At this point we know that the mark word has the bias pattern and
            // that we are not the bias owner in the current epoch. We need to
            // figure out more details about the state of the mark word in order to
            // know what operations can be legally performed on the object's
            // mark word.

            // If the low three bits in the xor result aren't clear, that means
            // the prototype header is no longer biasable and we have to revoke
            // the bias on this object.
            if (probability(FREQUENT_PROBABILITY, tmp.and(biasedLockMaskInPlace(INJECTED_VMCONFIG)).equal(0)))
            {
                // Biasing is still enabled for object's type. See whether the
                // epoch of the current bias is still valid, meaning that the epoch
                // bits of the mark word are equal to the epoch bits of the
                // prototype mark word. (Note that the prototype mark word's epoch bits
                // only change at a safepoint.) If not, attempt to rebias the object
                // toward the current thread. Note that we must be absolutely sure
                // that the current epoch is invalid in order to do this because
                // otherwise the manipulations it performs on the mark word are
                // illegal.
                if (probability(FREQUENT_PROBABILITY, tmp.and(epochMaskInPlace(INJECTED_VMCONFIG)).equal(0)))
                {
                    // The epoch of the current bias is still valid but we know nothing
                    // about the owner; it might be set or it might be clear. Try to
                    // acquire the bias of the object using an atomic operation. If this
                    // fails we will go in to the runtime to revoke the object's bias.
                    // Note that we first construct the presumed unbiased header so we
                    // don't accidentally blow away another thread's valid bias.
                    Word unbiasedMark = mark.and(biasedLockMaskInPlace(INJECTED_VMCONFIG) | ageMaskInPlace(INJECTED_VMCONFIG) | epochMaskInPlace(INJECTED_VMCONFIG));
                    Word biasedMark = unbiasedMark.or(thread);
                    if (probability(VERY_FAST_PATH_PROBABILITY, objectPointer.logicCompareAndSwapWord(markOffset(INJECTED_VMCONFIG), unbiasedMark, biasedMark, MARK_WORD_LOCATION)))
                    {
                        // Object is now biased to current thread -> done
                        counters.lockBiasAcquired.inc();
                        return true;
                    }
                    // If the biasing toward our thread failed, this means that another thread
                    // owns the bias and we need to revoke that bias. The revocation will occur
                    // in the interpreter runtime.
                    counters.lockStubRevoke.inc();
                }
                else
                {
                    // At this point we know the epoch has expired, meaning that the
                    // current bias owner, if any, is actually invalid. Under these
                    // circumstances _only_, are we allowed to use the current mark word
                    // value as the comparison value when doing the CAS to acquire the
                    // bias in the current epoch. In other words, we allow transfer of
                    // the bias from one thread to another directly in this situation.
                    Word biasedMark = prototypeMarkWord.or(thread);
                    if (probability(VERY_FAST_PATH_PROBABILITY, objectPointer.logicCompareAndSwapWord(markOffset(INJECTED_VMCONFIG), mark, biasedMark, MARK_WORD_LOCATION)))
                    {
                        // Object is now biased to current thread -> done
                        counters.lockBiasTransfer.inc();
                        return true;
                    }
                    // If the biasing toward our thread failed, then another thread
                    // succeeded in biasing it toward itself and we need to revoke that
                    // bias. The revocation will occur in the runtime in the slow case.
                    counters.lockStubEpochExpired.inc();
                }
                // slow-path runtime-call
                monitorenterStubC(MONITORENTER, object, lock);
                return true;
            }
            else
            {
                // The prototype mark word doesn't have the bias bit set any
                // more, indicating that objects of this data type are not supposed
                // to be biased any more. We are going to try to reset the mark of
                // this object to the prototype value and fall through to the
                // CAS-based locking scheme. Note that if our CAS fails, it means
                // that another thread raced us for the privilege of revoking the
                // bias of this particular object, so it's okay to continue in the
                // normal locking code.
                Word result = objectPointer.compareAndSwapWord(markOffset(INJECTED_VMCONFIG), mark, prototypeMarkWord, MARK_WORD_LOCATION);

                // Fall through to the normal CAS-based lock, because no matter what
                // the result of the above CAS, some thread must have succeeded in
                // removing the bias bit from the object's header.

                if (ENABLE_BREAKPOINT)
                {
                    bkpt(object, mark, tmp, result);
                }
                counters.revokeBias.inc();
                return false;
            }
        }
        else
        {
            // Biasing not enabled -> fall through to lightweight locking
            counters.unbiasable.inc();
            return false;
        }
    }

    @Fold
    public static boolean useFastInflatedLocking(OptionValues options)
    {
        return SimpleFastInflatedLocking.getValue(options);
    }

    private static boolean inlineFastLockSupported(OptionValues options)
    {
        return inlineFastLockSupported(INJECTED_VMCONFIG, options);
    }

    private static boolean inlineFastLockSupported(GraalHotSpotVMConfig config, OptionValues options)
    {
        return useFastInflatedLocking(options) && monitorMask(config) >= 0 && objectMonitorOwnerOffset(config) >= 0;
    }

    private static boolean tryEnterInflated(Object object, Word lock, Word mark, Register threadRegister, boolean trace, OptionValues options, Counters counters)
    {
        // write non-zero value to lock slot
        lock.writeWord(lockDisplacedMarkOffset(INJECTED_VMCONFIG), lock, DISPLACED_MARK_WORD_LOCATION);
        // mark is a pointer to the ObjectMonitor + monitorMask
        Word monitor = mark.subtract(monitorMask(INJECTED_VMCONFIG));
        int ownerOffset = objectMonitorOwnerOffset(INJECTED_VMCONFIG);
        Word owner = monitor.readWord(ownerOffset, OBJECT_MONITOR_OWNER_LOCATION);
        if (probability(FREQUENT_PROBABILITY, owner.equal(0)))
        {
            // it appears unlocked (owner == 0)
            if (probability(FREQUENT_PROBABILITY, monitor.logicCompareAndSwapWord(ownerOffset, owner, registerAsWord(threadRegister), OBJECT_MONITOR_OWNER_LOCATION)))
            {
                // success
                counters.inflatedCas.inc();
                return true;
            }
            else
            {
                counters.inflatedFailedCas.inc();
            }
        }
        else
        {
            counters.inflatedOwned.inc();
        }
        return false;
    }

    /**
     * Calls straight out to the monitorenter stub.
     */
    @Snippet
    public static void monitorenterStub(Object object, @ConstantParameter int lockDepth, @ConstantParameter boolean trace, @ConstantParameter OptionValues options)
    {
        verifyOop(object);
        incCounter(options);
        if (object == null)
        {
            DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.NullCheckException);
        }
        // BeginLockScope nodes do not read from object so a use of object
        // cannot float about the null check above
        final Word lock = beginLockScope(lockDepth);
        monitorenterStubC(MONITORENTER, object, lock);
    }

    @Snippet
    public static void monitorexit(Object object, @ConstantParameter int lockDepth, @ConstantParameter Register threadRegister, @ConstantParameter boolean trace, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        final Word mark = loadWordFromObject(object, markOffset(INJECTED_VMCONFIG));
        if (useBiasedLocking(INJECTED_VMCONFIG))
        {
            // Check for biased locking unlock case, which is a no-op
            // Note: we do not have to check the thread ID for two reasons.
            // First, the interpreter checks for IllegalMonitorStateException at
            // a higher level. Second, if the bias was revoked while we held the
            // lock, the object could not be rebiased toward another thread, so
            // the bias bit would be clear.
            if (probability(FREQUENT_PROBABILITY, mark.and(biasedLockMaskInPlace(INJECTED_VMCONFIG)).equal(WordFactory.unsigned(biasedLockPattern(INJECTED_VMCONFIG)))))
            {
                endLockScope();
                decCounter(options);
                counters.unlockBias.inc();
                return;
            }
        }

        final Word lock = CurrentLockNode.currentLock(lockDepth);

        // Load displaced mark
        final Word displacedMark = lock.readWord(lockDisplacedMarkOffset(INJECTED_VMCONFIG), DISPLACED_MARK_WORD_LOCATION);

        if (probability(NOT_LIKELY_PROBABILITY, displacedMark.equal(0)))
        {
            // Recursive locking => done
            counters.unlockCasRecursive.inc();
        }
        else
        {
            if (!tryExitInflated(object, mark, lock, threadRegister, trace, options, counters))
            {
                verifyOop(object);
                // Test if object's mark word is pointing to the displaced mark word, and if so,
                // restore
                // the displaced mark in the object - if the object's mark word is not pointing to
                // the displaced mark word, do unlocking via runtime call.
                Pointer objectPointer = Word.objectToTrackedPointer(object);
                if (probability(VERY_FAST_PATH_PROBABILITY, objectPointer.logicCompareAndSwapWord(markOffset(INJECTED_VMCONFIG), lock, displacedMark, MARK_WORD_LOCATION)))
                {
                    counters.unlockCas.inc();
                }
                else
                {
                    // The object's mark word was not pointing to the displaced header
                    counters.unlockStub.inc();
                    monitorexitStubC(MONITOREXIT, object, lock);
                }
            }
        }
        endLockScope();
        decCounter(options);
    }

    private static boolean inlineFastUnlockSupported(OptionValues options)
    {
        return inlineFastUnlockSupported(INJECTED_VMCONFIG, options);
    }

    private static boolean inlineFastUnlockSupported(GraalHotSpotVMConfig config, OptionValues options)
    {
        return useFastInflatedLocking(options) && objectMonitorEntryListOffset(config) >= 0 && objectMonitorCxqOffset(config) >= 0 && monitorMask(config) >= 0 && objectMonitorOwnerOffset(config) >= 0 && objectMonitorRecursionsOffset(config) >= 0;
    }

    private static boolean tryExitInflated(Object object, Word mark, Word lock, Register threadRegister, boolean trace, OptionValues options, Counters counters)
    {
        if (!inlineFastUnlockSupported(options))
        {
            return false;
        }
        if (probability(SLOW_PATH_PROBABILITY, mark.and(monitorMask(INJECTED_VMCONFIG)).notEqual(0)))
        {
            // Inflated case
            // mark is a pointer to the ObjectMonitor + monitorMask
            Word monitor = mark.subtract(monitorMask(INJECTED_VMCONFIG));
            int ownerOffset = objectMonitorOwnerOffset(INJECTED_VMCONFIG);
            Word owner = monitor.readWord(ownerOffset, OBJECT_MONITOR_OWNER_LOCATION);
            int recursionsOffset = objectMonitorRecursionsOffset(INJECTED_VMCONFIG);
            Word recursions = monitor.readWord(recursionsOffset, OBJECT_MONITOR_RECURSION_LOCATION);
            Word thread = registerAsWord(threadRegister);
            if (probability(FAST_PATH_PROBABILITY, owner.xor(thread).or(recursions).equal(0)))
            {
                // owner == thread && recursions == 0
                int cxqOffset = objectMonitorCxqOffset(INJECTED_VMCONFIG);
                Word cxq = monitor.readWord(cxqOffset, OBJECT_MONITOR_CXQ_LOCATION);
                int entryListOffset = objectMonitorEntryListOffset(INJECTED_VMCONFIG);
                Word entryList = monitor.readWord(entryListOffset, OBJECT_MONITOR_ENTRY_LIST_LOCATION);
                if (probability(FREQUENT_PROBABILITY, cxq.or(entryList).equal(0)))
                {
                    // cxq == 0 && entryList == 0
                    // Nobody is waiting, success
                    // release_store
                    MembarNode.memoryBarrier(LOAD_STORE | STORE_STORE);
                    monitor.writeWord(ownerOffset, WordFactory.zero());
                    counters.unlockInflatedSimple.inc();
                    return true;
                }
            }
            counters.unlockStubInflated.inc();
            monitorexitStubC(MONITOREXIT, object, lock);
            return true;
        }
        return false;
    }

    /**
     * Calls straight out to the monitorexit stub.
     */
    @Snippet
    public static void monitorexitStub(Object object, @ConstantParameter int lockDepth, @ConstantParameter boolean trace, @ConstantParameter OptionValues options)
    {
        verifyOop(object);
        final Word lock = CurrentLockNode.currentLock(lockDepth);
        monitorexitStubC(MONITOREXIT, object, lock);
        endLockScope();
        decCounter(options);
    }

    /**
     * Leaving the breakpoint code in to provide an example of how to use the {@link BreakpointNode}
     * intrinsic.
     */
    private static final boolean ENABLE_BREAKPOINT = false;

    private static final LocationIdentity MONITOR_COUNTER_LOCATION = NamedLocationIdentity.mutable("MonitorCounter");

    @NodeIntrinsic(BreakpointNode.class)
    static native void bkpt(Object object, Word mark, Word tmp, Word value);

    @Fold
    static boolean verifyBalancedMonitors(OptionValues options)
    {
        return VerifyBalancedMonitors.getValue(options);
    }

    public static void incCounter(OptionValues options)
    {
        if (verifyBalancedMonitors(options))
        {
            final Word counter = MonitorCounterNode.counter();
            final int count = counter.readInt(0, MONITOR_COUNTER_LOCATION);
            counter.writeInt(0, count + 1, MONITOR_COUNTER_LOCATION);
        }
    }

    public static void decCounter(OptionValues options)
    {
        if (verifyBalancedMonitors(options))
        {
            final Word counter = MonitorCounterNode.counter();
            final int count = counter.readInt(0, MONITOR_COUNTER_LOCATION);
            counter.writeInt(0, count - 1, MONITOR_COUNTER_LOCATION);
        }
    }

    @Snippet
    private static void initCounter()
    {
        final Word counter = MonitorCounterNode.counter();
        counter.writeInt(0, 0, MONITOR_COUNTER_LOCATION);
    }

    @Snippet
    private static void checkCounter(@ConstantParameter String errMsg)
    {
        final Word counter = MonitorCounterNode.counter();
        final int count = counter.readInt(0, MONITOR_COUNTER_LOCATION);
        if (count != 0)
        {
            vmError(errMsg, count);
        }
    }

    public static class Counters
    {
        /**
         * Counters for the various paths for acquiring a lock. The counters whose names start with
         * {@code "lock"} are mutually exclusive. The other counters are for paths that may be
         * shared.
         */
        public final SnippetCounter lockBiasExisting;
        public final SnippetCounter lockBiasAcquired;
        public final SnippetCounter lockBiasTransfer;
        public final SnippetCounter lockCas;
        public final SnippetCounter lockCasRecursive;
        public final SnippetCounter lockStubEpochExpired;
        public final SnippetCounter lockStubRevoke;
        public final SnippetCounter lockStubFailedCas;
        public final SnippetCounter inflatedCas;
        public final SnippetCounter inflatedFailedCas;
        public final SnippetCounter inflatedOwned;
        public final SnippetCounter unbiasable;
        public final SnippetCounter revokeBias;

        /**
         * Counters for the various paths for releasing a lock. The counters whose names start with
         * {@code "unlock"} are mutually exclusive. The other counters are for paths that may be
         * shared.
         */
        public final SnippetCounter unlockBias;
        public final SnippetCounter unlockCas;
        public final SnippetCounter unlockCasRecursive;
        public final SnippetCounter unlockStub;
        public final SnippetCounter unlockStubInflated;
        public final SnippetCounter unlockInflatedSimple;

        public Counters(SnippetCounter.Group.Factory factory)
        {
            SnippetCounter.Group enter = factory.createSnippetCounterGroup("MonitorEnters");
            SnippetCounter.Group exit = factory.createSnippetCounterGroup("MonitorExits");
            lockBiasExisting = new SnippetCounter(enter, "lock{bias:existing}", "bias-locked previously biased object");
            lockBiasAcquired = new SnippetCounter(enter, "lock{bias:acquired}", "bias-locked newly biased object");
            lockBiasTransfer = new SnippetCounter(enter, "lock{bias:transfer}", "bias-locked, biased transferred");
            lockCas = new SnippetCounter(enter, "lock{cas}", "cas-locked an object");
            lockCasRecursive = new SnippetCounter(enter, "lock{cas:recursive}", "cas-locked, recursive");
            lockStubEpochExpired = new SnippetCounter(enter, "lock{stub:epoch-expired}", "stub-locked, epoch expired");
            lockStubRevoke = new SnippetCounter(enter, "lock{stub:revoke}", "stub-locked, biased revoked");
            lockStubFailedCas = new SnippetCounter(enter, "lock{stub:failed-cas/stack}", "stub-locked, failed cas and stack locking");
            inflatedCas = new SnippetCounter(enter, "lock{inflated:cas}", "heavyweight-locked, cas-locked");
            inflatedFailedCas = new SnippetCounter(enter, "lock{inflated:failed-cas}", "heavyweight-locked, failed cas");
            inflatedOwned = new SnippetCounter(enter, "lock{inflated:owned}", "heavyweight-locked, already owned");
            unbiasable = new SnippetCounter(enter, "unbiasable", "object with unbiasable type");
            revokeBias = new SnippetCounter(enter, "revokeBias", "object had bias revoked");

            unlockBias = new SnippetCounter(exit, "unlock{bias}", "bias-unlocked an object");
            unlockCas = new SnippetCounter(exit, "unlock{cas}", "cas-unlocked an object");
            unlockCasRecursive = new SnippetCounter(exit, "unlock{cas:recursive}", "cas-unlocked an object, recursive");
            unlockStub = new SnippetCounter(exit, "unlock{stub}", "stub-unlocked an object");
            unlockStubInflated = new SnippetCounter(exit, "unlock{stub:inflated}", "stub-unlocked an object with inflated monitor");
            unlockInflatedSimple = new SnippetCounter(exit, "unlock{inflated}", "unlocked an object monitor");
        }
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo monitorenter = snippet(MonitorSnippets.class, "monitorenter");
        private final SnippetInfo monitorexit = snippet(MonitorSnippets.class, "monitorexit");
        private final SnippetInfo monitorenterStub = snippet(MonitorSnippets.class, "monitorenterStub");
        private final SnippetInfo monitorexitStub = snippet(MonitorSnippets.class, "monitorexitStub");
        private final SnippetInfo initCounter = snippet(MonitorSnippets.class, "initCounter");
        private final SnippetInfo checkCounter = snippet(MonitorSnippets.class, "checkCounter");

        private final boolean useFastLocking;
        public final Counters counters;

        public Templates(OptionValues options, SnippetCounter.Group.Factory factory, HotSpotProviders providers, TargetDescription target, boolean useFastLocking)
        {
            super(options, providers, providers.getSnippetReflection(), target);
            this.useFastLocking = useFastLocking;

            this.counters = new Counters(factory);
        }

        public void lower(RawMonitorEnterNode monitorenterNode, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            StructuredGraph graph = monitorenterNode.graph();
            checkBalancedMonitors(graph, tool);

            Arguments args;
            if (useFastLocking)
            {
                args = new Arguments(monitorenter, graph.getGuardsStage(), tool.getLoweringStage());
                args.add("object", monitorenterNode.object());
                args.add("hub", monitorenterNode.getHub());
                args.addConst("lockDepth", monitorenterNode.getMonitorId().getLockDepth());
                args.addConst("threadRegister", registers.getThreadRegister());
                args.addConst("stackPointerRegister", registers.getStackPointerRegister());
                args.addConst("trace", isTracingEnabledForType(monitorenterNode.object()) || isTracingEnabledForMethod(graph));
                args.addConst("options", graph.getOptions());
                args.addConst("counters", counters);
            }
            else
            {
                args = new Arguments(monitorenterStub, graph.getGuardsStage(), tool.getLoweringStage());
                args.add("object", monitorenterNode.object());
                args.addConst("lockDepth", monitorenterNode.getMonitorId().getLockDepth());
                args.addConst("trace", isTracingEnabledForType(monitorenterNode.object()) || isTracingEnabledForMethod(graph));
                args.addConst("options", graph.getOptions());
                args.addConst("counters", counters);
            }

            template(monitorenterNode, args).instantiate(providers.getMetaAccess(), monitorenterNode, DEFAULT_REPLACER, args);
        }

        public void lower(MonitorExitNode monitorexitNode, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            StructuredGraph graph = monitorexitNode.graph();

            Arguments args;
            if (useFastLocking)
            {
                args = new Arguments(monitorexit, graph.getGuardsStage(), tool.getLoweringStage());
            }
            else
            {
                args = new Arguments(monitorexitStub, graph.getGuardsStage(), tool.getLoweringStage());
            }
            args.add("object", monitorexitNode.object());
            args.addConst("lockDepth", monitorexitNode.getMonitorId().getLockDepth());
            args.addConst("threadRegister", registers.getThreadRegister());
            args.addConst("trace", isTracingEnabledForType(monitorexitNode.object()) || isTracingEnabledForMethod(graph));
            args.addConst("options", graph.getOptions());
            args.addConst("counters", counters);

            template(monitorexitNode, args).instantiate(providers.getMetaAccess(), monitorexitNode, DEFAULT_REPLACER, args);
        }

        public static boolean isTracingEnabledForType(ValueNode object)
        {
            ResolvedJavaType type = StampTool.typeOrNull(object.stamp(NodeView.DEFAULT));
            String filter = TraceMonitorsTypeFilter.getValue(object.getOptions());
            if (filter == null)
            {
                return false;
            }
            else
            {
                if (filter.length() == 0)
                {
                    return true;
                }
                if (type == null)
                {
                    return false;
                }
                return (type.getName().contains(filter));
            }
        }

        public static boolean isTracingEnabledForMethod(StructuredGraph graph)
        {
            String filter = TraceMonitorsMethodFilter.getValue(graph.getOptions());
            if (filter == null)
            {
                return false;
            }
            else
            {
                if (filter.length() == 0)
                {
                    return true;
                }
                if (graph.method() == null)
                {
                    return false;
                }
                return (graph.method().format("%H.%n").contains(filter));
            }
        }

        /**
         * If balanced monitor checking is enabled then nodes are inserted at the start and all
         * return points of the graph to initialize and check the monitor counter respectively.
         */
        private void checkBalancedMonitors(StructuredGraph graph, LoweringTool tool)
        {
            if (VerifyBalancedMonitors.getValue(options))
            {
                NodeIterable<MonitorCounterNode> nodes = graph.getNodes().filter(MonitorCounterNode.class);
                if (nodes.isEmpty())
                {
                    // Only insert the nodes if this is the first monitorenter being lowered.
                    JavaType returnType = initCounter.getMethod().getSignature().getReturnType(initCounter.getMethod().getDeclaringClass());
                    StampPair returnStamp = StampFactory.forDeclaredType(graph.getAssumptions(), returnType, false);
                    MethodCallTargetNode callTarget = graph.add(new MethodCallTargetNode(InvokeKind.Static, initCounter.getMethod(), new ValueNode[0], returnStamp, null));
                    InvokeNode invoke = graph.add(new InvokeNode(callTarget, 0));
                    invoke.setStateAfter(graph.start().stateAfter());
                    graph.addAfterFixed(graph.start(), invoke);

                    StructuredGraph inlineeGraph = providers.getReplacements().getSnippet(initCounter.getMethod(), null);
                    InliningUtil.inline(invoke, inlineeGraph, false, null);

                    List<ReturnNode> rets = graph.getNodes(ReturnNode.TYPE).snapshot();
                    for (ReturnNode ret : rets)
                    {
                        returnType = checkCounter.getMethod().getSignature().getReturnType(checkCounter.getMethod().getDeclaringClass());
                        String msg = "unbalanced monitors in " + graph.method().format("%H.%n(%p)") + ", count = %d";
                        ConstantNode errMsg = ConstantNode.forConstant(tool.getConstantReflection().forString(msg), providers.getMetaAccess(), graph);
                        returnStamp = StampFactory.forDeclaredType(graph.getAssumptions(), returnType, false);
                        callTarget = graph.add(new MethodCallTargetNode(InvokeKind.Static, checkCounter.getMethod(), new ValueNode[]{errMsg}, returnStamp, null));
                        invoke = graph.add(new InvokeNode(callTarget, 0));
                        Bytecode code = new ResolvedJavaMethodBytecode(graph.method());
                        FrameState stateAfter = new FrameState(null, code, BytecodeFrame.AFTER_BCI, new ValueNode[0], new ValueNode[0], 0, new ValueNode[0], null, false, false);
                        invoke.setStateAfter(graph.add(stateAfter));
                        graph.addBeforeFixed(ret, invoke);

                        Arguments args = new Arguments(checkCounter, graph.getGuardsStage(), tool.getLoweringStage());
                        args.addConst("errMsg", msg);
                        inlineeGraph = template(invoke, args).copySpecializedGraph();
                        InliningUtil.inline(invoke, inlineeGraph, false, null);
                    }
                }
            }
        }
    }

    public static final ForeignCallDescriptor MONITORENTER = new ForeignCallDescriptor("monitorenter", void.class, Object.class, Word.class);
    public static final ForeignCallDescriptor MONITOREXIT = new ForeignCallDescriptor("monitorexit", void.class, Object.class, Word.class);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void monitorenterStubC(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object object, Word lock);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void monitorexitStubC(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object object, Word lock);
}
