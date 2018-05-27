package giraaff.hotspot.phases;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.SpeculationLog;
import jdk.vm.ci.runtime.JVMCICompiler;

import giraaff.core.common.PermanentBailoutException;
import giraaff.core.common.cfg.Loop;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.iterators.NodeIterable;
import giraaff.loop.LoopsData;
import giraaff.loop.phases.LoopTransformations;
import giraaff.nodeinfo.InputType;
import giraaff.nodeinfo.Verbosity;
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
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;
import giraaff.phases.Phase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.util.GraalError;

public class OnStackReplacementPhase extends Phase
{
    public static class Options
    {
        // @Option "Deoptimize OSR compiled code when the OSR entry loop is finished if there is no mature profile available for the rest of the method."
        public static final OptionKey<Boolean> DeoptAfterOSR = new OptionKey<>(true);
        // @Option "Support OSR compilations with locks. If DeoptAfterOSR is true we can per definition not have unbalaced enter/extis mappings. If DeoptAfterOSR is false insert artificial monitor enters after the OSRStart to have balanced enter/exits in the graph."
        public static final OptionKey<Boolean> SupportOSRWithLocks = new OptionKey<>(true);
    }

    private static boolean supportOSRWithLocks(OptionValues options)
    {
        return Options.SupportOSRWithLocks.getValue(options);
    }

    @Override
    protected void run(StructuredGraph graph)
    {
        if (graph.getEntryBCI() == JVMCICompiler.INVOCATION_ENTRY_BCI)
        {
            // This happens during inlining in a OSR method, because the same phase plan will be used.
            return;
        }

        EntryMarkerNode osr;
        int maxIterations = -1;
        int iterations = 0;

        final EntryMarkerNode originalOSRNode = getEntryMarker(graph);
        final LoopBeginNode originalOSRLoop = osrLoop(originalOSRNode);
        final boolean currentOSRWithLocks = osrWithLocks(originalOSRNode);

        if (originalOSRLoop == null)
        {
            /*
             * OSR with Locks: We do not have an OSR loop for the original OSR bci. Therefore we
             * cannot decide where to deopt and which framestate will be used. In the worst case
             * the framestate of the OSR entry would be used.
             */
            throw new PermanentBailoutException("OSR compilation without OSR entry loop.");
        }

        if (!supportOSRWithLocks(graph.getOptions()) && currentOSRWithLocks)
        {
            throw new PermanentBailoutException("OSR with locks disabled.");
        }

        while (true)
        {
            osr = getEntryMarker(graph);
            LoopsData loops = new LoopsData(graph);
            // find the loop that contains the EntryMarker
            Loop<Block> l = loops.getCFG().getNodeToBlock().get(osr).getLoop();
            if (l == null)
            {
                break;
            }

            iterations++;
            if (maxIterations == -1)
            {
                maxIterations = l.getDepth();
            }
            else if (iterations > maxIterations)
            {
                throw GraalError.shouldNotReachHere();
            }
            // peel the outermost loop first
            while (l.getParent() != null)
            {
                l = l.getParent();
            }

            LoopTransformations.peel(loops.loop(l));
            osr.replaceAtUsages(InputType.Guard, AbstractBeginNode.prevBegin((FixedNode) osr.predecessor()));
            for (Node usage : osr.usages().snapshot())
            {
                EntryProxyNode proxy = (EntryProxyNode) usage;
                proxy.replaceAndDelete(proxy.value());
            }
            GraphUtil.removeFixedWithUnusedInputs(osr);
        }

        StartNode start = graph.start();
        FrameState osrState = osr.stateAfter();
        osr.setStateAfter(null);
        OSRStartNode osrStart = graph.add(new OSRStartNode());
        FixedNode next = osr.next();
        osr.setNext(null);
        osrStart.setNext(next);
        graph.setStart(osrStart);
        osrStart.setStateAfter(osrState);

        final int localsSize = osrState.localsSize();
        final int locksSize = osrState.locksSize();

        for (int i = 0; i < localsSize + locksSize; i++)
        {
            ValueNode value = null;
            if (i >= localsSize)
            {
                value = osrState.lockAt(i - localsSize);
            }
            else
            {
                value = osrState.localAt(i);
            }
            if (value instanceof EntryProxyNode)
            {
                EntryProxyNode proxy = (EntryProxyNode) value;
                /*
                 * We need to drop the stamp since the types we see during OSR may be too
                 * precise (if a branch was not parsed for example). In cases when this is
                 * possible, we insert a guard and narrow the OSRLocal stamp at its usages.
                 */
                Stamp narrowedStamp = proxy.value().stamp(NodeView.DEFAULT);
                Stamp unrestrictedStamp = proxy.stamp(NodeView.DEFAULT).unrestricted();
                ValueNode osrLocal;
                if (i >= localsSize)
                {
                    osrLocal = graph.addOrUnique(new OSRLockNode(i - localsSize, unrestrictedStamp));
                }
                else
                {
                    osrLocal = graph.addOrUnique(new OSRLocalNode(i, unrestrictedStamp));
                }
                // Speculate on the OSRLocal stamps that could be more precise.
                OSRLocalSpeculationReason reason = new OSRLocalSpeculationReason(osrState.bci, narrowedStamp, i);
                if (graph.getSpeculationLog().maySpeculate(reason) && osrLocal instanceof OSRLocalNode && value.getStackKind().equals(JavaKind.Object) && !narrowedStamp.isUnrestricted())
                {
                    // Add guard.
                    LogicNode check = graph.addOrUniqueWithInputs(InstanceOfNode.createHelper((ObjectStamp) narrowedStamp, osrLocal, null, null));
                    JavaConstant constant = graph.getSpeculationLog().speculate(reason);
                    FixedGuardNode guard = graph.add(new FixedGuardNode(check, DeoptimizationReason.OptimizedTypeCheckViolated, DeoptimizationAction.InvalidateRecompile, constant, false));
                    graph.addAfterFixed(osrStart, guard);

                    // Replace with a more specific type at usages.
                    // We know that we are at the root, so we need to replace the proxy in the state.
                    proxy.replaceAtMatchingUsages(osrLocal, n -> n == osrState);
                    osrLocal = graph.addOrUnique(new PiNode(osrLocal, narrowedStamp, guard));
                }
                proxy.replaceAndDelete(osrLocal);
            }
        }

        osr.replaceAtUsages(InputType.Guard, osrStart);
        GraphUtil.killCFG(start);
        new DeadCodeEliminationPhase(Optionality.Required).apply(graph);

        if (currentOSRWithLocks)
        {
            for (int i = osrState.monitorIdCount() - 1; i >= 0; --i)
            {
                MonitorIdNode id = osrState.monitorIdAt(i);
                ValueNode lockedObject = osrState.lockAt(i);
                OSRMonitorEnterNode osrMonitorEnter = graph.add(new OSRMonitorEnterNode(lockedObject, id));
                for (Node usage : id.usages())
                {
                    if (usage instanceof AccessMonitorNode)
                    {
                        AccessMonitorNode access = (AccessMonitorNode) usage;
                        access.setObject(lockedObject);
                    }
                }
                FixedNode oldNext = osrStart.next();
                oldNext.replaceAtPredecessor(null);
                osrMonitorEnter.setNext(oldNext);
                osrStart.setNext(osrMonitorEnter);
            }

            /*
             * Ensure balanced monitorenter - monitorexit
             *
             * Ensure that there is no monitor exit without a monitor enter in the graph. If there
             * is one this can only be done by bytecode as we have the monitor enter before the OSR
             * loop but the exit in a path of the loop that must be under a condition, else it will
             * throw an IllegalStateException anyway in the 2.iteration
             */
            for (MonitorExitNode exit : graph.getNodes(MonitorExitNode.TYPE))
            {
                MonitorIdNode id = exit.getMonitorId();
                if (id.usages().filter(MonitorEnterNode.class).count() != 1)
                {
                    throw new PermanentBailoutException("Unbalanced monitor enter-exit in OSR compilation with locks. Object is locked before the loop but released inside the loop.");
                }
            }
        }
        new DeadCodeEliminationPhase(Optionality.Required).apply(graph);
        // There must not be any parameter nodes left after OSR compilation.
    }

    private static EntryMarkerNode getEntryMarker(StructuredGraph graph)
    {
        NodeIterable<EntryMarkerNode> osrNodes = graph.getNodes(EntryMarkerNode.TYPE);
        EntryMarkerNode osr = osrNodes.first();
        if (osr == null)
        {
            throw new PermanentBailoutException("No OnStackReplacementNode generated");
        }
        if (osrNodes.count() > 1)
        {
            throw new GraalError("Multiple OnStackReplacementNodes generated");
        }
        if (osr.stateAfter().stackSize() != 0)
        {
            throw new PermanentBailoutException("OSR with stack entries not supported: %s", osr.stateAfter().toString(Verbosity.Debugger));
        }
        return osr;
    }

    private static LoopBeginNode osrLoop(EntryMarkerNode osr)
    {
        // check that there is an OSR loop for the OSR begin
        LoopsData loops = new LoopsData(osr.graph());
        Loop<Block> l = loops.getCFG().getNodeToBlock().get(osr).getLoop();
        if (l == null)
        {
            return null;
        }
        return (LoopBeginNode) l.getHeader().getBeginNode();
    }

    private static boolean osrWithLocks(EntryMarkerNode osr)
    {
        return osr.stateAfter().locksSize() != 0;
    }

    private static class OSRLocalSpeculationReason implements SpeculationLog.SpeculationReason
    {
        private int bci;
        private Stamp speculatedStamp;
        private int localIndex;

        OSRLocalSpeculationReason(int bci, Stamp speculatedStamp, int localIndex)
        {
            this.bci = bci;
            this.speculatedStamp = speculatedStamp;
            this.localIndex = localIndex;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof OSRLocalSpeculationReason)
            {
                OSRLocalSpeculationReason that = (OSRLocalSpeculationReason) obj;
                return this.bci == that.bci && this.speculatedStamp.equals(that.speculatedStamp) && this.localIndex == that.localIndex;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return (bci << 16) ^ speculatedStamp.hashCode() ^ localIndex;
        }
    }
}
