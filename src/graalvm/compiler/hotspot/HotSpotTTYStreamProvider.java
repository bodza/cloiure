package graalvm.compiler.hotspot;

import static graalvm.compiler.hotspot.HotSpotGraalOptionValues.HOTSPOT_OPTIONS;

import java.io.PrintStream;

import graalvm.compiler.debug.TTYStreamProvider;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.serviceprovider.ServiceProvider;

@ServiceProvider(TTYStreamProvider.class)
public class HotSpotTTYStreamProvider implements TTYStreamProvider {

    public static class Options {

        // @formatter:off
        @Option(help = "File to which logging is sent.  A %p in the name will be replaced with a string identifying " +
                       "the process, usually the process id and %t will be replaced by System.currentTimeMillis().", type = OptionType.Expert)
        public static final PrintStreamOptionKey LogFile = new PrintStreamOptionKey();
        // @formatter:on
    }

    @Override
    public PrintStream getStream() {
        return Options.LogFile.getStream(HOTSPOT_OPTIONS);
    }
}
