(ns #_cloiure.slang cloiure.cradle
    (:refer-clojure :exclude [when when-not]))

(defmacro § [& _])
(defmacro ß [& _])

(defmacro def-
    ([s] `(def ~(vary-meta s assoc :private true)))
    ([s i] `(def ~(vary-meta s assoc :private true) ~i)))

(defmacro any
    ([f x y] `(~f ~x ~y))
    ([f x y & z] `(let [f# ~f x# ~x _# (any f# x# ~y)] (if _# _# (any f# x# ~@z)))))
(defn =?
    ([x y] (if (sequential? x) (if (seq x) (or (=? (first x) y) (recur (rest x) y)) false) (if (sequential? y) (recur y x) (= x y))))
    ([x y & z] (=? x (cons y z))))

(letfn [(w' [w] (if (= '=> (first w)) (rest w) (cons nil w)))]
    (defmacro     when       [y & w] (let [[_ & w] (w' w)]            `(if     ~y (do ~@w) ~_)))
    (defmacro     when-not   [y & w] (let [[_ & w] (w' w)]            `(if-not ~y (do ~@w) ~_)))
    (defmacro let-when     [x y & w] (let [[_ & w] (w' w)] `(let [~@x] (if     ~y (do ~@w) ~_))))
    (defmacro let-when-not [x y & w] (let [[_ & w] (w' w)] `(let [~@x] (if-not ~y (do ~@w) ~_)))))

(letfn [(z' [z] (cond (vector? z) `((recur ~@z)) (some? z) `((recur ~z))))
        (w' [w] (if (= '=> (first w)) (rest w) (cons nil w)))
        (l' [x y z w] (let [x (cond (vector? x) x (symbol? x) [x x] :else [`_# x]) z (z' z) [_ & w] (w' w)] `(loop [~@x] (if ~y (do ~@w ~@z) ~_))))]
    (defmacro loop-when [x y & w] (l' x y nil w))
    (defmacro loop-when-recur [x y z & w] (l' x y z w)))

(letfn [(z' [z] (cond (vector? z) `(recur ~@z) (some? z) `(recur ~z)))
        (w' [w] (if (= '=> (first w)) (second w)))]
    (defmacro recur-if [y z & w] (let [z (z' z) _ (w' w)] `(if ~y ~z ~_))))

(defmacro cond-let [x y & w]
    (let [x (if (vector? x) x [`_# x]) z (when (seq w) `(cond-let ~@w))]
        `(if-let ~x ~y ~z)))

(defn third [x] (first (next (next x))))
(defn fourth [x] (first (next (next (next x)))))

(defmacro update! [x f & z] `(set! ~x (~f ~x ~@z)))

(def % rem)
(def & bit-and)
(def | bit-or)
(def << bit-shift-left)
(def >> bit-shift-right)
(def >>> unsigned-bit-shift-right)

(defmacro java-ns [name & _] #_(ensure symbol? name) `(do ~@_))
(defmacro class-ns [name & _] #_(ensure symbol? name) `(do ~@_))

#_(ns cloiure.cradle
    (:refer-clojure :exclude [when when-not])
    (:use [cloiure slang]))

(import
    [java.io Reader]
  #_[java.lang Character Class Exception IllegalArgumentException IllegalStateException Integer Number Object RuntimeException String StringBuilder Throwable UnsupportedOperationException]
    [java.lang.reflect Constructor Field #_Method Modifier]
    [java.util Arrays HashMap HashSet IdentityHashMap Iterator Map Map$Entry Set TreeMap]
    [java.util.regex Matcher Pattern]
    [clojure.lang AFn AFunction APersistentMap APersistentSet APersistentVector ArraySeq DynamicClassLoader PersistentList$EmptyList IFn ILookup ILookupSite ILookupThunk IMapEntry IMeta IObj IPersistentCollection IPersistentList IPersistentMap IPersistentSet IPersistentVector ISeq IType Keyword KeywordLookupSite LazySeq LineNumberingPushbackReader LispReader Namespace Numbers PersistentArrayMap PersistentHashSet PersistentList PersistentVector RestFn RT Symbol Tuple Util Var]
    [cloiure.asm ClassVisitor ClassWriter Label MethodVisitor Opcodes Type]
    [cloiure.asm.commons GeneratorAdapter Method]
)

(defn- ßsym  [x] (condp instance? x                      Keyword (.sym x)                          Var (.sym x)  (:sym x) ))
(defn- ßns   [x] (condp instance? x Symbol (namespace x) Keyword (namespace x)                     Var (.ns x)   (:ns x)  ))
(defn- ßname [x] (condp instance? x Symbol (name x)                            Namespace (.name x)               (:name x)))

(java-ns cloiure.lang.LispReader

(class-ns LispReader
    (defn #_"boolean" LispReader'isWhitespace [#_"int" ch]
        (or (Character/isWhitespace ch) (= ch \,))
    )

    (defn #_"void" LispReader'unread [#_"PushbackReader" r, #_"int" ch]
        (when-not (= ch -1)
            (.unread r, ch)
        )
        nil
    )
)
)

(java-ns cloiure.lang.Tuple

(class-ns Tuple
    (def #_"int" Tuple'MAX_SIZE 6)
)
)

(java-ns cloiure.lang.Reflector
    (§ soon definterface Reflector) (import [clojure.lang Reflector])
)

(java-ns cloiure.lang.Compiler
    (defprotocol Expr
        (#_"Object" Expr'''eval [#_"Expr" this])
        (#_"void" Expr'''emit [#_"Expr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen])
        (#_"boolean" Expr'''hasJavaClass [#_"Expr" this])
        (#_"Class" Expr'''getJavaClass [#_"Expr" this])
    )

    (defprotocol Assignable
        (#_"Object" Assignable'''evalAssign [#_"Assignable" this, #_"Expr" val])
        (#_"void" Assignable'''emitAssign [#_"Assignable" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Expr" val])
    )

    (defprotocol MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"MaybePrimitive" this])
        (#_"void" MaybePrimitive'''emitUnboxed [#_"MaybePrimitive" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen])
    )

    (defprotocol Literal
        (#_"Object" Literal'''literal [#_"Literal" this])
    )

    (defprotocol Untyped)

    (defprotocol Interop)

    (defprotocol IopMethod
        (#_"int" IopMethod'''numParams [#_"IopMethod" this])
        (#_"String" IopMethod'''getMethodName [#_"IopMethod" this])
        (#_"Type" IopMethod'''getReturnType [#_"IopMethod" this])
        (#_"Type[]" IopMethod'''getArgTypes [#_"IopMethod" this])
        (#_"void" IopMethod'''emit [#_"IopMethod" this, #_"IopObject" fn, #_"ClassVisitor" cv])
    )

    (defprotocol IopObject
        (#_"boolean" IopObject'''supportsMeta [#_"IopObject" this])
        (#_"void" IopObject'''emitStatics [#_"IopObject" this, #_"ClassVisitor" gen])
        (#_"void" IopObject'''emitMethods [#_"IopObject" this, #_"ClassVisitor" gen])
    )

    (defprotocol IParser
        (#_"Expr" IParser'''parse [#_"IParser" this, #_"Context" context, #_"ISeq" form])
    )

    (definterface Recur)
)

(java-ns cloiure.lang.Compiler
    (defrecord NilExpr            [] #_"Expr" #_"Literal")
    (defrecord BooleanExpr        [] #_"Expr" #_"Literal")
    (defrecord MonitorEnterExpr   [] #_"Expr" #_"Untyped")
    (defrecord MonitorExitExpr    [] #_"Expr" #_"Untyped")
    (defrecord AssignExpr         [] #_"Expr")
    (defrecord ImportExpr         [] #_"Expr")
    (defrecord EmptyExpr          [] #_"Expr")
    (defrecord ConstantExpr       [] #_"Expr" #_"Literal")
    (defrecord NumberExpr         [] #_"Expr" #_"Literal" #_"MaybePrimitive")
    (defrecord StringExpr         [] #_"Expr" #_"Literal")
    (defrecord KeywordExpr        [] #_"Expr" #_"Literal")
    (defrecord InstanceFieldExpr  [] #_"Expr" #_"MaybePrimitive" #_"Assignable" #_"Interop")
    (defrecord StaticFieldExpr    [] #_"Expr" #_"MaybePrimitive" #_"Assignable" #_"Interop")
    (defrecord InstanceMethodExpr [] #_"Expr" #_"MaybePrimitive" #_"Interop")
    (defrecord StaticMethodExpr   [] #_"Expr" #_"MaybePrimitive" #_"Interop")
    (defrecord UnresolvedVarExpr  [] #_"Expr")
    (defrecord VarExpr            [] #_"Expr" #_"Assignable")
    (defrecord TheVarExpr         [] #_"Expr")
    (defrecord BodyExpr           [] #_"Expr" #_"MaybePrimitive")
    (defrecord CatchClause        [])
    (defrecord TryExpr            [] #_"Expr")
    (defrecord ThrowExpr          [] #_"Expr" #_"Untyped")
    (defrecord NewExpr            [] #_"Expr")
    (defrecord MetaExpr           [] #_"Expr")
    (defrecord IfExpr             [] #_"Expr" #_"MaybePrimitive")
    (defrecord ListExpr           [] #_"Expr")
    (defrecord MapExpr            [] #_"Expr")
    (defrecord SetExpr            [] #_"Expr")
    (defrecord VectorExpr         [] #_"Expr")
    (defrecord KeywordInvokeExpr  [] #_"Expr")
    (defrecord InstanceOfExpr     [] #_"Expr" #_"MaybePrimitive")
    (defrecord InvokeExpr         [] #_"Expr")
    (defrecord LocalBinding       [])
    (defrecord LocalBindingExpr   [] #_"Expr" #_"MaybePrimitive" #_"Assignable")
    (defrecord MethodParamExpr    [] #_"Expr" #_"MaybePrimitive")
    (defrecord FnMethod           [] #_"IopMethod")
    (defrecord FnExpr             [] #_"Expr" #_"IopObject")
    (defrecord DefExpr            [] #_"Expr")
    (defrecord BindingInit        [])
    (defrecord LetFnExpr          [] #_"Expr")
    (defrecord LetExpr            [] #_"Expr" #_"MaybePrimitive")
    (defrecord RecurExpr          [] #_"Expr" #_"MaybePrimitive")
    (defrecord NewInstanceMethod  [] #_"IopMethod")
    (defrecord NewInstanceExpr    [] #_"Expr" #_"IopObject")
    (defrecord CaseExpr           [] #_"Expr" #_"MaybePrimitive")
)

(java-ns cloiure.lang.Intrinsics

(class-ns Intrinsics
    (def #_"{String int|[int]}" Intrinsics'ops
        (hash-map
            "public static int clojure.lang.Numbers.shiftLeftInt(int,int)"                  Opcodes/ISHL
            "public static int clojure.lang.Numbers.shiftRightInt(int,int)"                 Opcodes/ISHR
            "public static int clojure.lang.Numbers.unsignedShiftRightInt(int,int)"         Opcodes/IUSHR
            "public static int clojure.lang.Numbers.unchecked_int_add(int,int)"             Opcodes/IADD
            "public static int clojure.lang.Numbers.unchecked_int_subtract(int,int)"        Opcodes/ISUB
            "public static int clojure.lang.Numbers.unchecked_int_negate(int)"              Opcodes/INEG
            "public static int clojure.lang.Numbers.unchecked_int_inc(int)"               [ Opcodes/ICONST_1 Opcodes/IADD ]
            "public static int clojure.lang.Numbers.unchecked_int_dec(int)"               [ Opcodes/ICONST_1 Opcodes/ISUB ]
            "public static int clojure.lang.Numbers.unchecked_int_multiply(int,int)"        Opcodes/IMUL
            "public static int clojure.lang.Numbers.unchecked_int_divide(int,int)"          Opcodes/IDIV
            "public static int clojure.lang.Numbers.unchecked_int_remainder(int,int)"       Opcodes/IREM

            "public static long clojure.lang.Numbers.and(long,long)"                        Opcodes/LAND
            "public static long clojure.lang.Numbers.or(long,long)"                         Opcodes/LOR
            "public static long clojure.lang.Numbers.xor(long,long)"                        Opcodes/LXOR
            "public static long clojure.lang.Numbers.shiftLeft(long,long)"                [ Opcodes/L2I Opcodes/LSHL ]
            "public static long clojure.lang.Numbers.shiftRight(long,long)"               [ Opcodes/L2I Opcodes/LSHR ]
            "public static long clojure.lang.Numbers.unsignedShiftRight(long,long)"       [ Opcodes/L2I Opcodes/LUSHR ]
            "public static long clojure.lang.Numbers.quotient(long,long)"                   Opcodes/LDIV
            "public static long clojure.lang.Numbers.remainder(long,long)"                  Opcodes/LREM
            "public static long clojure.lang.Numbers.unchecked_add(long,long)"              Opcodes/LADD
            "public static long clojure.lang.Numbers.unchecked_minus(long)"                 Opcodes/LNEG
            "public static long clojure.lang.Numbers.unchecked_minus(long,long)"            Opcodes/LSUB
            "public static long clojure.lang.Numbers.unchecked_multiply(long,long)"         Opcodes/LMUL
            "public static long clojure.lang.Numbers.unchecked_inc(long)"                 [ Opcodes/LCONST_1 Opcodes/LADD ]
            "public static long clojure.lang.Numbers.unchecked_dec(long)"                 [ Opcodes/LCONST_1 Opcodes/LSUB ]

            "public static double clojure.lang.Numbers.add(double,double)"                  Opcodes/DADD
            "public static double clojure.lang.Numbers.minus(double)"                       Opcodes/DNEG
            "public static double clojure.lang.Numbers.minus(double,double)"                Opcodes/DSUB
            "public static double clojure.lang.Numbers.multiply(double,double)"             Opcodes/DMUL
            "public static double clojure.lang.Numbers.divide(double,double)"               Opcodes/DDIV
            "public static double clojure.lang.Numbers.inc(double)"                       [ Opcodes/DCONST_1 Opcodes/DADD ]
            "public static double clojure.lang.Numbers.dec(double)"                       [ Opcodes/DCONST_1 Opcodes/DSUB ]
            "public static double clojure.lang.Numbers.unchecked_add(double,double)"        Opcodes/DADD
            "public static double clojure.lang.Numbers.unchecked_minus(double)"             Opcodes/DNEG
            "public static double clojure.lang.Numbers.unchecked_minus(double,double)"      Opcodes/DSUB
            "public static double clojure.lang.Numbers.unchecked_multiply(double,double)"   Opcodes/DMUL
            "public static double clojure.lang.Numbers.unchecked_inc(double)"             [ Opcodes/DCONST_1 Opcodes/DADD ]
            "public static double clojure.lang.Numbers.unchecked_dec(double)"             [ Opcodes/DCONST_1 Opcodes/DSUB ]

            "public static boolean clojure.lang.RT.aget_boolean(boolean[],int)"                  Opcodes/BALOAD
            "public static byte clojure.lang.RT.aget_byte(byte[],int)"                           Opcodes/BALOAD
            "public static short clojure.lang.RT.aget_short(short[],int)"                        Opcodes/SALOAD
            "public static char clojure.lang.RT.aget_char(char[],int)"                           Opcodes/CALOAD
            "public static int clojure.lang.RT.aget_int(int[],int)"                              Opcodes/IALOAD
            "public static long clojure.lang.RT.aget_long(long[],int)"                           Opcodes/LALOAD
            "public static float clojure.lang.RT.aget_float(float[],int)"                        Opcodes/FALOAD
            "public static double clojure.lang.RT.aget_double(double[],int)"                     Opcodes/DALOAD
            "public static java.lang.Object clojure.lang.RT.aget_object(java.lang.Object[],int)" Opcodes/AALOAD

            "public static int clojure.lang.RT.alength_boolean(boolean[])"         Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength_byte(byte[])"               Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength_short(short[])"             Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength_char(char[])"               Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength_int(int[])"                 Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength_long(long[])"               Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength_float(float[])"             Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength_double(double[])"           Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength_object(java.lang.Object[])" Opcodes/ARRAYLENGTH

            "public static long clojure.lang.RT.longCast(byte)"                Opcodes/I2L
            "public static long clojure.lang.RT.longCast(short)"               Opcodes/I2L
            "public static long clojure.lang.RT.longCast(int)"                 Opcodes/I2L
            "public static long clojure.lang.RT.longCast(long)"                Opcodes/NOP

            "public static double clojure.lang.RT.doubleCast(byte)"            Opcodes/I2D
            "public static double clojure.lang.RT.doubleCast(short)"           Opcodes/I2D
            "public static double clojure.lang.RT.doubleCast(int)"             Opcodes/I2D
            "public static double clojure.lang.RT.doubleCast(long)"            Opcodes/L2D
            "public static double clojure.lang.RT.doubleCast(float)"           Opcodes/F2D
            "public static double clojure.lang.RT.doubleCast(double)"          Opcodes/NOP

            "public static int clojure.lang.RT.uncheckedIntCast(byte)"         Opcodes/NOP
            "public static int clojure.lang.RT.uncheckedIntCast(short)"        Opcodes/NOP
            "public static int clojure.lang.RT.uncheckedIntCast(char)"         Opcodes/NOP
            "public static int clojure.lang.RT.uncheckedIntCast(int)"          Opcodes/NOP
            "public static int clojure.lang.RT.uncheckedIntCast(long)"         Opcodes/L2I
            "public static int clojure.lang.RT.uncheckedIntCast(float)"        Opcodes/F2I
            "public static int clojure.lang.RT.uncheckedIntCast(double)"       Opcodes/D2I

            "public static long clojure.lang.RT.uncheckedLongCast(byte)"       Opcodes/I2L
            "public static long clojure.lang.RT.uncheckedLongCast(short)"      Opcodes/I2L
            "public static long clojure.lang.RT.uncheckedLongCast(int)"        Opcodes/I2L
            "public static long clojure.lang.RT.uncheckedLongCast(long)"       Opcodes/NOP
            "public static long clojure.lang.RT.uncheckedLongCast(float)"      Opcodes/F2L
            "public static long clojure.lang.RT.uncheckedLongCast(double)"     Opcodes/D2L

            "public static double clojure.lang.RT.uncheckedDoubleCast(byte)"   Opcodes/I2D
            "public static double clojure.lang.RT.uncheckedDoubleCast(short)"  Opcodes/I2D
            "public static double clojure.lang.RT.uncheckedDoubleCast(int)"    Opcodes/I2D
            "public static double clojure.lang.RT.uncheckedDoubleCast(long)"   Opcodes/L2D
            "public static double clojure.lang.RT.uncheckedDoubleCast(float)"  Opcodes/F2D
            "public static double clojure.lang.RT.uncheckedDoubleCast(double)" Opcodes/NOP
        )
    )

    ;; map to instructions terminated with comparator for branch to false
    (def #_"{String [int]}" Intrinsics'preds
        (hash-map
            "public static boolean clojure.lang.Numbers.equiv(long,long)"     [ Opcodes/LCMP  Opcodes/IFNE ]
            "public static boolean clojure.lang.Numbers.equiv(double,double)" [ Opcodes/DCMPL Opcodes/IFNE ]
            "public static boolean clojure.lang.Numbers.lt(long,long)"        [ Opcodes/LCMP  Opcodes/IFGE ]
            "public static boolean clojure.lang.Numbers.lt(double,double)"    [ Opcodes/DCMPG Opcodes/IFGE ]
            "public static boolean clojure.lang.Numbers.lte(long,long)"       [ Opcodes/LCMP  Opcodes/IFGT ]
            "public static boolean clojure.lang.Numbers.lte(double,double)"   [ Opcodes/DCMPG Opcodes/IFGT ]
            "public static boolean clojure.lang.Numbers.gt(long,long)"        [ Opcodes/LCMP  Opcodes/IFLE ]
            "public static boolean clojure.lang.Numbers.gt(double,double)"    [ Opcodes/DCMPL Opcodes/IFLE ]
            "public static boolean clojure.lang.Numbers.gte(long,long)"       [ Opcodes/LCMP  Opcodes/IFLT ]
            "public static boolean clojure.lang.Numbers.gte(double,double)"   [ Opcodes/DCMPL Opcodes/IFLT ]

            "public static boolean clojure.lang.Util.equiv(long,long)"        [ Opcodes/LCMP  Opcodes/IFNE ]
            "public static boolean clojure.lang.Util.equiv(double,double)"    [ Opcodes/DCMPL Opcodes/IFNE ]
            "public static boolean clojure.lang.Util.equiv(boolean,boolean)"  [ Opcodes/IF_ICMPNE ]

            "public static boolean clojure.lang.Numbers.isZero(long)"         [ Opcodes/LCONST_0 Opcodes/LCMP  Opcodes/IFNE ]
            "public static boolean clojure.lang.Numbers.isZero(double)"       [ Opcodes/DCONST_0 Opcodes/DCMPL Opcodes/IFNE ]
            "public static boolean clojure.lang.Numbers.isPos(long)"          [ Opcodes/LCONST_0 Opcodes/LCMP  Opcodes/IFLE ]
            "public static boolean clojure.lang.Numbers.isPos(double)"        [ Opcodes/DCONST_0 Opcodes/DCMPL Opcodes/IFLE ]
            "public static boolean clojure.lang.Numbers.isNeg(long)"          [ Opcodes/LCONST_0 Opcodes/LCMP  Opcodes/IFGE ]
            "public static boolean clojure.lang.Numbers.isNeg(double)"        [ Opcodes/DCONST_0 Opcodes/DCMPG Opcodes/IFGE ]
        )
    )
)
)

(java-ns cloiure.lang.Reflector

(class-ns Reflector
    (defn #_"Class" Reflector'classOf [#_"Object" x]
        (when (some? x)
            (.getClass x)
        )
    )

    (defn #_"boolean" Reflector'isPrimitive [#_"Class" c]
        (and (some? c) (.isPrimitive c) (not (= c Void/TYPE)))
    )

    (defn- #_"Throwable" Reflector'getCauseOrElse [#_"Exception" e]
        (or (.getCause e) e)
    )

    (defn- #_"String" Reflector'noMethodReport [#_"String" methodName, #_"Object" target]
        (str "No matching method found: " methodName (when (some? target) (str " for " (.getClass target))))
    )

    (defn #_"Field" Reflector'getField [#_"Class" c, #_"String" name, #_"boolean" static?]
        (let [#_"Field[]" allfields (.getFields c)]
            (loop-when [#_"int" i 0] (< i (alength allfields))
                (let [#_"Field" f (aget allfields i)]
                    (if (and (= name (.getName f)) (= (Modifier/isStatic (.getModifiers f)) static?))
                        f
                        (recur (inc i))
                    )
                )
            )
        )
    )

    (defn #_"PersistentVector" Reflector'getMethods [#_"Class" c, #_"int" arity, #_"String" name, #_"boolean" static?]
        (let [matches- #(and (= name (.getName %)) (= (Modifier/isStatic (.getModifiers %)) static?) (= (alength (.getParameterTypes %)) arity))
              #_"java.lang.reflect.Method[]" allmethods (.getMethods c)
              [#_"PersistentVector" methods #_"PersistentVector" bridges]
                (loop-when [methods [] bridges [] #_"int" i 0] (< i (alength allmethods)) => [methods bridges]
                    (let [#_"java.lang.reflect.Method" m (aget allmethods i)
                          [methods bridges]
                            (when (matches- m) => [methods bridges]
                                (try
                                    (if (and (.isBridge m) (= (.getMethod c, (.getName m), (.getParameterTypes m)) m))
                                        [methods (conj bridges m)]
                                        [(conj methods m) bridges]
                                    )
                                    (catch NoSuchMethodException _
                                        [methods bridges]
                                    )
                                )
                            )]
                        (recur methods bridges (inc i))
                    )
                )
              methods
                (when (zero? (count methods)) => methods
                    (loop-when [methods methods #_"int" i 0] (< i (count bridges)) => methods
                        (recur (conj methods (nth bridges i)) (inc i))
                    )
                )
              methods
                (when (and (not static?) (.isInterface c)) => methods
                    (let [allmethods (.getMethods Object)]
                        (loop-when [methods methods #_"int" i 0] (< i (alength allmethods)) => methods
                            (let [#_"java.lang.reflect.Method" m (aget allmethods i)]
                                (recur (if (matches- m) (conj methods m) methods) (inc i))
                            )
                        )
                    )
                )]
            methods
        )
    )

    (defn #_"Object" Reflector'boxArg [#_"Class" c, #_"Object" arg]
        (let [unexpected! #(throw (IllegalArgumentException. (str "Unexpected param type, expected: " c ", given: " (.getName (.getClass arg)))))]
            (cond
                (not (.isPrimitive c)) (cast c arg)
                (= c Boolean/TYPE)     (cast Boolean arg)
                (= c Character/TYPE)   (cast Character arg)
                (number? arg)
                    (condp = c
                        Integer/TYPE   (.intValue arg)
                        Float/TYPE     (.floatValue arg)
                        Double/TYPE    (.doubleValue arg)
                        Long/TYPE      (.longValue arg)
                        Short/TYPE     (.shortValue arg)
                        Byte/TYPE      (.byteValue arg)
                        (unexpected!)
                    )
                :else
                    (unexpected!)
            )
        )
    )

    (defn #_"Object[]" Reflector'boxArgs [#_"Class[]" params, #_"Object[]" args]
        (when (pos? (alength params))
            (let [#_"Object[]" a (make-array Object (alength params))]
                (dotimes [#_"int" i (alength params)]
                    (aset a i (Reflector'boxArg (aget params i), (aget args i)))
                )
                a
            )
        )
    )

    (defn #_"boolean" Reflector'paramArgTypeMatch [#_"Class" paramType, #_"Class" argType]
        (cond
            (nil? argType)
                (not (.isPrimitive paramType))
            (or (= paramType argType) (.isAssignableFrom paramType, argType))
                true
            :else
                (condp = paramType
                    Integer/TYPE   (any = argType Integer Long/TYPE Long Short/TYPE Byte/TYPE)
                    Float/TYPE     (any = argType Float Double/TYPE)
                    Double/TYPE    (any = argType Double Float/TYPE)
                    Long/TYPE      (any = argType Long Integer/TYPE Short/TYPE Byte/TYPE)
                    Character/TYPE (= argType Character)
                    Short/TYPE     (= argType Short)
                    Byte/TYPE      (= argType Byte)
                    Boolean/TYPE   (= argType Boolean)
                                   false
                )
        )
    )

    (defn #_"boolean" Reflector'isCongruent [#_"Class[]" params, #_"Object[]" args]
        (when (some? args) => (zero? (alength params))
            (and (= (alength params) (alength args))
                (loop-when [#_"boolean" ? true #_"int" i 0] (and ? (< i (alength params)))
                    (let [#_"Object" arg (aget args i)]
                        (recur (Reflector'paramArgTypeMatch (aget params i), (when (some? arg) (.getClass arg))) (inc i))
                    )
                )
            )
        )
    )

    (defn #_"boolean" Reflector'isMatch [#_"java.lang.reflect.Method" lhs, #_"java.lang.reflect.Method" rhs]
        (and (= (.getName lhs), (.getName rhs)) (Modifier/isPublic (.getModifiers (.getDeclaringClass lhs)))
            (let [#_"Class[]" types1 (.getParameterTypes lhs) #_"Class[]" types2 (.getParameterTypes rhs)]
                (and (= (alength types1) (alength types2))
                    (loop-when [#_"int" i 0] (< i (alength types1)) => true
                        (and (.isAssignableFrom (aget types1 i), (aget types2 i))
                            (recur (inc i))
                        )
                    )
                )
            )
        )
    )

    (defn #_"java.lang.reflect.Method" Reflector'getAsMethodOfPublicBase [#_"Class" c, #_"java.lang.reflect.Method" m]
        (or
            (let [#_"Class[]" ifaces (.getInterfaces c)]
                (loop-when [#_"int" j 0] (< j (alength ifaces))
                    (let [#_"java.lang.reflect.Method[]" methods (.getMethods (aget ifaces j))]
                        (or
                            (loop-when [#_"int" i 0] (< i (alength methods))
                                (let-when [#_"java.lang.reflect.Method" im (aget methods i)] (Reflector'isMatch im, m) => (recur (inc i))
                                    im
                                )
                            )
                            (recur (inc j))
                        )
                    )
                )
            )
            (when-let [#_"Class" sc (.getSuperclass c)]
                (let [#_"java.lang.reflect.Method[]" methods (.getMethods sc)]
                    (loop-when [#_"int" i 0] (< i (alength methods)) => (Reflector'getAsMethodOfPublicBase sc, m)
                        (let-when [#_"java.lang.reflect.Method" scm (aget methods i)] (Reflector'isMatch scm, m) => (recur (inc i))
                            scm
                        )
                    )
                )
            )
        )
    )

    (defn #_"Object" Reflector'prepRet [#_"Class" c, #_"Object" x]
        (cond
            (not (or (.isPrimitive c) (= c Boolean))) x
            (instance? Boolean x)                     (if x true false)
            :else                                     x
        )
    )

    (defn #_"boolean" Reflector'subsumes [#_"Class[]" c1, #_"Class[]" c2]
        ;; presumes matching lengths
        (loop-when [#_"boolean" better false #_"int" i 0] (< i (alength c1)) => better
            (when-not (= (aget c1 i) (aget c2 i)) => (recur better (inc i))
                (and (or (and (not (.isPrimitive (aget c1 i))) (.isPrimitive (aget c2 i))) (.isAssignableFrom (aget c2 i), (aget c1 i)))
                    (recur true (inc i))
                )
            )
        )
    )

    (defn #_"Object" Reflector'invokeMatchingMethod [#_"String" methodName, #_"PersistentVector" methods, #_"Object" target, #_"Object[]" args]
        (let-when [#_"int" n (count methods)] (pos? n) => (throw (IllegalArgumentException. (Reflector'noMethodReport methodName, target)))
            (let [[#_"java.lang.reflect.Method" m #_"Object[]" boxedArgs]
                    (if (= n 1)
                        (let [m (nth methods 0)]
                            [m (Reflector'boxArgs (.getParameterTypes m), args)]
                        )
                        ;; overloaded w/same arity
                        (let [#_"Iterator" it (.iterator methods)]
                            (loop-when [#_"java.lang.reflect.Method" found nil boxedArgs nil] (.hasNext it) => [found boxedArgs]
                                (let [m (.next it) #_"Class[]" params (.getParameterTypes m)
                                    [found boxedArgs]
                                        (if (and (Reflector'isCongruent params, args) (or (nil? found) (Reflector'subsumes params, (.getParameterTypes found))))
                                            [m (Reflector'boxArgs params, args)]
                                            [found boxedArgs]
                                        )]
                                    (recur found boxedArgs)
                                )
                            )
                        )
                    )]
                (when (some? m) => (throw (IllegalArgumentException. (Reflector'noMethodReport methodName, target)))
                    (let [m (when-not (Modifier/isPublic (.getModifiers (.getDeclaringClass m))) => m
                                ;; public method of non-public class, try to find it in hierarchy
                                (or (Reflector'getAsMethodOfPublicBase (.getClass target), m)
                                    (throw (IllegalArgumentException. (str "Can't call public method of non-public class: " m)))
                                )
                            )]
                        (try
                            (Reflector'prepRet (.getReturnType m), (.invoke m, target, boxedArgs))
                            (catch Exception e
                                (throw (Reflector'getCauseOrElse e))
                            )
                        )
                    )
                )
            )
        )
    )

    (defn #_"Object" Reflector'invokeInstanceMethod [#_"Object" target, #_"String" methodName, #_"Object[]" args]
        (let [#_"PersistentVector" methods (Reflector'getMethods (.getClass target), (alength args), methodName, false)]
            (Reflector'invokeMatchingMethod methodName, methods, target, args)
        )
    )

    (defn #_"Object" Reflector'invokeConstructor [#_"Class" c, #_"Object[]" args]
        (try
            (let [#_"Constructor[]" allctors (.getConstructors c)
                  #_"PersistentVector" ctors
                    (loop-when [ctors [] #_"int" i 0] (< i (alength allctors)) => ctors
                        (let [#_"Constructor" ctor (aget allctors i)
                              ctors
                                (when (= (alength (.getParameterTypes ctor)) (alength args)) => ctors
                                    (conj ctors ctor)
                                )]
                            (recur ctors (inc i))
                        )
                    )]
                (condp = (count ctors)
                    0   (throw (IllegalArgumentException. (str "No matching ctor found for " c)))
                    1   (let [#_"Constructor" ctor (nth ctors 0)]
                            (.newInstance ctor, (Reflector'boxArgs (.getParameterTypes ctor), args))
                        )
                    (or ;; overloaded w/same arity
                        (loop-when-recur [#_"Iterator" it (.iterator ctors)] (.hasNext it) [it]
                            (let [#_"Constructor" ctor (.next it)]
                                (let-when [#_"Class[]" params (.getParameterTypes ctor)] (Reflector'isCongruent params, args)
                                    (.newInstance ctor, (Reflector'boxArgs params, args))
                                )
                            )
                        )
                        (throw (IllegalArgumentException. (str "No matching ctor found for " c)))
                    )
                )
            )
            (catch Exception e
                (throw (Reflector'getCauseOrElse e))
            )
        )
    )

    (defn #_"Object" Reflector'invokeStaticMethod [#_"Class" c, #_"String" methodName, #_"Object[]" args]
        (if (= methodName "new")
            (Reflector'invokeConstructor c, args)
            (let [#_"PersistentVector" methods (Reflector'getMethods c, (alength args), methodName, true)]
                (Reflector'invokeMatchingMethod methodName, methods, nil, args)
            )
        )
    )

    (defn #_"Object" Reflector'getStaticField [#_"Class" c, #_"String" fieldName]
        (let [#_"Field" f (Reflector'getField c, fieldName, true)]
            (when (some? f) => (throw (IllegalArgumentException. (str "No matching field found: " fieldName " for " c)))
                (Reflector'prepRet (.getType f), (.get f, nil))
            )
        )
    )

    (defn #_"Object" Reflector'setStaticField [#_"Class" c, #_"String" fieldName, #_"Object" val]
        (let [#_"Field" f (Reflector'getField c, fieldName, true)]
            (when (some? f) => (throw (IllegalArgumentException. (str "No matching field found: " fieldName " for " c)))
                (.set f, nil, (Reflector'boxArg (.getType f), val))
                val
            )
        )
    )

    (defn #_"Object" Reflector'getInstanceField [#_"Object" target, #_"String" fieldName]
        (let [#_"Class" c (.getClass target) #_"Field" f (Reflector'getField c, fieldName, false)]
            (when (some? f) => (throw (IllegalArgumentException. (str "No matching field found: " fieldName " for " c)))
                (Reflector'prepRet (.getType f), (.get f, target))
            )
        )
    )

    (defn #_"Object" Reflector'setInstanceField [#_"Object" target, #_"String" fieldName, #_"Object" val]
        (let [#_"Class" c (.getClass target) #_"Field" f (Reflector'getField c, fieldName, false)]
            (when (some? f) => (throw (IllegalArgumentException. (str "No matching field found: " fieldName " for " (.getClass target))))
                (.set f, target, (Reflector'boxArg (.getType f), val))
                val
            )
        )
    )

    (defn #_"Object" Reflector'invokeNoArgInstanceMember [#_"Object" target, #_"String" name, #_"boolean" requireField]
        (let [#_"Class" c (.getClass target)]
            (if requireField
                (let [#_"Field" f (Reflector'getField c, name, false)]
                    (if (some? f)
                        (Reflector'getInstanceField target, name)
                        (throw (IllegalArgumentException. (str "No matching field found: " name " for " (.getClass target))))
                    )
                )
                (let [#_"PersistentVector" methods (Reflector'getMethods c, 0, name, false)]
                    (if (pos? (count methods))
                        (Reflector'invokeMatchingMethod name, methods, target, RT/EMPTY_ARRAY)
                        (Reflector'getInstanceField target, name)
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.Compiler

(def Context'enum-set
    (hash-set
        :Context'STATEMENT ;; value ignored
        :Context'EXPRESSION ;; value required
        :Context'RETURN ;; tail position relative to enclosing recur frame
        :Context'EVAL
    )
)

(class-ns Compiler
    (def #_"Class" Compiler'BOOLEANS_CLASS (Class/forName "[Z"))
    (def #_"Class" Compiler'BYTES_CLASS    (Class/forName "[B"))
    (def #_"Class" Compiler'SHORTS_CLASS   (Class/forName "[S"))
    (def #_"Class" Compiler'CHARS_CLASS    (Class/forName "[C"))
    (def #_"Class" Compiler'INTS_CLASS     (Class/forName "[I"))
    (def #_"Class" Compiler'LONGS_CLASS    (Class/forName "[J"))
    (def #_"Class" Compiler'FLOATS_CLASS   (Class/forName "[F"))
    (def #_"Class" Compiler'DOUBLES_CLASS  (Class/forName "[D"))
    (def #_"Class" Compiler'OBJECTS_CLASS  (Class/forName "[Ljava.lang.Object;"))

    (def #_"int" Compiler'MAX_POSITIONAL_ARITY 20)

    (def #_"String" Compiler'COMPILE_STUB_PREFIX "compile__stub")

    (def #_"Symbol" Compiler'FNONCE (with-meta 'fn* { :once true }))

    (defn #_"String" Compiler'cachedClassName [#_"int" n] (str "__cached_class__" n))
    (defn #_"String" Compiler'constantName    [#_"int" n] (str "const__" n))
    (defn #_"String" Compiler'siteNameStatic  [#_"int" n] (str "__site__" n "__"))
    (defn #_"String" Compiler'thunkNameStatic [#_"int" n] (str "__thunk__" n "__"))
)

(class-ns NilExpr
    (defn #_"NilExpr" NilExpr'new []
        (NilExpr.)
    )

    (extend-type NilExpr Literal
        (#_"Object" Literal'''literal [#_"NilExpr" this]
            nil
        )
    )

    (extend-type NilExpr Expr
        (#_"Object" Expr'''eval [#_"NilExpr" this]
            (Literal'''literal this)
        )

        (#_"void" Expr'''emit [#_"NilExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (.visitInsn gen, Opcodes/ACONST_NULL)
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"NilExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"NilExpr" this]
            nil
        )
    )

    (def #_"NilExpr" Compiler'NIL_EXPR (NilExpr'new))
)

(class-ns BooleanExpr
    (defn #_"BooleanExpr" BooleanExpr'new [#_"boolean" val]
        (merge (BooleanExpr.)
            (hash-map
                #_"boolean" :val val
            )
        )
    )

    (extend-type BooleanExpr Literal
        (#_"Object" Literal'''literal [#_"BooleanExpr" this]
            (if (:val this) true false)
        )
    )

    (extend-type BooleanExpr Expr
        (#_"Object" Expr'''eval [#_"BooleanExpr" this]
            (Literal'''literal this)
        )

        (#_"void" Expr'''emit [#_"BooleanExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (.getStatic gen, (Type/getType Boolean), (if (:val this) "TRUE" "FALSE"), (Type/getType Boolean))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"BooleanExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"BooleanExpr" this]
            Boolean
        )
    )

    (def #_"BooleanExpr" Compiler'TRUE_EXPR (BooleanExpr'new true))
    (def #_"BooleanExpr" Compiler'FALSE_EXPR (BooleanExpr'new false))
)

(class-ns Compiler
    (def #_"Var" ^:dynamic *loader*            ) ;; DynamicClassLoader
    (def #_"Var" ^:dynamic *line*              ) ;; Integer
    (def #_"Var" ^:dynamic *last-unique-id*    ) ;; Integer
    (def #_"Var" ^:dynamic *closes*            ) ;; IPersistentMap
    (def #_"Var" ^:dynamic *method*            ) ;; FnFrame
    (def #_"Var" ^:dynamic *local-env*         ) ;; symbol->localbinding
    (def #_"Var" ^:dynamic *last-local-num*    ) ;; Integer
    (def #_"Var" ^:dynamic *loop-locals*       ) ;; vector<localbinding>
    (def #_"Var" ^:dynamic *loop-label*        ) ;; Label
    (def #_"Var" ^:dynamic *constants*         ) ;; vector<object>
    (def #_"Var" ^:dynamic *constant-ids*      ) ;; IdentityHashMap
    (def #_"Var" ^:dynamic *used-constants*    ) ;; IPersistentSet
    (def #_"Var" ^:dynamic *keyword-callsites* ) ;; vector<keyword>
    (def #_"Var" ^:dynamic *protocol-callsites*) ;; vector<var>
    (def #_"Var" ^:dynamic *keywords*          ) ;; keyword->constid
    (def #_"Var" ^:dynamic *vars*              ) ;; var->constid
    (def #_"Var" ^:dynamic *no-recur*          ) ;; Boolean
    (def #_"Var" ^:dynamic *in-catch-finally*  ) ;; Boolean
    (def #_"Var" ^:dynamic *in-return-context* ) ;; Boolean
    (def #_"Var" ^:dynamic *compile-stub-sym*  ) ;; Symbol
    (def #_"Var" ^:dynamic *compile-stub-class*) ;; Class

    (def #_"Method[]" Compiler'createTupleMethods
        (object-array [
            (Method/getMethod "clojure.lang.IPersistentVector create()")
            (Method/getMethod "clojure.lang.IPersistentVector create(Object)")
            (Method/getMethod "clojure.lang.IPersistentVector create(Object, Object)")
            (Method/getMethod "clojure.lang.IPersistentVector create(Object, Object, Object)")
            (Method/getMethod "clojure.lang.IPersistentVector create(Object, Object, Object, Object)")
            (Method/getMethod "clojure.lang.IPersistentVector create(Object, Object, Object, Object, Object)")
            (Method/getMethod "clojure.lang.IPersistentVector create(Object, Object, Object, Object, Object, Object)")
        ])
    )

    (def- #_"Type[][]" Compiler'ARG_TYPES
        (let [#_"int" n Compiler'MAX_POSITIONAL_ARITY
              #_"Type[][]" a (make-array (Class/forName "[Lcloiure.asm.Type;") (+ n 2))
              #_"Type" t (Type/getType Object)]
            (dotimes [#_"int" i (inc n)]
                (let [#_"Type[]" b (make-array Type i)]
                    (dotimes [#_"int" j i]
                        (aset b j t)
                    )
                    (aset a i b)
                )
            )
            (let [#_"Type[]" b (make-array Type (inc n))]
                (dotimes [#_"int" j n]
                    (aset b j t)
                )
                (aset b n (Type/getType "[Ljava/lang/Object;"))
                (aset a (inc n) b)
                a
            )
        )
    )

    (def- #_"Type[]" Compiler'EXCEPTION_TYPES (make-array Type 0))

    (declare Compiler'specials)

    (defn #_"boolean" Compiler'isSpecial [#_"Object" sym]
        (contains? Compiler'specials sym)
    )

    (defn #_"boolean" Compiler'inTailCall [#_"Context" context]
        (and (= context :Context'RETURN) *in-return-context* (not *in-catch-finally*))
    )

    (defn #_"Namespace" Compiler'namespaceFor
        ([#_"Symbol" sym] (Compiler'namespaceFor *ns*, sym))
        ([#_"Namespace" inns, #_"Symbol" sym]
            ;; note, presumes non-nil sym.ns
            (let [#_"Symbol" nsSym (symbol (ßns sym))]
                ;; first check against currentNS' aliases, otherwise check the Namespaces map
                (or (.lookupAlias inns, nsSym) (find-ns nsSym))
            )
        )
    )

    (defn #_"Symbol" Compiler'resolveSymbol [#_"Symbol" sym]
        ;; already qualified or classname?
        (cond
            (pos? (.indexOf (ßname sym), (int \.)))
                sym
            (some? (ßns sym))
                (let [#_"Namespace" ns (Compiler'namespaceFor sym)]
                    (if (and (some? ns) (not (and (some? (ßname (ßname ns))) (= (ßname (ßname ns)) (ßns sym)))))
                        (symbol (ßname (ßname ns)), (ßname sym))
                        sym
                    )
                )
            :else
                (let [#_"Object" o (.getMapping *ns*, sym)]
                    (cond
                        (nil? o)            (symbol (ßname (ßname *ns*)), (ßname sym))
                        (instance? Class o) (symbol (.getName o))
                        (var? o)            (symbol (ßname (ßname (ßns o))), (ßname (ßsym o)))
                    )
                )
        )
    )

    (defn #_"Class" Compiler'maybePrimitiveType [#_"Expr" e]
        (when (and (satisfies? MaybePrimitive e) (Expr'''hasJavaClass e) (MaybePrimitive'''canEmitPrimitive e))
            (let-when [#_"Class" c (Expr'''getJavaClass e)] (Reflector'isPrimitive c)
                c
            )
        )
    )

    (defn #_"Class" Compiler'maybeJavaClass [#_"Iterable" exprs]
        (try
            (let [#_"Iterator" it (.iterator exprs)]
                (loop-when [#_"Class" match nil] (.hasNext it) => match
                    (let [#_"Expr" e (.next it)]
                        (cond
                            (instance? ThrowExpr e)
                                (recur match)
                            (Expr'''hasJavaClass e)
                                (let [#_"Class" c (Expr'''getJavaClass e)]
                                    (cond
                                        (nil? match) (recur c)
                                        (= match c) (recur match)
                                    )
                                )
                        )
                    )
                )
            )
            (catch Exception _
                nil
            )
        )
    )

    (defn #_"String" Compiler'getTypeStringForArgs [#_"IPersistentVector" args]
        (let [#_"StringBuilder" sb (StringBuilder.)]
            (dotimes [#_"int" i (count args)]
                (let [#_"Expr" arg (nth args i)]
                    (when (pos? i)
                        (.append sb, ", ")
                    )
                    (.append sb, (if (and (Expr'''hasJavaClass arg) (some? (Expr'''getJavaClass arg))) (.getName (Expr'''getJavaClass arg)) "unknown"))
                )
            )
            (.toString sb)
        )
    )

    (defn #_"int" Compiler'getMatchingParams [#_"String" methodName, #_"IPersistentVector" pars, #_"IPersistentVector" args, #_"IPersistentVector" rets]
        ;; presumes matching lengths
        (let [[#_"int" matchIdx #_"boolean" tied]
                (loop-when [matchIdx -1 tied false #_"boolean" foundExact false #_"int" i 0] (< i (count pars)) => [matchIdx tied]
                    (let [[#_"int" exact #_"boolean" match]
                            (loop-when [exact 0 match true #_"int" p 0 #_"ISeq" s (seq args)] (and match (< p (count args)) (some? s)) => [exact match]
                                (let [#_"Expr" arg (first s)
                                      #_"Class" aclass (if (Expr'''hasJavaClass arg) (Expr'''getJavaClass arg) Object) #_"Class" pclass (aget (nth pars i) p)
                                      [exact match]
                                        (if (and (Expr'''hasJavaClass arg) (= aclass pclass))
                                            [(inc exact) match]
                                            [exact (Reflector'paramArgTypeMatch pclass, aclass)]
                                        )]
                                    (recur exact match (inc p) (next s))
                                )
                            )
                          [matchIdx tied foundExact]
                            (cond (= exact (count args))
                                (let [matchIdx
                                        (when (or (not foundExact) (= matchIdx -1) (.isAssignableFrom (nth rets matchIdx), (nth rets i))) => matchIdx
                                            i
                                        )]
                                    [matchIdx false true]
                                )
                                (and match (not foundExact))
                                (let [[matchIdx tied]
                                        (cond (= matchIdx -1)
                                            (do
                                                [i tied]
                                            )
                                            (Reflector'subsumes (nth pars i), (nth pars matchIdx))
                                            (do
                                                [i false]
                                            )
                                            (Arrays/equals (nth pars matchIdx), (nth pars i))
                                            (let [matchIdx
                                                    (when (.isAssignableFrom (nth rets matchIdx), (nth rets i)) => matchIdx
                                                        i
                                                    )]
                                                [matchIdx tied]
                                            )
                                            (not (Reflector'subsumes (nth pars matchIdx), (nth pars i)))
                                            (do
                                                [matchIdx true]
                                            )
                                            :else
                                            (do
                                                [matchIdx tied]
                                            )
                                        )]
                                    [matchIdx tied foundExact]
                                )
                                :else
                                (do
                                    [matchIdx tied foundExact]
                                )
                            )]
                        (recur matchIdx tied foundExact (inc i))
                    )
                )]
            (when tied
                (throw (IllegalArgumentException. (str "More than one matching method found: " methodName)))
            )
            matchIdx
        )
    )

    (def #_"IPersistentMap" Compiler'CHAR_MAP
        (hash-map
            \- "_"
            \: "_COLON_"
            \+ "_PLUS_"
            \> "_GT_"
            \< "_LT_"
            \= "_EQ_"
            \~ "_TILDE_"
            \! "_BANG_"
            \@ "_CIRCA_"
            \# "_SHARP_"
            \' "_SINGLEQUOTE_"
            \" "_DOUBLEQUOTE_" ;; oops! "
            \% "_PERCENT_"
            \^ "_CARET_"
            \& "_AMPERSAND_"
            \* "_STAR_"
            \| "_BAR_"
            \{ "_LBRACE_"
            \} "_RBRACE_"
            \[ "_LBRACK_"
            \] "_RBRACK_"
            \/ "_SLASH_"
            \\ "_BSLASH_"
            \? "_QMARK_"
        )
    )

    (def #_"IPersistentMap" Compiler'DEMUNGE_MAP
        ;; DEMUNGE_MAP maps strings to characters in the opposite direction that CHAR_MAP does, plus it maps "$" to '/'.
        (loop-when [#_"IPersistentMap" m { "$" \/ } #_"ISeq" s (seq Compiler'CHAR_MAP)] (some? s) => m
            (let [#_"IMapEntry" e (first s)]
                (recur (assoc m (val e) (key e)) (next s))
            )
        )
    )

    (def #_"Pattern" Compiler'DEMUNGE_PATTERN
        ;; DEMUNGE_PATTERN searches for the first of any occurrence of the strings that are keys of DEMUNGE_MAP.
        ;; Note: Regex matching rules mean that #"_|_COLON_" "_COLON_" returns "_", but #"_COLON_|_" "_COLON_"
        ;; returns "_COLON_" as desired. Sorting string keys of DEMUNGE_MAP from longest to shortest ensures
        ;; correct matching behavior, even if some strings are prefixes of others.
        (let [#_"String[]" a (to-array (keys Compiler'DEMUNGE_MAP)) _ (Arrays/sort a, #(- (.length %2) (.length %1)))
              #_"StringBuilder" sb (StringBuilder.)]
            (dotimes [#_"int" i (alength a)]
                (when (pos? i)
                    (.append sb, "|")
                )
                (.append sb, "\\Q")
                (.append sb, (aget a i))
                (.append sb, "\\E")
            )
            (Pattern/compile (.toString sb))
        )
    )

    (defn #_"String" Compiler'munge [#_"String" name]
        (let [#_"StringBuilder" sb (StringBuilder.)]
            (doseq [#_"char" c (.toCharArray name)]
                (.append sb, (or (get Compiler'CHAR_MAP c) c))
            )
            (.toString sb)
        )
    )

    (defn #_"String" Compiler'demunge [#_"String" mungedName]
        (let [#_"StringBuilder" sb (StringBuilder.)
              #_"Matcher" m (.matcher Compiler'DEMUNGE_PATTERN, mungedName)
              #_"int" lastMatchEnd
                (loop-when [lastMatchEnd 0] (.find m) => lastMatchEnd
                    (let [#_"int" start (.start m) #_"int" end (.end m)]
                        ;; keep everything before the match
                        (.append sb, (.substring mungedName, lastMatchEnd, start))
                        ;; replace the match with DEMUNGE_MAP result
                        (.append sb, (get Compiler'DEMUNGE_MAP (.group m)))
                        (recur end)
                    )
                )]
            ;; keep everything after the last match
            (.append sb, (.substring mungedName, lastMatchEnd))
            (.toString sb)
        )
    )

    (defn #_"int" Compiler'nextUniqueId []
        (update! *last-unique-id* inc)
    )

    (defn- #_"int" Compiler'nextLocalNum []
        (update! *last-local-num* inc)
    )

    (declare LocalBinding'new)

    (defn #_"LocalBinding" Compiler'registerLocal [#_"Symbol" sym, #_"Symbol" tag, #_"Expr" init, #_"boolean" isArg]
        (let [#_"LocalBinding" lb (LocalBinding'new (Compiler'nextLocalNum), sym, tag, init, isArg)]
            (update! *local-env* assoc (:sym lb) lb)
            (update! *method* update :locals assoc (:uid lb) lb)
            lb
        )
    )

    (defn #_"LocalBinding" Compiler'complementLocalInit [#_"LocalBinding" lb, #_"Expr" init]
        (let [lb (assoc lb :init init)]
            (update! *local-env* assoc (:sym lb) lb)
            (update! *method* update :locals assoc (:uid lb) lb)
            lb
        )
    )

    (defn- #_"void" Compiler'closeOver [#_"LocalBinding" lb, #_"IopMethod" m]
        (when (and (some? lb) (some? m) (not (contains? (:locals m) (:uid lb))))
            (update! *closes* update (:uid (:objx m)) assoc (:uid lb) lb)
            (Compiler'closeOver lb, (:parent m))
        )
        nil
    )

    (defn #_"LocalBinding" Compiler'referenceLocal [#_"Symbol" sym]
        (when-let [#_"LocalBinding" lb (get *local-env* sym)]
            (Compiler'closeOver lb, *method*)
            lb
        )
    )

    (defn- #_"int" Compiler'registerConstant [#_"Object" o]
        (when (bound? #'*constants*) => -1
            (let [#_"IdentityHashMap<Object, Integer>" ids *constant-ids*]
                (or (get ids o)
                    (let [#_"PersistentVector" v *constants*]
                        (set! *constants* (conj v o))
                        (.put ids, o, (count v))
                        (count v)
                    )
                )
            )
        )
    )

    (defn- #_"int" Compiler'registerKeywordCallsite [#_"Keyword" k]
        (let [#_"IPersistentVector" v (conj *keyword-callsites* k)]
            (set! *keyword-callsites* v)
            (dec (count v))
        )
    )

    (defn- #_"int" Compiler'registerProtocolCallsite [#_"Var" v]
        (let [#_"IPersistentVector" v (conj *protocol-callsites* v)]
            (set! *protocol-callsites* v)
            (dec (count v))
        )
    )

    (defn- #_"void" Compiler'registerVar [#_"Var" var]
        (when (and (bound? #'*vars*) (nil? (get *vars* var)))
            (update! *vars* assoc var (Compiler'registerConstant var))
        )
        nil
    )

    (defn #_"Var" Compiler'lookupVar
        ([#_"Symbol" sym, #_"boolean" internNew] (Compiler'lookupVar sym, internNew, true))
        ([#_"Symbol" sym, #_"boolean" internNew, #_"boolean" registerMacro]
            ;; note - ns-qualified vars in other namespaces must already exist
            (let [#_"Var" var
                    (cond
                        (some? (ßns sym))
                            (when-let [#_"Namespace" ns (Compiler'namespaceFor sym)]
                                (let [#_"Symbol" name (symbol (ßname sym))]
                                    (if (and internNew (= ns *ns*))
                                        (.intern ns, name)
                                        (.findInternedVar ns, name)
                                    )
                                )
                            )
                        (= sym 'ns)    #'ns
                        (= sym 'in-ns) #'in-ns
                        :else ;; is it mapped?
                            (let [#_"Object" o (.getMapping *ns*, sym)]
                                (cond
                                    (nil? o) ;; introduce a new var in the current ns
                                        (when internNew
                                            (.intern *ns*, (symbol (ßname sym)))
                                        )
                                    (var? o)
                                        o
                                    :else
                                        (throw (RuntimeException. (str "Expecting var, but " sym " is mapped to " o)))
                                )
                            )
                    )]
                (when (and (some? var) (or (not (get (meta var) :macro)) registerMacro))
                    (Compiler'registerVar var)
                )
                var
            )
        )
    )

    (defn #_"Var" Compiler'isMacro [#_"Object" op]
        ;; no local macros for now
        (when-not (and (symbol? op) (some? (Compiler'referenceLocal op)))
            (when (or (symbol? op) (var? op))
                (let [#_"Var" v (if (var? op) op (Compiler'lookupVar op, false, false))]
                    (when (and (some? v) (get (meta v) :macro))
                        (when (or (= (ßns v) *ns*) (not (get (meta v) :private))) => (throw (IllegalStateException. (str "var: " v " is private")))
                            v
                        )
                    )
                )
            )
        )
    )

    (defn #_"IFn" Compiler'isInline [#_"Object" op, #_"int" arity]
        ;; no local inlines for now
        (when-not (and (symbol? op) (some? (Compiler'referenceLocal op)))
            (when (or (symbol? op) (var? op))
                (when-let [#_"Var" v (if (var? op) op (Compiler'lookupVar op, false))]
                    (when (or (= (ßns v) *ns*) (not (get (meta v) :private))) => (throw (IllegalStateException. (str "var: " v " is private")))
                        (when-let [#_"IFn" f (get (meta v) :inline)]
                            (let [#_"IFn" arityPred (get (meta v) :inline-arities)]
                                (when (or (nil? arityPred) (.invoke arityPred, arity))
                                    f
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    (defn #_"boolean" Compiler'namesStaticMember [#_"Symbol" sym]
        (and (some? (ßns sym)) (nil? (Compiler'namespaceFor sym)))
    )

    (defn- #_"Symbol" Compiler'tagOf [#_"Object" o]
        (let [#_"Object" tag (get (meta o) :tag)]
            (cond
                (symbol? tag) tag
                (string? tag) (symbol tag)
            )
        )
    )

    (defn #_"Object" Compiler'preserveTag [#_"ISeq" src, #_"Object" dst]
        (let-when [#_"Symbol" tag (Compiler'tagOf src)] (and (some? tag) (instance? IObj dst)) => dst
            (vary-meta dst assoc :tag tag)
        )
    )

    (defn #_"String" Compiler'destubClassName [#_"String" name]
        ;; skip over prefix + '.' or '/'
        (when (.startsWith name, Compiler'COMPILE_STUB_PREFIX) => name
            (.substring name, (inc (.length Compiler'COMPILE_STUB_PREFIX)))
        )
    )

    (defn #_"Type" Compiler'getType [#_"Class" c]
        (let [#_"String" desc (.getDescriptor (Type/getType c))
              desc
                (when (.startsWith desc, "L") => desc
                    (str "L" (Compiler'destubClassName (.substring desc, 1)))
                )]
            (Type/getType desc)
        )
    )

    (defn #_"Object" Compiler'resolveIn [#_"Namespace" n, #_"Symbol" sym, #_"boolean" allowPrivate]
        ;; note - ns-qualified vars must already exist
        (cond
            (some? (ßns sym))
                (let-when [#_"Namespace" ns (Compiler'namespaceFor n, sym)] (some? ns)          => (throw (RuntimeException. (str "No such namespace: " (ßns sym))))
                    (let-when [#_"Var" v (.findInternedVar ns, (symbol (ßname sym)))] (some? v) => (throw (RuntimeException. (str "No such var: " sym)))
                        (when (or (= (ßns v) *ns*) (not (get (meta v) :private)) allowPrivate)  => (throw (IllegalStateException. (str "var: " sym " is private")))
                            v
                        )
                    )
                )
            (or (pos? (.indexOf (ßname sym), (int \.))) (= (.charAt (ßname sym), 0) \[)) (RT/classForName (ßname sym))
            (= sym 'ns)                #'ns
            (= sym 'in-ns)             #'in-ns
            (= sym *compile-stub-sym*) *compile-stub-class*
            :else
                (or (.getMapping n, sym)
                    (when *allow-unresolved-vars* => (throw (RuntimeException. (str "Unable to resolve symbol: " sym " in this context")))
                        sym
                    )
                )
        )
    )

    (defn #_"Object" Compiler'resolve
        ([#_"Symbol" sym                          ] (Compiler'resolveIn *ns*, sym, false       ))
        ([#_"Symbol" sym, #_"boolean" allowPrivate] (Compiler'resolveIn *ns*, sym, allowPrivate))
    )

    (defn #_"Object" Compiler'maybeResolveIn [#_"Namespace" n, #_"Symbol" sym]
        ;; note - ns-qualified vars must already exist
        (cond
            (some? (ßns sym))
                (when-let [#_"Namespace" ns (Compiler'namespaceFor n, sym)]
                    (when-let [#_"Var" v (.findInternedVar ns, (symbol (ßname sym)))]
                        v
                    )
                )
            (or (and (pos? (.indexOf (ßname sym), (int \.))) (not (.endsWith (ßname sym), "."))) (= (.charAt (ßname sym), 0) \[))
                (RT/classForName (ßname sym))
            (= sym 'ns)
                #'ns
            (= sym 'in-ns)
                #'in-ns
            :else
                (.getMapping n, sym)
        )
    )

    (defn #_"boolean" Compiler'inty [#_"Class" c] (any = c Integer/TYPE Short/TYPE Byte/TYPE Character/TYPE))

    (defn #_"Class" Compiler'retType [#_"Class" tc, #_"Class" ret]
        (cond
            (nil? tc)
                ret
            (nil? ret)
                tc
            (and (.isPrimitive ret) (.isPrimitive tc))
                (if (or (and (Compiler'inty ret) (Compiler'inty tc)) (= ret tc))
                    tc
                    (throw (UnsupportedOperationException. (str "Cannot coerce " ret " to " tc ", use a cast instead")))
                )
            :else
                tc
        )
    )

    (defn #_"Class" Compiler'primClass [#_"Class" c]
        (if (.isPrimitive c) c Object)
    )

    (defn #_"Class" Compiler'boxClass [#_"Class" p]
        (when (.isPrimitive p) => p
            (condp = p
                Integer/TYPE   Integer
                Long/TYPE      Long
                Float/TYPE     Float
                Double/TYPE    Double
                Character/TYPE Character
                Short/TYPE     Short
                Byte/TYPE      Byte
                Boolean/TYPE   Boolean
                               nil
            )
        )
    )
)

(class-ns MonitorEnterExpr
    (defn #_"MonitorEnterExpr" MonitorEnterExpr'new [#_"Expr" target]
        (merge (MonitorEnterExpr.)
            (hash-map
                #_"Expr" :target target
            )
        )
    )

    (extend-type MonitorEnterExpr Expr
        (#_"Object" Expr'''eval [#_"MonitorEnterExpr" this]
            (throw (UnsupportedOperationException. "Can't eval monitor-enter"))
        )

        (#_"void" Expr'''emit [#_"MonitorEnterExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
            (.monitorEnter gen)
            (Expr'''emit Compiler'NIL_EXPR, context, objx, gen)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"MonitorEnterExpr" this]
            false
        )

        (#_"Class" Expr'''getJavaClass [#_"MonitorEnterExpr" this]
            (throw (UnsupportedOperationException. "Has no Java class"))
        )
    )
)

(declare Compiler'analyze)

(class-ns MonitorEnterParser
    (defn #_"IParser" MonitorEnterParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (MonitorEnterExpr'new (Compiler'analyze :Context'EXPRESSION, (second form)))
            )
        )
    )
)

(class-ns MonitorExitExpr
    (defn #_"MonitorExitExpr" MonitorExitExpr'new [#_"Expr" target]
        (merge (MonitorExitExpr.)
            (hash-map
                #_"Expr" :target target
            )
        )
    )

    (extend-type MonitorExitExpr Expr
        (#_"Object" Expr'''eval [#_"MonitorExitExpr" this]
            (throw (UnsupportedOperationException. "Can't eval monitor-exit"))
        )

        (#_"void" Expr'''emit [#_"MonitorExitExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
            (.monitorExit gen)
            (Expr'''emit Compiler'NIL_EXPR, context, objx, gen)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"MonitorExitExpr" this]
            false
        )

        (#_"Class" Expr'''getJavaClass [#_"MonitorExitExpr" this]
            (throw (UnsupportedOperationException. "Has no Java class"))
        )
    )
)

(class-ns MonitorExitParser
    (defn #_"IParser" MonitorExitParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (MonitorExitExpr'new (Compiler'analyze :Context'EXPRESSION, (second form)))
            )
        )
    )
)

(class-ns AssignExpr
    (defn #_"AssignExpr" AssignExpr'new [#_"Assignable" target, #_"Expr" val]
        (merge (AssignExpr.)
            (hash-map
                #_"Assignable" :target target
                #_"Expr" :val val
            )
        )
    )

    (extend-type AssignExpr Expr
        (#_"Object" Expr'''eval [#_"AssignExpr" this]
            (Assignable'''evalAssign (:target this), (:val this))
        )

        (#_"void" Expr'''emit [#_"AssignExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Assignable'''emitAssign (:target this), context, objx, gen, (:val this))
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"AssignExpr" this]
            (Expr'''hasJavaClass (:val this))
        )

        (#_"Class" Expr'''getJavaClass [#_"AssignExpr" this]
            (Expr'''getJavaClass (:val this))
        )
    )
)

(class-ns AssignParser
    (defn #_"IParser" AssignParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (= (count form) 3) => (throw (IllegalArgumentException. "Malformed assignment, expecting (set! target val)"))
                    (let [#_"Expr" target (Compiler'analyze :Context'EXPRESSION, (second form))]
                        (when (satisfies? Assignable target) => (throw (IllegalArgumentException. "Invalid assignment target"))
                            (AssignExpr'new target, (Compiler'analyze :Context'EXPRESSION, (third form)))
                        )
                    )
                )
            )
        )
    )
)

(class-ns ImportExpr
    (defn #_"ImportExpr" ImportExpr'new [#_"String" c]
        (merge (ImportExpr.)
            (hash-map
                #_"String" :c c
            )
        )
    )

    (extend-type ImportExpr Expr
        (#_"Object" Expr'''eval [#_"ImportExpr" this]
            (.importClass *ns*, (RT/classForNameNonLoading (:c this)))
            nil
        )

        (#_"void" Expr'''emit [#_"ImportExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (.getStatic gen, (Type/getType RT), "CURRENT_NS", (Type/getType Var))
            (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "Object deref()"))
            (.checkCast gen, (Type/getType Namespace))
            (.push gen, (:c this))
            (.invokeStatic gen, (Type/getType RT), (Method/getMethod "Class classForNameNonLoading(String)"))
            (.invokeVirtual gen, (Type/getType Namespace), (Method/getMethod "Class importClass(Class)"))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"ImportExpr" this]
            false
        )

        (#_"Class" Expr'''getJavaClass [#_"ImportExpr" this]
            (throw (IllegalArgumentException. "ImportExpr has no Java class"))
        )
    )
)

(class-ns ImportParser
    (defn #_"IParser" ImportParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (ImportExpr'new (second form))
            )
        )
    )
)

(class-ns EmptyExpr
    (defn #_"EmptyExpr" EmptyExpr'new [#_"Object" coll]
        (merge (EmptyExpr.)
            (hash-map
                #_"Object" :coll coll
            )
        )
    )

    (extend-type EmptyExpr Expr
        (#_"Object" Expr'''eval [#_"EmptyExpr" this]
            (:coll this)
        )

        (#_"void" Expr'''emit [#_"EmptyExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (cond
                (instance? IPersistentList (:coll this))   (.getStatic gen, (Type/getType PersistentList),     "EMPTY", (Type/getType PersistentList$EmptyList))
                (instance? IPersistentVector (:coll this)) (.getStatic gen, (Type/getType PersistentVector),   "EMPTY", (Type/getType PersistentVector))
                (instance? IPersistentMap (:coll this))    (.getStatic gen, (Type/getType PersistentArrayMap), "EMPTY", (Type/getType PersistentArrayMap))
                (instance? IPersistentSet (:coll this))    (.getStatic gen, (Type/getType PersistentHashSet),  "EMPTY", (Type/getType PersistentHashSet))
                :else                                      (throw (UnsupportedOperationException. "Unknown collection type"))
            )
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"EmptyExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"EmptyExpr" this]
            (cond
                (instance? IPersistentList (:coll this))   IPersistentList
                (instance? IPersistentVector (:coll this)) IPersistentVector
                (instance? IPersistentMap (:coll this))    IPersistentMap
                (instance? IPersistentSet (:coll this))    IPersistentSet
                :else                                      (throw (UnsupportedOperationException. "Unknown collection type"))
            )
        )
    )
)

(class-ns ConstantExpr
    (defn #_"ConstantExpr" ConstantExpr'new [#_"Object" v]
        (merge (ConstantExpr.)
            (hash-map
                #_"Object" :v v
                #_"int" :id (Compiler'registerConstant v)
            )
        )
    )

    (extend-type ConstantExpr Literal
        (#_"Object" Literal'''literal [#_"ConstantExpr" this]
            (:v this)
        )
    )

    (declare IopObject''emitConstant)

    (extend-type ConstantExpr Expr
        (#_"Object" Expr'''eval [#_"ConstantExpr" this]
            (Literal'''literal this)
        )

        (#_"void" Expr'''emit [#_"ConstantExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IopObject''emitConstant objx, gen, (:id this))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"ConstantExpr" this]
            (Modifier/isPublic (.getModifiers (.getClass (:v this))))
        )

        (#_"Class" Expr'''getJavaClass [#_"ConstantExpr" this]
            (cond
                (instance? APersistentMap (:v this))    APersistentMap
                (instance? APersistentSet (:v this))    APersistentSet
                (instance? APersistentVector (:v this)) APersistentVector
                :else                                   (.getClass (:v this))
            )
        )
    )
)

(class-ns NumberExpr
    (defn #_"NumberExpr" NumberExpr'new [#_"Number" n]
        (merge (NumberExpr.)
            (hash-map
                #_"Number" :n n
                #_"int" :id (Compiler'registerConstant n)
            )
        )
    )

    (extend-type NumberExpr Literal
        (#_"Object" Literal'''literal [#_"NumberExpr" this]
            (:n this)
        )
    )

    (extend-type NumberExpr Expr
        (#_"Object" Expr'''eval [#_"NumberExpr" this]
            (Literal'''literal this)
        )

        (#_"void" Expr'''emit [#_"NumberExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when-not (= context :Context'STATEMENT)
                (IopObject''emitConstant objx, gen, (:id this))
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"NumberExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"NumberExpr" this]
            (cond
                (instance? Integer (:n this)) Long/TYPE
                (instance? Double (:n this))  Double/TYPE
                (instance? Long (:n this))    Long/TYPE
                :else                         (throw (IllegalStateException. (str "Unsupported Number type: " (.getName (.getClass (:n this))))))
            )
        )
    )

    (extend-type NumberExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"NumberExpr" this]
            true
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"NumberExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (cond
                (instance? Integer (:n this)) (.push gen, (.longValue (:n this)))
                (instance? Double (:n this))  (.push gen, (.doubleValue (:n this)))
                (instance? Long (:n this))    (.push gen, (.longValue (:n this)))
            )
            nil
        )
    )

    (defn #_"Expr" NumberExpr'parse [#_"Number" form]
        (if (or (instance? Integer form) (instance? Double form) (instance? Long form))
            (NumberExpr'new form)
            (ConstantExpr'new form)
        )
    )
)

(class-ns StringExpr
    (defn #_"StringExpr" StringExpr'new [#_"String" str]
        (merge (StringExpr.)
            (hash-map
                #_"String" :str str
            )
        )
    )

    (extend-type StringExpr Literal
        (#_"Object" Literal'''literal [#_"StringExpr" this]
            (:str this)
        )
    )

    (extend-type StringExpr Expr
        (#_"Object" Expr'''eval [#_"StringExpr" this]
            (Literal'''literal this)
        )

        (#_"void" Expr'''emit [#_"StringExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when-not (= context :Context'STATEMENT)
                (.push gen, (:str this))
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"StringExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"StringExpr" this]
            String
        )
    )
)

(class-ns KeywordExpr
    (defn #_"KeywordExpr" KeywordExpr'new [#_"Keyword" k]
        (merge (KeywordExpr.)
            (hash-map
                #_"Keyword" :k k
            )
        )
    )

    (extend-type KeywordExpr Literal
        (#_"Object" Literal'''literal [#_"KeywordExpr" this]
            (:k this)
        )
    )

    (declare IopObject''emitKeyword)

    (extend-type KeywordExpr Expr
        (#_"Object" Expr'''eval [#_"KeywordExpr" this]
            (Literal'''literal this)
        )

        (#_"void" Expr'''emit [#_"KeywordExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IopObject''emitKeyword objx, gen, (:k this))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"KeywordExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"KeywordExpr" this]
            Keyword
        )
    )
)

(class-ns ConstantParser
    (defn #_"IParser" ConstantParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"int" n (dec (count form))]
                    (when (= n 1) => (throw (IllegalArgumentException. (str "Wrong number of arguments passed to quote: " n)))
                        (let [#_"Object" v (second form)]
                            (cond
                                (nil? v)                                                    Compiler'NIL_EXPR
                                (= v true)                                                  Compiler'TRUE_EXPR
                                (= v false)                                                 Compiler'FALSE_EXPR
                                (number? v)                                                 (NumberExpr'parse v)
                                (string? v)                                                 (StringExpr'new v)
                                (and (instance? IPersistentCollection v) (zero? (count v))) (EmptyExpr'new v)
                                :else                                                       (ConstantExpr'new v)
                            )
                        )
                    )
                )
            )
        )
    )
)

(class-ns Interop
    (defn #_"void" Interop'emitBoxReturn [#_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Class" returnType]
        (when (.isPrimitive returnType)
            (condp = returnType
                Boolean/TYPE
                    (let [#_"Label" falseLabel (.newLabel gen) #_"Label" endLabel (.newLabel gen)]
                        (.ifZCmp gen, GeneratorAdapter/EQ, falseLabel)
                        (.getStatic gen, (Type/getType Boolean), "TRUE", (Type/getType Boolean))
                        (.goTo gen, endLabel)
                        (.mark gen, falseLabel)
                        (.getStatic gen, (Type/getType Boolean), "FALSE", (Type/getType Boolean))
                        (.mark gen, endLabel)
                    )
                Byte/TYPE      (.invokeStatic gen, (Type/getType Byte), (Method/getMethod "Byte valueOf(byte)"))
                Short/TYPE     (.invokeStatic gen, (Type/getType Short), (Method/getMethod "Short valueOf(short)"))
                Character/TYPE (.invokeStatic gen, (Type/getType Character), (Method/getMethod "Character valueOf(char)"))
                Integer/TYPE   (.invokeStatic gen, (Type/getType Integer), (Method/getMethod "Integer valueOf(int)"))
                Long/TYPE      (.invokeStatic gen, (Type/getType Numbers), (Method/getMethod "Number num(long)"))
                Float/TYPE     (.invokeStatic gen, (Type/getType Float), (Method/getMethod "Float valueOf(float)"))
                Double/TYPE    (.invokeStatic gen, (Type/getType Double), (Method/getMethod "Double valueOf(double)"))
                Void/TYPE      (Expr'''emit Compiler'NIL_EXPR, :Context'EXPRESSION, objx, gen)
            )
        )
        nil
    )

    (defn #_"void" Interop'emitUnboxArg [#_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Class" paramType]
        (when (.isPrimitive paramType) => (.checkCast gen, (Type/getType paramType))
            (condp = paramType
                Boolean/TYPE
                (do
                    (.checkCast gen, (Type/getType Boolean))
                    (.invokeVirtual gen, (Type/getType Boolean), (Method/getMethod "boolean booleanValue()"))
                )
                Character/TYPE
                (do
                    (.checkCast gen, (Type/getType Character))
                    (.invokeVirtual gen, (Type/getType Character), (Method/getMethod "char charValue()"))
                )
                (do
                    (.checkCast gen, (Type/getType Number))
                    (let [#_"Method" m
                            (condp = paramType
                                Integer/TYPE (Method/getMethod "int intCast(Object)")
                                Float/TYPE   (Method/getMethod "float floatCast(Object)")
                                Double/TYPE  (Method/getMethod "double doubleCast(Object)")
                                Long/TYPE    (Method/getMethod "long longCast(Object)")
                                Byte/TYPE    (Method/getMethod "byte byteCast(Object)")
                                Short/TYPE   (Method/getMethod "short shortCast(Object)")
                                             nil
                            )]
                        (.invokeStatic gen, (Type/getType RT), m)
                    )
                )
            )
        )
        nil
    )

    (defn #_"Class" Interop'maybeClass [#_"Object" form, #_"boolean" stringOk]
        (cond
            (instance? Class form)
                form
            (symbol? form)
                (when (nil? (ßns form)) ;; if ns-qualified can't be classname
                    (cond
                        (= form *compile-stub-sym*)
                            *compile-stub-class*
                        (or (pos? (.indexOf (ßname form), (int \.))) (= (.charAt (ßname form), 0) \[))
                            (RT/classForNameNonLoading (ßname form))
                        :else
                            (let [#_"Object" o (.getMapping *ns*, form)]
                                (cond
                                    (instance? Class o)
                                        o
                                    (contains? *local-env* form)
                                        nil
                                    :else
                                        (try
                                            (RT/classForNameNonLoading (ßname form))
                                            (catch Exception _
                                                nil
                                            )
                                        )
                                )
                            )
                    )
                )
            (and stringOk (string? form))
                (RT/classForNameNonLoading form)
        )
    )

    (defn #_"Class" Interop'primClassForName [#_"Symbol" sym]
        (when (some? sym)
            (case (ßname sym)
                "boolean" Boolean/TYPE
                "byte"    Byte/TYPE
                "short"   Short/TYPE
                "char"    Character/TYPE
                "int"     Integer/TYPE
                "long"    Long/TYPE
                "float"   Float/TYPE
                "double"  Double/TYPE
                "void"    Void/TYPE
                          nil
            )
        )
    )

    (defn #_"Class" Interop'maybeSpecialTag [#_"Symbol" sym]
        (or (Interop'primClassForName sym)
            (case (ßname sym)
                "booleans" Compiler'BOOLEANS_CLASS
                "bytes"    Compiler'BYTES_CLASS
                "shorts"   Compiler'SHORTS_CLASS
                "chars"    Compiler'CHARS_CLASS
                "ints"     Compiler'INTS_CLASS
                "longs"    Compiler'LONGS_CLASS
                "floats"   Compiler'FLOATS_CLASS
                "doubles"  Compiler'DOUBLES_CLASS
                "objects"  Compiler'OBJECTS_CLASS
                           nil
            )
        )
    )

    (defn #_"Class" Interop'tagToClass [#_"Object" tag]
        (or
            (when (and (symbol? tag) (nil? (ßns tag))) ;; if ns-qualified can't be classname
                (Interop'maybeSpecialTag tag)
            )
            (Interop'maybeClass tag, true)
            (throw (IllegalArgumentException. (str "Unable to resolve classname: " tag)))
        )
    )

    (defn #_"Class" Interop'tagClass [#_"Object" tag]
        (when (some? tag) => Object
            (or
                (when (symbol? tag)
                    (Interop'primClassForName tag)
                )
                (Interop'tagToClass tag)
            )
        )
    )
)

(class-ns InstanceFieldExpr
    (defn #_"InstanceFieldExpr" InstanceFieldExpr'new [#_"int" line, #_"Expr" target, #_"String" fieldName, #_"Symbol" tag, #_"boolean" requireField]
        (let [#_"Class" c (when (Expr'''hasJavaClass target) (Expr'''getJavaClass target))
              #_"java.lang.reflect.Field" f (when (some? c) (Reflector'getField c, fieldName, false))]
            (when (and (nil? f) *warn-on-reflection*)
                (if (nil? c)
                    (.println *err*, (str "Reflection warning, line " line " - reference to field " fieldName " can't be resolved."))
                    (.println *err*, (str "Reflection warning, line " line " - reference to field " fieldName " on " (.getName c) " can't be resolved."))
                )
            )
            (merge (InstanceFieldExpr.)
                (hash-map
                    #_"Expr" :target target
                    #_"Class" :targetClass c
                    #_"java.lang.reflect.Field" :field f
                    #_"String" :fieldName fieldName
                    #_"int" :line line
                    #_"Symbol" :tag tag
                    #_"boolean" :requireField requireField
                )
            )
        )
    )

    (extend-type InstanceFieldExpr Expr
        (#_"Object" Expr'''eval [#_"InstanceFieldExpr" this]
            (Reflector'invokeNoArgInstanceMember (Expr'''eval (:target this)), (:fieldName this), (:requireField this))
        )

        (#_"void" Expr'''emit [#_"InstanceFieldExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (if (and (some? (:targetClass this)) (some? (:field this)))
                (do
                    (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (.checkCast gen, (Compiler'getType (:targetClass this)))
                    (.getField gen, (Compiler'getType (:targetClass this)), (:fieldName this), (Type/getType (.getType (:field this))))
                    (Interop'emitBoxReturn objx, gen, (.getType (:field this)))
                    (when (= context :Context'STATEMENT)
                        (.pop gen)
                    )
                )
                (do
                    (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (.push gen, (:fieldName this))
                    (.push gen, (:requireField this))
                    (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Object invokeNoArgInstanceMember(Object, String, boolean)"))
                    (when (= context :Context'STATEMENT)
                        (.pop gen)
                    )
                )
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"InstanceFieldExpr" this]
            (or (some? (:field this)) (some? (:tag this)))
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"InstanceFieldExpr" this]
            (if (some? (:tag this)) (Interop'tagToClass (:tag this)) (.getType (:field this)))
        )
    )

    (extend-type InstanceFieldExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"InstanceFieldExpr" this]
            (and (some? (:targetClass this)) (some? (:field this)) (Reflector'isPrimitive (.getType (:field this))))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"InstanceFieldExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when (and (some? (:targetClass this)) (some? (:field this))) => (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))
                (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
                (.visitLineNumber gen, (:line this), (.mark gen))
                (.checkCast gen, (Compiler'getType (:targetClass this)))
                (.getField gen, (Compiler'getType (:targetClass this)), (:fieldName this), (Type/getType (.getType (:field this))))
            )
            nil
        )
    )

    (extend-type InstanceFieldExpr Assignable
        (#_"Object" Assignable'''evalAssign [#_"InstanceFieldExpr" this, #_"Expr" val]
            (Reflector'setInstanceField (Expr'''eval (:target this)), (:fieldName this), (Expr'''eval val))
        )

        (#_"void" Assignable'''emitAssign [#_"InstanceFieldExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Expr" val]
            (if (and (some? (:targetClass this)) (some? (:field this)))
                (do
                    (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
                    (.checkCast gen, (Compiler'getType (:targetClass this)))
                    (Expr'''emit val, :Context'EXPRESSION, objx, gen)
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (.dupX1 gen)
                    (Interop'emitUnboxArg objx, gen, (.getType (:field this)))
                    (.putField gen, (Compiler'getType (:targetClass this)), (:fieldName this), (Type/getType (.getType (:field this))))
                )
                (do
                    (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
                    (.push gen, (:fieldName this))
                    (Expr'''emit val, :Context'EXPRESSION, objx, gen)
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Object setInstanceField(Object, String, Object)"))
                )
            )
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )
    )
)

(class-ns StaticFieldExpr
    (defn #_"StaticFieldExpr" StaticFieldExpr'new [#_"int" line, #_"Class" c, #_"String" fieldName, #_"Symbol" tag]
        (merge (StaticFieldExpr.)
            (hash-map
                #_"int" :line line
                #_"Class" :c c
                #_"String" :fieldName fieldName
                #_"Symbol" :tag tag

                #_"java.lang.reflect.Field" :field (.getField c, fieldName)
            )
        )
    )

    (extend-type StaticFieldExpr Expr
        (#_"Object" Expr'''eval [#_"StaticFieldExpr" this]
            (Reflector'getStaticField (:c this), (:fieldName this))
        )

        (#_"void" Expr'''emit [#_"StaticFieldExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (.visitLineNumber gen, (:line this), (.mark gen))

            (.getStatic gen, (Type/getType (:c this)), (:fieldName this), (Type/getType (.getType (:field this))))
            (Interop'emitBoxReturn objx, gen, (.getType (:field this)))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"StaticFieldExpr" this]
            true
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"StaticFieldExpr" this]
            (if (some? (:tag this)) (Interop'tagToClass (:tag this)) (.getType (:field this)))
        )
    )

    (extend-type StaticFieldExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"StaticFieldExpr" this]
            (Reflector'isPrimitive (.getType (:field this)))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"StaticFieldExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (.visitLineNumber gen, (:line this), (.mark gen))
            (.getStatic gen, (Type/getType (:c this)), (:fieldName this), (Type/getType (.getType (:field this))))
            nil
        )
    )

    (extend-type StaticFieldExpr Assignable
        (#_"Object" Assignable'''evalAssign [#_"StaticFieldExpr" this, #_"Expr" val]
            (Reflector'setStaticField (:c this), (:fieldName this), (Expr'''eval val))
        )

        (#_"void" Assignable'''emitAssign [#_"StaticFieldExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Expr" val]
            (Expr'''emit val, :Context'EXPRESSION, objx, gen)
            (.visitLineNumber gen, (:line this), (.mark gen))
            (.dup gen)
            (Interop'emitUnboxArg objx, gen, (.getType (:field this)))
            (.putStatic gen, (Type/getType (:c this)), (:fieldName this), (Type/getType (.getType (:field this))))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )
    )
)

(class-ns MethodExpr
    (defn #_"void" MethodExpr'emitArgsAsArray [#_"IPersistentVector" args, #_"IopObject" objx, #_"GeneratorAdapter" gen]
        (.push gen, (count args))
        (.newArray gen, (Type/getType Object))
        (dotimes [#_"int" i (count args)]
            (.dup gen)
            (.push gen, i)
            (Expr'''emit (nth args i), :Context'EXPRESSION, objx, gen)
            (.arrayStore gen, (Type/getType Object))
        )
        nil
    )

    (defn #_"void" MethodExpr'emitTypedArgs [#_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Class[]" parameterTypes, #_"IPersistentVector" args]
        (dotimes [#_"int" i (alength parameterTypes)]
            (let [#_"Expr" e (nth args i) #_"Class" primc (Compiler'maybePrimitiveType e)]
                (cond
                    (= primc (aget parameterTypes i))
                        (do
                            (MaybePrimitive'''emitUnboxed e, :Context'EXPRESSION, objx, gen)
                        )
                    (and (= primc Integer/TYPE) (= (aget parameterTypes i) Long/TYPE))
                        (do
                            (MaybePrimitive'''emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            (.visitInsn gen, Opcodes/I2L)
                        )
                    (and (= primc Long/TYPE) (= (aget parameterTypes i) Integer/TYPE))
                        (do
                            (MaybePrimitive'''emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            (.invokeStatic gen, (Type/getType RT), (Method/getMethod "int intCast(long)"))
                        )
                    (and (= primc Float/TYPE) (= (aget parameterTypes i) Double/TYPE))
                        (do
                            (MaybePrimitive'''emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            (.visitInsn gen, Opcodes/F2D)
                        )
                    (and (= primc Double/TYPE) (= (aget parameterTypes i) Float/TYPE))
                        (do
                            (MaybePrimitive'''emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            (.visitInsn gen, Opcodes/D2F)
                        )
                    :else
                        (do
                            (Expr'''emit e, :Context'EXPRESSION, objx, gen)
                            (Interop'emitUnboxArg objx, gen, (aget parameterTypes i))
                        )
                )
            )
        )
        nil
    )
)

(class-ns IopMethod
    (defn #_"IopMethod" IopMethod'init [#_"IopObject" objx, #_"IopMethod" parent]
        (hash-map
            #_"IopObject" :objx objx
            ;; when closures are defined inside other closures,
            ;; the closed over locals need to be propagated to the enclosing objx
            #_"IopMethod" :parent parent
            ;; uid->localbinding
            #_"IPersistentMap" :locals {}
            #_"Expr" :body nil
            #_"PersistentVector" :argLocals nil
            #_"int" :line 0
            #_"IPersistentMap" :methodMeta nil
        )
    )

    (defn #_"void" IopMethod'emitBody [#_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Class" retClass, #_"Expr" body]
        (if (and (Reflector'isPrimitive retClass) (MaybePrimitive'''canEmitPrimitive body))
            (let [#_"Class" c (Compiler'maybePrimitiveType body)]
                (cond (= c retClass)
                    (do
                        (MaybePrimitive'''emitUnboxed body, :Context'RETURN, objx, gen)
                    )
                    (and (= retClass Long/TYPE) (= c Integer/TYPE))
                    (do
                        (MaybePrimitive'''emitUnboxed body, :Context'RETURN, objx, gen)
                        (.visitInsn gen, Opcodes/I2L)
                    )
                    (and (= retClass Double/TYPE) (= c Float/TYPE))
                    (do
                        (MaybePrimitive'''emitUnboxed body, :Context'RETURN, objx, gen)
                        (.visitInsn gen, Opcodes/F2D)
                    )
                    (and (= retClass Integer/TYPE) (= c Long/TYPE))
                    (do
                        (MaybePrimitive'''emitUnboxed body, :Context'RETURN, objx, gen)
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "int intCast(long)"))
                    )
                    (and (= retClass Float/TYPE) (= c Double/TYPE))
                    (do
                        (MaybePrimitive'''emitUnboxed body, :Context'RETURN, objx, gen)
                        (.visitInsn gen, Opcodes/D2F)
                    )
                    :else
                    (do
                        (throw (IllegalArgumentException. (str "Mismatched primitive return, expected: " retClass ", had: " (Expr'''getJavaClass body))))
                    )
                )
            )
            (do
                (Expr'''emit body, :Context'RETURN, objx, gen)
                (if (= retClass Void/TYPE)
                    (.pop gen)
                    (.unbox gen, (Type/getType retClass))
                )
            )
        )
        nil
    )

    #_method
    (defn #_"void" IopMethod''emitClearLocals [#_"IopMethod" this, #_"GeneratorAdapter" gen]
        nil
    )

    #_method
    (defn #_"void" IopMethod''emitClearThis [#_"IopMethod" this, #_"GeneratorAdapter" gen]
        (.visitInsn gen, Opcodes/ACONST_NULL)
        (.visitVarInsn gen, Opcodes/ASTORE, 0)
        nil
    )
)

(class-ns InstanceMethodExpr
    (defn #_"InstanceMethodExpr" InstanceMethodExpr'new [#_"int" line, #_"Symbol" tag, #_"Expr" target, #_"String" methodName, #_"IPersistentVector" args, #_"boolean" tailPosition]
        (let [#_"java.lang.reflect.Method" method
                (if (and (Expr'''hasJavaClass target) (some? (Expr'''getJavaClass target)))
                    (let [#_"PersistentVector" methods (Reflector'getMethods (Expr'''getJavaClass target), (count args), methodName, false)]
                        (if (zero? (count methods))
                            (do
                                (when *warn-on-reflection*
                                    (.println *err*, (str "Reflection warning, line " line " - call to method " methodName " on " (.getName (Expr'''getJavaClass target)) " can't be resolved (no such method)."))
                                )
                                nil
                            )
                            (let [#_"int" methodidx
                                    (when (< 1 (count methods)) => 0
                                        (let [[#_"PersistentVector" pars #_"PersistentVector" rets]
                                                (loop-when [pars [] rets [] #_"int" i 0] (< i (count methods)) => [pars rets]
                                                    (let [#_"java.lang.reflect.Method" m (nth methods i)]
                                                        (recur (conj pars (.getParameterTypes m)) (conj rets (.getReturnType m)) (inc i))
                                                    )
                                                )]
                                            (Compiler'getMatchingParams methodName, pars, args, rets)
                                        )
                                    )
                                #_"java.lang.reflect.Method" m (when (<= 0 methodidx) (nth methods methodidx))
                                m (when (and (some? m) (not (Modifier/isPublic (.getModifiers (.getDeclaringClass m))))) => m
                                        ;; public method of non-public class, try to find it in hierarchy
                                        (Reflector'getAsMethodOfPublicBase (.getDeclaringClass m), m)
                                    )]
                                (when (and (nil? m) *warn-on-reflection*)
                                    (.println *err*, (str "Reflection warning, line " line " - call to method " methodName " on " (.getName (Expr'''getJavaClass target)) " can't be resolved (argument types: " (Compiler'getTypeStringForArgs args) ")."))
                                )
                                m
                            )
                        )
                    )
                    (do
                        (when *warn-on-reflection*
                            (.println *err*, (str "Reflection warning, line " line " - call to method " methodName " can't be resolved (target class is unknown)."))
                        )
                        nil
                    )
                )]
            (merge (InstanceMethodExpr.)
                (hash-map
                    #_"int" :line line
                    #_"Symbol" :tag tag
                    #_"Expr" :target target
                    #_"String" :methodName methodName
                    #_"IPersistentVector" :args args
                    #_"boolean" :tailPosition tailPosition

                    #_"java.lang.reflect.Method" :method method
                )
            )
        )
    )

    (extend-type InstanceMethodExpr Expr
        (#_"Object" Expr'''eval [#_"InstanceMethodExpr" this]
            (let [#_"Object" target (Expr'''eval (:target this)) #_"Object[]" args (make-array Object (count (:args this)))]
                (dotimes [#_"int" i (count (:args this))]
                    (aset args i (Expr'''eval (nth (:args this) i)))
                )
                (if (some? (:method this))
                    (Reflector'invokeMatchingMethod (:methodName this), [(:method this)], target, args)
                    (Reflector'invokeInstanceMethod target, (:methodName this), args)
                )
            )
        )

        (#_"void" Expr'''emit [#_"InstanceMethodExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (if (some? (:method this))
                (let [#_"Type" type (Type/getType (.getDeclaringClass (:method this)))]
                    (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
                    (.checkCast gen, type)
                    (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (when (= context :Context'RETURN)
                        (IopMethod''emitClearLocals *method*, gen)
                    )
                    (let [#_"Method" m (Method. (:methodName this), (Type/getReturnType (:method this)), (Type/getArgumentTypes (:method this)))]
                        (if (.isInterface (.getDeclaringClass (:method this)))
                            (.invokeInterface gen, type, m)
                            (.invokeVirtual gen, type, m)
                        )
                        (Interop'emitBoxReturn objx, gen, (.getReturnType (:method this)))
                    )
                )
                (do
                    (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
                    (.push gen, (:methodName this))
                    (MethodExpr'emitArgsAsArray (:args this), objx, gen)
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (when (= context :Context'RETURN)
                        (IopMethod''emitClearLocals *method*, gen)
                    )
                    (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Object invokeInstanceMethod(Object, String, Object[])"))
                )
            )
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"InstanceMethodExpr" this]
            (or (some? (:method this)) (some? (:tag this)))
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"InstanceMethodExpr" this]
            (Compiler'retType (when (some? (:tag this)) (Interop'tagToClass (:tag this))), (when (some? (:method this)) (.getReturnType (:method this))))
        )
    )

    (extend-type InstanceMethodExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"InstanceMethodExpr" this]
            (and (some? (:method this)) (Reflector'isPrimitive (.getReturnType (:method this))))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"InstanceMethodExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when (some? (:method this)) => (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))
                (let [#_"Type" type (Type/getType (.getDeclaringClass (:method this)))]
                    (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
                    (.checkCast gen, type)
                    (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (when (:tailPosition this)
                        (IopMethod''emitClearThis *method*, gen)
                    )
                    (let [#_"Method" m (Method. (:methodName this), (Type/getReturnType (:method this)), (Type/getArgumentTypes (:method this)))]
                        (if (.isInterface (.getDeclaringClass (:method this)))
                            (.invokeInterface gen, type, m)
                            (.invokeVirtual gen, type, m)
                        )
                    )
                )
            )
            nil
        )
    )
)

(class-ns StaticMethodExpr
    (defn #_"StaticMethodExpr" StaticMethodExpr'new [#_"int" line, #_"Symbol" tag, #_"Class" c, #_"String" methodName, #_"IPersistentVector" args, #_"boolean" tailPosition]
        (let [#_"java.lang.reflect.Method" method
                (let [#_"PersistentVector" methods (Reflector'getMethods c, (count args), methodName, true)]
                    (when-not (zero? (count methods)) => (throw (IllegalArgumentException. (str "No matching method: " methodName)))
                        (let [#_"int" methodidx
                                (when (< 1 (count methods)) => 0
                                    (let [[#_"PersistentVector" pars #_"PersistentVector" rets]
                                            (loop-when [pars [] rets [] #_"int" i 0] (< i (count methods)) => [pars rets]
                                                (let [#_"java.lang.reflect.Method" m (nth methods i)]
                                                    (recur (conj pars (.getParameterTypes m)) (conj rets (.getReturnType m)) (inc i))
                                                )
                                            )]
                                        (Compiler'getMatchingParams methodName, pars, args, rets)
                                    )
                                )
                              #_"java.lang.reflect.Method" m (when (<= 0 methodidx) (nth methods methodidx))]
                            (when (and (nil? m) *warn-on-reflection*)
                                (.println *err*, (str "Reflection warning, line " line " - call to static method " methodName " on " (.getName c) " can't be resolved (argument types: " (Compiler'getTypeStringForArgs args) ")."))
                            )
                            m
                        )
                    )
                )]
            (merge (StaticMethodExpr.)
                (hash-map
                    #_"int" :line line
                    #_"Symbol" :tag tag
                    #_"Class" :c c
                    #_"String" :methodName methodName
                    #_"IPersistentVector" :args args
                    #_"boolean" :tailPosition tailPosition

                    #_"java.lang.reflect.Method" :method method
                )
            )
        )
    )

    #_method
    (defn #_"boolean" StaticMethodExpr''canEmitIntrinsicPredicate [#_"StaticMethodExpr" this]
        (and (some? (:method this)) (some? (get Intrinsics'preds (.toString (:method this)))))
    )

    #_method
    (defn #_"void" StaticMethodExpr''emitIntrinsicPredicate [#_"StaticMethodExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Label" falseLabel]
        (.visitLineNumber gen, (:line this), (.mark gen))
        (when (some? (:method this)) => (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))
            (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
            (when (= context :Context'RETURN)
                (IopMethod''emitClearLocals *method*, gen)
            )
            (let [#_"[int]" preds (get Intrinsics'preds (.toString (:method this)))]
                (doseq [#_"int" pred (pop preds)]
                    (.visitInsn gen, pred)
                )
                (.visitJumpInsn gen, (peek preds), falseLabel)
            )
        )
        nil
    )

    (extend-type StaticMethodExpr Expr
        (#_"Object" Expr'''eval [#_"StaticMethodExpr" this]
            (let [#_"Object[]" args (make-array Object (count (:args this)))]
                (dotimes [#_"int" i (count (:args this))]
                    (aset args i (Expr'''eval (nth (:args this) i)))
                )
                (if (some? (:method this))
                    (Reflector'invokeMatchingMethod (:methodName this), [(:method this)], nil, args)
                    (Reflector'invokeStaticMethod (:c this), (:methodName this), args)
                )
            )
        )

        (#_"void" Expr'''emit [#_"StaticMethodExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (if (some? (:method this))
                (do
                    (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (when (:tailPosition this)
                        (IopMethod''emitClearThis *method*, gen)
                    )
                    (let [#_"Type" type (Type/getType (:c this))
                        #_"Method" m (Method. (:methodName this), (Type/getReturnType (:method this)), (Type/getArgumentTypes (:method this)))]
                        (.invokeStatic gen, type, m)
                        (when (= context :Context'STATEMENT) => (Interop'emitBoxReturn objx, gen, (.getReturnType (:method this)))
                            (let [#_"Class" rc (.getReturnType (:method this))]
                                (cond
                                    (any = rc Long/TYPE Double/TYPE) (.pop2 gen)
                                    (not (= rc Void/TYPE))           (.pop gen)
                                )
                            )
                        )
                    )
                )
                (do
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (.push gen, (.getName (:c this)))
                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "Class classForName(String)"))
                    (.push gen, (:methodName this))
                    (MethodExpr'emitArgsAsArray (:args this), objx, gen)
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (when (= context :Context'RETURN)
                        (IopMethod''emitClearLocals *method*, gen)
                    )
                    (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Object invokeStaticMethod(Class, String, Object[])"))
                    (when (= context :Context'STATEMENT)
                        (.pop gen)
                    )
                )
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"StaticMethodExpr" this]
            (or (some? (:method this)) (some? (:tag this)))
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"StaticMethodExpr" this]
            (Compiler'retType (when (some? (:tag this)) (Interop'tagToClass (:tag this))), (when (some? (:method this)) (.getReturnType (:method this))))
        )
    )

    (extend-type StaticMethodExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"StaticMethodExpr" this]
            (and (some? (:method this)) (Reflector'isPrimitive (.getReturnType (:method this))))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"StaticMethodExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when (some? (:method this)) => (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))
                (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
                (.visitLineNumber gen, (:line this), (.mark gen))
                (when (= context :Context'RETURN)
                    (IopMethod''emitClearLocals *method*, gen)
                )
                (let [#_"int|[int]" ops (get Intrinsics'ops (.toString (:method this)))]
                    (if (some? ops)
                        (if (vector? ops)
                            (doseq [#_"int" op ops]
                                (.visitInsn gen, op)
                            )
                            (.visitInsn gen, ops)
                        )
                        (let [#_"Method" m (Method. (:methodName this), (Type/getReturnType (:method this)), (Type/getArgumentTypes (:method this)))]
                            (.invokeStatic gen, (Type/getType (:c this)), m)
                        )
                    )
                )
            )
            nil
        )
    )
)

(class-ns HostParser
    (defn #_"IParser" HostParser'new []
        (reify IParser
            ;; (. x fieldname-sym) or
            ;; (. x 0-ary-method)
            ;; (. x methodname-sym args+)
            ;; (. x (methodname-sym args?))
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when-not (< (count form) 3) => (throw (IllegalArgumentException. "Malformed member expression, expecting (. target member ...)"))
                    ;; determine static or instance
                    ;; static target must be symbol, either fully.qualified.Classname or Classname that has been imported
                    (let [#_"int" line *line* #_"Class" c (Interop'maybeClass (second form), false)
                          ;; at this point c will be non-null if static
                          #_"Expr" instance (when (nil? c) (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (second form)))
                          #_"boolean" maybeField (and (= (count form) 3) (symbol? (third form)))
                          maybeField
                            (when (and maybeField (not= (.charAt (ßname (third form)), 0) \-)) => maybeField
                                (let [#_"String" name (ßname (third form))]
                                    (cond
                                        (some? c)
                                            (zero? (count (Reflector'getMethods c, 0, (Compiler'munge name), true)))
                                        (and (some? instance) (Expr'''hasJavaClass instance) (some? (Expr'''getJavaClass instance)))
                                            (zero? (count (Reflector'getMethods (Expr'''getJavaClass instance), 0, (Compiler'munge name), false)))
                                        :else
                                            maybeField
                                    )
                                )
                            )]
                        (if maybeField
                            (let [? (= (.charAt (ßname (third form)), 0) \-)
                                  #_"Symbol" sym (if ? (symbol (.substring (ßname (third form)), 1)) (third form))
                                  #_"Symbol" tag (Compiler'tagOf form)]
                                (if (some? c)
                                    (StaticFieldExpr'new line, c, (Compiler'munge (ßname sym)), tag)
                                    (InstanceFieldExpr'new line, instance, (Compiler'munge (ßname sym)), tag, ?)
                                )
                            )
                            (let [#_"ISeq" call (if (instance? ISeq (third form)) (third form) (next (next form)))]
                                (when (symbol? (first call)) => (throw (IllegalArgumentException. "Malformed member expression"))
                                    (let [#_"Symbol" sym (first call)
                                          #_"Symbol" tag (Compiler'tagOf form)
                                          #_"boolean" tailPosition (Compiler'inTailCall context)
                                          #_"PersistentVector" args
                                            (loop-when-recur [args [] #_"ISeq" s (next call)]
                                                             (some? s)
                                                             [(conj args (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (first s))) (next s)]
                                                          => args
                                            )]
                                        (if (some? c)
                                            (StaticMethodExpr'new line, tag, c, (Compiler'munge (ßname sym)), args, tailPosition)
                                            (InstanceMethodExpr'new line, tag, instance, (Compiler'munge (ßname sym)), args, tailPosition)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)

(class-ns UnresolvedVarExpr
    (defn #_"UnresolvedVarExpr" UnresolvedVarExpr'new [#_"Symbol" symbol]
        (merge (UnresolvedVarExpr.)
            (hash-map
                #_"Symbol" :symbol symbol
            )
        )
    )

    (extend-type UnresolvedVarExpr Expr
        (#_"Object" Expr'''eval [#_"UnresolvedVarExpr" this]
            (throw (IllegalArgumentException. "UnresolvedVarExpr cannot be evalled"))
        )

        (#_"void" Expr'''emit [#_"UnresolvedVarExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"UnresolvedVarExpr" this]
            false
        )

        (#_"Class" Expr'''getJavaClass [#_"UnresolvedVarExpr" this]
            (throw (IllegalArgumentException. "UnresolvedVarExpr has no Java class"))
        )
    )
)

(class-ns VarExpr
    (defn #_"VarExpr" VarExpr'new [#_"Var" var, #_"Symbol" tag]
        (merge (VarExpr.)
            (hash-map
                #_"Var" :var var
                #_"Object" :tag (or tag (get (meta var) :tag))
            )
        )
    )

    (declare IopObject''emitVarValue)

    (extend-type VarExpr Expr
        (#_"Object" Expr'''eval [#_"VarExpr" this]
            (deref (:var this))
        )

        (#_"void" Expr'''emit [#_"VarExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IopObject''emitVarValue objx, gen, (:var this))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"VarExpr" this]
            (some? (:tag this))
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"VarExpr" this]
            (Interop'tagToClass (:tag this))
        )
    )

    (declare IopObject''emitVar)

    (extend-type VarExpr Assignable
        (#_"Object" Assignable'''evalAssign [#_"VarExpr" this, #_"Expr" val]
            (var-set (:var this) (Expr'''eval val))
        )

        (#_"void" Assignable'''emitAssign [#_"VarExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Expr" val]
            (IopObject''emitVar objx, gen, (:var this))
            (Expr'''emit val, :Context'EXPRESSION, objx, gen)
            (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "Object set(Object)"))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )
    )
)

(class-ns TheVarExpr
    (defn #_"TheVarExpr" TheVarExpr'new [#_"Var" var]
        (merge (TheVarExpr.)
            (hash-map
                #_"Var" :var var
            )
        )
    )

    (extend-type TheVarExpr Expr
        (#_"Object" Expr'''eval [#_"TheVarExpr" this]
            (:var this)
        )

        (#_"void" Expr'''emit [#_"TheVarExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IopObject''emitVar objx, gen, (:var this))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"TheVarExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"TheVarExpr" this]
            Var
        )
    )
)

(class-ns TheVarParser
    (defn #_"IParser" TheVarParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"Symbol" sym (second form) #_"Var" v (Compiler'lookupVar sym, false)]
                    (when (some? v) => (throw (RuntimeException. (str "Unable to resolve var: " sym " in this context")))
                        (TheVarExpr'new v)
                    )
                )
            )
        )
    )
)

(class-ns BodyExpr
    (defn #_"BodyExpr" BodyExpr'new [#_"PersistentVector" exprs]
        (merge (BodyExpr.)
            (hash-map
                #_"PersistentVector" :exprs exprs
            )
        )
    )

    #_method
    (defn- #_"Expr" BodyExpr''lastExpr [#_"BodyExpr" this]
        (nth (:exprs this) (dec (count (:exprs this))))
    )

    (extend-type BodyExpr Expr
        (#_"Object" Expr'''eval [#_"BodyExpr" this]
            (let [#_"Iterator" it (.iterator (:exprs this))]
                (loop-when-recur [#_"Object" ret nil] (.hasNext it) [(Expr'''eval (.next it))] => ret)
            )
        )

        (#_"void" Expr'''emit [#_"BodyExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (dotimes [#_"int" i (dec (count (:exprs this)))]
                (Expr'''emit (nth (:exprs this) i), :Context'STATEMENT, objx, gen)
            )
            (Expr'''emit (BodyExpr''lastExpr this), context, objx, gen)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"BodyExpr" this]
            (Expr'''hasJavaClass (BodyExpr''lastExpr this))
        )

        (#_"Class" Expr'''getJavaClass [#_"BodyExpr" this]
            (Expr'''getJavaClass (BodyExpr''lastExpr this))
        )
    )

    (extend-type BodyExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"BodyExpr" this]
            (let [#_"Expr" e (BodyExpr''lastExpr this)]
                (and (satisfies? MaybePrimitive e) (MaybePrimitive'''canEmitPrimitive e))
            )
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"BodyExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (dotimes [#_"int" i (dec (count (:exprs this)))]
                (Expr'''emit (nth (:exprs this) i), :Context'STATEMENT, objx, gen)
            )
            (MaybePrimitive'''emitUnboxed (BodyExpr''lastExpr this), context, objx, gen)
            nil
        )
    )
)

(class-ns BodyParser
    (defn #_"IParser" BodyParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"ISeq" s form s (if (= (first s) 'do) (next s) s)
                      #_"PersistentVector" v
                        (loop-when [v [] s s] (some? s) => v
                            (let [#_"Context" c (if (and (not= context :Context'EVAL) (or (= context :Context'STATEMENT) (some? (next s)))) :Context'STATEMENT context)]
                                (recur (conj v (Compiler'analyze c, (first s))) (next s))
                            )
                        )]
                    (BodyExpr'new (if (pos? (count v)) v (conj v Compiler'NIL_EXPR)))
                )
            )
        )
    )
)

(class-ns CatchClause
    (defn #_"CatchClause" CatchClause'new [#_"Class" c, #_"LocalBinding" lb, #_"Expr" handler]
        (merge (CatchClause.)
            (hash-map
                #_"Class" :c c
                #_"LocalBinding" :lb lb
                #_"Expr" :handler handler
            )
        )
    )
)

(class-ns TryExpr
    (defn #_"TryExpr" TryExpr'new [#_"Expr" tryExpr, #_"PersistentVector" catchExprs, #_"Expr" finallyExpr]
        (merge (TryExpr.)
            (hash-map
                #_"Expr" :tryExpr tryExpr
                #_"PersistentVector" :catchExprs catchExprs
                #_"Expr" :finallyExpr finallyExpr

                #_"int" :retLocal (Compiler'nextLocalNum)
                #_"int" :finallyLocal (Compiler'nextLocalNum)
            )
        )
    )

    (extend-type TryExpr Expr
        (#_"Object" Expr'''eval [#_"TryExpr" this]
            (throw (UnsupportedOperationException. "Can't eval try"))
        )

        (#_"void" Expr'''emit [#_"TryExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (let [#_"Label" startTry (.newLabel gen) #_"Label" endTry (.newLabel gen) #_"Label" end (.newLabel gen) #_"Label" ret (.newLabel gen) #_"Label" finallyLabel (.newLabel gen)
                #_"int" n (count (:catchExprs this)) #_"Label[]" labels (make-array Label n) #_"Label[]" endLabels (make-array Label n)]
                (dotimes [#_"int" i n]
                    (aset labels i (.newLabel gen))
                    (aset endLabels i (.newLabel gen))
                )

                (.mark gen, startTry)
                (Expr'''emit (:tryExpr this), context, objx, gen)
                (when-not (= context :Context'STATEMENT)
                    (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:retLocal this))
                )
                (.mark gen, endTry)
                (when (some? (:finallyExpr this))
                    (Expr'''emit (:finallyExpr this), :Context'STATEMENT, objx, gen)
                )
                (.goTo gen, ret)

                (dotimes [#_"int" i n]
                    (let [#_"CatchClause" clause (nth (:catchExprs this) i)]
                        (.mark gen, (aget labels i))
                        ;; exception should be on stack
                        ;; put in clause local
                        (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx (:lb clause)))
                        (Expr'''emit (:handler clause), context, objx, gen)
                        (when-not (= context :Context'STATEMENT)
                            (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:retLocal this))
                        )
                        (.mark gen, (aget endLabels i))

                        (when (some? (:finallyExpr this))
                            (Expr'''emit (:finallyExpr this), :Context'STATEMENT, objx, gen)
                        )
                        (.goTo gen, ret)
                    )
                )
                (when (some? (:finallyExpr this))
                    (.mark gen, finallyLabel)
                    ;; exception should be on stack
                    (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:finallyLocal this))
                    (Expr'''emit (:finallyExpr this), :Context'STATEMENT, objx, gen)
                    (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), (:finallyLocal this))
                    (.throwException gen)
                )
                (.mark gen, ret)
                (when-not (= context :Context'STATEMENT)
                    (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), (:retLocal this))
                )
                (.mark gen, end)
                (dotimes [#_"int" i n]
                    (let [#_"CatchClause" clause (nth (:catchExprs this) i)]
                        (.visitTryCatchBlock gen, startTry, endTry, (aget labels i), (.replace (.getName (:c clause)), \., \/))
                    )
                )
                (when (some? (:finallyExpr this))
                    (.visitTryCatchBlock gen, startTry, endTry, finallyLabel, nil)
                    (dotimes [#_"int" i n]
                        (let [#_"CatchClause" _clause (nth (:catchExprs this) i)]
                            (.visitTryCatchBlock gen, (aget labels i), (aget endLabels i), finallyLabel, nil)
                        )
                    )
                )
                (dotimes [#_"int" i n]
                    (let [#_"CatchClause" clause (nth (:catchExprs this) i)]
                        (.visitLocalVariable gen, (:name (:lb clause)), "Ljava/lang/Object;", nil, (aget labels i), (aget endLabels i), (:idx (:lb clause)))
                    )
                )
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"TryExpr" this]
            (Expr'''hasJavaClass (:tryExpr this))
        )

        (#_"Class" Expr'''getJavaClass [#_"TryExpr" this]
            (Expr'''getJavaClass (:tryExpr this))
        )
    )
)

(class-ns TryParser
    (defn #_"IParser" TryParser'new []
        (reify IParser
            ;; (try try-expr* catch-expr* finally-expr?)
            ;; catch-expr: (catch class sym expr*)
            ;; finally-expr: (finally expr*)
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (= context :Context'RETURN) => (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                    (let [[#_"Expr" bodyExpr #_"PersistentVector" catches #_"Expr" finallyExpr #_"PersistentVector" body]
                            (loop-when [bodyExpr nil catches [] finallyExpr nil body [] #_"boolean" caught? false #_"ISeq" fs (next form)] (some? fs) => [bodyExpr catches finallyExpr body]
                                (let [#_"Object" f (first fs) #_"Object" op (when (instance? ISeq f) (first f))]
                                    (if (any = op 'catch 'finally)
                                        (let [bodyExpr
                                                (when (nil? bodyExpr) => bodyExpr
                                                    (binding [*no-recur* true, *in-return-context* false]
                                                        (IParser'''parse (BodyParser'new), context, (seq body))
                                                    )
                                                )]
                                            (if (= op 'catch)
                                                (let-when [#_"Class" c (Interop'maybeClass (second f), false)] (some? c) => (throw (IllegalArgumentException. (str "Unable to resolve classname: " (second f))))
                                                    (let-when [#_"Symbol" sym (third f)] (symbol? sym) => (throw (IllegalArgumentException. (str "Bad binding form, expected symbol, got: " sym)))
                                                        (when (nil? (namespace sym)) => (throw (RuntimeException. (str "Can't bind qualified name: " sym)))
                                                            (let [catches
                                                                    (binding [*local-env* *local-env*, *last-local-num* *last-local-num*, *in-catch-finally* true]
                                                                        (let [#_"LocalBinding" lb (Compiler'registerLocal sym, (when (symbol? (second f)) (second f)), nil, false)
                                                                              #_"Expr" handler (IParser'''parse (BodyParser'new), :Context'EXPRESSION, (next (next (next f))))]
                                                                            (conj catches (CatchClause'new c, lb, handler))
                                                                        )
                                                                    )]
                                                                (recur bodyExpr catches finallyExpr body true (next fs))
                                                            )
                                                        )
                                                    )
                                                )
                                                (when (nil? (next fs)) => (throw (RuntimeException. "finally clause must be last in try expression"))
                                                    (let [finallyExpr
                                                            (binding [*in-catch-finally* true]
                                                                (IParser'''parse (BodyParser'new), :Context'STATEMENT, (next f))
                                                            )]
                                                        (recur bodyExpr catches finallyExpr body caught? (next fs))
                                                    )
                                                )
                                            )
                                        )
                                        (when-not caught? => (throw (RuntimeException. "Only catch or finally clause can follow catch in try expression"))
                                            (recur bodyExpr catches finallyExpr (conj body f) caught? (next fs))
                                        )
                                    )
                                )
                            )]
                        (when (nil? bodyExpr) => (TryExpr'new bodyExpr, catches, finallyExpr)
                            ;; when there is neither catch nor finally, e.g. (try (expr)) return a body expr directly
                            (binding [*no-recur* true]
                                (IParser'''parse (BodyParser'new), context, (seq body))
                            )
                        )
                    )
                )
            )
        )
    )
)

(class-ns ThrowExpr
    (defn #_"ThrowExpr" ThrowExpr'new [#_"Expr" excExpr]
        (merge (ThrowExpr.)
            (hash-map
                #_"Expr" :excExpr excExpr
            )
        )
    )

    (extend-type ThrowExpr Expr
        (#_"Object" Expr'''eval [#_"ThrowExpr" this]
            (throw (RuntimeException. "Can't eval throw"))
        )

        (#_"void" Expr'''emit [#_"ThrowExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit (:excExpr this), :Context'EXPRESSION, objx, gen)
            (.checkCast gen, (Type/getType Throwable))
            (.throwException gen)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"ThrowExpr" this]
            false
        )

        (#_"Class" Expr'''getJavaClass [#_"ThrowExpr" this]
            (throw (UnsupportedOperationException. "Has no Java class"))
        )
    )
)

(class-ns ThrowParser
    (defn #_"IParser" ThrowParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (cond
                    (= context :Context'EVAL) (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                    (= (count form) 1)        (throw (IllegalArgumentException. "Too few arguments to throw, throw expects a single Throwable instance"))
                    (< 2 (count form))        (throw (IllegalArgumentException. "Too many arguments to throw, throw expects a single Throwable instance"))
                    :else                     (ThrowExpr'new (Compiler'analyze :Context'EXPRESSION, (second form)))
                )
            )
        )
    )
)

(class-ns NewExpr
    (defn #_"NewExpr" NewExpr'new [#_"Class" c, #_"IPersistentVector" args, #_"int" line]
        (let [#_"Constructor" ctor
                (let [#_"Constructor[]" allctors (.getConstructors c)
                      [#_"PersistentVector" ctors #_"PersistentVector" pars #_"PersistentVector" rets]
                        (loop-when [ctors [] pars [] rets [] #_"int" i 0] (< i (alength allctors)) => [ctors pars rets]
                            (let [#_"Constructor" ctor (aget allctors i) #_"Class[]" types (.getParameterTypes ctor)
                                  [ctors pars rets]
                                    (when (= (alength types) (count args)) => [ctors pars rets]
                                        [(conj ctors ctor) (conj pars types) (conj rets c)]
                                    )]
                                (recur ctors pars rets (inc i))
                            )
                        )]
                    (let-when [#_"int" n (count ctors)] (< 0 n) => (throw (IllegalArgumentException. (str "No matching ctor found for " c)))
                        (let [#_"int" i (if (< 1 n) (Compiler'getMatchingParams (.getName c), pars, args, rets) 0)
                              #_"Constructor" ctor (when (<= 0 i) (nth ctors i))]
                            (when (and (nil? ctor) *warn-on-reflection*)
                                (.println *err*, (str "Reflection warning, line " line " - call to " (.getName c) " ctor can't be resolved."))
                            )
                            ctor
                        )
                    )
                )]
            (merge (NewExpr.)
                (hash-map
                    #_"IPersistentVector" :args args
                    #_"Constructor" :ctor ctor
                    #_"Class" :c c
                )
            )
        )
    )

    (extend-type NewExpr Expr
        (#_"Object" Expr'''eval [#_"NewExpr" this]
            (let [#_"Object[]" args (make-array Object (count (:args this)))]
                (dotimes [#_"int" i (count (:args this))]
                    (aset args i (Expr'''eval (nth (:args this) i)))
                )
                (when (some? (:ctor this)) => (Reflector'invokeConstructor (:c this), args)
                    (.newInstance (:ctor this), (Reflector'boxArgs (.getParameterTypes (:ctor this)), args))
                )
            )
        )

        (#_"void" Expr'''emit [#_"NewExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (if (some? (:ctor this))
                (let [#_"Type" type (Compiler'getType (:c this))]
                    (.newInstance gen, type)
                    (.dup gen)
                    (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:ctor this)), (:args this))
                    (.invokeConstructor gen, type, (Method. "<init>", (Type/getConstructorDescriptor (:ctor this))))
                )
                (do
                    (.push gen, (Compiler'destubClassName (.getName (:c this))))
                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "Class classForName(String)"))
                    (MethodExpr'emitArgsAsArray (:args this), objx, gen)
                    (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Object invokeConstructor(Class, Object[])"))
                )
            )
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"NewExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"NewExpr" this]
            (:c this)
        )
    )
)

(class-ns NewParser
    (defn #_"IParser" NewParser'new []
        (reify IParser
            ;; (new Classname args...)
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"int" line *line*]
                    (when (< 1 (count form)) => (throw (IllegalArgumentException. "Wrong number of arguments, expecting: (new Classname args...)"))
                        (let [#_"Class" c (Interop'maybeClass (second form), false)]
                            (when (some? c) => (throw (IllegalArgumentException. (str "Unable to resolve classname: " (second form))))
                                (let [#_"PersistentVector" args
                                        (loop-when-recur [args [] #_"ISeq" s (next (next form))]
                                                         (some? s)
                                                         [(conj args (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (first s))) (next s)]
                                                      => args
                                        )]
                                    (NewExpr'new c, args, line)
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)

(class-ns MetaExpr
    (defn #_"MetaExpr" MetaExpr'new [#_"Expr" expr, #_"Expr" meta]
        (merge (MetaExpr.)
            (hash-map
                #_"Expr" :expr expr
                #_"Expr" :meta meta
            )
        )
    )

    (extend-type MetaExpr Expr
        (#_"Object" Expr'''eval [#_"MetaExpr" this]
            (with-meta (Expr'''eval (:expr this)) (Expr'''eval (:meta this)))
        )

        (#_"void" Expr'''emit [#_"MetaExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit (:expr this), :Context'EXPRESSION, objx, gen)
            (.checkCast gen, (Type/getType IObj))
            (Expr'''emit (:meta this), :Context'EXPRESSION, objx, gen)
            (.checkCast gen, (Type/getType IPersistentMap))
            (.invokeInterface gen, (Type/getType IObj), (Method/getMethod "clojure.lang.IObj withMeta(clojure.lang.IPersistentMap)"))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"MetaExpr" this]
            (Expr'''hasJavaClass (:expr this))
        )

        (#_"Class" Expr'''getJavaClass [#_"MetaExpr" this]
            (Expr'''getJavaClass (:expr this))
        )
    )
)

(class-ns IfExpr
    (defn #_"IfExpr" IfExpr'new [#_"int" line, #_"Expr" testExpr, #_"Expr" thenExpr, #_"Expr" elseExpr]
        (merge (IfExpr.)
            (hash-map
                #_"int" :line line
                #_"Expr" :testExpr testExpr
                #_"Expr" :thenExpr thenExpr
                #_"Expr" :elseExpr elseExpr
            )
        )
    )

    #_method
    (defn- #_"void" IfExpr''doEmit [#_"IfExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"boolean" emitUnboxed]
        (let [#_"Label" nullLabel (.newLabel gen) #_"Label" falseLabel (.newLabel gen) #_"Label" endLabel (.newLabel gen)]
            (.visitLineNumber gen, (:line this), (.mark gen))

            (cond (and (instance? StaticMethodExpr (:testExpr this)) (StaticMethodExpr''canEmitIntrinsicPredicate (:testExpr this)))
                (do
                    (StaticMethodExpr''emitIntrinsicPredicate (:testExpr this), :Context'EXPRESSION, objx, gen, falseLabel)
                )
                (= (Compiler'maybePrimitiveType (:testExpr this)) Boolean/TYPE)
                (do
                    (MaybePrimitive'''emitUnboxed (:testExpr this), :Context'EXPRESSION, objx, gen)
                    (.ifZCmp gen, GeneratorAdapter/EQ, falseLabel)
                )
                :else
                (do
                    (Expr'''emit (:testExpr this), :Context'EXPRESSION, objx, gen)
                    (.dup gen)
                    (.ifNull gen, nullLabel)
                    (.getStatic gen, (Type/getType Boolean), "FALSE", (Type/getType Boolean))
                    (.visitJumpInsn gen, Opcodes/IF_ACMPEQ, falseLabel)
                )
            )
            (if emitUnboxed
                (MaybePrimitive'''emitUnboxed (:thenExpr this), context, objx, gen)
                (Expr'''emit (:thenExpr this), context, objx, gen)
            )
            (.goTo gen, endLabel)
            (.mark gen, nullLabel)
            (.pop gen)
            (.mark gen, falseLabel)
            (if emitUnboxed
                (MaybePrimitive'''emitUnboxed (:elseExpr this), context, objx, gen)
                (Expr'''emit (:elseExpr this), context, objx, gen)
            )
            (.mark gen, endLabel)
        )
        nil
    )

    (extend-type IfExpr Expr
        (#_"Object" Expr'''eval [#_"IfExpr" this]
            (let [#_"Object" t (Expr'''eval (:testExpr this))]
                (if (and (some? t) (not= t false))
                    (Expr'''eval (:thenExpr this))
                    (Expr'''eval (:elseExpr this))
                )
            )
        )

        (#_"void" Expr'''emit [#_"IfExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IfExpr''doEmit this, context, objx, gen, false)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"IfExpr" this]
            (let [#_"Expr" then (:thenExpr this) #_"Expr" else (:elseExpr this)]
                (and (Expr'''hasJavaClass then)
                    (Expr'''hasJavaClass else)
                    (or (= (Expr'''getJavaClass then) (Expr'''getJavaClass else))
                        (= (Expr'''getJavaClass then) Recur)
                        (= (Expr'''getJavaClass else) Recur)
                        (and (nil? (Expr'''getJavaClass then)) (not (.isPrimitive (Expr'''getJavaClass else))))
                        (and (nil? (Expr'''getJavaClass else)) (not (.isPrimitive (Expr'''getJavaClass then))))))
            )
        )

        (#_"Class" Expr'''getJavaClass [#_"IfExpr" this]
            (let [#_"Class" thenClass (Expr'''getJavaClass (:thenExpr this))]
                (if (and (some? thenClass) (not= thenClass Recur))
                    thenClass
                    (Expr'''getJavaClass (:elseExpr this))
                )
            )
        )
    )

    (extend-type IfExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"IfExpr" this]
            (try
                (let [#_"Expr" then (:thenExpr this) #_"Expr" else (:elseExpr this)]
                    (and (satisfies? MaybePrimitive then)
                        (satisfies? MaybePrimitive else)
                        (or (= (Expr'''getJavaClass then) (Expr'''getJavaClass else))
                            (= (Expr'''getJavaClass then) Recur)
                            (= (Expr'''getJavaClass else) Recur))
                        (MaybePrimitive'''canEmitPrimitive then)
                        (MaybePrimitive'''canEmitPrimitive else))
                )
                (catch Exception _
                    false
                )
            )
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"IfExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IfExpr''doEmit this, context, objx, gen, true)
            nil
        )
    )
)

(class-ns IfParser
    (defn #_"IParser" IfParser'new []
        (reify IParser
            ;; (if test then) or (if test then else)
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (cond
                    (< 4 (count form)) (throw (IllegalArgumentException. "Too many arguments to if"))
                    (< (count form) 3) (throw (IllegalArgumentException. "Too few arguments to if"))
                )
                (let [#_"Expr" test (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (second form))
                      #_"Expr" then (Compiler'analyze context, (third form))
                      #_"Expr" else (Compiler'analyze context, (fourth form))]
                    (IfExpr'new *line*, test, then, else)
                )
            )
        )
    )
)

(class-ns ListExpr
    (defn #_"ListExpr" ListExpr'new [#_"IPersistentVector" args]
        (merge (ListExpr.)
            (hash-map
                #_"IPersistentVector" :args args
            )
        )
    )

    (extend-type ListExpr Expr
        (#_"Object" Expr'''eval [#_"ListExpr" this]
            (loop-when-recur [#_"IPersistentVector" v [] #_"int" i 0]
                            (< i (count (:args this)))
                            [(conj v (Expr'''eval (nth (:args this) i))) (inc i)]
                        => (seq v)
            )
        )

        (#_"void" Expr'''emit [#_"ListExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (MethodExpr'emitArgsAsArray (:args this), objx, gen)
            (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.ISeq arrayToList(Object[])"))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"ListExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"ListExpr" this]
            IPersistentList
        )
    )
)

(class-ns MapExpr
    (defn #_"MapExpr" MapExpr'new [#_"IPersistentVector" keyvals]
        (merge (MapExpr.)
            (hash-map
                #_"IPersistentVector" :keyvals keyvals
            )
        )
    )

    (extend-type MapExpr Expr
        (#_"Object" Expr'''eval [#_"MapExpr" this]
            (let [#_"Object[]" a (make-array Object (count (:keyvals this)))]
                (dotimes [#_"int" i (count (:keyvals this))]
                    (aset a i (Expr'''eval (nth (:keyvals this) i)))
                )
                (RT/map a)
            )
        )

        (#_"void" Expr'''emit [#_"MapExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (let [[#_"boolean" allKeysConstant #_"boolean" allConstantKeysUnique]
                    (loop-when [constant? true unique? true #_"IPersistentSet" keys #{} #_"int" i 0] (< i (count (:keyvals this))) => [constant? unique?]
                        (let [#_"Expr" k (nth (:keyvals this) i)
                            [constant? unique? keys]
                                (when (satisfies? Literal k) => [false unique? keys]
                                    (let-when-not [#_"Object" v (Expr'''eval k)] (contains? keys v) => [constant? false keys]
                                        [constant? unique? (conj keys v)]
                                    )
                                )]
                            (recur constant? unique? keys (+ i 2))
                        )
                    )]
                (MethodExpr'emitArgsAsArray (:keyvals this), objx, gen)
                (if (or (and allKeysConstant allConstantKeysUnique) (<= (count (:keyvals this)) 2))
                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.IPersistentMap mapUniqueKeys(Object[])"))
                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.IPersistentMap map(Object[])"))
                )
                (when (= context :Context'STATEMENT)
                    (.pop gen)
                )
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"MapExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"MapExpr" this]
            IPersistentMap
        )
    )

    (defn #_"Expr" MapExpr'parse [#_"Context" context, #_"IPersistentMap" form]
        (let [#_"Context" c (if (= context :Context'EVAL) context :Context'EXPRESSION)
              [#_"IPersistentVector" keyvals #_"boolean" keysConstant #_"boolean" allConstantKeysUnique #_"boolean" valsConstant]
                (loop-when [keyvals [], keysConstant true, allConstantKeysUnique true, #_"IPersistentSet" constantKeys #{}, valsConstant true, #_"ISeq" s (seq form)] (some? s) => [keyvals keysConstant allConstantKeysUnique valsConstant]
                    (let [#_"IMapEntry" e (first s) #_"Expr" k (Compiler'analyze c, (key e)) #_"Expr" v (Compiler'analyze c, (val e))
                          [keysConstant allConstantKeysUnique constantKeys]
                            (when (satisfies? Literal k) => [false allConstantKeysUnique constantKeys]
                                (let [#_"Object" kval (Expr'''eval k)]
                                    (if (contains? constantKeys kval)
                                        [keysConstant false constantKeys]
                                        [keysConstant allConstantKeysUnique (conj constantKeys kval)]
                                    )
                                )
                            )]
                        (recur (conj keyvals k v) keysConstant allConstantKeysUnique constantKeys (and valsConstant (satisfies? Literal v)) (next s))
                    )
                )
              #_"Expr" e (MapExpr'new keyvals)]
            (cond
                (and (instance? IObj form) (some? (meta form)))
                    (MetaExpr'new e, (MapExpr'parse c, (meta form)))
                keysConstant
                    (when allConstantKeysUnique => (throw (IllegalArgumentException. "Duplicate constant keys in map"))
                        (when valsConstant => e
                            (loop-when-recur [#_"IPersistentMap" m {} #_"int" i 0]
                                             (< i (count keyvals))
                                             [(assoc m (Literal'''literal (nth keyvals i)) (Literal'''literal (nth keyvals (inc i)))) (+ i 2)]
                                          => (ConstantExpr'new m)
                            )
                        )
                    )
                :else
                    e
            )
        )
    )
)

(class-ns SetExpr
    (defn #_"SetExpr" SetExpr'new [#_"IPersistentVector" keys]
        (merge (SetExpr.)
            (hash-map
                #_"IPersistentVector" :keys keys
            )
        )
    )

    (extend-type SetExpr Expr
        (#_"Object" Expr'''eval [#_"SetExpr" this]
            (let [#_"Object[]" a (make-array Object (count (:keys this)))]
                (dotimes [#_"int" i (count (:keys this))]
                    (aset a i (Expr'''eval (nth (:keys this) i)))
                )
                (RT/set a)
            )
        )

        (#_"void" Expr'''emit [#_"SetExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (MethodExpr'emitArgsAsArray (:keys this), objx, gen)
            (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.IPersistentSet set(Object[])"))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"SetExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"SetExpr" this]
            IPersistentSet
        )
    )

    (defn #_"Expr" SetExpr'parse [#_"Context" context, #_"IPersistentSet" form]
        (let [[#_"IPersistentVector" keys #_"boolean" constant?]
                (loop-when [keys [] constant? true #_"ISeq" s (seq form)] (some? s) => [keys constant?]
                    (let [#_"Expr" e (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (first s))]
                        (recur (conj keys e) (and constant? (satisfies? Literal e)) (next s))
                    )
                )]
            (cond
                (and (instance? IObj form) (some? (meta form)))
                    (MetaExpr'new (SetExpr'new keys), (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), (meta form)))
                constant?
                    (loop-when-recur [#_"IPersistentSet" s #{} #_"int" i 0]
                                     (< i (count keys))
                                     [(conj s (Literal'''literal (nth keys i))) (inc i)]
                                  => (ConstantExpr'new s)
                    )
                :else
                    (SetExpr'new keys)
            )
        )
    )
)

(class-ns VectorExpr
    (defn #_"VectorExpr" VectorExpr'new [#_"IPersistentVector" args]
        (merge (VectorExpr.)
            (hash-map
                #_"IPersistentVector" :args args
            )
        )
    )

    (extend-type VectorExpr Expr
        (#_"Object" Expr'''eval [#_"VectorExpr" this]
            (loop-when-recur [#_"IPersistentVector" v [] #_"int" i 0]
                            (< i (count (:args this)))
                            [(conj v (Expr'''eval (nth (:args this) i))) (inc i)]
                        => v
            )
        )

        (#_"void" Expr'''emit [#_"VectorExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (if (<= (count (:args this)) Tuple'MAX_SIZE)
                (do
                    (dotimes [#_"int" i (count (:args this))]
                        (Expr'''emit (nth (:args this) i), :Context'EXPRESSION, objx, gen)
                    )
                    (.invokeStatic gen, (Type/getType Tuple), (aget Compiler'createTupleMethods (count (:args this))))
                )
                (do
                    (MethodExpr'emitArgsAsArray (:args this), objx, gen)
                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.IPersistentVector vector(Object[])"))
                )
            )

            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"VectorExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"VectorExpr" this]
            IPersistentVector
        )
    )

    (defn #_"Expr" VectorExpr'parse [#_"Context" context, #_"IPersistentVector" form]
        (let [[#_"IPersistentVector" args #_"boolean" constant?]
                (loop-when [args [] constant? true #_"int" i 0] (< i (count form)) => [args constant?]
                    (let [#_"Expr" e (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (nth form i))]
                        (recur (conj args e) (and constant? (satisfies? Literal e)) (inc i))
                    )
                )]
            (cond
                (and (instance? IObj form) (some? (meta form)))
                    (MetaExpr'new (VectorExpr'new args), (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), (meta form)))
                constant?
                    (loop-when-recur [#_"IPersistentVector" v [] #_"int" i 0]
                                     (< i (count args))
                                     [(conj v (Literal'''literal (nth args i))) (inc i)]
                                  => (ConstantExpr'new v)
                    )
                :else
                    (VectorExpr'new args)
            )
        )
    )
)

(class-ns KeywordInvokeExpr
    (defn #_"KeywordInvokeExpr" KeywordInvokeExpr'new [#_"int" line, #_"Symbol" tag, #_"KeywordExpr" kw, #_"Expr" target]
        (merge (KeywordInvokeExpr.)
            (hash-map
                #_"int" :line line
                #_"Object" :tag tag
                #_"KeywordExpr" :kw kw
                #_"Expr" :target target

                #_"int" :siteIndex (Compiler'registerKeywordCallsite (:k kw))
            )
        )
    )

    (extend-type KeywordInvokeExpr Expr
        (#_"Object" Expr'''eval [#_"KeywordInvokeExpr" this]
            (.invoke (:k (:kw this)), (Expr'''eval (:target this)))
        )

        (#_"void" Expr'''emit [#_"KeywordInvokeExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (let [#_"Label" endLabel (.newLabel gen) #_"Label" faultLabel (.newLabel gen)]
                (.visitLineNumber gen, (:line this), (.mark gen))
                (.getStatic gen, (:objType objx), (Compiler'thunkNameStatic (:siteIndex this)), (Type/getType ILookupThunk))
                (.dup gen) ;; thunk, thunk
                (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen) ;; thunk, thunk, target
                (.visitLineNumber gen, (:line this), (.mark gen))
                (.dupX2 gen) ;; target, thunk, thunk, target
                (.invokeInterface gen, (Type/getType ILookupThunk), (Method/getMethod "Object get(Object)")) ;; target, thunk, result
                (.dupX2 gen) ;; result, target, thunk, result
                (.visitJumpInsn gen, Opcodes/IF_ACMPEQ, faultLabel) ;; result, target
                (.pop gen) ;; result
                (.goTo gen, endLabel)

                (.mark gen, faultLabel) ;; result, target
                (.swap gen) ;; target, result
                (.pop gen) ;; target
                (.dup gen) ;; target, target
                (.getStatic gen, (:objType objx), (Compiler'siteNameStatic (:siteIndex this)), (Type/getType KeywordLookupSite)) ;; target, target, site
                (.swap gen) ;; target, site, target
                (.invokeInterface gen, (Type/getType ILookupSite), (Method/getMethod "clojure.lang.ILookupThunk fault(Object)")) ;; target, new-thunk
                (.dup gen) ;; target, new-thunk, new-thunk
                (.putStatic gen, (:objType objx), (Compiler'thunkNameStatic (:siteIndex this)), (Type/getType ILookupThunk)) ;; target, new-thunk
                (.swap gen) ;; new-thunk, target
                (.invokeInterface gen, (Type/getType ILookupThunk), (Method/getMethod "Object get(Object)")) ;; result

                (.mark gen, endLabel)
                (when (= context :Context'STATEMENT)
                    (.pop gen)
                )
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"KeywordInvokeExpr" this]
            (some? (:tag this))
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"KeywordInvokeExpr" this]
            (Interop'tagToClass (:tag this))
        )
    )
)

(class-ns InstanceOfExpr
    (defn #_"InstanceOfExpr" InstanceOfExpr'new [#_"Class" c, #_"Expr" expr]
        (merge (InstanceOfExpr.)
            (hash-map
                #_"Class" :c c
                #_"Expr" :expr expr
            )
        )
    )

    (extend-type InstanceOfExpr Expr
        (#_"Object" Expr'''eval [#_"InstanceOfExpr" this]
            (if (.isInstance (:c this), (Expr'''eval (:expr this))) true false)
        )

        (#_"void" Expr'''emit [#_"InstanceOfExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (MaybePrimitive'''emitUnboxed this, context, objx, gen)
            (Interop'emitBoxReturn objx, gen, Boolean/TYPE)
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"InstanceOfExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"InstanceOfExpr" this]
            Boolean/TYPE
        )
    )

    (extend-type InstanceOfExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"InstanceOfExpr" this]
            true
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"InstanceOfExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit (:expr this), :Context'EXPRESSION, objx, gen)
            (.instanceOf gen, (Compiler'getType (:c this)))
            nil
        )
    )
)

(class-ns InvokeExpr
    (defn #_"InvokeExpr" InvokeExpr'new [#_"int" line, #_"Symbol" tag, #_"Expr" fexpr, #_"IPersistentVector" args, #_"boolean" tailPosition]
        (let [this
                (merge (InvokeExpr.)
                    (hash-map
                        #_"Expr" :fexpr fexpr
                        #_"Object" :tag (or tag (when (instance? VarExpr fexpr) (:tag fexpr)))
                        #_"IPersistentVector" :args args
                        #_"int" :line line
                        #_"boolean" :tailPosition tailPosition

                        #_"boolean" :isProtocol false
                        #_"int" :siteIndex -1
                        #_"Class" :protocolOn nil
                        #_"java.lang.reflect.Method" :onMethod nil
                    )
                )]
            (when (instance? VarExpr fexpr) => this
                (let [#_"Var" fvar (:var fexpr) #_"Var" pvar (get (meta fvar) :protocol)]
                    (when (and (some? pvar) (bound? #'*protocol-callsites*)) => this
                        (let [this (assoc this :isProtocol true)
                              this (assoc this :siteIndex (Compiler'registerProtocolCallsite (:var fexpr)))
                              this (assoc this :protocolOn (Interop'maybeClass (get (var-get pvar) :on), false))]
                            (when (some? (:protocolOn this)) => this
                                (let [#_"IPersistentMap" mmap (get (var-get pvar) :method-map)
                                      #_"Keyword" mmapVal (get mmap (keyword (ßsym fvar)))]
                                    (when (some? mmapVal) => (throw (IllegalArgumentException. (str "No method of interface: " (.getName (:protocolOn this)) " found for function: " (ßsym fvar) " of protocol: " (ßsym pvar) " (The protocol method may have been defined before and removed.)")))
                                        (let [#_"String" mname (Compiler'munge (.toString (ßsym mmapVal)))
                                              #_"PersistentVector" methods (Reflector'getMethods (:protocolOn this), (dec (count args)), mname, false)]
                                            (when (= (count methods) 1) => (throw (IllegalArgumentException. (str "No single method: " mname " of interface: " (.getName (:protocolOn this)) " found for function: " (ßsym fvar) " of protocol: " (ßsym pvar))))
                                                (assoc this :onMethod (nth methods 0))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    #_method
    (defn #_"void" InvokeExpr''emitArgsAndCall [#_"InvokeExpr" this, #_"int" firstArgToEmit, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
        (loop-when-recur [#_"int" i firstArgToEmit] (< i (Math/min Compiler'MAX_POSITIONAL_ARITY, (count (:args this)))) [(inc i)]
            (Expr'''emit (nth (:args this) i), :Context'EXPRESSION, objx, gen)
        )
        (when (< Compiler'MAX_POSITIONAL_ARITY (count (:args this)))
            (let [#_"PersistentVector" restArgs
                    (loop-when-recur [restArgs [] #_"int" i Compiler'MAX_POSITIONAL_ARITY]
                                     (< i (count (:args this)))
                                     [(conj restArgs (nth (:args this) i)) (inc i)]
                                  => restArgs
                    )]
                (MethodExpr'emitArgsAsArray restArgs, objx, gen)
            )
        )
        (.visitLineNumber gen, (:line this), (.mark gen))

        (when (:tailPosition this)
            (IopMethod''emitClearThis *method*, gen)
        )

        (.invokeInterface gen, (Type/getType IFn), (Method. "invoke", (Type/getType Object), (aget Compiler'ARG_TYPES (Math/min (inc Compiler'MAX_POSITIONAL_ARITY), (count (:args this))))))
        nil
    )

    #_method
    (defn #_"void" InvokeExpr''emitProto [#_"InvokeExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
        (let [#_"Label" onLabel (.newLabel gen) #_"Label" callLabel (.newLabel gen) #_"Label" endLabel (.newLabel gen)]
            (Expr'''emit (nth (:args this) 0), :Context'EXPRESSION, objx, gen)
            (.dup gen) ;; target, target
            (.invokeStatic gen, (Type/getType #_"Reflector" Util), (Method/getMethod "Class classOf(Object)")) ;; target, class
            (.getStatic gen, (:objType objx), (Compiler'cachedClassName (:siteIndex this)), (Type/getType Class)) ;; target, class, cached-class
            (.visitJumpInsn gen, Opcodes/IF_ACMPEQ, callLabel) ;; target
            (when (some? (:protocolOn this))
                (.dup gen) ;; target, target
                (.instanceOf gen, (Type/getType (:protocolOn this)))
                (.ifZCmp gen, GeneratorAdapter/NE, onLabel)
            )
            (.dup gen) ;; target, target
            (.invokeStatic gen, (Type/getType #_"Reflector" Util), (Method/getMethod "Class classOf(Object)")) ;; target, class
            (.putStatic gen, (:objType objx), (Compiler'cachedClassName (:siteIndex this)), (Type/getType Class)) ;; target
            (.mark gen, callLabel) ;; target
            (IopObject''emitVar objx, gen, (:var (:fexpr this)))
            (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "Object getRawRoot()")) ;; target, proto-fn
            (.swap gen)
            (InvokeExpr''emitArgsAndCall this, 1, context, objx, gen)
            (.goTo gen, endLabel)
            (.mark gen, onLabel) ;; target
            (when (some? (:protocolOn this))
                (.checkCast gen, (Type/getType (:protocolOn this)))
                (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:onMethod this)), (subvec (:args this) 1 (count (:args this))))
                (when (= context :Context'RETURN)
                    (IopMethod''emitClearLocals *method*, gen)
                )
                (let [#_"Method" m (Method. (.getName (:onMethod this)), (Type/getReturnType (:onMethod this)), (Type/getArgumentTypes (:onMethod this)))]
                    (.invokeInterface gen, (Type/getType (:protocolOn this)), m)
                    (Interop'emitBoxReturn objx, gen, (.getReturnType (:onMethod this)))
                )
            )
            (.mark gen, endLabel)
        )
        nil
    )

    (extend-type InvokeExpr Expr
        (#_"Object" Expr'''eval [#_"InvokeExpr" this]
            (let [#_"IFn" fn (Expr'''eval (:fexpr this))
                #_"PersistentVector" v (loop-when-recur [v [] #_"int" i 0] (< i (count (:args this))) [(conj v (Expr'''eval (nth (:args this) i))) (inc i)] => v)]
                (.applyTo fn, (seq v))
            )
        )

        (#_"void" Expr'''emit [#_"InvokeExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (if (:isProtocol this)
                (do
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (InvokeExpr''emitProto this, context, objx, gen)
                )
                (do
                    (Expr'''emit (:fexpr this), :Context'EXPRESSION, objx, gen)
                    (.visitLineNumber gen, (:line this), (.mark gen))
                    (.checkCast gen, (Type/getType IFn))
                    (InvokeExpr''emitArgsAndCall this, 0, context, objx, gen)
                )
            )
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"InvokeExpr" this]
            (some? (:tag this))
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"InvokeExpr" this]
            (Interop'tagToClass (:tag this))
        )
    )

    (defn #_"Expr" InvokeExpr'parse [#_"Context" context, #_"ISeq" form]
        (let [#_"boolean" tailPosition (Compiler'inTailCall context) context (if (= context :Context'EVAL) context :Context'EXPRESSION)
              #_"Expr" fexpr (Compiler'analyze context, (first form))]
            (or
                (when (and (instance? VarExpr fexpr) (= (:var fexpr) #'instance?) (= (count form) 3))
                    (let-when [#_"Expr" sexpr (Compiler'analyze :Context'EXPRESSION, (second form))] (instance? ConstantExpr sexpr)
                        (let-when [#_"Object" val (Literal'''literal sexpr)] (instance? Class val)
                            (InstanceOfExpr'new val, (Compiler'analyze context, (third form)))
                        )
                    )
                )

                (when (and (instance? KeywordExpr fexpr) (= (count form) 2) (bound? #'*keyword-callsites*))
                    (let [#_"Expr" target (Compiler'analyze context, (second form))]
                        (KeywordInvokeExpr'new *line*, (Compiler'tagOf form), fexpr, target)
                    )
                )

                (let [#_"PersistentVector" args
                        (loop-when-recur [args [] #_"ISeq" s (seq (next form))]
                                         (some? s)
                                         [(conj args (Compiler'analyze context, (first s))) (next s)]
                                      => args
                        )]
                    (InvokeExpr'new *line*, (Compiler'tagOf form), fexpr, args, tailPosition)
                )
            )
        )
    )
)

(class-ns LocalBinding
    (defn #_"LocalBinding" LocalBinding'new [#_"int" idx, #_"Symbol" sym, #_"Symbol" tag, #_"Expr" init, #_"boolean" isArg]
        (when (and (some? (Compiler'maybePrimitiveType init)) (some? tag))
            (throw (UnsupportedOperationException. "Can't type hint a local with a primitive initializer"))
        )
        (merge (LocalBinding.)
            (hash-map
                #_"int" :uid (Compiler'nextUniqueId)
                #_"int" :idx idx
                #_"Symbol" :sym sym
                #_"Symbol" :tag tag
                #_"Expr" :init init
                #_"boolean" :isArg isArg

                #_"String" :name (Compiler'munge (ßname sym))
                #_"boolean" :recurMistmatch false
            )
        )
    )

    #_memoize!
    (defn #_"boolean" LocalBinding''hasJavaClass [#_"LocalBinding" this]
        (let [? (and (some? (:init this)) (Expr'''hasJavaClass (:init this)))]
            (if (and ? (Reflector'isPrimitive (Expr'''getJavaClass (:init this))) (not (satisfies? MaybePrimitive (:init this))))
                false
                (or (some? (:tag this)) ?)
            )
        )
    )

    #_memoize!
    (defn #_"Class" LocalBinding''getJavaClass [#_"LocalBinding" this]
        (if (some? (:tag this)) (Interop'tagToClass (:tag this)) (Expr'''getJavaClass (:init this)))
    )

    #_method
    (defn #_"Class" LocalBinding''getPrimitiveType [#_"LocalBinding" this]
        (Compiler'maybePrimitiveType (:init this))
    )
)

(class-ns LocalBindingExpr
    (defn #_"LocalBindingExpr" LocalBindingExpr'new [#_"LocalBinding" lb, #_"Symbol" tag]
        (when (or (nil? (LocalBinding''getPrimitiveType lb)) (nil? tag)) => (throw (UnsupportedOperationException. "Can't type hint a primitive local"))
            (merge (LocalBindingExpr.)
                (hash-map
                    #_"LocalBinding" :lb lb
                    #_"Symbol" :tag tag
                )
            )
        )
    )

    (declare IopObject''emitLocal)

    (extend-type LocalBindingExpr Expr
        (#_"Object" Expr'''eval [#_"LocalBindingExpr" this]
            (throw (UnsupportedOperationException. "Can't eval locals"))
        )

        (#_"void" Expr'''emit [#_"LocalBindingExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when-not (= context :Context'STATEMENT)
                (IopObject''emitLocal objx, gen, (:lb this))
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"LocalBindingExpr" this]
            (or (some? (:tag this)) (LocalBinding''hasJavaClass (:lb this)))
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"LocalBindingExpr" this]
            (if (some? (:tag this)) (Interop'tagToClass (:tag this)) (LocalBinding''getJavaClass (:lb this)))
        )
    )

    (declare IopObject''emitUnboxedLocal)

    (extend-type LocalBindingExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"LocalBindingExpr" this]
            (some? (LocalBinding''getPrimitiveType (:lb this)))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"LocalBindingExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IopObject''emitUnboxedLocal objx, gen, (:lb this))
            nil
        )
    )

    (declare IopObject''emitAssignLocal)

    (extend-type LocalBindingExpr Assignable
        (#_"Object" Assignable'''evalAssign [#_"LocalBindingExpr" this, #_"Expr" val]
            (throw (UnsupportedOperationException. "Can't eval locals"))
        )

        (#_"void" Assignable'''emitAssign [#_"LocalBindingExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Expr" val]
            (IopObject''emitAssignLocal objx, gen, (:lb this), val)
            (when-not (= context :Context'STATEMENT)
                (IopObject''emitLocal objx, gen, (:lb this))
            )
            nil
        )
    )
)

(class-ns MethodParamExpr
    (defn #_"MethodParamExpr" MethodParamExpr'new [#_"Class" c]
        (merge (MethodParamExpr.)
            (hash-map
                #_"Class" :c c
            )
        )
    )

    (extend-type MethodParamExpr Expr
        (#_"Object" Expr'''eval [#_"MethodParamExpr" this]
            (throw (RuntimeException. "Can't eval"))
        )

        (#_"void" Expr'''emit [#_"MethodParamExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (throw (RuntimeException. "Can't emit"))
        )

        (#_"boolean" Expr'''hasJavaClass [#_"MethodParamExpr" this]
            (some? (:c this))
        )

        (#_"Class" Expr'''getJavaClass [#_"MethodParamExpr" this]
            (:c this)
        )
    )

    (extend-type MethodParamExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"MethodParamExpr" this]
            (Reflector'isPrimitive (:c this))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"MethodParamExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (throw (RuntimeException. "Can't emit"))
        )
    )
)

(class-ns FnMethod
    (defn #_"FnMethod" FnMethod'new [#_"IopObject" objx, #_"IopMethod" parent]
        (merge (FnMethod.) (IopMethod'init objx, parent)
            (hash-map
                ;; localbinding->localbinding
                #_"PersistentVector" :reqParms nil
                #_"LocalBinding" :restParm nil
                #_"Type[]" :argTypes nil
                #_"Class[]" :argClasses nil
                #_"Class" :retClass nil
            )
        )
    )

    #_method
    (defn #_"boolean" FnMethod''isVariadic [#_"FnMethod" this]
        (some? (:restParm this))
    )

    (extend-type FnMethod IopMethod
        (#_"int" IopMethod'''numParams [#_"FnMethod" this]
            (+ (count (:reqParms this)) (if (FnMethod''isVariadic this) 1 0))
        )

        (#_"String" IopMethod'''getMethodName [#_"FnMethod" this]
            (if (FnMethod''isVariadic this) "doInvoke" "invoke")
        )

        (#_"Type" IopMethod'''getReturnType [#_"FnMethod" this]
            (Type/getType Object)
        )

        (#_"Type[]" IopMethod'''getArgTypes [#_"FnMethod" this]
            (if (and (FnMethod''isVariadic this) (= (count (:reqParms this)) Compiler'MAX_POSITIONAL_ARITY))
                (let [#_"int" n (inc Compiler'MAX_POSITIONAL_ARITY) #_"Type[]" a (make-array Type n)]
                    (dotimes [#_"int" i n]
                        (aset a i (Type/getType Object))
                    )
                    a
                )
                (aget Compiler'ARG_TYPES (IopMethod'''numParams this))
            )
        )

        (#_"void" IopMethod'''emit [#_"FnMethod" this, #_"IopObject" fn, #_"ClassVisitor" cv]
            (let [#_"Method" m (Method. (IopMethod'''getMethodName this), (IopMethod'''getReturnType this), (IopMethod'''getArgTypes this))
                #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, Compiler'EXCEPTION_TYPES, cv)]
                (.visitCode gen)
                (let [#_"Label" loopLabel (.mark gen)]
                    (.visitLineNumber gen, (:line this), loopLabel)
                    (binding [*loop-label* loopLabel, *method* this]
                        (Expr'''emit (:body this), :Context'RETURN, fn, gen)
                        (let [#_"Label" end (.mark gen)]
                            (.visitLocalVariable gen, "this", "Ljava/lang/Object;", nil, loopLabel, end, 0)
                            (loop-when-recur [#_"ISeq" lbs (seq (:argLocals this))] (some? lbs) [(next lbs)]
                                (let [#_"LocalBinding" lb (first lbs)]
                                    (.visitLocalVariable gen, (:name lb), "Ljava/lang/Object;", nil, loopLabel, end, (:idx lb))
                                )
                            )
                        )
                    )
                    (.returnValue gen)
                    (.endMethod gen)
                )
            )
            nil
        )
    )

    (defn #_"FnMethod" FnMethod'parse [#_"IopObject" objx, #_"ISeq" form, #_"Object" retTag]
        ;; ([args] body...)
        (let [#_"IPersistentVector" parms (first form) #_"ISeq" body (next form)
              #_"FnMethod" fm
                (-> (FnMethod'new objx, *method*)
                    (assoc :line *line*)
                )]
            ;; register as the current method and set up a new env frame
            (binding [*method*            fm
                      *local-env*         *local-env*
                      *last-local-num*    -1
                      *loop-locals*       nil
                      *in-return-context* true]
                (let [retTag (if (string? retTag) (symbol retTag) retTag)
                      retTag (when (and (symbol? retTag) (any = (.getName retTag) "long" "double")) retTag)
                      #_"Class" retClass
                        (let-when [retClass (Interop'tagClass (or (Compiler'tagOf parms) retTag))] (.isPrimitive retClass) => Object
                            (when-not (any = retClass Double/TYPE Long/TYPE) => retClass
                                (throw (IllegalArgumentException. "Only long and double primitives are supported"))
                            )
                        )
                      fm (assoc fm :retClass retClass)]
                    ;; register 'this' as local 0
                    (if (some? (:thisName objx))
                        (Compiler'registerLocal (symbol (:thisName objx)), nil, nil, false)
                        (Compiler'nextLocalNum)
                    )
                    (let [fm (assoc fm #_"PersistentVector" :argTypes [] #_"PersistentVector" :argClasses [] :reqParms [] :restParm nil :argLocals [])
                          fm (loop-when [fm fm #_"boolean" rest? false #_"int" i 0] (< i (count parms)) => fm
                                (when (symbol? (nth parms i)) => (throw (IllegalArgumentException. "fn params must be Symbols"))
                                    (let [#_"Symbol" p (nth parms i)]
                                        (cond
                                            (some? (namespace p))
                                                (throw (RuntimeException. (str "Can't use qualified name as parameter: " p)))
                                            (= p '&)
                                                (when-not rest? => (throw (RuntimeException. "Invalid parameter list"))
                                                    (recur fm true (inc i))
                                                )
                                            :else
                                                (let [#_"Class" c (Compiler'primClass (Interop'tagClass (Compiler'tagOf p)))]
                                                    (when (and (.isPrimitive c) (not (any = c Double/TYPE Long/TYPE)))
                                                        (throw (IllegalArgumentException. (str "Only long and double primitives are supported: " p)))
                                                    )
                                                    (when (and rest? (some? (Compiler'tagOf p)))
                                                        (throw (RuntimeException. "& arg cannot have type hint"))
                                                    )
                                                    (let [c (if rest? ISeq c)
                                                          fm (-> fm (update :argTypes conj (Type/getType c)) (update :argClasses conj c))
                                                          #_"LocalBinding" lb
                                                            (if (.isPrimitive c)
                                                                (Compiler'registerLocal p, nil, (MethodParamExpr'new c), true)
                                                                (Compiler'registerLocal p, (if rest? 'clojure.lang.ISeq (Compiler'tagOf p)), nil, true)
                                                            )
                                                          fm (update fm :argLocals conj lb)]
                                                        (if-not rest?
                                                            (update fm :reqParms conj lb)
                                                            (assoc fm :restParm lb)
                                                        )
                                                    )
                                                )
                                        )
                                    )
                                )
                            )]
                        (when (< Compiler'MAX_POSITIONAL_ARITY (count (:reqParms fm)))
                            (throw (RuntimeException. (str "Can't specify more than " Compiler'MAX_POSITIONAL_ARITY " params")))
                        )
                        (set! *loop-locals* (:argLocals fm))
                        (-> fm
                            (update #_"Type[]" :argTypes #(.toArray %, (make-array Type (count %))))
                            (update #_"Class[]" :argClasses #(.toArray %, (make-array Class (count %))))
                            (assoc :body (IParser'''parse (BodyParser'new), :Context'RETURN, body))
                        )
                    )
                )
            )
        )
    )
)

(class-ns IopObject
    (defn #_"IopObject" IopObject'init [#_"Object" tag]
        (hash-map
            #_"int" :uid (Compiler'nextUniqueId)
            #_"Object" :tag tag
            #_"String" :name nil
            #_"String" :internalName nil
            #_"String" :thisName nil
            #_"Type" :objType nil
            #_"IPersistentVector" :closesExprs []
            #_"IPersistentMap" :fields nil
            #_"IPersistentVector" :hintedFields []
            #_"IPersistentMap" :keywords {}
            #_"IPersistentMap" :vars {}
            #_"int" :line 0
            #_"PersistentVector" :constants nil
            #_"int" :altCtorDrops 0
            #_"IPersistentVector" :keywordCallsites nil
            #_"IPersistentVector" :protocolCallsites nil
            #_"boolean" :onceOnly false
            #_"IPersistentMap" :opts {}

            #_"Class" :compiledClass nil
        )
    )

    #_method
    (defn #_"boolean" IopObject''isVolatile [#_"IopObject" this, #_"LocalBinding" lb]
        (and (contains? (:fields this) (:sym lb)) (get (meta (:sym lb)) :volatile-mutable))
    )

    #_method
    (defn #_"boolean" IopObject''isMutable [#_"IopObject" this, #_"LocalBinding" lb]
        (or (IopObject''isVolatile this, lb) (and (contains? (:fields this) (:sym lb)) (get (meta (:sym lb)) :unsynchronized-mutable)))
    )

    #_method
    (defn #_"boolean" IopObject''isDeftype [#_"IopObject" this]
        (some? (:fields this))
    )

    #_method
    (defn #_"Type" IopObject''constantType [#_"IopObject" this, #_"int" id]
        (let [#_"Object" o (nth (:constants this) id) #_"Class" c (Reflector'classOf o)]
            (or
                (when (and (some? c) (Modifier/isPublic (.getModifiers c)))
                    ;; can't emit derived fn types due to visibility
                    (cond
                        (.isAssignableFrom LazySeq, c) (Type/getType ISeq)
                        (= c Keyword)                  (Type/getType Keyword)
                        (.isAssignableFrom RestFn, c)  (Type/getType RestFn)
                        (.isAssignableFrom AFn, c)     (Type/getType AFn)
                        (= c Var)                      (Type/getType Var)
                        (= c String)                   (Type/getType String)
                    )
                )
                (Type/getType Object)
            )
        )
    )

    #_method
    (defn #_"Type[]" IopObject''ctorTypes [#_"IopObject" this]
        (let [#_"IPersistentVector" v (if (IopObject'''supportsMeta this) [(Type/getType IPersistentMap)] [])
              v (loop-when [v v #_"ISeq" s (vals (get *closes* (:uid this)))] (some? s) => v
                    (let [#_"Class" c (LocalBinding''getPrimitiveType (first s))]
                        (recur (conj v (if (some? c) (Type/getType c) (Type/getType Object))) (next s))
                    )
                )]
            (let [#_"Type[]" a (make-array Type (count v))]
                (dotimes [#_"int" i (count v)]
                    (aset a i (nth v i))
                )
                a
            )
        )
    )

    #_method
    (defn #_"Object" IopObject''doEval [#_"IopObject" this]
        (when-not (IopObject''isDeftype this)
            (.newInstance (:compiledClass this))
        )
    )

    (declare IopObject''emitValue)

    #_method
    (defn- #_"void" IopObject''emitKeywordCallsites [#_"IopObject" this, #_"GeneratorAdapter" clinitgen]
        (dotimes [#_"int" i (count (:keywordCallsites this))]
            (let [#_"Keyword" k (nth (:keywordCallsites this) i)]
                (.newInstance clinitgen, (Type/getType KeywordLookupSite))
                (.dup clinitgen)
                (IopObject''emitValue this, k, clinitgen)
                (.invokeConstructor clinitgen, (Type/getType KeywordLookupSite), (Method/getMethod "void <init>(clojure.lang.Keyword)"))
                (.dup clinitgen)
                (.putStatic clinitgen, (:objType this), (Compiler'siteNameStatic i), (Type/getType KeywordLookupSite))
                (.putStatic clinitgen, (:objType this), (Compiler'thunkNameStatic i), (Type/getType ILookupThunk))
            )
        )
        nil
    )

    #_method
    (defn #_"void" IopObject''emitObjectArray [#_"IopObject" this, #_"Object[]" a, #_"GeneratorAdapter" gen]
        (.push gen, (alength a))
        (.newArray gen, (Type/getType Object))
        (dotimes [#_"int" i (alength a)]
            (.dup gen)
            (.push gen, i)
            (IopObject''emitValue this, (aget a i), gen)
            (.arrayStore gen, (Type/getType Object))
        )
        nil
    )

    #_method
    (defn #_"void" IopObject''emitConstants [#_"IopObject" this, #_"GeneratorAdapter" clinitgen]
        (dotimes [#_"int" i (count (:constants this))]
            (when (contains? *used-constants* i)
                (IopObject''emitValue this, (nth (:constants this) i), clinitgen)
                (.checkCast clinitgen, (IopObject''constantType this, i))
                (.putStatic clinitgen, (:objType this), (Compiler'constantName i), (IopObject''constantType this, i))
            )
        )
        nil
    )

    #_method
    (defn #_"void" IopObject''emitClearCloses [#_"IopObject" this, #_"GeneratorAdapter" gen]
        nil
    )

    #_method
    (defn #_"void" IopObject''emitLetFnInits [#_"IopObject" this, #_"GeneratorAdapter" gen, #_"IopObject" objx, #_"IPersistentSet" letFnLocals]
        ;; objx arg is enclosing objx, not this
        (.checkCast gen, (:objType this))

        (loop-when-recur [#_"ISeq" s (vals (get *closes* (:uid this)))] (some? s) [(next s)]
            (let [#_"LocalBinding" lb (first s)]
                (when (contains? letFnLocals lb)
                    (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                        (.dup gen)
                        (if (some? primc)
                            (do
                                (IopObject''emitUnboxedLocal objx, gen, lb)
                                (.putField gen, (:objType this), (:name lb), (Type/getType primc))
                            )
                            (do
                                (IopObject''emitLocal objx, gen, lb)
                                (.putField gen, (:objType this), (:name lb), (Type/getType Object))
                            )
                        )
                    )
                )
            )
        )
        (.pop gen)
        nil
    )

    #_method
    (defn #_"void" IopObject''doEmit [#_"IopObject" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
        ;; emitting a Fn means constructing an instance, feeding closed-overs from enclosing scope, if any
        ;; objx arg is enclosing objx, not this
        (when-not (IopObject''isDeftype this) => (.visitInsn gen, Opcodes/ACONST_NULL)
            (.newInstance gen, (:objType this))
            (.dup gen)
            (when (IopObject'''supportsMeta this)
                (.visitInsn gen, Opcodes/ACONST_NULL)
            )
            (loop-when-recur [#_"ISeq" s (seq (:closesExprs this))] (some? s) [(next s)]
                (let [#_"LocalBindingExpr" lbe (first s) #_"LocalBinding" lb (:lb lbe)]
                    (if (some? (LocalBinding''getPrimitiveType lb))
                        (IopObject''emitUnboxedLocal objx, gen, lb)
                        (IopObject''emitLocal objx, gen, lb)
                    )
                )
            )
            (.invokeConstructor gen, (:objType this), (Method. "<init>", Type/VOID_TYPE, (IopObject''ctorTypes this)))
        )
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_method
    (defn #_"void" IopObject''emitAssignLocal [#_"IopObject" this, #_"GeneratorAdapter" gen, #_"LocalBinding" lb, #_"Expr" val]
        (when (IopObject''isMutable this, lb) => (throw (IllegalArgumentException. (str "Cannot assign to non-mutable: " (:name lb))))
            (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                (.loadThis gen)
                (if (some? primc)
                    (do
                        (when-not (and (satisfies? MaybePrimitive val) (MaybePrimitive'''canEmitPrimitive val))
                            (throw (IllegalArgumentException. (str "Must assign primitive to primitive mutable: " (:name lb))))
                        )
                        (MaybePrimitive'''emitUnboxed val, :Context'EXPRESSION, this, gen)
                        (.putField gen, (:objType this), (:name lb), (Type/getType primc))
                    )
                    (do
                        (Expr'''emit val, :Context'EXPRESSION, this, gen)
                        (.putField gen, (:objType this), (:name lb), (Type/getType Object))
                    )
                )
            )
        )
        nil
    )

    #_method
    (defn- #_"void" IopObject''emitLocal [#_"IopObject" this, #_"GeneratorAdapter" gen, #_"LocalBinding" lb]
        (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
            (if (contains? (get *closes* (:uid this)) (:uid lb))
                (do
                    (.loadThis gen)
                    (.getField gen, (:objType this), (:name lb), (Type/getType (or primc Object)))
                )
                (if (:isArg lb)
                    (.loadArg gen, (dec (:idx lb)))
                    (.visitVarInsn gen, (.getOpcode (Type/getType (or primc Object)), Opcodes/ILOAD), (:idx lb))
                )
            )
            (when (some? primc)
                (Interop'emitBoxReturn this, gen, primc)
            )
        )
        nil
    )

    #_method
    (defn- #_"void" IopObject''emitUnboxedLocal [#_"IopObject" this, #_"GeneratorAdapter" gen, #_"LocalBinding" lb]
        (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
            (if (contains? (get *closes* (:uid this)) (:uid lb))
                (do
                    (.loadThis gen)
                    (.getField gen, (:objType this), (:name lb), (Type/getType primc))
                )
                (if (:isArg lb)
                    (.loadArg gen, (dec (:idx lb)))
                    (.visitVarInsn gen, (.getOpcode (Type/getType primc), Opcodes/ILOAD), (:idx lb))
                )
            )
        )
        nil
    )

    #_method
    (defn #_"void" IopObject''emitVar [#_"IopObject" this, #_"GeneratorAdapter" gen, #_"Var" var]
        (IopObject''emitConstant this, gen, (get (:vars this) var))
        nil
    )

    #_method
    (defn #_"void" IopObject''emitVarValue [#_"IopObject" this, #_"GeneratorAdapter" gen, #_"Var" v]
        (IopObject''emitConstant this, gen, (get (:vars this) v))
        (.invokeVirtual gen, (Type/getType Var), (if (.isDynamic v) (Method/getMethod "Object get()") (Method/getMethod "Object getRawRoot()")))
        nil
    )

    #_method
    (defn #_"void" IopObject''emitKeyword [#_"IopObject" this, #_"GeneratorAdapter" gen, #_"Keyword" k]
        (IopObject''emitConstant this, gen, (get (:keywords this) k))
        nil
    )

    #_method
    (defn #_"void" IopObject''emitConstant [#_"IopObject" this, #_"GeneratorAdapter" gen, #_"int" id]
        (update! *used-constants* conj id)
        (.getStatic gen, (:objType this), (Compiler'constantName id), (IopObject''constantType this, id))
        nil
    )

    #_method
    (defn #_"void" IopObject''emitValue [#_"IopObject" this, #_"Object" value, #_"GeneratorAdapter" gen]
        (let [#_"boolean" partial?
                (cond (nil? value)
                    (do
                        (.visitInsn gen, Opcodes/ACONST_NULL)
                        true
                    )
                    (string? value)
                    (do
                        (.push gen, value)
                        true
                    )
                    (instance? Boolean value)
                    (do
                        (.getStatic gen, (Type/getType Boolean), (if (.booleanValue value) "TRUE" "FALSE"), (Type/getType Boolean))
                        true
                    )
                    (instance? Integer value)
                    (do
                        (.push gen, (.intValue value))
                        (.invokeStatic gen, (Type/getType Integer), (Method/getMethod "Integer valueOf(int)"))
                        true
                    )
                    (instance? Long value)
                    (do
                        (.push gen, (.longValue value))
                        (.invokeStatic gen, (Type/getType Long), (Method/getMethod "Long valueOf(long)"))
                        true
                    )
                    (instance? Double value)
                    (do
                        (.push gen, (.doubleValue value))
                        (.invokeStatic gen, (Type/getType Double), (Method/getMethod "Double valueOf(double)"))
                        true
                    )
                    (instance? Character value)
                    (do
                        (.push gen, (.charValue value))
                        (.invokeStatic gen, (Type/getType Character), (Method/getMethod "Character valueOf(char)"))
                        true
                    )
                    (instance? Class value)
                    (do
                        (if (.isPrimitive value)
                            (let [#_"Type" t
                                    (condp = value
                                        Boolean/TYPE   (Type/getType Boolean)
                                        Byte/TYPE      (Type/getType Byte)
                                        Character/TYPE (Type/getType Character)
                                        Double/TYPE    (Type/getType Double)
                                        Float/TYPE     (Type/getType Float)
                                        Integer/TYPE   (Type/getType Integer)
                                        Long/TYPE      (Type/getType Long)
                                        Short/TYPE     (Type/getType Short)
                                        (throw (RuntimeException. (str "Can't embed unknown primitive in code: " value)))
                                    )]
                                (.getStatic gen, t, "TYPE", (Type/getType Class))
                            )
                            (do
                                (.push gen, (Compiler'destubClassName (.getName value)))
                                (.invokeStatic gen, (Type/getType RT), (Method/getMethod "Class classForName(String)"))
                            )
                        )
                        true
                    )
                    (symbol? value)
                    (do
                        (.push gen, (ßns value))
                        (.push gen, (ßname value))
                        (.invokeStatic gen, (Type/getType Symbol), (Method/getMethod "clojure.lang.Symbol intern(String, String)"))
                        true
                    )
                    (keyword? value)
                    (do
                        (.push gen, (ßns (ßsym value)))
                        (.push gen, (ßname (ßsym value)))
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.Keyword keyword(String, String)"))
                        true
                    )
                    (var? value)
                    (do
                        (.push gen, (.toString (ßname (ßns value))))
                        (.push gen, (.toString (ßsym value)))
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.Var var(String, String)"))
                        true
                    )
                    (instance? IType value)
                    (let [#_"Method" ctor (Method. "<init>", (Type/getConstructorDescriptor (aget (.getConstructors (.getClass value)) 0)))]
                        (.newInstance gen, (Type/getType (.getClass value)))
                        (.dup gen)
                        (let [#_"IPersistentVector" fields (Reflector'invokeStaticMethod (.getClass value), "getBasis", (object-array 0))]
                            (loop-when-recur [#_"ISeq" s (seq fields)] (some? s) [(next s)]
                                (let [#_"Symbol" field (first s)]
                                    (IopObject''emitValue this, (Reflector'getInstanceField value, (Compiler'munge (ßname field))), gen)
                                    (let-when [#_"Class" k (Interop'tagClass (Compiler'tagOf field))] (.isPrimitive k)
                                        (let [#_"Type" b (Type/getType (Compiler'boxClass k))]
                                            (.invokeVirtual gen, b, (Method. (str (.getName k) "Value"), (str "()" (.getDescriptor (Type/getType k)))))
                                        )
                                    )
                                )
                            )
                            (.invokeConstructor gen, (Type/getType (.getClass value)), ctor)
                        )
                        true
                    )
                    (instance? IPersistentMap value)
                    (let [#_"Iterator" it (.iterator (.entrySet value))
                          #_"PersistentVector" v
                            (loop-when [v []] (.hasNext it) => v
                                (let [#_"Map$Entry" e (.next it)]
                                    (recur (conj v (key e) (val e)))
                                )
                            )]
                        (IopObject''emitObjectArray this, (.toArray v), gen)
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.IPersistentMap map(Object[])"))
                        true
                    )
                    (instance? IPersistentVector value)
                    (let [#_"IPersistentVector" args value]
                        (if (<= (count args) Tuple'MAX_SIZE)
                            (do
                                (dotimes [#_"int" i (count args)]
                                    (IopObject''emitValue this, (nth args i), gen)
                                )
                                (.invokeStatic gen, (Type/getType Tuple), (aget Compiler'createTupleMethods (count args)))
                            )
                            (do
                                (IopObject''emitObjectArray this, (.toArray args), gen)
                                (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.IPersistentVector vector(Object[])"))
                            )
                        )
                        true
                    )
                    (instance? PersistentHashSet value)
                    (let [#_"ISeq" vs (seq value)]
                        (if (nil? vs)
                            (do
                                (.getStatic gen, (Type/getType PersistentHashSet), "EMPTY", (Type/getType PersistentHashSet))
                            )
                            (do
                                (IopObject''emitObjectArray this, (RT/seqToArray vs), gen)
                                (.invokeStatic gen, (Type/getType PersistentHashSet), (Method/getMethod "clojure.lang.PersistentHashSet create(Object[])"))
                            )
                        )
                        true
                    )
                    (or (instance? ISeq value) (instance? IPersistentList value))
                    (let [#_"ISeq" vs (seq value)]
                        (IopObject''emitObjectArray this, (RT/seqToArray vs), gen)
                        (.invokeStatic gen, (Type/getType PersistentList), (Method/getMethod "clojure.lang.IPersistentList create(Object[])"))
                        true
                    )
                    (instance? Pattern value)
                    (do
                        (IopObject''emitValue this, (.toString value), gen)
                        (.invokeStatic gen, (Type/getType Pattern), (Method/getMethod "java.util.regex.Pattern compile(String)"))
                        true
                    )
                    :else
                    (let [#_"String" cs
                            (try
                                (RT/printString value)
                                (catch Exception e
                                    (throw (RuntimeException. (str "Can't embed object in code: " value)))
                                )
                            )]
                        (when (zero? (.length cs))
                            (throw (RuntimeException. (str "Can't embed unreadable object in code: " value)))
                        )
                        (when (.startsWith cs, "#<")
                            (throw (RuntimeException. (str "Can't embed unreadable object in code: " cs)))
                        )
                        (.push gen, cs)
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "Object readString(String)"))
                        false
                    )
                )]
            (when partial?
                (when (and (instance? IObj value) (pos? (count (meta value))))
                    (.checkCast gen, (Type/getType IObj))
                    (IopObject''emitValue this, (meta value), gen)
                    (.checkCast gen, (Type/getType IPersistentMap))
                    (.invokeInterface gen, (Type/getType IObj), (Method/getMethod "clojure.lang.IObj withMeta(clojure.lang.IPersistentMap)"))
                )
            )
        )
        nil
    )

    #_method
    (defn #_"IopObject" IopObject''compile [#_"IopObject" this, #_"String" superName, #_"String[]" interfaceNames, #_"boolean" oneTimeUse]
        (binding [*used-constants* #{}]
            ;; create bytecode for a class
            ;; with name current_ns.defname[$letname]+
            ;; anonymous fns get names fn__id
            ;; derived from AFn'RestFn
            (let [#_"ClassWriter" cw (ClassWriter. ClassWriter/COMPUTE_MAXS) #_"ClassVisitor" cv cw]
                (.visit cv, Opcodes/V1_5, (| Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER Opcodes/ACC_FINAL), (:internalName this), nil, superName, interfaceNames)
                (when (IopObject'''supportsMeta this)
                    (.visitField cv, Opcodes/ACC_FINAL, "__meta", (.getDescriptor (Type/getType IPersistentMap)), nil, nil)
                )
                ;; instance fields for closed-overs
                (loop-when-recur [#_"ISeq" s (vals (get *closes* (:uid this)))] (some? s) [(next s)]
                    (let [#_"LocalBinding" lb (first s)
                          #_"String" fd
                            (if (some? (LocalBinding''getPrimitiveType lb))
                                (.getDescriptor (Type/getType (LocalBinding''getPrimitiveType lb)))
                                ;; todo - when closed-overs are fields, use more specific types here and in ctor and emitLocal?
                                (.getDescriptor (Type/getType Object))
                            )]
                        (if (IopObject''isDeftype this)
                            (let [#_"int" access
                                    (cond
                                        (IopObject''isVolatile this, lb) Opcodes/ACC_VOLATILE
                                        (IopObject''isMutable this, lb) 0
                                        :else (| Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL)
                                    )]
                                (.visitField cv, access, (:name lb), fd, nil, nil)
                            )
                            ;; todo - only enable this non-private+writability for letfns where we need it
                            (let [#_"int" access
                                    (if (some? (LocalBinding''getPrimitiveType lb))
                                        (if (IopObject''isVolatile this, lb) Opcodes/ACC_VOLATILE 0)
                                        0
                                    )]
                                (.visitField cv, access, (:name lb), fd, nil, nil)
                            )
                        )
                    )
                )

                ;; static fields for callsites and thunks
                (dotimes [#_"int" i (count (:protocolCallsites this))]
                    (.visitField cv, (| Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC), (Compiler'cachedClassName i), (.getDescriptor (Type/getType Class)), nil, nil)
                )

                ;; ctor that takes closed-overs and inits base + fields
                (let [#_"Method" m (Method. "<init>", Type/VOID_TYPE, (IopObject''ctorTypes this))
                      #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, nil, cv)
                      #_"Label" start (.newLabel ctorgen) #_"Label" end (.newLabel ctorgen)]
                    (.visitCode ctorgen)
                    (.visitLineNumber ctorgen, (:line this), (.mark ctorgen))
                    (.visitLabel ctorgen, start)
                    (.loadThis ctorgen)
                    (.invokeConstructor ctorgen, (Type/getObjectType superName), (Method/getMethod "void <init>()"))

                    (when (IopObject'''supportsMeta this)
                        (.loadThis ctorgen)
                        (.visitVarInsn ctorgen, (.getOpcode (Type/getType IPersistentMap), Opcodes/ILOAD), 1)
                        (.putField ctorgen, (:objType this), "__meta", (Type/getType IPersistentMap))
                    )

                    (let [[this #_"int" a]
                            (loop-when [this this a (if (IopObject'''supportsMeta this) 2 1) #_"ISeq" s (vals (get *closes* (:uid this)))] (some? s) => [this a]
                                (let [#_"LocalBinding" lb (first s)]
                                    (.loadThis ctorgen)
                                    (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)
                                          a (if (some? primc)
                                                (do
                                                    (.visitVarInsn ctorgen, (.getOpcode (Type/getType primc), Opcodes/ILOAD), a)
                                                    (.putField ctorgen, (:objType this), (:name lb), (Type/getType primc))
                                                    (if (any = primc Long/TYPE Double/TYPE) (inc a) a)
                                                )
                                                (do
                                                    (.visitVarInsn ctorgen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), a)
                                                    (.putField ctorgen, (:objType this), (:name lb), (Type/getType Object))
                                                    a
                                                )
                                            )]
                                        (recur (update this :closesExprs conj (LocalBindingExpr'new lb, nil)) (inc a) (next s))
                                    )
                                )
                            )]

                        (.visitLabel ctorgen, end)
                        (.returnValue ctorgen)
                        (.endMethod ctorgen)

                        (when (pos? (:altCtorDrops this))
                            (let [#_"Type[]" ctorTypes (IopObject''ctorTypes this)]

                                ;; ctor that takes closed-overs and inits base + fields
                                (let [#_"Type[]" altCtorTypes (make-array Type (- (alength ctorTypes) (:altCtorDrops this)))
                                      _ (dotimes [#_"int" i (alength altCtorTypes)]
                                            (aset altCtorTypes i (aget ctorTypes i))
                                        )
                                      #_"Method" alt (Method. "<init>", Type/VOID_TYPE, altCtorTypes)
                                      #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, alt, nil, nil, cv)]
                                    (.visitCode ctorgen)
                                    (.loadThis ctorgen)
                                    (.loadArgs ctorgen)

                                    (.visitInsn ctorgen, Opcodes/ACONST_NULL) ;; __meta
                                    (.visitInsn ctorgen, Opcodes/ACONST_NULL) ;; __extmap
                                    (.visitInsn ctorgen, Opcodes/ICONST_0) ;; __hash
                                    (.visitInsn ctorgen, Opcodes/ICONST_0) ;; __hasheq

                                    (.invokeConstructor ctorgen, (:objType this), (Method. "<init>", Type/VOID_TYPE, ctorTypes))

                                    (.returnValue ctorgen)
                                    (.endMethod ctorgen)
                                )

                                ;; alt ctor no __hash, __hasheq
                                (let [#_"Type[]" altCtorTypes (make-array Type (- (alength ctorTypes) 2))
                                      _ (dotimes [#_"int" i (alength altCtorTypes)]
                                            (aset altCtorTypes i (aget ctorTypes i))
                                        )
                                      #_"Method" alt (Method. "<init>", Type/VOID_TYPE, altCtorTypes)
                                      #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, alt, nil, nil, cv)]
                                    (.visitCode ctorgen)
                                    (.loadThis ctorgen)
                                    (.loadArgs ctorgen)

                                    (.visitInsn ctorgen, Opcodes/ICONST_0) ;; __hash
                                    (.visitInsn ctorgen, Opcodes/ICONST_0) ;; __hasheq

                                    (.invokeConstructor ctorgen, (:objType this), (Method. "<init>", Type/VOID_TYPE, ctorTypes))

                                    (.returnValue ctorgen)
                                    (.endMethod ctorgen)
                                )
                            )
                        )

                        (when (IopObject'''supportsMeta this)
                            (let [#_"Type[]" ctorTypes (IopObject''ctorTypes this)]

                                ;; ctor that takes closed-overs but not meta
                                (let [#_"Type[]" noMetaCtorTypes (make-array Type (dec (alength ctorTypes)))
                                      _ (loop-when-recur [#_"int" i 1] (< i (alength ctorTypes)) [(inc i)]
                                            (aset noMetaCtorTypes (dec i) (aget ctorTypes i))
                                        )
                                      #_"Method" alt (Method. "<init>", Type/VOID_TYPE, noMetaCtorTypes)
                                      #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, alt, nil, nil, cv)]
                                    (.visitCode ctorgen)
                                    (.loadThis ctorgen)
                                    (.visitInsn ctorgen, Opcodes/ACONST_NULL) ;; nil meta
                                    (.loadArgs ctorgen)
                                    (.invokeConstructor ctorgen, (:objType this), (Method. "<init>", Type/VOID_TYPE, ctorTypes))
                                    (.returnValue ctorgen)
                                    (.endMethod ctorgen)
                                )

                                ;; meta()
                                (let [#_"Method" meth (Method/getMethod "clojure.lang.IPersistentMap meta()")
                                      #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, meth, nil, nil, cv)]
                                    (.visitCode gen)
                                    (.loadThis gen)
                                    (.getField gen, (:objType this), "__meta", (Type/getType IPersistentMap))
                                    (.returnValue gen)
                                    (.endMethod gen)
                                )

                                ;; withMeta()
                                (let [#_"Method" meth (Method/getMethod "clojure.lang.IObj withMeta(clojure.lang.IPersistentMap)")
                                      #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, meth, nil, nil, cv)]
                                    (.visitCode gen)
                                    (.newInstance gen, (:objType this))
                                    (.dup gen)
                                    (.loadArg gen, 0)
                                    (loop-when-recur [a a #_"ISeq" s (vals (get *closes* (:uid this)))] (some? s) [(inc a) (next s)]
                                        (let [#_"LocalBinding" lb (first s)]
                                            (.loadThis gen)
                                            (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                                                (.getField gen, (:objType this), (:name lb), (if (some? primc) (Type/getType primc) (Type/getType Object)))
                                            )
                                        )
                                    )
                                    (.invokeConstructor gen, (:objType this), (Method. "<init>", Type/VOID_TYPE, ctorTypes))
                                    (.returnValue gen)
                                    (.endMethod gen)
                                )
                            )
                        )

                        (IopObject'''emitStatics this, cv)
                        (IopObject'''emitMethods this, cv)

                        ;; static fields for constants
                        (dotimes [#_"int" i (count (:constants this))]
                            (when (contains? *used-constants* i)
                                (.visitField cv, (| Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL Opcodes/ACC_STATIC), (Compiler'constantName i), (.getDescriptor (IopObject''constantType this, i)), nil, nil)
                            )
                        )

                        ;; static fields for lookup sites
                        (dotimes [#_"int" i (count (:keywordCallsites this))]
                            (.visitField cv, (| Opcodes/ACC_FINAL Opcodes/ACC_STATIC), (Compiler'siteNameStatic i), (.getDescriptor (Type/getType KeywordLookupSite)), nil, nil)
                            (.visitField cv, Opcodes/ACC_STATIC, (Compiler'thunkNameStatic i), (.getDescriptor (Type/getType ILookupThunk)), nil, nil)
                        )

                        ;; static init for constants, keywords and vars
                        (let [#_"GeneratorAdapter" clinitgen (GeneratorAdapter. (| Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (Method/getMethod "void <clinit> ()"), nil, nil, cv)]
                            (.visitCode clinitgen)
                            (.visitLineNumber clinitgen, (:line this), (.mark clinitgen))

                            (when (pos? (count (:constants this)))
                                (IopObject''emitConstants this, clinitgen)
                            )

                            (when (pos? (count (:keywordCallsites this)))
                                (IopObject''emitKeywordCallsites this, clinitgen)
                            )

                            (.returnValue clinitgen)
                            (.endMethod clinitgen)
                            ;; end of class
                            (.visitEnd cv)

                            (assoc this :compiledClass (.defineClass *loader*, (:name this), (.toByteArray cw)))
                        )
                    )
                )
            )
        )
    )

    (defn #_"String" IopObject'trimGenID [#_"String" name]
        (let [#_"int" i (.lastIndexOf name, "__")]
            (if (= i -1) name (.substring name, 0, i))
        )
    )
)

(class-ns FnExpr
    (defn #_"FnExpr" FnExpr'new [#_"Object" tag]
        (merge (FnExpr.) (IopObject'init tag)
            (hash-map
                ;; if there is a variadic overload (there can only be one) it is stored here
                #_"FnMethod" :variadicMethod nil
                #_"IPersistentCollection" :methods nil
                #_"boolean" :hasMeta false
                #_"boolean" :hasEnclosingMethod false
            )
        )
    )

    #_method
    (defn #_"boolean" FnExpr''isVariadic [#_"FnExpr" this]
        (some? (:variadicMethod this))
    )

    (extend-type FnExpr Expr
        (#_"Object" Expr'''eval [#_"FnExpr" this]
            (IopObject''doEval this)
        )

        (#_"void" Expr'''emit [#_"FnExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IopObject''doEmit this, context, objx, gen)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"FnExpr" this]
            true
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"FnExpr" this]
            (if (some? (:tag this)) (Interop'tagToClass (:tag this)) AFunction)
        )
    )

    (extend-type FnExpr IopObject
        (#_"boolean" IopObject'''supportsMeta [#_"FnExpr" this]
            (:hasMeta this)
        )

        (#_"void" IopObject'''emitStatics [#_"FnExpr" this, #_"ClassVisitor" gen]
            nil
        )

        (#_"void" IopObject'''emitMethods [#_"FnExpr" this, #_"ClassVisitor" cv]
            ;; override of invoke/doInvoke for each method
            (loop-when-recur [#_"ISeq" s (seq (:methods this))] (some? s) [(next s)]
                (IopMethod'''emit (first s), this, cv)
            )

            (when (FnExpr''isVariadic this)
                (let [#_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, (Method/getMethod "int getRequiredArity()"), nil, nil, cv)]
                    (.visitCode gen)
                    (.push gen, (count (:reqParms (:variadicMethod this))))
                    (.returnValue gen)
                    (.endMethod gen)
                )
            )
            nil
        )
    )

    (defn #_"Expr" FnExpr'parse [#_"Context" context, #_"ISeq" form, #_"String" name]
        (let [#_"IPersistentMap" fmeta (meta form)
              #_"IopMethod" owner *method*
              #_"FnExpr" fn
                (-> (FnExpr'new (Compiler'tagOf form))
                    (assoc :hasEnclosingMethod (some? owner) :line *line*)
                )
              fn (when (some? (meta (first form))) => fn
                    (assoc fn :onceOnly (boolean (get (meta (first form)) :once)))
                )
              #_"String" basename (if (some? owner) (:name (:objx owner)) (Compiler'munge (ßname (ßname *ns*))))
              [#_"Symbol" nm name]
                (if (symbol? (second form))
                    (let [nm (second form)]
                        [nm (str (ßname nm) "__" (RT/nextID))]
                    )
                    (cond
                        (nil? name)   [nil (str "fn__" (RT/nextID))]
                        (some? owner) [nil (str name "__"(RT/nextID))]
                        :else         [nil name]
                    )
                )
              fn (assoc fn :name (str basename "$" (.replace (Compiler'munge name), ".", "_DOT_")))
              fn (assoc fn :internalName (.replace (:name fn), \., \/))
              fn (assoc fn :objType (Type/getObjectType (:internalName fn)))
              #_"Object" rettag (get fmeta :rettag)
              fn
                (binding [*constants*          []
                          *constant-ids*       (IdentityHashMap.)
                          *keywords*           {}
                          *vars*               {}
                          *keyword-callsites*  []
                          *protocol-callsites* []
                          *no-recur*           false]
                    ;; arglist might be preceded by symbol naming this fn
                    (let [[fn form]
                            (when (some? nm) => [fn form]
                                [(assoc fn :thisName (ßname nm)) (cons 'fn* (next (next form)))]
                            )
                          ;; now (fn [args] body...) or (fn ([args] body...) ([args2] body2...) ...)
                          ;; turn former into latter
                          form
                            (when (instance? IPersistentVector (second form)) => form
                                (list 'fn* (next form))
                            )
                          #_"FnMethod[]" a (make-array #_"FnMethod" Object (inc Compiler'MAX_POSITIONAL_ARITY))
                          #_"FnMethod" variadic
                            (loop-when [variadic nil #_"ISeq" s (next form)] (some? s) => variadic
                                (let [#_"FnMethod" f (FnMethod'parse fn, (first s), rettag)
                                      variadic
                                        (if (FnMethod''isVariadic f)
                                            (when (nil? variadic) => (throw (RuntimeException. "Can't have more than 1 variadic overload"))
                                                f
                                            )
                                            (let [#_"int" n (count (:reqParms f))]
                                                (when (nil? (aget a n)) => (throw (RuntimeException. "Can't have 2 overloads with same arity"))
                                                    (aset a n f)
                                                    variadic
                                                )
                                            )
                                        )]
                                    (recur variadic (next s))
                                )
                            )]
                        (when (some? variadic)
                            (loop-when-recur [#_"int" i (inc (count (:reqParms variadic)))] (<= i Compiler'MAX_POSITIONAL_ARITY) [(inc i)]
                                (when (some? (aget a i))
                                    (throw (RuntimeException. "Can't have fixed arity function with more params than variadic function"))
                                )
                            )
                        )
                        (let [#_"IPersistentCollection" methods
                                (loop-when-recur [methods nil #_"int" i 0]
                                                 (< i (alength a))
                                                 [(if (some? (aget a i)) (conj methods (aget a i)) methods) (inc i)]
                                              => (if (some? variadic) (conj methods variadic) methods)
                                )]
                            (assoc fn
                                :methods methods
                                :variadicMethod variadic
                                :keywords *keywords*
                                :vars *vars*
                                :constants *constants*
                                :keywordCallsites *keyword-callsites*
                                :protocolCallsites *protocol-callsites*
                            )
                        )
                    )
                )
              fmeta
                (when (some? fmeta)
                    (dissoc fmeta :line :column :rettag)
                )
              fn (assoc fn :hasMeta (pos? (count fmeta)))
              fn (IopObject''compile fn, (if (FnExpr''isVariadic fn) "clojure/lang/RestFn" "clojure/lang/AFunction"), nil, (:onceOnly fn))]
            (when (IopObject'''supportsMeta fn) => fn
                (MetaExpr'new fn, (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), fmeta))
            )
        )
    )

    #_method
    (defn #_"void" FnExpr''emitForDefn [#_"FnExpr" this, #_"IopObject" objx, #_"GeneratorAdapter" gen]
        (Expr'''emit this, :Context'EXPRESSION, objx, gen)
        nil
    )
)

(class-ns DefExpr
    (defn #_"DefExpr" DefExpr'new [#_"int" line, #_"Var" var, #_"Expr" init, #_"Expr" meta, #_"boolean" initProvided, #_"boolean" isDynamic, #_"boolean" shadowsCoreMapping]
        (merge (DefExpr.)
            (hash-map
                #_"int" :line line
                #_"Var" :var var
                #_"Expr" :init init
                #_"Expr" :meta meta
                #_"boolean" :initProvided initProvided
                #_"boolean" :isDynamic isDynamic
                #_"boolean" :shadowsCoreMapping shadowsCoreMapping
            )
        )
    )

    #_method
    (defn- #_"boolean" DefExpr''includesExplicitMetadata [#_"DefExpr" this, #_"MapExpr" expr]
        (loop-when [#_"int" i 0] (< i (count (:keyvals expr))) => false
            (recur-if (any = (:k (nth (:keyvals expr) i)) :declared :line :column) [(+ i 2)] => true)
        )
    )

    (extend-type DefExpr Expr
        (#_"Object" Expr'''eval [#_"DefExpr" this]
            (when (:initProvided this)
                (.bindRoot (:var this), (Expr'''eval (:init this)))
            )
            (when (some? (:meta this))
                (let [#_"IPersistentMap" metaMap (Expr'''eval (:meta this))]
                    (when (or (:initProvided this) true)
                        (.setMeta (:var this), metaMap)
                    )
                )
            )
            (.setDynamic (:var this), (:isDynamic this))
        )

        (#_"void" Expr'''emit [#_"DefExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IopObject''emitVar objx, gen, (:var this))
            (when (:shadowsCoreMapping this)
                (.dup gen)
                (.getField gen, (Type/getType Var), "ns", (Type/getType Namespace))
                (.swap gen)
                (.dup gen)
                (.getField gen, (Type/getType Var), "sym", (Type/getType Symbol))
                (.swap gen)
                (.invokeVirtual gen, (Type/getType Namespace), (Method/getMethod "clojure.lang.Var refer(clojure.lang.Symbol, clojure.lang.Var)"))
            )
            (when (:isDynamic this)
                (.push gen, (:isDynamic this))
                (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "clojure.lang.Var setDynamic(boolean)"))
            )
            (when (some? (:meta this))
                (.dup gen)
                (Expr'''emit (:meta this), :Context'EXPRESSION, objx, gen)
                (.checkCast gen, (Type/getType IPersistentMap))
                (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "void setMeta(clojure.lang.IPersistentMap)"))
            )
            (when (:initProvided this)
                (.dup gen)
                (if (instance? FnExpr (:init this))
                    (FnExpr''emitForDefn (:init this), objx, gen)
                    (Expr'''emit (:init this), :Context'EXPRESSION, objx, gen)
                )
                (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "void bindRoot(Object)"))
            )
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"DefExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"DefExpr" this]
            Var
        )
    )
)

(class-ns DefParser
    (defn #_"IParser" DefParser'new []
        (reify IParser
            ;; (def x) or (def x initexpr)
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (cond
                    (< 3 (count form))            (throw (IllegalArgumentException. "Too many arguments to def"))
                    (< (count form) 2)            (throw (IllegalArgumentException. "Too few arguments to def"))
                    (not (symbol? (second form))) (throw (IllegalArgumentException. "First argument to def must be a Symbol"))
                )
                (let [#_"Symbol" sym (second form) #_"Var" v (Compiler'lookupVar sym, true)]
                    (when (some? v) => (throw (RuntimeException. "Can't refer to qualified var that doesn't exist"))
                        (let [[v #_"boolean" shadowsCoreMapping]
                                (when-not (= (ßns v) *ns*) => [v false]
                                    (when (nil? (ßns sym)) => (throw (RuntimeException. "Can't create defs outside of current ns"))
                                        (let [v (.intern *ns*, sym)]
                                            (Compiler'registerVar v)
                                            [v true]
                                        )
                                    )
                                )
                              #_"IPersistentMap" m (meta sym) #_"boolean" dynamic? (boolean (get m :dynamic))]
                            (when dynamic?
                                (.setDynamic v)
                            )
                            (when (and (not dynamic?) (.startsWith (ßname sym), "*") (.endsWith (ßname sym), "*") (< 2 (.length (ßname sym))))
                                (.println *err*, (str "Warning: " sym " not declared dynamic and thus is not dynamically rebindable, but its name suggests otherwise. Please either indicate ^:dynamic or change the name."))
                            )
                            (let [#_"Context" c (if (= context :Context'EVAL) context :Context'EXPRESSION)
                                  m (assoc m :line *line*)]
                                (DefExpr'new *line*, v, (Compiler'analyze c, (third form), (ßname (ßsym v))), (Compiler'analyze c, m), (= (count form) 3), dynamic?, shadowsCoreMapping)
                            )
                        )
                    )
                )
            )
        )
    )
)

(class-ns BindingInit
    (defn #_"BindingInit" BindingInit'new [#_"LocalBinding" binding, #_"Expr" init]
        (merge (BindingInit.)
            (hash-map
                #_"LocalBinding" :binding binding
                #_"Expr" :init init
            )
        )
    )
)

(class-ns LetFnExpr
    (defn #_"LetFnExpr" LetFnExpr'new [#_"PersistentVector" bindingInits, #_"Expr" body]
        (merge (LetFnExpr.)
            (hash-map
                #_"PersistentVector" :bindingInits bindingInits
                #_"Expr" :body body
            )
        )
    )

    (extend-type LetFnExpr Expr
        (#_"Object" Expr'''eval [#_"LetFnExpr" this]
            (throw (UnsupportedOperationException. "Can't eval letfns"))
        )

        (#_"void" Expr'''emit [#_"LetFnExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (dotimes [#_"int" i (count (:bindingInits this))]
                (let [#_"BindingInit" bi (nth (:bindingInits this) i)]
                    (.visitInsn gen, Opcodes/ACONST_NULL)
                    (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx (:binding bi)))
                )
            )
            (let [#_"IPersistentSet" lbset
                    (loop-when [lbset #{} #_"int" i 0] (< i (count (:bindingInits this))) => lbset
                        (let [#_"BindingInit" bi (nth (:bindingInits this) i)]
                            (Expr'''emit (:init bi), :Context'EXPRESSION, objx, gen)
                            (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx (:binding bi)))
                            (recur (conj lbset (:binding bi)) (inc i))
                        )
                    )]
                (dotimes [#_"int" i (count (:bindingInits this))]
                    (let [#_"BindingInit" bi (nth (:bindingInits this) i)]
                        (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), (:idx (:binding bi)))
                        (IopObject''emitLetFnInits (:init bi), gen, objx, lbset)
                    )
                )
                (let [#_"Label" loopLabel (.mark gen)]
                    (Expr'''emit (:body this), context, objx, gen)
                    (let [#_"Label" end (.mark gen)]
                        (loop-when-recur [#_"ISeq" bis (seq (:bindingInits this))] (some? bis) [(next bis)]
                            (let [#_"BindingInit" bi (first bis)
                                #_"String" lname (:name (:binding bi)) lname (if (.endsWith lname, "__auto__") (str lname (RT/nextID)) lname)
                                #_"Class" primc (Compiler'maybePrimitiveType (:init bi))]
                                (.visitLocalVariable gen, lname, (if (some? primc) (Type/getDescriptor primc) "Ljava/lang/Object;"), nil, loopLabel, end, (:idx (:binding bi)))
                            )
                        )
                    )
                )
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"LetFnExpr" this]
            (Expr'''hasJavaClass (:body this))
        )

        (#_"Class" Expr'''getJavaClass [#_"LetFnExpr" this]
            (Expr'''getJavaClass (:body this))
        )
    )
)

(class-ns LetFnParser
    (defn #_"IParser" LetFnParser'new []
        (reify IParser
            ;; (letfns* [var (fn [args] body) ...] body...)
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (instance? IPersistentVector (second form)) => (throw (IllegalArgumentException. "Bad binding form, expected vector"))
                    (let [#_"IPersistentVector" bindings (second form)]
                        (when (zero? (% (count bindings) 2)) => (throw (IllegalArgumentException. "Bad binding form, expected matched symbol expression pairs"))
                            (if (= context :Context'EVAL)
                                (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                                (binding [*local-env* *local-env*, *last-local-num* *last-local-num*]
                                    ;; pre-seed env (like Lisp labels)
                                    (let [#_"PersistentVector" lbs
                                            (loop-when [lbs [] #_"int" i 0] (< i (count bindings)) => lbs
                                                (let-when [#_"Object" sym (nth bindings i)] (symbol? sym) => (throw (IllegalArgumentException. (str "Bad binding form, expected symbol, got: " sym)))
                                                    (when (nil? (namespace sym)) => (throw (RuntimeException. (str "Can't let qualified name: " sym)))
                                                        (recur (conj lbs (Compiler'registerLocal sym, (Compiler'tagOf sym), nil, false)) (+ i 2))
                                                    )
                                                )
                                            )
                                          #_"PersistentVector" bis
                                            (loop-when [bis [] #_"int" i 0] (< i (count bindings)) => bis
                                                (let [#_"Expr" init (Compiler'analyze :Context'EXPRESSION, (nth bindings (inc i)), (:name (nth bindings i)))
                                                      #_"LocalBinding" lb (Compiler'complementLocalInit (nth lbs (/ i 2)), init)]
                                                    (recur (conj bis (BindingInit'new lb, init)) (+ i 2))
                                                )
                                            )]
                                        (LetFnExpr'new bis, (IParser'''parse (BodyParser'new), context, (next (next form))))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)

(class-ns LetExpr
    (defn #_"LetExpr" LetExpr'new [#_"PersistentVector" bindingInits, #_"Expr" body, #_"boolean" isLoop]
        (merge (LetExpr.)
            (hash-map
                #_"PersistentVector" :bindingInits bindingInits
                #_"Expr" :body body
                #_"boolean" :isLoop isLoop
            )
        )
    )

    #_method
    (defn- #_"void" LetExpr''doEmit [#_"LetExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"boolean" emitUnboxed]
        (let [#_"HashMap<BindingInit, Label>" bindingLabels (HashMap.)]
            (dotimes [#_"int" i (count (:bindingInits this))]
                (let [#_"BindingInit" bi (nth (:bindingInits this) i)
                      #_"Class" primc (Compiler'maybePrimitiveType (:init bi))]
                    (if (some? primc)
                        (do
                            (MaybePrimitive'''emitUnboxed (:init bi), :Context'EXPRESSION, objx, gen)
                            (.visitVarInsn gen, (.getOpcode (Type/getType primc), Opcodes/ISTORE), (:idx (:binding bi)))
                        )
                        (do
                            (Expr'''emit (:init bi), :Context'EXPRESSION, objx, gen)
                            (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx (:binding bi)))
                        )
                    )
                    (.put bindingLabels, bi, (.mark gen))
                )
            )
            (let [#_"Label" loopLabel (.mark gen)]
                (if (:isLoop this)
                    (binding [*loop-label* loopLabel]
                        (if emitUnboxed
                            (MaybePrimitive'''emitUnboxed (:body this), context, objx, gen)
                            (Expr'''emit (:body this), context, objx, gen)
                        )
                    )
                    (if emitUnboxed
                        (MaybePrimitive'''emitUnboxed (:body this), context, objx, gen)
                        (Expr'''emit (:body this), context, objx, gen)
                    )
                )
                (let [#_"Label" end (.mark gen)]
                    (loop-when-recur [#_"ISeq" bis (seq (:bindingInits this))] (some? bis) [(next bis)]
                        (let [#_"BindingInit" bi (first bis)
                              #_"String" lname (:name (:binding bi)) lname (if (.endsWith lname, "__auto__") (str lname (RT/nextID)) lname)
                              #_"Class" primc (Compiler'maybePrimitiveType (:init bi))]
                            (.visitLocalVariable gen, lname, (if (some? primc) (Type/getDescriptor primc) "Ljava/lang/Object;"), nil, (get bindingLabels bi), end, (:idx (:binding bi)))
                        )
                    )
                )
            )
        )
        nil
    )

    (extend-type LetExpr Expr
        (#_"Object" Expr'''eval [#_"LetExpr" this]
            (throw (UnsupportedOperationException. "Can't eval let/loop"))
        )

        (#_"void" Expr'''emit [#_"LetExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (LetExpr''doEmit this, context, objx, gen, false)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"LetExpr" this]
            (Expr'''hasJavaClass (:body this))
        )

        (#_"Class" Expr'''getJavaClass [#_"LetExpr" this]
            (Expr'''getJavaClass (:body this))
        )
    )

    (extend-type LetExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"LetExpr" this]
            (and (satisfies? MaybePrimitive (:body this)) (MaybePrimitive'''canEmitPrimitive (:body this)))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"LetExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (LetExpr''doEmit this, context, objx, gen, true)
            nil
        )
    )
)

(class-ns LetParser
    (defn #_"IParser" LetParser'new []
        (reify IParser
            ;; (let [var val var2 val2 ...] body...)
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"boolean" isLoop (= (first form) 'loop*)]
                    (when (instance? IPersistentVector (second form)) => (throw (IllegalArgumentException. "Bad binding form, expected vector"))
                        (let [#_"IPersistentVector" bindings (second form)]
                            (when (zero? (% (count bindings) 2)) => (throw (IllegalArgumentException. "Bad binding form, expected matched symbol expression pairs"))
                                (if (or (= context :Context'EVAL) (and (= context :Context'EXPRESSION) isLoop))
                                    (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                                    (let [#_"ISeq" body (next (next form))
                                          #_"IPersistentMap" locals' (:locals *method*)]
                                        ;; may repeat once for each binding with a mismatch, return breaks
                                        (loop [#_"IPersistentVector" rms (vec (repeat (/ (count bindings) 2) false))]
                                            (let [#_"IPersistentMap" dynamicBindings
                                                    (hash-map
                                                        #'*local-env*      *local-env*
                                                        #'*last-local-num* *last-local-num*
                                                    )
                                                  dynamicBindings
                                                    (when isLoop => dynamicBindings
                                                        (assoc dynamicBindings #'*loop-locals* nil)
                                                    )
                                                  _ (update! *method* assoc :locals locals')
                                                  [rms #_"LetExpr" letExpr]
                                                    (try
                                                        (push-thread-bindings dynamicBindings)
                                                        (let [[#_"PersistentVector" bindingInits #_"PersistentVector" loopLocals]
                                                                (loop-when [bindingInits [] loopLocals [] #_"int" i 0] (< i (count bindings)) => [bindingInits loopLocals]
                                                                    (let-when [#_"Object" sym (nth bindings i)] (symbol? sym) => (throw (IllegalArgumentException. (str "Bad binding form, expected symbol, got: " sym)))
                                                                        (when (nil? (namespace sym)) => (throw (RuntimeException. (str "Can't let qualified name: " sym)))
                                                                            (let [#_"Expr" init (Compiler'analyze :Context'EXPRESSION, (nth bindings (inc i)), (ßname sym))
                                                                                  init
                                                                                    (when isLoop => init
                                                                                        (if (and (some? rms) (nth rms (/ i 2)))
                                                                                            (do
                                                                                                (when *warn-on-reflection*
                                                                                                    (.println *err*, (str "Auto-boxing loop arg: " sym))
                                                                                                )
                                                                                                (StaticMethodExpr'new 0, nil, RT, "box", [init], false)
                                                                                            )
                                                                                            (condp = (Compiler'maybePrimitiveType init)
                                                                                                Integer/TYPE (StaticMethodExpr'new 0, nil, RT, "longCast", [init], false)
                                                                                                Float/TYPE   (StaticMethodExpr'new 0, nil, RT, "doubleCast", [init], false)
                                                                                                             init
                                                                                            )
                                                                                        )
                                                                                    )
                                                                                  ;; sequential enhancement of env (like Lisp let*)
                                                                                  [bindingInits loopLocals]
                                                                                    (try
                                                                                        (when isLoop
                                                                                            (push-thread-bindings (hash-map #'*no-recur* false))
                                                                                        )
                                                                                        (let [#_"LocalBinding" lb (Compiler'registerLocal sym, (Compiler'tagOf sym), init, false)]
                                                                                            [(conj bindingInits (BindingInit'new lb, init)) (if isLoop (conj loopLocals lb) loopLocals)]
                                                                                        )
                                                                                        (finally
                                                                                            (when isLoop
                                                                                                (pop-thread-bindings)
                                                                                            )
                                                                                        )
                                                                                    )]
                                                                                (recur bindingInits loopLocals (+ i 2))
                                                                            )
                                                                        )
                                                                    )
                                                                )]
                                                            (when isLoop
                                                                (set! *loop-locals* loopLocals)
                                                            )
                                                            (let [#_"Expr" bodyExpr
                                                                    (try
                                                                        (when isLoop
                                                                            (push-thread-bindings
                                                                                (hash-map
                                                                                    #'*no-recur*          false
                                                                                    #'*in-return-context* (and (= context :Context'RETURN) *in-return-context*)
                                                                                )
                                                                            )
                                                                        )
                                                                        (IParser'''parse (BodyParser'new), (if isLoop :Context'RETURN context), body)
                                                                        (finally
                                                                            (when isLoop
                                                                                (pop-thread-bindings)
                                                                            )
                                                                        )
                                                                    )
                                                                  [rms #_"boolean" more?]
                                                                    (when isLoop => [rms false]
                                                                        (loop-when [rms rms more? false #_"int" i 0] (< i (count *loop-locals*)) => [rms more?]
                                                                            (let [[rms more?]
                                                                                    (when (:recurMistmatch (nth *loop-locals* i)) => [rms more?]
                                                                                        [(assoc rms i true) true]
                                                                                    )]
                                                                                (recur rms more? (inc i))
                                                                            )
                                                                        )
                                                                    )]
                                                                [rms (when-not more? (LetExpr'new bindingInits, bodyExpr, isLoop))]
                                                            )
                                                        )
                                                        (finally
                                                            (pop-thread-bindings)
                                                        )
                                                    )]
                                                (recur-if (nil? letExpr) [rms] => letExpr)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)

(class-ns RecurExpr
    (defn #_"RecurExpr" RecurExpr'new [#_"IPersistentVector" loopLocals, #_"IPersistentVector" args, #_"int" line]
        (merge (RecurExpr.)
            (hash-map
                #_"IPersistentVector" :loopLocals loopLocals
                #_"IPersistentVector" :args args
                #_"int" :line line
            )
        )
    )

    (extend-type RecurExpr Expr
        (#_"Object" Expr'''eval [#_"RecurExpr" this]
            (throw (UnsupportedOperationException. "Can't eval recur"))
        )

        (#_"void" Expr'''emit [#_"RecurExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (let-when [#_"Label" loopLabel *loop-label*] (some? loopLabel) => (throw (IllegalStateException.))
                (dotimes [#_"int" i (count (:loopLocals this))]
                    (let [#_"LocalBinding" lb (nth (:loopLocals this) i) #_"Expr" arg (nth (:args this) i)]
                        (when (some? (LocalBinding''getPrimitiveType lb)) => (Expr'''emit arg, :Context'EXPRESSION, objx, gen)
                            (let [#_"Class" primc (LocalBinding''getPrimitiveType lb) #_"Class" pc (Compiler'maybePrimitiveType arg)]
                                (cond (= primc pc)
                                    (do
                                        (MaybePrimitive'''emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                    )
                                    (and (= primc Long/TYPE) (= pc Integer/TYPE))
                                    (do
                                        (MaybePrimitive'''emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                        (.visitInsn gen, Opcodes/I2L)
                                    )
                                    (and (= primc Double/TYPE) (= pc Float/TYPE))
                                    (do
                                        (MaybePrimitive'''emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                        (.visitInsn gen, Opcodes/F2D)
                                    )
                                    (and (= primc Integer/TYPE) (= pc Long/TYPE))
                                    (do
                                        (MaybePrimitive'''emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "int intCast(long)"))
                                    )
                                    (and (= primc Float/TYPE) (= pc Double/TYPE))
                                    (do
                                        (MaybePrimitive'''emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                        (.visitInsn gen, Opcodes/D2F)
                                    )
                                    :else
                                    (do
                                        (throw (IllegalArgumentException. (str "recur arg for primitive local: " (:name lb) " is not matching primitive, had: " (if (Expr'''hasJavaClass arg) (.getName (Expr'''getJavaClass arg)) "Object") ", needed: " (.getName primc))))
                                    )
                                )
                            )
                        )
                    )
                )
                (loop-when-recur [#_"int" i (dec (count (:loopLocals this)))] (<= 0 i) [(dec i)]
                    (let [#_"LocalBinding" lb (nth (:loopLocals this) i) #_"Class" primc (LocalBinding''getPrimitiveType lb)]
                        (if (:isArg lb)
                            (.storeArg gen, (dec (:idx lb)))
                            (.visitVarInsn gen, (.getOpcode (if (some? primc) (Type/getType primc) (Type/getType Object)), Opcodes/ISTORE), (:idx lb))
                        )
                    )
                )
                (.goTo gen, loopLabel)
            )
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"RecurExpr" this]
            true
        )

        (#_"Class" Expr'''getJavaClass [#_"RecurExpr" this]
            Recur
        )
    )

    (extend-type RecurExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"RecurExpr" this]
            true
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"RecurExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit this, context, objx, gen)
            nil
        )
    )
)

(class-ns RecurParser
    (defn #_"IParser" RecurParser'new []
        (reify IParser
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when-not (and (= context :Context'RETURN) (some? *loop-locals*))
                    (throw (UnsupportedOperationException. "Can only recur from tail position"))
                )
                (when *no-recur*
                    (throw (UnsupportedOperationException. "Cannot recur across try"))
                )
                (let [#_"int" line *line*
                      #_"PersistentVector" args
                        (loop-when-recur [args [] #_"ISeq" s (seq (next form))]
                                         (some? s)
                                         [(conj args (Compiler'analyze :Context'EXPRESSION, (first s))) (next s)]
                                      => args
                        )]
                    (when-not (= (count args) (count *loop-locals*))
                        (throw (IllegalArgumentException. (str "Mismatched argument count to recur, expected: " (count *loop-locals*) " args, got: " (count args))))
                    )
                    (dotimes [#_"int" i (count *loop-locals*)]
                        (let [#_"LocalBinding" lb (nth *loop-locals* i)]
                            (when-let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                                (let [#_"Class" pc (Compiler'maybePrimitiveType (nth args i))
                                      #_"boolean" mismatch?
                                        (condp = primc
                                            Long/TYPE   (not (any = pc Long/TYPE Integer/TYPE Short/TYPE Character/TYPE Byte/TYPE))
                                            Double/TYPE (not (any = pc Double/TYPE Float/TYPE))
                                                        false
                                        )]
                                    (when mismatch?
                                        (update! *loop-locals* update i assoc :recurMistmatch true)
                                        (when *warn-on-reflection*
                                            (.println *err*, (str "line " line ": recur arg for primitive local: " (:name lb) " is not matching primitive, had: " (if (some? pc) (.getName pc) "Object") ", needed: " (.getName primc)))
                                        )
                                    )
                                )
                            )
                        )
                    )
                    (RecurExpr'new *loop-locals*, args, line)
                )
            )
        )
    )
)

(class-ns NewInstanceMethod
    (defn #_"NewInstanceMethod" NewInstanceMethod'new [#_"IopObject" objx, #_"IopMethod" parent]
        (merge (NewInstanceMethod.) (IopMethod'init objx, parent)
            (hash-map
                #_"String" :name nil
                #_"Type[]" :argTypes nil
                #_"Type" :retType nil
                #_"Class" :retClass nil
                #_"Class[]" :exClasses nil

                #_"IPersistentVector" :parms nil
            )
        )
    )

    (extend-type NewInstanceMethod IopMethod
        (#_"int" IopMethod'''numParams [#_"NewInstanceMethod" this]
            (count (:argLocals this))
        )

        (#_"String" IopMethod'''getMethodName [#_"NewInstanceMethod" this]
            (:name this)
        )

        (#_"Type" IopMethod'''getReturnType [#_"NewInstanceMethod" this]
            (:retType this)
        )

        (#_"Type[]" IopMethod'''getArgTypes [#_"NewInstanceMethod" this]
            (:argTypes this)
        )

        (#_"void" IopMethod'''emit [#_"NewInstanceMethod" this, #_"IopObject" obj, #_"ClassVisitor" cv]
            (let [#_"Method" m (Method. (IopMethod'''getMethodName this), (IopMethod'''getReturnType this), (IopMethod'''getArgTypes this))
                #_"Type[]" exTypes
                    (let-when [#_"int" n (alength (:exClasses this))] (pos? n)
                        (let [exTypes (make-array Type n)]
                            (dotimes [#_"int" i n]
                                (aset exTypes i (Type/getType (aget (:exClasses this) i)))
                            )
                            exTypes
                        )
                    )
                #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, exTypes, cv)]
                (.visitCode gen)
                (let [#_"Label" loopLabel (.mark gen)]
                    (.visitLineNumber gen, (:line this), loopLabel)
                    (binding [*loop-label* loopLabel, *method* this]
                        (IopMethod'emitBody (:objx this), gen, (:retClass this), (:body this))
                        (let [#_"Label" end (.mark gen)]
                            (.visitLocalVariable gen, "this", (.getDescriptor (:objType obj)), nil, loopLabel, end, 0)
                            (loop-when-recur [#_"ISeq" lbs (seq (:argLocals this))] (some? lbs) [(next lbs)]
                                (let [#_"LocalBinding" lb (first lbs)]
                                    (.visitLocalVariable gen, (:name lb), (.getDescriptor (aget (:argTypes this) (dec (:idx lb)))), nil, loopLabel, end, (:idx lb))
                                )
                            )
                        )
                    )
                    (.returnValue gen)
                    (.endMethod gen)
                )
            )
            nil
        )
    )

    (defn #_"IPersistentVector" NewInstanceMethod'msig [#_"String" name, #_"Class[]" paramTypes]
        [name (seq paramTypes)]
    )

    (defn- #_"Map" NewInstanceMethod'findMethodsWithNameAndArity [#_"String" name, #_"int" arity, #_"Map" mm]
        (let [#_"Map" found (HashMap.)]
            (doseq [#_"Map$Entry" e (.entrySet mm)]
                (let [#_"java.lang.reflect.Method" m (val e)]
                    (when (and (= name (.getName m)) (= (alength (.getParameterTypes m)) arity))
                        (.put found, (key e), (val e))
                    )
                )
            )
            found
        )
    )

    (defn- #_"Map" NewInstanceMethod'findMethodsWithName [#_"String" name, #_"Map" mm]
        (let [#_"Map" found (HashMap.)]
            (doseq [#_"Map$Entry" e (.entrySet mm)]
                (let [#_"java.lang.reflect.Method" m (val e)]
                    (when (= name (.getName m))
                        (.put found, (key e), (val e))
                    )
                )
            )
            found
        )
    )

    (defn #_"NewInstanceMethod" NewInstanceMethod'parse [#_"IopObject" objx, #_"ISeq" form, #_"Symbol" thistag, #_"Map" overrideables]
        ;; (methodname [this-name args*] body...)
        ;; this-name might be nil
        (let [#_"NewInstanceMethod" nim
                (-> (NewInstanceMethod'new objx, *method*)
                    (assoc :line *line*)
                )
              #_"Symbol" dotname (first form) #_"Symbol" name (with-meta (symbol (Compiler'munge (ßname dotname))) (meta dotname))
              #_"IPersistentVector" parms (second form)]
            (when (pos? (count parms)) => (throw (IllegalArgumentException. (str "Must supply at least one argument for 'this' in: " dotname)))
                (let [#_"Symbol" thisName (nth parms 0) parms (subvec parms 1 (count parms))
                      #_"ISeq" body (next (next form))]
                    ;; register as the current method and set up a new env frame
                    (binding [*method*            nim
                              *local-env*         *local-env*
                              *last-local-num*    -1
                              *loop-locals*       nil
                              *in-return-context* true]
                        ;; register 'this' as local 0
                        (if (some? thisName)
                            (Compiler'registerLocal thisName, thistag, nil, false)
                            (Compiler'nextLocalNum)
                        )
                        (let [nim (assoc nim :retClass (Interop'tagClass (Compiler'tagOf name)))
                              nim (assoc nim :argTypes (make-array Type (count parms)))
                              #_"Class[]" pclasses (make-array Class (count parms))
                              #_"Symbol[]" psyms (make-array #_"Symbol" Object (count parms))
                              #_"boolean" hinted?
                                (loop-when [hinted? (some? (Compiler'tagOf name)) #_"int" i 0] (< i (count parms)) => hinted?
                                    (let-when [#_"Object" sym (nth parms i)] (symbol? sym) => (throw (IllegalArgumentException. "params must be Symbols"))
                                        (let [#_"Object" tag (Compiler'tagOf sym) hinted? (or hinted? (some? tag))]
                                            (aset pclasses i (Interop'tagClass tag))
                                            (aset psyms i (if (some? (namespace sym)) (symbol (ßname sym)) sym))
                                            (recur hinted? (inc i))
                                        )
                                    )
                                )
                              #_"Map" matches (NewInstanceMethod'findMethodsWithNameAndArity (ßname name), (count parms), overrideables)
                              #_"Object" mk (NewInstanceMethod'msig (ßname name), pclasses)
                              [nim pclasses #_"java.lang.reflect.Method" m]
                                (case (count matches)
                                    0   (throw (IllegalArgumentException. (str "Can't define method not in interfaces: " (ßname name))))
                                    1   (if hinted? ;; validate match
                                            (let [m (get matches mk)]
                                                (when (nil? m)
                                                    (throw (IllegalArgumentException. (str "Can't find matching method: " (ßname name) ", leave off hints for auto match.")))
                                                )
                                                (when-not (= (.getReturnType m) (:retClass nim))
                                                    (throw (IllegalArgumentException. (str "Mismatched return type: " (ßname name) ", expected: " (.getName (.getReturnType m)) ", had: " (.getName (:retClass nim)))))
                                                )
                                                [nim pclasses m]
                                            )
                                            ;; adopt found method sig
                                            (let [m (.next (.iterator (.values matches)))]
                                                [(assoc nim :retClass (.getReturnType m)) (.getParameterTypes m) m]
                                            )
                                        )
                                        ;; must be hinted and match one method
                                        (when hinted? => (throw (IllegalArgumentException. (str "Must hint overloaded method: " (ßname name))))
                                            (let [m (get matches mk)]
                                                (when (nil? m)
                                                    (throw (IllegalArgumentException. (str "Can't find matching overloaded method: " (ßname name))))
                                                )
                                                (when-not (= (.getReturnType m) (:retClass nim))
                                                    (throw (IllegalArgumentException. (str "Mismatched return type: " (ßname name) ", expected: " (.getName (.getReturnType m)) ", had: " (.getName (:retClass nim)))))
                                                )
                                                [nim pclasses m]
                                            )
                                        )
                                )
                              ;; validate unique name+arity among additional methods
                              nim (assoc nim :retType (Type/getType (:retClass nim)))
                              nim (assoc nim :exClasses (.getExceptionTypes m))
                              #_"PersistentVector" argLocals
                                (loop-when [argLocals [] #_"int" i 0] (< i (count parms)) => argLocals
                                    (let [#_"LocalBinding" lb (Compiler'registerLocal (aget psyms i), nil, (MethodParamExpr'new (aget pclasses i)), true)]
                                        (aset (:argTypes nim) i (Type/getType (aget pclasses i)))
                                        (recur (conj argLocals lb) (inc i))
                                    )
                                )]
                            (dotimes [#_"int" i (count parms)]
                                (when (any = (aget pclasses i) Long/TYPE Double/TYPE)
                                    (Compiler'nextLocalNum)
                                )
                            )
                            (set! *loop-locals* argLocals)
                            (assoc nim
                                :name (ßname name)
                                :methodMeta (meta name)
                                :parms parms
                                :argLocals argLocals
                                :body (IParser'''parse (BodyParser'new), :Context'RETURN, body)
                            )
                        )
                    )
                )
            )
        )
    )
)

(class-ns NewInstanceExpr
    (defn- #_"NewInstanceExpr" NewInstanceExpr'new [#_"Object" tag]
        (merge (NewInstanceExpr.) (IopObject'init tag)
            (hash-map
                #_"IPersistentCollection" :methods nil

                #_"Map<IPersistentVector, java.lang.reflect.Method>" :overrideables nil
                #_"Map<IPersistentVector, Set<Class>>" :covariants nil
            )
        )
    )

    ;;;
     ; Current host interop uses reflection, which requires pre-existing classes.
     ; Work around this by:
     ; Generate a stub class that has the same interfaces and fields as the class we are generating.
     ; Use it as a type hint for this, and bind the simple name of the class to this stub (in resolve etc.)
     ; Unmunge the name (using a magic prefix) on any code gen for classes.
     ;;
    (defn #_"Class" NewInstanceExpr'compileStub [#_"String" superName, #_"NewInstanceExpr" ret, #_"String[]" interfaceNames, #_"ISeq" form]
        (let [#_"ClassWriter" cw (ClassWriter. ClassWriter/COMPUTE_MAXS) #_"ClassVisitor" cv cw]
            (.visit cv, Opcodes/V1_5, (| Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER), (str Compiler'COMPILE_STUB_PREFIX "/" (:internalName ret)), nil, superName, interfaceNames)

            ;; instance fields for closed-overs
            (loop-when-recur [#_"ISeq" s (vals (get *closes* (:uid ret)))] (some? s) [(next s)]
                (let [#_"LocalBinding" lb (first s)
                      #_"int" access (| Opcodes/ACC_PUBLIC (if (IopObject''isVolatile ret, lb) Opcodes/ACC_VOLATILE (if (IopObject''isMutable ret, lb) 0 Opcodes/ACC_FINAL)))]
                    (if (some? (LocalBinding''getPrimitiveType lb))
                        (.visitField cv, access, (:name lb), (.getDescriptor (Type/getType (LocalBinding''getPrimitiveType lb))), nil, nil)
                        ;; todo - when closed-overs are fields, use more specific types here and in ctor and emitLocal?
                        (.visitField cv, access, (:name lb), (.getDescriptor (Type/getType Object)), nil, nil)
                    )
                )
            )

            ;; ctor that takes closed-overs and does nothing
            (let [#_"Method" m (Method. "<init>", Type/VOID_TYPE, (IopObject''ctorTypes ret))
                  #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, nil, cv)]
                (.visitCode ctorgen)
                (.loadThis ctorgen)
                (.invokeConstructor ctorgen, (Type/getObjectType superName), (Method/getMethod "void <init>()"))
                (.returnValue ctorgen)
                (.endMethod ctorgen)
            )

            (when (pos? (:altCtorDrops ret))
                (let [#_"Type[]" ctorTypes (IopObject''ctorTypes ret)]

                    (let [#_"Type[]" altCtorTypes (make-array Type (- (alength ctorTypes) (:altCtorDrops ret)))
                          _ (dotimes [#_"int" i (alength altCtorTypes)]
                                (aset altCtorTypes i (aget ctorTypes i))
                            )
                          #_"Method" alt (Method. "<init>", Type/VOID_TYPE, altCtorTypes)
                          #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, alt, nil, nil, cv)]
                        (.visitCode ctorgen)
                        (.loadThis ctorgen)
                        (.loadArgs ctorgen)

                        (.visitInsn ctorgen, Opcodes/ACONST_NULL) ;; __meta
                        (.visitInsn ctorgen, Opcodes/ACONST_NULL) ;; __extmap
                        (.visitInsn ctorgen, Opcodes/ICONST_0) ;; __hash
                        (.visitInsn ctorgen, Opcodes/ICONST_0) ;; __hasheq

                        (.invokeConstructor ctorgen, (Type/getObjectType (str Compiler'COMPILE_STUB_PREFIX "/" (:internalName ret))), (Method. "<init>", Type/VOID_TYPE, ctorTypes))

                        (.returnValue ctorgen)
                        (.endMethod ctorgen)
                    )

                    ;; alt ctor no __hash, __hasheq
                    (let [#_"Type[]" altCtorTypes (make-array Type (- (alength ctorTypes) 2))
                          _ (dotimes [#_"int" i (alength altCtorTypes)]
                                (aset altCtorTypes i (aget ctorTypes i))
                            )
                          #_"Method" alt (Method. "<init>", Type/VOID_TYPE, altCtorTypes)
                          #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, alt, nil, nil, cv)]
                        (.visitCode ctorgen)
                        (.loadThis ctorgen)
                        (.loadArgs ctorgen)

                        (.visitInsn ctorgen, Opcodes/ICONST_0) ;; __hash
                        (.visitInsn ctorgen, Opcodes/ICONST_0) ;; __hasheq

                        (.invokeConstructor ctorgen, (Type/getObjectType (str Compiler'COMPILE_STUB_PREFIX "/" (:internalName ret))), (Method. "<init>", Type/VOID_TYPE, ctorTypes))

                        (.returnValue ctorgen)
                        (.endMethod ctorgen)
                    )
                )
            )

            ;; end of class
            (.visitEnd cv)

            (.defineClass *loader*, (str Compiler'COMPILE_STUB_PREFIX "." (:name ret)), (.toByteArray cw))
        )
    )

    (defn #_"String" NewInstanceExpr'slashname [#_"Class" c]
        (.replace (.getName c), \., \/)
    )

    (defn #_"String[]" NewInstanceExpr'interfaceNames [#_"IPersistentVector" interfaces]
        (let [#_"int" n (count interfaces)
              #_"String[]" inames (when (pos? n) (make-array String n))]
            (dotimes [#_"int" i n]
                (aset inames i (NewInstanceExpr'slashname (nth interfaces i)))
            )
            inames
        )
    )

    (extend-type NewInstanceExpr Expr
        (#_"Object" Expr'''eval [#_"NewInstanceExpr" this]
            (IopObject''doEval this)
        )

        (#_"void" Expr'''emit [#_"NewInstanceExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IopObject''doEmit this, context, objx, gen)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"NewInstanceExpr" this]
            true
        )

        #_memoize!
        (#_"Class" Expr'''getJavaClass [#_"NewInstanceExpr" this]
            (or (:compiledClass this)
                (if (some? (:tag this)) (Interop'tagToClass (:tag this)) IFn)
            )
        )
    )

    (extend-type NewInstanceExpr IopObject
        (#_"boolean" IopObject'''supportsMeta [#_"NewInstanceExpr" this]
            (not (IopObject''isDeftype this))
        )

        (#_"void" IopObject'''emitStatics [#_"NewInstanceExpr" this, #_"ClassVisitor" cv]
            (when (IopObject''isDeftype this)
                ;; getBasis()
                (let [#_"Method" meth (Method/getMethod "clojure.lang.IPersistentVector getBasis()")
                    #_"GeneratorAdapter" gen (GeneratorAdapter. (| Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), meth, nil, nil, cv)]
                    (IopObject''emitValue this, (:hintedFields this), gen)
                    (.returnValue gen)
                    (.endMethod gen)

                    (let-when [#_"int" n (count (:hintedFields this))] (< n (count (:fields this)))
                        ;; create(IPersistentMap)
                        (let [#_"String" className (.replace (:name this), \., \/)
                            #_"MethodVisitor" mv (.visitMethod cv, (| Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "create", (str "(Lclojure/lang/IPersistentMap;)L" className ";"), nil, nil)]
                            (.visitCode mv)

                            (loop-when-recur [#_"ISeq" s (seq (:hintedFields this)) #_"int" i 1] (some? s) [(next s) (inc i)]
                                (let [#_"String" bName (ßname (first s))
                                    #_"Class" k (Interop'tagClass (Compiler'tagOf (first s)))]
                                    (.visitVarInsn mv, Opcodes/ALOAD, 0)
                                    (.visitLdcInsn mv, bName)
                                    (.visitMethodInsn mv, Opcodes/INVOKESTATIC, "clojure/lang/Keyword", "intern", "(Ljava/lang/String;)Lclojure/lang/Keyword;")
                                    (.visitInsn mv, Opcodes/ACONST_NULL)
                                    (.visitMethodInsn mv, Opcodes/INVOKEINTERFACE, "clojure/lang/IPersistentMap", "valAt", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                                    (when (.isPrimitive k)
                                        (.visitTypeInsn mv, Opcodes/CHECKCAST, (.getInternalName (Type/getType (Compiler'boxClass k))))
                                    )
                                    (.visitVarInsn mv, Opcodes/ASTORE, i)
                                    (.visitVarInsn mv, Opcodes/ALOAD, 0)
                                    (.visitLdcInsn mv, bName)
                                    (.visitMethodInsn mv, Opcodes/INVOKESTATIC, "clojure/lang/Keyword", "intern", "(Ljava/lang/String;)Lclojure/lang/Keyword;")
                                    (.visitMethodInsn mv, Opcodes/INVOKEINTERFACE, "clojure/lang/IPersistentMap", "without", "(Ljava/lang/Object;)Lclojure/lang/IPersistentMap;")
                                    (.visitVarInsn mv, Opcodes/ASTORE, 0)
                                )
                            )

                            (.visitTypeInsn mv, Opcodes/NEW, className)
                            (.visitInsn mv, Opcodes/DUP)

                            (let [#_"Method" ctor (Method. "<init>", Type/VOID_TYPE, (IopObject''ctorTypes this))]
                                (dotimes [#_"int" i n]
                                    (.visitVarInsn mv, Opcodes/ALOAD, (inc i))
                                    (let-when [#_"Class" k (Interop'tagClass (Compiler'tagOf (nth (:hintedFields this) i)))] (.isPrimitive k)
                                        (.visitMethodInsn mv, Opcodes/INVOKEVIRTUAL, (.getInternalName (Type/getType (Compiler'boxClass k))), (str (.getName k) "Value"), (str "()" (.getDescriptor (Type/getType k))))
                                    )
                                )

                                (.visitInsn mv, Opcodes/ACONST_NULL) ;; __meta
                                (.visitVarInsn mv, Opcodes/ALOAD, 0) ;; __extmap
                                (.visitMethodInsn mv, Opcodes/INVOKESTATIC, "clojure/lang/RT", "seqOrElse", "(Ljava/lang/Object;)Ljava/lang/Object;")
                                (.visitInsn mv, Opcodes/ICONST_0) ;; __hash
                                (.visitInsn mv, Opcodes/ICONST_0) ;; __hasheq
                                (.visitMethodInsn mv, Opcodes/INVOKESPECIAL, className, "<init>", (.getDescriptor ctor))
                                (.visitInsn mv, Opcodes/ARETURN)
                                (.visitMaxs mv, (+ 4 n), (+ 1 n))
                                (.visitEnd mv)
                            )
                        )
                    )
                )
            )
            nil
        )

        (#_"void" IopObject'''emitMethods [#_"NewInstanceExpr" this, #_"ClassVisitor" cv]
            (loop-when-recur [#_"ISeq" s (seq (:methods this))] (some? s) [(next s)]
                (IopMethod'''emit (first s), this, cv)
            )
            ;; emit bridge methods
            (doseq [#_"Map$Entry<IPersistentVector, Set<Class>>" e (.entrySet (:covariants this))]
                (let [#_"java.lang.reflect.Method" m (get (:overrideables this) (key e))
                    #_"Class[]" params (.getParameterTypes m)
                    #_"Type[]" argTypes (make-array Type (alength params))
                    _ (dotimes [#_"int" i (alength params)]
                            (aset argTypes i (Type/getType (aget params i)))
                        )
                    #_"Method" target (Method. (.getName m), (Type/getType (.getReturnType m)), argTypes)]
                    (doseq [#_"Class" retType (val e)]
                        (let [#_"Method" meth (Method. (.getName m), (Type/getType retType), argTypes)
                            #_"GeneratorAdapter" gen (GeneratorAdapter. (| Opcodes/ACC_PUBLIC Opcodes/ACC_BRIDGE), meth, nil, Compiler'EXCEPTION_TYPES, cv)]
                            (.visitCode gen)
                            (.loadThis gen)
                            (.loadArgs gen)
                            (.invokeInterface gen, (Type/getType (.getDeclaringClass m)), target)
                            (.returnValue gen)
                            (.endMethod gen)
                        )
                    )
                )
            )
            nil
        )
    )

    (defn #_"IPersistentVector" NewInstanceExpr'msig [#_"java.lang.reflect.Method" m]
        [(.getName m) (seq (.getParameterTypes m)) (.getReturnType m)]
    )

    (defn #_"void" NewInstanceExpr'considerMethod [#_"java.lang.reflect.Method" m, #_"Map" mm]
        (let [#_"IPersistentVector" mk (NewInstanceExpr'msig m) #_"int" mods (.getModifiers m)]
            (when (not (or (contains? mm mk) (not (or (Modifier/isPublic mods) (Modifier/isProtected mods))) (Modifier/isStatic mods) (Modifier/isFinal mods)))
                (.put mm, mk, m)
            )
        )
        nil
    )

    (defn #_"[Map Map]" NewInstanceExpr'gatherMethods [#_"Class" sc, #_"ISeq" ifaces]
        (let [#_"Map" allm (HashMap.)
              game-
                (fn #_"void" [#_"Class" c, #_"Map" mm]
                    (loop-when-recur c (some? c) (.getSuperclass c)
                        (doseq [#_"java.lang.reflect.Method" m (.getDeclaredMethods c)]
                            (NewInstanceExpr'considerMethod m, mm)
                        )
                        (doseq [#_"java.lang.reflect.Method" m (.getMethods c)]
                            (NewInstanceExpr'considerMethod m, mm)
                        )
                    )
                    nil
                )
              _ (game- sc allm)
              _ (loop-when-recur ifaces (some? ifaces) (next ifaces)
                    (game- (first ifaces) allm)
                )
              #_"Map<IPersistentVector, java.lang.reflect.Method>" methods (HashMap.)
              #_"Map<IPersistentVector, Set<Class>>" covariants (HashMap.)]
            (loop-when-recur [#_"Iterator" it (.iterator (.entrySet allm))] (.hasNext it) [it]
                (let [#_"Map$Entry" e (.next it) #_"IPersistentVector" mk (pop (key e)) #_"java.lang.reflect.Method" m (val e)]
                    (if (contains? methods mk) ;; covariant return
                        (let [#_"Set<Class>" cvs
                                (or (get covariants mk)
                                    (let [cvs (HashSet.)]
                                        (.put covariants, mk, cvs)
                                        cvs
                                    )
                                )
                              #_"Class" tk (.getReturnType (get methods mk)) #_"Class" t (.getReturnType m)]
                            (when (.isAssignableFrom tk, t) => (.add cvs, t)
                                (.add cvs, tk)
                                (.put methods, mk, m)
                            )
                        )
                        (.put methods, mk, m)
                    )
                )
            )
            [methods covariants]
        )
    )

    (defn #_"IopObject" NewInstanceExpr'build [#_"IPersistentVector" interfaceSyms, #_"IPersistentVector" fieldSyms, #_"Symbol" thisSym, #_"String" tagName, #_"Symbol" className, #_"Symbol" typeTag, #_"ISeq" methodForms, #_"ISeq" form, #_"IPersistentMap" opts]
        (let [#_"String" name (.toString className) #_"String" name' (.replace name, \., \/)
              #_"NewInstanceExpr" nie
                (-> (NewInstanceExpr'new nil)
                    (assoc :name name :internalName name' :objType (Type/getObjectType name') :opts opts)
                )
              nie (if (some? thisSym) (assoc nie :thisName (ßname thisSym)) nie)
              nie
                (when (some? fieldSyms) => nie
                    (let [#_"Object[]" a (make-array Object (* 2 (count fieldSyms)))
                          #_"IPersistentMap" fmap
                            (loop-when [fmap {} #_"int" i 0] (< i (count fieldSyms)) => fmap
                                (let [#_"Symbol" sym (nth fieldSyms i)
                                      #_"LocalBinding" lb (LocalBinding'new -1, sym, nil, (MethodParamExpr'new (Interop'tagClass (Compiler'tagOf sym))), false)]
                                    (aset a (* i 2) (:uid lb))
                                    (aset a (inc (* i 2)) lb)
                                    (recur (assoc fmap sym lb) (inc i))
                                )
                            )
                          ;; todo - inject __meta et al into closes - when?
                          ;; use array map to preserve ctor order
                          _ (update! *closes* assoc (:uid nie) (PersistentArrayMap. a))
                          nie (assoc nie :fields fmap)]
                        (loop-when-recur [nie nie #_"int" i (dec (count fieldSyms))]
                                         (and (<= 0 i) (any = (ßname (nth fieldSyms i)) "__meta" "__extmap" "__hash" "__hasheq"))
                                         [(update nie :altCtorDrops inc) (dec i)]
                                      => nie
                        )
                    )
                )
              #_"PersistentVector" ifaces
                (loop-when [ifaces [] #_"ISeq" s (seq interfaceSyms)] (some? s) => ifaces
                    (let [#_"Class" c (Compiler'resolve (first s))]
                        (when (.isInterface c) => (throw (IllegalArgumentException. (str "only interfaces are supported, had: " (.getName c))))
                            (recur (conj ifaces c) (next s))
                        )
                    )
                )
              #_"Class" super Object
              [#_"Map" overrideables #_"Map" covariants] (NewInstanceExpr'gatherMethods super, (seq ifaces))
              nie (assoc nie :overrideables overrideables :covariants covariants)
              #_"String[]" inames (NewInstanceExpr'interfaceNames ifaces)
              #_"Class" stub (NewInstanceExpr'compileStub (NewInstanceExpr'slashname super), nie, inames, form)
              #_"Symbol" thistag (symbol (.getName stub))
              nie
                (binding [*constants*          []
                          *constant-ids*       (IdentityHashMap.)
                          *keywords*           {}
                          *vars*               {}
                          *keyword-callsites*  []
                          *protocol-callsites* []
                          *no-recur*           false]
                    (try
                        (let [nie
                                (when (IopObject''isDeftype nie) => nie
                                    (push-thread-bindings
                                        (hash-map
                                            #'*method*             nil
                                            #'*local-env*          (:fields nie)
                                            #'*compile-stub-sym*   (symbol tagName)
                                            #'*compile-stub-class* stub
                                        )
                                    )
                                    (assoc nie :hintedFields (subvec fieldSyms 0 (- (count fieldSyms) (:altCtorDrops nie))))
                                )
                              ;; now (methodname [args] body)*
                              nie (assoc nie :line *line*)
                              #_"IPersistentCollection" methods
                                (loop-when [methods nil #_"ISeq" s methodForms] (some? s) => methods
                                    (let [#_"NewInstanceMethod" m (NewInstanceMethod'parse nie, (first s), thistag, overrideables)]
                                        (recur (conj methods m) (next s))
                                    )
                                )]
                            (assoc nie
                                :methods methods
                                :keywords *keywords*
                                :vars *vars*
                                :constants *constants*
                                :keywordCallsites *keyword-callsites*
                                :protocolCallsites *protocol-callsites*
                            )
                        )
                        (finally
                            (when (IopObject''isDeftype nie)
                                (pop-thread-bindings)
                            )
                        )
                    )
                )]
            (IopObject''compile nie, (NewInstanceExpr'slashname super), inames, false)
        )
    )
)

(class-ns ReifyParser
    (defn #_"IParser" ReifyParser'new []
        (reify IParser
            ;; (reify this-name? [interfaces] (method-name [args] body)*)
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"ISeq" s form                                                  s (next s)
                      #_"IPersistentVector" ifaces (conj (first s) 'clojure.lang.IObj) s (next s)
                      #_"String" classname
                        (let [#_"IopMethod" owner *method*
                              #_"String" basename (if (some? owner) (IopObject'trimGenID (:name (:objx owner))) (Compiler'munge (ßname (ßname *ns*))))]
                            (str basename "$" "reify__" (RT/nextID))
                        )
                      #_"IopObject" nie (NewInstanceExpr'build ifaces, nil, nil, classname, (symbol classname), nil, s, form, nil)]
                    (when (and (instance? IObj form) (some? (meta form))) => nie
                        (MetaExpr'new nie, (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), (meta form)))
                    )
                )
            )
        )
    )
)

(class-ns DeftypeParser
    (defn #_"IParser" DeftypeParser'new []
        (reify IParser
            ;; (deftype* tagname classname [fields] :implements [interfaces] :tag tagname methods*)
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"ISeq" s form                         s (next s)
                      #_"String" tagname (.getName (first s)) s (next s)
                      #_"Symbol" classname (first s)          s (next s)
                      #_"IPersistentVector" fields (first s)  s (next s)
                      [#_"IPersistentMap" opts s]
                        (loop-when-recur [opts {} s s]
                                         (and (some? s) (keyword? (first s)))
                                         [(assoc opts (first s) (second s)) (next (next s))]
                                      => [opts s]
                        )]
                    (NewInstanceExpr'build (get opts :implements []), fields, nil, tagname, classname, (get opts :tag), s, form, opts)
                )
            )
        )
    )
)

(class-ns CaseExpr
    ;; (case* expr shift mask default map<minhash, [test then]> table-type test-type skip-check?)
    (defn #_"CaseExpr" CaseExpr'new [#_"int" line, #_"LocalBindingExpr" expr, #_"int" shift, #_"int" mask, #_"int" low, #_"int" high, #_"Expr" defaultExpr, #_"SortedMap<Integer, Expr>" tests, #_"HashMap<Integer, Expr>" thens, #_"Keyword" switchType, #_"Keyword" testType, #_"Set<Integer>" skipCheck]
        (when-not (any = switchType :compact :sparse)
            (throw (IllegalArgumentException. (str "Unexpected switch type: " switchType)))
        )
        (when-not (any = testType :int :hash-equiv :hash-identity)
            (throw (IllegalArgumentException. (str "Unexpected test type: " testType)))
        )
        (when (and (pos? (count skipCheck)) *warn-on-reflection*)
            (.println *err*, (str "Performance warning, line " line " - hash collision of some case test constants; if selected, those entries will be tested sequentially."))
        )
        (merge (CaseExpr.)
            (hash-map
                #_"LocalBindingExpr" :expr expr
                #_"int" :shift shift
                #_"int" :mask mask
                #_"int" :low low
                #_"int" :high high
                #_"Expr" :defaultExpr defaultExpr
                #_"SortedMap<Integer, Expr>" :tests tests
                #_"HashMap<Integer, Expr>" :thens thens
                #_"Keyword" :switchType switchType
                #_"Keyword" :testType testType
                #_"Set<Integer>" :skipCheck skipCheck
                #_"Class" :returnType (Compiler'maybeJavaClass (conj (vec (.values thens)) defaultExpr))
                #_"int" :line line
            )
        )
    )

    #_method
    (defn- #_"boolean" CaseExpr''isShiftMasked [#_"CaseExpr" this]
        (not= (:mask this) 0)
    )

    #_method
    (defn- #_"void" CaseExpr''emitShiftMask [#_"CaseExpr" this, #_"GeneratorAdapter" gen]
        (when (CaseExpr''isShiftMasked this)
            (.push gen, (:shift this))
            (.visitInsn gen, Opcodes/ISHR)
            (.push gen, (:mask this))
            (.visitInsn gen, Opcodes/IAND)
        )
        nil
    )

    #_method
    (defn- #_"void" CaseExpr''emitExprForInts [#_"CaseExpr" this, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Type" exprType, #_"Label" defaultLabel]
        (cond (nil? exprType)
            (do
                (when *warn-on-reflection*
                    (.println *err*, (str "Performance warning, line " (:line this) " - case has int tests, but tested expression is not primitive."))
                )
                (Expr'''emit (:expr this), :Context'EXPRESSION, objx, gen)
                (.instanceOf gen, (Type/getType Number))
                (.ifZCmp gen, GeneratorAdapter/EQ, defaultLabel)
                (Expr'''emit (:expr this), :Context'EXPRESSION, objx, gen)
                (.checkCast gen, (Type/getType Number))
                (.invokeVirtual gen, (Type/getType Number), (Method/getMethod "int intValue()"))
                (CaseExpr''emitShiftMask this, gen)
            )
            (or (= exprType Type/LONG_TYPE) (= exprType Type/INT_TYPE) (= exprType Type/SHORT_TYPE) (= exprType Type/BYTE_TYPE))
            (do
                (MaybePrimitive'''emitUnboxed (:expr this), :Context'EXPRESSION, objx, gen)
                (.cast gen, exprType, Type/INT_TYPE)
                (CaseExpr''emitShiftMask this, gen)
            )
            :else
            (do
                (.goTo gen, defaultLabel)
            )
        )
        nil
    )

    (defn- #_"void" CaseExpr'emitExpr [#_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Expr" expr, #_"boolean" emitUnboxed]
        (if (and emitUnboxed (satisfies? MaybePrimitive expr))
            (MaybePrimitive'''emitUnboxed expr, :Context'EXPRESSION, objx, gen)
            (Expr'''emit expr, :Context'EXPRESSION, objx, gen)
        )
        nil
    )

    #_method
    (defn- #_"void" CaseExpr''emitThenForInts [#_"CaseExpr" this, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Type" exprType, #_"Expr" test, #_"Expr" then, #_"Label" defaultLabel, #_"boolean" emitUnboxed]
        (cond (nil? exprType)
            (do
                (Expr'''emit (:expr this), :Context'EXPRESSION, objx, gen)
                (Expr'''emit test, :Context'EXPRESSION, objx, gen)
                (.invokeStatic gen, (Type/getType Util), (Method/getMethod "boolean equiv(Object, Object)"))
                (.ifZCmp gen, GeneratorAdapter/EQ, defaultLabel)
                (CaseExpr'emitExpr objx, gen, then, emitUnboxed)
            )
            (= exprType Type/LONG_TYPE)
            (do
                (MaybePrimitive'''emitUnboxed test, :Context'EXPRESSION, objx, gen)
                (MaybePrimitive'''emitUnboxed (:expr this), :Context'EXPRESSION, objx, gen)
                (.ifCmp gen, Type/LONG_TYPE, GeneratorAdapter/NE, defaultLabel)
                (CaseExpr'emitExpr objx, gen, then, emitUnboxed)
            )
            (or (= exprType Type/INT_TYPE) (= exprType Type/SHORT_TYPE) (= exprType Type/BYTE_TYPE))
            (do
                (when (CaseExpr''isShiftMasked this)
                    (MaybePrimitive'''emitUnboxed test, :Context'EXPRESSION, objx, gen)
                    (MaybePrimitive'''emitUnboxed (:expr this), :Context'EXPRESSION, objx, gen)
                    (.cast gen, exprType, Type/LONG_TYPE)
                    (.ifCmp gen, Type/LONG_TYPE, GeneratorAdapter/NE, defaultLabel)
                )
                (CaseExpr'emitExpr objx, gen, then, emitUnboxed)
            )
            :else
            (do
                (.goTo gen, defaultLabel)
            )
        )
        nil
    )

    #_method
    (defn- #_"void" CaseExpr''emitExprForHashes [#_"CaseExpr" this, #_"IopObject" objx, #_"GeneratorAdapter" gen]
        (Expr'''emit (:expr this), :Context'EXPRESSION, objx, gen)
        (.invokeStatic gen, (Type/getType Util), (Method/getMethod "int hash(Object)"))
        (CaseExpr''emitShiftMask this, gen)
        nil
    )

    #_method
    (defn- #_"void" CaseExpr''emitThenForHashes [#_"CaseExpr" this, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Expr" test, #_"Expr" then, #_"Label" defaultLabel, #_"boolean" emitUnboxed]
        (Expr'''emit (:expr this), :Context'EXPRESSION, objx, gen)
        (Expr'''emit test, :Context'EXPRESSION, objx, gen)
        (if (= (:testType this) :hash-identity)
            (do
                (.visitJumpInsn gen, Opcodes/IF_ACMPNE, defaultLabel)
            )
            (do
                (.invokeStatic gen, (Type/getType Util), (Method/getMethod "boolean equiv(Object, Object)"))
                (.ifZCmp gen, GeneratorAdapter/EQ, defaultLabel)
            )
        )
        (CaseExpr'emitExpr objx, gen, then, emitUnboxed)
        nil
    )

    #_method
    (defn- #_"void" CaseExpr''doEmit [#_"CaseExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"boolean" emitUnboxed]
        (let [#_"Label" defaultLabel (.newLabel gen) #_"Label" endLabel (.newLabel gen)
              #_"SortedMap<Integer, Label>" labels (TreeMap.) _ (doseq [#_"Integer" i (.keySet (:tests this))] (.put labels, i, (.newLabel gen)))]
            (.visitLineNumber gen, (:line this), (.mark gen))
            (let [#_"Class" primExprClass (Compiler'maybePrimitiveType (:expr this))
                  #_"Type" primExprType (when (some? primExprClass) (Type/getType primExprClass))]
                (if (= (:testType this) :int)
                    (CaseExpr''emitExprForInts this, objx, gen, primExprType, defaultLabel)
                    (CaseExpr''emitExprForHashes this, objx, gen)
                )
                (if (= (:switchType this) :sparse)
                    (let [#_"Label[]" la (make-array Label (count labels)) la (.toArray (.values labels), la)]
                        (.visitLookupSwitchInsn gen, defaultLabel, (Numbers/int_array (.keySet (:tests this))), la)
                    )
                    (let [#_"Label[]" la (make-array Label (inc (- (:high this) (:low this))))]
                        (loop-when-recur [#_"int" i (:low this)] (<= i (:high this)) [(inc i)]
                            (aset la (- i (:low this)) (if (contains? labels i) (get labels i) defaultLabel))
                        )
                        (.visitTableSwitchInsn gen, (:low this), (:high this), defaultLabel, la)
                    )
                )
                (doseq [#_"Integer" i (.keySet labels)]
                    (.mark gen, (get labels i))
                    (cond
                        (= (:testType this) :int)
                            (CaseExpr''emitThenForInts this, objx, gen, primExprType, (get (:tests this) i), (get (:thens this) i), defaultLabel, emitUnboxed)
                        (= (contains? (:skipCheck this) i) true)
                            (CaseExpr'emitExpr objx, gen, (get (:thens this) i), emitUnboxed)
                        :else
                            (CaseExpr''emitThenForHashes this, objx, gen, (get (:tests this) i), (get (:thens this) i), defaultLabel, emitUnboxed)
                    )
                    (.goTo gen, endLabel)
                )
                (.mark gen, defaultLabel)
                (CaseExpr'emitExpr objx, gen, (:defaultExpr this), emitUnboxed)
                (.mark gen, endLabel)
                (when (= context :Context'STATEMENT)
                    (.pop gen)
                )
            )
        )
        nil
    )

    (extend-type CaseExpr Expr
        (#_"Object" Expr'''eval [#_"CaseExpr" this]
            (throw (UnsupportedOperationException. "Can't eval case"))
        )

        (#_"void" Expr'''emit [#_"CaseExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (CaseExpr''doEmit this, context, objx, gen, false)
            nil
        )

        (#_"boolean" Expr'''hasJavaClass [#_"CaseExpr" this]
            (some? (:returnType this))
        )

        (#_"Class" Expr'''getJavaClass [#_"CaseExpr" this]
            (:returnType this)
        )
    )

    (extend-type CaseExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"CaseExpr" this]
            (Reflector'isPrimitive (:returnType this))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"CaseExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (CaseExpr''doEmit this, context, objx, gen, true)
            nil
        )
    )
)

(class-ns CaseParser
    (defn #_"IParser" CaseParser'new []
        (reify IParser
            ;; (case* expr shift mask default map<minhash, [test then]> table-type test-type skip-check?)
            ;; prepared by case macro and presumed correct
            ;; case macro binds actual expr in let so expr is always a local,
            ;; no need to worry about multiple evaluation
            #_override
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (if (= context :Context'EVAL)
                    (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                    (let [#_"IPersistentVector" args (vec (next form))
                          #_"Object" exprForm (nth args 0)
                          #_"int" shift (.intValue (nth args 1))
                          #_"int" mask (.intValue (nth args 2))
                          #_"Object" defaultForm (nth args 3)
                          #_"Map" caseMap (nth args 4)
                          #_"Keyword" switchType (nth args 5)
                          #_"Keyword" testType (nth args 6)
                          #_"Set" skipCheck (when (< 7 (count args)) (nth args 7))
                          #_"ISeq" keys (keys caseMap)
                          #_"int" low (.intValue (first keys))
                          #_"int" high (.intValue (nth keys (dec (count keys))))
                          #_"LocalBindingExpr" testExpr (Compiler'analyze :Context'EXPRESSION, exprForm)
                          #_"SortedMap<Integer, Expr>" tests (TreeMap.)
                          #_"HashMap<Integer, Expr>" thens (HashMap.)
                          _ (doseq [#_"Map$Entry" e (.entrySet caseMap)]
                                (let [#_"Integer" minhash (.intValue (key e)) #_"Object" pair (val e) ;; [test-val then-expr]
                                      #_"Expr" test
                                        (if (= testType :int)
                                            (NumberExpr'parse (.intValue (first pair)))
                                            (ConstantExpr'new (first pair))
                                        )
                                      #_"Expr" then (Compiler'analyze context, (second pair))]
                                    (.put tests, minhash, test)
                                    (.put thens, minhash, then)
                                )
                            )
                          #_"Expr" defaultExpr (Compiler'analyze context, (nth args 3))]
                        (CaseExpr'new *line*, testExpr, shift, mask, low, high, defaultExpr, tests, thens, switchType, testType, skipCheck)
                    )
                )
            )
        )
    )
)

(class-ns Compiler
    (def #_"IPersistentMap" Compiler'specials
        (hash-map
            'def                  (DefParser'new)
            'loop*                (LetParser'new)
            'recur                (RecurParser'new)
            'if                   (IfParser'new)
            'case*                (CaseParser'new)
            'let*                 (LetParser'new)
            'letfn*               (LetFnParser'new)
            'do                   (BodyParser'new)
            'fn*                  nil
            'quote                (ConstantParser'new)
            'var                  (TheVarParser'new)
            'clojure.core/import* (ImportParser'new)
            '.                    (HostParser'new)
            'set!                 (AssignParser'new)
            'deftype*             (DeftypeParser'new)
            'reify*               (ReifyParser'new)
            'try                  (TryParser'new)
            'throw                (ThrowParser'new)
            'monitor-enter        (MonitorEnterParser'new)
            'monitor-exit         (MonitorExitParser'new)
            'catch                nil
            'finally              nil
            'new                  (NewParser'new)
            '&                    nil
        )
    )

    (defn #_"Object" Compiler'macroexpand1 [#_"Object" form]
        (when (instance? ISeq form) => form
            (let-when-not [#_"Object" op (first form)] (Compiler'isSpecial op) => form
                ;; macro expansion
                (let [#_"Var" v (Compiler'isMacro op)]
                    (if (some? v)
                        (try
                            (.applyTo v, (cons form (cons *local-env* (next form))))
                            (§ catch ArityException e
                                ;; hide the 2 extra params for a macro
                                (throw (ArityException'new (- (:actual e) 2), (:name e)))
                            )
                        )
                        (when (symbol? op) => form
                            (let [#_"String" name (ßname op)]
                                ;; (.substring s 2 5) => (. s substring 2 5)
                                (cond
                                    (= (.charAt name, 0) \.)
                                        (when (< 1 (count form)) => (throw (IllegalArgumentException. "Malformed member expression, expecting (.member target ...)"))
                                            (let [#_"Object" target (second form)
                                                  target
                                                    (when (some? (Interop'maybeClass target, false)) => target
                                                        (with-meta (list 'clojure.core/identity target) { :tag 'Class })
                                                    )]
                                                (Compiler'preserveTag form, (list* '. target (symbol (.substring name, 1)) (next (next form))))
                                            )
                                        )
                                    (Compiler'namesStaticMember op)
                                        (let-when [#_"Symbol" target (symbol (ßns op))] (some? (Interop'maybeClass target, false)) => form
                                            (Compiler'preserveTag form, (list* '. target (symbol name) (next form)))
                                        )
                                    :else
                                        ;; (s.substring ...) => (. s substring ...)
                                        ;; (package.class.name ...) => (. package.class name ...)
                                        ;; (StringBuilder. ...) => (new StringBuilder ...)
                                        (let-when [#_"int" i (.lastIndexOf name, (int \.))] (= i (dec (.length name))) => form
                                            (list* 'new (symbol (.substring name, 0, i)) (next form))
                                        )
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    (defn #_"Object" Compiler'macroexpand [#_"Object" form]
        (let [#_"Object" f (Compiler'macroexpand1 form)]
            (if (= f form) form (recur f))
        )
    )

    (defn- #_"Expr" Compiler'analyzeSymbol [#_"Symbol" sym]
        (let [#_"Symbol" tag (Compiler'tagOf sym)]
            (or
                (cond
                    (nil? (ßns sym)) ;; ns-qualified syms are always Vars
                        (when-let [#_"LocalBinding" b (Compiler'referenceLocal sym)]
                            (LocalBindingExpr'new b, tag)
                        )
                    (nil? (Compiler'namespaceFor sym))
                        (when-let [#_"Class" c (Interop'maybeClass (symbol (ßns sym)), false)]
                            (when (some? (Reflector'getField c, (ßname sym), true)) => (throw (RuntimeException. (str "Unable to find static field: " (ßname sym) " in " c)))
                                (StaticFieldExpr'new *line*, c, (ßname sym), tag)
                            )
                        )
                )
                (let [#_"Object" o (Compiler'resolve sym)]
                    (cond
                        (var? o)
                            (when (nil? (Compiler'isMacro o)) => (throw (RuntimeException. (str "Can't take value of a macro: " o)))
                                (Compiler'registerVar o)
                                (VarExpr'new o, tag)
                            )
                        (instance? Class o)
                            (ConstantExpr'new o)
                        (symbol? o)
                            (UnresolvedVarExpr'new o)
                        :else
                            (throw (RuntimeException. (str "Unable to resolve symbol: " sym " in this context")))
                    )
                )
            )
        )
    )

    (defn- #_"KeywordExpr" Compiler'registerKeyword [#_"Keyword" k]
        (when (bound? #'*keywords*)
            (let-when [#_"IPersistentMap" m *keywords*] (nil? (get m k))
                (set! *keywords* (assoc m k (Compiler'registerConstant k)))
            )
        )
        (KeywordExpr'new k)
    )

    (defn- #_"Expr" Compiler'analyzeSeq [#_"Context" context, #_"ISeq" form, #_"String" name]
        (let [#_"IPersistentMap" meta (meta form)]
            (binding [*line* (if (contains? meta :line) (get meta :line) *line*)]
                (let-when [#_"Object" me (Compiler'macroexpand1 form)] (= me form) => (Compiler'analyze context, me, name)
                    (let-when [#_"Object" op (first form)] (some? op) => (throw (IllegalArgumentException. (str "Can't call nil, form: " form)))
                        (let [#_"IFn" inline (Compiler'isInline op, (count (next form)))]
                            (cond
                                (some? inline)
                                    (Compiler'analyze context, (Compiler'preserveTag form, (.applyTo inline, (next form))))
                                (= op 'fn*)
                                    (FnExpr'parse context, form, name)
                                :else
                                    (let [#_"IParser" p (get Compiler'specials op)]
                                        (if (some? p)
                                            (IParser'''parse p, context, form)
                                            (InvokeExpr'parse context, form)
                                        )
                                    )
                            )
                        )
                    )
                )
            )
        )
    )

    (defn #_"Expr" Compiler'analyze
        ([#_"Context" context, #_"Object" form] (Compiler'analyze context, form, nil))
        ([#_"Context" context, #_"Object" form, #_"String" name]
            (let [form
                    (when (instance? LazySeq form) => form
                        (with-meta (or (seq form) ()) (meta form))
                    )]
                (case form
                    nil                                    Compiler'NIL_EXPR
                    true                                   Compiler'TRUE_EXPR
                    false                                  Compiler'FALSE_EXPR
                    (cond
                        (symbol? form)                     (Compiler'analyzeSymbol form)
                        (keyword? form)                    (Compiler'registerKeyword form)
                        (number? form)                     (NumberExpr'parse form)
                        (string? form)                     (StringExpr'new (.intern form))
                        (and (instance? IPersistentCollection form) (not (instance? IType form)) (zero? (count form)))
                            (let-when [#_"Expr" e (EmptyExpr'new form)] (some? (meta form)) => e
                                (MetaExpr'new e, (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), (meta form)))
                            )
                        (instance? ISeq form)              (Compiler'analyzeSeq context, form, name)
                        (instance? IPersistentVector form) (VectorExpr'parse context, form)
                        (instance? IPersistentMap form)    (MapExpr'parse context, form)
                        (instance? IPersistentSet form)    (SetExpr'parse context, form)
                        :else                              (ConstantExpr'new form)
                    )
                )
            )
        )
    )

    (defn #_"Object" Compiler'eval [#_"Object" form]
        (let [#_"IPersistentMap" meta (meta form)]
            (binding [*loader* (RT/makeClassLoader), *line* (if (contains? meta :line) (get meta :line) *line*)]
                (let [form (Compiler'macroexpand form)]
                    (cond
                        (and (instance? ISeq form) (= (first form) 'do))
                            (loop-when-recur [#_"ISeq" s (next form)] (some? (next s)) [(next s)] => (Compiler'eval (first s))
                                (Compiler'eval (first s))
                            )
                        (or (instance? IType form) (and (instance? IPersistentCollection form) (not (and (symbol? (first form)) (.startsWith (ßname (first form)), "def")))))
                            (let [#_"IopObject" fexpr (Compiler'analyze :Context'EXPRESSION, (list 'fn* [] form), (str "eval" (RT/nextID)))]
                                (.invoke (Expr'''eval fexpr))
                            )
                        :else
                            (let [#_"Expr" expr (Compiler'analyze :Context'EVAL, form)]
                                (Expr'''eval expr)
                            )
                    )
                )
            )
        )
    )

    (defn- #_"void" Compiler'consumeWhitespaces [#_"LineNumberingPushbackReader" r]
        (loop-when-recur [#_"int" ch (LispReader/read1 r)] (LispReader'isWhitespace ch) [(LispReader/read1 r)] => (LispReader'unread r, ch))
        nil
    )

    (defn #_"Object" Compiler'load [#_"Reader" reader]
        (let [#_"LineNumberingPushbackReader" r (if (instance? LineNumberingPushbackReader reader) reader (LineNumberingPushbackReader. reader))
              #_"Object" EOF (Object.)]
            (binding [*ns* *ns*, *warn-on-reflection* *warn-on-reflection*, *line* 0]
                (loop [#_"Object" val nil]
                    (Compiler'consumeWhitespaces r)
                    (let-when [#_"Object" form (LispReader/read r, false, EOF, false, (§ obsolete nil))] (not= form EOF) => val
                        (recur
                            (binding [*last-unique-id*     -1
                                        *closes*             {}
                                        *no-recur*           false
                                        *in-catch-finally*   false
                                        *in-return-context*  false
                                        *compile-stub-sym*   nil
                                        *compile-stub-class* nil]
                                (Compiler'eval form)
                            )
                        )
                    )
                )
            )
        )
    )
)
)
