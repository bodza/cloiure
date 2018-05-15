package graalvm.compiler.truffle.runtime.debug;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleSplitting;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntime;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import graalvm.compiler.truffle.runtime.OptimizedDirectCallNode;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class TraceSplittingListener implements GraalTruffleRuntimeListener {

    private TraceSplittingListener() {
    }

    public static void install(GraalTruffleRuntime runtime) {
        if (TruffleCompilerOptions.getValue(TraceTruffleSplitting)) {
            runtime.addListener(new TraceSplittingListener());
        }
    }

    private int splitCount;

    @Override
    public void onCompilationSplit(OptimizedDirectCallNode callNode) {
        OptimizedCallTarget callTarget = callNode.getCallTarget();
        String label = String.format("split %3s-%-4s-%-4s ", splitCount++, Integer.toHexString(callNode.getCurrentCallTarget().hashCode()), callNode.getCallCount());
        final Map<String, Object> debugProperties = callTarget.getDebugProperties(null);
        debugProperties.put("SourceSection", extractSourceSection(callNode));
        TruffleCompilerRuntime.getRuntime().logEvent(0, label, callTarget.toString(), debugProperties);
    }

    private static String extractSourceSection(OptimizedDirectCallNode node) {
        Node cnode = node;
        while (cnode.getSourceSection() == null && !(cnode instanceof RootNode)) {
            cnode = cnode.getParent();
            if (cnode == null) {
                return "";
            }
        }
        return getShortDescription(cnode.getSourceSection());
    }

    static String getShortDescription(SourceSection sourceSection) {
        if (sourceSection.getSource() == null) {
            // TODO the source == null branch can be removed if the deprecated
            // SourceSection#createUnavailable has be removed.
            return "<Unknown>";
        }
        StringBuilder b = new StringBuilder();
        if (sourceSection.getSource().getPath() == null) {
            b.append(sourceSection.getSource().getName());
        } else {
            Path pathAbsolute = Paths.get(sourceSection.getSource().getPath());
            Path pathBase = new File("").getAbsoluteFile().toPath();
            try {
                Path pathRelative = pathBase.relativize(pathAbsolute);
                b.append(pathRelative.toFile());
            } catch (IllegalArgumentException e) {
                b.append(sourceSection.getSource().getName());
            }
        }

        b.append("~").append(formatIndices(sourceSection, true));
        return b.toString();
    }

    static String formatIndices(SourceSection sourceSection, boolean needsColumnSpecifier) {
        StringBuilder b = new StringBuilder();
        boolean singleLine = sourceSection.getStartLine() == sourceSection.getEndLine();
        if (singleLine) {
            b.append(sourceSection.getStartLine());
        } else {
            b.append(sourceSection.getStartLine()).append("-").append(sourceSection.getEndLine());
        }
        if (needsColumnSpecifier) {
            b.append(":");
            if (sourceSection.getCharLength() <= 1) {
                b.append(sourceSection.getCharIndex());
            } else {
                b.append(sourceSection.getCharIndex()).append("-").append(sourceSection.getCharIndex() + sourceSection.getCharLength() - 1);
            }
        }
        return b.toString();
    }

}
