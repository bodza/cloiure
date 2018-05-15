package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

/**
 * Loads an object's class (i.e., this node can be created for {@code object.getClass()}).
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class GetClassNode extends FloatingNode implements Lowerable, Canonicalizable, Virtualizable {

    public static final NodeClass<GetClassNode> TYPE = NodeClass.create(GetClassNode.class);
    @Input ValueNode object;

    public ValueNode getObject() {
        return object;
    }

    public GetClassNode(Stamp stamp, ValueNode object) {
        super(TYPE, stamp);
        this.object = object;
        assert ((ObjectStamp) object.stamp(NodeView.DEFAULT)).nonNull();
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    public static ValueNode tryFold(MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, NodeView view, ValueNode object) {
        if (metaAccess != null && object != null && object.stamp(view) instanceof ObjectStamp) {
            ObjectStamp objectStamp = (ObjectStamp) object.stamp(view);
            if (objectStamp.isExactType()) {
                return ConstantNode.forConstant(constantReflection.asJavaClass(objectStamp.type()), metaAccess);
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool) {
        NodeView view = NodeView.from(tool);
        ValueNode folded = tryFold(tool.getMetaAccess(), tool.getConstantReflection(), view, getObject());
        return folded == null ? this : folded;
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode alias = tool.getAlias(getObject());
        if (alias instanceof VirtualObjectNode) {
            VirtualObjectNode virtual = (VirtualObjectNode) alias;
            Constant javaClass = tool.getConstantReflectionProvider().asJavaClass(virtual.type());
            tool.replaceWithValue(ConstantNode.forConstant(stamp(NodeView.DEFAULT), javaClass, tool.getMetaAccessProvider(), graph()));
        }
    }
}
