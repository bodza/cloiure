package cloiure;

import cloiure.lang.Symbol;
import cloiure.lang.Var;
import cloiure.lang.RT;

public class main
{
    final static private Symbol CLOIURE_MAIN = Symbol.intern("cloiure.main");
    final static private Var REQUIRE = RT.var("cloiure.core", "require");
    final static private Var LEGACY_REPL = RT.var("cloiure.main", "legacy-repl");
    final static private Var LEGACY_SCRIPT = RT.var("cloiure.main", "legacy-script");
    final static private Var MAIN = RT.var("cloiure.main", "main");

    public static void legacy_repl(String[] args)
    {
        REQUIRE.invoke(CLOIURE_MAIN);
        LEGACY_REPL.invoke(RT.seq(args));
    }

    public static void legacy_script(String[] args)
    {
        REQUIRE.invoke(CLOIURE_MAIN);
        LEGACY_SCRIPT.invoke(RT.seq(args));
    }

    public static void main(String[] args)
    {
        REQUIRE.invoke(CLOIURE_MAIN);
        MAIN.applyTo(RT.seq(args));
    }
}
