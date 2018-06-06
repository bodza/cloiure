package giraaff.hotspot.amd64;

import jdk.vm.ci.code.Register;

import org.graalvm.collections.EconomicMap;

import giraaff.asm.amd64.AMD64Address;
import giraaff.core.amd64.AMD64AddressNode;
import giraaff.core.amd64.AMD64CompressAddressLowering;
import giraaff.core.common.CompressEncoding;
import giraaff.core.common.type.IntegerStamp;
import giraaff.graph.Node;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.loop.BasicInductionVariable;
import giraaff.loop.CountedLoopInfo;
import giraaff.loop.DerivedInductionVariable;
import giraaff.loop.InductionVariable;
import giraaff.loop.LoopEx;
import giraaff.loop.LoopsData;
import giraaff.nodes.CompressionNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.SignExtendNode;
import giraaff.nodes.calc.ZeroExtendNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;

// @class AMD64HotSpotAddressLowering
public final class AMD64HotSpotAddressLowering extends AMD64CompressAddressLowering
{
    // @def
    private static final int ADDRESS_BITS = 64;
    // @def
    private static final int INT_BITS = 32;

    // @field
    private final long ___heapBase;
    // @field
    private final Register ___heapBaseRegister;

    // @cons AMD64HotSpotAddressLowering
    public AMD64HotSpotAddressLowering(Register __heapBaseRegister)
    {
        super();
        this.___heapBase = HotSpotRuntime.oopEncoding.getBase();
        if (this.___heapBase == 0)
        {
            this.___heapBaseRegister = null;
        }
        else
        {
            this.___heapBaseRegister = __heapBaseRegister;
        }
    }

    @Override
    protected final boolean improveUncompression(AMD64AddressNode __addr, CompressionNode __compression, ValueNode __other)
    {
        CompressEncoding __encoding = __compression.getEncoding();
        AMD64Address.Scale __scale = AMD64Address.Scale.fromShift(__encoding.getShift());
        if (__scale == null)
        {
            return false;
        }

        if (this.___heapBaseRegister != null && __encoding.getBase() == this.___heapBase)
        {
            if (__other == null)
            {
                ValueNode __base = __compression.graph().unique(new AMD64CompressAddressLowering.HeapBaseNode(this.___heapBaseRegister));
                __addr.setBase(__base);
            }
            else
            {
                return false;
            }
        }
        else if (__encoding.getBase() != 0)
        {
            if (updateDisplacement(__addr, __encoding.getBase(), false))
            {
                __addr.setBase(__other);
            }
            else
            {
                return false;
            }
        }
        else
        {
            __addr.setBase(__other);
        }

        __addr.setScale(__scale);
        __addr.setIndex(__compression.getValue());
        return true;
    }

    @Override
    public void preProcess(StructuredGraph __graph)
    {
        if (__graph.hasLoops())
        {
            LoopsData __loopsData = new LoopsData(__graph);
            __loopsData.detectedCountedLoops();
            for (LoopEx __loop : __loopsData.countedLoops())
            {
                for (OffsetAddressNode __offsetAdressNode : __loop.whole().nodes().filter(OffsetAddressNode.class))
                {
                    tryOptimize(__offsetAdressNode, __loop);
                }
            }
        }
    }

    @Override
    public void postProcess(AddressNode __lowered)
    {
        // Allow implicit zero extend for always positive input. This assumes
        // that the upper bits of the operand is zero out by the backend.
        AMD64AddressNode __address = (AMD64AddressNode) __lowered;
        __address.setBase(tryImplicitZeroExtend(__address.getBase()));
        __address.setIndex(tryImplicitZeroExtend(__address.getIndex()));
    }

    private static void tryOptimize(OffsetAddressNode __offsetAddress, LoopEx __loop)
    {
        EconomicMap<Node, InductionVariable> __ivs = __loop.getInductionVariables();
        InductionVariable __currentIV = __ivs.get(__offsetAddress.getOffset());
        while (__currentIV != null)
        {
            if (!(__currentIV instanceof DerivedInductionVariable))
            {
                break;
            }
            ValueNode __currentValue = __currentIV.valueNode();
            if (__currentValue.isDeleted())
            {
                break;
            }

            if (__currentValue instanceof ZeroExtendNode)
            {
                ZeroExtendNode __zeroExtendNode = (ZeroExtendNode) __currentValue;
                if (applicableToImplicitZeroExtend(__zeroExtendNode))
                {
                    ValueNode __input = __zeroExtendNode.getValue();
                    if (__input instanceof AddNode)
                    {
                        AddNode __add = (AddNode) __input;
                        if (__add.getX().isConstant())
                        {
                            optimizeAdd(__zeroExtendNode, (ConstantNode) __add.getX(), __add.getY(), __loop);
                        }
                        else if (__add.getY().isConstant())
                        {
                            optimizeAdd(__zeroExtendNode, (ConstantNode) __add.getY(), __add.getX(), __loop);
                        }
                    }
                }
            }

            __currentIV = ((DerivedInductionVariable) __currentIV).getBase();
        }
    }

    ///
    // Given that Add(a, cst) is always positive, performs the following: ZeroExtend(Add(a, cst)) -> Add(SignExtend(a), SignExtend(cst)).
    ///
    private static void optimizeAdd(ZeroExtendNode __zeroExtendNode, ConstantNode __constant, ValueNode __other, LoopEx __loop)
    {
        StructuredGraph __graph = __zeroExtendNode.graph();
        AddNode __addNode = __graph.unique(new AddNode(signExtend(__other, __loop), ConstantNode.forLong(__constant.asJavaConstant().asInt(), __graph)));
        __zeroExtendNode.replaceAtUsages(__addNode);
    }

    ///
    // Create a sign extend for {@code input}, or zero extend if {@code input} can be proven positive.
    ///
    private static ValueNode signExtend(ValueNode __input, LoopEx __loop)
    {
        StructuredGraph __graph = __input.graph();
        if (__input instanceof PhiNode)
        {
            EconomicMap<Node, InductionVariable> __ivs = __loop.getInductionVariables();
            InductionVariable __inductionVariable = __ivs.get(__input);
            if (__inductionVariable != null && __inductionVariable instanceof BasicInductionVariable)
            {
                CountedLoopInfo __countedLoopInfo = __loop.counted();
                IntegerStamp __initStamp = (IntegerStamp) __inductionVariable.initNode().stamp(NodeView.DEFAULT);
                if (__initStamp.isPositive())
                {
                    if (__inductionVariable.isConstantExtremum())
                    {
                        long __init = __inductionVariable.constantInit();
                        long __stride = __inductionVariable.constantStride();
                        long __extremum = __inductionVariable.constantExtremum();

                        if (__init >= 0 && __extremum >= 0)
                        {
                            long __shortestTrip = (__extremum - __init) / __stride + 1;
                            if (__countedLoopInfo.constantMaxTripCount().equals(__shortestTrip))
                            {
                                return __graph.unique(new ZeroExtendNode(__input, INT_BITS, ADDRESS_BITS, true));
                            }
                        }
                    }
                    if (__countedLoopInfo.getCounter() == __inductionVariable && __inductionVariable.direction() == InductionVariable.Direction.Up && __countedLoopInfo.getOverFlowGuard() != null)
                    {
                        return __graph.unique(new ZeroExtendNode(__input, INT_BITS, ADDRESS_BITS, true));
                    }
                }
            }
        }
        return __input.graph().maybeAddOrUnique(SignExtendNode.create(__input, ADDRESS_BITS, NodeView.DEFAULT));
    }

    private static boolean applicableToImplicitZeroExtend(ZeroExtendNode __zeroExtendNode)
    {
        return __zeroExtendNode.isInputAlwaysPositive() && __zeroExtendNode.getInputBits() == INT_BITS && __zeroExtendNode.getResultBits() == ADDRESS_BITS;
    }

    private static ValueNode tryImplicitZeroExtend(ValueNode __input)
    {
        if (__input instanceof ZeroExtendNode)
        {
            ZeroExtendNode __zeroExtendNode = (ZeroExtendNode) __input;
            if (applicableToImplicitZeroExtend(__zeroExtendNode))
            {
                return __zeroExtendNode.getValue();
            }
        }
        return __input;
    }
}
