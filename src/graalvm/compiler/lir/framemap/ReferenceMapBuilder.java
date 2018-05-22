package graalvm.compiler.lir.framemap;

import jdk.vm.ci.code.ReferenceMap;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.lir.LIRFrameState;

public abstract class ReferenceMapBuilder
{
    public abstract void addLiveValue(Value value);

    public abstract ReferenceMap finish(LIRFrameState state);
}
