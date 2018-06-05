package giraaff.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.ExceptionHandler;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.bytecode.Bytecode;
import giraaff.bytecode.BytecodeLookupSwitch;
import giraaff.bytecode.BytecodeStream;
import giraaff.bytecode.BytecodeSwitch;
import giraaff.bytecode.BytecodeTableSwitch;
import giraaff.bytecode.Bytecodes;
import giraaff.core.common.GraalOptions;

///
// Builds a mapping between bytecodes and basic blocks and builds a conservative control flow graph
// (CFG). It makes one linear passes over the bytecodes to build the CFG where it detects block
// headers and connects them.
//
// It also creates exception dispatch blocks for exception handling. These blocks are between a
// bytecode that might throw an exception, and the actual exception handler entries, and are later
// used to create the type checks with the exception handler catch types. If a bytecode is covered
// by an exception handler, this bytecode ends the basic block. This guarantees that a) control flow
// cannot be transferred to an exception dispatch block in the middle of a block, and b) that every
// block has at most one exception dispatch block (which is always the last entry in the successor list).
//
// If a bytecode is covered by multiple exception handlers, a chain of exception dispatch blocks is
// created so that multiple exception handler types can be checked. The chains are re-used if
// multiple bytecodes are covered by the same exception handlers.
//
// Note that exception unwinds, i.e., bytecodes that can throw an exception but the exception is not
// handled in this method, do not end a basic block. Not modeling the exception unwind block reduces
// the complexity of the CFG, and there is no algorithm yet where the exception unwind block would matter.
//
// The class also handles subroutines (jsr and ret bytecodes): subroutines are inlined by
// duplicating the subroutine blocks. This is limited to simple, structured subroutines with a
// maximum subroutine nesting of 4. Otherwise, a bailout is thrown.
//
// Loops in the methods are detected. If a method contains an irreducible loop (a loop with more
// than one entry), a bailout is thrown. This simplifies the compiler later on since only structured
// loops need to be supported.
//
// A data flow analysis computes the live local variables from the point of view of the interpreter.
// The result is used later to prune frame states, i.e., remove local variable entries that are
// guaranteed to be never used again (even in the case of deoptimization).
//
// The algorithms and analysis in this class are conservative and do not use any assumptions or
// profiling information.
///
// @class BciBlockMapping
public final class BciBlockMapping
{
    // @class BciBlockMapping.BciBlock
    public static class BciBlock implements Cloneable
    {
        // @field
        int ___id;
        // @field
        final int ___startBci;
        // @field
        int ___endBci;
        // @field
        private boolean ___isExceptionEntry;
        // @field
        private boolean ___isLoopHeader;
        // @field
        int ___loopId;
        // @field
        int ___loopEnd;
        // @field
        List<BciBlock> ___successors;
        // @field
        private int ___predecessorCount;

        // @field
        private boolean ___visited;
        // @field
        private boolean ___active;
        // @field
        long ___loops;
        // @field
        JSRData ___jsrData;

        // @class BciBlockMapping.BciBlock.JSRData
        public static final class JSRData implements Cloneable
        {
            // @field
            public EconomicMap<JsrScope, BciBlock> ___jsrAlternatives;
            // @field
            public JsrScope ___jsrScope = JsrScope.EMPTY_SCOPE;
            // @field
            public BciBlock ___jsrSuccessor;
            // @field
            public int ___jsrReturnBci;
            // @field
            public BciBlock ___retSuccessor;
            // @field
            public boolean ___endsWithRet = false;

            public JSRData copy()
            {
                try
                {
                    return (JSRData) this.clone();
                }
                catch (CloneNotSupportedException __e)
                {
                    return null;
                }
            }
        }

        // @cons
        BciBlock(int __startBci)
        {
            super();
            this.___startBci = __startBci;
            this.___successors = new ArrayList<>();
        }

        public int getStartBci()
        {
            return this.___startBci;
        }

        public int getEndBci()
        {
            return this.___endBci;
        }

        public long getLoops()
        {
            return this.___loops;
        }

        public BciBlock exceptionDispatchBlock()
        {
            if (this.___successors.size() > 0 && this.___successors.get(this.___successors.size() - 1) instanceof ExceptionDispatchBlock)
            {
                return this.___successors.get(this.___successors.size() - 1);
            }
            return null;
        }

        public int getId()
        {
            return this.___id;
        }

        public int getPredecessorCount()
        {
            return this.___predecessorCount;
        }

        public int numNormalSuccessors()
        {
            if (exceptionDispatchBlock() != null)
            {
                return this.___successors.size() - 1;
            }
            return this.___successors.size();
        }

        public BciBlock copy()
        {
            try
            {
                BciBlock __block = (BciBlock) super.clone();
                if (__block.___jsrData != null)
                {
                    __block.___jsrData = __block.___jsrData.copy();
                }
                __block.___successors = new ArrayList<>(this.___successors);
                return __block;
            }
            catch (CloneNotSupportedException __e)
            {
                throw new RuntimeException(__e);
            }
        }

        public int getLoopDepth()
        {
            return Long.bitCount(this.___loops);
        }

        public boolean isLoopHeader()
        {
            return this.___isLoopHeader;
        }

        public boolean isExceptionEntry()
        {
            return this.___isExceptionEntry;
        }

        public BciBlock getSuccessor(int __index)
        {
            return this.___successors.get(__index);
        }

        ///
        // Get the loop id of the inner most loop.
        //
        // @return the loop id of the most inner loop or -1 if not part of any loop
        ///
        public int getLoopId()
        {
            long __l = this.___loops;
            if (__l == 0)
            {
                return -1;
            }
            int __pos = 0;
            for (int __lMask = 1; (__l & __lMask) == 0; __lMask = __lMask << 1)
            {
                __pos++;
            }
            return __pos;
        }

        ///
        // Iterate over loop ids.
        ///
        public Iterable<Integer> loopIdIterable()
        {
            // @closure
            return new Iterable<Integer>()
            {
                @Override
                public Iterator<Integer> iterator()
                {
                    return idIterator(BciBlock.this.___loops);
                }
            };
        }

        private static Iterator<Integer> idIterator(long __field)
        {
            // @closure
            return new Iterator<Integer>()
            {
                long __l = __field;
                int __pos = 0;
                int __lMask = 1;

                @Override
                public Integer next()
                {
                    for ( ; (__l & __lMask) == 0; __lMask = __lMask << 1)
                    {
                        __pos++;
                    }
                    __l &= ~__lMask;
                    return __pos;
                }

                @Override
                public boolean hasNext()
                {
                    return __l != 0;
                }
            };
        }

        public double probability()
        {
            return 1D;
        }

        public BciBlock getPostdominator()
        {
            return null;
        }

        private JSRData getOrCreateJSRData()
        {
            if (this.___jsrData == null)
            {
                this.___jsrData = new JSRData();
            }
            return this.___jsrData;
        }

        void setEndsWithRet()
        {
            getOrCreateJSRData().___endsWithRet = true;
        }

        public JsrScope getJsrScope()
        {
            if (this.___jsrData == null)
            {
                return JsrScope.EMPTY_SCOPE;
            }
            else
            {
                return this.___jsrData.___jsrScope;
            }
        }

        public boolean endsWithRet()
        {
            if (this.___jsrData == null)
            {
                return false;
            }
            else
            {
                return this.___jsrData.___endsWithRet;
            }
        }

        void setRetSuccessor(BciBlock __bciBlock)
        {
            this.getOrCreateJSRData().___retSuccessor = __bciBlock;
        }

        public BciBlock getRetSuccessor()
        {
            if (this.___jsrData == null)
            {
                return null;
            }
            else
            {
                return this.___jsrData.___retSuccessor;
            }
        }

        public BciBlock getJsrSuccessor()
        {
            if (this.___jsrData == null)
            {
                return null;
            }
            else
            {
                return this.___jsrData.___jsrSuccessor;
            }
        }

        public int getJsrReturnBci()
        {
            if (this.___jsrData == null)
            {
                return -1;
            }
            else
            {
                return this.___jsrData.___jsrReturnBci;
            }
        }

        public EconomicMap<JsrScope, BciBlock> getJsrAlternatives()
        {
            if (this.___jsrData == null)
            {
                return null;
            }
            else
            {
                return this.___jsrData.___jsrAlternatives;
            }
        }

        public void initJsrAlternatives()
        {
            JSRData __data = this.getOrCreateJSRData();
            if (__data.___jsrAlternatives == null)
            {
                __data.___jsrAlternatives = EconomicMap.create(Equivalence.DEFAULT);
            }
        }

        void setJsrScope(JsrScope __nextScope)
        {
            this.getOrCreateJSRData().___jsrScope = __nextScope;
        }

        void setJsrSuccessor(BciBlock __clone)
        {
            this.getOrCreateJSRData().___jsrSuccessor = __clone;
        }

        void setJsrReturnBci(int __bci)
        {
            this.getOrCreateJSRData().___jsrReturnBci = __bci;
        }

        public int getSuccessorCount()
        {
            return this.___successors.size();
        }

        public List<BciBlock> getSuccessors()
        {
            return this.___successors;
        }

        void setId(int __i)
        {
            this.___id = __i;
        }

        public void addSuccessor(BciBlock __sux)
        {
            this.___successors.add(__sux);
            __sux.___predecessorCount++;
        }

        public void clearSucccessors()
        {
            for (BciBlock __sux : this.___successors)
            {
                __sux.___predecessorCount--;
            }
            this.___successors.clear();
        }

        public boolean isExceptionDispatch()
        {
            return false;
        }
    }

    // @class BciBlockMapping.ExceptionDispatchBlock
    public static final class ExceptionDispatchBlock extends BciBlock
    {
        // @field
        public final ExceptionHandler ___handler;
        // @field
        public final int ___deoptBci;

        ///
        // Constructor for a normal dispatcher.
        ///
        // @cons
        ExceptionDispatchBlock(ExceptionHandler __handler, int __deoptBci)
        {
            super(__handler.getHandlerBCI());
            this.___endBci = this.___startBci;
            this.___deoptBci = __deoptBci;
            this.___handler = __handler;
        }

        ///
        // Constructor for the method unwind dispatcher.
        ///
        // @cons
        ExceptionDispatchBlock(int __deoptBci)
        {
            super(__deoptBci);
            this.___endBci = __deoptBci;
            this.___deoptBci = __deoptBci;
            this.___handler = null;
        }

        @Override
        public boolean isExceptionDispatch()
        {
            return true;
        }
    }

    ///
    // The blocks found in this method, in reverse postorder.
    ///
    // @field
    private BciBlock[] ___blocks;
    // @field
    public final Bytecode ___code;
    // @field
    public boolean ___hasJsrBytecodes;

    // @field
    private final ExceptionHandler[] ___exceptionHandlers;
    // @field
    private BciBlock ___startBlock;
    // @field
    private BciBlock[] ___loopHeaders;

    // @def
    private static final int LOOP_HEADER_MAX_CAPACITY = Long.SIZE;
    // @def
    private static final int LOOP_HEADER_INITIAL_CAPACITY = 4;

    // @field
    private int ___blocksNotYetAssignedId;

    ///
    // Creates a new BlockMap instance from {@code code}.
    ///
    // @cons
    private BciBlockMapping(Bytecode __code)
    {
        super();
        this.___code = __code;
        this.___exceptionHandlers = __code.getExceptionHandlers();
    }

    public BciBlock[] getBlocks()
    {
        return this.___blocks;
    }

    ///
    // Builds the block map and conservative CFG and numbers blocks.
    ///
    public void build(BytecodeStream __stream)
    {
        int __codeSize = this.___code.getCodeSize();
        BciBlock[] __blockMap = new BciBlock[__codeSize];
        makeExceptionEntries(__blockMap);
        iterateOverBytecodes(__blockMap, __stream);
        if (this.___hasJsrBytecodes)
        {
            if (!GraalOptions.supportJsrBytecodes)
            {
                throw new BailoutException("jsr/ret parsing disabled");
            }
            createJsrAlternatives(__blockMap, __blockMap[0]);
        }
        computeBlockOrder(__blockMap);
        fixLoopBits(__blockMap);

        this.___startBlock = __blockMap[0];
    }

    private void makeExceptionEntries(BciBlock[] __blockMap)
    {
        // start basic blocks at all exception handler blocks and mark them as exception entries
        for (ExceptionHandler __h : this.___exceptionHandlers)
        {
            BciBlock __xhandler = makeBlock(__blockMap, __h.getHandlerBCI());
            __xhandler.___isExceptionEntry = true;
        }
    }

    private void iterateOverBytecodes(BciBlock[] __blockMap, BytecodeStream __stream)
    {
        // iterate over the bytecodes top to bottom.
        // mark the entrypoints of basic blocks and build lists of successors for
        // all bytecodes that end basic blocks (i.e. goto, ifs, switches, throw, jsr, returns, ret)
        BciBlock __current = null;
        __stream.setBCI(0);
        while (__stream.currentBC() != Bytecodes.END)
        {
            int __bci = __stream.currentBCI();

            if (__current == null || __blockMap[__bci] != null)
            {
                BciBlock __b = makeBlock(__blockMap, __bci);
                if (__current != null)
                {
                    addSuccessor(__blockMap, __current.___endBci, __b);
                }
                __current = __b;
            }
            __blockMap[__bci] = __current;
            __current.___endBci = __bci;

            switch (__stream.currentBC())
            {
                case Bytecodes.IRETURN:
                case Bytecodes.LRETURN:
                case Bytecodes.ARETURN:
                case Bytecodes.RETURN:
                {
                    __current = null;
                    break;
                }
                case Bytecodes.ATHROW:
                {
                    __current = null;
                    ExceptionDispatchBlock __handler = handleExceptions(__blockMap, __bci);
                    if (__handler != null)
                    {
                        addSuccessor(__blockMap, __bci, __handler);
                    }
                    break;
                }
                case Bytecodes.IFEQ:
                case Bytecodes.IFNE:
                case Bytecodes.IFLT:
                case Bytecodes.IFGE:
                case Bytecodes.IFGT:
                case Bytecodes.IFLE:
                case Bytecodes.IF_ICMPEQ:
                case Bytecodes.IF_ICMPNE:
                case Bytecodes.IF_ICMPLT:
                case Bytecodes.IF_ICMPGE:
                case Bytecodes.IF_ICMPGT:
                case Bytecodes.IF_ICMPLE:
                case Bytecodes.IF_ACMPEQ:
                case Bytecodes.IF_ACMPNE:
                case Bytecodes.IFNULL:
                case Bytecodes.IFNONNULL:
                {
                    __current = null;
                    addSuccessor(__blockMap, __bci, makeBlock(__blockMap, __stream.readBranchDest()));
                    addSuccessor(__blockMap, __bci, makeBlock(__blockMap, __stream.nextBCI()));
                    break;
                }
                case Bytecodes.GOTO:
                case Bytecodes.GOTO_W:
                {
                    __current = null;
                    addSuccessor(__blockMap, __bci, makeBlock(__blockMap, __stream.readBranchDest()));
                    break;
                }
                case Bytecodes.TABLESWITCH:
                {
                    __current = null;
                    addSwitchSuccessors(__blockMap, __bci, new BytecodeTableSwitch(__stream, __bci));
                    break;
                }
                case Bytecodes.LOOKUPSWITCH:
                {
                    __current = null;
                    addSwitchSuccessors(__blockMap, __bci, new BytecodeLookupSwitch(__stream, __bci));
                    break;
                }
                case Bytecodes.JSR:
                case Bytecodes.JSR_W:
                {
                    this.___hasJsrBytecodes = true;
                    int __target = __stream.readBranchDest();
                    if (__target == 0)
                    {
                        throw new BailoutException("jsr target bci 0 not allowed");
                    }
                    BciBlock __b1 = makeBlock(__blockMap, __target);
                    __current.setJsrSuccessor(__b1);
                    __current.setJsrReturnBci(__stream.nextBCI());
                    __current = null;
                    addSuccessor(__blockMap, __bci, __b1);
                    break;
                }
                case Bytecodes.RET:
                {
                    __current.setEndsWithRet();
                    __current = null;
                    break;
                }
                case Bytecodes.INVOKEINTERFACE:
                case Bytecodes.INVOKESPECIAL:
                case Bytecodes.INVOKESTATIC:
                case Bytecodes.INVOKEVIRTUAL:
                case Bytecodes.INVOKEDYNAMIC:
                {
                    __current = null;
                    addSuccessor(__blockMap, __bci, makeBlock(__blockMap, __stream.nextBCI()));
                    ExceptionDispatchBlock __handler = handleExceptions(__blockMap, __bci);
                    if (__handler != null)
                    {
                        addSuccessor(__blockMap, __bci, __handler);
                    }
                    break;
                }
                case Bytecodes.IASTORE:
                case Bytecodes.LASTORE:
                case Bytecodes.AASTORE:
                case Bytecodes.BASTORE:
                case Bytecodes.CASTORE:
                case Bytecodes.SASTORE:
                case Bytecodes.IALOAD:
                case Bytecodes.LALOAD:
                case Bytecodes.AALOAD:
                case Bytecodes.BALOAD:
                case Bytecodes.CALOAD:
                case Bytecodes.SALOAD:
                case Bytecodes.ARRAYLENGTH:
                case Bytecodes.PUTSTATIC:
                case Bytecodes.GETSTATIC:
                case Bytecodes.PUTFIELD:
                case Bytecodes.GETFIELD:
                {
                    ExceptionDispatchBlock __handler = handleExceptions(__blockMap, __bci);
                    if (__handler != null)
                    {
                        __current = null;
                        addSuccessor(__blockMap, __bci, makeBlock(__blockMap, __stream.nextBCI()));
                        addSuccessor(__blockMap, __bci, __handler);
                    }
                }
            }
            __stream.next();
        }
    }

    private BciBlock makeBlock(BciBlock[] __blockMap, int __startBci)
    {
        BciBlock __oldBlock = __blockMap[__startBci];
        if (__oldBlock == null)
        {
            BciBlock __newBlock = new BciBlock(__startBci);
            this.___blocksNotYetAssignedId++;
            __blockMap[__startBci] = __newBlock;
            return __newBlock;
        }
        else if (__oldBlock.___startBci != __startBci)
        {
            // Backward branch into the middle of an already processed block.
            // Add the correct fall-through successor.
            BciBlock __newBlock = new BciBlock(__startBci);
            this.___blocksNotYetAssignedId++;
            __newBlock.___endBci = __oldBlock.___endBci;
            for (BciBlock __oldSuccessor : __oldBlock.getSuccessors())
            {
                __newBlock.addSuccessor(__oldSuccessor);
            }

            __oldBlock.___endBci = __startBci - 1;
            __oldBlock.clearSucccessors();
            __oldBlock.addSuccessor(__newBlock);

            for (int __i = __startBci; __i <= __newBlock.___endBci; __i++)
            {
                __blockMap[__i] = __newBlock;
            }
            return __newBlock;
        }
        else
        {
            return __oldBlock;
        }
    }

    private void addSwitchSuccessors(BciBlock[] __blockMap, int __predBci, BytecodeSwitch __bswitch)
    {
        // adds distinct targets to the successor list
        Collection<Integer> __targets = new TreeSet<>();
        for (int __i = 0; __i < __bswitch.numberOfCases(); __i++)
        {
            __targets.add(__bswitch.targetAt(__i));
        }
        __targets.add(__bswitch.defaultTarget());
        for (int __targetBci : __targets)
        {
            addSuccessor(__blockMap, __predBci, makeBlock(__blockMap, __targetBci));
        }
    }

    private static void addSuccessor(BciBlock[] __blockMap, int __predBci, BciBlock __sux)
    {
        BciBlock __predecessor = __blockMap[__predBci];
        if (__sux.___isExceptionEntry)
        {
            throw new BailoutException("exception handler can be reached by both normal and exceptional control flow");
        }
        __predecessor.addSuccessor(__sux);
    }

    // @field
    private final ArrayList<BciBlock> ___jsrVisited = new ArrayList<>();

    private void createJsrAlternatives(BciBlock[] __blockMap, BciBlock __block)
    {
        this.___jsrVisited.add(__block);
        JsrScope __scope = __block.getJsrScope();

        if (__block.endsWithRet())
        {
            __block.setRetSuccessor(__blockMap[__scope.nextReturnAddress()]);
            __block.addSuccessor(__block.getRetSuccessor());
        }

        if (__block.getJsrSuccessor() != null || !__scope.isEmpty())
        {
            for (int __i = 0; __i < __block.getSuccessorCount(); __i++)
            {
                BciBlock __successor = __block.getSuccessor(__i);
                JsrScope __nextScope = __scope;
                if (__successor == __block.getJsrSuccessor())
                {
                    __nextScope = __scope.push(__block.getJsrReturnBci());
                }
                if (__successor == __block.getRetSuccessor())
                {
                    __nextScope = __scope.pop();
                }
                if (!__successor.getJsrScope().isPrefixOf(__nextScope))
                {
                    throw new BailoutException("unstructured control flow (" + __successor.getJsrScope() + " " + __nextScope + ")");
                }
                if (!__nextScope.isEmpty())
                {
                    BciBlock __clone;
                    if (__successor.getJsrAlternatives() != null && __successor.getJsrAlternatives().containsKey(__nextScope))
                    {
                        __clone = __successor.getJsrAlternatives().get(__nextScope);
                    }
                    else
                    {
                        __successor.initJsrAlternatives();
                        __clone = __successor.copy();
                        this.___blocksNotYetAssignedId++;
                        __clone.setJsrScope(__nextScope);
                        __successor.getJsrAlternatives().put(__nextScope, __clone);
                    }
                    __block.getSuccessors().set(__i, __clone);
                    if (__successor == __block.getJsrSuccessor())
                    {
                        __block.setJsrSuccessor(__clone);
                    }
                    if (__successor == __block.getRetSuccessor())
                    {
                        __block.setRetSuccessor(__clone);
                    }
                }
            }
        }
        for (BciBlock __successor : __block.getSuccessors())
        {
            if (!this.___jsrVisited.contains(__successor))
            {
                createJsrAlternatives(__blockMap, __successor);
            }
        }
    }

    private ExceptionDispatchBlock handleExceptions(BciBlock[] __blockMap, int __bci)
    {
        ExceptionDispatchBlock __lastHandler = null;
        int __dispatchBlocks = 0;

        for (int __i = this.___exceptionHandlers.length - 1; __i >= 0; __i--)
        {
            ExceptionHandler __h = this.___exceptionHandlers[__i];
            if (__h.getStartBCI() <= __bci && __bci < __h.getEndBCI())
            {
                if (__h.isCatchAll())
                {
                    // Discard all information about succeeding exception handlers, since they
                    // can never be reached.
                    __dispatchBlocks = 0;
                    __lastHandler = null;
                }

                // We do not reuse exception dispatch blocks, because nested exception handlers
                // might have problems reasoning about the correct frame state.
                ExceptionDispatchBlock __curHandler = new ExceptionDispatchBlock(__h, __bci);
                __dispatchBlocks++;
                __curHandler.addSuccessor(__blockMap[__h.getHandlerBCI()]);
                if (__lastHandler != null)
                {
                    __curHandler.addSuccessor(__lastHandler);
                }
                __lastHandler = __curHandler;
            }
        }
        this.___blocksNotYetAssignedId += __dispatchBlocks;
        return __lastHandler;
    }

    // @field
    private boolean ___loopChanges;

    private void fixLoopBits(BciBlock[] __blockMap)
    {
        do
        {
            this.___loopChanges = false;
            for (BciBlock __b : this.___blocks)
            {
                __b.___visited = false;
            }

            long __loop = fixLoopBits(__blockMap, __blockMap[0]);

            if (__loop != 0)
            {
                // There is a path from a loop end to the method entry that does not pass the loop header.
                // Therefore, the loop is non reducible (has more than one entry).
                // We don't want to compile such methods because the IR only supports structured loops.
                throw new BailoutException("non-reducible loop: %016x", __loop);
            }
        } while (this.___loopChanges);
    }

    private void computeBlockOrder(BciBlock[] __blockMap)
    {
        int __maxBlocks = this.___blocksNotYetAssignedId;
        this.___blocks = new BciBlock[this.___blocksNotYetAssignedId];
        long __loop = computeBlockOrder(__blockMap[0]);

        if (__loop != 0)
        {
            // There is a path from a loop end to the method entry that does not pass the loop header.
            // Therefore, the loop is non reducible (has more than one entry).
            // We don't want to compile such methods because the IR only supports structured loops.
            throw new BailoutException("non-reducible loop");
        }

        // Purge null entries for unreached blocks and sort blocks such that loop bodies are always
        // consecutively in the array.
        int __blockCount = __maxBlocks - this.___blocksNotYetAssignedId + 1;
        BciBlock[] __newBlocks = new BciBlock[__blockCount];
        int __next = 0;
        for (int __i = 0; __i < this.___blocks.length; ++__i)
        {
            BciBlock __b = this.___blocks[__i];
            if (__b != null)
            {
                __b.setId(__next);
                __newBlocks[__next++] = __b;
                if (__b.___isLoopHeader)
                {
                    __next = handleLoopHeader(__newBlocks, __next, __i, __b);
                }
            }
        }

        // Add unwind block.
        int __deoptBci = this.___code.getMethod().isSynchronized() ? BytecodeFrame.UNWIND_BCI : BytecodeFrame.AFTER_EXCEPTION_BCI;
        ExceptionDispatchBlock __unwindBlock = new ExceptionDispatchBlock(__deoptBci);
        __unwindBlock.setId(__newBlocks.length - 1);
        __newBlocks[__newBlocks.length - 1] = __unwindBlock;

        this.___blocks = __newBlocks;
    }

    private int handleLoopHeader(BciBlock[] __newBlocks, int __nextStart, int __i, BciBlock __loopHeader)
    {
        int __next = __nextStart;
        int __endOfLoop = __nextStart - 1;
        for (int __j = __i + 1; __j < this.___blocks.length; ++__j)
        {
            BciBlock __other = this.___blocks[__j];
            if (__other != null && (__other.___loops & (1L << __loopHeader.___loopId)) != 0)
            {
                __other.setId(__next);
                __endOfLoop = __next;
                __newBlocks[__next++] = __other;
                this.___blocks[__j] = null;
                if (__other.___isLoopHeader)
                {
                    __next = handleLoopHeader(__newBlocks, __next, __j, __other);
                }
            }
        }
        __loopHeader.___loopEnd = __endOfLoop;
        return __next;
    }

    ///
    // Get the header block for a loop index.
    ///
    public BciBlock getLoopHeader(int __index)
    {
        return this.___loopHeaders[__index];
    }

    ///
    // The next available loop number.
    ///
    // @field
    private int ___nextLoop;

    ///
    // Mark the block as a loop header, using the next available loop number. Also checks for corner
    // cases that we don't want to compile.
    ///
    private void makeLoopHeader(BciBlock __block)
    {
        if (!__block.___isLoopHeader)
        {
            __block.___isLoopHeader = true;

            if (__block.___isExceptionEntry)
            {
                // Loops that are implicitly formed by an exception handler lead to all sorts of corner cases.
                // Don't compile such methods for now, until we see a concrete case that allows checking for correctness.
                throw new BailoutException("loop formed by an exception handler");
            }
            if (this.___nextLoop >= LOOP_HEADER_MAX_CAPACITY)
            {
                // This restriction can be removed by using a fall-back to a BitSet in case we have more than 64 loops.
                // Don't compile such methods for now, until we see a concrete case that allows checking for correctness.
                throw new BailoutException("too many loops in method");
            }

            __block.___loops = 1L << this.___nextLoop;
            if (this.___loopHeaders == null)
            {
                this.___loopHeaders = new BciBlock[LOOP_HEADER_INITIAL_CAPACITY];
            }
            else if (this.___nextLoop >= this.___loopHeaders.length)
            {
                this.___loopHeaders = Arrays.copyOf(this.___loopHeaders, LOOP_HEADER_MAX_CAPACITY);
            }
            this.___loopHeaders[this.___nextLoop] = __block;
            __block.___loopId = this.___nextLoop;
            this.___nextLoop++;
        }
    }

    ///
    // Depth-first traversal of the control flow graph. The flag {@linkplain BciBlock#visited} is
    // used to visit every block only once. The flag {@linkplain BciBlock#active} is used to detect
    // cycles (backward edges).
    ///
    private long computeBlockOrder(BciBlock __block)
    {
        if (__block.___visited)
        {
            if (__block.___active)
            {
                // Reached block via backward branch.
                makeLoopHeader(__block);
                // Return cached loop information for this block.
                return __block.___loops;
            }
            else if (__block.___isLoopHeader)
            {
                return __block.___loops & ~(1L << __block.___loopId);
            }
            else
            {
                return __block.___loops;
            }
        }

        __block.___visited = true;
        __block.___active = true;

        long __loops = 0;
        for (BciBlock __successor : __block.getSuccessors())
        {
            // Recursively process successors.
            __loops |= computeBlockOrder(__successor);
            if (__successor.___active)
            {
                // Reached block via backward branch.
                __loops |= (1L << __successor.___loopId);
            }
        }

        __block.___loops = __loops;

        if (__block.___isLoopHeader)
        {
            __loops &= ~(1L << __block.___loopId);
        }

        __block.___active = false;
        this.___blocksNotYetAssignedId--;
        this.___blocks[this.___blocksNotYetAssignedId] = __block;

        return __loops;
    }

    private long fixLoopBits(BciBlock[] __blockMap, BciBlock __block)
    {
        if (__block.___visited)
        {
            // Return cached loop information for this block.
            if (__block.___isLoopHeader)
            {
                return __block.___loops & ~(1L << __block.___loopId);
            }
            else
            {
                return __block.___loops;
            }
        }

        __block.___visited = true;
        long __loops = __block.___loops;
        for (BciBlock __successor : __block.getSuccessors())
        {
            // Recursively process successors.
            __loops |= fixLoopBits(__blockMap, __successor);
        }
        if (__block.___loops != __loops)
        {
            this.___loopChanges = true;
            __block.___loops = __loops;
        }

        if (__block.___isLoopHeader)
        {
            __loops &= ~(1L << __block.___loopId);
        }

        return __loops;
    }

    public static BciBlockMapping create(BytecodeStream __stream, Bytecode __code)
    {
        BciBlockMapping __map = new BciBlockMapping(__code);
        __map.build(__stream);
        return __map;
    }

    public BciBlock[] getLoopHeaders()
    {
        return this.___loopHeaders;
    }

    public BciBlock getStartBlock()
    {
        return this.___startBlock;
    }

    public ExceptionDispatchBlock getUnwindBlock()
    {
        return (ExceptionDispatchBlock) this.___blocks[this.___blocks.length - 1];
    }

    public int getLoopCount()
    {
        return this.___nextLoop;
    }

    public int getBlockCount()
    {
        return this.___blocks.length;
    }
}
