package giraaff.lir.phases;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.GraalOptions;
import giraaff.lir.LIR;
import giraaff.lir.gen.LIRGenerationResult;

///
// Base class for all {@link LIR low-level} phases. Subclasses should be stateless. There will be
// one global instance for each phase that is shared for all compilations.
///
// @class LIRPhase
public abstract class LIRPhase<C>
{
    // @cons LIRPhase
    public LIRPhase()
    {
        super();
    }

    public final void apply(TargetDescription __target, LIRGenerationResult __lirGenRes, C __context)
    {
        run(__target, __lirGenRes, __context);
    }

    protected abstract void run(TargetDescription __target, LIRGenerationResult __lirGenRes, C __context);

    public static CharSequence createName(Class<?> __clazz)
    {
        String __className = __clazz.getName();
        String __s = __className.substring(__className.lastIndexOf(".") + 1); // strip the package name
        int __innerClassPos = __s.indexOf('$');
        if (__innerClassPos > 0)
        {
            // Remove inner class name.
            __s = __s.substring(0, __innerClassPos);
        }
        if (__s.endsWith("Phase"))
        {
            __s = __s.substring(0, __s.length() - "Phase".length());
        }
        return __s;
    }

    protected CharSequence createName()
    {
        return createName(getClass());
    }

    public final CharSequence getName()
    {
        return createName();
    }
}
