package graalvm.compiler.core.amd64;

import graalvm.compiler.java.DefaultSuitesCreator;
import graalvm.compiler.lir.amd64.phases.StackMoveOptimizationPhase;
import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.tiers.CompilerConfiguration;

public class AMD64SuitesCreator extends DefaultSuitesCreator {

    public AMD64SuitesCreator(CompilerConfiguration compilerConfiguration, Plugins plugins) {
        super(compilerConfiguration, plugins);
    }

    @Override
    public LIRSuites createLIRSuites(OptionValues options) {
        LIRSuites lirSuites = super.createLIRSuites(options);
        if (StackMoveOptimizationPhase.Options.LIROptStackMoveOptimizer.getValue(options)) {
            /* Note: this phase must be inserted <b>after</b> RedundantMoveElimination */
            lirSuites.getPostAllocationOptimizationStage().appendPhase(new StackMoveOptimizationPhase());
        }
        return lirSuites;
    }
}
