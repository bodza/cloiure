package graalvm.compiler.truffle.runtime;

import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RepeatingNode;

public interface LoopNodeFactory extends PrioritizedServiceProvider {

    LoopNode create(RepeatingNode repeatingNode);

}
