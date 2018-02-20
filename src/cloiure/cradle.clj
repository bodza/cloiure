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

(defmacro interface! [name [& sups] & sigs]
    (let [tag- #(or (:tag (meta %)) Object)
          sig- (fn [[name [this & args]]] [name (vec (map tag- args)) (tag- name) (map meta args)])
          cname (with-meta (symbol (str (namespace-munge *ns*) "." name)) (meta name))]
        `(do
            (gen-interface :name ~cname :extends ~(vec (map resolve sups)) :methods ~(vec (map sig- sigs)))
            (import ~cname)
        )
    )
)
(defmacro class! [& _] `(interface! ~@_))

#_(ns cloiure.cradle
    (:refer-clojure :exclude [when when-not])
    (:use [cloiure slang]))

(import
    [java.io Reader]
  #_[java.lang Character Class Exception IllegalArgumentException IllegalStateException Integer Number Object RuntimeException String StringBuilder Throwable UnsupportedOperationException]
    [java.lang.reflect Constructor Field #_Method Modifier]
    [java.util Arrays Comparator HashMap HashSet IdentityHashMap Iterator Map Map$Entry Set TreeMap]
    [java.util.regex Matcher Pattern]
    [clojure.lang AFn AFunction APersistentMap APersistentSet APersistentVector ArraySeq DynamicClassLoader PersistentList$EmptyList IFn ILookup ILookupSite ILookupThunk IMapEntry IMeta IObj IPersistentCollection IPersistentList IPersistentMap IPersistentSet IPersistentVector ISeq IType Keyword KeywordLookupSite LazySeq LineNumberingPushbackReader LispReader Namespace Numbers PersistentArrayMap PersistentHashSet PersistentList PersistentVector RestFn RT Symbol Tuple Util Var]
    [cloiure.asm ClassVisitor ClassWriter Label MethodVisitor Opcodes Type]
    [cloiure.asm.commons GeneratorAdapter Method]
)

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

(java-ns cloiure.lang.Compiler
    (interface! Expr []
        #_abstract
        (#_"Object" eval [#_"Expr" this])
        #_abstract
        (#_"void" emit [#_"Expr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen])
        #_abstract
        (#_"boolean" hasJavaClass [#_"Expr" this])
        #_abstract
        (#_"Class" getJavaClass [#_"Expr" this])
    )

    (interface! IParser []
        #_abstract
        (#_"Expr" parse [#_"IParser" this, #_"Context" context, #_"ISeq" form])
    )

    (interface! AssignableExpr []
        #_abstract
        (#_"Object" evalAssign [#_"AssignableExpr" this, #_"Expr" val])
        #_abstract
        (#_"void" emitAssign [#_"AssignableExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Expr" val])
    )

    (interface! MaybePrimitiveExpr [Expr]
        #_abstract
        (#_"boolean" canEmitPrimitive [#_"MaybePrimitiveExpr" this])
        #_abstract
        (#_"void" emitUnboxed [#_"MaybePrimitiveExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen])
    )
)

(java-ns cloiure.lang.Reflector
    #_stateless
    (class! Reflector [])
)

(java-ns cloiure.lang.Compiler
    #_stateless
    (class! Recur [])
    #_abstract
    (class! UntypedExpr [Expr])
    (class! MonitorEnterExpr [#_"UntypedExpr"])
    (class! MonitorExitExpr [#_"UntypedExpr"])
    (class! AssignExpr [Expr])
    (class! ImportExpr [Expr])
    (class! EmptyExpr [Expr])
    #_abstract
    (class! LiteralExpr [Expr]
        #_abstract
        (#_"Object" literal [#_"LiteralExpr" this])
    )
    (class! NilExpr [#_"LiteralExpr"])
    (class! BooleanExpr [#_"LiteralExpr"])
    (class! ConstantExpr [#_"LiteralExpr"])
    (class! NumberExpr [#_"LiteralExpr" MaybePrimitiveExpr])
    (class! StringExpr [#_"LiteralExpr"])
    (class! KeywordExpr [#_"LiteralExpr"])
    #_abstract
    (class! HostExpr [Expr MaybePrimitiveExpr])
    #_abstract
    (class! FieldExpr [#_"HostExpr"])
    (class! InstanceFieldExpr [#_"FieldExpr" AssignableExpr])
    (class! StaticFieldExpr [#_"FieldExpr" AssignableExpr])
    #_abstract
    (class! MethodExpr [#_"HostExpr"])
    (class! InstanceMethodExpr [#_"MethodExpr"])
    (class! StaticMethodExpr [#_"MethodExpr"])
    (class! UnresolvedVarExpr [Expr])
    (class! VarExpr [Expr AssignableExpr])
    (class! TheVarExpr [Expr])
    (class! BodyExpr [Expr MaybePrimitiveExpr])
    (class! CatchClause [])
    (class! TryExpr [Expr])
    (class! ThrowExpr [#_"UntypedExpr"])
    (class! NewExpr [Expr])
    (class! MetaExpr [Expr])
    (class! IfExpr [Expr MaybePrimitiveExpr])
    (class! ListExpr [Expr])
    (class! MapExpr [Expr])
    (class! SetExpr [Expr])
    (class! VectorExpr [Expr])
    (class! KeywordInvokeExpr [Expr])
    (class! InstanceOfExpr [Expr MaybePrimitiveExpr])
    (class! StaticInvokeExpr [Expr MaybePrimitiveExpr])
    (class! InvokeExpr [Expr])
    (class! LocalBinding [])
    (class! PathNode [])
    (class! LocalBindingExpr [Expr MaybePrimitiveExpr AssignableExpr])
    #_abstract
    (class! ObjMethod []
        #_abstract
        (#_"int" numParams [#_"ObjMethod" this])
        #_abstract
        (#_"String" getMethodName [#_"ObjMethod" this])
        #_abstract
        (#_"Type" getReturnType [#_"ObjMethod" this])
        #_abstract
        (#_"Type[]" getArgTypes [#_"ObjMethod" this])
    )
    (class! MethodParamExpr [Expr MaybePrimitiveExpr])
    (class! FnMethod [#_"ObjMethod"])
    (class! ObjExpr [Expr]
        #_abstract
        (#_"void" emitStatics [#_"ObjExpr" this, #_"ClassVisitor" gen])
        #_abstract
        (#_"void" emitMethods [#_"ObjExpr" this, #_"ClassVisitor" gen])
        #_abstract
        (#_"boolean" supportsMeta [#_"ObjExpr" this])
    )
    (class! FnExpr [#_"ObjExpr"])
    (class! DefExpr [Expr])
    (class! BindingInit [])
    (class! LetFnExpr [Expr])
    (class! LetExpr [Expr MaybePrimitiveExpr])
    (class! RecurExpr [Expr MaybePrimitiveExpr])
    (class! NewInstanceMethod [#_"ObjMethod"])
    (class! NewInstanceExpr [#_"ObjExpr"])
    (class! CaseExpr [Expr MaybePrimitiveExpr])
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
                (loop-when [methods [] bridges [] #_"int" i 0] [< i (alength allmethods)] => [methods bridges]
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
                    (loop-when [methods methods #_"int" i 0] [< i (count bridges)] => methods
                        (recur (conj methods (nth bridges i)) (inc i))
                    )
                )
              methods
                (when (and (not static?) (.isInterface c)) => methods
                    (let [allmethods (.getMethods Object)]
                        (loop-when [methods methods #_"int" i 0] [< i (alength allmethods)] => methods
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
                    (loop-when [ctors [] #_"int" i 0] [< i (alength allctors)] => ctors
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

(def PathType'enum-set
    (hash-set
        :PathType'PATH
        :PathType'BRANCH
    )
)

(class-ns PathNode
    (defn #_"PathNode" PathNode'new [#_"PathType" type, #_"PathNode" parent]
        (hash-map
            #_"PathType" :type type
            #_"PathNode" :parent parent
        )
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

(class-ns LiteralExpr
    (defn #_"LiteralExpr" LiteralExpr'new []
        (hash-map)
    )

    #_override
    (defn #_"Object" Expr'''eval--LiteralExpr [#_"LiteralExpr" this]
        (.literal this)
    )
)

(class-ns NilExpr
    (defn #_"NilExpr" NilExpr'new []
        (LiteralExpr'new)
    )

    #_override
    (defn #_"Object" LiteralExpr'''literal--NilExpr [#_"NilExpr" this]
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--NilExpr [#_"NilExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.visitInsn gen, Opcodes/ACONST_NULL)
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--NilExpr [#_"NilExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--NilExpr [#_"NilExpr" this]
        nil
    )

    (def #_"NilExpr" Compiler'NIL_EXPR (NilExpr'new))
)

(class-ns BooleanExpr
    (defn #_"BooleanExpr" BooleanExpr'new [#_"boolean" val]
        (merge (LiteralExpr'new)
            (hash-map
                #_"boolean" :val val
            )
        )
    )

    #_override
    (defn #_"Object" LiteralExpr'''literal--BooleanExpr [#_"BooleanExpr" this]
        (if (:val this) true false)
    )

    #_override
    (defn #_"void" Expr'''emit--BooleanExpr [#_"BooleanExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.getStatic gen, (Type/getType Boolean), (if (:val this) "TRUE" "FALSE"), (Type/getType Boolean))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--BooleanExpr [#_"BooleanExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--BooleanExpr [#_"BooleanExpr" this]
        Boolean
    )

    (def #_"BooleanExpr" Compiler'TRUE_EXPR (BooleanExpr'new true))
    (def #_"BooleanExpr" Compiler'FALSE_EXPR (BooleanExpr'new false))
)

(class-ns Compiler
    (def #_"Var" ^:dynamic *loader*                  ) ;; DynamicClassLoader
    (def #_"Var" ^:dynamic *line*                    ) ;; Integer
    (def #_"Var" ^:dynamic *method*                  ) ;; FnFrame
    (def #_"Var" ^:dynamic *local-env*               ) ;; symbol->localbinding
    (def #_"Var" ^:dynamic *next-local-num*          ) ;; Integer
    (def #_"Var" ^:dynamic *loop-locals*             ) ;; vector<localbinding>
    (def #_"Var" ^:dynamic *loop-label*              ) ;; Label
    (def #_"Var" ^:dynamic *constants*               ) ;; vector<object>
    (def #_"Var" ^:dynamic *constant-ids*            ) ;; IdentityHashMap
    (def #_"Var" ^:dynamic *keyword-callsites*       ) ;; vector<keyword>
    (def #_"Var" ^:dynamic *protocol-callsites*      ) ;; vector<var>
    (def #_"Var" ^:dynamic *keywords*                ) ;; keyword->constid
    (def #_"Var" ^:dynamic *vars*                    ) ;; var->constid
    (def #_"Var" ^:dynamic *in-catch-finally*   false)
    (def #_"Var" ^:dynamic *in-return-context*  false)
    (def #_"Var" ^:dynamic *no-recur*           false)
    (def #_"Var" ^:dynamic *compile-stub-sym*     nil)
    (def #_"Var" ^:dynamic *compile-stub-class*   nil)
    (def #_"Var" ^:dynamic *clear-path*           nil) ;; PathNode chain
    (def #_"Var" ^:dynamic *clear-root*           nil) ;; tail of PathNode chain
    (def #_"Var" ^:dynamic *clear-sites*          nil) ;; LocalBinding -> Set<LocalBindingExpr>

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

    (def- #_"Type[]" Compiler'EXCEPTION_TYPES (object-array 0))

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
            (let [#_"Symbol" nsSym (symbol (:ns sym))]
                ;; first check against currentNS' aliases, otherwise check the Namespaces map
                (or (.lookupAlias inns, nsSym) (find-ns nsSym))
            )
        )
    )

    (defn #_"Symbol" Compiler'resolveSymbol [#_"Symbol" sym]
        ;; already qualified or classname?
        (cond
            (pos? (.indexOf (:name sym), (int \.)))
                sym
            (some? (:ns sym))
                (let [#_"Namespace" ns (Compiler'namespaceFor sym)]
                    (if (and (some? ns) (not (and (some? (:name (:name ns))) (= (:name (:name ns)) (:ns sym)))))
                        (symbol (:name (:name ns)), (:name sym))
                        sym
                    )
                )
            :else
                (let [#_"Object" o (.getMapping *ns*, sym)]
                    (cond
                        (nil? o)            (symbol (:name (:name *ns*)), (:name sym))
                        (instance? Class o) (symbol (.getName o))
                        (var? o)            (symbol (:name (:name (:ns o))), (:name (:sym o)))
                    )
                )
        )
    )

    (defn #_"Class" Compiler'maybePrimitiveType [#_"Expr" e]
        (when (and (instance? MaybePrimitiveExpr e) (.hasJavaClass e) (.canEmitPrimitive e))
            (let-when [#_"Class" c (.getJavaClass e)] (Reflector'isPrimitive c)
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
                            (.hasJavaClass e)
                                (let [#_"Class" c (.getJavaClass e)]
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
                    (.append sb, (if (and (.hasJavaClass arg) (some? (.getJavaClass arg))) (.getName (.getJavaClass arg)) "unknown"))
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
                                      #_"Class" aclass (if (.hasJavaClass arg) (.getJavaClass arg) Object) #_"Class" pclass (aget (nth pars i) p)
                                      [exact match]
                                        (if (and (.hasJavaClass arg) (= aclass pclass))
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

    (defn- #_"int" Compiler'getAndIncLocalNum []
        (let [#_"ObjMethod" m *method* #_"int" n (.intValue *next-local-num*)]
            (when (< (:maxLocal m) n)
                (§ set! (:maxLocal m) n)
            )
            (set! *next-local-num* (inc n))
            n
        )
    )

    (declare LocalBinding'new)

    (defn- #_"LocalBinding" Compiler'registerLocal [#_"Symbol" sym, #_"Symbol" tag, #_"Expr" init, #_"boolean" isArg]
        (let [#_"int" n (Compiler'getAndIncLocalNum)
              #_"LocalBinding" lb (LocalBinding'new n, sym, tag, init, isArg, *clear-root*)]
            (update! *local-env* assoc (:sym lb) lb)
            (let [#_"ObjMethod" method *method*]
                (§ update! (:locals method) assoc lb lb)
                (§ update! (:indexlocals method) assoc n lb)
                lb
            )
        )
    )

    (defn #_"void" Compiler'closeOver [#_"LocalBinding" b, #_"ObjMethod" method]
        (when (and (some? b) (some? method))
            (let [#_"LocalBinding" lb (get (:locals method) b)]
                (if (nil? lb)
                    (do
                        (§ update! (:closes (:objx method)) assoc b b)
                        (Compiler'closeOver b, (:parent method))
                    )
                    (do
                        (when (zero? (:idx lb))
                            (§ set! (:usesThis method) true)
                        )
                        (when *in-catch-finally*
                            (§ update! (:localsUsedInCatchFinally method) conj (:idx b))
                        )
                    )
                )
            )
        )
        nil
    )

    (defn #_"LocalBinding" Compiler'referenceLocal [#_"Symbol" sym]
        (when-let [#_"LocalBinding" lb (get *local-env* sym)]
            (when (zero? (:idx lb))
                (§ set! (:usesThis *method*) true)
            )
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
        (when (bound? #'*keyword-callsites*) => (throw (IllegalAccessError. "KEYWORD_CALLSITES is not bound"))
            (let [#_"IPersistentVector" v (conj *keyword-callsites* k)]
                (set! *keyword-callsites* v)
                (dec (count v))
            )
        )
    )

    (defn- #_"int" Compiler'registerProtocolCallsite [#_"Var" v]
        (when (bound? #'*protocol-callsites*) => (throw (IllegalAccessError. "PROTOCOL_CALLSITES is not bound"))
            (let [#_"IPersistentVector" v (conj *protocol-callsites* v)]
                (set! *protocol-callsites* v)
                (dec (count v))
            )
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
                        (some? (:ns sym))
                            (when-let [#_"Namespace" ns (Compiler'namespaceFor sym)]
                                (let [#_"Symbol" name (symbol (:name sym))]
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
                                            (.intern *ns*, (symbol (:name sym)))
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
                        (when (or (= (:ns v) *ns*) (not (get (meta v) :private))) => (throw (IllegalStateException. (str "var: " v " is private")))
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
                    (when (or (= (:ns v) *ns*) (not (get (meta v) :private))) => (throw (IllegalStateException. (str "var: " v " is private")))
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
        (and (some? (:ns sym)) (nil? (Compiler'namespaceFor sym)))
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

    (defn #_"ISeq" Compiler'fwdPath [#_"PathNode" p]
        (loop-when-recur [#_"ISeq" s nil p p] (some? p) [(cons p s) (:parent p)] => s)
    )

    (defn #_"PathNode" Compiler'commonPath [#_"PathNode" p1, #_"PathNode" p2]
        (let [#_"ISeq" s1 (Compiler'fwdPath p1) #_"ISeq" s2 (Compiler'fwdPath p2)]
            (when (= (first s1) (first s2))
                (loop-when-recur [s1 s1 s2 s2]
                                 (and (some? (second s1)) (= (second s1) (second s2)))
                                 [(next s1) (next s2)]
                              => (first s1)
                )
            )
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
            (some? (:ns sym))
                (let-when [#_"Namespace" ns (Compiler'namespaceFor n, sym)] (some? ns)          => (throw (RuntimeException. (str "No such namespace: " (:ns sym))))
                    (let-when [#_"Var" v (.findInternedVar ns, (symbol (:name sym)))] (some? v) => (throw (RuntimeException. (str "No such var: " sym)))
                        (when (or (= (:ns v) *ns*) (not (get (meta v) :private)) allowPrivate)  => (throw (IllegalStateException. (str "var: " sym " is private")))
                            v
                        )
                    )
                )
            (or (pos? (.indexOf (:name sym), (int \.))) (= (.charAt (:name sym), 0) \[)) (RT/classForName (:name sym))
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
            (some? (:ns sym))
                (when-let [#_"Namespace" ns (Compiler'namespaceFor n, sym)]
                    (when-let [#_"Var" v (.findInternedVar ns, (symbol (:name sym)))]
                        v
                    )
                )
            (or (and (pos? (.indexOf (:name sym), (int \.))) (not (.endsWith (:name sym), "."))) (= (.charAt (:name sym), 0) \[))
                (RT/classForName (:name sym))
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

(class-ns UntypedExpr
    (defn #_"UntypedExpr" UntypedExpr'new []
        (hash-map)
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--UntypedExpr [#_"UntypedExpr" this]
        (throw (IllegalArgumentException. "Has no Java class"))
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--UntypedExpr [#_"UntypedExpr" this]
        false
    )
)

(class-ns MonitorEnterExpr
    (defn #_"MonitorEnterExpr" MonitorEnterExpr'new [#_"Expr" target]
        (merge (UntypedExpr'new)
            (hash-map
                #_"Expr" :target target
            )
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--MonitorEnterExpr [#_"MonitorEnterExpr" this]
        (throw (UnsupportedOperationException. "Can't eval monitor-enter"))
    )

    #_override
    (defn #_"void" Expr'''emit--MonitorEnterExpr [#_"MonitorEnterExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emit (:target this), :Context'EXPRESSION, objx, gen)
        (.monitorEnter gen)
        (.emit Compiler'NIL_EXPR, context, objx, gen)
        nil
    )
)

(declare Compiler'analyze)

(class-ns MonitorEnterParser
    (defn #_"IParser" MonitorEnterParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (MonitorEnterExpr'new (Compiler'analyze :Context'EXPRESSION, (second form)))
            )
        )
    )
)

(class-ns MonitorExitExpr
    (defn #_"MonitorExitExpr" MonitorExitExpr'new [#_"Expr" target]
        (merge (UntypedExpr'new)
            (hash-map
                #_"Expr" :target target
            )
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--MonitorExitExpr [#_"MonitorExitExpr" this]
        (throw (UnsupportedOperationException. "Can't eval monitor-exit"))
    )

    #_override
    (defn #_"void" Expr'''emit--MonitorExitExpr [#_"MonitorExitExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emit (:target this), :Context'EXPRESSION, objx, gen)
        (.monitorExit gen)
        (.emit Compiler'NIL_EXPR, context, objx, gen)
        nil
    )
)

(class-ns MonitorExitParser
    (defn #_"IParser" MonitorExitParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (MonitorExitExpr'new (Compiler'analyze :Context'EXPRESSION, (second form)))
            )
        )
    )
)

(class-ns AssignExpr
    (defn #_"AssignExpr" AssignExpr'new [#_"AssignableExpr" target, #_"Expr" val]
        (hash-map
            #_"AssignableExpr" :target target
            #_"Expr" :val val
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--AssignExpr [#_"AssignExpr" this]
        (.evalAssign (:target this), (:val this))
    )

    #_override
    (defn #_"void" Expr'''emit--AssignExpr [#_"AssignExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emitAssign (:target this), context, objx, gen, (:val this))
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--AssignExpr [#_"AssignExpr" this]
        (.hasJavaClass (:val this))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--AssignExpr [#_"AssignExpr" this]
        (.getJavaClass (:val this))
    )
)

(class-ns AssignParser
    (defn #_"IParser" AssignParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (= (count form) 3) => (throw (IllegalArgumentException. "Malformed assignment, expecting (set! target val)"))
                    (let [#_"Expr" target (Compiler'analyze :Context'EXPRESSION, (second form))]
                        (when (instance? AssignableExpr target) => (throw (IllegalArgumentException. "Invalid assignment target"))
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
        (hash-map
            #_"String" :c c
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--ImportExpr [#_"ImportExpr" this]
        (.importClass *ns*, (RT/classForNameNonLoading (:c this)))
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--ImportExpr [#_"ImportExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--ImportExpr [#_"ImportExpr" this]
        false
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--ImportExpr [#_"ImportExpr" this]
        (throw (IllegalArgumentException. "ImportExpr has no Java class"))
    )
)

(class-ns ImportParser
    (defn #_"IParser" ImportParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (ImportExpr'new (second form))
            )
        )
    )
)

(class-ns EmptyExpr
    (defn #_"EmptyExpr" EmptyExpr'new [#_"Object" coll]
        (hash-map
            #_"Object" :coll coll
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--EmptyExpr [#_"EmptyExpr" this]
        (:coll this)
    )

    #_override
    (defn #_"void" Expr'''emit--EmptyExpr [#_"EmptyExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--EmptyExpr [#_"EmptyExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--EmptyExpr [#_"EmptyExpr" this]
        (cond
            (instance? IPersistentList (:coll this))   IPersistentList
            (instance? IPersistentVector (:coll this)) IPersistentVector
            (instance? IPersistentMap (:coll this))    IPersistentMap
            (instance? IPersistentSet (:coll this))    IPersistentSet
            :else                                      (throw (UnsupportedOperationException. "Unknown collection type"))
        )
    )
)

(class-ns ConstantExpr
    (defn #_"ConstantExpr" ConstantExpr'new [#_"Object" v]
        (merge (LiteralExpr'new)
            (hash-map
                ;; stuff quoted vals in classloader at compile time, pull out at runtime
                ;; this won't work for static compilation...
                #_"Object" :v v
                #_"int" :id (Compiler'registerConstant v)
            )
        )
    )

    #_override
    (defn #_"Object" LiteralExpr'''literal--ConstantExpr [#_"ConstantExpr" this]
        (:v this)
    )

    (declare ObjExpr''emitConstant)

    #_override
    (defn #_"void" Expr'''emit--ConstantExpr [#_"ConstantExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (ObjExpr''emitConstant objx, gen, (:id this))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--ConstantExpr [#_"ConstantExpr" this]
        (Modifier/isPublic (.getModifiers (.getClass (:v this))))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--ConstantExpr [#_"ConstantExpr" this]
        (cond
            (instance? APersistentMap (:v this))    APersistentMap
            (instance? APersistentSet (:v this))    APersistentSet
            (instance? APersistentVector (:v this)) APersistentVector
            :else                                   (.getClass (:v this))
        )
    )
)

(class-ns NumberExpr
    (defn #_"NumberExpr" NumberExpr'new [#_"Number" n]
        (merge (LiteralExpr'new)
            (hash-map
                #_"Number" :n n
                #_"int" :id (Compiler'registerConstant n)
            )
        )
    )

    #_override
    (defn #_"Object" LiteralExpr'''literal--NumberExpr [#_"NumberExpr" this]
        (:n this)
    )

    #_override
    (defn #_"void" Expr'''emit--NumberExpr [#_"NumberExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (when (not= context :Context'STATEMENT)
            (ObjExpr''emitConstant objx, gen, (:id this))
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--NumberExpr [#_"NumberExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--NumberExpr [#_"NumberExpr" this]
        (cond
            (instance? Integer (:n this)) Long/TYPE
            (instance? Double (:n this))  Double/TYPE
            (instance? Long (:n this))    Long/TYPE
            :else                         (throw (IllegalStateException. (str "Unsupported Number type: " (.getName (.getClass (:n this))))))
        )
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--NumberExpr [#_"NumberExpr" this]
        true
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--NumberExpr [#_"NumberExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (cond
            (instance? Integer (:n this)) (.push gen, (.longValue (:n this)))
            (instance? Double (:n this))  (.push gen, (.doubleValue (:n this)))
            (instance? Long (:n this))    (.push gen, (.longValue (:n this)))
        )
        nil
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
        (merge (LiteralExpr'new)
            (hash-map
                #_"String" :str str
            )
        )
    )

    #_override
    (defn #_"Object" LiteralExpr'''literal--StringExpr [#_"StringExpr" this]
        (:str this)
    )

    #_override
    (defn #_"void" Expr'''emit--StringExpr [#_"StringExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (when (not= context :Context'STATEMENT)
            (.push gen, (:str this))
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--StringExpr [#_"StringExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--StringExpr [#_"StringExpr" this]
        String
    )
)

(class-ns KeywordExpr
    (defn #_"KeywordExpr" KeywordExpr'new [#_"Keyword" k]
        (merge (LiteralExpr'new)
            (hash-map
                #_"Keyword" :k k
            )
        )
    )

    #_override
    (defn #_"Object" LiteralExpr'''literal--KeywordExpr [#_"KeywordExpr" this]
        (:k this)
    )

    #_override
    (defn #_"Object" Expr'''eval--KeywordExpr [#_"KeywordExpr" this]
        (:k this)
    )

    (declare ObjExpr''emitKeyword)

    #_override
    (defn #_"void" Expr'''emit--KeywordExpr [#_"KeywordExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (ObjExpr''emitKeyword objx, gen, (:k this))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--KeywordExpr [#_"KeywordExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--KeywordExpr [#_"KeywordExpr" this]
        Keyword
    )
)

(class-ns ConstantParser
    (defn #_"IParser" ConstantParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
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

(class-ns HostExpr
    (defn #_"HostExpr" HostExpr'new []
        (hash-map)
    )

    (defn #_"void" HostExpr'emitBoxReturn [#_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Class" returnType]
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
                Void/TYPE      (.emit Compiler'NIL_EXPR, :Context'EXPRESSION, objx, gen)
            )
        )
        nil
    )

    (defn #_"void" HostExpr'emitUnboxArg [#_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Class" paramType]
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

    (defn #_"Class" HostExpr'maybeClass [#_"Object" form, #_"boolean" stringOk]
        (cond
            (instance? Class form)
                form
            (symbol? form)
                (when (nil? (:ns form)) ;; if ns-qualified can't be classname
                    (cond
                        (= form *compile-stub-sym*)
                            *compile-stub-class*
                        (or (pos? (.indexOf (:name form), (int \.))) (= (.charAt (:name form), 0) \[))
                            (RT/classForNameNonLoading (:name form))
                        :else
                            (let [#_"Object" o (.getMapping *ns*, form)]
                                (cond
                                    (instance? Class o)
                                        o
                                    (contains? *local-env* form)
                                        nil
                                    :else
                                        (try
                                            (RT/classForNameNonLoading (:name form))
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

    (defn #_"Class" HostExpr'primClassForName [#_"Symbol" sym]
        (when (some? sym)
            (case (:name sym)
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

    (defn #_"Class" HostExpr'maybeSpecialTag [#_"Symbol" sym]
        (or (HostExpr'primClassForName sym)
            (case (:name sym)
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

    (defn #_"Class" HostExpr'tagToClass [#_"Object" tag]
        (or
            (when (and (symbol? tag) (nil? (:ns tag))) ;; if ns-qualified can't be classname
                (HostExpr'maybeSpecialTag tag)
            )
            (HostExpr'maybeClass tag, true)
            (throw (IllegalArgumentException. (str "Unable to resolve classname: " tag)))
        )
    )

    (defn #_"Class" HostExpr'tagClass [#_"Object" tag]
        (when (some? tag) => Object
            (or
                (when (symbol? tag)
                    (HostExpr'primClassForName tag)
                )
                (HostExpr'tagToClass tag)
            )
        )
    )
)

(class-ns FieldExpr
    (defn #_"FieldExpr" FieldExpr'new []
        (HostExpr'new)
    )

)

(class-ns InstanceFieldExpr
    (defn #_"InstanceFieldExpr" InstanceFieldExpr'new [#_"int" line, #_"Expr" target, #_"String" fieldName, #_"Symbol" tag, #_"boolean" requireField]
        (let [#_"Class" c (when (.hasJavaClass target) (.getJavaClass target))
              #_"java.lang.reflect.Field" f (when (some? c) (Reflector'getField c, fieldName, false))]
            (when (and (nil? f) *warn-on-reflection*)
                (if (nil? c)
                    (.println *err*, (str "Reflection warning, line " line " - reference to field " fieldName " can't be resolved."))
                    (.println *err*, (str "Reflection warning, line " line " - reference to field " fieldName " on " (.getName c) " can't be resolved."))
                )
            )
            (merge (FieldExpr'new)
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

    #_override
    (defn #_"Object" Expr'''eval--InstanceFieldExpr [#_"InstanceFieldExpr" this]
        (Reflector'invokeNoArgInstanceMember (.eval (:target this)), (:fieldName this), (:requireField this))
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--InstanceFieldExpr [#_"InstanceFieldExpr" this]
        (and (some? (:targetClass this)) (some? (:field this)) (Reflector'isPrimitive (.getType (:field this))))
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--InstanceFieldExpr [#_"InstanceFieldExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (when (and (some? (:targetClass this)) (some? (:field this))) => (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))
            (.emit (:target this), :Context'EXPRESSION, objx, gen)
            (.visitLineNumber gen, (:line this), (.mark gen))
            (.checkCast gen, (Compiler'getType (:targetClass this)))
            (.getField gen, (Compiler'getType (:targetClass this)), (:fieldName this), (Type/getType (.getType (:field this))))
        )
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--InstanceFieldExpr [#_"InstanceFieldExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (if (and (some? (:targetClass this)) (some? (:field this)))
            (do
                (.emit (:target this), :Context'EXPRESSION, objx, gen)
                (.visitLineNumber gen, (:line this), (.mark gen))
                (.checkCast gen, (Compiler'getType (:targetClass this)))
                (.getField gen, (Compiler'getType (:targetClass this)), (:fieldName this), (Type/getType (.getType (:field this))))
                (HostExpr'emitBoxReturn objx, gen, (.getType (:field this)))
                (when (= context :Context'STATEMENT)
                    (.pop gen)
                )
            )
            (do
                (.emit (:target this), :Context'EXPRESSION, objx, gen)
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--InstanceFieldExpr [#_"InstanceFieldExpr" this]
        (or (some? (:field this)) (some? (:tag this)))
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--InstanceFieldExpr [#_"InstanceFieldExpr" this]
        (if (some? (:tag this)) (HostExpr'tagToClass (:tag this)) (.getType (:field this)))
    )

    #_override
    (defn #_"Object" AssignableExpr'''evalAssign--InstanceFieldExpr [#_"InstanceFieldExpr" this, #_"Expr" val]
        (Reflector'setInstanceField (.eval (:target this)), (:fieldName this), (.eval val))
    )

    #_override
    (defn #_"void" AssignableExpr'''emitAssign--InstanceFieldExpr [#_"InstanceFieldExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Expr" val]
        (if (and (some? (:targetClass this)) (some? (:field this)))
            (do
                (.emit (:target this), :Context'EXPRESSION, objx, gen)
                (.checkCast gen, (Compiler'getType (:targetClass this)))
                (.emit val, :Context'EXPRESSION, objx, gen)
                (.visitLineNumber gen, (:line this), (.mark gen))
                (.dupX1 gen)
                (HostExpr'emitUnboxArg objx, gen, (.getType (:field this)))
                (.putField gen, (Compiler'getType (:targetClass this)), (:fieldName this), (Type/getType (.getType (:field this))))
            )
            (do
                (.emit (:target this), :Context'EXPRESSION, objx, gen)
                (.push gen, (:fieldName this))
                (.emit val, :Context'EXPRESSION, objx, gen)
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

(class-ns StaticFieldExpr
    (defn #_"StaticFieldExpr" StaticFieldExpr'new [#_"int" line, #_"Class" c, #_"String" fieldName, #_"Symbol" tag]
        (merge (FieldExpr'new)
            (hash-map
                #_"int" :line line
                #_"Class" :c c
                #_"String" :fieldName fieldName
                #_"Symbol" :tag tag

                #_"java.lang.reflect.Field" :field (.getField c, fieldName)
            )
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--StaticFieldExpr [#_"StaticFieldExpr" this]
        (Reflector'getStaticField (:c this), (:fieldName this))
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--StaticFieldExpr [#_"StaticFieldExpr" this]
        (Reflector'isPrimitive (.getType (:field this)))
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--StaticFieldExpr [#_"StaticFieldExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.visitLineNumber gen, (:line this), (.mark gen))
        (.getStatic gen, (Type/getType (:c this)), (:fieldName this), (Type/getType (.getType (:field this))))
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--StaticFieldExpr [#_"StaticFieldExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.visitLineNumber gen, (:line this), (.mark gen))

        (.getStatic gen, (Type/getType (:c this)), (:fieldName this), (Type/getType (.getType (:field this))))
        (HostExpr'emitBoxReturn objx, gen, (.getType (:field this)))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--StaticFieldExpr [#_"StaticFieldExpr" this]
        true
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--StaticFieldExpr [#_"StaticFieldExpr" this]
        (if (some? (:tag this)) (HostExpr'tagToClass (:tag this)) (.getType (:field this)))
    )

    #_override
    (defn #_"Object" AssignableExpr'''evalAssign--StaticFieldExpr [#_"StaticFieldExpr" this, #_"Expr" val]
        (Reflector'setStaticField (:c this), (:fieldName this), (.eval val))
    )

    #_override
    (defn #_"void" AssignableExpr'''emitAssign--StaticFieldExpr [#_"StaticFieldExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Expr" val]
        (.emit val, :Context'EXPRESSION, objx, gen)
        (.visitLineNumber gen, (:line this), (.mark gen))
        (.dup gen)
        (HostExpr'emitUnboxArg objx, gen, (.getType (:field this)))
        (.putStatic gen, (Type/getType (:c this)), (:fieldName this), (Type/getType (.getType (:field this))))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )
)

(class-ns MethodExpr
    (defn #_"MethodExpr" MethodExpr'new []
        (HostExpr'new)
    )

    (defn #_"void" MethodExpr'emitArgsAsArray [#_"IPersistentVector" args, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.push gen, (count args))
        (.newArray gen, (Type/getType Object))
        (dotimes [#_"int" i (count args)]
            (.dup gen)
            (.push gen, i)
            (.emit (nth args i), :Context'EXPRESSION, objx, gen)
            (.arrayStore gen, (Type/getType Object))
        )
        nil
    )

    (defn #_"void" MethodExpr'emitTypedArgs [#_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Class[]" parameterTypes, #_"IPersistentVector" args]
        (dotimes [#_"int" i (alength parameterTypes)]
            (let [#_"Expr" e (nth args i) #_"Class" primc (Compiler'maybePrimitiveType e)]
                (cond
                    (= primc (aget parameterTypes i))
                        (do
                            (.emitUnboxed e, :Context'EXPRESSION, objx, gen)
                        )
                    (and (= primc Integer/TYPE) (= (aget parameterTypes i) Long/TYPE))
                        (do
                            (.emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            (.visitInsn gen, Opcodes/I2L)
                        )
                    (and (= primc Long/TYPE) (= (aget parameterTypes i) Integer/TYPE))
                        (do
                            (.emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            (.invokeStatic gen, (Type/getType RT), (Method/getMethod "int intCast(long)"))
                        )
                    (and (= primc Float/TYPE) (= (aget parameterTypes i) Double/TYPE))
                        (do
                            (.emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            (.visitInsn gen, Opcodes/F2D)
                        )
                    (and (= primc Double/TYPE) (= (aget parameterTypes i) Float/TYPE))
                        (do
                            (.emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            (.visitInsn gen, Opcodes/D2F)
                        )
                    :else
                        (do
                            (.emit e, :Context'EXPRESSION, objx, gen)
                            (HostExpr'emitUnboxArg objx, gen, (aget parameterTypes i))
                        )
                )
            )
        )
        nil
    )
)

(class-ns ObjMethod
    (defn #_"ObjMethod" ObjMethod'new [#_"ObjExpr" objx, #_"ObjMethod" parent]
        (hash-map
            #_"ObjExpr" :objx objx
            ;; when closures are defined inside other closures,
            ;; the closed over locals need to be propagated to the enclosing objx
            #_"ObjMethod" :parent parent
            ;; localbinding->localbinding
            #_mutable #_"IPersistentMap" :locals nil
            ;; num->localbinding
            #_mutable #_"IPersistentMap" :indexlocals nil
            #_"Expr" :body nil
            #_"PersistentVector" :argLocals nil
            #_mutable #_"int" :maxLocal 0
            #_"int" :line 0
            #_mutable #_"boolean" :usesThis false
            #_mutable #_"PersistentHashSet" :localsUsedInCatchFinally #{}
            #_"IPersistentMap" :methodMeta nil
        )
    )

    (defn #_"void" ObjMethod'emitBody [#_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Class" retClass, #_"Expr" body]
        (if (and (Reflector'isPrimitive retClass) (.canEmitPrimitive body))
            (let [#_"Class" c (Compiler'maybePrimitiveType body)]
                (cond (= c retClass)
                    (do
                        (.emitUnboxed body, :Context'RETURN, objx, gen)
                    )
                    (and (= retClass Long/TYPE) (= c Integer/TYPE))
                    (do
                        (.emitUnboxed body, :Context'RETURN, objx, gen)
                        (.visitInsn gen, Opcodes/I2L)
                    )
                    (and (= retClass Double/TYPE) (= c Float/TYPE))
                    (do
                        (.emitUnboxed body, :Context'RETURN, objx, gen)
                        (.visitInsn gen, Opcodes/F2D)
                    )
                    (and (= retClass Integer/TYPE) (= c Long/TYPE))
                    (do
                        (.emitUnboxed body, :Context'RETURN, objx, gen)
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "int intCast(long)"))
                    )
                    (and (= retClass Float/TYPE) (= c Double/TYPE))
                    (do
                        (.emitUnboxed body, :Context'RETURN, objx, gen)
                        (.visitInsn gen, Opcodes/D2F)
                    )
                    :else
                    (do
                        (throw (IllegalArgumentException. (str "Mismatched primitive return, expected: " retClass ", had: " (.getJavaClass body))))
                    )
                )
            )
            (do
                (.emit body, :Context'RETURN, objx, gen)
                (if (= retClass Void/TYPE)
                    (.pop gen)
                    (.unbox gen, (Type/getType retClass))
                )
            )
        )
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--ObjMethod [#_"ObjMethod" this, #_"ObjExpr" fn, #_"ClassVisitor" cv]
        (let [#_"Method" m (Method. (.getMethodName this), (.getReturnType this), (.getArgTypes this))
              #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, Compiler'EXCEPTION_TYPES, cv)]
            (.visitCode gen)
            (let [#_"Label" loopLabel (.mark gen)]
                (.visitLineNumber gen, (:line this), loopLabel)
                (binding [*loop-label* loopLabel, *method* this]
                    (.emit (:body this), :Context'RETURN, fn, gen)
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

    #_method
    (defn #_"void" ObjMethod''emitClearLocals [#_"ObjMethod" this, #_"GeneratorAdapter" gen]
        nil
    )

    (declare LocalBinding''getPrimitiveType)

    #_method
    (defn #_"void" ObjMethod''emitClearLocalsOld [#_"ObjMethod" this, #_"GeneratorAdapter" gen]
        (dotimes [#_"int" i (count (:argLocals this))]
            (let [#_"LocalBinding" lb (nth (:argLocals this) i)]
                (when (and (not (contains? (:localsUsedInCatchFinally this) (:idx lb))) (nil? (LocalBinding''getPrimitiveType lb)))
                    (.visitInsn gen, Opcodes/ACONST_NULL)
                    (.storeArg gen, (dec (:idx lb)))
                )
            )
        )
        (loop-when-recur [#_"int" i (inc (.numParams this))] (< i (inc (:maxLocal this))) [(inc i)]
            (when (not (contains? (:localsUsedInCatchFinally this) i))
                (let [#_"LocalBinding" b (get (:indexlocals this) i)]
                    (when (or (nil? b) (nil? (Compiler'maybePrimitiveType (:init b))))
                        (.visitInsn gen, Opcodes/ACONST_NULL)
                        (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), i)
                    )
                )
            )
        )
        nil
    )

    #_method
    (defn #_"void" ObjMethod''emitClearThis [#_"ObjMethod" this, #_"GeneratorAdapter" gen]
        (.visitInsn gen, Opcodes/ACONST_NULL)
        (.visitVarInsn gen, Opcodes/ASTORE, 0)
        nil
    )
)

(class-ns InstanceMethodExpr
    (defn #_"InstanceMethodExpr" InstanceMethodExpr'new [#_"int" line, #_"Symbol" tag, #_"Expr" target, #_"String" methodName, #_"IPersistentVector" args, #_"boolean" tailPosition]
        (let [#_"java.lang.reflect.Method" method
                (if (and (.hasJavaClass target) (some? (.getJavaClass target)))
                    (let [#_"PersistentVector" methods (Reflector'getMethods (.getJavaClass target), (count args), methodName, false)]
                        (if (zero? (count methods))
                            (do
                                (when *warn-on-reflection*
                                    (.println *err*, (str "Reflection warning, line " line " - call to method " methodName " on " (.getName (.getJavaClass target)) " can't be resolved (no such method)."))
                                )
                                nil
                            )
                            (let [#_"int" methodidx
                                    (when (< 1 (count methods)) => 0
                                        (let [[#_"PersistentVector" pars #_"PersistentVector" rets]
                                                (loop-when [pars [] rets [] #_"int" i 0] [< i (count methods)] => [pars rets]
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
                                    (.println *err*, (str "Reflection warning, line " line " - call to method " methodName " on " (.getName (.getJavaClass target)) " can't be resolved (argument types: " (Compiler'getTypeStringForArgs args) ")."))
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
            (merge (MethodExpr'new)
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

    #_override
    (defn #_"Object" Expr'''eval--InstanceMethodExpr [#_"InstanceMethodExpr" this]
        (let [#_"Object" target (.eval (:target this)) #_"Object[]" args (make-array Object (count (:args this)))]
            (dotimes [#_"int" i (count (:args this))]
                (aset args i (.eval (nth (:args this) i)))
            )
            (if (some? (:method this))
                (Reflector'invokeMatchingMethod (:methodName this), [(:method this)], target, args)
                (Reflector'invokeInstanceMethod target, (:methodName this), args)
            )
        )
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--InstanceMethodExpr [#_"InstanceMethodExpr" this]
        (and (some? (:method this)) (Reflector'isPrimitive (.getReturnType (:method this))))
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--InstanceMethodExpr [#_"InstanceMethodExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (when (some? (:method this)) => (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))
            (let [#_"Type" type (Type/getType (.getDeclaringClass (:method this)))]
                (.emit (:target this), :Context'EXPRESSION, objx, gen)
                (.checkCast gen, type)
                (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
                (.visitLineNumber gen, (:line this), (.mark gen))
                (when (and (:tailPosition this) (not (:canBeDirect objx)))
                    (ObjMethod''emitClearThis *method*, gen)
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

    #_override
    (defn #_"void" Expr'''emit--InstanceMethodExpr [#_"InstanceMethodExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (if (some? (:method this))
            (let [#_"Type" type (Type/getType (.getDeclaringClass (:method this)))]
                (.emit (:target this), :Context'EXPRESSION, objx, gen)
                (.checkCast gen, type)
                (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
                (.visitLineNumber gen, (:line this), (.mark gen))
                (when (= context :Context'RETURN)
                    (ObjMethod''emitClearLocals *method*, gen)
                )
                (let [#_"Method" m (Method. (:methodName this), (Type/getReturnType (:method this)), (Type/getArgumentTypes (:method this)))]
                    (if (.isInterface (.getDeclaringClass (:method this)))
                        (.invokeInterface gen, type, m)
                        (.invokeVirtual gen, type, m)
                    )
                    (HostExpr'emitBoxReturn objx, gen, (.getReturnType (:method this)))
                )
            )
            (do
                (.emit (:target this), :Context'EXPRESSION, objx, gen)
                (.push gen, (:methodName this))
                (MethodExpr'emitArgsAsArray (:args this), objx, gen)
                (.visitLineNumber gen, (:line this), (.mark gen))
                (when (= context :Context'RETURN)
                    (ObjMethod''emitClearLocals *method*, gen)
                )
                (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Object invokeInstanceMethod(Object, String, Object[])"))
            )
        )
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--InstanceMethodExpr [#_"InstanceMethodExpr" this]
        (or (some? (:method this)) (some? (:tag this)))
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--InstanceMethodExpr [#_"InstanceMethodExpr" this]
        (Compiler'retType (when (some? (:tag this)) (HostExpr'tagToClass (:tag this))), (when (some? (:method this)) (.getReturnType (:method this))))
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
                                            (loop-when [pars [] rets [] #_"int" i 0] [< i (count methods)] => [pars rets]
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
            (merge (MethodExpr'new)
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

    #_override
    (defn #_"Object" Expr'''eval--StaticMethodExpr [#_"StaticMethodExpr" this]
        (let [#_"Object[]" args (make-array Object (count (:args this)))]
            (dotimes [#_"int" i (count (:args this))]
                (aset args i (.eval (nth (:args this) i)))
            )
            (if (some? (:method this))
                (Reflector'invokeMatchingMethod (:methodName this), [(:method this)], nil, args)
                (Reflector'invokeStaticMethod (:c this), (:methodName this), args)
            )
        )
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--StaticMethodExpr [#_"StaticMethodExpr" this]
        (and (some? (:method this)) (Reflector'isPrimitive (.getReturnType (:method this))))
    )

    #_method
    (defn #_"boolean" StaticMethodExpr''canEmitIntrinsicPredicate [#_"StaticMethodExpr" this]
        (and (some? (:method this)) (some? (get Intrinsics'preds (.toString (:method this)))))
    )

    #_method
    (defn #_"void" StaticMethodExpr''emitIntrinsicPredicate [#_"StaticMethodExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Label" falseLabel]
        (.visitLineNumber gen, (:line this), (.mark gen))
        (when (some? (:method this)) => (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))
            (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
            (when (= context :Context'RETURN)
                (ObjMethod''emitClearLocals *method*, gen)
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

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--StaticMethodExpr [#_"StaticMethodExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (when (some? (:method this)) => (throw (UnsupportedOperationException. "Unboxed emit of unknown member"))
            (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
            (.visitLineNumber gen, (:line this), (.mark gen))
            (when (= context :Context'RETURN)
                (ObjMethod''emitClearLocals *method*, gen)
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

    #_override
    (defn #_"void" Expr'''emit--StaticMethodExpr [#_"StaticMethodExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (if (some? (:method this))
            (do
                (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
                (.visitLineNumber gen, (:line this), (.mark gen))
                (when (and (:tailPosition this) (not (:canBeDirect objx)))
                    (ObjMethod''emitClearThis *method*, gen)
                )
                (let [#_"Type" type (Type/getType (:c this))
                      #_"Method" m (Method. (:methodName this), (Type/getReturnType (:method this)), (Type/getArgumentTypes (:method this)))]
                    (.invokeStatic gen, type, m)
                    (when (= context :Context'STATEMENT) => (HostExpr'emitBoxReturn objx, gen, (.getReturnType (:method this)))
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
                    (ObjMethod''emitClearLocals *method*, gen)
                )
                (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Object invokeStaticMethod(Class, String, Object[])"))
                (when (= context :Context'STATEMENT)
                    (.pop gen)
                )
            )
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--StaticMethodExpr [#_"StaticMethodExpr" this]
        (or (some? (:method this)) (some? (:tag this)))
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--StaticMethodExpr [#_"StaticMethodExpr" this]
        (Compiler'retType (when (some? (:tag this)) (HostExpr'tagToClass (:tag this))), (when (some? (:method this)) (.getReturnType (:method this))))
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
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when-not (< (count form) 3) => (throw (IllegalArgumentException. "Malformed member expression, expecting (. target member ...)"))
                    ;; determine static or instance
                    ;; static target must be symbol, either fully.qualified.Classname or Classname that has been imported
                    (let [#_"int" line *line* #_"Class" c (HostExpr'maybeClass (second form), false)
                          ;; at this point c will be non-null if static
                          #_"Expr" instance (when (nil? c) (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (second form)))
                          #_"boolean" maybeField (and (= (count form) 3) (symbol? (third form)))
                          maybeField
                            (when (and maybeField (not= (.charAt (:name (third form)), 0) \-)) => maybeField
                                (let [#_"String" name (:name (third form))]
                                    (cond
                                        (some? c)
                                            (zero? (count (Reflector'getMethods c, 0, (Compiler'munge name), true)))
                                        (and (some? instance) (.hasJavaClass instance) (some? (.getJavaClass instance)))
                                            (zero? (count (Reflector'getMethods (.getJavaClass instance), 0, (Compiler'munge name), false)))
                                        :else
                                            maybeField
                                    )
                                )
                            )]
                        (if maybeField
                            (let [? (= (.charAt (:name (third form)), 0) \-)
                                  #_"Symbol" sym (if ? (symbol (.substring (:name (third form)), 1)) (third form))
                                  #_"Symbol" tag (Compiler'tagOf form)]
                                (if (some? c)
                                    (StaticFieldExpr'new line, c, (Compiler'munge (:name sym)), tag)
                                    (InstanceFieldExpr'new line, instance, (Compiler'munge (:name sym)), tag, ?)
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
                                            (StaticMethodExpr'new line, tag, c, (Compiler'munge (:name sym)), args, tailPosition)
                                            (InstanceMethodExpr'new line, tag, instance, (Compiler'munge (:name sym)), args, tailPosition)
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
        (hash-map
            #_"Symbol" :symbol symbol
        )
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--UnresolvedVarExpr [#_"UnresolvedVarExpr" this]
        false
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--UnresolvedVarExpr [#_"UnresolvedVarExpr" this]
        (throw (IllegalArgumentException. "UnresolvedVarExpr has no Java class"))
    )

    #_override
    (defn #_"void" Expr'''emit--UnresolvedVarExpr [#_"UnresolvedVarExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        nil
    )

    #_override
    (defn #_"Object" Expr'''eval--UnresolvedVarExpr [#_"UnresolvedVarExpr" this]
        (throw (IllegalArgumentException. "UnresolvedVarExpr cannot be evalled"))
    )
)

(class-ns VarExpr
    (defn #_"VarExpr" VarExpr'new [#_"Var" var, #_"Symbol" tag]
        (hash-map
            #_"Var" :var var
            #_"Object" :tag (or tag (get (meta var) :tag))
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--VarExpr [#_"VarExpr" this]
        (deref (:var this))
    )

    (declare ObjExpr''emitVarValue)

    #_override
    (defn #_"void" Expr'''emit--VarExpr [#_"VarExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (ObjExpr''emitVarValue objx, gen, (:var this))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--VarExpr [#_"VarExpr" this]
        (some? (:tag this))
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--VarExpr [#_"VarExpr" this]
        (HostExpr'tagToClass (:tag this))
    )

    #_override
    (defn #_"Object" AssignableExpr'''evalAssign--VarExpr [#_"VarExpr" this, #_"Expr" val]
        (var-set (:var this) (.eval val))
    )

    (declare ObjExpr''emitVar)

    #_override
    (defn #_"void" AssignableExpr'''emitAssign--VarExpr [#_"VarExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Expr" val]
        (ObjExpr''emitVar objx, gen, (:var this))
        (.emit val, :Context'EXPRESSION, objx, gen)
        (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "Object set(Object)"))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )
)

(class-ns TheVarExpr
    (defn #_"TheVarExpr" TheVarExpr'new [#_"Var" var]
        (hash-map
            #_"Var" :var var
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--TheVarExpr [#_"TheVarExpr" this]
        (:var this)
    )

    #_override
    (defn #_"void" Expr'''emit--TheVarExpr [#_"TheVarExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (ObjExpr''emitVar objx, gen, (:var this))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--TheVarExpr [#_"TheVarExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--TheVarExpr [#_"TheVarExpr" this]
        Var
    )
)

(class-ns TheVarParser
    (defn #_"IParser" TheVarParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
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
        (hash-map
            #_"PersistentVector" :exprs exprs
        )
    )

    #_method
    (defn- #_"Expr" BodyExpr''lastExpr [#_"BodyExpr" this]
        (nth (:exprs this) (dec (count (:exprs this))))
    )

    #_override
    (defn #_"Object" Expr'''eval--BodyExpr [#_"BodyExpr" this]
        (let [#_"Iterator" it (.iterator (:exprs this))]
            (loop-when-recur [#_"Object" ret nil] (.hasNext it) [(.eval (.next it))] => ret)
        )
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--BodyExpr [#_"BodyExpr" this]
        (let [#_"Expr" e (BodyExpr''lastExpr this)]
            (and (instance? MaybePrimitiveExpr e) (.canEmitPrimitive e))
        )
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--BodyExpr [#_"BodyExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (dotimes [#_"int" i (dec (count (:exprs this)))]
            (.emit (nth (:exprs this) i), :Context'STATEMENT, objx, gen)
        )
        (.emitUnboxed (BodyExpr''lastExpr this), context, objx, gen)
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--BodyExpr [#_"BodyExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (dotimes [#_"int" i (dec (count (:exprs this)))]
            (.emit (nth (:exprs this) i), :Context'STATEMENT, objx, gen)
        )
        (.emit (BodyExpr''lastExpr this), context, objx, gen)
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--BodyExpr [#_"BodyExpr" this]
        (.hasJavaClass (BodyExpr''lastExpr this))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--BodyExpr [#_"BodyExpr" this]
        (.getJavaClass (BodyExpr''lastExpr this))
    )
)

(class-ns BodyParser
    (defn #_"IParser" BodyParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
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
        (hash-map
            #_"Class" :c c
            #_"LocalBinding" :lb lb
            #_"Expr" :handler handler

            #_mutable #_"Label" :label nil
            #_mutable #_"Label" :endLabel nil
        )
    )
)

(class-ns TryExpr
    (defn #_"TryExpr" TryExpr'new [#_"Expr" tryExpr, #_"PersistentVector" catchExprs, #_"Expr" finallyExpr, #_"int" retLocal, #_"int" finallyLocal]
        (hash-map
            #_"Expr" :tryExpr tryExpr
            #_"PersistentVector" :catchExprs catchExprs
            #_"Expr" :finallyExpr finallyExpr
            #_"int" :retLocal retLocal
            #_"int" :finallyLocal finallyLocal
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--TryExpr [#_"TryExpr" this]
        (throw (UnsupportedOperationException. "Can't eval try"))
    )

    #_override
    (defn #_"void" Expr'''emit--TryExpr [#_"TryExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (let [#_"Label" startTry (.newLabel gen) #_"Label" endTry (.newLabel gen) #_"Label" end (.newLabel gen) #_"Label" ret (.newLabel gen) #_"Label" finallyLabel (.newLabel gen)]
            (loop-when-recur [#_"int" i 0] (< i (count (:catchExprs this))) [(inc i)]
                (let [#_"CatchClause" clause (nth (:catchExprs this) i)]
                    (§ set! (:label clause) (.newLabel gen))
                    (§ set! (:endLabel clause) (.newLabel gen))
                )
            )

            (.mark gen, startTry)
            (.emit (:tryExpr this), context, objx, gen)
            (when (not= context :Context'STATEMENT)
                (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:retLocal this))
            )
            (.mark gen, endTry)
            (when (some? (:finallyExpr this))
                (.emit (:finallyExpr this), :Context'STATEMENT, objx, gen)
            )
            (.goTo gen, ret)

            (dotimes [#_"int" i (count (:catchExprs this))]
                (let [#_"CatchClause" clause (nth (:catchExprs this) i)]
                    (.mark gen, (:label clause))
                    ;; exception should be on stack
                    ;; put in clause local
                    (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx (:lb clause)))
                    (.emit (:handler clause), context, objx, gen)
                    (when (not= context :Context'STATEMENT)
                        (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:retLocal this))
                    )
                    (.mark gen, (:endLabel clause))

                    (when (some? (:finallyExpr this))
                        (.emit (:finallyExpr this), :Context'STATEMENT, objx, gen)
                    )
                    (.goTo gen, ret)
                )
            )
            (when (some? (:finallyExpr this))
                (.mark gen, finallyLabel)
                ;; exception should be on stack
                (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:finallyLocal this))
                (.emit (:finallyExpr this), :Context'STATEMENT, objx, gen)
                (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), (:finallyLocal this))
                (.throwException gen)
            )
            (.mark gen, ret)
            (when (not= context :Context'STATEMENT)
                (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), (:retLocal this))
            )
            (.mark gen, end)
            (dotimes [#_"int" i (count (:catchExprs this))]
                (let [#_"CatchClause" clause (nth (:catchExprs this) i)]
                    (.visitTryCatchBlock gen, startTry, endTry, (:label clause), (.replace (.getName (:c clause)), \., \/))
                )
            )
            (when (some? (:finallyExpr this))
                (.visitTryCatchBlock gen, startTry, endTry, finallyLabel, nil)
                (dotimes [#_"int" i (count (:catchExprs this))]
                    (let [#_"CatchClause" clause (nth (:catchExprs this) i)]
                        (.visitTryCatchBlock gen, (:label clause), (:endLabel clause), finallyLabel, nil)
                    )
                )
            )
            (dotimes [#_"int" i (count (:catchExprs this))]
                (let [#_"CatchClause" clause (nth (:catchExprs this) i)]
                    (.visitLocalVariable gen, (:name (:lb clause)), "Ljava/lang/Object;", nil, (:label clause), (:endLabel clause), (:idx (:lb clause)))
                )
            )
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--TryExpr [#_"TryExpr" this]
        (.hasJavaClass (:tryExpr this))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--TryExpr [#_"TryExpr" this]
        (.getJavaClass (:tryExpr this))
    )
)

(class-ns TryParser
    (defn #_"IParser" TryParser'new []
        (reify IParser
            ;; (try try-expr* catch-expr* finally-expr?)
            ;; catch-expr: (catch class sym expr*)
            ;; finally-expr: (finally expr*)
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (= context :Context'RETURN) => (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                    (let [[#_"Expr" bodyExpr #_"PersistentVector" catches #_"Expr" finallyExpr #_"PersistentVector" body]
                            (loop-when [bodyExpr nil catches [] finallyExpr nil body [] #_"boolean" caught? false #_"ISeq" fs (next form)] (some? fs) => [bodyExpr catches finallyExpr body]
                                (let [#_"Object" f (first fs) #_"Object" op (when (instance? ISeq f) (first f))]
                                    (if (any = op 'catch 'finally)
                                        (let [bodyExpr
                                                (when (nil? bodyExpr) => bodyExpr
                                                    (binding [*no-recur* true, *in-return-context* false]
                                                        (.parse (BodyParser'new), context, (seq body))
                                                    )
                                                )]
                                            (if (= op 'catch)
                                                (let-when [#_"Class" c (HostExpr'maybeClass (second f), false)] (some? c) => (throw (IllegalArgumentException. (str "Unable to resolve classname: " (second f))))
                                                    (let-when [#_"Symbol" sym (third f)] (symbol? sym) => (throw (IllegalArgumentException. (str "Bad binding form, expected symbol, got: " sym)))
                                                        (when (nil? (namespace sym)) => (throw (RuntimeException. (str "Can't bind qualified name: " sym)))
                                                            (binding [*local-env* *local-env*, *next-local-num* *next-local-num*, *in-catch-finally* true]
                                                                (let [#_"LocalBinding" lb (Compiler'registerLocal sym, (when (symbol? (second f)) (second f)), nil, false)
                                                                    #_"Expr" handler (.parse (BodyParser'new), :Context'EXPRESSION, (next (next (next f))))]
                                                                    (ß ass catches (conj catches (CatchClause'new c, lb, handler)))
                                                                )
                                                            )
                                                            (recur bodyExpr catches finallyExpr body true (next fs))
                                                        )
                                                    )
                                                )
                                                (when (nil? (next fs)) => (throw (RuntimeException. "finally clause must be last in try expression"))
                                                    (binding [*in-catch-finally* true]
                                                        (ß ass finallyExpr (.parse (BodyParser'new), :Context'STATEMENT, (next f)))
                                                    )
                                                    (recur bodyExpr catches finallyExpr body caught? (next fs))
                                                )
                                            )
                                        )
                                        (when-not caught? => (throw (RuntimeException. "Only catch or finally clause can follow catch in try expression"))
                                            (recur bodyExpr catches finallyExpr (conj body f) caught? (next fs))
                                        )
                                    )
                                )
                            )]
                        (when (nil? bodyExpr) => (TryExpr'new bodyExpr, catches, finallyExpr, (Compiler'getAndIncLocalNum), (Compiler'getAndIncLocalNum))
                            ;; when there is neither catch nor finally, e.g. (try (expr)) return a body expr directly
                            (binding [*no-recur* true]
                                (.parse (BodyParser'new), context, (seq body))
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
        (merge (UntypedExpr'new)
            (hash-map
                #_"Expr" :excExpr excExpr
            )
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--ThrowExpr [#_"ThrowExpr" this]
        (throw (RuntimeException. "Can't eval throw"))
    )

    #_override
    (defn #_"void" Expr'''emit--ThrowExpr [#_"ThrowExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emit (:excExpr this), :Context'EXPRESSION, objx, gen)
        (.checkCast gen, (Type/getType Throwable))
        (.throwException gen)
        nil
    )
)

(class-ns ThrowParser
    (defn #_"IParser" ThrowParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
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
                        (loop-when [ctors [] pars [] rets [] #_"int" i 0] [< i (alength allctors)] => [ctors pars rets]
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
            (hash-map
                #_"IPersistentVector" :args args
                #_"Constructor" :ctor ctor
                #_"Class" :c c
            )
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--NewExpr [#_"NewExpr" this]
        (let [#_"Object[]" args (make-array Object (count (:args this)))]
            (dotimes [#_"int" i (count (:args this))]
                (aset args i (.eval (nth (:args this) i)))
            )
            (when (some? (:ctor this)) => (Reflector'invokeConstructor (:c this), args)
                (.newInstance (:ctor this), (Reflector'boxArgs (.getParameterTypes (:ctor this)), args))
            )
        )
    )

    #_override
    (defn #_"void" Expr'''emit--NewExpr [#_"NewExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--NewExpr [#_"NewExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--NewExpr [#_"NewExpr" this]
        (:c this)
    )
)

(class-ns NewParser
    (defn #_"IParser" NewParser'new []
        (reify IParser
            ;; (new Classname args...)
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"int" line *line*]
                    (when (< 1 (count form)) => (throw (IllegalArgumentException. "Wrong number of arguments, expecting: (new Classname args...)"))
                        (let [#_"Class" c (HostExpr'maybeClass (second form), false)]
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
        (hash-map
            #_"Expr" :expr expr
            #_"Expr" :meta meta
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--MetaExpr [#_"MetaExpr" this]
        (with-meta (.eval (:expr this)) (.eval (:meta this)))
    )

    #_override
    (defn #_"void" Expr'''emit--MetaExpr [#_"MetaExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emit (:expr this), :Context'EXPRESSION, objx, gen)
        (.checkCast gen, (Type/getType IObj))
        (.emit (:meta this), :Context'EXPRESSION, objx, gen)
        (.checkCast gen, (Type/getType IPersistentMap))
        (.invokeInterface gen, (Type/getType IObj), (Method/getMethod "clojure.lang.IObj withMeta(clojure.lang.IPersistentMap)"))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--MetaExpr [#_"MetaExpr" this]
        (.hasJavaClass (:expr this))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--MetaExpr [#_"MetaExpr" this]
        (.getJavaClass (:expr this))
    )
)

(class-ns IfExpr
    (defn #_"IfExpr" IfExpr'new [#_"int" line, #_"Expr" testExpr, #_"Expr" thenExpr, #_"Expr" elseExpr]
        (hash-map
            #_"int" :line line
            #_"Expr" :testExpr testExpr
            #_"Expr" :thenExpr thenExpr
            #_"Expr" :elseExpr elseExpr
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--IfExpr [#_"IfExpr" this]
        (let [#_"Object" t (.eval (:testExpr this))]
            (if (and (some? t) (not= t false))
                (.eval (:thenExpr this))
                (.eval (:elseExpr this))
            )
        )
    )

    #_method
    (defn- #_"void" IfExpr''doEmit [#_"IfExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"boolean" emitUnboxed]
        (let [#_"Label" nullLabel (.newLabel gen) #_"Label" falseLabel (.newLabel gen) #_"Label" endLabel (.newLabel gen)]
            (.visitLineNumber gen, (:line this), (.mark gen))

            (cond (and (instance? StaticMethodExpr (:testExpr this)) (StaticMethodExpr''canEmitIntrinsicPredicate (:testExpr this)))
                (do
                    (StaticMethodExpr''emitIntrinsicPredicate (:testExpr this), :Context'EXPRESSION, objx, gen, falseLabel)
                )
                (= (Compiler'maybePrimitiveType (:testExpr this)) Boolean/TYPE)
                (do
                    (.emitUnboxed (:testExpr this), :Context'EXPRESSION, objx, gen)
                    (.ifZCmp gen, GeneratorAdapter/EQ, falseLabel)
                )
                :else
                (do
                    (.emit (:testExpr this), :Context'EXPRESSION, objx, gen)
                    (.dup gen)
                    (.ifNull gen, nullLabel)
                    (.getStatic gen, (Type/getType Boolean), "FALSE", (Type/getType Boolean))
                    (.visitJumpInsn gen, Opcodes/IF_ACMPEQ, falseLabel)
                )
            )
            (if emitUnboxed
                (.emitUnboxed (:thenExpr this), context, objx, gen)
                (.emit (:thenExpr this), context, objx, gen)
            )
            (.goTo gen, endLabel)
            (.mark gen, nullLabel)
            (.pop gen)
            (.mark gen, falseLabel)
            (if emitUnboxed
                (.emitUnboxed (:elseExpr this), context, objx, gen)
                (.emit (:elseExpr this), context, objx, gen)
            )
            (.mark gen, endLabel)
        )
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--IfExpr [#_"IfExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (IfExpr''doEmit this, context, objx, gen, false)
        nil
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--IfExpr [#_"IfExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (IfExpr''doEmit this, context, objx, gen, true)
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--IfExpr [#_"IfExpr" this]
        (and (.hasJavaClass (:thenExpr this))
             (.hasJavaClass (:elseExpr this))
            (or (= (.getJavaClass (:thenExpr this)) (.getJavaClass (:elseExpr this)))
                (= (.getJavaClass (:thenExpr this)) Recur)
                (= (.getJavaClass (:elseExpr this)) Recur)
                (and (nil? (.getJavaClass (:thenExpr this))) (not (.isPrimitive (.getJavaClass (:elseExpr this)))))
                (and (nil? (.getJavaClass (:elseExpr this))) (not (.isPrimitive (.getJavaClass (:thenExpr this)))))))
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--IfExpr [#_"IfExpr" this]
        (try
            (and (instance? MaybePrimitiveExpr (:thenExpr this))
                 (instance? MaybePrimitiveExpr (:elseExpr this))
                (or (= (.getJavaClass (:thenExpr this)) (.getJavaClass (:elseExpr this)))
                    (= (.getJavaClass (:thenExpr this)) Recur)
                    (= (.getJavaClass (:elseExpr this)) Recur))
                 (.canEmitPrimitive (:thenExpr this))
                 (.canEmitPrimitive (:elseExpr this)))
            (catch Exception e
                false
            )
        )
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--IfExpr [#_"IfExpr" this]
        (let [#_"Class" thenClass (.getJavaClass (:thenExpr this))]
            (if (and (some? thenClass) (not= thenClass Recur))
                thenClass
                (.getJavaClass (:elseExpr this))
            )
        )
    )
)

(class-ns IfParser
    (defn #_"IParser" IfParser'new []
        (reify IParser
            ;; (if test then) or (if test then else)
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (cond
                    (< 4 (count form)) (throw (IllegalArgumentException. "Too many arguments to if"))
                    (< (count form) 3) (throw (IllegalArgumentException. "Too few arguments to if"))
                )
                (let [#_"PathNode" branch (PathNode'new :PathType'BRANCH, *clear-path*)
                      #_"Expr" testexpr (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (second form))
                      #_"Expr" thenexpr
                        (binding [*clear-path* (PathNode'new :PathType'PATH, branch)]
                            (Compiler'analyze context, (third form))
                        )
                      #_"Expr" elseexpr
                        (binding [*clear-path* (PathNode'new :PathType'PATH, branch)]
                            (Compiler'analyze context, (fourth form))
                        )]
                    (IfExpr'new *line*, testexpr, thenexpr, elseexpr)
                )
            )
        )
    )
)

(class-ns ListExpr
    (defn #_"ListExpr" ListExpr'new [#_"IPersistentVector" args]
        (hash-map
            #_"IPersistentVector" :args args
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--ListExpr [#_"ListExpr" this]
        (loop-when-recur [#_"IPersistentVector" v [] #_"int" i 0]
                         (< i (count (:args this)))
                         [(conj v (.eval (nth (:args this) i))) (inc i)]
                      => (seq v)
        )
    )

    #_override
    (defn #_"void" Expr'''emit--ListExpr [#_"ListExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (MethodExpr'emitArgsAsArray (:args this), objx, gen)
        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.ISeq arrayToList(Object[])"))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--ListExpr [#_"ListExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--ListExpr [#_"ListExpr" this]
        IPersistentList
    )
)

(class-ns MapExpr
    (defn #_"MapExpr" MapExpr'new [#_"IPersistentVector" keyvals]
        (hash-map
            #_"IPersistentVector" :keyvals keyvals
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--MapExpr [#_"MapExpr" this]
        (let [#_"Object[]" a (make-array Object (count (:keyvals this)))]
            (dotimes [#_"int" i (count (:keyvals this))]
                (aset a i (.eval (nth (:keyvals this) i)))
            )
            (RT/map a)
        )
    )

    #_override
    (defn #_"void" Expr'''emit--MapExpr [#_"MapExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (let [[#_"boolean" allKeysConstant #_"boolean" allConstantKeysUnique]
                (loop-when [constant? true unique? true #_"IPersistentSet" keys #{} #_"int" i 0] (< i (count (:keyvals this))) => [constant? unique?]
                    (let [#_"Expr" k (nth (:keyvals this) i)
                          [constant? unique? keys]
                            (when (instance? LiteralExpr k) => [false unique? keys]
                                (let-when-not [#_"Object" v (.eval k)] (contains? keys v) => [constant? false keys]
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--MapExpr [#_"MapExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--MapExpr [#_"MapExpr" this]
        IPersistentMap
    )

    (defn #_"Expr" MapExpr'parse [#_"Context" context, #_"IPersistentMap" form]
        (let [#_"IPersistentVector" keyvals []
              #_"boolean" keysConstant true #_"boolean" valsConstant true #_"boolean" allConstantKeysUnique true
              #_"IPersistentSet" constantKeys #{}]
            (loop-when-recur [#_"ISeq" s (seq form)] (some? s) [(next s)]
                (let [#_"Context" c (if (= context :Context'EVAL) context :Context'EXPRESSION)
                      #_"IMapEntry" e (first s) #_"Expr" k (Compiler'analyze c, (key e)) #_"Expr" v (Compiler'analyze c, (val e))]
                    (ß ass keyvals (conj keyvals k v))
                    (if (instance? LiteralExpr k)
                        (let [#_"Object" kval (.eval k)]
                            (if (contains? constantKeys kval)
                                (ß ass allConstantKeysUnique false)
                                (ß ass constantKeys (conj constantKeys kval))
                            )
                        )
                        (ß ass keysConstant false)
                    )
                    (when (not (instance? LiteralExpr v))
                        (ß ass valsConstant false)
                    )
                )
            )

            (let [#_"Expr" e (MapExpr'new keyvals)]
                (cond
                    (and (instance? IObj form) (some? (meta form)))
                        (MetaExpr'new e, (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), (meta form)))
                    keysConstant
                        ;; TBD: Add more detail to exception thrown below.
                        (when allConstantKeysUnique => (throw (IllegalArgumentException. "Duplicate constant keys in map"))
                            (when valsConstant => e
                                (loop-when-recur [#_"IPersistentMap" m {} #_"int" i 0]
                                                 (< i (count keyvals))
                                                 [(assoc m (.literal (nth keyvals i)) (.literal (nth keyvals (inc i)))) (+ i 2)]
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
)

(class-ns SetExpr
    (defn #_"SetExpr" SetExpr'new [#_"IPersistentVector" keys]
        (hash-map
            #_"IPersistentVector" :keys keys
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--SetExpr [#_"SetExpr" this]
        (let [#_"Object[]" a (make-array Object (count (:keys this)))]
            (dotimes [#_"int" i (count (:keys this))]
                (aset a i (.eval (nth (:keys this) i)))
            )
            (RT/set a)
        )
    )

    #_override
    (defn #_"void" Expr'''emit--SetExpr [#_"SetExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (MethodExpr'emitArgsAsArray (:keys this), objx, gen)
        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.IPersistentSet set(Object[])"))
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--SetExpr [#_"SetExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--SetExpr [#_"SetExpr" this]
        IPersistentSet
    )

    (defn #_"Expr" SetExpr'parse [#_"Context" context, #_"IPersistentSet" form]
        (let [[#_"IPersistentVector" keys #_"boolean" constant?]
                (loop-when [keys [] constant? true #_"ISeq" s (seq form)] (some? s) => [keys constant?]
                    (let [#_"Expr" e (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (first s))]
                        (recur (conj keys e) (and constant? (instance? LiteralExpr e)) (next s))
                    )
                )]
            (cond
                (and (instance? IObj form) (some? (meta form)))
                    (MetaExpr'new (SetExpr'new keys), (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), (meta form)))
                constant?
                    (loop-when-recur [#_"IPersistentSet" s #{} #_"int" i 0]
                                     (< i (count keys))
                                     [(conj s (.literal (nth keys i))) (inc i)]
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
        (hash-map
            #_"IPersistentVector" :args args
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--VectorExpr [#_"VectorExpr" this]
        (loop-when-recur [#_"IPersistentVector" v [] #_"int" i 0]
                         (< i (count (:args this)))
                         [(conj v (.eval (nth (:args this) i))) (inc i)]
                      => v
        )
    )

    #_override
    (defn #_"void" Expr'''emit--VectorExpr [#_"VectorExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (if (<= (count (:args this)) Tuple'MAX_SIZE)
            (do
                (dotimes [#_"int" i (count (:args this))]
                    (.emit (nth (:args this) i), :Context'EXPRESSION, objx, gen)
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--VectorExpr [#_"VectorExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--VectorExpr [#_"VectorExpr" this]
        IPersistentVector
    )

    (defn #_"Expr" VectorExpr'parse [#_"Context" context, #_"IPersistentVector" form]
        (let [[#_"IPersistentVector" args #_"boolean" constant?]
                (loop-when [args [] constant? true #_"int" i 0] (< i (count form)) => [args constant?]
                    (let [#_"Expr" e (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (nth form i))]
                        (recur (conj args e) (and constant? (instance? LiteralExpr e)) (inc i))
                    )
                )]
            (cond
                (and (instance? IObj form) (some? (meta form)))
                    (MetaExpr'new (VectorExpr'new args), (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), (meta form)))
                constant?
                    (loop-when-recur [#_"IPersistentVector" v [] #_"int" i 0]
                                     (< i (count args))
                                     [(conj v (.literal (nth args i))) (inc i)]
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
        (hash-map
            #_"int" :line line
            #_"Object" :tag tag
            #_"KeywordExpr" :kw kw
            #_"Expr" :target target

            #_"int" :siteIndex (Compiler'registerKeywordCallsite (:k kw))
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--KeywordInvokeExpr [#_"KeywordInvokeExpr" this]
        (.invoke (:k (:kw this)), (.eval (:target this)))
    )

    #_override
    (defn #_"void" Expr'''emit--KeywordInvokeExpr [#_"KeywordInvokeExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (let [#_"Label" endLabel (.newLabel gen) #_"Label" faultLabel (.newLabel gen)]
            (.visitLineNumber gen, (:line this), (.mark gen))
            (.getStatic gen, (:objtype objx), (Compiler'thunkNameStatic (:siteIndex this)), (Type/getType ILookupThunk))
            (.dup gen) ;; thunk, thunk
            (.emit (:target this), :Context'EXPRESSION, objx, gen) ;; thunk, thunk, target
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
            (.getStatic gen, (:objtype objx), (Compiler'siteNameStatic (:siteIndex this)), (Type/getType KeywordLookupSite)) ;; target, target, site
            (.swap gen) ;; target, site, target
            (.invokeInterface gen, (Type/getType ILookupSite), (Method/getMethod "clojure.lang.ILookupThunk fault(Object)")) ;; target, new-thunk
            (.dup gen) ;; target, new-thunk, new-thunk
            (.putStatic gen, (:objtype objx), (Compiler'thunkNameStatic (:siteIndex this)), (Type/getType ILookupThunk)) ;; target, new-thunk
            (.swap gen) ;; new-thunk, target
            (.invokeInterface gen, (Type/getType ILookupThunk), (Method/getMethod "Object get(Object)")) ;; result

            (.mark gen, endLabel)
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--KeywordInvokeExpr [#_"KeywordInvokeExpr" this]
        (some? (:tag this))
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--KeywordInvokeExpr [#_"KeywordInvokeExpr" this]
        (HostExpr'tagToClass (:tag this))
    )
)

(class-ns InstanceOfExpr
    (defn #_"InstanceOfExpr" InstanceOfExpr'new [#_"Class" c, #_"Expr" expr]
        (hash-map
            #_"Class" :c c
            #_"Expr" :expr expr
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--InstanceOfExpr [#_"InstanceOfExpr" this]
        (if (.isInstance (:c this), (.eval (:expr this))) true false)
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--InstanceOfExpr [#_"InstanceOfExpr" this]
        true
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--InstanceOfExpr [#_"InstanceOfExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emit (:expr this), :Context'EXPRESSION, objx, gen)
        (.instanceOf gen, (Compiler'getType (:c this)))
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--InstanceOfExpr [#_"InstanceOfExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emitUnboxed this, context, objx, gen)
        (HostExpr'emitBoxReturn objx, gen, Boolean/TYPE)
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--InstanceOfExpr [#_"InstanceOfExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--InstanceOfExpr [#_"InstanceOfExpr" this]
        Boolean/TYPE
    )
)

(class-ns StaticInvokeExpr
    (defn #_"StaticInvokeExpr" StaticInvokeExpr'new [#_"Type" target, #_"Class" retClass, #_"Class[]" paramclasses, #_"Type[]" paramtypes, #_"boolean" variadic, #_"IPersistentVector" args, #_"Object" tag, #_"boolean" tailPosition]
        (hash-map
            #_"Type" :target target
            #_"Class" :retClass retClass
            #_"Class[]" :paramclasses paramclasses
            #_"Type[]" :paramtypes paramtypes
            #_"boolean" :variadic variadic
            #_"IPersistentVector" :args args
            #_"Object" :tag tag
            #_"boolean" :tailPosition tailPosition
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--StaticInvokeExpr [#_"StaticInvokeExpr" this]
        (throw (UnsupportedOperationException. "Can't eval StaticInvokeExpr"))
    )

    #_override
    (defn #_"void" Expr'''emit--StaticInvokeExpr [#_"StaticInvokeExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emitUnboxed this, context, objx, gen)
        (when (not= context :Context'STATEMENT)
            (HostExpr'emitBoxReturn objx, gen, (:retClass this))
        )
        (when (= context :Context'STATEMENT)
            (if (or (= (:retClass this) Long/TYPE) (= (:retClass this) Double/TYPE))
                (.pop2 gen)
                (.pop gen)
            )
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--StaticInvokeExpr [#_"StaticInvokeExpr" this]
        true
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--StaticInvokeExpr [#_"StaticInvokeExpr" this]
        (Compiler'retType (when (some? (:tag this)) (HostExpr'tagToClass (:tag this))), (:retClass this))
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--StaticInvokeExpr [#_"StaticInvokeExpr" this]
        (.isPrimitive (:retClass this))
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--StaticInvokeExpr [#_"StaticInvokeExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (let [#_"Method" ms (Method. "invokeStatic", (Type/getType (:retClass this)), (:paramtypes this))]
            (when (:variadic this) => (MethodExpr'emitTypedArgs objx, gen, (:paramclasses this), (:args this))
                (dotimes [#_"int" i (dec (alength (:paramclasses this)))]
                    (let [#_"Expr" e (nth (:args this) i)]
                        (if (= (Compiler'maybePrimitiveType e) (aget (:paramclasses this) i))
                            (do
                                (.emitUnboxed e, :Context'EXPRESSION, objx, gen)
                            )
                            (do
                                (.emit e, :Context'EXPRESSION, objx, gen)
                                (HostExpr'emitUnboxArg objx, gen, (aget (:paramclasses this) i))
                            )
                        )
                    )
                )
                (let [#_"IPersistentVector" restArgs (subvec (:args this) (dec (alength (:paramclasses this))) (count (:args this)))]
                    (MethodExpr'emitArgsAsArray restArgs, objx, gen)
                    (.invokeStatic gen, (Type/getType ArraySeq), (Method/getMethod "clojure.lang.ArraySeq create(Object[])"))
                )
            )

            (when (and (:tailPosition this) (not (:canBeDirect objx)))
                (ObjMethod''emitClearThis *method*, gen)
            )

            (.invokeStatic gen, (:target this), ms)
        )
        nil
    )

    (defn #_"Expr" StaticInvokeExpr'parse [#_"Var" v, #_"ISeq" args, #_"Object" tag, #_"boolean" tailPosition]
        (when (and (bound? v) (some? (var-get v)))
            (let [#_"Class" c (.getClass (var-get v)) #_"java.lang.reflect.Method[]" methods (.getMethods c) #_"int" argc (count args)
                  [#_"java.lang.reflect.Method" method #_"boolean" variadic]
                    (loop-when [#_"int" i 0] (< i (alength methods)) => [nil false]
                        (let [#_"java.lang.reflect.Method" m (aget methods i)]
                            (or
                                (when (and (Modifier/isStatic (.getModifiers m)) (= (.getName m) "invokeStatic"))
                                    (let [#_"Class[]" types (.getParameterTypes m) #_"int" n (alength types)]
                                        (cond
                                            (= n argc)
                                                [m (and (pos? n) (= (aget types (dec n)) ISeq))]
                                            (and (< 0 n argc) (= (aget types (dec n)) ISeq))
                                                [m true]
                                        )
                                    )
                                )
                                (recur (inc i))
                            )
                        )
                    )]
                (when (some? method)
                    (let [#_"Class" retClass (.getReturnType method) #_"Class[]" paramClasses (.getParameterTypes method)
                          #_"Type[]" paramTypes (make-array Type (alength paramClasses))
                          _ (dotimes [#_"int" i (alength paramClasses)]
                                (aset paramTypes i (Type/getType (aget paramClasses i)))
                            )
                          #_"Type" target (Type/getType c)
                          #_"PersistentVector" argv
                            (loop-when-recur [argv [] #_"ISeq" s (seq args)]
                                             (some? s)
                                             [(conj argv (Compiler'analyze :Context'EXPRESSION, (first s))) (next s)]
                                          => argv
                            )]
                        (StaticInvokeExpr'new target, retClass, paramClasses, paramTypes, variadic, argv, tag, tailPosition)
                    )
                )
            )
        )
    )
)

(class-ns InvokeExpr
    (defn #_"InvokeExpr" InvokeExpr'new [#_"int" line, #_"Symbol" tag, #_"Expr" fexpr, #_"IPersistentVector" args, #_"boolean" tailPosition]
        (let [this
                (hash-map
                    #_"Expr" :fexpr fexpr
                    #_"Object" :tag (or tag (when (instance? VarExpr fexpr) (:tag fexpr)))
                    #_"IPersistentVector" :args args
                    #_"int" :line line
                    #_"boolean" :tailPosition tailPosition

                    #_"boolean" :isProtocol false
                    #_"boolean" :isDirect false
                    #_"int" :siteIndex -1
                    #_"Class" :protocolOn nil
                    #_"java.lang.reflect.Method" :onMethod nil
                )]
            (when (instance? VarExpr fexpr) => this
                (let [#_"Var" fvar (:var fexpr) #_"Var" pvar (get (meta fvar) :protocol)]
                    (when (and (some? pvar) (bound? #'*protocol-callsites*)) => this
                        (let [this (assoc this :isProtocol true)
                              this (assoc this :siteIndex (Compiler'registerProtocolCallsite (:var fexpr)))
                              this (assoc this :protocolOn (HostExpr'maybeClass (get (var-get pvar) :on), false))]
                            (when (some? (:protocolOn this)) => this
                                (let [#_"IPersistentMap" mmap (get (var-get pvar) :method-map)
                                      #_"Keyword" mmapVal (get mmap (keyword (:sym fvar)))]
                                    (when (some? mmapVal) => (throw (IllegalArgumentException. (str "No method of interface: " (.getName (:protocolOn this)) " found for function: " (:sym fvar) " of protocol: " (:sym pvar) " (The protocol method may have been defined before and removed.)")))
                                        (let [#_"String" mname (Compiler'munge (.toString (:sym mmapVal)))
                                              #_"PersistentVector" methods (Reflector'getMethods (:protocolOn this), (dec (count args)), mname, false)]
                                            (when (= (count methods) 1) => (throw (IllegalArgumentException. (str "No single method: " mname " of interface: " (.getName (:protocolOn this)) " found for function: " (:sym fvar) " of protocol: " (:sym pvar))))
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

    #_override
    (defn #_"Object" Expr'''eval--InvokeExpr [#_"InvokeExpr" this]
        (let [#_"IFn" fn (.eval (:fexpr this))
              #_"PersistentVector" v (loop-when-recur [v [] #_"int" i 0] (< i (count (:args this))) [(conj v (.eval (nth (:args this) i))) (inc i)] => v)]
            (.applyTo fn, (seq v))
        )
    )

    #_method
    (defn #_"void" InvokeExpr''emitArgsAndCall [#_"InvokeExpr" this, #_"int" firstArgToEmit, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (loop-when-recur [#_"int" i firstArgToEmit] (< i (Math/min Compiler'MAX_POSITIONAL_ARITY, (count (:args this)))) [(inc i)]
            (.emit (nth (:args this) i), :Context'EXPRESSION, objx, gen)
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

        (when (and (:tailPosition this) (not (:canBeDirect objx)))
            (ObjMethod''emitClearThis *method*, gen)
        )

        (.invokeInterface gen, (Type/getType IFn), (Method. "invoke", (Type/getType Object), (aget Compiler'ARG_TYPES (Math/min (inc Compiler'MAX_POSITIONAL_ARITY), (count (:args this))))))
        nil
    )

    #_method
    (defn #_"void" InvokeExpr''emitProto [#_"InvokeExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (let [#_"Label" onLabel (.newLabel gen) #_"Label" callLabel (.newLabel gen) #_"Label" endLabel (.newLabel gen)]
            (.emit (nth (:args this) 0), :Context'EXPRESSION, objx, gen)
            (.dup gen) ;; target, target
            (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Class classOf(Object)")) ;; target, class
            (.getStatic gen, (:objtype objx), (Compiler'cachedClassName (:siteIndex this)), (Type/getType Class)) ;; target, class, cached-class
            (.visitJumpInsn gen, Opcodes/IF_ACMPEQ, callLabel) ;; target
            (when (some? (:protocolOn this))
                (.dup gen) ;; target, target
                (.instanceOf gen, (Type/getType (:protocolOn this)))
                (.ifZCmp gen, GeneratorAdapter/NE, onLabel)
            )
            (.dup gen) ;; target, target
            (.invokeStatic gen, (Type/getType Reflector), (Method/getMethod "Class classOf(Object)")) ;; target, class
            (.putStatic gen, (:objtype objx), (Compiler'cachedClassName (:siteIndex this)), (Type/getType Class)) ;; target
            (.mark gen, callLabel) ;; target
            (ObjExpr''emitVar objx, gen, (:var (:fexpr this)))
            (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "Object getRawRoot()")) ;; target, proto-fn
            (.swap gen)
            (InvokeExpr''emitArgsAndCall this, 1, context, objx, gen)
            (.goTo gen, endLabel)
            (.mark gen, onLabel) ;; target
            (when (some? (:protocolOn this))
                (.checkCast gen, (Type/getType (:protocolOn this)))
                (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:onMethod this)), (subvec (:args this) 1 (count (:args this))))
                (when (= context :Context'RETURN)
                    (ObjMethod''emitClearLocals *method*, gen)
                )
                (let [#_"Method" m (Method. (.getName (:onMethod this)), (Type/getReturnType (:onMethod this)), (Type/getArgumentTypes (:onMethod this)))]
                    (.invokeInterface gen, (Type/getType (:protocolOn this)), m)
                    (HostExpr'emitBoxReturn objx, gen, (.getReturnType (:onMethod this)))
                )
            )
            (.mark gen, endLabel)
        )
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--InvokeExpr [#_"InvokeExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (if (:isProtocol this)
            (do
                (.visitLineNumber gen, (:line this), (.mark gen))
                (InvokeExpr''emitProto this, context, objx, gen)
            )
            (do
                (.emit (:fexpr this), :Context'EXPRESSION, objx, gen)
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--InvokeExpr [#_"InvokeExpr" this]
        (some? (:tag this))
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--InvokeExpr [#_"InvokeExpr" this]
        (HostExpr'tagToClass (:tag this))
    )

    (defn #_"Expr" InvokeExpr'parse [#_"Context" context, #_"ISeq" form]
        (let [#_"boolean" tailPosition (Compiler'inTailCall context) context (if (= context :Context'EVAL) context :Context'EXPRESSION)
              #_"Expr" fexpr (Compiler'analyze context, (first form))]
            (or
                (when (and (instance? VarExpr fexpr) (= (:var fexpr) #'instance?) (= (count form) 3))
                    (let-when [#_"Expr" sexpr (Compiler'analyze :Context'EXPRESSION, (second form))] (instance? ConstantExpr sexpr)
                        (let-when [#_"Object" val (.literal sexpr)] (instance? Class val)
                            (InstanceOfExpr'new val, (Compiler'analyze context, (third form)))
                        )
                    )
                )

                (when (and #_"direct-linking" false (instance? VarExpr fexpr) (not= context :Context'EVAL))
                    (let [#_"Var" v (:var fexpr)]
                        (when-not (or (.isDynamic v) (get (meta v) :redef))
                            (let [#_"Symbol" formtag (Compiler'tagOf form) #_"Object" vtag (get (meta v) :tag)]
                                (StaticInvokeExpr'parse v, (next form), (or formtag vtag), tailPosition)
                            )
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
    (defn #_"LocalBinding" LocalBinding'new [#_"int" idx, #_"Symbol" sym, #_"Symbol" tag, #_"Expr" init, #_"boolean" isArg, #_"PathNode" clearPathRoot]
        (when (and (some? (Compiler'maybePrimitiveType init)) (some? tag))
            (throw (UnsupportedOperationException. "Can't type hint a local with a primitive initializer"))
        )
        (hash-map
            #_mutable #_"int" :idx idx
            #_"Symbol" :sym sym
            #_"Symbol" :tag tag
            #_mutable #_"Expr" :init init
            #_"boolean" :isArg isArg
            #_"PathNode" :clearPathRoot clearPathRoot

            #_"String" :name (Compiler'munge (:name sym))
            #_mutable #_"boolean" :canBeCleared true
            #_mutable #_"boolean" :recurMistmatch false
            #_mutable #_"boolean" :used false
        )
    )

    #_memoize!
    #_override
    (defn #_"boolean" Expr'''hasJavaClass--LocalBinding [#_"LocalBinding" this]
        (let [? (and (some? (:init this)) (.hasJavaClass (:init this)))]
            (if (and ? (Reflector'isPrimitive (.getJavaClass (:init this))) (not (instance? MaybePrimitiveExpr (:init this))))
                false
                (or (some? (:tag this)) ?)
            )
        )
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--LocalBinding [#_"LocalBinding" this]
        (if (some? (:tag this)) (HostExpr'tagToClass (:tag this)) (.getJavaClass (:init this)))
    )

    #_method
    (defn #_"Class" LocalBinding''getPrimitiveType [#_"LocalBinding" this]
        (Compiler'maybePrimitiveType (:init this))
    )
)

(class-ns LocalBindingExpr
    (defn #_"LocalBindingExpr" LocalBindingExpr'new [#_"LocalBinding" lb, #_"Symbol" tag]
        (when (or (nil? (LocalBinding''getPrimitiveType lb)) (nil? tag)) => (throw (UnsupportedOperationException. "Can't type hint a primitive local"))
            (let [this
                    (hash-map
                        #_"LocalBinding" :lb lb
                        #_"Symbol" :tag tag

                        #_"PathNode" :clearPath *clear-path*
                        #_"PathNode" :clearRoot *clear-root*
                        #_mutable #_"boolean" :shouldClear false
                    )]
                (§ set! (:used lb) true)
                (when (pos? (:idx lb)) => this
                    (let [#_"IPersistentCollection" sites (get *clear-sites* lb)]
                        (when (some? sites)
                            (loop-when-recur [#_"ISeq" s (seq sites)] (some? s) [(next s)]
                                (let [#_"LocalBindingExpr" lbe (first s)
                                      #_"PathNode" common (Compiler'commonPath (:clearPath this), (:clearPath lbe))]
                                    (when (and (some? common) (= (:type common) :PathType'PATH))
                                        (§ set! (:shouldClear lbe) false)
                                    )
                                )
                            )
                        )
                        (when (= (:clearRoot this) (:clearPathRoot lb)) => this
                            (§ set! (:shouldClear this) true)
                            (update! *clear-sites* assoc lb (conj sites this))
                            this
                        )
                    )
                )
            )
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--LocalBindingExpr [#_"LocalBindingExpr" this]
        (throw (UnsupportedOperationException. "Can't eval locals"))
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--LocalBindingExpr [#_"LocalBindingExpr" this]
        (some? (LocalBinding''getPrimitiveType (:lb this)))
    )

    (declare ObjExpr''emitUnboxedLocal)

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--LocalBindingExpr [#_"LocalBindingExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (ObjExpr''emitUnboxedLocal objx, gen, (:lb this))
        nil
    )

    (declare ObjExpr''emitLocal)

    #_override
    (defn #_"void" Expr'''emit--LocalBindingExpr [#_"LocalBindingExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (when (not= context :Context'STATEMENT)
            (ObjExpr''emitLocal objx, gen, (:lb this), (:shouldClear this))
        )
        nil
    )

    #_override
    (defn #_"Object" AssignableExpr'''evalAssign--LocalBindingExpr [#_"LocalBindingExpr" this, #_"Expr" val]
        (throw (UnsupportedOperationException. "Can't eval locals"))
    )

    (declare ObjExpr''emitAssignLocal)

    #_override
    (defn #_"void" AssignableExpr'''emitAssign--LocalBindingExpr [#_"LocalBindingExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Expr" val]
        (ObjExpr''emitAssignLocal objx, gen, (:lb this), val)
        (when (not= context :Context'STATEMENT)
            (ObjExpr''emitLocal objx, gen, (:lb this), false)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--LocalBindingExpr [#_"LocalBindingExpr" this]
        (or (some? (:tag this)) (.hasJavaClass (:lb this)))
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--LocalBindingExpr [#_"LocalBindingExpr" this]
        (if (some? (:tag this)) (HostExpr'tagToClass (:tag this)) (.getJavaClass (:lb this)))
    )
)

(class-ns MethodParamExpr
    (defn #_"MethodParamExpr" MethodParamExpr'new [#_"Class" c]
        (hash-map
            #_"Class" :c c
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--MethodParamExpr [#_"MethodParamExpr" this]
        (throw (RuntimeException. "Can't eval"))
    )

    #_override
    (defn #_"void" Expr'''emit--MethodParamExpr [#_"MethodParamExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (throw (RuntimeException. "Can't emit"))
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--MethodParamExpr [#_"MethodParamExpr" this]
        (some? (:c this))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--MethodParamExpr [#_"MethodParamExpr" this]
        (:c this)
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--MethodParamExpr [#_"MethodParamExpr" this]
        (Reflector'isPrimitive (:c this))
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--MethodParamExpr [#_"MethodParamExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (throw (RuntimeException. "Can't emit"))
    )
)

(class-ns FnMethod
    (defn #_"FnMethod" FnMethod'new [#_"ObjExpr" objx, #_"ObjMethod" parent]
        (merge (ObjMethod'new objx, parent)
            (hash-map
                ;; localbinding->localbinding
                #_"PersistentVector" :reqParms nil
                #_"LocalBinding" :restParm nil
                #_"Type[]" :argTypes nil
                #_"Class[]" :argClasses nil
                #_"Class" :retClass nil
                #_"String" :prim nil
            )
        )
    )

    (defn #_"char" FnMethod'classChar [#_"Object" x]
        (let [#_"Class" c
                (cond
                    (instance? Class x) x
                    (symbol? x)         (HostExpr'primClassForName x)
                )]
            (cond
                (or (nil? c) (not (.isPrimitive c))) \O
                (= c Long/TYPE)                      \L
                (= c Double/TYPE)                    \D
                :else
                    (throw (IllegalArgumentException. "Only long and double primitives are supported"))
            )
        )
    )

    (defn #_"String" FnMethod'primInterface [#_"IPersistentVector" args]
        (let [#_"StringBuilder" sb (StringBuilder.)]
            (dotimes [#_"int" i (count args)]
                (.append sb, (FnMethod'classChar (Compiler'tagOf (nth args i))))
            )
            (.append sb, (FnMethod'classChar (Compiler'tagOf args)))
            (let [#_"String" s (.toString sb) #_"boolean" prim? (or (.contains s, "L") (.contains s, "D"))]
                (cond
                    (and prim? (< 4 (count args))) (throw (IllegalArgumentException. "fns taking primitives support only 4 or fewer args"))
                    prim?                           (str "clojure.lang.IFn$" s)
                )
            )
        )
    )

    (defn #_"FnMethod" FnMethod'parse [#_"ObjExpr" objx, #_"ISeq" form, #_"Object" retTag]
        ;; ([args] body...)
        (let [#_"IPersistentVector" parms (first form) #_"ISeq" body (next form)
              #_"FnMethod" fm
                (-> (FnMethod'new objx, *method*)
                    (assoc :line *line*)
                )
              ;; register as the current method and set up a new env frame
              #_"PathNode" pnode (or *clear-path* (PathNode'new :PathType'PATH, nil))]
            (binding [*method*            fm
                      *local-env*         *local-env*
                      *next-local-num*    0
                      *loop-locals*       nil
                      *clear-path*        pnode
                      *clear-root*        pnode
                      *clear-sites*       {}
                      *in-return-context* true]
                (let [#_"String" prim (FnMethod'primInterface parms) prim (when (some? prim) (.replace prim, \., \/))
                      fm (assoc fm :prim prim)
                      retTag (if (string? retTag) (symbol retTag) retTag)
                      retTag (when (and (symbol? retTag) (any = (.getName retTag) "long" "double")) retTag)
                      #_"Class" retClass
                        (let-when [retClass (HostExpr'tagClass (or (Compiler'tagOf parms) retTag))] (.isPrimitive retClass) => Object
                            (when-not (any = retClass Double/TYPE Long/TYPE) => retClass
                                (throw (IllegalArgumentException. "Only long and double primitives are supported"))
                            )
                        )
                      fm (assoc fm :retClass retClass)]
                    ;; register 'this' as local 0
                    (if (some? (:thisName objx))
                        (Compiler'registerLocal (symbol (:thisName objx)), nil, nil, false)
                        (Compiler'getAndIncLocalNum)
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
                                                (let [#_"Class" c (Compiler'primClass (HostExpr'tagClass (Compiler'tagOf p)))]
                                                    (when (and (.isPrimitive c) (not (any = c Double/TYPE Long/TYPE)))
                                                        (throw (IllegalArgumentException. (str "Only long and double primitives are supported: " p)))
                                                    )
                                                    (when (and rest? (some? (Compiler'tagOf p)))
                                                        (throw (RuntimeException. "& arg cannot have type hint"))
                                                    )
                                                    (when (and rest? (some? prim))
                                                        (throw (RuntimeException. "fns taking primitives cannot be variadic"))
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
                        (let [fm (assoc fm
                                    #_"Type[]" :argTypes (.toArray (:argTypes fm), (make-array Type (count (:argTypes fm))))
                                    #_"Class[]" :argClasses (.toArray (:argClasses fm), (make-array Class (count (:argClasses fm))))
                                )]
                            (when (some? prim)
                                (dotimes [#_"int" i (alength (:argClasses fm))]
                                    (when (any = (aget (:argClasses fm) i) Long/TYPE Double/TYPE)
                                        (Compiler'getAndIncLocalNum)
                                    )
                                )
                            )
                            (assoc fm :body (.parse (BodyParser'new), :Context'RETURN, body))
                        )
                    )
                )
            )
        )
    )

    #_method
    (defn #_"void" FnMethod''doEmitStatic [#_"FnMethod" this, #_"ObjExpr" fn, #_"ClassVisitor" cv]
        (let [#_"Type" returnType (Type/getType (:retClass this))
              #_"Method" ms (Method. "invokeStatic", returnType, (:argTypes this))
              #_"GeneratorAdapter" gen (GeneratorAdapter. (| Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), ms, nil, Compiler'EXCEPTION_TYPES, cv)]
            (.visitCode gen)
            (let [#_"Label" loopLabel (.mark gen)]
                (.visitLineNumber gen, (:line this), loopLabel)
                (binding [*loop-label* loopLabel, *method* this]
                    (ObjMethod'emitBody (:objx this), gen, (:retClass this), (:body this))
                    (let [#_"Label" end (.mark gen)]
                        (loop-when-recur [#_"ISeq" lbs (seq (:argLocals this))] (some? lbs) [(next lbs)]
                            (let [#_"LocalBinding" lb (first lbs)]
                                (.visitLocalVariable gen, (:name lb), (.getDescriptor (aget (:argTypes this) (:idx lb))), nil, loopLabel, end, (:idx lb))
                            )
                        )
                    )
                )
                (.returnValue gen)
                (.endMethod gen)
                ;; generate the regular invoke, calling the static method
                (let [#_"Method" m (Method. (.getMethodName this), (Type/getType Object), (.getArgTypes this))
                      gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, Compiler'EXCEPTION_TYPES, cv)]
                    (.visitCode gen)
                    (dotimes [#_"int" i (alength (:argTypes this))]
                        (.loadArg gen, i)
                        (HostExpr'emitUnboxArg fn, gen, (aget (:argClasses this) i))
                        (when-not (.isPrimitive (aget (:argClasses this) i))
                            (.visitInsn gen, Opcodes/ACONST_NULL)
                            (.storeArg gen, i)
                        )
                    )
                    (let [#_"Label" callLabel (.mark gen)]
                        (.visitLineNumber gen, (:line this), callLabel)
                        (.invokeStatic gen, (:objtype (:objx this)), ms)
                        (.box gen, returnType)
                        (.returnValue gen)
                        (.endMethod gen)
                        ;; generate invokePrim if prim
                        (when (some? (:prim this))
                            (let [returnType (if (any = (:retClass this) Double/TYPE Long/TYPE) (.getReturnType this) (Type/getType Object))
                                  #_"Method" pm (Method. "invokePrim", returnType, (:argTypes this))
                                  gen (GeneratorAdapter. (| Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL), pm, nil, Compiler'EXCEPTION_TYPES, cv)]
                                (.visitCode gen)
                                (dotimes [#_"int" i (alength (:argTypes this))]
                                    (.loadArg gen, i)
                                    (when-not (.isPrimitive (aget (:argClasses this) i))
                                        (.visitInsn gen, Opcodes/ACONST_NULL)
                                        (.storeArg gen, i)
                                    )
                                )
                                (.invokeStatic gen, (:objtype (:objx this)), ms)
                                (.returnValue gen)
                                (.endMethod gen)
                            )
                        )
                    )
                )
            )
        )
        nil
    )

    #_method
    (defn #_"void" FnMethod''doEmitPrim [#_"FnMethod" this, #_"ObjExpr" fn, #_"ClassVisitor" cv]
        (let [#_"Type" returnType (if (any = (:retClass this) Double/TYPE Long/TYPE) (.getReturnType this) (Type/getType Object))
              #_"Method" ms (Method. "invokePrim", returnType, (:argTypes this))
              #_"GeneratorAdapter" gen (GeneratorAdapter. (| Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL), ms, nil, Compiler'EXCEPTION_TYPES, cv)]
            (.visitCode gen)
            (let [#_"Label" loopLabel (.mark gen)]
                (.visitLineNumber gen, (:line this), loopLabel)
                (binding [*loop-label* loopLabel, *method* this]
                    (ObjMethod'emitBody (:objx this), gen, (:retClass this), (:body this))
                    (let [#_"Label" end (.mark gen)]
                        (.visitLocalVariable gen, "this", "Ljava/lang/Object;", nil, loopLabel, end, 0)
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
            ;; generate the regular invoke, calling the prim method
            (let [#_"Method" m (Method. (.getMethodName this), (Type/getType Object), (.getArgTypes this))
                  gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, Compiler'EXCEPTION_TYPES, cv)]
                (.visitCode gen)
                (.loadThis gen)
                (dotimes [#_"int" i (alength (:argTypes this))]
                    (.loadArg gen, i)
                    (HostExpr'emitUnboxArg fn, gen, (aget (:argClasses this) i))
                )
                (.invokeInterface gen, (Type/getType (str "L" (:prim this) ";")), ms)
                (.box gen, (.getReturnType this))
                (.returnValue gen)
                (.endMethod gen)
            )
        )
        nil
    )

    #_method
    (defn- #_"void" FnMethod''doEmit [#_"FnMethod" this, #_"ObjExpr" fn, #_"ClassVisitor" cv]
        (let [#_"Method" m (Method. (.getMethodName this), (.getReturnType this), (.getArgTypes this))
              #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, Compiler'EXCEPTION_TYPES, cv)]
            (.visitCode gen)
            (let [#_"Label" loopLabel (.mark gen)]
                (.visitLineNumber gen, (:line this), loopLabel)
                (binding [*loop-label* loopLabel, *method* this]
                    (.emit (:body this), :Context'RETURN, fn, gen)
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

    #_override
    (defn #_"void" Expr'''emit--FnMethod [#_"FnMethod" this, #_"ObjExpr" fn, #_"ClassVisitor" cv]
        (cond
            (:canBeDirect fn)    (FnMethod''doEmitStatic this, fn, cv)
            (some? (:prim this)) (FnMethod''doEmitPrim this, fn, cv)
            :else                (FnMethod''doEmit this, fn, cv)
        )
        nil
    )

    #_method
    (defn #_"boolean" FnMethod''isVariadic [#_"FnMethod" this]
        (some? (:restParm this))
    )

    #_override
    (defn #_"int" ObjMethod'''numParams--FnMethod [#_"FnMethod" this]
        (+ (count (:reqParms this)) (if (FnMethod''isVariadic this) 1 0))
    )

    #_override
    (defn #_"String" ObjMethod'''getMethodName--FnMethod [#_"FnMethod" this]
        (if (FnMethod''isVariadic this) "doInvoke" "invoke")
    )

    #_override
    (defn #_"Type" ObjMethod'''getReturnType--FnMethod [#_"FnMethod" this]
        (if (some? (:prim this))
            (Type/getType (:retClass this))
            (Type/getType Object)
        )
    )

    #_override
    (defn #_"Type[]" ObjMethod'''getArgTypes--FnMethod [#_"FnMethod" this]
        (if (and (FnMethod''isVariadic this) (= (count (:reqParms this)) Compiler'MAX_POSITIONAL_ARITY))
            (let [#_"int" n (inc Compiler'MAX_POSITIONAL_ARITY) #_"Type[]" a (make-array Type n)]
                (dotimes [#_"int" i n]
                    (aset a i (Type/getType Object))
                )
                a
            )
            (aget Compiler'ARG_TYPES (.numParams this))
        )
    )
)

(class-ns ObjExpr
    (defn #_"ObjExpr" ObjExpr'new [#_"Object" tag]
        (hash-map
            #_"String" :name nil
            #_"String" :internalName nil
            #_"String" :thisName nil
            #_"Type" :objtype nil
            #_"Object" :tag tag
            ;; localbinding->itself
            #_mutable #_"IPersistentMap" :closes {}
            ;; localbndingexprs
            #_"IPersistentVector" :closesExprs []
            ;; symbols
            #_"IPersistentSet" :volatiles #{}

            ;; symbol->lb
            #_"IPersistentMap" :fields nil

            ;; hinted fields
            #_"IPersistentVector" :hintedFields []

            ;; Keyword->KeywordExpr
            #_"IPersistentMap" :keywords {}
            #_"IPersistentMap" :vars {}
            #_mutable #_"Class" :compiledClass nil
            #_"int" :line 0
            #_"PersistentVector" :constants nil
            #_mutable #_"IPersistentSet" :usedConstants #{}

            #_"int" :constantsID 0
            #_"int" :altCtorDrops 0

            #_"IPersistentVector" :keywordCallsites nil
            #_"IPersistentVector" :protocolCallsites nil
            #_"boolean" :onceOnly false

            #_"Object" :src nil

            #_"IPersistentMap" :opts {}

            #_"IPersistentMap" :classMeta nil
            #_"boolean" :canBeDirect false

            #_mutable #_"DynamicClassLoader" :loader nil
            #_"byte[]" :bytecode nil
        )
    )

    #_method
    (defn #_"Type" ObjExpr''constantType [#_"ObjExpr" this, #_"int" id]
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

    (defn #_"String" ObjExpr'trimGenID [#_"String" name]
        (let [#_"int" i (.lastIndexOf name, "__")]
            (if (= i -1) name (.substring name, 0, i))
        )
    )

    #_method
    (defn #_"Type[]" ObjExpr''ctorTypes [#_"ObjExpr" this]
        (let [#_"IPersistentVector" v (if (.supportsMeta this) [(Type/getType IPersistentMap)] [])
              v (loop-when [v v #_"ISeq" s (keys (:closes this))] (some? s) => v
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

    (declare ObjExpr''emitValue)

    #_method
    (defn- #_"void" ObjExpr''emitKeywordCallsites [#_"ObjExpr" this, #_"GeneratorAdapter" clinitgen]
        (dotimes [#_"int" i (count (:keywordCallsites this))]
            (let [#_"Keyword" k (nth (:keywordCallsites this) i)]
                (.newInstance clinitgen, (Type/getType KeywordLookupSite))
                (.dup clinitgen)
                (ObjExpr''emitValue this, k, clinitgen)
                (.invokeConstructor clinitgen, (Type/getType KeywordLookupSite), (Method/getMethod "void <init>(clojure.lang.Keyword)"))
                (.dup clinitgen)
                (.putStatic clinitgen, (:objtype this), (Compiler'siteNameStatic i), (Type/getType KeywordLookupSite))
                (.putStatic clinitgen, (:objtype this), (Compiler'thunkNameStatic i), (Type/getType ILookupThunk))
            )
        )
        nil
    )

    #_override
    (defn #_"void" ObjExpr'''emitStatics--ObjExpr [#_"ObjExpr" this, #_"ClassVisitor" gen]
        nil
    )

    #_override
    (defn #_"void" ObjExpr'''emitMethods--ObjExpr [#_"ObjExpr" this, #_"ClassVisitor" gen]
        nil
    )

    #_method
    (defn #_"void" ObjExpr''emitObjectArray [#_"ObjExpr" this, #_"Object[]" a, #_"GeneratorAdapter" gen]
        (.push gen, (alength a))
        (.newArray gen, (Type/getType Object))
        (dotimes [#_"int" i (alength a)]
            (.dup gen)
            (.push gen, i)
            (ObjExpr''emitValue this, (aget a i), gen)
            (.arrayStore gen, (Type/getType Object))
        )
        nil
    )

    #_method
    (defn #_"void" ObjExpr''emitConstants [#_"ObjExpr" this, #_"GeneratorAdapter" clinitgen]
        (dotimes [#_"int" i (count (:constants this))]
            (when (contains? (:usedConstants this) i)
                (ObjExpr''emitValue this, (nth (:constants this) i), clinitgen)
                (.checkCast clinitgen, (ObjExpr''constantType this, i))
                (.putStatic clinitgen, (:objtype this), (Compiler'constantName i), (ObjExpr''constantType this, i))
            )
        )
        nil
    )

    #_method
    (defn #_"boolean" ObjExpr''isVolatile [#_"ObjExpr" this, #_"LocalBinding" lb]
        (and (contains? (:fields this) (:sym lb)) (get (meta (:sym lb)) :volatile-mutable))
    )

    #_method
    (defn #_"boolean" ObjExpr''isMutable [#_"ObjExpr" this, #_"LocalBinding" lb]
        (or (ObjExpr''isVolatile this, lb) (and (contains? (:fields this) (:sym lb)) (get (meta (:sym lb)) :unsynchronized-mutable)))
    )

    #_method
    (defn #_"boolean" ObjExpr''isDeftype [#_"ObjExpr" this]
        (some? (:fields this))
    )

    #_override
    (defn #_"boolean" ObjExpr'''supportsMeta--ObjExpr [#_"ObjExpr" this]
        (not (ObjExpr''isDeftype this))
    )

    #_method
    (defn #_"void" ObjExpr''emitClearCloses [#_"ObjExpr" this, #_"GeneratorAdapter" gen]
        nil
    )

    #_method
    (defn #_"Class" ObjExpr''getCompiledClass [#_"ObjExpr" this]
        (§ sync this
            (when (nil? (:compiledClass this))
                (§ set! (:loader this) *loader*)
                (§ set! (:compiledClass this) (.defineClass (:loader this), (:name this), (:bytecode this), (:src this)))
            )
            (:compiledClass this)
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--ObjExpr [#_"ObjExpr" this]
        (when-not (ObjExpr''isDeftype this)
            (.newInstance (ObjExpr''getCompiledClass this))
        )
    )

    #_method
    (defn #_"void" ObjExpr''emitLetFnInits [#_"ObjExpr" this, #_"GeneratorAdapter" gen, #_"ObjExpr" objx, #_"IPersistentSet" letFnLocals]
        ;; objx arg is enclosing objx, not this
        (.checkCast gen, (:objtype this))

        (loop-when-recur [#_"ISeq" s (keys (:closes this))] (some? s) [(next s)]
            (let [#_"LocalBinding" lb (first s)]
                (when (contains? letFnLocals lb)
                    (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                        (.dup gen)
                        (if (some? primc)
                            (do
                                (ObjExpr''emitUnboxedLocal objx, gen, lb)
                                (.putField gen, (:objtype this), (:name lb), (Type/getType primc))
                            )
                            (do
                                (ObjExpr''emitLocal objx, gen, lb, false)
                                (.putField gen, (:objtype this), (:name lb), (Type/getType Object))
                            )
                        )
                    )
                )
            )
        )
        (.pop gen)
        nil
    )

    #_override
    (defn #_"void" Expr'''emit--ObjExpr [#_"ObjExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        ;; emitting a Fn means constructing an instance, feeding closed-overs from enclosing scope, if any
        ;; objx arg is enclosing objx, not this
        (when-not (ObjExpr''isDeftype this) => (.visitInsn gen, Opcodes/ACONST_NULL)
            (.newInstance gen, (:objtype this))
            (.dup gen)
            (when (.supportsMeta this)
                (.visitInsn gen, Opcodes/ACONST_NULL)
            )
            (loop-when-recur [#_"ISeq" s (seq (:closesExprs this))] (some? s) [(next s)]
                (let [#_"LocalBindingExpr" lbe (first s) #_"LocalBinding" lb (:lb lbe)]
                    (if (some? (LocalBinding''getPrimitiveType lb))
                        (ObjExpr''emitUnboxedLocal objx, gen, lb)
                        (ObjExpr''emitLocal objx, gen, lb, (:shouldClear lbe))
                    )
                )
            )
            (.invokeConstructor gen, (:objtype this), (Method. "<init>", Type/VOID_TYPE, (ObjExpr''ctorTypes this)))
        )
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--ObjExpr [#_"ObjExpr" this]
        true
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--ObjExpr [#_"ObjExpr" this]
        (or (:compiledClass this)
            (if (some? (:tag this)) (HostExpr'tagToClass (:tag this)) IFn)
        )
    )

    #_method
    (defn #_"void" ObjExpr''emitAssignLocal [#_"ObjExpr" this, #_"GeneratorAdapter" gen, #_"LocalBinding" lb, #_"Expr" val]
        (when (ObjExpr''isMutable this, lb) => (throw (IllegalArgumentException. (str "Cannot assign to non-mutable: " (:name lb))))
            (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                (.loadThis gen)
                (if (some? primc)
                    (do
                        (when (not (and (instance? MaybePrimitiveExpr val) (.canEmitPrimitive val)))
                            (throw (IllegalArgumentException. (str "Must assign primitive to primitive mutable: " (:name lb))))
                        )
                        (.emitUnboxed val, :Context'EXPRESSION, this, gen)
                        (.putField gen, (:objtype this), (:name lb), (Type/getType primc))
                    )
                    (do
                        (.emit val, :Context'EXPRESSION, this, gen)
                        (.putField gen, (:objtype this), (:name lb), (Type/getType Object))
                    )
                )
            )
        )
        nil
    )

    #_method
    (defn- #_"void" ObjExpr''emitLocal [#_"ObjExpr" this, #_"GeneratorAdapter" gen, #_"LocalBinding" lb, #_"boolean" clear?]
        (if (contains? (:closes this) lb)
            (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                (.loadThis gen)
                (if (some? primc)
                    (do
                        (.getField gen, (:objtype this), (:name lb), (Type/getType primc))
                        (HostExpr'emitBoxReturn this, gen, primc)
                    )
                    (do
                        (.getField gen, (:objtype this), (:name lb), (Type/getType Object))
                        (when (and (:onceOnly this) clear? (:canBeCleared lb))
                            (.loadThis gen)
                            (.visitInsn gen, Opcodes/ACONST_NULL)
                            (.putField gen, (:objtype this), (:name lb), (Type/getType Object))
                        )
                    )
                )
            )
            (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                (if (:isArg lb)
                    (let [#_"int" argoff (if (:canBeDirect this) 0 1)]
                        (.loadArg gen, (- (:idx lb) argoff))
                        (cond (some? primc)
                            (do
                                (HostExpr'emitBoxReturn this, gen, primc)
                            )
                            (and clear? (:canBeCleared lb))
                            (do
                                (.visitInsn gen, Opcodes/ACONST_NULL)
                                (.storeArg gen, (- (:idx lb) argoff))
                            )
                        )
                    )
                    (if (some? primc)
                        (do
                            (.visitVarInsn gen, (.getOpcode (Type/getType primc), Opcodes/ILOAD), (:idx lb))
                            (HostExpr'emitBoxReturn this, gen, primc)
                        )
                        (do
                            (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), (:idx lb))
                            (when (and clear? (:canBeCleared lb))
                                (.visitInsn gen, Opcodes/ACONST_NULL)
                                (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx lb))
                            )
                        )
                    )
                )
            )
        )
        nil
    )

    #_method
    (defn- #_"void" ObjExpr''emitUnboxedLocal [#_"ObjExpr" this, #_"GeneratorAdapter" gen, #_"LocalBinding" lb]
        (cond (contains? (:closes this) lb)
            (do
                (.loadThis gen)
                (.getField gen, (:objtype this), (:name lb), (Type/getType (LocalBinding''getPrimitiveType lb)))
            )
            (:isArg lb)
            (do
                (.loadArg gen, (- (:idx lb) (if (:canBeDirect this) 0 1)))
            )
            :else
            (do
                (.visitVarInsn gen, (.getOpcode (Type/getType (LocalBinding''getPrimitiveType lb)), Opcodes/ILOAD), (:idx lb))
            )
        )
        nil
    )

    #_method
    (defn #_"void" ObjExpr''emitVar [#_"ObjExpr" this, #_"GeneratorAdapter" gen, #_"Var" var]
        (ObjExpr''emitConstant this, gen, (get (:vars this) var))
        nil
    )

    #_method
    (defn #_"void" ObjExpr''emitVarValue [#_"ObjExpr" this, #_"GeneratorAdapter" gen, #_"Var" v]
        (ObjExpr''emitConstant this, gen, (get (:vars this) v))
        (.invokeVirtual gen, (Type/getType Var), (if (.isDynamic v) (Method/getMethod "Object get()") (Method/getMethod "Object getRawRoot()")))
        nil
    )

    #_method
    (defn #_"void" ObjExpr''emitKeyword [#_"ObjExpr" this, #_"GeneratorAdapter" gen, #_"Keyword" k]
        (ObjExpr''emitConstant this, gen, (get (:keywords this) k))
        nil
    )

    #_method
    (defn #_"void" ObjExpr''emitConstant [#_"ObjExpr" this, #_"GeneratorAdapter" gen, #_"int" id]
        (§ update! (:usedConstants this) conj id)
        (.getStatic gen, (:objtype this), (Compiler'constantName id), (ObjExpr''constantType this, id))
        nil
    )

    #_method
    (defn #_"void" ObjExpr''emitValue [#_"ObjExpr" this, #_"Object" value, #_"GeneratorAdapter" gen]
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
                        (.push gen, (:ns value))
                        (.push gen, (:name value))
                        (.invokeStatic gen, (Type/getType Symbol), (Method/getMethod "clojure.lang.Symbol intern(String, String)"))
                        true
                    )
                    (keyword? value)
                    (do
                        (.push gen, (:ns (:sym value)))
                        (.push gen, (:name (:sym value)))
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.Keyword keyword(String, String)"))
                        true
                    )
                    (var? value)
                    (do
                        (.push gen, (.toString (:name (:ns value))))
                        (.push gen, (.toString (:sym value)))
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
                                    (ObjExpr''emitValue this, (Reflector'getInstanceField value, (Compiler'munge (:name field))), gen)
                                    (let-when [#_"Class" k (HostExpr'tagClass (Compiler'tagOf field))] (.isPrimitive k)
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
                        (ObjExpr''emitObjectArray this, (.toArray v), gen)
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.IPersistentMap map(Object[])"))
                        true
                    )
                    (instance? IPersistentVector value)
                    (let [#_"IPersistentVector" args value]
                        (if (<= (count args) Tuple'MAX_SIZE)
                            (do
                                (dotimes [#_"int" i (count args)]
                                    (ObjExpr''emitValue this, (nth args i), gen)
                                )
                                (.invokeStatic gen, (Type/getType Tuple), (aget Compiler'createTupleMethods (count args)))
                            )
                            (do
                                (ObjExpr''emitObjectArray this, (.toArray args), gen)
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
                                (ObjExpr''emitObjectArray this, (RT/seqToArray vs), gen)
                                (.invokeStatic gen, (Type/getType PersistentHashSet), (Method/getMethod "clojure.lang.PersistentHashSet create(Object[])"))
                            )
                        )
                        true
                    )
                    (or (instance? ISeq value) (instance? IPersistentList value))
                    (let [#_"ISeq" vs (seq value)]
                        (ObjExpr''emitObjectArray this, (RT/seqToArray vs), gen)
                        (.invokeStatic gen, (Type/getType PersistentList), (Method/getMethod "clojure.lang.IPersistentList create(Object[])"))
                        true
                    )
                    (instance? Pattern value)
                    (do
                        (ObjExpr''emitValue this, (.toString value), gen)
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
                    (ObjExpr''emitValue this, (meta value), gen)
                    (.checkCast gen, (Type/getType IPersistentMap))
                    (.invokeInterface gen, (Type/getType IObj), (Method/getMethod "clojure.lang.IObj withMeta(clojure.lang.IPersistentMap)"))
                )
            )
        )
        nil
    )

    #_method
    (defn #_"ObjExpr" ObjExpr''compile [#_"ObjExpr" this, #_"String" superName, #_"String[]" interfaceNames, #_"boolean" oneTimeUse]
        ;; create bytecode for a class
        ;; with name current_ns.defname[$letname]+
        ;; anonymous fns get names fn__id
        ;; derived from AFn'RestFn
        (let [#_"ClassWriter" cw (ClassWriter. ClassWriter/COMPUTE_MAXS) #_"ClassVisitor" cv cw]
            (.visit cv, Opcodes/V1_5, (| Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER Opcodes/ACC_FINAL), (:internalName this), nil, superName, interfaceNames)
            (when (.supportsMeta this)
                (.visitField cv, Opcodes/ACC_FINAL, "__meta", (.getDescriptor (Type/getType IPersistentMap)), nil, nil)
            )
            ;; instance fields for closed-overs
            (loop-when-recur [#_"ISeq" s (keys (:closes this))] (some? s) [(next s)]
                (let [#_"LocalBinding" lb (first s)
                      #_"String" fd
                        (if (some? (LocalBinding''getPrimitiveType lb))
                            (.getDescriptor (Type/getType (LocalBinding''getPrimitiveType lb)))
                            ;; todo - when closed-overs are fields, use more specific types here and in ctor and emitLocal?
                            (.getDescriptor (Type/getType Object))
                        )]
                    (if (ObjExpr''isDeftype this)
                        (let [#_"int" access
                                (cond
                                    (ObjExpr''isVolatile this, lb) Opcodes/ACC_VOLATILE
                                    (ObjExpr''isMutable this, lb) 0
                                    :else (| Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL)
                                )]
                            (.visitField cv, access, (:name lb), fd, nil, nil)
                        )
                        ;; todo - only enable this non-private+writability for letfns where we need it
                        (let [#_"int" access
                                (if (some? (LocalBinding''getPrimitiveType lb))
                                    (if (ObjExpr''isVolatile this, lb) Opcodes/ACC_VOLATILE 0)
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
            (let [#_"Method" m (Method. "<init>", Type/VOID_TYPE, (ObjExpr''ctorTypes this))
                  #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, nil, cv)
                  #_"Label" start (.newLabel ctorgen) #_"Label" end (.newLabel ctorgen)]
                (.visitCode ctorgen)
                (.visitLineNumber ctorgen, (:line this), (.mark ctorgen))
                (.visitLabel ctorgen, start)
                (.loadThis ctorgen)
                (.invokeConstructor ctorgen, (Type/getObjectType superName), (Method/getMethod "void <init>()"))

                (when (.supportsMeta this)
                    (.loadThis ctorgen)
                    (.visitVarInsn ctorgen, (.getOpcode (Type/getType IPersistentMap), Opcodes/ILOAD), 1)
                    (.putField ctorgen, (:objtype this), "__meta", (Type/getType IPersistentMap))
                )

                (let [[this #_"int" a]
                        (loop-when [this this a (if (.supportsMeta this) 2 1) #_"ISeq" s (keys (:closes this))] (some? s) => [this a]
                            (let [#_"LocalBinding" lb (first s)]
                                (.loadThis ctorgen)
                                (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)
                                      a (if (some? primc)
                                            (do
                                                (.visitVarInsn ctorgen, (.getOpcode (Type/getType primc), Opcodes/ILOAD), a)
                                                (.putField ctorgen, (:objtype this), (:name lb), (Type/getType primc))
                                                (if (any = primc Long/TYPE Double/TYPE) (inc a) a)
                                            )
                                            (do
                                                (.visitVarInsn ctorgen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), a)
                                                (.putField ctorgen, (:objtype this), (:name lb), (Type/getType Object))
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
                        (let [#_"Type[]" ctorTypes (ObjExpr''ctorTypes this)]

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

                                (.invokeConstructor ctorgen, (:objtype this), (Method. "<init>", Type/VOID_TYPE, ctorTypes))

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

                                (.invokeConstructor ctorgen, (:objtype this), (Method. "<init>", Type/VOID_TYPE, ctorTypes))

                                (.returnValue ctorgen)
                                (.endMethod ctorgen)
                            )
                        )
                    )

                    (when (.supportsMeta this)
                        (let [#_"Type[]" ctorTypes (ObjExpr''ctorTypes this)]

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
                                (.invokeConstructor ctorgen, (:objtype this), (Method. "<init>", Type/VOID_TYPE, ctorTypes))
                                (.returnValue ctorgen)
                                (.endMethod ctorgen)
                            )

                            ;; meta()
                            (let [#_"Method" meth (Method/getMethod "clojure.lang.IPersistentMap meta()")
                                  #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, meth, nil, nil, cv)]
                                (.visitCode gen)
                                (.loadThis gen)
                                (.getField gen, (:objtype this), "__meta", (Type/getType IPersistentMap))
                                (.returnValue gen)
                                (.endMethod gen)
                            )

                            ;; withMeta()
                            (let [#_"Method" meth (Method/getMethod "clojure.lang.IObj withMeta(clojure.lang.IPersistentMap)")
                                  #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, meth, nil, nil, cv)]
                                (.visitCode gen)
                                (.newInstance gen, (:objtype this))
                                (.dup gen)
                                (.loadArg gen, 0)
                                (loop-when-recur [a a #_"ISeq" s (keys (:closes this))] (some? s) [(inc a) (next s)]
                                    (let [#_"LocalBinding" lb (first s)]
                                        (.loadThis gen)
                                        (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                                            (.getField gen, (:objtype this), (:name lb), (if (some? primc) (Type/getType primc) (Type/getType Object)))
                                        )
                                    )
                                )
                                (.invokeConstructor gen, (:objtype this), (Method. "<init>", Type/VOID_TYPE, ctorTypes))
                                (.returnValue gen)
                                (.endMethod gen)
                            )
                        )
                    )

                    (.emitStatics this, cv)
                    (.emitMethods this, cv)

                    ;; static fields for constants
                    (dotimes [#_"int" i (count (:constants this))]
                        (when (contains? (:usedConstants this) i)
                            (.visitField cv, (| Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL Opcodes/ACC_STATIC), (Compiler'constantName i), (.getDescriptor (ObjExpr''constantType this, i)), nil, nil)
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
                            (ObjExpr''emitConstants this, clinitgen)
                        )

                        (when (pos? (count (:keywordCallsites this)))
                            (ObjExpr''emitKeywordCallsites this, clinitgen)
                        )

                        (when (and (ObjExpr''isDeftype this) (get (:opts this) :load-ns))
                            (let [#_"String" nsname (namespace (second (:src this)))]
                                (when (not (= nsname "clojure.core"))
                                    (.push clinitgen, "clojure.core")
                                    (.push clinitgen, "require")
                                    (.invokeStatic clinitgen, (Type/getType RT), (Method/getMethod "clojure.lang.Var var(String, String)"))
                                    (.invokeVirtual clinitgen, (Type/getType Var), (Method/getMethod "Object getRawRoot()"))
                                    (.checkCast clinitgen, (Type/getType IFn))
                                    (.push clinitgen, nsname)
                                    (.invokeStatic clinitgen, (Type/getType Symbol), (Method/getMethod "clojure.lang.Symbol intern(String)"))
                                    (.invokeInterface clinitgen, (Type/getType IFn), (Method/getMethod "Object invoke(Object)"))
                                    (.pop clinitgen)
                                )
                            )
                        )

                        (.returnValue clinitgen)
                        (.endMethod clinitgen)
                        ;; end of class
                        (.visitEnd cv)

                        (assoc this :bytecode (.toByteArray cw))
                    )
                )
            )
        )
    )
)

(class-ns FnExpr
    (defn #_"FnExpr" FnExpr'new [#_"Object" tag]
        (merge (ObjExpr'new tag)
            (hash-map
                ;; if there is a variadic overload (there can only be one) it is stored here
                #_"FnMethod" :variadicMethod nil
                #_"IPersistentCollection" :methods nil
                #_"boolean" :hasPrimSigs false
                #_"boolean" :hasMeta false
                #_"boolean" :hasEnclosingMethod false
            )
        )
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--FnExpr [#_"FnExpr" this]
        true
    )

    #_override
    (defn #_"boolean" ObjExpr'''supportsMeta--FnExpr [#_"FnExpr" this]
        (:hasMeta this)
    )

    #_memoize!
    #_override
    (defn #_"Class" Expr'''getJavaClass--FnExpr [#_"FnExpr" this]
        (if (some? (:tag this)) (HostExpr'tagToClass (:tag this)) AFunction)
    )

    #_method
    (defn #_"boolean" FnExpr''isVariadic [#_"FnExpr" this]
        (some? (:variadicMethod this))
    )

    #_override
    (defn #_"void" ObjExpr'''emitMethods--FnExpr [#_"FnExpr" this, #_"ClassVisitor" cv]
        ;; override of invoke/doInvoke for each method
        (loop-when-recur [#_"ISeq" s (seq (:methods this))] (some? s) [(next s)]
            (.emit (first s), this, cv)
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

    (defn #_"Expr" FnExpr'parse [#_"Context" context, #_"ISeq" form, #_"String" name]
        (let [#_"IPersistentMap" fmeta (meta form)
              #_"ObjMethod" owner *method*
              #_"FnExpr" fn
                (-> (FnExpr'new (Compiler'tagOf form))
                    (assoc :src form :hasEnclosingMethod (some? owner) :line *line*)
                )
              fn (when (some? (meta (first form))) => fn
                    (assoc fn :onceOnly (boolean (get (meta (first form)) :once)))
                )
              #_"String" basename (if (some? owner) (:name (:objx owner)) (Compiler'munge (:name (:name *ns*))))
              [#_"Symbol" nm name]
                (if (symbol? (second form))
                    (let [nm (second form)]
                        [nm (str (:name nm) "__" (RT/nextID))]
                    )
                    (cond
                        (nil? name)   [nil (str "fn__" (RT/nextID))]
                        (some? owner) [nil (str name "__"(RT/nextID))]
                        :else         [nil name]
                    )
                )
              fn (assoc fn :name (str basename "$" (.replace (Compiler'munge name), ".", "_DOT_")))
              fn (assoc fn :internalName (.replace (:name fn), \., \/))
              fn (assoc fn :objtype (Type/getObjectType (:internalName fn)))
              #_"Object" rettag (get fmeta :rettag)
              #_"PersistentVector" prims []
              [fn prims]
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
                                [(assoc fn :thisName (:name nm)) (cons 'fn* (next (next form)))]
                            )
                          ;; now (fn [args] body...) or (fn ([args] body...) ([args2] body2...) ...)
                          ;; turn former into latter
                          form
                            (when (instance? IPersistentVector (second form)) => form
                                (list 'fn* (next form))
                            )
                          #_"FnMethod[]" a (make-array #_"FnMethod" Object (inc Compiler'MAX_POSITIONAL_ARITY))
                          [#_"FnMethod" variadic #_"boolean" usesThis prims]
                            (loop-when [variadic nil usesThis false prims prims #_"ISeq" s (next form)] (some? s) => [variadic usesThis prims]
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
                                        )
                                      prims
                                        (when (some? (:prim f)) => prims
                                            (conj prims (:prim f))
                                        )]
                                    (recur variadic (or usesThis (:usesThis f)) prims (next s))
                                )
                            )]
                        (when (some? variadic)
                            (loop-when-recur [#_"int" i (inc (count (:reqParms variadic)))] (<= i Compiler'MAX_POSITIONAL_ARITY) [(inc i)]
                                (when (some? (aget a i))
                                    (throw (RuntimeException. "Can't have fixed arity function with more params than variadic function"))
                                )
                            )
                        )
                        (let [fn (assoc fn :canBeDirect (and (not (:hasEnclosingMethod fn)) (zero? (count (:closes fn))) (not usesThis)))
                              #_"IPersistentCollection" methods
                                (loop-when-recur [methods nil #_"int" i 0]
                                                 (< i (alength a))
                                                 [(if (some? (aget a i)) (conj methods (aget a i)) methods) (inc i)]
                                              => (if (some? variadic) (conj methods variadic) methods)
                                )]
                            (when (:canBeDirect fn)
                                (doseq [#_"FnMethod" fm methods]
                                    (when (some? (:locals fm))
                                        (doseq [#_"LocalBinding" lb (keys (:locals fm))]
                                            (when (:isArg lb)
                                                (§ update! (:idx lb) dec)
                                            )
                                        )
                                    )
                                )
                            )
                            [(assoc fn
                                :methods methods
                                :variadicMethod variadic
                                :keywords *keywords*
                                :vars *vars*
                                :constants *constants*
                                :keywordCallsites *keyword-callsites*
                                :protocolCallsites *protocol-callsites*
                                :constantsID (RT/nextID)
                            ) prims]
                        )
                    )
                )
              fn (assoc fn :hasPrimSigs (pos? (count prims)))
              fmeta
                (when (some? fmeta)
                    (dissoc fmeta :line :column :rettag)
                )
              fn (assoc fn :hasMeta (pos? (count fmeta)))]
            (ObjExpr''compile fn, (if (FnExpr''isVariadic fn) "clojure/lang/RestFn" "clojure/lang/AFunction"), (when (pos? (count prims)) (.toArray prims, (make-array String (count prims)))), (:onceOnly fn))
            (ObjExpr''getCompiledClass fn)
            (when (.supportsMeta fn) => fn
                (MetaExpr'new fn, (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), fmeta))
            )
        )
    )

    #_method
    (defn #_"void" FnExpr''emitForDefn [#_"FnExpr" this, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emit this, :Context'EXPRESSION, objx, gen)
        nil
    )
)

(class-ns DefExpr
    (defn #_"DefExpr" DefExpr'new [#_"int" line, #_"Var" var, #_"Expr" init, #_"Expr" meta, #_"boolean" initProvided, #_"boolean" isDynamic, #_"boolean" shadowsCoreMapping]
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

    #_method
    (defn- #_"boolean" DefExpr''includesExplicitMetadata [#_"DefExpr" this, #_"MapExpr" expr]
        (loop-when [#_"int" i 0] (< i (count (:keyvals expr))) => false
            (recur-if (any = (:k (nth (:keyvals expr) i)) :declared :line :column) [(+ i 2)] => true)
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--DefExpr [#_"DefExpr" this]
        (when (:initProvided this)
            (.bindRoot (:var this), (.eval (:init this)))
        )
        (when (some? (:meta this))
            (let [#_"IPersistentMap" metaMap (.eval (:meta this))]
                (when (or (:initProvided this) true)
                    (.setMeta (:var this), metaMap)
                )
            )
        )
        (.setDynamic (:var this), (:isDynamic this))
    )

    #_override
    (defn #_"void" Expr'''emit--DefExpr [#_"DefExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (ObjExpr''emitVar objx, gen, (:var this))
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
            (.emit (:meta this), :Context'EXPRESSION, objx, gen)
            (.checkCast gen, (Type/getType IPersistentMap))
            (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "void setMeta(clojure.lang.IPersistentMap)"))
        )
        (when (:initProvided this)
            (.dup gen)
            (if (instance? FnExpr (:init this))
                (FnExpr''emitForDefn (:init this), objx, gen)
                (.emit (:init this), :Context'EXPRESSION, objx, gen)
            )
            (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "void bindRoot(Object)"))
        )
        (when (= context :Context'STATEMENT)
            (.pop gen)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--DefExpr [#_"DefExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--DefExpr [#_"DefExpr" this]
        Var
    )
)

(class-ns DefParser
    (defn #_"IParser" DefParser'new []
        (reify IParser
            ;; (def x) or (def x initexpr) or (def x "docstring" initexpr)
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [[#_"String" docstring form]
                        (when (and (= (count form) 4) (string? (third form))) => [nil form]
                            [(third form) (list (first form) (second form) (fourth form))]
                        )]
                    (cond
                        (< 3 (count form))            (throw (IllegalArgumentException. "Too many arguments to def"))
                        (< (count form) 2)            (throw (IllegalArgumentException. "Too few arguments to def"))
                        (not (symbol? (second form))) (throw (IllegalArgumentException. "First argument to def must be a Symbol"))
                    )
                    (let [#_"Symbol" sym (second form) #_"Var" v (Compiler'lookupVar sym, true)]
                        (when (some? v) => (throw (RuntimeException. "Can't refer to qualified var that doesn't exist"))
                            (let [[v #_"boolean" shadowsCoreMapping]
                                    (when (not (= (:ns v) *ns*)) => [v false]
                                        (when (nil? (:ns sym)) => (throw (RuntimeException. "Can't create defs outside of current ns"))
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
                                (when (and (not dynamic?) (.startsWith (:name sym), "*") (.endsWith (:name sym), "*") (< 2 (.length (:name sym))))
                                    (.println *err*, (str "Warning: " sym " not declared dynamic and thus is not dynamically rebindable, but its name suggests otherwise. Please either indicate ^:dynamic or change the name."))
                                )
                                (let [#_"Context" c (if (= context :Context'EVAL) context :Context'EXPRESSION)
                                      m (assoc m :line *line*) m (if (some? docstring) (assoc m :doc docstring) m)]
                                    (DefExpr'new *line*, v, (Compiler'analyze c, (third form), (:name (:sym v))), (Compiler'analyze c, m), (= (count form) 3), dynamic?, shadowsCoreMapping)
                                )
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
        (hash-map
            #_"LocalBinding" :binding binding
            #_"Expr" :init init
        )
    )
)

(class-ns LetFnExpr
    (defn #_"LetFnExpr" LetFnExpr'new [#_"PersistentVector" bindingInits, #_"Expr" body]
        (hash-map
            #_"PersistentVector" :bindingInits bindingInits
            #_"Expr" :body body
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--LetFnExpr [#_"LetFnExpr" this]
        (throw (UnsupportedOperationException. "Can't eval letfns"))
    )

    #_override
    (defn #_"void" Expr'''emit--LetFnExpr [#_"LetFnExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (dotimes [#_"int" i (count (:bindingInits this))]
            (let [#_"BindingInit" bi (nth (:bindingInits this) i)]
                (.visitInsn gen, Opcodes/ACONST_NULL)
                (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx (:binding bi)))
            )
        )
        (let [#_"IPersistentSet" lbset
                (loop-when [lbset #{} #_"int" i 0] (< i (count (:bindingInits this))) => lbset
                    (let [#_"BindingInit" bi (nth (:bindingInits this) i)]
                        (.emit (:init bi), :Context'EXPRESSION, objx, gen)
                        (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx (:binding bi)))
                        (recur (conj lbset (:binding bi)) (inc i))
                    )
                )]
            (dotimes [#_"int" i (count (:bindingInits this))]
                (let [#_"BindingInit" bi (nth (:bindingInits this) i)]
                    (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ILOAD), (:idx (:binding bi)))
                    (ObjExpr''emitLetFnInits (:init bi), gen, objx, lbset)
                )
            )
            (let [#_"Label" loopLabel (.mark gen)]
                (.emit (:body this), context, objx, gen)
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--LetFnExpr [#_"LetFnExpr" this]
        (.hasJavaClass (:body this))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--LetFnExpr [#_"LetFnExpr" this]
        (.getJavaClass (:body this))
    )
)

(class-ns LetFnParser
    (defn #_"IParser" LetFnParser'new []
        (reify IParser
            ;; (letfns* [var (fn [args] body) ...] body...)
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (instance? IPersistentVector (second form)) => (throw (IllegalArgumentException. "Bad binding form, expected vector"))
                    (let [#_"IPersistentVector" bindings (second form)]
                        (when (zero? (% (count bindings) 2)) => (throw (IllegalArgumentException. "Bad binding form, expected matched symbol expression pairs"))
                            (if (= context :Context'EVAL)
                                (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                                (binding [*local-env* *local-env*, *next-local-num* *next-local-num*]
                                    ;; pre-seed env (like Lisp labels)
                                    (let [#_"PersistentVector" lbs
                                            (loop-when [lbs [] #_"int" i 0] (< i (count bindings)) => lbs
                                                (let-when [#_"Object" sym (nth bindings i)] (symbol? sym) => (throw (IllegalArgumentException. (str "Bad binding form, expected symbol, got: " sym)))
                                                    (when (nil? (namespace sym)) => (throw (RuntimeException. (str "Can't let qualified name: " sym)))
                                                        (let [#_"LocalBinding" lb (Compiler'registerLocal sym, (Compiler'tagOf sym), nil, false)]
                                                            (§ set! (:canBeCleared lb) false)
                                                            (recur (conj lbs lb) (+ i 2))
                                                        )
                                                    )
                                                )
                                            )
                                          #_"PersistentVector" bis
                                            (loop-when [bis [] #_"int" i 0] (< i (count bindings)) => bis
                                                (let [#_"Expr" init (Compiler'analyze :Context'EXPRESSION, (nth bindings (inc i)), (:name (nth bindings i)))
                                                      #_"LocalBinding" lb (nth lbs (/ i 2))]
                                                    (§ set! (:init lb) init)
                                                    (recur (conj bis (BindingInit'new lb, init)) (+ i 2))
                                                )
                                            )]
                                        (LetFnExpr'new bis, (.parse (BodyParser'new), context, (next (next form))))
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
        (hash-map
            #_"PersistentVector" :bindingInits bindingInits
            #_"Expr" :body body
            #_"boolean" :isLoop isLoop
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--LetExpr [#_"LetExpr" this]
        (throw (UnsupportedOperationException. "Can't eval let/loop"))
    )

    #_method
    (defn- #_"void" LetExpr''doEmit [#_"LetExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"boolean" emitUnboxed]
        (let [#_"HashMap<BindingInit, Label>" bindingLabels (HashMap.)]
            (dotimes [#_"int" i (count (:bindingInits this))]
                (let [#_"BindingInit" bi (nth (:bindingInits this) i)
                      #_"Class" primc (Compiler'maybePrimitiveType (:init bi))]
                    (if (some? primc)
                        (do
                            (.emitUnboxed (:init bi), :Context'EXPRESSION, objx, gen)
                            (.visitVarInsn gen, (.getOpcode (Type/getType primc), Opcodes/ISTORE), (:idx (:binding bi)))
                        )
                        (do
                            (.emit (:init bi), :Context'EXPRESSION, objx, gen)
                            (if (and (not (:used (:binding bi))) (:canBeCleared (:binding bi)))
                                (.pop gen)
                                (.visitVarInsn gen, (.getOpcode (Type/getType Object), Opcodes/ISTORE), (:idx (:binding bi)))
                            )
                        )
                    )
                    (.put bindingLabels, bi, (.mark gen))
                )
            )
            (let [#_"Label" loopLabel (.mark gen)]
                (if (:isLoop this)
                    (binding [*loop-label* loopLabel]
                        (if emitUnboxed
                            (.emitUnboxed (:body this), context, objx, gen)
                            (.emit (:body this), context, objx, gen)
                        )
                    )
                    (if emitUnboxed
                        (.emitUnboxed (:body this), context, objx, gen)
                        (.emit (:body this), context, objx, gen)
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

    #_override
    (defn #_"void" Expr'''emit--LetExpr [#_"LetExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (LetExpr''doEmit this, context, objx, gen, false)
        nil
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--LetExpr [#_"LetExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (LetExpr''doEmit this, context, objx, gen, true)
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--LetExpr [#_"LetExpr" this]
        (.hasJavaClass (:body this))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--LetExpr [#_"LetExpr" this]
        (.getJavaClass (:body this))
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--LetExpr [#_"LetExpr" this]
        (and (instance? MaybePrimitiveExpr (:body this)) (.canEmitPrimitive (:body this)))
    )
)

(class-ns LetParser
    (defn #_"IParser" LetParser'new []
        (reify IParser
            ;; (let [var val var2 val2 ...] body...)
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"boolean" isLoop (= (first form) 'loop*)]
                    (when (instance? IPersistentVector (second form)) => (throw (IllegalArgumentException. "Bad binding form, expected vector"))
                        (let [#_"IPersistentVector" bindings (second form)]
                            (when (zero? (% (count bindings) 2)) => (throw (IllegalArgumentException. "Bad binding form, expected matched symbol expression pairs"))
                                (if (or (= context :Context'EVAL) (and (= context :Context'EXPRESSION) isLoop))
                                    (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                                    (let [#_"ISeq" body (next (next form))
                                          #_"ObjMethod" method *method*
                                          #_"IPersistentMap" locals' (:locals method)
                                          #_"IPersistentMap" indexLocals' (:indexlocals method)
                                          #_"IPersistentVector" recurMismatches
                                            (loop-when-recur [recurMismatches [] #_"int" i 0]
                                                             (< i (/ (count bindings) 2))
                                                             [(conj recurMismatches false) (inc i)]
                                                          => recurMismatches
                                            )]
                                        ;; may repeat once for each binding with a mismatch, return breaks
                                        (while true
                                            (let [#_"IPersistentMap" dynamicBindings
                                                    (hash-map
                                                        #'*local-env*      *local-env*
                                                        #'*next-local-num* *next-local-num*
                                                    )
                                                  dynamicBindings
                                                    (when isLoop => dynamicBindings
                                                        (assoc dynamicBindings #'*loop-locals* nil)
                                                    )
                                                  _ (§ set! (:locals method) locals')
                                                  _ (§ set! (:indexlocals method) indexLocals')
                                                  #_"PathNode" looproot (PathNode'new :PathType'PATH, *clear-path*)
                                                  #_"PathNode" clearroot (PathNode'new :PathType'PATH, looproot)
                                                  #_"PathNode" clearpath (PathNode'new :PathType'PATH, looproot)]
                                                (try
                                                    (push-thread-bindings dynamicBindings)
                                                    (let [#_"PersistentVector" bindingInits []
                                                          #_"PersistentVector" loopLocals []
                                                          _ (loop-when-recur [#_"int" i 0] (< i (count bindings)) [(+ i 2)]
                                                                (let-when [#_"Object" sym (nth bindings i)] (symbol? sym) => (throw (IllegalArgumentException. (str "Bad binding form, expected symbol, got: " sym)))
                                                                    (when (nil? (namespace sym)) => (throw (RuntimeException. (str "Can't let qualified name: " sym)))
                                                                        (let [#_"Expr" init (Compiler'analyze :Context'EXPRESSION, (nth bindings (inc i)), (:name sym))
                                                                              init
                                                                                (when isLoop => init
                                                                                    (if (and (some? recurMismatches) (nth recurMismatches (/ i 2)))
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
                                                                                )]
                                                                            ;; sequential enhancement of env (like Lisp let*)
                                                                            (try
                                                                                (when isLoop
                                                                                    (push-thread-bindings
                                                                                        (hash-map
                                                                                            #'*clear-path* clearpath
                                                                                            #'*clear-root* clearroot
                                                                                            #'*no-recur*   false
                                                                                        )
                                                                                    )
                                                                                )
                                                                                (let [#_"LocalBinding" lb (Compiler'registerLocal sym, (Compiler'tagOf sym), init, false)]
                                                                                    (ß ass bindingInits (conj bindingInits (BindingInit'new lb, init)))
                                                                                    (when isLoop
                                                                                        (ß ass loopLocals (conj loopLocals lb))
                                                                                    )
                                                                                )
                                                                                (finally
                                                                                    (when isLoop
                                                                                        (pop-thread-bindings)
                                                                                    )
                                                                                )
                                                                            )
                                                                        )
                                                                    )
                                                                )
                                                            )]
                                                        (when isLoop
                                                            (set! *loop-locals* loopLocals)
                                                        )
                                                        (let [#_"boolean" moreMismatches false
                                                              #_"Expr" bodyExpr
                                                                (try
                                                                    (when isLoop
                                                                        (push-thread-bindings
                                                                            (hash-map
                                                                                #'*clear-path*        clearpath
                                                                                #'*clear-root*        clearroot
                                                                                #'*no-recur*          false
                                                                                #'*in-return-context* (and (= context :Context'RETURN) *in-return-context*)
                                                                            )
                                                                        )
                                                                    )
                                                                    (.parse (BodyParser'new), (if isLoop :Context'RETURN context), body)
                                                                    (finally
                                                                        (when isLoop
                                                                            (pop-thread-bindings)
                                                                            (loop-when-recur [#_"int" i 0] (< i (count loopLocals)) [(inc i)]
                                                                                (when (:recurMistmatch (nth loopLocals i))
                                                                                    (ß ass recurMismatches (assoc recurMismatches i true))
                                                                                    (ß ass moreMismatches true)
                                                                                )
                                                                            )
                                                                        )
                                                                    )
                                                                )]
                                                            (when-not moreMismatches
                                                                (ß return (LetExpr'new bindingInits, bodyExpr, isLoop))
                                                            )
                                                        )
                                                    )
                                                    (finally
                                                        (pop-thread-bindings)
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
    )
)

(class-ns RecurExpr
    (defn #_"RecurExpr" RecurExpr'new [#_"IPersistentVector" loopLocals, #_"IPersistentVector" args, #_"int" line]
        (hash-map
            #_"IPersistentVector" :loopLocals loopLocals
            #_"IPersistentVector" :args args
            #_"int" :line line
        )
    )

    #_override
    (defn #_"Object" Expr'''eval--RecurExpr [#_"RecurExpr" this]
        (throw (UnsupportedOperationException. "Can't eval recur"))
    )

    #_override
    (defn #_"void" Expr'''emit--RecurExpr [#_"RecurExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (let-when [#_"Label" loopLabel *loop-label*] (some? loopLabel) => (throw (IllegalStateException.))
            (dotimes [#_"int" i (count (:loopLocals this))]
                (let [#_"LocalBinding" lb (nth (:loopLocals this) i) #_"Expr" arg (nth (:args this) i)]
                    (when (some? (LocalBinding''getPrimitiveType lb)) => (.emit arg, :Context'EXPRESSION, objx, gen)
                        (let [#_"Class" primc (LocalBinding''getPrimitiveType lb) #_"Class" pc (Compiler'maybePrimitiveType arg)]
                            (cond (= primc pc)
                                (do
                                    (.emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                )
                                (and (= primc Long/TYPE) (= pc Integer/TYPE))
                                (do
                                    (.emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                    (.visitInsn gen, Opcodes/I2L)
                                )
                                (and (= primc Double/TYPE) (= pc Float/TYPE))
                                (do
                                    (.emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                    (.visitInsn gen, Opcodes/F2D)
                                )
                                (and (= primc Integer/TYPE) (= pc Long/TYPE))
                                (do
                                    (.emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "int intCast(long)"))
                                )
                                (and (= primc Float/TYPE) (= pc Double/TYPE))
                                (do
                                    (.emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                    (.visitInsn gen, Opcodes/D2F)
                                )
                                :else
                                (do
                                    (throw (IllegalArgumentException. (str "recur arg for primitive local: " (:name lb) " is not matching primitive, had: " (if (.hasJavaClass arg) (.getName (.getJavaClass arg)) "Object") ", needed: " (.getName primc))))
                                )
                            )
                        )
                    )
                )
            )
            (loop-when-recur [#_"int" i (dec (count (:loopLocals this)))] (<= 0 i) [(dec i)]
                (let [#_"LocalBinding" lb (nth (:loopLocals this) i) #_"Class" primc (LocalBinding''getPrimitiveType lb)]
                    (if (:isArg lb)
                        (.storeArg gen, (- (:idx lb) (if (:canBeDirect objx) 0 1)))
                        (.visitVarInsn gen, (.getOpcode (if (some? primc) (Type/getType primc) (Type/getType Object)), Opcodes/ISTORE), (:idx lb))
                    )
                )
            )
            (.goTo gen, loopLabel)
        )
        nil
    )

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--RecurExpr [#_"RecurExpr" this]
        true
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--RecurExpr [#_"RecurExpr" this]
        Recur
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--RecurExpr [#_"RecurExpr" this]
        true
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--RecurExpr [#_"RecurExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emit this, context, objx, gen)
        nil
    )
)

(class-ns RecurParser
    (defn #_"IParser" RecurParser'new []
        (reify IParser
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"IPersistentVector" loopLocals *loop-locals*]
                    (when-not (and (= context :Context'RETURN) (some? loopLocals))
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
                        (when-not (= (count args) (count loopLocals))
                            (throw (IllegalArgumentException. (str "Mismatched argument count to recur, expected: " (count loopLocals) " args, got: " (count args))))
                        )
                        (dotimes [#_"int" i (count loopLocals)]
                            (let [#_"LocalBinding" lb (nth loopLocals i)]
                                (when-let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                                    (let [#_"Class" pc (Compiler'maybePrimitiveType (nth args i))
                                          #_"boolean" mismatch?
                                            (condp = primc
                                                Long/TYPE   (not (any = pc Long/TYPE Integer/TYPE Short/TYPE Character/TYPE Byte/TYPE))
                                                Double/TYPE (not (any = pc Double/TYPE Float/TYPE))
                                                            false
                                            )]
                                        (when mismatch?
                                            (§ set! (:recurMistmatch lb) true)
                                            (when *warn-on-reflection*
                                                (.println *err*, (str "line " line ": recur arg for primitive local: " (:name lb) " is not matching primitive, had: " (if (some? pc) (.getName pc) "Object") ", needed: " (.getName primc)))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                        (RecurExpr'new loopLocals, args, line)
                    )
                )
            )
        )
    )
)

(class-ns NewInstanceMethod
    (def #_"Symbol" NewInstanceMethod'dummyThis 'dummy_this_dlskjsdfower)

    (defn #_"NewInstanceMethod" NewInstanceMethod'new [#_"ObjExpr" objx, #_"ObjMethod" parent]
        (merge (ObjMethod'new objx, parent)
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

    #_override
    (defn #_"int" ObjMethod'''numParams--NewInstanceMethod [#_"NewInstanceMethod" this]
        (count (:argLocals this))
    )

    #_override
    (defn #_"String" ObjMethod'''getMethodName--NewInstanceMethod [#_"NewInstanceMethod" this]
        (:name this)
    )

    #_override
    (defn #_"Type" ObjMethod'''getReturnType--NewInstanceMethod [#_"NewInstanceMethod" this]
        (:retType this)
    )

    #_override
    (defn #_"Type[]" ObjMethod'''getArgTypes--NewInstanceMethod [#_"NewInstanceMethod" this]
        (:argTypes this)
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

    (defn #_"NewInstanceMethod" NewInstanceMethod'parse [#_"ObjExpr" objx, #_"ISeq" form, #_"Symbol" thistag, #_"Map" overrideables]
        ;; (methodname [this-name args*] body...)
        ;; this-name might be nil
        (let [#_"NewInstanceMethod" nim
                (-> (NewInstanceMethod'new objx, *method*)
                    (assoc :line *line*)
                )
              #_"Symbol" dotname (first form) #_"Symbol" name (with-meta (symbol (Compiler'munge (:name dotname))) (meta dotname))
              #_"IPersistentVector" parms (second form)]
            (when (pos? (count parms)) => (throw (IllegalArgumentException. (str "Must supply at least one argument for 'this' in: " dotname)))
                (let [#_"Symbol" thisName (nth parms 0)
                      parms (subvec parms 1 (count parms))
                      #_"ISeq" body (next (next form))
                      ;; register as the current method and set up a new env frame
                      #_"PathNode" pnode (PathNode'new :PathType'PATH, *clear-path*)]
                    (binding [*method*            nim
                              *local-env*         *local-env*
                              *next-local-num*    0
                              *loop-locals*       nil
                              *clear-path*        pnode
                              *clear-root*        pnode
                              *clear-sites*       {}
                              *in-return-context* true]
                        ;; register 'this' as local 0
                        (if (some? thisName)
                            (Compiler'registerLocal (or thisName NewInstanceMethod'dummyThis), thistag, nil, false)
                            (Compiler'getAndIncLocalNum)
                        )
                        (let [nim (assoc nim :retClass (HostExpr'tagClass (Compiler'tagOf name)))
                              nim (assoc nim :argTypes (make-array Type (count parms)))
                              #_"Class[]" pclasses (make-array Class (count parms))
                              #_"Symbol[]" psyms (make-array #_"Symbol" Object (count parms))
                              #_"boolean" hinted?
                                (loop-when [hinted? (some? (Compiler'tagOf name)) #_"int" i 0] (< i (count parms)) => hinted?
                                    (let-when [#_"Object" sym (nth parms i)] (symbol? sym) => (throw (IllegalArgumentException. "params must be Symbols"))
                                        (let [#_"Object" tag (Compiler'tagOf sym) hinted? (or hinted? (some? tag))]
                                            (aset pclasses i (HostExpr'tagClass tag))
                                            (aset psyms i (if (some? (namespace sym)) (symbol (:name sym)) sym))
                                            (recur hinted? (inc i))
                                        )
                                    )
                                )
                              #_"Map" matches (NewInstanceMethod'findMethodsWithNameAndArity (:name name), (count parms), overrideables)
                              #_"Object" mk (NewInstanceMethod'msig (:name name), pclasses)
                              [nim pclasses #_"java.lang.reflect.Method" m]
                                (case (count matches)
                                    0   (throw (IllegalArgumentException. (str "Can't define method not in interfaces: " (:name name))))
                                    1   (if hinted? ;; validate match
                                            (let [m (get matches mk)]
                                                (when (nil? m)
                                                    (throw (IllegalArgumentException. (str "Can't find matching method: " (:name name) ", leave off hints for auto match.")))
                                                )
                                                (when-not (= (.getReturnType m) (:retClass nim))
                                                    (throw (IllegalArgumentException. (str "Mismatched return type: " (:name name) ", expected: " (.getName (.getReturnType m)) ", had: " (.getName (:retClass nim)))))
                                                )
                                                [nim pclasses m]
                                            )
                                            ;; adopt found method sig
                                            (let [m (.next (.iterator (.values matches)))]
                                                [(assoc nim :retClass (.getReturnType m)) (.getParameterTypes m) m]
                                            )
                                        )
                                        ;; must be hinted and match one method
                                        (when hinted? => (throw (IllegalArgumentException. (str "Must hint overloaded method: " (:name name))))
                                            (let [m (get matches mk)]
                                                (when (nil? m)
                                                    (throw (IllegalArgumentException. (str "Can't find matching overloaded method: " (:name name))))
                                                )
                                                (when-not (= (.getReturnType m) (:retClass nim))
                                                    (throw (IllegalArgumentException. (str "Mismatched return type: " (:name name) ", expected: " (.getName (.getReturnType m)) ", had: " (.getName (:retClass nim)))))
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
                                    (Compiler'getAndIncLocalNum)
                                )
                            )
                            (set! *loop-locals* argLocals)
                            (assoc nim
                                :name (:name name)
                                :methodMeta (meta name)
                                :parms parms
                                :argLocals argLocals
                                :body (.parse (BodyParser'new), :Context'RETURN, body)
                            )
                        )
                    )
                )
            )
        )
    )

    #_override
    (defn #_"void" Expr'''emit--NewInstanceMethod [#_"NewInstanceMethod" this, #_"ObjExpr" obj, #_"ClassVisitor" cv]
        (let [#_"Method" m (Method. (.getMethodName this), (.getReturnType this), (.getArgTypes this))
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
                    (ObjMethod'emitBody (:objx this), gen, (:retClass this), (:body this))
                    (let [#_"Label" end (.mark gen)]
                        (.visitLocalVariable gen, "this", (.getDescriptor (:objtype obj)), nil, loopLabel, end, 0)
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

(class-ns NewInstanceExpr
    (defn- #_"NewInstanceExpr" NewInstanceExpr'new [#_"Object" tag]
        (merge (ObjExpr'new tag)
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
            (loop-when-recur [#_"ISeq" s (keys (:closes ret))] (some? s) [(next s)]
                (let [#_"LocalBinding" lb (first s)
                      #_"int" access (| Opcodes/ACC_PUBLIC (if (ObjExpr''isVolatile ret, lb) Opcodes/ACC_VOLATILE (if (ObjExpr''isMutable ret, lb) 0 Opcodes/ACC_FINAL)))]
                    (if (some? (LocalBinding''getPrimitiveType lb))
                        (.visitField cv, access, (:name lb), (.getDescriptor (Type/getType (LocalBinding''getPrimitiveType lb))), nil, nil)
                        ;; todo - when closed-overs are fields, use more specific types here and in ctor and emitLocal?
                        (.visitField cv, access, (:name lb), (.getDescriptor (Type/getType Object)), nil, nil)
                    )
                )
            )

            ;; ctor that takes closed-overs and does nothing
            (let [#_"Method" m (Method. "<init>", Type/VOID_TYPE, (ObjExpr''ctorTypes ret))
                  #_"GeneratorAdapter" ctorgen (GeneratorAdapter. Opcodes/ACC_PUBLIC, m, nil, nil, cv)]
                (.visitCode ctorgen)
                (.loadThis ctorgen)
                (.invokeConstructor ctorgen, (Type/getObjectType superName), (Method/getMethod "void <init>()"))
                (.returnValue ctorgen)
                (.endMethod ctorgen)
            )

            (when (pos? (:altCtorDrops ret))
                (let [#_"Type[]" ctorTypes (ObjExpr''ctorTypes ret)]

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

            (.defineClass *loader*, (str Compiler'COMPILE_STUB_PREFIX "." (:name ret)), (.toByteArray cw), form)
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

    #_override
    (defn #_"void" ObjExpr'''emitStatics--NewInstanceExpr [#_"NewInstanceExpr" this, #_"ClassVisitor" cv]
        (when (ObjExpr''isDeftype this)
            ;; getBasis()
            (let [#_"Method" meth (Method/getMethod "clojure.lang.IPersistentVector getBasis()")
                  #_"GeneratorAdapter" gen (GeneratorAdapter. (| Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), meth, nil, nil, cv)]
                (ObjExpr''emitValue this, (:hintedFields this), gen)
                (.returnValue gen)
                (.endMethod gen)

                (let-when [#_"int" n (count (:hintedFields this))] (< n (count (:fields this)))
                    ;; create(IPersistentMap)
                    (let [#_"String" className (.replace (:name this), \., \/)
                          #_"MethodVisitor" mv (.visitMethod cv, (| Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "create", (str "(Lclojure/lang/IPersistentMap;)L" className ";"), nil, nil)]
                        (.visitCode mv)

                        (loop-when-recur [#_"ISeq" s (seq (:hintedFields this)) #_"int" i 1] (some? s) [(next s) (inc i)]
                            (let [#_"String" bName (:name (first s))
                                  #_"Class" k (HostExpr'tagClass (Compiler'tagOf (first s)))]
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

                        (let [#_"Method" ctor (Method. "<init>", Type/VOID_TYPE, (ObjExpr''ctorTypes this))]
                            (dotimes [#_"int" i n]
                                (.visitVarInsn mv, Opcodes/ALOAD, (inc i))
                                (let-when [#_"Class" k (HostExpr'tagClass (Compiler'tagOf (nth (:hintedFields this) i)))] (.isPrimitive k)
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

    #_override
    (defn #_"void" ObjExpr'''emitMethods--NewInstanceExpr [#_"NewInstanceExpr" this, #_"ClassVisitor" cv]
        (loop-when-recur [#_"ISeq" s (seq (:methods this))] (some? s) [(next s)]
            (.emit (first s), this, cv)
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

    (defn #_"Map[]" NewInstanceExpr'gatherMethods [#_"Class" sc, #_"ISeq" ifaces]
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
            (ß new Map[] (object-array [ methods, covariants ]))
        )
    )

    (defn #_"ObjExpr" NewInstanceExpr'build [#_"IPersistentVector" interfaceSyms, #_"IPersistentVector" fieldSyms, #_"Symbol" thisSym, #_"String" tagName, #_"Symbol" className, #_"Symbol" typeTag, #_"ISeq" methodForms, #_"ISeq" form, #_"IPersistentMap" opts]
        (let [#_"String" name (.toString className) #_"String" name' (.replace name, \., \/)
              #_"NewInstanceExpr" nie
                (-> (NewInstanceExpr'new nil)
                    (assoc :src form :name name :classMeta (meta className) :internalName name' :objtype (Type/getObjectType name') :opts opts)
                )
              nie (if (some? thisSym) (assoc nie :thisName (:name thisSym)) nie)
              nie
                (when (some? fieldSyms) => nie
                    (let [#_"Object[]" a (make-array Object (* 2 (count fieldSyms)))
                          #_"IPersistentMap" fmap
                            (loop-when [fmap {} #_"int" i 0] (< i (count fieldSyms)) => fmap
                                (let [#_"Symbol" sym (nth fieldSyms i)
                                      #_"LocalBinding" lb (LocalBinding'new -1, sym, nil, (MethodParamExpr'new (HostExpr'tagClass (Compiler'tagOf sym))), false, nil)]
                                    (aset a (* i 2) lb)
                                    (aset a (inc (* i 2)) lb)
                                    (recur (assoc fmap sym lb) (inc i))
                                )
                            )
                          ;; todo - inject __meta et al into closes - when?
                          ;; use array map to preserve ctor order
                          _ (§ set! (:closes nie) (PersistentArrayMap'new a))
                          nie (assoc nie :fields fmap)]
                        (loop-when-recur [nie nie #_"int" i (dec (count fieldSyms))]
                                         (and (<= 0 i) (any = (:name (nth fieldSyms i)) "__meta" "__extmap" "__hash" "__hasheq"))
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
              #_"Map[]" mc (NewInstanceExpr'gatherMethods super, (seq ifaces))
              #_"Map" overrideables (aget mc 0) #_"Map" covariants (aget mc 1)
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
                                (when (ObjExpr''isDeftype nie) => nie
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
                                :constantsID (RT/nextID)
                                :keywordCallsites *keyword-callsites*
                                :protocolCallsites *protocol-callsites*
                            )
                        )
                        (finally
                            (when (ObjExpr''isDeftype nie)
                                (pop-thread-bindings)
                            )
                        )
                    )
                )]
            (ObjExpr''compile nie, (NewInstanceExpr'slashname super), inames, false)
            (ObjExpr''getCompiledClass nie)
            nie
        )
    )
)

(class-ns ReifyParser
    (defn #_"IParser" ReifyParser'new []
        (reify IParser
            ;; (reify this-name? [interfaces] (method-name [args] body)*)
            #_override
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"ISeq" s form                                                  s (next s)
                      #_"IPersistentVector" ifaces (conj (first s) 'clojure.lang.IObj) s (next s)
                      #_"String" classname
                        (let [#_"ObjMethod" owner *method*
                              #_"String" basename (if (some? owner) (ObjExpr'trimGenID (:name (:objx owner))) (Compiler'munge (:name (:name *ns*))))]
                            (str basename "$" "reify__" (RT/nextID))
                        )
                      #_"ObjExpr" nie (NewInstanceExpr'build ifaces, nil, nil, classname, (symbol classname), nil, s, form, nil)]
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
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
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

    #_override
    (defn #_"boolean" Expr'''hasJavaClass--CaseExpr [#_"CaseExpr" this]
        (some? (:returnType this))
    )

    #_override
    (defn #_"boolean" MaybePrimitiveExpr'''canEmitPrimitive--CaseExpr [#_"CaseExpr" this]
        (Reflector'isPrimitive (:returnType this))
    )

    #_override
    (defn #_"Class" Expr'''getJavaClass--CaseExpr [#_"CaseExpr" this]
        (:returnType this)
    )

    #_override
    (defn #_"Object" Expr'''eval--CaseExpr [#_"CaseExpr" this]
        (throw (UnsupportedOperationException. "Can't eval case"))
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
    (defn- #_"void" CaseExpr''emitExprForInts [#_"CaseExpr" this, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Type" exprType, #_"Label" defaultLabel]
        (cond (nil? exprType)
            (do
                (when *warn-on-reflection*
                    (.println *err*, (str "Performance warning, line " (:line this) " - case has int tests, but tested expression is not primitive."))
                )
                (.emit (:expr this), :Context'EXPRESSION, objx, gen)
                (.instanceOf gen, (Type/getType Number))
                (.ifZCmp gen, GeneratorAdapter/EQ, defaultLabel)
                (.emit (:expr this), :Context'EXPRESSION, objx, gen)
                (.checkCast gen, (Type/getType Number))
                (.invokeVirtual gen, (Type/getType Number), (Method/getMethod "int intValue()"))
                (CaseExpr''emitShiftMask this, gen)
            )
            (or (= exprType Type/LONG_TYPE) (= exprType Type/INT_TYPE) (= exprType Type/SHORT_TYPE) (= exprType Type/BYTE_TYPE))
            (do
                (.emitUnboxed (:expr this), :Context'EXPRESSION, objx, gen)
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

    (defn- #_"void" CaseExpr'emitExpr [#_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Expr" expr, #_"boolean" emitUnboxed]
        (if (and emitUnboxed (instance? MaybePrimitiveExpr expr))
            (.emitUnboxed expr, :Context'EXPRESSION, objx, gen)
            (.emit expr, :Context'EXPRESSION, objx, gen)
        )
        nil
    )

    #_method
    (defn- #_"void" CaseExpr''emitThenForInts [#_"CaseExpr" this, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Type" exprType, #_"Expr" test, #_"Expr" then, #_"Label" defaultLabel, #_"boolean" emitUnboxed]
        (cond (nil? exprType)
            (do
                (.emit (:expr this), :Context'EXPRESSION, objx, gen)
                (.emit test, :Context'EXPRESSION, objx, gen)
                (.invokeStatic gen, (Type/getType Util), (Method/getMethod "boolean equiv(Object, Object)"))
                (.ifZCmp gen, GeneratorAdapter/EQ, defaultLabel)
                (CaseExpr'emitExpr objx, gen, then, emitUnboxed)
            )
            (= exprType Type/LONG_TYPE)
            (do
                (.emitUnboxed test, :Context'EXPRESSION, objx, gen)
                (.emitUnboxed (:expr this), :Context'EXPRESSION, objx, gen)
                (.ifCmp gen, Type/LONG_TYPE, GeneratorAdapter/NE, defaultLabel)
                (CaseExpr'emitExpr objx, gen, then, emitUnboxed)
            )
            (or (= exprType Type/INT_TYPE) (= exprType Type/SHORT_TYPE) (= exprType Type/BYTE_TYPE))
            (do
                (when (CaseExpr''isShiftMasked this)
                    (.emitUnboxed test, :Context'EXPRESSION, objx, gen)
                    (.emitUnboxed (:expr this), :Context'EXPRESSION, objx, gen)
                    (.cast gen, exprType, Type/LONG_TYPE)
                    (.ifCmp gen, Type/LONG_TYPE, GeneratorAdapter/NE, defaultLabel)
                )
                ;; else direct match
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
    (defn- #_"void" CaseExpr''emitExprForHashes [#_"CaseExpr" this, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (.emit (:expr this), :Context'EXPRESSION, objx, gen)
        (.invokeStatic gen, (Type/getType Util), (Method/getMethod "int hash(Object)"))
        (CaseExpr''emitShiftMask this, gen)
        nil
    )

    #_method
    (defn- #_"void" CaseExpr''emitThenForHashes [#_"CaseExpr" this, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"Expr" test, #_"Expr" then, #_"Label" defaultLabel, #_"boolean" emitUnboxed]
        (.emit (:expr this), :Context'EXPRESSION, objx, gen)
        (.emit test, :Context'EXPRESSION, objx, gen)
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
    (defn- #_"void" CaseExpr''doEmit [#_"CaseExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen, #_"boolean" emitUnboxed]
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

    #_override
    (defn #_"void" Expr'''emit--CaseExpr [#_"CaseExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (CaseExpr''doEmit this, context, objx, gen, false)
        nil
    )

    #_override
    (defn #_"void" MaybePrimitiveExpr'''emitUnboxed--CaseExpr [#_"CaseExpr" this, #_"Context" context, #_"ObjExpr" objx, #_"GeneratorAdapter" gen]
        (CaseExpr''doEmit this, context, objx, gen, true)
        nil
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
            (#_"Expr" parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
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
                          #_"LocalBindingExpr" testexpr (Compiler'analyze :Context'EXPRESSION, exprForm)
                          _ (§ set! (:shouldClear testexpr) false)
                          #_"SortedMap<Integer, Expr>" tests (TreeMap.)
                          #_"HashMap<Integer, Expr>" thens (HashMap.)
                          #_"PathNode" branch (PathNode'new :PathType'BRANCH, *clear-path*)
                          _ (doseq [#_"Map$Entry" e (.entrySet caseMap)]
                                (let [#_"Integer" minhash (.intValue (key e)) #_"Object" pair (val e) ;; [test-val then-expr]
                                      #_"Expr" testExpr
                                        (if (= testType :int)
                                            (NumberExpr'parse (.intValue (first pair)))
                                            (ConstantExpr'new (first pair))
                                        )]
                                    (.put tests, minhash, testExpr)
                                    (let [#_"Expr" thenExpr
                                            (binding [*clear-path* (PathNode'new :PathType'PATH, branch)]
                                                (Compiler'analyze context, (second pair))
                                            )]
                                        (.put thens, minhash, thenExpr)
                                    )
                                )
                            )
                          #_"Expr" defaultExpr
                            (binding [*clear-path* (PathNode'new :PathType'PATH, branch)]
                                (Compiler'analyze context, (nth args 3))
                            )]
                        (CaseExpr'new *line*, testexpr, shift, mask, low, high, defaultExpr, tests, thens, switchType, testType, skipCheck)
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
                            (let [#_"String" name (:name op)]
                                ;; (.substring s 2 5) => (. s substring 2 5)
                                (cond
                                    (= (.charAt name, 0) \.)
                                        (when (< 1 (count form)) => (throw (IllegalArgumentException. "Malformed member expression, expecting (.member target ...)"))
                                            (let [#_"Object" target (second form)
                                                  target
                                                    (when (some? (HostExpr'maybeClass target, false)) => target
                                                        (with-meta (list 'clojure.core/identity target) { :tag 'Class })
                                                    )]
                                                (Compiler'preserveTag form, (list* '. target (symbol (.substring name, 1)) (next (next form))))
                                            )
                                        )
                                    (Compiler'namesStaticMember op)
                                        (let-when [#_"Symbol" target (symbol (:ns op))] (some? (HostExpr'maybeClass target, false)) => form
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
                    (nil? (:ns sym)) ;; ns-qualified syms are always Vars
                        (when-let [#_"LocalBinding" b (Compiler'referenceLocal sym)]
                            (LocalBindingExpr'new b, tag)
                        )
                    (nil? (Compiler'namespaceFor sym))
                        (when-let [#_"Class" c (HostExpr'maybeClass (symbol (:ns sym)), false)]
                            (when (some? (Reflector'getField c, (:name sym), true)) => (throw (RuntimeException. (str "Unable to find static field: " (:name sym) " in " c)))
                                (StaticFieldExpr'new *line*, c, (:name sym), tag)
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
                                            (.parse p, context, form)
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
                        (or (instance? IType form) (and (instance? IPersistentCollection form) (not (and (symbol? (first form)) (.startsWith (:name (first form)), "def")))))
                            (let [#_"ObjExpr" fexpr (Compiler'analyze :Context'EXPRESSION, (list 'fn* [] form), (str "eval" (RT/nextID)))]
                                (.invoke (.eval fexpr))
                            )
                        :else
                            (let [#_"Expr" expr (Compiler'analyze :Context'EVAL, form)]
                                (.eval expr)
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
        (let [#_"LineNumberingPushbackReader" r (if (instance? LineNumberingPushbackReader reader) reader (LineNumberingPushbackReader. reader))]
            (binding [*ns* *ns*, *warn-on-reflection* *warn-on-reflection*, *line* 0]
                (let [#_"Object" EOF (Object.)]
                    (loop [#_"Object" val nil]
                        (Compiler'consumeWhitespaces r)
                        (let-when [#_"Object" form (LispReader/read r, false, EOF, false, (§ obsolete nil))] (not= form EOF) => val
                            (recur (Compiler'eval form))
                        )
                    )
                )
            )
        )
    )
)
)
