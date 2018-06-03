package giraaff.hotspot.replacements;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.Constant;

import giraaff.core.common.type.DataPointerConstant;

///
// Represents an encoded representation of a constant.
///
// @class EncodedSymbolConstant
public final class EncodedSymbolConstant extends DataPointerConstant
{
    // @field
    private final Constant ___constant;
    // @field
    private byte[] ___bytes;

    // @cons
    public EncodedSymbolConstant(Constant __constant)
    {
        super(1);
        this.___constant = __constant;
    }

    @Override
    public int getSerializedSize()
    {
        return getEncodedConstant().length;
    }

    @Override
    public void serialize(ByteBuffer __buffer)
    {
        __buffer.put(getEncodedConstant());
    }

    ///
    // Converts a string to a byte array with modified UTF-8 encoding. The first two bytes of the
    // byte array store the length of the string in bytes.
    //
    // @param s a java.lang.String in UTF-16
    ///
    private static byte[] toUTF8String(String __s)
    {
        try (ByteArrayOutputStream __bytes = new ByteArrayOutputStream())
        {
            DataOutputStream __stream = new DataOutputStream(__bytes);
            __stream.writeUTF(__s);
            return __bytes.toByteArray();
        }
        catch (Exception __e)
        {
            throw new BailoutException(__e, "UTF-8 encoding failed: %s", __s);
        }
    }

    private static byte[] encodeConstant(Constant __constant)
    {
        if (__constant instanceof HotSpotObjectConstant)
        {
            return toUTF8String(((HotSpotObjectConstant) __constant).asObject(String.class));
        }
        else if (__constant instanceof HotSpotMetaspaceConstant)
        {
            HotSpotMetaspaceConstant __metaspaceConstant = ((HotSpotMetaspaceConstant) __constant);
            HotSpotResolvedObjectType __klass = __metaspaceConstant.asResolvedJavaType();
            if (__klass != null)
            {
                return toUTF8String(__klass.getName());
            }
            HotSpotResolvedJavaMethod __method = __metaspaceConstant.asResolvedJavaMethod();
            if (__method != null)
            {
                byte[] __methodName = toUTF8String(__method.getName());
                byte[] __signature = toUTF8String(__method.getSignature().toMethodDescriptor());
                byte[] __result = new byte[__methodName.length + __signature.length];
                int __resultPos = 0;
                System.arraycopy(__methodName, 0, __result, __resultPos, __methodName.length);
                __resultPos += __methodName.length;
                System.arraycopy(__signature, 0, __result, __resultPos, __signature.length);
                __resultPos += __signature.length;
                return __result;
            }
        }
        throw new BailoutException("encoding of constant %s failed", __constant);
    }

    public byte[] getEncodedConstant()
    {
        if (this.___bytes == null)
        {
            this.___bytes = encodeConstant(this.___constant);
        }
        return this.___bytes;
    }

    @Override
    public String toValueString()
    {
        return "encoded symbol\"" + this.___constant.toValueString() + "\"";
    }
}
