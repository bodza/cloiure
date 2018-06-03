package giraaff.virtual.phases.ea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import giraaff.graph.Node;
import giraaff.nodes.StructuredGraph;
import giraaff.util.GraalError;

/**
 * An {@link EffectList} can be used to maintain a list of {@link Effect}s and backtrack to a
 * previous state by truncating the list.
 */
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

        void apply(StructuredGraph graph, ArrayList<Node> obsoleteNodes);
    }

    // @iface EffectList.SimpleEffect
    public interface SimpleEffect extends Effect
    {
        @Override
        default void apply(StructuredGraph graph, ArrayList<Node> obsoleteNodes)
        {
            apply(graph);
        }

        void apply(StructuredGraph graph);
    }

    // @def
    private static final Effect[] EMPTY_ARRAY = new Effect[0];
    // @def
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    // @field
    private Effect[] effects = EMPTY_ARRAY;
    // @field
    private int size;

    // @cons
    public EffectList()
    {
        super();
    }

    private void enlarge(int __elements)
    {
        int __length = effects.length;
        if (size + __elements > __length)
        {
            while (size + __elements > __length)
            {
                __length = Math.max(__length * 2, 4);
            }
            effects = Arrays.copyOf(effects, __length);
        }
    }

    public void add(String __name, SimpleEffect __effect)
    {
        add(__name, (Effect) __effect);
    }

    public void add(String __name, Effect __effect)
    {
        enlarge(1);
        effects[size++] = __effect;
    }

    public void addAll(EffectList __list)
    {
        enlarge(__list.size);
        System.arraycopy(__list.effects, 0, effects, size, __list.size);
        size += __list.size;
    }

    public void insertAll(EffectList __list, int __position)
    {
        enlarge(__list.size);
        System.arraycopy(effects, __position, effects, __position + __list.size, size - __position);
        System.arraycopy(__list.effects, 0, effects, __position, __list.size);
        size += __list.size;
    }

    public int checkpoint()
    {
        return size;
    }

    public int size()
    {
        return size;
    }

    public void backtrack(int __checkpoint)
    {
        size = __checkpoint;
    }

    @Override
    public Iterator<Effect> iterator()
    {
        // @closure
        return new Iterator<Effect>()
        {
            // @field
            int index;
            // @field
            final int listSize = EffectList.this.size;

            @Override
            public boolean hasNext()
            {
                return index < listSize;
            }

            @Override
            public Effect next()
            {
                return effects[index++];
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Effect get(int __index)
    {
        if (__index >= size)
        {
            throw new IndexOutOfBoundsException();
        }
        return effects[__index];
    }

    public void clear()
    {
        size = 0;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    public void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes, boolean __cfgKills)
    {
        boolean __message = false;
        for (int __i = 0; __i < size(); __i++)
        {
            Effect __effect = effects[__i];
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
