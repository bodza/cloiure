package graalvm.compiler.truffle.compiler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graalvm.compiler.asm.Assembler;
import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.asm.CompilationResultBuilderFactory;
import graalvm.compiler.lir.asm.DataBuilder;
import graalvm.compiler.lir.asm.FrameContext;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.truffle.compiler.nodes.TruffleAssumption;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.meta.Assumptions.Assumption;

/**
 * A mechanism for Truffle to update a {@link CompilationResult} before it is
 * {@linkplain CompilationResult#close() closed} by the Graal compiler.
 */
class TruffleCompilationResultBuilderFactory implements CompilationResultBuilderFactory {

    /**
     * The graph being compiled.
     */
    private final StructuredGraph graph;

    /**
     * List into which {@link TruffleAssumption}s are added.
     */
    private final List<TruffleAssumption> validAssumptions;

    TruffleCompilationResultBuilderFactory(StructuredGraph graph, List<TruffleAssumption> validAssumptions) {
        this.graph = graph;
        this.validAssumptions = validAssumptions;
    }

    @Override
    public CompilationResultBuilder createBuilder(CodeCacheProvider codeCache, ForeignCallsProvider foreignCalls, FrameMap frameMap, Assembler asm, DataBuilder dataBuilder, FrameContext frameContext,
                    OptionValues options, DebugContext debug, CompilationResult compilationResult) {
        return new CompilationResultBuilder(codeCache, foreignCalls, frameMap, asm, dataBuilder, frameContext, options, debug, compilationResult) {
            @Override
            protected void closeCompilationResult() {
                CompilationResult result = this.compilationResult;
                result.setMethods(graph.method(), graph.getMethods());
                result.setBytecodeSize(graph.getBytecodeSize());

                Set<Assumption> newAssumptions = new HashSet<>();
                for (Assumption assumption : graph.getAssumptions()) {
                    TruffleCompilationResultBuilderFactory.processAssumption(newAssumptions, assumption, validAssumptions);
                }

                if (result.getAssumptions() != null) {
                    for (Assumption assumption : result.getAssumptions()) {
                        TruffleCompilationResultBuilderFactory.processAssumption(newAssumptions, assumption, validAssumptions);
                    }
                }

                result.setAssumptions(newAssumptions.toArray(new Assumption[newAssumptions.size()]));
                super.closeCompilationResult();
            }
        };
    }

    static void processAssumption(Set<Assumption> newAssumptions, Assumption assumption, List<TruffleAssumption> manual) {
        if (assumption != null) {
            if (assumption instanceof TruffleAssumption) {
                TruffleAssumption assumptionValidAssumption = (TruffleAssumption) assumption;
                manual.add(assumptionValidAssumption);
            } else {
                newAssumptions.add(assumption);
            }
        }
    }
}
