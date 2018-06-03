package giraaff.replacements.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.Bytecode;
import giraaff.bytecode.BytecodeProvider;

/**
 * A {@link BytecodeProvider} that provides bytecode properties of a {@link ResolvedJavaMethod} as
 * parsed from a class file. This avoids all {@linkplain java.lang.instrument.Instrumentation
 * instrumentation} and any bytecode rewriting performed by the VM.
 *
 * This mechanism retrieves class files based on the name and {@link ClassLoader} of existing
 * {@link Class} instances. It bypasses all VM parsing and verification of the class file and
 * assumes the class files are well formed. As such, it should only be used for classes from a
 * trusted source such as the boot class (or module) path.
 *
 * A combination of {@link Class#forName(String)} and an existing {@link MetaAccessProvider} is used
 * to resolve constant pool references. This opens up the opportunity for linkage errors if the
 * referee is structurally changed through redefinition (e.g. a referred to method is renamed or
 * deleted). This will result in an appropriate {@link LinkageError} being thrown. The only way to
 * avoid this is to have a completely isolated {@code jdk.vm.ci.meta} implementation for parsing
 * snippet/intrinsic bytecodes.
 */
// @class ClassfileBytecodeProvider
public final class ClassfileBytecodeProvider implements BytecodeProvider
{
    // @field
    private final ClassLoader loader;
    // @field
    private final EconomicMap<Class<?>, Classfile> classfiles = EconomicMap.create(Equivalence.IDENTITY);
    // @field
    private final EconomicMap<String, Class<?>> classes = EconomicMap.create();
    // @field
    private final EconomicMap<ResolvedJavaType, FieldsCache> fields = EconomicMap.create();
    // @field
    private final EconomicMap<ResolvedJavaType, MethodsCache> methods = EconomicMap.create();
    // @field
    final MetaAccessProvider metaAccess;
    // @field
    final SnippetReflectionProvider snippetReflection;

    // @cons
    public ClassfileBytecodeProvider(MetaAccessProvider __metaAccess, SnippetReflectionProvider __snippetReflection)
    {
        super();
        this.metaAccess = __metaAccess;
        this.snippetReflection = __snippetReflection;
        ClassLoader __cl = getClass().getClassLoader();
        this.loader = __cl == null ? ClassLoader.getSystemClassLoader() : __cl;
    }

    // @cons
    public ClassfileBytecodeProvider(MetaAccessProvider __metaAccess, SnippetReflectionProvider __snippetReflection, ClassLoader __loader)
    {
        super();
        this.metaAccess = __metaAccess;
        this.snippetReflection = __snippetReflection;
        this.loader = __loader;
    }

    @Override
    public Bytecode getBytecode(ResolvedJavaMethod __method)
    {
        Classfile __classfile = getClassfile(resolveToClass(__method.getDeclaringClass().getName()));
        return __classfile.getCode(__method.getName(), __method.getSignature().toMethodDescriptor());
    }

    @Override
    public boolean supportsInvokedynamic()
    {
        return false;
    }

    @Override
    public boolean shouldRecordMethodDependencies()
    {
        return false;
    }

    /**
     * Gets the class file bytes for {@code c}.
     */
    private static InputStream getClassfileAsStream(Class<?> __c) throws IOException
    {
        return __c.getModule().getResourceAsStream(__c.getName().replace('.', '/') + ".class");
    }

    /**
     * Gets a {@link Classfile} created by parsing the class file bytes for {@code c}.
     *
     * @throws NoClassDefFoundError if the class file cannot be found
     */
    private synchronized Classfile getClassfile(Class<?> __c)
    {
        Classfile __classfile = classfiles.get(__c);
        if (__classfile == null)
        {
            try
            {
                ResolvedJavaType __type = metaAccess.lookupJavaType(__c);
                InputStream __in = getClassfileAsStream(__c);
                if (__in != null)
                {
                    DataInputStream __stream = new DataInputStream(__in);
                    __classfile = new Classfile(__type, __stream, this);
                    classfiles.put(__c, __classfile);
                    return __classfile;
                }
                throw new NoClassDefFoundError(__c.getName());
            }
            catch (IOException __e)
            {
                throw (NoClassDefFoundError) new NoClassDefFoundError(__c.getName()).initCause(__e);
            }
        }
        return __classfile;
    }

    synchronized Class<?> resolveToClass(String __descriptor)
    {
        Class<?> __c = classes.get(__descriptor);
        if (__c == null)
        {
            if (__descriptor.length() == 1)
            {
                __c = JavaKind.fromPrimitiveOrVoidTypeChar(__descriptor.charAt(0)).toJavaClass();
            }
            else
            {
                int __dimensions = 0;
                while (__descriptor.charAt(__dimensions) == '[')
                {
                    __dimensions++;
                }
                String __name;
                if (__dimensions == 0 && __descriptor.startsWith("L") && __descriptor.endsWith(";"))
                {
                    __name = __descriptor.substring(1, __descriptor.length() - 1).replace('/', '.');
                }
                else
                {
                    __name = __descriptor.replace('/', '.');
                }
                try
                {
                    __c = Class.forName(__name, true, loader);
                    classes.put(__descriptor, __c);
                }
                catch (ClassNotFoundException __e)
                {
                    throw new NoClassDefFoundError(__descriptor);
                }
            }
        }
        return __c;
    }

    /**
     * Name and type of a field.
     */
    // @class ClassfileBytecodeProvider.FieldKey
    static final class FieldKey
    {
        // @field
        final String name;
        // @field
        final String type;

        // @cons
        FieldKey(String __name, String __type)
        {
            super();
            this.name = __name;
            this.type = __type;
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (__obj instanceof FieldKey)
            {
                FieldKey __that = (FieldKey) __obj;
                return __that.name.equals(this.name) && __that.type.equals(this.type);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return name.hashCode() ^ type.hashCode();
        }
    }

    /**
     * Name and descriptor of a method.
     */
    // @class ClassfileBytecodeProvider.MethodKey
    static final class MethodKey
    {
        // @field
        final String name;
        // @field
        final String descriptor;

        // @cons
        MethodKey(String __name, String __descriptor)
        {
            super();
            this.name = __name;
            this.descriptor = __descriptor;
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (__obj instanceof MethodKey)
            {
                MethodKey __that = (MethodKey) __obj;
                return __that.name.equals(this.name) && __that.descriptor.equals(this.descriptor);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return name.hashCode() ^ descriptor.hashCode();
        }
    }

    /**
     * Method cache for a {@link ResolvedJavaType}.
     */
    // @class ClassfileBytecodeProvider.MethodsCache
    static final class MethodsCache
    {
        // @field
        volatile EconomicMap<MethodKey, ResolvedJavaMethod> constructors;
        // @field
        volatile EconomicMap<MethodKey, ResolvedJavaMethod> methods;

        ResolvedJavaMethod lookup(ResolvedJavaType __type, String __name, String __descriptor)
        {
            MethodKey __key = new MethodKey(__name, __descriptor);

            if (__name.equals("<clinit>"))
            {
                // no need to cache <clinit> as it will be looked up at most once
                return __type.getClassInitializer();
            }
            if (!__name.equals("<init>"))
            {
                if (methods == null)
                {
                    // racy initialization is safe since 'methods' is volatile
                    methods = createMethodMap(__type.getDeclaredMethods());
                }

                return methods.get(__key);
            }
            else
            {
                if (constructors == null)
                {
                    // racy initialization is safe since instanceFields is volatile
                    constructors = createMethodMap(__type.getDeclaredConstructors());
                }
                return constructors.get(__key);
            }
        }

        private static EconomicMap<MethodKey, ResolvedJavaMethod> createMethodMap(ResolvedJavaMethod[] __methodArray)
        {
            EconomicMap<MethodKey, ResolvedJavaMethod> __map = EconomicMap.create();
            for (ResolvedJavaMethod __m : __methodArray)
            {
                __map.put(new MethodKey(__m.getName(), __m.getSignature().toMethodDescriptor()), __m);
            }
            return __map;
        }
    }

    /**
     * Field cache for a {@link ResolvedJavaType}.
     */
    // @class ClassfileBytecodeProvider.FieldsCache
    static final class FieldsCache
    {
        // @field
        volatile EconomicMap<FieldKey, ResolvedJavaField> instanceFields;
        // @field
        volatile EconomicMap<FieldKey, ResolvedJavaField> staticFields;

        ResolvedJavaField lookup(ResolvedJavaType __type, String __name, String __fieldType, boolean __isStatic)
        {
            FieldKey __key = new FieldKey(__name, __fieldType);

            if (__isStatic)
            {
                if (staticFields == null)
                {
                    // racy initialization is safe since staticFields is volatile
                    staticFields = createFieldMap(__type.getStaticFields());
                }
                return staticFields.get(__key);
            }
            else
            {
                if (instanceFields == null)
                {
                    // racy initialization is safe since instanceFields is volatile
                    instanceFields = createFieldMap(__type.getInstanceFields(false));
                }
                return instanceFields.get(__key);
            }
        }

        private static EconomicMap<FieldKey, ResolvedJavaField> createFieldMap(ResolvedJavaField[] __fieldArray)
        {
            EconomicMap<FieldKey, ResolvedJavaField> __map = EconomicMap.create();
            for (ResolvedJavaField __f : __fieldArray)
            {
                __map.put(new FieldKey(__f.getName(), __f.getType().getName()), __f);
            }
            return __map;
        }
    }

    /**
     * Gets the methods cache for {@code type}.
     *
     * Synchronized since the cache is lazily created.
     */
    private synchronized MethodsCache getMethods(ResolvedJavaType __type)
    {
        MethodsCache __methodsCache = methods.get(__type);
        if (__methodsCache == null)
        {
            __methodsCache = new MethodsCache();
            methods.put(__type, __methodsCache);
        }
        return __methodsCache;
    }

    /**
     * Gets the fields cache for {@code type}.
     *
     * Synchronized since the cache is lazily created.
     */
    private synchronized FieldsCache getFields(ResolvedJavaType __type)
    {
        FieldsCache __fieldsCache = fields.get(__type);
        if (__fieldsCache == null)
        {
            __fieldsCache = new FieldsCache();
            fields.put(__type, __fieldsCache);
        }
        return __fieldsCache;
    }

    ResolvedJavaField findField(ResolvedJavaType __type, String __name, String __fieldType, boolean __isStatic)
    {
        return getFields(__type).lookup(__type, __name, __fieldType, __isStatic);
    }

    ResolvedJavaMethod findMethod(ResolvedJavaType __type, String __name, String __descriptor, boolean __isStatic)
    {
        ResolvedJavaMethod __method = getMethods(__type).lookup(__type, __name, __descriptor);
        if (__method != null && __method.isStatic() == __isStatic)
        {
            return __method;
        }
        return null;
    }
}
