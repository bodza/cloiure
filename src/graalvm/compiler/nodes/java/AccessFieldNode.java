package graalvm.compiler.nodes.java;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

import jdk.vm.ci.meta.ResolvedJavaField;

/**
 * The base class of all instructions that access fields.
 */
public abstract class AccessFieldNode extends FixedWithNextNode implements Lowerable
{
    public static final NodeClass<AccessFieldNode> TYPE = NodeClass.create(AccessFieldNode.class);
    @OptionalInput ValueNode object;

    protected final ResolvedJavaField field;

    public ValueNode object()
    {
        return object;
    }

    /**
     * Constructs a new access field object.
     *
     * @param object the instruction producing the receiver object
     * @param field the compiler interface representation of the field
     */
    public AccessFieldNode(NodeClass<? extends AccessFieldNode> c, Stamp stamp, ValueNode object, ResolvedJavaField field)
    {
        super(c, stamp);
        this.object = object;
        this.field = field;
    }

    /**
     * Gets the compiler interface field for this field access.
     *
     * @return the compiler interface field for this field access
     */
    public ResolvedJavaField field()
    {
        return field;
    }

    /**
     * Checks whether this field access is an access to a static field.
     *
     * @return {@code true} if this field access is to a static field
     */
    public boolean isStatic()
    {
        return field.isStatic();
    }

    /**
     * Checks whether this field is declared volatile.
     *
     * @return {@code true} if the field is resolved and declared volatile
     */
    public boolean isVolatile()
    {
        return field.isVolatile();
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Name)
        {
            return super.toString(verbosity) + "#" + field.getName();
        }
        else
        {
            return super.toString(verbosity);
        }
    }
}
