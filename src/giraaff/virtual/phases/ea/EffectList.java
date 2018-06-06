package giraaff.virtual.phases.ea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import giraaff.graph.Node;
import giraaff.nodes.StructuredGraph;
import giraaff.util.GraalError;

///
// An {@link EffectList} can be used to maintain a list of {@link EffectList.Effect}s and backtrack
// to a previous state by truncating the list.
///
// @class EffectList
public class EffectList implements Iterable<EffectList.Effect>
{
    // @iface EffectList.Effect
    public interface Effect
    {
        default boolean isVisible()
        {
            return true;
        }

        default boolean isCfgKill()
        {
            return false;
        }

        void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes);
    }

    // @iface EffectList.SimpleEffect
    public interface SimpleEffect extends EffectList.Effect
    {
        @Override
        default void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes)
        {
            apply(__graph);
        }

        void apply(StructuredGraph __graph);
    }

    // @def
    private static final EffectList.Effect[] EMPTY_ARRAY = new EffectList.Effect[0];
    // @def
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    // @field
    private EffectList.Effect[] ___effects = EMPTY_ARRAY;
    // @field
    private int ___size;

    // @cons EffectList
    public EffectList()
    {
        super();
    }

    private void enlarge(int __elements)
    {
        int __length = this.___effects.length;
        if (this.___size + __elements > __length)
        {
            while (this.___size + __elements > __length)
            {
                __length = Math.max(__length * 2, 4);
            }
            this.___effects = Arrays.copyOf(this.___effects, __length);
        }
    }

    public void add(String __name, EffectList.SimpleEffect __effect)
    {
        add(__name, (EffectList.Effect) __effect);
    }

    public void add(String __name, EffectList.Effect __effect)
    {
        enlarge(1);
        this.___effects[this.___size++] = __effect;
    }

    public void addAll(EffectList __list)
    {
        enlarge(__list.___size);
        System.arraycopy(__list.___effects, 0, this.___effects, this.___size, __list.___size);
        this.___size += __list.___size;
    }

    public void insertAll(EffectList __list, int __position)
    {
        enlarge(__list.___size);
        System.arraycopy(this.___effects, __position, this.___effects, __position + __list.___size, this.___size - __position);
        System.arraycopy(__list.___effects, 0, this.___effects, __position, __list.___size);
        this.___size += __list.___size;
    }

    public int checkpoint()
    {
        return this.___size;
    }

    public int size()
    {
        return this.___size;
    }

    public void backtrack(int __checkpoint)
    {
        this.___size = __checkpoint;
    }

    @Override
    public Iterator<EffectList.Effect> iterator()
    {
        // @closure
        return new Iterator<EffectList.Effect>()
        {
            // @field
            int ___index;
            // @field
            final int ___listSize = EffectList.this.___size;

            @Override
            public boolean hasNext()
            {
                return this.___index < this.___listSize;
            }

            @Override
            public EffectList.Effect next()
            {
                return EffectList.this.___effects[this.___index++];
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    public EffectList.Effect get(int __index)
    {
        if (__index >= this.___size)
        {
            throw new IndexOutOfBoundsException();
        }
        return this.___effects[__index];
    }

    public void clear()
    {
        this.___size = 0;
    }

    public boolean isEmpty()
    {
        return this.___size == 0;
    }

    public void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes, boolean __cfgKills)
    {
        boolean __message = false;
        for (int __i = 0; __i < size(); __i++)
        {
            EffectList.Effect __effect = this.___effects[__i];
            if (__effect.isCfgKill() == __cfgKills)
            {
                if (!__message)
                {
                    __message = true;
                }
                try
                {
                    __effect.apply(__graph, __obsoleteNodes);
                }
                catch (Throwable __t)
                {
                    throw new GraalError(__t);
                }
            }
        }
    }

    private String getName(int __i)
    {
        return "";
    }
}
