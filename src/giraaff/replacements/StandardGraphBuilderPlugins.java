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

    public static void registerInvocationPlugins(MetaAccessProvider __metaAccess, SnippetReflectionProvider __snippetReflection, InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider, boolean __allowDeoptimization)
    {
        registerObjectPlugins(__plugins);
        registerClassPlugins(__plugins);
        registerMathPlugins(__plugins, __allowDeoptimization);
        registerUnsignedMathPlugins(__plugins);
        registerStringPlugins(__plugins, __bytecodeProvider, __snippetReflection);
        registerCharacterPlugins(__plugins);
        registerShortPlugins(__plugins);
        registerIntegerLongPlugins(__plugins, JavaKind.Int);
        registerIntegerLongPlugins(__plugins, JavaKind.Long);
        registerFloatPlugins(__plugins);
        registerDoublePlugins(__plugins);
        registerArraysPlugins(__plugins, __bytecodeProvider);
        registerArrayPlugins(__plugins, __bytecodeProvider);
        registerUnsafePlugins(__plugins, __bytecodeProvider);
        registerEdgesPlugins(__metaAccess, __plugins);
        registerGraalDirectivesPlugins(__plugins);
        registerBoxingPlugins(__plugins);
    }

    private static void registerStringPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider, SnippetReflectionProvider __snippetReflection)
    {
        final Registration __r = new Registration(__plugins, String.class, __bytecodeProvider);
        // @closure
        __r.register1("hashCode", Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                if (__receiver.isConstant())
                {
                    String __s = __snippetReflection.asObject(String.class, (JavaConstant) __receiver.get().asConstant());
                    __b.addPush(JavaKind.Int, __b.add(ConstantNode.forInt(__s.hashCode())));
                    return true;
                }
                return false;
            }
        });
    }

    private static void registerArraysPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        Registration __r = new Registration(__plugins, Arrays.class, __bytecodeProvider);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", boolean[].class, boolean[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", byte[].class, byte[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", short[].class, short[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", char[].class, char[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", int[].class, int[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", long[].class, long[].class);
    }

    private static void registerArrayPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        Registration __r = new Registration(__plugins, Array.class, __bytecodeProvider);
        // @closure
        __r.register2("newInstance", Class.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __unused, ValueNode __componentType, ValueNode __length)
            {
                __b.addPush(JavaKind.Object, new DynamicNewArrayNode(__componentType, __length, true));
                return true;
            }
        });
        __r.registerMethodSubstitution(ArraySubstitutions.class, "getLength", Object.class);
    }

    private static void registerUnsafePlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        Registration __r = new Registration(__plugins, "jdk.internal.misc.Unsafe", __bytecodeProvider);

        for (JavaKind __kind : JavaKind.values())
        {
            if ((__kind.isPrimitive() && __kind != JavaKind.Void) || __kind == JavaKind.Object)
            {
                Class<?> __javaClass = __kind == JavaKind.Object ? Object.class : __kind.toJavaClass();
                String __kindName = __kind.name();
                String __getName = "get" + __kindName;
                String __putName = "put" + __kindName;
                // object-based accesses
                __r.register3(__getName, Receiver.class, Object.class, long.class, new UnsafeGetPlugin(__kind, false));
                __r.register4(__putName, Receiver.class, Object.class, long.class, __javaClass, new UnsafePutPlugin(__kind, false));
                // volatile object-based accesses
                __r.register3(__getName + "Volatile", Receiver.class, Object.class, long.class, new UnsafeGetPlugin(__kind, true));
                __r.register4(__putName + "Volatile", Receiver.class, Object.class, long.class, __javaClass, new UnsafePutPlugin(__kind, true));
                // ordered object-based accesses
                __r.register4("put" + __kindName + "Release", Receiver.class, Object.class, long.class, __javaClass, UnsafePutPlugin.putOrdered(__kind));
                if (__kind != JavaKind.Boolean && __kind != JavaKind.Object)
                {
                    // raw accesses to memory addresses
                    __r.register2(__getName, Receiver.class, long.class, new UnsafeGetPlugin(__kind, false));
                    __r.register3(__putName, Receiver.class, long.class, __kind.toJavaClass(), new UnsafePutPlugin(__kind, false));
                }
            }
        }

        // Accesses to native memory addresses.
        __r.register2("getAddress", Receiver.class, long.class, new UnsafeGetPlugin(JavaKind.Long, false));
        __r.register3("putAddress", Receiver.class, long.class, long.class, new UnsafePutPlugin(JavaKind.Long, false));

        for (JavaKind __kind : new JavaKind[] { JavaKind.Int, JavaKind.Long, JavaKind.Object })
        {
            Class<?> __javaClass = __kind == JavaKind.Object ? Object.class : __kind.toJavaClass();
            // @closure
            __r.register5("compareAndSet" + __kind.name(), Receiver.class, Object.class, long.class, __javaClass, __javaClass, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __unsafe, ValueNode __object, ValueNode __offset, ValueNode __expected, ValueNode __x)
                {
                    // emits a null-check for the otherwise unused receiver
                    __unsafe.get();
                    __b.addPush(JavaKind.Int, new UnsafeCompareAndSwapNode(__object, __offset, __expected, __x, __kind, LocationIdentity.any()));
                    __b.getGraph().markUnsafeAccess();
                    return true;
                }
            });
        }

        // @closure
        __r.register2("allocateInstance", Receiver.class, Class.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __unsafe, ValueNode __clazz)
            {
                // emits a null-check for the otherwise unused receiver
                __unsafe.get();
                __b.addPush(JavaKind.Object, new DynamicNewInstanceNode(__b.nullCheckedValue(__clazz, DeoptimizationAction.None), true));
                return true;
            }
        });

        __r.register1("loadFence", Receiver.class, new UnsafeFencePlugin(MemoryBarriers.LOAD_LOAD | MemoryBarriers.LOAD_STORE));
        __r.register1("storeFence", Receiver.class, new UnsafeFencePlugin(MemoryBarriers.STORE_STORE | MemoryBarriers.LOAD_STORE));
        __r.register1("fullFence", Receiver.class, new UnsafeFencePlugin(MemoryBarriers.LOAD_LOAD | MemoryBarriers.STORE_STORE | MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_LOAD));
    }

    private static void registerIntegerLongPlugins(InvocationPlugins __plugins, JavaKind __kind)
    {
        Class<?> __declaringClass = __kind.toBoxedJavaClass();
        Class<?> __type = __kind.toJavaClass();
        Registration __r = new Registration(__plugins, __declaringClass);
        // @closure
        __r.register1("reverseBytes", __type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.push(__kind, __b.append(new ReverseBytesNode(__value).canonical(null)));
                return true;
            }
        });
        // @closure
        __r.register2("divideUnsigned", __type, __type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __dividend, ValueNode __divisor)
            {
                __b.push(__kind, __b.append(UnsignedDivNode.create(__dividend, __divisor, NodeView.DEFAULT)));
                return true;
            }
        });
        // @closure
        __r.register2("remainderUnsigned", __type, __type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __dividend, ValueNode __divisor)
            {
                __b.push(__kind, __b.append(UnsignedRemNode.create(__dividend, __divisor, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    private static void registerCharacterPlugins(InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, Character.class);
        // @closure
        __r.register1("reverseBytes", char.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                // return (char) (Integer.reverse(i) >> 16);
                ReverseBytesNode __reverse = __b.add(new ReverseBytesNode(__value));
                RightShiftNode __rightShift = __b.add(new RightShiftNode(__reverse, __b.add(ConstantNode.forInt(16))));
                ZeroExtendNode __charCast = __b.add(new ZeroExtendNode(__b.add(new NarrowNode(__rightShift, 16)), 32));
                __b.push(JavaKind.Char, __b.append(__charCast.canonical(null)));
                return true;
            }
        });
    }

    private static void registerShortPlugins(InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, Short.class);
        // @closure
        __r.register1("reverseBytes", short.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                // return (short) (Integer.reverse(i) >> 16);
                ReverseBytesNode __reverse = __b.add(new ReverseBytesNode(__value));
                RightShiftNode __rightShift = __b.add(new RightShiftNode(__reverse, __b.add(ConstantNode.forInt(16))));
                SignExtendNode __charCast = __b.add(new SignExtendNode(__b.add(new NarrowNode(__rightShift, 16)), 32));
                __b.push(JavaKind.Short, __b.append(__charCast.canonical(null)));
                return true;
            }
        });
    }

    private static void registerFloatPlugins(InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, Float.class);
        // @closure
        __r.register1("floatToRawIntBits", float.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.push(JavaKind.Int, __b.append(ReinterpretNode.create(JavaKind.Int, __value, NodeView.DEFAULT)));
                return true;
            }
        });
        // @closure
        __r.register1("floatToIntBits", float.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                LogicNode __notNan = __b.append(FloatEqualsNode.create(__value, __value, NodeView.DEFAULT));
                ValueNode __raw = __b.append(ReinterpretNode.create(JavaKind.Int, __value, NodeView.DEFAULT));
                ValueNode __result = __b.append(ConditionalNode.create(__notNan, __raw, ConstantNode.forInt(0x7fc00000), NodeView.DEFAULT));
                __b.push(JavaKind.Int, __result);
                return true;
            }
        });
        // @closure
        __r.register1("intBitsToFloat", int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.push(JavaKind.Float, __b.append(ReinterpretNode.create(JavaKind.Float, __value, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    private static void registerDoublePlugins(InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, Double.class);
        // @closure
        __r.register1("doubleToRawLongBits", double.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.push(JavaKind.Long, __b.append(ReinterpretNode.create(JavaKind.Long, __value, NodeView.DEFAULT)));
                return true;
            }
        });
        // @closure
        __r.register1("doubleToLongBits", double.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                LogicNode __notNan = __b.append(FloatEqualsNode.create(__value, __value, NodeView.DEFAULT));
                ValueNode __raw = __b.append(ReinterpretNode.create(JavaKind.Long, __value, NodeView.DEFAULT));
                ValueNode __result = __b.append(ConditionalNode.create(__notNan, __raw, ConstantNode.forLong(0x7ff8000000000000L), NodeView.DEFAULT));
                __b.push(JavaKind.Long, __result);
                return true;
            }
        });
        // @closure
        __r.register1("longBitsToDouble", long.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.push(JavaKind.Double, __b.append(ReinterpretNode.create(JavaKind.Double, __value, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    private static void registerMathPlugins(InvocationPlugins __plugins, boolean __allowDeoptimization)
    {
        Registration __r = new Registration(__plugins, Math.class);
        if (__allowDeoptimization)
        {
            for (JavaKind __kind : new JavaKind[] { JavaKind.Int, JavaKind.Long })
            {
                Class<?> __type = __kind.toJavaClass();

                // @closure
                __r.register1("decrementExact", __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __x)
                    {
                        __b.addPush(__kind, new IntegerSubExactNode(__x, ConstantNode.forIntegerKind(__kind, 1)));
                        return true;
                    }
                });

                // @closure
                __r.register1("incrementExact", __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __x)
                    {
                        __b.addPush(__kind, new IntegerAddExactNode(__x, ConstantNode.forIntegerKind(__kind, 1)));
                        return true;
                    }
                });

                // @closure
                __r.register2("addExact", __type, __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __x, ValueNode __y)
                    {
                        __b.addPush(__kind, new IntegerAddExactNode(__x, __y));
                        return true;
                    }
                });

                // @closure
                __r.register2("subtractExact", __type, __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __x, ValueNode __y)
                    {
                        __b.addPush(__kind, new IntegerSubExactNode(__x, __y));
                        return true;
                    }
                });

                // @closure
                __r.register2("multiplyExact", __type, __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __x, ValueNode __y)
                    {
                        __b.addPush(__kind, new IntegerMulExactNode(__x, __y));
                        return true;
                    }
                });
            }
        }
        // @closure
        __r.register1("abs", Float.TYPE, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.push(JavaKind.Float, __b.append(new AbsNode(__value).canonical(null)));
                return true;
            }
        });
        // @closure
        __r.register1("abs", Double.TYPE, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.push(JavaKind.Double, __b.append(new AbsNode(__value).canonical(null)));
                return true;
            }
        });
        // @closure
        __r.register1("sqrt", Double.TYPE, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.push(JavaKind.Double, __b.append(SqrtNode.create(__value, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    // @class StandardGraphBuilderPlugins.UnsignedMathPlugin
    public static final class UnsignedMathPlugin implements InvocationPlugin
    {
        // @field
        private final Condition condition;

        // @cons
        public UnsignedMathPlugin(Condition __condition)
        {
            super();
            this.condition = __condition;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __x, ValueNode __y)
        {
            CanonicalizedCondition __canonical = condition.canonicalize();
            StructuredGraph __graph = __b.getGraph();

            ValueNode __lhs = __canonical.mustMirror() ? __y : __x;
            ValueNode __rhs = __canonical.mustMirror() ? __x : __y;

            ValueNode __trueValue = ConstantNode.forBoolean(!__canonical.mustNegate(), __graph);
            ValueNode __falseValue = ConstantNode.forBoolean(__canonical.mustNegate(), __graph);

            LogicNode __compare = CompareNode.createCompareNode(__graph, __b.getConstantReflection(), __b.getMetaAccess(), null, __canonical.getCanonicalCondition(), __lhs, __rhs, NodeView.DEFAULT);
            __b.addPush(JavaKind.Boolean, new ConditionalNode(__compare, __trueValue, __falseValue));
            return true;
        }
    }

    private static void registerUnsignedMathPlugins(InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, UnsignedMath.class);
        __r.register2("aboveThan", int.class, int.class, new UnsignedMathPlugin(Condition.AT));
        __r.register2("aboveThan", long.class, long.class, new UnsignedMathPlugin(Condition.AT));
        __r.register2("belowThan", int.class, int.class, new UnsignedMathPlugin(Condition.BT));
        __r.register2("belowThan", long.class, long.class, new UnsignedMathPlugin(Condition.BT));
        __r.register2("aboveOrEqual", int.class, int.class, new UnsignedMathPlugin(Condition.AE));
        __r.register2("aboveOrEqual", long.class, long.class, new UnsignedMathPlugin(Condition.AE));
        __r.register2("belowOrEqual", int.class, int.class, new UnsignedMathPlugin(Condition.BE));
        __r.register2("belowOrEqual", long.class, long.class, new UnsignedMathPlugin(Condition.BE));
    }

    protected static void registerBoxingPlugins(InvocationPlugins __plugins)
    {
        for (JavaKind __kind : JavaKind.values())
        {
            if (__kind.isPrimitive() && __kind != JavaKind.Void)
            {
                new BoxPlugin(__kind).register(__plugins);
                new UnboxPlugin(__kind).register(__plugins);
            }
        }
    }

    private static void registerObjectPlugins(InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, Object.class);
        // @closure
        __r.register1("<init>", Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                /*
                 * Object.<init> is a common instrumentation point so only perform this rewrite if
                 * the current definition is the normal empty method with a single return bytecode.
                 * The finalizer registration will instead be performed by the BytecodeParser.
                 */
                if (__targetMethod.getCodeSize() == 1)
                {
                    ValueNode __object = __receiver.get();
                    if (RegisterFinalizerNode.mayHaveFinalizer(__object, __b.getAssumptions()))
                    {
                        __b.add(new RegisterFinalizerNode(__object));
                    }
                    return true;
                }
                return false;
            }
        });
        // @closure
        __r.register1("getClass", Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                ValueNode __object = __receiver.get();
                ValueNode __folded = GetClassNode.tryFold(__b.getMetaAccess(), __b.getConstantReflection(), NodeView.DEFAULT, GraphUtil.originalValue(__object));
                if (__folded != null)
                {
                    __b.addPush(JavaKind.Object, __folded);
                }
                else
                {
                    Stamp __stamp = StampFactory.objectNonNull(TypeReference.createTrusted(__b.getAssumptions(), __b.getMetaAccess().lookupJavaType(Class.class)));
                    __b.addPush(JavaKind.Object, new GetClassNode(__stamp, __object));
                }
                return true;
            }
        });
    }

    private static void registerClassPlugins(InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, Class.class);
        // @closure
        __r.register2("isInstance", Receiver.class, Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __type, ValueNode __object)
            {
                LogicNode __condition = __b.append(InstanceOfDynamicNode.create(__b.getAssumptions(), __b.getConstantReflection(), __type.get(), __object, false));
                __b.push(JavaKind.Boolean, __b.append(new ConditionalNode(__condition).canonical(null)));
                return true;
            }
        });
        // @closure
        __r.register2("isAssignableFrom", Receiver.class, Class.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __type, ValueNode __otherType)
            {
                ClassIsAssignableFromNode __condition = __b.append(new ClassIsAssignableFromNode(__type.get(), __otherType));
                __b.push(JavaKind.Boolean, __b.append(new ConditionalNode(__condition).canonical(null)));
                return true;
            }
        });
    }

    /**
     * Substitutions for improving the performance of some critical methods in {@link Edges}. These
     * substitutions improve the performance by forcing the relevant methods to be inlined
     * (intrinsification being a special form of inlining) and removing a checked cast.
     */
    private static void registerEdgesPlugins(MetaAccessProvider __metaAccess, InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, Edges.class);
        for (Class<?> __c : new Class<?>[] { Node.class, NodeList.class })
        {
            // @closure
            __r.register2("get" + __c.getSimpleName() + "Unsafe", Node.class, long.class, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __node, ValueNode __offset)
                {
                    ObjectStamp __stamp = StampFactory.object(TypeReference.createTrusted(__b.getAssumptions(), __metaAccess.lookupJavaType(__c)));
                    RawLoadNode __value = __b.add(new RawLoadNode(__stamp, __node, __offset, LocationIdentity.any(), JavaKind.Object));
                    __b.addPush(JavaKind.Object, __value);
                    return true;
                }
            });
            // @closure
            __r.register3("put" + __c.getSimpleName() + "Unsafe", Node.class, long.class, __c, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __node, ValueNode __offset, ValueNode __value)
                {
                    __b.add(new RawStoreNode(__node, __offset, __value, JavaKind.Object, LocationIdentity.any()));
                    return true;
                }
            });
        }
    }

    // @class StandardGraphBuilderPlugins.BoxPlugin
    public static final class BoxPlugin implements InvocationPlugin
    {
        // @field
        private final JavaKind kind;

        // @cons
        BoxPlugin(JavaKind __kind)
        {
            super();
            this.kind = __kind;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
        {
            if (__b.parsingIntrinsic())
            {
                ResolvedJavaMethod __rootMethod = __b.getGraph().method();
                if (__b.getMetaAccess().lookupJavaType(BoxingSnippets.class).isAssignableFrom(__rootMethod.getDeclaringClass()))
                {
                    // disable invocation plugins for boxing snippets, so that the original JDK methods are inlined
                    return false;
                }
            }
            ResolvedJavaType __resultType = __b.getMetaAccess().lookupJavaType(kind.toBoxedJavaClass());
            __b.addPush(JavaKind.Object, new BoxNode(__value, __resultType, kind));
            return true;
        }

        void register(InvocationPlugins __plugins)
        {
            __plugins.register(this, kind.toBoxedJavaClass(), "valueOf", kind.toJavaClass());
        }
    }

    // @class StandardGraphBuilderPlugins.UnboxPlugin
    public static final class UnboxPlugin implements InvocationPlugin
    {
        // @field
        private final JavaKind kind;

        // @cons
        UnboxPlugin(JavaKind __kind)
        {
            super();
            this.kind = __kind;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
        {
            if (__b.parsingIntrinsic())
            {
                ResolvedJavaMethod __rootMethod = __b.getGraph().method();
                if (__b.getMetaAccess().lookupJavaType(BoxingSnippets.class).isAssignableFrom(__rootMethod.getDeclaringClass()))
                {
                    // disable invocation plugins for unboxing snippets, so that the original JDK methods are inlined
                    return false;
                }
            }
            ValueNode __valueNode = UnboxNode.create(__b.getMetaAccess(), __b.getConstantReflection(), __receiver.get(), kind);
            __b.addPush(kind, __valueNode);
            return true;
        }

        void register(InvocationPlugins __plugins)
        {
            String __name = kind.toJavaClass().getSimpleName() + "Value";
            __plugins.register(this, kind.toBoxedJavaClass(), __name, Receiver.class);
        }
    }

    // @class StandardGraphBuilderPlugins.UnsafeGetPlugin
    public static final class UnsafeGetPlugin implements InvocationPlugin
    {
        // @field
        private final JavaKind returnKind;
        // @field
        private final boolean isVolatile;

        // @cons
        public UnsafeGetPlugin(JavaKind __returnKind, boolean __isVolatile)
        {
            super();
            this.returnKind = __returnKind;
            this.isVolatile = __isVolatile;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __unsafe, ValueNode __address)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            __b.addPush(returnKind, new UnsafeMemoryLoadNode(__address, returnKind, NamedLocationIdentity.OFF_HEAP_LOCATION));
            __b.getGraph().markUnsafeAccess();
            return true;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __unsafe, ValueNode __object, ValueNode __offset)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            if (isVolatile)
            {
                __b.add(new MembarNode(MemoryBarriers.JMM_PRE_VOLATILE_READ));
            }
            LocationIdentity __locationIdentity = __object.isNullConstant() ? NamedLocationIdentity.OFF_HEAP_LOCATION : LocationIdentity.any();
            __b.addPush(returnKind, new RawLoadNode(__object, __offset, returnKind, __locationIdentity));
            if (isVolatile)
            {
                __b.add(new MembarNode(MemoryBarriers.JMM_POST_VOLATILE_READ));
            }
            __b.getGraph().markUnsafeAccess();
            return true;
        }
    }

    // @class StandardGraphBuilderPlugins.UnsafePutPlugin
    public static final class UnsafePutPlugin implements InvocationPlugin
    {
        // @field
        private final JavaKind kind;
        // @field
        private final boolean hasBarrier;
        // @field
        private final int preWrite;
        // @field
        private final int postWrite;

        // @cons
        public UnsafePutPlugin(JavaKind __kind, boolean __isVolatile)
        {
            this(__kind, __isVolatile, MemoryBarriers.JMM_PRE_VOLATILE_WRITE, MemoryBarriers.JMM_POST_VOLATILE_WRITE);
        }

        // @cons
        private UnsafePutPlugin(JavaKind __kind, boolean __hasBarrier, int __preWrite, int __postWrite)
        {
            super();
            this.kind = __kind;
            this.hasBarrier = __hasBarrier;
            this.preWrite = __preWrite;
            this.postWrite = __postWrite;
        }

        public static UnsafePutPlugin putOrdered(JavaKind __kind)
        {
            return new UnsafePutPlugin(__kind, true, MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_STORE, 0);
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __unsafe, ValueNode __address, ValueNode __value)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            __b.add(new UnsafeMemoryStoreNode(__address, __value, kind, NamedLocationIdentity.OFF_HEAP_LOCATION));
            __b.getGraph().markUnsafeAccess();
            return true;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __unsafe, ValueNode __object, ValueNode __offset, ValueNode __value)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            if (hasBarrier)
            {
                __b.add(new MembarNode(preWrite));
            }
            LocationIdentity __locationIdentity = __object.isNullConstant() ? NamedLocationIdentity.OFF_HEAP_LOCATION : LocationIdentity.any();
            __b.add(new RawStoreNode(__object, __offset, __value, kind, __locationIdentity));
            if (hasBarrier)
            {
                __b.add(new MembarNode(postWrite));
            }
            __b.getGraph().markUnsafeAccess();
            return true;
        }
    }

    // @class StandardGraphBuilderPlugins.UnsafeFencePlugin
    public static final class UnsafeFencePlugin implements InvocationPlugin
    {
        // @field
        private final int barriers;

        // @cons
        public UnsafeFencePlugin(int __barriers)
        {
            super();
            this.barriers = __barriers;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __unsafe)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            __b.add(new MembarNode(barriers));
            return true;
        }
    }

    // @class StandardGraphBuilderPlugins.DirectiveSpeculationReason
    private static final class DirectiveSpeculationReason implements SpeculationLog.SpeculationReason
    {
        // @field
        private final BytecodePosition pos;

        // @cons
        private DirectiveSpeculationReason(BytecodePosition __pos)
        {
            super();
            this.pos = __pos;
        }

        @Override
        public int hashCode()
        {
            return pos.hashCode();
        }

        @Override
        public boolean equals(Object __obj)
        {
            return __obj instanceof DirectiveSpeculationReason && ((DirectiveSpeculationReason) __obj).pos.equals(this.pos);
        }
    }

    private static void registerGraalDirectivesPlugins(InvocationPlugins __plugins)
    {
        Registration __r = new Registration(__plugins, GraalDirectives.class);
        // @closure
        __r.register0("deoptimize", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                __b.add(new DeoptimizeNode(DeoptimizationAction.None, DeoptimizationReason.TransferToInterpreter));
                return true;
            }
        });

        // @closure
        __r.register0("deoptimizeAndInvalidate", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                __b.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TransferToInterpreter));
                return true;
            }
        });

        // @closure
        __r.register0("deoptimizeAndInvalidateWithSpeculation", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                GraalError.guarantee(__b.getGraph().getSpeculationLog() != null, "A speculation log is need to use 'deoptimizeAndInvalidateWithSpeculation'");
                BytecodePosition __pos = new BytecodePosition(null, __b.getMethod(), __b.bci());
                DirectiveSpeculationReason __reason = new DirectiveSpeculationReason(__pos);
                JavaConstant __speculation;
                if (__b.getGraph().getSpeculationLog().maySpeculate(__reason))
                {
                    __speculation = __b.getGraph().getSpeculationLog().speculate(__reason);
                }
                else
                {
                    __speculation = JavaConstant.defaultForKind(JavaKind.Object);
                }
                __b.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TransferToInterpreter, __speculation));
                return true;
            }
        });

        // @closure
        __r.register0("inCompiledCode", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                __b.addPush(JavaKind.Boolean, ConstantNode.forBoolean(true));
                return true;
            }
        });

        // @closure
        __r.register0("controlFlowAnchor", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                __b.add(new ControlFlowAnchorNode());
                return true;
            }
        });

        // @closure
        __r.register2("injectBranchProbability", double.class, boolean.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __probability, ValueNode __condition)
            {
                __b.addPush(JavaKind.Boolean, new BranchProbabilityNode(__probability, __condition));
                return true;
            }
        });

        // @closure
        InvocationPlugin blackholePlugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.add(new BlackholeNode(__value));
                return true;
            }
        };

        // @closure
        InvocationPlugin bindToRegisterPlugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.add(new BindToRegisterNode(__value));
                return true;
            }
        };
        for (JavaKind __kind : JavaKind.values())
        {
            if ((__kind.isPrimitive() && __kind != JavaKind.Void) || __kind == JavaKind.Object)
            {
                Class<?> __javaClass = __kind == JavaKind.Object ? Object.class : __kind.toJavaClass();
                __r.register1("blackhole", __javaClass, blackholePlugin);
                __r.register1("bindToRegister", __javaClass, bindToRegisterPlugin);

                // @closure
                __r.register1("opaque", __javaClass, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
                    {
                        __b.addPush(__kind, new OpaqueNode(__value));
                        return true;
                    }
                });
            }
        }

        // @closure
        InvocationPlugin spillPlugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver)
            {
                __b.add(new SpillRegistersNode());
                return true;
            }
        };
        __r.register0("spillRegisters", spillPlugin);

        // @closure
        __r.register1("guardingNonNull", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __value)
            {
                __b.addPush(__value.getStackKind(), __b.nullCheckedValue(__value));
                return true;
            }
        });

        // @closure
        __r.register1("ensureVirtualized", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __object)
            {
                __b.add(new EnsureVirtualizedNode(__object, false));
                return true;
            }
        });
        // @closure
        __r.register1("ensureVirtualizedHere", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, Receiver __receiver, ValueNode __object)
            {
                __b.add(new EnsureVirtualizedNode(__object, true));
                return true;
            }
        });
    }
}
