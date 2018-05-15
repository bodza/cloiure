package graalvm.graphio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class GraphJavadocSnippets {
    static GraphStructure<AcmeGraph, AcmeNode, AcmeNodeType, AcmePorts> acmeGraphStructure() {
        // @formatter:off
        // BEGIN: graalvm.graphio.GraphJavadocSnippets#acmeGraphStructure
        class AcmeGraphStructure implements
        GraphStructure<AcmeGraph, AcmeNode, AcmeNodeType, AcmePorts> {

            @Override
            public AcmeGraph graph(AcmeGraph currentGraph, Object obj) {
                return obj instanceof AcmeGraph ? (AcmeGraph) obj : null;
            }

            @Override
            public Iterable<? extends AcmeNode> nodes(AcmeGraph graph) {
                return graph.allNodes();
            }

            @Override
            public int nodesCount(AcmeGraph graph) {
                return graph.allNodes().size();
            }

            @Override
            public int nodeId(AcmeNode node) {
                return node.id;
            }

            @Override
            public boolean nodeHasPredecessor(AcmeNode node) {
                return node.id > 0;
            }

            @Override
            public void nodeProperties(
                AcmeGraph graph, AcmeNode node, Map<String, ? super Object> properties
            ) {
                properties.put("id", node.id);
            }

            @Override
            public AcmeNodeType nodeClass(Object obj) {
                return obj instanceof AcmeNodeType ? (AcmeNodeType) obj : null;
            }

            @Override
            public AcmeNode node(Object obj) {
                return obj instanceof AcmeNode ? (AcmeNode) obj : null;
            }

            @Override
            public AcmeNodeType classForNode(AcmeNode node) {
                // we have only one type of nodes
                return AcmeNodeType.STANDARD;
            }


            @Override
            public String nameTemplate(AcmeNodeType nodeClass) {
                return "Acme ({p#id})";
            }

            @Override
            public Object nodeClassType(AcmeNodeType nodeClass) {
                return nodeClass.getClass();
            }

            @Override
            public AcmePorts portInputs(AcmeNodeType nodeClass) {
                return AcmePorts.INPUT;
            }

            @Override
            public AcmePorts portOutputs(AcmeNodeType nodeClass) {
                return AcmePorts.OUTPUT;
            }

            @Override
            public int portSize(AcmePorts port) {
                return port == AcmePorts.OUTPUT ? 1 : 0;
            }

            @Override
            public boolean edgeDirect(AcmePorts port, int index) {
                return false;
            }

            @Override
            public String edgeName(AcmePorts port, int index) {
                return port.name();
            }

            @Override
            public Object edgeType(AcmePorts port, int index) {
                return port;
            }

            @Override
            public Collection<? extends AcmeNode> edgeNodes(
                AcmeGraph graph, AcmeNode node, AcmePorts port, int index
            ) {
                if (port == AcmePorts.OUTPUT) {
                    return node.outgoing.targets;
                }
                return null;
            }
        }

        // END: graalvm.graphio.GraphJavadocSnippets#acmeGraphStructure

        return new AcmeGraphStructure();
    }

    // BEGIN: graalvm.graphio.GraphJavadocSnippets#buildOutput
    static GraphOutput<AcmeGraph, ?> buildOutput(WritableByteChannel channel)
    throws IOException {
        return GraphOutput.newBuilder(acmeGraphStructure()).
            // use the latest version; currently 6.0
            protocolVersion(6, 0).
            build(channel);
    }
    // END: graalvm.graphio.GraphJavadocSnippets#buildOutput

    // BEGIN: graalvm.graphio.GraphJavadocSnippets#buildAll
    static GraphOutput<AcmeGraph, ?> buildAll(WritableByteChannel channel)
    throws IOException {
        GraphBlocks<AcmeGraph, AcmeBlocks, AcmeNode> graphBlocks = acmeBlocks();
        GraphElements<AcmeMethod, AcmeField,
            AcmeSignature, AcmeCodePosition> graphElements = acmeElements();
        GraphTypes graphTypes = acmeTypes();

        return GraphOutput.newBuilder(acmeGraphStructure()).
            protocolVersion(6, 0).
            blocks(graphBlocks).
            elements(graphElements).
            types(graphTypes).
            build(channel);
    }
    // END: graalvm.graphio.GraphJavadocSnippets#buildAll

    private static GraphTypes acmeTypes() {
        GraphTypes graphTypes = null;
        // in real world don't return null
        return graphTypes;
    }

    private static GraphElements<AcmeMethod, AcmeField, AcmeSignature, AcmeCodePosition> acmeElements() {
        GraphElements<AcmeMethod, AcmeField, AcmeSignature, AcmeCodePosition> graphElements = null;
        // in real world don't return null
        return graphElements;
    }

    private static GraphBlocks<AcmeGraph, AcmeBlocks, AcmeNode> acmeBlocks() {
        GraphBlocks<AcmeGraph, AcmeBlocks, AcmeNode> graphBlocks = null;
        // in real world don't return null
        return graphBlocks;
    }

    private static class AcmeGraph {
        final AcmeNode root;

        AcmeGraph(AcmeNode root) {
            this.root = root;
        }

        Set<AcmeNode> allNodes() {
            return allNodes(root, new LinkedHashSet<>());
        }

        private static Set<AcmeNode> allNodes(AcmeNode node, Set<AcmeNode> collectTo) {
            if (collectTo.add(node)) {
                for (AcmeNode target : node.outgoing.targets) {
                    allNodes(target, collectTo);
                }
            }
            return collectTo;
        }
    }

    private static class AcmeNode {
        final int id;
        final AcmeEdges outgoing;

        AcmeNode(int id) {
            this.id = id;
            this.outgoing = new AcmeEdges();
        }

        void linkTo(AcmeNode target) {
            outgoing.targets.add(target);
        }
    }

    private enum AcmeNodeType {
        STANDARD
    }

    private enum AcmePorts {
        INPUT,
        OUTPUT;
    }

    private static class AcmeEdges {
        final Set<AcmeNode> targets;

        AcmeEdges() {
            this.targets = new LinkedHashSet<>();
        }
    }

    private static class AcmeBlocks {
    }

    private static class AcmeMethod {
    }

    private static class AcmeField {
    }

    private static class AcmeSignature {
    }

    private static class AcmeCodePosition {
    }

    // BEGIN: graalvm.graphio.GraphJavadocSnippets#dump
    static void dump(File toFile) throws IOException {
        try (
            FileChannel ch = new FileOutputStream(toFile).getChannel();
            GraphOutput<AcmeGraph, ?> output = buildOutput(ch);
        ) {
            AcmeNode root = new AcmeNode(0);
            AcmeNode n1 = new AcmeNode(1);
            AcmeNode n2 = new AcmeNode(2);
            AcmeNode n3 = new AcmeNode(3);

            root.linkTo(n1);
            root.linkTo(n2);
            n1.linkTo(n3);
            n2.linkTo(n3);

            AcmeGraph diamondGraph = new AcmeGraph(root);

            output.beginGroup(diamondGraph, "Diamond", "dia", null, 0, null);
            output.print(diamondGraph, null, 0, "Diamond graph #%d", 1);
            output.endGroup();
        }
    }
    // END: graalvm.graphio.GraphJavadocSnippets#dump

}
