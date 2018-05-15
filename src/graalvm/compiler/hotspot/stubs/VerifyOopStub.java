package graalvm.compiler.hotspot.stubs;

import static graalvm.compiler.hotspot.stubs.StubUtil.verifyObject;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.options.OptionValues;

/**
 * Stub called via {@link HotSpotHostForeignCallsProvider#VERIFY_OOP}.
 */
public class VerifyOopStub extends SnippetStub {

    public VerifyOopStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage) {
        super("verifyOop", options, providers, linkage);
    }

    @Snippet
    private static Object verifyOop(Object object) {
        return verifyObject(object);
    }
}
