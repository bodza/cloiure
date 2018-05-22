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

/**
 * The {@code InstanceOfNode} represents an instanceof test.
 */
public class InstanceOfNode extends UnaryOpLogicNode implements Lowerable, Virtualizable
{
    public static final NodeClass<InstanceOfNode> TYPE = NodeClass.create(InstanceOfNode.class);

    private ObjectStamp checkedStamp;

    private JavaTypeProfile profile;
    @OptionalInput(InputType.Anchor) protected AnchoringNode anchor;

    private InstanceOfNode(ObjectStamp checkedStamp, ValueNode object, JavaTypeProfile profile, AnchoringNode anchor)
    {
        this(TYPE, checkedStamp, object, profile, anchor);
    }

    protected InstanceOfNode(NodeClass<? extends InstanceOfNode> c, ObjectStamp checkedStamp, ValueNode object, JavaTypeProfile profile, AnchoringNode anchor)
    {
        super(c, object);
        this.checkedStamp = checkedStamp;
        this.profile = profile;
        this.anchor = anchor;
    }

    public static LogicNode createAllowNull(TypeReference type, ValueNode object, JavaTypeProfile profile, AnchoringNode anchor)
    {
        if (StampTool.isPointerNonNull(object))
        {
            return create(type, object, profile, anchor);
        }
        return createHelper(StampFactory.object(type), object, profile, anchor);
    }

    public static LogicNode create(TypeReference type, ValueNode object)
    {
        return create(type, object, null, null);
    }

    public static LogicNode create(TypeReference type, ValueNode object, JavaTypeProfile profile, AnchoringNode anchor)
    {
        return createHelper(StampFactory.objectNonNull(type), object, profile, anchor);
    }

    public static LogicNode createHelper(ObjectStamp checkedStamp, ValueNode object, JavaTypeProfile profile, AnchoringNode anchor)
    {
        LogicNode synonym = findSynonym(checkedStamp, object, NodeView.DEFAULT);
        if (synonym != null)
        {
            return synonym;
        }
        else
        {
            return new InstanceOfNode(checkedStamp, object, profile, anchor);
        }
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        NodeView view = NodeView.from(tool);
        LogicNode synonym = findSynonym(checkedStamp, forValue, view);
        if (synonym != null)
        {
            return synonym;
        }
        else
        {
            return this;
        }
    }

    public static LogicNode findSynonym(ObjectStamp checkedStamp, ValueNode object, NodeView view)
    {
        ObjectStamp inputStamp = (ObjectStamp) object.stamp(view);
        ObjectStamp joinedStamp = (ObjectStamp) checkedStamp.join(inputStamp);

        if (joinedStamp.isEmpty())
        {
            // The check can never succeed, the intersection of the two stamps is empty.
            return LogicConstantNode.contradiction();
        }
        else
        {
            ObjectStamp meetStamp = (ObjectStamp) checkedStamp.meet(inputStamp);
            if (checkedStamp.equals(meetStamp))
            {
                // The check will always succeed, the union of the two stamps is equal to the
                // checked stamp.
                return LogicConstantNode.tautology();
            }
            else if (checkedStamp.alwaysNull())
            {
                return IsNullNode.create(object);
            }
            else if (Objects.equals(checkedStamp.type(), meetStamp.type()) && checkedStamp.isExactType() == meetStamp.isExactType() && checkedStamp.alwaysNull() == meetStamp.alwaysNull())
            {
                // The only difference makes the null-ness of the value => simplify the check.
                if (checkedStamp.nonNull())
                {
                    return LogicNegationNode.create(IsNullNode.create(object));
                }
                else
                {
                    return IsNullNode.create(object);
                }
            }
        }
        return null;
    }

    /**
     * Gets the type being tested.
     */
    public TypeReference type()
    {
        return StampTool.typeReferenceOrNull(checkedStamp);
    }

    public JavaTypeProfile profile()
    {
        return profile;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(getValue());
        TriState fold = tryFold(alias.stamp(NodeView.DEFAULT));
        if (fold != TriState.UNKNOWN)
        {
            tool.replaceWithValue(LogicConstantNode.forBoolean(fold.isTrue(), graph()));
        }
    }

    @Override
    public Stamp getSucceedingStampForValue(boolean negated)
    {
        if (negated)
        {
            return null;
        }
        else
        {
            return checkedStamp;
        }
    }

    @Override
    public TriState tryFold(Stamp valueStamp)
    {
        if (valueStamp instanceof ObjectStamp)
        {
            ObjectStamp inputStamp = (ObjectStamp) valueStamp;
            ObjectStamp joinedStamp = (ObjectStamp) checkedStamp.join(inputStamp);

            if (joinedStamp.isEmpty())
            {
                // The check can never succeed, the intersection of the two stamps is empty.
                return TriState.FALSE;
            }
            else
            {
                ObjectStamp meetStamp = (ObjectStamp) checkedStamp.meet(inputStamp);
                if (checkedStamp.equals(meetStamp))
                {
                    // The check will always succeed, the union of the two stamps is equal to the
                    // checked stamp.
                    return TriState.TRUE;
                }
            }
        }
        return TriState.UNKNOWN;
    }

    public boolean allowsNull()
    {
        return !checkedStamp.nonNull();
    }

    public void setProfile(JavaTypeProfile typeProfile, AnchoringNode anchor)
    {
        this.profile = typeProfile;
        updateUsagesInterface(this.anchor, anchor);
        this.anchor = anchor;
    }

    public AnchoringNode getAnchor()
    {
        return anchor;
    }

    public ObjectStamp getCheckedStamp()
    {
        return checkedStamp;
    }

    public void strengthenCheckedStamp(ObjectStamp newCheckedStamp)
    {
        this.checkedStamp = newCheckedStamp;
    }
}
