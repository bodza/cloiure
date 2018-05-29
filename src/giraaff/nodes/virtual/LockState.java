package giraaff.nodes.virtual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import giraaff.nodes.java.MonitorIdNode;

/**
 * The class implements a simple linked list of MonitorIdNodes, which can be used to describe the
 * current lock state of an object.
 */
// @class LockState
public final class LockState
{
    public final MonitorIdNode monitorId;
    public final LockState next;

    // @cons
    public LockState(MonitorIdNode monitorId, LockState next)
    {
        super();
        this.monitorId = monitorId;
        this.next = next;
    }

    @Override
    public String toString()
    {
        return monitorId.getLockDepth() + (next == null ? "" : "," + next);
    }

    public static List<MonitorIdNode> asList(LockState state)
    {
        if (state == null)
        {
            return Collections.emptyList();
        }
        else
        {
            ArrayList<MonitorIdNode> result = new ArrayList<>();
            LockState a = state;
            do
            {
                result.add(a.monitorId);
                a = a.next;
            } while (a != null);
            return result;
        }
    }
}
