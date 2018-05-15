package graalvm.compiler.virtual.nodes;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.virtual.EscapeObjectState;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

/**
 * This class encapsulated the materialized state of an escape analyzed object.
 */
@NodeInfo
public final class MaterializedObjectState extends EscapeObjectState implements Node.ValueNumberable {

    public static final NodeClass<MaterializedObjectState> TYPE = NodeClass.create(MaterializedObjectState.class);
    @Input ValueNode materializedValue;

    public ValueNode materializedValue() {
        return materializedValue;
    }

    public MaterializedObjectState(VirtualObjectNode object, ValueNode materializedValue) {
        super(TYPE, object);
        this.materializedValue = materializedValue;
    }

    @Override
    public MaterializedObjectState duplicateWithVirtualState() {
        return graph().addWithoutUnique(new MaterializedObjectState(object(), materializedValue));
    }

    @Override
    public void applyToNonVirtual(NodeClosure<? super ValueNode> closure) {
        closure.apply(this, materializedValue);
    }
}
