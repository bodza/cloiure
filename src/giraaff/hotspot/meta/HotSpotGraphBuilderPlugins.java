package giraaff.hotspot.meta;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VolatileCallSite;
import java.lang.reflect.Array;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.word.LocationIdentity;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BytecodeProvider;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.CurrentJavaThreadNode;
import giraaff.hotspot.replacements.CallSiteTargetNode;
import giraaff.hotspot.replacements.ClassGetHubNode;
import giraaff.hotspot.replacements.HotSpotArraySubstitutions;
import giraaff.hotspot.replacements.HotSpotClassSubstitutions;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.IdentityHashCodeNode;
import giraaff.hotspot.replacements.ObjectCloneNode;
import giraaff.hotspot.replacements.ObjectSubstitutions;
import giraaff.hotspot.replacements.ReflectionGetCallerClassNode;
import giraaff.hotspot.replacements.ReflectionSubstitutions;
import giraaff.hotspot.replacements.arraycopy.ArrayCopyNode;
import giraaff.hotspot.word.HotSpotWordTypes;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DynamicPiNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.IntegerConvertNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.graphbuilderconf.ForeignCallPlugin;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
import giraaff.nodes.java.InstanceOfDynamicNode;
import giraaff.nodes.memory.HeapAccess;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.StampProvider;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.replacements.InlineDuringParsingPlugin;
import giraaff.replacements.MethodHandlePlugin;
import giraaff.replacements.ReplacementsImpl;
import giraaff.replacements.StandardGraphBuilderPlugins;
import giraaff.word.WordOperationPlugin;
import giraaff.word.WordTypes;

///
// Defines the {@link GraphBuilderConfiguration.Plugins} used when running on HotSpot.
///
// @class HotSpotGraphBuilderPlugins
public final class HotSpotGraphBuilderPlugins
{
    // @cons HotSpotGraphBuilderPlugins
    private HotSpotGraphBuilderPlugins()
    {
        super();
    }

    ///
    // Creates a {@link GraphBuilderConfiguration.Plugins} object that should be used when running on HotSpot.
    ///
    public static GraphBuilderConfiguration.Plugins create(CompilerConfiguration __compilerConfiguration, HotSpotWordTypes __wordTypes, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, SnippetReflectionProvider __snippetReflection, ForeignCallsProvider __foreignCalls, LoweringProvider __lowerer, StampProvider __stampProvider, ReplacementsImpl __replacements)
    {
        InvocationPlugins __invocationPlugins = new HotSpotInvocationPlugins();

        GraphBuilderConfiguration.Plugins __plugins = new GraphBuilderConfiguration.Plugins(__invocationPlugins);
        HotSpotWordOperationPlugin __wordOperationPlugin = new HotSpotWordOperationPlugin(__snippetReflection, __wordTypes);
        HotSpotNodePlugin __nodePlugin = new HotSpotNodePlugin(__wordOperationPlugin);

        __plugins.appendTypePlugin(__nodePlugin);
        __plugins.appendNodePlugin(__nodePlugin);
        __plugins.appendNodePlugin(new MethodHandlePlugin(__constantReflection.getMethodHandleAccess(), true));
        __plugins.appendInlineInvokePlugin(__replacements);
        if (GraalOptions.inlineDuringParsing)
        {
            __plugins.appendInlineInvokePlugin(new InlineDuringParsingPlugin());
        }

        __invocationPlugins.defer(new Runnable()
        {
            @Override
            public void run()
            {
                BytecodeProvider __replacementBytecodeProvider = __replacements.getDefaultReplacementBytecodeProvider();
                registerObjectPlugins(__invocationPlugins, __replacementBytecodeProvider);
                registerClassPlugins(__plugins, __replacementBytecodeProvider);
                registerSystemPlugins(__invocationPlugins, __foreignCalls);
                registerThreadPlugins(__invocationPlugins, __metaAccess, __wordTypes, __replacementBytecodeProvider);
                registerCallSitePlugins(__invocationPlugins);
                registerReflectionPlugins(__invocationPlugins, __replacementBytecodeProvider);
                registerConstantPoolPlugins(__invocationPlugins, __wordTypes, __replacementBytecodeProvider);
                registerUnsafePlugins(__invocationPlugins, __replacementBytecodeProvider);
                StandardGraphBuilderPlugins.registerInvocationPlugins(__metaAccess, __snippetReflection, __invocationPlugins, __replacementBytecodeProvider, true);
                registerArrayPlugins(__invocationPlugins, __replacementBytecodeProvider);
            }
        });
        return __plugins;
    }

    private static void registerObjectPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Object.class, __bytecodeProvider);
        // FIXME: clone() requires speculation and requires a fix in here (to check that b.getAssumptions() != null),
        // and in ReplacementImpl.getSubstitution() where there is an instantiation of IntrinsicGraphBuilder using
        // a constructor that sets StructuredGraph.AllowAssumptions to YES automatically. The former has to inherit
        // the assumptions settings from the root compile instead.
        // @closure
        __r.register1("clone", InvocationPlugin.Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                ValueNode __object = __receiver.get();
                __b.addPush(JavaKind.Object, new ObjectCloneNode(__b.getInvokeKind(), __targetMethod, __b.bci(), __b.getInvokeReturnStamp(__b.getAssumptions()), __object));
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
        __r.registerMethodSubstitution(ObjectSubstitutions.class, "hashCode", InvocationPlugin.Receiver.class);
    }

    private static void registerClassPlugins(GraphBuilderConfiguration.Plugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins.getInvocationPlugins(), Class.class, __bytecodeProvider);

        __r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "getModifiers", InvocationPlugin.Receiver.class);
        __r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "isInterface", InvocationPlugin.Receiver.class);
        __r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "isArray", InvocationPlugin.Receiver.class);
        __r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "isPrimitive", InvocationPlugin.Receiver.class);
        __r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "getSuperclass", InvocationPlugin.Receiver.class);
        if (HotSpotRuntime.arrayKlassComponentMirrorOffset != Integer.MAX_VALUE)
        {
            __r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "getComponentType", InvocationPlugin.Receiver.class);
        }

        // @closure
        __r.register2("cast", InvocationPlugin.Receiver.class, Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __object)
            {
                ValueNode __javaClass = __receiver.get();
                LogicNode __condition = __b.append(InstanceOfDynamicNode.create(__b.getAssumptions(), __b.getConstantReflection(), __javaClass, __object, true));
                if (__condition.isTautology())
                {
                    __b.addPush(JavaKind.Object, __object);
                }
                else
                {
                    FixedGuardNode __fixedGuard = __b.add(new FixedGuardNode(__condition, DeoptimizationReason.ClassCastException, DeoptimizationAction.InvalidateReprofile, false));
                    __b.addPush(JavaKind.Object, DynamicPiNode.create(__b.getAssumptions(), __b.getConstantReflection(), __object, __fixedGuard, __javaClass));
                }
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
    }

    private static void registerCallSitePlugins(InvocationPlugins __plugins)
    {
        // @closure
        InvocationPlugin __plugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                ValueNode __callSite = __receiver.get();
                ValueNode __folded = CallSiteTargetNode.tryFold(GraphUtil.originalValue(__callSite), __b.getMetaAccess(), __b.getAssumptions());
                if (__folded != null)
                {
                    __b.addPush(JavaKind.Object, __folded);
                }
                else
                {
                    __b.addPush(JavaKind.Object, new CallSiteTargetNode(__b.getInvokeKind(), __targetMethod, __b.bci(), __b.getInvokeReturnStamp(__b.getAssumptions()), __callSite));
                }
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        };
        __plugins.register(__plugin, ConstantCallSite.class, "getTarget", InvocationPlugin.Receiver.class);
        __plugins.register(__plugin, MutableCallSite.class, "getTarget", InvocationPlugin.Receiver.class);
        __plugins.register(__plugin, VolatileCallSite.class, "getTarget", InvocationPlugin.Receiver.class);
    }

    private static void registerReflectionPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, reflectionClass, __bytecodeProvider);
        // @closure
        __r.register0("getCallerClass", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                __b.addPush(JavaKind.Object, new ReflectionGetCallerClassNode(__b.getInvokeKind(), __targetMethod, __b.bci(), __b.getInvokeReturnStamp(__b.getAssumptions())));
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
        __r.registerMethodSubstitution(ReflectionSubstitutions.class, "getClassAccessFlags", Class.class);
    }

    private static void registerUnsafePlugins(InvocationPlugins __plugins, BytecodeProvider __replacementBytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, "jdk.internal.misc.Unsafe", __replacementBytecodeProvider);
        __r.registerMethodSubstitution(HotSpotUnsafeSubstitutions.class, HotSpotUnsafeSubstitutions.copyMemoryName, "copyMemory", InvocationPlugin.Receiver.class, Object.class, long.class, Object.class, long.class, long.class);
    }

    // @def
    private static final LocationIdentity INSTANCE_KLASS_CONSTANTS = NamedLocationIdentity.immutable("InstanceKlass::_constants");
    // @def
    private static final LocationIdentity CONSTANT_POOL_LENGTH = NamedLocationIdentity.immutable("ConstantPool::_length");

    ///
    // Emits a node to get the metaspace {@code ConstantPool} pointer given the value of the
    // {@code constantPoolOop} field in a ConstantPool value.
    //
    // @param constantPoolOop value of the {@code constantPoolOop} field in a ConstantPool value
    // @return a node representing the metaspace {@code ConstantPool} pointer associated with
    //         {@code constantPoolOop}
    ///
    private static ValueNode getMetaspaceConstantPool(GraphBuilderContext __b, ValueNode __constantPoolOop, WordTypes __wordTypes)
    {
        // ConstantPool.constantPoolOop is in fact the holder class.
        ValueNode __value = __b.nullCheckedValue(__constantPoolOop, DeoptimizationAction.None);
        ValueNode __klass = __b.add(ClassGetHubNode.create(__value, __b.getMetaAccess(), __b.getConstantReflection(), false));

        boolean __notCompressible = false;
        AddressNode __constantsAddress = __b.add(new OffsetAddressNode(__klass, __b.add(ConstantNode.forLong(HotSpotRuntime.instanceKlassConstantsOffset))));
        return WordOperationPlugin.readOp(__b, __wordTypes.getWordKind(), __constantsAddress, INSTANCE_KLASS_CONSTANTS, HeapAccess.BarrierType.NONE, __notCompressible);
    }

    ///
    // Emits a node representing an element in a metaspace {@code ConstantPool}.
    //
    // @param constantPoolOop value of the {@code constantPoolOop} field in a ConstantPool value
    ///
    private static boolean readMetaspaceConstantPoolElement(GraphBuilderContext __b, ValueNode __constantPoolOop, ValueNode __index, JavaKind __elementKind, WordTypes __wordTypes)
    {
        ValueNode __constants = getMetaspaceConstantPool(__b, __constantPoolOop, __wordTypes);
        int __shift = CodeUtil.log2(__wordTypes.getWordKind().getByteCount());
        ValueNode __scaledIndex = __b.add(new LeftShiftNode(IntegerConvertNode.convert(__index, StampFactory.forKind(JavaKind.Long), NodeView.DEFAULT), __b.add(ConstantNode.forInt(__shift))));
        ValueNode __offset = __b.add(new AddNode(__scaledIndex, __b.add(ConstantNode.forLong(HotSpotRuntime.constantPoolSize))));
        AddressNode __elementAddress = __b.add(new OffsetAddressNode(__constants, __offset));
        boolean __notCompressible = false;
        ValueNode __elementValue = WordOperationPlugin.readOp(__b, __elementKind, __elementAddress, NamedLocationIdentity.getArrayLocation(__elementKind), HeapAccess.BarrierType.NONE, __notCompressible);
        __b.addPush(__elementKind, __elementValue);
        return true;
    }

    private static void registerConstantPoolPlugins(InvocationPlugins __plugins, WordTypes __wordTypes, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, constantPoolClass, __bytecodeProvider);

        // @closure
        __r.register2("getSize0", InvocationPlugin.Receiver.class, Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __constantPoolOop)
            {
                boolean __notCompressible = false;
                ValueNode __constants = getMetaspaceConstantPool(__b, __constantPoolOop, __wordTypes);
                AddressNode __lengthAddress = __b.add(new OffsetAddressNode(__constants, __b.add(ConstantNode.forLong(HotSpotRuntime.constantPoolLengthOffset))));
                ValueNode __length = WordOperationPlugin.readOp(__b, JavaKind.Int, __lengthAddress, CONSTANT_POOL_LENGTH, HeapAccess.BarrierType.NONE, __notCompressible);
                __b.addPush(JavaKind.Int, __length);
                return true;
            }
        });

        // @closure
        __r.register3("getIntAt0", InvocationPlugin.Receiver.class, Object.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __constantPoolOop, ValueNode __index)
            {
                return readMetaspaceConstantPoolElement(__b, __constantPoolOop, __index, JavaKind.Int, __wordTypes);
            }
        });
        // @closure
        __r.register3("getLongAt0", InvocationPlugin.Receiver.class, Object.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __constantPoolOop, ValueNode __index)
            {
                return readMetaspaceConstantPoolElement(__b, __constantPoolOop, __index, JavaKind.Long, __wordTypes);
            }
        });
    }

    private static void registerSystemPlugins(InvocationPlugins __plugins, ForeignCallsProvider __foreignCalls)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, System.class);
        __r.register0("currentTimeMillis", new ForeignCallPlugin(__foreignCalls, HotSpotHostForeignCallsProvider.JAVA_TIME_MILLIS));
        __r.register0("nanoTime", new ForeignCallPlugin(__foreignCalls, HotSpotHostForeignCallsProvider.JAVA_TIME_NANOS));
        // @closure
        __r.register1("identityHashCode", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __object)
            {
                __b.addPush(JavaKind.Int, new IdentityHashCodeNode(__object));
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
        // @closure
        __r.register5("arraycopy", Object.class, int.class, Object.class, int.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __src, ValueNode __srcPos, ValueNode __dst, ValueNode __dstPos, ValueNode __length)
            {
                __b.add(new ArrayCopyNode(__b.bci(), __src, __srcPos, __dst, __dstPos, __length));
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
    }

    private static void registerArrayPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Array.class, __bytecodeProvider);
        __r.setAllowOverwrite(true);
        __r.registerMethodSubstitution(HotSpotArraySubstitutions.class, "newInstance", Class.class, int.class);
    }

    private static void registerThreadPlugins(InvocationPlugins __plugins, MetaAccessProvider __metaAccess, WordTypes __wordTypes, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Thread.class, __bytecodeProvider);
        // @closure
        __r.register0("currentThread", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                CurrentJavaThreadNode __thread = __b.add(new CurrentJavaThreadNode(__wordTypes.getWordKind()));
                ValueNode __offset = __b.add(ConstantNode.forLong(HotSpotRuntime.threadObjectOffset));
                AddressNode __address = __b.add(new OffsetAddressNode(__thread, __offset));
                // JavaThread::_threadObj is never compressed
                ObjectStamp __stamp = StampFactory.objectNonNull(TypeReference.create(__b.getAssumptions(), __metaAccess.lookupJavaType(Thread.class)));
                __b.addPush(JavaKind.Object, new ReadNode(__address, HotSpotReplacementsUtil.JAVA_THREAD_THREAD_OBJECT_LOCATION, __stamp, HeapAccess.BarrierType.NONE));
                return true;
            }
        });
    }

    // @def
    public static final String reflectionClass = "jdk.internal.reflect.Reflection";
    // @def
    public static final String constantPoolClass = "jdk.internal.reflect.ConstantPool";
}
