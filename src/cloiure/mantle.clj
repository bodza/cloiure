(ns #_cloiure.slang cloiure.mantle
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

#_(ns cloiure.mantle
    (:refer-clojure :exclude [when when-not])
    (:use [cloiure slang]))

#_(ns cloiure.core)

(§ def unquote)
(§ def unquote-splicing)

;;;
 ; Creates a new list containing the items.
 ;;
(§ def list (cloiure.lang.PersistentList/creator))

;;;
 ; Returns a new seq where x is the first element and seq is the rest.
 ;;
(§ def cons (fn* cons [x seq] (cloiure.lang.RT/cons x seq)))

;; during bootstrap we don't have destructuring let, loop or fn, will redefine later

(§ def ^:macro let  (fn* let  [&form &env & decl] (cons 'let* decl)))
(§ def ^:macro loop (fn* loop [&form &env & decl] (cons 'loop* decl)))
(§ def ^:macro fn   (fn* fn   [&form &env & decl] (.withMeta ^cloiure.lang.IObj (cons 'fn* decl) (.meta ^cloiure.lang.IMeta &form))))

;;;
 ; Returns the first item in the collection. Calls seq on its argument. If coll is nil, returns nil.
 ;;
(§ def first (fn first [coll] (cloiure.lang.RT/first coll)))

;;;
 ; Returns a seq of the items after the first. Calls seq on its argument. If there are no more items, returns nil.
 ;;
(§ def ^cloiure.lang.ISeq next (fn next [x] (cloiure.lang.RT/next x)))

;;;
 ; Returns a possibly empty seq of the items after the first. Calls seq on its argument.
 ;;
(§ def ^cloiure.lang.ISeq rest (fn rest [x] (cloiure.lang.RT/more x)))

;;;
 ; conj[oin].
 ; Returns a new collection with the xs 'added'. (conj nil item) returns (item).
 ; The 'addition' may happen at different 'places' depending on the concrete type.
 ;;
(§ def conj
    (fn conj
        ([] [])
        ([coll] coll)
        ([coll x] (cloiure.lang.RT/conj coll x))
        ([coll x & xs]
            (if xs
                (recur (cloiure.lang.RT/conj coll x) (first xs) (next xs))
                (cloiure.lang.RT/conj coll x)
            )
        )
    )
)

(§ def second (fn second [x] (first (next x))))
(§ def ffirst (fn ffirst [x] (first (first x))))
(§ def nfirst (fn nfirst [x] (next  (first x))))
(§ def fnext  (fn fnext  [x] (first (next x))))
(§ def nnext  (fn nnext  [x] (next  (next x))))

;;;
 ; Returns a seq on the collection. If the collection is empty, returns nil.
 ; (seq nil) returns nil. seq also works on Strings, native Java arrays (of reference types)
 ; and any objects that implement Iterable. Note that seqs cache values, thus seq should not
 ; be used on any Iterable whose iterator repeatedly returns the same mutable object.
 ;;
(§ def ^cloiure.lang.ISeq seq (fn seq [coll] (cloiure.lang.RT/seq coll)))

;;;
 ; Evaluates x and tests if it is an instance of the class c. Returns true or false.
 ;;
(§ def instance? (fn instance? [^Class c x] (.isInstance c x)))

(§ def seq?    (fn seq?    [x] (instance? cloiure.lang.ISeq x)))
(§ def char?   (fn char?   [x] (instance? Character x)))
(§ def string? (fn string? [x] (instance? String x)))
(§ def map?    (fn map?    [x] (instance? cloiure.lang.IPersistentMap x)))
(§ def vector? (fn vector? [x] (instance? cloiure.lang.IPersistentVector x)))

;;;
 ; assoc[iate].
 ; When applied to a map, returns a new map of the same (hashed/sorted) type, that contains the mapping of key(s) to val(s).
 ; When applied to a vector, returns a new vector that contains val at index. Note - index must be <= (count vector).
 ;;
(§ def assoc
    (fn assoc
        ([map key val] (cloiure.lang.RT/assoc map key val))
        ([map key val & kvs]
            (let [ret (cloiure.lang.RT/assoc map key val)]
                (if kvs
                    (if (next kvs)
                        (recur ret (first kvs) (second kvs) (nnext kvs))
                        (throw (IllegalArgumentException. "assoc expects even number of arguments after map/vector, found odd number"))
                    )
                    ret
                )
            )
        )
    )
)

;;;
 ; Returns the metadata of obj, returns nil if there is no metadata.
 ;;
(§ def meta (fn meta [x] (if (instance? cloiure.lang.IMeta x) (.meta ^cloiure.lang.IMeta x))))

;;;
 ; Returns an object of the same type and value as obj, with map m as its metadata.
 ;;
(§ def with-meta (fn with-meta [^cloiure.lang.IObj x m] (.withMeta x m)))

(§ def ^:private ^:dynamic assert-valid-fdecl (fn [fdecl]))

(§ def ^:private sigs
    (fn [fdecl]
        (assert-valid-fdecl fdecl)
        (let [asig
                (fn [fdecl]
                    (let [arglist (first fdecl)
                          ;; elide implicit macro args
                          arglist
                            (if (cloiure.lang.Util/equals '&form (first arglist))
                                (cloiure.lang.RT/subvec arglist 2 (cloiure.lang.RT/count arglist))
                                arglist
                            )
                          body (next fdecl)]
                        (if (map? (first body))
                            (if (next body)
                                (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                                arglist
                            )
                            arglist
                        )
                    )
                )
              resolve-tag
                (fn [argvec]
                    (let [m (meta argvec) ^cloiure.lang.Symbol tag (:tag m)]
                        (if (instance? cloiure.lang.Symbol tag)
                            (if (cloiure.lang.Util/equiv (.indexOf (.getName tag) ".") -1)
                                (if (cloiure.lang.Util/equals nil (cloiure.lang.Compiler$HostExpr/maybeSpecialTag tag))
                                    (let [c (cloiure.lang.Compiler$HostExpr/maybeClass tag false)]
                                        (if c
                                            (with-meta argvec (assoc m :tag (cloiure.lang.Symbol/intern (.getName c))))
                                            argvec
                                        )
                                    )
                                    argvec
                                )
                                argvec
                            )
                            argvec
                        )
                    )
                )]
            (if (seq? (first fdecl))
                (loop [ret [] fdecls fdecl]
                    (if fdecls
                        (recur (conj ret (resolve-tag (asig (first fdecls)))) (next fdecls))
                        (seq ret)
                    )
                )
                (list (resolve-tag (asig fdecl)))
            )
        )
    )
)

;;;
 ; Return the last item in coll, in linear time.
 ;;
(§ def last (fn last [s] (if (next s) (recur (next s)) (first s))))

;;;
 ; Return a seq of all but the last item in coll, in linear time.
 ;;
(§ def butlast (fn butlast [s] (loop [ret [] s s] (if (next s) (recur (conj ret (first s)) (next s)) (seq ret)))))

;;;
 ; Same as (def name (fn [params* ] exprs*)) or (def name (fn ([params* ] exprs*)+)) with any doc-string or attrs added to the var metadata.
 ;;
(§ def defn
    (fn defn [&form &env name & fdecl]
        ;; note: cannot delegate this check to def because of the call to (with-meta name ..)
        (if (instance? cloiure.lang.Symbol name) nil (throw (IllegalArgumentException. "first argument to defn must be a symbol")))
        (let [m     (if (string? (first fdecl)) {:doc (first fdecl)}   {})
              fdecl (if (string? (first fdecl)) (next fdecl)           fdecl)
              m     (if (map?    (first fdecl)) (conj m (first fdecl)) m)
              fdecl (if (map?    (first fdecl)) (next fdecl)           fdecl)
              fdecl (if (vector? (first fdecl)) (list fdecl)           fdecl)
              m     (if (map?    (last fdecl))  (conj m (last fdecl))  m)
              fdecl (if (map?    (last fdecl))  (butlast fdecl)        fdecl)
              m     (conj {:arglists (list 'quote (sigs fdecl))} m)
              m     (let [inline (:inline m) ifn (first inline) iname (second inline)]
                        ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
                        (if (if (cloiure.lang.Util/equiv 'fn ifn) (if (instance? cloiure.lang.Symbol iname) false true))
                            ;; inserts the same fn name to the inline fn if it does not have one
                            (assoc m :inline
                                (cons ifn (cons (cloiure.lang.Symbol/intern (.concat (.getName ^cloiure.lang.Symbol name) "__inliner")) (next inline)))
                            )
                            m
                        )
                    )
              m     (conj (if (meta name) (meta name) {}) m)]
            (list 'def (with-meta name m)
                ;; todo - restore propagation of fn name
                ;; must figure out how to convey primitive hints to self calls first
                ;; (cons `fn fdecl)
                (with-meta (cons `fn fdecl) {:rettag (:tag m)})
            )
        )
    )
)

(§ .setMacro (var defn))

;;;
 ; Returns an array of Objects containing the contents of coll, which
 ; can be any Collection. Maps to java.util.Collection.toArray().
 ;;
(§ defn ^"[Ljava.lang.Object;" to-array [coll] (cloiure.lang.RT/toArray coll))

;;;
 ; Throws a ClassCastException if x is not a c, else returns x.
 ;;
(§ defn cast [^Class c x] (.cast c x))

;;;
 ; Creates a new vector containing the args.
 ;;
(§ defn vector
    ([] [])
    ([a] [a])
    ([a b] [a b])
    ([a b c] [a b c])
    ([a b c d] [a b c d])
    ([a b c d e] [a b c d e])
    ([a b c d e f] [a b c d e f])
    ([a b c d e f & args] (cloiure.lang.LazilyPersistentVector/create (cons a (cons b (cons c (cons d (cons e (cons f args))))))))
)

;;;
 ; Creates a new vector containing the contents of coll. Java arrays
 ; will be aliased and should not be modified.
 ;;
(§ defn vec [coll]
    (if (vector? coll)
        (if (instance? cloiure.lang.IObj coll)
            (with-meta coll nil)
            (cloiure.lang.LazilyPersistentVector/create coll)
        )
        (cloiure.lang.LazilyPersistentVector/create coll)
    )
)

;;;
 ; keyval => key val
 ; Returns a new hash map with supplied mappings. If any keys are
 ; equal, they are handled as if by repeated uses of assoc.
 ;;
(§ defn hash-map
    ([] {})
    ([& keyvals] (cloiure.lang.PersistentHashMap/create keyvals))
)

;;;
 ; Returns a new hash set with supplied keys. Any equal keys are
 ; handled as if by repeated uses of conj.
 ;;
(§ defn hash-set
    ([] #{})
    ([& keys] (cloiure.lang.PersistentHashSet/create keys))
)

;;;
 ; keyval => key val
 ; Returns a new sorted map with supplied mappings. If any keys are
 ; equal, they are handled as if by repeated uses of assoc.
 ;;
(§ defn sorted-map [& keyvals]
    (cloiure.lang.PersistentTreeMap/create keyvals)
)

;;;
 ; keyval => key val
 ; Returns a new sorted map with supplied mappings, using the supplied
 ; comparator. If any keys are equal, they are handled as if by
 ; repeated uses of assoc.
 ;;
(§ defn sorted-map-by [comparator & keyvals]
    (cloiure.lang.PersistentTreeMap/create comparator keyvals)
)

;;;
 ; Returns a new sorted set with supplied keys. Any equal keys are
 ; handled as if by repeated uses of conj.
 ;;
(§ defn sorted-set [& keys]
    (cloiure.lang.PersistentTreeSet/create keys)
)

;;;
 ; Returns a new sorted set with supplied keys, using the supplied
 ; comparator. Any equal keys are handled as if by repeated uses of
 ; conj.
 ;;
(§ defn sorted-set-by [comparator & keys]
    (cloiure.lang.PersistentTreeSet/create comparator keys)
)

;;;
 ; Returns true if x is nil, false otherwise.
 ;;
(§ defn ^Boolean nil?
    {:inline (fn [x] (list 'cloiure.lang.Util/identical x nil))}
    [x] (cloiure.lang.Util/identical x nil)
)

;;;
 ; Like defn, but the resulting function name is declared as a macro
 ; and will be used as a macro by the compiler when it is called.
 ;;
(§ def defmacro
    (fn [&form &env name & args]
        (let [prefix
                (loop [p (list name) args args]
                    (let [f (first args)]
                        (if (string? f)
                            (recur (cons f p) (next args))
                            (if (map? f)
                                (recur (cons f p) (next args))
                                p
                            )
                        )
                    )
                )
              fdecl
                (loop [fd args]
                    (if (string? (first fd))
                        (recur (next fd))
                        (if (map? (first fd))
                            (recur (next fd))
                            fd
                        )
                    )
                )
              fdecl (if (vector? (first fdecl)) (list fdecl) fdecl)
              add-implicit-args
                (fn [fd]
                    (let [args (first fd)]
                        (cons (vec (cons '&form (cons '&env args))) (next fd))
                    )
                )
              add-args
                (fn [acc ds]
                    (if (nil? ds)
                        acc
                        (let [d (first ds)]
                            (if (map? d)
                                (conj acc d)
                                (recur (conj acc (add-implicit-args d)) (next ds))
                            )
                        )
                    )
                )
              fdecl (seq (add-args [] fdecl))
              decl
                (loop [p prefix d fdecl]
                    (if p
                        (recur (next p) (cons (first p) d))
                        d
                    )
                )]
            (list 'do (cons `defn decl) (list '. (list 'var name) '(setMacro)) (list 'var name))
        )
    )
)

(§ .setMacro (var defmacro))

;;;
 ; Evaluates test. If logical true, evaluates body in an implicit do.
 ;;
(§ defmacro when [test & body] (list 'if test (cons 'do body)))

;;;
 ; Evaluates test. If logical false, evaluates body in an implicit do.
 ;;
(§ defmacro when-not [test & body] (list 'if test nil (cons 'do body)))

;;;
 ; Returns true if x is the value false, false otherwise.
 ;;
(§ defn ^Boolean false? [x] (cloiure.lang.Util/identical x false))

;;;
 ; Returns true if x is the value true, false otherwise.
 ;;
(§ defn ^Boolean true? [x] (cloiure.lang.Util/identical x true))

;;;
 ; Return true if x is a Boolean.
 ;;
(§ defn boolean? [x] (instance? Boolean x))

;;;
 ; Returns true if x is logical false, false otherwise.
 ;;
(§ defn ^Boolean not [x] (if x false true))

;;;
 ; Returns true if x is not nil, false otherwise.
 ;;
(§ defn ^Boolean some? [x] (not (nil? x)))

;;;
 ; Returns true given any argument.
 ;;
(§ defn ^Boolean any? [x] true)

;;;
 ; With no args, returns the empty string. With one arg x, returns x.toString().
 ; (str nil) returns the empty string.
 ; With more than one arg, returns the concatenation of the str values of the args.
 ;;
(§ defn ^String str
    ([] "")
    ([^Object x] (if (nil? x) "" (.toString x)))
    ([x & ys]
        ((fn [^StringBuilder sb more]
            (if more
                (recur (.append sb (str (first more))) (next more))
                (str sb)
            ))
            (StringBuilder. (str x)) ys
        )
    )
)

;;;
 ; Return true if x is a Symbol.
 ;;
(§ defn symbol? [x] (instance? cloiure.lang.Symbol x))

;;;
 ; Return true if x is a Keyword.
 ;;
(§ defn keyword? [x] (instance? cloiure.lang.Keyword x))

;;;
 ; Returns a Symbol with the given namespace and name.
 ;;
(§ defn ^cloiure.lang.Symbol symbol
    ([name] (if (symbol? name) name (cloiure.lang.Symbol/intern name)))
    ([ns name] (cloiure.lang.Symbol/intern ns name))
)

;;;
 ; Returns a new symbol with a unique name. If a prefix string is supplied,
 ; the name is prefix# where # is some unique number.
 ; If prefix is not supplied, the prefix is 'G__'.
 ;;
(§ defn gensym
    ([] (gensym "G__"))
    ([prefix] (cloiure.lang.Symbol/intern (str prefix (cloiure.lang.RT/nextID))))
)

;;;
 ; Takes a set of test/expr pairs. It evaluates each test one at a time.
 ; If a test returns logical true, cond evaluates and returns the value of the
 ; corresponding expr and doesn't evaluate any of the other tests or exprs.
 ; (cond) returns nil.
 ;;
(§ defmacro cond [& clauses]
    (when clauses
        (list 'if (first clauses)
            (if (next clauses)
                (second clauses)
                (throw (IllegalArgumentException. "cond requires an even number of forms"))
            )
            (cons 'cloiure.core/cond (next (next clauses)))
        )
    )
)

;;;
 ; Returns a Keyword with the given namespace and name.
 ; Do not use ":" in the keyword strings, it will be added automatically.
 ;;
(§ defn ^cloiure.lang.Keyword keyword
    ([name]
        (cond
            (keyword? name) name
            (symbol? name) (cloiure.lang.Keyword/intern ^cloiure.lang.Symbol name)
            (string? name) (cloiure.lang.Keyword/intern (cloiure.lang.Symbol/intern ^String name))
        )
    )
    ([ns name] (cloiure.lang.Keyword/intern (cloiure.lang.Symbol/intern ns name)))
)

;;;
 ; Returns a Keyword with the given namespace and name if one already exists.
 ; This function will not intern a new keyword. If the keyword has not already
 ; been interned, it will return nil.
 ; Do not use ":" in the keyword strings, it will be added automatically.
 ;;
(§ defn ^cloiure.lang.Keyword find-keyword
    ([name]
        (cond
            (keyword? name) name
            (symbol? name) (cloiure.lang.Keyword/find ^cloiure.lang.Symbol name)
            (string? name) (cloiure.lang.Keyword/find ^String name)
        )
    )
    ([ns name] (cloiure.lang.Keyword/find ns name))
)

(§ defn ^:private spread [arglist]
    (cond
        (nil? arglist) nil
        (nil? (next arglist)) (seq (first arglist))
        :else (cons (first arglist) (spread (next arglist)))
    )
)

;;;
 ; Creates a new seq containing the items prepended to the rest,
 ; the last of which will be treated as a sequence.
 ;;
(§ defn list*
    ([args] (seq args))
    ([a args] (cons a args))
    ([a b args] (cons a (cons b args)))
    ([a b c args] (cons a (cons b (cons c args))))
    ([a b c d & more] (cons a (cons b (cons c (cons d (spread more))))))
)

;;;
 ; Applies fn f to the argument list formed by prepending intervening arguments to args.
 ;;
(§ defn apply
    ([^cloiure.lang.IFn f args] (.applyTo f (seq args)))
    ([^cloiure.lang.IFn f x args] (.applyTo f (list* x args)))
    ([^cloiure.lang.IFn f x y args] (.applyTo f (list* x y args)))
    ([^cloiure.lang.IFn f x y z args] (.applyTo f (list* x y z args)))
    ([^cloiure.lang.IFn f a b c d & more] (.applyTo f (cons a (cons b (cons c (cons d (spread more)))))))
)

;;;
 ; Returns an object of the same type and value as obj,
 ; with (apply f (meta obj) args) as its metadata.
 ;;
(§ defn vary-meta [obj f & args] (with-meta obj (apply f (meta obj) args)))

;;;
 ; Takes a body of expressions that returns an ISeq or nil, and yields
 ; a Seqable object that will invoke the body only the first time seq
 ; is called, and will cache the result and return it on all subsequent
 ; seq calls. See also - realized?
 ;;
(§ defmacro lazy-seq [& body]
    (list 'new 'cloiure.lang.LazySeq (list* '^{:once true} fn* [] body))
)

(§ defn ^cloiure.lang.ChunkBuffer chunk-buffer [capacity]
    (cloiure.lang.ChunkBuffer. capacity)
)

(§ defn chunk-append [^cloiure.lang.ChunkBuffer b x]
    (.add b x)
)

(§ defn ^cloiure.lang.IChunk chunk [^cloiure.lang.ChunkBuffer b]
    (.chunk b)
)

(§ defn ^cloiure.lang.IChunk chunk-first [^cloiure.lang.IChunkedSeq s]
    (.chunkedFirst s)
)

(§ defn ^cloiure.lang.ISeq chunk-rest [^cloiure.lang.IChunkedSeq s]
    (.chunkedMore s)
)

(§ defn ^cloiure.lang.ISeq chunk-next [^cloiure.lang.IChunkedSeq s]
    (.chunkedNext s)
)

(§ defn chunk-cons [chunk rest]
    (if (cloiure.lang.Numbers/isZero (cloiure.lang.RT/count chunk))
        rest
        (cloiure.lang.ChunkedCons. chunk rest)
    )
)

(§ defn chunked-seq? [s]
    (instance? cloiure.lang.IChunkedSeq s)
)

;;;
 ; Returns a lazy seq representing the concatenation of the elements in the supplied colls.
 ;;
(§ defn concat
    ([] (lazy-seq nil))
    ([x] (lazy-seq x))
    ([x y]
        (lazy-seq
            (let [s (seq x)]
                (if s
                    (if (chunked-seq? s)
                        (chunk-cons (chunk-first s) (concat (chunk-rest s) y))
                        (cons (first s) (concat (rest s) y))
                    )
                    y
                )
            )
        )
    )
    ([x y & zs]
        (let [cat
                (fn cat [xys zs]
                    (lazy-seq
                        (let [xys (seq xys)]
                            (if xys
                                (if (chunked-seq? xys)
                                    (chunk-cons (chunk-first xys) (cat (chunk-rest xys) zs))
                                    (cons (first xys) (cat (rest xys) zs))
                                )
                                (when zs
                                    (cat (first zs) (next zs))
                                )
                            )
                        )
                    )
                )]
            (cat (concat x y) zs)
        )
    )
)

;;;
 ; Takes a body of expressions and yields a Delay object that will invoke
 ; the body only the first time it is forced (with force or deref/@), and
 ; will cache the result and return it on all subsequent force calls.
 ; See also - realized?
 ;;
(§ defmacro delay [& body] (list 'new 'cloiure.lang.Delay (list* `^{:once true} fn* [] body)))

;;;
 ; Returns true if x is a Delay created with delay.
 ;;
(§ defn delay? [x] (instance? cloiure.lang.Delay x))

;;;
 ; If x is a Delay, returns the (possibly cached) value of its expression, else returns x.
 ;;
(§ defn force [x] (cloiure.lang.Delay/force x))

;;;
 ; Evaluates test. If logical false, evaluates and returns then expr,
 ; otherwise else expr, if supplied, else nil.
 ;;
(§ defmacro if-not
    ([test then] `(if-not ~test ~then nil))
    ([test then else] `(if (not ~test) ~then ~else))
)

;;;
 ; Tests if 2 arguments are the same object.
 ;;
(§ defn identical?
    {:inline (fn [x y] `(cloiure.lang.Util/identical ~x ~y)) :inline-arities #{2}}
    ([x y] (cloiure.lang.Util/identical x y))
)

;; equiv-based

;;;
 ; Equality. Returns true if x equals y, false if not. Same as Java x.equals(y) except it also
 ; works for nil, and compares numbers and collections in a type-independent manner. Cloiure's
 ; immutable data structures define equals() (and thus =) as a value, not an identity, comparison.
 ;;
(§ defn =
    {:inline (fn [x y] `(cloiure.lang.Util/equiv ~x ~y)) :inline-arities #{2}}
    ([x] true)
    ([x y] (cloiure.lang.Util/equiv x y))
    ([x y & more]
        (if (cloiure.lang.Util/equiv x y)
            (if (next more)
                (recur y (first more) (next more))
                (cloiure.lang.Util/equiv y (first more))
            )
            false
        )
    )
)

;; equals-based

;;;
 ; Equality. Returns true if x equals y, false if not. Same as Java x.equals(y) except it also
 ; works for nil. Boxed numbers must have same type. Cloiure's immutable data structures define
 ; equals() (and thus =) as a value, not an identity, comparison.
 ;;
#_(defn =
    {:inline (fn [x y] `(cloiure.lang.Util/equals ~x ~y)) :inline-arities #{2}}
    ([x] true)
    ([x y] (cloiure.lang.Util/equals x y))
    ([x y & more]
        (if (= x y)
            (if (next more)
                (recur y (first more) (next more))
                (= y (first more))
            )
            false
        )
    )
)

;;;
 ; Same as (not (= obj1 obj2)).
 ;;
(§ defn ^Boolean not=
    ([x] false)
    ([x y] (not (= x y)))
    ([x y & more] (not (apply = x y more)))
)

;;;
 ; Comparator. Returns a negative number, zero, or a positive number when x is logically
 ; 'less than', 'equal to', or 'greater than' y. Same as Java x.compareTo(y) except it also
 ; works for nil, and compares numbers and collections in a type-independent manner.
 ; x must implement Comparable.
 ;;
(§ defn compare
    {:inline (fn [x y] `(cloiure.lang.Util/compare ~x ~y))}
    [x y] (cloiure.lang.Util/compare x y)
)

;;;
 ; Evaluates exprs one at a time, from left to right. If a form returns logical false
 ; (nil or false), and returns that value and doesn't evaluate any of the other expressions,
 ; otherwise it returns the value of the last expr. (and) returns true.
 ;;
(§ defmacro and
    ([] true)
    ([x] x)
    ([x & next] `(let [and# ~x] (if and# (and ~@next) and#)))
)

;;;
 ; Evaluates exprs one at a time, from left to right. If a form returns a logical true value,
 ; or returns that value and doesn't evaluate any of the other expressions, otherwise it returns
 ; the value of the last expression. (or) returns nil.
 ;;
(§ defmacro or
    ([] nil)
    ([x] x)
    ([x & next] `(let [or# ~x] (if or# or# (or ~@next))))
)

;;;
 ; Returns true if num is zero, else false.
 ;;
(§ defn zero?
    {:inline (fn [num] `(cloiure.lang.Numbers/isZero ~num))}
    [num] (cloiure.lang.Numbers/isZero num)
)

;;;
 ; Returns the number of items in the collection. (count nil) returns 0.
 ; Also works on strings, arrays, and Java Collections and Maps.
 ;;
(§ defn count
    {:inline (fn [coll] `(cloiure.lang.RT/count ~coll))}
    [coll] (cloiure.lang.RT/count coll)
)

;;;
 ; Coerce to int.
 ;;
(§ defn int
    {:inline (fn [x] `(cloiure.lang.RT/intCast ~x))}
    [x] (cloiure.lang.RT/intCast x)
)

;;;
 ; Returns the value at the index.
 ; get returns nil if index out of bounds, nth throws an exception unless not-found is supplied.
 ; nth also works for strings, Java arrays, regex Matchers and Lists, and, in O(n) time, for sequences.
 ;;
(§ defn nth
    {:inline (fn [c i & nf] `(cloiure.lang.RT/nth ~c ~i ~@nf)) :inline-arities #{2 3}}
    ([coll index]           (cloiure.lang.RT/nth coll index          ))
    ([coll index not-found] (cloiure.lang.RT/nth coll index not-found))
)

;;;
 ; Returns non-nil if nums are in monotonically increasing order, otherwise false.
 ;;
(§ defn <
    {:inline (fn [x y] `(cloiure.lang.Numbers/lt ~x ~y)) :inline-arities #{2}}
    ([x] true)
    ([x y] (cloiure.lang.Numbers/lt x y))
    ([x y & more]
        (if (< x y)
            (if (next more)
                (recur y (first more) (next more))
                (< y (first more))
            )
            false
        )
    )
)

;;;
 ; Returns a number one greater than num. Supports arbitrary precision.
 ; See also: inc
 ;;
(§ defn inc'
    {:inline (fn [x] `(cloiure.lang.Numbers/incP ~x))}
    [x] (cloiure.lang.Numbers/incP x)
)

;;;
 ; Returns a number one greater than num. Does not auto-promote longs, will throw on overflow.
 ; See also: inc'
 ;;
(§ defn inc
    {:inline (fn [x] `(cloiure.lang.Numbers/inc ~x))}
    [x] (cloiure.lang.Numbers/inc x)
)

;; reduce is defined again later after InternalReduce loads

(§ defn ^:private reduce1
    ([f coll]
        (let [s (seq coll)]
            (if s
                (reduce1 f (first s) (next s))
                (f)
            )
        )
    )
    ([f val coll]
        (let [s (seq coll)]
            (if s
                (if (chunked-seq? s)
                    (recur f (.reduce (chunk-first s) f val) (chunk-next s))
                    (recur f (f val (first s)) (next s))
                )
                val
            )
        )
    )
)

;;;
 ; Returns a seq of the items in coll in reverse order. Not lazy.
 ;;
(§ defn reverse [coll] (reduce1 conj () coll))

(§ defn ^:private nary-inline [op]
    (fn
        ([x] `(. cloiure.lang.Numbers (~op ~x)))
        ([x y] `(. cloiure.lang.Numbers (~op ~x ~y)))
        ([x y & more] (reduce1 (fn [a b] `(. cloiure.lang.Numbers (~op ~a ~b))) `(. cloiure.lang.Numbers (~op ~x ~y)) more))
    )
)

(§ defn ^:private >1? [n] (cloiure.lang.Numbers/gt n 1))
(§ defn ^:private >0? [n] (cloiure.lang.Numbers/gt n 0))

;;;
 ; Returns the sum of nums. (+') returns 0. Supports arbitrary precision.
 ; See also: +
 ;;
(§ defn +'
    {:inline (nary-inline 'addP) :inline-arities >1?}
    ([] 0)
    ([x] (cast Number x))
    ([x y] (cloiure.lang.Numbers/addP x y))
    ([x y & more] (reduce1 +' (+' x y) more))
)

;;;
 ; Returns the sum of nums. (+) returns 0. Does not auto-promote longs, will throw on overflow.
 ; See also: +'
 ;;
(§ defn +
    {:inline (nary-inline 'add) :inline-arities >1?}
    ([] 0)
    ([x] (cast Number x))
    ([x y] (cloiure.lang.Numbers/add x y))
    ([x y & more] (reduce1 + (+ x y) more))
)

;;;
 ; Returns the product of nums. (*') returns 1. Supports arbitrary precision.
 ; See also: *
 ;;
(§ defn *'
    {:inline (nary-inline 'multiplyP) :inline-arities >1?}
    ([] 1)
    ([x] (cast Number x))
    ([x y] (cloiure.lang.Numbers/multiplyP x y))
    ([x y & more] (reduce1 *' (*' x y) more))
)

;;;
 ; Returns the product of nums. (*) returns 1. Does not auto-promote longs, will throw on overflow.
 ; See also: *'
 ;;
(§ defn *
    {:inline (nary-inline 'multiply) :inline-arities >1?}
    ([] 1)
    ([x] (cast Number x))
    ([x y] (cloiure.lang.Numbers/multiply x y))
    ([x y & more] (reduce1 * (* x y) more))
)

;;;
 ; If no denominators are supplied, returns 1/numerator,
 ; else returns numerator divided by all of the denominators.
 ;;
(§ defn /
    {:inline (nary-inline 'divide) :inline-arities >1?}
    ([x] (/ 1 x))
    ([x y] (cloiure.lang.Numbers/divide x y))
    ([x y & more] (reduce1 / (/ x y) more))
)

;;;
 ; If no ys are supplied, returns the negation of x, else subtracts
 ; the ys from x and returns the result. Supports arbitrary precision.
 ; See also: -
 ;;
(§ defn -'
    {:inline (nary-inline 'minusP) :inline-arities >0?}
    ([x] (cloiure.lang.Numbers/minusP x))
    ([x y] (cloiure.lang.Numbers/minusP x y))
    ([x y & more] (reduce1 -' (-' x y) more))
)

;;;
 ; If no ys are supplied, returns the negation of x, else subtracts
 ; the ys from x and returns the result. Does not auto-promote longs, will throw on overflow.
 ; See also: -'
 ;;
(§ defn -
    {:inline (nary-inline 'minus) :inline-arities >0?}
    ([x] (cloiure.lang.Numbers/minus x))
    ([x y] (cloiure.lang.Numbers/minus x y))
    ([x y & more] (reduce1 - (- x y) more))
)

;;;
 ; Returns non-nil if nums are in monotonically non-decreasing order, otherwise false.
 ;;
(§ defn <=
    {:inline (fn [x y] `(cloiure.lang.Numbers/lte ~x ~y)) :inline-arities #{2}}
    ([x] true)
    ([x y] (cloiure.lang.Numbers/lte x y))
    ([x y & more]
        (if (<= x y)
            (if (next more)
                (recur y (first more) (next more))
                (<= y (first more))
            )
            false
        )
    )
)

;;;
 ; Returns non-nil if nums are in monotonically decreasing order, otherwise false.
 ;;
(§ defn >
    {:inline (fn [x y] `(cloiure.lang.Numbers/gt ~x ~y)) :inline-arities #{2}}
    ([x] true)
    ([x y] (cloiure.lang.Numbers/gt x y))
    ([x y & more]
        (if (> x y)
            (if (next more)
                (recur y (first more) (next more))
                (> y (first more))
            )
            false
        )
    )
)

;;;
 ; Returns non-nil if nums are in monotonically non-increasing order, otherwise false.
 ;;
(§ defn >=
    {:inline (fn [x y] `(cloiure.lang.Numbers/gte ~x ~y)) :inline-arities #{2}}
    ([x] true)
    ([x y] (cloiure.lang.Numbers/gte x y))
    ([x y & more]
        (if (>= x y)
            (if (next more)
                (recur y (first more) (next more))
                (>= y (first more))
            )
            false
        )
    )
)

;;;
 ; Returns non-nil if nums all have the equivalent value (type-independent), otherwise false.
 ;;
(§ defn ==
    {:inline (fn [x y] `(cloiure.lang.Numbers/equiv ~x ~y)) :inline-arities #{2}}
    ([x] true)
    ([x y] (cloiure.lang.Numbers/equiv x y))
    ([x y & more]
        (if (== x y)
            (if (next more)
                (recur y (first more) (next more))
                (== y (first more))
            )
            false
        )
    )
)

;;;
 ; Returns the greatest of the nums.
 ;;
(§ defn max
    {:inline (nary-inline 'max) :inline-arities >1?}
    ([x] x)
    ([x y] (cloiure.lang.Numbers/max x y))
    ([x y & more] (reduce1 max (max x y) more))
)

;;;
 ; Returns the least of the nums.
 ;;
(§ defn min
    {:inline (nary-inline 'min) :inline-arities >1?}
    ([x] x)
    ([x y] (cloiure.lang.Numbers/min x y))
    ([x y & more] (reduce1 min (min x y) more))
)

;;;
 ; Returns a number one less than num. Supports arbitrary precision.
 ; See also: dec
 ;;
(§ defn dec'
    {:inline (fn [x] `(cloiure.lang.Numbers/decP ~x))}
    [x] (cloiure.lang.Numbers/decP x)
)

;;;
 ; Returns a number one less than num. Does not auto-promote longs, will throw on overflow.
 ; See also: dec'
 ;;
(§ defn dec
    {:inline (fn [x] `(cloiure.lang.Numbers/dec ~x))}
    [x] (cloiure.lang.Numbers/dec x)
)

;;;
 ; Returns a number one greater than x, an int.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-inc-int
    {:inline (fn [x] `(cloiure.lang.Numbers/unchecked_int_inc ~x))}
    [x] (cloiure.lang.Numbers/unchecked_int_inc x)
)

;;;
 ; Returns a number one greater than x, a long.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-inc
    {:inline (fn [x] `(cloiure.lang.Numbers/unchecked_inc ~x))}
    [x] (cloiure.lang.Numbers/unchecked_inc x)
)

;;;
 ; Returns a number one less than x, an int.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-dec-int
    {:inline (fn [x] `(cloiure.lang.Numbers/unchecked_int_dec ~x))}
    [x] (cloiure.lang.Numbers/unchecked_int_dec x)
)

;;;
 ; Returns a number one less than x, a long.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-dec
    {:inline (fn [x] `(cloiure.lang.Numbers/unchecked_dec ~x))}
    [x] (cloiure.lang.Numbers/unchecked_dec x)
)

;;;
 ; Returns the negation of x, an int.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-negate-int
    {:inline (fn [x] `(cloiure.lang.Numbers/unchecked_int_negate ~x))}
    [x] (cloiure.lang.Numbers/unchecked_int_negate x)
)

;;;
 ; Returns the negation of x, a long.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-negate
    {:inline (fn [x] `(cloiure.lang.Numbers/unchecked_minus ~x))}
    [x] (cloiure.lang.Numbers/unchecked_minus x)
)

;;;
 ; Returns the sum of x and y, both int.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-add-int
    {:inline (fn [x y] `(cloiure.lang.Numbers/unchecked_int_add ~x ~y))}
    [x y] (cloiure.lang.Numbers/unchecked_int_add x y)
)

;;;
 ; Returns the sum of x and y, both long.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-add
    {:inline (fn [x y] `(cloiure.lang.Numbers/unchecked_add ~x ~y))}
    [x y] (cloiure.lang.Numbers/unchecked_add x y)
)

;;;
 ; Returns the difference of x and y, both int.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-subtract-int
    {:inline (fn [x y] `(cloiure.lang.Numbers/unchecked_int_subtract ~x ~y))}
    [x y] (cloiure.lang.Numbers/unchecked_int_subtract x y)
)

;;;
 ; Returns the difference of x and y, both long.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-subtract
    {:inline (fn [x y] `(cloiure.lang.Numbers/unchecked_minus ~x ~y))}
    [x y] (cloiure.lang.Numbers/unchecked_minus x y)
)

;;;
 ; Returns the product of x and y, both int.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-multiply-int
    {:inline (fn [x y] `(cloiure.lang.Numbers/unchecked_int_multiply ~x ~y))}
    [x y] (cloiure.lang.Numbers/unchecked_int_multiply x y)
)

;;;
 ; Returns the product of x and y, both long.
 ; Note - uses a primitive operator subject to overflow.
 ;;
(§ defn unchecked-multiply
    {:inline (fn [x y] `(cloiure.lang.Numbers/unchecked_multiply ~x ~y))}
    [x y] (cloiure.lang.Numbers/unchecked_multiply x y)
)

;;;
 ; Returns the division of x by y, both int.
 ; Note - uses a primitive operator subject to truncation.
 ;;
(§ defn unchecked-divide-int
    {:inline (fn [x y] `(cloiure.lang.Numbers/unchecked_int_divide ~x ~y))}
    [x y] (cloiure.lang.Numbers/unchecked_int_divide x y)
)

;;;
 ; Returns the remainder of division of x by y, both int.
 ; Note - uses a primitive operator subject to truncation.
 ;;
(§ defn unchecked-remainder-int
    {:inline (fn [x y] `(cloiure.lang.Numbers/unchecked_int_remainder ~x ~y))}
    [x y] (cloiure.lang.Numbers/unchecked_int_remainder x y)
)

;;;
 ; Returns true if num is greater than zero, else false.
 ;;
(§ defn pos?
    {:inline (fn [num] `(cloiure.lang.Numbers/isPos ~num))}
    [num] (cloiure.lang.Numbers/isPos num)
)

;;;
 ; Returns true if num is less than zero, else false.
 ;;
(§ defn neg?
    {:inline (fn [num] `(cloiure.lang.Numbers/isNeg ~num))}
    [num] (cloiure.lang.Numbers/isNeg num)
)

;;;
 ; quot[ient] of dividing numerator by denominator.
 ;;
(§ defn quot
    {:inline (fn [x y] `(cloiure.lang.Numbers/quotient ~x ~y))}
    [num div] (cloiure.lang.Numbers/quotient num div)
)

;;;
 ; rem[ainder] of dividing numerator by denominator.
 ;;
(§ defn rem
    {:inline (fn [x y] `(cloiure.lang.Numbers/remainder ~x ~y))}
    [num div] (cloiure.lang.Numbers/remainder num div)
)

;;;
 ; Returns the rational value of num.
 ;;
(§ defn rationalize [num] (cloiure.lang.Numbers/rationalize num))

;;;
 ; Bitwise complement.
 ;;
(§ defn bit-not
    {:inline (fn [x] `(cloiure.lang.Numbers/not ~x))}
    [x] (cloiure.lang.Numbers/not x)
)

;;;
 ; Bitwise and.
 ;;
(§ defn bit-and
    {:inline (nary-inline 'and) :inline-arities >1?}
    ([x y] (cloiure.lang.Numbers/and x y))
    ([x y & more] (reduce1 bit-and (bit-and x y) more))
)

;;;
 ; Bitwise or.
 ;;
(§ defn bit-or
    {:inline (nary-inline 'or) :inline-arities >1?}
    ([x y] (cloiure.lang.Numbers/or x y))
    ([x y & more] (reduce1 bit-or (bit-or x y) more))
)

;;;
 ; Bitwise exclusive or.
 ;;
(§ defn bit-xor
    {:inline (nary-inline 'xor) :inline-arities >1?}
    ([x y] (cloiure.lang.Numbers/xor x y))
    ([x y & more] (reduce1 bit-xor (bit-xor x y) more))
)

;;;
 ; Bitwise and with complement.
 ;;
(§ defn bit-and-not
    {:inline (nary-inline 'andNot) :inline-arities >1?}
    ([x y] (cloiure.lang.Numbers/andNot x y))
    ([x y & more] (reduce1 bit-and-not (bit-and-not x y) more))
)

;;;
 ; Clear bit at index n.
 ;;
(§ defn bit-clear [x n] (cloiure.lang.Numbers/clearBit x n))

;;;
 ; Set bit at index n.
 ;;
(§ defn bit-set [x n] (cloiure.lang.Numbers/setBit x n))

;;;
 ; Flip bit at index n.
 ;;
(§ defn bit-flip [x n] (cloiure.lang.Numbers/flipBit x n))

;;;
 ; Test bit at index n.
 ;;
(§ defn bit-test [x n] (cloiure.lang.Numbers/testBit x n))

;;;
 ; Bitwise shift left.
 ;;
(§ defn bit-shift-left
    {:inline (fn [x n] `(cloiure.lang.Numbers/shiftLeft ~x ~n))}
    [x n] (cloiure.lang.Numbers/shiftLeft x n)
)

;;;
 ; Bitwise shift right.
 ;;
(§ defn bit-shift-right
    {:inline (fn [x n] `(cloiure.lang.Numbers/shiftRight ~x ~n))}
    [x n] (cloiure.lang.Numbers/shiftRight x n)
)

;;;
 ; Bitwise shift right, without sign-extension.
 ;;
(§ defn unsigned-bit-shift-right
    {:inline (fn [x n] `(cloiure.lang.Numbers/unsignedShiftRight ~x ~n))}
    [x n] (cloiure.lang.Numbers/unsignedShiftRight x n)
)

;;;
 ; Returns true if n is an integer.
 ;;
(§ defn integer? [n]
    (or (instance? Integer n)
        (instance? Long n)
        (instance? cloiure.lang.BigInt n)
        (instance? BigInteger n)
        (instance? Short n)
        (instance? Byte n)
    )
)

;;;
 ; Returns true if n is even, throws an exception if n is not an integer.
 ;;
(§ defn even? [n]
    (if (integer? n)
        (zero? (bit-and (cloiure.lang.RT/uncheckedLongCast n) 1))
        (throw (IllegalArgumentException. (str "Argument must be an integer: " n)))
    )
)

;;;
 ; Returns true if n is odd, throws an exception if n is not an integer.
 ;;
(§ defn odd? [n] (not (even? n)))

;;;
 ; Return true if x is a fixed precision integer.
 ;;
(§ defn int? [x]
    (or (instance? Long x)
        (instance? Integer x)
        (instance? Short x)
        (instance? Byte x)
    )
)

;;;
 ; Return true if x is a positive fixed precision integer.
 ;;
(§ defn pos-int? [x] (and (int? x) (pos? x)))

;;;
 ; Return true if x is a negative fixed precision integer.
 ;;
(§ defn neg-int? [x] (and (int? x) (neg? x)))

;;;
 ; Return true if x is a non-negative fixed precision integer.
 ;;
(§ defn nat-int? [x] (and (int? x) (not (neg? x))))

;;;
 ; Return true if x is a Double.
 ;;
(§ defn double? [x] (instance? Double x))

;;;
 ; Takes a fn f and returns a fn that takes the same arguments as f,
 ; has the same effects, if any, and returns the opposite truth value.
 ;;
(§ defn complement [f]
    (fn
        ([] (not (f)))
        ([x] (not (f x)))
        ([x y] (not (f x y)))
        ([x y & zs] (not (apply f x y zs)))
    )
)

;;;
 ; Returns a function that takes any number of arguments and returns x.
 ;;
(§ defn constantly [x] (fn [& args] x))

;;;
 ; Returns its argument.
 ;;
(§ defn identity [x] x)

;; list stuff

;;;
 ; For a list or queue, same as first, for a vector, same as, but much
 ; more efficient than, last. If the collection is empty, returns nil.
 ;;
(§ defn peek [coll] (cloiure.lang.RT/peek coll))

;;;
 ; For a list or queue, returns a new list/queue without the first item,
 ; for a vector, returns a new vector without the last item.
 ; If the collection is empty, throws an exception.
 ; Note - not the same as next/butlast.
 ;;
(§ defn pop [coll] (cloiure.lang.RT/pop coll))

;; map stuff

;;;
 ; Return true if x is a map entry
 ;;
(§ defn map-entry? [x] (instance? java.util.Map$Entry x))

;;;
 ; Returns true if key is present in the given collection, otherwise
 ; returns false. Note that for numerically indexed collections, like
 ; vectors and Java arrays, this tests if the numeric key is within the
 ; range of indexes. 'contains?' operates constant or logarithmic time;
 ; it will not perform a linear search for a value. See also 'some'.
 ;;
(§ defn contains? [coll key] (cloiure.lang.RT/contains coll key))

;;;
 ; Returns the value mapped to key, not-found or nil if key not present.
 ;;
(§ defn get
    {:inline (fn [m k & nf] `(cloiure.lang.RT/get ~m ~k ~@nf)) :inline-arities #{2 3}}
    ([map key] (cloiure.lang.RT/get map key))
    ([map key not-found] (cloiure.lang.RT/get map key not-found))
)

;;;
 ; dissoc[iate]. Returns a new map of the same (hashed/sorted) type,
 ; that does not contain a mapping for key(s).
 ;;
(§ defn dissoc
    ([map] map)
    ([map key] (cloiure.lang.RT/dissoc map key))
    ([map key & ks]
        (let [ret (dissoc map key)]
            (if ks
                (recur ret (first ks) (next ks))
                ret
            )
        )
    )
)

;;;
 ; disj[oin]. Returns a new set of the same (hashed/sorted) type,
 ; that does not contain key(s).
 ;;
(§ defn disj
    ([set] set)
    ([^cloiure.lang.IPersistentSet set key]
        (when set
            (.disjoin set key)
        )
    )
    ([set key & ks]
        (when set
            (let [ret (disj set key)]
                (if ks
                    (recur ret (first ks) (next ks))
                    ret
                )
            )
        )
    )
)

;;;
 ; Returns the map entry for key, or nil if key not present.
 ;;
(§ defn find [map key] (cloiure.lang.RT/find map key))

;;;
 ; Returns a map containing only those entries in map whose key is in keys.
 ;;
(§ defn select-keys [map keyseq]
    (loop [ret {} keys (seq keyseq)]
        (if keys
            (let [entry (cloiure.lang.RT/find map (first keys))]
                (recur (if entry (conj ret entry) ret) (next keys))
            )
            (with-meta ret (meta map))
        )
    )
)

;;;
 ; Returns a sequence of the map's keys, in the same order as (seq map).
 ;;
(§ defn keys [map] (cloiure.lang.RT/keys map))

;;;
 ; Returns a sequence of the map's values, in the same order as (seq map).
 ;;
(§ defn vals [map] (cloiure.lang.RT/vals map))

;;;
 ; Returns the key of the map entry.
 ;;
(§ defn key [^java.util.Map$Entry e] (.getKey e))

;;;
 ; Returns the value in the map entry.
 ;;
(§ defn val [^java.util.Map$Entry e] (.getValue e))

;;;
 ; Returns, in constant time, a seq of the items in rev (which can be a vector or sorted-map), in reverse order.
 ; If rev is empty, returns nil.
 ;;
(§ defn rseq [^cloiure.lang.Reversible rev] (.rseq rev))

;;;
 ; Returns the name String of a string, symbol or keyword.
 ;;
(§ defn ^String name [x] (if (string? x) x (.getName ^cloiure.lang.Named x)))

;;;
 ; Returns the namespace String of a symbol or keyword, or nil if not present.
 ;;
(§ defn ^String namespace [^cloiure.lang.Named x] (.getNamespace x))

;;;
 ; Coerce to boolean.
 ;;
(§ defn boolean
    {:inline (fn [x] `(cloiure.lang.RT/booleanCast ~x))}
    [x] (cloiure.lang.RT/booleanCast x)
)

;;;
 ; Return true if x is a symbol or keyword.
 ;;
(§ defn ident? [x] (or (keyword? x) (symbol? x)))

;;;
 ; Return true if x is a symbol or keyword without a namespace.
 ;;
(§ defn simple-ident? [x] (and (ident? x) (nil? (namespace x))))

;;;
 ; Return true if x is a symbol or keyword with a namespace.
 ;;
(§ defn qualified-ident? [x] (boolean (and (ident? x) (namespace x) true)))

;;;
 ; Return true if x is a symbol without a namespace.
 ;;
(§ defn simple-symbol? [x] (and (symbol? x) (nil? (namespace x))))

;;;
 ; Return true if x is a symbol with a namespace.
 ;;
(§ defn qualified-symbol? [x] (boolean (and (symbol? x) (namespace x) true)))

;;;
 ; Return true if x is a keyword without a namespace.
 ;;
(§ defn simple-keyword? [x] (and (keyword? x) (nil? (namespace x))))

;;;
 ; Return true if x is a keyword with a namespace.
 ;;
(§ defn qualified-keyword? [x] (boolean (and (keyword? x) (namespace x) true)))

;;;
 ; Executes exprs in an implicit do, while holding the monitor of x.
 ; Will release the monitor of x in all circumstances.
 ;;
(§ defmacro locking [x & body]
    `(let [lockee# ~x]
        (try
            (monitor-enter lockee#)
            ~@body
            (finally
                (monitor-exit lockee#)
            )
        )
    )
)

;;;
 ; form => fieldName-symbol or (instanceMethodName-symbol args*)
 ;
 ; Expands into a member access (.) of the first member on the first argument,
 ; followed by the next member on the result, etc. For instance:
 ;
 ; (.. System (getProperties) (get "os.name"))
 ;
 ; expands to:
 ;
 ; (. (. System (getProperties)) (get "os.name"))
 ;
 ; but is easier to write, read, and understand.
 ;;
(§ defmacro ..
    ([x form] `(. ~x ~form))
    ([x form & more] `(.. (. ~x ~form) ~@more))
)

;;;
 ; Threads the expr through the forms. Inserts x as the second item
 ; in the first form, making a list of it if it is not a list already.
 ; If there are more forms, inserts the first form as the second item
 ; in second form, etc.
 ;;
(§ defmacro -> [x & forms]
    (loop [x x forms forms]
        (if forms
            (let [form (first forms)
                  threaded
                    (if (seq? form)
                        (with-meta `(~(first form) ~x ~@(next form)) (meta form))
                        (list form x)
                    )]
                (recur threaded (next forms))
            )
            x
        )
    )
)

;;;
 ; Threads the expr through the forms. Inserts x as the last item
 ; in the first form, making a list of it if it is not a list already.
 ; If there are more forms, inserts the first form as the last item
 ; in second form, etc.
 ;;
(§ defmacro ->> [x & forms]
    (loop [x x forms forms]
        (if forms
            (let [form (first forms)
                  threaded
                    (if (seq? form)
                        (with-meta `(~(first form) ~@(next form) ~x) (meta form))
                        (list form x)
                    )]
                (recur threaded (next forms))
            )
            x
        )
    )
)

(§ def map)

;;;
 ; Throws an exception if the given option map contains keys not listed as valid, else returns nil.
 ;;
(§ defn ^:private check-valid-options [options & valid-keys]
    (when (seq (apply disj (apply hash-set (keys options)) valid-keys))
        (throw (IllegalArgumentException. (apply str "Only these options are valid: " (first valid-keys) (map #(str ", " %) (rest valid-keys)))))
    )
)

;; multimethods

(§ def global-hierarchy)

;;;
 ; Creates a new multimethod with the associated dispatch function.
 ; The docstring and attr-map are optional.
 ;
 ; Options are key-value pairs and may be one of:
 ;
 ; :default
 ;
 ; The default dispatch value, defaults to :default
 ;
 ; :hierarchy
 ;
 ; The value used for hierarchical dispatch (e.g. ::square is-a ::shape)
 ;
 ; Hierarchies are type-like relationships that do not depend upon type
 ; inheritance. By default Cloiure's multimethods dispatch off of a
 ; global hierarchy map. However, a hierarchy relationship can be
 ; created with the derive function used to augment the root ancestor
 ; created with make-hierarchy.
 ;
 ; Multimethods expect the value of the hierarchy option to be supplied as
 ; a reference type e.g. a var (i.e. via the Var-quote dispatch macro #'
 ; or the var special form).
 ;;
(§ defmacro defmulti [mm-name & options]
    (let [docstring   (if (string? (first options)) (first options) nil)
          options     (if (string? (first options)) (next options) options)
          m           (if (map? (first options)) (first options) {})
          options     (if (map? (first options)) (next options) options)
          dispatch-fn (first options)
          options     (next options)
          m           (if docstring (assoc m :doc docstring) m)
          m           (if (meta mm-name) (conj (meta mm-name) m) m)
          mm-name     (with-meta mm-name m)]
        (when (= (count options) 1)
            (throw (Exception. "The syntax for defmulti has changed. Example: (defmulti name dispatch-fn :default dispatch-value)"))
        )
        (let [options   (apply hash-map options)
              default   (get options :default :default)
              hierarchy (get options :hierarchy #'global-hierarchy)]
            (check-valid-options options :default :hierarchy)
            `(let [v# (def ~mm-name)]
                (when-not (and (.hasRoot v#) (instance? cloiure.lang.MultiFn (deref v#)))
                    (def ~mm-name (cloiure.lang.MultiFn. ~(name mm-name) ~dispatch-fn ~default ~hierarchy))
                )
            )
        )
    )
)

;;;
 ; Creates and installs a new method of multimethod associated with dispatch-value.
 ;;
(§ defmacro defmethod [multifn dispatch-val & fn-tail]
    `(.addMethod ~(with-meta multifn {:tag 'cloiure.lang.MultiFn}) ~dispatch-val (fn ~@fn-tail))
)

;;;
 ; Removes all of the methods of multimethod.
 ;;
(§ defn remove-all-methods [^cloiure.lang.MultiFn multifn]
    (.reset multifn)
)

;;;
 ; Removes the method of multimethod associated with dispatch-value.
 ;;
(§ defn remove-method [^cloiure.lang.MultiFn multifn dispatch-val]
    (.removeMethod multifn dispatch-val)
)

;;;
 ; Causes the multimethod to prefer matches of dispatch-val-x over dispatch-val-y when there is a conflict.
 ;;
(§ defn prefer-method [^cloiure.lang.MultiFn multifn dispatch-val-x dispatch-val-y]
    (.preferMethod multifn dispatch-val-x dispatch-val-y)
)

;;;
 ; Given a multimethod, returns a map of dispatch values -> dispatch fns.
 ;;
(§ defn methods [^cloiure.lang.MultiFn multifn]
    (.getMethodTable multifn)
)

;;;
 ; Given a multimethod and a dispatch value, returns the dispatch fn
 ; that would apply to that value, or nil if none apply and no default.
 ;;
(§ defn get-method [^cloiure.lang.MultiFn multifn dispatch-val]
    (.getMethod multifn dispatch-val)
)

;;;
 ; Given a multimethod, returns a map of preferred value -> set of other values.
 ;;
(§ defn prefers [^cloiure.lang.MultiFn multifn]
    (.getPreferTable multifn)
)

;; var stuff

(§ defmacro ^:private assert-args [& pairs]
    `(do
        (when-not ~(first pairs)
            (throw (IllegalArgumentException. (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form)))))
        )
        ~(let [more (nnext pairs)]
            (when more
                (list* `assert-args more)
            )
        )
    )
)

;;;
 ; bindings => binding-form test
 ;
 ; If test is true, evaluates then with binding-form bound to the value of test, if not, yields else.
 ;;
(§ defmacro if-let
    ([bindings then] `(if-let ~bindings ~then nil))
    ([bindings then else & oldform]
        (assert-args
            (vector? bindings) "a vector for its binding"
            (nil? oldform) "1 or 2 forms after binding vector"
            (= 2 (count bindings)) "exactly 2 forms in binding vector"
        )
        (let [form (bindings 0) tst (bindings 1)]
            `(let [temp# ~tst]
                (if temp# (let [~form temp#] ~then) ~else)
            )
        )
    )
)

;;;
 ; bindings => binding-form test
 ;
 ; When test is true, evaluates body with binding-form bound to the value of test.
 ;;
(§ defmacro when-let [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (= 2 (count bindings)) "exactly 2 forms in binding vector"
    )
    (let [form (bindings 0) tst (bindings 1)]
        `(let [temp# ~tst]
            (when temp# (let [~form temp#] ~@body))
        )
    )
)

;;;
 ; bindings => binding-form test
 ;
 ; If test is not nil, evaluates then with binding-form bound to the value of test, if not, yields else.
 ;;
(§ defmacro if-some
    ([bindings then] `(if-some ~bindings ~then nil))
    ([bindings then else & oldform]
        (assert-args
            (vector? bindings) "a vector for its binding"
            (nil? oldform) "1 or 2 forms after binding vector"
            (= 2 (count bindings)) "exactly 2 forms in binding vector"
        )
        (let [form (bindings 0) tst (bindings 1)]
            `(let [temp# ~tst]
                (if (nil? temp#) ~else (let [~form temp#] ~then))
            )
        )
    )
)

;;;
 ; bindings => binding-form test
 ;
 ; When test is not nil, evaluates body with binding-form bound to the value of test.
 ;;
(§ defmacro when-some [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (= 2 (count bindings)) "exactly 2 forms in binding vector"
    )
    (let [form (bindings 0) tst (bindings 1)]
        `(let [temp# ~tst]
            (if (nil? temp#) nil (let [~form temp#] ~@body))
        )
    )
)

;;;
 ; WARNING: This is a low-level function.
 ; Prefer high-level macros like binding where ever possible.
 ;
 ; Takes a map of Var/value pairs. Binds each Var to the associated value for
 ; the current thread. Each call *MUST* be accompanied by a matching call to
 ; pop-thread-bindings wrapped in a try-finally!
 ;
 ; (push-thread-bindings bindings)
 ; (try
 ; ...
 ; (finally
 ; (pop-thread-bindings)))
 ;;
(§ defn push-thread-bindings [bindings] (cloiure.lang.Var/pushThreadBindings bindings))

;;;
 ; Pop one set of bindings pushed with push-binding before.
 ; It is an error to pop bindings without pushing before.
 ;;
(§ defn pop-thread-bindings [] (cloiure.lang.Var/popThreadBindings))

;;;
 ; Get a map with the Var/value pairs which is currently in effect for the current thread.
 ;;
(§ defn get-thread-bindings [] (cloiure.lang.Var/getThreadBindings))

;;;
 ; binding => var-symbol init-expr
 ;
 ; Creates new bindings for the (already-existing) vars, with the
 ; supplied initial values, executes the exprs in an implicit do, then
 ; re-establishes the bindings that existed before. The new bindings
 ; are made in parallel (unlike let); all init-exprs are evaluated
 ; before the vars are bound to their new values.
 ;;
(§ defmacro binding [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (even? (count bindings)) "an even number of forms in binding vector"
    )
    (let [var-ize
            (fn [var-vals]
                (loop [ret [] vvs (seq var-vals)]
                    (if vvs
                        (recur (conj (conj ret `(var ~(first vvs))) (second vvs)) (next (next vvs)))
                        (seq ret)
                    )
                )
            )]
        `(let []
            (push-thread-bindings (hash-map ~@(var-ize bindings)))
            (try
                ~@body
                (finally
                    (pop-thread-bindings)
                )
            )
        )
    )
)

;;;
 ; Takes a map of Var/value pairs. Installs for the given Vars the associated
 ; values as thread-local bindings. Then calls f with the supplied arguments.
 ; Pops the installed bindings after f returned. Returns whatever f returns.
 ;;
(§ defn with-bindings* [binding-map f & args]
    (push-thread-bindings binding-map)
    (try
        (apply f args)
        (finally
            (pop-thread-bindings)
        )
    )
)

;;;
 ; Takes a map of Var/value pairs. Installs for the given Vars the associated
 ; values as thread-local bindings. Then executes body. Pops the installed
 ; bindings after body was evaluated. Returns the value of body.
 ;;
(§ defmacro with-bindings [binding-map & body]
    `(with-bindings* ~binding-map (fn [] ~@body))
)

;;;
 ; Returns a function, which will install the same bindings in effect as in
 ; the thread at the time bound-fn* was called and then call f with any given
 ; arguments. This may be used to define a helper function which runs on a
 ; different thread, but needs the same bindings in place.
 ;;
(§ defn bound-fn* [f]
    (let [bindings (get-thread-bindings)]
        (fn [& args] (apply with-bindings* bindings f args))
    )
)

;;;
 ; Returns a function defined by the given fntail, which will install the
 ; same bindings in effect as in the thread at the time bound-fn was called.
 ; This may be used to define a helper function which runs on a different
 ; thread, but needs the same bindings in place.
 ;;
(§ defmacro bound-fn [& fntail] `(bound-fn* (fn ~@fntail)))

;;;
 ; Returns the global var named by the namespace-qualified symbol,
 ; or nil if no var with that name.
 ;;
(§ defn find-var [sym] (cloiure.lang.Var/find sym))

(§ defn ^:private setup-reference [^cloiure.lang.IReference r options]
    (let [opts (apply hash-map options)]
        (when (:meta opts)
            (.resetMeta r (:meta opts))
        )
        r
    )
)

;;;
 ; Also reader macro: @ref/@var/@atom/@delay.
 ; Within a transaction, returns the in-transaction-value of ref, else
 ; returns the most-recently-committed value of ref. When applied to a var
 ; or atom, returns its current state. When applied to a delay, forces
 ; it if not already forced. See also - realized?.
 ;;
(§ defn deref [ref] (.deref ^cloiure.lang.IDeref ref))

;;;
 ; Creates and returns an Atom with an initial value of x and zero or more
 ; options (in any order):
 ;
 ; :meta      metadata-map
 ;
 ; If metadata-map is supplied, it will become the metadata on the atom.
 ;;
(§ defn atom
    ([x] (cloiure.lang.Atom. x))
    ([x & options] (setup-reference (atom x) options))
)

;;;
 ; Atomically swaps the value of atom to be: (apply f current-value-of-atom args).
 ; Note that f may be called multiple times, and thus should be free of side effects.
 ; Returns the value that was swapped in.
 ;;
(§ defn swap!
    ([^cloiure.lang.IAtom atom f] (.swap atom f))
    ([^cloiure.lang.IAtom atom f x] (.swap atom f x))
    ([^cloiure.lang.IAtom atom f x y] (.swap atom f x y))
    ([^cloiure.lang.IAtom atom f x y & args] (.swap atom f x y args))
)

;;;
 ; Atomically swaps the value of atom to be: (apply f current-value-of-atom args).
 ; Note that f may be called multiple times, and thus should be free of side effects.
 ; Returns [old new], the value of the atom before and after the swap.
 ;;
(§ defn ^cloiure.lang.IPersistentVector swap-vals!
    ([^cloiure.lang.IAtom2 atom f] (.swapVals atom f))
    ([^cloiure.lang.IAtom2 atom f x] (.swapVals atom f x))
    ([^cloiure.lang.IAtom2 atom f x y] (.swapVals atom f x y))
    ([^cloiure.lang.IAtom2 atom f x y & args] (.swapVals atom f x y args))
)

;;;
 ; Atomically sets the value of atom to newval if and only if the
 ; current value of the atom is identical to oldval. Returns true if
 ; set happened, else false
 ;;
(§ defn compare-and-set! [^cloiure.lang.IAtom atom oldval newval]
    (.compareAndSet atom oldval newval)
)

;;;
 ; Sets the value of atom to newval without regard for the current value.
 ; Returns newval.
 ;;
(§ defn reset! [^cloiure.lang.IAtom atom newval]
    (.reset atom newval)
)

;;;
 ; Sets the value of atom to newval. Returns [old new], the value of the
 ; atom before and after the reset.
 ;;
(§ defn ^cloiure.lang.IPersistentVector reset-vals! [^cloiure.lang.IAtom2 atom newval]
    (.resetVals atom newval)
)

;;;
 ; Atomically sets the metadata for a namespace/var/ref/atom to be:
 ;
 ; (apply f its-current-meta args)
 ;
 ; f must be free of side-effects.
 ;;
(§ defn alter-meta! [^cloiure.lang.IReference iref f & args]
    (.alterMeta iref f args)
)

;;;
 ; Atomically resets the metadata for a namespace/var/ref/atom.
 ;;
(§ defn reset-meta! [^cloiure.lang.IReference iref metadata-map]
    (.resetMeta iref metadata-map)
)

;;;
 ; Creates and returns a Volatile with an initial value of val.
 ;;
(§ defn ^cloiure.lang.Volatile volatile! [val]
    (cloiure.lang.Volatile. val)
)

;;;
 ; Sets the value of volatile to newval without regard for the
 ; current value. Returns newval.
 ;;
(§ defn vreset! [^cloiure.lang.Volatile vol newval]
    (.reset vol newval)
)

;;;
 ; Non-atomically swaps the value of the volatile as if:
 ; (apply f current-value-of-vol args).
 ; Returns the value that was swapped in.
 ;;
(§ defmacro vswap! [vol f & args]
    (let [v (with-meta vol {:tag 'cloiure.lang.Volatile})]
        `(.reset ~v (~f (.deref ~v) ~@args))
    )
)

;;;
 ; Returns true if x is a volatile.
 ;;
(§ defn volatile? [x] (instance? cloiure.lang.Volatile x))

;;;
 ; Takes a set of functions and returns a fn that is the composition
 ; of those fns. The returned fn takes a variable number of args,
 ; applies the rightmost of fns to the args, the next
 ; fn (right-to-left) to the result, etc.
 ;;
(§ defn comp
    ([] identity)
    ([f] f)
    ([f g]
        (fn
            ([] (f (g)))
            ([x] (f (g x)))
            ([x y] (f (g x y)))
            ([x y z] (f (g x y z)))
            ([x y z & args] (f (apply g x y z args)))
        )
    )
    ([f g & fs] (reduce1 comp (list* f g fs)))
)

;;;
 ; Takes a set of functions and returns a fn that is the juxtaposition
 ; of those fns. The returned fn takes a variable number of args, and
 ; returns a vector containing the result of applying each fn to the
 ; args (left-to-right).
 ; ((juxt a b c) x) => [(a x) (b x) (c x)]
 ;;
(§ defn juxt
    ([f]
        (fn
            ([] [(f)])
            ([x] [(f x)])
            ([x y] [(f x y)])
            ([x y z] [(f x y z)])
            ([x y z & args] [(apply f x y z args)])
        )
    )
    ([f g]
        (fn
            ([] [(f) (g)])
            ([x] [(f x) (g x)])
            ([x y] [(f x y) (g x y)])
            ([x y z] [(f x y z) (g x y z)])
            ([x y z & args] [(apply f x y z args) (apply g x y z args)])
        )
    )
    ([f g h]
        (fn
            ([] [(f) (g) (h)])
            ([x] [(f x) (g x) (h x)])
            ([x y] [(f x y) (g x y) (h x y)])
            ([x y z] [(f x y z) (g x y z) (h x y z)])
            ([x y z & args] [(apply f x y z args) (apply g x y z args) (apply h x y z args)])
        )
    )
    ([f g h & fs]
        (let [fs (list* f g h fs)]
            (fn
                ([] (reduce1 #(conj %1 (%2)) [] fs))
                ([x] (reduce1 #(conj %1 (%2 x)) [] fs))
                ([x y] (reduce1 #(conj %1 (%2 x y)) [] fs))
                ([x y z] (reduce1 #(conj %1 (%2 x y z)) [] fs))
                ([x y z & args] (reduce1 #(conj %1 (apply %2 x y z args)) [] fs))
            )
        )
    )
)

;;;
 ; Takes a function f and fewer than the normal arguments to f, and
 ; returns a fn that takes a variable number of additional args. When
 ; called, the returned function calls f with args + additional args.
 ;;
(§ defn partial
    ([f] f)
    ([f arg1]
        (fn
            ([] (f arg1))
            ([x] (f arg1 x))
            ([x y] (f arg1 x y))
            ([x y z] (f arg1 x y z))
            ([x y z & args] (apply f arg1 x y z args))
        )
    )
    ([f arg1 arg2]
        (fn
            ([] (f arg1 arg2))
            ([x] (f arg1 arg2 x))
            ([x y] (f arg1 arg2 x y))
            ([x y z] (f arg1 arg2 x y z))
            ([x y z & args] (apply f arg1 arg2 x y z args))
        )
    )
    ([f arg1 arg2 arg3]
        (fn
            ([] (f arg1 arg2 arg3))
            ([x] (f arg1 arg2 arg3 x))
            ([x y] (f arg1 arg2 arg3 x y))
            ([x y z] (f arg1 arg2 arg3 x y z))
            ([x y z & args] (apply f arg1 arg2 arg3 x y z args))
        )
    )
    ([f arg1 arg2 arg3 & more]
        (fn [& args] (apply f arg1 arg2 arg3 (concat more args)))
    )
)

;;;
 ; Coerces coll to a (possibly empty) sequence, if it is not already one.
 ; Will not force a lazy seq. (sequence nil) yields (), When a transducer
 ; is supplied, returns a lazy sequence of applications of the transform
 ; to the items in coll(s), i.e. to the set of first items of each coll,
 ; followed by the set of second items in each coll, until any one of the
 ; colls is exhausted. Any remaining items in other colls are ignored.
 ; The transform should accept number-of-colls arguments
 ;;
(§ defn sequence
    ([coll]
        (if (seq? coll) coll (or (seq coll) ()))
    )
    ([xform coll]
        (or (cloiure.lang.RT/chunkIteratorSeq (cloiure.lang.TransformerIterator/create xform (cloiure.lang.RT/iter coll))) ())
    )
    ([xform coll & colls]
        (or (cloiure.lang.RT/chunkIteratorSeq (cloiure.lang.TransformerIterator/createMulti xform (map #(cloiure.lang.RT/iter %) (cons coll colls)))) ())
    )
)

;;;
 ; Returns true if (pred x) is logical true for every x in coll, else false.
 ;;
(§ defn ^Boolean every? [pred coll]
    (cond
        (nil? (seq coll)) true
        (pred (first coll)) (recur pred (next coll))
        :else false
    )
)

;;;
 ; Returns false if (pred x) is logical true for every x in coll, else true.
 ;;
(§ def ^Boolean not-every? (comp not every?))

;;;
 ; Returns the first logical true value of (pred x) for any x in coll,
 ; else nil. One common idiom is to use a set as pred, for example
 ; this will return :fred if :fred is in the sequence, otherwise nil:
 ; (some #{:fred} coll).
 ;;
(§ defn some [pred coll]
    (when (seq coll)
        (or (pred (first coll)) (recur pred (next coll)))
    )
)

;;;
 ; Returns false if (pred x) is logical true for any x in coll, else true.
 ;;
(§ def ^Boolean not-any? (comp not some))

;; will be redefed later with arg checks

;;;
 ; bindings => name n
 ;
 ; Repeatedly executes body (presumably for side-effects) with name
 ; bound to integers from 0 through n-1.
 ;;
(§ defmacro dotimes [bindings & body]
    (let [i (first bindings) n (second bindings)]
        `(let [n# (cloiure.lang.RT/longCast ~n)]
            (loop [~i 0]
                (when (< ~i n#)
                    ~@body
                    (recur (unchecked-inc ~i))
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence consisting of the result of applying f to
 ; the set of first items of each coll, followed by applying f to the
 ; set of second items in each coll, until any one of the colls is
 ; exhausted. Any remaining items in other colls are ignored. Function
 ; f should accept number-of-colls arguments. Returns a transducer when
 ; no collection is provided.
 ;;
(§ defn map
    ([f]
        (fn [rf]
            (fn
                ([] (rf))
                ([result] (rf result))
                ([result input] (rf result (f input)))
                ([result input & inputs] (rf result (apply f input inputs)))
            )
        )
    )
    ([f coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (if (chunked-seq? s)
                    (let [c (chunk-first s) size (int (count c)) b (chunk-buffer size)]
                        (dotimes [i size]
                            (chunk-append b (f (.nth c i)))
                        )
                        (chunk-cons (chunk b) (map f (chunk-rest s)))
                    )
                    (cons (f (first s)) (map f (rest s)))
                )
            )
        )
    )
    ([f c1 c2]
        (lazy-seq
            (let [s1 (seq c1) s2 (seq c2)]
                (when (and s1 s2)
                    (cons (f (first s1) (first s2)) (map f (rest s1) (rest s2)))
                )
            )
        )
    )
    ([f c1 c2 c3]
        (lazy-seq
            (let [s1 (seq c1) s2 (seq c2) s3 (seq c3)]
                (when (and s1 s2 s3)
                    (cons (f (first s1) (first s2) (first s3)) (map f (rest s1) (rest s2) (rest s3)))
                )
            )
        )
    )
    ([f c1 c2 c3 & colls]
        (let [step
                (fn step [cs]
                    (lazy-seq
                        (let [ss (map seq cs)]
                            (when (every? identity ss)
                                (cons (map first ss) (step (map rest ss)))
                            )
                        )
                    )
                )]
            (map #(apply f %) (step (conj colls c3 c2 c1)))
        )
    )
)

;;;
 ; defs the supplied var names with no bindings, useful for making forward declarations.
 ;;
(§ defmacro declare [& names]
    `(do
        ~@(map #(list 'def (vary-meta % assoc :declared true)) names)
    )
)

(§ declare cat)

;;;
 ; Returns the result of applying concat to the result of applying map to f and colls.
 ; Thus function f should return a collection.
 ; Returns a transducer when no collections are provided.
 ;;
(§ defn mapcat
    ([f] (comp (map f) cat))
    ([f & colls] (apply concat (apply map f colls)))
)

;;;
 ; Returns a lazy sequence of the items in coll for which (pred item) returns logical true.
 ; pred must be free of side-effects.
 ; Returns a transducer when no collection is provided.
 ;;
(§ defn filter
    ([pred]
        (fn [rf]
            (fn
                ([] (rf))
                ([result] (rf result))
                ([result input] (if (pred input) (rf result input) result))
            )
        )
    )
    ([pred coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (if (chunked-seq? s)
                    (let [c (chunk-first s) size (count c) b (chunk-buffer size)]
                        (dotimes [i size]
                            (let [v (.nth c i)]
                                (when (pred v)
                                    (chunk-append b v)
                                )
                            )
                        )
                        (chunk-cons (chunk b) (filter pred (chunk-rest s)))
                    )
                    (let [f (first s) r (rest s)]
                        (if (pred f)
                            (cons f (filter pred r))
                            (filter pred r)
                        )
                    )
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of the items in coll for which (pred item) returns logical false.
 ; pred must be free of side-effects.
 ; Returns a transducer when no collection is provided.
 ;;
(§ defn remove
    ([pred]      (filter (complement pred)     ))
    ([pred coll] (filter (complement pred) coll))
)

;;;
 ; Wraps x in a way such that a reduce will terminate with the value x.
 ;;
(§ defn reduced [x] (cloiure.lang.Reduced. x))

;;;
 ; Returns true if x is the result of a call to reduced.
 ;;
(§ defn reduced?
    {:inline (fn [x] `(cloiure.lang.RT/isReduced ~x)) :inline-arities #{1}}
    [x] (cloiure.lang.RT/isReduced x)
)

;;;
 ; If x is already reduced?, returns it, else returns (reduced x).
 ;;
(§ defn ensure-reduced [x] (if (reduced? x) x (reduced x)))

;;;
 ; If x is reduced?, returns (deref x), else returns x.
 ;;
(§ defn unreduced [x] (if (reduced? x) (deref x) x))

;;;
 ; Returns a lazy sequence of the first n items in coll, or all items if there are fewer than n.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(§ defn take
    ([n]
        (fn [rf]
            (let [nv (volatile! n)]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input]
                        (let [n @nv nn (vswap! nv dec) result (if (pos? n) (rf result input) result)]
                            (if (not (pos? nn))
                                (ensure-reduced result)
                                result
                            )
                        )
                    )
                )
            )
        )
    )
    ([n coll]
        (lazy-seq
            (when (pos? n)
                (when-let [s (seq coll)]
                    (cons (first s) (take (dec n) (rest s)))
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of successive items from coll while (pred item) returns logical true.
 ; pred must be free of side-effects.
 ; Returns a transducer when no collection is provided.
 ;;
(§ defn take-while
    ([pred]
        (fn [rf]
            (fn
                ([] (rf))
                ([result] (rf result))
                ([result input] (if (pred input) (rf result input) (reduced result)))
            )
        )
    )
    ([pred coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (when (pred (first s))
                    (cons (first s) (take-while pred (rest s)))
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of all but the first n items in coll.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(§ defn drop
    ([n]
        (fn [rf]
            (let [nv (volatile! n)]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input]
                        (let [n @nv]
                            (vswap! nv dec)
                            (if (pos? n)
                                result
                                (rf result input)
                            )
                        )
                    )
                )
            )
        )
    )
    ([n coll]
        (let [step
                (fn [n coll]
                    (let [s (seq coll)]
                        (if (and (pos? n) s)
                            (recur (dec n) (rest s))
                            s
                        )
                    )
                )]
            (lazy-seq (step n coll))
        )
    )
)

;;;
 ; Return a lazy sequence of all but the last n (default 1) items in coll.
 ;;
(§ defn drop-last
    ([coll] (drop-last 1 coll))
    ([n coll] (map (fn [x _] x) coll (drop n coll)))
)

;;;
 ; Returns a seq of the last n items in coll. Depending on the type of coll
 ; may be no better than linear time. For vectors, see also subvec.
 ;;
(§ defn take-last [n coll]
    (loop [s (seq coll) lead (seq (drop n coll))]
        (if lead
            (recur (next s) (next lead))
            s
        )
    )
)

;;;
 ; Returns a lazy sequence of the items in coll starting from the
 ; first item for which (pred item) returns logical false.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(§ defn drop-while
    ([pred]
        (fn [rf]
            (let [dv (volatile! true)]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input]
                        (let [drop? @dv]
                            (if (and drop? (pred input))
                                result
                                (do
                                    (vreset! dv nil)
                                    (rf result input)
                                )
                            )
                        )
                    )
                )
            )
        )
    )
    ([pred coll]
        (let [step
                (fn [pred coll]
                    (let [s (seq coll)]
                        (if (and s (pred (first s)))
                            (recur pred (rest s))
                            s
                        )
                    )
                )]
            (lazy-seq (step pred coll))
        )
    )
)

;;;
 ; Returns a lazy (infinite!) sequence of repetitions of the items in coll.
 ;;
(§ defn cycle [coll] (cloiure.lang.Cycle/create (seq coll)))

;;;
 ; Returns a vector of [(take n coll) (drop n coll)].
 ;;
(§ defn split-at [n coll]
    [(take n coll) (drop n coll)]
)

;;;
 ; Returns a vector of [(take-while pred coll) (drop-while pred coll)].
 ;;
(§ defn split-with [pred coll]
    [(take-while pred coll) (drop-while pred coll)]
)

;;;
 ; Returns a lazy (infinite!, or length n if supplied) sequence of xs.
 ;;
(§ defn repeat
    ([x]   (cloiure.lang.Repeat/create   x))
    ([n x] (cloiure.lang.Repeat/create n x))
)

;;;
 ; Returns a lazy sequence of x, (f x), (f (f x)), etc.
 ; f must be free of side-effects.
 ;;
(§ defn iterate [f x] (cloiure.lang.Iterate/create f x))

;;;
 ; Returns a lazy seq of nums from start (inclusive) to end (exclusive),
 ; by step, where start defaults to 0, step to 1, and end to infinity.
 ; When step is equal to 0, returns an infinite sequence of start.
 ; When start is equal to end, returns empty list.
 ;;
(§ defn range
    ([] (iterate inc' 0))
    ([end]
        (if (instance? Long end)
            (cloiure.lang.LongRange/create end)
            (cloiure.lang.Range/create end)
        )
    )
    ([start end]
        (if (and (instance? Long start) (instance? Long end))
            (cloiure.lang.LongRange/create start end)
            (cloiure.lang.Range/create start end)
        )
    )
    ([start end step]
        (if (and (instance? Long start) (instance? Long end) (instance? Long step))
            (cloiure.lang.LongRange/create start end step)
            (cloiure.lang.Range/create start end step)
        )
    )
)

;;;
 ; Returns a map that consists of the rest of the maps conj-ed onto
 ; the first. If a key occurs in more than one map, the mapping from
 ; the latter (left-to-right) will be the mapping in the result.
 ;;
(§ defn merge [& maps]
    (when (some identity maps)
        (reduce1 #(conj (or %1 {}) %2) maps)
    )
)

;;;
 ; Returns a map that consists of the rest of the maps conj-ed onto
 ; the first. If a key occurs in more than one map, the mapping(s)
 ; from the latter (left-to-right) will be combined with the mapping in
 ; the result by calling (f val-in-result val-in-latter).
 ;;
(§ defn merge-with [f & maps]
    (when (some identity maps)
        (let [merge-entry
                (fn [m e]
                    (let [k (key e) v (val e)]
                        (if (contains? m k)
                            (assoc m k (f (get m k) v))
                            (assoc m k v)
                        )
                    )
                )
              merge2
                (fn [m1 m2]
                    (reduce1 merge-entry (or m1 {}) (seq m2))
                )]
            (reduce1 merge2 maps)
        )
    )
)

;;;
 ; Returns a map with the keys mapped to the corresponding vals.
 ;;
(§ defn zipmap [keys vals]
    (loop [map {} ks (seq keys) vs (seq vals)]
        (if (and ks vs)
            (recur (assoc map (first ks) (first vs)) (next ks) (next vs))
            map
        )
    )
)

;;;
 ; Returns the lines of text from rdr as a lazy sequence of strings.
 ; rdr must implement java.io.BufferedReader.
 ;;
(§ defn line-seq [^java.io.BufferedReader rdr]
    (when-let [line (.readLine rdr)]
        (cons line (lazy-seq (line-seq rdr)))
    )
)

;;;
 ; Returns an implementation of java.util.Comparator based upon pred.
 ;;
(§ defn comparator [pred]
    (fn [x y]
        (cond (pred x y) -1 (pred y x) 1 :else 0)
    )
)

;;;
 ; Returns a sorted sequence of the items in coll.
 ; If no comparator is supplied, uses compare. comparator must implement java.util.Comparator.
 ; Guaranteed to be stable: equal elements will not be reordered.
 ; If coll is a Java array, it will be modified. To avoid this, sort a copy of the array.
 ;;
(§ defn sort
    ([coll] (sort compare coll))
    ([^java.util.Comparator comp coll]
        (if (seq coll)
            (let [a (to-array coll)]
                (java.util.Arrays/sort a comp)
                (seq a)
            )
            ()
        )
    )
)

;;;
 ; Returns a sorted sequence of the items in coll, where the sort order is determined by comparing (keyfn item).
 ; If no comparator is supplied, uses compare. comparator must implement java.util.Comparator.
 ; Guaranteed to be stable: equal elements will not be reordered.
 ; If coll is a Java array, it will be modified. To avoid this, sort a copy of the array.
 ;;
(§ defn sort-by
    ([keyfn coll]
        (sort-by keyfn compare coll)
    )
    ([keyfn ^java.util.Comparator comp coll]
        (sort (fn [x y] (.compare comp (keyfn x) (keyfn y))) coll)
    )
)

;;;
 ; When lazy sequences are produced via functions that have side
 ; effects, any effects other than those needed to produce the first
 ; element in the seq do not occur until the seq is consumed. dorun can
 ; be used to force any effects. Walks through the successive nexts of
 ; the seq, does not retain the head and returns nil.
 ;;
(§ defn dorun
    ([coll]
        (when-let [s (seq coll)]
            (recur (next s))
        )
    )
    ([n coll]
        (when (and (seq coll) (pos? n))
            (recur (dec n) (next coll))
        )
    )
)

;;;
 ; When lazy sequences are produced via functions that have side
 ; effects, any effects other than those needed to produce the first
 ; element in the seq do not occur until the seq is consumed. doall can
 ; be used to force any effects. Walks through the successive nexts of
 ; the seq, retains the head and returns it, thus causing the entire
 ; seq to reside in memory at one time.
 ;;
(§ defn doall
    ([coll]
        (dorun coll)
        coll
    )
    ([n coll]
        (dorun n coll)
        coll
    )
)

;;;
 ; Returns the nth next of coll, (seq coll) when n is 0.
 ;;
(§ defn nthnext [coll n]
    (loop [n n xs (seq coll)]
        (if (and xs (pos? n))
            (recur (dec n) (next xs))
            xs
        )
    )
)

;;;
 ; Returns the nth rest of coll, coll when n is 0.
 ;;
(§ defn nthrest [coll n]
    (loop [n n xs coll]
        (if-let [xs (and (pos? n) (seq xs))]
            (recur (dec n) (rest xs))
            xs
        )
    )
)

;;;
 ; Returns a lazy sequence of lists of n items each, at offsets step
 ; apart. If step is not supplied, defaults to n, i.e. the partitions
 ; do not overlap. If a pad collection is supplied, use its elements as
 ; necessary to complete last partition upto n items. In case there are
 ; not enough padding elements, return a partition with less than n items.
 ;;
(§ defn partition
    ([n coll] (partition n n coll))
    ([n step coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (let [p (doall (take n s))]
                    (when (= n (count p))
                        (cons p (partition n step (nthrest s step)))
                    )
                )
            )
        )
    )
    ([n step pad coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (let [p (doall (take n s))]
                    (if (= n (count p))
                        (cons p (partition n step pad (nthrest s step)))
                        (list (take n (concat p pad)))
                    )
                )
            )
        )
    )
)

;; evaluation

;;;
 ; Evaluates the form data structure (not text!) and returns the result.
 ;;
(§ defn eval [form] (cloiure.lang.Compiler/eval form))

;;;
 ; Repeatedly executes body (presumably for side-effects) with bindings and filtering as provided by "for".
 ; Does not retain the head of the sequence. Returns nil.
 ;;
(§ defmacro doseq [seq-exprs & body]
    (assert-args
        (vector? seq-exprs) "a vector for its binding"
        (even? (count seq-exprs)) "an even number of forms in binding vector"
    )
    (let [step
            (fn step [recform exprs]
                (if-not exprs
                    [true `(do ~@body)]
                    (let [k (first exprs) v (second exprs)]
                        (if (keyword? k)
                            (let [steppair (step recform (nnext exprs)) needrec (steppair 0) subform (steppair 1)]
                                (cond
                                    (= k :let)   [needrec `(let ~v ~subform)]
                                    (= k :while) [false `(when ~v ~subform ~@(when needrec [recform]))]
                                    (= k :when)  [false `(if ~v (do ~subform ~@(when needrec [recform])) ~recform)]
                                )
                            )
                            (let [seq- (gensym "seq_")
                                  chunk- (with-meta (gensym "chunk_") {:tag 'cloiure.lang.IChunk})
                                  count- (gensym "count_")
                                  i- (gensym "i_")
                                  recform `(recur (next ~seq-) nil 0 0)
                                  steppair (step recform (nnext exprs))
                                  needrec (steppair 0)
                                  subform (steppair 1)
                                  recform-chunk `(recur ~seq- ~chunk- ~count- (unchecked-inc ~i-))
                                  steppair-chunk (step recform-chunk (nnext exprs))
                                  subform-chunk (steppair-chunk 1)]
                                [true
                                    `(loop [~seq- (seq ~v), ~chunk- nil, ~count- 0, ~i- 0]
                                        (if (< ~i- ~count-)
                                            (let [~k (.nth ~chunk- ~i-)]
                                                ~subform-chunk
                                                ~@(when needrec [recform-chunk])
                                            )
                                            (when-let [~seq- (seq ~seq-)]
                                                (if (chunked-seq? ~seq-)
                                                    (let [c# (chunk-first ~seq-)]
                                                        (recur (chunk-rest ~seq-) c# (int (count c#)) (int 0))
                                                    )
                                                    (let [~k (first ~seq-)]
                                                        ~subform
                                                        ~@(when needrec [recform])
                                                    )
                                                )
                                            )
                                        )
                                    )
                                ]
                            )
                        )
                    )
                )
            )]
        (nth (step nil (seq seq-exprs)) 1)
    )
)

;;;
 ; bindings => name n
 ;
 ; Repeatedly executes body (presumably for side-effects) with name
 ; bound to integers from 0 through n-1.
 ;;
(§ defmacro dotimes [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (= 2 (count bindings)) "exactly 2 forms in binding vector"
    )
    (let [i (first bindings) n (second bindings)]
        `(let [n# (long ~n)]
            (loop [~i 0]
                (when (< ~i n#)
                    ~@body
                    (recur (unchecked-inc ~i))
                )
            )
        )
    )
)

;;;
 ; Returns a new coll consisting of to-coll with all of the items of
 ; from-coll conjoined.
 ;;
#_(defn into [to from]
    (let [ret to items (seq from)]
        (if items
            (recur (conj ret (first items)) (next items))
            ret
        )
    )
)

;;;
 ; Returns a new, transient version of the collection, in constant time.
 ;;
(§ defn transient [^cloiure.lang.IEditableCollection coll]
    (.asTransient coll)
)

;;;
 ; Returns a new, persistent version of the transient collection, in
 ; constant time. The transient collection cannot be used after this
 ; call, any such use will throw an exception.
 ;;
(§ defn persistent! [^cloiure.lang.ITransientCollection coll]
    (.persistent coll)
)

;;;
 ; Adds x to the transient collection, and return coll. The 'addition'
 ; may happen at different 'places' depending on the concrete type.
 ;;
(§ defn conj!
    ([] (transient []))
    ([coll] coll)
    ([^cloiure.lang.ITransientCollection coll x] (.conj coll x))
)

;;;
 ; When applied to a transient map, adds mapping of key(s) to val(s).
 ; When applied to a transient vector, sets the val at index.
 ; Note - index must be <= (count vector). Returns coll.
 ;;
(§ defn assoc!
    ([^cloiure.lang.ITransientAssociative coll key val] (.assoc coll key val))
    ([^cloiure.lang.ITransientAssociative coll key val & kvs]
        (let [ret (.assoc coll key val)]
            (if kvs
                (recur ret (first kvs) (second kvs) (nnext kvs))
                ret
            )
        )
    )
)

;;;
 ; Returns a transient map that doesn't contain a mapping for key(s).
 ;;
(§ defn dissoc!
    ([^cloiure.lang.ITransientMap map key] (.without map key))
    ([^cloiure.lang.ITransientMap map key & ks]
        (let [ret (.without map key)]
            (if ks
                (recur ret (first ks) (next ks))
                ret
            )
        )
    )
)

;;;
 ; Removes the last item from a transient vector.
 ; If the collection is empty, throws an exception. Returns coll.
 ;;
(§ defn pop! [^cloiure.lang.ITransientVector coll]
    (.pop coll)
)

;;;
 ; disj[oin].
 ; Returns a transient set of the same (hashed/sorted) type, that does not contain key(s).
 ;;
(§ defn disj!
    ([set] set)
    ([^cloiure.lang.ITransientSet set key] (.disjoin set key))
    ([^cloiure.lang.ITransientSet set key & ks]
        (let [ret (.disjoin set key)]
            (if ks
                (recur ret (first ks) (next ks))
                ret
            )
        )
    )
)

;; redef into with batch support

;;;
 ; Returns a new coll consisting of to-coll with all of the items of from-coll conjoined.
 ;;
(§ defn ^:private into1 [to from]
    (if (instance? cloiure.lang.IEditableCollection to)
        (persistent! (reduce1 conj! (transient to) from))
        (reduce1 conj to from)
    )
)

;;;
 ; import-list => (package-symbol class-name-symbols*)
 ;
 ; For each name in class-name-symbols, adds a mapping from name to the class named by package.name
 ; to the current namespace. Use :import in the ns macro in preference to calling this directly.
 ;;
(§ defmacro import [& import-symbols-or-lists]
    (let [specs (map #(if (and (seq? %) (= 'quote (first %))) (second %) %) import-symbols-or-lists)]
        `(do
            ~@(map #(list 'cloiure.core/import* %)
                (reduce1
                    (fn [v spec]
                        (if (symbol? spec)
                            (conj v (name spec))
                            (let [p (first spec) cs (rest spec)] (into1 v (map #(str p "." %) cs)))
                        )
                    )
                    [] specs
                )
            )
        )
    )
)

;;;
 ; Returns an array with components set to the values in aseq.
 ; The array's component type is type if provided, or the type of the first value in aseq if present, or Object.
 ; All values in aseq must be compatible with the component type.
 ; Class objects for the primitive types can be obtained using, e.g., Integer/TYPE.
 ;;
(§ defn into-array
    ([aseq]      (cloiure.lang.RT/seqToTypedArray      (seq aseq)))
    ([type aseq] (cloiure.lang.RT/seqToTypedArray type (seq aseq)))
)

(§ defn ^:private array [& items] (into-array items))

;;;
 ; Returns the Class of x.
 ;;
(§ defn ^Class class [^Object x] (if (nil? x) x (.getClass x)))

;;;
 ; Returns the :type metadata of x, or its Class if none.
 ;;
(§ defn type [x] (or (get (meta x) :type) (class x)))

;;;
 ; Coerce to Number.
 ;;
(§ defn ^Number num
    {:inline (fn [x] `(cloiure.lang.Numbers/num ~x))}
    [x] (cloiure.lang.Numbers/num x)
)

;;;
 ; Coerce to long.
 ;;
(§ defn long
    {:inline (fn [x] `(cloiure.lang.RT/longCast ~x))}
    [^Number x] (cloiure.lang.RT/longCast x)
)

;;;
 ; Coerce to float.
 ;;
(§ defn float
    {:inline (fn [x] `(cloiure.lang.RT/floatCast ~x))}
    [^Number x] (cloiure.lang.RT/floatCast x)
)

;;;
 ; Coerce to double.
 ;;
(§ defn double
    {:inline (fn [x] `(cloiure.lang.RT/doubleCast ~x))}
    [^Number x] (cloiure.lang.RT/doubleCast x)
)

;;;
 ; Coerce to short.
 ;;
(§ defn short
    {:inline (fn [x] `(cloiure.lang.RT/shortCast ~x))}
    [^Number x] (cloiure.lang.RT/shortCast x)
)

;;;
 ; Coerce to byte.
 ;;
(§ defn byte
    {:inline (fn [x] `(cloiure.lang.RT/byteCast ~x))}
    [^Number x] (cloiure.lang.RT/byteCast x)
)

;;;
 ; Coerce to char.
 ;;
(§ defn char
    {:inline (fn [x] `(cloiure.lang.RT/charCast ~x))}
    [x] (cloiure.lang.RT/charCast x)
)

;;;
 ; Coerce to byte. Subject to rounding or truncation.
 ;;
(§ defn unchecked-byte
    {:inline (fn [x] `(cloiure.lang.RT/uncheckedByteCast ~x))}
    [^Number x] (cloiure.lang.RT/uncheckedByteCast x)
)

;;;
 ; Coerce to short. Subject to rounding or truncation.
 ;;
(§ defn unchecked-short
    {:inline (fn [x] `(cloiure.lang.RT/uncheckedShortCast ~x))}
    [^Number x] (cloiure.lang.RT/uncheckedShortCast x)
)

;;;
 ; Coerce to char. Subject to rounding or truncation.
 ;;
(§ defn unchecked-char
    {:inline (fn [x] `(cloiure.lang.RT/uncheckedCharCast ~x))}
    [x] (cloiure.lang.RT/uncheckedCharCast x)
)

;;;
 ; Coerce to int. Subject to rounding or truncation.
 ;;
(§ defn unchecked-int
    {:inline (fn [x] `(cloiure.lang.RT/uncheckedIntCast ~x))}
    [^Number x] (cloiure.lang.RT/uncheckedIntCast x)
)

;;;
 ; Coerce to long. Subject to rounding or truncation.
 ;;
(§ defn unchecked-long
    {:inline (fn [x] `(cloiure.lang.RT/uncheckedLongCast ~x))}
    [^Number x] (cloiure.lang.RT/uncheckedLongCast x)
)

;;;
 ; Coerce to float. Subject to rounding.
 ;;
(§ defn unchecked-float
    {:inline (fn [x] `(cloiure.lang.RT/uncheckedFloatCast ~x))}
    [^Number x] (cloiure.lang.RT/uncheckedFloatCast x)
)

;;;
 ; Coerce to double. Subject to rounding.
 ;;
(§ defn unchecked-double
    {:inline (fn [x] `(cloiure.lang.RT/uncheckedDoubleCast ~x))}
    [^Number x] (cloiure.lang.RT/uncheckedDoubleCast x)
)

;;;
 ; Returns true if x is a Number.
 ;;
(§ defn number? [x] (instance? Number x))

;;;
 ; Modulus of num and div. Truncates toward negative infinity.
 ;;
(§ defn mod [num div]
    (let [m (rem num div)]
        (if (or (zero? m) (= (pos? num) (pos? div)))
            m
            (+ m div)
        )
    )
)

;;;
 ; Returns true if n is a Ratio.
 ;;
(§ defn ratio? [n] (instance? cloiure.lang.Ratio n))

;;;
 ; Returns the numerator part of a Ratio.
 ;;
(§ defn ^BigInteger numerator [r] (.numerator ^cloiure.lang.Ratio r))

;;;
 ; Returns the denominator part of a Ratio.
 ;;
(§ defn ^BigInteger denominator [r] (.denominator ^cloiure.lang.Ratio r))

;;;
 ; Returns true if n is a BigDecimal.
 ;;
(§ defn decimal? [n] (instance? BigDecimal n))

;;;
 ; Returns true if n is a floating point number.
 ;;
(§ defn float? [n] (or (instance? Double n) (instance? Float n)))

;;;
 ; Returns true if n is a rational number.
 ;;
(§ defn rational? [n] (or (integer? n) (ratio? n) (decimal? n)))

;;;
 ; Coerce to BigInt.
 ;;
(§ defn ^cloiure.lang.BigInt bigint [x]
    (cond
        (instance? cloiure.lang.BigInt x) x
        (instance? BigInteger x) (cloiure.lang.BigInt/fromBigInteger x)
        (decimal? x) (bigint (.toBigInteger ^BigDecimal x))
        (float? x) (bigint (BigDecimal/valueOf (double x)))
        (ratio? x) (bigint (.bigIntegerValue ^cloiure.lang.Ratio x))
        (number? x) (cloiure.lang.BigInt/valueOf (long x))
        :else (bigint (BigInteger. x))
    )
)

;;;
 ; Coerce to BigInteger.
 ;;
(§ defn ^BigInteger biginteger [x]
    (cond
        (instance? BigInteger x) x
        (instance? cloiure.lang.BigInt x) (.toBigInteger ^cloiure.lang.BigInt x)
        (decimal? x) (.toBigInteger ^BigDecimal x)
        (float? x) (.toBigInteger (BigDecimal/valueOf (double x)))
        (ratio? x) (.bigIntegerValue ^cloiure.lang.Ratio x)
        (number? x) (BigInteger/valueOf (long x))
        :else (BigInteger. x)
    )
)

;;;
 ; Coerce to BigDecimal.
 ;;
(§ defn ^BigDecimal bigdec [x]
    (cond
        (decimal? x) x
        (float? x) (BigDecimal/valueOf (double x))
        (ratio? x) (/ (BigDecimal. (.numerator ^cloiure.lang.Ratio x)) (.denominator ^cloiure.lang.Ratio x))
        (instance? cloiure.lang.BigInt x) (.toBigDecimal ^cloiure.lang.BigInt x)
        (instance? BigInteger x) (BigDecimal. ^BigInteger x)
        (number? x) (BigDecimal/valueOf (long x))
        :else (BigDecimal. x)
    )
)

(§ def ^:dynamic ^:private print-initialized false)

(§ defmulti print-method (fn [x writer] (let [t (get (meta x) :type)] (if (keyword? t) t (class x)))))

(§ defn ^:private pr-on [x w]
    (print-method x w)
    nil
)

;;;
 ; Prints the object(s) to the output stream that is the current value of *out*.
 ; Prints the object(s), separated by spaces if there is more than one.
 ; By default, pr and prn print in a way that objects can be read by the reader.
 ;;
(§ defn ^:dynamic pr
    ([] nil)
    ([x] (pr-on x *out*))
    ([x & more]
        (pr x)
        (.append *out* \space)
        (if-let [nmore (next more)]
            (recur (first more) nmore)
            (apply pr more)
        )
    )
)

;;;
 ; Writes a newline to *out*.
 ;;
(§ defn newline []
    (.append *out* \newline)
    nil
)

;;;
 ; Flushes the output stream that is the current value of *out*.
 ;;
(§ defn flush []
    (.flush *out*)
    nil
)

;;;
 ; Same as pr followed by (newline). Observes *flush-on-newline*.
 ;;
(§ defn prn [& more]
    (apply pr more)
    (newline)
    (when *flush-on-newline*
        (flush)
    )
)

;;;
 ; Prints the object(s) to the output stream that is the current value of *out*.
 ; print and println produce output for human consumption.
 ;;
(§ defn print [& more]
    (binding [*print-readably* nil]
        (apply pr more)
    )
)

;;;
 ; Same as print followed by (newline).
 ;;
(§ defn println [& more]
    (binding [*print-readably* nil]
        (apply prn more)
    )
)

;;;
 ; Reads the next object from stream, which must be an instance of
 ; java.io.PushbackReader or some derivee. stream defaults to the
 ; current value of *in*.
 ;
 ; Opts is a persistent map with valid keys:
 ;
 ; :eof - on eof, return value unless :eofthrow, then throw.
 ;        if not specified, will throw.
 ;;
(§ defn read
    ([]
        (read *in*)
    )
    ([stream]
        (read stream true nil)
    )
    ([stream eof-error? eof-value]
        (read stream eof-error? eof-value false)
    )
    ([stream eof-error? eof-value recursive?]
        (cloiure.lang.LispReader/read stream (boolean eof-error?) eof-value recursive?)
    )
)

;;;
 ; Reads the next line from stream that is the current value of *in*.
 ;;
(§ defn read-line []
    (if (instance? cloiure.lang.LineNumberingPushbackReader *in*)
        (.readLine ^cloiure.lang.LineNumberingPushbackReader *in*)
        (.readLine ^java.io.BufferedReader *in*)
    )
)

;;;
 ; Reads one object from the string s.
 ;;
(§ defn read-string [s] (cloiure.lang.RT/readString s))

;;;
 ; Returns a persistent vector of the items in vector from start (inclusive) to end (exclusive).
 ; If end is not supplied, defaults to (count vector). This operation is O(1) and very fast, as
 ; the resulting vector shares structure with the original and no trimming is done.
 ;;
(§ defn subvec
    ([v start] (subvec v start (count v)))
    ([v start end] (cloiure.lang.RT/subvec v start end))
)

;;;
 ; bindings => [name init ...]
 ;
 ; Evaluates body in a try expression with names bound to the values of the inits,
 ; and a finally clause that calls (.close name) on each name in reverse order.
 ;;
(§ defmacro with-open [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (even? (count bindings)) "an even number of forms in binding vector"
    )
    (cond
        (= (count bindings) 0) `(do ~@body)
        (symbol? (bindings 0))
            `(let ~(subvec bindings 0 2)
                (try
                    (with-open ~(subvec bindings 2) ~@body)
                    (finally
                        (.close ~(bindings 0))
                    )
                )
            )
        :else (throw (IllegalArgumentException. "with-open only allows Symbols in bindings"))
    )
)

;;;
 ; Evaluates x, then calls all of the methods and functions with the
 ; value of x supplied at the front of the given arguments. The forms
 ; are evaluated in order. Returns x.
 ;
 ; (doto (new java.util.HashMap) (.put "a" 1) (.put "b" 2))
 ;;
(§ defmacro doto [x & forms]
    (let [gx (gensym)]
        `(let [~gx ~x]
            ~@(map (fn [f] (with-meta (if (seq? f) `(~(first f) ~gx ~@(next f)) `(~f ~gx)) (meta f))) forms)
            ~gx
        )
    )
)

;;;
 ; Expands into code that creates a fn that expects to be passed an
 ; object and any args and calls the named instance method on the
 ; object passing the args. Use when you want to treat a Java method as
 ; a first-class fn. name may be type-hinted with the method receiver's
 ; type in order to avoid reflective calls.
 ;;
(§ defmacro memfn [name & args]
    (let [t (with-meta (gensym "target") (meta name))]
        `(fn [~t ~@args] (. ~t (~name ~@args)))
    )
)

;;;
 ; Evaluates expr and prints the time it took. Returns the value of expr.
 ;;
(§ defmacro time [expr]
    `(let [start# (System/nanoTime) ret# ~expr]
        (prn (str "Elapsed time: " (/ (double (- (System/nanoTime) start#)) 1000000.0) " msecs"))
        ret#
    )
)

(§ import [java.lang.reflect Array])

;;;
 ; Returns the length of the Java array. Works on arrays of all types.
 ;;
(§ defn alength
    {:inline (fn [a] `(cloiure.lang.RT/alength ~a))}
    [array] (cloiure.lang.RT/alength array)
)

;;;
 ; Returns a clone of the Java array. Works on arrays of known types.
 ;;
(§ defn aclone
    {:inline (fn [a] `(cloiure.lang.RT/aclone ~a))}
    [array] (cloiure.lang.RT/aclone array)
)

;;;
 ; Returns the value at the index/indices. Works on Java arrays of all types.
 ;;
(§ defn aget
    {:inline (fn [a i] `(cloiure.lang.RT/aget ~a (int ~i))) :inline-arities #{2}}
    ([array idx]
        (cloiure.lang.Reflector/prepRet (.getComponentType (class array)) (Array/get array idx))
    )
    ([array idx & idxs]
        (apply aget (aget array idx) idxs)
    )
)

;;;
 ; Sets the value at the index/indices.
 ; Works on Java arrays of reference types. Returns val.
 ;;
(§ defn aset
    {:inline (fn [a i v] `(cloiure.lang.RT/aset ~a (int ~i) ~v)) :inline-arities #{3}}
    ([array idx val]
        (Array/set array idx val)
        val
    )
    ([array idx idx2 & idxv]
        (apply aset (aget array idx) idx2 idxv)
    )
)

(§ defmacro ^:private def-aset [name method coerce]
    `(defn ~name
        ([array# idx# val#]
            (. Array (~method array# idx# (~coerce val#)))
            val#
        )
        ([array# idx# idx2# & idxv#]
            (apply ~name (aget array# idx#) idx2# idxv#)
        )
    )
)

;;;
 ; Sets the value at the index/indices. Works on arrays of int. Returns val.
 ;;
(§ def-aset aset-int setInt int)

;;;
 ; Sets the value at the index/indices. Works on arrays of long. Returns val.
 ;;
(§ def-aset aset-long setLong long)

;;;
 ; Sets the value at the index/indices. Works on arrays of boolean. Returns val.
 ;;
(§ def-aset aset-boolean setBoolean boolean)

;;;
 ; Sets the value at the index/indices. Works on arrays of float. Returns val.
 ;;
(§ def-aset aset-float setFloat float)

;;;
 ; Sets the value at the index/indices. Works on arrays of double. Returns val.
 ;;
(§ def-aset aset-double setDouble double)

;;;
 ; Sets the value at the index/indices. Works on arrays of short. Returns val.
 ;;
(§ def-aset aset-short setShort short)

;;;
 ; Sets the value at the index/indices. Works on arrays of byte. Returns val.
 ;;
(§ def-aset aset-byte setByte byte)

;;;
 ; Sets the value at the index/indices. Works on arrays of char. Returns val.
 ;;
(§ def-aset aset-char setChar char)

;;;
 ; Creates and returns an array of instances of the specified class of the specified dimension(s).
 ; Note that a class object is required.
 ; Class objects can be obtained by using their imported or fully-qualified name.
 ; Class objects for the primitive types can be obtained using, e.g., Integer/TYPE.
 ;;
(§ defn make-array
    ([^Class type len] (Array/newInstance type (int len)))
    ([^Class type dim & more-dims]
        (let [dims (cons dim more-dims) ^"[I" dimarray (make-array Integer/TYPE (count dims))]
            (dotimes [i (alength dimarray)]
                (aset-int dimarray i (nth dims i))
            )
            (Array/newInstance type dimarray)
        )
    )
)

;;;
 ; If form represents a macro form, returns its expansion, else returns form.
 ;;
(§ defn macroexpand-1 [form] (cloiure.lang.Compiler/macroexpand1 form))

;;;
 ; Repeatedly calls macroexpand-1 on form until it no longer
 ; represents a macro form, then returns it. Note neither
 ; macroexpand-1 nor macroexpand expand macros in subforms.
 ;;
(§ defn macroexpand [form]
    (let [ex (macroexpand-1 form)]
        (if (identical? ex form)
            form
            (macroexpand ex)
        )
    )
)

;;;
 ; Sequentially read and evaluate the set of forms contained in the stream/file.
 ;;
(§ defn load-reader [rdr] (cloiure.lang.Compiler/load rdr))

;;;
 ; Sequentially read and evaluate the set of forms contained in the string.
 ;;
(§ defn load-string [s]
    (let [rdr (-> s (java.io.StringReader.) (cloiure.lang.LineNumberingPushbackReader.))]
        (load-reader rdr)
    )
)

;;;
 ; Returns true if x implements IPersistentSet.
 ;;
(§ defn set? [x] (instance? cloiure.lang.IPersistentSet x))

;;;
 ; Returns a set of the distinct elements of coll.
 ;;
(§ defn set [coll]
    (if (set? coll)
        (with-meta coll nil)
        (if (instance? cloiure.lang.IReduceInit coll)
            (persistent! (.reduce ^cloiure.lang.IReduceInit coll conj! (transient #{})))
            (persistent! (reduce1 conj! (transient #{}) coll))
        )
    )
)

(§ defn ^:private filter-key [keyfn pred amap]
    (loop [ret {} es (seq amap)]
        (if es
            (if (pred (keyfn (first es)))
                (recur (assoc ret (key (first es)) (val (first es))) (next es))
                (recur ret (next es))
            )
            ret
        )
    )
)

;;;
 ; Returns the namespace named by the symbol or nil if it doesn't exist.
 ;;
(§ defn find-ns [sym] (cloiure.lang.Namespace/find sym))

;;;
 ; Create a new namespace named by the symbol if one doesn't already exist,
 ; returns it or the already-existing namespace of the same name.
 ;;
(§ defn create-ns [sym] (cloiure.lang.Namespace/findOrCreate sym))

;;;
 ; Removes the namespace named by the symbol. Use with caution.
 ; Cannot be used to remove the cloiure namespace.
 ;;
(§ defn remove-ns [sym] (cloiure.lang.Namespace/remove sym))

;;;
 ; Returns a sequence of all namespaces.
 ;;
(§ defn all-ns [] (cloiure.lang.Namespace/all))

;;;
 ; If passed a namespace, returns it. Else, when passed a symbol,
 ; returns the namespace named by it, throwing an exception if not found.
 ;;
(§ defn ^cloiure.lang.Namespace the-ns [x]
    (if (instance? cloiure.lang.Namespace x)
        x
        (or (find-ns x) (throw (Exception. (str "No namespace: " x " found"))))
    )
)

;;;
 ; Returns the name of the namespace, a symbol.
 ;;
(§ defn ns-name [ns]
    (.getName (the-ns ns))
)

;;;
 ; Returns a map of all the mappings for the namespace.
 ;;
(§ defn ns-map [ns]
    (.getMappings (the-ns ns))
)

;;;
 ; Removes the mappings for the symbol from the namespace.
 ;;
(§ defn ns-unmap [ns sym]
    (.unmap (the-ns ns) sym)
)

;;;
 ; Returns a map of the public intern mappings for the namespace.
 ;;
(§ defn ns-publics [ns]
    (let [ns (the-ns ns)]
        (filter-key val
            (fn [^cloiure.lang.Var v]
                (and (instance? cloiure.lang.Var v) (= ns (.ns v)) (.isPublic v))
            )
            (ns-map ns)
        )
    )
)

;;;
 ; Returns a map of the import mappings for the namespace.
 ;;
(§ defn ns-imports [ns]
    (filter-key val (partial instance? Class) (ns-map ns))
)

;;;
 ; Returns a map of the intern mappings for the namespace.
 ;;
(§ defn ns-interns [ns]
    (let [ns (the-ns ns)]
        (filter-key val
            (fn [^cloiure.lang.Var v]
                (and (instance? cloiure.lang.Var v) (= ns (.ns v)))
            )
            (ns-map ns)
        )
    )
)

;;;
 ; refers to all public vars of ns, subject to filters.
 ; filters can include at most one each of:
 ;
 ; :exclude list-of-symbols
 ; :only    list-of-symbols
 ; :rename  map-of-fromsymbol-tosymbol
 ;
 ; For each public interned var in the namespace named by the symbol, adds a mapping
 ; from the name of the var to the var to the current namespace. Throws an exception
 ; if name is already mapped to something else in the current namespace. Filters can
 ; be used to select a subset, via inclusion or exclusion, or to provide a mapping
 ; to a symbol different from the var's name, in order to prevent clashes.
 ; Use :use in the ns macro in preference to calling this directly.
 ;;
(§ defn refer [ns-sym & filters]
    (let [ns (or (find-ns ns-sym) (throw (Exception. (str "No namespace: " ns-sym))))
          fs (apply hash-map filters)
          nspublics (ns-publics ns)
          rename (or (:rename fs) {})
          exclude (set (:exclude fs))
          to-do
            (if (= :all (:refer fs))
                (keys nspublics)
                (or (:refer fs) (:only fs) (keys nspublics))
            )]
        (when (and to-do (not (instance? cloiure.lang.Sequential to-do)))
            (throw (Exception. ":only/:refer value must be a sequential collection of symbols"))
        )
        (doseq [sym to-do]
            (when-not (exclude sym)
                (let [v (nspublics sym)]
                    (when-not v
                        (throw (java.lang.IllegalAccessError.
                            (if (get (ns-interns ns) sym)
                                (str sym " is not public")
                                (str sym " does not exist")
                            )
                        ))
                    )
                    (.refer *ns* (or (rename sym) sym) v)
                )
            )
        )
    )
)

;;;
 ; Returns a map of the refer mappings for the namespace.
 ;;
(§ defn ns-refers [ns]
    (let [ns (the-ns ns)]
        (filter-key val
            (fn [^cloiure.lang.Var v]
                (and (instance? cloiure.lang.Var v) (not= ns (.ns v)))
            )
            (ns-map ns)
        )
    )
)

;;;
 ; Add an alias in the current namespace to another namespace.
 ; Arguments are two symbols: the alias to be used, and the symbolic name of the target namespace.
 ; Use :as in the ns macro in preference to calling this directly.
 ;;
(§ defn alias [alias namespace-sym]
    (.addAlias *ns* alias (the-ns namespace-sym))
)

;;;
 ; Returns a map of the aliases for the namespace.
 ;;
(§ defn ns-aliases [ns]
    (.getAliases (the-ns ns))
)

;;;
 ; Removes the alias for the symbol from the namespace.
 ;;
(§ defn ns-unalias [ns sym]
    (.removeAlias (the-ns ns) sym)
)

;;;
 ; Returns a lazy seq of every nth item in coll.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(§ defn take-nth
    ([n]
        (fn [rf]
            (let [iv (volatile! -1)]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input]
                        (let [i (vswap! iv inc)]
                            (if (zero? (rem i n))
                                (rf result input)
                                result
                            )
                        )
                    )
                )
            )
        )
    )
    ([n coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (cons (first s) (take-nth n (drop n s)))
            )
        )
    )
)

;;;
 ; Returns a lazy seq of the first item in each coll, then the second, etc.
 ;;
(§ defn interleave
    ([] ())
    ([c1] (lazy-seq c1))
    ([c1 c2]
        (lazy-seq
            (let [s1 (seq c1) s2 (seq c2)]
                (when (and s1 s2)
                    (cons (first s1) (cons (first s2) (interleave (rest s1) (rest s2))))
                )
            )
        )
    )
    ([c1 c2 & colls]
        (lazy-seq
            (let [ss (map seq (conj colls c2 c1))]
                (when (every? identity ss)
                    (concat (map first ss) (apply interleave (map rest ss)))
                )
            )
        )
    )
)

;;;
 ; Gets the value in the var object.
 ;;
(§ defn var-get [^cloiure.lang.Var x] (.get x))

;;;
 ; Sets the value in the var object to val.
 ; The var must be thread-locally bound.
 ;;
(§ defn var-set [^cloiure.lang.Var x val] (.set x val))

;;;
 ; varbinding => symbol init-expr
 ;
 ; Executes the exprs in a context in which the symbols are bound to
 ; vars with per-thread bindings to the init-exprs. The symbols refer
 ; to the var objects themselves, and must be accessed with var-get and
 ; var-set.
 ;;
(§ defmacro with-local-vars [name-vals-vec & body]
    (assert-args
        (vector? name-vals-vec) "a vector for its binding"
        (even? (count name-vals-vec)) "an even number of forms in binding vector"
    )
    `(let [~@(interleave (take-nth 2 name-vals-vec) (repeat '(.setDynamic (cloiure.lang.Var/create))))]
        (cloiure.lang.Var/pushThreadBindings (hash-map ~@name-vals-vec))
        (try
            ~@body
            (finally
                (cloiure.lang.Var/popThreadBindings)
            )
        )
    )
)

;;;
 ; Returns the var or Class to which a symbol will be resolved in the namespace
 ; (unless found in the environment), else nil. Note that if the symbol is fully qualified,
 ; the var/Class to which it resolves need not be present in the namespace.
 ;;
(§ defn ns-resolve
    ([ns sym] (ns-resolve ns nil sym))
    ([ns env sym]
        (when-not (contains? env sym)
            (cloiure.lang.Compiler/maybeResolveIn (the-ns ns) sym)
        )
    )
)

(§ defn resolve
    ([    sym] (ns-resolve *ns*     sym))
    ([env sym] (ns-resolve *ns* env sym))
)

;;;
 ; Constructs an array-map.
 ; If any keys are equal, they are handled as if by repeated uses of assoc.
 ;;
(§ defn array-map
    ([] cloiure.lang.PersistentArrayMap/EMPTY)
    ([& keyvals] (cloiure.lang.PersistentArrayMap/createAsIfByAssoc (to-array keyvals)))
)

;; redefine let and loop with destructuring

(§ defn destructure [bindings]
    (let [bents (partition 2 bindings)
          pb (fn pb [bvec b v]
                (let [pvec
                        (fn [bvec b val]
                            (let [gvec (gensym "vec__") gseq (gensym "seq__") gfirst (gensym "first__") has-rest (some #{'&} b)]
                                (loop [ret (let [ret (conj bvec gvec val)] (if has-rest (conj ret gseq (list `seq gvec)) ret)) n 0 bs b seen-rest? false]
                                    (if (seq bs)
                                        (let [firstb (first bs)]
                                            (cond
                                                (= firstb '&)   (recur (pb ret (second bs) gseq) n (nnext bs) true)
                                                (= firstb :as)  (pb ret (second bs) gvec)
                                                :else           (if seen-rest?
                                                                    (throw (Exception. "Unsupported binding form, only :as can follow & parameter"))
                                                                    (recur
                                                                        (pb (if has-rest (conj ret gfirst `(first ~gseq) gseq `(next ~gseq)) ret)
                                                                            firstb
                                                                            (if has-rest gfirst (list `nth gvec n nil))
                                                                        )
                                                                        (inc n) (next bs) seen-rest?
                                                                    )
                                                                )
                                            )
                                        )
                                        ret
                                    )
                                )
                            )
                        )
                      pmap
                        (fn [bvec b v]
                            (let [gmap (gensym "map__") gmapseq (with-meta gmap {:tag 'cloiure.lang.ISeq}) defaults (:or b)]
                                (loop [ret (-> (conj bvec gmap v gmap)
                                            (conj `(if (seq? ~gmap) (cloiure.lang.PersistentHashMap/create (seq ~gmapseq)) ~gmap))
                                            ((fn [ret] (if (:as b) (conj ret (:as b) gmap) ret)))
                                        )
                                       bes (let [trafos (reduce1
                                                    (fn [trafos mk]
                                                        (if (keyword? mk)
                                                            (let [mkns (namespace mk) mkn (name mk)]
                                                                (cond
                                                                    (= mkn "keys") (assoc trafos mk #(keyword (or mkns (namespace %)) (name %)))
                                                                    (= mkn "syms") (assoc trafos mk #(list `quote (symbol (or mkns (namespace %)) (name %))))
                                                                    (= mkn "strs") (assoc trafos mk str)
                                                                    :else trafos
                                                                )
                                                            )
                                                            trafos
                                                        )
                                                    )
                                                    {} (keys b)
                                                )]
                                            (reduce1
                                                (fn [bes entry] (reduce1 #(assoc %1 %2 ((val entry) %2)) (dissoc bes (key entry)) ((key entry) bes)))
                                                (dissoc b :as :or) trafos
                                            )
                                        )]
                                    (if (seq bes)
                                        (let [bb (key (first bes)) bk (val (first bes))
                                              local (if (instance? cloiure.lang.Named bb) (with-meta (symbol nil (name bb)) (meta bb)) bb)
                                              bv (if (contains? defaults local)
                                                    (list `get gmap bk (defaults local))
                                                    (list `get gmap bk)
                                                )]
                                            (recur (if (ident? bb) (conj ret local bv) (pb ret bb bv)) (next bes))
                                        )
                                        ret
                                    )
                                )
                            )
                        )]
                    (cond
                        (symbol? b) (conj bvec b v)
                        (vector? b) (pvec bvec b v)
                        (map? b) (pmap bvec b v)
                        :else (throw (Exception. (str "Unsupported binding form: " b)))
                    )
                )
            )
          process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
        (if (every? symbol? (map first bents))
            bindings
            (reduce1 process-entry [] bents)
        )
    )
)

;;;
 ; binding => binding-form init-expr
 ;
 ; Evaluates the exprs in a lexical context in which the symbols in the
 ; binding-forms are bound to their respective init-exprs or parts therein.
 ;;
(§ defmacro let {:special-form true, :forms '[(let [bindings*] exprs*)]} [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (even? (count bindings)) "an even number of forms in binding vector"
    )
    `(let* ~(destructure bindings) ~@body)
)

(§ defn ^:private maybe-destructured [params body]
    (if (every? symbol? params)
        (cons params body)
        (loop [params params new-params (with-meta [] (meta params)) lets []]
            (if params
                (if (symbol? (first params))
                    (recur (next params) (conj new-params (first params)) lets)
                    (let [gparam (gensym "p__")]
                        (recur (next params) (conj new-params gparam) (conj lets (first params) gparam))
                    )
                )
                `(~new-params (let ~lets ~@body))
            )
        )
    )
)

;; redefine fn with destructuring

;;;
 ; params => positional-params*, or positional-params* & next-param
 ; positional-param => binding-form
 ; next-param => binding-form
 ; name => symbol
 ;
 ; Defines a function.
 ;;
(§ defmacro fn {:special-form true, :forms '[(fn name? [params* ] exprs*) (fn name? ([params* ] exprs*)+)]} [& sigs]
    (let [name (when (symbol? (first sigs)) (first sigs))
          sigs (if name (next sigs) sigs)
          sigs
            (if (vector? (first sigs))
                (list sigs)
                (if (seq? (first sigs))
                    sigs
                    ;; assume single arity syntax
                    (throw (IllegalArgumentException.
                        (if (seq sigs)
                            (str "Parameter declaration " (first sigs) " should be a vector")
                            (str "Parameter declaration missing")
                        )
                    ))
                )
            )
          psig
            (fn* [sig]
                ;; ensure correct type before destructuring sig
                (when (not (seq? sig))
                    (throw (IllegalArgumentException. (str "Invalid signature " sig " should be a list")))
                )
                (let [[params & body] sig
                      _ (when (not (vector? params))
                            (throw (IllegalArgumentException.
                                (if (seq? (first sigs))
                                    (str "Parameter declaration " params " should be a vector")
                                    (str "Invalid signature " sig " should be a list")
                                )
                            ))
                        )
                      conds (when (and (next body) (map? (first body))) (first body))
                      body (if conds (next body) body)
                      conds (or conds (meta params))
                ]
                    (maybe-destructured params body)
                )
            )
          new-sigs (map psig sigs)]
        (with-meta (if name (list* 'fn* name new-sigs) (cons 'fn* new-sigs)) (meta &form))
    )
)

;;;
 ; Evaluates the exprs in a lexical context in which the symbols in
 ; the binding-forms are bound to their respective init-exprs or parts
 ; therein. Acts as a recur target.
 ;;
(§ defmacro loop {:special-form true, :forms '[(loop [bindings*] exprs*)]} [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (even? (count bindings)) "an even number of forms in binding vector"
    )
    (let [db (destructure bindings)]
        (if (= db bindings)
            `(loop* ~bindings ~@body)
            (let [vs (take-nth 2 (drop 1 bindings))
                  bs (take-nth 2 bindings)
                  gs (map (fn [b] (if (symbol? b) b (gensym))) bs)
                  bfs (reduce1
                        (fn [ret [b v g]]
                            (if (symbol? b)
                                (conj ret g v)
                                (conj ret g v b g)
                            )
                        )
                        [] (map vector bs vs gs)
                    )]
                `(let ~bfs
                    (loop* ~(vec (interleave gs gs))
                        (let ~(vec (interleave bs gs))
                            ~@body
                        )
                    )
                )
            )
        )
    )
)

;;;
 ; bindings => x xs
 ;
 ; Roughly the same as (when (seq xs) (let [x (first xs)] body)) but xs is evaluated only once.
 ;;
(§ defmacro when-first [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (= 2 (count bindings)) "exactly 2 forms in binding vector"
    )
    (let [[x xs] bindings]
        `(when-let [xs# (seq ~xs)]
            (let [~x (first xs#)]
                ~@body
            )
        )
    )
)

;;;
 ; Expands to code which yields a lazy sequence of the concatenation
 ; of the supplied colls. Each coll expr is not evaluated until it is
 ; needed.
 ;
 ; (lazy-cat xs ys zs) === (concat (lazy-seq xs) (lazy-seq ys) (lazy-seq zs))
 ;;
(§ defmacro lazy-cat [& colls]
    `(concat ~@(map #(list `lazy-seq %) colls))
)

;;;
 ; List comprehension.
 ; Takes a vector of one or more binding-form/collection-expr pairs, each followed
 ; by zero or more modifiers, and yields a lazy sequence of evaluations of expr.
 ; Collections are iterated in a nested fashion, rightmost fastest, and nested
 ; coll-exprs can refer to bindings created in prior binding-forms.
 ; Supported modifiers are: :let [binding-form expr ...], :while test, :when test.
 ;
 ; (take 100 (for [x (range 100000000) y (range 1000000) :while (< y x)] [x y]))
 ;;
(§ defmacro for [seq-exprs body-expr]
    (assert-args
        (vector? seq-exprs) "a vector for its binding"
        (even? (count seq-exprs)) "an even number of forms in binding vector"
    )
    (let [to-groups
            (fn [seq-exprs]
                (reduce1
                    (fn [groups [k v]]
                        (if (keyword? k)
                            (conj (pop groups) (conj (peek groups) [k v]))
                            (conj groups [k v])
                        )
                    )
                    [] (partition 2 seq-exprs)
                )
            )
          err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))
          emit-bind
            (fn emit-bind [[[bind expr & mod-pairs] & [[_ next-expr] :as next-groups]]]
                (let [giter (gensym "iter__") gxs (gensym "s__")
                      do-mod
                        (fn do-mod [[[k v :as pair] & etc]]
                            (cond
                                (= k :let) `(let ~v ~(do-mod etc))
                                (= k :while) `(when ~v ~(do-mod etc))
                                (= k :when) `(if ~v ~(do-mod etc) (recur (rest ~gxs)))
                                (keyword? k) (err "Invalid 'for' keyword " k)
                                next-groups
                                    `(let [iterys# ~(emit-bind next-groups) fs# (seq (iterys# ~next-expr))]
                                        (if fs#
                                            (concat fs# (~giter (rest ~gxs)))
                                            (recur (rest ~gxs))
                                        )
                                    )
                                :else `(cons ~body-expr (~giter (rest ~gxs)))
                            )
                        )]
                    (if next-groups
                        #_"not the inner-most loop"
                        `(fn ~giter [~gxs]
                            (lazy-seq
                                (loop [~gxs ~gxs]
                                    (when-first [~bind ~gxs]
                                        ~(do-mod mod-pairs)
                                    )
                                )
                            )
                        )
                        #_"inner-most loop"
                        (let [gi (gensym "i__") gb (gensym "b__")
                              do-cmod
                                (fn do-cmod [[[k v :as pair] & etc]]
                                    (cond
                                        (= k :let) `(let ~v ~(do-cmod etc))
                                        (= k :while) `(when ~v ~(do-cmod etc))
                                        (= k :when) `(if ~v ~(do-cmod etc) (recur (unchecked-inc ~gi)))
                                        (keyword? k) (err "Invalid 'for' keyword " k)
                                        :else `(do (chunk-append ~gb ~body-expr) (recur (unchecked-inc ~gi)))
                                    )
                                )]
                            `(fn ~giter [~gxs]
                                (lazy-seq
                                    (loop [~gxs ~gxs]
                                        (when-let [~gxs (seq ~gxs)]
                                            (if (chunked-seq? ~gxs)
                                                (let [c# (chunk-first ~gxs) size# (int (count c#)) ~gb (chunk-buffer size#)]
                                                    (if (loop [~gi (int 0)]
                                                            (if (< ~gi size#)
                                                                (let [~bind (.nth c# ~gi)]
                                                                    ~(do-cmod mod-pairs)
                                                                )
                                                                true
                                                            )
                                                        )
                                                        (chunk-cons (chunk ~gb) (~giter (chunk-rest ~gxs)))
                                                        (chunk-cons (chunk ~gb) nil)
                                                    )
                                                )
                                                (let [~bind (first ~gxs)]
                                                    ~(do-mod mod-pairs)
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )]
        `(let [iter# ~(emit-bind (to-groups seq-exprs))]
            (iter# ~(second seq-exprs))
        )
    )
)

;;;
 ; Ignores body, yields nil.
 ;;
(§ defmacro comment [& body])

;;;
 ; Evaluates exprs in a context in which *out* is bound to a fresh StringWriter.
 ; Returns the string created by any nested printing calls.
 ;;
(§ defmacro with-out-str [& body]
    `(let [s# (java.io.StringWriter.)]
        (binding [*out* s#]
            ~@body
            (str s#)
        )
    )
)

;;;
 ; Evaluates body in a context in which *in* is bound to a fresh StringReader
 ; initialized with the string s.
 ;;
(§ defmacro with-in-str [s & body]
    `(with-open [s# (-> ~s (java.io.StringReader.) (cloiure.lang.LineNumberingPushbackReader.))]
        (binding [*in* s#]
            ~@body
        )
    )
)

;;;
 ; pr/prn/print/println to a string, returning it.
 ;;
(§ defn ^String pr-str      [& xs] (with-out-str (apply pr      xs)))
(§ defn ^String prn-str     [& xs] (with-out-str (apply prn     xs)))
(§ defn ^String print-str   [& xs] (with-out-str (apply print   xs)))
(§ defn ^String println-str [& xs] (with-out-str (apply println xs)))

(§ import [cloiure.lang ExceptionInfo IExceptionInfo])

;;;
 ; Create an instance of ExceptionInfo, a RuntimeException subclass
 ; that carries a map of additional data.
 ;;
(§ defn ex-info
    ([msg map      ] (ExceptionInfo. msg map      ))
    ([msg map cause] (ExceptionInfo. msg map cause))
)

;;;
 ; Returns exception data (a map) if ex is an IExceptionInfo.
 ; Otherwise returns nil.
 ;;
(§ defn ex-data [ex]
    (when (instance? IExceptionInfo ex)
        (.getData ^IExceptionInfo ex)
    )
)

;;;
 ; Evaluates expr and throws an exception if it does not evaluate to logical true.
 ;;
(§ defmacro assert
    ([x]
        (when *assert*
            `(when-not ~x
                (throw (AssertionError. (str "Assert failed: " (pr-str '~x))))
            )
        )
    )
    ([x message]
        (when *assert*
            `(when-not ~x
                (throw (AssertionError. (str "Assert failed: " ~message "\n" (pr-str '~x))))
            )
        )
    )
)

;;;
 ; Returns an instance of java.util.regex.Pattern, for use, e.g. in re-matcher.
 ;;
(§ defn ^java.util.regex.Pattern re-pattern [s]
    (if (instance? java.util.regex.Pattern s)
        s
        (java.util.regex.Pattern/compile s)
    )
)

;;;
 ; Returns an instance of java.util.regex.Matcher, for use, e.g. in re-find.
 ;;
(§ defn ^java.util.regex.Matcher re-matcher [^java.util.regex.Pattern re s]
    (.matcher re s)
)

;;;
 ; Returns the groups from the most recent match/find. If there are no
 ; nested groups, returns a string of the entire match. If there are
 ; nested groups, returns a vector of the groups, the first element
 ; being the entire match.
 ;;
(§ defn re-groups [^java.util.regex.Matcher m]
    (let [gc (.groupCount m)]
        (if (zero? gc)
            (.group m)
            (loop [ret [] c 0]
                (if (<= c gc)
                    (recur (conj ret (.group m c)) (inc c))
                    ret
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of successive matches of pattern in string,
 ; using java.util.regex.Matcher.find(), each such match processed with
 ; re-groups.
 ;;
(§ defn re-seq [^java.util.regex.Pattern re s]
    (let [m (re-matcher re s)]
        ((fn step []
            (when (.find m)
                (cons (re-groups m) (lazy-seq (step)))
            )
        ))
    )
)

;;;
 ; Returns the match, if any, of string to pattern, using
 ; java.util.regex.Matcher.matches(). Uses re-groups to return
 ; the groups.
 ;;
(§ defn re-matches [^java.util.regex.Pattern re s]
    (let [m (re-matcher re s)]
        (when (.matches m)
            (re-groups m)
        )
    )
)

;;;
 ; Returns the next regex match, if any, of string to pattern, using
 ; java.util.regex.Matcher.find(). Uses re-groups to return
 ; the groups.
 ;;
(§ defn re-find
    ([^java.util.regex.Matcher m]
        (when (.find m)
            (re-groups m)
        )
    )
    ([^java.util.regex.Pattern re s]
        (let [m (re-matcher re s)]
            (re-find m)
        )
    )
)

;;;
 ; Returns a random floating point number between 0 (inclusive) and
 ; n (default 1) (exclusive).
 ;;
(§ defn rand
    ([] (Math/random))
    ([n] (* n (rand)))
)

;;;
 ; Returns a random integer between 0 (inclusive) and n (exclusive).
 ;;
(§ defn rand-int [n] (int (rand n)))

;;;
 ; Same as defn, yielding non-public def.
 ;;
(§ defmacro defn- [name & decls]
    (list* `defn (with-meta name (assoc (meta name) :private true)) decls)
)

;;;
 ; Returns a lazy sequence of the nodes in a tree, via a depth-first walk.
 ; branch? must be a fn of one arg that returns true if passed a node
 ; that can have children (but may not). children must be a fn of one
 ; arg that returns a sequence of the children. Will only be called on
 ; nodes for which branch? returns true. Root is the root node of the
 ; tree.
 ;;
(§ defn tree-seq [branch? children root]
    (let [walk
            (fn walk [node]
                (lazy-seq
                    (cons node (when (branch? node) (mapcat walk (children node))))
                )
            )]
        (walk root)
    )
)

;;;
 ; Returns true if s names a special form.
 ;;
(§ defn special-symbol? [s] (contains? cloiure.lang.Compiler/specials s))

;;;
 ; Returns true if v is of type cloiure.lang.Var.
 ;;
(§ defn var? [v] (instance? cloiure.lang.Var v))

;;;
 ; Returns the substring of s beginning at start inclusive,
 ; and ending at end (defaults to length of string), exclusive.
 ;;
(§ defn ^String subs
    ([^String s start    ] (.substring s start    ))
    ([^String s start end] (.substring s start end))
)

;;;
 ; Returns the x for which (k x), a number, is greatest.
 ; If there are multiple such xs, the last one is returned.
 ;;
(§ defn max-key
    ([k x] x)
    ([k x y] (if (> (k x) (k y)) x y))
    ([k x y & more]
        (let [kx (k x) ky (k y) [v kv] (if (> kx ky) [x kx] [y ky])]
            (loop [v v kv kv more more]
                (if more
                    (let [w (first more) kw (k w)]
                        (if (>= kw kv)
                            (recur w kw (next more))
                            (recur v kv (next more))
                        )
                    )
                    v
                )
            )
        )
    )
)

;;;
 ; Returns the x for which (k x), a number, is least.
 ; If there are multiple such xs, the last one is returned.
 ;;
(§ defn min-key
    ([k x] x)
    ([k x y] (if (< (k x) (k y)) x y))
    ([k x y & more]
        (let [kx (k x) ky (k y) [v kv] (if (< kx ky) [x kx] [y ky])]
            (loop [v v kv kv more more]
                (if more
                    (let [w (first more) kw (k w)]
                        (if (<= kw kv)
                            (recur w kw (next more))
                            (recur v kv (next more))
                        )
                    )
                    v
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of the elements of coll with duplicates removed.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(§ defn distinct
    ([]
        (fn [rf]
            (let [seen (volatile! #{})]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input]
                        (if (contains? @seen input)
                            result
                            (do (vswap! seen conj input) (rf result input))
                        )
                    )
                )
            )
        )
    )
    ([coll]
        (let [step
                (fn step [xs seen]
                    (lazy-seq
                        ((fn [[f :as xs] seen]
                            (when-let [s (seq xs)]
                                (if (contains? seen f)
                                    (recur (rest s) seen)
                                    (cons f (step (rest s) (conj seen f)))
                                )
                            ))
                            xs seen
                        )
                    )
                )]
            (step coll #{})
        )
    )
)

;;;
 ; Given a map of replacement pairs and a vector/collection, returns
 ; a vector/seq with any elements = a key in smap replaced with the
 ; corresponding val in smap. Returns a transducer when no collection
 ; is provided.
 ;;
(§ defn replace
    ([smap]
        (map #(if-let [e (find smap %)] (val e) %))
    )
    ([smap coll]
        (if (vector? coll)
            (reduce1
                (fn [v i]
                    (if-let [e (find smap (nth v i))]
                        (assoc v i (val e))
                        v
                    )
                )
                coll (range (count coll))
            )
            (map #(if-let [e (find smap %)] (val e) %) coll)
        )
    )
)

;;;
 ; Sets the precision and rounding mode to be used for BigDecimal operations.
 ;
 ; Usage: (with-precision 10 (/ 1M 3))
 ; or:    (with-precision 10 :rounding HALF_DOWN (/ 1M 3))
 ;
 ; The rounding mode is one of CEILING, FLOOR, HALF_UP, HALF_DOWN,
 ; HALF_EVEN, UP, DOWN and UNNECESSARY; it defaults to HALF_UP.
 ;;
(§ defmacro with-precision [precision & exprs]
    (let [[body rm]
            (if (= (first exprs) :rounding)
                [(next (next exprs)) `((. java.math.RoundingMode ~(second exprs)))]
                [exprs nil]
            )]
        `(binding [*math-context* (java.math.MathContext. ~precision ~@rm)]
            ~@body
        )
    )
)

(§ defn ^:private mk-bound-fn [^cloiure.lang.Sorted sc test key]
    (fn [e] (test (.compare (.comparator sc) (.entryKey sc e) key) 0))
)

;;;
 ; sc must be a sorted collection, test(s) one of <, <=, > or >=.
 ; Returns a seq of those entries with keys ek for which
 ; (test (.. sc comparator (compare ek key)) 0) is true.
 ;;
(§ defn subseq
    ([^cloiure.lang.Sorted sc test key]
        (let [include (mk-bound-fn sc test key)]
            (if (#{> >=} test)
                (when-let [[e :as s] (.seqFrom sc key true)]
                    (if (include e) s (next s))
                )
                (take-while include (.seq sc true))
            )
        )
    )
    ([^cloiure.lang.Sorted sc start-test start-key end-test end-key]
        (when-let [[e :as s] (.seqFrom sc start-key true)]
            (take-while (mk-bound-fn sc end-test end-key)
                (if ((mk-bound-fn sc start-test start-key) e) s (next s))
            )
        )
    )
)

;;;
 ; sc must be a sorted collection, test(s) one of <, <=, > or >=.
 ; Returns a reverse seq of those entries with keys ek for which
 ; (test (.. sc comparator (compare ek key)) 0) is true.
 ;;
(§ defn rsubseq
    ([^cloiure.lang.Sorted sc test key]
        (let [include (mk-bound-fn sc test key)]
            (if (#{< <=} test)
                (when-let [[e :as s] (.seqFrom sc key false)]
                    (if (include e) s (next s))
                )
                (take-while include (.seq sc false))
            )
        )
    )
    ([^cloiure.lang.Sorted sc start-test start-key end-test end-key]
        (when-let [[e :as s] (.seqFrom sc end-key false)]
            (take-while (mk-bound-fn sc start-test start-key)
                (if ((mk-bound-fn sc end-test end-key) e) s (next s))
            )
        )
    )
)

;;;
 ; Takes a function of no args, presumably with side effects, and returns
 ; an infinite (or length n if supplied) lazy sequence of calls to it.
 ;;
(§ defn repeatedly
    ([f] (lazy-seq (cons (f) (repeatedly f))))
    ([n f] (take n (repeatedly f)))
)

;;;
 ; Returns the hash code of its argument. Note this is the hash code
 ; consistent with =, and thus is different than .hashCode for Integer,
 ; Short, Byte and Cloiure collections.
 ;;
(§ defn hash [x] (cloiure.lang.Util/hasheq x))

;;;
 ; Mix final collection hash for ordered or unordered collections.
 ; hash-basis is the combined collection hash, count is the number
 ; of elements included in the basis. Note this is the hash code
 ; consistent with =, different from .hashCode.
 ; See http://clojure.org/data_structures#hash for full algorithms.
 ;;
(§ defn ^long mix-collection-hash [^long hash-basis ^long count] (cloiure.lang.Murmur3/mixCollHash hash-basis count))

;;;
 ; Returns the hash code, consistent with =, for an external ordered
 ; collection implementing Iterable.
 ; See http://clojure.org/data_structures#hash for full algorithms.
 ;;
(§ defn ^long hash-ordered-coll [coll] (cloiure.lang.Murmur3/hashOrdered coll))

;;;
 ; Returns the hash code, consistent with =, for an external unordered
 ; collection implementing Iterable. For maps, the iterator should return
 ; map entries whose hash is computed as (hash-ordered-coll [k v]).
 ; See http://clojure.org/data_structures#hash for full algorithms.
 ;;
(§ defn ^long hash-unordered-coll [coll] (cloiure.lang.Murmur3/hashUnordered coll))

;;;
 ; Returns a lazy seq of the elements of coll separated by sep.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(§ defn interpose
    ([sep]
        (fn [rf]
            (let [started (volatile! false)]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input]
                        (if @started
                            (let [sepr (rf result sep)]
                                (if (reduced? sepr)
                                    sepr
                                    (rf sepr input)
                                )
                            )
                            (do (vreset! started true) (rf result input))
                        )
                    )
                )
            )
        )
    )
    ([sep coll]
        (drop 1 (interleave (repeat sep) coll)))
)

;;;
 ; Experimental - like defmacro, except defines a named function whose
 ; body is the expansion, calls to which may be expanded inline as if
 ; it were a macro. Cannot be used with variadic (&) args.
 ;;
(§ defmacro definline [name & decl]
    (let [[pre-args [args expr]] (split-with (comp not vector?) decl)]
        `(do
            (defn ~name ~@pre-args ~args ~(apply (eval (list `fn args expr)) args))
            (alter-meta! (var ~name) assoc :inline (fn ~name ~args ~expr))
            (var ~name)
        )
    )
)

;;;
 ; Returns an empty collection of the same category as coll, or nil.
 ;;
(§ defn empty [coll]
    (when (instance? cloiure.lang.IPersistentCollection coll)
        (.empty ^cloiure.lang.IPersistentCollection coll)
    )
)

;;;
 ; Maps an expression across an array a, using an index named idx, and
 ; return value named ret, initialized to a clone of a, then setting
 ; each element of ret to the evaluation of expr, returning the new
 ; array ret.
 ;;
(§ defmacro amap [a idx ret expr]
    `(let [a# ~a l# (alength a#) ~ret (aclone a#)]
        (loop [~idx 0]
            (if (< ~idx l#)
                (do (aset ~ret ~idx ~expr) (recur (unchecked-inc ~idx)))
                ~ret
            )
        )
    )
)

;;;
 ; Reduces an expression across an array a, using an index named idx,
 ; and return value named ret, initialized to init, setting ret to the
 ; evaluation of expr at each step, returning ret.
 ;;
(§ defmacro areduce [a idx ret init expr]
    `(let [a# ~a l# (alength a#)]
        (loop [~idx 0 ~ret ~init]
            (if (< ~idx l#)
                (recur (unchecked-inc-int ~idx) ~expr)
                ~ret
            )
        )
    )
)

;;;
 ; Creates an array of booleans.
 ;;
(§ defn boolean-array
    {:inline (fn [& args] `(cloiure.lang.Numbers/boolean_array ~@args)) :inline-arities #{1 2}}
    ([size-or-seq]          (cloiure.lang.Numbers/boolean_array size-or-seq))
    ([size init-val-or-seq] (cloiure.lang.Numbers/boolean_array size init-val-or-seq))
)

;;;
 ; Creates an array of bytes.
 ;;
(§ defn byte-array
    {:inline (fn [& args] `(cloiure.lang.Numbers/byte_array ~@args)) :inline-arities #{1 2}}
    ([size-or-seq]          (cloiure.lang.Numbers/byte_array size-or-seq))
    ([size init-val-or-seq] (cloiure.lang.Numbers/byte_array size init-val-or-seq))
)

;;;
 ; Creates an array of chars.
 ;;
(§ defn char-array
    {:inline (fn [& args] `(cloiure.lang.Numbers/char_array ~@args)) :inline-arities #{1 2}}
    ([size-or-seq]          (cloiure.lang.Numbers/char_array size-or-seq))
    ([size init-val-or-seq] (cloiure.lang.Numbers/char_array size init-val-or-seq))
)

;;;
 ; Creates an array of shorts.
 ;;
(§ defn short-array
    {:inline (fn [& args] `(cloiure.lang.Numbers/short_array ~@args)) :inline-arities #{1 2}}
    ([size-or-seq]          (cloiure.lang.Numbers/short_array size-or-seq))
    ([size init-val-or-seq] (cloiure.lang.Numbers/short_array size init-val-or-seq))
)

;;;
 ; Creates an array of ints.
 ;;
(§ defn int-array
    {:inline (fn [& args] `(cloiure.lang.Numbers/int_array ~@args)) :inline-arities #{1 2}}
    ([size-or-seq]          (cloiure.lang.Numbers/int_array size-or-seq))
    ([size init-val-or-seq] (cloiure.lang.Numbers/int_array size init-val-or-seq))
)

;;;
 ; Creates an array of longs.
 ;;
(§ defn long-array
    {:inline (fn [& args] `(cloiure.lang.Numbers/long_array ~@args)) :inline-arities #{1 2}}
    ([size-or-seq]          (cloiure.lang.Numbers/long_array size-or-seq))
    ([size init-val-or-seq] (cloiure.lang.Numbers/long_array size init-val-or-seq))
)

;;;
 ; Creates an array of floats.
 ;;
(§ defn float-array
    {:inline (fn [& args] `(cloiure.lang.Numbers/float_array ~@args)) :inline-arities #{1 2}}
    ([size-or-seq]          (cloiure.lang.Numbers/float_array size-or-seq))
    ([size init-val-or-seq] (cloiure.lang.Numbers/float_array size init-val-or-seq))
)

;;;
 ; Creates an array of doubles.
 ;;
(§ defn double-array
    {:inline (fn [& args] `(cloiure.lang.Numbers/double_array ~@args)) :inline-arities #{1 2}}
    ([size-or-seq]          (cloiure.lang.Numbers/double_array size-or-seq))
    ([size init-val-or-seq] (cloiure.lang.Numbers/double_array size init-val-or-seq))
)

;;;
 ; Creates an array of objects.
 ;;
(§ defn object-array
    {:inline (fn [arg] `(cloiure.lang.RT/object_array ~arg)) :inline-arities #{1}}
    ([size-or-seq] (cloiure.lang.RT/object_array size-or-seq))
)

(§ definline booleans [xs] `(cloiure.lang.Numbers/booleans ~xs))
(§ definline bytes    [xs] `(cloiure.lang.Numbers/bytes    ~xs))
(§ definline chars    [xs] `(cloiure.lang.Numbers/chars    ~xs))
(§ definline shorts   [xs] `(cloiure.lang.Numbers/shorts   ~xs))
(§ definline ints     [xs] `(cloiure.lang.Numbers/ints     ~xs))
(§ definline longs    [xs] `(cloiure.lang.Numbers/longs    ~xs))
(§ definline floats   [xs] `(cloiure.lang.Numbers/floats   ~xs))
(§ definline doubles  [xs] `(cloiure.lang.Numbers/doubles  ~xs))

;;;
 ; Return true if x is a byte array.
 ;;
(§ defn bytes? [x] (if (nil? x) false (= (.getComponentType (class x)) Byte/TYPE)))

;;;
 ; Returns true if x is an instance of Class.
 ;;
(§ defn class? [x] (instance? Class x))

;;;
 ; Atomically alters the root binding of var v by applying f to its current value plus any args.
 ;;
(§ defn alter-var-root [^cloiure.lang.Var v f & args] (.alterRoot v f args))

;;;
 ; Returns true if all of the vars provided as arguments have any bound value, root or thread-local.
 ; Implies that deref'ing the provided vars will succeed. Returns true if no vars are provided.
 ;;
(§ defn bound? [& vars] (every? #(.isBound ^cloiure.lang.Var %) vars))

;;;
 ; Returns true if all of the vars provided as arguments have thread-local bindings.
 ; Implies that set!'ing the provided vars will succeed. Returns true if no vars are provided.
 ;;
(§ defn thread-bound? [& vars] (every? #(.getThreadBinding ^cloiure.lang.Var %) vars))

;;;
 ; Creates a hierarchy object for use with derive, isa?, etc.
 ;;
(§ defn make-hierarchy [] {:parents {} :descendants {} :ancestors {}})

(§ def ^:private global-hierarchy (make-hierarchy))

;;;
 ; If coll is empty, returns nil, else coll.
 ;;
(§ defn not-empty [coll] (when (seq coll) coll))

;;;
 ; Returns the immediate superclass and direct interfaces of c, if any.
 ;;
(§ defn bases [^Class c]
    (when c
        (let [i (seq (.getInterfaces c)) s (.getSuperclass c)]
            (if s (cons s i) i)
        )
    )
)

;;;
 ; Returns the immediate and indirect superclasses and interfaces of c, if any.
 ;;
(§ defn supers [^Class class]
    (loop [ret (set (bases class)) cs ret]
        (if (seq cs)
            (let [c (first cs) bs (bases c)]
                (recur (into1 ret bs) (into1 (disj cs c) bs))
            )
            (not-empty ret)
        )
    )
)

;;;
 ; Returns true if (= child parent), or child is directly or indirectly derived
 ; from parent, either via a Java type inheritance relationship or a relationship
 ; established via derive. h must be a hierarchy obtained from make-hierarchy,
 ; if not supplied, defaults to the global hierarchy.
 ;;
(§ defn isa?
    ([child parent] (isa? global-hierarchy child parent))
    ([h child parent]
        (or (= child parent)
            (and (class? parent) (class? child) (.isAssignableFrom ^Class parent child))
            (contains? ((:ancestors h) child) parent)
            (and (class? child) (some #(contains? ((:ancestors h) %) parent) (supers child)))
            (and (vector? parent) (vector? child) (= (count parent) (count child))
                (loop [ret true i 0]
                    (if (or (not ret) (= i (count parent)))
                        ret
                        (recur (isa? h (child i) (parent i)) (inc i))
                    )
                )
            )
        )
    )
)

;;;
 ; Returns the immediate parents of tag, either via a Java type inheritance
 ; relationship or a relationship established via derive. h must be a hierarchy
 ; obtained from make-hierarchy, if not supplied, defaults to the global hierarchy.
 ;;
(§ defn parents
    ([tag] (parents global-hierarchy tag))
    ([h tag]
        (not-empty
            (let [tp (get (:parents h) tag)]
                (if (class? tag)
                    (into1 (set (bases tag)) tp)
                    tp
                )
            )
        )
    )
)

;;;
 ; Returns the immediate and indirect parents of tag, either via a Java type
 ; inheritance relationship or a relationship established via derive. h must
 ; be a hierarchy obtained from make-hierarchy, if not supplied, defaults to
 ; the global hierarchy.
 ;;
(§ defn ancestors
    ([tag] (ancestors global-hierarchy tag))
    ([h tag]
        (not-empty
            (let [ta (get (:ancestors h) tag)]
                (if (class? tag)
                    (let [superclasses (set (supers tag))]
                        (reduce1 into1 superclasses (cons ta (map #(get (:ancestors h) %) superclasses)))
                    )
                    ta
                )
            )
        )
    )
)

;;;
 ; Returns the immediate and indirect children of tag, through a relationship
 ; established via derive. h must be a hierarchy obtained from make-hierarchy,
 ; if not supplied, defaults to the global hierarchy.
 ; Note: does not work on Java type inheritance relationships.
 ;;
(§ defn descendants
    ([tag] (descendants global-hierarchy tag))
    ([h tag]
        (if (class? tag)
            (throw (UnsupportedOperationException. "Can't get descendants of classes"))
            (not-empty (get (:descendants h) tag))
        )
    )
)

;;;
 ; Establishes a parent/child relationship between parent and tag.
 ; Parent must be a namespace-qualified symbol or keyword and child
 ; can be either a namespace-qualified symbol or keyword or a class.
 ; h must be a hierarchy obtained from make-hierarchy, if not
 ; supplied, defaults to, and modifies, the global hierarchy.
 ;;
(§ defn derive
    ([tag parent]
        (assert (namespace parent))
        (assert (or (class? tag) (and (instance? cloiure.lang.Named tag) (namespace tag))))

        (alter-var-root #'global-hierarchy derive tag parent)
        nil
    )
    ([h tag parent]
        (assert (not= tag parent))
        (assert (or (class? tag) (instance? cloiure.lang.Named tag)))
        (assert (instance? cloiure.lang.Named parent))

        (let [tp (:parents h) td (:descendants h) ta (:ancestors h)
              tf
                (fn [m source sources target targets]
                    (reduce1
                        (fn [ret k]
                            (assoc ret k (reduce1 conj (get targets k #{}) (cons target (targets target))))
                        )
                        m (cons source (sources source))
                    )
                )]
            (or
                (when-not (contains? (tp tag) parent)
                    (when (contains? (ta tag) parent)
                        (throw (Exception. (print-str tag "already has" parent "as ancestor")))
                    )
                    (when (contains? (ta parent) tag)
                        (throw (Exception. (print-str "Cyclic derivation:" parent "has" tag "as ancestor")))
                    )
                    {
                        :parents (assoc (:parents h) tag (conj (get tp tag #{}) parent))
                        :ancestors (tf (:ancestors h) tag td parent ta)
                        :descendants (tf (:descendants h) parent ta tag td)
                    }
                )
                h
            )
        )
    )
)

(§ declare flatten)

;;;
 ; Removes a parent/child relationship between parent and tag.
 ; h must be a hierarchy obtained from make-hierarchy, if not
 ; supplied, defaults to, and modifies, the global hierarchy.
 ;;
(§ defn underive
    ([tag parent]
        (alter-var-root #'global-hierarchy underive tag parent)
        nil
    )
    ([h tag parent]
        (let [parentMap     (:parents h)
              childsParents (if (parentMap tag) (disj (parentMap tag) parent) #{})
              newParents    (if (not-empty childsParents) (assoc parentMap tag childsParents) (dissoc parentMap tag))
              deriv-seq     (flatten (map #(cons (key %) (interpose (key %) (val %))) (seq newParents)))]
            (if (contains? (parentMap tag) parent)
                (reduce1 #(apply derive %1 %2) (make-hierarchy) (partition 2 deriv-seq))
                h
            )
        )
    )
)

;;;
 ; Returns true if no two of the arguments are =.
 ;;
(§ defn ^Boolean distinct?
    ([x] true)
    ([x y] (not (= x y)))
    ([x y & more]
        (if (not= x y)
            (loop [s #{x y} [x & etc :as xs] more]
                (if xs
                    (if (contains? s x)
                        false
                        (recur (conj s x) etc)
                    )
                    true
                )
            )
            false
        )
    )
)

;;;
 ; Returns a seq on a java.util.Iterator. Note that most collections
 ; providing iterators implement Iterable and thus support seq directly.
 ; Seqs cache values, thus iterator-seq should not be used on any
 ; iterator that repeatedly returns the same mutable object.
 ;;
(§ defn iterator-seq [iter] (cloiure.lang.RT/chunkIteratorSeq iter))

;;;
 ; Formats a string using String/format.
 ; See java.util.Formatter for format string syntax.
 ;;
(§ defn ^String format [fmt & args] (String/format fmt (to-array args)))

;;;
 ; Prints formatted output, as per format.
 ;;
(§ defn printf [fmt & args] (print (apply format fmt args)))

(§ defmacro with-loading-context [& body]
    `((fn loading# []
        (cloiure.lang.Var/pushThreadBindings {cloiure.lang.Compiler/LOADER (.getClassLoader (.getClass ^Object loading#))})
        (try
            ~@body
            (finally
                (cloiure.lang.Var/popThreadBindings)
            )
        )
    ))
)

;;;
 ; Sets *ns* to the namespace named by name (unevaluated), creating it if needed.
 ;
 ; references can be zero or more of:
 ; (:refer-cloiure ...) (:require ...) (:use ...) (:import ...) (:load ...)
 ; with the syntax of refer-cloiure/require/use/import/load respectively,
 ; except the arguments are unevaluated and need not be quoted.
 ;
 ; If :refer-cloiure is not used, a default (refer 'cloiure.core) is used.
 ; Use of ns is preferred to individual calls to in-ns/require/use/import:
 ;
 ; (ns foo.bar
 ;   (:refer-cloiure :exclude [ancestors printf])
 ;   (:require (cloiure.contrib sql combinatorics))
 ;   (:use (my.lib this that))
 ;   (:import (java.util Date Timer Random)
 ;            (java.sql Connection Statement)))
 ;;
(§ defmacro ns [name & references]
    (let [process-reference (fn [[kname & args]] `(~(symbol "cloiure.core" (cloiure.core/name kname)) ~@(map #(list 'quote %) args)))
          docstring         (when (string? (first references)) (first references))
          references        (if docstring (next references) references)
          name              (if docstring (vary-meta name assoc :doc docstring) name)
          metadata          (when (map? (first references)) (first references))
          references        (if metadata (next references) references)
          name              (if metadata (vary-meta name merge metadata) name)
          ;; ns-effect (cloiure.core/in-ns name)
          name-metadata (meta name)]
        `(do
            (cloiure.core/in-ns '~name)
            ~@(when name-metadata
                `((.resetMeta (cloiure.lang.Namespace/find '~name) ~name-metadata))
            )
            (with-loading-context
                ~@(when (and (not= name 'cloiure.core) (not-any? #(= :refer-cloiure (first %)) references))
                    `((cloiure.core/refer '~'cloiure.core))
                )
                ~@(map process-reference references)
            )
            nil
        )
    )
)

;;;
 ; Same as (refer 'cloiure.core <filters>).
 ;;
(§ defmacro refer-cloiure [& filters]
    `(cloiure.core/refer '~'cloiure.core ~@filters)
)

;;;
 ; defs name to have the root value of the expr iff the named var has no root value,
 ; else expr is unevaluated.
 ;;
(§ defmacro defonce [name expr]
    `(let [v# (def ~name)]
        (when-not (.hasRoot v#)
            (def ~name ~expr)
        )
    )
)

;;;
 ; A stack of paths currently being loaded by this thread.
 ;;
(§ defonce ^:dynamic ^:private *pending-paths* ())

;;;
 ; True while a verbose load is pending.
 ;;
(§ defonce ^:dynamic ^:private *loading-verbosely* false)

;;;
 ; Throws a CompilerException with a message if pred is true.
 ;;
(§ defn- throw-if [pred fmt & args]
    (when pred
        (let [^String message (apply format fmt args)
              exception (Exception. message)
              raw-trace (.getStackTrace exception)
              boring? #(not= (.getMethodName ^StackTraceElement %) "doInvoke")
              trace (into-array StackTraceElement (drop 2 (drop-while boring? raw-trace)))]
            (.setStackTrace exception trace)
            (throw (cloiure.lang.Compiler$CompilerException.
                (.deref cloiure.lang.Compiler/LINE)
                (.deref cloiure.lang.Compiler/COLUMN)
                exception
            ))
        )
    )
)

;;;
 ; Returns true if x is a libspec.
 ;;
(§ defn- libspec? [x]
    (or (symbol? x) (and (vector? x) (or (nil? (second x)) (keyword? (second x)))))
)

;;;
 ; Prepends a symbol or a seq to coll.
 ;;
(§ defn- prependss [x coll]
    (if (symbol? x) (cons x coll) (concat x coll))
)

;;;
 ; Returns the root directory path for a lib.
 ;;
(§ defn- ^String root-resource [lib]
    (str \/ (.. (name lib) (replace \- \_) (replace \. \/)))
)

;;;
 ; Returns the root resource path for a lib.
 ;;
(§ defn- root-directory [lib]
    (let [d (root-resource lib)]
        (subs d 0 (.lastIndexOf d "/"))
    )
)

(§ def ^:declared ^:redef load)

;;;
 ; Loads a lib given its name. If need-ns, ensures that the associated
 ; namespace exists after loading. If require, records the load so any
 ; duplicate loads can be skipped.
 ;;
(§ defn- load-one [lib need-ns require]
    (load (root-resource lib))
    (throw-if (and need-ns (not (find-ns lib))) "namespace '%s' not found after loading '%s'" lib (root-resource lib))
)

;;;
 ; Loads a lib with options.
 ;;
(§ defn- load-lib [prefix lib & options]
    (throw-if (and prefix (pos? (.indexOf (name lib) (int \.))))
        "Found lib name '%s' containing period with prefix '%s'.  lib names inside prefix lists must not contain periods"
        (name lib) prefix
    )
    (let [lib (if prefix (symbol (str prefix \. lib)) lib)
          opts (apply hash-map options)
          {:keys [as reload require use verbose]} opts
          need-ns (or as use)
          filter-opts (select-keys opts '(:exclude :only :rename :refer))
          undefined-on-entry (not (find-ns lib))]
        (binding [*loading-verbosely* (or *loading-verbosely* verbose)]
            (try
                (load-one lib need-ns require)
                (catch Exception e
                    (when undefined-on-entry
                        (remove-ns lib)
                    )
                    (throw e)
                )
            )
            (when (and need-ns *loading-verbosely*)
                (printf "(cloiure.core/in-ns '%s)\n" (ns-name *ns*))
            )
            (when as
                (when *loading-verbosely*
                    (printf "(cloiure.core/alias '%s '%s)\n" as lib)
                )
                (alias as lib)
            )
            (when (or use (:refer filter-opts))
                (when *loading-verbosely*
                    (printf "(cloiure.core/refer '%s" lib)
                    (doseq [opt filter-opts]
                        (printf " %s '%s" (key opt) (print-str (val opt)))
                    )
                    (printf ")\n")
                )
                (apply refer lib (mapcat seq filter-opts))
            )
        )
    )
)

;;;
 ; Loads libs, interpreting libspecs, prefix lists, and flags for forwarding to load-lib.
 ;;
(§ defn- load-libs [& args]
    (let [flags (filter keyword? args)
          opts (interleave flags (repeat true))
          args (filter (complement keyword?) args)]
        ;; check for unsupported options
        (let [supported #{:as :reload :require :use :verbose :refer} unsupported (seq (remove supported flags))]
            (throw-if unsupported (apply str "Unsupported option(s) supplied: " (interpose \, unsupported)))
        )
        ;; check a load target was specified
        (throw-if (not (seq args)) "Nothing specified to load")
        (doseq [arg args]
            (if (libspec? arg)
                (apply load-lib nil (prependss arg opts))
                (let [[prefix & args] arg]
                    (throw-if (nil? prefix) "prefix cannot be nil")
                    (doseq [arg args]
                        (apply load-lib prefix (prependss arg opts))
                    )
                )
            )
        )
    )
)

;;;
 ; Detects and rejects non-trivial cyclic load dependencies. The exception
 ; message shows the dependency chain with the cycle highlighted. Ignores
 ; the trivial case of a file attempting to load itself.
 ;;
(§ defn- check-cyclic-dependency [path]
    (when (some #{path} (rest *pending-paths*))
        (let [pending (map #(if (= % path) (str "[ " % " ]") %) (cons path *pending-paths*))
              chain (apply str (interpose "->" pending))]
            (throw-if true "Cyclic load dependency: %s" chain)
        )
    )
)

;;;
 ; Loads libs, skipping any that are already loaded. Each argument is either
 ; a libspec that identifies a lib, a prefix list that identifies multiple libs
 ; whose names share a common prefix, or a flag that modifies how all the identified
 ; libs are loaded. Use :require in the ns macro in preference to calling this directly.
 ;
 ; Libs
 ;
 ; A 'lib' is a named set of resources in classpath whose contents define a library of
 ; Cloiure code. Lib names are symbols and each lib is associated with a Cloiure namespace
 ; and a Java package that share its name. A lib's name also locates its root directory
 ; within classpath using Java's package name to classpath-relative path mapping. All
 ; resources in a lib should be contained in the directory structure under its root
 ; directory. All definitions a lib makes should be in its associated namespace.
 ;
 ; 'require loads a lib by loading its root resource. The root resource path is derived
 ; from the lib name in the following manner:
 ;
 ; Consider a lib named by the symbol 'x.y.z; it has the root directory <classpath>/x/y/,
 ; and its root resource is <classpath>/x/y/z.cli. The root resource should contain code
 ; to create the lib's namespace (usually by using the ns macro) and load any additional
 ; lib resources.
 ;
 ; Libspecs
 ;
 ; A libspec is a lib name or a vector containing a lib name followed by options expressed
 ; as sequential keywords and arguments.
 ;
 ; Recognized options:
 ;
 ; :as takes a symbol as its argument and makes that symbol an alias to the lib's namespace in the current namespace.
 ; :refer takes a list of symbols to refer from the namespace or the :all keyword to bring in all public vars.
 ;
 ; Prefix Lists
 ;
 ; It's common for Cloiure code to depend on several libs whose names have the same prefix.
 ; When specifying libs, prefix lists can be used to reduce repetition. A prefix list contains
 ; the shared prefix followed by libspecs with the shared prefix removed from the lib names.
 ; After removing the prefix, the names that remain must not contain any periods.
 ;
 ; Flags
 ;
 ; A flag is a keyword. Recognized flags:
 ;
 ; :reload forces loading of all the identified libs even if they are already loaded.
 ; :verbose triggers printing information about each load, alias, and refer.
 ;
 ; Example:
 ;
 ; The following would load the libraries cloiure.zip and cloiure.set abbreviated as 's'.
 ;
 ; (require '(cloiure zip [set :as s]))
 ;;
(§ defn require [& args] (apply load-libs :require args))

;;;
 ; Like 'require, but also refers to each lib's namespace using cloiure.core/refer.
 ; Use :use in the ns macro in preference to calling this directly.
 ;
 ; 'use accepts additional options in libspecs: :exclude, :only, :rename.
 ; The arguments and semantics for :exclude, :only, and :rename are the same
 ; as those documented for cloiure.core/refer.
 ;;
(§ defn use [& args] (apply load-libs :require :use args))

;;;
 ; Loads Cloiure code from resources in classpath. A path is interpreted as
 ; classpath-relative if it begins with a slash or relative to the root
 ; directory for the current namespace otherwise.
 ;;
(§ defn load {:redef true} [& paths]
    (doseq [^String path paths]
        (let [^String path (if (.startsWith path "/") path (str (root-directory (ns-name *ns*)) \/ path))]
            (when *loading-verbosely*
                (printf "(cloiure.core/load \"%s\")\n" path)
                (flush)
            )
            (check-cyclic-dependency path)
            (when-not (= path (first *pending-paths*))
                (binding [*pending-paths* (conj *pending-paths* path)]
                    (cloiure.lang.RT/load (.substring path 1))
                )
            )
        )
    )
)

;;;
 ; Returns the value in a nested associative structure,
 ; where ks is a sequence of keys. Returns nil if the key
 ; is not present, or the not-found value if supplied.
 ;;
(§ defn get-in
    ([m ks] (reduce1 get m ks))
    ([m ks not-found]
        (loop [sentinel (Object.) m m ks (seq ks)]
            (if ks
                (let [m (get m (first ks) sentinel)]
                    (if (identical? sentinel m)
                        not-found
                        (recur sentinel m (next ks))
                    )
                )
                m
            )
        )
    )
)

;;;
 ; Associates a value in a nested associative structure, where ks is
 ; a sequence of keys and v is the new value and returns a new nested
 ; structure. If any levels do not exist, hash-maps will be created.
 ;;
(§ defn assoc-in [m [k & ks] v]
    (if ks
        (assoc m k (assoc-in (get m k) ks v))
        (assoc m k v)
    )
)

;;;
 ; 'Updates' a value in a nested associative structure, where ks is
 ; a sequence of keys and f is a function that will take the old value
 ; and any supplied args and return the new value, and returns a new
 ; nested structure. If any levels do not exist, hash-maps will be
 ; created.
 ;;
(§ defn update-in [m ks f & args]
    (let [up
            (fn up [m ks f args]
                (let [[k & ks] ks]
                    (if ks
                        (assoc m k (up (get m k) ks f args))
                        (assoc m k (apply f (get m k) args))
                    )
                )
            )]
        (up m ks f args)
    )
)

;;;
 ; 'Updates' a value in an associative structure, where k is a key and f is a function
 ; that will take the old value and any supplied args and return the new value, and
 ; returns a new structure. If the key does not exist, nil is passed as the old value.
 ;;
(§ defn update
    ([m k f] (assoc m k (f (get m k))))
    ([m k f x] (assoc m k (f (get m k) x)))
    ([m k f x y] (assoc m k (f (get m k) x y)))
    ([m k f x y z] (assoc m k (f (get m k) x y z)))
    ([m k f x y z & more] (assoc m k (apply f (get m k) x y z more)))
)

;;;
 ; Returns true if coll has no items - same as (not (seq coll)).
 ; Please use the idiom (seq x) rather than (not (empty? x)).
 ;;
(§ defn empty? [coll] (not (seq coll)))

;;;
 ; Returns true if x implements IPersistentCollection.
 ;;
(§ defn coll? [x] (instance? cloiure.lang.IPersistentCollection x))

;;;
 ; Returns true if x implements IPersistentList.
 ;;
(§ defn list? [x] (instance? cloiure.lang.IPersistentList x))

;;;
 ; Return true if the seq function is supported for x.
 ;;
(§ defn seqable? [x] (cloiure.lang.RT/canSeq x))

;;;
 ; Returns true if x implements IFn.
 ; Note that many data structures (e.g. sets and maps) implement IFn.
 ;;
(§ defn ifn? [x] (instance? cloiure.lang.IFn x))

;;;
 ; Returns true if x implements Fn, i.e. is an object created via fn.
 ;;
(§ defn fn? [x] (instance? cloiure.lang.Fn x))

;;;
 ; Returns true if coll implements Associative.
 ;;
(§ defn associative? [coll] (instance? cloiure.lang.Associative coll))

;;;
 ; Returns true if coll implements Sequential.
 ;;
(§ defn sequential? [coll] (instance? cloiure.lang.Sequential coll))

;;;
 ; Returns true if coll implements Sorted.
 ;;
(§ defn sorted? [coll] (instance? cloiure.lang.Sorted coll))

;;;
 ; Returns true if coll implements count in constant time.
 ;;
(§ defn counted? [coll] (instance? cloiure.lang.Counted coll))

;;;
 ; Returns true if coll implements Reversible.
 ;;
(§ defn reversible? [coll] (instance? cloiure.lang.Reversible coll))

;;;
 ; Return true if coll implements Indexed, indicating efficient lookup by index.
 ;;
(§ defn indexed? [coll] (instance? cloiure.lang.Indexed coll))

;;;
 ; Bound in a repl thread to the most recent value printed.
 ;;
(§ def ^:dynamic *1)

;;;
 ; Bound in a repl thread to the second most recent value printed.
 ;;
(§ def ^:dynamic *2)

;;;
 ; Bound in a repl thread to the third most recent value printed.
 ;;
(§ def ^:dynamic *3)

;;;
 ; Bound in a repl thread to the most recent exception caught by the repl.
 ;;
(§ def ^:dynamic *e)

;;;
 ; trampoline can be used to convert algorithms requiring mutual recursion without
 ; stack consumption. Calls f with supplied args, if any. If f returns a fn, calls
 ; that fn with no arguments, and continues to repeat, until the return value is
 ; not a fn, then returns that non-fn value. Note that if you want to return a fn
 ; as a final value, you must wrap it in some data structure and unpack it after
 ; trampoline returns.
 ;;
(§ defn trampoline
    ([f]
        (let [ret (f)]
            (if (fn? ret)
                (recur ret)
                ret
            )
        )
    )
    ([f & args] (trampoline #(apply f args)))
)

;;;
 ; Finds or creates a var named by the symbol name in the namespace
 ; ns (which can be a symbol or a namespace), setting its root binding
 ; to val if supplied. The namespace must exist. The var will adopt
 ; any metadata from the name symbol. Returns the var.
 ;;
(§ defn intern
    ([ns ^cloiure.lang.Symbol name]
        (let [v (cloiure.lang.Var/intern (the-ns ns) name)]
            (when (meta name)
                (.setMeta v (meta name))
            )
            v
        )
    )
    ([ns name val]
        (let [v (cloiure.lang.Var/intern (the-ns ns) name val)]
            (when (meta name)
                (.setMeta v (meta name))
            )
            v
        )
    )
)

;;;
 ; Repeatedly executes body while test expression is true. Presumes
 ; some side-effect will cause test to become false/nil. Returns nil.
 ;;
(§ defmacro while [test & body]
    `(loop [] (when ~test ~@body (recur)))
)

;;;
 ; Returns a memoized version of a referentially transparent function.
 ; The memoized version of the function keeps a cache of the mapping from
 ; arguments to results and, when calls with the same arguments are repeated
 ; often, has higher performance at the expense of higher memory use.
 ;;
(§ defn memoize [f]
    (let [mem (atom {})]
        (fn [& args]
            (if-let [e (find @mem args)]
                (val e)
                (let [ret (apply f args)]
                    (swap! mem assoc args ret)
                    ret
                )
            )
        )
    )
)

;;;
 ; Takes a binary predicate, an expression, and a set of clauses.
 ; Each clause can take the form of either:
 ;
 ; test-expr result-expr
 ;
 ; test-expr :>> result-fn
 ;
 ; Note :>> is an ordinary keyword.
 ;
 ; For each clause, (pred test-expr expr) is evaluated. If it returns logical true,
 ; the clause is a match. If a binary clause matches, the result-expr is returned,
 ; if a ternary clause matches, its result-fn, which must be a unary function, is
 ; called with the result of the predicate as its argument, the result of that call
 ; being the return value of condp. A single default expression can follow the clauses,
 ; and its value will be returned if no clause matches. If no default expression
 ; is provided and no clause matches, an IllegalArgumentException is thrown.
 ;;
(§ defmacro condp [pred expr & clauses]
    (let [gpred (gensym "pred__") gexpr (gensym "expr__")
          emit
            (fn emit [pred expr args]
                (let [[[a b c :as clause] more] (split-at (if (= :>> (second args)) 3 2) args) n (count clause)]
                    (cond
                        (= 0 n) `(throw (IllegalArgumentException. (str "No matching clause: " ~expr)))
                        (= 1 n) a
                        (= 2 n) `(if (~pred ~a ~expr)
                                    ~b
                                    ~(emit pred expr more)
                                )
                        :else   `(if-let [p# (~pred ~a ~expr)]
                                    (~c p#)
                                    ~(emit pred expr more)
                                )
                    )
                )
            )]
        `(let [~gpred ~pred ~gexpr ~expr]
            ~(emit gpred gexpr clauses)
        )
    )
)

(§ defmacro ^:private add-doc-and-meta [name docstring meta]
    `(alter-meta! (var ~name) merge (assoc ~meta :doc ~docstring))
)

(§ add-doc-and-meta *warn-on-reflection*
    "When set to true, the compiler will emit warnings when reflection
    is needed to resolve Java method calls or field accesses.
    Defaults to false."
)

(§ add-doc-and-meta *ns*
    "A cloiure.lang.Namespace object representing the current namespace."
)

(§ add-doc-and-meta *in*
    "A java.io.Reader object representing standard input for read operations.
    Defaults to System/in, wrapped in a LineNumberingPushbackReader."
)

(§ add-doc-and-meta *out*
    "A java.io.Writer object representing standard output for print operations.
    Defaults to System/out, wrapped in an OutputStreamWriter."
)

(§ add-doc-and-meta *err*
    "A java.io.Writer object representing standard error for print operations.
    Defaults to System/err, wrapped in a PrintWriter."
)

(§ add-doc-and-meta *flush-on-newline*
    "When set to true, output will be flushed whenever a newline is printed.
    Defaults to true."
)

(§ add-doc-and-meta *print-readably*
    "When set to logical false, strings and characters will be printed with
    non-alphanumeric characters converted to the appropriate escape sequences.
    Defaults to true."
)

;;;
 ; fnspec => (fname [params*] exprs) or (fname ([params*] exprs)+)
 ;
 ; Takes a vector of function specs and a body, and generates a set of
 ; bindings of functions to their names. All of the names are available
 ; in all of the definitions of the functions, as well as the body.
 ;;
(§ defmacro letfn {:special-form true, :forms '[(letfn [fnspecs*] exprs*)]} [fnspecs & body]
    `(letfn* ~(vec (interleave (map first fnspecs) (map #(cons `fn %) fnspecs))) ~@body)
)

;;;
 ; Takes a function f, and returns a function that calls f, replacing a nil first argument
 ; to f with the supplied value x. Higher arity versions can replace arguments in the second
 ; and third positions (y, z). Note that the function f can take any number of arguments,
 ; not just the one(s) being nil-patched.
 ;;
(§ defn fnil
    ([f x]
        (fn
            ([a] (f (if (nil? a) x a)))
            ([a b] (f (if (nil? a) x a) b))
            ([a b c] (f (if (nil? a) x a) b c))
            ([a b c & ds] (apply f (if (nil? a) x a) b c ds))
        )
    )
    ([f x y]
        (fn
            ([a b] (f (if (nil? a) x a) (if (nil? b) y b)))
            ([a b c] (f (if (nil? a) x a) (if (nil? b) y b) c))
            ([a b c & ds] (apply f (if (nil? a) x a) (if (nil? b) y b) c ds))
        )
    )
    ([f x y z]
        (fn
            ([a b] (f (if (nil? a) x a) (if (nil? b) y b)))
            ([a b c] (f (if (nil? a) x a) (if (nil? b) y b) (if (nil? c) z c)))
            ([a b c & ds] (apply f (if (nil? a) x a) (if (nil? b) y b) (if (nil? c) z c) ds))
        )
    )
)

(§ defn- shift-mask [shift mask x]
    (-> x (bit-shift-right shift) (bit-and mask))
)

(§ def ^:private max-mask-bits 13)
(§ def ^:private max-switch-table-size (bit-shift-left 1 max-mask-bits))

;;;
 ; Takes a collection of hashes and returns [shift mask] or nil if none found.
 ;;
(§ defn- maybe-min-hash [hashes]
    (first
        (filter (fn [[s m]] (apply distinct? (map #(shift-mask s m %) hashes)))
            (for [mask (map #(dec (bit-shift-left 1 %)) (range 1 (inc max-mask-bits))) shift (range 0 31)]
                [shift mask]
            )
        )
    )
)

;;;
 ; Transforms a sequence of test constants and a corresponding sequence of then
 ; expressions into a sorted map to be consumed by case*. The form of the map
 ; entries are {(case-f test) [(test-f test) then]}.
 ;;
(§ defn- case-map [case-f test-f tests thens]
    (into1 (sorted-map)
        (zipmap
            (map case-f tests)
            (map vector (map test-f tests) thens)
        )
    )
)

;;;
 ; Returns true if the collection of ints can fit within the max-table-switch-size,
 ; false otherwise.
 ;;
(§ defn- fits-table? [ints]
    (< (- (apply max (seq ints)) (apply min (seq ints))) max-switch-table-size)
)

;;;
 ; Takes a sequence of int-sized test constants and a corresponding sequence of
 ; then expressions. Returns a tuple of [shift mask case-map switch-type] where
 ; case-map is a map of int case values to [test then] tuples, and switch-type
 ; is either :sparse or :compact.
 ;;
(§ defn- prep-ints [tests thens]
    (if (fits-table? tests)
        ;; compact case ints, no shift-mask
        [0 0 (case-map int int tests thens) :compact]
        (let [[shift mask] (or (maybe-min-hash (map int tests)) [0 0])]
            (if (zero? mask)
                ;; sparse case ints, no shift-mask
                [0 0 (case-map int int tests thens) :sparse]
                ;; compact case ints, with shift-mask
                [shift mask (case-map #(shift-mask shift mask (int %)) int tests thens) :compact]
            )
        )
    )
)

;;;
 ; Takes a case expression, default expression, and a sequence of test constants
 ; and a corresponding sequence of then expressions. Returns a tuple of
 ; [tests thens skip-check-set] where no tests have the same hash. Each set of
 ; input test constants with the same hash is replaced with a single test
 ; constant (the case int), and their respective thens are combined into:
 ;
 ; (condp = expr test-1 then-1 ... test-n then-n default).
 ;
 ; The skip-check is a set of case ints for which post-switch equivalence
 ; checking must not be done (the cases holding the above condp thens).
 ;;
(§ defn- merge-hash-collisions [expr-sym default tests thens]
    (let [buckets
            (loop [m {} ks tests vs thens]
                (if (and ks vs)
                    (recur (update m (cloiure.lang.Util/hash (first ks)) (fnil conj []) [(first ks) (first vs)]) (next ks) (next vs))
                    m
                )
            )
          assoc-multi
            (fn [m h bucket]
                (let [testexprs (apply concat bucket) expr `(condp = ~expr-sym ~@testexprs ~default)]
                    (assoc m h expr)
                )
            )
          hmap
            (reduce1
                (fn [m [h bucket]]
                    (if (== 1 (count bucket))
                        (assoc m (ffirst bucket) (second (first bucket)))
                        (assoc-multi m h bucket)
                    )
                )
                {} buckets
            )
          skip-check
            (->> buckets
                (filter #(< 1 (count (second %))))
                (map first)
                (into1 #{})
            )]
        [(keys hmap) (vals hmap) skip-check]
    )
)

;;;
 ; Takes a sequence of test constants and a corresponding sequence of then
 ; expressions. Returns a tuple of [shift mask case-map switch-type skip-check]
 ; where case-map is a map of int case values to [test then] tuples, switch-type
 ; is either :sparse or :compact, and skip-check is a set of case ints for which
 ; post-switch equivalence checking must not be done (occurs with hash collisions).
 ;;
(§ defn- prep-hashes [expr-sym default tests thens]
    (let [hashcode #(cloiure.lang.Util/hash %) hashes (into1 #{} (map hashcode tests))]
        (if (== (count tests) (count hashes))
            (if (fits-table? hashes)
                ;; compact case ints, no shift-mask
                [0 0 (case-map hashcode identity tests thens) :compact]
                (let [[shift mask] (or (maybe-min-hash hashes) [0 0])]
                    (if (zero? mask)
                        ;; sparse case ints, no shift-mask
                        [0 0 (case-map hashcode identity tests thens) :sparse]
                        ;; compact case ints, with shift-mask
                        [shift mask (case-map #(shift-mask shift mask (hashcode %)) identity tests thens) :compact]
                    )
                )
            )
            ;; resolve hash collisions and try again
            (let [[tests thens skip-check] (merge-hash-collisions expr-sym default tests thens)
                  [shift mask case-map switch-type] (prep-hashes expr-sym default tests thens)
                  skip-check
                    (if (zero? mask)
                        skip-check
                        (into1 #{} (map #(shift-mask shift mask %) skip-check))
                    )]
                [shift mask case-map switch-type skip-check]
            )
        )
    )
)

;;;
 ; Takes an expression, and a set of clauses.
 ;
 ; Each clause can take the form of either:
 ;
 ; test-constant result-expr
 ;
 ; (test-constant1 ... test-constantN) result-expr
 ;
 ; The test-constants are not evaluated. They must be compile-time
 ; literals, and need not be quoted. If the expression is equal to a
 ; test-constant, the corresponding result-expr is returned. A single
 ; default expression can follow the clauses, and its value will be
 ; returned if no clause matches. If no default expression is provided
 ; and no clause matches, an IllegalArgumentException is thrown.
 ;
 ; Unlike cond and condp, case does a constant-time dispatch, the
 ; clauses are not considered sequentially. All manner of constant
 ; expressions are acceptable in case, including numbers, strings,
 ; symbols, keywords, and (Cloiure) composites thereof. Note that since
 ; lists are used to group multiple constants that map to the same
 ; expression, a vector can be used to match a list if needed. The
 ; test-constants need not be all of the same type.
 ;;
(§ defmacro case [e & clauses]
    (let [ge (with-meta (gensym) {:tag Object})
          default
            (if (odd? (count clauses))
                (last clauses)
                `(throw (IllegalArgumentException. (str "No matching clause: " ~ge)))
            )]
        (if (> 2 (count clauses))
            `(let [~ge ~e] ~default)
            (let [pairs (partition 2 clauses)
                  assoc-test
                    (fn assoc-test [m test expr]
                        (if (contains? m test)
                            (throw (IllegalArgumentException. (str "Duplicate case test constant: " test)))
                            (assoc m test expr)
                        )
                    )
                  pairs
                    (reduce1
                        (fn [m [test expr]]
                            (if (seq? test)
                                (reduce1 #(assoc-test %1 %2 expr) m test)
                                (assoc-test m test expr)
                            )
                        )
                        {} pairs
                    )
                  tests (keys pairs)
                  thens (vals pairs)
                  mode
                    (cond
                        (every? #(and (integer? %) (<= Integer/MIN_VALUE % Integer/MAX_VALUE)) tests) :ints
                        (every? keyword? tests) :identity
                        :else :hashes
                    )]
                (condp = mode
                    :ints
                        (let [[shift mask imap switch-type] (prep-ints tests thens)]
                            `(let [~ge ~e] (case* ~ge ~shift ~mask ~default ~imap ~switch-type :int))
                        )
                    :hashes
                        (let [[shift mask imap switch-type skip-check] (prep-hashes ge default tests thens)]
                            `(let [~ge ~e] (case* ~ge ~shift ~mask ~default ~imap ~switch-type :hash-equiv ~skip-check))
                        )
                    :identity
                        (let [[shift mask imap switch-type skip-check] (prep-hashes ge default tests thens)]
                            `(let [~ge ~e] (case* ~ge ~shift ~mask ~default ~imap ~switch-type :hash-identity ~skip-check))
                        )
                )
            )
        )
    )
)

(§ in-ns 'cloiure.core)

(§ import
    [java.io NotSerializableException Serializable]
    [java.lang.reflect Constructor Modifier]
    [cloiure.asm ClassVisitor ClassWriter Opcodes Type]
    [cloiure.asm.commons GeneratorAdapter Method]
    [cloiure.lang DynamicClassLoader IPersistentMap IProxy PersistentHashMap Reflector RT]
)

(§ defn method-sig [^java.lang.reflect.Method meth]
    [(.getName meth) (seq (.getParameterTypes meth)) (.getReturnType meth)]
)

(§ defn- most-specific [rtypes]
    (or (some (fn [t] (when (every? #(isa? t %) rtypes) t)) rtypes) (throw (Exception. "Incompatible return types")))
)

;;;
 ; Takes a collection of [msig meth] and returns a seq of maps from return-types to meths.
 ;;
(§ defn- group-by-sig [coll]
    (vals
        (reduce1
            (fn [m [msig meth]]
                (let [rtype (peek msig) argsig (pop msig)]
                    (assoc m argsig (assoc (m argsig {}) rtype meth))
                )
            )
            {} coll
        )
    )
)

(§ defn ^String proxy-name [^Class super interfaces]
    (let [inames (into1 (sorted-set) (map #(.getName ^Class %) interfaces))]
        (apply str (.replace (str *ns*) \- \_) ".proxy"
            (interleave (repeat "$")
                (concat
                    [(.getName super)]
                    (map #(subs % (inc (.lastIndexOf ^String % "."))) inames)
                    [(Integer/toHexString (hash inames))]
                )
            )
        )
    )
)

(§ defn- generate-proxy [^Class super interfaces]
    (let [cv         (ClassWriter. ClassWriter/COMPUTE_MAXS)
          pname      (proxy-name super interfaces)
          cname      (.replace pname \. \/) ;; (str "cloiure/lang/" (gensym "Proxy__"))
          ctype      (Type/getObjectType cname)
          iname      (fn [^Class c] (.getInternalName (Type/getType c)))
          fmap       "__cloiureFnMap"
          totype     (fn [^Class c] (Type/getType c))
          to-types   (fn [cs] (if (pos? (count cs)) (into-array (map totype cs)) (make-array Type 0)))
          super-type ^Type (totype super)
          imap-type  ^Type (totype IPersistentMap)
          ifn-type   (totype cloiure.lang.IFn)
          obj-type   (totype Object)
          sym-type   (totype cloiure.lang.Symbol)
          rt-type    (totype cloiure.lang.RT)
          ex-type    (totype java.lang.UnsupportedOperationException)
          gen-bridge
            (fn [^java.lang.reflect.Method meth ^java.lang.reflect.Method dest]
                (let [pclasses (.getParameterTypes meth)
                      ptypes   (to-types pclasses)
                      rtype    ^Type (totype (.getReturnType meth))
                      m        (Method. (.getName meth) rtype ptypes)
                      dtype    (totype (.getDeclaringClass dest))
                      dm       (Method. (.getName dest) (totype (.getReturnType dest)) (to-types (.getParameterTypes dest)))
                      gen      (GeneratorAdapter. (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_BRIDGE) m nil nil cv)]
                    (.visitCode gen)
                    (.loadThis gen)
                    (dotimes [i (count ptypes)]
                        (.loadArg gen i)
                    )
                    (if (-> dest .getDeclaringClass .isInterface)
                        (.invokeInterface gen dtype dm)
                        (.invokeVirtual gen dtype dm)
                    )
                    (.returnValue gen)
                    (.endMethod gen)
                )
            )
          gen-method
            (fn [^java.lang.reflect.Method meth else-gen]
                (let [pclasses   (.getParameterTypes meth)
                      ptypes     (to-types pclasses)
                      rtype      ^Type (totype (.getReturnType meth))
                      m          (Method. (.getName meth) rtype ptypes)
                      gen        (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)
                      else-label (.newLabel gen)
                      end-label  (.newLabel gen)
                      decl-type  (Type/getType (.getDeclaringClass meth))]
                    (.visitCode gen)
                    (if (> (count pclasses) 18)
                        (else-gen gen m)
                        (do
                            (.loadThis gen)
                            (.getField gen ctype fmap imap-type)
                            (.push gen (.getName meth))
                            ;; lookup fn in map
                            (.invokeStatic gen rt-type (Method/getMethod "Object get(Object, Object)"))
                            (.dup gen)
                            (.ifNull gen else-label)
                            ;; if found
                            (.checkCast gen ifn-type)
                            (.loadThis gen)
                            ;; box args
                            (dotimes [i (count ptypes)]
                                (.loadArg gen i)
                                (cloiure.lang.Compiler$HostExpr/emitBoxReturn nil gen (nth pclasses i))
                            )
                            ;; call fn
                            (.invokeInterface gen ifn-type (Method. "invoke" obj-type (into-array (cons obj-type (repeat (count ptypes) obj-type)))))
                            ;; unbox return
                            (.unbox gen rtype)
                            (when (= (.getSort rtype) Type/VOID)
                                (.pop gen)
                            )
                            (.goTo gen end-label)
                            ;; else call supplied alternative generator
                            (.mark gen else-label)
                            (.pop gen)
                            (else-gen gen m)
                            (.mark gen end-label)
                        )
                    )
                    (.returnValue gen)
                    (.endMethod gen)
                )
            )]
        ;; start class definition
        (.visit cv Opcodes/V1_5 (+ Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER) cname nil (iname super) (into-array (map iname (cons IProxy interfaces))))
        ;; add field for fn mappings
        (.visitField cv (+ Opcodes/ACC_PRIVATE Opcodes/ACC_VOLATILE) fmap (.getDescriptor imap-type) nil nil)
        ;; add ctors matching/calling super's
        (doseq [^Constructor ctor (.getDeclaredConstructors super)]
            (when-not (Modifier/isPrivate (.getModifiers ctor))
                (let [ptypes (to-types (.getParameterTypes ctor))
                      m (Method. "<init>" Type/VOID_TYPE ptypes)
                      gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)]
                    (.visitCode gen)
                    ;; call super ctor
                    (.loadThis gen)
                    (.dup gen)
                    (.loadArgs gen)
                    (.invokeConstructor gen super-type m)
                    (.returnValue gen)
                    (.endMethod gen)
                )
            )
        )
        ;; disable serialization
        (when (some #(isa? % Serializable) (cons super interfaces))
            (let [m (Method/getMethod "void writeObject(java.io.ObjectOutputStream)")
                  gen (GeneratorAdapter. Opcodes/ACC_PRIVATE m nil nil cv)]
                (.visitCode gen)
                (.loadThis gen)
                (.loadArgs gen)
                (.throwException gen (totype NotSerializableException) pname)
                (.endMethod gen)
            )
            (let [m (Method/getMethod "void readObject(java.io.ObjectInputStream)")
                  gen (GeneratorAdapter. Opcodes/ACC_PRIVATE m nil nil cv)]
                (.visitCode gen)
                (.loadThis gen)
                (.loadArgs gen)
                (.throwException gen (totype NotSerializableException) pname)
                (.endMethod gen)
            )
        )
        ;; add IProxy methods
        (let [m (Method/getMethod "void __initCloiureFnMappings(cloiure.lang.IPersistentMap)")
              gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)]
            (.visitCode gen)
            (.loadThis gen)
            (.loadArgs gen)
            (.putField gen ctype fmap imap-type)
            (.returnValue gen)
            (.endMethod gen)
        )
        (let [m (Method/getMethod "void __updateCloiureFnMappings(cloiure.lang.IPersistentMap)")
              gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)]
            (.visitCode gen)
            (.loadThis gen)
            (.dup gen)
            (.getField gen ctype fmap imap-type)
            (.checkCast gen (totype cloiure.lang.IPersistentCollection))
            (.loadArgs gen)
            (.invokeInterface gen (totype cloiure.lang.IPersistentCollection) (Method/getMethod "cloiure.lang.IPersistentCollection cons(Object)"))
            (.checkCast gen imap-type)
            (.putField gen ctype fmap imap-type)
            (.returnValue gen)
            (.endMethod gen)
        )
        (let [m (Method/getMethod "cloiure.lang.IPersistentMap __getCloiureFnMappings()")
              gen (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)]
            (.visitCode gen)
            (.loadThis gen)
            (.getField gen ctype fmap imap-type)
            (.returnValue gen)
            (.endMethod gen)
        )
        ;; calc set of supers' non-private instance methods
        (let [[mm considered]
                (loop [mm {} considered #{} c super]
                    (if c
                        (let [[mm considered]
                                (loop [mm mm considered considered meths (concat (seq (.getDeclaredMethods c)) (seq (.getMethods c)))]
                                    (if (seq meths)
                                        (let [^java.lang.reflect.Method meth (first meths) mods (.getModifiers meth) mk (method-sig meth)]
                                            (if (or (considered mk)
                                                    (not (or (Modifier/isPublic mods) (Modifier/isProtected mods)))
                                                 ;; (Modifier/isPrivate mods)
                                                    (Modifier/isStatic mods)
                                                    (Modifier/isFinal mods)
                                                    (= "finalize" (.getName meth))
                                                )
                                                (recur mm (conj considered mk) (next meths))
                                                (recur (assoc mm mk meth) (conj considered mk) (next meths))
                                            )
                                        )
                                        [mm considered]
                                    )
                                )]
                            (recur mm considered (.getSuperclass c))
                        )
                        [mm considered]
                    )
                )
              ifaces-meths
                (into1 {}
                    (for [^Class iface interfaces meth (.getMethods iface) :let [msig (method-sig meth)] :when (not (considered msig))]
                        {msig meth}
                    )
                )
              ;; Treat abstract methods as interface methods
              [mm ifaces-meths]
                (let [abstract? (fn [[_ ^Method meth]] (Modifier/isAbstract (.getModifiers meth)))
                      mm-no-abstract (remove abstract? mm)
                      abstract-meths (filter abstract? mm)]
                    [mm-no-abstract (concat ifaces-meths abstract-meths)]
                )
              mgroups      (group-by-sig (concat mm ifaces-meths))
              rtypes       (map #(most-specific (keys %)) mgroups)
              mb           (map #(vector (%1 %2) (vals (dissoc %1 %2))) mgroups rtypes)
              bridge?      (reduce1 into1 #{} (map second mb))
              ifaces-meths (remove bridge? (vals ifaces-meths))
              mm           (remove bridge? (vals mm))]
            ;; add methods matching supers', if no mapping -> call super
            (doseq [[^java.lang.reflect.Method dest bridges] mb ^java.lang.reflect.Method meth bridges]
                (gen-bridge meth dest)
            )
            (doseq [^java.lang.reflect.Method meth mm]
                (gen-method meth
                    (fn [^GeneratorAdapter gen ^Method m]
                        (.loadThis gen)
                        ;; push args
                        (.loadArgs gen)
                        ;; call super
                        (.visitMethodInsn gen Opcodes/INVOKESPECIAL (.getInternalName super-type) (.getName m) (.getDescriptor m))
                    )
                )
            )
            ;; add methods matching interfaces', if no mapping -> throw
            (doseq [^java.lang.reflect.Method meth ifaces-meths]
                (gen-method meth (fn [^GeneratorAdapter gen ^Method m] (.throwException gen ex-type (.getName m))))
            )
        )
        ;; finish class def
        (.visitEnd cv)
        [cname (.toByteArray cv)]
    )
)

(§ defn- get-super-and-interfaces [bases]
    (if (.isInterface ^Class (first bases))
        [Object bases]
        [(first bases) (next bases)]
    )
)

;;;
 ; Takes an optional single class followed by zero or more interfaces.
 ; If not supplied, class defaults to Object. Creates and returns
 ; an instance of a proxy class derived from the supplied classes.
 ; The resulting value is cached and used for any subsequent
 ; requests for the same class set. Returns a Class object.
 ;;
(§ defn get-proxy-class [& bases]
    (let [[super interfaces] (get-super-and-interfaces bases) pname (proxy-name super interfaces)]
        (or (RT/loadClassForName pname)
            (let [[cname bytecode] (generate-proxy super interfaces)]
                (.defineClass ^DynamicClassLoader (deref cloiure.lang.Compiler/LOADER) pname bytecode [super interfaces])
            )
        )
    )
)

;;;
 ; Takes a proxy class and any arguments for its superclass ctor and
 ; creates and returns an instance of the proxy.
 ;;
(§ defn construct-proxy [c & ctor-args]
    (Reflector/invokeConstructor c (to-array ctor-args))
)

;;;
 ; Takes a proxy instance and a map of strings (which must correspond to
 ; methods of the proxy superclass/superinterfaces) to fns (which must take
 ; arguments matching the corresponding method, plus an additional (explicit)
 ; first arg corresponding to this, and sets the proxy's fn map.
 ; Returns the proxy.
 ;;
(§ defn init-proxy [^IProxy proxy mappings]
    (.__initCloiureFnMappings proxy mappings)
    proxy
)

;;;
 ; Takes a proxy instance and a map of strings (which must correspond to
 ; methods of the proxy superclass/superinterfaces) to fns (which must take
 ; arguments matching the corresponding method, plus an additional (explicit)
 ; first arg corresponding to this, and updates (via assoc) the proxy's fn map.
 ; nil can be passed instead of a fn, in which case the corresponding method
 ; will revert to the default behavior. Note that this function can be used
 ; to update the behavior of an existing instance without changing its identity.
 ; Returns the proxy.
 ;;
(§ defn update-proxy [^IProxy proxy mappings]
    (.__updateCloiureFnMappings proxy mappings)
    proxy
)

;;;
 ; Takes a proxy instance and returns the proxy's fn map.
 ;;
(§ defn proxy-mappings [^IProxy proxy] (.__getCloiureFnMappings proxy))

;;;
 ; class-and-interfaces - a vector of class names.
 ; args - a (possibly empty) vector of arguments to the superclass constructor.
 ;
 ; f => (name [params*] body) or (name ([params*] body) ([params+] body) ...)
 ;
 ; Expands to code which creates a instance of a proxy class that implements
 ; the named class/interface(s) by calling the supplied fns. A single class,
 ; if provided, must be first. If not provided, it defaults to Object.
 ;
 ; The interfaces names must be valid interface types. If a method fn is not
 ; provided for a class method, the superclass methd will be called. If a method
 ; fn is not provided for an interface method, an UnsupportedOperationException
 ; will be thrown should it be called. Method fns are closures and can capture
 ; the environment in which proxy is called. Each method fn takes an additional
 ; implicit first arg, which is bound to 'this. Note that while method fns can
 ; be provided to override protected methods, they have no other access to
 ; protected members, nor to super, as these capabilities cannot be proxied.
 ;;
(§ defmacro proxy [class-and-interfaces args & fs]
    (let [bases (map #(or (resolve %) (throw (Exception. (str "Can't resolve: " %)))) class-and-interfaces)
          [super interfaces] (get-super-and-interfaces bases)
          pc-effect (apply get-proxy-class bases)
          pname (proxy-name super interfaces)]
        ;; remember the class to prevent it from disappearing before use
        (intern *ns* (symbol pname) pc-effect)
        `(let [ ;; pc# (get-proxy-class ~@class-and-interfaces)
               p# (new ~(symbol pname) ~@args)] ;; (construct-proxy pc# ~@args)]
            (init-proxy p#
                ~(loop [fmap {} fs fs]
                    (if fs
                        (let [[sym & meths] (first fs)
                              meths (if (vector? (first meths)) (list meths) meths)
                              meths (map (fn [[params & body]] (cons (apply vector 'this params) body)) meths)]
                            (if-not (contains? fmap (name sym))
                                (recur (assoc fmap (name sym) (cons `fn meths)) (next fs))
                                (throw (IllegalArgumentException. (str "Method '" (name sym) "' redefined")))
                            )
                        )
                        fmap
                    )
                )
            )
            p#
        )
    )
)

(§ defn proxy-call-with-super [call this meth]
    (let [m (proxy-mappings this)]
        (update-proxy this (assoc m meth nil))
        (try
            (call)
            (finally
                (update-proxy this m)
            )
        )
    )
)

;;;
 ; Use to call a superclass method in the body of a proxy method.
 ; Note, expansion captures 'this.
 ;;
(§ defmacro proxy-super [meth & args]
    `(proxy-call-with-super (fn [] (. ~'this ~meth ~@args)) ~'this ~(name meth))
)

(§ in-ns 'cloiure.core)

(§ import [java.io Writer])

;;;
 ; *print-length* controls how many items of each collection the printer will print.
 ; If it is bound to logical false, there is no limit. Otherwise, it must be bound
 ; to an integer indicating the maximum number of items of each collection to print.
 ; If a collection contains more items, the printer will print items up to the limit
 ; followed by '...' to represent the remaining items. The root binding is nil
 ; indicating no limit.
 ;;
(§ def ^:dynamic *print-length* nil)

;;;
 ; *print-level* controls how many levels deep the printer will print nested objects.
 ; If it is bound to logical false, there is no limit. Otherwise, it must be bound
 ; to an integer indicating the maximum level to print. Each argument to print is at
 ; level 0; if an argument is a collection, its items are at level 1; and so on.
 ; If an object is a collection and is at a level greater than or equal to the value
 ; bound to *print-level*, the printer prints '#' to represent it. The root binding
 ; is nil indicating no limit.
 ;;
(§ def ^:dynamic *print-level* nil)

;;;
 ; *print-namespace-maps* controls whether the printer will print namespace map literal
 ; syntax. It defaults to false, but the REPL binds to true.
 ;;
(§ def ^:dynamic *print-namespace-maps* false)

(§ defn- print-sequential [^String begin, print-one, ^String sep, ^String end, sequence, ^Writer w]
    (binding [*print-level* (and *print-level* (dec *print-level*))]
        (if (and *print-level* (neg? *print-level*))
            (.write w "#")
            (do
                (.write w begin)
                (when-let [xs (seq sequence)]
                    (if *print-length*
                        (loop [[x & xs] xs print-length *print-length*]
                            (if (zero? print-length)
                                (.write w "...")
                                (do
                                    (print-one x w)
                                    (when xs
                                        (.write w sep)
                                        (recur xs (dec print-length))
                                    )
                                )
                            )
                        )
                        (loop [[x & xs] xs]
                            (print-one x w)
                            (when xs
                                (.write w sep)
                                (recur xs)
                            )
                        )
                    )
                )
                (.write w end)
            )
        )
    )
)

(§ defn print-simple [o, ^Writer w]
    (.write w (str o))
)

(§ defmethod print-method :default [o, ^Writer w]
    (if (instance? cloiure.lang.IObj o)
        (print-method (vary-meta o #(dissoc % :type)) w)
        (print-simple o w)
    )
)

(§ defmethod print-method nil [o, ^Writer w]
    (.write w "nil")
)

(§ defn print-ctor [o print-args ^Writer w]
    (.write w "#=(")
    (.write w (.getName ^Class (class o)))
    (.write w ". ")
    (print-args o w)
    (.write w ")")
)

(§ defn- print-tagged-object [o rep ^Writer w]
    (.write w "#object[")
    (let [c (class o)]
        (if (.isArray c)
            (print-method (.getName c) w)
            (.write w (.getName c))
        )
    )
    (.write w " ")
    (.write w (format "0x%x " (System/identityHashCode o)))
    (print-method rep w)
    (.write w "]")
)

(§ defn- print-object [o, ^Writer w]
    (print-tagged-object o (str o) w)
)

(§ defmethod print-method Object [o, ^Writer w]
    (print-object o w)
)

(§ defmethod print-method cloiure.lang.Keyword [o, ^Writer w]
    (.write w (str o))
)

(§ defmethod print-method Number [o, ^Writer w]
    (.write w (str o))
)

(§ defmethod print-method Double [o, ^Writer w]
    (cond
        (= Double/POSITIVE_INFINITY o) (.write w "##Inf")
        (= Double/NEGATIVE_INFINITY o) (.write w "##-Inf")
        (.isNaN ^Double o) (.write w "##NaN")
        :else (.write w (str o))
    )
)

(§ defmethod print-method Float [o, ^Writer w]
    (cond
        (= Float/POSITIVE_INFINITY o) (.write w "##Inf")
        (= Float/NEGATIVE_INFINITY o) (.write w "##-Inf")
        (.isNaN ^Float o) (.write w "##NaN")
        :else (.write w (str o))
    )
)

(§ defmethod print-method Boolean [o, ^Writer w]
    (.write w (str o))
)

(§ defmethod print-method cloiure.lang.Symbol [o, ^Writer w]
    (print-simple o w)
)

(§ defmethod print-method cloiure.lang.Var [o, ^Writer w]
    (print-simple o w)
)

(§ defmethod print-method cloiure.lang.ISeq [o, ^Writer w]
    (print-sequential "(" pr-on " " ")" o w)
)

(§ prefer-method print-method cloiure.lang.ISeq cloiure.lang.IPersistentCollection)
(§ prefer-method print-method cloiure.lang.ISeq java.util.Collection)

;;;
 ; Returns escape string for char or nil if none.
 ;;
(§ def ^String char-escape-string
    (hash-map
        \newline   "\\n"
        \tab       "\\t"
        \return    "\\r"
        \"         "\\\""
        \\         "\\\\"
        \formfeed  "\\f"
        \backspace "\\b"
    )
)

(§ defmethod print-method String [^String s, ^Writer w]
    (if *print-readably*
        (do
            (.append w \") ;; oops! "
            (dotimes [n (count s)]
                (let [c (.charAt s n) e (char-escape-string c)]
                    (if e (.write w e) (.append w c))
                )
            )
            (.append w \") ;; oops! "
        )
        (.write w s)
    )
    nil
)

(§ defmethod print-method cloiure.lang.IPersistentVector [v, ^Writer w]
    (print-sequential "[" pr-on " " "]" v w)
)

(§ defn- print-prefix-map [prefix m print-one w]
    (print-sequential
        (str prefix "{")
        (fn [e ^Writer w] (do (print-one (key e) w) (.append w \space) (print-one (val e) w)))
        ", "
        "}"
        (seq m) w
    )
)

(§ defn- print-map [m print-one w]
    (print-prefix-map nil m print-one w)
)

(§ defn- strip-ns [named]
    (if (symbol? named)
        (symbol nil (name named))
        (keyword nil (name named))
    )
)

;;;
 ; Returns [lifted-ns lifted-map] or nil if m can't be lifted.
 ;;
(§ defn- lift-ns [m]
    (when *print-namespace-maps*
        (loop [ns nil [[k v :as entry] & entries] (seq m) lm {}]
            (if entry
                (when (or (keyword? k) (symbol? k))
                    (if ns
                        (when (= ns (namespace k))
                            (recur ns entries (assoc lm (strip-ns k) v))
                        )
                        (when-let [new-ns (namespace k)]
                            (recur new-ns entries (assoc lm (strip-ns k) v))
                        )
                    )
                )
                [ns (apply conj (empty m) lm)]
            )
        )
    )
)

(§ defmethod print-method cloiure.lang.IPersistentMap [m, ^Writer w]
    (let [[ns lift-map] (lift-ns m)]
        (if ns
            (print-prefix-map (str "#:" ns) lift-map pr-on w)
            (print-map m pr-on w)
        )
    )
)

(§ prefer-method print-method cloiure.lang.IPersistentCollection java.util.Collection)
(§ prefer-method print-method cloiure.lang.IPersistentCollection java.util.Map)

(§ defmethod print-method java.util.List [c, ^Writer w]
    (if *print-readably*
        (print-sequential "(" pr-on " " ")" c w)
        (print-object c w)
    )
)

(§ defmethod print-method java.util.Map [m, ^Writer w]
    (if *print-readably*
        (print-map m pr-on w)
        (print-object m w)
    )
)

(§ defmethod print-method java.util.Set [s, ^Writer w]
    (if *print-readably*
        (print-sequential "#{" pr-on " " "}" (seq s) w)
        (print-object s w)
    )
)

(§ defmethod print-method cloiure.lang.IPersistentSet [s, ^Writer w]
    (print-sequential "#{" pr-on " " "}" (seq s) w)
)

;;;
 ; Returns name string for char or nil if none
 ;;
(§ def ^String char-name-string
    (hash-map
        \newline   "newline"
        \tab       "tab"
        \space     "space"
        \backspace "backspace"
        \formfeed  "formfeed"
        \return    "return"
    )
)

(§ defmethod print-method java.lang.Character [^Character c, ^Writer w]
    (if *print-readably*
        (do
            (.append w \\)
            (let [n (char-name-string c)]
                (if n (.write w n) (.append w c))
            )
        )
        (.append w c)
    )
    nil
)

(§ def primitives-classnames
    (hash-map
        Float/TYPE     "Float/TYPE"
        Integer/TYPE   "Integer/TYPE"
        Long/TYPE      "Long/TYPE"
        Boolean/TYPE   "Boolean/TYPE"
        Character/TYPE "Character/TYPE"
        Double/TYPE    "Double/TYPE"
        Byte/TYPE      "Byte/TYPE"
        Short/TYPE     "Short/TYPE"
    )
)

(§ defmethod print-method Class [^Class c, ^Writer w]
    (.write w (.getName c))
)

(§ defmethod print-method java.math.BigDecimal [b, ^Writer w]
    (.write w (str b))
    (.write w "M")
)

(§ defmethod print-method cloiure.lang.BigInt [b, ^Writer w]
    (.write w (str b))
    (.write w "N")
)

(§ defmethod print-method java.util.regex.Pattern [p ^Writer w]
    (.write w "#\"")
    (loop [[^Character c & r :as s] (seq (.pattern ^java.util.regex.Pattern p)) qmode false]
        (when s
            (condp = c
                \\
                    (let [[^Character c2 & r2] r]
                        (.append w \\)
                        (.append w c2)
                        (if qmode
                            (recur r2 (not= c2 \E))
                            (recur r2 (= c2 \Q))
                        )
                    )
                \" ;; oops! "
                    (do
                        (if qmode
                            (.write w "\\E\\\"\\Q")
                            (.write w "\\\"")
                        )
                        (recur r qmode)
                    )
                (do
                    (.append w c)
                    (recur r qmode)
                )
            )
        )
    )
    (.append w \") ;; oops! "
)

(§ defn- deref-as-map [^cloiure.lang.IDeref o]
    (let [pending (and (instance? cloiure.lang.IPending o) (not (.isRealized ^cloiure.lang.IPending o)))
          [ex val]
            (when-not pending
                (try
                    [false (deref o)]
                    (catch Throwable e
                        [true e]
                    )
                )
            )]
        (hash-map
            :status
                (cond
                    ex :failed
                    pending :pending
                    :else :ready
                )
            :val val
        )
    )
)

(§ defmethod print-method cloiure.lang.IDeref [o ^Writer w]
    (print-tagged-object o (deref-as-map o) w)
)

(§ defmethod print-method StackTraceElement [^StackTraceElement o ^Writer w]
    (print-method [(symbol (.getClassName o)) (symbol (.getMethodName o)) (.getFileName o) (.getLineNumber o)] w)
)

;;;
 ; Constructs a data representation for a StackTraceElement.
 ;;
(§ defn StackTraceElement->vec [^StackTraceElement o]
    [(symbol (.getClassName o)) (symbol (.getMethodName o)) (.getFileName o) (.getLineNumber o)]
)

;;;
 ; Constructs a data representation for a Throwable.
 ;;
(§ defn Throwable->map [^Throwable o]
    (let [base
            (fn [^Throwable t]
                (merge {:type (symbol (.getName (class t))) :message (.getLocalizedMessage t)}
                    (when-let [ed (ex-data t)]
                        {:data ed}
                    )
                    (let [st (.getStackTrace t)]
                        (when (pos? (alength st))
                            {:at (StackTraceElement->vec (aget st 0))}
                        )
                    )
                )
            )
          via
            (loop [via [], ^Throwable t o]
                (if t (recur (conj via t) (.getCause t)) via)
            )
          ^Throwable root (peek via)
          m {
                :cause (.getLocalizedMessage root)
                :via (vec (map base via))
                :trace (vec (map StackTraceElement->vec (.getStackTrace ^Throwable (or root o))))
            }
          data (ex-data root)]
        (if data (assoc m :data data) m)
    )
)

(§ defn- print-throwable [^Throwable o ^Writer w]
    (.write w "#error {\n :cause ")
    (let [{:keys [cause data via trace]} (Throwable->map o)
          print-via
            #(do
                (.write w "{:type ")
                (print-method (:type %) w)
                (.write w "\n   :message ")
                (print-method (:message %) w)
                (when-let [data (:data %)]
                    (.write w "\n   :data ")
                    (print-method data w)
                )
                (when-let [at (:at %)]
                    (.write w "\n   :at ")
                    (print-method (:at %) w)
                )
                (.write w "}")
            )]
        (print-method cause w)
        (when data
            (.write w "\n :data ")
            (print-method data w)
        )
        (when via
            (.write w "\n :via\n [")
            (when-let [fv (first via)]
                (print-via fv)
                (doseq [v (rest via)]
                    (.write w "\n  ")
                    (print-via v)
                )
            )
            (.write w "]")
        )
        (when trace
            (.write w "\n :trace\n [")
            (when-let [ft (first trace)]
                (print-method ft w)
                (doseq [t (rest trace)]
                    (.write w "\n  ")
                    (print-method t w)
                )
            )
            (.write w "]")
        )
    )
    (.write w "}")
)

(§ defmethod print-method Throwable [^Throwable o ^Writer w]
    (print-throwable o w)
)

(§ def ^:private print-initialized true)

(§ in-ns 'cloiure.core)

(§ import
    [java.lang.reflect Constructor Modifier]
    [cloiure.asm ClassVisitor ClassWriter Opcodes Type]
    [cloiure.asm.commons GeneratorAdapter Method]
    [cloiure.lang IPersistentMap]
)

(§ def ^:private prim->class
     (hash-map
        'int      Integer/TYPE   'ints     (Class/forName "[I")
        'long     Long/TYPE      'longs    (Class/forName "[J")
        'float    Float/TYPE     'floats   (Class/forName "[F")
        'double   Double/TYPE    'doubles  (Class/forName "[D")
        'void     Void/TYPE
        'short    Short/TYPE     'shorts   (Class/forName "[S")
        'boolean  Boolean/TYPE   'booleans (Class/forName "[Z")
        'byte     Byte/TYPE      'bytes    (Class/forName "[B")
        'char     Character/TYPE 'chars    (Class/forName "[C")
    )
)

(§ defn- ^Class the-class [x]
    (cond
        (class? x) x
        (contains? prim->class x) (prim->class x)
        :else (let [s (str x)] (cloiure.lang.RT/classForName (if (some #{\. \[} s) s (str "java.lang." s))))
    )
)

;;;
 ; Returns an asm Type object for c, which may be a primitive class (such as Integer/TYPE),
 ; any other class (such as Double), or a fully-qualified class name given as a string or symbol
 ; (such as 'java.lang.String).
 ;;
(§ defn- ^Type asm-type [c]
    (if (or (instance? Class c) (prim->class c))
        (Type/getType (the-class c))
        (let [s (str c)]
            (Type/getObjectType (.replace (if (some #{\. \[} s) s (str "java.lang." s)) "." "/"))
        )
    )
)

(§ defn- generate-interface [{:keys [name extends methods]}]
    (when (some #(-> % first cloiure.core/name (.contains "-")) methods)
        (throw (IllegalArgumentException. "Interface methods must not contain '-'"))
    )
    (let [iname (.replace (str name) "." "/") cv (ClassWriter. ClassWriter/COMPUTE_MAXS)]
        (.visit cv Opcodes/V1_5 (+ Opcodes/ACC_PUBLIC Opcodes/ACC_ABSTRACT Opcodes/ACC_INTERFACE) iname nil "java/lang/Object"
            (when (seq extends)
                (into-array (map #(.getInternalName (asm-type %)) extends))
            )
        )
        (doseq [[mname pclasses rclass pmetas] methods]
            (let [md (Type/getMethodDescriptor (asm-type rclass) (if pclasses (into-array Type (map asm-type pclasses)) (make-array Type 0)))
                  mv (.visitMethod cv (+ Opcodes/ACC_PUBLIC Opcodes/ACC_ABSTRACT) (str mname) md nil nil)]
                (.visitEnd mv)
            )
        )
        (.visitEnd cv)
        [iname (.toByteArray cv)]
    )
)

;;;
 ; In all subsequent sections taking types, the primitive types can be
 ; referred to by their Java names (int, float etc), and classes in the
 ; java.lang package can be used without a package qualifier. All other
 ; classes must be fully qualified.
 ;
 ; Options should be a set of key/value pairs, all except for :name are
 ; optional:
 ;
 ; :name aname
 ;
 ; The package-qualified name of the class to be generated.
 ;
 ; :extends [interface ...]
 ;
 ; One or more interfaces, which will be extended by this interface.
 ;
 ; :methods [ [name [param-types] return-type], ...]
 ;
 ; This parameter is used to specify the signatures of the methods of the
 ; generated interface. Do not repeat superinterface signatures here.
 ;;
(§ defmacro gen-interface [& options]
    (let [options-map (apply hash-map options) [cname bytecode] (generate-interface options-map)]
        (.defineClass ^DynamicClassLoader (deref cloiure.lang.Compiler/LOADER) (str (:name options-map)) bytecode options)
    )
)

(§ in-ns 'cloiure.core)

;;;
 ; Convert a Cloiure namespace name to a legal Java package name.
 ;;
(§ defn namespace-munge [ns] (.replace (str ns) \- \_))

;; for now, built on gen-interface

;;;
 ; Creates a new Java interface with the given name and method sigs.
 ; The method return types and parameter types may be specified with
 ; type hints, defaulting to Object if omitted.
 ;
 ; (definterface MyInterface
 ;  (^int method1 [x])
 ;  (^Bar method2 [^Baz b ^Quux q]))
 ;;
(§ defmacro definterface [name & sigs]
    (let [tag (fn [x] (or (:tag (meta x)) Object))
          psig (fn [[name [& args]]] (vector name (vec (map tag args)) (tag name) (map meta args)))
          cname (with-meta (symbol (str (namespace-munge *ns*) "." name)) (meta name))]
        `(let []
            (gen-interface :name ~cname :methods ~(vec (map psig sigs)))
            (import ~cname)
        )
    )
)

(§ defn- parse-opts [s]
    (loop [opts {} [k v & rs :as s] s]
        (if (keyword? k)
            (recur (assoc opts k v) rs)
            [opts s]
        )
    )
)

(§ defn- parse-impls [specs]
    (loop [ret {} s specs]
        (if (seq s)
            (recur (assoc ret (first s) (take-while seq? (next s))) (drop-while seq? (next s)))
            ret
        )
    )
)

(§ defn- parse-opts+specs [opts+specs]
    (let [[opts specs] (parse-opts opts+specs)
          impls (parse-impls specs)
          interfaces
            (-> (map #(if (var? (resolve %)) (:on (deref (resolve %))) %) (keys impls))
                set (disj 'Object 'java.lang.Object) vec
            )
          methods
            (map (fn [[name params & body]] (cons name (maybe-destructured params body))) (apply concat (vals impls)))]
        (when-let [bad-opts (seq (remove #{:no-print :load-ns} (keys opts)))]
            (throw (IllegalArgumentException. (apply print-str "Unsupported option(s) -" bad-opts)))
        )
        [interfaces methods opts]
    )
)

;;;
 ; reify is a macro with the following structure:
 ;
 ; (reify options* specs*)
 ;
 ; Currently there are no options.
 ;
 ; Each spec consists of the protocol or interface name followed by
 ; zero or more method bodies:
 ;
 ; protocol-or-interface-or-Object
 ;  (methodName [args+] body)*
 ;
 ; Methods should be supplied for all methods of the desired protocol(s)
 ; and interface(s). You can also define overrides for methods of Object.
 ; Note that the first parameter must be supplied to correspond to the
 ; target object ('this' in Java parlance). Thus methods for interfaces
 ; will take one more argument than do the interface declarations. Note
 ; also that recur calls to the method head should *not* pass the target
 ; object, it will be supplied automatically and can not be substituted.
 ;
 ; The return type can be indicated by a type hint on the method name, and
 ; arg types can be indicated by a type hint on arg names. If you leave out
 ; all hints, reify will try to match on same name/arity method in the
 ; protocol(s)/interface(s) - this is preferred. If you supply any hints at
 ; all, no inference is done, so all hints (or default of Object) must be
 ; correct, for both arguments and return type. If a method is overloaded
 ; in a protocol/interface, multiple independent method definitions must be
 ; supplied. If overloaded with same arity in an interface you must specify
 ; complete hints to disambiguate - a missing hint implies Object.
 ;
 ; recur works to method heads. The method bodies of reify are lexical
 ; closures, and can refer to the surrounding local scope:
 ;
 ; (str (let [f "foo"]
 ;  (reify Object
 ;   (toString [this] f))))
 ; == "foo"
 ;
 ; (seq (let [f "foo"]
 ;  (reify cloiure.lang.Seqable
 ;   (seq [this] (seq f)))))
 ; == (\f \o \o)
 ;
 ; reify always implements cloiure.lang.IObj and transfers meta
 ; data of the form to the created object.
 ;
 ; (meta ^{:k :v} (reify Object (toString [this] "foo")))
 ; == {:k :v}
 ;;
(§ defmacro reify [& opts+specs]
    (let [[interfaces methods] (parse-opts+specs opts+specs)]
        (with-meta `(reify* ~interfaces ~@methods) (meta &form))
    )
)

(§ defn hash-combine [x y]
    (cloiure.lang.Util/hashCombine x (cloiure.lang.Util/hash y))
)

(§ defn munge [s]
    ((if (symbol? s) symbol str) (cloiure.lang.Compiler/munge (str s)))
)

(§ defn- imap-cons [^IPersistentMap this o]
    (cond
        (map-entry? o)
            (let [^java.util.Map$Entry pair o]
                (.assoc this (.getKey pair) (.getValue pair))
            )
        (instance? cloiure.lang.IPersistentVector o)
            (let [^cloiure.lang.IPersistentVector vec o]
                (.assoc this (.nth vec 0) (.nth vec 1))
            )
        :else
            (loop [this this o o]
                (if (seq o)
                    (let [^java.util.Map$Entry pair (first o)]
                        (recur (.assoc this (.getKey pair) (.getValue pair)) (rest o))
                    )
                    this
                )
            )
    )
)

;;;
 ; Used to build a positional factory for a given type. Because of the
 ; limitation of 20 arguments to Cloiure functions, this factory needs to be
 ; constructed to deal with more arguments. It does this by building a straight
 ; forward type ctor call in the <=20 case, and a call to the same
 ; ctor pulling the extra args out of the & overage parameter. Finally, the
 ; arity is constrained to the number of expected fields and an ArityException
 ; will be thrown at runtime if the actual arg count does not match.
 ;;
(§ defn- build-positional-factory [nom classname fields]
    (let [fn-name           (symbol (str '-> nom))
          [field-args over] (split-at 20 fields)
          field-count       (count fields)
          arg-count         (count field-args)
          over-count        (count over)
          docstring         (str "Positional factory function for class " classname ".")]
        `(defn ~fn-name
            ~docstring
            [~@field-args ~@(if (seq over) '[& overage] [])]
            ~(if (seq over)
                `(if (= (count ~'overage) ~over-count)
                    (new ~classname
                        ~@field-args
                        ~@(for [i (range 0 (count over))]
                            (list `nth 'overage i)
                        )
                    )
                    (throw (cloiure.lang.ArityException. (+ ~arg-count (count ~'overage)) (name '~fn-name)))
                )
                `(new ~classname ~@field-args)
            )
        )
    )
)

(§ defn- validate-fields [fields name]
    (when-not (vector? fields)
        (throw (AssertionError. "No fields vector given."))
    )
    (let [specials '#{__meta __hash __hasheq __extmap}]
        (when (some specials fields)
            (throw (AssertionError. (str "The names in " specials " cannot be used as field names for types.")))
        )
    )
    (let [non-syms (remove symbol? fields)]
        (when (seq non-syms)
            (throw (cloiure.lang.Compiler$CompilerException.
                (.deref cloiure.lang.Compiler/LINE)
                (.deref cloiure.lang.Compiler/COLUMN)
                (AssertionError. (apply str "deftype fields must be symbols, " *ns* "." name " had: " (interpose ", " non-syms)))
            ))
        )
    )
)

;;;
 ; Do not use this directly - use deftype.
 ;;
(§ defn- emit-deftype* [tagname cname fields interfaces methods opts]
    (let [classname (with-meta (symbol (str (namespace-munge *ns*) "." cname)) (meta cname)) interfaces (conj interfaces 'cloiure.lang.IType)]
        `(deftype* ~(symbol (name (ns-name *ns*)) (name tagname))
            ~classname
            ~fields
            :implements ~interfaces
            ~@(mapcat identity opts)
            ~@methods
        )
    )
)

;;;
 ; (deftype name [fields*] options* specs*)
 ;
 ; Options are expressed as sequential keywords and arguments (in any order).
 ;
 ; Supported options:

 ; :load-ns - if true, importing the type class will cause the namespace
 ;            in which the type was defined to be loaded. Defaults to false.
 ;
 ; Each spec consists of a protocol or interface name followed by zero
 ; or more method bodies:
 ;
 ; protocol-or-interface-or-Object
 ; (methodName [args*] body)*
 ;
 ; Dynamically generates compiled bytecode for class with the given name,
 ; in a package with the same name as the current namespace, the given fields,
 ; and, optionally, methods for protocols and/or interfaces.
 ;
 ; The class will have the (by default, immutable) fields named by fields, which
 ; can have type hints. Protocols/interfaces and methods are optional. The only
 ; methods that can be supplied are those declared in the protocols/interfaces.
 ; Note that method bodies are not closures, the local environment includes only
 ; the named fields, and those fields can be accessed directly. Fields can be
 ; qualified with the metadata :volatile-mutable true or :unsynchronized-mutable true,
 ; at which point (set! afield aval) will be supported in method bodies. Note well
 ; that mutable fields are extremely difficult to use correctly, and are present only
 ; to facilitate the building of higher level constructs, such as Cloiure's reference
 ; types, in Cloiure itself. They are for experts only - if the semantics and
 ; implications of :volatile-mutable or :unsynchronized-mutable are not immediately
 ; apparent to you, you should not be using them.
 ;
 ; Method definitions take the form:
 ;
 ; (methodname [args*] body)
 ;
 ; The argument and return types can be hinted on the arg and methodname
 ; symbols. If not supplied, they will be inferred, so type hints should be
 ; reserved for disambiguation.
 ;
 ; Methods should be supplied for all methods of the desired protocol(s)
 ; and interface(s). You can also define overrides for methods of Object.
 ; Note that a parameter must be supplied to correspond to the target object
 ; ('this' in Java parlance). Thus methods for interfaces will take one more
 ; argument than do the interface declarations. Note also that recur calls
 ; to the method head should *not* pass the target object, it will be
 ; supplied automatically and can not be substituted.
 ;
 ; In the method bodies, the (unqualified) name can be used to name the
 ; class (for calls to new, instance?, etc).
 ;
 ; One constructor will be defined, taking the designated fields. Note
 ; that the field names __meta, __extmap, __hash and __hasheq are currently
 ; reserved and should not be used when defining your own types.
 ;
 ; Given (deftype TypeName ...), a factory function called ->TypeName
 ; will be defined, taking positional parameters for the fields.
 ;;
(§ defmacro deftype [name fields & opts+specs]
    (validate-fields fields name)
    (let [gname                     name
          [interfaces methods opts] (parse-opts+specs opts+specs)
          ns-part                   (namespace-munge *ns*)
          classname                 (symbol (str ns-part "." gname))
          hinted-fields             fields
          fields                    (vec (map #(with-meta % nil) fields))
          [field-args over]         (split-at 20 fields)]
        `(let []
            ~(emit-deftype* name gname (vec hinted-fields) (vec interfaces) methods opts)
            (import ~classname)
            ~(build-positional-factory gname classname fields)
            ~classname
        )
    )
)

(§ defn- expand-method-impl-cache [^cloiure.lang.MethodImplCache cache c f]
    (if (.map cache)
        (let [cs (assoc (.map cache) c (cloiure.lang.MethodImplCache$Entry. c f))]
            (cloiure.lang.MethodImplCache. (.protocol cache) (.methodk cache) cs)
        )
        (let [cs (into1 {} (remove (fn [[c e]] (nil? e)) (map vec (partition 2 (.table cache)))))
              cs (assoc cs c (cloiure.lang.MethodImplCache$Entry. c f))]
            (if-let [[shift mask] (maybe-min-hash (map hash (keys cs)))]
                (let [table (make-array Object (* 2 (inc mask)))
                      table
                        (reduce1
                            (fn [^objects t [c e]]
                                (let [i (* 2 (int (shift-mask shift mask (hash c))))]
                                    (aset t i c)
                                    (aset t (inc i) e)
                                    t
                                )
                            )
                            table cs
                        )]
                    (cloiure.lang.MethodImplCache. (.protocol cache) (.methodk cache) shift mask table)
                )
                (cloiure.lang.MethodImplCache. (.protocol cache) (.methodk cache) cs)
            )
        )
    )
)

(§ defn- super-chain [^Class c]
    (when c
        (cons c (super-chain (.getSuperclass c)))
    )
)

(§ defn- pref
    ([] nil)
    ([a] a)
    ([^Class a ^Class b] (if (.isAssignableFrom a b) b a))
)

(§ defn find-protocol-impl [protocol x]
    (if (instance? (:on-interface protocol) x)
        x
        (let [c (class x) impl #(get (:impls protocol) %)]
            (or (impl c)
                (and c
                    (or (first (remove nil? (map impl (butlast (super-chain c)))))
                        (when-let [t (reduce1 pref (filter impl (disj (supers c) Object)))]
                            (impl t)
                        )
                        (impl Object)
                    )
                )
            )
        )
    )
)

(§ defn find-protocol-method [protocol methodk x]
    (get (find-protocol-impl protocol x) methodk)
)

(§ defn- protocol? [maybe-p]
    (boolean (:on-interface maybe-p))
)

(§ defn- implements? [protocol atype]
    (and atype (.isAssignableFrom ^Class (:on-interface protocol) atype))
)

;;;
 ; Returns true if atype extends protocol.
 ;;
(§ defn extends? [protocol atype]
    (boolean (or (implements? protocol atype) (get (:impls protocol) atype)))
)

;;;
 ; Returns a collection of the types explicitly extending protocol.
 ;;
(§ defn extenders [protocol] (keys (:impls protocol)))

;;;
 ; Returns true if x satisfies the protocol.
 ;;
(§ defn satisfies? [protocol x]
    (boolean (find-protocol-impl protocol x))
)

(§ defn -cache-protocol-fn [^cloiure.lang.AFunction pf x ^Class c ^cloiure.lang.IFn interf]
    (let [cache (.__methodImplCache pf)
          f (if (.isInstance c x) interf (find-protocol-method (.protocol cache) (.methodk cache) x))]
        (when-not f
            (throw (IllegalArgumentException.
                (str "No implementation of method: " (.methodk cache)
                     " of protocol: " (:var (.protocol cache))
                     " found for class: " (if (nil? x) "nil" (.getName (class x))))
            ))
        )
        (set! (.__methodImplCache pf) (expand-method-impl-cache cache (class x) f))
        f
    )
)

(§ defn- emit-method-builder [on-interface method on-method arglists]
    (let [methodk (keyword method) gthis (with-meta (gensym) {:tag 'cloiure.lang.AFunction}) ginterf (gensym)]
        `(fn [cache#]
            (let [~ginterf
                    (fn ~@(map
                        (fn [args]
                            (let [gargs (map #(gensym (str "gf__" % "__")) args) target (first gargs)]
                                `([~@gargs] (. ~(with-meta target {:tag on-interface}) (~(or on-method method) ~@(rest gargs))))
                            )
                        )
                        arglists
                    ))
                  ^cloiure.lang.AFunction f#
                    (fn ~gthis ~@(map
                        (fn [args]
                            (let [gargs (map #(gensym (str "gf__" % "__")) args) target (first gargs)]
                                `([~@gargs]
                                    (let [cache# (.__methodImplCache ~gthis)
                                          f# (.fnFor cache# (cloiure.lang.Util/classOf ~target))]
                                        (if f#
                                            (f# ~@gargs)
                                            ((-cache-protocol-fn ~gthis ~target ~on-interface ~ginterf) ~@gargs)
                                        )
                                    )
                                )
                            )
                        )
                        arglists
                    ))]
                (set! (.__methodImplCache f#) cache#)
                f#
            )
        )
    )
)

(§ defn -reset-methods [protocol]
    (doseq [[^cloiure.lang.Var v build] (:method-builders protocol)]
        (let [cache (cloiure.lang.MethodImplCache. protocol (keyword (.sym v)))]
            (.bindRoot v (build cache))
        )
    )
)

(§ defn- assert-same-protocol [protocol-var method-syms]
    (doseq [m method-syms]
        (let [v (resolve m) p (:protocol (meta v))]
            (when (and v (bound? v) (not= protocol-var p))
                (binding [*out* *err*]
                    (println "Warning: protocol" protocol-var "is overwriting"
                        (if p
                            (str "method " (.sym v) " of protocol " (.sym p))
                            (str "function " (.sym v))
                        )
                    )
                )
            )
        )
    )
)

(§ defn- emit-protocol [name opts+sigs]
    (let [iname (symbol (str (munge (namespace-munge *ns*)) "." (munge name)))
                [opts sigs]
                (loop [opts {:on (list 'quote iname) :on-interface iname} sigs opts+sigs]
                (condp #(%1 %2) (first sigs)
                    string? (recur (assoc opts :doc (first sigs)) (next sigs))
                    keyword? (recur (assoc opts (first sigs) (second sigs)) (nnext sigs))
                    [opts sigs]))
                sigs (when sigs
                    (reduce1 (fn [m s]
                                (let [name-meta (meta (first s))
                                        mname (with-meta (first s) nil)
                                        [arglists doc]
                                        (loop [as [] rs (rest s)]
                                        (if (vector? (first rs))
                                            (recur (conj as (first rs)) (next rs))
                                            [(seq as) (first rs)]))]
                                    (when (some #{0} (map count arglists))
                                    (throw (IllegalArgumentException. (str "Definition of function " mname " in protocol " name " must take at least one arg."))))
                                    (when (m (keyword mname))
                                    (throw (IllegalArgumentException. (str "Function " mname " in protocol " name " was redefined. Specify all arities in single definition."))))
                                    (assoc m (keyword mname)
                                        (merge name-meta
                                                {:name (vary-meta mname assoc :doc doc :arglists arglists)
                                                :arglists arglists
                                                :doc doc}))))
                                {} sigs))
                meths (mapcat (fn [sig]
                                (let [m (munge (:name sig))]
                                (map #(vector m (vec (repeat (dec (count %))'Object)) 'Object)
                                    (:arglists sig))))
                            (vals sigs))
    ]
        `(do
            (defonce ~name {})
            (gen-interface :name ~iname :methods ~meths)
            (alter-meta! (var ~name) assoc :doc ~(:doc opts))
            ~(when sigs
                `(#'assert-same-protocol (var ~name) '~(map :name (vals sigs)))
            )
            (alter-var-root (var ~name) merge
                (assoc ~opts
                    :sigs '~sigs
                    :var (var ~name)
                    :method-map
                        ~(and (:on opts)
                            (apply hash-map
                                (mapcat
                                    (fn [s] [(keyword (:name s)) (keyword (or (:on s) (:name s)))])
                                    (vals sigs)
                                )
                            )
                        )
                    :method-builders
                        ~(apply hash-map
                            (mapcat
                                (fn [s] [
                                    `(intern *ns* (with-meta '~(:name s) (merge '~s {:protocol (var ~name)})))
                                    (emit-method-builder (:on-interface opts) (:name s) (:on s) (:arglists s))
                                ])
                                (vals sigs)
                            )
                        )
                )
            )
            (-reset-methods ~name)
            '~name
        )
    )
)

;;;
 ; A protocol is a named set of named methods and their signatures:
 ;
 ; (defprotocol AProtocolName
 ;
 ;  ;; optional doc string
 ;  "A doc string for AProtocol abstraction"
 ;
 ;  ;; method signatures
 ;  (bar [this a b] "bar docs")
 ;  (baz [this a] [this a b] [this a b c] "baz docs"))
 ;
 ; No implementations are provided. Docs can be specified for the protocol
 ; overall and for each method. The above yields a set of polymorphic
 ; functions and a protocol object. All are namespace-qualified by the ns
 ; enclosing the definition The resulting functions dispatch on the type of
 ; their first argument, which is required and corresponds to the implicit
 ; target object ('this' in Java parlance). defprotocol is dynamic, has no
 ; special compile-time effect, and defines no new types or classes.
 ; Implementations of the protocol methods can be provided using extend.
 ;
 ; defprotocol will automatically generate a corresponding interface, with
 ; the same name as the protocol, i.e. given a protocol: my.ns/Protocol, an
 ; interface: my.ns.Protocol. The interface will have methods corresponding
 ; to the protocol functions, and the protocol will automatically work with
 ; instances of the interface.
 ;
 ; Note that you should not use this interface with deftype or reify, as
 ; they support the protocol directly:
 ;
 ; (defprotocol P
 ;  (foo [this])
 ;  (bar-me [this] [this y]))
 ;
 ; (deftype Foo [a b c]
 ;  P
 ;  (foo [this] a)
 ;  (bar-me [this] b)
 ;  (bar-me [this y] (+ c y)))
 ;
 ; (bar-me (Foo. 1 2 3) 42)
 ; => 45
 ;
 ; (foo
 ;  (let [x 42]
 ;   (reify P
 ;    (foo [this] 17)
 ;    (bar-me [this] x)
 ;    (bar-me [this y] x))))
 ; => 17
 ;;
(§ defmacro defprotocol [name & opts+sigs]
    (emit-protocol name opts+sigs)
)

;;;
 ; Implementations of protocol methods can be provided using the extend
 ; construct:
 ;
 ; (extend AType
 ;  AProtocol
 ;  {:foo an-existing-fn
 ;   :bar (fn [a b] ...)
 ;   :baz (fn ([a]...) ([a b] ...)...)}
 ;  BProtocol
 ;   {...}
 ;  ...)
 ;
 ; extend takes a type/class (or interface, see below), and one or more
 ; protocol + method map pairs. It will extend the polymorphism of the
 ; protocol's methods to call the supplied methods when an AType is
 ; provided as the first argument.
 ;
 ; Method maps are maps of the keyword-ized method names to ordinary
 ; fns. This facilitates easy reuse of existing fns and fn maps, for
 ; code reuse/mixins without derivation or composition. You can extend
 ; an interface to a protocol. This is primarily to facilitate interop
 ; with the host (e.g. Java) but opens the door to incidental multiple
 ; inheritance of implementation since a class can inherit from more
 ; than one interface, both of which extend the protocol. It is TBD how
 ; to specify which impl to use. You can extend a protocol on nil.
 ;
 ; If you are supplying the definitions explicitly (i.e. not reusing
 ; exsting functions or mixin maps), you may find it more convenient to
 ; use the extend-type or extend-protocol macros.
 ;
 ; Note that multiple independent extend clauses can exist for the same
 ; type, not all protocols need be defined in a single extend call.
 ;
 ; See also: extends?, satisfies?, extenders.
 ;;
(§ defn extend [atype & proto+mmaps]
    (doseq [[proto mmap] (partition 2 proto+mmaps)]
        (when-not (protocol? proto)
            (throw (IllegalArgumentException. (str proto " is not a protocol")))
        )
        (when (implements? proto atype)
            (throw (IllegalArgumentException. (str atype " already directly implements " (:on-interface proto) " for protocol:" (:var proto))))
        )
        (-reset-methods (alter-var-root (:var proto) assoc-in [:impls atype] mmap))
    )
)

(§ defn- emit-impl [[p fs]]
    [p (zipmap (map #(-> % first keyword) fs) (map #(cons `fn (drop 1 %)) fs))]
)

(§ defn- emit-hinted-impl [c [p fs]]
    (let [hint
            (fn [specs]
                (let [specs (if (vector? (first specs)) (list specs) specs)]
                    (map
                        (fn [[[target & args] & body]]
                            (cons (apply vector (vary-meta target assoc :tag c) args) body)
                        )
                        specs
                    )
                )
            )]
        [p (zipmap (map #(-> % first name keyword) fs) (map #(cons `fn (hint (drop 1 %))) fs))]
    )
)

(§ defn- emit-extend-type [c specs]
    (let [impls (parse-impls specs)]
        `(extend ~c ~@(mapcat (partial emit-hinted-impl c) impls))
    )
)

;;;
 ; A macro that expands into an extend call. Useful when you are supplying
 ; the definitions explicitly inline, extend-type automatically creates
 ; the maps required by extend. Propagates the class as a type hint on the
 ; first argument of all fns.
 ;
 ; (extend-type MyType
 ;  Countable
 ;  (cnt [c] ...)
 ;  Foo
 ;  (bar [x y] ...)
 ;  (baz ([x] ...) ([x y & zs] ...)))
 ;
 ; expands into:
 ;
 ; (extend MyType
 ;  Countable
 ;  {:cnt (fn [c] ...)}
 ;  Foo
 ;  {:baz (fn ([x] ...) ([x y & zs] ...))
 ;   :bar (fn [x y] ...)})
 ;;
(§ defmacro extend-type [t & specs]
    (emit-extend-type t specs)
)

(§ defn- emit-extend-protocol [p specs]
    (let [impls (parse-impls specs)]
        `(do
            ~@(map (fn [[t fs]] `(extend-type ~t ~p ~@fs)) impls)
        )
    )
)

;;;
 ; Useful when you want to provide several implementations of the same
 ; protocol all at once. Takes a single protocol and the implementation
 ; of that protocol for one or more types. Expands into calls to
 ; extend-type:
 ;
 ; (extend-protocol Protocol
 ;  AType
 ;  (foo [x] ...)
 ;  (bar [x y] ...)
 ;  BType
 ;  (foo [x] ...)
 ;  (bar [x y] ...)
 ;  AClass
 ;  (foo [x] ...)
 ;  (bar [x y] ...)
 ;  nil
 ;  (foo [x] ...)
 ;  (bar [x y] ...))
 ;
 ; expands into:
 ;
 ; (do
 ;  (cloiure.core/extend-type AType Protocol
 ;   (foo [x] ...)
 ;   (bar [x y] ...))
 ;  (cloiure.core/extend-type BType Protocol
 ;   (foo [x] ...)
 ;   (bar [x y] ...))
 ;  (cloiure.core/extend-type AClass Protocol
 ;   (foo [x] ...)
 ;   (bar [x y] ...))
 ;  (cloiure.core/extend-type nil Protocol
 ;   (foo [x] ...)
 ;   (bar [x y] ...)))
 ;;
(§ defmacro extend-protocol [p & specs]
    (emit-extend-protocol p specs)
)

#_(ns cloiure.core.protocols)

;;;
 ; Protocol for collection types that can implement reduce faster
 ; than first/next recursion. Called by cloiure.core/reduce.
 ; Baseline implementation defined in terms of Iterable.
 ;;
(§ defprotocol CollReduce
    (coll-reduce [coll f] [coll f val])
)

;;;
 ; Protocol for concrete seq types that can reduce themselves faster
 ; than first/next recursion. Called by cloiure.core/reduce.
 ;;
(§ defprotocol InternalReduce
    (internal-reduce [seq f start])
)

(§ defn- seq-reduce
    ([coll f]
        (if-let [s (seq coll)]
            (internal-reduce (next s) f (first s))
            (f)
        )
    )
    ([coll f val]
        (let [s (seq coll)]
            (internal-reduce s f val)
        )
    )
)

(§ defn- iter-reduce
    ([^java.lang.Iterable coll f]
        (let [iter (.iterator coll)]
            (if (.hasNext iter)
                (loop [ret (.next iter)]
                    (if (.hasNext iter)
                        (let [ret (f ret (.next iter))]
                            (if (reduced? ret)
                                @ret
                                (recur ret)
                            )
                        )
                        ret
                    )
                )
                (f)
            )
        )
    )
    ([^java.lang.Iterable coll f val]
        (let [iter (.iterator coll)]
            (loop [ret val]
                (if (.hasNext iter)
                    (let [ret (f ret (.next iter))]
                        (if (reduced? ret)
                            @ret
                            (recur ret)
                        )
                    )
                    ret
                )
            )
        )
    )
)

;;;
 ; Reduces a seq, ignoring any opportunities to switch to
 ; a more specialized implementation.
 ;;
(§ defn- naive-seq-reduce [s f val]
    (loop [s (seq s) val val]
        (if s
            (let [ret (f val (first s))]
                (if (reduced? ret)
                    @ret
                    (recur (next s) ret)
                )
            )
            val
        )
    )
)

;;;
 ; Reduces via IReduceInit if possible, else naively.
 ;;
(§ defn- interface-or-naive-reduce [coll f val]
    (if (instance? cloiure.lang.IReduceInit coll)
        (.reduce ^cloiure.lang.IReduceInit coll f val)
        (naive-seq-reduce coll f val)
    )
)

(§ extend-protocol CollReduce
    nil
    (coll-reduce
        ([coll f] (f))
        ([coll f val] val)
    )

    Object
    (coll-reduce
        ([coll f] (seq-reduce coll f))
        ([coll f val] (seq-reduce coll f val))
    )

    cloiure.lang.IReduceInit
    (coll-reduce
        ([coll f] (.reduce ^cloiure.lang.IReduce coll f))
        ([coll f val] (.reduce coll f val))
    )

    ;; aseqs are iterable, masking internal-reducers
    cloiure.lang.ASeq
    (coll-reduce
        ([coll f] (seq-reduce coll f))
        ([coll f val] (seq-reduce coll f val))
    )

    ;; for range
    cloiure.lang.LazySeq
    (coll-reduce
        ([coll f] (seq-reduce coll f))
        ([coll f val] (seq-reduce coll f val))
    )

    ;; vector's chunked seq is faster than its iter
    cloiure.lang.PersistentVector
    (coll-reduce
        ([coll f] (seq-reduce coll f))
        ([coll f val] (seq-reduce coll f val))
    )

    Iterable
    (coll-reduce
        ([coll f] (iter-reduce coll f))
        ([coll f val] (iter-reduce coll f val))
    )

    cloiure.lang.APersistentMap$KeySeq
    (coll-reduce
        ([coll f] (iter-reduce coll f))
        ([coll f val] (iter-reduce coll f val))
    )

    cloiure.lang.APersistentMap$ValSeq
    (coll-reduce
        ([coll f] (iter-reduce coll f))
        ([coll f val] (iter-reduce coll f val))
    )
)

(§ extend-protocol InternalReduce
    nil
    (internal-reduce [s f val] val)

    ;; handles vectors and ranges
    cloiure.lang.IChunkedSeq
    (internal-reduce [s f val]
        (if-let [s (seq s)]
            (if (chunked-seq? s)
                (let [ret (.reduce (chunk-first s) f val)]
                    (if (reduced? ret)
                        @ret
                        (recur (chunk-next s) f ret)
                    )
                )
                (interface-or-naive-reduce s f val)
            )
            val
        )
    )

    cloiure.lang.StringSeq
    (internal-reduce [str-seq f val]
        (let [s (.s str-seq) len (.length s)]
            (loop [i (.i str-seq) val val]
                (if (< i len)
                    (let [ret (f val (.charAt s i))]
                        (if (reduced? ret)
                            @ret
                            (recur (inc i) ret)
                        )
                    )
                    val
                )
            )
        )
    )

    java.lang.Object
    (internal-reduce [s f val]
        (loop [cls (class s) s s f f val val]
            (if-let [s (seq s)]
                (if (identical? (class s) cls)
                    (let [ret (f val (first s))]
                        (if (reduced? ret)
                            @ret
                            (recur cls (next s) f ret)
                        )
                    )
                    (interface-or-naive-reduce s f val)
                )
                val
            )
        )
    )
)

;;;
 ; Protocol for concrete associative types that can reduce themselves
 ; via a function of key and val faster than first/next recursion over
 ; map entries. Called by cloiure.core/reduce-kv, and has same
 ; semantics (just different arg order).
 ;;
(§ defprotocol IKVReduce
    (kv-reduce [amap f init])
)

(§ in-ns 'cloiure.core)

(§ import [cloiure.lang Murmur3])

(§ deftype VecNode [edit arr])

(§ def EMPTY-NODE (VecNode. nil (object-array 32)))

(§ definterface IVecImpl
    (^int tailoff [])
    (arrayFor [^int i])
    (pushTail [^int level ^cloiure.core.VecNode parent ^cloiure.core.VecNode tailnode])
    (popTail [^int level node])
    (newPath [edit ^int level node])
    (doAssoc [^int level node ^int i val])
)

(§ definterface ArrayManager
    (array [^int size])
    (^int alength [arr])
    (aclone [arr])
    (aget [arr ^int i])
    (aset [arr ^int i val])
)

(§ deftype ArrayChunk [^cloiure.core.ArrayManager am arr ^int off ^int end]
    cloiure.lang.Indexed
    (nth [_ i] (.aget am arr (+ off i)))
    (count [_] (- end off))

    cloiure.lang.IChunk
    (dropFirst [_]
        (if (= off end)
            (throw (IllegalStateException. "dropFirst of empty chunk"))
            (ArrayChunk. am arr (inc off) end)
        )
    )
    (reduce [_ f init]
        (loop [ret init i off]
            (if (< i end)
                (let [ret (f ret (.aget am arr i))]
                    (if (reduced? ret)
                        ret
                        (recur ret (inc i))
                    )
                )
                ret
            )
        )
    )
)

(§ deftype VecSeq [^cloiure.core.ArrayManager am ^cloiure.core.IVecImpl vec anode ^int i ^int offset]
    :no-print true

    cloiure.core.protocols.InternalReduce
    (internal-reduce [_ f val]
        (loop [result val aidx (+ i offset)]
            (if (< aidx (count vec))
                (let [node (.arrayFor vec aidx)
                      result
                        (loop [result result node-idx (bit-and 0x1f aidx)]
                            (if (< node-idx (.alength am node))
                                (let [result (f result (.aget am node node-idx))]
                                    (if (reduced? result)
                                        result
                                        (recur result (inc node-idx))
                                    )
                                )
                                result
                            )
                        )]
                    (if (reduced? result)
                        @result
                        (recur result (bit-and 0xffe0 (+ aidx 32)))
                    )
                )
                result
            )
        )
    )

    cloiure.lang.ISeq
    (first [_] (.aget am anode offset))
    (next [this]
        (if (< (inc offset) (.alength am anode))
            (VecSeq. am vec anode i (inc offset))
            (.chunkedNext this)
        )
    )
    (more [this]
        (let [s (.next this)]
            (or s cloiure.lang.PersistentList/EMPTY)
        )
    )
    (cons [this o] (cloiure.lang.Cons. o this))
    (count [this]
        (loop [i 1 s (next this)]
            (if s
                (if (instance? cloiure.lang.Counted s)
                    (+ i (.count s))
                    (recur (inc i) (next s))
                )
                i
            )
        )
    )
    (equiv [this o]
        (cond
            (identical? this o)
                true
            (or (instance? cloiure.lang.Sequential o) (instance? java.util.List o))
                (loop [me this you (seq o)]
                    (if (nil? me)
                        (nil? you)
                        (and (cloiure.lang.Util/equiv (first me) (first you))
                            (recur (next me) (next you))
                        )
                    )
                )
            :else
                false
        )
    )
    (empty [_] cloiure.lang.PersistentList/EMPTY)

    cloiure.lang.Seqable
    (seq [this] this)

    cloiure.lang.IChunkedSeq
    (chunkedFirst [_]
        (ArrayChunk. am anode offset (.alength am anode))
    )
    (chunkedNext [_]
        (let [nexti (+ i (.alength am anode))]
            (when (< nexti (count vec))
                (VecSeq. am vec (.arrayFor vec nexti) nexti 0)
            )
        )
    )
    (chunkedMore [this]
        (let [s (.chunkedNext this)]
            (or s cloiure.lang.PersistentList/EMPTY)
        )
    )
)

(§ defmethod print-method ::VecSeq [v w]
    ((get (methods print-method) cloiure.lang.ISeq) v w)
)

(§ deftype Vec [^cloiure.core.ArrayManager am ^int cnt ^int shift ^cloiure.core.VecNode root tail _meta]
    Object
    (equals [this o]
        (cond
            (identical? this o)
                true
            (instance? cloiure.lang.IPersistentVector o)
                (and (= cnt (count o))
                    (loop [i (int 0)]
                        (cond
                            (= i cnt) true
                            (.equals (.nth this i) (nth o i)) (recur (inc i))
                            :else false
                        )
                    )
                )
            (or (instance? cloiure.lang.Sequential o) (instance? java.util.List o))
                (if-let [st (seq this)]
                    (.equals st (seq o))
                    (nil? (seq o))
                )
            :else
                false
        )
    )

    ;; todo - cache
    (hashCode [this]
        (loop [hash (int 1) i (int 0)]
            (if (= i cnt)
                hash
                (let [val (.nth this i)]
                    (recur (unchecked-add-int (unchecked-multiply-int 31 hash) (cloiure.lang.Util/hash val)) (inc i))
                )
            )
        )
    )

    ;; todo - cache
    cloiure.lang.IHashEq
    (hasheq [this] (Murmur3/hashOrdered this))

    cloiure.lang.Counted
    (count [_] cnt)

    cloiure.lang.IMeta
    (meta [_] _meta)

    cloiure.lang.IObj
    (withMeta [_ m] (Vec. am cnt shift root tail m))

    cloiure.lang.Indexed
    (nth [this i]
        (let [a (.arrayFor this i)]
            (.aget am a (bit-and i (int 0x1f)))
        )
    )
    (nth [this i not-found]
        (let [z (int 0)]
            (if (and (>= i z) (< i (.count this)))
                (.nth this i)
                not-found
            )
        )
    )

    cloiure.lang.IPersistentCollection
    (cons [this val]
        (if (< (- cnt (.tailoff this)) (int 32))
            (let [new-tail (.array am (inc (.alength am tail)))]
                (System/arraycopy tail 0 new-tail 0 (.alength am tail))
                (.aset am new-tail (.alength am tail) val)
                (Vec. am (inc cnt) shift root new-tail (meta this))
            )
            (let [tail-node (VecNode. (.edit root) tail)]
                (if (> (bit-shift-right cnt (int 5)) (bit-shift-left (int 1) shift)) ;; overflow root?
                    (let [new-root (VecNode. (.edit root) (object-array 32))]
                        (-> ^objects (.arr new-root)
                            (aset 0 root)
                            (aset 1 (.newPath this (.edit root) shift tail-node))
                        )
                        (Vec. am (inc cnt) (+ shift (int 5)) new-root (let [tl (.array am 1)] (.aset am tl 0 val) tl) (meta this))
                    )
                    (Vec. am (inc cnt) shift (.pushTail this shift root tail-node) (let [tl (.array am 1)] (.aset am tl 0 val) tl) (meta this))
                )
            )
        )
    )
    (empty [_] (Vec. am 0 5 EMPTY-NODE (.array am 0) nil))
    (equiv [this o]
        (cond
            (instance? cloiure.lang.IPersistentVector o)
                (and
                    (= cnt (count o))
                    (loop [i (int 0)]
                        (cond
                            (= i cnt) true
                            (= (.nth this i) (nth o i)) (recur (inc i))
                            :else false
                        )
                    )
                )
            (or (instance? cloiure.lang.Sequential o) (instance? java.util.List o))
                (cloiure.lang.Util/equiv (seq this) (seq o))
            :else
                false
        )
    )

    cloiure.lang.IPersistentStack
    (peek [this]
        (when (> cnt (int 0))
            (.nth this (dec cnt))
        )
    )
    (pop [this]
        (cond
            (zero? cnt)
                (throw (IllegalStateException. "Can't pop empty vector"))
            (= 1 cnt)
                (Vec. am 0 5 EMPTY-NODE (.array am 0) (meta this))
            (> (- cnt (.tailoff this)) 1)
                (let [new-tail (.array am (dec (.alength am tail)))]
                    (System/arraycopy tail 0 new-tail 0 (.alength am new-tail))
                    (Vec. am (dec cnt) shift root new-tail (meta this))
                )
            :else
                (let [new-tail (.arrayFor this (- cnt 2)) new-root ^cloiure.core.VecNode (.popTail this shift root)]
                    (cond
                        (nil? new-root)
                            (Vec. am (dec cnt) shift EMPTY-NODE new-tail (meta this))
                        (and (> shift 5) (nil? (aget ^objects (.arr new-root) 1)))
                            (Vec. am (dec cnt) (- shift 5) (aget ^objects (.arr new-root) 0) new-tail (meta this))
                        :else
                            (Vec. am (dec cnt) shift new-root new-tail (meta this))
                    )
                )
        )
    )

    cloiure.lang.IPersistentVector
    (assocN [this i val]
        (cond
            (and (<= (int 0) i) (< i cnt))
                (if (>= i (.tailoff this))
                    (let [new-tail (.array am (.alength am tail))]
                        (System/arraycopy tail 0 new-tail 0 (.alength am tail))
                        (.aset am new-tail (bit-and i (int 0x1f)) val)
                        (Vec. am cnt shift root new-tail (meta this))
                    )
                    (Vec. am cnt shift (.doAssoc this shift root i val) tail (meta this))
                )
            (= i cnt)
                (.cons this val)
            :else
                (throw (IndexOutOfBoundsException.))
        )
    )
    (length [_] cnt)

    cloiure.lang.Reversible
    (rseq [this]
        (if (> (.count this) 0)
            (cloiure.lang.APersistentVector$RSeq. this (dec (.count this)))
            nil
        )
    )

    cloiure.lang.Associative
    (assoc [this k v]
        (if (cloiure.lang.Util/isInteger k)
            (.assocN this k v)
            (throw (IllegalArgumentException. "Key must be integer"))
        )
    )
    (containsKey [this k]
        (and (cloiure.lang.Util/isInteger k)
            (<= 0 (int k))
            (< (int k) cnt)
        )
    )
    (entryAt [this k]
        (if (.containsKey this k)
            (cloiure.lang.MapEntry/create k (.nth this (int k)))
            nil
        )
    )

    cloiure.lang.ILookup
    (valAt [this k not-found]
        (if (cloiure.lang.Util/isInteger k)
            (let [i (int k)]
                (if (and (>= i 0) (< i cnt))
                    (.nth this i)
                    not-found
                )
            )
            not-found
        )
    )
    (valAt [this k] (.valAt this k nil))

    cloiure.lang.IFn
    (invoke [this k]
        (if (cloiure.lang.Util/isInteger k)
            (let [i (int k)]
                (if (and (>= i 0) (< i cnt))
                    (.nth this i)
                    (throw (IndexOutOfBoundsException.))
                )
            )
            (throw (IllegalArgumentException. "Key must be integer"))
        )
    )

    cloiure.lang.Seqable
    (seq [this]
        (if (zero? cnt)
            nil
            (VecSeq. am this (.arrayFor this 0) 0 0)
        )
    )

    cloiure.lang.Sequential ;; marker, no methods

    cloiure.core.IVecImpl
    (tailoff [_] (- cnt (.alength am tail)))
    (arrayFor [this i]
        (if (and (<= (int 0) i) (< i cnt))
            (if (>= i (.tailoff this))
                tail
                (loop [node root level shift]
                    (if (zero? level)
                        (.arr node)
                        (recur (aget ^objects (.arr node) (bit-and (bit-shift-right i level) (int 0x1f))) (- level (int 5)))
                    )
                )
            )
            (throw (IndexOutOfBoundsException.))
        )
    )
    (pushTail [this level parent tailnode]
        (let [subidx (bit-and (bit-shift-right (dec cnt) level) (int 0x1f))
              parent ^cloiure.core.VecNode parent
              ret (VecNode. (.edit parent) (aclone ^objects (.arr parent)))
              node-to-insert
                (if (= level (int 5))
                    tailnode
                    (let [child (aget ^objects (.arr parent) subidx)]
                        (if child
                            (.pushTail this (- level (int 5)) child tailnode)
                            (.newPath this (.edit root) (- level (int 5)) tailnode)
                        )
                    )
                )]
            (aset ^objects (.arr ret) subidx node-to-insert)
            ret
        )
    )
    (popTail [this level node]
        (let [node ^cloiure.core.VecNode node
              subidx (bit-and (bit-shift-right (- cnt (int 2)) level) (int 0x1f))]
            (cond
                (> level 5)
                    (let [new-child (.popTail this (- level 5) (aget ^objects (.arr node) subidx))]
                        (if (and (nil? new-child) (zero? subidx))
                            nil
                            (let [arr (aclone ^objects (.arr node))]
                                (aset arr subidx new-child)
                                (VecNode. (.edit root) arr)
                            )
                        )
                    )
                (zero? subidx)
                    nil
                :else
                    (let [arr (aclone ^objects (.arr node))]
                        (aset arr subidx nil)
                        (VecNode. (.edit root) arr)
                    )
            )
        )
    )
    (newPath [this edit ^int level node]
        (if (zero? level)
            node
            (let [ret (VecNode. edit (object-array 32))]
                (aset ^objects (.arr ret) 0 (.newPath this edit (- level (int 5)) node))
                ret
            )
        )
    )
    (doAssoc [this level node i val]
        (let [node ^cloiure.core.VecNode node]
            (if (zero? level)
                ;; on this branch, array will need val type
                (let [arr (.aclone am (.arr node))]
                    (.aset am arr (bit-and i (int 0x1f)) val)
                    (VecNode. (.edit node) arr)
                )
                (let [arr (aclone ^objects (.arr node))
                      subidx (bit-and (bit-shift-right i level) (int 0x1f))]
                    (aset arr subidx (.doAssoc this (- level (int 5)) (aget arr subidx) i val))
                    (VecNode. (.edit node) arr)
                )
            )
        )
    )

    java.lang.Comparable
    (compareTo [this o]
        (if (identical? this o)
            0
            (let [^cloiure.lang.IPersistentVector v (cast cloiure.lang.IPersistentVector o) vcnt (.count v)]
                (cond
                    (< cnt vcnt)
                        -1
                    (> cnt vcnt)
                        1
                    :else
                        (loop [i (int 0)]
                            (if (= i cnt)
                                0
                                (let [comp (cloiure.lang.Util/compare (.nth this i) (.nth v i))]
                                    (if (= 0 comp)
                                        (recur (inc i))
                                        comp
                                    )
                                )
                            )
                        )
                )
            )
        )
    )

    java.lang.Iterable
    (iterator [this]
        (let [i (java.util.concurrent.atomic.AtomicInteger. 0)]
            (reify java.util.Iterator
                (hasNext [_] (< (.get i) cnt))
                (next [_]
                    (try
                        (.nth this (dec (.incrementAndGet i)))
                        (catch IndexOutOfBoundsException _
                            (throw (java.util.NoSuchElementException.))
                        )
                    )
                )
            )
        )
    )

    java.util.Collection
    (contains [this o] (boolean (some #(= % o) this)))
    (toArray [this] (into-array Object this))
    (toArray [this arr]
        (when (<= cnt (count arr)) => (into-array Object this)
            (dotimes [i cnt]
                (aset arr i (.nth this i))
            )
            arr
        )
    )
    (size [_] cnt)

    java.util.List
    (get [this i] (.nth this i))
)

(§ defmethod print-method ::Vec [v w]
    ((get (methods print-method) cloiure.lang.IPersistentVector) v w)
)

(§ defmacro ^:private mk-am [t]
    (let [garr (gensym) tgarr (with-meta garr {:tag (symbol (str t "s"))})]
        `(reify cloiure.core.ArrayManager
            (array [_ size#] (~(symbol (str t "-array")) size#))
            (alength [_ ~garr] (alength ~tgarr))
            (aclone [_ ~garr] (aclone ~tgarr))
            (aget [_ ~garr i#] (aget ~tgarr i#))
            (aset [_ ~garr i# val#] (aset ~tgarr i# (~t val#)))
        )
    )
)

(§ def ^:private ams
    (hash-map
        :int     (mk-am int)
        :long    (mk-am long)
        :float   (mk-am float)
        :double  (mk-am double)
        :byte    (mk-am byte)
        :short   (mk-am short)
        :char    (mk-am char)
        :boolean (mk-am boolean)
    )
)

(§ defmacro ^:private ams-check [t]
    `(let [am# (ams ~t)]
        (if am#
            am#
            (throw (IllegalArgumentException. (str "Unrecognized type " ~t)))
        )
    )
)

;;;
 ; Creates a new vector of a single primitive type t, where t is one
 ; of :int :long :float :double :byte :short :char or :boolean. The
 ; resulting vector complies with the interface of vectors in general,
 ; but stores the values unboxed internally.
 ;
 ; Optionally takes one or more elements to populate the vector.
 ;;
(§ defn vector-of
    ([t]
        (let [^cloiure.core.ArrayManager am (ams-check t)]
            (Vec. am 0 5 EMPTY-NODE (.array am 0) nil)
        )
    )
    ([t x1]
        (let [^cloiure.core.ArrayManager am (ams-check t) arr (.array am 1)]
            (.aset am arr 0 x1)
            (Vec. am 1 5 EMPTY-NODE arr nil)
        )
    )
    ([t x1 x2]
        (let [^cloiure.core.ArrayManager am (ams-check t) arr (.array am 2)]
            (.aset am arr 0 x1)
            (.aset am arr 1 x2)
            (Vec. am 2 5 EMPTY-NODE arr nil)
        )
    )
    ([t x1 x2 x3]
        (let [^cloiure.core.ArrayManager am (ams-check t) arr (.array am 3)]
            (.aset am arr 0 x1)
            (.aset am arr 1 x2)
            (.aset am arr 2 x3)
            (Vec. am 3 5 EMPTY-NODE arr nil)
        )
    )
    ([t x1 x2 x3 x4]
        (let [^cloiure.core.ArrayManager am (ams-check t) arr (.array am 4)]
            (.aset am arr 0 x1)
            (.aset am arr 1 x2)
            (.aset am arr 2 x3)
            (.aset am arr 3 x4)
            (Vec. am 4 5 EMPTY-NODE arr nil)
        )
    )
    ([t x1 x2 x3 x4 & xn]
        (loop [v (vector-of t x1 x2 x3 x4) xn xn]
            (if xn
                (recur (conj v (first xn)) (next xn))
                v
            )
        )
    )
)

(§ in-ns 'cloiure.core)

;; redefine reduce with internal-reduce

;;;
 ; f should be a function of 2 arguments. If val is not supplied, returns
 ; the result of applying f to the first 2 items in coll, then applying f
 ; to that result and the 3rd item, etc. If coll contains no items, f must
 ; accept no arguments as well, and reduce returns the result of calling f
 ; with no arguments. If coll has only 1 item, it is returned and f is not
 ; called. If val is supplied, returns the result of applying f to val and
 ; the first item in coll, then applying f to that result and the 2nd item,
 ; etc. If coll contains no items, returns val and f is not called.
 ;;
(§ defn reduce
    ([f coll]
        (if (instance? cloiure.lang.IReduce coll)
            (.reduce ^cloiure.lang.IReduce coll f)
            (cloiure.core.protocols/coll-reduce coll f)
        )
    )
    ([f val coll]
        (if (instance? cloiure.lang.IReduceInit coll)
            (.reduce ^cloiure.lang.IReduceInit coll f val)
            (cloiure.core.protocols/coll-reduce coll f val)
        )
    )
)

(§ extend-protocol cloiure.core.protocols/IKVReduce
    nil
    (kv-reduce [_ f init] init)

    ;; slow path default
    cloiure.lang.IPersistentMap
    (kv-reduce [amap f init] (reduce (fn [ret [k v]] (f ret k v)) init amap))

    cloiure.lang.IKVReduce
    (kv-reduce [amap f init] (.kvreduce amap f init))
)

;;;
 ; Reduces an associative collection. f should be a function of 3 arguments.
 ; Returns the result of applying f to init, the first key and the first value
 ; in coll, then applying f to that result and the 2nd key and value, etc.
 ; If coll contains no entries, returns init and f is not called. Note that
 ; reduce-kv is supported on vectors, where the keys will be the ordinals.
 ;;
(§ defn reduce-kv [f init coll]
    (cloiure.core.protocols/kv-reduce coll f init)
)

;;;
 ; Takes a reducing function f of 2 args and returns a fn suitable for
 ; transduce by adding an arity-1 signature that calls cf (default -
 ; identity) on the result argument.
 ;;
(§ defn completing
    ([f] (completing f identity))
    ([f cf]
        (fn
            ([] (f))
            ([x] (cf x))
            ([x y] (f x y))
        )
    )
)

;;;
 ; reduce with a transformation of f (xf). If init is not supplied, (f) will
 ; be called to produce it. f should be a reducing step function that accepts
 ; both 1 and 2 arguments, if it accepts only 2 you can add the arity-1 with
 ; 'completing'. Returns the result of applying (the transformed) xf to init
 ; and the first item in coll, then applying xf to that result and the 2nd
 ; item, etc. If coll contains no items, returns init and f is not called.
 ; Note that certain transforms may inject or skip items.
 ;;
(§ defn transduce
    ([xform f coll] (transduce xform f (f) coll))
    ([xform f init coll]
        (let [f (xform f)
              ret
                (if (instance? cloiure.lang.IReduceInit coll)
                    (.reduce ^cloiure.lang.IReduceInit coll f init)
                    (cloiure.core.protocols/coll-reduce coll f init)
                )]
            (f ret)
        )
    )
)

;;;
 ; Returns a new coll consisting of to-coll with all of the items of from-coll
 ; conjoined. A transducer may be supplied.
 ;;
(§ defn into
    ([] [])
    ([to] to)
    ([to from]
        (if (instance? cloiure.lang.IEditableCollection to)
            (with-meta (persistent! (reduce conj! (transient to) from)) (meta to))
            (reduce conj to from)
        )
    )
    ([to xform from]
        (if (instance? cloiure.lang.IEditableCollection to)
            (with-meta (persistent! (transduce xform conj! (transient to) from)) (meta to))
            (transduce xform conj to from)
        )
    )
)

;;;
 ; Returns a vector consisting of the result of applying f to the set of first
 ; items of each coll, followed by applying f to the set of second items in each
 ; coll, until any one of the colls is exhausted. Any remaining items in other
 ; colls are ignored. Function f should accept number-of-colls arguments.
 ;;
(§ defn mapv
    ([f coll]
        (-> (reduce (fn [v o] (conj! v (f o))) (transient []) coll) persistent!)
    )
    ([f c1 c2]
        (into [] (map f c1 c2))
    )
    ([f c1 c2 c3]
        (into [] (map f c1 c2 c3))
    )
    ([f c1 c2 c3 & colls]
        (into [] (apply map f c1 c2 c3 colls))
    )
)

;;;
 ; Returns a vector of the items in coll for which (pred item)
 ; returns logical true. pred must be free of side-effects.
 ;;
(§ defn filterv [pred coll]
    (-> (reduce (fn [v o] (if (pred o) (conj! v o) v)) (transient []) coll) persistent!)
)

;;;
 ; Takes any nested combination of sequential things (lists, vectors, etc.)
 ; and returns their contents as a single, flat sequence.
 ; (flatten nil) returns an empty sequence.
 ;;
(§ defn flatten [x]
    (filter (complement sequential?) (rest (tree-seq sequential? seq x)))
)

;;;
 ; Returns a map of the elements of coll keyed by the result of
 ; f on each element. The value at each key will be a vector of the
 ; corresponding elements, in the order they appeared in coll.
 ;;
(§ defn group-by [f coll]
    (persistent!
        (reduce
            (fn [ret x]
                (let [k (f x)]
                    (assoc! ret k (conj (get ret k []) x))
                )
            )
            (transient {}) coll
        )
    )
)

;;;
 ; Applies f to each value in coll, splitting it each time f returns
 ; a new value. Returns a lazy seq of partitions. Returns a stateful
 ; transducer when no collection is provided.
 ;;
(§ defn partition-by
    ([f]
        (fn [rf]
            (let [l (java.util.ArrayList.) pv (volatile! ::none)]
                (fn
                    ([] (rf))
                    ([result]
                        (let [result
                                (if (.isEmpty l)
                                    result
                                    (let [v (vec (.toArray l))]
                                        (.clear l) ;; clear first!
                                        (unreduced (rf result v))
                                    )
                                )]
                            (rf result)
                        )
                    )
                    ([result input]
                        (let [pval @pv val (f input)]
                            (vreset! pv val)
                            (if (or (identical? pval ::none) (= val pval))
                                (do
                                    (.add l input)
                                    result
                                )
                                (let [v (vec (.toArray l))]
                                    (.clear l)
                                    (let [ret (rf result v)]
                                        (when-not (reduced? ret)
                                            (.add l input)
                                        )
                                        ret
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
    ([f coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (let [fst (first s)
                      fv (f fst)
                      run (cons fst (take-while #(= fv (f %)) (next s)))]
                    (cons run (partition-by f (seq (drop (count run) s))))
                )
            )
        )
    )
)

;;;
 ; Returns a map from distinct items in coll to the number of times they appear.
 ;;
(§ defn frequencies [coll]
    (persistent!
        (reduce
            (fn [counts x]
                (assoc! counts x (inc (get counts x 0)))
            )
            (transient {}) coll
        )
    )
)

;;;
 ; Returns a lazy seq of the intermediate values of the reduction (as per reduce)
 ; of coll by f, starting with init.
 ;;
(§ defn reductions
    ([f coll]
        (lazy-seq
            (if-let [s (seq coll)]
                (reductions f (first s) (rest s))
                (list (f))
            )
        )
    )
    ([f init coll]
        (if (reduced? init)
            (list @init)
            (cons init
                (lazy-seq
                    (when-let [s (seq coll)]
                        (reductions f (f init (first s)) (rest s))
                    )
                )
            )
        )
    )
)

;;;
 ; Return a random element of the (sequential) collection. Will have
 ; the same performance characteristics as nth for the given collection.
 ;;
(§ defn rand-nth [coll]
    (nth coll (rand-int (count coll)))
)

;;;
 ; Returns a lazy sequence of lists like partition, but may include
 ; partitions with fewer than n items at the end. Returns a stateful
 ; transducer when no collection is provided.
 ;;
(§ defn partition-all
    ([^long n]
        (fn [rf]
            (let [l (java.util.ArrayList. n)]
                (fn
                    ([] (rf))
                    ([result]
                        (let [result
                                (if (.isEmpty l)
                                    result
                                    (let [v (vec (.toArray l))]
                                        (.clear l) ;; clear first!
                                        (unreduced (rf result v))
                                    )
                                )]
                            (rf result)
                        )
                    )
                    ([result input]
                        (.add l input)
                        (if (= n (.size l))
                            (let [v (vec (.toArray l))]
                                (.clear l)
                                (rf result v)
                            )
                            result
                        )
                    )
                )
            )
        )
    )
    ([n coll]
        (partition-all n n coll)
    )
    ([n step coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (let [seg (doall (take n s))]
                    (cons seg (partition-all n step (nthrest s step)))
                )
            )
        )
    )
)

;;;
 ; Return a random permutation of coll.
 ;;
(§ defn shuffle [^java.util.Collection coll]
    (let [al (java.util.ArrayList. coll)]
        (java.util.Collections/shuffle al)
        (cloiure.lang.RT/vector (.toArray al))
    )
)

;;;
 ; Returns a lazy sequence consisting of the result of applying f to 0
 ; and the first item of coll, followed by applying f to 1 and the second
 ; item in coll, etc, until coll is exhausted. Thus function f should
 ; accept 2 arguments, index and item. Returns a stateful transducer when
 ; no collection is provided.
 ;;
(§ defn map-indexed
    ([f]
        (fn [rf]
            (let [i (volatile! -1)]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input] (rf result (f (vswap! i inc) input)))
                )
            )
        )
    )
    ([f coll]
        (letfn [(mapi [idx coll]
                    (lazy-seq
                        (when-let [s (seq coll)]
                            (if (chunked-seq? s)
                                (let [c (chunk-first s) size (int (count c)) b (chunk-buffer size)]
                                    (dotimes [i size]
                                        (chunk-append b (f (+ idx i) (.nth c i)))
                                    )
                                    (chunk-cons (chunk b) (mapi (+ idx size) (chunk-rest s)))
                                )
                                (cons (f idx (first s)) (mapi (inc idx) (rest s)))
                            )
                        )
                    )
                )]
            (mapi 0 coll)
        )
    )
)

;;;
 ; Returns a lazy sequence of the non-nil results of (f item). Note,
 ; this means false return values will be included. f must be free of
 ; side-effects. Returns a transducer when no collection is provided.
 ;;
(§ defn keep
    ([f]
        (fn [rf]
            (fn
                ([] (rf))
                ([result] (rf result))
                ([result input]
                    (let [v (f input)]
                        (if (nil? v)
                            result
                            (rf result v)
                        )
                    )
                )
            )
        )
    )
    ([f coll]
        (lazy-seq
            (when-let [s (seq coll)]
                (if (chunked-seq? s)
                    (let [c (chunk-first s) size (count c) b (chunk-buffer size)]
                        (dotimes [i size]
                            (let [x (f (.nth c i))]
                                (when-not (nil? x)
                                    (chunk-append b x)
                                )
                            )
                        )
                        (chunk-cons (chunk b) (keep f (chunk-rest s)))
                    )
                    (let [x (f (first s))]
                        (if (nil? x)
                            (keep f (rest s))
                            (cons x (keep f (rest s)))
                        )
                    )
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of the non-nil results of (f index item).
 ; Note, this means false return values will be included. f must be free
 ; of side-effects. Returns a stateful transducer when no collection is
 ; provided.
 ;;
(§ defn keep-indexed
    ([f]
        (fn [rf]
            (let [iv (volatile! -1)]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input]
                        (let [i (vswap! iv inc) v (f i input)]
                            (if (nil? v)
                                result
                                (rf result v)
                            )
                        )
                    )
                )
            )
        )
    )
    ([f coll]
        (letfn [(keepi [idx coll]
                    (lazy-seq
                        (when-let [s (seq coll)]
                            (if (chunked-seq? s)
                                (let [c (chunk-first s) size (count c) b (chunk-buffer size)]
                                    (dotimes [i size]
                                        (let [x (f (+ idx i) (.nth c i))]
                                            (when-not (nil? x)
                                                (chunk-append b x)
                                            )
                                        )
                                    )
                                    (chunk-cons (chunk b) (keepi (+ idx size) (chunk-rest s)))
                                )
                                (let [x (f idx (first s))]
                                    (if (nil? x)
                                        (keepi (inc idx) (rest s))
                                        (cons x (keepi (inc idx) (rest s)))
                                    )
                                )
                            )
                        )
                    )
                )]
            (keepi 0 coll)
        )
    )
)

;;;
 ; If coll is counted? returns its count, else will count at most the first n
 ; elements of coll using its seq.
 ;;
(§ defn bounded-count [n coll]
    (if (counted? coll)
        (count coll)
        (loop [i 0 s (seq coll)]
            (if (and s (< i n))
                (recur (inc i) (next s))
                i
            )
        )
    )
)

;;;
 ; Takes a set of predicates and returns a function f that returns true if all
 ; of its composing predicates return a logical true value against all of its
 ; arguments, else it returns false. Note that f is short-circuiting in that
 ; it will stop execution on the first argument that triggers a logical false
 ; result against the original predicates.
 ;;
(§ defn every-pred
    ([p]
        (fn ep1
            ([] true)
            ([x] (boolean (p x)))
            ([x y] (boolean (and (p x) (p y))))
            ([x y z] (boolean (and (p x) (p y) (p z))))
            ([x y z & args] (boolean (and (ep1 x y z) (every? p args))))
        )
    )
    ([p1 p2]
        (fn ep2
            ([] true)
            ([x] (boolean (and (p1 x) (p2 x))))
            ([x y] (boolean (and (p1 x) (p1 y) (p2 x) (p2 y))))
            ([x y z] (boolean (and (p1 x) (p1 y) (p1 z) (p2 x) (p2 y) (p2 z))))
            ([x y z & args] (boolean (and (ep2 x y z) (every? #(and (p1 %) (p2 %)) args))))
        )
    )
    ([p1 p2 p3]
        (fn ep3
            ([] true)
            ([x] (boolean (and (p1 x) (p2 x) (p3 x))))
            ([x y] (boolean (and (p1 x) (p2 x) (p3 x) (p1 y) (p2 y) (p3 y))))
            ([x y z] (boolean (and (p1 x) (p2 x) (p3 x) (p1 y) (p2 y) (p3 y) (p1 z) (p2 z) (p3 z))))
            ([x y z & args] (boolean (and (ep3 x y z) (every? #(and (p1 %) (p2 %) (p3 %)) args))))
        )
    )
    ([p1 p2 p3 & ps]
        (let [ps (list* p1 p2 p3 ps)]
            (fn epn
                ([] true)
                ([x] (every? #(% x) ps))
                ([x y] (every? #(and (% x) (% y)) ps))
                ([x y z] (every? #(and (% x) (% y) (% z)) ps))
                ([x y z & args] (boolean (and (epn x y z) (every? #(every? % args) ps))))
            )
        )
    )
)

;;;
 ; Takes a set of predicates and returns a function f that returns the first
 ; logical true value returned by one of its composing predicates against any of
 ; its arguments, else it returns logical false. Note that f is short-circuiting
 ; in that it will stop execution on the first argument that triggers a logical
 ; true result against the original predicates.
 ;;
(§ defn some-fn
    ([p]
        (fn sp1
            ([] nil)
            ([x] (p x))
            ([x y] (or (p x) (p y)))
            ([x y z] (or (p x) (p y) (p z)))
            ([x y z & args] (or (sp1 x y z) (some p args)))
        )
    )
    ([p1 p2]
        (fn sp2
            ([] nil)
            ([x] (or (p1 x) (p2 x)))
            ([x y] (or (p1 x) (p1 y) (p2 x) (p2 y)))
            ([x y z] (or (p1 x) (p1 y) (p1 z) (p2 x) (p2 y) (p2 z)))
            ([x y z & args] (or (sp2 x y z) (some #(or (p1 %) (p2 %)) args)))
        )
    )
    ([p1 p2 p3]
        (fn sp3
            ([] nil)
            ([x] (or (p1 x) (p2 x) (p3 x)))
            ([x y] (or (p1 x) (p2 x) (p3 x) (p1 y) (p2 y) (p3 y)))
            ([x y z] (or (p1 x) (p2 x) (p3 x) (p1 y) (p2 y) (p3 y) (p1 z) (p2 z) (p3 z)))
            ([x y z & args] (or (sp3 x y z) (some #(or (p1 %) (p2 %) (p3 %)) args)))
        )
    )
    ([p1 p2 p3 & ps]
        (let [ps (list* p1 p2 p3 ps)]
            (fn spn
                ([] nil)
                ([x] (some #(% x) ps))
                ([x y] (some #(or (% x) (% y)) ps))
                ([x y z] (some #(or (% x) (% y) (% z)) ps))
                ([x y z & args] (or (spn x y z) (some #(some % args) ps)))
            )
        )
    )
)

;;;
 ; A good fdecl looks like (([a] ...) ([a b] ...)) near the end of defn.
 ;;
(§ defn- ^:dynamic assert-valid-fdecl [fdecl]
    (when (empty? fdecl)
        (throw (IllegalArgumentException. "Parameter declaration missing"))
    )
    (let [argdecls
            (map
                #(if (seq? %)
                    (first %)
                    (throw (IllegalArgumentException.
                        (if (seq? (first fdecl))
                            (str "Invalid signature \"" % "\" should be a list")
                            (str "Parameter declaration \"" % "\" should be a vector")
                        )
                    ))
                )
                fdecl
            )
          bad-args (seq (remove #(vector? %) argdecls))]
        (when bad-args
            (throw (IllegalArgumentException. (str "Parameter declaration \"" (first bad-args) "\" should be a vector")))
        )
    )
)

;;;
 ; Temporarily redefines Vars during a call to func. Each val of binding-map
 ; will replace the root value of its key which must be a Var. After func is
 ; called with no args, the root values of all the Vars will be set back to
 ; their old values. These temporary changes will be visible in all threads.
 ; Useful for mocking out functions during testing.
 ;;
(§ defn with-redefs-fn [binding-map func]
    (let [root-bind
            (fn [m]
                (doseq [[a-var a-val] m]
                    (.bindRoot ^cloiure.lang.Var a-var a-val)
                )
            )
          old-vals
            (zipmap
                (keys binding-map)
                (map #(.getRawRoot ^cloiure.lang.Var %) (keys binding-map))
            )]
        (try
            (root-bind binding-map)
            (func)
            (finally
                (root-bind old-vals)
            )
        )
    )
)

;;;
 ; binding => var-symbol temp-value-expr
 ;
 ; Temporarily redefines Vars while executing the body. The temp-value-exprs
 ; will be evaluated and each resulting value will replace in parallel the root
 ; value of its Var. After the body is executed, the root values of all the
 ; Vars will be set back to their old values. These temporary changes will be
 ; visible in all threads. Useful for mocking out functions during testing.
 ;;
(§ defmacro with-redefs [bindings & body]
    `(with-redefs-fn
        ~(zipmap (map #(list `var %) (take-nth 2 bindings)) (take-nth 2 (next bindings)))
        (fn [] ~@body)
    )
)

;;;
 ; Returns true if a value has been produced for a delay or lazy sequence.
 ;;
(§ defn realized? [^cloiure.lang.IPending x] (.isRealized x))

;;;
 ; Takes an expression and a set of test/form pairs. Threads expr (via ->)
 ; through each form for which the corresponding test expression is true.
 ; Note that, unlike cond branching, cond-> threading does not short circuit
 ; after the first true test expression.
 ;;
(§ defmacro cond-> [expr & clauses]
    (assert (even? (count clauses)))
    (let [g (gensym)
          steps (map (fn [[test step]] `(if ~test (-> ~g ~step) ~g)) (partition 2 clauses))]
        `(let [~g ~expr ~@(interleave (repeat g) (butlast steps))]
            ~(if (empty? steps)
                g
                (last steps)
            )
        )
    )
)

;;;
 ; Takes an expression and a set of test/form pairs. Threads expr (via ->>)
 ; through each form for which the corresponding test expression is true.
 ; Note that, unlike cond branching, cond->> threading does not short circuit
 ; after the first true test expression.
 ;;
(§ defmacro cond->> [expr & clauses]
    (assert (even? (count clauses)))
    (let [g (gensym)
          steps (map (fn [[test step]] `(if ~test (->> ~g ~step) ~g)) (partition 2 clauses))]
        `(let [~g ~expr ~@(interleave (repeat g) (butlast steps))]
            ~(if (empty? steps)
                g
                (last steps)
            )
        )
    )
)

;;;
 ; Binds name to expr, evaluates the first form in the lexical context
 ; of that binding, then binds name to that result, repeating for each
 ; successive form, returning the result of the last form.
 ;;
(§ defmacro as-> [expr name & forms]
    `(let [~name ~expr ~@(interleave (repeat name) (butlast forms))]
        ~(if (empty? forms)
            name
            (last forms)
        )
    )
)

;;;
 ; When expr is not nil, threads it into the first form (via ->),
 ; and when that result is not nil, through the next, etc.
 ;;
(§ defmacro some-> [expr & forms]
    (let [g (gensym)
          steps (map (fn [step] `(if (nil? ~g) nil (-> ~g ~step))) forms)]
        `(let [~g ~expr ~@(interleave (repeat g) (butlast steps))]
            ~(if (empty? steps)
                g
                (last steps)
            )
        )
    )
)

;;;
 ; When expr is not nil, threads it into the first form (via ->>),
 ; and when that result is not nil, through the next, etc.
 ;;
(§ defmacro some->> [expr & forms]
    (let [g (gensym)
          steps (map (fn [step] `(if (nil? ~g) nil (->> ~g ~step))) forms)]
        `(let [~g ~expr ~@(interleave (repeat g) (butlast steps))]
            ~(if (empty? steps)
                g
                (last steps)
            )
        )
    )
)

(§ defn ^:private preserving-reduced [rf]
    #(let [ret (rf %1 %2)]
        (if (reduced? ret) (reduced ret) ret)
    )
)

;;;
 ; A transducer which concatenates the contents of each input, which must
 ; be a collection, into the reduction.
 ;;
(§ defn cat [rf]
    (let [rrf (preserving-reduced rf)]
        (fn
            ([] (rf))
            ([result] (rf result))
            ([result input] (reduce rrf result input))
        )
    )
)

;;;
 ; Returns a transducer that ends transduction when pred returns true for an input.
 ; When retf is supplied it must be a fn of 2 arguments - it will be passed the
 ; (completed) result so far and the input that triggered the predicate, and its
 ; return value (if it does not throw an exception) will be the return value of the
 ; transducer. If retf is not supplied, the input that triggered the predicate will
 ; be returned. If the predicate never returns true the transduction is unaffected.
 ;;
(§ defn halt-when
    ([pred] (halt-when pred nil))
    ([pred retf]
        (fn [rf]
            (fn
                ([] (rf))
                ([result]
                    (if (and (map? result) (contains? result ::halt))
                        (::halt result)
                        (rf result)
                    )
                )
                ([result input]
                    (if (pred input)
                        (reduced {::halt (if retf (retf (rf result) input) input)})
                        (rf result input)
                    )
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence removing consecutive duplicates in coll.
 ; Returns a transducer when no collection is provided.
 ;;
(§ defn dedupe
    ([]
        (fn [rf]
            (let [pv (volatile! ::none)]
                (fn
                    ([] (rf))
                    ([result] (rf result))
                    ([result input]
                        (let [prior @pv]
                            (vreset! pv input)
                            (if (= prior input)
                                result
                                (rf result input)
                            )
                        )
                    )
                )
            )
        )
    )
    ([coll] (sequence (dedupe) coll))
)

;;;
 ; Returns items from coll with random probability of prob (0.0 - 1.0).
 ; Returns a transducer when no collection is provided.
 ;;
(§ defn random-sample
    ([prob     ] (filter (fn [_] (< (rand) prob))     ))
    ([prob coll] (filter (fn [_] (< (rand) prob)) coll))
)

(§ deftype Eduction [xform coll]
    Iterable
    (iterator [_] (cloiure.lang.TransformerIterator/create xform (cloiure.lang.RT/iter coll)))

    cloiure.lang.IReduceInit
    ;; Note that, (completing f) isolates completion of inner rf from outer rf.
    (reduce [_ f init] (transduce xform (completing f) init coll))

    cloiure.lang.Sequential
)

;;;
 ; Returns a reducible/iterable application of the transducers to the items in
 ; coll. Transducers are applied in order as if combined with comp. Note that,
 ; these applications will be performed every time reduce/iterator is called.
 ;;
(§ defn eduction [& xforms]
    (Eduction. (apply comp (butlast xforms)) (last xforms))
)

(§ defmethod print-method Eduction [c, ^Writer w]
    (if *print-readably*
        (print-sequential "(" pr-on " " ")" c w)
        (print-object c w)
    )
)

;;;
 ; Runs the supplied procedure (via reduce), for purposes of side effects,
 ; on successive items in the collection. Returns nil.
 ;;
(§ defn run! [proc coll]
    (reduce #(proc %2) nil coll)
    nil
)

#_(ns cloiure.set)

;;;
 ; Move a maximal element of coll according to fn k (which returns a number) to the front of coll.
 ;;
(§ defn- bubble-max-key [k coll]
    (let [max (apply max-key k coll)]
        (cons max (remove #(identical? max %) coll))
    )
)

;;;
 ; Return a set that is the union of the input sets.
 ;;
(§ defn union
    ([] #{})
    ([s1] s1)
    ([s1 s2]
        (if (< (count s1) (count s2))
            (reduce conj s2 s1)
            (reduce conj s1 s2)
        )
    )
    ([s1 s2 & sets]
        (let [bubbled-sets (bubble-max-key count (conj sets s2 s1))]
            (reduce into (first bubbled-sets) (rest bubbled-sets))
        )
    )
)

;;;
 ; Return a set that is the intersection of the input sets.
 ;;
(§ defn intersection
    ([s1] s1)
    ([s1 s2]
        (if (< (count s2) (count s1))
            (recur s2 s1)
            (reduce
                (fn [result item]
                    (if (contains? s2 item)
                        result
                        (disj result item)
                    )
                )
                s1 s1
            )
        )
    )
    ([s1 s2 & sets]
        (let [bubbled-sets (bubble-max-key #(- (count %)) (conj sets s2 s1))]
            (reduce intersection (first bubbled-sets) (rest bubbled-sets))
        )
    )
)

;;;
 ; Return a set that is the first set without elements of the remaining sets.
 ;;
(§ defn difference
    ([s1] s1)
    ([s1 s2]
        (if (< (count s1) (count s2))
            (reduce
                (fn [result item]
                    (if (contains? s2 item)
                        (disj result item)
                        result
                    )
                )
                s1 s1
            )
            (reduce disj s1 s2)
        )
    )
    ([s1 s2 & sets] (reduce difference s1 (conj sets s2)))
)

;;;
 ; Returns a set of the elements for which pred is true.
 ;;
(§ defn select [pred xset]
    (reduce (fn [s k] (if (pred k) s (disj s k))) xset xset)
)

;;;
 ; Returns a rel of the elements of xrel with only the keys in ks.
 ;;
(§ defn project [xrel ks]
    (with-meta (set (map #(select-keys % ks) xrel)) (meta xrel))
)

;;;
 ; Returns the map with the keys in kmap renamed to the vals in kmap.
 ;;
(§ defn rename-keys [map kmap]
    (reduce
        (fn [m [old new]]
            (if (contains? map old)
                (assoc m new (get map old))
                m
            )
        )
        (apply dissoc map (keys kmap)) kmap
    )
)

;;;
 ; Returns a rel of the maps in xrel with the keys in kmap renamed to the vals in kmap.
 ;;
(§ defn rename [xrel kmap]
    (with-meta (set (map #(rename-keys % kmap) xrel)) (meta xrel))
)

;;;
 ; Returns a map of the distinct values of ks in the xrel mapped to
 ; a set of the maps in xrel with the corresponding values of ks.
 ;;
(§ defn index [xrel ks]
    (reduce
        (fn [m x]
            (let [ik (select-keys x ks)]
                (assoc m ik (conj (get m ik #{}) x))
            )
        )
        {} xrel
    )
)

;;;
 ; Returns the map with the vals mapped to the keys.
 ;;
(§ defn map-invert [m] (reduce (fn [m [k v]] (assoc m v k)) {} m))

;;;
 ; When passed 2 rels, returns the rel corresponding to the natural join.
 ; When passed an additional keymap, joins on the corresponding keys.
 ;;
(§ defn join
    ([xrel yrel] ;; natural join
        (if (and (seq xrel) (seq yrel))
            (let [ks (intersection (set (keys (first xrel))) (set (keys (first yrel))))
                  [r s] (if (<= (count xrel) (count yrel)) [xrel yrel] [yrel xrel])
                  idx (index r ks)]
                (reduce
                    (fn [ret x]
                        (let [found (idx (select-keys x ks))]
                            (if found
                                (reduce #(conj %1 (merge %2 x)) ret found)
                                ret
                            )
                        )
                    )
                    #{} s
                )
            )
            #{}
        )
    )
    ([xrel yrel km] ;; arbitrary key mapping
        (let [[r s k] (if (<= (count xrel) (count yrel)) [xrel yrel (map-invert km)] [yrel xrel km])
              idx (index r (vals k))]
            (reduce
                (fn [ret x]
                    (let [found (idx (rename-keys (select-keys x (keys k)) k))]
                        (if found
                            (reduce #(conj %1 (merge %2 x)) ret found)
                            ret
                        )
                    )
                )
                #{} s
            )
        )
    )
)

;;;
 ; Is set1 a subset of set2?
 ;;
(§ defn ^Boolean subset? [set1 set2]
    (and (<= (count set1) (count set2)) (every? #(contains? set2 %) set1))
)

;;;
 ; Is set1 a superset of set2?
 ;;
(§ defn ^Boolean superset? [set1 set2]
    (and (>= (count set1) (count set2)) (every? #(contains? set1 %) set2))
)

#_(ns cloiure.data
    (:require [cloiure.set :as set]))

(§ declare diff)

;;;
 ; Internal helper for diff.
 ;;
(§ defn- atom-diff [a b] (if (= a b) [nil nil a] [a b nil]))

;; for big things a sparse vector class would be better

;;;
 ; Convert an associative-by-numeric-index collection into
 ; an equivalent vector, with nil for any missing keys.
 ;;
(§ defn- vectorize [m]
    (when (seq m)
        (reduce
            (fn [result [k v]] (assoc result k v))
            (vec (repeat (apply max (keys m)) nil))
            m
        )
    )
)

;;;
 ; Diff associative things a and b, comparing only the key k.
 ;;
(§ defn- diff-associative-key [a b k]
    (let [va (get a k) vb (get b k) [a* b* ab] (diff va vb) in-a (contains? a k) in-b (contains? b k)
          same (and in-a in-b (or (not (nil? ab)) (and (nil? va) (nil? vb))))]
        [
            (when (and in-a (or (not (nil? a*)) (not same))) {k a*})
            (when (and in-b (or (not (nil? b*)) (not same))) {k b*})
            (when same {k ab})
        ]
    )
)

;;;
 ; Diff associative things a and b, comparing only keys in ks.
 ;;
(§ defn- diff-associative [a b ks]
    (reduce
        (fn [diff1 diff2] (doall (map merge diff1 diff2)))
        [nil nil nil]
        (map (partial diff-associative-key a b) ks)
    )
)

(§ defn- diff-sequential [a b]
    (vec (map vectorize
        (diff-associative
            (if (vector? a) a (vec a))
            (if (vector? b) b (vec b))
            (range (max (count a) (count b)))
        )
    ))
)

;;;
 ; Implementation detail. Subject to change.
 ;;
(§ defprotocol EqualityPartition
    (equality-partition [x] "Implementation detail. Subject to change.")
)

;;;
 ; Implementation detail. Subject to change.
 ;;
(§ defprotocol Diff
    (diff-similar [a b] "Implementation detail. Subject to change.")
)

(§ extend nil
    Diff
    {:diff-similar atom-diff}
)

(§ extend Object
    Diff
    {:diff-similar (fn [a b] ((if (.. a getClass isArray) diff-sequential atom-diff) a b))}

    EqualityPartition
    {:equality-partition (fn [x] (if (.. x getClass isArray) :sequential :atom))}
)

(§ extend-protocol EqualityPartition
    nil
    (equality-partition [x] :atom)

    java.util.Set
    (equality-partition [x] :set)

    java.util.List
    (equality-partition [x] :sequential)

    java.util.Map
    (equality-partition [x] :map)
)

(§ defn- as-set-value [s] (if (set? s) s (into #{} s)))

(§ extend-protocol Diff
    java.util.Set
    (diff-similar [a b]
        (let [aval (as-set-value a) bval (as-set-value b)]
            [
                (not-empty (set/difference aval bval))
                (not-empty (set/difference bval aval))
                (not-empty (set/intersection aval bval))
            ]
        )
    )

    java.util.List
    (diff-similar [a b] (diff-sequential a b))

    java.util.Map
    (diff-similar [a b] (diff-associative a b (set/union (keys a) (keys b))))
)

;;;
 ; Recursively compares a and b, returning a tuple of
 ; [things-only-in-a things-only-in-b things-in-both].
 ; Comparison rules:
 ;
 ; * For equal a and b, return [nil nil a].
 ; * Maps are subdiffed where keys match and values differ.
 ; * Sets are never subdiffed.
 ; * All sequential things are treated as associative collections by their indexes, with results returned as vectors.
 ; * Everything else (including strings!) is treated as an atom and compared for equality.
 ;;
(§ defn diff [a b]
    (if (= a b)
        [nil nil a]
        (if (= (equality-partition a) (equality-partition b))
            (diff-similar a b)
            (atom-diff a b)
        )
    )
)

;;;
 ; Cloiure String utilities
 ;
 ; It is poor form to (:use cloiure.string). Instead, use require with :as
 ; to specify a prefix, e.g.
 ;
 ; (ns your.namespace.here
 ;  (:require [cloiure.string :as str]))
 ;
 ; Design notes for cloiure.string:
 ;
 ; 1. Strings are objects (as opposed to sequences). As such, the string being
 ; manipulated is the first argument to a function; passing nil will result in
 ; a NullPointerException unless documented otherwise. If you want sequence-y
 ; behavior instead, use a sequence.
 ;
 ; 2. Functions are generally not lazy, and call straight to host methods where
 ; those are available and efficient.
 ;
 ; 3. Functions take advantage of String implementation details to write
 ; high-performing loop/recurs instead of using higher-order functions.
 ; (This is not idiomatic in general-purpose application code.)
 ;
 ; 4. When a function is documented to accept a string argument, it will take
 ; any implementation of the correct *interface* on the host platform. In Java,
 ; this is CharSequence, which is more general than String. In ordinary usage
 ; you will almost always pass concrete strings. If you are doing something
 ; unusual, e.g. passing a mutable implementation of CharSequence, then
 ; thread-safety is your responsibility.
 ;;
#_(ns cloiure.string
    (:refer-cloiure :exclude [replace reverse])
    (:import [java.util.regex Pattern Matcher]
             [cloiure.lang LazilyPersistentVector]))

;;;
 ; Returns s with its characters reversed.
 ;;
(§ defn ^String reverse [^CharSequence s]
    (.toString (.reverse (StringBuilder. s)))
)

;;;
 ; Given a replacement string that you wish to be a literal replacement
 ; for a pattern match in replace or replace-first, do the necessary
 ; escaping of special characters in the replacement.
 ;;
(§ defn ^String re-quote-replacement [^CharSequence replacement]
    (Matcher/quoteReplacement (.toString ^CharSequence replacement))
)

(§ defn- replace-by [^CharSequence s re f]
    (let [m (re-matcher re s)]
        (if (.find m)
            (let [buffer (StringBuffer. (.length s))]
                (loop [found true]
                    (if found
                        (do
                            (.appendReplacement m buffer (Matcher/quoteReplacement (f (re-groups m))))
                            (recur (.find m))
                        )
                        (do
                            (.appendTail m buffer)
                            (.toString buffer)
                        )
                    )
                )
            )
            s
        )
    )
)

;;;
 ; Replaces all instance of match with replacement in s.
 ;
 ; match/replacement can be:
 ;
 ;  string / string
 ;  char / char
 ;  pattern / (string or function of match).
 ;
 ; See also replace-first.
 ;
 ; The replacement is literal (i.e. none of its characters are treated
 ; specially) for all cases above except pattern / string.
 ;
 ; For pattern / string, $1, $2, etc. in the replacement string are
 ; substituted with the string that matched the corresponding
 ; parenthesized group in the pattern. If you wish your replacement
 ; string r to be used literally, use (re-quote-replacement r) as
 ; the replacement argument. See also documentation for
 ; java.util.regex.Matcher's appendReplacement method.
 ;
 ; Example:
 ;
 ; (cloiure.string/replace "Almost Pig Latin" #"\b(\w)(\w+)\b" "$2$1ay")
 ; -> "lmostAay igPay atinLay"
 ;;
(§ defn ^String replace [^CharSequence s match replacement]
    (let [s (.toString s)]
        (cond
            (instance? Character match)
                (.replace s ^Character match ^Character replacement)
            (instance? CharSequence match)
                (.replace s ^CharSequence match ^CharSequence replacement)
            (instance? Pattern match)
                (if (instance? CharSequence replacement)
                    (.replaceAll (re-matcher ^Pattern match s) (.toString ^CharSequence replacement))
                    (replace-by s match replacement)
                )
            :else
                (throw (IllegalArgumentException. (str "Invalid match arg: " match)))
        )
    )
)

(§ defn- replace-first-by [^CharSequence s ^Pattern re f]
    (let [m (re-matcher re s)]
        (if (.find m)
            (let [buffer (StringBuffer. (.length s)) rep (Matcher/quoteReplacement (f (re-groups m)))]
                (.appendReplacement m buffer rep)
                (.appendTail m buffer)
                (str buffer)
            )
            s
        )
    )
)

(§ defn- replace-first-char [^CharSequence s ^Character match replace]
    (let [s (.toString s) i (.indexOf s (int match))]
        (if (= -1 i)
            s
            (str (subs s 0 i) replace (subs s (inc i)))
        )
    )
)

(§ defn- replace-first-str [^CharSequence s ^String match ^String replace]
    (let [^String s (.toString s) i (.indexOf s match)]
        (if (= -1 i)
            s
            (str (subs s 0 i) replace (subs s (+ i (.length match))))
        )
    )
)

;;;
 ; Replaces the first instance of match with replacement in s.
 ;
 ; match/replacement can be:
 ;
 ;  char / char
 ;  string / string
 ;  pattern / (string or function of match).
 ;
 ; See also replace.
 ;
 ; The replacement is literal (i.e. none of its characters are treated
 ; specially) for all cases above except pattern / string.
 ;
 ; For pattern / string, $1, $2, etc. in the replacement string are
 ; substituted with the string that matched the corresponding
 ; parenthesized group in the pattern. If you wish your replacement
 ; string r to be used literally, use (re-quote-replacement r) as
 ; the replacement argument. See also documentation for
 ; java.util.regex.Matcher's appendReplacement method.
 ;
 ; Example:
 ;
 ; (cloiure.string/replace-first "swap first two words"
 ; #"(\w+)(\s+)(\w+)" "$3$2$1")
 ; -> "first swap two words"
 ;;
(§ defn ^String replace-first [^CharSequence s match replacement]
    (let [s (.toString s)]
        (cond
            (instance? Character match)
                (replace-first-char s match replacement)
            (instance? CharSequence match)
                (replace-first-str s (.toString ^CharSequence match) (.toString ^CharSequence replacement))
            (instance? Pattern match)
                (if (instance? CharSequence replacement)
                    (.replaceFirst (re-matcher ^Pattern match s) (.toString ^CharSequence replacement))
                    (replace-first-by s match replacement)
                )
            :else
                (throw (IllegalArgumentException. (str "Invalid match arg: " match)))
        )
    )
)

;;;
 ; Returns a string of all elements in coll, as returned by (seq coll),
 ; separated by an optional separator.
 ;;
(§ defn ^String join
    ([coll] (apply str coll))
    ([separator coll]
        (loop [sb (StringBuilder. (str (first coll))) more (next coll) sep (str separator)]
            (if more
                (recur (-> sb (.append sep) (.append (str (first more)))) (next more) sep)
                (str sb)
            )
        )
    )
)

;;;
 ; Converts first character of the string to upper-case, all other characters to lower-case.
 ;;
(§ defn ^String capitalize [^CharSequence s]
    (let [s (.toString s)]
        (if (< (count s) 2)
            (.toUpperCase s)
            (str (.toUpperCase (subs s 0 1)) (.toLowerCase (subs s 1)))
        )
    )
)

;;;
 ; Converts string to all upper-case.
 ;;
(§ defn ^String upper-case [^CharSequence s] (-> s (.toString) (.toUpperCase)))

;;;
 ; Converts string to all lower-case.
 ;;
(§ defn ^String lower-case [^CharSequence s] (-> s (.toString) (.toLowerCase)))

;;;
 ; Splits string on a regular expression. Optional argument limit is
 ; the maximum number of splits. Not lazy. Returns vector of the splits.
 ;;
(§ defn split
    ([^CharSequence s ^Pattern re      ] (LazilyPersistentVector/createOwning (.split re s      )))
    ([^CharSequence s ^Pattern re limit] (LazilyPersistentVector/createOwning (.split re s limit)))
)

;;;
 ; Splits s on \n or \r\n.
 ;;
(§ defn split-lines [^CharSequence s] (split s #"\r?\n"))

;;;
 ; Removes whitespace from both ends of string.
 ;;
(§ defn ^String trim [^CharSequence s]
    (let [len (.length s)]
        (loop [rindex len]
            (if (zero? rindex)
                ""
                (if (Character/isWhitespace (.charAt s (dec rindex)))
                    (recur (dec rindex))
                    ;; There is at least one non-whitespace char in the string,
                    ;; so no need to check for lindex reaching len.
                    (loop [lindex 0]
                        (if (Character/isWhitespace (.charAt s lindex))
                            (recur (inc lindex))
                            (-> s (.subSequence lindex rindex) (.toString))
                        )
                    )
                )
            )
        )
    )
)

;;;
 ; Removes whitespace from the left side of string.
 ;;
(§ defn ^String triml [^CharSequence s]
    (let [len (.length s)]
        (loop [index 0]
            (if (= len index)
                ""
                (if (Character/isWhitespace (.charAt s index))
                    (recur (unchecked-inc index))
                    (-> s (.subSequence index len) (.toString))
                )
            )
        )
    )
)

;;;
 ; Removes whitespace from the right side of string.
 ;;
(§ defn ^String trimr [^CharSequence s]
    (loop [index (.length s)]
        (if (zero? index)
            ""
            (if (Character/isWhitespace (.charAt s (unchecked-dec index)))
                (recur (unchecked-dec index))
                (-> s (.subSequence 0 index) (.toString))
            )
        )
    )
)

;;;
 ; Removes all trailing newline \n or return \r characters from string.
 ; Similar to Perl's chomp.
 ;;
(§ defn ^String trim-newline [^CharSequence s]
    (loop [index (.length s)]
        (if (zero? index)
            ""
            (let [ch (.charAt s (dec index))]
                (if (or (= ch \newline) (= ch \return))
                    (recur (dec index))
                    (-> s (.subSequence 0 index) (.toString))
                )
            )
        )
    )
)

;;;
 ; True if s is nil, empty, or contains only whitespace.
 ;;
(§ defn blank? [^CharSequence s]
    (if s
        (loop [index (int 0)]
            (if (= (.length s) index)
                true
                (if (Character/isWhitespace (.charAt s index))
                    (recur (inc index))
                    false
                )
            )
        )
        true
    )
)

;;;
 ; Return a new string, using cmap to escape each character ch
 ; from s as follows:
 ;
 ; If (cmap ch) is nil, append ch to the new string.
 ; If (cmap ch) is non-nil, append (str (cmap ch)) instead.
 ;;
(§ defn ^String escape [^CharSequence s cmap]
    (loop [index (int 0) buffer (StringBuilder. (.length s))]
        (if (= (.length s) index)
            (.toString buffer)
            (let [ch (.charAt s index)]
                (if-let [replacement (cmap ch)]
                    (.append buffer replacement)
                    (.append buffer ch)
                )
                (recur (inc index) buffer)
            )
        )
    )
)

;;;
 ; Return index of value (string or char) in s, optionally searching
 ; forward from from-index. Return nil if value not found.
 ;;
(§ defn index-of
    ([^CharSequence s value]
        (let [result ^long
                (if (instance? Character value)
                    (.indexOf (.toString s) ^int (.charValue ^Character value))
                    (.indexOf (.toString s) ^String value)
                )]
            (if (= result -1) nil result)
        )
    )
    ([^CharSequence s value ^long from-index]
        (let [result ^long
                (if (instance? Character value)
                    (.indexOf (.toString s) ^int (.charValue ^Character value) (unchecked-int from-index))
                    (.indexOf (.toString s) ^String value (unchecked-int from-index))
                )]
            (if (= result -1) nil result)
        )
    )
)

;;;
 ; Return last index of value (string or char) in s, optionally
 ; searching backward from from-index. Return nil if value not found.
 ;;
(§ defn last-index-of
    ([^CharSequence s value]
        (let [result ^long
                (if (instance? Character value)
                    (.lastIndexOf (.toString s) ^int (.charValue ^Character value))
                    (.lastIndexOf (.toString s) ^String value)
                )]
            (if (= result -1) nil result)
        )
    )
    ([^CharSequence s value ^long from-index]
        (let [result ^long
                (if (instance? Character value)
                    (.lastIndexOf (.toString s) ^int (.charValue ^Character value) (unchecked-int from-index))
                    (.lastIndexOf (.toString s) ^String value (unchecked-int from-index))
                )]
            (if (= result -1) nil result)
        )
    )
)

;;;
 ; True if s starts with substr.
 ;;
(§ defn starts-with? [^CharSequence s ^String substr] (.startsWith (.toString s) substr))

;;;
 ; True if s ends with substr.
 ;;
(§ defn ends-with? [^CharSequence s ^String substr] (.endsWith (.toString s) substr))

;;;
 ; True if s includes substr.
 ;;
(§ defn includes? [^CharSequence s ^CharSequence substr] (.contains (.toString s) substr))

;;;
 ; This namespace defines a generic tree walker for Cloiure data structures.
 ; It takes any data structure (list, vector, map, set, seq), calls a function
 ; on every element, and uses the return value of the function in place of the
 ; original. This makes it fairly easy to write recursive search-and-replace
 ; functions, as shown in the examples.
 ;
 ; Note: "walk" supports all Cloiure data structures EXCEPT maps created with
 ; sorted-map-by. There is no (obvious) way to retrieve the sorting function.
 ;;
#_(ns cloiure.walk)

;;;
 ; Traverses form, an arbitrary data structure. inner and outer are functions.
 ; Applies inner to each element of form, building up a data structure of the
 ; same type, then applies outer to the result. Recognizes all Cloiure data
 ; structures. Consumes seqs as with doall.
 ;;
(§ defn walk [inner outer form]
    (cond
        (list? form)                            (outer (apply list (map inner form)))
        (instance? cloiure.lang.IMapEntry form) (outer (vec (map inner form)))
        (seq? form)                             (outer (doall (map inner form)))
        (coll? form)                            (outer (into (empty form) (map inner form)))
        :else                                   (outer form)
    )
)

;;;
 ; Performs a depth-first, post-order traversal of form. Calls f on
 ; each sub-form, uses f's return value in place of the original.
 ; Recognizes all Cloiure data structures. Consumes seqs as with doall.
 ;;
(§ defn postwalk [f form] (walk (partial postwalk f) f form))

;;;
 ; Like postwalk, but does pre-order traversal.
 ;;
(§ defn prewalk [f form] (walk (partial prewalk f) identity (f form)))

;; Note: I wanted to write:
;;
;; (defn walk [f form]
;;  (let [pf (partial walk f)]
;;   (if (coll? form)
;;    (f (into (empty form) (map pf form)))
;;    (f form))))
;;
;; but this throws a ClassCastException when applied to a map.

;;;
 ; Demonstrates the behavior of postwalk by printing each form as it is
 ; walked. Returns form.
 ;;
(§ defn postwalk-demo [form]
    (postwalk (fn [x] (print "Walked: ") (prn x) x) form)
)

;;;
 ; Demonstrates the behavior of prewalk by printing each form as it is
 ; walked. Returns form.
 ;;
(§ defn prewalk-demo [form]
    (prewalk (fn [x] (print "Walked: ") (prn x) x) form)
)

;;;
 ; Recursively transforms all map keys from strings to keywords.
 ;;
(§ defn keywordize-keys [m]
    (let [f (fn [[k v]] (if (string? k) [(keyword k) v] [k v]))]
        ;; only apply to maps
        (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)
    )
)

;;;
 ; Recursively transforms all map keys from keywords to strings.
 ;;
(§ defn stringify-keys [m]
    (let [f (fn [[k v]] (if (keyword? k) [(name k) v] [k v]))]
        ;; only apply to maps
        (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)
    )
)

;;;
 ; Recursively transforms form by replacing keys in smap with their
 ; values. Like cloiure/replace but works on any data structure. Does
 ; replacement at the root of the tree first.
 ;;
(§ defn prewalk-replace [smap form]
    (prewalk (fn [x] (if (contains? smap x) (smap x) x)) form)
)

;;;
 ; Recursively transforms form by replacing keys in smap with their
 ; values. Like cloiure/replace but works on any data structure. Does
 ; replacement at the leaves of the tree first.
 ;;
(§ defn postwalk-replace [smap form]
    (postwalk (fn [x] (if (contains? smap x) (smap x) x)) form)
)

;;;
 ; Recursively performs all possible macroexpansions in form.
 ;;
(§ defn macroexpand-all [form]
    (prewalk (fn [x] (if (seq? x) (macroexpand x) x)) form)
)

#_(ns cloiure.core.reducers
    (:refer-cloiure :exclude [reduce map mapcat filter remove take take-while drop flatten cat])
    (:require [cloiure.walk :as walk]))

(§ alias 'core 'cloiure.core)

(§ def pool (delay (java.util.concurrent.ForkJoinPool.)))

(§ defn fjtask [^Callable f]
    (java.util.concurrent.ForkJoinTask/adapt f)
)

(§ defn- fjinvoke [f]
    (if (java.util.concurrent.ForkJoinTask/inForkJoinPool)
        (f)
        (.invoke ^java.util.concurrent.ForkJoinPool @pool ^java.util.concurrent.ForkJoinTask (fjtask f))
    )
)

(§ defn- fjfork [task] (.fork ^java.util.concurrent.ForkJoinTask task))

(§ defn- fjjoin [task] (.join ^java.util.concurrent.ForkJoinTask task))

;;;
 ; Like core/reduce except:
 ; - when init is not provided, (f) is used;
 ; - maps are reduced with reduce-kv.
 ;;
(§ defn reduce
    ([f coll] (reduce f (f) coll))
    ([f init coll]
        (if (instance? java.util.Map coll)
            (cloiure.core.protocols/kv-reduce coll f init)
            (cloiure.core.protocols/coll-reduce coll f init)
        )
    )
)

(§ defprotocol CollFold
    (coll-fold [coll n combinef reducef])
)

;;;
 ; Reduces a collection using a (potentially parallel) reduce-combine
 ; strategy. The collection is partitioned into groups of approximately
 ; n (default 512), each of which is reduced with reducef (with a seed
 ; value obtained by calling (combinef) with no arguments). The results
 ; of these reductions are then reduced with combinef (default reducef).
 ; combinef must be associative, and, when called with no arguments,
 ; (combinef) must produce its identity element. These operations may
 ; be performed in parallel, but the results will preserve order.
 ;;
(§ defn fold
    ([reducef coll] (fold reducef reducef coll))
    ([combinef reducef coll] (fold 512 combinef reducef coll))
    ([n combinef reducef coll] (coll-fold coll n combinef reducef))
)

;;;
 ; Given a reducible collection, and a transformation function xf,
 ; returns a reducible collection, where any supplied reducing fn will
 ; be transformed by xf. xf is a function of reducing fn to reducing fn.
 ;;
(§ defn reducer
    ([coll xf]
        (reify
            cloiure.core.protocols/CollReduce
            (coll-reduce [this f1] (cloiure.core.protocols/coll-reduce this f1 (f1)))
            (coll-reduce [_ f1 init] (cloiure.core.protocols/coll-reduce coll (xf f1) init))
        )
    )
)

;;;
 ; Given a foldable collection, and a transformation function xf,
 ; returns a foldable collection, where any supplied reducing fn will
 ; be transformed by xf. xf is a function of reducing fn to reducing fn.
 ;;
(§ defn folder
    ([coll xf]
        (reify
            cloiure.core.protocols/CollReduce
            (coll-reduce [_ f1] (cloiure.core.protocols/coll-reduce coll (xf f1) (f1)))
            (coll-reduce [_ f1 init] (cloiure.core.protocols/coll-reduce coll (xf f1) init))

            CollFold
            (coll-fold [_ n combinef reducef] (coll-fold coll n combinef (xf reducef)))
        )
    )
)

(§ defn- do-curried [name doc meta args body]
    (let [cargs (vec (butlast args))]
        `(defn ~name ~doc ~meta
            (~cargs (fn [x#] (~name ~@cargs x#)))
            (~args ~@body)
        )
    )
)

;;;
 ; Builds another arity of the fn that returns a fn awaiting the last param.
 ;;
(§ defmacro ^:private defcurried [name doc meta args & body]
    (do-curried name doc meta args body)
)

(§ defn- do-rfn [f1 k fkv]
    `(fn
        ([] (~f1))
        ~(cloiure.walk/postwalk
            #(if (sequential? %)
                ((if (vector? %) vec identity) (core/remove #{k} %))
                %
            )
            fkv
        )
        ~fkv
    )
)

;;;
 ; Builds 3-arity reducing fn given names of wrapped fn and key, and k/v impl.
 ;;
(§ defmacro ^:private rfn [[f1 k] fkv]
    (do-rfn f1 k fkv)
)

;;;
 ; Applies f to every value in the reduction of coll. Foldable.
 ;;
(§ defcurried map [f coll]
    (folder coll
        (fn [f1]
            (rfn [f1 k]
                ([ret k v]
                    (f1 ret (f k v))
                )
            )
        )
    )
)

;;;
 ; Applies f to every value in the reduction of coll, concatenating
 ; the result colls of (f val). Foldable.
 ;;
(§ defcurried mapcat [f coll]
    (folder coll
        (fn [f1]
            (let [f1
                    (fn
                        ([ret   v] (let [x (f1 ret   v)] (if (reduced? x) (reduced x) x)))
                        ([ret k v] (let [x (f1 ret k v)] (if (reduced? x) (reduced x) x)))
                    )]
                (rfn [f1 k]
                    ([ret k v]
                        (reduce f1 ret (f k v))
                    )
                )
            )
        )
    )
)

;;;
 ; Retains values in the reduction of coll for which (pred val)
 ; returns logical true. Foldable.
 ;;
(§ defcurried filter [pred coll]
    (folder coll
        (fn [f1]
            (rfn [f1 k]
                ([ret k v]
                    (if (pred k v)
                        (f1 ret k v)
                        ret
                    )
                )
            )
        )
    )
)

;;;
 ; Removes values in the reduction of coll for which (pred val)
 ; returns logical true. Foldable.
 ;;
(§ defcurried remove [pred coll]
    (filter (complement pred) coll)
)

;;;
 ; Takes any nested combination of sequential things (lists, vectors, etc.)
 ; and returns their contents as a single, flat foldable collection.
 ;;
(§ defcurried flatten [coll]
    (folder coll
        (fn [f1]
            (fn
                ([] (f1))
                ([ret v]
                    (if (sequential? v)
                        (cloiure.core.protocols/coll-reduce (flatten v) f1 ret)
                        (f1 ret v)
                    )
                )
            )
        )
    )
)

;;;
 ; Ends the reduction of coll when (pred val) returns logical false.
 ;;
(§ defcurried take-while [pred coll]
    (reducer coll
        (fn [f1]
            (rfn [f1 k]
                ([ret k v]
                    (if (pred k v)
                        (f1 ret k v)
                        (reduced ret)
                    )
                )
            )
        )
    )
)

;;;
 ; Ends the reduction of coll after consuming n values.
 ;;
(§ defcurried take [n coll]
    (reducer coll
        (fn [f1]
            (let [cnt (atom n)]
                (rfn [f1 k]
                    ([ret k v]
                        (swap! cnt dec)
                        (if (neg? @cnt)
                            (reduced ret)
                            (f1 ret k v)
                        )
                    )
                )
            )
        )
    )
)

;;;
 ; Elides the first n values from the reduction of coll.
 ;;
(§ defcurried drop [n coll]
    (reducer coll
        (fn [f1]
            (let [cnt (atom n)]
                (rfn [f1 k]
                    ([ret k v]
                        (swap! cnt dec)
                        (if (neg? @cnt)
                            (f1 ret k v)
                            ret
                        )
                    )
                )
            )
        )
    )
)

;; do not construct this directly, use cat

(§ deftype Cat [cnt left right]
    cloiure.lang.Counted
    (count [_] cnt)

    cloiure.lang.Seqable
    (seq [_] (concat (seq left) (seq right)))

    cloiure.core.protocols/CollReduce
    (coll-reduce [this f1]
        (cloiure.core.protocols/coll-reduce this f1 (f1))
    )
    (coll-reduce [_ f1 init]
        (cloiure.core.protocols/coll-reduce right f1 (cloiure.core.protocols/coll-reduce left f1 init))
    )

    CollFold
    (coll-fold [_ n combinef reducef]
        (fjinvoke
            (fn []
                (let [rt (fjfork (fjtask #(coll-fold right n combinef reducef)))]
                    (combinef
                        (coll-fold left n combinef reducef)
                        (fjjoin rt)
                    )
                )
            )
        )
    )
)

;;;
 ; A high-performance combining fn that yields the catenation of the reduced values.
 ; The result is reducible, foldable, seqable and counted, providing the identity
 ; collections are reducible, seqable and counted. The single argument version will
 ; build a combining fn with the supplied identity constructor. Tests for identity
 ; with (zero? (count x)). See also foldcat.
 ;;
(§ defn cat
    ([] (java.util.ArrayList.))
    ([ctor]
        (fn
            ([] (ctor))
            ([left right] (cat left right))
        )
    )
    ([left right]
        (cond
            (zero? (count left)) right
            (zero? (count right)) left
            :else (Cat. (+ (count left) (count right)) left right)
        )
    )
)

;;;
 ; .adds x to acc and returns acc.
 ;;
(§ defn append! [^java.util.Collection acc x] (doto acc (.add x)))

;;;
 ; Equivalent to (fold cat append! coll).
 ;;
(§ defn foldcat [coll] (fold cat append! coll))

;;;
 ; Builds a combining fn out of the supplied operator and identity
 ; constructor. op must be associative and ctor called with no args
 ; must return an identity value for it.
 ;;
(§ defn monoid [op ctor] (fn m ([] (ctor)) ([a b] (op a b))))

(§ defn- foldvec [v n combinef reducef]
    (cond
        (empty? v)
            (combinef)
        (<= (count v) n)
            (reduce reducef (combinef) v)
        :else
            (let [split (quot (count v) 2) v1 (subvec v 0 split) v2 (subvec v split (count v))
                  fc (fn [child] #(foldvec child n combinef reducef))]
                (fjinvoke
                    #(let [f1 (fc v1) t2 (fjtask (fc v2))]
                        (fjfork t2)
                        (combinef (f1) (fjjoin t2))
                    )
                )
            )
    )
)

(§ extend-protocol CollFold
    nil
    (coll-fold [coll n combinef reducef] (combinef))

    Object
    (coll-fold [coll n combinef reducef] (reduce reducef (combinef) coll)) ;; can't fold, single reduce

    cloiure.lang.IPersistentVector
    (coll-fold [v n combinef reducef] (foldvec v n combinef reducef))

    cloiure.lang.PersistentHashMap
    (coll-fold [m n combinef reducef] (.fold m n combinef reducef fjinvoke fjtask fjfork fjjoin))
)

#_(ns cloiure.stacktrace)

;;;
 ; Returns the last 'cause' Throwable in a chain of Throwables.
 ;;
(§ defn root-cause [tr] (if-let [cause (.getCause tr)] (recur cause) tr))

;;;
 ; Prints a Cloiure-oriented view of one element in a stack trace.
 ;;
(§ defn print-trace-element [e]
    (let [class (.getClassName e) method (.getMethodName e)
          match (re-matches #"^([A-Za-z0-9_.-]+)\$(\w+)__\d+$" (str class))]
        (if (and match (= "invoke" method))
            (apply printf "%s/%s" (rest match))
            (printf "%s.%s" class method)
        )
    )
    (printf " (%s:%d)" (or (.getFileName e) "") (.getLineNumber e))
)

;;;
 ; Prints the class and message of a Throwable.
 ;;
(§ defn print-throwable [tr] (printf "%s: %s" (.getName (class tr)) (.getMessage tr)))

;;;
 ; Prints a Cloiure-oriented stack trace of tr, a Throwable.
 ; Prints a maximum of n stack frames (default: unlimited).
 ; Does not print chained exceptions (causes).
 ;;
(§ defn print-stack-trace
    ([tr] (print-stack-trace tr nil))
    ([^Throwable tr n]
        (let [st (.getStackTrace tr)]
            (print-throwable tr)
            (newline)
            (print " at ")
            (if-let [e (first st)]
                (print-trace-element e)
                (print "[empty stack trace]")
            )
            (newline)
            (doseq [e (if (nil? n) (rest st) (take (dec n) (rest st)))]
                (print "    ")
                (print-trace-element e)
                (newline)
            )
        )
    )
)

;;;
 ; Like print-stack-trace but prints chained exceptions (causes).
 ;;
(§ defn print-cause-trace
    ([tr] (print-cause-trace tr nil))
    ([tr n]
        (print-stack-trace tr n)
        (when-let [cause (.getCause tr)]
            (print "Caused by: " )
            (recur cause n)
        )
    )
)

;;;
 ; REPL utility. Prints a brief stack trace for the root cause of the
 ; most recent exception.
 ;;
(§ defn e []
    (print-stack-trace (root-cause *e) 8)
)

#_(ns cloiure.repl
    (:import [java.io LineNumberReader InputStreamReader PushbackReader]
             [cloiure.lang RT Reflector]))

(§ def ^:private special-doc-map
    (hash-map
        '.              {:forms ['(.instanceMember instance args*) '(.instanceMember Classname args*) '(Classname/staticMethod args*) 'Classname/staticField]
                         :doc "The instance member form works for both fields and methods.
                               They all expand into calls to the dot operator at macroexpansion time."}
        'def            {:forms ['(def symbol doc-string? init?)]
                         :doc "Creates and interns a global var with the name of symbol in the current namespace (*ns*) or locates such a var if
                               it already exists. If init is supplied, it is evaluated, and the root binding of the var is set to the resulting value.
                               If init is not supplied, the root binding of the var is unaffected."}
        'do             {:forms ['(do exprs*)]
                         :doc "Evaluates the expressions in order and returns the value of the last. If no expressions are supplied, returns nil."}
        'if             {:forms ['(if test then else?)]
                         :doc "Evaluates test. If not the singular values nil or false,
                               evaluates and yields then, otherwise, evaluates and yields else.
                               If else is not supplied it defaults to nil."}
        'monitor-enter  {:forms ['(monitor-enter x)]
                         :doc "Synchronization primitive that should be avoided in user code. Use the 'locking' macro."}
        'monitor-exit   {:forms ['(monitor-exit x)]
                         :doc "Synchronization primitive that should be avoided in user code. Use the 'locking' macro."}
        'new            {:forms ['(Classname. args*) '(new Classname args*)]
                         :doc "The args, if any, are evaluated from left to right, and passed to the constructor of the class named by Classname.
                               The constructed object is returned."}
        'quote          {:forms ['(quote form)]
                         :doc "Yields the unevaluated form."}
        'recur          {:forms ['(recur exprs*)]
                         :doc "Evaluates the exprs in order, then, in parallel, rebinds the bindings of the recursion point to the values of the exprs.
                               Execution then jumps back to the recursion point, a loop or fn method."}
        'set!           {:forms ['(set! var-symbol expr) '(set! (. instance-expr instanceFieldName-symbol) expr) '(set! (. Classname-symbol staticFieldName-symbol) expr)]
                         :doc "Used to set thread-local-bound vars, Java object instance fields, and Java class static fields."}
        'throw          {:forms ['(throw expr)]
                         :doc "The expr is evaluated and thrown, therefore it should yield an instance of some derivee of Throwable."}
        'try            {:forms ['(try expr* catch-clause* finally-clause?)]
                         :doc "catch-clause => (catch classname name expr*)
                               finally-clause => (finally expr*)
                               Catches and handles Java exceptions."}
        'var            {:forms ['(var symbol)]
                         :doc "The symbol must resolve to a var, and the Var object itself (not its value) is returned.
                               The reader macro #'x expands to (var x)."}
    )
)

(§ defn- special-doc [name-symbol]
    (assoc (or (special-doc-map name-symbol) (meta (resolve name-symbol))) :name name-symbol :special-form true)
)

(§ defn- namespace-doc [nspace]
    (assoc (meta nspace) :name (ns-name nspace))
)

(§ defn- print-doc [{n :ns nm :name :keys [forms arglists special-form doc macro] :as m}]
    (println "-------------------------")
    (println (str (when n (str (ns-name n) "/")) nm))
    (when forms
        (doseq [f forms]
            (print "  ")
            (prn f)
        )
    )
    (when arglists
        (prn arglists)
    )
    (cond
        special-form (println "Special Form")
        macro        (println "Macro")
    )
    (when doc
        (println " " doc)
    )
)

;;;
 ; Prints documentation for a var or special form given its name.
 ;;
(§ defmacro doc [name]
    (if-let [special-name ('{& fn catch try finally try} name)]
        `(#'print-doc (#'special-doc '~special-name))
        (cond
            (special-doc-map name) `(#'print-doc (#'special-doc '~name))
            (find-ns name)         `(#'print-doc (#'namespace-doc (find-ns '~name)))
            (resolve name)         `(#'print-doc (meta (var ~name)))
        )
    )
)

;;;
 ; Given a string representation of a fn class,
 ; as in a stack trace element, returns a readable version.
 ;;
(§ defn demunge [fn-name] (cloiure.lang.Compiler/demunge fn-name))

;;;
 ; Returns the initial cause of an exception or error by peeling off all of its wrappers.
 ;;
(§ defn root-cause [^Throwable t]
    (loop [cause t]
        (if-let [cause (.getCause cause)]
            (recur cause)
            cause
        )
    )
)

;;;
 ; Returns a (possibly unmunged) string representation of a StackTraceElement.
 ;;
(§ defn stack-element-str [^StackTraceElement el]
    (let [cloiure-fn? true]
        (str
            (if cloiure-fn?
                (demunge (.getClassName el))
                (str (.getClassName el) "." (.getMethodName el))
            )
            " (" (.getFileName el) ":" (.getLineNumber el) ")"
        )
    )
)

;;;
 ; Prints a stack trace of the exception, to the depth requested. If none supplied,
 ; uses the root cause of the most recent repl exception (*e), and a depth of 12.
 ;;
(§ defn pst
    ([] (pst 12))
    ([e-or-depth]
        (if (instance? Throwable e-or-depth)
            (pst e-or-depth 12)
            (when-let [e *e]
                (pst (root-cause e) e-or-depth)
            )
        )
    )
    ([^Throwable e depth]
        (binding [*out* *err*]
            (println (str (-> e class .getSimpleName) " " (.getMessage e) (when-let [info (ex-data e)] (str " " (pr-str info)))))
            (let [st (.getStackTrace e) cause (.getCause e)]
                (doseq [el (take depth (remove #(#{"cloiure.lang.RestFn" "cloiure.lang.AFn"} (.getClassName %)) st))]
                    (println (str \tab (stack-element-str el)))
                )
                (when cause
                    (println "Caused by:")
                    (pst cause (min depth (+ 2 (- (count (.getStackTrace cause)) (count st)))))
                )
            )
        )
    )
)

;;;
 ; Returns a function that takes one arg and uses that as an exception message
 ; to stop the given thread. Defaults to the current thread.
 ;;
(§ defn thread-stopper
    ([] (thread-stopper (Thread/currentThread)))
    ([thread] (fn [msg] (.stop thread (Error. msg))))
)

;;;
 ; Register INT signal handler. After calling this, Ctrl-C will cause
 ; the given function f to be called with a single argument, the signal.
 ; Uses thread-stopper if no function given.
 ;;
(§ defn set-break-handler!
    ([] (set-break-handler! (thread-stopper)))
    ([f]
        (sun.misc.Signal/handle
            (sun.misc.Signal. "INT")
            (proxy [sun.misc.SignalHandler] []
                (handle [signal] (f (str "-- caught signal " signal)))
            )
        )
    )
)

#_(ns cloiure.main
    (:refer-cloiure :exclude [with-bindings])
    (:import [cloiure.lang Compiler Compiler$CompilerException LineNumberingPushbackReader RT])
;;  (:use [cloiure.repl :only [demunge root-cause stack-element-str]])
)

(§ declare main)

;; redundantly copied from cloiure.repl to avoid dep

;;;
 ; Given a string representation of a fn class,
 ; as in a stack trace element, returns a readable version.
 ;;
(§ defn demunge [fn-name] (cloiure.lang.Compiler/demunge fn-name))

;;;
 ; Returns the initial cause of an exception or error by peeling off all of its wrappers.
 ;;
(§ defn root-cause [^Throwable t]
    (loop [cause t]
        (if-let [cause (.getCause cause)]
            (recur cause)
            cause
        )
    )
)

;;;
 ; Returns a (possibly unmunged) string representation of a StackTraceElement.
 ;;
(§ defn stack-element-str [^StackTraceElement el]
    (let [cloiure-fn? true]
        (str
            (if cloiure-fn?
                (demunge (.getClassName el))
                (str (.getClassName el) "." (.getMethodName el))
            )
            " (" (.getFileName el) ":" (.getLineNumber el) ")"
        )
    )
)

;; end of redundantly copied from cloiure.repl to avoid dep

;;;
 ; Executes body in the context of thread-local bindings for several vars that often need to be set!.
 ;;
(§ defmacro with-bindings [& body]
    `(binding [*ns* *ns*
               *warn-on-reflection* *warn-on-reflection*
               *math-context* *math-context*
               *print-length* *print-length*
               *print-level* *print-level*
               *print-namespace-maps* true
               *assert* *assert*
               *1 nil
               *2 nil
               *3 nil
               *e nil]
        ~@body
    )
)

;;;
 ; Default :prompt hook for repl.
 ;;
(§ defn repl-prompt [] (printf "%s=> " (ns-name *ns*)))

;;;
 ; If the next character on stream s is a newline, skips it, otherwise leaves
 ; the stream untouched. Returns :line-start, :stream-end, or :body to indicate
 ; the relative location of the next character on s. The stream must either be
 ; an instance of LineNumberingPushbackReader or duplicate its behavior of both
 ; supporting .unread and collapsing all of CR, LF, and CRLF to a single \newline.
 ;;
(§ defn skip-if-eol [s]
    (let [c (.read s)]
        (cond
            (= c (int \newline)) :line-start
            (= c -1) :stream-end
            :else (do (.unread s c) :body)
        )
    )
)

;;;
 ; Skips whitespace characters on stream s. Returns :line-start, :stream-end,
 ; or :body to indicate the relative location of the next character on s.
 ; Interprets comma as whitespace and semicolon as comment to end of line.
 ; Does not interpret #! as comment to end of line because only one character
 ; of lookahead is available. The stream must either be an instance of
 ; LineNumberingPushbackReader or duplicate its behavior of both supporting
 ; .unread and collapsing all of CR, LF, and CRLF to a single \newline.
 ;;
(§ defn skip-whitespace [s]
    (loop [c (.read s)]
        (cond
            (= c (int \newline)) :line-start
            (= c -1) :stream-end
            (= c (int \;)) (do (.readLine s) :line-start)
            (or (Character/isWhitespace (char c)) (= c (int \,))) (recur (.read s))
            :else (do (.unread s c) :body)
        )
    )
)

;;;
 ; Default :read hook for repl. Reads from *in* which must either be an instance
 ; of LineNumberingPushbackReader or duplicate its behavior of both supporting
 ; .unread and collapsing all of CR, LF, and CRLF into a single \newline.
 ; repl-read:
 ; - skips whitespace, then
 ; - returns request-prompt on start of line, or
 ; - returns request-exit on end of stream, or
 ; - reads an object from the input stream, then
 ; - skips the next input character if it's end of line, then
 ; - returns the object.
 ;;
(§ defn repl-read [request-prompt request-exit]
    (or ({:line-start request-prompt :stream-end request-exit} (skip-whitespace *in*))
        (let [input (read *in*)]
            (skip-if-eol *in*)
            input
        )
    )
)

;;;
 ; Returns the root cause of throwables.
 ;;
(§ defn repl-exception [throwable] (root-cause throwable))

;;;
 ; Default :caught hook for repl.
 ;;
(§ defn repl-caught [e]
    (let [ex (repl-exception e) tr (.getStackTrace ex) el (when-not (zero? (count tr)) (aget tr 0))]
        (binding [*out* *err*]
            (println
                (str (-> ex class .getSimpleName) " " (.getMessage ex) " "
                    (when-not (instance? cloiure.lang.Compiler$CompilerException ex)
                        (str " " (if el (stack-element-str el) "[trace missing]"))
                    )
                )
            )
        )
    )
)

;;;
 ; A sequence of lib specs that are applied to `require` by default when a new command-line REPL is started.
 ;;
(def repl-requires [['cloiure.repl :refer ['doc 'pst]]])

;;;
 ; Generic, reusable, read-eval-print loop. By default, reads from *in*, writes
 ; to *out*, and prints exception summaries to *err*. If you use the default
 ; :read hook, *in* must either be an instance of LineNumberingPushbackReader or
 ; duplicate its behavior of both supporting .unread and collapsing CR, LF, and
 ; CRLF into a single \newline. Options are sequential keyword-value pairs.
 ;
 ; Available options and their defaults:
 ;
 ; :init function of no arguments, initialization hook called with bindings
 ;       for set!-able vars in place.
 ;       default: #()
 ;
 ; :need-prompt function of no arguments, called before each read-eval-print
 ;              except the first, the user will be prompted if it returns true.
 ;              default: (if (instance? LineNumberingPushbackReader *in*)
 ;                        #(.atLineStart *in*)
 ;                        #(identity true))
 ;
 ; :prompt function of no arguments, prompts for more input.
 ;         default: repl-prompt
 ;
 ; :flush function of no arguments, flushes output.
 ;        default: flush
 ;
 ; :read function of two arguments, reads from *in*:
 ;       - returns its first argument to request a fresh prompt
 ;       - depending on need-prompt, this may cause the repl to prompt before reading again
 ;       - returns its second argument to request an exit from the repl
 ;       - else returns the next object read from the input stream
 ;       default: repl-read
 ;
 ; :eval function of one argument, returns the evaluation of its argument.
 ;       default: eval
 ;
 ; :print function of one argument, prints its argument to the output.
 ;        default: prn
 ;
 ; :caught function of one argument, a throwable, called when read, eval, or
 ;         print throws an exception or error.
 ;         default: repl-caught
 ;;
(§ defn repl [& options]
    (let [cl (.getContextClassLoader (Thread/currentThread))]
        (.setContextClassLoader (Thread/currentThread) (cloiure.lang.DynamicClassLoader. cl))
    )
    (let [{:keys [init need-prompt prompt flush read eval print caught]
           :or {init        #()
                need-prompt (if (instance? LineNumberingPushbackReader *in*) #(.atLineStart ^LineNumberingPushbackReader *in*) #(identity true))
                prompt      repl-prompt
                flush       flush
                read        repl-read
                eval        eval
                print       prn
                caught      repl-caught}
            } (apply hash-map options)
          request-prompt (Object.)
          request-exit (Object.)
          read-eval-print
            (fn []
                (try
                    (let [input (read request-prompt request-exit)]
                        (or (#{request-prompt request-exit} input)
                            (let [value (eval input)]
                                (print value)
                                (set! *3 *2)
                                (set! *2 *1)
                                (set! *1 value)
                            )
                        )
                    )
                    (catch Throwable e
                        (caught e)
                        (set! *e e)
                    )
                )
            )]
        (with-bindings (try (init) (catch Throwable e (caught e) (set! *e e)))
            (prompt)
            (flush)
            (loop []
                (when-not (try (identical? (read-eval-print) request-exit) (catch Throwable e (caught e) (set! *e e) nil))
                    (when (need-prompt)
                        (prompt)
                        (flush)
                    )
                    (recur)
                )
            )
        )
    )
)

(§ defn main [& _args]
    (try
        (repl :init (fn [] (in-ns 'user) (apply require repl-requires)))
        (prn)
        (finally
            (flush)
        )
    )
)
