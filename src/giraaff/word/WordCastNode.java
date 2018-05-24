package giraaff.word;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.ConstantValue;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Casts between Word and Object exposed by the {@link Word#fromAddress},
 * {@link Word#objectToTrackedPointer}, {@link Word#objectToUntrackedPointer} and
 * {@link Word#toObject()} operations. It has an impact on the pointer maps for the GC, so it must
 * not be scheduled or optimized away.
 */
public final class WordCastNode extends FixedWithNextNode implements LIRLowerable, Canonicalizable
{
    public static final NodeClass<WordCastNode> TYPE = NodeClass.create(WordCastNode.class);

    @Input ValueNode input;
    public final boolean trackedPointer;

    public static WordCastNode wordToObject(ValueNode input, JavaKind wordKind)
    {
        return new WordCastNode(StampFactory.object(), input);
    }

    public static WordCastNode wordToObjectNonNull(ValueNode input, JavaKind wordKind)
    {
        return new WordCastNode(StampFactory.objectNonNull(), input);
    }

    public static WordCastNode addressToWord(ValueNode input, JavaKind wordKind)
    {
        return new WordCastNode(StampFactory.forKind(wordKind), input);
    }

    public static WordCastNode objectToTrackedPointer(ValueNode input, JavaKind wordKind)
    {
        return new WordCastNode(StampFactory.forKind(wordKind), input, true);
    }

    public static WordCastNode objectToUntrackedPointer(ValueNode input, JavaKind wordKind)
    {
        return new WordCastNode(StampFactory.forKind(wordKind), input, false);
    }

    protected WordCastNode(Stamp stamp, ValueNode input)
    {
        this(stamp, input, true);
    }

    protected WordCastNode(Stamp stamp, ValueNode input, boolean trackedPointer)
    {
        super(TYPE, stamp);
        this.input = input;
        this.trackedPointer = trackedPointer;
    }

    public ValueNode getInput()
    {
        return input;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (tool.allUsagesAvailable() && hasNoUsages())
        {
            // If the cast is unused, it can be eliminated.
            return input;
        }

        if (input.isConstant())
        {
            // Null pointers are uncritical for GC, so they can be constant folded.
            if (input.asJavaConstant().isNull())
            {
                return ConstantNode.forIntegerStamp(stamp(NodeView.DEFAULT), 0);
            }
            else if (input.asJavaConstant().getJavaKind().isNumericInteger() && input.asJavaConstant().asLong() == 0)
            {
                return ConstantNode.forConstant(stamp(NodeView.DEFAULT), JavaConstant.NULL_POINTER, tool.getMetaAccess());
            }
        }

        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        Value value = generator.operand(input);
        ValueKind<?> kind = generator.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));

        if (trackedPointer && LIRKind.isValue(kind) && !LIRKind.isValue(value))
        {
            // just change the PlatformKind, but don't drop reference information
            kind = value.getValueKind().changeType(kind.getPlatformKind());
        }

        if (kind.equals(value.getValueKind()) && !(value instanceof ConstantValue))
        {
            generator.setResult(this, value);
        }
        else
        {
            AllocatableValue result = generator.getLIRGeneratorTool().newVariable(kind);
            generator.getLIRGeneratorTool().emitMove(result, value);
            generator.setResult(this, result);
        }
    }
}
