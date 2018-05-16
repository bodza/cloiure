package graalvm.compiler.hotspot.meta;

import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.POINTER_EQ;
import static graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode.POINTER_NE;
import static graalvm.compiler.nodes.ConstantNode.forBoolean;
import static org.graalvm.word.LocationIdentity.any;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.bytecode.BridgeMethodUtils;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.nodes.LoadIndexedPointerNode;
import graalvm.compiler.hotspot.nodes.type.KlassPointerStamp;
import graalvm.compiler.hotspot.nodes.type.MetaspacePointerStamp;
import graalvm.compiler.hotspot.nodes.type.MethodPointerStamp;
import graalvm.compiler.hotspot.word.HotSpotOperation;
import graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode;
import graalvm.compiler.hotspot.word.PointerCastNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.ConditionalNode;
import graalvm.compiler.nodes.calc.IsNullNode;
import graalvm.compiler.nodes.calc.PointerEqualsNode;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import graalvm.compiler.nodes.java.LoadIndexedNode;
import graalvm.compiler.nodes.memory.HeapAccess.BarrierType;
import graalvm.compiler.nodes.memory.ReadNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.word.WordOperationPlugin;
import graalvm.compiler.word.WordTypes;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Extends {@link WordOperationPlugin} to handle {@linkplain HotSpotOperation HotSpot word
 * operations}.
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
                assert args.length == 2;
                HotspotOpcode opcode = operation.opcode();
                ValueNode left = args[0];
                ValueNode right = args[1];
                assert left.stamp(NodeView.DEFAULT) instanceof MetaspacePointerStamp : left + " " + left.stamp(NodeView.DEFAULT);
                assert right.stamp(NodeView.DEFAULT) instanceof MetaspacePointerStamp : right + " " + right.stamp(NodeView.DEFAULT);
                assert opcode == POINTER_EQ || opcode == POINTER_NE;

                PointerEqualsNode comparison = b.add(new PointerEqualsNode(left, right));
                ValueNode eqValue = b.add(forBoolean(opcode == POINTER_EQ));
                ValueNode neValue = b.add(forBoolean(opcode == POINTER_NE));
                b.addPush(returnKind, ConditionalNode.create(comparison, eqValue, neValue, NodeView.DEFAULT));
                break;

            case IS_NULL:
                assert args.length == 1;
                ValueNode pointer = args[0];
                assert pointer.stamp(NodeView.DEFAULT) instanceof MetaspacePointerStamp;

                LogicNode isNull = b.addWithInputs(IsNullNode.create(pointer));
                b.addPush(returnKind, ConditionalNode.create(isNull, b.add(forBoolean(true)), b.add(forBoolean(false)), NodeView.DEFAULT));
                break;

            case FROM_POINTER:
                assert args.length == 1;
                b.addPush(returnKind, new PointerCastNode(StampFactory.forKind(wordKind), args[0]));
                break;

            case TO_KLASS_POINTER:
                assert args.length == 1;
                b.addPush(returnKind, new PointerCastNode(KlassPointerStamp.klass(), args[0]));
                break;

            case TO_METHOD_POINTER:
                assert args.length == 1;
                b.addPush(returnKind, new PointerCastNode(MethodPointerStamp.method(), args[0]));
                break;

            case READ_KLASS_POINTER:
                assert args.length == 2 || args.length == 3;
                Stamp readStamp = KlassPointerStamp.klass();
                AddressNode address = makeAddress(b, args[0], args[1]);
                LocationIdentity location;
                if (args.length == 2)
                {
                    location = any();
                }
                else
                {
                    assert args[2].isConstant();
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
