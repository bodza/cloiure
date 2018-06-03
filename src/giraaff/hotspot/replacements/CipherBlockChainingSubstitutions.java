package giraaff.hotspot.replacements;

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
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;
import giraaff.word.Word;

///
// Substitutions for {@code com.sun.crypto.provider.CipherBlockChaining} methods.
///
@ClassSubstitution(className = "com.sun.crypto.provider.CipherBlockChaining", optional = true)
// @class CipherBlockChainingSubstitutions
public final class CipherBlockChainingSubstitutions
{
    // @cons
    private CipherBlockChainingSubstitutions()
    {
        super();
    }

    // @def
    private static final long embeddedCipherOffset;
    // @def
    private static final long rOffset;
    // @def
    private static final Class<?> cipherBlockChainingClass;
    // @def
    private static final Class<?> feedbackCipherClass;
    static
    {
        try
        {
            // Need to use the system class loader as com.sun.crypto.provider.FeedbackCipher is normally loaded
            // by the extension class loader, which is not delegated to by the JVMCI class loader.
            ClassLoader __cl = ClassLoader.getSystemClassLoader();

            feedbackCipherClass = Class.forName("com.sun.crypto.provider.FeedbackCipher", true, __cl);
            embeddedCipherOffset = UnsafeAccess.UNSAFE.objectFieldOffset(feedbackCipherClass.getDeclaredField("embeddedCipher"));

            cipherBlockChainingClass = Class.forName("com.sun.crypto.provider.CipherBlockChaining", true, __cl);
            rOffset = UnsafeAccess.UNSAFE.objectFieldOffset(cipherBlockChainingClass.getDeclaredField("r"));
        }
        catch (Exception __ex)
        {
            throw new GraalError(__ex);
        }
    }

    // @Fold
    static Class<?> getAESCryptClass()
    {
        return AESCryptSubstitutions.AESCryptClass;
    }

    @MethodSubstitution(isStatic = false)
    static int encrypt(Object __rcvr, byte[] __in, int __inOffset, int __inLength, byte[] __out, int __outOffset)
    {
        Object __realReceiver = PiNode.piCastNonNull(__rcvr, cipherBlockChainingClass);
        Object __embeddedCipher = RawLoadNode.load(__realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (getAESCryptClass().isInstance(__embeddedCipher))
        {
            Object __aesCipher = getAESCryptClass().cast(__embeddedCipher);
            crypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset, __aesCipher, true, false);
            return __inLength;
        }
        else
        {
            return encrypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset);
        }
    }

    @MethodSubstitution(isStatic = false, value = "implEncrypt")
    static int implEncrypt(Object __rcvr, byte[] __in, int __inOffset, int __inLength, byte[] __out, int __outOffset)
    {
        Object __realReceiver = PiNode.piCastNonNull(__rcvr, cipherBlockChainingClass);
        Object __embeddedCipher = RawLoadNode.load(__realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (getAESCryptClass().isInstance(__embeddedCipher))
        {
            Object __aesCipher = getAESCryptClass().cast(__embeddedCipher);
            crypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset, __aesCipher, true, false);
            return __inLength;
        }
        else
        {
            return implEncrypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset);
        }
    }

    @MethodSubstitution(isStatic = false)
    static int decrypt(Object __rcvr, byte[] __in, int __inOffset, int __inLength, byte[] __out, int __outOffset)
    {
        Object __realReceiver = PiNode.piCastNonNull(__rcvr, cipherBlockChainingClass);
        Object __embeddedCipher = RawLoadNode.load(__realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (__in != __out && getAESCryptClass().isInstance(__embeddedCipher))
        {
            Object __aesCipher = getAESCryptClass().cast(__embeddedCipher);
            crypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset, __aesCipher, false, false);
            return __inLength;
        }
        else
        {
            return decrypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset);
        }
    }

    @MethodSubstitution(isStatic = false)
    static int implDecrypt(Object __rcvr, byte[] __in, int __inOffset, int __inLength, byte[] __out, int __outOffset)
    {
        Object __realReceiver = PiNode.piCastNonNull(__rcvr, cipherBlockChainingClass);
        Object __embeddedCipher = RawLoadNode.load(__realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (__in != __out && getAESCryptClass().isInstance(__embeddedCipher))
        {
            Object __aesCipher = getAESCryptClass().cast(__embeddedCipher);
            crypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset, __aesCipher, false, false);
            return __inLength;
        }
        else
        {
            return implDecrypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset);
        }
    }

    ///
    // Variation for platforms (e.g. SPARC) that need do key expansion in stubs due to compatibility
    // issues between Java key expansion and hardware crypto instructions.
    ///
    @MethodSubstitution(isStatic = false, value = "decrypt")
    static int decryptWithOriginalKey(Object __rcvr, byte[] __in, int __inOffset, int __inLength, byte[] __out, int __outOffset)
    {
        Object __realReceiver = PiNode.piCastNonNull(__rcvr, cipherBlockChainingClass);
        Object __embeddedCipher = RawLoadNode.load(__realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (__in != __out && getAESCryptClass().isInstance(__embeddedCipher))
        {
            Object __aesCipher = getAESCryptClass().cast(__embeddedCipher);
            crypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset, __aesCipher, false, true);
            return __inLength;
        }
        else
        {
            return decryptWithOriginalKey(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset);
        }
    }

    ///
    // @see #decryptWithOriginalKey(Object, byte[], int, int, byte[], int)
    ///
    @MethodSubstitution(isStatic = false, value = "implDecrypt")
    static int implDecryptWithOriginalKey(Object __rcvr, byte[] __in, int __inOffset, int __inLength, byte[] __out, int __outOffset)
    {
        Object __realReceiver = PiNode.piCastNonNull(__rcvr, cipherBlockChainingClass);
        Object __embeddedCipher = RawLoadNode.load(__realReceiver, embeddedCipherOffset, JavaKind.Object, LocationIdentity.any());
        if (__in != __out && getAESCryptClass().isInstance(__embeddedCipher))
        {
            Object __aesCipher = getAESCryptClass().cast(__embeddedCipher);
            crypt(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset, __aesCipher, false, true);
            return __inLength;
        }
        else
        {
            return implDecryptWithOriginalKey(__realReceiver, __in, __inOffset, __inLength, __out, __outOffset);
        }
    }

    private static void crypt(Object __rcvr, byte[] __in, int __inOffset, int __inLength, byte[] __out, int __outOffset, Object __embeddedCipher, boolean __encrypt, boolean __withOriginalKey)
    {
        AESCryptSubstitutions.checkArgs(__in, __inOffset, __out, __outOffset);
        Object __realReceiver = PiNode.piCastNonNull(__rcvr, cipherBlockChainingClass);
        Object __aesCipher = getAESCryptClass().cast(__embeddedCipher);
        Object __kObject = RawLoadNode.load(__aesCipher, AESCryptSubstitutions.kOffset, JavaKind.Object, LocationIdentity.any());
        Object __rObject = RawLoadNode.load(__realReceiver, rOffset, JavaKind.Object, LocationIdentity.any());
        Pointer __kAddr = Word.objectToTrackedPointer(__kObject).add(HotSpotRuntime.getArrayBaseOffset(JavaKind.Int));
        Pointer __rAddr = Word.objectToTrackedPointer(__rObject).add(HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte));
        Word __inAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__in, HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte) + __inOffset));
        Word __outAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__out, HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte) + __outOffset));
        if (__encrypt)
        {
            encryptAESCryptStub(HotSpotBackend.ENCRYPT, __inAddr, __outAddr, __kAddr, __rAddr, __inLength);
        }
        else
        {
            if (__withOriginalKey)
            {
                Object __lastKeyObject = RawLoadNode.load(__aesCipher, AESCryptSubstitutions.lastKeyOffset, JavaKind.Object, LocationIdentity.any());
                Pointer __lastKeyAddr = Word.objectToTrackedPointer(__lastKeyObject).add(HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte));
                decryptAESCryptWithOriginalKeyStub(HotSpotBackend.DECRYPT_WITH_ORIGINAL_KEY, __inAddr, __outAddr, __kAddr, __rAddr, __inLength, __lastKeyAddr);
            }
            else
            {
                decryptAESCryptStub(HotSpotBackend.DECRYPT, __inAddr, __outAddr, __kAddr, __rAddr, __inLength);
            }
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void encryptAESCryptStub(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Word __in, Word __out, Pointer __key, Pointer __r, int __inLength);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void decryptAESCryptStub(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Word __in, Word __out, Pointer __key, Pointer __r, int __inLength);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void decryptAESCryptWithOriginalKeyStub(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Word __in, Word __out, Pointer __key, Pointer __r, int __inLength, Pointer __originalKey);
}
