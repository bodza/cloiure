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

#_(ns ^{:doc "The core Cloiure language."
       :author "Rich Hickey"}
  cloiure.core)

(§ def unquote)
(§ def unquote-splicing)

(§ def
 ^{:arglists '([& items])
   :doc "Creates a new list containing the items."
   :added "1.0"}
  list (. cloiure.lang.PersistentList creator))

(§ def
 ^{:arglists '([x seq])
    :doc "Returns a new seq where x is the first element and seq is
    the rest."
   :added "1.0"
   :static true}

 cons (fn* ^:static cons [x seq] (. cloiure.lang.RT (cons x seq))))

; during bootstrap we don't have destructuring let, loop or fn, will redefine later
(§ def
  ^{:macro true
    :added "1.0"}
  let (fn* let [&form &env & decl] (cons 'let* decl)))

(§ def
 ^{:macro true
   :added "1.0"}
 loop (fn* loop [&form &env & decl] (cons 'loop* decl)))

(§ def
 ^{:macro true
   :added "1.0"}
 fn (fn* fn [&form &env & decl]
         (.withMeta ^cloiure.lang.IObj (cons 'fn* decl)
                    (.meta ^cloiure.lang.IMeta &form))))

(§ def
 ^{:arglists '([coll])
   :doc "Returns the first item in the collection. Calls seq on its
    argument. If coll is nil, returns nil."
   :added "1.0"
   :static true}
 first (fn ^:static first [coll] (. cloiure.lang.RT (first coll))))

(§ def
 ^{:arglists '([coll])
   :tag cloiure.lang.ISeq
   :doc "Returns a seq of the items after the first. Calls seq on its
  argument.  If there are no more items, returns nil."
   :added "1.0"
   :static true}
 next (fn ^:static next [x] (. cloiure.lang.RT (next x))))

(§ def
 ^{:arglists '([coll])
   :tag cloiure.lang.ISeq
   :doc "Returns a possibly empty seq of the items after the first. Calls seq on its
  argument."
   :added "1.0"
   :static true}
 rest (fn ^:static rest [x] (. cloiure.lang.RT (more x))))

(§ def
 ^{:arglists '([coll x] [coll x & xs])
   :doc "conj[oin]. Returns a new collection with the xs
    'added'. (conj nil item) returns (item).  The 'addition' may
    happen at different 'places' depending on the concrete type."
   :added "1.0"
   :static true}
 conj (fn ^:static conj
        ([] [])
        ([coll] coll)
        ([coll x] (cloiure.lang.RT/conj coll x))
        ([coll x & xs]
         (if xs
           (recur (cloiure.lang.RT/conj coll x) (first xs) (next xs))
           (cloiure.lang.RT/conj coll x)))))

(§ def
 ^{:doc "Same as (first (next x))"
   :arglists '([x])
   :added "1.0"
   :static true}
 second (fn ^:static second [x] (first (next x))))

(§ def
 ^{:doc "Same as (first (first x))"
   :arglists '([x])
   :added "1.0"
   :static true}
 ffirst (fn ^:static ffirst [x] (first (first x))))

(§ def
 ^{:doc "Same as (next (first x))"
   :arglists '([x])
   :added "1.0"
   :static true}
 nfirst (fn ^:static nfirst [x] (next (first x))))

(§ def
 ^{:doc "Same as (first (next x))"
   :arglists '([x])
   :added "1.0"
   :static true}
 fnext (fn ^:static fnext [x] (first (next x))))

(§ def
 ^{:doc "Same as (next (next x))"
   :arglists '([x])
   :added "1.0"
   :static true}
 nnext (fn ^:static nnext [x] (next (next x))))

(§ def
 ^{:arglists '(^cloiure.lang.ISeq [coll])
   :doc "Returns a seq on the collection. If the collection is
    empty, returns nil.  (seq nil) returns nil. seq also works on
    Strings, native Java arrays (of reference types) and any objects
    that implement Iterable. Note that seqs cache values, thus seq
    should not be used on any Iterable whose iterator repeatedly
    returns the same mutable object."
   :tag cloiure.lang.ISeq
   :added "1.0"
   :static true}
 seq (fn ^:static seq ^cloiure.lang.ISeq [coll] (. cloiure.lang.RT (seq coll))))

(§ def
 ^{:arglists '([^Class c x])
   :doc "Evaluates x and tests if it is an instance of the class
    c. Returns true or false"
   :added "1.0"}
 instance? (fn instance? [^Class c x] (. c (isInstance x))))

(§ def
 ^{:arglists '([x])
   :doc "Return true if x implements ISeq"
   :added "1.0"
   :static true}
 seq? (fn ^:static seq? [x] (instance? cloiure.lang.ISeq x)))

(§ def
 ^{:arglists '([x])
   :doc "Return true if x is a Character"
   :added "1.0"
   :static true}
 char? (fn ^:static char? [x] (instance? Character x)))

(§ def
 ^{:arglists '([x])
   :doc "Return true if x is a String"
   :added "1.0"
   :static true}
 string? (fn ^:static string? [x] (instance? String x)))

(§ def
 ^{:arglists '([x])
   :doc "Return true if x implements IPersistentMap"
   :added "1.0"
   :static true}
 map? (fn ^:static map? [x] (instance? cloiure.lang.IPersistentMap x)))

(§ def
 ^{:arglists '([x])
   :doc "Return true if x implements IPersistentVector"
   :added "1.0"
   :static true}
 vector? (fn ^:static vector? [x] (instance? cloiure.lang.IPersistentVector x)))

(§ def
 ^{:arglists '([map key val] [map key val & kvs])
   :doc "assoc[iate]. When applied to a map, returns a new map of the
    same (hashed/sorted) type, that contains the mapping of key(s) to
    val(s). When applied to a vector, returns a new vector that
    contains val at index. Note - index must be <= (count vector)."
   :added "1.0"
   :static true}
 assoc
 (fn ^:static assoc
   ([map key val] (cloiure.lang.RT/assoc map key val))
   ([map key val & kvs]
    (let [ret (cloiure.lang.RT/assoc map key val)]
      (if kvs
        (if (next kvs)
          (recur ret (first kvs) (second kvs) (nnext kvs))
          (throw (IllegalArgumentException.
                  "assoc expects even number of arguments after map/vector, found odd number")))
        ret)))))

;;;;;;;;;;;;;;;;; metadata ;;;;;;;;;;;;;;;;;;;;;;;;;;;
(§ def
 ^{:arglists '([obj])
   :doc "Returns the metadata of obj, returns nil if there is no metadata."
   :added "1.0"
   :static true}
 meta (fn ^:static meta [x]
        (if (instance? cloiure.lang.IMeta x)
          (. ^cloiure.lang.IMeta x (meta)))))

(§ def
 ^{:arglists '([^cloiure.lang.IObj obj m])
   :doc "Returns an object of the same type and value as obj, with
    map m as its metadata."
   :added "1.0"
   :static true}
 with-meta (fn ^:static with-meta [^cloiure.lang.IObj x m]
             (. x (withMeta m))))

(§ def ^{:private true :dynamic true}
  assert-valid-fdecl (fn [fdecl]))

(§ def
 ^{:private true}
 sigs
 (fn [fdecl]
   (assert-valid-fdecl fdecl)
   (let [asig
         (fn [fdecl]
           (let [arglist (first fdecl)
                 ;; elide implicit macro args
                 arglist (if (cloiure.lang.Util/equals '&form (first arglist))
                           (cloiure.lang.RT/subvec arglist 2 (cloiure.lang.RT/count arglist))
                           arglist)
                 body (next fdecl)]
             (if (map? (first body))
               (if (next body)
                 (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                 arglist)
               arglist)))
         resolve-tag (fn [argvec]
                        (let [m (meta argvec)
                              ^cloiure.lang.Symbol tag (:tag m)]
                          (if (instance? cloiure.lang.Symbol tag)
                            (if (cloiure.lang.Util/equiv (.indexOf (.getName tag) ".") -1)
                              (if (cloiure.lang.Util/equals nil (cloiure.lang.Compiler$HostExpr/maybeSpecialTag tag))
                                (let [c (cloiure.lang.Compiler$HostExpr/maybeClass tag false)]
                                  (if c
                                    (with-meta argvec (assoc m :tag (cloiure.lang.Symbol/intern (.getName c))))
                                    argvec))
                                argvec)
                              argvec)
                            argvec)))]
     (if (seq? (first fdecl))
       (loop [ret [] fdecls fdecl]
         (if fdecls
           (recur (conj ret (resolve-tag (asig (first fdecls)))) (next fdecls))
           (seq ret)))
       (list (resolve-tag (asig fdecl)))))))

(§ def
 ^{:arglists '([coll])
   :doc "Return the last item in coll, in linear time"
   :added "1.0"
   :static true}
 last (fn ^:static last [s]
        (if (next s)
          (recur (next s))
          (first s))))

(§ def
 ^{:arglists '([coll])
   :doc "Return a seq of all but the last item in coll, in linear time"
   :added "1.0"
   :static true}
 butlast (fn ^:static butlast [s]
           (loop [ret [] s s]
             (if (next s)
               (recur (conj ret (first s)) (next s))
               (seq ret)))))

(§ def

 ^{:doc "Same as (def name (fn [params* ] exprs*)) or (def
    name (fn ([params* ] exprs*)+)) with any doc-string or attrs added
    to the var metadata. prepost-map defines a map with optional keys
    :pre and :post that contain collections of pre or post conditions."
   :arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                [name doc-string? attr-map? ([params*] prepost-map? body)+ attr-map?])
   :added "1.0"}
 defn (fn defn [&form &env name & fdecl]
        ;; Note: Cannot delegate this check to def because of the call to (with-meta name ..)
        (if (instance? cloiure.lang.Symbol name)
          nil
          (throw (IllegalArgumentException. "First argument to defn must be a symbol")))
        (let [m (if (string? (first fdecl))
                  {:doc (first fdecl)}
                  {})
              fdecl (if (string? (first fdecl))
                      (next fdecl)
                      fdecl)
              m (if (map? (first fdecl))
                  (conj m (first fdecl))
                  m)
              fdecl (if (map? (first fdecl))
                      (next fdecl)
                      fdecl)
              fdecl (if (vector? (first fdecl))
                      (list fdecl)
                      fdecl)
              m (if (map? (last fdecl))
                  (conj m (last fdecl))
                  m)
              fdecl (if (map? (last fdecl))
                      (butlast fdecl)
                      fdecl)
              m (conj {:arglists (list 'quote (sigs fdecl))} m)
              m (let [inline (:inline m)
                      ifn (first inline)
                      iname (second inline)]
                  ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
                  (if (if (cloiure.lang.Util/equiv 'fn ifn)
                        (if (instance? cloiure.lang.Symbol iname) false true))
                    ;; inserts the same fn name to the inline fn if it does not have one
                    (assoc m :inline (cons ifn (cons (cloiure.lang.Symbol/intern (.concat (.getName ^cloiure.lang.Symbol name) "__inliner"))
                                                     (next inline))))
                    m))
              m (conj (if (meta name) (meta name) {}) m)]
          (list 'def (with-meta name m)
                ;; todo - restore propagation of fn name
                ;; must figure out how to convey primitive hints to self calls first
                                                                ;; (cons `fn fdecl)
                                                                (with-meta (cons `fn fdecl) {:rettag (:tag m)})))))

(§ . (var defn) (setMacro))

(§ defn to-array
  "Returns an array of Objects containing the contents of coll, which
  can be any Collection.  Maps to java.util.Collection.toArray()."
  {:tag "[Ljava.lang.Object;"
   :added "1.0"
   :static true}
  [coll] (. cloiure.lang.RT (toArray coll)))

(§ defn cast
  "Throws a ClassCastException if x is not a c, else returns x."
  {:added "1.0"
   :static true}
  [^Class c x]
  (. c (cast x)))

(§ defn vector
  "Creates a new vector containing the args."
  {:added "1.0"
   :static true}
  ([] [])
  ([a] [a])
  ([a b] [a b])
  ([a b c] [a b c])
  ([a b c d] [a b c d])
        ([a b c d e] [a b c d e])
        ([a b c d e f] [a b c d e f])
  ([a b c d e f & args]
     (. cloiure.lang.LazilyPersistentVector (create (cons a (cons b (cons c (cons d (cons e (cons f args))))))))))

(§ defn vec
  "Creates a new vector containing the contents of coll. Java arrays
  will be aliased and should not be modified."
  {:added "1.0"
   :static true}
  ([coll]
   (if (vector? coll)
     (if (instance? cloiure.lang.IObj coll)
       (with-meta coll nil)
       (cloiure.lang.LazilyPersistentVector/create coll))
     (cloiure.lang.LazilyPersistentVector/create coll))))

(§ defn hash-map
  "keyval => key val
  Returns a new hash map with supplied mappings.  If any keys are
  equal, they are handled as if by repeated uses of assoc."
  {:added "1.0"
   :static true}
  ([] {})
  ([& keyvals]
   (. cloiure.lang.PersistentHashMap (create keyvals))))

(§ defn hash-set
  "Returns a new hash set with supplied keys.  Any equal keys are
  handled as if by repeated uses of conj."
  {:added "1.0"
   :static true}
  ([] #{})
  ([& keys]
   (cloiure.lang.PersistentHashSet/create keys)))

(§ defn sorted-map
  "keyval => key val
  Returns a new sorted map with supplied mappings.  If any keys are
  equal, they are handled as if by repeated uses of assoc."
  {:added "1.0"
   :static true}
  ([& keyvals]
   (cloiure.lang.PersistentTreeMap/create keyvals)))

(§ defn sorted-map-by
  "keyval => key val
  Returns a new sorted map with supplied mappings, using the supplied
  comparator.  If any keys are equal, they are handled as if by
  repeated uses of assoc."
  {:added "1.0"
   :static true}
  ([comparator & keyvals]
   (cloiure.lang.PersistentTreeMap/create comparator keyvals)))

(§ defn sorted-set
  "Returns a new sorted set with supplied keys.  Any equal keys are
  handled as if by repeated uses of conj."
  {:added "1.0"
   :static true}
  ([& keys]
   (cloiure.lang.PersistentTreeSet/create keys)))

(§ defn sorted-set-by
  "Returns a new sorted set with supplied keys, using the supplied
  comparator.  Any equal keys are handled as if by repeated uses of
  conj."
  {:added "1.1"
   :static true}
  ([comparator & keys]
   (cloiure.lang.PersistentTreeSet/create comparator keys)))

;;;;;;;;;;;;;;;;;;;;
(§ defn nil?
  "Returns true if x is nil, false otherwise."
  {:tag Boolean
   :added "1.0"
   :static true
   :inline (fn [x] (list 'cloiure.lang.Util/identical x nil))}
  [x] (cloiure.lang.Util/identical x nil))

(§ def

 ^{:doc "Like defn, but the resulting function name is declared as a
  macro and will be used as a macro by the compiler when it is
  called."
   :arglists '([name doc-string? attr-map? [params*] body]
                 [name doc-string? attr-map? ([params*] body)+ attr-map?])
   :added "1.0"}
 defmacro (fn [&form &env
                name & args]
             (let [prefix (loop [p (list name) args args]
                            (let [f (first args)]
                              (if (string? f)
                                (recur (cons f p) (next args))
                                (if (map? f)
                                  (recur (cons f p) (next args))
                                  p))))
                   fdecl (loop [fd args]
                           (if (string? (first fd))
                             (recur (next fd))
                             (if (map? (first fd))
                               (recur (next fd))
                               fd)))
                   fdecl (if (vector? (first fdecl))
                           (list fdecl)
                           fdecl)
                   add-implicit-args (fn [fd]
                             (let [args (first fd)]
                               (cons (vec (cons '&form (cons '&env args))) (next fd))))
                   add-args (fn [acc ds]
                              (if (nil? ds)
                                acc
                                (let [d (first ds)]
                                  (if (map? d)
                                    (conj acc d)
                                    (recur (conj acc (add-implicit-args d)) (next ds))))))
                   fdecl (seq (add-args [] fdecl))
                   decl (loop [p prefix d fdecl]
                          (if p
                            (recur (next p) (cons (first p) d))
                            d))]
               (list 'do
                     (cons `defn decl)
                     (list '. (list 'var name) '(setMacro))
                     (list 'var name)))))

(§ . (var defmacro) (setMacro))

(§ defmacro when
  "Evaluates test. If logical true, evaluates body in an implicit do."
  {:added "1.0"}
  [test & body]
  (list 'if test (cons 'do body)))

(§ defmacro when-not
  "Evaluates test. If logical false, evaluates body in an implicit do."
  {:added "1.0"}
  [test & body]
    (list 'if test nil (cons 'do body)))

(§ defn false?
  "Returns true if x is the value false, false otherwise."
  {:tag Boolean,
   :added "1.0"
   :static true}
  [x] (cloiure.lang.Util/identical x false))

(§ defn true?
  "Returns true if x is the value true, false otherwise."
  {:tag Boolean,
   :added "1.0"
   :static true}
  [x] (cloiure.lang.Util/identical x true))

(§ defn boolean?
  "Return true if x is a Boolean"
  {:added "1.9"}
  [x] (instance? Boolean x))

(§ defn not
  "Returns true if x is logical false, false otherwise."
  {:tag Boolean
   :added "1.0"
   :static true}
  [x] (if x false true))

(§ defn some?
  "Returns true if x is not nil, false otherwise."
  {:tag Boolean
   :added "1.6"
   :static true}
  [x] (not (nil? x)))

(§ defn any?
  "Returns true given any argument."
  {:tag Boolean
   :added "1.9"}
  [x] true)

(§ defn str
  "With no args, returns the empty string. With one arg x, returns
  x.toString().  (str nil) returns the empty string. With more than
  one arg, returns the concatenation of the str values of the args."
  {:tag String
   :added "1.0"
   :static true}
  (^String [] "")
  (^String [^Object x]
   (if (nil? x) "" (. x (toString))))
  (^String [x & ys]
     ((fn [^StringBuilder sb more]
          (if more
            (recur (. sb  (append (str (first more)))) (next more))
            (str sb)))
      (new StringBuilder (str x)) ys)))

(§ defn symbol?
  "Return true if x is a Symbol"
  {:added "1.0"
   :static true}
  [x] (instance? cloiure.lang.Symbol x))

(§ defn keyword?
  "Return true if x is a Keyword"
  {:added "1.0"
   :static true}
  [x] (instance? cloiure.lang.Keyword x))

(§ defn symbol
  "Returns a Symbol with the given namespace and name."
  {:tag cloiure.lang.Symbol
   :added "1.0"
   :static true}
  ([name] (if (symbol? name) name (cloiure.lang.Symbol/intern name)))
  ([ns name] (cloiure.lang.Symbol/intern ns name)))

(§ defn gensym
  "Returns a new symbol with a unique name. If a prefix string is
  supplied, the name is prefix# where # is some unique number. If
  prefix is not supplied, the prefix is 'G__'."
  {:added "1.0"
   :static true}
  ([] (gensym "G__"))
  ([prefix-string] (. cloiure.lang.Symbol (intern (str prefix-string (str (. cloiure.lang.RT (nextID))))))))

(§ defmacro cond
  "Takes a set of test/expr pairs. It evaluates each test one at a
  time.  If a test returns logical true, cond evaluates and returns
  the value of the corresponding expr and doesn't evaluate any of the
  other tests or exprs. (cond) returns nil."
  {:added "1.0"}
  [& clauses]
    (when clauses
      (list 'if (first clauses)
            (if (next clauses)
                (second clauses)
                (throw (IllegalArgumentException.
                         "cond requires an even number of forms")))
            (cons 'cloiure.core/cond (next (next clauses))))))

(§ defn keyword
  "Returns a Keyword with the given namespace and name.  Do not use :
  in the keyword strings, it will be added automatically."
  {:tag cloiure.lang.Keyword
   :added "1.0"
   :static true}
  ([name] (cond (keyword? name) name
                (symbol? name) (cloiure.lang.Keyword/intern ^cloiure.lang.Symbol name)
                (string? name) (cloiure.lang.Keyword/intern ^String name)))
  ([ns name] (cloiure.lang.Keyword/intern ns name)))

(§ defn find-keyword
  "Returns a Keyword with the given namespace and name if one already
  exists.  This function will not intern a new keyword. If the keyword
  has not already been interned, it will return nil.  Do not use :
  in the keyword strings, it will be added automatically."
  {:tag cloiure.lang.Keyword
   :added "1.3"
   :static true}
  ([name] (cond (keyword? name) name
                (symbol? name) (cloiure.lang.Keyword/find ^cloiure.lang.Symbol name)
                (string? name) (cloiure.lang.Keyword/find ^String name)))
  ([ns name] (cloiure.lang.Keyword/find ns name)))

(§ defn spread
  {:private true
   :static true}
  [arglist]
  (cond
   (nil? arglist) nil
   (nil? (next arglist)) (seq (first arglist))
   :else (cons (first arglist) (spread (next arglist)))))

(§ defn list*
  "Creates a new seq containing the items prepended to the rest, the
  last of which will be treated as a sequence."
  {:added "1.0"
   :static true}
  ([args] (seq args))
  ([a args] (cons a args))
  ([a b args] (cons a (cons b args)))
  ([a b c args] (cons a (cons b (cons c args))))
  ([a b c d & more]
     (cons a (cons b (cons c (cons d (spread more)))))))

(§ defn apply
  "Applies fn f to the argument list formed by prepending intervening arguments to args."
  {:added "1.0"
   :static true}
  ([^cloiure.lang.IFn f args]
     (. f (applyTo (seq args))))
  ([^cloiure.lang.IFn f x args]
     (. f (applyTo (list* x args))))
  ([^cloiure.lang.IFn f x y args]
     (. f (applyTo (list* x y args))))
  ([^cloiure.lang.IFn f x y z args]
     (. f (applyTo (list* x y z args))))
  ([^cloiure.lang.IFn f a b c d & args]
     (. f (applyTo (cons a (cons b (cons c (cons d (spread args)))))))))

(§ defn vary-meta
 "Returns an object of the same type and value as obj, with
  (apply f (meta obj) args) as its metadata."
 {:added "1.0"
   :static true}
 [obj f & args]
  (with-meta obj (apply f (meta obj) args)))

(§ defmacro lazy-seq
  "Takes a body of expressions that returns an ISeq or nil, and yields
  a Seqable object that will invoke the body only the first time seq
  is called, and will cache the result and return it on all subsequent
  seq calls. See also - realized?"
  {:added "1.0"}
  [& body]
  (list 'new 'cloiure.lang.LazySeq (list* '^{:once true} fn* [] body)))

(§ defn ^:static ^cloiure.lang.ChunkBuffer chunk-buffer ^cloiure.lang.ChunkBuffer [capacity]
  (cloiure.lang.ChunkBuffer. capacity))

(§ defn ^:static chunk-append [^cloiure.lang.ChunkBuffer b x]
  (.add b x))

(§ defn ^:static ^cloiure.lang.IChunk chunk [^cloiure.lang.ChunkBuffer b]
  (.chunk b))

(§ defn ^:static  ^cloiure.lang.IChunk chunk-first ^cloiure.lang.IChunk [^cloiure.lang.IChunkedSeq s]
  (.chunkedFirst s))

(§ defn ^:static ^cloiure.lang.ISeq chunk-rest ^cloiure.lang.ISeq [^cloiure.lang.IChunkedSeq s]
  (.chunkedMore s))

(§ defn ^:static ^cloiure.lang.ISeq chunk-next ^cloiure.lang.ISeq [^cloiure.lang.IChunkedSeq s]
  (.chunkedNext s))

(§ defn ^:static chunk-cons [chunk rest]
  (if (cloiure.lang.Numbers/isZero (cloiure.lang.RT/count chunk))
    rest
    (cloiure.lang.ChunkedCons. chunk rest)))

(§ defn ^:static chunked-seq? [s]
  (instance? cloiure.lang.IChunkedSeq s))

(§ defn concat
  "Returns a lazy seq representing the concatenation of the elements in the supplied colls."
  {:added "1.0"
   :static true}
  ([] (lazy-seq nil))
  ([x] (lazy-seq x))
  ([x y]
    (lazy-seq
      (let [s (seq x)]
        (if s
          (if (chunked-seq? s)
            (chunk-cons (chunk-first s) (concat (chunk-rest s) y))
            (cons (first s) (concat (rest s) y)))
          y))))
  ([x y & zs]
     (let [cat (fn cat [xys zs]
                 (lazy-seq
                   (let [xys (seq xys)]
                     (if xys
                       (if (chunked-seq? xys)
                         (chunk-cons (chunk-first xys)
                                     (cat (chunk-rest xys) zs))
                         (cons (first xys) (cat (rest xys) zs)))
                       (when zs
                         (cat (first zs) (next zs)))))))]
       (cat (concat x y) zs))))

;;;;;;;;;;;;;;;;at this point all the support for syntax-quote exists;;;;;;;;;;;;;;;;;;;;;;
(§ defmacro delay
  "Takes a body of expressions and yields a Delay object that will
  invoke the body only the first time it is forced (with force or deref/@), and
  will cache the result and return it on all subsequent force
  calls. See also - realized?"
  {:added "1.0"}
  [& body]
    (list 'new 'cloiure.lang.Delay (list* `^{:once true} fn* [] body)))

(§ defn delay?
  "returns true if x is a Delay created with delay"
  {:added "1.0"
   :static true}
  [x] (instance? cloiure.lang.Delay x))

(§ defn force
  "If x is a Delay, returns the (possibly cached) value of its expression, else returns x"
  {:added "1.0"
   :static true}
  [x] (. cloiure.lang.Delay (force x)))

(§ defmacro if-not
  "Evaluates test. If logical false, evaluates and returns then expr,
  otherwise else expr, if supplied, else nil."
  {:added "1.0"}
  ([test then] `(if-not ~test ~then nil))
  ([test then else]
   `(if (not ~test) ~then ~else)))

(§ defn identical?
  "Tests if 2 arguments are the same object"
  {:inline (fn [x y] `(. cloiure.lang.Util identical ~x ~y))
   :inline-arities #{2}
   :added "1.0"}
  ([x y] (cloiure.lang.Util/identical x y)))

; equiv-based
(§ defn =
  "Equality. Returns true if x equals y, false if not. Same as
  Java x.equals(y) except it also works for nil, and compares
  numbers and collections in a type-independent manner.  Cloiure's immutable data
  structures define equals() (and thus =) as a value, not an identity,
  comparison."
  {:inline (fn [x y] `(. cloiure.lang.Util equiv ~x ~y))
   :inline-arities #{2}
   :added "1.0"}
  ([x] true)
  ([x y] (cloiure.lang.Util/equiv x y))
  ([x y & more]
   (if (cloiure.lang.Util/equiv x y)
     (if (next more)
       (recur y (first more) (next more))
       (cloiure.lang.Util/equiv y (first more)))
     false)))

; equals-based
#_(defn =
  "Equality. Returns true if x equals y, false if not. Same as Java
  x.equals(y) except it also works for nil. Boxed numbers must have
  same type. Cloiure's immutable data structures define equals() (and
  thus =) as a value, not an identity, comparison."
  {:inline (fn [x y] `(. cloiure.lang.Util equals ~x ~y))
   :inline-arities #{2}
   :added "1.0"}
  ([x] true)
  ([x y] (cloiure.lang.Util/equals x y))
  ([x y & more]
   (if (= x y)
     (if (next more)
       (recur y (first more) (next more))
       (= y (first more)))
     false)))

(§ defn not=
  "Same as (not (= obj1 obj2))"
  {:tag Boolean
   :added "1.0"
   :static true}
  ([x] false)
  ([x y] (not (= x y)))
  ([x y & more]
   (not (apply = x y more))))

(§ defn compare
  "Comparator. Returns a negative number, zero, or a positive number
  when x is logically 'less than', 'equal to', or 'greater than'
  y. Same as Java x.compareTo(y) except it also works for nil, and
  compares numbers and collections in a type-independent manner. x
  must implement Comparable"
  {
   :inline (fn [x y] `(. cloiure.lang.Util compare ~x ~y))
   :added "1.0"}
  [x y] (. cloiure.lang.Util (compare x y)))

(§ defmacro and
  "Evaluates exprs one at a time, from left to right. If a form
  returns logical false (nil or false), and returns that value and
  doesn't evaluate any of the other expressions, otherwise it returns
  the value of the last expr. (and) returns true."
  {:added "1.0"}
  ([] true)
  ([x] x)
  ([x & next]
   `(let [and# ~x]
      (if and# (and ~@next) and#))))

(§ defmacro or
  "Evaluates exprs one at a time, from left to right. If a form
  returns a logical true value, or returns that value and doesn't
  evaluate any of the other expressions, otherwise it returns the
  value of the last expression. (or) returns nil."
  {:added "1.0"}
  ([] nil)
  ([x] x)
  ([x & next]
      `(let [or# ~x]
         (if or# or# (or ~@next)))))

;;;;;;;;;;;;;;;;;;; sequence fns  ;;;;;;;;;;;;;;;;;;;;;;;
(§ defn zero?
  "Returns true if num is zero, else false"
  {
   :inline (fn [num] `(. cloiure.lang.Numbers (isZero ~num)))
   :added "1.0"}
  [num] (. cloiure.lang.Numbers (isZero num)))

(§ defn count
  "Returns the number of items in the collection. (count nil) returns
  0.  Also works on strings, arrays, and Java Collections and Maps"
  {
   :inline (fn  [x] `(. cloiure.lang.RT (count ~x)))
   :added "1.0"}
  [coll] (cloiure.lang.RT/count coll))

(§ defn int
  "Coerce to int"
  {
   :inline (fn  [x] `(. cloiure.lang.RT (~(if *unchecked-math* 'uncheckedIntCast 'intCast) ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.RT (intCast x)))

(§ defn nth
  "Returns the value at the index. get returns nil if index out of
  bounds, nth throws an exception unless not-found is supplied.  nth
  also works for strings, Java arrays, regex Matchers and Lists, and,
  in O(n) time, for sequences."
  {:inline (fn  [c i & nf] `(. cloiure.lang.RT (nth ~c ~i ~@nf)))
   :inline-arities #{2 3}
   :added "1.0"}
  ([coll index] (. cloiure.lang.RT (nth coll index)))
  ([coll index not-found] (. cloiure.lang.RT (nth coll index not-found))))

(§ defn <
  "Returns non-nil if nums are in monotonically increasing order,
  otherwise false."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (lt ~x ~y)))
   :inline-arities #{2}
   :added "1.0"}
  ([x] true)
  ([x y] (. cloiure.lang.Numbers (lt x y)))
  ([x y & more]
   (if (< x y)
     (if (next more)
       (recur y (first more) (next more))
       (< y (first more)))
     false)))

(§ defn inc'
  "Returns a number one greater than num. Supports arbitrary precision.
  See also: inc"
  {:inline (fn [x] `(. cloiure.lang.Numbers (incP ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (incP x)))

(§ defn inc
  "Returns a number one greater than num. Does not auto-promote
  longs, will throw on overflow. See also: inc'"
  {:inline (fn [x] `(. cloiure.lang.Numbers (~(if *unchecked-math* 'unchecked_inc 'inc) ~x)))
   :added "1.2"}
  [x] (. cloiure.lang.Numbers (inc x)))

;; reduce is defined again later after InternalReduce loads
(§ defn ^:private ^:static
  reduce1
       ([f coll]
             (let [s (seq coll)]
               (if s
         (reduce1 f (first s) (next s))
                 (f))))
       ([f val coll]
          (let [s (seq coll)]
            (if s
              (if (chunked-seq? s)
                (recur f
                       (.reduce (chunk-first s) f val)
                       (chunk-next s))
                (recur f (f val (first s)) (next s)))
         val))))

(§ defn reverse
  "Returns a seq of the items in coll in reverse order. Not lazy."
  {:added "1.0"
   :static true}
  [coll]
    (reduce1 conj () coll))

;; math stuff
(§ defn ^:private nary-inline
  ([op] (nary-inline op op))
  ([op unchecked-op]
     (fn
       ([x] (let [op (if *unchecked-math* unchecked-op op)]
              `(. cloiure.lang.Numbers (~op ~x))))
       ([x y] (let [op (if *unchecked-math* unchecked-op op)]
                `(. cloiure.lang.Numbers (~op ~x ~y))))
       ([x y & more]
          (let [op (if *unchecked-math* unchecked-op op)]
            (reduce1
             (fn [a b] `(. cloiure.lang.Numbers (~op ~a ~b)))
             `(. cloiure.lang.Numbers (~op ~x ~y)) more))))))

(§ defn ^:private >1? [n] (cloiure.lang.Numbers/gt n 1))
(§ defn ^:private >0? [n] (cloiure.lang.Numbers/gt n 0))

(§ defn +'
  "Returns the sum of nums. (+') returns 0. Supports arbitrary precision.
  See also: +"
  {:inline (nary-inline 'addP)
   :inline-arities >1?
   :added "1.0"}
  ([] 0)
  ([x] (cast Number x))
  ([x y] (. cloiure.lang.Numbers (addP x y)))
  ([x y & more]
   (reduce1 +' (+' x y) more)))

(§ defn +
  "Returns the sum of nums. (+) returns 0. Does not auto-promote
  longs, will throw on overflow. See also: +'"
  {:inline (nary-inline 'add 'unchecked_add)
   :inline-arities >1?
   :added "1.2"}
  ([] 0)
  ([x] (cast Number x))
  ([x y] (. cloiure.lang.Numbers (add x y)))
  ([x y & more]
     (reduce1 + (+ x y) more)))

(§ defn *'
  "Returns the product of nums. (*') returns 1. Supports arbitrary precision.
  See also: *"
  {:inline (nary-inline 'multiplyP)
   :inline-arities >1?
   :added "1.0"}
  ([] 1)
  ([x] (cast Number x))
  ([x y] (. cloiure.lang.Numbers (multiplyP x y)))
  ([x y & more]
   (reduce1 *' (*' x y) more)))

(§ defn *
  "Returns the product of nums. (*) returns 1. Does not auto-promote
  longs, will throw on overflow. See also: *'"
  {:inline (nary-inline 'multiply 'unchecked_multiply)
   :inline-arities >1?
   :added "1.2"}
  ([] 1)
  ([x] (cast Number x))
  ([x y] (. cloiure.lang.Numbers (multiply x y)))
  ([x y & more]
     (reduce1 * (* x y) more)))

(§ defn /
  "If no denominators are supplied, returns 1/numerator,
  else returns numerator divided by all of the denominators."
  {:inline (nary-inline 'divide)
   :inline-arities >1?
   :added "1.0"}
  ([x] (/ 1 x))
  ([x y] (. cloiure.lang.Numbers (divide x y)))
  ([x y & more]
   (reduce1 / (/ x y) more)))

(§ defn -'
  "If no ys are supplied, returns the negation of x, else subtracts
  the ys from x and returns the result. Supports arbitrary precision.
  See also: -"
  {:inline (nary-inline 'minusP)
   :inline-arities >0?
   :added "1.0"}
  ([x] (. cloiure.lang.Numbers (minusP x)))
  ([x y] (. cloiure.lang.Numbers (minusP x y)))
  ([x y & more]
   (reduce1 -' (-' x y) more)))

(§ defn -
  "If no ys are supplied, returns the negation of x, else subtracts
  the ys from x and returns the result. Does not auto-promote
  longs, will throw on overflow. See also: -'"
  {:inline (nary-inline 'minus 'unchecked_minus)
   :inline-arities >0?
   :added "1.2"}
  ([x] (. cloiure.lang.Numbers (minus x)))
  ([x y] (. cloiure.lang.Numbers (minus x y)))
  ([x y & more]
     (reduce1 - (- x y) more)))

(§ defn <=
  "Returns non-nil if nums are in monotonically non-decreasing order,
  otherwise false."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (lte ~x ~y)))
   :inline-arities #{2}
   :added "1.0"}
  ([x] true)
  ([x y] (. cloiure.lang.Numbers (lte x y)))
  ([x y & more]
   (if (<= x y)
     (if (next more)
       (recur y (first more) (next more))
       (<= y (first more)))
     false)))

(§ defn >
  "Returns non-nil if nums are in monotonically decreasing order,
  otherwise false."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (gt ~x ~y)))
   :inline-arities #{2}
   :added "1.0"}
  ([x] true)
  ([x y] (. cloiure.lang.Numbers (gt x y)))
  ([x y & more]
   (if (> x y)
     (if (next more)
       (recur y (first more) (next more))
       (> y (first more)))
     false)))

(§ defn >=
  "Returns non-nil if nums are in monotonically non-increasing order,
  otherwise false."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (gte ~x ~y)))
   :inline-arities #{2}
   :added "1.0"}
  ([x] true)
  ([x y] (. cloiure.lang.Numbers (gte x y)))
  ([x y & more]
   (if (>= x y)
     (if (next more)
       (recur y (first more) (next more))
       (>= y (first more)))
     false)))

(§ defn ==
  "Returns non-nil if nums all have the equivalent
  value (type-independent), otherwise false"
  {:inline (fn [x y] `(. cloiure.lang.Numbers (equiv ~x ~y)))
   :inline-arities #{2}
   :added "1.0"}
  ([x] true)
  ([x y] (. cloiure.lang.Numbers (equiv x y)))
  ([x y & more]
   (if (== x y)
     (if (next more)
       (recur y (first more) (next more))
       (== y (first more)))
     false)))

(§ defn max
  "Returns the greatest of the nums."
  {:added "1.0"
   :inline-arities >1?
   :inline (nary-inline 'max)}
  ([x] x)
  ([x y] (. cloiure.lang.Numbers (max x y)))
  ([x y & more]
   (reduce1 max (max x y) more)))

(§ defn min
  "Returns the least of the nums."
  {:added "1.0"
   :inline-arities >1?
   :inline (nary-inline 'min)}
  ([x] x)
  ([x y] (. cloiure.lang.Numbers (min x y)))
  ([x y & more]
   (reduce1 min (min x y) more)))

(§ defn dec'
  "Returns a number one less than num. Supports arbitrary precision.
  See also: dec"
  {:inline (fn [x] `(. cloiure.lang.Numbers (decP ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (decP x)))

(§ defn dec
  "Returns a number one less than num. Does not auto-promote
  longs, will throw on overflow. See also: dec'"
  {:inline (fn [x] `(. cloiure.lang.Numbers (~(if *unchecked-math* 'unchecked_dec 'dec) ~x)))
   :added "1.2"}
  [x] (. cloiure.lang.Numbers (dec x)))

(§ defn unchecked-inc-int
  "Returns a number one greater than x, an int.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. cloiure.lang.Numbers (unchecked_int_inc ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (unchecked_int_inc x)))

(§ defn unchecked-inc
  "Returns a number one greater than x, a long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. cloiure.lang.Numbers (unchecked_inc ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (unchecked_inc x)))

(§ defn unchecked-dec-int
  "Returns a number one less than x, an int.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. cloiure.lang.Numbers (unchecked_int_dec ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (unchecked_int_dec x)))

(§ defn unchecked-dec
  "Returns a number one less than x, a long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. cloiure.lang.Numbers (unchecked_dec ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (unchecked_dec x)))

(§ defn unchecked-negate-int
  "Returns the negation of x, an int.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. cloiure.lang.Numbers (unchecked_int_negate ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (unchecked_int_negate x)))

(§ defn unchecked-negate
  "Returns the negation of x, a long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. cloiure.lang.Numbers (unchecked_minus ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (unchecked_minus x)))

(§ defn unchecked-add-int
  "Returns the sum of x and y, both int.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (unchecked_int_add ~x ~y)))
   :added "1.0"}
  [x y] (. cloiure.lang.Numbers (unchecked_int_add x y)))

(§ defn unchecked-add
  "Returns the sum of x and y, both long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (unchecked_add ~x ~y)))
   :added "1.0"}
  [x y] (. cloiure.lang.Numbers (unchecked_add x y)))

(§ defn unchecked-subtract-int
  "Returns the difference of x and y, both int.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (unchecked_int_subtract ~x ~y)))
   :added "1.0"}
  [x y] (. cloiure.lang.Numbers (unchecked_int_subtract x y)))

(§ defn unchecked-subtract
  "Returns the difference of x and y, both long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (unchecked_minus ~x ~y)))
   :added "1.0"}
  [x y] (. cloiure.lang.Numbers (unchecked_minus x y)))

(§ defn unchecked-multiply-int
  "Returns the product of x and y, both int.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (unchecked_int_multiply ~x ~y)))
   :added "1.0"}
  [x y] (. cloiure.lang.Numbers (unchecked_int_multiply x y)))

(§ defn unchecked-multiply
  "Returns the product of x and y, both long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (unchecked_multiply ~x ~y)))
   :added "1.0"}
  [x y] (. cloiure.lang.Numbers (unchecked_multiply x y)))

(§ defn unchecked-divide-int
  "Returns the division of x by y, both int.
  Note - uses a primitive operator subject to truncation."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (unchecked_int_divide ~x ~y)))
   :added "1.0"}
  [x y] (. cloiure.lang.Numbers (unchecked_int_divide x y)))

(§ defn unchecked-remainder-int
  "Returns the remainder of division of x by y, both int.
  Note - uses a primitive operator subject to truncation."
  {:inline (fn [x y] `(. cloiure.lang.Numbers (unchecked_int_remainder ~x ~y)))
   :added "1.0"}
  [x y] (. cloiure.lang.Numbers (unchecked_int_remainder x y)))

(§ defn pos?
  "Returns true if num is greater than zero, else false"
  {
   :inline (fn [num] `(. cloiure.lang.Numbers (isPos ~num)))
   :added "1.0"}
  [num] (. cloiure.lang.Numbers (isPos num)))

(§ defn neg?
  "Returns true if num is less than zero, else false"
  {
   :inline (fn [num] `(. cloiure.lang.Numbers (isNeg ~num)))
   :added "1.0"}
  [num] (. cloiure.lang.Numbers (isNeg num)))

(§ defn quot
  "quot[ient] of dividing numerator by denominator."
  {:added "1.0"
   :static true
   :inline (fn [x y] `(. cloiure.lang.Numbers (quotient ~x ~y)))}
  [num div]
    (. cloiure.lang.Numbers (quotient num div)))

(§ defn rem
  "remainder of dividing numerator by denominator."
  {:added "1.0"
   :static true
   :inline (fn [x y] `(. cloiure.lang.Numbers (remainder ~x ~y)))}
  [num div]
    (. cloiure.lang.Numbers (remainder num div)))

(§ defn rationalize
  "returns the rational value of num"
  {:added "1.0"
   :static true}
  [num]
  (. cloiure.lang.Numbers (rationalize num)))

;; Bit ops

(§ defn bit-not
  "Bitwise complement"
  {:inline (fn [x] `(. cloiure.lang.Numbers (not ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers not x))

(§ defn bit-and
  "Bitwise and"
   {:inline (nary-inline 'and)
    :inline-arities >1?
    :added "1.0"}
   ([x y] (. cloiure.lang.Numbers and x y))
   ([x y & more]
      (reduce1 bit-and (bit-and x y) more)))

(§ defn bit-or
  "Bitwise or"
  {:inline (nary-inline 'or)
   :inline-arities >1?
   :added "1.0"}
  ([x y] (. cloiure.lang.Numbers or x y))
  ([x y & more]
    (reduce1 bit-or (bit-or x y) more)))

(§ defn bit-xor
  "Bitwise exclusive or"
  {:inline (nary-inline 'xor)
   :inline-arities >1?
   :added "1.0"}
  ([x y] (. cloiure.lang.Numbers xor x y))
  ([x y & more]
    (reduce1 bit-xor (bit-xor x y) more)))

(§ defn bit-and-not
  "Bitwise and with complement"
  {:inline (nary-inline 'andNot)
   :inline-arities >1?
   :added "1.0"
   :static true}
  ([x y] (. cloiure.lang.Numbers andNot x y))
  ([x y & more]
    (reduce1 bit-and-not (bit-and-not x y) more)))

(§ defn bit-clear
  "Clear bit at index n"
  {:added "1.0"
   :static true}
  [x n] (. cloiure.lang.Numbers clearBit x n))

(§ defn bit-set
  "Set bit at index n"
  {:added "1.0"
   :static true}
  [x n] (. cloiure.lang.Numbers setBit x n))

(§ defn bit-flip
  "Flip bit at index n"
  {:added "1.0"
   :static true}
  [x n] (. cloiure.lang.Numbers flipBit x n))

(§ defn bit-test
  "Test bit at index n"
  {:added "1.0"
   :static true}
  [x n] (. cloiure.lang.Numbers testBit x n))

(§ defn bit-shift-left
  "Bitwise shift left"
  {:inline (fn [x n] `(. cloiure.lang.Numbers (shiftLeft ~x ~n)))
   :added "1.0"}
  [x n] (. cloiure.lang.Numbers shiftLeft x n))

(§ defn bit-shift-right
  "Bitwise shift right"
  {:inline (fn [x n] `(. cloiure.lang.Numbers (shiftRight ~x ~n)))
   :added "1.0"}
  [x n] (. cloiure.lang.Numbers shiftRight x n))

(§ defn unsigned-bit-shift-right
  "Bitwise shift right, without sign-extension."
  {:inline (fn [x n] `(. cloiure.lang.Numbers (unsignedShiftRight ~x ~n)))
   :added "1.6"}
  [x n] (. cloiure.lang.Numbers unsignedShiftRight x n))

(§ defn integer?
  "Returns true if n is an integer"
  {:added "1.0"
   :static true}
  [n]
  (or (instance? Integer n)
      (instance? Long n)
      (instance? cloiure.lang.BigInt n)
      (instance? BigInteger n)
      (instance? Short n)
      (instance? Byte n)))

(§ defn even?
  "Returns true if n is even, throws an exception if n is not an integer"
  {:added "1.0"
   :static true}
   [n] (if (integer? n)
        (zero? (bit-and (cloiure.lang.RT/uncheckedLongCast n) 1))
        (throw (IllegalArgumentException. (str "Argument must be an integer: " n)))))

(§ defn odd?
  "Returns true if n is odd, throws an exception if n is not an integer"
  {:added "1.0"
   :static true}
  [n] (not (even? n)))

(§ defn int?
  "Return true if x is a fixed precision integer"
  {:added "1.9"}
  [x] (or (instance? Long x)
          (instance? Integer x)
          (instance? Short x)
          (instance? Byte x)))

(§ defn pos-int?
  "Return true if x is a positive fixed precision integer"
  {:added "1.9"}
  [x] (and (int? x)
           (pos? x)))

(§ defn neg-int?
  "Return true if x is a negative fixed precision integer"
  {:added "1.9"}
  [x] (and (int? x)
           (neg? x)))

(§ defn nat-int?
  "Return true if x is a non-negative fixed precision integer"
  {:added "1.9"}
  [x] (and (int? x)
           (not (neg? x))))

(§ defn double?
  "Return true if x is a Double"
  {:added "1.9"}
  [x] (instance? Double x))

;;

(§ defn complement
  "Takes a fn f and returns a fn that takes the same arguments as f,
  has the same effects, if any, and returns the opposite truth value."
  {:added "1.0"
   :static true}
  [f]
  (fn
    ([] (not (f)))
    ([x] (not (f x)))
    ([x y] (not (f x y)))
    ([x y & zs] (not (apply f x y zs)))))

(§ defn constantly
  "Returns a function that takes any number of arguments and returns x."
  {:added "1.0"
   :static true}
  [x] (fn [& args] x))

(§ defn identity
  "Returns its argument."
  {:added "1.0"
   :static true}
  [x] x)

;; Collection stuff

;; list stuff
(§ defn peek
  "For a list or queue, same as first, for a vector, same as, but much
  more efficient than, last. If the collection is empty, returns nil."
  {:added "1.0"
   :static true}
  [coll] (. cloiure.lang.RT (peek coll)))

(§ defn pop
  "For a list or queue, returns a new list/queue without the first
  item, for a vector, returns a new vector without the last item. If
  the collection is empty, throws an exception.  Note - not the same
  as next/butlast."
  {:added "1.0"
   :static true}
  [coll] (. cloiure.lang.RT (pop coll)))

;; map stuff

(§ defn map-entry?
  "Return true if x is a map entry"
  {:added "1.8"}
  [x]
        (instance? java.util.Map$Entry x))

(§ defn contains?
  "Returns true if key is present in the given collection, otherwise
  returns false.  Note that for numerically indexed collections like
  vectors and Java arrays, this tests if the numeric key is within the
  range of indexes. 'contains?' operates constant or logarithmic time;
  it will not perform a linear search for a value.  See also 'some'."
  {:added "1.0"
   :static true}
  [coll key] (. cloiure.lang.RT (contains coll key)))

(§ defn get
  "Returns the value mapped to key, not-found or nil if key not present."
  {:inline (fn  [m k & nf] `(. cloiure.lang.RT (get ~m ~k ~@nf)))
   :inline-arities #{2 3}
   :added "1.0"}
  ([map key]
   (. cloiure.lang.RT (get map key)))
  ([map key not-found]
   (. cloiure.lang.RT (get map key not-found))))

(§ defn dissoc
  "dissoc[iate]. Returns a new map of the same (hashed/sorted) type,
  that does not contain a mapping for key(s)."
  {:added "1.0"
   :static true}
  ([map] map)
  ([map key]
   (. cloiure.lang.RT (dissoc map key)))
  ([map key & ks]
   (let [ret (dissoc map key)]
     (if ks
       (recur ret (first ks) (next ks))
       ret))))

(§ defn disj
  "disj[oin]. Returns a new set of the same (hashed/sorted) type, that
  does not contain key(s)."
  {:added "1.0"
   :static true}
  ([set] set)
  ([^cloiure.lang.IPersistentSet set key]
   (when set
     (. set (disjoin key))))
  ([set key & ks]
   (when set
     (let [ret (disj set key)]
       (if ks
         (recur ret (first ks) (next ks))
         ret)))))

(§ defn find
  "Returns the map entry for key, or nil if key not present."
  {:added "1.0"
   :static true}
  [map key] (. cloiure.lang.RT (find map key)))

(§ defn select-keys
  "Returns a map containing only those entries in map whose key is in keys"
  {:added "1.0"
   :static true}
  [map keyseq]
    (loop [ret {} keys (seq keyseq)]
      (if keys
        (let [entry (. cloiure.lang.RT (find map (first keys)))]
          (recur
           (if entry
             (conj ret entry)
             ret)
           (next keys)))
        (with-meta ret (meta map)))))

(§ defn keys
  "Returns a sequence of the map's keys, in the same order as (seq map)."
  {:added "1.0"
   :static true}
  [map] (. cloiure.lang.RT (keys map)))

(§ defn vals
  "Returns a sequence of the map's values, in the same order as (seq map)."
  {:added "1.0"
   :static true}
  [map] (. cloiure.lang.RT (vals map)))

(§ defn key
  "Returns the key of the map entry."
  {:added "1.0"
   :static true}
  [^java.util.Map$Entry e]
    (. e (getKey)))

(§ defn val
  "Returns the value in the map entry."
  {:added "1.0"
   :static true}
  [^java.util.Map$Entry e]
    (. e (getValue)))

(§ defn rseq
  "Returns, in constant time, a seq of the items in rev (which
  can be a vector or sorted-map), in reverse order. If rev is empty returns nil"
  {:added "1.0"
   :static true}
  [^cloiure.lang.Reversible rev]
    (. rev (rseq)))

(§ defn name
  "Returns the name String of a string, symbol or keyword."
  {:tag String
   :added "1.0"
   :static true}
  [x]
  (if (string? x) x (. ^cloiure.lang.Named x (getName))))

(§ defn namespace
  "Returns the namespace String of a symbol or keyword, or nil if not present."
  {:tag String
   :added "1.0"
   :static true}
  [^cloiure.lang.Named x]
    (. x (getNamespace)))

(§ defn boolean
  "Coerce to boolean"
  {
   :inline (fn  [x] `(. cloiure.lang.RT (booleanCast ~x)))
   :added "1.0"}
  [x] (cloiure.lang.RT/booleanCast x))

(§ defn ident?
  "Return true if x is a symbol or keyword"
  {:added "1.9"}
  [x] (or (keyword? x) (symbol? x)))

(§ defn simple-ident?
  "Return true if x is a symbol or keyword without a namespace"
  {:added "1.9"}
  [x] (and (ident? x) (nil? (namespace x))))

(§ defn qualified-ident?
  "Return true if x is a symbol or keyword with a namespace"
  {:added "1.9"}
  [x] (boolean (and (ident? x) (namespace x) true)))

(§ defn simple-symbol?
  "Return true if x is a symbol without a namespace"
  {:added "1.9"}
  [x] (and (symbol? x) (nil? (namespace x))))

(§ defn qualified-symbol?
  "Return true if x is a symbol with a namespace"
  {:added "1.9"}
  [x] (boolean (and (symbol? x) (namespace x) true)))

(§ defn simple-keyword?
  "Return true if x is a keyword without a namespace"
  {:added "1.9"}
  [x] (and (keyword? x) (nil? (namespace x))))

(§ defn qualified-keyword?
  "Return true if x is a keyword with a namespace"
  {:added "1.9"}
  [x] (boolean (and (keyword? x) (namespace x) true)))

(§ defmacro locking
  "Executes exprs in an implicit do, while holding the monitor of x.
  Will release the monitor of x in all circumstances."
  {:added "1.0"}
  [x & body]
  `(let [lockee# ~x]
     (try
      (monitor-enter lockee#)
      ~@body
      (finally
       (monitor-exit lockee#)))))

(§ defmacro ..
  "form => fieldName-symbol or (instanceMethodName-symbol args*)

  Expands into a member access (.) of the first member on the first
  argument, followed by the next member on the result, etc. For
  instance:

  (.. System (getProperties) (get \"os.name\"))

  expands to:

  (. (. System (getProperties)) (get \"os.name\"))

  but is easier to write, read, and understand."
  {:added "1.0"}
  ([x form] `(. ~x ~form))
  ([x form & more] `(.. (. ~x ~form) ~@more)))

(§ defmacro ->
  "Threads the expr through the forms. Inserts x as the
  second item in the first form, making a list of it if it is not a
  list already. If there are more forms, inserts the first form as the
  second item in second form, etc."
  {:added "1.0"}
  [x & forms]
  (loop [x x, forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (with-meta `(~(first form) ~x ~@(next form)) (meta form))
                       (list form x))]
        (recur threaded (next forms)))
      x)))

(§ defmacro ->>
  "Threads the expr through the forms. Inserts x as the
  last item in the first form, making a list of it if it is not a
  list already. If there are more forms, inserts the first form as the
  last item in second form, etc."
  {:added "1.1"}
  [x & forms]
  (loop [x x, forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
              (with-meta `(~(first form) ~@(next form)  ~x) (meta form))
              (list form x))]
        (recur threaded (next forms)))
      x)))

(§ def map)

(§ defn ^:private check-valid-options
  "Throws an exception if the given option map contains keys not listed
  as valid, else returns nil."
  [options & valid-keys]
  (when (seq (apply disj (apply hash-set (keys options)) valid-keys))
    (throw
      (IllegalArgumentException.
        (apply str "Only these options are valid: "
          (first valid-keys)
          (map #(str ", " %) (rest valid-keys)))))))

;; multimethods
(§ def global-hierarchy)

(§ defmacro defmulti
  "Creates a new multimethod with the associated dispatch function.
  The docstring and attr-map are optional.

  Options are key-value pairs and may be one of:

  :default

  The default dispatch value, defaults to :default

  :hierarchy

  The value used for hierarchical dispatch (e.g. ::square is-a ::shape)

  Hierarchies are type-like relationships that do not depend upon type
  inheritance. By default Cloiure's multimethods dispatch off of a
  global hierarchy map.  However, a hierarchy relationship can be
  created with the derive function used to augment the root ancestor
  created with make-hierarchy.

  Multimethods expect the value of the hierarchy option to be supplied as
  a reference type e.g. a var (i.e. via the Var-quote dispatch macro #'
  or the var special form)."
  {:arglists '([name docstring? attr-map? dispatch-fn & options])
   :added "1.0"}
  [mm-name & options]
  (let [docstring   (if (string? (first options))
                      (first options)
                      nil)
        options     (if (string? (first options))
                      (next options)
                      options)
        m           (if (map? (first options))
                      (first options)
                      {})
        options     (if (map? (first options))
                      (next options)
                      options)
        dispatch-fn (first options)
        options     (next options)
        m           (if docstring
                      (assoc m :doc docstring)
                      m)
        m           (if (meta mm-name)
                      (conj (meta mm-name) m)
                      m)
        mm-name (with-meta mm-name m)]
    (when (= (count options) 1)
      (throw (Exception. "The syntax for defmulti has changed. Example: (defmulti name dispatch-fn :default dispatch-value)")))
    (let [options   (apply hash-map options)
          default   (get options :default :default)
          hierarchy (get options :hierarchy #'global-hierarchy)]
      (check-valid-options options :default :hierarchy)
      `(let [v# (def ~mm-name)]
         (when-not (and (.hasRoot v#) (instance? cloiure.lang.MultiFn (deref v#)))
           (def ~mm-name
                (new cloiure.lang.MultiFn ~(name mm-name) ~dispatch-fn ~default ~hierarchy)))))))

(§ defmacro defmethod
  "Creates and installs a new method of multimethod associated with dispatch-value. "
  {:added "1.0"}
  [multifn dispatch-val & fn-tail]
  `(. ~(with-meta multifn {:tag 'cloiure.lang.MultiFn}) addMethod ~dispatch-val (fn ~@fn-tail)))

(§ defn remove-all-methods
  "Removes all of the methods of multimethod."
  {:added "1.2"
   :static true}
 [^cloiure.lang.MultiFn multifn]
 (.reset multifn))

(§ defn remove-method
  "Removes the method of multimethod associated with dispatch-value."
  {:added "1.0"
   :static true}
 [^cloiure.lang.MultiFn multifn dispatch-val]
 (. multifn removeMethod dispatch-val))

(§ defn prefer-method
  "Causes the multimethod to prefer matches of dispatch-val-x over dispatch-val-y
   when there is a conflict"
  {:added "1.0"
   :static true}
  [^cloiure.lang.MultiFn multifn dispatch-val-x dispatch-val-y]
  (. multifn preferMethod dispatch-val-x dispatch-val-y))

(§ defn methods
  "Given a multimethod, returns a map of dispatch values -> dispatch fns"
  {:added "1.0"
   :static true}
  [^cloiure.lang.MultiFn multifn] (.getMethodTable multifn))

(§ defn get-method
  "Given a multimethod and a dispatch value, returns the dispatch fn
  that would apply to that value, or nil if none apply and no default"
  {:added "1.0"
   :static true}
  [^cloiure.lang.MultiFn multifn dispatch-val] (.getMethod multifn dispatch-val))

(§ defn prefers
  "Given a multimethod, returns a map of preferred value -> set of other values"
  {:added "1.0"
   :static true}
  [^cloiure.lang.MultiFn multifn] (.getPreferTable multifn))

;;;;;;;;; var stuff

(§ defmacro ^{:private true} assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                  (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args more)))))

(§ defmacro if-let
  "bindings => binding-form test

  If test is true, evaluates then with binding-form bound to the value of
  test, if not, yields else"
  {:added "1.0"}
  ([bindings then]
   `(if-let ~bindings ~then nil))
  ([bindings then else & oldform]
   (assert-args
     (vector? bindings) "a vector for its binding"
     (nil? oldform) "1 or 2 forms after binding vector"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
   (let [form (bindings 0) tst (bindings 1)]
     `(let [temp# ~tst]
        (if temp#
          (let [~form temp#]
            ~then)
          ~else)))))

(§ defmacro when-let
  "bindings => binding-form test

  When test is true, evaluates body with binding-form bound to the value of test"
  {:added "1.0"}
  [bindings & body]
  (assert-args
     (vector? bindings) "a vector for its binding"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
   (let [form (bindings 0) tst (bindings 1)]
    `(let [temp# ~tst]
       (when temp#
         (let [~form temp#]
           ~@body)))))

(§ defmacro if-some
  "bindings => binding-form test

   If test is not nil, evaluates then with binding-form bound to the
   value of test, if not, yields else"
  {:added "1.6"}
  ([bindings then]
   `(if-some ~bindings ~then nil))
  ([bindings then else & oldform]
   (assert-args
     (vector? bindings) "a vector for its binding"
     (nil? oldform) "1 or 2 forms after binding vector"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
   (let [form (bindings 0) tst (bindings 1)]
     `(let [temp# ~tst]
        (if (nil? temp#)
          ~else
          (let [~form temp#]
            ~then))))))

(§ defmacro when-some
  "bindings => binding-form test

   When test is not nil, evaluates body with binding-form bound to the
   value of test"
  {:added "1.6"}
  [bindings & body]
  (assert-args
     (vector? bindings) "a vector for its binding"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
   (let [form (bindings 0) tst (bindings 1)]
    `(let [temp# ~tst]
       (if (nil? temp#)
         nil
         (let [~form temp#]
           ~@body)))))

(§ defn push-thread-bindings
  "WARNING: This is a low-level function. Prefer high-level macros like
  binding where ever possible.

  Takes a map of Var/value pairs. Binds each Var to the associated value for
  the current thread. Each call *MUST* be accompanied by a matching call to
  pop-thread-bindings wrapped in a try-finally!

      (push-thread-bindings bindings)
      (try
        ...
        (finally
          (pop-thread-bindings)))"
  {:added "1.1"
   :static true}
  [bindings]
  (cloiure.lang.Var/pushThreadBindings bindings))

(§ defn pop-thread-bindings
  "Pop one set of bindings pushed with push-binding before. It is an error to
  pop bindings without pushing before."
  {:added "1.1"
   :static true}
  []
  (cloiure.lang.Var/popThreadBindings))

(§ defn get-thread-bindings
  "Get a map with the Var/value pairs which is currently in effect for the
  current thread."
  {:added "1.1"
   :static true}
  []
  (cloiure.lang.Var/getThreadBindings))

(§ defmacro binding
  "binding => var-symbol init-expr

  Creates new bindings for the (already-existing) vars, with the
  supplied initial values, executes the exprs in an implicit do, then
  re-establishes the bindings that existed before.  The new bindings
  are made in parallel (unlike let); all init-exprs are evaluated
  before the vars are bound to their new values."
  {:added "1.0"}
  [bindings & body]
  (assert-args
    (vector? bindings) "a vector for its binding"
    (even? (count bindings)) "an even number of forms in binding vector")
  (let [var-ize (fn [var-vals]
                  (loop [ret [] vvs (seq var-vals)]
                    (if vvs
                      (recur  (conj (conj ret `(var ~(first vvs))) (second vvs))
                             (next (next vvs)))
                      (seq ret))))]
    `(let []
       (push-thread-bindings (hash-map ~@(var-ize bindings)))
       (try
         ~@body
         (finally
           (pop-thread-bindings))))))

(§ defn with-bindings*
  "Takes a map of Var/value pairs. Installs for the given Vars the associated
  values as thread-local bindings. Then calls f with the supplied arguments.
  Pops the installed bindings after f returned. Returns whatever f returns."
  {:added "1.1"
   :static true}
  [binding-map f & args]
  (push-thread-bindings binding-map)
  (try
    (apply f args)
    (finally
      (pop-thread-bindings))))

(§ defmacro with-bindings
  "Takes a map of Var/value pairs. Installs for the given Vars the associated
  values as thread-local bindings. Then executes body. Pops the installed
  bindings after body was evaluated. Returns the value of body."
  {:added "1.1"}
  [binding-map & body]
  `(with-bindings* ~binding-map (fn [] ~@body)))

(§ defn bound-fn*
  "Returns a function, which will install the same bindings in effect as in
  the thread at the time bound-fn* was called and then call f with any given
  arguments. This may be used to define a helper function which runs on a
  different thread, but needs the same bindings in place."
  {:added "1.1"
   :static true}
  [f]
  (let [bindings (get-thread-bindings)]
    (fn [& args]
      (apply with-bindings* bindings f args))))

(§ defmacro bound-fn
  "Returns a function defined by the given fntail, which will install the
  same bindings in effect as in the thread at the time bound-fn was called.
  This may be used to define a helper function which runs on a different
  thread, but needs the same bindings in place."
  {:added "1.1"}
  [& fntail]
  `(bound-fn* (fn ~@fntail)))

(§ defn find-var
  "Returns the global var named by the namespace-qualified symbol, or
  nil if no var with that name."
  {:added "1.0"
   :static true}
  [sym] (. cloiure.lang.Var (find sym)))

(§ defn binding-conveyor-fn
  {:private true
   :added "1.3"}
  [f]
  (let [frame (cloiure.lang.Var/cloneThreadBindingFrame)]
    (fn
      ([]
         (cloiure.lang.Var/resetThreadBindingFrame frame)
         (f))
      ([x]
         (cloiure.lang.Var/resetThreadBindingFrame frame)
         (f x))
      ([x y]
         (cloiure.lang.Var/resetThreadBindingFrame frame)
         (f x y))
      ([x y z]
         (cloiure.lang.Var/resetThreadBindingFrame frame)
         (f x y z))
      ([x y z & args]
         (cloiure.lang.Var/resetThreadBindingFrame frame)
         (apply f x y z args)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Refs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(§ defn ^{:private true}
  setup-reference [^cloiure.lang.ARef r options]
  (let [opts (apply hash-map options)]
    (when (:meta opts)
      (.resetMeta r (:meta opts)))
    (when (:validator opts)
      (.setValidator r (:validator opts)))
    r))

(§ defn agent
  "Creates and returns an agent with an initial value of state and
  zero or more options (in any order):

  :meta metadata-map

  :validator validate-fn

  :error-handler handler-fn

  :error-mode mode-keyword

  If metadata-map is supplied, it will become the metadata on the
  agent. validate-fn must be nil or a side-effect-free fn of one
  argument, which will be passed the intended new state on any state
  change. If the new state is unacceptable, the validate-fn should
  return false or throw an exception.  handler-fn is called if an
  action throws an exception or if validate-fn rejects a new state --
  see set-error-handler! for details.  The mode-keyword may be either
  :continue (the default if an error-handler is given) or :fail (the
  default if no error-handler is given) -- see set-error-mode! for
  details."
  {:added "1.0"
   :static true
   }
  ([state & options]
     (let [a (new cloiure.lang.Agent state)
           opts (apply hash-map options)]
       (setup-reference a options)
       (when (:error-handler opts)
         (.setErrorHandler a (:error-handler opts)))
       (.setErrorMode a (or (:error-mode opts)
                            (if (:error-handler opts) :continue :fail)))
       a)))

(§ defn set-agent-send-executor!
  "Sets the ExecutorService to be used by send"
  {:added "1.5"}
  [executor]
  (set! cloiure.lang.Agent/pooledExecutor executor))

(§ defn set-agent-send-off-executor!
  "Sets the ExecutorService to be used by send-off"
  {:added "1.5"}
  [executor]
  (set! cloiure.lang.Agent/soloExecutor executor))

(§ defn send-via
  "Dispatch an action to an agent. Returns the agent immediately.
  Subsequently, in a thread supplied by executor, the state of the agent
  will be set to the value of:

  (apply action-fn state-of-agent args)"
  {:added "1.5"}
  [executor ^cloiure.lang.Agent a f & args]
  (.dispatch a (binding [*agent* a] (binding-conveyor-fn f)) args executor))

(§ defn send
  "Dispatch an action to an agent. Returns the agent immediately.
  Subsequently, in a thread from a thread pool, the state of the agent
  will be set to the value of:

  (apply action-fn state-of-agent args)"
  {:added "1.0"
   :static true}
  [^cloiure.lang.Agent a f & args]
  (apply send-via cloiure.lang.Agent/pooledExecutor a f args))

(§ defn send-off
  "Dispatch a potentially blocking action to an agent. Returns the
  agent immediately. Subsequently, in a separate thread, the state of
  the agent will be set to the value of:

  (apply action-fn state-of-agent args)"
  {:added "1.0"
   :static true}
  [^cloiure.lang.Agent a f & args]
  (apply send-via cloiure.lang.Agent/soloExecutor a f args))

(§ defn release-pending-sends
  "Normally, actions sent directly or indirectly during another action
  are held until the action completes (changes the agent's
  state). This function can be used to dispatch any pending sent
  actions immediately. This has no impact on actions sent during a
  transaction, which are still held until commit. If no action is
  occurring, does nothing. Returns the number of actions dispatched."
  {:added "1.0"
   :static true}
  [] (cloiure.lang.Agent/releasePendingSends))

(§ defn add-watch
  "Adds a watch function to an agent/atom/var/ref reference. The watch
  fn must be a fn of 4 args: a key, the reference, its old-state, its
  new-state. Whenever the reference's state might have been changed,
  any registered watches will have their functions called. The watch fn
  will be called synchronously, on the agent's thread if an agent,
  before any pending sends if agent or ref. Note that an atom's or
  ref's state may have changed again prior to the fn call, so use
  old/new-state rather than derefing the reference. Note also that watch
  fns may be called from multiple threads simultaneously. Var watchers
  are triggered only by root binding changes, not thread-local
  set!s. Keys must be unique per reference, and can be used to remove
  the watch with remove-watch, but are otherwise considered opaque by
  the watch mechanism."
  {:added "1.0"
   :static true}
  [^cloiure.lang.IRef reference key fn] (.addWatch reference key fn))

(§ defn remove-watch
  "Removes a watch (set by add-watch) from a reference"
  {:added "1.0"
   :static true}
  [^cloiure.lang.IRef reference key]
  (.removeWatch reference key))

(§ defn agent-error
  "Returns the exception thrown during an asynchronous action of the
  agent if the agent is failed.  Returns nil if the agent is not
  failed."
  {:added "1.2"
   :static true}
  [^cloiure.lang.Agent a] (.getError a))

(§ defn restart-agent
  "When an agent is failed, changes the agent state to new-state and
  then un-fails the agent so that sends are allowed again.  If
  a :clear-actions true option is given, any actions queued on the
  agent that were being held while it was failed will be discarded,
  otherwise those held actions will proceed.  The new-state must pass
  the validator if any, or restart will throw an exception and the
  agent will remain failed with its old state and error.  Watchers, if
  any, will NOT be notified of the new state.  Throws an exception if
  the agent is not failed."
  {:added "1.2"
   :static true
   }
  [^cloiure.lang.Agent a, new-state & options]
  (let [opts (apply hash-map options)]
    (.restart a new-state (if (:clear-actions opts) true false))))

(§ defn set-error-handler!
  "Sets the error-handler of agent a to handler-fn.  If an action
  being run by the agent throws an exception or doesn't pass the
  validator fn, handler-fn will be called with two arguments: the
  agent and the exception."
  {:added "1.2"
   :static true}
  [^cloiure.lang.Agent a, handler-fn]
  (.setErrorHandler a handler-fn))

(§ defn error-handler
  "Returns the error-handler of agent a, or nil if there is none.
  See set-error-handler!"
  {:added "1.2"
   :static true}
  [^cloiure.lang.Agent a]
  (.getErrorHandler a))

(§ defn set-error-mode!
  "Sets the error-mode of agent a to mode-keyword, which must be
  either :fail or :continue.  If an action being run by the agent
  throws an exception or doesn't pass the validator fn, an
  error-handler may be called (see set-error-handler!), after which,
  if the mode is :continue, the agent will continue as if neither the
  action that caused the error nor the error itself ever happened.

  If the mode is :fail, the agent will become failed and will stop
  accepting new 'send' and 'send-off' actions, and any previously
  queued actions will be held until a 'restart-agent'.  Deref will
  still work, returning the state of the agent before the error."
  {:added "1.2"
   :static true}
  [^cloiure.lang.Agent a, mode-keyword]
  (.setErrorMode a mode-keyword))

(§ defn error-mode
  "Returns the error-mode of agent a.  See set-error-mode!"
  {:added "1.2"
   :static true}
  [^cloiure.lang.Agent a]
  (.getErrorMode a))

(§ defn agent-errors
  "DEPRECATED: Use 'agent-error' instead.
  Returns a sequence of the exceptions thrown during asynchronous
  actions of the agent."
  {:added "1.0"
   :deprecated "1.2"}
  [a]
  (when-let [e (agent-error a)]
    (list e)))

(§ defn clear-agent-errors
  "DEPRECATED: Use 'restart-agent' instead.
  Clears any exceptions thrown during asynchronous actions of the
  agent, allowing subsequent actions to occur."
  {:added "1.0"
   :deprecated "1.2"}
  [^cloiure.lang.Agent a] (restart-agent a (.deref a)))

(§ defn shutdown-agents
  "Initiates a shutdown of the thread pools that back the agent
  system. Running actions will complete, but no new actions will be
  accepted"
  {:added "1.0"
   :static true}
  [] (. cloiure.lang.Agent shutdown))

(§ defn ref
  "Creates and returns a Ref with an initial value of x and zero or
  more options (in any order):

  :meta metadata-map

  :validator validate-fn

  :min-history (default 0)
  :max-history (default 10)

  If metadata-map is supplied, it will become the metadata on the
  ref. validate-fn must be nil or a side-effect-free fn of one
  argument, which will be passed the intended new state on any state
  change. If the new state is unacceptable, the validate-fn should
  return false or throw an exception. validate-fn will be called on
  transaction commit, when all refs have their final values.

  Normally refs accumulate history dynamically as needed to deal with
  read demands. If you know in advance you will need history you can
  set :min-history to ensure it will be available when first needed (instead
  of after a read fault). History is limited, and the limit can be set
  with :max-history."
  {:added "1.0"
   :static true
   }
  ([x] (new cloiure.lang.Ref x))
  ([x & options]
   (let [r  ^cloiure.lang.Ref (setup-reference (ref x) options)
         opts (apply hash-map options)]
    (when (:max-history opts)
      (.setMaxHistory r (:max-history opts)))
    (when (:min-history opts)
      (.setMinHistory r (:min-history opts)))
    r)))

(§ defn ^:private deref-future
  ([^java.util.concurrent.Future fut]
     (.get fut))
  ([^java.util.concurrent.Future fut timeout-ms timeout-val]
     (try (.get fut timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)
          (catch java.util.concurrent.TimeoutException e
            timeout-val))))

(§ defn deref
  "Also reader macro: @ref/@agent/@var/@atom/@delay/@future/@promise. Within a transaction,
  returns the in-transaction-value of ref, else returns the
  most-recently-committed value of ref. When applied to a var, agent
  or atom, returns its current state. When applied to a delay, forces
  it if not already forced. When applied to a future, will block if
  computation not complete. When applied to a promise, will block
  until a value is delivered.  The variant taking a timeout can be
  used for blocking references (futures and promises), and will return
  timeout-val if the timeout (in milliseconds) is reached before a
  value is available. See also - realized?."
  {:added "1.0"
   :static true}
  ([ref] (if (instance? cloiure.lang.IDeref ref)
           (.deref ^cloiure.lang.IDeref ref)
           (deref-future ref)))
  ([ref timeout-ms timeout-val]
     (if (instance? cloiure.lang.IBlockingDeref ref)
       (.deref ^cloiure.lang.IBlockingDeref ref timeout-ms timeout-val)
       (deref-future ref timeout-ms timeout-val))))

(§ defn atom
  "Creates and returns an Atom with an initial value of x and zero or
  more options (in any order):

  :meta metadata-map

  :validator validate-fn

  If metadata-map is supplied, it will become the metadata on the
  atom. validate-fn must be nil or a side-effect-free fn of one
  argument, which will be passed the intended new state on any state
  change. If the new state is unacceptable, the validate-fn should
  return false or throw an exception."
  {:added "1.0"
   :static true}
  ([x] (new cloiure.lang.Atom x))
  ([x & options] (setup-reference (atom x) options)))

(§ defn swap!
  "Atomically swaps the value of atom to be:
  (apply f current-value-of-atom args). Note that f may be called
  multiple times, and thus should be free of side effects.  Returns
  the value that was swapped in."
  {:added "1.0"
   :static true}
  ([^cloiure.lang.IAtom atom f] (.swap atom f))
  ([^cloiure.lang.IAtom atom f x] (.swap atom f x))
  ([^cloiure.lang.IAtom atom f x y] (.swap atom f x y))
  ([^cloiure.lang.IAtom atom f x y & args] (.swap atom f x y args)))

(§ defn swap-vals!
  "Atomically swaps the value of atom to be:
  (apply f current-value-of-atom args). Note that f may be called
  multiple times, and thus should be free of side effects.
  Returns [old new], the value of the atom before and after the swap."
  {:added "1.9"}
  (^cloiure.lang.IPersistentVector [^cloiure.lang.IAtom2 atom f] (.swapVals atom f))
  (^cloiure.lang.IPersistentVector [^cloiure.lang.IAtom2 atom f x] (.swapVals atom f x))
  (^cloiure.lang.IPersistentVector [^cloiure.lang.IAtom2 atom f x y] (.swapVals atom f x y))
  (^cloiure.lang.IPersistentVector [^cloiure.lang.IAtom2 atom f x y & args] (.swapVals atom f x y args)))

(§ defn compare-and-set!
  "Atomically sets the value of atom to newval if and only if the
  current value of the atom is identical to oldval. Returns true if
  set happened, else false"
  {:added "1.0"
   :static true}
  [^cloiure.lang.IAtom atom oldval newval] (.compareAndSet atom oldval newval))

(§ defn reset!
  "Sets the value of atom to newval without regard for the
  current value. Returns newval."
  {:added "1.0"
   :static true}
  [^cloiure.lang.IAtom atom newval] (.reset atom newval))

(§ defn reset-vals!
  "Sets the value of atom to newval. Returns [old new], the value of the
   atom before and after the reset."
  {:added "1.9"}
  ^cloiure.lang.IPersistentVector [^cloiure.lang.IAtom2 atom newval] (.resetVals atom newval))

(§ defn set-validator!
  "Sets the validator-fn for a var/ref/agent/atom. validator-fn must be nil or a
  side-effect-free fn of one argument, which will be passed the intended
  new state on any state change. If the new state is unacceptable, the
  validator-fn should return false or throw an exception. If the current state (root
  value if var) is not acceptable to the new validator, an exception
  will be thrown and the validator will not be changed."
  {:added "1.0"
   :static true}
  [^cloiure.lang.IRef iref validator-fn] (. iref (setValidator validator-fn)))

(§ defn get-validator
  "Gets the validator-fn for a var/ref/agent/atom."
  {:added "1.0"
   :static true}
 [^cloiure.lang.IRef iref] (. iref (getValidator)))

(§ defn alter-meta!
  "Atomically sets the metadata for a namespace/var/ref/agent/atom to be:

  (apply f its-current-meta args)

  f must be free of side-effects"
  {:added "1.0"
   :static true}
 [^cloiure.lang.IReference iref f & args] (.alterMeta iref f args))

(§ defn reset-meta!
  "Atomically resets the metadata for a namespace/var/ref/agent/atom"
  {:added "1.0"
   :static true}
 [^cloiure.lang.IReference iref metadata-map] (.resetMeta iref metadata-map))

(§ defn commute
  "Must be called in a transaction. Sets the in-transaction-value of
  ref to:

  (apply fun in-transaction-value-of-ref args)

  and returns the in-transaction-value of ref.

  At the commit point of the transaction, sets the value of ref to be:

  (apply fun most-recently-committed-value-of-ref args)

  Thus fun should be commutative, or, failing that, you must accept
  last-one-in-wins behavior.  commute allows for more concurrency than
  ref-set."
  {:added "1.0"
   :static true}

  [^cloiure.lang.Ref ref fun & args]
    (. ref (commute fun args)))

(§ defn alter
  "Must be called in a transaction. Sets the in-transaction-value of
  ref to:

  (apply fun in-transaction-value-of-ref args)

  and returns the in-transaction-value of ref."
  {:added "1.0"
   :static true}
  [^cloiure.lang.Ref ref fun & args]
    (. ref (alter fun args)))

(§ defn ref-set
  "Must be called in a transaction. Sets the value of ref.
  Returns val."
  {:added "1.0"
   :static true}
  [^cloiure.lang.Ref ref val]
    (. ref (set val)))

(§ defn ref-history-count
  "Returns the history count of a ref"
  {:added "1.1"
   :static true}
  [^cloiure.lang.Ref ref]
    (.getHistoryCount ref))

(§ defn ref-min-history
  "Gets the min-history of a ref, or sets it and returns the ref"
  {:added "1.1"
   :static true}
  ([^cloiure.lang.Ref ref]
    (.getMinHistory ref))
  ([^cloiure.lang.Ref ref n]
    (.setMinHistory ref n)))

(§ defn ref-max-history
  "Gets the max-history of a ref, or sets it and returns the ref"
  {:added "1.1"
   :static true}
  ([^cloiure.lang.Ref ref]
    (.getMaxHistory ref))
  ([^cloiure.lang.Ref ref n]
    (.setMaxHistory ref n)))

(§ defn ensure
  "Must be called in a transaction. Protects the ref from modification
  by other transactions.  Returns the in-transaction-value of
  ref. Allows for more concurrency than (ref-set ref @ref)"
  {:added "1.0"
   :static true}
  [^cloiure.lang.Ref ref]
    (. ref (touch))
    (. ref (deref)))

(§ defmacro sync
  "transaction-flags => TBD, pass nil for now

  Runs the exprs (in an implicit do) in a transaction that encompasses
  exprs and any nested calls.  Starts a transaction if none is already
  running on this thread. Any uncaught exception will abort the
  transaction and flow out of sync. The exprs may be run more than
  once, but any effects on Refs will be atomic."
  {:added "1.0"}
  [flags-ignored-for-now & body]
  `(. cloiure.lang.LockingTransaction
      (runInTransaction (fn [] ~@body))))

(§ defmacro io!
  "If an io! block occurs in a transaction, throws an
  IllegalStateException, else runs body in an implicit do. If the
  first expression in body is a literal string, will use that as the
  exception message."
  {:added "1.0"}
  [& body]
  (let [message (when (string? (first body)) (first body))
        body (if message (next body) body)]
    `(if (cloiure.lang.LockingTransaction/isRunning)
       (throw (new IllegalStateException ~(or message "I/O in transaction")))
       (do ~@body))))

(§ defn volatile!
  "Creates and returns a Volatile with an initial value of val."
  {:added "1.7"
   :tag cloiure.lang.Volatile}
  [val]
  (cloiure.lang.Volatile. val))

(§ defn vreset!
  "Sets the value of volatile to newval without regard for the
   current value. Returns newval."
  {:added "1.7"}
  [^cloiure.lang.Volatile vol newval]
  (.reset vol newval))

(§ defmacro vswap!
  "Non-atomically swaps the value of the volatile as if:
   (apply f current-value-of-vol args). Returns the value that
   was swapped in."
  {:added "1.7"}
  [vol f & args]
  (let [v (with-meta vol {:tag 'cloiure.lang.Volatile})]
    `(.reset ~v (~f (.deref ~v) ~@args))))

(§ defn volatile?
  "Returns true if x is a volatile."
  {:added "1.7"}
  [x]
  (instance? cloiure.lang.Volatile x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; fn stuff ;;;;;;;;;;;;;;;;

(§ defn comp
  "Takes a set of functions and returns a fn that is the composition
  of those fns.  The returned fn takes a variable number of args,
  applies the rightmost of fns to the args, the next
  fn (right-to-left) to the result, etc."
  {:added "1.0"
   :static true}
  ([] identity)
  ([f] f)
  ([f g]
     (fn
       ([] (f (g)))
       ([x] (f (g x)))
       ([x y] (f (g x y)))
       ([x y z] (f (g x y z)))
       ([x y z & args] (f (apply g x y z args)))))
  ([f g & fs]
     (reduce1 comp (list* f g fs))))

(§ defn juxt
  "Takes a set of functions and returns a fn that is the juxtaposition
  of those fns.  The returned fn takes a variable number of args, and
  returns a vector containing the result of applying each fn to the
  args (left-to-right).
  ((juxt a b c) x) => [(a x) (b x) (c x)]"
  {:added "1.1"
   :static true}
  ([f]
     (fn
       ([] [(f)])
       ([x] [(f x)])
       ([x y] [(f x y)])
       ([x y z] [(f x y z)])
       ([x y z & args] [(apply f x y z args)])))
  ([f g]
     (fn
       ([] [(f) (g)])
       ([x] [(f x) (g x)])
       ([x y] [(f x y) (g x y)])
       ([x y z] [(f x y z) (g x y z)])
       ([x y z & args] [(apply f x y z args) (apply g x y z args)])))
  ([f g h]
     (fn
       ([] [(f) (g) (h)])
       ([x] [(f x) (g x) (h x)])
       ([x y] [(f x y) (g x y) (h x y)])
       ([x y z] [(f x y z) (g x y z) (h x y z)])
       ([x y z & args] [(apply f x y z args) (apply g x y z args) (apply h x y z args)])))
  ([f g h & fs]
     (let [fs (list* f g h fs)]
       (fn
         ([] (reduce1 #(conj %1 (%2)) [] fs))
         ([x] (reduce1 #(conj %1 (%2 x)) [] fs))
         ([x y] (reduce1 #(conj %1 (%2 x y)) [] fs))
         ([x y z] (reduce1 #(conj %1 (%2 x y z)) [] fs))
         ([x y z & args] (reduce1 #(conj %1 (apply %2 x y z args)) [] fs))))))

(§ defn partial
  "Takes a function f and fewer than the normal arguments to f, and
  returns a fn that takes a variable number of additional args. When
  called, the returned function calls f with args + additional args."
  {:added "1.0"
   :static true}
  ([f] f)
  ([f arg1]
   (fn
     ([] (f arg1))
     ([x] (f arg1 x))
     ([x y] (f arg1 x y))
     ([x y z] (f arg1 x y z))
     ([x y z & args] (apply f arg1 x y z args))))
  ([f arg1 arg2]
   (fn
     ([] (f arg1 arg2))
     ([x] (f arg1 arg2 x))
     ([x y] (f arg1 arg2 x y))
     ([x y z] (f arg1 arg2 x y z))
     ([x y z & args] (apply f arg1 arg2 x y z args))))
  ([f arg1 arg2 arg3]
   (fn
     ([] (f arg1 arg2 arg3))
     ([x] (f arg1 arg2 arg3 x))
     ([x y] (f arg1 arg2 arg3 x y))
     ([x y z] (f arg1 arg2 arg3 x y z))
     ([x y z & args] (apply f arg1 arg2 arg3 x y z args))))
  ([f arg1 arg2 arg3 & more]
   (fn [& args] (apply f arg1 arg2 arg3 (concat more args)))))

;;;;;;;;;;;;;;;;;;; sequence fns  ;;;;;;;;;;;;;;;;;;;;;;;

(§ defn sequence
  "Coerces coll to a (possibly empty) sequence, if it is not already
  one. Will not force a lazy seq. (sequence nil) yields (), When a
  transducer is supplied, returns a lazy sequence of applications of
  the transform to the items in coll(s), i.e. to the set of first
  items of each coll, followed by the set of second
  items in each coll, until any one of the colls is exhausted.  Any
  remaining items in other colls are ignored. The transform should accept
  number-of-colls arguments"
  {:added "1.0"
   :static true}
  ([coll]
     (if (seq? coll) coll
         (or (seq coll) ())))
  ([xform coll]
     (or (cloiure.lang.RT/chunkIteratorSeq
         (cloiure.lang.TransformerIterator/create xform (cloiure.lang.RT/iter coll)))
       ()))
  ([xform coll & colls]
     (or (cloiure.lang.RT/chunkIteratorSeq
         (cloiure.lang.TransformerIterator/createMulti
           xform
           (map #(cloiure.lang.RT/iter %) (cons coll colls))))
       ())))

(§ defn every?
  "Returns true if (pred x) is logical true for every x in coll, else
  false."
  {:tag Boolean
   :added "1.0"
   :static true}
  [pred coll]
  (cond
   (nil? (seq coll)) true
   (pred (first coll)) (recur pred (next coll))
   :else false))

(§ def
 ^{:tag Boolean
   :doc "Returns false if (pred x) is logical true for every x in
  coll, else true."
   :arglists '([pred coll])
   :added "1.0"}
 not-every? (comp not every?))

(§ defn some
  "Returns the first logical true value of (pred x) for any x in coll,
  else nil.  One common idiom is to use a set as pred, for example
  this will return :fred if :fred is in the sequence, otherwise nil:
  (some #{:fred} coll)"
  {:added "1.0"
   :static true}
  [pred coll]
    (when (seq coll)
      (or (pred (first coll)) (recur pred (next coll)))))

(§ def
 ^{:tag Boolean
   :doc "Returns false if (pred x) is logical true for any x in coll,
  else true."
   :arglists '([pred coll])
   :added "1.0"}
 not-any? (comp not some))

; will be redefed later with arg checks
(§ defmacro dotimes
  "bindings => name n

  Repeatedly executes body (presumably for side-effects) with name
  bound to integers from 0 through n-1."
  {:added "1.0"}
  [bindings & body]
  (let [i (first bindings)
        n (second bindings)]
    `(let [n# (cloiure.lang.RT/longCast ~n)]
       (loop [~i 0]
         (when (< ~i n#)
           ~@body
           (recur (unchecked-inc ~i)))))))

(§ defn map
  "Returns a lazy sequence consisting of the result of applying f to
  the set of first items of each coll, followed by applying f to the
  set of second items in each coll, until any one of the colls is
  exhausted.  Any remaining items in other colls are ignored. Function
  f should accept number-of-colls arguments. Returns a transducer when
  no collection is provided."
  {:added "1.0"
   :static true}
  ([f]
    (fn [rf]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
           (rf result (f input)))
        ([result input & inputs]
           (rf result (apply f input inputs))))))
  ([f coll]
   (lazy-seq
    (when-let [s (seq coll)]
      (if (chunked-seq? s)
        (let [c (chunk-first s)
              size (int (count c))
              b (chunk-buffer size)]
          (dotimes [i size]
              (chunk-append b (f (.nth c i))))
          (chunk-cons (chunk b) (map f (chunk-rest s))))
        (cons (f (first s)) (map f (rest s)))))))
  ([f c1 c2]
   (lazy-seq
    (let [s1 (seq c1) s2 (seq c2)]
      (when (and s1 s2)
        (cons (f (first s1) (first s2))
              (map f (rest s1) (rest s2)))))))
  ([f c1 c2 c3]
   (lazy-seq
    (let [s1 (seq c1) s2 (seq c2) s3 (seq c3)]
      (when (and  s1 s2 s3)
        (cons (f (first s1) (first s2) (first s3))
              (map f (rest s1) (rest s2) (rest s3)))))))
  ([f c1 c2 c3 & colls]
   (let [step (fn step [cs]
                 (lazy-seq
                  (let [ss (map seq cs)]
                    (when (every? identity ss)
                      (cons (map first ss) (step (map rest ss)))))))]
     (map #(apply f %) (step (conj colls c3 c2 c1))))))

(§ defmacro declare
  "defs the supplied var names with no bindings, useful for making forward declarations."
  {:added "1.0"}
  [& names] `(do ~@(map #(list 'def (vary-meta % assoc :declared true)) names)))

(§ declare cat)

(§ defn mapcat
  "Returns the result of applying concat to the result of applying map
  to f and colls.  Thus function f should return a collection. Returns
  a transducer when no collections are provided"
  {:added "1.0"
   :static true}
  ([f] (comp (map f) cat))
  ([f & colls]
     (apply concat (apply map f colls))))

(§ defn filter
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns logical true. pred must be free of side-effects.
  Returns a transducer when no collection is provided."
  {:added "1.0"
   :static true}
  ([pred]
    (fn [rf]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
           (if (pred input)
             (rf result input)
             result)))))
  ([pred coll]
   (lazy-seq
    (when-let [s (seq coll)]
      (if (chunked-seq? s)
        (let [c (chunk-first s)
              size (count c)
              b (chunk-buffer size)]
          (dotimes [i size]
              (let [v (.nth c i)]
                (when (pred v)
                  (chunk-append b v))))
          (chunk-cons (chunk b) (filter pred (chunk-rest s))))
        (let [f (first s) r (rest s)]
          (if (pred f)
            (cons f (filter pred r))
            (filter pred r))))))))

(§ defn remove
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns logical false. pred must be free of side-effects.
  Returns a transducer when no collection is provided."
  {:added "1.0"
   :static true}
  ([pred] (filter (complement pred)))
  ([pred coll]
     (filter (complement pred) coll)))

(§ defn reduced
  "Wraps x in a way such that a reduce will terminate with the value x"
  {:added "1.5"}
  [x]
  (cloiure.lang.Reduced. x))

(§ defn reduced?
  "Returns true if x is the result of a call to reduced"
  {:inline (fn [x] `(cloiure.lang.RT/isReduced ~x ))
   :inline-arities #{1}
   :added "1.5"}
  ([x] (cloiure.lang.RT/isReduced x)))

(§ defn ensure-reduced
  "If x is already reduced?, returns it, else returns (reduced x)"
  {:added "1.7"}
  [x]
  (if (reduced? x) x (reduced x)))

(§ defn unreduced
  "If x is reduced?, returns (deref x), else returns x"
  {:added "1.7"}
  [x]
  (if (reduced? x) (deref x) x))

(§ defn take
  "Returns a lazy sequence of the first n items in coll, or all items if
  there are fewer than n.  Returns a stateful transducer when
  no collection is provided."
  {:added "1.0"
   :static true}
  ([n]
     (fn [rf]
       (let [nv (volatile! n)]
         (fn
           ([] (rf))
           ([result] (rf result))
           ([result input]
              (let [n @nv
                    nn (vswap! nv dec)
                    result (if (pos? n)
                             (rf result input)
                             result)]
                (if (not (pos? nn))
                  (ensure-reduced result)
                  result)))))))
  ([n coll]
     (lazy-seq
      (when (pos? n)
        (when-let [s (seq coll)]
          (cons (first s) (take (dec n) (rest s))))))))

(§ defn take-while
  "Returns a lazy sequence of successive items from coll while
  (pred item) returns logical true. pred must be free of side-effects.
  Returns a transducer when no collection is provided."
  {:added "1.0"
   :static true}
  ([pred]
     (fn [rf]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
            (if (pred input)
              (rf result input)
              (reduced result))))))
  ([pred coll]
     (lazy-seq
      (when-let [s (seq coll)]
        (when (pred (first s))
          (cons (first s) (take-while pred (rest s))))))))

(§ defn drop
  "Returns a lazy sequence of all but the first n items in coll.
  Returns a stateful transducer when no collection is provided."
  {:added "1.0"
   :static true}
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
                  (rf result input))))))))
  ([n coll]
     (let [step (fn [n coll]
                  (let [s (seq coll)]
                    (if (and (pos? n) s)
                      (recur (dec n) (rest s))
                      s)))]
       (lazy-seq (step n coll)))))

(§ defn drop-last
  "Return a lazy sequence of all but the last n (default 1) items in coll"
  {:added "1.0"
   :static true}
  ([coll] (drop-last 1 coll))
  ([n coll] (map (fn [x _] x) coll (drop n coll))))

(§ defn take-last
  "Returns a seq of the last n items in coll.  Depending on the type
  of coll may be no better than linear time.  For vectors, see also subvec."
  {:added "1.1"
   :static true}
  [n coll]
  (loop [s (seq coll), lead (seq (drop n coll))]
    (if lead
      (recur (next s) (next lead))
      s)))

(§ defn drop-while
  "Returns a lazy sequence of the items in coll starting from the
  first item for which (pred item) returns logical false.  Returns a
  stateful transducer when no collection is provided."
  {:added "1.0"
   :static true}
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
                    (rf result input)))))))))
  ([pred coll]
     (let [step (fn [pred coll]
                  (let [s (seq coll)]
                    (if (and s (pred (first s)))
                      (recur pred (rest s))
                      s)))]
       (lazy-seq (step pred coll)))))

(§ defn cycle
  "Returns a lazy (infinite!) sequence of repetitions of the items in coll."
  {:added "1.0"
   :static true}
  [coll] (cloiure.lang.Cycle/create (seq coll)))

(§ defn split-at
  "Returns a vector of [(take n coll) (drop n coll)]"
  {:added "1.0"
   :static true}
  [n coll]
    [(take n coll) (drop n coll)])

(§ defn split-with
  "Returns a vector of [(take-while pred coll) (drop-while pred coll)]"
  {:added "1.0"
   :static true}
  [pred coll]
    [(take-while pred coll) (drop-while pred coll)])

(§ defn repeat
  "Returns a lazy (infinite!, or length n if supplied) sequence of xs."
  {:added "1.0"
   :static true}
  ([x] (cloiure.lang.Repeat/create x))
  ([n x] (cloiure.lang.Repeat/create n x)))

(§ defn replicate
  "DEPRECATED: Use 'repeat' instead.
   Returns a lazy seq of n xs."
  {:added "1.0"
   :deprecated "1.3"}
  [n x] (take n (repeat x)))

(§ defn iterate
  "Returns a lazy sequence of x, (f x), (f (f x)) etc. f must be free of side-effects"
  {:added "1.0"
   :static true}
  [f x] (cloiure.lang.Iterate/create f x))

(§ defn range
  "Returns a lazy seq of nums from start (inclusive) to end
  (exclusive), by step, where start defaults to 0, step to 1, and end to
  infinity. When step is equal to 0, returns an infinite sequence of
  start. When start is equal to end, returns empty list."
  {:added "1.0"
   :static true}
  ([]
   (iterate inc' 0))
  ([end]
   (if (instance? Long end)
     (cloiure.lang.LongRange/create end)
     (cloiure.lang.Range/create end)))
  ([start end]
   (if (and (instance? Long start) (instance? Long end))
     (cloiure.lang.LongRange/create start end)
     (cloiure.lang.Range/create start end)))
  ([start end step]
   (if (and (instance? Long start) (instance? Long end) (instance? Long step))
     (cloiure.lang.LongRange/create start end step)
     (cloiure.lang.Range/create start end step))))

(§ defn merge
  "Returns a map that consists of the rest of the maps conj-ed onto
  the first.  If a key occurs in more than one map, the mapping from
  the latter (left-to-right) will be the mapping in the result."
  {:added "1.0"
   :static true}
  [& maps]
  (when (some identity maps)
    (reduce1 #(conj (or %1 {}) %2) maps)))

(§ defn merge-with
  "Returns a map that consists of the rest of the maps conj-ed onto
  the first.  If a key occurs in more than one map, the mapping(s)
  from the latter (left-to-right) will be combined with the mapping in
  the result by calling (f val-in-result val-in-latter)."
  {:added "1.0"
   :static true}
  [f & maps]
  (when (some identity maps)
    (let [merge-entry (fn [m e]
                        (let [k (key e) v (val e)]
                          (if (contains? m k)
                            (assoc m k (f (get m k) v))
                            (assoc m k v))))
          merge2 (fn [m1 m2]
                   (reduce1 merge-entry (or m1 {}) (seq m2)))]
      (reduce1 merge2 maps))))

(§ defn zipmap
  "Returns a map with the keys mapped to the corresponding vals."
  {:added "1.0"
   :static true}
  [keys vals]
    (loop [map {}
           ks (seq keys)
           vs (seq vals)]
      (if (and ks vs)
        (recur (assoc map (first ks) (first vs))
               (next ks)
               (next vs))
        map)))

(§ defn line-seq
  "Returns the lines of text from rdr as a lazy sequence of strings.
  rdr must implement java.io.BufferedReader."
  {:added "1.0"
   :static true}
  [^java.io.BufferedReader rdr]
  (when-let [line (.readLine rdr)]
    (cons line (lazy-seq (line-seq rdr)))))

(§ defn comparator
  "Returns an implementation of java.util.Comparator based upon pred."
  {:added "1.0"
   :static true}
  [pred]
    (fn [x y]
      (cond (pred x y) -1 (pred y x) 1 :else 0)))

(§ defn sort
  "Returns a sorted sequence of the items in coll. If no comparator is
  supplied, uses compare.  comparator must implement
  java.util.Comparator.  Guaranteed to be stable: equal elements will
  not be reordered.  If coll is a Java array, it will be modified.  To
  avoid this, sort a copy of the array."
  {:added "1.0"
   :static true}
  ([coll]
   (sort compare coll))
  ([^java.util.Comparator comp coll]
   (if (seq coll)
     (let [a (to-array coll)]
       (. java.util.Arrays (sort a comp))
       (seq a))
     ())))

(§ defn sort-by
  "Returns a sorted sequence of the items in coll, where the sort
  order is determined by comparing (keyfn item).  If no comparator is
  supplied, uses compare.  comparator must implement
  java.util.Comparator.  Guaranteed to be stable: equal elements will
  not be reordered.  If coll is a Java array, it will be modified.  To
  avoid this, sort a copy of the array."
  {:added "1.0"
   :static true}
  ([keyfn coll]
   (sort-by keyfn compare coll))
  ([keyfn ^java.util.Comparator comp coll]
   (sort (fn [x y] (. comp (compare (keyfn x) (keyfn y)))) coll)))

(§ defn dorun
  "When lazy sequences are produced via functions that have side
  effects, any effects other than those needed to produce the first
  element in the seq do not occur until the seq is consumed. dorun can
  be used to force any effects. Walks through the successive nexts of
  the seq, does not retain the head and returns nil."
  {:added "1.0"
   :static true}
  ([coll]
   (when-let [s (seq coll)]
     (recur (next s))))
  ([n coll]
   (when (and (seq coll) (pos? n))
     (recur (dec n) (next coll)))))

(§ defn doall
  "When lazy sequences are produced via functions that have side
  effects, any effects other than those needed to produce the first
  element in the seq do not occur until the seq is consumed. doall can
  be used to force any effects. Walks through the successive nexts of
  the seq, retains the head and returns it, thus causing the entire
  seq to reside in memory at one time."
  {:added "1.0"
   :static true}
  ([coll]
   (dorun coll)
   coll)
  ([n coll]
   (dorun n coll)
   coll))

(§ defn nthnext
  "Returns the nth next of coll, (seq coll) when n is 0."
  {:added "1.0"
   :static true}
  [coll n]
    (loop [n n xs (seq coll)]
      (if (and xs (pos? n))
        (recur (dec n) (next xs))
        xs)))

(§ defn nthrest
  "Returns the nth rest of coll, coll when n is 0."
  {:added "1.3"
   :static true}
  [coll n]
    (loop [n n xs coll]
      (if-let [xs (and (pos? n) (seq xs))]
        (recur (dec n) (rest xs))
        xs)))

(§ defn partition
  "Returns a lazy sequence of lists of n items each, at offsets step
  apart. If step is not supplied, defaults to n, i.e. the partitions
  do not overlap. If a pad collection is supplied, use its elements as
  necessary to complete last partition upto n items. In case there are
  not enough padding elements, return a partition with less than n items."
  {:added "1.0"
   :static true}
  ([n coll]
     (partition n n coll))
  ([n step coll]
     (lazy-seq
       (when-let [s (seq coll)]
         (let [p (doall (take n s))]
           (when (= n (count p))
             (cons p (partition n step (nthrest s step))))))))
  ([n step pad coll]
     (lazy-seq
       (when-let [s (seq coll)]
         (let [p (doall (take n s))]
           (if (= n (count p))
             (cons p (partition n step pad (nthrest s step)))
             (list (take n (concat p pad)))))))))

;; evaluation

(§ defn eval
  "Evaluates the form data structure (not text!) and returns the result."
  {:added "1.0"
   :static true}
  [form] (. cloiure.lang.Compiler (eval form)))

(§ defmacro doseq
  "Repeatedly executes body (presumably for side-effects) with
  bindings and filtering as provided by \"for\".  Does not retain
  the head of the sequence. Returns nil."
  {:added "1.0"}
  [seq-exprs & body]
  (assert-args
     (vector? seq-exprs) "a vector for its binding"
     (even? (count seq-exprs)) "an even number of forms in binding vector")
  (let [step (fn step [recform exprs]
               (if-not exprs
                 [true `(do ~@body)]
                 (let [k (first exprs)
                       v (second exprs)]
                   (if (keyword? k)
                     (let [steppair (step recform (nnext exprs))
                           needrec (steppair 0)
                           subform (steppair 1)]
                       (cond
                         (= k :let) [needrec `(let ~v ~subform)]
                         (= k :while) [false `(when ~v
                                                ~subform
                                                ~@(when needrec [recform]))]
                         (= k :when) [false `(if ~v
                                               (do
                                                 ~subform
                                                 ~@(when needrec [recform]))
                                               ~recform)]))
                     (let [seq- (gensym "seq_")
                           chunk- (with-meta (gensym "chunk_")
                                             {:tag 'cloiure.lang.IChunk})
                           count- (gensym "count_")
                           i- (gensym "i_")
                           recform `(recur (next ~seq-) nil 0 0)
                           steppair (step recform (nnext exprs))
                           needrec (steppair 0)
                           subform (steppair 1)
                           recform-chunk
                             `(recur ~seq- ~chunk- ~count- (unchecked-inc ~i-))
                           steppair-chunk (step recform-chunk (nnext exprs))
                           subform-chunk (steppair-chunk 1)]
                       [true
                        `(loop [~seq- (seq ~v), ~chunk- nil,
                                ~count- 0, ~i- 0]
                           (if (< ~i- ~count-)
                             (let [~k (.nth ~chunk- ~i-)]
                               ~subform-chunk
                               ~@(when needrec [recform-chunk]))
                             (when-let [~seq- (seq ~seq-)]
                               (if (chunked-seq? ~seq-)
                                 (let [c# (chunk-first ~seq-)]
                                   (recur (chunk-rest ~seq-) c#
                                          (int (count c#)) (int 0)))
                                 (let [~k (first ~seq-)]
                                   ~subform
                                   ~@(when needrec [recform]))))))])))))]
    (nth (step nil (seq seq-exprs)) 1)))

(§ defn await
  "Blocks the current thread (indefinitely!) until all actions
  dispatched thus far, from this thread or agent, to the agent(s) have
  occurred.  Will block on failed agents.  Will never return if
  a failed agent is restarted with :clear-actions true or shutdown-agents was called."
  {:added "1.0"
   :static true}
  [& agents]
  (io! "await in transaction"
    (when *agent*
      (throw (new Exception "Can't await in agent action")))
    (let [latch (new java.util.concurrent.CountDownLatch (count agents))
          count-down (fn [agent] (. latch (countDown)) agent)]
      (doseq [agent agents]
        (send agent count-down))
      (. latch (await)))))

(§ defn ^:static await1 [^cloiure.lang.Agent a]
  (when (pos? (.getQueueCount a))
    (await a))
    a)

(§ defn await-for
  "Blocks the current thread until all actions dispatched thus
  far (from this thread or agent) to the agents have occurred, or the
  timeout (in milliseconds) has elapsed. Returns logical false if
  returning due to timeout, logical true otherwise."
  {:added "1.0"
   :static true}
  [timeout-ms & agents]
    (io! "await-for in transaction"
     (when *agent*
       (throw (new Exception "Can't await in agent action")))
     (let [latch (new java.util.concurrent.CountDownLatch (count agents))
           count-down (fn [agent] (. latch (countDown)) agent)]
       (doseq [agent agents]
           (send agent count-down))
       (. latch (await  timeout-ms (. java.util.concurrent.TimeUnit MILLISECONDS))))))

(§ defmacro dotimes
  "bindings => name n

  Repeatedly executes body (presumably for side-effects) with name
  bound to integers from 0 through n-1."
  {:added "1.0"}
  [bindings & body]
  (assert-args
     (vector? bindings) "a vector for its binding"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
  (let [i (first bindings)
        n (second bindings)]
    `(let [n# (long ~n)]
       (loop [~i 0]
         (when (< ~i n#)
           ~@body
           (recur (unchecked-inc ~i)))))))

#_(defn into
  "Returns a new coll consisting of to-coll with all of the items of
  from-coll conjoined."
  {:added "1.0"}
  [to from]
    (let [ret to items (seq from)]
      (if items
        (recur (conj ret (first items)) (next items))
        ret)))

;;;;;;;;;;;;;;;;;;;;; editable collections ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(§ defn transient
  "Returns a new, transient version of the collection, in constant time."
  {:added "1.1"
   :static true}
  [^cloiure.lang.IEditableCollection coll]
  (.asTransient coll))

(§ defn persistent!
  "Returns a new, persistent version of the transient collection, in
  constant time. The transient collection cannot be used after this
  call, any such use will throw an exception."
  {:added "1.1"
   :static true}
  [^cloiure.lang.ITransientCollection coll]
  (.persistent coll))

(§ defn conj!
  "Adds x to the transient collection, and return coll. The 'addition'
  may happen at different 'places' depending on the concrete type."
  {:added "1.1"
   :static true}
  ([] (transient []))
  ([coll] coll)
  ([^cloiure.lang.ITransientCollection coll x]
     (.conj coll x)))

(§ defn assoc!
  "When applied to a transient map, adds mapping of key(s) to
  val(s). When applied to a transient vector, sets the val at index.
  Note - index must be <= (count vector). Returns coll."
  {:added "1.1"
   :static true}
  ([^cloiure.lang.ITransientAssociative coll key val] (.assoc coll key val))
  ([^cloiure.lang.ITransientAssociative coll key val & kvs]
   (let [ret (.assoc coll key val)]
     (if kvs
       (recur ret (first kvs) (second kvs) (nnext kvs))
       ret))))

(§ defn dissoc!
  "Returns a transient map that doesn't contain a mapping for key(s)."
  {:added "1.1"
   :static true}
  ([^cloiure.lang.ITransientMap map key] (.without map key))
  ([^cloiure.lang.ITransientMap map key & ks]
   (let [ret (.without map key)]
     (if ks
       (recur ret (first ks) (next ks))
       ret))))

(§ defn pop!
  "Removes the last item from a transient vector. If
  the collection is empty, throws an exception. Returns coll"
  {:added "1.1"
   :static true}
  [^cloiure.lang.ITransientVector coll]
  (.pop coll))

(§ defn disj!
  "disj[oin]. Returns a transient set of the same (hashed/sorted) type, that
  does not contain key(s)."
  {:added "1.1"
   :static true}
  ([set] set)
  ([^cloiure.lang.ITransientSet set key]
   (. set (disjoin key)))
  ([^cloiure.lang.ITransientSet set key & ks]
   (let [ret (. set (disjoin key))]
     (if ks
       (recur ret (first ks) (next ks))
       ret))))

; redef into with batch support
(§ defn ^:private into1
  "Returns a new coll consisting of to-coll with all of the items of
  from-coll conjoined."
  {:added "1.0"
   :static true}
  [to from]
  (if (instance? cloiure.lang.IEditableCollection to)
    (persistent! (reduce1 conj! (transient to) from))
    (reduce1 conj to from)))

(§ defmacro import
  "import-list => (package-symbol class-name-symbols*)

  For each name in class-name-symbols, adds a mapping from name to the
  class named by package.name to the current namespace. Use :import in the ns
  macro in preference to calling this directly."
  {:added "1.0"}
  [& import-symbols-or-lists]
  (let [specs (map #(if (and (seq? %) (= 'quote (first %))) (second %) %)
                   import-symbols-or-lists)]
    `(do ~@(map #(list 'cloiure.core/import* %)
                (reduce1 (fn [v spec]
                          (if (symbol? spec)
                            (conj v (name spec))
                            (let [p (first spec) cs (rest spec)]
                              (into1 v (map #(str p "." %) cs)))))
                        [] specs)))))

(§ defn into-array
  "Returns an array with components set to the values in aseq. The array's
  component type is type if provided, or the type of the first value in
  aseq if present, or Object. All values in aseq must be compatible with
  the component type. Class objects for the primitive types can be obtained
  using, e.g., Integer/TYPE."
  {:added "1.0"
   :static true}
  ([aseq]
     (cloiure.lang.RT/seqToTypedArray (seq aseq)))
  ([type aseq]
     (cloiure.lang.RT/seqToTypedArray type (seq aseq))))

(§ defn ^{:private true}
  array [& items]
    (into-array items))

(§ defn class
  "Returns the Class of x"
  {:added "1.0"
   :static true}
  ^Class [^Object x] (if (nil? x) x (. x (getClass))))

(§ defn type
  "Returns the :type metadata of x, or its Class if none"
  {:added "1.0"
   :static true}
  [x]
  (or (get (meta x) :type) (class x)))

(§ defn num
  "Coerce to Number"
  {:tag Number
   :inline (fn  [x] `(. cloiure.lang.Numbers (num ~x)))
   :added "1.0"}
  [x] (. cloiure.lang.Numbers (num x)))

(§ defn long
  "Coerce to long"
  {:inline (fn  [x] `(. cloiure.lang.RT (longCast ~x)))
   :added "1.0"}
  [^Number x] (cloiure.lang.RT/longCast x))

(§ defn float
  "Coerce to float"
  {:inline (fn  [x] `(. cloiure.lang.RT (~(if *unchecked-math* 'uncheckedFloatCast 'floatCast) ~x)))
   :added "1.0"}
  [^Number x] (cloiure.lang.RT/floatCast x))

(§ defn double
  "Coerce to double"
  {:inline (fn  [x] `(. cloiure.lang.RT (doubleCast ~x)))
   :added "1.0"}
  [^Number x] (cloiure.lang.RT/doubleCast x))

(§ defn short
  "Coerce to short"
  {:inline (fn  [x] `(. cloiure.lang.RT (~(if *unchecked-math* 'uncheckedShortCast 'shortCast) ~x)))
   :added "1.0"}
  [^Number x] (cloiure.lang.RT/shortCast x))

(§ defn byte
  "Coerce to byte"
  {:inline (fn  [x] `(. cloiure.lang.RT (~(if *unchecked-math* 'uncheckedByteCast 'byteCast) ~x)))
   :added "1.0"}
  [^Number x] (cloiure.lang.RT/byteCast x))

(§ defn char
  "Coerce to char"
  {:inline (fn  [x] `(. cloiure.lang.RT (~(if *unchecked-math* 'uncheckedCharCast 'charCast) ~x)))
   :added "1.1"}
  [x] (. cloiure.lang.RT (charCast x)))

(§ defn unchecked-byte
  "Coerce to byte. Subject to rounding or truncation."
  {:inline (fn  [x] `(. cloiure.lang.RT (uncheckedByteCast ~x)))
   :added "1.3"}
  [^Number x] (cloiure.lang.RT/uncheckedByteCast x))

(§ defn unchecked-short
  "Coerce to short. Subject to rounding or truncation."
  {:inline (fn  [x] `(. cloiure.lang.RT (uncheckedShortCast ~x)))
   :added "1.3"}
  [^Number x] (cloiure.lang.RT/uncheckedShortCast x))

(§ defn unchecked-char
  "Coerce to char. Subject to rounding or truncation."
  {:inline (fn  [x] `(. cloiure.lang.RT (uncheckedCharCast ~x)))
   :added "1.3"}
  [x] (. cloiure.lang.RT (uncheckedCharCast x)))

(§ defn unchecked-int
  "Coerce to int. Subject to rounding or truncation."
  {:inline (fn  [x] `(. cloiure.lang.RT (uncheckedIntCast ~x)))
   :added "1.3"}
  [^Number x] (cloiure.lang.RT/uncheckedIntCast x))

(§ defn unchecked-long
  "Coerce to long. Subject to rounding or truncation."
  {:inline (fn  [x] `(. cloiure.lang.RT (uncheckedLongCast ~x)))
   :added "1.3"}
  [^Number x] (cloiure.lang.RT/uncheckedLongCast x))

(§ defn unchecked-float
  "Coerce to float. Subject to rounding."
  {:inline (fn  [x] `(. cloiure.lang.RT (uncheckedFloatCast ~x)))
   :added "1.3"}
  [^Number x] (cloiure.lang.RT/uncheckedFloatCast x))

(§ defn unchecked-double
  "Coerce to double. Subject to rounding."
  {:inline (fn  [x] `(. cloiure.lang.RT (uncheckedDoubleCast ~x)))
   :added "1.3"}
  [^Number x] (cloiure.lang.RT/uncheckedDoubleCast x))

(§ defn number?
  "Returns true if x is a Number"
  {:added "1.0"
   :static true}
  [x]
  (instance? Number x))

(§ defn mod
  "Modulus of num and div. Truncates toward negative infinity."
  {:added "1.0"
   :static true}
  [num div]
  (let [m (rem num div)]
    (if (or (zero? m) (= (pos? num) (pos? div)))
      m
      (+ m div))))

(§ defn ratio?
  "Returns true if n is a Ratio"
  {:added "1.0"
   :static true}
  [n] (instance? cloiure.lang.Ratio n))

(§ defn numerator
  "Returns the numerator part of a Ratio."
  {:tag BigInteger
   :added "1.2"
   :static true}
  [r]
  (.numerator ^cloiure.lang.Ratio r))

(§ defn denominator
  "Returns the denominator part of a Ratio."
  {:tag BigInteger
   :added "1.2"
   :static true}
  [r]
  (.denominator ^cloiure.lang.Ratio r))

(§ defn decimal?
  "Returns true if n is a BigDecimal"
  {:added "1.0"
   :static true}
  [n] (instance? BigDecimal n))

(§ defn float?
  "Returns true if n is a floating point number"
  {:added "1.0"
   :static true}
  [n]
  (or (instance? Double n)
      (instance? Float n)))

(§ defn rational?
  "Returns true if n is a rational number"
  {:added "1.0"
   :static true}
  [n]
  (or (integer? n) (ratio? n) (decimal? n)))

(§ defn bigint
  "Coerce to BigInt"
  {:tag cloiure.lang.BigInt
   :static true
   :added "1.3"}
  [x] (cond
       (instance? cloiure.lang.BigInt x) x
       (instance? BigInteger x) (cloiure.lang.BigInt/fromBigInteger x)
       (decimal? x) (bigint (.toBigInteger ^BigDecimal x))
       (float? x)  (bigint (. BigDecimal valueOf (double x)))
       (ratio? x) (bigint (.bigIntegerValue ^cloiure.lang.Ratio x))
       (number? x) (cloiure.lang.BigInt/valueOf (long x))
       :else (bigint (BigInteger. x))))

(§ defn biginteger
  "Coerce to BigInteger"
  {:tag BigInteger
   :added "1.0"
   :static true}
  [x] (cond
       (instance? BigInteger x) x
       (instance? cloiure.lang.BigInt x) (.toBigInteger ^cloiure.lang.BigInt x)
       (decimal? x) (.toBigInteger ^BigDecimal x)
       (float? x) (.toBigInteger (. BigDecimal valueOf (double x)))
       (ratio? x) (.bigIntegerValue ^cloiure.lang.Ratio x)
       (number? x) (BigInteger/valueOf (long x))
       :else (BigInteger. x)))

(§ defn bigdec
  "Coerce to BigDecimal"
  {:tag BigDecimal
   :added "1.0"
   :static true}
  [x] (cond
       (decimal? x) x
       (float? x) (. BigDecimal valueOf (double x))
       (ratio? x) (/ (BigDecimal. (.numerator ^cloiure.lang.Ratio x)) (.denominator ^cloiure.lang.Ratio x))
       (instance? cloiure.lang.BigInt x) (.toBigDecimal ^cloiure.lang.BigInt x)
       (instance? BigInteger x) (BigDecimal. ^BigInteger x)
       (number? x) (BigDecimal/valueOf (long x))
       :else (BigDecimal. x)))

(§ def ^:dynamic ^{:private true} print-initialized false)

(§ defmulti print-method (fn [x writer]
                         (let [t (get (meta x) :type)]
                           (if (keyword? t) t (class x)))))
(§ defmulti print-dup (fn [x writer] (class x)))

(§ defn pr-on
  {:private true
   :static true}
  [x w]
  (if *print-dup*
    (print-dup x w)
    (print-method x w))
  nil)

(§ defn pr
  "Prints the object(s) to the output stream that is the current value
  of *out*.  Prints the object(s), separated by spaces if there is
  more than one.  By default, pr and prn print in a way that objects
  can be read by the reader"
  {:dynamic true
   :added "1.0"}
  ([] nil)
  ([x]
     (pr-on x *out*))
  ([x & more]
   (pr x)
   (. *out* (append \space))
   (if-let [nmore (next more)]
     (recur (first more) nmore)
     (apply pr more))))

(§ def ^:private ^String system-newline
     (System/getProperty "line.separator"))

(§ defn newline
  "Writes a platform-specific newline to *out*"
  {:added "1.0"
   :static true}
  []
    (. *out* (append system-newline))
    nil)

(§ defn flush
  "Flushes the output stream that is the current value of
  *out*"
  {:added "1.0"
   :static true}
  []
    (. *out* (flush))
    nil)

(§ defn prn
  "Same as pr followed by (newline). Observes *flush-on-newline*"
  {:added "1.0"
   :static true}
  [& more]
    (apply pr more)
    (newline)
    (when *flush-on-newline*
      (flush)))

(§ defn print
  "Prints the object(s) to the output stream that is the current value
  of *out*.  print and println produce output for human consumption."
  {:added "1.0"
   :static true}
  [& more]
    (binding [*print-readably* nil]
      (apply pr more)))

(§ defn println
  "Same as print followed by (newline)"
  {:added "1.0"
   :static true}
  [& more]
    (binding [*print-readably* nil]
      (apply prn more)))

(§ defn read
  "Reads the next object from stream, which must be an instance of
  java.io.PushbackReader or some derivee.  stream defaults to the
  current value of *in*.

  Opts is a persistent map with valid keys:
    :read-cond - :allow to process reader conditionals, or
                 :preserve to keep all branches
    :features - persistent set of feature keywords for reader conditionals
    :eof - on eof, return value unless :eofthrow, then throw.
           if not specified, will throw

  Note that read can execute code (controlled by *read-eval*),
  and as such should be used only with trusted sources.

  For data structure interop use cloiure.edn/read"
  {:added "1.0"
   :static true}
  ([]
   (read *in*))
  ([stream]
   (read stream true nil))
  ([stream eof-error? eof-value]
   (read stream eof-error? eof-value false))
  ([stream eof-error? eof-value recursive?]
   (. cloiure.lang.LispReader (read stream (boolean eof-error?) eof-value recursive?)))
  ([opts stream]
   (. cloiure.lang.LispReader (read stream opts))))

(§ defn read-line
  "Reads the next line from stream that is the current value of *in* ."
  {:added "1.0"
   :static true}
  []
  (if (instance? cloiure.lang.LineNumberingPushbackReader *in*)
    (.readLine ^cloiure.lang.LineNumberingPushbackReader *in*)
    (.readLine ^java.io.BufferedReader *in*)))

(§ defn read-string
  "Reads one object from the string s. Optionally include reader
  options, as specified in read.

  Note that read-string can execute code (controlled by *read-eval*),
  and as such should be used only with trusted sources.

  For data structure interop use cloiure.edn/read-string"
  {:added "1.0"
   :static true}
  ([s] (cloiure.lang.RT/readString s))
  ([opts s] (cloiure.lang.RT/readString s opts)))

(§ defn subvec
  "Returns a persistent vector of the items in vector from
  start (inclusive) to end (exclusive).  If end is not supplied,
  defaults to (count vector). This operation is O(1) and very fast, as
  the resulting vector shares structure with the original and no
  trimming is done."
  {:added "1.0"
   :static true}
  ([v start]
   (subvec v start (count v)))
  ([v start end]
   (. cloiure.lang.RT (subvec v start end))))

(§ defmacro with-open
  "bindings => [name init ...]

  Evaluates body in a try expression with names bound to the values
  of the inits, and a finally clause that calls (.close name) on each
  name in reverse order."
  {:added "1.0"}
  [bindings & body]
  (assert-args
     (vector? bindings) "a vector for its binding"
     (even? (count bindings)) "an even number of forms in binding vector")
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-open ~(subvec bindings 2) ~@body)
                                (finally
                                  (. ~(bindings 0) close))))
    :else (throw (IllegalArgumentException.
                   "with-open only allows Symbols in bindings"))))

(§ defmacro doto
  "Evaluates x then calls all of the methods and functions with the
  value of x supplied at the front of the given arguments.  The forms
  are evaluated in order.  Returns x.

  (doto (new java.util.HashMap) (.put \"a\" 1) (.put \"b\" 2))"
  {:added "1.0"}
  [x & forms]
    (let [gx (gensym)]
      `(let [~gx ~x]
         ~@(map (fn [f]
                  (with-meta
                    (if (seq? f)
                      `(~(first f) ~gx ~@(next f))
                      `(~f ~gx))
                    (meta f)))
                forms)
         ~gx)))

(§ defmacro memfn
  "Expands into code that creates a fn that expects to be passed an
  object and any args and calls the named instance method on the
  object passing the args. Use when you want to treat a Java method as
  a first-class fn. name may be type-hinted with the method receiver's
  type in order to avoid reflective calls."
  {:added "1.0"}
  [name & args]
  (let [t (with-meta (gensym "target")
            (meta name))]
    `(fn [~t ~@args]
       (. ~t (~name ~@args)))))

(§ defmacro time
  "Evaluates expr and prints the time it took.  Returns the value of
 expr."
  {:added "1.0"}
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (prn (str "Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))

(§ import '(java.lang.reflect Array))

(§ defn alength
  "Returns the length of the Java array. Works on arrays of all
  types."
  {:inline (fn [a] `(. cloiure.lang.RT (alength ~a)))
   :added "1.0"}
  [array] (. cloiure.lang.RT (alength array)))

(§ defn aclone
  "Returns a clone of the Java array. Works on arrays of known
  types."
  {:inline (fn [a] `(. cloiure.lang.RT (aclone ~a)))
   :added "1.0"}
  [array] (. cloiure.lang.RT (aclone array)))

(§ defn aget
  "Returns the value at the index/indices. Works on Java arrays of all
  types."
  {:inline (fn [a i] `(. cloiure.lang.RT (aget ~a (int ~i))))
   :inline-arities #{2}
   :added "1.0"}
  ([array idx]
   (cloiure.lang.Reflector/prepRet (.getComponentType (class array)) (. Array (get array idx))))
  ([array idx & idxs]
   (apply aget (aget array idx) idxs)))

(§ defn aset
  "Sets the value at the index/indices. Works on Java arrays of
  reference types. Returns val."
  {:inline (fn [a i v] `(. cloiure.lang.RT (aset ~a (int ~i) ~v)))
   :inline-arities #{3}
   :added "1.0"}
  ([array idx val]
   (. Array (set array idx val))
   val)
  ([array idx idx2 & idxv]
   (apply aset (aget array idx) idx2 idxv)))

(§ defmacro
  ^{:private true}
  def-aset [name method coerce]
    `(defn ~name
       {:arglists '([~'array ~'idx ~'val] [~'array ~'idx ~'idx2 & ~'idxv])}
       ([array# idx# val#]
        (. Array (~method array# idx# (~coerce val#)))
        val#)
       ([array# idx# idx2# & idxv#]
        (apply ~name (aget array# idx#) idx2# idxv#))))

(§ def-aset
  ^{:doc "Sets the value at the index/indices. Works on arrays of int. Returns val."
    :added "1.0"}
  aset-int setInt int)

(§ def-aset
  ^{:doc "Sets the value at the index/indices. Works on arrays of long. Returns val."
    :added "1.0"}
  aset-long setLong long)

(§ def-aset
  ^{:doc "Sets the value at the index/indices. Works on arrays of boolean. Returns val."
    :added "1.0"}
  aset-boolean setBoolean boolean)

(§ def-aset
  ^{:doc "Sets the value at the index/indices. Works on arrays of float. Returns val."
    :added "1.0"}
  aset-float setFloat float)

(§ def-aset
  ^{:doc "Sets the value at the index/indices. Works on arrays of double. Returns val."
    :added "1.0"}
  aset-double setDouble double)

(§ def-aset
  ^{:doc "Sets the value at the index/indices. Works on arrays of short. Returns val."
    :added "1.0"}
  aset-short setShort short)

(§ def-aset
  ^{:doc "Sets the value at the index/indices. Works on arrays of byte. Returns val."
    :added "1.0"}
  aset-byte setByte byte)

(§ def-aset
  ^{:doc "Sets the value at the index/indices. Works on arrays of char. Returns val."
    :added "1.0"}
  aset-char setChar char)

(§ defn make-array
  "Creates and returns an array of instances of the specified class of
  the specified dimension(s).  Note that a class object is required.
  Class objects can be obtained by using their imported or
  fully-qualified name.  Class objects for the primitive types can be
  obtained using, e.g., Integer/TYPE."
  {:added "1.0"
   :static true}
  ([^Class type len]
   (. Array (newInstance type (int len))))
  ([^Class type dim & more-dims]
   (let [dims (cons dim more-dims)
         ^"[I" dimarray (make-array (. Integer TYPE)  (count dims))]
     (dotimes [i (alength dimarray)]
       (aset-int dimarray i (nth dims i)))
     (. Array (newInstance type dimarray)))))

(§ defn to-array-2d
  "Returns a (potentially-ragged) 2-dimensional array of Objects
  containing the contents of coll, which can be any Collection of any
  Collection."
  {:tag "[[Ljava.lang.Object;"
   :added "1.0"
   :static true}
  [^java.util.Collection coll]
    (let [ret (make-array (. Class (forName "[Ljava.lang.Object;")) (. coll (size)))]
      (loop [i 0 xs (seq coll)]
        (when xs
          (aset ret i (to-array (first xs)))
          (recur (inc i) (next xs))))
      ret))

(§ defn macroexpand-1
  "If form represents a macro form, returns its expansion,
  else returns form."
  {:added "1.0"
   :static true}
  [form]
    (. cloiure.lang.Compiler (macroexpand1 form)))

(§ defn macroexpand
  "Repeatedly calls macroexpand-1 on form until it no longer
  represents a macro form, then returns it.  Note neither
  macroexpand-1 nor macroexpand expand macros in subforms."
  {:added "1.0"
   :static true}
  [form]
    (let [ex (macroexpand-1 form)]
      (if (identical? ex form)
        form
        (macroexpand ex))))

(§ defn load-reader
  "Sequentially read and evaluate the set of forms contained in the
  stream/file"
  {:added "1.0"
   :static true}
  [rdr] (. cloiure.lang.Compiler (load rdr)))

(§ defn load-string
  "Sequentially read and evaluate the set of forms contained in the
  string"
  {:added "1.0"
   :static true}
  [s]
  (let [rdr (-> (java.io.StringReader. s)
                (cloiure.lang.LineNumberingPushbackReader.))]
    (load-reader rdr)))

(§ defn set?
  "Returns true if x implements IPersistentSet"
  {:added "1.0"
   :static true}
  [x] (instance? cloiure.lang.IPersistentSet x))

(§ defn set
  "Returns a set of the distinct elements of coll."
  {:added "1.0"
   :static true}
  [coll]
  (if (set? coll)
    (with-meta coll nil)
    (if (instance? cloiure.lang.IReduceInit coll)
      (persistent! (.reduce ^cloiure.lang.IReduceInit coll conj! (transient #{})))
      (persistent! (reduce1 conj! (transient #{}) coll)))))

(§ defn ^{:private true
   :static true}
  filter-key [keyfn pred amap]
    (loop [ret {} es (seq amap)]
      (if es
        (if (pred (keyfn (first es)))
          (recur (assoc ret (key (first es)) (val (first es))) (next es))
          (recur ret (next es)))
        ret)))

(§ defn find-ns
  "Returns the namespace named by the symbol or nil if it doesn't exist."
  {:added "1.0"
   :static true}
  [sym] (cloiure.lang.Namespace/find sym))

(§ defn create-ns
  "Create a new namespace named by the symbol if one doesn't already
  exist, returns it or the already-existing namespace of the same
  name."
  {:added "1.0"
   :static true}
  [sym] (cloiure.lang.Namespace/findOrCreate sym))

(§ defn remove-ns
  "Removes the namespace named by the symbol. Use with caution.
  Cannot be used to remove the cloiure namespace."
  {:added "1.0"
   :static true}
  [sym] (cloiure.lang.Namespace/remove sym))

(§ defn all-ns
  "Returns a sequence of all namespaces."
  {:added "1.0"
   :static true}
  [] (cloiure.lang.Namespace/all))

(§ defn the-ns
  "If passed a namespace, returns it. Else, when passed a symbol,
  returns the namespace named by it, throwing an exception if not
  found."
  {:added "1.0"
   :static true}
  ^cloiure.lang.Namespace [x]
  (if (instance? cloiure.lang.Namespace x)
    x
    (or (find-ns x) (throw (Exception. (str "No namespace: " x " found"))))))

(§ defn ns-name
  "Returns the name of the namespace, a symbol."
  {:added "1.0"
   :static true}
  [ns]
  (.getName (the-ns ns)))

(§ defn ns-map
  "Returns a map of all the mappings for the namespace."
  {:added "1.0"
   :static true}
  [ns]
  (.getMappings (the-ns ns)))

(§ defn ns-unmap
  "Removes the mappings for the symbol from the namespace."
  {:added "1.0"
   :static true}
  [ns sym]
  (.unmap (the-ns ns) sym))

;(defn export [syms]
;  (doseq [sym syms]
;   (.. *ns* (intern sym) (setExported true))))

(§ defn ns-publics
  "Returns a map of the public intern mappings for the namespace."
  {:added "1.0"
   :static true}
  [ns]
  (let [ns (the-ns ns)]
    (filter-key val (fn [^cloiure.lang.Var v] (and (instance? cloiure.lang.Var v)
                                 (= ns (.ns v))
                                 (.isPublic v)))
                (ns-map ns))))

(§ defn ns-imports
  "Returns a map of the import mappings for the namespace."
  {:added "1.0"
   :static true}
  [ns]
  (filter-key val (partial instance? Class) (ns-map ns)))

(§ defn ns-interns
  "Returns a map of the intern mappings for the namespace."
  {:added "1.0"
   :static true}
  [ns]
  (let [ns (the-ns ns)]
    (filter-key val (fn [^cloiure.lang.Var v] (and (instance? cloiure.lang.Var v)
                                 (= ns (.ns v))))
                (ns-map ns))))

(§ defn refer
  "refers to all public vars of ns, subject to filters.
  filters can include at most one each of:

  :exclude list-of-symbols
  :only list-of-symbols
  :rename map-of-fromsymbol-tosymbol

  For each public interned var in the namespace named by the symbol,
  adds a mapping from the name of the var to the var to the current
  namespace.  Throws an exception if name is already mapped to
  something else in the current namespace. Filters can be used to
  select a subset, via inclusion or exclusion, or to provide a mapping
  to a symbol different from the var's name, in order to prevent
  clashes. Use :use in the ns macro in preference to calling this directly."
  {:added "1.0"}
  [ns-sym & filters]
    (let [ns (or (find-ns ns-sym) (throw (new Exception (str "No namespace: " ns-sym))))
          fs (apply hash-map filters)
          nspublics (ns-publics ns)
          rename (or (:rename fs) {})
          exclude (set (:exclude fs))
          to-do (if (= :all (:refer fs))
                  (keys nspublics)
                  (or (:refer fs) (:only fs) (keys nspublics)))]
      (when (and to-do (not (instance? cloiure.lang.Sequential to-do)))
        (throw (new Exception ":only/:refer value must be a sequential collection of symbols")))
      (doseq [sym to-do]
        (when-not (exclude sym)
          (let [v (nspublics sym)]
            (when-not v
              (throw (new java.lang.IllegalAccessError
                          (if (get (ns-interns ns) sym)
                            (str sym " is not public")
                            (str sym " does not exist")))))
            (. *ns* (refer (or (rename sym) sym) v)))))))

(§ defn ns-refers
  "Returns a map of the refer mappings for the namespace."
  {:added "1.0"
   :static true}
  [ns]
  (let [ns (the-ns ns)]
    (filter-key val (fn [^cloiure.lang.Var v] (and (instance? cloiure.lang.Var v)
                                 (not= ns (.ns v))))
                (ns-map ns))))

(§ defn alias
  "Add an alias in the current namespace to another
  namespace. Arguments are two symbols: the alias to be used, and
  the symbolic name of the target namespace. Use :as in the ns macro in preference
  to calling this directly."
  {:added "1.0"
   :static true}
  [alias namespace-sym]
  (.addAlias *ns* alias (the-ns namespace-sym)))

(§ defn ns-aliases
  "Returns a map of the aliases for the namespace."
  {:added "1.0"
   :static true}
  [ns]
  (.getAliases (the-ns ns)))

(§ defn ns-unalias
  "Removes the alias for the symbol from the namespace."
  {:added "1.0"
   :static true}
  [ns sym]
  (.removeAlias (the-ns ns) sym))

(§ defn take-nth
  "Returns a lazy seq of every nth item in coll.  Returns a stateful
  transducer when no collection is provided."
  {:added "1.0"
   :static true}
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
                  result)))))))
  ([n coll]
     (lazy-seq
      (when-let [s (seq coll)]
        (cons (first s) (take-nth n (drop n s)))))))

(§ defn interleave
  "Returns a lazy seq of the first item in each coll, then the second etc."
  {:added "1.0"
   :static true}
  ([] ())
  ([c1] (lazy-seq c1))
  ([c1 c2]
     (lazy-seq
      (let [s1 (seq c1) s2 (seq c2)]
        (when (and s1 s2)
          (cons (first s1) (cons (first s2)
                                 (interleave (rest s1) (rest s2))))))))
  ([c1 c2 & colls]
     (lazy-seq
      (let [ss (map seq (conj colls c2 c1))]
        (when (every? identity ss)
          (concat (map first ss) (apply interleave (map rest ss))))))))

(§ defn var-get
  "Gets the value in the var object"
  {:added "1.0"
   :static true}
  [^cloiure.lang.Var x] (. x (get)))

(§ defn var-set
  "Sets the value in the var object to val. The var must be
 thread-locally bound."
  {:added "1.0"
   :static true}
  [^cloiure.lang.Var x val] (. x (set val)))

(§ defmacro with-local-vars
  "varbinding=> symbol init-expr

  Executes the exprs in a context in which the symbols are bound to
  vars with per-thread bindings to the init-exprs.  The symbols refer
  to the var objects themselves, and must be accessed with var-get and
  var-set"
  {:added "1.0"}
  [name-vals-vec & body]
  (assert-args
     (vector? name-vals-vec) "a vector for its binding"
     (even? (count name-vals-vec)) "an even number of forms in binding vector")
  `(let [~@(interleave (take-nth 2 name-vals-vec)
                       (repeat '(.. cloiure.lang.Var create setDynamic)))]
     (. cloiure.lang.Var (pushThreadBindings (hash-map ~@name-vals-vec)))
     (try
      ~@body
      (finally (. cloiure.lang.Var (popThreadBindings))))))

(§ defn ns-resolve
  "Returns the var or Class to which a symbol will be resolved in the
  namespace (unless found in the environment), else nil.  Note that
  if the symbol is fully qualified, the var/Class to which it resolves
  need not be present in the namespace."
  {:added "1.0"
   :static true}
  ([ns sym]
    (ns-resolve ns nil sym))
  ([ns env sym]
    (when-not (contains? env sym)
      (cloiure.lang.Compiler/maybeResolveIn (the-ns ns) sym))))

(§ defn resolve
  "same as (ns-resolve *ns* symbol) or (ns-resolve *ns* &env symbol)"
  {:added "1.0"
   :static true}
  ([sym] (ns-resolve *ns* sym))
  ([env sym] (ns-resolve *ns* env sym)))

(§ defn array-map
  "Constructs an array-map. If any keys are equal, they are handled as
  if by repeated uses of assoc."
  {:added "1.0"
   :static true}
  ([] (. cloiure.lang.PersistentArrayMap EMPTY))
  ([& keyvals]
     (cloiure.lang.PersistentArrayMap/createAsIfByAssoc (to-array keyvals))))

;; redefine let and loop  with destructuring
(§ defn destructure [bindings]
  (let [bents (partition 2 bindings)
        pb (fn pb [bvec b v]
             (let [pvec
                   (fn [bvec b val]
                     (let [gvec (gensym "vec__")
                           gseq (gensym "seq__")
                           gfirst (gensym "first__")
                           has-rest (some #{'&} b)]
                       (loop [ret (let [ret (conj bvec gvec val)]
                                    (if has-rest
                                      (conj ret gseq (list `seq gvec))
                                      ret))
                              n 0
                              bs b
                              seen-rest? false]
                         (if (seq bs)
                           (let [firstb (first bs)]
                             (cond
                              (= firstb '&) (recur (pb ret (second bs) gseq)
                                                   n
                                                   (nnext bs)
                                                   true)
                              (= firstb :as) (pb ret (second bs) gvec)
                              :else (if seen-rest?
                                      (throw (new Exception "Unsupported binding form, only :as can follow & parameter"))
                                      (recur (pb (if has-rest
                                                   (conj ret
                                                         gfirst `(first ~gseq)
                                                         gseq `(next ~gseq))
                                                   ret)
                                                 firstb
                                                 (if has-rest
                                                   gfirst
                                                   (list `nth gvec n nil)))
                                             (inc n)
                                             (next bs)
                                             seen-rest?))))
                           ret))))
                   pmap
                   (fn [bvec b v]
                     (let [gmap (gensym "map__")
                           gmapseq (with-meta gmap {:tag 'cloiure.lang.ISeq})
                           defaults (:or b)]
                       (loop [ret (-> bvec (conj gmap) (conj v)
                                      (conj gmap) (conj `(if (seq? ~gmap) (cloiure.lang.PersistentHashMap/create (seq ~gmapseq)) ~gmap))
                                      ((fn [ret]
                                         (if (:as b)
                                           (conj ret (:as b) gmap)
                                           ret))))
                              bes (let [transforms
                                          (reduce1
                                            (fn [transforms mk]
                                              (if (keyword? mk)
                                                (let [mkns (namespace mk)
                                                      mkn (name mk)]
                                                  (cond (= mkn "keys") (assoc transforms mk #(keyword (or mkns (namespace %)) (name %)))
                                                        (= mkn "syms") (assoc transforms mk #(list `quote (symbol (or mkns (namespace %)) (name %))))
                                                        (= mkn "strs") (assoc transforms mk str)
                                                        :else transforms))
                                                transforms))
                                            {}
                                            (keys b))]
                                    (reduce1
                                        (fn [bes entry]
                                          (reduce1 #(assoc %1 %2 ((val entry) %2))
                                                   (dissoc bes (key entry))
                                                   ((key entry) bes)))
                                        (dissoc b :as :or)
                                        transforms))]
                         (if (seq bes)
                           (let [bb (key (first bes))
                                 bk (val (first bes))
                                 local (if (instance? cloiure.lang.Named bb) (with-meta (symbol nil (name bb)) (meta bb)) bb)
                                 bv (if (contains? defaults local)
                                      (list `get gmap bk (defaults local))
                                      (list `get gmap bk))]
                             (recur (if (ident? bb)
                                      (-> ret (conj local bv))
                                      (pb ret bb bv))
                                    (next bes)))
                           ret))))]
               (cond
                (symbol? b) (-> bvec (conj b) (conj v))
                (vector? b) (pvec bvec b v)
                (map? b) (pmap bvec b v)
                :else (throw (new Exception (str "Unsupported binding form: " b))))))
        process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
    (if (every? symbol? (map first bents))
      bindings
      (reduce1 process-entry [] bents))))

(§ defmacro let
  "binding => binding-form init-expr

  Evaluates the exprs in a lexical context in which the symbols in
  the binding-forms are bound to their respective init-exprs or parts
  therein."
  {:added "1.0", :special-form true, :forms '[(let [bindings*] exprs*)]}
  [bindings & body]
  (assert-args
     (vector? bindings) "a vector for its binding"
     (even? (count bindings)) "an even number of forms in binding vector")
  `(let* ~(destructure bindings) ~@body))

(§ defn ^{:private true}
  maybe-destructured
  [params body]
  (if (every? symbol? params)
    (cons params body)
    (loop [params params
           new-params (with-meta [] (meta params))
           lets []]
      (if params
        (if (symbol? (first params))
          (recur (next params) (conj new-params (first params)) lets)
          (let [gparam (gensym "p__")]
            (recur (next params) (conj new-params gparam)
                   (-> lets (conj (first params)) (conj gparam)))))
        `(~new-params
          (let ~lets
            ~@body))))))

; redefine fn with destructuring and pre/post conditions
(§ defmacro fn
  "params => positional-params*, or positional-params* & next-param
  positional-param => binding-form
  next-param => binding-form
  name => symbol

  Defines a function"
  {:added "1.0", :special-form true,
   :forms '[(fn name? [params* ] exprs*) (fn name? ([params* ] exprs*)+)]}
  [& sigs]
    (let [name (if (symbol? (first sigs)) (first sigs) nil)
          sigs (if name (next sigs) sigs)
          sigs (if (vector? (first sigs))
                 (list sigs)
                 (if (seq? (first sigs))
                   sigs
                   ;; Assume single arity syntax
                   (throw (IllegalArgumentException.
                            (if (seq sigs)
                              (str "Parameter declaration "
                                   (first sigs)
                                   " should be a vector")
                              (str "Parameter declaration missing"))))))
          psig (fn* [sig]
                 ;; Ensure correct type before destructuring sig
                 (when (not (seq? sig))
                   (throw (IllegalArgumentException.
                            (str "Invalid signature " sig
                                 " should be a list"))))
                 (let [[params & body] sig
                       _ (when (not (vector? params))
                           (throw (IllegalArgumentException.
                                    (if (seq? (first sigs))
                                      (str "Parameter declaration " params
                                           " should be a vector")
                                      (str "Invalid signature " sig
                                           " should be a list")))))
                       conds (when (and (next body) (map? (first body)))
                                           (first body))
                       body (if conds (next body) body)
                       conds (or conds (meta params))
                       pre (:pre conds)
                       post (:post conds)
                       body (if post
                              `((let [~'% ~(if (< 1 (count body))
                                            `(do ~@body)
                                            (first body))]
                                 ~@(map (fn* [c] `(assert ~c)) post)
                                 ~'%))
                              body)
                       body (if pre
                              (concat (map (fn* [c] `(assert ~c)) pre)
                                      body)
                              body)]
                   (maybe-destructured params body)))
          new-sigs (map psig sigs)]
      (with-meta
        (if name
          (list* 'fn* name new-sigs)
          (cons 'fn* new-sigs))
        (meta &form))))

(§ defmacro loop
  "Evaluates the exprs in a lexical context in which the symbols in
  the binding-forms are bound to their respective init-exprs or parts
  therein. Acts as a recur target."
  {:added "1.0", :special-form true, :forms '[(loop [bindings*] exprs*)]}
  [bindings & body]
    (assert-args
      (vector? bindings) "a vector for its binding"
      (even? (count bindings)) "an even number of forms in binding vector")
    (let [db (destructure bindings)]
      (if (= db bindings)
        `(loop* ~bindings ~@body)
        (let [vs (take-nth 2 (drop 1 bindings))
              bs (take-nth 2 bindings)
              gs (map (fn [b] (if (symbol? b) b (gensym))) bs)
              bfs (reduce1 (fn [ret [b v g]]
                            (if (symbol? b)
                              (conj ret g v)
                              (conj ret g v b g)))
                          [] (map vector bs vs gs))]
          `(let ~bfs
             (loop* ~(vec (interleave gs gs))
               (let ~(vec (interleave bs gs))
                 ~@body)))))))

(§ defmacro when-first
  "bindings => x xs

  Roughly the same as (when (seq xs) (let [x (first xs)] body)) but xs is evaluated only once"
  {:added "1.0"}
  [bindings & body]
  (assert-args
     (vector? bindings) "a vector for its binding"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
  (let [[x xs] bindings]
    `(when-let [xs# (seq ~xs)]
       (let [~x (first xs#)]
           ~@body))))

(§ defmacro lazy-cat
  "Expands to code which yields a lazy sequence of the concatenation
  of the supplied colls.  Each coll expr is not evaluated until it is
  needed.

  (lazy-cat xs ys zs) === (concat (lazy-seq xs) (lazy-seq ys) (lazy-seq zs))"
  {:added "1.0"}
  [& colls]
  `(concat ~@(map #(list `lazy-seq %) colls)))

(§ defmacro for
  "List comprehension. Takes a vector of one or more
   binding-form/collection-expr pairs, each followed by zero or more
   modifiers, and yields a lazy sequence of evaluations of expr.
   Collections are iterated in a nested fashion, rightmost fastest,
   and nested coll-exprs can refer to bindings created in prior
   binding-forms.  Supported modifiers are: :let [binding-form expr ...],
   :while test, :when test.

  (take 100 (for [x (range 100000000) y (range 1000000) :while (< y x)] [x y]))"
  {:added "1.0"}
  [seq-exprs body-expr]
  (assert-args
     (vector? seq-exprs) "a vector for its binding"
     (even? (count seq-exprs)) "an even number of forms in binding vector")
  (let [to-groups (fn [seq-exprs]
                    (reduce1 (fn [groups [k v]]
                              (if (keyword? k)
                                (conj (pop groups) (conj (peek groups) [k v]))
                                (conj groups [k v])))
                            [] (partition 2 seq-exprs)))
        err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))
        emit-bind (fn emit-bind [[[bind expr & mod-pairs]
                                  & [[_ next-expr] :as next-groups]]]
                    (let [giter (gensym "iter__")
                          gxs (gensym "s__")
                          do-mod (fn do-mod [[[k v :as pair] & etc]]
                                   (cond
                                     (= k :let) `(let ~v ~(do-mod etc))
                                     (= k :while) `(when ~v ~(do-mod etc))
                                     (= k :when) `(if ~v
                                                    ~(do-mod etc)
                                                    (recur (rest ~gxs)))
                                     (keyword? k) (err "Invalid 'for' keyword " k)
                                     next-groups
                                      `(let [iterys# ~(emit-bind next-groups)
                                             fs# (seq (iterys# ~next-expr))]
                                         (if fs#
                                           (concat fs# (~giter (rest ~gxs)))
                                           (recur (rest ~gxs))))
                                     :else `(cons ~body-expr
                                                  (~giter (rest ~gxs)))))]
                      (if next-groups
                        #_"not the inner-most loop"
                        `(fn ~giter [~gxs]
                           (lazy-seq
                             (loop [~gxs ~gxs]
                               (when-first [~bind ~gxs]
                                 ~(do-mod mod-pairs)))))
                        #_"inner-most loop"
                        (let [gi (gensym "i__")
                              gb (gensym "b__")
                              do-cmod (fn do-cmod [[[k v :as pair] & etc]]
                                        (cond
                                          (= k :let) `(let ~v ~(do-cmod etc))
                                          (= k :while) `(when ~v ~(do-cmod etc))
                                          (= k :when) `(if ~v
                                                         ~(do-cmod etc)
                                                         (recur
                                                           (unchecked-inc ~gi)))
                                          (keyword? k)
                                            (err "Invalid 'for' keyword " k)
                                          :else
                                            `(do (chunk-append ~gb ~body-expr)
                                                 (recur (unchecked-inc ~gi)))))]
                          `(fn ~giter [~gxs]
                             (lazy-seq
                               (loop [~gxs ~gxs]
                                 (when-let [~gxs (seq ~gxs)]
                                   (if (chunked-seq? ~gxs)
                                     (let [c# (chunk-first ~gxs)
                                           size# (int (count c#))
                                           ~gb (chunk-buffer size#)]
                                       (if (loop [~gi (int 0)]
                                             (if (< ~gi size#)
                                               (let [~bind (.nth c# ~gi)]
                                                 ~(do-cmod mod-pairs))
                                               true))
                                         (chunk-cons
                                           (chunk ~gb)
                                           (~giter (chunk-rest ~gxs)))
                                         (chunk-cons (chunk ~gb) nil)))
                                     (let [~bind (first ~gxs)]
                                       ~(do-mod mod-pairs)))))))))))]
    `(let [iter# ~(emit-bind (to-groups seq-exprs))]
        (iter# ~(second seq-exprs)))))

(§ defmacro comment
  "Ignores body, yields nil"
  {:added "1.0"}
  [& body])

(§ defmacro with-out-str
  "Evaluates exprs in a context in which *out* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  {:added "1.0"}
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       ~@body
       (str s#))))

(§ defmacro with-in-str
  "Evaluates body in a context in which *in* is bound to a fresh
  StringReader initialized with the string s."
  {:added "1.0"}
  [s & body]
  `(with-open [s# (-> (java.io.StringReader. ~s) cloiure.lang.LineNumberingPushbackReader.)]
     (binding [*in* s#]
       ~@body)))

(§ defn pr-str
  "pr to a string, returning it"
  {:tag String
   :added "1.0"
   :static true}
  [& xs]
    (with-out-str
     (apply pr xs)))

(§ defn prn-str
  "prn to a string, returning it"
  {:tag String
   :added "1.0"
   :static true}
  [& xs]
  (with-out-str
   (apply prn xs)))

(§ defn print-str
  "print to a string, returning it"
  {:tag String
   :added "1.0"
   :static true}
  [& xs]
    (with-out-str
     (apply print xs)))

(§ defn println-str
  "println to a string, returning it"
  {:tag String
   :added "1.0"
   :static true}
  [& xs]
    (with-out-str
     (apply println xs)))

(§ import cloiure.lang.ExceptionInfo cloiure.lang.IExceptionInfo)
(§ defn ex-info
  "Create an instance of ExceptionInfo, a RuntimeException subclass
   that carries a map of additional data."
  {:added "1.4"}
  ([msg map]
     (ExceptionInfo. msg map))
  ([msg map cause]
     (ExceptionInfo. msg map cause)))

(§ defn ex-data
  "Returns exception data (a map) if ex is an IExceptionInfo.
   Otherwise returns nil."
  {:added "1.4"}
  [ex]
  (when (instance? IExceptionInfo ex)
    (.getData ^IExceptionInfo ex)))

(§ defmacro assert
  "Evaluates expr and throws an exception if it does not evaluate to
  logical true."
  {:added "1.0"}
  ([x]
     (when *assert*
       `(when-not ~x
          (throw (new AssertionError (str "Assert failed: " (pr-str '~x)))))))
  ([x message]
     (when *assert*
       `(when-not ~x
          (throw (new AssertionError (str "Assert failed: " ~message "\n" (pr-str '~x))))))))

(§ defn test
  "test [v] finds fn at key :test in var metadata and calls it,
  presuming failure will throw exception"
  {:added "1.0"}
  [v]
    (let [f (:test (meta v))]
      (if f
        (do (f) :ok)
        :no-test)))

(§ defn re-pattern
  "Returns an instance of java.util.regex.Pattern, for use, e.g. in
  re-matcher."
  {:tag java.util.regex.Pattern
   :added "1.0"
   :static true}
  [s] (if (instance? java.util.regex.Pattern s)
        s
        (. java.util.regex.Pattern (compile s))))

(§ defn re-matcher
  "Returns an instance of java.util.regex.Matcher, for use, e.g. in
  re-find."
  {:tag java.util.regex.Matcher
   :added "1.0"
   :static true}
  [^java.util.regex.Pattern re s]
    (. re (matcher s)))

(§ defn re-groups
  "Returns the groups from the most recent match/find. If there are no
  nested groups, returns a string of the entire match. If there are
  nested groups, returns a vector of the groups, the first element
  being the entire match."
  {:added "1.0"
   :static true}
  [^java.util.regex.Matcher m]
    (let [gc  (. m (groupCount))]
      (if (zero? gc)
        (. m (group))
        (loop [ret [] c 0]
          (if (<= c gc)
            (recur (conj ret (. m (group c))) (inc c))
            ret)))))

(§ defn re-seq
  "Returns a lazy sequence of successive matches of pattern in string,
  using java.util.regex.Matcher.find(), each such match processed with
  re-groups."
  {:added "1.0"
   :static true}
  [^java.util.regex.Pattern re s]
  (let [m (re-matcher re s)]
    ((fn step []
       (when (. m (find))
         (cons (re-groups m) (lazy-seq (step))))))))

(§ defn re-matches
  "Returns the match, if any, of string to pattern, using
  java.util.regex.Matcher.matches().  Uses re-groups to return the
  groups."
  {:added "1.0"
   :static true}
  [^java.util.regex.Pattern re s]
    (let [m (re-matcher re s)]
      (when (. m (matches))
        (re-groups m))))

(§ defn re-find
  "Returns the next regex match, if any, of string to pattern, using
  java.util.regex.Matcher.find().  Uses re-groups to return the
  groups."
  {:added "1.0"
   :static true}
  ([^java.util.regex.Matcher m]
   (when (. m (find))
     (re-groups m)))
  ([^java.util.regex.Pattern re s]
   (let [m (re-matcher re s)]
     (re-find m))))

(§ defn rand
  "Returns a random floating point number between 0 (inclusive) and
  n (default 1) (exclusive)."
  {:added "1.0"
   :static true}
  ([] (. Math (random)))
  ([n] (* n (rand))))

(§ defn rand-int
  "Returns a random integer between 0 (inclusive) and n (exclusive)."
  {:added "1.0"
   :static true}
  [n] (int (rand n)))

(§ defmacro defn-
  "same as defn, yielding non-public def"
  {:added "1.0"}
  [name & decls]
    (list* `defn (with-meta name (assoc (meta name) :private true)) decls))

(§ defn tree-seq
  "Returns a lazy sequence of the nodes in a tree, via a depth-first walk.
   branch? must be a fn of one arg that returns true if passed a node
   that can have children (but may not).  children must be a fn of one
   arg that returns a sequence of the children. Will only be called on
   nodes for which branch? returns true. Root is the root node of the
  tree."
  {:added "1.0"
   :static true}
   [branch? children root]
   (let [walk (fn walk [node]
                (lazy-seq
                 (cons node
                  (when (branch? node)
                    (mapcat walk (children node))))))]
     (walk root)))

(§ defn special-symbol?
  "Returns true if s names a special form"
  {:added "1.0"
   :static true}
  [s]
    (contains? (. cloiure.lang.Compiler specials) s))

(§ defn var?
  "Returns true if v is of type cloiure.lang.Var"
  {:added "1.0"
   :static true}
  [v] (instance? cloiure.lang.Var v))

(§ defn subs
  "Returns the substring of s beginning at start inclusive, and ending
  at end (defaults to length of string), exclusive."
  {:added "1.0"
   :static true}
  (^String [^String s start] (. s (substring start)))
  (^String [^String s start end] (. s (substring start end))))

(§ defn max-key
  "Returns the x for which (k x), a number, is greatest.

  If there are multiple such xs, the last one is returned."
  {:added "1.0"
   :static true}
  ([k x] x)
  ([k x y] (if (> (k x) (k y)) x y))
  ([k x y & more]
   (let [kx (k x) ky (k y)
         [v kv] (if (> kx ky) [x kx] [y ky])]
     (loop [v v kv kv more more]
       (if more
         (let [w (first more)
               kw (k w)]
           (if (>= kw kv)
             (recur w kw (next more))
             (recur v kv (next more))))
         v)))))

(§ defn min-key
  "Returns the x for which (k x), a number, is least.

  If there are multiple such xs, the last one is returned."
  {:added "1.0"
   :static true}
  ([k x] x)
  ([k x y] (if (< (k x) (k y)) x y))
  ([k x y & more]
   (let [kx (k x) ky (k y)
         [v kv] (if (< kx ky) [x kx] [y ky])]
     (loop [v v kv kv more more]
       (if more
         (let [w (first more)
               kw (k w)]
           (if (<= kw kv)
             (recur w kw (next more))
             (recur v kv (next more))))
         v)))))

(§ defn distinct
  "Returns a lazy sequence of the elements of coll with duplicates removed.
  Returns a stateful transducer when no collection is provided."
  {:added "1.0"
   :static true}
  ([]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (if (contains? @seen input)
            result
            (do (vswap! seen conj input)
                (rf result input))))))))
  ([coll]
   (let [step (fn step [xs seen]
                (lazy-seq
                  ((fn [[f :as xs] seen]
                     (when-let [s (seq xs)]
                       (if (contains? seen f)
                         (recur (rest s) seen)
                         (cons f (step (rest s) (conj seen f))))))
                   xs seen)))]
     (step coll #{}))))

(§ defn replace
  "Given a map of replacement pairs and a vector/collection, returns a
  vector/seq with any elements = a key in smap replaced with the
  corresponding val in smap.  Returns a transducer when no collection
  is provided."
  {:added "1.0"
   :static true}
  ([smap]
     (map #(if-let [e (find smap %)] (val e) %)))
  ([smap coll]
     (if (vector? coll)
       (reduce1 (fn [v i]
                  (if-let [e (find smap (nth v i))]
                    (assoc v i (val e))
                    v))
                coll (range (count coll)))
       (map #(if-let [e (find smap %)] (val e) %) coll))))

(§ defmacro dosync
  "Runs the exprs (in an implicit do) in a transaction that encompasses
  exprs and any nested calls.  Starts a transaction if none is already
  running on this thread. Any uncaught exception will abort the
  transaction and flow out of dosync. The exprs may be run more than
  once, but any effects on Refs will be atomic."
  {:added "1.0"}
  [& exprs]
  `(sync nil ~@exprs))

(§ defmacro with-precision
  "Sets the precision and rounding mode to be used for BigDecimal operations.

  Usage: (with-precision 10 (/ 1M 3))
  or:    (with-precision 10 :rounding HALF_DOWN (/ 1M 3))

  The rounding mode is one of CEILING, FLOOR, HALF_UP, HALF_DOWN,
  HALF_EVEN, UP, DOWN and UNNECESSARY; it defaults to HALF_UP."
  {:added "1.0"}
  [precision & exprs]
    (let [[body rm] (if (= (first exprs) :rounding)
                      [(next (next exprs))
                       `((. java.math.RoundingMode ~(second exprs)))]
                      [exprs nil])]
      `(binding [*math-context* (java.math.MathContext. ~precision ~@rm)]
         ~@body)))

(§ defn mk-bound-fn
  {:private true}
  [^cloiure.lang.Sorted sc test key]
  (fn [e]
    (test (.. sc comparator (compare (. sc entryKey e) key)) 0)))

(§ defn subseq
  "sc must be a sorted collection, test(s) one of <, <=, > or
  >=. Returns a seq of those entries with keys ek for
  which (test (.. sc comparator (compare ek key)) 0) is true"
  {:added "1.0"
   :static true}
  ([^cloiure.lang.Sorted sc test key]
   (let [include (mk-bound-fn sc test key)]
     (if (#{> >=} test)
       (when-let [[e :as s] (. sc seqFrom key true)]
         (if (include e) s (next s)))
       (take-while include (. sc seq true)))))
  ([^cloiure.lang.Sorted sc start-test start-key end-test end-key]
   (when-let [[e :as s] (. sc seqFrom start-key true)]
     (take-while (mk-bound-fn sc end-test end-key)
                 (if ((mk-bound-fn sc start-test start-key) e) s (next s))))))

(§ defn rsubseq
  "sc must be a sorted collection, test(s) one of <, <=, > or
  >=. Returns a reverse seq of those entries with keys ek for
  which (test (.. sc comparator (compare ek key)) 0) is true"
  {:added "1.0"
   :static true}
  ([^cloiure.lang.Sorted sc test key]
   (let [include (mk-bound-fn sc test key)]
     (if (#{< <=} test)
       (when-let [[e :as s] (. sc seqFrom key false)]
         (if (include e) s (next s)))
       (take-while include (. sc seq false)))))
  ([^cloiure.lang.Sorted sc start-test start-key end-test end-key]
   (when-let [[e :as s] (. sc seqFrom end-key false)]
     (take-while (mk-bound-fn sc start-test start-key)
                 (if ((mk-bound-fn sc end-test end-key) e) s (next s))))))

(§ defn repeatedly
  "Takes a function of no args, presumably with side effects, and
  returns an infinite (or length n if supplied) lazy sequence of calls
  to it"
  {:added "1.0"
   :static true}
  ([f] (lazy-seq (cons (f) (repeatedly f))))
  ([n f] (take n (repeatedly f))))

(§ defn add-classpath
  "DEPRECATED

  Adds the url (String or URL object) to the classpath per
  URLClassLoader.addURL"
  {:added "1.0"
   :deprecated "1.1"}
  [url]
  (println "WARNING: add-classpath is deprecated")
  (cloiure.lang.RT/addURL url))

(§ defn hash
  "Returns the hash code of its argument. Note this is the hash code
  consistent with =, and thus is different than .hashCode for Integer,
  Short, Byte and Cloiure collections."

  {:added "1.0"
   :static true}
  [x] (. cloiure.lang.Util (hasheq x)))

(§ defn mix-collection-hash
  "Mix final collection hash for ordered or unordered collections.
   hash-basis is the combined collection hash, count is the number
   of elements included in the basis. Note this is the hash code
   consistent with =, different from .hashCode.
   See http://clojure.org/data_structures#hash for full algorithms."
  {:added "1.6"
   :static true}
  ^long
  [^long hash-basis ^long count] (cloiure.lang.Murmur3/mixCollHash hash-basis count))

(§ defn hash-ordered-coll
  "Returns the hash code, consistent with =, for an external ordered
   collection implementing Iterable.
   See http://clojure.org/data_structures#hash for full algorithms."
  {:added "1.6"
   :static true}
  ^long
  [coll] (cloiure.lang.Murmur3/hashOrdered coll))

(§ defn hash-unordered-coll
  "Returns the hash code, consistent with =, for an external unordered
   collection implementing Iterable. For maps, the iterator should
   return map entries whose hash is computed as
     (hash-ordered-coll [k v]).
   See http://clojure.org/data_structures#hash for full algorithms."
  {:added "1.6"
   :static true}
  ^long
  [coll] (cloiure.lang.Murmur3/hashUnordered coll))

(§ defn interpose
  "Returns a lazy seq of the elements of coll separated by sep.
  Returns a stateful transducer when no collection is provided."
  {:added "1.0"
   :static true}
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
                (rf sepr input)))
            (do
              (vreset! started true)
              (rf result input))))))))
  ([sep coll]
   (drop 1 (interleave (repeat sep) coll))))

(§ defmacro definline
  "Experimental - like defmacro, except defines a named function whose
  body is the expansion, calls to which may be expanded inline as if
  it were a macro. Cannot be used with variadic (&) args."
  {:added "1.0"}
  [name & decl]
  (let [[pre-args [args expr]] (split-with (comp not vector?) decl)]
    `(do
       (defn ~name ~@pre-args ~args ~(apply (eval (list `fn args expr)) args))
       (alter-meta! (var ~name) assoc :inline (fn ~name ~args ~expr))
       (var ~name))))

(§ defn empty
  "Returns an empty collection of the same category as coll, or nil"
  {:added "1.0"
   :static true}
  [coll]
  (when (instance? cloiure.lang.IPersistentCollection coll)
    (.empty ^cloiure.lang.IPersistentCollection coll)))

(§ defmacro amap
  "Maps an expression across an array a, using an index named idx, and
  return value named ret, initialized to a clone of a, then setting
  each element of ret to the evaluation of expr, returning the new
  array ret."
  {:added "1.0"}
  [a idx ret expr]
  `(let [a# ~a l# (alength a#)
         ~ret (aclone a#)]
     (loop  [~idx 0]
       (if (< ~idx  l#)
         (do
           (aset ~ret ~idx ~expr)
           (recur (unchecked-inc ~idx)))
         ~ret))))

(§ defmacro areduce
  "Reduces an expression across an array a, using an index named idx,
  and return value named ret, initialized to init, setting ret to the
  evaluation of expr at each step, returning ret."
  {:added "1.0"}
  [a idx ret init expr]
  `(let [a# ~a l# (alength a#)]
     (loop  [~idx 0 ~ret ~init]
       (if (< ~idx l#)
         (recur (unchecked-inc-int ~idx) ~expr)
         ~ret))))

(§ defn float-array
  "Creates an array of floats"
  {:inline (fn [& args] `(. cloiure.lang.Numbers float_array ~@args))
   :inline-arities #{1 2}
   :added "1.0"}
  ([size-or-seq] (. cloiure.lang.Numbers float_array size-or-seq))
  ([size init-val-or-seq] (. cloiure.lang.Numbers float_array size init-val-or-seq)))

(§ defn boolean-array
  "Creates an array of booleans"
  {:inline (fn [& args] `(. cloiure.lang.Numbers boolean_array ~@args))
   :inline-arities #{1 2}
   :added "1.1"}
  ([size-or-seq] (. cloiure.lang.Numbers boolean_array size-or-seq))
  ([size init-val-or-seq] (. cloiure.lang.Numbers boolean_array size init-val-or-seq)))

(§ defn byte-array
  "Creates an array of bytes"
  {:inline (fn [& args] `(. cloiure.lang.Numbers byte_array ~@args))
   :inline-arities #{1 2}
   :added "1.1"}
  ([size-or-seq] (. cloiure.lang.Numbers byte_array size-or-seq))
  ([size init-val-or-seq] (. cloiure.lang.Numbers byte_array size init-val-or-seq)))

(§ defn char-array
  "Creates an array of chars"
  {:inline (fn [& args] `(. cloiure.lang.Numbers char_array ~@args))
   :inline-arities #{1 2}
   :added "1.1"}
  ([size-or-seq] (. cloiure.lang.Numbers char_array size-or-seq))
  ([size init-val-or-seq] (. cloiure.lang.Numbers char_array size init-val-or-seq)))

(§ defn short-array
  "Creates an array of shorts"
  {:inline (fn [& args] `(. cloiure.lang.Numbers short_array ~@args))
   :inline-arities #{1 2}
   :added "1.1"}
  ([size-or-seq] (. cloiure.lang.Numbers short_array size-or-seq))
  ([size init-val-or-seq] (. cloiure.lang.Numbers short_array size init-val-or-seq)))

(§ defn double-array
  "Creates an array of doubles"
  {:inline (fn [& args] `(. cloiure.lang.Numbers double_array ~@args))
   :inline-arities #{1 2}
   :added "1.0"}
  ([size-or-seq] (. cloiure.lang.Numbers double_array size-or-seq))
  ([size init-val-or-seq] (. cloiure.lang.Numbers double_array size init-val-or-seq)))

(§ defn object-array
  "Creates an array of objects"
  {:inline (fn [arg] `(. cloiure.lang.RT object_array ~arg))
   :inline-arities #{1}
   :added "1.2"}
  ([size-or-seq] (. cloiure.lang.RT object_array size-or-seq)))

(§ defn int-array
  "Creates an array of ints"
  {:inline (fn [& args] `(. cloiure.lang.Numbers int_array ~@args))
   :inline-arities #{1 2}
   :added "1.0"}
  ([size-or-seq] (. cloiure.lang.Numbers int_array size-or-seq))
  ([size init-val-or-seq] (. cloiure.lang.Numbers int_array size init-val-or-seq)))

(§ defn long-array
  "Creates an array of longs"
  {:inline (fn [& args] `(. cloiure.lang.Numbers long_array ~@args))
   :inline-arities #{1 2}
   :added "1.0"}
  ([size-or-seq] (. cloiure.lang.Numbers long_array size-or-seq))
  ([size init-val-or-seq] (. cloiure.lang.Numbers long_array size init-val-or-seq)))

(§ definline booleans
  "Casts to boolean[]"
  {:added "1.1"}
  [xs] `(. cloiure.lang.Numbers booleans ~xs))

(§ definline bytes
  "Casts to bytes[]"
  {:added "1.1"}
  [xs] `(. cloiure.lang.Numbers bytes ~xs))

(§ definline chars
  "Casts to chars[]"
  {:added "1.1"}
  [xs] `(. cloiure.lang.Numbers chars ~xs))

(§ definline shorts
  "Casts to shorts[]"
  {:added "1.1"}
  [xs] `(. cloiure.lang.Numbers shorts ~xs))

(§ definline floats
  "Casts to float[]"
  {:added "1.0"}
  [xs] `(. cloiure.lang.Numbers floats ~xs))

(§ definline ints
  "Casts to int[]"
  {:added "1.0"}
  [xs] `(. cloiure.lang.Numbers ints ~xs))

(§ definline doubles
  "Casts to double[]"
  {:added "1.0"}
  [xs] `(. cloiure.lang.Numbers doubles ~xs))

(§ definline longs
  "Casts to long[]"
  {:added "1.0"}
  [xs] `(. cloiure.lang.Numbers longs ~xs))

(§ defn bytes?
  "Return true if x is a byte array"
  {:added "1.9"}
  [x] (if (nil? x)
        false
        (-> x class .getComponentType (= Byte/TYPE))))

(§ import '(java.util.concurrent BlockingQueue LinkedBlockingQueue))

(§ defn seque
  "Creates a queued seq on another (presumably lazy) seq s. The queued
  seq will produce a concrete seq in the background, and can get up to
  n items ahead of the consumer. n-or-q can be an integer n buffer
  size, or an instance of java.util.concurrent BlockingQueue. Note
  that reading from a seque can block if the reader gets ahead of the
  producer."
  {:added "1.0"
   :static true}
  ([s] (seque 100 s))
  ([n-or-q s]
   (let [^BlockingQueue q (if (instance? BlockingQueue n-or-q)
                             n-or-q
                             (LinkedBlockingQueue. (int n-or-q)))
         NIL (Object.) ;; nil sentinel since LBQ doesn't support nils
         agt (agent (lazy-seq s)) ;; never start with nil; that signifies we've already put eos
         log-error (fn [q e]
                     (if (.offer q q)
                       (throw e)
                       e))
         fill (fn [s]
                (when s
                  (if (instance? Exception s) ;; we failed to .offer an error earlier
                    (log-error q s)
                    (try
                      (loop [[x & xs :as s] (seq s)]
                        (if s
                          (if (.offer q (if (nil? x) NIL x))
                            (recur xs)
                            s)
                          (when-not (.offer q q) ;; q itself is eos sentinel
                            ()))) ;; empty seq, not nil, so we know to put eos next time
                      (catch Exception e
                        (log-error q e))))))
         drain (fn drain []
                 (lazy-seq
                  (let [x (.take q)]
                    (if (identical? x q) ;; q itself is eos sentinel
                      (do @agt nil)  ;; touch agent just to propagate errors
                      (do
                        (send-off agt fill)
                        (release-pending-sends)
                        (cons (if (identical? x NIL) nil x) (drain)))))))]
     (send-off agt fill)
     (drain))))

(§ defn class?
  "Returns true if x is an instance of Class"
  {:added "1.0"
   :static true}
  [x] (instance? Class x))

(§ defn- is-annotation? [c]
  (and (class? c)
       (.isAssignableFrom java.lang.annotation.Annotation c)))

(§ defn- is-runtime-annotation? [^Class c]
  (boolean
   (and (is-annotation? c)
        (when-let [^java.lang.annotation.Retention r
                   (.getAnnotation c java.lang.annotation.Retention)]
          (= (.value r) java.lang.annotation.RetentionPolicy/RUNTIME)))))

(§ defn- descriptor [^Class c] (cloiure.asm.Type/getDescriptor c))

(§ declare process-annotation)
(§ defn- add-annotation [^cloiure.asm.AnnotationVisitor av name v]
  (cond
   (vector? v) (let [avec (.visitArray av name)]
                 (doseq [vval v]
                   (add-annotation avec "value" vval))
                 (.visitEnd avec))
   (symbol? v) (let [ev (eval v)]
                 (cond
                  (instance? java.lang.Enum ev)
                  (.visitEnum av name (descriptor (class ev)) (str ev))
                  (class? ev) (.visit av name (cloiure.asm.Type/getType ev))
                  :else (throw (IllegalArgumentException.
                                (str "Unsupported annotation value: " v " of class " (class ev))))))
   (seq? v) (let [[nested nv] v
                  c (resolve nested)
                  nav (.visitAnnotation av name (descriptor c))]
              (process-annotation nav nv)
              (.visitEnd nav))
   :else (.visit av name v)))

(§ defn- process-annotation [av v]
  (if (map? v)
    (doseq [[k v] v]
      (add-annotation av (name k) v))
    (add-annotation av "value" v)))

(§ defn- add-annotations
  ([visitor m] (add-annotations visitor m nil))
  ([visitor m i]
     (doseq [[k v] m]
       (when (symbol? k)
         (when-let [c (resolve k)]
           (when (is-annotation? c)
                                        ;; this is known duck/reflective as no common base of ASM Visitors
             (let [av (if i
                        (.visitParameterAnnotation visitor i (descriptor c)
                                                   (is-runtime-annotation? c))
                        (.visitAnnotation visitor (descriptor c)
                                          (is-runtime-annotation? c)))]
               (process-annotation av v)
               (.visitEnd av))))))))

(§ defn alter-var-root
  "Atomically alters the root binding of var v by applying f to its
  current value plus any args"
  {:added "1.0"
   :static true}
  [^cloiure.lang.Var v f & args] (.alterRoot v f args))

(§ defn bound?
  "Returns true if all of the vars provided as arguments have any bound value, root or thread-local.
   Implies that deref'ing the provided vars will succeed. Returns true if no vars are provided."
  {:added "1.2"
   :static true}
  [& vars]
  (every? #(.isBound ^cloiure.lang.Var %) vars))

(§ defn thread-bound?
  "Returns true if all of the vars provided as arguments have thread-local bindings.
   Implies that set!'ing the provided vars will succeed.  Returns true if no vars are provided."
  {:added "1.2"
   :static true}
  [& vars]
  (every? #(.getThreadBinding ^cloiure.lang.Var %) vars))

(§ defn make-hierarchy
  "Creates a hierarchy object for use with derive, isa? etc."
  {:added "1.0"
   :static true}
  [] {:parents {} :descendants {} :ancestors {}})

(§ def ^{:private true}
     global-hierarchy (make-hierarchy))

(§ defn not-empty
  "If coll is empty, returns nil, else coll"
  {:added "1.0"
   :static true}
  [coll] (when (seq coll) coll))

(§ defn bases
  "Returns the immediate superclass and direct interfaces of c, if any"
  {:added "1.0"
   :static true}
  [^Class c]
  (when c
    (let [i (seq (.getInterfaces c))
          s (.getSuperclass c)]
      (if s (cons s i) i))))

(§ defn supers
  "Returns the immediate and indirect superclasses and interfaces of c, if any"
  {:added "1.0"
   :static true}
  [^Class class]
  (loop [ret (set (bases class)) cs ret]
    (if (seq cs)
      (let [c (first cs) bs (bases c)]
        (recur (into1 ret bs) (into1 (disj cs c) bs)))
      (not-empty ret))))

(§ defn isa?
  "Returns true if (= child parent), or child is directly or indirectly derived from
  parent, either via a Java type inheritance relationship or a
  relationship established via derive. h must be a hierarchy obtained
  from make-hierarchy, if not supplied defaults to the global
  hierarchy"
  {:added "1.0"}
  ([child parent] (isa? global-hierarchy child parent))
  ([h child parent]
   (or (= child parent)
       (and (class? parent) (class? child)
            (. ^Class parent isAssignableFrom child))
       (contains? ((:ancestors h) child) parent)
       (and (class? child) (some #(contains? ((:ancestors h) %) parent) (supers child)))
       (and (vector? parent) (vector? child)
            (= (count parent) (count child))
            (loop [ret true i 0]
              (if (or (not ret) (= i (count parent)))
                ret
                (recur (isa? h (child i) (parent i)) (inc i))))))))

(§ defn parents
  "Returns the immediate parents of tag, either via a Java type
  inheritance relationship or a relationship established via derive. h
  must be a hierarchy obtained from make-hierarchy, if not supplied
  defaults to the global hierarchy"
  {:added "1.0"}
  ([tag] (parents global-hierarchy tag))
  ([h tag] (not-empty
            (let [tp (get (:parents h) tag)]
              (if (class? tag)
                (into1 (set (bases tag)) tp)
                tp)))))

(§ defn ancestors
  "Returns the immediate and indirect parents of tag, either via a Java type
  inheritance relationship or a relationship established via derive. h
  must be a hierarchy obtained from make-hierarchy, if not supplied
  defaults to the global hierarchy"
  {:added "1.0"}
  ([tag] (ancestors global-hierarchy tag))
  ([h tag] (not-empty
            (let [ta (get (:ancestors h) tag)]
              (if (class? tag)
                (let [superclasses (set (supers tag))]
                  (reduce1 into1 superclasses
                    (cons ta
                          (map #(get (:ancestors h) %) superclasses))))
                ta)))))

(§ defn descendants
  "Returns the immediate and indirect children of tag, through a
  relationship established via derive. h must be a hierarchy obtained
  from make-hierarchy, if not supplied defaults to the global
  hierarchy. Note: does not work on Java type inheritance
  relationships."
  {:added "1.0"}
  ([tag] (descendants global-hierarchy tag))
  ([h tag] (if (class? tag)
             (throw (java.lang.UnsupportedOperationException. "Can't get descendants of classes"))
             (not-empty (get (:descendants h) tag)))))

(§ defn derive
  "Establishes a parent/child relationship between parent and
  tag. Parent must be a namespace-qualified symbol or keyword and
  child can be either a namespace-qualified symbol or keyword or a
  class. h must be a hierarchy obtained from make-hierarchy, if not
  supplied defaults to, and modifies, the global hierarchy."
  {:added "1.0"}
  ([tag parent]
   (assert (namespace parent))
   (assert (or (class? tag) (and (instance? cloiure.lang.Named tag) (namespace tag))))

   (alter-var-root #'global-hierarchy derive tag parent) nil)
  ([h tag parent]
   (assert (not= tag parent))
   (assert (or (class? tag) (instance? cloiure.lang.Named tag)))
   (assert (instance? cloiure.lang.Named parent))

   (let [tp (:parents h)
         td (:descendants h)
         ta (:ancestors h)
         tf (fn [m source sources target targets]
              (reduce1 (fn [ret k]
                        (assoc ret k
                               (reduce1 conj (get targets k #{}) (cons target (targets target)))))
                      m (cons source (sources source))))]
     (or
      (when-not (contains? (tp tag) parent)
        (when (contains? (ta tag) parent)
          (throw (Exception. (print-str tag "already has" parent "as ancestor"))))
        (when (contains? (ta parent) tag)
          (throw (Exception. (print-str "Cyclic derivation:" parent "has" tag "as ancestor"))))
        {:parents (assoc (:parents h) tag (conj (get tp tag #{}) parent))
         :ancestors (tf (:ancestors h) tag td parent ta)
         :descendants (tf (:descendants h) parent ta tag td)})
      h))))

(§ declare flatten)

(§ defn underive
  "Removes a parent/child relationship between parent and
  tag. h must be a hierarchy obtained from make-hierarchy, if not
  supplied defaults to, and modifies, the global hierarchy."
  {:added "1.0"}
  ([tag parent] (alter-var-root #'global-hierarchy underive tag parent) nil)
  ([h tag parent]
    (let [parentMap (:parents h)
          childsParents (if (parentMap tag)
                          (disj (parentMap tag) parent) #{})
          newParents (if (not-empty childsParents)
                       (assoc parentMap tag childsParents)
                       (dissoc parentMap tag))
          deriv-seq (flatten (map #(cons (key %) (interpose (key %) (val %)))
                                       (seq newParents)))]
      (if (contains? (parentMap tag) parent)
        (reduce1 #(apply derive %1 %2) (make-hierarchy)
                (partition 2 deriv-seq))
        h))))

(§ defn distinct?
  "Returns true if no two of the arguments are ="
  {:tag Boolean
   :added "1.0"
   :static true}
  ([x] true)
  ([x y] (not (= x y)))
  ([x y & more]
   (if (not= x y)
     (loop [s #{x y} [x & etc :as xs] more]
       (if xs
         (if (contains? s x)
           false
           (recur (conj s x) etc))
         true))
     false)))

(§ defn iterator-seq
  "Returns a seq on a java.util.Iterator. Note that most collections
  providing iterators implement Iterable and thus support seq directly.
  Seqs cache values, thus iterator-seq should not be used on any
  iterator that repeatedly returns the same mutable object."
  {:added "1.0"
   :static true}
  [iter]
  (cloiure.lang.RT/chunkIteratorSeq iter))

(§ defn enumeration-seq
  "Returns a seq on a java.util.Enumeration"
  {:added "1.0"
   :static true}
  [e]
  (cloiure.lang.EnumerationSeq/create e))

(§ defn format
  "Formats a string using java.lang.String.format, see java.util.Formatter for format
  string syntax"
  {:added "1.0"
   :static true}
  ^String [fmt & args]
  (String/format fmt (to-array args)))

(§ defn printf
  "Prints formatted output, as per format"
  {:added "1.0"
   :static true}
  [fmt & args]
  (print (apply format fmt args)))

(§ declare gen-class)

(§ defmacro with-loading-context [& body]
  `((fn loading# []
        (. cloiure.lang.Var (pushThreadBindings {cloiure.lang.Compiler/LOADER
                                                 (.getClassLoader (.getClass ^Object loading#))}))
        (try
         ~@body
         (finally
          (. cloiure.lang.Var (popThreadBindings)))))))

(§ defmacro ns
  "Sets *ns* to the namespace named by name (unevaluated), creating it
  if needed.  references can be zero or more of: (:refer-cloiure ...)
  (:require ...) (:use ...) (:import ...) (:load ...) (:gen-class)
  with the syntax of refer-cloiure/require/use/import/load/gen-class
  respectively, except the arguments are unevaluated and need not be
  quoted. (:gen-class ...), when supplied, defaults to :name
  corresponding to the ns name, :main true, :impl-ns same as ns, and
  :init-impl-ns true. All options of gen-class are
  supported. The :gen-class directive is ignored when not
  compiling. If :gen-class is not supplied, when compiled only an
  nsname__init.class will be generated. If :refer-cloiure is not used, a
  default (refer 'cloiure.core) is used.  Use of ns is preferred to
  individual calls to in-ns/require/use/import:

  (ns foo.bar
    (:refer-cloiure :exclude [ancestors printf])
    (:require (cloiure.contrib sql combinatorics))
    (:use (my.lib this that))
    (:import (java.util Date Timer Random)
             (java.sql Connection Statement)))"
  {:arglists '([name docstring? attr-map? references*])
   :added "1.0"}
  [name & references]
  (let [process-reference
        (fn [[kname & args]]
          `(~(symbol "cloiure.core" (cloiure.core/name kname))
             ~@(map #(list 'quote %) args)))
        docstring  (when (string? (first references)) (first references))
        references (if docstring (next references) references)
        name (if docstring
               (vary-meta name assoc :doc docstring)
               name)
        metadata   (when (map? (first references)) (first references))
        references (if metadata (next references) references)
        name (if metadata
               (vary-meta name merge metadata)
               name)
        gen-class-clause (first (filter #(= :gen-class (first %)) references))
        gen-class-call
          (when gen-class-clause
            (list* `gen-class :name (.replace (str name) \- \_) :impl-ns name :main true (next gen-class-clause)))
        references (remove #(= :gen-class (first %)) references)
        ;; ns-effect (cloiure.core/in-ns name)
        name-metadata (meta name)]
    `(do
       (cloiure.core/in-ns '~name)
       ~@(when name-metadata
           `((.resetMeta (cloiure.lang.Namespace/find '~name) ~name-metadata)))
       (with-loading-context
        ~@(when gen-class-call (list gen-class-call))
        ~@(when (and (not= name 'cloiure.core) (not-any? #(= :refer-cloiure (first %)) references))
            `((cloiure.core/refer '~'cloiure.core)))
        ~@(map process-reference references))
        (if (.equals '~name 'cloiure.core)
          nil
          (do (dosync (commute @#'*loaded-libs* conj '~name)) nil)))))

(§ defmacro refer-cloiure
  "Same as (refer 'cloiure.core <filters>)"
  {:added "1.0"}
  [& filters]
  `(cloiure.core/refer '~'cloiure.core ~@filters))

(§ defmacro defonce
  "defs name to have the root value of the expr iff the named var has no root value,
  else expr is unevaluated"
  {:added "1.0"}
  [name expr]
  `(let [v# (def ~name)]
     (when-not (.hasRoot v#)
       (def ~name ~expr))))

;;;;;;;;;;; require/use/load, contributed by Stephen C. Gilardi ;;;;;;;;;;;;;;;;;;

(§ defonce ^:dynamic
  ^{:private true
     :doc "A ref to a sorted set of symbols representing loaded libs"}
  *loaded-libs* (ref (sorted-set)))

(§ defonce ^:dynamic
  ^{:private true
     :doc "A stack of paths currently being loaded by this thread"}
  *pending-paths* ())

(§ defonce ^:dynamic
  ^{:private true :doc
     "True while a verbose load is pending"}
  *loading-verbosely* false)

(§ defn- throw-if
  "Throws a CompilerException with a message if pred is true"
  [pred fmt & args]
  (when pred
    (let [^String message (apply format fmt args)
          exception (Exception. message)
          raw-trace (.getStackTrace exception)
          boring? #(not= (.getMethodName ^StackTraceElement %) "doInvoke")
          trace (into-array StackTraceElement (drop 2 (drop-while boring? raw-trace)))]
      (.setStackTrace exception trace)
      (throw (cloiure.lang.Compiler$CompilerException.
              *file*
              (.deref cloiure.lang.Compiler/LINE)
              (.deref cloiure.lang.Compiler/COLUMN)
              exception)))))

(§ defn- libspec?
  "Returns true if x is a libspec"
  [x]
  (or (symbol? x)
      (and (vector? x)
           (or
            (nil? (second x))
            (keyword? (second x))))))

(§ defn- prependss
  "Prepends a symbol or a seq to coll"
  [x coll]
  (if (symbol? x)
    (cons x coll)
    (concat x coll)))

(§ defn- root-resource
  "Returns the root directory path for a lib"
  {:tag String}
  [lib]
  (str \/
       (.. (name lib)
           (replace \- \_)
           (replace \. \/))))

(§ defn- root-directory
  "Returns the root resource path for a lib"
  [lib]
  (let [d (root-resource lib)]
    (subs d 0 (.lastIndexOf d "/"))))

(§ def ^:declared ^:redef load)

(§ defn- load-one
  "Loads a lib given its name. If need-ns, ensures that the associated
  namespace exists after loading. If require, records the load so any
  duplicate loads can be skipped."
  [lib need-ns require]
  (load (root-resource lib))
  (throw-if (and need-ns (not (find-ns lib)))
            "namespace '%s' not found after loading '%s'"
            lib (root-resource lib))
  (when require
    (dosync
     (commute *loaded-libs* conj lib))))

(§ defn- load-all
  "Loads a lib given its name and forces a load of any libs it directly or
  indirectly loads. If need-ns, ensures that the associated namespace
  exists after loading. If require, records the load so any duplicate loads
  can be skipped."
  [lib need-ns require]
  (dosync
   (commute *loaded-libs* #(reduce1 conj %1 %2)
            (binding [*loaded-libs* (ref (sorted-set))]
              (load-one lib need-ns require)
              @*loaded-libs*))))

(§ defn- load-lib
  "Loads a lib with options"
  [prefix lib & options]
  (throw-if (and prefix (pos? (.indexOf (name lib) (int \.))))
            "Found lib name '%s' containing period with prefix '%s'.  lib names inside prefix lists must not contain periods"
            (name lib) prefix)
  (let [lib (if prefix (symbol (str prefix \. lib)) lib)
        opts (apply hash-map options)
        {:keys [as reload reload-all require use verbose]} opts
        loaded (contains? @*loaded-libs* lib)
        load (cond reload-all
                   load-all
                   (or reload (not require) (not loaded))
                   load-one)
        need-ns (or as use)
        filter-opts (select-keys opts '(:exclude :only :rename :refer))
        undefined-on-entry (not (find-ns lib))]
    (binding [*loading-verbosely* (or *loading-verbosely* verbose)]
      (if load
        (try
          (load lib need-ns require)
          (catch Exception e
            (when undefined-on-entry
              (remove-ns lib))
            (throw e)))
        (throw-if (and need-ns (not (find-ns lib)))
                  "namespace '%s' not found" lib))
      (when (and need-ns *loading-verbosely*)
        (printf "(cloiure.core/in-ns '%s)\n" (ns-name *ns*)))
      (when as
        (when *loading-verbosely*
          (printf "(cloiure.core/alias '%s '%s)\n" as lib))
        (alias as lib))
      (when (or use (:refer filter-opts))
        (when *loading-verbosely*
          (printf "(cloiure.core/refer '%s" lib)
          (doseq [opt filter-opts]
            (printf " %s '%s" (key opt) (print-str (val opt))))
          (printf ")\n"))
        (apply refer lib (mapcat seq filter-opts))))))

(§ defn- load-libs
  "Loads libs, interpreting libspecs, prefix lists, and flags for
  forwarding to load-lib"
  [& args]
  (let [flags (filter keyword? args)
        opts (interleave flags (repeat true))
        args (filter (complement keyword?) args)]
    ;; check for unsupported options
    (let [supported #{:as :reload :reload-all :require :use :verbose :refer}
          unsupported (seq (remove supported flags))]
      (throw-if unsupported
                (apply str "Unsupported option(s) supplied: "
                     (interpose \, unsupported))))
    ;; check a load target was specified
    (throw-if (not (seq args)) "Nothing specified to load")
    (doseq [arg args]
      (if (libspec? arg)
        (apply load-lib nil (prependss arg opts))
        (let [[prefix & args] arg]
          (throw-if (nil? prefix) "prefix cannot be nil")
          (doseq [arg args]
            (apply load-lib prefix (prependss arg opts))))))))

(§ defn- check-cyclic-dependency
  "Detects and rejects non-trivial cyclic load dependencies. The
  exception message shows the dependency chain with the cycle
  highlighted. Ignores the trivial case of a file attempting to load
  itself because that can occur when a gen-class'd class loads its
  implementation."
  [path]
  (when (some #{path} (rest *pending-paths*))
    (let [pending (map #(if (= % path) (str "[ " % " ]") %)
                       (cons path *pending-paths*))
          chain (apply str (interpose "->" pending))]
      (throw-if true "Cyclic load dependency: %s" chain))))

;; Public

(§ defn require
  "Loads libs, skipping any that are already loaded. Each argument is
  either a libspec that identifies a lib, a prefix list that identifies
  multiple libs whose names share a common prefix, or a flag that modifies
  how all the identified libs are loaded. Use :require in the ns macro
  in preference to calling this directly.

  Libs

  A 'lib' is a named set of resources in classpath whose contents define a
  library of Cloiure code. Lib names are symbols and each lib is associated
  with a Cloiure namespace and a Java package that share its name. A lib's
  name also locates its root directory within classpath using Java's
  package name to classpath-relative path mapping. All resources in a lib
  should be contained in the directory structure under its root directory.
  All definitions a lib makes should be in its associated namespace.

  'require loads a lib by loading its root resource. The root resource path
  is derived from the lib name in the following manner:
  Consider a lib named by the symbol 'x.y.z; it has the root directory
  <classpath>/x/y/, and its root resource is <classpath>/x/y/z.cli, or
  <classpath>/x/y/z.clic if <classpath>/x/y/z.cli does not exist. The
  root resource should contain code to create the lib's
  namespace (usually by using the ns macro) and load any additional
  lib resources.

  Libspecs

  A libspec is a lib name or a vector containing a lib name followed by
  options expressed as sequential keywords and arguments.

  Recognized options:
  :as takes a symbol as its argument and makes that symbol an alias to the
    lib's namespace in the current namespace.
  :refer takes a list of symbols to refer from the namespace or the :all
    keyword to bring in all public vars.

  Prefix Lists

  It's common for Cloiure code to depend on several libs whose names have
  the same prefix. When specifying libs, prefix lists can be used to reduce
  repetition. A prefix list contains the shared prefix followed by libspecs
  with the shared prefix removed from the lib names. After removing the
  prefix, the names that remain must not contain any periods.

  Flags

  A flag is a keyword.
  Recognized flags: :reload, :reload-all, :verbose
  :reload forces loading of all the identified libs even if they are
    already loaded
  :reload-all implies :reload and also forces loading of all libs that the
    identified libs directly or indirectly load via require or use
  :verbose triggers printing information about each load, alias, and refer

  Example:

  The following would load the libraries cloiure.zip and cloiure.set
  abbreviated as 's'.

  (require '(cloiure zip [set :as s]))"
  {:added "1.0"}

  [& args]
  (apply load-libs :require args))

(§ defn use
  "Like 'require, but also refers to each lib's namespace using
  cloiure.core/refer. Use :use in the ns macro in preference to calling
  this directly.

  'use accepts additional options in libspecs: :exclude, :only, :rename.
  The arguments and semantics for :exclude, :only, and :rename are the same
  as those documented for cloiure.core/refer."
  {:added "1.0"}
  [& args] (apply load-libs :require :use args))

(§ defn loaded-libs
  "Returns a sorted set of symbols naming the currently loaded libs"
  {:added "1.0"}
  [] @*loaded-libs*)

(§ defn load
  "Loads Cloiure code from resources in classpath. A path is interpreted as
  classpath-relative if it begins with a slash or relative to the root
  directory for the current namespace otherwise."
  {:redef true
   :added "1.0"}
  [& paths]
  (doseq [^String path paths]
    (let [^String path (if (.startsWith path "/")
                          path
                          (str (root-directory (ns-name *ns*)) \/ path))]
      (when *loading-verbosely*
        (printf "(cloiure.core/load \"%s\")\n" path)
        (flush))
      (check-cyclic-dependency path)
      (when-not (= path (first *pending-paths*))
        (binding [*pending-paths* (conj *pending-paths* path)]
          (cloiure.lang.RT/load (.substring path 1)))))))

(§ defn compile
  "Compiles the namespace named by the symbol lib into a set of
  classfiles. The source for the lib must be in a proper
  classpath-relative directory. The output files will go into the
  directory specified by *compile-path*, and that directory too must
  be in the classpath."
  {:added "1.0"}
  [lib]
  (binding [*compile-files* true]
    (load-one lib true true))
  lib)

;;;;;;;;;;;;; nested associative ops ;;;;;;;;;;;

(§ defn get-in
  "Returns the value in a nested associative structure,
  where ks is a sequence of keys. Returns nil if the key
  is not present, or the not-found value if supplied."
  {:added "1.2"
   :static true}
  ([m ks]
     (reduce1 get m ks))
  ([m ks not-found]
     (loop [sentinel (Object.)
            m m
            ks (seq ks)]
       (if ks
         (let [m (get m (first ks) sentinel)]
           (if (identical? sentinel m)
             not-found
             (recur sentinel m (next ks))))
         m))))

(§ defn assoc-in
  "Associates a value in a nested associative structure, where ks is a
  sequence of keys and v is the new value and returns a new nested structure.
  If any levels do not exist, hash-maps will be created."
  {:added "1.0"
   :static true}
  [m [k & ks] v]
  (if ks
    (assoc m k (assoc-in (get m k) ks v))
    (assoc m k v)))

(§ defn update-in
  "'Updates' a value in a nested associative structure, where ks is a
  sequence of keys and f is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  nested structure.  If any levels do not exist, hash-maps will be
  created."
  {:added "1.0"
   :static true}
  ([m ks f & args]
     (let [up (fn up [m ks f args]
                (let [[k & ks] ks]
                  (if ks
                    (assoc m k (up (get m k) ks f args))
                    (assoc m k (apply f (get m k) args)))))]
       (up m ks f args))))

(§ defn update
  "'Updates' a value in an associative structure, where k is a
  key and f is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  structure.  If the key does not exist, nil is passed as the old value."
  {:added "1.7"
   :static true}
  ([m k f]
   (assoc m k (f (get m k))))
  ([m k f x]
   (assoc m k (f (get m k) x)))
  ([m k f x y]
   (assoc m k (f (get m k) x y)))
  ([m k f x y z]
   (assoc m k (f (get m k) x y z)))
  ([m k f x y z & more]
   (assoc m k (apply f (get m k) x y z more))))

(§ defn empty?
  "Returns true if coll has no items - same as (not (seq coll)).
  Please use the idiom (seq x) rather than (not (empty? x))"
  {:added "1.0"
   :static true}
  [coll] (not (seq coll)))

(§ defn coll?
  "Returns true if x implements IPersistentCollection"
  {:added "1.0"
   :static true}
  [x] (instance? cloiure.lang.IPersistentCollection x))

(§ defn list?
  "Returns true if x implements IPersistentList"
  {:added "1.0"
   :static true}
  [x] (instance? cloiure.lang.IPersistentList x))

(§ defn seqable?
  "Return true if the seq function is supported for x"
  {:added "1.9"}
  [x] (cloiure.lang.RT/canSeq x))

(§ defn ifn?
  "Returns true if x implements IFn. Note that many data structures
  (e.g. sets and maps) implement IFn"
  {:added "1.0"
   :static true}
  [x] (instance? cloiure.lang.IFn x))

(§ defn fn?
  "Returns true if x implements Fn, i.e. is an object created via fn."
  {:added "1.0"
   :static true}
  [x] (instance? cloiure.lang.Fn x))

(§ defn associative?
 "Returns true if coll implements Associative"
 {:added "1.0"
  :static true}
  [coll] (instance? cloiure.lang.Associative coll))

(§ defn sequential?
 "Returns true if coll implements Sequential"
 {:added "1.0"
  :static true}
  [coll] (instance? cloiure.lang.Sequential coll))

(§ defn sorted?
 "Returns true if coll implements Sorted"
 {:added "1.0"
   :static true}
  [coll] (instance? cloiure.lang.Sorted coll))

(§ defn counted?
 "Returns true if coll implements count in constant time"
 {:added "1.0"
   :static true}
  [coll] (instance? cloiure.lang.Counted coll))

(§ defn reversible?
 "Returns true if coll implements Reversible"
 {:added "1.0"
   :static true}
  [coll] (instance? cloiure.lang.Reversible coll))

(§ defn indexed?
  "Return true if coll implements Indexed, indicating efficient lookup by index"
  {:added "1.9"}
  [coll] (instance? cloiure.lang.Indexed coll))

(§ def ^:dynamic
 ^{:doc "bound in a repl thread to the most recent value printed"
   :added "1.0"}
 *1)

(§ def ^:dynamic
 ^{:doc "bound in a repl thread to the second most recent value printed"
   :added "1.0"}
 *2)

(§ def ^:dynamic
 ^{:doc "bound in a repl thread to the third most recent value printed"
   :added "1.0"}
 *3)

(§ def ^:dynamic
 ^{:doc "bound in a repl thread to the most recent exception caught by the repl"
   :added "1.0"}
 *e)

(§ defn trampoline
  "trampoline can be used to convert algorithms requiring mutual
  recursion without stack consumption. Calls f with supplied args, if
  any. If f returns a fn, calls that fn with no arguments, and
  continues to repeat, until the return value is not a fn, then
  returns that non-fn value. Note that if you want to return a fn as a
  final value, you must wrap it in some data structure and unpack it
  after trampoline returns."
  {:added "1.0"
   :static true}
  ([f]
     (let [ret (f)]
       (if (fn? ret)
         (recur ret)
         ret)))
  ([f & args]
     (trampoline #(apply f args))))

(§ defn intern
  "Finds or creates a var named by the symbol name in the namespace
  ns (which can be a symbol or a namespace), setting its root binding
  to val if supplied. The namespace must exist. The var will adopt any
  metadata from the name symbol.  Returns the var."
  {:added "1.0"
   :static true}
  ([ns ^cloiure.lang.Symbol name]
     (let [v (cloiure.lang.Var/intern (the-ns ns) name)]
       (when (meta name) (.setMeta v (meta name)))
       v))
  ([ns name val]
     (let [v (cloiure.lang.Var/intern (the-ns ns) name val)]
       (when (meta name) (.setMeta v (meta name)))
       v)))

(§ defmacro while
  "Repeatedly executes body while test expression is true. Presumes
  some side-effect will cause test to become false/nil. Returns nil"
  {:added "1.0"}
  [test & body]
  `(loop []
     (when ~test
       ~@body
       (recur))))

(§ defn memoize
  "Returns a memoized version of a referentially transparent function. The
  memoized version of the function keeps a cache of the mapping from arguments
  to results and, when calls with the same arguments are repeated often, has
  higher performance at the expense of higher memory use."
  {:added "1.0"
   :static true}
  [f]
  (let [mem (atom {})]
    (fn [& args]
      (if-let [e (find @mem args)]
        (val e)
        (let [ret (apply f args)]
          (swap! mem assoc args ret)
          ret)))))

(§ defmacro condp
  "Takes a binary predicate, an expression, and a set of clauses.
  Each clause can take the form of either:

  test-expr result-expr

  test-expr :>> result-fn

  Note :>> is an ordinary keyword.

  For each clause, (pred test-expr expr) is evaluated. If it returns
  logical true, the clause is a match. If a binary clause matches, the
  result-expr is returned, if a ternary clause matches, its result-fn,
  which must be a unary function, is called with the result of the
  predicate as its argument, the result of that call being the return
  value of condp. A single default expression can follow the clauses,
  and its value will be returned if no clause matches. If no default
  expression is provided and no clause matches, an
  IllegalArgumentException is thrown."
  {:added "1.0"}

  [pred expr & clauses]
  (let [gpred (gensym "pred__")
        gexpr (gensym "expr__")
        emit (fn emit [pred expr args]
               (let [[[a b c :as clause] more]
                       (split-at (if (= :>> (second args)) 3 2) args)
                       n (count clause)]
                 (cond
                  (= 0 n) `(throw (IllegalArgumentException. (str "No matching clause: " ~expr)))
                  (= 1 n) a
                  (= 2 n) `(if (~pred ~a ~expr)
                             ~b
                             ~(emit pred expr more))
                  :else `(if-let [p# (~pred ~a ~expr)]
                           (~c p#)
                           ~(emit pred expr more)))))]
    `(let [~gpred ~pred
           ~gexpr ~expr]
       ~(emit gpred gexpr clauses))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; var documentation ;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ alter-meta! #'*agent* assoc :added "1.0")
(§ alter-meta! #'in-ns assoc :added "1.0")
(§ alter-meta! #'load-file assoc :added "1.0")

(§ defmacro add-doc-and-meta {:private true} [name docstring meta]
  `(alter-meta! (var ~name) merge (assoc ~meta :doc ~docstring)))

(§ add-doc-and-meta *file*
  "The path of the file being evaluated, as a String.

  When there is no file, e.g. in the REPL, the value is not defined."
  {:added "1.0"})

(§ add-doc-and-meta *command-line-args*
  "A sequence of the supplied command line arguments, or nil if
  none were supplied"
  {:added "1.0"})

(§ add-doc-and-meta *warn-on-reflection*
  "When set to true, the compiler will emit warnings when reflection is
  needed to resolve Java method calls or field accesses.

  Defaults to false."
  {:added "1.0"})

(§ add-doc-and-meta *compile-path*
  "Specifies the directory where 'compile' will write out .class
  files. This directory must be in the classpath for 'compile' to
  work.

  Defaults to \"classes\""
  {:added "1.0"})

(§ add-doc-and-meta *compile-files*
  "Set to true when compiling files, false otherwise."
  {:added "1.0"})

(§ add-doc-and-meta *unchecked-math*
  "While bound to true, compilations of +, -, *, inc, dec and the
  coercions will be done without overflow checks. While bound
  to :warn-on-boxed, same behavior as true, and a warning is emitted
  when compilation uses boxed math. Default: false."
  {:added "1.3"})

(§ add-doc-and-meta *compiler-options*
  "A map of keys to options.
  Note, when binding dynamically make sure to merge with previous value.
  Supported options:
  :elide-meta - a collection of metadata keys to elide during compilation.
  :disable-locals-clearing - set to true to disable clearing, useful for using a debugger
  Alpha, subject to change."
  {:added "1.4"})

(§ add-doc-and-meta *ns*
  "A cloiure.lang.Namespace object representing the current namespace."
  {:added "1.0"})

(§ add-doc-and-meta *in*
  "A java.io.Reader object representing standard input for read operations.

  Defaults to System/in, wrapped in a LineNumberingPushbackReader"
  {:added "1.0"})

(§ add-doc-and-meta *out*
  "A java.io.Writer object representing standard output for print operations.

  Defaults to System/out, wrapped in an OutputStreamWriter"
  {:added "1.0"})

(§ add-doc-and-meta *err*
  "A java.io.Writer object representing standard error for print operations.

  Defaults to System/err, wrapped in a PrintWriter"
  {:added "1.0"})

(§ add-doc-and-meta *flush-on-newline*
  "When set to true, output will be flushed whenever a newline is printed.

  Defaults to true."
  {:added "1.0"})

(§ add-doc-and-meta *print-meta*
  "If set to logical true, when printing an object, its metadata will also
  be printed in a form that can be read back by the reader.

  Defaults to false."
  {:added "1.0"})

(§ add-doc-and-meta *print-dup*
  "When set to logical true, objects will be printed in a way that preserves
  their type when read in later.

  Defaults to false."
  {:added "1.0"})

(§ add-doc-and-meta *print-readably*
  "When set to logical false, strings and characters will be printed with
  non-alphanumeric characters converted to the appropriate escape sequences.

  Defaults to true"
  {:added "1.0"})

(§ add-doc-and-meta *read-eval*
 "Defaults to true (or value specified by system property, see below)
  ***This setting implies that the full power of the reader is in play,
  including syntax that can cause code to execute. It should never be
  used with untrusted sources. See also: cloiure.edn/read.***

  When set to logical false in the thread-local binding,
  the eval reader (#=) and record/type literal syntax are disabled in read/load.
  Example (will fail): (binding [*read-eval* false] (read-string \"#=(* 2 21)\"))

  The default binding can be controlled by the system property
  'cloiure.read.eval' System properties can be set on the command line
  like this:

  java -Dcloiure.read.eval=false ...

  The system property can also be set to 'unknown' via
  -Dcloiure.read.eval=unknown, in which case the default binding
  is :unknown and all reads will fail in contexts where *read-eval*
  has not been explicitly bound to either true or false. This setting
  can be a useful diagnostic tool to ensure that all of your reads
  occur in considered contexts. You can also accomplish this in a
  particular scope by binding *read-eval* to :unknown
  "
  {:added "1.0"})

(§ defn future?
  "Returns true if x is a future"
  {:added "1.1"
   :static true}
  [x] (instance? java.util.concurrent.Future x))

(§ defn future-done?
  "Returns true if future f is done"
  {:added "1.1"
   :static true}
  [^java.util.concurrent.Future f] (.isDone f))

(§ defmacro letfn
  "fnspec ==> (fname [params*] exprs) or (fname ([params*] exprs)+)

  Takes a vector of function specs and a body, and generates a set of
  bindings of functions to their names. All of the names are available
  in all of the definitions of the functions, as well as the body."
  {:added "1.0", :forms '[(letfn [fnspecs*] exprs*)],
   :special-form true, :url nil}
  [fnspecs & body]
  `(letfn* ~(vec (interleave (map first fnspecs)
                             (map #(cons `fn %) fnspecs)))
           ~@body))

(§ defn fnil
  "Takes a function f, and returns a function that calls f, replacing
  a nil first argument to f with the supplied value x. Higher arity
  versions can replace arguments in the second and third
  positions (y, z). Note that the function f can take any number of
  arguments, not just the one(s) being nil-patched."
  {:added "1.2"
   :static true}
  ([f x]
   (fn
     ([a] (f (if (nil? a) x a)))
     ([a b] (f (if (nil? a) x a) b))
     ([a b c] (f (if (nil? a) x a) b c))
     ([a b c & ds] (apply f (if (nil? a) x a) b c ds))))
  ([f x y]
   (fn
     ([a b] (f (if (nil? a) x a) (if (nil? b) y b)))
     ([a b c] (f (if (nil? a) x a) (if (nil? b) y b) c))
     ([a b c & ds] (apply f (if (nil? a) x a) (if (nil? b) y b) c ds))))
  ([f x y z]
   (fn
     ([a b] (f (if (nil? a) x a) (if (nil? b) y b)))
     ([a b c] (f (if (nil? a) x a) (if (nil? b) y b) (if (nil? c) z c)))
     ([a b c & ds] (apply f (if (nil? a) x a) (if (nil? b) y b) (if (nil? c) z c) ds)))))

;;;;;;; case ;;;;;;;;;;;;;
(§ defn- shift-mask [shift mask x]
  (-> x (bit-shift-right shift) (bit-and mask)))

(§ def ^:private max-mask-bits 13)
(§ def ^:private max-switch-table-size (bit-shift-left 1 max-mask-bits))

(§ defn- maybe-min-hash
  "takes a collection of hashes and returns [shift mask] or nil if none found"
  [hashes]
  (first
    (filter (fn [[s m]]
              (apply distinct? (map #(shift-mask s m %) hashes)))
            (for [mask (map #(dec (bit-shift-left 1 %)) (range 1 (inc max-mask-bits)))
                  shift (range 0 31)]
              [shift mask]))))

(§ defn- case-map
  "Transforms a sequence of test constants and a corresponding sequence of then
  expressions into a sorted map to be consumed by case*. The form of the map
  entries are {(case-f test) [(test-f test) then]}."
  [case-f test-f tests thens]
  (into1 (sorted-map)
    (zipmap (map case-f tests)
            (map vector
              (map test-f tests)
              thens))))

(§ defn- fits-table?
  "Returns true if the collection of ints can fit within the
  max-table-switch-size, false otherwise."
  [ints]
  (< (- (apply max (seq ints)) (apply min (seq ints))) max-switch-table-size))

(§ defn- prep-ints
  "Takes a sequence of int-sized test constants and a corresponding sequence of
  then expressions. Returns a tuple of [shift mask case-map switch-type] where
  case-map is a map of int case values to [test then] tuples, and switch-type
  is either :sparse or :compact."
  [tests thens]
  (if (fits-table? tests)
    ;; compact case ints, no shift-mask
    [0 0 (case-map int int tests thens) :compact]
    (let [[shift mask] (or (maybe-min-hash (map int tests)) [0 0])]
      (if (zero? mask)
        ;; sparse case ints, no shift-mask
        [0 0 (case-map int int tests thens) :sparse]
        ;; compact case ints, with shift-mask
        [shift mask (case-map #(shift-mask shift mask (int %)) int tests thens) :compact]))))

(§ defn- merge-hash-collisions
  "Takes a case expression, default expression, and a sequence of test constants
  and a corresponding sequence of then expressions. Returns a tuple of
  [tests thens skip-check-set] where no tests have the same hash. Each set of
  input test constants with the same hash is replaced with a single test
  constant (the case int), and their respective thens are combined into:
  (condp = expr
    test-1 then-1
    ...
    test-n then-n
    default).
  The skip-check is a set of case ints for which post-switch equivalence
  checking must not be done (the cases holding the above condp thens)."
  [expr-sym default tests thens]
  (let [buckets (loop [m {} ks tests vs thens]
                  (if (and ks vs)
                    (recur
                      (update m (cloiure.lang.Util/hash (first ks)) (fnil conj []) [(first ks) (first vs)])
                      (next ks) (next vs))
                    m))
        assoc-multi (fn [m h bucket]
                      (let [testexprs (apply concat bucket)
                            expr `(condp = ~expr-sym ~@testexprs ~default)]
                        (assoc m h expr)))
        hmap (reduce1
               (fn [m [h bucket]]
                 (if (== 1 (count bucket))
                   (assoc m (ffirst bucket) (second (first bucket)))
                   (assoc-multi m h bucket)))
               {} buckets)
        skip-check (->> buckets
                     (filter #(< 1 (count (second %))))
                     (map first)
                     (into1 #{}))]
    [(keys hmap) (vals hmap) skip-check]))

(§ defn- prep-hashes
  "Takes a sequence of test constants and a corresponding sequence of then
  expressions. Returns a tuple of [shift mask case-map switch-type skip-check]
  where case-map is a map of int case values to [test then] tuples, switch-type
  is either :sparse or :compact, and skip-check is a set of case ints for which
  post-switch equivalence checking must not be done (occurs with hash
  collisions)."
  [expr-sym default tests thens]
  (let [hashcode #(cloiure.lang.Util/hash %)
        hashes (into1 #{} (map hashcode tests))]
    (if (== (count tests) (count hashes))
      (if (fits-table? hashes)
        ;; compact case ints, no shift-mask
        [0 0 (case-map hashcode identity tests thens) :compact]
        (let [[shift mask] (or (maybe-min-hash hashes) [0 0])]
          (if (zero? mask)
            ;; sparse case ints, no shift-mask
            [0 0 (case-map hashcode identity tests thens) :sparse]
            ;; compact case ints, with shift-mask
            [shift mask (case-map #(shift-mask shift mask (hashcode %)) identity tests thens) :compact])))
      ;; resolve hash collisions and try again
      (let [[tests thens skip-check] (merge-hash-collisions expr-sym default tests thens)
            [shift mask case-map switch-type] (prep-hashes expr-sym default tests thens)
            skip-check (if (zero? mask)
                         skip-check
                         (into1 #{} (map #(shift-mask shift mask %) skip-check)))]
        [shift mask case-map switch-type skip-check]))))

(§ defmacro case
  "Takes an expression, and a set of clauses.

  Each clause can take the form of either:

  test-constant result-expr

  (test-constant1 ... test-constantN)  result-expr

  The test-constants are not evaluated. They must be compile-time
  literals, and need not be quoted.  If the expression is equal to a
  test-constant, the corresponding result-expr is returned. A single
  default expression can follow the clauses, and its value will be
  returned if no clause matches. If no default expression is provided
  and no clause matches, an IllegalArgumentException is thrown.

  Unlike cond and condp, case does a constant-time dispatch, the
  clauses are not considered sequentially.  All manner of constant
  expressions are acceptable in case, including numbers, strings,
  symbols, keywords, and (Cloiure) composites thereof. Note that since
  lists are used to group multiple constants that map to the same
  expression, a vector can be used to match a list if needed. The
  test-constants need not be all of the same type."
  {:added "1.2"}

  [e & clauses]
  (let [ge (with-meta (gensym) {:tag Object})
        default (if (odd? (count clauses))
                  (last clauses)
                  `(throw (IllegalArgumentException. (str "No matching clause: " ~ge))))]
    (if (> 2 (count clauses))
      `(let [~ge ~e] ~default)
      (let [pairs (partition 2 clauses)
            assoc-test (fn assoc-test [m test expr]
                         (if (contains? m test)
                           (throw (IllegalArgumentException. (str "Duplicate case test constant: " test)))
                           (assoc m test expr)))
            pairs (reduce1
                       (fn [m [test expr]]
                         (if (seq? test)
                           (reduce1 #(assoc-test %1 %2 expr) m test)
                           (assoc-test m test expr)))
                       {} pairs)
            tests (keys pairs)
            thens (vals pairs)
            mode (cond
                   (every? #(and (integer? %) (<= Integer/MIN_VALUE % Integer/MAX_VALUE)) tests)
                   :ints
                   (every? keyword? tests)
                   :identity
                   :else :hashes)]
        (condp = mode
          :ints
          (let [[shift mask imap switch-type] (prep-ints tests thens)]
            `(let [~ge ~e] (case* ~ge ~shift ~mask ~default ~imap ~switch-type :int)))
          :hashes
          (let [[shift mask imap switch-type skip-check] (prep-hashes ge default tests thens)]
            `(let [~ge ~e] (case* ~ge ~shift ~mask ~default ~imap ~switch-type :hash-equiv ~skip-check)))
          :identity
          (let [[shift mask imap switch-type skip-check] (prep-hashes ge default tests thens)]
            `(let [~ge ~e] (case* ~ge ~shift ~mask ~default ~imap ~switch-type :hash-identity ~skip-check))))))))

;; redefine reduce with internal-reduce

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; helper files ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(§ alter-meta! (find-ns 'cloiure.core) assoc :doc "Fundamental library of the Cloiure language")
(§ load "core_proxy")
(§ load "core_print")
(§ load "genclass")
(§ load "core_deftype")
(§ load "core/protocols")
(§ load "gvec")

(§ defmacro ^:private when-class [class-name & body]
  `(try
     (Class/forName ^String ~class-name)
     ~@body
     (catch ClassNotFoundException _#)))

(§ when-class "java.sql.Timestamp"
  (load "instant"))

(§ defprotocol Inst
  (inst-ms* [inst]))

(§ extend-protocol Inst
  java.util.Date
  (inst-ms* [inst] (.getTime ^java.util.Date inst)))

;; conditionally extend to Instant on Java 8+
(§ when-class "java.time.Instant"
  (load "core_instant18"))

(§ defn inst-ms
  "Return the number of milliseconds since January 1, 1970, 00:00:00 GMT"
  {:added "1.9"}
  [inst]
  (inst-ms* inst))

(§ defn inst?
  "Return true if x satisfies Inst"
  {:added "1.9"}
  [x]
  (satisfies? Inst x))

(§ load "uuid")

(§ defn uuid?
  "Return true if x is a java.util.UUID"
  {:added "1.9"}
  [x] (instance? java.util.UUID x))

(§ defn reduce
  "f should be a function of 2 arguments. If val is not supplied,
  returns the result of applying f to the first 2 items in coll, then
  applying f to that result and the 3rd item, etc. If coll contains no
  items, f must accept no arguments as well, and reduce returns the
  result of calling f with no arguments.  If coll has only 1 item, it
  is returned and f is not called.  If val is supplied, returns the
  result of applying f to val and the first item in coll, then
  applying f to that result and the 2nd item, etc. If coll contains no
  items, returns val and f is not called."
  {:added "1.0"}
  ([f coll]
     (if (instance? cloiure.lang.IReduce coll)
       (.reduce ^cloiure.lang.IReduce coll f)
       (cloiure.core.protocols/coll-reduce coll f)))
  ([f val coll]
     (if (instance? cloiure.lang.IReduceInit coll)
       (.reduce ^cloiure.lang.IReduceInit coll f val)
       (cloiure.core.protocols/coll-reduce coll f val))))

(§ extend-protocol cloiure.core.protocols/IKVReduce
 nil
 (kv-reduce
  [_ f init]
  init)

 ;; slow path default
 cloiure.lang.IPersistentMap
 (kv-reduce
  [amap f init]
  (reduce (fn [ret [k v]] (f ret k v)) init amap))

 cloiure.lang.IKVReduce
 (kv-reduce
  [amap f init]
  (.kvreduce amap f init)))

(§ defn reduce-kv
  "Reduces an associative collection. f should be a function of 3
  arguments. Returns the result of applying f to init, the first key
  and the first value in coll, then applying f to that result and the
  2nd key and value, etc. If coll contains no entries, returns init
  and f is not called. Note that reduce-kv is supported on vectors,
  where the keys will be the ordinals."
  {:added "1.4"}
  ([f init coll]
     (cloiure.core.protocols/kv-reduce coll f init)))

(§ defn completing
  "Takes a reducing function f of 2 args and returns a fn suitable for
  transduce by adding an arity-1 signature that calls cf (default -
  identity) on the result argument."
  {:added "1.7"}
  ([f] (completing f identity))
  ([f cf]
     (fn
       ([] (f))
       ([x] (cf x))
       ([x y] (f x y)))))

(§ defn transduce
  "reduce with a transformation of f (xf). If init is not
  supplied, (f) will be called to produce it. f should be a reducing
  step function that accepts both 1 and 2 arguments, if it accepts
  only 2 you can add the arity-1 with 'completing'. Returns the result
  of applying (the transformed) xf to init and the first item in coll,
  then applying xf to that result and the 2nd item, etc. If coll
  contains no items, returns init and f is not called. Note that
  certain transforms may inject or skip items."  {:added "1.7"}
  ([xform f coll] (transduce xform f (f) coll))
  ([xform f init coll]
     (let [f (xform f)
           ret (if (instance? cloiure.lang.IReduceInit coll)
                 (.reduce ^cloiure.lang.IReduceInit coll f init)
                 (cloiure.core.protocols/coll-reduce coll f init))]
       (f ret))))

(§ defn into
  "Returns a new coll consisting of to-coll with all of the items of
  from-coll conjoined. A transducer may be supplied."
  {:added "1.0"
   :static true}
  ([] [])
  ([to] to)
  ([to from]
     (if (instance? cloiure.lang.IEditableCollection to)
       (with-meta (persistent! (reduce conj! (transient to) from)) (meta to))
       (reduce conj to from)))
  ([to xform from]
     (if (instance? cloiure.lang.IEditableCollection to)
       (with-meta (persistent! (transduce xform conj! (transient to) from)) (meta to))
       (transduce xform conj to from))))

(§ defn mapv
  "Returns a vector consisting of the result of applying f to the
  set of first items of each coll, followed by applying f to the set
  of second items in each coll, until any one of the colls is
  exhausted.  Any remaining items in other colls are ignored. Function
  f should accept number-of-colls arguments."
  {:added "1.4"
   :static true}
  ([f coll]
     (-> (reduce (fn [v o] (conj! v (f o))) (transient []) coll)
         persistent!))
  ([f c1 c2]
     (into [] (map f c1 c2)))
  ([f c1 c2 c3]
     (into [] (map f c1 c2 c3)))
  ([f c1 c2 c3 & colls]
     (into [] (apply map f c1 c2 c3 colls))))

(§ defn filterv
  "Returns a vector of the items in coll for which
  (pred item) returns logical true. pred must be free of side-effects."
  {:added "1.4"
   :static true}
  [pred coll]
  (-> (reduce (fn [v o] (if (pred o) (conj! v o) v))
              (transient [])
              coll)
      persistent!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; futures (needs proxy) ;;;;;;;;;;;;;;;;;;

(§ defn future-call
  "Takes a function of no args and yields a future object that will
  invoke the function in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block, unless the variant
  of deref with timeout is used. See also - realized?."
  {:added "1.1"
   :static true}
  [f]
  (let [f (binding-conveyor-fn f)
        fut (.submit cloiure.lang.Agent/soloExecutor ^Callable f)]
    (reify
     cloiure.lang.IDeref
     (deref [_] (deref-future fut))
     cloiure.lang.IBlockingDeref
     (deref
      [_ timeout-ms timeout-val]
      (deref-future fut timeout-ms timeout-val))
     cloiure.lang.IPending
     (isRealized [_] (.isDone fut))
     java.util.concurrent.Future
      (get [_] (.get fut))
      (get [_ timeout unit] (.get fut timeout unit))
      (isCancelled [_] (.isCancelled fut))
      (isDone [_] (.isDone fut))
      (cancel [_ interrupt?] (.cancel fut interrupt?)))))

(§ defmacro future
  "Takes a body of expressions and yields a future object that will
  invoke the body in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block, unless the variant of
  deref with timeout is used. See also - realized?."
  {:added "1.1"}
  [& body] `(future-call (^{:once true} fn* [] ~@body)))

(§ defn future-cancel
  "Cancels the future, if possible."
  {:added "1.1"
   :static true}
  [^java.util.concurrent.Future f] (.cancel f true))

(§ defn future-cancelled?
  "Returns true if future f is cancelled"
  {:added "1.1"
   :static true}
  [^java.util.concurrent.Future f] (.isCancelled f))

(§ defn pmap
  "Like map, except f is applied in parallel. Semi-lazy in that the
  parallel computation stays ahead of the consumption, but doesn't
  realize the entire result unless required. Only useful for
  computationally intensive functions where the time of f dominates
  the coordination overhead."
  {:added "1.0"
   :static true}
  ([f coll]
   (let [n (+ 2 (.. Runtime getRuntime availableProcessors))
         rets (map #(future (f %)) coll)
         step (fn step [[x & xs :as vs] fs]
                (lazy-seq
                 (if-let [s (seq fs)]
                   (cons (deref x) (step xs (rest s)))
                   (map deref vs))))]
     (step rets (drop n rets))))
  ([f coll & colls]
   (let [step (fn step [cs]
                (lazy-seq
                 (let [ss (map seq cs)]
                   (when (every? identity ss)
                     (cons (map first ss) (step (map rest ss)))))))]
     (pmap #(apply f %) (step (cons coll colls))))))

(§ defn pcalls
  "Executes the no-arg fns in parallel, returning a lazy sequence of
  their values"
  {:added "1.0"
   :static true}
  [& fns] (pmap #(%) fns))

(§ defmacro pvalues
  "Returns a lazy sequence of the values of the exprs, which are
  evaluated in parallel"
  {:added "1.0"
   :static true}
  [& exprs]
  `(pcalls ~@(map #(list `fn [] %) exprs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; cloiure version number ;;;;;;;;;;;;;;;;;;;;;;

(§ let [version-string "1.9.0-MISPELED"
      [_ major minor incremental qualifier snapshot]
      (re-matches
       #"(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9_]+))?(?:-(SNAPSHOT))?"
       version-string)
      cloiure-version {:major       (Integer/valueOf ^String major)
                       :minor       (Integer/valueOf ^String minor)
                       :incremental (Integer/valueOf ^String incremental)
                       :qualifier   (if (= qualifier "SNAPSHOT") nil qualifier)}]
  (def ^:dynamic *cloiure-version*
    (if (.contains version-string "SNAPSHOT")
      (cloiure.lang.RT/assoc cloiure-version :interim true)
      cloiure-version)))

(§ add-doc-and-meta *cloiure-version*
  "The version info for Cloiure core, as a map containing :major :minor
  :incremental and :qualifier keys. Feature releases may increment
  :minor and/or :major, bugfix releases will increment :incremental.
  Possible values of :qualifier include \"GA\", \"SNAPSHOT\", \"RC-x\" \"BETA-x\""
  {:added "1.0"})

(§ defn
  cloiure-version
  "Returns cloiure version as a printable string."
  {:added "1.0"}
  []
  (str (:major *cloiure-version*)
       "."
       (:minor *cloiure-version*)
       (when-let [i (:incremental *cloiure-version*)]
         (str "." i))
       (when-let [q (:qualifier *cloiure-version*)]
         (when (pos? (count q)) (str "-" q)))
       (when (:interim *cloiure-version*)
         "-SNAPSHOT")))

(§ defn promise
  "Returns a promise object that can be read with deref/@, and set,
  once only, with deliver. Calls to deref/@ prior to delivery will
  block, unless the variant of deref with timeout is used. All
  subsequent derefs will return the same delivered value without
  blocking. See also - realized?."
  {:added "1.1"
   :static true}
  []
  (let [d (java.util.concurrent.CountDownLatch. 1)
        v (atom d)]
    (reify
     cloiure.lang.IDeref
       (deref [_] (.await d) @v)
     cloiure.lang.IBlockingDeref
       (deref
        [_ timeout-ms timeout-val]
        (if (.await d timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)
          @v
          timeout-val))
     cloiure.lang.IPending
      (isRealized [this]
       (zero? (.getCount d)))
     cloiure.lang.IFn
     (invoke
      [this x]
      (when (and (pos? (.getCount d))
                 (compare-and-set! v d x))
        (.countDown d)
        this)))))

(§ defn deliver
  "Delivers the supplied value to the promise, releasing any pending
  derefs. A subsequent call to deliver on a promise will have no effect."
  {:added "1.1"
   :static true}
  [promise val] (promise val))

(§ defn flatten
  "Takes any nested combination of sequential things (lists, vectors,
  etc.) and returns their contents as a single, flat sequence.
  (flatten nil) returns an empty sequence."
  {:added "1.2"
   :static true}
  [x]
  (filter (complement sequential?)
          (rest (tree-seq sequential? seq x))))

(§ defn group-by
  "Returns a map of the elements of coll keyed by the result of
  f on each element. The value at each key will be a vector of the
  corresponding elements, in the order they appeared in coll."
  {:added "1.2"
   :static true}
  [f coll]
  (persistent!
   (reduce
    (fn [ret x]
      (let [k (f x)]
        (assoc! ret k (conj (get ret k []) x))))
    (transient {}) coll)))

(§ defn partition-by
  "Applies f to each value in coll, splitting it each time f returns a
   new value.  Returns a lazy seq of partitions.  Returns a stateful
   transducer when no collection is provided."
  {:added "1.2"
   :static true}
  ([f]
  (fn [rf]
    (let [a (java.util.ArrayList.)
          pv (volatile! ::none)]
      (fn
        ([] (rf))
        ([result]
           (let [result (if (.isEmpty a)
                          result
                          (let [v (vec (.toArray a))]
                            ;; clear first!
                            (.clear a)
                            (unreduced (rf result v))))]
             (rf result)))
        ([result input]
           (let [pval @pv
                 val (f input)]
             (vreset! pv val)
             (if (or (identical? pval ::none)
                     (= val pval))
               (do
                 (.add a input)
                 result)
               (let [v (vec (.toArray a))]
                 (.clear a)
                 (let [ret (rf result v)]
                   (when-not (reduced? ret)
                     (.add a input))
                   ret)))))))))
  ([f coll]
     (lazy-seq
      (when-let [s (seq coll)]
        (let [fst (first s)
              fv (f fst)
              run (cons fst (take-while #(= fv (f %)) (next s)))]
          (cons run (partition-by f (seq (drop (count run) s)))))))))

(§ defn frequencies
  "Returns a map from distinct items in coll to the number of times
  they appear."
  {:added "1.2"
   :static true}
  [coll]
  (persistent!
   (reduce (fn [counts x]
             (assoc! counts x (inc (get counts x 0))))
           (transient {}) coll)))

(§ defn reductions
  "Returns a lazy seq of the intermediate values of the reduction (as
  per reduce) of coll by f, starting with init."
  {:added "1.2"}
  ([f coll]
     (lazy-seq
      (if-let [s (seq coll)]
        (reductions f (first s) (rest s))
        (list (f)))))
  ([f init coll]
     (if (reduced? init)
       (list @init)
       (cons init
             (lazy-seq
              (when-let [s (seq coll)]
                (reductions f (f init (first s)) (rest s))))))))

(§ defn rand-nth
  "Return a random element of the (sequential) collection. Will have
  the same performance characteristics as nth for the given
  collection."
  {:added "1.2"
   :static true}
  [coll]
  (nth coll (rand-int (count coll))))

(§ defn partition-all
  "Returns a lazy sequence of lists like partition, but may include
  partitions with fewer than n items at the end.  Returns a stateful
  transducer when no collection is provided."
  {:added "1.2"
   :static true}
  ([^long n]
   (fn [rf]
     (let [a (java.util.ArrayList. n)]
       (fn
         ([] (rf))
         ([result]
            (let [result (if (.isEmpty a)
                           result
                           (let [v (vec (.toArray a))]
                             ;; clear first!
                             (.clear a)
                             (unreduced (rf result v))))]
              (rf result)))
         ([result input]
            (.add a input)
            (if (= n (.size a))
              (let [v (vec (.toArray a))]
                (.clear a)
                (rf result v))
              result))))))
  ([n coll]
     (partition-all n n coll))
  ([n step coll]
     (lazy-seq
      (when-let [s (seq coll)]
        (let [seg (doall (take n s))]
          (cons seg (partition-all n step (nthrest s step))))))))

(§ defn shuffle
  "Return a random permutation of coll"
  {:added "1.2"
   :static true}
  [^java.util.Collection coll]
  (let [al (java.util.ArrayList. coll)]
    (java.util.Collections/shuffle al)
    (cloiure.lang.RT/vector (.toArray al))))

(§ defn map-indexed
  "Returns a lazy sequence consisting of the result of applying f to 0
  and the first item of coll, followed by applying f to 1 and the second
  item in coll, etc, until coll is exhausted. Thus function f should
  accept 2 arguments, index and item. Returns a stateful transducer when
  no collection is provided."
  {:added "1.2"
   :static true}
  ([f]
   (fn [rf]
     (let [i (volatile! -1)]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (rf result (f (vswap! i inc) input)))))))
  ([f coll]
   (letfn [(mapi [idx coll]
                 (lazy-seq
                   (when-let [s (seq coll)]
                     (if (chunked-seq? s)
                       (let [c (chunk-first s)
                             size (int (count c))
                             b (chunk-buffer size)]
                         (dotimes [i size]
                           (chunk-append b (f (+ idx i) (.nth c i))))
                         (chunk-cons (chunk b) (mapi (+ idx size) (chunk-rest s))))
                       (cons (f idx (first s)) (mapi (inc idx) (rest s)))))))]
     (mapi 0 coll))))

(§ defn keep
  "Returns a lazy sequence of the non-nil results of (f item). Note,
  this means false return values will be included.  f must be free of
  side-effects.  Returns a transducer when no collection is provided."
  {:added "1.2"
   :static true}
  ([f]
   (fn [rf]
     (fn
       ([] (rf))
       ([result] (rf result))
       ([result input]
          (let [v (f input)]
            (if (nil? v)
              result
              (rf result v)))))))
  ([f coll]
   (lazy-seq
    (when-let [s (seq coll)]
      (if (chunked-seq? s)
        (let [c (chunk-first s)
              size (count c)
              b (chunk-buffer size)]
          (dotimes [i size]
            (let [x (f (.nth c i))]
              (when-not (nil? x)
                (chunk-append b x))))
          (chunk-cons (chunk b) (keep f (chunk-rest s))))
        (let [x (f (first s))]
          (if (nil? x)
            (keep f (rest s))
            (cons x (keep f (rest s))))))))))

(§ defn keep-indexed
  "Returns a lazy sequence of the non-nil results of (f index item). Note,
  this means false return values will be included.  f must be free of
  side-effects.  Returns a stateful transducer when no collection is
  provided."
  {:added "1.2"
   :static true}
  ([f]
   (fn [rf]
     (let [iv (volatile! -1)]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
            (let [i (vswap! iv inc)
                  v (f i input)]
              (if (nil? v)
                result
                (rf result v))))))))
  ([f coll]
     (letfn [(keepi [idx coll]
               (lazy-seq
                (when-let [s (seq coll)]
                  (if (chunked-seq? s)
                    (let [c (chunk-first s)
                          size (count c)
                          b (chunk-buffer size)]
                      (dotimes [i size]
                        (let [x (f (+ idx i) (.nth c i))]
                          (when-not (nil? x)
                            (chunk-append b x))))
                      (chunk-cons (chunk b) (keepi (+ idx size) (chunk-rest s))))
                    (let [x (f idx (first s))]
                      (if (nil? x)
                        (keepi (inc idx) (rest s))
                        (cons x (keepi (inc idx) (rest s)))))))))]
       (keepi 0 coll))))

(§ defn bounded-count
  "If coll is counted? returns its count, else will count at most the first n
  elements of coll using its seq"
  {:added "1.9"}
  [n coll]
  (if (counted? coll)
    (count coll)
    (loop [i 0 s (seq coll)]
      (if (and s (< i n))
        (recur (inc i) (next s))
        i))))

(§ defn every-pred
  "Takes a set of predicates and returns a function f that returns true if all of its
  composing predicates return a logical true value against all of its arguments, else it returns
  false. Note that f is short-circuiting in that it will stop execution on the first
  argument that triggers a logical false result against the original predicates."
  {:added "1.3"}
  ([p]
     (fn ep1
       ([] true)
       ([x] (boolean (p x)))
       ([x y] (boolean (and (p x) (p y))))
       ([x y z] (boolean (and (p x) (p y) (p z))))
       ([x y z & args] (boolean (and (ep1 x y z)
                                     (every? p args))))))
  ([p1 p2]
     (fn ep2
       ([] true)
       ([x] (boolean (and (p1 x) (p2 x))))
       ([x y] (boolean (and (p1 x) (p1 y) (p2 x) (p2 y))))
       ([x y z] (boolean (and (p1 x) (p1 y) (p1 z) (p2 x) (p2 y) (p2 z))))
       ([x y z & args] (boolean (and (ep2 x y z)
                                     (every? #(and (p1 %) (p2 %)) args))))))
  ([p1 p2 p3]
     (fn ep3
       ([] true)
       ([x] (boolean (and (p1 x) (p2 x) (p3 x))))
       ([x y] (boolean (and (p1 x) (p2 x) (p3 x) (p1 y) (p2 y) (p3 y))))
       ([x y z] (boolean (and (p1 x) (p2 x) (p3 x) (p1 y) (p2 y) (p3 y) (p1 z) (p2 z) (p3 z))))
       ([x y z & args] (boolean (and (ep3 x y z)
                                     (every? #(and (p1 %) (p2 %) (p3 %)) args))))))
  ([p1 p2 p3 & ps]
     (let [ps (list* p1 p2 p3 ps)]
       (fn epn
         ([] true)
         ([x] (every? #(% x) ps))
         ([x y] (every? #(and (% x) (% y)) ps))
         ([x y z] (every? #(and (% x) (% y) (% z)) ps))
         ([x y z & args] (boolean (and (epn x y z)
                                       (every? #(every? % args) ps))))))))

(§ defn some-fn
  "Takes a set of predicates and returns a function f that returns the first logical true value
  returned by one of its composing predicates against any of its arguments, else it returns
  logical false. Note that f is short-circuiting in that it will stop execution on the first
  argument that triggers a logical true result against the original predicates."
  {:added "1.3"}
  ([p]
     (fn sp1
       ([] nil)
       ([x] (p x))
       ([x y] (or (p x) (p y)))
       ([x y z] (or (p x) (p y) (p z)))
       ([x y z & args] (or (sp1 x y z)
                           (some p args)))))
  ([p1 p2]
     (fn sp2
       ([] nil)
       ([x] (or (p1 x) (p2 x)))
       ([x y] (or (p1 x) (p1 y) (p2 x) (p2 y)))
       ([x y z] (or (p1 x) (p1 y) (p1 z) (p2 x) (p2 y) (p2 z)))
       ([x y z & args] (or (sp2 x y z)
                           (some #(or (p1 %) (p2 %)) args)))))
  ([p1 p2 p3]
     (fn sp3
       ([] nil)
       ([x] (or (p1 x) (p2 x) (p3 x)))
       ([x y] (or (p1 x) (p2 x) (p3 x) (p1 y) (p2 y) (p3 y)))
       ([x y z] (or (p1 x) (p2 x) (p3 x) (p1 y) (p2 y) (p3 y) (p1 z) (p2 z) (p3 z)))
       ([x y z & args] (or (sp3 x y z)
                           (some #(or (p1 %) (p2 %) (p3 %)) args)))))
  ([p1 p2 p3 & ps]
     (let [ps (list* p1 p2 p3 ps)]
       (fn spn
         ([] nil)
         ([x] (some #(% x) ps))
         ([x y] (some #(or (% x) (% y)) ps))
         ([x y z] (some #(or (% x) (% y) (% z)) ps))
         ([x y z & args] (or (spn x y z)
                             (some #(some % args) ps)))))))

(§ defn- ^{:dynamic true} assert-valid-fdecl
  "A good fdecl looks like (([a] ...) ([a b] ...)) near the end of defn."
  [fdecl]
  (when (empty? fdecl) (throw (IllegalArgumentException.
                                "Parameter declaration missing")))
  (let [argdecls (map
                   #(if (seq? %)
                      (first %)
                      (throw (IllegalArgumentException.
                        (if (seq? (first fdecl))
                          (str "Invalid signature \""
                               %
                               "\" should be a list")
                          (str "Parameter declaration \""
                               %
                               "\" should be a vector")))))
                   fdecl)
        bad-args (seq (remove #(vector? %) argdecls))]
    (when bad-args
      (throw (IllegalArgumentException. (str "Parameter declaration \"" (first bad-args)
                                             "\" should be a vector"))))))

(§ defn with-redefs-fn
  "Temporarily redefines Vars during a call to func.  Each val of
  binding-map will replace the root value of its key which must be
  a Var.  After func is called with no args, the root values of all
  the Vars will be set back to their old values.  These temporary
  changes will be visible in all threads.  Useful for mocking out
  functions during testing."
  {:added "1.3"}
  [binding-map func]
  (let [root-bind (fn [m]
                    (doseq [[a-var a-val] m]
                      (.bindRoot ^cloiure.lang.Var a-var a-val)))
        old-vals (zipmap (keys binding-map)
                         (map #(.getRawRoot ^cloiure.lang.Var %) (keys binding-map)))]
    (try
      (root-bind binding-map)
      (func)
      (finally
        (root-bind old-vals)))))

(§ defmacro with-redefs
  "binding => var-symbol temp-value-expr

  Temporarily redefines Vars while executing the body.  The
  temp-value-exprs will be evaluated and each resulting value will
  replace in parallel the root value of its Var.  After the body is
  executed, the root values of all the Vars will be set back to their
  old values.  These temporary changes will be visible in all threads.
  Useful for mocking out functions during testing."
  {:added "1.3"}
  [bindings & body]
  `(with-redefs-fn ~(zipmap (map #(list `var %) (take-nth 2 bindings))
                            (take-nth 2 (next bindings)))
                    (fn [] ~@body)))

(§ defn realized?
  "Returns true if a value has been produced for a promise, delay, future or lazy sequence."
  {:added "1.3"}
  [^cloiure.lang.IPending x] (.isRealized x))

(§ defmacro cond->
  "Takes an expression and a set of test/form pairs. Threads expr (via ->)
  through each form for which the corresponding test
  expression is true. Note that, unlike cond branching, cond-> threading does
  not short circuit after the first true test expression."
  {:added "1.5"}
  [expr & clauses]
  (assert (even? (count clauses)))
  (let [g (gensym)
        steps (map (fn [[test step]] `(if ~test (-> ~g ~step) ~g))
                   (partition 2 clauses))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))

(§ defmacro cond->>
  "Takes an expression and a set of test/form pairs. Threads expr (via ->>)
  through each form for which the corresponding test expression
  is true.  Note that, unlike cond branching, cond->> threading does not short circuit
  after the first true test expression."
  {:added "1.5"}
  [expr & clauses]
  (assert (even? (count clauses)))
  (let [g (gensym)
        steps (map (fn [[test step]] `(if ~test (->> ~g ~step) ~g))
                   (partition 2 clauses))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))

(§ defmacro as->
  "Binds name to expr, evaluates the first form in the lexical context
  of that binding, then binds name to that result, repeating for each
  successive form, returning the result of the last form."
  {:added "1.5"}
  [expr name & forms]
  `(let [~name ~expr
         ~@(interleave (repeat name) (butlast forms))]
     ~(if (empty? forms)
        name
        (last forms))))

(§ defmacro some->
  "When expr is not nil, threads it into the first form (via ->),
  and when that result is not nil, through the next etc"
  {:added "1.5"}
  [expr & forms]
  (let [g (gensym)
        steps (map (fn [step] `(if (nil? ~g) nil (-> ~g ~step)))
                   forms)]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))

(§ defmacro some->>
  "When expr is not nil, threads it into the first form (via ->>),
  and when that result is not nil, through the next etc"
  {:added "1.5"}
  [expr & forms]
  (let [g (gensym)
        steps (map (fn [step] `(if (nil? ~g) nil (->> ~g ~step)))
                   forms)]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))

(§ defn ^:private preserving-reduced
  [rf]
  #(let [ret (rf %1 %2)]
     (if (reduced? ret)
       (reduced ret)
       ret)))

(§ defn cat
  "A transducer which concatenates the contents of each input, which must be a
  collection, into the reduction."
  {:added "1.7"}
  [rf]
  (let [rrf (preserving-reduced rf)]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
         (reduce rrf result input)))))

(§ defn halt-when
  "Returns a transducer that ends transduction when pred returns true
  for an input. When retf is supplied it must be a fn of 2 arguments -
  it will be passed the (completed) result so far and the input that
  triggered the predicate, and its return value (if it does not throw
  an exception) will be the return value of the transducer. If retf
  is not supplied, the input that triggered the predicate will be
  returned. If the predicate never returns true the transduction is
  unaffected."
  {:added "1.9"}
  ([pred] (halt-when pred nil))
  ([pred retf]
     (fn [rf]
       (fn
         ([] (rf))
         ([result]
            (if (and (map? result) (contains? result ::halt))
              (::halt result)
              (rf result)))
         ([result input]
            (if (pred input)
              (reduced {::halt (if retf (retf (rf result) input) input)})
              (rf result input)))))))

(§ defn dedupe
  "Returns a lazy sequence removing consecutive duplicates in coll.
  Returns a transducer when no collection is provided."
  {:added "1.7"}
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
                (rf result input))))))))
  ([coll] (sequence (dedupe) coll)))

(§ defn random-sample
  "Returns items from coll with random probability of prob (0.0 -
  1.0).  Returns a transducer when no collection is provided."
  {:added "1.7"}
  ([prob]
     (filter (fn [_] (< (rand) prob))))
  ([prob coll]
     (filter (fn [_] (< (rand) prob)) coll)))

(§ deftype Eduction [xform coll]
   Iterable
   (iterator [_]
     (cloiure.lang.TransformerIterator/create xform (cloiure.lang.RT/iter coll)))

   cloiure.lang.IReduceInit
   (reduce [_ f init]
     ;; NB (completing f) isolates completion of inner rf from outer rf
     (transduce xform (completing f) init coll))

   cloiure.lang.Sequential)

(§ defn eduction
  "Returns a reducible/iterable application of the transducers
  to the items in coll. Transducers are applied in order as if
  combined with comp. Note that these applications will be
  performed every time reduce/iterator is called."
  {:arglists '([xform* coll])
   :added "1.7"}
  [& xforms]
  (Eduction. (apply comp (butlast xforms)) (last xforms)))

(§ defmethod print-method Eduction [c, ^Writer w]
  (if *print-readably*
    (do
      (print-sequential "(" pr-on " " ")" c w))
    (print-object c w)))

(§ defn run!
  "Runs the supplied procedure (via reduce), for purposes of side
  effects, on successive items in the collection. Returns nil"
  {:added "1.7"}
  [proc coll]
  (reduce #(proc %2) nil coll)
  nil)

(§ defn tagged-literal?
  "Return true if the value is the data representation of a tagged literal"
  {:added "1.7"}
  [value]
  (instance? cloiure.lang.TaggedLiteral value))

(§ defn tagged-literal
  "Construct a data representation of a tagged literal from a
  tag symbol and a form."
  {:added "1.7"}
  [^cloiure.lang.Symbol tag form]
  (cloiure.lang.TaggedLiteral/create tag form))

(§ defn reader-conditional?
  "Return true if the value is the data representation of a reader conditional"
  {:added "1.7"}
  [value]
  (instance? cloiure.lang.ReaderConditional value))

(§ defn reader-conditional
  "Construct a data representation of a reader conditional.
  If true, splicing? indicates read-cond-splicing."
  {:added "1.7"}
  [form ^Boolean splicing?]
  (cloiure.lang.ReaderConditional/create form splicing?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; data readers ;;;;;;;;;;;;;;;;;;

(§ def ^{:added "1.4"} default-data-readers
  "Default map of data reader functions provided by Cloiure. May be
  overridden by binding *data-readers*."
  (merge
    {'uuid #'cloiure.uuid/default-uuid-reader}
    (when-class "java.sql.Timestamp"
      {'inst #'cloiure.instant/read-instant-date})))

(§ def ^{:added "1.4" :dynamic true} *data-readers*
  "Map from reader tag symbols to data reader Vars.

  When Cloiure starts, it searches for files named 'data_readers.cli'
  and 'data_readers.clic' at the root of the classpath. Each such file
  must contain a literal map of symbols, like this:

      {foo/bar my.project.foo/bar
       foo/baz my.project/baz}

  The first symbol in each pair is a tag that will be recognized by
  the Cloiure reader. The second symbol in the pair is the
  fully-qualified name of a Var which will be invoked by the reader to
  parse the form following the tag. For example, given the
  data_readers.cli file above, the Cloiure reader would parse this
  form:

      #foo/bar [1 2 3]

  by invoking the Var #'my.project.foo/bar on the vector [1 2 3]. The
  data reader function is invoked on the form AFTER it has been read
  as a normal Cloiure data structure by the reader.

  Reader tags without namespace qualifiers are reserved for
  Cloiure. Default reader tags are defined in
  cloiure.core/default-data-readers but may be overridden in
  data_readers.cli, data_readers.clic, or by rebinding this Var."
  {})

(§ def ^{:added "1.5" :dynamic true} *default-data-reader-fn*
  "When no data reader is found for a tag and *default-data-reader-fn*
  is non-nil, it will be called with two arguments,
  the tag and the value.  If *default-data-reader-fn* is nil (the
  default), an exception will be thrown for the unknown tag."
  nil)

(§ defn- data-reader-urls []
  (let [cl (.. Thread currentThread getContextClassLoader)]
    (concat
      (enumeration-seq (.getResources cl "data_readers.cli"))
      (enumeration-seq (.getResources cl "data_readers.clic")))))

(§ defn- data-reader-var [sym]
  (intern (create-ns (symbol (namespace sym)))
          (symbol (name sym))))

(§ defn- load-data-reader-file [mappings ^java.net.URL url]
  (with-open [rdr (cloiure.lang.LineNumberingPushbackReader.
                   (java.io.InputStreamReader.
                    (.openStream url) "UTF-8"))]
    (binding [*file* (.getFile url)]
      (let [read-opts (if (.endsWith (.getPath url) "clic")
                        {:eof nil :read-cond :allow}
                        {:eof nil})
            new-mappings (read read-opts rdr)]
        (when (not (map? new-mappings))
          (throw (ex-info (str "Not a valid data-reader map")
                          {:url url})))
        (reduce
         (fn [m [k v]]
           (when (not (symbol? k))
             (throw (ex-info (str "Invalid form in data-reader file")
                             {:url url
                              :form k})))
           (let [v-var (data-reader-var v)]
             (when (and (contains? mappings k)
                        (not= (mappings k) v-var))
               (throw (ex-info "Conflicting data-reader mapping"
                               {:url url
                                :conflict k
                                :mappings m})))
             (assoc m k v-var)))
         mappings
         new-mappings)))))

(§ defn- load-data-readers []
  (alter-var-root #'*data-readers*
                  (fn [mappings]
                    (reduce load-data-reader-file
                            mappings (data-reader-urls)))))

(§ try
 (load-data-readers)
 (catch Throwable t
   (.printStackTrace t)
   (throw t)))

(§ defn uri?
  "Return true if x is a java.net.URI"
  {:added "1.9"}
  [x] (instance? java.net.URI x))
(§ in-ns 'cloiure.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;; definterface ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ defn namespace-munge
  "Convert a Cloiure namespace name to a legal Java package name."
  {:added "1.2"}
  [ns]
  (.replace (str ns) \- \_))

; for now, built on gen-interface
(§ defmacro definterface
  "Creates a new Java interface with the given name and method sigs.
  The method return types and parameter types may be specified with type hints,
  defaulting to Object if omitted.

  (definterface MyInterface
    (^int method1 [x])
    (^Bar method2 [^Baz b ^Quux q]))"
  {:added "1.2"} ;; Present since 1.2, but made public in 1.5.
  [name & sigs]
  (let [tag (fn [x] (or (:tag (meta x)) Object))
        psig (fn [[name [& args]]]
               (vector name (vec (map tag args)) (tag name) (map meta args)))
        cname (with-meta (symbol (str (namespace-munge *ns*) "." name)) (meta name))]
    `(let []
       (gen-interface :name ~cname :methods ~(vec (map psig sigs)))
       (import ~cname))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;; reify/deftype ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ defn- parse-opts [s]
  (loop [opts {} [k v & rs :as s] s]
    (if (keyword? k)
      (recur (assoc opts k v) rs)
      [opts s])))

(§ defn- parse-impls [specs]
  (loop [ret {} s specs]
    (if (seq s)
      (recur (assoc ret (first s) (take-while seq? (next s)))
             (drop-while seq? (next s)))
      ret)))

(§ defn- parse-opts+specs [opts+specs]
  (let [[opts specs] (parse-opts opts+specs)
        impls (parse-impls specs)
        interfaces (-> (map #(if (var? (resolve %))
                               (:on (deref (resolve %)))
                               %)
                            (keys impls))
                       set
                       (disj 'Object 'java.lang.Object)
                       vec)
        methods (map (fn [[name params & body]]
                       (cons name (maybe-destructured params body)))
                     (apply concat (vals impls)))]
    (when-let [bad-opts (seq (remove #{:no-print :load-ns} (keys opts)))]
      (throw (IllegalArgumentException. (apply print-str "Unsupported option(s) -" bad-opts))))
    [interfaces methods opts]))

(§ defmacro reify
  "reify is a macro with the following structure:

 (reify options* specs*)

  Currently there are no options.

  Each spec consists of the protocol or interface name followed by zero
  or more method bodies:

  protocol-or-interface-or-Object
  (methodName [args+] body)*

  Methods should be supplied for all methods of the desired
  protocol(s) and interface(s). You can also define overrides for
  methods of Object. Note that the first parameter must be supplied to
  correspond to the target object ('this' in Java parlance). Thus
  methods for interfaces will take one more argument than do the
  interface declarations.  Note also that recur calls to the method
  head should *not* pass the target object, it will be supplied
  automatically and can not be substituted.

  The return type can be indicated by a type hint on the method name,
  and arg types can be indicated by a type hint on arg names. If you
  leave out all hints, reify will try to match on same name/arity
  method in the protocol(s)/interface(s) - this is preferred. If you
  supply any hints at all, no inference is done, so all hints (or
  default of Object) must be correct, for both arguments and return
  type. If a method is overloaded in a protocol/interface, multiple
  independent method definitions must be supplied.  If overloaded with
  same arity in an interface you must specify complete hints to
  disambiguate - a missing hint implies Object.

  recur works to method heads The method bodies of reify are lexical
  closures, and can refer to the surrounding local scope:

  (str (let [f \"foo\"]
       (reify Object
         (toString [this] f))))
  == \"foo\"

  (seq (let [f \"foo\"]
       (reify cloiure.lang.Seqable
         (seq [this] (seq f)))))
  == (\\f \\o \\o))

  reify always implements cloiure.lang.IObj and transfers meta
  data of the form to the created object.

  (meta ^{:k :v} (reify Object (toString [this] \"foo\")))
  == {:k :v}"
  {:added "1.2"}
  [& opts+specs]
  (let [[interfaces methods] (parse-opts+specs opts+specs)]
    (with-meta `(reify* ~interfaces ~@methods) (meta &form))))

(§ defn hash-combine [x y]
  (cloiure.lang.Util/hashCombine x (cloiure.lang.Util/hash y)))

(§ defn munge [s]
  ((if (symbol? s) symbol str) (cloiure.lang.Compiler/munge (str s))))

(§ defn- imap-cons
  [^IPersistentMap this o]
  (cond
   (map-entry? o)
     (let [^java.util.Map$Entry pair o]
       (.assoc this (.getKey pair) (.getValue pair)))
   (instance? cloiure.lang.IPersistentVector o)
     (let [^cloiure.lang.IPersistentVector vec o]
       (.assoc this (.nth vec 0) (.nth vec 1)))
   :else (loop [this this
                o o]
      (if (seq o)
        (let [^java.util.Map$Entry pair (first o)]
          (recur (.assoc this (.getKey pair) (.getValue pair)) (rest o)))
        this))))

(§ defn- emit-defrecord
  "Do not use this directly - use defrecord"
  {:added "1.2"}
  [tagname cname fields interfaces methods opts]
  (let [classname (with-meta (symbol (str (namespace-munge *ns*) "." cname)) (meta cname))
        interfaces (vec interfaces)
        interface-set (set (map resolve interfaces))
        methodname-set (set (map first methods))
        hinted-fields fields
        fields (vec (map #(with-meta % nil) fields))
        base-fields fields
        fields (conj fields '__meta '__extmap
                     '^:unsynchronized-mutable __hash
                     '^:unsynchronized-mutable __hasheq)
        type-hash (hash classname)]
    (when (some #{:volatile-mutable :unsynchronized-mutable} (mapcat (comp keys meta) hinted-fields))
      (throw (IllegalArgumentException. ":volatile-mutable or :unsynchronized-mutable not supported for record fields")))
    (let [gs (gensym)]
    (letfn
     [(irecord [[i m]]
        [(conj i 'cloiure.lang.IRecord)
         m])
      (eqhash [[i m]]
        [(conj i 'cloiure.lang.IHashEq)
         (conj m
               `(hasheq [this#] (let [hq# ~'__hasheq]
                                  (if (zero? hq#)
                                    (let [h# (int (bit-xor ~type-hash (cloiure.lang.APersistentMap/mapHasheq this#)))]
                                      (set! ~'__hasheq h#)
                                      h#)
                                    hq#)))
               `(hashCode [this#] (let [hash# ~'__hash]
                                    (if (zero? hash#)
                                      (let [h# (cloiure.lang.APersistentMap/mapHash this#)]
                                        (set! ~'__hash h#)
                                        h#)
                                      hash#)))
               `(equals [this# ~gs] (cloiure.lang.APersistentMap/mapEquals this# ~gs)))])
      (iobj [[i m]]
            [(conj i 'cloiure.lang.IObj)
             (conj m `(meta [this#] ~'__meta)
                   `(withMeta [this# ~gs] (new ~tagname ~@(replace {'__meta gs} fields))))])
      (ilookup [[i m]]
         [(conj i 'cloiure.lang.ILookup 'cloiure.lang.IKeywordLookup)
          (conj m `(valAt [this# k#] (.valAt this# k# nil))
                `(valAt [this# k# else#]
                   (case k# ~@(mapcat (fn [fld] [(keyword fld) fld])
                                       base-fields)
                         (get ~'__extmap k# else#)))
                `(getLookupThunk [this# k#]
                   (let [~'gclass (class this#)]
                     (case k#
                           ~@(let [hinted-target (with-meta 'gtarget {:tag tagname})]
                               (mapcat
                                (fn [fld]
                                  [(keyword fld)
                                   `(reify cloiure.lang.ILookupThunk
                                           (get [~'thunk ~'gtarget]
                                                (if (identical? (class ~'gtarget) ~'gclass)
                                                  (. ~hinted-target ~(symbol (str "-" fld)))
                                                  ~'thunk)))])
                                base-fields))
                           nil))))])
      (imap [[i m]]
            [(conj i 'cloiure.lang.IPersistentMap)
             (conj m
                   `(count [this#] (+ ~(count base-fields) (count ~'__extmap)))
                   `(empty [this#] (throw (UnsupportedOperationException. (str "Can't create empty: " ~(str classname)))))
                   `(cons [this# e#] ((var imap-cons) this# e#))
                   `(equiv [this# ~gs]
                        (boolean
                         (or (identical? this# ~gs)
                             (when (identical? (class this#) (class ~gs))
                               (let [~gs ~(with-meta gs {:tag tagname})]
                                 (and  ~@(map (fn [fld] `(= ~fld (. ~gs ~(symbol (str "-" fld))))) base-fields)
                                       (= ~'__extmap (. ~gs ~'__extmap))))))))
                   `(containsKey [this# k#] (not (identical? this# (.valAt this# k# this#))))
                   `(entryAt [this# k#] (let [v# (.valAt this# k# this#)]
                                            (when-not (identical? this# v#)
                                              (cloiure.lang.MapEntry/create k# v#))))
                   `(seq [this#] (seq (concat [~@(map #(list `cloiure.lang.MapEntry/create (keyword %) %) base-fields)]
                                              ~'__extmap)))
                   `(iterator [~gs]
                        (cloiure.lang.RecordIterator. ~gs [~@(map keyword base-fields)] (RT/iter ~'__extmap)))
                   `(assoc [this# k# ~gs]
                     (condp identical? k#
                       ~@(mapcat (fn [fld]
                                   [(keyword fld) (list* `new tagname (replace {fld gs} (remove '#{__hash __hasheq} fields)))])
                                 base-fields)
                       (new ~tagname ~@(remove '#{__extmap __hash __hasheq} fields) (assoc ~'__extmap k# ~gs))))
                   `(without [this# k#] (if (contains? #{~@(map keyword base-fields)} k#)
                                            (dissoc (with-meta (into {} this#) ~'__meta) k#)
                                            (new ~tagname ~@(remove '#{__extmap __hash __hasheq} fields)
                                                 (not-empty (dissoc ~'__extmap k#))))))])
      (ijavamap [[i m]]
                [(conj i 'java.util.Map)
                 (conj m
                       `(size [this#] (.count this#))
                       `(isEmpty [this#] (= 0 (.count this#)))
                       `(containsValue [this# v#] (boolean (some #{v#} (vals this#))))
                       `(get [this# k#] (.valAt this# k#))
                       `(put [this# k# v#] (throw (UnsupportedOperationException.)))
                       `(remove [this# k#] (throw (UnsupportedOperationException.)))
                       `(putAll [this# m#] (throw (UnsupportedOperationException.)))
                       `(clear [this#] (throw (UnsupportedOperationException.)))
                       `(keySet [this#] (set (keys this#)))
                       `(values [this#] (vals this#))
                       `(entrySet [this#] (set this#)))])
      ]
     (let [[i m] (-> [interfaces methods] irecord eqhash iobj ilookup imap ijavamap)]
       `(deftype* ~(symbol (name (ns-name *ns*)) (name tagname)) ~classname
          ~(conj hinted-fields '__meta '__extmap
                 '^int ^:unsynchronized-mutable __hash
                 '^int ^:unsynchronized-mutable __hasheq)
          :implements ~(vec i)
          ~@(mapcat identity opts)
          ~@m))))))

(§ defn- build-positional-factory
  "Used to build a positional factory for a given type/record.  Because of the
  limitation of 20 arguments to Cloiure functions, this factory needs to be
  constructed to deal with more arguments.  It does this by building a straight
  forward type/record ctor call in the <=20 case, and a call to the same
  ctor pulling the extra args out of the & overage parameter.  Finally, the
  arity is constrained to the number of expected fields and an ArityException
  will be thrown at runtime if the actual arg count does not match."
  [nom classname fields]
  (let [fn-name (symbol (str '-> nom))
        [field-args over] (split-at 20 fields)
        field-count (count fields)
        arg-count (count field-args)
        over-count (count over)
        docstring (str "Positional factory function for class " classname ".")]
    `(defn ~fn-name
       ~docstring
       [~@field-args ~@(if (seq over) '[& overage] [])]
       ~(if (seq over)
          `(if (= (count ~'overage) ~over-count)
             (new ~classname
                  ~@field-args
                  ~@(for [i (range 0 (count over))]
                      (list `nth 'overage i)))
             (throw (cloiure.lang.ArityException. (+ ~arg-count (count ~'overage)) (name '~fn-name))))
          `(new ~classname ~@field-args)))))

(§ defn- validate-fields
  ""
  [fields name]
  (when-not (vector? fields)
    (throw (AssertionError. "No fields vector given.")))
  (let [specials '#{__meta __hash __hasheq __extmap}]
    (when (some specials fields)
      (throw (AssertionError. (str "The names in " specials " cannot be used as field names for types or records.")))))
  (let [non-syms (remove symbol? fields)]
    (when (seq non-syms)
      (throw (cloiure.lang.Compiler$CompilerException.
              *file*
              (.deref cloiure.lang.Compiler/LINE)
              (.deref cloiure.lang.Compiler/COLUMN)
              (AssertionError.
               (str "defrecord and deftype fields must be symbols, "
                    *ns* "." name " had: "
                    (apply str (interpose ", " non-syms)))))))))

(§ defmacro defrecord
  "(defrecord name [fields*]  options* specs*)

  Options are expressed as sequential keywords and arguments (in any order).

  Supported options:
  :load-ns - if true, importing the record class will cause the
             namespace in which the record was defined to be loaded.
             Defaults to false.

  Each spec consists of a protocol or interface name followed by zero
  or more method bodies:

  protocol-or-interface-or-Object
  (methodName [args*] body)*

  Dynamically generates compiled bytecode for class with the given
  name, in a package with the same name as the current namespace, the
  given fields, and, optionally, methods for protocols and/or
  interfaces.

  The class will have the (immutable) fields named by
  fields, which can have type hints. Protocols/interfaces and methods
  are optional. The only methods that can be supplied are those
  declared in the protocols/interfaces.  Note that method bodies are
  not closures, the local environment includes only the named fields,
  and those fields can be accessed directly.

  Method definitions take the form:

  (methodname [args*] body)

  The argument and return types can be hinted on the arg and
  methodname symbols. If not supplied, they will be inferred, so type
  hints should be reserved for disambiguation.

  Methods should be supplied for all methods of the desired
  protocol(s) and interface(s). You can also define overrides for
  methods of Object. Note that a parameter must be supplied to
  correspond to the target object ('this' in Java parlance). Thus
  methods for interfaces will take one more argument than do the
  interface declarations. Note also that recur calls to the method
  head should *not* pass the target object, it will be supplied
  automatically and can not be substituted.

  In the method bodies, the (unqualified) name can be used to name the
  class (for calls to new, instance? etc).

  The class will have implementations of several (cloiure.lang)
  interfaces generated automatically: IObj (metadata support) and
  IPersistentMap, and all of their superinterfaces.

  In addition, defrecord will define type-and-value-based =,
  and will defined Java .hashCode and .equals consistent with the
  contract for java.util.Map.

  When AOT compiling, generates compiled bytecode for a class with the
  given name (a symbol), prepends the current ns as the package, and
  writes the .class file to the *compile-path* directory.

  Two constructors will be defined, one taking the designated fields
  followed by a metadata map (nil for none) and an extension field
  map (nil for none), and one taking only the fields (using nil for
  meta and extension fields). Note that the field names __meta,
  __extmap, __hash and __hasheq are currently reserved and should not
  be used when defining your own records.

  Given (defrecord TypeName ...), two factory functions will be
  defined: ->TypeName, taking positional parameters for the fields,
  and map->TypeName, taking a map of keywords to field values."
  {:added "1.2"
   :arglists '([name [& fields] & opts+specs])}

  [name fields & opts+specs]
  (validate-fields fields name)
  (let [gname name
        [interfaces methods opts] (parse-opts+specs opts+specs)
        ns-part (namespace-munge *ns*)
        classname (symbol (str ns-part "." gname))
        hinted-fields fields
        fields (vec (map #(with-meta % nil) fields))]
    `(let []
       (declare ~(symbol (str  '-> gname)))
       (declare ~(symbol (str 'map-> gname)))
       ~(emit-defrecord name gname (vec hinted-fields) (vec interfaces) methods opts)
       (import ~classname)
       ~(build-positional-factory gname classname fields)
       (defn ~(symbol (str 'map-> gname))
         ~(str "Factory function for class " classname ", taking a map of keywords to field values.")
         ([m#] (~(symbol (str classname "/create"))
                (if (instance? cloiure.lang.MapEquivalence m#) m# (into {} m#)))))
       ~classname)))

(§ defn record?
  "Returns true if x is a record"
  {:added "1.6"
   :static true}
  [x]
  (instance? cloiure.lang.IRecord x))

(§ defn- emit-deftype*
  "Do not use this directly - use deftype"
  [tagname cname fields interfaces methods opts]
  (let [classname (with-meta (symbol (str (namespace-munge *ns*) "." cname)) (meta cname))
        interfaces (conj interfaces 'cloiure.lang.IType)]
    `(deftype* ~(symbol (name (ns-name *ns*)) (name tagname)) ~classname ~fields
       :implements ~interfaces
       ~@(mapcat identity opts)
       ~@methods)))

(§ defmacro deftype
  "(deftype name [fields*]  options* specs*)

  Options are expressed as sequential keywords and arguments (in any order).

  Supported options:
  :load-ns - if true, importing the type class will cause the
             namespace in which the type was defined to be loaded.
             Defaults to false.

  Each spec consists of a protocol or interface name followed by zero
  or more method bodies:

  protocol-or-interface-or-Object
  (methodName [args*] body)*

  Dynamically generates compiled bytecode for class with the given
  name, in a package with the same name as the current namespace, the
  given fields, and, optionally, methods for protocols and/or
  interfaces.

  The class will have the (by default, immutable) fields named by
  fields, which can have type hints. Protocols/interfaces and methods
  are optional. The only methods that can be supplied are those
  declared in the protocols/interfaces.  Note that method bodies are
  not closures, the local environment includes only the named fields,
  and those fields can be accessed directly. Fields can be qualified
  with the metadata :volatile-mutable true or :unsynchronized-mutable
  true, at which point (set! afield aval) will be supported in method
  bodies. Note well that mutable fields are extremely difficult to use
  correctly, and are present only to facilitate the building of higher
  level constructs, such as Cloiure's reference types, in Cloiure
  itself. They are for experts only - if the semantics and
  implications of :volatile-mutable or :unsynchronized-mutable are not
  immediately apparent to you, you should not be using them.

  Method definitions take the form:

  (methodname [args*] body)

  The argument and return types can be hinted on the arg and
  methodname symbols. If not supplied, they will be inferred, so type
  hints should be reserved for disambiguation.

  Methods should be supplied for all methods of the desired
  protocol(s) and interface(s). You can also define overrides for
  methods of Object. Note that a parameter must be supplied to
  correspond to the target object ('this' in Java parlance). Thus
  methods for interfaces will take one more argument than do the
  interface declarations. Note also that recur calls to the method
  head should *not* pass the target object, it will be supplied
  automatically and can not be substituted.

  In the method bodies, the (unqualified) name can be used to name the
  class (for calls to new, instance? etc).

  When AOT compiling, generates compiled bytecode for a class with the
  given name (a symbol), prepends the current ns as the package, and
  writes the .class file to the *compile-path* directory.

  One constructor will be defined, taking the designated fields.  Note
  that the field names __meta, __extmap, __hash and __hasheq are currently
  reserved and should not be used when defining your own types.

  Given (deftype TypeName ...), a factory function called ->TypeName
  will be defined, taking positional parameters for the fields"
  {:added "1.2"
   :arglists '([name [& fields] & opts+specs])}

  [name fields & opts+specs]
  (validate-fields fields name)
  (let [gname name
        [interfaces methods opts] (parse-opts+specs opts+specs)
        ns-part (namespace-munge *ns*)
        classname (symbol (str ns-part "." gname))
        hinted-fields fields
        fields (vec (map #(with-meta % nil) fields))
        [field-args over] (split-at 20 fields)]
    `(let []
       ~(emit-deftype* name gname (vec hinted-fields) (vec interfaces) methods opts)
       (import ~classname)
       ~(build-positional-factory gname classname fields)
       ~classname)))

;;;;;;;;;;;;;;;;;;;;;;; protocols ;;;;;;;;;;;;;;;;;;;;;;;;

(§ defn- expand-method-impl-cache [^cloiure.lang.MethodImplCache cache c f]
  (if (.map cache)
    (let [cs (assoc (.map cache) c (cloiure.lang.MethodImplCache$Entry. c f))]
      (cloiure.lang.MethodImplCache. (.protocol cache) (.methodk cache) cs))
    (let [cs (into1 {} (remove (fn [[c e]] (nil? e)) (map vec (partition 2 (.table cache)))))
          cs (assoc cs c (cloiure.lang.MethodImplCache$Entry. c f))]
      (if-let [[shift mask] (maybe-min-hash (map hash (keys cs)))]
        (let [table (make-array Object (* 2 (inc mask)))
              table (reduce1 (fn [^objects t [c e]]
                               (let [i (* 2 (int (shift-mask shift mask (hash c))))]
                                 (aset t i c)
                                 (aset t (inc i) e)
                                 t))
                             table cs)]
          (cloiure.lang.MethodImplCache. (.protocol cache) (.methodk cache) shift mask table))
        (cloiure.lang.MethodImplCache. (.protocol cache) (.methodk cache) cs)))))

(§ defn- super-chain [^Class c]
  (when c
    (cons c (super-chain (.getSuperclass c)))))

(§ defn- pref
  ([] nil)
  ([a] a)
  ([^Class a ^Class b]
     (if (.isAssignableFrom a b) b a)))

(§ defn find-protocol-impl [protocol x]
  (if (instance? (:on-interface protocol) x)
    x
    (let [c (class x)
          impl #(get (:impls protocol) %)]
      (or (impl c)
          (and c (or (first (remove nil? (map impl (butlast (super-chain c)))))
                     (when-let [t (reduce1 pref (filter impl (disj (supers c) Object)))]
                       (impl t))
                     (impl Object)))))))

(§ defn find-protocol-method [protocol methodk x]
  (get (find-protocol-impl protocol x) methodk))

(§ defn- protocol?
  [maybe-p]
  (boolean (:on-interface maybe-p)))

(§ defn- implements? [protocol atype]
  (and atype (.isAssignableFrom ^Class (:on-interface protocol) atype)))

(§ defn extends?
  "Returns true if atype extends protocol"
  {:added "1.2"}
  [protocol atype]
  (boolean (or (implements? protocol atype)
               (get (:impls protocol) atype))))

(§ defn extenders
  "Returns a collection of the types explicitly extending protocol"
  {:added "1.2"}
  [protocol]
  (keys (:impls protocol)))

(§ defn satisfies?
  "Returns true if x satisfies the protocol"
  {:added "1.2"}
  [protocol x]
  (boolean (find-protocol-impl protocol x)))

(§ defn -cache-protocol-fn [^cloiure.lang.AFunction pf x ^Class c ^cloiure.lang.IFn interf]
  (let [cache  (.__methodImplCache pf)
        f (if (.isInstance c x)
            interf
            (find-protocol-method (.protocol cache) (.methodk cache) x))]
    (when-not f
      (throw (IllegalArgumentException. (str "No implementation of method: " (.methodk cache)
                                             " of protocol: " (:var (.protocol cache))
                                             " found for class: " (if (nil? x) "nil" (.getName (class x)))))))
    (set! (.__methodImplCache pf) (expand-method-impl-cache cache (class x) f))
    f))

(§ defn- emit-method-builder [on-interface method on-method arglists]
  (let [methodk (keyword method)
        gthis (with-meta (gensym) {:tag 'cloiure.lang.AFunction})
        ginterf (gensym)]
    `(fn [cache#]
       (let [~ginterf
             (fn
               ~@(map
                  (fn [args]
                    (let [gargs (map #(gensym (str "gf__" % "__")) args)
                          target (first gargs)]
                      `([~@gargs]
                          (. ~(with-meta target {:tag on-interface}) (~(or on-method method) ~@(rest gargs))))))
                  arglists))
             ^cloiure.lang.AFunction f#
             (fn ~gthis
               ~@(map
                  (fn [args]
                    (let [gargs (map #(gensym (str "gf__" % "__")) args)
                          target (first gargs)]
                      `([~@gargs]
                          (let [cache# (.__methodImplCache ~gthis)
                                f# (.fnFor cache# (cloiure.lang.Util/classOf ~target))]
                            (if f#
                              (f# ~@gargs)
                              ((-cache-protocol-fn ~gthis ~target ~on-interface ~ginterf) ~@gargs))))))
                  arglists))]
         (set! (.__methodImplCache f#) cache#)
         f#))))

(§ defn -reset-methods [protocol]
  (doseq [[^cloiure.lang.Var v build] (:method-builders protocol)]
    (let [cache (cloiure.lang.MethodImplCache. protocol (keyword (.sym v)))]
      (.bindRoot v (build cache)))))

(§ defn- assert-same-protocol [protocol-var method-syms]
  (doseq [m method-syms]
    (let [v (resolve m)
          p (:protocol (meta v))]
      (when (and v (bound? v) (not= protocol-var p))
        (binding [*out* *err*]
          (println "Warning: protocol" protocol-var "is overwriting"
                   (if p
                     (str "method " (.sym v) " of protocol " (.sym p))
                     (str "function " (.sym v)))))))))

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
                      (vals sigs))]
  `(do
     (defonce ~name {})
     (gen-interface :name ~iname :methods ~meths)
     (alter-meta! (var ~name) assoc :doc ~(:doc opts))
     ~(when sigs
        `(#'assert-same-protocol (var ~name) '~(map :name (vals sigs))))
     (alter-var-root (var ~name) merge
                     (assoc ~opts
                       :sigs '~sigs
                       :var (var ~name)
                       :method-map
                         ~(and (:on opts)
                               (apply hash-map
                                      (mapcat
                                       (fn [s]
                                         [(keyword (:name s)) (keyword (or (:on s) (:name s)))])
                                       (vals sigs))))
                       :method-builders
                        ~(apply hash-map
                                (mapcat
                                 (fn [s]
                                   [`(intern *ns* (with-meta '~(:name s) (merge '~s {:protocol (var ~name)})))
                                    (emit-method-builder (:on-interface opts) (:name s) (:on s) (:arglists s))])
                                 (vals sigs)))))
     (-reset-methods ~name)
     '~name)))

(§ defmacro defprotocol
  "A protocol is a named set of named methods and their signatures:
  (defprotocol AProtocolName

    ;optional doc string
    \"A doc string for AProtocol abstraction\"

  ;method signatures
    (bar [this a b] \"bar docs\")
    (baz [this a] [this a b] [this a b c] \"baz docs\"))

  No implementations are provided. Docs can be specified for the
  protocol overall and for each method. The above yields a set of
  polymorphic functions and a protocol object. All are
  namespace-qualified by the ns enclosing the definition The resulting
  functions dispatch on the type of their first argument, which is
  required and corresponds to the implicit target object ('this' in
  Java parlance). defprotocol is dynamic, has no special compile-time
  effect, and defines no new types or classes. Implementations of
  the protocol methods can be provided using extend.

  defprotocol will automatically generate a corresponding interface,
  with the same name as the protocol, i.e. given a protocol:
  my.ns/Protocol, an interface: my.ns.Protocol. The interface will
  have methods corresponding to the protocol functions, and the
  protocol will automatically work with instances of the interface.

  Note that you should not use this interface with deftype or
  reify, as they support the protocol directly:

  (defprotocol P
    (foo [this])
    (bar-me [this] [this y]))

  (deftype Foo [a b c]
   P
    (foo [this] a)
    (bar-me [this] b)
    (bar-me [this y] (+ c y)))

  (bar-me (Foo. 1 2 3) 42)
  => 45

  (foo
    (let [x 42]
      (reify P
        (foo [this] 17)
        (bar-me [this] x)
        (bar-me [this y] x))))
  => 17"
  {:added "1.2"}
  [name & opts+sigs]
  (emit-protocol name opts+sigs))

(§ defn extend
  "Implementations of protocol methods can be provided using the extend construct:

  (extend AType
    AProtocol
     {:foo an-existing-fn
      :bar (fn [a b] ...)
      :baz (fn ([a]...) ([a b] ...)...)}
    BProtocol
      {...}
    ...)

  extend takes a type/class (or interface, see below), and one or more
  protocol + method map pairs. It will extend the polymorphism of the
  protocol's methods to call the supplied methods when an AType is
  provided as the first argument.

  Method maps are maps of the keyword-ized method names to ordinary
  fns. This facilitates easy reuse of existing fns and fn maps, for
  code reuse/mixins without derivation or composition. You can extend
  an interface to a protocol. This is primarily to facilitate interop
  with the host (e.g. Java) but opens the door to incidental multiple
  inheritance of implementation since a class can inherit from more
  than one interface, both of which extend the protocol. It is TBD how
  to specify which impl to use. You can extend a protocol on nil.

  If you are supplying the definitions explicitly (i.e. not reusing
  exsting functions or mixin maps), you may find it more convenient to
  use the extend-type or extend-protocol macros.

  Note that multiple independent extend clauses can exist for the same
  type, not all protocols need be defined in a single extend call.

  See also:
  extends?, satisfies?, extenders"
  {:added "1.2"}
  [atype & proto+mmaps]
  (doseq [[proto mmap] (partition 2 proto+mmaps)]
    (when-not (protocol? proto)
      (throw (IllegalArgumentException.
              (str proto " is not a protocol"))))
    (when (implements? proto atype)
      (throw (IllegalArgumentException.
              (str atype " already directly implements " (:on-interface proto) " for protocol:"
                   (:var proto)))))
    (-reset-methods (alter-var-root (:var proto) assoc-in [:impls atype] mmap))))

(§ defn- emit-impl [[p fs]]
  [p (zipmap (map #(-> % first keyword) fs)
             (map #(cons `fn (drop 1 %)) fs))])

(§ defn- emit-hinted-impl [c [p fs]]
  (let [hint (fn [specs]
               (let [specs (if (vector? (first specs))
                                        (list specs)
                                        specs)]
                 (map (fn [[[target & args] & body]]
                        (cons (apply vector (vary-meta target assoc :tag c) args)
                              body))
                      specs)))]
    [p (zipmap (map #(-> % first name keyword) fs)
               (map #(cons `fn (hint (drop 1 %))) fs))]))

(§ defn- emit-extend-type [c specs]
  (let [impls (parse-impls specs)]
    `(extend ~c
             ~@(mapcat (partial emit-hinted-impl c) impls))))

(§ defmacro extend-type
  "A macro that expands into an extend call. Useful when you are
  supplying the definitions explicitly inline, extend-type
  automatically creates the maps required by extend.  Propagates the
  class as a type hint on the first argument of all fns.

  (extend-type MyType
    Countable
      (cnt [c] ...)
    Foo
      (bar [x y] ...)
      (baz ([x] ...) ([x y & zs] ...)))

  expands into:

  (extend MyType
   Countable
     {:cnt (fn [c] ...)}
   Foo
     {:baz (fn ([x] ...) ([x y & zs] ...))
      :bar (fn [x y] ...)})"
  {:added "1.2"}
  [t & specs]
  (emit-extend-type t specs))

(§ defn- emit-extend-protocol [p specs]
  (let [impls (parse-impls specs)]
    `(do
       ~@(map (fn [[t fs]]
                `(extend-type ~t ~p ~@fs))
              impls))))

(§ defmacro extend-protocol
  "Useful when you want to provide several implementations of the same
  protocol all at once. Takes a single protocol and the implementation
  of that protocol for one or more types. Expands into calls to
  extend-type:

  (extend-protocol Protocol
    AType
      (foo [x] ...)
      (bar [x y] ...)
    BType
      (foo [x] ...)
      (bar [x y] ...)
    AClass
      (foo [x] ...)
      (bar [x y] ...)
    nil
      (foo [x] ...)
      (bar [x y] ...))

  expands into:

  (do
   (cloiure.core/extend-type AType Protocol
     (foo [x] ...)
     (bar [x y] ...))
   (cloiure.core/extend-type BType Protocol
     (foo [x] ...)
     (bar [x y] ...))
   (cloiure.core/extend-type AClass Protocol
     (foo [x] ...)
     (bar [x y] ...))
   (cloiure.core/extend-type nil Protocol
     (foo [x] ...)
     (bar [x y] ...)))"
  {:added "1.2"}

  [p & specs]
  (emit-extend-protocol p specs))
(§ in-ns 'cloiure.core)

(§ import 'java.time.Instant)

(§ set! *warn-on-reflection* true)

(§ extend-protocol Inst
  java.time.Instant
  (inst-ms* [inst] (.toEpochMilli ^java.time.Instant inst)))
(§ in-ns 'cloiure.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; printing ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ import '(java.io Writer))

(§ set! *warn-on-reflection* true)
(§ def ^:dynamic
 ^{:doc "*print-length* controls how many items of each collection the
  printer will print. If it is bound to logical false, there is no
  limit. Otherwise, it must be bound to an integer indicating the maximum
  number of items of each collection to print. If a collection contains
  more items, the printer will print items up to the limit followed by
  '...' to represent the remaining items. The root binding is nil
  indicating no limit."
   :added "1.0"}
 *print-length* nil)

(§ def ^:dynamic
 ^{:doc "*print-level* controls how many levels deep the printer will
  print nested objects. If it is bound to logical false, there is no
  limit. Otherwise, it must be bound to an integer indicating the maximum
  level to print. Each argument to print is at level 0; if an argument is a
  collection, its items are at level 1; and so on. If an object is a
  collection and is at a level greater than or equal to the value bound to
  *print-level*, the printer prints '#' to represent it. The root binding
  is nil indicating no limit."
   :added "1.0"}
 *print-level* nil)

(§ def ^:dynamic *verbose-defrecords* false)

(§ def ^:dynamic
 ^{:doc "*print-namespace-maps* controls whether the printer will print
  namespace map literal syntax. It defaults to false, but the REPL binds
  to true."
   :added "1.9"}
 *print-namespace-maps* false)

(§ defn- print-sequential [^String begin, print-one, ^String sep, ^String end, sequence, ^Writer w]
  (binding [*print-level* (and (not *print-dup*) *print-level* (dec *print-level*))]
    (if (and *print-level* (neg? *print-level*))
      (.write w "#")
      (do
        (.write w begin)
        (when-let [xs (seq sequence)]
          (if (and (not *print-dup*) *print-length*)
            (loop [[x & xs] xs
                   print-length *print-length*]
              (if (zero? print-length)
                (.write w "...")
                (do
                  (print-one x w)
                  (when xs
                    (.write w sep)
                    (recur xs (dec print-length))))))
            (loop [[x & xs] xs]
              (print-one x w)
              (when xs
                (.write w sep)
                (recur xs)))))
        (.write w end)))))

(§ defn- print-meta [o, ^Writer w]
  (when-let [m (meta o)]
    (when (and (pos? (count m))
               (or *print-dup*
                   (and *print-meta* *print-readably*)))
      (.write w "^")
      (if (and (= (count m) 1) (:tag m))
          (pr-on (:tag m) w)
          (pr-on m w))
      (.write w " "))))

(§ defn print-simple [o, ^Writer w]
  (print-meta o w)
  (.write w (str o)))

(§ defmethod print-method :default [o, ^Writer w]
  (if (instance? cloiure.lang.IObj o)
    (print-method (vary-meta o #(dissoc % :type)) w)
    (print-simple o w)))

(§ defmethod print-method nil [o, ^Writer w]
  (.write w "nil"))

(§ defmethod print-dup nil [o w] (print-method o w))

(§ defn print-ctor [o print-args ^Writer w]
  (.write w "#=(")
  (.write w (.getName ^Class (class o)))
  (.write w ". ")
  (print-args o w)
  (.write w ")"))

(§ defn- print-tagged-object [o rep ^Writer w]
  (when (instance? cloiure.lang.IMeta o)
    (print-meta o w))
  (.write w "#object[")
  (let [c (class o)]
    (if (.isArray c)
      (print-method (.getName c) w)
      (.write w (.getName c))))
  (.write w " ")
  (.write w (format "0x%x " (System/identityHashCode o)))
  (print-method rep w)
  (.write w "]"))

(§ defn- print-object [o, ^Writer w]
  (print-tagged-object o (str o) w))

(§ defmethod print-method Object [o, ^Writer w]
  (print-object o w))

(§ defmethod print-method cloiure.lang.Keyword [o, ^Writer w]
  (.write w (str o)))

(§ defmethod print-dup cloiure.lang.Keyword [o w] (print-method o w))

(§ defmethod print-method Number [o, ^Writer w]
  (.write w (str o)))

(§ defmethod print-method Double [o, ^Writer w]
  (cond
    (= Double/POSITIVE_INFINITY o) (.write w "##Inf")
    (= Double/NEGATIVE_INFINITY o) (.write w "##-Inf")
    (.isNaN ^Double o) (.write w "##NaN")
    :else (.write w (str o))))

(§ defmethod print-method Float [o, ^Writer w]
  (cond
    (= Float/POSITIVE_INFINITY o) (.write w "##Inf")
    (= Float/NEGATIVE_INFINITY o) (.write w "##-Inf")
    (.isNaN ^Float o) (.write w "##NaN")
    :else (.write w (str o))))

(§ defmethod print-dup Number [o, ^Writer w]
  (print-ctor o
              (fn [o w]
                  (print-dup (str o) w))
              w))

(§ defmethod print-dup cloiure.lang.Fn [o, ^Writer w]
  (print-ctor o (fn [o w]) w))

(§ prefer-method print-dup cloiure.lang.IPersistentCollection cloiure.lang.Fn)
(§ prefer-method print-dup java.util.Map cloiure.lang.Fn)
(§ prefer-method print-dup java.util.Collection cloiure.lang.Fn)

(§ defmethod print-method Boolean [o, ^Writer w]
  (.write w (str o)))

(§ defmethod print-dup Boolean [o w] (print-method o w))

(§ defmethod print-method cloiure.lang.Symbol [o, ^Writer w]
  (print-simple o w))

(§ defmethod print-dup cloiure.lang.Symbol [o w] (print-method o w))

(§ defmethod print-method cloiure.lang.Var [o, ^Writer w]
  (print-simple o w))

(§ defmethod print-dup cloiure.lang.Var [^cloiure.lang.Var o, ^Writer w]
  (.write w (str "#=(var " (.name (.ns o)) "/" (.sym o) ")")))

(§ defmethod print-method cloiure.lang.ISeq [o, ^Writer w]
  (print-meta o w)
  (print-sequential "(" pr-on " " ")" o w))

(§ defmethod print-dup cloiure.lang.ISeq [o w] (print-method o w))
(§ defmethod print-dup cloiure.lang.IPersistentList [o w] (print-method o w))
(§ prefer-method print-method cloiure.lang.ISeq cloiure.lang.IPersistentCollection)
(§ prefer-method print-dup cloiure.lang.ISeq cloiure.lang.IPersistentCollection)
(§ prefer-method print-method cloiure.lang.ISeq java.util.Collection)
(§ prefer-method print-dup cloiure.lang.ISeq java.util.Collection)

(§ defmethod print-dup java.util.Collection [o, ^Writer w]
 (print-ctor o #(print-sequential "[" print-dup " " "]" %1 %2) w))

(§ defmethod print-dup cloiure.lang.IPersistentCollection [o, ^Writer w]
  (print-meta o w)
  (.write w "#=(")
  (.write w (.getName ^Class (class o)))
  (.write w "/create ")
  (print-sequential "[" print-dup " " "]" o w)
  (.write w ")"))

(§ prefer-method print-dup cloiure.lang.IPersistentCollection java.util.Collection)

(§ def ^{:tag String
       :doc "Returns escape string for char or nil if none"
       :added "1.0"}
  char-escape-string
    {\newline "\\n"
     \tab  "\\t"
     \return "\\r"
     \" "\\\""
     \\  "\\\\"
     \formfeed "\\f"
     \backspace "\\b"})

(§ defmethod print-method String [^String s, ^Writer w]
  (if (or *print-dup* *print-readably*)
    (do (.append w \")
      (dotimes [n (count s)]
        (let [c (.charAt s n)
              e (char-escape-string c)]
          (if e (.write w e) (.append w c))))
      (.append w \"))
    (.write w s))
  nil)

(§ defmethod print-dup String [s w] (print-method s w))

(§ defmethod print-method cloiure.lang.IPersistentVector [v, ^Writer w]
  (print-meta v w)
  (print-sequential "[" pr-on " " "]" v w))

(§ defn- print-prefix-map [prefix m print-one w]
  (print-sequential
    (str prefix "{")
    (fn [e ^Writer w]
      (do (print-one (key e) w) (.append w \space) (print-one (val e) w)))
    ", "
    "}"
    (seq m) w))

(§ defn- print-map [m print-one w]
  (print-prefix-map nil m print-one w))

(§ defn- strip-ns
  [named]
  (if (symbol? named)
    (symbol nil (name named))
    (keyword nil (name named))))

(§ defn- lift-ns
  "Returns [lifted-ns lifted-map] or nil if m can't be lifted."
  [m]
  (when *print-namespace-maps*
    (loop [ns nil
           [[k v :as entry] & entries] (seq m)
           lm {}]
      (if entry
        (when (or (keyword? k) (symbol? k))
          (if ns
            (when (= ns (namespace k))
              (recur ns entries (assoc lm (strip-ns k) v)))
            (when-let [new-ns (namespace k)]
              (recur new-ns entries (assoc lm (strip-ns k) v)))))
        [ns (apply conj (empty m) lm)]))))

(§ defmethod print-method cloiure.lang.IPersistentMap [m, ^Writer w]
  (print-meta m w)
  (let [[ns lift-map] (lift-ns m)]
    (if ns
      (print-prefix-map (str "#:" ns) lift-map pr-on w)
      (print-map m pr-on w))))

(§ defmethod print-dup java.util.Map [m, ^Writer w]
  (print-ctor m #(print-map (seq %1) print-dup %2) w))

(§ defmethod print-dup cloiure.lang.IPersistentMap [m, ^Writer w]
  (print-meta m w)
  (.write w "#=(")
  (.write w (.getName (class m)))
  (.write w "/create ")
  (print-map m print-dup w)
  (.write w ")"))

;; java.util
(§ prefer-method print-method cloiure.lang.IPersistentCollection java.util.Collection)
(§ prefer-method print-method cloiure.lang.IPersistentCollection java.util.RandomAccess)
(§ prefer-method print-method java.util.RandomAccess java.util.List)
(§ prefer-method print-method cloiure.lang.IPersistentCollection java.util.Map)

(§ defmethod print-method java.util.List [c, ^Writer w]
  (if *print-readably*
    (do
      (print-meta c w)
      (print-sequential "(" pr-on " " ")" c w))
    (print-object c w)))

(§ defmethod print-method java.util.RandomAccess [v, ^Writer w]
  (if *print-readably*
    (do
      (print-meta v w)
      (print-sequential "[" pr-on " " "]" v w))
    (print-object v w)))

(§ defmethod print-method java.util.Map [m, ^Writer w]
  (if *print-readably*
    (do
      (print-meta m w)
      (print-map m pr-on w))
    (print-object m w)))

(§ defmethod print-method java.util.Set [s, ^Writer w]
  (if *print-readably*
    (do
      (print-meta s w)
      (print-sequential "#{" pr-on " " "}" (seq s) w))
    (print-object s w)))

;; Records

(§ defmethod print-method cloiure.lang.IRecord [r, ^Writer w]
  (print-meta r w)
  (.write w "#")
  (.write w (.getName (class r)))
  (print-map r pr-on w))

(§ defmethod print-dup cloiure.lang.IRecord [r, ^Writer w]
  (print-meta r w)
  (.write w "#")
  (.write w (.getName (class r)))
  (if *verbose-defrecords*
    (print-map r print-dup w)
    (print-sequential "[" pr-on ", " "]" (vals r) w)))

(§ prefer-method print-method cloiure.lang.IRecord java.util.Map)
(§ prefer-method print-method cloiure.lang.IRecord cloiure.lang.IPersistentMap)
(§ prefer-method print-dup cloiure.lang.IRecord cloiure.lang.IPersistentMap)
(§ prefer-method print-dup cloiure.lang.IPersistentCollection java.util.Map)
(§ prefer-method print-dup cloiure.lang.IRecord cloiure.lang.IPersistentCollection)
(§ prefer-method print-dup cloiure.lang.IRecord java.util.Map)

(§ defmethod print-method cloiure.lang.IPersistentSet [s, ^Writer w]
  (print-meta s w)
  (print-sequential "#{" pr-on " " "}" (seq s) w))

(§ def ^{:tag String
       :doc "Returns name string for char or nil if none"
       :added "1.0"}
 char-name-string
   {\newline "newline"
    \tab "tab"
    \space "space"
    \backspace "backspace"
    \formfeed "formfeed"
    \return "return"})

(§ defmethod print-method java.lang.Character [^Character c, ^Writer w]
  (if (or *print-dup* *print-readably*)
    (do (.append w \\)
        (let [n (char-name-string c)]
          (if n (.write w n) (.append w c))))
    (.append w c))
  nil)

(§ defmethod print-dup java.lang.Character [c w] (print-method c w))
(§ defmethod print-dup java.lang.Long [o w] (print-method o w))
(§ defmethod print-dup java.lang.Double [o w] (print-method o w))
(§ defmethod print-dup cloiure.lang.Ratio [o w] (print-method o w))
(§ defmethod print-dup java.math.BigDecimal [o w] (print-method o w))
(§ defmethod print-dup cloiure.lang.BigInt [o w] (print-method o w))
(§ defmethod print-dup cloiure.lang.PersistentHashMap [o w] (print-method o w))
(§ defmethod print-dup cloiure.lang.PersistentHashSet [o w] (print-method o w))
(§ defmethod print-dup cloiure.lang.PersistentVector [o w] (print-method o w))
(§ defmethod print-dup cloiure.lang.LazilyPersistentVector [o w] (print-method o w))

(§ def primitives-classnames
  {Float/TYPE "Float/TYPE"
   Integer/TYPE "Integer/TYPE"
   Long/TYPE "Long/TYPE"
   Boolean/TYPE "Boolean/TYPE"
   Character/TYPE "Character/TYPE"
   Double/TYPE "Double/TYPE"
   Byte/TYPE "Byte/TYPE"
   Short/TYPE "Short/TYPE"})

(§ defmethod print-method Class [^Class c, ^Writer w]
  (.write w (.getName c)))

(§ defmethod print-dup Class [^Class c, ^Writer w]
  (cond
    (.isPrimitive c) (do
                       (.write w "#=(identity ")
                       (.write w ^String (primitives-classnames c))
                       (.write w ")"))
    (.isArray c) (do
                   (.write w "#=(java.lang.Class/forName \"")
                   (.write w (.getName c))
                   (.write w "\")"))
    :else (do
            (.write w "#=")
            (.write w (.getName c)))))

(§ defmethod print-method java.math.BigDecimal [b, ^Writer w]
  (.write w (str b))
  (.write w "M"))

(§ defmethod print-method cloiure.lang.BigInt [b, ^Writer w]
  (.write w (str b))
  (.write w "N"))

(§ defmethod print-method java.util.regex.Pattern [p ^Writer w]
  (.write w "#\"")
  (loop [[^Character c & r :as s] (seq (.pattern ^java.util.regex.Pattern p))
         qmode false]
    (when s
      (cond
        (= c \\) (let [[^Character c2 & r2] r]
                   (.append w \\)
                   (.append w c2)
                   (if qmode
                      (recur r2 (not= c2 \E))
                      (recur r2 (= c2 \Q))))
        (= c \") (do
                   (if qmode
                     (.write w "\\E\\\"\\Q")
                     (.write w "\\\""))
                   (recur r qmode))
        :else    (do
                   (.append w c)
                   (recur r qmode)))))
  (.append w \"))

(§ defmethod print-dup java.util.regex.Pattern [p ^Writer w] (print-method p w))

(§ defmethod print-dup cloiure.lang.Namespace [^cloiure.lang.Namespace n ^Writer w]
  (.write w "#=(find-ns ")
  (print-dup (.name n) w)
  (.write w ")"))

(§ defn- deref-as-map [^cloiure.lang.IDeref o]
  (let [pending (and (instance? cloiure.lang.IPending o)
                     (not (.isRealized ^cloiure.lang.IPending o)))
        [ex val]
        (when-not pending
          (try [false (deref o)]
               (catch Throwable e
                 [true e])))]
    {:status
     (cond
      (or ex
          (and (instance? cloiure.lang.Agent o)
               (agent-error o)))
      :failed

      pending
      :pending

      :else
      :ready)

     :val val}))

(§ defmethod print-method cloiure.lang.IDeref [o ^Writer w]
  (print-tagged-object o (deref-as-map o) w))

(§ defmethod print-method StackTraceElement [^StackTraceElement o ^Writer w]
  (print-method [(symbol (.getClassName o)) (symbol (.getMethodName o)) (.getFileName o) (.getLineNumber o)] w))

(§ defn StackTraceElement->vec
  "Constructs a data representation for a StackTraceElement"
  {:added "1.9"}
  [^StackTraceElement o]
  [(symbol (.getClassName o)) (symbol (.getMethodName o)) (.getFileName o) (.getLineNumber o)])

(§ defn Throwable->map
  "Constructs a data representation for a Throwable."
  {:added "1.7"}
  [^Throwable o]
  (let [base (fn [^Throwable t]
               (merge {:type (symbol (.getName (class t)))
                       :message (.getLocalizedMessage t)}
                 (when-let [ed (ex-data t)]
                   {:data ed})
                 (let [st (.getStackTrace t)]
                   (when (pos? (alength st))
                     {:at (StackTraceElement->vec (aget st 0))}))))
        via (loop [via [], ^Throwable t o]
              (if t
                (recur (conj via t) (.getCause t))
                via))
        ^Throwable root (peek via)
        m {:cause (.getLocalizedMessage root)
           :via (vec (map base via))
           :trace (vec (map StackTraceElement->vec
                            (.getStackTrace ^Throwable (or root o))))}
        data (ex-data root)]
    (if data
      (assoc m :data data)
      m)))

(§ defn- print-throwable [^Throwable o ^Writer w]
  (.write w "#error {\n :cause ")
  (let [{:keys [cause data via trace]} (Throwable->map o)
        print-via #(do (.write w "{:type ")
                               (print-method (:type %) w)
                                           (.write w "\n   :message ")
                                           (print-method (:message %) w)
             (when-let [data (:data %)]
               (.write w "\n   :data ")
               (print-method data w))
             (when-let [at (:at %)]
               (.write w "\n   :at ")
               (print-method (:at %) w))
             (.write w "}"))]
    (print-method cause w)
    (when data
      (.write w "\n :data ")
      (print-method data w))
    (when via
      (.write w "\n :via\n [")
      (when-let [fv (first via)]
            (print-via fv)
        (doseq [v (rest via)]
          (.write w "\n  ")
                  (print-via v)))
      (.write w "]"))
    (when trace
      (.write w "\n :trace\n [")
      (when-let [ft (first trace)]
        (print-method ft w)
        (doseq [t (rest trace)]
          (.write w "\n  ")
          (print-method t w)))
      (.write w "]")))
  (.write w "}"))

(§ defmethod print-method Throwable [^Throwable o ^Writer w]
  (print-throwable o w))

(§ defmethod print-method cloiure.lang.TaggedLiteral [o ^Writer w]
  (.write w "#")
  (print-method (:tag o) w)
  (.write w " ")
  (print-method (:form o) w))

(§ defmethod print-method cloiure.lang.ReaderConditional [o ^Writer w]
  (.write w "#?")
  (when (:splicing? o) (.write w "@"))
  (print-method (:form o) w))

(§ def ^{:private true} print-initialized true)
(§ in-ns 'cloiure.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;; proxy ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ import
 '(cloiure.asm ClassWriter ClassVisitor Opcodes Type)
 '(java.lang.reflect Modifier Constructor)
 '(java.io Serializable NotSerializableException)
 '(cloiure.asm.commons Method GeneratorAdapter)
 '(cloiure.lang IProxy Reflector DynamicClassLoader IPersistentMap PersistentHashMap RT))

(§ defn method-sig [^java.lang.reflect.Method meth]
  [(. meth (getName)) (seq (. meth (getParameterTypes))) (. meth getReturnType)])

(§ defn- most-specific [rtypes]
  (or (some (fn [t] (when (every? #(isa? t %) rtypes) t)) rtypes)
    (throw (Exception. "Incompatible return types"))))

(§ defn- group-by-sig
  "Takes a collection of [msig meth] and returns a seq of maps from
   return-types to meths."
  [coll]
  (vals (reduce1 (fn [m [msig meth]]
                  (let [rtype (peek msig)
                        argsig (pop msig)]
                    (assoc m argsig (assoc (m argsig {}) rtype meth))))
          {} coll)))

(§ defn proxy-name
 {:tag String}
 [^Class super interfaces]
  (let [inames (into1 (sorted-set) (map #(.getName ^Class %) interfaces))]
    (apply str (.replace (str *ns*) \- \_) ".proxy"
      (interleave (repeat "$")
        (concat
          [(.getName super)]
          (map #(subs % (inc (.lastIndexOf ^String % "."))) inames)
          [(Integer/toHexString (hash inames))])))))

(§ defn- generate-proxy [^Class super interfaces]
  (let [cv (new ClassWriter (. ClassWriter COMPUTE_MAXS))
        pname (proxy-name super interfaces)
        cname (.replace pname \. \/) ;; (str "cloiure/lang/" (gensym "Proxy__"))
        ctype (. Type (getObjectType cname))
        iname (fn [^Class c] (.. Type (getType c) (getInternalName)))
        fmap "__cloiureFnMap"
        totype (fn [^Class c] (. Type (getType c)))
        to-types (fn [cs] (if (pos? (count cs))
                            (into-array (map totype cs))
                            (make-array Type 0)))
        super-type ^Type (totype super)
        imap-type ^Type (totype IPersistentMap)
        ifn-type (totype cloiure.lang.IFn)
        obj-type (totype Object)
        sym-type (totype cloiure.lang.Symbol)
        rt-type  (totype cloiure.lang.RT)
        ex-type  (totype java.lang.UnsupportedOperationException)
        gen-bridge
        (fn [^java.lang.reflect.Method meth ^java.lang.reflect.Method dest]
            (let [pclasses (. meth (getParameterTypes))
                  ptypes (to-types pclasses)
                  rtype ^Type (totype (. meth (getReturnType)))
                  m (new Method (. meth (getName)) rtype ptypes)
                  dtype (totype (.getDeclaringClass dest))
                  dm (new Method (. dest (getName)) (totype (. dest (getReturnType))) (to-types (. dest (getParameterTypes))))
                  gen (new GeneratorAdapter (bit-or (. Opcodes ACC_PUBLIC) (. Opcodes ACC_BRIDGE)) m nil nil cv)]
              (. gen (visitCode))
              (. gen (loadThis))
              (dotimes [i (count ptypes)]
                  (. gen (loadArg i)))
              (if (-> dest .getDeclaringClass .isInterface)
                (. gen (invokeInterface dtype dm))
                (. gen (invokeVirtual dtype dm)))
              (. gen (returnValue))
              (. gen (endMethod))))
        gen-method
        (fn [^java.lang.reflect.Method meth else-gen]
            (let [pclasses (. meth (getParameterTypes))
                  ptypes (to-types pclasses)
                  rtype ^Type (totype (. meth (getReturnType)))
                  m (new Method (. meth (getName)) rtype ptypes)
                  gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)
                  else-label (. gen (newLabel))
                  end-label (. gen (newLabel))
                  decl-type (. Type (getType (. meth (getDeclaringClass))))]
              (. gen (visitCode))
              (if (> (count pclasses) 18)
                (else-gen gen m)
                (do
                  (. gen (loadThis))
                  (. gen (getField ctype fmap imap-type))

                  (. gen (push (. meth (getName))))
                                        ;; lookup fn in map
                  (. gen (invokeStatic rt-type (. Method (getMethod "Object get(Object, Object)"))))
                  (. gen (dup))
                  (. gen (ifNull else-label))
                                        ;; if found
                  (.checkCast gen ifn-type)
                  (. gen (loadThis))
                                        ;; box args
                  (dotimes [i (count ptypes)]
                      (. gen (loadArg i))
                    (. cloiure.lang.Compiler$HostExpr (emitBoxReturn nil gen (nth pclasses i))))
                                        ;; call fn
                  (. gen (invokeInterface ifn-type (new Method "invoke" obj-type
                                                        (into-array (cons obj-type
                                                                          (replicate (count ptypes) obj-type))))))
                                        ;; unbox return
                  (. gen (unbox rtype))
                  (when (= (. rtype (getSort)) (. Type VOID))
                    (. gen (pop)))
                  (. gen (goTo end-label))

                                        ;; else call supplied alternative generator
                  (. gen (mark else-label))
                  (. gen (pop))

                  (else-gen gen m)

                  (. gen (mark end-label))))
              (. gen (returnValue))
              (. gen (endMethod))))]

                                        ;; start class definition
    (. cv (visit (. Opcodes V1_5) (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_SUPER))
                 cname nil (iname super)
                 (into-array (map iname (cons IProxy interfaces)))))
                                        ;; add field for fn mappings
    (. cv (visitField (+ (. Opcodes ACC_PRIVATE) (. Opcodes ACC_VOLATILE))
                      fmap (. imap-type (getDescriptor)) nil nil))
                                        ;; add ctors matching/calling super's
    (doseq [^Constructor ctor (. super (getDeclaredConstructors))]
        (when-not (. Modifier (isPrivate (. ctor (getModifiers))))
          (let [ptypes (to-types (. ctor (getParameterTypes)))
                m (new Method "<init>" (. Type VOID_TYPE) ptypes)
                gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)]
            (. gen (visitCode))
                                        ;; call super ctor
            (. gen (loadThis))
            (. gen (dup))
            (. gen (loadArgs))
            (. gen (invokeConstructor super-type m))

            (. gen (returnValue))
            (. gen (endMethod)))))
                                        ;; disable serialization
    (when (some #(isa? % Serializable) (cons super interfaces))
      (let [m (. Method (getMethod "void writeObject(java.io.ObjectOutputStream)"))
            gen (new GeneratorAdapter (. Opcodes ACC_PRIVATE) m nil nil cv)]
        (. gen (visitCode))
        (. gen (loadThis))
        (. gen (loadArgs))
        (. gen (throwException (totype NotSerializableException) pname))
        (. gen (endMethod)))
      (let [m (. Method (getMethod "void readObject(java.io.ObjectInputStream)"))
            gen (new GeneratorAdapter (. Opcodes ACC_PRIVATE) m nil nil cv)]
        (. gen (visitCode))
        (. gen (loadThis))
        (. gen (loadArgs))
        (. gen (throwException (totype NotSerializableException) pname))
        (. gen (endMethod))))
                                        ;; add IProxy methods
    (let [m (. Method (getMethod "void __initCloiureFnMappings(cloiure.lang.IPersistentMap)"))
          gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)]
      (. gen (visitCode))
      (. gen (loadThis))
      (. gen (loadArgs))
      (. gen (putField ctype fmap imap-type))

      (. gen (returnValue))
      (. gen (endMethod)))
    (let [m (. Method (getMethod "void __updateCloiureFnMappings(cloiure.lang.IPersistentMap)"))
          gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)]
      (. gen (visitCode))
      (. gen (loadThis))
      (. gen (dup))
      (. gen (getField ctype fmap imap-type))
      (.checkCast gen (totype cloiure.lang.IPersistentCollection))
      (. gen (loadArgs))
      (. gen (invokeInterface (totype cloiure.lang.IPersistentCollection)
                              (. Method (getMethod "cloiure.lang.IPersistentCollection cons(Object)"))))
      (. gen (checkCast imap-type))
      (. gen (putField ctype fmap imap-type))

      (. gen (returnValue))
      (. gen (endMethod)))
    (let [m (. Method (getMethod "cloiure.lang.IPersistentMap __getCloiureFnMappings()"))
          gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)]
      (. gen (visitCode))
      (. gen (loadThis))
      (. gen (getField ctype fmap imap-type))
      (. gen (returnValue))
      (. gen (endMethod)))

                                        ;; calc set of supers' non-private instance methods
    (let [[mm considered]
            (loop [mm {} considered #{} c super]
              (if c
                (let [[mm considered]
                      (loop [mm mm
                             considered considered
                             meths (concat
                                    (seq (. c (getDeclaredMethods)))
                                    (seq (. c (getMethods))))]
                        (if (seq meths)
                          (let [^java.lang.reflect.Method meth (first meths)
                                mods (. meth (getModifiers))
                                mk (method-sig meth)]
                            (if (or (considered mk)
                                    (not (or (Modifier/isPublic mods) (Modifier/isProtected mods)))
                                    ;; (. Modifier (isPrivate mods))
                                    (. Modifier (isStatic mods))
                                    (. Modifier (isFinal mods))
                                    (= "finalize" (.getName meth)))
                              (recur mm (conj considered mk) (next meths))
                              (recur (assoc mm mk meth) (conj considered mk) (next meths))))
                          [mm considered]))]
                  (recur mm considered (. c (getSuperclass))))
                [mm considered]))
          ifaces-meths (into1 {}
                         (for [^Class iface interfaces meth (. iface (getMethods))
                               :let [msig (method-sig meth)] :when (not (considered msig))]
                           {msig meth}))
          ;; Treat abstract methods as interface methods
          [mm ifaces-meths] (let [abstract? (fn [[_ ^Method meth]]
                                              (Modifier/isAbstract (. meth (getModifiers))))
                                  mm-no-abstract (remove abstract? mm)
                                  abstract-meths (filter abstract? mm)]
                              [mm-no-abstract (concat ifaces-meths abstract-meths)])
          mgroups (group-by-sig (concat mm ifaces-meths))
          rtypes (map #(most-specific (keys %)) mgroups)
          mb (map #(vector (%1 %2) (vals (dissoc %1 %2))) mgroups rtypes)
          bridge? (reduce1 into1 #{} (map second mb))
          ifaces-meths (remove bridge? (vals ifaces-meths))
          mm (remove bridge? (vals mm))]
                                        ;; add methods matching supers', if no mapping -> call super
      (doseq [[^java.lang.reflect.Method dest bridges] mb
              ^java.lang.reflect.Method meth bridges]
          (gen-bridge meth dest))
      (doseq [^java.lang.reflect.Method meth mm]
          (gen-method meth
                      (fn [^GeneratorAdapter gen ^Method m]
                          (. gen (loadThis))
                                        ;; push args
                        (. gen (loadArgs))
                                        ;; call super
                        (. gen (visitMethodInsn (. Opcodes INVOKESPECIAL)
                                                (. super-type (getInternalName))
                                                (. m (getName))
                                                (. m (getDescriptor)))))))

                                        ;; add methods matching interfaces', if no mapping -> throw
      (doseq [^java.lang.reflect.Method meth ifaces-meths]
                (gen-method meth
                            (fn [^GeneratorAdapter gen ^Method m]
                                (. gen (throwException ex-type (. m (getName))))))))

                                        ;; finish class def
    (. cv (visitEnd))
    [cname (. cv toByteArray)]))

(§ defn- get-super-and-interfaces [bases]
  (if (. ^Class (first bases) (isInterface))
    [Object bases]
    [(first bases) (next bases)]))

(§ defn get-proxy-class
  "Takes an optional single class followed by zero or more
  interfaces. If not supplied class defaults to Object.  Creates an
  returns an instance of a proxy class derived from the supplied
  classes. The resulting value is cached and used for any subsequent
  requests for the same class set. Returns a Class object."
  {:added "1.0"}
  [& bases]
    (let [[super interfaces] (get-super-and-interfaces bases)
          pname (proxy-name super interfaces)]
      (or (RT/loadClassForName pname)
          (let [[cname bytecode] (generate-proxy super interfaces)]
            (. ^DynamicClassLoader (deref cloiure.lang.Compiler/LOADER) (defineClass pname bytecode [super interfaces]))))))

(§ defn construct-proxy
  "Takes a proxy class and any arguments for its superclass ctor and
  creates and returns an instance of the proxy."
  {:added "1.0"}
  [c & ctor-args]
    (. Reflector (invokeConstructor c (to-array ctor-args))))

(§ defn init-proxy
  "Takes a proxy instance and a map of strings (which must
  correspond to methods of the proxy superclass/superinterfaces) to
  fns (which must take arguments matching the corresponding method,
  plus an additional (explicit) first arg corresponding to this, and
  sets the proxy's fn map.  Returns the proxy."
  {:added "1.0"}
  [^IProxy proxy mappings]
    (. proxy (__initCloiureFnMappings mappings))
    proxy)

(§ defn update-proxy
  "Takes a proxy instance and a map of strings (which must
  correspond to methods of the proxy superclass/superinterfaces) to
  fns (which must take arguments matching the corresponding method,
  plus an additional (explicit) first arg corresponding to this, and
  updates (via assoc) the proxy's fn map. nil can be passed instead of
  a fn, in which case the corresponding method will revert to the
  default behavior. Note that this function can be used to update the
  behavior of an existing instance without changing its identity.
  Returns the proxy."
  {:added "1.0"}
  [^IProxy proxy mappings]
    (. proxy (__updateCloiureFnMappings mappings))
    proxy)

(§ defn proxy-mappings
  "Takes a proxy instance and returns the proxy's fn map."
  {:added "1.0"}
  [^IProxy proxy]
    (. proxy (__getCloiureFnMappings)))

(§ defmacro proxy
  "class-and-interfaces - a vector of class names

  args - a (possibly empty) vector of arguments to the superclass
  constructor.

  f => (name [params*] body) or
  (name ([params*] body) ([params+] body) ...)

  Expands to code which creates a instance of a proxy class that
  implements the named class/interface(s) by calling the supplied
  fns. A single class, if provided, must be first. If not provided it
  defaults to Object.

  The interfaces names must be valid interface types. If a method fn
  is not provided for a class method, the superclass methd will be
  called. If a method fn is not provided for an interface method, an
  UnsupportedOperationException will be thrown should it be
  called. Method fns are closures and can capture the environment in
  which proxy is called. Each method fn takes an additional implicit
  first arg, which is bound to 'this. Note that while method fns can
  be provided to override protected methods, they have no other access
  to protected members, nor to super, as these capabilities cannot be
  proxied."
  {:added "1.0"}
  [class-and-interfaces args & fs]
   (let [bases (map #(or (resolve %) (throw (Exception. (str "Can't resolve: " %))))
                    class-and-interfaces)
         [super interfaces] (get-super-and-interfaces bases)
         compile-effect (when *compile-files*
                          (let [[cname bytecode] (generate-proxy super interfaces)]
                            (cloiure.lang.Compiler/writeClassFile cname bytecode)))
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
                    meths (if (vector? (first meths))
                            (list meths)
                            meths)
                    meths (map (fn [[params & body]]
                                   (cons (apply vector 'this params) body))
                               meths)]
                (if-not (contains? fmap (name sym))
                (recur (assoc fmap (name sym) (cons `fn meths)) (next fs))
                           (throw (IllegalArgumentException.
                                      (str "Method '" (name sym) "' redefined")))))
              fmap)))
        p#)))

(§ defn proxy-call-with-super [call this meth]
 (let [m (proxy-mappings this)]
    (update-proxy this (assoc m meth nil))
    (try
      (call)
      (finally (update-proxy this m)))))

(§ defmacro proxy-super
  "Use to call a superclass method in the body of a proxy method.
  Note, expansion captures 'this"
  {:added "1.0"}
  [meth & args]
 `(proxy-call-with-super (fn [] (. ~'this ~meth ~@args))  ~'this ~(name meth)))

#_(ns cloiure.core.protocols)

(§ set! *warn-on-reflection* true)

(§ defprotocol CollReduce
  "Protocol for collection types that can implement reduce faster than
  first/next recursion. Called by cloiure.core/reduce. Baseline
  implementation defined in terms of Iterable."
  (coll-reduce [coll f] [coll f val]))

(§ defprotocol InternalReduce
  "Protocol for concrete seq types that can reduce themselves
   faster than first/next recursion. Called by cloiure.core/reduce."
  (internal-reduce [seq f start]))

(§ defn- seq-reduce
  ([coll f]
     (if-let [s (seq coll)]
       (internal-reduce (next s) f (first s))
       (f)))
  ([coll f val]
     (let [s (seq coll)]
       (internal-reduce s f val))))

(§ defn- iter-reduce
  ([^java.lang.Iterable coll f]
   (let [iter (.iterator coll)]
     (if (.hasNext iter)
       (loop [ret (.next iter)]
         (if (.hasNext iter)
           (let [ret (f ret (.next iter))]
             (if (reduced? ret)
               @ret
               (recur ret)))
           ret))
       (f))))
  ([^java.lang.Iterable coll f val]
   (let [iter (.iterator coll)]
     (loop [ret val]
       (if (.hasNext iter)
         (let [ret (f ret (.next iter))]
           (if (reduced? ret)
             @ret
             (recur ret)))
         ret)))))

(§ defn- naive-seq-reduce
  "Reduces a seq, ignoring any opportunities to switch to a more
  specialized implementation."
  [s f val]
  (loop [s (seq s)
         val val]
    (if s
      (let [ret (f val (first s))]
        (if (reduced? ret)
          @ret
          (recur (next s) ret)))
      val)))

(§ defn- interface-or-naive-reduce
  "Reduces via IReduceInit if possible, else naively."
  [coll f val]
  (if (instance? cloiure.lang.IReduceInit coll)
    (.reduce ^cloiure.lang.IReduceInit coll f val)
    (naive-seq-reduce coll f val)))

(§ extend-protocol CollReduce
  nil
  (coll-reduce
   ([coll f] (f))
   ([coll f val] val))

  Object
  (coll-reduce
   ([coll f] (seq-reduce coll f))
   ([coll f val] (seq-reduce coll f val)))

  cloiure.lang.IReduceInit
  (coll-reduce
    ([coll f] (.reduce ^cloiure.lang.IReduce coll f))
    ([coll f val] (.reduce coll f val)))

  ;; aseqs are iterable, masking internal-reducers
  cloiure.lang.ASeq
  (coll-reduce
   ([coll f] (seq-reduce coll f))
   ([coll f val] (seq-reduce coll f val)))

  ;; for range
  cloiure.lang.LazySeq
  (coll-reduce
   ([coll f] (seq-reduce coll f))
   ([coll f val] (seq-reduce coll f val)))

  ;; vector's chunked seq is faster than its iter
  cloiure.lang.PersistentVector
  (coll-reduce
   ([coll f] (seq-reduce coll f))
   ([coll f val] (seq-reduce coll f val)))

  Iterable
  (coll-reduce
   ([coll f] (iter-reduce coll f))
   ([coll f val] (iter-reduce coll f val)))

  cloiure.lang.APersistentMap$KeySeq
  (coll-reduce
    ([coll f] (iter-reduce coll f))
    ([coll f val] (iter-reduce coll f val)))

  cloiure.lang.APersistentMap$ValSeq
  (coll-reduce
    ([coll f] (iter-reduce coll f))
    ([coll f val] (iter-reduce coll f val))))

(§ extend-protocol InternalReduce
  nil
  (internal-reduce
   [s f val]
   val)

  ;; handles vectors and ranges
  cloiure.lang.IChunkedSeq
  (internal-reduce
   [s f val]
   (if-let [s (seq s)]
     (if (chunked-seq? s)
       (let [ret (.reduce (chunk-first s) f val)]
         (if (reduced? ret)
           @ret
           (recur (chunk-next s)
                  f
                  ret)))
       (interface-or-naive-reduce s f val))
     val))

  cloiure.lang.StringSeq
  (internal-reduce
   [str-seq f val]
   (let [s (.s str-seq)
         len (.length s)]
     (loop [i (.i str-seq)
            val val]
       (if (< i len)
         (let [ret (f val (.charAt s i))]
                (if (reduced? ret)
                  @ret
                  (recur (inc i) ret)))
         val))))

  java.lang.Object
  (internal-reduce
   [s f val]
   (loop [cls (class s)
          s s
          f f
          val val]
     (if-let [s (seq s)]
       (if (identical? (class s) cls)
         (let [ret (f val (first s))]
                (if (reduced? ret)
                  @ret
                  (recur cls (next s) f ret)))
         (interface-or-naive-reduce s f val))
       val))))

(§ defprotocol IKVReduce
  "Protocol for concrete associative types that can reduce themselves
   via a function of key and val faster than first/next recursion over map
   entries. Called by cloiure.core/reduce-kv, and has same
   semantics (just different arg order)."
  (kv-reduce [amap f init]))

#_(ns ^{:doc
      "A library for reduction and parallel folding. Alpha and subject
      to change.  Note that fold and its derivatives require Java 7+ or
      Java 6 + jsr166y.jar for fork/join support. See Cloiure's pom.xml for the
      dependency info."
      :author "Rich Hickey"}
  cloiure.core.reducers
  (:refer-cloiure :exclude [reduce map mapcat filter remove take take-while drop flatten cat])
  (:require [cloiure.walk :as walk]))

(§ alias 'core 'cloiure.core)
(§ set! *warn-on-reflection* true)

;;;;;;;;;;;;;; some fj stuff ;;;;;;;;;;

(§ defmacro ^:private compile-if
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to
  `then`.  Else expand to `else`.

  (compile-if (Class/forName \"java.util.concurrent.ForkJoinTask\")
    (do-cool-stuff-with-fork-join)
    (fall-back-to-executor-services))"
  [exp then else]
  (if (try (eval exp)
           (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))

(§ compile-if
 (Class/forName "java.util.concurrent.ForkJoinTask")
 ;; We're running a JDK 7+
 (do
   (def pool (delay (java.util.concurrent.ForkJoinPool.)))

   (defn fjtask [^Callable f]
     (java.util.concurrent.ForkJoinTask/adapt f))

   (defn- fjinvoke [f]
     (if (java.util.concurrent.ForkJoinTask/inForkJoinPool)
       (f)
       (.invoke ^java.util.concurrent.ForkJoinPool @pool ^java.util.concurrent.ForkJoinTask (fjtask f))))

   (defn- fjfork [task] (.fork ^java.util.concurrent.ForkJoinTask task))

   (defn- fjjoin [task] (.join ^java.util.concurrent.ForkJoinTask task)))
 ;; We're running a JDK <7
 (do
   (def pool (delay (jsr166y.ForkJoinPool.)))

   (defn fjtask [^Callable f]
     (jsr166y.ForkJoinTask/adapt f))

   (defn- fjinvoke [f]
     (if (jsr166y.ForkJoinTask/inForkJoinPool)
       (f)
       (.invoke ^jsr166y.ForkJoinPool @pool ^jsr166y.ForkJoinTask (fjtask f))))

   (defn- fjfork [task] (.fork ^jsr166y.ForkJoinTask task))

   (defn- fjjoin [task] (.join ^jsr166y.ForkJoinTask task))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ defn reduce
  "Like core/reduce except:
     When init is not provided, (f) is used.
     Maps are reduced with reduce-kv"
  ([f coll] (reduce f (f) coll))
  ([f init coll]
     (if (instance? java.util.Map coll)
       (cloiure.core.protocols/kv-reduce coll f init)
       (cloiure.core.protocols/coll-reduce coll f init))))

(§ defprotocol CollFold
  (coll-fold [coll n combinef reducef]))

(§ defn fold
  "Reduces a collection using a (potentially parallel) reduce-combine
  strategy. The collection is partitioned into groups of approximately
  n (default 512), each of which is reduced with reducef (with a seed
  value obtained by calling (combinef) with no arguments). The results
  of these reductions are then reduced with combinef (default
  reducef). combinef must be associative, and, when called with no
  arguments, (combinef) must produce its identity element. These
  operations may be performed in parallel, but the results will
  preserve order."
  {:added "1.5"}
  ([reducef coll] (fold reducef reducef coll))
  ([combinef reducef coll] (fold 512 combinef reducef coll))
  ([n combinef reducef coll]
     (coll-fold coll n combinef reducef)))

(§ defn reducer
  "Given a reducible collection, and a transformation function xf,
  returns a reducible collection, where any supplied reducing
  fn will be transformed by xf. xf is a function of reducing fn to
  reducing fn."
  {:added "1.5"}
  ([coll xf]
     (reify
      cloiure.core.protocols/CollReduce
      (coll-reduce [this f1]
                   (cloiure.core.protocols/coll-reduce this f1 (f1)))
      (coll-reduce [_ f1 init]
                   (cloiure.core.protocols/coll-reduce coll (xf f1) init)))))

(§ defn folder
  "Given a foldable collection, and a transformation function xf,
  returns a foldable collection, where any supplied reducing
  fn will be transformed by xf. xf is a function of reducing fn to
  reducing fn."
  {:added "1.5"}
  ([coll xf]
     (reify
      cloiure.core.protocols/CollReduce
      (coll-reduce [_ f1]
                   (cloiure.core.protocols/coll-reduce coll (xf f1) (f1)))
      (coll-reduce [_ f1 init]
                   (cloiure.core.protocols/coll-reduce coll (xf f1) init))

      CollFold
      (coll-fold [_ n combinef reducef]
                 (coll-fold coll n combinef (xf reducef))))))

(§ defn- do-curried
  [name doc meta args body]
  (let [cargs (vec (butlast args))]
    `(defn ~name ~doc ~meta
       (~cargs (fn [x#] (~name ~@cargs x#)))
       (~args ~@body))))

(§ defmacro ^:private defcurried
  "Builds another arity of the fn that returns a fn awaiting the last
  param"
  [name doc meta args & body]
  (do-curried name doc meta args body))

(§ defn- do-rfn [f1 k fkv]
  `(fn
     ([] (~f1))
     ~(cloiure.walk/postwalk
       #(if (sequential? %)
          ((if (vector? %) vec identity)
           (core/remove #{k} %))
          %)
       fkv)
     ~fkv))

(§ defmacro ^:private rfn
  "Builds 3-arity reducing fn given names of wrapped fn and key, and k/v impl."
  [[f1 k] fkv]
  (do-rfn f1 k fkv))

(§ defcurried map
  "Applies f to every value in the reduction of coll. Foldable."
  {:added "1.5"}
  [f coll]
  (folder coll
   (fn [f1]
     (rfn [f1 k]
          ([ret k v]
             (f1 ret (f k v)))))))

(§ defcurried mapcat
  "Applies f to every value in the reduction of coll, concatenating the result
  colls of (f val). Foldable."
  {:added "1.5"}
  [f coll]
  (folder coll
   (fn [f1]
     (let [f1 (fn
                ([ret v]
                  (let [x (f1 ret v)] (if (reduced? x) (reduced x) x)))
                ([ret k v]
                  (let [x (f1 ret k v)] (if (reduced? x) (reduced x) x))))]
       (rfn [f1 k]
            ([ret k v]
               (reduce f1 ret (f k v))))))))

(§ defcurried filter
  "Retains values in the reduction of coll for which (pred val)
  returns logical true. Foldable."
  {:added "1.5"}
  [pred coll]
  (folder coll
   (fn [f1]
     (rfn [f1 k]
          ([ret k v]
             (if (pred k v)
               (f1 ret k v)
               ret))))))

(§ defcurried remove
  "Removes values in the reduction of coll for which (pred val)
  returns logical true. Foldable."
  {:added "1.5"}
  [pred coll]
  (filter (complement pred) coll))

(§ defcurried flatten
  "Takes any nested combination of sequential things (lists, vectors,
  etc.) and returns their contents as a single, flat foldable
  collection."
  {:added "1.5"}
  [coll]
  (folder coll
   (fn [f1]
     (fn
       ([] (f1))
       ([ret v]
          (if (sequential? v)
            (cloiure.core.protocols/coll-reduce (flatten v) f1 ret)
            (f1 ret v)))))))

(§ defcurried take-while
  "Ends the reduction of coll when (pred val) returns logical false."
  {:added "1.5"}
  [pred coll]
  (reducer coll
   (fn [f1]
     (rfn [f1 k]
          ([ret k v]
             (if (pred k v)
               (f1 ret k v)
               (reduced ret)))))))

(§ defcurried take
  "Ends the reduction of coll after consuming n values."
  {:added "1.5"}
  [n coll]
  (reducer coll
   (fn [f1]
     (let [cnt (atom n)]
       (rfn [f1 k]
         ([ret k v]
            (swap! cnt dec)
            (if (neg? @cnt)
              (reduced ret)
              (f1 ret k v))))))))

(§ defcurried drop
  "Elides the first n values from the reduction of coll."
  {:added "1.5"}
  [n coll]
  (reducer coll
   (fn [f1]
     (let [cnt (atom n)]
       (rfn [f1 k]
         ([ret k v]
            (swap! cnt dec)
            (if (neg? @cnt)
              (f1 ret k v)
              ret)))))))

;; do not construct this directly, use cat
(§ deftype Cat [cnt left right]
  cloiure.lang.Counted
  (count [_] cnt)

  cloiure.lang.Seqable
  (seq [_] (concat (seq left) (seq right)))

  cloiure.core.protocols/CollReduce
  (coll-reduce [this f1] (cloiure.core.protocols/coll-reduce this f1 (f1)))
  (coll-reduce
   [_  f1 init]
   (cloiure.core.protocols/coll-reduce
    right f1
    (cloiure.core.protocols/coll-reduce left f1 init)))

  CollFold
  (coll-fold
   [_ n combinef reducef]
   (fjinvoke
    (fn []
      (let [rt (fjfork (fjtask #(coll-fold right n combinef reducef)))]
        (combinef
         (coll-fold left n combinef reducef)
         (fjjoin rt)))))))

(§ defn cat
  "A high-performance combining fn that yields the catenation of the
  reduced values. The result is reducible, foldable, seqable and
  counted, providing the identity collections are reducible, seqable
  and counted. The single argument version will build a combining fn
  with the supplied identity constructor. Tests for identity
  with (zero? (count x)). See also foldcat."
  {:added "1.5"}
  ([] (java.util.ArrayList.))
  ([ctor]
     (fn
       ([] (ctor))
       ([left right] (cat left right))))
  ([left right]
     (cond
      (zero? (count left)) right
      (zero? (count right)) left
      :else
      (Cat. (+ (count left) (count right)) left right))))

(§ defn append!
  ".adds x to acc and returns acc"
  {:added "1.5"}
  [^java.util.Collection acc x]
  (doto acc (.add x)))

(§ defn foldcat
  "Equivalent to (fold cat append! coll)"
  {:added "1.5"}
  [coll]
  (fold cat append! coll))

(§ defn monoid
  "Builds a combining fn out of the supplied operator and identity
  constructor. op must be associative and ctor called with no args
  must return an identity value for it."
  {:added "1.5"}
  [op ctor]
  (fn m
    ([] (ctor))
    ([a b] (op a b))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; fold impls ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(§ defn- foldvec
  [v n combinef reducef]
  (cond
   (empty? v) (combinef)
   (<= (count v) n) (reduce reducef (combinef) v)
   :else
   (let [split (quot (count v) 2)
         v1 (subvec v 0 split)
         v2 (subvec v split (count v))
         fc (fn [child] #(foldvec child n combinef reducef))]
     (fjinvoke
      #(let [f1 (fc v1)
             t2 (fjtask (fc v2))]
         (fjfork t2)
         (combinef (f1) (fjjoin t2)))))))

(§ extend-protocol CollFold
 nil
 (coll-fold
  [coll n combinef reducef]
  (combinef))

 Object
 (coll-fold
  [coll n combinef reducef]
  ;; can't fold, single reduce
  (reduce reducef (combinef) coll))

 cloiure.lang.IPersistentVector
 (coll-fold
  [v n combinef reducef]
  (foldvec v n combinef reducef))

 cloiure.lang.PersistentHashMap
 (coll-fold
  [m n combinef reducef]
  (.fold m n combinef reducef fjinvoke fjtask fjfork fjjoin)))

#_(ns ^{:doc "Socket server support"
      :author "Alex Miller"}
  cloiure.core.server
  (:require [cloiure.string :as str]
            [cloiure.edn :as edn]
            [cloiure.main :as m])
  (:import [java.net InetAddress Socket ServerSocket SocketException]
           [java.util.concurrent.locks ReentrantLock]))

(§ set! *warn-on-reflection* true)

(§ def ^:dynamic *session* nil)

;; lock protects servers
(§ defonce ^:private lock (ReentrantLock.))
(§ defonce ^:private servers {})

(§ defmacro ^:private with-lock
  [lock-expr & body]
  `(let [lockee# ~(with-meta lock-expr {:tag 'java.util.concurrent.locks.ReentrantLock})]
     (.lock lockee#)
     (try
       ~@body
       (finally
         (.unlock lockee#)))))

(§ defmacro ^:private thread
  [^String name daemon & body]
  `(doto (Thread. (fn [] ~@body) ~name)
    (.setDaemon ~daemon)
    (.start)))

(§ defn- required
  "Throw if opts does not contain prop."
  [opts prop]
  (when (nil? (get opts prop))
    (throw (ex-info (str "Missing required socket server property " prop) opts))))

(§ defn- validate-opts
  "Validate server config options"
  [{:keys [name port accept] :as opts}]
  (doseq [prop [:name :port :accept]] (required opts prop))
  (when (or (not (integer? port)) (not (< -1 port 65535)))
    (throw (ex-info (str "Invalid socket server port: " port) opts))))

(§ defn- accept-connection
  "Start accept function, to be invoked on a client thread, given:
    conn - client socket
    name - server name
    client-id - client identifier
    in - in stream
    out - out stream
    err - err stream
    accept - accept fn symbol to invoke
    args - to pass to accept-fn"
  [^Socket conn name client-id in out err accept args]
  (try
    (binding [*in* in
              *out* out
              *err* err
              *session* {:server name :client client-id}]
      (with-lock lock
        (alter-var-root #'servers assoc-in [name :sessions client-id] {}))
      (require (symbol (namespace accept)))
      (let [accept-fn (resolve accept)]
        (apply accept-fn args)))
    (catch SocketException _disconnect)
    (finally
      (with-lock lock
        (alter-var-root #'servers update-in [name :sessions] dissoc client-id))
      (.close conn))))

(§ defn start-server
  "Start a socket server given the specified opts:
    :address Host or address, string, defaults to loopback address
    :port Port, integer, required
    :name Name, required
    :accept Namespaced symbol of the accept function to invoke, required
    :args Vector of args to pass to accept function
    :bind-err Bind *err* to socket out stream?, defaults to true
    :server-daemon Is server thread a daemon?, defaults to true
    :client-daemon Are client threads daemons?, defaults to true
   Returns server socket."
  [opts]
  (validate-opts opts)
  (let [{:keys [address port name accept args bind-err server-daemon client-daemon]
         :or {bind-err true
              server-daemon true
              client-daemon true}} opts
         address (InetAddress/getByName address)  ;; nil returns loopback
         socket (ServerSocket. port 0 address)]
    (with-lock lock
      (alter-var-root #'servers assoc name {:name name, :socket socket, :sessions {}}))
    (thread
      (str "Cloiure Server " name) server-daemon
      (try
        (loop [client-counter 1]
          (when (not (.isClosed socket))
            (try
              (let [conn (.accept socket)
                    in (cloiure.lang.LineNumberingPushbackReader. (java.io.InputStreamReader. (.getInputStream conn)))
                    out (java.io.BufferedWriter. (java.io.OutputStreamWriter. (.getOutputStream conn)))
                    client-id (str client-counter)]
                (thread
                  (str "Cloiure Connection " name " " client-id) client-daemon
                  (accept-connection conn name client-id in out (if bind-err out *err*) accept args)))
              (catch SocketException _disconnect))
            (recur (inc client-counter))))
        (finally
          (with-lock lock
            (alter-var-root #'servers dissoc name)))))
    socket))

(§ defn stop-server
  "Stop server with name or use the server-name from *session* if none supplied.
  Returns true if server stopped successfully, nil if not found, or throws if
  there is an error closing the socket."
  ([]
   (stop-server (:server *session*)))
  ([name]
   (with-lock lock
     (let [server-socket ^ServerSocket (get-in servers [name :socket])]
       (when server-socket
         (alter-var-root #'servers dissoc name)
         (.close server-socket)
         true)))))

(§ defn stop-servers
  "Stop all servers ignores all errors, and returns nil."
  []
  (with-lock lock
    (doseq [name (keys servers)]
      (future (stop-server name)))))

(§ defn- parse-props
  "Parse cloiure.server.* from properties to produce a map of server configs."
  [props]
  (reduce
    (fn [acc [^String k ^String v]]
      (let [[k1 k2 k3] (str/split k #"\.")]
        (if (and (= k1 "cloiure") (= k2 "server"))
          (conj acc (merge {:name k3} (edn/read-string v)))
          acc)))
    [] props))

(§ defn start-servers
  "Start all servers specified in the system properties."
  [system-props]
  (doseq [server (parse-props system-props)]
    (start-server server)))

(§ defn repl-init
  "Initialize repl in user namespace and make standard repl requires."
  []
  (in-ns 'user)
  (apply require cloiure.main/repl-requires))

(§ defn repl-read
  "Enhanced :read hook for repl supporting :repl/quit."
  [request-prompt request-exit]
  (or ({:line-start request-prompt :stream-end request-exit}
        (m/skip-whitespace *in*))
      (let [input (read {:read-cond :allow} *in*)]
        (m/skip-if-eol *in*)
        (case input
          :repl/quit request-exit
          input))))

(§ defn repl
  "REPL with predefined hooks for attachable socket server."
  []
  (m/repl
    :init repl-init
    :read repl-read))

#_(ns ^{:skip-wiki true} cloiure.core.specs.alpha
  (:require [cloiure.spec.alpha :as s]))

;;;; destructure

(§ s/def ::local-name (s/and simple-symbol? #(not= '& %)))

(§ s/def ::binding-form
  (s/or :sym ::local-name
        :seq ::seq-binding-form
        :map ::map-binding-form))

;; sequential destructuring

(§ s/def ::seq-binding-form
  (s/and vector?
         (s/cat :elems (s/* ::binding-form)
                :rest (s/? (s/cat :amp #{'&} :form ::binding-form))
                :as (s/? (s/cat :as #{:as} :sym ::local-name)))))

;; map destructuring

(§ s/def ::keys (s/coll-of ident? :kind vector?))
(§ s/def ::syms (s/coll-of symbol? :kind vector?))
(§ s/def ::strs (s/coll-of simple-symbol? :kind vector?))
(§ s/def ::or (s/map-of simple-symbol? any?))
(§ s/def ::as ::local-name)

(§ s/def ::map-special-binding
  (s/keys :opt-un [::as ::or ::keys ::syms ::strs]))

(§ s/def ::map-binding (s/tuple ::binding-form any?))

(§ s/def ::ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (s/coll-of simple-symbol? :kind vector?)))

(§ s/def ::map-bindings
  (s/every (s/or :mb ::map-binding
                 :nsk ::ns-keys
                 :msb (s/tuple #{:as :or :keys :syms :strs} any?)) :into {}))

(§ s/def ::map-binding-form (s/merge ::map-bindings ::map-special-binding))

;; bindings

(§ s/def ::binding (s/cat :binding ::binding-form :init-expr any?))
(§ s/def ::bindings (s/and vector? (s/* ::binding)))

;; let, if-let, when-let

(§ s/fdef cloiure.core/let
  :args (s/cat :bindings ::bindings
               :body (s/* any?)))

(§ s/fdef cloiure.core/if-let
  :args (s/cat :bindings (s/and vector? ::binding)
               :then any?
               :else (s/? any?)))

(§ s/fdef cloiure.core/when-let
  :args (s/cat :bindings (s/and vector? ::binding)
               :body (s/* any?)))

;; defn, defn-, fn

(§ s/def ::arg-list
  (s/and
    vector?
    (s/cat :args (s/* ::binding-form)
           :varargs (s/? (s/cat :amp #{'&} :form ::binding-form)))))

(§ s/def ::args+body
  (s/cat :args ::arg-list
         :body (s/alt :prepost+body (s/cat :prepost map?
                                           :body (s/+ any?))
                      :body (s/* any?))))

(§ s/def ::defn-args
  (s/cat :name simple-symbol?
         :docstring (s/? string?)
         :meta (s/? map?)
         :bs (s/alt :arity-1 ::args+body
                    :arity-n (s/cat :bodies (s/+ (s/spec ::args+body))
                                    :attr (s/? map?)))))

(§ s/fdef cloiure.core/defn
  :args ::defn-args
  :ret any?)

(§ s/fdef cloiure.core/defn-
  :args ::defn-args
  :ret any?)

(§ s/fdef cloiure.core/fn
  :args (s/cat :name (s/? simple-symbol?)
               :bs (s/alt :arity-1 ::args+body
                          :arity-n (s/+ (s/spec ::args+body))))
  :ret any?)

;;;; ns

(§ s/def ::exclude (s/coll-of simple-symbol?))
(§ s/def ::only (s/coll-of simple-symbol?))
(§ s/def ::rename (s/map-of simple-symbol? simple-symbol?))
(§ s/def ::filters (s/keys* :opt-un [::exclude ::only ::rename]))

(§ s/def ::ns-refer-cloiure
  (s/spec (s/cat :clause #{:refer-cloiure}
                 :filters ::filters)))

(§ s/def ::refer (s/or :all #{:all}
                     :syms (s/coll-of simple-symbol?)))

(§ s/def ::prefix-list
  (s/spec
    (s/cat :prefix simple-symbol?
           :libspecs (s/+ ::libspec))))

(§ s/def ::libspec
  (s/alt :lib simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [::as ::refer])))))

(§ s/def ::ns-require
  (s/spec (s/cat :clause #{:require}
                 :body (s/+ (s/alt :libspec ::libspec
                                   :prefix-list ::prefix-list
                                   :flag #{:reload :reload-all :verbose})))))

(§ s/def ::package-list
  (s/spec
    (s/cat :package simple-symbol?
           :classes (s/* simple-symbol?))))

(§ s/def ::import-list
  (s/* (s/alt :class simple-symbol?
              :package-list ::package-list)))

(§ s/def ::ns-import
  (s/spec
    (s/cat :clause #{:import}
           :classes ::import-list)))

(§ s/def ::ns-refer
  (s/spec (s/cat :clause #{:refer}
                 :lib simple-symbol?
                 :filters ::filters)))

;; same as ::prefix-list, but with ::use-libspec instead
(§ s/def ::use-prefix-list
  (s/spec
    (s/cat :prefix simple-symbol?
           :libspecs (s/+ ::use-libspec))))

;; same as ::libspec, but also supports the ::filters options in the libspec
(§ s/def ::use-libspec
  (s/alt :lib simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [::as ::refer ::exclude ::only ::rename])))))

(§ s/def ::ns-use
  (s/spec (s/cat :clause #{:use}
                 :libs (s/+ (s/alt :libspec ::use-libspec
                                   :prefix-list ::use-prefix-list
                                   :flag #{:reload :reload-all :verbose})))))

(§ s/def ::ns-load
  (s/spec (s/cat :clause #{:load}
                 :libs (s/* string?))))

(§ s/def ::name simple-symbol?)
(§ s/def ::extends simple-symbol?)
(§ s/def ::implements (s/coll-of simple-symbol? :kind vector?))
(§ s/def ::init symbol?)
(§ s/def ::class-ident (s/or :class simple-symbol? :class-name string?))
(§ s/def ::signature (s/coll-of ::class-ident :kind vector?))
(§ s/def ::constructors (s/map-of ::signature ::signature))
(§ s/def ::post-init symbol?)
(§ s/def ::method (s/and vector?
                  (s/cat :name simple-symbol?
                         :param-types ::signature
                         :return-type simple-symbol?)))
(§ s/def ::methods (s/coll-of ::method :kind vector?))
(§ s/def ::main boolean?)
(§ s/def ::factory simple-symbol?)
(§ s/def ::state simple-symbol?)
(§ s/def ::get simple-symbol?)
(§ s/def ::set simple-symbol?)
(§ s/def ::expose (s/keys :opt-un [::get ::set]))
(§ s/def ::exposes (s/map-of simple-symbol? ::expose))
(§ s/def ::prefix string?)
(§ s/def ::impl-ns simple-symbol?)
(§ s/def ::load-impl-ns boolean?)

(§ s/def ::ns-gen-class
  (s/spec (s/cat :clause #{:gen-class}
                 :options (s/keys* :opt-un [::name ::extends ::implements
                                            ::init ::constructors ::post-init
                                            ::methods ::main ::factory ::state
                                            ::exposes ::prefix ::impl-ns ::load-impl-ns]))))

(§ s/def ::ns-clauses
  (s/* (s/alt :refer-cloiure ::ns-refer-cloiure
              :require ::ns-require
              :import ::ns-import
              :use ::ns-use
              :refer ::ns-refer
              :load ::ns-load
              :gen-class ::ns-gen-class)))

(§ s/def ::ns-form
  (s/cat :name simple-symbol?
         :docstring (s/? string?)
         :attr-map (s/? map?)
         :clauses ::ns-clauses))

(§ s/fdef cloiure.core/ns
  :args ::ns-form)

(§ defmacro ^:private quotable
  "Returns a spec that accepts both the spec and a (quote ...) form of the spec"
  [spec]
  `(s/or :spec ~spec :quoted-spec (s/cat :quote #{'quote} :spec ~spec)))

(§ s/def ::quotable-import-list
  (s/* (s/alt :class (quotable simple-symbol?)
              :package-list (quotable ::package-list))))

(§ s/fdef cloiure.core/import
  :args ::quotable-import-list)

(§ s/fdef cloiure.core/refer-cloiure
  :args (s/* (s/alt
               :exclude (s/cat :op (quotable #{:exclude}) :arg (quotable ::exclude))
               :only (s/cat :op (quotable #{:only}) :arg (quotable ::only))
               :rename (s/cat :op (quotable #{:rename}) :arg (quotable ::rename)))))
(§ ns
  ^{:author "Stuart Halloway",
    :doc "Non-core data functions."}
  cloiure.data
  (:require [cloiure.set :as set]))

(§ declare diff)

(§ defn- atom-diff
  "Internal helper for diff."
  [a b]
  (if (= a b) [nil nil a] [a b nil]))

;; for big things a sparse vector class would be better
(§ defn- vectorize
  "Convert an associative-by-numeric-index collection into
   an equivalent vector, with nil for any missing keys"
  [m]
  (when (seq m)
    (reduce
     (fn [result [k v]] (assoc result k v))
     (vec (repeat (apply max (keys m))  nil))
     m)))

(§ defn- diff-associative-key
  "Diff associative things a and b, comparing only the key k."
  [a b k]
  (let [va (get a k)
        vb (get b k)
        [a* b* ab] (diff va vb)
        in-a (contains? a k)
        in-b (contains? b k)
        same (and in-a in-b
                  (or (not (nil? ab))
                      (and (nil? va) (nil? vb))))]
    [(when (and in-a (or (not (nil? a*)) (not same))) {k a*})
     (when (and in-b (or (not (nil? b*)) (not same))) {k b*})
     (when same {k ab})
     ]))

(§ defn- diff-associative
  "Diff associative things a and b, comparing only keys in ks."
  [a b ks]
  (reduce
   (fn [diff1 diff2]
     (doall (map merge diff1 diff2)))
   [nil nil nil]
   (map
    (partial diff-associative-key a b)
    ks)))

(§ defn- diff-sequential
  [a b]
  (vec (map vectorize (diff-associative
                       (if (vector? a) a (vec a))
                       (if (vector? b) b (vec b))
                       (range (max (count a) (count b)))))))

(§ defprotocol ^{:added "1.3"} EqualityPartition
  "Implementation detail. Subject to change."
  (^{:added "1.3"} equality-partition [x] "Implementation detail. Subject to change."))

(§ defprotocol ^{:added "1.3"} Diff
  "Implementation detail. Subject to change."
  (^{:added "1.3"} diff-similar [a b] "Implementation detail. Subject to change."))

(§ extend nil
        Diff
        {:diff-similar atom-diff})

(§ extend Object
        Diff
        {:diff-similar (fn [a b] ((if (.. a getClass isArray) diff-sequential atom-diff) a b))}
        EqualityPartition
        {:equality-partition (fn [x] (if (.. x getClass isArray) :sequential :atom))})

(§ extend-protocol EqualityPartition
  nil
  (equality-partition [x] :atom)

  java.util.Set
  (equality-partition [x] :set)

  java.util.List
  (equality-partition [x] :sequential)

  java.util.Map
  (equality-partition [x] :map))

(§ defn- as-set-value
  [s]
  (if (set? s) s (into #{} s)))

(§ extend-protocol Diff
  java.util.Set
  (diff-similar
   [a b]
   (let [aval (as-set-value a)
         bval (as-set-value b)]
     [(not-empty (set/difference aval bval))
      (not-empty (set/difference bval aval))
      (not-empty (set/intersection aval bval))]))

  java.util.List
  (diff-similar [a b]
    (diff-sequential a b))

  java.util.Map
  (diff-similar [a b]
    (diff-associative a b (set/union (keys a) (keys b)))))

(§ defn diff
  "Recursively compares a and b, returning a tuple of
  [things-only-in-a things-only-in-b things-in-both].
  Comparison rules:

  * For equal a and b, return [nil nil a].
  * Maps are subdiffed where keys match and values differ.
  * Sets are never subdiffed.
  * All sequential things are treated as associative collections
    by their indexes, with results returned as vectors.
  * Everything else (including strings!) is treated as
    an atom and compared for equality."
  {:added "1.3"}
  [a b]
  (if (= a b)
    [nil nil a]
    (if (= (equality-partition a) (equality-partition b))
      (diff-similar a b)
      (atom-diff a b))))

#_(ns ^{:doc "edn reading."
      :author "Rich Hickey"}
  cloiure.edn
  (:refer-cloiure :exclude [read read-string]))

(§ defn read
  "Reads the next object from stream, which must be an instance of
  java.io.PushbackReader or some derivee.  stream defaults to the
  current value of *in*.

  Reads data in the edn format (subset of Cloiure data):
  http://edn-format.org

  opts is a map that can include the following keys:
  :eof - value to return on end-of-file. When not supplied, eof throws an exception.
  :readers  - a map of tag symbols to data-reader functions to be considered before default-data-readers.
              When not supplied, only the default-data-readers will be used.
  :default - A function of two args, that will, if present and no reader is found for a tag,
             be called with the tag and the value."

  {:added "1.5"}
  ([]
   (read *in*))
  ([stream]
   (read {} stream))
  ([opts stream]
     (cloiure.lang.EdnReader/read stream opts)))

(§ defn read-string
  "Reads one object from the string s. Returns nil when s is nil or empty.

  Reads data in the edn format (subset of Cloiure data):
  http://edn-format.org

  opts is a map as per cloiure.edn/read"
  {:added "1.5"}
  ([s] (read-string {:eof nil} s))
  ([opts s] (when s (cloiure.lang.EdnReader/readString s opts))))
(§ in-ns 'cloiure.core)

(§ import '(java.lang.reflect Modifier Constructor)
        '(cloiure.asm ClassWriter ClassVisitor Opcodes Type)
        '(cloiure.asm.commons Method GeneratorAdapter)
        '(cloiure.lang IPersistentMap))

;(defn method-sig [^java.lang.reflect.Method meth]
;  [(. meth (getName)) (seq (. meth (getParameterTypes)))])

(§ defn- filter-methods [^Class c invalid-method?]
  (loop [mm {}
         considered #{}
         c c]
    (if c
      (let [[mm considered]
            (loop [mm mm
                   considered considered
                   meths (seq (concat
                                (seq (. c (getDeclaredMethods)))
                                (seq (. c (getMethods)))))]
              (if meths
                (let [^java.lang.reflect.Method meth (first meths)
                      mods (. meth (getModifiers))
                      mk (method-sig meth)]
                  (if (or (considered mk)
                          (invalid-method? meth))
                    (recur mm (conj considered mk) (next meths))
                    (recur (assoc mm mk meth) (conj considered mk) (next meths))))
                [mm considered]))]
        (recur mm considered (. c (getSuperclass))))
      mm)))

(§ defn- non-private-methods [^Class c]
  (let [not-overridable? (fn [^java.lang.reflect.Method meth]
                           (let [mods (. meth (getModifiers))]
                             (or (not (or (Modifier/isPublic mods) (Modifier/isProtected mods)))
                                 (. Modifier (isStatic mods))
                                 (. Modifier (isFinal mods))
                                 (= "finalize" (.getName meth)))))]
    (filter-methods c not-overridable?)))

(§ defn- protected-final-methods [^Class c]
  (let [not-exposable? (fn [^java.lang.reflect.Method meth]
                         (let [mods (. meth (getModifiers))]
                           (not (and (Modifier/isProtected mods)
                                     (Modifier/isFinal mods)
                                     (not (Modifier/isStatic mods))))))]
    (filter-methods c not-exposable?)))

(§ defn- ctor-sigs [^Class super]
  (for [^Constructor ctor (. super (getDeclaredConstructors))
        :when (not (. Modifier (isPrivate (. ctor (getModifiers)))))]
    (apply vector (. ctor (getParameterTypes)))))

(§ defn- escape-class-name [^Class c]
  (.. (.getSimpleName c)
      (replace "[]" "<>")))

(§ defn- overload-name [mname pclasses]
  (if (seq pclasses)
    (apply str mname (interleave (repeat \-)
                                 (map escape-class-name pclasses)))
    (str mname "-void")))

(§ defn- ^java.lang.reflect.Field find-field [^Class c f]
  (let [start-class c]
    (loop [c c]
      (if (= c Object)
        (throw (new Exception (str "field, " f ", not defined in class, " start-class ", or its ancestors")))
        (let [dflds (.getDeclaredFields c)
              rfld (first (filter #(= f (.getName ^java.lang.reflect.Field %)) dflds))]
          (or rfld (recur (.getSuperclass c))))))))

; (distinct (map first(keys (mapcat non-private-methods [Object IPersistentMap]))))

(§ def ^{:private true} prim->class
     {'int Integer/TYPE
      'ints (Class/forName "[I")
      'long Long/TYPE
      'longs (Class/forName "[J")
      'float Float/TYPE
      'floats (Class/forName "[F")
      'double Double/TYPE
      'doubles (Class/forName "[D")
      'void Void/TYPE
      'short Short/TYPE
      'shorts (Class/forName "[S")
      'boolean Boolean/TYPE
      'booleans (Class/forName "[Z")
      'byte Byte/TYPE
      'bytes (Class/forName "[B")
      'char Character/TYPE
      'chars (Class/forName "[C")})

(§ defn- ^Class the-class [x]
  (cond
   (class? x) x
   (contains? prim->class x) (prim->class x)
   :else (let [strx (str x)]
           (cloiure.lang.RT/classForName
            (if (some #{\. \[} strx)
              strx
              (str "java.lang." strx))))))

;; someday this can be made codepoint aware
(§ defn- valid-java-method-name
  [^String s]
  (= s (cloiure.lang.Compiler/munge s)))

(§ defn- validate-generate-class-options
  [{:keys [methods]}]
  (let [[mname] (remove valid-java-method-name (map (comp str first) methods))]
    (when mname (throw (IllegalArgumentException. (str "Not a valid method name: " mname))))))

(§ defn- generate-class [options-map]
  (validate-generate-class-options options-map)
  (let [default-options {:prefix "-" :load-impl-ns true :impl-ns (ns-name *ns*)}
        {:keys [name extends implements constructors methods main factory state init exposes
                exposes-methods prefix load-impl-ns impl-ns post-init]}
          (merge default-options options-map)
        name-meta (meta name)
        name (str name)
        super (if extends (the-class extends) Object)
        interfaces (map the-class implements)
        supers (cons super interfaces)
        ctor-sig-map (or constructors (zipmap (ctor-sigs super) (ctor-sigs super)))
        cv (new ClassWriter (. ClassWriter COMPUTE_MAXS))
        cname (. name (replace "." "/"))
        pkg-name name
        impl-pkg-name (str impl-ns)
        impl-cname (.. impl-pkg-name (replace "." "/") (replace \- \_))
        ctype (. Type (getObjectType cname))
        iname (fn [^Class c] (.. Type (getType c) (getInternalName)))
        totype (fn [^Class c] (. Type (getType c)))
        to-types (fn [cs] (if (pos? (count cs))
                            (into-array (map totype cs))
                            (make-array Type 0)))
        obj-type ^Type (totype Object)
        arg-types (fn [n] (if (pos? n)
                            (into-array (replicate n obj-type))
                            (make-array Type 0)))
        super-type ^Type (totype super)
        init-name (str init)
        post-init-name (str post-init)
        factory-name (str factory)
        state-name (str state)
        main-name "main"
        var-name (fn [s] (cloiure.lang.Compiler/munge (str s "__var")))
        class-type  (totype Class)
        rt-type  (totype cloiure.lang.RT)
        var-type ^Type (totype cloiure.lang.Var)
        ifn-type (totype cloiure.lang.IFn)
        iseq-type (totype cloiure.lang.ISeq)
        ex-type  (totype java.lang.UnsupportedOperationException)
        util-type (totype cloiure.lang.Util)
        all-sigs (distinct (concat (map #(let[[m p] (key %)] {m [p]}) (mapcat non-private-methods supers))
                                   (map (fn [[m p]] {(str m) [p]}) methods)))
        sigs-by-name (apply merge-with concat {} all-sigs)
        overloads (into1 {} (filter (fn [[m s]] (next s)) sigs-by-name))
        var-fields (concat (when init [init-name])
                           (when post-init [post-init-name])
                           (when main [main-name])
                           ;; (when exposes-methods (map str (vals exposes-methods)))
                           (distinct (concat (keys sigs-by-name)
                                             (mapcat (fn [[m s]] (map #(overload-name m (map the-class %)) s)) overloads)
                                             (mapcat (comp (partial map str) vals val) exposes))))
        emit-get-var (fn [^GeneratorAdapter gen v]
                       (let [false-label (. gen newLabel)
                             end-label (. gen newLabel)]
                         (. gen getStatic ctype (var-name v) var-type)
                         (. gen dup)
                         (. gen invokeVirtual var-type (. Method (getMethod "boolean isBound()")))
                         (. gen ifZCmp (. GeneratorAdapter EQ) false-label)
                         (. gen invokeVirtual var-type (. Method (getMethod "Object get()")))
                         (. gen goTo end-label)
                         (. gen mark false-label)
                         (. gen pop)
                         (. gen visitInsn (. Opcodes ACONST_NULL))
                         (. gen mark end-label)))
        emit-unsupported (fn [^GeneratorAdapter gen ^Method m]
                           (. gen (throwException ex-type (str (. m (getName)) " ("
                                                               impl-pkg-name "/" prefix (.getName m)
                                                               " not defined?)"))))
        emit-forwarding-method
        (fn [name pclasses rclass as-static else-gen]
          (let [mname (str name)
                pmetas (map meta pclasses)
                pclasses (map the-class pclasses)
                rclass (the-class rclass)
                ptypes (to-types pclasses)
                rtype ^Type (totype rclass)
                m (new Method mname rtype ptypes)
                is-overload (seq (overloads mname))
                gen (new GeneratorAdapter (+ (. Opcodes ACC_PUBLIC) (if as-static (. Opcodes ACC_STATIC) 0))
                         m nil nil cv)
                found-label (. gen (newLabel))
                else-label (. gen (newLabel))
                end-label (. gen (newLabel))]
            (add-annotations gen (meta name))
            (dotimes [i (count pmetas)]
              (add-annotations gen (nth pmetas i) i))
            (. gen (visitCode))
            (if (> (count pclasses) 18)
              (else-gen gen m)
              (do
                (when is-overload
                  (emit-get-var gen (overload-name mname pclasses))
                  (. gen (dup))
                  (. gen (ifNonNull found-label))
                  (. gen (pop)))
                (emit-get-var gen mname)
                (. gen (dup))
                (. gen (ifNull else-label))
                (when is-overload
                  (. gen (mark found-label)))
                                        ;; if found
                (.checkCast gen ifn-type)
                (when-not as-static
                  (. gen (loadThis)))
                                        ;; box args
                (dotimes [i (count ptypes)]
                  (. gen (loadArg i))
                  (. cloiure.lang.Compiler$HostExpr (emitBoxReturn nil gen (nth pclasses i))))
                                        ;; call fn
                (. gen (invokeInterface ifn-type (new Method "invoke" obj-type
                                                      (to-types (replicate (+ (count ptypes)
                                                                              (if as-static 0 1))
                                                                           Object)))))
                                        ;; (into-array (cons obj-type
                                        ;;                 (replicate (count ptypes) obj-type))))))
                                        ;; unbox return
                (. gen (unbox rtype))
                (when (= (. rtype (getSort)) (. Type VOID))
                  (. gen (pop)))
                (. gen (goTo end-label))

                                        ;; else call supplied alternative generator
                (. gen (mark else-label))
                (. gen (pop))

                (else-gen gen m)

                (. gen (mark end-label))))
            (. gen (returnValue))
            (. gen (endMethod))))
        ]
                                        ;; start class definition
    (. cv (visit (. Opcodes V1_5) (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_SUPER))
                 cname nil (iname super)
                 (when-let [ifc (seq interfaces)]
                   (into-array (map iname ifc)))))

                                        ;; class annotations
    (add-annotations cv name-meta)

                                        ;; static fields for vars
    (doseq [v var-fields]
      (. cv (visitField (+ (. Opcodes ACC_PRIVATE) (. Opcodes ACC_FINAL) (. Opcodes ACC_STATIC))
                        (var-name v)
                        (. var-type getDescriptor)
                        nil nil)))

                                        ;; instance field for state
    (when state
      (. cv (visitField (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_FINAL))
                        state-name
                        (. obj-type getDescriptor)
                        nil nil)))

                                        ;; static init to set up var fields and load init
    (let [gen (new GeneratorAdapter (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_STATIC))
                   (. Method getMethod "void <clinit> ()")
                   nil nil cv)]
      (. gen (visitCode))
      (doseq [v var-fields]
        (. gen push impl-pkg-name)
        (. gen push (str prefix v))
        (. gen (invokeStatic var-type (. Method (getMethod "cloiure.lang.Var internPrivate(String,String)"))))
        (. gen putStatic ctype (var-name v) var-type))

      (when load-impl-ns
        (. gen push (str "/" impl-cname))
        (. gen push ctype)
        (. gen (invokeStatic util-type (. Method (getMethod "Object loadWithClass(String,Class)"))))
;        (. gen push (str (.replace impl-pkg-name \- \_) "__init"))
;        (. gen (invokeStatic class-type (. Method (getMethod "Class forName(String)"))))
        (. gen pop))

      (. gen (returnValue))
      (. gen (endMethod)))

                                        ;; ctors
    (doseq [[pclasses super-pclasses] ctor-sig-map]
      (let [constructor-annotations (meta pclasses)
            pclasses (map the-class pclasses)
            super-pclasses (map the-class super-pclasses)
            ptypes (to-types pclasses)
            super-ptypes (to-types super-pclasses)
            m (new Method "<init>" (. Type VOID_TYPE) ptypes)
            super-m (new Method "<init>" (. Type VOID_TYPE) super-ptypes)
            gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)
            _ (add-annotations gen constructor-annotations)
            no-init-label (. gen newLabel)
            end-label (. gen newLabel)
            no-post-init-label (. gen newLabel)
            end-post-init-label (. gen newLabel)
            nth-method (. Method (getMethod "Object nth(Object,int)"))
            local (. gen newLocal obj-type)]
        (. gen (visitCode))

        (if init
          (do
            (emit-get-var gen init-name)
            (. gen dup)
            (. gen ifNull no-init-label)
            (.checkCast gen ifn-type)
                                        ;; box init args
            (dotimes [i (count pclasses)]
              (. gen (loadArg i))
              (. cloiure.lang.Compiler$HostExpr (emitBoxReturn nil gen (nth pclasses i))))
                                        ;; call init fn
            (. gen (invokeInterface ifn-type (new Method "invoke" obj-type
                                                  (arg-types (count ptypes)))))
                                        ;; expecting [[super-ctor-args] state] returned
            (. gen dup)
            (. gen push (int 0))
            (. gen (invokeStatic rt-type nth-method))
            (. gen storeLocal local)

            (. gen (loadThis))
            (. gen dupX1)
            (dotimes [i (count super-pclasses)]
              (. gen loadLocal local)
              (. gen push (int i))
              (. gen (invokeStatic rt-type nth-method))
              (. cloiure.lang.Compiler$HostExpr (emitUnboxArg nil gen (nth super-pclasses i))))
            (. gen (invokeConstructor super-type super-m))

            (if state
              (do
                (. gen push (int 1))
                (. gen (invokeStatic rt-type nth-method))
                (. gen (putField ctype state-name obj-type)))
              (. gen pop))

            (. gen goTo end-label)
                                        ;; no init found
            (. gen mark no-init-label)
            (. gen (throwException ex-type (str impl-pkg-name "/" prefix init-name " not defined")))
            (. gen mark end-label))
          (if (= pclasses super-pclasses)
            (do
              (. gen (loadThis))
              (. gen (loadArgs))
              (. gen (invokeConstructor super-type super-m)))
            (throw (new Exception ":init not specified, but ctor and super ctor args differ"))))

        (when post-init
          (emit-get-var gen post-init-name)
          (. gen dup)
          (. gen ifNull no-post-init-label)
          (.checkCast gen ifn-type)
          (. gen (loadThis))
                                       ;; box init args
          (dotimes [i (count pclasses)]
            (. gen (loadArg i))
            (. cloiure.lang.Compiler$HostExpr (emitBoxReturn nil gen (nth pclasses i))))
                                       ;; call init fn
          (. gen (invokeInterface ifn-type (new Method "invoke" obj-type
                                                (arg-types (inc (count ptypes))))))
          (. gen pop)
          (. gen goTo end-post-init-label)
                                       ;; no init found
          (. gen mark no-post-init-label)
          (. gen (throwException ex-type (str impl-pkg-name "/" prefix post-init-name " not defined")))
          (. gen mark end-post-init-label))

        (. gen (returnValue))
        (. gen (endMethod))
                                        ;; factory
        (when factory
          (let [fm (new Method factory-name ctype ptypes)
                gen (new GeneratorAdapter (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_STATIC))
                         fm nil nil cv)]
            (. gen (visitCode))
            (. gen newInstance ctype)
            (. gen dup)
            (. gen (loadArgs))
            (. gen (invokeConstructor ctype m))
            (. gen (returnValue))
            (. gen (endMethod))))))

                                        ;; add methods matching supers', if no fn -> call super
    (let [mm (non-private-methods super)]
      (doseq [^java.lang.reflect.Method meth (vals mm)]
             (emit-forwarding-method (.getName meth) (.getParameterTypes meth) (.getReturnType meth) false
                                     (fn [^GeneratorAdapter gen ^Method m]
                                       (. gen (loadThis))
                                        ;; push args
                                       (. gen (loadArgs))
                                        ;; call super
                                       (. gen (visitMethodInsn (. Opcodes INVOKESPECIAL)
                                                               (. super-type (getInternalName))
                                                               (. m (getName))
                                                               (. m (getDescriptor)))))))
                                        ;; add methods matching interfaces', if no fn -> throw
      (reduce1 (fn [mm ^java.lang.reflect.Method meth]
                (if (contains? mm (method-sig meth))
                  mm
                  (do
                    (emit-forwarding-method (.getName meth) (.getParameterTypes meth) (.getReturnType meth) false
                                            emit-unsupported)
                    (assoc mm (method-sig meth) meth))))
              mm (mapcat #(.getMethods ^Class %) interfaces))
                                        ;; extra methods
       (doseq [[mname pclasses rclass :as msig] methods]
         (emit-forwarding-method mname pclasses rclass (:static (meta msig))
                                 emit-unsupported))
                                        ;; expose specified overridden superclass methods
       (doseq [[local-mname ^java.lang.reflect.Method m] (reduce1 (fn [ms [[name _ _] m]]
                              (if (contains? exposes-methods (symbol name))
                                (conj ms [((symbol name) exposes-methods) m])
                                ms)) [] (concat (seq mm)
                                                (seq (protected-final-methods super))))]
         (let [ptypes (to-types (.getParameterTypes m))
               rtype (totype (.getReturnType m))
               exposer-m (new Method (str local-mname) rtype ptypes)
               target-m (new Method (.getName m) rtype ptypes)
               gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) exposer-m nil nil cv)]
           (. gen (loadThis))
           (. gen (loadArgs))
           (. gen (visitMethodInsn (. Opcodes INVOKESPECIAL)
                                   (. super-type (getInternalName))
                                   (. target-m (getName))
                                   (. target-m (getDescriptor))))
           (. gen (returnValue))
           (. gen (endMethod)))))
                                        ;; main
    (when main
      (let [m (. Method getMethod "void main (String[])")
            gen (new GeneratorAdapter (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_STATIC))
                     m nil nil cv)
            no-main-label (. gen newLabel)
            end-label (. gen newLabel)]
        (. gen (visitCode))

        (emit-get-var gen main-name)
        (. gen dup)
        (. gen ifNull no-main-label)
        (.checkCast gen ifn-type)
        (. gen loadArgs)
        (. gen (invokeStatic rt-type (. Method (getMethod "cloiure.lang.ISeq seq(Object)"))))
        (. gen (invokeInterface ifn-type (new Method "applyTo" obj-type
                                              (into-array [iseq-type]))))
        (. gen pop)
        (. gen goTo end-label)
                                        ;; no main found
        (. gen mark no-main-label)
        (. gen (throwException ex-type (str impl-pkg-name "/" prefix main-name " not defined")))
        (. gen mark end-label)
        (. gen (returnValue))
        (. gen (endMethod))))
                                        ;; field exposers
    (doseq [[f {getter :get setter :set}] exposes]
      (let [fld (find-field super (str f))
            ftype (totype (.getType fld))
            static? (Modifier/isStatic (.getModifiers fld))
            acc (+ Opcodes/ACC_PUBLIC (if static? Opcodes/ACC_STATIC 0))]
        (when getter
          (let [m (new Method (str getter) ftype (to-types []))
                gen (new GeneratorAdapter acc m nil nil cv)]
            (. gen (visitCode))
            (if static?
              (. gen getStatic ctype (str f) ftype)
              (do
                (. gen loadThis)
                (. gen getField ctype (str f) ftype)))
            (. gen (returnValue))
            (. gen (endMethod))))
        (when setter
          (let [m (new Method (str setter) Type/VOID_TYPE (into-array [ftype]))
                gen (new GeneratorAdapter acc m nil nil cv)]
            (. gen (visitCode))
            (if static?
              (do
                (. gen loadArgs)
                (. gen putStatic ctype (str f) ftype))
              (do
                (. gen loadThis)
                (. gen loadArgs)
                (. gen putField ctype (str f) ftype)))
            (. gen (returnValue))
            (. gen (endMethod))))))
                                        ;; finish class def
    (. cv (visitEnd))
    [cname (. cv (toByteArray))]))

(§ defmacro gen-class
  "When compiling, generates compiled bytecode for a class with the
  given package-qualified :name (which, as all names in these
  parameters, can be a string or symbol), and writes the .class file
  to the *compile-path* directory.  When not compiling, does
  nothing. The gen-class construct contains no implementation, as the
  implementation will be dynamically sought by the generated class in
  functions in an implementing Cloiure namespace. Given a generated
  class org.mydomain.MyClass with a method named mymethod, gen-class
  will generate an implementation that looks for a function named by
  (str prefix mymethod) (default prefix: \"-\") in a
  Cloiure namespace specified by :impl-ns
  (defaults to the current namespace). All inherited methods,
  generated methods, and init and main functions (see :methods, :init,
  and :main below) will be found similarly prefixed. By default, the
  static initializer for the generated class will attempt to load the
  Cloiure support code for the class as a resource from the classpath,
  e.g. in the example case, ``org/mydomain/MyClass__init.class``. This
  behavior can be controlled by :load-impl-ns

  Note that methods with a maximum of 18 parameters are supported.

  In all subsequent sections taking types, the primitive types can be
  referred to by their Java names (int, float etc), and classes in the
  java.lang package can be used without a package qualifier. All other
  classes must be fully qualified.

  Options should be a set of key/value pairs, all except for :name are optional:

  :name aname

  The package-qualified name of the class to be generated

  :extends aclass

  Specifies the superclass, the non-private methods of which will be
  overridden by the class. If not provided, defaults to Object.

  :implements [interface ...]

  One or more interfaces, the methods of which will be implemented by the class.

  :init name

  If supplied, names a function that will be called with the arguments
  to the constructor. Must return [ [superclass-constructor-args] state]
  If not supplied, the constructor args are passed directly to
  the superclass constructor and the state will be nil

  :constructors {[param-types] [super-param-types], ...}

  By default, constructors are created for the generated class which
  match the signature(s) of the constructors for the superclass. This
  parameter may be used to explicitly specify constructors, each entry
  providing a mapping from a constructor signature to a superclass
  constructor signature. When you supply this, you must supply an :init
  specifier.

  :post-init name

  If supplied, names a function that will be called with the object as
  the first argument, followed by the arguments to the constructor.
  It will be called every time an object of this class is created,
  immediately after all the inherited constructors have completed.
  Its return value is ignored.

  :methods [ [name [param-types] return-type], ...]

  The generated class automatically defines all of the non-private
  methods of its superclasses/interfaces. This parameter can be used
  to specify the signatures of additional methods of the generated
  class. Static methods can be specified with ^{:static true} in the
  signature's metadata. Do not repeat superclass/interface signatures
  here.

  :main boolean

  If supplied and true, a static public main function will be generated. It will
  pass each string of the String[] argument as a separate argument to
  a function called (str prefix main).

  :factory name

  If supplied, a (set of) public static factory function(s) will be
  created with the given name, and the same signature(s) as the
  constructor(s).

  :state name

  If supplied, a public final instance field with the given name will be
  created. You must supply an :init function in order to provide a
  value for the state. Note that, though final, the state can be a ref
  or agent, supporting the creation of Java objects with transactional
  or asynchronous mutation semantics.

  :exposes {protected-field-name {:get name :set name}, ...}

  Since the implementations of the methods of the generated class
  occur in Cloiure functions, they have no access to the inherited
  protected fields of the superclass. This parameter can be used to
  generate public getter/setter methods exposing the protected field(s)
  for use in the implementation.

  :exposes-methods {super-method-name exposed-name, ...}

  It is sometimes necessary to call the superclass' implementation of an
  overridden method.  Those methods may be exposed and referred in
  the new method implementation by a local name.

  :prefix string

  Default: \"-\" Methods called e.g. Foo will be looked up in vars called
  prefixFoo in the implementing ns.

  :impl-ns name

  Default: the name of the current ns. Implementations of methods will be
  looked up in this namespace.

  :load-impl-ns boolean

  Default: true. Causes the static initializer for the generated class
  to reference the load code for the implementing namespace. Should be
  true when implementing-ns is the default, false if you intend to
  load the code via some other method."
  {:added "1.0"}

  [& options]
    (when *compile-files*
      (let [options-map (into1 {} (map vec (partition 2 options)))
            [cname bytecode] (generate-class options-map)]
        (cloiure.lang.Compiler/writeClassFile cname bytecode))))

;;;;;;;;;;;;;;;;;;;; gen-interface ;;;;;;;;;;;;;;;;;;;;;;
;; based on original contribution by Chris Houser

(§ defn- ^Type asm-type
  "Returns an asm Type object for c, which may be a primitive class
  (such as Integer/TYPE), any other class (such as Double), or a
  fully-qualified class name given as a string or symbol
  (such as 'java.lang.String)"
  [c]
  (if (or (instance? Class c) (prim->class c))
    (Type/getType (the-class c))
    (let [strx (str c)]
      (Type/getObjectType
       (.replace (if (some #{\. \[} strx)
                   strx
                   (str "java.lang." strx))
                 "." "/")))))

(§ defn- generate-interface
  [{:keys [name extends methods]}]
  (when (some #(-> % first cloiure.core/name (.contains "-")) methods)
    (throw
      (IllegalArgumentException. "Interface methods must not contain '-'")))
  (let [iname (.replace (str name) "." "/")
        cv (ClassWriter. ClassWriter/COMPUTE_MAXS)]
    (. cv visit Opcodes/V1_5 (+ Opcodes/ACC_PUBLIC
                                Opcodes/ACC_ABSTRACT
                                Opcodes/ACC_INTERFACE)
       iname nil "java/lang/Object"
       (when (seq extends)
         (into-array (map #(.getInternalName (asm-type %)) extends))))
    (when (not= "NO_SOURCE_FILE" *source-path*) (. cv visitSource *source-path* nil))
    (add-annotations cv (meta name))
    (doseq [[mname pclasses rclass pmetas] methods]
      (let [mv (. cv visitMethod (+ Opcodes/ACC_PUBLIC Opcodes/ACC_ABSTRACT)
                  (str mname)
                  (Type/getMethodDescriptor (asm-type rclass)
                                            (if pclasses
                                              (into-array Type (map asm-type pclasses))
                                              (make-array Type 0)))
                  nil nil)]
        (add-annotations mv (meta mname))
        (dotimes [i (count pmetas)]
          (add-annotations mv (nth pmetas i) i))
        (. mv visitEnd)))
    (. cv visitEnd)
    [iname (. cv toByteArray)]))

(§ defmacro gen-interface
  "When compiling, generates compiled bytecode for an interface with
  the given package-qualified :name (which, as all names in these
  parameters, can be a string or symbol), and writes the .class file
  to the *compile-path* directory.  When not compiling, does nothing.

  In all subsequent sections taking types, the primitive types can be
  referred to by their Java names (int, float etc), and classes in the
  java.lang package can be used without a package qualifier. All other
  classes must be fully qualified.

  Options should be a set of key/value pairs, all except for :name are
  optional:

  :name aname

  The package-qualified name of the class to be generated

  :extends [interface ...]

  One or more interfaces, which will be extended by this interface.

  :methods [ [name [param-types] return-type], ...]

  This parameter is used to specify the signatures of the methods of
  the generated interface.  Do not repeat superinterface signatures
  here."
  {:added "1.0"}

  [& options]
    (let [options-map (apply hash-map options)
          [cname bytecode] (generate-interface options-map)]
      (when *compile-files*
        (cloiure.lang.Compiler/writeClassFile cname bytecode))
      (.defineClass ^DynamicClassLoader (deref cloiure.lang.Compiler/LOADER)
                    (str (:name options-map)) bytecode options)))

(§ comment

(§ defn gen-and-load-class
  "Generates and immediately loads the bytecode for the specified
  class. Note that a class generated this way can be loaded only once
  - the JVM supports only one class with a given name per
  classloader. Subsequent to generation you can import it into any
  desired namespaces just like any other class. See gen-class for a
  description of the options."
  {:added "1.0"}

  [& options]
  (let [options-map (apply hash-map options)
        [cname bytecode] (generate-class options-map)]
    (.. (cloiure.lang.RT/getRootClassLoader) (defineClass cname bytecode options))))

)
(§ in-ns 'cloiure.core)

(§ import '(cloiure.lang Murmur3))

(§ set! *warn-on-reflection* true)

(§ deftype VecNode [edit arr])

(§ def EMPTY-NODE (VecNode. nil (object-array 32)))

(§ definterface IVecImpl
  (^int tailoff [])
  (arrayFor [^int i])
  (pushTail [^int level ^cloiure.core.VecNode parent ^cloiure.core.VecNode tailnode])
  (popTail [^int level node])
  (newPath [edit ^int level node])
  (doAssoc [^int level node ^int i val]))

(§ definterface ArrayManager
  (array [^int size])
  (^int alength [arr])
  (aclone [arr])
  (aget [arr ^int i])
  (aset [arr ^int i val]))

(§ deftype ArrayChunk [^cloiure.core.ArrayManager am arr ^int off ^int end]

  cloiure.lang.Indexed
  (nth [_ i] (.aget am arr (+ off i)))

  (count [_] (- end off))

  cloiure.lang.IChunk
  (dropFirst [_]
    (if (= off end)
      (throw (IllegalStateException. "dropFirst of empty chunk"))
      (new ArrayChunk am arr (inc off) end)))

  (reduce [_ f init]
    (loop [ret init i off]
      (if (< i end)
        (let [ret (f ret (.aget am arr i))]
          (if (reduced? ret)
            ret
            (recur ret (inc i))))
        ret))))

(§ deftype VecSeq [^cloiure.core.ArrayManager am ^cloiure.core.IVecImpl vec anode ^int i ^int offset]
  :no-print true

  cloiure.core.protocols.InternalReduce
  (internal-reduce
   [_ f val]
   (loop [result val
          aidx (+ i offset)]
     (if (< aidx (count vec))
       (let [node (.arrayFor vec aidx)
             result (loop [result result
                           node-idx (bit-and 0x1f aidx)]
                      (if (< node-idx (.alength am node))
                        (let [result (f result (.aget am node node-idx))]
                          (if (reduced? result)
                            result
                            (recur result (inc node-idx))))
                        result))]
         (if (reduced? result)
           @result
           (recur result (bit-and 0xffe0 (+ aidx 32)))))
       result)))

  cloiure.lang.ISeq
  (first [_] (.aget am anode offset))
  (next [this]
    (if (< (inc offset) (.alength am anode))
      (new VecSeq am vec anode i (inc offset))
      (.chunkedNext this)))
  (more [this]
    (let [s (.next this)]
      (or s (cloiure.lang.PersistentList/EMPTY))))
  (cons [this o]
    (cloiure.lang.Cons. o this))
  (count [this]
    (loop [i 1
           s (next this)]
      (if s
        (if (instance? cloiure.lang.Counted s)
          (+ i (.count s))
          (recur (inc i) (next s)))
        i)))
  (equiv [this o]
    (cond
     (identical? this o) true
     (or (instance? cloiure.lang.Sequential o) (instance? java.util.List o))
     (loop [me this
            you (seq o)]
       (if (nil? me)
         (nil? you)
         (and (cloiure.lang.Util/equiv (first me) (first you))
              (recur (next me) (next you)))))
     :else false))
  (empty [_]
    cloiure.lang.PersistentList/EMPTY)

  cloiure.lang.Seqable
  (seq [this] this)

  cloiure.lang.IChunkedSeq
  (chunkedFirst [_] (ArrayChunk. am anode offset (.alength am anode)))
  (chunkedNext [_]
   (let [nexti (+ i (.alength am anode))]
     (when (< nexti (count vec))
       (new VecSeq am vec (.arrayFor vec nexti) nexti 0))))
  (chunkedMore [this]
    (let [s (.chunkedNext this)]
      (or s (cloiure.lang.PersistentList/EMPTY)))))

(§ defmethod print-method ::VecSeq [v w]
  ((get (methods print-method) cloiure.lang.ISeq) v w))

(§ deftype Vec [^cloiure.core.ArrayManager am ^int cnt ^int shift ^cloiure.core.VecNode root tail _meta]
  Object
  (equals [this o]
    (cond
     (identical? this o) true
     (or (instance? cloiure.lang.IPersistentVector o) (instance? java.util.RandomAccess o))
       (and (= cnt (count o))
            (loop [i (int 0)]
              (cond
               (= i cnt) true
               (.equals (.nth this i) (nth o i)) (recur (inc i))
               :else false)))
     (or (instance? cloiure.lang.Sequential o) (instance? java.util.List o))
       (if-let [st (seq this)]
         (.equals st (seq o))
         (nil? (seq o)))
     :else false))

  ;; todo - cache
  (hashCode [this]
    (loop [hash (int 1) i (int 0)]
      (if (= i cnt)
        hash
        (let [val (.nth this i)]
          (recur (unchecked-add-int (unchecked-multiply-int 31 hash)
                                (cloiure.lang.Util/hash val))
                 (inc i))))))

  ;; todo - cache
  cloiure.lang.IHashEq
  (hasheq [this]
    (Murmur3/hashOrdered this))

  cloiure.lang.Counted
  (count [_] cnt)

  cloiure.lang.IMeta
  (meta [_] _meta)

  cloiure.lang.IObj
  (withMeta [_ m] (new Vec am cnt shift root tail m))

  cloiure.lang.Indexed
  (nth [this i]
    (let [a (.arrayFor this i)]
      (.aget am a (bit-and i (int 0x1f)))))
  (nth [this i not-found]
       (let [z (int 0)]
         (if (and (>= i z) (< i (.count this)))
           (.nth this i)
           not-found)))

  cloiure.lang.IPersistentCollection
  (cons [this val]
     (if (< (- cnt (.tailoff this)) (int 32))
      (let [new-tail (.array am (inc (.alength am tail)))]
        (System/arraycopy tail 0 new-tail 0 (.alength am tail))
        (.aset am new-tail (.alength am tail) val)
        (new Vec am (inc cnt) shift root new-tail (meta this)))
      (let [tail-node (VecNode. (.edit root) tail)]
        (if (> (bit-shift-right cnt (int 5)) (bit-shift-left (int 1) shift)) ;; overflow root?
          (let [new-root (VecNode. (.edit root) (object-array 32))]
            (doto ^objects (.arr new-root)
              (aset 0 root)
              (aset 1 (.newPath this (.edit root) shift tail-node)))
            (new Vec am (inc cnt) (+ shift (int 5)) new-root (let [tl (.array am 1)] (.aset am  tl 0 val) tl) (meta this)))
          (new Vec am (inc cnt) shift (.pushTail this shift root tail-node)
                 (let [tl (.array am 1)] (.aset am  tl 0 val) tl) (meta this))))))

  (empty [_] (new Vec am 0 5 EMPTY-NODE (.array am 0) nil))
  (equiv [this o]
    (cond
     (or (instance? cloiure.lang.IPersistentVector o) (instance? java.util.RandomAccess o))
       (and (= cnt (count o))
            (loop [i (int 0)]
              (cond
               (= i cnt) true
               (= (.nth this i) (nth o i)) (recur (inc i))
               :else false)))
     (or (instance? cloiure.lang.Sequential o) (instance? java.util.List o))
       (cloiure.lang.Util/equiv (seq this) (seq o))
     :else false))

  cloiure.lang.IPersistentStack
  (peek [this]
    (when (> cnt (int 0))
      (.nth this (dec cnt))))

  (pop [this]
   (cond
    (zero? cnt)
      (throw (IllegalStateException. "Can't pop empty vector"))
    (= 1 cnt)
      (new Vec am 0 5 EMPTY-NODE (.array am 0) (meta this))
    (> (- cnt (.tailoff this)) 1)
      (let [new-tail (.array am (dec (.alength am tail)))]
        (System/arraycopy tail 0 new-tail 0 (.alength am new-tail))
        (new Vec am (dec cnt) shift root new-tail (meta this)))
    :else
      (let [new-tail (.arrayFor this (- cnt 2))
            new-root ^cloiure.core.VecNode (.popTail this shift root)]
        (cond
         (nil? new-root)
           (new Vec am (dec cnt) shift EMPTY-NODE new-tail (meta this))
         (and (> shift 5) (nil? (aget ^objects (.arr new-root) 1)))
           (new Vec am (dec cnt) (- shift 5) (aget ^objects (.arr new-root) 0) new-tail (meta this))
         :else
           (new Vec am (dec cnt) shift new-root new-tail (meta this))))))

  cloiure.lang.IPersistentVector
  (assocN [this i val]
    (cond
     (and (<= (int 0) i) (< i cnt))
       (if (>= i (.tailoff this))
         (let [new-tail (.array am (.alength am tail))]
           (System/arraycopy tail 0 new-tail 0 (.alength am tail))
           (.aset am new-tail (bit-and i (int 0x1f)) val)
           (new Vec am cnt shift root new-tail (meta this)))
         (new Vec am cnt shift (.doAssoc this shift root i val) tail (meta this)))
     (= i cnt) (.cons this val)
     :else (throw (IndexOutOfBoundsException.))))
  (length [_] cnt)

  cloiure.lang.Reversible
  (rseq [this]
        (if (> (.count this) 0)
          (cloiure.lang.APersistentVector$RSeq. this (dec (.count this)))
          nil))

  cloiure.lang.Associative
  (assoc [this k v]
    (if (cloiure.lang.Util/isInteger k)
      (.assocN this k v)
      (throw (IllegalArgumentException. "Key must be integer"))))
  (containsKey [this k]
    (and (cloiure.lang.Util/isInteger k)
         (<= 0 (int k))
         (< (int k) cnt)))
  (entryAt [this k]
    (if (.containsKey this k)
      (cloiure.lang.MapEntry/create k (.nth this (int k)))
      nil))

  cloiure.lang.ILookup
  (valAt [this k not-found]
    (if (cloiure.lang.Util/isInteger k)
      (let [i (int k)]
        (if (and (>= i 0) (< i cnt))
          (.nth this i)
          not-found))
      not-found))

  (valAt [this k] (.valAt this k nil))

  cloiure.lang.IFn
  (invoke [this k]
    (if (cloiure.lang.Util/isInteger k)
      (let [i (int k)]
        (if (and (>= i 0) (< i cnt))
          (.nth this i)
          (throw (IndexOutOfBoundsException.))))
      (throw (IllegalArgumentException. "Key must be integer"))))

  cloiure.lang.Seqable
  (seq [this]
    (if (zero? cnt)
      nil
      (VecSeq. am this (.arrayFor this 0) 0 0)))

  cloiure.lang.Sequential ;; marker, no methods

  cloiure.core.IVecImpl
  (tailoff [_]
    (- cnt (.alength am tail)))

  (arrayFor [this i]
    (if (and  (<= (int 0) i) (< i cnt))
      (if (>= i (.tailoff this))
        tail
        (loop [node root level shift]
          (if (zero? level)
            (.arr node)
            (recur (aget ^objects (.arr node) (bit-and (bit-shift-right i level) (int 0x1f)))
                   (- level (int 5))))))
      (throw (IndexOutOfBoundsException.))))

  (pushTail [this level parent tailnode]
    (let [subidx (bit-and (bit-shift-right (dec cnt) level) (int 0x1f))
          parent ^cloiure.core.VecNode parent
          ret (VecNode. (.edit parent) (aclone ^objects (.arr parent)))
          node-to-insert (if (= level (int 5))
                           tailnode
                           (let [child (aget ^objects (.arr parent) subidx)]
                             (if child
                               (.pushTail this (- level (int 5)) child tailnode)
                               (.newPath this (.edit root) (- level (int 5)) tailnode))))]
      (aset ^objects (.arr ret) subidx node-to-insert)
      ret))

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
               (VecNode. (.edit root) arr))))
       (zero? subidx) nil
       :else (let [arr (aclone ^objects (.arr node))]
               (aset arr subidx nil)
               (VecNode. (.edit root) arr)))))

  (newPath [this edit ^int level node]
    (if (zero? level)
      node
      (let [ret (VecNode. edit (object-array 32))]
        (aset ^objects (.arr ret) 0 (.newPath this edit (- level (int 5)) node))
        ret)))

  (doAssoc [this level node i val]
    (let [node ^cloiure.core.VecNode node]
      (if (zero? level)
        ;; on this branch, array will need val type
        (let [arr (.aclone am (.arr node))]
          (.aset am arr (bit-and i (int 0x1f)) val)
          (VecNode. (.edit node) arr))
        (let [arr (aclone ^objects (.arr node))
              subidx (bit-and (bit-shift-right i level) (int 0x1f))]
          (aset arr subidx (.doAssoc this (- level (int 5)) (aget arr subidx) i val))
          (VecNode. (.edit node) arr)))))

  java.lang.Comparable
  (compareTo [this o]
    (if (identical? this o)
      0
      (let [^cloiure.lang.IPersistentVector v (cast cloiure.lang.IPersistentVector o)
            vcnt (.count v)]
        (cond
          (< cnt vcnt) -1
          (> cnt vcnt) 1
          :else
            (loop [i (int 0)]
              (if (= i cnt)
                0
                (let [comp (cloiure.lang.Util/compare (.nth this i) (.nth v i))]
                  (if (= 0 comp)
                    (recur (inc i))
                    comp))))))))

  java.lang.Iterable
  (iterator [this]
    (let [i (java.util.concurrent.atomic.AtomicInteger. 0)]
      (reify java.util.Iterator
        (hasNext [_] (< (.get i) cnt))
        (next [_] (try
                    (.nth this (dec (.incrementAndGet i)))
                    (catch IndexOutOfBoundsException _
                      (throw (java.util.NoSuchElementException.)))))
        (remove [_] (throw (UnsupportedOperationException.))))))

  java.util.Collection
  (contains [this o] (boolean (some #(= % o) this)))
  (containsAll [this c] (every? #(.contains this %) c))
  (isEmpty [_] (zero? cnt))
  (toArray [this] (into-array Object this))
  (toArray [this arr]
    (if (>= (count arr) cnt)
      (do
        (dotimes [i cnt]
          (aset arr i (.nth this i)))
        arr)
      (into-array Object this)))
  (size [_] cnt)
  (add [_ o] (throw (UnsupportedOperationException.)))
  (addAll [_ c] (throw (UnsupportedOperationException.)))
  (clear [_] (throw (UnsupportedOperationException.)))
  (^boolean remove [_ o] (throw (UnsupportedOperationException.)))
  (removeAll [_ c] (throw (UnsupportedOperationException.)))
  (retainAll [_ c] (throw (UnsupportedOperationException.)))

  java.util.List
  (get [this i] (.nth this i))
  (indexOf [this o]
    (loop [i (int 0)]
      (cond
        (== i cnt) -1
        (= o (.nth this i)) i
        :else (recur (inc i)))))
  (lastIndexOf [this o]
    (loop [i (dec cnt)]
      (cond
        (< i 0) -1
        (= o (.nth this i)) i
        :else (recur (dec i)))))
  (listIterator [this] (.listIterator this 0))
  (listIterator [this i]
    (let [i (java.util.concurrent.atomic.AtomicInteger. i)]
      (reify java.util.ListIterator
        (hasNext [_] (< (.get i) cnt))
        (hasPrevious [_] (pos? i))
        (next [_] (try
                    (.nth this (dec (.incrementAndGet i)))
                    (catch IndexOutOfBoundsException _
                      (throw (java.util.NoSuchElementException.)))))
        (nextIndex [_] (.get i))
        (previous [_] (try
                        (.nth this (.decrementAndGet i))
                        (catch IndexOutOfBoundsException _
                          (throw (java.util.NoSuchElementException.)))))
        (previousIndex [_] (dec (.get i)))
        (add [_ e] (throw (UnsupportedOperationException.)))
        (remove [_] (throw (UnsupportedOperationException.)))
        (set [_ e] (throw (UnsupportedOperationException.))))))
  (subList [this a z] (subvec this a z))
  (add [_ i o] (throw (UnsupportedOperationException.)))
  (addAll [_ i c] (throw (UnsupportedOperationException.)))
  (^Object remove [_ ^int i] (throw (UnsupportedOperationException.)))
  (set [_ i e] (throw (UnsupportedOperationException.)))
)

(§ defmethod print-method ::Vec [v w]
  ((get (methods print-method) cloiure.lang.IPersistentVector) v w))

(§ defmacro mk-am {:private true} [t]
  (let [garr (gensym)
        tgarr (with-meta garr {:tag (symbol (str t "s"))})]
    `(reify cloiure.core.ArrayManager
            (array [_ size#] (~(symbol (str t "-array")) size#))
            (alength [_ ~garr] (alength ~tgarr))
            (aclone [_ ~garr] (aclone ~tgarr))
            (aget [_ ~garr i#] (aget ~tgarr i#))
            (aset [_ ~garr i# val#] (aset ~tgarr i# (~t val#))))))

(§ def ^{:private true} ams
     {:int (mk-am int)
      :long (mk-am long)
      :float (mk-am float)
      :double (mk-am double)
      :byte (mk-am byte)
      :short (mk-am short)
      :char (mk-am char)
      :boolean (mk-am boolean)})

(§ defmacro ^:private ams-check [t]
  `(let [am# (ams ~t)]
     (if am#
       am#
       (throw (IllegalArgumentException. (str "Unrecognized type " ~t))))))

(§ defn vector-of
  "Creates a new vector of a single primitive type t, where t is one
  of :int :long :float :double :byte :short :char or :boolean. The
  resulting vector complies with the interface of vectors in general,
  but stores the values unboxed internally.

  Optionally takes one or more elements to populate the vector."
  {:added "1.2"
   :arglists '([t] [t & elements])}
  ([t]
   (let [^cloiure.core.ArrayManager am (ams-check t)]
     (Vec. am 0 5 EMPTY-NODE (.array am 0) nil)))
  ([t x1]
   (let [^cloiure.core.ArrayManager am (ams-check t)
         arr (.array am 1)]
     (.aset am arr 0 x1)
     (Vec. am 1 5 EMPTY-NODE arr nil)))
  ([t x1 x2]
   (let [^cloiure.core.ArrayManager am (ams-check t)
         arr (.array am 2)]
     (.aset am arr 0 x1)
     (.aset am arr 1 x2)
     (Vec. am 2 5 EMPTY-NODE arr nil)))
  ([t x1 x2 x3]
   (let [^cloiure.core.ArrayManager am (ams-check t)
         arr (.array am 3)]
     (.aset am arr 0 x1)
     (.aset am arr 1 x2)
     (.aset am arr 2 x3)
     (Vec. am 3 5 EMPTY-NODE arr nil)))
  ([t x1 x2 x3 x4]
   (let [^cloiure.core.ArrayManager am (ams-check t)
         arr (.array am 4)]
     (.aset am arr 0 x1)
     (.aset am arr 1 x2)
     (.aset am arr 2 x3)
     (.aset am arr 3 x4)
     (Vec. am 4 5 EMPTY-NODE arr nil)))
  ([t x1 x2 x3 x4 & xn]
   (loop [v  (vector-of t x1 x2 x3 x4)
          xn xn]
     (if xn
       (recur (conj v (first xn)) (next xn))
       v))))

#_(ns cloiure.instant
  (:import [java.util Calendar Date GregorianCalendar TimeZone]
           [java.sql Timestamp]))

(§ set! *warn-on-reflection* true)

;;; ------------------------------------------------------------------------
;;; convenience macros

(§ defmacro ^:private fail
  [msg]
  `(throw (RuntimeException. ~msg)))

(§ defmacro ^:private verify
  ([test msg] `(when-not ~test (fail ~msg)))
  ([test] `(verify ~test ~(str "failed: " (pr-str test)))))

(§ defn- divisible?
  [num div]
  (zero? (mod num div)))

(§ defn- indivisible?
  [num div]
  (not (divisible? num div)))

;;; ------------------------------------------------------------------------
;;; parser implementation

(§ defn- parse-int [^String s]
  (Long/parseLong s))

(§ defn- zero-fill-right [^String s width]
  (cond (= width (count s)) s
        (< width (count s)) (.substring s 0 width)
        :else (loop [b (StringBuilder. s)]
                (if (< (.length b) width)
                  (recur (.append b \0))
                  (.toString b)))))

(§ def parse-timestamp
     "Parse a string containing an RFC3339-like like timestamp.

The function new-instant is called with the following arguments.

                min  max           default
                ---  ------------  -------
  years          0           9999      N/A (s must provide years)
  months         1             12        1
  days           1             31        1 (actual max days depends
  hours          0             23        0  on month and year)
  minutes        0             59        0
  seconds        0             60        0 (though 60 is only valid
  nanoseconds    0      999999999        0  when minutes is 59)
  offset-sign   -1              1        0
  offset-hours   0             23        0
  offset-minutes 0             59        0

These are all integers and will be non-nil. (The listed defaults
will be passed if the corresponding field is not present in s.)

Grammar (of s):

  date-fullyear   = 4DIGIT
  date-month      = 2DIGIT  ; 01-12
  date-mday       = 2DIGIT  ; 01-28, 01-29, 01-30, 01-31 based on
                            ; month/year
  time-hour       = 2DIGIT  ; 00-23
  time-minute     = 2DIGIT  ; 00-59
  time-second     = 2DIGIT  ; 00-58, 00-59, 00-60 based on leap second
                            ; rules
  time-secfrac    = '.' 1*DIGIT
  time-numoffset  = ('+' / '-') time-hour ':' time-minute
  time-offset     = 'Z' / time-numoffset

  time-part       = time-hour [ ':' time-minute [ ':' time-second
                    [time-secfrac] [time-offset] ] ]

  timestamp       = date-year [ '-' date-month [ '-' date-mday
                    [ 'T' time-part ] ] ]

Unlike RFC3339:

  - we only parse the timestamp format
  - timestamp can elide trailing components
  - time-offset is optional (defaults to +00:00)

Though time-offset is syntactically optional, a missing time-offset
will be treated as if the time-offset zero (+00:00) had been
specified.
"
     (let [timestamp #"(\d\d\d\d)(?:-(\d\d)(?:-(\d\d)(?:[T](\d\d)(?::(\d\d)(?::(\d\d)(?:[.](\d+))?)?)?)?)?)?(?:[Z]|([-+])(\d\d):(\d\d))?"]

       (fn [new-instant ^CharSequence cs]
         (if-let [[_ years months days hours minutes seconds fraction
                   offset-sign offset-hours offset-minutes]
                  (re-matches timestamp cs)]
           (new-instant
            (parse-int years)
            (if-not months   1 (parse-int months))
            (if-not days     1 (parse-int days))
            (if-not hours    0 (parse-int hours))
            (if-not minutes  0 (parse-int minutes))
            (if-not seconds  0 (parse-int seconds))
            (if-not fraction 0 (parse-int (zero-fill-right fraction 9)))
            (cond (= "-" offset-sign) -1
                  (= "+" offset-sign)  1
                  :else                0)
            (if-not offset-hours   0 (parse-int offset-hours))
            (if-not offset-minutes 0 (parse-int offset-minutes)))
           (fail (str "Unrecognized date/time syntax: " cs))))))

;;; ------------------------------------------------------------------------
;;; Verification of Extra-Grammatical Restrictions from RFC3339

(§ defn- leap-year?
  [year]
  (and (divisible? year 4)
       (or (indivisible? year 100)
           (divisible? year 400))))

(§ def ^:private days-in-month
     (let [dim-norm [nil 31 28 31 30 31 30 31 31 30 31 30 31]
           dim-leap [nil 31 29 31 30 31 30 31 31 30 31 30 31]]
       (fn [month leap-year?]
         ((if leap-year? dim-leap dim-norm) month))))

(§ defn validated
  "Return a function which constructs an instant by calling constructor
after first validating that those arguments are in range and otherwise
plausible. The resulting function will throw an exception if called
with invalid arguments."
  [new-instance]
  (fn [years months days hours minutes seconds nanoseconds
       offset-sign offset-hours offset-minutes]
    (verify (<= 1 months 12))
    (verify (<= 1 days (days-in-month months (leap-year? years))))
    (verify (<= 0 hours 23))
    (verify (<= 0 minutes 59))
    (verify (<= 0 seconds (if (= minutes 59) 60 59)))
    (verify (<= 0 nanoseconds 999999999))
    (verify (<= -1 offset-sign 1))
    (verify (<= 0 offset-hours 23))
    (verify (<= 0 offset-minutes 59))
    (new-instance years months days hours minutes seconds nanoseconds
                  offset-sign offset-hours offset-minutes)))

;;; ------------------------------------------------------------------------
;;; print integration

(§ def ^:private ^ThreadLocal thread-local-utc-date-format
  ;; SimpleDateFormat is not thread-safe, so we use a ThreadLocal proxy for access.
  ;; http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335
  (proxy [ThreadLocal] []
    (initialValue []
      (doto (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00")
        ;; RFC3339 says to use -00:00 when the timezone is unknown (+00:00 implies a known GMT)
        (.setTimeZone (java.util.TimeZone/getTimeZone "GMT"))))))

(§ defn- print-date
  "Print a java.util.Date as RFC3339 timestamp, always in UTC."
  [^java.util.Date d, ^java.io.Writer w]
  (let [^java.text.DateFormat utc-format (.get thread-local-utc-date-format)]
    (.write w "#inst \"")
    (.write w (.format utc-format d))
    (.write w "\"")))

(§ defmethod print-method java.util.Date
  [^java.util.Date d, ^java.io.Writer w]
  (print-date d w))

(§ defmethod print-dup java.util.Date
  [^java.util.Date d, ^java.io.Writer w]
  (print-date d w))

(§ defn- print-calendar
  "Print a java.util.Calendar as RFC3339 timestamp, preserving timezone."
  [^java.util.Calendar c, ^java.io.Writer w]
  (let [calstr (format "%1$tFT%1$tT.%1$tL%1$tz" c)
        offset-minutes (- (.length calstr) 2)]
    ;; calstr is almost right, but is missing the colon in the offset
    (.write w "#inst \"")
    (.write w calstr 0 offset-minutes)
    (.write w ":")
    (.write w calstr offset-minutes 2)
    (.write w "\"")))

(§ defmethod print-method java.util.Calendar
  [^java.util.Calendar c, ^java.io.Writer w]
  (print-calendar c w))

(§ defmethod print-dup java.util.Calendar
  [^java.util.Calendar c, ^java.io.Writer w]
  (print-calendar c w))

(§ def ^:private ^ThreadLocal thread-local-utc-timestamp-format
  ;; SimpleDateFormat is not thread-safe, so we use a ThreadLocal proxy for access.
  ;; http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335
  (proxy [ThreadLocal] []
    (initialValue []
      (doto (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss")
        (.setTimeZone (java.util.TimeZone/getTimeZone "GMT"))))))

(§ defn- print-timestamp
  "Print a java.sql.Timestamp as RFC3339 timestamp, always in UTC."
  [^java.sql.Timestamp ts, ^java.io.Writer w]
  (let [^java.text.DateFormat utc-format (.get thread-local-utc-timestamp-format)]
    (.write w "#inst \"")
    (.write w (.format utc-format ts))
    ;; add on nanos and offset
    ;; RFC3339 says to use -00:00 when the timezone is unknown (+00:00 implies a known GMT)
    (.write w (format ".%09d-00:00" (.getNanos ts)))
    (.write w "\"")))

(§ defmethod print-method java.sql.Timestamp
  [^java.sql.Timestamp ts, ^java.io.Writer w]
  (print-timestamp ts w))

(§ defmethod print-dup java.sql.Timestamp
  [^java.sql.Timestamp ts, ^java.io.Writer w]
  (print-timestamp ts w))

;;; ------------------------------------------------------------------------
;;; reader integration

(§ defn- construct-calendar
  "Construct a java.util.Calendar, preserving the timezone
offset, but truncating the subsecond fraction to milliseconds."
  ^GregorianCalendar
  [years months days hours minutes seconds nanoseconds
   offset-sign offset-hours offset-minutes]
  (doto (GregorianCalendar. years (dec months) days hours minutes seconds)
    (.set Calendar/MILLISECOND (quot nanoseconds 1000000))
    (.setTimeZone (TimeZone/getTimeZone
                   (format "GMT%s%02d:%02d"
                           (if (neg? offset-sign) "-" "+")
                           offset-hours offset-minutes)))))

(§ defn- construct-date
  "Construct a java.util.Date, which expresses the original instant as
milliseconds since the epoch, UTC."
  [years months days hours minutes seconds nanoseconds
   offset-sign offset-hours offset-minutes]
  (.getTime (construct-calendar years months days
                                hours minutes seconds nanoseconds
                                offset-sign offset-hours offset-minutes)))

(§ defn- construct-timestamp
  "Construct a java.sql.Timestamp, which has nanosecond precision."
  [years months days hours minutes seconds nanoseconds
   offset-sign offset-hours offset-minutes]
  (doto (Timestamp.
         (.getTimeInMillis
          (construct-calendar years months days
                              hours minutes seconds 0
                              offset-sign offset-hours offset-minutes)))
    ;; nanos must be set separately, pass 0 above for the base calendar
    (.setNanos nanoseconds)))

(§ def read-instant-date
  "To read an instant as a java.util.Date, bind *data-readers* to a map with
this var as the value for the 'inst key. The timezone offset will be used
to convert into UTC."
  (partial parse-timestamp (validated construct-date)))

(§ def read-instant-calendar
  "To read an instant as a java.util.Calendar, bind *data-readers* to a map with
this var as the value for the 'inst key.  Calendar preserves the timezone
offset."
  (partial parse-timestamp (validated construct-calendar)))

(§ def read-instant-timestamp
  "To read an instant as a java.sql.Timestamp, bind *data-readers* to a
map with this var as the value for the 'inst key. Timestamp preserves
fractional seconds with nanosecond precision. The timezone offset will
be used to convert into UTC."
  (partial parse-timestamp (validated construct-timestamp)))

#_(ns ^{:doc "Top-level main function for Cloiure REPL and scripts."
       :author "Stephen C. Gilardi and Rich Hickey"}
  cloiure.main
  (:refer-cloiure :exclude [with-bindings])
  (:require [cloiure.spec.alpha])
  (:import (cloiure.lang Compiler Compiler$CompilerException LineNumberingPushbackReader RT))
  ;; (:use [cloiure.repl :only (demunge root-cause stack-element-str)])
  )

(§ declare main)

;;;;;;;;;;;;;;;;;;; redundantly copied from cloiure.repl to avoid dep ;;;;;;;;;;;;;;
(§ defn demunge
  "Given a string representation of a fn class,
  as in a stack trace element, returns a readable version."
  {:added "1.3"}
  [fn-name]
  (cloiure.lang.Compiler/demunge fn-name))

(§ defn root-cause
  "Returns the initial cause of an exception or error by peeling off all of
  its wrappers"
  {:added "1.3"}
  [^Throwable t]
  (loop [cause t]
    (if (and (instance? cloiure.lang.Compiler$CompilerException cause)
             (not= (.source ^cloiure.lang.Compiler$CompilerException cause) "NO_SOURCE_FILE"))
      cause
      (if-let [cause (.getCause cause)]
        (recur cause)
        cause))))

(§ defn stack-element-str
  "Returns a (possibly unmunged) string representation of a StackTraceElement"
  {:added "1.3"}
  [^StackTraceElement el]
  (let [file (.getFileName el)
        cloiure-fn? (and file (or (.endsWith file ".cli")
                                  (.endsWith file ".clic")
                                  (= file "NO_SOURCE_FILE")))]
    (str (if cloiure-fn?
           (demunge (.getClassName el))
           (str (.getClassName el) "." (.getMethodName el)))
         " (" (.getFileName el) ":" (.getLineNumber el) ")")))
;;;;;;;;;;;;;;;;;;; end of redundantly copied from cloiure.repl to avoid dep ;;;;;;;;;;;;;;

(§ defmacro with-bindings
  "Executes body in the context of thread-local bindings for several vars
  that often need to be set!: *ns* *warn-on-reflection* *math-context*
  *print-meta* *print-length* *print-level* *compile-path*
  *command-line-args* *1 *2 *3 *e"
  [& body]
  `(binding [*ns* *ns*
             *warn-on-reflection* *warn-on-reflection*
             *math-context* *math-context*
             *print-meta* *print-meta*
             *print-length* *print-length*
             *print-level* *print-level*
             *print-namespace-maps* true
             *data-readers* *data-readers*
             *default-data-reader-fn* *default-data-reader-fn*
             *compile-path* (System/getProperty "cloiure.compile.path" "classes")
             *command-line-args* *command-line-args*
             *unchecked-math* *unchecked-math*
             *assert* *assert*
             cloiure.spec.alpha/*explain-out* cloiure.spec.alpha/*explain-out*
             *1 nil
             *2 nil
             *3 nil
             *e nil]
     ~@body))

(§ defn repl-prompt
  "Default :prompt hook for repl"
  []
  (printf "%s=> " (ns-name *ns*)))

(§ defn skip-if-eol
  "If the next character on stream s is a newline, skips it, otherwise
  leaves the stream untouched. Returns :line-start, :stream-end, or :body
  to indicate the relative location of the next character on s. The stream
  must either be an instance of LineNumberingPushbackReader or duplicate
  its behavior of both supporting .unread and collapsing all of CR, LF, and
  CRLF to a single \\newline."
  [s]
  (let [c (.read s)]
    (cond
     (= c (int \newline)) :line-start
     (= c -1) :stream-end
     :else (do (.unread s c) :body))))

(§ defn skip-whitespace
  "Skips whitespace characters on stream s. Returns :line-start, :stream-end,
  or :body to indicate the relative location of the next character on s.
  Interprets comma as whitespace and semicolon as comment to end of line.
  Does not interpret #! as comment to end of line because only one
  character of lookahead is available. The stream must either be an
  instance of LineNumberingPushbackReader or duplicate its behavior of both
  supporting .unread and collapsing all of CR, LF, and CRLF to a single
  \\newline."
  [s]
  (loop [c (.read s)]
    (cond
     (= c (int \newline)) :line-start
     (= c -1) :stream-end
     (= c (int \;)) (do (.readLine s) :line-start)
     (or (Character/isWhitespace (char c)) (= c (int \,))) (recur (.read s))
     :else (do (.unread s c) :body))))

(§ defn repl-read
  "Default :read hook for repl. Reads from *in* which must either be an
  instance of LineNumberingPushbackReader or duplicate its behavior of both
  supporting .unread and collapsing all of CR, LF, and CRLF into a single
  \\newline. repl-read:
    - skips whitespace, then
      - returns request-prompt on start of line, or
      - returns request-exit on end of stream, or
      - reads an object from the input stream, then
        - skips the next input character if it's end of line, then
        - returns the object."
  [request-prompt request-exit]
  (or ({:line-start request-prompt :stream-end request-exit}
       (skip-whitespace *in*))
      (let [input (read {:read-cond :allow} *in*)]
        (skip-if-eol *in*)
        input)))

(§ defn repl-exception
  "Returns the root cause of throwables"
  [throwable]
  (root-cause throwable))

(§ defn repl-caught
  "Default :caught hook for repl"
  [e]
  (let [ex (repl-exception e)
        tr (.getStackTrace ex)
        el (when-not (zero? (count tr)) (aget tr 0))]
    (binding [*out* *err*]
      (println (str (-> ex class .getSimpleName)
                    " " (.getMessage ex) " "
                    (when-not (instance? cloiure.lang.Compiler$CompilerException ex)
                      (str " " (if el (stack-element-str el) "[trace missing]"))))))))

(§ def ^{:doc "A sequence of lib specs that are applied to `require`
by default when a new command-line REPL is started."} repl-requires
  '[[cloiure.repl :refer (source apropos dir pst doc find-doc)]])

(§ defmacro with-read-known
  "Evaluates body with *read-eval* set to a \"known\" value,
   i.e. substituting true for :unknown if necessary."
  [& body]
  `(binding [*read-eval* (if (= :unknown *read-eval*) true *read-eval*)]
     ~@body))

(§ defn repl
  "Generic, reusable, read-eval-print loop. By default, reads from *in*,
  writes to *out*, and prints exception summaries to *err*. If you use the
  default :read hook, *in* must either be an instance of
  LineNumberingPushbackReader or duplicate its behavior of both supporting
  .unread and collapsing CR, LF, and CRLF into a single \\newline. Options
  are sequential keyword-value pairs. Available options and their defaults:

     - :init, function of no arguments, initialization hook called with
       bindings for set!-able vars in place.
       default: #()

     - :need-prompt, function of no arguments, called before each
       read-eval-print except the first, the user will be prompted if it
       returns true.
       default: (if (instance? LineNumberingPushbackReader *in*)
                  #(.atLineStart *in*)
                  #(identity true))

     - :prompt, function of no arguments, prompts for more input.
       default: repl-prompt

     - :flush, function of no arguments, flushes output
       default: flush

     - :read, function of two arguments, reads from *in*:
         - returns its first argument to request a fresh prompt
           - depending on need-prompt, this may cause the repl to prompt
             before reading again
         - returns its second argument to request an exit from the repl
         - else returns the next object read from the input stream
       default: repl-read

     - :eval, function of one argument, returns the evaluation of its
       argument
       default: eval

     - :print, function of one argument, prints its argument to the output
       default: prn

     - :caught, function of one argument, a throwable, called when
       read, eval, or print throws an exception or error
       default: repl-caught"
  [& options]
  (let [cl (.getContextClassLoader (Thread/currentThread))]
    (.setContextClassLoader (Thread/currentThread) (cloiure.lang.DynamicClassLoader. cl)))
  (let [{:keys [init need-prompt prompt flush read eval print caught]
         :or {init        #()
              need-prompt (if (instance? LineNumberingPushbackReader *in*)
                            #(.atLineStart ^LineNumberingPushbackReader *in*)
                            #(identity true))
              prompt      repl-prompt
              flush       flush
              read        repl-read
              eval        eval
              print       prn
              caught      repl-caught}}
        (apply hash-map options)
        request-prompt (Object.)
        request-exit (Object.)
        read-eval-print
        (fn []
          (try
            (let [read-eval *read-eval*
                  input (with-read-known (read request-prompt request-exit))]
             (or (#{request-prompt request-exit} input)
                 (let [value (binding [*read-eval* read-eval] (eval input))]
                   (print value)
                   (set! *3 *2)
                   (set! *2 *1)
                   (set! *1 value))))
           (catch Throwable e
             (caught e)
             (set! *e e))))]
    (with-bindings
     (try
      (init)
      (catch Throwable e
        (caught e)
        (set! *e e)))
     (prompt)
     (flush)
     (loop []
       (when-not
         (try (identical? (read-eval-print) request-exit)
          (catch Throwable e
           (caught e)
           (set! *e e)
           nil))
         (when (need-prompt)
           (prompt)
           (flush))
         (recur))))))

(§ defn load-script
  "Loads Cloiure source from a file or resource given its path. Paths
  beginning with @ or @/ are considered relative to classpath."
  [^String path]
  (if (.startsWith path "@")
    (RT/loadResourceScript
     (.substring path (if (.startsWith path "@/") 2 1)))
    (Compiler/loadFile path)))

(§ defn- init-opt
  "Load a script"
  [path]
  (load-script path))

(§ defn- eval-opt
  "Evals expressions in str, prints each non-nil result using prn"
  [str]
  (let [eof (Object.)
        reader (LineNumberingPushbackReader. (java.io.StringReader. str))]
      (loop [input (with-read-known (read reader false eof))]
        (when-not (= input eof)
          (let [value (eval input)]
            (when-not (nil? value)
              (prn value))
            (recur (with-read-known (read reader false eof))))))))

(§ defn- init-dispatch
  "Returns the handler associated with an init opt"
  [opt]
  ({"-i"     init-opt
    "--init" init-opt
    "-e"     eval-opt
    "--eval" eval-opt} opt))

(§ defn- initialize
  "Common initialize routine for repl, script, and null opts"
  [args inits]
  (in-ns 'user)
  (set! *command-line-args* args)
  (doseq [[opt arg] inits]
    ((init-dispatch opt) arg)))

(§ defn- main-opt
  "Call the -main function from a namespace with string arguments from
  the command line."
  [[_ main-ns & args] inits]
  (with-bindings
    (initialize args inits)
    (apply (ns-resolve (doto (symbol main-ns) require) '-main) args)))

(§ defn- repl-opt
  "Start a repl with args and inits. Print greeting if no eval options were
  present"
  [[_ & args] inits]
  (when-not (some #(= eval-opt (init-dispatch (first %))) inits)
    (println "Cloiure" (cloiure-version)))
  (repl :init (fn []
                (initialize args inits)
                (apply require repl-requires)))
  (prn)
  (System/exit 0))

(§ defn- script-opt
  "Run a script from a file, resource, or standard in with args and inits"
  [[path & args] inits]
  (with-bindings
    (initialize args inits)
    (if (= path "-")
      (load-reader *in*)
      (load-script path))))

(§ defn- null-opt
  "No repl or script opt present, just bind args and run inits"
  [args inits]
  (with-bindings
    (initialize args inits)))

(§ defn- help-opt
  "Print help text for main"
  [_ _]
  (println (:doc (meta (var main)))))

(§ defn- main-dispatch
  "Returns the handler associated with a main option"
  [opt]
  (or
   ({"-r"     repl-opt
     "--repl" repl-opt
     "-m"     main-opt
     "--main" main-opt
     nil      null-opt
     "-h"     help-opt
     "--help" help-opt
     "-?"     help-opt} opt)
   script-opt))

(§ defn main
  "Usage: java -cp cloiure.jar cloiure.main [init-opt*] [main-opt] [arg*]

  With no options or args, runs an interactive Read-Eval-Print Loop

  init options:
    -i, --init path     Load a file or resource
    -e, --eval string   Evaluate expressions in string; print non-nil values

  main options:
    -m, --main ns-name  Call the -main function from a namespace with args
    -r, --repl          Run a repl
    path                Run a script from a file or resource
    -                   Run a script from standard input
    -h, -?, --help      Print this help message and exit

  operation:

    - Establishes thread-local bindings for commonly set!-able vars
    - Enters the user namespace
    - Binds *command-line-args* to a seq of strings containing command line
      args that appear after any main option
    - Runs all init options in order
    - Calls a -main function or runs a repl or script if requested

  The init options may be repeated and mixed freely, but must appear before
  any main option. The appearance of any eval option before running a repl
  suppresses the usual repl greeting message: \"Cloiure ~(cloiure-version)\".

  Paths may be absolute or relative in the filesystem or relative to
  classpath. Classpath-relative paths have prefix of @ or @/"
  [& args]
  (try
   (if args
     (loop [[opt arg & more :as args] args inits []]
       (if (init-dispatch opt)
         (recur more (conj inits [opt arg]))
         ((main-dispatch opt) args inits)))
     (repl-opt nil nil))
   (finally
     (flush))))

#_(ns ^{:author "Stuart Halloway"
      :added "1.3"
      :doc "Reflection on Host Types
Alpha - subject to change.

Two main entry points:

* type-reflect reflects on something that implements TypeReference.
* reflect (for REPL use) reflects on the class of an instance, or
  on a class if passed a class

Key features:

* Exposes the read side of reflection as pure data. Reflecting
  on a type returns a map with keys :bases, :flags, and :members.

* Canonicalizes class names as Cloiure symbols. Types can extend
  to the TypeReference protocol to indicate that they can be
  unambiguously resolved as a type name. The canonical format
  requires one non-Java-ish convention: array brackets are <>
  instead of [] so they can be part of a Cloiure symbol.

* Pluggable Reflectors for different implementations. The default
  JavaReflector is good when you have a class in hand, or use
  the AsmReflector for \"hands off\" reflection without forcing
  classes to load.

Platform implementers must:

* Create an implementation of Reflector.
* Create one or more implementations of TypeReference.
* def default-reflector to be an instance that satisfies Reflector."}
  cloiure.reflect
  (:require [cloiure.set :as set]))

(§ defprotocol Reflector
  "Protocol for reflection implementers."
  (do-reflect [reflector typeref]))

(§ defprotocol TypeReference
  "A TypeReference can be unambiguously converted to a type name on
   the host platform.

   All typerefs are normalized into symbols. If you need to
   normalize a typeref yourself, call typesym."
  (typename [o] "Returns Java name as returned by ASM getClassName, e.g. byte[], java.lang.String[]"))

(§ declare default-reflector)

(§ defn type-reflect
  "Alpha - subject to change.
   Reflect on a typeref, returning a map with :bases, :flags, and
  :members. In the discussion below, names are always Cloiure symbols.

   :bases            a set of names of the type's bases
   :flags            a set of keywords naming the boolean attributes
                     of the type.
   :members          a set of the type's members. Each member is a map
                     and can be a constructor, method, or field.

   Keys common to all members:
   :name             name of the type
   :declaring-class  name of the declarer
   :flags            keyword naming boolean attributes of the member

   Keys specific to constructors:
   :parameter-types  vector of parameter type names
   :exception-types  vector of exception type names

   Key specific to methods:
   :parameter-types  vector of parameter type names
   :exception-types  vector of exception type names
   :return-type      return type name

   Keys specific to fields:
   :type             type name

   Options:

     :ancestors     in addition to the keys described above, also
                    include an :ancestors key with the entire set of
                    ancestors, and add all ancestor members to
                    :members.
     :reflector     implementation to use. Defaults to JavaReflector,
                    AsmReflector is also an option."
  {:added "1.3"}
  [typeref & options]
  (let [{:keys [ancestors reflector]}
        (merge {:reflector default-reflector}
               (apply hash-map options))
        refl (partial do-reflect reflector)
        result (refl typeref)]
    ;; could make simpler loop of two args: names an
    (if ancestors
      (let [make-ancestor-map (fn [names]
                            (zipmap names (map refl names)))]
        (loop [reflections (make-ancestor-map (:bases result))]
          (let [ancestors-visited (set (keys reflections))
                ancestors-to-visit (set/difference (set (mapcat :bases (vals reflections)))
                                               ancestors-visited)]
            (if (seq ancestors-to-visit)
              (recur (merge reflections (make-ancestor-map ancestors-to-visit)))
              (apply merge-with into result {:ancestors ancestors-visited}
                     (map #(select-keys % [:members]) (vals reflections)))))))
      result)))

(§ defn reflect
  "Alpha - subject to change.
   Reflect on the type of obj (or obj itself if obj is a class).
   Return value and options are the same as for type-reflect. "
  {:added "1.3"}
  [obj & options]
  (apply type-reflect (if (class? obj) obj (class obj)) options))

(§ load "reflect/java")
(§ in-ns 'cloiure.reflect)

(§ require '[cloiure.set :as set]
         '[cloiure.string :as str])
(§ import '[cloiure.asm ClassReader ClassVisitor Type Opcodes]
         '[java.lang.reflect Modifier]
         java.io.InputStream)

(§ extend-protocol TypeReference
  cloiure.lang.Symbol
  (typename [s] (str/replace (str s) "<>" "[]"))

  Class
  ;; neither .getName not .getSimpleName returns the right thing, so best to delegate to Type
  (typename
   [c]
   (typename (Type/getType c)))

  Type
  (typename
   [t]
   (-> (.getClassName t))))

(§ defn- typesym
  "Given a typeref, create a legal Cloiure symbol version of the
   type's name."
  [t]
  (-> (typename t)
      (str/replace "[]" "<>")
      (symbol)))

(§ defn- resource-name
  "Given a typeref, return implied resource name. Used by Reflectors
   such as ASM that need to find and read classbytes from files."
  [typeref]
  (-> (typename typeref)
      (str/replace "." "/")
      (str ".class")))

(§ defn- access-flag
  [[name flag & contexts]]
  {:name name :flag flag :contexts (set (map keyword contexts))})

(§ defn- field-descriptor->class-symbol
  "Convert a Java field descriptor to a Cloiure class symbol. Field
   descriptors are described in section 4.3.2 of the JVM spec, 2nd ed.:
   http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#14152"
  [^String d]
  {:pre [(string? d)]}
  (typesym (Type/getType d)))

(§ defn- internal-name->class-symbol
  "Convert a Java internal name to a Cloiure class symbol. Internal
   names uses slashes instead of dots, e.g. java/lang/String. See
   Section 4.2 of the JVM spec, 2nd ed.:

   http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#14757"
  [d]
  {:pre [(string? d)]}
  (typesym (Type/getObjectType d)))

(§ def ^{:doc "The Java access bitflags, along with their friendly names and
the kinds of objects to which they can apply."}
  flag-descriptors
  (vec
   (map access-flag
        [[:public 0x0001 :class :field :method]
         [:private 0x002 :class :field :method]
         [:protected 0x0004  :class :field :method]
         [:static 0x0008  :field :method]
         [:final 0x0010  :class :field :method]
         ;; :super is ancient history and is unfindable (?) by
         ;; reflection. skip it
         #_[:super 0x0020  :class]
         [:synchronized 0x0020  :method]
         [:volatile 0x0040  :field]
         [:bridge 0x0040  :method]
         [:varargs 0x0080  :method]
         [:transient 0x0080  :field]
         [:native 0x0100  :method]
         [:interface 0x0200  :class]
         [:abstract 0x0400  :class :method]
         [:strict 0x0800  :method]
         [:synthetic 0x1000  :class :field :method]
         [:annotation 0x2000  :class]
         [:enum 0x4000  :class :field :inner]])))

(§ defn- parse-flags
  "Convert reflection bitflags into a set of keywords."
  [flags context]
  (reduce
   (fn [result fd]
     (if (and (get (:contexts fd) context)
              (not (zero? (bit-and flags (:flag fd)))))
       (conj result (:name fd))
       result))
   #{}
   flag-descriptors))

(§ defrecord Constructor
  [name declaring-class parameter-types exception-types flags])

(§ defn- constructor->map
  [^java.lang.reflect.Constructor constructor]
  (Constructor.
   (symbol (.getName constructor))
   (typesym (.getDeclaringClass constructor))
   (vec (map typesym (.getParameterTypes constructor)))
   (vec (map typesym (.getExceptionTypes constructor)))
   (parse-flags (.getModifiers constructor) :method)))

(§ defn- declared-constructors
  "Return a set of the declared constructors of class as a Cloiure map."
  [^Class cls]
  (set (map
        constructor->map
        (.getDeclaredConstructors cls))))

(§ defrecord Method
  [name return-type declaring-class parameter-types exception-types flags])

(§ defn- method->map
  [^java.lang.reflect.Method method]
  (Method.
   (symbol (.getName method))
   (typesym (.getReturnType method))
   (typesym (.getDeclaringClass method))
   (vec (map typesym (.getParameterTypes method)))
   (vec (map typesym (.getExceptionTypes method)))
   (parse-flags (.getModifiers method) :method)))

(§ defn- declared-methods
  "Return a set of the declared constructors of class as a Cloiure map."
  [^Class cls]
  (set (map
        method->map
        (.getDeclaredMethods cls))))

(§ defrecord Field
  [name type declaring-class flags])

(§ defn- field->map
  [^java.lang.reflect.Field field]
  (Field.
   (symbol (.getName field))
   (typesym (.getType field))
   (typesym (.getDeclaringClass field))
   (parse-flags (.getModifiers field) :field)))

(§ defn- declared-fields
  "Return a set of the declared fields of class as a Cloiure map."
  [^Class cls]
  (set (map
        field->map
        (.getDeclaredFields cls))))

(§ deftype JavaReflector [classloader]
  Reflector
  (do-reflect [_ typeref]
           (let [cls (cloiure.lang.RT/classForName (typename typeref) false classloader)]
             {:bases (not-empty (set (map typesym (bases cls))))
              :flags (parse-flags (.getModifiers cls) :class)
              :members (set/union (declared-fields cls)
                                  (declared-methods cls)
                                  (declared-constructors cls))})))

(§ def ^:private default-reflector
     (JavaReflector. (.getContextClassLoader (Thread/currentThread))))

(§ defn- parse-method-descriptor
  [^String md]
  {:parameter-types (vec (map typesym (Type/getArgumentTypes md)))
   :return-type (typesym (Type/getReturnType md))})

(§ defprotocol ClassResolver
  (^InputStream resolve-class [this name]
                "Given a class name, return that typeref's class bytes as an InputStream."))

(§ extend-protocol ClassResolver
  cloiure.lang.Fn
  (resolve-class [this typeref] (this typeref))

  ClassLoader
  (resolve-class [this typeref]
                 (.getResourceAsStream this (resource-name typeref))))

(§ deftype AsmReflector [class-resolver]
  Reflector
  (do-reflect [_ typeref]
    (with-open [is (resolve-class class-resolver typeref)]
      (let [class-symbol (typesym typeref)
            r (ClassReader. is)
            result (atom {:bases #{} :flags #{} :members #{}})]
        (.accept
         r
         (proxy
          [ClassVisitor]
          [Opcodes/ASM4]
          (visit [version access name signature superName interfaces]
                 (let [flags (parse-flags access :class)
                       ;; ignore java.lang.Object on interfaces to match reflection
                       superName (if (and (flags :interface)
                                          (= superName "java/lang/Object"))
                                   nil
                                   superName)
                       bases (->> (cons superName interfaces)
                                  (remove nil?)
                                  (map internal-name->class-symbol)
                                  (map symbol)
                                  (set)
                                  (not-empty))]
                   (swap! result merge {:bases bases
                                        :flags flags})))
          (visitAnnotation [desc visible])
          (visitSource [name debug])
          (visitInnerClass [name outerName innerName access])
          (visitField [access name desc signature value]
                      (swap! result update :members (fnil conj #{})
                             (Field. (symbol name)
                                     (field-descriptor->class-symbol desc)
                                     class-symbol
                                     (parse-flags access :field)))
                      nil)
          (visitMethod [access name desc signature exceptions]
                       (when-not (= name "<clinit>")
                         (let [constructor? (= name "<init>")]
                           (swap! result update :members (fnil conj #{})
                                  (let [{:keys [parameter-types return-type]} (parse-method-descriptor desc)
                                        flags (parse-flags access :method)]
                                    (if constructor?
                                      (Constructor. class-symbol
                                                    class-symbol
                                                    parameter-types
                                                    (vec (map internal-name->class-symbol exceptions))
                                                    flags)
                                      (Method. (symbol name)
                                               return-type
                                               class-symbol
                                               parameter-types
                                               (vec (map internal-name->class-symbol exceptions))
                                               flags))))))
                       nil)
          (visitEnd [])
          ) 0)
        @result))))
(§ ns
  ^{:author "Chris Houser, Christophe Grand, Stephen Gilardi, Michel Salim"
    :doc "Utilities meant to be used interactively at the REPL"}
  cloiure.repl
  (:require [cloiure.spec.alpha :as spec])
  (:import (java.io LineNumberReader InputStreamReader PushbackReader)
           (cloiure.lang RT Reflector)))

(§ def ^:private special-doc-map
  '{. {:url "java_interop#dot"
       :forms [(.instanceMember instance args*)
               (.instanceMember Classname args*)
               (Classname/staticMethod args*)
               Classname/staticField]
       :doc "The instance member form works for both fields and methods.
  They all expand into calls to the dot operator at macroexpansion time."}
    def {:forms [(def symbol doc-string? init?)]
         :doc "Creates and interns a global var with the name
  of symbol in the current namespace (*ns*) or locates such a var if
  it already exists.  If init is supplied, it is evaluated, and the
  root binding of the var is set to the resulting value.  If init is
  not supplied, the root binding of the var is unaffected."}
    do {:forms [(do exprs*)]
        :doc "Evaluates the expressions in order and returns the value of
  the last. If no expressions are supplied, returns nil."}
    if {:forms [(if test then else?)]
        :doc "Evaluates test. If not the singular values nil or false,
  evaluates and yields then, otherwise, evaluates and yields else. If
  else is not supplied it defaults to nil."}
    monitor-enter {:forms [(monitor-enter x)]
                   :doc "Synchronization primitive that should be avoided
  in user code. Use the 'locking' macro."}
    monitor-exit {:forms [(monitor-exit x)]
                  :doc "Synchronization primitive that should be avoided
  in user code. Use the 'locking' macro."}
    new {:forms [(Classname. args*) (new Classname args*)]
         :url "java_interop#new"
         :doc "The args, if any, are evaluated from left to right, and
  passed to the constructor of the class named by Classname. The
  constructed object is returned."}
    quote {:forms [(quote form)]
           :doc "Yields the unevaluated form."}
    recur {:forms [(recur exprs*)]
           :doc "Evaluates the exprs in order, then, in parallel, rebinds
  the bindings of the recursion point to the values of the exprs.
  Execution then jumps back to the recursion point, a loop or fn method."}
    set! {:forms[(set! var-symbol expr)
                 (set! (. instance-expr instanceFieldName-symbol) expr)
                 (set! (. Classname-symbol staticFieldName-symbol) expr)]
          :url "vars#set"
          :doc "Used to set thread-local-bound vars, Java object instance
fields, and Java class static fields."}
    throw {:forms [(throw expr)]
           :doc "The expr is evaluated and thrown, therefore it should
  yield an instance of some derivee of Throwable."}
    try {:forms [(try expr* catch-clause* finally-clause?)]
         :doc "catch-clause => (catch classname name expr*)
  finally-clause => (finally expr*)

  Catches and handles Java exceptions."}
    var {:forms [(var symbol)]
         :doc "The symbol must resolve to a var, and the Var object
itself (not its value) is returned. The reader macro #'x expands to (var x)."}})

(§ defn- special-doc [name-symbol]
  (assoc (or (special-doc-map name-symbol) (meta (resolve name-symbol)))
         :name name-symbol
         :special-form true))

(§ defn- namespace-doc [nspace]
  (assoc (meta nspace) :name (ns-name nspace)))

(§ defn- print-doc [{n :ns
                   nm :name
                   :keys [forms arglists special-form doc url macro spec]
                   :as m}]
  (println "-------------------------")
  (println (or spec (str (when n (str (ns-name n) "/")) nm)))
  (when forms
    (doseq [f forms]
      (print "  ")
      (prn f)))
  (when arglists
    (prn arglists))
  (cond
    special-form
    (do
      (println "Special Form")
      (println " " doc)
      (if (contains? m :url)
        (when url
          (println (str "\n  Please see http://clojure.org/" url)))
        (println (str "\n  Please see http://clojure.org/special_forms#" nm))))
    macro
    (println "Macro")
    spec
    (println "Spec"))
  (when doc (println " " doc))
  (when n
    (when-let [fnspec (spec/get-spec (symbol (str (ns-name n)) (name nm)))]
      (println "Spec")
      (doseq [role [:args :ret :fn]]
        (when-let [spec (get fnspec role)]
          (println " " (str (name role) ":") (spec/describe spec)))))))

(§ defn find-doc
  "Prints documentation for any var whose documentation or name
 contains a match for re-string-or-pattern"
  {:added "1.0"}
  [re-string-or-pattern]
    (let [re (re-pattern re-string-or-pattern)
          ms (concat (mapcat #(sort-by :name (map meta (vals (ns-interns %))))
                             (all-ns))
                     (map namespace-doc (all-ns))
                     (map special-doc (keys special-doc-map)))]
      (doseq [m ms
              :when (and (:doc m)
                         (or (re-find (re-matcher re (:doc m)))
                             (re-find (re-matcher re (str (:name m))))))]
               (print-doc m))))

(§ defmacro doc
  "Prints documentation for a var or special form given its name,
   or for a spec if given a keyword"
  {:added "1.0"}
  [name]
  (if-let [special-name ('{& fn catch try finally try} name)]
    `(#'print-doc (#'special-doc '~special-name))
    (cond
      (special-doc-map name) `(#'print-doc (#'special-doc '~name))
      (keyword? name) `(#'print-doc {:spec '~name :doc '~(spec/describe name)})
      (find-ns name) `(#'print-doc (#'namespace-doc (find-ns '~name)))
      (resolve name) `(#'print-doc (meta (var ~name))))))

;; ----------------------------------------------------------------------
;; Examine Cloiure functions (Vars, really)

(§ defn source-fn
  "Returns a string of the source code for the given symbol, if it can
  find it.  This requires that the symbol resolve to a Var defined in
  a namespace for which the .cli is in the classpath.  Returns nil if
  it can't find the source.  For most REPL usage, 'source' is more
  convenient.

  Example: (source-fn 'filter)"
  [x]
  (when-let [v (resolve x)]
    (when-let [filepath (:file (meta v))]
      (when-let [strm (.getResourceAsStream (RT/baseLoader) filepath)]
        (with-open [rdr (LineNumberReader. (InputStreamReader. strm))]
          (dotimes [_ (dec (:line (meta v)))] (.readLine rdr))
          (let [text (StringBuilder.)
                pbr (proxy [PushbackReader] [rdr]
                      (read [] (let [i (proxy-super read)]
                                 (.append text (char i))
                                 i)))
                read-opts (if (.endsWith ^String filepath "clic") {:read-cond :allow} {})]
            (if (= :unknown *read-eval*)
              (throw (IllegalStateException. "Unable to read source while *read-eval* is :unknown."))
              (read read-opts (PushbackReader. pbr)))
            (str text)))))))

(§ defmacro source
  "Prints the source code for the given symbol, if it can find it.
  This requires that the symbol resolve to a Var defined in a
  namespace for which the .cli is in the classpath.

  Example: (source filter)"
  [n]
  `(println (or (source-fn '~n) (str "Source not found"))))

(§ defn apropos
  "Given a regular expression or stringable thing, return a seq of all
public definitions in all currently-loaded namespaces that match the
str-or-pattern."
  [str-or-pattern]
  (let [matches? (if (instance? java.util.regex.Pattern str-or-pattern)
                   #(re-find str-or-pattern (str %))
                   #(.contains (str %) (str str-or-pattern)))]
    (sort (mapcat (fn [ns]
                    (let [ns-name (str ns)]
                      (map #(symbol ns-name (str %))
                           (filter matches? (keys (ns-publics ns))))))
                  (all-ns)))))

(§ defn dir-fn
  "Returns a sorted seq of symbols naming public vars in
  a namespace or namespace alias. Looks for aliases in *ns*"
  [ns]
  (sort (map first (ns-publics (the-ns (get (ns-aliases *ns*) ns ns))))))

(§ defmacro dir
  "Prints a sorted directory of public vars in a namespace"
  [nsname]
  `(doseq [v# (dir-fn '~nsname)]
     (println v#)))

(§ defn demunge
  "Given a string representation of a fn class,
  as in a stack trace element, returns a readable version."
  {:added "1.3"}
  [fn-name]
  (cloiure.lang.Compiler/demunge fn-name))

(§ defn root-cause
  "Returns the initial cause of an exception or error by peeling off all of
  its wrappers"
  {:added "1.3"}
  [^Throwable t]
  (loop [cause t]
    (if (and (instance? cloiure.lang.Compiler$CompilerException cause)
             (not= (.source ^cloiure.lang.Compiler$CompilerException cause) "NO_SOURCE_FILE"))
      cause
      (if-let [cause (.getCause cause)]
        (recur cause)
        cause))))

(§ defn stack-element-str
  "Returns a (possibly unmunged) string representation of a StackTraceElement"
  {:added "1.3"}
  [^StackTraceElement el]
  (let [file (.getFileName el)
        cloiure-fn? (and file (or (.endsWith file ".cli")
                                  (.endsWith file ".clic")
                                  (= file "NO_SOURCE_FILE")))]
    (str (if cloiure-fn?
           (demunge (.getClassName el))
           (str (.getClassName el) "." (.getMethodName el)))
         " (" (.getFileName el) ":" (.getLineNumber el) ")")))

(§ defn pst
  "Prints a stack trace of the exception, to the depth requested. If none supplied, uses the root cause of the
  most recent repl exception (*e), and a depth of 12."
  {:added "1.3"}
  ([] (pst 12))
  ([e-or-depth]
     (if (instance? Throwable e-or-depth)
       (pst e-or-depth 12)
       (when-let [e *e]
         (pst (root-cause e) e-or-depth))))
  ([^Throwable e depth]
     (binding [*out* *err*]
       (println (str (-> e class .getSimpleName) " "
                     (.getMessage e)
                     (when-let [info (ex-data e)] (str " " (pr-str info)))))
       (let [st (.getStackTrace e)
             cause (.getCause e)]
         (doseq [el (take depth
                          (remove #(#{"cloiure.lang.RestFn" "cloiure.lang.AFn"} (.getClassName %))
                                  st))]
           (println (str \tab (stack-element-str el))))
         (when cause
           (println "Caused by:")
           (pst cause (min depth
                           (+ 2 (- (count (.getStackTrace cause))
                                   (count st))))))))))

;; ----------------------------------------------------------------------
;; Handle Ctrl-C keystrokes

(§ defn thread-stopper
  "Returns a function that takes one arg and uses that as an exception message
  to stop the given thread.  Defaults to the current thread"
  ([] (thread-stopper (Thread/currentThread)))
  ([thread] (fn [msg] (.stop thread (Error. msg)))))

(§ defn set-break-handler!
  "Register INT signal handler.  After calling this, Ctrl-C will cause
  the given function f to be called with a single argument, the signal.
  Uses thread-stopper if no function given."
  ([] (set-break-handler! (thread-stopper)))
  ([f]
   (sun.misc.Signal/handle
     (sun.misc.Signal. "INT")
     (proxy [sun.misc.SignalHandler] []
       (handle [signal]
         (f (str "-- caught signal " signal)))))))

#_(ns ^{:doc "Set operations such as union/intersection."
       :author "Rich Hickey"}
       cloiure.set)

(§ defn- bubble-max-key
  "Move a maximal element of coll according to fn k (which returns a
  number) to the front of coll."
  [k coll]
  (let [max (apply max-key k coll)]
    (cons max (remove #(identical? max %) coll))))

(§ defn union
  "Return a set that is the union of the input sets"
  {:added "1.0"}
  ([] #{})
  ([s1] s1)
  ([s1 s2]
     (if (< (count s1) (count s2))
       (reduce conj s2 s1)
       (reduce conj s1 s2)))
  ([s1 s2 & sets]
     (let [bubbled-sets (bubble-max-key count (conj sets s2 s1))]
       (reduce into (first bubbled-sets) (rest bubbled-sets)))))

(§ defn intersection
  "Return a set that is the intersection of the input sets"
  {:added "1.0"}
  ([s1] s1)
  ([s1 s2]
     (if (< (count s2) (count s1))
       (recur s2 s1)
       (reduce (fn [result item]
                   (if (contains? s2 item)
                     result
                     (disj result item)))
               s1 s1)))
  ([s1 s2 & sets]
     (let [bubbled-sets (bubble-max-key #(- (count %)) (conj sets s2 s1))]
       (reduce intersection (first bubbled-sets) (rest bubbled-sets)))))

(§ defn difference
  "Return a set that is the first set without elements of the remaining sets"
  {:added "1.0"}
  ([s1] s1)
  ([s1 s2]
     (if (< (count s1) (count s2))
       (reduce (fn [result item]
                   (if (contains? s2 item)
                     (disj result item)
                     result))
               s1 s1)
       (reduce disj s1 s2)))
  ([s1 s2 & sets]
     (reduce difference s1 (conj sets s2))))

(§ defn select
  "Returns a set of the elements for which pred is true"
  {:added "1.0"}
  [pred xset]
    (reduce (fn [s k] (if (pred k) s (disj s k)))
            xset xset))

(§ defn project
  "Returns a rel of the elements of xrel with only the keys in ks"
  {:added "1.0"}
  [xrel ks]
  (with-meta (set (map #(select-keys % ks) xrel)) (meta xrel)))

(§ defn rename-keys
  "Returns the map with the keys in kmap renamed to the vals in kmap"
  {:added "1.0"}
  [map kmap]
    (reduce
     (fn [m [old new]]
       (if (contains? map old)
         (assoc m new (get map old))
         m))
     (apply dissoc map (keys kmap)) kmap))

(§ defn rename
  "Returns a rel of the maps in xrel with the keys in kmap renamed to the vals in kmap"
  {:added "1.0"}
  [xrel kmap]
  (with-meta (set (map #(rename-keys % kmap) xrel)) (meta xrel)))

(§ defn index
  "Returns a map of the distinct values of ks in the xrel mapped to a
  set of the maps in xrel with the corresponding values of ks."
  {:added "1.0"}
  [xrel ks]
    (reduce
     (fn [m x]
       (let [ik (select-keys x ks)]
         (assoc m ik (conj (get m ik #{}) x))))
     {} xrel))

(§ defn map-invert
  "Returns the map with the vals mapped to the keys."
  {:added "1.0"}
  [m] (reduce (fn [m [k v]] (assoc m v k)) {} m))

(§ defn join
  "When passed 2 rels, returns the rel corresponding to the natural
  join. When passed an additional keymap, joins on the corresponding
  keys."
  {:added "1.0"}
  ([xrel yrel] ;; natural join
   (if (and (seq xrel) (seq yrel))
     (let [ks (intersection (set (keys (first xrel))) (set (keys (first yrel))))
           [r s] (if (<= (count xrel) (count yrel))
                   [xrel yrel]
                   [yrel xrel])
           idx (index r ks)]
       (reduce (fn [ret x]
                 (let [found (idx (select-keys x ks))]
                   (if found
                     (reduce #(conj %1 (merge %2 x)) ret found)
                     ret)))
               #{} s))
     #{}))
  ([xrel yrel km] ;; arbitrary key mapping
   (let [[r s k] (if (<= (count xrel) (count yrel))
                   [xrel yrel (map-invert km)]
                   [yrel xrel km])
         idx (index r (vals k))]
     (reduce (fn [ret x]
               (let [found (idx (rename-keys (select-keys x (keys k)) k))]
                 (if found
                   (reduce #(conj %1 (merge %2 x)) ret found)
                   ret)))
             #{} s))))

(§ defn subset?
  "Is set1 a subset of set2?"
  {:added "1.2",
   :tag Boolean}
  [set1 set2]
  (and (<= (count set1) (count set2))
       (every? #(contains? set2 %) set1)))

(§ defn superset?
  "Is set1 a superset of set2?"
  {:added "1.2",
   :tag Boolean}
  [set1 set2]
  (and (>= (count set1) (count set2))
       (every? #(contains? set1 %) set2)))

#_(ns cloiure.spec.alpha
  (:refer-cloiure :exclude [+ * and assert or cat def keys merge])
  (:require [cloiure.walk :as walk]
            [cloiure.spec.gen.alpha :as gen]
            [cloiure.string :as str]))

(§ alias 'c 'cloiure.core)

(§ set! *warn-on-reflection* true)

(§ def ^:dynamic *recursion-limit*
  "A soft limit on how many times a branching spec (or/alt/*/opt-keys/multi-spec)
  can be recursed through during generation. After this a
  non-recursive branch will be chosen."
  4)

(§ def ^:dynamic *fspec-iterations*
  "The number of times an anonymous fn specified by fspec will be (generatively) tested during conform"
  21)

(§ def ^:dynamic *coll-check-limit*
  "The number of elements validated in a collection spec'ed with 'every'"
  101)

(§ def ^:dynamic *coll-error-limit*
  "The number of errors reported by explain in a collection spec'ed with 'every'"
  20)

(§ defprotocol Spec
  (conform* [spec x])
  (unform* [spec y])
  (explain* [spec path via in x])
  (gen* [spec overrides path rmap])
  (with-gen* [spec gfn])
  (describe* [spec]))

(§ defonce ^:private registry-ref (atom {}))

(§ defn- deep-resolve [reg k]
  (loop [spec k]
    (if (ident? spec)
      (recur (get reg spec))
      spec)))

(§ defn- reg-resolve
  "returns the spec/regex at end of alias chain starting with k, nil if not found, k if k not ident"
  [k]
  (if (ident? k)
    (let [reg @registry-ref
          spec (get reg k)]
      (if-not (ident? spec)
        spec
        (deep-resolve reg spec)))
    k))

(§ defn- reg-resolve!
  "returns the spec/regex at end of alias chain starting with k, throws if not found, k if k not ident"
  [k]
  (if (ident? k)
    (c/or (reg-resolve k)
          (throw (Exception. (str "Unable to resolve spec: " k))))
    k))

(§ defn spec?
  "returns x if x is a spec object, else logical false"
  [x]
  (when (instance? cloiure.spec.alpha.Spec x)
    x))

(§ defn regex?
  "returns x if x is a (cloiure.spec) regex op, else logical false"
  [x]
  (c/and (::op x) x))

(§ defn- with-name [spec name]
  (cond
   (ident? spec) spec
   (regex? spec) (assoc spec ::name name)

   (instance? cloiure.lang.IObj spec)
   (with-meta spec (assoc (meta spec) ::name name))))

(§ defn- spec-name [spec]
  (cond
   (ident? spec) spec

   (regex? spec) (::name spec)

   (instance? cloiure.lang.IObj spec)
   (-> (meta spec) ::name)))

(§ declare spec-impl)
(§ declare regex-spec-impl)

(§ defn- maybe-spec
  "spec-or-k must be a spec, regex or resolvable kw/sym, else returns nil."
  [spec-or-k]
  (let [s (c/or (c/and (ident? spec-or-k) (reg-resolve spec-or-k))
                (spec? spec-or-k)
                (regex? spec-or-k)
                nil)]
    (if (regex? s)
      (with-name (regex-spec-impl s nil) (spec-name s))
      s)))

(§ defn- the-spec
  "spec-or-k must be a spec, regex or kw/sym, else returns nil. Throws if unresolvable kw/sym"
  [spec-or-k]
  (c/or (maybe-spec spec-or-k)
        (when (ident? spec-or-k)
          (throw (Exception. (str "Unable to resolve spec: " spec-or-k))))))

(§ defprotocol Specize
  (specize* [_] [_ form]))

(§ extend-protocol Specize
  cloiure.lang.Keyword
  (specize* ([k] (specize* (reg-resolve! k)))
            ([k _] (specize* (reg-resolve! k))))

  cloiure.lang.Symbol
  (specize* ([s] (specize* (reg-resolve! s)))
            ([s _] (specize* (reg-resolve! s))))

  Object
  (specize* ([o] (spec-impl ::unknown o nil nil))
            ([o form] (spec-impl form o nil nil))))

(§ defn- specize
  ([s] (c/or (spec? s) (specize* s)))
  ([s form] (c/or (spec? s) (specize* s form))))

(§ defn invalid?
  "tests the validity of a conform return value"
  [ret]
  (identical? ::invalid ret))

(§ defn conform
  "Given a spec and a value, returns :cloiure.spec.alpha/invalid
        if value does not match spec, else the (possibly destructured) value."
  [spec x]
  (conform* (specize spec) x))

(§ defn unform
  "Given a spec and a value created by or compliant with a call to
  'conform' with the same spec, returns a value with all conform
  destructuring undone."
  [spec x]
  (unform* (specize spec) x))

(§ defn form
  "returns the spec as data"
  [spec]
  ;;TODO - incorporate gens
  (describe* (specize spec)))

(§ defn abbrev [form]
  (cond
   (seq? form)
   (walk/postwalk (fn [form]
                    (cond
                     (c/and (symbol? form) (namespace form))
                     (-> form name symbol)

                     (c/and (seq? form) (= 'fn (first form)) (= '[%] (second form)))
                     (last form)

                     :else form))
                  form)

   (c/and (symbol? form) (namespace form))
   (-> form name symbol)

   :else form))

(§ defn describe
  "returns an abbreviated description of the spec as data"
  [spec]
  (abbrev (form spec)))

(§ defn with-gen
  "Takes a spec and a no-arg, generator-returning fn and returns a version of that spec that uses that generator"
  [spec gen-fn]
  (let [spec (reg-resolve spec)]
    (if (regex? spec)
      (assoc spec ::gfn gen-fn)
      (with-gen* (specize spec) gen-fn))))

(§ defn explain-data* [spec path via in x]
  (let [probs (explain* (specize spec) path via in x)]
    (when-not (empty? probs)
      {::problems probs
       ::spec spec
       ::value x})))

(§ defn explain-data
  "Given a spec and a value x which ought to conform, returns nil if x
  conforms, else a map with at least the key ::problems whose value is
  a collection of problem-maps, where problem-map has at least :path :pred and :val
  keys describing the predicate and the value that failed at that
  path."
  [spec x]
  (explain-data* spec [] (if-let [name (spec-name spec)] [name] []) [] x))

(§ defn explain-printer
  "Default printer for explain-data. nil indicates a successful validation."
  [ed]
  (if ed
    (let [problems (sort-by #(- (count (:path %))) (::problems ed))]
      ;;(prn {:ed ed})
      (doseq [{:keys [path pred val reason via in] :as prob} problems]
        (when-not (empty? in)
          (print "In:" (pr-str in) ""))
        (print "val: ")
        (pr val)
        (print " fails")
        (when-not (empty? via)
          (print " spec:" (pr-str (last via))))
        (when-not (empty? path)
          (print " at:" (pr-str path)))
        (print " predicate: ")
        (pr (abbrev pred))
        (when reason (print ", " reason))
        (doseq [[k v] prob]
          (when-not (#{:path :pred :val :reason :via :in} k)
            (print "\n\t" (pr-str k) " ")
            (pr v)))
        (newline)))
    (println "Success!")))

(§ def ^:dynamic *explain-out* explain-printer)

(§ defn explain-out
  "Prints explanation data (per 'explain-data') to *out* using the printer in *explain-out*,
   by default explain-printer."
  [ed]
  (*explain-out* ed))

(§ defn explain
  "Given a spec and a value that fails to conform, prints an explanation to *out*."
  [spec x]
  (explain-out (explain-data spec x)))

(§ defn explain-str
  "Given a spec and a value that fails to conform, returns an explanation as a string."
  [spec x]
  (with-out-str (explain spec x)))

(§ declare valid?)

(§ defn- gensub
  [spec overrides path rmap form]
  ;;(prn {:spec spec :over overrides :path path :form form})
  (let [spec (specize spec)]
    (if-let [g (c/or (when-let [gfn (c/or (get overrides (c/or (spec-name spec) spec))
                                          (get overrides path))]
                       (gfn))
                     (gen* spec overrides path rmap))]
      (gen/such-that #(valid? spec %) g 100)
      (let [abbr (abbrev form)]
        (throw (ex-info (str "Unable to construct gen at: " path " for: " abbr)
                        {::path path ::form form ::failure :no-gen}))))))

(§ defn gen
  "Given a spec, returns the generator for it, or throws if none can
  be constructed. Optionally an overrides map can be provided which
  should map spec names or paths (vectors of keywords) to no-arg
  generator-creating fns. These will be used instead of the generators at those
  names/paths. Note that parent generator (in the spec or overrides
  map) will supersede those of any subtrees. A generator for a regex
  op must always return a sequential collection (i.e. a generator for
  s/? should return either an empty sequence/vector or a
  sequence/vector with one item in it)"
  ([spec] (gen spec nil))
  ([spec overrides] (gensub spec overrides [] {::recursion-limit *recursion-limit*} spec)))

(§ defn- ->sym
  "Returns a symbol from a symbol or var"
  [x]
  (if (var? x)
    (let [^cloiure.lang.Var v x]
      (symbol (str (.name (.ns v)))
              (str (.sym v))))
    x))

(§ defn- unfn [expr]
  (if (c/and (seq? expr)
             (symbol? (first expr))
             (= "fn*" (name (first expr))))
    (let [[[s] & form] (rest expr)]
      (conj (walk/postwalk-replace {s '%} form) '[%] 'fn))
    expr))

(§ defn- res [form]
  (cond
   (keyword? form) form
   (symbol? form) (c/or (-> form resolve ->sym) form)
   (sequential? form) (walk/postwalk #(if (symbol? %) (res %) %) (unfn form))
   :else form))

(§ defn ^:skip-wiki def-impl
  "Do not call this directly, use 'def'"
  [k form spec]
  (c/assert (c/and (ident? k) (namespace k)) "k must be namespaced keyword or resolvable symbol")
  (let [spec (if (c/or (spec? spec) (regex? spec) (get @registry-ref spec))
               spec
               (spec-impl form spec nil nil))]
    (swap! registry-ref assoc k (with-name spec k))
    k))

(§ defn- ns-qualify
  "Qualify symbol s by resolving it or using the current *ns*."
  [s]
  (if-let [ns-sym (some-> s namespace symbol)]
    (c/or (some-> (get (ns-aliases *ns*) ns-sym) str (symbol (name s)))
          s)
    (symbol (str (.name *ns*)) (str s))))

(§ defmacro def
  "Given a namespace-qualified keyword or resolvable symbol k, and a
  spec, spec-name, predicate or regex-op makes an entry in the
  registry mapping k to the spec"
  [k spec-form]
  (let [k (if (symbol? k) (ns-qualify k) k)]
    `(def-impl '~k '~(res spec-form) ~spec-form)))

(§ defn registry
  "returns the registry map, prefer 'get-spec' to lookup a spec by name"
  []
  @registry-ref)

(§ defn get-spec
  "Returns spec registered for keyword/symbol/var k, or nil."
  [k]
  (get (registry) (if (keyword? k) k (->sym k))))

(§ defmacro spec
  "Takes a single predicate form, e.g. can be the name of a predicate,
  like even?, or a fn literal like #(< % 42). Note that it is not
  generally necessary to wrap predicates in spec when using the rest
  of the spec macros, only to attach a unique generator

  Can also be passed the result of one of the regex ops -
  cat, alt, *, +, ?, in which case it will return a regex-conforming
  spec, useful when nesting an independent regex.
  ---

  Optionally takes :gen generator-fn, which must be a fn of no args that
  returns a test.check generator.

  Returns a spec."
  [form & {:keys [gen]}]
  (when form
    `(spec-impl '~(res form) ~form ~gen nil)))

(§ defmacro multi-spec
  "Takes the name of a spec/predicate-returning multimethod and a
  tag-restoring keyword or fn (retag).  Returns a spec that when
  conforming or explaining data will pass it to the multimethod to get
  an appropriate spec. You can e.g. use multi-spec to dynamically and
  extensibly associate specs with 'tagged' data (i.e. data where one
  of the fields indicates the shape of the rest of the structure).

  (defmulti mspec :tag)

  The methods should ignore their argument and return a predicate/spec:
  (defmethod mspec :int [_] (s/keys :req-un [::tag ::i]))

  retag is used during generation to retag generated values with
  matching tags. retag can either be a keyword, at which key the
  dispatch-tag will be assoc'ed, or a fn of generated value and
  dispatch-tag that should return an appropriately retagged value.

  Note that because the tags themselves comprise an open set,
  the tag key spec cannot enumerate the values, but can e.g.
  test for keyword?.

  Note also that the dispatch values of the multimethod will be
  included in the path, i.e. in reporting and gen overrides, even
  though those values are not evident in the spec.
"
  [mm retag]
  `(multi-spec-impl '~(res mm) (var ~mm) ~retag))

(§ defmacro keys
  "Creates and returns a map validating spec. :req and :opt are both
  vectors of namespaced-qualified keywords. The validator will ensure
  the :req keys are present. The :opt keys serve as documentation and
  may be used by the generator.

  The :req key vector supports 'and' and 'or' for key groups:

  (s/keys :req [::x ::y (or ::secret (and ::user ::pwd))] :opt [::z])

  There are also -un versions of :req and :opt. These allow
  you to connect unqualified keys to specs.  In each case, fully
  qualfied keywords are passed, which name the specs, but unqualified
  keys (with the same name component) are expected and checked at
  conform-time, and generated during gen:

  (s/keys :req-un [:my.ns/x :my.ns/y])

  The above says keys :x and :y are required, and will be validated
  and generated by specs (if they exist) named :my.ns/x :my.ns/y
  respectively.

  In addition, the values of *all* namespace-qualified keys will be validated
  (and possibly destructured) by any registered specs. Note: there is
  no support for inline value specification, by design.

  Optionally takes :gen generator-fn, which must be a fn of no args that
  returns a test.check generator."
  [& {:keys [req req-un opt opt-un gen]}]
  (let [unk #(-> % name keyword)
        req-keys (filterv keyword? (flatten req))
        req-un-specs (filterv keyword? (flatten req-un))
        _ (c/assert (every? #(c/and (keyword? %) (namespace %)) (concat req-keys req-un-specs opt opt-un))
                    "all keys must be namespace-qualified keywords")
        req-specs (into req-keys req-un-specs)
        req-keys (into req-keys (map unk req-un-specs))
        opt-keys (into (vec opt) (map unk opt-un))
        opt-specs (into (vec opt) opt-un)
        gx (gensym)
        parse-req (fn [rk f]
                    (map (fn [x]
                           (if (keyword? x)
                             `(contains? ~gx ~(f x))
                             (walk/postwalk
                               (fn [y] (if (keyword? y) `(contains? ~gx ~(f y)) y))
                               x)))
                         rk))
        pred-exprs [`(map? ~gx)]
        pred-exprs (into pred-exprs (parse-req req identity))
        pred-exprs (into pred-exprs (parse-req req-un unk))
        keys-pred `(fn* [~gx] (c/and ~@pred-exprs))
        pred-exprs (mapv (fn [e] `(fn* [~gx] ~e)) pred-exprs)
        pred-forms (walk/postwalk res pred-exprs)]
    ;; `(map-spec-impl ~req-keys '~req ~opt '~pred-forms ~pred-exprs ~gen)
    `(map-spec-impl {:req '~req :opt '~opt :req-un '~req-un :opt-un '~opt-un
                     :req-keys '~req-keys :req-specs '~req-specs
                     :opt-keys '~opt-keys :opt-specs '~opt-specs
                     :pred-forms '~pred-forms
                     :pred-exprs ~pred-exprs
                     :keys-pred ~keys-pred
                     :gfn ~gen})))

(§ defmacro or
  "Takes key+pred pairs, e.g.

  (s/or :even even? :small #(< % 42))

  Returns a destructuring spec that returns a map entry containing the
  key of the first matching pred and the corresponding value. Thus the
  'key' and 'val' functions can be used to refer generically to the
  components of the tagged return."
  [& key-pred-forms]
  (let [pairs (partition 2 key-pred-forms)
        keys (mapv first pairs)
        pred-forms (mapv second pairs)
        pf (mapv res pred-forms)]
    (c/assert (c/and (even? (count key-pred-forms)) (every? keyword? keys)) "spec/or expects k1 p1 k2 p2..., where ks are keywords")
    `(or-spec-impl ~keys '~pf ~pred-forms nil)))

(§ defmacro and
  "Takes predicate/spec-forms, e.g.

  (s/and even? #(< % 42))

  Returns a spec that returns the conformed value. Successive
  conformed values propagate through rest of predicates."
  [& pred-forms]
  `(and-spec-impl '~(mapv res pred-forms) ~(vec pred-forms) nil))

(§ defmacro merge
  "Takes map-validating specs (e.g. 'keys' specs) and
  returns a spec that returns a conformed map satisfying all of the
  specs.  Unlike 'and', merge can generate maps satisfying the
  union of the predicates."
  [& pred-forms]
  `(merge-spec-impl '~(mapv res pred-forms) ~(vec pred-forms) nil))

(§ defn- res-kind
  [opts]
  (let [{kind :kind :as mopts} opts]
    (->>
      (if kind
        (assoc mopts :kind `~(res kind))
        mopts)
      (mapcat identity))))

(§ defmacro every
  "takes a pred and validates collection elements against that pred.

  Note that 'every' does not do exhaustive checking, rather it samples
  *coll-check-limit* elements. Nor (as a result) does it do any
  conforming of elements. 'explain' will report at most *coll-error-limit*
  problems.  Thus 'every' should be suitable for potentially large
  collections.

  Takes several kwargs options that further constrain the collection:

  :kind - a pred/spec that the collection type must satisfy, e.g. vector?
        (default nil) Note that if :kind is specified and :into is
        not, this pred must generate in order for every to generate.
  :count - specifies coll has exactly this count (default nil)
  :min-count, :max-count - coll has count (<= min-count count max-count) (defaults nil)
  :distinct - all the elements are distinct (default nil)

  And additional args that control gen

  :gen-max - the maximum coll size to generate (default 20)
  :into - one of [], (), {}, #{} - the default collection to generate into
      (default: empty coll as generated by :kind pred if supplied, else [])

  Optionally takes :gen generator-fn, which must be a fn of no args that
  returns a test.check generator

  See also - coll-of, every-kv
"
  [pred & {:keys [into kind count max-count min-count distinct gen-max gen] :as opts}]
  (let [desc (::describe opts)
        nopts (-> opts
                (dissoc :gen ::describe)
                (assoc ::kind-form `'~(res (:kind opts))
                       ::describe (c/or desc `'(every ~(res pred) ~@(res-kind opts)))))
        gx (gensym)
        cpreds (cond-> [(list (c/or kind `coll?) gx)]
                       count (conj `(= ~count (bounded-count ~count ~gx)))

                       (c/or min-count max-count)
                       (conj `(<= (c/or ~min-count 0)
                                   (bounded-count (if ~max-count (inc ~max-count) ~min-count) ~gx)
                                   (c/or ~max-count Integer/MAX_VALUE)))

                       distinct
                       (conj `(c/or (empty? ~gx) (apply distinct? ~gx))))]
    `(every-impl '~pred ~pred ~(assoc nopts ::cpred `(fn* [~gx] (c/and ~@cpreds))) ~gen)))

(§ defmacro every-kv
  "like 'every' but takes separate key and val preds and works on associative collections.

  Same options as 'every', :into defaults to {}

  See also - map-of"

  [kpred vpred & opts]
  (let [desc `(every-kv ~(res kpred) ~(res vpred) ~@(res-kind opts))]
    `(every (tuple ~kpred ~vpred) ::kfn (fn [i# v#] (nth v# 0)) :into {} ::describe '~desc ~@opts)))

(§ defmacro coll-of
  "Returns a spec for a collection of items satisfying pred. Unlike
  'every', coll-of will exhaustively conform every value.

  Same options as 'every'. conform will produce a collection
  corresponding to :into if supplied, else will match the input collection,
  avoiding rebuilding when possible.

  See also - every, map-of"
  [pred & opts]
  (let [desc `(coll-of ~(res pred) ~@(res-kind opts))]
    `(every ~pred ::conform-all true ::describe '~desc ~@opts)))

(§ defmacro map-of
  "Returns a spec for a map whose keys satisfy kpred and vals satisfy
  vpred. Unlike 'every-kv', map-of will exhaustively conform every
  value.

  Same options as 'every', :kind defaults to map?, with the addition of:

  :conform-keys - conform keys as well as values (default false)

  See also - every-kv"
  [kpred vpred & opts]
  (let [desc `(map-of ~(res kpred) ~(res vpred) ~@(res-kind opts))]
    `(every-kv ~kpred ~vpred ::conform-all true :kind map? ::describe '~desc ~@opts)))

(§ defmacro *
  "Returns a regex op that matches zero or more values matching
  pred. Produces a vector of matches iff there is at least one match"
  [pred-form]
  `(rep-impl '~(res pred-form) ~pred-form))

(§ defmacro +
  "Returns a regex op that matches one or more values matching
  pred. Produces a vector of matches"
  [pred-form]
  `(rep+impl '~(res pred-form) ~pred-form))

(§ defmacro ?
  "Returns a regex op that matches zero or one value matching
  pred. Produces a single value (not a collection) if matched."
  [pred-form]
  `(maybe-impl ~pred-form '~(res pred-form)))

(§ defmacro alt
  "Takes key+pred pairs, e.g.

  (s/alt :even even? :small #(< % 42))

  Returns a regex op that returns a map entry containing the key of the
  first matching pred and the corresponding value. Thus the
  'key' and 'val' functions can be used to refer generically to the
  components of the tagged return"
  [& key-pred-forms]
  (let [pairs (partition 2 key-pred-forms)
        keys (mapv first pairs)
        pred-forms (mapv second pairs)
        pf (mapv res pred-forms)]
    (c/assert (c/and (even? (count key-pred-forms)) (every? keyword? keys)) "alt expects k1 p1 k2 p2..., where ks are keywords")
    `(alt-impl ~keys ~pred-forms '~pf)))

(§ defmacro cat
  "Takes key+pred pairs, e.g.

  (s/cat :e even? :o odd?)

  Returns a regex op that matches (all) values in sequence, returning a map
  containing the keys of each pred and the corresponding value."
  [& key-pred-forms]
  (let [pairs (partition 2 key-pred-forms)
        keys (mapv first pairs)
        pred-forms (mapv second pairs)
        pf (mapv res pred-forms)]
    ;;(prn key-pred-forms)
    (c/assert (c/and (even? (count key-pred-forms)) (every? keyword? keys)) "cat expects k1 p1 k2 p2..., where ks are keywords")
    `(cat-impl ~keys ~pred-forms '~pf)))

(§ defmacro &
  "takes a regex op re, and predicates. Returns a regex-op that consumes
  input as per re but subjects the resulting value to the
  conjunction of the predicates, and any conforming they might perform."
  [re & preds]
  (let [pv (vec preds)]
    `(amp-impl ~re ~pv '~(mapv res pv))))

(§ defmacro conformer
  "takes a predicate function with the semantics of conform i.e. it should return either a
  (possibly converted) value or :cloiure.spec.alpha/invalid, and returns a
  spec that uses it as a predicate/conformer. Optionally takes a
  second fn that does unform of result of first"
  ([f] `(spec-impl '(conformer ~(res f)) ~f nil true))
  ([f unf] `(spec-impl '(conformer ~(res f) ~(res unf)) ~f nil true ~unf)))

(§ defmacro fspec
  "takes :args :ret and (optional) :fn kwargs whose values are preds
  and returns a spec whose conform/explain take a fn and validates it
  using generative testing. The conformed value is always the fn itself.

  See 'fdef' for a single operation that creates an fspec and
  registers it, as well as a full description of :args, :ret and :fn

  fspecs can generate functions that validate the arguments and
  fabricate a return value compliant with the :ret spec, ignoring
  the :fn spec if present.

  Optionally takes :gen generator-fn, which must be a fn of no args
  that returns a test.check generator."

  [& {:keys [args ret fn gen] :or {ret `any?}}]
  `(fspec-impl (spec ~args) '~(res args)
               (spec ~ret) '~(res ret)
               (spec ~fn) '~(res fn) ~gen))

(§ defmacro tuple
  "takes one or more preds and returns a spec for a tuple, a vector
  where each element conforms to the corresponding pred. Each element
  will be referred to in paths using its ordinal."
  [& preds]
  (c/assert (not (empty? preds)))
  `(tuple-impl '~(mapv res preds) ~(vec preds)))

(§ defn- macroexpand-check
  [v args]
  (let [fn-spec (get-spec v)]
    (when-let [arg-spec (:args fn-spec)]
      (when (invalid? (conform arg-spec args))
        (let [ed (assoc (explain-data* arg-spec [:args]
                                       (if-let [name (spec-name arg-spec)] [name] []) [] args)
                   ::args args)]
          (throw (ex-info
                   (str
                     "Call to " (->sym v) " did not conform to spec:\n"
                     (with-out-str (explain-out ed)))
                   ed)))))))

(§ defmacro fdef
  "Takes a symbol naming a function, and one or more of the following:

  :args A regex spec for the function arguments as they were a list to be
    passed to apply - in this way, a single spec can handle functions with
    multiple arities
  :ret A spec for the function's return value
  :fn A spec of the relationship between args and ret - the
    value passed is {:args conformed-args :ret conformed-ret} and is
    expected to contain predicates that relate those values

  Qualifies fn-sym with resolve, or using *ns* if no resolution found.
  Registers an fspec in the global registry, where it can be retrieved
  by calling get-spec with the var or fully-qualified symbol.

  Once registered, function specs are included in doc, checked by
  instrument, tested by the runner cloiure.spec.test.alpha/check, and (if
  a macro) used to explain errors during macroexpansion.

  Note that :fn specs require the presence of :args and :ret specs to
  conform values, and so :fn specs will be ignored if :args or :ret
  are missing.

  Returns the qualified fn-sym.

  For example, to register function specs for the symbol function:

  (s/fdef cloiure.core/symbol
    :args (s/alt :separate (s/cat :ns string? :n string?)
                 :str string?
                 :sym symbol?)
    :ret symbol?)"
  [fn-sym & specs]
  `(cloiure.spec.alpha/def ~fn-sym (cloiure.spec.alpha/fspec ~@specs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; impl ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(§ defn- recur-limit? [rmap id path k]
  (c/and (> (get rmap id) (::recursion-limit rmap))
         (contains? (set path) k)))

(§ defn- inck [m k]
  (assoc m k (inc (c/or (get m k) 0))))

(§ defn- dt
  ([pred x form] (dt pred x form nil))
  ([pred x form cpred?]
     (if pred
       (if-let [spec (the-spec pred)]
         (conform spec x)
         (if (ifn? pred)
           (if cpred?
             (pred x)
             (if (pred x) x ::invalid))
           (throw (Exception. (str (pr-str form) " is not a fn, expected predicate fn")))))
       x)))

(§ defn valid?
  "Helper function that returns true when x is valid for spec."
  ([spec x]
     (let [spec (specize spec)]
       (not (invalid? (conform* spec x)))))
  ([spec x form]
     (let [spec (specize spec form)]
       (not (invalid? (conform* spec x))))))

(§ defn- pvalid?
  "internal helper function that returns true when x is valid for spec."
  ([pred x]
     (not (invalid? (dt pred x ::unknown))))
  ([pred x form]
     (not (invalid? (dt pred x form)))))

(§ defn- explain-1 [form pred path via in v]
  ;;(prn {:form form :pred pred :path path :in in :v v})
  (let [pred (maybe-spec pred)]
    (if (spec? pred)
      (explain* pred path (if-let [name (spec-name pred)] (conj via name) via) in v)
      [{:path path :pred form :val v :via via :in in}])))

(§ defn ^:skip-wiki map-spec-impl
  "Do not call this directly, use 'spec' with a map argument"
  [{:keys [req-un opt-un keys-pred pred-exprs opt-keys req-specs req req-keys opt-specs pred-forms opt gfn]
    :as argm}]
  (let [k->s (zipmap (concat req-keys opt-keys) (concat req-specs opt-specs))
        keys->specnames #(c/or (k->s %) %)
        id (java.util.UUID/randomUUID)]
    (reify
     Specize
     (specize* [s] s)
     (specize* [s _] s)

     Spec
     (conform* [_ m]
               (if (keys-pred m)
                 (let [reg (registry)]
                   (loop [ret m, [[k v] & ks :as keys] m]
                     (if keys
                       (let [sname (keys->specnames k)]
                         (if-let [s (get reg sname)]
                           (let [cv (conform s v)]
                             (if (invalid? cv)
                               ::invalid
                               (recur (if (identical? cv v) ret (assoc ret k cv))
                                      ks)))
                           (recur ret ks)))
                       ret)))
                 ::invalid))
     (unform* [_ m]
              (let [reg (registry)]
                (loop [ret m, [k & ks :as keys] (c/keys m)]
                  (if keys
                    (if (contains? reg (keys->specnames k))
                      (let [cv (get m k)
                            v (unform (keys->specnames k) cv)]
                        (recur (if (identical? cv v) ret (assoc ret k v))
                               ks))
                      (recur ret ks))
                    ret))))
     (explain* [_ path via in x]
               (if-not (map? x)
                 [{:path path :pred 'map? :val x :via via :in in}]
                 (let [reg (registry)]
                   (apply concat
                          (when-let [probs (->> (map (fn [pred form] (when-not (pred x) form))
                                                     pred-exprs pred-forms)
                                                (keep identity)
                                                seq)]
                            (map
                             #(identity {:path path :pred % :val x :via via :in in})
                             probs))
                          (map (fn [[k v]]
                                 (when-not (c/or (not (contains? reg (keys->specnames k)))
                                                 (pvalid? (keys->specnames k) v k))
                                   (explain-1 (keys->specnames k) (keys->specnames k) (conj path k) via (conj in k) v)))
                               (seq x))))))
     (gen* [_ overrides path rmap]
           (if gfn
             (gfn)
             (let [rmap (inck rmap id)
                   gen (fn [k s] (gensub s overrides (conj path k) rmap k))
                   ogen (fn [k s]
                          (when-not (recur-limit? rmap id path k)
                            [k (gen/delay (gensub s overrides (conj path k) rmap k))]))
                   req-gens (map gen req-keys req-specs)
                   opt-gens (remove nil? (map ogen opt-keys opt-specs))]
               (when (every? identity (concat req-gens opt-gens))
                 (let [reqs (zipmap req-keys req-gens)
                       opts (into {} opt-gens)]
                   (gen/bind (gen/choose 0 (count opts))
                             #(let [args (concat (seq reqs) (when (seq opts) (shuffle (seq opts))))]
                                (->> args
                                     (take (c/+ % (count reqs)))
                                     (apply concat)
                                     (apply gen/hash-map)))))))))
     (with-gen* [_ gfn] (map-spec-impl (assoc argm :gfn gfn)))
     (describe* [_] (cons `keys
                          (cond-> []
                                  req (conj :req req)
                                  opt (conj :opt opt)
                                  req-un (conj :req-un req-un)
                                  opt-un (conj :opt-un opt-un)))))))

(§ defn ^:skip-wiki spec-impl
  "Do not call this directly, use 'spec'"
  ([form pred gfn cpred?] (spec-impl form pred gfn cpred? nil))
  ([form pred gfn cpred? unc]
     (cond
      (spec? pred) (cond-> pred gfn (with-gen gfn))
      (regex? pred) (regex-spec-impl pred gfn)
      (ident? pred) (cond-> (the-spec pred) gfn (with-gen gfn))
      :else
      (reify
       Specize
       (specize* [s] s)
       (specize* [s _] s)

       Spec
       (conform* [_ x] (let [ret (pred x)]
                         (if cpred?
                           ret
                           (if ret x ::invalid))))
       (unform* [_ x] (if cpred?
                        (if unc
                          (unc x)
                          (throw (IllegalStateException. "no unform fn for conformer")))
                        x))
       (explain* [_ path via in x]
                 (when (invalid? (dt pred x form cpred?))
                   [{:path path :pred form :val x :via via :in in}]))
       (gen* [_ _ _ _] (if gfn
                         (gfn)
                         (gen/gen-for-pred pred)))
       (with-gen* [_ gfn] (spec-impl form pred gfn cpred? unc))
       (describe* [_] form)))))

(§ defn ^:skip-wiki multi-spec-impl
  "Do not call this directly, use 'multi-spec'"
  ([form mmvar retag] (multi-spec-impl form mmvar retag nil))
  ([form mmvar retag gfn]
     (let [id (java.util.UUID/randomUUID)
           predx #(let [^cloiure.lang.MultiFn mm @mmvar]
                    (c/and (.getMethod mm ((.dispatchFn mm) %))
                           (mm %)))
           dval #((.dispatchFn ^cloiure.lang.MultiFn @mmvar) %)
           tag (if (keyword? retag)
                 #(assoc %1 retag %2)
                 retag)]
       (reify
        Specize
        (specize* [s] s)
        (specize* [s _] s)

        Spec
        (conform* [_ x] (if-let [pred (predx x)]
                          (dt pred x form)
                          ::invalid))
        (unform* [_ x] (if-let [pred (predx x)]
                         (unform pred x)
                         (throw (IllegalStateException. (str "No method of: " form " for dispatch value: " (dval x))))))
        (explain* [_ path via in x]
                  (let [dv (dval x)
                        path (conj path dv)]
                    (if-let [pred (predx x)]
                      (explain-1 form pred path via in x)
                      [{:path path :pred form :val x :reason "no method" :via via :in in}])))
        (gen* [_ overrides path rmap]
              (if gfn
                (gfn)
                (let [gen (fn [[k f]]
                            (let [p (f nil)]
                              (let [rmap (inck rmap id)]
                                (when-not (recur-limit? rmap id path k)
                                  (gen/delay
                                   (gen/fmap
                                    #(tag % k)
                                    (gensub p overrides (conj path k) rmap (list 'method form k))))))))
                      gs (->> (methods @mmvar)
                              (remove (fn [[k]] (invalid? k)))
                              (map gen)
                              (remove nil?))]
                  (when (every? identity gs)
                    (gen/one-of gs)))))
        (with-gen* [_ gfn] (multi-spec-impl form mmvar retag gfn))
        (describe* [_] `(multi-spec ~form ~retag))))))

(§ defn ^:skip-wiki tuple-impl
  "Do not call this directly, use 'tuple'"
  ([forms preds] (tuple-impl forms preds nil))
  ([forms preds gfn]
     (let [specs (delay (mapv specize preds forms))
           cnt (count preds)]
       (reify
        Specize
        (specize* [s] s)
        (specize* [s _] s)

        Spec
        (conform* [_ x]
                  (let [specs @specs]
                    (if-not (c/and (vector? x)
                                   (= (count x) cnt))
                      ::invalid
                      (loop [ret x, i 0]
                        (if (= i cnt)
                          ret
                          (let [v (x i)
                                cv (conform* (specs i) v)]
                            (if (invalid? cv)
                              ::invalid
                              (recur (if (identical? cv v) ret (assoc ret i cv))
                                     (inc i)))))))))
        (unform* [_ x]
                 (c/assert (c/and (vector? x)
                                  (= (count x) (count preds))))
                 (loop [ret x, i 0]
                   (if (= i (count x))
                     ret
                     (let [cv (x i)
                           v (unform (preds i) cv)]
                       (recur (if (identical? cv v) ret (assoc ret i v))
                              (inc i))))))
        (explain* [_ path via in x]
                  (cond
                   (not (vector? x))
                   [{:path path :pred 'vector? :val x :via via :in in}]

                   (not= (count x) (count preds))
                   [{:path path :pred `(= (count ~'%) ~(count preds)) :val x :via via :in in}]

                   :else
                   (apply concat
                          (map (fn [i form pred]
                                 (let [v (x i)]
                                   (when-not (pvalid? pred v)
                                     (explain-1 form pred (conj path i) via (conj in i) v))))
                               (range (count preds)) forms preds))))
        (gen* [_ overrides path rmap]
              (if gfn
                (gfn)
                (let [gen (fn [i p f]
                            (gensub p overrides (conj path i) rmap f))
                      gs (map gen (range (count preds)) preds forms)]
                  (when (every? identity gs)
                    (apply gen/tuple gs)))))
        (with-gen* [_ gfn] (tuple-impl forms preds gfn))
        (describe* [_] `(tuple ~@forms))))))

(§ defn- tagged-ret [tag ret]
  (cloiure.lang.MapEntry. tag ret))

(§ defn ^:skip-wiki or-spec-impl
  "Do not call this directly, use 'or'"
  [keys forms preds gfn]
  (let [id (java.util.UUID/randomUUID)
        kps (zipmap keys preds)
        specs (delay (mapv specize preds forms))
        cform (case (count preds)
                    2 (fn [x]
                        (let [specs @specs
                              ret (conform* (specs 0) x)]
                          (if (invalid? ret)
                            (let [ret (conform* (specs 1) x)]
                              (if (invalid? ret)
                                ::invalid
                                (tagged-ret (keys 1) ret)))
                            (tagged-ret (keys 0) ret))))
                    3 (fn [x]
                        (let [specs @specs
                              ret (conform* (specs 0) x)]
                          (if (invalid? ret)
                            (let [ret (conform* (specs 1) x)]
                              (if (invalid? ret)
                                (let [ret (conform* (specs 2) x)]
                                  (if (invalid? ret)
                                    ::invalid
                                    (tagged-ret (keys 2) ret)))
                                (tagged-ret (keys 1) ret)))
                            (tagged-ret (keys 0) ret))))
                    (fn [x]
                      (let [specs @specs]
                        (loop [i 0]
                          (if (< i (count specs))
                            (let [spec (specs i)]
                              (let [ret (conform* spec x)]
                                (if (invalid? ret)
                                  (recur (inc i))
                                  (tagged-ret (keys i) ret))))
                            ::invalid)))))]
    (reify
     Specize
     (specize* [s] s)
     (specize* [s _] s)

     Spec
     (conform* [_ x] (cform x))
     (unform* [_ [k x]] (unform (kps k) x))
     (explain* [this path via in x]
               (when-not (pvalid? this x)
                 (apply concat
                        (map (fn [k form pred]
                               (when-not (pvalid? pred x)
                                 (explain-1 form pred (conj path k) via in x)))
                             keys forms preds))))
     (gen* [_ overrides path rmap]
           (if gfn
             (gfn)
             (let [gen (fn [k p f]
                         (let [rmap (inck rmap id)]
                           (when-not (recur-limit? rmap id path k)
                             (gen/delay
                              (gensub p overrides (conj path k) rmap f)))))
                   gs (remove nil? (map gen keys preds forms))]
               (when-not (empty? gs)
                 (gen/one-of gs)))))
     (with-gen* [_ gfn] (or-spec-impl keys forms preds gfn))
     (describe* [_] `(or ~@(mapcat vector keys forms))))))

(§ defn- and-preds [x preds forms]
  (loop [ret x
         [pred & preds] preds
         [form & forms] forms]
    (if pred
      (let [nret (dt pred ret form)]
        (if (invalid? nret)
          ::invalid
          ;;propagate conformed values
          (recur nret preds forms)))
      ret)))

(§ defn- explain-pred-list
  [forms preds path via in x]
  (loop [ret x
         [form & forms] forms
         [pred & preds] preds]
    (when pred
      (let [nret (dt pred ret form)]
        (if (invalid? nret)
          (explain-1 form pred path via in ret)
          (recur nret forms preds))))))

(§ defn ^:skip-wiki and-spec-impl
  "Do not call this directly, use 'and'"
  [forms preds gfn]
  (let [specs (delay (mapv specize preds forms))
        cform
        (case (count preds)
              2 (fn [x]
                  (let [specs @specs
                        ret (conform* (specs 0) x)]
                    (if (invalid? ret)
                      ::invalid
                      (conform* (specs 1) ret))))
              3 (fn [x]
                  (let [specs @specs
                        ret (conform* (specs 0) x)]
                    (if (invalid? ret)
                      ::invalid
                      (let [ret (conform* (specs 1) ret)]
                        (if (invalid? ret)
                          ::invalid
                          (conform* (specs 2) ret))))))
              (fn [x]
                (let [specs @specs]
                  (loop [ret x i 0]
                    (if (< i (count specs))
                      (let [nret (conform* (specs i) ret)]
                        (if (invalid? nret)
                          ::invalid
                          ;;propagate conformed values
                          (recur nret (inc i))))
                      ret)))))]
    (reify
     Specize
     (specize* [s] s)
     (specize* [s _] s)

     Spec
     (conform* [_ x] (cform x))
     (unform* [_ x] (reduce #(unform %2 %1) x (reverse preds)))
     (explain* [_ path via in x] (explain-pred-list forms preds path via in x))
     (gen* [_ overrides path rmap] (if gfn (gfn) (gensub (first preds) overrides path rmap (first forms))))
     (with-gen* [_ gfn] (and-spec-impl forms preds gfn))
     (describe* [_] `(and ~@forms)))))

(§ defn ^:skip-wiki merge-spec-impl
  "Do not call this directly, use 'merge'"
  [forms preds gfn]
  (reify
   Specize
   (specize* [s] s)
   (specize* [s _] s)

   Spec
   (conform* [_ x] (let [ms (map #(dt %1 x %2) preds forms)]
                     (if (some invalid? ms)
                       ::invalid
                       (apply c/merge ms))))
   (unform* [_ x] (apply c/merge (map #(unform % x) (reverse preds))))
   (explain* [_ path via in x]
             (apply concat
                    (map #(explain-1 %1 %2 path via in x)
                         forms preds)))
   (gen* [_ overrides path rmap]
         (if gfn
           (gfn)
           (gen/fmap
            #(apply c/merge %)
            (apply gen/tuple (map #(gensub %1 overrides path rmap %2)
                                  preds forms)))))
   (with-gen* [_ gfn] (merge-spec-impl forms preds gfn))
   (describe* [_] `(merge ~@forms))))

(§ defn- coll-prob [x kfn kform distinct count min-count max-count
                  path via in]
  (let [pred (c/or kfn coll?)
        kform (c/or kform `coll?)]
    (cond
     (not (pvalid? pred x))
     (explain-1 kform pred path via in x)

     (c/and count (not= count (bounded-count count x)))
     [{:path path :pred `(= ~count (c/count ~'%)) :val x :via via :in in}]

     (c/and (c/or min-count max-count)
            (not (<= (c/or min-count 0)
                     (bounded-count (if max-count (inc max-count) min-count) x)
                     (c/or max-count Integer/MAX_VALUE))))
     [{:path path :pred `(<= ~(c/or min-count 0) (c/count ~'%) ~(c/or max-count 'Integer/MAX_VALUE)) :val x :via via :in in}]

     (c/and distinct (not (empty? x)) (not (apply distinct? x)))
     [{:path path :pred 'distinct? :val x :via via :in in}])))

(§ def ^:private empty-coll {`vector? [], `set? #{}, `list? (), `map? {}})

(§ defn ^:skip-wiki every-impl
  "Do not call this directly, use 'every', 'every-kv', 'coll-of' or 'map-of'"
  ([form pred opts] (every-impl form pred opts nil))
  ([form pred {conform-into :into
               describe-form ::describe
               :keys [kind ::kind-form count max-count min-count distinct gen-max ::kfn ::cpred
                      conform-keys ::conform-all]
               :or {gen-max 20}
               :as opts}
    gfn]
     (let [gen-into (if conform-into (empty conform-into) (get empty-coll kind-form))
           spec (delay (specize pred))
           check? #(valid? @spec %)
           kfn (c/or kfn (fn [i v] i))
           addcv (fn [ret i v cv] (conj ret cv))
           cfns (fn [x]
                  ;;returns a tuple of [init add complete] fns
                  (cond
                   (c/and (vector? x) (c/or (not conform-into) (vector? conform-into)))
                   [identity
                    (fn [ret i v cv]
                      (if (identical? v cv)
                        ret
                        (assoc ret i cv)))
                    identity]

                   (c/and (map? x) (c/or (c/and kind (not conform-into)) (map? conform-into)))
                   [(if conform-keys empty identity)
                    (fn [ret i v cv]
                      (if (c/and (identical? v cv) (not conform-keys))
                        ret
                        (assoc ret (nth (if conform-keys cv v) 0) (nth cv 1))))
                    identity]

                   (c/or (list? conform-into) (seq? conform-into) (c/and (not conform-into) (c/or (list? x) (seq? x))))
                   [(constantly ()) addcv reverse]

                   :else [#(empty (c/or conform-into %)) addcv identity]))]
       (reify
        Specize
        (specize* [s] s)
        (specize* [s _] s)

        Spec
        (conform* [_ x]
                  (let [spec @spec]
                    (cond
                     (not (cpred x)) ::invalid

                     conform-all
                     (let [[init add complete] (cfns x)]
                       (loop [ret (init x), i 0, [v & vs :as vseq] (seq x)]
                         (if vseq
                           (let [cv (conform* spec v)]
                             (if (invalid? cv)
                               ::invalid
                               (recur (add ret i v cv) (inc i) vs)))
                           (complete ret))))

                     :else
                     (if (indexed? x)
                       (let [step (max 1 (long (/ (c/count x) *coll-check-limit*)))]
                         (loop [i 0]
                           (if (>= i (c/count x))
                             x
                             (if (valid? spec (nth x i))
                               (recur (c/+ i step))
                               ::invalid))))
                       (let [limit *coll-check-limit*]
                         (loop [i 0 [v & vs :as vseq] (seq x)]
                           (cond
                            (c/or (nil? vseq) (= i limit)) x
                            (valid? spec v) (recur (inc i) vs)
                            :else ::invalid)))))))
        (unform* [_ x]
                 (if conform-all
                   (let [spec @spec
                         [init add complete] (cfns x)]
                     (loop [ret (init x), i 0, [v & vs :as vseq] (seq x)]
                       (if (>= i (c/count x))
                         (complete ret)
                         (recur (add ret i v (unform* spec v)) (inc i) vs))))
                   x))
        (explain* [_ path via in x]
                  (c/or (coll-prob x kind kind-form distinct count min-count max-count
                                   path via in)
                        (apply concat
                               ((if conform-all identity (partial take *coll-error-limit*))
                                (keep identity
                                      (map (fn [i v]
                                             (let [k (kfn i v)]
                                               (when-not (check? v)
                                                 (let [prob (explain-1 form pred path via (conj in k) v)]
                                                   prob))))
                                           (range) x))))))
        (gen* [_ overrides path rmap]
              (if gfn
                (gfn)
                (let [pgen (gensub pred overrides path rmap form)]
                  (gen/bind
                   (cond
                    gen-into (gen/return gen-into)
                    kind (gen/fmap #(if (empty? %) % (empty %))
                                   (gensub kind overrides path rmap form))
                    :else (gen/return []))
                   (fn [init]
                     (gen/fmap
                      #(if (vector? init) % (into init %))
                      (cond
                       distinct
                       (if count
                         (gen/vector-distinct pgen {:num-elements count :max-tries 100})
                         (gen/vector-distinct pgen {:min-elements (c/or min-count 0)
                                                    :max-elements (c/or max-count (max gen-max (c/* 2 (c/or min-count 0))))
                                                    :max-tries 100}))

                       count
                       (gen/vector pgen count)

                       (c/or min-count max-count)
                       (gen/vector pgen (c/or min-count 0) (c/or max-count (max gen-max (c/* 2 (c/or min-count 0)))))

                       :else
                       (gen/vector pgen 0 gen-max))))))))

        (with-gen* [_ gfn] (every-impl form pred opts gfn))
        (describe* [_] (c/or describe-form `(every ~(res form) ~@(mapcat identity opts))))))))

;;;;;;;;;;;;;;;;;;;;;;; regex ;;;;;;;;;;;;;;;;;;;
;;See:
;; http://matt.might.net/articles/implementation-of-regular-expression-matching-in-scheme-with-derivatives/
;; http://www.ccs.neu.edu/home/turon/re-deriv.pdf

;;ctors
(§ defn- accept [x] {::op ::accept :ret x})

(§ defn- accept? [{:keys [::op]}]
  (= ::accept op))

(§ defn- pcat* [{[p1 & pr :as ps] :ps,  [k1 & kr :as ks] :ks, [f1 & fr :as forms] :forms, ret :ret, rep+ :rep+}]
  (when (every? identity ps)
    (if (accept? p1)
      (let [rp (:ret p1)
            ret (conj ret (if ks {k1 rp} rp))]
        (if pr
          (pcat* {:ps pr :ks kr :forms fr :ret ret})
          (accept ret)))
      {::op ::pcat, :ps ps, :ret ret, :ks ks, :forms forms :rep+ rep+})))

(§ defn- pcat [& ps] (pcat* {:ps ps :ret []}))

(§ defn ^:skip-wiki cat-impl
  "Do not call this directly, use 'cat'"
  [ks ps forms]
  (pcat* {:ks ks, :ps ps, :forms forms, :ret {}}))

(§ defn- rep* [p1 p2 ret splice form]
  (when p1
    (let [r {::op ::rep, :p2 p2, :splice splice, :forms form :id (java.util.UUID/randomUUID)}]
      (if (accept? p1)
        (assoc r :p1 p2 :ret (conj ret (:ret p1)))
        (assoc r :p1 p1, :ret ret)))))

(§ defn ^:skip-wiki rep-impl
  "Do not call this directly, use '*'"
  [form p] (rep* p p [] false form))

(§ defn ^:skip-wiki rep+impl
  "Do not call this directly, use '+'"
  [form p]
  (pcat* {:ps [p (rep* p p [] true form)] :forms `[~form (* ~form)] :ret [] :rep+ form}))

(§ defn ^:skip-wiki amp-impl
  "Do not call this directly, use '&'"
  [re preds pred-forms]
  {::op ::amp :p1 re :ps preds :forms pred-forms})

(§ defn- filter-alt [ps ks forms f]
  (if (c/or ks forms)
    (let [pks (->> (map vector ps
                        (c/or (seq ks) (repeat nil))
                        (c/or (seq forms) (repeat nil)))
                   (filter #(-> % first f)))]
      [(seq (map first pks)) (when ks (seq (map second pks))) (when forms (seq (map #(nth % 2) pks)))])
    [(seq (filter f ps)) ks forms]))

(§ defn- alt* [ps ks forms]
  (let [[[p1 & pr :as ps] [k1 :as ks] forms] (filter-alt ps ks forms identity)]
    (when ps
      (let [ret {::op ::alt, :ps ps, :ks ks :forms forms}]
        (if (nil? pr)
          (if k1
            (if (accept? p1)
              (accept (tagged-ret k1 (:ret p1)))
              ret)
            p1)
          ret)))))

(§ defn- alts [& ps] (alt* ps nil nil))
(§ defn- alt2 [p1 p2] (if (c/and p1 p2) (alts p1 p2) (c/or p1 p2)))

(§ defn ^:skip-wiki alt-impl
  "Do not call this directly, use 'alt'"
  [ks ps forms] (assoc (alt* ps ks forms) :id (java.util.UUID/randomUUID)))

(§ defn ^:skip-wiki maybe-impl
  "Do not call this directly, use '?'"
  [p form] (assoc (alt* [p (accept ::nil)] nil [form ::nil]) :maybe form))

(§ defn- noret? [p1 pret]
  (c/or (= pret ::nil)
        (c/and (#{::rep ::pcat} (::op (reg-resolve! p1))) ;;hrm, shouldn't know these
               (empty? pret))
        nil))

(§ declare preturn)

(§ defn- accept-nil? [p]
  (let [{:keys [::op ps p1 p2 forms] :as p} (reg-resolve! p)]
    (case op
          ::accept true
          nil nil
          ::amp (c/and (accept-nil? p1)
                       (c/or (noret? p1 (preturn p1))
                             (let [ret (-> (preturn p1) (and-preds ps (next forms)))]
                               (not (invalid? ret)))))
          ::rep (c/or (identical? p1 p2) (accept-nil? p1))
          ::pcat (every? accept-nil? ps)
          ::alt (c/some accept-nil? ps))))

(§ declare add-ret)

(§ defn- preturn [p]
  (let [{[p0 & pr :as ps] :ps, [k :as ks] :ks, :keys [::op p1 ret forms] :as p} (reg-resolve! p)]
    (case op
          ::accept ret
          nil nil
          ::amp (let [pret (preturn p1)]
                  (if (noret? p1 pret)
                    ::nil
                    (and-preds pret ps forms)))
          ::rep (add-ret p1 ret k)
          ::pcat (add-ret p0 ret k)
          ::alt (let [[[p0] [k0]] (filter-alt ps ks forms accept-nil?)
                      r (if (nil? p0) ::nil (preturn p0))]
                  (if k0 (tagged-ret k0 r) r)))))

(§ defn- op-unform [p x]
  ;;(prn {:p p :x x})
  (let [{[p0 & pr :as ps] :ps, [k :as ks] :ks, :keys [::op p1 ret forms rep+ maybe] :as p} (reg-resolve! p)
        kps (zipmap ks ps)]
    (case op
          ::accept [ret]
          nil [(unform p x)]
          ::amp (let [px (reduce #(unform %2 %1) x (reverse ps))]
                  (op-unform p1 px))
          ::rep (mapcat #(op-unform p1 %) x)
          ::pcat (if rep+
                   (mapcat #(op-unform p0 %) x)
                   (mapcat (fn [k]
                             (when (contains? x k)
                               (op-unform (kps k) (get x k))))
                           ks))
          ::alt (if maybe
                  [(unform p0 x)]
                  (let [[k v] x]
                    (op-unform (kps k) v))))))

(§ defn- add-ret [p r k]
  (let [{:keys [::op ps splice] :as p} (reg-resolve! p)
        prop #(let [ret (preturn p)]
                (if (empty? ret) r ((if splice into conj) r (if k {k ret} ret))))]
    (case op
          nil r
          (::alt ::accept ::amp)
          (let [ret (preturn p)]
            ;;(prn {:ret ret})
            (if (= ret ::nil) r (conj r (if k {k ret} ret))))

          (::rep ::pcat) (prop))))

(§ defn- deriv
  [p x]
  (let [{[p0 & pr :as ps] :ps, [k0 & kr :as ks] :ks, :keys [::op p1 p2 ret splice forms] :as p} (reg-resolve! p)]
    (when p
      (case op
            ::accept nil
            nil (let [ret (dt p x p)]
                  (when-not (invalid? ret) (accept ret)))
            ::amp (when-let [p1 (deriv p1 x)]
                    (if (= ::accept (::op p1))
                      (let [ret (-> (preturn p1) (and-preds ps (next forms)))]
                        (when-not (invalid? ret)
                          (accept ret)))
                      (amp-impl p1 ps forms)))
            ::pcat (alt2 (pcat* {:ps (cons (deriv p0 x) pr), :ks ks, :forms forms, :ret ret})
                         (when (accept-nil? p0) (deriv (pcat* {:ps pr, :ks kr, :forms (next forms), :ret (add-ret p0 ret k0)}) x)))
            ::alt (alt* (map #(deriv % x) ps) ks forms)
            ::rep (alt2 (rep* (deriv p1 x) p2 ret splice forms)
                        (when (accept-nil? p1) (deriv (rep* p2 p2 (add-ret p1 ret nil) splice forms) x)))))))

(§ defn- op-describe [p]
  (let [{:keys [::op ps ks forms splice p1 rep+ maybe] :as p} (reg-resolve! p)]
    ;;(prn {:op op :ks ks :forms forms :p p})
    (when p
      (case op
            ::accept nil
            nil p
            ::amp (list* 'cloiure.spec.alpha/& (op-describe p1) forms)
            ::pcat (if rep+
                     (list `+ rep+)
                     (cons `cat (mapcat vector (c/or (seq ks) (repeat :_)) forms)))
            ::alt (if maybe
                    (list `? maybe)
                    (cons `alt (mapcat vector ks forms)))
            ::rep (list (if splice `+ `*) forms)))))

(§ defn- op-explain [form p path via in input]
  ;;(prn {:form form :p p :path path :input input})
  (let [[x :as input] input
        {:keys [::op ps ks forms splice p1 p2] :as p} (reg-resolve! p)
        via (if-let [name (spec-name p)] (conj via name) via)
        insufficient (fn [path form]
                       [{:path path
                         :reason "Insufficient input"
                         :pred form
                         :val ()
                         :via via
                         :in in}])]
    (when p
      (case op
            ::accept nil
            nil (if (empty? input)
                  (insufficient path form)
                  (explain-1 form p path via in x))
            ::amp (if (empty? input)
                    (if (accept-nil? p1)
                      (explain-pred-list forms ps path via in (preturn p1))
                      (insufficient path (op-describe p1)))
                    (if-let [p1 (deriv p1 x)]
                      (explain-pred-list forms ps path via in (preturn p1))
                      (op-explain (op-describe p1) p1 path via in input)))
            ::pcat (let [pkfs (map vector
                                   ps
                                   (c/or (seq ks) (repeat nil))
                                   (c/or (seq forms) (repeat nil)))
                         [pred k form] (if (= 1 (count pkfs))
                                         (first pkfs)
                                         (first (remove (fn [[p]] (accept-nil? p)) pkfs)))
                         path (if k (conj path k) path)
                         form (c/or form (op-describe pred))]
                     (if (c/and (empty? input) (not pred))
                       (insufficient path form)
                       (op-explain form pred path via in input)))
            ::alt (if (empty? input)
                    (insufficient path (op-describe p))
                    (apply concat
                           (map (fn [k form pred]
                                  (op-explain (c/or form (op-describe pred))
                                              pred
                                              (if k (conj path k) path)
                                              via
                                              in
                                              input))
                                (c/or (seq ks) (repeat nil))
                                (c/or (seq forms) (repeat nil))
                                ps)))
            ::rep (op-explain (if (identical? p1 p2)
                                forms
                                (op-describe p1))
                              p1 path via in input)))))

(§ defn- re-gen [p overrides path rmap f]
  ;;(prn {:op op :ks ks :forms forms})
  (let [origp p
        {:keys [::op ps ks p1 p2 forms splice ret id ::gfn] :as p} (reg-resolve! p)
        rmap (if id (inck rmap id) rmap)
        ggens (fn [ps ks forms]
                (let [gen (fn [p k f]
                            ;;(prn {:k k :path path :rmap rmap :op op :id id})
                            (when-not (c/and rmap id k (recur-limit? rmap id path k))
                              (if id
                                (gen/delay (re-gen p overrides (if k (conj path k) path) rmap (c/or f p)))
                                (re-gen p overrides (if k (conj path k) path) rmap (c/or f p)))))]
                  (map gen ps (c/or (seq ks) (repeat nil)) (c/or (seq forms) (repeat nil)))))]
    (c/or (when-let [gfn (c/or (get overrides (spec-name origp))
                               (get overrides (spec-name p) )
                               (get overrides path))]
            (case op
                  (:accept nil) (gen/fmap vector (gfn))
                  (gfn)))
          (when gfn
            (gfn))
          (when p
            (case op
                  ::accept (if (= ret ::nil)
                             (gen/return [])
                             (gen/return [ret]))
                  nil (when-let [g (gensub p overrides path rmap f)]
                        (gen/fmap vector g))
                  ::amp (re-gen p1 overrides path rmap (op-describe p1))
                  ::pcat (let [gens (ggens ps ks forms)]
                           (when (every? identity gens)
                             (apply gen/cat gens)))
                  ::alt (let [gens (remove nil? (ggens ps ks forms))]
                          (when-not (empty? gens)
                            (gen/one-of gens)))
                  ::rep (if (recur-limit? rmap id [id] id)
                          (gen/return [])
                          (when-let [g (re-gen p2 overrides path rmap forms)]
                            (gen/fmap #(apply concat %)
                                      (gen/vector g)))))))))

(§ defn- re-conform [p [x & xs :as data]]
  ;;(prn {:p p :x x :xs xs})
  (if (empty? data)
    (if (accept-nil? p)
      (let [ret (preturn p)]
        (if (= ret ::nil)
          nil
          ret))
      ::invalid)
    (if-let [dp (deriv p x)]
      (recur dp xs)
      ::invalid)))

(§ defn- re-explain [path via in re input]
  (loop [p re [x & xs :as data] input i 0]
    ;;(prn {:p p :x x :xs xs :re re}) (prn)
    (if (empty? data)
      (if (accept-nil? p)
        nil ;;success
        (op-explain (op-describe p) p path via in nil))
      (if-let [dp (deriv p x)]
        (recur dp xs (inc i))
        (if (accept? p)
          (if (= (::op p) ::pcat)
            (op-explain (op-describe p) p path via (conj in i) (seq data))
            [{:path path
              :reason "Extra input"
              :pred (op-describe re)
              :val data
              :via via
              :in (conj in i)}])
          (c/or (op-explain (op-describe p) p path via (conj in i) (seq data))
                [{:path path
                  :reason "Extra input"
                  :pred (op-describe p)
                  :val data
                  :via via
                  :in (conj in i)}]))))))

(§ defn ^:skip-wiki regex-spec-impl
  "Do not call this directly, use 'spec' with a regex op argument"
  [re gfn]
  (reify
   Specize
   (specize* [s] s)
   (specize* [s _] s)

   Spec
   (conform* [_ x]
             (if (c/or (nil? x) (coll? x))
               (re-conform re (seq x))
               ::invalid))
   (unform* [_ x] (op-unform re x))
   (explain* [_ path via in x]
             (if (c/or (nil? x) (coll? x))
               (re-explain path via in re (seq x))
               [{:path path :pred (op-describe re) :val x :via via :in in}]))
   (gen* [_ overrides path rmap]
         (if gfn
           (gfn)
           (re-gen re overrides path rmap (op-describe re))))
   (with-gen* [_ gfn] (regex-spec-impl re gfn))
   (describe* [_] (op-describe re))))

;;;;;;;;;;;;;;;;; HOFs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ defn- call-valid?
  [f specs args]
  (let [cargs (conform (:args specs) args)]
    (when-not (invalid? cargs)
      (let [ret (apply f args)
            cret (conform (:ret specs) ret)]
        (c/and (not (invalid? cret))
               (if (:fn specs)
                 (pvalid? (:fn specs) {:args cargs :ret cret})
                 true))))))

(§ defn- validate-fn
  "returns f if valid, else smallest"
  [f specs iters]
  (let [g (gen (:args specs))
        prop (gen/for-all* [g] #(call-valid? f specs %))]
    (let [ret (gen/quick-check iters prop)]
      (if-let [[smallest] (-> ret :shrunk :smallest)]
        smallest
        f))))

(§ defn ^:skip-wiki fspec-impl
  "Do not call this directly, use 'fspec'"
  [argspec aform retspec rform fnspec fform gfn]
  (let [specs {:args argspec :ret retspec :fn fnspec}]
    (reify
     cloiure.lang.ILookup
     (valAt [this k] (get specs k))
     (valAt [_ k not-found] (get specs k not-found))

     Specize
     (specize* [s] s)
     (specize* [s _] s)

     Spec
     (conform* [this f] (if argspec
                          (if (ifn? f)
                            (if (identical? f (validate-fn f specs *fspec-iterations*)) f ::invalid)
                            ::invalid)
                          (throw (Exception. (str "Can't conform fspec without args spec: " (pr-str (describe this)))))))
     (unform* [_ f] f)
     (explain* [_ path via in f]
               (if (ifn? f)
                 (let [args (validate-fn f specs 100)]
                   (if (identical? f args) ;;hrm, we might not be able to reproduce
                     nil
                     (let [ret (try (apply f args) (catch Throwable t t))]
                       (if (instance? Throwable ret)
                         ;;TODO add exception data
                         [{:path path :pred '(apply fn) :val args :reason (.getMessage ^Throwable ret) :via via :in in}]

                         (let [cret (dt retspec ret rform)]
                           (if (invalid? cret)
                             (explain-1 rform retspec (conj path :ret) via in ret)
                             (when fnspec
                               (let [cargs (conform argspec args)]
                                 (explain-1 fform fnspec (conj path :fn) via in {:args cargs :ret cret})))))))))
                 [{:path path :pred 'ifn? :val f :via via :in in}]))
     (gen* [_ overrides _ _] (if gfn
             (gfn)
             (gen/return
              (fn [& args]
                (c/assert (pvalid? argspec args) (with-out-str (explain argspec args)))
                (gen/generate (gen retspec overrides))))))
     (with-gen* [_ gfn] (fspec-impl argspec aform retspec rform fnspec fform gfn))
     (describe* [_] `(fspec :args ~aform :ret ~rform :fn ~fform)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; non-primitives ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(§ cloiure.spec.alpha/def ::kvs->map (conformer #(zipmap (map ::k %) (map ::v %)) #(map (fn [[k v]] {::k k ::v v}) %)))

(§ defmacro keys*
  "takes the same arguments as spec/keys and returns a regex op that matches sequences of key/values,
  converts them into a map, and conforms that map with a corresponding
  spec/keys call:

  user=> (s/conform (s/keys :req-un [::a ::c]) {:a 1 :c 2})
  {:a 1, :c 2}
  user=> (s/conform (s/keys* :req-un [::a ::c]) [:a 1 :c 2])
  {:a 1, :c 2}

  the resulting regex op can be composed into a larger regex:

  user=> (s/conform (s/cat :i1 integer? :m (s/keys* :req-un [::a ::c]) :i2 integer?) [42 :a 1 :c 2 :d 4 99])
  {:i1 42, :m {:a 1, :c 2, :d 4}, :i2 99}"
  [& kspecs]
  `(let [mspec# (keys ~@kspecs)]
     (with-gen (cloiure.spec.alpha/& (* (cat ::k keyword? ::v any?)) ::kvs->map mspec#)
       (fn [] (gen/fmap (fn [m#] (apply concat m#)) (gen mspec#))))))

(§ defn ^:skip-wiki nonconforming
  "takes a spec and returns a spec that has the same properties except
  'conform' returns the original (not the conformed) value. Note, will specize regex ops."
  [spec]
  (let [spec (delay (specize spec))]
    (reify
     Specize
     (specize* [s] s)
     (specize* [s _] s)

     Spec
     (conform* [_ x] (let [ret (conform* @spec x)]
                       (if (invalid? ret)
                         ::invalid
                         x)))
     (unform* [_ x] x)
     (explain* [_ path via in x] (explain* @spec path via in x))
     (gen* [_ overrides path rmap] (gen* @spec overrides path rmap))
     (with-gen* [_ gfn] (nonconforming (with-gen* @spec gfn)))
     (describe* [_] `(nonconforming ~(describe* @spec))))))

(§ defn ^:skip-wiki nilable-impl
  "Do not call this directly, use 'nilable'"
  [form pred gfn]
  (let [spec (delay (specize pred form))]
    (reify
     Specize
     (specize* [s] s)
     (specize* [s _] s)

     Spec
     (conform* [_ x] (if (nil? x) nil (conform* @spec x)))
     (unform* [_ x] (if (nil? x) nil (unform* @spec x)))
     (explain* [_ path via in x]
               (when-not (c/or (pvalid? @spec x) (nil? x))
                 (conj
                   (explain-1 form pred (conj path ::pred) via in x)
                   {:path (conj path ::nil) :pred 'nil? :val x :via via :in in})))
     (gen* [_ overrides path rmap]
           (if gfn
             (gfn)
             (gen/frequency
               [[1 (gen/delay (gen/return nil))]
                [9 (gen/delay (gensub pred overrides (conj path ::pred) rmap form))]])))
     (with-gen* [_ gfn] (nilable-impl form pred gfn))
     (describe* [_] `(nilable ~(res form))))))

(§ defmacro nilable
  "returns a spec that accepts nil and values satisfying pred"
  [pred]
  (let [pf (res pred)]
    `(nilable-impl '~pf ~pred nil)))

(§ defn exercise
  "generates a number (default 10) of values compatible with spec and maps conform over them,
  returning a sequence of [val conformed-val] tuples. Optionally takes
  a generator overrides map as per gen"
  ([spec] (exercise spec 10))
  ([spec n] (exercise spec n nil))
  ([spec n overrides]
     (map #(vector % (conform spec %)) (gen/sample (gen spec overrides) n))))

(§ defn exercise-fn
  "exercises the fn named by sym (a symbol) by applying it to
  n (default 10) generated samples of its args spec. When fspec is
  supplied its arg spec is used, and sym-or-f can be a fn.  Returns a
  sequence of tuples of [args ret]. "
  ([sym] (exercise-fn sym 10))
  ([sym n] (exercise-fn sym n (get-spec sym)))
  ([sym-or-f n fspec]
     (let [f (if (symbol? sym-or-f) (resolve sym-or-f) sym-or-f)]
       (if-let [arg-spec (c/and fspec (:args fspec))]
         (for [args (gen/sample (gen arg-spec) n)]
           [args (apply f args)])
         (throw (Exception. "No :args spec found, can't generate"))))))

(§ defn inst-in-range?
  "Return true if inst at or after start and before end"
  [start end inst]
  (c/and (inst? inst)
         (let [t (inst-ms inst)]
           (c/and (<= (inst-ms start) t) (< t (inst-ms end))))))

(§ defmacro inst-in
  "Returns a spec that validates insts in the range from start
(§ inclusive) to end (exclusive)."
  [start end]
  `(let [st# (inst-ms ~start)
         et# (inst-ms ~end)
         mkdate# (fn [d#] (java.util.Date. ^{:tag ~'long} d#))]
     (spec (and inst? #(inst-in-range? ~start ~end %))
       :gen (fn []
              (gen/fmap mkdate#
                (gen/large-integer* {:min st# :max et#}))))))

(§ defn int-in-range?
  "Return true if start <= val, val < end and val is a fixed
  precision integer."
  [start end val]
  (c/and int? (<= start val) (< val end)))

(§ defmacro int-in
  "Returns a spec that validates fixed precision integers in the
  range from start (inclusive) to end (exclusive)."
  [start end]
  `(spec (and int? #(int-in-range? ~start ~end %))
     :gen #(gen/large-integer* {:min ~start :max (dec ~end)})))

(§ defmacro double-in
  "Specs a 64-bit floating point number. Options:

    :infinite? - whether +/- infinity allowed (default true)
    :NaN?      - whether NaN allowed (default true)
    :min       - minimum value (inclusive, default none)
    :max       - maximum value (inclusive, default none)"
  [& {:keys [infinite? NaN? min max]
    :or {infinite? true NaN? true}
    :as m}]
  `(spec (and c/double?
              ~@(when-not infinite? '[#(not (Double/isInfinite %))])
              ~@(when-not NaN? '[#(not (Double/isNaN %))])
              ~@(when max `[#(<= % ~max)])
              ~@(when min `[#(<= ~min %)]))
         :gen #(gen/double* ~m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; assert ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(§ defonce
  ^{:dynamic true
    :doc "If true, compiler will enable spec asserts, which are then
subject to runtime control via check-asserts? If false, compiler
will eliminate all spec assert overhead. See 'assert'.

Initially set to boolean value of cloiure.spec.compile-asserts
system property. Defaults to true."}
  *compile-asserts*
  (not= "false" (System/getProperty "cloiure.spec.compile-asserts")))

(§ defn check-asserts?
  "Returns the value set by check-asserts."
  []
  cloiure.lang.RT/checkSpecAsserts)

(§ defn check-asserts
  "Enable or disable spec asserts that have been compiled
with '*compile-asserts*' true.  See 'assert'.

Initially set to boolean value of cloiure.spec.check-asserts
system property. Defaults to false."
  [flag]
  (set! (. cloiure.lang.RT checkSpecAsserts) flag))

(§ defn assert*
  "Do not call this directly, use 'assert'."
  [spec x]
  (if (valid? spec x)
    x
    (let [ed (c/merge (assoc (explain-data* spec [] [] [] x)
                        ::failure :assertion-failed))]
      (throw (ex-info
              (str "Spec assertion failed\n" (with-out-str (explain-out ed)))
              ed)))))

(§ defmacro assert
  "spec-checking assert expression. Returns x if x is valid? according
to spec, else throws an ex-info with explain-data plus ::failure of
:assertion-failed.

Can be disabled at either compile time or runtime:

If *compile-asserts* is false at compile time, compiles to x. Defaults
to value of 'cloiure.spec.compile-asserts' system property, or true if
not set.

If (check-asserts?) is false at runtime, always returns x. Defaults to
value of 'cloiure.spec.check-asserts' system property, or false if not
set. You can toggle check-asserts? with (check-asserts bool)."
  [spec x]
  (if *compile-asserts*
    `(if cloiure.lang.RT/checkSpecAsserts
       (assert* ~spec ~x)
       ~x)
    x))

#_(ns cloiure.spec.gen.alpha
    (:refer-cloiure :exclude [boolean bytes cat hash-map list map not-empty set vector
                              char double int keyword symbol string uuid delay]))

(§ alias 'c 'cloiure.core)

(§ defn- dynaload
  [s]
  (let [ns (namespace s)]
    (assert ns)
    (require (c/symbol ns))
    (let [v (resolve s)]
      (if v
        @v
        (throw (RuntimeException. (str "Var " s " is not on the classpath")))))))

(§ def ^:private quick-check-ref
     (c/delay (dynaload 'cloiure.test.check/quick-check)))
(§ defn quick-check
  [& args]
  (apply @quick-check-ref args))

(§ def ^:private for-all*-ref
     (c/delay (dynaload 'cloiure.test.check.properties/for-all*)))
(§ defn for-all*
  "Dynamically loaded cloiure.test.check.properties/for-all*."
  [& args]
  (apply @for-all*-ref args))

(§ let [g? (c/delay (dynaload 'cloiure.test.check.generators/generator?))
      g (c/delay (dynaload 'cloiure.test.check.generators/generate))
      mkg (c/delay (dynaload 'cloiure.test.check.generators/->Generator))]
  (defn- generator?
    [x]
    (@g? x))
  (defn- generator
    [gfn]
    (@mkg gfn))
  (defn generate
    "Generate a single value using generator."
    [generator]
    (@g generator)))

(§ defn ^:skip-wiki delay-impl
  [gfnd]
  ;;N.B. depends on test.check impl details
  (generator (fn [rnd size]
               ((:gen @gfnd) rnd size))))

(§ defmacro delay
  "given body that returns a generator, returns a
  generator that delegates to that, but delays
  creation until used."
  [& body]
  `(delay-impl (c/delay ~@body)))

(§ defn gen-for-name
  "Dynamically loads test.check generator named s."
  [s]
  (let [g (dynaload s)]
    (if (generator? g)
      g
      (throw (RuntimeException. (str "Var " s " is not a generator"))))))

(§ defmacro ^:skip-wiki lazy-combinator
  "Implementation macro, do not call directly."
  [s]
  (let [fqn (c/symbol "cloiure.test.check.generators" (name s))
        doc (str "Lazy loaded version of " fqn)]
    `(let [g# (c/delay (dynaload '~fqn))]
       (defn ~s
         ~doc
         [& ~'args]
         (apply @g# ~'args)))))

(§ defmacro ^:skip-wiki lazy-combinators
  "Implementation macro, do not call directly."
  [& syms]
  `(do
     ~@(c/map
        (fn [s] (c/list 'lazy-combinator s))
        syms)))

(§ lazy-combinators hash-map list map not-empty set vector vector-distinct fmap elements
                  bind choose fmap one-of such-that tuple sample return
                  large-integer* double* frequency)

(§ defmacro ^:skip-wiki lazy-prim
  "Implementation macro, do not call directly."
  [s]
  (let [fqn (c/symbol "cloiure.test.check.generators" (name s))
        doc (str "Fn returning " fqn)]
    `(let [g# (c/delay (dynaload '~fqn))]
       (defn ~s
         ~doc
         [& ~'args]
         @g#))))

(§ defmacro ^:skip-wiki lazy-prims
  "Implementation macro, do not call directly."
  [& syms]
  `(do
     ~@(c/map
        (fn [s] (c/list 'lazy-prim s))
        syms)))

(§ lazy-prims any any-printable boolean bytes char char-alpha char-alphanumeric char-ascii double
            int keyword keyword-ns large-integer ratio simple-type simple-type-printable
            string string-ascii string-alphanumeric symbol symbol-ns uuid)

(§ defn cat
  "Returns a generator of a sequence catenated from results of
gens, each of which should generate something sequential."
  [& gens]
  (fmap #(apply concat %)
        (apply tuple gens)))

(§ defn- qualified? [ident] (not (nil? (namespace ident))))

(§ def ^:private
  gen-builtins
  (c/delay
   (let [simple (simple-type-printable)]
     {any? (one-of [(return nil) (any-printable)])
      some? (such-that some? (any-printable))
      number? (one-of [(large-integer) (double)])
      integer? (large-integer)
      int? (large-integer)
      pos-int? (large-integer* {:min 1})
      neg-int? (large-integer* {:max -1})
      nat-int? (large-integer* {:min 0})
      float? (double)
      double? (double)
      boolean? (boolean)
      string? (string-alphanumeric)
      ident? (one-of [(keyword-ns) (symbol-ns)])
      simple-ident? (one-of [(keyword) (symbol)])
      qualified-ident? (such-that qualified? (one-of [(keyword-ns) (symbol-ns)]))
      keyword? (keyword-ns)
      simple-keyword? (keyword)
      qualified-keyword? (such-that qualified? (keyword-ns))
      symbol? (symbol-ns)
      simple-symbol? (symbol)
      qualified-symbol? (such-that qualified? (symbol-ns))
      uuid? (uuid)
      uri? (fmap #(java.net.URI/create (str "http://" % ".com")) (uuid))
      decimal? (fmap #(BigDecimal/valueOf %)
                    (double* {:infinite? false :NaN? false}))
      inst? (fmap #(java.util.Date. %)
                  (large-integer))
      seqable? (one-of [(return nil)
                        (list simple)
                        (vector simple)
                        (map simple simple)
                        (set simple)
                        (string-alphanumeric)])
      indexed? (vector simple)
      map? (map simple simple)
      vector? (vector simple)
      list? (list simple)
      seq? (list simple)
      char? (char)
      set? (set simple)
      nil? (return nil)
      false? (return false)
      true? (return true)
      zero? (return 0)
      rational? (one-of [(large-integer) (ratio)])
      coll? (one-of [(map simple simple)
                     (list simple)
                     (vector simple)
                     (set simple)])
      empty? (elements [nil '() [] {} #{}])
      associative? (one-of [(map simple simple) (vector simple)])
      sequential? (one-of [(list simple) (vector simple)])
      ratio? (such-that ratio? (ratio))
      bytes? (bytes)})))

(§ defn gen-for-pred
  "Given a predicate, returns a built-in generator if one exists."
  [pred]
  (if (set? pred)
    (elements pred)
    (get @gen-builtins pred)))

(§ comment
  (require :reload 'cloiure.spec.gen.alpha)
  (in-ns 'cloiure.spec.gen.alpha)

  ;; combinators, see call to lazy-combinators above for complete list
  (generate (one-of [(gen-for-pred integer?) (gen-for-pred string?)]))
  (generate (such-that #(< 10000 %) (gen-for-pred integer?)))
  (let [reqs {:a (gen-for-pred number?)
              :b (gen-for-pred ratio?)}
        opts {:c (gen-for-pred string?)}]
    (generate (bind (choose 0 (count opts))
                    #(let [args (concat (seq reqs) (shuffle (seq opts)))]
                       (->> args
                            (take (+ % (count reqs)))
                            (mapcat identity)
                            (apply hash-map))))))
  (generate (cat (list (gen-for-pred string?))
                 (list (gen-for-pred ratio?))))

  ;; load your own generator
  (gen-for-name 'cloiure.test.check.generators/int)

  ;; failure modes
  (gen-for-name 'unqualified)
  (gen-for-name 'cloiure.core/+)
  (gen-for-name 'cloiure.core/name-does-not-exist)
  (gen-for-name 'ns.does.not.exist/f)
)

#_(ns cloiure.spec.test.alpha
  (:refer-cloiure :exclude [test])
  (:require
 #_[cloiure.pprint :as pp]
   [cloiure.spec.alpha :as s]
   [cloiure.spec.gen.alpha :as gen]
   [cloiure.string :as str]))

(§ in-ns 'cloiure.spec.test.check)
(§ in-ns 'cloiure.spec.test.alpha)
(§ alias 'stc 'cloiure.spec.test.check)

(§ defn- throwable?
  [x]
  (instance? Throwable x))

(§ defn ->sym
  [x]
  (@#'s/->sym x))

(§ defn- ->var
  [s-or-v]
  (if (var? s-or-v)
    s-or-v
    (let [v (and (symbol? s-or-v) (resolve s-or-v))]
      (if (var? v)
        v
        (throw (IllegalArgumentException. (str (pr-str s-or-v) " does not name a var")))))))

(§ defn- collectionize
  [x]
  (if (symbol? x)
    (list x)
    x))

(§ defn enumerate-namespace
  "Given a symbol naming an ns, or a collection of such symbols,
returns the set of all symbols naming vars in those nses."
  [ns-sym-or-syms]
  (into
   #{}
   (mapcat (fn [ns-sym]
             (map
              (fn [name-sym]
                (symbol (name ns-sym) (name name-sym)))
              (keys (ns-interns ns-sym)))))
   (collectionize ns-sym-or-syms)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; instrument ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ def ^:private ^:dynamic *instrument-enabled*
  "if false, instrumented fns call straight through"
  true)

(§ defn- fn-spec?
  "Fn-spec must include at least :args or :ret specs."
  [m]
  (or (:args m) (:ret m)))

(§ defmacro with-instrument-disabled
  "Disables instrument's checking of calls, within a scope."
  [& body]
  `(binding [*instrument-enabled* nil]
     ~@body))

(§ defn- interpret-stack-trace-element
  "Given the vector-of-syms form of a stacktrace element produced
by e.g. Throwable->map, returns a map form that adds some keys
guessing the original Cloiure names. Returns a map with

  :class         class name symbol from stack trace
  :method        method symbol from stack trace
  :file          filename from stack trace
  :line          line number from stack trace
  :var-scope     optional Cloiure var symbol scoping fn def
  :local-fn      optional local Cloiure symbol scoping fn def

For non-Cloiure fns, :scope and :local-fn will be absent."
  [[cls method file line]]
  (let [cloiure? (contains? '#{invoke invokeStatic} method)
        demunge #(cloiure.lang.Compiler/demunge %)
        degensym #(str/replace % #"--.*" "")
        [ns-sym name-sym local] (when cloiure?
                                  (->> (str/split (str cls) #"\$" 3)
                                       (map demunge)))]
    (merge {:file file
            :line line
            :method method
            :class cls}
           (when (and ns-sym name-sym)
             {:var-scope (symbol ns-sym name-sym)})
           (when local
             {:local-fn (symbol (degensym local))}))))

(§ defn- stacktrace-relevant-to-instrument
  "Takes a coll of stack trace elements (as returned by
StackTraceElement->vec) and returns a coll of maps as per
interpret-stack-trace-element that are relevant to a
failure in instrument."
  [elems]
  (let [plumbing? (fn [{:keys [var-scope]}]
                    (contains? '#{cloiure.spec.test.alpha/spec-checking-fn} var-scope))]
    (sequence (comp (map StackTraceElement->vec)
                    (map interpret-stack-trace-element)
                    (filter :var-scope)
                    (drop-while plumbing?))
              elems)))

(§ defn- spec-checking-fn
  [v f fn-spec]
  (let [fn-spec (@#'s/maybe-spec fn-spec)
        conform! (fn [v role spec data args]
                   (let [conformed (s/conform spec data)]
                     (if (= (§ :spec :s/invalid) conformed)
                       (let [caller (->> (.getStackTrace (Thread/currentThread))
                                         stacktrace-relevant-to-instrument
                                         first)
                             ed (merge (assoc (s/explain-data* spec [role] [] [] data)
                                         (§ :spec :s/args) args
                                         (§ :spec :s/failure) :instrument)
                                       (when caller
                                         {::caller (dissoc caller :class :method)}))]
                         (throw (ex-info
                                 (str "Call to " v " did not conform to spec:\n" (with-out-str (s/explain-out ed)))
                                 ed)))
                       conformed)))]
    (fn
     [& args]
     (if *instrument-enabled*
       (with-instrument-disabled
         (when (:args fn-spec) (conform! v :args (:args fn-spec) args args))
         (binding [*instrument-enabled* true]
           (.applyTo ^cloiure.lang.IFn f args)))
       (.applyTo ^cloiure.lang.IFn f args)))))

(§ defn- no-fspec
  [v spec]
  (ex-info (str "Fn at " v " is not spec'ed.")
           {:var v :spec spec (§ :spec :s/failure) :no-fspec}))

(§ defonce ^:private instrumented-vars (atom {}))

(§ defn- instrument-choose-fn
  "Helper for instrument."
  [f spec sym {over :gen :keys [stub replace]}]
  (if (some #{sym} stub)
    (-> spec (s/gen over) gen/generate)
    (get replace sym f)))

(§ defn- instrument-choose-spec
  "Helper for instrument"
  [spec sym {overrides :spec}]
  (get overrides sym spec))

(§ defn- instrument-1
  [s opts]
  (when-let [v (resolve s)]
    (when-not (-> v meta :macro)
      (let [spec (s/get-spec v)
            {:keys [raw wrapped]} (get @instrumented-vars v)
            current @v
            to-wrap (if (= wrapped current) raw current)
            ospec (or (instrument-choose-spec spec s opts)
                      (throw (no-fspec v spec)))
            ofn (instrument-choose-fn to-wrap ospec s opts)
            checked (spec-checking-fn v ofn ospec)]
        (alter-var-root v (constantly checked))
        (swap! instrumented-vars assoc v {:raw to-wrap :wrapped checked})
        (->sym v)))))

(§ defn- unstrument-1
  [s]
  (when-let [v (resolve s)]
    (when-let [{:keys [raw wrapped]} (get @instrumented-vars v)]
      (swap! instrumented-vars dissoc v)
      (let [current @v]
        (when (= wrapped current)
          (alter-var-root v (constantly raw))
          (->sym v))))))

(§ defn- opt-syms
  "Returns set of symbols referenced by 'instrument' opts map"
  [opts]
  (reduce into #{} [(:stub opts) (keys (:replace opts)) (keys (:spec opts))]))

(§ defn- fn-spec-name?
  [s]
  (and (symbol? s)
       (not (some-> (resolve s) meta :macro))))

(§ defn instrumentable-syms
  "Given an opts map as per instrument, returns the set of syms
that can be instrumented."
  ([] (instrumentable-syms nil))
  ([opts]
     (assert (every? ident? (keys (:gen opts))) "instrument :gen expects ident keys")
     (reduce into #{} [(filter fn-spec-name? (keys (s/registry)))
                       (keys (:spec opts))
                       (:stub opts)
                       (keys (:replace opts))])))

(§ defn instrument
  "Instruments the vars named by sym-or-syms, a symbol or collection
of symbols, or all instrumentable vars if sym-or-syms is not
specified.

If a var has an :args fn-spec, sets the var's root binding to a
fn that checks arg conformance (throwing an exception on failure)
before delegating to the original fn.

The opts map can be used to override registered specs, and/or to
replace fn implementations entirely. Opts for symbols not included
in sym-or-syms are ignored. This facilitates sharing a common
options map across many different calls to instrument.

The opts map may have the following keys:

  :spec     a map from var-name symbols to override specs
  :stub     a set of var-name symbols to be replaced by stubs
  :gen      a map from spec names to generator overrides
  :replace  a map from var-name symbols to replacement fns

:spec overrides registered fn-specs with specs your provide. Use
:spec overrides to provide specs for libraries that do not have
them, or to constrain your own use of a fn to a subset of its
spec'ed contract.

:stub replaces a fn with a stub that checks :args, then uses the
:ret spec to generate a return value.

:gen overrides are used only for :stub generation.

:replace replaces a fn with a fn that checks args conformance, then
invokes the fn you provide, enabling arbitrary stubbing and mocking.

:spec can be used in combination with :stub or :replace.

Returns a collection of syms naming the vars instrumented."
  ([] (instrument (instrumentable-syms)))
  ([sym-or-syms] (instrument sym-or-syms nil))
  ([sym-or-syms opts]
     (locking instrumented-vars
       (into
        []
        (comp (filter (instrumentable-syms opts))
              (distinct)
              (map #(instrument-1 % opts))
              (remove nil?))
        (collectionize sym-or-syms)))))

(§ defn unstrument
  "Undoes instrument on the vars named by sym-or-syms, specified
as in instrument. With no args, unstruments all instrumented vars.
Returns a collection of syms naming the vars unstrumented."
  ([] (unstrument (map ->sym (keys @instrumented-vars))))
  ([sym-or-syms]
     (locking instrumented-vars
       (into
        []
        (comp (filter symbol?)
              (map unstrument-1)
              (remove nil?))
        (collectionize sym-or-syms)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; testing  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(§ defn- explain-check
  [args spec v role]
  (ex-info
   "Specification-based check failed"
   (when-not (s/valid? spec v nil)
     (assoc (s/explain-data* spec [role] [] [] v)
       ::args args
       ::val v
       (§ :spec :s/failure) :check-failed))))

(§ defn- check-call
  "Returns true if call passes specs, otherwise *returns* an exception
with explain-data + ::s/failure."
  [f specs args]
  (let [cargs (when (:args specs) (s/conform (:args specs) args))]
    (if (= cargs (§ :spec :s/invalid))
      (explain-check args (:args specs) args :args)
      (let [ret (apply f args)
            cret (when (:ret specs) (s/conform (:ret specs) ret))]
        (if (= cret (§ :spec :s/invalid))
          (explain-check args (:ret specs) ret :ret)
          (if (and (:args specs) (:ret specs) (:fn specs))
            (if (s/valid? (:fn specs) {:args cargs :ret cret})
              true
              (explain-check args (:fn specs) {:args cargs :ret cret} :fn))
            true))))))

(§ defn- quick-check
  [f specs {gen :gen opts (§ :spec :stc/opts)}]
  (let [{:keys [num-tests] :or {num-tests 1000}} opts
        g (try (s/gen (:args specs) gen) (catch Throwable t t))]
    (if (throwable? g)
      {:result g}
      (let [prop (gen/for-all* [g] #(check-call f specs %))]
        (apply gen/quick-check num-tests prop (mapcat identity opts))))))

(§ defn- make-check-result
  "Builds spec result map."
  [check-sym spec test-check-ret]
  (merge {:spec spec
          (§ :spec :stc/ret) test-check-ret}
         (when check-sym
           {:sym check-sym})
         (when-let [result (-> test-check-ret :result)]
           (when-not (true? result) {:failure result}))
         (when-let [shrunk (-> test-check-ret :shrunk)]
           {:failure (:result shrunk)})))

(§ defn- check-1
  [{:keys [s f v spec]} opts]
  (let [re-inst? (and v (seq (unstrument s)) true)
        f (or f (when v @v))
        specd (s/spec spec)]
    (try
     (cond
      (or (nil? f) (some-> v meta :macro))
      {:failure (ex-info "No fn to spec" {(§ :spec :s/failure) :no-fn})
       :sym s :spec spec}

      (:args specd)
      (let [tcret (quick-check f specd opts)]
        (make-check-result s spec tcret))

      :default
      {:failure (ex-info "No :args spec" {(§ :spec :s/failure) :no-args-spec})
       :sym s :spec spec})
     (finally
      (when re-inst? (instrument s))))))

(§ defn- sym->check-map
  [s]
  (let [v (resolve s)]
    {:s s
     :v v
     :spec (when v (s/get-spec v))}))

(§ defn- validate-check-opts
  [opts]
  (assert (every? ident? (keys (:gen opts))) "check :gen expects ident keys"))

(§ defn check-fn
  "Runs generative tests for fn f using spec and opts. See
'check' for options and return."
  ([f spec] (check-fn f spec nil))
  ([f spec opts]
     (validate-check-opts opts)
     (check-1 {:f f :spec spec} opts)))

(§ defn checkable-syms
  "Given an opts map as per check, returns the set of syms that
can be checked."
  ([] (checkable-syms nil))
  ([opts]
     (validate-check-opts opts)
     (reduce into #{} [(filter fn-spec-name? (keys (s/registry)))
                       (keys (:spec opts))])))

(§ defn check
  "Run generative tests for spec conformance on vars named by
sym-or-syms, a symbol or collection of symbols. If sym-or-syms
is not specified, check all checkable vars.

The opts map includes the following optional keys, where stc
aliases cloiure.spec.test.check:

::stc/opts  opts to flow through test.check/quick-check
:gen        map from spec names to generator overrides

The ::stc/opts include :num-tests in addition to the keys
documented by test.check. Generator overrides are passed to
spec/gen when generating function args.

Returns a lazy sequence of check result maps with the following
keys

:spec       the spec tested
:sym        optional symbol naming the var tested
:failure    optional test failure
::stc/ret   optional value returned by test.check/quick-check

The value for :failure can be any exception. Exceptions thrown by
spec itself will have an ::s/failure value in ex-data:

:check-failed   at least one checked return did not conform
:no-args-spec   no :args spec provided
:no-fn          no fn provided
:no-fspec       no fspec provided
:no-gen         unable to generate :args
:instrument     invalid args detected by instrument
"
  ([] (check (checkable-syms)))
  ([sym-or-syms] (check sym-or-syms nil))
  ([sym-or-syms opts]
     (->> (collectionize sym-or-syms)
          (filter (checkable-syms opts))
          (pmap
           #(check-1 (sym->check-map %) opts)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; check reporting  ;;;;;;;;;;;;;;;;;;;;;;;;

(§ defn- failure-type
  [x]
  ((§ :spec :s/failure) (ex-data x)))

(§ defn- unwrap-failure
  [x]
  (if (failure-type x)
    (ex-data x)
    x))

(§ defn- result-type
  "Returns the type of the check result. This can be any of the
::s/failure keywords documented in 'check', or:

  :check-passed   all checked fn returns conformed
  :check-threw    checked fn threw an exception"
  [ret]
  (let [failure (:failure ret)]
    (cond
     (nil? failure) :check-passed
     (failure-type failure) (failure-type failure)
     :default :check-threw)))

(§ defn abbrev-result
  "Given a check result, returns an abbreviated version
suitable for summary use."
  [x]
  (if (:failure x)
    (-> (dissoc x (§ :spec :stc/ret))
        (update :spec s/describe)
        (update :failure unwrap-failure))
    (dissoc x :spec (§ :spec :stc/ret))))

(§ defn summarize-results
  "Given a collection of check-results, e.g. from 'check', pretty
prints the summary-result (default abbrev-result) of each.

Returns a map with :total, the total number of results, plus a
key with a count for each different :type of result."
  ([check-results] (summarize-results check-results abbrev-result))
  ([check-results summary-result]
     (reduce
      (fn [summary result]
      #_(pp/pprint (summary-result result))
        (-> summary
            (update :total inc)
            (update (result-type result) (fnil inc 0))))
      {:total 0}
       check-results)))

#_(ns ^{:doc "Print stack traces oriented towards Cloiure, not Java."
       :author "Stuart Sierra"}
  cloiure.stacktrace)

(§ defn root-cause
  "Returns the last 'cause' Throwable in a chain of Throwables."
  {:added "1.1"}
  [tr]
  (if-let [cause (.getCause tr)]
    (recur cause)
    tr))

(§ defn print-trace-element
  "Prints a Cloiure-oriented view of one element in a stack trace."
  {:added "1.1"}
  [e]
  (let [class (.getClassName e)
        method (.getMethodName e)]
    (let [match (re-matches #"^([A-Za-z0-9_.-]+)\$(\w+)__\d+$" (str class))]
      (if (and match (= "invoke" method))
        (apply printf "%s/%s" (rest match))
        (printf "%s.%s" class method))))
  (printf " (%s:%d)" (or (.getFileName e) "") (.getLineNumber e)))

(§ defn print-throwable
  "Prints the class and message of a Throwable."
  {:added "1.1"}
  [tr]
  (printf "%s: %s" (.getName (class tr)) (.getMessage tr)))

(§ defn print-stack-trace
  "Prints a Cloiure-oriented stack trace of tr, a Throwable.
  Prints a maximum of n stack frames (default: unlimited).
  Does not print chained exceptions (causes)."
  {:added "1.1"}
  ([tr] (print-stack-trace tr nil))
  ([^Throwable tr n]
     (let [st (.getStackTrace tr)]
       (print-throwable tr)
       (newline)
       (print " at ")
       (if-let [e (first st)]
         (print-trace-element e)
         (print "[empty stack trace]"))
       (newline)
       (doseq [e (if (nil? n)
                   (rest st)
                   (take (dec n) (rest st)))]
         (print "    ")
         (print-trace-element e)
         (newline)))))

(§ defn print-cause-trace
  "Like print-stack-trace but prints chained exceptions (causes)."
  {:added "1.1"}
  ([tr] (print-cause-trace tr nil))
  ([tr n]
     (print-stack-trace tr n)
     (when-let [cause (.getCause tr)]
       (print "Caused by: " )
       (recur cause n))))

(§ defn e
  "REPL utility.  Prints a brief stack trace for the root cause of the
  most recent exception."
  {:added "1.1"}
  []
  (print-stack-trace (root-cause *e) 8))

#_(ns ^{:doc "Cloiure String utilities

It is poor form to (:use cloiure.string). Instead, use require
with :as to specify a prefix, e.g.

(§ ns your.namespace.here
  (:require [cloiure.string :as str]))

Design notes for cloiure.string:

1. Strings are objects (as opposed to sequences). As such, the
   string being manipulated is the first argument to a function;
   passing nil will result in a NullPointerException unless
   documented otherwise. If you want sequence-y behavior instead,
   use a sequence.

2. Functions are generally not lazy, and call straight to host
   methods where those are available and efficient.

3. Functions take advantage of String implementation details to
   write high-performing loop/recurs instead of using higher-order
   functions. (This is not idiomatic in general-purpose application
   code.)

4. When a function is documented to accept a string argument, it
   will take any implementation of the correct *interface* on the
   host platform. In Java, this is CharSequence, which is more
   general than String. In ordinary usage you will almost always
   pass concrete strings. If you are doing something unusual,
   e.g. passing a mutable implementation of CharSequence, then
   thread-safety is your responsibility."
      :author "Stuart Sierra, Stuart Halloway, David Liebke"}
  cloiure.string
  (:refer-cloiure :exclude (replace reverse))
  (:import (java.util.regex Pattern Matcher)
           cloiure.lang.LazilyPersistentVector))

(§ set! *warn-on-reflection* true)

(§ defn ^String reverse
  "Returns s with its characters reversed."
  {:added "1.2"}
  [^CharSequence s]
  (.toString (.reverse (StringBuilder. s))))

(§ defn ^String re-quote-replacement
  "Given a replacement string that you wish to be a literal
   replacement for a pattern match in replace or replace-first, do the
   necessary escaping of special characters in the replacement."
  {:added "1.5"}
  [^CharSequence replacement]
  (Matcher/quoteReplacement (.toString ^CharSequence replacement)))

(§ defn- replace-by
  [^CharSequence s re f]
  (let [m (re-matcher re s)]
    (if (.find m)
      (let [buffer (StringBuffer. (.length s))]
        (loop [found true]
          (if found
            (do (.appendReplacement m buffer (Matcher/quoteReplacement (f (re-groups m))))
                (recur (.find m)))
            (do (.appendTail m buffer)
                (.toString buffer)))))
      s)))

(§ defn ^String replace
  "Replaces all instance of match with replacement in s.

   match/replacement can be:

   string / string
   char / char
   pattern / (string or function of match).

   See also replace-first.

   The replacement is literal (i.e. none of its characters are treated
   specially) for all cases above except pattern / string.

   For pattern / string, $1, $2, etc. in the replacement string are
   substituted with the string that matched the corresponding
   parenthesized group in the pattern.  If you wish your replacement
   string r to be used literally, use (re-quote-replacement r) as the
   replacement argument.  See also documentation for
   java.util.regex.Matcher's appendReplacement method.

   Example:
   (cloiure.string/replace \"Almost Pig Latin\" #\"\\b(\\w)(\\w+)\\b\" \"$2$1ay\")
   -> \"lmostAay igPay atinLay\""
  {:added "1.2"}
  [^CharSequence s match replacement]
  (let [s (.toString s)]
    (cond
     (instance? Character match) (.replace s ^Character match ^Character replacement)
     (instance? CharSequence match) (.replace s ^CharSequence match ^CharSequence replacement)
     (instance? Pattern match) (if (instance? CharSequence replacement)
                                 (.replaceAll (re-matcher ^Pattern match s)
                                              (.toString ^CharSequence replacement))
                                 (replace-by s match replacement))
     :else (throw (IllegalArgumentException. (str "Invalid match arg: " match))))))

(§ defn- replace-first-by
  [^CharSequence s ^Pattern re f]
  (let [m (re-matcher re s)]
    (if (.find m)
      (let [buffer (StringBuffer. (.length s))
            rep (Matcher/quoteReplacement (f (re-groups m)))]
        (.appendReplacement m buffer rep)
        (.appendTail m buffer)
        (str buffer))
      s)))

(§ defn- replace-first-char
  [^CharSequence s ^Character match replace]
  (let [s (.toString s)
        i (.indexOf s (int match))]
    (if (= -1 i)
      s
      (str (subs s 0 i) replace (subs s (inc i))))))

(§ defn- replace-first-str
  [^CharSequence s ^String match ^String replace]
  (let [^String s (.toString s)
        i (.indexOf s match)]
    (if (= -1 i)
      s
      (str (subs s 0 i) replace (subs s (+ i (.length match)))))))

(§ defn ^String replace-first
  "Replaces the first instance of match with replacement in s.

   match/replacement can be:

   char / char
   string / string
   pattern / (string or function of match).

   See also replace.

   The replacement is literal (i.e. none of its characters are treated
   specially) for all cases above except pattern / string.

   For pattern / string, $1, $2, etc. in the replacement string are
   substituted with the string that matched the corresponding
   parenthesized group in the pattern.  If you wish your replacement
   string r to be used literally, use (re-quote-replacement r) as the
   replacement argument.  See also documentation for
   java.util.regex.Matcher's appendReplacement method.

   Example:
   (cloiure.string/replace-first \"swap first two words\"
                                 #\"(\\w+)(\\s+)(\\w+)\" \"$3$2$1\")
   -> \"first swap two words\""
  {:added "1.2"}
  [^CharSequence s match replacement]
  (let [s (.toString s)]
    (cond
     (instance? Character match)
     (replace-first-char s match replacement)
     (instance? CharSequence match)
     (replace-first-str s (.toString ^CharSequence match)
                        (.toString ^CharSequence replacement))
     (instance? Pattern match)
     (if (instance? CharSequence replacement)
       (.replaceFirst (re-matcher ^Pattern match s)
                      (.toString ^CharSequence replacement))
       (replace-first-by s match replacement))
     :else (throw (IllegalArgumentException. (str "Invalid match arg: " match))))))

(§ defn ^String join
  "Returns a string of all elements in coll, as returned by (seq coll),
   separated by an optional separator."
  {:added "1.2"}
  ([coll]
     (apply str coll))
  ([separator coll]
     (loop [sb (StringBuilder. (str (first coll)))
            more (next coll)
            sep (str separator)]
       (if more
         (recur (-> sb (.append sep) (.append (str (first more))))
                (next more)
                sep)
         (str sb)))))

(§ defn ^String capitalize
  "Converts first character of the string to upper-case, all other
  characters to lower-case."
  {:added "1.2"}
  [^CharSequence s]
  (let [s (.toString s)]
    (if (< (count s) 2)
      (.toUpperCase s)
      (str (.toUpperCase (subs s 0 1))
           (.toLowerCase (subs s 1))))))

(§ defn ^String upper-case
  "Converts string to all upper-case."
  {:added "1.2"}
  [^CharSequence s]
  (.. s toString toUpperCase))

(§ defn ^String lower-case
  "Converts string to all lower-case."
  {:added "1.2"}
  [^CharSequence s]
  (.. s toString toLowerCase))

(§ defn split
  "Splits string on a regular expression.  Optional argument limit is
  the maximum number of splits. Not lazy. Returns vector of the splits."
  {:added "1.2"}
  ([^CharSequence s ^Pattern re]
     (LazilyPersistentVector/createOwning (.split re s)))
  ([ ^CharSequence s ^Pattern re limit]
     (LazilyPersistentVector/createOwning (.split re s limit))))

(§ defn split-lines
  "Splits s on \\n or \\r\\n."
  {:added "1.2"}
  [^CharSequence s]
  (split s #"\r?\n"))

(§ defn ^String trim
  "Removes whitespace from both ends of string."
  {:added "1.2"}
  [^CharSequence s]
  (let [len (.length s)]
    (loop [rindex len]
      (if (zero? rindex)
        ""
        (if (Character/isWhitespace (.charAt s (dec rindex)))
          (recur (dec rindex))
          ;; there is at least one non-whitespace char in the string,
          ;; so no need to check for lindex reaching len.
          (loop [lindex 0]
            (if (Character/isWhitespace (.charAt s lindex))
              (recur (inc lindex))
              (.. s (subSequence lindex rindex) toString))))))))

(§ defn ^String triml
  "Removes whitespace from the left side of string."
  {:added "1.2"}
  [^CharSequence s]
  (let [len (.length s)]
    (loop [index 0]
      (if (= len index)
        ""
        (if (Character/isWhitespace (.charAt s index))
          (recur (unchecked-inc index))
          (.. s (subSequence index len) toString))))))

(§ defn ^String trimr
  "Removes whitespace from the right side of string."
  {:added "1.2"}
  [^CharSequence s]
  (loop [index (.length s)]
    (if (zero? index)
      ""
      (if (Character/isWhitespace (.charAt s (unchecked-dec index)))
        (recur (unchecked-dec index))
        (.. s (subSequence 0 index) toString)))))

(§ defn ^String trim-newline
  "Removes all trailing newline \\n or return \\r characters from
  string.  Similar to Perl's chomp."
  {:added "1.2"}
  [^CharSequence s]
  (loop [index (.length s)]
    (if (zero? index)
      ""
      (let [ch (.charAt s (dec index))]
        (if (or (= ch \newline) (= ch \return))
          (recur (dec index))
          (.. s (subSequence 0 index) toString))))))

(§ defn blank?
  "True if s is nil, empty, or contains only whitespace."
  {:added "1.2"}
  [^CharSequence s]
  (if s
    (loop [index (int 0)]
      (if (= (.length s) index)
        true
        (if (Character/isWhitespace (.charAt s index))
          (recur (inc index))
          false)))
    true))

(§ defn ^String escape
  "Return a new string, using cmap to escape each character ch
   from s as follows:

   If (cmap ch) is nil, append ch to the new string.
   If (cmap ch) is non-nil, append (str (cmap ch)) instead."
  {:added "1.2"}
  [^CharSequence s cmap]
  (loop [index (int 0)
         buffer (StringBuilder. (.length s))]
    (if (= (.length s) index)
      (.toString buffer)
      (let [ch (.charAt s index)]
        (if-let [replacement (cmap ch)]
          (.append buffer replacement)
          (.append buffer ch))
        (recur (inc index) buffer)))))

(§ defn index-of
  "Return index of value (string or char) in s, optionally searching
  forward from from-index. Return nil if value not found."
  {:added "1.8"}
  ([^CharSequence s value]
  (let [result ^long
        (if (instance? Character value)
          (.indexOf (.toString s) ^int (.charValue ^Character value))
          (.indexOf (.toString s) ^String value))]
    (if (= result -1)
      nil
      result)))
  ([^CharSequence s value ^long from-index]
  (let [result ^long
        (if (instance? Character value)
          (.indexOf (.toString s) ^int (.charValue ^Character value) (unchecked-int from-index))
          (.indexOf (.toString s) ^String value (unchecked-int from-index)))]
    (if (= result -1)
      nil
      result))))

(§ defn last-index-of
  "Return last index of value (string or char) in s, optionally
  searching backward from from-index. Return nil if value not found."
  {:added "1.8"}
  ([^CharSequence s value]
  (let [result ^long
        (if (instance? Character value)
          (.lastIndexOf (.toString s) ^int (.charValue ^Character value))
          (.lastIndexOf (.toString s) ^String value))]
    (if (= result -1)
      nil
      result)))
  ([^CharSequence s value ^long from-index]
  (let [result ^long
        (if (instance? Character value)
          (.lastIndexOf (.toString s) ^int (.charValue ^Character value) (unchecked-int from-index))
          (.lastIndexOf (.toString s) ^String value (unchecked-int from-index)))]
    (if (= result -1)
      nil
      result))))

(§ defn starts-with?
  "True if s starts with substr."
  {:added "1.8"}
  [^CharSequence s ^String substr]
  (.startsWith (.toString s) substr))

(§ defn ends-with?
  "True if s ends with substr."
  {:added "1.8"}
  [^CharSequence s ^String substr]
  (.endsWith (.toString s) substr))

(§ defn includes?
  "True if s includes substr."
  {:added "1.8"}
  [^CharSequence s ^CharSequence substr]
  (.contains (.toString s) substr))

#_(ns cloiure.uuid)

(§ defn- default-uuid-reader [form]
  {:pre [(string? form)]}
  (java.util.UUID/fromString form))

(§ defmethod print-method java.util.UUID [uuid ^java.io.Writer w]
  (.write w (str "#uuid \"" (str uuid) "\"")))

(§ defmethod print-dup java.util.UUID [o w]
  (print-method o w))
(§ ns
  ^{:author "Stuart Sierra",
     :doc "This file defines a generic tree walker for Cloiure data
structures.  It takes any data structure (list, vector, map, set,
seq), calls a function on every element, and uses the return value
of the function in place of the original.  This makes it fairly
easy to write recursive search-and-replace functions, as shown in
the examples.

Note: \"walk\" supports all Cloiure data structures EXCEPT maps
created with sorted-map-by.  There is no (obvious) way to retrieve
the sorting function."}
  cloiure.walk)

(§ defn walk
  "Traverses form, an arbitrary data structure.  inner and outer are
  functions.  Applies inner to each element of form, building up a
  data structure of the same type, then applies outer to the result.
  Recognizes all Cloiure data structures. Consumes seqs as with doall."

  {:added "1.1"}
  [inner outer form]
  (cond
   (list? form) (outer (apply list (map inner form)))
   (instance? cloiure.lang.IMapEntry form) (outer (vec (map inner form)))
   (seq? form) (outer (doall (map inner form)))
   (instance? cloiure.lang.IRecord form)
     (outer (reduce (fn [r x] (conj r (inner x))) form form))
   (coll? form) (outer (into (empty form) (map inner form)))
   :else (outer form)))

(§ defn postwalk
  "Performs a depth-first, post-order traversal of form.  Calls f on
  each sub-form, uses f's return value in place of the original.
  Recognizes all Cloiure data structures. Consumes seqs as with doall."
  {:added "1.1"}
  [f form]
  (walk (partial postwalk f) f form))

(§ defn prewalk
  "Like postwalk, but does pre-order traversal."
  {:added "1.1"}
  [f form]
  (walk (partial prewalk f) identity (f form)))

;; Note: I wanted to write:
;;
;; (defn walk
;;   [f form]
;;   (let [pf (partial walk f)]
;;     (if (coll? form)
;;       (f (into (empty form) (map pf form)))
;;       (f form))))
;;
;; but this throws a ClassCastException when applied to a map.

(§ defn postwalk-demo
  "Demonstrates the behavior of postwalk by printing each form as it is
  walked.  Returns form."
  {:added "1.1"}
  [form]
  (postwalk (fn [x] (print "Walked: ") (prn x) x) form))

(§ defn prewalk-demo
  "Demonstrates the behavior of prewalk by printing each form as it is
  walked.  Returns form."
  {:added "1.1"}
  [form]
  (prewalk (fn [x] (print "Walked: ") (prn x) x) form))

(§ defn keywordize-keys
  "Recursively transforms all map keys from strings to keywords."
  {:added "1.1"}
  [m]
  (let [f (fn [[k v]] (if (string? k) [(keyword k) v] [k v]))]
    ;; only apply to maps
    (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(§ defn stringify-keys
  "Recursively transforms all map keys from keywords to strings."
  {:added "1.1"}
  [m]
  (let [f (fn [[k v]] (if (keyword? k) [(name k) v] [k v]))]
    ;; only apply to maps
    (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(§ defn prewalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values.  Like cloiure/replace but works on any data structure.  Does
  replacement at the root of the tree first."
  {:added "1.1"}
  [smap form]
  (prewalk (fn [x] (if (contains? smap x) (smap x) x)) form))

(§ defn postwalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values.  Like cloiure/replace but works on any data structure.  Does
  replacement at the leaves of the tree first."
  {:added "1.1"}
  [smap form]
  (postwalk (fn [x] (if (contains? smap x) (smap x) x)) form))

(§ defn macroexpand-all
  "Recursively performs all possible macroexpansions in form."
  {:added "1.1"}
  [form]
  (prewalk (fn [x] (if (seq? x) (macroexpand x) x)) form))

#_(ns ^{:doc "Functional hierarchical zipper, with navigation, editing,
  and enumeration.  See Huet"
       :author "Rich Hickey"}
  cloiure.zip
  (:refer-cloiure :exclude (replace remove next)))

(§ defn zipper
  "Creates a new zipper structure.

  branch? is a fn that, given a node, returns true if can have
  children, even if it currently doesn't.

  children is a fn that, given a branch node, returns a seq of its
  children.

  make-node is a fn that, given an existing node and a seq of
  children, returns a new branch node with the supplied children.
  root is the root node."
  {:added "1.0"}
  [branch? children make-node root]
    ^{:zip/branch? branch? :zip/children children :zip/make-node make-node}
    [root nil])

(§ defn seq-zip
  "Returns a zipper for nested sequences, given a root sequence"
  {:added "1.0"}
  [root]
    (zipper seq?
            identity
            (fn [node children] (with-meta children (meta node)))
            root))

(§ defn vector-zip
  "Returns a zipper for nested vectors, given a root vector"
  {:added "1.0"}
  [root]
    (zipper vector?
            seq
            (fn [node children] (with-meta (vec children) (meta node)))
            root))

(§ defn node
  "Returns the node at loc"
  {:added "1.0"}
  [loc] (loc 0))

(§ defn branch?
  "Returns true if the node at loc is a branch"
  {:added "1.0"}
  [loc]
    ((:zip/branch? (meta loc)) (node loc)))

(§ defn children
  "Returns a seq of the children of node at loc, which must be a branch"
  {:added "1.0"}
  [loc]
    (if (branch? loc)
      ((:zip/children (meta loc)) (node loc))
      (throw (Exception. "called children on a leaf node"))))

(§ defn make-node
  "Returns a new branch node, given an existing node and new
  children. The loc is only used to supply the constructor."
  {:added "1.0"}
  [loc node children]
    ((:zip/make-node (meta loc)) node children))

(§ defn path
  "Returns a seq of nodes leading to this loc"
  {:added "1.0"}
  [loc]
    (:pnodes (loc 1)))

(§ defn lefts
  "Returns a seq of the left siblings of this loc"
  {:added "1.0"}
  [loc]
    (seq (:l (loc 1))))

(§ defn rights
  "Returns a seq of the right siblings of this loc"
  {:added "1.0"}
  [loc]
    (:r (loc 1)))

(§ defn down
  "Returns the loc of the leftmost child of the node at this loc, or
  nil if no children"
  {:added "1.0"}
  [loc]
    (when (branch? loc)
      (let [[node path] loc
            [c & cnext :as cs] (children loc)]
        (when cs
          (with-meta [c {:l []
                         :pnodes (if path (conj (:pnodes path) node) [node])
                         :ppath path
                         :r cnext}] (meta loc))))))

(§ defn up
  "Returns the loc of the parent of the node at this loc, or nil if at
  the top"
  {:added "1.0"}
  [loc]
    (let [[node {l :l, ppath :ppath, pnodes :pnodes r :r, changed? :changed?, :as path}] loc]
      (when pnodes
        (let [pnode (peek pnodes)]
          (with-meta (if changed?
                       [(make-node loc pnode (concat l (cons node r)))
                        (and ppath (assoc ppath :changed? true))]
                       [pnode ppath])
                     (meta loc))))))

(§ defn root
  "zips all the way up and returns the root node, reflecting any
 changes."
  {:added "1.0"}
  [loc]
    (if (= :end (loc 1))
      (node loc)
      (let [p (up loc)]
        (if p
          (recur p)
          (node loc)))))

(§ defn right
  "Returns the loc of the right sibling of the node at this loc, or nil"
  {:added "1.0"}
  [loc]
    (let [[node {l :l  [r & rnext :as rs] :r :as path}] loc]
      (when (and path rs)
        (with-meta [r (assoc path :l (conj l node) :r rnext)] (meta loc)))))

(§ defn rightmost
  "Returns the loc of the rightmost sibling of the node at this loc, or self"
  {:added "1.0"}
  [loc]
    (let [[node {l :l r :r :as path}] loc]
      (if (and path r)
        (with-meta [(last r) (assoc path :l (apply conj l node (butlast r)) :r nil)] (meta loc))
        loc)))

(§ defn left
  "Returns the loc of the left sibling of the node at this loc, or nil"
  {:added "1.0"}
  [loc]
    (let [[node {l :l r :r :as path}] loc]
      (when (and path (seq l))
        (with-meta [(peek l) (assoc path :l (pop l) :r (cons node r))] (meta loc)))))

(§ defn leftmost
  "Returns the loc of the leftmost sibling of the node at this loc, or self"
  {:added "1.0"}
  [loc]
    (let [[node {l :l r :r :as path}] loc]
      (if (and path (seq l))
        (with-meta [(first l) (assoc path :l [] :r (concat (rest l) [node] r))] (meta loc))
        loc)))

(§ defn insert-left
  "Inserts the item as the left sibling of the node at this loc,
 without moving"
  {:added "1.0"}
  [loc item]
    (let [[node {l :l :as path}] loc]
      (if (nil? path)
        (throw (new Exception "Insert at top"))
        (with-meta [node (assoc path :l (conj l item) :changed? true)] (meta loc)))))

(§ defn insert-right
  "Inserts the item as the right sibling of the node at this loc,
  without moving"
  {:added "1.0"}
  [loc item]
    (let [[node {r :r :as path}] loc]
      (if (nil? path)
        (throw (new Exception "Insert at top"))
        (with-meta [node (assoc path :r (cons item r) :changed? true)] (meta loc)))))

(§ defn replace
  "Replaces the node at this loc, without moving"
  {:added "1.0"}
  [loc node]
    (let [[_ path] loc]
      (with-meta [node (assoc path :changed? true)] (meta loc))))

(§ defn edit
  "Replaces the node at this loc with the value of (f node args)"
  {:added "1.0"}
  [loc f & args]
    (replace loc (apply f (node loc) args)))

(§ defn insert-child
  "Inserts the item as the leftmost child of the node at this loc,
  without moving"
  {:added "1.0"}
  [loc item]
    (replace loc (make-node loc (node loc) (cons item (children loc)))))

(§ defn append-child
  "Inserts the item as the rightmost child of the node at this loc,
  without moving"
  {:added "1.0"}
  [loc item]
    (replace loc (make-node loc (node loc) (concat (children loc) [item]))))

(§ defn next
  "Moves to the next loc in the hierarchy, depth-first. When reaching
  the end, returns a distinguished loc detectable via end?. If already
  at the end, stays there."
  {:added "1.0"}
  [loc]
    (if (= :end (loc 1))
      loc
      (or
       (and (branch? loc) (down loc))
       (right loc)
       (loop [p loc]
         (if (up p)
           (or (right (up p)) (recur (up p)))
           [(node p) :end])))))

(§ defn prev
  "Moves to the previous loc in the hierarchy, depth-first. If already
  at the root, returns nil."
  {:added "1.0"}
  [loc]
    (if-let [lloc (left loc)]
      (loop [loc lloc]
        (if-let [child (and (branch? loc) (down loc))]
          (recur (rightmost child))
          loc))
      (up loc)))

(§ defn end?
  "Returns true if loc represents the end of a depth-first walk"
  {:added "1.0"}
  [loc]
    (= :end (loc 1)))

(§ defn remove
  "Removes the node at loc, returning the loc that would have preceded
  it in a depth-first walk."
  {:added "1.0"}
  [loc]
    (let [[node {l :l, ppath :ppath, pnodes :pnodes, rs :r, :as path}] loc]
      (if (nil? path)
        (throw (new Exception "Remove at top"))
        (if (pos? (count l))
          (loop [loc (with-meta [(peek l) (assoc path :l (pop l) :changed? true)] (meta loc))]
            (if-let [child (and (branch? loc) (down loc))]
              (recur (rightmost child))
              loc))
          (with-meta [(make-node loc (peek pnodes) rs)
                      (and ppath (assoc ppath :changed? true))]
                     (meta loc))))))
