package graalvm.compiler.lir.framemap;

import graalvm.compiler.lir.LIRFrameState;

import jdk.vm.ci.code.ReferenceMap;
import jdk.vm.ci.meta.Value;

public abstract class ReferenceMapBuilder
{
    public abstract void addLiveValue(Value value);

    public abstract ReferenceMap finish(LIRFrameState state);
}
