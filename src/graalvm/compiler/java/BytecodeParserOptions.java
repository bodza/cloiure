package graalvm.compiler.java;

import graalvm.compiler.options.OptionKey;

/**
 * Options related to {@link BytecodeParser}.
 */
public class BytecodeParserOptions
{
    // "Inlines trivial methods during bytecode parsing."
    public static final OptionKey<Boolean> InlineDuringParsing = new OptionKey<>(true);

    // "Inlines partial intrinsic exits during bytecode parsing when possible. A partial intrinsic exit is a call within an intrinsic to the method being intrinsified and denotes semantics of the original method that the intrinsic does not support."
    public static final OptionKey<Boolean> InlinePartialIntrinsicExitDuringParsing = new OptionKey<>(true);

    // "Inlines intrinsic methods during bytecode parsing."
    public static final OptionKey<Boolean> InlineIntrinsicsDuringParsing = new OptionKey<>(true);

    // "Maximum depth when inlining during bytecode parsing."
    public static final OptionKey<Integer> InlineDuringParsingMaxDepth = new OptionKey<>(10);

    // "When creating info points hide the methods of the substitutions."
    public static final OptionKey<Boolean> HideSubstitutionStates = new OptionKey<>(false);

    // "Use intrinsics guarded by a virtual dispatch test at indirect call sites."
    public static final OptionKey<Boolean> UseGuardedIntrinsics = new OptionKey<>(true);
}
