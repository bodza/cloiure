package graalvm.compiler.truffle.compiler.hotspot;

import graalvm.compiler.serviceprovider.ServiceProvider;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompiler;

@ServiceProvider(HotSpotTruffleCompiler.Factory.class)
public class HotSpotTruffleCompilerFactory extends HotSpotTruffleCompiler.Factory {

    @Override
    public HotSpotTruffleCompiler create(TruffleCompilerRuntime runtime) {
        return HotSpotTruffleCompilerImpl.create(runtime);
    }

}
