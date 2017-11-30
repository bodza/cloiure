package cloiure.lang;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;

// Compiles libs and generates class files stored within the directory
// named by the Java System property "cloiure.compile.path". Arguments are
// strings naming the libs to be compiled. The libs and compile-path must
// all be within CLASSPATH.

public class Compile
{
    private static final String PATH_PROP = "cloiure.compile.path";
    private static final String REFLECTION_WARNING_PROP = "cloiure.compile.warn-on-reflection";
    private static final String UNCHECKED_MATH_PROP = "cloiure.compile.unchecked-math";

    private static final Var compile_path = RT.var("cloiure.core", "*compile-path*");
    private static final Var compile = RT.var("cloiure.core", "compile");
    private static final Var warn_on_reflection = RT.var("cloiure.core", "*warn-on-reflection*");
    private static final Var unchecked_math = RT.var("cloiure.core", "*unchecked-math*");

    public static void main(String[] args) throws IOException, ClassNotFoundException
    {
        OutputStreamWriter out = (OutputStreamWriter) RT.OUT.deref();
        PrintWriter err = RT.errPrintWriter();
        String path = System.getProperty(PATH_PROP);
        int count = args.length;

        if (path == null)
        {
            err.println("ERROR: Must set system property " + PATH_PROP +
                    "\nto the location for compiled .class files." +
                    "\nThis directory must also be on your CLASSPATH.");
            System.exit(1);
        }

        boolean warnOnReflection = System.getProperty(REFLECTION_WARNING_PROP, "false").equals("true");
        String uncheckedMathProp = System.getProperty(UNCHECKED_MATH_PROP);
        Object uncheckedMath = Boolean.FALSE;
        if ("true".equals(uncheckedMathProp))
            uncheckedMath = Boolean.TRUE;
        else if ("warn-on-boxed".equals(uncheckedMathProp))
            uncheckedMath = Keyword.intern("warn-on-boxed");

        // force load to avoid transitive compilation during lazy load
        RT.load("cloiure/core/specs/alpha");

        try
        {
            Var.pushThreadBindings(RT.map(compile_path, path, warn_on_reflection, warnOnReflection, unchecked_math, uncheckedMath));

            for (String lib : args)
            {
                out.write("Compiling " + lib + " to " + path + "\n");
                out.flush();
                compile.invoke(Symbol.intern(lib));
            }
        }
        finally
        {
            Var.popThreadBindings();
            try
            {
                out.flush();
            }
            catch (IOException e)
            {
                e.printStackTrace(err);
            }
        }
    }
}
