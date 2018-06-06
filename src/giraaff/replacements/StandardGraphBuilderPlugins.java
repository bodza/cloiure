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
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.NarrowNode;
import giraaff.nodes.calc.RightShiftNode;
import giraaff.nodes.calc.SignExtendNode;
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
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
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

///
// Provides non-runtime specific {@link InvocationPlugin}s.
///
// @class StandardGraphBuilderPlugins
public final class StandardGraphBuilderPlugins
{
    // @cons StandardGraphBuilderPlugins
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
        registerArraysPlugins(__plugins, __bytecodeProvider);
        registerArrayPlugins(__plugins, __bytecodeProvider);
        registerUnsafePlugins(__plugins, __bytecodeProvider);
        registerEdgesPlugins(__metaAccess, __plugins);
        registerGraalDirectivesPlugins(__plugins);
        registerBoxingPlugins(__plugins);
    }

    private static void registerStringPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider, SnippetReflectionProvider __snippetReflection)
    {
        final InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, String.class, __bytecodeProvider);
        // @closure
        __r.register1("hashCode", InvocationPlugin.Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
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
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Arrays.class, __bytecodeProvider);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", boolean[].class, boolean[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", byte[].class, byte[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", short[].class, short[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", char[].class, char[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", int[].class, int[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", long[].class, long[].class);
    }

    private static void registerArrayPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Array.class, __bytecodeProvider);
        // @closure
        __r.register2("newInstance", Class.class, int.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unused, ValueNode __componentType, ValueNode __length)
            {
                __b.addPush(JavaKind.Object, new DynamicNewArrayNode(__componentType, __length, true));
                return true;
            }
        });
        __r.registerMethodSubstitution(ArraySubstitutions.class, "getLength", Object.class);
    }

    private static void registerUnsafePlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, "jdk.internal.misc.Unsafe", __bytecodeProvider);

        for (JavaKind __kind : JavaKind.values())
        {
            if ((__kind.isPrimitive() && __kind != JavaKind.Void) || __kind == JavaKind.Object)
            {
                Class<?> __javaClass = __kind == JavaKind.Object ? Object.class : __kind.toJavaClass();
                String __kindName = __kind.name();
                String __getName = "get" + __kindName;
                String __putName = "put" + __kindName;
                // object-based accesses
                __r.register3(__getName, InvocationPlugin.Receiver.class, Object.class, long.class, new StandardGraphBuilderPlugins.UnsafeGetPlugin(__kind, false));
                __r.register4(__putName, InvocationPlugin.Receiver.class, Object.class, long.class, __javaClass, new StandardGraphBuilderPlugins.UnsafePutPlugin(__kind, false));
                // volatile object-based accesses
                __r.register3(__getName + "Volatile", InvocationPlugin.Receiver.class, Object.class, long.class, new StandardGraphBuilderPlugins.UnsafeGetPlugin(__kind, true));
                __r.register4(__putName + "Volatile", InvocationPlugin.Receiver.class, Object.class, long.class, __javaClass, new StandardGraphBuilderPlugins.UnsafePutPlugin(__kind, true));
                // ordered object-based accesses
                __r.register4("put" + __kindName + "Release", InvocationPlugin.Receiver.class, Object.class, long.class, __javaClass, StandardGraphBuilderPlugins.UnsafePutPlugin.putOrdered(__kind));
                if (__kind != JavaKind.Boolean && __kind != JavaKind.Object)
                {
                    // raw accesses to memory addresses
                    __r.register2(__getName, InvocationPlugin.Receiver.class, long.class, new StandardGraphBuilderPlugins.UnsafeGetPlugin(__kind, false));
                    __r.register3(__putName, InvocationPlugin.Receiver.class, long.class, __kind.toJavaClass(), new StandardGraphBuilderPlugins.UnsafePutPlugin(__kind, false));
                }
            }
        }

        // Accesses to native memory addresses.
        __r.register2("getAddress", InvocationPlugin.Receiver.class, long.class, new StandardGraphBuilderPlugins.UnsafeGetPlugin(JavaKind.Long, false));
        __r.register3("putAddress", InvocationPlugin.Receiver.class, long.class, long.class, new StandardGraphBuilderPlugins.UnsafePutPlugin(JavaKind.Long, false));

        for (JavaKind __kind : new JavaKind[] { JavaKind.Int, JavaKind.Long, JavaKind.Object })
        {
            Class<?> __javaClass = __kind == JavaKind.Object ? Object.class : __kind.toJavaClass();
            // @closure
            __r.register5("compareAndSet" + __kind.name(), InvocationPlugin.Receiver.class, Object.class, long.class, __javaClass, __javaClass, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe, ValueNode __object, ValueNode __offset, ValueNode __expected, ValueNode __x)
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
        __r.register2("allocateInstance", InvocationPlugin.Receiver.class, Class.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe, ValueNode __clazz)
            {
                // emits a null-check for the otherwise unused receiver
                __unsafe.get();
                __b.addPush(JavaKind.Object, new DynamicNewInstanceNode(__b.nullCheckedValue(__clazz, DeoptimizationAction.None), true));
                return true;
            }
        });

        __r.register1("loadFence", InvocationPlugin.Receiver.class, new StandardGraphBuilderPlugins.UnsafeFencePlugin(MemoryBarriers.LOAD_LOAD | MemoryBarriers.LOAD_STORE));
        __r.register1("storeFence", InvocationPlugin.Receiver.class, new StandardGraphBuilderPlugins.UnsafeFencePlugin(MemoryBarriers.STORE_STORE | MemoryBarriers.LOAD_STORE));
        __r.register1("fullFence", InvocationPlugin.Receiver.class, new StandardGraphBuilderPlugins.UnsafeFencePlugin(MemoryBarriers.LOAD_LOAD | MemoryBarriers.STORE_STORE | MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_LOAD));
    }

    private static void registerIntegerLongPlugins(InvocationPlugins __plugins, JavaKind __kind)
    {
        Class<?> __declaringClass = __kind.toBoxedJavaClass();
        Class<?> __type = __kind.toJavaClass();
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, __declaringClass);
        // @closure
        __r.register1("reverseBytes", __type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
            {
                __b.push(__kind, __b.append(new ReverseBytesNode(__value).canonical(null)));
                return true;
            }
        });
        // @closure
        __r.register2("divideUnsigned", __type, __type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __dividend, ValueNode __divisor)
            {
                __b.push(__kind, __b.append(UnsignedDivNode.create(__dividend, __divisor, NodeView.DEFAULT)));
                return true;
            }
        });
        // @closure
        __r.register2("remainderUnsigned", __type, __type, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __dividend, ValueNode __divisor)
            {
                __b.push(__kind, __b.append(UnsignedRemNode.create(__dividend, __divisor, NodeView.DEFAULT)));
                return true;
            }
        });
    }

    private static void registerCharacterPlugins(InvocationPlugins __plugins)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Character.class);
        // @closure
        __r.register1("reverseBytes", char.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
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
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Short.class);
        // @closure
        __r.register1("reverseBytes", short.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
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

    private static void registerMathPlugins(InvocationPlugins __plugins, boolean __allowDeoptimization)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Math.class);
        if (__allowDeoptimization)
        {
            for (JavaKind __kind : new JavaKind[] { JavaKind.Int, JavaKind.Long })
            {
                Class<?> __type = __kind.toJavaClass();

                // @closure
                __r.register1("decrementExact", __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __x)
                    {
                        __b.addPush(__kind, new IntegerSubExactNode(__x, ConstantNode.forIntegerKind(__kind, 1)));
                        return true;
                    }
                });

                // @closure
                __r.register1("incrementExact", __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __x)
                    {
                        __b.addPush(__kind, new IntegerAddExactNode(__x, ConstantNode.forIntegerKind(__kind, 1)));
                        return true;
                    }
                });

                // @closure
                __r.register2("addExact", __type, __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __x, ValueNode __y)
                    {
                        __b.addPush(__kind, new IntegerAddExactNode(__x, __y));
                        return true;
                    }
                });

                // @closure
                __r.register2("subtractExact", __type, __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __x, ValueNode __y)
                    {
                        __b.addPush(__kind, new IntegerSubExactNode(__x, __y));
                        return true;
                    }
                });

                // @closure
                __r.register2("multiplyExact", __type, __type, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __x, ValueNode __y)
                    {
                        __b.addPush(__kind, new IntegerMulExactNode(__x, __y));
                        return true;
                    }
                });
            }
        }
    }

    // @class StandardGraphBuilderPlugins.UnsignedMathPlugin
    public static final class UnsignedMathPlugin implements InvocationPlugin
    {
        // @field
        private final Condition ___condition;

        // @cons StandardGraphBuilderPlugins.UnsignedMathPlugin
        public UnsignedMathPlugin(Condition __condition)
        {
            super();
            this.___condition = __condition;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __x, ValueNode __y)
        {
            Condition.CanonicalizedCondition __canonical = this.___condition.canonicalize();
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
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, UnsignedMath.class);
        __r.register2("aboveThan", int.class, int.class, new StandardGraphBuilderPlugins.UnsignedMathPlugin(Condition.AT));
        __r.register2("aboveThan", long.class, long.class, new StandardGraphBuilderPlugins.UnsignedMathPlugin(Condition.AT));
        __r.register2("belowThan", int.class, int.class, new StandardGraphBuilderPlugins.UnsignedMathPlugin(Condition.BT));
        __r.register2("belowThan", long.class, long.class, new StandardGraphBuilderPlugins.UnsignedMathPlugin(Condition.BT));
        __r.register2("aboveOrEqual", int.class, int.class, new StandardGraphBuilderPlugins.UnsignedMathPlugin(Condition.AE));
        __r.register2("aboveOrEqual", long.class, long.class, new StandardGraphBuilderPlugins.UnsignedMathPlugin(Condition.AE));
        __r.register2("belowOrEqual", int.class, int.class, new StandardGraphBuilderPlugins.UnsignedMathPlugin(Condition.BE));
        __r.register2("belowOrEqual", long.class, long.class, new StandardGraphBuilderPlugins.UnsignedMathPlugin(Condition.BE));
    }

    protected static void registerBoxingPlugins(InvocationPlugins __plugins)
    {
        for (JavaKind __kind : JavaKind.values())
        {
            if (__kind.isPrimitive() && __kind != JavaKind.Void)
            {
                new StandardGraphBuilderPlugins.BoxPlugin(__kind).register(__plugins);
                new StandardGraphBuilderPlugins.UnboxPlugin(__kind).register(__plugins);
            }
        }
    }

    private static void registerObjectPlugins(InvocationPlugins __plugins)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Object.class);
        // @closure
        __r.register1("<init>", InvocationPlugin.Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                // Object.<init> is a common instrumentation point so only perform this rewrite if
                // the current definition is the normal empty method with a single return bytecode.
                // The finalizer registration will instead be performed by the BytecodeParser.
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
        __r.register1("getClass", InvocationPlugin.Receiver.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
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
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Class.class);
        // @closure
        __r.register2("isInstance", InvocationPlugin.Receiver.class, Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __type, ValueNode __object)
            {
                LogicNode __condition = __b.append(InstanceOfDynamicNode.create(__b.getAssumptions(), __b.getConstantReflection(), __type.get(), __object, false));
                __b.push(JavaKind.Boolean, __b.append(new ConditionalNode(__condition).canonical(null)));
                return true;
            }
        });
        // @closure
        __r.register2("isAssignableFrom", InvocationPlugin.Receiver.class, Class.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __type, ValueNode __otherType)
            {
                ClassIsAssignableFromNode __condition = __b.append(new ClassIsAssignableFromNode(__type.get(), __otherType));
                __b.push(JavaKind.Boolean, __b.append(new ConditionalNode(__condition).canonical(null)));
                return true;
            }
        });
    }

    ///
    // Substitutions for improving the performance of some critical methods in {@link Edges}. These
    // substitutions improve the performance by forcing the relevant methods to be inlined
    // (intrinsification being a special form of inlining) and removing a checked cast.
    ///
    private static void registerEdgesPlugins(MetaAccessProvider __metaAccess, InvocationPlugins __plugins)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Edges.class);
        for (Class<?> __c : new Class<?>[] { Node.class, NodeList.class })
        {
            // @closure
            __r.register2("get" + __c.getSimpleName() + "Unsafe", Node.class, long.class, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __node, ValueNode __offset)
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
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __node, ValueNode __offset, ValueNode __value)
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
        private final JavaKind ___kind;

        // @cons StandardGraphBuilderPlugins.BoxPlugin
        BoxPlugin(JavaKind __kind)
        {
            super();
            this.___kind = __kind;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
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
            ResolvedJavaType __resultType = __b.getMetaAccess().lookupJavaType(this.___kind.toBoxedJavaClass());
            __b.addPush(JavaKind.Object, new BoxNode(__value, __resultType, this.___kind));
            return true;
        }

        void register(InvocationPlugins __plugins)
        {
            __plugins.register(this, this.___kind.toBoxedJavaClass(), "valueOf", this.___kind.toJavaClass());
        }
    }

    // @class StandardGraphBuilderPlugins.UnboxPlugin
    public static final class UnboxPlugin implements InvocationPlugin
    {
        // @field
        private final JavaKind ___kind;

        // @cons StandardGraphBuilderPlugins.UnboxPlugin
        UnboxPlugin(JavaKind __kind)
        {
            super();
            this.___kind = __kind;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
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
            ValueNode __valueNode = UnboxNode.create(__b.getMetaAccess(), __b.getConstantReflection(), __receiver.get(), this.___kind);
            __b.addPush(this.___kind, __valueNode);
            return true;
        }

        void register(InvocationPlugins __plugins)
        {
            String __name = this.___kind.toJavaClass().getSimpleName() + "Value";
            __plugins.register(this, this.___kind.toBoxedJavaClass(), __name, InvocationPlugin.Receiver.class);
        }
    }

    // @class StandardGraphBuilderPlugins.UnsafeGetPlugin
    public static final class UnsafeGetPlugin implements InvocationPlugin
    {
        // @field
        private final JavaKind ___returnKind;
        // @field
        private final boolean ___isVolatile;

        // @cons StandardGraphBuilderPlugins.UnsafeGetPlugin
        public UnsafeGetPlugin(JavaKind __returnKind, boolean __isVolatile)
        {
            super();
            this.___returnKind = __returnKind;
            this.___isVolatile = __isVolatile;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe, ValueNode __address)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            __b.addPush(this.___returnKind, new UnsafeMemoryLoadNode(__address, this.___returnKind, NamedLocationIdentity.OFF_HEAP_LOCATION));
            __b.getGraph().markUnsafeAccess();
            return true;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe, ValueNode __object, ValueNode __offset)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            if (this.___isVolatile)
            {
                __b.add(new MembarNode(MemoryBarriers.JMM_PRE_VOLATILE_READ));
            }
            LocationIdentity __locationIdentity = __object.isNullConstant() ? NamedLocationIdentity.OFF_HEAP_LOCATION : LocationIdentity.any();
            __b.addPush(this.___returnKind, new RawLoadNode(__object, __offset, this.___returnKind, __locationIdentity));
            if (this.___isVolatile)
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
        private final JavaKind ___kind;
        // @field
        private final boolean ___hasBarrier;
        // @field
        private final int ___preWrite;
        // @field
        private final int ___postWrite;

        // @cons StandardGraphBuilderPlugins.UnsafePutPlugin
        public UnsafePutPlugin(JavaKind __kind, boolean __isVolatile)
        {
            this(__kind, __isVolatile, MemoryBarriers.JMM_PRE_VOLATILE_WRITE, MemoryBarriers.JMM_POST_VOLATILE_WRITE);
        }

        // @cons StandardGraphBuilderPlugins.UnsafePutPlugin
        private UnsafePutPlugin(JavaKind __kind, boolean __hasBarrier, int __preWrite, int __postWrite)
        {
            super();
            this.___kind = __kind;
            this.___hasBarrier = __hasBarrier;
            this.___preWrite = __preWrite;
            this.___postWrite = __postWrite;
        }

        public static StandardGraphBuilderPlugins.UnsafePutPlugin putOrdered(JavaKind __kind)
        {
            return new StandardGraphBuilderPlugins.UnsafePutPlugin(__kind, true, MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_STORE, 0);
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe, ValueNode __address, ValueNode __value)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            __b.add(new UnsafeMemoryStoreNode(__address, __value, this.___kind, NamedLocationIdentity.OFF_HEAP_LOCATION));
            __b.getGraph().markUnsafeAccess();
            return true;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe, ValueNode __object, ValueNode __offset, ValueNode __value)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            if (this.___hasBarrier)
            {
                __b.add(new MembarNode(this.___preWrite));
            }
            LocationIdentity __locationIdentity = __object.isNullConstant() ? NamedLocationIdentity.OFF_HEAP_LOCATION : LocationIdentity.any();
            __b.add(new RawStoreNode(__object, __offset, __value, this.___kind, __locationIdentity));
            if (this.___hasBarrier)
            {
                __b.add(new MembarNode(this.___postWrite));
            }
            __b.getGraph().markUnsafeAccess();
            return true;
        }
    }

    // @class StandardGraphBuilderPlugins.UnsafeFencePlugin
    public static final class UnsafeFencePlugin implements InvocationPlugin
    {
        // @field
        private final int ___barriers;

        // @cons StandardGraphBuilderPlugins.UnsafeFencePlugin
        public UnsafeFencePlugin(int __barriers)
        {
            super();
            this.___barriers = __barriers;
        }

        @Override
        public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe)
        {
            // emits a null-check for the otherwise unused receiver
            __unsafe.get();
            __b.add(new MembarNode(this.___barriers));
            return true;
        }
    }

    // @class StandardGraphBuilderPlugins.DirectiveSpeculationReason
    private static final class DirectiveSpeculationReason implements SpeculationLog.SpeculationReason
    {
        // @field
        private final BytecodePosition ___pos;

        // @cons StandardGraphBuilderPlugins.DirectiveSpeculationReason
        private DirectiveSpeculationReason(BytecodePosition __pos)
        {
            super();
            this.___pos = __pos;
        }

        @Override
        public int hashCode()
        {
            return this.___pos.hashCode();
        }

        @Override
        public boolean equals(Object __obj)
        {
            return __obj instanceof StandardGraphBuilderPlugins.DirectiveSpeculationReason && ((StandardGraphBuilderPlugins.DirectiveSpeculationReason) __obj).___pos.equals(this.___pos);
        }
    }

    private static void registerGraalDirectivesPlugins(InvocationPlugins __plugins)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, GraalDirectives.class);
        // @closure
        __r.register0("deoptimize", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                __b.add(new DeoptimizeNode(DeoptimizationAction.None, DeoptimizationReason.TransferToInterpreter));
                return true;
            }
        });

        // @closure
        __r.register0("deoptimizeAndInvalidate", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                __b.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TransferToInterpreter));
                return true;
            }
        });

        // @closure
        __r.register0("deoptimizeAndInvalidateWithSpeculation", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                GraalError.guarantee(__b.getGraph().getSpeculationLog() != null, "A speculation log is need to use 'deoptimizeAndInvalidateWithSpeculation'");
                BytecodePosition __pos = new BytecodePosition(null, __b.getMethod(), __b.bci());
                StandardGraphBuilderPlugins.DirectiveSpeculationReason __reason = new StandardGraphBuilderPlugins.DirectiveSpeculationReason(__pos);
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
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                __b.addPush(JavaKind.Boolean, ConstantNode.forBoolean(true));
                return true;
            }
        });

        // @closure
        __r.register0("controlFlowAnchor", new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
            {
                __b.add(new ControlFlowAnchorNode());
                return true;
            }
        });

        // @closure
        InvocationPlugin blackholePlugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
            {
                __b.add(new BlackholeNode(__value));
                return true;
            }
        };

        // @closure
        InvocationPlugin bindToRegisterPlugin = new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
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
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
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
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
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
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
            {
                __b.addPush(__value.getStackKind(), __b.nullCheckedValue(__value));
                return true;
            }
        });

        // @closure
        __r.register1("ensureVirtualized", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __object)
            {
                __b.add(new EnsureVirtualizedNode(__object, false));
                return true;
            }
        });
        // @closure
        __r.register1("ensureVirtualizedHere", Object.class, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __object)
            {
                __b.add(new EnsureVirtualizedNode(__object, true));
                return true;
            }
        });
    }
}
