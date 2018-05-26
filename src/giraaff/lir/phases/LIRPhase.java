package giraaff.lir.phases;

import jdk.vm.ci.code.TargetDescription;

import giraaff.lir.LIR;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.options.OptionKey;

/**
 * Base class for all {@link LIR low-level} phases. Subclasses should be stateless. There will be
 * one global instance for each phase that is shared for all compilations.
 */
public abstract class LIRPhase<C>
{
    public static class Options
    {
        // @Option "Enable LIR level optimiztations."
        public static final OptionKey<Boolean> LIROptimization = new OptionKey<>(true);
    }

    public LIRPhase()
    {
    }

    public final void apply(TargetDescription target, LIRGenerationResult lirGenRes, C context)
    {
        run(target, lirGenRes, context);
    }

    protected abstract void run(TargetDescription target, LIRGenerationResult lirGenRes, C context);

    public static CharSequence createName(Class<?> clazz)
    {
        String className = clazz.getName();
        String s = className.substring(className.lastIndexOf(".") + 1); // strip the package name
        int innerClassPos = s.indexOf('$');
        if (innerClassPos > 0)
        {
            // Remove inner class name.
            s = s.substring(0, innerClassPos);
        }
        if (s.endsWith("Phase"))
        {
            s = s.substring(0, s.length() - "Phase".length());
        }
        return s;
    }

    protected CharSequence createName()
    {
        return createName(getClass());
    }

    public final CharSequence getName()
    {
        CharSequence name = createName();
        return name;
    }
}
