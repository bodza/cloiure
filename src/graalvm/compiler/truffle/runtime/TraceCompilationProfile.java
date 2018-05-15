package graalvm.compiler.truffle.runtime;

import org.graalvm.options.OptionValues;

public class TraceCompilationProfile extends OptimizedCompilationProfile {

    public TraceCompilationProfile(OptionValues options) {
        super(options);
    }

    private int directCallCount;
    private int indirectCallCount;
    private int inlinedCallCount;

    @Override
    public void profileDirectCall(Object[] args) {
        directCallCount++;
        super.profileDirectCall(args);
    }

    @Override
    public void profileIndirectCall() {
        indirectCallCount++;
        super.profileIndirectCall();
    }

    @Override
    public void profileInlinedCall() {
        super.profileInlinedCall();
        inlinedCallCount++;
    }

    public int getIndirectCallCount() {
        return indirectCallCount;
    }

    public int getDirectCallCount() {
        return directCallCount;
    }

    public int getInlinedCallCount() {
        return inlinedCallCount;
    }

    public int getTotalCallCount() {
        return directCallCount + indirectCallCount + inlinedCallCount;
    }

    /* Lazy class loading factory method. */
    public static OptimizedCompilationProfile create(OptionValues optionValues) {
        return new TraceCompilationProfile(optionValues);
    }

}
