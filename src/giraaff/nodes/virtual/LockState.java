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
    // @field
    public final MonitorIdNode monitorId;
    // @field
    public final LockState next;

    // @cons
    public LockState(MonitorIdNode __monitorId, LockState __next)
    {
        super();
        this.monitorId = __monitorId;
        this.next = __next;
    }

    public static List<MonitorIdNode> asList(LockState __state)
    {
        if (__state == null)
        {
            return Collections.emptyList();
        }
        else
        {
            ArrayList<MonitorIdNode> __result = new ArrayList<>();
            LockState __a = __state;
            do
            {
                __result.add(__a.monitorId);
                __a = __a.next;
            } while (__a != null);
            return __result;
        }
    }
}
