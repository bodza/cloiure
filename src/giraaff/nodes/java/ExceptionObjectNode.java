package giraaff.nodes.java;

import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.BeginStateSplitNode;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.KillingBeginNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

import giraaff.nodeinfo.InputType;

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
