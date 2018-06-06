package giraaff.nodes.cfg;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import giraaff.core.common.cfg.AbstractControlFlowGraph;
import giraaff.core.common.cfg.Loop;
import giraaff.graph.Node;
import giraaff.graph.NodeMap;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.StructuredGraph;
import giraaff.util.GraalError;

// @class ControlFlowGraph
public final class ControlFlowGraph implements AbstractControlFlowGraph<Block>
{
    ///
    // Don't allow probability values to be become too small or too high as this makes frequency
    // calculations over- or underflow the range of a double. This commonly happens with infinite
    // loops within infinite loops. The value is chosen a bit lower than half the maximum exponent
    // supported by double. That way we can never overflow to infinity when multiplying two
    // probability values.
    ///
    // @def
    public static final double MIN_PROBABILITY = 0x1.0p-500;
    // @def
    public static final double MAX_PROBABILITY = 1 / MIN_PROBABILITY;

    // @field
    public final StructuredGraph ___graph;

    // @field
    private NodeMap<Block> ___nodeToBlock;
    // @field
    private Block[] ___reversePostOrder;
    // @field
    private List<Loop<Block>> ___loops;
    // @field
    private int ___maxDominatorDepth;

    // @iface ControlFlowGraph.RecursiveVisitor
    public interface RecursiveVisitor<V>
    {
        V enter(Block __b);

        void exit(Block __b, V __value);
    }

    public static ControlFlowGraph compute(StructuredGraph __graph, boolean __connectBlocks, boolean __computeLoops, boolean __computeDominators, boolean __computePostdominators)
    {
        ControlFlowGraph __cfg = new ControlFlowGraph(__graph);
        __cfg.identifyBlocks();
        __cfg.computeProbabilities();

        if (__computeLoops)
        {
            __cfg.computeLoopInformation();
        }
        if (__computeDominators)
        {
            __cfg.computeDominators();
        }
        if (__computePostdominators)
        {
            __cfg.computePostdominators();
        }

        return __cfg;
    }

    @SuppressWarnings("unchecked")
    public <V> void visitDominatorTreeDefault(ControlFlowGraph.RecursiveVisitor<V> __visitor)
    {
        Block[] __stack = new Block[this.___maxDominatorDepth + 1];
        Block __current = getStartBlock();
        int __tos = 0;
        Object[] __values = null;
        int __valuesTOS = 0;

        while (__tos >= 0)
        {
            Block __state = __stack[__tos];
            if (__state == null || __state.getDominator() == null || __state.getDominator().getPostdominator() != __state)
            {
                if (__state == null)
                {
                    // We enter this block for the first time.
                    V __value = __visitor.enter(__current);
                    if (__value != null || __values != null)
                    {
                        if (__values == null)
                        {
                            __values = new Object[this.___maxDominatorDepth + 1];
                        }
                        __values[__valuesTOS++] = __value;
                    }

                    Block __dominated = skipPostDom(__current.getFirstDominated());
                    if (__dominated != null)
                    {
                        // Descend into dominated.
                        __stack[__tos] = __dominated;
                        __current = __dominated;
                        __stack[++__tos] = null;
                        continue;
                    }
                }
                else
                {
                    Block __next = skipPostDom(__state.getDominatedSibling());
                    if (__next != null)
                    {
                        // Descend into dominated.
                        __stack[__tos] = __next;
                        __current = __next;
                        __stack[++__tos] = null;
                        continue;
                    }
                }

                // Finished processing all normal dominators.
                Block __postDom = __current.getPostdominator();
                if (__postDom != null && __postDom.getDominator() == __current)
                {
                    // Descend into post dominator.
                    __stack[__tos] = __postDom;
                    __current = __postDom;
                    __stack[++__tos] = null;
                    continue;
                }
            }

            // Finished processing this node, exit and pop from stack.
            V __value = null;
            if (__values != null && __valuesTOS > 0)
            {
                __value = (V) __values[--__valuesTOS];
            }
            __visitor.exit(__current, __value);
            __current = __current.getDominator();
            --__tos;
        }
    }

    private static Block skipPostDom(Block __block)
    {
        if (__block != null && __block.getDominator().getPostdominator() == __block)
        {
            // This is an always reached block.
            return __block.getDominatedSibling();
        }
        return __block;
    }

    // @class ControlFlowGraph.DeferredExit
    public static final class DeferredExit
    {
        // @cons ControlFlowGraph.DeferredExit
        public DeferredExit(Block __block, ControlFlowGraph.DeferredExit __next)
        {
            super();
            this.___block = __block;
            this.___next = __next;
        }

        // @field
        private final Block ___block;
        // @field
        private final ControlFlowGraph.DeferredExit ___next;

        public Block getBlock()
        {
            return this.___block;
        }

        public ControlFlowGraph.DeferredExit getNext()
        {
            return this.___next;
        }
    }

    public static void addDeferredExit(ControlFlowGraph.DeferredExit[] __deferredExits, Block __b)
    {
        Loop<Block> __outermostExited = __b.getDominator().getLoop();
        Loop<Block> __exitBlockLoop = __b.getLoop();
        while (__outermostExited.getParent() != null && __outermostExited.getParent() != __exitBlockLoop)
        {
            __outermostExited = __outermostExited.getParent();
        }
        int __loopIndex = __outermostExited.getIndex();
        __deferredExits[__loopIndex] = new ControlFlowGraph.DeferredExit(__b, __deferredExits[__loopIndex]);
    }

    @SuppressWarnings({"unchecked"})
    public <V> void visitDominatorTreeDeferLoopExits(ControlFlowGraph.RecursiveVisitor<V> __visitor)
    {
        Block[] __stack = new Block[getBlocks().length];
        int __tos = 0;
        BitSet __visited = new BitSet(getBlocks().length);
        int __loopCount = getLoops().size();
        ControlFlowGraph.DeferredExit[] __deferredExits = new ControlFlowGraph.DeferredExit[__loopCount];
        Object[] __values = null;
        int __valuesTOS = 0;
        __stack[0] = getStartBlock();

        while (__tos >= 0)
        {
            Block __cur = __stack[__tos];
            int __curId = __cur.getId();
            if (__visited.get(__curId))
            {
                V __value = null;
                if (__values != null && __valuesTOS > 0)
                {
                    __value = (V) __values[--__valuesTOS];
                }
                __visitor.exit(__cur, __value);
                --__tos;
                if (__cur.isLoopHeader())
                {
                    int __loopIndex = __cur.getLoop().getIndex();
                    ControlFlowGraph.DeferredExit __deferredExit = __deferredExits[__loopIndex];
                    if (__deferredExit != null)
                    {
                        while (__deferredExit != null)
                        {
                            __stack[++__tos] = __deferredExit.___block;
                            __deferredExit = __deferredExit.___next;
                        }
                        __deferredExits[__loopIndex] = null;
                    }
                }
            }
            else
            {
                __visited.set(__curId);
                V __value = __visitor.enter(__cur);
                if (__value != null || __values != null)
                {
                    if (__values == null)
                    {
                        __values = new Object[this.___maxDominatorDepth + 1];
                    }
                    __values[__valuesTOS++] = __value;
                }

                Block __alwaysReached = __cur.getPostdominator();
                if (__alwaysReached != null)
                {
                    if (__alwaysReached.getDominator() != __cur)
                    {
                        __alwaysReached = null;
                    }
                    else if (isDominatorTreeLoopExit(__alwaysReached))
                    {
                        addDeferredExit(__deferredExits, __alwaysReached);
                    }
                    else
                    {
                        __stack[++__tos] = __alwaysReached;
                    }
                }

                Block __b = __cur.getFirstDominated();
                while (__b != null)
                {
                    if (__b != __alwaysReached)
                    {
                        if (isDominatorTreeLoopExit(__b))
                        {
                            addDeferredExit(__deferredExits, __b);
                        }
                        else
                        {
                            __stack[++__tos] = __b;
                        }
                    }
                    __b = __b.getDominatedSibling();
                }
            }
        }
    }

    public <V> void visitDominatorTree(ControlFlowGraph.RecursiveVisitor<V> __visitor, boolean __deferLoopExits)
    {
        if (__deferLoopExits && this.getLoops().size() > 0)
        {
            visitDominatorTreeDeferLoopExits(__visitor);
        }
        else
        {
            visitDominatorTreeDefault(__visitor);
        }
    }

    public static boolean isDominatorTreeLoopExit(Block __b)
    {
        Block __dominator = __b.getDominator();
        return __dominator != null && __b.getLoop() != __dominator.getLoop() && (!__b.isLoopHeader() || __dominator.getLoopDepth() >= __b.getLoopDepth());
    }

    // @cons ControlFlowGraph
    private ControlFlowGraph(StructuredGraph __graph)
    {
        super();
        this.___graph = __graph;
        this.___nodeToBlock = __graph.createNodeMap();
    }

    private void computeDominators()
    {
        Block[] __blocks = this.___reversePostOrder;
        int __curMaxDominatorDepth = 0;
        for (int __i = 1; __i < __blocks.length; __i++)
        {
            Block __block = __blocks[__i];
            Block __dominator = null;
            for (Block __pred : __block.getPredecessors())
            {
                if (!__pred.isLoopEnd())
                {
                    __dominator = ((__dominator == null) ? __pred : commonDominatorRaw(__dominator, __pred));
                }
            }

            // Set dominator.
            __block.setDominator(__dominator);

            // Keep dominated linked list sorted by block ID such that predecessor blocks are always
            // before successor blocks.
            Block __currentDominated = __dominator.getFirstDominated();
            if (__currentDominated != null && __currentDominated.getId() < __block.getId())
            {
                while (__currentDominated.getDominatedSibling() != null && __currentDominated.getDominatedSibling().getId() < __block.getId())
                {
                    __currentDominated = __currentDominated.getDominatedSibling();
                }
                __block.setDominatedSibling(__currentDominated.getDominatedSibling());
                __currentDominated.setDominatedSibling(__block);
            }
            else
            {
                __block.setDominatedSibling(__dominator.getFirstDominated());
                __dominator.setFirstDominated(__block);
            }

            __curMaxDominatorDepth = Math.max(__curMaxDominatorDepth, __block.getDominatorDepth());
        }
        this.___maxDominatorDepth = __curMaxDominatorDepth;
        calcDominatorRanges(getStartBlock(), this.___reversePostOrder.length);
    }

    private static void calcDominatorRanges(Block __block, int __size)
    {
        Block[] __stack = new Block[__size];
        __stack[0] = __block;
        int __tos = 0;
        int __myNumber = 0;

        do
        {
            Block __cur = __stack[__tos];
            Block __dominated = __cur.getFirstDominated();

            if (__cur.getDominatorNumber() == -1)
            {
                __cur.setDominatorNumber(__myNumber);
                if (__dominated != null)
                {
                    // Push children onto stack.
                    do
                    {
                        __stack[++__tos] = __dominated;
                        __dominated = __dominated.getDominatedSibling();
                    } while (__dominated != null);
                }
                else
                {
                    __cur.setMaxChildDomNumber(__myNumber);
                    --__tos;
                }
                ++__myNumber;
            }
            else
            {
                __cur.setMaxChildDomNumber(__dominated.getMaxChildDominatorNumber());
                --__tos;
            }
        } while (__tos >= 0);
    }

    private static Block commonDominatorRaw(Block __a, Block __b)
    {
        int __aDomDepth = __a.getDominatorDepth();
        int __bDomDepth = __b.getDominatorDepth();
        if (__aDomDepth > __bDomDepth)
        {
            return commonDominatorRawSameDepth(__a.getDominator(__aDomDepth - __bDomDepth), __b);
        }
        else
        {
            return commonDominatorRawSameDepth(__a, __b.getDominator(__bDomDepth - __aDomDepth));
        }
    }

    private static Block commonDominatorRawSameDepth(Block __a, Block __b)
    {
        Block __iterA = __a;
        Block __iterB = __b;
        while (__iterA != __iterB)
        {
            __iterA = __iterA.getDominator();
            __iterB = __iterB.getDominator();
        }
        return __iterA;
    }

    @Override
    public Block[] getBlocks()
    {
        return this.___reversePostOrder;
    }

    @Override
    public Block getStartBlock()
    {
        return this.___reversePostOrder[0];
    }

    public Block[] reversePostOrder()
    {
        return this.___reversePostOrder;
    }

    public NodeMap<Block> getNodeToBlock()
    {
        return this.___nodeToBlock;
    }

    public Block blockFor(Node __node)
    {
        return this.___nodeToBlock.get(__node);
    }

    @Override
    public List<Loop<Block>> getLoops()
    {
        return this.___loops;
    }

    public int getMaxDominatorDepth()
    {
        return this.___maxDominatorDepth;
    }

    private void identifyBlock(Block __block)
    {
        FixedWithNextNode __cur = __block.getBeginNode();
        while (true)
        {
            this.___nodeToBlock.set(__cur, __block);
            FixedNode __next = __cur.next();
            if (__next instanceof AbstractBeginNode)
            {
                __block.___endNode = __cur;
                return;
            }
            else if (__next instanceof FixedWithNextNode)
            {
                __cur = (FixedWithNextNode) __next;
            }
            else
            {
                this.___nodeToBlock.set(__next, __block);
                __block.___endNode = __next;
                return;
            }
        }
    }

    ///
    // Identify and connect blocks (including loop backward edges). Predecessors need to be in the
    // order expected when iterating phi inputs.
    ///
    private void identifyBlocks()
    {
        // Find all block headers.
        int __numBlocks = 0;
        for (AbstractBeginNode __begin : this.___graph.getNodes(AbstractBeginNode.TYPE))
        {
            Block __block = new Block(__begin);
            identifyBlock(__block);
            __numBlocks++;
        }

        // Compute reverse post order.
        int __count = 0;
        NodeMap<Block> __nodeMap = this.___nodeToBlock;
        Block[] __stack = new Block[__numBlocks];
        int __tos = 0;
        Block __startBlock = blockFor(this.___graph.start());
        __stack[0] = __startBlock;
        __startBlock.setPredecessors(Block.EMPTY_ARRAY);
        do
        {
            Block __block = __stack[__tos];
            int __id = __block.getId();
            if (__id == AbstractControlFlowGraph.BLOCK_ID_INITIAL)
            {
                // First time we see this block: push all successors.
                FixedNode __last = __block.getEndNode();
                if (__last instanceof EndNode)
                {
                    EndNode __endNode = (EndNode) __last;
                    Block __suxBlock = __nodeMap.get(__endNode.merge());
                    if (__suxBlock.getId() == AbstractControlFlowGraph.BLOCK_ID_INITIAL)
                    {
                        __stack[++__tos] = __suxBlock;
                    }
                    __block.setSuccessors(new Block[] { __suxBlock });
                }
                else if (__last instanceof IfNode)
                {
                    IfNode __ifNode = (IfNode) __last;
                    Block __trueSucc = __nodeMap.get(__ifNode.trueSuccessor());
                    __stack[++__tos] = __trueSucc;
                    Block __falseSucc = __nodeMap.get(__ifNode.falseSuccessor());
                    __stack[++__tos] = __falseSucc;
                    __block.setSuccessors(new Block[] { __trueSucc, __falseSucc });
                    Block[] __ifPred = new Block[] { __block };
                    __trueSucc.setPredecessors(__ifPred);
                    __falseSucc.setPredecessors(__ifPred);
                }
                else if (__last instanceof LoopEndNode)
                {
                    LoopEndNode __loopEndNode = (LoopEndNode) __last;
                    __block.setSuccessors(new Block[] { __nodeMap.get(__loopEndNode.loopBegin()) });
                    // Nothing to do push onto the stack.
                }
                else if (__last instanceof ControlSinkNode)
                {
                    __block.setSuccessors(Block.EMPTY_ARRAY);
                }
                else
                {
                    int __startTos = __tos;
                    Block[] __ifPred = new Block[] { __block };
                    for (Node __suxNode : __last.successors())
                    {
                        Block __sux = __nodeMap.get(__suxNode);
                        __stack[++__tos] = __sux;
                        __sux.setPredecessors(__ifPred);
                    }
                    int __suxCount = __tos - __startTos;
                    Block[] __successors = new Block[__suxCount];
                    System.arraycopy(__stack, __startTos + 1, __successors, 0, __suxCount);
                    __block.setSuccessors(__successors);
                }
                __block.setId(AbstractControlFlowGraph.BLOCK_ID_VISITED);
                AbstractBeginNode __beginNode = __block.getBeginNode();
                if (__beginNode instanceof LoopBeginNode)
                {
                    computeLoopPredecessors(__nodeMap, __block, (LoopBeginNode) __beginNode);
                }
                else if (__beginNode instanceof MergeNode)
                {
                    MergeNode __mergeNode = (MergeNode) __beginNode;
                    int __forwardEndCount = __mergeNode.forwardEndCount();
                    Block[] __predecessors = new Block[__forwardEndCount];
                    for (int __i = 0; __i < __forwardEndCount; ++__i)
                    {
                        __predecessors[__i] = __nodeMap.get(__mergeNode.forwardEndAt(__i));
                    }
                    __block.setPredecessors(__predecessors);
                }
            }
            else if (__id == AbstractControlFlowGraph.BLOCK_ID_VISITED)
            {
                // Second time we see this block: All successors have been processed,
                // so add block to result list. Can safely reuse the stack for this.
                --__tos;
                __count++;
                int __index = __numBlocks - __count;
                __stack[__index] = __block;
                __block.setId(__index);
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        } while (__tos >= 0);

        // Compute reverse postorder and number blocks.
        this.___reversePostOrder = __stack;
    }

    private static void computeLoopPredecessors(NodeMap<Block> __nodeMap, Block __block, LoopBeginNode __loopBeginNode)
    {
        int __forwardEndCount = __loopBeginNode.forwardEndCount();
        LoopEndNode[] __loopEnds = __loopBeginNode.orderedLoopEnds();
        Block[] __predecessors = new Block[__forwardEndCount + __loopEnds.length];
        for (int __i = 0; __i < __forwardEndCount; ++__i)
        {
            __predecessors[__i] = __nodeMap.get(__loopBeginNode.forwardEndAt(__i));
        }
        for (int __i = 0; __i < __loopEnds.length; ++__i)
        {
            __predecessors[__i + __forwardEndCount] = __nodeMap.get(__loopEnds[__i]);
        }
        __block.setPredecessors(__predecessors);
    }

    private void computeProbabilities()
    {
        for (Block __block : this.___reversePostOrder)
        {
            Block[] __predecessors = __block.getPredecessors();

            double __probability;
            if (__predecessors.length == 0)
            {
                __probability = 1D;
            }
            else if (__predecessors.length == 1)
            {
                Block __pred = __predecessors[0];
                __probability = __pred.___probability;
                if (__pred.getSuccessorCount() > 1)
                {
                    ControlSplitNode __controlSplit = (ControlSplitNode) __pred.getEndNode();
                    __probability = multiplyProbabilities(__probability, __controlSplit.probability(__block.getBeginNode()));
                }
            }
            else
            {
                __probability = __predecessors[0].___probability;
                for (int __i = 1; __i < __predecessors.length; ++__i)
                {
                    __probability += __predecessors[__i].___probability;
                }

                if (__block.getBeginNode() instanceof LoopBeginNode)
                {
                    LoopBeginNode __loopBegin = (LoopBeginNode) __block.getBeginNode();
                    __probability = multiplyProbabilities(__probability, __loopBegin.loopFrequency());
                }
            }
            if (__probability < MIN_PROBABILITY)
            {
                __probability = MIN_PROBABILITY;
            }
            else if (__probability > MAX_PROBABILITY)
            {
                __probability = MAX_PROBABILITY;
            }
            __block.setProbability(__probability);
        }
    }

    private void computeLoopInformation()
    {
        this.___loops = new ArrayList<>();
        if (this.___graph.hasLoops())
        {
            Block[] __stack = new Block[this.___reversePostOrder.length];
            for (Block __block : this.___reversePostOrder)
            {
                AbstractBeginNode __beginNode = __block.getBeginNode();
                if (__beginNode instanceof LoopBeginNode)
                {
                    Loop<Block> __parent = __block.getLoop();
                    Loop<Block> __loop = new HIRLoop(__parent, this.___loops.size(), __block);
                    if (__parent != null)
                    {
                        __parent.getChildren().add(__loop);
                    }
                    this.___loops.add(__loop);
                    __block.setLoop(__loop);
                    __loop.getBlocks().add(__block);

                    LoopBeginNode __loopBegin = (LoopBeginNode) __beginNode;
                    for (LoopEndNode __end : __loopBegin.loopEnds())
                    {
                        Block __endBlock = this.___nodeToBlock.get(__end);
                        computeLoopBlocks(__endBlock, __loop, __stack, true);
                    }

                    if (this.___graph.getGuardsStage() != StructuredGraph.GuardsStage.AFTER_FSA)
                    {
                        for (LoopExitNode __exit : __loopBegin.loopExits())
                        {
                            Block __exitBlock = this.___nodeToBlock.get(__exit);
                            computeLoopBlocks(__exitBlock.getFirstPredecessor(), __loop, __stack, true);
                            __loop.addExit(__exitBlock);
                        }

                        // The following loop can add new blocks to the end of the loop's block list.
                        int __size = __loop.getBlocks().size();
                        for (int __i = 0; __i < __size; ++__i)
                        {
                            Block __b = __loop.getBlocks().get(__i);
                            for (Block __sux : __b.getSuccessors())
                            {
                                if (__sux.getLoop() != __loop)
                                {
                                    AbstractBeginNode __begin = __sux.getBeginNode();
                                    if (!(__begin instanceof LoopExitNode && ((LoopExitNode) __begin).loopBegin() == __loopBegin))
                                    {
                                        computeLoopBlocks(__sux, __loop, __stack, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compute the loop exit blocks after FSA.
        if (this.___graph.getGuardsStage() == StructuredGraph.GuardsStage.AFTER_FSA)
        {
            for (Block __b : this.___reversePostOrder)
            {
                if (__b.getLoop() != null)
                {
                    for (Block __succ : __b.getSuccessors())
                    {
                        // if the loop of the succ is a different one (or none)
                        if (__b.getLoop() != __succ.getLoop())
                        {
                            // and the succ loop is not a child loop of the curr one
                            if (__succ.getLoop() == null)
                            {
                                // we might exit multiple loops if b.loops is not a loop at depth 0
                                Loop<Block> __curr = __b.getLoop();
                                while (__curr != null)
                                {
                                    __curr.addExit(__succ);
                                    __curr = __curr.getParent();
                                }
                            }
                            else
                            {
                                // succ also has a loop, might be a child loop
                                //
                                // if it is a child loop we do not exit a loop. if it is a loop
                                // different than b.loop and not a child loop it must be a parent
                                // loop, thus we exit all loops between b.loop and succ.loop
                                //
                                // if we exit multiple loops immediately after each other the
                                // bytecode parser might generate loop exit nodes after another and
                                // the CFG will identify them as separate blocks, we just take the
                                // first one and exit all loops at this one
                                if (__succ.getLoop().getParent() != __b.getLoop())
                                {
                                    // b.loop must not be a transitive parent of succ.loop
                                    Loop<Block> __curr = __b.getLoop();
                                    while (__curr != null && __curr != __succ.getLoop())
                                    {
                                        __curr.addExit(__succ);
                                        __curr = __curr.getParent();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void computeLoopBlocks(Block __start, Loop<Block> __loop, Block[] __stack, boolean __usePred)
    {
        if (__start.getLoop() != __loop)
        {
            __start.setLoop(__loop);
            __stack[0] = __start;
            __loop.getBlocks().add(__start);
            int __tos = 0;
            do
            {
                Block __block = __stack[__tos--];

                // Add predecessors or successors to the loop.
                for (Block __b : (__usePred ? __block.getPredecessors() : __block.getSuccessors()))
                {
                    if (__b.getLoop() != __loop)
                    {
                        __stack[++__tos] = __b;
                        __b.setLoop(__loop);
                        __loop.getBlocks().add(__b);
                    }
                }
            } while (__tos >= 0);
        }
    }

    public void computePostdominators()
    {
        Block[] __reversePostOrderTmp = this.___reversePostOrder;
        outer: for (int j = __reversePostOrderTmp.length - 1; j >= 0; --j)
        {
            Block __block = __reversePostOrderTmp[j];
            if (__block.isLoopEnd())
            {
                // We do not want the loop header registered as the postdominator of the loop end.
                continue;
            }
            if (__block.getSuccessorCount() == 0)
            {
                // No successors => no postdominator.
                continue;
            }
            Block __firstSucc = __block.getSuccessors()[0];
            if (__block.getSuccessorCount() == 1)
            {
                __block.___postdominator = __firstSucc;
                continue;
            }
            Block __postdominator = __firstSucc;
            for (Block __sux : __block.getSuccessors())
            {
                __postdominator = commonPostdominator(__postdominator, __sux);
                if (__postdominator == null)
                {
                    // There is a dead end => no postdominator available.
                    continue outer;
                }
            }
            __block.setPostDominator(__postdominator);
        }
    }

    private static Block commonPostdominator(Block __a, Block __b)
    {
        Block __iterA = __a;
        Block __iterB = __b;
        while (__iterA != __iterB)
        {
            if (__iterA.getId() < __iterB.getId())
            {
                __iterA = __iterA.getPostdominator();
                if (__iterA == null)
                {
                    return null;
                }
            }
            else
            {
                __iterB = __iterB.getPostdominator();
                if (__iterB == null)
                {
                    return null;
                }
            }
        }
        return __iterA;
    }

    public void setNodeToBlock(NodeMap<Block> __nodeMap)
    {
        this.___nodeToBlock = __nodeMap;
    }

    ///
    // Multiplies a and b and clamps the between {@link ControlFlowGraph#MIN_PROBABILITY} and
    // {@link ControlFlowGraph#MAX_PROBABILITY}.
    ///
    public static double multiplyProbabilities(double __a, double __b)
    {
        double __r = __a * __b;
        if (__r > MAX_PROBABILITY)
        {
            return MAX_PROBABILITY;
        }
        if (__r < MIN_PROBABILITY)
        {
            return MIN_PROBABILITY;
        }
        return __r;
    }
}
