package graalvm.compiler.graph.iterators;

import java.util.Iterator;

import graalvm.compiler.graph.Node;

public class PredicatedProxyNodeIterator<T extends Node> extends NodeIterator<T> {

    private final Iterator<T> iterator;
    private final NodePredicate predicate;

    public PredicatedProxyNodeIterator(Iterator<T> iterator, NodePredicate predicate) {
        this.iterator = iterator;
        this.predicate = predicate;
    }

    @Override
    protected void forward() {
        while ((current == null || !current.isAlive() || !predicate.apply(current)) && iterator.hasNext()) {
            current = iterator.next();
        }
        if (current != null && (!current.isAlive() || !predicate.apply(current))) {
            current = null;
        }
    }
}
