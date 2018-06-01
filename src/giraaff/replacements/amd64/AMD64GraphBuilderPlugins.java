package giraaff.replacements.amd64;

import java.util.Arrays;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.word.LocationIdentity;

import giraaff.bytecode.BytecodeProvider;
import giraaff.lir.amd64.AMD64ArithmeticLIRGeneratorTool.RoundingMode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
import giraaff.nodes.graphbuilderconf.InvocationPlugins.Registration;
import giraaff.nodes.java.AtomicReadAndAddNode;
import giraaff.nodes.java.AtomicReadAndWriteNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.replacements.ArraysSubstitutions;
import giraaff.replacements.IntegerSubstitutions;
import giraaff.replacements.LongSubstitutions;
import giraaff.replacements.StandardGraphBuilderPlugins.UnsafeGetPlugin;
import giraaff.replacements.StandardGraphBuilderPlugins.UnsafePutPlugin;
import giraaff.replacements.nodes.BitCountNode;

// @class AMD64GraphBuilderPlugins
public final class AMD64GraphBuilderPlugins
{
    public static void register(Plugins plugins, BytecodeProvider replacementsBytecodeProvider, AMD64 arch)
    {
        InvocationPlugins invocationPlugins = plugins.getInvocationPlugins();
        invocationPlugins.defer(new Runnable()
        {
            @Override
            public void run()
            {
                registerIntegerLongPlugins(invocationPlugins, IntegerSubstitutions.class, JavaKind.Int, arch, replacementsBytecodeProvider);
                registerIntegerLongPlugins(invocationPlugins, LongSubstitutions.class, JavaKind.Long, arch, replacementsBytecodeProvider);
                registerUnsafePlugins(invocationPlugins, replacementsBytecodeProvider);
                registerStringPlugins(invocationPlugins, arch, replacementsBytecodeProvider);
                registerStringLatin1Plugins(invocationPlugins, replacementsBytecodeProvider);
                registerStringUTF16Plugins(invocationPlugins, replacementsBytecodeProvider);
                registerMathPlugins(invocationPlugins, arch, replacementsBytecodeProvider);
                registerArraysEqualsPlugins(invocationPlugins, replacementsBytecodeProvider);
            }
        });
    }

    private static void registerIntegerLongPlugins(InvocationPlugins plugins, Class<?> substituteDeclaringClass, JavaKind kind, AMD64 arch, BytecodeProvider bytecodeProvider)
    {
        Class<?> declaringClass = kind.toBoxedJavaClass();
        Class<?> type = kind.toJavaClass();
        Registration r = new Registration(plugins, declaringClass, bytecodeProvider);
        if (arch.getFeatures().contains(AMD64.CPUFeature.LZCNT) && arch.getFlags().contains(AMD64.Flag.UseCountLeadingZerosInstruction))
        {
            // @closure
            r.register1("numberOfLeadingZeros", type, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
                {
                    ValueNode folded = AMD64CountLeadingZerosNode.tryFold(value);
                    if (folded != null)
                    {
                        b.addPush(JavaKind.Int, folded);
                    }
                    else
                    {
                        b.addPush(JavaKind.Int, new AMD64CountLeadingZerosNode(value));
                    }
                    return true;
                }
            });
        }
        else
        {
            r.registerMethodSubstitution(substituteDeclaringClass, "numberOfLeadingZeros", type);
        }
        if (arch.getFeatures().contains(AMD64.CPUFeature.BMI1) && arch.getFlags().contains(AMD64.Flag.UseCountTrailingZerosInstruction))
        {
            // @closure
            r.register1("numberOfTrailingZeros", type, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
                {
                    ValueNode folded = AMD64CountTrailingZerosNode.tryFold(value);
                    if (folded != null)
                    {
                        b.addPush(JavaKind.Int, folded);
                    }
                    else
                    {
                        b.addPush(JavaKind.Int, new AMD64CountTrailingZerosNode(value));
                    }
                    return true;
                }
            });
        }
        else
        {
            r.registerMethodSubstitution(substituteDeclaringClass, "numberOfTrailingZeros", type);
        }

        if (arch.getFeatures().contains(AMD64.CPUFeature.POPCNT))
        {
            // @closure
            r.register1("bitCount", type, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value)
                {
                    b.push(JavaKind.Int, b.append(new BitCountNode(value).canonical(null)));
                    return true;
                }
            });
        }
    }

    private static void registerMathPlugins(InvocationPlugins plugins, AMD64 arch, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, Math.class, bytecodeProvider);

        if (arch.getFeatures().contains(CPUFeature.SSE4_1))
        {
            registerRound(r, "rint", RoundingMode.NEAREST);
            registerRound(r, "ceil", RoundingMode.UP);
            registerRound(r, "floor", RoundingMode.DOWN);
        }
    }

    private static void registerRound(Registration r, String name, RoundingMode mode)
    {
        // @closure
        r.register1(name, Double.TYPE, new InvocationPlugin()
        {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode arg)
            {
                b.push(JavaKind.Double, b.append(new AMD64RoundNode(arg, mode)));
                return true;
            }
        });
    }

    private static void registerStringPlugins(InvocationPlugins plugins, AMD64 arch, BytecodeProvider replacementsBytecodeProvider)
    {
    }

    private static void registerStringLatin1Plugins(InvocationPlugins plugins, BytecodeProvider replacementsBytecodeProvider)
    {
        Registration r = new Registration(plugins, "java.lang.StringLatin1", replacementsBytecodeProvider);
        r.setAllowOverwrite(true);
        r.registerMethodSubstitution(AMD64StringLatin1Substitutions.class, "compareTo", byte[].class, byte[].class);
        r.registerMethodSubstitution(AMD64StringLatin1Substitutions.class, "compareToUTF16", byte[].class, byte[].class);
    }

    private static void registerStringUTF16Plugins(InvocationPlugins plugins, BytecodeProvider replacementsBytecodeProvider)
    {
        Registration r = new Registration(plugins, "java.lang.StringUTF16", replacementsBytecodeProvider);
        r.setAllowOverwrite(true);
        r.registerMethodSubstitution(AMD64StringUTF16Substitutions.class, "compareTo", byte[].class, byte[].class);
        r.registerMethodSubstitution(AMD64StringUTF16Substitutions.class, "compareToLatin1", byte[].class, byte[].class);
    }

    private static void registerUnsafePlugins(InvocationPlugins plugins, BytecodeProvider replacementsBytecodeProvider)
    {
        Registration r = new Registration(plugins, "jdk.internal.misc.Unsafe", replacementsBytecodeProvider);
        for (JavaKind kind : new JavaKind[] { JavaKind.Int, JavaKind.Long, JavaKind.Object })
        {
            Class<?> javaClass = kind == JavaKind.Object ? Object.class : kind.toJavaClass();

            // @closure
            r.register4("getAndSet" + kind.name(), Receiver.class, Object.class, long.class, javaClass, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe, ValueNode object, ValueNode offset, ValueNode value)
                {
                    // emits a null-check for the otherwise unused receiver
                    unsafe.get();
                    b.addPush(kind, new AtomicReadAndWriteNode(object, offset, value, kind, LocationIdentity.any()));
                    b.getGraph().markUnsafeAccess();
                    return true;
                }
            });
            if (kind != JavaKind.Object)
            {
                // @closure
                r.register4("getAndAdd" + kind.name(), Receiver.class, Object.class, long.class, javaClass, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver unsafe, ValueNode object, ValueNode offset, ValueNode delta)
                    {
                        // emits a null-check for the otherwise unused receiver
                        unsafe.get();
                        AddressNode address = b.add(new OffsetAddressNode(object, offset));
                        b.addPush(kind, new AtomicReadAndAddNode(address, delta, LocationIdentity.any()));
                        b.getGraph().markUnsafeAccess();
                        return true;
                    }
                });
            }
        }

        for (JavaKind kind : new JavaKind[] { JavaKind.Char, JavaKind.Short, JavaKind.Int, JavaKind.Long })
        {
            Class<?> javaClass = kind.toJavaClass();
            r.registerOptional3("get" + kind.name() + "Unaligned", Receiver.class, Object.class, long.class, new UnsafeGetPlugin(kind, false));
            r.registerOptional4("put" + kind.name() + "Unaligned", Receiver.class, Object.class, long.class, javaClass, new UnsafePutPlugin(kind, false));
        }
    }

    private static void registerArraysEqualsPlugins(InvocationPlugins plugins, BytecodeProvider bytecodeProvider)
    {
        Registration r = new Registration(plugins, Arrays.class, bytecodeProvider);
        r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", float[].class, float[].class);
        r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", double[].class, double[].class);
    }
}
