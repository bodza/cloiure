package giraaff.lir.gen;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInsertionBuffer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

/**
 * Converts phi instructions into moves.
 *
 * Resolves cycles:
 *
 * <pre>
 *
 *  r1 := r2  becomes  temp := r1
 *  r2 := r1           r1 := r2
 *                     r2 := temp
 * </pre>
 *
 * and orders moves:
 *
 * <pre>
 *  r2 := r3  becomes  r1 := r2
 *  r1 := r2           r2 := r3
 * </pre>
 */
// @class PhiResolver
public final class PhiResolver
{
    /**
     * Tracks a data flow dependency between a source operand and any number of the destination operands.
     */
    // @class PhiResolver.PhiResolverNode
    static final class PhiResolverNode
    {
        /**
         * A source operand whose value flows into the {@linkplain #destinations destination} operands.
         */
        // @field
        final Value operand;

        /**
         * The operands whose values are defined by the {@linkplain #operand source} operand.
         */
        // @field
        final ArrayList<PhiResolverNode> destinations;

        /**
         * Denotes if a move instruction has already been emitted to initialize the value of {@link #operand}.
         */
        // @field
        boolean assigned;

        /**
         * Specifies if this operand been visited for the purpose of emitting a move instruction.
         */
        // @field
        boolean visited;

        /**
         * Specifies if this is the initial definition in data flow path for a given value.
         */
        // @field
        boolean startNode;

        // @cons
        PhiResolverNode(Value __operand)
        {
            super();
            this.operand = __operand;
            destinations = new ArrayList<>(4);
        }
    }

    // @field
    private final LIRGeneratorTool gen;
    // @field
    private final MoveFactory moveFactory;
    // @field
    private final LIRInsertionBuffer buffer;
    // @field
    private final int insertBefore;

    /**
     * The operand loop header phi for the operand currently being process in {@link #dispose()}.
     */
    // @field
    private PhiResolverNode loop;

    // @field
    private Value temp;

    // @field
    private final ArrayList<PhiResolverNode> variableOperands = new ArrayList<>(3);
    // @field
    private final ArrayList<PhiResolverNode> otherOperands = new ArrayList<>(3);

    /**
     * Maps operands to nodes.
     */
    // @field
    private final EconomicMap<Value, PhiResolverNode> operandToNodeMap = EconomicMap.create(Equivalence.DEFAULT);

    public static PhiResolver create(LIRGeneratorTool __gen)
    {
        AbstractBlockBase<?> __block = __gen.getCurrentBlock();
        ArrayList<LIRInstruction> __instructions = __gen.getResult().getLIR().getLIRforBlock(__block);

        return new PhiResolver(__gen, new LIRInsertionBuffer(), __instructions, __instructions.size());
    }

    public static PhiResolver create(LIRGeneratorTool __gen, LIRInsertionBuffer __buffer, List<LIRInstruction> __instructions, int __insertBefore)
    {
        return new PhiResolver(__gen, __buffer, __instructions, __insertBefore);
    }

    // @cons
    protected PhiResolver(LIRGeneratorTool __gen, LIRInsertionBuffer __buffer, List<LIRInstruction> __instructions, int __insertBefore)
    {
        super();
        this.gen = __gen;
        moveFactory = __gen.getSpillMoveFactory();
        temp = Value.ILLEGAL;

        this.buffer = __buffer;
        this.buffer.init(__instructions);
        this.insertBefore = __insertBefore;
    }

    public void dispose()
    {
        // resolve any cycles in moves from and to variables
        for (int __i = variableOperands.size() - 1; __i >= 0; __i--)
        {
            PhiResolverNode __node = variableOperands.get(__i);
            if (!__node.visited)
            {
                loop = null;
                move(__node, null);
                __node.startNode = true;
            }
        }

        // generate move for move from non variable to arbitrary destination
        for (int __i = otherOperands.size() - 1; __i >= 0; __i--)
        {
            PhiResolverNode __node = otherOperands.get(__i);
            for (int __j = __node.destinations.size() - 1; __j >= 0; __j--)
            {
                emitMove(__node.destinations.get(__j).operand, __node.operand);
            }
        }
        buffer.finish();
    }

    public void move(Value __dst, Value __src)
    {
        sourceNode(__src).destinations.add(destinationNode(__dst));
    }

    private PhiResolverNode createNode(Value __operand, boolean __source)
    {
        PhiResolverNode __node;
        if (LIRValueUtil.isVariable(__operand))
        {
            __node = operandToNodeMap.get(__operand);
            if (__node == null)
            {
                __node = new PhiResolverNode(__operand);
                operandToNodeMap.put(__operand, __node);
            }
            // Make sure that all variables show up in the list when they are used as the source of a move.
            if (__source)
            {
                if (!variableOperands.contains(__node))
                {
                    variableOperands.add(__node);
                }
            }
        }
        else
        {
            __node = new PhiResolverNode(__operand);
            otherOperands.add(__node);
        }
        return __node;
    }

    private PhiResolverNode destinationNode(Value __opr)
    {
        return createNode(__opr, false);
    }

    private void emitMove(Value __dest, Value __src)
    {
        LIRInstruction __move = moveFactory.createMove((AllocatableValue) __dest, __src);
        buffer.append(insertBefore, __move);
    }

    // Traverse assignment graph in depth first order and generate moves in post order
    // ie. two assignments: b := c, a := b start with node c:
    // Call graph: move(c, NULL) -> move(b, c) -> move(a, b)
    // Generates moves in this order: move b to a and move c to b
    // ie. cycle a := b, b := a start with node a
    // Call graph: move(a, NULL) -> move(b, a) -> move(a, b)
    // Generates moves in this order: move b to temp, move a to b, move temp to a
    private void move(PhiResolverNode __dest, PhiResolverNode __src)
    {
        if (!__dest.visited)
        {
            __dest.visited = true;
            for (int __i = __dest.destinations.size() - 1; __i >= 0; __i--)
            {
                move(__dest.destinations.get(__i), __dest);
            }
        }
        else if (!__dest.startNode)
        {
            // cycle in graph detected
            loop = __dest;
            moveToTemp(__src.operand);
            return;
        }

        if (!__dest.assigned)
        {
            if (loop == __dest)
            {
                moveTempTo(__dest.operand);
                __dest.assigned = true;
            }
            else if (__src != null)
            {
                emitMove(__dest.operand, __src.operand);
                __dest.assigned = true;
            }
        }
    }

    private void moveTempTo(Value __dest)
    {
        emitMove(__dest, temp);
        temp = Value.ILLEGAL;
    }

    private void moveToTemp(Value __src)
    {
        temp = gen.newVariable(__src.getValueKind());
        emitMove(temp, __src);
    }

    private PhiResolverNode sourceNode(Value __opr)
    {
        return createNode(__opr, true);
    }
}
