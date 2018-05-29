package giraaff.virtual.phases.ea;

import java.lang.reflect.Field;
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

    private static final Effect[] EMPTY_ARRAY = new Effect[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private Effect[] effects = EMPTY_ARRAY;
    private int size;

    // @cons
    public EffectList()
    {
        super();
    }

    private void enlarge(int elements)
    {
        int length = effects.length;
        if (size + elements > length)
        {
            while (size + elements > length)
            {
                length = Math.max(length * 2, 4);
            }
            effects = Arrays.copyOf(effects, length);
        }
    }

    public void add(String name, SimpleEffect effect)
    {
        add(name, (Effect) effect);
    }

    public void add(String name, Effect effect)
    {
        enlarge(1);
        effects[size++] = effect;
    }

    public void addAll(EffectList list)
    {
        enlarge(list.size);
        System.arraycopy(list.effects, 0, effects, size, list.size);
        size += list.size;
    }

    public void insertAll(EffectList list, int position)
    {
        enlarge(list.size);
        System.arraycopy(effects, position, effects, position + list.size, size - position);
        System.arraycopy(list.effects, 0, effects, position, list.size);
        size += list.size;
    }

    public int checkpoint()
    {
        return size;
    }

    public int size()
    {
        return size;
    }

    public void backtrack(int checkpoint)
    {
        size = checkpoint;
    }

    @Override
    public Iterator<Effect> iterator()
    {
        return new Iterator<Effect>()
        {
            int index;
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

    public Effect get(int index)
    {
        if (index >= size)
        {
            throw new IndexOutOfBoundsException();
        }
        return effects[index];
    }

    public void clear()
    {
        size = 0;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    public void apply(StructuredGraph graph, ArrayList<Node> obsoleteNodes, boolean cfgKills)
    {
        boolean message = false;
        for (int i = 0; i < size(); i++)
        {
            Effect effect = effects[i];
            if (effect.isCfgKill() == cfgKills)
            {
                if (!message)
                {
                    message = true;
                }
                try
                {
                    effect.apply(graph, obsoleteNodes);
                }
                catch (Throwable t)
                {
                    StringBuilder sb = new StringBuilder();
                    toString(sb, i);
                    throw new GraalError(t).addContext("effect", sb);
                }
            }
        }
    }

    private void toString(StringBuilder sb, int i)
    {
        Effect effect = effects[i];
        sb.append(getName(i)).append(" [");
        boolean first = true;
        for (Field field : effect.getClass().getDeclaredFields())
        {
            try
            {
                field.setAccessible(true);
                Object object = field.get(effect);
                if (object == this)
                {
                    // Inner classes could capture the EffectList itself.
                    continue;
                }
                sb.append(first ? "" : ", ").append(field.getName()).append("=").append(format(object));
                first = false;
            }
            catch (SecurityException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
        sb.append(']');
    }

    private static String format(Object object)
    {
        if (object != null && Object[].class.isAssignableFrom(object.getClass()))
        {
            return Arrays.toString((Object[]) object);
        }
        return "" + object;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++)
        {
            Effect effect = get(i);
            if (effect.isVisible())
            {
                toString(sb, i);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String getName(int i)
    {
        return "";
    }
}
