package giraaff.hotspot.amd64;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaKind;

import org.graalvm.collections.EconomicMap;

import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.core.amd64.AMD64AddressNode;
import giraaff.core.amd64.AMD64CompressAddressLowering;
import giraaff.core.common.CompressEncoding;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.graph.Node;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.nodes.GraalHotSpotVMConfigNode;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
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
import giraaff.options.OptionValues;

public class AMD64HotSpotAddressLowering extends AMD64CompressAddressLowering
{
    private static final int ADDRESS_BITS = 64;
    private static final int INT_BITS = 32;

    private final long heapBase;
    private final Register heapBaseRegister;
    private final GraalHotSpotVMConfig config;
    private final boolean generatePIC;

    public AMD64HotSpotAddressLowering(GraalHotSpotVMConfig config, Register heapBaseRegister, OptionValues options)
    {
        this.heapBase = config.getOopEncoding().getBase();
        this.config = config;
        this.generatePIC = GraalOptions.GeneratePIC.getValue(options);
        if (heapBase == 0 && !generatePIC)
        {
            this.heapBaseRegister = null;
        }
        else
        {
            this.heapBaseRegister = heapBaseRegister;
        }
    }

    @Override
    protected final boolean improveUncompression(AMD64AddressNode addr, CompressionNode compression, ValueNode other)
    {
        CompressEncoding encoding = compression.getEncoding();
        Scale scale = Scale.fromShift(encoding.getShift());
        if (scale == null)
        {
            return false;
        }

        if (heapBaseRegister != null && encoding.getBase() == heapBase)
        {
            if ((!generatePIC || compression.stamp(NodeView.DEFAULT) instanceof ObjectStamp) && other == null)
            {
                // With PIC it is only legal to do for oops since the base value may be
                // different at runtime.
                ValueNode base = compression.graph().unique(new HeapBaseNode(heapBaseRegister));
                addr.setBase(base);
            }
            else
            {
                return false;
            }
        }
        else if (encoding.getBase() != 0 || (generatePIC && compression.stamp(NodeView.DEFAULT) instanceof KlassPointerStamp))
        {
            if (generatePIC)
            {
                if (other == null)
                {
                    ValueNode base = compression.graph().unique(new GraalHotSpotVMConfigNode(config, config.MARKID_NARROW_KLASS_BASE_ADDRESS, JavaKind.Long));
                    addr.setBase(base);
                }
                else
                {
                    return false;
                }
            }
            else
            {
                if (updateDisplacement(addr, encoding.getBase(), false))
                {
                    addr.setBase(other);
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {
            addr.setBase(other);
        }

        addr.setScale(scale);
        addr.setIndex(compression.getValue());
        return true;
    }

    @Override
    public void preProcess(StructuredGraph graph)
    {
        if (graph.hasLoops())
        {
            LoopsData loopsData = new LoopsData(graph);
            loopsData.detectedCountedLoops();
            for (LoopEx loop : loopsData.countedLoops())
            {
                for (OffsetAddressNode offsetAdressNode : loop.whole().nodes().filter(OffsetAddressNode.class))
                {
                    tryOptimize(offsetAdressNode, loop);
                }
            }
        }
    }

    @Override
    public void postProcess(AddressNode lowered)
    {
        // Allow implicit zero extend for always positive input. This
        // assumes that the upper bits of the operand is zero out by
        // the backend.
        AMD64AddressNode address = (AMD64AddressNode) lowered;
        address.setBase(tryImplicitZeroExtend(address.getBase()));
        address.setIndex(tryImplicitZeroExtend(address.getIndex()));
    }

    private static void tryOptimize(OffsetAddressNode offsetAddress, LoopEx loop)
    {
        EconomicMap<Node, InductionVariable> ivs = loop.getInductionVariables();
        InductionVariable currentIV = ivs.get(offsetAddress.getOffset());
        while (currentIV != null)
        {
            if (!(currentIV instanceof DerivedInductionVariable))
            {
                break;
            }
            ValueNode currentValue = currentIV.valueNode();
            if (currentValue.isDeleted())
            {
                break;
            }

            if (currentValue instanceof ZeroExtendNode)
            {
                ZeroExtendNode zeroExtendNode = (ZeroExtendNode) currentValue;
                if (applicableToImplicitZeroExtend(zeroExtendNode))
                {
                    ValueNode input = zeroExtendNode.getValue();
                    if (input instanceof AddNode)
                    {
                        AddNode add = (AddNode) input;
                        if (add.getX().isConstant())
                        {
                            optimizeAdd(zeroExtendNode, (ConstantNode) add.getX(), add.getY(), loop);
                        }
                        else if (add.getY().isConstant())
                        {
                            optimizeAdd(zeroExtendNode, (ConstantNode) add.getY(), add.getX(), loop);
                        }
                    }
                }
            }

            currentIV = ((DerivedInductionVariable) currentIV).getBase();
        }
    }

    /**
     * Given that Add(a, cst) is always positive, performs the following: ZeroExtend(Add(a, cst)) ->
     * Add(SignExtend(a), SignExtend(cst)).
     */
    private static void optimizeAdd(ZeroExtendNode zeroExtendNode, ConstantNode constant, ValueNode other, LoopEx loop)
    {
        StructuredGraph graph = zeroExtendNode.graph();
        AddNode addNode = graph.unique(new AddNode(signExtend(other, loop), ConstantNode.forLong(constant.asJavaConstant().asInt(), graph)));
        zeroExtendNode.replaceAtUsages(addNode);
    }

    /**
     * Create a sign extend for {@code input}, or zero extend if {@code input} can be proven
     * positive.
     */
    private static ValueNode signExtend(ValueNode input, LoopEx loop)
    {
        StructuredGraph graph = input.graph();
        if (input instanceof PhiNode)
        {
            EconomicMap<Node, InductionVariable> ivs = loop.getInductionVariables();
            InductionVariable inductionVariable = ivs.get(input);
            if (inductionVariable != null && inductionVariable instanceof BasicInductionVariable)
            {
                CountedLoopInfo countedLoopInfo = loop.counted();
                IntegerStamp initStamp = (IntegerStamp) inductionVariable.initNode().stamp(NodeView.DEFAULT);
                if (initStamp.isPositive())
                {
                    if (inductionVariable.isConstantExtremum())
                    {
                        long init = inductionVariable.constantInit();
                        long stride = inductionVariable.constantStride();
                        long extremum = inductionVariable.constantExtremum();

                        if (init >= 0 && extremum >= 0)
                        {
                            long shortestTrip = (extremum - init) / stride + 1;
                            if (countedLoopInfo.constantMaxTripCount().equals(shortestTrip))
                            {
                                return graph.unique(new ZeroExtendNode(input, INT_BITS, ADDRESS_BITS, true));
                            }
                        }
                    }
                    if (countedLoopInfo.getCounter() == inductionVariable && inductionVariable.direction() == InductionVariable.Direction.Up && countedLoopInfo.getOverFlowGuard() != null)
                    {
                        return graph.unique(new ZeroExtendNode(input, INT_BITS, ADDRESS_BITS, true));
                    }
                }
            }
        }
        return input.graph().maybeAddOrUnique(SignExtendNode.create(input, ADDRESS_BITS, NodeView.DEFAULT));
    }

    private static boolean applicableToImplicitZeroExtend(ZeroExtendNode zeroExtendNode)
    {
        return zeroExtendNode.isInputAlwaysPositive() && zeroExtendNode.getInputBits() == INT_BITS && zeroExtendNode.getResultBits() == ADDRESS_BITS;
    }

    private static ValueNode tryImplicitZeroExtend(ValueNode input)
    {
        if (input instanceof ZeroExtendNode)
        {
            ZeroExtendNode zeroExtendNode = (ZeroExtendNode) input;
            if (applicableToImplicitZeroExtend(zeroExtendNode))
            {
                return zeroExtendNode.getValue();
            }
        }
        return input;
    }
}
