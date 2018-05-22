package graalvm.compiler.replacements.amd64;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NamedLocationIdentity;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.ValueNodeUtil;
import graalvm.compiler.nodes.memory.MemoryAccess;
import graalvm.compiler.nodes.memory.MemoryNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public class AMD64StringIndexOfNode extends FixedWithNextNode implements LIRLowerable, MemoryAccess
{
    public static final NodeClass<AMD64StringIndexOfNode> TYPE = NodeClass.create(AMD64StringIndexOfNode.class);

    @OptionalInput(InputType.Memory) protected MemoryNode lastLocationAccess;

    @Input protected NodeInputList<ValueNode> arguments;

    public AMD64StringIndexOfNode(ValueNode sourcePointer, ValueNode sourceCount, ValueNode targetPointer, ValueNode targetCount)
    {
        super(TYPE, StampFactory.forInteger(32));
        this.arguments = new NodeInputList<>(this, new ValueNode[]{sourcePointer, sourceCount, targetPointer, targetCount});
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return NamedLocationIdentity.getArrayLocation(JavaKind.Char);
    }

    ValueNode sourcePointer()
    {
        return arguments.get(0);
    }

    ValueNode sourceCount()
    {
        return arguments.get(1);
    }

    ValueNode targetPointer()
    {
        return arguments.get(2);
    }

    ValueNode targetCount()
    {
        return arguments.get(3);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        int constantTargetCount = -1;
        if (targetCount().isConstant())
        {
            constantTargetCount = targetCount().asJavaConstant().asInt();
        }
        Value result = gen.getLIRGeneratorTool().emitStringIndexOf(gen.operand(sourcePointer()), gen.operand(sourceCount()), gen.operand(targetPointer()), gen.operand(targetCount()), constantTargetCount);
        gen.setResult(this, result);
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode lla)
    {
        updateUsages(ValueNodeUtil.asNode(lastLocationAccess), ValueNodeUtil.asNode(lla));
        lastLocationAccess = lla;
    }

    @NodeIntrinsic
    public static native int optimizedStringIndexPointer(Pointer sourcePointer, int sourceCount, Pointer targetPointer, int targetCount);
}
