package giraaff.loop;

import giraaff.core.common.type.Stamp;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.util.GraalError;

///
// This class describes a value node that is an induction variable in a counted loop.
///
// @class InductionVariable
public abstract class InductionVariable
{
    // @enum InductionVariable.Direction
    public enum Direction
    {
        Up,
        Down;

        public Direction opposite()
        {
            switch (this)
            {
                case Up:
                    return Down;
                case Down:
                    return Up;
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    public abstract StructuredGraph graph();

    // @field
    protected final LoopEx ___loop;

    // @cons
    public InductionVariable(LoopEx __loop)
    {
        super();
        this.___loop = __loop;
    }

    public LoopEx getLoop()
    {
        return this.___loop;
    }

    public abstract Direction direction();

    ///
    // Returns the value node that is described by this induction variable.
    ///
    public abstract ValueNode valueNode();

    ///
    // Returns the node that gives the initial value of this induction variable.
    ///
    public abstract ValueNode initNode();

    ///
    // Returns the stride of the induction variable. The stride is the value that is added to the
    // induction variable at each iteration.
    ///
    public abstract ValueNode strideNode();

    public abstract boolean isConstantInit();

    public abstract boolean isConstantStride();

    public abstract long constantInit();

    public abstract long constantStride();

    ///
    // Returns the extremum value of the induction variable. The extremum value is the value of the
    // induction variable in the loop body of the last iteration, only taking into account the main
    // loop limit test. It's possible for the loop to exit before this value if
    // {@link CountedLoopInfo#isExactTripCount()} returns false for the containing loop.
    ///
    public ValueNode extremumNode()
    {
        return extremumNode(false, valueNode().stamp(NodeView.DEFAULT));
    }

    public abstract ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __stamp);

    public abstract boolean isConstantExtremum();

    public abstract long constantExtremum();

    ///
    // Returns the exit value of the induction variable. The exit value is the value of the
    // induction variable at the loop exit.
    ///
    public abstract ValueNode exitValueNode();

    ///
    // Deletes any nodes created within the scope of this object that have no usages.
    ///
    public abstract void deleteUnusedNodes();
}
