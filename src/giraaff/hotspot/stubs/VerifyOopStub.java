package giraaff.hotspot.stubs;

import giraaff.api.replacements.Snippet;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotHostForeignCallsProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.stubs.StubUtil;
import giraaff.options.OptionValues;

/**
 * Stub called via {@link HotSpotHostForeignCallsProvider#VERIFY_OOP}.
 */
public class VerifyOopStub extends SnippetStub
{
    public VerifyOopStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("verifyOop", options, providers, linkage);
    }

    @Snippet
    private static Object verifyOop(Object object)
    {
        return StubUtil.verifyObject(object);
    }
}
