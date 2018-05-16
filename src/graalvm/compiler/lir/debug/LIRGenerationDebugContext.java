package graalvm.compiler.lir.debug;

import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.LIR;

import jdk.vm.ci.meta.Value;

/**
 * Provides information about {@link LIR} generation for debugging purposes.
 */
public interface LIRGenerationDebugContext
{
    /**
     * Gets an object that represents the source of an {@link LIR} {@link Value operand} in a higher
     * representation.
     */
    Object getSourceForOperand(Value value);

    static LIRGenerationDebugContext getFromDebugContext(DebugContext debug)
    {
        if (debug.areScopesEnabled())
        {
            LIRGenerationDebugContext lirGen = debug.contextLookup(LIRGenerationDebugContext.class);
            assert lirGen != null;
            return lirGen;
        }
        return null;
    }

    static Object getSourceForOperandFromDebugContext(DebugContext debug, Value value)
    {
        LIRGenerationDebugContext gen = getFromDebugContext(debug);
        if (gen != null)
        {
            return gen.getSourceForOperand(value);
        }
        return null;
    }
}
