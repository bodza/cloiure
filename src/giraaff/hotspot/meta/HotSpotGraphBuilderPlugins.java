package giraaff.hotspot.meta;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VolatileCallSite;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.zip.CRC32;

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
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.nodes.CurrentJavaThreadNode;
import giraaff.hotspot.replacements.AESCryptSubstitutions;
import giraaff.hotspot.replacements.BigIntegerSubstitutions;
import giraaff.hotspot.replacements.CRC32CSubstitutions;
import giraaff.hotspot.replacements.CRC32Substitutions;
import giraaff.hotspot.replacements.CallSiteTargetNode;
import giraaff.hotspot.replacements.CipherBlockChainingSubstitutions;
import giraaff.hotspot.replacements.ClassGetHubNode;
import giraaff.hotspot.replacements.HotSpotArraySubstitutions;
import giraaff.hotspot.replacements.HotSpotClassSubstitutions;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.IdentityHashCodeNode;
import giraaff.hotspot.replacements.ObjectCloneNode;
import giraaff.hotspot.replacements.ObjectSubstitutions;
import giraaff.hotspot.replacements.ReflectionGetCallerClassNode;
import giraaff.hotspot.replacements.ReflectionSubstitutions;
import giraaff.hotspot.replacements.SHA2Substitutions;
import giraaff.hotspot.replacements.SHA5Substitutions;
import giraaff.hotspot.replacements.ThreadSubstitutions;
import giraaff.hotspot.replacements.arraycopy.ArrayCopyNode;
import giraaff.hotspot.word.HotSpotWordTypes;
import giraaff.java.BytecodeParserOptions;
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
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
import giraaff.nodes.graphbuilderconf.InvocationPlugins.Registration;
import giraaff.nodes.java.InstanceOfDynamicNode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.StampProvider;
import giraaff.nodes.util.GraphUtil;
import giraaff.options.OptionValues;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.replacements.InlineDuringParsingPlugin;
import giraaff.replacements.MethodHandlePlugin;
import giraaff.replacements.ReplacementsImpl;
import giraaff.replacements.StandardGraphBuilderPlugins;
import giraaff.word.WordOperationPlugin;
import giraaff.word.WordTypes;

/**
 * Defines the {@link Plugins} used when running on HotSpot.
 */
public class HotSpotGraphBuilderPlugins
{
    /**
     * Creates a {@link Plugins} object that should be used when running on HotSpot.
     */
    public static Plugins create(CompilerConfiguration compilerConfiguration, GraalHotSpotVMConfig config, HotSpotWordTypes wordTypes, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, SnippetReflectionProvider snippetReflection, ForeignCallsProvider foreignCalls, LoweringProvider lowerer, StampProvider stampProvider, ReplacementsImpl replacements)
    {
        InvocationPlugins invocationPlugins = new HotSpotInvocationPlugins(config, compilerConfiguration);

        Plugins plugins = new Plugins(invocationPlugins);
        HotSpotWordOperationPlugin wordOperationPlugin = new HotSpotWordOperationPlugin(snippetReflection, wordTypes);
        HotSpotNodePlugin nodePlugin = new HotSpotNodePlugin(wordOperationPlugin);

        plugins.appendTypePlugin(nodePlugin);
        plugins.appendNodePlugin(nodePlugin);
        OptionValues options = replacements.getOptions();
        plugins.appendNodePlugin(new MethodHandlePlugin(constantReflection.getMethodHandleAccess(), true));
        plugins.appendInlineInvokePlugin(replacements);
        if (BytecodeParserOptions.InlineDuringParsing.getValue(options))
        {
            plugins.appendInlineInvokePlugin(new InlineDuringParsingPlugin());
        }

        invocationPlugins.defer(new Runnable()
        {
            @Override
            public void run()
            {
                BytecodeProvider replacementBytecodeProvider = replacements.getDefaultReplacementBytecodeProvider();
                registerObjectPlugins(invocationPlugins, options, config, replacementBytecodeProvider);
                registerClassPlugins(plugins, config, replacementBytecodeProvider);
                registerSystemPlugins(invocationPlugins, foreignCalls);
                registerThreadPlugins(invocationPlugins, metaAccess, wordTypes, config, replacementBytecodeProvider);
                registerCallSitePlugins(invocationPlugins);
                registerReflectionPlugins(invocationPlugins, replacementBytecodeProvider);
                registerConstantPoolPlugins(invocationPlugins, wordTypes, config, replacementBytecodeProvider);
                registerAESPlugins(invocationPlugins, config, replacementBytecodeProvider);
                registerCRC32Plugins(invocationPlugins, config, replacementBytecodeProvider);
                registerCRC32CPlugins(invocationPlugins, config, replacementBytecodeProvider);
                registerBigIntegerPlugins(invocationPlugins, config, replacementBytecodeProvider);
                registerSHAPlugins(invocationPlugins, config, replacementBytecodeProvider);
                registerUnsafePlugins(invocationPlugins, replacementBytecodeProvider);
                StandardGraphBuilderPlugins.registerInvocationPlugins(metaAccess, snippetReflection, invocationPlugins, replacementBytecodeProvider, true);
                registerArrayPlugins(invocationPlugins, replacementBytecodeProvider);
            }
        });
        return plugins;
    }

    private static void registerObjectPlugins(InvocationPlugins plugins, OptionValues options, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, Object.class, bytecodeProvider);
        // FIXME: clone() requires speculation and requires a fix in here (to check that b.getAssumptions() != null),
        // and in ReplacementImpl.getSubstitution() where there is an instantiation of IntrinsicGraphBuilder using
        // a constructor that sets AllowAssumptions to YES automatically. The former has to inherit the assumptions
        // settings from the root compile instead.
        r.register1("clone", Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                ValueNode object = receiver.get();
                b.addPush(JavaKind.Object, new ObjectCloneNode(b.getInvokeKind(), targetMethod, b.bci(), b.getInvokeReturnStamp(b.getAssumptions()), object));
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
        r.registerMethodSubstitution(ObjectSubstitutions.class, "hashCode", Receiver.class);
        if (config.inlineNotify())
        {
            r.registerMethodSubstitution(ObjectSubstitutions.class, "notify", Receiver.class);
        }
        if (config.inlineNotifyAll())
        {
            r.registerMethodSubstitution(ObjectSubstitutions.class, "notifyAll", Receiver.class);
        }
    }

    private static void registerClassPlugins(Plugins plugins, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins.getInvocationPlugins(), Class.class, bytecodeProvider);

        r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "getModifiers", Receiver.class);
        r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "isInterface", Receiver.class);
        r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "isArray", Receiver.class);
        r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "isPrimitive", Receiver.class);
        r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "getSuperclass", Receiver.class);

        if (config.getFieldOffset("ArrayKlass::_component_mirror", Integer.class, "oop", Integer.MAX_VALUE) != Integer.MAX_VALUE)
        {
            r.registerMethodSubstitution(HotSpotClassSubstitutions.class, "getComponentType", Receiver.class);
        }

        r.register2("cast", Receiver.class, Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode object)
            {
                ValueNode javaClass = receiver.get();
                LogicNode condition = b.append(InstanceOfDynamicNode.create(b.getAssumptions(), b.getConstantReflection(), javaClass, object, true));
                if (condition.isTautology())
                {
                    b.addPush(JavaKind.Object, object);
                }
                else
                {
                    FixedGuardNode fixedGuard = b.add(new FixedGuardNode(condition, DeoptimizationReason.ClassCastException, DeoptimizationAction.InvalidateReprofile, false));
                    b.addPush(JavaKind.Object, DynamicPiNode.create(b.getAssumptions(), b.getConstantReflection(), object, fixedGuard, javaClass));
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

    private static void registerCallSitePlugins(InvocationPlugins plugins)
    {
        InvocationPlugin plugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                ValueNode callSite = receiver.get();
                ValueNode folded = CallSiteTargetNode.tryFold(GraphUtil.originalValue(callSite), b.getMetaAccess(), b.getAssumptions());
                if (folded != null)
                {
                    b.addPush(JavaKind.Object, folded);
                }
                else
                {
                    b.addPush(JavaKind.Object, new CallSiteTargetNode(b.getInvokeKind(), targetMethod, b.bci(), b.getInvokeReturnStamp(b.getAssumptions()), callSite));
                }
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        };
        plugins.register(plugin, ConstantCallSite.class, "getTarget", Receiver.class);
        plugins.register(plugin, MutableCallSite.class, "getTarget", Receiver.class);
        plugins.register(plugin, VolatileCallSite.class, "getTarget", Receiver.class);
    }

    private static void registerReflectionPlugins(InvocationPlugins plugins, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, reflectionClass, bytecodeProvider);
        r.register0("getCallerClass", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                b.addPush(JavaKind.Object, new ReflectionGetCallerClassNode(b.getInvokeKind(), targetMethod, b.bci(), b.getInvokeReturnStamp(b.getAssumptions())));
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
        r.registerMethodSubstitution(ReflectionSubstitutions.class, "getClassAccessFlags", Class.class);
    }

    private static void registerUnsafePlugins(InvocationPlugins plugins, BytecodeProvider replacementBytecodeProvider)
    {
        Registration r = new Registration(plugins, "jdk.internal.misc.Unsafe", replacementBytecodeProvider);
        r.registerMethodSubstitution(HotSpotUnsafeSubstitutions.class, HotSpotUnsafeSubstitutions.copyMemoryName, "copyMemory", Receiver.class, Object.class, long.class, Object.class, long.class, long.class);
    }

    private static final LocationIdentity INSTANCE_KLASS_CONSTANTS = NamedLocationIdentity.immutable("InstanceKlass::_constants");
    private static final LocationIdentity CONSTANT_POOL_LENGTH = NamedLocationIdentity.immutable("ConstantPool::_length");

    /**
     * Emits a node to get the metaspace {@code ConstantPool} pointer given the value of the
     * {@code constantPoolOop} field in a ConstantPool value.
     *
     * @param constantPoolOop value of the {@code constantPoolOop} field in a ConstantPool value
     * @return a node representing the metaspace {@code ConstantPool} pointer associated with
     *         {@code constantPoolOop}
     */
    private static ValueNode getMetaspaceConstantPool(GraphBuilderContext b, ValueNode constantPoolOop, WordTypes wordTypes, GraalHotSpotVMConfig config)
    {
        // ConstantPool.constantPoolOop is in fact the holder class.
        ValueNode value = b.nullCheckedValue(constantPoolOop, DeoptimizationAction.None);
        ValueNode klass = b.add(ClassGetHubNode.create(value, b.getMetaAccess(), b.getConstantReflection(), false));

        boolean notCompressible = false;
        AddressNode constantsAddress = b.add(new OffsetAddressNode(klass, b.add(ConstantNode.forLong(config.instanceKlassConstantsOffset))));
        return WordOperationPlugin.readOp(b, wordTypes.getWordKind(), constantsAddress, INSTANCE_KLASS_CONSTANTS, BarrierType.NONE, notCompressible);
    }

    /**
     * Emits a node representing an element in a metaspace {@code ConstantPool}.
     *
     * @param constantPoolOop value of the {@code constantPoolOop} field in a ConstantPool value
     */
    private static boolean readMetaspaceConstantPoolElement(GraphBuilderContext b, ValueNode constantPoolOop, ValueNode index, JavaKind elementKind, WordTypes wordTypes, GraalHotSpotVMConfig config)
    {
        ValueNode constants = getMetaspaceConstantPool(b, constantPoolOop, wordTypes, config);
        int shift = CodeUtil.log2(wordTypes.getWordKind().getByteCount());
        ValueNode scaledIndex = b.add(new LeftShiftNode(IntegerConvertNode.convert(index, StampFactory.forKind(JavaKind.Long), NodeView.DEFAULT), b.add(ConstantNode.forInt(shift))));
        ValueNode offset = b.add(new AddNode(scaledIndex, b.add(ConstantNode.forLong(config.constantPoolSize))));
        AddressNode elementAddress = b.add(new OffsetAddressNode(constants, offset));
        boolean notCompressible = false;
        ValueNode elementValue = WordOperationPlugin.readOp(b, elementKind, elementAddress, NamedLocationIdentity.getArrayLocation(elementKind), BarrierType.NONE, notCompressible);
        b.addPush(elementKind, elementValue);
        return true;
    }

    private static void registerConstantPoolPlugins(InvocationPlugins plugins, WordTypes wordTypes, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, constantPoolClass, bytecodeProvider);

        r.register2("getSize0", Receiver.class, Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode constantPoolOop)
            {
                boolean notCompressible = false;
                ValueNode constants = getMetaspaceConstantPool(b, constantPoolOop, wordTypes, config);
                AddressNode lengthAddress = b.add(new OffsetAddressNode(constants, b.add(ConstantNode.forLong(config.constantPoolLengthOffset))));
                ValueNode length = WordOperationPlugin.readOp(b, JavaKind.Int, lengthAddress, CONSTANT_POOL_LENGTH, BarrierType.NONE, notCompressible);
                b.addPush(JavaKind.Int, length);
                return true;
            }
        });

        r.register3("getIntAt0", Receiver.class, Object.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode constantPoolOop, ValueNode index)
            {
                return readMetaspaceConstantPoolElement(b, constantPoolOop, index, JavaKind.Int, wordTypes, config);
            }
        });
        r.register3("getLongAt0", Receiver.class, Object.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode constantPoolOop, ValueNode index)
            {
                return readMetaspaceConstantPoolElement(b, constantPoolOop, index, JavaKind.Long, wordTypes, config);
            }
        });
        r.register3("getFloatAt0", Receiver.class, Object.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode constantPoolOop, ValueNode index)
            {
                return readMetaspaceConstantPoolElement(b, constantPoolOop, index, JavaKind.Float, wordTypes, config);
            }
        });
        r.register3("getDoubleAt0", Receiver.class, Object.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode constantPoolOop, ValueNode index)
            {
                return readMetaspaceConstantPoolElement(b, constantPoolOop, index, JavaKind.Double, wordTypes, config);
            }
        });
    }

    private static void registerSystemPlugins(InvocationPlugins plugins, ForeignCallsProvider foreignCalls)
    {
        Registration r = new Registration(plugins, System.class);
        r.register0("currentTimeMillis", new ForeignCallPlugin(foreignCalls, HotSpotHostForeignCallsProvider.JAVA_TIME_MILLIS));
        r.register0("nanoTime", new ForeignCallPlugin(foreignCalls, HotSpotHostForeignCallsProvider.JAVA_TIME_NANOS));
        r.register1("identityHashCode", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode object)
            {
                b.addPush(JavaKind.Int, new IdentityHashCodeNode(object));
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
        r.register5("arraycopy", Object.class, int.class, Object.class, int.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode src, ValueNode srcPos, ValueNode dst, ValueNode dstPos, ValueNode length)
            {
                b.add(new ArrayCopyNode(b.bci(), src, srcPos, dst, dstPos, length));
                return true;
            }

            @Override
            public boolean inlineOnly()
            {
                return true;
            }
        });
    }

    private static void registerArrayPlugins(InvocationPlugins plugins, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, Array.class, bytecodeProvider);
        r.setAllowOverwrite(true);
        r.registerMethodSubstitution(HotSpotArraySubstitutions.class, "newInstance", Class.class, int.class);
    }

    private static void registerThreadPlugins(InvocationPlugins plugins, MetaAccessProvider metaAccess, WordTypes wordTypes, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, Thread.class, bytecodeProvider);
        r.register0("currentThread", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                CurrentJavaThreadNode thread = b.add(new CurrentJavaThreadNode(wordTypes.getWordKind()));
                ValueNode offset = b.add(ConstantNode.forLong(config.threadObjectOffset));
                AddressNode address = b.add(new OffsetAddressNode(thread, offset));
                // JavaThread::_threadObj is never compressed
                ObjectStamp stamp = StampFactory.objectNonNull(TypeReference.create(b.getAssumptions(), metaAccess.lookupJavaType(Thread.class)));
                b.addPush(JavaKind.Object, new ReadNode(address, HotSpotReplacementsUtil.JAVA_THREAD_THREAD_OBJECT_LOCATION, stamp, BarrierType.NONE));
                return true;
            }
        });

        r.registerMethodSubstitution(ThreadSubstitutions.class, "isInterrupted", Receiver.class, boolean.class);
    }

    public static final String cbcEncryptName = "implEncrypt";
    public static final String cbcDecryptName = "implDecrypt";
    public static final String aesEncryptName = "implEncryptBlock";
    public static final String aesDecryptName = "implDecryptBlock";

    public static final String reflectionClass = "jdk.internal.reflect.Reflection";
    public static final String constantPoolClass = "jdk.internal.reflect.ConstantPool";

    private static void registerAESPlugins(InvocationPlugins plugins, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        if (config.useAESIntrinsics)
        {
            Registration r = new Registration(plugins, "com.sun.crypto.provider.CipherBlockChaining", bytecodeProvider);
            r.registerMethodSubstitution(CipherBlockChainingSubstitutions.class, cbcEncryptName, Receiver.class, byte[].class, int.class, int.class, byte[].class, int.class);
            r.registerMethodSubstitution(CipherBlockChainingSubstitutions.class, cbcDecryptName, cbcDecryptName, Receiver.class, byte[].class, int.class, int.class, byte[].class, int.class);
            r = new Registration(plugins, "com.sun.crypto.provider.AESCrypt", bytecodeProvider);
            r.registerMethodSubstitution(AESCryptSubstitutions.class, aesEncryptName, Receiver.class, byte[].class, int.class, byte[].class, int.class);
            r.registerMethodSubstitution(AESCryptSubstitutions.class, aesDecryptName, aesDecryptName, Receiver.class, byte[].class, int.class, byte[].class, int.class);
        }
    }

    private static void registerBigIntegerPlugins(InvocationPlugins plugins, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, BigInteger.class, bytecodeProvider);
        if (config.useMultiplyToLenIntrinsic())
        {
            r.registerMethodSubstitution(BigIntegerSubstitutions.class, "implMultiplyToLen", "multiplyToLenStatic", int[].class, int.class, int[].class, int.class, int[].class);
        }
        if (config.useMulAddIntrinsic())
        {
            r.registerMethodSubstitution(BigIntegerSubstitutions.class, "implMulAdd", int[].class, int[].class, int.class, int.class, int.class);
        }
        if (config.useMontgomeryMultiplyIntrinsic())
        {
            r.registerMethodSubstitution(BigIntegerSubstitutions.class, "implMontgomeryMultiply", int[].class, int[].class, int[].class, int.class, long.class, int[].class);
        }
        if (config.useMontgomerySquareIntrinsic())
        {
            r.registerMethodSubstitution(BigIntegerSubstitutions.class, "implMontgomerySquare", int[].class, int[].class, int.class, long.class, int[].class);
        }
        if (config.useSquareToLenIntrinsic())
        {
            r.registerMethodSubstitution(BigIntegerSubstitutions.class, "implSquareToLen", int[].class, int.class, int[].class, int.class);
        }
    }

    private static void registerSHAPlugins(InvocationPlugins plugins, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        if (config.useSHA256Intrinsics())
        {
            Registration r = new Registration(plugins, "sun.security.provider.SHA2", bytecodeProvider);
            r.registerMethodSubstitution(SHA2Substitutions.class, SHA2Substitutions.implCompressName, "implCompress0", Receiver.class, byte[].class, int.class);
        }
        if (config.useSHA512Intrinsics())
        {
            Registration r = new Registration(plugins, "sun.security.provider.SHA5", bytecodeProvider);
            r.registerMethodSubstitution(SHA5Substitutions.class, SHA5Substitutions.implCompressName, "implCompress0", Receiver.class, byte[].class, int.class);
        }
    }

    private static void registerCRC32Plugins(InvocationPlugins plugins, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        if (config.useCRC32Intrinsics)
        {
            Registration r = new Registration(plugins, CRC32.class, bytecodeProvider);
            r.registerMethodSubstitution(CRC32Substitutions.class, "update", int.class, int.class);
            r.registerMethodSubstitution(CRC32Substitutions.class, "updateBytes0", int.class, byte[].class, int.class, int.class);
            r.registerMethodSubstitution(CRC32Substitutions.class, "updateByteBuffer0", int.class, long.class, int.class, int.class);
        }
    }

    private static void registerCRC32CPlugins(InvocationPlugins plugins, GraalHotSpotVMConfig config, BytecodeProvider bytecodeProvider)
    {
        if (config.useCRC32CIntrinsics)
        {
            Registration r = new Registration(plugins, "java.util.zip.CRC32C", bytecodeProvider);
            r.registerMethodSubstitution(CRC32CSubstitutions.class, "updateBytes", int.class, byte[].class, int.class, int.class);
            r.registerMethodSubstitution(CRC32CSubstitutions.class, "updateDirectByteBuffer", int.class, long.class, int.class, int.class);
        }
    }
}
