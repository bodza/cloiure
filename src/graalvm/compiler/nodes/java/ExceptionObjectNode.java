package graalvm.compiler.nodes.java;

import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.BeginStateSplitNode;
import graalvm.compiler.nodes.InvokeWithExceptionNode;
import graalvm.compiler.nodes.KillingBeginNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

import graalvm.compiler.nodeinfo.InputType;

/**
 * The entry to an exception handler with the exception coming from a call (as opposed to a local
 * throw instruction or implicit exception).
 */
public final class ExceptionObjectNode extends BeginStateSplitNode implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<ExceptionObjectNode> TYPE = NodeClass.create(ExceptionObjectNode.class);

    public ExceptionObjectNode(MetaAccessProvider metaAccess)
    {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createTrustedWithoutAssumptions(metaAccess.lookupJavaType(Throwable.class))));
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    /**
     * An exception handler is an entry point to a method from the runtime and so represents an
     * instruction that cannot be re-executed. It therefore needs a frame state.
     */
    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        if (graph().getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            /*
             * Now the lowering to BeginNode+LoadExceptionNode can be performed, since no more
             * deopts can float in between the begin node and the load exception node.
             */
            LocationIdentity locationsKilledByInvoke = ((InvokeWithExceptionNode) predecessor()).getLocationIdentity();
            AbstractBeginNode entry = graph().add(KillingBeginNode.create(locationsKilledByInvoke));
            LoadExceptionObjectNode loadException = graph().add(new LoadExceptionObjectNode(stamp(NodeView.DEFAULT)));

            loadException.setStateAfter(stateAfter());
            replaceAtUsages(InputType.Value, loadException);
            graph().replaceFixedWithFixed(this, entry);
            entry.graph().addAfterFixed(entry, loadException);

            loadException.lower(tool);
        }
    }
}
