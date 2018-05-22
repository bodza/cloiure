package giraaff.hotspot.meta;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.hotspot.nodes.profiling.ProfileBranchNode;
import giraaff.hotspot.nodes.profiling.ProfileInvokeNode;
import giraaff.hotspot.nodes.profiling.ProfileNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.LogicNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.ProfilingPlugin;
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;

public abstract class HotSpotProfilingPlugin implements ProfilingPlugin
{
    public static class Options
    {
        // Option "Emit profiling of invokes."
        public static final OptionKey<Boolean> ProfileInvokes = new OptionKey<>(true);
        // Option "Emit profiling of backedges."
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
