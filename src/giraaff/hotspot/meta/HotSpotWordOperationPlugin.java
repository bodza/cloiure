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
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.type.StampTool;
import giraaff.util.GraalError;
import giraaff.word.WordOperationPlugin;
import giraaff.word.WordTypes;

/**
 * Extends {@link WordOperationPlugin} to handle {@linkplain HotSpotOperation HotSpot word operations}.
 */
class HotSpotWordOperationPlugin extends WordOperationPlugin
{
    HotSpotWordOperationPlugin(SnippetReflectionProvider snippetReflection, WordTypes wordTypes)
    {
        super(snippetReflection, wordTypes);
    }

    @Override
    protected LoadIndexedNode createLoadIndexedNode(ValueNode array, ValueNode index)
    {
        ResolvedJavaType arrayType = StampTool.typeOrNull(array);
        Stamp componentStamp = wordTypes.getWordStamp(arrayType.getComponentType());
        if (componentStamp instanceof MetaspacePointerStamp)
        {
            return new LoadIndexedPointerNode(componentStamp, array, index);
        }
        else
        {
            return super.createLoadIndexedNode(array, index);
        }
    }

    @Override
    public boolean handleInvoke(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode[] args)
    {
        if (!wordTypes.isWordOperation(method))
        {
            return false;
        }

        HotSpotOperation operation = BridgeMethodUtils.getAnnotation(HotSpotOperation.class, method);
        if (operation == null)
        {
            processWordOperation(b, args, wordTypes.getWordOperation(method, b.getMethod().getDeclaringClass()));
            return true;
        }
        processHotSpotWordOperation(b, method, args, operation);
        return true;
    }

    protected void processHotSpotWordOperation(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode[] args, HotSpotOperation operation)
    {
        JavaKind returnKind = method.getSignature().getReturnKind();
        switch (operation.opcode())
        {
            case POINTER_EQ:
            case POINTER_NE:
                HotspotOpcode opcode = operation.opcode();
                ValueNode left = args[0];
                ValueNode right = args[1];

                PointerEqualsNode comparison = b.add(new PointerEqualsNode(left, right));
                ValueNode eqValue = b.add(ConstantNode.forBoolean(opcode == HotspotOpcode.POINTER_EQ));
                ValueNode neValue = b.add(ConstantNode.forBoolean(opcode == HotspotOpcode.POINTER_NE));
                b.addPush(returnKind, ConditionalNode.create(comparison, eqValue, neValue, NodeView.DEFAULT));
                break;

            case IS_NULL:
                ValueNode pointer = args[0];

                LogicNode isNull = b.addWithInputs(IsNullNode.create(pointer));
                b.addPush(returnKind, ConditionalNode.create(isNull, b.add(ConstantNode.forBoolean(true)), b.add(ConstantNode.forBoolean(false)), NodeView.DEFAULT));
                break;

            case FROM_POINTER:
                b.addPush(returnKind, new PointerCastNode(StampFactory.forKind(wordKind), args[0]));
                break;

            case TO_KLASS_POINTER:
                b.addPush(returnKind, new PointerCastNode(KlassPointerStamp.klass(), args[0]));
                break;

            case TO_METHOD_POINTER:
                b.addPush(returnKind, new PointerCastNode(MethodPointerStamp.method(), args[0]));
                break;

            case READ_KLASS_POINTER:
                Stamp readStamp = KlassPointerStamp.klass();
                AddressNode address = makeAddress(b, args[0], args[1]);
                LocationIdentity location;
                if (args.length == 2)
                {
                    location = LocationIdentity.any();
                }
                else
                {
                    location = snippetReflection.asObject(LocationIdentity.class, args[2].asJavaConstant());
                }
                ReadNode read = b.add(new ReadNode(address, location, readStamp, BarrierType.NONE));
                b.push(returnKind, read);
                break;

            default:
                throw GraalError.shouldNotReachHere("unknown operation: " + operation.opcode());
        }
    }
}
