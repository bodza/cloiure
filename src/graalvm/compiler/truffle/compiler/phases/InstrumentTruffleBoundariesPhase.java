package graalvm.compiler.truffle.compiler.phases;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeSourcePosition;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.tiers.PhaseContext;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;

import jdk.vm.ci.meta.JavaConstant;

/**
 * Instruments calls to {@code TruffleBoundary}-annotated methods in the graph, by adding execution
 * counters to respective callsites. If this phase is enabled, the runtime outputs a summary of all
 * such compiled callsites and their execution counts, when the program exits.
 *
 * The phase is enabled with the following flag:
 *
 * <pre>
 * -Dgraal.TruffleInstrumentBoundaries
 * </pre>
 *
 * The phase can be configured to only instrument callsites in specific methods, by providing the
 * following method filter flag:
 *
 * <pre>
 * -Dgraal.TruffleInstrumentBoundariesFilter
 * </pre>
 *
 * The flag:
 *
 * <pre>
 * -Dgraal.TruffleInstrumentBoundariesPerInlineSite
 * </pre>
 *
 * decides whether to treat different inlining sites separately when tracking the execution counts.
 */
public class InstrumentTruffleBoundariesPhase extends InstrumentPhase {

    public InstrumentTruffleBoundariesPhase(OptionValues options, SnippetReflectionProvider snippetReflection, Instrumentation instrumentation) {
        super(options, snippetReflection, instrumentation);
    }

    @Override
    protected void instrumentGraph(StructuredGraph graph, PhaseContext context, JavaConstant tableConstant) {
        TruffleCompilerRuntime runtime = TruffleCompilerRuntime.getRuntime();
        for (Node n : graph.getNodes()) {
            if (n instanceof Invoke && runtime.isTruffleBoundary(((Invoke) n).callTarget().targetMethod())) {
                Point p = getOrCreatePoint(n);
                if (p != null) {
                    insertCounter(graph, context, tableConstant, (FixedWithNextNode) n.predecessor(), p.slotIndex(0));
                }
            }
        }
    }

    @Override
    protected int instrumentationPointSlotCount() {
        return 1;
    }

    @Override
    protected boolean instrumentPerInlineSite(OptionValues options) {
        return TruffleCompilerOptions.TruffleInstrumentBoundariesPerInlineSite.getValue(options);
    }

    @Override
    protected Point createPoint(int id, int startIndex, Node n) {
        return new BoundaryPoint(id, startIndex, n.getNodeSourcePosition());
    }

    public class BoundaryPoint extends Point {
        BoundaryPoint(int id, int rawIndex, NodeSourcePosition position) {
            super(id, rawIndex, position);
        }

        @Override
        public int slotCount() {
            return 1;
        }

        @Override
        public boolean isPrettified(OptionValues options) {
            return TruffleCompilerOptions.TruffleInstrumentBoundariesPerInlineSite.getValue(options);
        }

        @Override
        public long getHotness() {
            return getInstrumentation().getAccessTable()[rawIndex];
        }

        @Override
        public String toString() {
            return "[" + id + "] count = " + getHotness();
        }
    }
}
