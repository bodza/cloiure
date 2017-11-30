package cloiure.java.api;

import cloiure.lang.IFn;
import cloiure.lang.Symbol;
import cloiure.lang.Var;

/**
 * <p>The Cloiure class provides a minimal interface to bootstrap Cloiure access
 * from other JVM languages. It provides:</p>
 *
 * <ol>
 * <li>The ability to use Cloiure's namespaces to locate an arbitrary
 * <a href="http://clojure.org/vars">var</a>, returning the
 * var's {@link cloiure.lang.IFn} interface.</li>
 * <li>A convenience method <code>read</code> for reading data using
 * Cloiure's edn reader</li>
 * </ol>
 *
 * <p>To lookup and call a Cloiure function:</p>
 *
 * <pre>
 * IFn plus = Cloiure.var("cloiure.core", "+");
 * plus.invoke(1, 2);
 * </pre>
 *
 * <p>Functions in <code>cloiure.core</code> are automatically loaded. Other
 * namespaces can be loaded via <code>require</code>:</p>
 *
 * <pre>
 * IFn require = Cloiure.var("cloiure.core", "require");
 * require.invoke(Cloiure.read("cloiure.set"));
 * </pre>
 *
 * <p><code>IFn</code>s can be passed to higher order functions, e.g. the
 * example below passes <code>plus</code> to <code>read</code>:</p>
 *
 * <pre>
 * IFn map = Cloiure.var("cloiure.core", "map");
 * IFn inc = Cloiure.var("cloiure.core", "inc");
 * map.invoke(inc, Cloiure.read("[1 2 3]"));
 * </pre>
 */
public class Cloiure
{
    private Cloiure()
    {
    }

    private static Symbol asSym(Object o)
    {
        Symbol s;
        if (o instanceof String)
        {
            s = Symbol.intern((String) o);
        }
        else
        {
            s = (Symbol) o;
        }
        return s;
    }

    /**
     * Returns the var associated with qualifiedName.
     *
     * @param qualifiedName  a String or cloiure.lang.Symbol
     * @return               a cloiure.lang.IFn
     */
    public static IFn var(Object qualifiedName)
    {
        Symbol s = asSym(qualifiedName);
        return var(s.getNamespace(), s.getName());
    }

    /**
     * Returns an IFn associated with the namespace and name.
     *
     * @param ns        a String or cloiure.lang.Symbol
     * @param name      a String or cloiure.lang.Symbol
     * @return          a cloiure.lang.IFn
     */
    public static IFn var(Object ns, Object name)
    {
        return Var.intern(asSym(ns), asSym(name));
    }

    /**
     * Read one object from the String s.  Reads data in the
     * <a href="http://edn-format.org">edn format</a>.
     * @param s   a String
     * @return    an Object, or nil.
     */
    public static Object read(String s)
    {
        return EDN_READ_STRING.invoke(s);
    }

    static
    {
        Symbol edn = (Symbol) var("cloiure.core", "symbol").invoke("cloiure.edn");
        var("cloiure.core", "require").invoke(edn);
    }
    private static final IFn EDN_READ_STRING = var("cloiure.edn", "read-string");
}
