package giraaff.nodes.java;

import jdk.vm.ci.meta.ResolvedJavaField;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// The base class of all instructions that access fields.
///
// @class AccessFieldNode
public abstract class AccessFieldNode extends FixedWithNextNode implements Lowerable
{
    // @def
    public static final NodeClass<AccessFieldNode> TYPE = NodeClass.create(AccessFieldNode.class);

    @Node.OptionalInput
    // @field
    ValueNode ___object;

    // @field
    protected final ResolvedJavaField ___field;

    public ValueNode object()
    {
        return this.___object;
    }

    ///
    // Constructs a new access field object.
    //
    // @param object the instruction producing the receiver object
    // @param field the compiler interface representation of the field
    ///
    // @cons AccessFieldNode
    public AccessFieldNode(NodeClass<? extends AccessFieldNode> __c, Stamp __stamp, ValueNode __object, ResolvedJavaField __field)
    {
        super(__c, __stamp);
        this.___object = __object;
        this.___field = __field;
    }

    ///
    // Gets the compiler interface field for this field access.
    //
    // @return the compiler interface field for this field access
    ///
    public ResolvedJavaField field()
    {
        return this.___field;
    }

    ///
    // Checks whether this field access is an access to a static field.
    //
    // @return {@code true} if this field access is to a static field
    ///
    public boolean isStatic()
    {
        return this.___field.isStatic();
    }

    ///
    // Checks whether this field is declared volatile.
    //
    // @return {@code true} if the field is resolved and declared volatile
    ///
    public boolean isVolatile()
    {
        return this.___field.isVolatile();
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }
}
