package giraaff.nodes.spi;

import giraaff.nodes.extended.GuardingNode;

///
// This interface marks nodes whose result is the same as one of their inputs. Such nodes are used
// to add type information, to introduce scheduling restrictions, etc.
//
// For some algorithms it is necessary or advantageous to see through these proxies.
///
// @iface ValueProxy
public interface ValueProxy extends LimitedValueProxy
{
    GuardingNode getGuard();
}
