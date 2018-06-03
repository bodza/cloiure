package giraaff.core.amd64;

import jdk.vm.ci.meta.JavaConstant;

import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.core.common.NumUtil;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.calc.NegateNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.phases.common.AddressLoweringPhase.AddressLowering;

// @class AMD64AddressLowering
public class AMD64AddressLowering extends AddressLowering
{
    // @def
    private static final int ADDRESS_BITS = 64;

    @Override
    public AddressNode lower(ValueNode __base, ValueNode __offset)
    {
        AMD64AddressNode __ret = new AMD64AddressNode(__base, __offset);
        StructuredGraph __graph = __base.graph();

        boolean __changed;
        do
        {
            __changed = improve(__graph, __ret, false, false);
        } while (__changed);

        return __graph.unique(__ret);
    }

    private static boolean checkAddressBitWidth(ValueNode __value)
    {
        return __value == null || __value.stamp(NodeView.DEFAULT) instanceof AbstractPointerStamp || IntegerStamp.getBits(__value.stamp(NodeView.DEFAULT)) == ADDRESS_BITS;
    }

    /**
     * Tries to optimize addresses so that they match the AMD64-specific addressing mode better
     * (base + index * scale + displacement).
     *
     * @param graph the current graph
     * @param ret the address that should be optimized
     * @param isBaseNegated determines if the address base is negated. if so, all values that are
     *            extracted from the base will be negated as well
     * @param isIndexNegated determines if the index is negated. if so, all values that are
     *            extracted from the index will be negated as well
     * @return true if the address was modified
     */
    protected boolean improve(StructuredGraph __graph, AMD64AddressNode __ret, boolean __isBaseNegated, boolean __isIndexNegated)
    {
        ValueNode __newBase = improveInput(__ret, __ret.getBase(), 0, __isBaseNegated);
        if (__newBase != __ret.getBase())
        {
            __ret.setBase(__newBase);
            return true;
        }

        ValueNode __newIdx = improveInput(__ret, __ret.getIndex(), __ret.getScale().log2, __isIndexNegated);
        if (__newIdx != __ret.getIndex())
        {
            __ret.setIndex(__newIdx);
            return true;
        }

        if (__ret.getIndex() instanceof LeftShiftNode)
        {
            LeftShiftNode __shift = (LeftShiftNode) __ret.getIndex();
            if (__shift.getY().isConstant())
            {
                int __amount = __ret.getScale().log2 + __shift.getY().asJavaConstant().asInt();
                Scale __scale = Scale.fromShift(__amount);
                if (__scale != null)
                {
                    __ret.setIndex(__shift.getX());
                    __ret.setScale(__scale);
                    return true;
                }
            }
        }

        if (__ret.getScale() == Scale.Times1)
        {
            if (__ret.getIndex() == null && __ret.getBase() instanceof AddNode)
            {
                AddNode __add = (AddNode) __ret.getBase();
                __ret.setBase(__add.getX());
                __ret.setIndex(considerNegation(__graph, __add.getY(), __isBaseNegated));
                return true;
            }

            if (__ret.getBase() == null && __ret.getIndex() instanceof AddNode)
            {
                AddNode __add = (AddNode) __ret.getIndex();
                __ret.setBase(considerNegation(__graph, __add.getX(), __isIndexNegated));
                __ret.setIndex(__add.getY());
                return true;
            }

            if (__ret.getBase() instanceof LeftShiftNode && !(__ret.getIndex() instanceof LeftShiftNode))
            {
                ValueNode __tmp = __ret.getBase();
                __ret.setBase(considerNegation(__graph, __ret.getIndex(), __isIndexNegated != __isBaseNegated));
                __ret.setIndex(considerNegation(__graph, __tmp, __isIndexNegated != __isBaseNegated));
                return true;
            }
        }

        return improveNegation(__graph, __ret, __isBaseNegated, __isIndexNegated);
    }

    private boolean improveNegation(StructuredGraph __graph, AMD64AddressNode __ret, boolean __originalBaseNegated, boolean __originalIndexNegated)
    {
        boolean __baseNegated = __originalBaseNegated;
        boolean __indexNegated = __originalIndexNegated;

        ValueNode __originalBase = __ret.getBase();
        ValueNode __originalIndex = __ret.getIndex();

        if (__ret.getBase() instanceof NegateNode)
        {
            NegateNode __negate = (NegateNode) __ret.getBase();
            __ret.setBase(__negate.getValue());
            __baseNegated = !__baseNegated;
        }

        if (__ret.getIndex() instanceof NegateNode)
        {
            NegateNode __negate = (NegateNode) __ret.getIndex();
            __ret.setIndex(__negate.getValue());
            __indexNegated = !__indexNegated;
        }

        if (__baseNegated != __originalBaseNegated || __indexNegated != __originalIndexNegated)
        {
            ValueNode __base = __ret.getBase();
            ValueNode __index = __ret.getIndex();

            boolean __improved = improve(__graph, __ret, __baseNegated, __indexNegated);
            if (__baseNegated != __originalBaseNegated)
            {
                if (__base == __ret.getBase())
                {
                    __ret.setBase(__originalBase);
                }
                else if (__ret.getBase() != null)
                {
                    __ret.setBase(__graph.maybeAddOrUnique(NegateNode.create(__ret.getBase(), NodeView.DEFAULT)));
                }
            }

            if (__indexNegated != __originalIndexNegated)
            {
                if (__index == __ret.getIndex())
                {
                    __ret.setIndex(__originalIndex);
                }
                else if (__ret.getIndex() != null)
                {
                    __ret.setIndex(__graph.maybeAddOrUnique(NegateNode.create(__ret.getIndex(), NodeView.DEFAULT)));
                }
            }
            return __improved;
        }
        return false;
    }

    private static ValueNode considerNegation(StructuredGraph __graph, ValueNode __value, boolean __negate)
    {
        if (__negate && __value != null)
        {
            return __graph.maybeAddOrUnique(NegateNode.create(__value, NodeView.DEFAULT));
        }
        return __value;
    }

    private static ValueNode improveInput(AMD64AddressNode __address, ValueNode __node, int __shift, boolean __negateExtractedDisplacement)
    {
        if (__node == null)
        {
            return null;
        }

        JavaConstant __c = __node.asJavaConstant();
        if (__c != null)
        {
            return improveConstDisp(__address, __node, __c, null, __shift, __negateExtractedDisplacement);
        }
        else
        {
            if (__node.stamp(NodeView.DEFAULT) instanceof IntegerStamp)
            {
                /*
                 * we can't swallow zero-extends because of multiple reasons:
                 *
                 * a) we might encounter something like the following: ZeroExtend(Add(negativeValue,
                 * positiveValue)). if we swallow the zero-extend in this case and subsequently
                 * optimize the add, we might end up with a negative value that has less than 64
                 * bits in base or index. such a value would require sign extension instead of
                 * zero-extension but the backend can only do (implicit) zero-extension by using a
                 * larger register (e.g. rax instead of eax).
                 *
                 * b) our backend does not guarantee that the upper half of a 64-bit register equals
                 * 0 if a 32-bit value is stored in there.
                 *
                 * c) we also can't swallow zero-extends with less than 32 bits as most of these
                 * values are immediately sign-extended to 32 bit by the backend (therefore, the
                 * subsequent implicit zero-extension to 64 bit won't do what we expect).
                 */

                if (__node instanceof AddNode)
                {
                    AddNode __add = (AddNode) __node;
                    if (__add.getX().isConstant())
                    {
                        return improveConstDisp(__address, __node, __add.getX().asJavaConstant(), __add.getY(), __shift, __negateExtractedDisplacement);
                    }
                    else if (__add.getY().isConstant())
                    {
                        return improveConstDisp(__address, __node, __add.getY().asJavaConstant(), __add.getX(), __shift, __negateExtractedDisplacement);
                    }
                }
            }
        }

        return __node;
    }

    private static ValueNode improveConstDisp(AMD64AddressNode __address, ValueNode __original, JavaConstant __c, ValueNode __other, int __shift, boolean __negateExtractedDisplacement)
    {
        if (__c.getJavaKind().isNumericInteger())
        {
            long __delta = __c.asLong() << __shift;
            if (updateDisplacement(__address, __delta, __negateExtractedDisplacement))
            {
                return __other;
            }
        }
        return __original;
    }

    protected static boolean updateDisplacement(AMD64AddressNode __address, long __displacementDelta, boolean __negateDelta)
    {
        long __sign = __negateDelta ? -1 : 1;
        long __disp = __address.getDisplacement() + __displacementDelta * __sign;
        if (NumUtil.isInt(__disp))
        {
            __address.setDisplacement((int) __disp);
            return true;
        }
        return false;
    }
}
