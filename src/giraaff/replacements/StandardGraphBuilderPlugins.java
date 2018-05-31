package giraaff.replacements;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;

import jdk.vm.ci.code.BytecodePosition;
import jdk.vm.ci.code.MemoryBarriers;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.SpeculationLog;

import org.graalvm.word.LocationIdentity;

import giraaff.api.directives.GraalDirectives;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BytecodeProvider;
import giraaff.core.common.calc.Condition;
import giraaff.core.common.calc.Condition.CanonicalizedCondition;
import giraaff.core.common.calc.UnsignedMath;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Edges;
import giraaff.graph.Node;
import giraaff.graph.NodeList;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AbsNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.FloatEqualsNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.NarrowNode;
import giraaff.nodes.calc.ReinterpretNode;
import giraaff.nodes.calc.RightShiftNode;
import giraaff.nodes.calc.SignExtendNode;
import giraaff.nodes.calc.SqrtNode;
import giraaff.nodes.calc.UnsignedDivNode;
import giraaff.nodes.calc.UnsignedRemNode;
import giraaff.nodes.calc.ZeroExtendNode;
import giraaff.nodes.debug.BindToRegisterNode;
import giraaff.nodes.debug.BlackholeNode;
import giraaff.nodes.debug.ControlFlowAnchorNode;
import giraaff.nodes.debug.OpaqueNode;
import giraaff.nodes.debug.SpillRegistersNode;
import giraaff.nodes.extended.BoxNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.GetClassNode;
import giraaff.nodes.extended.MembarNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.extended.RawStoreNode;
import giraaff.nodes.extended.UnboxNode;
import giraaff.nodes.extended.UnsafeMemoryLoadNode;
import giraaff.nodes.extended.UnsafeMemoryStoreNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
import giraaff.nodes.graphbuilderconf.InvocationPlugins.Registration;
import giraaff.nodes.java.ClassIsAssignableFromNode;
import giraaff.nodes.java.DynamicNewArrayNode;
import giraaff.nodes.java.DynamicNewInstanceNode;
import giraaff.nodes.java.InstanceOfDynamicNode;
import giraaff.nodes.java.RegisterFinalizerNode;
import giraaff.nodes.java.UnsafeCompareAndSwapNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.EnsureVirtualizedNode;
import giraaff.replacements.nodes.ReverseBytesNode;
import giraaff.replacements.nodes.VirtualizableInvokeMacroNode;
import giraaff.replacements.nodes.arithmetic.IntegerAddExactNode;
import giraaff.replacements.nodes.arithmetic.IntegerMulExactNode;
import giraaff.replacements.nodes.arithmetic.IntegerSubExactNode;
import giraaff.util.GraalError;

/**
 * Provides non-runtime specific {@link InvocationPlugin}s.
 */
// @class StandardGraphBuilderPlugins
public final class StandardGraphBuilderPlugins
{
    // @cons
    private StandardGraphBuilderPlugins()
    {
        super();
    }

    public static void registerInvocationPlugins(MetaAccessProvider metaAccess, SnippetReflectionProvider snippetReflection, InvocationPlugins plugins, BytecodeProvider bytecodeProvider, boolean allowDeoptimization)
    {
        registerObjectPlugins(plugins);
        registerClassPlugins(plugins);
        registerMathPlugins(plugins, allowDeoptimization);
        registerUnsignedMathPlugins(plugins);
        registerStringPlugins(plugins, bytecodeProvider, snippetReflection);
        registerCharacterPlugins(plugins);
        registerShortPlugins(plugins);
        registerIntegerLongPlugins(plugins, JavaKind.Int);
        registerIntegerLongPlugins(plugins, JavaKind.Long);
        registerFloatPlugins(plugins);
        registerDoublePlugins(plugins);
        registerArraysPlugins(plugins, bytecodeProvider);
        registerArrayPlugins(plugins, bytecodeProvider);
        registerUnsafePlugins(plugins, bytecodeProvider);
        registerEdgesPlugins(metaAccess, plugins);
        registerGraalDirectivesPlugins(plugins);
        registerBoxingPlugins(plugins);
    }

    private static void registerStringPlugins(InvocationPlugins plugins, BytecodeProvider bytecodeProvider, SnippetReflectionProvider snippetReflection)
    {
        final Registration r = new Registration(plugins, String.class, bytecodeProvider);
        r.register1("hashCode", Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                if (receiver.isConstant())
                {
                    String s = snippetReflection.asObject(String.class, (JavaConstant) receiver.get().asConstant());
                    b.addPush(JavaKind.Int, b.add(ConstantNode.forInt(s.hashCode())));
                    return true;
                }
                return false;
            }
        });
    }

    private static void registerArraysPlugins(InvocationPlugins plugins, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, Arrays.class, bytecodeProvider);
        r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", boolean[].class, boolean[].class);
        r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", byte[].class, byte[].class);
        r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", short[].class, short[].class);
        r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", char[].class, char[].class);
        r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", int[].class, int[].class);
        r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", long[].class, long[].class);
    }

    private static void registerArrayPlugins(InvocationPlugins plugins, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, Array.class, bytecodeProvider);
        r.register2("newInstance", Class.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unused, ValueNode componentType, ValueNode length)
            {
                b.addPush(JavaKind.Object, new DynamicNewArrayNode(componentType, length, true));
                return true;
            }
        });
        r.registerMethodSubstitution(ArraySubstitutions.class, "getLength", Object.class);
    }

    private static void registerUnsafePlugins(InvocationPlugins plugins, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, "jdk.internal.misc.Unsafe", bytecodeProvider);

        for (JavaKind kind : JavaKind.values())
        {
            if ((kind.isPrimitive() && kind != JavaKind.Void) || kind == JavaKind.Object)
            {
                Class<?> javaClass = kind == JavaKind.Object ? Object.class : kind.toJavaClass();
                String kindName = kind.name();
                String getName = "get" + kindName;
                String putName = "put" + kindName;
                // object-based accesses
                r.register3(getName, Receiver.class, Object.class, long.class, new UnsafeGetPlugin(kind, false));
                r.register4(putName, Receiver.class, Object.class, long.class, javaClass, new UnsafePutPlugin(kind, false));
                // volatile object-based accesses
                r.register3(getName + "Volatile", Receiver.class, Object.class, long.class, new UnsafeGetPlugin(kind, true));
                r.register4(putName + "Volatile", Receiver.class, Object.class, long.class, javaClass, new UnsafePutPlugin(kind, true));
                // ordered object-based accesses
                r.register4("put" + kindName + "Release", Receiver.class, Object.class, long.class, javaClass, UnsafePutPlugin.putOrdered(kind));
                if (kind != JavaKind.Boolean && kind != JavaKind.Object)
                {
                    // raw accesses to memory addresses
                    r.register2(getName, Receiver.class, long.class, new UnsafeGetPlugin(kind, false));
                    r.register3(putName, Receiver.class, long.class, kind.toJavaClass(), new UnsafePutPlugin(kind, false));
                }
            }
        }

        // Accesses to native memory addresses.
        r.register2("getAddress", Receiver.class, long.class, new UnsafeGetPlugin(JavaKind.Long, false));
        r.register3("putAddress", Receiver.class, long.class, long.class, new UnsafePutPlugin(JavaKind.Long, false));

        for (JavaKind kind : new JavaKind[] { JavaKind.Int, JavaKind.Long, JavaKind.Object })
        {
            Class<?> javaClass = kind == JavaKind.Object ? Object.class : kind.toJavaClass();
            r.register5("compareAndSet" + kind.name(), Receiver.class, Object.class, long.class, javaClass, javaClass, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe, ValueNode object, ValueNode offset, ValueNode expected, ValueNode x)
                {
                    // emits a null-check for the otherwise unused receiver
                    unsafe.get();
                    b.addPush(JavaKind.Int, new UnsafeCompareAndSwapNode(object, offset, expected, x, kind, LocationIdentity.any()));
                    b.getGraph().markUnsafeAccess();
                    return true;
                }
            });
        }

        r.register2("allocateInstance", Receiver.class, Class.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe, ValueNode clazz)
            {
                // emits a null-check for the otherwise unused receiver
                unsafe.get();
                b.addPush(JavaKind.Object, new DynamicNewInstanceNode(b.nullCheckedValue(clazz, DeoptimizationAction.None), true));
                return true;
            }
        });

        r.register1("loadFence", Receiver.class, new UnsafeFencePlugin(MemoryBarriers.LOAD_LOAD | MemoryBarriers.LOAD_STORE));
        r.register1("storeFence", Receiver.class, new UnsafeFencePlugin(MemoryBarriers.STORE_STORE | MemoryBarriers.LOAD_STORE));
        r.register1("fullFence", Receiver.class, new UnsafeFencePlugin(MemoryBarriers.LOAD_LOAD | MemoryBarriers.STORE_STORE | MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_LOAD));
    }

    private static void registerIntegerLongPlugins(InvocationPlugins plugins, JavaKind kind)
    {
        Class<?> declaringClass = kind.toBoxedJavaClass();
        Class<?> type = kind.toJavaClass();
        Registration r = new Registration(plugins, declaringClass);
        r.register1("reverseBytes", type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.push(kind, b.append(new ReverseBytesNode(value).canonical(null)));
                return true;
            }
        });
        r.register2("divideUnsigned", type, type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode dividend, ValueNode divisor)
            {
                b.push(kind, b.append(UnsignedDivNode.create(dividend, divisor, NodeView.DEFAULT)));
                return true;
            }
        });
        r.register2("remainderUnsigned", type, type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode dividend, ValueNode divisor)
            {
                b.push(kind, b.append(UnsignedRemNode.create(dividend, divisor, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    private static void registerCharacterPlugins(InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, Character.class);
        r.register1("reverseBytes", char.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                // return (char) (Integer.reverse(i) >> 16);
                ReverseBytesNode reverse = b.add(new ReverseBytesNode(value));
                RightShiftNode rightShift = b.add(new RightShiftNode(reverse, b.add(ConstantNode.forInt(16))));
                ZeroExtendNode charCast = b.add(new ZeroExtendNode(b.add(new NarrowNode(rightShift, 16)), 32));
                b.push(JavaKind.Char, b.append(charCast.canonical(null)));
                return true;
            }
        });
    }

    private static void registerShortPlugins(InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, Short.class);
        r.register1("reverseBytes", short.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                // return (short) (Integer.reverse(i) >> 16);
                ReverseBytesNode reverse = b.add(new ReverseBytesNode(value));
                RightShiftNode rightShift = b.add(new RightShiftNode(reverse, b.add(ConstantNode.forInt(16))));
                SignExtendNode charCast = b.add(new SignExtendNode(b.add(new NarrowNode(rightShift, 16)), 32));
                b.push(JavaKind.Short, b.append(charCast.canonical(null)));
                return true;
            }
        });
    }

    private static void registerFloatPlugins(InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, Float.class);
        r.register1("floatToRawIntBits", float.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.push(JavaKind.Int, b.append(ReinterpretNode.create(JavaKind.Int, value, NodeView.DEFAULT)));
                return true;
            }
        });
        r.register1("floatToIntBits", float.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                LogicNode notNan = b.append(FloatEqualsNode.create(value, value, NodeView.DEFAULT));
                ValueNode raw = b.append(ReinterpretNode.create(JavaKind.Int, value, NodeView.DEFAULT));
                ValueNode result = b.append(ConditionalNode.create(notNan, raw, ConstantNode.forInt(0x7fc00000), NodeView.DEFAULT));
                b.push(JavaKind.Int, result);
                return true;
            }
        });
        r.register1("intBitsToFloat", int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.push(JavaKind.Float, b.append(ReinterpretNode.create(JavaKind.Float, value, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    private static void registerDoublePlugins(InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, Double.class);
        r.register1("doubleToRawLongBits", double.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.push(JavaKind.Long, b.append(ReinterpretNode.create(JavaKind.Long, value, NodeView.DEFAULT)));
                return true;
            }
        });
        r.register1("doubleToLongBits", double.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                LogicNode notNan = b.append(FloatEqualsNode.create(value, value, NodeView.DEFAULT));
                ValueNode raw = b.append(ReinterpretNode.create(JavaKind.Long, value, NodeView.DEFAULT));
                ValueNode result = b.append(ConditionalNode.create(notNan, raw, ConstantNode.forLong(0x7ff8000000000000L), NodeView.DEFAULT));
                b.push(JavaKind.Long, result);
                return true;
            }
        });
        r.register1("longBitsToDouble", long.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.push(JavaKind.Double, b.append(ReinterpretNode.create(JavaKind.Double, value, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    private static void registerMathPlugins(InvocationPlugins plugins, boolean allowDeoptimization)
    {
        Registration r = new Registration(plugins, Math.class);
        if (allowDeoptimization)
        {
            for (JavaKind kind : new JavaKind[] { JavaKind.Int, JavaKind.Long })
            {
                Class<?> type = kind.toJavaClass();

                r.register1("decrementExact", type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode x)
                    {
                        b.addPush(kind, new IntegerSubExactNode(x, ConstantNode.forIntegerKind(kind, 1)));
                        return true;
                    }
                });

                r.register1("incrementExact", type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode x)
                    {
                        b.addPush(kind, new IntegerAddExactNode(x, ConstantNode.forIntegerKind(kind, 1)));
                        return true;
                    }
                });

                r.register2("addExact", type, type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode x, ValueNode y)
                    {
                        b.addPush(kind, new IntegerAddExactNode(x, y));
                        return true;
                    }
                });

                r.register2("subtractExact", type, type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode x, ValueNode y)
                    {
                        b.addPush(kind, new IntegerSubExactNode(x, y));
                        return true;
                    }
                });

                r.register2("multiplyExact", type, type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode x, ValueNode y)
                    {
                        b.addPush(kind, new IntegerMulExactNode(x, y));
                        return true;
                    }
                });
            }
        }
        r.register1("abs", Float.TYPE, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.push(JavaKind.Float, b.append(new AbsNode(value).canonical(null)));
                return true;
            }
        });
        r.register1("abs", Double.TYPE, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.push(JavaKind.Double, b.append(new AbsNode(value).canonical(null)));
                return true;
            }
        });
        r.register1("sqrt", Double.TYPE, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.push(JavaKind.Double, b.append(SqrtNode.create(value, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    // @class StandardGraphBuilderPlugins.UnsignedMathPlugin
    public static final class UnsignedMathPlugin implements InvocationPlugin
    {
        private final Condition condition;

        // @cons
        public UnsignedMathPlugin(Condition condition)
        {
            super();
            this.condition = condition;
        }

        @Override
        public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode x, ValueNode y)
        {
            CanonicalizedCondition canonical = condition.canonicalize();
            StructuredGraph graph = b.getGraph();

            ValueNode lhs = canonical.mustMirror() ? y : x;
            ValueNode rhs = canonical.mustMirror() ? x : y;

            ValueNode trueValue = ConstantNode.forBoolean(!canonical.mustNegate(), graph);
            ValueNode falseValue = ConstantNode.forBoolean(canonical.mustNegate(), graph);

            LogicNode compare = CompareNode.createCompareNode(graph, b.getConstantReflection(), b.getMetaAccess(), null, canonical.getCanonicalCondition(), lhs, rhs, NodeView.DEFAULT);
            b.addPush(JavaKind.Boolean, new ConditionalNode(compare, trueValue, falseValue));
            return true;
        }
    }

    private static void registerUnsignedMathPlugins(InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, UnsignedMath.class);
        r.register2("aboveThan", int.class, int.class, new UnsignedMathPlugin(Condition.AT));
        r.register2("aboveThan", long.class, long.class, new UnsignedMathPlugin(Condition.AT));
        r.register2("belowThan", int.class, int.class, new UnsignedMathPlugin(Condition.BT));
        r.register2("belowThan", long.class, long.class, new UnsignedMathPlugin(Condition.BT));
        r.register2("aboveOrEqual", int.class, int.class, new UnsignedMathPlugin(Condition.AE));
        r.register2("aboveOrEqual", long.class, long.class, new UnsignedMathPlugin(Condition.AE));
        r.register2("belowOrEqual", int.class, int.class, new UnsignedMathPlugin(Condition.BE));
        r.register2("belowOrEqual", long.class, long.class, new UnsignedMathPlugin(Condition.BE));
    }

    protected static void registerBoxingPlugins(InvocationPlugins plugins)
    {
        for (JavaKind kind : JavaKind.values())
        {
            if (kind.isPrimitive() && kind != JavaKind.Void)
            {
                new BoxPlugin(kind).register(plugins);
                new UnboxPlugin(kind).register(plugins);
            }
        }
    }

    private static void registerObjectPlugins(InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, Object.class);
        r.register1("<init>", Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                /*
                 * Object.<init> is a common instrumentation point so only perform this rewrite if
                 * the current definition is the normal empty method with a single return bytecode.
                 * The finalizer registration will instead be performed by the BytecodeParser.
                 */
                if (targetMethod.getCodeSize() == 1)
                {
                    ValueNode object = receiver.get();
                    if (RegisterFinalizerNode.mayHaveFinalizer(object, b.getAssumptions()))
                    {
                        b.add(new RegisterFinalizerNode(object));
                    }
                    return true;
                }
                return false;
            }
        });
        r.register1("getClass", Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                ValueNode object = receiver.get();
                ValueNode folded = GetClassNode.tryFold(b.getMetaAccess(), b.getConstantReflection(), NodeView.DEFAULT, GraphUtil.originalValue(object));
                if (folded != null)
                {
                    b.addPush(JavaKind.Object, folded);
                }
                else
                {
                    Stamp stamp = StampFactory.objectNonNull(TypeReference.createTrusted(b.getAssumptions(), b.getMetaAccess().lookupJavaType(Class.class)));
                    b.addPush(JavaKind.Object, new GetClassNode(stamp, object));
                }
                return true;
            }
        });
    }

    private static void registerClassPlugins(InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, Class.class);
        r.register2("isInstance", Receiver.class, Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver type, ValueNode object)
            {
                LogicNode condition = b.append(InstanceOfDynamicNode.create(b.getAssumptions(), b.getConstantReflection(), type.get(), object, false));
                b.push(JavaKind.Boolean, b.append(new ConditionalNode(condition).canonical(null)));
                return true;
            }
        });
        r.register2("isAssignableFrom", Receiver.class, Class.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver type, ValueNode otherType)
            {
                ClassIsAssignableFromNode condition = b.append(new ClassIsAssignableFromNode(type.get(), otherType));
                b.push(JavaKind.Boolean, b.append(new ConditionalNode(condition).canonical(null)));
                return true;
            }
        });
    }

    /**
     * Substitutions for improving the performance of some critical methods in {@link Edges}. These
     * substitutions improve the performance by forcing the relevant methods to be inlined
     * (intrinsification being a special form of inlining) and removing a checked cast.
     */
    private static void registerEdgesPlugins(MetaAccessProvider metaAccess, InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, Edges.class);
        for (Class<?> c : new Class<?>[] { Node.class, NodeList.class })
        {
            r.register2("get" + c.getSimpleName() + "Unsafe", Node.class, long.class, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode node, ValueNode offset)
                {
                    ObjectStamp stamp = StampFactory.object(TypeReference.createTrusted(b.getAssumptions(), metaAccess.lookupJavaType(c)));
                    RawLoadNode value = b.add(new RawLoadNode(stamp, node, offset, LocationIdentity.any(), JavaKind.Object));
                    b.addPush(JavaKind.Object, value);
                    return true;
                }
            });
            r.register3("put" + c.getSimpleName() + "Unsafe", Node.class, long.class, c, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode node, ValueNode offset, ValueNode value)
                {
                    b.add(new RawStoreNode(node, offset, value, JavaKind.Object, LocationIdentity.any()));
                    return true;
                }
            });
        }
    }

    // @class StandardGraphBuilderPlugins.BoxPlugin
    public static final class BoxPlugin implements InvocationPlugin
    {
        private final JavaKind kind;

        // @cons
        BoxPlugin(JavaKind kind)
        {
            super();
            this.kind = kind;
        }

        @Override
        public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
        {
            if (b.parsingIntrinsic())
            {
                ResolvedJavaMethod rootMethod = b.getGraph().method();
                if (b.getMetaAccess().lookupJavaType(BoxingSnippets.class).isAssignableFrom(rootMethod.getDeclaringClass()))
                {
                    // disable invocation plugins for boxing snippets, so that the original JDK methods are inlined
                    return false;
                }
            }
            ResolvedJavaType resultType = b.getMetaAccess().lookupJavaType(kind.toBoxedJavaClass());
            b.addPush(JavaKind.Object, new BoxNode(value, resultType, kind));
            return true;
        }

        void register(InvocationPlugins plugins)
        {
            plugins.register(this, kind.toBoxedJavaClass(), "valueOf", kind.toJavaClass());
        }
    }

    // @class StandardGraphBuilderPlugins.UnboxPlugin
    public static final class UnboxPlugin implements InvocationPlugin
    {
        private final JavaKind kind;

        // @cons
        UnboxPlugin(JavaKind kind)
        {
            super();
            this.kind = kind;
        }

        @Override
        public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
        {
            if (b.parsingIntrinsic())
            {
                ResolvedJavaMethod rootMethod = b.getGraph().method();
                if (b.getMetaAccess().lookupJavaType(BoxingSnippets.class).isAssignableFrom(rootMethod.getDeclaringClass()))
                {
                    // disable invocation plugins for unboxing snippets, so that the original JDK methods are inlined
                    return false;
                }
            }
            ValueNode valueNode = UnboxNode.create(b.getMetaAccess(), b.getConstantReflection(), receiver.get(), kind);
            b.addPush(kind, valueNode);
            return true;
        }

        void register(InvocationPlugins plugins)
        {
            String name = kind.toJavaClass().getSimpleName() + "Value";
            plugins.register(this, kind.toBoxedJavaClass(), name, Receiver.class);
        }
    }

    // @class StandardGraphBuilderPlugins.UnsafeGetPlugin
    public static final class UnsafeGetPlugin implements InvocationPlugin
    {
        private final JavaKind returnKind;
        private final boolean isVolatile;

        // @cons
        public UnsafeGetPlugin(JavaKind returnKind, boolean isVolatile)
        {
            super();
            this.returnKind = returnKind;
            this.isVolatile = isVolatile;
        }

        @Override
        public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe, ValueNode address)
        {
            // emits a null-check for the otherwise unused receiver
            unsafe.get();
            b.addPush(returnKind, new UnsafeMemoryLoadNode(address, returnKind, NamedLocationIdentity.OFF_HEAP_LOCATION));
            b.getGraph().markUnsafeAccess();
            return true;
        }

        @Override
        public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe, ValueNode object, ValueNode offset)
        {
            // emits a null-check for the otherwise unused receiver
            unsafe.get();
            if (isVolatile)
            {
                b.add(new MembarNode(MemoryBarriers.JMM_PRE_VOLATILE_READ));
            }
            LocationIdentity locationIdentity = object.isNullConstant() ? NamedLocationIdentity.OFF_HEAP_LOCATION : LocationIdentity.any();
            b.addPush(returnKind, new RawLoadNode(object, offset, returnKind, locationIdentity));
            if (isVolatile)
            {
                b.add(new MembarNode(MemoryBarriers.JMM_POST_VOLATILE_READ));
            }
            b.getGraph().markUnsafeAccess();
            return true;
        }
    }

    // @class StandardGraphBuilderPlugins.UnsafePutPlugin
    public static final class UnsafePutPlugin implements InvocationPlugin
    {
        private final JavaKind kind;
        private final boolean hasBarrier;
        private final int preWrite;
        private final int postWrite;

        // @cons
        public UnsafePutPlugin(JavaKind kind, boolean isVolatile)
        {
            this(kind, isVolatile, MemoryBarriers.JMM_PRE_VOLATILE_WRITE, MemoryBarriers.JMM_POST_VOLATILE_WRITE);
        }

        // @cons
        private UnsafePutPlugin(JavaKind kind, boolean hasBarrier, int preWrite, int postWrite)
        {
            super();
            this.kind = kind;
            this.hasBarrier = hasBarrier;
            this.preWrite = preWrite;
            this.postWrite = postWrite;
        }

        public static UnsafePutPlugin putOrdered(JavaKind kind)
        {
            return new UnsafePutPlugin(kind, true, MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_STORE, 0);
        }

        @Override
        public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe, ValueNode address, ValueNode value)
        {
            // emits a null-check for the otherwise unused receiver
            unsafe.get();
            b.add(new UnsafeMemoryStoreNode(address, value, kind, NamedLocationIdentity.OFF_HEAP_LOCATION));
            b.getGraph().markUnsafeAccess();
            return true;
        }

        @Override
        public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe, ValueNode object, ValueNode offset, ValueNode value)
        {
            // emits a null-check for the otherwise unused receiver
            unsafe.get();
            if (hasBarrier)
            {
                b.add(new MembarNode(preWrite));
            }
            LocationIdentity locationIdentity = object.isNullConstant() ? NamedLocationIdentity.OFF_HEAP_LOCATION : LocationIdentity.any();
            b.add(new RawStoreNode(object, offset, value, kind, locationIdentity));
            if (hasBarrier)
            {
                b.add(new MembarNode(postWrite));
            }
            b.getGraph().markUnsafeAccess();
            return true;
        }
    }

    // @class StandardGraphBuilderPlugins.UnsafeFencePlugin
    public static final class UnsafeFencePlugin implements InvocationPlugin
    {
        private final int barriers;

        // @cons
        public UnsafeFencePlugin(int barriers)
        {
            super();
            this.barriers = barriers;
        }

        @Override
        public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe)
        {
            // emits a null-check for the otherwise unused receiver
            unsafe.get();
            b.add(new MembarNode(barriers));
            return true;
        }
    }

    // @class StandardGraphBuilderPlugins.DirectiveSpeculationReason
    private static final class DirectiveSpeculationReason implements SpeculationLog.SpeculationReason
    {
        private final BytecodePosition pos;

        // @cons
        private DirectiveSpeculationReason(BytecodePosition pos)
        {
            super();
            this.pos = pos;
        }

        @Override
        public int hashCode()
        {
            return pos.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof DirectiveSpeculationReason && ((DirectiveSpeculationReason) obj).pos.equals(this.pos);
        }
    }

    private static void registerGraalDirectivesPlugins(InvocationPlugins plugins)
    {
        Registration r = new Registration(plugins, GraalDirectives.class);
        r.register0("deoptimize", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                b.add(new DeoptimizeNode(DeoptimizationAction.None, DeoptimizationReason.TransferToInterpreter));
                return true;
            }
        });

        r.register0("deoptimizeAndInvalidate", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                b.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TransferToInterpreter));
                return true;
            }
        });

        r.register0("deoptimizeAndInvalidateWithSpeculation", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                GraalError.guarantee(b.getGraph().getSpeculationLog() != null, "A speculation log is need to use 'deoptimizeAndInvalidateWithSpeculation'");
                BytecodePosition pos = new BytecodePosition(null, b.getMethod(), b.bci());
                DirectiveSpeculationReason reason = new DirectiveSpeculationReason(pos);
                JavaConstant speculation;
                if (b.getGraph().getSpeculationLog().maySpeculate(reason))
                {
                    speculation = b.getGraph().getSpeculationLog().speculate(reason);
                }
                else
                {
                    speculation = JavaConstant.defaultForKind(JavaKind.Object);
                }
                b.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TransferToInterpreter, speculation));
                return true;
            }
        });

        r.register0("inCompiledCode", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                b.addPush(JavaKind.Boolean, ConstantNode.forBoolean(true));
                return true;
            }
        });

        r.register0("controlFlowAnchor", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                b.add(new ControlFlowAnchorNode());
                return true;
            }
        });

        r.register2("injectBranchProbability", double.class, boolean.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode probability, ValueNode condition)
            {
                b.addPush(JavaKind.Boolean, new BranchProbabilityNode(probability, condition));
                return true;
            }
        });

        InvocationPlugin blackholePlugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.add(new BlackholeNode(value));
                return true;
            }
        };

        InvocationPlugin bindToRegisterPlugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.add(new BindToRegisterNode(value));
                return true;
            }
        };
        for (JavaKind kind : JavaKind.values())
        {
            if ((kind.isPrimitive() && kind != JavaKind.Void) || kind == JavaKind.Object)
            {
                Class<?> javaClass = kind == JavaKind.Object ? Object.class : kind.toJavaClass();
                r.register1("blackhole", javaClass, blackholePlugin);
                r.register1("bindToRegister", javaClass, bindToRegisterPlugin);

                r.register1("opaque", javaClass, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
                    {
                        b.addPush(kind, new OpaqueNode(value));
                        return true;
                    }
                });
            }
        }

        InvocationPlugin spillPlugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver)
            {
                b.add(new SpillRegistersNode());
                return true;
            }
        };
        r.register0("spillRegisters", spillPlugin);

        r.register1("guardingNonNull", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
            {
                b.addPush(value.getStackKind(), b.nullCheckedValue(value));
                return true;
            }
        });

        r.register1("ensureVirtualized", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode object)
            {
                b.add(new EnsureVirtualizedNode(object, false));
                return true;
            }
        });
        r.register1("ensureVirtualizedHere", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode object)
            {
                b.add(new EnsureVirtualizedNode(object, true));
                return true;
            }
        });
    }
}
