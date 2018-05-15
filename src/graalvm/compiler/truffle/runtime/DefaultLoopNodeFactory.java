package graalvm.compiler.truffle.runtime;

import graalvm.compiler.serviceprovider.ServiceProvider;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RepeatingNode;

@ServiceProvider(LoopNodeFactory.class)
public class DefaultLoopNodeFactory implements LoopNodeFactory {

    @Override
    public LoopNode create(RepeatingNode repeatingNode) {
        return OptimizedOSRLoopNode.create(repeatingNode);
    }

}
