package graalvm.compiler.truffle.compiler.hotspot;

import graalvm.compiler.hotspot.HotSpotCompilationIdentifier;
import graalvm.compiler.truffle.common.CompilableTruffleAST;

import jdk.vm.ci.hotspot.HotSpotCompilationRequest;

/**
 * A {@link HotSpotCompilationIdentifier} for Truffle compilations.
 */
public class HotSpotTruffleCompilationIdentifier extends HotSpotCompilationIdentifier {

    private final CompilableTruffleAST compilable;

    public HotSpotTruffleCompilationIdentifier(HotSpotCompilationRequest request, CompilableTruffleAST compilable) {
        super(request);
        this.compilable = compilable;
    }

    @Override
    public String toString(Verbosity verbosity) {
        return buildString(new StringBuilder(), verbosity).toString();
    }

    @Override
    protected StringBuilder buildName(StringBuilder sb) {
        return sb.append(compilable.toString());
    }

    @Override
    protected StringBuilder buildID(StringBuilder sb) {
        return super.buildID(sb.append("Truffle"));
    }

}
