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

///
// Casts between Word and Object exposed by the {@link Word#fromAddress},
// {@link Word#objectToTrackedPointer}, {@link Word#objectToUntrackedPointer} and
// {@link Word#toObject()} operations. It has an impact on the pointer maps for the GC, so it must
// not be scheduled or optimized away.
///
// @class WordCastNode
public final class WordCastNode extends FixedWithNextNode implements LIRLowerable, Canonicalizable
{
    // @def
    public static final NodeClass<WordCastNode> TYPE = NodeClass.create(WordCastNode.class);

    @Input
    // @field
    ValueNode ___input;
    // @field
    public final boolean ___trackedPointer;

    public static WordCastNode wordToObject(ValueNode __input, JavaKind __wordKind)
    {
        return new WordCastNode(StampFactory.object(), __input);
    }

    public static WordCastNode wordToObjectNonNull(ValueNode __input, JavaKind __wordKind)
    {
        return new WordCastNode(StampFactory.objectNonNull(), __input);
    }

    public static WordCastNode addressToWord(ValueNode __input, JavaKind __wordKind)
    {
        return new WordCastNode(StampFactory.forKind(__wordKind), __input);
    }

    public static WordCastNode objectToTrackedPointer(ValueNode __input, JavaKind __wordKind)
    {
        return new WordCastNode(StampFactory.forKind(__wordKind), __input, true);
    }

    public static WordCastNode objectToUntrackedPointer(ValueNode __input, JavaKind __wordKind)
    {
        return new WordCastNode(StampFactory.forKind(__wordKind), __input, false);
    }

    // @cons
    protected WordCastNode(Stamp __stamp, ValueNode __input)
    {
        this(__stamp, __input, true);
    }

    // @cons
    protected WordCastNode(Stamp __stamp, ValueNode __input, boolean __trackedPointer)
    {
        super(TYPE, __stamp);
        this.___input = __input;
        this.___trackedPointer = __trackedPointer;
    }

    public ValueNode getInput()
    {
        return this.___input;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (__tool.allUsagesAvailable() && hasNoUsages())
        {
            // If the cast is unused, it can be eliminated.
            return this.___input;
        }

        if (this.___input.isConstant())
        {
            // Null pointers are uncritical for GC, so they can be constant folded.
            if (this.___input.asJavaConstant().isNull())
            {
                return ConstantNode.forIntegerStamp(stamp(NodeView.DEFAULT), 0);
            }
            else if (this.___input.asJavaConstant().getJavaKind().isNumericInteger() && this.___input.asJavaConstant().asLong() == 0)
            {
                return ConstantNode.forConstant(stamp(NodeView.DEFAULT), JavaConstant.NULL_POINTER, __tool.getMetaAccess());
            }
        }

        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __value = __gen.operand(this.___input);
        ValueKind<?> __kind = __gen.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));

        if (this.___trackedPointer && LIRKind.isValue(__kind) && !LIRKind.isValue(__value))
        {
            // just change the PlatformKind, but don't drop reference information
            __kind = __value.getValueKind().changeType(__kind.getPlatformKind());
        }

        if (__kind.equals(__value.getValueKind()) && !(__value instanceof ConstantValue))
        {
            __gen.setResult(this, __value);
        }
        else
        {
            AllocatableValue __result = __gen.getLIRGeneratorTool().newVariable(__kind);
            __gen.getLIRGeneratorTool().emitMove(__result, __value);
            __gen.setResult(this, __result);
        }
    }
}
