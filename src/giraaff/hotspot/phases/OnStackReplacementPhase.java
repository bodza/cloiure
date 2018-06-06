package giraaff.hotspot.phases;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.SpeculationLog;
import jdk.vm.ci.runtime.JVMCICompiler;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.Loop;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.iterators.NodeIterable;
import giraaff.loop.LoopsData;
import giraaff.loop.phases.LoopTransformations;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.EntryMarkerNode;
import giraaff.nodes.EntryProxyNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.extended.OSRLocalNode;
import giraaff.nodes.extended.OSRLockNode;
import giraaff.nodes.extended.OSRMonitorEnterNode;
import giraaff.nodes.extended.OSRStartNode;
import giraaff.nodes.java.AccessMonitorNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.java.MonitorEnterNode;
import giraaff.nodes.java.MonitorExitNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.util.GraalError;

// @class OnStackReplacementPhase
public final class OnStackReplacementPhase extends Phase
{
    // @cons OnStackReplacementPhase
    public OnStackReplacementPhase()
    {
        super();
    }

    @Override
    protected void run(StructuredGraph __graph)
    {
        if (__graph.getEntryBCI() == JVMCICompiler.INVOCATION_ENTRY_BCI)
        {
            // This happens during inlining in a OSR method, because the same phase plan will be used.
            return;
        }

        EntryMarkerNode __osr;
        int __maxIterations = -1;
        int __iterations = 0;

        final EntryMarkerNode __originalOSRNode = getEntryMarker(__graph);
        final LoopBeginNode __originalOSRLoop = osrLoop(__originalOSRNode);
        final boolean __currentOSRWithLocks = osrWithLocks(__originalOSRNode);

        if (__originalOSRLoop == null)
        {
            // OSR with Locks: We do not have an OSR loop for the original OSR bci. Therefore we
            // cannot decide where to deopt and which framestate will be used. In the worst case
            // the framestate of the OSR entry would be used.
            throw new BailoutException("OSR compilation without OSR entry loop.");
        }

        if (!GraalOptions.supportOSRWithLocks && __currentOSRWithLocks)
        {
            throw new BailoutException("OSR with locks disabled.");
        }

        while (true)
        {
            __osr = getEntryMarker(__graph);
            LoopsData __loops = new LoopsData(__graph);
            // find the loop that contains the EntryMarker
            Loop<Block> __l = __loops.getCFG().getNodeToBlock().get(__osr).getLoop();
            if (__l == null)
            {
                break;
            }

            __iterations++;
            if (__maxIterations == -1)
            {
                __maxIterations = __l.getDepth();
            }
            else if (__iterations > __maxIterations)
            {
                throw GraalError.shouldNotReachHere();
            }
            // peel the outermost loop first
            while (__l.getParent() != null)
            {
                __l = __l.getParent();
            }

            LoopTransformations.peel(__loops.loop(__l));
            __osr.replaceAtUsages(InputType.Guard, AbstractBeginNode.prevBegin((FixedNode) __osr.predecessor()));
            for (Node __usage : __osr.usages().snapshot())
            {
                EntryProxyNode __proxy = (EntryProxyNode) __usage;
                __proxy.replaceAndDelete(__proxy.value());
            }
            GraphUtil.removeFixedWithUnusedInputs(__osr);
        }

        StartNode __start = __graph.start();
        FrameState __osrState = __osr.stateAfter();
        __osr.setStateAfter(null);
        OSRStartNode __osrStart = __graph.add(new OSRStartNode());
        FixedNode __next = __osr.next();
        __osr.setNext(null);
        __osrStart.setNext(__next);
        __graph.setStart(__osrStart);
        __osrStart.setStateAfter(__osrState);

        final int __localsSize = __osrState.localsSize();
        final int __locksSize = __osrState.locksSize();

        for (int __i = 0; __i < __localsSize + __locksSize; __i++)
        {
            ValueNode __value = null;
            if (__i >= __localsSize)
            {
                __value = __osrState.lockAt(__i - __localsSize);
            }
            else
            {
                __value = __osrState.localAt(__i);
            }
            if (__value instanceof EntryProxyNode)
            {
                EntryProxyNode __proxy = (EntryProxyNode) __value;
                // We need to drop the stamp since the types we see during OSR may be too
                // precise (if a branch was not parsed for example). In cases when this is
                // possible, we insert a guard and narrow the OSRLocal stamp at its usages.
                Stamp __narrowedStamp = __proxy.value().stamp(NodeView.DEFAULT);
                Stamp __unrestrictedStamp = __proxy.stamp(NodeView.DEFAULT).unrestricted();
                ValueNode __osrLocal;
                if (__i >= __localsSize)
                {
                    __osrLocal = __graph.addOrUnique(new OSRLockNode(__i - __localsSize, __unrestrictedStamp));
                }
                else
                {
                    __osrLocal = __graph.addOrUnique(new OSRLocalNode(__i, __unrestrictedStamp));
                }
                // Speculate on the OSRLocal stamps that could be more precise.
                OnStackReplacementPhase.OSRLocalSpeculationReason __reason = new OnStackReplacementPhase.OSRLocalSpeculationReason(__osrState.___bci, __narrowedStamp, __i);
                if (__graph.getSpeculationLog().maySpeculate(__reason) && __osrLocal instanceof OSRLocalNode && __value.getStackKind().equals(JavaKind.Object) && !__narrowedStamp.isUnrestricted())
                {
                    // Add guard.
                    LogicNode __check = __graph.addOrUniqueWithInputs(InstanceOfNode.createHelper((ObjectStamp) __narrowedStamp, __osrLocal, null, null));
                    JavaConstant __constant = __graph.getSpeculationLog().speculate(__reason);
                    FixedGuardNode __guard = __graph.add(new FixedGuardNode(__check, DeoptimizationReason.OptimizedTypeCheckViolated, DeoptimizationAction.InvalidateRecompile, __constant, false));
                    __graph.addAfterFixed(__osrStart, __guard);

                    // Replace with a more specific type at usages.
                    // We know that we are at the root, so we need to replace the proxy in the state.
                    __proxy.replaceAtMatchingUsages(__osrLocal, __n -> __n == __osrState);
                    __osrLocal = __graph.addOrUnique(new PiNode(__osrLocal, __narrowedStamp, __guard));
                }
                __proxy.replaceAndDelete(__osrLocal);
            }
        }

        __osr.replaceAtUsages(InputType.Guard, __osrStart);
        GraphUtil.killCFG(__start);
        new DeadCodeEliminationPhase(DeadCodeEliminationPhase.Optionality.Required).apply(__graph);

        if (__currentOSRWithLocks)
        {
            for (int __i = __osrState.monitorIdCount() - 1; __i >= 0; --__i)
            {
                MonitorIdNode __id = __osrState.monitorIdAt(__i);
                ValueNode __lockedObject = __osrState.lockAt(__i);
                OSRMonitorEnterNode __osrMonitorEnter = __graph.add(new OSRMonitorEnterNode(__lockedObject, __id));
                for (Node __usage : __id.usages())
                {
                    if (__usage instanceof AccessMonitorNode)
                    {
                        AccessMonitorNode __access = (AccessMonitorNode) __usage;
                        __access.setObject(__lockedObject);
                    }
                }
                FixedNode __oldNext = __osrStart.next();
                __oldNext.replaceAtPredecessor(null);
                __osrMonitorEnter.setNext(__oldNext);
                __osrStart.setNext(__osrMonitorEnter);
            }

            // Ensure balanced monitorenter - monitorexit
            //
            // Ensure that there is no monitor exit without a monitor enter in the graph. If there
            // is one this can only be done by bytecode as we have the monitor enter before the OSR
            // loop but the exit in a path of the loop that must be under a condition, else it will
            // throw an IllegalStateException anyway in the 2.iteration
            for (MonitorExitNode __exit : __graph.getNodes(MonitorExitNode.TYPE))
            {
                MonitorIdNode __id = __exit.getMonitorId();
                if (__id.usages().filter(MonitorEnterNode.class).count() != 1)
                {
                    throw new BailoutException("Unbalanced monitor enter-exit in OSR compilation with locks. Object is locked before the loop, but released inside the loop.");
                }
            }
        }
        new DeadCodeEliminationPhase(DeadCodeEliminationPhase.Optionality.Required).apply(__graph);
        // There must not be any parameter nodes left after OSR compilation.
    }

    private static EntryMarkerNode getEntryMarker(StructuredGraph __graph)
    {
        NodeIterable<EntryMarkerNode> __osrNodes = __graph.getNodes(EntryMarkerNode.TYPE);
        EntryMarkerNode __osr = __osrNodes.first();
        if (__osr == null)
        {
            throw new BailoutException("no OnStackReplacementNode generated");
        }
        if (__osrNodes.count() > 1)
        {
            throw new GraalError("multiple OnStackReplacementNodes generated");
        }
        if (__osr.stateAfter().stackSize() != 0)
        {
            throw new BailoutException("OSR with stack entries not supported");
        }
        return __osr;
    }

    private static LoopBeginNode osrLoop(EntryMarkerNode __osr)
    {
        // check that there is an OSR loop for the OSR begin
        LoopsData __loops = new LoopsData(__osr.graph());
        Loop<Block> __l = __loops.getCFG().getNodeToBlock().get(__osr).getLoop();
        if (__l == null)
        {
            return null;
        }
        return (LoopBeginNode) __l.getHeader().getBeginNode();
    }

    private static boolean osrWithLocks(EntryMarkerNode __osr)
    {
        return __osr.stateAfter().locksSize() != 0;
    }

    // @class OnStackReplacementPhase.OSRLocalSpeculationReason
    private static final class OSRLocalSpeculationReason implements SpeculationLog.SpeculationReason
    {
        // @field
        private int ___bci;
        // @field
        private Stamp ___speculatedStamp;
        // @field
        private int ___localIndex;

        // @cons OnStackReplacementPhase.OSRLocalSpeculationReason
        OSRLocalSpeculationReason(int __bci, Stamp __speculatedStamp, int __localIndex)
        {
            super();
            this.___bci = __bci;
            this.___speculatedStamp = __speculatedStamp;
            this.___localIndex = __localIndex;
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (__obj instanceof OnStackReplacementPhase.OSRLocalSpeculationReason)
            {
                OnStackReplacementPhase.OSRLocalSpeculationReason __that = (OnStackReplacementPhase.OSRLocalSpeculationReason) __obj;
                return this.___bci == __that.___bci && this.___speculatedStamp.equals(__that.___speculatedStamp) && this.___localIndex == __that.___localIndex;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return (this.___bci << 16) ^ this.___speculatedStamp.hashCode() ^ this.___localIndex;
        }
    }
}
