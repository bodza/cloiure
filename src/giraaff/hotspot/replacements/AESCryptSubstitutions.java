package giraaff.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
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
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;
import giraaff.word.Word;

/**
 * Substitutions for {@code com.sun.crypto.provider.AESCrypt} methods.
 */
@ClassSubstitution(className = "com.sun.crypto.provider.AESCrypt", optional = true)
public class AESCryptSubstitutions
{
    static final long kOffset;
    static final long lastKeyOffset;
    static final Class<?> AESCryptClass;

    /**
     * The AES block size is a constant 128 bits as defined by the
     * <a href="http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.197.pdf">standard<a/>.
     */
    static final int AES_BLOCK_SIZE_IN_BYTES = 16;

    static
    {
        try
        {
            // Need to use the system class loader as com.sun.crypto.provider.AESCrypt is normally loaded
            // by the extension class loader, which is not delegated to by the JVMCI class loader.
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            AESCryptClass = Class.forName("com.sun.crypto.provider.AESCrypt", true, cl);
            kOffset = UnsafeAccess.UNSAFE.objectFieldOffset(AESCryptClass.getDeclaredField("K"));
            lastKeyOffset = UnsafeAccess.UNSAFE.objectFieldOffset(AESCryptClass.getDeclaredField("lastKey"));
        }
        catch (Exception ex)
        {
            throw new GraalError(ex);
        }
    }

    @MethodSubstitution(isStatic = false)
    static void encryptBlock(Object rcvr, byte[] in, int inOffset, byte[] out, int outOffset)
    {
        crypt(rcvr, in, inOffset, out, outOffset, true, false);
    }

    @MethodSubstitution(isStatic = false)
    static void implEncryptBlock(Object rcvr, byte[] in, int inOffset, byte[] out, int outOffset)
    {
        crypt(rcvr, in, inOffset, out, outOffset, true, false);
    }

    @MethodSubstitution(isStatic = false)
    static void decryptBlock(Object rcvr, byte[] in, int inOffset, byte[] out, int outOffset)
    {
        crypt(rcvr, in, inOffset, out, outOffset, false, false);
    }

    @MethodSubstitution(isStatic = false)
    static void implDecryptBlock(Object rcvr, byte[] in, int inOffset, byte[] out, int outOffset)
    {
        crypt(rcvr, in, inOffset, out, outOffset, false, false);
    }

    /**
     * Variation for platforms (e.g. SPARC) that need do key expansion in stubs due to compatibility
     * issues between Java key expansion and hardware crypto instructions.
     */
    @MethodSubstitution(value = "decryptBlock", isStatic = false)
    static void decryptBlockWithOriginalKey(Object rcvr, byte[] in, int inOffset, byte[] out, int outOffset)
    {
        crypt(rcvr, in, inOffset, out, outOffset, false, true);
    }

    /**
     * @see #decryptBlockWithOriginalKey(Object, byte[], int, byte[], int)
     */
    @MethodSubstitution(value = "implDecryptBlock", isStatic = false)
    static void implDecryptBlockWithOriginalKey(Object rcvr, byte[] in, int inOffset, byte[] out, int outOffset)
    {
        crypt(rcvr, in, inOffset, out, outOffset, false, true);
    }

    private static void crypt(Object rcvr, byte[] in, int inOffset, byte[] out, int outOffset, boolean encrypt, boolean withOriginalKey)
    {
        checkArgs(in, inOffset, out, outOffset);
        Object realReceiver = PiNode.piCastNonNull(rcvr, AESCryptClass);
        Object kObject = RawLoadNode.load(realReceiver, kOffset, JavaKind.Object, LocationIdentity.any());
        Pointer kAddr = Word.objectToTrackedPointer(kObject).add(HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Int));
        Word inAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(in, HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Byte) + inOffset));
        Word outAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(out, HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Byte) + outOffset));
        if (encrypt)
        {
            encryptBlockStub(HotSpotBackend.ENCRYPT_BLOCK, inAddr, outAddr, kAddr);
        }
        else
        {
            if (withOriginalKey)
            {
                Object lastKeyObject = RawLoadNode.load(realReceiver, lastKeyOffset, JavaKind.Object, LocationIdentity.any());
                Pointer lastKeyAddr = Word.objectToTrackedPointer(lastKeyObject).add(HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Byte));
                decryptBlockWithOriginalKeyStub(HotSpotBackend.DECRYPT_BLOCK_WITH_ORIGINAL_KEY, inAddr, outAddr, kAddr, lastKeyAddr);
            }
            else
            {
                decryptBlockStub(HotSpotBackend.DECRYPT_BLOCK, inAddr, outAddr, kAddr);
            }
        }
    }

    /**
     * Perform null and array bounds checks for arguments to a cipher operation.
     */
    static void checkArgs(byte[] in, int inOffset, byte[] out, int outOffset)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, inOffset < 0 || in.length - AES_BLOCK_SIZE_IN_BYTES < inOffset || outOffset < 0 || out.length - AES_BLOCK_SIZE_IN_BYTES < outOffset))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void encryptBlockStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word in, Word out, Pointer key);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void decryptBlockStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word in, Word out, Pointer key);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void decryptBlockWithOriginalKeyStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word in, Word out, Pointer key, Pointer originalKey);
}
