package giraaff.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * This class contains utility methods for commonly used functional patterns for collections.
 */
public final class CollectionsUtil
{
    private CollectionsUtil()
    {
    }

    /**
     * Returns whether all elements in {@code inputs} match {@code predicate}. May not evaluate
     * {@code predicate} on all elements if not necessary for determining the result. If
     * {@code inputs} is empty then {@code true} is returned and {@code predicate} is not evaluated.
     *
     * @return {@code true} if either all elements in {@code inputs} match {@code predicate} or
     *         {@code inputs} is empty, otherwise {@code false}.
     */
    public static <T> boolean allMatch(T[] inputs, Predicate<T> predicate)
    {
        return allMatch(Arrays.asList(inputs), predicate);
    }

    /**
     * Returns whether all elements in {@code inputs} match {@code predicate}. May not evaluate
     * {@code predicate} on all elements if not necessary for determining the result. If
     * {@code inputs} is empty then {@code true} is returned and {@code predicate} is not evaluated.
     *
     * @return {@code true} if either all elements in {@code inputs} match {@code predicate} or
     *         {@code inputs} is empty, otherwise {@code false}.
     */
    public static <T> boolean allMatch(Iterable<T> inputs, Predicate<T> predicate)
    {
        for (T t : inputs)
        {
            if (!predicate.test(t))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Filters {@code inputs} with {@code predicate}, applies {@code mapper} and adds them in the
     * array provided by {@code arrayGenerator}.
     *
     * @return the array provided by {@code arrayGenerator}.
     */
    public static <T, R> R[] filterAndMapToArray(T[] inputs, Predicate<? super T> predicate, Function<? super T, ? extends R> mapper, IntFunction<R[]> arrayGenerator)
    {
        List<R> resultList = new ArrayList<>();
        for (T t : inputs)
        {
            if (predicate.test(t))
            {
                resultList.add(mapper.apply(t));
            }
        }
        return resultList.toArray(arrayGenerator.apply(resultList.size()));
    }

    /**
     * Applies {@code mapper} on the elements in {@code inputs}, and joins them together separated
     * by {@code delimiter}.
     *
     * @return a new String that is composed from {@code inputs}.
     */
    public static <T, R> String mapAndJoin(T[] inputs, Function<? super T, ? extends R> mapper, String delimiter)
    {
        return mapAndJoin(Arrays.asList(inputs), mapper, delimiter, "", "");
    }

    /**
     * Applies {@code mapper} on the elements in {@code inputs}, and joins them together separated
     * by {@code delimiter} and starting with {@code prefix}.
     *
     * @return a new String that is composed from {@code inputs}.
     */
    public static <T, R> String mapAndJoin(T[] inputs, Function<? super T, ? extends R> mapper, String delimiter, String prefix)
    {
        return mapAndJoin(Arrays.asList(inputs), mapper, delimiter, prefix, "");
    }

    /**
     * Applies {@code mapper} on the elements in {@code inputs}, and joins them together separated
     * by {@code delimiter} and starting with {@code prefix} and ending with {@code suffix}.
     *
     * @return a new String that is composed from {@code inputs}.
     */
    public static <T, R> String mapAndJoin(T[] inputs, Function<? super T, ? extends R> mapper, String delimiter, String prefix, String suffix)
    {
        return mapAndJoin(Arrays.asList(inputs), mapper, delimiter, prefix, suffix);
    }

    /**
     * Applies {@code mapper} on the elements in {@code inputs}, and joins them together separated
     * by {@code delimiter}.
     *
     * @return a new String that is composed from {@code inputs}.
     */
    public static <T, R> String mapAndJoin(Iterable<T> inputs, Function<? super T, ? extends R> mapper, String delimiter)
    {
        return mapAndJoin(inputs, mapper, delimiter, "", "");
    }

    /**
     * Applies {@code mapper} on the elements in {@code inputs}, and joins them together separated
     * by {@code delimiter} and starting with {@code prefix}.
     *
     * @return a new String that is composed from {@code inputs}.
     */
    public static <T, R> String mapAndJoin(Iterable<T> inputs, Function<? super T, ? extends R> mapper, String delimiter, String prefix)
    {
        return mapAndJoin(inputs, mapper, delimiter, prefix, "");
    }

    /**
     * Applies {@code mapper} on the elements in {@code inputs}, and joins them together separated
     * by {@code delimiter} and starting with {@code prefix} and ending with {@code suffix}.
     *
     * @return a new String that is composed from {@code inputs}.
     */
    public static <T, R> String mapAndJoin(Iterable<T> inputs, Function<? super T, ? extends R> mapper, String delimiter, String prefix, String suffix)
    {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (T t : inputs)
        {
            sb.append(sep).append(prefix).append(mapper.apply(t)).append(suffix);
            sep = delimiter;
        }
        return sb.toString();
    }
}
