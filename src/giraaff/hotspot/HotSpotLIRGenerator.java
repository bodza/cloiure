package giraaff.hotspot;

import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.LIRKind;
import giraaff.hotspot.meta.HotSpotConstantLoadAction;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.aot.ResolveMethodAndLoadCountersNode;
import giraaff.hotspot.nodes.profiling.RandomSeedNode;
import giraaff.hotspot.replacements.EncodedSymbolConstant;
import giraaff.lir.LIRFrameState;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.gen.LIRGenerator;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.util.GraalError;

/**
 * This interface defines the contract a HotSpot backend LIR generator needs to fulfill in addition
 * to abstract methods from {@link LIRGenerator} and {@link LIRGeneratorTool}.
 */
public interface HotSpotLIRGenerator extends LIRGeneratorTool
{
    /**
     * Emits an operation to make a tail call.
     *
     * @param args the arguments of the call
     * @param address the target address of the call
     */
    void emitTailcall(Value[] args, Value address);

    void emitDeoptimizeCaller(DeoptimizationAction action, DeoptimizationReason reason);

    /**
     * Emits code for a {@link LoadConstantIndirectlyNode}.
     *
     * @return value of loaded address in register
     */
    default Value emitLoadObjectAddress(Constant constant)
    {
        throw new GraalError("Emitting code to load an object address is not currently supported on %s", target().arch);
    }

    /**
     * Emits code for a {@link LoadConstantIndirectlyNode}.
     *
     * @param constant original constant
     * @param action action to perform on the metaspace object
     * @return Value of loaded address in register
     */
    default Value emitLoadMetaspaceAddress(Constant constant, HotSpotConstantLoadAction action)
    {
        throw new GraalError("Emitting code to load a metaspace address is not currently supported on %s", target().arch);
    }

    /**
     * Emits code for a {@link GraalHotSpotVMConfigNode}.
     *
     * @param markId id of the value to load
     * @param kind type of the value to load
     * @return value of loaded global in register
     */
    default Value emitLoadConfigValue(int markId, LIRKind kind)
    {
        throw new GraalError("Emitting code to load a config value is not currently supported on %s", target().arch);
    }

    /**
     * Emits code for a {@link ResolveConstantNode} to resolve a {@link HotSpotObjectConstant}.
     *
     * @param constant original constant
     * @param constantDescription a description of the string that need to be materialized (and
     *            interned) as java.lang.String, generated with {@link EncodedSymbolConstant}
     * @param frameState frame state for the runtime call
     * @return the address of the requested constant.
     */
    default Value emitObjectConstantRetrieval(Constant constant, Value constantDescription, LIRFrameState frameState)
    {
        throw new GraalError("Emitting code to resolve an object constant is not currently supported on %s", target().arch);
    }

    /**
     * Emits code to resolve a dynamic constant.
     *
     * @param constant original constant
     * @param frameState frame state for the runtime call
     * @return the address of the requested constant.
     */
    default Value emitResolveDynamicInvoke(Constant constant, LIRFrameState frameState)
    {
        throw new GraalError("Emitting code to resolve a dynamic constant is not currently supported on %s", target().arch);
    }

    /**
     * Emits code for a {@link ResolveConstantNode} to resolve a {@link HotSpotMetaspaceConstant}.
     *
     * @param constant original constant
     * @param constantDescription a symbolic description of the {@link HotSpotMetaspaceConstant}
     *            generated by {@link EncodedSymbolConstant}
     * @param frameState frame state for the runtime call
     * @return the address of the requested constant.
     */
    default Value emitMetaspaceConstantRetrieval(Constant constant, Value constantDescription, LIRFrameState frameState)
    {
        throw new GraalError("Emitting code to resolve a metaspace constant is not currently supported on %s", target().arch);
    }

    /**
     * Emits code for a {@link ResolveMethodAndLoadCountersNode} to resolve a
     * {@link HotSpotMetaspaceConstant} that represents a {@link ResolvedJavaMethod} and return the
     * corresponding MethodCounters object.
     *
     * @param method original constant
     * @param klassHint a klass in which the method is declared
     * @param methodDescription is symbolic description of the constant generated by
     *            {@link EncodedSymbolConstant}
     * @param frameState frame state for the runtime call
     * @return the address of the requested constant.
     */
    default Value emitResolveMethodAndLoadCounters(Constant method, Value klassHint, Value methodDescription, LIRFrameState frameState)
    {
        throw new GraalError("Emitting code to resolve a method and load counters is not currently supported on %s", target().arch);
    }

    /**
     * Emits code for a {@link ResolveConstantNode} to resolve a klass
     * {@link HotSpotMetaspaceConstant} and run static initializer.
     *
     *
     * @param constant original constant
     * @param constantDescription a symbolic description of the {@link HotSpotMetaspaceConstant}
     *            generated by {@link EncodedSymbolConstant}
     * @param frameState frame state for the runtime call
     * @return the address of the requested constant.
     */
    default Value emitKlassInitializationAndRetrieval(Constant constant, Value constantDescription, LIRFrameState frameState)
    {
        throw new GraalError("Emitting code to initialize a class is not currently supported on %s", target().arch);
    }

    /**
     * Emits code for a {@link RandomSeedNode}.
     *
     * @return value of the counter
     */
    default Value emitRandomSeed()
    {
        throw new GraalError("Emitting code to return a random seed is not currently supported on %s", target().arch);
    }

    /**
     * Gets a stack slot for a lock at a given lock nesting depth.
     */
    VirtualStackSlot getLockSlot(int lockDepth);

    @Override
    HotSpotProviders getProviders();
}
