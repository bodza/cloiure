package giraaff.hotspot.replacements;

import java.lang.reflect.Method;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.AllowAssumptions;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.java.NewInstanceNode;
import giraaff.nodes.java.StoreFieldNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Replacements;
import giraaff.nodes.spi.VirtualizableAllocation;
import giraaff.nodes.type.StampTool;
import giraaff.replacements.nodes.BasicObjectCloneNode;

// @class ObjectCloneNode
public final class ObjectCloneNode extends BasicObjectCloneNode implements VirtualizableAllocation, ArrayLengthProvider
{
    public static final NodeClass<ObjectCloneNode> TYPE = NodeClass.create(ObjectCloneNode.class);

    // @cons
    public ObjectCloneNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode receiver)
    {
        super(TYPE, invokeKind, targetMethod, bci, returnStamp, receiver);
    }

    @Override
    protected Stamp computeStamp(ValueNode object)
    {
        if (getConcreteType(object.stamp(NodeView.DEFAULT)) != null)
        {
            return AbstractPointerStamp.pointerNonNull(object.stamp(NodeView.DEFAULT));
        }
        /*
         * If this call can't be intrinsified don't report a non-null stamp, otherwise the stamp
         * would change when this is lowered back to an invoke and we might lose a null check.
         */
        return AbstractPointerStamp.pointerMaybeNull(object.stamp(NodeView.DEFAULT));
    }

    @Override
    protected StructuredGraph getLoweredSnippetGraph(LoweringTool tool)
    {
        ResolvedJavaType type = StampTool.typeOrNull(getObject());
        if (type != null)
        {
            if (type.isArray())
            {
                Method method = ObjectCloneSnippets.arrayCloneMethods.get(type.getComponentType().getJavaKind());
                if (method != null)
                {
                    final ResolvedJavaMethod snippetMethod = tool.getMetaAccess().lookupJavaMethod(method);
                    final Replacements replacements = tool.getReplacements();
                    StructuredGraph snippetGraph = replacements.getSnippet(snippetMethod, null);

                    return lowerReplacement((StructuredGraph) snippetGraph.copy(), tool);
                }
            }
            else
            {
                Assumptions assumptions = graph().getAssumptions();
                type = getConcreteType(getObject().stamp(NodeView.DEFAULT));
                if (type != null)
                {
                    StructuredGraph newGraph = new StructuredGraph.Builder(graph().getOptions(), AllowAssumptions.ifNonNull(assumptions)).build();
                    ParameterNode param = newGraph.addWithoutUnique(new ParameterNode(0, StampPair.createSingle(getObject().stamp(NodeView.DEFAULT))));
                    NewInstanceNode newInstance = newGraph.add(new NewInstanceNode(type, true));
                    newGraph.addAfterFixed(newGraph.start(), newInstance);
                    ReturnNode returnNode = newGraph.add(new ReturnNode(newInstance));
                    newGraph.addAfterFixed(newInstance, returnNode);

                    for (ResolvedJavaField field : type.getInstanceFields(true))
                    {
                        LoadFieldNode load = newGraph.add(LoadFieldNode.create(newGraph.getAssumptions(), param, field));
                        newGraph.addBeforeFixed(returnNode, load);
                        newGraph.addBeforeFixed(returnNode, newGraph.add(new StoreFieldNode(newInstance, field, load)));
                    }
                    return lowerReplacement(newGraph, tool);
                }
            }
        }
        return null;
    }
}
