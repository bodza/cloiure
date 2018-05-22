package giraaff.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.debug.GraalError;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.word.Word;
import giraaff.util.UnsafeAccess;

@ClassSubstitution(className = "sun.security.provider.SHA5", optional = true)
public class SHA5Substitutions
{
    static final long stateOffset;

    static final Class<?> shaClass;

    public static final String implCompressName = "implCompress0";

    static
    {
        try
        {
            // Need to use the system class loader as com.sun.crypto.provider.AESCrypt
            // is normally loaded by the extension class loader which is not delegated
            // to by the JVMCI class loader.
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            shaClass = Class.forName("sun.security.provider.SHA5", true, cl);
            stateOffset = UnsafeAccess.UNSAFE.objectFieldOffset(shaClass.getDeclaredField("state"));
        }
        catch (Exception ex)
        {
            throw new GraalError(ex);
        }
    }

    @MethodSubstitution(isStatic = false)
    static void implCompress0(Object receiver, byte[] buf, int ofs)
    {
        Object realReceiver = PiNode.piCastNonNull(receiver, shaClass);
        Object state = RawLoadNode.load(realReceiver, stateOffset, JavaKind.Object, LocationIdentity.any());
        Word bufAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(buf, HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Byte) + ofs));
        Word stateAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(state, HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Int)));
        HotSpotBackend.sha5ImplCompressStub(bufAddr, stateAddr);
    }
}
