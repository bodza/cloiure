package giraaff.graph.iterators;

import giraaff.graph.Node;

// @class NodePredicates
public abstract class NodePredicates
{
    private static final TautologyPredicate TAUTOLOGY = new TautologyPredicate();
    private static final ContradictionPredicate CONTRADICTION = new ContradictionPredicate();
    private static final IsNullPredicate IS_NULL = new IsNullPredicate();

    public static NodePredicate alwaysTrue()
    {
        return TAUTOLOGY;
    }

    public static NodePredicate alwaysFalse()
    {
        return CONTRADICTION;
    }

    public static NodePredicate isNull()
    {
        return IS_NULL;
    }

    public static NegativeTypePredicate isNotA(Class<? extends Node> clazz)
    {
        return new NegativeTypePredicate(clazz);
    }

    public static PositiveTypePredicate isA(Class<? extends Node> clazz)
    {
        return new PositiveTypePredicate(clazz);
    }

    // @class NodePredicates.TautologyPredicate
    static final class TautologyPredicate implements NodePredicate
    {
        @Override
        public boolean apply(Node n)
        {
            return true;
        }

        @Override
        public NodePredicate and(NodePredicate np)
        {
            return np;
        }
    }

    // @class NodePredicates.ContradictionPredicate
    static final class ContradictionPredicate implements NodePredicate
    {
        @Override
        public boolean apply(Node n)
        {
            return false;
        }

        @Override
        public NodePredicate and(NodePredicate np)
        {
            return this;
        }
    }

    // @class NodePredicates.AndPredicate
    static final class AndPredicate implements NodePredicate
    {
        private final NodePredicate a;
        private final NodePredicate b;

        // @cons
        AndPredicate(NodePredicate a, NodePredicate b)
        {
            super();
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean apply(Node n)
        {
            return a.apply(n) && b.apply(n);
        }
    }

    // @class NodePredicates.NotPredicate
    static final class NotPredicate implements NodePredicate
    {
        private final NodePredicate a;

        // @cons
        NotPredicate(NodePredicate n)
        {
            super();
            this.a = n;
        }

        @Override
        public boolean apply(Node n)
        {
            return !a.apply(n);
        }

        @Override
        public NodePredicate negate()
        {
            return a;
        }
    }

    // @class NodePredicates.IsNullPredicate
    static final class IsNullPredicate implements NodePredicate
    {
        @Override
        public boolean apply(Node n)
        {
            return n == null;
        }
    }

    // @class NodePredicates.PositiveTypePredicate
    public static final class PositiveTypePredicate implements NodePredicate
    {
        private final Class<?> type;
        private PositiveTypePredicate or;

        // @cons
        PositiveTypePredicate(Class<?> type)
        {
            super();
            this.type = type;
        }

        // @cons
        public PositiveTypePredicate(NegativeTypePredicate a)
        {
            super();
            type = a.type;
            if (a.nor != null)
            {
                or = new PositiveTypePredicate(a.nor);
            }
        }

        @Override
        public boolean apply(Node n)
        {
            return type.isInstance(n) || (or != null && or.apply(n));
        }

        public PositiveTypePredicate or(Class<? extends Node> clazz)
        {
            if (or == null)
            {
                or = new PositiveTypePredicate(clazz);
            }
            else
            {
                or.or(clazz);
            }
            return this;
        }

        @Override
        public NodePredicate negate()
        {
            return new NegativeTypePredicate(this);
        }
    }

    // @class NodePredicates.NegativeTypePredicate
    public static final class NegativeTypePredicate implements NodePredicate
    {
        private final Class<?> type;
        private NegativeTypePredicate nor;

        // @cons
        NegativeTypePredicate(Class<?> type)
        {
            super();
            this.type = type;
        }

        // @cons
        public NegativeTypePredicate(PositiveTypePredicate a)
        {
            super();
            type = a.type;
            if (a.or != null)
            {
                nor = new NegativeTypePredicate(a.or);
            }
        }

        @Override
        public boolean apply(Node n)
        {
            return !type.isInstance(n) && (nor == null || nor.apply(n));
        }

        public NegativeTypePredicate nor(Class<? extends Node> clazz)
        {
            if (nor == null)
            {
                nor = new NegativeTypePredicate(clazz);
            }
            else
            {
                nor.nor(clazz);
            }
            return this;
        }

        @Override
        public NodePredicate negate()
        {
            return new PositiveTypePredicate(this);
        }
    }
}
