package giraaff.lir.dfa;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collection;

import giraaff.core.common.cfg.AbstractBlockBase;

/**
 * Ensures that an element is only in the worklist once.
 *
 */
class UniqueWorkList extends ArrayDeque<AbstractBlockBase<?>>
{
    BitSet valid;

    UniqueWorkList(int size)
    {
        this.valid = new BitSet(size);
    }

    @Override
    public AbstractBlockBase<?> poll()
    {
        AbstractBlockBase<?> result = super.poll();
        if (result != null)
        {
            valid.set(result.getId(), false);
        }
        return result;
    }

    @Override
    public boolean add(AbstractBlockBase<?> pred)
    {
        if (!valid.get(pred.getId()))
        {
            valid.set(pred.getId(), true);
            return super.add(pred);
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends AbstractBlockBase<?>> collection)
    {
        boolean changed = false;
        for (AbstractBlockBase<?> element : collection)
        {
            if (!valid.get(element.getId()))
            {
                valid.set(element.getId(), true);
                super.add(element);
                changed = true;
            }
        }
        return changed;
    }
}
