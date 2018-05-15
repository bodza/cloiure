package graalvm.compiler.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RepeatingNode;

public final class OptimizedLoopNode extends LoopNode {

    @Child private RepeatingNode repeatingNode;

    public OptimizedLoopNode(RepeatingNode repeatingNode) {
        this.repeatingNode = repeatingNode;
    }

    @Override
    public RepeatingNode getRepeatingNode() {
        return repeatingNode;
    }

    @Override
    public void executeLoop(VirtualFrame frame) {
        int loopCount = 0;
        try {
            while (repeatingNode.executeRepeating(frame)) {
                if (CompilerDirectives.inInterpreter()) {
                    loopCount++;
                }
            }
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                reportLoopCount(this, loopCount);
            }
        }
    }

    static LoopNode create(RepeatingNode repeatingNode) {
        return new OptimizedLoopNode(repeatingNode);
    }

}
