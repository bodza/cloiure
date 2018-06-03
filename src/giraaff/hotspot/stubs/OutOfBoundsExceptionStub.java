package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.AllocaNode;
import giraaff.util.GraalError;
import giraaff.word.Word;

///
// Stub to allocate an {@link ArrayIndexOutOfBoundsException} thrown by a bytecode.
///
// @class OutOfBoundsExceptionStub
public final class OutOfBoundsExceptionStub extends CreateExceptionStub
{
    // @cons
    public OutOfBoundsExceptionStub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super("createOutOfBoundsException", __providers, __linkage);
    }

    // @def
    private static final int MAX_INT_STRING_SIZE = Integer.toString(Integer.MIN_VALUE).length();

    @Override
    protected Object getConstantParameterValue(int __index, String __name)
    {
        switch (__index)
        {
            case 1:
                return this.___providers.getRegisters().getThreadRegister();
            case 2:
            {
                int __wordSize = this.___providers.getWordTypes().getWordKind().getByteCount();
                // (MAX_INT_STRING_SIZE + 1) / wordSize, rounded up
                return MAX_INT_STRING_SIZE / __wordSize + 1;
            }
            default:
                throw GraalError.shouldNotReachHere("unknown parameter " + __name + " at index " + __index);
        }
    }

    @Snippet
    private static Object createOutOfBoundsException(int __idx, @ConstantParameter Register __threadRegister, @ConstantParameter int __bufferSizeInWords)
    {
        Word __buffer = AllocaNode.alloca(__bufferSizeInWords);

        long __number = __idx;
        if (__number < 0)
        {
            __number = -__number;
        }

        Word __ptr = __buffer.add(MAX_INT_STRING_SIZE);
        __ptr.writeByte(0, (byte) 0);
        do
        {
            long __digit = __number % 10;
            __number /= 10;

            __ptr = __ptr.subtract(1);
            __ptr.writeByte(0, (byte) ('0' + __digit));
        } while (__number > 0);

        if (__idx < 0)
        {
            __ptr = __ptr.subtract(1);
            __ptr.writeByte(0, (byte) '-');
        }

        return createException(__threadRegister, ArrayIndexOutOfBoundsException.class, __ptr);
    }
}
