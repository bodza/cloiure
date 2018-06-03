package giraaff.hotspot.replacements;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
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
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;
import giraaff.word.Word;

///
// Substitutions for {@code com.sun.crypto.provider.AESCrypt} methods.
///
@ClassSubstitution(className = "com.sun.crypto.provider.AESCrypt", optional = true)
// @class AESCryptSubstitutions
public final class AESCryptSubstitutions
{
    // @cons
    private AESCryptSubstitutions()
    {
        super();
    }

    // @def
    static final long kOffset;
    // @def
    static final long lastKeyOffset;
    // @def
    static final Class<?> AESCryptClass;

    ///
    // The AES block size is a constant 128 bits as defined by the
    // <a href="http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.197.pdf">standard<a/>.
    ///
    // @def
    static final int AES_BLOCK_SIZE_IN_BYTES = 16;

    static
    {
        try
        {
            // Need to use the system class loader as com.sun.crypto.provider.AESCrypt is normally loaded
            // by the extension class loader, which is not delegated to by the JVMCI class loader.
            ClassLoader __cl = ClassLoader.getSystemClassLoader();
            AESCryptClass = Class.forName("com.sun.crypto.provider.AESCrypt", true, __cl);
            kOffset = UnsafeAccess.UNSAFE.objectFieldOffset(AESCryptClass.getDeclaredField("K"));
            lastKeyOffset = UnsafeAccess.UNSAFE.objectFieldOffset(AESCryptClass.getDeclaredField("lastKey"));
        }
        catch (Exception __ex)
        {
            throw new GraalError(__ex);
        }
    }

    @MethodSubstitution(isStatic = false)
    static void encryptBlock(Object __rcvr, byte[] __in, int __inOffset, byte[] __out, int __outOffset)
    {
        crypt(__rcvr, __in, __inOffset, __out, __outOffset, true, false);
    }

    @MethodSubstitution(isStatic = false)
    static void implEncryptBlock(Object __rcvr, byte[] __in, int __inOffset, byte[] __out, int __outOffset)
    {
        crypt(__rcvr, __in, __inOffset, __out, __outOffset, true, false);
    }

    @MethodSubstitution(isStatic = false)
    static void decryptBlock(Object __rcvr, byte[] __in, int __inOffset, byte[] __out, int __outOffset)
    {
        crypt(__rcvr, __in, __inOffset, __out, __outOffset, false, false);
    }

    @MethodSubstitution(isStatic = false)
    static void implDecryptBlock(Object __rcvr, byte[] __in, int __inOffset, byte[] __out, int __outOffset)
    {
        crypt(__rcvr, __in, __inOffset, __out, __outOffset, false, false);
    }

    ///
    // Variation for platforms (e.g. SPARC) that need do key expansion in stubs due to compatibility
    // issues between Java key expansion and hardware crypto instructions.
    ///
    @MethodSubstitution(value = "decryptBlock", isStatic = false)
    static void decryptBlockWithOriginalKey(Object __rcvr, byte[] __in, int __inOffset, byte[] __out, int __outOffset)
    {
        crypt(__rcvr, __in, __inOffset, __out, __outOffset, false, true);
    }

    ///
    // @see #decryptBlockWithOriginalKey(Object, byte[], int, byte[], int)
    ///
    @MethodSubstitution(value = "implDecryptBlock", isStatic = false)
    static void implDecryptBlockWithOriginalKey(Object __rcvr, byte[] __in, int __inOffset, byte[] __out, int __outOffset)
    {
        crypt(__rcvr, __in, __inOffset, __out, __outOffset, false, true);
    }

    private static void crypt(Object __rcvr, byte[] __in, int __inOffset, byte[] __out, int __outOffset, boolean __encrypt, boolean __withOriginalKey)
    {
        checkArgs(__in, __inOffset, __out, __outOffset);
        Object __realReceiver = PiNode.piCastNonNull(__rcvr, AESCryptClass);
        Object __kObject = RawLoadNode.load(__realReceiver, kOffset, JavaKind.Object, LocationIdentity.any());
        Pointer __kAddr = Word.objectToTrackedPointer(__kObject).add(HotSpotRuntime.getArrayBaseOffset(JavaKind.Int));
        Word __inAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__in, HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte) + __inOffset));
        Word __outAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__out, HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte) + __outOffset));
        if (__encrypt)
        {
            encryptBlockStub(HotSpotBackend.ENCRYPT_BLOCK, __inAddr, __outAddr, __kAddr);
        }
        else
        {
            if (__withOriginalKey)
            {
                Object __lastKeyObject = RawLoadNode.load(__realReceiver, lastKeyOffset, JavaKind.Object, LocationIdentity.any());
                Pointer __lastKeyAddr = Word.objectToTrackedPointer(__lastKeyObject).add(HotSpotRuntime.getArrayBaseOffset(JavaKind.Byte));
                decryptBlockWithOriginalKeyStub(HotSpotBackend.DECRYPT_BLOCK_WITH_ORIGINAL_KEY, __inAddr, __outAddr, __kAddr, __lastKeyAddr);
            }
            else
            {
                decryptBlockStub(HotSpotBackend.DECRYPT_BLOCK, __inAddr, __outAddr, __kAddr);
            }
        }
    }

    ///
    // Perform null and array bounds checks for arguments to a cipher operation.
    ///
    static void checkArgs(byte[] __in, int __inOffset, byte[] __out, int __outOffset)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, __inOffset < 0 || __in.length - AES_BLOCK_SIZE_IN_BYTES < __inOffset || __outOffset < 0 || __out.length - AES_BLOCK_SIZE_IN_BYTES < __outOffset))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void encryptBlockStub(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Word __in, Word __out, Pointer __key);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void decryptBlockStub(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Word __in, Word __out, Pointer __key);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void decryptBlockWithOriginalKeyStub(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Word __in, Word __out, Pointer __key, Pointer __originalKey);
}
