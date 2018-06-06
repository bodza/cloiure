package giraaff.replacements.amd64;

import java.util.Arrays;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.word.LocationIdentity;

import giraaff.bytecode.BytecodeProvider;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
import giraaff.nodes.java.AtomicReadAndAddNode;
import giraaff.nodes.java.AtomicReadAndWriteNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.replacements.ArraysSubstitutions;
import giraaff.replacements.IntegerSubstitutions;
import giraaff.replacements.LongSubstitutions;
import giraaff.replacements.StandardGraphBuilderPlugins;
import giraaff.replacements.nodes.BitCountNode;

// @class AMD64GraphBuilderPlugins
public final class AMD64GraphBuilderPlugins
{
    public static void register(GraphBuilderConfiguration.Plugins __plugins, BytecodeProvider __replacementsBytecodeProvider, AMD64 __arch)
    {
        InvocationPlugins __invocationPlugins = __plugins.getInvocationPlugins();
        __invocationPlugins.defer(new Runnable()
        {
            @Override
            public void run()
            {
                registerIntegerLongPlugins(__invocationPlugins, IntegerSubstitutions.class, JavaKind.Int, __arch, __replacementsBytecodeProvider);
                registerIntegerLongPlugins(__invocationPlugins, LongSubstitutions.class, JavaKind.Long, __arch, __replacementsBytecodeProvider);
                registerUnsafePlugins(__invocationPlugins, __replacementsBytecodeProvider);
                registerStringLatin1Plugins(__invocationPlugins, __replacementsBytecodeProvider);
                registerStringUTF16Plugins(__invocationPlugins, __replacementsBytecodeProvider);
                registerArraysEqualsPlugins(__invocationPlugins, __replacementsBytecodeProvider);
            }
        });
    }

    private static void registerIntegerLongPlugins(InvocationPlugins __plugins, Class<?> __substituteDeclaringClass, JavaKind __kind, AMD64 __arch, BytecodeProvider __bytecodeProvider)
    {
        Class<?> __declaringClass = __kind.toBoxedJavaClass();
        Class<?> __type = __kind.toJavaClass();
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, __declaringClass, __bytecodeProvider);
        if (__arch.getFeatures().contains(AMD64.CPUFeature.LZCNT) && __arch.getFlags().contains(AMD64.Flag.UseCountLeadingZerosInstruction))
        {
            // @closure
            __r.register1("numberOfLeadingZeros", __type, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
                {
                    ValueNode __folded = AMD64CountLeadingZerosNode.tryFold(__value);
                    if (__folded != null)
                    {
                        __b.addPush(JavaKind.Int, __folded);
                    }
                    else
                    {
                        __b.addPush(JavaKind.Int, new AMD64CountLeadingZerosNode(__value));
                    }
                    return true;
                }
            });
        }
        else
        {
            __r.registerMethodSubstitution(__substituteDeclaringClass, "numberOfLeadingZeros", __type);
        }
        if (__arch.getFeatures().contains(AMD64.CPUFeature.BMI1) && __arch.getFlags().contains(AMD64.Flag.UseCountTrailingZerosInstruction))
        {
            // @closure
            __r.register1("numberOfTrailingZeros", __type, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
                {
                    ValueNode __folded = AMD64CountTrailingZerosNode.tryFold(__value);
                    if (__folded != null)
                    {
                        __b.addPush(JavaKind.Int, __folded);
                    }
                    else
                    {
                        __b.addPush(JavaKind.Int, new AMD64CountTrailingZerosNode(__value));
                    }
                    return true;
                }
            });
        }
        else
        {
            __r.registerMethodSubstitution(__substituteDeclaringClass, "numberOfTrailingZeros", __type);
        }

        if (__arch.getFeatures().contains(AMD64.CPUFeature.POPCNT))
        {
            // @closure
            __r.register1("bitCount", __type, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __value)
                {
                    __b.push(JavaKind.Int, __b.append(new BitCountNode(__value).canonical(null)));
                    return true;
                }
            });
        }
    }

    private static void registerStringLatin1Plugins(InvocationPlugins __plugins, BytecodeProvider __replacementsBytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, "java.lang.StringLatin1", __replacementsBytecodeProvider);
        __r.setAllowOverwrite(true);
        __r.registerMethodSubstitution(AMD64StringLatin1Substitutions.class, "compareTo", byte[].class, byte[].class);
        __r.registerMethodSubstitution(AMD64StringLatin1Substitutions.class, "compareToUTF16", byte[].class, byte[].class);
    }

    private static void registerStringUTF16Plugins(InvocationPlugins __plugins, BytecodeProvider __replacementsBytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, "java.lang.StringUTF16", __replacementsBytecodeProvider);
        __r.setAllowOverwrite(true);
        __r.registerMethodSubstitution(AMD64StringUTF16Substitutions.class, "compareTo", byte[].class, byte[].class);
        __r.registerMethodSubstitution(AMD64StringUTF16Substitutions.class, "compareToLatin1", byte[].class, byte[].class);
    }

    private static void registerUnsafePlugins(InvocationPlugins __plugins, BytecodeProvider __replacementsBytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, "jdk.internal.misc.Unsafe", __replacementsBytecodeProvider);
        for (JavaKind __kind : new JavaKind[] { JavaKind.Int, JavaKind.Long, JavaKind.Object })
        {
            Class<?> __javaClass = __kind == JavaKind.Object ? Object.class : __kind.toJavaClass();

            // @closure
            __r.register4("getAndSet" + __kind.name(), InvocationPlugin.Receiver.class, Object.class, long.class, __javaClass, new InvocationPlugin()
            {
                @Override
                public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe, ValueNode __object, ValueNode __offset, ValueNode __value)
                {
                    // emits a null-check for the otherwise unused receiver
                    __unsafe.get();
                    __b.addPush(__kind, new AtomicReadAndWriteNode(__object, __offset, __value, __kind, LocationIdentity.any()));
                    __b.getGraph().markUnsafeAccess();
                    return true;
                }
            });
            if (__kind != JavaKind.Object)
            {
                // @closure
                __r.register4("getAndAdd" + __kind.name(), InvocationPlugin.Receiver.class, Object.class, long.class, __javaClass, new InvocationPlugin()
                {
                    @Override
                    public boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __unsafe, ValueNode __object, ValueNode __offset, ValueNode __delta)
                    {
                        // emits a null-check for the otherwise unused receiver
                        __unsafe.get();
                        AddressNode __address = __b.add(new OffsetAddressNode(__object, __offset));
                        __b.addPush(__kind, new AtomicReadAndAddNode(__address, __delta, LocationIdentity.any()));
                        __b.getGraph().markUnsafeAccess();
                        return true;
                    }
                });
            }
        }

        for (JavaKind __kind : new JavaKind[] { JavaKind.Char, JavaKind.Short, JavaKind.Int, JavaKind.Long })
        {
            Class<?> __javaClass = __kind.toJavaClass();
            __r.registerOptional3("get" + __kind.name() + "Unaligned", InvocationPlugin.Receiver.class, Object.class, long.class, new StandardGraphBuilderPlugins.UnsafeGetPlugin(__kind, false));
            __r.registerOptional4("put" + __kind.name() + "Unaligned", InvocationPlugin.Receiver.class, Object.class, long.class, __javaClass, new StandardGraphBuilderPlugins.UnsafePutPlugin(__kind, false));
        }
    }

    private static void registerArraysEqualsPlugins(InvocationPlugins __plugins, BytecodeProvider __bytecodeProvider)
    {
        InvocationPlugins.Registration __r = new InvocationPlugins.Registration(__plugins, Arrays.class, __bytecodeProvider);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", float[].class, float[].class);
        __r.registerMethodSubstitution(ArraysSubstitutions.class, "equals", double[].class, double[].class);
    }
}
