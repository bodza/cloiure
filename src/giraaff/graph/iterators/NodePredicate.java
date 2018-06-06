package giraaff.graph.iterators;

import java.util.function.Predicate;

import giraaff.graph.Node;
import giraaff.graph.iterators.NodePredicates;

// @iface NodePredicate
public interface NodePredicate extends Predicate<Node>
{
    boolean apply(Node __n);

    @Override
    default boolean test(Node __n)
    {
        return apply(__n);
    }

    default NodePredicate and(NodePredicate __np)
    {
        return new NodePredicates.AndPredicate(this, __np);
    }
}
