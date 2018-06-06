package giraaff.nodes.graphbuilderconf;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.BytecodeProvider;
import giraaff.nodes.Invoke;
import giraaff.nodes.ValueNode;

///
// Plugin for specifying what is inlined during graph parsing. This plugin is also notified
// {@link #notifyBeforeInline before} and {@link #notifyAfterInline} the inlining, as well as of
// {@link #notifyNotInlined non-inlined} invocations (i.e., those for which an {@link Invoke} node
// is created).
///
// @iface InlineInvokePlugin
public interface InlineInvokePlugin extends GraphBuilderPlugin
{
    ///
    // Result of a {@link #shouldInlineInvoke inlining decision}.
    ///
    // @class InlineInvokePlugin.InlineInvokeInfo
    static final class InlineInvokeInfo
    {
        ///
        // Denotes a call site that must not be inlined and should be implemented by a node that
        // does not speculate on the call not raising an exception.
        ///
        // @def
        public static final InlineInvokePlugin.InlineInvokeInfo DO_NOT_INLINE_WITH_EXCEPTION = new InlineInvokePlugin.InlineInvokeInfo(null, null, null);

        ///
        // Denotes a call site must not be inlined and can be implemented by a node that speculates
        // the call will not throw an exception.
        ///
        // @def
        public static final InlineInvokePlugin.InlineInvokeInfo DO_NOT_INLINE_NO_EXCEPTION = new InlineInvokePlugin.InlineInvokeInfo(null, null, null);

        ///
        // Denotes a call site must not be inlined and the execution should be transferred to
        // interpreter in case of an exception.
        ///
        // @def
        public static final InlineInvokePlugin.InlineInvokeInfo DO_NOT_INLINE_DEOPTIMIZE_ON_EXCEPTION = new InlineInvokePlugin.InlineInvokeInfo(null, null, null);

        // @field
        private final ResolvedJavaMethod ___methodToInline;
        // @field
        private final ResolvedJavaMethod ___originalMethod;
        // @field
        private final BytecodeProvider ___intrinsicBytecodeProvider;

        public static InlineInvokePlugin.InlineInvokeInfo createStandardInlineInfo(ResolvedJavaMethod __methodToInline)
        {
            return new InlineInvokePlugin.InlineInvokeInfo(__methodToInline, null, null);
        }

        public static InlineInvokePlugin.InlineInvokeInfo createIntrinsicInlineInfo(ResolvedJavaMethod __methodToInline, ResolvedJavaMethod __originalMethod, BytecodeProvider __intrinsicBytecodeProvider)
        {
            return new InlineInvokePlugin.InlineInvokeInfo(__methodToInline, __originalMethod, __intrinsicBytecodeProvider);
        }

        // @cons InlineInvokePlugin.InlineInvokeInfo
        private InlineInvokeInfo(ResolvedJavaMethod __methodToInline, ResolvedJavaMethod __originalMethod, BytecodeProvider __intrinsicBytecodeProvider)
        {
            super();
            this.___methodToInline = __methodToInline;
            this.___originalMethod = __originalMethod;
            this.___intrinsicBytecodeProvider = __intrinsicBytecodeProvider;
        }

        ///
        // Returns the method to be inlined, or {@code null} if the call site must not be inlined.
        ///
        public ResolvedJavaMethod getMethodToInline()
        {
            return this.___methodToInline;
        }

        public boolean allowsInlining()
        {
            return this.___methodToInline != null;
        }

        ///
        // Returns the original method if this is an inline of an intrinsic, or {@code null} if the
        // call site must not be inlined.
        ///
        public ResolvedJavaMethod getOriginalMethod()
        {
            return this.___originalMethod;
        }

        ///
        // Gets the provider of bytecode to be parsed for {@link #getMethodToInline()} if is is an
        // intrinsic for the original method (i.e., the {@code method} passed to
        // {@link InlineInvokePlugin#shouldInlineInvoke}). A {@code null} return value indicates
        // that this is not an intrinsic inlining.
        ///
        public BytecodeProvider getIntrinsicBytecodeProvider()
        {
            return this.___intrinsicBytecodeProvider;
        }
    }

    ///
    // Determines whether a call to a given method is to be inlined. The return value is a tri-state:
    //
    // Non-null return value with a non-null {@link InlineInvokePlugin.InlineInvokeInfo#getMethodToInline method}:
    // That {@link InlineInvokePlugin.InlineInvokeInfo#getMethodToInline method} is inlined. Note that it can be
    // a different method than the one specified here as the parameter, which allows method substitutions.
    //
    // Non-null return value with a null {@link InlineInvokePlugin.InlineInvokeInfo#getMethodToInline method},
    // e.g. {@link InlineInvokePlugin.InlineInvokeInfo#DO_NOT_INLINE_WITH_EXCEPTION}: The method is not inlined,
    // and other plugins with a lower priority cannot overwrite this decision.
    //
    // Null return value: This plugin made no decision, other plugins with a lower priority are asked.
    //
    // @param b the context
    // @param method the target method of an invoke
    // @param args the arguments to the invoke
    ///
    default InlineInvokePlugin.InlineInvokeInfo shouldInlineInvoke(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args)
    {
        return null;
    }

    ///
    // Notification that a method is about to be inlined.
    //
    // @param methodToInline the inlined method
    ///
    default void notifyBeforeInline(ResolvedJavaMethod __methodToInline)
    {
    }

    ///
    // Notification that a method was inlined.
    //
    // @param methodToInline the inlined method
    ///
    default void notifyAfterInline(ResolvedJavaMethod __methodToInline)
    {
    }

    ///
    // Notifies this plugin of the {@link Invoke} node created for a method that was not inlined per
    // {@link #shouldInlineInvoke}.
    //
    // @param b the context
    // @param method the method that was not inlined
    // @param invoke the invoke node created for the call to {@code method}
    ///
    default void notifyNotInlined(GraphBuilderContext __b, ResolvedJavaMethod __method, Invoke __invoke)
    {
    }
}
