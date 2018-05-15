package graalvm.compiler.nodes;

import java.util.Iterator;
import java.util.NoSuchElementException;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.extended.AnchoringNode;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

@NodeInfo(allowedUsageTypes = {InputType.Guard, InputType.Anchor})
public abstract class AbstractBeginNode extends FixedWithNextNode implements LIRLowerable, GuardingNode, AnchoringNode, IterableNodeType {

    public static final NodeClass<AbstractBeginNode> TYPE = NodeClass.create(AbstractBeginNode.class);

    protected AbstractBeginNode(NodeClass<? extends AbstractBeginNode> c) {
        this(c, StampFactory.forVoid());
    }

    protected AbstractBeginNode(NodeClass<? extends AbstractBeginNode> c, Stamp stamp) {
        super(c, stamp);
    }

    public static AbstractBeginNode prevBegin(FixedNode from) {
        Node next = from;
        while (next != null) {
            if (next instanceof AbstractBeginNode) {
                AbstractBeginNode begin = (AbstractBeginNode) next;
                return begin;
            }
            next = next.predecessor();
        }
        return null;
    }

    private void evacuateGuards(FixedNode evacuateFrom) {
        if (!hasNoUsages()) {
            AbstractBeginNode prevBegin = prevBegin(evacuateFrom);
            assert prevBegin != null;
            for (Node anchored : anchored().snapshot()) {
                anchored.replaceFirstInput(this, prevBegin);
            }
        }
    }

    public void prepareDelete() {
        prepareDelete((FixedNode) predecessor());
    }

    public void prepareDelete(FixedNode evacuateFrom) {
        evacuateGuards(evacuateFrom);
    }

    @Override
    public boolean verify() {
        assertTrue(predecessor() != null || this == graph().start() || this instanceof AbstractMergeNode, "begin nodes must be connected");
        return super.verify();
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        // nop
    }

    public NodeIterable<GuardNode> guards() {
        return usages().filter(GuardNode.class);
    }

    public NodeIterable<Node> anchored() {
        return usages();
    }

    public NodeIterable<FixedNode> getBlockNodes() {
        return new NodeIterable<FixedNode>() {

            @Override
            public Iterator<FixedNode> iterator() {
                return new BlockNodeIterator(AbstractBeginNode.this);
            }
        };
    }

    private class BlockNodeIterator implements Iterator<FixedNode> {

        private FixedNode current;

        BlockNodeIterator(FixedNode next) {
            this.current = next;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public FixedNode next() {
            FixedNode ret = current;
            if (ret == null) {
                throw new NoSuchElementException();
            }
            if (current instanceof FixedWithNextNode) {
                current = ((FixedWithNextNode) current).next();
                if (current instanceof AbstractBeginNode) {
                    current = null;
                }
            } else {
                current = null;
            }
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
