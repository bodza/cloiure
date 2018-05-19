package graalvm.compiler.hotspot.phases;

import static graalvm.compiler.nodes.ConstantNode.getConstantNodes;
import static graalvm.compiler.nodes.NamedLocationIdentity.FINAL_LOCATION;

import graalvm.compiler.core.common.CompressEncoding;
import graalvm.compiler.core.common.type.AbstractObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.nodes.HotSpotCompressionNode;
import graalvm.compiler.hotspot.nodes.type.HotSpotNarrowOopStamp;
import graalvm.compiler.hotspot.nodes.type.KlassPointerStamp;
import graalvm.compiler.hotspot.replacements.HubGetClassNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.FloatingReadNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.memory.address.OffsetAddressNode;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaField;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.hotspot.HotSpotResolvedPrimitiveType;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * For AOT compilation we aren't allowed to use a {@link Class} reference ({@code javaMirror})
 * directly. Instead the {@link Class} reference should be obtained from the {@code Klass} object.
 * The reason for this is, that in Class Data Sharing (CDS) a {@code Klass} object is mapped to a
 * fixed address in memory, but the {@code javaMirror} is not (which lives in the Java heap).
 *
 * Lowering can introduce new {@link ConstantNode}s containing a {@link Class} reference, thus this
 * phase must be applied after {@link LoweringPhase}.
 */
public class LoadJavaMirrorWithKlassPhase extends BasePhase<PhaseContext>
{
    private final CompressEncoding oopEncoding;

    public LoadJavaMirrorWithKlassPhase(GraalHotSpotVMConfig config)
    {
        this.oopEncoding = config.useCompressedOops ? config.getOopEncoding() : null;
    }

    private ValueNode getClassConstantReplacement(StructuredGraph graph, PhaseContext context, JavaConstant constant)
    {
        if (constant instanceof HotSpotObjectConstant)
        {
            ConstantReflectionProvider constantReflection = context.getConstantReflection();
            ResolvedJavaType type = constantReflection.asJavaType(constant);
            if (type != null)
            {
                MetaAccessProvider metaAccess = context.getMetaAccess();
                Stamp stamp = StampFactory.objectNonNull(TypeReference.createExactTrusted(metaAccess.lookupJavaType(Class.class)));

                if (type instanceof HotSpotResolvedObjectType)
                {
                    ConstantNode klass = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), ((HotSpotResolvedObjectType) type).klass(), metaAccess, graph);
                    ValueNode getClass = graph.unique(new HubGetClassNode(metaAccess, klass));

                    if (((HotSpotObjectConstant) constant).isCompressed())
                    {
                        return HotSpotCompressionNode.compress(getClass, oopEncoding);
                    }
                    else
                    {
                        return getClass;
                    }
                }
                else
                {
                    /*
                     * Primitive classes are more difficult since they don't have a corresponding
                     * Klass* so get them from Class.TYPE for the java box type.
                     */
                    HotSpotResolvedPrimitiveType primitive = (HotSpotResolvedPrimitiveType) type;
                    ResolvedJavaType boxingClass = metaAccess.lookupJavaType(primitive.getJavaKind().toBoxedJavaClass());
                    ConstantNode clazz = ConstantNode.forConstant(context.getConstantReflection().asJavaClass(boxingClass), metaAccess, graph);
                    HotSpotResolvedJavaField[] a = (HotSpotResolvedJavaField[]) boxingClass.getStaticFields();
                    HotSpotResolvedJavaField typeField = null;
                    for (HotSpotResolvedJavaField f : a)
                    {
                        if (f.getName().equals("TYPE"))
                        {
                            typeField = f;
                            break;
                        }
                    }
                    if (typeField == null)
                    {
                        throw new GraalError("Can't find TYPE field in class");
                    }

                    if (oopEncoding != null)
                    {
                        stamp = HotSpotNarrowOopStamp.compressed((AbstractObjectStamp) stamp, oopEncoding);
                    }
                    AddressNode address = graph.unique(new OffsetAddressNode(clazz, ConstantNode.forLong(typeField.offset(), graph)));
                    ValueNode read = graph.unique(new FloatingReadNode(address, FINAL_LOCATION, null, stamp));

                    if (oopEncoding == null || ((HotSpotObjectConstant) constant).isCompressed())
                    {
                        return read;
                    }
                    else
                    {
                        return HotSpotCompressionNode.uncompress(read, oopEncoding);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        for (ConstantNode node : getConstantNodes(graph))
        {
            JavaConstant constant = node.asJavaConstant();
            ValueNode freadNode = getClassConstantReplacement(graph, context, constant);
            if (freadNode != null)
            {
                node.replace(graph, freadNode);
            }
        }
    }

    @Override
    public float codeSizeIncrease()
    {
        return 2.5f;
    }
}
