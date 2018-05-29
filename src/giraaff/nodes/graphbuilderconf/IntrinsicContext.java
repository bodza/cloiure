package giraaff.nodes.graphbuilderconf;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.BytecodeProvider;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.Invoke;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.graphbuilderconf.IntrinsicContext.CompilationContext;
import giraaff.nodes.java.ExceptionObjectNode;

/**
 * An intrinsic is a substitute implementation of a Java method (or a bytecode in the case of
 * snippets) that is itself implemented in Java. This interface provides information about the
 * intrinsic currently being processed by the graph builder.
 *
 * When in the scope of an intrinsic, the graph builder does not check the value kinds flowing
 * through the JVM state since intrinsics can employ non-Java kinds to represent values such as raw
 * machine words and pointers.
 */
// @class IntrinsicContext
public final class IntrinsicContext
{
    /**
     * Method being intrinsified.
     */
    final ResolvedJavaMethod originalMethod;

    /**
     * Method providing the intrinsic implementation.
     */
    final ResolvedJavaMethod intrinsicMethod;

    /**
     * Provider of bytecode to be parsed for a method that is part of an intrinsic.
     */
    final BytecodeProvider bytecodeProvider;

    final CompilationContext compilationContext;

    final boolean allowPartialIntrinsicArgumentMismatch;

    // @cons
    public IntrinsicContext(ResolvedJavaMethod method, ResolvedJavaMethod intrinsic, BytecodeProvider bytecodeProvider, CompilationContext compilationContext)
    {
        this(method, intrinsic, bytecodeProvider, compilationContext, false);
    }

    // @cons
    public IntrinsicContext(ResolvedJavaMethod method, ResolvedJavaMethod intrinsic, BytecodeProvider bytecodeProvider, CompilationContext compilationContext, boolean allowPartialIntrinsicArgumentMismatch)
    {
        super();
        this.originalMethod = method;
        this.intrinsicMethod = intrinsic;
        this.bytecodeProvider = bytecodeProvider;
        this.compilationContext = compilationContext;
        this.allowPartialIntrinsicArgumentMismatch = allowPartialIntrinsicArgumentMismatch;
    }

    /**
     * A partial intrinsic exits by (effectively) calling the intrinsified method. Normally, this
     * call must use exactly the same arguments as the call that is being intrinsified. This allows
     * to override this behavior.
     */
    public boolean allowPartialIntrinsicArgumentMismatch()
    {
        return allowPartialIntrinsicArgumentMismatch;
    }

    /**
     * Gets the method being intrinsified.
     */
    public ResolvedJavaMethod getOriginalMethod()
    {
        return originalMethod;
    }

    /**
     * Gets the method providing the intrinsic implementation.
     */
    public ResolvedJavaMethod getIntrinsicMethod()
    {
        return intrinsicMethod;
    }

    /**
     * Gets provider of bytecode to be parsed for a method that is part of an intrinsic.
     */
    public BytecodeProvider getBytecodeProvider()
    {
        return bytecodeProvider;
    }

    /**
     * Determines if a call within the compilation scope of this intrinsic represents a call to the
     * {@linkplain #getOriginalMethod() original} method. This denotes the path where a partial
     * intrinsification falls back to the original method.
     */
    public boolean isCallToOriginal(ResolvedJavaMethod targetMethod)
    {
        return originalMethod.equals(targetMethod) || intrinsicMethod.equals(targetMethod);
    }

    public boolean isPostParseInlined()
    {
        return compilationContext.equals(CompilationContext.INLINE_AFTER_PARSING);
    }

    public boolean isCompilationRoot()
    {
        return compilationContext.equals(CompilationContext.ROOT_COMPILATION);
    }

    /**
     * Denotes the compilation context in which an intrinsic is being parsed.
     */
    // @enum IntrinsicContext.CompilationContext
    public enum CompilationContext
    {
        /**
         * An intrinsic is being processed when parsing an invoke bytecode that calls the
         * intrinsified method.
         */
        INLINE_DURING_PARSING,

        /**
         * An intrinsic is being processed when inlining an {@link Invoke} in an existing graph.
         */
        INLINE_AFTER_PARSING,

        /**
         * An intrinsic is the root of compilation.
         */
        ROOT_COMPILATION
    }

    /**
     * Models the state of a graph in terms of {@link StateSplit#hasSideEffect() side effects} that
     * are control flow predecessors of the current point in a graph.
     */
    // @iface IntrinsicContext.SideEffectsState
    public interface SideEffectsState
    {
        /**
         * Determines if the current program point is preceded by one or more side effects.
         */
        boolean isAfterSideEffect();

        /**
         * Gets the side effects preceding the current program point.
         */
        Iterable<StateSplit> sideEffects();

        /**
         * Records a side effect for the current program point.
         */
        void addSideEffect(StateSplit sideEffect);
    }

    public FrameState createFrameState(StructuredGraph graph, SideEffectsState sideEffects, StateSplit forStateSplit)
    {
        if (forStateSplit.hasSideEffect())
        {
            if (sideEffects.isAfterSideEffect())
            {
                // Only the last side effect on any execution path in a replacement
                // can inherit the stateAfter of the replaced node.
                FrameState invalid = graph.add(new FrameState(BytecodeFrame.INVALID_FRAMESTATE_BCI));
                for (StateSplit lastSideEffect : sideEffects.sideEffects())
                {
                    lastSideEffect.setStateAfter(invalid);
                }
            }
            sideEffects.addSideEffect(forStateSplit);
            FrameState frameState;
            if (forStateSplit instanceof ExceptionObjectNode)
            {
                frameState = graph.add(new FrameState(BytecodeFrame.AFTER_EXCEPTION_BCI, (ExceptionObjectNode) forStateSplit));
            }
            else
            {
                frameState = graph.add(new FrameState(BytecodeFrame.AFTER_BCI));
            }
            return frameState;
        }
        else
        {
            if (forStateSplit instanceof AbstractMergeNode)
            {
                // merge nodes always need a frame state
                if (sideEffects.isAfterSideEffect())
                {
                    // a merge after one or more side effects
                    return graph.add(new FrameState(BytecodeFrame.AFTER_BCI));
                }
                else
                {
                    // a merge before any side effects
                    return graph.add(new FrameState(BytecodeFrame.BEFORE_BCI));
                }
            }
            else
            {
                // other non-side-effects do not need a state
                return null;
            }
        }
    }

    @Override
    public String toString()
    {
        return "Intrinsic{original: " + originalMethod.format("%H.%n(%p)") + ", intrinsic: " + intrinsicMethod.format("%H.%n(%p)") + ", context: " + compilationContext + "}";
    }
}
