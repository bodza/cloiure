package giraaff.graph.iterators;

import java.util.function.Predicate;

import giraaff.graph.Node;
import giraaff.graph.iterators.NodePredicates.AndPredicate;

public interface NodePredicate extends Predicate<Node>
{
    boolean apply(Node n);

    @Override
    default boolean test(Node n)
    {
        return apply(n);
    }

    default NodePredicate and(NodePredicate np)
    {
        return new AndPredicate(this, np);
    }
}
