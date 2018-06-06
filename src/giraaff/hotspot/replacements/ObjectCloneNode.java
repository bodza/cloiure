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
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StructuredGraph;
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
    // @def
    public static final NodeClass<ObjectCloneNode> TYPE = NodeClass.create(ObjectCloneNode.class);

    // @cons ObjectCloneNode
    public ObjectCloneNode(CallTargetNode.InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode __receiver)
    {
        super(TYPE, __invokeKind, __targetMethod, __bci, __returnStamp, __receiver);
    }

    @Override
    protected Stamp computeStamp(ValueNode __object)
    {
        if (getConcreteType(__object.stamp(NodeView.DEFAULT)) != null)
        {
            return AbstractPointerStamp.pointerNonNull(__object.stamp(NodeView.DEFAULT));
        }
        // If this call can't be intrinsified don't report a non-null stamp, otherwise the stamp
        // would change when this is lowered back to an invoke and we might lose a null check.
        return AbstractPointerStamp.pointerMaybeNull(__object.stamp(NodeView.DEFAULT));
    }

    @Override
    protected StructuredGraph getLoweredSnippetGraph(LoweringTool __tool)
    {
        ResolvedJavaType __type = StampTool.typeOrNull(getObject());
        if (__type != null)
        {
            if (__type.isArray())
            {
                Method __method = ObjectCloneSnippets.arrayCloneMethods.get(__type.getComponentType().getJavaKind());
                if (__method != null)
                {
                    final ResolvedJavaMethod __snippetMethod = __tool.getMetaAccess().lookupJavaMethod(__method);
                    final Replacements __replacements = __tool.getReplacements();
                    StructuredGraph __snippetGraph = __replacements.getSnippet(__snippetMethod, null);

                    return lowerReplacement((StructuredGraph) __snippetGraph.copy(), __tool);
                }
            }
            else
            {
                Assumptions __assumptions = graph().getAssumptions();
                __type = getConcreteType(getObject().stamp(NodeView.DEFAULT));
                if (__type != null)
                {
                    StructuredGraph __newGraph = new StructuredGraph.GraphBuilder(StructuredGraph.AllowAssumptions.ifNonNull(__assumptions)).build();
                    ParameterNode __param = __newGraph.addWithoutUnique(new ParameterNode(0, StampPair.createSingle(getObject().stamp(NodeView.DEFAULT))));
                    NewInstanceNode __newInstance = __newGraph.add(new NewInstanceNode(__type, true));
                    __newGraph.addAfterFixed(__newGraph.start(), __newInstance);
                    ReturnNode __returnNode = __newGraph.add(new ReturnNode(__newInstance));
                    __newGraph.addAfterFixed(__newInstance, __returnNode);

                    for (ResolvedJavaField __field : __type.getInstanceFields(true))
                    {
                        LoadFieldNode __load = __newGraph.add(LoadFieldNode.create(__newGraph.getAssumptions(), __param, __field));
                        __newGraph.addBeforeFixed(__returnNode, __load);
                        __newGraph.addBeforeFixed(__returnNode, __newGraph.add(new StoreFieldNode(__newInstance, __field, __load)));
                    }
                    return lowerReplacement(__newGraph, __tool);
                }
            }
        }
        return null;
    }
}
