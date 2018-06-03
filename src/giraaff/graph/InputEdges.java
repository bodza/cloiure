package giraaff.graph;

import java.util.ArrayList;

import giraaff.graph.Edges.Type;
import giraaff.graph.NodeClass.InputInfo;
import giraaff.nodeinfo.InputType;

// @class InputEdges
public final class InputEdges extends Edges
{
    // @field
    private final InputType[] ___inputTypes;
    // @field
    private final boolean[] ___isOptional;

    // @cons
    public InputEdges(int __directCount, ArrayList<InputInfo> __edges)
    {
        super(Type.Inputs, __directCount, __edges);

        this.___inputTypes = new InputType[__edges.size()];
        this.___isOptional = new boolean[__edges.size()];
        for (int __i = 0; __i < __edges.size(); __i++)
        {
            this.___inputTypes[__i] = __edges.get(__i).___inputType;
            this.___isOptional[__i] = __edges.get(__i).___optional;
        }
    }

    public static void translateInto(InputEdges __inputs, ArrayList<InputInfo> __infos)
    {
        for (int __index = 0; __index < __inputs.getCount(); __index++)
        {
            __infos.add(new InputInfo(__inputs.___offsets[__index], __inputs.getName(__index), __inputs.getType(__index), __inputs.getDeclaringClass(__index), __inputs.___inputTypes[__index], __inputs.isOptional(__index)));
        }
    }

    public InputType getInputType(int __index)
    {
        return this.___inputTypes[__index];
    }

    public boolean isOptional(int __index)
    {
        return this.___isOptional[__index];
    }

    @Override
    public void update(Node __node, Node __oldValue, Node __newValue)
    {
        __node.updateUsages(__oldValue, __newValue);
    }
}
