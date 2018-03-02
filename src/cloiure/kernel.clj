(ns #_cloiure.slang cloiure.kernel
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

#_(ns cloiure.kernel
    (:refer-clojure :exclude [when when-not])
    (:use [cloiure slang]))

(import
    [java.io InputStreamReader OutputStreamWriter PrintWriter PushbackReader Reader #_StringReader StringWriter Writer]
  #_[java.lang Character Class Exception IllegalArgumentException IllegalStateException Integer Number NumberFormatException Object RuntimeException String StringBuilder Throwable UnsupportedOperationException]
    [java.lang.ref Reference ReferenceQueue SoftReference WeakReference]
    [java.lang.reflect Array]
    [java.math BigDecimal BigInteger MathContext]
    [java.net URL URLClassLoader]
    [java.security AccessController PrivilegedAction]
    [java.util AbstractCollection AbstractSet ArrayList Collection Comparator EmptyStackException HashMap Iterator LinkedList List Map Map$Entry NoSuchElementException Queue Set Stack]
    [java.util.concurrent Callable ConcurrentHashMap]
    [java.util.concurrent.atomic AtomicBoolean AtomicInteger AtomicReference]
    [java.util.concurrent.locks ReentrantReadWriteLock]
    [java.util.regex Matcher Pattern]
)

(declare Compiler'BOOLEANS_CLASS)
(declare Compiler'BYTES_CLASS)
(declare Compiler'CHARS_CLASS)
(declare Compiler'DOUBLES_CLASS)
(declare Compiler'FLOATS_CLASS)
(declare Compiler'INTS_CLASS)
(declare Compiler'LOADER)
(declare Compiler'LONGS_CLASS)
(declare Compiler'OBJECTS_CLASS)
(declare Compiler'SHORTS_CLASS)
(declare Compiler'load)
(declare PersistentHashMap'EMPTY)
(declare PersistentHashMap'bitpos)
(declare PersistentHashMap'cloneAndSet-3)
(declare PersistentHashMap'cloneAndSet-5)
(declare PersistentHashMap'create-1a)
(declare PersistentHashMap'create-2)
(declare PersistentHashMap'createNode-6)
(declare PersistentHashMap'createNode-7)
(declare PersistentHashMap'hash)
(declare PersistentHashMap'mask)
(declare PersistentHashMap'new)
(declare PersistentHashMap'removePair)
(declare PersistentHashSet'EMPTY)
(declare PersistentList'EMPTY)
(declare PersistentList'create)
(declare PersistentList'new)
(declare PersistentVector'EMPTY)
(declare PersistentVector'new)
(declare PersistentVector'newPath)
(declare Reflector'prepRet)
(declare RT'CLOIURE_NS)
(declare RT'DEFAULT_COMPARATOR)
(declare RT'DEFAULT_IMPORTS)
(declare RT'EMPTY_ARRAY)
(declare RT'T)
(declare RT'TAG_KEY)
(declare RT'assoc)
(declare RT'baseLoader)
(declare RT'booleanCast-1o)
(declare RT'conj)
(declare RT'cons)
(declare RT'count)
(declare RT'errPrintWriter)
(declare RT'first)
(declare RT'isReduced)
(declare RT'keys)
(declare RT'length)
(declare RT'list)
(declare RT'listStar)
(declare RT'longCast-1o)
(declare RT'map)
(declare RT'next)
(declare RT'nth)
(declare RT'print)
(declare RT'printInnerSeq)
(declare RT'printString)
(declare RT'second)
(declare RT'seq)
(declare RT'seqFrom)
(declare RT'seqToPassedArray)
(declare RT'subvec)
(declare RT'toArray)
(declare RT'var)
(declare RT'vector)

(java-ns cloiure.lang.Murmur3

;;;
 ; See http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp
 ; MurmurHash3_x86_32
 ;
 ; @author Austin Appleby
 ; @author Dimitris Andreou
 ; @author Kurt Alfred Kluever
 ;;
(class-ns Murmur3
    (def- #_"int" Murmur3'seed 0)
    (def- #_"int" Murmur3'C1 0xcc9e2d51)
    (def- #_"int" Murmur3'C2 0x1b873593)

    (defn- #_"int" Murmur3'mixK1 [#_"int" k1]
        (-> k1 (* Murmur3'C1) (Integer/rotateLeft 15) (* Murmur3'C2))
    )

    (defn- #_"int" Murmur3'mixH1 [#_"int" h1, #_"int" k1]
        (-> h1 (bit-xor k1) (Integer/rotateLeft 13) (* 5) (+ 0xe6546b64))
    )

    ;; finalization mix - force all bits of a hash block to avalanche
    (defn- #_"int" Murmur3'fmix [#_"int" h1, #_"int" n]
        (let [h1 (bit-xor h1 n)    h1 (bit-xor h1 (>>> h1 16))
              h1 (* h1 0x85ebca6b) h1 (bit-xor h1 (>>> h1 13))
              h1 (* h1 0xc2b2ae35) h1 (bit-xor h1 (>>> h1 16))]
            h1
        )
    )

    (defn #_"int" Murmur3'hashInt [#_"int" input]
        (when-not (zero? input) => 0
            (let [#_"int" k1 (Murmur3'mixK1 input)
                  #_"int" h1 (Murmur3'mixH1 Murmur3'seed, k1)]
                (Murmur3'fmix h1, 4)
            )
        )
    )

    (defn #_"int" Murmur3'hashLong [#_"long" input]
        (when-not (zero? input) => 0
            (let [#_"int" low (int input)
                  #_"int" high (int (>>> input 32))
                  #_"int" k1 (Murmur3'mixK1 low)
                  #_"int" h1 (Murmur3'mixH1 Murmur3'seed, k1)
                  k1 (Murmur3'mixK1 high)
                  h1 (Murmur3'mixH1 h1, k1)]
                (Murmur3'fmix h1, 8)
            )
        )
    )

    (defn #_"int" Murmur3'hashUnencodedChars [#_"CharSequence" input]
        (let [#_"int" h1 ;; step through the input 2 chars at a time
                (loop-when [h1 Murmur3'seed #_"int" i 1] (< i (.length input)) => h1
                    (let [#_"int" k1 (| (.charAt input, (dec i)) (<< (.charAt input, i) 16))]
                        (recur (Murmur3'mixH1 h1, (Murmur3'mixK1 k1)) (+ i 2))
                    )
                )
              h1 ;; deal with any remaining characters
                (when (= (& (.length input) 1) 1) => h1
                    (let [#_"int" k1 (.charAt input, (dec (.length input)))]
                        (bit-xor h1 (Murmur3'mixK1 k1))
                    )
                )]
            (Murmur3'fmix h1, (* 2 (.length input)))
        )
    )

    (defn #_"int" Murmur3'mixCollHash [#_"int" hash, #_"int" n]
        (Murmur3'fmix (Murmur3'mixH1 Murmur3'seed, (Murmur3'mixK1 hash)), n)
    )

    (declare Util'hasheq)

    (defn #_"int" Murmur3'hashOrdered [#_"Iterable" xs]
        (let [#_"Iterator" it (.iterator xs)]
            (loop-when-recur [#_"int" hash 1 #_"int" n 0]
                             (.hasNext it)
                             [(+ (* 31 hash) (Util'hasheq (.next it))) (inc n)]
                          => (Murmur3'mixCollHash hash, n)
            )
        )
    )

    (defn #_"int" Murmur3'hashUnordered [#_"Iterable" xs]
        (let [#_"Iterator" it (.iterator xs)]
            (loop-when-recur [#_"int" hash 0 #_"int" n 0]
                             (.hasNext it)
                             [(+ hash (Util'hasheq (.next it))) (inc n)]
                          => (Murmur3'mixCollHash hash, n)
            )
        )
    )
)
)

(java-ns cloiure.lang.IFn
    ;;;
     ; IFn provides complete access to invoking any of Cloiure's APIs.
     ; You can also access any other library written in Cloiure, after
     ; adding either its source or compiled form to the classpath.
     ;;
    (interface! IFn [Callable Runnable]
        #_abstract
        (#_"Object" invoke [#_"IFn" this])
        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1])
        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2])
        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3])
        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4])
        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5])
        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6])
        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7])
        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18])

        #_abstract
        (#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19])

        #_abstract
    #_(#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20])

        #_abstract
    #_(#_"Object" invoke [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" #_arg19, #_"Object" #_arg20 & #_"Object..." args])

        #_abstract
        (#_"Object" applyTo [#_"IFn" this, #_"ISeq" args])
    )
)

(java-ns cloiure.lang.Fn
    (interface! Fn []
    )
)

(java-ns cloiure.lang.Sequential
    (interface! Sequential []
    )
)

(java-ns cloiure.lang.Seqable
    (interface! Seqable []
        #_abstract
        (#_"ISeq" seq [#_"Seqable" this])
    )
)

(java-ns cloiure.lang.Reversible
    (interface! Reversible []
        #_abstract
        (#_"ISeq" rseq [#_"Reversible" this])
    )
)

(java-ns cloiure.lang.Sorted
    (interface! Sorted []
        #_abstract
        (#_"Comparator" comparator [#_"Sorted" this])
        #_abstract
        (#_"Object" entryKey [#_"Sorted" this, #_"Object" entry])
        #_abstract
        (#_"ISeq" seq [#_"Sorted" this, #_"boolean" ascending])
        #_abstract
        (#_"ISeq" seqFrom [#_"Sorted" this, #_"Object" key, #_"boolean" ascending])
    )
)

(java-ns cloiure.lang.Counted
    (interface! Counted []
        #_abstract
        (#_"int" count [#_"Counted" this])
    )
)

(java-ns cloiure.lang.IPersistentCollection
    (interface! IPersistentCollection [Seqable]
        #_abstract
        (#_"int" count [#_"IPersistentCollection" this])
        #_abstract
        (#_"IPersistentCollection" cons [#_"IPersistentCollection" this, #_"Object" o])
        #_abstract
        (#_"IPersistentCollection" empty [#_"IPersistentCollection" this])
        #_abstract
        (#_"boolean" equiv [#_"IPersistentCollection" this, #_"Object" o])
    )
)

(java-ns cloiure.lang.ISeq
    (interface! ISeq [IPersistentCollection]
        #_abstract
        (#_"Object" first [#_"ISeq" this])
        #_abstract
        (#_"ISeq" next [#_"ISeq" this])
        #_abstract
        (#_"ISeq" more [#_"ISeq" this])
        #_abstract
        (#_"ISeq" cons [#_"ISeq" this, #_"Object" o])
    )
)

(java-ns cloiure.lang.IAtom
    (interface! IAtom []
        #_abstract
        (#_"Object" swap [#_"IAtom" this, #_"IFn" f])
        #_abstract
        (#_"Object" swap [#_"IAtom" this, #_"IFn" f, #_"Object" arg])
        #_abstract
        (#_"Object" swap [#_"IAtom" this, #_"IFn" f, #_"Object" arg1, #_"Object" arg2])
        #_abstract
        (#_"Object" swap [#_"IAtom" this, #_"IFn" f, #_"Object" x, #_"Object" y, #_"ISeq" args])
        #_abstract
        (#_"boolean" compareAndSet [#_"IAtom" this, #_"Object" oldv, #_"Object" newv])
        #_abstract
        (#_"Object" reset [#_"IAtom" this, #_"Object" newval])
    )
)

(java-ns cloiure.lang.IAtom2
    (interface! IAtom2 [IAtom]
        #_abstract
        (#_"IPersistentVector" swapVals [#_"IAtom2" this, #_"IFn" f])
        #_abstract
        (#_"IPersistentVector" swapVals [#_"IAtom2" this, #_"IFn" f, #_"Object" arg])
        #_abstract
        (#_"IPersistentVector" swapVals [#_"IAtom2" this, #_"IFn" f, #_"Object" arg1, #_"Object" arg2])
        #_abstract
        (#_"IPersistentVector" swapVals [#_"IAtom2" this, #_"IFn" f, #_"Object" x, #_"Object" y, #_"ISeq" args])
        #_abstract
        (#_"IPersistentVector" resetVals [#_"IAtom2" this, #_"Object" newv])
    )
)

(java-ns cloiure.lang.IDeref
    (interface! IDeref []
        #_abstract
        (#_"Object" deref [#_"IDeref" this])
    )
)

(java-ns cloiure.lang.IEditableCollection
    (interface! IEditableCollection []
        #_abstract
        (#_"ITransientCollection" asTransient [#_"IEditableCollection" this])
    )
)

(java-ns cloiure.lang.IExceptionInfo
    (interface! IExceptionInfo []
        #_abstract
        (#_"IPersistentMap" getData [#_"IExceptionInfo" this])
    )
)

(java-ns cloiure.lang.IHashEq
    (interface! IHashEq []
        #_abstract
        (#_"int" hasheq [#_"IHashEq" this])
    )
)

(java-ns cloiure.lang.MapEquivalence
    (interface! MapEquivalence []
    )
)

(java-ns cloiure.lang.ILookup
    (interface! ILookup []
        #_abstract
        (#_"Object" valAt [#_"ILookup" this, #_"Object" key])
        #_abstract
        (#_"Object" valAt [#_"ILookup" this, #_"Object" key, #_"Object" notFound])
    )
)

(java-ns cloiure.lang.ILookupSite
    (interface! ILookupSite []
        #_abstract
        (#_"ILookupThunk" fault [#_"ILookupSite" this, #_"Object" target])
    )
)

(java-ns cloiure.lang.ILookupThunk
    (interface! ILookupThunk []
        #_abstract
        (#_"Object" get [#_"ILookupThunk" this, #_"Object" target])
    )
)

(java-ns cloiure.lang.IMapEntry
    (interface! IMapEntry [Map$Entry]
        #_abstract
        (#_"Object" key [#_"IMapEntry" this])
        #_abstract
        (#_"Object" val [#_"IMapEntry" this])
    )
)

(java-ns cloiure.lang.IMapIterable
    (interface! IMapIterable []
        #_abstract
        (#_"Iterator" keyIterator [#_"IMapIterable" this])
        #_abstract
        (#_"Iterator" valIterator [#_"IMapIterable" this])
    )
)

(java-ns cloiure.lang.Named
    (interface! Named []
        #_abstract
        (#_"String" getNamespace [#_"Named" this])
        #_abstract
        (#_"String" getName [#_"Named" this])
    )
)

(java-ns cloiure.lang.IMeta
    (interface! IMeta []
        #_abstract
        (#_"IPersistentMap" meta [#_"IMeta" this])
    )
)

(java-ns cloiure.lang.IObj
    (interface! IObj [IMeta]
        #_abstract
        (#_"IObj" withMeta [#_"IObj" this, #_"IPersistentMap" meta])
    )
)

(java-ns cloiure.lang.IReference
    (interface! IReference [IMeta]
        #_abstract
        (#_"IPersistentMap" alterMeta [#_"IReference" this, #_"IFn" alter, #_"ISeq" args])
        #_abstract
        (#_"IPersistentMap" resetMeta [#_"IReference" this, #_"IPersistentMap" m])
    )
)

(java-ns cloiure.lang.Indexed
    (interface! Indexed [Counted]
        #_abstract
        (#_"Object" nth [#_"Indexed" this, #_"int" i])
        #_abstract
        (#_"Object" nth [#_"Indexed" this, #_"int" i, #_"Object" notFound])
    )
)

(java-ns cloiure.lang.IndexedSeq
    (interface! IndexedSeq [ISeq Sequential Counted]
        #_abstract
        (#_"int" index [#_"IndexedSeq" this])
    )
)

(java-ns cloiure.lang.IChunk
    (interface! IChunk [Indexed]
        #_abstract
        (#_"IChunk" dropFirst [#_"IChunk" this])
        #_abstract
        (#_"Object" reduce [#_"IChunk" this, #_"IFn" f, #_"Object" start])
    )
)

(java-ns cloiure.lang.IChunkedSeq
    (interface! IChunkedSeq [ISeq Sequential]
        #_abstract
        (#_"IChunk" chunkedFirst [#_"IChunkedSeq" this])
        #_abstract
        (#_"ISeq" chunkedNext [#_"IChunkedSeq" this])
        #_abstract
        (#_"ISeq" chunkedMore [#_"IChunkedSeq" this])
    )
)

(java-ns cloiure.lang.IPending
    (interface! IPending []
        #_abstract
        (#_"boolean" isRealized [#_"IPending" this])
    )
)

(java-ns cloiure.lang.Associative
    (interface! Associative [IPersistentCollection ILookup]
        #_abstract
        (#_"boolean" containsKey [#_"Associative" this, #_"Object" key])
        #_abstract
        (#_"IMapEntry" entryAt [#_"Associative" this, #_"Object" key])
        #_abstract
        (#_"Associative" assoc [#_"Associative" this, #_"Object" key, #_"Object" val])
    )
)

(java-ns cloiure.lang.IPersistentMap
    (interface! IPersistentMap [Iterable Associative Counted]
        #_abstract
        (#_"IPersistentMap" assoc [#_"IPersistentMap" this, #_"Object" key, #_"Object" val])
        #_abstract
        (#_"IPersistentMap" assocEx [#_"IPersistentMap" this, #_"Object" key, #_"Object" val])
        #_abstract
        (#_"IPersistentMap" without [#_"IPersistentMap" this, #_"Object" key])
    )
)

(java-ns cloiure.lang.IPersistentSet
    (interface! IPersistentSet [IPersistentCollection Counted]
        #_abstract
        (#_"IPersistentSet" disjoin [#_"IPersistentSet" this, #_"Object" key])
        #_abstract
        (#_"boolean" contains [#_"IPersistentSet" this, #_"Object" key])
        #_abstract
        (#_"Object" get [#_"IPersistentSet" this, #_"Object" key])
    )
)

(java-ns cloiure.lang.IPersistentStack
    (interface! IPersistentStack [IPersistentCollection]
        #_abstract
        (#_"Object" peek [#_"IPersistentStack" this])
        #_abstract
        (#_"IPersistentStack" pop [#_"IPersistentStack" this])
    )
)

(java-ns cloiure.lang.IPersistentList
    (interface! IPersistentList [Sequential IPersistentStack]
    )
)

(java-ns cloiure.lang.IPersistentVector
    (interface! IPersistentVector [Associative Sequential IPersistentStack Reversible Indexed]
        #_abstract
        (#_"IPersistentVector" assocN [#_"IPersistentVector" this, #_"int" i, #_"Object" val])
        #_abstract
        (#_"IPersistentVector" cons [#_"IPersistentVector" this, #_"Object" o])
    )
)

(java-ns cloiure.lang.IReduceInit
    (interface! IReduceInit []
        #_abstract
        (#_"Object" reduce [#_"IReduceInit" this, #_"IFn" f, #_"Object" start])
    )
)

(java-ns cloiure.lang.IReduce
    (interface! IReduce [IReduceInit]
        #_abstract
        (#_"Object" reduce [#_"IReduce" this, #_"IFn" f])
    )
)

(java-ns cloiure.lang.IKVReduce
    (interface! IKVReduce []
        #_abstract
        (#_"Object" kvreduce [#_"IKVReduce" this, #_"IFn" f, #_"Object" r])
    )
)

(java-ns cloiure.lang.ITransientCollection
    (interface! ITransientCollection []
        #_abstract
        (#_"ITransientCollection" conj [#_"ITransientCollection" this, #_"Object" val])
        #_abstract
        (#_"IPersistentCollection" persistent [#_"ITransientCollection" this])
    )
)

(java-ns cloiure.lang.ITransientAssociative
    (interface! ITransientAssociative [ITransientCollection ILookup]
        #_abstract
        (#_"ITransientAssociative" assoc [#_"ITransientAssociative" this, #_"Object" key, #_"Object" val])
    )
)

(java-ns cloiure.lang.ITransientAssociative2
    (interface! ITransientAssociative2 [ITransientAssociative]
        #_abstract
        (#_"boolean" containsKey [#_"ITransientAssociative2" this, #_"Object" key])
        #_abstract
        (#_"IMapEntry" entryAt [#_"ITransientAssociative2" this, #_"Object" key])
    )
)

(java-ns cloiure.lang.ITransientMap
    (interface! ITransientMap [ITransientAssociative Counted]
        #_abstract
        (#_"ITransientMap" assoc [#_"ITransientMap" this, #_"Object" key, #_"Object" val])
        #_abstract
        (#_"ITransientMap" without [#_"ITransientMap" this, #_"Object" key])
        #_abstract
        (#_"IPersistentMap" persistent [#_"ITransientMap" this])
    )
)

(java-ns cloiure.lang.ITransientSet
    (interface! ITransientSet [ITransientCollection Counted]
        #_abstract
        (#_"ITransientSet" disjoin [#_"ITransientSet" this, #_"Object" key])
        #_abstract
        (#_"boolean" contains [#_"ITransientSet" this, #_"Object" key])
        #_abstract
        (#_"Object" get [#_"ITransientSet" this, #_"Object" key])
    )
)

(java-ns cloiure.lang.ITransientVector
    (interface! ITransientVector [ITransientAssociative Indexed]
        #_abstract
        (#_"ITransientVector" assocN [#_"ITransientVector" this, #_"int" i, #_"Object" val])
        #_abstract
        (#_"ITransientVector" pop [#_"ITransientVector" this])
    )
)

(java-ns cloiure.lang.PersistentHashMap
    (interface! INode []
        #_abstract
        (#_"INode" assoc [#_"INode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"Box" addedLeaf])
        #_abstract
        (#_"INode" without [#_"INode" this, #_"int" shift, #_"int" hash, #_"Object" key])
        #_abstract
        (#_"IMapEntry" find [#_"INode" this, #_"int" shift, #_"int" hash, #_"Object" key])
        #_abstract
        (#_"Object" find [#_"INode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" notFound])
        #_abstract
        (#_"ISeq" nodeSeq [#_"INode" this])
        #_abstract
        (#_"INode" assoc [#_"INode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"Box" addedLeaf])
        #_abstract
        (#_"INode" without [#_"INode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Box" removedLeaf])
        #_abstract
        (#_"Object" kvreduce [#_"INode" this, #_"IFn" f, #_"Object" r])
        #_abstract
        (#_"Object" fold [#_"INode" this, #_"IFn" combinef, #_"IFn" reducef, #_"IFn" fjtask, #_"IFn" fjfork, #_"IFn" fjjoin])
        ;; returns the result of (f [k v]) for each iterated element
        #_abstract
        (#_"Iterator" iterator [#_"INode" this, #_"IFn" f])
    )
)

(java-ns cloiure.lang.Range
    (interface! RangeBoundsCheck []
        #_abstract
        (#_"boolean" exceededBounds [#_"RangeBoundsCheck" this, #_"Object" val])
    )
)

(java-ns cloiure.lang.LongRange
    (interface! LongRangeBoundsCheck []
        #_abstract
        (#_"boolean" exceededBounds [#_"LongRangeBoundsCheck" this, #_"long" val])
    )
)

(java-ns cloiure.lang.IType
    (interface! IType []
    )
)

(java-ns cloiure.lang.IProxy
    (interface! IProxy []
        #_abstract
        (#_"void" __initCloiureFnMappings [#_"IProxy" this, #_"IPersistentMap" m])
        #_abstract
        (#_"void" __updateCloiureFnMappings [#_"IProxy" this, #_"IPersistentMap" m])
        #_abstract
        (#_"IPersistentMap" __getCloiureFnMappings [#_"IProxy" this])
    )
)

(java-ns cloiure.lang.Util
    (interface! EquivPred []
        #_abstract
        (#_"boolean" equiv [#_"EquivPred" this, #_"Object" k1, #_"Object" k2])
    )

    #_stateless
    (class! Util [])
)

(java-ns cloiure.lang.DynamicClassLoader
    (class! DynamicClassLoader [#_"URLClassLoader"])
)

(java-ns cloiure.lang.ExceptionInfo
    (class! ExceptionInfo [#_"RuntimeException" IExceptionInfo])
)

(java-ns cloiure.lang.BigInt
    (class! BigInt [#_"Number" IHashEq])
)

(java-ns cloiure.lang.Ratio
    (class! Ratio [#_"Number" Comparable])
)

(java-ns cloiure.lang.Numbers
    (interface! Ops []
        #_abstract
        (#_"Ops" combine [#_"Ops" this, #_"Ops" y])
        #_abstract
        (#_"Ops" opsWithLong [#_"Ops" this, #_"LongOps" x])
        #_abstract
        (#_"Ops" opsWithDouble [#_"Ops" this, #_"DoubleOps" x])
        #_abstract
        (#_"Ops" opsWithRatio [#_"Ops" this, #_"RatioOps" x])
        #_abstract
        (#_"Ops" opsWithBigInt [#_"Ops" this, #_"BigIntOps" x])
        #_abstract
        (#_"Ops" opsWithBigDecimal [#_"Ops" this, #_"BigDecimalOps" x])
        #_abstract
        (#_"boolean" isZero [#_"Ops" this, #_"Number" x])
        #_abstract
        (#_"boolean" isPos [#_"Ops" this, #_"Number" x])
        #_abstract
        (#_"boolean" isNeg [#_"Ops" this, #_"Number" x])
        #_abstract
        (#_"Number" add [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"Number" addP [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"Number" multiply [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"Number" multiplyP [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"Number" divide [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"Number" quotient [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"Number" remainder [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"boolean" equiv [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"boolean" lt [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"boolean" lte [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"boolean" gte [#_"Ops" this, #_"Number" x, #_"Number" y])
        #_abstract
        (#_"Number" negate [#_"Ops" this, #_"Number" x])
        #_abstract
        (#_"Number" negateP [#_"Ops" this, #_"Number" x])
        #_abstract
        (#_"Number" inc [#_"Ops" this, #_"Number" x])
        #_abstract
        (#_"Number" incP [#_"Ops" this, #_"Number" x])
        #_abstract
        (#_"Number" dec [#_"Ops" this, #_"Number" x])
        #_abstract
        (#_"Number" decP [#_"Ops" this, #_"Number" x])
    )

    #_abstract
    (class! OpsP [Ops])
    (class! LongOps [Ops])
    (class! DoubleOps [#_"OpsP"])
    (class! RatioOps [#_"OpsP"])
    (class! BigIntOps [#_"OpsP"])
    (class! BigDecimalOps [#_"OpsP"])
    #_stateless
    (class! Numbers [])
)

(java-ns cloiure.lang.ArityException
    (class! ArityException [#_"IllegalArgumentException"])
)

(java-ns cloiure.lang.AFn
    #_abstract
    (class! AFn [IFn]
        #_abstract
        (#_"Object" throwArity [#_"AFn" this, #_"int" n])
    )
)

(java-ns cloiure.lang.Symbol
    (class! Symbol [#_"AFn" IObj Comparable Named IHashEq])
)

(java-ns cloiure.lang.Keyword
    (class! Keyword [IFn Comparable Named IHashEq])
)

(java-ns cloiure.lang.AFunction
    #_abstract
    (class! AFunction [#_"AFn" IObj Comparator Fn])
)

(java-ns cloiure.lang.RestFn
    #_abstract
    (class! RestFn [#_"AFunction"]
        #_abstract
        (#_"int" getRequiredArity [#_"RestFn" this])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" args])
        #_abstract
        (#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" args])
        #_abstract
      #_(#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" args])
        #_abstract
      #_(#_"Object" doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20, #_"Object" args])
    )
)

(java-ns cloiure.lang.ASeq
    #_abstract
    (class! ASeq [IObj ISeq Sequential List IHashEq])
)

(java-ns cloiure.lang.LazySeq
    (class! LazySeq [IObj ISeq Sequential List IPending IHashEq])
)

(java-ns cloiure.lang.APersistentMap
    (class! KeySeq [#_"ASeq"])
    (class! ValSeq [#_"ASeq"])
    #_abstract
    (class! APersistentMap [#_"AFn" IPersistentMap Map Iterable MapEquivalence IHashEq])
)

(java-ns cloiure.lang.APersistentSet
    #_abstract
    (class! APersistentSet [#_"AFn" IPersistentSet Collection Set IHashEq])
)

(java-ns cloiure.lang.APersistentVector
    (class! VSeq [#_"ASeq" IndexedSeq IReduce])
    (class! RSeq [#_"ASeq" IndexedSeq Counted])
    #_abstract
    (class! APersistentVector [#_"AFn" IPersistentVector Iterable List Comparable IHashEq]
        #_abstract
        (#_"Iterator" rangedIterator [#_"APersistentVector" this, #_"int" start, #_"int" end])
    )
    (class! SubVector [#_"APersistentVector" IObj])
)

(java-ns cloiure.lang.AMapEntry
    #_abstract
    (class! AMapEntry [#_"APersistentVector" IMapEntry])
)

(java-ns cloiure.lang.ArrayChunk
    (class! ArrayChunk [IChunk])
)

(java-ns cloiure.lang.ArraySeq
    (class! ArraySeq_int [#_"ASeq" IndexedSeq IReduce])
    (class! ArraySeq_float [#_"ASeq" IndexedSeq IReduce])
    (class! ArraySeq_double [#_"ASeq" IndexedSeq IReduce])
    (class! ArraySeq_long [#_"ASeq" IndexedSeq IReduce])
    (class! ArraySeq_byte [#_"ASeq" IndexedSeq IReduce])
    (class! ArraySeq_char [#_"ASeq" IndexedSeq IReduce])
    (class! ArraySeq_short [#_"ASeq" IndexedSeq IReduce])
    (class! ArraySeq_boolean [#_"ASeq" IndexedSeq IReduce])
    (class! ArraySeq [#_"ASeq" IndexedSeq IReduce])
)

(java-ns cloiure.lang.Atom
    (class! Atom [IReference IDeref IAtom2])
)

(java-ns cloiure.lang.ATransientMap
    #_abstract
    (class! ATransientMap [#_"AFn" ITransientMap ITransientAssociative2]
        #_abstract
        (#_"void" ensureEditable [#_"ATransientMap" this])
        #_abstract
        (#_"ITransientMap" doAssoc [#_"ATransientMap" this, #_"Object" key, #_"Object" val])
        #_abstract
        (#_"ITransientMap" doWithout [#_"ATransientMap" this, #_"Object" key])
        #_abstract
        (#_"Object" doValAt [#_"ATransientMap" this, #_"Object" key, #_"Object" notFound])
        #_abstract
        (#_"int" doCount [#_"ATransientMap" this])
        #_abstract
        (#_"IPersistentMap" doPersistent [#_"ATransientMap" this])
    )
)

(java-ns cloiure.lang.ATransientSet
    #_abstract
    (class! ATransientSet [#_"AFn" ITransientSet])
)

(java-ns cloiure.lang.Binding
    (class! Binding #_"<T>" [])
)

(java-ns cloiure.lang.Box
    (class! Box [])
)

(java-ns cloiure.lang.ChunkBuffer
    (class! ChunkBuffer [Counted])
)

(java-ns cloiure.lang.ChunkedCons
    (class! ChunkedCons [#_"ASeq" IChunkedSeq])
)

(java-ns cloiure.lang.Cons
    (class! Cons [#_"ASeq"])
)

(java-ns cloiure.lang.Cycle
    (class! Cycle [#_"ASeq" IReduce IPending])
)

(java-ns cloiure.lang.Delay
    (class! Delay [IDeref IPending])
)

(java-ns cloiure.lang.Iterate
    (class! Iterate [#_"ASeq" IReduce IPending])
)

(java-ns cloiure.lang.KeywordLookupSite
    (class! KeywordLookupSite [ILookupSite ILookupThunk])
)

(java-ns cloiure.lang.LongRange
    (class! LongChunk [IChunk])
    (class! LongRange [#_"ASeq" Counted IChunkedSeq IReduce])
)

(java-ns cloiure.lang.MapEntry
    (class! MapEntry [#_"AMapEntry"])
)

(java-ns cloiure.lang.MethodImplCache
    (class! Entry [])
    (class! MethodImplCache [])
)

(java-ns cloiure.lang.MultiFn
    (class! MultiFn [#_"AFn"])
)

(java-ns cloiure.lang.Namespace
    (class! Namespace [IReference])
)

(java-ns cloiure.lang.PersistentArrayMap
    (class! MSeq [#_"ASeq" Counted])
    (class! TransientArrayMap [#_"ATransientMap"])
    (class! PersistentArrayMap [#_"APersistentMap" IObj IEditableCollection IMapIterable IKVReduce])
)

(java-ns cloiure.lang.PersistentHashMap
    (class! TransientHashMap [#_"ATransientMap"])
    (class! HSeq [#_"ASeq"])
    (class! ArrayNode [INode])
    (class! BitmapIndexedNode [INode])
    (class! HashCollisionNode [INode])
    (class! NodeSeq [#_"ASeq"])
    (class! PersistentHashMap [#_"APersistentMap" IEditableCollection IObj IMapIterable IKVReduce])
)

(java-ns cloiure.lang.PersistentHashSet
    (class! TransientHashSet [#_"ATransientSet"])
    (class! PersistentHashSet [#_"APersistentSet" IObj IEditableCollection])
)

(java-ns cloiure.lang.PersistentList
    (class! Primordial [#_"RestFn"])
    (class! EmptyList [IObj IPersistentList List ISeq Counted IHashEq])
    (class! PersistentList [#_"ASeq" IPersistentList IReduce List Counted])
)

(java-ns cloiure.lang.PersistentQueue
    (class! QSeq [#_"ASeq"])
    (class! PersistentQueue [IObj IPersistentList Collection Counted IHashEq])
)

(java-ns cloiure.lang.PersistentTreeMap
    #_abstract
    (class! TNode [#_"AMapEntry"]
        #_abstract
        (#_"TNode" left [#_"TNode" this])
        #_abstract
        (#_"TNode" right [#_"TNode" this])
        #_abstract
        (#_"TNode" addLeft [#_"TNode" this, #_"TNode" ins])
        #_abstract
        (#_"TNode" addRight [#_"TNode" this, #_"TNode" ins])
        #_abstract
        (#_"TNode" removeLeft [#_"TNode" this, #_"TNode" del])
        #_abstract
        (#_"TNode" removeRight [#_"TNode" this, #_"TNode" del])
        #_abstract
        (#_"TNode" blacken [#_"TNode" this])
        #_abstract
        (#_"TNode" redden [#_"TNode" this])
        #_abstract
        (#_"TNode" balanceLeft [#_"TNode" this, #_"TNode" parent])
        #_abstract
        (#_"TNode" balanceRight [#_"TNode" this, #_"TNode" parent])
        #_abstract
        (#_"TNode" replace [#_"TNode" this, #_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right])
    )
    (class! Black [#_"TNode"])
    (class! BlackVal [#_"Black"])
    (class! BlackBranch [#_"Black"])
    (class! BlackBranchVal [#_"BlackBranch"])
    (class! Red [#_"TNode"])
    (class! RedVal [#_"Red"])
    (class! RedBranch [#_"Red"])
    (class! RedBranchVal [#_"RedBranch"])
    (class! TSeq [#_"ASeq"])
    (class! PersistentTreeMap [#_"APersistentMap" IObj Reversible Sorted IKVReduce])
)

(java-ns cloiure.lang.PersistentTreeSet
    (class! PersistentTreeSet [#_"APersistentSet" IObj Reversible Sorted])
)

(java-ns cloiure.lang.PersistentVector
    (class! VNode [])
    (class! ChunkedSeq [#_"ASeq" IChunkedSeq Counted])
    (class! TransientVector [#_"AFn" ITransientVector ITransientAssociative2 Counted])
    (class! PersistentVector [#_"APersistentVector" IObj IEditableCollection IReduce IKVReduce])
)

(java-ns cloiure.lang.Range
    (class! Range [#_"ASeq" IChunkedSeq IReduce])
)

(java-ns cloiure.lang.Reduced
    (class! Reduced [IDeref])
)

(java-ns cloiure.lang.Repeat
    (class! Repeat [#_"ASeq" IReduce])
)

(java-ns cloiure.lang.StringSeq
    (class! StringSeq [#_"ASeq" IndexedSeq])
)

(java-ns cloiure.lang.Tuple
    #_stateless
    (class! Tuple [])
)

(java-ns cloiure.lang.Var
    (class! TBox [])
    (class! Unbound [#_"AFn"])
    (class! Frame [])
    (class! Var [IReference IFn IDeref])
)

(java-ns cloiure.lang.Volatile
    (class! Volatile [IDeref])
)

(java-ns cloiure.lang.RT
    #_stateless
    (class! RT [])
)

(java-ns cloiure.lang.Util

(class-ns Util
    (defn- #_"boolean" Util'pcequiv [#_"Object" k1, #_"Object" k2]
        (if (instance? IPersistentCollection k1)
            (.equiv (cast IPersistentCollection k1), k2)
            (.equiv (cast IPersistentCollection k2), k1)
        )
    )

    (declare Numbers'equal)

    (defn #_"boolean" Util'equiv-2oo [#_"Object" k1, #_"Object" k2]
        (cond
            (= k1 k2) true
            (nil? k1) false
            (and (instance? Number k1) (instance? Number k2)) (Numbers'equal (cast Number k1), (cast Number k2))
            (or (instance? IPersistentCollection k1) (instance? IPersistentCollection k2)) (Util'pcequiv k1, k2)
            :else (.equals k1, k2)
        )
    )

    (def #_"EquivPred" Util'equivNull
        (reify EquivPred
            #_override
            (#_"boolean" equiv [#_"EquivPred" _self, #_"Object" k1, #_"Object" k2]
                (nil? k2)
            )
        )
    )

    (def #_"EquivPred" Util'equivEquals
        (reify EquivPred
            #_override
            (#_"boolean" equiv [#_"EquivPred" _self, #_"Object" k1, #_"Object" k2]
                (.equals k1, k2)
            )
        )
    )

    (def #_"EquivPred" Util'equivNumber
        (reify EquivPred
            #_override
            (#_"boolean" equiv [#_"EquivPred" _self, #_"Object" k1, #_"Object" k2]
                (and (instance? Number k2) (Numbers'equal (cast Number k1), (cast Number k2)))
            )
        )
    )

    (def #_"EquivPred" Util'equivColl
        (reify EquivPred
            #_override
            (#_"boolean" equiv [#_"EquivPred" _self, #_"Object" k1, #_"Object" k2]
                (if (or (instance? IPersistentCollection k1) (instance? IPersistentCollection k2)) (Util'pcequiv k1, k2) (.equals k1, k2))
            )
        )
    )

    (defn #_"EquivPred" Util'equivPred [#_"Object" k1]
        (cond
            (nil? k1)                                         Util'equivNull
            (instance? Number k1)                             Util'equivNumber
            (or (instance? String k1) (instance? Symbol k1))  Util'equivEquals
            (or (instance? Collection k1) (instance? Map k1)) Util'equivColl
            :else                                             Util'equivEquals
        )
    )

    (defn #_"boolean" Util'equiv-2ll [#_"long" k1, #_"long" k2]
        (= k1 k2)
    )

    (defn #_"boolean" Util'equiv-2ol [#_"Object" k1, #_"long" k2]
        (Util'equiv-2oo k1, (cast Object k2))
    )

    (defn #_"boolean" Util'equiv-2lo [#_"long" k1, #_"Object" k2]
        (Util'equiv-2oo (cast Object k1), k2)
    )

    (defn #_"boolean" Util'equiv-2dd [#_"double" k1, #_"double" k2]
        (= k1 k2)
    )

    (defn #_"boolean" Util'equiv-2od [#_"Object" k1, #_"double" k2]
        (Util'equiv-2oo k1, (cast Object k2))
    )

    (defn #_"boolean" Util'equiv-2do [#_"double" k1, #_"Object" k2]
        (Util'equiv-2oo (cast Object k1), k2)
    )

    (defn #_"boolean" Util'equiv-2bb [#_"boolean" k1, #_"boolean" k2]
        (= k1 k2)
    )

    (defn #_"boolean" Util'equiv-2ob [#_"Object" k1, #_"boolean" k2]
        (Util'equiv-2oo k1, (cast Object k2))
    )

    (defn #_"boolean" Util'equiv-2bo [#_"boolean" k1, #_"Object" k2]
        (Util'equiv-2oo (cast Object k1), k2)
    )

    (defn #_"boolean" Util'equiv-2cc [#_"char" c1, #_"char" c2]
        (= c1 c2)
    )

    (defn #_"boolean" Util'equals [#_"Object" k1, #_"Object" k2]
        (or (= k1 k2) (and (some? k1) (.equals k1, k2)))
    )

    (defn #_"boolean" Util'identical [#_"Object" k1, #_"Object" k2]
        (= k1 k2)
    )

    (declare Numbers'compare)

    (defn #_"int" Util'compare [#_"Object" k1, #_"Object" k2]
        (cond
            (= k1 k2)             0
            (nil? k1)             -1
            (nil? k2)             1
            (instance? Number k1) (Numbers'compare (cast Number k1), (cast Number k2))
            :else                 (.compareTo (cast Comparable k1), k2)
        )
    )

    (defn #_"int" Util'hash [#_"Object" o]
        (cond
            (nil? o) 0
            :else    (.hashCode o)
        )
    )

    (declare Numbers'hasheq)

    (defn #_"int" Util'hasheq [#_"Object" o]
        (cond
            (nil? o)              0
            (instance? IHashEq o) (.hasheq (cast IHashEq o))
            (instance? Number o)  (Numbers'hasheq (cast Number o))
            (instance? String o)  (Murmur3'hashInt (.hashCode o))
            :else                 (.hashCode o)
        )
    )

    (defn #_"int" Util'hashCombine [#_"int" seed, #_"int" hash]
        ;; a la boost
        (bit-xor seed (+ hash 0x9e3779b9 (<< seed 6) (>> seed 2)))
    )

    (defn #_"<K, V> void" Util'clearCache [#_"ReferenceQueue" rq, #_"ConcurrentHashMap<K, Reference<V>>" cache]
        ;; cleanup any dead entries
        (when (some? (.poll rq))
            (while (some? (.poll rq))
            )
            (doseq [#_"Map$Entry<K, Reference<V>>" e (.entrySet cache)]
                (let-when [#_"Reference<V>" r (.getValue e)] (and (some? r) (nil? (.get r)))
                    (.remove cache, (.getKey e), r)
                )
            )
        )
        nil
    )
)
)

(java-ns cloiure.lang.DynamicClassLoader

(class-ns DynamicClassLoader
    (def #_"ConcurrentHashMap<String, Reference<Class>>" DynamicClassLoader'classCache (ConcurrentHashMap.))

    (def #_"ReferenceQueue" DynamicClassLoader'RQ (ReferenceQueue.))

    (defn #_"DynamicClassLoader" DynamicClassLoader'new [#_"ClassLoader" parent]
        (merge (§ foreign URLClassLoader'new (make-array URL 0), parent)
            (hash-map
                #_"HashMap<Integer, Object[]>" :constantVals (HashMap.)
            )
        )
    )

    #_method
    (defn #_"Class" DynamicClassLoader''defineClass [#_"DynamicClassLoader" this, #_"String" name, #_"byte[]" bytes]
        (Util'clearCache DynamicClassLoader'RQ, DynamicClassLoader'classCache)
        (let [#_"Class" c (.defineClass this, name, bytes, 0, (alength bytes))]
            (.put DynamicClassLoader'classCache, name, (SoftReference. c, DynamicClassLoader'RQ))
            c
        )
    )

    (defn #_"Class<?>" DynamicClassLoader'findInMemoryClass [#_"String" name]
        (when-let [#_"Reference<Class>" r (.get DynamicClassLoader'classCache, name)]
            (or (.get r) (do (.remove DynamicClassLoader'classCache, name, r) nil))
        )
    )

    #_foreign
    (defn #_"Class<?>" findClass---DynamicClassLoader [#_"DynamicClassLoader" this, #_"String" name]
        (or (DynamicClassLoader'findInMemoryClass name) (.findClass (§ super ), name))
    )

    #_foreign
    (defn #_"Class<?>" loadClass---DynamicClassLoader [#_"DynamicClassLoader" this, #_"String" name, #_"boolean" resolve]
        (§ sync this
            (let [#_"Class" c
                    (or (.findLoadedClass this, name)
                        (DynamicClassLoader'findInMemoryClass name)
                        (.loadClass (§ super ), name, false)
                    )]
                (when resolve
                    (.resolveClass this, c)
                )
                c
            )
        )
    )

    #_method
    (defn #_"void" DynamicClassLoader''registerConstants [#_"DynamicClassLoader" this, #_"int" id, #_"Object[]" val]
        (.put (:constantVals this), id, val)
        nil
    )

    #_method
    (defn #_"Object[]" DynamicClassLoader''getConstants [#_"DynamicClassLoader" this, #_"int" id]
        (.get (:constantVals this), id)
    )
)
)

(java-ns cloiure.lang.ExceptionInfo

;;;
 ; Exception that carries data (a map) as additional payload. Cloiure programs that need
 ; richer semantics for exceptions should use this in lieu of defining project-specific
 ; exception classes.
 ;;
(class-ns ExceptionInfo
    (defn #_"ExceptionInfo" ExceptionInfo'new
        ([#_"String" s, #_"IPersistentMap" data] (ExceptionInfo'new s, data, nil))
        ([#_"String" s, #_"IPersistentMap" data, #_"Throwable" t]
            ;; nil cause is equivalent to not passing a cause
            (when (some? data) => (throw (IllegalArgumentException. "Additional data must be non-nil."))
                (merge (§ foreign RuntimeException'new s, t)
                    (hash-map
                        #_"IPersistentMap" :data data
                    )
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" IExceptionInfo'''getData--ExceptionInfo [#_"ExceptionInfo" this]
        (:data this)
    )

    #_foreign
    (defn #_"String" toString---ExceptionInfo [#_"ExceptionInfo" this]
        (str "cloiure.lang.ExceptionInfo: " (.getMessage this) " " (:data this))
    )
)
)

(java-ns cloiure.lang.BigInt

(class-ns BigInt
    (defn- #_"BigInt" BigInt'new [#_"long" lpart, #_"BigInteger" bipart]
        (merge (§ foreign Number'new)
            (hash-map
                #_"long" :lpart lpart
                #_"BigInteger" :bipart bipart
            )
        )
    )

    (def #_"BigInt" BigInt'ZERO (BigInt'new 0, nil))
    (def #_"BigInt" BigInt'ONE (BigInt'new 1, nil))

    ;; must follow Long
    #_foreign
    (defn #_"int" hashCode---BigInt [#_"BigInt" this]
        (if (nil? (:bipart this))
            (int (bit-xor (:lpart this) (>>> (:lpart this) 32)))
            (.hashCode (:bipart this))
        )
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--BigInt [#_"BigInt" this]
        (if (nil? (:bipart this))
            (Murmur3'hashLong (:lpart this))
            (.hashCode (:bipart this))
        )
    )

    #_foreign
    (defn #_"boolean" equals---BigInt [#_"BigInt" this, #_"Object" obj]
        (cond
            (= this obj)
                true
            (instance? BigInt obj)
                (let [#_"BigInt" o (cast BigInt obj)]
                    (if (nil? (:bipart this))
                        (and (nil? (:bipart o)) (= (:lpart this) (:lpart o)))
                        (and (some? (:bipart o)) (.equals (:bipart this), (:bipart o)))
                    )
                )
            :else
                false
        )
    )

    (defn #_"BigInt" BigInt'fromBigInteger [#_"BigInteger" val]
        (if (< (.bitLength val) 64)
            (BigInt'new (.longValue val), nil)
            (BigInt'new 0, val)
        )
    )

    (defn #_"BigInt" BigInt'fromLong [#_"long" val]
        (BigInt'new val, nil)
    )

    #_method
    (defn #_"BigInteger" BigInt''toBigInteger [#_"BigInt" this]
        (if (nil? (:bipart this))
            (BigInteger/valueOf (:lpart this))
            (:bipart this)
        )
    )

    #_method
    (defn #_"BigDecimal" BigInt''toBigDecimal [#_"BigInt" this]
        (if (nil? (:bipart this))
            (BigDecimal/valueOf (:lpart this))
            (BigDecimal. (:bipart this))
        )
    )

    #_method
    (defn #_"int" BigInt''intValue [#_"BigInt" this]
        (if (nil? (:bipart this))
            (int (:lpart this))
            (.intValue (:bipart this))
        )
    )

    #_method
    (defn #_"long" BigInt''longValue [#_"BigInt" this]
        (if (nil? (:bipart this))
            (:lpart this)
            (.longValue (:bipart this))
        )
    )

    #_method
    (defn #_"float" BigInt''floatValue [#_"BigInt" this]
        (if (nil? (:bipart this))
            (:lpart this)
            (.floatValue (:bipart this))
        )
    )

    #_method
    (defn #_"double" BigInt''doubleValue [#_"BigInt" this]
        (if (nil? (:bipart this))
            (:lpart this)
            (.doubleValue (:bipart this))
        )
    )

    #_method
    (defn #_"byte" BigInt''byteValue [#_"BigInt" this]
        (if (nil? (:bipart this))
            (byte (:lpart this))
            (.byteValue (:bipart this))
        )
    )

    #_method
    (defn #_"short" BigInt''shortValue [#_"BigInt" this]
        (if (nil? (:bipart this))
            (short (:lpart this))
            (.shortValue (:bipart this))
        )
    )

    (defn #_"BigInt" BigInt'valueOf [#_"long" val]
        (BigInt'new val, nil)
    )

    #_foreign
    (defn #_"String" toString---BigInt [#_"BigInt" this]
        (if (nil? (:bipart this))
            (String/valueOf (:lpart this))
            (.toString (:bipart this))
        )
    )

    #_method
    (defn #_"int" BigInt''bitLength [#_"BigInt" this]
        (.bitLength (BigInt''toBigInteger this))
    )

    #_method
    (defn #_"BigInt" BigInt''add [#_"BigInt" this, #_"BigInt" y]
        (or
            (when (and (nil? (:bipart this)) (nil? (:bipart y)))
                (let [#_"long" ret (+ (:lpart this) (:lpart y))]
                    (when (or (<= 0 (bit-xor ret (:lpart this))) (<= 0 (bit-xor ret (:lpart y))))
                        (BigInt'valueOf ret)
                    )
                )
            )
            (BigInt'fromBigInteger (.add (BigInt''toBigInteger this), (BigInt''toBigInteger y)))
        )
    )

    #_method
    (defn #_"BigInt" BigInt''multiply [#_"BigInt" this, #_"BigInt" y]
        (or
            (when (and (nil? (:bipart this)) (nil? (:bipart y)))
                (let [#_"long" ret (* (:lpart this) (:lpart y))]
                    (when (or (zero? (:lpart y)) (and (= (/ ret (:lpart y)) (:lpart this)) (not= (:lpart this) Long/MIN_VALUE)))
                        (BigInt'valueOf ret)
                    )
                )
            )
            (BigInt'fromBigInteger (.multiply (BigInt''toBigInteger this), (BigInt''toBigInteger y)))
        )
    )

    #_method
    (defn #_"BigInt" BigInt''quotient [#_"BigInt" this, #_"BigInt" y]
        (if (and (nil? (:bipart this)) (nil? (:bipart y)))
            (if (and (= (:lpart this) Long/MIN_VALUE) (= (:lpart y) -1))
                (BigInt'fromBigInteger (.negate (BigInt''toBigInteger this)))
                (BigInt'valueOf (/ (:lpart this) (:lpart y)))
            )
            (BigInt'fromBigInteger (.divide (BigInt''toBigInteger this), (BigInt''toBigInteger y)))
        )
    )

    #_method
    (defn #_"BigInt" BigInt''remainder [#_"BigInt" this, #_"BigInt" y]
        (if (and (nil? (:bipart this)) (nil? (:bipart y)))
            (BigInt'valueOf (% (:lpart this) (:lpart y)))
            (BigInt'fromBigInteger (.remainder (BigInt''toBigInteger this), (BigInt''toBigInteger y)))
        )
    )

    #_method
    (defn #_"boolean" BigInt''lt [#_"BigInt" this, #_"BigInt" y]
        (if (and (nil? (:bipart this)) (nil? (:bipart y)))
            (< (:lpart this) (:lpart y))
            (neg? (.compareTo (BigInt''toBigInteger this), (BigInt''toBigInteger y)))
        )
    )
)
)

(java-ns cloiure.lang.Ratio

(class-ns Ratio
    (defn #_"Ratio" Ratio'new [#_"BigInteger" numerator, #_"BigInteger" denominator]
        (merge (§ foreign Number'new)
            (hash-map
                #_"BigInteger" :numerator numerator
                #_"BigInteger" :denominator denominator
            )
        )
    )

    #_foreign
    (defn #_"boolean" equals---Ratio [#_"Ratio" this, #_"Object" arg0]
        (and (some? arg0)
             (instance? Ratio arg0)
             (.equals (:numerator (cast Ratio arg0)), (:numerator this))
             (.equals (:denominator (cast Ratio arg0)), (:denominator this))
        )
    )

    #_foreign
    (defn #_"int" hashCode---Ratio [#_"Ratio" this]
        (bit-xor (.hashCode (:numerator this)) (.hashCode (:denominator this)))
    )

    #_foreign
    (defn #_"String" toString---Ratio [#_"Ratio" this]
        (str (:numerator this) "/" (:denominator this))
    )

    #_method
    (defn #_"BigInteger" Ratio''bigIntegerValue [#_"Ratio" this]
        (.divide (:numerator this), (:denominator this))
    )

    #_method
    (defn #_"long" Ratio''longValue [#_"Ratio" this]
        (.longValue (Ratio''bigIntegerValue this))
    )

    #_method
    (defn #_"BigDecimal" Ratio''decimalValue
        ([#_"Ratio" this] (Ratio''decimalValue this, MathContext/UNLIMITED))
        ([#_"Ratio" this, #_"MathContext" mc]
            (let [#_"BigDecimal" numerator (BigDecimal. (:numerator this))
                  #_"BigDecimal" denominator (BigDecimal. (:denominator this))]
                (.divide numerator, denominator, mc)
            )
        )
    )

    #_method
    (defn #_"double" Ratio''doubleValue [#_"Ratio" this]
        (.doubleValue (Ratio''decimalValue this, MathContext/DECIMAL64))
    )

    #_method
    (defn #_"float" Ratio''floatValue [#_"Ratio" this]
        (float (Ratio''doubleValue this))
    )

    #_method
    (defn #_"int" Ratio''intValue [#_"Ratio" this]
        (int (Ratio''doubleValue this))
    )

    #_foreign
    (defn #_"int" compareTo---Ratio [#_"Ratio" this, #_"Object" o]
        (Numbers'compare this, (cast Number o))
    )
)
)

(java-ns cloiure.lang.Numbers

(class-ns OpsP
    (defn #_"OpsP" OpsP'new []
        (hash-map)
    )

    #_override
    (defn #_"Number" Ops'''addP--OpsP [#_"OpsP" this, #_"Number" x, #_"Number" y]
        (.add this, x, y)
    )

    #_override
    (defn #_"Number" Ops'''multiplyP--OpsP [#_"OpsP" this, #_"Number" x, #_"Number" y]
        (.multiply this, x, y)
    )

    #_override
    (defn #_"Number" Ops'''negateP--OpsP [#_"OpsP" this, #_"Number" x]
        (.negate this, x)
    )

    #_override
    (defn #_"Number" Ops'''incP--OpsP [#_"OpsP" this, #_"Number" x]
        (.inc this, x)
    )

    #_override
    (defn #_"Number" Ops'''decP--OpsP [#_"OpsP" this, #_"Number" x]
        (.dec this, x)
    )
)

(class-ns LongOps
    (defn #_"LongOps" LongOps'new []
        (hash-map)
    )

    #_override
    (defn #_"Ops" Ops'''combine--LongOps [#_"LongOps" this, #_"Ops" y]
        (.opsWithLong y, this)
    )

    #_override
    (defn #_"Ops" Ops'''opsWithLong--LongOps [#_"LongOps" this, #_"LongOps" x]
        this
    )

    (declare Numbers'DOUBLE_OPS)

    #_override
    (defn #_"Ops" Ops'''opsWithDouble--LongOps [#_"LongOps" this, #_"DoubleOps" x]
        Numbers'DOUBLE_OPS
    )

    (declare Numbers'RATIO_OPS)

    #_override
    (defn #_"Ops" Ops'''opsWithRatio--LongOps [#_"LongOps" this, #_"RatioOps" x]
        Numbers'RATIO_OPS
    )

    (declare Numbers'BIGINT_OPS)

    #_override
    (defn #_"Ops" Ops'''opsWithBigInt--LongOps [#_"LongOps" this, #_"BigIntOps" x]
        Numbers'BIGINT_OPS
    )

    (declare Numbers'BIGDECIMAL_OPS)

    #_override
    (defn #_"Ops" Ops'''opsWithBigDecimal--LongOps [#_"LongOps" this, #_"BigDecimalOps" x]
        Numbers'BIGDECIMAL_OPS
    )

    #_override
    (defn #_"boolean" Ops'''isZero--LongOps [#_"LongOps" this, #_"Number" x]
        (zero? (.longValue x))
    )

    #_override
    (defn #_"boolean" Ops'''isPos--LongOps [#_"LongOps" this, #_"Number" x]
        (pos? (.longValue x))
    )

    #_override
    (defn #_"boolean" Ops'''isNeg--LongOps [#_"LongOps" this, #_"Number" x]
        (neg? (.longValue x))
    )

    (declare Numbers'num-1l)
    (declare Numbers'add-2ll)

    #_override
    (defn #_"Number" Ops'''add--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (Numbers'num-1l (Numbers'add-2ll (.longValue x), (.longValue y)))
    )

    #_override
    (defn #_"Number" Ops'''addP--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (let [#_"long" lx (.longValue x) #_"long" ly (.longValue y) #_"long" lz (+ lx ly)]
            (if (and (neg? (bit-xor lz lx)) (neg? (bit-xor lz ly)))
                (.add Numbers'BIGINT_OPS, x, y)
                (Numbers'num-1l lz)
            )
        )
    )

    (declare Numbers'multiply-2ll)

    #_override
    (defn #_"Number" Ops'''multiply--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (Numbers'num-1l (Numbers'multiply-2ll (.longValue x), (.longValue y)))
    )

    #_override
    (defn #_"Number" Ops'''multiplyP--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (let [#_"long" lx (.longValue x) #_"long" ly (.longValue y)]
            (if (and (= lx Long/MIN_VALUE) (neg? ly))
                (.multiply Numbers'BIGINT_OPS, x, y)
                (let [#_"long" lz (* lx ly)]
                    (if (and (not= ly 0) (not= (/ lz ly) lx))
                        (.multiply Numbers'BIGINT_OPS, x, y)
                        (Numbers'num-1l lz)
                    )
                )
            )
        )
    )

    (defn #_"long" LongOps'gcd [#_"long" u, #_"long" v] (if (zero? v) u (recur v (% u v))))

    #_override
    (defn #_"Number" Ops'''divide--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (let [#_"long" lx (.longValue x) #_"long" ly (.longValue y)]
            (let-when-not [#_"long" gcd (LongOps'gcd lx, ly)] (zero? gcd) => (Numbers'num-1l 0)
                (let-when-not [lx (/ lx gcd) ly (/ ly gcd)] (= ly 1) => (Numbers'num-1l lx)
                    (let [[lx ly]
                            (when (neg? ly) => [lx ly]
                                [(- lx) (- ly)]
                            )]
                        (Ratio'new (BigInteger/valueOf lx), (BigInteger/valueOf ly))
                    )
                )
            )
        )
    )

    #_override
    (defn #_"Number" Ops'''quotient--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (Numbers'num-1l (/ (.longValue x) (.longValue y)))
    )

    #_override
    (defn #_"Number" Ops'''remainder--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (Numbers'num-1l (% (.longValue x) (.longValue y)))
    )

    #_override
    (defn #_"boolean" Ops'''equiv--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (= (.longValue x) (.longValue y))
    )

    #_override
    (defn #_"boolean" Ops'''lt--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (< (.longValue x) (.longValue y))
    )

    #_override
    (defn #_"boolean" Ops'''lte--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (<= (.longValue x) (.longValue y))
    )

    #_override
    (defn #_"boolean" Ops'''gte--LongOps [#_"LongOps" this, #_"Number" x, #_"Number" y]
        (>= (.longValue x) (.longValue y))
    )

    (declare Numbers'minus-1l)

    #_override
    (defn #_"Number" Ops'''negate--LongOps [#_"LongOps" this, #_"Number" x]
        (let [#_"long" val (.longValue x)]
            (Numbers'num-1l (Numbers'minus-1l val))
        )
    )

    #_override
    (defn #_"Number" Ops'''negateP--LongOps [#_"LongOps" this, #_"Number" x]
        (let [#_"long" val (.longValue x)]
            (if (< Long/MIN_VALUE val)
                (Numbers'num-1l (- val))
                (BigInt'fromBigInteger (.negate (BigInteger/valueOf val)))
            )
        )
    )

    (declare Numbers'inc-1l)

    #_override
    (defn #_"Number" Ops'''inc--LongOps [#_"LongOps" this, #_"Number" x]
        (let [#_"long" val (.longValue x)]
            (Numbers'num-1l (Numbers'inc-1l val))
        )
    )

    #_override
    (defn #_"Number" Ops'''incP--LongOps [#_"LongOps" this, #_"Number" x]
        (let [#_"long" val (.longValue x)]
            (if (< val Long/MAX_VALUE)
                (Numbers'num-1l (inc val))
                (.inc Numbers'BIGINT_OPS, x)
            )
        )
    )

    (declare Numbers'dec-1l)

    #_override
    (defn #_"Number" Ops'''dec--LongOps [#_"LongOps" this, #_"Number" x]
        (let [#_"long" val (.longValue x)]
            (Numbers'num-1l (Numbers'dec-1l val))
        )
    )

    #_override
    (defn #_"Number" Ops'''decP--LongOps [#_"LongOps" this, #_"Number" x]
        (let [#_"long" val (.longValue x)]
            (if (< Long/MIN_VALUE val)
                (Numbers'num-1l (dec val))
                (.dec Numbers'BIGINT_OPS, x)
            )
        )
    )
)

(class-ns DoubleOps
    (defn #_"DoubleOps" DoubleOps'new []
        (OpsP'new)
    )

    #_override
    (defn #_"Ops" Ops'''combine--DoubleOps [#_"DoubleOps" this, #_"Ops" y]
        (.opsWithDouble y, this)
    )

    #_override
    (defn #_"Ops" Ops'''opsWithLong--DoubleOps [#_"DoubleOps" this, #_"LongOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithDouble--DoubleOps [#_"DoubleOps" this, #_"DoubleOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithRatio--DoubleOps [#_"DoubleOps" this, #_"RatioOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithBigInt--DoubleOps [#_"DoubleOps" this, #_"BigIntOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithBigDecimal--DoubleOps [#_"DoubleOps" this, #_"BigDecimalOps" x]
        this
    )

    #_override
    (defn #_"boolean" Ops'''isZero--DoubleOps [#_"DoubleOps" this, #_"Number" x]
        (zero? (.doubleValue x))
    )

    #_override
    (defn #_"boolean" Ops'''isPos--DoubleOps [#_"DoubleOps" this, #_"Number" x]
        (pos? (.doubleValue x))
    )

    #_override
    (defn #_"boolean" Ops'''isNeg--DoubleOps [#_"DoubleOps" this, #_"Number" x]
        (neg? (.doubleValue x))
    )

    #_override
    (defn #_"Number" Ops'''add--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (Double/valueOf (+ (.doubleValue x) (.doubleValue y)))
    )

    #_override
    (defn #_"Number" Ops'''multiply--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (Double/valueOf (* (.doubleValue x) (.doubleValue y)))
    )

    #_override
    (defn #_"Number" Ops'''divide--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (Double/valueOf (/ (.doubleValue x) (.doubleValue y)))
    )

    (declare Numbers'quotient-2dd)

    #_override
    (defn #_"Number" Ops'''quotient--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (Numbers'quotient-2dd (.doubleValue x), (.doubleValue y))
    )

    (declare Numbers'remainder-2dd)

    #_override
    (defn #_"Number" Ops'''remainder--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (Numbers'remainder-2dd (.doubleValue x), (.doubleValue y))
    )

    #_override
    (defn #_"boolean" Ops'''equiv--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (= (.doubleValue x) (.doubleValue y))
    )

    #_override
    (defn #_"boolean" Ops'''lt--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (< (.doubleValue x) (.doubleValue y))
    )

    #_override
    (defn #_"boolean" Ops'''lte--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (<= (.doubleValue x) (.doubleValue y))
    )

    #_override
    (defn #_"boolean" Ops'''gte--DoubleOps [#_"DoubleOps" this, #_"Number" x, #_"Number" y]
        (>= (.doubleValue x) (.doubleValue y))
    )

    #_override
    (defn #_"Number" Ops'''negate--DoubleOps [#_"DoubleOps" this, #_"Number" x]
        (Double/valueOf (- (.doubleValue x)))
    )

    #_override
    (defn #_"Number" Ops'''inc--DoubleOps [#_"DoubleOps" this, #_"Number" x]
        (Double/valueOf (inc (.doubleValue x)))
    )

    #_override
    (defn #_"Number" Ops'''dec--DoubleOps [#_"DoubleOps" this, #_"Number" x]
        (Double/valueOf (dec (.doubleValue x)))
    )
)

(class-ns RatioOps
    (defn #_"RatioOps" RatioOps'new []
        (OpsP'new)
    )

    #_override
    (defn #_"Ops" Ops'''combine--RatioOps [#_"RatioOps" this, #_"Ops" y]
        (.opsWithRatio y, this)
    )

    #_override
    (defn #_"Ops" Ops'''opsWithLong--RatioOps [#_"RatioOps" this, #_"LongOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithDouble--RatioOps [#_"RatioOps" this, #_"DoubleOps" x]
        Numbers'DOUBLE_OPS
    )

    #_override
    (defn #_"Ops" Ops'''opsWithRatio--RatioOps [#_"RatioOps" this, #_"RatioOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithBigInt--RatioOps [#_"RatioOps" this, #_"BigIntOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithBigDecimal--RatioOps [#_"RatioOps" this, #_"BigDecimalOps" x]
        Numbers'BIGDECIMAL_OPS
    )

    #_override
    (defn #_"boolean" Ops'''isZero--RatioOps [#_"RatioOps" this, #_"Number" x]
        (zero? (.signum (:numerator (cast Ratio x))))
    )

    #_override
    (defn #_"boolean" Ops'''isPos--RatioOps [#_"RatioOps" this, #_"Number" x]
        (pos? (.signum (:numerator (cast Ratio x))))
    )

    #_override
    (defn #_"boolean" Ops'''isNeg--RatioOps [#_"RatioOps" this, #_"Number" x]
        (neg? (.signum (:numerator (cast Ratio x))))
    )

    (defn #_"Number" RatioOps'normalizeRet [#_"Number" ret, #_"Number" x, #_"Number" y]
        ret
    )

    (declare Numbers'toRatio)

    #_override
    (defn #_"Number" Ops'''add--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)
              #_"Number" ret (.divide this, (.add (.multiply (:numerator ry), (:denominator rx)), (.multiply (:numerator rx), (:denominator ry))), (.multiply (:denominator ry), (:denominator rx)))]
            (RatioOps'normalizeRet ret, x, y)
        )
    )

    #_override
    (defn #_"Number" Ops'''multiply--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)
              #_"Number" ret (.divide this, (.multiply (:numerator ry), (:numerator rx)), (.multiply (:denominator ry), (:denominator rx)))]
            (RatioOps'normalizeRet ret, x, y)
        )
    )

    #_override
    (defn #_"Number" Ops'''divide--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)
              #_"Number" ret (.divide this, (.multiply (:denominator ry), (:numerator rx)), (.multiply (:numerator ry), (:denominator rx)))]
            (RatioOps'normalizeRet ret, x, y)
        )
    )

    #_override
    (defn #_"Number" Ops'''quotient--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)
              #_"BigInteger" q (.divide (.multiply (:numerator rx), (:denominator ry)), (.multiply (:denominator rx), (:numerator ry)))]
            (RatioOps'normalizeRet (BigInt'fromBigInteger q), x, y)
        )
    )

    (declare Numbers'minus-2oo)
    (declare Numbers'multiply-2oo)

    #_override
    (defn #_"Number" Ops'''remainder--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)
              #_"BigInteger" q (.divide (.multiply (:numerator rx), (:denominator ry)), (.multiply (:denominator rx), (:numerator ry)))
              #_"Number" ret (Numbers'minus-2oo x, (Numbers'multiply-2oo q, y))]
            (RatioOps'normalizeRet ret, x, y)
        )
    )

    #_override
    (defn #_"boolean" Ops'''equiv--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
            (and (.equals (:numerator rx), (:numerator ry)) (.equals (:denominator rx), (:denominator ry)))
        )
    )

    (declare Numbers'lt-2oo)

    #_override
    (defn #_"boolean" Ops'''lt--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
            (Numbers'lt-2oo (.multiply (:numerator rx), (:denominator ry)), (.multiply (:numerator ry), (:denominator rx)))
        )
    )

    (declare Numbers'lte-2oo)

    #_override
    (defn #_"boolean" Ops'''lte--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
            (Numbers'lte-2oo (.multiply (:numerator rx), (:denominator ry)), (.multiply (:numerator ry), (:denominator rx)))
        )
    )

    (declare Numbers'gte-2oo)

    #_override
    (defn #_"boolean" Ops'''gte--RatioOps [#_"RatioOps" this, #_"Number" x, #_"Number" y]
        (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
            (Numbers'gte-2oo (.multiply (:numerator rx), (:denominator ry)), (.multiply (:numerator ry), (:denominator rx)))
        )
    )

    #_override
    (defn #_"Number" Ops'''negate--RatioOps [#_"RatioOps" this, #_"Number" x]
        (let [#_"Ratio" r (cast Ratio x)]
            (Ratio'new (.negate (:numerator r)), (:denominator r))
        )
    )

    (declare Numbers'add-2ol)

    #_override
    (defn #_"Number" Ops'''inc--RatioOps [#_"RatioOps" this, #_"Number" x]
        (Numbers'add-2ol x, 1)
    )

    #_override
    (defn #_"Number" Ops'''dec--RatioOps [#_"RatioOps" this, #_"Number" x]
        (Numbers'add-2ol x, -1)
    )
)

(class-ns BigIntOps
    (defn #_"BigIntOps" BigIntOps'new []
        (OpsP'new)
    )

    #_override
    (defn #_"Ops" Ops'''combine--BigIntOps [#_"BigIntOps" this, #_"Ops" y]
        (.opsWithBigInt y, this)
    )

    #_override
    (defn #_"Ops" Ops'''opsWithLong--BigIntOps [#_"BigIntOps" this, #_"LongOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithDouble--BigIntOps [#_"BigIntOps" this, #_"DoubleOps" x]
        Numbers'DOUBLE_OPS
    )

    #_override
    (defn #_"Ops" Ops'''opsWithRatio--BigIntOps [#_"BigIntOps" this, #_"RatioOps" x]
        Numbers'RATIO_OPS
    )

    #_override
    (defn #_"Ops" Ops'''opsWithBigInt--BigIntOps [#_"BigIntOps" this, #_"BigIntOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithBigDecimal--BigIntOps [#_"BigIntOps" this, #_"BigDecimalOps" x]
        Numbers'BIGDECIMAL_OPS
    )

    (declare Numbers'toBigInt)

    #_override
    (defn #_"boolean" Ops'''isZero--BigIntOps [#_"BigIntOps" this, #_"Number" x]
        (let [#_"BigInt" bx (Numbers'toBigInt x)]
            (zero? (if (some? (:bipart bx)) (.signum (:bipart bx)) (:lpart bx)))
        )
    )

    #_override
    (defn #_"boolean" Ops'''isPos--BigIntOps [#_"BigIntOps" this, #_"Number" x]
        (let [#_"BigInt" bx (Numbers'toBigInt x)]
            (pos? (if (some? (:bipart bx)) (.signum (:bipart bx)) (:lpart bx)))
        )
    )

    #_override
    (defn #_"boolean" Ops'''isNeg--BigIntOps [#_"BigIntOps" this, #_"Number" x]
        (let [#_"BigInt" bx (Numbers'toBigInt x)]
            (neg? (if (some? (:bipart bx)) (.signum (:bipart bx)) (:lpart bx)))
        )
    )

    #_override
    (defn #_"Number" Ops'''add--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (BigInt''add (Numbers'toBigInt x), (Numbers'toBigInt y))
    )

    #_override
    (defn #_"Number" Ops'''multiply--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (BigInt''multiply (Numbers'toBigInt x), (Numbers'toBigInt y))
    )

    (declare Numbers'divide-2ii)

    #_override
    (defn #_"Number" Ops'''divide--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (Numbers'divide-2ii (.toBigInteger this, x), (.toBigInteger this, y))
    )

    #_override
    (defn #_"Number" Ops'''quotient--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (BigInt''quotient (Numbers'toBigInt x), (Numbers'toBigInt y))
    )

    #_override
    (defn #_"Number" Ops'''remainder--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (BigInt''remainder (Numbers'toBigInt x), (Numbers'toBigInt y))
    )

    #_override
    (defn #_"boolean" Ops'''equiv--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (.equals (Numbers'toBigInt x), (Numbers'toBigInt y))
    )

    #_override
    (defn #_"boolean" Ops'''lt--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (BigInt''lt (Numbers'toBigInt x), (Numbers'toBigInt y))
    )

    #_override
    (defn #_"boolean" Ops'''lte--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (<= (.compareTo (.toBigInteger this, x), (.toBigInteger this, y)) 0)
    )

    #_override
    (defn #_"boolean" Ops'''gte--BigIntOps [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
        (>= (.compareTo (.toBigInteger this, x), (.toBigInteger this, y)) 0)
    )

    #_override
    (defn #_"Number" Ops'''negate--BigIntOps [#_"BigIntOps" this, #_"Number" x]
        (BigInt'fromBigInteger (.negate (.toBigInteger this, x)))
    )

    #_override
    (defn #_"Number" Ops'''inc--BigIntOps [#_"BigIntOps" this, #_"Number" x]
        (BigInt'fromBigInteger (.add (.toBigInteger this, x), BigInteger/ONE))
    )

    #_override
    (defn #_"Number" Ops'''dec--BigIntOps [#_"BigIntOps" this, #_"Number" x]
        (BigInt'fromBigInteger (.subtract (.toBigInteger this, x), BigInteger/ONE))
    )
)

(class-ns BigDecimalOps
    (def #_"Var" BigDecimalOps'MATH_CONTEXT (§ soon RT'MATH_CONTEXT))

    (defn #_"BigDecimalOps" BigDecimalOps'new []
        (OpsP'new)
    )

    #_override
    (defn #_"Ops" Ops'''combine--BigDecimalOps [#_"BigDecimalOps" this, #_"Ops" y]
        (.opsWithBigDecimal y, this)
    )

    #_override
    (defn #_"Ops" Ops'''opsWithLong--BigDecimalOps [#_"BigDecimalOps" this, #_"LongOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithDouble--BigDecimalOps [#_"BigDecimalOps" this, #_"DoubleOps" x]
        Numbers'DOUBLE_OPS
    )

    #_override
    (defn #_"Ops" Ops'''opsWithRatio--BigDecimalOps [#_"BigDecimalOps" this, #_"RatioOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithBigInt--BigDecimalOps [#_"BigDecimalOps" this, #_"BigIntOps" x]
        this
    )

    #_override
    (defn #_"Ops" Ops'''opsWithBigDecimal--BigDecimalOps [#_"BigDecimalOps" this, #_"BigDecimalOps" x]
        this
    )

    #_override
    (defn #_"boolean" Ops'''isZero--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x]
        (let [#_"BigDecimal" bx (cast BigDecimal x)]
            (zero? (.signum bx))
        )
    )

    #_override
    (defn #_"boolean" Ops'''isPos--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x]
        (let [#_"BigDecimal" bx (cast BigDecimal x)]
            (pos? (.signum bx))
        )
    )

    #_override
    (defn #_"boolean" Ops'''isNeg--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x]
        (let [#_"BigDecimal" bx (cast BigDecimal x)]
            (neg? (.signum bx))
        )
    )

    #_override
    (defn #_"Number" Ops'''add--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (let [#_"MathContext" mc (cast MathContext (.deref BigDecimalOps'MATH_CONTEXT))]
            (if (nil? mc) (.add (.toBigDecimal this, x), (.toBigDecimal this, y)) (.add (.toBigDecimal this, x), (.toBigDecimal this, y), mc))
        )
    )

    #_override
    (defn #_"Number" Ops'''multiply--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (let [#_"MathContext" mc (cast MathContext (.deref BigDecimalOps'MATH_CONTEXT))]
            (if (nil? mc) (.multiply (.toBigDecimal this, x), (.toBigDecimal this, y)) (.multiply (.toBigDecimal this, x), (.toBigDecimal this, y), mc))
        )
    )

    #_override
    (defn #_"Number" Ops'''divide--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (let [#_"MathContext" mc (cast MathContext (.deref BigDecimalOps'MATH_CONTEXT))]
            (if (nil? mc) (.divide (.toBigDecimal this, x), (.toBigDecimal this, y)) (.divide (.toBigDecimal this, x), (.toBigDecimal this, y), mc))
        )
    )

    #_override
    (defn #_"Number" Ops'''quotient--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (let [#_"MathContext" mc (cast MathContext (.deref BigDecimalOps'MATH_CONTEXT))]
            (if (nil? mc) (.divideToIntegralValue (.toBigDecimal this, x), (.toBigDecimal this, y)) (.divideToIntegralValue (.toBigDecimal this, x), (.toBigDecimal this, y), mc))
        )
    )

    #_override
    (defn #_"Number" Ops'''remainder--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (let [#_"MathContext" mc (cast MathContext (.deref BigDecimalOps'MATH_CONTEXT))]
            (if (nil? mc) (.remainder (.toBigDecimal this, x), (.toBigDecimal this, y)) (.remainder (.toBigDecimal this, x), (.toBigDecimal this, y), mc))
        )
    )

    #_override
    (defn #_"boolean" Ops'''equiv--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (zero? (.compareTo (.toBigDecimal this, x), (.toBigDecimal this, y)))
    )

    #_override
    (defn #_"boolean" Ops'''lt--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (neg? (.compareTo (.toBigDecimal this, x), (.toBigDecimal this, y)))
    )

    #_override
    (defn #_"boolean" Ops'''lte--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (<= (.compareTo (.toBigDecimal this, x), (.toBigDecimal this, y)) 0)
    )

    #_override
    (defn #_"boolean" Ops'''gte--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x, #_"Number" y]
        (>= (.compareTo (.toBigDecimal this, x), (.toBigDecimal this, y)) 0)
    )

    #_override
    (defn #_"Number" Ops'''negate--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x]
        (let [#_"MathContext" mc (cast MathContext (.deref BigDecimalOps'MATH_CONTEXT))]
            (if (nil? mc) (.negate (cast BigDecimal x)) (.negate (cast BigDecimal x), mc))
        )
    )

    #_override
    (defn #_"Number" Ops'''inc--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x]
        (let [#_"MathContext" mc (cast MathContext (.deref BigDecimalOps'MATH_CONTEXT))
              #_"BigDecimal" bx (cast BigDecimal x)]
            (if (nil? mc) (.add bx, BigDecimal/ONE) (.add bx, BigDecimal/ONE, mc))
        )
    )

    #_override
    (defn #_"Number" Ops'''dec--BigDecimalOps [#_"BigDecimalOps" this, #_"Number" x]
        (let [#_"MathContext" mc (cast MathContext (.deref BigDecimalOps'MATH_CONTEXT))
              #_"BigDecimal" bx (cast BigDecimal x)]
            (if (nil? mc) (.subtract bx, BigDecimal/ONE) (.subtract bx, BigDecimal/ONE, mc))
        )
    )
)

(def Category'enum-set
    (hash-set
        :Category'INTEGER
        :Category'FLOATING
        :Category'DECIMAL
        :Category'RATIO
    )
)

(class-ns Numbers
    (def #_"LongOps"       Numbers'LONG_OPS       (LongOps'new)      )
    (def #_"DoubleOps"     Numbers'DOUBLE_OPS     (DoubleOps'new)    )
    (def #_"RatioOps"      Numbers'RATIO_OPS      (RatioOps'new)     )
    (def #_"BigIntOps"     Numbers'BIGINT_OPS     (BigIntOps'new)    )
    (def #_"BigDecimalOps" Numbers'BIGDECIMAL_OPS (BigDecimalOps'new))

    (defn #_"Ops" Numbers'ops [#_"Object" x]
        (condp = (.getClass x)
            Integer    Numbers'LONG_OPS
            Long       Numbers'LONG_OPS
            BigInt     Numbers'BIGINT_OPS
            BigInteger Numbers'BIGINT_OPS
            Ratio      Numbers'RATIO_OPS
            Float      Numbers'DOUBLE_OPS
            Double     Numbers'DOUBLE_OPS
            BigDecimal Numbers'BIGDECIMAL_OPS
                       Numbers'LONG_OPS
        )
    )

    (defn #_"Category" Numbers'category [#_"Object" x]
        (condp = (.getClass x)
            Integer    :Category'INTEGER
            Long       :Category'INTEGER
            BigInt     :Category'INTEGER
            Ratio      :Category'RATIO
            Float      :Category'FLOATING
            Double     :Category'FLOATING
            BigDecimal :Category'DECIMAL
                       :Category'INTEGER
        )
    )

    (defn #_"boolean" Numbers'isInteger [#_"Object" x]
        (or (instance? Integer x) (instance? Long x) (instance? BigInt x) (instance? BigInteger x))
    )

    (defn #_"boolean" Numbers'isNaN [#_"Object" x]
        (or (and (instance? Double x) (.isNaN (cast Double x))) (and (instance? Float x) (.isNaN (cast Float x))))
    )

    (defn #_"Number" Numbers'num-1l [#_"long"   x] (Long/valueOf   x))
    (defn #_"Number" Numbers'num-1f [#_"float"  x] (Float/valueOf  x))
    (defn #_"Number" Numbers'num-1d [#_"double" x] (Double/valueOf x))
    (defn #_"Number" Numbers'num-1o [#_"Object" x] (cast Number    x))

    (defn #_"boolean" Numbers'isZero-1o [#_"Object" x] (.isZero (Numbers'ops x), (cast Number x)))
    (defn #_"boolean" Numbers'isPos-1o  [#_"Object" x] (.isPos  (Numbers'ops x), (cast Number x)))
    (defn #_"boolean" Numbers'isNeg-1o  [#_"Object" x] (.isNeg  (Numbers'ops x), (cast Number x)))

    (defn #_"Number" Numbers'minus-1o  [#_"Object" x] (.negate  (Numbers'ops x), (cast Number x)))
    (defn #_"Number" Numbers'minusP-1o [#_"Object" x] (.negateP (Numbers'ops x), (cast Number x)))
    (defn #_"Number" Numbers'inc-1o    [#_"Object" x] (.inc     (Numbers'ops x), (cast Number x)))
    (defn #_"Number" Numbers'incP-1o   [#_"Object" x] (.incP    (Numbers'ops x), (cast Number x)))
    (defn #_"Number" Numbers'dec-1o    [#_"Object" x] (.dec     (Numbers'ops x), (cast Number x)))
    (defn #_"Number" Numbers'decP-1o   [#_"Object" x] (.decP    (Numbers'ops x), (cast Number x)))

    (defn #_"Number" Numbers'add-2oo [#_"Object" x, #_"Object" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.add (cast Number x), (cast Number y)))
    )

    (defn #_"Number" Numbers'addP-2oo [#_"Object" x, #_"Object" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.addP (cast Number x), (cast Number y)))
    )

    (defn #_"Number" Numbers'minus-2oo [#_"Object" x, #_"Object" y]
        (let [#_"Ops" yops (Numbers'ops y)]
            (-> (.combine (Numbers'ops x), yops) (.add (cast Number x), (.negate yops, (cast Number y))))
        )
    )

    (defn #_"Number" Numbers'minusP-2oo [#_"Object" x, #_"Object" y]
        (let [#_"Ops" yops (Numbers'ops y)
              #_"Number" negativeY (.negateP yops, (cast Number y))
              #_"Ops" negativeYOps (Numbers'ops negativeY)]
            (-> (.combine (Numbers'ops x), negativeYOps) (.addP (cast Number x), negativeY))
        )
    )

    (defn #_"Number" Numbers'multiply-2oo [#_"Object" x, #_"Object" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.multiply (cast Number x), (cast Number y)))
    )

    (defn #_"Number" Numbers'multiplyP-2oo [#_"Object" x, #_"Object" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.multiplyP (cast Number x), (cast Number y)))
    )

    (defn #_"Number" Numbers'divide-2oo [#_"Object" x, #_"Object" y]
        (cond
            (Numbers'isNaN x) (cast Number x)
            (Numbers'isNaN y) (cast Number y)
            :else
                (let [#_"Ops" yops (Numbers'ops y)]
                    (when (.isZero yops, (cast Number y))
                        (throw (ArithmeticException. "Divide by zero"))
                    )
                    (-> (.combine (Numbers'ops x), yops) (.divide (cast Number x), (cast Number y)))
                )
        )
    )

    (defn #_"Number" Numbers'quotient-2oo [#_"Object" x, #_"Object" y]
        (let [#_"Ops" yops (Numbers'ops y)]
            (when (.isZero yops, (cast Number y))
                (throw (ArithmeticException. "Divide by zero"))
            )
            (-> (.combine (Numbers'ops x), yops) (.quotient (cast Number x), (cast Number y)))
        )
    )

    (defn #_"Number" Numbers'remainder-2oo [#_"Object" x, #_"Object" y]
        (let [#_"Ops" yops (Numbers'ops y)]
            (when (.isZero yops, (cast Number y))
                (throw (ArithmeticException. "Divide by zero"))
            )
            (-> (.combine (Numbers'ops x), yops) (.remainder (cast Number x), (cast Number y)))
        )
    )

    (defn #_"double" Numbers'quotient-2dd [#_"double" n, #_"double" d]
        (when (zero? d)
            (throw (ArithmeticException. "Divide by zero"))
        )

        (let [#_"double" q (/ n d)]
            (cond (<= Long/MIN_VALUE q Long/MAX_VALUE)
                (do
                    (double (long q))
                )
                :else ;; bigint quotient
                (do
                    (.doubleValue (.toBigInteger (BigDecimal. q)))
                )
            )
        )
    )

    (defn #_"double" Numbers'remainder-2dd [#_"double" n, #_"double" d]
        (when (zero? d)
            (throw (ArithmeticException. "Divide by zero"))
        )

        (let [#_"double" q (/ n d)]
            (cond (<= Long/MIN_VALUE q Long/MAX_VALUE)
                (do
                    (- n (* (long q) d))
                )
                :else ;; bigint quotient
                (let [#_"Number" bq (.toBigInteger (BigDecimal. q))]
                    (- n (* (.doubleValue bq) d))
                )
            )
        )
    )

    (defn #_"boolean" Numbers'equiv-2nn [#_"Number" x, #_"Number" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.equiv x, y))
    )

    (defn #_"boolean" Numbers'equiv-2oo [#_"Object" x, #_"Object" y]
        (Numbers'equiv-2nn (cast Number x), (cast Number y))
    )

    (defn #_"boolean" Numbers'equal [#_"Number" x, #_"Number" y]
        (and (= (Numbers'category x) (Numbers'category y)) (.equiv (.combine (Numbers'ops x), (Numbers'ops y)), x, y))
    )

    (defn #_"boolean" Numbers'lt-2oo [#_"Object" x, #_"Object" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.lt (cast Number x), (cast Number y)))
    )

    (defn #_"boolean" Numbers'lte-2oo [#_"Object" x, #_"Object" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.lte (cast Number x), (cast Number y)))
    )

    (defn #_"boolean" Numbers'gt-2oo [#_"Object" x, #_"Object" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.lt (cast Number y), (cast Number x)))
    )

    (defn #_"boolean" Numbers'gte-2oo [#_"Object" x, #_"Object" y]
        (-> (.combine (Numbers'ops x), (Numbers'ops y)) (.gte (cast Number x), (cast Number y)))
    )

    (defn #_"int" Numbers'compare [#_"Number" x, #_"Number" y]
        (let [#_"Ops" ops (.combine (Numbers'ops x), (Numbers'ops y))]
            (cond (.lt ops, x, y) -1 (.lt ops, y, x) 1 :else 0)
        )
    )

    (defn #_"BigInt" Numbers'toBigInt [#_"Object" x]
        (cond
            (instance? BigInt x)     (cast BigInt x)
            (instance? BigInteger x) (BigInt'fromBigInteger (cast BigInteger x))
            :else                    (BigInt'fromLong (.longValue (cast Number x)))
        )
    )

    (defn #_"BigInteger" Numbers'toBigInteger [#_"Object" x]
        (cond
            (instance? BigInteger x) (cast BigInteger x)
            (instance? BigInt x)     (BigInt''toBigInteger (cast BigInt x))
            :else                    (BigInteger/valueOf (.longValue (cast Number x)))
        )
    )

    (defn #_"BigDecimal" Numbers'toBigDecimal [#_"Object" x]
        (cond
            (instance? BigDecimal x)
                (cast BigDecimal x)
            (instance? BigInt x)
                (let [#_"BigInt" bi (cast BigInt x)]
                    (if (nil? (:bipart bi))
                        (BigDecimal/valueOf (:lpart bi))
                        (BigDecimal. (:bipart bi))
                    )
                )
            (instance? BigInteger x)
                (BigDecimal. (cast BigInteger x))
            (instance? Double x)
                (BigDecimal. (.doubleValue (cast Number x)))
            (instance? Float x)
                (BigDecimal. (.doubleValue (cast Number x)))
            (instance? Ratio x)
                (let [#_"Ratio" r (cast Ratio x)]
                    (cast BigDecimal (Numbers'divide-2oo (BigDecimal. (:numerator r)), (:denominator r)))
                )
            :else
                (BigDecimal/valueOf (.longValue (cast Number x)))
        )
    )

    (defn #_"Ratio" Numbers'toRatio [#_"Object" x]
        (cond
            (instance? Ratio x)
                (cast Ratio x)
            (instance? BigDecimal x)
                (let [#_"BigDecimal" bx (cast BigDecimal x) #_"BigInteger" bv (.unscaledValue bx) #_"int" scale (.scale bx)]
                    (if (neg? scale)
                        (Ratio'new (.multiply bv, (.pow BigInteger/TEN, (- scale))), BigInteger/ONE)
                        (Ratio'new bv, (.pow BigInteger/TEN, scale))
                    )
                )
            :else
                (Ratio'new (Numbers'toBigInteger x), BigInteger/ONE)
        )
    )

    (defn #_"Number" Numbers'rationalize [#_"Number" x]
        (cond
            (or (instance? Float x) (instance? Double x))
                (Numbers'rationalize (BigDecimal/valueOf (.doubleValue x)))
            (instance? BigDecimal x)
                (let [#_"BigDecimal" bx (cast BigDecimal x) #_"BigInteger" bv (.unscaledValue bx) #_"int" scale (.scale bx)]
                    (if (neg? scale)
                        (BigInt'fromBigInteger (.multiply bv, (.pow BigInteger/TEN, (- scale))))
                        (Numbers'divide-2ii bv, (.pow BigInteger/TEN, scale))
                    )
                )
            :else
                x
        )
    )

    (defn #_"Number" Numbers'reduceBigInt [#_"BigInt" val]
        (or (:bipart val) (Numbers'num-1l (:lpart val)))
    )

    (defn #_"Number" Numbers'divide-2ii [#_"BigInteger" n, #_"BigInteger" d]
        (when-not (.equals d, BigInteger/ZERO) => (throw (ArithmeticException. "Divide by zero"))
            (let [#_"BigInteger" gcd (.gcd n, d)]
                (when-not (.equals gcd, BigInteger/ZERO) => BigInt'ZERO
                    (let [n (.divide n, gcd) d (.divide d, gcd)]
                        (cond
                            (.equals d, BigInteger/ONE)
                                (BigInt'fromBigInteger n)
                            (.equals d, (.negate BigInteger/ONE))
                                (BigInt'fromBigInteger (.negate n))
                            :else
                                (Ratio'new (if (neg? (.signum d)) (.negate n) n), (if (neg? (.signum d)) (.negate d) d))
                        )
                    )
                )
            )
        )
    )

    (defn #_"long" Numbers'bitOpsCast [#_"Object" x]
        (let [#_"Class" xc (.getClass x)]               ;; no bignums, no decimals
            (when (any = xc Long Integer Short Byte) => (throw (IllegalArgumentException. (str "bit operation not supported for: " xc)))
                (RT'longCast-1o x)
            )
        )
    )

    (defn #_"int" Numbers'shiftLeftInt [#_"int" x, #_"int" n]
        (<< x n)
    )

    (defn #_"long" Numbers'shiftLeft-2ll [#_"long"   x, #_"long"   n] (<< x n))
    (defn #_"long" Numbers'shiftLeft-2oo [#_"Object" x, #_"Object" n] (Numbers'shiftLeft-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast n)))
    (defn #_"long" Numbers'shiftLeft-2ol [#_"Object" x, #_"long"   n] (Numbers'shiftLeft-2ll (Numbers'bitOpsCast x),                     n ))
    (defn #_"long" Numbers'shiftLeft-2lo [#_"long"   x, #_"Object" n] (Numbers'shiftLeft-2ll                     x , (Numbers'bitOpsCast n)))

    (defn #_"int" Numbers'shiftRightInt [#_"int" x, #_"int" n]
        (>> x n)
    )

    (defn #_"long" Numbers'shiftRight-2ll [#_"long"   x, #_"long"   n] (>> x n))
    (defn #_"long" Numbers'shiftRight-2oo [#_"Object" x, #_"Object" n] (Numbers'shiftRight-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast n)))
    (defn #_"long" Numbers'shiftRight-2ol [#_"Object" x, #_"long"   n] (Numbers'shiftRight-2ll (Numbers'bitOpsCast x),                     n ))
    (defn #_"long" Numbers'shiftRight-2lo [#_"long"   x, #_"Object" n] (Numbers'shiftRight-2ll                     x , (Numbers'bitOpsCast n)))

    (defn #_"int" Numbers'unsignedShiftRightInt [#_"int" x, #_"int" n]
        (>>> x n)
    )

    (defn #_"long" Numbers'unsignedShiftRight-2ll [#_"long"   x, #_"long"   n] (>>> x n))
    (defn #_"long" Numbers'unsignedShiftRight-2oo [#_"Object" x, #_"Object" n] (Numbers'unsignedShiftRight-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast n)))
    (defn #_"long" Numbers'unsignedShiftRight-2ol [#_"Object" x, #_"long"   n] (Numbers'unsignedShiftRight-2ll (Numbers'bitOpsCast x),                     n ))
    (defn #_"long" Numbers'unsignedShiftRight-2lo [#_"long"   x, #_"Object" n] (Numbers'unsignedShiftRight-2ll                     x , (Numbers'bitOpsCast n)))

    (defn #_"float[]" Numbers'float_array-2 [#_"int" size, #_"Object" init]
        (let [#_"float[]" ret (.float-array size)]
            (if (instance? Number init)
                (let [#_"float" f (.floatValue (cast Number init))]
                    (dotimes [#_"int" i (alength ret)]
                        (aset ret i f)
                    )
                )
                (let [#_"ISeq" s (RT'seq init)]
                    (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                        (aset ret i (.floatValue (cast Number (.first s))))
                    )
                )
            )
            ret
        )
    )

    (defn #_"float[]" Numbers'float_array-1 [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (.float-array (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq)
                  #_"int" size (RT'count s)
                  #_"float[]" ret (.float-array size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset ret i (.floatValue (cast Number (.first s))))
                )
                ret
            )
        )
    )

    (defn #_"double[]" Numbers'double_array-2 [#_"int" size, #_"Object" init]
        (let [#_"double[]" ret (.double-array size)]
            (if (instance? Number init)
                (let [#_"double" f (.doubleValue (cast Number init))]
                    (dotimes [#_"int" i (alength ret)]
                        (aset ret i f)
                    )
                )
                (let [#_"ISeq" s (RT'seq init)]
                    (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                        (aset ret i (.doubleValue (cast Number (.first s))))
                    )
                )
            )
            ret
        )
    )

    (defn #_"double[]" Numbers'double_array-1 [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (.double-array (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq)
                  #_"int" size (RT'count s)
                  #_"double[]" ret (.double-array size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset ret i (.doubleValue (cast Number (.first s))))
                )
                ret
            )
        )
    )

    (defn #_"int[]" Numbers'int_array-2 [#_"int" size, #_"Object" init]
        (let [#_"int[]" ret (.int-array size)]
            (if (instance? Number init)
                (let [#_"int" f (.intValue (cast Number init))]
                    (dotimes [#_"int" i (alength ret)]
                        (aset ret i f)
                    )
                )
                (let [#_"ISeq" s (RT'seq init)]
                    (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                        (aset ret i (.intValue (cast Number (.first s))))
                    )
                )
            )
            ret
        )
    )

    (defn #_"int[]" Numbers'int_array-1 [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (.int-array (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq)
                  #_"int" size (RT'count s)
                  #_"int[]" ret (.int-array size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset ret i (.intValue (cast Number (.first s))))
                )
                ret
            )
        )
    )

    (defn #_"long[]" Numbers'long_array-2 [#_"int" size, #_"Object" init]
        (let [#_"long[]" ret (.long-array size)]
            (if (instance? Number init)
                (let [#_"long" f (.longValue (cast Number init))]
                    (dotimes [#_"int" i (alength ret)]
                        (aset ret i f)
                    )
                )
                (let [#_"ISeq" s (RT'seq init)]
                    (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                        (aset ret i (.longValue (cast Number (.first s))))
                    )
                )
            )
            ret
        )
    )

    (defn #_"long[]" Numbers'long_array-1 [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (.long-array (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq)
                  #_"int" size (RT'count s)
                  #_"long[]" ret (.long-array size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset ret i (.longValue (cast Number (.first s))))
                )
                ret
            )
        )
    )

    (defn #_"short[]" Numbers'short_array-2 [#_"int" size, #_"Object" init]
        (let [#_"short[]" ret (.short-array size)]
            (if (instance? Short init)
                (let [#_"short" s (cast Short init)]
                    (dotimes [#_"int" i (alength ret)]
                        (aset ret i s)
                    )
                )
                (let [#_"ISeq" s (RT'seq init)]
                    (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                        (aset ret i (.shortValue (cast Number (.first s))))
                    )
                )
            )
            ret
        )
    )

    (defn #_"short[]" Numbers'short_array-1 [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (.short-array (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq)
                  #_"int" size (RT'count s)
                  #_"short[]" ret (.short-array size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset ret i (.shortValue (cast Number (.first s))))
                )
                ret
            )
        )
    )

    (defn #_"char[]" Numbers'char_array-2 [#_"int" size, #_"Object" init]
        (let [#_"char[]" ret (.char-array size)]
            (if (instance? Character init)
                (let [#_"char" c (cast Character init)]
                    (dotimes [#_"int" i (alength ret)]
                        (aset ret i c)
                    )
                )
                (let [#_"ISeq" s (RT'seq init)]
                    (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                        (aset ret i (cast Character (.first s)))
                    )
                )
            )
            ret
        )
    )

    (defn #_"char[]" Numbers'char_array-1 [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (.char-array (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq)
                  #_"int" size (RT'count s)
                  #_"char[]" ret (.char-array size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset ret i (cast Character (.first s)))
                )
                ret
            )
        )
    )

    (defn #_"byte[]" Numbers'byte_array-2 [#_"int" size, #_"Object" init]
        (let [#_"byte[]" ret (.byte-array size)]
            (if (instance? Byte init)
                (let [#_"byte" b (cast Byte init)]
                    (dotimes [#_"int" i (alength ret)]
                        (aset ret i b)
                    )
                )
                (let [#_"ISeq" s (RT'seq init)]
                    (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                        (aset ret i (.byteValue (cast Number (.first s))))
                    )
                )
            )
            ret
        )
    )

    (defn #_"byte[]" Numbers'byte_array-1 [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (.byte-array (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq)
                  #_"int" size (RT'count s)
                  #_"byte[]" ret (.byte-array size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset ret i (.byteValue (cast Number (.first s))))
                )
                ret
            )
        )
    )

    (defn #_"boolean[]" Numbers'boolean_array-2 [#_"int" size, #_"Object" init]
        (let [#_"boolean[]" ret (.boolean-array size)]
            (if (instance? Boolean init)
                (let [#_"boolean" b (cast Boolean init)]
                    (dotimes [#_"int" i (alength ret)]
                        (aset ret i b)
                    )
                )
                (let [#_"ISeq" s (RT'seq init)]
                    (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                        (aset ret i (cast Boolean (.first s)))
                    )
                )
            )
            ret
        )
    )

    (defn #_"boolean[]" Numbers'boolean_array-1 [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (.boolean-array (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq)
                  #_"int" size (RT'count s)
                  #_"boolean[]" ret (.boolean-array size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset ret i (cast Boolean (.first s)))
                )
                ret
            )
        )
    )

    (defn #_"boolean[]" Numbers'booleans [#_"Object" array] (cast Compiler'BOOLEANS_CLASS array))
    (defn #_"byte[]"    Numbers'bytes    [#_"Object" array] (cast Compiler'BYTES_CLASS    array))
    (defn #_"short[]"   Numbers'shorts   [#_"Object" array] (cast Compiler'SHORTS_CLASS   array))
    (defn #_"char[]"    Numbers'chars    [#_"Object" array] (cast Compiler'CHARS_CLASS    array))
    (defn #_"int[]"     Numbers'ints     [#_"Object" array] (cast Compiler'INTS_CLASS     array))
    (defn #_"long[]"    Numbers'longs    [#_"Object" array] (cast Compiler'LONGS_CLASS    array))
    (defn #_"float[]"   Numbers'floats   [#_"Object" array] (cast Compiler'FLOATS_CLASS   array))
    (defn #_"double[]"  Numbers'doubles  [#_"Object" array] (cast Compiler'DOUBLES_CLASS  array))

    (defn #_"double" Numbers'add-2dd    [#_"double" x, #_"double" y] (+ x y))
    (defn #_"double" Numbers'addP-2dd   [#_"double" x, #_"double" y] (+ x y))
    (defn #_"double" Numbers'minus-2dd  [#_"double" x, #_"double" y] (- x y))
    (defn #_"double" Numbers'minusP-2dd [#_"double" x, #_"double" y] (- x y))

    (defn #_"double" Numbers'minus-1d  [#_"double" x] (- x))
    (defn #_"double" Numbers'minusP-1d [#_"double" x] (- x))
    (defn #_"double" Numbers'inc-1d    [#_"double" x] (inc x))
    (defn #_"double" Numbers'incP-1d   [#_"double" x] (inc x))
    (defn #_"double" Numbers'dec-1d    [#_"double" x] (dec x))
    (defn #_"double" Numbers'decP-1d   [#_"double" x] (dec x))

    (defn #_"double" Numbers'multiply-2dd  [#_"double" x, #_"double" y] (* x y))
    (defn #_"double" Numbers'multiplyP-2dd [#_"double" x, #_"double" y] (* x y))
    (defn #_"double" Numbers'divide-2dd    [#_"double" x, #_"double" y] (/ x y))

    (defn #_"boolean" Numbers'equiv-2dd [#_"double" x, #_"double" y] (= x y))
    (defn #_"boolean" Numbers'lt-2dd    [#_"double" x, #_"double" y] (< x y))
    (defn #_"boolean" Numbers'lte-2dd   [#_"double" x, #_"double" y] (<= x y))
    (defn #_"boolean" Numbers'gt-2dd    [#_"double" x, #_"double" y] (> x y))
    (defn #_"boolean" Numbers'gte-2dd   [#_"double" x, #_"double" y] (>= x y))

    (defn #_"boolean" Numbers'isPos-1d  [#_"double" x] (> x 0))
    (defn #_"boolean" Numbers'isNeg-1d  [#_"double" x] (< x 0))
    (defn #_"boolean" Numbers'isZero-1d [#_"double" x] (zero? x))

    (defn #_"int" Numbers'unchecked_int_add       [#_"int" x, #_"int" y] (+ x y))
    (defn #_"int" Numbers'unchecked_int_subtract  [#_"int" x, #_"int" y] (- x y))
    (defn #_"int" Numbers'unchecked_int_multiply  [#_"int" x, #_"int" y] (* x y))
    (defn #_"int" Numbers'unchecked_int_divide    [#_"int" x, #_"int" y] (/ x y))
    (defn #_"int" Numbers'unchecked_int_remainder [#_"int" x, #_"int" y] (% x y))

    (defn #_"int" Numbers'unchecked_int_inc    [#_"int" x] (inc x))
    (defn #_"int" Numbers'unchecked_int_dec    [#_"int" x] (dec x))
    (defn #_"int" Numbers'unchecked_int_negate [#_"int" x] (- x)  )

    (defn #_"long" Numbers'not-1l [#_"long"   x] (bit-not x))
    (defn #_"long" Numbers'not-1o [#_"Object" x] (Numbers'not-1l (Numbers'bitOpsCast x)))

    (defn #_"long" Numbers'and-2ll [#_"long"   x, #_"long"   y] (& x y))
    (defn #_"long" Numbers'and-2oo [#_"Object" x, #_"Object" y] (Numbers'and-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast y)))
    (defn #_"long" Numbers'and-2ol [#_"Object" x, #_"long"   y] (Numbers'and-2ll (Numbers'bitOpsCast x),                     y ))
    (defn #_"long" Numbers'and-2lo [#_"long"   x, #_"Object" y] (Numbers'and-2ll                     x , (Numbers'bitOpsCast y)))

    (defn #_"long" Numbers'or-2ll [#_"long"   x, #_"long"   y] (| x y))
    (defn #_"long" Numbers'or-2oo [#_"Object" x, #_"Object" y] (Numbers'or-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast y)))
    (defn #_"long" Numbers'or-2ol [#_"Object" x, #_"long"   y] (Numbers'or-2ll (Numbers'bitOpsCast x),                     y ))
    (defn #_"long" Numbers'or-2lo [#_"long"   x, #_"Object" y] (Numbers'or-2ll                     x , (Numbers'bitOpsCast y)))

    (defn #_"long" Numbers'xor-2ll [#_"long"   x, #_"long"   y] (bit-xor x y))
    (defn #_"long" Numbers'xor-2oo [#_"Object" x, #_"Object" y] (Numbers'xor-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast y)))
    (defn #_"long" Numbers'xor-2ol [#_"Object" x, #_"long"   y] (Numbers'xor-2ll (Numbers'bitOpsCast x),                     y ))
    (defn #_"long" Numbers'xor-2lo [#_"long"   x, #_"Object" y] (Numbers'xor-2ll                     x , (Numbers'bitOpsCast y)))

    (defn #_"long" Numbers'andNot-2ll [#_"long"   x, #_"long"   y] (& x (bit-not y)))
    (defn #_"long" Numbers'andNot-2oo [#_"Object" x, #_"Object" y] (Numbers'andNot-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast y)))
    (defn #_"long" Numbers'andNot-2ol [#_"Object" x, #_"long"   y] (Numbers'andNot-2ll (Numbers'bitOpsCast x),                     y ))
    (defn #_"long" Numbers'andNot-2lo [#_"long"   x, #_"Object" y] (Numbers'andNot-2ll                     x , (Numbers'bitOpsCast y)))

    (defn #_"long" Numbers'clearBit-2ll [#_"long"   x, #_"long"   n] (& x (bit-not (<< 1 n))))
    (defn #_"long" Numbers'clearBit-2oo [#_"Object" x, #_"Object" n] (Numbers'clearBit-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast n)))
    (defn #_"long" Numbers'clearBit-2ol [#_"Object" x, #_"long"   n] (Numbers'clearBit-2ll (Numbers'bitOpsCast x),                     n ))
    (defn #_"long" Numbers'clearBit-2lo [#_"long"   x, #_"Object" n] (Numbers'clearBit-2ll                     x , (Numbers'bitOpsCast n)))

    (defn #_"long" Numbers'setBit-2ll [#_"long"   x, #_"long"   n] (| x (<< 1 n)))
    (defn #_"long" Numbers'setBit-2oo [#_"Object" x, #_"Object" n] (Numbers'setBit-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast n)))
    (defn #_"long" Numbers'setBit-2ol [#_"Object" x, #_"long"   n] (Numbers'setBit-2ll (Numbers'bitOpsCast x),                     n ))
    (defn #_"long" Numbers'setBit-2lo [#_"long"   x, #_"Object" n] (Numbers'setBit-2ll                     x , (Numbers'bitOpsCast n)))

    (defn #_"long" Numbers'flipBit-2ll [#_"long"   x, #_"long"   n] (bit-xor x (<< 1 n)))
    (defn #_"long" Numbers'flipBit-2oo [#_"Object" x, #_"Object" n] (Numbers'flipBit-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast n)))
    (defn #_"long" Numbers'flipBit-2ol [#_"Object" x, #_"long"   n] (Numbers'flipBit-2ll (Numbers'bitOpsCast x),                     n ))
    (defn #_"long" Numbers'flipBit-2lo [#_"long"   x, #_"Object" n] (Numbers'flipBit-2ll                     x , (Numbers'bitOpsCast n)))

    (defn #_"boolean" Numbers'testBit-2ll [#_"long"   x, #_"long"   n] (not= (& x (<< 1 n)) 0))
    (defn #_"boolean" Numbers'testBit-2oo [#_"Object" x, #_"Object" n] (Numbers'testBit-2ll (Numbers'bitOpsCast x), (Numbers'bitOpsCast n)))
    (defn #_"boolean" Numbers'testBit-2ol [#_"Object" x, #_"long"   n] (Numbers'testBit-2ll (Numbers'bitOpsCast x),                     n ))
    (defn #_"boolean" Numbers'testBit-2lo [#_"long"   x, #_"Object" n] (Numbers'testBit-2ll                     x , (Numbers'bitOpsCast n)))

    (defn #_"Number" Numbers'quotient-2do [#_"double" x, #_"Object" y] (Numbers'quotient-2oo (cast Object x),              y ))
    (defn #_"Number" Numbers'quotient-2od [#_"Object" x, #_"double" y] (Numbers'quotient-2oo              x , (cast Object y)))
    (defn #_"Number" Numbers'quotient-2lo [#_"long"   x, #_"Object" y] (Numbers'quotient-2oo (cast Object x),              y ))
    (defn #_"Number" Numbers'quotient-2ol [#_"Object" x, #_"long"   y] (Numbers'quotient-2oo              x , (cast Object y)))
    (defn #_"double" Numbers'quotient-2dl [#_"double" x, #_"long"   y] (Numbers'quotient-2dd              x ,      (double y)))
    (defn #_"double" Numbers'quotient-2ld [#_"long"   x, #_"double" y] (Numbers'quotient-2dd      (double x),              y ))

    (defn #_"Number" Numbers'remainder-2do [#_"double" x, #_"Object" y] (Numbers'remainder-2oo (cast Object x),              y ))
    (defn #_"Number" Numbers'remainder-2od [#_"Object" x, #_"double" y] (Numbers'remainder-2oo              x , (cast Object y)))
    (defn #_"Number" Numbers'remainder-2lo [#_"long"   x, #_"Object" y] (Numbers'remainder-2oo (cast Object x),              y ))
    (defn #_"Number" Numbers'remainder-2ol [#_"Object" x, #_"long"   y] (Numbers'remainder-2oo              x , (cast Object y)))
    (defn #_"double" Numbers'remainder-2dl [#_"double" x, #_"long"   y] (Numbers'remainder-2dd              x ,      (double y)))
    (defn #_"double" Numbers'remainder-2ld [#_"long"   x, #_"double" y] (Numbers'remainder-2dd      (double x),              y ))

    (defn #_"int" Numbers'throwIntOverflow []
        (throw (ArithmeticException. "integer overflow"))
    )

    (defn #_"long" Numbers'add-2ll [#_"long" x, #_"long" y]
        (let [#_"long" ret (+ x y)]
            (when-not (and (neg? (bit-xor ret x)) (neg? (bit-xor ret y))) => (Numbers'throwIntOverflow)
                ret
            )
        )
    )

    (defn #_"Number" Numbers'addP-2ll [#_"long" x, #_"long" y]
        (let [#_"long" ret (+ x y)]
            (if (and (neg? (bit-xor ret x)) (neg? (bit-xor ret y)))
                (Numbers'addP-2oo (cast Number x), (cast Number y))
                (Numbers'num-1l ret)
            )
        )
    )

    (defn #_"long" Numbers'minus-2ll [#_"long" x, #_"long" y]
        (let [#_"long" ret (- x y)]
            (when-not (and (neg? (bit-xor ret x)) (neg? (bit-xor ret (bit-not y)))) => (Numbers'throwIntOverflow)
                ret
            )
        )
    )

    (defn #_"Number" Numbers'minusP-2ll [#_"long" x, #_"long" y]
        (let [#_"long" ret (- x y)]
            (if (and (neg? (bit-xor ret x)) (neg? (bit-xor ret (bit-not y))))
                (Numbers'minusP-2oo (cast Number x), (cast Number y))
                (Numbers'num-1l ret)
            )
        )
    )

    (defn #_"long" Numbers'minus-1l [#_"long" x]
        (when-not (= x Long/MIN_VALUE) => (Numbers'throwIntOverflow)
            (- x)
        )
    )

    (defn #_"Number" Numbers'minusP-1l [#_"long" x]
        (if (= x Long/MIN_VALUE)
            (BigInt'fromBigInteger (.negate (BigInteger/valueOf x)))
            (Numbers'num-1l (- x))
        )
    )

    (defn #_"long" Numbers'inc-1l [#_"long" x] (if (= x Long/MAX_VALUE) (Numbers'throwIntOverflow) (inc x)))
    (defn #_"long" Numbers'dec-1l [#_"long" x] (if (= x Long/MIN_VALUE) (Numbers'throwIntOverflow) (dec x)))

    (defn #_"Number" Numbers'incP-1l [#_"long" x] (if (= x Long/MAX_VALUE) (.inc Numbers'BIGINT_OPS, x) (Numbers'num-1l (inc x))))
    (defn #_"Number" Numbers'decP-1l [#_"long" x] (if (= x Long/MIN_VALUE) (.dec Numbers'BIGINT_OPS, x) (Numbers'num-1l (dec x))))

    (defn #_"long" Numbers'multiply-2ll [#_"long" x, #_"long" y]
        (when-not (and (= x Long/MIN_VALUE) (neg? y)) => (Numbers'throwIntOverflow)
            (let [#_"long" ret (* x y)]
                (when (or (zero? y) (= (/ ret y) x)) => (Numbers'throwIntOverflow)
                    ret
                )
            )
        )
    )

    (defn #_"Number" Numbers'multiplyP-2ll [#_"long" x, #_"long" y]
        (when-not (and (= x Long/MIN_VALUE) (neg? y)) => (Numbers'multiplyP-2oo (cast Number x), (cast Number y))
            (let [#_"long" ret (* x y)]
                (when (or (zero? y) (= (/ ret y) x)) => (Numbers'multiplyP-2oo (cast Number x), (cast Number y))
                    (Numbers'num-1l ret)
                )
            )
        )
    )

    (defn #_"long" Numbers'quotient-2ll  [#_"long" x, #_"long" y] (/ x y))
    (defn #_"long" Numbers'remainder-2ll [#_"long" x, #_"long" y] (% x y))

    (defn #_"boolean" Numbers'equiv-2ll [#_"long" x, #_"long" y] (= x y))
    (defn #_"boolean" Numbers'lt-2ll    [#_"long" x, #_"long" y] (< x y))
    (defn #_"boolean" Numbers'lte-2ll   [#_"long" x, #_"long" y] (<= x y))
    (defn #_"boolean" Numbers'gt-2ll    [#_"long" x, #_"long" y] (> x y))
    (defn #_"boolean" Numbers'gte-2ll   [#_"long" x, #_"long" y] (>= x y))

    (defn #_"boolean" Numbers'isPos-1l  [#_"long" x] (> x 0))
    (defn #_"boolean" Numbers'isNeg-1l  [#_"long" x] (< x 0))
    (defn #_"boolean" Numbers'isZero-1l [#_"long" x] (zero? x))

    (defn #_"Number" Numbers'add-2lo [#_"long"   x, #_"Object" y] (Numbers'add-2oo (cast Object x), y))
    (defn #_"Number" Numbers'add-2ol [#_"Object" x, #_"long"   y] (Numbers'add-2oo x, (cast Object y)))
    (defn #_"double" Numbers'add-2do [#_"double" x, #_"Object" y] (Numbers'add-2dd x, (.doubleValue (cast Number y))))
    (defn #_"double" Numbers'add-2od [#_"Object" x, #_"double" y] (Numbers'add-2dd (.doubleValue (cast Number x)), y))
    (defn #_"double" Numbers'add-2dl [#_"double" x, #_"long"   y] (+ x y))
    (defn #_"double" Numbers'add-2ld [#_"long"   x, #_"double" y] (+ x y))

    (defn #_"Number" Numbers'addP-2lo [#_"long"   x, #_"Object" y] (Numbers'addP-2oo (cast Object x), y))
    (defn #_"Number" Numbers'addP-2ol [#_"Object" x, #_"long"   y] (Numbers'addP-2oo x, (cast Object y)))
    (defn #_"double" Numbers'addP-2do [#_"double" x, #_"Object" y] (Numbers'addP-2dd x, (.doubleValue (cast Number y))))
    (defn #_"double" Numbers'addP-2od [#_"Object" x, #_"double" y] (Numbers'addP-2dd (.doubleValue (cast Number x)), y))
    (defn #_"double" Numbers'addP-2dl [#_"double" x, #_"long"   y] (+ x y))
    (defn #_"double" Numbers'addP-2ld [#_"long"   x, #_"double" y] (+ x y))

    (defn #_"Number" Numbers'minus-2lo [#_"long"   x, #_"Object" y] (Numbers'minus-2oo (cast Object x), y))
    (defn #_"Number" Numbers'minus-2ol [#_"Object" x, #_"long"   y] (Numbers'minus-2oo x, (cast Object y)))
    (defn #_"double" Numbers'minus-2do [#_"double" x, #_"Object" y] (Numbers'minus-2dd x, (.doubleValue (cast Number y))))
    (defn #_"double" Numbers'minus-2od [#_"Object" x, #_"double" y] (Numbers'minus-2dd (.doubleValue (cast Number x)), y))
    (defn #_"double" Numbers'minus-2dl [#_"double" x, #_"long"   y] (- x y))
    (defn #_"double" Numbers'minus-2ld [#_"long"   x, #_"double" y] (- x y))

    (defn #_"Number" Numbers'minusP-2lo [#_"long"   x, #_"Object" y] (Numbers'minusP-2oo (cast Object x), y))
    (defn #_"Number" Numbers'minusP-2ol [#_"Object" x, #_"long"   y] (Numbers'minusP-2oo x, (cast Object y)))
    (defn #_"double" Numbers'minusP-2do [#_"double" x, #_"Object" y] (Numbers'minusP-2dd x, (.doubleValue (cast Number y))))
    (defn #_"double" Numbers'minusP-2od [#_"Object" x, #_"double" y] (Numbers'minusP-2dd (.doubleValue (cast Number x)), y))
    (defn #_"double" Numbers'minusP-2dl [#_"double" x, #_"long"   y] (- x y))
    (defn #_"double" Numbers'minusP-2ld [#_"long"   x, #_"double" y] (- x y))

    (defn #_"Number" Numbers'multiply-2lo [#_"long"   x, #_"Object" y] (Numbers'multiply-2oo (cast Object x), y))
    (defn #_"Number" Numbers'multiply-2ol [#_"Object" x, #_"long"   y] (Numbers'multiply-2oo x, (cast Object y)))
    (defn #_"double" Numbers'multiply-2do [#_"double" x, #_"Object" y] (Numbers'multiply-2dd x, (.doubleValue (cast Number y))))
    (defn #_"double" Numbers'multiply-2od [#_"Object" x, #_"double" y] (Numbers'multiply-2dd (.doubleValue (cast Number x)), y))
    (defn #_"double" Numbers'multiply-2dl [#_"double" x, #_"long"   y] (* x y))
    (defn #_"double" Numbers'multiply-2ld [#_"long"   x, #_"double" y] (* x y))

    (defn #_"Number" Numbers'multiplyP-2lo [#_"long"   x, #_"Object" y] (Numbers'multiplyP-2oo (cast Object x), y))
    (defn #_"Number" Numbers'multiplyP-2ol [#_"Object" x, #_"long"   y] (Numbers'multiplyP-2oo x, (cast Object y)))
    (defn #_"double" Numbers'multiplyP-2do [#_"double" x, #_"Object" y] (Numbers'multiplyP-2dd x, (.doubleValue (cast Number y))))
    (defn #_"double" Numbers'multiplyP-2od [#_"Object" x, #_"double" y] (Numbers'multiplyP-2dd (.doubleValue (cast Number x)), y))
    (defn #_"double" Numbers'multiplyP-2dl [#_"double" x, #_"long"   y] (* x y))
    (defn #_"double" Numbers'multiplyP-2ld [#_"long"   x, #_"double" y] (* x y))

    (defn #_"Number" Numbers'divide-2lo [#_"long"   x, #_"Object" y] (Numbers'divide-2oo (cast Object x), y))
    (defn #_"Number" Numbers'divide-2ol [#_"Object" x, #_"long"   y] (Numbers'divide-2oo x, (cast Object y)))
    (defn #_"double" Numbers'divide-2do [#_"double" x, #_"Object" y] (/ x (.doubleValue (cast Number y))))
    (defn #_"double" Numbers'divide-2od [#_"Object" x, #_"double" y] (/ (.doubleValue (cast Number x)) y))
    (defn #_"double" Numbers'divide-2dl [#_"double" x, #_"long"   y] (/ x y))
    (defn #_"double" Numbers'divide-2ld [#_"long"   x, #_"double" y] (/ x y))
    (defn #_"Number" Numbers'divide-2ll [#_"long"   x, #_"long"   y] (Numbers'divide-2oo (cast Number x), (cast Number y)))

    (defn #_"boolean" Numbers'lt-2lo [#_"long"   x, #_"Object" y] (Numbers'lt-2oo (cast Object x), y))
    (defn #_"boolean" Numbers'lt-2ol [#_"Object" x, #_"long"   y] (Numbers'lt-2oo x, (cast Object y)))
    (defn #_"boolean" Numbers'lt-2do [#_"double" x, #_"Object" y] (< x (.doubleValue (cast Number y))))
    (defn #_"boolean" Numbers'lt-2od [#_"Object" x, #_"double" y] (< (.doubleValue (cast Number x)) y))
    (defn #_"boolean" Numbers'lt-2dl [#_"double" x, #_"long"   y] (< x y))
    (defn #_"boolean" Numbers'lt-2ld [#_"long"   x, #_"double" y] (< x y))

    (defn #_"boolean" Numbers'lte-2lo [#_"long"   x, #_"Object" y] (Numbers'lte-2oo (cast Object x), y))
    (defn #_"boolean" Numbers'lte-2ol [#_"Object" x, #_"long"   y] (Numbers'lte-2oo x, (cast Object y)))
    (defn #_"boolean" Numbers'lte-2do [#_"double" x, #_"Object" y] (<= x (.doubleValue (cast Number y))))
    (defn #_"boolean" Numbers'lte-2od [#_"Object" x, #_"double" y] (<= (.doubleValue (cast Number x)) y))
    (defn #_"boolean" Numbers'lte-2dl [#_"double" x, #_"long"   y] (<= x y))
    (defn #_"boolean" Numbers'lte-2ld [#_"long"   x, #_"double" y] (<= x y))

    (defn #_"boolean" Numbers'gt-2lo [#_"long"   x, #_"Object" y] (Numbers'gt-2oo (cast Object x), y))
    (defn #_"boolean" Numbers'gt-2ol [#_"Object" x, #_"long"   y] (Numbers'gt-2oo x, (cast Object y)))
    (defn #_"boolean" Numbers'gt-2do [#_"double" x, #_"Object" y] (> x (.doubleValue (cast Number y))))
    (defn #_"boolean" Numbers'gt-2od [#_"Object" x, #_"double" y] (> (.doubleValue (cast Number x)) y))
    (defn #_"boolean" Numbers'gt-2dl [#_"double" x, #_"long"   y] (> x y))
    (defn #_"boolean" Numbers'gt-2ld [#_"long"   x, #_"double" y] (> x y))

    (defn #_"boolean" Numbers'gte-2lo [#_"long"   x, #_"Object" y] (Numbers'gte-2oo (cast Object x), y))
    (defn #_"boolean" Numbers'gte-2ol [#_"Object" x, #_"long"   y] (Numbers'gte-2oo x, (cast Object y)))
    (defn #_"boolean" Numbers'gte-2do [#_"double" x, #_"Object" y] (>= x (.doubleValue (cast Number y))))
    (defn #_"boolean" Numbers'gte-2od [#_"Object" x, #_"double" y] (>= (.doubleValue (cast Number x)) y))
    (defn #_"boolean" Numbers'gte-2dl [#_"double" x, #_"long"   y] (>= x y))
    (defn #_"boolean" Numbers'gte-2ld [#_"long"   x, #_"double" y] (>= x y))

    (defn #_"boolean" Numbers'equiv-2lo [#_"long"   x, #_"Object" y] (Numbers'equiv-2oo (cast Object x), y))
    (defn #_"boolean" Numbers'equiv-2ol [#_"Object" x, #_"long"   y] (Numbers'equiv-2oo x, (cast Object y)))
    (defn #_"boolean" Numbers'equiv-2do [#_"double" x, #_"Object" y] (= x (.doubleValue (cast Number y))))
    (defn #_"boolean" Numbers'equiv-2od [#_"Object" x, #_"double" y] (= (.doubleValue (cast Number x)) y))
    (defn #_"boolean" Numbers'equiv-2dl [#_"double" x, #_"long"   y] (= x y))
    (defn #_"boolean" Numbers'equiv-2ld [#_"long"   x, #_"double" y] (= x y))

    (defn #_"long" Numbers'max-2ll [#_"long" x, #_"long" y] (if (> x y) x y))
    (defn #_"long" Numbers'min-2ll [#_"long" x, #_"long" y] (if (< x y) x y))

    (defn #_"double" Numbers'max-2dd [#_"double" x, #_"double" y] (Math/max x, y))
    (defn #_"double" Numbers'min-2dd [#_"double" x, #_"double" y] (Math/min x, y))

    (defn #_"Object" Numbers'max-2ld [#_"long" x, #_"double" y] (cond (Double/isNaN y) y (> x y) x :else y))
    (defn #_"Object" Numbers'max-2dl [#_"double" x, #_"long" y] (cond (Double/isNaN x) x (> x y) x :else y))
    (defn #_"Object" Numbers'min-2ld [#_"long" x, #_"double" y] (cond (Double/isNaN y) y (< x y) x :else y))
    (defn #_"Object" Numbers'min-2dl [#_"double" x, #_"long" y] (cond (Double/isNaN x) x (< x y) x :else y))

    (defn #_"Object" Numbers'max-2lo [#_"long" x, #_"Object" y] (cond (Numbers'isNaN y) y (Numbers'gt-2lo x, y) x :else y))
    (defn #_"Object" Numbers'max-2ol [#_"Object" x, #_"long" y] (cond (Numbers'isNaN x) x (Numbers'gt-2ol x, y) x :else y))
    (defn #_"Object" Numbers'min-2lo [#_"long" x, #_"Object" y] (cond (Numbers'isNaN y) y (Numbers'lt-2lo x, y) x :else y))
    (defn #_"Object" Numbers'min-2ol [#_"Object" x, #_"long" y] (cond (Numbers'isNaN x) x (Numbers'lt-2ol x, y) x :else y))

    (defn #_"Object" Numbers'max-2do [#_"double" x, #_"Object" y] (cond (Double/isNaN x) x (Numbers'isNaN y) y (> x (.doubleValue (cast Number y))) x :else y))
    (defn #_"Object" Numbers'max-2od [#_"Object" x, #_"double" y] (cond (Numbers'isNaN x) x (Double/isNaN y) y (> (.doubleValue (cast Number x)) y) x :else y))
    (defn #_"Object" Numbers'min-2do [#_"double" x, #_"Object" y] (cond (Double/isNaN x) x (Numbers'isNaN y) y (< x (.doubleValue (cast Number y))) x :else y))
    (defn #_"Object" Numbers'min-2od [#_"Object" x, #_"double" y] (cond (Numbers'isNaN x) x (Double/isNaN y) y (< (.doubleValue (cast Number x)) y) x :else y))

    (defn #_"Object" Numbers'max-2oo [#_"Object" x, #_"Object" y] (cond (Numbers'isNaN x) x (Numbers'isNaN y) y (Numbers'gt-2oo x, y) x :else y))
    (defn #_"Object" Numbers'min-2oo [#_"Object" x, #_"Object" y] (cond (Numbers'isNaN x) x (Numbers'isNaN y) y (Numbers'lt-2oo x, y) x :else y))

    (defn #_"long" Numbers'unchecked_add-2ll      [#_"long" x, #_"long" y] (+ x y))
    (defn #_"long" Numbers'unchecked_minus-2ll    [#_"long" x, #_"long" y] (- x y))
    (defn #_"long" Numbers'unchecked_multiply-2ll [#_"long" x, #_"long" y] (* x y))

    (defn #_"long" Numbers'unchecked_minus-1l [#_"long" x] (- x))
    (defn #_"long" Numbers'unchecked_inc-1l   [#_"long" x] (inc x))
    (defn #_"long" Numbers'unchecked_dec-1l   [#_"long" x] (dec x))

    (defn #_"Number" Numbers'unchecked_add-2oo      [#_"Object" x, #_"Object" y] (Numbers'add-2oo      x, y))
    (defn #_"Number" Numbers'unchecked_minus-2oo    [#_"Object" x, #_"Object" y] (Numbers'minus-2oo    x, y))
    (defn #_"Number" Numbers'unchecked_multiply-2oo [#_"Object" x, #_"Object" y] (Numbers'multiply-2oo x, y))

    (defn #_"Number" Numbers'unchecked_inc-1o   [#_"Object" x] (Numbers'inc-1o   x))
    (defn #_"Number" Numbers'unchecked_dec-1o   [#_"Object" x] (Numbers'dec-1o   x))
    (defn #_"Number" Numbers'unchecked_minus-1o [#_"Object" x] (Numbers'minus-1o x))

    (defn #_"double" Numbers'unchecked_add-2dd      [#_"double" x, #_"double" y] (Numbers'add-2dd      x, y))
    (defn #_"double" Numbers'unchecked_minus-2dd    [#_"double" x, #_"double" y] (Numbers'minus-2dd    x, y))
    (defn #_"double" Numbers'unchecked_multiply-2dd [#_"double" x, #_"double" y] (Numbers'multiply-2dd x, y))

    (defn #_"double" Numbers'unchecked_inc-1d   [#_"double" x] (Numbers'inc-1d   x))
    (defn #_"double" Numbers'unchecked_dec-1d   [#_"double" x] (Numbers'dec-1d   x))
    (defn #_"double" Numbers'unchecked_minus-1d [#_"double" x] (Numbers'minus-1d x))

    (defn #_"double" Numbers'unchecked_add-2do      [#_"double" x, #_"Object" y] (Numbers'add-2do      x, y))
    (defn #_"double" Numbers'unchecked_minus-2do    [#_"double" x, #_"Object" y] (Numbers'minus-2do    x, y))
    (defn #_"double" Numbers'unchecked_multiply-2do [#_"double" x, #_"Object" y] (Numbers'multiply-2do x, y))

    (defn #_"double" Numbers'unchecked_add-2od      [#_"Object" x, #_"double" y] (Numbers'add-2od      x, y))
    (defn #_"double" Numbers'unchecked_minus-2od    [#_"Object" x, #_"double" y] (Numbers'minus-2od    x, y))
    (defn #_"double" Numbers'unchecked_multiply-2od [#_"Object" x, #_"double" y] (Numbers'multiply-2od x, y))

    (defn #_"double" Numbers'unchecked_add-2dl      [#_"double" x, #_"long" y] (Numbers'add-2dl      x, y))
    (defn #_"double" Numbers'unchecked_minus-2dl    [#_"double" x, #_"long" y] (Numbers'minus-2dl    x, y))
    (defn #_"double" Numbers'unchecked_multiply-2dl [#_"double" x, #_"long" y] (Numbers'multiply-2dl x, y))

    (defn #_"double" Numbers'unchecked_add-2ld      [#_"long" x, #_"double" y] (Numbers'add-2ld      x, y))
    (defn #_"double" Numbers'unchecked_minus-2ld    [#_"long" x, #_"double" y] (Numbers'minus-2ld    x, y))
    (defn #_"double" Numbers'unchecked_multiply-2ld [#_"long" x, #_"double" y] (Numbers'multiply-2ld x, y))

    (defn #_"Number" Numbers'unchecked_add-2lo      [#_"long" x, #_"Object" y] (Numbers'add-2lo      x, y))
    (defn #_"Number" Numbers'unchecked_minus-2lo    [#_"long" x, #_"Object" y] (Numbers'minus-2lo    x, y))
    (defn #_"Number" Numbers'unchecked_multiply-2lo [#_"long" x, #_"Object" y] (Numbers'multiply-2lo x, y))

    (defn #_"Number" Numbers'unchecked_add-2ol      [#_"Object" x, #_"long" y] (Numbers'add-2ol      x, y))
    (defn #_"Number" Numbers'unchecked_minus-2ol    [#_"Object" x, #_"long" y] (Numbers'minus-2ol    x, y))
    (defn #_"Number" Numbers'unchecked_multiply-2ol [#_"Object" x, #_"long" y] (Numbers'multiply-2ol x, y))

    (defn- #_"int" Numbers'hasheqFrom [#_"Number" x, #_"Class" xc]
        (cond
            (or (any = xc Integer Short Byte) (and (= xc BigInteger) (Numbers'lte-2ol x, Long/MAX_VALUE) (Numbers'gte-2ol x, Long/MIN_VALUE)))
                (Murmur3'hashLong (.longValue x))
            (= xc BigDecimal)
                ;; stripTrailingZeros() to make all numerically equal BigDecimal values come out the same before calling hashCode.
                ;; Special check for 0 because stripTrailingZeros() does not do anything to values equal to 0 with different scales.
                (.hashCode (if (Numbers'isZero-1o x) BigDecimal/ZERO (.stripTrailingZeros (cast BigDecimal x))))
            (and (= xc Float) (.equals x, (float -0.0)))
                0 ;; match 0.0f
            :else
                (.hashCode x)
        )
    )

    (defn #_"int" Numbers'hasheq [#_"Number" x]
        (let [#_"Class" xc (.getClass x)]
            (condp = xc
                Long
                    (Murmur3'hashLong (.longValue x))
                Double
                    (if (.equals x, -0.0)
                        0 ;; match 0.0
                        (.hashCode x)
                    )
                (Numbers'hasheqFrom x, xc)
            )
        )
    )
)
)

(java-ns cloiure.lang.ArityException

(class-ns ArityException
    (defn #_"ArityException" ArityException'new
        ([#_"int" actual, #_"String" name] (ArityException'new actual, name, nil))
        ([#_"int" actual, #_"String" name, #_"Throwable" cause]
            (merge (§ foreign IllegalArgumentException'new (str "Wrong number of args (" actual ") passed to: " name), cause)
                (hash-map
                    #_"int" :actual actual
                    #_"String" :name name
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.AFn

(class-ns AFn
    (defn #_"AFn" AFn'new []
        (hash-map)
    )

    #_foreign
    (defn #_"Object" call---AFn [#_"AFn" this]
        (.invoke this)
    )

    #_foreign
    (defn #_"void" run---AFn [#_"AFn" this]
        (.invoke this)
        nil
    )

    #_override
    (defn #_"Object" IFn'''invoke-1--AFn [#_"AFn" this]
        (.throwArity this, 0)
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--AFn [#_"AFn" this, #_"Object" arg1]
        (.throwArity this, 1)
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2]
        (.throwArity this, 2)
    )

    #_override
    (defn #_"Object" IFn'''invoke-4--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3]
        (.throwArity this, 3)
    )

    #_override
    (defn #_"Object" IFn'''invoke-5--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4]
        (.throwArity this, 4)
    )

    #_override
    (defn #_"Object" IFn'''invoke-6--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5]
        (.throwArity this, 5)
    )

    #_override
    (defn #_"Object" IFn'''invoke-7--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6]
        (.throwArity this, 6)
    )

    #_override
    (defn #_"Object" IFn'''invoke-8--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7]
        (.throwArity this, 7)
    )

    #_override
    (defn #_"Object" IFn'''invoke-9--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8]
        (.throwArity this, 8)
    )

    #_override
    (defn #_"Object" IFn'''invoke-10--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9]
        (.throwArity this, 9)
    )

    #_override
    (defn #_"Object" IFn'''invoke-11--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10]
        (.throwArity this, 10)
    )

    #_override
    (defn #_"Object" IFn'''invoke-12--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11]
        (.throwArity this, 11)
    )

    #_override
    (defn #_"Object" IFn'''invoke-13--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12]
        (.throwArity this, 12)
    )

    #_override
    (defn #_"Object" IFn'''invoke-14--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13]
        (.throwArity this, 13)
    )

    #_override
    (defn #_"Object" IFn'''invoke-15--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14]
        (.throwArity this, 14)
    )

    #_override
    (defn #_"Object" IFn'''invoke-16--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15]
        (.throwArity this, 15)
    )

    #_override
    (defn #_"Object" IFn'''invoke-17--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16]
        (.throwArity this, 16)
    )

    #_override
    (defn #_"Object" IFn'''invoke-18--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17]
        (.throwArity this, 17)
    )

    #_override
    (defn #_"Object" IFn'''invoke-19--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18]
        (.throwArity this, 18)
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-20--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19]
        (.throwArity this, 19)
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-21--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20]
        (.throwArity this, 20)
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-22--AFn [#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20 & #_"Object..." args]
        (.throwArity this, 21)
    )

    (declare AFn'applyToHelper)

    #_override
    (defn #_"Object" IFn'''applyTo--AFn [#_"AFn" this, #_"ISeq" args]
        (AFn'applyToHelper this, args)
    )

    (declare RT'boundedLength)
    (declare RT'seqToArray)

    (defn #_"Object" AFn'applyToHelper [#_"IFn" ifn, #_"ISeq" args]
        (case (RT'boundedLength args, 20)
            0
                (.invoke ifn)
            1
                (.invoke ifn, (.first args))
            2
                (.invoke ifn, (.first args),
                    (.first (.next args))
                )
            3
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            4
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            5
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            6
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            7
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            8
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            9
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            10
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            11
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            12
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            13
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            14
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            15
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            16
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            17
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            18
                (.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            19
              #_(.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            20
              #_(.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (.next args))
                )
            #_else
              #_(.invoke ifn, (.first args),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (.first (§ ass args (.next args))),
                    (RT'seqToArray (.next args))
                )
        )
    )

    (declare Compiler'demunge)

    #_override
    (defn #_"Object" AFn'''throwArity--AFn [#_"AFn" this, #_"int" n]
        (throw (ArityException'new n, (Compiler'demunge (.getSimpleName (.getClass this)))))
    )
)
)

(java-ns cloiure.lang.Symbol

(class-ns Symbol
    (defn- #_"Symbol" Symbol'new
        ([#_"String" ns, #_"String" name] (Symbol'new nil, ns, name))
        ([#_"IPersistentMap" meta, #_"String" ns, #_"String" name]
            (merge (AFn'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"String" :ns ns
                    #_"String" :name name

                    #_mutable #_"int" :_hasheq 0
                    #_mutable #_"String" :_str nil
                )
            )
        )
    )

    (defn #_"Symbol" Symbol'intern
        ([#_"String" nsname]
            (let [#_"int" i (.indexOf nsname, (int \/))]
                (if (or (= i -1) (= nsname "/"))
                    (Symbol'new nil, nsname)
                    (Symbol'new (.substring nsname, 0, i), (.substring nsname, (inc i)))
                )
            )
        )
        ([#_"String" ns, #_"String" name]
            (Symbol'new ns, name)
        )
    )

    #_foreign
    (defn #_"String" toString---Symbol [#_"Symbol" this]
        (when (nil? (:_str this))
            (§ set! (:_str this) (if (some? (:ns this)) (str (:ns this) "/" (:name this)) (:name this)))
        )
        (:_str this)
    )

    #_override
    (defn #_"String" Named'''getNamespace--Symbol [#_"Symbol" this]
        (:ns this)
    )

    #_override
    (defn #_"String" Named'''getName--Symbol [#_"Symbol" this]
        (:name this)
    )

    #_foreign
    (defn #_"boolean" equals---Symbol [#_"Symbol" this, #_"Object" o]
        (cond
            (= this o)
                true
            (instance? Symbol o)
                (let [#_"Symbol" symbol (cast Symbol o)]
                    (and (Util'equals (:ns this), (:ns symbol)) (.equals (:name this), (:name symbol)))
                )
            :else
                false
        )
    )

    #_foreign
    (defn #_"int" hashCode---Symbol [#_"Symbol" this]
        (Util'hashCombine (.hashCode (:name this)), (Util'hash (:ns this)))
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--Symbol [#_"Symbol" this]
        (when (zero? (:_hasheq this))
            (§ set! (:_hasheq this) (Util'hashCombine (Murmur3'hashUnencodedChars (:name this)), (Util'hash (:ns this))))
        )
        (:_hasheq this)
    )

    #_override
    (defn #_"Symbol" IObj'''withMeta--Symbol [#_"Symbol" this, #_"IPersistentMap" meta]
        (Symbol'new meta, (:ns this), (:name this))
    )

    #_foreign
    (defn #_"int" compareTo---Symbol [#_"Symbol" this, #_"Object" o]
        (let [#_"Symbol" s (cast Symbol o)]
            (cond
                (.equals this, o)                       0
                (and (nil? (:ns this)) (some? (:ns s))) -1
                (nil? (:ns this))                       (.compareTo (:name this), (:name s))
                (nil? (:ns s))                          1
                :else
                    (let-when [#_"int" nsc (.compareTo (:ns this), (:ns s))] (zero? nsc) => nsc
                        (.compareTo (:name this), (:name s))
                    )
            )
        )
    )

    (declare RT'get)

    #_override
    (defn #_"Object" IFn'''invoke-2--Symbol [#_"Symbol" this, #_"Object" obj]
        (RT'get obj, this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--Symbol [#_"Symbol" this, #_"Object" obj, #_"Object" notFound]
        (RT'get obj, this, notFound)
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--Symbol [#_"Symbol" this]
        (:_meta this)
    )
)
)

(java-ns cloiure.lang.Keyword

(class-ns Keyword
    (def- #_"ConcurrentHashMap<Symbol, Reference<Keyword>>" Keyword'TABLE (ConcurrentHashMap.))

    (def #_"ReferenceQueue" Keyword'RQ (ReferenceQueue.))

    (defn- #_"Keyword" Keyword'new [#_"Symbol" sym]
        (hash-map
            #_"Symbol" :sym sym
            #_"int" :hasheq (+ (.hasheq sym) 0x9e3779b9)

            #_mutable #_"String" :_str nil
        )
    )

    (defn #_"Keyword" Keyword'intern [#_"Symbol" sym]
        (let [#_"Reference<Keyword>" r (.get Keyword'TABLE, sym)
              [sym r #_"Keyword" k]
                (when (nil? r) => [sym r nil]
                    (Util'clearCache Keyword'RQ, Keyword'TABLE)
                    (let [sym
                            (when (some? (.meta sym)) => sym
                                (cast Symbol (.withMeta sym, nil))
                            )
                          k (Keyword'new sym)
                          r (.putIfAbsent Keyword'TABLE, sym, (WeakReference. #_"<Keyword>" k, Keyword'RQ))]
                        [sym r k]
                    )
                )]
            (when (some? r) => k
                (or (.get r)
                    (do ;; entry died in the interim, do over
                        (.remove Keyword'TABLE, sym, r)
                        (recur #_"Keyword'intern" sym)
                    )
                )
            )
        )
    )

    (defn #_"Keyword" Keyword'find [#_"Symbol" sym]
        (let [#_"Reference<Keyword>" ref (.get Keyword'TABLE, sym)]
            (when (some? ref)
                (.get ref)
            )
        )
    )

    (defn #_"Keyword" Keyword'find-2 [#_"String" ns, #_"String" name]
        (Keyword'find (Symbol'intern ns, name))
    )

    (defn #_"Keyword" Keyword'find-1 [#_"String" nsname]
        (Keyword'find (Symbol'intern nsname))
    )

    #_foreign
    (defn #_"int" hashCode---Keyword [#_"Keyword" this]
        (+ (.hashCode (:sym this)) 0x9e3779b9)
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--Keyword [#_"Keyword" this]
        (:hasheq this)
    )

    #_foreign
    (defn #_"String" toString---Keyword [#_"Keyword" this]
        (when (nil? (:_str this))
            (§ set! (:_str this) (str ":" (:sym this)))
        )
        (:_str this)
    )

    #_method
    (defn #_"Object" Keyword''throwArity [#_"Keyword" this]
        (throw (IllegalArgumentException. (str "Wrong number of args passed to keyword: " this)))
    )

    #_foreign
    (defn #_"Object" call---Keyword [#_"Keyword" this]
        (Keyword''throwArity this)
    )

    #_foreign
    (defn #_"void" run---Keyword [#_"Keyword" this]
        (throw (UnsupportedOperationException.))
    )

    #_override
    (defn #_"Object" IFn'''invoke-1--Keyword [#_"Keyword" this]
        (Keyword''throwArity this)
    )

    #_foreign
    (defn #_"int" compareTo---Keyword [#_"Keyword" this, #_"Object" o]
        (.compareTo (:sym this), (:sym (cast Keyword o)))
    )

    #_override
    (defn #_"String" Named'''getNamespace--Keyword [#_"Keyword" this]
        (.getNamespace (:sym this))
    )

    #_override
    (defn #_"String" Named'''getName--Keyword [#_"Keyword" this]
        (.getName (:sym this))
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--Keyword [#_"Keyword" this, #_"Object" obj]
        (if (instance? ILookup obj)
            (.valAt (cast ILookup obj), this)
            (RT'get obj, this)
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--Keyword [#_"Keyword" this, #_"Object" obj, #_"Object" notFound]
        (if (instance? ILookup obj)
            (.valAt (cast ILookup obj), this, notFound)
            (RT'get obj, this, notFound)
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-4--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-5--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-6--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-7--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-8--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-9--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-10--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-11--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-12--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-13--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-14--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-15--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-16--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-17--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-18--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''invoke-19--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18]
        (Keyword''throwArity this)
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-20--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19]
        (Keyword''throwArity this)
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-21--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20]
        (Keyword''throwArity this)
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-22--Keyword [#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20 & #_"Object..." args]
        (Keyword''throwArity this)
    )

    #_override
    (defn #_"Object" IFn'''applyTo--Keyword [#_"Keyword" this, #_"ISeq" args]
        (AFn'applyToHelper this, args)
    )
)
)

(java-ns cloiure.lang.AFunction

(class-ns AFunction
    (defn #_"AFunction" AFunction'new []
        (merge (AFn'new)
            (hash-map
                #_volatile #_"MethodImplCache" :__methodImplCache nil
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--AFunction [#_"AFunction" this]
        nil
    )

    #_override
    (defn #_"IObj" IObj'''withMeta--AFunction [#_"AFunction" this, #_"IPersistentMap" meta]
        (§ proxy RestFn()
            #_override
            (defn #_"Object" RestFn'''doInvoke-2--RestFn [#_"RestFn" this, #_"Object" args]
                (.applyTo (§ this AFunction), (cast ISeq args))
            )

            #_override
            (defn #_"IPersistentMap" IMeta'''meta--RestFn [#_"RestFn" this]
                meta
            )

            #_override
            (defn #_"IObj" IObj'''withMeta--RestFn [#_"RestFn" this, #_"IPersistentMap" meta]
                (.withMeta (§ this AFunction), meta)
            )

            #_override
            (defn #_"int" RestFn'''getRequiredArity--RestFn [#_"RestFn" this]
                0
            )
        )
    )

    #_foreign
    (defn #_"int" compare---AFunction [#_"AFunction" this, #_"Object" o1, #_"Object" o2]
        (let [#_"Object" o (.invoke this, o1, o2)]
            (if (instance? Boolean o)
                (cond (RT'booleanCast-1o o) -1 (RT'booleanCast-1o (.invoke this, o2, o1)) 1 :else 0)
                (.intValue (cast Number o))
            )
        )
    )
)
)

(java-ns cloiure.lang.RestFn

(class-ns RestFn
    (defn #_"RestFn" RestFn'new []
        (AFunction'new)
    )

    (defn #_"ISeq" RestFn'findKey [#_"Object" key, #_"ISeq" args]
        (loop-when args (some? args)
            (if (= key (.first args)) (.next args) (recur (RT'next (RT'next args))))
        )
    )

    (declare ArraySeq'create-1)

    (defn #_"ISeq" RestFn'ontoArrayPrepend [#_"Object[]" array & #_"Object..." args]
        (loop-when-recur [#_"ISeq" s (ArraySeq'create-1 array) #_"int" i (dec (alength args))] (<= 0 i) [(RT'cons (aget args i), s) (dec i)] => s)
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-2--RestFn [#_"RestFn" this, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-3--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-4--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-5--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-6--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-7--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-8--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-9--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-10--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-11--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-12--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-13--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-14--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-15--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-16--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-17--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-18--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-19--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-20--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" args]
        nil
    )

    #_override
  #_(defn #_"Object" RestFn'''doInvoke-21--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" args]
        nil
    )

    #_override
  #_(defn #_"Object" RestFn'''doInvoke-22--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20, #_"Object" args]
        nil
    )

    #_override
    (defn #_"Object" IFn'''applyTo--RestFn [#_"RestFn" this, #_"ISeq" args]
        (when (< (.getRequiredArity this) (RT'boundedLength args, (.getRequiredArity this))) => (AFn'applyToHelper this, args)
            (case (.getRequiredArity this)
                0
                    (.doInvoke this, args)
                1
                    (.doInvoke this, (.first args),
                        (.next args)
                    )
                2
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                3
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                4
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                5
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                6
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                7
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                8
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                9
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                10
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                11
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                12
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                13
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                14
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                15
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                16
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                17
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                18
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                19
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                20
                    (.doInvoke this, (.first args),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.first (§ ass args (.next args))),
                        (.next args)
                    )
                (.throwArity this, -1)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-1--RestFn [#_"RestFn" this]
        (case (.getRequiredArity this)
            0
                (.doInvoke this, nil)
            (do
                (.throwArity this, 0)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--RestFn [#_"RestFn" this, #_"Object" arg1]
        (case (.getRequiredArity this)
            0
                (.doInvoke this, (ArraySeq'create-1 arg1))
            1
                (.doInvoke this, arg1, nil)
            (do
                (.throwArity this, 1)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2))
            2
                (.doInvoke this, arg1, arg2, nil)
            (do
                (.throwArity this, 2)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-4--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3))
            3
                (.doInvoke this, arg1, arg2, arg3, nil)
            (do
                (.throwArity this, 3)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-5--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4, nil)
            (do
                (.throwArity this, 4)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-6--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, nil)
            (do
                (.throwArity this, 5)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-7--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, nil)
            (do
                (.throwArity this, 6)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-8--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, nil)
            (do
                (.throwArity this, 7)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-9--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, nil)
            (do
                (.throwArity this, 8)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-10--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, nil)
            (do
                (.throwArity this, 9)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-11--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, nil)
            (do
                (.throwArity this, 10)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-12--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, nil)
            (do
                (.throwArity this, 11)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-13--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, nil)
            (do
                (.throwArity this, 12)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-14--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12, arg13))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12, arg13))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12, arg13))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12, arg13))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12, arg13))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12, arg13))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (ArraySeq'create-1 arg13))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, nil)
            (do
                (.throwArity this, 13)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-15--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12, arg13, arg14))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12, arg13, arg14))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12, arg13, arg14))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12, arg13, arg14))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12, arg13, arg14))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (ArraySeq'create-1 arg13, arg14))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13,
                    (ArraySeq'create-1 arg14))
            14
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, nil)
            (do
                (.throwArity this, 14)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-16--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12, arg13, arg14, arg15))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12, arg13, arg14, arg15))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12, arg13, arg14, arg15))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12, arg13, arg14, arg15))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (ArraySeq'create-1 arg13, arg14, arg15))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13,
                    (ArraySeq'create-1 arg14, arg15))
            14
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
                    (ArraySeq'create-1 arg15))
            15
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, nil)
            (do
                (.throwArity this, 15)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-17--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12, arg13, arg14, arg15, arg16))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12, arg13, arg14, arg15, arg16))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12, arg13, arg14, arg15, arg16))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (ArraySeq'create-1 arg13, arg14, arg15, arg16))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13,
                    (ArraySeq'create-1 arg14, arg15, arg16))
            14
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
                    (ArraySeq'create-1 arg15, arg16))
            15
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                    (ArraySeq'create-1 arg16))
            16
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, nil)
            (do
                (.throwArity this, 16)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-18--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12, arg13, arg14, arg15, arg16, arg17))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12, arg13, arg14, arg15, arg16, arg17))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (ArraySeq'create-1 arg13, arg14, arg15, arg16, arg17))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13,
                    (ArraySeq'create-1 arg14, arg15, arg16, arg17))
            14
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
                    (ArraySeq'create-1 arg15, arg16, arg17))
            15
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                    (ArraySeq'create-1 arg16, arg17))
            16
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16,
                    (ArraySeq'create-1 arg17))
            17
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, nil)
            (do
                (.throwArity this, 17)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-19--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12, arg13, arg14, arg15, arg16, arg17, arg18))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (ArraySeq'create-1 arg13, arg14, arg15, arg16, arg17, arg18))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13,
                    (ArraySeq'create-1 arg14, arg15, arg16, arg17, arg18))
            14
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
                    (ArraySeq'create-1 arg15, arg16, arg17, arg18))
            15
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                    (ArraySeq'create-1 arg16, arg17, arg18))
            16
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16,
                    (ArraySeq'create-1 arg17, arg18))
            17
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17,
                    (ArraySeq'create-1 arg18))
            18
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, nil)
            (do
                (.throwArity this, 18)
            )
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-20--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (ArraySeq'create-1 arg13, arg14, arg15, arg16, arg17, arg18, arg19))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13,
                    (ArraySeq'create-1 arg14, arg15, arg16, arg17, arg18, arg19))
            14
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
                    (ArraySeq'create-1 arg15, arg16, arg17, arg18, arg19))
            15
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                    (ArraySeq'create-1 arg16, arg17, arg18, arg19))
            16
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16,
                    (ArraySeq'create-1 arg17, arg18, arg19))
            17
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17,
                    (ArraySeq'create-1 arg18, arg19))
            18
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18,
                    (ArraySeq'create-1 arg19))
            19
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, nil)
            (do
                (.throwArity this, 19)
            )
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-21--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (ArraySeq'create-1 arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            1
                (.doInvoke this, arg1,
                    (ArraySeq'create-1 arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            2
                (.doInvoke this, arg1, arg2,
                    (ArraySeq'create-1 arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (ArraySeq'create-1 arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (ArraySeq'create-1 arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (ArraySeq'create-1 arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (ArraySeq'create-1 arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (ArraySeq'create-1 arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (ArraySeq'create-1 arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (ArraySeq'create-1 arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (ArraySeq'create-1 arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (ArraySeq'create-1 arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (ArraySeq'create-1 arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13,
                    (ArraySeq'create-1 arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            14
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
                    (ArraySeq'create-1 arg15, arg16, arg17, arg18, arg19, arg20))
            15
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                    (ArraySeq'create-1 arg16, arg17, arg18, arg19, arg20))
            16
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16,
                    (ArraySeq'create-1 arg17, arg18, arg19, arg20))
            17
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17,
                    (ArraySeq'create-1 arg18, arg19, arg20))
            18
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18,
                    (ArraySeq'create-1 arg19, arg20))
            19
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19,
                    (ArraySeq'create-1 arg20))
            20
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20, nil)
            (do
                (.throwArity this, 20)
            )
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-22--RestFn [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20 & #_"Object..." args]
        (case (.getRequiredArity this)
            0
                (.doInvoke this,
                    (RestFn'ontoArrayPrepend args, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            1
                (.doInvoke this, arg1,
                    (RestFn'ontoArrayPrepend args, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            2
                (.doInvoke this, arg1, arg2,
                    (RestFn'ontoArrayPrepend args, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            3
                (.doInvoke this, arg1, arg2, arg3,
                    (RestFn'ontoArrayPrepend args, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            4
                (.doInvoke this, arg1, arg2, arg3, arg4,
                    (RestFn'ontoArrayPrepend args, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            5
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5,
                    (RestFn'ontoArrayPrepend args, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            6
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                    (RestFn'ontoArrayPrepend args, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            7
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                    (RestFn'ontoArrayPrepend args, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            8
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                    (RestFn'ontoArrayPrepend args, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            9
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                    (RestFn'ontoArrayPrepend args, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            10
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
                    (RestFn'ontoArrayPrepend args, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            11
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
                    (RestFn'ontoArrayPrepend args, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            12
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
                    (RestFn'ontoArrayPrepend args, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            13
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13,
                    (RestFn'ontoArrayPrepend args, arg14, arg15, arg16, arg17, arg18, arg19, arg20))
            14
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
                    (RestFn'ontoArrayPrepend args, arg15, arg16, arg17, arg18, arg19, arg20))
            15
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                    (RestFn'ontoArrayPrepend args, arg16, arg17, arg18, arg19, arg20))
            16
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16,
                    (RestFn'ontoArrayPrepend args, arg17, arg18, arg19, arg20))
            17
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17,
                    (RestFn'ontoArrayPrepend args, arg18, arg19, arg20))
            18
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18,
                    (RestFn'ontoArrayPrepend args, arg19, arg20))
            19
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19,
                    (RestFn'ontoArrayPrepend args, arg20))
            20
                (.doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20,
                    (ArraySeq'create-1 args))
            (do
                (.throwArity this, 21)
            )
        )
    )
)
)

(java-ns cloiure.lang.ASeq

(class-ns ASeq
    (defn #_"ASeq" ASeq'new [#_"IPersistentMap" meta]
        (hash-map
            #_"IPersistentMap" :_meta meta

            #_mutable #_"int" :_hash 0
            #_mutable #_"int" :_hasheq 0
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--ASeq [#_"ASeq" this]
        (:_meta this)
    )

    #_foreign
    (defn #_"String" toString---ASeq [#_"ASeq" this]
        (RT'printString this)
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--ASeq [#_"ASeq" this]
        PersistentList'EMPTY
    )

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--ASeq [#_"ASeq" this, #_"Object" obj]
        (and (or (instance? Sequential obj) (instance? List obj))
            (let [#_"ISeq" ms (RT'seq obj)]
                (loop-when [#_"ISeq" s (.seq this) ms ms] (some? s) => (nil? ms)
                    (and (some? ms) (Util'equiv-2oo (.first s), (.first ms)) (recur (.next s) (.next ms)))
                )
            )
        )
    )

    #_foreign
    (defn #_"boolean" equals---ASeq [#_"ASeq" this, #_"Object" obj]
        (or (= this obj)
            (and (or (instance? Sequential obj) (instance? List obj))
                (let [#_"ISeq" ms (RT'seq obj)]
                    (loop-when [#_"ISeq" s (.seq this) ms ms] (some? s) => (nil? ms)
                        (and (some? ms) (Util'equals (.first s), (.first ms)) (recur (.next s) (.next ms)))
                    )
                )
            )
        )
    )

    #_foreign
    (defn #_"int" hashCode---ASeq [#_"ASeq" this]
        (let-when [#_"int" hash (:_hash this)] (zero? hash) => hash
            (loop-when [hash 1 #_"ISeq" s (.seq this)] (some? s) => (§ set! (:_hash this) hash)
                (recur (+ (* 31 hash) (if (some? (.first s)) (.hashCode (.first s)) 0)) (.next s))
            )
        )
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--ASeq [#_"ASeq" this]
        (let-when [#_"int" cached (:_hasheq this)] (zero? cached) => cached
            (§ set! (:_hasheq this) (Murmur3'hashOrdered this))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ASeq [#_"ASeq" this]
        (loop-when [#_"ISeq" s (.next this) #_"int" i 1] (some? s) => i
            (if (instance? Counted s) (+ i (.count s)) (recur (.next s) (inc i)))
        )
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--ASeq [#_"ASeq" this]
        this
    )

    (declare Cons'new)

    #_override
    (defn #_"ISeq" ISeq'''cons--ASeq [#_"ASeq" this, #_"Object" o]
        (Cons'new o, this)
    )

    #_override
    (defn #_"ISeq" ISeq'''more--ASeq [#_"ASeq" this]
        (or (.next this) PersistentList'EMPTY)
    )

    #_foreign
    (defn #_"Object[]" toArray---ASeq [#_"ASeq" this]
        (RT'seqToArray (.seq this))
    )

    #_foreign
    (defn #_"Object[]" toArray---ASeq [#_"ASeq" this, #_"Object[]" a]
        (RT'seqToPassedArray (.seq this), a)
    )

    #_foreign
    (defn #_"int" size---ASeq [#_"ASeq" this]
        (.count this)
    )

    #_foreign
    (defn #_"boolean" contains---ASeq [#_"ASeq" this, #_"Object" o]
        (loop-when [#_"ISeq" s (.seq this)] (some? s) => false
            (or (Util'equiv-2oo (.first s), o) (recur (.next s)))
        )
    )

    (declare SeqIterator'new)

    #_foreign
    (defn #_"Iterator" iterator---ASeq [#_"ASeq" this]
        (SeqIterator'new this)
    )

    #_foreign
    (defn #_"Object" get---ASeq [#_"ASeq" this, #_"int" index]
        (RT'nth this, index)
    )
)
)

(java-ns cloiure.lang.LazySeq

(class-ns LazySeq
    (defn- #_"LazySeq" LazySeq'init [#_"IPersistentMap" meta, #_"ISeq" s, #_"IFn" fn]
        (hash-map
            #_"IPersistentMap" :_meta meta
            #_mutable #_"ISeq" :s s
            #_mutable #_"IFn" :fn fn

            #_mutable #_"Object" :sv nil
        )
    )

    (defn- #_"LazySeq" LazySeq'new
        ([#_"IPersistentMap" meta, #_"ISeq" s] (LazySeq'init meta, s, nil))
        ([#_"IFn" fn]                          (LazySeq'init nil, nil, fn))
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--LazySeq [#_"LazySeq" this]
        (:_meta this)
    )

    #_override
    (defn #_"LazySeq" IObj'''withMeta--LazySeq [#_"LazySeq" this, #_"IPersistentMap" meta]
        (LazySeq'new meta, (.seq this))
    )

    #_method
    (defn #_"Object" LazySeq''sval [#_"LazySeq" this]
        (§ sync this
            (when (some? (:fn this))
                (§ set! (:sv this) (.invoke (:fn this)))
                (§ set! (:fn this) nil)
            )
            (or (:sv this) (:s this))
        )
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--LazySeq [#_"LazySeq" this]
        (§ sync this
            (LazySeq''sval this)
            (when (some? (:sv this))
                (let [#_"Object" ls (:sv this) _ (§ set! (:sv this) nil)
                      ls (loop-when-recur ls (instance? LazySeq ls) (LazySeq''sval (cast LazySeq ls)) => ls)]
                    (§ set! (:s this) (RT'seq ls))
                )
            )
            (:s this)
        )
    )

    #_override
    (defn #_"int" Counted'''count--LazySeq [#_"LazySeq" this]
        (loop-when-recur [#_"int" c 0 #_"ISeq" s (.seq this)] (some? s) [(inc c) (.next s)] => c)
    )

    #_override
    (defn #_"Object" ISeq'''first--LazySeq [#_"LazySeq" this]
        (.seq this)
        (when (some? (:s this))
            (.first (:s this))
        )
    )

    #_override
    (defn #_"ISeq" ISeq'''next--LazySeq [#_"LazySeq" this]
        (.seq this)
        (when (some? (:s this))
            (.next (:s this))
        )
    )

    #_override
    (defn #_"ISeq" ISeq'''more--LazySeq [#_"LazySeq" this]
        (.seq this)
        (if (some? (:s this)) (.more (:s this)) PersistentList'EMPTY)
    )

    #_override
    (defn #_"ISeq" ISeq'''cons--LazySeq [#_"LazySeq" this, #_"Object" o]
        (RT'cons o, (.seq this))
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--LazySeq [#_"LazySeq" this]
        PersistentList'EMPTY
    )

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--LazySeq [#_"LazySeq" this, #_"Object" o]
        (let [#_"ISeq" s (.seq this)]
            (if (some? s)
                (.equiv s, o)
                (and (or (instance? Sequential o) (instance? List o)) (nil? (RT'seq o)))
            )
        )
    )

    #_foreign
    (defn #_"int" hashCode---LazySeq [#_"LazySeq" this]
        (let [#_"ISeq" s (.seq this)]
            (if (some? s) (Util'hash s) 1)
        )
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--LazySeq [#_"LazySeq" this]
        (Murmur3'hashOrdered this)
    )

    #_foreign
    (defn #_"boolean" equals---LazySeq [#_"LazySeq" this, #_"Object" o]
        (let [#_"ISeq" s (.seq this)]
            (if (some? s)
                (.equals s, o)
                (and (or (instance? Sequential o) (instance? List o)) (nil? (RT'seq o)))
            )
        )
    )

    #_foreign
    (defn #_"Object[]" toArray---LazySeq [#_"LazySeq" this]
        (RT'seqToArray (.seq this))
    )

    #_foreign
    (defn #_"Object[]" toArray---LazySeq [#_"LazySeq" this, #_"Object[]" a]
        (RT'seqToPassedArray (.seq this), a)
    )

    #_foreign
    (defn #_"int" size---LazySeq [#_"LazySeq" this]
        (.count this)
    )

    #_foreign
    (defn #_"boolean" contains---LazySeq [#_"LazySeq" this, #_"Object" o]
        (loop-when [#_"ISeq" s (.seq this)] (some? s) => false
            (or (Util'equiv-2oo (.first s), o) (recur (.next s)))
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---LazySeq [#_"LazySeq" this]
        (SeqIterator'new this)
    )

    #_foreign
    (defn #_"Object" get---LazySeq [#_"LazySeq" this, #_"int" index]
        (RT'nth this, index)
    )

    #_override
    (defn #_"boolean" IPending'''isRealized--LazySeq [#_"LazySeq" this]
        (§ sync this
            (nil? (:fn this))
        )
    )
)
)

(java-ns cloiure.lang.APersistentMap

(class-ns KeySeq
    (defn- #_"KeySeq" KeySeq'new
        ([#_"ISeq" seq, #_"Iterable" iterable] (KeySeq'new nil, seq, iterable))
        ([#_"IPersistentMap" meta, #_"ISeq" seq, #_"Iterable" iterable]
            (merge (ASeq'new meta)
                (hash-map
                    #_"ISeq" :seq seq
                    #_"Iterable" :iterable iterable
                )
            )
        )
    )

    (defn #_"KeySeq" KeySeq'create [#_"ISeq" seq]
        (when (some? seq)
            (KeySeq'new seq, nil)
        )
    )

    (defn #_"KeySeq" KeySeq'createFromMap [#_"IPersistentMap" map]
        (when (some? map)
            (when-let [#_"ISeq" seq (.seq map)]
                (KeySeq'new seq, map)
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--KeySeq [#_"KeySeq" this]
        (.getKey (cast Map$Entry (.first (:seq this))))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--KeySeq [#_"KeySeq" this]
        (KeySeq'create this, (.next (:seq this)))
    )

    #_override
    (defn #_"KeySeq" IObj'''withMeta--KeySeq [#_"KeySeq" this, #_"IPersistentMap" meta]
        (KeySeq'new meta, (:seq this), (:iterable this))
    )

    #_foreign
    (defn #_"Iterator" iterator---KeySeq [#_"KeySeq" this]
        (cond
            (nil? (:iterable this))
                (.iterator (§ super ))
            (instance? IMapIterable (:iterable this))
                (.keyIterator (cast IMapIterable (:iterable this)))
            :else
                (let [#_"Iterator" it (.iterator (:iterable this))]
                    (reify Iterator
                        #_foreign
                        (#_"boolean" hasNext [#_"Iterator" _self]
                            (.hasNext it)
                        )

                        #_foreign
                        (#_"Object" next [#_"Iterator" _self]
                            (.getKey (cast Map$Entry (.next it)))
                        )
                    )
                )
        )
    )
)

(class-ns ValSeq
    (defn- #_"ValSeq" ValSeq'new
        ([#_"ISeq" seq, #_"Iterable" iterable] (ValSeq'new nil, seq, iterable))
        ([#_"IPersistentMap" meta, #_"ISeq" seq, #_"Iterable" iterable]
            (merge (ASeq'new meta)
                (hash-map
                    #_"ISeq" :seq seq
                    #_"Iterable" :iterable iterable
                )
            )
        )
    )

    (defn #_"ValSeq" ValSeq'create [#_"ISeq" seq]
        (when (some? seq)
            (ValSeq'new seq, nil)
        )
    )

    (defn #_"ValSeq" ValSeq'createFromMap [#_"IPersistentMap" map]
        (when (some? map)
            (when-let [#_"ISeq" seq (.seq map)]
                (ValSeq'new seq, map)
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ValSeq [#_"ValSeq" this]
        (.getValue (cast Map$Entry (.first (:seq this))))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ValSeq [#_"ValSeq" this]
        (ValSeq'create this, (.next (:seq this)))
    )

    #_override
    (defn #_"ValSeq" IObj'''withMeta--ValSeq [#_"ValSeq" this, #_"IPersistentMap" meta]
        (ValSeq'new meta, (:seq this), (:iterable this))
    )

    #_foreign
    (defn #_"Iterator" iterator---ValSeq [#_"ValSeq" this]
        (cond
            (nil? (:iterable this))
                (.iterator (§ super ))
            (instance? IMapIterable (:iterable this))
                (.valIterator (cast IMapIterable (:iterable this)))
            :else
                (let [#_"Iterator" it (.iterator (:iterable this))]
                    (reify Iterator
                        #_foreign
                        (#_"boolean" hasNext [#_"Iterator" _self]
                            (.hasNext it)
                        )

                        #_foreign
                        (#_"Object" next [#_"Iterator" _self]
                            (.getValue (cast Map$Entry (.next it)))
                        )
                    )
                )
        )
    )
)

(class-ns APersistentMap
    (defn #_"APersistentMap" APersistentMap'new []
        (merge (AFn'new)
            (hash-map
                #_mutable #_"int" :_hash 0
                #_mutable #_"int" :_hasheq 0
            )
        )
    )

    #_foreign
    (defn #_"String" toString---APersistentMap [#_"APersistentMap" this]
        (RT'printString this)
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''cons--APersistentMap [#_"APersistentMap" this, #_"Object" o]
        (cond
            (instance? Map$Entry o)
                (let [#_"Map$Entry" e (cast Map$Entry o)]
                    (.assoc this, (.getKey e), (.getValue e))
                )
            (instance? IPersistentVector o)
                (let [#_"IPersistentVector" v (cast IPersistentVector o)]
                    (when (= (.count v) 2) => (throw (IllegalArgumentException. "Vector arg to map conj must be a pair"))
                        (.assoc this, (.nth v, 0), (.nth v, 1))
                    )
                )
            :else
                (loop-when [#_"IPersistentMap" m this #_"ISeq" s (RT'seq o)] (some? s) => m
                    (let [#_"Map$Entry" e (cast Map$Entry (.first s))]
                        (recur (.assoc m, (.getKey e), (.getValue e)) (.next s))
                    )
                )
        )
    )

    (defn #_"boolean" APersistentMap'mapEquals [#_"IPersistentMap" m1, #_"Object" obj]
        (cond
            (= m1 obj)
                true
            (not (instance? Map obj))
                false
            :else
                (let-when [#_"Map" m (cast Map obj)] (= (.size m) (.count m1)) => false
                    (loop-when [#_"ISeq" s (.seq m1)] (some? s) => true
                        (let [#_"Map$Entry" e (cast Map$Entry (.first s)) #_"Object" k (.getKey e)]
                            (and (.containsKey m, k) (Util'equals (.getValue e), (.get m, k))
                                (recur (.next s))
                            )
                        )
                    )
                )
        )
    )

    #_foreign
    (defn #_"boolean" equals---APersistentMap [#_"APersistentMap" this, #_"Object" obj]
        (APersistentMap'mapEquals this, obj)
    )

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--APersistentMap [#_"APersistentMap" this, #_"Object" obj]
        (cond
            (not (instance? Map obj))
                false
            (and (instance? IPersistentMap obj) (not (instance? MapEquivalence obj)))
                false
            :else
                (let-when [#_"Map" m (cast Map obj)] (= (.size m) (.size this)) => false
                    (loop-when [#_"ISeq" s (.seq this)] (some? s) => true
                        (let [#_"Map$Entry" e (cast Map$Entry (.first s)) #_"Object" k (.getKey e)]
                            (and (.containsKey m, k) (Util'equiv-2oo (.getValue e), (.get m, k))
                                (recur (.next s))
                            )
                        )
                    )
                )
        )
    )

    (defn #_"int" APersistentMap'mapHash [#_"IPersistentMap" m]
        (loop-when [#_"int" hash 0 #_"ISeq" s (.seq m)] (some? s) => hash
            (let [#_"Map$Entry" e (cast Map$Entry (.first s)) #_"Object" k (.getKey e) #_"Object" v (.getValue e)]
                (recur (+ hash (bit-xor (if (some? k) (.hashCode k) 0) (if (some? v) (.hashCode v) 0))) (.next s))
            )
        )
    )

    #_foreign
    (defn #_"int" hashCode---APersistentMap [#_"APersistentMap" this]
        (let-when [#_"int" cached (:_hash this)] (zero? cached) => cached
            (§ set! (:_hash this) (APersistentMap'mapHash this))
        )
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--APersistentMap [#_"APersistentMap" this]
        (let-when [#_"int" cached (:_hasheq this)] (zero? cached) => cached
            (§ set! (:_hasheq this) (Murmur3'hashUnordered this))
        )
    )

    (defn #_"int" APersistentMap'mapHasheq [#_"IPersistentMap" m]
        (Murmur3'hashUnordered m)
    )

    (declare MapEntry'create)

    (defn #_"Object" APersistentMap'MAKE_ENTRY [#_"Object" key, #_"Object" val]
        (MapEntry'create key, val)
    )

    (defn #_"Object" APersistentMap'MAKE_KEY [#_"Object" key, #_"Object" val]
        key
    )

    (defn #_"Object" APersistentMap'MAKE_VAL [#_"Object" key, #_"Object" val]
        val
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--APersistentMap [#_"APersistentMap" this, #_"Object" arg1]
        (.valAt this, arg1)
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--APersistentMap [#_"APersistentMap" this, #_"Object" arg1, #_"Object" notFound]
        (.valAt this, arg1, notFound)
    )

    #_foreign
    (defn #_"Set" entrySet---APersistentMap [#_"APersistentMap" this!]
        (proxy [AbstractSet] []
            #_foreign
            (#_"Iterator" iterator [#_"AbstractSet" #_this]
                (.iterator this!)
            )

            #_foreign
            (#_"int" size [#_"AbstractSet" #_this]
                (.count this!)
            )

            #_foreign
            (#_"int" hashCode [#_"AbstractSet" #_this]
                (.hashCode this!)
            )

            #_foreign
            (#_"boolean" contains [#_"AbstractSet" #_this, #_"Object" o]
                (and (instance? Map$Entry o)
                    (let [#_"Map$Entry" e (cast Map$Entry o) #_"Map$Entry" found (.entryAt this!, (.getKey e))]
                        (and (some? found) (Util'equals (.getValue found), (.getValue e)))
                    )
                )
            )
        )
    )

    #_foreign
    (defn #_"Object" get---APersistentMap [#_"APersistentMap" this, #_"Object" key]
        (.valAt this, key)
    )

    #_method
    (defn #_"Set" APersistentMap''keySet [#_"APersistentMap" this!]
        (proxy [AbstractSet] []
            #_foreign
            (#_"Iterator" iterator [#_"AbstractSet" #_this]
                (let [#_"Iterator" it (.iterator this!)]
                    (reify Iterator
                        #_foreign
                        (#_"boolean" hasNext [#_"Iterator" _self]
                            (.hasNext it)
                        )

                        #_foreign
                        (#_"Object" next [#_"Iterator" _self]
                            (.getKey (cast Map$Entry (.next it)))
                        )
                    )
                )
            )

            #_foreign
            (#_"int" size [#_"AbstractSet" #_this]
                (.count this!)
            )

            #_foreign
            (#_"boolean" contains [#_"AbstractSet" #_this, #_"Object" o]
                (.containsKey this!, o)
            )
        )
    )

    #_foreign
    (defn #_"int" size---APersistentMap [#_"APersistentMap" this]
        (.count this)
    )

    #_method
    (defn #_"Collection" APersistentMap''values [#_"APersistentMap" this!]
        (proxy [AbstractCollection] []
            #_foreign
            (#_"Iterator" iterator [#_"AbstractCollection" #_this]
                (let [#_"Iterator" it (.iterator this!)]
                    (reify Iterator
                        #_foreign
                        (#_"boolean" hasNext [#_"Iterator" _self]
                            (.hasNext it)
                        )

                        #_foreign
                        (#_"Object" next [#_"Iterator" _self]
                            (.getValue (cast Map$Entry (.next it)))
                        )
                    )
                )
            )

            #_foreign
            (#_"int" size [#_"AbstractCollection" #_this]
                (.count this!)
            )
        )
    )

    #_method
    (defn #_"boolean" APersistentMap''containsValue [#_"APersistentMap" this, #_"Object" value]
        (.contains (APersistentMap''values this), value)
    )
)
)

(java-ns cloiure.lang.APersistentSet

(class-ns APersistentSet
    (defn #_"APersistentSet" APersistentSet'new [#_"IPersistentMap" impl]
        (merge (AFn'new)
            (hash-map
                #_"IPersistentMap" :impl impl

                #_mutable #_"int" :_hash 0
                #_mutable #_"int" :_hasheq 0
            )
        )
    )

    #_foreign
    (defn #_"String" toString---APersistentSet [#_"APersistentSet" this]
        (RT'printString this)
    )

    #_override
    (defn #_"boolean" IPersistentSet'''contains--APersistentSet [#_"APersistentSet" this, #_"Object" key]
        (.containsKey (:impl this), key)
    )

    #_override
    (defn #_"Object" IPersistentSet'''get--APersistentSet [#_"APersistentSet" this, #_"Object" key]
        (.valAt (:impl this), key)
    )

    #_override
    (defn #_"int" Counted'''count--APersistentSet [#_"APersistentSet" this]
        (.count (:impl this))
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--APersistentSet [#_"APersistentSet" this]
        (RT'keys (:impl this))
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--APersistentSet [#_"APersistentSet" this, #_"Object" arg1]
        (.get this, arg1)
    )

    (defn #_"boolean" APersistentSet'setEquals [#_"IPersistentSet" s1, #_"Object" obj]
        (or (= s1 obj)
            (and (instance? Set obj)
                (let-when [#_"Set" m (cast Set obj)] (= (.size m) (.count s1)) => false
                    (loop-when [#_"Iterator" it (.iterator m)] (.hasNext it) => true
                        (and (.contains s1, (.next it)) (recur it))
                    )
                )
            )
        )
    )

    #_foreign
    (defn #_"boolean" equals---APersistentSet [#_"APersistentSet" this, #_"Object" obj]
        (APersistentSet'setEquals this, obj)
    )

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--APersistentSet [#_"APersistentSet" this, #_"Object" obj]
        (and (instance? Set obj)
            (let-when [#_"Set" m (cast Set obj)] (= (.size m) (.size this)) => false
                (loop-when [#_"Iterator" it (.iterator m)] (.hasNext it) => true
                    (and (.contains this, (.next it)) (recur it))
                )
            )
        )
    )

    #_foreign
    (defn #_"int" hashCode---APersistentSet [#_"APersistentSet" this]
        (let-when [#_"int" hash (:_hash this)] (zero? hash) => hash
            (loop-when [hash 0 #_"ISeq" s (.seq this)] (some? s) => (§ set! (:_hash this) hash)
                (recur (+ hash (Util'hash (.first s))) (.next s))
            )
        )
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--APersistentSet [#_"APersistentSet" this]
        (let-when [#_"int" cached (:_hasheq this)] (zero? cached) => cached
            (§ set! (:_hasheq this) (Murmur3'hashUnordered this))
        )
    )

    #_foreign
    (defn #_"Object[]" toArray---APersistentSet [#_"APersistentSet" this]
        (RT'seqToArray (.seq this))
    )

    #_foreign
    (defn #_"Object[]" toArray---APersistentSet [#_"APersistentSet" this, #_"Object[]" a]
        (RT'seqToPassedArray (.seq this), a)
    )

    #_foreign
    (defn #_"int" size---APersistentSet [#_"APersistentSet" this]
        (.count this)
    )

    #_foreign
    (defn #_"Iterator" iterator---APersistentSet [#_"APersistentSet" this]
        (if (instance? IMapIterable (:impl this))
            (.keyIterator (cast IMapIterable (:impl this)))
            (let [#_"Iterator" it (.iterator (:impl this))]
                (reify Iterator
                    #_foreign
                    (#_"boolean" hasNext [#_"Iterator" _self]
                        (.hasNext it)
                    )

                    #_foreign
                    (#_"Object" next [#_"Iterator" _self]
                        (.key (cast IMapEntry (.next it)))
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.APersistentVector

(class-ns VSeq
    (defn #_"VSeq" VSeq'new
        ([#_"IPersistentVector" v, #_"int" i] (VSeq'new nil, v, i))
        ([#_"IPersistentMap" meta, #_"IPersistentVector" v, #_"int" i]
            (merge (ASeq'new meta)
                (hash-map
                    #_"IPersistentVector" :v v
                    #_"int" :i i
                )
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--VSeq [#_"VSeq" this]
        (.nth (:v this), (:i this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--VSeq [#_"VSeq" this]
        (when (< (inc (:i this)) (.count (:v this)))
            (VSeq'new (:v this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" IndexedSeq'''index--VSeq [#_"VSeq" this]
        (:i this)
    )

    #_override
    (defn #_"int" Counted'''count--VSeq [#_"VSeq" this]
        (- (.count (:v this)) (:i this))
    )

    #_override
    (defn #_"VSeq" IObj'''withMeta--VSeq [#_"VSeq" this, #_"IPersistentMap" meta]
        (VSeq'new meta, (:v this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--VSeq [#_"VSeq" this, #_"IFn" f]
        (let [#_"IPersistentVector" v (:v this) #_"int" i (:i this) #_"int" n (.count v)]
            (loop-when [#_"Object" r (.nth v, i) i (inc i)] (< i n) => r
                (let-when [r (.invoke f, r, (.nth v, i))] (RT'isReduced r) => (recur r (inc i))
                    (.deref (cast IDeref r))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--VSeq [#_"VSeq" this, #_"IFn" f, #_"Object" r]
        (let [#_"IPersistentVector" v (:v this) #_"int" i (:i this) #_"int" n (.count v)]
            (loop-when [r (.invoke f, r, (.nth v, i)) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (when (RT'isReduced r) => (recur (.invoke f, r, (.nth v, i)) (inc i))
                    (.deref (cast IDeref r))
                )
            )
        )
    )
)

(class-ns RSeq
    (defn #_"RSeq" RSeq'new
        ([#_"IPersistentVector" v, #_"int" i] (RSeq'new nil, v, i))
        ([#_"IPersistentMap" meta, #_"IPersistentVector" v, #_"int" i]
            (merge (ASeq'new meta)
                (hash-map
                    #_"IPersistentVector" :v v
                    #_"int" :i i
                )
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--RSeq [#_"RSeq" this]
        (.nth (:v this), (:i this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--RSeq [#_"RSeq" this]
        (when (pos? (:i this))
            (RSeq'new (:v this), (dec (:i this)))
        )
    )

    #_override
    (defn #_"int" IndexedSeq'''index--RSeq [#_"RSeq" this]
        (:i this)
    )

    #_override
    (defn #_"int" Counted'''count--RSeq [#_"RSeq" this]
        (inc (:i this))
    )

    #_override
    (defn #_"RSeq" IObj'''withMeta--RSeq [#_"RSeq" this, #_"IPersistentMap" meta]
        (RSeq'new meta, (:v this), (:i this))
    )
)

(class-ns APersistentVector
    (defn #_"APersistentVector" APersistentVector'new []
        (merge (AFn'new)
            (hash-map
                #_mutable #_"int" :_hash 0
                #_mutable #_"int" :_hasheq 0
            )
        )
    )

    #_foreign
    (defn #_"String" toString---APersistentVector [#_"APersistentVector" this]
        (RT'printString this)
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--APersistentVector [#_"APersistentVector" this]
        (when (pos? (.count this))
            (VSeq'new this, 0)
        )
    )

    #_override
    (defn #_"ISeq" Reversible'''rseq--APersistentVector [#_"APersistentVector" this]
        (when (pos? (.count this))
            (RSeq'new this, (dec (.count this)))
        )
    )

    (defn #_"boolean" APersistentVector'doEquals [#_"IPersistentVector" v, #_"Object" obj]
        (cond
            (instance? IPersistentVector obj)
                (let-when [#_"IPersistentVector" ov (cast IPersistentVector obj)] (= (.count ov) (.count v)) => false
                    (loop-when [#_"int" i 0] (< i (.count v)) => true
                        (recur-if (Util'equals (.nth v, i), (.nth ov, i)) [(inc i)] => false)
                    )
                )
            (instance? List obj)
                (let-when [#_"Collection" ma (cast Collection obj)] (and (= (.size ma) (.count v)) (= (.hashCode ma) (.hashCode v))) => false
                    (loop-when [#_"Iterator" i1 (.iterator (cast List v)) #_"Iterator" i2 (.iterator ma)] (.hasNext i1) => true
                        (recur-if (Util'equals (.next i1), (.next i2)) [i1 i2] => false)
                    )
                )
            :else
                (when (instance? Sequential obj) => false
                    (loop-when [#_"int" i 0 #_"ISeq" ms (RT'seq obj)] (< i (.count v)) => (nil? ms)
                        (recur-if (and (some? ms) (Util'equals (.nth v, i), (.first ms))) [(inc i) (.next ms)] => false)
                    )
                )
        )
    )

    (defn #_"boolean" APersistentVector'doEquiv [#_"IPersistentVector" v, #_"Object" obj]
        (cond
            (instance? IPersistentVector obj)
                (let-when [#_"IPersistentVector" ov (cast IPersistentVector obj)] (= (.count ov) (.count v)) => false
                    (loop-when [#_"int" i 0] (< i (.count v)) => true
                        (recur-if (Util'equiv-2oo (.nth v, i), (.nth ov, i)) [(inc i)] => false)
                    )
                )
            (instance? List obj)
                (let-when [#_"Collection" ma (cast Collection obj)] (= (.size ma) (.count v)) => false
                    (loop-when [#_"Iterator" i1 (.iterator (cast List v)) #_"Iterator" i2 (.iterator ma)] (.hasNext i1) => true
                        (recur-if (Util'equiv-2oo (.next i1), (.next i2)) [i1 i2] => false)
                    )
                )
            :else
                (when (instance? Sequential obj) => false
                    (loop-when [#_"int" i 0 #_"ISeq" ms (RT'seq obj)] (< i (.count v)) => (nil? ms)
                        (recur-if (and (some? ms) (Util'equiv-2oo (.nth v, i), (.first ms))) [(inc i) (.next ms)] => false)
                    )
                )
        )
    )

    #_foreign
    (defn #_"boolean" equals---APersistentVector [#_"APersistentVector" this, #_"Object" obj]
        (or (= obj this) (APersistentVector'doEquals this, obj))
    )

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--APersistentVector [#_"APersistentVector" this, #_"Object" obj]
        (or (= obj this) (APersistentVector'doEquiv this, obj))
    )

    #_foreign
    (defn #_"int" hashCode---APersistentVector [#_"APersistentVector" this]
        (let-when [#_"int" hash (:_hash this)] (zero? hash) => hash
            (let [hash
                    (loop-when [hash 1 #_"int" i 0] (< i (.count this)) => hash
                        (let [#_"Object" o (.nth this, i)]
                            (recur (+ (* 31 hash) (if (some? o) (.hashCode o) 0)) (inc i))
                        )
                    )]
                (§ set! (:_hash this) hash)
            )
        )
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--APersistentVector [#_"APersistentVector" this]
        (let-when [#_"int" hash (:_hasheq this)] (zero? hash) => hash
            (let [hash
                    (loop-when [hash 1 #_"int" i 0] (< i (.count this)) => (Murmur3'mixCollHash hash, i)
                        (recur (+ (* 31 hash) (Util'hasheq (.nth this, i))) (inc i))
                    )]
                (§ set! (:_hasheq this) hash)
            )
        )
    )

    #_foreign
    (defn #_"Object" get---APersistentVector [#_"APersistentVector" this, #_"int" index]
        (.nth this, index)
    )

    #_override
    (defn #_"Object" Indexed'''nth-3--APersistentVector [#_"APersistentVector" this, #_"int" i, #_"Object" notFound]
        (if (< -1 i (.count this)) (.nth this, i) notFound)
    )

    #_override
    (defn #_"Iterator" APersistentVector'''rangedIterator--APersistentVector [#_"APersistentVector" this, #_"int" start, #_"int" end]
        (§ reify Iterator
            [#_mutable #_"int" i start]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (< i end)
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (when (< i end) => (throw (NoSuchElementException.))
                    (let [_ (.nth this, i)]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--APersistentVector [#_"APersistentVector" this, #_"Object" arg1]
        (when (Numbers'isInteger arg1) => (throw (IllegalArgumentException. "Key must be integer"))
            (.nth this, (.intValue (cast Number arg1)))
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---APersistentVector [#_"APersistentVector" this]
        (§ reify Iterator
            [#_mutable #_"int" i 0]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (< i (.count this))
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (when (< i (.count this)) => (throw (NoSuchElementException.))
                    (let [_ (.nth this, i)]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )

    #_override
    (defn #_"Object" IPersistentStack'''peek--APersistentVector [#_"APersistentVector" this]
        (when (pos? (.count this))
            (.nth this, (dec (.count this)))
        )
    )

    #_override
    (defn #_"boolean" Associative'''containsKey--APersistentVector [#_"APersistentVector" this, #_"Object" key]
        (and (Numbers'isInteger key) (< -1 (.intValue (cast Number key)) (.count this)))
    )

    #_override
    (defn #_"IMapEntry" Associative'''entryAt--APersistentVector [#_"APersistentVector" this, #_"Object" key]
        (when (Numbers'isInteger key)
            (let-when [#_"int" i (.intValue (cast Number key))] (< -1 i (.count this))
                (cast IMapEntry (MapEntry'create key, (.nth this, i)))
            )
        )
    )

    #_override
    (defn #_"IPersistentVector" Associative'''assoc--APersistentVector [#_"APersistentVector" this, #_"Object" key, #_"Object" val]
        (when (Numbers'isInteger key) => (throw (IllegalArgumentException. "Key must be integer"))
            (.assocN this, (.intValue (cast Number key)), val)
        )
    )

    #_override
    (defn #_"Object" ILookup'''valAt-3--APersistentVector [#_"APersistentVector" this, #_"Object" key, #_"Object" notFound]
        (when (Numbers'isInteger key) => notFound
            (let-when [#_"int" i (.intValue (cast Number key))] (< -1 i (.count this)) => notFound
                (.nth this, i)
            )
        )
    )

    #_override
    (defn #_"Object" ILookup'''valAt-2--APersistentVector [#_"APersistentVector" this, #_"Object" key]
        (.valAt this, key, nil)
    )

    #_foreign
    (defn #_"Object[]" toArray---APersistentVector [#_"APersistentVector" this]
        (let [#_"Object[]" a (make-array Object (.count this))]
            (dotimes [#_"int" i (.count this)]
                (aset a i (.nth this, i))
            )
            a
        )
    )

    #_foreign
    (defn #_"Object[]" toArray---APersistentVector [#_"APersistentVector" this, #_"Object[]" a]
        (RT'seqToPassedArray (.seq this), a)
    )

    #_foreign
    (defn #_"int" size---APersistentVector [#_"APersistentVector" this]
        (.count this)
    )

    #_foreign
    (defn #_"boolean" contains---APersistentVector [#_"APersistentVector" this, #_"Object" o]
        (loop-when [#_"ISeq" s (.seq this)] (some? s) => false
            (or (Util'equiv-2oo (.first s), o) (recur (.next s)))
        )
    )

    #_foreign
    (defn #_"int" compareTo---APersistentVector [#_"APersistentVector" this, #_"Object" o]
        (let [#_"IPersistentVector" v (cast IPersistentVector o) #_"int" n (.count this) #_"int" m (.count v)]
            (cond (< n m) -1 (< m n) 1
                :else
                    (loop-when [#_"int" i 0] (< i n) => 0
                        (let [#_"int" cmp (Util'compare (.nth this, i), (.nth v, i))]
                            (recur-if (zero? cmp) [(inc i)] => cmp)
                        )
                    )
            )
        )
    )
)

(class-ns SubVector
    (defn #_"SubVector" SubVector'new [#_"IPersistentMap" meta, #_"IPersistentVector" v, #_"int" start, #_"int" end]
        (let [[v start end]
                (when (instance? SubVector v) => [v start end]
                    (let [#_"SubVector" sv (cast SubVector v)]
                        [(:v sv) (+ (:start sv) start) (+ (:start sv) end)]
                    )
                )]
            (merge (APersistentVector'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"IPersistentVector" :v v
                    #_"int" :start start
                    #_"int" :end end
                )
            )
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---SubVector [#_"SubVector" this]
        (when (instance? APersistentVector (:v this)) => (.iterator (§ super ))
            (.rangedIterator (cast APersistentVector (:v this)), (:start this), (:end this))
        )
    )

    #_override
    (defn #_"Object" Indexed'''nth-2--SubVector [#_"SubVector" this, #_"int" i]
        (when (and (<= 0 i) (< (+ (:start this) i) (:end this))) => (throw (IndexOutOfBoundsException.))
            (.nth (:v this), (+ (:start this) i))
        )
    )

    #_override
    (defn #_"IPersistentVector" IPersistentVector'''assocN--SubVector [#_"SubVector" this, #_"int" i, #_"Object" val]
        (cond
            (< (:end this) (+ (:start this) i)) (throw (IndexOutOfBoundsException.))
            (= (+ (:start this) i) (:end this)) (.cons this, val)
            :else (SubVector'new (:_meta this), (.assocN (:v this), (+ (:start this) i), val), (:start this), (:end this))
        )
    )

    #_override
    (defn #_"int" Counted'''count--SubVector [#_"SubVector" this]
        (- (:end this) (:start this))
    )

    #_override
    (defn #_"IPersistentVector" IPersistentVector'''cons--SubVector [#_"SubVector" this, #_"Object" o]
        (SubVector'new (:_meta this), (.assocN (:v this), (:end this), o), (:start this), (inc (:end this)))
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--SubVector [#_"SubVector" this]
        (.withMeta PersistentVector'EMPTY, (.meta this))
    )

    #_override
    (defn #_"IPersistentStack" IPersistentStack'''pop--SubVector [#_"SubVector" this]
        (if (= (dec (:end this)) (:start this))
            PersistentVector'EMPTY
            (SubVector'new (:_meta this), (:v this), (:start this), (dec (:end this)))
        )
    )

    #_override
    (defn #_"SubVector" IObj'''withMeta--SubVector [#_"SubVector" this, #_"IPersistentMap" meta]
        (when-not (= meta (:_meta this)) => this
            (SubVector'new meta, (:v this), (:start this), (:end this))
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--SubVector [#_"SubVector" this]
        (:_meta this)
    )
)
)

(java-ns cloiure.lang.AMapEntry

(class-ns AMapEntry
    (defn #_"AMapEntry" AMapEntry'new []
        (APersistentVector'new)
    )

    #_override
    (defn #_"Object" Indexed'''nth-2--AMapEntry [#_"AMapEntry" this, #_"int" i]
        (case i 0 (.key this) 1 (.val this) (throw (IndexOutOfBoundsException.)))
    )

    (declare LazilyPersistentVector'createOwning)

    #_method
    (defn- #_"IPersistentVector" AMapEntry''asVector [#_"AMapEntry" this]
        (LazilyPersistentVector'createOwning (.key this), (.val this))
    )

    #_override
    (defn #_"IPersistentVector" IPersistentVector'''assocN--AMapEntry [#_"AMapEntry" this, #_"int" i, #_"Object" val]
        (.assocN (AMapEntry''asVector this), i, val)
    )

    #_override
    (defn #_"int" Counted'''count--AMapEntry [#_"AMapEntry" this]
        2
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--AMapEntry [#_"AMapEntry" this]
        (.seq (AMapEntry''asVector this))
    )

    #_override
    (defn #_"IPersistentVector" IPersistentVector'''cons--AMapEntry [#_"AMapEntry" this, #_"Object" o]
        (.cons (AMapEntry''asVector this), o)
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--AMapEntry [#_"AMapEntry" this]
        nil
    )

    #_override
    (defn #_"IPersistentStack" IPersistentStack'''pop--AMapEntry [#_"AMapEntry" this]
        (LazilyPersistentVector'createOwning (.key this))
    )
)
)

(java-ns cloiure.lang.MapEntry

(class-ns MapEntry
    (defn- #_"MapEntry" MapEntry'new [#_"Object" key, #_"Object" val]
        (merge (AMapEntry'new)
            (hash-map
                #_"Object" :_key key
                #_"Object" :_val val
            )
        )
    )

    (defn #_"MapEntry" MapEntry'create [#_"Object" key, #_"Object" val]
        (MapEntry'new key, val)
    )

    #_override
    (defn #_"Object" IMapEntry'''key--MapEntry [#_"MapEntry" this]
        (:_key this)
    )

    #_override
    (defn #_"Object" IMapEntry'''val--MapEntry [#_"MapEntry" this]
        (:_val this)
    )

    #_foreign
    (defn #_"Object" getKey---MapEntry [#_"MapEntry" this]
        (.key this)
    )

    #_foreign
    (defn #_"Object" getValue---MapEntry [#_"MapEntry" this]
        (.val this)
    )
)
)

(java-ns cloiure.lang.ArrayChunk

(class-ns ArrayChunk
    (defn #_"ArrayChunk" ArrayChunk'new
        ([#_"Object[]" array] (ArrayChunk'new array, 0))
        ([#_"Object[]" array, #_"int" off] (ArrayChunk'new array, off, (alength array)))
        ([#_"Object[]" array, #_"int" off, #_"int" end]
            (hash-map
                #_"Object[]" :array array
                #_"int" :off off
                #_"int" :end end
            )
        )
    )

    #_override
    (defn #_"Object" Indexed'''nth-2--ArrayChunk [#_"ArrayChunk" this, #_"int" i]
        (aget (:array this) (+ (:off this) i))
    )

    #_override
    (defn #_"Object" Indexed'''nth-3--ArrayChunk [#_"ArrayChunk" this, #_"int" i, #_"Object" notFound]
        (if (< -1 i (.count this)) (.nth this, i) notFound)
    )

    #_override
    (defn #_"int" Counted'''count--ArrayChunk [#_"ArrayChunk" this]
        (- (:end this) (:off this))
    )

    #_override
    (defn #_"IChunk" IChunk'''dropFirst--ArrayChunk [#_"ArrayChunk" this]
        (when-not (= (:off this) (:end this)) => (throw (IllegalStateException. "dropFirst of empty chunk"))
            (ArrayChunk'new (:array this), (inc (:off this)), (:end this))
        )
    )

    #_override
    (defn #_"Object" IChunk'''reduce--ArrayChunk [#_"ArrayChunk" this, #_"IFn" f, #_"Object" r]
        (let [r (.invoke f, r, (aget (:array this) (:off this)))]
            (when-not (RT'isReduced r) => r
                (loop-when [#_"int" i (inc (:off this))] (< i (:end this)) => r
                    (let [r (.invoke f, r, (aget (:array this) i))]
                        (when-not (RT'isReduced r) => r
                            (recur (inc i))
                        )
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.ArrayIter

(class-ns ArrayIter_int
    (defn #_"Iterator" ArrayIter_int'new [#_"int[]" a, #_"int" i]
        (§ reify Iterator
            [#_mutable #_"int" i i]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (and (some? a) (< i (alength a)))
            )

            #_foreign
            (#_"Long" next [#_"Iterator" _self]
                (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                    (let [_ (Long/valueOf (aget a i))]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )
)

(class-ns ArrayIter_float
    (defn #_"Iterator" ArrayIter_float'new [#_"float[]" a, #_"int" i]
        (§ reify Iterator
            [#_mutable #_"int" i i]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (and (some? a) (< i (alength a)))
            )

            #_foreign
            (#_"Double" next [#_"Iterator" _self]
                (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                    (let [_ (Double/valueOf (aget a i))]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )
)

(class-ns ArrayIter_double
    (defn #_"Iterator" ArrayIter_double'new [#_"double[]" a, #_"int" i]
        (§ reify Iterator
            [#_mutable #_"int" i i]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (and (some? a) (< i (alength a)))
            )

            #_foreign
            (#_"Double" next [#_"Iterator" _self]
                (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                    (let [_ (aget a i)]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )
)

(class-ns ArrayIter_long
    (defn #_"Iterator" ArrayIter_long'new [#_"long[]" a, #_"int" i]
        (§ reify Iterator
            [#_mutable #_"int" i i]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (and (some? a) (< i (alength a)))
            )

            #_foreign
            (#_"Long" next [#_"Iterator" _self]
                (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                    (let [_ (Long/valueOf (aget a i))]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )
)

(class-ns ArrayIter_byte
    (defn #_"Iterator" ArrayIter_byte'new [#_"byte[]" a, #_"int" i]
        (§ reify Iterator
            [#_mutable #_"int" i i]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (and (some? a) (< i (alength a)))
            )

            #_foreign
            (#_"Byte" next [#_"Iterator" _self]
                (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                    (let [_ (aget a i)]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )
)

(class-ns ArrayIter_char
    (defn #_"Iterator" ArrayIter_char'new [#_"char[]" a, #_"int" i]
        (§ reify Iterator
            [#_mutable #_"int" i i]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (and (some? a) (< i (alength a)))
            )

            #_foreign
            (#_"Character" next [#_"Iterator" _self]
                (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                    (let [_ (aget a i)]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )
)

(class-ns ArrayIter_short
    (defn #_"Iterator" ArrayIter_short'new [#_"short[]" a, #_"int" i]
        (§ reify Iterator
            [#_mutable #_"int" i i]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (and (some? a) (< i (alength a)))
            )

            #_foreign
            (#_"Long" next [#_"Iterator" _self]
                (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                    (let [_ (Long/valueOf (aget a i))]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )
)

(class-ns ArrayIter_boolean
    (defn #_"Iterator" ArrayIter_boolean'new [#_"boolean[]" a, #_"int" i]
        (§ reify Iterator
            [#_mutable #_"int" i i]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (and (some? a) (< i (alength a)))
            )

            #_foreign
            (#_"Boolean" next [#_"Iterator" _self]
                (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                    (let [_ (Boolean/valueOf (aget a i))]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )
)

(class-ns ArrayIter
    (def #_"Iterator" ArrayIter'EMPTY_ITERATOR
        (reify Iterator
            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                false
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (throw (NoSuchElementException.))
            )
        )
    )

    (defn #_"Iterator" ArrayIter'new [#_"Object" array, #_"int" i]
        (let [#_"Object[]" a (cast Compiler'OBJECTS_CLASS array)]
            (§ reify Iterator
                [#_mutable #_"int" i i]

                #_foreign
                (#_"boolean" hasNext [#_"Iterator" _self]
                    (and (some? a) (< i (alength a)))
                )

                #_foreign
                (#_"Object" next [#_"Iterator" _self]
                    (when (and (some? a) (< i (alength a))) => (throw (NoSuchElementException.))
                        (let [_ (aget a i)]
                            (update! i inc)
                            _
                        )
                    )
                )
            )
        )
    )

    (defn #_"Iterator" ArrayIter'create-0 []
        ArrayIter'EMPTY_ITERATOR
    )

    (defn #_"Iterator" ArrayIter'create-1 [& #_"Object..." a]
        (when (and (some? a) (pos? (alength a))) => ArrayIter'EMPTY_ITERATOR
            (ArrayIter'new a, 0)
        )
    )

    (defn #_"Iterator" ArrayIter'createFromObject [#_"Object" a]
        (when (and (some? a) (pos? (Array/getLength a))) => ArrayIter'EMPTY_ITERATOR
            (let [#_"Class" c (.getClass a)]
                (condp = c
                    Compiler'INTS_CLASS     (ArrayIter_int'new     (cast c a), 0)
                    Compiler'FLOATS_CLASS   (ArrayIter_float'new   (cast c a), 0)
                    Compiler'DOUBLES_CLASS  (ArrayIter_double'new  (cast c a), 0)
                    Compiler'LONGS_CLASS    (ArrayIter_long'new    (cast c a), 0)
                    Compiler'BYTES_CLASS    (ArrayIter_byte'new    (cast c a), 0)
                    Compiler'CHARS_CLASS    (ArrayIter_char'new    (cast c a), 0)
                    Compiler'SHORTS_CLASS   (ArrayIter_short'new   (cast c a), 0)
                    Compiler'BOOLEANS_CLASS (ArrayIter_boolean'new (cast c a), 0)
                                            (ArrayIter'new                 a,  0)
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.ArraySeq

(class-ns ArraySeq_int
    (defn #_"ArraySeq_int" ArraySeq_int'new [#_"IPersistentMap" meta, #_"int[]" array, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"int[]" :array array
                #_"int" :i i
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq_int [#_"ArraySeq_int" this]
        (aget (:array this) (:i this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq_int [#_"ArraySeq_int" this]
        (when (< (inc (:i this)) (alength (:array this)))
            (ArraySeq_int'new (.meta this), (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq_int [#_"ArraySeq_int" this]
        (- (alength (:array this)) (:i this))
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq_int [#_"ArraySeq_int" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq_int" IObj'''withMeta--ArraySeq_int [#_"ArraySeq_int" this, #_"IPersistentMap" meta]
        (ArraySeq_int'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq_int [#_"ArraySeq_int" this, #_"IFn" f]
        (let [#_"int[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [#_"Object" r (aget a i) i (inc i)] (< i n) => r
                (let [r (.invoke f, r, (aget a i))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq_int [#_"ArraySeq_int" this, #_"IFn" f, #_"Object" r]
        (let [#_"int[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [r (.invoke f, r, (aget a i)) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (aget a i)) (inc i)))
            )
        )
    )
)

(class-ns ArraySeq_float
    (defn #_"ArraySeq_float" ArraySeq_float'new [#_"IPersistentMap" meta, #_"float[]" array, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"float[]" :array array
                #_"int" :i i
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq_float [#_"ArraySeq_float" this]
        (Numbers'num-1f (aget (:array this) (:i this)))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq_float [#_"ArraySeq_float" this]
        (when (< (inc (:i this)) (alength (:array this)))
            (ArraySeq_float'new (.meta this), (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq_float [#_"ArraySeq_float" this]
        (- (alength (:array this)) (:i this))
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq_float [#_"ArraySeq_float" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq_float" IObj'''withMeta--ArraySeq_float [#_"ArraySeq_float" this, #_"IPersistentMap" meta]
        (ArraySeq_float'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq_float [#_"ArraySeq_float" this, #_"IFn" f]
        (let [#_"float[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [#_"Object" r (Numbers'num-1f (aget a i)) i (inc i)] (< i n) => r
                (let [r (.invoke f, r, (Numbers'num-1f (aget a i)))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq_float [#_"ArraySeq_float" this, #_"IFn" f, #_"Object" r]
        (let [#_"float[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [r (.invoke f, r, (Numbers'num-1f (aget a i))) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (Numbers'num-1f (aget a i))) (inc i)))
            )
        )
    )
)

(class-ns ArraySeq_double
    (defn #_"ArraySeq_double" ArraySeq_double'new [#_"IPersistentMap" meta, #_"double[]" array, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"double[]" :array array
                #_"int" :i i
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq_double [#_"ArraySeq_double" this]
        (aget (:array this) (:i this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq_double [#_"ArraySeq_double" this]
        (when (< (inc (:i this)) (alength (:array this)))
            (ArraySeq_double'new (.meta this), (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq_double [#_"ArraySeq_double" this]
        (- (alength (:array this)) (:i this))
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq_double [#_"ArraySeq_double" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq_double" IObj'''withMeta--ArraySeq_double [#_"ArraySeq_double" this, #_"IPersistentMap" meta]
        (ArraySeq_double'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq_double [#_"ArraySeq_double" this, #_"IFn" f]
        (let [#_"double[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [#_"Object" r (aget a i) i (inc i)] (< i n) => r
                (let [r (.invoke f, r, (aget a i))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq_double [#_"ArraySeq_double" this, #_"IFn" f, #_"Object" r]
        (let [#_"double[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [r (.invoke f, r, (aget a i)) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (aget a i)) (inc i)))
            )
        )
    )
)

(class-ns ArraySeq_long
    (defn #_"ArraySeq_long" ArraySeq_long'new [#_"IPersistentMap" meta, #_"long[]" array, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"long[]" :array array
                #_"int" :i i
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq_long [#_"ArraySeq_long" this]
        (Numbers'num-1l (aget (:array this) (:i this)))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq_long [#_"ArraySeq_long" this]
        (when (< (inc (:i this)) (alength (:array this)))
            (ArraySeq_long'new (.meta this), (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq_long [#_"ArraySeq_long" this]
        (- (alength (:array this)) (:i this))
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq_long [#_"ArraySeq_long" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq_long" IObj'''withMeta--ArraySeq_long [#_"ArraySeq_long" this, #_"IPersistentMap" meta]
        (ArraySeq_long'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq_long [#_"ArraySeq_long" this, #_"IFn" f]
        (let [#_"long[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [#_"Object" r (Numbers'num-1l (aget a i)) i (inc i)] (< i n) => r
                (let [r (.invoke f, r, (Numbers'num-1l (aget a i)))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq_long [#_"ArraySeq_long" this, #_"IFn" f, #_"Object" r]
        (let [#_"long[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [r (.invoke f, r, (Numbers'num-1l (aget a i))) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (Numbers'num-1l (aget a i))) (inc i)))
            )
        )
    )
)

(class-ns ArraySeq_byte
    (defn #_"ArraySeq_byte" ArraySeq_byte'new [#_"IPersistentMap" meta, #_"byte[]" array, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"byte[]" :array array
                #_"int" :i i
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq_byte [#_"ArraySeq_byte" this]
        (aget (:array this) (:i this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq_byte [#_"ArraySeq_byte" this]
        (when (< (inc (:i this)) (alength (:array this)))
            (ArraySeq_byte'new (.meta this), (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq_byte [#_"ArraySeq_byte" this]
        (- (alength (:array this)) (:i this))
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq_byte [#_"ArraySeq_byte" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq_byte" IObj'''withMeta--ArraySeq_byte [#_"ArraySeq_byte" this, #_"IPersistentMap" meta]
        (ArraySeq_byte'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq_byte [#_"ArraySeq_byte" this, #_"IFn" f]
        (let [#_"byte[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [#_"Object" r (aget a i) i (inc i)] (< i n) => r
                (let [r (.invoke f, r, (aget a i))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq_byte [#_"ArraySeq_byte" this, #_"IFn" f, #_"Object" r]
        (let [#_"byte[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [r (.invoke f, r, (aget a i)) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (aget a i)) (inc i)))
            )
        )
    )
)

(class-ns ArraySeq_char
    (defn #_"ArraySeq_char" ArraySeq_char'new [#_"IPersistentMap" meta, #_"char[]" array, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"char[]" :array array
                #_"int" :i i
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq_char [#_"ArraySeq_char" this]
        (aget (:array this) (:i this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq_char [#_"ArraySeq_char" this]
        (when (< (inc (:i this)) (alength (:array this)))
            (ArraySeq_char'new (.meta this), (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq_char [#_"ArraySeq_char" this]
        (- (alength (:array this)) (:i this))
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq_char [#_"ArraySeq_char" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq_char" IObj'''withMeta--ArraySeq_char [#_"ArraySeq_char" this, #_"IPersistentMap" meta]
        (ArraySeq_char'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq_char [#_"ArraySeq_char" this, #_"IFn" f]
        (let [#_"char[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [#_"Object" r (aget a i) i (inc i)] (< i n) => r
                (let [r (.invoke f, r, (aget a i))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq_char [#_"ArraySeq_char" this, #_"IFn" f, #_"Object" r]
        (let [#_"char[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [r (.invoke f, r, (aget a i)) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (aget a i)) (inc i)))
            )
        )
    )
)

(class-ns ArraySeq_short
    (defn #_"ArraySeq_short" ArraySeq_short'new [#_"IPersistentMap" meta, #_"short[]" array, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"short[]" :array array
                #_"int" :i i
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq_short [#_"ArraySeq_short" this]
        (aget (:array this) (:i this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq_short [#_"ArraySeq_short" this]
        (when (< (inc (:i this)) (alength (:array this)))
            (ArraySeq_short'new (.meta this), (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq_short [#_"ArraySeq_short" this]
        (- (alength (:array this)) (:i this))
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq_short [#_"ArraySeq_short" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq_short" IObj'''withMeta--ArraySeq_short [#_"ArraySeq_short" this, #_"IPersistentMap" meta]
        (ArraySeq_short'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq_short [#_"ArraySeq_short" this, #_"IFn" f]
        (let [#_"short[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [#_"Object" r (aget a i) i (inc i)] (< i n) => r
                (let [r (.invoke f, r, (aget a i))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq_short [#_"ArraySeq_short" this, #_"IFn" f, #_"Object" r]
        (let [#_"short[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [r (.invoke f, r, (aget a i)) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (aget a i)) (inc i)))
            )
        )
    )
)

(class-ns ArraySeq_boolean
    (defn #_"ArraySeq_boolean" ArraySeq_boolean'new [#_"IPersistentMap" meta, #_"boolean[]" array, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"boolean[]" :array array
                #_"int" :i i
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq_boolean [#_"ArraySeq_boolean" this]
        (aget (:array this) (:i this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq_boolean [#_"ArraySeq_boolean" this]
        (when (< (inc (:i this)) (alength (:array this)))
            (ArraySeq_boolean'new (.meta this), (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq_boolean [#_"ArraySeq_boolean" this]
        (- (alength (:array this)) (:i this))
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq_boolean [#_"ArraySeq_boolean" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq_boolean" IObj'''withMeta--ArraySeq_boolean [#_"ArraySeq_boolean" this, #_"IPersistentMap" meta]
        (ArraySeq_boolean'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq_boolean [#_"ArraySeq_boolean" this, #_"IFn" f]
        (let [#_"boolean[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [#_"Object" r (aget a i) i (inc i)] (< i n) => r
                (let [r (.invoke f, r, (aget a i))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq_boolean [#_"ArraySeq_boolean" this, #_"IFn" f, #_"Object" r]
        (let [#_"boolean[]" a (:array this) #_"int" i (:i this) #_"int" n (alength a)]
            (loop-when [r (.invoke f, r, (aget a i)) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (aget a i)) (inc i)))
            )
        )
    )
)

(class-ns ArraySeq
    (defn #_"ArraySeq" ArraySeq'new
        ([#_"Object" array, #_"int" i] (ArraySeq'new nil, array, i))
        ([#_"IPersistentMap" meta, #_"Object" array, #_"int" i]
            (merge (ASeq'new meta)
                (hash-map
                    #_"Object[]" :array (cast Compiler'OBJECTS_CLASS array)
                    #_"int" :i i
                )
            )
        )
    )

    (defn #_"ArraySeq" ArraySeq'create-0 []
        nil
    )

    (defn #_"ArraySeq" ArraySeq'create-1 [& #_"Object..." array]
        (when (and (some? array) (pos? (alength array)))
            (ArraySeq'new array, 0)
        )
    )

    (defn #_"ISeq" ArraySeq'createFromObject [#_"Object" array]
        (when (and (some? array) (pos? (Array/getLength array)))
            (let [#_"Class" c (.getClass array)]
                (condp = c
                    Compiler'INTS_CLASS     (ArraySeq_int'new     nil, (cast c array), 0)
                    Compiler'FLOATS_CLASS   (ArraySeq_float'new   nil, (cast c array), 0)
                    Compiler'DOUBLES_CLASS  (ArraySeq_double'new  nil, (cast c array), 0)
                    Compiler'LONGS_CLASS    (ArraySeq_long'new    nil, (cast c array), 0)
                    Compiler'BYTES_CLASS    (ArraySeq_byte'new    nil, (cast c array), 0)
                    Compiler'CHARS_CLASS    (ArraySeq_char'new    nil, (cast c array), 0)
                    Compiler'SHORTS_CLASS   (ArraySeq_short'new   nil, (cast c array), 0)
                    Compiler'BOOLEANS_CLASS (ArraySeq_boolean'new nil, (cast c array), 0)
                                            (ArraySeq'new                      array,  0)
                )
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ArraySeq [#_"ArraySeq" this]
        (when (some? (:array this))
            (aget (:array this) (:i this))
        )
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ArraySeq [#_"ArraySeq" this]
        (when (and (some? (:array this)) (< (inc (:i this)) (alength (:array this))))
            (ArraySeq'new (:array this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" Counted'''count--ArraySeq [#_"ArraySeq" this]
        (if (some? (:array this)) (- (alength (:array this)) (:i this)) 0)
    )

    #_override
    (defn #_"int" IndexedSeq'''index--ArraySeq [#_"ArraySeq" this]
        (:i this)
    )

    #_override
    (defn #_"ArraySeq" IObj'''withMeta--ArraySeq [#_"ArraySeq" this, #_"IPersistentMap" meta]
        (ArraySeq'new meta, (:array this), (:i this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--ArraySeq [#_"ArraySeq" this, #_"IFn" f]
        (when-let [#_"Object[]" a (:array this)]
            (let [#_"int" i (:i this) #_"int" n (alength a)]
                (loop-when [#_"Object" r (aget a i) i (inc i)] (< i n) => r
                    (let [r (.invoke f, r, (aget a i))]
                        (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                    )
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--ArraySeq [#_"ArraySeq" this, #_"IFn" f, #_"Object" r]
        (when-let [#_"Object[]" a (:array this)]
            (let [#_"int" i (:i this) #_"int" n (alength a)]
                (loop-when [r (.invoke f, r, (aget a i)) i (inc i)] (< i n) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (aget a i)) (inc i)))
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.Atom

(class-ns Atom
    (defn #_"Atom" Atom'new
        ([#_"Object" state] (Atom'new state, nil))
        ([#_"Object" state, #_"IPersistentMap" meta]
            (hash-map
                #_"AtomicReference" :state (AtomicReference. state)
                #_mutable #_"IPersistentMap" :_meta meta
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--Atom [#_"Atom" this]
        (§ sync this
            (:_meta this)
        )
    )

    #_override
    (defn #_"IPersistentMap" IReference'''alterMeta--Atom [#_"Atom" this, #_"IFn" alter, #_"ISeq" args]
        (§ sync this
            (§ update! (:_meta this) #(cast IPersistentMap (.applyTo alter, (Cons'new %, args))))
        )
    )

    #_override
    (defn #_"IPersistentMap" IReference'''resetMeta--Atom [#_"Atom" this, #_"IPersistentMap" m]
        (§ sync this
            (§ set! (:_meta this) m)
        )
    )

    #_override
    (defn #_"Object" IDeref'''deref--Atom [#_"Atom" this]
        (.get (:state this))
    )

    #_override
    (defn #_"Object" IAtom'''swap-2--Atom [#_"Atom" this, #_"IFn" f]
        (loop []
            (let [#_"Object" v (.deref this) #_"Object" newv (.invoke f, v)]
                (when (.compareAndSet (:state this), v, newv) => (recur)
                    newv
                )
            )
        )
    )

    #_override
    (defn #_"Object" IAtom'''swap-3--Atom [#_"Atom" this, #_"IFn" f, #_"Object" arg]
        (loop []
            (let [#_"Object" v (.deref this) #_"Object" newv (.invoke f, v, arg)]
                (when (.compareAndSet (:state this), v, newv) => (recur)
                    newv
                )
            )
        )
    )

    #_override
    (defn #_"Object" IAtom'''swap-4--Atom [#_"Atom" this, #_"IFn" f, #_"Object" arg1, #_"Object" arg2]
        (loop []
            (let [#_"Object" v (.deref this) #_"Object" newv (.invoke f, v, arg1, arg2)]
                (when (.compareAndSet (:state this), v, newv) => (recur)
                    newv
                )
            )
        )
    )

    #_override
    (defn #_"Object" IAtom'''swap-5--Atom [#_"Atom" this, #_"IFn" f, #_"Object" x, #_"Object" y, #_"ISeq" args]
        (loop []
            (let [#_"Object" v (.deref this) #_"Object" newv (.applyTo f, (RT'listStar v, x, y, args))]
                (when (.compareAndSet (:state this), v, newv) => (recur)
                    newv
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentVector" IAtom2'''swapVals-2--Atom [#_"Atom" this, #_"IFn" f]
        (loop []
            (let [#_"Object" oldv (.deref this) #_"Object" newv (.invoke f, oldv)]
                (when (.compareAndSet (:state this), oldv, newv) => (recur)
                    (LazilyPersistentVector'createOwning oldv, newv)
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentVector" IAtom2'''swapVals-3--Atom [#_"Atom" this, #_"IFn" f, #_"Object" arg]
        (loop []
            (let [#_"Object" oldv (.deref this) #_"Object" newv (.invoke f, oldv, arg)]
                (when (.compareAndSet (:state this), oldv, newv) => (recur)
                    (LazilyPersistentVector'createOwning oldv, newv)
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentVector" IAtom2'''swapVals-4--Atom [#_"Atom" this, #_"IFn" f, #_"Object" arg1, #_"Object" arg2]
        (loop []
            (let [#_"Object" oldv (.deref this) #_"Object" newv (.invoke f, oldv, arg1, arg2)]
                (when (.compareAndSet (:state this), oldv, newv) => (recur)
                    (LazilyPersistentVector'createOwning oldv, newv)
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentVector" IAtom2'''swapVals-5--Atom [#_"Atom" this, #_"IFn" f, #_"Object" x, #_"Object" y, #_"ISeq" args]
        (loop []
            (let [#_"Object" oldv (.deref this) #_"Object" newv (.applyTo f, (RT'listStar oldv, x, y, args))]
                (when (.compareAndSet (:state this), oldv, newv) => (recur)
                    (LazilyPersistentVector'createOwning oldv, newv)
                )
            )
        )
    )

    #_override
    (defn #_"boolean" IAtom'''compareAndSet--Atom [#_"Atom" this, #_"Object" oldv, #_"Object" newv]
        (.compareAndSet (:state this), oldv, newv)
    )

    #_override
    (defn #_"Object" IAtom'''reset--Atom [#_"Atom" this, #_"Object" newval]
        (let [#_"Object" oldval (.get (:state this))]
            (.set (:state this), newval)
            newval
        )
    )

    #_override
    (defn #_"IPersistentVector" IAtom2'''resetVals--Atom [#_"Atom" this, #_"Object" newv]
        (loop []
            (let [#_"Object" oldv (.deref this)]
                (when (.compareAndSet (:state this), oldv, newv) => (recur)
                    (LazilyPersistentVector'createOwning oldv, newv)
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.ATransientMap

(class-ns ATransientMap
    (defn #_"ATransientMap" ATransientMap'new []
        (AFn'new)
    )

    #_method
    (defn #_"ITransientMap" ATransientMap''conj [#_"ATransientMap" this, #_"Object" o]
        (.ensureEditable this)
        (cond
            (instance? Map$Entry o)
                (let [#_"Map$Entry" e (cast Map$Entry o)]
                    (.assoc this, (.getKey e), (.getValue e))
                )
            (instance? IPersistentVector o)
                (let [#_"IPersistentVector" v (cast IPersistentVector o)]
                    (when (= (.count v) 2) => (throw (IllegalArgumentException. "Vector arg to map conj must be a pair"))
                        (.assoc this, (.nth v, 0), (.nth v, 1))
                    )
                )
            :else
                (loop-when [#_"ITransientMap" m this #_"ISeq" s (RT'seq o)] (some? s) => m
                    (let [#_"Map$Entry" e (cast Map$Entry (.first s))]
                        (recur (.assoc m, (.getKey e), (.getValue e)) (.next s))
                    )
                )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--ATransientMap [#_"ATransientMap" this, #_"Object" arg1]
        (.valAt this, arg1)
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--ATransientMap [#_"ATransientMap" this, #_"Object" arg1, #_"Object" notFound]
        (.valAt this, arg1, notFound)
    )

    #_override
    (defn #_"Object" ILookup'''valAt-2--ATransientMap [#_"ATransientMap" this, #_"Object" key]
        (.valAt this, key, nil)
    )

    #_override
    (defn #_"ITransientMap" ITransientMap'''assoc--ATransientMap [#_"ATransientMap" this, #_"Object" key, #_"Object" val]
        (.ensureEditable this)
        (.doAssoc this, key, val)
    )

    #_override
    (defn #_"ITransientMap" ITransientMap'''without--ATransientMap [#_"ATransientMap" this, #_"Object" key]
        (.ensureEditable this)
        (.doWithout this, key)
    )

    #_override
    (defn #_"IPersistentMap" ITransientMap'''persistent--ATransientMap [#_"ATransientMap" this]
        (.ensureEditable this)
        (.doPersistent this)
    )

    #_override
    (defn #_"Object" ILookup'''valAt-3--ATransientMap [#_"ATransientMap" this, #_"Object" key, #_"Object" notFound]
        (.ensureEditable this)
        (.doValAt this, key, notFound)
    )

    (def- #_"Object" ATransientMap'NOT_FOUND (Object.))

    #_override
    (defn #_"boolean" ITransientAssociative2'''containsKey--ATransientMap [#_"ATransientMap" this, #_"Object" key]
        (not= (.valAt this, key, ATransientMap'NOT_FOUND) ATransientMap'NOT_FOUND)
    )

    #_override
    (defn #_"IMapEntry" ITransientAssociative2'''entryAt--ATransientMap [#_"ATransientMap" this, #_"Object" key]
        (let [#_"Object" v (.valAt this, key, ATransientMap'NOT_FOUND)]
            (when-not (= v ATransientMap'NOT_FOUND)
                (MapEntry'create key, v)
            )
        )
    )

    #_override
    (defn #_"int" Counted'''count--ATransientMap [#_"ATransientMap" this]
        (.ensureEditable this)
        (.doCount this)
    )
)
)

(java-ns cloiure.lang.ATransientSet

(class-ns ATransientSet
    (defn #_"ATransientSet" ATransientSet'new [#_"ITransientMap" impl]
        (merge (AFn'new)
            (hash-map
                #_"ITransientMap" :impl impl
            )
        )
    )

    #_override
    (defn #_"int" Counted'''count--ATransientSet [#_"ATransientSet" this]
        (.count (:impl this))
    )

    #_override
    (defn #_"ITransientSet" ITransientCollection'''conj--ATransientSet [#_"ATransientSet" this, #_"Object" val]
        (let [#_"ITransientMap" m (.assoc (:impl this), val, val)]
            (when-not (= m (:impl this)) => this
                (assoc this :impl m)
            )
        )
    )

    #_override
    (defn #_"boolean" ITransientSet'''contains--ATransientSet [#_"ATransientSet" this, #_"Object" key]
        (not= this (.valAt (:impl this), key, this))
    )

    #_override
    (defn #_"ITransientSet" ITransientSet'''disjoin--ATransientSet [#_"ATransientSet" this, #_"Object" key]
        (let [#_"ITransientMap" m (.without (:impl this), key)]
            (when-not (= m (:impl this)) => this
                (assoc this :impl m)
            )
        )
    )

    #_override
    (defn #_"Object" ITransientSet'''get--ATransientSet [#_"ATransientSet" this, #_"Object" key]
        (.valAt (:impl this), key)
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--ATransientSet [#_"ATransientSet" this, #_"Object" key, #_"Object" notFound]
        (.valAt (:impl this), key, notFound)
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--ATransientSet [#_"ATransientSet" this, #_"Object" key]
        (.valAt (:impl this), key)
    )
)
)

(java-ns cloiure.lang.Binding

(class-ns Binding
    (defn #_"Binding" Binding'new
        ([#_"T" val] (Binding'new val, nil))
        ([#_"T" val, #_"Binding" rest]
            (hash-map
                #_"T" :val val
                #_"Binding" :rest rest
            )
        )
    )
)
)

(java-ns cloiure.lang.Box

(class-ns Box
    (defn #_"Box" Box'new [#_"Object" val]
        (hash-map
            #_mutable #_"Object" :val val
        )
    )
)
)

(java-ns cloiure.lang.ChunkBuffer

(class-ns ChunkBuffer
    (defn #_"ChunkBuffer" ChunkBuffer'new [#_"int" capacity]
        (hash-map
            #_mutable #_"Object[]" :buffer (make-array Object capacity)
            #_mutable #_"int" :end 0
        )
    )

    #_method
    (defn #_"void" ChunkBuffer''add [#_"ChunkBuffer" this, #_"Object" o]
        (aset (:buffer this) (:end this) o)
        (§ update! (:end this) inc)
        nil
    )

    #_method
    (defn #_"IChunk" ChunkBuffer''chunk [#_"ChunkBuffer" this]
        (let [_ (ArrayChunk'new (:buffer this), 0, (:end this))]
            (§ set! (:buffer this) nil)
            _
        )
    )

    #_override
    (defn #_"int" Counted'''count--ChunkBuffer [#_"ChunkBuffer" this]
        (:end this)
    )
)
)

(java-ns cloiure.lang.ChunkedCons

(class-ns ChunkedCons
    (defn #_"ChunkedCons" ChunkedCons'new
        ([#_"IChunk" chunk, #_"ISeq" more] (ChunkedCons'new nil, chunk, more))
        ([#_"IPersistentMap" meta, #_"IChunk" chunk, #_"ISeq" more]
            (merge (ASeq'new meta)
                (hash-map
                    #_"IChunk" :chunk chunk
                    #_"ISeq" :_more more
                )
            )
        )
    )

    #_override
    (defn #_"ChunkedCons" IObj'''withMeta--ChunkedCons [#_"ChunkedCons" this, #_"IPersistentMap" meta]
        (when-not (= meta (:_meta this)) => this
            (ChunkedCons'new meta, (:chunk this), (:_more this))
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ChunkedCons [#_"ChunkedCons" this]
        (.nth (:chunk this), 0)
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ChunkedCons [#_"ChunkedCons" this]
        (if (< 1 (.count (:chunk this)))
            (ChunkedCons'new (.dropFirst (:chunk this)), (:_more this))
            (.chunkedNext this)
        )
    )

    #_override
    (defn #_"ISeq" ISeq'''more--ChunkedCons [#_"ChunkedCons" this]
        (if (< 1 (.count (:chunk this)))
            (ChunkedCons'new (.dropFirst (:chunk this)), (:_more this))
            (or (:_more this) PersistentList'EMPTY)
        )
    )

    #_override
    (defn #_"IChunk" IChunkedSeq'''chunkedFirst--ChunkedCons [#_"ChunkedCons" this]
        (:chunk this)
    )

    #_override
    (defn #_"ISeq" IChunkedSeq'''chunkedNext--ChunkedCons [#_"ChunkedCons" this]
        (.seq (.chunkedMore this))
    )

    #_override
    (defn #_"ISeq" IChunkedSeq'''chunkedMore--ChunkedCons [#_"ChunkedCons" this]
        (or (:_more this) PersistentList'EMPTY)
    )
)
)

(java-ns cloiure.lang.Cons

(class-ns Cons
    (defn #_"Cons" Cons'new
        ([#_"Object" _first, #_"ISeq" _more] (Cons'new nil, _first, _more))
        ([#_"IPersistentMap" meta, #_"Object" _first, #_"ISeq" _more]
            (merge (ASeq'new meta)
                (hash-map
                    #_"Object" :_first _first
                    #_"ISeq" :_more _more
                )
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--Cons [#_"Cons" this]
        (:_first this)
    )

    #_override
    (defn #_"ISeq" ISeq'''next--Cons [#_"Cons" this]
        (.seq (.more this))
    )

    #_override
    (defn #_"ISeq" ISeq'''more--Cons [#_"Cons" this]
        (or (:_more this) PersistentList'EMPTY)
    )

    #_override
    (defn #_"int" Counted'''count--Cons [#_"Cons" this]
        (inc (RT'count (:_more this)))
    )

    #_override
    (defn #_"Cons" IObj'''withMeta--Cons [#_"Cons" this, #_"IPersistentMap" meta]
        (Cons'new meta, (:_first this), (:_more this))
    )
)
)

(java-ns cloiure.lang.Cycle

(class-ns Cycle
    (defn- #_"Cycle" Cycle'new
        ([#_"ISeq" all, #_"ISeq" prev, #_"ISeq" current] (Cycle'new nil, all, prev, current, nil))
        ([#_"IPersistentMap" meta, #_"ISeq" all, #_"ISeq" prev, #_"ISeq" current, #_"ISeq" next]
            (merge (ASeq'new meta)
                (hash-map
                    #_"ISeq" :all all ;; never nil
                    #_"ISeq" :prev prev
                    #_volatile #_"ISeq" :_current current ;; lazily realized
                    #_volatile #_"ISeq" :_next next ;; cached
                )
            )
        )
    )

    (defn #_"ISeq" Cycle'create [#_"ISeq" vals]
        (if (some? vals) (Cycle'new vals, nil, vals) PersistentList'EMPTY)
    )

    #_method
    (defn- #_"ISeq" Cycle''current [#_"Cycle" this]
        (when (nil? (:_current this))
            (let [#_"ISeq" current (.next (:prev this))]
                (§ set! (:_current this) (or current (:all this)))
            )
        )
        (:_current this)
    )

    #_override
    (defn #_"boolean" IPending'''isRealized--Cycle [#_"Cycle" this]
        (some? (:_current this))
    )

    #_override
    (defn #_"Object" ISeq'''first--Cycle [#_"Cycle" this]
        (.first (Cycle''current this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--Cycle [#_"Cycle" this]
        (when (nil? (:_next this))
            (§ set! (:_next this) (Cycle'new (:all this), (Cycle''current this), nil))
        )
        (:_next this)
    )

    #_override
    (defn #_"Cycle" IObj'''withMeta--Cycle [#_"Cycle" this, #_"IPersistentMap" meta]
        (Cycle'new meta, (:all this), (:prev this), (:_current this), (:_next this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--Cycle [#_"Cycle" this, #_"IFn" f]
        (loop [#_"ISeq" s (Cycle''current this) #_"Object" r (.first s)]
            (let [s (or (.next s) (:all this)) r (.invoke f, r, (.first s))]
                (when-not (RT'isReduced r) => (.deref (cast IDeref r))
                    (recur s r)
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--Cycle [#_"Cycle" this, #_"IFn" f, #_"Object" r]
        (loop [#_"ISeq" s (Cycle''current this) r (.invoke f, r, (.first s))]
            (when-not (RT'isReduced r) => (.deref (cast IDeref r))
                (let [s (or (.next s) (:all this))]
                    (recur s (.invoke f, r, (.first s)))
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.Delay

(class-ns Delay
    (defn #_"Delay" Delay'new [#_"IFn" fn]
        (hash-map
            #_volatile #_"Object" :val nil
            #_volatile #_"Throwable" :exception nil
            #_volatile #_"IFn" :fn fn
        )
    )

    (defn #_"Object" Delay'force [#_"Object" x]
        (if (instance? Delay x) (.deref (cast Delay x)) x)
    )

    #_override
    (defn #_"Object" IDeref'''deref--Delay [#_"Delay" this]
        (when (some? (:fn this))
            (§ sync this
                ;; double check
                (when (some? (:fn this))
                    (try
                        (§ set! (:val this) (.invoke (:fn this)))
                        (catch Throwable t
                            (§ set! (:exception this) t)
                        )
                    )
                    (§ set! (:fn this) nil)
                )
            )
        )
        (when (some? (:exception this))
            (throw (:exception this))
        )
        (:val this)
    )

    #_override
    (defn #_"boolean" IPending'''isRealized--Delay [#_"Delay" this]
        (§ sync this
            (nil? (:fn this))
        )
    )
)
)

(java-ns cloiure.lang.Iterate

(class-ns Iterate
    (def- #_"Object" Iterate'UNREALIZED_SEED (Object.))

    (defn- #_"Iterate" Iterate'new
        ([#_"IFn" f, #_"Object" prevSeed, #_"Object" seed] (Iterate'new nil, f, prevSeed, seed, nil))
        ([#_"IPersistentMap" meta, #_"IFn" f, #_"Object" prevSeed, #_"Object" seed, #_"ISeq" next]
            (merge (ASeq'new meta)
                (hash-map
                    #_"IFn" :f f ;; never nil
                    #_"Object" :prevSeed prevSeed
                    #_volatile #_"Object" :_seed seed ;; lazily realized
                    #_volatile #_"ISeq" :_next next ;; cached
                )
            )
        )
    )

    (defn #_"ISeq" Iterate'create [#_"IFn" f, #_"Object" seed]
        (Iterate'new f, nil, seed)
    )

    #_override
    (defn #_"boolean" IPending'''isRealized--Iterate [#_"Iterate" this]
        (not= (:_seed this) Iterate'UNREALIZED_SEED)
    )

    #_override
    (defn #_"Object" ISeq'''first--Iterate [#_"Iterate" this]
        (when (= (:_seed this) Iterate'UNREALIZED_SEED)
            (§ set! (:_seed this) (.invoke (:f this), (:prevSeed this)))
        )
        (:_seed this)
    )

    #_override
    (defn #_"ISeq" ISeq'''next--Iterate [#_"Iterate" this]
        (when (nil? (:_next this))
            (§ set! (:_next this) (Iterate'new (:f this), (.first this), Iterate'UNREALIZED_SEED))
        )
        (:_next this)
    )

    #_override
    (defn #_"Iterate" IObj'''withMeta--Iterate [#_"Iterate" this, #_"IPersistentMap" meta]
        (Iterate'new meta, (:f this), (:prevSeed this), (:_seed this), (:_next this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--Iterate [#_"Iterate" this, #_"IFn" f]
        (loop [#_"Object" r (.first this) #_"Object" v (.invoke (:f this), r)]
            (let [r (.invoke f, r, v)]
                (when-not (RT'isReduced r) => (.deref (cast IDeref r))
                    (recur r (.invoke (:f this), v))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--Iterate [#_"Iterate" this, #_"IFn" f, #_"Object" r]
        (loop [r r #_"Object" v (.first this)]
            (let [r (.invoke f, r, v)]
                (when-not (RT'isReduced r) => (.deref (cast IDeref r))
                    (recur r (.invoke (:f this), v))
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.KeywordLookupSite

(class-ns KeywordLookupSite
    (defn #_"KeywordLookupSite" KeywordLookupSite'new [#_"Keyword" k]
        (hash-map
            #_"Keyword" :k k
        )
    )

    #_method
    (defn- #_"ILookupThunk" KeywordLookupSite''ilookupThunk [#_"KeywordLookupSite" this, #_"Class" c]
        (reify ILookupThunk
            #_override
            (#_"Object" get [#_"ILookupThunk" self, #_"Object" target]
                (if (and (some? target) (= (.getClass target) c))
                    (.valAt (cast ILookup target), (:k this))
                    self
                )
            )
        )
    )

    #_override
    (defn #_"ILookupThunk" ILookupSite'''fault--KeywordLookupSite [#_"KeywordLookupSite" this, #_"Object" target]
        (if (instance? ILookup target)
            (KeywordLookupSite''ilookupThunk this, (.getClass target))
            this
        )
    )

    #_override
    (defn #_"Object" ILookupThunk'''get--KeywordLookupSite [#_"KeywordLookupSite" this, #_"Object" target]
        (if (instance? ILookup target)
            this
            (RT'get target, (:k this))
        )
    )
)
)

(java-ns cloiure.lang.LongRange

(class-ns LongChunk
    (defn #_"LongChunk" LongChunk'new [#_"long" start, #_"long" step, #_"int" count]
        (hash-map
            #_"long" :start start
            #_"long" :step step
            #_"int" :count count
        )
    )

    #_method
    (defn #_"long" LongChunk''first [#_"LongChunk" this]
        (:start this)
    )

    #_override
    (defn #_"Object" Indexed'''nth-2--LongChunk [#_"LongChunk" this, #_"int" i]
        (+ (:start this) (* i (:step this)))
    )

    #_override
    (defn #_"Object" Indexed'''nth-3--LongChunk [#_"LongChunk" this, #_"int" i, #_"Object" notFound]
        (if (< -1 i (:count this)) (+ (:start this) (* i (:step this))) notFound)
    )

    #_override
    (defn #_"int" Counted'''count--LongChunk [#_"LongChunk" this]
        (:count this)
    )

    #_override
    (defn #_"LongChunk" IChunk'''dropFirst--LongChunk [#_"LongChunk" this]
        (when (< 1 (:count this)) => (throw (IllegalStateException. "dropFirst of empty chunk"))
            (LongChunk'new (+ (:start this) (:step this)), (:step this), (dec (:count this)))
        )
    )

    #_override
    (defn #_"Object" IChunk'''reduce--LongChunk [#_"LongChunk" this, #_"IFn" f, #_"Object" r]
        (loop-when [r r #_"long" x (:start this) #_"int" i 0] (< i (:count this)) => r
            (let-when-not [r (.invoke f, r, x)] (RT'isReduced r) => r
                (recur r (+ x (:step this)) (inc i))
            )
        )
    )
)

;;;
 ; Implements the special common case of a finite range based on long start, end, and step.
 ;;
(class-ns LongRange
    (def- #_"int" LongRange'CHUNK_SIZE 32)

    (defn- #_"LongRangeBoundsCheck" LongRange'positiveStep [#_"long" end]
        (reify LongRangeBoundsCheck
            #_override
            (#_"boolean" exceededBounds [#_"LongRangeBoundsCheck" _self, #_"long" val]
                (<= end val)
            )
        )
    )

    (defn- #_"LongRangeBoundsCheck" LongRange'negativeStep [#_"long" end]
        (reify LongRangeBoundsCheck
            #_override
            (#_"boolean" exceededBounds [#_"LongRangeBoundsCheck" _self, #_"long" val]
                (<= val end)
            )
        )
    )

    (defn- #_"LongRange" LongRange'new
        ([#_"long" start, #_"long" end, #_"long" step, #_"LongRangeBoundsCheck" boundsCheck]
            (LongRange'new start, end, step, boundsCheck, nil, nil)
        )
        ([#_"long" start, #_"long" end, #_"long" step, #_"LongRangeBoundsCheck" boundsCheck, #_"LongChunk" chunk, #_"ISeq" chunkNext]
            (LongRange'new nil, start, end, step, boundsCheck, chunk, chunkNext)
        )
        ([#_"IPersistentMap" meta, #_"long" start, #_"long" end, #_"long" step, #_"LongRangeBoundsCheck" boundsCheck, #_"LongChunk" chunk, #_"ISeq" chunkNext]
            (merge (ASeq'new meta)
                (hash-map
                    ;; Invariants guarantee this is never an empty or infinite seq
                    #_"long" :start start
                    #_"long" :end end
                    #_"long" :step step
                    #_"LongRangeBoundsCheck" :boundsCheck boundsCheck

                    #_volatile #_"LongChunk" :_chunk chunk ;; lazy
                    #_volatile #_"ISeq" :_chunkNext chunkNext ;; lazy
                    #_volatile #_"ISeq" :_next nil ;; cached
                )
            )
        )
    )

    (defn #_"ISeq" LongRange'create-1 [#_"long" end]
        (when (< 0 end) => PersistentList'EMPTY
            (LongRange'new 0, end, 1, (LongRange'positiveStep end))
        )
    )

    (defn #_"ISeq" LongRange'create-2 [#_"long" start, #_"long" end]
        (when (< start end) => PersistentList'EMPTY
            (LongRange'new start, end, 1, (LongRange'positiveStep end))
        )
    )

    (declare Repeat'create-1)

    (defn #_"ISeq" LongRange'create-3 [#_"long" start, #_"long" end, #_"long" step]
        (cond
            (pos? step) (if (< start end) (LongRange'new start, end, step, (LongRange'positiveStep end)) PersistentList'EMPTY)
            (neg? step) (if (< end start) (LongRange'new start, end, step, (LongRange'negativeStep end)) PersistentList'EMPTY)
            :else       (if (= start end) PersistentList'EMPTY (Repeat'create-1 start))
        )
    )

    #_override
    (defn #_"LongRange" IObj'''withMeta--LongRange [#_"LongRange" this, #_"IPersistentMap" meta]
        (when-not (= meta (:_meta this)) => this
            (LongRange'new meta, (:start this), (:end this), (:step this), (:boundsCheck this), (:_chunk this), (:_chunkNext this))
        )
    )

    ;; fallback count mechanism for pathological cases
    ;; returns either exact count or CHUNK_SIZE+1
    #_method
    (defn #_"long" LongRange''steppingCount [#_"LongRange" this, #_"long" start, #_"long" end, #_"long" step]
        (loop-when [#_"long" s start #_"long" n 1] (<= n LongRange'CHUNK_SIZE) => n
            (let [[s n]
                    (try
                        (let [s (Numbers'add-2ll s, step)]
                            (if (.exceededBounds (:boundsCheck this), s)
                                [nil n]
                                [s (inc n)]
                            )
                        )
                        (catch ArithmeticException _
                            [nil n]
                        )
                    )]
                (recur-if (some? s) [s n] => n)
            )
        )
    )

    ;; returns exact size of remaining items OR throws ArithmeticException for overflow case
    #_method
    (defn #_"long" LongRange''rangeCount [#_"LongRange" this, #_"long" start, #_"long" end, #_"long" step]
        ;; (1) count = ceiling ((end - start) / step)
        ;; (2) ceiling(a/b) = (a+b+o)/b where o=-1 for positive stepping and +1 for negative stepping
        ;; thus: count = end - start + step + o / step
        (/ (Numbers'add-2ll (Numbers'add-2ll (Numbers'minus-2ll end, start), step), (if (pos? (:step this)) -1 1)) step)
    )

    #_override
    (defn #_"int" Counted'''count--LongRange [#_"LongRange" this]
        (try
            (let [#_"long" n (LongRange''rangeCount this, (:start this), (:end this), (:step this))]
                (when (<= n Integer/MAX_VALUE) => (Numbers'throwIntOverflow)
                    (int n)
                )
            )
            (catch ArithmeticException _
                ;; rare case from large range or step, fall back to iterating and counting
                (let [#_"long" n
                        (loop-when-recur [#_"Iterator" it (.iterator this) n 0] (.hasNext it) [it (inc n)] => n
                            (.next it)
                        )]
                    (when (<= n Integer/MAX_VALUE) => (Numbers'throwIntOverflow)
                        (int n)
                    )
                )
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--LongRange [#_"LongRange" this]
        (:start this)
    )

    #_method
    (defn #_"void" LongRange''forceChunk [#_"LongRange" this]
        (when (nil? (:_chunk this))
            (let [#_"long" n
                    (try
                        (LongRange''rangeCount this, (:start this), (:end this), (:step this))
                        (catch ArithmeticException e
                            ;; size of total range is > Long.MAX_VALUE, so must step to count
                            ;; this only happens in pathological range cases like:
                            ;; (range -9223372036854775808 9223372036854775807 9223372036854775807)
                            (LongRange''steppingCount this, (:start this), (:end this), (:step this))
                        )
                    )]
                (if (< LongRange'CHUNK_SIZE n)
                    ;; not last chunk
                    (let [#_"long" nextStart (+ (:start this) (* (:step this) LongRange'CHUNK_SIZE))] ;; cannot overflow, must be < end
                        (§ set! (:_chunkNext this) (LongRange'new nextStart, (:end this), (:step this), (:boundsCheck this)))
                        (§ set! (:_chunk this) (LongChunk'new (:start this), (:step this), LongRange'CHUNK_SIZE))
                    )
                    ;; last chunk
                    (§ set! (:_chunk this) (LongChunk'new (:start this), (:step this), (int n))) ;; n must be <= CHUNK_SIZE
                )
            )
        )
        nil
    )

    #_override
    (defn #_"ISeq" ISeq'''next--LongRange [#_"LongRange" this]
        (let-when [#_"ISeq" _next (:_next this)] (nil? _next) => _next
            (LongRange''forceChunk this)
            (when (< 1 (.count (:_chunk this))) => (.chunkedNext this)
                (let [#_"LongChunk" _rest (.dropFirst (:_chunk this))]
                    (§ set! (:_next this) (LongRange'new (LongChunk''first _rest), (:end this), (:step this), (:boundsCheck this), _rest, (:_chunkNext this)))
                )
            )
        )
    )

    #_override
    (defn #_"IChunk" IChunkedSeq'''chunkedFirst--LongRange [#_"LongRange" this]
        (LongRange''forceChunk this)
        (:_chunk this)
    )

    #_override
    (defn #_"ISeq" IChunkedSeq'''chunkedNext--LongRange [#_"LongRange" this]
        (.seq (.chunkedMore this))
    )

    #_override
    (defn #_"ISeq" IChunkedSeq'''chunkedMore--LongRange [#_"LongRange" this]
        (LongRange''forceChunk this)
        (or (:_chunkNext this) PersistentList'EMPTY)
    )

    #_override
    (defn #_"Object" IReduce'''reduce--LongRange [#_"LongRange" this, #_"IFn" f]
        (loop [#_"Object" r (:start this) #_"long" n r]
            (let-when-not [n (+ n (:step this))] (.exceededBounds (:boundsCheck this), n) => r
                (let-when-not [r (.invoke f, r, n)] (RT'isReduced r) => (.deref (cast Reduced r))
                    (recur r n)
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--LongRange [#_"LongRange" this, #_"IFn" f, #_"Object" r]
        (loop [r r #_"long" n (:start this)]
            (let-when-not [r (.invoke f, r, n)] (RT'isReduced r) => (.deref (cast Reduced r))
                (let-when-not [n (+ n (:step this))] (.exceededBounds (:boundsCheck this), n) => r
                    (recur r n)
                )
            )
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---LongRange [#_"LongRange" this]
        (§ reify Iterator
            [#_mutable #_"long" n (:start this)
             #_mutable #_"boolean" m true]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                m
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (when m => (throw (NoSuchElementException.))
                    (let [_ n]
                        (try
                            (update! n Numbers'add-2ll (:step this))
                            (set! m (not (.exceededBounds (:boundsCheck this), n)))
                            (catch ArithmeticException e
                                (set! m false)
                            )
                        )
                        _
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.MethodImplCache

(class-ns Entry
    (defn #_"Entry" Entry'new [#_"Class" c, #_"IFn" fn]
        (hash-map
            #_"Class" :c c
            #_"IFn" :fn fn
        )
    )
)

(class-ns MethodImplCache
    (defn- #_"MethodImplCache" MethodImplCache'init [#_"IPersistentMap" protocol, #_"Keyword" methodk, #_"int" shift, #_"int" mask, #_"Object[]" table, #_"Map" map]
        (hash-map
            #_"IPersistentMap" :protocol protocol
            #_"Keyword" :methodk methodk
            #_"int" :shift shift
            #_"int" :mask mask
            #_"Object[]" :table table ;; [class, entry. class, entry ...]
            #_"Map" :map map

            #_mutable #_"Entry" :mre nil
        )
    )

    (defn #_"MethodImplCache" MethodImplCache'new
        ([#_"IPersistentMap" protocol, #_"Keyword" methodk]
            (MethodImplCache'new protocol, methodk, 0, 0, RT'EMPTY_ARRAY)
        )
        ([#_"IPersistentMap" protocol, #_"Keyword" methodk, #_"int" shift, #_"int" mask, #_"Object[]" table]
            (MethodImplCache'init protocol, methodk, shift, mask, table, nil)
        )
        ([#_"IPersistentMap" protocol, #_"Keyword" methodk, #_"Map" map]
            (MethodImplCache'init protocol, methodk, 0, 0, nil, map)
        )
    )

    #_method
    (defn #_"IFn" MethodImplCache''findFnFor [#_"MethodImplCache" this, #_"Class" c]
        (if (some? (:map this))
            (let [#_"Entry" e (cast Entry (.get (:map this), c))]
                (§ set! (:mre this) e)
                (when (some? e) (:fn e))
            )
            (let [#_"int" idx (<< (& (>> (Util'hash c) (:shift this)) (:mask this)) 1)]
                (when (and (< idx (alength (:table this))) (= (aget (:table this) idx) c))
                    (let [#_"Entry" e (cast Entry (aget (:table this) (inc idx)))]
                        (§ set! (:mre this) e)
                        (when (some? e) (:fn e))
                    )
                )
            )
        )
    )

    #_method
    (defn #_"IFn" MethodImplCache''fnFor [#_"MethodImplCache" this, #_"Class" c]
        (let [#_"Entry" last (:mre this)]
            (if (and (some? last) (= (:c last) c)) (:fn last) (MethodImplCache''findFnFor this, c))
        )
    )
)
)

(java-ns cloiure.lang.MultiFn

(class-ns MultiFn
    (def #_"Var" MultiFn'assoc (§ soon RT'var "cloiure.core", "assoc"))
    (def #_"Var" MultiFn'dissoc (§ soon RT'var "cloiure.core", "dissoc"))
    (def #_"Var" MultiFn'isa (§ soon RT'var "cloiure.core", "isa?"))
    (def #_"Var" MultiFn'parents (§ soon RT'var "cloiure.core", "parents"))

    (defn #_"MultiFn" MultiFn'new [#_"String" name, #_"IFn" dispatchFn, #_"Object" defaultDispatchVal, #_"IDeref" hierarchy]
        (merge (AFn'new)
            (hash-map
                #_"String" :name name
                #_"IFn" :dispatchFn dispatchFn
                #_"Object" :defaultDispatchVal defaultDispatchVal
                #_"IDeref" :hierarchy hierarchy

                #_"ReentrantReadWriteLock" :rw (ReentrantReadWriteLock.)

                #_volatile #_"IPersistentMap" :methodTable PersistentHashMap'EMPTY
                #_volatile #_"IPersistentMap" :preferTable PersistentHashMap'EMPTY
                #_volatile #_"IPersistentMap" :methodCache PersistentHashMap'EMPTY
                #_volatile #_"Object" :cachedHierarchy nil
            )
        )
    )

    #_method
    (defn #_"MultiFn" MultiFn''reset [#_"MultiFn" this]
        (.lock (.writeLock (:rw this)))
        (try
            (§ set! (:methodTable this) PersistentHashMap'EMPTY)
            (§ set! (:methodCache this) PersistentHashMap'EMPTY)
            (§ set! (:preferTable this) PersistentHashMap'EMPTY)
            (§ set! (:cachedHierarchy this) nil)
            this
            (finally
                (.unlock (.writeLock (:rw this)))
            )
        )
    )

    #_method
    (defn- #_"IPersistentMap" MultiFn''resetCache [#_"MultiFn" this]
        (.lock (.writeLock (:rw this)))
        (try
            (§ set! (:methodCache this) (:methodTable this))
            (§ set! (:cachedHierarchy this) (.deref (:hierarchy this)))
            (:methodCache this)
            (finally
                (.unlock (.writeLock (:rw this)))
            )
        )
    )

    #_method
    (defn #_"MultiFn" MultiFn''addMethod [#_"MultiFn" this, #_"Object" dispatchVal, #_"IFn" method]
        (.lock (.writeLock (:rw this)))
        (try
            (let [_ (§ update! (:methodTable this) #(.assoc %, dispatchVal, method))]
                (MultiFn''resetCache this)
                this
            )
            (finally
                (.unlock (.writeLock (:rw this)))
            )
        )
    )

    #_method
    (defn #_"MultiFn" MultiFn''removeMethod [#_"MultiFn" this, #_"Object" dispatchVal]
        (.lock (.writeLock (:rw this)))
        (try
            (let [_ (§ update! (:methodTable this) #(.without %, dispatchVal))]
                (MultiFn''resetCache this)
                this
            )
            (finally
                (.unlock (.writeLock (:rw this)))
            )
        )
    )

    #_method
    (defn- #_"boolean" MultiFn''prefers [#_"MultiFn" this, #_"Object" x, #_"Object" y]
        (or
            (let [#_"IPersistentSet" xprefs (cast IPersistentSet (.valAt (:preferTable this), x))]
                (and (some? xprefs) (.contains xprefs, y))
            )
            (loop-when [#_"ISeq" ps (RT'seq (.invoke MultiFn'parents, y))] (some? ps) => false
                (or (MultiFn''prefers this, x, (.first ps)) (recur (.next ps)))
            )
            (loop-when [#_"ISeq" ps (RT'seq (.invoke MultiFn'parents, x))] (some? ps) => false
                (or (MultiFn''prefers this, (.first ps), y) (recur (.next ps)))
            )
        )
    )

    #_method
    (defn #_"MultiFn" MultiFn''preferMethod [#_"MultiFn" this, #_"Object" dispatchValX, #_"Object" dispatchValY]
        (.lock (.writeLock (:rw this)))
        (try
            (when (MultiFn''prefers this, dispatchValY, dispatchValX)
                (throw (IllegalStateException. (str "Preference conflict in multimethod '" (:name this) "': " dispatchValY " is already preferred to " dispatchValX)))
            )
            (let [_ (§ update! (:preferTable this) #(.assoc %, dispatchValX, (RT'conj (cast IPersistentCollection (RT'get %, dispatchValX, PersistentHashSet'EMPTY)), dispatchValY)))]
                (MultiFn''resetCache this)
                this
            )
            (finally
                (.unlock (.writeLock (:rw this)))
            )
        )
    )

    #_method
    (defn- #_"boolean" MultiFn''isA [#_"MultiFn" this, #_"Object" x, #_"Object" y]
        (RT'booleanCast-1o (.invoke MultiFn'isa, (.deref (:hierarchy this)), x, y))
    )

    #_method
    (defn- #_"boolean" MultiFn''dominates [#_"MultiFn" this, #_"Object" x, #_"Object" y]
        (or (MultiFn''prefers this, x, y) (MultiFn''isA this, x, y))
    )

    #_method
    (defn- #_"IFn" MultiFn''findAndCacheBestMethod [#_"MultiFn" this, #_"Object" dispatchVal]
        (.lock (.readLock (:rw this)))
        (let [#_"IPersistentMap" mt (:methodTable this) #_"IPersistentMap" pt (:preferTable this) #_"Object" ch (:cachedHierarchy this)
              #_"Object" bestValue
                (try
                    (let [#_"Iterator" it (.iterator (:methodTable this))
                            #_"Map$Entry" bestEntry
                            (loop-when [bestEntry nil] (.hasNext it) => bestEntry
                                (let-when [#_"Map$Entry" e (cast Map$Entry (.next it))] (MultiFn''isA this, dispatchVal, (.getKey e)) => (recur bestEntry)
                                    (let [bestEntry
                                            (when (or (nil? bestEntry) (MultiFn''dominates this, (.getKey e), (.getKey bestEntry))) => bestEntry
                                                e
                                            )]
                                        (when-not (MultiFn''dominates this, (.getKey bestEntry), (.getKey e))
                                            (throw (IllegalArgumentException. (str "Multiple methods in multimethod '" (:name this) "' match dispatch value: " dispatchVal " -> " (.getKey e) " and " (.getKey bestEntry) ", and neither is preferred")))
                                        )
                                        (recur bestEntry)
                                    )
                                )
                            )]
                        (if (some? bestEntry) (.getValue bestEntry) (.valAt (:methodTable this), (:defaultDispatchVal this)))
                    )
                    (finally
                        (.unlock (.readLock (:rw this)))
                    )
                )]
            (when (some? bestValue)
                ;; ensure basis has stayed stable throughout, else redo
                (.lock (.writeLock (:rw this)))
                (try
                    (if (and (= mt (:methodTable this)) (= pt (:preferTable this)) (= ch (:cachedHierarchy this)) (= (:cachedHierarchy this) (.deref (:hierarchy this))))
                        (do
                            ;; place in cache
                            (§ update! (:methodCache this) #(.assoc %, dispatchVal, bestValue))
                            (cast IFn bestValue)
                        )
                        (do
                            (MultiFn''resetCache this)
                            (MultiFn''findAndCacheBestMethod this, dispatchVal)
                        )
                    )
                    (finally
                        (.unlock (.writeLock (:rw this)))
                    )
                )
            )
        )
    )

    #_method
    (defn #_"IFn" MultiFn''getMethod [#_"MultiFn" this, #_"Object" dispatchVal]
        (when-not (= (:cachedHierarchy this) (.deref (:hierarchy this)))
            (MultiFn''resetCache this)
        )
        (let [#_"IFn" targetFn (cast IFn (.valAt (:methodCache this), dispatchVal))]
            (or targetFn (MultiFn''findAndCacheBestMethod this, dispatchVal))
        )
    )

    #_method
    (defn- #_"IFn" MultiFn''getFn [#_"MultiFn" this, #_"Object" dispatchVal]
        (let [#_"IFn" targetFn (MultiFn''getMethod this, dispatchVal)]
            (or targetFn (throw (IllegalArgumentException. (str "No method in multimethod '" (:name this) "' for dispatch value: " dispatchVal))))
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-1--MultiFn [#_"MultiFn" this]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this))))
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--MultiFn [#_"MultiFn" this, #_"Object" arg1]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1)), arg1
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2)), arg1, arg2
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-4--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3)), arg1, arg2, arg3
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-5--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4)), arg1, arg2, arg3, arg4
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-6--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5)), arg1, arg2, arg3, arg4, arg5
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-7--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6)), arg1, arg2, arg3, arg4, arg5, arg6
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-8--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7)), arg1, arg2, arg3, arg4, arg5, arg6, arg7
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-9--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-10--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-11--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-12--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-13--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-14--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-15--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-16--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-17--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-18--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-19--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-20--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-21--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-22--MultiFn [#_"MultiFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20 & #_"Object..." args]
        (.invoke (MultiFn''getFn this, (.invoke (:dispatchFn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20, args)), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20,
            args
        )
    )
)
)

(java-ns cloiure.lang.Namespace

(class-ns Namespace
    (def #_"ConcurrentHashMap<Symbol, Namespace>" Namespace'namespaces (ConcurrentHashMap.))

    (defn #_"Namespace" Namespace'new [#_"Symbol" name]
        (hash-map
            #_mutable #_"IPersistentMap" :_meta (.meta name)
            #_"Symbol" :name name

            #_"AtomicReference<IPersistentMap>" :mappings (AtomicReference. RT'DEFAULT_IMPORTS)
            #_"AtomicReference<IPersistentMap>" :aliases (AtomicReference. (RT'map))
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--Namespace [#_"Namespace" this]
        (§ sync this
            (:_meta this)
        )
    )

    #_override
    (defn #_"IPersistentMap" IReference'''alterMeta--Namespace [#_"Namespace" this, #_"IFn" alter, #_"ISeq" args]
        (§ sync this
            (§ update! (:_meta this) #(cast IPersistentMap (.applyTo alter, (Cons'new %, args))))
        )
    )

    #_override
    (defn #_"IPersistentMap" IReference'''resetMeta--Namespace [#_"Namespace" this, #_"IPersistentMap" m]
        (§ sync this
            (§ set! (:_meta this) m)
        )
    )

    #_foreign
    (defn #_"String" toString---Namespace [#_"Namespace" this]
        (.toString (:name this))
    )

    (defn #_"ISeq" Namespace'all []
        (RT'seq (.values Namespace'namespaces))
    )

    #_method
    (defn #_"Symbol" Namespace''getName [#_"Namespace" this]
        (:name this)
    )

    #_method
    (defn #_"IPersistentMap" Namespace''getMappings [#_"Namespace" this]
        (.get (:mappings this))
    )

    #_method
    (defn- #_"void" Namespace''warnOrFailOnReplace [#_"Namespace" this, #_"Symbol" sym, #_"Object" o, #_"Var" var]
        (or
            (when (instance? Var o)
                (let [#_"Namespace" ns (:ns (cast Var o))]
                    (when-not (or (= ns this) (= (:ns var) RT'CLOIURE_NS)) => :ok
                        (when-not (= ns RT'CLOIURE_NS)
                            (throw (IllegalStateException. (str sym " already refers to: " o " in namespace: " (:name this))))
                        )
                    )
                )
            )
            (.println (RT'errPrintWriter), (str "WARNING: " sym " already refers to: " o " in namespace: " (:name this) ", being replaced by: " var))
        )
        nil
    )

    (declare Var'new)

    #_method
    (defn #_"Var" Namespace''intern [#_"Namespace" this, #_"Symbol" sym]
        (when (nil? (:ns sym)) => (throw (IllegalArgumentException. "Can't intern namespace-qualified symbol"))
            (let [[#_"IPersistentMap" m #_"Object" o #_"Var" v]
                    (loop [v nil]
                        (let-when [m (Namespace''getMappings this) o (.valAt m, sym)] (nil? o) => [m o v]
                            (let [v (or v (Var'new this, sym))]
                                (.compareAndSet (:mappings this), m, (.assoc m, sym, v))
                                (recur v)
                            )
                        )
                    )]
                (when-not (and (instance? Var o) (= (:ns (cast Var o)) this)) => (cast Var o)
                    (let [v (or v (Var'new this, sym))]
                        (Namespace''warnOrFailOnReplace this, sym, o, v)
                        (loop-when-recur m (not (.compareAndSet (:mappings this), m, (.assoc m, sym, v))) (Namespace''getMappings this))
                        v
                    )
                )
            )
        )
    )

    #_method
    (defn #_"Var" Namespace''referenceVar [#_"Namespace" this, #_"Symbol" sym, #_"Var" var]
        (when (nil? (:ns sym)) => (throw (IllegalArgumentException. "Can't intern namespace-qualified symbol"))
            (let [[#_"IPersistentMap" m #_"Object" o]
                    (loop []
                        (let-when [m (Namespace''getMappings this) o (.valAt m, sym)] (nil? o) => [m o]
                            (.compareAndSet (:mappings this), m, (.assoc m, sym, var))
                            (recur)
                        )
                    )]
                (when-not (= o var)
                    (Namespace''warnOrFailOnReplace this, sym, o, var)
                    (loop-when-recur m (not (.compareAndSet (:mappings this), m, (.assoc m, sym, var))) (Namespace''getMappings this))
                )
                var
            )
        )
    )

    (defn #_"boolean" Namespace'areDifferentInstancesOfSameClassName [#_"Class" cls1, #_"Class" cls2]
        (and (not= cls1 cls2) (.equals (.getName cls1), (.getName cls2)))
    )

    #_method
    (defn #_"Class" Namespace''referenceClass [#_"Namespace" this, #_"Symbol" sym, #_"Class" cls]
        (when (nil? (:ns sym)) => (throw (IllegalArgumentException. "Can't intern namespace-qualified symbol"))
            (let [#_"Class" c
                    (loop []
                        (let [#_"IPersistentMap" m (Namespace''getMappings this) c (cast Class (.valAt m, sym))]
                            (when (or (nil? c) (Namespace'areDifferentInstancesOfSameClassName c, cls)) => c
                                (.compareAndSet (:mappings this), m, (.assoc m, sym, cls))
                                (recur)
                            )
                        )
                    )]
                (when (= c cls) => (throw (IllegalStateException. (str sym " already refers to: " c " in namespace: " (:name this))))
                    c
                )
            )
        )
    )

    #_method
    (defn #_"void" Namespace''unmap [#_"Namespace" this, #_"Symbol" sym]
        (when (nil? (:ns sym)) => (throw (IllegalArgumentException. "Can't unintern namespace-qualified symbol"))
            (loop-when-recur [#_"IPersistentMap" m (Namespace''getMappings this)] (.containsKey m, sym) [(Namespace''getMappings this)]
                (.compareAndSet (:mappings this), m, (.without m, sym))
            )
        )
        nil
    )

    #_method
    (defn #_"Class" Namespace''importClass [#_"Namespace" this, #_"Class" cls]
        (let [#_"String" s (.getName cls)]
            (Namespace''referenceClass this, (Symbol'intern (.substring s, (inc (.lastIndexOf s, (int \.))))), cls)
        )
    )

    #_method
    (defn #_"Var" Namespace''refer [#_"Namespace" this, #_"Symbol" sym, #_"Var" var]
        (Namespace''referenceVar this, sym, var)
    )

    (defn #_"Namespace" Namespace'findOrCreate [#_"Symbol" name]
        (or (.get Namespace'namespaces, name)
            (let [#_"Namespace" ns (Namespace'new name)]
                (or (.putIfAbsent Namespace'namespaces, name, ns) ns)
            )
        )
    )

    (defn #_"Namespace" Namespace'remove [#_"Symbol" name]
        (when (= name (:name RT'CLOIURE_NS))
            (throw (IllegalArgumentException. "Cannot remove cloiure namespace"))
        )
        (.remove Namespace'namespaces, name)
    )

    (defn #_"Namespace" Namespace'find [#_"Symbol" name]
        (.get Namespace'namespaces, name)
    )

    #_method
    (defn #_"Object" Namespace''getMapping [#_"Namespace" this, #_"Symbol" name]
        (.valAt (.get (:mappings this)), name)
    )

    #_method
    (defn #_"Var" Namespace''findInternedVar [#_"Namespace" this, #_"Symbol" symbol]
        (let [#_"Object" o (.valAt (.get (:mappings this)), symbol)]
            (when (and (some? o) (instance? Var o) (= (:ns (cast Var o)) this))
                (cast Var o)
            )
        )
    )

    #_method
    (defn #_"IPersistentMap" Namespace''getAliases [#_"Namespace" this]
        (.get (:aliases this))
    )

    #_method
    (defn #_"Namespace" Namespace''lookupAlias [#_"Namespace" this, #_"Symbol" alias]
        (cast Namespace (.valAt (Namespace''getAliases this), alias))
    )

    #_method
    (defn #_"void" Namespace''addAlias [#_"Namespace" this, #_"Symbol" alias, #_"Namespace" ns]
        (when (and (some? alias) (some? ns)) => (throw (NullPointerException. "Expecting Symbol + Namespace"))
            (let [#_"IPersistentMap" m
                    (loop-when-recur [m (Namespace''getAliases this)] (not (.containsKey m, alias)) [(Namespace''getAliases this)] => m
                        (.compareAndSet (:aliases this), m, (.assoc m, alias, ns))
                    )]
                ;; you can rebind an alias, but only to the initially-aliased namespace
                (when-not (.equals (.valAt m, alias), ns)
                    (throw (IllegalStateException. (str "Alias " alias " already exists in namespace " (:name this) ", aliasing " (.valAt m, alias))))
                )
            )
        )
        nil
    )

    #_method
    (defn #_"void" Namespace''removeAlias [#_"Namespace" this, #_"Symbol" alias]
        (loop-when-recur [#_"IPersistentMap" m (Namespace''getAliases this)] (.containsKey m, alias) [(Namespace''getAliases this)]
            (.compareAndSet (:aliases this), m, (.without m, alias))
        )
        nil
    )
)
)

(java-ns cloiure.lang.PersistentArrayMap

(class-ns MSeq
    (defn #_"MSeq" MSeq'new
        ([#_"Object[]" array, #_"int" i] (MSeq'new nil, array, i))
        ([#_"IPersistentMap" meta, #_"Object[]" array, #_"int" i]
            (merge (ASeq'new meta)
                (hash-map
                    #_"Object[]" :array array
                    #_"int" :i i
                )
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--MSeq [#_"MSeq" this]
        (MapEntry'create (aget (:array this) (:i this)), (aget (:array this) (inc (:i this))))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--MSeq [#_"MSeq" this]
        (when (< (+ (:i this) 2) (alength (:array this)))
            (MSeq'new (:array this), (+ (:i this) 2))
        )
    )

    #_override
    (defn #_"int" Counted'''count--MSeq [#_"MSeq" this]
        (/ (- (alength (:array this)) (:i this)) 2)
    )

    #_override
    (defn #_"MSeq" IObj'''withMeta--MSeq [#_"MSeq" this, #_"IPersistentMap" meta]
        (MSeq'new meta, (:array this), (:i this))
    )
)

(class-ns MIter
    (defn #_"Iterator" MIter'new [#_"Object[]" a, #_"IFn" f]
        (§ reify Iterator
            [#_mutable #_"int" i -2]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (< (+ i 2) (alength a))
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (update! i + 2)
                (try
                    (.invoke f, (aget a i), (aget a (inc i)))
                    (catch IndexOutOfBoundsException _
                        (throw (NoSuchElementException.))
                    )
                )
            )
        )
    )
)

;;;
 ; Simple implementation of persistent map on an array.
 ;
 ; Note that instances of this class are constant values, i.e. add/remove etc return new values.
 ; Copies array on every change, so only appropriate for _very_small_ maps. nil keys and values are
 ; ok, but you won't be able to distinguish a nil value via valAt, use contains/entryAt for that.
 ;;
(class-ns PersistentArrayMap
    (def #_"int" PersistentArrayMap'HASHTABLE_THRESHOLD 16)

    (defn #_"PersistentArrayMap" PersistentArrayMap'new
        ([] (PersistentArrayMap'new (make-array Object 0)))
        ;; This ctor captures/aliases the passed array, so do not modify it later.
        ([#_"Object[]" init] (PersistentArrayMap'new nil, init))
        ([#_"IPersistentMap" meta, #_"Object[]" init]
            (merge (APersistentMap'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"Object[]" :array init
                )
            )
        )
    )

    (def #_"PersistentArrayMap" PersistentArrayMap'EMPTY (PersistentArrayMap'new))

    (defn #_"IPersistentMap" PersistentArrayMap'create [#_"Map" other]
        (let [#_"Iterator" it (.iterator (.entrySet other))]
            (loop-when [#_"ITransientMap" ret (.asTransient PersistentArrayMap'EMPTY)] (.hasNext it) => (.persistent ret)
                (let [#_"Map$Entry" e (cast Map$Entry (.next it))]
                    (recur (.assoc ret, (.getKey e), (.getValue e)))
                )
            )
        )
    )

    #_override
    (defn #_"PersistentArrayMap" IObj'''withMeta--PersistentArrayMap [#_"PersistentArrayMap" this, #_"IPersistentMap" meta]
        (PersistentArrayMap'new meta, (:array this))
    )

    #_method
    (defn #_"PersistentArrayMap" PersistentArrayMap''create [#_"PersistentArrayMap" this & #_"Object..." init]
        (PersistentArrayMap'new (.meta this), init)
    )

    #_method
    (defn #_"IPersistentMap" PersistentArrayMap''createHT [#_"PersistentArrayMap" this, #_"Object[]" init]
        (PersistentHashMap'create-2 (.meta this), init)
    )

    (defn #_"boolean" PersistentArrayMap'equalKey [#_"Object" k1, #_"Object" k2]
        (if (instance? Keyword k1) (= k1 k2) (Util'equiv-2oo k1, k2))
    )

    (defn #_"PersistentArrayMap" PersistentArrayMap'createWithCheck [#_"Object[]" init]
        (loop-when-recur [#_"int" i 0] (< i (alength init)) [(+ i 2)]
            (loop-when-recur [#_"int" j (+ i 2)] (< j (alength init)) [(+ j 2)]
                (when (PersistentArrayMap'equalKey (aget init i), (aget init j))
                    (throw (IllegalArgumentException. (str "Duplicate key: " (aget init i))))
                )
            )
        )
        (PersistentArrayMap'new init)
    )

    (defn #_"PersistentArrayMap" PersistentArrayMap'createAsIfByAssoc [#_"Object[]" init]
        (when (= (& (alength init) 1) 1)
            (throw (IllegalArgumentException. (str "No value supplied for key: " (aget init (dec (alength init))))))
        )
        ;; If this looks like it is doing busy-work, it is because it is achieving these goals: O(n^2) run time
        ;; like createWithCheck(), never modify init arg, and only allocate memory if there are duplicate keys.
        (let [#_"int" n
                (loop-when [n 0 #_"int" i 0] (< i (alength init)) => n
                    (let [#_"boolean" dup?
                            (loop-when [dup? false #_"int" j 0] (< j i) => dup?
                                (or (PersistentArrayMap'equalKey (aget init i), (aget init j))
                                    (recur dup? (+ j 2))
                                )
                            )]
                        (recur (if dup? n (+ n 2)) (+ i 2))
                    )
                )
              init
                (when (< n (alength init)) => init
                    ;; Create a new shorter array with unique keys, and the last value associated with each key.
                    ;; To behave like assoc, the first occurrence of each key must be used, since its metadata
                    ;; may be different than later equal keys.
                    (let [#_"Object[]" nodups (make-array Object n)
                          #_"int" m
                            (loop-when [m 0 #_"int" i 0] (< i (alength init)) => m
                                (let [#_"boolean" dup?
                                        (loop-when [dup? false #_"int" j 0] (< j m) => dup?
                                            (or (PersistentArrayMap'equalKey (aget init i), (aget nodups j))
                                                (recur dup? (+ j 2))
                                            )
                                        )
                                      m (when-not dup? => m
                                            (let [#_"int" j
                                                    (loop-when [j (- (alength init) 2)] (<= i j) => j
                                                        (if (PersistentArrayMap'equalKey (aget init i), (aget init j))
                                                            j
                                                            (recur (- j 2))
                                                        )
                                                    )]
                                                (aset nodups m (aget init i))
                                                (aset nodups (inc m) (aget init (inc j)))
                                                (+ m 2)
                                            )
                                        )]
                                    (recur m (+ i 2))
                                )
                            )]
                        (when (= m n) => (throw (IllegalArgumentException. (str "Internal error: m=" m)))
                            nodups
                        )
                    )
                )]
            (PersistentArrayMap'new init)
        )
    )

    #_override
    (defn #_"int" Counted'''count--PersistentArrayMap [#_"PersistentArrayMap" this]
        (/ (alength (:array this)) 2)
    )

    #_method
    (defn- #_"int" PersistentArrayMap''indexOfObject [#_"PersistentArrayMap" this, #_"Object" key]
        (let [#_"EquivPred" ep (Util'equivPred key)]
            (loop-when [#_"int" i 0] (< i (alength (:array this))) => -1
                (if (.equiv ep, key, (aget (:array this) i)) i (recur (+ i 2)))
            )
        )
    )

    #_method
    (defn- #_"int" PersistentArrayMap''indexOf [#_"PersistentArrayMap" this, #_"Object" key]
        (when (instance? Keyword key) => (PersistentArrayMap''indexOfObject this, key)
            (loop-when [#_"int" i 0] (< i (alength (:array this))) => -1
                (if (= key (aget (:array this) i)) i (recur (+ i 2)))
            )
        )
    )

    #_override
    (defn #_"boolean" Associative'''containsKey--PersistentArrayMap [#_"PersistentArrayMap" this, #_"Object" key]
        (<= 0 (PersistentArrayMap''indexOf this, key))
    )

    #_override
    (defn #_"IMapEntry" Associative'''entryAt--PersistentArrayMap [#_"PersistentArrayMap" this, #_"Object" key]
        (let-when [#_"int" i (PersistentArrayMap''indexOf this, key)] (<= 0 i)
            (cast IMapEntry (MapEntry'create (aget (:array this) i), (aget (:array this) (inc i))))
        )
    )

    #_override
    (defn #_"IPersistentMap" IPersistentMap'''assocEx--PersistentArrayMap [#_"PersistentArrayMap" this, #_"Object" key, #_"Object" val]
        (let [#_"int" i (PersistentArrayMap''indexOf this, key)]
            (when-not (<= 0 i) => (throw (RuntimeException. "Key already present"))
                ;; didn't have key, grow
                (if (< PersistentArrayMap'HASHTABLE_THRESHOLD (alength (:array this)))
                    (.assocEx (PersistentArrayMap''createHT this, (:array this)), key, val)
                    (let [#_"int" n (alength (:array this)) #_"Object[]" newArray (make-array Object (+ n 2))]
                        (when (pos? n)
                            (System/arraycopy (:array this), 0, newArray, 2, n)
                        )
                        (aset newArray 0 key)
                        (aset newArray 1 val)
                        (PersistentArrayMap''create this, newArray)
                    )
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" IPersistentMap'''assoc--PersistentArrayMap [#_"PersistentArrayMap" this, #_"Object" key, #_"Object" val]
        (let [#_"int" i (PersistentArrayMap''indexOf this, key)]
            (if (<= 0 i) ;; already have key, same-sized replacement
                (if (= (aget (:array this) (inc i)) val) ;; no change, no op
                    this
                    (let [#_"Object[]" newArray (.clone (:array this))]
                        (aset newArray (inc i) val)
                        (PersistentArrayMap''create this, newArray)
                    )
                )
                ;; didn't have key, grow
                (if (< PersistentArrayMap'HASHTABLE_THRESHOLD (alength (:array this)))
                    (.assoc (PersistentArrayMap''createHT this, (:array this)), key, val)
                    (let [#_"int" n (alength (:array this)) #_"Object[]" newArray (make-array Object (+ n 2))]
                        (when (pos? n)
                            (System/arraycopy (:array this), 0, newArray, 0, n)
                        )
                        (aset newArray n key)
                        (aset newArray (inc n) val)
                        (PersistentArrayMap''create this, newArray)
                    )
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" IPersistentMap'''without--PersistentArrayMap [#_"PersistentArrayMap" this, #_"Object" key]
        (let-when [#_"int" i (PersistentArrayMap''indexOf this, key)] (<= 0 i) => this ;; don't have key, no op
            ;; have key, will remove
            (let-when [#_"int" n (- (alength (:array this)) 2)] (pos? n) => (.empty this)
                (let [#_"Object[]" a (make-array Object n)]
                    (System/arraycopy (:array this), 0, a, 0, i)
                    (System/arraycopy (:array this), (+ i 2), a, i, (- n i))
                    (PersistentArrayMap''create this, a)
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" IPersistentCollection'''empty--PersistentArrayMap [#_"PersistentArrayMap" this]
        (cast IPersistentMap (.withMeta PersistentArrayMap'EMPTY, (.meta this)))
    )

    #_override
    (defn #_"Object" ILookup'''valAt-3--PersistentArrayMap [#_"PersistentArrayMap" this, #_"Object" key, #_"Object" notFound]
        (let [#_"int" i (PersistentArrayMap''indexOf this, key)]
            (if (<= 0 i) (aget (:array this) (inc i)) notFound)
        )
    )

    #_override
    (defn #_"Object" ILookup'''valAt-2--PersistentArrayMap [#_"PersistentArrayMap" this, #_"Object" key]
        (.valAt this, key, nil)
    )

    #_method
    (defn #_"int" PersistentArrayMap''capacity [#_"PersistentArrayMap" this]
        (.count this)
    )

    #_foreign
    (defn #_"Iterator" iterator---PersistentArrayMap [#_"PersistentArrayMap" this]
        (MIter'new (:array this), APersistentMap'MAKE_ENTRY)
    )

    #_override
    (defn #_"Iterator" IMapIterable'''keyIterator--PersistentArrayMap [#_"PersistentArrayMap" this]
        (MIter'new (:array this), APersistentMap'MAKE_KEY)
    )

    #_override
    (defn #_"Iterator" IMapIterable'''valIterator--PersistentArrayMap [#_"PersistentArrayMap" this]
        (MIter'new (:array this), APersistentMap'MAKE_VAL)
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--PersistentArrayMap [#_"PersistentArrayMap" this]
        (when (pos? (alength (:array this)))
            (MSeq'new (:array this), 0)
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--PersistentArrayMap [#_"PersistentArrayMap" this]
        (:_meta this)
    )

    #_override
    (defn #_"Object" IKVReduce'''kvreduce--PersistentArrayMap [#_"PersistentArrayMap" this, #_"IFn" f, #_"Object" r]
        (loop-when [r r #_"int" i 0] (< i (alength (:array this))) => r
            (let [r (.invoke f, r, (aget (:array this) i), (aget (:array this) (inc i)))]
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (+ i 2)))
            )
        )
    )

    (declare TransientArrayMap'new)

    #_override
    (defn #_"ITransientMap" IEditableCollection'''asTransient--PersistentArrayMap [#_"PersistentArrayMap" this]
        (TransientArrayMap'new (:array this))
    )
)

(class-ns TransientArrayMap
    (defn #_"TransientArrayMap" TransientArrayMap'new [#_"Object[]" array]
        (let [#_"Object[]" a (make-array Object (Math/max PersistentArrayMap'HASHTABLE_THRESHOLD, (alength array)))
              _ (System/arraycopy array, 0, a, 0, (alength array))]
            (merge (ATransientMap'new)
                (hash-map
                    #_"Object[]" :array a
                    #_"int" :len (alength array)
                    #_volatile #_"Thread" :owner (Thread/currentThread)
                )
            )
        )
    )

    #_method
    (defn- #_"int" TransientArrayMap''indexOf [#_"TransientArrayMap" this, #_"Object" key]
        (loop-when [#_"int" i 0] (< i (:len this)) => -1
            (if (PersistentArrayMap'equalKey (aget (:array this) i), key) i (recur (+ i 2)))
        )
    )

    #_override
    (defn #_"ITransientMap" ATransientMap'''doAssoc--TransientArrayMap [#_"TransientArrayMap" this, #_"Object" key, #_"Object" val]
        (let [#_"int" i (TransientArrayMap''indexOf this, key)]
            (cond (<= 0 i) ;; already have key,
                (do
                    (when-not (= (aget (:array this) (inc i)) val) ;; no change, no op
                        (aset (:array this) (inc i) val)
                    )
                    this
                )
                :else ;; didn't have key, grow
                (if (< (:len this) (alength (:array this)))
                    (let [_ (aset (:array this) (:len this) key) this (update this :len inc)
                          _ (aset (:array this) (:len this) val) this (update this :len inc)]
                        this
                    )
                    (-> (PersistentHashMap'create-1a (:array this)) (.asTransient) (.assoc key, val))
                )
            )
        )
    )

    #_override
    (defn #_"ITransientMap" ATransientMap'''doWithout--TransientArrayMap [#_"TransientArrayMap" this, #_"Object" key]
        (let-when [#_"int" i (TransientArrayMap''indexOf this, key)] (<= 0 i) => this
            ;; have key, will remove
            (when (<= 2 (:len this))
                (aset (:array this) i (aget (:array this) (- (:len this) 2)))
                (aset (:array this) (inc i) (aget (:array this) (- (:len this) 1)))
            )
            (update this :len - 2)
        )
    )

    #_override
    (defn #_"Object" ATransientMap'''doValAt--TransientArrayMap [#_"TransientArrayMap" this, #_"Object" key, #_"Object" notFound]
        (let [#_"int" i (TransientArrayMap''indexOf this, key)]
            (if (<= 0 i) (aget (:array this) (inc i)) notFound)
        )
    )

    #_override
    (defn #_"int" ATransientMap'''doCount--TransientArrayMap [#_"TransientArrayMap" this]
        (/ (:len this) 2)
    )

    #_override
    (defn #_"IPersistentMap" ATransientMap'''doPersistent--TransientArrayMap [#_"TransientArrayMap" this]
        (.ensureEditable this)
        (§ set! (:owner this) nil)
        (let [#_"Object[]" a (make-array Object (:len this))]
            (System/arraycopy (:array this), 0, a, 0, (:len this))
            (PersistentArrayMap'new a)
        )
    )

    #_override
    (defn #_"void" ATransientMap'''ensureEditable--TransientArrayMap [#_"TransientArrayMap" this]
        (when (nil? (:owner this))
            (throw (IllegalAccessError. "Transient used after persistent! call"))
        )
        nil
    )
)
)

(java-ns cloiure.lang.PersistentHashMap

(class-ns HSeq
    (defn- #_"HSeq" HSeq'new [#_"IPersistentMap" meta, #_"INode[]" nodes, #_"int" i, #_"ISeq" s]
        (merge (ASeq'new meta)
            (hash-map
                #_"INode[]" :nodes nodes
                #_"int" :i i
                #_"ISeq" :s s
            )
        )
    )

    (defn- #_"ISeq" HSeq'create-4 [#_"IPersistentMap" meta, #_"INode[]" nodes, #_"int" i, #_"ISeq" s]
        (when (nil? s) => (HSeq'new meta, nodes, i, s)
            (loop-when i (< i (alength nodes))
                (let-when [#_"INode" ai (aget nodes i)] (some? ai) => (recur (inc i))
                    (let-when [s (.nodeSeq ai)] (some? s) => (recur (inc i))
                        (HSeq'new meta, nodes, (inc i), s)
                    )
                )
            )
        )
    )

    (defn #_"ISeq" HSeq'create-1 [#_"INode[]" nodes]
        (HSeq'create-4 nil, nodes, 0, nil)
    )

    #_override
    (defn #_"HSeq" IObj'''withMeta--HSeq [#_"HSeq" this, #_"IPersistentMap" meta]
        (HSeq'new meta, (:nodes this), (:i this), (:s this))
    )

    #_override
    (defn #_"Object" ISeq'''first--HSeq [#_"HSeq" this]
        (.first (:s this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--HSeq [#_"HSeq" this]
        (HSeq'create-4 nil, (:nodes this), (:i this), (.next (:s this)))
    )
)

(class-ns HIter
    (defn #_"Iterator" HIter'new [#_"INode[]" a, #_"IFn" f]
        (§ reify Iterator
            [#_mutable #_"int" i 0
             #_mutable #_"Iterator" it nil]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (loop []
                    (or
                        (when (some? it)
                            (or (.hasNext it)
                                (set! it nil)
                            )
                        )
                        (and (< i (alength a))
                            (let [#_"INode" ai (aget a i)]
                                (update! i inc)
                                (when (some? ai)
                                    (set! it (.iterator ai, f))
                                )
                                (recur)
                            )
                        )
                    )
                )
            )

            #_foreign
            (#_"Object" next [#_"Iterator" self]
                (when (.hasNext self) => (throw (NoSuchElementException.))
                    (.next it)
                )
            )
        )
    )
)

(class-ns NodeIter
    (def- #_"Object" NodeIter'NULL (Object.))

    (defn #_"Iterator" NodeIter'new [#_"Object[]" a, #_"IFn" f]
        (§ reify Iterator
            [#_mutable #_"int" i 0
             #_mutable #_"Object" e NodeIter'NULL
             #_mutable #_"Iterator" it nil]

            #_private
            (#_"boolean" step [_self]
                (loop-when [] (< i (alength a)) => false
                    (let [#_"Object" key (aget a i) #_"Object" nodeOrVal (aget a (inc i)) _ (update! i + 2)]
                        (cond
                            (some? key)
                                (do
                                    (set! e (.invoke f, key, nodeOrVal))
                                    true
                                )
                            (some? nodeOrVal)
                                (let-when [#_"Iterator" it' (.iterator (cast INode nodeOrVal), f)] (and (some? it') (.hasNext it')) => (recur)
                                    (set! it it')
                                    true
                                )
                            :else
                                (recur)
                        )
                    )
                )
            )

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" self]
                (or (not (identical? e NodeIter'NULL)) (some? it) (.step self))
            )

            #_foreign
            (#_"Object" next [#_"Iterator" self]
                (let [#_"Object" e' e]
                    (cond
                        (not (identical? e' NodeIter'NULL))
                            (do
                                (set! e NodeIter'NULL)
                                e'
                            )
                        (some? it)
                            (let [e' (.next it)]
                                (when-not (.hasNext it)
                                    (set! it nil)
                                )
                                e'
                            )
                        (.step self)
                            (.next self)
                        :else
                            (throw (NoSuchElementException.))
                    )
                )
            )
        )
    )
)

(class-ns NodeSeq
    (defn #_"NodeSeq" NodeSeq'new
        ([#_"Object[]" array, #_"int" i] (NodeSeq'new nil, array, i, nil))
        ([#_"IPersistentMap" meta, #_"Object[]" array, #_"int" i, #_"ISeq" s]
            (merge (ASeq'new meta)
                (hash-map
                    #_"Object[]" :array array
                    #_"int" :i i
                    #_"ISeq" :s s
                )
            )
        )
    )

    (defn- #_"ISeq" NodeSeq'create-3 [#_"Object[]" array, #_"int" i, #_"ISeq" s]
        (when (nil? s) => (NodeSeq'new nil, array, i, s)
            (loop-when i (< i (alength array))
                (when (nil? (aget array i)) => (NodeSeq'new nil, array, i, nil)
                    (or
                        (when-let [#_"INode" node (cast INode (aget array (inc i)))]
                            (when-let [s (.nodeSeq node)]
                                (NodeSeq'new nil, array, (+ i 2), s)
                            )
                        )
                        (recur (+ i 2))
                    )
                )
            )
        )
    )

    (defn #_"ISeq" NodeSeq'create-1 [#_"Object[]" array]
        (NodeSeq'create-3 array, 0, nil)
    )

    (defn #_"Object" NodeSeq'kvreduce [#_"Object[]" array, #_"IFn" f, #_"Object" r]
        (loop-when [r r #_"int" i 0] (< i (alength array)) => r
            (let [r (if (some? (aget array i))
                        (.invoke f, r, (aget array i), (aget array (inc i)))
                        (let-when [#_"INode" node (cast INode (aget array (inc i)))] (some? node) => r
                            (.kvreduce node, f, r)
                        )
                    )]
                (when-not (RT'isReduced r) => r
                    (recur r (+ i 2))
                )
            )
        )
    )

    #_override
    (defn #_"NodeSeq" IObj'''withMeta--NodeSeq [#_"NodeSeq" this, #_"IPersistentMap" meta]
        (NodeSeq'new meta, (:array this), (:i this), (:s this))
    )

    #_override
    (defn #_"Object" ISeq'''first--NodeSeq [#_"NodeSeq" this]
        (if (some? (:s this))
            (.first (:s this))
            (MapEntry'create (aget (:array this) (:i this)), (aget (:array this) (inc (:i this))))
        )
    )

    #_override
    (defn #_"ISeq" ISeq'''next--NodeSeq [#_"NodeSeq" this]
        (if (some? (:s this))
            (NodeSeq'create-3 (:array this), (:i this), (.next (:s this)))
            (NodeSeq'create-3 (:array this), (+ (:i this) 2), nil)
        )
    )
)

(class-ns ArrayNode
    (defn #_"ArrayNode" ArrayNode'new [#_"AtomicReference<Thread>" edit, #_"int" count, #_"INode[]" array]
        (hash-map
            #_"AtomicReference<Thread>" :edit edit
            #_"int" :count count
            #_"INode[]" :array array
        )
    )

    (declare BitmapIndexedNode'EMPTY)

    #_override
    (defn #_"INode" INode'''assoc-6--ArrayNode [#_"ArrayNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"Box" addedLeaf]
        (let [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" ai (aget (:array this) i)]
            (if (some? ai)
                (let [#_"INode" node (.assoc ai, (+ shift 5), hash, key, val, addedLeaf)]
                    (when-not (= node ai) => this
                        (ArrayNode'new nil, (:count this), (PersistentHashMap'cloneAndSet-3 (:array this), i, node))
                    )
                )
                (let [#_"INode" node (.assoc BitmapIndexedNode'EMPTY, (+ shift 5), hash, key, val, addedLeaf)]
                    (ArrayNode'new nil, (inc (:count this)), (PersistentHashMap'cloneAndSet-3 (:array this), i, node))
                )
            )
        )
    )

    (declare BitmapIndexedNode'new)

    #_method
    (defn- #_"INode" ArrayNode''pack [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit, #_"int" idx]
        (let [#_"Object[]" a (make-array Object (* 2 (dec (:count this))))
              [#_"int" bitmap #_"int" j]
                (loop-when [bitmap 0 j 1 #_"int" i 0] (< i idx) => [bitmap j]
                    (let [[bitmap j]
                            (when (some? (aget (:array this) i)) => [bitmap j]
                                (aset a j (aget (:array this) i))
                                [(| bitmap (<< 1 i)) (+ j 2)]
                            )]
                        (recur bitmap j (inc i))
                    )
                )
              bitmap
                (loop-when [bitmap bitmap j j #_"int" i (inc idx)] (< i (alength (:array this))) => bitmap
                    (let [[bitmap j]
                            (when (some? (aget (:array this) i)) => [bitmap j]
                                (aset a j (aget (:array this) i))
                                [(| bitmap (<< 1 i)) (+ j 2)]
                            )]
                        (recur bitmap j (inc i))
                    )
                )]
            (BitmapIndexedNode'new edit, bitmap, a)
        )
    )

    #_override
    (defn #_"INode" INode'''without-4--ArrayNode [#_"ArrayNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
        (let-when [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" ai (aget (:array this) i)] (some? ai) => this
            (let-when-not [#_"INode" node (.without ai, (+ shift 5), hash, key)] (= node ai) => this
                (cond
                    (some? node)         (ArrayNode'new nil, (:count this), (PersistentHashMap'cloneAndSet-3 (:array this), i, node))
                    (<= (:count this) 8) (ArrayNode''pack this, nil, i) ;; shrink
                    :else                (ArrayNode'new nil, (dec (:count this)), (PersistentHashMap'cloneAndSet-3 (:array this), i, node))
                )
            )
        )
    )

    #_override
    (defn #_"IMapEntry" INode'''find-4--ArrayNode [#_"ArrayNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
        (let [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" node (aget (:array this) i)]
            (when (some? node)
                (.find node, (+ shift 5), hash, key)
            )
        )
    )

    #_override
    (defn #_"Object" INode'''find-5--ArrayNode [#_"ArrayNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" notFound]
        (let [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" node (aget (:array this) i)]
            (when (some? node) => notFound
                (.find node, (+ shift 5), hash, key, notFound)
            )
        )
    )

    #_override
    (defn #_"ISeq" INode'''nodeSeq--ArrayNode [#_"ArrayNode" this]
        (HSeq'create-1 (:array this))
    )

    #_override
    (defn #_"Iterator" INode'''iterator--ArrayNode [#_"ArrayNode" this, #_"IFn" f]
        (HIter'new (:array this), f)
    )

    #_override
    (defn #_"Object" INode'''kvreduce--ArrayNode [#_"ArrayNode" this, #_"IFn" f, #_"Object" r]
        (let [#_"INode[]" a (:array this)]
            (loop-when [r r #_"int" i 0] (< i (alength a)) => r
                (let-when [#_"INode" node (aget a i)] (some? node) => (recur r (inc i))
                    (let [r (.kvreduce node, f, r)]
                        (when-not (RT'isReduced r) => r
                            (recur r (inc i))
                        )
                    )
                )
            )
        )
    )

    (defn #_"Object" ArrayNode'foldTasks [#_"PersistentVector" tasks, #_"IFn" combinef, #_"IFn" fjtask, #_"IFn" fjfork, #_"IFn" fjjoin]
        (let [#_"int" n (.count tasks)]
            (case n
                0   (.invoke combinef)
                1   (.call (.nth tasks, 0))
                    (let [#_"PersistentVector" t1 (RT'subvec tasks, 0, (quot n 2)) #_"PersistentVector" t2 (RT'subvec tasks, (quot n 2), n)
                          #_"Object" forked (.invoke fjfork, (.invoke fjtask, #(ArrayNode'foldTasks t2, combinef, fjtask, fjfork, fjjoin)))]
                        (.invoke combinef, (ArrayNode'foldTasks t1, combinef, fjtask, fjfork, fjjoin), (.invoke fjjoin, forked))
                    )
            )
        )
    )

    #_override
    (defn #_"Object" INode'''fold--ArrayNode [#_"ArrayNode" this, #_"IFn" combinef, #_"IFn" reducef, #_"IFn" fjtask, #_"IFn" fjfork, #_"IFn" fjjoin]
        (let [#_"INode[]" a (:array this)
              #_"PersistentVector" tasks
                (loop-when [tasks PersistentVector'EMPTY #_"int" i 0] (< i (alength a)) => tasks
                    (let [#_"INode" node (aget a i)
                          tasks
                            (when (some? node) => tasks
                                (.cons tasks, #(.fold node, combinef, reducef, fjtask, fjfork, fjjoin))
                            )]
                        (recur tasks (inc i))
                    )
                )]
            (ArrayNode'foldTasks tasks, combinef, fjtask, fjfork, fjjoin)
        )
    )

    #_method
    (defn- #_"ArrayNode" ArrayNode''ensureEditable [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit]
        (if (= (:edit this) edit)
            this
            (ArrayNode'new edit, (:count this), (.clone (:array this)))
        )
    )

    #_method
    (defn- #_"ArrayNode" ArrayNode''editAndSet [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"INode" node]
        (let [#_"ArrayNode" e (ArrayNode''ensureEditable this, edit)]
            (aset (:array e) i node)
            e
        )
    )

    #_override
    (defn #_"INode" INode'''assoc-7--ArrayNode [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"Box" addedLeaf]
        (let [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" ai (aget (:array this) i)]
            (if (some? ai)
                (let [#_"INode" node (.assoc ai, edit, (+ shift 5), hash, key, val, addedLeaf)]
                    (when-not (= node ai) => this
                        (ArrayNode''editAndSet this, edit, i, node)
                    )
                )
                (-> (ArrayNode''editAndSet this, edit, i, (.assoc BitmapIndexedNode'EMPTY, edit, (+ shift 5), hash, key, val, addedLeaf))
                    (update :count inc)
                )
            )
        )
    )

    #_override
    (defn #_"INode" INode'''without-6--ArrayNode [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Box" removedLeaf]
        (let-when [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" ai (aget (:array this) i)] (some? ai) => this
            (let-when-not [#_"INode" node (.without ai, edit, (+ shift 5), hash, key, removedLeaf)] (= node ai) => this
                (cond
                    (some? node)         (ArrayNode''editAndSet this, edit, i, node)
                    (<= (:count this) 8) (ArrayNode''pack this, edit, i) ;; shrink
                    :else            (-> (ArrayNode''editAndSet this, edit, i, node) (update :count dec))
                )
            )
        )
    )
)

(class-ns BitmapIndexedNode
    (defn #_"BitmapIndexedNode" BitmapIndexedNode'new [#_"AtomicReference<Thread>" edit, #_"int" bitmap, #_"Object[]" array]
        (hash-map
            #_"AtomicReference<Thread>" :edit edit
            #_"int" :bitmap bitmap
            #_"Object[]" :array array
        )
    )

    (def #_"BitmapIndexedNode" BitmapIndexedNode'EMPTY (BitmapIndexedNode'new nil, 0, (object-array 0)))

    #_method
    (defn #_"int" BitmapIndexedNode''index [#_"BitmapIndexedNode" this, #_"int" bit]
        (Integer/bitCount (& (:bitmap this) (dec bit)))
    )

    #_override
    (defn #_"INode" INode'''assoc-6--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"Box" addedLeaf]
        (let [#_"int" bit (PersistentHashMap'bitpos hash, shift) #_"int" idx (BitmapIndexedNode''index this, bit)]
            (if-not (zero? (& (:bitmap this) bit))
                (let [#_"Object" keyOrNull (aget (:array this) (* 2 idx))
                      #_"Object" valOrNode (aget (:array this) (inc (* 2 idx)))
                      _ (cond
                            (nil? keyOrNull)
                                (let [#_"INode" n (.assoc (cast INode valOrNode), (+ shift 5), hash, key, val, addedLeaf)]
                                    (when-not (= n valOrNode)
                                        (PersistentHashMap'cloneAndSet-3 (:array this), (inc (* 2 idx)), n)
                                    )
                                )
                            (Util'equiv-2oo key, keyOrNull)
                                (when-not (= val valOrNode)
                                    (PersistentHashMap'cloneAndSet-3 (:array this), (inc (* 2 idx)), val)
                                )
                            :else
                                (let [_ (§ set! (:val addedLeaf) addedLeaf)]
                                    (PersistentHashMap'cloneAndSet-5 (:array this), (* 2 idx), nil, (inc (* 2 idx)), (PersistentHashMap'createNode-6 (+ shift 5), keyOrNull, valOrNode, hash, key, val))
                                )
                        )]
                    (if (some? _) (BitmapIndexedNode'new nil, (:bitmap this), _) this)
                )
                (let [#_"int" n (Integer/bitCount (:bitmap this))]
                    (if (<= 16 n)
                        (let [#_"INode[]" nodes (make-array #_"INode" Object 32) #_"int" jdx (PersistentHashMap'mask hash, shift)]
                            (aset nodes jdx (.assoc BitmapIndexedNode'EMPTY, (+ shift 5), hash, key, val, addedLeaf))
                            (loop-when [#_"int" j 0 #_"int" i 0] (< i 32)
                                (when-not (= (& (>>> (:bitmap this) i) 1) 0) => (recur j (inc i))
                                    (if (some? (aget (:array this) j))
                                        (aset nodes i (.assoc BitmapIndexedNode'EMPTY, (+ shift 5), (PersistentHashMap'hash (aget (:array this) j)), (aget (:array this) j), (aget (:array this) (inc j)), addedLeaf))
                                        (aset nodes i (cast INode (aget (:array this) (inc j))))
                                    )
                                    (recur (+ j 2) (inc i))
                                )
                            )
                            (ArrayNode'new nil, (inc n), nodes)
                        )
                        (let [#_"Object[]" a (make-array Object (* 2 (inc n)))]
                            (System/arraycopy (:array this), 0, a, 0, (* 2 idx))
                            (aset a (* 2 idx) key)
                            (§ set! (:val addedLeaf) addedLeaf)
                            (aset a (inc (* 2 idx)) val)
                            (System/arraycopy (:array this), (* 2 idx), a, (* 2 (inc idx)), (* 2 (- n idx)))
                            (BitmapIndexedNode'new nil, (| (:bitmap this) bit), a)
                        )
                    )
                )
            )
        )
    )

    #_override
    (defn #_"INode" INode'''without-4--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
        (let-when-not [#_"int" bit (PersistentHashMap'bitpos hash, shift)] (zero? (& (:bitmap this) bit)) => this
            (let [#_"int" i (BitmapIndexedNode''index this, bit) #_"int" ii (* 2 i)
                  #_"Object" keyOrNull (aget (:array this) ii)
                  #_"Object" valOrNode (aget (:array this) (inc ii))]
                (if (some? keyOrNull)
                    (when (Util'equiv-2oo key, keyOrNull) => this
                        ;; TODO: collapse
                        (BitmapIndexedNode'new nil, (bit-xor (:bitmap this) bit), (PersistentHashMap'removePair (:array this), i))
                    )
                    (let [#_"INode" n (.without (cast INode valOrNode), (+ shift 5), hash, key)]
                        (cond
                            (= n valOrNode)
                                this
                            (some? n)
                                (BitmapIndexedNode'new nil, (:bitmap this), (PersistentHashMap'cloneAndSet-3 (:array this), (inc ii), n))
                            (= (:bitmap this) bit)
                                nil
                            :else
                                (BitmapIndexedNode'new nil, (bit-xor (:bitmap this) bit), (PersistentHashMap'removePair (:array this), i))
                        )
                    )
                )
            )
        )
    )

    #_override
    (defn #_"IMapEntry" INode'''find-4--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
        (let-when-not [#_"int" bit (PersistentHashMap'bitpos hash, shift)] (zero? (& (:bitmap this) bit))
            (let [#_"int" i (BitmapIndexedNode''index this, bit)
                  #_"Object" keyOrNull (aget (:array this) (* 2 i))
                  #_"Object" valOrNode (aget (:array this) (inc (* 2 i)))]
                (cond
                    (nil? keyOrNull)                (.find (cast INode valOrNode), (+ shift 5), hash, key)
                    (Util'equiv-2oo key, keyOrNull) (cast IMapEntry (MapEntry'create keyOrNull, valOrNode))
                )
            )
        )
    )

    #_override
    (defn #_"Object" INode'''find-5--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" notFound]
        (let-when-not [#_"int" bit (PersistentHashMap'bitpos hash, shift)] (zero? (& (:bitmap this) bit)) => notFound
            (let [#_"int" i (BitmapIndexedNode''index this, bit)
                  #_"Object" keyOrNull (aget (:array this) (* 2 i))
                  #_"Object" valOrNode (aget (:array this) (inc (* 2 i)))]
                (cond
                    (nil? keyOrNull)                (.find (cast INode valOrNode), (+ shift 5), hash, key, notFound)
                    (Util'equiv-2oo key, keyOrNull) valOrNode
                    :else                           notFound
                )
            )
        )
    )

    #_override
    (defn #_"ISeq" INode'''nodeSeq--BitmapIndexedNode [#_"BitmapIndexedNode" this]
        (NodeSeq'create-1 (:array this))
    )

    #_override
    (defn #_"Iterator" INode'''iterator--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"IFn" f]
        (NodeIter'new (:array this), f)
    )

    #_override
    (defn #_"Object" INode'''kvreduce--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"IFn" f, #_"Object" r]
        (NodeSeq'kvreduce (:array this), f, r)
    )

    #_override
    (defn #_"Object" INode'''fold--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"IFn" combinef, #_"IFn" reducef, #_"IFn" fjtask, #_"IFn" fjfork, #_"IFn" fjjoin]
        (NodeSeq'kvreduce (:array this), reducef, (.invoke combinef))
    )

    #_method
    (defn- #_"BitmapIndexedNode" BitmapIndexedNode''ensureEditable [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit]
        (when-not (= (:edit this) edit) => this
            (let [#_"int" n (Integer/bitCount (:bitmap this)) #_"Object[]" a (make-array Object (* 2 (inc n)))] ;; make room for next assoc
                (System/arraycopy (:array this), 0, a, 0, (* 2 n))
                (BitmapIndexedNode'new edit, (:bitmap this), a)
            )
        )
    )

    #_method
    (defn- #_"BitmapIndexedNode" BitmapIndexedNode''editAndSet-4 [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"Object" x]
        (let [#_"BitmapIndexedNode" e (BitmapIndexedNode''ensureEditable this, edit)]
            (aset (:array e) i x)
            e
        )
    )

    #_method
    (defn- #_"BitmapIndexedNode" BitmapIndexedNode''editAndSet-6 [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"Object" x, #_"int" j, #_"Object" y]
        (let [#_"BitmapIndexedNode" e (BitmapIndexedNode''ensureEditable this, edit)]
            (aset (:array e) i x)
            (aset (:array e) j y)
            e
        )
    )

    #_method
    (defn- #_"BitmapIndexedNode" BitmapIndexedNode''editAndRemovePair [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" bit, #_"int" i]
        (when-not (= (:bitmap this) bit)
            (let [#_"BitmapIndexedNode" e (-> (BitmapIndexedNode''ensureEditable this, edit) (update :bitmap bit-xor bit))
                  #_"Object[]" a (:array e) #_"int" n (alength a)]
                (System/arraycopy a, (* 2 (inc i)), a, (* 2 i), (- n (* 2 (inc i))))
                (aset a (- n 2) nil)
                (aset a (- n 1) nil)
                e
            )
        )
    )

    #_override
    (defn #_"INode" INode'''assoc-7--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"Box" addedLeaf]
        (let [#_"int" bit (PersistentHashMap'bitpos hash, shift) #_"int" idx (BitmapIndexedNode''index this, bit)]
            (if-not (zero? (& (:bitmap this) bit))
                (let [#_"Object" keyOrNull (aget (:array this) (* 2 idx))
                      #_"Object" valOrNode (aget (:array this) (inc (* 2 idx)))]
                    (cond
                        (nil? keyOrNull)
                            (let [#_"INode" n (.assoc (cast INode valOrNode), edit, (+ shift 5), hash, key, val, addedLeaf)]
                                (when-not (= n valOrNode) => this
                                    (BitmapIndexedNode''editAndSet-4 this, edit, (inc (* 2 idx)), n)
                                )
                            )
                        (Util'equiv-2oo key, keyOrNull)
                            (when-not (= val valOrNode) => this
                                (BitmapIndexedNode''editAndSet-4 this, edit, (inc (* 2 idx)), val)
                            )
                        :else
                            (let [_ (§ set! (:val addedLeaf) addedLeaf)]
                                (BitmapIndexedNode''editAndSet-6 this, edit, (* 2 idx), nil, (inc (* 2 idx)), (PersistentHashMap'createNode-7 edit, (+ shift 5), keyOrNull, valOrNode, hash, key, val))
                            )
                    )
                )
                (let [#_"int" n (Integer/bitCount (:bitmap this))]
                    (cond
                        (< (* n 2) (alength (:array this)))
                            (let [_ (§ set! (:val addedLeaf) addedLeaf)
                                  #_"BitmapIndexedNode" e (-> (BitmapIndexedNode''ensureEditable this, edit) (update :bitmap | bit))]
                                (System/arraycopy (:array e), (* 2 idx), (:array e), (* 2 (inc idx)), (* 2 (- n idx)))
                                (aset (:array e) (* 2 idx) key)
                                (aset (:array e) (inc (* 2 idx)) val)
                                e
                            )
                        (<= 16 n)
                            (let [#_"INode[]" nodes (make-array #_"INode" Object 32) #_"int" jdx (PersistentHashMap'mask hash, shift)]
                                (aset nodes jdx (.assoc BitmapIndexedNode'EMPTY, edit, (+ shift 5), hash, key, val, addedLeaf))
                                (loop-when [#_"int" j 0 #_"int" i 0] (< i 32)
                                    (when-not (= (& (>>> (:bitmap this) i) 1) 0) => (recur j (inc i))
                                        (if (some? (aget (:array this) j))
                                            (aset nodes i (.assoc BitmapIndexedNode'EMPTY, edit, (+ shift 5), (PersistentHashMap'hash (aget (:array this) j)), (aget (:array this) j), (aget (:array this) (inc j)), addedLeaf))
                                            (aset nodes i (cast INode (aget (:array this) (inc j))))
                                        )
                                        (recur (+ j 2) (inc i))
                                    )
                                )
                                (ArrayNode'new edit, (inc n), nodes)
                            )
                        :else
                            (let [#_"Object[]" a (make-array Object (* 2 (+ n 4)))]
                                (System/arraycopy (:array this), 0, a, 0, (* 2 idx))
                                (aset a (* 2 idx) key)
                                (§ set! (:val addedLeaf) addedLeaf)
                                (aset a (inc (* 2 idx)) val)
                                (System/arraycopy (:array this), (* 2 idx), a, (* 2 (inc idx)), (* 2 (- n idx)))
                                (-> (BitmapIndexedNode''ensureEditable this, edit)
                                    (assoc :array a)
                                    (update :bitmap | bit)
                                )
                            )
                    )
                )
            )
        )
    )

    #_override
    (defn #_"INode" INode'''without-6--BitmapIndexedNode [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Box" removedLeaf]
        (let-when-not [#_"int" bit (PersistentHashMap'bitpos hash, shift)] (zero? (& (:bitmap this) bit)) => this
            (let [#_"int" i (BitmapIndexedNode''index this, bit) #_"int" ii (* 2 i)
                  #_"Object" keyOrNull (aget (:array this) ii)
                  #_"Object" valOrNode (aget (:array this) (inc ii))]
                (if (some? keyOrNull)
                    (when (Util'equiv-2oo key, keyOrNull) => this
                        (§ set! (:val removedLeaf) removedLeaf)
                        ;; TODO: collapse
                        (BitmapIndexedNode''editAndRemovePair this, edit, bit, i)
                    )
                    (let [#_"INode" n (.without (cast INode valOrNode), edit, (+ shift 5), hash, key, removedLeaf)]
                        (cond
                            (= n valOrNode)
                                this
                            (some? n)
                                (BitmapIndexedNode''editAndSet-4 this, edit, (inc ii), n)
                            (= (:bitmap this) bit)
                                nil
                            :else
                                (BitmapIndexedNode''editAndRemovePair this, edit, bit, i)
                        )
                    )
                )
            )
        )
    )
)

(class-ns HashCollisionNode
    (defn #_"HashCollisionNode" HashCollisionNode'new [#_"AtomicReference<Thread>" edit, #_"int" hash, #_"int" count & #_"Object..." array]
        (hash-map
            #_"AtomicReference<Thread>" :edit edit
            #_"int" :hash hash
            #_"int" :count count
            #_"Object[]" :array array
        )
    )

    #_method
    (defn #_"int" HashCollisionNode''findIndex [#_"HashCollisionNode" this, #_"Object" key]
        (let [#_"int" n (* 2 (:count this))]
            (loop-when [#_"int" i 0] (< i n) => -1
                (if (Util'equiv-2oo key, (aget (:array this) i)) i (recur (+ i 2)))
            )
        )
    )

    #_override
    (defn #_"INode" INode'''assoc-6--HashCollisionNode [#_"HashCollisionNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"Box" addedLeaf]
        (if (= hash (:hash this))
            (let [#_"int" i (HashCollisionNode''findIndex this, key)]
                (if (<= 0 i)
                    (when-not (= (aget (:array this) (inc i)) val) => this
                        (HashCollisionNode'new nil, hash, (:count this), (PersistentHashMap'cloneAndSet-3 (:array this), (inc i), val))
                    )
                    (let [#_"int" n (:count this) #_"Object[]" a (make-array Object (* 2 (inc n)))]
                        (System/arraycopy (:array this), 0, a, 0, (* 2 n))
                        (aset a (* 2 n) key)
                        (aset a (inc (* 2 n)) val)
                        (§ set! (:val addedLeaf) addedLeaf)
                        (HashCollisionNode'new (:edit this), hash, (inc n), a)
                    )
                )
            )
            ;; nest it in a bitmap node
            (let [#_"BitmapIndexedNode" node (BitmapIndexedNode'new nil, (PersistentHashMap'bitpos (:hash this), shift), (object-array [ nil, this ]))]
                (.assoc node, shift, hash, key, val, addedLeaf)
            )
        )
    )

    #_override
    (defn #_"INode" INode'''without-4--HashCollisionNode [#_"HashCollisionNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
        (let-when [#_"int" i (HashCollisionNode''findIndex this, key)] (<= 0 i) => this
            (let-when [#_"int" n (:count this)] (< 1 n)
                (HashCollisionNode'new nil, hash, (dec n), (PersistentHashMap'removePair (:array this), (/ i 2)))
            )
        )
    )

    #_override
    (defn #_"IMapEntry" INode'''find-4--HashCollisionNode [#_"HashCollisionNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
        (let-when [#_"int" i (HashCollisionNode''findIndex this, key)] (<= 0 i)
            (let-when [#_"Object" ai (aget (:array this) i)] (Util'equiv-2oo key, ai)
                (cast IMapEntry (MapEntry'create ai, (aget (:array this) (inc i))))
            )
        )
    )

    #_override
    (defn #_"Object" INode'''find-5--HashCollisionNode [#_"HashCollisionNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" notFound]
        (let-when [#_"int" i (HashCollisionNode''findIndex this, key)] (<= 0 i) => notFound
            (when (Util'equiv-2oo key, (aget (:array this) i)) => notFound
                (aget (:array this) (inc i))
            )
        )
    )

    #_override
    (defn #_"ISeq" INode'''nodeSeq--HashCollisionNode [#_"HashCollisionNode" this]
        (NodeSeq'create-1 (:array this))
    )

    #_override
    (defn #_"Iterator" INode'''iterator--HashCollisionNode [#_"HashCollisionNode" this, #_"IFn" f]
        (NodeIter'new (:array this), f)
    )

    #_override
    (defn #_"Object" INode'''kvreduce--HashCollisionNode [#_"HashCollisionNode" this, #_"IFn" f, #_"Object" r]
        (NodeSeq'kvreduce (:array this), f, r)
    )

    #_override
    (defn #_"Object" INode'''fold--HashCollisionNode [#_"HashCollisionNode" this, #_"IFn" combinef, #_"IFn" reducef, #_"IFn" fjtask, #_"IFn" fjfork, #_"IFn" fjjoin]
        (NodeSeq'kvreduce (:array this), reducef, (.invoke combinef))
    )

    #_method
    (defn- #_"HashCollisionNode" HashCollisionNode''ensureEditable-2 [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit]
        (when-not (= (:edit this) edit) => this
            (let [#_"int" n (:count this) #_"Object[]" a (make-array Object (* 2 (inc n)))] ;; make room for next assoc
                (System/arraycopy (:array this), 0, a, 0, (* 2 n))
                (HashCollisionNode'new edit, (:hash this), n, a)
            )
        )
    )

    #_method
    (defn- #_"HashCollisionNode" HashCollisionNode''ensureEditable-4 [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" count, #_"Object[]" array]
        (if (= (:edit this) edit)
            (assoc this :array array :count count)
            (HashCollisionNode'new edit, (:hash this), count, array)
        )
    )

    #_method
    (defn- #_"HashCollisionNode" HashCollisionNode''editAndSet-4 [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"Object" a]
        (let [#_"HashCollisionNode" e (HashCollisionNode''ensureEditable-2 this, edit)]
            (aset (:array e) i a)
            e
        )
    )

    #_method
    (defn- #_"HashCollisionNode" HashCollisionNode''editAndSet-6 [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"Object" a, #_"int" j, #_"Object" b]
        (let [#_"HashCollisionNode" e (HashCollisionNode''ensureEditable-2 this, edit)]
            (aset (:array e) i a)
            (aset (:array e) j b)
            e
        )
    )

    #_override
    (defn #_"INode" INode'''assoc-7--HashCollisionNode [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"Box" addedLeaf]
        (if (= hash (:hash this))
            (let [#_"int" i (HashCollisionNode''findIndex this, key)]
                (if (<= 0 i)
                    (when-not (= (aget (:array this) (inc i)) val) => this
                        (HashCollisionNode''editAndSet-4 this, edit, (inc i), val)
                    )
                    (let [#_"int" n (:count this) #_"int" m (alength (:array this))]
                        (if (< (* 2 n) m)
                            (let [_ (§ set! (:val addedLeaf) addedLeaf)]
                                (-> (HashCollisionNode''editAndSet-6 this, edit, (* 2 n), key, (inc (* 2 n)), val)
                                    (update :count inc)
                                )
                            )
                            (let [#_"Object[]" a (make-array Object (+ m 2))]
                                (System/arraycopy (:array this), 0, a, 0, m)
                                (aset a m key)
                                (aset a (inc m) val)
                                (§ set! (:val addedLeaf) addedLeaf)
                                (HashCollisionNode''ensureEditable-4 this, edit, (inc n), a)
                            )
                        )
                    )
                )
            )
            ;; nest it in a bitmap node
            (let [#_"BitmapIndexedNode" node (BitmapIndexedNode'new edit, (PersistentHashMap'bitpos (:hash this), shift), (object-array [ nil, this, nil, nil ]))]
                (.assoc node, edit, shift, hash, key, val, addedLeaf)
            )
        )
    )

    #_override
    (defn #_"INode" INode'''without-6--HashCollisionNode [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Box" removedLeaf]
        (let-when [#_"int" i (HashCollisionNode''findIndex this, key)] (<= 0 i) => this
            (§ set! (:val removedLeaf) removedLeaf)
            (let-when [#_"int" n (:count this)] (< 1 n)
                (let [#_"HashCollisionNode" e (-> (HashCollisionNode''ensureEditable-2 this, edit) (update :count dec))
                      #_"int" m (* 2 n)]
                    (aset (:array e) i (aget (:array e) (- m 2)))
                    (aset (:array e) (inc i) (aget (:array e) (- m 1)))
                    (aset (:array e) (- m 2) nil)
                    (aset (:array e) (- m 1) nil)
                    e
                )
            )
        )
    )
)

(class-ns TransientHashMap
    (defn #_"TransientHashMap" TransientHashMap'new
        ([#_"PersistentHashMap" m]
            (TransientHashMap'new (AtomicReference. (Thread/currentThread)), (:root m), (:count m), (:hasNull m), (:nullValue m))
        )
        ([#_"AtomicReference<Thread>" edit, #_"INode" root, #_"int" count, #_"boolean" hasNull, #_"Object" nullValue]
            (merge (ATransientMap'new)
                (hash-map
                    #_"AtomicReference<Thread>" :edit edit
                    #_"INode" :root root
                    #_"int" :count count
                    #_"boolean" :hasNull hasNull
                    #_"Object" :nullValue nullValue

                    #_"Box" :leafFlag (Box'new nil)
                )
            )
        )
    )

    #_override
    (defn #_"ITransientMap" ATransientMap'''doAssoc--TransientHashMap [#_"TransientHashMap" this, #_"Object" key, #_"Object" val]
        (if (nil? key)
            (let [this (if (= (:nullValue this) val) this (assoc this :nullValue val))]
                (when-not (:hasNull this) => this
                    (-> this (update :count inc) (assoc :hasNull true))
                )
            )
            (let [_ (§ set! (:val (:leafFlag this)) nil)
                  #_"INode" n (.assoc (or (:root this) BitmapIndexedNode'EMPTY), (:edit this), 0, (PersistentHashMap'hash key), key, val, (:leafFlag this))
                  this (if (= (:root this) n) this (assoc this :root n))]
                (when (some? (:val (:leafFlag this))) => this
                    (update this :count inc)
                )
            )
        )
    )

    #_override
    (defn #_"ITransientMap" ATransientMap'''doWithout--TransientHashMap [#_"TransientHashMap" this, #_"Object" key]
        (if (nil? key)
            (when (:hasNull this) => this
                (-> this (assoc :hasNull false :nullValue nil) (update :count dec))
            )
            (when (some? (:root this)) => this
                (let [_ (§ set! (:val (:leafFlag this)) nil)
                      #_"INode" n (.without (:root this), (:edit this), 0, (PersistentHashMap'hash key), key, (:leafFlag this))
                      this (if (= (:root this) n) this (assoc this :root n))]
                    (when (some? (:val (:leafFlag this))) => this
                        (update this :count dec)
                    )
                )
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" ATransientMap'''doPersistent--TransientHashMap [#_"TransientHashMap" this]
        (.set (:edit this), nil)
        (PersistentHashMap'new (:count this), (:root this), (:hasNull this), (:nullValue this))
    )

    #_override
    (defn #_"Object" ATransientMap'''doValAt--TransientHashMap [#_"TransientHashMap" this, #_"Object" key, #_"Object" notFound]
        (if (nil? key)
            (when (:hasNull this) => notFound
                (:nullValue this)
            )
            (when (some? (:root this)) => notFound
                (.find (:root this), 0, (PersistentHashMap'hash key), key, notFound)
            )
        )
    )

    #_override
    (defn #_"int" ATransientMap'''doCount--TransientHashMap [#_"TransientHashMap" this]
        (:count this)
    )

    #_override
    (defn #_"void" ATransientMap'''ensureEditable--TransientHashMap [#_"TransientHashMap" this]
        (when (nil? (.get (:edit this)))
            (throw (IllegalAccessError. "Transient used after persistent! call"))
        )
        nil
    )
)

;;;
 ; A persistent rendition of Phil Bagwell's Hash Array Mapped Trie.
 ;
 ; Uses path copying for persistence,
 ; hash collision leaves vs. extended hashing,
 ; node polymorphism vs. conditionals,
 ; no sub-tree pools or root-resizing.
 ;
 ; Any errors are my own.
 ;;
(class-ns PersistentHashMap
    (def- #_"Object" PersistentHashMap'NOT_FOUND (Object.))

    (defn #_"PersistentHashMap" PersistentHashMap'new
        ([#_"int" count, #_"INode" root, #_"boolean" hasNull, #_"Object" nullValue] (PersistentHashMap'new nil, count, root, hasNull, nullValue))
        ([#_"IPersistentMap" meta, #_"int" count, #_"INode" root, #_"boolean" hasNull, #_"Object" nullValue]
            (merge (APersistentMap'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"int" :count count
                    #_"INode" :root root
                    #_"boolean" :hasNull hasNull
                    #_"Object" :nullValue nullValue
                )
            )
        )
    )

    (def #_"PersistentHashMap" PersistentHashMap'EMPTY (PersistentHashMap'new 0, nil, false, nil))

    (defn #_"IPersistentMap" PersistentHashMap'create-1m [#_"Map" other]
        (let [#_"Iterator" it (.iterator (.entrySet other))]
            (loop-when [#_"ITransientMap" m (.asTransient PersistentHashMap'EMPTY)] (.hasNext it) => (.persistent m)
                (let [#_"Map$Entry" e (cast Map$Entry (.next it))]
                    (recur (.assoc m, (.getKey e), (.getValue e)))
                )
            )
        )
    )

    (defn #_"PersistentHashMap" PersistentHashMap'create-1a [& #_"Object..." a]
        (loop-when-recur [#_"ITransientMap" m (.asTransient PersistentHashMap'EMPTY) #_"int" i 0]
                         (< i (alength a))
                         [(.assoc m, (aget a i), (aget a (inc i))) (+ i 2)]
                      => (cast PersistentHashMap (.persistent m))
        )
    )

    (defn #_"PersistentHashMap" PersistentHashMap'createWithCheck-1a [& #_"Object..." a]
        (let [#_"ITransientMap" m (.asTransient PersistentHashMap'EMPTY)
              m (loop-when [m m #_"int" i 0] (< i (alength a)) => m
                    (let [m (.assoc m, (aget a i), (aget a (inc i)))]
                        (when (= (.count m) (inc (/ i 2))) => (throw (IllegalArgumentException. (str "Duplicate key: " (aget a i))))
                            (recur m (+ i 2))
                        )
                    )
                )]
            (cast PersistentHashMap (.persistent m))
        )
    )

    (defn #_"PersistentHashMap" PersistentHashMap'create-1s [#_"ISeq" s]
        (let [#_"ITransientMap" m (.asTransient PersistentHashMap'EMPTY)
              m (loop-when [m m s s] (some? s) => m
                    (when (some? (.next s)) => (throw (IllegalArgumentException. (str "No value supplied for key: " (.first s))))
                        (recur (.assoc m, (.first s), (RT'second s)) (.next (.next s)))
                    )
                )]
            (cast PersistentHashMap (.persistent m))
        )
    )

    (defn #_"PersistentHashMap" PersistentHashMap'createWithCheck-1s [#_"ISeq" s]
        (let [#_"ITransientMap" m (.asTransient PersistentHashMap'EMPTY)
              m (loop-when [m m s s #_"int" i 0] (some? s) => m
                    (when (some? (.next s)) => (throw (IllegalArgumentException. (str "No value supplied for key: " (.first s))))
                        (let [m (.assoc m, (.first s), (RT'second s))]
                            (when (= (.count m) (inc i)) => (throw (IllegalArgumentException. (str "Duplicate key: " (.first s))))
                                (recur m (.next (.next s)) (inc i))
                            )
                        )
                    )
                )]
            (cast PersistentHashMap (.persistent m))
        )
    )

    (defn #_"PersistentHashMap" PersistentHashMap'create-2 [#_"IPersistentMap" meta & #_"Object..." init]
        (-> (PersistentHashMap'create-1a init) (.withMeta meta))
    )

    (defn #_"int" PersistentHashMap'hash [#_"Object" k]
        (Util'hasheq k)
    )

    #_override
    (defn #_"boolean" Associative'''containsKey--PersistentHashMap [#_"PersistentHashMap" this, #_"Object" key]
        (if (nil? key)
            (:hasNull this)
            (and (some? (:root this))
                 (not= (.find (:root this), 0, (PersistentHashMap'hash key), key, PersistentHashMap'NOT_FOUND) PersistentHashMap'NOT_FOUND)
            )
        )
    )

    #_override
    (defn #_"IMapEntry" Associative'''entryAt--PersistentHashMap [#_"PersistentHashMap" this, #_"Object" key]
        (if (nil? key)
            (when (:hasNull this) (cast IMapEntry (MapEntry'create nil, (:nullValue this))))
            (when (some? (:root this)) (.find (:root this), 0, (PersistentHashMap'hash key), key))
        )
    )

    #_override
    (defn #_"IPersistentMap" IPersistentMap'''assoc--PersistentHashMap [#_"PersistentHashMap" this, #_"Object" key, #_"Object" val]
        (if (nil? key)
            (when-not (and (:hasNull this) (= val (:nullValue this))) => this
                (PersistentHashMap'new (.meta this), (+ (:count this) (if (:hasNull this) 0 1)), (:root this), true, val)
            )
            (let [#_"Box" addedLeaf (Box'new nil)
                  #_"INode" newroot (.assoc (or (:root this) BitmapIndexedNode'EMPTY), 0, (PersistentHashMap'hash key), key, val, addedLeaf)]
                (when-not (= newroot (:root this)) => this
                    (PersistentHashMap'new (.meta this), (+ (:count this) (if (some? (:val addedLeaf)) 1 0)), newroot, (:hasNull this), (:nullValue this))
                )
            )
        )
    )

    #_override
    (defn #_"Object" ILookup'''valAt-3--PersistentHashMap [#_"PersistentHashMap" this, #_"Object" key, #_"Object" notFound]
        (if (nil? key)
            (if (:hasNull this) (:nullValue this) notFound)
            (if (some? (:root this)) (.find (:root this), 0, (PersistentHashMap'hash key), key, notFound) notFound)
        )
    )

    #_override
    (defn #_"Object" ILookup'''valAt-2--PersistentHashMap [#_"PersistentHashMap" this, #_"Object" key]
        (.valAt this, key, nil)
    )

    #_override
    (defn #_"IPersistentMap" IPersistentMap'''assocEx--PersistentHashMap [#_"PersistentHashMap" this, #_"Object" key, #_"Object" val]
        (when (.containsKey this, key)
            (throw (RuntimeException. "Key already present"))
        )
        (.assoc this, key, val)
    )

    #_override
    (defn #_"IPersistentMap" IPersistentMap'''without--PersistentHashMap [#_"PersistentHashMap" this, #_"Object" key]
        (cond
            (nil? key)
                (if (:hasNull this) (PersistentHashMap'new (.meta this), (dec (:count this)), (:root this), false, nil) this)
            (nil? (:root this))
                this
            :else
                (let [#_"INode" newroot (.without (:root this), 0, (PersistentHashMap'hash key), key)]
                    (when-not (= newroot (:root this)) => this
                        (PersistentHashMap'new (.meta this), (dec (:count this)), newroot, (:hasNull this), (:nullValue this))
                    )
                )
        )
    )

    (def #_"Iterator" PersistentHashMap'EMPTY_ITER
        (reify Iterator
            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                false
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (throw (NoSuchElementException.))
            )
        )
    )

    #_method
    (defn- #_"Iterator" PersistentHashMap''iterator [#_"PersistentHashMap" this, #_"IFn" f]
        (let-when [#_"Iterator" it (if (some? (:root this)) (.iterator (:root this), f) PersistentHashMap'EMPTY_ITER)] (:hasNull this) => it
            (§ reify Iterator
                [#_mutable #_"boolean" seen false]

                #_foreign
                (#_"boolean" hasNext [#_"Iterator" _self]
                    (or (not seen) (.hasNext it))
                )

                #_foreign
                (#_"Object" next [#_"Iterator" _self]
                    (when (not seen) => (.next it)
                        (set! seen true)
                        (.invoke f, nil, (:nullValue this))
                    )
                )
            )
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---PersistentHashMap [#_"PersistentHashMap" this]
        (PersistentHashMap''iterator this, APersistentMap'MAKE_ENTRY)
    )

    #_override
    (defn #_"Iterator" IMapIterable'''keyIterator--PersistentHashMap [#_"PersistentHashMap" this]
        (PersistentHashMap''iterator this, APersistentMap'MAKE_KEY)
    )

    #_override
    (defn #_"Iterator" IMapIterable'''valIterator--PersistentHashMap [#_"PersistentHashMap" this]
        (PersistentHashMap''iterator this, APersistentMap'MAKE_VAL)
    )

    #_override
    (defn #_"Object" IKVReduce'''kvreduce--PersistentHashMap [#_"PersistentHashMap" this, #_"IFn" f, #_"Object" r]
        (let [r (if (:hasNull this) (.invoke f, r, nil, (:nullValue this)) r)]
            (when-not (RT'isReduced r) => (.deref (cast IDeref r))
                (when (some? (:root this)) => r
                    (let [r (.kvreduce (:root this), f, r)]
                        (when-not (RT'isReduced r) => (.deref (cast IDeref r))
                            r
                        )
                    )
                )
            )
        )
    )

    #_method
    (defn #_"Object" PersistentHashMap''fold [#_"PersistentHashMap" this, #_"long" n, #_"IFn" combinef, #_"IFn" reducef, #_"IFn" fjinvoke, #_"IFn" fjtask, #_"IFn" fjfork, #_"IFn" fjjoin]
        ;; we are ignoring n for now
        (.invoke fjinvoke,
            #(let [_ (.invoke combinef)
                  _ (if (some? (:root this)) (.invoke combinef, _, (.fold (:root this), combinef, reducef, fjtask, fjfork, fjjoin)) _)
                  _ (if (:hasNull this) (.invoke combinef, _, (.invoke reducef, (.invoke combinef), nil, (:nullValue this))) _)]
                _
            )
        )
    )

    #_override
    (defn #_"int" Counted'''count--PersistentHashMap [#_"PersistentHashMap" this]
        (:count this)
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--PersistentHashMap [#_"PersistentHashMap" this]
        (let [#_"ISeq" s (when (some? (:root this)) (.nodeSeq (:root this)))]
            (if (:hasNull this) (Cons'new (MapEntry'create nil, (:nullValue this)), s) s)
        )
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--PersistentHashMap [#_"PersistentHashMap" this]
        (.withMeta PersistentHashMap'EMPTY, (.meta this))
    )

    (defn #_"int" PersistentHashMap'mask [#_"int" hash, #_"int" shift]
        (& (>>> hash shift) 0x01f)
    )

    #_override
    (defn #_"PersistentHashMap" IObj'''withMeta--PersistentHashMap [#_"PersistentHashMap" this, #_"IPersistentMap" meta]
        (PersistentHashMap'new meta, (:count this), (:root this), (:hasNull this), (:nullValue this))
    )

    #_override
    (defn #_"TransientHashMap" IEditableCollection'''asTransient--PersistentHashMap [#_"PersistentHashMap" this]
        (TransientHashMap'new this)
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--PersistentHashMap [#_"PersistentHashMap" this]
        (:_meta this)
    )

    (defn- #_"Object[]" PersistentHashMap'cloneAndSet-3 [#_"Object[]" array, #_"int" i, #_"Object" x]
        (let [#_"Object[]" a (.clone array)]
            (aset a i x)
            a
        )
    )

    (defn- #_"Object[]" PersistentHashMap'cloneAndSet-5 [#_"Object[]" array, #_"int" i, #_"Object" x, #_"int" j, #_"Object" y]
        (let [#_"Object[]" a (.clone array)]
            (aset a i x)
            (aset a j y)
            a
        )
    )

    (defn- #_"Object[]" PersistentHashMap'removePair [#_"Object[]" array, #_"int" i]
        (let [#_"Object[]" a (make-array Object (- (alength array) 2))]
            (System/arraycopy array, 0, a, 0, (* 2 i))
            (System/arraycopy array, (* 2 (inc i)), a, (* 2 i), (- (alength a) (* 2 i)))
            a
        )
    )

    (defn- #_"INode" PersistentHashMap'createNode-6 [#_"int" shift, #_"Object" key1, #_"Object" val1, #_"int" key2hash, #_"Object" key2, #_"Object" val2]
        (let [#_"int" key1hash (PersistentHashMap'hash key1)]
            (when-not (= key1hash key2hash) => (HashCollisionNode'new nil, key1hash, 2, (object-array [ key1, val1, key2, val2 ]))
                (let [#_"Box" addedLeaf (Box'new nil) #_"AtomicReference<Thread>" edit (AtomicReference.)]
                    (-> BitmapIndexedNode'EMPTY
                        (.assoc edit, shift, key1hash, key1, val1, addedLeaf)
                        (.assoc edit, shift, key2hash, key2, val2, addedLeaf)
                    )
                )
            )
        )
    )

    (defn- #_"INode" PersistentHashMap'createNode-7 [#_"AtomicReference<Thread>" edit, #_"int" shift, #_"Object" key1, #_"Object" val1, #_"int" key2hash, #_"Object" key2, #_"Object" val2]
        (let [#_"int" key1hash (PersistentHashMap'hash key1)]
            (when-not (= key1hash key2hash) => (HashCollisionNode'new nil, key1hash, 2, (object-array [ key1, val1, key2, val2 ]))
                (let [#_"Box" addedLeaf (Box'new nil)]
                    (-> BitmapIndexedNode'EMPTY
                        (.assoc edit, shift, key1hash, key1, val1, addedLeaf)
                        (.assoc edit, shift, key2hash, key2, val2, addedLeaf)
                    )
                )
            )
        )
    )

    (defn- #_"int" PersistentHashMap'bitpos [#_"int" hash, #_"int" shift]
        (<< 1 (PersistentHashMap'mask hash, shift))
    )
)
)

(java-ns cloiure.lang.PersistentHashSet

(class-ns TransientHashSet
    (defn #_"TransientHashSet" TransientHashSet'new [#_"ITransientMap" impl]
        (ATransientSet'new impl)
    )

    (declare PersistentHashSet'new)

    #_override
    (defn #_"IPersistentCollection" ITransientCollection'''persistent--TransientHashSet [#_"TransientHashSet" this]
        (PersistentHashSet'new nil, (.persistent (:impl this)))
    )
)

(class-ns PersistentHashSet
    (defn #_"PersistentHashSet" PersistentHashSet'new [#_"IPersistentMap" meta, #_"IPersistentMap" impl]
        (merge (APersistentSet'new impl)
            (hash-map
                #_"IPersistentMap" :_meta meta
            )
        )
    )

    (def #_"PersistentHashSet" PersistentHashSet'EMPTY (PersistentHashSet'new nil, PersistentHashMap'EMPTY))

    (defn #_"PersistentHashSet" PersistentHashSet'create-1a [& #_"Object..." items]
        (loop-when-recur [#_"ITransientSet" s (cast ITransientSet (.asTransient PersistentHashSet'EMPTY)) #_"int" i 0]
                         (< i (alength items))
                         [(cast ITransientSet (.conj s, (aget items i))) (inc i)]
                      => (cast PersistentHashSet (.persistent s))
        )
    )

    (defn #_"PersistentHashSet" PersistentHashSet'create-1l [#_"List" items]
        (let [#_"Iterator" it (.iterator items)]
            (loop-when-recur [#_"ITransientSet" s (cast ITransientSet (.asTransient PersistentHashSet'EMPTY))]
                             (.hasNext it)
                             [(cast ITransientSet (.conj s, (.next it)))]
                          => (cast PersistentHashSet (.persistent s))
            )
        )
    )

    (defn #_"PersistentHashSet" PersistentHashSet'create-1s [#_"ISeq" items]
        (loop-when-recur [#_"ITransientSet" s (cast ITransientSet (.asTransient PersistentHashSet'EMPTY)) items items]
                         (some? items)
                         [(cast ITransientSet (.conj s, (.first items))) (.next items)]
                      => (cast PersistentHashSet (.persistent s))
        )
    )

    (defn #_"PersistentHashSet" PersistentHashSet'createWithCheck-1a [& #_"Object..." items]
        (let [#_"ITransientSet" s (cast ITransientSet (.asTransient PersistentHashSet'EMPTY))
              s (loop-when [s s #_"int" i 0] (< i (alength items)) => s
                    (let [s (cast ITransientSet (.conj s, (aget items i)))]
                        (when (= (.count s) (inc i)) => (throw (IllegalArgumentException. (str "Duplicate key: " (aget items i))))
                            (recur s (inc i))
                        )
                    )
                )]
            (cast PersistentHashSet (.persistent s))
        )
    )

    (defn #_"PersistentHashSet" PersistentHashSet'createWithCheck-1i [#_"Iterable" items]
        (let [#_"Iterator" it (.iterator items)
              #_"ITransientSet" s (cast ITransientSet (.asTransient PersistentHashSet'EMPTY))
              s (loop-when [s s #_"int" i 0] (.hasNext it) => s
                    (let [#_"Object" key (.next it) s (cast ITransientSet (.conj s, key))]
                        (when (= (.count s) (inc i)) => (throw (IllegalArgumentException. (str "Duplicate key: " key)))
                            (recur s (inc i))
                        )
                    )
                )]
            (cast PersistentHashSet (.persistent s))
        )
    )

    (defn #_"PersistentHashSet" PersistentHashSet'createWithCheck-1s [#_"ISeq" items]
        (let [#_"ITransientSet" s (cast ITransientSet (.asTransient PersistentHashSet'EMPTY))
              s (loop-when [s s items items #_"int" i 0] (some? items) => s
                    (let [s (cast ITransientSet (.conj s, (.first items)))]
                        (when (= (.count s) (inc i)) => (throw (IllegalArgumentException. (str "Duplicate key: " (.first items))))
                            (recur s (.next items) (inc i))
                        )
                    )
                )]
            (cast PersistentHashSet (.persistent s))
        )
    )

    #_override
    (defn #_"IPersistentSet" IPersistentSet'''disjoin--PersistentHashSet [#_"PersistentHashSet" this, #_"Object" key]
        (if (.contains this, key)
            (PersistentHashSet'new (.meta this), (.without (:impl this), key))
            this
        )
    )

    #_override
    (defn #_"IPersistentSet" IPersistentCollection'''cons--PersistentHashSet [#_"PersistentHashSet" this, #_"Object" o]
        (if (.contains this, o)
            this
            (PersistentHashSet'new (.meta this), (.assoc (:impl this), o, o))
        )
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--PersistentHashSet [#_"PersistentHashSet" this]
        (.withMeta PersistentHashSet'EMPTY, (.meta this))
    )

    #_override
    (defn #_"PersistentHashSet" IObj'''withMeta--PersistentHashSet [#_"PersistentHashSet" this, #_"IPersistentMap" meta]
        (PersistentHashSet'new meta, (:impl this))
    )

    #_override
    (defn #_"ITransientCollection" IEditableCollection'''asTransient--PersistentHashSet [#_"PersistentHashSet" this]
        (TransientHashSet'new (.asTransient (cast PersistentHashMap (:impl this))))
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--PersistentHashSet [#_"PersistentHashSet" this]
        (:_meta this)
    )
)
)

(java-ns cloiure.lang.PersistentList

(class-ns Primordial
    (defn #_"Primordial" Primordial'new []
        (RestFn'new)
    )

    #_override
    (defn #_"int" RestFn'''getRequiredArity--Primordial [#_"Primordial" this]
        0
    )

    #_override
    (defn #_"Object" RestFn'''doInvoke-2--Primordial [#_"Primordial" this, #_"Object" args]
        (if (instance? ArraySeq args)
            (let [#_"Object[]" a (:array (cast ArraySeq args)) #_"int" i0 (:i (cast ArraySeq args))]
                (loop-when-recur [#_"IPersistentList" l PersistentList'EMPTY #_"int" i (dec (alength a))]
                                 (<= i0 i)
                                 [(cast IPersistentList (.cons l, (aget a i))) (dec i)]
                              => l
                )
            )
            (PersistentList'create (RT'seqToArray (RT'seq args)))
        )
    )

    (defn #_"Object" Primordial'invokeStatic [#_"ISeq" args]
        (if (instance? ArraySeq args)
            (let [#_"Object[]" a (:array (cast ArraySeq args))]
                (loop-when-recur [#_"IPersistentList" l PersistentList'EMPTY #_"int" i (dec (alength a))]
                                 (<= 0 i)
                                 [(cast IPersistentList (.cons l, (aget a i))) (dec i)]
                              => l
                )
            )
            (PersistentList'create (RT'seqToArray (RT'seq args)))
        )
    )

    #_override
    (defn #_"Primordial" IObj'''withMeta--Primordial [#_"Primordial" this, #_"IPersistentMap" meta]
        (throw (UnsupportedOperationException.))
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--Primordial [#_"Primordial" this]
        nil
    )
)

(class-ns EmptyList
    (def #_"int" EmptyList'HASHEQ (§ soon Murmur3'hashOrdered Collections/EMPTY_LIST))

    (defn #_"EmptyList" EmptyList'new [#_"IPersistentMap" meta]
        (hash-map
            #_"IPersistentMap" :_meta meta
        )
    )

    #_foreign
    (defn #_"int" hashCode---EmptyList [#_"EmptyList" this]
        1
    )

    #_method
    (defn #_"int" EmptyList'hasheq [#_"EmptyList" this]
        EmptyList'HASHEQ
    )

    #_foreign
    (defn #_"String" toString---EmptyList [#_"EmptyList" this]
        "()"
    )

    #_foreign
    (defn #_"boolean" equals---EmptyList [#_"EmptyList" this, #_"Object" o]
        (and (or (instance? Sequential o) (instance? List o)) (nil? (RT'seq o)))
    )

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--EmptyList [#_"EmptyList" this, #_"Object" o]
        (.equals this, o)
    )

    #_override
    (defn #_"Object" ISeq'''first--EmptyList [#_"EmptyList" this]
        nil
    )

    #_override
    (defn #_"ISeq" ISeq'''next--EmptyList [#_"EmptyList" this]
        nil
    )

    #_override
    (defn #_"ISeq" ISeq'''more--EmptyList [#_"EmptyList" this]
        this
    )

    #_override
    (defn #_"PersistentList" IPersistentCollection'''cons--EmptyList [#_"EmptyList" this, #_"Object" o]
        (PersistentList'new (.meta this), o, nil, 1)
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--EmptyList [#_"EmptyList" this]
        this
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--EmptyList [#_"EmptyList" this]
        (:_meta this)
    )

    #_override
    (defn #_"EmptyList" IObj'''withMeta--EmptyList [#_"EmptyList" this, #_"IPersistentMap" meta]
        (when-not (= meta (.meta this)) => this
            (EmptyList'new meta)
        )
    )

    #_override
    (defn #_"Object" IPersistentStack'''peek--EmptyList [#_"EmptyList" this]
        nil
    )

    #_override
    (defn #_"IPersistentList" IPersistentStack'''pop--EmptyList [#_"EmptyList" this]
        (throw (IllegalStateException. "Can't pop empty list"))
    )

    #_override
    (defn #_"int" Counted'''count--EmptyList [#_"EmptyList" this]
        0
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--EmptyList [#_"EmptyList" this]
        nil
    )

    #_foreign
    (defn #_"int" size---EmptyList [#_"EmptyList" this]
        0
    )

    #_foreign
    (defn #_"boolean" contains---EmptyList [#_"EmptyList" this, #_"Object" o]
        false
    )

    #_foreign
    (defn #_"Iterator" iterator---EmptyList [#_"EmptyList" this]
        (reify Iterator
            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                false
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (throw (NoSuchElementException.))
            )
        )
    )

    #_foreign
    (defn #_"Object[]" toArray---EmptyList [#_"EmptyList" this]
        RT'EMPTY_ARRAY
    )

    #_foreign
    (defn #_"Object[]" toArray---EmptyList [#_"EmptyList" this, #_"Object[]" objects]
        (when (pos? (alength objects))
            (aset objects 0 nil)
        )
        objects
    )

    #_foreign
    (defn #_"Object" get---EmptyList [#_"EmptyList" this, #_"int" index]
        (RT'nth this, index)
    )
)

(class-ns PersistentList
    (def #_"IFn" PersistentList'creator (§ soon Primordial'new))

    (def #_"EmptyList" PersistentList'EMPTY (§ soon EmptyList'new nil))

    (defn #_"PersistentList" PersistentList'new
        ([#_"Object" _first] (PersistentList'new nil, _first, nil, 1))
        ([#_"IPersistentMap" meta, #_"Object" _first, #_"IPersistentList" _rest, #_"int" _count]
            (merge (ASeq'new meta)
                (hash-map
                    #_"Object" :_first _first
                    #_"IPersistentList" :_rest _rest
                    #_"int" :_count _count
                )
            )
        )
    )

    (defn #_"IPersistentList" PersistentList'create [#_"Object[]" a]
        (loop-when-recur [#_"IPersistentList" l PersistentList'EMPTY #_"int" i (dec (alength a))]
                         (<= 0 i)
                         [(cast IPersistentList (.cons l, (aget a i))) (dec i)]
                      => l
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--PersistentList [#_"PersistentList" this]
        (:_first this)
    )

    #_override
    (defn #_"ISeq" ISeq'''next--PersistentList [#_"PersistentList" this]
        (when-not (= (:_count this) 1)
            (cast ISeq (:_rest this))
        )
    )

    #_override
    (defn #_"Object" IPersistentStack'''peek--PersistentList [#_"PersistentList" this]
        (.first this)
    )

    #_override
    (defn #_"IPersistentList" IPersistentStack'''pop--PersistentList [#_"PersistentList" this]
        (or (:_rest this) (.withMeta PersistentList'EMPTY, (:_meta this)))
    )

    #_override
    (defn #_"int" Counted'''count--PersistentList [#_"PersistentList" this]
        (:_count this)
    )

    #_override
    (defn #_"PersistentList" IPersistentCollection'''cons--PersistentList [#_"PersistentList" this, #_"Object" o]
        (PersistentList'new (.meta this), o, this, (inc (:_count this)))
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--PersistentList [#_"PersistentList" this]
        (.withMeta PersistentList'EMPTY, (.meta this))
    )

    #_override
    (defn #_"PersistentList" IObj'''withMeta--PersistentList [#_"PersistentList" this, #_"IPersistentMap" meta]
        (when-not (= meta (:_meta this)) => this
            (PersistentList'new meta, (:_first this), (:_rest this), (:_count this))
        )
    )

    #_override
    (defn #_"Object" IReduce'''reduce--PersistentList [#_"PersistentList" this, #_"IFn" f]
        (loop-when [#_"Object" r (.first this) #_"ISeq" s (.next this)] (some? s) => r
            (let [r (.invoke f, r, (.first s))]
                (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (.next s)))
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--PersistentList [#_"PersistentList" this, #_"IFn" f, #_"Object" r]
        (loop-when [r (.invoke f, r, (.first this)) #_"ISeq" s (.next this)] (some? s) => (if (RT'isReduced r) (.deref (cast IDeref r)) r)
            (if (RT'isReduced r) (.deref (cast IDeref r)) (recur (.invoke f, r, (.first s)) (.next s)))
        )
    )
)
)

(java-ns cloiure.lang.PersistentQueue

(class-ns QSeq
    (defn #_"QSeq" QSeq'new
        ([#_"ISeq" f, #_"ISeq" rseq] (QSeq'new nil, f, rseq))
        ([#_"IPersistentMap" meta, #_"ISeq" f, #_"ISeq" rseq]
            (merge (ASeq'new meta)
                (hash-map
                    #_"ISeq" :f f
                    #_"ISeq" :rseq rseq
                )
            )
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--QSeq [#_"QSeq" this]
        (.first (:f this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--QSeq [#_"QSeq" this]
        (let [#_"ISeq" f (.next (:f this)) #_"ISeq" r (:rseq this)]
            (cond
                (some? f) (QSeq'new f, r)
                (some? r) (QSeq'new r, nil)
            )
        )
    )

    #_override
    (defn #_"int" Counted'''count--QSeq [#_"QSeq" this]
        (+ (RT'count (:f this)) (RT'count (:rseq this)))
    )

    #_override
    (defn #_"QSeq" IObj'''withMeta--QSeq [#_"QSeq" this, #_"IPersistentMap" meta]
        (QSeq'new meta, (:f this), (:rseq this))
    )
)

;;;
 ; conses onto rear, peeks/pops from front
 ;
 ; See Okasaki's Batched Queues.
 ; Differs in that, it uses a PersistentVector as the rear, which is in-order,
 ; so no reversing or suspensions required for persistent use.
 ;;
(class-ns PersistentQueue
    (defn #_"PersistentQueue" PersistentQueue'new [#_"IPersistentMap" meta, #_"int" cnt, #_"ISeq" f, #_"PersistentVector" r]
        (hash-map
            #_"IPersistentMap" :_meta meta
            #_"int" :cnt cnt
            #_"ISeq" :f f
            #_"PersistentVector" :r r

            #_mutable #_"int" :_hash 0
            #_mutable #_"int" :_hasheq 0
        )
    )

    (def #_"PersistentQueue" PersistentQueue'EMPTY (PersistentQueue'new nil, 0, nil, nil))

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--PersistentQueue [#_"PersistentQueue" this, #_"Object" obj]
        (and (instance? Sequential obj)
            (loop-when [#_"ISeq" s (.seq this) #_"ISeq" ms (RT'seq obj)] (some? s) => (nil? ms)
                (and (some? ms) (Util'equiv-2oo (.first s), (.first ms))
                    (recur (.next s) (.next ms))
                )
            )
        )
    )

    #_foreign
    (defn #_"boolean" equals---PersistentQueue [#_"PersistentQueue" this, #_"Object" obj]
        (and (instance? Sequential obj)
            (loop-when [#_"ISeq" s (.seq this) #_"ISeq" ms (RT'seq obj)] (some? s) => (nil? ms)
                (and (some? ms) (Util'equals (.first s), (.first ms))
                    (recur (.next s) (.next ms))
                )
            )
        )
    )

    #_foreign
    (defn #_"int" hashCode---PersistentQueue [#_"PersistentQueue" this]
        (let-when [#_"int" hash (:_hash this)] (zero? hash) => hash
            (loop-when [hash 1 #_"ISeq" s (.seq this)] (some? s) => (§ set! (:_hash this) hash)
                (recur (+ (* 31 hash) (if (some? (.first s)) (.hashCode (.first s)) 0)) (.next s))
            )
        )
    )

    #_override
    (defn #_"int" IHashEq'''hasheq--PersistentQueue [#_"PersistentQueue" this]
        (let-when [#_"int" cached (:_hasheq this)] (zero? cached) => cached
            (§ set! (:_hasheq this) (Murmur3'hashOrdered this))
        )
    )

    #_override
    (defn #_"Object" IPersistentStack'''peek--PersistentQueue [#_"PersistentQueue" this]
        (RT'first (:f this))
    )

    #_override
    (defn #_"PersistentQueue" IPersistentStack'''pop--PersistentQueue [#_"PersistentQueue" this]
        (when (some? (:f this)) => this ;; hmmm... pop of empty queue -> empty queue?
            (let [#_"ISeq" f (.next (:f this)) #_"PersistentVector" r (:r this)
                  [f r]
                    (when (nil? f) => [f r]
                        [(RT'seq r) nil]
                    )]
                (PersistentQueue'new (.meta this), (dec (:cnt this)), f, r)
            )
        )
    )

    #_override
    (defn #_"int" Counted'''count--PersistentQueue [#_"PersistentQueue" this]
        (:cnt this)
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--PersistentQueue [#_"PersistentQueue" this]
        (when (some? (:f this))
            (QSeq'new (:f this), (RT'seq (:r this)))
        )
    )

    #_override
    (defn #_"PersistentQueue" IPersistentCollection'''cons--PersistentQueue [#_"PersistentQueue" this, #_"Object" o]
        (let [[#_"ISeq" f #_"PersistentVector" r]
                (if (nil? (:f this)) ;; empty
                    [(RT'list o) nil]
                    [(:f this) (.cons (or (:r this) PersistentVector'EMPTY), o)]
                )]
            (PersistentQueue'new (.meta this), (inc (:cnt this)), f, r)
        )
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--PersistentQueue [#_"PersistentQueue" this]
        (.withMeta PersistentQueue'EMPTY, (.meta this))
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--PersistentQueue [#_"PersistentQueue" this]
        (:_meta this)
    )

    #_override
    (defn #_"PersistentQueue" IObj'''withMeta--PersistentQueue [#_"PersistentQueue" this, #_"IPersistentMap" meta]
        (PersistentQueue'new meta, (:cnt this), (:f this), (:r this))
    )

    #_foreign
    (defn #_"Object[]" toArray---PersistentQueue [#_"PersistentQueue" this]
        (RT'seqToArray (.seq this))
    )

    #_foreign
    (defn #_"Object[]" toArray---PersistentQueue [#_"PersistentQueue" this, #_"Object[]" a]
        (RT'seqToPassedArray (.seq this), a)
    )

    #_foreign
    (defn #_"int" size---PersistentQueue [#_"PersistentQueue" this]
        (.count this)
    )

    #_foreign
    (defn #_"boolean" contains---PersistentQueue [#_"PersistentQueue" this, #_"Object" o]
        (loop-when [#_"ISeq" s (.seq this)] (some? s) => false
            (or (Util'equiv-2oo (.first s), o) (recur (.next s)))
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---PersistentQueue [#_"PersistentQueue" this]
        (let [#_"Iterator" it (when (some? (:r this)) (.iterator (:r this)))]
            (§ reify Iterator
                [#_mutable #_"ISeq" s (:f this)]

                #_foreign
                (#_"boolean" hasNext [#_"Iterator" _self]
                    (or (and (some? s) (some? (.seq s))) (and (some? it) (.hasNext it)))
                )

                #_foreign
                (#_"Object" next [#_"Iterator" _self]
                    (if (some? s)
                        (let [_ (.first s)]
                            (update! s #(.next %))
                            _
                        )
                        (when (and (some? it) (.hasNext it)) => (throw (NoSuchElementException.))
                            (.next it)
                        )
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.PersistentTreeMap

(class-ns TNode
    (defn #_"TNode" TNode'new [#_"Object" key]
        (merge (AMapEntry'new)
            (hash-map
                #_"Object" :key key
            )
        )
    )

    #_override
    (defn #_"Object" IMapEntry'''key--TNode [#_"TNode" this]
        (:key this)
    )

    #_override
    (defn #_"Object" IMapEntry'''val--TNode [#_"TNode" this]
        nil
    )

    #_foreign
    (defn #_"Object" getKey---TNode [#_"TNode" this]
        (.key this)
    )

    #_foreign
    (defn #_"Object" getValue---TNode [#_"TNode" this]
        (.val this)
    )

    #_override
    (defn #_"TNode" TNode'''left--TNode [#_"TNode" this]
        nil
    )

    #_override
    (defn #_"TNode" TNode'''right--TNode [#_"TNode" this]
        nil
    )

    (declare PersistentTreeMap'black)

    #_override
    (defn #_"TNode" TNode'''balanceLeft--TNode [#_"TNode" this, #_"TNode" parent]
        (PersistentTreeMap'black (:key parent), (.val parent), this, (.right parent))
    )

    #_override
    (defn #_"TNode" TNode'''balanceRight--TNode [#_"TNode" this, #_"TNode" parent]
        (PersistentTreeMap'black (:key parent), (.val parent), (.left parent), this)
    )

    #_override
    (defn #_"Object" IKVReduce'''kvreduce--TNode [#_"TNode" this, #_"IFn" f, #_"Object" r]
        (or
            (when (some? (.left this))
                (let [r (.kvreduce (.left this), f, r)]
                    (when (RT'isReduced r)
                        r
                    )
                )
            )
            (let [r (.invoke f, r, (.key this), (.val this))]
                (cond
                    (RT'isReduced r)      r
                    (some? (.right this)) (.kvreduce (.right this), f, r)
                    :else                 r
                )
            )
        )
    )
)

(class-ns Black
    (defn #_"Black" Black'new [#_"Object" key]
        (TNode'new key)
    )

    #_override
    (defn #_"TNode" TNode'''addLeft--Black [#_"Black" this, #_"TNode" ins]
        (.balanceLeft ins, this)
    )

    #_override
    (defn #_"TNode" TNode'''addRight--Black [#_"Black" this, #_"TNode" ins]
        (.balanceRight ins, this)
    )

    (declare PersistentTreeMap'balanceLeftDel)

    #_override
    (defn #_"TNode" TNode'''removeLeft--Black [#_"Black" this, #_"TNode" del]
        (PersistentTreeMap'balanceLeftDel (:key this), (.val this), del, (.right this))
    )

    (declare PersistentTreeMap'balanceRightDel)

    #_override
    (defn #_"TNode" TNode'''removeRight--Black [#_"Black" this, #_"TNode" del]
        (PersistentTreeMap'balanceRightDel (:key this), (.val this), (.left this), del)
    )

    #_override
    (defn #_"TNode" TNode'''blacken--Black [#_"Black" this]
        this
    )

    (declare Red'new)

    #_override
    (defn #_"TNode" TNode'''redden--Black [#_"Black" this]
        (Red'new (:key this))
    )

    #_override
    (defn #_"TNode" TNode'''replace--Black [#_"Black" this, #_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right]
        (PersistentTreeMap'black key, val, left, right)
    )
)

(class-ns BlackVal
    (defn #_"BlackVal" BlackVal'new [#_"Object" key, #_"Object" val]
        (merge (Black'new key)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    #_override
    (defn #_"Object" IMapEntry'''val--BlackVal [#_"BlackVal" this]
        (:val this)
    )

    (declare RedVal'new)

    #_override
    (defn #_"TNode" TNode'''redden--BlackVal [#_"BlackVal" this]
        (RedVal'new (:key this), (:val this))
    )
)

(class-ns BlackBranch
    (defn #_"BlackBranch" BlackBranch'new [#_"Object" key, #_"TNode" left, #_"TNode" right]
        (merge (Black'new key)
            (hash-map
                #_"TNode" :left left
                #_"TNode" :right right
            )
        )
    )

    #_override
    (defn #_"TNode" TNode'''left--BlackBranch [#_"BlackBranch" this]
        (:left this)
    )

    #_override
    (defn #_"TNode" TNode'''right--BlackBranch [#_"BlackBranch" this]
        (:right this)
    )

    (declare RedBranch'new)

    #_override
    (defn #_"TNode" TNode'''redden--BlackBranch [#_"BlackBranch" this]
        (RedBranch'new (:key this), (:left this), (:right this))
    )
)

(class-ns BlackBranchVal
    (defn #_"BlackBranchVal" BlackBranchVal'new [#_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right]
        (merge (BlackBranch'new key, left, right)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    #_override
    (defn #_"Object" IMapEntry'''val--BlackBranchVal [#_"BlackBranchVal" this]
        (:val this)
    )

    (declare RedBranchVal'new)

    #_override
    (defn #_"TNode" TNode'''redden--BlackBranchVal [#_"BlackBranchVal" this]
        (RedBranchVal'new (:key this), (:val this), (:left this), (:right this))
    )
)

(class-ns Red
    (defn #_"Red" Red'new [#_"Object" key]
        (TNode'new key)
    )

    (declare PersistentTreeMap'red)

    #_override
    (defn #_"TNode" TNode'''addLeft--Red [#_"Red" this, #_"TNode" ins]
        (PersistentTreeMap'red (:key this), (.val this), ins, (.right this))
    )

    #_override
    (defn #_"TNode" TNode'''addRight--Red [#_"Red" this, #_"TNode" ins]
        (PersistentTreeMap'red (:key this), (.val this), (.left this), ins)
    )

    #_override
    (defn #_"TNode" TNode'''removeLeft--Red [#_"Red" this, #_"TNode" del]
        (PersistentTreeMap'red (:key this), (.val this), del, (.right this))
    )

    #_override
    (defn #_"TNode" TNode'''removeRight--Red [#_"Red" this, #_"TNode" del]
        (PersistentTreeMap'red (:key this), (.val this), (.left this), del)
    )

    #_override
    (defn #_"TNode" TNode'''blacken--Red [#_"Red" this]
        (Black'new (:key this))
    )

    #_override
    (defn #_"TNode" TNode'''redden--Red [#_"Red" this]
        (throw (UnsupportedOperationException. "Invariant violation"))
    )

    #_override
    (defn #_"TNode" TNode'''replace--Red [#_"Red" this, #_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right]
        (PersistentTreeMap'red key, val, left, right)
    )
)

(class-ns RedVal
    (defn #_"RedVal" RedVal'new [#_"Object" key, #_"Object" val]
        (merge (Red'new key)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    #_override
    (defn #_"Object" IMapEntry'''val--RedVal [#_"RedVal" this]
        (:val this)
    )

    #_override
    (defn #_"TNode" TNode'''blacken--RedVal [#_"RedVal" this]
        (BlackVal'new (:key this), (:val this))
    )
)

(class-ns RedBranch
    (defn #_"RedBranch" RedBranch'new [#_"Object" key, #_"TNode" left, #_"TNode" right]
        (merge (Red'new key)
            (hash-map
                #_"TNode" :left left
                #_"TNode" :right right
            )
        )
    )

    #_override
    (defn #_"TNode" TNode'''left--RedBranch [#_"RedBranch" this]
        (:left this)
    )

    #_override
    (defn #_"TNode" TNode'''right--RedBranch [#_"RedBranch" this]
        (:right this)
    )

    #_override
    (defn #_"TNode" TNode'''balanceLeft--RedBranch [#_"RedBranch" this, #_"TNode" parent]
        (cond (instance? Red (:left this))
            (do
                (PersistentTreeMap'red (:key this), (.val this), (.blacken (:left this)), (PersistentTreeMap'black (:key parent), (.val parent), (:right this), (.right parent)))
            )
            (instance? Red (:right this))
            (do
                (PersistentTreeMap'red (:key (:right this)), (.val (:right this)), (PersistentTreeMap'black (:key this), (.val this), (:left this), (.left (:right this))), (PersistentTreeMap'black (:key parent), (.val parent), (.right (:right this)), (.right parent)))
            )
            :else
            (do
                (.balanceLeft (§ super ), parent)
            )
        )
    )

    #_override
    (defn #_"TNode" TNode'''balanceRight--RedBranch [#_"RedBranch" this, #_"TNode" parent]
        (cond (instance? Red (:right this))
            (do
                (PersistentTreeMap'red (:key this), (.val this), (PersistentTreeMap'black (:key parent), (.val parent), (.left parent), (:left this)), (.blacken (:right this)))
            )
            (instance? Red (:left this))
            (do
                (PersistentTreeMap'red (:key (:left this)), (.val (:left this)), (PersistentTreeMap'black (:key parent), (.val parent), (.left parent), (.left (:left this))), (PersistentTreeMap'black (:key this), (.val this), (.right (:left this)), (:right this)))
            )
            :else
            (do
                (.balanceRight (§ super ), parent)
            )
        )
    )

    #_override
    (defn #_"TNode" TNode'''blacken--RedBranch [#_"RedBranch" this]
        (BlackBranch'new (:key this), (:left this), (:right this))
    )
)

(class-ns RedBranchVal
    (defn #_"RedBranchVal" RedBranchVal'new [#_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right]
        (merge (RedBranch'new key, left, right)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    #_override
    (defn #_"Object" IMapEntry'''val--RedBranchVal [#_"RedBranchVal" this]
        (:val this)
    )

    #_override
    (defn #_"TNode" TNode'''blacken--RedBranchVal [#_"RedBranchVal" this]
        (BlackBranchVal'new (:key this), (:val this), (:left this), (:right this))
    )
)

(class-ns TSeq
    (defn #_"TSeq" TSeq'new
        ([#_"ISeq" stack, #_"boolean" asc] (TSeq'new stack, asc, -1))
        ([#_"ISeq" stack, #_"boolean" asc, #_"int" cnt] (TSeq'new nil, stack, asc, cnt))
        ([#_"IPersistentMap" meta, #_"ISeq" stack, #_"boolean" asc, #_"int" cnt]
            (merge (ASeq'new meta)
                (hash-map
                    #_"ISeq" :stack stack
                    #_"boolean" :asc asc
                    #_"int" :cnt cnt
                )
            )
        )
    )

    (defn #_"ISeq" TSeq'push [#_"TNode" t, #_"ISeq" stack, #_"boolean" asc]
        (loop-when [stack stack t t] (some? t) => stack
            (recur (RT'cons t, stack) (if asc (.left t) (.right t)))
        )
    )

    (defn #_"TSeq" TSeq'create [#_"TNode" t, #_"boolean" asc, #_"int" cnt]
        (TSeq'new (TSeq'push t, nil, asc), asc, cnt)
    )

    #_override
    (defn #_"Object" ISeq'''first--TSeq [#_"TSeq" this]
        (.first (:stack this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--TSeq [#_"TSeq" this]
        (let [#_"TNode" t (cast TNode (.first (:stack this))) #_"boolean" asc? (:asc this)]
            (when-let [#_"ISeq" stack (TSeq'push (if asc? (.right t) (.left t)), (.next (:stack this)), asc?)]
                (TSeq'new stack, asc?, (dec (:cnt this)))
            )
        )
    )

    #_override
    (defn #_"int" Counted'''count--TSeq [#_"TSeq" this]
        (when (neg? (:cnt this)) => (:cnt this)
            (.count (§ super ))
        )
    )

    #_override
    (defn #_"TSeq" IObj'''withMeta--TSeq [#_"TSeq" this, #_"IPersistentMap" meta]
        (TSeq'new meta, (:stack this), (:asc this), (:cnt this))
    )
)

(class-ns NodeIterator
    (defn #_"Iterator" NodeIterator'new [#_"TNode" t, #_"boolean" asc?]
        (let [#_"Stack" s (Stack.)
              push!
                (fn #_"void" [#_"TNode" t]
                    (loop-when-recur t (some? t) (if asc? (.left t) (.right t)) => nil
                        (.push s, t)
                    )
                )
              _ (push! t)]
            (reify Iterator
                #_foreign
                (#_"boolean" hasNext [#_"Iterator" _self]
                    (not (.isEmpty s))
                )

                #_foreign
                (#_"Object" next [#_"Iterator" _self]
                    (try
                        (let [#_"TNode" t (cast TNode (.pop s))]
                            (push! (if asc? (.right t) (.left t)))
                            t
                        )
                        (catch EmptyStackException _
                            (throw (NoSuchElementException.))
                        )
                    )
                )
            )
        )
    )
)

;;;
 ; Persistent Red Black Tree.
 ;
 ; Note that instances of this class are constant values,
 ; i.e. add/remove etc return new values.
 ;
 ; See Okasaki, Kahrs, Larsen, et al.
 ;;
(class-ns PersistentTreeMap
    (defn #_"PersistentTreeMap" PersistentTreeMap'new
        ([] (PersistentTreeMap'new RT'DEFAULT_COMPARATOR))
        ([#_"Comparator" comp] (PersistentTreeMap'new nil, comp))
        ([#_"IPersistentMap" meta, #_"Comparator" comp] (PersistentTreeMap'new meta, comp, nil, 0))
        ([#_"IPersistentMap" meta, #_"Comparator" comp, #_"TNode" tree, #_"int" _count]
            (merge (APersistentMap'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"Comparator" :comp comp
                    #_"TNode" :tree tree
                    #_"int" :_count _count
                )
            )
        )
    )

    (def #_"PersistentTreeMap" PersistentTreeMap'EMPTY (PersistentTreeMap'new))

    (defn #_"IPersistentMap" PersistentTreeMap'create-1m [#_"Map" other]
        (let [#_"Iterator" it (.iterator (.entrySet other))]
            (loop-when [#_"IPersistentMap" ret PersistentTreeMap'EMPTY] (.hasNext it) => ret
                (let [#_"Map$Entry" e (cast Map$Entry (.next it))]
                    (recur (.assoc ret, (.getKey e), (.getValue e)))
                )
            )
        )
    )

    #_override
    (defn #_"PersistentTreeMap" IObj'''withMeta--PersistentTreeMap [#_"PersistentTreeMap" this, #_"IPersistentMap" meta]
        (PersistentTreeMap'new meta, (:comp this), (:tree this), (:_count this))
    )

    (defn #_"PersistentTreeMap" PersistentTreeMap'create-1s [#_"ISeq" s]
        (loop-when [#_"IPersistentMap" m PersistentTreeMap'EMPTY s s] (some? s) => (cast PersistentTreeMap m)
            (when (some? (.next s)) => (throw (IllegalArgumentException. (str "No value supplied for key: " (.first s))))
                (recur (.assoc m, (.first s), (RT'second s)) (.next (.next s)))
            )
        )
    )

    (defn #_"PersistentTreeMap" PersistentTreeMap'create-2 [#_"Comparator" comp, #_"ISeq" s]
        (loop-when [#_"IPersistentMap" m (PersistentTreeMap'new comp) s s] (some? s) => (cast PersistentTreeMap m)
            (when (some? (.next s)) => (throw (IllegalArgumentException. (str "No value supplied for key: " (.first s))))
                (recur (.assoc m, (.first s), (RT'second s)) (.next (.next s)))
            )
        )
    )

    #_override
    (defn #_"boolean" Associative'''containsKey--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" key]
        (some? (.entryAt this, key))
    )

    #_foreign
    (defn #_"boolean" equals---PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" obj]
        (try
            (.equals (§ super ), obj)
            (catch ClassCastException _
                false
            )
        )
    )

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" obj]
        (try
            (.equiv (§ super ), obj)
            (catch ClassCastException _
                false
            )
        )
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--PersistentTreeMap [#_"PersistentTreeMap" this]
        (when (pos? (:_count this))
            (TSeq'create (:tree this), true, (:_count this))
        )
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--PersistentTreeMap [#_"PersistentTreeMap" this]
        (PersistentTreeMap'new (.meta this), (:comp this))
    )

    #_override
    (defn #_"ISeq" Reversible'''rseq--PersistentTreeMap [#_"PersistentTreeMap" this]
        (when (pos? (:_count this))
            (TSeq'create (:tree this), false, (:_count this))
        )
    )

    #_override
    (defn #_"Comparator" Sorted'''comparator--PersistentTreeMap [#_"PersistentTreeMap" this]
        (:comp this)
    )

    #_method
    (defn #_"int" PersistentTreeMap''doCompare [#_"PersistentTreeMap" this, #_"Object" k1, #_"Object" k2]
        (.compare (:comp this), k1, k2)
    )

    #_override
    (defn #_"Object" Sorted'''entryKey--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" entry]
        (.key (cast IMapEntry entry))
    )

    #_override
    (defn #_"ISeq" Sorted'''seq--PersistentTreeMap [#_"PersistentTreeMap" this, #_"boolean" ascending]
        (when (pos? (:_count this))
            (TSeq'create (:tree this), ascending, (:_count this))
        )
    )

    #_override
    (defn #_"ISeq" Sorted'''seqFrom--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" key, #_"boolean" ascending]
        (when (pos? (:_count this))
            (loop-when [#_"ISeq" s nil #_"TNode" t (:tree this)] (some? t) => (when (some? s) (TSeq'new s, ascending))
                (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
                    (cond
                        (zero? cmp) (TSeq'new (RT'cons t, s), ascending)
                        ascending   (if (neg? cmp) (recur (RT'cons t, s) (.left t)) (recur s (.right t)))
                        :else       (if (pos? cmp) (recur (RT'cons t, s) (.right t)) (recur s (.left t)))
                    )
                )
            )
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---PersistentTreeMap [#_"PersistentTreeMap" this]
        (NodeIterator'new (:tree this), true)
    )

    #_override
    (defn #_"Object" IKVReduce'''kvreduce--PersistentTreeMap [#_"PersistentTreeMap" this, #_"IFn" f, #_"Object" r]
        (let [r (if (some? (:tree this)) (.kvreduce (:tree this), f, r) r)]
            (if (RT'isReduced r) (.deref (cast IDeref r)) r)
        )
    )

    #_method
    (defn #_"Iterator" PersistentTreeMap''reverseIterator [#_"PersistentTreeMap" this]
        (NodeIterator'new (:tree this), false)
    )

    #_method
    (defn #_"Iterator" PersistentTreeMap''keys [#_"PersistentTreeMap" this]
        (let [#_"Iterator" it (.iterator this)]
            (reify Iterator
                #_foreign
                (#_"boolean" hasNext [#_"Iterator" _self]
                    (.hasNext it)
                )

                #_foreign
                (#_"Object" next [#_"Iterator" _self]
                    (:key (cast TNode (.next it)))
                )
            )
        )
    )

    #_method
    (defn #_"Iterator" PersistentTreeMap''vals [#_"PersistentTreeMap" this]
        (let [#_"Iterator" it (.iterator this)]
            (reify Iterator
                #_foreign
                (#_"boolean" hasNext [#_"Iterator" _self]
                    (.hasNext it)
                )

                #_foreign
                (#_"Object" next [#_"Iterator" _self]
                    (.val (cast TNode (.next it)))
                )
            )
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''min [#_"PersistentTreeMap" this]
        (when-let [#_"TNode" t (:tree this)]
            (loop-when-recur t (some? (.left t)) (.left t) => t)
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''max [#_"PersistentTreeMap" this]
        (when-let [#_"TNode" t (:tree this)]
            (loop-when-recur t (some? (.right t)) (.right t) => t)
        )
    )

    #_method
    (defn #_"Object" PersistentTreeMap''minKey [#_"PersistentTreeMap" this]
        (let [#_"TNode" t (PersistentTreeMap''min this)]
            (when (some? t) (:key t))
        )
    )

    #_method
    (defn #_"Object" PersistentTreeMap''maxKey [#_"PersistentTreeMap" this]
        (let [#_"TNode" t (PersistentTreeMap''max this)]
            (when (some? t) (:key t))
        )
    )

    #_method
    (defn #_"int" PersistentTreeMap''depth-2 [#_"PersistentTreeMap" this, #_"TNode" t]
        (when (some? t) => 0
            (inc (Math/max (PersistentTreeMap''depth-2 this, (.left t)), (PersistentTreeMap''depth-2 this, (.right t))))
        )
    )

    #_method
    (defn #_"int" PersistentTreeMap''depth-1 [#_"PersistentTreeMap" this]
        (PersistentTreeMap''depth-2 this, (:tree this))
    )

    #_override
    (defn #_"Object" ILookup'''valAt-3--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" key, #_"Object" notFound]
        (let [#_"TNode" n (.entryAt this, key)]
            (if (some? n) (.val n) notFound)
        )
    )

    #_override
    (defn #_"Object" ILookup'''valAt-2--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" key]
        (.valAt this, key, nil)
    )

    #_method
    (defn #_"int" PersistentTreeMap''capacity [#_"PersistentTreeMap" this]
        (:_count this)
    )

    #_override
    (defn #_"int" Counted'''count--PersistentTreeMap [#_"PersistentTreeMap" this]
        (:_count this)
    )

    #_override
    (defn #_"TNode" Associative'''entryAt--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" key]
        (loop-when [#_"TNode" t (:tree this)] (some? t) => t
            (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
                (cond
                    (neg? cmp) (recur (.left t))
                    (pos? cmp) (recur (.right t))
                    :else      t
                )
            )
        )
    )

    (defn #_"TNode" PersistentTreeMap'rightBalance [#_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" ins]
        (cond
            (and (instance? Red ins) (instance? Red (.right ins)))
                (PersistentTreeMap'red (:key ins), (.val ins), (PersistentTreeMap'black key, val, left, (.left ins)), (.blacken (.right ins)))
            (and (instance? Red ins) (instance? Red (.left ins)))
                (PersistentTreeMap'red (:key (.left ins)), (.val (.left ins)), (PersistentTreeMap'black key, val, left, (.left (.left ins))), (PersistentTreeMap'black (:key ins), (.val ins), (.right (.left ins)), (.right ins)))
            :else
                (PersistentTreeMap'black key, val, left, ins)
        )
    )

    (defn #_"TNode" PersistentTreeMap'balanceLeftDel [#_"Object" key, #_"Object" val, #_"TNode" del, #_"TNode" right]
        (cond
            (instance? Red del)
                (PersistentTreeMap'red key, val, (.blacken del), right)
            (instance? Black right)
                (PersistentTreeMap'rightBalance key, val, del, (.redden right))
            (and (instance? Red right) (instance? Black (.left right)))
                (PersistentTreeMap'red (:key (.left right)), (.val (.left right)), (PersistentTreeMap'black key, val, del, (.left (.left right))), (PersistentTreeMap'rightBalance (:key right), (.val right), (.right (.left right)), (.redden (.right right))))
            :else
                (throw (UnsupportedOperationException. "Invariant violation"))
        )
    )

    (defn #_"TNode" PersistentTreeMap'leftBalance [#_"Object" key, #_"Object" val, #_"TNode" ins, #_"TNode" right]
        (cond
            (and (instance? Red ins) (instance? Red (.left ins)))
                (PersistentTreeMap'red (:key ins), (.val ins), (.blacken (.left ins)), (PersistentTreeMap'black key, val, (.right ins), right))
            (and (instance? Red ins) (instance? Red (.right ins)))
                (PersistentTreeMap'red (:key (.right ins)), (.val (.right ins)), (PersistentTreeMap'black (:key ins), (.val ins), (.left ins), (.left (.right ins))), (PersistentTreeMap'black key, val, (.right (.right ins)), right))
            :else
                (PersistentTreeMap'black key, val, ins, right)
        )
    )

    (defn #_"TNode" PersistentTreeMap'balanceRightDel [#_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" del]
        (cond
            (instance? Red del)
                (PersistentTreeMap'red key, val, left, (.blacken del))
            (instance? Black left)
                (PersistentTreeMap'leftBalance key, val, (.redden left), del)
            (and (instance? Red left) (instance? Black (.right left)))
                (PersistentTreeMap'red (:key (.right left)), (.val (.right left)), (PersistentTreeMap'leftBalance (:key left), (.val left), (.redden (.left left)), (.left (.right left))), (PersistentTreeMap'black key, val, (.right (.right left)), del))
            :else
                (throw (UnsupportedOperationException. "Invariant violation"))
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''add [#_"PersistentTreeMap" this, #_"TNode" t, #_"Object" key, #_"Object" val, #_"Box" found]
        (if (nil? t)
            (if (nil? val)
                (Red'new key)
                (RedVal'new key, val)
            )
            (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
                (if (zero? cmp)
                    (do
                        (§ set! (:val found) t)
                        nil
                    )
                    (let [#_"TNode" ins (if (neg? cmp) (PersistentTreeMap''add this, (.left t), key, val, found) (PersistentTreeMap''add this, (.right t), key, val, found))]
                        (cond
                            (nil? ins) nil ;; found below
                            (neg? cmp) (.addLeft t, ins)
                            :else      (.addRight t, ins)
                        )
                    )
                )
            )
        )
    )

    (defn- #_"TNode" PersistentTreeMap'append [#_"TNode" left, #_"TNode" right]
        (cond
            (nil? left)
                right
            (nil? right)
                left
            (instance? Red left)
                (if (instance? Red right)
                    (let [#_"TNode" app (PersistentTreeMap'append (.right left), (.left right))]
                        (if (instance? Red app)
                            (PersistentTreeMap'red (:key app), (.val app), (PersistentTreeMap'red (:key left), (.val left), (.left left), (.left app)), (PersistentTreeMap'red (:key right), (.val right), (.right app), (.right right)))
                            (PersistentTreeMap'red (:key left), (.val left), (.left left), (PersistentTreeMap'red (:key right), (.val right), app, (.right right)))
                        )
                    )
                    (PersistentTreeMap'red (:key left), (.val left), (.left left), (PersistentTreeMap'append (.right left), right))
                )
            (instance? Red right)
                (PersistentTreeMap'red (:key right), (.val right), (PersistentTreeMap'append left, (.left right)), (.right right))
            :else ;; black/black
                (let [#_"TNode" app (PersistentTreeMap'append (.right left), (.left right))]
                    (if (instance? Red app)
                        (PersistentTreeMap'red (:key app), (.val app), (PersistentTreeMap'black (:key left), (.val left), (.left left), (.left app)), (PersistentTreeMap'black (:key right), (.val right), (.right app), (.right right)))
                        (PersistentTreeMap'balanceLeftDel (:key left), (.val left), (.left left), (PersistentTreeMap'black (:key right), (.val right), app, (.right right)))
                    )
                )
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''remove [#_"PersistentTreeMap" this, #_"TNode" t, #_"Object" key, #_"Box" found]
        (when (some? t) => nil ;; not found indicator
            (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
                (if (zero? cmp)
                    (do
                        (§ set! (:val found) t)
                        (PersistentTreeMap'append (.left t), (.right t))
                    )
                    (let [#_"TNode" del (if (neg? cmp) (PersistentTreeMap''remove this, (.left t), key, found) (PersistentTreeMap''remove this, (.right t), key, found))]
                        (when (or (some? del) (some? (:val found))) => nil ;; not found below
                            (if (neg? cmp)
                                (if (instance? Black (.left t))
                                    (PersistentTreeMap'balanceLeftDel (:key t), (.val t), del, (.right t))
                                    (PersistentTreeMap'red (:key t), (.val t), del, (.right t))
                                )
                                (if (instance? Black (.right t))
                                    (PersistentTreeMap'balanceRightDel (:key t), (.val t), (.left t), del)
                                    (PersistentTreeMap'red (:key t), (.val t), (.left t), del)
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''replace [#_"PersistentTreeMap" this, #_"TNode" t, #_"Object" key, #_"Object" val]
        (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
            (.replace t, (:key t), (if (zero? cmp) val (.val t)), (if (neg? cmp) (PersistentTreeMap''replace this, (.left t), key, val) (.left t)), (if (pos? cmp) (PersistentTreeMap''replace this, (.right t), key, val) (.right t)))
        )
    )

    (defn #_"Red" PersistentTreeMap'red [#_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right]
        (if (and (nil? left) (nil? right))
            (if (nil? val)
                (Red'new key)
                (RedVal'new key, val)
            )
            (if (nil? val)
                (RedBranch'new key, left, right)
                (RedBranchVal'new key, val, left, right)
            )
        )
    )

    (defn #_"Black" PersistentTreeMap'black [#_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right]
        (if (and (nil? left) (nil? right))
            (if (nil? val)
                (Black'new key)
                (BlackVal'new key, val)
            )
            (if (nil? val)
                (BlackBranch'new key, left, right)
                (BlackBranchVal'new key, val, left, right)
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--PersistentTreeMap [#_"PersistentTreeMap" this]
        (:_meta this)
    )

    #_override
    (defn #_"PersistentTreeMap" IPersistentMap'''assocEx--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" key, #_"Object" val]
        (let [#_"Box" found (Box'new nil) #_"TNode" t (PersistentTreeMap''add this, (:tree this), key, val, found)]
            (when (nil? t) ;; nil == already contains key
                (throw (RuntimeException. "Key already present"))
            )
            (PersistentTreeMap'new (.meta this), (:comp this), (.blacken t), (inc (:_count this)))
        )
    )

    #_override
    (defn #_"PersistentTreeMap" IPersistentMap'''assoc--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" key, #_"Object" val]
        (let [#_"Box" found (Box'new nil) #_"TNode" t (PersistentTreeMap''add this, (:tree this), key, val, found)]
            (if (nil? t) ;; nil == already contains key
                (if (= (.val (cast TNode (:val found))) val) ;; note only get same collection on identity of val, not equals()
                    this
                    (PersistentTreeMap'new (.meta this), (:comp this), (PersistentTreeMap''replace this, (:tree this), key, val), (:_count this))
                )
                (PersistentTreeMap'new (.meta this), (:comp this), (.blacken t), (inc (:_count this)))
            )
        )
    )

    #_override
    (defn #_"PersistentTreeMap" IPersistentMap'''without--PersistentTreeMap [#_"PersistentTreeMap" this, #_"Object" key]
        (let [#_"Box" found (Box'new nil) #_"TNode" t (PersistentTreeMap''remove this, (:tree this), key, found)]
            (if (nil? t)
                (if (nil? (:val found)) ;; nil == doesn't contain key
                    this
                    (PersistentTreeMap'new (.meta this), (:comp this)) ;; empty
                )
                (PersistentTreeMap'new (.meta this), (:comp this), (.blacken t), (dec (:_count this)))
            )
        )
    )
)
)

(java-ns cloiure.lang.PersistentTreeSet

(class-ns PersistentTreeSet
    (defn #_"PersistentTreeSet" PersistentTreeSet'new [#_"IPersistentMap" meta, #_"IPersistentMap" impl]
        (merge (APersistentSet'new impl)
            (hash-map
                #_"IPersistentMap" :_meta meta
            )
        )
    )

    (def #_"PersistentTreeSet" PersistentTreeSet'EMPTY (PersistentTreeSet'new nil, PersistentTreeMap'EMPTY))

    (defn #_"PersistentTreeSet" PersistentTreeSet'create-1 [#_"ISeq" s]
        (loop-when-recur [#_"PersistentTreeSet" t PersistentTreeSet'EMPTY s s]
                         (some? s)
                         [(cast PersistentTreeSet (.cons t, (.first s))) (.next s)]
                      => t
        )
    )

    (defn #_"PersistentTreeSet" PersistentTreeSet'create-2 [#_"Comparator" comp, #_"ISeq" s]
        (loop-when-recur [#_"PersistentTreeSet" t (PersistentTreeSet'new nil, (PersistentTreeMap'new nil, comp)) s s]
                         (some? s)
                         [(cast PersistentTreeSet (.cons t, (.first s))) (.next s)]
                      => t
        )
    )

    #_foreign
    (defn #_"boolean" equals---PersistentTreeSet [#_"PersistentTreeSet" this, #_"Object" obj]
        (try
            (.equals (§ super ), obj)
            (catch ClassCastException _
                false
            )
        )
    )

    #_override
    (defn #_"boolean" IPersistentCollection'''equiv--PersistentTreeSet [#_"PersistentTreeSet" this, #_"Object" obj]
        (try
            (.equiv (§ super ), obj)
            (catch ClassCastException _
                false
            )
        )
    )

    #_override
    (defn #_"IPersistentSet" IPersistentSet'''disjoin--PersistentTreeSet [#_"PersistentTreeSet" this, #_"Object" key]
        (if (.contains this, key)
            (PersistentTreeSet'new (.meta this), (.without (:impl this), key))
            this
        )
    )

    #_override
    (defn #_"IPersistentSet" IPersistentCollection'''cons--PersistentTreeSet [#_"PersistentTreeSet" this, #_"Object" o]
        (if (.contains this, o)
            this
            (PersistentTreeSet'new (.meta this), (.assoc (:impl this), o, o))
        )
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--PersistentTreeSet [#_"PersistentTreeSet" this]
        (PersistentTreeSet'new (.meta this), (cast PersistentTreeMap (.empty (:impl this))))
    )

    #_override
    (defn #_"ISeq" Reversible'''rseq--PersistentTreeSet [#_"PersistentTreeSet" this]
        (KeySeq'create (.rseq (cast Reversible (:impl this))))
    )

    #_override
    (defn #_"PersistentTreeSet" IObj'''withMeta--PersistentTreeSet [#_"PersistentTreeSet" this, #_"IPersistentMap" meta]
        (PersistentTreeSet'new meta, (:impl this))
    )

    #_override
    (defn #_"Comparator" Sorted'''comparator--PersistentTreeSet [#_"PersistentTreeSet" this]
        (.comparator (cast Sorted (:impl this)))
    )

    #_override
    (defn #_"Object" Sorted'''entryKey--PersistentTreeSet [#_"PersistentTreeSet" this, #_"Object" entry]
        entry
    )

    #_override
    (defn #_"ISeq" Sorted'''seq--PersistentTreeSet [#_"PersistentTreeSet" this, #_"boolean" ascending]
        (let [#_"PersistentTreeMap" m (cast PersistentTreeMap (:impl this))]
            (RT'keys (.seq m, ascending))
        )
    )

    #_override
    (defn #_"ISeq" Sorted'''seqFrom--PersistentTreeSet [#_"PersistentTreeSet" this, #_"Object" key, #_"boolean" ascending]
        (let [#_"PersistentTreeMap" m (cast PersistentTreeMap (:impl this))]
            (RT'keys (.seqFrom m, key, ascending))
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--PersistentTreeSet [#_"PersistentTreeSet" this]
        (:_meta this)
    )
)
)

(java-ns cloiure.lang.PersistentVector

(class-ns VNode
    (defn #_"VNode" VNode'new
        ([#_"AtomicReference<Thread>" edit] (VNode'new edit, (make-array Object 32)))
        ([#_"AtomicReference<Thread>" edit, #_"Object[]" array]
            (hash-map
                #_"AtomicReference<Thread>" :edit edit
                #_"Object[]" :array array
            )
        )
    )
)

(declare PersistentVector''arrayFor)

(class-ns ChunkedSeq
    (defn #_"ChunkedSeq" ChunkedSeq'new
        ([#_"PersistentVector" vec, #_"int" i, #_"int" offset] (ChunkedSeq'new nil, vec, (PersistentVector''arrayFor vec, i), i, offset))
        ([#_"PersistentVector" vec, #_"Object[]" node, #_"int" i, #_"int" offset] (ChunkedSeq'new nil, vec, node, i, offset))
        ([#_"IPersistentMap" meta, #_"PersistentVector" vec, #_"Object[]" node, #_"int" i, #_"int" offset]
            (merge (ASeq'new meta)
                (hash-map
                    #_"PersistentVector" :vec vec
                    #_"Object[]" :node node
                    #_"int" :i i
                    #_"int" :offset offset
                )
            )
        )
    )

    #_override
    (defn #_"IChunk" IChunkedSeq'''chunkedFirst--ChunkedSeq [#_"ChunkedSeq" this]
        (ArrayChunk'new (:node this), (:offset this))
    )

    #_override
    (defn #_"ISeq" IChunkedSeq'''chunkedNext--ChunkedSeq [#_"ChunkedSeq" this]
        (when (< (+ (:i this) (alength (:node this))) (:cnt (:vec this)))
            (ChunkedSeq'new (:vec this), (+ (:i this) (alength (:node this))), 0)
        )
    )

    #_override
    (defn #_"ISeq" IChunkedSeq'''chunkedMore--ChunkedSeq [#_"ChunkedSeq" this]
        (or (.chunkedNext this) PersistentList'EMPTY)
    )

    #_override
    (defn #_"ChunkedSeq" IObj'''withMeta--ChunkedSeq [#_"ChunkedSeq" this, #_"IPersistentMap" meta]
        (when-not (= meta (:_meta this)) => this
            (ChunkedSeq'new meta, (:vec this), (:node this), (:i this), (:offset this))
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--ChunkedSeq [#_"ChunkedSeq" this]
        (aget (:node this) (:offset this))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--ChunkedSeq [#_"ChunkedSeq" this]
        (if (< (inc (:offset this)) (alength (:node this)))
            (ChunkedSeq'new (:vec this), (:node this), (:i this), (inc (:offset this)))
            (.chunkedNext this)
        )
    )

    #_override
    (defn #_"int" Counted'''count--ChunkedSeq [#_"ChunkedSeq" this]
        (- (:cnt (:vec this)) (+ (:i this) (:offset this)))
    )
)

(class-ns TransientVector
    (defn #_"TransientVector" TransientVector'new
        ([#_"PersistentVector" v] (TransientVector'new (:cnt v), (:shift v), (.editableRoot (:root v)), (.editableTail (:tail v))))
        ([#_"int" cnt, #_"int" shift, #_"VNode" root, #_"Object[]" tail]
            (merge (AFn'new)
                (hash-map
                    #_"int" :cnt cnt
                    #_"int" :shift shift
                    #_"VNode" :root root
                    #_"Object[]" :tail tail
                )
            )
        )
    )

    #_method
    (defn TransientVector''ensureEditable
        (#_"void" [#_"TransientVector" this]
            (when-not (some? (.get (:edit (:root this)))) => nil
                (throw (IllegalAccessError. "Transient used after persistent! call"))
            )
        )
        (#_"VNode" [#_"TransientVector" this, #_"VNode" node]
            (when-not (= (:edit node) (:edit (:root this))) => node
                (VNode'new (:edit (:root this)), (.clone (:array node)))
            )
        )
    )

    #_override
    (defn #_"int" Counted'''count--TransientVector [#_"TransientVector" this]
        (TransientVector''ensureEditable this)
        (:cnt this)
    )

    (defn #_"VNode" TransientVector'editableRoot [#_"VNode" node]
        (VNode'new (AtomicReference. (Thread/currentThread)), (.clone (:array node)))
    )

    #_method
    (defn- #_"int" TransientVector''tailoff [#_"TransientVector" this]
        (if (< (:cnt this) 32) 0 (<< (>>> (dec (:cnt this)) 5) 5))
    )

    #_override
    (defn #_"PersistentVector" ITransientCollection'''persistent--TransientVector [#_"TransientVector" this]
        (TransientVector''ensureEditable this)
        (.set (:edit (:root this)), nil)
        (let [#_"Object[]" trimmedTail (make-array Object (- (:cnt this) (TransientVector''tailoff this)))]
            (System/arraycopy (:tail this), 0, trimmedTail, 0, (alength trimmedTail))
            (PersistentVector'new (:cnt this), (:shift this), (:root this), trimmedTail)
        )
    )

    (defn #_"Object[]" TransientVector'editableTail [#_"Object[]" tail]
        (let [#_"Object[]" a (make-array Object 32)]
            (System/arraycopy tail, 0, a, 0, (alength tail))
            a
        )
    )

    #_method
    (defn- #_"VNode" TransientVector''pushTail [#_"TransientVector" this, #_"int" level, #_"VNode" parent, #_"VNode" tailnode]
        ;; if parent is leaf, insert node,
        ;; else does it map to an existing child? -> nodeToInsert = pushNode one more level
        ;; else alloc new path
        ;; return nodeToInsert placed in parent
        (let [parent (TransientVector''ensureEditable this, parent)
              #_"int" i (& (>>> (dec (:cnt this)) level) 0x01f)
              #_"VNode" nodeToInsert
                (when-not (= level 5) => tailnode
                    (let [#_"VNode" child (cast VNode (aget (:array parent) i))]
                        (if (some? child)
                            (TransientVector''pushTail this, (- level 5), child, tailnode)
                            (PersistentVector'newPath (:edit (:root this)), (- level 5), tailnode)
                        )
                    )
                )]
            (aset (:array parent) i nodeToInsert)
            parent
        )
    )

    #_override
    (defn #_"TransientVector" ITransientCollection'''conj--TransientVector [#_"TransientVector" this, #_"Object" val]
        (TransientVector''ensureEditable this)
        (let [#_"int" n (:cnt this)]
            (if (< (- n (TransientVector''tailoff this)) 32) ;; room in tail?
                (do
                    (aset (:tail this) (& n 0x01f) val)
                    (update this :cnt inc)
                )
                ;; full tail, push into tree
                (let [#_"VNode" tailnode (VNode'new (:edit (:root this)), (:tail this))
                      this (assoc this :tail (make-array Object 32))
                      _ (aset (:tail this) 0 val)
                      #_"int" shift (:shift this)
                      [#_"VNode" root shift]
                        (if (< (<< 1 shift) (>>> n 5)) ;; overflow root?
                            (let [root (VNode'new (:edit (:root this)))]
                                (aset (:array root) 0 (:root this))
                                (aset (:array root) 1 (PersistentVector'newPath (:edit (:root this)), shift, tailnode))
                                [root (+ shift 5)]
                            )
                            [(TransientVector''pushTail this, shift, (:root this), tailnode) shift]
                        )]
                    (-> this (assoc :root root :shift shift) (update :cnt inc))
                )
            )
        )
    )

    #_method
    (defn- #_"Object[]" TransientVector''arrayFor [#_"TransientVector" this, #_"int" i]
        (when (< -1 i (:cnt this)) => (throw (IndexOutOfBoundsException.))
            (when (< i (TransientVector''tailoff this)) => (:tail this)
                (loop-when-recur [#_"VNode" node (:root this) #_"int" level (:shift this)]
                                 (< 0 level)
                                 [(cast VNode (aget (:array node) (& (>>> i level) 0x01f))) (- level 5)]
                              => (:array node)
                )
            )
        )
    )

    #_method
    (defn- #_"Object[]" TransientVector''editableArrayFor [#_"TransientVector" this, #_"int" i]
        (when (< -1 i (:cnt this)) => (throw (IndexOutOfBoundsException.))
            (when (< i (TransientVector''tailoff this)) => (:tail this)
                (loop-when-recur [#_"VNode" node (:root this) #_"int" level (:shift this)]
                                 (< 0 level)
                                 [(TransientVector''ensureEditable this, (cast VNode (aget (:array node) (& (>>> i level) 0x01f)))) (- level 5)]
                              => (:array node)
                )
            )
        )
    )

    #_override
    (defn #_"Object" ILookup'''valAt-2--TransientVector [#_"TransientVector" this, #_"Object" key]
        ;; note - relies on ensureEditable in 2-arg valAt
        (.valAt this, key, nil)
    )

    #_override
    (defn #_"Object" ILookup'''valAt-3--TransientVector [#_"TransientVector" this, #_"Object" key, #_"Object" notFound]
        (TransientVector''ensureEditable this)
        (when (Numbers'isInteger key) => notFound
            (let-when [#_"int" i (.intValue (cast Number key))] (< -1 i (:cnt this)) => notFound
                (.nth this, i)
            )
        )
    )

    (def- #_"Object" TransientVector'NOT_FOUND (Object.))

    #_override
    (defn #_"boolean" ITransientAssociative2'''containsKey--TransientVector [#_"TransientVector" this, #_"Object" key]
        (not= (.valAt this, key, TransientVector'NOT_FOUND) TransientVector'NOT_FOUND)
    )

    #_override
    (defn #_"IMapEntry" ITransientAssociative2'''entryAt--TransientVector [#_"TransientVector" this, #_"Object" key]
        (let [#_"Object" v (.valAt this, key, TransientVector'NOT_FOUND)]
            (when-not (= v TransientVector'NOT_FOUND)
                (MapEntry'create key, v)
            )
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--TransientVector [#_"TransientVector" this, #_"Object" arg1]
        ;; note - relies on ensureEditable in nth
        (when (Numbers'isInteger arg1) => (throw (IllegalArgumentException. "Key must be integer"))
            (.nth this, (.intValue (cast Number arg1)))
        )
    )

    #_override
    (defn #_"Object" Indexed'''nth-2--TransientVector [#_"TransientVector" this, #_"int" i]
        (TransientVector''ensureEditable this)
        (let [#_"Object[]" node (TransientVector''arrayFor this, i)]
            (aget node (& i 0x01f))
        )
    )

    #_override
    (defn #_"Object" Indexed'''nth-3--TransientVector [#_"TransientVector" this, #_"int" i, #_"Object" notFound]
        (when (< -1 i (.count this)) => notFound
            (.nth this, i)
        )
    )

    #_method
    (defn- #_"VNode" TransientVector''doAssoc [#_"TransientVector" this, #_"int" level, #_"VNode" node, #_"int" i, #_"Object" val]
        (let [node (TransientVector''ensureEditable this, node)]
            (if (zero? level)
                (aset (:array node) (& i 0x01f) val)
                (let [#_"int" si (& (>>> i level) 0x01f)]
                    (aset (:array node) si (TransientVector''doAssoc this, (- level 5), (cast VNode (aget (:array node) si)), i, val))
                )
            )
            node
        )
    )

    #_override
    (defn #_"TransientVector" ITransientVector'''assocN--TransientVector [#_"TransientVector" this, #_"int" i, #_"Object" val]
        (TransientVector''ensureEditable this)
        (if (< -1 i (:cnt this))
            (if (<= (TransientVector''tailoff this) i)
                (do
                    (aset (:tail this) (& i 0x01f) val)
                    this
                )
                (do
                    (assoc this :root (TransientVector''doAssoc this, (:shift this), (:root this), i, val))
                )
            )
            (when (= i (:cnt this)) => (throw (IndexOutOfBoundsException.))
                (.conj this, val)
            )
        )
    )

    #_override
    (defn #_"TransientVector" ITransientAssociative'''assoc--TransientVector [#_"TransientVector" this, #_"Object" key, #_"Object" val]
        ;; note - relies on ensureEditable in assocN
        (when (Numbers'isInteger key) => (throw (IllegalArgumentException. "Key must be integer"))
            (.assocN this, (.intValue (cast Number key)), val)
        )
    )

    #_method
    (defn- #_"VNode" TransientVector''popTail [#_"TransientVector" this, #_"int" level, #_"VNode" node]
        (let [node (TransientVector''ensureEditable this, node)
              #_"int" i (& (>>> (- (:cnt this) 2) level) 0x01f)]
            (cond
                (< 5 level)
                    (let [#_"VNode" child (TransientVector''popTail this, (- level 5), (cast VNode (aget (:array node) i)))]
                        (when-not (and (nil? child) (zero? i))
                            (aset (:array node) i child)
                            node
                        )
                    )
                (pos? i)
                    (do
                        (aset (:array node) i nil)
                        node
                    )
            )
        )
    )

    #_override
    (defn #_"TransientVector" ITransientVector'''pop--TransientVector [#_"TransientVector" this]
        (TransientVector''ensureEditable this)
        (let [#_"int" n (:cnt this)]
            (when-not (zero? n) => (throw (IllegalStateException. "Can't pop empty vector"))
                (when (and (not= n 1) (zero? (& (dec n) 0x01f))) => (assoc this :cnt (dec n))
                    (let [#_"Object[]" tail (TransientVector''editableArrayFor this, (- n 2))
                          #_"int" shift (:shift this) #_"VNode" root (:root this)
                          root (or (TransientVector''popTail this, shift, root) (VNode'new (:edit root)))
                          [shift root]
                            (when (and (< 5 shift) (nil? (aget (:array root) 1))) => [shift root]
                                [(- shift 5) (TransientVector''ensureEditable this, (cast VNode (aget (:array root) 0)))]
                            )]
                        (assoc this :cnt (dec n) :shift shift :root root :tail tail)
                    )
                )
            )
        )
    )
)

(class-ns PersistentVector
    (def #_"AtomicReference<Thread>" PersistentVector'NOEDIT (AtomicReference. nil))
    (def #_"VNode" PersistentVector'EMPTY_NODE (VNode'new PersistentVector'NOEDIT, (object-array 32)))

    (defn #_"PersistentVector" PersistentVector'new
        ([#_"int" cnt, #_"int" shift, #_"VNode" root, #_"Object[]" tail] (PersistentVector'new nil, cnt, shift, root, tail))
        ([#_"IPersistentMap" meta, #_"int" cnt, #_"int" shift, #_"VNode" root, #_"Object[]" tail]
            (merge (APersistentVector'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"int" :cnt cnt
                    #_"int" :shift shift
                    #_"VNode" :root root
                    #_"Object[]" :tail tail
                )
            )
        )
    )

    (def #_"PersistentVector" PersistentVector'EMPTY (PersistentVector'new 0, 5, PersistentVector'EMPTY_NODE, (object-array 0)))

    (defn #_"PersistentVector" PersistentVector'adopt [#_"Object[]" items]
        (PersistentVector'new (alength items), 5, PersistentVector'EMPTY_NODE, items)
    )

    (defn #_"PersistentVector" PersistentVector'create-1r [#_"IReduceInit" items]
        (let [conj- (fn ([v] v) ([v o] (.conj (cast ITransientVector v), o)))]
            (.persistent (.reduce items, conj-, (.asTransient PersistentVector'EMPTY)))
        )
    )

    (defn #_"PersistentVector" PersistentVector'create-1s [#_"ISeq" items]
        (let [#_"Object[]" a (make-array Object 32)
              #_"int" i
                (loop-when-recur [items items i 0] (and (some? items) (< i 32)) [(.next items) (inc i)] => i
                    (aset a i (.first items))
                )]
            (cond
                (some? items) ;; >32, construct with array directly
                    (let [#_"PersistentVector" v0 (PersistentVector'new 32, 5, PersistentVector'EMPTY_NODE, a)]
                        (loop-when-recur [#_"TransientVector" v (.asTransient v0) items items]
                                         (some? items)
                                         [(.conj v, (.first items)) (.next items)]
                                      => (.persistent v)
                        )
                    )
                (= i 32) ;; exactly 32, skip copy
                    (PersistentVector'new 32, 5, PersistentVector'EMPTY_NODE, a)
                :else ;; <32, copy to minimum array and construct
                    (let [#_"Object[]" b (make-array Object i)]
                        (System/arraycopy a, 0, b, 0, i)
                        (PersistentVector'new i, 5, PersistentVector'EMPTY_NODE, b)
                    )
            )
        )
    )

    (defn #_"PersistentVector" PersistentVector'create-1l [#_"List" items]
        (let-when [#_"int" n (.size items)] (< 32 n) => (PersistentVector'new n, 5, PersistentVector'EMPTY_NODE, (.toArray items))
            (loop-when-recur [#_"TransientVector" v (.asTransient PersistentVector'EMPTY) #_"int" i 0]
                             (< i n)
                             [(.conj v, (.get items, i)) (inc i)]
                          => (.persistent v)
            )
        )
    )

    (defn #_"PersistentVector" PersistentVector'create-1i [#_"Iterable" items]
        (when-not (instance? ArrayList items) => (PersistentVector'create-1l (cast ArrayList items)) ;; optimize common case
            (let [#_"Iterator" it (.iterator items)]
                (loop-when-recur [#_"TransientVector" v (.asTransient PersistentVector'EMPTY)]
                                 (.hasNext it)
                                 [(.conj v, (.next it))]
                              => (.persistent v)
                )
            )
        )
    )

    (defn #_"PersistentVector" PersistentVector'create-1a [& #_"Object..." items]
        (loop-when-recur [#_"TransientVector" v (.asTransient PersistentVector'EMPTY) #_"int" i 0]
                         (< i (alength items))
                         [(.conj v, (aget items i)) (inc i)]
                      => (.persistent v)
        )
    )

    #_override
    (defn #_"TransientVector" IEditableCollection'''asTransient--PersistentVector [#_"PersistentVector" this]
        (TransientVector'new this)
    )

    #_method
    (defn #_"int" PersistentVector''tailoff [#_"PersistentVector" this]
        (if (< (:cnt this) 32) 0 (<< (>>> (dec (:cnt this)) 5) 5))
    )

    #_method
    (defn #_"Object[]" PersistentVector''arrayFor [#_"PersistentVector" this, #_"int" i]
        (when (< -1 i (:cnt this)) => (throw (IndexOutOfBoundsException.))
            (when (< i (PersistentVector''tailoff this)) => (:tail this)
                (loop-when-recur [#_"VNode" node (:root this) #_"int" level (:shift this)]
                                 (< 0 level)
                                 [(cast VNode (aget (:array node) (& (>>> i level) 0x01f))) (- level 5)]
                              => (:array node)
                )
            )
        )
    )

    #_override
    (defn #_"Object" Indexed'''nth-2--PersistentVector [#_"PersistentVector" this, #_"int" i]
        (aget (PersistentVector''arrayFor this, i) (& i 0x01f))
    )

    #_override
    (defn #_"Object" Indexed'''nth-3--PersistentVector [#_"PersistentVector" this, #_"int" i, #_"Object" notFound]
        (when (< -1 i (:cnt this)) => notFound
            (.nth this, i)
        )
    )

    (defn- #_"VNode" PersistentVector'doAssoc [#_"int" level, #_"VNode" node, #_"int" i, #_"Object" val]
        (let [#_"VNode" ret (VNode'new (:edit node), (.clone (:array node)))]
            (if (zero? level)
                (aset (:array ret) (& i 0x01f) val)
                (let [#_"int" si (& (>>> i level) 0x01f)]
                    (aset (:array ret) si (PersistentVector'doAssoc (- level 5), (cast VNode (aget (:array node) si)), i, val))
                )
            )
            ret
        )
    )

    #_override
    (defn #_"PersistentVector" IPersistentVector'''assocN--PersistentVector [#_"PersistentVector" this, #_"int" i, #_"Object" val]
        (if (< -1 i (:cnt this))
            (if (<= (PersistentVector''tailoff this) i)
                (let [#_"Object[]" tail (make-array Object (alength (:tail this)))]
                    (System/arraycopy (:tail this), 0, tail, 0, (alength (:tail this)))
                    (aset tail (& i 0x01f) val)
                    (PersistentVector'new (.meta this), (:cnt this), (:shift this), (:root this), tail)
                )
                (PersistentVector'new (.meta this), (:cnt this), (:shift this), (PersistentVector'doAssoc (:shift this), (:root this), i, val), (:tail this))
            )
            (when (= i (:cnt this)) => (throw (IndexOutOfBoundsException.))
                (.cons this, val)
            )
        )
    )

    #_override
    (defn #_"int" Counted'''count--PersistentVector [#_"PersistentVector" this]
        (:cnt this)
    )

    #_override
    (defn #_"PersistentVector" IObj'''withMeta--PersistentVector [#_"PersistentVector" this, #_"IPersistentMap" meta]
        (PersistentVector'new meta, (:cnt this), (:shift this), (:root this), (:tail this))
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--PersistentVector [#_"PersistentVector" this]
        (:_meta this)
    )

    #_method
    (defn- #_"VNode" PersistentVector''pushTail [#_"PersistentVector" this, #_"int" level, #_"VNode" parent, #_"VNode" tailnode]
        ;; if parent is leaf, insert node,
        ;; else does it map to an existing child? -> nodeToInsert = pushNode one more level
        ;; else alloc new path
        ;; return nodeToInsert placed in copy of parent
        (let [#_"int" i (& (>>> (dec (:cnt this)) level) 0x01f)
              #_"VNode" ret (VNode'new (:edit parent), (.clone (:array parent)))
              #_"VNode" nodeToInsert
                (when-not (= level 5) => tailnode
                    (let [#_"VNode" child (cast VNode (aget (:array parent) i))]
                        (if (some? child)
                            (PersistentVector''pushTail this, (- level 5), child, tailnode)
                            (PersistentVector'newPath (:edit (:root this)), (- level 5), tailnode)
                        )
                    )
                )]
            (aset (:array ret) i nodeToInsert)
            ret
        )
    )

    #_override
    (defn #_"PersistentVector" IPersistentVector'''cons--PersistentVector [#_"PersistentVector" this, #_"Object" val]
        (let [#_"int" n (:cnt this)]
            (if (< (- n (PersistentVector''tailoff this)) 32) ;; room in tail?
                (let [#_"int" e (alength (:tail this)) #_"Object[]" tail (make-array Object (inc e))]
                    (System/arraycopy (:tail this), 0, tail, 0, e)
                    (aset tail e val)
                    (PersistentVector'new (.meta this), (inc n), (:shift this), (:root this), tail)
                )
                ;; full tail, push into tree
                (let [#_"VNode" tailnode (VNode'new (:edit (:root this)), (:tail this))
                      #_"int" shift (:shift this)
                      [#_"VNode" root shift]
                        (if (< (<< 1 shift) (>>> n 5)) ;; overflow root?
                            (let [root (VNode'new (:edit (:root this)))]
                                (aset (:array root) 0 (:root this))
                                (aset (:array root) 1 (PersistentVector'newPath (:edit (:root this)), shift, tailnode))
                                [root (+ shift 5)]
                            )
                            [(PersistentVector''pushTail this, shift, (:root this), tailnode) shift]
                        )]
                    (PersistentVector'new (.meta this), (inc n), shift, root, (object-array [ val ]))
                )
            )
        )
    )

    (defn- #_"VNode" PersistentVector'newPath [#_"AtomicReference<Thread>" edit, #_"int" level, #_"VNode" node]
        (when-not (zero? level) => node
            (let [#_"VNode" ret (VNode'new edit)]
                (aset (:array ret) 0 (PersistentVector'newPath edit, (- level 5), node))
                ret
            )
        )
    )

    #_method
    (defn #_"IChunkedSeq" PersistentVector''chunkedSeq [#_"PersistentVector" this]
        (when (pos? (.count this))
            (ChunkedSeq'new this, 0, 0)
        )
    )

    #_override
    (defn #_"ISeq" Seqable'''seq--PersistentVector [#_"PersistentVector" this]
        (PersistentVector''chunkedSeq this)
    )

    #_override
    (defn #_"Iterator" APersistentVector'''rangedIterator--PersistentVector [#_"PersistentVector" this, #_"int" start, #_"int" end]
        (§ reify Iterator
            [#_mutable #_"int" i start
             #_mutable #_"int" base (- start (% start 32))
             #_mutable #_"Object[]" a (when (< start (.count this)) (PersistentVector''arrayFor this, start))]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (< i end)
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (when (< i end) => (throw (NoSuchElementException.))
                    (when (= i (+ base 32))
                        (set! a (PersistentVector''arrayFor this, i))
                        (set! base i)
                    )
                    (let [_ (aget a (& i 0x01f))]
                        (update! i inc)
                        _
                    )
                )
            )
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---PersistentVector [#_"PersistentVector" this]
        (.rangedIterator this, 0, (.count this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--PersistentVector [#_"PersistentVector" this, #_"IFn" f]
        (when (pos? (:cnt this)) => (.invoke f)
            (loop-when [#_"Object" r (aget (PersistentVector''arrayFor this, 0) 0) #_"int" i 0] (< i (:cnt this)) => r
                (let [#_"Object[]" a (PersistentVector''arrayFor this, i)
                      r (loop-when [r r #_"int" j (if (zero? i) 1 0)] (< j (alength a)) => r
                            (let [r (.invoke f, r, (aget a j))]
                                (when-not (RT'isReduced r) => (ß return (.deref (cast IDeref r)))
                                    (recur r (inc j))
                                )
                            )
                        )]
                    (recur r (+ i (alength a)))
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--PersistentVector [#_"PersistentVector" this, #_"IFn" f, #_"Object" r]
        (loop-when [r r #_"int" i 0] (< i (:cnt this)) => r
            (let [#_"Object[]" a (PersistentVector''arrayFor this, i)
                  r (loop-when [r r #_"int" j 0] (< j (alength a)) => r
                        (let [r (.invoke f, r, (aget a j))]
                            (when-not (RT'isReduced r) => (ß return (.deref (cast IDeref r)))
                                (recur r (inc j))
                            )
                        )
                    )]
                (recur r (+ i (alength a)))
            )
        )
    )

    #_override
    (defn #_"Object" IKVReduce'''kvreduce--PersistentVector [#_"PersistentVector" this, #_"IFn" f, #_"Object" r]
        (loop-when [r r #_"int" i 0] (< i (:cnt this)) => r
            (let [#_"Object[]" a (PersistentVector''arrayFor this, i)
                  r (loop-when [r r #_"int" j 0] (< j (alength a)) => r
                        (let [r (.invoke f, r, (+ j i), (aget a j))]
                            (when-not (RT'isReduced r) => (ß return (.deref (cast IDeref r)))
                                (recur r (inc j))
                            )
                        )
                    )]
                (recur r (+ i (alength a)))
            )
        )
    )

    #_override
    (defn #_"IPersistentCollection" IPersistentCollection'''empty--PersistentVector [#_"PersistentVector" this]
        (.withMeta PersistentVector'EMPTY, (.meta this))
    )

    #_method
    (defn- #_"VNode" PersistentVector''popTail [#_"PersistentVector" this, #_"int" level, #_"VNode" node]
        (let [#_"int" i (& (>>> (- (:cnt this) 2) level) 0x01f)]
            (cond
                (< 5 level)
                    (let [#_"VNode" child (PersistentVector''popTail this, (- level 5), (cast VNode (aget (:array node) i)))]
                        (when-not (and (nil? child) (zero? i))
                            (let [#_"VNode" ret (VNode'new (:edit (:root this)), (.clone (:array node)))]
                                (aset (:array ret) i child)
                                ret
                            )
                        )
                    )
                (pos? i)
                    (let [#_"VNode" ret (VNode'new (:edit (:root this)), (.clone (:array node)))]
                        (aset (:array ret) i nil)
                        ret
                    )
            )
        )
    )

    #_override
    (defn #_"PersistentVector" IPersistentStack'''pop--PersistentVector [#_"PersistentVector" this]
        (cond
            (zero? (:cnt this))
                (throw (IllegalStateException. "Can't pop empty vector"))
            (= (:cnt this) 1)
                (.withMeta PersistentVector'EMPTY, (.meta this))
            (< 1 (- (:cnt this) (PersistentVector''tailoff this)))
                (let [#_"Object[]" tail (make-array Object (dec (alength (:tail this))))]
                    (System/arraycopy (:tail this), 0, tail, 0, (alength tail))
                    (PersistentVector'new (.meta this), (dec (:cnt this)), (:shift this), (:root this), tail)
                )
            :else
                (let [#_"Object[]" tail (PersistentVector''arrayFor this, (- (:cnt this) 2))
                      #_"int" shift (:shift this)
                      #_"VNode" root (or (PersistentVector''popTail this, shift, (:root this)) PersistentVector'EMPTY_NODE)
                      [shift root]
                        (when (and (< 5 shift) (nil? (aget (:array root) 1))) => [shift root]
                            [(- shift 5) (cast VNode (aget (:array root) 0))]
                        )]
                    (PersistentVector'new (.meta this), (dec (:cnt this)), shift, root, tail)
                )
        )
    )
)
)

(java-ns cloiure.lang.LazilyPersistentVector

(class-ns LazilyPersistentVector
    (defn #_"IPersistentVector" LazilyPersistentVector'createOwning [& #_"Object..." items]
        (if (<= (alength items) 32)
            (PersistentVector'new (alength items), 5, PersistentVector'EMPTY_NODE, items)
            (PersistentVector'create-1a items)
        )
    )

    (defn #_"int" LazilyPersistentVector'fcount [#_"Object" c]
        (if (instance? Counted c)
            (.count (cast Counted c))
            (.size (cast Collection c))
        )
    )

    (defn #_"IPersistentVector" LazilyPersistentVector'create [#_"Object" obj]
        (cond
            (instance? IReduceInit obj) (PersistentVector'create-1r (cast IReduceInit obj))
            (instance? ISeq obj)        (PersistentVector'create-1s (RT'seq obj))
            (instance? Iterable obj)    (PersistentVector'create-1i (cast Iterable obj))
            :else                       (LazilyPersistentVector'createOwning (RT'toArray obj))
        )
    )
)
)

(java-ns cloiure.lang.Range

;;;
 ; Implements generic numeric (potentially infinite) range.
 ;;
(class-ns Range
    (def- #_"int" Range'CHUNK_SIZE 32)

    (defn- #_"RangeBoundsCheck" Range'positiveStep [#_"Object" end]
        (reify RangeBoundsCheck
            #_override
            (#_"boolean" exceededBounds [#_"RangeBoundsCheck" _self, #_"Object" val]
                (Numbers'gte-2oo val, end)
            )
        )
    )

    (defn- #_"RangeBoundsCheck" Range'negativeStep [#_"Object" end]
        (reify RangeBoundsCheck
            #_override
            (#_"boolean" exceededBounds [#_"RangeBoundsCheck" _self, #_"Object" val]
                (Numbers'lte-2oo val, end)
            )
        )
    )

    (defn- #_"Range" Range'new
        ([#_"Object" start, #_"Object" end, #_"Object" step, #_"RangeBoundsCheck" boundsCheck]
            (Range'new start, end, step, boundsCheck, nil, nil)
        )
        ([#_"Object" start, #_"Object" end, #_"Object" step, #_"RangeBoundsCheck" boundsCheck, #_"IChunk" chunk, #_"ISeq" chunkNext]
            (Range'new nil, start, end, step, boundsCheck, chunk, chunkNext)
        )
        ([#_"IPersistentMap" meta, #_"Object" start, #_"Object" end, #_"Object" step, #_"RangeBoundsCheck" boundsCheck, #_"IChunk" chunk, #_"ISeq" chunkNext]
            (merge (ASeq'new meta)
                (hash-map
                    ;; Invariants guarantee this is never an "empty" seq
                    #_"Object" :start start
                    #_"Object" :end end
                    #_"Object" :step step
                    #_"RangeBoundsCheck" :boundsCheck boundsCheck

                    #_volatile #_"IChunk" :_chunk chunk ;; lazy
                    #_volatile #_"ISeq" :_chunkNext chunkNext ;; lazy
                    #_volatile #_"ISeq" :_next nil ;; cached
                )
            )
        )
    )

    (defn #_"ISeq" Range'create
        ([#_"Object" end]
            (when (Numbers'isPos-1o end) => PersistentList'EMPTY
                (Range'new 0, end, 1, (Range'positiveStep end))
            )
        )
        ([#_"Object" start, #_"Object" end]
            (Range'create start, end, 1)
        )
        ([#_"Object" start, #_"Object" end, #_"Object" step]
            (cond
                (or (and (Numbers'isPos-1o step) (Numbers'gt-2oo start, end))
                    (and (Numbers'isNeg-1o step) (Numbers'gt-2oo end, start))
                    (Numbers'equiv-2oo start, end)
                )
                    PersistentList'EMPTY
                (Numbers'isZero-1o step)
                    (Repeat'create-1 start)
                :else
                    (Range'new start, end, step, (if (Numbers'isPos-1o step) (Range'positiveStep end) (Range'negativeStep end)))
            )
        )
    )

    #_override
    (defn #_"Range" IObj'''withMeta--Range [#_"Range" this, #_"IPersistentMap" meta]
        (when-not (= meta (:_meta this)) => this
            (Range'new meta, (:end this), (:start this), (:step this), (:boundsCheck this), (:_chunk this), (:_chunkNext this))
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--Range [#_"Range" this]
        (:start this)
    )

    #_method
    (defn #_"void" Range''forceChunk [#_"Range" this]
        (when (nil? (:_chunk this))
            (let [#_"Object[]" a (make-array Object Range'CHUNK_SIZE)]
                (loop [#_"Object" n (:start this) #_"int" i 0]
                    (if (< i Range'CHUNK_SIZE)
                        (do
                            (aset a i n)
                            (let-when [n (Numbers'addP-2oo n, (:step this))] (.exceededBounds (:boundsCheck this), n) => (recur n (inc i))
                                ;; partial last chunk
                                (§ set! (:_chunk this) (ArrayChunk'new a, 0, (inc i)))
                            )
                        )
                        (if (.exceededBounds (:boundsCheck this), n)
                            (do
                                ;; full last chunk
                                (§ set! (:_chunk this) (ArrayChunk'new a, 0, Range'CHUNK_SIZE))
                            )
                            (do
                                ;; full intermediate chunk
                                (§ set! (:_chunk this) (ArrayChunk'new a, 0, Range'CHUNK_SIZE))
                                (§ set! (:_chunkNext this) (Range'new n, (:end this), (:step this), (:boundsCheck this)))
                            )
                        )
                    )
                )
            )
        )
        nil
    )

    #_override
    (defn #_"ISeq" ISeq'''next--Range [#_"Range" this]
        (let-when [#_"Range" _next (:_next this)] (nil? _next) => _next
            (Range''forceChunk this)
            (when (< 1 (.count (:_chunk this))) => (.chunkedNext this)
                (let [#_"IChunk" _rest (.dropFirst (:_chunk this))]
                    (§ set! (:_next this) (Range'new (.nth _rest, 0), (:end this), (:step this), (:boundsCheck this), _rest, (:_chunkNext this)))
                )
            )
        )
    )

    #_override
    (defn #_"IChunk" IChunkedSeq'''chunkedFirst--Range [#_"Range" this]
        (Range''forceChunk this)
        (:_chunk this)
    )

    #_override
    (defn #_"ISeq" IChunkedSeq'''chunkedNext--Range [#_"Range" this]
        (.seq (.chunkedMore this))
    )

    #_override
    (defn #_"ISeq" IChunkedSeq'''chunkedMore--Range [#_"Range" this]
        (Range''forceChunk this)
        (or (:_chunkNext this) PersistentList'EMPTY)
    )

    #_override
    (defn #_"Object" IReduce'''reduce--Range [#_"Range" this, #_"IFn" f]
        (loop [#_"Object" r (:start this) #_"Number" n r]
            (let-when-not [n (Numbers'addP-2oo n, (:step this))] (.exceededBounds (:boundsCheck this), n) => r
                (let-when-not [r (.invoke f, r, n)] (RT'isReduced r) => (.deref (cast Reduced r))
                    (recur r n)
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--Range [#_"Range" this, #_"IFn" f, #_"Object" r]
        (loop [r r #_"Object" n (:start this)]
            (let-when-not [r (.invoke f, r, n)] (RT'isReduced r) => (.deref (cast Reduced r))
                (let-when-not [n (Numbers'addP-2oo n, (:step this))] (.exceededBounds (:boundsCheck this), n) => r
                    (recur r n)
                )
            )
        )
    )

    #_foreign
    (defn #_"Iterator" iterator---Range [#_"Range" this]
        (§ reify Iterator
            [#_mutable #_"Object" n (:start this)]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (not (.exceededBounds (:boundsCheck this), n))
            )

            #_foreign
            (#_"Object" next [#_"Iterator" self]
                (when (.hasNext self) => (throw (NoSuchElementException.))
                    (let [_ n]
                        (update! n Numbers'addP-2oo (:step this))
                        _
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.Reduced

(class-ns Reduced
    (defn #_"Reduced" Reduced'new [#_"Object" val]
        (hash-map
            #_"Object" :val val
        )
    )

    #_override
    (defn #_"Object" IDeref'''deref--Reduced [#_"Reduced" this]
        (:val this)
    )
)
)

(java-ns cloiure.lang.Repeat

(class-ns Repeat
    (def- #_"long" Repeat'INFINITE -1)

    (defn- #_"Repeat" Repeat'new
        ([#_"long" count, #_"Object" val] (Repeat'new nil, count, val))
        ([#_"IPersistentMap" meta, #_"long" count, #_"Object" val]
            (merge (ASeq'new meta)
                (hash-map
                    #_"long" :count count ;; always INFINITE or pos?
                    #_"Object" :val val

                    #_volatile #_"ISeq" :_next nil ;; cached
                )
            )
        )
    )

    (defn #_"Repeat" Repeat'create-1 [#_"Object" val]
        (Repeat'new Repeat'INFINITE, val)
    )

    (defn #_"ISeq" Repeat'create-2 [#_"long" count, #_"Object" val]
        (if (pos? count) (Repeat'new count, val) PersistentList'EMPTY)
    )

    #_override
    (defn #_"Object" ISeq'''first--Repeat [#_"Repeat" this]
        (:val this)
    )

    #_override
    (defn #_"ISeq" ISeq'''next--Repeat [#_"Repeat" this]
        (when (nil? (:_next this))
            (cond
                (< 1 (:count this))               (§ set! (:_next this) (Repeat'new (dec (:count this)), (:val this)))
                (= (:count this) Repeat'INFINITE) (§ set! (:_next this) this)
            )
        )
        (:_next this)
    )

    #_override
    (defn #_"Repeat" IObj'''withMeta--Repeat [#_"Repeat" this, #_"IPersistentMap" meta]
        (Repeat'new meta, (:count this), (:val this))
    )

    #_override
    (defn #_"Object" IReduce'''reduce--Repeat [#_"Repeat" this, #_"IFn" f]
        (let [#_"Object" r (:val this)]
            (if (= (:count this) Repeat'INFINITE)
                (loop [r r]
                    (let [r (.invoke f, r, (:val this))]
                        (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r))
                    )
                )
                (loop-when [r r #_"long" i 1] (< i (:count this)) => r
                    (let [r (.invoke f, r, (:val this))]
                        (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                    )
                )
            )
        )
    )

    #_override
    (defn #_"Object" IReduceInit'''reduce--Repeat [#_"Repeat" this, #_"IFn" f, #_"Object" r]
        (if (= (:count this) Repeat'INFINITE)
            (loop [r r]
                (let [r (.invoke f, r, (:val this))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r))
                )
            )
            (loop-when [r r #_"long" i 0] (< i (:count this)) => r
                (let [r (.invoke f, r, (:val this))]
                    (if (RT'isReduced r) (.deref (cast IDeref r)) (recur r (inc i)))
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.SeqIterator

(class-ns SeqIterator
    (def- #_"Object" SeqIterator'START (Object.))

    (defn #_"Iterator" SeqIterator'new [#_"Object" o]
        (§ reify Iterator
            [#_mutable #_"Object" s SeqIterator'START
             #_mutable #_"Object" n o]

            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (some?
                    (condp identical? s
                        SeqIterator'START (do (set! s nil) (update! n RT'seq))
                        n (update! n RT'next)
                        :else n
                    )
                )
            )

            #_foreign
            (#_"Object" next [#_"Iterator" self]
                (when (.hasNext self) => (throw (NoSuchElementException.))
                    (RT'first (set! s n))
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.StringSeq

(class-ns StringSeq
    (defn- #_"StringSeq" StringSeq'new [#_"IPersistentMap" meta, #_"CharSequence" s, #_"int" i]
        (merge (ASeq'new meta)
            (hash-map
                #_"CharSequence" :s s
                #_"int" :i i
            )
        )
    )

    (defn #_"StringSeq" StringSeq'create [#_"CharSequence" s]
        (when (pos? (.length s))
            (StringSeq'new nil, s, 0)
        )
    )

    #_override
    (defn #_"StringSeq" IObj'''withMeta--StringSeq [#_"StringSeq" this, #_"IPersistentMap" meta]
        (when-not (= meta (.meta this)) => this
            (StringSeq'new meta, (:s this), (:i this))
        )
    )

    #_override
    (defn #_"Object" ISeq'''first--StringSeq [#_"StringSeq" this]
        (Character/valueOf (.charAt (:s this), (:i this)))
    )

    #_override
    (defn #_"ISeq" ISeq'''next--StringSeq [#_"StringSeq" this]
        (when (< (inc (:i this)) (.length (:s this)))
            (StringSeq'new (:_meta this), (:s this), (inc (:i this)))
        )
    )

    #_override
    (defn #_"int" IndexedSeq'''index--StringSeq [#_"StringSeq" this]
        (:i this)
    )

    #_override
    (defn #_"int" Counted'''count--StringSeq [#_"StringSeq" this]
        (- (.length (:s this)) (:i this))
    )
)
)

(java-ns cloiure.lang.TransformerIterator

(class-ns MultiIterator
    (defn #_"Iterator" MultiIterator'new [#_"Iterator[]" iters]
        (reify Iterator
            #_foreign
            (#_"boolean" hasNext [#_"Iterator" _self]
                (loop-when [#_"int" i 0] (< i (alength iters)) => true
                    (and (.hasNext (aget iters i)) (recur (inc i)))
                )
            )

            #_foreign
            (#_"Object" next [#_"Iterator" _self]
                (let [#_"Object[]" a (make-array Object (alength iters))]
                    (dotimes [#_"int" i (alength iters)]
                        (aset a i (.next (aget iters i)))
                    )
                    (ArraySeq'new a, 0)
                )
            )
        )
    )
)

(class-ns TransformerIterator
    (def- #_"Object" TransformerIterator'NONE (Object.))

    (defn- #_"Iterator" TransformerIterator'new [#_"IFn" xform, #_"Iterator" source, #_"boolean" multi?]
        (let [#_volatile #_"Queue" q (LinkedList.)
              #_"IFn" xf
                (cast IFn (.invoke xform,
                    (fn #_"Object"
                        ([] nil)
                        ([#_"Object" r] r)
                        ([#_"Object" r, #_"Object" o] (.add q, o) r)
                    )
                ))]
            (§ reify Iterator
                [#_volatile #_"Object" n TransformerIterator'NONE
                 #_volatile #_"boolean" completed? false]

                #_foreign
                (#_"boolean" hasNext [#_"Iterator" _self]
                    (loop []
                        (cond
                            (not (identical? n TransformerIterator'NONE))
                                true
                            (not (.isEmpty q))
                                (do
                                    (set! n (.remove q))
                                    (recur)
                                )
                            completed?
                                false
                            (.hasNext source)
                                (let [#_"Object" r
                                        (if multi?
                                            (.applyTo xf, (RT'cons nil, (.next source)))
                                            (.invoke xf, nil, (.next source))
                                        )]
                                    (when (RT'isReduced r)
                                        (.invoke xf, nil)
                                        (set! completed? true)
                                    )
                                    (recur)
                                )
                            :else
                                (do
                                    (.invoke xf, nil)
                                    (set! completed? true)
                                    (recur)
                                )
                        )
                    )
                )

                #_foreign
                (#_"Object" next [#_"Iterator" self]
                    (when (.hasNext self) => (throw (NoSuchElementException.))
                        (let [_ n]
                            (set! n TransformerIterator'NONE)
                            _
                        )
                    )
                )
            )
        )
    )

    (defn #_"Iterator" TransformerIterator'create [#_"IFn" xform, #_"Iterator" source]
        (TransformerIterator'new xform, source, false)
    )

    (defn #_"Iterator" TransformerIterator'createMulti [#_"IFn" xform, #_"List" sources]
        (let [#_"Iterator[]" iters (make-array Iterator (.size sources))]
            (dotimes [#_"int" i (.size sources)]
                (aset iters i (cast Iterator (.get sources, i)))
            )
            (TransformerIterator'new xform, (MultiIterator'new iters), true)
        )
    )
)
)

(java-ns cloiure.lang.Tuple

(class-ns Tuple
    (def #_"int" Tuple'MAX_SIZE 6)

    (defn #_"IPersistentVector" Tuple'create
        ([] PersistentVector'EMPTY)
        ([#_"Object" v0] (RT'vector v0))
        ([#_"Object" v0, #_"Object" v1] (RT'vector v0, v1))
        ([#_"Object" v0, #_"Object" v1, #_"Object" v2] (RT'vector v0, v1, v2))
        ([#_"Object" v0, #_"Object" v1, #_"Object" v2, #_"Object" v3] (RT'vector v0, v1, v2, v3))
        ([#_"Object" v0, #_"Object" v1, #_"Object" v2, #_"Object" v3, #_"Object" v4] (RT'vector v0, v1, v2, v3, v4))
        ([#_"Object" v0, #_"Object" v1, #_"Object" v2, #_"Object" v3, #_"Object" v4, #_"Object" v5] (RT'vector v0, v1, v2, v3, v4, v5))
    )
)
)

(java-ns cloiure.lang.Var

(class-ns TBox
    (defn #_"TBox" TBox'new [#_"Thread" t, #_"Object" val]
        (hash-map
            #_"Thread" :thread t
            #_volatile #_"Object" :val val
        )
    )
)

(class-ns Unbound
    (defn #_"Unbound" Unbound'new [#_"Var" v]
        (merge (AFn'new)
            (hash-map
                #_"Var" :v v
            )
        )
    )

    #_foreign
    (defn #_"String" toString---Unbound [#_"Unbound" this]
        (str "Unbound: " (:v this))
    )

    #_override
    (defn #_"Object" AFn'''throwArity--Unbound [#_"Unbound" this, #_"int" n]
        (throw (IllegalStateException. (str "Attempting to call unbound fn: " (:v this))))
    )
)

(class-ns Frame
    (defn #_"Frame" Frame'new [#_"Associative" bindings, #_"Frame" prev]
        (hash-map
            ;; Var->TBox
            #_"Associative" :bindings bindings
            ;; Var->val
            #_"Frame" :prev prev
        )
    )

    (def #_"Frame" Frame'TOP (Frame'new PersistentHashMap'EMPTY, nil))
)

(class-ns Var
    (def #_"ThreadLocal<Frame>" Var'dvals
        (proxy [ThreadLocal #_"<Frame>"] []
            #_foreign
            (#_"Frame" initialValue [#_"ThreadLocal<Frame>" #_this]
                Frame'TOP
            )
        )
    )

    (def #_"Keyword" Var'privateKey (Keyword'intern (Symbol'intern "private")))
    (def #_"IPersistentMap" Var'privateMeta (§ soon PersistentArrayMap'new (object-array [ Var'privateKey, Boolean/TRUE ])))
    (def #_"Keyword" Var'macroKey (Keyword'intern (Symbol'intern "macro")))
    (def #_"Keyword" Var'nameKey (Keyword'intern (Symbol'intern "name")))
    (def #_"Keyword" Var'nsKey (Keyword'intern (Symbol'intern "ns")))

    (defn #_"Var" Var'find [#_"Symbol" nsQualifiedSym]
        (when (some? (:ns nsQualifiedSym)) => (throw (IllegalArgumentException. "Symbol must be namespace-qualified"))
            (let [#_"Namespace" ns (Namespace'find (Symbol'intern (:ns nsQualifiedSym)))]
                (when (some? ns) => (throw (IllegalArgumentException. (str "No such namespace: " (:ns nsQualifiedSym))))
                    (Namespace''findInternedVar ns, (Symbol'intern (:name nsQualifiedSym)))
                )
            )
        )
    )

    #_method
    (defn #_"void" Var''setMeta [#_"Var" this, #_"IPersistentMap" m]
        ;; ensure these basis keys
        (§ soon .resetMeta this, (-> m (.assoc Var'nameKey, (:sym this)) (.assoc Var'nsKey, (:ns this))))
        nil
    )

    (defn #_"Var" Var'create
        ([               ] (Var'new nil, nil      ))
        ([#_"Object" root] (Var'new nil, nil, root))
    )

    (defn #_"Var" Var'new
        ([#_"Namespace" ns, #_"Symbol" sym]
            (let [this
                    (hash-map
                        #_mutable #_"IPersistentMap" :_meta nil
                        #_"Namespace" :ns ns
                        #_"Symbol" :sym sym

                        #_volatile #_"Object" :root (Unbound'new (§ cyc this))
                        #_volatile #_"boolean" :dynamic false
                        #_"AtomicBoolean" :threadBound (AtomicBoolean. false)
                    )]
                (Var''setMeta this, PersistentHashMap'EMPTY)
                this
            )
        )
        ([#_"Namespace" ns, #_"Symbol" sym, #_"Object" root]
            (let [this (Var'new ns, sym)]
                (§ set! (:root this) root)
                this
            )
        )
    )

    #_override
    (defn #_"IPersistentMap" IMeta'''meta--Var [#_"Var" this]
        (§ sync this
            (:_meta this)
        )
    )

    #_override
    (defn #_"IPersistentMap" IReference'''alterMeta--Var [#_"Var" this, #_"IFn" alter, #_"ISeq" args]
        (§ sync this
            (§ update! (:_meta this) #(cast IPersistentMap (.applyTo alter, (Cons'new %, args))))
        )
    )

    #_override
    (defn #_"IPersistentMap" IReference'''resetMeta--Var [#_"Var" this, #_"IPersistentMap" m]
        (§ sync this
            (§ set! (:_meta this) m)
        )
    )

    #_method
    (defn #_"Var" Var''setDynamic
        ([#_"Var" this] (Var''setDynamic this, true))
        ([#_"Var" this, #_"boolean" b] (§ set! (:dynamic this) b) this)
    )

    #_method
    (defn #_"boolean" Var''isDynamic [#_"Var" this]
        (:dynamic this)
    )

    #_foreign
    (defn #_"String" toString---Var [#_"Var" this]
        (if (some? (:ns this))
            (str "#'" (:name (:ns this)) "/" (:sym this))
            (str "#<Var: " (or (:sym this) "--unnamed--") ">")
        )
    )

    #_method
    (defn #_"boolean" Var''hasRoot [#_"Var" this]
        (not (instance? Unbound (:root this)))
    )

    #_method
    (defn #_"boolean" Var''isBound [#_"Var" this]
        (or (Var''hasRoot this) (and (.get (:threadBound this)) (.containsKey (:bindings (.get Var'dvals)), this)))
    )

    #_method
    (defn #_"Object" Var''get [#_"Var" this]
        (if (.get (:threadBound this)) (.deref this) (:root this))
    )

    #_method
    (defn #_"TBox" Var''getThreadBinding [#_"Var" this]
        (when (.get (:threadBound this))
            (when-let [#_"IMapEntry" e (.entryAt (:bindings (.get Var'dvals)), this)]
                (cast TBox (.val e))
            )
        )
    )

    #_override
    (defn #_"Object" IDeref'''deref--Var [#_"Var" this]
        (let [#_"TBox" b (Var''getThreadBinding this)]
            (if (some? b) (:val b) (:root this))
        )
    )

    #_method
    (defn #_"Object" Var''set [#_"Var" this, #_"Object" val]
        (let [#_"TBox" tb (Var''getThreadBinding this)]
            (when (some? tb) => (throw (IllegalStateException. (str "Can't change/establish root binding of: " (:sym this) " with set")))
                (when (= (Thread/currentThread) (:thread tb)) => (throw (IllegalStateException. (str "Can't set!: " (:sym this) " from non-binding thread")))
                    (§ set! (:val tb) val)
                )
            )
        )
    )

    #_method
    (defn #_"Object" Var''alter [#_"Var" this, #_"IFn" fn, #_"ISeq" args]
        (Var''set this, (.applyTo fn, (RT'cons (.deref this), args)))
        this
    )

    #_method
    (defn #_"void" Var''setMacro [#_"Var" this]
        (.alterMeta this, RT'assoc, (RT'list Var'macroKey, RT'T))
        nil
    )

    #_method
    (defn #_"boolean" Var''isMacro [#_"Var" this]
        (RT'booleanCast-1o (.valAt (.meta this), Var'macroKey))
    )

    #_method
    (defn #_"boolean" Var''isPublic [#_"Var" this]
        (not (RT'booleanCast-1o (.valAt (.meta this), Var'privateKey)))
    )

    #_method
    (defn #_"Object" Var''getRawRoot [#_"Var" this]
        (:root this)
    )

    #_method
    (defn #_"Object" Var''getTag [#_"Var" this]
        (.valAt (.meta this), RT'TAG_KEY)
    )

    #_method
    (defn #_"void" Var''setTag [#_"Var" this, #_"Symbol" tag]
        (.alterMeta this, RT'assoc, (RT'list RT'TAG_KEY, tag))
        nil
    )

    ;; binding root always clears macro flag
    #_method
    (defn #_"void" Var''bindRoot [#_"Var" this, #_"Object" root]
        (§ sync this
            (§ set! (:root this) root)
            (.alterMeta this, RT'dissoc, (RT'list Var'macroKey))
        )
        nil
    )

    #_method
    (defn #_"void" Var''swapRoot [#_"Var" this, #_"Object" root]
        (§ sync this
            (§ set! (:root this) root)
        )
        nil
    )

    #_method
    (defn #_"void" Var''unbindRoot [#_"Var" this]
        (§ sync this
            (§ set! (:root this) (Unbound'new (§ cyc this)))
        )
        nil
    )

    #_method
    (defn #_"void" Var''commuteRoot [#_"Var" this, #_"IFn" fn]
        (§ sync this
            (§ set! (:root this) (.invoke fn, (:root this)))
        )
        nil
    )

    #_method
    (defn #_"Object" Var''alterRoot [#_"Var" this, #_"IFn" fn, #_"ISeq" args]
        (§ sync this
            (§ set! (:root this) (.applyTo fn, (RT'cons (:root this), args)))
        )
    )

    (defn #_"Var" Var'intern
        ([#_"Namespace" ns, #_"Symbol" sym]
            (Namespace''intern ns, sym)
        )
        ([#_"Namespace" ns, #_"Symbol" sym, #_"Object" root]
            (Var'intern ns, sym, root, true)
        )
        ([#_"Namespace" ns, #_"Symbol" sym, #_"Object" root, #_"boolean" replaceRoot]
            (let [#_"Var" v (Namespace''intern ns, sym)]
                (when (or (not (Var''hasRoot v)) replaceRoot)
                    (Var''bindRoot v, root)
                )
                v
            )
        )
    )

    (defn #_"Var" Var'internPrivate [#_"String" nsName, #_"String" sym]
        (let [#_"Namespace" ns (Namespace'findOrCreate (Symbol'intern nsName)) #_"Var" v (Var'intern ns, (Symbol'intern sym))]
            (Var''setMeta v, Var'privateMeta)
            v
        )
    )

    (defn #_"void" Var'pushThreadBindings [#_"Associative" bindings]
        (let [#_"Frame" f (.get Var'dvals)]
            (loop-when [#_"Associative" m (:bindings f) #_"ISeq" s (.seq bindings)] (some? s) => (.set Var'dvals, (Frame'new m, f))
                (let [#_"IMapEntry" e (cast IMapEntry (.first s)) #_"Var" v (cast Var (.key e))]
                    (when-not (Var''isDynamic v)
                        (throw (IllegalStateException. (str "Can't dynamically bind non-dynamic var: " (:ns v) "/" (:sym v))))
                    )
                    (.set (:threadBound v), true)
                    (recur (.assoc m, v, (TBox'new (Thread/currentThread), (.val e))) (.next s))
                )
            )
        )
        nil
    )

    (defn #_"void" Var'popThreadBindings []
        (let [#_"Frame" f (:prev (.get Var'dvals))]
            (cond
                (nil? f)        (throw (IllegalStateException. "Pop without matching push"))
                (= f Frame'TOP) (.remove Var'dvals)
                :else           (.set Var'dvals, f)
            )
        )
        nil
    )

    (defn #_"Associative" Var'getThreadBindings []
        (let [#_"Frame" f (.get Var'dvals)]
            (loop-when [#_"IPersistentMap" m PersistentHashMap'EMPTY #_"ISeq" s (.seq (:bindings f))] (some? s) => m
                (let [#_"IMapEntry" e (cast IMapEntry (.first s)) #_"Var" v (cast Var (.key e)) #_"TBox" b (cast TBox (.val e))]
                    (recur (.assoc m, v, (:val b)) (.next s))
                )
            )
        )
    )

    #_method
    (defn #_"IFn" Var''fn [#_"Var" this]
        (cast IFn (.deref this))
    )

    #_foreign
    (defn #_"Object" call---Var [#_"Var" this]
        (.invoke this)
    )

    #_foreign
    (defn #_"void" run---Var [#_"Var" this]
        (.invoke this)
        nil
    )

    #_override
    (defn #_"Object" IFn'''invoke-1--Var [#_"Var" this]
        (.invoke (Var''fn this))
    )

    #_override
    (defn #_"Object" IFn'''invoke-2--Var [#_"Var" this, #_"Object" arg1]
        (.invoke (Var''fn this), arg1)
    )

    #_override
    (defn #_"Object" IFn'''invoke-3--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2]
        (.invoke (Var''fn this), arg1, arg2
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-4--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3]
        (.invoke (Var''fn this), arg1, arg2, arg3
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-5--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-6--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-7--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-8--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-9--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-10--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-11--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-12--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-13--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-14--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-15--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-16--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-17--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-18--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17
        )
    )

    #_override
    (defn #_"Object" IFn'''invoke-19--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-20--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-21--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20
        )
    )

    #_override
  #_(defn #_"Object" IFn'''invoke-22--Var [#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" arg10, #_"Object" arg11, #_"Object" arg12, #_"Object" arg13, #_"Object" arg14, #_"Object" arg15, #_"Object" arg16, #_"Object" arg17, #_"Object" arg18, #_"Object" arg19, #_"Object" arg20 & #_"Object..." args]
        (.invoke (Var''fn this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20,
            (cast Compiler'OBJECTS_CLASS args)
        )
    )

    #_override
    (defn #_"Object" IFn'''applyTo--Var [#_"Var" this, #_"ISeq" args]
        (.applyTo (Var''fn this), args)
    )
)
)

(java-ns cloiure.lang.Volatile

(class-ns Volatile
    (defn #_"Volatile" Volatile'new [#_"Object" val]
        (hash-map
            #_volatile #_"Object" :val val
        )
    )

    #_override
    (defn #_"Object" IDeref'''deref--Volatile [#_"Volatile" this]
        (:val this)
    )

    #_method
    (defn #_"Object" Volatile''reset [#_"Volatile" this, #_"Object" newval]
        (§ set! (:val this) newval)
    )
)
)

(java-ns cloiure.lang.RT

(class-ns RT
    (def #_"Boolean" RT'T Boolean/TRUE)
    (def #_"Boolean" RT'F Boolean/FALSE)

    ;; simple-symbol->class
    (def #_"IPersistentMap" RT'DEFAULT_IMPORTS (§ soon RT'map
        (object-array [
            (Symbol'intern "Boolean")                         Boolean
            (Symbol'intern "Byte")                            Byte
            (Symbol'intern "Character")                       Character
            (Symbol'intern "Class")                           Class
            (Symbol'intern "ClassLoader")                     ClassLoader
            (Symbol'intern "Compiler")                        Compiler
            (Symbol'intern "Double")                          Double
            (Symbol'intern "Enum")                            Enum
            (Symbol'intern "Float")                           Float
            (Symbol'intern "InheritableThreadLocal")          InheritableThreadLocal
            (Symbol'intern "Integer")                         Integer
            (Symbol'intern "Long")                            Long
            (Symbol'intern "Math")                            Math
            (Symbol'intern "Number")                          Number
            (Symbol'intern "Object")                          Object
            (Symbol'intern "Package")                         Package
            (Symbol'intern "Process")                         Process
            (Symbol'intern "ProcessBuilder")                  ProcessBuilder
            (Symbol'intern "Runtime")                         Runtime
            (Symbol'intern "RuntimePermission")               RuntimePermission
            (Symbol'intern "SecurityManager")                 SecurityManager
            (Symbol'intern "Short")                           Short
            (Symbol'intern "StackTraceElement")               StackTraceElement
            (Symbol'intern "StrictMath")                      StrictMath
            (Symbol'intern "String")                          String
            (Symbol'intern "StringBuffer")                    StringBuffer
            (Symbol'intern "StringBuilder")                   StringBuilder
            (Symbol'intern "System")                          System
            (Symbol'intern "Thread")                          Thread
            (Symbol'intern "ThreadGroup")                     ThreadGroup
            (Symbol'intern "ThreadLocal")                     ThreadLocal
            (Symbol'intern "Throwable")                       Throwable
            (Symbol'intern "Void")                            Void
            (Symbol'intern "Appendable")                      Appendable
            (Symbol'intern "CharSequence")                    CharSequence
            (Symbol'intern "Cloneable")                       Cloneable
            (Symbol'intern "Comparable")                      Comparable
            (Symbol'intern "Iterable")                        Iterable
            (Symbol'intern "Readable")                        Readable
            (Symbol'intern "Runnable")                        Runnable
            (Symbol'intern "Callable")                        Callable
            (Symbol'intern "BigInteger")                      BigInteger
            (Symbol'intern "BigDecimal")                      BigDecimal
            (Symbol'intern "ArithmeticException")             ArithmeticException
            (Symbol'intern "ArrayIndexOutOfBoundsException")  ArrayIndexOutOfBoundsException
            (Symbol'intern "ArrayStoreException")             ArrayStoreException
            (Symbol'intern "ClassCastException")              ClassCastException
            (Symbol'intern "ClassNotFoundException")          ClassNotFoundException
            (Symbol'intern "CloneNotSupportedException")      CloneNotSupportedException
            (Symbol'intern "EnumConstantNotPresentException") EnumConstantNotPresentException
            (Symbol'intern "Exception")                       Exception
            (Symbol'intern "IllegalAccessException")          IllegalAccessException
            (Symbol'intern "IllegalArgumentException")        IllegalArgumentException
            (Symbol'intern "IllegalMonitorStateException")    IllegalMonitorStateException
            (Symbol'intern "IllegalStateException")           IllegalStateException
            (Symbol'intern "IllegalThreadStateException")     IllegalThreadStateException
            (Symbol'intern "IndexOutOfBoundsException")       IndexOutOfBoundsException
            (Symbol'intern "InstantiationException")          InstantiationException
            (Symbol'intern "InterruptedException")            InterruptedException
            (Symbol'intern "NegativeArraySizeException")      NegativeArraySizeException
            (Symbol'intern "NoSuchFieldException")            NoSuchFieldException
            (Symbol'intern "NoSuchMethodException")           NoSuchMethodException
            (Symbol'intern "NullPointerException")            NullPointerException
            (Symbol'intern "NumberFormatException")           NumberFormatException
            (Symbol'intern "RuntimeException")                RuntimeException
            (Symbol'intern "SecurityException")               SecurityException
            (Symbol'intern "StringIndexOutOfBoundsException") StringIndexOutOfBoundsException
            (Symbol'intern "TypeNotPresentException")         TypeNotPresentException
            (Symbol'intern "UnsupportedOperationException")   UnsupportedOperationException
            (Symbol'intern "AbstractMethodError")             AbstractMethodError
            (Symbol'intern "AssertionError")                  AssertionError
            (Symbol'intern "ClassCircularityError")           ClassCircularityError
            (Symbol'intern "ClassFormatError")                ClassFormatError
            (Symbol'intern "Error")                           Error
            (Symbol'intern "ExceptionInInitializerError")     ExceptionInInitializerError
            (Symbol'intern "IllegalAccessError")              IllegalAccessError
            (Symbol'intern "IncompatibleClassChangeError")    IncompatibleClassChangeError
            (Symbol'intern "InstantiationError")              InstantiationError
            (Symbol'intern "InternalError")                   InternalError
            (Symbol'intern "LinkageError")                    LinkageError
            (Symbol'intern "NoClassDefFoundError")            NoClassDefFoundError
            (Symbol'intern "NoSuchFieldError")                NoSuchFieldError
            (Symbol'intern "NoSuchMethodError")               NoSuchMethodError
            (Symbol'intern "OutOfMemoryError")                OutOfMemoryError
            (Symbol'intern "StackOverflowError")              StackOverflowError
            (Symbol'intern "ThreadDeath")                     ThreadDeath
            (Symbol'intern "UnknownError")                    UnknownError
            (Symbol'intern "UnsatisfiedLinkError")            UnsatisfiedLinkError
            (Symbol'intern "UnsupportedClassVersionError")    UnsupportedClassVersionError
            (Symbol'intern "VerifyError")                     VerifyError
            (Symbol'intern "VirtualMachineError")             VirtualMachineError
            (Symbol'intern "Thread$UncaughtExceptionHandler") Thread$UncaughtExceptionHandler
            (Symbol'intern "Thread$State")                    Thread$State
            (Symbol'intern "Deprecated")                      Deprecated
            (Symbol'intern "Override")                        Override
            (Symbol'intern "SuppressWarnings")                SuppressWarnings
        ])
    ))

    (def #_"Namespace" RT'CLOIURE_NS (§ soon Namespace'findOrCreate (Symbol'intern "cloiure.core")))

    ;;;
     ; A java.io.Reader object representing standard input for read operations.
     ; Defaults to System/in, wrapped in a PushbackReader.
     ;;
    (def #_"Var" RT'IN (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*in*"), (PushbackReader. (InputStreamReader. System/in)))))
    ;;;
     ; A java.io.Writer object representing standard output for print operations.
     ; Defaults to System/out, wrapped in an OutputStreamWriter.
     ;;
    (def #_"Var" RT'OUT (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*out*"), (OutputStreamWriter. System/out))))
    ;;;
     ; A java.io.Writer object representing standard error for print operations.
     ; Defaults to System/err, wrapped in a PrintWriter.
     ;;
    (def #_"Var" RT'ERR (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*err*"), (PrintWriter. (OutputStreamWriter. System/err), true))))

    (def #_"Keyword" RT'TAG_KEY (Keyword'intern (Symbol'intern "tag")))

    (def #_"Var" RT'ASSERT (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*assert*"), RT'T)))
    (def #_"Var" RT'MATH_CONTEXT (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*math-context*"), nil)))

    (def #_"Keyword" RT'LINE_KEY (Keyword'intern (Symbol'intern "line")))
    (def #_"Keyword" RT'COLUMN_KEY (Keyword'intern (Symbol'intern "column")))
    (def #_"Keyword" RT'DECLARED_KEY (Keyword'intern (Symbol'intern "declared")))

    ;;;
     ; A cloiure.lang.Namespace object representing the current namespace.
     ;;
    (def #_"Var" RT'CURRENT_NS (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*ns*"), RT'CLOIURE_NS)))
    ;;;
     ; When set to true, output will be flushed whenever a newline is printed.
     ; Defaults to true.
     ;;
    (def #_"Var" RT'FLUSH_ON_NEWLINE (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*flush-on-newline*"), RT'T)))
    ;;;
     ; When set to logical false, strings and characters will be printed with
     ; non-alphanumeric characters converted to the appropriate escape sequences.
     ; Defaults to true.
     ;;
    (def #_"Var" RT'PRINT_READABLY (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*print-readably*"), RT'T)))
    ;;;
     ; When set to true, the compiler will emit warnings when reflection
     ; is needed to resolve Java method calls or field accesses.
     ; Defaults to false.
     ;;
    (def #_"Var" RT'WARN_ON_REFLECTION (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*warn-on-reflection*"), RT'F)))
    (def #_"Var" RT'ALLOW_UNRESOLVED_VARS (§ soon Var''setDynamic (Var'intern RT'CLOIURE_NS, (Symbol'intern "*allow-unresolved-vars*"), RT'F)))

    (def #_"Var" RT'IN_NS_VAR (§ soon Var'intern RT'CLOIURE_NS, (Symbol'intern "in-ns"), RT'F))
    (def #_"Var" RT'NS_VAR (§ soon Var'intern RT'CLOIURE_NS, (Symbol'intern "ns"), RT'F))
    (def #_"Var" RT'PRINT_INITIALIZED (§ soon Var'intern RT'CLOIURE_NS, (Symbol'intern "print-initialized")))
    (def #_"Var" RT'PR_ON (§ soon Var'intern RT'CLOIURE_NS, (Symbol'intern "pr-on")))

    (defn #_"Object" RT'inNamespace [#_"Object" arg1]
        (let [#_"Namespace" ns (Namespace'findOrCreate (cast Symbol arg1))]
            (Var''set RT'CURRENT_NS, ns)
            ns
        )
    )

    (defn #_"Object" RT'bootNamespace [#_"Object" __form, #_"Object" __env, #_"Object" arg1]
        (let [#_"Namespace" ns (Namespace'findOrCreate (cast Symbol arg1))]
            (Var''set RT'CURRENT_NS, ns)
            ns
        )
    )

    ;; duck typing stderr plays nice with e.g. swank
    (defn #_"PrintWriter" RT'errPrintWriter []
        (let [#_"Writer" w (cast Writer (.deref RT'ERR))]
            (if (instance? PrintWriter w) (cast PrintWriter w) (PrintWriter. w))
        )
    )

    (def #_"Object[]" RT'EMPTY_ARRAY (make-array Object 0))

    (def #_"Comparator" RT'DEFAULT_COMPARATOR
        (reify Comparator
            #_foreign
            (#_"int" compare [#_"Comparator" _self, #_"Object" o1, #_"Object" o2]
                (Util'compare o1, o2)
            )
        )
    )

    (def #_"AtomicInteger" RT'ID (AtomicInteger. 1))

    (§ static
        (Var''setTag RT'OUT, (Symbol'intern "java.io.Writer"))
        (Var''setTag RT'CURRENT_NS, (Symbol'intern "cloiure.lang.Namespace"))
        (Var''setTag RT'MATH_CONTEXT, (Symbol'intern "java.math.MathContext"))
    )

    (defn #_"Keyword" RT'keyword [#_"String" ns, #_"String" name]
        (Keyword'intern (Symbol'intern ns, name))
    )

    (defn #_"Var" RT'var
        ([#_"String" ns, #_"String" name]
            (Var'intern (Namespace'findOrCreate (Symbol'intern nil, ns)), (Symbol'intern nil, name))
        )
        ([#_"String" ns, #_"String" name, #_"Object" init]
            (Var'intern (Namespace'findOrCreate (Symbol'intern nil, ns)), (Symbol'intern nil, name), init)
        )
    )

    (defn #_"int" RT'nextID []
        (.getAndIncrement RT'ID)
    )

    (def- #_"int" RT'CHUNK_SIZE 32)

    (defn #_"ISeq" RT'chunkIteratorSeq [#_"Iterator" it]
        (when (.hasNext it)
            (LazySeq'new
                (fn #_"Object" []
                    (let [#_"Object[]" a (make-array Object RT'CHUNK_SIZE)
                          #_"int" n
                            (loop-when-recur [n 0] (and (.hasNext it) (< n RT'CHUNK_SIZE)) [(inc n)] => n
                                (aset a n (.next it))
                            )]
                        (ChunkedCons'new (ArrayChunk'new a, 0, n), (RT'chunkIteratorSeq it))
                    )
                )
            )
        )
    )

    (defn #_"ISeq" RT'seq [#_"Object" coll]
        (cond
            (instance? ASeq coll)    (cast ASeq coll)
            (instance? LazySeq coll) (.seq (cast LazySeq coll))
            :else                    (RT'seqFrom coll)
        )
    )

    ;; N.B. canSeq must be kept in sync with this!
    (defn #_"ISeq" RT'seqFrom [#_"Object" coll]
        (cond
            (instance? Seqable coll)      (.seq (cast Seqable coll))
            (nil? coll)                   nil
            (instance? Iterable coll)     (RT'chunkIteratorSeq (.iterator (cast Iterable coll)))
            (.isArray (.getClass coll))   (ArraySeq'createFromObject coll)
            (instance? CharSequence coll) (StringSeq'create (cast CharSequence coll))
            (instance? Map coll)          (RT'seq (.entrySet (cast Map coll)))
            :else (throw (IllegalArgumentException. (str "Don't know how to create ISeq from: " (.getName (.getClass coll)))))
        )
    )

    (defn #_"boolean" RT'canSeq [#_"Object" coll]
        (or
            (instance? ISeq coll)
            (instance? Seqable coll)
            (nil? coll)
            (instance? Iterable coll)
            (.isArray (.getClass coll))
            (instance? CharSequence coll)
            (instance? Map coll)
        )
    )

    (defn #_"Iterator" RT'iter [#_"Object" coll]
        (cond
            (instance? Iterable coll)
                (.iterator (cast Iterable coll))
            (nil? coll)
                (reify Iterator
                    #_foreign
                    (#_"boolean" hasNext [#_"Iterator" _self]
                        false
                    )

                    #_foreign
                    (#_"Object" next [#_"Iterator" _self]
                        (throw (NoSuchElementException.))
                    )
                )
            (instance? Map coll)
                (.iterator (.entrySet (cast Map coll)))
            (instance? String coll)
                (let [#_"String" s (cast String coll)]
                    (§ reify Iterator
                        [#_mutable #_"int" i 0]

                        #_foreign
                        (#_"boolean" hasNext [#_"Iterator" _self]
                            (< i (.length s))
                        )

                        #_foreign
                        (#_"Object" next [#_"Iterator" _self]
                            (let [_ (.charAt s, i)]
                                (update! i inc)
                                _
                            )
                        )
                    )
                )
            (.isArray (.getClass coll))
                (ArrayIter'createFromObject coll)
            :else
                (RT'iter (RT'seq coll))
        )
    )

    (defn #_"Object" RT'seqOrElse [#_"Object" o]
        (when (some? (RT'seq o))
            o
        )
    )

    (defn #_"ISeq" RT'keys [#_"Object" coll]
        (if (instance? IPersistentMap coll)
            (KeySeq'createFromMap (cast IPersistentMap coll))
            (KeySeq'create (RT'seq coll))
        )
    )

    (defn #_"ISeq" RT'vals [#_"Object" coll]
        (if (instance? IPersistentMap coll)
            (ValSeq'createFromMap (cast IPersistentMap coll))
            (ValSeq'create (RT'seq coll))
        )
    )

    (defn #_"IPersistentMap" RT'meta [#_"Object" x]
        (when (instance? IMeta x)
            (.meta (cast IMeta x))
        )
    )

    (defn #_"int" RT'count [#_"Object" o]
        (cond
            (instance? Counted o)
                (.count (cast Counted o))
            (nil? o)
                0
            (instance? IPersistentCollection o)
                (loop-when [#_"int" i 0 #_"ISeq" s (RT'seq o)] (some? s) => i
                    (when (instance? Counted s) => (recur (inc i) (.next s))
                        (+ i (.count s))
                    )
                )
            (instance? CharSequence o)
                (.length (cast CharSequence o))
            (instance? Collection o)
                (.size (cast Collection o))
            (instance? Map o)
                (.size (cast Map o))
            (instance? Map$Entry o)
                2
            (.isArray (.getClass o))
                (Array/getLength o)
            :else
                (throw (UnsupportedOperationException. (str "count not supported on this type: " (.getSimpleName (.getClass o)))))
        )
    )

    (defn #_"IPersistentCollection" RT'conj [#_"IPersistentCollection" coll, #_"Object" x]
        (if (some? coll) (.cons coll, x) (PersistentList'new x))
    )

    (defn #_"ISeq" RT'cons [#_"Object" x, #_"Object" coll]
        (cond
            (nil? coll)           (PersistentList'new x)
            (instance? ISeq coll) (Cons'new x, (cast ISeq coll))
            :else                 (Cons'new x, (RT'seq coll))
        )
    )

    (defn #_"Object" RT'first [#_"Object" x]
        (if (instance? ISeq x)
            (.first (cast ISeq x))
            (let [#_"ISeq" s (RT'seq x)]
                (when (some? s)
                    (.first s)
                )
            )
        )
    )

    (defn #_"Object" RT'second [#_"Object" x]
        (RT'first (RT'next x))
    )

    (defn #_"Object" RT'third [#_"Object" x]
        (RT'first (RT'next (RT'next x)))
    )

    (defn #_"Object" RT'fourth [#_"Object" x]
        (RT'first (RT'next (RT'next (RT'next x))))
    )

    (defn #_"ISeq" RT'next [#_"Object" x]
        (if (instance? ISeq x)
            (.next (cast ISeq x))
            (let [#_"ISeq" s (RT'seq x)]
                (when (some? s)
                    (.next s)
                )
            )
        )
    )

    (defn #_"ISeq" RT'more [#_"Object" x]
        (if (instance? ISeq x)
            (.more (cast ISeq x))
            (let [#_"ISeq" s (RT'seq x)]
                (if (some? s) (.more s) PersistentList'EMPTY)
            )
        )
    )

    (defn #_"Object" RT'peek [#_"Object" x]
        (when (some? x)
            (.peek (cast IPersistentStack x))
        )
    )

    (defn #_"Object" RT'pop [#_"Object" x]
        (when (some? x)
            (.pop (cast IPersistentStack x))
        )
    )

    (defn #_"Object" RT'get
        ([#_"Object" coll, #_"Object" key]
            (cond
                (instance? ILookup coll)
                    (.valAt (cast ILookup coll), key)
                (nil? coll)
                    nil
                (instance? Map coll)
                    (.get (cast Map coll), key)
                (instance? IPersistentSet coll)
                    (.get (cast IPersistentSet coll), key)
                (and (instance? Number key) (or (instance? String coll) (.isArray (.getClass coll))))
                    (let-when [#_"int" n (.intValue (cast Number key))] (< -1 n (RT'count coll))
                        (RT'nth coll, n)
                    )
                (instance? ITransientSet coll)
                    (.get (cast ITransientSet coll), key)
            )
        )
        ([#_"Object" coll, #_"Object" key, #_"Object" notFound]
            (cond
                (instance? ILookup coll)
                    (.valAt (cast ILookup coll), key, notFound)
                (nil? coll)
                    notFound
                (instance? Map coll)
                    (let [#_"Map" m (cast Map coll)]
                        (if (.containsKey m, key) (.get m, key) notFound)
                    )
                (instance? IPersistentSet coll)
                    (let [#_"IPersistentSet" s (cast IPersistentSet coll)]
                        (if (.contains s, key) (.get s, key) notFound)
                    )
                (and (instance? Number key) (or (instance? String coll) (.isArray (.getClass coll))))
                    (let [#_"int" n (.intValue (cast Number key))]
                        (if (< -1 n (RT'count coll)) (RT'nth coll, n) notFound)
                    )
                (instance? ITransientSet coll)
                    (let [#_"ITransientSet" s (cast ITransientSet coll)]
                        (if (.contains s, key) (.get s, key) notFound)
                    )
                :else
                    notFound
            )
        )
    )

    (defn #_"Associative" RT'assoc [#_"Object" coll, #_"Object" key, #_"Object" val]
        (if (some? coll)
            (.assoc (cast Associative coll), key, val)
            (PersistentArrayMap'new (object-array [ key, val ]))
        )
    )

    (defn #_"Object" RT'contains [#_"Object" coll, #_"Object" key]
        (cond
            (nil? coll)
                RT'F
            (instance? Associative coll)
                (if (.containsKey (cast Associative coll), key) RT'T RT'F)
            (instance? IPersistentSet coll)
                (if (.contains (cast IPersistentSet coll), key) RT'T RT'F)
            (instance? Map coll)
                (if (.containsKey (cast Map coll), key) RT'T RT'F)
            (instance? Set coll)
                (if (.contains (cast Set coll), key) RT'T RT'F)
            (and (instance? Number key) (or (instance? String coll) (.isArray (.getClass coll))))
                (let [#_"int" n (.intValue (cast Number key))]
                    (if (< -1 n (RT'count coll)) RT'T RT'F)
                )
            (instance? ITransientSet coll)
                (if (.contains (cast ITransientSet coll), key) RT'T RT'F)
            (instance? ITransientAssociative2 coll)
                (if (.containsKey (cast ITransientAssociative2 coll), key) RT'T RT'F)
            :else
                (throw (IllegalArgumentException. (str "contains? not supported on type: " (.getName (.getClass coll)))))
        )
    )

    (defn #_"Object" RT'find [#_"Object" coll, #_"Object" key]
        (cond
            (nil? coll)
                nil
            (instance? Associative coll)
                (.entryAt (cast Associative coll), key)
            (instance? Map coll)
                (let-when [#_"Map" m (cast Map coll)] (.containsKey m, key)
                    (MapEntry'create key, (.get m, key))
                )
            (instance? ITransientAssociative2 coll)
                (.entryAt (cast ITransientAssociative2 coll), key)
            :else
                (throw (IllegalArgumentException. (str "find not supported on type: " (.getName (.getClass coll)))))
        )
    )

    ;; takes a seq of key, val, key, val
    ;; returns tail starting at val of matching key if found, else nil

    (defn #_"ISeq" RT'findKey [#_"Keyword" key, #_"ISeq" keyvals]
        (loop-when keyvals (some? keyvals)
            (let-when [#_"ISeq" r (.next keyvals)] (some? r) => (throw (RuntimeException. "Malformed keyword argslist"))
                (when-not (= (.first keyvals) key) => r
                    (recur (.next r))
                )
            )
        )
    )

    (defn #_"Object" RT'dissoc [#_"Object" coll, #_"Object" key]
        (when (some? coll)
            (.without (cast IPersistentMap coll), key)
        )
    )

    (defn #_"Object" RT'nth
        ([#_"Object" coll, #_"int" n]
            (cond
                (instance? Indexed coll)
                    (.nth (cast Indexed coll), n)
                (nil? coll)
                    nil
                (instance? CharSequence coll)
                    (Character/valueOf (.charAt (cast CharSequence coll), n))
                (.isArray (.getClass coll))
                    (Reflector'prepRet (.getComponentType (.getClass coll)), (Array/get coll, n))
                (instance? Matcher coll)
                    (.group (cast Matcher coll), n)
                (instance? Map$Entry coll)
                    (let [#_"Map$Entry" e (cast Map$Entry coll)]
                        (case n 0 (.getKey e) 1 (.getValue e) (throw (IndexOutOfBoundsException.)))
                    )
                (instance? Sequential coll)
                    (loop-when [#_"int" i 0 #_"ISeq" s (RT'seq coll)] (and (<= i n) (some? s)) => (throw (IndexOutOfBoundsException.))
                        (recur-if (< i n) [(inc i) (.next s)] => (.first s))
                    )
                :else
                    (throw (UnsupportedOperationException. (str "nth not supported on this type: " (.getSimpleName (.getClass coll)))))
            )
        )
        ([#_"Object" coll, #_"int" n, #_"Object" notFound]
            (cond
                (instance? Indexed coll)
                    (.nth (cast Indexed coll), n, notFound)
                (nil? coll)
                    notFound
                (neg? n)
                    notFound
                (instance? CharSequence coll)
                    (let [#_"CharSequence" s (cast CharSequence coll)]
                        (if (< n (.length s)) (Character/valueOf (.charAt s, n)) notFound)
                    )
                (.isArray (.getClass coll))
                    (when (< n (Array/getLength coll)) => notFound
                        (Reflector'prepRet (.getComponentType (.getClass coll)), (Array/get coll, n))
                    )
                (instance? Matcher coll)
                    (let-when [#_"Matcher" m (cast Matcher coll)] (< n (.groupCount m)) => notFound
                        (.group m, n)
                    )
                (instance? Map$Entry coll)
                    (let [#_"Map$Entry" e (cast Map$Entry coll)]
                        (case n 0 (.getKey e) 1 (.getValue e) notFound)
                    )
                (instance? Sequential coll)
                    (loop-when [#_"int" i 0 #_"ISeq" s (RT'seq coll)] (and (<= i n) (some? s)) => notFound
                        (recur-if (< i n) [(inc i) (.next s)] => (.first s))
                    )
                :else
                    (throw (UnsupportedOperationException. (str "nth not supported on this type: " (.getSimpleName (.getClass coll)))))
            )
        )
    )

    (defn #_"Object" RT'assocN [#_"int" n, #_"Object" val, #_"Object" coll]
        (cond
            (nil? coll)
                nil
            (instance? IPersistentVector coll)
                (.assocN (cast IPersistentVector coll), n, val)
            (instance? Compiler'OBJECTS_CLASS coll)
                ;; hmm... this is not persistent
                (let [#_"Object[]" array (cast Compiler'OBJECTS_CLASS coll)]
                    (aset array n val)
                    array
                )
        )
    )

    (defn #_"boolean" RT'hasTag [#_"Object" o, #_"Object" tag]
        (Util'equals tag, (RT'get (RT'meta o), RT'TAG_KEY))
    )

    (defn #_"Object"    RT'box-1o [#_"Object"  x] x)
    (defn #_"Character" RT'box-1c [#_"char"    x] (Character/valueOf x))
    (defn #_"Object"    RT'box-1z [#_"boolean" x] (if x RT'T RT'F))
    (defn #_"Object"    RT'box-1Z [#_"Boolean" x] x)
    (defn #_"Number"    RT'box-1b [#_"byte"    x] x)
    (defn #_"Number"    RT'box-1s [#_"short"   x] x)
    (defn #_"Number"    RT'box-1i [#_"int"     x] x)
    (defn #_"Number"    RT'box-1l [#_"long"    x] x)
    (defn #_"Number"    RT'box-1f [#_"float"   x] x)
    (defn #_"Number"    RT'box-1d [#_"double"  x] x)

    (defn #_"char" RT'charCast-1b [#_"byte" x]
        (let [#_"char" i (char x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for char: " x)))
                i
            )
        )
    )

    (defn #_"char" RT'charCast-1s [#_"short" x]
        (let [#_"char" i (char x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for char: " x)))
                i
            )
        )
    )

    (defn #_"char" RT'charCast-1c [#_"char" x]
        x
    )

    (defn #_"char" RT'charCast-1i [#_"int" x]
        (let [#_"char" i (char x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for char: " x)))
                i
            )
        )
    )

    (defn #_"char" RT'charCast-1l [#_"long" x]
        (let [#_"char" i (char x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for char: " x)))
                i
            )
        )
    )

    (defn #_"char" RT'charCast-1f [#_"float" x]
        (when (<= Character/MIN_VALUE x Character/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for char: " x)))
            (char x)
        )
    )

    (defn #_"char" RT'charCast-1d [#_"double" x]
        (when (<= Character/MIN_VALUE x Character/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for char: " x)))
            (char x)
        )
    )

    (defn #_"char" RT'charCast-1o [#_"Object" x]
        (if (instance? Character x)
            (.charValue (cast Character x))
            (let [#_"long" n (.longValue (cast Number x))]
                (when (<= Character/MIN_VALUE n Character/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for char: " x)))
                    (char n)
                )
            )
        )
    )

    (defn #_"boolean" RT'booleanCast-1b [#_"boolean" x]
        x
    )

    (defn #_"boolean" RT'booleanCast-1o [#_"Object" x]
        (if (instance? Boolean x) (.booleanValue (cast Boolean x)) (some? x))
    )

    (defn #_"byte" RT'byteCast-1b [#_"byte" x]
        x
    )

    (defn #_"byte" RT'byteCast-1s [#_"short" x]
        (let [#_"byte" i (byte x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for byte: " x)))
                i
            )
        )
    )

    (defn #_"byte" RT'byteCast-1i [#_"int" x]
        (let [#_"byte" i (byte x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for byte: " x)))
                i
            )
        )
    )

    (defn #_"byte" RT'byteCast-1l [#_"long" x]
        (let [#_"byte" i (byte x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for byte: " x)))
                i
            )
        )
    )

    (defn #_"byte" RT'byteCast-1f [#_"float" x]
        (when (<= Byte/MIN_VALUE x Byte/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for byte: " x)))
            (byte x)
        )
    )

    (defn #_"byte" RT'byteCast-1d [#_"double" x]
        (when (<= Byte/MIN_VALUE x Byte/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for byte: " x)))
            (byte x)
        )
    )

    (defn #_"byte" RT'byteCast-1o [#_"Object" x]
        (if (instance? Byte x)
            (.byteValue (cast Byte x))
            (let [#_"long" n (RT'longCast-1o x)]
                (when (<= Byte/MIN_VALUE n Byte/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for byte: " x)))
                    (byte n)
                )
            )
        )
    )

    (defn #_"short" RT'shortCast-1b [#_"byte"  x] x)
    (defn #_"short" RT'shortCast-1s [#_"short" x] x)

    (defn #_"short" RT'shortCast-1i [#_"int" x]
        (let [#_"short" i (short x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for short: " x)))
                i
            )
        )
    )

    (defn #_"short" RT'shortCast-1l [#_"long" x]
        (let [#_"short" i (short x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for short: " x)))
                i
            )
        )
    )

    (defn #_"short" RT'shortCast-1f [#_"float" x]
        (when (<= Short/MIN_VALUE x Short/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for short: " x)))
            (short x)
        )
    )

    (defn #_"short" RT'shortCast-1d [#_"double" x]
        (when (<= Short/MIN_VALUE x Short/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for short: " x)))
            (short x)
        )
    )

    (defn #_"short" RT'shortCast-1o [#_"Object" x]
        (if (instance? Short x)
            (.shortValue (cast Short x))
            (let [#_"long" n (RT'longCast-1o x)]
                (when (<= Short/MIN_VALUE n Short/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for short: " x)))
                    (short n)
                )
            )
        )
    )

    (defn #_"int" RT'intCast-1b [#_"byte"  x] x)
    (defn #_"int" RT'intCast-1s [#_"short" x] x)
    (defn #_"int" RT'intCast-1c [#_"char"  x] x)
    (defn #_"int" RT'intCast-1i [#_"int"   x] x)

    (defn #_"int" RT'intCast-1l [#_"long" x]
        (let [#_"int" i (int x)]
            (when (= i x) => (throw (IllegalArgumentException. (str "Value out of range for int: " x)))
                i
            )
        )
    )

    (defn #_"int" RT'intCast-1f [#_"float" x]
        (when (<= Integer/MIN_VALUE x Integer/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for int: " x)))
            (int x)
        )
    )

    (defn #_"int" RT'intCast-1d [#_"double" x]
        (when (<= Integer/MIN_VALUE x Integer/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for int: " x)))
            (int x)
        )
    )

    (defn #_"int" RT'intCast-1o [#_"Object" x]
        (cond
            (instance? Integer x) (.intValue (cast Integer x))
            (instance? Number x)  (RT'intCast-1l (RT'longCast-1o x))
            :else                 (.charValue (cast Character x))
        )
    )

    (defn #_"long" RT'longCast-1b [#_"byte"  x] x)
    (defn #_"long" RT'longCast-1s [#_"short" x] x)
    (defn #_"long" RT'longCast-1i [#_"int"   x] x)
    (defn #_"long" RT'longCast-1l [#_"long"  x] x)

    (defn #_"long" RT'longCast-1f [#_"float" x]
        (when (<= Long/MIN_VALUE x Long/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for long: " x)))
            (long x)
        )
    )

    (defn #_"long" RT'longCast-1d [#_"double" x]
        (when (<= Long/MIN_VALUE x Long/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for long: " x)))
            (long x)
        )
    )

    (defn #_"long" RT'longCast-1o [#_"Object" x]
        (cond
            (or (instance? Integer x) (instance? Long x))
                (.longValue (cast Number x))
            (instance? BigInt x)
                (let [#_"BigInt" bi (cast BigInt x)]
                    (when (nil? (:bipart bi)) => (throw (IllegalArgumentException. (str "Value out of range for long: " x)))
                        (:lpart bi)
                    )
                )
            (instance? BigInteger x)
                (let [#_"BigInteger" bi (cast BigInteger x)]
                    (when (< (.bitLength bi) 64) => (throw (IllegalArgumentException. (str "Value out of range for long: " x)))
                        (.longValue bi)
                    )
                )
            (or (instance? Byte x) (instance? Short x))
                (.longValue (cast Number x))
            (instance? Ratio x)
                (RT'longCast-1o (Ratio''bigIntegerValue (cast Ratio x)))
            (instance? Character x)
                (RT'longCast-1l (.charValue (cast Character x)))
            :else
                (RT'longCast-1d (.doubleValue (cast Number x)))
        )
    )

    (defn #_"float" RT'floatCast-1b [#_"byte"  x] x)
    (defn #_"float" RT'floatCast-1s [#_"short" x] x)
    (defn #_"float" RT'floatCast-1i [#_"int"   x] x)
    (defn #_"float" RT'floatCast-1l [#_"long"  x] x)
    (defn #_"float" RT'floatCast-1f [#_"float" x] x)

    (defn #_"float" RT'floatCast-1d [#_"double" x]
        (when (<= (- Float/MAX_VALUE) x Float/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for float: " x)))
            (float x)
        )
    )

    (defn #_"float" RT'floatCast-1o [#_"Object" x]
        (if (instance? Float x)
            (.floatValue (cast Float x))
            (let [#_"double" n (.doubleValue (cast Number x))]
                (when (<= (- Float/MAX_VALUE) n Float/MAX_VALUE) => (throw (IllegalArgumentException. (str "Value out of range for float: " x)))
                    (float n)
                )
            )
        )
    )

    (defn #_"double" RT'doubleCast-1b [#_"byte"   x] x)
    (defn #_"double" RT'doubleCast-1s [#_"short"  x] x)
    (defn #_"double" RT'doubleCast-1i [#_"int"    x] x)
    (defn #_"double" RT'doubleCast-1l [#_"long"   x] x)
    (defn #_"double" RT'doubleCast-1f [#_"float"  x] x)
    (defn #_"double" RT'doubleCast-1d [#_"double" x] x)

    (defn #_"double" RT'doubleCast-1o [#_"Object" x]
        (.doubleValue (cast Number x))
    )

    (defn #_"byte" RT'uncheckedByteCast-1b [#_"byte"   x]       x )
    (defn #_"byte" RT'uncheckedByteCast-1s [#_"short"  x] (byte x))
    (defn #_"byte" RT'uncheckedByteCast-1i [#_"int"    x] (byte x))
    (defn #_"byte" RT'uncheckedByteCast-1l [#_"long"   x] (byte x))
    (defn #_"byte" RT'uncheckedByteCast-1f [#_"float"  x] (byte x))
    (defn #_"byte" RT'uncheckedByteCast-1d [#_"double" x] (byte x))

    (defn #_"byte" RT'uncheckedByteCast-1o [#_"Object" x]
        (.byteValue (cast Number x))
    )

    (defn #_"short" RT'uncheckedShortCast-1b [#_"byte"   x]        x )
    (defn #_"short" RT'uncheckedShortCast-1s [#_"short"  x]        x )
    (defn #_"short" RT'uncheckedShortCast-1i [#_"int"    x] (short x))
    (defn #_"short" RT'uncheckedShortCast-1l [#_"long"   x] (short x))
    (defn #_"short" RT'uncheckedShortCast-1f [#_"float"  x] (short x))
    (defn #_"short" RT'uncheckedShortCast-1d [#_"double" x] (short x))

    (defn #_"short" RT'uncheckedShortCast-1o [#_"Object" x]
        (.shortValue (cast Number x))
    )

    (defn #_"char" RT'uncheckedCharCast-1b [#_"byte"   x] (char x))
    (defn #_"char" RT'uncheckedCharCast-1s [#_"short"  x] (char x))
    (defn #_"char" RT'uncheckedCharCast-1c [#_"char"   x]       x )
    (defn #_"char" RT'uncheckedCharCast-1i [#_"int"    x] (char x))
    (defn #_"char" RT'uncheckedCharCast-1l [#_"long"   x] (char x))
    (defn #_"char" RT'uncheckedCharCast-1f [#_"float"  x] (char x))
    (defn #_"char" RT'uncheckedCharCast-1d [#_"double" x] (char x))

    (defn #_"char" RT'uncheckedCharCast-1o [#_"Object" x]
        (if (instance? Character x) (.charValue (cast Character x)) (char (.longValue (cast Number x))))
    )

    (defn #_"int" RT'uncheckedIntCast-1b [#_"byte"   x]      x )
    (defn #_"int" RT'uncheckedIntCast-1s [#_"short"  x]      x )
    (defn #_"int" RT'uncheckedIntCast-1c [#_"char"   x]      x )
    (defn #_"int" RT'uncheckedIntCast-1i [#_"int"    x]      x )
    (defn #_"int" RT'uncheckedIntCast-1l [#_"long"   x] (int x))
    (defn #_"int" RT'uncheckedIntCast-1f [#_"float"  x] (int x))
    (defn #_"int" RT'uncheckedIntCast-1d [#_"double" x] (int x))

    (defn #_"int" RT'uncheckedIntCast-1o [#_"Object" x]
        (if (instance? Number x) (.intValue (cast Number x)) (.charValue (cast Character x)))
    )

    (defn #_"long" RT'uncheckedLongCast-1b [#_"byte"   x]       x )
    (defn #_"long" RT'uncheckedLongCast-1s [#_"short"  x]       x )
    (defn #_"long" RT'uncheckedLongCast-1i [#_"int"    x]       x )
    (defn #_"long" RT'uncheckedLongCast-1l [#_"long"   x]       x )
    (defn #_"long" RT'uncheckedLongCast-1f [#_"float"  x] (long x))
    (defn #_"long" RT'uncheckedLongCast-1d [#_"double" x] (long x))

    (defn #_"long" RT'uncheckedLongCast-1o [#_"Object" x]
        (.longValue (cast Number x))
    )

    (defn #_"float" RT'uncheckedFloatCast-1b [#_"byte"   x]        x )
    (defn #_"float" RT'uncheckedFloatCast-1s [#_"short"  x]        x )
    (defn #_"float" RT'uncheckedFloatCast-1i [#_"int"    x]        x )
    (defn #_"float" RT'uncheckedFloatCast-1l [#_"long"   x]        x )
    (defn #_"float" RT'uncheckedFloatCast-1f [#_"float"  x]        x )
    (defn #_"float" RT'uncheckedFloatCast-1d [#_"double" x] (float x))

    (defn #_"float" RT'uncheckedFloatCast-1o [#_"Object" x]
        (.floatValue (cast Number x))
    )

    (defn #_"double" RT'uncheckedDoubleCast-1b [#_"byte"   x] x)
    (defn #_"double" RT'uncheckedDoubleCast-1s [#_"short"  x] x)
    (defn #_"double" RT'uncheckedDoubleCast-1i [#_"int"    x] x)
    (defn #_"double" RT'uncheckedDoubleCast-1l [#_"long"   x] x)
    (defn #_"double" RT'uncheckedDoubleCast-1f [#_"float"  x] x)
    (defn #_"double" RT'uncheckedDoubleCast-1d [#_"double" x] x)

    (defn #_"double" RT'uncheckedDoubleCast-1o [#_"Object" x]
        (.doubleValue (cast Number x))
    )

    (defn #_"IPersistentMap" RT'map [& #_"Object..." init]
        (cond
            (nil? init)
                PersistentArrayMap'EMPTY
            (<= (alength init) PersistentArrayMap'HASHTABLE_THRESHOLD)
                (PersistentArrayMap'createWithCheck init)
            :else
                (PersistentHashMap'createWithCheck-1a init)
        )
    )

    (defn #_"IPersistentMap" RT'mapUniqueKeys [& #_"Object..." init]
        (cond
            (nil? init)
                PersistentArrayMap'EMPTY
            (<= (alength init) PersistentArrayMap'HASHTABLE_THRESHOLD)
                (PersistentArrayMap'new init)
            :else
                (PersistentHashMap'create-1a init)
        )
    )

    (defn #_"IPersistentSet" RT'set [& #_"Object..." init]
        (PersistentHashSet'createWithCheck-1a init)
    )

    (defn #_"IPersistentVector" RT'vector [& #_"Object..." init]
        (LazilyPersistentVector'createOwning init)
    )

    (defn #_"IPersistentVector" RT'subvec [#_"IPersistentVector" v, #_"int" from, #_"int" over]
        (when (<= 0 from over (.count v)) => (throw (IndexOutOfBoundsException.))
            (if (< from over) (SubVector'new nil, v, from, over) PersistentVector'EMPTY)
        )
    )

    (defn #_"ISeq" RT'list
        ([] nil)
        ([#_"Object" arg1] (PersistentList'new arg1))
        ([#_"Object" arg1, #_"Object" arg2] (RT'listStar arg1, arg2, nil))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3] (RT'listStar arg1, arg2, arg3, nil))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4] (RT'listStar arg1, arg2, arg3, arg4, nil))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5] (RT'listStar arg1, arg2, arg3, arg4, arg5, nil))
    )

    (defn #_"ISeq" RT'listStar
        ([#_"Object" arg1, #_"ISeq" rest] (cast ISeq (RT'cons arg1, rest)))
        ([#_"Object" arg1, #_"Object" arg2, #_"ISeq" rest] (cast ISeq (RT'cons arg1, (RT'cons arg2, rest))))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"ISeq" rest] (cast ISeq (RT'cons arg1, (RT'cons arg2, (RT'cons arg3, rest)))))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"ISeq" rest] (cast ISeq (RT'cons arg1, (RT'cons arg2, (RT'cons arg3, (RT'cons arg4, rest))))))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"ISeq" rest] (cast ISeq (RT'cons arg1, (RT'cons arg2, (RT'cons arg3, (RT'cons arg4, (RT'cons arg5, rest)))))))
    )

    (defn #_"ISeq" RT'arrayToList [#_"Object[]" a]
        (loop-when-recur [#_"ISeq" s nil #_"int" i (dec (alength a))] (<= 0 i) [(cast ISeq (RT'cons (aget a i), s)) (dec i)] => s)
    )

    (defn #_"Object[]" RT'object_array [#_"Object" sizeOrSeq]
        (if (instance? Number sizeOrSeq)
            (make-array Object (.intValue (cast Number sizeOrSeq)))
            (let [#_"ISeq" s (RT'seq sizeOrSeq) #_"int" size (RT'count s) #_"Object[]" a (make-array Object size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (.next s)]
                    (aset a i (.first s))
                )
                a
            )
        )
    )

    (defn #_"Object[]" RT'toArray [#_"Object" coll]
        (cond
            (nil? coll)
                RT'EMPTY_ARRAY
            (instance? Compiler'OBJECTS_CLASS coll)
                (cast Compiler'OBJECTS_CLASS coll)
            (instance? Collection coll)
                (.toArray (cast Collection coll))
            (instance? Iterable coll)
                (let [#_"List" l (ArrayList.)]
                    (doseq [#_"Object" o (cast Iterable coll)]
                        (.add l, o)
                    )
                    (.toArray l)
                )
            (instance? Map coll)
                (.toArray (.entrySet (cast Map coll)))
            (instance? String coll)
                (let [#_"char[]" chars (.toCharArray (cast String coll))
                      #_"Object[]" a (make-array Object (alength chars))]
                    (dotimes [#_"int" i (alength chars)]
                        (aset a i (aget chars i))
                    )
                    a
                )
            (.isArray (.getClass coll))
                (let [#_"ISeq" s (RT'seq coll)
                      #_"Object[]" a (make-array Object (RT'count s))]
                    (loop-when-recur [#_"int" i 0 s s] (< i (alength a)) [(inc i) (.next s)]
                        (aset a i (.first s))
                    )
                    a
                )
            :else
                (throw (RuntimeException. (str "Unable to convert: " (.getClass coll) " to Object[]")))
        )
    )

    (defn #_"Object[]" RT'seqToArray [#_"ISeq" s]
        (let [#_"Object[]" a (make-array Object (RT'length s))]
            (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)]
                (aset a i (.first s))
            )
            a
        )
    )

    (defn #_"Object[]" RT'seqToPassedArray [#_"ISeq" s, #_"Object[]" passed]
        (let [#_"Object[]" a passed #_"int" n (RT'count s)
              a (if (< (alength a) n) (cast Compiler'OBJECTS_CLASS (Array/newInstance (.getComponentType (.getClass passed)), n)) a)]
            (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)]
                (aset a i (.first s))
            )
            (when (< n (alength passed))
                (aset a n nil)
            )
            a
        )
    )

    (defn #_"Object" RT'seqToTypedArray-2 [#_"Class" type, #_"ISeq" s]
        (let [#_"Object" a (Array/newInstance type, (RT'length s))]
            (condp = type
                Integer/TYPE
                    (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)]
                        (Array/set a, i, (RT'intCast-1o (.first s)))
                    )
                Byte/TYPE
                    (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)]
                        (Array/set a, i, (RT'byteCast-1o (.first s)))
                    )
                Float/TYPE
                    (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)]
                        (Array/set a, i, (RT'floatCast-1o (.first s)))
                    )
                Short/TYPE
                    (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)]
                        (Array/set a, i, (RT'shortCast-1o (.first s)))
                    )
                Character/TYPE
                    (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)]
                        (Array/set a, i, (RT'charCast-1o (.first s)))
                    )
                #_else
                    (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)]
                        (Array/set a, i, (.first s))
                    )
            )
            a
        )
    )

    (defn #_"Object" RT'seqToTypedArray-1 [#_"ISeq" s]
        (let [#_"Class" type (if (and (some? s) (some? (.first s))) (.getClass (.first s)) Object)]
            (RT'seqToTypedArray-2 type, s)
        )
    )

    (defn #_"int" RT'length [#_"ISeq" s]
        (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (.next s)] => i)
    )

    (defn #_"int" RT'boundedLength [#_"ISeq" s, #_"int" limit]
        (loop-when-recur [#_"int" i 0 s s] (and (some? s) (<= i limit)) [(inc i) (.next s)] => i)
    )

    (defn #_"boolean" RT'isReduced [#_"Object" r] (instance? Reduced r))

    (defn #_"String" RT'printString [#_"Object" x]
        (let [#_"StringWriter" sw (StringWriter.)]
            (RT'print x, sw)
            (.toString sw)
        )
    )

    (declare LispReader'read)

    (defn #_"Object" RT'readString [#_"String" s]
        (let [#_"PushbackReader" r (PushbackReader. (java.io.StringReader. s))]
            (LispReader'read r)
        )
    )

    (defn #_"void" RT'print [#_"Object" x, #_"Writer" w]
        (if (and (Var''isBound RT'PRINT_INITIALIZED) (RT'booleanCast-1o (.deref RT'PRINT_INITIALIZED)))
            (.invoke RT'PR_ON, x, w) ;; call multimethod
            (let [#_"boolean" readably (RT'booleanCast-1o (.deref RT'PRINT_READABLY))]
                (cond (nil? x)
                    (do
                        (.write w, "nil")
                    )
                    (or (instance? ISeq x) (instance? IPersistentList x))
                    (do
                        (.write w, \()
                        (RT'printInnerSeq (RT'seq x), w)
                        (.write w, \))
                    )
                    (instance? String x)
                        (let [#_"String" s (cast String x)]
                            (when readably => (.write w, s)
                                (.write w, \") ;; oops! "
                                (dotimes [#_"int" i (.length s)]
                                    (let [#_"char" c (.charAt s, i)]
                                        (case c
                                            \newline   (.write w, "\\n")
                                            \tab       (.write w, "\\t")
                                            \"         (.write w, "\\\"")
                                            \\         (.write w, "\\\\")
                                            \return    (.write w, "\\r")
                                            \formfeed  (.write w, "\\f")
                                            \backspace (.write w, "\\b")
                                                       (.write w, c)
                                        )
                                    )
                                )
                                (.write w, \") ;; oops! "
                            )
                        )
                    (instance? IPersistentMap x)
                    (do
                        (.write w, \{)
                        (loop-when-recur [#_"ISeq" s (RT'seq x)] (some? s) [(.next s)]
                            (let [#_"IMapEntry" e (cast IMapEntry (.first s))]
                                (RT'print (.key e), w)
                                (.write w, \space)
                                (RT'print (.val e), w)
                                (when (some? (.next s))
                                    (.write w, ", ")
                                )
                            )
                        )
                        (.write w, \})
                    )
                    (instance? IPersistentVector x)
                        (let [#_"IPersistentVector" a (cast IPersistentVector x)]
                            (.write w, \[)
                            (dotimes [#_"int" i (.count a)]
                                (RT'print (.nth a, i), w)
                                (when (< i (dec (.count a)))
                                    (.write w, \space)
                                )
                            )
                            (.write w, \])
                        )
                    (instance? IPersistentSet x)
                    (do
                        (.write w, "#{")
                        (loop-when-recur [#_"ISeq" s (RT'seq x)] (some? s) [(.next s)]
                            (RT'print (.first s), w)
                            (when (some? (.next s))
                                (.write w, \space)
                            )
                        )
                        (.write w, \})
                    )
                    (instance? Character x)
                        (let [#_"char" c (.charValue (cast Character x))]
                            (when readably => (.write w, c)
                                (.write w, \\)
                                (case c
                                    \newline   (.write w, "newline")
                                    \tab       (.write w, "tab")
                                    \space     (.write w, "space")
                                    \return    (.write w, "return")
                                    \formfeed  (.write w, "formfeed")
                                    \backspace (.write w, "backspace")
                                               (.write w, c)
                                )
                            )
                        )
                    (instance? Class x)
                    (do
                        (.write w, "#=")
                        (.write w, (.getName (cast Class x)))
                    )
                    (and (instance? BigDecimal x) readably)
                    (do
                        (.write w, (.toString x))
                        (.write w, \M)
                    )
                    (and (instance? BigInt x) readably)
                    (do
                        (.write w, (.toString x))
                        (.write w, \N)
                    )
                    (and (instance? BigInteger x) readably)
                    (do
                        (.write w, (.toString x))
                        (.write w, "BIGINT")
                    )
                    (instance? Var x)
                        (let [#_"Var" v (cast Var x)]
                            (.write w, (str "#=(var " (:name (:ns v)) "/" (:sym v) ")"))
                        )
                    (instance? Pattern x)
                        (let [#_"Pattern" p (cast Pattern x)]
                            (.write w, (str "#\"" (.pattern p) "\""))
                        )
                    :else
                    (do
                        (.write w, (.toString x))
                    )
                )
            )
        )
        nil
    )

    (defn- #_"void" RT'printInnerSeq [#_"ISeq" x, #_"Writer" w]
        (loop-when-recur [#_"ISeq" s x] (some? s) [(.next s)]
            (RT'print (.first s), w)
            (when (some? (.next s))
                (.write w, \space)
            )
        )
        nil
    )

    (defn #_"ClassLoader" RT'makeClassLoader []
        (cast ClassLoader
            (AccessController/doPrivileged
                (reify PrivilegedAction
                    #_foreign
                    (#_"Object" run [#_"PrivilegedAction" _self]
                        (DynamicClassLoader'new (RT'baseLoader))
                    )
                )
            )
        )
    )

    (defn #_"ClassLoader" RT'baseLoader []
        (if (Var''isBound Compiler'LOADER)
            (cast ClassLoader (.deref Compiler'LOADER))
            (.getContextClassLoader (Thread/currentThread))
        )
    )

    (defn #_"Class" RT'classForName
        ([#_"String" name] (RT'classForName name, true, (RT'baseLoader)))
        ([#_"String" name, #_"boolean" load?, #_"ClassLoader" loader]
            (let [#_"Class" c
                    (when-not (instance? DynamicClassLoader loader)
                        (DynamicClassLoader'findInMemoryClass name)
                    )]
                (or c (Class/forName name, load?, loader))
            )
        )
    )

    (defn #_"Class" RT'classForNameNonLoading [#_"String" name]
        (RT'classForName name, false, (RT'baseLoader))
    )

    (defn #_"Class" RT'loadClassForName [#_"String" name]
        (try
            (RT'classForNameNonLoading name)
            (RT'classForName name)
            (catch ClassNotFoundException _
                nil
            )
        )
    )

    (defn #_"boolean" RT'aget_boolean [#_"boolean[]" a, #_"int" i] (aget a i))
    (defn #_"byte"    RT'aget_byte    [#_"byte[]"    a, #_"int" i] (aget a i))
    (defn #_"short"   RT'aget_short   [#_"short[]"   a, #_"int" i] (aget a i))
    (defn #_"char"    RT'aget_char    [#_"char[]"    a, #_"int" i] (aget a i))
    (defn #_"int"     RT'aget_int     [#_"int[]"     a, #_"int" i] (aget a i))
    (defn #_"long"    RT'aget_long    [#_"long[]"    a, #_"int" i] (aget a i))
    (defn #_"float"   RT'aget_float   [#_"float[]"   a, #_"int" i] (aget a i))
    (defn #_"double"  RT'aget_double  [#_"double[]"  a, #_"int" i] (aget a i))
    (defn #_"Object"  RT'aget_object  [#_"Object[]"  a, #_"int" i] (aget a i))

    (defn #_"boolean" RT'aset_boolean [#_"boolean[]" a, #_"int" i, #_"boolean" v] (aset a i v) v)
    (defn #_"byte"    RT'aset_byte    [#_"byte[]"    a, #_"int" i, #_"byte"    v] (aset a i v) v)
    (defn #_"short"   RT'aset_short   [#_"short[]"   a, #_"int" i, #_"short"   v] (aset a i v) v)
    (defn #_"char"    RT'aset_char    [#_"char[]"    a, #_"int" i, #_"char"    v] (aset a i v) v)
    (defn #_"int"     RT'aset_int     [#_"int[]"     a, #_"int" i, #_"int"     v] (aset a i v) v)
    (defn #_"long"    RT'aset_long    [#_"long[]"    a, #_"int" i, #_"long"    v] (aset a i v) v)
    (defn #_"float"   RT'aset_float   [#_"float[]"   a, #_"int" i, #_"float"   v] (aset a i v) v)
    (defn #_"double"  RT'aset_double  [#_"double[]"  a, #_"int" i, #_"double"  v] (aset a i v) v)
    (defn #_"Object"  RT'aset_object  [#_"Object[]"  a, #_"int" i, #_"Object"  v] (aset a i v) v)

    (defn #_"int" RT'alength_boolean [#_"boolean[]" a] (alength a))
    (defn #_"int" RT'alength_byte    [#_"byte[]"    a] (alength a))
    (defn #_"int" RT'alength_short   [#_"short[]"   a] (alength a))
    (defn #_"int" RT'alength_char    [#_"char[]"    a] (alength a))
    (defn #_"int" RT'alength_int     [#_"int[]"     a] (alength a))
    (defn #_"int" RT'alength_long    [#_"long[]"    a] (alength a))
    (defn #_"int" RT'alength_float   [#_"float[]"   a] (alength a))
    (defn #_"int" RT'alength_double  [#_"double[]"  a] (alength a))
    (defn #_"int" RT'alength_object  [#_"Object[]"  a] (alength a))

    (defn #_"boolean[]" RT'aclone_boolean [#_"boolean[]" a] (.clone a))
    (defn #_"byte[]"    RT'aclone_byte    [#_"byte[]"    a] (.clone a))
    (defn #_"short[]"   RT'aclone_short   [#_"short[]"   a] (.clone a))
    (defn #_"char[]"    RT'aclone_char    [#_"char[]"    a] (.clone a))
    (defn #_"int[]"     RT'aclone_int     [#_"int[]"     a] (.clone a))
    (defn #_"long[]"    RT'aclone_long    [#_"long[]"    a] (.clone a))
    (defn #_"float[]"   RT'aclone_float   [#_"float[]"   a] (.clone a))
    (defn #_"double[]"  RT'aclone_double  [#_"double[]"  a] (.clone a))
    (defn #_"Object[]"  RT'aclone_object  [#_"Object[]"  a] (.clone a))
)
)
