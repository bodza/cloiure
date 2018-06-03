package giraaff.nodes.java;

import java.util.Objects;

import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNegationNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.UnaryOpLogicNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.extended.AnchoringNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;

///
// The {@code InstanceOfNode} represents an instanceof test.
///
// @class InstanceOfNode
public final class InstanceOfNode extends UnaryOpLogicNode implements Lowerable, Virtualizable
{
    // @def
    public static final NodeClass<InstanceOfNode> TYPE = NodeClass.create(InstanceOfNode.class);

    // @field
    private ObjectStamp ___checkedStamp;

    // @field
    private JavaTypeProfile ___profile;
    @OptionalInput(InputType.Anchor)
    // @field
    protected AnchoringNode ___anchor;

    // @cons
    private InstanceOfNode(ObjectStamp __checkedStamp, ValueNode __object, JavaTypeProfile __profile, AnchoringNode __anchor)
    {
        this(TYPE, __checkedStamp, __object, __profile, __anchor);
    }

    // @cons
    protected InstanceOfNode(NodeClass<? extends InstanceOfNode> __c, ObjectStamp __checkedStamp, ValueNode __object, JavaTypeProfile __profile, AnchoringNode __anchor)
    {
        super(__c, __object);
        this.___checkedStamp = __checkedStamp;
        this.___profile = __profile;
        this.___anchor = __anchor;
    }

    public static LogicNode createAllowNull(TypeReference __type, ValueNode __object, JavaTypeProfile __profile, AnchoringNode __anchor)
    {
        if (StampTool.isPointerNonNull(__object))
        {
            return create(__type, __object, __profile, __anchor);
        }
        return createHelper(StampFactory.object(__type), __object, __profile, __anchor);
    }

    public static LogicNode create(TypeReference __type, ValueNode __object)
    {
        return create(__type, __object, null, null);
    }

    public static LogicNode create(TypeReference __type, ValueNode __object, JavaTypeProfile __profile, AnchoringNode __anchor)
    {
        return createHelper(StampFactory.objectNonNull(__type), __object, __profile, __anchor);
    }

    public static LogicNode createHelper(ObjectStamp __checkedStamp, ValueNode __object, JavaTypeProfile __profile, AnchoringNode __anchor)
    {
        LogicNode __synonym = findSynonym(__checkedStamp, __object, NodeView.DEFAULT);
        if (__synonym != null)
        {
            return __synonym;
        }
        else
        {
            return new InstanceOfNode(__checkedStamp, __object, __profile, __anchor);
        }
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        NodeView __view = NodeView.from(__tool);
        LogicNode __synonym = findSynonym(this.___checkedStamp, __forValue, __view);
        if (__synonym != null)
        {
            return __synonym;
        }
        else
        {
            return this;
        }
    }

    public static LogicNode findSynonym(ObjectStamp __checkedStamp, ValueNode __object, NodeView __view)
    {
        ObjectStamp __inputStamp = (ObjectStamp) __object.stamp(__view);
        ObjectStamp __joinedStamp = (ObjectStamp) __checkedStamp.join(__inputStamp);

        if (__joinedStamp.isEmpty())
        {
            // The check can never succeed, the intersection of the two stamps is empty.
            return LogicConstantNode.contradiction();
        }
        else
        {
            ObjectStamp __meetStamp = (ObjectStamp) __checkedStamp.meet(__inputStamp);
            if (__checkedStamp.equals(__meetStamp))
            {
                // The check will always succeed, the union of the two stamps is equal to the checked stamp.
                return LogicConstantNode.tautology();
            }
            else if (__checkedStamp.alwaysNull())
            {
                return IsNullNode.create(__object);
            }
            else if (Objects.equals(__checkedStamp.type(), __meetStamp.type()) && __checkedStamp.isExactType() == __meetStamp.isExactType() && __checkedStamp.alwaysNull() == __meetStamp.alwaysNull())
            {
                // The only difference makes the null-ness of the value => simplify the check.
                if (__checkedStamp.nonNull())
                {
                    return LogicNegationNode.create(IsNullNode.create(__object));
                }
                else
                {
                    return IsNullNode.create(__object);
                }
            }
        }
        return null;
    }

    ///
    // Gets the type being tested.
    ///
    public TypeReference type()
    {
        return StampTool.typeReferenceOrNull(this.___checkedStamp);
    }

    public JavaTypeProfile profile()
    {
        return this.___profile;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(getValue());
        TriState __fold = tryFold(__alias.stamp(NodeView.DEFAULT));
        if (__fold != TriState.UNKNOWN)
        {
            __tool.replaceWithValue(LogicConstantNode.forBoolean(__fold.isTrue(), graph()));
        }
    }

    @Override
    public Stamp getSucceedingStampForValue(boolean __negated)
    {
        if (__negated)
        {
            return null;
        }
        else
        {
            return this.___checkedStamp;
        }
    }

    @Override
    public TriState tryFold(Stamp __valueStamp)
    {
        if (__valueStamp instanceof ObjectStamp)
        {
            ObjectStamp __inputStamp = (ObjectStamp) __valueStamp;
            ObjectStamp __joinedStamp = (ObjectStamp) this.___checkedStamp.join(__inputStamp);

            if (__joinedStamp.isEmpty())
            {
                // The check can never succeed, the intersection of the two stamps is empty.
                return TriState.FALSE;
            }
            else
            {
                ObjectStamp __meetStamp = (ObjectStamp) this.___checkedStamp.meet(__inputStamp);
                if (this.___checkedStamp.equals(__meetStamp))
                {
                    // The check will always succeed, the union of the two stamps is equal to the checked stamp.
                    return TriState.TRUE;
                }
            }
        }
        return TriState.UNKNOWN;
    }

    public boolean allowsNull()
    {
        return !this.___checkedStamp.nonNull();
    }

    public void setProfile(JavaTypeProfile __typeProfile, AnchoringNode __anchor)
    {
        this.___profile = __typeProfile;
        updateUsagesInterface(this.___anchor, __anchor);
        this.___anchor = __anchor;
    }

    public AnchoringNode getAnchor()
    {
        return this.___anchor;
    }

    public ObjectStamp getCheckedStamp()
    {
        return this.___checkedStamp;
    }

    public void strengthenCheckedStamp(ObjectStamp __newCheckedStamp)
    {
        this.___checkedStamp = __newCheckedStamp;
    }
}
