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
        return mapAndJoin(Arrays.asList(inputs), mapper, delimiter);
    }

    /**
     * Applies {@code mapper} on the elements in {@code inputs}, and joins them together separated
     * by {@code delimiter}.
     *
     * @return a new String that is composed from {@code inputs}.
     */
    public static <T, R> String mapAndJoin(Iterable<T> inputs, Function<? super T, ? extends R> mapper, String delimiter)
    {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (T t : inputs)
        {
            sb.append(sep).append(mapper.apply(t));
            sep = delimiter;
        }
        return sb.toString();
    }
}
