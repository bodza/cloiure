package giraaff.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;
import giraaff.word.Word;

/**
 * Substitutions for {@code com.sun.crypto.provider.CipherBlockChaining} methods.
 */
@ClassSubstitution(className = "com.sun.crypto.provider.CipherBlockChaining", optional = true)
public class CipherBlockChainingSubstitutions
{
    private static final long embeddedCipherOffset;
    private static final long rOffset;
    private static final Class<?> cipherBlockChainingClass;
    private static final Class<?> feedbackCipherClass;
    static
    {
        try
        {
            // Need to use the system class loader as com.sun.crypto.provider.FeedbackCipher is normally loaded
            // by the extension class loader, which is not delegated to by the JVMCI class loader.
            ClassLoader cl = ClassLoader.getSystemClassLoader();

            feedbackCipherClass = Class.forName("com.sun.crypto.provider.FeedbackCipher", true, cl);
            embeddedCipherOffset = UnsafeAccess.UNSAFE.objectFieldOffset(feedbackCipherClass.getDeclaredField("embeddedCipher"));

            cipherBlockChainingClass = Class.forName("com.sun.crypto.provider.CipherBlockChaining", true, cl);
            rOffset = UnsafeAccess.UNSAFE.objectFieldOffset(cipherBlockChainingClass.getDeclaredField("r"));
        }
        catch (Exception ex)
        {
            throw new GraalError(ex);
        }
    }

    // @Fold
    static Class<?> getAESCryptClass()
    {
        return AESCryptSubstitutions.AESCryptClass;
    }

    @MethodSubstitution(isStatic = false)
    static int encrypt(Object rcvr, byte[] in, int inOffset, int inLength, byte[] out, int outOffset)
    {
        Object realReceiver = PiNode.piCastNonNull(rcvr, cipherBlockChainingClass);
        Object embeddedCipher = RawLoadNode.load(realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (getAESCryptClass().isInstance(embeddedCipher))
        {
            Object aesCipher = getAESCryptClass().cast(embeddedCipher);
            crypt(realReceiver, in, inOffset, inLength, out, outOffset, aesCipher, true, false);
            return inLength;
        }
        else
        {
            return encrypt(realReceiver, in, inOffset, inLength, out, outOffset);
        }
    }

    @MethodSubstitution(isStatic = false, value = "implEncrypt")
    static int implEncrypt(Object rcvr, byte[] in, int inOffset, int inLength, byte[] out, int outOffset)
    {
        Object realReceiver = PiNode.piCastNonNull(rcvr, cipherBlockChainingClass);
        Object embeddedCipher = RawLoadNode.load(realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (getAESCryptClass().isInstance(embeddedCipher))
        {
            Object aesCipher = getAESCryptClass().cast(embeddedCipher);
            crypt(realReceiver, in, inOffset, inLength, out, outOffset, aesCipher, true, false);
            return inLength;
        }
        else
        {
            return implEncrypt(realReceiver, in, inOffset, inLength, out, outOffset);
        }
    }

    @MethodSubstitution(isStatic = false)
    static int decrypt(Object rcvr, byte[] in, int inOffset, int inLength, byte[] out, int outOffset)
    {
        Object realReceiver = PiNode.piCastNonNull(rcvr, cipherBlockChainingClass);
        Object embeddedCipher = RawLoadNode.load(realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (in != out && getAESCryptClass().isInstance(embeddedCipher))
        {
            Object aesCipher = getAESCryptClass().cast(embeddedCipher);
            crypt(realReceiver, in, inOffset, inLength, out, outOffset, aesCipher, false, false);
            return inLength;
        }
        else
        {
            return decrypt(realReceiver, in, inOffset, inLength, out, outOffset);
        }
    }

    @MethodSubstitution(isStatic = false)
    static int implDecrypt(Object rcvr, byte[] in, int inOffset, int inLength, byte[] out, int outOffset)
    {
        Object realReceiver = PiNode.piCastNonNull(rcvr, cipherBlockChainingClass);
        Object embeddedCipher = RawLoadNode.load(realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (in != out && getAESCryptClass().isInstance(embeddedCipher))
        {
            Object aesCipher = getAESCryptClass().cast(embeddedCipher);
            crypt(realReceiver, in, inOffset, inLength, out, outOffset, aesCipher, false, false);
            return inLength;
        }
        else
        {
            return implDecrypt(realReceiver, in, inOffset, inLength, out, outOffset);
        }
    }

    /**
     * Variation for platforms (e.g. SPARC) that need do key expansion in stubs due to compatibility
     * issues between Java key expansion and hardware crypto instructions.
     */
    @MethodSubstitution(isStatic = false, value = "decrypt")
    static int decryptWithOriginalKey(Object rcvr, byte[] in, int inOffset, int inLength, byte[] out, int outOffset)
    {
        Object realReceiver = PiNode.piCastNonNull(rcvr, cipherBlockChainingClass);
        Object embeddedCipher = RawLoadNode.load(realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (in != out && getAESCryptClass().isInstance(embeddedCipher))
        {
            Object aesCipher = getAESCryptClass().cast(embeddedCipher);
            crypt(realReceiver, in, inOffset, inLength, out, outOffset, aesCipher, false, true);
            return inLength;
        }
        else
        {
            return decryptWithOriginalKey(realReceiver, in, inOffset, inLength, out, outOffset);
        }
    }

    /**
     * @see #decryptWithOriginalKey(Object, byte[], int, int, byte[], int)
     */
    @MethodSubstitution(isStatic = false, value = "implDecrypt")
    static int implDecryptWithOriginalKey(Object rcvr, byte[] in, int inOffset, int inLength, byte[] out, int outOffset)
    {
        Object realReceiver = PiNode.piCastNonNull(rcvr, cipherBlockChainingClass);
        Object embeddedCipher = RawLoadNode.load(realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (in != out && getAESCryptClass().isInstance(embeddedCipher))
        {
            Object aesCipher = getAESCryptClass().cast(embeddedCipher);
            crypt(realReceiver, in, inOffset, inLength, out, outOffset, aesCipher, false, true);
            return inLength;
        }
        else
        {
            return implDecryptWithOriginalKey(realReceiver, in, inOffset, inLength, out, outOffset);
        }
    }

    private static void crypt(Object rcvr, byte[] in, int inOffset, int inLength, byte[] out, int outOffset, Object embeddedCipher, boolean encrypt, boolean withOriginalKey)
    {
        AESCryptSubstitutions.checkArgs(in, inOffset, out, outOffset);
        Object realReceiver = PiNode.piCastNonNull(rcvr, cipherBlockChainingClass);
        Object aesCipher = getAESCryptClass().cast(embeddedCipher);
        Object kObject = RawLoadNode.load(aesCipher, AESCryptSubstitutions.kOffset, JavaKind.Object, LocationIdentity.any());
        Object rObject = RawLoadNode.load(realReceiver, rOffset, JavaKind.Object, LocationIdentity.any());
        Pointer kAddr = Word.objectToTrackedPointer(kObject).add(HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Int));
        Pointer rAddr = Word.objectToTrackedPointer(rObject).add(HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Byte));
        Word inAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(in, HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Byte) + inOffset));
        Word outAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(out, HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Byte) + outOffset));
        if (encrypt)
        {
            encryptAESCryptStub(HotSpotBackend.ENCRYPT, inAddr, outAddr, kAddr, rAddr, inLength);
        }
        else
        {
            if (withOriginalKey)
            {
                Object lastKeyObject = RawLoadNode.load(aesCipher, AESCryptSubstitutions.lastKeyOffset, JavaKind.Object, LocationIdentity.any());
                Pointer lastKeyAddr = Word.objectToTrackedPointer(lastKeyObject).add(HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Byte));
                decryptAESCryptWithOriginalKeyStub(HotSpotBackend.DECRYPT_WITH_ORIGINAL_KEY, inAddr, outAddr, kAddr, rAddr, inLength, lastKeyAddr);
            }
            else
            {
                decryptAESCryptStub(HotSpotBackend.DECRYPT, inAddr, outAddr, kAddr, rAddr, inLength);
            }
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void encryptAESCryptStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word in, Word out, Pointer key, Pointer r, int inLength);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void decryptAESCryptStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word in, Word out, Pointer key, Pointer r, int inLength);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void decryptAESCryptWithOriginalKeyStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word in, Word out, Pointer key, Pointer r, int inLength, Pointer originalKey);
}
