package graalvm.compiler.hotspot.meta;

import graalvm.compiler.hotspot.nodes.profiling.ProfileBranchNode;
import graalvm.compiler.hotspot.nodes.profiling.ProfileInvokeNode;
import graalvm.compiler.hotspot.nodes.profiling.ProfileNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.ConditionalNode;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import graalvm.compiler.nodes.graphbuilderconf.ProfilingPlugin;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public abstract class HotSpotProfilingPlugin implements ProfilingPlugin
{
    public static class Options
    {
        @Option(help = "Emit profiling of invokes", type = OptionType.Expert)
        public static final OptionKey<Boolean> ProfileInvokes = new OptionKey<>(true);
        @Option(help = "Emit profiling of backedges", type = OptionType.Expert)
        public static final OptionKey<Boolean> ProfileBackedges = new OptionKey<>(true);
    }

    public abstract int invokeNotifyFreqLog(OptionValues options);

    public abstract int invokeInlineeNotifyFreqLog(OptionValues options);

    public abstract int invokeProfilePobabilityLog(OptionValues options);

    public abstract int backedgeNotifyFreqLog(OptionValues options);

    public abstract int backedgeProfilePobabilityLog(OptionValues options);

    @Override
    public boolean shouldProfile(GraphBuilderContext builder, ResolvedJavaMethod method)
    {
        return !builder.parsingIntrinsic();
    }

    @Override
    public void profileInvoke(GraphBuilderContext builder, ResolvedJavaMethod method, FrameState frameState)
    {
        OptionValues options = builder.getOptions();
        if (Options.ProfileInvokes.getValue(options) && !method.isClassInitializer())
        {
            ProfileNode p = builder.append(new ProfileInvokeNode(method, invokeNotifyFreqLog(options), invokeProfilePobabilityLog(options)));
            p.setStateBefore(frameState);
        }
    }

    @Override
    public void profileGoto(GraphBuilderContext builder, ResolvedJavaMethod method, int bci, int targetBci, FrameState frameState)
    {
        OptionValues options = builder.getOptions();
        if (Options.ProfileBackedges.getValue(options) && targetBci <= bci)
        {
            ProfileNode p = builder.append(new ProfileBranchNode(method, backedgeNotifyFreqLog(options), backedgeProfilePobabilityLog(options), bci, targetBci));
            p.setStateBefore(frameState);
        }
    }

    @Override
    public void profileIf(GraphBuilderContext builder, ResolvedJavaMethod method, int bci, LogicNode condition, int trueBranchBci, int falseBranchBci, FrameState frameState)
    {
        OptionValues options = builder.getOptions();
        if (Options.ProfileBackedges.getValue(options) && (falseBranchBci <= bci || trueBranchBci <= bci))
        {
            boolean negate = false;
            int targetBci = trueBranchBci;
            if (falseBranchBci <= bci)
            {
                negate = true;
                targetBci = falseBranchBci;
            }
            ValueNode trueValue = builder.append(ConstantNode.forBoolean(!negate));
            ValueNode falseValue = builder.append(ConstantNode.forBoolean(negate));
            ConditionalNode branchCondition = builder.append(new ConditionalNode(condition, trueValue, falseValue));
            ProfileNode p = builder.append(new ProfileBranchNode(method, backedgeNotifyFreqLog(options), backedgeProfilePobabilityLog(options), branchCondition, bci, targetBci));
            p.setStateBefore(frameState);
        }
    }
}
