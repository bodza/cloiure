package giraaff.graph.iterators;

import giraaff.graph.Node;

// @class NodePredicates
public abstract class NodePredicates
{
    // @def
    private static final NodePredicates.TautologyPredicate TAUTOLOGY = new NodePredicates.TautologyPredicate();
    // @def
    private static final NodePredicates.ContradictionPredicate CONTRADICTION = new NodePredicates.ContradictionPredicate();
    // @def
    private static final NodePredicates.IsNullPredicate IS_NULL = new NodePredicates.IsNullPredicate();

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

    public static NodePredicates.NegativeTypePredicate isNotA(Class<? extends Node> __clazz)
    {
        return new NodePredicates.NegativeTypePredicate(__clazz);
    }

    public static NodePredicates.PositiveTypePredicate isA(Class<? extends Node> __clazz)
    {
        return new NodePredicates.PositiveTypePredicate(__clazz);
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
        private final NodePredicate ___a;
        // @field
        private final NodePredicate ___b;

        // @cons NodePredicates.AndPredicate
        AndPredicate(NodePredicate __a, NodePredicate __b)
        {
            super();
            this.___a = __a;
            this.___b = __b;
        }

        @Override
        public boolean apply(Node __n)
        {
            return this.___a.apply(__n) && this.___b.apply(__n);
        }
    }

    // @class NodePredicates.NotPredicate
    static final class NotPredicate implements NodePredicate
    {
        // @field
        private final NodePredicate ___a;

        // @cons NodePredicates.NotPredicate
        NotPredicate(NodePredicate __n)
        {
            super();
            this.___a = __n;
        }

        @Override
        public boolean apply(Node __n)
        {
            return !this.___a.apply(__n);
        }

        @Override
        public NodePredicate negate()
        {
            return this.___a;
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
        private final Class<?> ___type;
        // @field
        private NodePredicates.PositiveTypePredicate ___or;

        // @cons NodePredicates.PositiveTypePredicate
        PositiveTypePredicate(Class<?> __type)
        {
            super();
            this.___type = __type;
        }

        // @cons NodePredicates.PositiveTypePredicate
        public PositiveTypePredicate(NodePredicates.NegativeTypePredicate __a)
        {
            super();
            this.___type = __a.___type;
            if (__a.___nor != null)
            {
                this.___or = new NodePredicates.PositiveTypePredicate(__a.___nor);
            }
        }

        @Override
        public boolean apply(Node __n)
        {
            return this.___type.isInstance(__n) || (this.___or != null && this.___or.apply(__n));
        }

        public NodePredicates.PositiveTypePredicate or(Class<? extends Node> __clazz)
        {
            if (this.___or == null)
            {
                this.___or = new NodePredicates.PositiveTypePredicate(__clazz);
            }
            else
            {
                this.___or.or(__clazz);
            }
            return this;
        }

        @Override
        public NodePredicate negate()
        {
            return new NodePredicates.NegativeTypePredicate(this);
        }
    }

    // @class NodePredicates.NegativeTypePredicate
    public static final class NegativeTypePredicate implements NodePredicate
    {
        // @field
        private final Class<?> ___type;
        // @field
        private NodePredicates.NegativeTypePredicate ___nor;

        // @cons NodePredicates.NegativeTypePredicate
        NegativeTypePredicate(Class<?> __type)
        {
            super();
            this.___type = __type;
        }

        // @cons NodePredicates.NegativeTypePredicate
        public NegativeTypePredicate(NodePredicates.PositiveTypePredicate __a)
        {
            super();
            this.___type = __a.___type;
            if (__a.___or != null)
            {
                this.___nor = new NodePredicates.NegativeTypePredicate(__a.___or);
            }
        }

        @Override
        public boolean apply(Node __n)
        {
            return !this.___type.isInstance(__n) && (this.___nor == null || this.___nor.apply(__n));
        }

        public NodePredicates.NegativeTypePredicate nor(Class<? extends Node> __clazz)
        {
            if (this.___nor == null)
            {
                this.___nor = new NodePredicates.NegativeTypePredicate(__clazz);
            }
            else
            {
                this.___nor.nor(__clazz);
            }
            return this;
        }

        @Override
        public NodePredicate negate()
        {
            return new NodePredicates.PositiveTypePredicate(this);
        }
    }
}
