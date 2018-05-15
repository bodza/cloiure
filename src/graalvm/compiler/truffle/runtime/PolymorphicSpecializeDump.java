package graalvm.compiler.truffle.runtime;

import com.oracle.truffle.api.dsl.Introspection;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import graalvm.compiler.debug.DebugContext;
import graalvm.graphio.GraphOutput;
import graalvm.graphio.GraphStructure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.getOptions;

class PolymorphicSpecializeDump {

    public static void dumpPolymorphicSpecialize(List<Node> toDump, List<OptimizedDirectCallNode> knownCallNodes) {
        assert toDump.size() > 0;
        assert knownCallNodes.size() > 0;
        final DebugContext debugContext = DebugContext.create(getOptions(), Collections.emptyList());
        try {
            Collections.reverse(toDump);
            PolymorphicSpecializeDump.PolymorphicSpecializeGraph graph = new PolymorphicSpecializeDump.PolymorphicSpecializeGraph(knownCallNodes, toDump);
            final GraphOutput<PolymorphicSpecializeGraph, ?> output = debugContext.buildOutput(
                            GraphOutput.newBuilder(new PolymorphicSpecializeDump.PolymorphicSpecializeGraphStructure()).protocolVersion(6, 0));
            output.beginGroup(graph, "Polymorphic Specialize [" + knownCallNodes.get(0).getCurrentCallTarget() + "]", "Polymorphic Specialize", null, 0, null);
            output.print(graph, null, 0, toDump.get(toDump.size() - 1).toString());
            output.endGroup();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class PolymorphicSpecializeGraph {
        int idCounter = 0;
        final List<DumpNode> nodes = new ArrayList<>();

        class DumpNode {

            DumpNode(Node node) {
                this.node = node;
            }

            final Node node;

            final int id = idCounter++;
            DumpEdge edge;
            DumpNodeClass nodeClass;

            void setNewClass() {
                nodeClass = new DumpNodeClass(this);
            }
        }

        static class DumpNodeClass {
            final DumpNode node;

            DumpNodeClass(DumpNode node) {
                this.node = node;
            }
        }

        class DumpEdge {
            DumpEdge(DumpNode node) {
                this.node = node;
            }

            final DumpNode node;
        }

        enum DumpEdgeEnum {
            CHILD
        }

        DumpNode makeNode(Node node) {
            DumpNode n = new DumpNode(node);
            n.setNewClass();
            nodes.add(n);
            return n;
        }

        PolymorphicSpecializeGraph(List<OptimizedDirectCallNode> needsSplitCallNodes, List<Node> nodeChain) {
            DumpNode last = null;
            for (int i = 0; i < nodeChain.size(); i++) {
                if (i == 0) {
                    for (OptimizedDirectCallNode callNode : needsSplitCallNodes) {
                        makeNode(callNode);
                    }
                    last = makeNode(nodeChain.get(i));
                    for (DumpNode dumpNode : nodes) {
                        dumpNode.edge = new DumpEdge(last);
                    }
                } else {
                    DumpNode n = makeNode(nodeChain.get(i));
                    last.edge = new DumpEdge(n);
                    last = n;
                }
            }
        }
    }

    static class PolymorphicSpecializeGraphStructure
                    implements GraphStructure<PolymorphicSpecializeGraph, PolymorphicSpecializeGraph.DumpNode, PolymorphicSpecializeGraph.DumpNodeClass, PolymorphicSpecializeGraph.DumpEdge> {

        @Override
        public PolymorphicSpecializeGraph graph(PolymorphicSpecializeGraph currentGraph, Object obj) {
            return (obj instanceof PolymorphicSpecializeGraph) ? (PolymorphicSpecializeGraph) obj : null;
        }

        @Override
        public Iterable<? extends PolymorphicSpecializeGraph.DumpNode> nodes(PolymorphicSpecializeGraph graph) {
            return graph.nodes;
        }

        @Override
        public int nodesCount(PolymorphicSpecializeGraph graph) {
            return graph.nodes.size();
        }

        @Override
        public int nodeId(PolymorphicSpecializeGraph.DumpNode node) {
            return node.id;
        }

        @Override
        public boolean nodeHasPredecessor(PolymorphicSpecializeGraph.DumpNode node) {
            return false;
        }

        @Override
        public void nodeProperties(PolymorphicSpecializeGraph graph, PolymorphicSpecializeGraph.DumpNode node, Map<String, ? super Object> properties) {
            properties.put("label", node.node.toString());
            properties.put("ROOT?", node.node instanceof RootNode);
            properties.put("LEAF?", node.edge == null);
            properties.put("RootNode", node.node.getRootNode());
            properties.putAll(node.node.getDebugProperties());
            properties.put("SourceSection", node.node.getSourceSection());
            if (Introspection.isIntrospectable(node.node)) {
                final List<Introspection.SpecializationInfo> specializations = Introspection.getSpecializations(node.node);
                for (Introspection.SpecializationInfo specialization : specializations) {
                    properties.put(specialization.getMethodName() + ".isActive", specialization.isActive());
                    properties.put(specialization.getMethodName() + ".isExcluded", specialization.isExcluded());
                    properties.put(specialization.getMethodName() + ".instances", specialization.getInstances());
                }
            }
        }

        @Override
        public PolymorphicSpecializeGraph.DumpNode node(Object obj) {
            return (obj instanceof PolymorphicSpecializeGraph.DumpNode) ? (PolymorphicSpecializeGraph.DumpNode) obj : null;
        }

        @Override
        public PolymorphicSpecializeGraph.DumpNodeClass nodeClass(Object obj) {
            return (obj instanceof PolymorphicSpecializeGraph.DumpNodeClass) ? (PolymorphicSpecializeGraph.DumpNodeClass) obj : null;

        }

        @Override
        public PolymorphicSpecializeGraph.DumpNodeClass classForNode(PolymorphicSpecializeGraph.DumpNode node) {
            return node.nodeClass;
        }

        @Override
        public String nameTemplate(PolymorphicSpecializeGraph.DumpNodeClass nodeClass) {
            return "{p#label}";
        }

        @Override
        public Object nodeClassType(PolymorphicSpecializeGraph.DumpNodeClass nodeClass) {
            return nodeClass.getClass();
        }

        @Override
        public PolymorphicSpecializeGraph.DumpEdge portInputs(PolymorphicSpecializeGraph.DumpNodeClass nodeClass) {
            return null;
        }

        @Override
        public PolymorphicSpecializeGraph.DumpEdge portOutputs(PolymorphicSpecializeGraph.DumpNodeClass nodeClass) {
            return nodeClass.node.edge;
        }

        @Override
        public int portSize(PolymorphicSpecializeGraph.DumpEdge port) {
            return port == null ? 0 : 1;
        }

        @Override
        public boolean edgeDirect(PolymorphicSpecializeGraph.DumpEdge port, int index) {
            return port != null;
        }

        @Override
        public String edgeName(PolymorphicSpecializeGraph.DumpEdge port, int index) {
            return "";
        }

        @Override
        public Object edgeType(PolymorphicSpecializeGraph.DumpEdge port, int index) {
            return PolymorphicSpecializeGraph.DumpEdgeEnum.CHILD;
        }

        @Override
        public Collection<? extends PolymorphicSpecializeGraph.DumpNode> edgeNodes(PolymorphicSpecializeGraph graph, PolymorphicSpecializeGraph.DumpNode node, PolymorphicSpecializeGraph.DumpEdge port,
                        int index) {
            return Collections.singleton(node.edge.node);
        }
    }
}
