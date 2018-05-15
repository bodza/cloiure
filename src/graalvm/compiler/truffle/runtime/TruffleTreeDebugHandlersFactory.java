package graalvm.compiler.truffle.runtime;

import java.util.Collections;
import java.util.List;

import graalvm.compiler.debug.DebugHandler;
import graalvm.compiler.debug.DebugHandlersFactory;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.serviceprovider.ServiceProvider;

@ServiceProvider(DebugHandlersFactory.class)
public class TruffleTreeDebugHandlersFactory implements DebugHandlersFactory {

    @Override
    public List<DebugHandler> createHandlers(OptionValues options) {
        return Collections.singletonList(new TruffleTreeDumpHandler(options));
    }
}
