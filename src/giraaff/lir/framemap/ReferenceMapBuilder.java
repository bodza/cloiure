package giraaff.lir.framemap;

import jdk.vm.ci.code.ReferenceMap;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRFrameState;

public abstract class ReferenceMapBuilder
{
    public abstract void addLiveValue(Value value);

    public abstract ReferenceMap finish(LIRFrameState state);
}
