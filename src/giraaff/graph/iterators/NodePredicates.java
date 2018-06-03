package giraaff.graph.iterators;

import giraaff.graph.Node;

// @class NodePredicates
public abstract class NodePredicates
{
    // @def
    private static final TautologyPredicate TAUTOLOGY = new TautologyPredicate();
    // @def
    private static final ContradictionPredicate CONTRADICTION = new ContradictionPredicate();
    // @def
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

    public static NegativeTypePredicate isNotA(Class<? extends Node> __clazz)
    {
        return new NegativeTypePredicate(__clazz);
    }

    public static PositiveTypePredicate isA(Class<? extends Node> __clazz)
    {
        return new PositiveTypePredicate(__clazz);
    }

    // @class NodePredicates.TautologyPredicate
    static final class TautologyPredicate implements NodePredicate
    {
        @Override
        public boolean apply(Node __n)
        {
            return true;
        }

        @Override
        public NodePredicate and(NodePredicate __np)
        {
            return __np;
        }
    }

    // @class NodePredicates.ContradictionPredicate
    static final class ContradictionPredicate implements NodePredicate
    {
        @Override
        public boolean apply(Node __n)
        {
            return false;
        }

        @Override
        public NodePredicate and(NodePredicate __np)
        {
            return this;
        }
    }

    // @class NodePredicates.AndPredicate
    static final class AndPredicate implements NodePredicate
    {
        // @field
        private final NodePredicate a;
        // @field
        private final NodePredicate b;

        // @cons
        AndPredicate(NodePredicate __a, NodePredicate __b)
        {
            super();
            this.a = __a;
            this.b = __b;
        }

        @Override
        public boolean apply(Node __n)
        {
            return a.apply(__n) && b.apply(__n);
        }
    }

    // @class NodePredicates.NotPredicate
    static final class NotPredicate implements NodePredicate
    {
        // @field
        private final NodePredicate a;

        // @cons
        NotPredicate(NodePredicate __n)
        {
            super();
            this.a = __n;
        }

        @Override
        public boolean apply(Node __n)
        {
            return !a.apply(__n);
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
        public boolean apply(Node __n)
        {
            return __n == null;
        }
    }

    // @class NodePredicates.PositiveTypePredicate
    public static final class PositiveTypePredicate implements NodePredicate
    {
        // @field
        private final Class<?> type;
        // @field
        private PositiveTypePredicate or;

        // @cons
        PositiveTypePredicate(Class<?> __type)
        {
            super();
            this.type = __type;
        }

        // @cons
        public PositiveTypePredicate(NegativeTypePredicate __a)
        {
            super();
            type = __a.type;
            if (__a.nor != null)
            {
                or = new PositiveTypePredicate(__a.nor);
            }
        }

        @Override
        public boolean apply(Node __n)
        {
            return type.isInstance(__n) || (or != null && or.apply(__n));
        }

        public PositiveTypePredicate or(Class<? extends Node> __clazz)
        {
            if (or == null)
            {
                or = new PositiveTypePredicate(__clazz);
            }
            else
            {
                or.or(__clazz);
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
        // @field
        private final Class<?> type;
        // @field
        private NegativeTypePredicate nor;

        // @cons
        NegativeTypePredicate(Class<?> __type)
        {
            super();
            this.type = __type;
        }

        // @cons
        public NegativeTypePredicate(PositiveTypePredicate __a)
        {
            super();
            type = __a.type;
            if (__a.or != null)
            {
                nor = new NegativeTypePredicate(__a.or);
            }
        }

        @Override
        public boolean apply(Node __n)
        {
            return !type.isInstance(__n) && (nor == null || nor.apply(__n));
        }

        public NegativeTypePredicate nor(Class<? extends Node> __clazz)
        {
            if (nor == null)
            {
                nor = new NegativeTypePredicate(__clazz);
            }
            else
            {
                nor.nor(__clazz);
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
