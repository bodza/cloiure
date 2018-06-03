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

///
// The entry to an exception handler with the exception coming from a call (as opposed to a local
// throw instruction or implicit exception).
///
// @NodeInfo.allowedUsageTypes "Memory"
// @class ExceptionObjectNode
public final class ExceptionObjectNode extends BeginStateSplitNode implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<ExceptionObjectNode> TYPE = NodeClass.create(ExceptionObjectNode.class);

    // @cons
    public ExceptionObjectNode(MetaAccessProvider __metaAccess)
    {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createTrustedWithoutAssumptions(__metaAccess.lookupJavaType(Throwable.class))));
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    ///
    // An exception handler is an entry point to a method from the runtime and so represents an
    // instruction that cannot be re-executed. It therefore needs a frame state.
    ///
    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        if (graph().getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            // Now the lowering to BeginNode+LoadExceptionNode can be performed, since no more
            // deopts can float in between the begin node and the load exception node.
            LocationIdentity __locationsKilledByInvoke = ((InvokeWithExceptionNode) predecessor()).getLocationIdentity();
            AbstractBeginNode __entry = graph().add(KillingBeginNode.create(__locationsKilledByInvoke));
            LoadExceptionObjectNode __loadException = graph().add(new LoadExceptionObjectNode(stamp(NodeView.DEFAULT)));

            __loadException.setStateAfter(stateAfter());
            replaceAtUsages(InputType.Value, __loadException);
            graph().replaceFixedWithFixed(this, __entry);
            __entry.graph().addAfterFixed(__entry, __loadException);

            __loadException.lower(__tool);
        }
    }
}
