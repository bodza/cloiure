package giraaff.loop;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;

import jdk.vm.ci.meta.TriState;

import org.graalvm.collections.EconomicMap;

import giraaff.graph.Graph;
import giraaff.graph.Graph.DuplicationReplacement;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.GuardNode;
import giraaff.nodes.GuardPhiNode;
import giraaff.nodes.GuardProxyNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.ValueProxyNode;
import giraaff.nodes.VirtualState;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.java.MonitorEnterNode;
import giraaff.nodes.spi.NodeWithState;
import giraaff.nodes.virtual.CommitAllocationNode;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.util.GraalError;

// @class LoopFragment
public abstract class LoopFragment
{
    // @field
    private final LoopEx ___loop;
    // @field
    private final LoopFragment ___original;
    // @field
    protected NodeBitMap ___nodes;
    // @field
    protected boolean ___nodesReady;
    // @field
    private EconomicMap<Node, Node> ___duplicationMap;

    // @cons
    public LoopFragment(LoopEx __loop)
    {
        this(__loop, null);
        this.___nodesReady = true;
    }

    // @cons
    public LoopFragment(LoopEx __loop, LoopFragment __original)
    {
        super();
        this.___loop = __loop;
        this.___original = __original;
        this.___nodesReady = false;
    }

    ///
    // Return the original LoopEx for this fragment. For duplicated fragments this returns null.
    ///
    protected LoopEx loop()
    {
        return this.___loop;
    }

    public abstract LoopFragment duplicate();

    public abstract void insertBefore(LoopEx __l);

    public void disconnect()
    {
        // TODO possibly abstract
    }

    public boolean contains(Node __n)
    {
        return nodes().isMarkedAndGrow(__n);
    }

    @SuppressWarnings("unchecked")
    public <New extends Node, Old extends New> New getDuplicatedNode(Old __n)
    {
        return (New) this.___duplicationMap.get(__n);
    }

    protected <New extends Node, Old extends New> void putDuplicatedNode(Old __oldNode, New __newNode)
    {
        this.___duplicationMap.put(__oldNode, __newNode);
    }

    ///
    // Gets the corresponding value in this fragment. Should be called on duplicate fragments with a
    // node from the original fragment as argument.
    //
    // @param b original value
    // @return corresponding value in the peel
    ///
    protected abstract ValueNode prim(ValueNode __b);

    public boolean isDuplicate()
    {
        return this.___original != null;
    }

    public LoopFragment original()
    {
        return this.___original;
    }

    public abstract NodeBitMap nodes();

    public StructuredGraph graph()
    {
        LoopEx __l;
        if (isDuplicate())
        {
            __l = original().loop();
        }
        else
        {
            __l = loop();
        }
        return __l.loopBegin().graph();
    }

    protected abstract DuplicationReplacement getDuplicationReplacement();

    protected abstract void beforeDuplication();

    protected abstract void finishDuplication();

    protected void patchNodes(final DuplicationReplacement __dataFix)
    {
        if (isDuplicate() && !this.___nodesReady)
        {
            final DuplicationReplacement __cfgFix = original().getDuplicationReplacement();
            DuplicationReplacement __dr;
            if (__cfgFix == null && __dataFix != null)
            {
                __dr = __dataFix;
            }
            else if (__cfgFix != null && __dataFix == null)
            {
                __dr = __cfgFix;
            }
            else if (__cfgFix != null && __dataFix != null)
            {
                // @closure
                __dr = new DuplicationReplacement()
                {
                    @Override
                    public Node replacement(Node __o)
                    {
                        Node __r1 = __dataFix.replacement(__o);
                        if (__r1 != __o)
                        {
                            return __r1;
                        }
                        Node __r2 = __cfgFix.replacement(__o);
                        if (__r2 != __o)
                        {
                            return __r2;
                        }
                        return __o;
                    }
                };
            }
            else
            {
                __dr = null;
            }
            beforeDuplication();
            NodeIterable<Node> __nodesIterable = original().nodes();
            this.___duplicationMap = graph().addDuplicates(__nodesIterable, graph(), __nodesIterable.count(), __dr);
            finishDuplication();
            this.___nodes = new NodeBitMap(graph());
            this.___nodes.markAll(this.___duplicationMap.getValues());
            this.___nodesReady = true;
        }
        else
        {
            // TODO apply fix?
        }
    }

    protected static NodeBitMap computeNodes(Graph __graph, Iterable<AbstractBeginNode> __blocks)
    {
        return computeNodes(__graph, __blocks, Collections.emptyList());
    }

    protected static NodeBitMap computeNodes(Graph __graph, Iterable<AbstractBeginNode> __blocks, Iterable<AbstractBeginNode> __earlyExits)
    {
        final NodeBitMap __nodes = __graph.createNodeBitMap();
        computeNodes(__nodes, __graph, __blocks, __earlyExits);
        return __nodes;
    }

    protected static void computeNodes(NodeBitMap __nodes, Graph __graph, Iterable<AbstractBeginNode> __blocks, Iterable<AbstractBeginNode> __earlyExits)
    {
        for (AbstractBeginNode __b : __blocks)
        {
            if (__b.isDeleted())
            {
                continue;
            }

            for (Node __n : __b.getBlockNodes())
            {
                if (__n instanceof Invoke)
                {
                    __nodes.mark(((Invoke) __n).callTarget());
                }
                if (__n instanceof NodeWithState)
                {
                    NodeWithState __withState = (NodeWithState) __n;
                    __withState.states().forEach(__state -> __state.applyToVirtual(__node -> __nodes.mark(__node)));
                }
                if (__n instanceof AbstractMergeNode)
                {
                    // if a merge is in the loop, all of its phis are also in the loop
                    for (PhiNode __phi : ((AbstractMergeNode) __n).phis())
                    {
                        __nodes.mark(__phi);
                    }
                }
                __nodes.mark(__n);
            }
        }
        for (AbstractBeginNode __earlyExit : __earlyExits)
        {
            if (__earlyExit.isDeleted())
            {
                continue;
            }

            __nodes.mark(__earlyExit);

            if (__earlyExit instanceof LoopExitNode)
            {
                LoopExitNode __loopExit = (LoopExitNode) __earlyExit;
                FrameState __stateAfter = __loopExit.stateAfter();
                if (__stateAfter != null)
                {
                    __stateAfter.applyToVirtual(__node -> __nodes.mark(__node));
                }
                for (ProxyNode __proxy : __loopExit.proxies())
                {
                    __nodes.mark(__proxy);
                }
            }
        }

        final NodeBitMap __nonLoopNodes = __graph.createNodeBitMap();
        Deque<WorkListEntry> __worklist = new ArrayDeque<>();
        for (AbstractBeginNode __b : __blocks)
        {
            if (__b.isDeleted())
            {
                continue;
            }

            for (Node __n : __b.getBlockNodes())
            {
                if (__n instanceof CommitAllocationNode)
                {
                    for (VirtualObjectNode __obj : ((CommitAllocationNode) __n).getVirtualObjects())
                    {
                        markFloating(__worklist, __obj, __nodes, __nonLoopNodes);
                    }
                }
                if (__n instanceof MonitorEnterNode)
                {
                    markFloating(__worklist, ((MonitorEnterNode) __n).getMonitorId(), __nodes, __nonLoopNodes);
                }
                if (__n instanceof AbstractMergeNode)
                {
                    // Since we already marked all phi nodes as being in the loop to break cycles,
                    // we also have to iterate over their usages here.
                    for (PhiNode __phi : ((AbstractMergeNode) __n).phis())
                    {
                        for (Node __usage : __phi.usages())
                        {
                            markFloating(__worklist, __usage, __nodes, __nonLoopNodes);
                        }
                    }
                }
                for (Node __usage : __n.usages())
                {
                    markFloating(__worklist, __usage, __nodes, __nonLoopNodes);
                }
            }
        }
    }

    // @class LoopFragment.WorkListEntry
    static final class WorkListEntry
    {
        // @field
        final Iterator<Node> ___usages;
        // @field
        final Node ___n;
        // @field
        boolean ___isLoopNode;

        // @cons
        WorkListEntry(Node __n, NodeBitMap __loopNodes)
        {
            super();
            this.___n = __n;
            this.___usages = __n.usages().iterator();
            this.___isLoopNode = __loopNodes.isMarked(__n);
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (!(__obj instanceof WorkListEntry))
            {
                return false;
            }
            WorkListEntry __other = (WorkListEntry) __obj;
            return this.___n == __other.___n;
        }

        @Override
        public int hashCode()
        {
            return this.___n.hashCode();
        }
    }

    static TriState isLoopNode(Node __n, NodeBitMap __loopNodes, NodeBitMap __nonLoopNodes)
    {
        if (__loopNodes.isMarked(__n))
        {
            return TriState.TRUE;
        }
        if (__nonLoopNodes.isMarked(__n))
        {
            return TriState.FALSE;
        }
        if (__n instanceof FixedNode || __n instanceof PhiNode)
        {
            // phi nodes are treated the same as fixed nodes in this algorithm to break cycles
            return TriState.FALSE;
        }
        return TriState.UNKNOWN;
    }

    private static void markFloating(Deque<WorkListEntry> __workList, Node __start, NodeBitMap __loopNodes, NodeBitMap __nonLoopNodes)
    {
        if (isLoopNode(__start, __loopNodes, __nonLoopNodes).isKnown())
        {
            return;
        }
        __workList.push(new WorkListEntry(__start, __loopNodes));
        while (!__workList.isEmpty())
        {
            WorkListEntry __currentEntry = __workList.peek();
            if (__currentEntry.___usages.hasNext())
            {
                Node __current = __currentEntry.___usages.next();
                TriState __result = isLoopNode(__current, __loopNodes, __nonLoopNodes);
                if (__result.isKnown())
                {
                    if (__result.toBoolean())
                    {
                        __currentEntry.___isLoopNode = true;
                    }
                }
                else
                {
                    __workList.push(new WorkListEntry(__current, __loopNodes));
                }
            }
            else
            {
                __workList.pop();
                boolean __isLoopNode = __currentEntry.___isLoopNode;
                Node __current = __currentEntry.___n;
                if (!__isLoopNode && __current instanceof GuardNode)
                {
                    // (gd) this is only OK if we are not going to make loop transforms based on this
                    __isLoopNode = true;
                }
                if (__isLoopNode)
                {
                    __loopNodes.mark(__current);
                    for (WorkListEntry __e : __workList)
                    {
                        __e.___isLoopNode = true;
                    }
                }
                else
                {
                    __nonLoopNodes.mark(__current);
                }
            }
        }
    }

    public static NodeIterable<AbstractBeginNode> toHirBlocks(final Iterable<Block> __blocks)
    {
        // @closure
        return new NodeIterable<AbstractBeginNode>()
        {
            @Override
            public Iterator<AbstractBeginNode> iterator()
            {
                final Iterator<Block> __it = __blocks.iterator();
                // @closure
                return new Iterator<AbstractBeginNode>()
                {
                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public AbstractBeginNode next()
                    {
                        return __it.next().getBeginNode();
                    }

                    @Override
                    public boolean hasNext()
                    {
                        return __it.hasNext();
                    }
                };
            }
        };
    }

    public static NodeIterable<AbstractBeginNode> toHirExits(final Iterable<Block> __blocks)
    {
        // @closure
        return new NodeIterable<AbstractBeginNode>()
        {
            @Override
            public Iterator<AbstractBeginNode> iterator()
            {
                final Iterator<Block> __it = __blocks.iterator();
                // @closure
                return new Iterator<AbstractBeginNode>()
                {
                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }

                    ///
                    // Return the true LoopExitNode for this loop or the BeginNode for the block.
                    ///
                    @Override
                    public AbstractBeginNode next()
                    {
                        Block __next = __it.next();
                        LoopExitNode __exit = __next.getLoopExit();
                        if (__exit != null)
                        {
                            return __exit;
                        }
                        return __next.getBeginNode();
                    }

                    @Override
                    public boolean hasNext()
                    {
                        return __it.hasNext();
                    }
                };
            }
        };
    }

    ///
    // Merges the early exits (i.e. loop exits) that were duplicated as part of this fragment, with
    // the original fragment's exits.
    ///
    protected void mergeEarlyExits()
    {
        StructuredGraph __graph = graph();
        for (AbstractBeginNode __earlyExit : LoopFragment.toHirBlocks(original().loop().loop().getExits()))
        {
            LoopExitNode __loopEarlyExit = (LoopExitNode) __earlyExit;
            FixedNode __next = __loopEarlyExit.next();
            if (__loopEarlyExit.isDeleted() || !this.original().contains(__loopEarlyExit))
            {
                continue;
            }
            AbstractBeginNode __newEarlyExit = getDuplicatedNode(__loopEarlyExit);
            if (__newEarlyExit == null)
            {
                continue;
            }
            MergeNode __merge = __graph.add(new MergeNode());
            EndNode __originalEnd = __graph.add(new EndNode());
            EndNode __newEnd = __graph.add(new EndNode());
            __merge.addForwardEnd(__originalEnd);
            __merge.addForwardEnd(__newEnd);
            __loopEarlyExit.setNext(__originalEnd);
            __newEarlyExit.setNext(__newEnd);
            __merge.setNext(__next);

            FrameState __exitState = __loopEarlyExit.stateAfter();
            if (__exitState != null)
            {
                FrameState __originalExitState = __exitState;
                __exitState = __exitState.duplicateWithVirtualState();
                __loopEarlyExit.setStateAfter(__exitState);
                __merge.setStateAfter(__originalExitState);
                // Using the old exit's state as the merge's state is necessary because some of the VirtualState
                // nodes contained in the old exit's state may be shared by other dominated VirtualStates.
                // Those dominated virtual states need to see the proxy->phi update that are applied below.
                //
                // We now update the original fragment's nodes accordingly:
                __originalExitState.applyToVirtual(__node -> this.___original.___nodes.clearAndGrow(__node));
                __exitState.applyToVirtual(__node -> this.___original.___nodes.markAndGrow(__node));
            }
            FrameState __finalExitState = __exitState;

            for (Node __anchored : __loopEarlyExit.anchored().snapshot())
            {
                __anchored.replaceFirstInput(__loopEarlyExit, __merge);
            }

            boolean __newEarlyExitIsLoopExit = __newEarlyExit instanceof LoopExitNode;
            for (ProxyNode __vpn : __loopEarlyExit.proxies().snapshot())
            {
                if (__vpn.hasNoUsages())
                {
                    continue;
                }
                if (__vpn.value() == null)
                {
                    __vpn.replaceAtUsages(null);
                    continue;
                }
                final ValueNode __replaceWith;
                ValueNode __newVpn = prim(__newEarlyExitIsLoopExit ? __vpn : __vpn.value());
                if (__newVpn != null)
                {
                    PhiNode __phi;
                    if (__vpn instanceof ValueProxyNode)
                    {
                        __phi = __graph.addWithoutUnique(new ValuePhiNode(__vpn.stamp(NodeView.DEFAULT), __merge));
                    }
                    else if (__vpn instanceof GuardProxyNode)
                    {
                        __phi = __graph.addWithoutUnique(new GuardPhiNode(__merge));
                    }
                    else
                    {
                        throw GraalError.shouldNotReachHere();
                    }
                    __phi.addInput(__vpn);
                    __phi.addInput(__newVpn);
                    __replaceWith = __phi;
                }
                else
                {
                    __replaceWith = __vpn.value();
                }
                __vpn.replaceAtMatchingUsages(__replaceWith, __usage ->
                {
                    if (__merge.isPhiAtMerge(__usage))
                    {
                        return false;
                    }
                    if (__usage instanceof VirtualState)
                    {
                        VirtualState __stateUsage = (VirtualState) __usage;
                        if (__finalExitState != null && __finalExitState.isPartOfThisState(__stateUsage))
                        {
                            return false;
                        }
                    }
                    return true;
                });
            }
        }
    }
}
