package graalvm.compiler.nodes.debug;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_IGNORED;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_IGNORED;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * This node can be used to add a counter to the code that will estimate the dynamic number of calls
 * by adding an increment to the compiled code. This should of course only be used for
 * debugging/testing purposes.
 *
 * A unique counter will be created for each unique name passed to the constructor. Depending on the
 * value of withContext, the name of the root method is added to the counter's name.
 */
//@formatter:off
@NodeInfo(size = SIZE_IGNORED,
          sizeRationale = "Node is a debugging node that should not be used in production.",
          cycles = CYCLES_IGNORED,
          cyclesRationale = "Node is a debugging node that should not be used in production.")
//@formatter:on
public class DynamicCounterNode extends FixedWithNextNode implements LIRLowerable {

    public static final NodeClass<DynamicCounterNode> TYPE = NodeClass.create(DynamicCounterNode.class);
    @Input ValueNode increment;

    protected final String name;
    protected final String group;
    protected final boolean withContext;

    public DynamicCounterNode(String name, String group, ValueNode increment, boolean withContext) {
        this(TYPE, name, group, increment, withContext);
    }

    protected DynamicCounterNode(NodeClass<? extends DynamicCounterNode> c, String name, String group, ValueNode increment, boolean withContext) {
        super(c, StampFactory.forVoid());
        this.name = name;
        this.group = group;
        this.increment = increment;
        this.withContext = withContext;
    }

    public ValueNode getIncrement() {
        return increment;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public boolean isWithContext() {
        return withContext;
    }

    public static void addCounterBefore(String group, String name, long increment, boolean withContext, FixedNode position) {
        StructuredGraph graph = position.graph();
        graph.addBeforeFixed(position, position.graph().add(new DynamicCounterNode(name, group, ConstantNode.forLong(increment, position.graph()), withContext)));
    }

    @NodeIntrinsic
    public static native void counter(@ConstantNodeParameter String name, @ConstantNodeParameter String group, long increment, @ConstantNodeParameter boolean addContext);

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        LIRGeneratorTool lirGen = generator.getLIRGeneratorTool();
        String nameWithContext;
        if (isWithContext()) {
            nameWithContext = getName() + " @ ";
            if (graph().method() != null) {
                StackTraceElement stackTraceElement = graph().method().asStackTraceElement(0);
                if (stackTraceElement != null) {
                    nameWithContext += " " + stackTraceElement.toString();
                } else {
                    nameWithContext += graph().method().format("%h.%n");
                }
            }
            if (graph().name != null) {
                nameWithContext += " (" + graph().name + ")";
            }

        } else {
            nameWithContext = getName();
        }
        LIRInstruction counterOp = lirGen.createBenchmarkCounter(nameWithContext, getGroup(), generator.operand(increment));
        if (counterOp != null) {
            lirGen.append(counterOp);
        } else {
            throw GraalError.unimplemented("Benchmark counters not enabled or not implemented by the back end.");
        }
    }

}
