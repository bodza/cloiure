package graalvm.compiler.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;

public class HotSpotAOTProfilingPlugin extends HotSpotProfilingPlugin
{
    public static class Options
    {
        // Option "Do profiling and callbacks to tiered runtime."
        public static final OptionKey<Boolean> TieredAOT = new OptionKey<>(false);
        // Option "Invocation notification frequency."
        public static final OptionKey<Integer> TierAInvokeNotifyFreqLog = new OptionKey<>(13);
        // Option "Inlinee invocation notification frequency (-1 means count, but do not notify)."
        public static final OptionKey<Integer> TierAInvokeInlineeNotifyFreqLog = new OptionKey<>(-1);
        // Option "Invocation profile probability."
        public static final OptionKey<Integer> TierAInvokeProfileProbabilityLog = new OptionKey<>(8);
        // Option "Backedge notification frequency."
        public static final OptionKey<Integer> TierABackedgeNotifyFreqLog = new OptionKey<>(16);
        // Option "Backedge profile probability."
        public static final OptionKey<Integer> TierABackedgeProfileProbabilityLog = new OptionKey<>(12);
    }

    @Override
    public boolean shouldProfile(GraphBuilderContext builder, ResolvedJavaMethod method)
    {
        return super.shouldProfile(builder, method) && ((HotSpotResolvedObjectType) method.getDeclaringClass()).getFingerprint() != 0;
    }

    @Override
    public int invokeNotifyFreqLog(OptionValues options)
    {
        return Options.TierAInvokeNotifyFreqLog.getValue(options);
    }

    @Override
    public int invokeInlineeNotifyFreqLog(OptionValues options)
    {
        return Options.TierAInvokeInlineeNotifyFreqLog.getValue(options);
    }

    @Override
    public int invokeProfilePobabilityLog(OptionValues options)
    {
        return Options.TierAInvokeProfileProbabilityLog.getValue(options);
    }

    @Override
    public int backedgeNotifyFreqLog(OptionValues options)
    {
        return Options.TierABackedgeNotifyFreqLog.getValue(options);
    }

    @Override
    public int backedgeProfilePobabilityLog(OptionValues options)
    {
        return Options.TierABackedgeProfileProbabilityLog.getValue(options);
    }
}
