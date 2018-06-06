package giraaff.hotspot.meta;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BridgeMethodUtils;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.hotspot.nodes.LoadIndexedPointerNode;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.nodes.type.MetaspacePointerStamp;
import giraaff.hotspot.nodes.type.MethodPointerStamp;
import giraaff.hotspot.word.HotSpotOperation;
import giraaff.hotspot.word.HotSpotOperation.HotspotOpcode;
import giraaff.hotspot.word.PointerCastNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.calc.PointerEqualsNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.memory.HeapAccess;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.type.StampTool;
import giraaff.util.GraalError;
import giraaff.word.WordOperationPlugin;
import giraaff.word.WordTypes;

///
// Extends {@link WordOperationPlugin} to handle {@linkplain HotSpotOperation HotSpot word operations}.
///
// @class HotSpotWordOperationPlugin
final class HotSpotWordOperationPlugin extends WordOperationPlugin
{
    // @cons HotSpotWordOperationPlugin
    HotSpotWordOperationPlugin(SnippetReflectionProvider __snippetReflection, WordTypes __wordTypes)
    {
        super(__snippetReflection, __wordTypes);
    }

    @Override
    protected LoadIndexedNode createLoadIndexedNode(ValueNode __array, ValueNode __index)
    {
        ResolvedJavaType __arrayType = StampTool.typeOrNull(__array);
        Stamp __componentStamp = this.___wordTypes.getWordStamp(__arrayType.getComponentType());
        if (__componentStamp instanceof MetaspacePointerStamp)
        {
            return new LoadIndexedPointerNode(__componentStamp, __array, __index);
        }
        else
        {
            return super.createLoadIndexedNode(__array, __index);
        }
    }

    @Override
    public boolean handleInvoke(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args)
    {
        if (!this.___wordTypes.isWordOperation(__method))
        {
            return false;
        }

        HotSpotOperation __operation = BridgeMethodUtils.getAnnotation(HotSpotOperation.class, __method);
        if (__operation == null)
        {
            processWordOperation(__b, __args, this.___wordTypes.getWordOperation(__method, __b.getMethod().getDeclaringClass()));
            return true;
        }
        processHotSpotWordOperation(__b, __method, __args, __operation);
        return true;
    }

    protected void processHotSpotWordOperation(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args, HotSpotOperation __operation)
    {
        JavaKind __returnKind = __method.getSignature().getReturnKind();
        switch (__operation.opcode())
        {
            case POINTER_EQ:
            case POINTER_NE:
            {
                HotSpotOperation.HotspotOpcode __opcode = __operation.opcode();
                ValueNode __left = __args[0];
                ValueNode __right = __args[1];

                PointerEqualsNode __comparison = __b.add(new PointerEqualsNode(__left, __right));
                ValueNode __eqValue = __b.add(ConstantNode.forBoolean(__opcode == HotSpotOperation.HotspotOpcode.POINTER_EQ));
                ValueNode __neValue = __b.add(ConstantNode.forBoolean(__opcode == HotSpotOperation.HotspotOpcode.POINTER_NE));
                __b.addPush(__returnKind, ConditionalNode.create(__comparison, __eqValue, __neValue, NodeView.DEFAULT));
                break;
            }

            case IS_NULL:
            {
                ValueNode __pointer = __args[0];

                LogicNode __isNull = __b.addWithInputs(IsNullNode.create(__pointer));
                __b.addPush(__returnKind, ConditionalNode.create(__isNull, __b.add(ConstantNode.forBoolean(true)), __b.add(ConstantNode.forBoolean(false)), NodeView.DEFAULT));
                break;
            }

            case FROM_POINTER:
            {
                __b.addPush(__returnKind, new PointerCastNode(StampFactory.forKind(this.___wordKind), __args[0]));
                break;
            }

            case TO_KLASS_POINTER:
            {
                __b.addPush(__returnKind, new PointerCastNode(KlassPointerStamp.klass(), __args[0]));
                break;
            }

            case TO_METHOD_POINTER:
            {
                __b.addPush(__returnKind, new PointerCastNode(MethodPointerStamp.method(), __args[0]));
                break;
            }

            case READ_KLASS_POINTER:
            {
                Stamp __readStamp = KlassPointerStamp.klass();
                AddressNode __address = makeAddress(__b, __args[0], __args[1]);
                LocationIdentity __location;
                if (__args.length == 2)
                {
                    __location = LocationIdentity.any();
                }
                else
                {
                    __location = this.___snippetReflection.asObject(LocationIdentity.class, __args[2].asJavaConstant());
                }
                ReadNode __read = __b.add(new ReadNode(__address, __location, __readStamp, HeapAccess.BarrierType.NONE));
                __b.push(__returnKind, __read);
                break;
            }

            default:
                throw GraalError.shouldNotReachHere("unknown operation: " + __operation.opcode());
        }
    }
}
