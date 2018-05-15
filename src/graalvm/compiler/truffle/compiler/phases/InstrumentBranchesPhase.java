package graalvm.compiler.truffle.compiler.phases;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeSourcePosition;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.tiers.PhaseContext;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;

import jdk.vm.ci.meta.JavaConstant;

/**
 * Instruments {@link IfNode}s in the graph, by adding execution counters to the true and the false
 * branch of each {@link IfNode}. If this phase is enabled, the runtime outputs a summary of all the
 * compiled {@link IfNode}s and the execution count of their branches, when the program exits.
 *
 * The phase is enabled with the following flag:
 *
 * <pre>
 * -Dgraal.TruffleInstrumentBranches
 * </pre>
 *
 * The phase can be configured to only instrument the {@link IfNode}s in specific methods, by
 * providing the following method filter flag:
 *
 * <pre>
 * -Dgraal.TruffleInstrumentBranchesFilter
 * </pre>
 *
 * The flag:
 *
 * <pre>
 * -Dgraal.TruffleInstrumentBranchesPerInlineSite
 * </pre>
 *
 * decides whether to treat different inlining sites separately when tracking the execution counts
 * of an {@link IfNode}.
 */
public class InstrumentBranchesPhase extends InstrumentPhase {

    public InstrumentBranchesPhase(OptionValues options, SnippetReflectionProvider snippetReflection, Instrumentation instrumentation) {
        super(options, snippetReflection, instrumentation);
    }

    @Override
    protected void instrumentGraph(StructuredGraph graph, PhaseContext context, JavaConstant tableConstant) {
        for (IfNode n : graph.getNodes().filter(IfNode.class)) {
            Point p = getOrCreatePoint(n);
            if (p != null) {
                insertCounter(graph, context, tableConstant, n.trueSuccessor(), p.slotIndex(0));
                insertCounter(graph, context, tableConstant, n.falseSuccessor(), p.slotIndex(1));
            }
        }
    }

    @Override
    protected int instrumentationPointSlotCount() {
        return 2;
    }

    @Override
    protected boolean instrumentPerInlineSite(OptionValues options) {
        return TruffleCompilerOptions.TruffleInstrumentBranchesPerInlineSite.getValue(options);
    }

    @Override
    protected Point createPoint(int id, int startIndex, Node n) {
        return new IfPoint(id, startIndex, n.getNodeSourcePosition());
    }

    public enum BranchState {
        NONE,
        IF,
        ELSE,
        BOTH;

        public static BranchState from(boolean ifVisited, boolean elseVisited) {
            if (ifVisited && elseVisited) {
                return BOTH;
            } else if (ifVisited && !elseVisited) {
                return IF;
            } else if (!ifVisited && elseVisited) {
                return ELSE;
            } else {
                return NONE;
            }
        }
    }

    public class IfPoint extends InstrumentPhase.Point {
        IfPoint(int id, int rawIndex, NodeSourcePosition position) {
            super(id, rawIndex, position);
        }

        @Override
        public int slotCount() {
            return 2;
        }

        @Override
        public boolean isPrettified(OptionValues options) {
            return TruffleCompilerOptions.TruffleInstrumentBranchesPerInlineSite.getValue(options);
        }

        public long ifVisits() {
            return getInstrumentation().getAccessTable()[rawIndex];
        }

        public long elseVisits() {
            return getInstrumentation().getAccessTable()[rawIndex + 1];
        }

        public BranchState getBranchState() {
            return BranchState.from(ifVisits() > 0, elseVisits() > 0);
        }

        public String getCounts() {
            return "if=" + ifVisits() + "#, else=" + elseVisits() + "#";
        }

        @Override
        public long getHotness() {
            return ifVisits() + elseVisits();
        }

        @Override
        public String toString() {
            return "[" + id + "] state = " + getBranchState() + "(" + getCounts() + ")";
        }
    }
}
