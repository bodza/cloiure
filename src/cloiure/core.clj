(ns cloiure.core
    (:refer-clojure :only [*ns* *print-length* + - -> = alength aget aset assoc atom binding case char condp cons count declare defmacro defn defprotocol defrecord doseq dotimes extend-protocol extend-type fn hash-map hash-set identical? if-some import inc int-array intern key keys let letfn list locking loop make-array merge object-array pos? reify satisfies? the-ns to-array val vary-meta vec vector when-some while with-meta])
)

(defmacro § [& _])
(defmacro ß [& _])

(defmacro java-ns    [_ & s] (cons 'do s))
(defmacro class-ns   [_ & s] (cons 'do s))
(defmacro clojure-ns [_ & s] (cons 'do s))

(clojure.core/doseq [% (clojure.core/keys (clojure.core/ns-imports *ns*))] (clojure.core/ns-unmap *ns* %))

(import
    [java.lang ArithmeticException Boolean Byte Character CharSequence Class #_ClassCastException ClassLoader ClassNotFoundException Comparable Exception IndexOutOfBoundsException Integer Long NoSuchMethodException Number Object RuntimeException String StringBuilder System Thread ThreadLocal Throwable Void]
)

(def #_"Class" Object'array (Class/forName "[Ljava.lang.Object;"))

(import
    [java.io BufferedReader InputStreamReader OutputStreamWriter PrintWriter PushbackReader #_Reader #_StringReader StringWriter Writer]
    [java.lang.ref #_Reference ReferenceQueue SoftReference WeakReference]
    [java.lang.reflect Array #_Constructor #_Field #_Method Modifier]
    [java.security AccessController PrivilegedAction]
    [java.util Arrays Comparator IdentityHashMap]
    [java.util.regex Matcher Pattern]
    [cloiure.asm #_ClassVisitor ClassWriter Label #_MethodVisitor Opcodes Type]
    [cloiure.asm.commons GeneratorAdapter Method]
    [cloiure.math BigInteger]
    [cloiure.util.concurrent.atomic AtomicReference]
)

;;;
 ; A java.io.Reader object representing standard input for read operations.
 ; Defaults to System/in, wrapped in a PushbackReader.
 ;;
(def ^:dynamic *in* (PushbackReader. (InputStreamReader. System/in)))
;;;
 ; A java.io.Writer object representing standard output for print operations.
 ; Defaults to System/out, wrapped in an OutputStreamWriter.
 ;;
(def ^:dynamic *out* (OutputStreamWriter. System/out))
;;;
 ; A java.io.Writer object representing standard error for print operations.
 ; Defaults to System/err, wrapped in a PrintWriter.
 ;;
(def ^:dynamic *err* (PrintWriter. (OutputStreamWriter. System/err), true))

;;;
 ; When set to true, output will be flushed whenever a newline is printed.
 ; Defaults to true.
 ;;
(def ^:dynamic *flush-on-newline* true)
;;;
 ; When set to logical false, strings and characters will be printed with
 ; non-alphanumeric characters converted to the appropriate escape sequences.
 ; Defaults to true.
 ;;
(def ^:dynamic *print-readably* true)
;;;
 ; When set to true, the compiler will emit warnings when reflection
 ; is needed to resolve Java method calls or field accesses.
 ; Defaults to false.
 ;;
(def ^:dynamic *warn-on-reflection* false)

;;;
 ; Evaluates x and tests if it is an instance of class c. Returns true or false.
 ;;
(defn instance? [^Class c x] (.isInstance c x))

(defn class?   [x] (instance? Class x))
(defn boolean? [x] (instance? Boolean x))
(defn char?    [x] (instance? Character x))
(defn number?  [x] (instance? Number x))
(defn string?  [x] (instance? String x))

(defmacro throw! [^String s] `(throw (RuntimeException. ~s)))

(defmacro def-      [x & s] `(def      ~(vary-meta x assoc :private true) ~@s))
(defmacro defn-     [x & s] `(defn     ~(vary-meta x assoc :private true) ~@s))
(defmacro defmacro- [x & s] `(defmacro ~(vary-meta x assoc :private true) ~@s))

(defn identity   [x] x)
(defn constantly [x] (fn [& _] x))

(defn ^Boolean nil?   [x] (identical? x nil))
(defn ^Boolean false? [x] (identical? x false))
(defn ^Boolean true?  [x] (identical? x true))
(defn ^Boolean not    [x] (if x false true))
(defn ^Boolean some?  [x] (not (nil? x)))
(defn ^Boolean any?   [_] true)

;;;
 ; Evaluates test. If logical false, evaluates and returns then expr,
 ; otherwise else expr, if supplied, else nil.
 ;;
(defmacro if-not
    ([? then] (if-not ? then nil))
    ([? then else] (list 'if ? else then))
)

;;;
 ; Evaluates exprs one at a time, from left to right. If a form returns logical false
 ; (nil or false), and returns that value and doesn't evaluate any of the other expressions,
 ; otherwise it returns the value of the last expr. (and) returns true.
 ;;
(defmacro and
    ([] true)
    ([x] x)
    ([x & s] `(let [and# ~x] (if and# (and ~@s) and#)))
)

;;;
 ; Evaluates exprs one at a time, from left to right. If a form returns a logical true value,
 ; or returns that value and doesn't evaluate any of the other expressions, otherwise it returns
 ; the value of the last expression. (or) returns nil.
 ;;
(defmacro or
    ([] nil)
    ([x] x)
    ([x & s] `(let [or# ~x] (if or# or# (or ~@s))))
)

(java-ns cloiure.lang.Seqable
    (defprotocol Seqable
        (#_"ISeq" Seqable'''seq [#_"Seqable" this])
    )

    (extend-protocol Seqable
        nil                  (Seqable'''seq [_] nil)
        clojure.lang.Seqable (Seqable'''seq [o] (.seq o))
    )
)

(java-ns cloiure.lang.ISeq
    (defprotocol ISeq
        (#_"Object" ISeq'''first [#_"ISeq" this])
        (#_"ISeq" ISeq'''next [#_"ISeq" this])
    )

    (extend-protocol ISeq
        nil               (ISeq'''first [_] nil)        (ISeq'''next [_] nil)
        clojure.lang.ISeq (ISeq'''first [o] (.first o)) (ISeq'''next [o] (.next o))
    )
)

;;;
 ; Returns a seq on the collection. If the collection is empty, returns nil.
 ; (seq nil) returns nil. seq also works on strings, arrays (of reference types).
 ;;
(defn ^cloiure.core.ISeq seq [s] (Seqable'''seq s))

(defn seq? [x] (and (some? x) (satisfies? ISeq x)))

;;;
 ; Returns the first item in the collection. Calls seq on its argument.
 ; If s is nil, returns nil.
 ;;
(defn first [s]
    (if (seq? s)
        (ISeq'''first s)
        (when-some [s (seq s)]
            (ISeq'''first s)
        )
    )
)

;;;
 ; Returns a seq of the items after the first. Calls seq on its argument.
 ; If there are no more items, returns nil.
 ;;
(defn ^cloiure.core.ISeq next [s]
    (if (seq? s)
        (ISeq'''next s)
        (when-some [s (seq s)]
            (ISeq'''next s)
        )
    )
)

(defn second [s] (first (next s)))
(defn third  [s] (first (next (next s))))
(defn fourth [s] (first (next (next (next s)))))
(defn ffirst [s] (first (first s)))
(defn nnext  [s] (next (next s)))
(defn last   [s] (if-some [r (next s)] (recur r) (first s)))

;;;
 ; defs the supplied var names with no bindings, useful for making forward declarations.
 ;;
(§ defmacro declare [& names]
    `(do
        ~@(map #(list 'def (vary-meta % assoc :declared true)) names)
    )
)

(java-ns cloiure.lang.IObject
    (defprotocol IObject
        (#_"boolean" IObject'''equals [#_"IObject" this, #_"Object" that])
        (#_"int" IObject'''hashCode [#_"IObject" this])
        (#_"String" IObject'''toString [#_"IObject" this])
    )

    (extend-type nil IObject
        (#_"boolean" IObject'''equals [#_"nil" this, #_"Object" that]
            (nil? that)
        )

        (#_"int" IObject'''hashCode [#_"nil" this]
            0
        )

        (#_"String" IObject'''toString [#_"nil" this]
            "nil"
        )
    )

    (extend-type Object IObject
        (#_"boolean" IObject'''equals [#_"Object" this, #_"Object" that]
            (.equals this, that)
        )

        (#_"int" IObject'''hashCode [#_"Object" this]
            (.hashCode this)
        )

        (#_"String" IObject'''toString [#_"Object" this]
            (.toString this)
        )
    )
)

(java-ns cloiure.lang.IHashEq
    (defprotocol IHashEq
        (#_"int" IHashEq'''hasheq [#_"IHashEq" this])
    )
)

(java-ns cloiure.lang.Reflector
    #_stateless
    (defrecord Reflector [])
)

(java-ns cloiure.lang.Compiler
    (defprotocol Expr
        (#_"Object" Expr'''eval [#_"Expr" this])
        (#_"void" Expr'''emit [#_"Expr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen])
        (#_"Class" Expr'''getClass [#_"Expr" this])
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

    #_stateless
    (defrecord Recur [])
)

(java-ns cloiure.lang.Compiler
    (defrecord NilExpr            []) (extend-type NilExpr Expr Literal)
    (defrecord BooleanExpr        []) (extend-type BooleanExpr Expr Literal)
    (defrecord MonitorEnterExpr   []) (extend-type MonitorEnterExpr Expr Untyped)
    (defrecord MonitorExitExpr    []) (extend-type MonitorExitExpr Expr Untyped)
    (defrecord AssignExpr         []) (extend-type AssignExpr Expr)
    (defrecord ImportExpr         []) (extend-type ImportExpr Expr)
    (defrecord EmptyExpr          []) (extend-type EmptyExpr Expr)
    (defrecord ConstantExpr       []) (extend-type ConstantExpr Expr Literal)
    (defrecord NumberExpr         []) (extend-type NumberExpr Expr Literal MaybePrimitive)
    (defrecord StringExpr         []) (extend-type StringExpr Expr Literal)
    (defrecord KeywordExpr        []) (extend-type KeywordExpr Expr Literal)
    (defrecord InstanceFieldExpr  []) (extend-type InstanceFieldExpr Assignable Expr Interop MaybePrimitive)
    (defrecord StaticFieldExpr    []) (extend-type StaticFieldExpr Assignable Expr Interop MaybePrimitive)
    (defrecord InstanceMethodExpr []) (extend-type InstanceMethodExpr Expr Interop MaybePrimitive)
    (defrecord StaticMethodExpr   []) (extend-type StaticMethodExpr Expr Interop MaybePrimitive)
    (defrecord UnresolvedVarExpr  []) (extend-type UnresolvedVarExpr Expr)
    (defrecord VarExpr            []) (extend-type VarExpr Assignable Expr)
    (defrecord TheVarExpr         []) (extend-type TheVarExpr Expr)
    (defrecord BodyExpr           []) (extend-type BodyExpr Expr MaybePrimitive)
    (defrecord CatchClause        [])
    (defrecord TryExpr            []) (extend-type TryExpr Expr)
    (defrecord ThrowExpr          []) (extend-type ThrowExpr Expr Untyped)
    (defrecord NewExpr            []) (extend-type NewExpr Expr)
    (defrecord MetaExpr           []) (extend-type MetaExpr Expr)
    (defrecord IfExpr             []) (extend-type IfExpr Expr MaybePrimitive)
    (defrecord ListExpr           []) (extend-type ListExpr Expr)
    (defrecord MapExpr            []) (extend-type MapExpr Expr)
    (defrecord SetExpr            []) (extend-type SetExpr Expr)
    (defrecord VectorExpr         []) (extend-type VectorExpr Expr)
    (defrecord KeywordInvokeExpr  []) (extend-type KeywordInvokeExpr Expr)
    (defrecord InstanceOfExpr     []) (extend-type InstanceOfExpr Expr MaybePrimitive)
    (defrecord InvokeExpr         []) (extend-type InvokeExpr Expr)
    (defrecord LocalBinding       [])
    (defrecord LocalBindingExpr   []) (extend-type LocalBindingExpr Assignable Expr MaybePrimitive)
    (defrecord MethodParamExpr    []) (extend-type MethodParamExpr Expr MaybePrimitive)
    (defrecord FnMethod           []) (extend-type FnMethod IopMethod)
    (defrecord FnExpr             []) (extend-type FnExpr Expr IopObject)
    (defrecord DefExpr            []) (extend-type DefExpr Expr)
    (defrecord BindingInit        [])
    (defrecord LetFnExpr          []) (extend-type LetFnExpr Expr)
    (defrecord LetExpr            []) (extend-type LetExpr Expr MaybePrimitive)
    (defrecord RecurExpr          []) (extend-type RecurExpr Expr MaybePrimitive)
    (defrecord NewInstanceMethod  []) (extend-type NewInstanceMethod IopMethod)
    (defrecord NewInstanceExpr    []) (extend-type NewInstanceExpr Expr IopObject)
    (defrecord CaseExpr           []) (extend-type CaseExpr Expr MaybePrimitive)
)

(java-ns cloiure.lang.IFn
    (defprotocol IFn
        (#_"Object" IFn'''invoke
            [#_"IFn" this]
            [#_"IFn" this, #_"Object" arg1]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9]
            [#_"IFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9 & #_"Object..." args]
        )
        (#_"Object" IFn'''applyTo [#_"IFn" this, #_"ISeq" args])
    )
)

;;;
 ; Returns true if x implements IFn.
 ; Note that many data structures (e.g. sets and maps) implement IFn.
 ;;
(defn ifn? [x] (or (satisfies? IFn x) (instance? clojure.lang.IFn x)))

(java-ns cloiure.lang.Fn
    (defprotocol Fn)
)

;;;
 ; Returns true if x implements Fn, i.e. is an object created via fn.
 ;;
(defn fn? [x] (or (satisfies? Fn x) (instance? clojure.lang.Fn x)))

(java-ns cloiure.lang.IType
    (defprotocol IType)
)

(java-ns cloiure.lang.INamed
    (defprotocol INamed
        (#_"String" INamed'''getNamespace [#_"INamed" this])
        (#_"String" INamed'''getName [#_"INamed" this])
    )
)

(java-ns cloiure.lang.IMeta
    (defprotocol IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"IMeta" this])
    )
)

(java-ns cloiure.lang.IObj
    (defprotocol IObj
        (#_"IObj" IObj'''withMeta [#_"IObj" this, #_"IPersistentMap" meta])
    )
)

(java-ns cloiure.lang.IReference
    (defprotocol IReference
        (#_"IPersistentMap" IReference'''alterMeta [#_"IReference" this, #_"IFn" f, #_"ISeq" args])
        (#_"IPersistentMap" IReference'''resetMeta [#_"IReference" this, #_"IPersistentMap" m])
    )
)

(java-ns cloiure.lang.IDeref
    (defprotocol IDeref
        (#_"Object" IDeref'''deref [#_"IDeref" this])
    )
)

(java-ns cloiure.lang.IAtom
    (defprotocol IAtom
        (#_"boolean" IAtom'''compareAndSet [#_"IAtom" this, #_"Object" o, #_"Object" o'])
        (#_"Object" IAtom'''swap [#_"IAtom" this, #_"IFn" f, #_"ISeq" args])
        (#_"Object" IAtom'''reset [#_"IAtom" this, #_"Object" o'])
        (#_"[Object Object]" IAtom'''swapVals [#_"IAtom" this, #_"IFn" f, #_"ISeq" args])
        (#_"[Object Object]" IAtom'''resetVals [#_"IAtom" this, #_"Object" o'])
    )
)

(java-ns cloiure.lang.IPending
    (defprotocol IPending
        (#_"boolean" IPending'''isRealized [#_"IPending" this])
    )
)

(java-ns cloiure.lang.Sequential
    (defprotocol Sequential)
)

(defn sequential? [x] (or (satisfies? Sequential x) (instance? clojure.lang.Sequential x)))

(java-ns cloiure.lang.Reversible
    (defprotocol Reversible
        (#_"ISeq" Reversible'''rseq [#_"Reversible" this])
    )
)

(defn reversible? [x] (or (satisfies? Reversible x) (instance? clojure.lang.Reversible x)))

(java-ns cloiure.lang.Sorted
    (defprotocol Sorted
        (#_"Comparator" Sorted'''comparator [#_"Sorted" this])
        (#_"Object" Sorted'''entryKey [#_"Sorted" this, #_"Object" entry])
        (#_"ISeq" Sorted'''seq [#_"Sorted" this, #_"boolean" ascending?])
        (#_"ISeq" Sorted'''seqFrom [#_"Sorted" this, #_"Object" key, #_"boolean" ascending?])
    )
)

(defn sorted? [x] (or (satisfies? Sorted x) (instance? clojure.lang.Sorted x)))

(java-ns cloiure.lang.Counted
    (defprotocol Counted
        (#_"int" Counted'''count [#_"Counted" this])
    )
)

;;;
 ; Returns true if x implements count in constant time.
 ;;
(defn counted? [x] (or (satisfies? Counted x) (instance? clojure.lang.Counted x)))

(java-ns cloiure.lang.Indexed
    (defprotocol Indexed
        (#_"Object" Indexed'''nth
            [#_"Indexed" this, #_"int" i]
            [#_"Indexed" this, #_"int" i, #_"Object" notFound]
        )
    )
)

;;;
 ; Return true if x implements Indexed, indicating efficient lookup by index.
 ;;
(defn indexed? [x] (or (satisfies? Indexed x) (instance? clojure.lang.Indexed x)))

(java-ns cloiure.lang.ILookup
    (defprotocol ILookup
        (#_"Object" ILookup'''valAt
            [#_"ILookup" this, #_"Object" key]
            [#_"ILookup" this, #_"Object" key, #_"Object" notFound]
        )
    )
)

(java-ns cloiure.lang.ILookupSite
    (defprotocol ILookupSite
        (#_"ILookupThunk" ILookupSite'''fault [#_"ILookupSite" this, #_"Object" target])
    )
)

(java-ns cloiure.lang.ILookupThunk
    (defprotocol ILookupThunk
        (#_"Object" ILookupThunk'''get [#_"ILookupThunk" this, #_"Object" target])
    )
)

(java-ns cloiure.lang.IMapEntry
    (defprotocol IMapEntry
        (#_"Object" IMapEntry'''key [#_"IMapEntry" this])
        (#_"Object" IMapEntry'''val [#_"IMapEntry" this])
    )
)

(defn map-entry? [x] (or (satisfies? IMapEntry x) (instance? clojure.lang.IMapEntry x)))

(java-ns cloiure.lang.IPersistentCollection
    (defprotocol IPersistentCollection
        (#_"IPersistentCollection" IPersistentCollection'''conj [#_"IPersistentCollection" this, #_"Object" o])
        (#_"IPersistentCollection" IPersistentCollection'''empty [#_"IPersistentCollection" this])
    )
)

(defn coll? [x] (or (satisfies? IPersistentCollection x) (instance? clojure.lang.IPersistentCollection x)))

(java-ns cloiure.lang.IEditableCollection
    (defprotocol IEditableCollection
        (#_"ITransientCollection" IEditableCollection'''asTransient [#_"IEditableCollection" this])
    )
)

(defn editable? [x] (or (satisfies? IEditableCollection x) (instance? clojure.lang.IEditableCollection x)))

(java-ns cloiure.lang.Associative
    (defprotocol Associative
        (#_"Associative" Associative'''assoc [#_"Associative" this, #_"Object" key, #_"Object" val])
        (#_"boolean" Associative'''containsKey [#_"Associative" this, #_"Object" key])
        (#_"IMapEntry" Associative'''entryAt [#_"Associative" this, #_"Object" key])
    )
)

(defn associative? [x] (or (satisfies? Associative x) (instance? clojure.lang.Associative x)))

(java-ns cloiure.lang.IPersistentMap
    (defprotocol IPersistentMap
        (#_"IPersistentMap" IPersistentMap'''dissoc [#_"IPersistentMap" this, #_"Object" key])
    )
)

(defn map? [x] (or (satisfies? IPersistentMap x) (instance? clojure.lang.IPersistentMap x)))

(java-ns cloiure.lang.IPersistentSet
    (defprotocol IPersistentSet
        (#_"IPersistentSet" IPersistentSet'''disj [#_"IPersistentSet" this, #_"Object" key])
        (#_"boolean" IPersistentSet'''contains [#_"IPersistentSet" this, #_"Object" key])
        (#_"Object" IPersistentSet'''get [#_"IPersistentSet" this, #_"Object" key])
    )
)

(defn set? [x] (or (satisfies? IPersistentSet x) (instance? clojure.lang.IPersistentSet x)))

(java-ns cloiure.lang.IPersistentStack
    (defprotocol IPersistentStack
        (#_"Object" IPersistentStack'''peek [#_"IPersistentStack" this])
        (#_"IPersistentStack" IPersistentStack'''pop [#_"IPersistentStack" this])
    )
)

(java-ns cloiure.lang.IPersistentList
    (defprotocol IPersistentList)
)

(defn list? [x] (or (satisfies? IPersistentList x) (instance? clojure.lang.IPersistentList x)))

(java-ns cloiure.lang.IPersistentVector
    (defprotocol IPersistentVector
        (#_"IPersistentVector" IPersistentVector'''assocN [#_"IPersistentVector" this, #_"int" i, #_"Object" val])
    )
)

(defn vector? [x] (or (satisfies? IPersistentVector x) (instance? clojure.lang.IPersistentVector x)))

(java-ns cloiure.lang.ITransientCollection
    (defprotocol ITransientCollection
        (#_"ITransientCollection" ITransientCollection'''conj [#_"ITransientCollection" this, #_"Object" val])
        (#_"IPersistentCollection" ITransientCollection'''persistent [#_"ITransientCollection" this])
    )
)

(java-ns cloiure.lang.ITransientAssociative
    (defprotocol ITransientAssociative
        (#_"ITransientAssociative" ITransientAssociative'''assoc [#_"ITransientAssociative" this, #_"Object" key, #_"Object" val])
        (#_"boolean" ITransientAssociative'''containsKey [#_"ITransientAssociative" this, #_"Object" key])
        (#_"IMapEntry" ITransientAssociative'''entryAt [#_"ITransientAssociative" this, #_"Object" key])
    )
)

(java-ns cloiure.lang.ITransientMap
    (defprotocol ITransientMap
        (#_"ITransientMap" ITransientMap'''dissoc [#_"ITransientMap" this, #_"Object" key])
    )
)

(java-ns cloiure.lang.ITransientSet
    (defprotocol ITransientSet
        (#_"ITransientSet" ITransientSet'''disj [#_"ITransientSet" this, #_"Object" key])
        (#_"boolean" ITransientSet'''contains [#_"ITransientSet" this, #_"Object" key])
        (#_"Object" ITransientSet'''get [#_"ITransientSet" this, #_"Object" key])
    )
)

(java-ns cloiure.lang.ITransientVector
    (defprotocol ITransientVector
        (#_"ITransientVector" ITransientVector'''assocN [#_"ITransientVector" this, #_"int" i, #_"Object" val])
        (#_"ITransientVector" ITransientVector'''pop [#_"ITransientVector" this])
    )
)

(java-ns cloiure.lang.PersistentHashMap
    (defprotocol INode
        (#_"INode" INode'''assoc [#_"INode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"boolean'" addedLeaf])
        (#_"INode" INode'''dissoc [#_"INode" this, #_"int" shift, #_"int" hash, #_"Object" key])
        (#_"IMapEntry|Object" INode'''find
            [#_"INode" this, #_"int" shift, #_"int" hash, #_"Object" key]
            [#_"INode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" notFound]
        )
        (#_"ISeq" INode'''nodeSeq [#_"INode" this])
        (#_"INode" INode'''assocT [#_"INode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"boolean'" addedLeaf])
        (#_"INode" INode'''dissocT [#_"INode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"boolean'" removedLeaf])
        (#_"Object" INode'''kvreduce [#_"INode" this, #_"IFn" f, #_"Object" r])
    )
)

(java-ns cloiure.lang.IReduce
    (defprotocol IReduce
        (#_"Object" IReduce'''reduce
            [#_"IReduce" this, #_"IFn" f]
            [#_"IReduce" this, #_"IFn" f, #_"Object" r]
        )
    )
)

(java-ns cloiure.lang.IKVReduce
    (defprotocol IKVReduce
        (#_"Object" IKVReduce'''kvreduce [#_"IKVReduce" this, #_"IFn" f, #_"Object" r])
    )
)

(java-ns cloiure.lang.Range
    (defprotocol RangeBoundsCheck
        (#_"boolean" RangeBoundsCheck'''exceededBounds [#_"RangeBoundsCheck" this, #_"Object" val])
    )
)

(java-ns cloiure.lang.Util
    #_stateless
    (defrecord Util [])
)

(java-ns cloiure.lang.DynamicClassLoader
    (defrecord DynamicClassLoader #_"ClassLoader" [])
)

(java-ns cloiure.lang.Ratio
    (defrecord Ratio #_"Number" []) (extend-type Ratio #_"Comparable" IObject)
)

(java-ns cloiure.lang.Numbers
    (defprotocol Ops
        (#_"Ops" Ops'''combine [#_"Ops" this, #_"Ops" y])
        (#_"Ops" Ops'''opsWithLong [#_"Ops" this, #_"LongOps" x])
        (#_"Ops" Ops'''opsWithRatio [#_"Ops" this, #_"RatioOps" x])
        (#_"Ops" Ops'''opsWithBigInt [#_"Ops" this, #_"BigIntOps" x])
        (#_"boolean" Ops'''eq [#_"Ops" this, #_"Number" x, #_"Number" y])
        (#_"boolean" Ops'''lt [#_"Ops" this, #_"Number" x, #_"Number" y])
        (#_"boolean" Ops'''lte [#_"Ops" this, #_"Number" x, #_"Number" y])
        (#_"boolean" Ops'''isZero [#_"Ops" this, #_"Number" x])
        (#_"boolean" Ops'''isPos [#_"Ops" this, #_"Number" x])
        (#_"boolean" Ops'''isNeg [#_"Ops" this, #_"Number" x])
        (#_"Number" Ops'''add [#_"Ops" this, #_"Number" x, #_"Number" y])
        (#_"Number" Ops'''negate [#_"Ops" this, #_"Number" x])
        (#_"Number" Ops'''inc [#_"Ops" this, #_"Number" x])
        (#_"Number" Ops'''dec [#_"Ops" this, #_"Number" x])
        (#_"Number" Ops'''multiply [#_"Ops" this, #_"Number" x, #_"Number" y])
        (#_"Number" Ops'''divide [#_"Ops" this, #_"Number" x, #_"Number" y])
        (#_"Number" Ops'''quotient [#_"Ops" this, #_"Number" x, #_"Number" y])
        (#_"Number" Ops'''remainder [#_"Ops" this, #_"Number" x, #_"Number" y])
    )

    (defrecord LongOps []) (extend-type LongOps Ops)
    (defrecord RatioOps []) (extend-type RatioOps Ops)
    (defrecord BigIntOps []) (extend-type BigIntOps Ops)
    #_stateless
    (defrecord Numbers [])
)

(java-ns cloiure.lang.Atom
    (defrecord Atom []) (extend-type Atom IAtom IMeta IDeref IReference)
)

(java-ns cloiure.lang.Volatile
    (defrecord Volatile []) (extend-type Volatile IDeref)
)

(declare AFn'''throwArity)

(java-ns cloiure.lang.AFn
    #_abstract
    (defrecord AFn []) (extend-type AFn IFn) (§ soon
        #_abstract
        (#_"Object" AFn'''throwArity [#_"AFn" this, #_"int" n])
    )
)

(java-ns cloiure.lang.Symbol
    (defrecord Symbol #_"AFn" []) (extend-type Symbol #_"Comparable" IFn IHashEq IMeta INamed IObj IObject)
)

(defn symbol? [x] (or (instance? Symbol x) (instance? clojure.lang.Symbol x)))

(java-ns cloiure.lang.Keyword
    (defrecord Keyword []) (extend-type Keyword #_"Comparable" IFn IHashEq INamed IObject)
)

(defn keyword? [x] (or (instance? Keyword x) (instance? clojure.lang.Keyword x)))

(java-ns cloiure.lang.AFunction
    #_abstract
    (defrecord AFunction #_"AFn" []) (extend-type AFunction #_"Comparator" Fn IFn IMeta IObj)
)

(declare RestFn'''getRequiredArity)
(declare RestFn'''doInvoke)

(java-ns cloiure.lang.RestFn
    #_abstract
    (defrecord RestFn #_"AFunction" []) (extend-type RestFn #_"Comparator" Fn IFn IMeta IObj) (§ soon
        #_abstract
        (#_"int" RestFn'''getRequiredArity [#_"RestFn" this])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" args])
        #_abstract
        (#_"Object" RestFn'''doInvoke [#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9, #_"Object" args])
    )
)

(java-ns cloiure.lang.ASeq
    #_abstract
    (defrecord ASeq []) (extend-type ASeq IHashEq IMeta IObj IObject IPersistentCollection ISeq Seqable Sequential)
)

(java-ns cloiure.lang.LazySeq
    (defrecord LazySeq []) (extend-type LazySeq IHashEq IMeta IObj IObject IPending IPersistentCollection ISeq Seqable Sequential)
)

(java-ns cloiure.lang.APersistentMap
    #_abstract
    (defrecord APersistentMap #_"AFn" []) (extend-type APersistentMap Associative Counted IFn IHashEq ILookup IObject IPersistentCollection IPersistentMap Seqable)
)

(java-ns cloiure.lang.APersistentSet
    #_abstract
    (defrecord APersistentSet #_"AFn" []) (extend-type APersistentSet Counted IFn IHashEq IObject IPersistentCollection IPersistentSet Seqable)
)

(java-ns cloiure.lang.APersistentVector
    (defrecord VSeq #_"ASeq" []) (extend-type VSeq Counted IHashEq IMeta IObj IObject IPersistentCollection IReduce ISeq Seqable Sequential)
    (defrecord RSeq #_"ASeq" []) (extend-type RSeq Counted IHashEq IMeta IObj IObject IPersistentCollection ISeq Seqable Sequential)
    #_abstract
    (defrecord APersistentVector #_"AFn" []) (extend-type APersistentVector Associative #_"Comparable" Counted IFn IHashEq ILookup Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord SubVector #_"APersistentVector" []) (extend-type SubVector Associative #_"Comparable" Counted IFn IHashEq ILookup IMeta Indexed IObj IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
)

(java-ns cloiure.lang.AMapEntry
    #_abstract
    (defrecord AMapEntry #_"APersistentVector" []) (extend-type AMapEntry Associative #_"Comparable" Counted IFn IHashEq ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
)

(java-ns cloiure.lang.ArraySeq
    (defrecord ArraySeq #_"ASeq" []) (extend-type ArraySeq Counted IHashEq IMeta IObj IObject IPersistentCollection IReduce ISeq Seqable Sequential)
)

(declare ATransientMap'''ensureEditable)
(declare ATransientMap'''doAssoc)
(declare ATransientMap'''doDissoc)
(declare ATransientMap'''doValAt)
(declare ATransientMap'''doCount)
(declare ATransientMap'''doPersistent)

(java-ns cloiure.lang.ATransientMap
    #_abstract
    (defrecord ATransientMap #_"AFn" []) (extend-type ATransientMap Counted IFn ILookup ITransientAssociative ITransientCollection ITransientMap) (§ soon
        #_abstract
        (#_"void" ATransientMap'''ensureEditable [#_"ATransientMap" this])
        #_abstract
        (#_"ITransientMap" ATransientMap'''doAssoc [#_"ATransientMap" this, #_"Object" key, #_"Object" val])
        #_abstract
        (#_"ITransientMap" ATransientMap'''doDissoc [#_"ATransientMap" this, #_"Object" key])
        #_abstract
        (#_"Object" ATransientMap'''doValAt [#_"ATransientMap" this, #_"Object" key, #_"Object" notFound])
        #_abstract
        (#_"int" ATransientMap'''doCount [#_"ATransientMap" this])
        #_abstract
        (#_"IPersistentMap" ATransientMap'''doPersistent [#_"ATransientMap" this])
    )
)

(java-ns cloiure.lang.ATransientSet
    #_abstract
    (defrecord ATransientSet #_"AFn" []) (extend-type ATransientSet Counted IFn ITransientCollection ITransientSet)
)

(java-ns cloiure.lang.Cons
    (defrecord Cons #_"ASeq" []) (extend-type Cons Counted IHashEq IMeta IObj IObject IPersistentCollection ISeq Seqable Sequential)
)

(java-ns cloiure.lang.Cycle
    (defrecord Cycle #_"ASeq" []) (extend-type Cycle IHashEq IMeta IObj IObject IPending IPersistentCollection IReduce ISeq Seqable Sequential)
)

(java-ns cloiure.lang.Delay
    (defrecord Delay []) (extend-type Delay IDeref IPending)
)

(java-ns cloiure.lang.Iterate
    (defrecord Iterate #_"ASeq" []) (extend-type Iterate IHashEq IMeta IObj IObject IPending IPersistentCollection IReduce ISeq Seqable Sequential)
)

(java-ns cloiure.lang.KeywordLookupSite
    (defrecord KeywordLookupSite []) (extend-type KeywordLookupSite ILookupSite ILookupThunk)
)

(java-ns cloiure.lang.MapEntry
    (defrecord MapEntry #_"AMapEntry" []) (extend-type MapEntry Associative #_"Comparable" Counted IFn IHashEq ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
)

(java-ns cloiure.lang.MethodImplCache
    (defrecord Entry [])
    (defrecord MethodImplCache [])
)

(java-ns cloiure.lang.Namespace
    (defrecord Namespace []) (extend-type Namespace IObject)
)

(java-ns cloiure.lang.PersistentArrayMap
    (defrecord MSeq #_"ASeq" []) (extend-type MSeq Counted IHashEq IMeta IObj IObject IPersistentCollection ISeq Seqable Sequential)
    (defrecord TransientArrayMap #_"ATransientMap" []) (extend-type TransientArrayMap Counted IFn ILookup ITransientAssociative ITransientCollection ITransientMap)
    (defrecord PersistentArrayMap #_"APersistentMap" []) (extend-type PersistentArrayMap Associative Counted IEditableCollection IFn IHashEq IKVReduce ILookup IMeta IObj IObject IPersistentCollection IPersistentMap Seqable)
)

(java-ns cloiure.lang.PersistentHashMap
    (defrecord TransientHashMap #_"ATransientMap" []) (extend-type TransientHashMap Counted IFn ILookup ITransientAssociative ITransientCollection ITransientMap)
    (defrecord HSeq #_"ASeq" []) (extend-type HSeq IHashEq IMeta IObj IObject IPersistentCollection ISeq Seqable Sequential)
    (defrecord ArrayNode []) (extend-type ArrayNode INode)
    (defrecord BitmapIndexedNode []) (extend-type BitmapIndexedNode INode)
    (defrecord HashCollisionNode []) (extend-type HashCollisionNode INode)
    (defrecord NodeSeq #_"ASeq" []) (extend-type NodeSeq IHashEq IMeta IObj IObject IPersistentCollection ISeq Seqable Sequential)
    (defrecord PersistentHashMap #_"APersistentMap" []) (extend-type PersistentHashMap Associative Counted IEditableCollection IFn IHashEq IKVReduce ILookup IMeta IObj IObject IPersistentCollection IPersistentMap Seqable)
)

(java-ns cloiure.lang.PersistentHashSet
    (defrecord TransientHashSet #_"ATransientSet" []) (extend-type TransientHashSet Counted IFn ITransientCollection ITransientSet)
    (defrecord PersistentHashSet #_"APersistentSet" []) (extend-type PersistentHashSet Counted IEditableCollection IFn IHashEq IMeta IObj IObject IPersistentCollection IPersistentSet Seqable)
)

(java-ns cloiure.lang.PersistentList
    (defrecord Primordial #_"RestFn" []) (extend-type Primordial #_"Comparator" Fn IFn IMeta IObj)
    (defrecord EmptyList []) (extend-type EmptyList Counted IHashEq IMeta IObj IObject IPersistentCollection IPersistentList IPersistentStack ISeq Seqable Sequential)
    (defrecord PersistentList #_"ASeq" []) (extend-type PersistentList Counted IHashEq IMeta IObj IObject IPersistentCollection IPersistentList IPersistentStack IReduce ISeq Seqable Sequential)
)

(java-ns cloiure.lang.PersistentQueue
    (defrecord QSeq #_"ASeq" []) (extend-type QSeq Counted IHashEq IMeta IObj IObject IPersistentCollection ISeq Seqable Sequential)
    (defrecord PersistentQueue []) (extend-type PersistentQueue Counted IHashEq IMeta IObj IObject IPersistentCollection IPersistentList IPersistentStack Seqable Sequential)
)

(declare TNode'''left)
(declare TNode'''right)
(declare TNode'''addLeft)
(declare TNode'''addRight)
(declare TNode'''removeLeft)
(declare TNode'''removeRight)
(declare TNode'''blacken)
(declare TNode'''redden)
(declare TNode'''balanceLeft)
(declare TNode'''balanceRight)
(declare TNode'''replace)

(java-ns cloiure.lang.PersistentTreeMap
    #_abstract
    (defrecord TNode #_"AMapEntry" []) (extend-type TNode Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential) (§ soon
        #_abstract
        (#_"TNode" TNode'''left [#_"TNode" this])
        #_abstract
        (#_"TNode" TNode'''right [#_"TNode" this])
        #_abstract
        (#_"TNode" TNode'''addLeft [#_"TNode" this, #_"TNode" ins])
        #_abstract
        (#_"TNode" TNode'''addRight [#_"TNode" this, #_"TNode" ins])
        #_abstract
        (#_"TNode" TNode'''removeLeft [#_"TNode" this, #_"TNode" del])
        #_abstract
        (#_"TNode" TNode'''removeRight [#_"TNode" this, #_"TNode" del])
        #_abstract
        (#_"TNode" TNode'''blacken [#_"TNode" this])
        #_abstract
        (#_"TNode" TNode'''redden [#_"TNode" this])
        #_abstract
        (#_"TNode" TNode'''balanceLeft [#_"TNode" this, #_"TNode" parent])
        #_abstract
        (#_"TNode" TNode'''balanceRight [#_"TNode" this, #_"TNode" parent])
        #_abstract
        (#_"TNode" TNode'''replace [#_"TNode" this, #_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right])
    )
    (defrecord Black #_"TNode" []) (extend-type Black Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord BlackVal #_"Black" []) (extend-type BlackVal Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord BlackBranch #_"Black" []) (extend-type BlackBranch Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord BlackBranchVal #_"BlackBranch" []) (extend-type BlackBranchVal Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord Red #_"TNode" []) (extend-type Red Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord RedVal #_"Red" []) (extend-type RedVal Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord RedBranch #_"Red" []) (extend-type RedBranch Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord RedBranchVal #_"RedBranch" []) (extend-type RedBranchVal Associative #_"Comparable" Counted IFn IHashEq IKVReduce ILookup IMapEntry Indexed IObject IPersistentCollection IPersistentStack IPersistentVector Reversible Seqable Sequential)
    (defrecord TSeq #_"ASeq" []) (extend-type TSeq Counted IHashEq IMeta IObj IObject IPersistentCollection ISeq Seqable Sequential)
    (defrecord PersistentTreeMap #_"APersistentMap" []) (extend-type PersistentTreeMap Associative Counted IFn IHashEq IKVReduce ILookup IMeta IObj IObject IPersistentCollection IPersistentMap Reversible Seqable Sorted)
)

(java-ns cloiure.lang.PersistentTreeSet
    (defrecord PersistentTreeSet #_"APersistentSet" []) (extend-type PersistentTreeSet Counted IFn IHashEq IMeta IObj IObject IPersistentCollection IPersistentSet Reversible Seqable Sorted)
)

(java-ns cloiure.lang.PersistentVector
    (defrecord VNode [])
    (defrecord TransientVector #_"AFn" []) (extend-type TransientVector Counted IFn ILookup Indexed ITransientAssociative ITransientCollection ITransientVector)
    (defrecord PersistentVector #_"APersistentVector" []) (extend-type PersistentVector Associative #_"Comparable" Counted IEditableCollection IFn IHashEq IKVReduce ILookup IMeta Indexed IObj IObject IPersistentCollection IPersistentStack IPersistentVector IReduce Reversible Seqable Sequential)
)

(java-ns cloiure.lang.Repeat
    (defrecord Repeat #_"ASeq" []) (extend-type Repeat IHashEq IMeta IObj IObject IPersistentCollection IReduce ISeq Seqable Sequential)
)

(java-ns cloiure.lang.Range
    (defrecord Range #_"ASeq" []) (extend-type Range Counted IHashEq IMeta IObj IObject IPersistentCollection IReduce ISeq Seqable Sequential)
)

(java-ns cloiure.lang.Reduced
    (defrecord Reduced []) (extend-type Reduced IDeref)
)

(java-ns cloiure.lang.StringSeq
    (defrecord StringSeq #_"ASeq" []) (extend-type StringSeq Counted IHashEq IMeta IObj IObject IPersistentCollection IReduce ISeq Seqable Sequential)
)

(java-ns cloiure.lang.Tuple
    #_stateless
    (defrecord Tuple [])
)

(java-ns cloiure.lang.Var
    (defrecord Unbound #_"AFn" []) (extend-type Unbound IFn IObject)
    (defrecord Var []) (extend-type Var IDeref IFn IMeta IObject IReference)
)

(java-ns cloiure.lang.RT
    #_stateless
    (defrecord RT [])
)

(letfn [(=> [s] (if (= '=> (first s)) (next s) (cons nil s)))]
    (defmacro     when       [? & s] (let [[e & s] (=> s)]               `(if     ~? (do ~@s) ~e)))
    (defmacro     when-not   [? & s] (let [[e & s] (=> s)]               `(if-not ~? (do ~@s) ~e)))
    (defmacro let-when     [v ? & s] (let [[e & s] (=> s)] `(let ~(vec v) (if     ~? (do ~@s) ~e))))
    (defmacro let-when-not [v ? & s] (let [[e & s] (=> s)] `(let ~(vec v) (if-not ~? (do ~@s) ~e))))
)

;;;
 ; Takes a set of test/expr pairs. It evaluates each test one at a time.
 ; If a test returns logical true, cond evaluates and returns the value of the
 ; corresponding expr and doesn't evaluate any of the other tests or exprs.
 ; (cond) returns nil.
 ;;
(defmacro cond [& s]
    (when s
        `(if ~(first s)
            ~(when (next s) => (throw! "cond requires an even number of forms")
                (second s)
            )
            (cond ~@(nnext s))
        )
    )
)

(letfn [(v' [v] (cond (vector? v) v (symbol? v) [v v] :else [`_# v]))
        (r' [r] (cond (vector? r) `((recur ~@r)) (some? r) `((recur ~r))))
        (=> [s] (if (= '=> (first s)) (next s) (cons nil s)))
        (l' [v ? r s] (let [r (r' r) [e & s] (=> s)] `(loop ~(v' v) (if ~? (do ~@s ~@r) ~e))))]
    (defmacro loop-when [v ? & s] (l' v ? nil s))
    (defmacro loop-when-recur [v ? r & s] (l' v ? r s))
)

(letfn [(r' [r] (cond (vector? r) `(recur ~@r) (some? r) `(recur ~r)))
        (=> [s] (if (= '=> (first s)) (second s)))]
    (defmacro recur-if [? r & s] `(if ~? ~(r' r) ~(=> s)))
)

(defmacro cond-let [v r & s]
    (let [v (if (vector? v) v [`_# v]) e (when (seq s) `(cond-let ~@s))]
        `(if-some ~v ~r ~e)
    )
)

(defmacro any
    ([f x y] `(~f ~x ~y))
    ([f x y & z] `(let [f# ~f x# ~x _# (any f# x# ~y)] (if _# _# (any f# x# ~@z))))
)

;; naïve reduce to be redefined later with IReduce

(defn reduce
    ([f s] (if-some [s (seq s)] (reduce f (first s) (next s)) (f)))
    ([f r s] (if-some [s (seq s)] (recur f (f r (first s)) (next s)) r))
)

(declare persistent!)
(declare transient)

(defn reduce!
    ([f s] (if-some [s (seq s)] (reduce! f (first s) (next s)) (f)))
    ([f r s] (persistent! (reduce f (transient r) s)))
)

(declare conj!)
(declare conj)

(defn into [to from]
    (if (editable? to)
        (reduce! conj! to from)
        (reduce conj to from)
    )
)

(defmacro update! [x f & z] `(set! ~x (~f ~x ~@z)))

;;;
 ; Throws a ClassCastException if x is not a c, else returns x.
 ;;
(defn cast [^Class c x] (.cast c x))

;;;
 ; Returns the Class of x.
 ;;
(defn ^Class class [^Object x] (when (some? x) (.getClass x)))

(defn integer? [n]
    (or (instance? Long n)
        (instance? BigInteger n)
        (instance? Integer n)
        (instance? Byte n)
    )
)

;;;
 ; Returns the namespace String of a symbol or keyword, or nil if not present.
 ;;
(defn ^String namespace [^cloiure.core.INamed x] (INamed'''getNamespace x))

;;;
 ; Returns the name String of a string, symbol or keyword.
 ;;
(defn ^String name [x] (if (string? x) x (INamed'''getName ^cloiure.core.INamed x)))

;;;
 ; Returns the metadata of obj, returns nil if there is no metadata.
 ;;
(defn meta [x] (when (satisfies? IMeta x) (IMeta'''meta ^cloiure.core.IMeta x)))

;;;
 ; Returns an object of the same type and value as obj, with map m as its metadata.
 ;;
(§ defn with-meta [^cloiure.core.IObj x m] (IObj'''withMeta x m))

(declare apply)

;;;
 ; Returns an object of the same type and value as x,
 ; with (apply f (meta x) args) as its metadata.
 ;;
(§ defn vary-meta [x f & args] (with-meta x (apply f (meta x) args)))

;;;
 ; Atomically sets the metadata for a var/atom to be: (apply f its-current-meta args)
 ; f must be free of side-effects.
 ;;
(defn alter-meta! [^cloiure.core.IReference r f & args] (IReference'''alterMeta r f args))

;;;
 ; Atomically resets the metadata for a var/atom.
 ;;
(defn reset-meta! [^cloiure.core.IReference r m] (IReference'''resetMeta r m))

;;;
 ; When applied to a var or atom, returns its current state.
 ; When applied to a delay, forces it if not already forced.
 ; See also - realized?. Also reader macro: @.
 ;;
(defn deref [^cloiure.core.IDeref ref] (IDeref'''deref ref))

;;;
 ; Returns true if a value has been produced for a delay or lazy sequence.
 ;;
(defn realized? [^cloiure.core.IPending x] (IPending'''isRealized x))

;;;
 ; With no args, returns the empty string. With one arg x, returns x.toString().
 ; (str nil) returns the empty string.
 ; With more than one arg, returns the concatenation of the str values of the args.
 ;;
(defn ^String str
    ([] "")
    ([^Object x] (if (nil? x) "" (IObject'''toString x)))
    ([x & y]
        ((fn [^StringBuilder s z] (recur-if z [(.append s (str (first z))) (next z)] => (str s)))
            (StringBuilder. (str x)) y
        )
    )
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

            "public static boolean clojure.lang.RT.aget(boolean[],int)"                     Opcodes/BALOAD
            "public static byte clojure.lang.RT.aget(byte[],int)"                           Opcodes/BALOAD
            "public static char clojure.lang.RT.aget(char[],int)"                           Opcodes/CALOAD
            "public static int clojure.lang.RT.aget(int[],int)"                             Opcodes/IALOAD
            "public static long clojure.lang.RT.aget(long[],int)"                           Opcodes/LALOAD
            "public static java.lang.Object clojure.lang.RT.aget(java.lang.Object[],int)"   Opcodes/AALOAD

            "public static int clojure.lang.RT.alength(boolean[])"             Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength(byte[])"                Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength(char[])"                Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength(int[])"                 Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength(long[])"                Opcodes/ARRAYLENGTH
            "public static int clojure.lang.RT.alength(java.lang.Object[])"    Opcodes/ARRAYLENGTH

            "public static long clojure.lang.RT.longCast(byte)"                Opcodes/I2L
            "public static long clojure.lang.RT.longCast(int)"                 Opcodes/I2L
            "public static long clojure.lang.RT.longCast(long)"                Opcodes/NOP

            "public static int clojure.lang.RT.uncheckedIntCast(byte)"         Opcodes/NOP
            "public static int clojure.lang.RT.uncheckedIntCast(char)"         Opcodes/NOP
            "public static int clojure.lang.RT.uncheckedIntCast(int)"          Opcodes/NOP
            "public static int clojure.lang.RT.uncheckedIntCast(long)"         Opcodes/L2I

            "public static long clojure.lang.RT.uncheckedLongCast(byte)"       Opcodes/I2L
            "public static long clojure.lang.RT.uncheckedLongCast(int)"        Opcodes/I2L
            "public static long clojure.lang.RT.uncheckedLongCast(long)"       Opcodes/NOP
        )
    )

    ;; map to instructions terminated with comparator for branch to false
    (def #_"{String [int]}" Intrinsics'preds
        (hash-map
            "public static boolean clojure.lang.Numbers.equiv(long,long)"     [ Opcodes/LCMP  Opcodes/IFNE ]
            "public static boolean clojure.lang.Numbers.lt(long,long)"        [ Opcodes/LCMP  Opcodes/IFGE ]
            "public static boolean clojure.lang.Numbers.lte(long,long)"       [ Opcodes/LCMP  Opcodes/IFGT ]
            "public static boolean clojure.lang.Numbers.gt(long,long)"        [ Opcodes/LCMP  Opcodes/IFLE ]
            "public static boolean clojure.lang.Numbers.gte(long,long)"       [ Opcodes/LCMP  Opcodes/IFLT ]

            "public static boolean clojure.lang.Util.equiv(long,long)"        [ Opcodes/LCMP  Opcodes/IFNE ]
            "public static boolean clojure.lang.Util.equiv(boolean,boolean)"  [ Opcodes/IF_ICMPNE ]

            "public static boolean clojure.lang.Numbers.isZero(long)"         [ Opcodes/LCONST_0 Opcodes/LCMP  Opcodes/IFNE ]
            "public static boolean clojure.lang.Numbers.isPos(long)"          [ Opcodes/LCONST_0 Opcodes/LCMP  Opcodes/IFLE ]
            "public static boolean clojure.lang.Numbers.isNeg(long)"          [ Opcodes/LCONST_0 Opcodes/LCMP  Opcodes/IFGE ]
        )
    )
)
)

(java-ns cloiure.lang.Reflector

(class-ns Reflector
    (defn #_"Class" Reflector'classOf [#_"Object" o]
        (class o)
    )

    (defn #_"boolean" Reflector'isPrimitive [#_"Class" c]
        (and (some? c) (.isPrimitive c) (not (= c Void/TYPE)))
    )

    (declare <)

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

    (declare zero?)
    (declare nth)

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
        (let [unexpected! #(throw! (str "unexpected param type, expected: " c ", given: " (.getName (class arg))))]
            (cond
                (not (.isPrimitive c)) (cast c arg)
                (= c Boolean/TYPE)     (cast Boolean arg)
                (= c Character/TYPE)   (cast Character arg)
                (number? arg)
                    (condp = c
                        Integer/TYPE   (.intValue ^Number arg)
                        Long/TYPE      (.longValue ^Number arg)
                        Byte/TYPE      (.byteValue ^Number arg)
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
                    Integer/TYPE   (any = argType Integer Long/TYPE Long Byte/TYPE)
                    Long/TYPE      (any = argType Long Integer/TYPE Byte/TYPE)
                    Character/TYPE (= argType Character)
                    Byte/TYPE      (= argType Byte)
                    Boolean/TYPE   (= argType Boolean)
                                   false
                )
        )
    )

    (defn #_"boolean" Reflector'isCongruent [#_"Class[]" params, #_"Object[]" args]
        (when (some? args) => (zero? (alength params))
            (and (= (alength params) (alength args))
                (loop-when-recur [#_"boolean" ? true #_"int" i 0]
                                 (and ? (< i (alength params)))
                                 [(Reflector'paramArgTypeMatch (aget params i), (class (aget args i))) (inc i)]
                              => ?
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
            (when-some [#_"Class" sc (.getSuperclass c)]
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
        (let-when [#_"int" n (count methods)] (pos? n) => (throw! (str "no matching method found: " methodName (when (some? target) (str " for " (class target)))))
            (let [[#_"java.lang.reflect.Method" m #_"Object[]" boxedArgs]
                    (if (= n 1)
                        (let [m (nth methods 0)]
                            [m (Reflector'boxArgs (.getParameterTypes m), args)]
                        )
                        ;; overloaded w/same arity
                        (loop-when [#_"java.lang.reflect.Method" found nil boxedArgs nil #_"ISeq" s (seq methods)] (some? s) => [found boxedArgs]
                            (let [m (first s) #_"Class[]" params (.getParameterTypes m)
                                  [found boxedArgs]
                                    (if (and (Reflector'isCongruent params, args) (or (nil? found) (Reflector'subsumes params, (.getParameterTypes found))))
                                        [m (Reflector'boxArgs params, args)]
                                        [found boxedArgs]
                                    )]
                                (recur found boxedArgs (next s))
                            )
                        )
                    )]
                (when (some? m) => (throw! (str "no matching method found: " methodName (when (some? target) (str " for " (class target)))))
                    (let [m (when-not (Modifier/isPublic (.getModifiers (.getDeclaringClass m))) => m
                                ;; public method of non-public class, try to find it in hierarchy
                                (or (Reflector'getAsMethodOfPublicBase (class target), m)
                                    (throw! (str "can't call public method of non-public class: " m))
                                )
                            )]
                        (try
                            (Reflector'prepRet (.getReturnType m), (.invoke m, target, boxedArgs))
                            (catch Exception e
                                (throw (or (.getCause e) e))
                            )
                        )
                    )
                )
            )
        )
    )

    (defn #_"Object" Reflector'invokeInstanceMethod [#_"Object" target, #_"String" methodName, #_"Object[]" args]
        (let [#_"PersistentVector" methods (Reflector'getMethods (class target), (alength args), methodName, false)]
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
                    0   (throw! (str "no matching ctor found for " c))
                    1   (let [#_"Constructor" ctor (nth ctors 0)]
                            (.newInstance ctor, (Reflector'boxArgs (.getParameterTypes ctor), args))
                        )
                    (or ;; overloaded w/same arity
                        (loop-when-recur [#_"ISeq" s (seq ctors)] (some? s) [(next s)]
                            (let [#_"Constructor" ctor (first s)]
                                (let-when [#_"Class[]" params (.getParameterTypes ctor)] (Reflector'isCongruent params, args)
                                    (.newInstance ctor, (Reflector'boxArgs params, args))
                                )
                            )
                        )
                        (throw! (str "no matching ctor found for " c))
                    )
                )
            )
            (catch Exception e
                (throw (or (.getCause e) e))
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
            (when (some? f) => (throw! (str "no matching field found: " fieldName " for " c))
                (Reflector'prepRet (.getType f), (.get f, nil))
            )
        )
    )

    (defn #_"Object" Reflector'setStaticField [#_"Class" c, #_"String" fieldName, #_"Object" val]
        (let [#_"Field" f (Reflector'getField c, fieldName, true)]
            (when (some? f) => (throw! (str "no matching field found: " fieldName " for " c))
                (.set f, nil, (Reflector'boxArg (.getType f), val))
                val
            )
        )
    )

    (defn #_"Object" Reflector'getInstanceField [#_"Object" target, #_"String" fieldName]
        (let [#_"Class" c (class target) #_"Field" f (Reflector'getField c, fieldName, false)]
            (when (some? f) => (throw! (str "no matching field found: " fieldName " for " c))
                (Reflector'prepRet (.getType f), (.get f, target))
            )
        )
    )

    (defn #_"Object" Reflector'setInstanceField [#_"Object" target, #_"String" fieldName, #_"Object" val]
        (let [#_"Class" c (class target) #_"Field" f (Reflector'getField c, fieldName, false)]
            (when (some? f) => (throw! (str "no matching field found: " fieldName " for " (class target)))
                (.set f, target, (Reflector'boxArg (.getType f), val))
                val
            )
        )
    )

    (defn #_"Object" Reflector'invokeNoArgInstanceMember [#_"Object" target, #_"String" name, #_"boolean" requireField]
        (let [#_"Class" c (class target)]
            (if requireField
                (let [#_"Field" f (Reflector'getField c, name, false)]
                    (if (some? f)
                        (Reflector'getInstanceField target, name)
                        (throw! (str "no matching field found: " name " for " (class target)))
                    )
                )
                (let [#_"PersistentVector" methods (Reflector'getMethods c, 0, name, false)]
                    (if (pos? (count methods))
                        (Reflector'invokeMatchingMethod name, methods, target, (make-array Object 0))
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
    (def #_"Class" Compiler'CHARS_CLASS    (Class/forName "[C"))
    (def #_"Class" Compiler'INTS_CLASS     (Class/forName "[I"))
    (def #_"Class" Compiler'LONGS_CLASS    (Class/forName "[J"))

    (def #_"int" Compiler'MAX_POSITIONAL_ARITY 9)

    (def #_"String" Compiler'COMPILE_STUB_PREFIX "compile__stub")

    (def #_"Symbol" Compiler'FNONCE (with-meta 'fn* {:once true}))

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

        (#_"Class" Expr'''getClass [#_"NilExpr" this]
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

        (#_"Class" Expr'''getClass [#_"BooleanExpr" this]
            Boolean
        )
    )

    (def #_"BooleanExpr" Compiler'TRUE_EXPR (BooleanExpr'new true))
    (def #_"BooleanExpr" Compiler'FALSE_EXPR (BooleanExpr'new false))
)

(class-ns Compiler
    (def #_"Var" ^:dynamic *class-loader*      ) ;; DynamicClassLoader
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

    (def #_"[Method]" Compiler'createTupleMethods
        (vector
            (Method/getMethod "cloiure.core.IPersistentVector create()")
            (Method/getMethod "cloiure.core.IPersistentVector create(Object)")
            (Method/getMethod "cloiure.core.IPersistentVector create(Object, Object)")
            (Method/getMethod "cloiure.core.IPersistentVector create(Object, Object, Object)")
            (Method/getMethod "cloiure.core.IPersistentVector create(Object, Object, Object, Object)")
            (Method/getMethod "cloiure.core.IPersistentVector create(Object, Object, Object, Object, Object)")
            (Method/getMethod "cloiure.core.IPersistentVector create(Object, Object, Object, Object, Object, Object)")
        )
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
                (aset b n (Type/getType Object'array))
                (aset a (inc n) b)
                a
            )
        )
    )

    (def- #_"Type[]" Compiler'EXCEPTION_TYPES (make-array Type 0))

    (declare contains?)
    (declare Compiler'specials)

    (defn #_"boolean" Compiler'isSpecial [#_"Object" sym]
        (contains? Compiler'specials sym)
    )

    (defn #_"boolean" Compiler'inTailCall [#_"Context" context]
        (and (= context :Context'RETURN) *in-return-context* (not *in-catch-finally*))
    )

    (declare symbol)
    (declare Namespace''getAlias)
    (declare find-ns)

    (defn #_"Namespace" Compiler'namespaceFor
        ([#_"Symbol" sym] (Compiler'namespaceFor *ns*, sym))
        ([#_"Namespace" inns, #_"Symbol" sym]
            ;; note, presumes non-nil sym.ns
            (let [#_"Symbol" nsSym (symbol (:ns sym))]
                ;; first check against currentNS' aliases, otherwise check the Namespaces map
                (or (Namespace''getAlias inns, nsSym) (find-ns nsSym))
            )
        )
    )

    (declare int)
    (declare Namespace''getMapping)
    (declare var?)

    (defn #_"Symbol" Compiler'resolveSymbol [#_"Symbol" sym]
        ;; already qualified or classname?
        (cond
            (pos? (.indexOf (:name sym), (int \.)))
                sym
            (some? (:ns sym))
                (let [#_"Namespace" ns (Compiler'namespaceFor sym)]
                    (if (and (some? ns) (not (and (some? (:name (:name ns))) (= (:name (:name ns)) (:ns sym)))))
                        (symbol (:name (:name ns)) (:name sym))
                        sym
                    )
                )
            :else
                (let [#_"Object" o (Namespace''getMapping *ns*, sym)]
                    (cond
                        (nil? o)   (symbol (:name (:name *ns*)) (:name sym))
                        (class? o) (symbol (.getName ^Class o))
                        (var? o)   (symbol (:name (:name (:ns o))) (:name (:sym o)))
                    )
                )
        )
    )

    (defn #_"Class" Compiler'maybePrimitiveType [#_"Expr" e]
        (let-when [#_"Class" c (Expr'''getClass e)] (Reflector'isPrimitive c)
            (when (and (satisfies? MaybePrimitive e) (MaybePrimitive'''canEmitPrimitive e))
                c
            )
        )
    )

    (defn #_"Class" Compiler'maybeClass [#_"IPersistentVector" exprs]
        (loop-when [#_"Class" match nil #_"ISeq" s (seq exprs)] (some? s) => match
            (let [#_"Expr" e (first s)]
                (condp instance? e
                    NilExpr (recur-if (nil? match) [match (next s)])
                    ThrowExpr (recur match (next s))
                    (let [#_"Class" c (Expr'''getClass e)]
                        (recur-if (and (some? c) (any = match nil c)) [c (next s)])
                    )
                )
            )
        )
    )

    (defn #_"String" Compiler'getTypeStringForArgs [#_"IPersistentVector" args]
        (let [#_"StringBuilder" sb (StringBuilder.)]
            (dotimes [#_"int" i (count args)]
                (let [#_"Class" c (Expr'''getClass (nth args i))]
                    (when (pos? i)
                        (.append sb, ", ")
                    )
                    (.append sb, (if (some? c) (.getName c) "unknown"))
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
                                (let [#_"Class" aclass (Expr'''getClass (first s)) #_"Class" pclass (aget (nth pars i) p)
                                      [exact match]
                                        (if (and (some? aclass) (= aclass pclass))
                                            [(inc exact) match]
                                            [exact (Reflector'paramArgTypeMatch pclass, (or aclass Object))]
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
                (throw! (str "more than one matching method found: " methodName))
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
        (let [#_"String[]" a (to-array (keys Compiler'DEMUNGE_MAP)) _ (Arrays/sort a, #(- (count %2) (count %1)))
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

    (declare get)

    (defn #_"String" Compiler'munge [#_"String" name]
        (let [#_"StringBuilder" sb (StringBuilder.)]
            (doseq [#_"char" ch name]
                (.append sb, (or (get Compiler'CHAR_MAP ch) ch))
            )
            (.toString sb)
        )
    )

    (defn #_"String" Compiler'demunge [#_"String" mean]
        (let [#_"StringBuilder" sb (StringBuilder.)
              #_"Matcher" m (.matcher Compiler'DEMUNGE_PATTERN, mean)
              #_"int" i
                (loop-when [i 0] (.find m) => i
                    (let [#_"int" start (.start m) #_"int" end (.end m)]
                        ;; keep everything before the match
                        (.append sb, (.substring mean, i, start))
                        ;; replace the match with DEMUNGE_MAP result
                        (.append sb, (get Compiler'DEMUNGE_MAP (.group m)))
                        (recur end)
                    )
                )]
            ;; keep everything after the last match
            (.append sb, (.substring mean, i))
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
    (declare update)

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
        (when-some [#_"LocalBinding" lb (get *local-env* sym)]
            (Compiler'closeOver lb, *method*)
            lb
        )
    )

    (declare bound?)

    (defn- #_"int" Compiler'registerConstant [#_"Object" o]
        (when (bound? #'*constants*) => -1
            (or (.get *constant-ids*, o)
                (let [#_"int" n (count *constants*)]
                    (update! *constants* conj o)
                    (.put *constant-ids*, o, n)
                    n
                )
            )
        )
    )

    (declare dec)

    (defn- #_"int" Compiler'registerKeywordCallsite [#_"Keyword" k]
        (dec (count (update! *keyword-callsites* conj k)))
    )

    (defn- #_"int" Compiler'registerProtocolCallsite [#_"Var" v]
        (dec (count (update! *protocol-callsites* conj v)))
    )

    (defn- #_"void" Compiler'registerVar [#_"Var" var]
        (when (and (bound? #'*vars*) (nil? (get *vars* var)))
            (update! *vars* assoc var (Compiler'registerConstant var))
        )
        nil
    )

    (declare Namespace''intern)
    (declare Namespace''findInternedVar)

    (defn #_"Var" Compiler'lookupVar
        ([#_"Symbol" sym, #_"boolean" internNew] (Compiler'lookupVar sym, internNew, true))
        ([#_"Symbol" sym, #_"boolean" internNew, #_"boolean" registerMacro]
            ;; note - ns-qualified vars in other namespaces must already exist
            (let [#_"Var" var
                    (cond
                        (some? (:ns sym))
                            (when-some [#_"Namespace" ns (Compiler'namespaceFor sym)]
                                (let [#_"Symbol" name (symbol (:name sym))]
                                    (if (and internNew (= ns *ns*))
                                        (Namespace''intern ns, name)
                                        (Namespace''findInternedVar ns, name)
                                    )
                                )
                            )
                        (= sym 'ns)    #'ns
                        (= sym 'in-ns) #'in-ns
                        :else ;; is it mapped?
                            (let [#_"Object" o (Namespace''getMapping *ns*, sym)]
                                (cond
                                    (nil? o) ;; introduce a new var in the current ns
                                        (when internNew
                                            (Namespace''intern *ns*, (symbol (:name sym)))
                                        )
                                    (var? o)
                                        o
                                    :else
                                        (throw! (str "expecting var, but " sym " is mapped to " o))
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
                        (when (or (= (:ns v) *ns*) (not (get (meta v) :private))) => (throw! (str "var: " v " is private"))
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
                (when-some [#_"Var" v (if (var? op) op (Compiler'lookupVar op, false))]
                    (when (or (= (:ns v) *ns*) (not (get (meta v) :private))) => (throw! (str "var: " v " is private"))
                        (when-some [#_"IFn" f (get (meta v) :inline)]
                            (let [#_"IFn" arityPred (get (meta v) :inline-arities)]
                                (when (or (nil? arityPred) (IFn'''invoke arityPred, arity))
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
        (let-when [#_"Symbol" tag (Compiler'tagOf src)] (and (some? tag) (satisfies? IObj dst)) => dst
            (vary-meta dst assoc :tag tag)
        )
    )

    (defn #_"String" Compiler'destubClassName [#_"String" name]
        ;; skip over prefix + '.' or '/'
        (when (.startsWith name, Compiler'COMPILE_STUB_PREFIX) => name
            (.substring name, (inc (count Compiler'COMPILE_STUB_PREFIX)))
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

    (declare RT'classForName)

    (defn #_"Object" Compiler'resolveIn [#_"Namespace" n, #_"Symbol" sym, #_"boolean" allowPrivate]
        ;; note - ns-qualified vars must already exist
        (cond
            (some? (:ns sym))
                (let-when [#_"Namespace" ns (Compiler'namespaceFor n, sym)] (some? ns)                    => (throw! (str "no such namespace: " (:ns sym)))
                    (let-when [#_"Var" v (Namespace''findInternedVar ns, (symbol (:name sym)))] (some? v) => (throw! (str "no such var: " sym))
                        (when (or (= (:ns v) *ns*) (not (get (meta v) :private)) allowPrivate)            => (throw! (str "var: " sym " is private"))
                            v
                        )
                    )
                )
            (or (pos? (.indexOf (:name sym), (int \.))) (= (nth (:name sym) 0) \[)) (RT'classForName (:name sym))
            (= sym 'ns)                #'ns
            (= sym 'in-ns)             #'in-ns
            (= sym *compile-stub-sym*) *compile-stub-class*
            :else (or (Namespace''getMapping n, sym) (throw! (str "unable to resolve symbol: " sym " in this context")))
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
                (when-some [#_"Namespace" ns (Compiler'namespaceFor n, sym)]
                    (when-some [#_"Var" v (Namespace''findInternedVar ns, (symbol (:name sym)))]
                        v
                    )
                )
            (or (and (pos? (.indexOf (:name sym), (int \.))) (not (.endsWith (:name sym), "."))) (= (nth (:name sym) 0) \[))
                (RT'classForName (:name sym))
            (= sym 'ns)
                #'ns
            (= sym 'in-ns)
                #'in-ns
            :else
                (Namespace''getMapping n, sym)
        )
    )

    (defn #_"boolean" Compiler'inty [#_"Class" c] (any = c Integer/TYPE Byte/TYPE Character/TYPE))

    (defn #_"Class" Compiler'retType [#_"Class" tc, #_"Class" ret]
        (cond
            (nil? tc)
                ret
            (nil? ret)
                tc
            (and (.isPrimitive ret) (.isPrimitive tc))
                (when (or (and (Compiler'inty ret) (Compiler'inty tc)) (= ret tc)) => (throw! (str "cannot coerce " ret " to " tc ": use a cast instead"))
                    tc
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
                Character/TYPE Character
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
            (throw! "can't eval monitor-enter")
        )

        (#_"void" Expr'''emit [#_"MonitorEnterExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
            (.monitorEnter gen)
            (Expr'''emit Compiler'NIL_EXPR, context, objx, gen)
            nil
        )

        (#_"Class" Expr'''getClass [#_"MonitorEnterExpr" this]
            nil
        )
    )
)

(declare Compiler'analyze)

(class-ns MonitorEnterParser
    (defn #_"IParser" MonitorEnterParser'new []
        (reify IParser
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
            (throw! "can't eval monitor-exit")
        )

        (#_"void" Expr'''emit [#_"MonitorExitExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen)
            (.monitorExit gen)
            (Expr'''emit Compiler'NIL_EXPR, context, objx, gen)
            nil
        )

        (#_"Class" Expr'''getClass [#_"MonitorExitExpr" this]
            nil
        )
    )
)

(class-ns MonitorExitParser
    (defn #_"IParser" MonitorExitParser'new []
        (reify IParser
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

        (#_"Class" Expr'''getClass [#_"AssignExpr" this]
            (Expr'''getClass (:val this))
        )
    )
)

(class-ns AssignParser
    (defn #_"IParser" AssignParser'new []
        (reify IParser
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (= (count form) 3) => (throw! "malformed assignment, expecting (set! target val)")
                    (let [#_"Expr" target (Compiler'analyze :Context'EXPRESSION, (second form))]
                        (when (satisfies? Assignable target) => (throw! "invalid assignment target")
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

    (declare Namespace''importClass)
    (declare RT'classForNameNonLoading)

    (extend-type ImportExpr Expr
        (#_"Object" Expr'''eval [#_"ImportExpr" this]
            (Namespace''importClass *ns*, (RT'classForNameNonLoading (:c this)))
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

        (#_"Class" Expr'''getClass [#_"ImportExpr" this]
            nil
        )
    )
)

(class-ns ImportParser
    (defn #_"IParser" ImportParser'new []
        (reify IParser
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
            (condp satisfies? (:coll this)
                IPersistentList   (.getStatic gen, (Type/getType PersistentList),     "EMPTY", (Type/getType EmptyList))
                IPersistentVector (.getStatic gen, (Type/getType PersistentVector),   "EMPTY", (Type/getType PersistentVector))
                IPersistentMap    (.getStatic gen, (Type/getType PersistentArrayMap), "EMPTY", (Type/getType PersistentArrayMap))
                IPersistentSet    (.getStatic gen, (Type/getType PersistentHashSet),  "EMPTY", (Type/getType PersistentHashSet))
                                  (throw! "unknown collection type")
            )
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"EmptyExpr" this]
            (condp satisfies? (:coll this)
                IPersistentList   cloiure.core.IPersistentList
                IPersistentVector cloiure.core.IPersistentVector
                IPersistentMap    cloiure.core.IPersistentMap
                IPersistentSet    cloiure.core.IPersistentSet
                                  (throw! "unknown collection type")
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

        (#_"Class" Expr'''getClass [#_"ConstantExpr" this]
            (when (Modifier/isPublic (.getModifiers (class (:v this))))
                (condp instance? (:v this)
                    APersistentMap    APersistentMap
                    APersistentSet    APersistentSet
                    APersistentVector APersistentVector
                                      (class (:v this))
                )
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

        (#_"Class" Expr'''getClass [#_"NumberExpr" this]
            (condp instance? (:n this)
                Integer Long/TYPE
                Long    Long/TYPE
                        (throw! (str "unsupported Number type: " (.getName (class (:n this)))))
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
                (instance? Long (:n this))    (.push gen, (.longValue (:n this)))
            )
            nil
        )
    )

    (defn #_"Expr" NumberExpr'parse [#_"Number" form]
        (if (or (instance? Integer form) (instance? Long form))
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

        (#_"Class" Expr'''getClass [#_"StringExpr" this]
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

        (#_"Class" Expr'''getClass [#_"KeywordExpr" this]
            Keyword
        )
    )
)

(class-ns ConstantParser
    (defn #_"IParser" ConstantParser'new []
        (reify IParser
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"int" n (dec (count form))]
                    (when (= n 1) => (throw! (str "wrong number of arguments passed to quote: " n))
                        (let [#_"Object" v (second form)]
                            (cond
                                (nil? v)                          Compiler'NIL_EXPR
                                (= v true)                        Compiler'TRUE_EXPR
                                (= v false)                       Compiler'FALSE_EXPR
                                (number? v)                       (NumberExpr'parse v)
                                (string? v)                       (StringExpr'new v)
                                (and (coll? v) (zero? (count v))) (EmptyExpr'new v)
                                :else                             (ConstantExpr'new v)
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
                Character/TYPE (.invokeStatic gen, (Type/getType Character), (Method/getMethod "Character valueOf(char)"))
                Integer/TYPE   (.invokeStatic gen, (Type/getType Integer), (Method/getMethod "Integer valueOf(int)"))
                Long/TYPE      (.invokeStatic gen, (Type/getType Numbers), (Method/getMethod "Number num(long)"))
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
                                Long/TYPE    (Method/getMethod "long longCast(Object)")
                              #_Byte/TYPE  #_(Method/getMethod "byte byteCast(Object)")
                                           #_nil
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
            (class? form)
                form
            (symbol? form)
                (when (nil? (:ns form)) ;; if ns-qualified can't be classname
                    (cond
                        (= form *compile-stub-sym*)
                            *compile-stub-class*
                        (or (pos? (.indexOf (:name form), (int \.))) (= (nth (:name form) 0) \[))
                            (RT'classForNameNonLoading (:name form))
                        :else
                            (let [#_"Object" o (Namespace''getMapping *ns*, form)]
                                (cond
                                    (class? o)
                                        o
                                    (contains? *local-env* form)
                                        nil
                                    :else
                                        (try
                                            (RT'classForNameNonLoading (:name form))
                                            (catch Exception _
                                                nil
                                            )
                                        )
                                )
                            )
                    )
                )
            (and stringOk (string? form))
                (RT'classForNameNonLoading form)
        )
    )

    (defn #_"Class" Interop'primClassForName [#_"Symbol" sym]
        (when (some? sym)
            (case (:name sym)
                "boolean" Boolean/TYPE
                "byte"    Byte/TYPE
                "char"    Character/TYPE
                "int"     Integer/TYPE
                "long"    Long/TYPE
                "void"    Void/TYPE
                          nil
            )
        )
    )

    (defn #_"Class" Interop'maybeSpecialTag [#_"Symbol" sym]
        (or (Interop'primClassForName sym)
            (case (:name sym)
                "booleans" Compiler'BOOLEANS_CLASS
                "bytes"    Compiler'BYTES_CLASS
                "chars"    Compiler'CHARS_CLASS
                "ints"     Compiler'INTS_CLASS
                "longs"    Compiler'LONGS_CLASS
                "objects"  Object'array
                           nil
            )
        )
    )

    (defn #_"Class" Interop'tagToClass [#_"Object" tag]
        (or
            (when (and (symbol? tag) (nil? (:ns tag))) ;; if ns-qualified can't be classname
                (Interop'maybeSpecialTag tag)
            )
            (Interop'maybeClass tag, true)
            (throw! (str "unable to resolve classname: " tag))
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
        (let [#_"Class" c (Expr'''getClass target)
              #_"Field" f (when (some? c) (Reflector'getField c, fieldName, false))]
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
                    #_"Field" :field f
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

        #_memoize!
        (#_"Class" Expr'''getClass [#_"InstanceFieldExpr" this]
            (cond (some? (:tag this)) (Interop'tagToClass (:tag this)) (some? (:field this)) (.getType (:field this)))
        )
    )

    (extend-type InstanceFieldExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"InstanceFieldExpr" this]
            (and (some? (:targetClass this)) (some? (:field this)) (Reflector'isPrimitive (.getType (:field this))))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"InstanceFieldExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when (and (some? (:targetClass this)) (some? (:field this))) => (throw! "unboxed emit of unknown member")
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

                #_"Field" :field (.getField c, fieldName)
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

        #_memoize!
        (#_"Class" Expr'''getClass [#_"StaticFieldExpr" this]
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
                    (and (= retClass Integer/TYPE) (= c Long/TYPE))
                    (do
                        (MaybePrimitive'''emitUnboxed body, :Context'RETURN, objx, gen)
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "int intCast(long)"))
                    )
                    :else
                    (do
                        (throw! (str "mismatched primitive return, expected: " retClass ", had: " (Expr'''getClass body)))
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

(declare <=)

(class-ns InstanceMethodExpr
    (defn #_"InstanceMethodExpr" InstanceMethodExpr'new [#_"int" line, #_"Symbol" tag, #_"Expr" target, #_"String" methodName, #_"IPersistentVector" args, #_"boolean" tailPosition]
        (let [#_"java.lang.reflect.Method" method
                (if (some? (Expr'''getClass target))
                    (let [#_"PersistentVector" methods (Reflector'getMethods (Expr'''getClass target), (count args), methodName, false)]
                        (if (zero? (count methods))
                            (do
                                (when *warn-on-reflection*
                                    (.println *err*, (str "Reflection warning, line " line " - call to method " methodName " on " (.getName (Expr'''getClass target)) " can't be resolved (no such method)."))
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
                                    (.println *err*, (str "Reflection warning, line " line " - call to method " methodName " on " (.getName (Expr'''getClass target)) " can't be resolved (argument types: " (Compiler'getTypeStringForArgs args) ")."))
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

        #_memoize!
        (#_"Class" Expr'''getClass [#_"InstanceMethodExpr" this]
            (Compiler'retType (when (some? (:tag this)) (Interop'tagToClass (:tag this))), (when (some? (:method this)) (.getReturnType (:method this))))
        )
    )

    (extend-type InstanceMethodExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"InstanceMethodExpr" this]
            (and (some? (:method this)) (Reflector'isPrimitive (.getReturnType (:method this))))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"InstanceMethodExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when (some? (:method this)) => (throw! "unboxed emit of unknown member")
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
                    (when-not (zero? (count methods)) => (throw! (str "no matching method: " methodName))
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
        (and (some? (:method this)) (some? (get Intrinsics'preds (str (:method this)))))
    )

    (declare pop)
    (declare peek)

    #_method
    (defn #_"void" StaticMethodExpr''emitIntrinsicPredicate [#_"StaticMethodExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"Label" falseLabel]
        (.visitLineNumber gen, (:line this), (.mark gen))
        (when (some? (:method this)) => (throw! "unboxed emit of unknown member")
            (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
            (when (= context :Context'RETURN)
                (IopMethod''emitClearLocals *method*, gen)
            )
            (let [#_"[int]" preds (get Intrinsics'preds (str (:method this)))]
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
                                    (= rc Long/TYPE)       (.pop2 gen)
                                    (not (= rc Void/TYPE)) (.pop gen)
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

        #_memoize!
        (#_"Class" Expr'''getClass [#_"StaticMethodExpr" this]
            (Compiler'retType (when (some? (:tag this)) (Interop'tagToClass (:tag this))), (when (some? (:method this)) (.getReturnType (:method this))))
        )
    )

    (extend-type StaticMethodExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"StaticMethodExpr" this]
            (and (some? (:method this)) (Reflector'isPrimitive (.getReturnType (:method this))))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"StaticMethodExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when (some? (:method this)) => (throw! "unboxed emit of unknown member")
                (MethodExpr'emitTypedArgs objx, gen, (.getParameterTypes (:method this)), (:args this))
                (.visitLineNumber gen, (:line this), (.mark gen))
                (when (= context :Context'RETURN)
                    (IopMethod''emitClearLocals *method*, gen)
                )
                (let [#_"int|[int]" ops (get Intrinsics'ops (str (:method this)))]
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

(declare not=)

(class-ns HostParser
    (defn #_"IParser" HostParser'new []
        (reify IParser
            ;; (. x fieldname-sym) or
            ;; (. x 0-ary-method)
            ;; (. x methodname-sym args+)
            ;; (. x (methodname-sym args?))
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when-not (< (count form) 3) => (throw! "malformed member expression, expecting (. target member ...)")
                    ;; determine static or instance
                    ;; static target must be symbol, either fully.qualified.Classname or Classname that has been imported
                    (let [#_"int" line *line* #_"Class" c (Interop'maybeClass (second form), false)
                          ;; at this point c will be non-null if static
                          #_"Expr" instance (when (nil? c) (Compiler'analyze (if (= context :Context'EVAL) context :Context'EXPRESSION), (second form)))
                          #_"boolean" maybeField (and (= (count form) 3) (symbol? (third form)))
                          maybeField
                            (when (and maybeField (not= (nth (:name (third form)) 0) \-)) => maybeField
                                (let [#_"String" name (:name (third form))]
                                    (cond
                                        (some? c)
                                            (zero? (count (Reflector'getMethods c, 0, (Compiler'munge name), true)))
                                        (and (some? instance) (some? (Expr'''getClass instance)))
                                            (zero? (count (Reflector'getMethods (Expr'''getClass instance), 0, (Compiler'munge name), false)))
                                        :else
                                            maybeField
                                    )
                                )
                            )]
                        (if maybeField
                            (let [? (= (nth (:name (third form)) 0) \-)
                                  #_"Symbol" sym (if ? (symbol (.substring (:name (third form)), 1)) (third form))
                                  #_"Symbol" tag (Compiler'tagOf form)]
                                (if (some? c)
                                    (StaticFieldExpr'new line, c, (Compiler'munge (:name sym)), tag)
                                    (InstanceFieldExpr'new line, instance, (Compiler'munge (:name sym)), tag, ?)
                                )
                            )
                            (let [#_"ISeq" call (if (seq? (third form)) (third form) (next (next form)))]
                                (when (symbol? (first call)) => (throw! "malformed member expression")
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
        (merge (UnresolvedVarExpr.)
            (hash-map
                #_"Symbol" :symbol symbol
            )
        )
    )

    (extend-type UnresolvedVarExpr Expr
        (#_"Object" Expr'''eval [#_"UnresolvedVarExpr" this]
            (throw! "can't eval")
        )

        (#_"void" Expr'''emit [#_"UnresolvedVarExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            nil
        )

        (#_"Class" Expr'''getClass [#_"UnresolvedVarExpr" this]
            nil
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

        #_memoize!
        (#_"Class" Expr'''getClass [#_"VarExpr" this]
            (when (some? (:tag this)) (Interop'tagToClass (:tag this)))
        )
    )

    (declare var-set)
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

        (#_"Class" Expr'''getClass [#_"TheVarExpr" this]
            Var
        )
    )
)

(class-ns TheVarParser
    (defn #_"IParser" TheVarParser'new []
        (reify IParser
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"Symbol" sym (second form) #_"Var" v (Compiler'lookupVar sym, false)]
                    (when (some? v) => (throw! (str "unable to resolve var: " sym " in this context"))
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
            (loop-when-recur [#_"Object" ret nil #_"ISeq" s (seq (:exprs this))] (some? s) [(Expr'''eval (first s)) (next s)] => ret)
        )

        (#_"void" Expr'''emit [#_"BodyExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (dotimes [#_"int" i (dec (count (:exprs this)))]
                (Expr'''emit (nth (:exprs this) i), :Context'STATEMENT, objx, gen)
            )
            (Expr'''emit (BodyExpr''lastExpr this), context, objx, gen)
            nil
        )

        (#_"Class" Expr'''getClass [#_"BodyExpr" this]
            (Expr'''getClass (BodyExpr''lastExpr this))
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
            (throw! "can't eval try")
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

        (#_"Class" Expr'''getClass [#_"TryExpr" this]
            (Expr'''getClass (:tryExpr this))
        )
    )
)

(class-ns TryParser
    (defn #_"IParser" TryParser'new []
        (reify IParser
            ;; (try try-expr* catch-expr* finally-expr?)
            ;; catch-expr: (catch class sym expr*)
            ;; finally-expr: (finally expr*)
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (= context :Context'RETURN) => (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                    (let [[#_"Expr" bodyExpr #_"PersistentVector" catches #_"Expr" finallyExpr #_"PersistentVector" body]
                            (loop-when [bodyExpr nil catches [] finallyExpr nil body [] #_"boolean" caught? false #_"ISeq" fs (next form)] (some? fs) => [bodyExpr catches finallyExpr body]
                                (let [#_"Object" f (first fs) #_"Object" op (when (seq? f) (first f))]
                                    (if (any = op 'catch 'finally)
                                        (let [bodyExpr
                                                (when (nil? bodyExpr) => bodyExpr
                                                    (binding [*no-recur* true, *in-return-context* false]
                                                        (IParser'''parse (BodyParser'new), context, (seq body))
                                                    )
                                                )]
                                            (if (= op 'catch)
                                                (let-when [#_"Class" c (Interop'maybeClass (second f), false)] (some? c) => (throw! (str "unable to resolve classname: " (second f)))
                                                    (let-when [#_"Symbol" sym (third f)] (symbol? sym) => (throw! (str "bad binding form, expected symbol, got: " sym))
                                                        (when (nil? (namespace sym)) => (throw! (str "can't bind qualified name: " sym))
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
                                                (when (nil? (next fs)) => (throw! "finally clause must be last in try expression")
                                                    (let [finallyExpr
                                                            (binding [*in-catch-finally* true]
                                                                (IParser'''parse (BodyParser'new), :Context'STATEMENT, (next f))
                                                            )]
                                                        (recur bodyExpr catches finallyExpr body caught? (next fs))
                                                    )
                                                )
                                            )
                                        )
                                        (when-not caught? => (throw! "only catch or finally clause can follow catch in try expression")
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
            (throw! "can't eval throw")
        )

        (#_"void" Expr'''emit [#_"ThrowExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (Expr'''emit (:excExpr this), :Context'EXPRESSION, objx, gen)
            (.checkCast gen, (Type/getType Throwable))
            (.throwException gen)
            nil
        )

        (#_"Class" Expr'''getClass [#_"ThrowExpr" this]
            nil
        )
    )
)

(class-ns ThrowParser
    (defn #_"IParser" ThrowParser'new []
        (reify IParser
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (cond
                    (= context :Context'EVAL) (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                    (= (count form) 1)        (throw! "too few arguments to throw: single Throwable expected")
                    (< 2 (count form))        (throw! "too many arguments to throw: single Throwable expected")
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
                    (let-when [#_"int" n (count ctors)] (< 0 n) => (throw! (str "no matching ctor found for " c))
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

        (#_"Class" Expr'''getClass [#_"NewExpr" this]
            (:c this)
        )
    )
)

(class-ns NewParser
    (defn #_"IParser" NewParser'new []
        (reify IParser
            ;; (new Classname args...)
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"int" line *line*]
                    (when (< 1 (count form)) => (throw! "wrong number of arguments, expecting: (new Classname args...)")
                        (let [#_"Class" c (Interop'maybeClass (second form), false)]
                            (when (some? c) => (throw! (str "unable to resolve classname: " (second form)))
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
            (.checkCast gen, (Type/getType cloiure.core.IObj))
            (Expr'''emit (:meta this), :Context'EXPRESSION, objx, gen)
            (.checkCast gen, (Type/getType cloiure.core.IPersistentMap))
            (.invokeInterface gen, (Type/getType cloiure.core.IObj), (Method/getMethod "cloiure.core.IObj withMeta(cloiure.core.IPersistentMap)"))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"MetaExpr" this]
            (Expr'''getClass (:expr this))
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
            (Expr'''eval (if (any = (Expr'''eval (:testExpr this)) nil false) (:elseExpr this) (:thenExpr this)))
        )

        (#_"void" Expr'''emit [#_"IfExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (IfExpr''doEmit this, context, objx, gen, false)
            nil
        )

        (#_"Class" Expr'''getClass [#_"IfExpr" this]
            (let [#_"Expr" then (:thenExpr this) #_"Class" t (Expr'''getClass then)
                  #_"Expr" else (:elseExpr this) #_"Class" e (Expr'''getClass else)]
                (when (and (or (some? t) (instance? NilExpr then))
                           (or (some? e) (instance? NilExpr else))
                           (or (= t e)
                               (any = Recur t e)
                               (and (nil? t) (not (.isPrimitive e)))
                               (and (nil? e) (not (.isPrimitive t)))))
                    (if (any = t nil Recur) e t)
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
                         (let [#_"Class" t (Expr'''getClass then) #_"Class" e (Expr'''getClass else)]
                            (or (= t e)
                                (any = Recur t e)))
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
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (cond
                    (< 4 (count form)) (throw! "too many arguments to if")
                    (< (count form) 3) (throw! "too few arguments to if")
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
            (.invokeStatic gen, (Type/getType RT), (Method/getMethod "cloiure.core.ISeq arrayToSeq(Object[])"))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"ListExpr" this]
            cloiure.core.IPersistentList
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

    (declare RT'map)

    (extend-type MapExpr Expr
        (#_"Object" Expr'''eval [#_"MapExpr" this]
            (let [#_"Object[]" a (make-array Object (count (:keyvals this)))]
                (dotimes [#_"int" i (count (:keyvals this))]
                    (aset a i (Expr'''eval (nth (:keyvals this) i)))
                )
                (RT'map a)
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
                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "cloiure.core.IPersistentMap mapUniqueKeys(Object[])"))
                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "cloiure.core.IPersistentMap map(Object[])"))
                )
                (when (= context :Context'STATEMENT)
                    (.pop gen)
                )
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"MapExpr" this]
            cloiure.core.IPersistentMap
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
                (and (satisfies? IObj form) (some? (meta form)))
                    (MetaExpr'new e, (MapExpr'parse c, (meta form)))
                keysConstant
                    (when allConstantKeysUnique => (throw! "duplicate constant keys in map")
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

    (declare RT'set)

    (extend-type SetExpr Expr
        (#_"Object" Expr'''eval [#_"SetExpr" this]
            (let [#_"Object[]" a (make-array Object (count (:keys this)))]
                (dotimes [#_"int" i (count (:keys this))]
                    (aset a i (Expr'''eval (nth (:keys this) i)))
                )
                (RT'set a)
            )
        )

        (#_"void" Expr'''emit [#_"SetExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (MethodExpr'emitArgsAsArray (:keys this), objx, gen)
            (.invokeStatic gen, (Type/getType RT), (Method/getMethod "cloiure.core.IPersistentSet set(Object[])"))
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"SetExpr" this]
            cloiure.core.IPersistentSet
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
                (and (satisfies? IObj form) (some? (meta form)))
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

    (declare Tuple'MAX_SIZE)

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
                    (.invokeStatic gen, (Type/getType Tuple), (nth Compiler'createTupleMethods (count (:args this))))
                )
                (do
                    (MethodExpr'emitArgsAsArray (:args this), objx, gen)
                    (.invokeStatic gen, (Type/getType RT), (Method/getMethod "cloiure.core.IPersistentVector vector(Object[])"))
                )
            )

            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"VectorExpr" this]
            cloiure.core.IPersistentVector
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
                (and (satisfies? IObj form) (some? (meta form)))
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
            (IFn'''invoke (:k (:kw this)), (Expr'''eval (:target this)))
        )

        (#_"void" Expr'''emit [#_"KeywordInvokeExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (let [#_"Label" endLabel (.newLabel gen) #_"Label" faultLabel (.newLabel gen)]
                (.visitLineNumber gen, (:line this), (.mark gen))
                (.getStatic gen, (:objType objx), (Compiler'thunkNameStatic (:siteIndex this)), (Type/getType cloiure.core.ILookupThunk))
                (.dup gen) ;; thunk, thunk
                (Expr'''emit (:target this), :Context'EXPRESSION, objx, gen) ;; thunk, thunk, target
                (.visitLineNumber gen, (:line this), (.mark gen))
                (.dupX2 gen) ;; target, thunk, thunk, target
                (.invokeInterface gen, (Type/getType cloiure.core.ILookupThunk), (Method/getMethod "Object get(Object)")) ;; target, thunk, result
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
                (.invokeInterface gen, (Type/getType cloiure.core.ILookupSite), (Method/getMethod "cloiure.core.ILookupThunk fault(Object)")) ;; target, new-thunk
                (.dup gen) ;; target, new-thunk, new-thunk
                (.putStatic gen, (:objType objx), (Compiler'thunkNameStatic (:siteIndex this)), (Type/getType cloiure.core.ILookupThunk)) ;; target, new-thunk
                (.swap gen) ;; new-thunk, target
                (.invokeInterface gen, (Type/getType cloiure.core.ILookupThunk), (Method/getMethod "Object get(Object)")) ;; result

                (.mark gen, endLabel)
                (when (= context :Context'STATEMENT)
                    (.pop gen)
                )
            )
            nil
        )

        #_memoize!
        (#_"Class" Expr'''getClass [#_"KeywordInvokeExpr" this]
            (when (some? (:tag this)) (Interop'tagToClass (:tag this)))
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
            (instance? (:c this) (Expr'''eval (:expr this)))
        )

        (#_"void" Expr'''emit [#_"InstanceOfExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (MaybePrimitive'''emitUnboxed this, context, objx, gen)
            (Interop'emitBoxReturn objx, gen, Boolean/TYPE)
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"InstanceOfExpr" this]
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

(declare var-get)
(declare keyword)

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
                                      #_"Keyword" mmapVal (get mmap (keyword (:sym fvar)))]
                                    (when (some? mmapVal) => (throw! (str "no method of interface: " (.getName (:protocolOn this)) " found for function: " (:sym fvar) " of protocol: " (:sym pvar)))
                                        (let [#_"String" mname (Compiler'munge (str (:sym mmapVal)))
                                              #_"PersistentVector" methods (Reflector'getMethods (:protocolOn this), (dec (count args)), mname, false)]
                                            (when (= (count methods) 1) => (throw! (str "no single method: " mname " of interface: " (.getName (:protocolOn this)) " found for function: " (:sym fvar) " of protocol: " (:sym pvar)))
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

    (declare min)

    #_method
    (defn #_"void" InvokeExpr''emitArgsAndCall [#_"InvokeExpr" this, #_"int" firstArgToEmit, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
        (loop-when-recur [#_"int" i firstArgToEmit] (< i (min Compiler'MAX_POSITIONAL_ARITY (count (:args this)))) [(inc i)]
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

        (.invokeInterface gen, (Type/getType cloiure.core.IFn), (Method. "invoke", (Type/getType Object), (aget Compiler'ARG_TYPES (min (inc Compiler'MAX_POSITIONAL_ARITY) (count (:args this))))))
        nil
    )

    (declare subvec)

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
                (IFn'''applyTo fn, (seq v))
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
                    (.checkCast gen, (Type/getType cloiure.core.IFn))
                    (InvokeExpr''emitArgsAndCall this, 0, context, objx, gen)
                )
            )
            (when (= context :Context'STATEMENT)
                (.pop gen)
            )
            nil
        )

        #_memoize!
        (#_"Class" Expr'''getClass [#_"InvokeExpr" this]
            (when (some? (:tag this)) (Interop'tagToClass (:tag this)))
        )
    )

    (defn #_"Expr" InvokeExpr'parse [#_"Context" context, #_"ISeq" form]
        (let [#_"boolean" tailPosition (Compiler'inTailCall context) context (if (= context :Context'EVAL) context :Context'EXPRESSION)
              #_"Expr" fexpr (Compiler'analyze context, (first form))]
            (or
                (when (and (instance? VarExpr fexpr) (= (:var fexpr) #'instance?) (= (count form) 3))
                    (let-when [#_"Expr" sexpr (Compiler'analyze :Context'EXPRESSION, (second form))] (instance? ConstantExpr sexpr)
                        (let-when [#_"Object" val (Literal'''literal sexpr)] (class? val)
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
            (throw! "can't type hint a local with a primitive initializer")
        )
        (merge (LocalBinding.)
            (hash-map
                #_"int" :uid (Compiler'nextUniqueId)
                #_"int" :idx idx
                #_"Symbol" :sym sym
                #_"Symbol" :tag tag
                #_"Expr" :init init
                #_"boolean" :isArg isArg

                #_"String" :name (Compiler'munge (:name sym))
                #_"boolean" :recurMistmatch false
            )
        )
    )

    #_memoize!
    (defn #_"Class" LocalBinding''getClass [#_"LocalBinding" this]
        (let [#_"Expr" e (:init this)]
            (if (some? (:tag this))
                (when-not (and (some? e) (Reflector'isPrimitive (Expr'''getClass e)) (not (satisfies? MaybePrimitive e)))
                    (Interop'tagToClass (:tag this))
                )
                (when (and (some? e) (not (and (Reflector'isPrimitive (Expr'''getClass e)) (not (satisfies? MaybePrimitive e)))))
                    (Expr'''getClass e)
                )
            )
        )
    )

    #_method
    (defn #_"Class" LocalBinding''getPrimitiveType [#_"LocalBinding" this]
        (Compiler'maybePrimitiveType (:init this))
    )
)

(class-ns LocalBindingExpr
    (defn #_"LocalBindingExpr" LocalBindingExpr'new [#_"LocalBinding" lb, #_"Symbol" tag]
        (when (or (nil? (LocalBinding''getPrimitiveType lb)) (nil? tag)) => (throw! "can't type hint a primitive local")
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
            (throw! "can't eval locals")
        )

        (#_"void" Expr'''emit [#_"LocalBindingExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (when-not (= context :Context'STATEMENT)
                (IopObject''emitLocal objx, gen, (:lb this))
            )
            nil
        )

        #_memoize!
        (#_"Class" Expr'''getClass [#_"LocalBindingExpr" this]
            (if (some? (:tag this)) (Interop'tagToClass (:tag this)) (LocalBinding''getClass (:lb this)))
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
            (throw! "can't eval locals")
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
            (throw! "can't eval")
        )

        (#_"void" Expr'''emit [#_"MethodParamExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (throw! "can't emit")
        )

        (#_"Class" Expr'''getClass [#_"MethodParamExpr" this]
            (:c this)
        )
    )

    (extend-type MethodParamExpr MaybePrimitive
        (#_"boolean" MaybePrimitive'''canEmitPrimitive [#_"MethodParamExpr" this]
            (Reflector'isPrimitive (:c this))
        )

        (#_"void" MaybePrimitive'''emitUnboxed [#_"MethodParamExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (throw! "can't emit")
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

    (declare into-array)

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
                      retTag (when (and (symbol? retTag) (= (INamed'''getName retTag) "long")) retTag)
                      #_"Class" retClass
                        (let-when [retClass (Interop'tagClass (or (Compiler'tagOf parms) retTag))] (.isPrimitive retClass) => Object
                            (when-not (= retClass Long/TYPE) => retClass
                                (throw! "only long primitives are supported")
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
                                (when (symbol? (nth parms i)) => (throw! "fn params must be Symbols")
                                    (let [#_"Symbol" p (nth parms i)]
                                        (cond
                                            (some? (namespace p))
                                                (throw! (str "can't use qualified name as parameter: " p))
                                            (= p '&)
                                                (when-not rest? => (throw! "invalid parameter list")
                                                    (recur fm true (inc i))
                                                )
                                            :else
                                                (let [#_"Class" c (Compiler'primClass (Interop'tagClass (Compiler'tagOf p)))]
                                                    (when (and (.isPrimitive c) (not= c Long/TYPE))
                                                        (throw! (str "only long primitives are supported: " p))
                                                    )
                                                    (when (and rest? (some? (Compiler'tagOf p)))
                                                        (throw! "& arg cannot have type hint")
                                                    )
                                                    (let [c (if rest? cloiure.core.ISeq c)
                                                          fm (-> fm (update :argTypes conj (Type/getType c)) (update :argClasses conj c))
                                                          #_"LocalBinding" lb
                                                            (if (.isPrimitive c)
                                                                (Compiler'registerLocal p, nil, (MethodParamExpr'new c), true)
                                                                (Compiler'registerLocal p, (if rest? 'cloiure.core.ISeq (Compiler'tagOf p)), nil, true)
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
                            (throw! (str "can't specify more than " Compiler'MAX_POSITIONAL_ARITY " params"))
                        )
                        (set! *loop-locals* (:argLocals fm))
                        (-> fm
                            (update #_"Type[]" :argTypes #(into-array Type %))
                            (update #_"Class[]" :argClasses #(into-array Class %))
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
                        (.isAssignableFrom LazySeq, c) (Type/getType cloiure.core.ISeq)
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

    (declare vals)

    #_method
    (defn #_"Type[]" IopObject''ctorTypes [#_"IopObject" this]
        (let [#_"IPersistentVector" v (if (IopObject'''supportsMeta this) [(Type/getType cloiure.core.IPersistentMap)] [])
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
                (.putStatic clinitgen, (:objType this), (Compiler'thunkNameStatic i), (Type/getType cloiure.core.ILookupThunk))
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
        (when (IopObject''isMutable this, lb) => (throw! (str "cannot assign to non-mutable: " (:name lb)))
            (let [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                (.loadThis gen)
                (if (some? primc)
                    (do
                        (when-not (and (satisfies? MaybePrimitive val) (MaybePrimitive'''canEmitPrimitive val))
                            (throw! (str "must assign primitive to primitive mutable: " (:name lb)))
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
        (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "Object get()"))
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

    (declare RT'seqToArray)
    (declare RT'printString)

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
                        (.getStatic gen, (Type/getType Boolean), (if (.booleanValue ^Boolean value) "TRUE" "FALSE"), (Type/getType Boolean))
                        true
                    )
                    (instance? Integer value)
                    (do
                        (.push gen, (.intValue ^Integer value))
                        (.invokeStatic gen, (Type/getType Integer), (Method/getMethod "Integer valueOf(int)"))
                        true
                    )
                    (instance? Long value)
                    (do
                        (.push gen, (.longValue ^Long value))
                        (.invokeStatic gen, (Type/getType Long), (Method/getMethod "Long valueOf(long)"))
                        true
                    )
                    (char? value)
                    (do
                        (.push gen, (.charValue ^Character value))
                        (.invokeStatic gen, (Type/getType Character), (Method/getMethod "Character valueOf(char)"))
                        true
                    )
                    (class? value)
                    (do
                        (if (.isPrimitive value)
                            (let [#_"Type" t
                                    (condp = value
                                        Integer/TYPE   (Type/getType Integer)
                                        Long/TYPE      (Type/getType Long)
                                        Boolean/TYPE   (Type/getType Boolean)
                                        Byte/TYPE      (Type/getType Byte)
                                        Character/TYPE (Type/getType Character)
                                        (throw! (str "can't embed unknown primitive in code: " value))
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
                        (.push gen, (str (:name (:ns value))))
                        (.push gen, (str (:sym value)))
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "clojure.lang.Var var(String, String)"))
                        true
                    )
                    (satisfies? IType value)
                    (let [#_"Method" ctor (Method. "<init>", (Type/getConstructorDescriptor (aget (.getConstructors (class value)) 0)))]
                        (.newInstance gen, (Type/getType (class value)))
                        (.dup gen)
                        (let [#_"IPersistentVector" fields (Reflector'invokeStaticMethod (class value), "getBasis", (object-array 0))]
                            (loop-when-recur [#_"ISeq" s (seq fields)] (some? s) [(next s)]
                                (let [#_"Symbol" field (first s)]
                                    (IopObject''emitValue this, (Reflector'getInstanceField value, (Compiler'munge (:name field))), gen)
                                    (let-when [#_"Class" k (Interop'tagClass (Compiler'tagOf field))] (.isPrimitive k)
                                        (let [#_"Type" b (Type/getType (Compiler'boxClass k))]
                                            (.invokeVirtual gen, b, (Method. (str (.getName k) "Value"), (str "()" (.getDescriptor (Type/getType k)))))
                                        )
                                    )
                                )
                            )
                            (.invokeConstructor gen, (Type/getType (class value)), ctor)
                        )
                        true
                    )
                    (map? value)
                    (let [#_"PersistentVector" v
                            (loop-when [v [] #_"ISeq" s (seq value)] (some? s) => v
                                (let [#_"IMapEntry" e (first s)]
                                    (recur (conj v (key e) (val e)) (next s))
                                )
                            )]
                        (IopObject''emitObjectArray this, (to-array v), gen)
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "cloiure.core.IPersistentMap map(Object[])"))
                        true
                    )
                    (vector? value)
                    (let [#_"IPersistentVector" args value]
                        (if (<= (count args) Tuple'MAX_SIZE)
                            (do
                                (dotimes [#_"int" i (count args)]
                                    (IopObject''emitValue this, (nth args i), gen)
                                )
                                (.invokeStatic gen, (Type/getType Tuple), (nth Compiler'createTupleMethods (count args)))
                            )
                            (do
                                (IopObject''emitObjectArray this, (to-array args), gen)
                                (.invokeStatic gen, (Type/getType RT), (Method/getMethod "cloiure.core.IPersistentVector vector(Object[])"))
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
                                (IopObject''emitObjectArray this, (RT'seqToArray vs), gen)
                                (.invokeStatic gen, (Type/getType PersistentHashSet), (Method/getMethod "clojure.lang.PersistentHashSet create(Object[])"))
                            )
                        )
                        true
                    )
                    (or (seq? value) (list? value))
                    (let [#_"ISeq" vs (seq value)]
                        (IopObject''emitObjectArray this, (RT'seqToArray vs), gen)
                        (.invokeStatic gen, (Type/getType PersistentList), (Method/getMethod "cloiure.core.IPersistentList create(Object[])"))
                        true
                    )
                    (instance? Pattern value)
                    (do
                        (IopObject''emitValue this, (str value), gen)
                        (.invokeStatic gen, (Type/getType Pattern), (Method/getMethod "java.util.regex.Pattern compile(String)"))
                        true
                    )
                    :else
                    (let [#_"String" cs
                            (try
                                (RT'printString value)
                                (catch Exception _
                                    (throw! (str "can't embed object in code: " value))
                                )
                            )]
                        (when (zero? (count cs))
                            (throw! (str "can't embed unreadable object in code: " value))
                        )
                        (when (.startsWith cs, "#<")
                            (throw! (str "can't embed unreadable object in code: " cs))
                        )
                        (.push gen, cs)
                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "Object readString(String)"))
                        false
                    )
                )]
            (when partial?
                (when (and (satisfies? IObj value) (pos? (count (meta value))))
                    (.checkCast gen, (Type/getType cloiure.core.IObj))
                    (IopObject''emitValue this, (meta value), gen)
                    (.checkCast gen, (Type/getType cloiure.core.IPersistentMap))
                    (.invokeInterface gen, (Type/getType cloiure.core.IObj), (Method/getMethod "cloiure.core.IObj withMeta(cloiure.core.IPersistentMap)"))
                )
            )
        )
        nil
    )

    #_method
    (defn #_"IopObject" IopObject''compile [#_"IopObject" this, #_"String" superName, #_"String[]" interfaceNames, #_"boolean" _oneTimeUse]
        (binding [*used-constants* #{}]
            ;; create bytecode for a class
            ;; with name current_ns.defname[$letname]+
            ;; anonymous fns get names fn__id
            ;; derived from AFn'RestFn
            (let [#_"ClassWriter" cw (ClassWriter. ClassWriter/COMPUTE_MAXS) #_"ClassVisitor" cv cw]
                (.visit cv, Opcodes/V1_5, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER Opcodes/ACC_FINAL), (:internalName this), nil, superName, interfaceNames)
                (when (IopObject'''supportsMeta this)
                    (.visitField cv, Opcodes/ACC_FINAL, "__meta", (.getDescriptor (Type/getType cloiure.core.IPersistentMap)), nil, nil)
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
                                        :else (+ Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL)
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
                    (.visitField cv, (+ Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC), (Compiler'cachedClassName i), (.getDescriptor (Type/getType Class)), nil, nil)
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
                        (.visitVarInsn ctorgen, (.getOpcode (Type/getType cloiure.core.IPersistentMap), Opcodes/ILOAD), 1)
                        (.putField ctorgen, (:objType this), "__meta", (Type/getType cloiure.core.IPersistentMap))
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
                                                    (if (= primc Long/TYPE) (inc a) a)
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
                                (let [#_"Method" meth (Method/getMethod "cloiure.core.IPersistentMap meta()")
                                      #_"GeneratorAdapter" gen (GeneratorAdapter. Opcodes/ACC_PUBLIC, meth, nil, nil, cv)]
                                    (.visitCode gen)
                                    (.loadThis gen)
                                    (.getField gen, (:objType this), "__meta", (Type/getType cloiure.core.IPersistentMap))
                                    (.returnValue gen)
                                    (.endMethod gen)
                                )

                                ;; withMeta()
                                (let [#_"Method" meth (Method/getMethod "cloiure.core.IObj withMeta(cloiure.core.IPersistentMap)")
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
                                (.visitField cv, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL Opcodes/ACC_STATIC), (Compiler'constantName i), (.getDescriptor (IopObject''constantType this, i)), nil, nil)
                            )
                        )

                        ;; static fields for lookup sites
                        (dotimes [#_"int" i (count (:keywordCallsites this))]
                            (.visitField cv, (+ Opcodes/ACC_FINAL Opcodes/ACC_STATIC), (Compiler'siteNameStatic i), (.getDescriptor (Type/getType KeywordLookupSite)), nil, nil)
                            (.visitField cv, Opcodes/ACC_STATIC, (Compiler'thunkNameStatic i), (.getDescriptor (Type/getType cloiure.core.ILookupThunk)), nil, nil)
                        )

                        ;; static init for constants, keywords and vars
                        (let [#_"GeneratorAdapter" clinitgen (GeneratorAdapter. (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (Method/getMethod "void <clinit> ()"), nil, nil, cv)]
                            (.visitCode clinitgen)
                            (.visitLineNumber clinitgen, (:line this), (.mark clinitgen))

                            (IopObject''emitConstants this, clinitgen)
                            (IopObject''emitKeywordCallsites this, clinitgen)

                            (.returnValue clinitgen)
                            (.endMethod clinitgen)
                        )

                        ;; end of class
                        (.visitEnd cv)

                        (assoc this :compiledClass (.defineClass *class-loader*, (:name this), (.toByteArray cw), (§ obsolete nil)))
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

        #_memoize!
        (#_"Class" Expr'''getClass [#_"FnExpr" this]
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

    (declare boolean)
    (declare RT'nextID)
    (declare dissoc)

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
              #_"String" basename (if (some? owner) (:name (:objx owner)) (Compiler'munge (:name (:name *ns*))))
              [#_"Symbol" nm name]
                (if (symbol? (second form))
                    (let [nm (second form)]
                        [nm (str (:name nm) "__" (RT'nextID))]
                    )
                    (cond
                        (nil? name)   [nil (str "fn__" (RT'nextID))]
                        (some? owner) [nil (str name "__"(RT'nextID))]
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
                                [(assoc fn :thisName (:name nm)) (cons 'fn* (next (next form)))]
                            )
                          ;; now (fn [args] body...) or (fn ([args] body...) ([args2] body2...) ...)
                          ;; turn former into latter
                          form
                            (when (vector? (second form)) => form
                                (list 'fn* (next form))
                            )
                          #_"FnMethod[]" a (make-array #_"FnMethod" Object (inc Compiler'MAX_POSITIONAL_ARITY))
                          #_"FnMethod" variadic
                            (loop-when [variadic nil #_"ISeq" s (next form)] (some? s) => variadic
                                (let [#_"FnMethod" f (FnMethod'parse fn, (first s), rettag)
                                      variadic
                                        (if (FnMethod''isVariadic f)
                                            (when (nil? variadic) => (throw! "can't have more than 1 variadic overload")
                                                f
                                            )
                                            (let [#_"int" n (count (:reqParms f))]
                                                (when (nil? (aget a n)) => (throw! "can't have 2 overloads with same arity")
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
                                    (throw! "can't have fixed arity function with more params than variadic function")
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
    (defn #_"DefExpr" DefExpr'new [#_"int" line, #_"Var" var, #_"Expr" init, #_"Expr" meta, #_"boolean" initProvided, #_"boolean" shadowsCoreMapping]
        (merge (DefExpr.)
            (hash-map
                #_"int" :line line
                #_"Var" :var var
                #_"Expr" :init init
                #_"Expr" :meta meta
                #_"boolean" :initProvided initProvided
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

    (declare Var''bindRoot)

    (extend-type DefExpr Expr
        (#_"Object" Expr'''eval [#_"DefExpr" this]
            (when (:initProvided this)
                (Var''bindRoot (:var this), (Expr'''eval (:init this)))
            )
            (when (some? (:meta this))
                (reset-meta! (:var this) (Expr'''eval (:meta this)))
            )
            (:var this)
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
            (when (some? (:meta this))
                (.dup gen)
                (Expr'''emit (:meta this), :Context'EXPRESSION, objx, gen)
                (.checkCast gen, (Type/getType cloiure.core.IPersistentMap))
                (.invokeVirtual gen, (Type/getType Var), (Method/getMethod "void setMeta(cloiure.core.IPersistentMap)"))
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

        (#_"Class" Expr'''getClass [#_"DefExpr" this]
            Var
        )
    )
)

(class-ns DefParser
    (defn #_"IParser" DefParser'new []
        (reify IParser
            ;; (def x) or (def x initexpr)
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (cond
                    (< 3 (count form))            (throw! "too many arguments to def")
                    (< (count form) 2)            (throw! "too few arguments to def")
                    (not (symbol? (second form))) (throw! "first argument to def must be a Symbol")
                )
                (let [#_"Symbol" sym (second form) #_"Var" v (Compiler'lookupVar sym, true)]
                    (when (some? v) => (throw! "can't refer to qualified var that doesn't exist")
                        (let [[v #_"boolean" shadowsCoreMapping]
                                (when-not (= (:ns v) *ns*) => [v false]
                                    (when (nil? (:ns sym)) => (throw! "can't create defs outside of current ns")
                                        (let [v (Namespace''intern *ns*, sym)]
                                            (Compiler'registerVar v)
                                            [v true]
                                        )
                                    )
                                )
                              #_"Context" c (if (= context :Context'EVAL) context :Context'EXPRESSION)
                              #_"Expr" init (Compiler'analyze c, (third form), (:name (:sym v)))
                              #_"Expr" meta (Compiler'analyze c, (assoc (meta sym) :line *line*))]
                            (DefExpr'new *line*, v, init, meta, (= (count form) 3), shadowsCoreMapping)
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
            (throw! "can't eval letfns")
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
                                  #_"String" lname (:name (:binding bi)) lname (if (.endsWith lname, "__auto__") (str lname (RT'nextID)) lname)
                                  #_"Class" primc (Compiler'maybePrimitiveType (:init bi))]
                                (.visitLocalVariable gen, lname, (if (some? primc) (Type/getDescriptor primc) "Ljava/lang/Object;"), nil, loopLabel, end, (:idx (:binding bi)))
                            )
                        )
                    )
                )
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"LetFnExpr" this]
            (Expr'''getClass (:body this))
        )
    )
)

(declare even?)
(declare quot)

(class-ns LetFnParser
    (defn #_"IParser" LetFnParser'new []
        (reify IParser
            ;; (letfns* [var (fn [args] body) ...] body...)
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when (vector? (second form)) => (throw! "bad binding form, expected vector")
                    (let [#_"IPersistentVector" bindings (second form)]
                        (when (even? (count bindings)) => (throw! "bad binding form, expected matched symbol expression pairs")
                            (if (= context :Context'EVAL)
                                (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                                (binding [*local-env* *local-env*, *last-local-num* *last-local-num*]
                                    ;; pre-seed env (like Lisp labels)
                                    (let [#_"PersistentVector" lbs
                                            (loop-when [lbs [] #_"int" i 0] (< i (count bindings)) => lbs
                                                (let-when [#_"Object" sym (nth bindings i)] (symbol? sym) => (throw! (str "bad binding form, expected symbol, got: " sym))
                                                    (when (nil? (namespace sym)) => (throw! (str "can't let qualified name: " sym))
                                                        (recur (conj lbs (Compiler'registerLocal sym, (Compiler'tagOf sym), nil, false)) (+ i 2))
                                                    )
                                                )
                                            )
                                          #_"PersistentVector" bis
                                            (loop-when [bis [] #_"int" i 0] (< i (count bindings)) => bis
                                                (let [#_"Expr" init (Compiler'analyze :Context'EXPRESSION, (nth bindings (inc i)), (:name (nth bindings i)))
                                                      #_"LocalBinding" lb (Compiler'complementLocalInit (nth lbs (quot i 2)), init)]
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
        (let [#_"{BindingInit Label}" bindingLabels
                (loop-when [bindingLabels {} #_"int" i 0] (< i (count (:bindingInits this))) => bindingLabels
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
                        (recur (assoc bindingLabels bi (.mark gen)) (inc i))
                    )
                )
              #_"Label" loopLabel (.mark gen)]
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
                          #_"String" lname (:name (:binding bi)) lname (if (.endsWith lname, "__auto__") (str lname (RT'nextID)) lname)
                          #_"Class" primc (Compiler'maybePrimitiveType (:init bi))]
                        (.visitLocalVariable gen, lname, (if (some? primc) (Type/getDescriptor primc) "Ljava/lang/Object;"), nil, (get bindingLabels bi), end, (:idx (:binding bi)))
                    )
                )
            )
        )
        nil
    )

    (extend-type LetExpr Expr
        (#_"Object" Expr'''eval [#_"LetExpr" this]
            (throw! "can't eval let/loop")
        )

        (#_"void" Expr'''emit [#_"LetExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (LetExpr''doEmit this, context, objx, gen, false)
            nil
        )

        (#_"Class" Expr'''getClass [#_"LetExpr" this]
            (Expr'''getClass (:body this))
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

(declare repeat)
(declare push-thread-bindings)
(declare pop-thread-bindings)

(class-ns LetParser
    (defn #_"IParser" LetParser'new []
        (reify IParser
            ;; (let [var val var2 val2 ...] body...)
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"boolean" isLoop (= (first form) 'loop*)]
                    (when (vector? (second form)) => (throw! "bad binding form, expected vector")
                        (let [#_"IPersistentVector" bindings (second form)]
                            (when (even? (count bindings)) => (throw! "bad binding form, expected matched symbol expression pairs")
                                (if (or (= context :Context'EVAL) (and (= context :Context'EXPRESSION) isLoop))
                                    (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                                    (let [#_"ISeq" body (next (next form))
                                          #_"IPersistentMap" locals' (:locals *method*)]
                                        ;; may repeat once for each binding with a mismatch, return breaks
                                        (loop [#_"IPersistentVector" rms (vec (repeat (quot (count bindings) 2) false))]
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
                                                                    (let-when [#_"Object" sym (nth bindings i)] (symbol? sym) => (throw! (str "bad binding form, expected symbol, got: " sym))
                                                                        (when (nil? (namespace sym)) => (throw! (str "can't let qualified name: " sym))
                                                                            (let [#_"Expr" init (Compiler'analyze :Context'EXPRESSION, (nth bindings (inc i)), (:name sym))
                                                                                  init
                                                                                    (when isLoop => init
                                                                                        (if (and (some? rms) (nth rms (quot i 2)))
                                                                                            (do
                                                                                                (when *warn-on-reflection*
                                                                                                    (.println *err*, (str "Auto-boxing loop arg: " sym))
                                                                                                )
                                                                                                (StaticMethodExpr'new 0, nil, RT, "box", [init], false)
                                                                                            )
                                                                                            (condp = (Compiler'maybePrimitiveType init)
                                                                                                Integer/TYPE (StaticMethodExpr'new 0, nil, RT, "longCast", [init], false)
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
            (throw! "can't eval recur")
        )

        (#_"void" Expr'''emit [#_"RecurExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (let-when [#_"Label" loopLabel *loop-label*] (some? loopLabel) => (throw! "recur misses loop label")
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
                                    (and (= primc Integer/TYPE) (= pc Long/TYPE))
                                    (do
                                        (MaybePrimitive'''emitUnboxed arg, :Context'EXPRESSION, objx, gen)
                                        (.invokeStatic gen, (Type/getType RT), (Method/getMethod "int intCast(long)"))
                                    )
                                    :else
                                    (do
                                        (throw! (str "recur arg for primitive local: " (:name lb) " is not matching primitive, had: " (.getName (or (Expr'''getClass arg) Object)) ", needed: " (.getName primc)))
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
                            (.visitVarInsn gen, (.getOpcode (Type/getType (or primc Object)), Opcodes/ISTORE), (:idx lb))
                        )
                    )
                )
                (.goTo gen, loopLabel)
            )
            nil
        )

        (#_"Class" Expr'''getClass [#_"RecurExpr" this]
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
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (when-not (and (= context :Context'RETURN) (some? *loop-locals*))
                    (throw! "can only recur from tail position")
                )
                (when *no-recur*
                    (throw! "cannot recur across try")
                )
                (let [#_"int" line *line*
                      #_"PersistentVector" args
                        (loop-when-recur [args [] #_"ISeq" s (seq (next form))]
                                         (some? s)
                                         [(conj args (Compiler'analyze :Context'EXPRESSION, (first s))) (next s)]
                                      => args
                        )]
                    (when-not (= (count args) (count *loop-locals*))
                        (throw! (str "mismatched argument count to recur, expected: " (count *loop-locals*) " args, got: " (count args)))
                    )
                    (dotimes [#_"int" i (count *loop-locals*)]
                        (let [#_"LocalBinding" lb (nth *loop-locals* i)]
                            (when-some [#_"Class" primc (LocalBinding''getPrimitiveType lb)]
                                (let [#_"Class" pc (Compiler'maybePrimitiveType (nth args i))
                                      #_"boolean" mismatch?
                                        (condp = primc
                                            Long/TYPE   (not (any = pc Long/TYPE Integer/TYPE Character/TYPE Byte/TYPE))
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

    (defn- #_"IPersistentMap" NewInstanceMethod'findMethodsWithNameAndArity [#_"String" name, #_"int" arity, #_"IPersistentMap" overrideables]
        (loop-when [#_"IPersistentMap" found {} #_"ISeq" s (seq overrideables)] (some? s) => found
            (let [#_"IMapEntry" e (first s) #_"java.lang.reflect.Method" m (val e)
                  found
                    (when (and (= name (.getName m)) (= (alength (.getParameterTypes m)) arity)) => found
                        (assoc found (key e) m)
                    )]
                (recur found (next s))
            )
        )
    )

    (defn #_"NewInstanceMethod" NewInstanceMethod'parse [#_"IopObject" objx, #_"ISeq" form, #_"Symbol" thistag, #_"IPersistentMap" overrideables]
        ;; (methodname [this-name args*] body...)
        ;; this-name might be nil
        (let [#_"NewInstanceMethod" nim
                (-> (NewInstanceMethod'new objx, *method*)
                    (assoc :line *line*)
                )
              #_"Symbol" dotname (first form) #_"Symbol" name (with-meta (symbol (Compiler'munge (:name dotname))) (meta dotname))
              #_"IPersistentVector" parms (second form)]
            (when (pos? (count parms)) => (throw! (str "must supply at least one argument for 'this' in: " dotname))
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
                                    (let-when [#_"Object" sym (nth parms i)] (symbol? sym) => (throw! "params must be Symbols")
                                        (let [#_"Object" tag (Compiler'tagOf sym) hinted? (or hinted? (some? tag))]
                                            (aset pclasses i (Interop'tagClass tag))
                                            (aset psyms i (if (some? (namespace sym)) (symbol (:name sym)) sym))
                                            (recur hinted? (inc i))
                                        )
                                    )
                                )
                              #_"IPersistentMap" matches (NewInstanceMethod'findMethodsWithNameAndArity (:name name), (count parms), overrideables)
                              #_"IPersistentVector" mk [(:name name) (seq pclasses)]
                              [nim pclasses #_"java.lang.reflect.Method" m]
                                (case (count matches)
                                    0   (throw! (str "can't define method not in interfaces: " (:name name)))
                                    1   (if hinted? ;; validate match
                                            (let [m (get matches mk)]
                                                (when (nil? m)
                                                    (throw! (str "can't find matching method: " (:name name) ", leave off hints for auto match."))
                                                )
                                                (when-not (= (.getReturnType m) (:retClass nim))
                                                    (throw! (str "mismatched return type: " (:name name) ", expected: " (.getName (.getReturnType m)) ", had: " (.getName (:retClass nim))))
                                                )
                                                [nim pclasses m]
                                            )
                                            ;; adopt found method sig
                                            (let [m (val (first matches))]
                                                [(assoc nim :retClass (.getReturnType m)) (.getParameterTypes m) m]
                                            )
                                        )
                                        ;; must be hinted and match one method
                                        (when hinted? => (throw! (str "must hint overloaded method: " (:name name)))
                                            (let [m (get matches mk)]
                                                (when (nil? m)
                                                    (throw! (str "can't find matching overloaded method: " (:name name)))
                                                )
                                                (when-not (= (.getReturnType m) (:retClass nim))
                                                    (throw! (str "mismatched return type: " (:name name) ", expected: " (.getName (.getReturnType m)) ", had: " (.getName (:retClass nim))))
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
                                (when (= (aget pclasses i) Long/TYPE)
                                    (Compiler'nextLocalNum)
                                )
                            )
                            (set! *loop-locals* argLocals)
                            (assoc nim
                                :name (:name name)
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

                #_"{IPersistentVector java.lang.reflect.Method}" :overrideables nil
                #_"{IPersistentVector {Class}}" :covariants nil
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
            (.visit cv, Opcodes/V1_5, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER), (str Compiler'COMPILE_STUB_PREFIX "/" (:internalName ret)), nil, superName, interfaceNames)

            ;; instance fields for closed-overs
            (loop-when-recur [#_"ISeq" s (vals (get *closes* (:uid ret)))] (some? s) [(next s)]
                (let [#_"LocalBinding" lb (first s)
                      #_"int" access (+ Opcodes/ACC_PUBLIC (if (IopObject''isVolatile ret, lb) Opcodes/ACC_VOLATILE (if (IopObject''isMutable ret, lb) 0 Opcodes/ACC_FINAL)))]
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

            (.defineClass *class-loader*, (str Compiler'COMPILE_STUB_PREFIX "." (:name ret)), (.toByteArray cw), (§ obsolete nil))
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

        #_memoize!
        (#_"Class" Expr'''getClass [#_"NewInstanceExpr" this]
            (or (:compiledClass this)
                (if (some? (:tag this)) (Interop'tagToClass (:tag this)) cloiure.core.IFn)
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
                (let [#_"Method" meth (Method/getMethod "cloiure.core.IPersistentVector getBasis()")
                      #_"GeneratorAdapter" gen (GeneratorAdapter. (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), meth, nil, nil, cv)]
                    (IopObject''emitValue this, (:hintedFields this), gen)
                    (.returnValue gen)
                    (.endMethod gen)

                    (let-when [#_"int" n (count (:hintedFields this))] (< n (count (:fields this)))
                        ;; create(IPersistentMap)
                        (let [#_"String" className (.replace (:name this), \., \/)
                              #_"MethodVisitor" mv (.visitMethod cv, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "create", (str "(Lcloiure/core/IPersistentMap;)L" className ";"), nil, nil)]
                            (.visitCode mv)

                            (loop-when-recur [#_"ISeq" s (seq (:hintedFields this)) #_"int" i 1] (some? s) [(next s) (inc i)]
                                (let [#_"String" bName (:name (first s))
                                      #_"Class" k (Interop'tagClass (Compiler'tagOf (first s)))]
                                    (.visitVarInsn mv, Opcodes/ALOAD, 0)
                                    (.visitLdcInsn mv, bName)
                                    (.visitMethodInsn mv, Opcodes/INVOKESTATIC, "clojure/lang/Keyword", "intern", "(Ljava/lang/String;)Lclojure/lang/Keyword;")
                                    (.visitInsn mv, Opcodes/ACONST_NULL)
                                    (.visitMethodInsn mv, Opcodes/INVOKEINTERFACE, "cloiure/core/IPersistentMap", "valAt", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                                    (when (.isPrimitive k)
                                        (.visitTypeInsn mv, Opcodes/CHECKCAST, (.getInternalName (Type/getType (Compiler'boxClass k))))
                                    )
                                    (.visitVarInsn mv, Opcodes/ASTORE, i)
                                    (.visitVarInsn mv, Opcodes/ALOAD, 0)
                                    (.visitLdcInsn mv, bName)
                                    (.visitMethodInsn mv, Opcodes/INVOKESTATIC, "clojure/lang/Keyword", "intern", "(Ljava/lang/String;)Lclojure/lang/Keyword;")
                                    (.visitMethodInsn mv, Opcodes/INVOKEINTERFACE, "cloiure/core/IPersistentMap", "dissoc", "(Ljava/lang/Object;)Lcloiure/core/IPersistentMap;")
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
            (doseq [#_"IMapEntry" e (:covariants this)]
                (let [#_"java.lang.reflect.Method" m (get (:overrideables this) (key e))
                      #_"Class[]" params (.getParameterTypes m)
                      #_"Type[]" argTypes (make-array Type (alength params))
                      _ (dotimes [#_"int" i (alength params)]
                            (aset argTypes i (Type/getType (aget params i)))
                        )
                      #_"Method" target (Method. (.getName m), (Type/getType (.getReturnType m)), argTypes)]
                    (doseq [#_"Class" retType (val e)]
                        (let [#_"Method" meth (Method. (.getName m), (Type/getType retType), argTypes)
                              #_"GeneratorAdapter" gen (GeneratorAdapter. (+ Opcodes/ACC_PUBLIC Opcodes/ACC_BRIDGE), meth, nil, Compiler'EXCEPTION_TYPES, cv)]
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

    (defn- #_"IPersistentVector" NewInstanceExpr'considerMethod [#_"java.lang.reflect.Method" m]
        (let [#_"int" mods (.getModifiers m)]
            (when (and (or (Modifier/isPublic mods) (Modifier/isProtected mods)) (not (Modifier/isStatic mods)) (not (Modifier/isFinal mods)))
                [(.getName m) (seq (.getParameterTypes m)) (.getReturnType m)]
            )
        )
    )

    (declare assoc!)
    (declare concat)

    (defn- #_"ITransientMap" NewInstanceExpr'harvestMethods [#_"ITransientMap" m, #_"Class" c]
        (when (some? c) => m
            (let [m (reduce #(if-some [#_"IPersistentVector" v (NewInstanceExpr'considerMethod %2)] (assoc! %1 v %2) %1)
                            m
                            (concat (.getMethods c) (.getDeclaredMethods c))
                    )]
                (recur m (.getSuperclass c))
            )
        )
    )

    (defn #_"[{IPersistentVector java.lang.reflect.Method} {IPersistentVector {Class}}]" NewInstanceExpr'gatherMethods [#_"Class" super, #_"ISeq" ifaces]
        (let [#_"IPersistentMap" all (reduce! NewInstanceExpr'harvestMethods {} (cons super ifaces))]
            (loop-when [#_"IPersistentMap" methods {} #_"IPersistentMap" covariants {} #_"ISeq" s (seq all)] (some? s) => [methods covariants]
                (let [#_"IMapEntry" e (first s) #_"IPersistentVector" mk (pop (key e)) #_"java.lang.reflect.Method" m (val e)]
                    (if (contains? methods mk) ;; covariant return
                        (let [#_"Class" tk (.getReturnType (get methods mk)) #_"Class" t (.getReturnType m)
                              senj- #(conj (or %1 #{}) %2)]
                            (if (.isAssignableFrom tk, t)
                                (recur (assoc methods mk m) (update covariants mk senj- tk) (next s))
                                (recur        methods       (update covariants mk senj- t)  (next s))
                            )
                        )
                        (recur (assoc methods mk m) covariants (next s))
                    )
                )
            )
        )
    )

    (declare *)
    (declare PersistentArrayMap'new)

    (defn #_"IopObject" NewInstanceExpr'build [#_"IPersistentVector" interfaceSyms, #_"IPersistentVector" fieldSyms, #_"Symbol" thisSym, #_"String" tagName, #_"Symbol" className, #_"Symbol" typeTag, #_"ISeq" methodForms, #_"ISeq" form, #_"IPersistentMap" opts]
        (let [#_"String" name (str className) #_"String" name' (.replace name, \., \/)
              #_"NewInstanceExpr" nie
                (-> (NewInstanceExpr'new nil)
                    (assoc :name name :internalName name' :objType (Type/getObjectType name') :opts opts)
                )
              nie (if (some? thisSym) (assoc nie :thisName (:name thisSym)) nie)
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
                          ;; use array map to preserve ctor order
                          _ (update! *closes* assoc (:uid nie) (PersistentArrayMap'new a))
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
                        (when (.isInterface c) => (throw! (str "only interfaces are supported, had: " (.getName c)))
                            (recur (conj ifaces c) (next s))
                        )
                    )
                )
              #_"Class" super Object
              [#_"IPersistentMap" overrideables #_"IPersistentMap" covariants] (NewInstanceExpr'gatherMethods super, (seq ifaces))
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
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"ISeq" s form                                                  s (next s)
                      #_"IPersistentVector" ifaces (conj (first s) 'cloiure.core.IObj) s (next s)
                      #_"String" classname
                        (let [#_"IopMethod" owner *method*
                              #_"String" basename (if (some? owner) (IopObject'trimGenID (:name (:objx owner))) (Compiler'munge (:name (:name *ns*))))]
                            (str basename "$" "reify__" (RT'nextID))
                        )
                      #_"IopObject" nie (NewInstanceExpr'build ifaces, nil, nil, classname, (symbol classname), nil, s, form, nil)]
                    (when (and (satisfies? IObj form) (some? (meta form))) => nie
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
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (let [#_"ISeq" s form                                 s (next s)
                      #_"String" tagname (INamed'''getName (first s)) s (next s)
                      #_"Symbol" classname (first s)                  s (next s)
                      #_"IPersistentVector" fields (first s)          s (next s)
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
    (defn #_"CaseExpr" CaseExpr'new [#_"int" line, #_"LocalBindingExpr" expr, #_"int" shift, #_"int" mask, #_"int" low, #_"int" high, #_"Expr" defaultExpr, #_"sorted {Integer Expr}" tests, #_"{Integer Expr}" thens, #_"Keyword" switchType, #_"Keyword" testType, #_"{Integer}" skipCheck]
        (when-not (any = switchType :compact :sparse)
            (throw! (str "unexpected switch type: " switchType))
        )
        (when-not (any = testType :int :hash-equiv :hash-identity)
            (throw! (str "unexpected test type: " testType))
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
                #_"sorted {Integer Expr}" :tests tests
                #_"{Integer Expr}" :thens thens
                #_"Keyword" :switchType switchType
                #_"Keyword" :testType testType
                #_"{Integer}" :skipCheck skipCheck
                #_"Class" :returnType (Compiler'maybeClass (conj (vec (vals thens)) defaultExpr))
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
            (any = exprType Type/LONG_TYPE Type/INT_TYPE Type/BYTE_TYPE)
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
            (any = exprType Type/INT_TYPE Type/BYTE_TYPE)
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

    (declare sorted-map)

    #_method
    (defn- #_"void" CaseExpr''doEmit [#_"CaseExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen, #_"boolean" emitUnboxed]
        (let [#_"Label" defaultLabel (.newLabel gen) #_"Label" endLabel (.newLabel gen)
              #_"sorted {Integer Label}" labels (reduce! #(assoc! %1 %2 (.newLabel gen)) (sorted-map) (keys (:tests this)))]
            (.visitLineNumber gen, (:line this), (.mark gen))
            (let [#_"Class" primExprClass (Compiler'maybePrimitiveType (:expr this))
                  #_"Type" primExprType (when (some? primExprClass) (Type/getType primExprClass))]
                (if (= (:testType this) :int)
                    (CaseExpr''emitExprForInts this, objx, gen, primExprType, defaultLabel)
                    (CaseExpr''emitExprForHashes this, objx, gen)
                )
                (if (= (:switchType this) :sparse)
                    (let [#_"Label[]" la (into-array Label (vals labels))]
                        (.visitLookupSwitchInsn gen, defaultLabel, (int-array (keys (:tests this))), la)
                    )
                    (let [#_"Label[]" la (make-array Label (inc (- (:high this) (:low this))))]
                        (loop-when-recur [#_"int" i (:low this)] (<= i (:high this)) [(inc i)]
                            (aset la (- i (:low this)) (if (contains? labels i) (get labels i) defaultLabel))
                        )
                        (.visitTableSwitchInsn gen, (:low this), (:high this), defaultLabel, la)
                    )
                )
                (doseq [#_"Integer" i (keys labels)]
                    (.mark gen, (get labels i))
                    (cond
                        (= (:testType this) :int)
                            (CaseExpr''emitThenForInts this, objx, gen, primExprType, (get (:tests this) i), (get (:thens this) i), defaultLabel, emitUnboxed)
                        (contains? (:skipCheck this) i)
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
            (throw! "can't eval case")
        )

        (#_"void" Expr'''emit [#_"CaseExpr" this, #_"Context" context, #_"IopObject" objx, #_"GeneratorAdapter" gen]
            (CaseExpr''doEmit this, context, objx, gen, false)
            nil
        )

        (#_"Class" Expr'''getClass [#_"CaseExpr" this]
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
            (#_"Expr" IParser'''parse [#_"IParser" _self, #_"Context" context, #_"ISeq" form]
                (if (= context :Context'EVAL)
                    (Compiler'analyze context, (list (list Compiler'FNONCE [] form)))
                    (let [#_"IPersistentVector" args (vec (next form))
                          #_"Object" exprForm (nth args 0)
                          #_"int" shift (.intValue (nth args 1))
                          #_"int" mask (.intValue (nth args 2))
                          #_"Object" defaultForm (nth args 3)
                          #_"IPersistentMap" caseMap (nth args 4)
                          #_"Keyword" switchType (nth args 5)
                          #_"Keyword" testType (nth args 6)
                          #_"IPersistentSet" skipCheck (when (< 7 (count args)) (nth args 7))
                          #_"ISeq" keys (keys caseMap)
                          #_"int" low (.intValue (first keys))
                          #_"int" high (.intValue (nth keys (dec (count keys))))
                          #_"LocalBindingExpr" testExpr (Compiler'analyze :Context'EXPRESSION, exprForm)
                          [#_"sorted {Integer Expr}" tests #_"{Integer Expr}" thens]
                            (loop-when [tests (sorted-map) thens {} #_"ISeq" s (seq caseMap)] (some? s) => [tests thens]
                                (let [#_"IMapEntry" e (first s)
                                      #_"Integer" minhash (.intValue (key e)) #_"Object" pair (val e) ;; [test-val then-expr]
                                      #_"Expr" test (if (= testType :int) (NumberExpr'parse (.intValue (first pair))) (ConstantExpr'new (first pair)))
                                      #_"Expr" then (Compiler'analyze context, (second pair))]
                                    (recur (assoc tests minhash test) (assoc thens minhash then) (next s))
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
            'def           (DefParser'new)
            'loop*         (LetParser'new)
            'recur         (RecurParser'new)
            'if            (IfParser'new)
            'case*         (CaseParser'new)
            'let*          (LetParser'new)
            'letfn*        (LetFnParser'new)
            'do            (BodyParser'new)
            'fn*           nil
            'quote         (ConstantParser'new)
            'var           (TheVarParser'new)
            'import*       (ImportParser'new)
            '.             (HostParser'new)
            'set!          (AssignParser'new)
            'deftype*      (DeftypeParser'new)
            'reify*        (ReifyParser'new)
            'try           (TryParser'new)
            'throw         (ThrowParser'new)
            'monitor-enter (MonitorEnterParser'new)
            'monitor-exit  (MonitorExitParser'new)
            'catch         nil
            'finally       nil
            'new           (NewParser'new)
            '&             nil
        )
    )

    (declare list*)

    (defn #_"Object" Compiler'macroexpand1 [#_"Object" form]
        (when (seq? form) => form
            (let-when [#_"Object" op (first form)] (not (Compiler'isSpecial op)) => form
                (let-when [#_"Var" v (Compiler'isMacro op)] (nil? v) => (apply v form *local-env* (next form)) ;; macro expansion
                    (when (symbol? op) => form
                        (let [#_"String" n (:name op)]
                            ;; (.substring s 2 5) => (. s substring 2 5)
                            (cond
                                (= (nth n 0) \.)
                                    (when (< 1 (count form)) => (throw! "malformed member expression, expecting (.member target ...)")
                                        (let [#_"Object" target (second form)
                                              target
                                                (when (some? (Interop'maybeClass target, false)) => target
                                                    (with-meta (list 'cloiure.core/identity target) {:tag 'java.lang.Class})
                                                )]
                                            (Compiler'preserveTag form, (list* '. target (symbol (.substring n, 1)) (next (next form))))
                                        )
                                    )
                                (Compiler'namesStaticMember op)
                                    (let-when [#_"Symbol" target (symbol (:ns op))] (some? (Interop'maybeClass target, false)) => form
                                        (Compiler'preserveTag form, (list* '. target (symbol n) (next form)))
                                    )
                                :else
                                    ;; (s.substring ...) => (. s substring ...)
                                    ;; (package.class.name ...) => (. package.class name ...)
                                    ;; (StringBuilder. ...) => (new StringBuilder ...)
                                    (let-when [#_"int" i (.lastIndexOf n, (int \.))] (= i (dec (count n))) => form
                                        (list* 'new (symbol (.substring n, 0, i)) (next form))
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
                        (when-some [#_"LocalBinding" lb (Compiler'referenceLocal sym)]
                            (LocalBindingExpr'new lb, tag)
                        )
                    (nil? (Compiler'namespaceFor sym))
                        (when-some [#_"Class" c (Interop'maybeClass (symbol (:ns sym)), false)]
                            (when (some? (Reflector'getField c, (:name sym), true)) => (throw! (str "unable to find static field: " (:name sym) " in " c))
                                (StaticFieldExpr'new *line*, c, (:name sym), tag)
                            )
                        )
                )
                (let [#_"Object" o (Compiler'resolve sym)]
                    (cond
                        (var? o)
                            (when (nil? (Compiler'isMacro o)) => (throw! (str "can't take value of a macro: " o))
                                (Compiler'registerVar o)
                                (VarExpr'new o, tag)
                            )
                        (class? o)
                            (ConstantExpr'new o)
                        (symbol? o)
                            (UnresolvedVarExpr'new o)
                        :else
                            (throw! (str "unable to resolve symbol: " sym " in this context"))
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
                    (let-when [#_"Object" op (first form)] (some? op) => (throw! (str "can't call nil, form: " form))
                        (let [#_"IFn" inline (Compiler'isInline op, (count (next form)))]
                            (cond
                                (some? inline)
                                    (Compiler'analyze context, (Compiler'preserveTag form, (IFn'''applyTo inline, (next form))))
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
                    nil                 Compiler'NIL_EXPR
                    true                Compiler'TRUE_EXPR
                    false               Compiler'FALSE_EXPR
                    (cond
                        (symbol? form)  (Compiler'analyzeSymbol form)
                        (keyword? form) (Compiler'registerKeyword form)
                        (number? form)  (NumberExpr'parse form)
                        (string? form)  (StringExpr'new (.intern ^String form))
                        (and (coll? form) (not (satisfies? IType form)) (zero? (count form)))
                            (let-when [#_"Expr" e (EmptyExpr'new form)] (some? (meta form)) => e
                                (MetaExpr'new e, (MapExpr'parse (if (= context :Context'EVAL) context :Context'EXPRESSION), (meta form)))
                            )
                        (seq? form)     (Compiler'analyzeSeq context, form, name)
                        (vector? form)  (VectorExpr'parse context, form)
                        (map? form)     (MapExpr'parse context, form)
                        (set? form)     (SetExpr'parse context, form)
                        :else           (ConstantExpr'new form)
                    )
                )
            )
        )
    )

    (defn #_"ClassLoader" Compiler'baseLoader []
        (if (bound? #'*class-loader*)
            *class-loader*
            (.getContextClassLoader (Thread/currentThread))
        )
    )

    (declare DynamicClassLoader'new)

    (defn #_"ClassLoader" Compiler'makeClassLoader []
        (cast ClassLoader
            (AccessController/doPrivileged
                (reify PrivilegedAction
                    #_foreign
                    (#_"Object" run [#_"PrivilegedAction" _self]
                        (DynamicClassLoader'new (Compiler'baseLoader))
                    )
                )
            )
        )
    )

    (defn #_"Object" Compiler'eval [#_"Object" form]
        (let [#_"IPersistentMap" meta (meta form)]
            (binding [*class-loader* (Compiler'makeClassLoader), *line* (if (contains? meta :line) (get meta :line) *line*)]
                (let [form (Compiler'macroexpand form)]
                    (cond
                        (and (seq? form) (= (first form) 'do))
                            (loop-when-recur [#_"ISeq" s (next form)] (some? (next s)) [(next s)] => (Compiler'eval (first s))
                                (Compiler'eval (first s))
                            )
                        (or (satisfies? IType form) (and (coll? form) (not (and (symbol? (first form)) (.startsWith (:name (first form)), "def")))))
                            (let [#_"IopObject" fexpr (Compiler'analyze :Context'EXPRESSION, (list 'fn* [] form), (str "eval" (RT'nextID)))]
                                (IFn'''invoke (Expr'''eval fexpr))
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
)
)

(java-ns cloiure.lang.LispReader

(class-ns LispReader
    (def #_"Var" ^:dynamic *arg-env*   ) ;; sorted-map num->gensymbol
    (def #_"Var" ^:dynamic *gensym-env*) ;; symbol->gensymbol

    (defn #_"Symbol" LispReader'garg [#_"int" n]
        (symbol (str (if (= n -1) "rest" (str "p" n)) "__" (RT'nextID) "#"))
    )

    (defn #_"Symbol" LispReader'registerArg [#_"int" n]
        (when (bound? #'*arg-env*) => (throw! "arg literal not in #()")
            (or (get *arg-env* n)
                (let [#_"Symbol" sym (LispReader'garg n)]
                    (update! *arg-env* assoc n sym)
                    sym
                )
            )
        )
    )

    (defn #_"Symbol" LispReader'registerGensym [#_"Symbol" sym]
        (when (bound? #'*gensym-env*) => (throw! "gensym literal not in syntax-quote")
            (or (get *gensym-env* sym)
                (let [#_"Symbol" gsym (symbol (str (:name sym) "__" (RT'nextID) "__auto__"))]
                    (update! *gensym-env* assoc sym gsym)
                    gsym
                )
            )
        )
    )

    (declare LispReader'macros)

    (defn- #_"boolean" LispReader'isMacro [#_"char" ch]
        (contains? LispReader'macros ch)
    )

    (defn- #_"boolean" LispReader'isTerminatingMacro [#_"char" ch]
        (and (LispReader'isMacro ch) (not (any = ch \# \' \%)))
    )

    (defn #_"boolean" LispReader'isDigit [#_"char" ch, #_"int" base]
        (not= (Character/digit ch, base) -1)
    )

    (defn #_"boolean" LispReader'isWhitespace [#_"char" ch]
        (or (Character/isWhitespace ch) (= ch \,))
    )

    (defn #_"Character" LispReader'read1 [#_"Reader" r]
        (let [#_"int" c (.read r)]
            (when-not (= c -1)
                (char c)
            )
        )
    )

    (defn #_"void" LispReader'unread [#_"PushbackReader" r, #_"Character" ch]
        (when (some? ch)
            (.unread r, (int ch))
        )
        nil
    )

    (defn- #_"void" LispReader'consumeWhitespaces [#_"PushbackReader" r]
        (loop-when-recur [#_"char" ch (LispReader'read1 r)] (LispReader'isWhitespace ch) [(LispReader'read1 r)] => (LispReader'unread r, ch))
        nil
    )

    (def- #_"Pattern" LispReader'rxInteger #"([-+]?)(?:(0)|([1-9][0-9]*)|0[xX]([0-9A-Fa-f]+)|0([0-7]+)|([1-9][0-9]?)[rR]([0-9A-Za-z]+)|0[0-9]+)")
    (def- #_"Pattern" LispReader'rxRatio   #"([-+]?[0-9]+)/([0-9]+)")

    (declare Numbers'divide)

    (defn- #_"Object" LispReader'matchNumber [#_"String" s]
        (let [_ (or
                    (let-when [#_"Matcher" m (.matcher LispReader'rxInteger, s)] (.matches m)
                        (when (nil? (.group m, 2)) => (Long/valueOf 0)
                            (let [[#_"String" n #_"int" radix]
                                    (cond-let
                                        [n (.group m, 3)] [n 10]
                                        [n (.group m, 4)] [n 16]
                                        [n (.group m, 5)] [n 8]
                                        [n (.group m, 7)] [n (Integer/parseInt (.group m, 6))]
                                    )]
                                (when (some? n) => :nil
                                    (let [#_"BigInteger" bn (BigInteger. n, radix) bn (if (= (.group m, 1) "-") (.negate bn) bn)]
                                        (when (< (.bitLength bn) 64) => bn
                                            (Long/valueOf (.longValue bn))
                                        )
                                    )
                                )
                            )
                        )
                    )
                    (let-when [#_"Matcher" m (.matcher LispReader'rxRatio, s)] (.matches m)
                        (let [#_"String" n (.group m, 1) n (if (.startsWith n, "+") (.substring n, 1) n)]
                            (Numbers'divide (BigInteger. n), (BigInteger. (.group m, 2)))
                        )
                    )
                )]
            (when-not (= _ :nil) _)
        )
    )

    (defn- #_"Object" LispReader'readNumber [#_"PushbackReader" r, #_"char" ch]
        (let [#_"String" s
                (let [#_"StringBuilder" sb (StringBuilder.) _ (.append sb, ch)]
                    (loop []
                        (let [ch (LispReader'read1 r)]
                            (if (or (nil? ch) (LispReader'isWhitespace ch) (LispReader'isMacro ch))
                                (do
                                    (LispReader'unread r, ch)
                                    (.toString sb)
                                )
                                (do
                                    (.append sb, ch)
                                    (recur)
                                )
                            )
                        )
                    )
                )]
            (or (LispReader'matchNumber s) (throw! (str "invalid number: " s)))
        )
    )

    (defn- #_"String" LispReader'readToken [#_"PushbackReader" r, #_"char" ch]
        (let [#_"StringBuilder" sb (StringBuilder.) _ (.append sb, ch)]
            (loop []
                (let [ch (LispReader'read1 r)]
                    (if (or (nil? ch) (LispReader'isWhitespace ch) (LispReader'isTerminatingMacro ch))
                        (do
                            (LispReader'unread r, ch)
                            (.toString sb)
                        )
                        (do
                            (.append sb, ch)
                            (recur)
                        )
                    )
                )
            )
        )
    )

    (def- #_"Pattern" LispReader'rxSymbol #"[:]?([\D&&[^/]].*/)?(/|[\D&&[^/]][^/]*)")

    (defn- #_"Object" LispReader'matchSymbol [#_"String" s]
        (let-when [#_"Matcher" m (.matcher LispReader'rxSymbol, s)] (.matches m)
            (let [#_"String" ns (.group m, 1) #_"String" n (.group m, 2)]
                (cond
                    (or (and (some? ns) (.endsWith ns, ":/")) (.endsWith n, ":") (not= (.indexOf s, "::", 1) -1))
                        nil
                    (.startsWith s, "::")
                        (let [#_"Symbol" ks (symbol (.substring s, 2))
                              #_"Namespace" kns (if (some? (:ns ks)) (Namespace''getAlias *ns*, (symbol (:ns ks))) *ns*)]
                            ;; auto-resolving keyword
                            (when (some? kns)
                                (keyword (:name (:name kns)) (:name ks))
                            )
                        )
                    :else
                        (let [#_"boolean" kw? (= (nth s 0) \:) #_"Symbol" sym (symbol (.substring s, (if kw? 1 0)))]
                            (if kw? (keyword sym) sym)
                        )
                )
            )
        )
    )

    (defn- #_"Object" LispReader'interpretToken [#_"String" s]
        (case s "nil" nil "true" true "false" false
            (or (LispReader'matchSymbol s) (throw! (str "invalid token: " s)))
        )
    )

    (defn #_"Object" LispReader'read
        ([#_"PushbackReader" r] (LispReader'read r, true, nil))
        ([#_"PushbackReader" r, #_"boolean" eofIsError, #_"Object" eofValue] (LispReader'read r, eofIsError, eofValue, nil, nil))
        ([#_"PushbackReader" r, #_"boolean" eofIsError, #_"Object" eofValue, #_"Character" returnOn, #_"Object" returnOnValue]
            (loop []
                (let [#_"char" ch (loop-when-recur [ch (LispReader'read1 r)] (LispReader'isWhitespace ch) [(LispReader'read1 r)] => ch)]
                    (cond
                        (nil? ch)
                            (if eofIsError (throw! "EOF while reading") eofValue)
                        (and (some? returnOn) (= returnOn ch))
                            returnOnValue
                        (LispReader'isDigit ch, 10)
                            (LispReader'readNumber r, ch)
                        :else
                            (let [#_"IFn" fn (get LispReader'macros ch)]
                                (if (some? fn)
                                    (let [#_"Object" o (fn r ch)]
                                        ;; no op macros return the reader
                                        (recur-if (identical? o r) [] => o)
                                    )
                                    (do
                                        (when (any = ch \+ \-)
                                            (let [#_"char" ch2 (LispReader'read1 r)]
                                                (when (LispReader'isDigit ch2, 10)
                                                    (LispReader'unread r, ch2)
                                                    (ß return (LispReader'readNumber r, ch))
                                                )
                                                (LispReader'unread r, ch2)
                                            )
                                        )
                                        (LispReader'interpretToken (LispReader'readToken r, ch))
                                    )
                                )
                            )
                    )
                )
            )
        )
    )

    (defn- #_"int" LispReader'scanDigits [#_"String" token, #_"int" offset, #_"int" n, #_"int" base]
        (when (= (+ offset n) (count token)) => (throw! (str "invalid unicode character: \\" token))
            (loop-when [#_"int" c 0 #_"int" i 0] (< i n) => c
                (let [#_"char" ch (nth token (+ offset i)) #_"int" d (Character/digit ch, base)]
                    (when-not (= d -1) => (throw! (str "invalid digit: " ch))
                        (recur (+ (* c base) d) (inc i))
                    )
                )
            )
        )
    )

    (defn- #_"int" LispReader'readDigits [#_"PushbackReader" r, #_"char" ch, #_"int" base, #_"int" n, #_"boolean" exact?]
        (let-when-not [#_"int" c (Character/digit ch, base)] (= c -1) => (throw! (str "invalid digit: " ch))
            (let [[c #_"int" i]
                    (loop-when [c c i 1] (< i n) => [c i]
                        (let [ch (LispReader'read1 r)]
                            (if (or (nil? ch) (LispReader'isWhitespace ch) (LispReader'isMacro ch))
                                (do
                                    (LispReader'unread r, ch)
                                    [c i]
                                )
                                (let [#_"int" d (Character/digit ch, base)]
                                    (when-not (= d -1) => (throw! (str "invalid digit: " ch))
                                        (recur (+ (* c base) d) (inc i))
                                    )
                                )
                            )
                        )
                    )]
                (when (or (= i n) (not exact?)) => (throw! (str "invalid character length: " i ", should be: " n))
                    c
                )
            )
        )
    )

    (def- #_"Object" LispReader'READ_EOF (Object.))
    (def- #_"Object" LispReader'READ_FINISHED (Object.))

    (defn #_"PersistentVector" LispReader'readDelimitedForms [#_"PushbackReader" r, #_"char" delim]
        (loop [#_"PersistentVector" v []]
            (let [#_"Object" form (LispReader'read r, false, LispReader'READ_EOF, delim, LispReader'READ_FINISHED)]
                (condp identical? form
                    LispReader'READ_EOF
                        (throw! "EOF while reading")
                    LispReader'READ_FINISHED
                        v
                    (recur (conj v form))
                )
            )
        )
    )
)

(class-ns RegexReader
    (defn #_"Object" regex-reader [#_"PushbackReader" r, #_"char" _delim]
        (let [#_"StringBuilder" sb (StringBuilder.)]
            (loop []
                (let-when [#_"char" ch (LispReader'read1 r)] (some? ch) => (throw! "EOF while reading regex")
                    (when-not (= ch \") ;; oops! "
                        (.append sb, ch)
                        (when (= ch \\) ;; escape
                            (let-when [ch (LispReader'read1 r)] (some? ch) => (throw! "EOF while reading regex")
                                (.append sb, ch)
                            )
                        )
                        (recur)
                    )
                )
            )
            (Pattern/compile (.toString sb))
        )
    )
)

(class-ns StringReader
    (defn- #_"char" StringReader'escape [#_"PushbackReader" r]
        (let-when [#_"char" ch (LispReader'read1 r)] (some? ch) => (throw! "EOF while reading string")
            (case ch
                \t  \tab
                \r  \return
                \n  \newline
                \\  ch
                \"  ch ;; oops! "
                \b  \backspace
                \f  \formfeed
                \u  (let [ch (LispReader'read1 r)]
                        (when (LispReader'isDigit ch, 16) => (throw! (str "invalid unicode escape: \\u" ch))
                            (char (LispReader'readDigits r, ch, 16, 4, true))
                        )
                    )
                (when (LispReader'isDigit ch, #_8 4) => (throw! (str "unsupported escape character: \\" ch))
                    (let [#_"int" c (LispReader'readDigits r, ch, 8, 3, false)]
                      #_(when (< 0377 c)
                            (throw! "octal escape sequence must be in range [0, 377]")
                        )
                        (char c)
                    )
                )
            )
        )
    )

    (defn #_"Object" string-reader [#_"PushbackReader" r, #_"char" _delim]
        (let [#_"StringBuilder" sb (StringBuilder.)]
            (loop []
                (let-when [#_"char" ch (LispReader'read1 r)] (some? ch) => (throw! "EOF while reading string")
                    (when-not (= ch \") ;; oops! "
                        (.append sb, (if (= ch \\) (StringReader'escape r) ch))
                        (recur)
                    )
                )
            )
            (.toString sb)
        )
    )
)

(class-ns CommentReader
    (defn #_"Object" comment-reader [#_"PushbackReader" r, #_"char" _delim]
        (while (not (any = (LispReader'read1 r) nil \newline \return)))
        r
    )
)

(class-ns DiscardReader
    (defn #_"Object" discard-reader [#_"PushbackReader" r, #_"char" _delim]
        (LispReader'read r)
        r
    )
)

(class-ns QuoteReader
    (defn #_"Object" quote-reader [#_"PushbackReader" r, #_"char" _delim]
        (list 'quote (LispReader'read r))
    )
)

(class-ns DerefReader
    (defn #_"Object" deref-reader [#_"PushbackReader" r, #_"char" _delim]
        (list 'cloiure.core/deref (LispReader'read r))
    )
)

(class-ns VarReader
    (defn #_"Object" var-reader [#_"PushbackReader" r, #_"char" _delim]
        (list 'var (LispReader'read r))
    )
)

(class-ns DispatchReader
    (declare LispReader'dispatchMacros)

    (defn #_"Object" dispatch-reader [#_"PushbackReader" r, #_"char" _delim]
        (let-when [#_"char" ch (LispReader'read1 r)] (some? ch) => (throw! "EOF while reading character")
            (let-when [#_"IFn" fn (get LispReader'dispatchMacros ch)] (nil? fn) => (fn r ch)
                (LispReader'unread r, ch)
                (throw! (str "no dispatch macro for: " ch))
            )
        )
    )
)

(declare rseq)

(class-ns FnReader
    (defn #_"Object" fn-reader [#_"PushbackReader" r, #_"char" _delim]
        (when-not (bound? #'*arg-env*) => (throw! "nested #()s are not allowed")
            (binding [*arg-env* (sorted-map)]
                (LispReader'unread r, \()
                (let [#_"PersistentVector" args []
                      args
                        (let-when [#_"ISeq" rs (rseq *arg-env*)] (some? rs) => args
                            (let [args
                                    (let-when [#_"int" n (key (first rs))] (pos? n) => args
                                        (loop-when-recur [args args #_"int" i 1]
                                                         (<= i n)
                                                         [(conj args (or (get *arg-env* i) (LispReader'garg i))) (inc i)]
                                                      => args
                                        )
                                    )]
                                (let-when [#_"Object" rest (get *arg-env* -1)] (some? rest) => args
                                    (conj args '& rest)
                                )
                            )
                        )]
                    (list 'fn* args (LispReader'read r))
                )
            )
        )
    )
)

(class-ns ArgReader
    (defn #_"Object" arg-reader [#_"PushbackReader" r, #_"char" _delim]
        (when (bound? #'*arg-env*) => (LispReader'interpretToken (LispReader'readToken r, \%))
            (let [#_"char" ch (LispReader'read1 r) _ (LispReader'unread r, ch)]
                ;; % alone is first arg
                (if (or (nil? ch) (LispReader'isWhitespace ch) (LispReader'isTerminatingMacro ch))
                    (LispReader'registerArg 1)
                    (let [#_"Object" n (LispReader'read r)]
                        (cond
                            (= n '&)    (LispReader'registerArg -1)
                            (number? n) (LispReader'registerArg (.intValue ^Number n))
                            :else       (throw! "arg literal must be %, %& or %integer")
                        )
                    )
                )
            )
        )
    )
)

(class-ns MetaReader
    (defn #_"Object" meta-reader [#_"PushbackReader" r, #_"char" _delim]
        (let [#_"Object" meta (LispReader'read r)
              meta
                (cond
                    (or (symbol? meta) (string? meta)) {:tag meta}
                    (keyword? meta)                         {meta true}
                    (map? meta)                              meta
                    :else (throw! "metadata must be Symbol, Keyword, String or Map")
                )
              #_"Object" o (LispReader'read r)]
            (when (satisfies? IMeta o) => (throw! "metadata can only be applied to IMetas")
                (if (satisfies? IReference o)
                    (do
                        (reset-meta! o meta)
                        o
                    )
                    (let [#_"IPersistentMap" m
                            (loop-when [m (meta o) #_"ISeq" s (seq meta)] (some? s) => m
                                (let [#_"IMapEntry" e (first s)]
                                    (recur (assoc m (key e) (val e)) (next s))
                                )
                            )]
                        (with-meta o m)
                    )
                )
            )
        )
    )
)

(class-ns SyntaxQuoteReader
    (defn- #_"IPersistentVector" SyntaxQuoteReader'flattened [#_"IPersistentMap" m]
        (loop-when [#_"IPersistentVector" v [] #_"ISeq" s (seq m)] (some? s) => v
            (let [#_"IMapEntry" e (first s)]
                (recur (conj v (key e) (val e)) (next s))
            )
        )
    )

    (defn #_"boolean" SyntaxQuoteReader'isUnquote [#_"Object" form]
        (and (seq? form) (= (first form) 'cloiure.core/unquote))
    )

    (defn #_"boolean" SyntaxQuoteReader'isUnquoteSplicing [#_"Object" form]
        (and (seq? form) (= (first form) 'cloiure.core/unquote-splicing))
    )

    (declare SyntaxQuoteReader'syntaxQuote)

    (defn- #_"ISeq" SyntaxQuoteReader'sqExpandList [#_"ISeq" s]
        (loop-when [#_"PersistentVector" v [] s s] (some? s) => (seq v)
            (let [#_"Object" item (first s)
                  v (cond
                        (SyntaxQuoteReader'isUnquote item)         (conj v (list 'cloiure.core/list (second item)))
                        (SyntaxQuoteReader'isUnquoteSplicing item) (conj v (second item))
                        :else                                      (conj v (list 'cloiure.core/list (SyntaxQuoteReader'syntaxQuote item)))
                    )]
                (recur v (next s))
            )
        )
    )

    (defn #_"Object" SyntaxQuoteReader'syntaxQuote [#_"Object" form]
        (let [#_"Object" q
                (cond
                    (Compiler'isSpecial form)
                        (list 'quote form)
                    (symbol? form)
                        (let [#_"String" ns (:ns form) #_"String" n (:name form)
                              form
                                (cond
                                    (and (nil? ns) (.endsWith n, "#"))
                                        (LispReader'registerGensym (symbol (.substring n, 0, (dec (count n)))))
                                    (and (nil? ns) (.endsWith n, "."))
                                        (symbol (str (:name (Compiler'resolveSymbol (symbol (.substring n, 0, (dec (count n)))))) "."))
                                    (and (nil? ns) (.startsWith n, "."))
                                        form ;; simply quote method names
                                    :else
                                        (let-when [#_"Object" c (when (some? ns) (Namespace''getMapping *ns*, (symbol ns)))] (class? c) => (Compiler'resolveSymbol form)
                                            ;; Classname/foo -> package.qualified.Classname/foo
                                            (symbol (.getName c) n)
                                        )
                                )]
                            (list 'quote form)
                        )
                    (SyntaxQuoteReader'isUnquote form)
                        (ß return (second form))
                    (SyntaxQuoteReader'isUnquoteSplicing form)
                        (throw! "splice not in list")
                    (coll? form)
                        (cond
                            (map? form)
                                (list 'cloiure.core/apply 'cloiure.core/hash-map (list 'cloiure.core/seq (cons 'cloiure.core/concat (SyntaxQuoteReader'sqExpandList (seq (SyntaxQuoteReader'flattened form))))))
                            (vector? form)
                                (list 'cloiure.core/apply 'cloiure.core/vector (list 'cloiure.core/seq (cons 'cloiure.core/concat (SyntaxQuoteReader'sqExpandList (seq form)))))
                            (set? form)
                                (list 'cloiure.core/apply 'cloiure.core/hash-set (list 'cloiure.core/seq (cons 'cloiure.core/concat (SyntaxQuoteReader'sqExpandList (seq form)))))
                            (or (seq? form) (list? form))
                                (let-when [#_"ISeq" s (seq form)] (some? s) => (cons 'cloiure.core/list nil)
                                    (list 'cloiure.core/seq (cons 'cloiure.core/concat (SyntaxQuoteReader'sqExpandList s)))
                                )
                            :else
                                (throw! "unknown collection type")
                        )
                    (or (keyword? form) (number? form) (char? form) (string? form))
                        form
                    :else
                        (list 'quote form)
                )]
            (when (and (satisfies? IObj form) (seq (dissoc (meta form) :line :column))) => q
                (list 'cloiure.core/with-meta q (SyntaxQuoteReader'syntaxQuote (meta form)))
            )
        )
    )

    (defn #_"Object" syntax-quote-reader [#_"PushbackReader" r, #_"char" _delim]
        (binding [*gensym-env* {}]
            (SyntaxQuoteReader'syntaxQuote (LispReader'read r))
        )
    )
)

(class-ns UnquoteReader
    (defn #_"Object" unquote-reader [#_"PushbackReader" r, #_"char" _delim]
        (let-when [#_"char" ch (LispReader'read1 r)] (some? ch) => (throw! "EOF while reading character")
            (if (= ch \@)
                (list 'cloiure.core/unquote-splicing (LispReader'read r))
                (do
                    (LispReader'unread r, ch)
                    (list 'cloiure.core/unquote (LispReader'read r))
                )
            )
        )
    )
)

(class-ns CharacterReader
    (defn #_"Object" character-reader [#_"PushbackReader" r, #_"char" _delim]
        (let-when [#_"char" ch (LispReader'read1 r)] (some? ch) => (throw! "EOF while reading character")
            (let [#_"String" token (LispReader'readToken r, ch)]
                (when-not (= (count token) 1) => (Character/valueOf (nth token 0))
                    (case token
                        "newline"   \newline
                        "space"     \space
                        "tab"       \tab
                        "backspace" \backspace
                        "formfeed"  \formfeed
                        "return"    \return
                        (case (nth token 0)
                            \u  (let [#_"int" c (LispReader'scanDigits token, 1, 4, 16)]
                                    (when (<= 0xd800 c 0xdfff) ;; surrogate code unit?
                                        (throw! (str "invalid character constant: \\u" (Integer/toString c, 16)))
                                    )
                                    (char c)
                                )
                            \o  (let [#_"int" n (dec (count token))]
                                    (when (< 3 n)
                                        (throw! (str "invalid octal escape sequence length: " n))
                                    )
                                    (let [#_"int" c (LispReader'scanDigits token, 1, n, 8)]
                                        (when (< 0377 c)
                                            (throw! "octal escape sequence must be in range [0, 377]")
                                        )
                                        (char c)
                                    )
                                )
                            (throw! (str "unsupported character: \\" token))
                        )
                    )
                )
            )
        )
    )
)

(class-ns ListReader
    (defn #_"Object" list-reader [#_"PushbackReader" r, #_"char" _delim]
        (let-when [#_"PersistentVector" v (LispReader'readDelimitedForms r, \))] (seq v) => ()
            (PersistentList/create #_(to-array v) v)
        )
    )
)

(class-ns VectorReader
    (defn #_"Object" vector-reader [#_"PushbackReader" r, #_"char" _delim]
        (#_"LazilyPersistentVector'create" identity (LispReader'readDelimitedForms r, \]))
    )
)

(class-ns MapReader
    (defn #_"Object" map-reader [#_"PushbackReader" r, #_"char" _delim]
        (let [#_"PersistentVector" v (LispReader'readDelimitedForms r, \})]
            (when (even? (count v)) => (throw! "map literal must contain an even number of forms")
                (RT'map (to-array v))
            )
        )
    )
)

(declare PersistentHashSet'createWithCheck-1s)

(class-ns SetReader
    (defn #_"Object" set-reader [#_"PushbackReader" r, #_"char" _delim]
        (PersistentHashSet'createWithCheck-1s (LispReader'readDelimitedForms r, \}))
    )
)

(class-ns UnmatchedDelimiterReader
    (defn #_"Object" unmatched-delimiter-reader [#_"PushbackReader" _r, #_"char" delim]
        (throw! (str "unmatched delimiter: " delim))
    )
)

(class-ns LispReader
    (def #_"{char IFn}" LispReader'macros
        (hash-map
            \"  string-reader ;; oops! "
            \;  comment-reader
            \'  quote-reader
            \@  deref-reader
            \^  meta-reader
            \`  syntax-quote-reader
            \~  unquote-reader
            \(  list-reader,    \) unmatched-delimiter-reader
            \[  vector-reader,  \] unmatched-delimiter-reader
            \{  map-reader,     \} unmatched-delimiter-reader
            \\  character-reader
            \%  arg-reader
            \#  dispatch-reader
        )
    )

    (def #_"{char IFn}" LispReader'dispatchMacros
        (hash-map
            \^  meta-reader
            \'  var-reader
            \"  regex-reader ;; oops! "
            \(  fn-reader
            \{  set-reader
            \!  comment-reader
            \_  discard-reader
        )
    )
)
)

(java-ns cloiure.lang.Compiler

(class-ns Compiler
    (defn #_"Object" Compiler'load [#_"Reader" reader]
        (let [#_"PushbackReader" r (if (instance? PushbackReader reader) reader (PushbackReader. reader))
              #_"Object" EOF (Object.)]
            (binding [*ns* *ns*, *warn-on-reflection* *warn-on-reflection*, *line* 0]
                (loop [#_"Object" val nil]
                    (LispReader'consumeWhitespaces r)
                    (let-when-not [#_"Object" form (LispReader'read r, false, EOF)] (identical? form EOF) => val
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

;;;
 ; Evaluates the form data structure (not text!) and returns the result.
 ;;
(defn eval [form] (Compiler'eval form))

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

    (declare bit-xor)

    (defn- #_"int" Murmur3'mixH1 [#_"int" h1, #_"int" k1]
        (-> h1 (bit-xor k1) (Integer/rotateLeft 13) (* 5) (+ 0xe6546b64))
    )

    (declare unsigned-bit-shift-right)

    ;; finalization mix - force all bits of a hash block to avalanche
    (defn- #_"int" Murmur3'fmix [#_"int" h1, #_"int" n]
        (let [h1 (bit-xor h1 n)    h1 (bit-xor h1 (unsigned-bit-shift-right h1 16))
              h1 (* h1 0x85ebca6b) h1 (bit-xor h1 (unsigned-bit-shift-right h1 13))
              h1 (* h1 0xc2b2ae35) h1 (bit-xor h1 (unsigned-bit-shift-right h1 16))]
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
                  #_"int" high (int (unsigned-bit-shift-right input 32))
                  #_"int" k1 (Murmur3'mixK1 low)
                  #_"int" h1 (Murmur3'mixH1 Murmur3'seed, k1)
                  k1 (Murmur3'mixK1 high)
                  h1 (Murmur3'mixH1 h1, k1)]
                (Murmur3'fmix h1, 8)
            )
        )
    )

    (declare bit-or)
    (declare bit-shift-left)
    (declare odd?)

    (defn #_"int" Murmur3'hashUnencodedChars [#_"CharSequence" s]
        (let [#_"int" h1 ;; step through the input 2 chars at a time
                (loop-when [h1 Murmur3'seed #_"int" i 1] (< i (.length s)) => h1
                    (let [#_"int" k1 (bit-or (.charAt s, (dec i)) (bit-shift-left (.charAt s, i) 16))]
                        (recur (Murmur3'mixH1 h1, (Murmur3'mixK1 k1)) (+ i 2))
                    )
                )
              h1 ;; deal with any remaining characters
                (when (odd? (.length s)) => h1
                    (let [#_"int" k1 (.charAt s, (dec (.length s)))]
                        (bit-xor h1 (Murmur3'mixK1 k1))
                    )
                )]
            (Murmur3'fmix h1, (* 2 (.length s)))
        )
    )

    (defn #_"int" Murmur3'mixCollHash [#_"int" hash, #_"int" n]
        (Murmur3'fmix (Murmur3'mixH1 Murmur3'seed, (Murmur3'mixK1 hash)), n)
    )

    (declare Util'hasheq)

    (defn #_"int" Murmur3'hashOrdered [#_"Seqable" items]
        (loop-when-recur [#_"int" hash 1 #_"int" n 0 #_"ISeq" s (seq items)]
                         (some? s)
                         [(+ (* 31 hash) (Util'hasheq (first s))) (inc n) (next s)]
                      => (Murmur3'mixCollHash hash, n)
        )
    )

    (defn #_"int" Murmur3'hashUnordered [#_"Seqable" items]
        (loop-when-recur [#_"int" hash 0 #_"int" n 0 #_"ISeq" s (seq items)]
                         (some? s)
                         [(+ hash (Util'hasheq (first s))) (inc n) (next s)]
                      => (Murmur3'mixCollHash hash, n)
        )
    )
)
)

(java-ns cloiure.lang.Atom

(class-ns Atom
    (defn #_"Atom" Atom'new
        ([#_"Object" data] (Atom'new nil, data))
        ([#_"IPersistentMap" meta, #_"Object" data]
            (merge (Atom.)
                (hash-map
                    #_"AtomicReference" :meta (AtomicReference. meta)
                    #_"AtomicReference" :data (AtomicReference. data)
                )
            )
        )
    )

    (extend-type Atom IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"Atom" this]
            (.get (:meta this))
        )
    )

    (extend-type Atom IReference
        (#_"IPersistentMap" IReference'''alterMeta [#_"Atom" this, #_"IFn" f, #_"ISeq" args]
            (loop []
                (let [#_"IPersistentMap" m (.get (:meta this)) #_"IPersistentMap" m' (apply f m args)]
                    (when (.compareAndSet (:meta this), m, m') => (recur)
                        m'
                    )
                )
            )
        )

        (#_"IPersistentMap" IReference'''resetMeta [#_"Atom" this, #_"IPersistentMap" m']
            (.set (:meta this), m')
            m'
        )
    )

    (extend-type Atom IDeref
        (#_"Object" IDeref'''deref [#_"Atom" this]
            (.get (:data this))
        )
    )

    (extend-type Atom IAtom
        (#_"boolean" IAtom'''compareAndSet [#_"Atom" this, #_"Object" o, #_"Object" o']
            (.compareAndSet (:data this), o, o')
        )

        (#_"Object" IAtom'''swap [#_"Atom" this, #_"IFn" f, #_"ISeq" args]
            (loop []
                (let [#_"Object" o (.get (:data this)) #_"Object" o' (apply f o args)]
                    (when (.compareAndSet (:data this), o, o') => (recur)
                        o'
                    )
                )
            )
        )

        (#_"Object" IAtom'''reset [#_"Atom" this, #_"Object" o']
            (.set (:data this), o')
            o'
        )

        (#_"[Object Object]" IAtom'''swapVals [#_"Atom" this, #_"IFn" f, #_"ISeq" args]
            (loop []
                (let [#_"Object" o (.get (:data this)) #_"Object" o' (apply f o args)]
                    (when (.compareAndSet (:data this), o, o') => (recur)
                        [o o']
                    )
                )
            )
        )

        (#_"[Object Object]" IAtom'''resetVals [#_"Atom" this, #_"Object" o']
            (loop []
                (let [#_"Object" o (.get (:data this))]
                    (when (.compareAndSet (:data this), o, o') => (recur)
                        [o o']
                    )
                )
            )
        )
    )
)
)

;;;
 ; Creates and returns an Atom with an initial value of x and optional meta m.
 ;;
(§ defn atom
    ([x] (Atom'new x))
    ([m x] (Atom'new m x))
)

;;;
 ; Atomically sets the value of atom to x' if and only if the current value of the atom is identical to x.
 ; Returns true if set happened, else false.
 ;;
(defn compare-and-set! [^cloiure.core.IAtom a x x'] (IAtom'''compareAndSet a x x'))

;;;
 ; Atomically swaps the value of atom to be: (apply f current-value-of-atom args).
 ; Note that f may be called multiple times, and thus should be free of side effects.
 ; Returns the value that was swapped in.
 ;;
(defn swap! [^cloiure.core.IAtom a f & args] (IAtom'''swap a f args))

;;;
 ; Sets the value of atom to x' without regard for the current value.
 ; Returns x'.
 ;;
(defn reset! [^cloiure.core.IAtom a x'] (IAtom'''reset a x'))

;;;
 ; Atomically swaps the value of atom to be: (apply f current-value-of-atom args).
 ; Note that f may be called multiple times, and thus should be free of side effects.
 ; Returns [old new], the value of the atom before and after the swap.
 ;;
(defn ^cloiure.core.IPersistentVector swap-vals! [^cloiure.core.IAtom a f & args] (IAtom'''swapVals a f args))

;;;
 ; Sets the value of atom to x'. Returns [old new], the value of the
 ; atom before and after the reset.
 ;;
(defn ^cloiure.core.IPersistentVector reset-vals! [^cloiure.core.IAtom a x'] (IAtom'''resetVals a x'))

(java-ns cloiure.lang.Volatile

(class-ns Volatile
    (defn #_"Volatile" Volatile'new [#_"Object" o]
        (merge (Volatile.)
            (hash-map
                #_"Object'" :data (AtomicReference. o)
            )
        )
    )

    (extend-type Volatile IDeref
        (#_"Object" IDeref'''deref [#_"Volatile" this]
            (.get (:data this))
        )
    )

    #_method
    (defn #_"Object" Volatile''reset [#_"Volatile" this, #_"Object" o']
        (.set (:data this), o')
        o'
    )
)
)

;;;
 ; Creates and returns a Volatile with an initial value of o.
 ;;
(defn ^Volatile volatile! [o] (Volatile'new o))

;;;
 ; Sets the value of volatile to o without regard for the
 ; current value. Returns o.
 ;;
(defn vreset! [^Volatile v o] (Volatile''reset v o))

;;;
 ; Non-atomically swaps the value of the volatile as if:
 ; (apply f current-value-of-vol args).
 ; Returns the value that was swapped in.
 ;;
(defmacro vswap! [v f & args]
    (let [v (with-meta v {:tag 'cloiure.core.Volatile})]
        `(vreset! ~v (~f (deref ~v) ~@args))
    )
)

;;;
 ; Returns true if x is a volatile.
 ;;
(defn volatile? [x] (instance? Volatile x))

(java-ns cloiure.lang.Util

(class-ns Util
    (declare Numbers'equal)

    (defn #_"boolean" Util'equiv [#_"Object" k1, #_"Object" k2]
        (cond
            (identical? k1 k2)              true
            (nil? k1)                       false
            (and (number? k1) (number? k2)) (Numbers'equal k1, k2)
            (coll? k1)                      (IObject'''equals k1, k2)
            (coll? k2)                      (IObject'''equals k2, k1)
            :else                           (IObject'''equals k1, k2)
        )
    )

    (declare Numbers'compare)

    (defn #_"int" Util'compare [#_"Object" k1, #_"Object" k2]
        (cond
            (= k1 k2)    0
            (nil? k1)    -1
            (nil? k2)    1
            (number? k1) (Numbers'compare k1, k2)
            :else        (.compareTo (cast Comparable k1), k2)
        )
    )

    (declare Numbers'hasheq)

    (defn #_"int" Util'hasheq [#_"Object" o]
        (cond
            (nil? o)               0
            (satisfies? IHashEq o) (IHashEq'''hasheq o)
            (number? o)            (Numbers'hasheq o)
            (string? o)            (Murmur3'hashInt (IObject'''hashCode o))
            :else                  (IObject'''hashCode o)
        )
    )

    (declare bit-shift-right)

    (defn #_"int" Util'hashCombine [#_"int" seed, #_"int" hash]
        ;; a la boost
        (bit-xor seed (+ hash 0x9e3779b9 (bit-shift-left seed 6) (bit-shift-right seed 2)))
    )

    (defn #_"<K, V> void" Util'clearCache [#_"ReferenceQueue" rq, #_"{K Reference<V>}'" cache]
        ;; cleanup any dead entries
        (when (some? (.poll rq))
            (while (some? (.poll rq)))
            (doseq [#_"IMapEntry<K, Reference<V>>" e @cache]
                (let-when [#_"Reference<V>" r (val e)] (and (some? r) (nil? (.get r)))
                    (swap! cache #(if (identical? (get % (key e)) r) (dissoc % (key e)) %))
                )
            )
        )
        nil
    )
)
)

;;;
 ; Equality. Returns true if x equals y, false if not. Same as Java x.equals(y) except it also
 ; works for nil, and compares numbers and collections in a type-independent manner. Cloiure's
 ; immutable data structures define equals() (and thus =) as a value, not an identity, comparison.
 ;;
(§ defn =
    ([x] true)
    ([x y] (Util'equiv x y))
    ([x y & s] (and (= x y) (recur-if (next s) [y (first s) (next s)] => (= y (first s)))))
)

;;;
 ; Same as (not (= obj1 obj2)).
 ;;
(defn ^Boolean not=
    ([x] false)
    ([x y] (not (= x y)))
    ([x y & s] (not (apply = x y s)))
)

;;;
 ; Comparator. Returns a negative number, zero, or a positive number when x is logically
 ; 'less than', 'equal to', or 'greater than' y. Same as Java x.compareTo(y) except it also
 ; works for nil, and compares numbers and collections in a type-independent manner.
 ; x must implement Comparable.
 ;;
(defn compare [x y] (Util'compare x y))

(java-ns cloiure.lang.DynamicClassLoader

(class-ns DynamicClassLoader
    (def #_"{String Reference<Class>}'" DynamicClassLoader'classCache (atom {}))
    (def #_"ReferenceQueue" DynamicClassLoader'RQ (ReferenceQueue.))

    (defn #_"DynamicClassLoader" DynamicClassLoader'new [#_"ClassLoader" parent]
        (merge (DynamicClassLoader.) (§ foreign ClassLoader'new parent))
    )

    #_method
    (defn #_"Class" DynamicClassLoader''defineClass [#_"DynamicClassLoader" this, #_"String" name, #_"byte[]" bytes]
        (Util'clearCache DynamicClassLoader'RQ, DynamicClassLoader'classCache)
        (let [#_"Class" c (.defineClass this, name, bytes, 0, (alength bytes))]
            (swap! DynamicClassLoader'classCache assoc name (SoftReference. c, DynamicClassLoader'RQ))
            c
        )
    )

    (defn #_"Class<?>" DynamicClassLoader'findInMemoryClass [#_"String" name]
        (when-some [#_"Reference<Class>" r (get @DynamicClassLoader'classCache name)]
            (or (.get r) (do (swap! DynamicClassLoader'classCache #(if (identical? (get % name) r) (dissoc % name) %)) nil))
        )
    )

    #_foreign
    (defn #_"Class<?>" findClass---DynamicClassLoader [#_"DynamicClassLoader" this, #_"String" name]
        (or (DynamicClassLoader'findInMemoryClass name) (throw (ClassNotFoundException. name)))
    )
)
)

(java-ns cloiure.lang.Ratio

(class-ns Ratio
    (defn #_"Ratio" Ratio'new [#_"BigInteger" numerator, #_"BigInteger" denominator]
        (merge (Ratio.) (§ foreign Number'new)
            (hash-map
                #_"BigInteger" :n numerator
                #_"BigInteger" :d denominator
            )
        )
    )

    (extend-type Ratio IObject
        (#_"boolean" IObject'''equals [#_"Ratio" this, #_"Object" that]
            (and (instance? Ratio that) (= (:n that) (:n this)) (= (:d that) (:d this)))
        )

        (#_"int" IObject'''hashCode [#_"Ratio" this]
            (bit-xor (IObject'''hashCode (:n this)) (IObject'''hashCode (:d this)))
        )

        (#_"String" IObject'''toString [#_"Ratio" this]
            (str (:n this) "/" (:d this))
        )
    )

    #_method
    (defn #_"BigInteger" Ratio''bigIntegerValue [#_"Ratio" this]
        (.divide (:n this), (:d this))
    )

    #_method
    (defn #_"long" Ratio''longValue [#_"Ratio" this]
        (.longValue (Ratio''bigIntegerValue this))
    )

    #_method
    (defn #_"int" Ratio''intValue [#_"Ratio" this]
        (.intValue (Ratio''bigIntegerValue this))
    )

    #_foreign
    (defn #_"int" compareTo---Ratio [#_"Ratio" this, #_"Object" that]
        (Numbers'compare this, (cast Number that))
    )
)
)

(java-ns cloiure.lang.Numbers

(class-ns LongOps
    (defn #_"LongOps" LongOps'new []
        (LongOps.)
    )

    (defn #_"long" LongOps'gcd [#_"long" u, #_"long" v] (if (§ interop #_"==" v 0) u (recur v (§ interop #_"%" u v))))

    (declare Numbers'RATIO_OPS)
    (declare Numbers'BIGINT_OPS)

    (extend-type LongOps Ops
        (#_"Ops" Ops'''combine [#_"LongOps" this, #_"Ops" y] (Ops'''opsWithLong y, this))

        (#_"Ops" Ops'''opsWithLong [#_"LongOps" this, #_"LongOps" x] this)
        (#_"Ops" Ops'''opsWithRatio [#_"LongOps" this, #_"RatioOps" x] Numbers'RATIO_OPS)
        (#_"Ops" Ops'''opsWithBigInt [#_"LongOps" this, #_"BigIntOps" x] Numbers'BIGINT_OPS)

        (#_"boolean" Ops'''eq [#_"LongOps" this, #_"Number" x, #_"Number" y] (§ interop #_"==" (.longValue x) (.longValue y)))
        (#_"boolean" Ops'''lt [#_"LongOps" this, #_"Number" x, #_"Number" y] (§ interop #_"<" (.longValue x) (.longValue y)))
        (#_"boolean" Ops'''lte [#_"LongOps" this, #_"Number" x, #_"Number" y] (§ interop #_"<=" (.longValue x) (.longValue y)))

        (#_"boolean" Ops'''isZero [#_"LongOps" this, #_"Number" x] (§ interop #_"==" (.longValue x) 0))
        (#_"boolean" Ops'''isPos [#_"LongOps" this, #_"Number" x] (§ interop #_">" (.longValue x) 0))
        (#_"boolean" Ops'''isNeg [#_"LongOps" this, #_"Number" x] (§ interop #_"<" (.longValue x) 0))

        (#_"Number" Ops'''add [#_"LongOps" this, #_"Number" x, #_"Number" y]
            (let [#_"long" lx (.longValue x) #_"long" ly (.longValue y) #_"long" lz (§ interop #_"+" lx ly)]
                (when (and (§ interop #_"<" (§ interop #_"^" lz lx) 0) (§ interop #_"<" (§ interop #_"^" lz ly) 0)) => (Long/valueOf lz)
                    (Ops'''add Numbers'BIGINT_OPS, x, y)
                )
            )
        )

        (#_"Number" Ops'''negate [#_"LongOps" this, #_"Number" x]
            (let [#_"long" lx (.longValue x)]
                (when (§ interop #_"==" lx Long/MIN_VALUE) => (Long/valueOf (§ interop #_"-" lx))
                    (.negate (BigInteger/valueOf lx))
                )
            )
        )

        (#_"Number" Ops'''inc [#_"LongOps" this, #_"Number" x]
            (let [#_"long" lx (.longValue x)]
                (when (§ interop #_"==" lx Long/MAX_VALUE) => (Long/valueOf (§ interop #_"+" lx 1))
                    (Ops'''inc Numbers'BIGINT_OPS, x)
                )
            )
        )

        (#_"Number" Ops'''dec [#_"LongOps" this, #_"Number" x]
            (let [#_"long" lx (.longValue x)]
                (when (§ interop #_"==" lx Long/MIN_VALUE) => (Long/valueOf (§ interop #_"-" lx 1))
                    (Ops'''dec Numbers'BIGINT_OPS, x)
                )
            )
        )
    
        (#_"Number" Ops'''multiply [#_"LongOps" this, #_"Number" x, #_"Number" y]
            (let [#_"long" lx (.longValue x) #_"long" ly (.longValue y)]
                (when-not (and (§ interop #_"==" lx Long/MIN_VALUE) (§ interop #_"<" ly 0)) => (Ops'''multiply Numbers'BIGINT_OPS, x, y)
                    (let [#_"long" lz (§ interop #_"*" lx ly)]
                        (when (or (§ interop #_"==" ly 0) (§ interop #_"==" (§ interop #_"/" lz ly) lx)) => (Ops'''multiply Numbers'BIGINT_OPS, x, y)
                            (Long/valueOf lz)
                        )
                    )
                )
            )
        )

        (#_"Number" Ops'''divide [#_"LongOps" this, #_"Number" x, #_"Number" y]
            (let [#_"long" lx (.longValue x) #_"long" ly (.longValue y)]
                (let-when-not [#_"long" gcd (LongOps'gcd lx, ly)] (§ interop #_"==" gcd 0) => (Long/valueOf 0)
                    (let-when-not [lx (§ interop #_"/" lx gcd) ly (§ interop #_"/" ly gcd)] (§ interop #_"==" ly 1) => (Long/valueOf lx)
                        (let [[lx ly]
                                (when (§ interop #_"<" ly 0) => [lx ly]
                                    [(§ interop #_"-" lx) (§ interop #_"-" ly)]
                                )]
                            (Ratio'new (BigInteger/valueOf lx), (BigInteger/valueOf ly))
                        )
                    )
                )
            )
        )

        (#_"Number" Ops'''quotient [#_"LongOps" this, #_"Number" x, #_"Number" y] (Long/valueOf (§ interop #_"/" (.longValue x) (.longValue y))))
        (#_"Number" Ops'''remainder [#_"LongOps" this, #_"Number" x, #_"Number" y] (Long/valueOf (§ interop #_"%" (.longValue x) (.longValue y))))
)
)

(class-ns RatioOps
    (defn #_"RatioOps" RatioOps'new []
        (RatioOps.)
    )

    (declare Numbers'toRatio)
    (declare Numbers'subtract)
    (declare Numbers'multiply)
    (declare Numbers'lt)
    (declare Numbers'lte)
    (declare Numbers'gte)

    (extend-type RatioOps Ops
        (#_"Ops" Ops'''combine [#_"RatioOps" this, #_"Ops" y] (Ops'''opsWithRatio y, this))

        (#_"Ops" Ops'''opsWithLong [#_"RatioOps" this, #_"LongOps" x] this)
        (#_"Ops" Ops'''opsWithRatio [#_"RatioOps" this, #_"RatioOps" x] this)
        (#_"Ops" Ops'''opsWithBigInt [#_"RatioOps" this, #_"BigIntOps" x] this)

        (#_"boolean" Ops'''eq [#_"RatioOps" this, #_"Number" x, #_"Number" y]
            (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
                (and (§ interop #_"equals" (:n rx) (:n ry)) (§ interop #_"equals" (:d rx) (:d ry)))
            )
        )

        (#_"boolean" Ops'''lt [#_"RatioOps" this, #_"Number" x, #_"Number" y]
            (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
                (Numbers'lt (.multiply (:n rx), (:d ry)), (.multiply (:n ry), (:d rx)))
            )
        )

        (#_"boolean" Ops'''lte [#_"RatioOps" this, #_"Number" x, #_"Number" y]
            (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
                (Numbers'lte (.multiply (:n rx), (:d ry)), (.multiply (:n ry), (:d rx)))
            )
        )

        (#_"boolean" Ops'''isZero [#_"RatioOps" this, #_"Number" x] (§ interop #_"==" (.signum (:n (cast Ratio x))) 0))
        (#_"boolean" Ops'''isPos [#_"RatioOps" this, #_"Number" x] (§ interop #_">" (.signum (:n (cast Ratio x))) 0))
        (#_"boolean" Ops'''isNeg [#_"RatioOps" this, #_"Number" x] (§ interop #_"<" (.signum (:n (cast Ratio x))) 0))

        (#_"Number" Ops'''add [#_"RatioOps" this, #_"Number" x, #_"Number" y]
            (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
                (Ops'''divide this, (.add (.multiply (:n ry), (:d rx)), (.multiply (:n rx), (:d ry))), (.multiply (:d ry), (:d rx)))
            )
        )

        (#_"Number" Ops'''negate [#_"RatioOps" this, #_"Number" x]
            (let [#_"Ratio" r (Numbers'toRatio x)]
                (Ratio'new (.negate (:n r)), (:d r))
            )
        )

        (#_"Number" Ops'''inc [#_"RatioOps" this, #_"Number" x] (Ops'''add this, x, 1))
        (#_"Number" Ops'''dec [#_"RatioOps" this, #_"Number" x] (Ops'''add this, x, -1))

        (#_"Number" Ops'''multiply [#_"RatioOps" this, #_"Number" x, #_"Number" y]
            (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
                (Numbers'divide (.multiply (:n ry), (:n rx)), (.multiply (:d ry), (:d rx)))
            )
        )

        (#_"Number" Ops'''divide [#_"RatioOps" this, #_"Number" x, #_"Number" y]
            (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
                (Numbers'divide (.multiply (:d ry), (:n rx)), (.multiply (:n ry), (:d rx)))
            )
        )

        (#_"Number" Ops'''quotient [#_"RatioOps" this, #_"Number" x, #_"Number" y]
            (let [#_"Ratio" rx (Numbers'toRatio x) #_"Ratio" ry (Numbers'toRatio y)]
               (.divide (.multiply (:n rx), (:d ry)), (.multiply (:d rx), (:n ry)))
            )
        )

        (#_"Number" Ops'''remainder [#_"RatioOps" this, #_"Number" x, #_"Number" y]
            (Numbers'subtract x, (Numbers'multiply (Ops'''quotient this, x, y), y))
        )
    )
)

(class-ns BigIntOps
    (defn #_"BigIntOps" BigIntOps'new []
        (BigIntOps.)
    )

    (declare Numbers'toBigInteger)

    (extend-type BigIntOps Ops
        (#_"Ops" Ops'''combine [#_"BigIntOps" this, #_"Ops" y] (Ops'''opsWithBigInt y, this))

        (#_"Ops" Ops'''opsWithLong [#_"BigIntOps" this, #_"LongOps" x] this)
        (#_"Ops" Ops'''opsWithRatio [#_"BigIntOps" this, #_"RatioOps" x] Numbers'RATIO_OPS)
        (#_"Ops" Ops'''opsWithBigInt [#_"BigIntOps" this, #_"BigIntOps" x] this)

        (#_"boolean" Ops'''eq [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
            (§ interop #_"equals" (Numbers'toBigInteger x) (Numbers'toBigInteger y))
        )

        (#_"boolean" Ops'''lt [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
            (§ interop #_"<" (.compareTo (Numbers'toBigInteger x), (Numbers'toBigInteger y)) 0)
        )

        (#_"boolean" Ops'''lte [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
            (§ interop #_"<=" (.compareTo (Numbers'toBigInteger x), (Numbers'toBigInteger y)) 0)
        )

        (#_"boolean" Ops'''isZero [#_"BigIntOps" this, #_"Number" x] (§ interop #_"==" (.signum (Numbers'toBigInteger x)) 0))
        (#_"boolean" Ops'''isPos [#_"BigIntOps" this, #_"Number" x] (§ interop #_">" (.signum (Numbers'toBigInteger x)) 0))
        (#_"boolean" Ops'''isNeg [#_"BigIntOps" this, #_"Number" x] (§ interop #_"<" (.signum (Numbers'toBigInteger x)) 0))

        (#_"Number" Ops'''add [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
            (.add (Numbers'toBigInteger x), (Numbers'toBigInteger y))
        )

        (#_"Number" Ops'''negate [#_"BigIntOps" this, #_"Number" x] (.negate (Numbers'toBigInteger x)))

        (#_"Number" Ops'''inc [#_"BigIntOps" this, #_"Number" x] (.add (Numbers'toBigInteger x), BigInteger/ONE))
        (#_"Number" Ops'''dec [#_"BigIntOps" this, #_"Number" x] (.subtract (Numbers'toBigInteger x), BigInteger/ONE))

        (#_"Number" Ops'''multiply [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
            (.multiply (Numbers'toBigInteger x), (Numbers'toBigInteger y))
        )

        (#_"Number" Ops'''divide [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
            (let [#_"BigInteger" n (Numbers'toBigInteger x) #_"BigInteger" d (Numbers'toBigInteger y)]
                (when-not (§ interop #_"equals" d BigInteger/ZERO) => (throw (ArithmeticException. "Divide by zero"))
                    (let [#_"BigInteger" gcd (.gcd n, d)]
                        (when-not (§ interop #_"equals" gcd BigInteger/ZERO) => BigInteger/ZERO
                            (let [n (.divide n, gcd) d (.divide d, gcd)]
                                (§ interop condp #_"equals" d
                                    BigInteger/ONE           n
                                    (.negate BigInteger/ONE) (.negate n)
                                                             (Ratio'new (if (§ interop #_"<" (.signum d) 0) (.negate n) n), (if (§ interop #_"<" (.signum d) 0) (.negate d) d))
                                )
                            )
                        )
                    )
                )
            )
        )

        (#_"Number" Ops'''quotient [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
            (.divide (Numbers'toBigInteger x), (Numbers'toBigInteger y))
        )

        (#_"Number" Ops'''remainder [#_"BigIntOps" this, #_"Number" x, #_"Number" y]
            (.remainder (Numbers'toBigInteger x), (Numbers'toBigInteger y))
        )
    )
)

(class-ns Numbers
    (def #_"LongOps"       Numbers'LONG_OPS       (LongOps'new)      )
    (def #_"RatioOps"      Numbers'RATIO_OPS      (RatioOps'new)     )
    (def #_"BigIntOps"     Numbers'BIGINT_OPS     (BigIntOps'new)    )

    (defn #_"Ops" Numbers'ops [#_"Object" x]
        (condp = (class x)
            BigInteger Numbers'BIGINT_OPS
            Ratio      Numbers'RATIO_OPS
                       Numbers'LONG_OPS
        )
    )

    (defn #_"int" Numbers'compare [#_"Number" x, #_"Number" y]
        (let [#_"Ops" ops (Ops'''combine (Numbers'ops x), (Numbers'ops y))]
            (cond (Ops'''lt ops, x, y) -1 (Ops'''lt ops, y, x) 1 :else 0)
        )
    )

    (defn #_"boolean" Numbers'equal [#_"Number" x, #_"Number" y]
        (-> (Ops'''combine (Numbers'ops x), (Numbers'ops y)) (Ops'''eq x, y))
    )

    (defn #_"boolean" Numbers'lt [#_"Object" x, #_"Object" y]
        (-> (Ops'''combine (Numbers'ops x), (Numbers'ops y)) (Ops'''lt (cast Number x), (cast Number y)))
    )

    (defn #_"boolean" Numbers'lte [#_"Object" x, #_"Object" y]
        (-> (Ops'''combine (Numbers'ops x), (Numbers'ops y)) (Ops'''lte (cast Number x), (cast Number y)))
    )

    (defn #_"boolean" Numbers'gt [#_"Object" x, #_"Object" y]
        (-> (Ops'''combine (Numbers'ops x), (Numbers'ops y)) (Ops'''lt (cast Number y), (cast Number x)))
    )

    (defn #_"boolean" Numbers'gte [#_"Object" x, #_"Object" y]
        (-> (Ops'''combine (Numbers'ops x), (Numbers'ops y)) (Ops'''lte (cast Number y), (cast Number x)))
    )

    (defn #_"boolean" Numbers'isZero [#_"Object" x] (Ops'''isZero (Numbers'ops x), (cast Number x)))
    (defn #_"boolean" Numbers'isPos  [#_"Object" x] (Ops'''isPos  (Numbers'ops x), (cast Number x)))
    (defn #_"boolean" Numbers'isNeg  [#_"Object" x] (Ops'''isNeg  (Numbers'ops x), (cast Number x)))

    (defn #_"Number" Numbers'add [#_"Object" x, #_"Object" y]
        (-> (Ops'''combine (Numbers'ops x), (Numbers'ops y)) (Ops'''add (cast Number x), (cast Number y)))
    )

    (defn #_"Number" Numbers'subtract [#_"Object" x, #_"Object" y]
        (let [#_"Number" negativeY (Ops'''negate (Numbers'ops y), (cast Number y))]
            (-> (Ops'''combine (Numbers'ops x), (Numbers'ops negativeY)) (Ops'''add (cast Number x), negativeY))
        )
    )

    (defn #_"Number" Numbers'negate [#_"Object" x] (Ops'''negate (Numbers'ops x), (cast Number x)))
    (defn #_"Number" Numbers'inc    [#_"Object" x] (Ops'''inc    (Numbers'ops x), (cast Number x)))
    (defn #_"Number" Numbers'dec    [#_"Object" x] (Ops'''dec    (Numbers'ops x), (cast Number x)))

    (defn #_"Number" Numbers'multiply [#_"Object" x, #_"Object" y]
        (-> (Ops'''combine (Numbers'ops x), (Numbers'ops y)) (Ops'''multiply (cast Number x), (cast Number y)))
    )

    (defn #_"Number" Numbers'divide [#_"Object" x, #_"Object" y]
        (let-when-not [#_"Ops" yops (Numbers'ops y)] (Ops'''isZero yops, (cast Number y)) => (throw (ArithmeticException. "Divide by zero"))
            (-> (Ops'''combine (Numbers'ops x), yops) (Ops'''divide (cast Number x), (cast Number y)))
        )
    )

    (defn #_"Number" Numbers'quotient [#_"Object" x, #_"Object" y]
        (let-when-not [#_"Ops" yops (Numbers'ops y)] (Ops'''isZero yops, (cast Number y)) => (throw (ArithmeticException. "Divide by zero"))
            (-> (Ops'''combine (Numbers'ops x), yops) (Ops'''quotient (cast Number x), (cast Number y)))
        )
    )

    (defn #_"Number" Numbers'remainder [#_"Object" x, #_"Object" y]
        (let-when-not [#_"Ops" yops (Numbers'ops y)] (Ops'''isZero yops, (cast Number y)) => (throw (ArithmeticException. "Divide by zero"))
            (-> (Ops'''combine (Numbers'ops x), yops) (Ops'''remainder (cast Number x), (cast Number y)))
        )
    )

    (defn #_"BigInteger" Numbers'toBigInteger [#_"Object" x]
        (condp instance? x
            BigInteger x
                       (BigInteger/valueOf (.longValue (cast Number x)))
        )
    )

    (defn #_"Ratio" Numbers'toRatio [#_"Object" x]
        (condp instance? x
            Ratio x
                  (Ratio'new (Numbers'toBigInteger x), BigInteger/ONE)
        )
    )

    (declare long)

    (defn- #_"long" Numbers'bitOpsCast [#_"Object" x]
        (let [#_"Class" c (class x)]
            (when (any = c Long Integer Byte) => (throw! (str "bit operation not supported for: " c))
                (long x)
            )
        )
    )

    (defn #_"long" Numbers'not [#_"Object" x] (§ interop #_"~" (Numbers'bitOpsCast x)))

    (defn #_"long" Numbers'and [#_"Object" x, #_"Object" y] (§ interop #_"&" (Numbers'bitOpsCast x) (Numbers'bitOpsCast y)))
    (defn #_"long" Numbers'or  [#_"Object" x, #_"Object" y] (§ interop #_"|" (Numbers'bitOpsCast x) (Numbers'bitOpsCast y)))
    (defn #_"long" Numbers'xor [#_"Object" x, #_"Object" y] (§ interop #_"^" (Numbers'bitOpsCast x) (Numbers'bitOpsCast y)))

    (defn #_"long" Numbers'andNot [#_"Object" x, #_"Object" y] (§ interop #_"&" (Numbers'bitOpsCast x) (§ interop #_"~" (Numbers'bitOpsCast y))))

    (defn #_"long" Numbers'shiftLeft          [#_"Object" x, #_"Object" n] (§ interop #_"<<"  (Numbers'bitOpsCast x) (Numbers'bitOpsCast n)))
    (defn #_"long" Numbers'shiftRight         [#_"Object" x, #_"Object" n] (§ interop #_">>"  (Numbers'bitOpsCast x) (Numbers'bitOpsCast n)))
    (defn #_"long" Numbers'unsignedShiftRight [#_"Object" x, #_"Object" n] (§ interop #_">>>" (Numbers'bitOpsCast x) (Numbers'bitOpsCast n)))

    (defn #_"long" Numbers'clearBit [#_"Object" x, #_"Object" n] (§ interop #_"&" (Numbers'bitOpsCast x) (§ interop #_"~" (§ interop #_"<<" 1 (Numbers'bitOpsCast n)))))
    (defn #_"long" Numbers'setBit   [#_"Object" x, #_"Object" n] (§ interop #_"|" (Numbers'bitOpsCast x)                  (§ interop #_"<<" 1 (Numbers'bitOpsCast n))))
    (defn #_"long" Numbers'flipBit  [#_"Object" x, #_"Object" n] (§ interop #_"^" (Numbers'bitOpsCast x)                  (§ interop #_"<<" 1 (Numbers'bitOpsCast n))))

    (defn #_"boolean" Numbers'testBit [#_"Object" x, #_"Object" n] (§ interop #_"!=" (§ interop #_"&" (Numbers'bitOpsCast x) (§ interop #_"<<" 1 (Numbers'bitOpsCast n))) 0))

    (defn #_"int" Numbers'hasheq [#_"Number" x]
        (let [#_"Class" c (class x)]
            (if (or (any = c Long Integer Byte) (and (= c BigInteger) (§ interop #_"<=" Long/MIN_VALUE x Long/MAX_VALUE)))
                (Murmur3'hashLong (.longValue x))
                (IObject'''hashCode x)
            )
        )
    )
)
)

;;;
 ; Returns non-nil if nums are in monotonically increasing order, otherwise false.
 ;;
(defn <
    ([x] true)
    ([x y] (Numbers'lt x y))
    ([x y & s] (and (< x y) (recur-if (next s) [y (first s) (next s)] => (< y (first s)))))
)

;;;
 ; Returns non-nil if nums are in monotonically non-decreasing order, otherwise false.
 ;;
(defn <=
    ([x] true)
    ([x y] (Numbers'lte x y))
    ([x y & s] (and (<= x y) (recur-if (next s) [y (first s) (next s)] => (<= y (first s)))))
)

;;;
 ; Returns non-nil if nums are in monotonically decreasing order, otherwise false.
 ;;
(defn >
    ([x] true)
    ([x y] (Numbers'gt x y))
    ([x y & s] (and (> x y) (recur-if (next s) [y (first s) (next s)] => (> y (first s)))))
)

;;;
 ; Returns non-nil if nums are in monotonically non-increasing order, otherwise false.
 ;;
(defn >=
    ([x] true)
    ([x y] (Numbers'gte x y))
    ([x y & s] (and (>= x y) (recur-if (next s) [y (first s) (next s)] => (>= y (first s)))))
)

;;;
 ; Returns the greatest of the nums.
 ;;
(defn max
    ([x] x)
    ([x y] (if (> x y) x y))
    ([x y & s] (reduce max (max x y) s))
)

;;;
 ; Returns the least of the nums.
 ;;
(defn min
    ([x] x)
    ([x y] (if (< x y) x y))
    ([x y & s] (reduce min (min x y) s))
)

;;;
 ; Returns true if n is zero | greater than zero | less than zero, else false.
 ;;
(defn zero? [n] (Numbers'isZero n))
(§ defn pos?  [n] (Numbers'isPos  n))
(defn neg?  [n] (Numbers'isNeg  n))

;;;
 ; Returns the sum of nums. (+) returns 0. Supports arbitrary precision.
 ;;
(§ defn +
    ([] 0)
    ([x] (cast Number x))
    ([x y] (Numbers'add x y))
    ([x y & s] (reduce + (+ x y) s))
)

;;;
 ; If no ys are supplied, returns the negation of x, else subtracts
 ; the ys from x and returns the result. Supports arbitrary precision.
 ;;
(§ defn -
    ([x] (Numbers'negate x))
    ([x y] (Numbers'subtract x y))
    ([x y & s] (reduce - (- x y) s))
)

;;;
 ; Returns a number one greater than num. Supports arbitrary precision.
 ;;
(§ defn inc [x] (Numbers'inc x))

;;;
 ; Returns a number one less than num. Supports arbitrary precision.
 ;;
(defn dec [x] (Numbers'dec x))

;;;
 ; Returns the product of nums. (*) returns 1. Supports arbitrary precision.
 ;;
(defn *
    ([] 1)
    ([x] (cast Number x))
    ([x y] (Numbers'multiply x y))
    ([x y & s] (reduce * (* x y) s))
)

;;;
 ; If no denominators are supplied, returns 1/numerator,
 ; else returns numerator divided by all of the denominators.
 ;;
(defn /
    ([x] (/ 1 x))
    ([x y] (Numbers'divide x y))
    ([x y & s] (reduce / (/ x y) s))
)

;;;
 ; quot[ient] of dividing numerator by denominator.
 ;;
(defn quot [num div] (Numbers'quotient num div))

;;;
 ; rem[ainder] of dividing numerator by denominator.
 ;;
(defn rem [num div] (Numbers'remainder num div))

;;;
 ; Modulus of num and div. Truncates toward negative infinity.
 ;;
(defn mod [num div]
    (let-when [m (rem num div)] (or (zero? m) (= (pos? num) (pos? div))) => (+ m div)
        m
    )
)

;;;
 ; Bitwise complement.
 ;;
(defn bit-not [x] (Numbers'not x))

;;;
 ; Bitwise and.
 ;;
(defn bit-and
    ([x y] (Numbers'and x y))
    ([x y & s] (reduce bit-and (bit-and x y) s))
)

;;;
 ; Bitwise or.
 ;;
(defn bit-or
    ([x y] (Numbers'or x y))
    ([x y & s] (reduce bit-or (bit-or x y) s))
)

;;;
 ; Bitwise exclusive or.
 ;;
(defn bit-xor
    ([x y] (Numbers'xor x y))
    ([x y & s] (reduce bit-xor (bit-xor x y) s))
)

;;;
 ; Bitwise and with complement.
 ;;
(defn bit-and-not
    ([x y] (Numbers'andNot x y))
    ([x y & s] (reduce bit-and-not (bit-and-not x y) s))
)

;;;
 ; Clear | set | flip | test bit at index i.
 ;;
(defn bit-clear [x i] (Numbers'clearBit x i))
(defn bit-set   [x i] (Numbers'setBit   x i))
(defn bit-flip  [x i] (Numbers'flipBit  x i))
(defn bit-test  [x i] (Numbers'testBit  x i))

;;;
 ; Bitwise shift left | right | right, without sign-extension.
 ;;
(defn          bit-shift-left  [x n] (Numbers'shiftLeft          x n))
(defn          bit-shift-right [x n] (Numbers'shiftRight         x n))
(defn unsigned-bit-shift-right [x n] (Numbers'unsignedShiftRight x n))

;;;
 ; Returns true if n is even, throws an exception if n is not an integer.
 ;;
(defn even? [n]
    (when (integer? n) => (throw! (str "argument must be an integer: " n))
        (zero? (bit-and n 1))
    )
)

;;;
 ; Returns true if n is odd, throws an exception if n is not an integer.
 ;;
(defn odd? [n] (not (even? n)))

(java-ns cloiure.lang.AFn

(class-ns AFn
    (defn #_"AFn" AFn'new []
        (AFn.)
    )

    (extend-type AFn IFn
        (#_"Object" IFn'''invoke
            ([#_"AFn" this] (AFn'''throwArity this, 0))
            ([#_"AFn" this, #_"Object" arg1] (AFn'''throwArity this, 1))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2] (AFn'''throwArity this, 2))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3] (AFn'''throwArity this, 3))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4] (AFn'''throwArity this, 4))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5] (AFn'''throwArity this, 5))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6] (AFn'''throwArity this, 6))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7] (AFn'''throwArity this, 7))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8] (AFn'''throwArity this, 8))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9] (AFn'''throwArity this, 9))
            ([#_"AFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9 & #_"Object..." args] (AFn'''throwArity this, 10))
        )
    )

    (declare AFn'applyToHelper)

    (extend-type AFn IFn
        (#_"Object" IFn'''applyTo [#_"AFn" this, #_"ISeq" args]
            (AFn'applyToHelper this, args)
        )
    )

    (declare RT'boundedLength)

    (defn #_"Object" AFn'applyToHelper [#_"IFn" ifn, #_"ISeq" args]
        (case (RT'boundedLength args, 9)
            0
                (IFn'''invoke ifn)
            1
                (IFn'''invoke ifn, (first args))
            2
                (IFn'''invoke ifn, (first args),
                    (first (next args))
                )
            3
                (IFn'''invoke ifn, (first args),
                    (first (§ ass args (next args))),
                    (first (next args))
                )
            4
                (IFn'''invoke ifn, (first args),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (next args))
                )
            5
                (IFn'''invoke ifn, (first args),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (next args))
                )
            6
                (IFn'''invoke ifn, (first args),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (next args))
                )
            7
                (IFn'''invoke ifn, (first args),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (next args))
                )
            8
                (IFn'''invoke ifn, (first args),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (next args))
                )
            9
                (IFn'''invoke ifn, (first args),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (next args))
                )
            #_else
                (§ soon IFn'''invoke ifn, (first args),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (first (§ ass args (next args))),
                    (RT'seqToArray (next args))
                )
        )
    )

    (declare Compiler'demunge)

    #_override
    (defn #_"Object" AFn'''throwArity--AFn [#_"AFn" this, #_"int" n]
        (throw! (str "wrong number of args (" n ") passed to: " (Compiler'demunge (.getName (class this)))))
    )
)
)

(java-ns cloiure.lang.Symbol

(class-ns Symbol
    (defn- #_"Symbol" Symbol'new
        ([#_"String" ns, #_"String" name] (Symbol'new nil, ns, name))
        ([#_"IPersistentMap" meta, #_"String" ns, #_"String" name]
            (merge (Symbol.) (AFn'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"String" :ns ns
                    #_"String" :name name
                )
            )
        )
    )

    (extend-type Symbol IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"Symbol" this]
            (:_meta this)
        )
    )

    (extend-type Symbol IObj
        (#_"Symbol" IObj'''withMeta [#_"Symbol" this, #_"IPersistentMap" meta]
            (Symbol'new meta, (:ns this), (:name this))
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

    (extend-type Symbol IObject
        (#_"String" IObject'''toString [#_"Symbol" this]
            (if (some? (:ns this)) (str (:ns this) "/" (:name this)) (:name this))
        )
    )

    (extend-type Symbol INamed
        (#_"String" INamed'''getNamespace [#_"Symbol" this]
            (:ns this)
        )

        (#_"String" INamed'''getName [#_"Symbol" this]
            (:name this)
        )
    )

    (extend-type Symbol IObject
        (#_"boolean" IObject'''equals [#_"Symbol" this, #_"Object" that]
            (or (identical? this that)
                (and (symbol? that) (= (:ns this) (:ns that)) (= (:name this) (:name that)))
            )
        )

        (#_"int" IObject'''hashCode [#_"Symbol" this]
            (Util'hashCombine (IObject'''hashCode (:name this)), (IObject'''hashCode (:ns this)))
        )
    )

    (extend-type Symbol IHashEq
        (#_"int" IHashEq'''hasheq [#_"Symbol" this]
            (Util'hashCombine (Murmur3'hashUnencodedChars (:name this)), (IObject'''hashCode (:ns this)))
        )
    )

    #_foreign
    (defn #_"int" compareTo---Symbol [#_"Symbol" this, #_"Symbol" that]
        (cond
            (= this that)                              0
            (and (nil? (:ns this)) (some? (:ns that))) -1
            (nil? (:ns this))                          (compare (:name this) (:name that))
            (nil? (:ns that))                          1
            :else
                (let-when [#_"int" cmp (compare (:ns this) (:ns that))] (zero? cmp) => cmp
                    (compare (:name this) (:name that))
                )
        )
    )

    (extend-type Symbol IFn
        (#_"Object" IFn'''invoke
            ([#_"Symbol" this, #_"Object" arg1] (get arg1 this))
            ([#_"Symbol" this, #_"Object" arg1, #_"Object" notFound] (get arg1 this notFound))
        )
    )
)
)

(java-ns cloiure.lang.Keyword

(class-ns Keyword
    (def- #_"{Symbol Reference<Keyword>}'" Keyword'TABLE (atom {}))

    (def #_"ReferenceQueue" Keyword'RQ (ReferenceQueue.))

    (defn- #_"Keyword" Keyword'new [#_"Symbol" sym]
        (merge (Keyword.)
            (hash-map
                #_"Symbol" :sym sym
                #_"int" :hasheq (+ (IHashEq'''hasheq sym) 0x9e3779b9)
            )
        )
    )

    (defn #_"Keyword" Keyword'intern [#_"Symbol" sym]
        (let [#_"Reference<Keyword>" r (get @Keyword'TABLE sym)
              [sym r #_"Keyword" k]
                (when (nil? r) => [sym r nil]
                    (Util'clearCache Keyword'RQ, Keyword'TABLE)
                    (let [sym
                            (when (some? (meta sym)) => sym
                                (with-meta sym nil)
                            )
                          k (Keyword'new sym) r (WeakReference. #_"<Keyword>" k, Keyword'RQ)
                          _ (swap! Keyword'TABLE assoc sym r)]
                        [sym r k]
                    )
                )]
            (when (some? r) => k
                (or (.get r)
                    (do ;; entry died in the interim, do over
                        (swap! Keyword'TABLE #(if (identical? (get % sym) r) (dissoc % sym) %))
                        (recur #_"Keyword'intern" sym)
                    )
                )
            )
        )
    )

    (defn #_"Keyword" Keyword'find [#_"Symbol" sym]
        (when-some [#_"Reference<Keyword>" ref (get @Keyword'TABLE sym)]
            (.get ref)
        )
    )

    (extend-type Keyword IHashEq
        (#_"int" IHashEq'''hasheq [#_"Keyword" this]
            (:hasheq this)
        )
    )

    (extend-type Keyword IObject
        (#_"int" IObject'''hashCode [#_"Keyword" this]
            (+ (IObject'''hashCode (:sym this)) 0x9e3779b9)
        )

        (#_"String" IObject'''toString [#_"Keyword" this]
            (str ":" (:sym this))
        )
    )

    #_foreign
    (defn #_"int" compareTo---Keyword [#_"Keyword" this, #_"Keyword" that]
        (compare (:sym this) (:sym that))
    )

    (extend-type Keyword INamed
        (#_"String" INamed'''getNamespace [#_"Keyword" this]
            (INamed'''getNamespace (:sym this))
        )

        (#_"String" INamed'''getName [#_"Keyword" this]
            (INamed'''getName (:sym this))
        )
    )

    #_method
    (defn- #_"Object" Keyword''throwArity [#_"Keyword" this]
        (throw! (str "wrong number of args passed to keyword: " this))
    )

    (extend-type Keyword IFn
        (#_"Object" IFn'''invoke
            ([#_"Keyword" this] (Keyword''throwArity this))
            ([#_"Keyword" this, #_"Object" obj] (get obj this))
            ([#_"Keyword" this, #_"Object" obj, #_"Object" notFound] (get obj this notFound))
            ([#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3] (Keyword''throwArity this))
            ([#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4] (Keyword''throwArity this))
            ([#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5] (Keyword''throwArity this))
            ([#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6] (Keyword''throwArity this))
            ([#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7] (Keyword''throwArity this))
            ([#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8] (Keyword''throwArity this))
            ([#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9] (Keyword''throwArity this))
            ([#_"Keyword" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9 & #_"Object..." args] (Keyword''throwArity this))
        )

        (#_"Object" IFn'''applyTo [#_"Keyword" this, #_"ISeq" args]
            (AFn'applyToHelper this, args)
        )
    )
)
)

(java-ns cloiure.lang.AFunction

(class-ns AFunction
    (defn #_"AFunction" AFunction'new []
        (merge (AFunction.) (AFn'new)
            (hash-map
                #_"MethodImplCache'" :__methodImplCache (volatile! nil)
            )
        )
    )

    (extend-type AFunction IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"AFunction" this]
            nil
        )
    )

    (extend-type AFunction IObj
        (#_"AFunction" IObj'''withMeta [#_"AFunction" this, #_"IPersistentMap" meta]
            (throw! "unsupported operation")
        )
    )

    #_foreign
    (defn #_"int" compare---AFunction [#_"AFunction" this, #_"Object" o1, #_"Object" o2]
        (let [#_"Object" o (IFn'''invoke this, o1, o2)]
            (if (instance? Boolean o)
                (cond (boolean o) -1 (boolean (IFn'''invoke this, o2, o1)) 1 :else 0)
                (.intValue (cast Number o))
            )
        )
    )
)
)

(java-ns cloiure.lang.RestFn

(class-ns RestFn
    (defn #_"RestFn" RestFn'new []
        (merge (RestFn.) (AFunction'new))
    )

    (defn #_"ISeq" RestFn'findKey [#_"Object" key, #_"ISeq" args]
        (loop-when args (some? args)
            (if (= key (first args)) (next args) (recur (next (next args))))
        )
    )

    (declare ArraySeq'create)

    (defn #_"ISeq" RestFn'ontoArrayPrepend [#_"Object[]" a & #_"Object..." args]
        (loop-when-recur [#_"ISeq" s (ArraySeq'create a) #_"int" i (dec (alength args))] (<= 0 i) [(cons (aget args i) s) (dec i)] => s)
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

    (extend-type RestFn IFn
        (#_"Object" IFn'''applyTo [#_"RestFn" this, #_"ISeq" args]
            (let-when [#_"int" n (RestFn'''getRequiredArity this)] (< n (RT'boundedLength args, n)) => (AFn'applyToHelper this, args)
                (case n
                    0
                        (RestFn'''doInvoke this, args)
                    1
                        (RestFn'''doInvoke this, (first args),
                            (next args)
                        )
                    2
                        (RestFn'''doInvoke this, (first args),
                            (first (§ ass args (next args))),
                            (next args)
                        )
                    3
                        (RestFn'''doInvoke this, (first args),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (next args)
                        )
                    4
                        (RestFn'''doInvoke this, (first args),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (next args)
                        )
                    5
                        (RestFn'''doInvoke this, (first args),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (next args)
                        )
                    6
                        (RestFn'''doInvoke this, (first args),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (next args)
                        )
                    7
                        (RestFn'''doInvoke this, (first args),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (next args)
                        )
                    8
                        (RestFn'''doInvoke this, (first args),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (next args)
                        )
                    9
                        (RestFn'''doInvoke this, (first args),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (first (§ ass args (next args))),
                            (next args)
                        )
                    (AFn'''throwArity this, -1)
                )
            )
        )

        (#_"Object" IFn'''invoke
            ([#_"RestFn" this]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this, nil)
                    (do
                        (AFn'''throwArity this, 0)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this, (ArraySeq'create arg1))
                    1
                        (RestFn'''doInvoke this, arg1, nil)
                    (do
                        (AFn'''throwArity this, 1)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (ArraySeq'create arg1, arg2))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (ArraySeq'create arg2))
                    2
                        (RestFn'''doInvoke this, arg1, arg2, nil)
                    (do
                        (AFn'''throwArity this, 2)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (ArraySeq'create arg1, arg2, arg3))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (ArraySeq'create arg2, arg3))
                    2
                        (RestFn'''doInvoke this, arg1, arg2,
                            (ArraySeq'create arg3))
                    3
                        (RestFn'''doInvoke this, arg1, arg2, arg3, nil)
                    (do
                        (AFn'''throwArity this, 3)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (ArraySeq'create arg1, arg2, arg3, arg4))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (ArraySeq'create arg2, arg3, arg4))
                    2
                        (RestFn'''doInvoke this, arg1, arg2,
                            (ArraySeq'create arg3, arg4))
                    3
                        (RestFn'''doInvoke this, arg1, arg2, arg3,
                            (ArraySeq'create arg4))
                    4
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, nil)
                    (do
                        (AFn'''throwArity this, 4)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (ArraySeq'create arg1, arg2, arg3, arg4, arg5))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (ArraySeq'create arg2, arg3, arg4, arg5))
                    2
                        (RestFn'''doInvoke this, arg1, arg2,
                            (ArraySeq'create arg3, arg4, arg5))
                    3
                        (RestFn'''doInvoke this, arg1, arg2, arg3,
                            (ArraySeq'create arg4, arg5))
                    4
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4,
                            (ArraySeq'create arg5))
                    5
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, nil)
                    (do
                        (AFn'''throwArity this, 5)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (ArraySeq'create arg1, arg2, arg3, arg4, arg5, arg6))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (ArraySeq'create arg2, arg3, arg4, arg5, arg6))
                    2
                        (RestFn'''doInvoke this, arg1, arg2,
                            (ArraySeq'create arg3, arg4, arg5, arg6))
                    3
                        (RestFn'''doInvoke this, arg1, arg2, arg3,
                            (ArraySeq'create arg4, arg5, arg6))
                    4
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4,
                            (ArraySeq'create arg5, arg6))
                    5
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5,
                            (ArraySeq'create arg6))
                    6
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, nil)
                    (do
                        (AFn'''throwArity this, 6)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (ArraySeq'create arg1, arg2, arg3, arg4, arg5, arg6, arg7))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (ArraySeq'create arg2, arg3, arg4, arg5, arg6, arg7))
                    2
                        (RestFn'''doInvoke this, arg1, arg2,
                            (ArraySeq'create arg3, arg4, arg5, arg6, arg7))
                    3
                        (RestFn'''doInvoke this, arg1, arg2, arg3,
                            (ArraySeq'create arg4, arg5, arg6, arg7))
                    4
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4,
                            (ArraySeq'create arg5, arg6, arg7))
                    5
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5,
                            (ArraySeq'create arg6, arg7))
                    6
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                            (ArraySeq'create arg7))
                    7
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, nil)
                    (do
                        (AFn'''throwArity this, 7)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (ArraySeq'create arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (ArraySeq'create arg2, arg3, arg4, arg5, arg6, arg7, arg8))
                    2
                        (RestFn'''doInvoke this, arg1, arg2,
                            (ArraySeq'create arg3, arg4, arg5, arg6, arg7, arg8))
                    3
                        (RestFn'''doInvoke this, arg1, arg2, arg3,
                            (ArraySeq'create arg4, arg5, arg6, arg7, arg8))
                    4
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4,
                            (ArraySeq'create arg5, arg6, arg7, arg8))
                    5
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5,
                            (ArraySeq'create arg6, arg7, arg8))
                    6
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                            (ArraySeq'create arg7, arg8))
                    7
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                            (ArraySeq'create arg8))
                    8
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, nil)
                    (do
                        (AFn'''throwArity this, 8)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (ArraySeq'create arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (ArraySeq'create arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9))
                    2
                        (RestFn'''doInvoke this, arg1, arg2,
                            (ArraySeq'create arg3, arg4, arg5, arg6, arg7, arg8, arg9))
                    3
                        (RestFn'''doInvoke this, arg1, arg2, arg3,
                            (ArraySeq'create arg4, arg5, arg6, arg7, arg8, arg9))
                    4
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4,
                            (ArraySeq'create arg5, arg6, arg7, arg8, arg9))
                    5
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5,
                            (ArraySeq'create arg6, arg7, arg8, arg9))
                    6
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                            (ArraySeq'create arg7, arg8, arg9))
                    7
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                            (ArraySeq'create arg8, arg9))
                    8
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                            (ArraySeq'create arg9))
                    9
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, nil)
                    (do
                        (AFn'''throwArity this, 9)
                    )
                )
            )

            ([#_"RestFn" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9 & #_"Object..." args]
                (case (RestFn'''getRequiredArity this)
                    0
                        (RestFn'''doInvoke this,
                            (RestFn'ontoArrayPrepend args, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9))
                    1
                        (RestFn'''doInvoke this, arg1,
                            (RestFn'ontoArrayPrepend args, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9))
                    2
                        (RestFn'''doInvoke this, arg1, arg2,
                            (RestFn'ontoArrayPrepend args, arg3, arg4, arg5, arg6, arg7, arg8, arg9))
                    3
                        (RestFn'''doInvoke this, arg1, arg2, arg3,
                            (RestFn'ontoArrayPrepend args, arg4, arg5, arg6, arg7, arg8, arg9))
                    4
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4,
                            (RestFn'ontoArrayPrepend args, arg5, arg6, arg7, arg8, arg9))
                    5
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5,
                            (RestFn'ontoArrayPrepend args, arg6, arg7, arg8, arg9))
                    6
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6,
                            (RestFn'ontoArrayPrepend args, arg7, arg8, arg9))
                    7
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                            (RestFn'ontoArrayPrepend args, arg8, arg9))
                    8
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
                            (RestFn'ontoArrayPrepend args, arg9))
                    9
                        (RestFn'''doInvoke this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9,
                            (ArraySeq'create args))
                    (do
                        (AFn'''throwArity this, 10)
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.ASeq

(class-ns ASeq
    (defn #_"ASeq" ASeq'new [#_"IPersistentMap" meta]
        (merge (ASeq.)
            (hash-map
                #_"IPersistentMap" :_meta meta
            )
        )
    )

    (extend-type ASeq IPersistentCollection
        (#_"IPersistentCollection" IPersistentCollection'''empty [#_"ASeq" this]
            ()
        )
    )

    (extend-type ASeq IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"ASeq" this]
            (:_meta this)
        )
    )

    (extend-type ASeq Seqable
        (#_"ISeq" Seqable'''seq [#_"ASeq" this]
            this
        )
    )

    (extend-type ASeq IHashEq
        (#_"int" IHashEq'''hasheq [#_"ASeq" this]
            (Murmur3'hashOrdered this)
        )
    )

    (declare RT'printString)

    (extend-type ASeq IObject
        (#_"boolean" IObject'''equals [#_"ASeq" this, #_"Object" that]
            (or (identical? this that)
                (and (sequential? that)
                    (loop-when [#_"ISeq" s (seq this) #_"ISeq" z (seq that)] (some? s) => (nil? z)
                        (and (some? z) (= (first s) (first z)) (recur (next s) (next z)))
                    )
                )
            )
        )

        (#_"int" IObject'''hashCode [#_"ASeq" this]
            (loop-when [#_"int" hash 1 #_"ISeq" s (seq this)] (some? s) => hash
                (recur (+ (* 31 hash) (if (some? (first s)) (IObject'''hashCode (first s)) 0)) (next s))
            )
        )

        (#_"String" IObject'''toString [#_"ASeq" this]
            (RT'printString this)
        )
    )
)
)

(java-ns cloiure.lang.LazySeq

(class-ns LazySeq
    (defn- #_"LazySeq" LazySeq'init [#_"IPersistentMap" meta, #_"IFn" f, #_"ISeq" s]
        (merge (LazySeq.)
            (hash-map
                #_"IPersistentMap" :_meta meta

                #_"IFn'" :f (volatile! f)
                #_"Object'" :o (volatile! nil)
                #_"ISeq'" :s (volatile! s)
            )
        )
    )

    (defn- #_"LazySeq" LazySeq'new
        ([#_"IFn" f]                           (LazySeq'init nil,  f,   nil))
        ([#_"IPersistentMap" meta, #_"ISeq" s] (LazySeq'init meta, nil, s  ))
    )

    (extend-type LazySeq IPersistentCollection
        (#_"IPersistentCollection" IPersistentCollection'''empty [#_"LazySeq" this]
            ()
        )
    )

    (extend-type LazySeq IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"LazySeq" this]
            (:_meta this)
        )
    )

    (extend-type LazySeq IObj
        (#_"LazySeq" IObj'''withMeta [#_"LazySeq" this, #_"IPersistentMap" meta]
            (LazySeq'new meta, (seq this))
        )
    )

    #_method
    (defn- #_"Object" LazySeq''step [#_"LazySeq" this]
        (locking this
            (when-some [#_"IFn" f @(:f this)]
                (vreset! (:f this) nil)
                (vreset! (:o this) (f))
            )
            (or @(:o this) @(:s this))
        )
    )

    (extend-type LazySeq Seqable
        (#_"ISeq" Seqable'''seq [#_"LazySeq" this]
            (locking this
                (LazySeq''step this)
                (when-some [#_"Object" o @(:o this)]
                    (vreset! (:o this) nil)
                    (vreset! (:s this) (loop-when-recur o (instance? LazySeq o) (LazySeq''step o) => (seq o)))
                )
                @(:s this)
            )
        )
    )

    (extend-type LazySeq ISeq
        (#_"Object" ISeq'''first [#_"LazySeq" this]
            (when-some [#_"ISeq" s (seq this)]
                (first s)
            )
        )

        (#_"ISeq" ISeq'''next [#_"LazySeq" this]
            (when-some [#_"ISeq" s (seq this)]
                (next s)
            )
        )
    )

    (extend-type LazySeq IObject
        (#_"boolean" IObject'''equals [#_"LazySeq" this, #_"Object" that]
            (if-some [#_"ISeq" s (seq this)]
                (= s that)
                (and (sequential? that) (nil? (seq that)))
            )
        )

        (#_"int" IObject'''hashCode [#_"LazySeq" this]
            (if-some [#_"ISeq" s (seq this)]
                (IObject'''hashCode s)
                1
            )
        )
    )

    (extend-type LazySeq IHashEq
        (#_"int" IHashEq'''hasheq [#_"LazySeq" this]
            (Murmur3'hashOrdered this)
        )
    )

    (extend-type LazySeq IPending
        (#_"boolean" IPending'''isRealized [#_"LazySeq" this]
            (locking this
                (nil? @(:f this))
            )
        )
    )
)
)

(java-ns cloiure.lang.APersistentMap

(class-ns APersistentMap
    (defn #_"APersistentMap" APersistentMap'new []
        (merge (APersistentMap.) (AFn'new))
    )

    (extend-type APersistentMap IObject
        (#_"String" IObject'''toString [#_"APersistentMap" this]
            (RT'printString this)
        )
    )

    (extend-type APersistentMap IPersistentCollection
        (#_"IPersistentCollection" IPersistentCollection'''conj [#_"APersistentMap" this, #_"Object" o]
            (condp satisfies? o
                IMapEntry
                    (assoc this (key o) (val o))
                IPersistentVector
                    (when (= (count o) 2) => (throw! "vector arg to map conj must be a pair")
                        (assoc this (nth o 0) (nth o 1))
                    )
                #_else
                    (loop-when [this this #_"ISeq" s (seq o)] (some? s) => this
                        (let [#_"IMapEntry" e (first s)]
                            (recur (assoc this (key e) (val e)) (next s))
                        )
                    )
            )
        )
    )

    (extend-type APersistentMap IObject
        (#_"boolean" IObject'''equals [#_"APersistentMap" this, #_"Object" that]
            (or (identical? this that)
                (and (map? that) (= (count that) (count this))
                    (loop-when [#_"ISeq" s (seq this)] (some? s) => true
                        (let [#_"IMapEntry" e (first s) #_"Object" k (key e)]
                            (and (contains? that k) (= (val e) (get that k))
                                (recur (next s))
                            )
                        )
                    )
                )
            )
        )

        (#_"int" IObject'''hashCode [#_"APersistentMap" this]
            (loop-when [#_"int" hash 0 #_"ISeq" s (seq this)] (some? s) => hash
                (let [#_"IMapEntry" e (first s) #_"Object" k (key e) #_"Object" v (val e)]
                    (recur (+ hash (bit-xor (if (some? k) (IObject'''hashCode k) 0) (if (some? v) (IObject'''hashCode v) 0))) (next s))
                )
            )
        )
    )

    (extend-type APersistentMap IHashEq
        (#_"int" IHashEq'''hasheq [#_"APersistentMap" this]
            (Murmur3'hashUnordered this)
        )
    )

    (extend-type APersistentMap IFn
        (#_"Object" IFn'''invoke
            ([#_"APersistentMap" this, #_"Object" key] (get this key))
            ([#_"APersistentMap" this, #_"Object" key, #_"Object" notFound] (get this key notFound))
        )
    )
)
)

(java-ns cloiure.lang.APersistentSet

(class-ns APersistentSet
    (defn #_"APersistentSet" APersistentSet'new [#_"IPersistentMap" impl]
        (merge (APersistentSet.) (AFn'new)
            (hash-map
                #_"IPersistentMap" :impl impl
            )
        )
    )

    (extend-type APersistentSet IObject
        (#_"String" IObject'''toString [#_"APersistentSet" this]
            (RT'printString this)
        )
    )

    (extend-type APersistentSet IPersistentSet
        (#_"boolean" IPersistentSet'''contains [#_"APersistentSet" this, #_"Object" key]
            (contains? (:impl this) key)
        )

        (#_"Object" IPersistentSet'''get [#_"APersistentSet" this, #_"Object" key]
            (get (:impl this) key)
        )
    )

    (extend-type APersistentSet Counted
        (#_"int" Counted'''count [#_"APersistentSet" this]
            (count (:impl this))
        )
    )

    (extend-type APersistentSet Seqable
        (#_"ISeq" Seqable'''seq [#_"APersistentSet" this]
            (keys (:impl this))
        )
    )

    (extend-type APersistentSet IFn
        (#_"Object" IFn'''invoke
            ([#_"APersistentSet" this, #_"Object" key] (get this key))
            ([#_"APersistentSet" this, #_"Object" key, #_"Object" notFound] (get this key notFound))
        )
    )

    (extend-type APersistentSet IObject
        (#_"boolean" IObject'''equals [#_"APersistentSet" this, #_"Object" that]
            (or (identical? this that)
                (and (set? that) (= (count this) (count that))
                    (loop-when [#_"ISeq" s (seq that)] (some? s) => true
                        (and (contains? this (first s)) (recur (next s)))
                    )
                )
            )
        )

        (#_"int" IObject'''hashCode [#_"APersistentSet" this]
            (loop-when [#_"int" hash 0 #_"ISeq" s (seq this)] (some? s) => hash
                (recur (+ hash (IObject'''hashCode (first s))) (next s))
            )
        )
    )

    (extend-type APersistentSet IHashEq
        (#_"int" IHashEq'''hasheq [#_"APersistentSet" this]
            (Murmur3'hashUnordered this)
        )
    )
)
)

(java-ns cloiure.lang.APersistentVector

(class-ns VSeq
    (defn #_"VSeq" VSeq'new
        ([#_"IPersistentVector" v, #_"int" i] (VSeq'new nil, v, i))
        ([#_"IPersistentMap" meta, #_"IPersistentVector" v, #_"int" i]
            (merge (VSeq.) (ASeq'new meta)
                (hash-map
                    #_"IPersistentVector" :v v
                    #_"int" :i i
                )
            )
        )
    )

    (extend-type VSeq IObj
        (#_"VSeq" IObj'''withMeta [#_"VSeq" this, #_"IPersistentMap" meta]
            (VSeq'new meta, (:v this), (:i this))
        )
    )

    (extend-type VSeq ISeq
        (#_"Object" ISeq'''first [#_"VSeq" this]
            (nth (:v this) (:i this))
        )

        (#_"ISeq" ISeq'''next [#_"VSeq" this]
            (when (< (inc (:i this)) (count (:v this)))
                (VSeq'new (:v this), (inc (:i this)))
            )
        )
    )

    (extend-type VSeq Counted
        (#_"int" Counted'''count [#_"VSeq" this]
            (- (count (:v this)) (:i this))
        )
    )

    (declare reduced?)

    (extend-type VSeq IReduce
        (#_"Object" IReduce'''reduce
            ([#_"VSeq" this, #_"IFn" f]
                (let [#_"IPersistentVector" v (:v this) #_"int" i (:i this) #_"int" n (count v)]
                    (loop-when [#_"Object" r (nth v i) i (inc i)] (< i n) => r
                        (let-when [r (f r (nth v i))] (reduced? r) => (recur r (inc i))
                            @r
                        )
                    )
                )
            )
            ([#_"VSeq" this, #_"IFn" f, #_"Object" r]
                (let [#_"IPersistentVector" v (:v this) #_"int" i (:i this) #_"int" n (count v)]
                    (loop-when [r (f r (nth v i)) i (inc i)] (< i n) => (if (reduced? r) @r r)
                        (when (reduced? r) => (recur (f r (nth v i)) (inc i))
                            @r
                        )
                    )
                )
            )
        )
    )
)

(class-ns RSeq
    (defn #_"RSeq" RSeq'new
        ([#_"IPersistentVector" v, #_"int" i] (RSeq'new nil, v, i))
        ([#_"IPersistentMap" meta, #_"IPersistentVector" v, #_"int" i]
            (merge (RSeq.) (ASeq'new meta)
                (hash-map
                    #_"IPersistentVector" :v v
                    #_"int" :i i
                )
            )
        )
    )

    (extend-type RSeq IObj
        (#_"RSeq" IObj'''withMeta [#_"RSeq" this, #_"IPersistentMap" meta]
            (RSeq'new meta, (:v this), (:i this))
        )
    )

    (extend-type RSeq ISeq
        (#_"Object" ISeq'''first [#_"RSeq" this]
            (nth (:v this) (:i this))
        )

        (#_"ISeq" ISeq'''next [#_"RSeq" this]
            (when (pos? (:i this))
                (RSeq'new (:v this), (dec (:i this)))
            )
        )
    )

    (extend-type RSeq Counted
        (#_"int" Counted'''count [#_"RSeq" this]
            (inc (:i this))
        )
    )
)

(class-ns APersistentVector
    (defn #_"APersistentVector" APersistentVector'new []
        (merge (APersistentVector.) (AFn'new))
    )

    (extend-type APersistentVector IObject
        (#_"String" IObject'''toString [#_"APersistentVector" this]
            (RT'printString this)
        )
    )

    (extend-type APersistentVector Seqable
        (#_"ISeq" Seqable'''seq [#_"APersistentVector" this]
            (when (pos? (count this))
                (VSeq'new this, 0)
            )
        )
    )

    (extend-type APersistentVector Reversible
        (#_"ISeq" Reversible'''rseq [#_"APersistentVector" this]
            (when (pos? (count this))
                (RSeq'new this, (dec (count this)))
            )
        )
    )

    (extend-type APersistentVector IObject
        (#_"boolean" IObject'''equals [#_"APersistentVector" this, #_"Object" that]
            (or (identical? this that)
                (cond
                    (vector? that)
                        (when (= (count this) (count that)) => false
                            (loop-when [#_"int" i 0] (< i (count this)) => true
                                (recur-if (= (nth this i) (nth that i)) [(inc i)] => false)
                            )
                        )
                    (sequential? that)
                        (loop-when [#_"int" i 0 #_"ISeq" s (seq that)] (< i (count this)) => (nil? s)
                            (recur-if (and (some? s) (= (nth this i) (first s))) [(inc i) (next s)] => false)
                        )
                    :else
                        false
                )
            )
        )

        (#_"int" IObject'''hashCode [#_"APersistentVector" this]
            (loop-when [#_"int" hash 1 #_"int" i 0] (< i (count this)) => hash
                (let [#_"Object" o (nth this i)]
                    (recur (+ (* 31 hash) (if (some? o) (IObject'''hashCode o) 0)) (inc i))
                )
            )
        )
    )

    (extend-type APersistentVector IHashEq
        (#_"int" IHashEq'''hasheq [#_"APersistentVector" this]
            (loop-when [#_"int" hash 1 #_"int" i 0] (< i (count this)) => (Murmur3'mixCollHash hash, i)
                (recur (+ (* 31 hash) (Util'hasheq (nth this i))) (inc i))
            )
        )
    )

    (extend-type APersistentVector IFn
        (#_"Object" IFn'''invoke [#_"APersistentVector" this, #_"Object" i]
            (when (integer? i) => (throw! "key must be integer")
                (nth this (.intValue i))
            )
        )
    )

    (extend-type APersistentVector IPersistentStack
        (#_"Object" IPersistentStack'''peek [#_"APersistentVector" this]
            (let-when [#_"int" n (count this)] (pos? n)
                (nth this (dec n))
            )
        )
    )

    (declare MapEntry'create)

    (extend-type APersistentVector Associative
        (#_"IPersistentVector" Associative'''assoc [#_"APersistentVector" this, #_"Object" key, #_"Object" val]
            (when (integer? key) => (throw! "key must be integer")
                (IPersistentVector'''assocN this, (.intValue key), val)
            )
        )

        (#_"boolean" Associative'''containsKey [#_"APersistentVector" this, #_"Object" key]
            (and (integer? key) (< -1 (.intValue key) (count this)))
        )

        (#_"IMapEntry" Associative'''entryAt [#_"APersistentVector" this, #_"Object" key]
            (when (integer? key)
                (let-when [#_"int" i (.intValue key)] (< -1 i (count this))
                    (MapEntry'create key, (nth this i))
                )
            )
        )
    )

    (extend-type APersistentVector ILookup
        (#_"Object" ILookup'''valAt
            ([#_"APersistentVector" this, #_"Object" key] (ILookup'''valAt this, key, nil))
            ([#_"APersistentVector" this, #_"Object" key, #_"Object" notFound]
                (when (integer? key) => notFound
                    (let-when [#_"int" i (.intValue key)] (< -1 i (count this)) => notFound
                        (nth this i)
                    )
                )
            )
        )
    )

    #_foreign
    (defn #_"int" compareTo---APersistentVector [#_"APersistentVector" this, #_"IPersistentVector" that]
        (let [#_"int" n (count this) #_"int" m (count that)]
            (cond (< n m) -1 (< m n) 1
                :else
                    (loop-when [#_"int" i 0] (< i n) => 0
                        (let [#_"int" cmp (compare (nth this i) (nth that i))]
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
                    (let [#_"SubVector" sv v]
                        [(:v sv) (+ (:start sv) start) (+ (:start sv) end)]
                    )
                )]
            (merge (SubVector.) (APersistentVector'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"IPersistentVector" :v v
                    #_"int" :start start
                    #_"int" :end end
                )
            )
        )
    )

    (extend-type SubVector IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"SubVector" this]
            (:_meta this)
        )
    )

    (extend-type SubVector IObj
        (#_"SubVector" IObj'''withMeta [#_"SubVector" this, #_"IPersistentMap" meta]
            (when-not (= meta (:_meta this)) => this
                (SubVector'new meta, (:v this), (:start this), (:end this))
            )
        )
    )

    (extend-type SubVector Indexed
        (#_"Object" Indexed'''nth
            ([#_"SubVector" this, #_"int" i]
                (let-when [i (+ (:start this) i)] (and (<= (:start this) i) (< i (:end this))) => (throw (IndexOutOfBoundsException.))
                    (nth (:v this) i)
                )
            )
            ([#_"SubVector" this, #_"int" i, #_"Object" notFound]
                (when (< -1 i (count this)) => notFound
                    (nth this i)
                )
            )
        )
    )

    (extend-type SubVector IPersistentVector
        (#_"IPersistentVector" IPersistentVector'''assocN [#_"SubVector" this, #_"int" i, #_"Object" val]
            (cond
                (< (:end this) (+ (:start this) i)) (throw (IndexOutOfBoundsException.))
                (= (+ (:start this) i) (:end this)) (conj this val)
                :else (SubVector'new (:_meta this), (IPersistentVector'''assocN (:v this), (+ (:start this) i), val), (:start this), (:end this))
            )
        )
    )

    (extend-type SubVector Counted
        (#_"int" Counted'''count [#_"SubVector" this]
            (- (:end this) (:start this))
        )
    )

    (extend-type SubVector IPersistentCollection
        (#_"IPersistentVector" IPersistentCollection'''conj [#_"SubVector" this, #_"Object" o]
            (SubVector'new (:_meta this), (IPersistentVector'''assocN (:v this), (:end this), o), (:start this), (inc (:end this)))
        )

        (#_"IPersistentVector" IPersistentCollection'''empty [#_"SubVector" this]
            (with-meta [] (meta this))
        )
    )

    (extend-type SubVector IPersistentStack
        (#_"IPersistentStack" IPersistentStack'''pop [#_"SubVector" this]
            (if (= (dec (:end this)) (:start this))
                []
                (SubVector'new (:_meta this), (:v this), (:start this), (dec (:end this)))
            )
        )
    )
)
)

(java-ns cloiure.lang.AMapEntry

(class-ns AMapEntry
    (defn #_"AMapEntry" AMapEntry'new []
        (merge (AMapEntry.) (APersistentVector'new))
    )

    (extend-type AMapEntry Indexed
        (#_"Object" Indexed'''nth
            ([#_"AMapEntry" this, #_"int" i]
                (case i 0 (key this) 1 (val this) (throw (IndexOutOfBoundsException.)))
            )
            ([#_"AMapEntry" this, #_"int" i, #_"Object" notFound]
                (when (< -1 i (count this)) => notFound
                    (nth this i)
                )
            )
        )
    )

    (declare LazilyPersistentVector'createOwning)

    #_method
    (defn- #_"IPersistentVector" AMapEntry''asVector [#_"AMapEntry" this]
        (LazilyPersistentVector'createOwning (key this), (val this))
    )

    (extend-type AMapEntry IPersistentVector
        (#_"IPersistentVector" IPersistentVector'''assocN [#_"AMapEntry" this, #_"int" i, #_"Object" val]
            (IPersistentVector'''assocN (AMapEntry''asVector this), i, val)
        )
    )

    (extend-type AMapEntry Counted
        (#_"int" Counted'''count [#_"AMapEntry" this]
            2
        )
    )

    (extend-type AMapEntry Seqable
        (#_"ISeq" Seqable'''seq [#_"AMapEntry" this]
            (seq (AMapEntry''asVector this))
        )
    )

    (extend-type AMapEntry IPersistentCollection
        (#_"IPersistentVector" IPersistentCollection'''conj [#_"AMapEntry" this, #_"Object" o]
            (conj (AMapEntry''asVector this) o)
        )

        (#_"IPersistentVector" IPersistentCollection'''empty [#_"AMapEntry" this]
            nil
        )
    )

    (extend-type AMapEntry IPersistentStack
        (#_"IPersistentStack" IPersistentStack'''pop [#_"AMapEntry" this]
            (LazilyPersistentVector'createOwning (key this))
        )
    )
)
)

(java-ns cloiure.lang.MapEntry

(class-ns MapEntry
    (defn- #_"MapEntry" MapEntry'new [#_"Object" key, #_"Object" val]
        (merge (MapEntry.) (AMapEntry'new)
            (hash-map
                #_"Object" :_key key
                #_"Object" :_val val
            )
        )
    )

    (defn #_"MapEntry" MapEntry'create [#_"Object" key, #_"Object" val]
        (MapEntry'new key, val)
    )

    (extend-type MapEntry IMapEntry
        (#_"Object" IMapEntry'''key [#_"MapEntry" this]
            (:_key this)
        )

        (#_"Object" IMapEntry'''val [#_"MapEntry" this]
            (:_val this)
        )
    )
)
)

(java-ns cloiure.lang.ArraySeq

(class-ns ArraySeq
    (defn #_"ArraySeq" ArraySeq'new
        ([#_"Object[]" a, #_"int" i] (ArraySeq'new nil, a, i))
        ([#_"IPersistentMap" meta, #_"Object[]" a, #_"int" i]
            (merge (ArraySeq.) (ASeq'new meta)
                (hash-map
                    #_"Object[]" :a a
                    #_"int" :i i
                )
            )
        )
    )

    (extend-type ArraySeq IObj
        (#_"ArraySeq" IObj'''withMeta [#_"ArraySeq" this, #_"IPersistentMap" meta]
            (ArraySeq'new meta, (:a this), (:i this))
        )
    )

    (defn #_"ArraySeq" ArraySeq'create [& #_"Object..." a]
        (when (and (some? a) (pos? (alength a)))
            (ArraySeq'new a, 0)
        )
    )

    (§ soon extend-protocol Seqable Object'array
        (#_"ArraySeq" Seqable'''seq [#_"Object[]" a] (ArraySeq'create a))
    )

    (extend-type ArraySeq ISeq
        (#_"Object" ISeq'''first [#_"ArraySeq" this]
            (when (some? (:a this))
                (aget (:a this) (:i this))
            )
        )

        (#_"ISeq" ISeq'''next [#_"ArraySeq" this]
            (when (and (some? (:a this)) (< (inc (:i this)) (alength (:a this))))
                (ArraySeq'new (:a this), (inc (:i this)))
            )
        )
    )

    (extend-type ArraySeq Counted
        (#_"int" Counted'''count [#_"ArraySeq" this]
            (if (some? (:a this)) (- (alength (:a this)) (:i this)) 0)
        )
    )

    (extend-type ArraySeq IReduce
        (#_"Object" IReduce'''reduce
            ([#_"ArraySeq" this, #_"IFn" f]
                (when-some [#_"Object[]" a (:a this)]
                    (let [#_"int" i (:i this) #_"int" n (alength a)]
                        (loop-when [#_"Object" r (aget a i) i (inc i)] (< i n) => r
                            (let [r (f r (aget a i))]
                                (if (reduced? r) @r (recur r (inc i)))
                            )
                        )
                    )
                )
            )
            ([#_"ArraySeq" this, #_"IFn" f, #_"Object" r]
                (when-some [#_"Object[]" a (:a this)]
                    (let [#_"int" i (:i this) #_"int" n (alength a)]
                        (loop-when [r (f r (aget a i)) i (inc i)] (< i n) => (if (reduced? r) @r r)
                            (if (reduced? r) @r (recur (f r (aget a i)) (inc i)))
                        )
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.ATransientMap

(class-ns ATransientMap
    (defn #_"ATransientMap" ATransientMap'new []
        (merge (ATransientMap.) (AFn'new))
    )

    #_method
    (defn #_"ITransientMap" ATransientMap''conj [#_"ATransientMap" this, #_"Object" o]
        (ATransientMap'''ensureEditable this)
        (condp satisfies? o
            IMapEntry
                (assoc this (key o) (val o))
            IPersistentVector
                (when (= (count o) 2) => (throw! "vector arg to map conj must be a pair")
                    (assoc this (nth o 0) (nth o 1))
                )
            #_else
                (loop-when [this this #_"ISeq" s (seq o)] (some? s) => this
                    (let [#_"IMapEntry" e (first s)]
                        (recur (assoc this (key e) (val e)) (next s))
                    )
                )
        )
    )

    (extend-type ATransientMap IFn
        (#_"Object" IFn'''invoke
            ([#_"ATransientMap" this, #_"Object" key] (get this key))
            ([#_"ATransientMap" this, #_"Object" key, #_"Object" notFound] (get this key notFound))
        )
    )

    (extend-type ATransientMap ITransientAssociative
        (#_"ITransientMap" ITransientAssociative'''assoc [#_"ATransientMap" this, #_"Object" key, #_"Object" val]
            (ATransientMap'''ensureEditable this)
            (ATransientMap'''doAssoc this, key, val)
        )
    )

    (extend-type ATransientMap ITransientMap
        (#_"ITransientMap" ITransientMap'''dissoc [#_"ATransientMap" this, #_"Object" key]
            (ATransientMap'''ensureEditable this)
            (ATransientMap'''doDissoc this, key)
        )
    )

    (extend-type ATransientMap ITransientCollection
        (#_"IPersistentMap" ITransientCollection'''persistent [#_"ATransientMap" this]
            (ATransientMap'''ensureEditable this)
            (ATransientMap'''doPersistent this)
        )
    )

    (extend-type ATransientMap ILookup
        (#_"Object" ILookup'''valAt
            ([#_"ATransientMap" this, #_"Object" key] (ILookup'''valAt this, key, nil))
            ([#_"ATransientMap" this, #_"Object" key, #_"Object" notFound]
                (ATransientMap'''ensureEditable this)
                (ATransientMap'''doValAt this, key, notFound)
            )
        )
    )

    (def- #_"Object" ATransientMap'NOT_FOUND (Object.))

    (extend-type ATransientMap ITransientAssociative
        (#_"boolean" ITransientAssociative'''containsKey [#_"ATransientMap" this, #_"Object" key]
            (not (identical? (get this key ATransientMap'NOT_FOUND) ATransientMap'NOT_FOUND))
        )

        (#_"IMapEntry" ITransientAssociative'''entryAt [#_"ATransientMap" this, #_"Object" key]
            (let [#_"Object" v (get this key ATransientMap'NOT_FOUND)]
                (when-not (identical? v ATransientMap'NOT_FOUND)
                    (MapEntry'create key, v)
                )
            )
        )
    )

    (extend-type ATransientMap Counted
        (#_"int" Counted'''count [#_"ATransientMap" this]
            (ATransientMap'''ensureEditable this)
            (ATransientMap'''doCount this)
        )
    )
)
)

(java-ns cloiure.lang.ATransientSet

(class-ns ATransientSet
    (defn #_"ATransientSet" ATransientSet'new [#_"ITransientMap" impl]
        (merge (ATransientSet.) (AFn'new)
            (hash-map
                #_"ITransientMap" :impl impl
            )
        )
    )

    (extend-type ATransientSet Counted
        (#_"int" Counted'''count [#_"ATransientSet" this]
            (count (:impl this))
        )
    )

    (extend-type ATransientSet ITransientCollection
        (#_"ITransientSet" ITransientCollection'''conj [#_"ATransientSet" this, #_"Object" val]
            (let [#_"ITransientMap" m (assoc (:impl this) val val)]
                (when-not (= m (:impl this)) => this
                    (assoc this :impl m)
                )
            )
        )
    )

    (extend-type ATransientSet ITransientSet
        (#_"ITransientSet" ITransientSet'''disj [#_"ATransientSet" this, #_"Object" key]
            (let [#_"ITransientMap" m (dissoc (:impl this) key)]
                (when-not (= m (:impl this)) => this
                    (assoc this :impl m)
                )
            )
        )

        (#_"boolean" ITransientSet'''contains [#_"ATransientSet" this, #_"Object" key]
            (not (identical? (get (:impl this) key this) this))
        )

        (#_"Object" ITransientSet'''get [#_"ATransientSet" this, #_"Object" key]
            (get (:impl this) key)
        )
    )

    (extend-type ATransientSet IFn
        (#_"Object" IFn'''invoke
            ([#_"ATransientSet" this, #_"Object" key] (get (:impl this) key))
            ([#_"ATransientSet" this, #_"Object" key, #_"Object" notFound] (get (:impl this) key notFound))
        )
    )
)
)

(java-ns cloiure.lang.Cons

(class-ns Cons
    (defn #_"Cons" Cons'new
        ([#_"Object" _first, #_"ISeq" _more] (Cons'new nil, _first, _more))
        ([#_"IPersistentMap" meta, #_"Object" _first, #_"ISeq" _more]
            (merge (Cons.) (ASeq'new meta)
                (hash-map
                    #_"Object" :_first _first
                    #_"ISeq" :_more _more
                )
            )
        )
    )

    (extend-type Cons IObj
        (#_"Cons" IObj'''withMeta [#_"Cons" this, #_"IPersistentMap" meta]
            (Cons'new meta, (:_first this), (:_more this))
        )
    )

    (extend-type Cons ISeq
        (#_"Object" ISeq'''first [#_"Cons" this]
            (:_first this)
        )

        (#_"ISeq" ISeq'''next [#_"Cons" this]
            (seq (:_more this))
        )
    )

    (extend-type Cons Counted
        (#_"int" Counted'''count [#_"Cons" this]
            (inc (count (:_more this)))
        )
    )
)
)

(java-ns cloiure.lang.Cycle

(class-ns Cycle
    (defn- #_"Cycle" Cycle'new
        ([#_"ISeq" all, #_"ISeq" prev, #_"ISeq" current] (Cycle'new nil, all, prev, current))
        ([#_"IPersistentMap" meta, #_"ISeq" all, #_"ISeq" prev, #_"ISeq" current]
            (merge (Cycle.) (ASeq'new meta)
                (hash-map
                    #_"ISeq" :all all ;; never nil
                    #_"ISeq" :prev prev

                    #_"ISeq'" :_current (volatile! current) ;; lazily realized
                )
            )
        )
    )

    (extend-type Cycle IObj
        (#_"Cycle" IObj'''withMeta [#_"Cycle" this, #_"IPersistentMap" meta]
            (Cycle'new meta, (:all this), (:prev this), @(:_current this))
        )
    )

    (defn #_"ISeq" Cycle'create [#_"Seqable" s] (if-some [s (seq s)] (Cycle'new s, nil, s) ()))

    #_method
    (defn- #_"ISeq" Cycle''current [#_"Cycle" this]
        (or @(:_current this)
            (vreset! (:_current this) (or (next (:prev this)) (:all this)))
        )
    )

    (extend-type Cycle IPending
        (#_"boolean" IPending'''isRealized [#_"Cycle" this]
            (some? @(:_current this))
        )
    )

    (extend-type Cycle ISeq
        (#_"Object" ISeq'''first [#_"Cycle" this]
            (first (Cycle''current this))
        )

        (#_"ISeq" ISeq'''next [#_"Cycle" this]
            (Cycle'new (:all this), (Cycle''current this), nil)
        )
    )

    (extend-type Cycle IReduce
        (#_"Object" IReduce'''reduce
            ([#_"Cycle" this, #_"IFn" f]
                (loop [#_"ISeq" s (Cycle''current this) #_"Object" r (first s)]
                    (let [s (or (next s) (:all this)) r (f r (first s))]
                        (when-not (reduced? r) => @r
                            (recur s r)
                        )
                    )
                )
            )
            ([#_"Cycle" this, #_"IFn" f, #_"Object" r]
                (loop [#_"ISeq" s (Cycle''current this) r (f r (first s))]
                    (when-not (reduced? r) => @r
                        (let [s (or (next s) (:all this))]
                            (recur s (f r (first s)))
                        )
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.Delay

(class-ns Delay
    (defn #_"Delay" Delay'new [#_"IFn" f]
        (merge (Delay.)
            (hash-map
                #_"IFn'" :f (volatile! f)
                #_"Object'" :o (volatile! nil)
                #_"Throwable'" :e (volatile! nil)
            )
        )
    )

    (defn #_"Object" Delay'force [#_"Object" x]
        (if (instance? Delay x) (deref x) x)
    )

    (extend-type Delay IDeref
        (#_"Object" IDeref'''deref [#_"Delay" this]
            (when (some? @(:f this))
                (locking this
                    ;; double check
                    (when-some [#_"IFn" f @(:f this)]
                        (vreset! (:f this) nil)
                        (try
                            (vreset! (:o this) (f))
                            (catch Throwable t
                                (vreset! (:e this) t)
                            )
                        )
                    )
                )
            )
            (when-some [#_"Throwable" e @(:e this)]
                (throw e)
            )
            @(:o this)
        )
    )

    (extend-type Delay IPending
        (#_"boolean" IPending'''isRealized [#_"Delay" this]
            (locking this
                (nil? @(:f this))
            )
        )
    )
)
)

(java-ns cloiure.lang.Iterate

(class-ns Iterate
    (def- #_"Object" Iterate'UNREALIZED_SEED (Object.))

    (defn- #_"Iterate" Iterate'new
        ([#_"IFn" f, #_"Object" prev, #_"Object" seed] (Iterate'new nil, f, prev, seed))
        ([#_"IPersistentMap" meta, #_"IFn" f, #_"Object" prev, #_"Object" seed]
            (merge (Iterate.) (ASeq'new meta)
                (hash-map
                    #_"IFn" :f f ;; never nil
                    #_"Object" :prev prev

                    #_"Object'" :_seed (volatile! seed) ;; lazily realized
                )
            )
        )
    )

    (extend-type Iterate IObj
        (#_"Iterate" IObj'''withMeta [#_"Iterate" this, #_"IPersistentMap" meta]
            (Iterate'new meta, (:f this), (:prev this), @(:_seed this))
        )
    )

    (defn #_"ISeq" Iterate'create [#_"IFn" f, #_"Object" seed] (Iterate'new f, nil, seed))

    (extend-type Iterate IPending
        (#_"boolean" IPending'''isRealized [#_"Iterate" this]
            (not (identical? @(:_seed this) Iterate'UNREALIZED_SEED))
        )
    )

    (extend-type Iterate ISeq
        (#_"Object" ISeq'''first [#_"Iterate" this]
            (let-when [#_"Object" seed @(:_seed this)] (identical? seed Iterate'UNREALIZED_SEED) => seed
                (vreset! (:_seed this) ((:f this) (:prev this)))
            )
        )

        (#_"ISeq" ISeq'''next [#_"Iterate" this]
            (Iterate'new (:f this), (first this), Iterate'UNREALIZED_SEED)
        )
    )

    (extend-type Iterate IReduce
        (#_"Object" IReduce'''reduce
            ([#_"Iterate" this, #_"IFn" f]
                (loop [#_"Object" r (first this) #_"Object" v ((:f this) r)]
                    (let-when [r (f r v)] (reduced? r) => (recur r ((:f this) v))
                        @r
                    )
                )
            )
            ([#_"Iterate" this, #_"IFn" f, #_"Object" r]
                (loop [r r #_"Object" v (first this)]
                    (let-when [r (f r v)] (reduced? r) => (recur r ((:f this) v))
                        @r
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.KeywordLookupSite

(class-ns KeywordLookupSite
    (defn #_"KeywordLookupSite" KeywordLookupSite'new [#_"Keyword" k]
        (merge (KeywordLookupSite.)
            (hash-map
                #_"Keyword" :k k
            )
        )
    )

    #_method
    (defn- #_"ILookupThunk" KeywordLookupSite''ilookupThunk [#_"KeywordLookupSite" this, #_"Class" c]
        (reify ILookupThunk
            (#_"Object" ILookupThunk'''get [#_"ILookupThunk" self, #_"Object" target]
                (if (and (some? target) (= (class target) c))
                    (ILookup'''valAt (cast cloiure.core.ILookup target), (:k this))
                    self
                )
            )
        )
    )

    (extend-type KeywordLookupSite ILookupSite
        (#_"ILookupThunk" ILookupSite'''fault [#_"KeywordLookupSite" this, #_"Object" target]
            (if (satisfies? ILookup target)
                (KeywordLookupSite''ilookupThunk this, (class target))
                this
            )
        )
    )

    (extend-type KeywordLookupSite ILookupThunk
        (#_"Object" ILookupThunk'''get [#_"KeywordLookupSite" this, #_"Object" target]
            (if (satisfies? ILookup target)
                this
                (get target (:k this))
            )
        )
    )
)
)

(java-ns cloiure.lang.MethodImplCache

(class-ns Entry
    (defn #_"Entry" Entry'new [#_"Class" c, #_"IFn" fn]
        (merge (Entry.)
            (hash-map
                #_"Class" :c c
                #_"IFn" :fn fn
            )
        )
    )
)

(class-ns MethodImplCache
    (defn- #_"MethodImplCache" MethodImplCache'init [#_"IPersistentMap" protocol, #_"Keyword" methodk, #_"int" shift, #_"int" mask, #_"Object[]" table, #_"IPersistentMap" map]
        (merge (MethodImplCache.)
            (hash-map
                #_"IPersistentMap" :protocol protocol
                #_"Keyword" :methodk methodk
                #_"int" :shift shift
                #_"int" :mask mask
                #_"Object[]" :table table ;; [class, entry. class, entry ...]
                #_"IPersistentMap" :map map
            )
        )
    )

    (defn #_"MethodImplCache" MethodImplCache'new
        ([#_"IPersistentMap" protocol, #_"Keyword" methodk]
            (MethodImplCache'new protocol, methodk, 0, 0, (make-array Object 0))
        )
        ([#_"IPersistentMap" protocol, #_"Keyword" methodk, #_"int" shift, #_"int" mask, #_"Object[]" table]
            (MethodImplCache'init protocol, methodk, shift, mask, table, nil)
        )
        ([#_"IPersistentMap" protocol, #_"Keyword" methodk, #_"IPersistentMap" map]
            (MethodImplCache'init protocol, methodk, 0, 0, nil, map)
        )
    )

    #_method
    (defn #_"IFn" MethodImplCache''fnFor [#_"MethodImplCache" this, #_"Class" c]
        (if (some? (:map this))
            (when-some [#_"Entry" e (get (:map this) c)]
                (:fn e)
            )
            (let [#_"int" i (bit-shift-left (bit-and (bit-shift-right (IObject'''hashCode c) (:shift this)) (:mask this)) 1)]
                (let-when [#_"Object[]" t (:table this)] (and (< i (alength t)) (= (aget t i) c))
                    (when-some [#_"Entry" e (aget t (inc i))]
                        (:fn e)
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.Namespace

(class-ns Namespace
    (def #_"{Symbol Namespace}'" Namespace'namespaces (atom {}))

    (defn #_"Namespace" Namespace'new [#_"Symbol" name]
        (merge (Namespace.)
            (hash-map
                #_"Symbol" :name name

                #_"{Symbol Class|Var}'" :mappings (atom {})
                #_"{Symbol Namespace}'" :aliases (atom {})
            )
        )
    )

    (extend-type Namespace IObject
        (#_"String" IObject'''toString [#_"Namespace" this]
            (:name (:name this))
        )
    )

    (defn #_"ISeq" Namespace'all []
        (vals @Namespace'namespaces)
    )

    #_method
    (defn #_"IPersistentMap" Namespace''getMappings [#_"Namespace" this]
        @(:mappings this)
    )

    #_method
    (defn #_"Object" Namespace''getMapping [#_"Namespace" this, #_"Symbol" name]
        (get @(:mappings this) name)
    )

    (declare RT'CLOIURE_NS)

    #_method
    (defn- #_"void" Namespace''warnOrFailOnReplace [#_"Namespace" this, #_"Symbol" sym, #_"Object" o, #_"Var" var]
        (or
            (when (var? o)
                (when-not (or (= (:ns o) this) (= (:ns var) RT'CLOIURE_NS)) => :ok
                    (when-not (= (:ns o) RT'CLOIURE_NS)
                        (throw! (str sym " already refers to: " o " in namespace: " (:name this)))
                    )
                )
            )
            (.println *err*, (str "WARNING: " sym " already refers to: " o " in namespace: " (:name this) ", being replaced by: " var))
        )
        nil
    )

    (declare Var'new)

    #_method
    (defn #_"Var" Namespace''intern [#_"Namespace" this, #_"Symbol" sym]
        (when (nil? (:ns sym)) => (throw! "can't intern namespace-qualified symbol")
            (let [#_"Object" o
                    (or (get @(:mappings this) sym)
                        (let [#_"Var" v (Var'new this, sym)]
                            (swap! (:mappings this) assoc sym v)
                            v
                        )
                    )]
                (when-not (and (var? o) (= (:ns o) this)) => o
                    (let [#_"Var" v (Var'new this, sym)]
                        (Namespace''warnOrFailOnReplace this, sym, o, v)
                        (swap! (:mappings this) assoc sym v)
                        v
                    )
                )
            )
        )
    )

    #_method
    (defn #_"Var" Namespace''referenceVar [#_"Namespace" this, #_"Symbol" sym, #_"Var" var]
        (when (nil? (:ns sym)) => (throw! "can't intern namespace-qualified symbol")
            (let [#_"Object" o
                    (or (get @(:mappings this) sym)
                        (do
                            (swap! (:mappings this) assoc sym var)
                            var
                        )
                    )]
                (when-not (= o var)
                    (Namespace''warnOrFailOnReplace this, sym, o, var)
                    (swap! (:mappings this) assoc sym var)
                )
                var
            )
        )
    )

    (defn- #_"boolean" Namespace'areDifferentInstancesOfSameClassName [#_"Class" c1, #_"Class" c2]
        (and (not= c1 c2) (= (.getName c1) (.getName c2)))
    )

    #_method
    (defn #_"Class" Namespace''referenceClass [#_"Namespace" this, #_"Symbol" sym, #_"Class" cls]
        (when (nil? (:ns sym)) => (throw! "can't intern namespace-qualified symbol")
            (let [#_"Class" c
                    (let [c (get @(:mappings this) sym)]
                        (when (or (nil? c) (Namespace'areDifferentInstancesOfSameClassName c, cls)) => c
                            (swap! (:mappings this) assoc sym cls)
                            cls
                        )
                    )]
                (when (= c cls) => (throw! (str sym " already refers to: " c " in namespace: " (:name this)))
                    c
                )
            )
        )
    )

    #_method
    (defn #_"void" Namespace''unmap [#_"Namespace" this, #_"Symbol" sym]
        (when (nil? (:ns sym)) => (throw! "can't unintern namespace-qualified symbol")
            (swap! (:mappings this) dissoc sym)
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

    (defn #_"Namespace" Namespace'find [#_"Symbol" name]
        (get @Namespace'namespaces name)
    )

    (defn #_"Namespace" Namespace'findOrCreate [#_"Symbol" name]
        (or (Namespace'find name)
            (let [#_"Namespace" ns (Namespace'new name)]
                (swap! Namespace'namespaces assoc name ns)
                ns
            )
        )
    )

    (defn #_"Namespace" Namespace'remove [#_"Symbol" name]
        (when-not (= name (:name RT'CLOIURE_NS)) => (throw! "cannot remove core namespace")
            (get (first (swap-vals! Namespace'namespaces dissoc name)) name)
        )
    )

    #_method
    (defn #_"Var" Namespace''findInternedVar [#_"Namespace" this, #_"Symbol" name]
        (let [#_"Object" o (get @(:mappings this) name)]
            (when (and (var? o) (= (:ns o) this))
                o
            )
        )
    )

    #_method
    (defn #_"IPersistentMap" Namespace''getAliases [#_"Namespace" this]
        @(:aliases this)
    )

    #_method
    (defn #_"Namespace" Namespace''getAlias [#_"Namespace" this, #_"Symbol" alias]
        (get @(:aliases this) alias)
    )

    #_method
    (defn #_"void" Namespace''addAlias [#_"Namespace" this, #_"Symbol" alias, #_"Namespace" ns]
        (when (and (some? alias) (some? ns)) => (throw! "expecting Symbol + Namespace")
            (let [#_"Object" o
                    (or (get @(:aliases this) alias)
                        (do
                            (swap! (:aliases this) assoc alias ns)
                            ns
                        )
                    )]
                ;; you can rebind an alias, but only to the initially-aliased namespace
                (when-not (= o ns)
                    (throw! (str "alias " alias " already exists in namespace " (:name this) ", aliasing " o))
                )
            )
        )
        nil
    )

    #_method
    (defn #_"void" Namespace''removeAlias [#_"Namespace" this, #_"Symbol" alias]
        (swap! (:aliases this) dissoc alias)
        nil
    )
)
)

(java-ns cloiure.lang.PersistentArrayMap

(class-ns MSeq
    (defn #_"MSeq" MSeq'new
        ([#_"Object[]" a, #_"int" i] (MSeq'new nil, a, i))
        ([#_"IPersistentMap" meta, #_"Object[]" a, #_"int" i]
            (merge (MSeq.) (ASeq'new meta)
                (hash-map
                    #_"Object[]" :a a
                    #_"int" :i i
                )
            )
        )
    )

    (extend-type MSeq IObj
        (#_"MSeq" IObj'''withMeta [#_"MSeq" this, #_"IPersistentMap" meta]
            (MSeq'new meta, (:a this), (:i this))
        )
    )

    (extend-type MSeq ISeq
        (#_"Object" ISeq'''first [#_"MSeq" this]
            (MapEntry'create (aget (:a this) (:i this)), (aget (:a this) (inc (:i this))))
        )

        (#_"ISeq" ISeq'''next [#_"MSeq" this]
            (when (< (+ (:i this) 2) (alength (:a this)))
                (MSeq'new (:a this), (+ (:i this) 2))
            )
        )
    )

    (extend-type MSeq Counted
        (#_"int" Counted'''count [#_"MSeq" this]
            (quot (- (alength (:a this)) (:i this)) 2)
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
        ([] (PersistentArrayMap'new nil))
        ;; This ctor captures/aliases the passed array, so do not modify it later.
        ([#_"Object[]" a] (PersistentArrayMap'new nil, a))
        ([#_"IPersistentMap" meta, #_"Object[]" a]
            (merge (PersistentArrayMap.) (APersistentMap'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"Object[]" :a (or a (make-array Object 0))
                )
            )
        )
    )

    (extend-type PersistentArrayMap IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"PersistentArrayMap" this]
            (:_meta this)
        )
    )

    (extend-type PersistentArrayMap IObj
        (#_"PersistentArrayMap" IObj'''withMeta [#_"PersistentArrayMap" this, #_"IPersistentMap" meta]
            (PersistentArrayMap'new meta, (:a this))
        )
    )

    (def #_"PersistentArrayMap" PersistentArrayMap'EMPTY (PersistentArrayMap'new))

    #_method
    (defn #_"PersistentArrayMap" PersistentArrayMap''create [#_"PersistentArrayMap" this & #_"Object..." init]
        (PersistentArrayMap'new (meta this), init)
    )

    (defn #_"PersistentArrayMap" PersistentArrayMap'createWithCheck [#_"Object[]" init]
        (loop-when-recur [#_"int" i 0] (< i (alength init)) [(+ i 2)]
            (loop-when-recur [#_"int" j (+ i 2)] (< j (alength init)) [(+ j 2)]
                (when (= (aget init i) (aget init j))
                    (throw! (str "duplicate key: " (aget init i)))
                )
            )
        )
        (PersistentArrayMap'new init)
    )

    (defn #_"PersistentArrayMap" PersistentArrayMap'createAsIfByAssoc [#_"Object[]" init]
        (when (odd? (alength init))
            (throw! (str "no value supplied for key: " (aget init (dec (alength init)))))
        )
        ;; If this looks like it is doing busy-work, it is because it is achieving these goals: O(n^2) run time
        ;; like createWithCheck(), never modify init arg, and only allocate memory if there are duplicate keys.
        (let [#_"int" n
                (loop-when [n 0 #_"int" i 0] (< i (alength init)) => n
                    (let [#_"boolean" dup?
                            (loop-when [dup? false #_"int" j 0] (< j i) => dup?
                                (or (= (aget init i) (aget init j))
                                    (recur dup? (+ j 2))
                                )
                            )]
                        (recur (if dup? n (+ n 2)) (+ i 2))
                    )
                )
              init
                (when (< n (alength init)) => init
                    ;; Create a new, shorter array with unique keys, and the last value associated with each key.
                    ;; To behave like assoc, the first occurrence of each key must be used, since its metadata
                    ;; may be different than later equal keys.
                    (let [#_"Object[]" nodups (make-array Object n)
                          #_"int" m
                            (loop-when [m 0 #_"int" i 0] (< i (alength init)) => m
                                (let [#_"boolean" dup?
                                        (loop-when [dup? false #_"int" j 0] (< j m) => dup?
                                            (or (= (aget init i) (aget nodups j))
                                                (recur dup? (+ j 2))
                                            )
                                        )
                                      m (when-not dup? => m
                                            (let [#_"int" j
                                                    (loop-when [j (- (alength init) 2)] (<= i j) => j
                                                        (if (= (aget init i) (aget init j))
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
                        (when (= m n) => (throw! (str "internal error: m=" m))
                            nodups
                        )
                    )
                )]
            (PersistentArrayMap'new init)
        )
    )

    (extend-type PersistentArrayMap Counted
        (#_"int" Counted'''count [#_"PersistentArrayMap" this]
            (quot (alength (:a this)) 2)
        )
    )

    #_method
    (defn- #_"int" PersistentArrayMap''indexOf [#_"PersistentArrayMap" this, #_"Object" key]
        (loop-when [#_"int" i 0] (< i (alength (:a this))) => -1
            (if (= key (aget (:a this) i)) i (recur (+ i 2)))
        )
    )

    (extend-type PersistentArrayMap Associative
        (#_"boolean" Associative'''containsKey [#_"PersistentArrayMap" this, #_"Object" key]
            (<= 0 (PersistentArrayMap''indexOf this, key))
        )

        (#_"IMapEntry" Associative'''entryAt [#_"PersistentArrayMap" this, #_"Object" key]
            (let-when [#_"int" i (PersistentArrayMap''indexOf this, key)] (<= 0 i)
                (MapEntry'create (aget (:a this) i), (aget (:a this) (inc i)))
            )
        )
    )

    (declare PersistentHashMap'create-1a)

    (extend-type PersistentArrayMap Associative
        (#_"IPersistentMap" Associative'''assoc [#_"PersistentArrayMap" this, #_"Object" key, #_"Object" val]
            (let [#_"int" i (PersistentArrayMap''indexOf this, key)]
                (if (<= 0 i) ;; already have key, same-sized replacement
                    (if (= (aget (:a this) (inc i)) val) ;; no change, no op
                        this
                        (let [#_"Object[]" a (.clone (:a this))]
                            (aset a (inc i) val)
                            (PersistentArrayMap''create this, a)
                        )
                    )
                    ;; didn't have key, grow
                    (if (< PersistentArrayMap'HASHTABLE_THRESHOLD (alength (:a this)))
                        (-> (PersistentHashMap'create-1a (:a this)) (assoc key val) (with-meta (meta this)))
                        (let [#_"int" n (alength (:a this)) #_"Object[]" a (make-array Object (+ n 2))]
                            (when (pos? n)
                                (System/arraycopy (:a this), 0, a, 0, n)
                            )
                            (aset a n key)
                            (aset a (inc n) val)
                            (PersistentArrayMap''create this, a)
                        )
                    )
                )
            )
        )
    )

    (declare empty)

    (extend-type PersistentArrayMap IPersistentMap
        (#_"IPersistentMap" IPersistentMap'''dissoc [#_"PersistentArrayMap" this, #_"Object" key]
            (let-when [#_"int" i (PersistentArrayMap''indexOf this, key)] (<= 0 i) => this ;; don't have key, no op
                ;; have key, will remove
                (let-when [#_"int" n (- (alength (:a this)) 2)] (pos? n) => (empty this)
                    (let [#_"Object[]" a (make-array Object n)]
                        (System/arraycopy (:a this), 0, a, 0, i)
                        (System/arraycopy (:a this), (+ i 2), a, i, (- n i))
                        (PersistentArrayMap''create this, a)
                    )
                )
            )
        )
    )

    (extend-type PersistentArrayMap IPersistentCollection
        (#_"IPersistentMap" IPersistentCollection'''empty [#_"PersistentArrayMap" this]
            (with-meta PersistentArrayMap'EMPTY (meta this))
        )
    )

    (extend-type PersistentArrayMap ILookup
        (#_"Object" ILookup'''valAt
            ([#_"PersistentArrayMap" this, #_"Object" key] (ILookup'''valAt this, key, nil))
            ([#_"PersistentArrayMap" this, #_"Object" key, #_"Object" notFound]
                (let [#_"int" i (PersistentArrayMap''indexOf this, key)]
                    (if (<= 0 i) (aget (:a this) (inc i)) notFound)
                )
            )
        )
    )

    #_method
    (defn #_"int" PersistentArrayMap''capacity [#_"PersistentArrayMap" this]
        (count this)
    )

    (extend-type PersistentArrayMap Seqable
        (#_"ISeq" Seqable'''seq [#_"PersistentArrayMap" this]
            (when (pos? (alength (:a this)))
                (MSeq'new (:a this), 0)
            )
        )
    )

    (extend-type PersistentArrayMap IKVReduce
        (#_"Object" IKVReduce'''kvreduce [#_"PersistentArrayMap" this, #_"IFn" f, #_"Object" r]
            (loop-when [r r #_"int" i 0] (< i (alength (:a this))) => r
                (let [r (f r (aget (:a this) i), (aget (:a this) (inc i)))]
                    (if (reduced? r) @r (recur r (+ i 2)))
                )
            )
        )
    )

    (declare TransientArrayMap'new)

    (extend-type PersistentArrayMap IEditableCollection
        (#_"ITransientMap" IEditableCollection'''asTransient [#_"PersistentArrayMap" this]
            (TransientArrayMap'new (:a this))
        )
    )
)

(class-ns TransientArrayMap
    (defn #_"TransientArrayMap" TransientArrayMap'new [#_"Object[]" a]
        (let [#_"int" n (alength a)
              #_"Object[]" a' (make-array Object (max PersistentArrayMap'HASHTABLE_THRESHOLD n)) _ (System/arraycopy a, 0, a', 0, n)]
            (merge (TransientArrayMap.) (ATransientMap'new)
                (hash-map
                    #_"Object[]" :a a'
                    #_"int" :n n

                    #_"Thread'" :owner (volatile! (Thread/currentThread))
                )
            )
        )
    )

    #_method
    (defn- #_"int" TransientArrayMap''indexOf [#_"TransientArrayMap" this, #_"Object" key]
        (loop-when [#_"int" i 0] (< i (:n this)) => -1
            (if (= (aget (:a this) i) key) i (recur (+ i 2)))
        )
    )

    #_override
    (defn #_"ITransientMap" ATransientMap'''doAssoc--TransientArrayMap [#_"TransientArrayMap" this, #_"Object" key, #_"Object" val]
        (let [#_"int" i (TransientArrayMap''indexOf this, key)]
            (cond (<= 0 i) ;; already have key,
                (do
                    (when-not (= (aget (:a this) (inc i)) val) ;; no change, no op
                        (aset (:a this) (inc i) val)
                    )
                    this
                )
                :else ;; didn't have key, grow
                (if (< (:n this) (alength (:a this)))
                    (let [_ (aset (:a this) (:n this) key) this (update this :n inc)
                          _ (aset (:a this) (:n this) val) this (update this :n inc)]
                        this
                    )
                    (-> (PersistentHashMap'create-1a (:a this)) (transient) (assoc key val))
                )
            )
        )
    )

    #_override
    (defn #_"ITransientMap" ATransientMap'''doDissoc--TransientArrayMap [#_"TransientArrayMap" this, #_"Object" key]
        (let-when [#_"int" i (TransientArrayMap''indexOf this, key)] (<= 0 i) => this
            ;; have key, will remove
            (when (<= 2 (:n this))
                (aset (:a this) i (aget (:a this) (- (:n this) 2)))
                (aset (:a this) (inc i) (aget (:a this) (- (:n this) 1)))
            )
            (update this :n - 2)
        )
    )

    #_override
    (defn #_"Object" ATransientMap'''doValAt--TransientArrayMap [#_"TransientArrayMap" this, #_"Object" key, #_"Object" notFound]
        (let [#_"int" i (TransientArrayMap''indexOf this, key)]
            (if (<= 0 i) (aget (:a this) (inc i)) notFound)
        )
    )

    #_override
    (defn #_"int" ATransientMap'''doCount--TransientArrayMap [#_"TransientArrayMap" this]
        (quot (:n this) 2)
    )

    #_override
    (defn #_"IPersistentMap" ATransientMap'''doPersistent--TransientArrayMap [#_"TransientArrayMap" this]
        (ATransientMap'''ensureEditable this)
        (vreset! (:owner this) nil)
        (let [#_"Object[]" a' (make-array Object (:n this)) _ (System/arraycopy (:a this), 0, a', 0, (:n this))]
            (PersistentArrayMap'new a')
        )
    )

    #_override
    (defn #_"void" ATransientMap'''ensureEditable--TransientArrayMap [#_"TransientArrayMap" this]
        (when (nil? @(:owner this))
            (throw! "transient used after persistent! call")
        )
        nil
    )
)
)

(java-ns cloiure.lang.PersistentHashMap

(class-ns HSeq
    (defn- #_"HSeq" HSeq'new [#_"IPersistentMap" meta, #_"INode[]" nodes, #_"int" i, #_"ISeq" s]
        (merge (HSeq.) (ASeq'new meta)
            (hash-map
                #_"INode[]" :nodes nodes
                #_"int" :i i
                #_"ISeq" :s s
            )
        )
    )

    (extend-type HSeq IObj
        (#_"HSeq" IObj'''withMeta [#_"HSeq" this, #_"IPersistentMap" meta]
            (HSeq'new meta, (:nodes this), (:i this), (:s this))
        )
    )

    (defn- #_"ISeq" HSeq'create-4 [#_"IPersistentMap" meta, #_"INode[]" nodes, #_"int" i, #_"ISeq" s]
        (when (nil? s) => (HSeq'new meta, nodes, i, s)
            (loop-when i (< i (alength nodes))
                (let-when [#_"INode" ai (aget nodes i)] (some? ai) => (recur (inc i))
                    (let-when [s (INode'''nodeSeq ai)] (some? s) => (recur (inc i))
                        (HSeq'new meta, nodes, (inc i), s)
                    )
                )
            )
        )
    )

    (defn #_"ISeq" HSeq'create-1 [#_"INode[]" nodes]
        (HSeq'create-4 nil, nodes, 0, nil)
    )

    (extend-type HSeq ISeq
        (#_"Object" ISeq'''first [#_"HSeq" this]
            (first (:s this))
        )

        (#_"ISeq" ISeq'''next [#_"HSeq" this]
            (HSeq'create-4 nil, (:nodes this), (:i this), (next (:s this)))
        )
    )
)

(class-ns NodeSeq
    (defn #_"NodeSeq" NodeSeq'new
        ([#_"Object[]" a, #_"int" i] (NodeSeq'new nil, a, i, nil))
        ([#_"IPersistentMap" meta, #_"Object[]" a, #_"int" i, #_"ISeq" s]
            (merge (NodeSeq.) (ASeq'new meta)
                (hash-map
                    #_"Object[]" :a a
                    #_"int" :i i
                    #_"ISeq" :s s
                )
            )
        )
    )

    (extend-type NodeSeq IObj
        (#_"NodeSeq" IObj'''withMeta [#_"NodeSeq" this, #_"IPersistentMap" meta]
            (NodeSeq'new meta, (:a this), (:i this), (:s this))
        )
    )

    (defn- #_"ISeq" NodeSeq'create-3 [#_"Object[]" a, #_"int" i, #_"ISeq" s]
        (when (nil? s) => (NodeSeq'new nil, a, i, s)
            (loop-when i (< i (alength a))
                (when (nil? (aget a i)) => (NodeSeq'new nil, a, i, nil)
                    (or
                        (when-some [#_"INode" node (cast cloiure.core.INode (aget a (inc i)))]
                            (when-some [s (INode'''nodeSeq node)]
                                (NodeSeq'new nil, a, (+ i 2), s)
                            )
                        )
                        (recur (+ i 2))
                    )
                )
            )
        )
    )

    (defn #_"ISeq" NodeSeq'create-1 [#_"Object[]" a]
        (NodeSeq'create-3 a, 0, nil)
    )

    (extend-type NodeSeq ISeq
        (#_"Object" ISeq'''first [#_"NodeSeq" this]
            (if (some? (:s this))
                (first (:s this))
                (MapEntry'create (aget (:a this) (:i this)), (aget (:a this) (inc (:i this))))
            )
        )

        (#_"ISeq" ISeq'''next [#_"NodeSeq" this]
            (if (some? (:s this))
                (NodeSeq'create-3 (:a this), (:i this), (next (:s this)))
                (NodeSeq'create-3 (:a this), (+ (:i this) 2), nil)
            )
        )
    )

    (defn #_"Object" NodeSeq'kvreduce [#_"Object[]" a, #_"IFn" f, #_"Object" r]
        (loop-when [r r #_"int" i 0] (< i (alength a)) => r
            (let [r (if (some? (aget a i))
                        (f r (aget a i), (aget a (inc i)))
                        (let-when [#_"INode" node (cast cloiure.core.INode (aget a (inc i)))] (some? node) => r
                            (INode'''kvreduce node, f, r)
                        )
                    )]
                (when-not (reduced? r) => r
                    (recur r (+ i 2))
                )
            )
        )
    )
)

(class-ns PersistentHashMap
    (defn #_"int" PersistentHashMap'mask [#_"int" hash, #_"int" shift]
        (bit-and (unsigned-bit-shift-right hash shift) 0x01f)
    )

    (defn- #_"int" PersistentHashMap'bitpos [#_"int" hash, #_"int" shift]
        (bit-shift-left 1 (PersistentHashMap'mask hash, shift))
    )

    (defn- #_"Object[]" PersistentHashMap'cloneAndSet
        ([#_"Object[]" a, #_"int" i, #_"Object" x]                          (let [a (.clone a)] (aset a i x)              a))
        ([#_"Object[]" a, #_"int" i, #_"Object" x, #_"int" j, #_"Object" y] (let [a (.clone a)] (aset a i x) (aset a j y) a))
    )

    (defn- #_"Object[]" PersistentHashMap'removePair [#_"Object[]" a, #_"int" i]
        (let [#_"Object[]" a' (make-array Object (- (alength a) 2)) #_"int" ii (* 2 i)]
            (System/arraycopy a, 0, a', 0, ii)
            (System/arraycopy a, (+ ii 2), a', ii, (- (alength a') ii))
            a'
        )
    )
)

(class-ns ArrayNode
    (defn #_"ArrayNode" ArrayNode'new [#_"AtomicReference<Thread>" edit, #_"int" n, #_"INode[]" a]
        (merge (ArrayNode.)
            (hash-map
                #_"AtomicReference<Thread>" :edit edit
                #_"int" :n n
                #_"INode[]" :a a
            )
        )
    )

    (declare BitmapIndexedNode'EMPTY)

    (extend-type ArrayNode INode
        (#_"INode" INode'''assoc [#_"ArrayNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"boolean'" addedLeaf]
            (let [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" ai (aget (:a this) i)]
                (if (some? ai)
                    (let [#_"INode" node (INode'''assoc ai, (+ shift 5), hash, key, val, addedLeaf)]
                        (when-not (= node ai) => this
                            (ArrayNode'new nil, (:n this), (PersistentHashMap'cloneAndSet (:a this), i, node))
                        )
                    )
                    (let [#_"INode" node (INode'''assoc BitmapIndexedNode'EMPTY, (+ shift 5), hash, key, val, addedLeaf)]
                        (ArrayNode'new nil, (inc (:n this)), (PersistentHashMap'cloneAndSet (:a this), i, node))
                    )
                )
            )
        )
    )

    (declare BitmapIndexedNode'new)

    #_method
    (defn- #_"INode" ArrayNode''pack [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit, #_"int" idx]
        (let [#_"Object[]" a' (make-array Object (* 2 (dec (:n this))))
              [#_"int" bitmap #_"int" j]
                (loop-when [bitmap 0 j 1 #_"int" i 0] (< i idx) => [bitmap j]
                    (let [[bitmap j]
                            (when (some? (aget (:a this) i)) => [bitmap j]
                                (aset a' j (aget (:a this) i))
                                [(bit-or bitmap (bit-shift-left 1 i)) (+ j 2)]
                            )]
                        (recur bitmap j (inc i))
                    )
                )
              bitmap
                (loop-when [bitmap bitmap j j #_"int" i (inc idx)] (< i (alength (:a this))) => bitmap
                    (let [[bitmap j]
                            (when (some? (aget (:a this) i)) => [bitmap j]
                                (aset a' j (aget (:a this) i))
                                [(bit-or bitmap (bit-shift-left 1 i)) (+ j 2)]
                            )]
                        (recur bitmap j (inc i))
                    )
                )]
            (BitmapIndexedNode'new edit, bitmap, a')
        )
    )

    (extend-type ArrayNode INode
        (#_"INode" INode'''dissoc [#_"ArrayNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
            (let-when [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" ai (aget (:a this) i)] (some? ai) => this
                (let-when-not [#_"INode" node (INode'''dissoc ai, (+ shift 5), hash, key)] (= node ai) => this
                    (cond
                        (some? node)     (ArrayNode'new nil, (:n this), (PersistentHashMap'cloneAndSet (:a this), i, node))
                        (<= (:n this) 8) (ArrayNode''pack this, nil, i) ;; shrink
                        :else            (ArrayNode'new nil, (dec (:n this)), (PersistentHashMap'cloneAndSet (:a this), i, node))
                    )
                )
            )
        )

        (#_"IMapEntry|Object" INode'''find
            ([#_"ArrayNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
                (let [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" node (aget (:a this) i)]
                    (when (some? node)
                        (INode'''find node, (+ shift 5), hash, key)
                    )
                )
            )
            ([#_"ArrayNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" notFound]
                (let [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" node (aget (:a this) i)]
                    (when (some? node) => notFound
                        (INode'''find node, (+ shift 5), hash, key, notFound)
                    )
                )
            )
        )

        (#_"ISeq" INode'''nodeSeq [#_"ArrayNode" this]
            (HSeq'create-1 (:a this))
        )

        (#_"Object" INode'''kvreduce [#_"ArrayNode" this, #_"IFn" f, #_"Object" r]
            (let [#_"INode[]" a (:a this)]
                (loop-when [r r #_"int" i 0] (< i (alength a)) => r
                    (let-when [#_"INode" node (aget a i)] (some? node) => (recur r (inc i))
                        (let [r (INode'''kvreduce node, f, r)]
                            (when-not (reduced? r) => r
                                (recur r (inc i))
                            )
                        )
                    )
                )
            )
        )
    )

    #_method
    (defn- #_"ArrayNode" ArrayNode''ensureEditable [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit]
        (if (= (:edit this) edit)
            this
            (ArrayNode'new edit, (:n this), (.clone (:a this)))
        )
    )

    #_method
    (defn- #_"ArrayNode" ArrayNode''editAndSet [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"INode" node]
        (let [#_"ArrayNode" e (ArrayNode''ensureEditable this, edit)]
            (aset (:a e) i node)
            e
        )
    )

    (extend-type ArrayNode INode
        (#_"INode" INode'''assocT [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"boolean'" addedLeaf]
            (let [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" ai (aget (:a this) i)]
                (if (some? ai)
                    (let [#_"INode" node (INode'''assocT ai, edit, (+ shift 5), hash, key, val, addedLeaf)]
                        (when-not (= node ai) => this
                            (ArrayNode''editAndSet this, edit, i, node)
                        )
                    )
                    (-> (ArrayNode''editAndSet this, edit, i, (INode'''assocT BitmapIndexedNode'EMPTY, edit, (+ shift 5), hash, key, val, addedLeaf))
                        (update :n inc)
                    )
                )
            )
        )

        (#_"INode" INode'''dissocT [#_"ArrayNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"boolean'" removedLeaf]
            (let-when [#_"int" i (PersistentHashMap'mask hash, shift) #_"INode" ai (aget (:a this) i)] (some? ai) => this
                (let-when-not [#_"INode" node (INode'''dissocT ai, edit, (+ shift 5), hash, key, removedLeaf)] (= node ai) => this
                    (cond
                        (some? node)     (ArrayNode''editAndSet this, edit, i, node)
                        (<= (:n this) 8) (ArrayNode''pack this, edit, i) ;; shrink
                        :else            (-> (ArrayNode''editAndSet this, edit, i, node) (update :n dec))
                    )
                )
            )
        )
    )
)

(class-ns BitmapIndexedNode
    (defn #_"BitmapIndexedNode" BitmapIndexedNode'new [#_"AtomicReference<Thread>" edit, #_"int" bitmap, #_"Object[]" a]
        (merge (BitmapIndexedNode.)
            (hash-map
                #_"AtomicReference<Thread>" :edit edit
                #_"int" :bitmap bitmap
                #_"Object[]" :a a
            )
        )
    )

    (def #_"BitmapIndexedNode" BitmapIndexedNode'EMPTY (BitmapIndexedNode'new nil, 0, (object-array 0)))

    #_method
    (defn #_"int" BitmapIndexedNode''index [#_"BitmapIndexedNode" this, #_"int" bit]
        (Integer/bitCount (bit-and (:bitmap this) (dec bit)))
    )

    (declare HashCollisionNode'new)

    (defn- #_"INode" BitmapIndexedNode'createNode-6 [#_"int" shift, #_"Object" key1, #_"Object" val1, #_"int" key2hash, #_"Object" key2, #_"Object" val2]
        (let [#_"int" key1hash (Util'hasheq key1)]
            (when-not (= key1hash key2hash) => (HashCollisionNode'new nil, key1hash, 2, (object-array [ key1, val1, key2, val2 ]))
                (let [#_"boolean'" addedLeaf (volatile! false) #_"AtomicReference<Thread>" edit (AtomicReference. nil)]
                    (-> BitmapIndexedNode'EMPTY
                        (INode'''assocT edit, shift, key1hash, key1, val1, addedLeaf)
                        (INode'''assocT edit, shift, key2hash, key2, val2, addedLeaf)
                    )
                )
            )
        )
    )

    (extend-type BitmapIndexedNode INode
        (#_"INode" INode'''assoc [#_"BitmapIndexedNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"boolean'" addedLeaf]
            (let [#_"int" bit (PersistentHashMap'bitpos hash, shift) #_"int" idx (BitmapIndexedNode''index this, bit)]
                (if-not (zero? (bit-and (:bitmap this) bit))
                    (let [#_"Object" keyOrNull (aget (:a this) (* 2 idx))
                          #_"Object" valOrNode (aget (:a this) (inc (* 2 idx)))
                          _ (cond
                                (nil? keyOrNull)
                                    (let [#_"INode" node (INode'''assoc (cast cloiure.core.INode valOrNode), (+ shift 5), hash, key, val, addedLeaf)]
                                        (when-not (= node valOrNode)
                                            (PersistentHashMap'cloneAndSet (:a this), (inc (* 2 idx)), node)
                                        )
                                    )
                                (= key keyOrNull)
                                    (when-not (= val valOrNode)
                                        (PersistentHashMap'cloneAndSet (:a this), (inc (* 2 idx)), val)
                                    )
                                :else
                                    (let [_ (vreset! addedLeaf true)]
                                        (PersistentHashMap'cloneAndSet (:a this), (* 2 idx), nil, (inc (* 2 idx)), (BitmapIndexedNode'createNode-6 (+ shift 5), keyOrNull, valOrNode, hash, key, val))
                                    )
                            )]
                        (if (some? _) (BitmapIndexedNode'new nil, (:bitmap this), _) this)
                    )
                    (let [#_"int" n (Integer/bitCount (:bitmap this))]
                        (if (<= 16 n)
                            (let [#_"INode[]" nodes (make-array #_"INode" Object 32) #_"int" jdx (PersistentHashMap'mask hash, shift)]
                                (aset nodes jdx (INode'''assoc BitmapIndexedNode'EMPTY, (+ shift 5), hash, key, val, addedLeaf))
                                (loop-when [#_"int" j 0 #_"int" i 0] (< i 32)
                                    (when (odd? (unsigned-bit-shift-right (:bitmap this) i)) => (recur j (inc i))
                                        (if (some? (aget (:a this) j))
                                            (aset nodes i (INode'''assoc BitmapIndexedNode'EMPTY, (+ shift 5), (Util'hasheq (aget (:a this) j)), (aget (:a this) j), (aget (:a this) (inc j)), addedLeaf))
                                            (aset nodes i (cast cloiure.core.INode (aget (:a this) (inc j))))
                                        )
                                        (recur (+ j 2) (inc i))
                                    )
                                )
                                (ArrayNode'new nil, (inc n), nodes)
                            )
                            (let [#_"Object[]" a' (make-array Object (* 2 (inc n)))]
                                (System/arraycopy (:a this), 0, a', 0, (* 2 idx))
                                (aset a' (* 2 idx) key)
                                (vreset! addedLeaf true)
                                (aset a' (inc (* 2 idx)) val)
                                (System/arraycopy (:a this), (* 2 idx), a', (* 2 (inc idx)), (* 2 (- n idx)))
                                (BitmapIndexedNode'new nil, (bit-or (:bitmap this) bit), a')
                            )
                        )
                    )
                )
            )
        )

        (#_"INode" INode'''dissoc [#_"BitmapIndexedNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
            (let-when-not [#_"int" bit (PersistentHashMap'bitpos hash, shift)] (zero? (bit-and (:bitmap this) bit)) => this
                (let [#_"int" i (BitmapIndexedNode''index this, bit) #_"int" ii (* 2 i)
                      #_"Object" keyOrNull (aget (:a this) ii)
                      #_"Object" valOrNode (aget (:a this) (inc ii))]
                    (if (some? keyOrNull)
                        (when (= key keyOrNull) => this
                            ;; TODO: collapse
                            (BitmapIndexedNode'new nil, (bit-xor (:bitmap this) bit), (PersistentHashMap'removePair (:a this), i))
                        )
                        (let [#_"INode" node (INode'''dissoc (cast cloiure.core.INode valOrNode), (+ shift 5), hash, key)]
                            (cond
                                (= node valOrNode)
                                    this
                                (some? node)
                                    (BitmapIndexedNode'new nil, (:bitmap this), (PersistentHashMap'cloneAndSet (:a this), (inc ii), node))
                                (= (:bitmap this) bit)
                                    nil
                                :else
                                    (BitmapIndexedNode'new nil, (bit-xor (:bitmap this) bit), (PersistentHashMap'removePair (:a this), i))
                            )
                        )
                    )
                )
            )
        )

        (#_"IMapEntry|Object" INode'''find
            ([#_"BitmapIndexedNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
                (let-when-not [#_"int" bit (PersistentHashMap'bitpos hash, shift)] (zero? (bit-and (:bitmap this) bit))
                    (let [#_"int" i (BitmapIndexedNode''index this, bit)
                        #_"Object" keyOrNull (aget (:a this) (* 2 i))
                        #_"Object" valOrNode (aget (:a this) (inc (* 2 i)))]
                        (cond
                            (nil? keyOrNull)  (INode'''find (cast cloiure.core.INode valOrNode), (+ shift 5), hash, key)
                            (= key keyOrNull) (MapEntry'create keyOrNull, valOrNode)
                        )
                    )
                )
            )
            ([#_"BitmapIndexedNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" notFound]
                (let-when-not [#_"int" bit (PersistentHashMap'bitpos hash, shift)] (zero? (bit-and (:bitmap this) bit)) => notFound
                    (let [#_"int" i (BitmapIndexedNode''index this, bit)
                        #_"Object" keyOrNull (aget (:a this) (* 2 i))
                        #_"Object" valOrNode (aget (:a this) (inc (* 2 i)))]
                        (cond
                            (nil? keyOrNull)  (INode'''find (cast cloiure.core.INode valOrNode), (+ shift 5), hash, key, notFound)
                            (= key keyOrNull) valOrNode
                            :else             notFound
                        )
                    )
                )
            )
        )

        (#_"ISeq" INode'''nodeSeq [#_"BitmapIndexedNode" this]
            (NodeSeq'create-1 (:a this))
        )

        (#_"Object" INode'''kvreduce [#_"BitmapIndexedNode" this, #_"IFn" f, #_"Object" r]
            (NodeSeq'kvreduce (:a this), f, r)
        )
    )

    #_method
    (defn- #_"BitmapIndexedNode" BitmapIndexedNode''ensureEditable [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit]
        (when-not (= (:edit this) edit) => this
            (let [#_"int" n (Integer/bitCount (:bitmap this)) #_"Object[]" a' (make-array Object (* 2 (inc n)))] ;; make room for next assoc
                (System/arraycopy (:a this), 0, a', 0, (* 2 n))
                (BitmapIndexedNode'new edit, (:bitmap this), a')
            )
        )
    )

    #_method
    (defn- #_"BitmapIndexedNode" BitmapIndexedNode''editAndSet-4 [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"Object" x]
        (let [#_"BitmapIndexedNode" e (BitmapIndexedNode''ensureEditable this, edit)]
            (aset (:a e) i x)
            e
        )
    )

    #_method
    (defn- #_"BitmapIndexedNode" BitmapIndexedNode''editAndSet-6 [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"Object" x, #_"int" j, #_"Object" y]
        (let [#_"BitmapIndexedNode" e (BitmapIndexedNode''ensureEditable this, edit)]
            (aset (:a e) i x)
            (aset (:a e) j y)
            e
        )
    )

    #_method
    (defn- #_"BitmapIndexedNode" BitmapIndexedNode''editAndRemovePair [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" bit, #_"int" i]
        (when-not (= (:bitmap this) bit)
            (let [#_"BitmapIndexedNode" e (-> (BitmapIndexedNode''ensureEditable this, edit) (update :bitmap bit-xor bit))
                  #_"Object[]" a (:a e) #_"int" n (alength a)]
                (System/arraycopy a, (* 2 (inc i)), a, (* 2 i), (- n (* 2 (inc i))))
                (aset a (- n 2) nil)
                (aset a (- n 1) nil)
                e
            )
        )
    )

    (defn- #_"INode" BitmapIndexedNode'createNode-7 [#_"AtomicReference<Thread>" edit, #_"int" shift, #_"Object" key1, #_"Object" val1, #_"int" key2hash, #_"Object" key2, #_"Object" val2]
        (let [#_"int" key1hash (Util'hasheq key1)]
            (when-not (= key1hash key2hash) => (HashCollisionNode'new nil, key1hash, 2, (object-array [ key1, val1, key2, val2 ]))
                (let [#_"boolean'" addedLeaf (volatile! false)]
                    (-> BitmapIndexedNode'EMPTY
                        (INode'''assocT edit, shift, key1hash, key1, val1, addedLeaf)
                        (INode'''assocT edit, shift, key2hash, key2, val2, addedLeaf)
                    )
                )
            )
        )
    )

    (extend-type BitmapIndexedNode INode
        (#_"INode" INode'''assocT [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"boolean'" addedLeaf]
            (let [#_"int" bit (PersistentHashMap'bitpos hash, shift) #_"int" idx (BitmapIndexedNode''index this, bit)]
                (if-not (zero? (bit-and (:bitmap this) bit))
                    (let [#_"Object" keyOrNull (aget (:a this) (* 2 idx))
                          #_"Object" valOrNode (aget (:a this) (inc (* 2 idx)))]
                        (cond
                            (nil? keyOrNull)
                                (let [#_"INode" node (INode'''assocT (cast cloiure.core.INode valOrNode), edit, (+ shift 5), hash, key, val, addedLeaf)]
                                    (when-not (= node valOrNode) => this
                                        (BitmapIndexedNode''editAndSet-4 this, edit, (inc (* 2 idx)), node)
                                    )
                                )
                            (= key keyOrNull)
                                (when-not (= val valOrNode) => this
                                    (BitmapIndexedNode''editAndSet-4 this, edit, (inc (* 2 idx)), val)
                                )
                            :else
                                (let [_ (vreset! addedLeaf true)]
                                    (BitmapIndexedNode''editAndSet-6 this, edit, (* 2 idx), nil, (inc (* 2 idx)), (BitmapIndexedNode'createNode-7 edit, (+ shift 5), keyOrNull, valOrNode, hash, key, val))
                                )
                        )
                    )
                    (let [#_"int" n (Integer/bitCount (:bitmap this))]
                        (cond
                            (< (* n 2) (alength (:a this)))
                                (let [_ (vreset! addedLeaf true)
                                      #_"BitmapIndexedNode" e (-> (BitmapIndexedNode''ensureEditable this, edit) (update :bitmap bit-or bit))]
                                    (System/arraycopy (:a e), (* 2 idx), (:a e), (* 2 (inc idx)), (* 2 (- n idx)))
                                    (aset (:a e) (* 2 idx) key)
                                    (aset (:a e) (inc (* 2 idx)) val)
                                    e
                                )
                            (<= 16 n)
                                (let [#_"INode[]" nodes (make-array #_"INode" Object 32) #_"int" jdx (PersistentHashMap'mask hash, shift)]
                                    (aset nodes jdx (INode'''assocT BitmapIndexedNode'EMPTY, edit, (+ shift 5), hash, key, val, addedLeaf))
                                    (loop-when [#_"int" j 0 #_"int" i 0] (< i 32)
                                        (when (odd? (unsigned-bit-shift-right (:bitmap this) i)) => (recur j (inc i))
                                            (if (some? (aget (:a this) j))
                                                (aset nodes i (INode'''assocT BitmapIndexedNode'EMPTY, edit, (+ shift 5), (Util'hasheq (aget (:a this) j)), (aget (:a this) j), (aget (:a this) (inc j)), addedLeaf))
                                                (aset nodes i (cast cloiure.core.INode (aget (:a this) (inc j))))
                                            )
                                            (recur (+ j 2) (inc i))
                                        )
                                    )
                                    (ArrayNode'new edit, (inc n), nodes)
                                )
                            :else
                                (let [#_"Object[]" a' (make-array Object (* 2 (+ n 4)))]
                                    (System/arraycopy (:a this), 0, a', 0, (* 2 idx))
                                    (aset a' (* 2 idx) key)
                                    (vreset! addedLeaf true)
                                    (aset a' (inc (* 2 idx)) val)
                                    (System/arraycopy (:a this), (* 2 idx), a', (* 2 (inc idx)), (* 2 (- n idx)))
                                    (-> (BitmapIndexedNode''ensureEditable this, edit)
                                        (assoc :a a')
                                        (update :bitmap bit-or bit)
                                    )
                                )
                        )
                    )
                )
            )
        )

        (#_"INode" INode'''dissocT [#_"BitmapIndexedNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"boolean'" removedLeaf]
            (let-when-not [#_"int" bit (PersistentHashMap'bitpos hash, shift)] (zero? (bit-and (:bitmap this) bit)) => this
                (let [#_"int" i (BitmapIndexedNode''index this, bit) #_"int" ii (* 2 i)
                      #_"Object" keyOrNull (aget (:a this) ii)
                      #_"Object" valOrNode (aget (:a this) (inc ii))]
                    (if (some? keyOrNull)
                        (when (= key keyOrNull) => this
                            (vreset! removedLeaf true)
                            ;; TODO: collapse
                            (BitmapIndexedNode''editAndRemovePair this, edit, bit, i)
                        )
                        (let [#_"INode" node (INode'''dissocT (cast cloiure.core.INode valOrNode), edit, (+ shift 5), hash, key, removedLeaf)]
                            (cond
                                (= node valOrNode)
                                    this
                                (some? node)
                                    (BitmapIndexedNode''editAndSet-4 this, edit, (inc ii), node)
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
)

(class-ns HashCollisionNode
    (defn #_"HashCollisionNode" HashCollisionNode'new [#_"AtomicReference<Thread>" edit, #_"int" hash, #_"int" n & #_"Object..." a]
        (merge (HashCollisionNode.)
            (hash-map
                #_"AtomicReference<Thread>" :edit edit
                #_"int" :hash hash
                #_"int" :n n
                #_"Object[]" :a a
            )
        )
    )

    #_method
    (defn #_"int" HashCollisionNode''findIndex [#_"HashCollisionNode" this, #_"Object" key]
        (let [#_"int" m (* 2 (:n this))]
            (loop-when [#_"int" i 0] (< i m) => -1
                (if (= key (aget (:a this) i)) i (recur (+ i 2)))
            )
        )
    )

    (extend-type HashCollisionNode INode
        (#_"INode" INode'''assoc [#_"HashCollisionNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"boolean'" addedLeaf]
            (if (= hash (:hash this))
                (let [#_"int" i (HashCollisionNode''findIndex this, key)]
                    (if (<= 0 i)
                        (when-not (= (aget (:a this) (inc i)) val) => this
                            (HashCollisionNode'new nil, hash, (:n this), (PersistentHashMap'cloneAndSet (:a this), (inc i), val))
                        )
                        (let [#_"int" n (:n this) #_"Object[]" a' (make-array Object (* 2 (inc n)))]
                            (System/arraycopy (:a this), 0, a', 0, (* 2 n))
                            (aset a' (* 2 n) key)
                            (aset a' (inc (* 2 n)) val)
                            (vreset! addedLeaf true)
                            (HashCollisionNode'new (:edit this), hash, (inc n), a')
                        )
                    )
                )
                ;; nest it in a bitmap node
                (let [#_"BitmapIndexedNode" node (BitmapIndexedNode'new nil, (PersistentHashMap'bitpos (:hash this), shift), (object-array [ nil, this ]))]
                    (INode'''assoc node, shift, hash, key, val, addedLeaf)
                )
            )
        )

        (#_"INode" INode'''dissoc [#_"HashCollisionNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
            (let-when [#_"int" i (HashCollisionNode''findIndex this, key)] (<= 0 i) => this
                (let-when [#_"int" n (:n this)] (< 1 n)
                    (HashCollisionNode'new nil, hash, (dec n), (PersistentHashMap'removePair (:a this), (quot i 2)))
                )
            )
        )

        (#_"IMapEntry|Object" INode'''find
            ([#_"HashCollisionNode" this, #_"int" shift, #_"int" hash, #_"Object" key]
                (let-when [#_"int" i (HashCollisionNode''findIndex this, key)] (<= 0 i)
                    (let-when [#_"Object" ai (aget (:a this) i)] (= key ai)
                        (MapEntry'create ai, (aget (:a this) (inc i)))
                    )
                )
            )
            ([#_"HashCollisionNode" this, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" notFound]
                (let-when [#_"int" i (HashCollisionNode''findIndex this, key)] (<= 0 i) => notFound
                    (when (= key (aget (:a this) i)) => notFound
                        (aget (:a this) (inc i))
                    )
                )
            )
        )

        (#_"ISeq" INode'''nodeSeq [#_"HashCollisionNode" this]
            (NodeSeq'create-1 (:a this))
        )

        (#_"Object" INode'''kvreduce [#_"HashCollisionNode" this, #_"IFn" f, #_"Object" r]
            (NodeSeq'kvreduce (:a this), f, r)
        )
    )

    #_method
    (defn- #_"HashCollisionNode" HashCollisionNode''ensureEditable-2 [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit]
        (when-not (= (:edit this) edit) => this
            (let [#_"int" n (:n this) #_"Object[]" a' (make-array Object (* 2 (inc n)))] ;; make room for next assoc
                (System/arraycopy (:a this), 0, a', 0, (* 2 n))
                (HashCollisionNode'new edit, (:hash this), n, a')
            )
        )
    )

    #_method
    (defn- #_"HashCollisionNode" HashCollisionNode''ensureEditable-4 [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" n, #_"Object[]" a]
        (if (= (:edit this) edit)
            (assoc this :a a :n n)
            (HashCollisionNode'new edit, (:hash this), n, a)
        )
    )

    #_method
    (defn- #_"HashCollisionNode" HashCollisionNode''editAndSet-4 [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"Object" x]
        (let [#_"HashCollisionNode" e (HashCollisionNode''ensureEditable-2 this, edit)]
            (aset (:a e) i x)
            e
        )
    )

    #_method
    (defn- #_"HashCollisionNode" HashCollisionNode''editAndSet-6 [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" i, #_"Object" x, #_"int" j, #_"Object" y]
        (let [#_"HashCollisionNode" e (HashCollisionNode''ensureEditable-2 this, edit)]
            (aset (:a e) i x)
            (aset (:a e) j y)
            e
        )
    )

    (extend-type HashCollisionNode INode
        (#_"INode" INode'''assocT [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"Object" val, #_"boolean'" addedLeaf]
            (if (= hash (:hash this))
                (let [#_"int" i (HashCollisionNode''findIndex this, key)]
                    (if (<= 0 i)
                        (when-not (= (aget (:a this) (inc i)) val) => this
                            (HashCollisionNode''editAndSet-4 this, edit, (inc i), val)
                        )
                        (let [#_"int" n (:n this) #_"int" m (alength (:a this))]
                            (if (< (* 2 n) m)
                                (let [_ (vreset! addedLeaf true)]
                                    (-> (HashCollisionNode''editAndSet-6 this, edit, (* 2 n), key, (inc (* 2 n)), val)
                                        (update :n inc)
                                    )
                                )
                                (let [#_"Object[]" a' (make-array Object (+ m 2))]
                                    (System/arraycopy (:a this), 0, a', 0, m)
                                    (aset a' m key)
                                    (aset a' (inc m) val)
                                    (vreset! addedLeaf true)
                                    (HashCollisionNode''ensureEditable-4 this, edit, (inc n), a')
                                )
                            )
                        )
                    )
                )
                ;; nest it in a bitmap node
                (let [#_"BitmapIndexedNode" node (BitmapIndexedNode'new edit, (PersistentHashMap'bitpos (:hash this), shift), (object-array [ nil, this, nil, nil ]))]
                    (INode'''assocT node, edit, shift, hash, key, val, addedLeaf)
                )
            )
        )

        (#_"INode" INode'''dissocT [#_"HashCollisionNode" this, #_"AtomicReference<Thread>" edit, #_"int" shift, #_"int" hash, #_"Object" key, #_"boolean'" removedLeaf]
            (let-when [#_"int" i (HashCollisionNode''findIndex this, key)] (<= 0 i) => this
                (vreset! removedLeaf true)
                (let-when [#_"int" n (:n this)] (< 1 n)
                    (let [#_"HashCollisionNode" e (-> (HashCollisionNode''ensureEditable-2 this, edit) (update :n dec))
                          #_"int" m (* 2 n)]
                        (aset (:a e) i (aget (:a e) (- m 2)))
                        (aset (:a e) (inc i) (aget (:a e) (- m 1)))
                        (aset (:a e) (- m 2) nil)
                        (aset (:a e) (- m 1) nil)
                        e
                    )
                )
            )
        )
    )
)

(class-ns TransientHashMap
    (defn #_"TransientHashMap" TransientHashMap'new
        ([#_"PersistentHashMap" m]
            (TransientHashMap'new (AtomicReference. (Thread/currentThread)), (:root m), (:n m), (:hasNull m), (:nullValue m))
        )
        ([#_"AtomicReference<Thread>" edit, #_"INode" root, #_"int" n, #_"boolean" hasNull, #_"Object" nullValue]
            (merge (TransientHashMap.) (ATransientMap'new)
                (hash-map
                    #_"AtomicReference<Thread>" :edit edit
                    #_"INode" :root root
                    #_"int" :n n
                    #_"boolean" :hasNull hasNull
                    #_"Object" :nullValue nullValue
                )
            )
        )
    )

    #_override
    (defn #_"ITransientMap" ATransientMap'''doAssoc--TransientHashMap [#_"TransientHashMap" this, #_"Object" key, #_"Object" val]
        (if (nil? key)
            (let [this (if (= (:nullValue this) val) this (assoc this :nullValue val))]
                (when-not (:hasNull this) => this
                    (-> this (update :n inc) (assoc :hasNull true))
                )
            )
            (let [#_"boolean'" addedLeaf (volatile! false)
                  #_"INode" node (INode'''assocT (or (:root this) BitmapIndexedNode'EMPTY), (:edit this), 0, (Util'hasheq key), key, val, addedLeaf)
                  this (if (= (:root this) node) this (assoc this :root node))]
                (when @addedLeaf => this
                    (update this :n inc)
                )
            )
        )
    )

    #_override
    (defn #_"ITransientMap" ATransientMap'''doDissoc--TransientHashMap [#_"TransientHashMap" this, #_"Object" key]
        (if (nil? key)
            (when (:hasNull this) => this
                (-> this (assoc :hasNull false :nullValue nil) (update :n dec))
            )
            (when (some? (:root this)) => this
                (let [#_"boolean'" removedLeaf (volatile! false)
                      #_"INode" node (INode'''dissocT (:root this), (:edit this), 0, (Util'hasheq key), key, removedLeaf)
                      this (if (= (:root this) node) this (assoc this :root node))]
                    (when @removedLeaf => this
                        (update this :n dec)
                    )
                )
            )
        )
    )

    (declare PersistentHashMap'new)

    #_override
    (defn #_"IPersistentMap" ATransientMap'''doPersistent--TransientHashMap [#_"TransientHashMap" this]
        (.set (:edit this), nil)
        (PersistentHashMap'new (:n this), (:root this), (:hasNull this), (:nullValue this))
    )

    #_override
    (defn #_"Object" ATransientMap'''doValAt--TransientHashMap [#_"TransientHashMap" this, #_"Object" key, #_"Object" notFound]
        (if (nil? key)
            (when (:hasNull this) => notFound
                (:nullValue this)
            )
            (when (some? (:root this)) => notFound
                (INode'''find (:root this), 0, (Util'hasheq key), key, notFound)
            )
        )
    )

    #_override
    (defn #_"int" ATransientMap'''doCount--TransientHashMap [#_"TransientHashMap" this]
        (:n this)
    )

    #_override
    (defn #_"void" ATransientMap'''ensureEditable--TransientHashMap [#_"TransientHashMap" this]
        (when (nil? (.get (:edit this)))
            (throw! "transient used after persistent! call")
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
    (defn #_"PersistentHashMap" PersistentHashMap'new
        ([#_"int" n, #_"INode" root, #_"boolean" hasNull, #_"Object" nullValue] (PersistentHashMap'new nil, n, root, hasNull, nullValue))
        ([#_"IPersistentMap" meta, #_"int" n, #_"INode" root, #_"boolean" hasNull, #_"Object" nullValue]
            (merge (PersistentHashMap.) (APersistentMap'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"int" :n n
                    #_"INode" :root root
                    #_"boolean" :hasNull hasNull
                    #_"Object" :nullValue nullValue
                )
            )
        )
    )

    (extend-type PersistentHashMap IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"PersistentHashMap" this]
            (:_meta this)
        )
    )

    (extend-type PersistentHashMap IObj
        (#_"PersistentHashMap" IObj'''withMeta [#_"PersistentHashMap" this, #_"IPersistentMap" meta]
            (PersistentHashMap'new meta, (:n this), (:root this), (:hasNull this), (:nullValue this))
        )
    )

    (def #_"PersistentHashMap" PersistentHashMap'EMPTY (PersistentHashMap'new 0, nil, false, nil))

    (defn #_"PersistentHashMap" PersistentHashMap'create-1a [& #_"Object..." a]
        (loop-when-recur [#_"ITransientMap" m (transient PersistentHashMap'EMPTY) #_"int" i 0]
                         (< i (alength a))
                         [(assoc! m (aget a i) (aget a (inc i))) (+ i 2)]
                      => (persistent! m)
        )
    )

    (defn #_"PersistentHashMap" PersistentHashMap'create-1s [#_"Seqable" keyvals]
        (let [#_"ITransientMap" m (transient PersistentHashMap'EMPTY)
              m (loop-when [m m #_"ISeq" s (seq keyvals)] (some? s) => m
                    (when (some? (next s)) => (throw! (str "no value supplied for key: " (first s)))
                        (recur (assoc! m (first s) (second s)) (next (next s)))
                    )
                )]
            (persistent! m)
        )
    )

    (defn #_"PersistentHashMap" PersistentHashMap'createWithCheck-1a [& #_"Object..." a]
        (let [#_"ITransientMap" m (transient PersistentHashMap'EMPTY)
              m (loop-when [m m #_"int" i 0] (< i (alength a)) => m
                    (let [m (assoc! m (aget a i) (aget a (inc i)))]
                        (when (= (count m) (inc (quot i 2))) => (throw! (str "duplicate key: " (aget a i)))
                            (recur m (+ i 2))
                        )
                    )
                )]
            (persistent! m)
        )
    )

    (defn #_"PersistentHashMap" PersistentHashMap'createWithCheck-1s [#_"Seqable" keyvals]
        (let [#_"ITransientMap" m (transient PersistentHashMap'EMPTY)
              m (loop-when [m m #_"ISeq" s (seq keyvals) #_"int" i 0] (some? s) => m
                    (when (some? (next s)) => (throw! (str "no value supplied for key: " (first s)))
                        (let [m (assoc! m (first s) (second s))]
                            (when (= (count m) (inc i)) => (throw! (str "duplicate key: " (first s)))
                                (recur m (next (next s)) (inc i))
                            )
                        )
                    )
                )]
            (persistent! m)
        )
    )

    (def- #_"Object" PersistentHashMap'NOT_FOUND (Object.))

    (extend-type PersistentHashMap Associative
        (#_"boolean" Associative'''containsKey [#_"PersistentHashMap" this, #_"Object" key]
            (if (nil? key)
                (:hasNull this)
                (and (some? (:root this))
                    (not (identical? (INode'''find (:root this), 0, (Util'hasheq key), key, PersistentHashMap'NOT_FOUND) PersistentHashMap'NOT_FOUND))
                )
            )
        )

        (#_"IMapEntry" Associative'''entryAt [#_"PersistentHashMap" this, #_"Object" key]
            (if (nil? key)
                (when (:hasNull this)
                    (MapEntry'create nil, (:nullValue this))
                )
                (when (some? (:root this))
                    (INode'''find (:root this), 0, (Util'hasheq key), key)
                )
            )
        )
    )

    (extend-type PersistentHashMap Associative
        (#_"IPersistentMap" Associative'''assoc [#_"PersistentHashMap" this, #_"Object" key, #_"Object" val]
            (if (nil? key)
                (when-not (and (:hasNull this) (= val (:nullValue this))) => this
                    (PersistentHashMap'new (meta this), (+ (:n this) (if (:hasNull this) 0 1)), (:root this), true, val)
                )
                (let [#_"boolean'" addedLeaf (volatile! false)
                      #_"INode" newroot (INode'''assoc (or (:root this) BitmapIndexedNode'EMPTY), 0, (Util'hasheq key), key, val, addedLeaf)]
                    (when-not (= newroot (:root this)) => this
                        (PersistentHashMap'new (meta this), (+ (:n this) (if @addedLeaf 1 0)), newroot, (:hasNull this), (:nullValue this))
                    )
                )
            )
        )
    )

    (extend-type PersistentHashMap ILookup
        (#_"Object" ILookup'''valAt
            ([#_"PersistentHashMap" this, #_"Object" key] (ILookup'''valAt this, key, nil))
            ([#_"PersistentHashMap" this, #_"Object" key, #_"Object" notFound]
                (if (nil? key)
                    (when (:hasNull this) => notFound
                        (:nullValue this)
                    )
                    (when (some? (:root this)) => notFound
                        (INode'''find (:root this), 0, (Util'hasheq key), key, notFound)
                    )
                )
            )
        )
    )

    (extend-type PersistentHashMap IPersistentMap
        (#_"IPersistentMap" IPersistentMap'''dissoc [#_"PersistentHashMap" this, #_"Object" key]
            (cond
                (nil? key)
                    (if (:hasNull this) (PersistentHashMap'new (meta this), (dec (:n this)), (:root this), false, nil) this)
                (nil? (:root this))
                    this
                :else
                    (let [#_"INode" newroot (INode'''dissoc (:root this), 0, (Util'hasheq key), key)]
                        (when-not (= newroot (:root this)) => this
                            (PersistentHashMap'new (meta this), (dec (:n this)), newroot, (:hasNull this), (:nullValue this))
                        )
                    )
            )
        )
    )

    (extend-type PersistentHashMap IKVReduce
        (#_"Object" IKVReduce'''kvreduce [#_"PersistentHashMap" this, #_"IFn" f, #_"Object" r]
            (let [r (if (:hasNull this) (f r nil (:nullValue this)) r)]
                (when-not (reduced? r) => @r
                    (when (some? (:root this)) => r
                        (let [r (INode'''kvreduce (:root this), f, r)]
                            (when-not (reduced? r) => @r
                                r
                            )
                        )
                    )
                )
            )
        )
    )

    (extend-type PersistentHashMap Counted
        (#_"int" Counted'''count [#_"PersistentHashMap" this]
            (:n this)
        )
    )

    (extend-type PersistentHashMap Seqable
        (#_"ISeq" Seqable'''seq [#_"PersistentHashMap" this]
            (let [#_"ISeq" s (when (some? (:root this)) (INode'''nodeSeq (:root this)))]
                (if (:hasNull this) (Cons'new (MapEntry'create nil, (:nullValue this)), s) s)
            )
        )
    )

    (extend-type PersistentHashMap IPersistentCollection
        (#_"IPersistentCollection" IPersistentCollection'''empty [#_"PersistentHashMap" this]
            (with-meta PersistentHashMap'EMPTY (meta this))
        )
    )

    (extend-type PersistentHashMap IEditableCollection
        (#_"TransientHashMap" IEditableCollection'''asTransient [#_"PersistentHashMap" this]
            (TransientHashMap'new this)
        )
    )
)
)

(java-ns cloiure.lang.PersistentHashSet

(class-ns TransientHashSet
    (defn #_"TransientHashSet" TransientHashSet'new [#_"ITransientMap" impl]
        (merge (TransientHashSet.) (ATransientSet'new impl))
    )

    (declare PersistentHashSet'new)

    (extend-type TransientHashSet ITransientCollection
        (#_"PersistentHashSet" ITransientCollection'''persistent [#_"TransientHashSet" this]
            (PersistentHashSet'new nil, (persistent! (:impl this)))
        )
    )
)

(class-ns PersistentHashSet
    (defn #_"PersistentHashSet" PersistentHashSet'new [#_"IPersistentMap" meta, #_"IPersistentMap" impl]
        (merge (PersistentHashSet.) (APersistentSet'new impl)
            (hash-map
                #_"IPersistentMap" :_meta meta
            )
        )
    )

    (extend-type PersistentHashSet IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"PersistentHashSet" this]
            (:_meta this)
        )
    )

    (extend-type PersistentHashSet IObj
        (#_"PersistentHashSet" IObj'''withMeta [#_"PersistentHashSet" this, #_"IPersistentMap" meta]
            (PersistentHashSet'new meta, (:impl this))
        )
    )

    (def #_"PersistentHashSet" PersistentHashSet'EMPTY (PersistentHashSet'new nil, PersistentHashMap'EMPTY))

    (defn #_"PersistentHashSet" PersistentHashSet'create-1a [& #_"Object..." items]
        (loop-when-recur [#_"ITransientSet" s (transient PersistentHashSet'EMPTY) #_"int" i 0]
                         (< i (alength items))
                         [(conj! s (aget items i)) (inc i)]
                      => (persistent! s)
        )
    )

    (defn #_"PersistentHashSet" PersistentHashSet'create-1s [#_"Seqable" items]
        (loop-when-recur [#_"ITransientSet" s (transient PersistentHashSet'EMPTY) #_"ISeq" q (seq items)]
                         (some? q)
                         [(conj! s (first q)) (next q)]
                      => (persistent! s)
        )
    )

    (defn #_"PersistentHashSet" PersistentHashSet'createWithCheck-1a [& #_"Object..." items]
        (let [#_"ITransientSet" s (transient PersistentHashSet'EMPTY)
              s (loop-when [s s #_"int" i 0] (< i (alength items)) => s
                    (let [s (conj! s (aget items i))]
                        (when (= (count s) (inc i)) => (throw! (str "duplicate key: " (aget items i)))
                            (recur s (inc i))
                        )
                    )
                )]
            (persistent! s)
        )
    )

    (defn #_"PersistentHashSet" PersistentHashSet'createWithCheck-1s [#_"Seqable" items]
        (let [#_"ITransientSet" s (transient PersistentHashSet'EMPTY)
              s (loop-when [s s #_"ISeq" q (seq items) #_"int" i 0] (some? q) => s
                    (let [#_"Object" key (first q) s (conj! s key)]
                        (when (= (count s) (inc i)) => (throw! (str "duplicate key: " key))
                            (recur s (next q) (inc i))
                        )
                    )
                )]
            (persistent! s)
        )
    )

    (extend-type PersistentHashSet IPersistentSet
        (#_"IPersistentSet" IPersistentSet'''disj [#_"PersistentHashSet" this, #_"Object" key]
            (if (contains? this key)
                (PersistentHashSet'new (meta this), (dissoc (:impl this) key))
                this
            )
        )
    )

    (extend-type PersistentHashSet IPersistentCollection
        (#_"PersistentHashSet" IPersistentCollection'''conj [#_"PersistentHashSet" this, #_"Object" o]
            (if (contains? this o)
                this
                (PersistentHashSet'new (meta this), (assoc (:impl this) o o))
            )
        )

        (#_"PersistentHashSet" IPersistentCollection'''empty [#_"PersistentHashSet" this]
            (with-meta PersistentHashSet'EMPTY (meta this))
        )
    )

    (extend-type PersistentHashSet IEditableCollection
        (#_"ITransientCollection" IEditableCollection'''asTransient [#_"PersistentHashSet" this]
            (TransientHashSet'new (transient (:impl this)))
        )
    )
)
)

(java-ns cloiure.lang.PersistentList

(class-ns Primordial
    (defn #_"Primordial" Primordial'new []
        (merge (Primordial.) (RestFn'new))
    )

    (extend-type Primordial IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"Primordial" this]
            nil
        )
    )

    (extend-type Primordial IObj
        (#_"Primordial" IObj'''withMeta [#_"Primordial" this, #_"IPersistentMap" meta]
            (throw! "unsupported operation")
        )
    )

    #_override
    (defn #_"int" RestFn'''getRequiredArity--Primordial [#_"Primordial" this]
        0
    )

    (declare PersistentList'EMPTY)
    (declare PersistentList'create)

    #_override
    (defn #_"Object" RestFn'''doInvoke-2--Primordial [#_"Primordial" this, #_"Object" args]
        (if (instance? ArraySeq args)
            (let [#_"Object[]" a (:a args) #_"int" i0 (:i args)]
                (loop-when-recur [#_"IPersistentList" l PersistentList'EMPTY #_"int" i (dec (alength a))]
                                 (<= i0 i)
                                 [(conj l (aget a i)) (dec i)]
                              => l
                )
            )
            (PersistentList'create (RT'seqToArray (seq args)))
        )
    )

    (defn #_"Object" Primordial'invokeStatic [#_"ISeq" args]
        (if (instance? ArraySeq args)
            (let [#_"Object[]" a (:a args)]
                (loop-when-recur [#_"IPersistentList" l PersistentList'EMPTY #_"int" i (dec (alength a))]
                                 (<= 0 i)
                                 [(conj l (aget a i)) (dec i)]
                              => l
                )
            )
            (PersistentList'create (RT'seqToArray (seq args)))
        )
    )
)

(class-ns EmptyList
    (def #_"int" EmptyList'HASHEQ (§ soon Murmur3'hashOrdered nil))

    (defn #_"EmptyList" EmptyList'new [#_"IPersistentMap" meta]
        (merge (EmptyList.)
            (hash-map
                #_"IPersistentMap" :_meta meta
            )
        )
    )

    (extend-type EmptyList IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"EmptyList" this]
            (:_meta this)
        )
    )

    (extend-type EmptyList IObj
        (#_"EmptyList" IObj'''withMeta [#_"EmptyList" this, #_"IPersistentMap" meta]
            (when-not (= meta (meta this)) => this
                (EmptyList'new meta)
            )
        )
    )

    (extend-type EmptyList IHashEq
        (#_"int" IHashEq'''hasheq [#_"EmptyList" this]
            EmptyList'HASHEQ
        )
    )

    (extend-type EmptyList IObject
        (#_"boolean" IObject'''equals [#_"EmptyList" this, #_"Object" that]
            (and (sequential? that) (nil? (seq that)))
        )

        (#_"int" IObject'''hashCode [#_"EmptyList" this]
            1
        )

        (#_"String" IObject'''toString [#_"EmptyList" this]
            "()"
        )
    )

    (extend-type EmptyList ISeq
        (#_"Object" ISeq'''first [#_"EmptyList" this]
            nil
        )

        (#_"ISeq" ISeq'''next [#_"EmptyList" this]
            nil
        )
    )

    (declare PersistentList'new)

    (extend-type EmptyList IPersistentCollection
        (#_"PersistentList" IPersistentCollection'''conj [#_"EmptyList" this, #_"Object" o]
            (PersistentList'new (meta this), o, nil, 1)
        )

        (#_"EmptyList" IPersistentCollection'''empty [#_"EmptyList" this]
            this
        )
    )

    (extend-type EmptyList IPersistentStack
        (#_"Object" IPersistentStack'''peek [#_"EmptyList" this]
            nil
        )

        (#_"IPersistentList" IPersistentStack'''pop [#_"EmptyList" this]
            (throw! "can't pop empty list")
        )
    )

    (extend-type EmptyList Counted
        (#_"int" Counted'''count [#_"EmptyList" this]
            0
        )
    )

    (extend-type EmptyList Seqable
        (#_"ISeq" Seqable'''seq [#_"EmptyList" this]
            nil
        )
    )
)

(class-ns PersistentList
    (def #_"IFn" PersistentList'creator (§ soon Primordial'new))

    (def #_"EmptyList" PersistentList'EMPTY (§ soon EmptyList'new nil))

    (defn #_"PersistentList" PersistentList'new
        ([#_"Object" _first] (PersistentList'new nil, _first, nil, 1))
        ([#_"IPersistentMap" meta, #_"Object" _first, #_"IPersistentList" _rest, #_"int" _count]
            (merge (PersistentList.) (ASeq'new meta)
                (hash-map
                    #_"Object" :_first _first
                    #_"IPersistentList" :_rest _rest
                    #_"int" :_count _count
                )
            )
        )
    )

    (extend-type PersistentList IObj
        (#_"PersistentList" IObj'''withMeta [#_"PersistentList" this, #_"IPersistentMap" meta]
            (when-not (= meta (:_meta this)) => this
                (PersistentList'new meta, (:_first this), (:_rest this), (:_count this))
            )
        )
    )

    (defn #_"IPersistentList" PersistentList'create [#_"Object[]" a]
        (loop-when-recur [#_"IPersistentList" l PersistentList'EMPTY #_"int" i (dec (alength a))]
                         (<= 0 i)
                         [(conj l (aget a i)) (dec i)]
                      => l
        )
    )

    (extend-type PersistentList ISeq
        (#_"Object" ISeq'''first [#_"PersistentList" this]
            (:_first this)
        )

        (#_"ISeq" ISeq'''next [#_"PersistentList" this]
            (when-not (= (:_count this) 1)
                (:_rest this)
            )
        )
    )

    (extend-type PersistentList IPersistentStack
        (#_"Object" IPersistentStack'''peek [#_"PersistentList" this]
            (first this)
        )

        (#_"IPersistentList" IPersistentStack'''pop [#_"PersistentList" this]
            (or (:_rest this) (with-meta PersistentList'EMPTY (:_meta this)))
        )
    )

    (extend-type PersistentList Counted
        (#_"int" Counted'''count [#_"PersistentList" this]
            (:_count this)
        )
    )

    (extend-type PersistentList IPersistentCollection
        (#_"PersistentList" IPersistentCollection'''conj [#_"PersistentList" this, #_"Object" o]
            (PersistentList'new (meta this), o, this, (inc (:_count this)))
        )

        (#_"PersistentList" IPersistentCollection'''empty [#_"PersistentList" this]
            (with-meta PersistentList'EMPTY (meta this))
        )
    )

    (extend-type PersistentList IReduce
        (#_"Object" IReduce'''reduce
            ([#_"PersistentList" this, #_"IFn" f]
                (loop-when [#_"Object" r (first this) #_"ISeq" s (next this)] (some? s) => r
                    (let [r (f r (first s))]
                        (if (reduced? r) @r (recur r (next s)))
                    )
                )
            )
            ([#_"PersistentList" this, #_"IFn" f, #_"Object" r]
                (loop-when [r (f r (first this)) #_"ISeq" s (next this)] (some? s) => (if (reduced? r) @r r)
                    (if (reduced? r) @r (recur (f r (first s)) (next s)))
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.PersistentQueue

(class-ns QSeq
    (defn #_"QSeq" QSeq'new
        ([#_"ISeq" f, #_"ISeq" rseq] (QSeq'new nil, f, rseq))
        ([#_"IPersistentMap" meta, #_"ISeq" f, #_"ISeq" rseq]
            (merge (QSeq.) (ASeq'new meta)
                (hash-map
                    #_"ISeq" :f f
                    #_"ISeq" :rseq rseq
                )
            )
        )
    )

    (extend-type QSeq IObj
        (#_"QSeq" IObj'''withMeta [#_"QSeq" this, #_"IPersistentMap" meta]
            (QSeq'new meta, (:f this), (:rseq this))
        )
    )

    (extend-type QSeq ISeq
        (#_"Object" ISeq'''first [#_"QSeq" this]
            (first (:f this))
        )

        (#_"ISeq" ISeq'''next [#_"QSeq" this]
            (let [#_"ISeq" f (next (:f this)) #_"ISeq" r (:rseq this)]
                (cond
                    (some? f) (QSeq'new f, r)
                    (some? r) (QSeq'new r, nil)
                )
            )
        )
    )

    (extend-type QSeq Counted
        (#_"int" Counted'''count [#_"QSeq" this]
            (+ (count (:f this)) (count (:rseq this)))
        )
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
        (merge (PersistentQueue.)
            (hash-map
                #_"IPersistentMap" :_meta meta
                #_"int" :cnt cnt
                #_"ISeq" :f f
                #_"PersistentVector" :r r
            )
        )
    )

    (extend-type PersistentQueue IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"PersistentQueue" this]
            (:_meta this)
        )
    )

    (extend-type PersistentQueue IObj
        (#_"PersistentQueue" IObj'''withMeta [#_"PersistentQueue" this, #_"IPersistentMap" meta]
            (PersistentQueue'new meta, (:cnt this), (:f this), (:r this))
        )
    )

    (def #_"PersistentQueue" PersistentQueue'EMPTY (PersistentQueue'new nil, 0, nil, nil))

    (extend-type PersistentQueue IObject
        (#_"boolean" IObject'''equals [#_"PersistentQueue" this, #_"Object" that]
            (or (identical? this that)
                (and (sequential? that)
                    (loop-when [#_"ISeq" s (seq this) #_"ISeq" z (seq that)] (some? s) => (nil? z)
                        (and (some? z) (= (first s) (first z))
                            (recur (next s) (next z))
                        )
                    )
                )
            )
        )

        (#_"int" IObject'''hashCode [#_"PersistentQueue" this]
            (loop-when [#_"int" hash 1 #_"ISeq" s (seq this)] (some? s) => hash
                (recur (+ (* 31 hash) (if (some? (first s)) (IObject'''hashCode (first s)) 0)) (next s))
            )
        )
    )

    (extend-type PersistentQueue IHashEq
        (#_"int" IHashEq'''hasheq [#_"PersistentQueue" this]
            (Murmur3'hashOrdered this)
        )
    )

    (extend-type PersistentQueue IPersistentStack
        (#_"Object" IPersistentStack'''peek [#_"PersistentQueue" this]
            (first (:f this))
        )

        (#_"PersistentQueue" IPersistentStack'''pop [#_"PersistentQueue" this]
            (when (some? (:f this)) => this ;; hmmm... pop of empty queue -> empty queue?
                (let [#_"ISeq" f (next (:f this)) #_"PersistentVector" r (:r this)
                      [f r]
                        (when (nil? f) => [f r]
                            [(seq r) nil]
                        )]
                    (PersistentQueue'new (meta this), (dec (:cnt this)), f, r)
                )
            )
        )
    )

    (extend-type PersistentQueue Counted
        (#_"int" Counted'''count [#_"PersistentQueue" this]
            (:cnt this)
        )
    )

    (extend-type PersistentQueue Seqable
        (#_"ISeq" Seqable'''seq [#_"PersistentQueue" this]
            (when (some? (:f this))
                (QSeq'new (:f this), (seq (:r this)))
            )
        )
    )

    (extend-type PersistentQueue IPersistentCollection
        (#_"PersistentQueue" IPersistentCollection'''conj [#_"PersistentQueue" this, #_"Object" o]
            (let [[#_"ISeq" f #_"PersistentVector" r]
                    (if (nil? (:f this)) ;; empty
                        [(list o) nil]
                        [(:f this) (conj (or (:r this) []) o)]
                    )]
                (PersistentQueue'new (meta this), (inc (:cnt this)), f, r)
            )
        )

        (#_"PersistentQueue" IPersistentCollection'''empty [#_"PersistentQueue" this]
            (with-meta PersistentQueue'EMPTY (meta this))
        )
    )
)
)

(java-ns cloiure.lang.PersistentTreeMap

(class-ns TNode
    (defn #_"TNode" TNode'new [#_"Object" key]
        (merge (TNode.) (AMapEntry'new)
            (hash-map
                #_"Object" :key key
            )
        )
    )

    (extend-type TNode IMapEntry
        (#_"Object" IMapEntry'''key [#_"TNode" this]
            (:key this)
        )

        (#_"Object" IMapEntry'''val [#_"TNode" this]
            nil
        )
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
        (PersistentTreeMap'black (:key parent), (IMapEntry'''val parent), this, (TNode'''right parent))
    )

    #_override
    (defn #_"TNode" TNode'''balanceRight--TNode [#_"TNode" this, #_"TNode" parent]
        (PersistentTreeMap'black (:key parent), (IMapEntry'''val parent), (TNode'''left parent), this)
    )

    (extend-type TNode IKVReduce
        (#_"Object" IKVReduce'''kvreduce [#_"TNode" this, #_"IFn" f, #_"Object" r]
            (or
                (when (some? (TNode'''left this))
                    (let [r (INode'''kvreduce (TNode'''left this), f, r)]
                        (when (reduced? r)
                            r
                        )
                    )
                )
                (let [r (f r (key this) (val this))]
                    (cond
                        (reduced? r)          r
                        (some? (TNode'''right this)) (INode'''kvreduce (TNode'''right this), f, r)
                        :else                 r
                    )
                )
            )
        )
    )
)

(class-ns Black
    (defn #_"Black" Black'new [#_"Object" key]
        (merge (Black.) (TNode'new key))
    )

    #_override
    (defn #_"TNode" TNode'''addLeft--Black [#_"Black" this, #_"TNode" ins]
        (TNode'''balanceLeft ins, this)
    )

    #_override
    (defn #_"TNode" TNode'''addRight--Black [#_"Black" this, #_"TNode" ins]
        (TNode'''balanceRight ins, this)
    )

    (declare PersistentTreeMap'balanceLeftDel)

    #_override
    (defn #_"TNode" TNode'''removeLeft--Black [#_"Black" this, #_"TNode" del]
        (PersistentTreeMap'balanceLeftDel (:key this), (IMapEntry'''val this), del, (TNode'''right this))
    )

    (declare PersistentTreeMap'balanceRightDel)

    #_override
    (defn #_"TNode" TNode'''removeRight--Black [#_"Black" this, #_"TNode" del]
        (PersistentTreeMap'balanceRightDel (:key this), (IMapEntry'''val this), (TNode'''left this), del)
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
        (merge (BlackVal.) (Black'new key)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    (extend-type BlackVal IMapEntry
        (#_"Object" IMapEntry'''val [#_"BlackVal" this]
            (:val this)
        )
    )

    (declare RedVal'new)

    #_override
    (defn #_"TNode" TNode'''redden--BlackVal [#_"BlackVal" this]
        (RedVal'new (:key this), (:val this))
    )
)

(class-ns BlackBranch
    (defn #_"BlackBranch" BlackBranch'new [#_"Object" key, #_"TNode" left, #_"TNode" right]
        (merge (BlackBranch.) (Black'new key)
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
        (merge (BlackBranchVal.) (BlackBranch'new key, left, right)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    (extend-type BlackBranchVal IMapEntry
        (#_"Object" IMapEntry'''val [#_"BlackBranchVal" this]
            (:val this)
        )
    )

    (declare RedBranchVal'new)

    #_override
    (defn #_"TNode" TNode'''redden--BlackBranchVal [#_"BlackBranchVal" this]
        (RedBranchVal'new (:key this), (:val this), (:left this), (:right this))
    )
)

(class-ns Red
    (defn #_"Red" Red'new [#_"Object" key]
        (merge (Red.) (TNode'new key))
    )

    (declare PersistentTreeMap'red)

    #_override
    (defn #_"TNode" TNode'''addLeft--Red [#_"Red" this, #_"TNode" ins]
        (PersistentTreeMap'red (:key this), (IMapEntry'''val this), ins, (TNode'''right this))
    )

    #_override
    (defn #_"TNode" TNode'''addRight--Red [#_"Red" this, #_"TNode" ins]
        (PersistentTreeMap'red (:key this), (IMapEntry'''val this), (TNode'''left this), ins)
    )

    #_override
    (defn #_"TNode" TNode'''removeLeft--Red [#_"Red" this, #_"TNode" del]
        (PersistentTreeMap'red (:key this), (IMapEntry'''val this), del, (TNode'''right this))
    )

    #_override
    (defn #_"TNode" TNode'''removeRight--Red [#_"Red" this, #_"TNode" del]
        (PersistentTreeMap'red (:key this), (IMapEntry'''val this), (TNode'''left this), del)
    )

    #_override
    (defn #_"TNode" TNode'''blacken--Red [#_"Red" this]
        (Black'new (:key this))
    )

    #_override
    (defn #_"TNode" TNode'''redden--Red [#_"Red" this]
        (throw! "invariant violation")
    )

    #_override
    (defn #_"TNode" TNode'''replace--Red [#_"Red" this, #_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" right]
        (PersistentTreeMap'red key, val, left, right)
    )
)

(class-ns RedVal
    (defn #_"RedVal" RedVal'new [#_"Object" key, #_"Object" val]
        (merge (RedVal.) (Red'new key)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    (extend-type RedVal IMapEntry
        (#_"Object" IMapEntry'''val [#_"RedVal" this]
            (:val this)
        )
    )

    #_override
    (defn #_"TNode" TNode'''blacken--RedVal [#_"RedVal" this]
        (BlackVal'new (:key this), (:val this))
    )
)

(class-ns RedBranch
    (defn #_"RedBranch" RedBranch'new [#_"Object" key, #_"TNode" left, #_"TNode" right]
        (merge (RedBranch.) (Red'new key)
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
                (PersistentTreeMap'red (:key this), (IMapEntry'''val this), (TNode'''blacken (:left this)), (PersistentTreeMap'black (:key parent), (IMapEntry'''val parent), (:right this), (TNode'''right parent)))
            )
            (instance? Red (:right this))
            (do
                (PersistentTreeMap'red (:key (:right this)), (IMapEntry'''val (:right this)), (PersistentTreeMap'black (:key this), (IMapEntry'''val this), (:left this), (TNode'''left (:right this))), (PersistentTreeMap'black (:key parent), (IMapEntry'''val parent), (TNode'''right (:right this)), (TNode'''right parent)))
            )
            :else
            (do
                (TNode'''balanceLeft (§ super ), parent)
            )
        )
    )

    #_override
    (defn #_"TNode" TNode'''balanceRight--RedBranch [#_"RedBranch" this, #_"TNode" parent]
        (cond (instance? Red (:right this))
            (do
                (PersistentTreeMap'red (:key this), (IMapEntry'''val this), (PersistentTreeMap'black (:key parent), (IMapEntry'''val parent), (TNode'''left parent), (:left this)), (TNode'''blacken (:right this)))
            )
            (instance? Red (:left this))
            (do
                (PersistentTreeMap'red (:key (:left this)), (IMapEntry'''val (:left this)), (PersistentTreeMap'black (:key parent), (IMapEntry'''val parent), (TNode'''left parent), (TNode'''left (:left this))), (PersistentTreeMap'black (:key this), (IMapEntry'''val this), (TNode'''right (:left this)), (:right this)))
            )
            :else
            (do
                (TNode'''balanceRight (§ super ), parent)
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
        (merge (RedBranchVal.) (RedBranch'new key, left, right)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    (extend-type RedBranchVal IMapEntry
        (#_"Object" IMapEntry'''val [#_"RedBranchVal" this]
            (:val this)
        )
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
            (merge (TSeq.) (ASeq'new meta)
                (hash-map
                    #_"ISeq" :stack stack
                    #_"boolean" :asc asc
                    #_"int" :cnt cnt
                )
            )
        )
    )

    (extend-type TSeq IObj
        (#_"TSeq" IObj'''withMeta [#_"TSeq" this, #_"IPersistentMap" meta]
            (TSeq'new meta, (:stack this), (:asc this), (:cnt this))
        )
    )

    (defn #_"ISeq" TSeq'push [#_"TNode" t, #_"ISeq" stack, #_"boolean" asc]
        (loop-when [stack stack t t] (some? t) => stack
            (recur (cons t stack) (if asc (TNode'''left t) (TNode'''right t)))
        )
    )

    (defn #_"TSeq" TSeq'create [#_"TNode" t, #_"boolean" asc, #_"int" cnt]
        (TSeq'new (TSeq'push t, nil, asc), asc, cnt)
    )

    (extend-type TSeq ISeq
        (#_"Object" ISeq'''first [#_"TSeq" this]
            (first (:stack this))
        )

        (#_"ISeq" ISeq'''next [#_"TSeq" this]
            (let [#_"TNode" t (cast TNode (first (:stack this))) #_"boolean" asc? (:asc this)]
                (when-some [#_"ISeq" stack (TSeq'push (if asc? (TNode'''right t) (TNode'''left t)), (next (:stack this)), asc?)]
                    (TSeq'new stack, asc?, (dec (:cnt this)))
                )
            )
        )
    )

    (extend-type TSeq Counted
        (#_"int" Counted'''count [#_"TSeq" this]
            (when (neg? (:cnt this)) => (:cnt this)
                (Counted'''count (§ super ))
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
        ([] (PersistentTreeMap'new compare))
        ([#_"Comparator" comp] (PersistentTreeMap'new nil, comp))
        ([#_"IPersistentMap" meta, #_"Comparator" comp] (PersistentTreeMap'new meta, comp, nil, 0))
        ([#_"IPersistentMap" meta, #_"Comparator" comp, #_"TNode" tree, #_"int" _count]
            (merge (PersistentTreeMap.) (APersistentMap'new)
                (hash-map
                    #_"IPersistentMap" :_meta meta
                    #_"Comparator" :comp comp
                    #_"TNode" :tree tree
                    #_"int" :_count _count
                )
            )
        )
    )

    (extend-type PersistentTreeMap IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"PersistentTreeMap" this]
            (:_meta this)
        )
    )

    (extend-type PersistentTreeMap IObj
        (#_"PersistentTreeMap" IObj'''withMeta [#_"PersistentTreeMap" this, #_"IPersistentMap" meta]
            (PersistentTreeMap'new meta, (:comp this), (:tree this), (:_count this))
        )
    )

    (def #_"PersistentTreeMap" PersistentTreeMap'EMPTY (PersistentTreeMap'new))

    (defn #_"PersistentTreeMap" PersistentTreeMap'create
        ([#_"Seqable" keyvals]
            (loop-when [#_"IPersistentMap" m PersistentTreeMap'EMPTY #_"ISeq" s (seq keyvals)] (some? s) => m
                (when (some? (next s)) => (throw! (str "no value supplied for key: " (first s)))
                    (recur (assoc m (first s) (second s)) (next (next s)))
                )
            )
        )
        ([#_"Comparator" comp, #_"Seqable" keyvals]
            (loop-when [#_"IPersistentMap" m (PersistentTreeMap'new comp) #_"ISeq" s (seq keyvals)] (some? s) => m
                (when (some? (next s)) => (throw! (str "no value supplied for key: " (first s)))
                    (recur (assoc m (first s) (second s)) (next (next s)))
                )
            )
        )
    )

    (declare find)

    (extend-type PersistentTreeMap Associative
        (#_"boolean" Associative'''containsKey [#_"PersistentTreeMap" this, #_"Object" key]
            (some? (find this key))
        )
    )

    (extend-type PersistentTreeMap Seqable
        (#_"ISeq" Seqable'''seq [#_"PersistentTreeMap" this]
            (when (pos? (:_count this))
                (TSeq'create (:tree this), true, (:_count this))
            )
        )
    )

    (extend-type PersistentTreeMap IPersistentCollection
        (#_"IPersistentCollection" IPersistentCollection'''empty [#_"PersistentTreeMap" this]
            (PersistentTreeMap'new (meta this), (:comp this))
        )
    )

    (extend-type PersistentTreeMap Reversible
        (#_"ISeq" Reversible'''rseq [#_"PersistentTreeMap" this]
            (when (pos? (:_count this))
                (TSeq'create (:tree this), false, (:_count this))
            )
        )
    )

    (extend-type PersistentTreeMap Sorted
        (#_"Comparator" Sorted'''comparator [#_"PersistentTreeMap" this]
            (:comp this)
        )
    )

    #_method
    (defn #_"int" PersistentTreeMap''doCompare [#_"PersistentTreeMap" this, #_"Object" k1, #_"Object" k2]
        (.compare (:comp this), k1, k2)
    )

    (extend-type PersistentTreeMap Sorted
        (#_"Object" Sorted'''entryKey [#_"PersistentTreeMap" this, #_"Object" entry]
            (key entry)
        )

        (#_"ISeq" Sorted'''seq [#_"PersistentTreeMap" this, #_"boolean" ascending?]
            (when (pos? (:_count this))
                (TSeq'create (:tree this), ascending?, (:_count this))
            )
        )

        (#_"ISeq" Sorted'''seqFrom [#_"PersistentTreeMap" this, #_"Object" key, #_"boolean" ascending?]
            (when (pos? (:_count this))
                (loop-when [#_"ISeq" s nil #_"TNode" t (:tree this)] (some? t) => (when (some? s) (TSeq'new s, ascending?))
                    (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
                        (cond
                            (zero? cmp) (TSeq'new (cons t s), ascending?)
                            ascending?  (if (neg? cmp) (recur (cons t s) (TNode'''left t)) (recur s (TNode'''right t)))
                            :else       (if (pos? cmp) (recur (cons t s) (TNode'''right t)) (recur s (TNode'''left t)))
                        )
                    )
                )
            )
        )
    )

    (extend-type PersistentTreeMap IKVReduce
        (#_"Object" IKVReduce'''kvreduce [#_"PersistentTreeMap" this, #_"IFn" f, #_"Object" r]
            (let [r (if (some? (:tree this)) (INode'''kvreduce (:tree this), f, r) r)]
                (if (reduced? r) @r r)
            )
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''min [#_"PersistentTreeMap" this]
        (when-some [#_"TNode" t (:tree this)]
            (loop-when-recur t (some? (TNode'''left t)) (TNode'''left t) => t)
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''max [#_"PersistentTreeMap" this]
        (when-some [#_"TNode" t (:tree this)]
            (loop-when-recur t (some? (TNode'''right t)) (TNode'''right t) => t)
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
            (inc (max (PersistentTreeMap''depth-2 this, (TNode'''left t)) (PersistentTreeMap''depth-2 this, (TNode'''right t))))
        )
    )

    #_method
    (defn #_"int" PersistentTreeMap''depth-1 [#_"PersistentTreeMap" this]
        (PersistentTreeMap''depth-2 this, (:tree this))
    )

    (extend-type PersistentTreeMap ILookup
        (#_"Object" ILookup'''valAt
            ([#_"PersistentTreeMap" this, #_"Object" key] (ILookup'''valAt this, key, nil))
            ([#_"PersistentTreeMap" this, #_"Object" key, #_"Object" notFound]
                (let [#_"TNode" node (find this key)]
                    (if (some? node) (IMapEntry'''val node) notFound)
                )
            )
        )
    )

    #_method
    (defn #_"int" PersistentTreeMap''capacity [#_"PersistentTreeMap" this]
        (:_count this)
    )

    (extend-type PersistentTreeMap Counted
        (#_"int" Counted'''count [#_"PersistentTreeMap" this]
            (:_count this)
        )
    )

    (extend-type PersistentTreeMap Associative
        (#_"TNode" Associative'''entryAt [#_"PersistentTreeMap" this, #_"Object" key]
            (loop-when [#_"TNode" t (:tree this)] (some? t) => t
                (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
                    (cond
                        (neg? cmp) (recur (TNode'''left t))
                        (pos? cmp) (recur (TNode'''right t))
                        :else      t
                    )
                )
            )
        )
    )

    (defn #_"TNode" PersistentTreeMap'rightBalance [#_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" ins]
        (cond
            (and (instance? Red ins) (instance? Red (TNode'''right ins)))
                (PersistentTreeMap'red (:key ins), (IMapEntry'''val ins), (PersistentTreeMap'black key, val, left, (TNode'''left ins)), (TNode'''blacken (TNode'''right ins)))
            (and (instance? Red ins) (instance? Red (TNode'''left ins)))
                (PersistentTreeMap'red (:key (TNode'''left ins)), (IMapEntry'''val (TNode'''left ins)), (PersistentTreeMap'black key, val, left, (TNode'''left (TNode'''left ins))), (PersistentTreeMap'black (:key ins), (IMapEntry'''val ins), (TNode'''right (TNode'''left ins)), (TNode'''right ins)))
            :else
                (PersistentTreeMap'black key, val, left, ins)
        )
    )

    (defn #_"TNode" PersistentTreeMap'balanceLeftDel [#_"Object" key, #_"Object" val, #_"TNode" del, #_"TNode" right]
        (cond
            (instance? Red del)
                (PersistentTreeMap'red key, val, (TNode'''blacken del), right)
            (instance? Black right)
                (PersistentTreeMap'rightBalance key, val, del, (TNode'''redden right))
            (and (instance? Red right) (instance? Black (TNode'''left right)))
                (PersistentTreeMap'red (:key (TNode'''left right)), (IMapEntry'''val (TNode'''left right)), (PersistentTreeMap'black key, val, del, (TNode'''left (TNode'''left right))), (PersistentTreeMap'rightBalance (:key right), (IMapEntry'''val right), (TNode'''right (TNode'''left right)), (TNode'''redden (TNode'''right right))))
            :else
                (throw! "invariant violation")
        )
    )

    (defn #_"TNode" PersistentTreeMap'leftBalance [#_"Object" key, #_"Object" val, #_"TNode" ins, #_"TNode" right]
        (cond
            (and (instance? Red ins) (instance? Red (TNode'''left ins)))
                (PersistentTreeMap'red (:key ins), (IMapEntry'''val ins), (TNode'''blacken (TNode'''left ins)), (PersistentTreeMap'black key, val, (TNode'''right ins), right))
            (and (instance? Red ins) (instance? Red (TNode'''right ins)))
                (PersistentTreeMap'red (:key (TNode'''right ins)), (IMapEntry'''val (TNode'''right ins)), (PersistentTreeMap'black (:key ins), (IMapEntry'''val ins), (TNode'''left ins), (TNode'''left (TNode'''right ins))), (PersistentTreeMap'black key, val, (TNode'''right (TNode'''right ins)), right))
            :else
                (PersistentTreeMap'black key, val, ins, right)
        )
    )

    (defn #_"TNode" PersistentTreeMap'balanceRightDel [#_"Object" key, #_"Object" val, #_"TNode" left, #_"TNode" del]
        (cond
            (instance? Red del)
                (PersistentTreeMap'red key, val, left, (TNode'''blacken del))
            (instance? Black left)
                (PersistentTreeMap'leftBalance key, val, (TNode'''redden left), del)
            (and (instance? Red left) (instance? Black (TNode'''right left)))
                (PersistentTreeMap'red (:key (TNode'''right left)), (IMapEntry'''val (TNode'''right left)), (PersistentTreeMap'leftBalance (:key left), (IMapEntry'''val left), (TNode'''redden (TNode'''left left)), (TNode'''left (TNode'''right left))), (PersistentTreeMap'black key, val, (TNode'''right (TNode'''right left)), del))
            :else
                (throw! "invariant violation")
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''add [#_"PersistentTreeMap" this, #_"TNode" t, #_"Object" key, #_"Object" val, #_"Volatile" found]
        (if (nil? t)
            (if (nil? val)
                (Red'new key)
                (RedVal'new key, val)
            )
            (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
                (if (zero? cmp)
                    (do
                        (vreset! found t)
                        nil
                    )
                    (let [#_"TNode" ins (if (neg? cmp) (PersistentTreeMap''add this, (TNode'''left t), key, val, found) (PersistentTreeMap''add this, (TNode'''right t), key, val, found))]
                        (cond
                            (nil? ins) nil ;; found below
                            (neg? cmp) (TNode'''addLeft t, ins)
                            :else      (TNode'''addRight t, ins)
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
                    (let [#_"TNode" app (PersistentTreeMap'append (TNode'''right left), (TNode'''left right))]
                        (if (instance? Red app)
                            (PersistentTreeMap'red (:key app), (IMapEntry'''val app), (PersistentTreeMap'red (:key left), (IMapEntry'''val left), (TNode'''left left), (TNode'''left app)), (PersistentTreeMap'red (:key right), (IMapEntry'''val right), (TNode'''right app), (TNode'''right right)))
                            (PersistentTreeMap'red (:key left), (IMapEntry'''val left), (TNode'''left left), (PersistentTreeMap'red (:key right), (IMapEntry'''val right), app, (TNode'''right right)))
                        )
                    )
                    (PersistentTreeMap'red (:key left), (IMapEntry'''val left), (TNode'''left left), (PersistentTreeMap'append (TNode'''right left), right))
                )
            (instance? Red right)
                (PersistentTreeMap'red (:key right), (IMapEntry'''val right), (PersistentTreeMap'append left, (TNode'''left right)), (TNode'''right right))
            :else ;; black/black
                (let [#_"TNode" app (PersistentTreeMap'append (TNode'''right left), (TNode'''left right))]
                    (if (instance? Red app)
                        (PersistentTreeMap'red (:key app), (IMapEntry'''val app), (PersistentTreeMap'black (:key left), (IMapEntry'''val left), (TNode'''left left), (TNode'''left app)), (PersistentTreeMap'black (:key right), (IMapEntry'''val right), (TNode'''right app), (TNode'''right right)))
                        (PersistentTreeMap'balanceLeftDel (:key left), (IMapEntry'''val left), (TNode'''left left), (PersistentTreeMap'black (:key right), (IMapEntry'''val right), app, (TNode'''right right)))
                    )
                )
        )
    )

    #_method
    (defn #_"TNode" PersistentTreeMap''remove [#_"PersistentTreeMap" this, #_"TNode" t, #_"Object" key, #_"Volatile" found]
        (when (some? t) => nil ;; not found indicator
            (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
                (if (zero? cmp)
                    (do
                        (vreset! found t)
                        (PersistentTreeMap'append (TNode'''left t), (TNode'''right t))
                    )
                    (let [#_"TNode" del (if (neg? cmp) (PersistentTreeMap''remove this, (TNode'''left t), key, found) (PersistentTreeMap''remove this, (TNode'''right t), key, found))]
                        (when (or (some? del) (some? @found)) => nil ;; not found below
                            (if (neg? cmp)
                                (if (instance? Black (TNode'''left t))
                                    (PersistentTreeMap'balanceLeftDel (:key t), (IMapEntry'''val t), del, (TNode'''right t))
                                    (PersistentTreeMap'red (:key t), (IMapEntry'''val t), del, (TNode'''right t))
                                )
                                (if (instance? Black (TNode'''right t))
                                    (PersistentTreeMap'balanceRightDel (:key t), (IMapEntry'''val t), (TNode'''left t), del)
                                    (PersistentTreeMap'red (:key t), (IMapEntry'''val t), (TNode'''left t), del)
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    #_method
    (defn- #_"TNode" PersistentTreeMap''replace [#_"PersistentTreeMap" this, #_"TNode" t, #_"Object" key, #_"Object" val]
        (let [#_"int" cmp (PersistentTreeMap''doCompare this, key, (:key t))]
            (TNode'''replace t, (:key t), (if (zero? cmp) val (IMapEntry'''val t)), (if (neg? cmp) (PersistentTreeMap''replace this, (TNode'''left t), key, val) (TNode'''left t)), (if (pos? cmp) (PersistentTreeMap''replace this, (TNode'''right t), key, val) (TNode'''right t)))
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

    (extend-type PersistentTreeMap Associative
        (#_"PersistentTreeMap" Associative'''assoc [#_"PersistentTreeMap" this, #_"Object" key, #_"Object" val]
            (let [#_"Volatile" found (volatile! nil) #_"TNode" t (PersistentTreeMap''add this, (:tree this), key, val, found)]
                (if (nil? t)
                    (if (= (IMapEntry'''val (cast TNode @found)) val)
                        this
                        (PersistentTreeMap'new (meta this), (:comp this), (PersistentTreeMap''replace this, (:tree this), key, val), (:_count this))
                    )
                    (PersistentTreeMap'new (meta this), (:comp this), (TNode'''blacken t), (inc (:_count this)))
                )
            )
        )
    )

    (extend-type PersistentTreeMap IPersistentMap
        (#_"PersistentTreeMap" IPersistentMap'''dissoc [#_"PersistentTreeMap" this, #_"Object" key]
            (let [#_"Volatile" found (volatile! nil) #_"TNode" t (PersistentTreeMap''remove this, (:tree this), key, found)]
                (if (nil? t)
                    (if (nil? @found)
                        this
                        (PersistentTreeMap'new (meta this), (:comp this))
                    )
                    (PersistentTreeMap'new (meta this), (:comp this), (TNode'''blacken t), (dec (:_count this)))
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.PersistentTreeSet

(class-ns PersistentTreeSet
    (defn #_"PersistentTreeSet" PersistentTreeSet'new [#_"IPersistentMap" meta, #_"IPersistentMap" impl]
        (merge (PersistentTreeSet.) (APersistentSet'new impl)
            (hash-map
                #_"IPersistentMap" :_meta meta
            )
        )
    )

    (extend-type PersistentTreeSet IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"PersistentTreeSet" this]
            (:_meta this)
        )
    )

    (extend-type PersistentTreeSet IObj
        (#_"PersistentTreeSet" IObj'''withMeta [#_"PersistentTreeSet" this, #_"IPersistentMap" meta]
            (PersistentTreeSet'new meta, (:impl this))
        )
    )

    (def #_"PersistentTreeSet" PersistentTreeSet'EMPTY (PersistentTreeSet'new nil, PersistentTreeMap'EMPTY))

    (defn #_"PersistentTreeSet" PersistentTreeSet'create
        ([                     #_"Seqable" items] (into PersistentTreeSet'EMPTY                                        items))
        ([#_"Comparator" comp, #_"Seqable" items] (into (PersistentTreeSet'new nil, (PersistentTreeMap'new nil, comp)) items))
    )

    (extend-type PersistentTreeSet IPersistentSet
        (#_"IPersistentSet" IPersistentSet'''disj [#_"PersistentTreeSet" this, #_"Object" key]
            (if (contains? this key)
                (PersistentTreeSet'new (meta this), (dissoc (:impl this) key))
                this
            )
        )
    )

    (extend-type PersistentTreeSet IPersistentCollection
        (#_"PersistentTreeSet" IPersistentCollection'''conj [#_"PersistentTreeSet" this, #_"Object" o]
            (if (contains? this o)
                this
                (PersistentTreeSet'new (meta this), (assoc (:impl this) o o))
            )
        )

        (#_"PersistentTreeSet" IPersistentCollection'''empty [#_"PersistentTreeSet" this]
            (PersistentTreeSet'new (meta this), (empty (:impl this)))
        )
    )

    (declare map)

    (extend-type PersistentTreeSet Reversible
        (#_"ISeq" Reversible'''rseq [#_"PersistentTreeSet" this]
            (map key (rseq (:impl this)))
        )
    )

    (extend-type PersistentTreeSet Sorted
        (#_"Comparator" Sorted'''comparator [#_"PersistentTreeSet" this]
            (Sorted'''comparator (:impl this))
        )

        (#_"Object" Sorted'''entryKey [#_"PersistentTreeSet" this, #_"Object" entry]
            entry
        )

        (#_"ISeq" Sorted'''seq [#_"PersistentTreeSet" this, #_"boolean" ascending?]
            (keys (Sorted'''seq (:impl this), ascending?))
        )

        (#_"ISeq" Sorted'''seqFrom [#_"PersistentTreeSet" this, #_"Object" key, #_"boolean" ascending?]
            (keys (Sorted'''seqFrom (:impl this), key, ascending?))
        )
    )
)
)

(java-ns cloiure.lang.PersistentVector

(class-ns VNode
    (defn #_"VNode" VNode'new
        ([#_"AtomicReference<Thread>" edit] (VNode'new edit, (make-array Object 32)))
        ([#_"AtomicReference<Thread>" edit, #_"Object[]" a]
            (merge (VNode.)
                (hash-map
                    #_"AtomicReference<Thread>" :edit edit
                    #_"Object[]" :a a
                )
            )
        )
    )

    (defn #_"VNode" VNode'newPath [#_"AtomicReference<Thread>" edit, #_"int" level, #_"VNode" node]
        (when-not (zero? level) => node
            (let [#_"VNode" v (VNode'new edit)]
                (aset (:a v) 0 (VNode'newPath edit, (- level 5), node))
                v
            )
        )
    )
)

(class-ns TransientVector
    (defn- #_"VNode" TransientVector'editableRoot [#_"VNode" node]
        (VNode'new (AtomicReference. (Thread/currentThread)), (.clone (:a node)))
    )

    (defn- #_"Object[]" TransientVector'editableTail [#_"Object[]" tail]
        (let [#_"Object[]" a (make-array Object 32)]
            (System/arraycopy tail, 0, a, 0, (alength tail))
            a
        )
    )

    (defn #_"TransientVector" TransientVector'new
        ([#_"PersistentVector" v]
            (TransientVector'new (:cnt v), (:shift v), (TransientVector'editableRoot (:root v)), (TransientVector'editableTail (:tail v)))
        )
        ([#_"int" cnt, #_"int" shift, #_"VNode" root, #_"Object[]" tail]
            (merge (TransientVector.) (AFn'new)
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
                (throw! "transient used after persistent! call")
            )
        )
        (#_"VNode" [#_"TransientVector" this, #_"VNode" node]
            (when-not (= (:edit node) (:edit (:root this))) => node
                (VNode'new (:edit (:root this)), (.clone (:a node)))
            )
        )
    )

    (extend-type TransientVector Counted
        (#_"int" Counted'''count [#_"TransientVector" this]
            (TransientVector''ensureEditable this)
            (:cnt this)
        )
    )

    #_method
    (defn- #_"int" TransientVector''tailoff [#_"TransientVector" this]
        (if (< (:cnt this) 32) 0 (bit-shift-left (unsigned-bit-shift-right (dec (:cnt this)) 5) 5))
    )

    (declare PersistentVector'new)

    (extend-type TransientVector ITransientCollection
        (#_"PersistentVector" ITransientCollection'''persistent [#_"TransientVector" this]
            (TransientVector''ensureEditable this)
            (.set (:edit (:root this)), nil)
            (let [#_"Object[]" trimmedTail (make-array Object (- (:cnt this) (TransientVector''tailoff this)))]
                (System/arraycopy (:tail this), 0, trimmedTail, 0, (alength trimmedTail))
                (PersistentVector'new (:cnt this), (:shift this), (:root this), trimmedTail)
            )
        )
    )

    #_method
    (defn- #_"VNode" TransientVector''pushTail [#_"TransientVector" this, #_"int" level, #_"VNode" parent, #_"VNode" tailnode]
        ;; if parent is leaf, insert node,
        ;; else does it map to an existing child? -> nodeToInsert = pushNode one more level
        ;; else alloc new path
        ;; return nodeToInsert placed in parent
        (let [parent (TransientVector''ensureEditable this, parent)
              #_"int" i (bit-and (unsigned-bit-shift-right (dec (:cnt this)) level) 0x01f)
              #_"VNode" nodeToInsert
                (when-not (= level 5) => tailnode
                    (let [#_"VNode" child (cast VNode (aget (:a parent) i))]
                        (if (some? child)
                            (TransientVector''pushTail this, (- level 5), child, tailnode)
                            (VNode'newPath (:edit (:root this)), (- level 5), tailnode)
                        )
                    )
                )]
            (aset (:a parent) i nodeToInsert)
            parent
        )
    )

    (extend-type TransientVector ITransientCollection
        (#_"TransientVector" ITransientCollection'''conj [#_"TransientVector" this, #_"Object" val]
            (TransientVector''ensureEditable this)
            (let [#_"int" n (:cnt this)]
                (if (< (- n (TransientVector''tailoff this)) 32) ;; room in tail?
                    (do
                        (aset (:tail this) (bit-and n 0x01f) val)
                        (update this :cnt inc)
                    )
                    ;; full tail, push into tree
                    (let [#_"VNode" tailnode (VNode'new (:edit (:root this)), (:tail this))
                          this (assoc this :tail (make-array Object 32))
                          _ (aset (:tail this) 0 val)
                          #_"int" shift (:shift this)
                          [#_"VNode" root shift]
                            (if (< (bit-shift-left 1 shift) (unsigned-bit-shift-right n 5)) ;; overflow root?
                                (let [root (VNode'new (:edit (:root this)))]
                                    (aset (:a root) 0 (:root this))
                                    (aset (:a root) 1 (VNode'newPath (:edit (:root this)), shift, tailnode))
                                    [root (+ shift 5)]
                                )
                                [(TransientVector''pushTail this, shift, (:root this), tailnode) shift]
                            )]
                        (-> this (assoc :root root :shift shift) (update :cnt inc))
                    )
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
                                 [(cast VNode (aget (:a node) (bit-and (unsigned-bit-shift-right i level) 0x01f))) (- level 5)]
                              => (:a node)
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
                                 [(TransientVector''ensureEditable this, (cast VNode (aget (:a node) (bit-and (unsigned-bit-shift-right i level) 0x01f)))) (- level 5)]
                              => (:a node)
                )
            )
        )
    )

    (extend-type TransientVector ILookup
        (#_"Object" ILookup'''valAt
            ([#_"TransientVector" this, #_"Object" key] (ILookup'''valAt this, key, nil))
            ([#_"TransientVector" this, #_"Object" key, #_"Object" notFound]
                (TransientVector''ensureEditable this)
                (when (integer? key) => notFound
                    (let-when [#_"int" i (.intValue key)] (< -1 i (:cnt this)) => notFound
                        (nth this i)
                    )
                )
            )
        )
    )

    (def- #_"Object" TransientVector'NOT_FOUND (Object.))

    (extend-type TransientVector ITransientAssociative
        (#_"boolean" ITransientAssociative'''containsKey [#_"TransientVector" this, #_"Object" key]
            (not (identical? (get this key TransientVector'NOT_FOUND) TransientVector'NOT_FOUND))
        )

        (#_"IMapEntry" ITransientAssociative'''entryAt [#_"TransientVector" this, #_"Object" key]
            (let [#_"Object" v (get this key TransientVector'NOT_FOUND)]
                (when-not (identical? v TransientVector'NOT_FOUND)
                    (MapEntry'create key, v)
                )
            )
        )
    )

    (extend-type TransientVector IFn
        (#_"Object" IFn'''invoke [#_"TransientVector" this, #_"Object" i]
            ;; note - relies on ensureEditable in nth
            (when (integer? i) => (throw! "key must be integer")
                (nth this (.intValue i))
            )
        )
    )

    (extend-type TransientVector Indexed
        (#_"Object" Indexed'''nth
            ([#_"TransientVector" this, #_"int" i]
                (TransientVector''ensureEditable this)
                (aget (TransientVector''arrayFor this, i) (bit-and i 0x01f))
            )
            ([#_"TransientVector" this, #_"int" i, #_"Object" notFound]
                (when (< -1 i (count this)) => notFound
                    (nth this i)
                )
            )
        )
    )

    #_method
    (defn- #_"VNode" TransientVector''doAssoc [#_"TransientVector" this, #_"int" level, #_"VNode" node, #_"int" i, #_"Object" val]
        (let [node (TransientVector''ensureEditable this, node)]
            (if (zero? level)
                (aset (:a node) (bit-and i 0x01f) val)
                (let [#_"int" si (bit-and (unsigned-bit-shift-right i level) 0x01f)]
                    (aset (:a node) si (TransientVector''doAssoc this, (- level 5), (cast VNode (aget (:a node) si)), i, val))
                )
            )
            node
        )
    )

    (extend-type TransientVector ITransientVector
        (#_"TransientVector" ITransientVector'''assocN [#_"TransientVector" this, #_"int" i, #_"Object" val]
            (TransientVector''ensureEditable this)
            (if (< -1 i (:cnt this))
                (if (<= (TransientVector''tailoff this) i)
                    (do
                        (aset (:tail this) (bit-and i 0x01f) val)
                        this
                    )
                    (do
                        (assoc this :root (TransientVector''doAssoc this, (:shift this), (:root this), i, val))
                    )
                )
                (when (= i (:cnt this)) => (throw (IndexOutOfBoundsException.))
                    (conj! this val)
                )
            )
        )
    )

    (extend-type TransientVector ITransientAssociative
        (#_"TransientVector" ITransientAssociative'''assoc [#_"TransientVector" this, #_"Object" key, #_"Object" val]
            ;; note - relies on ensureEditable in assocN
            (when (integer? key) => (throw! "key must be integer")
                (ITransientVector'''assocN this, (.intValue key), val)
            )
        )
    )

    #_method
    (defn- #_"VNode" TransientVector''popTail [#_"TransientVector" this, #_"int" level, #_"VNode" node]
        (let [node (TransientVector''ensureEditable this, node)
              #_"int" i (bit-and (unsigned-bit-shift-right (- (:cnt this) 2) level) 0x01f)]
            (cond
                (< 5 level)
                    (let [#_"VNode" child (TransientVector''popTail this, (- level 5), (cast VNode (aget (:a node) i)))]
                        (when-not (and (nil? child) (zero? i))
                            (aset (:a node) i child)
                            node
                        )
                    )
                (pos? i)
                    (do
                        (aset (:a node) i nil)
                        node
                    )
            )
        )
    )

    (extend-type TransientVector ITransientVector
        (#_"TransientVector" ITransientVector'''pop [#_"TransientVector" this]
            (TransientVector''ensureEditable this)
            (let [#_"int" n (:cnt this)]
                (when-not (zero? n) => (throw! "can't pop empty vector")
                    (when (and (not= n 1) (zero? (bit-and (dec n) 0x01f))) => (assoc this :cnt (dec n))
                        (let [#_"Object[]" tail (TransientVector''editableArrayFor this, (- n 2))
                              #_"int" shift (:shift this) #_"VNode" root (:root this)
                              root (or (TransientVector''popTail this, shift, root) (VNode'new (:edit root)))
                              [shift root]
                                (when (and (< 5 shift) (nil? (aget (:a root) 1))) => [shift root]
                                    [(- shift 5) (TransientVector''ensureEditable this, (cast VNode (aget (:a root) 0)))]
                                )]
                            (assoc this :cnt (dec n) :shift shift :root root :tail tail)
                        )
                    )
                )
            )
        )
    )
)

(class-ns PersistentVector
    (def #_"VNode" PersistentVector'EMPTY_NODE (VNode'new (AtomicReference. nil), (object-array 32)))

    (defn #_"PersistentVector" PersistentVector'new
        ([#_"int" cnt, #_"int" shift, #_"VNode" root, #_"Object[]" tail] (PersistentVector'new nil, cnt, shift, root, tail))
        ([#_"IPersistentMap" meta, #_"int" cnt, #_"int" shift, #_"VNode" root, #_"Object[]" tail]
            (merge (PersistentVector.) (APersistentVector'new)
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

    (extend-type PersistentVector IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"PersistentVector" this]
            (:_meta this)
        )
    )

    (extend-type PersistentVector IObj
        (#_"PersistentVector" IObj'''withMeta [#_"PersistentVector" this, #_"IPersistentMap" meta]
            (PersistentVector'new meta, (:cnt this), (:shift this), (:root this), (:tail this))
        )
    )

    (def #_"PersistentVector" PersistentVector'EMPTY (PersistentVector'new 0, 5, PersistentVector'EMPTY_NODE, (object-array 0)))

    (defn #_"PersistentVector" PersistentVector'adopt [#_"Object[]" items]
        (PersistentVector'new (alength items), 5, PersistentVector'EMPTY_NODE, items)
    )

    (defn #_"PersistentVector" PersistentVector'create-1r [#_"IReduce" items]
        (into PersistentVector'EMPTY items)
    )

    (defn #_"PersistentVector" PersistentVector'create-1s [#_"Seqable" items]
        (let [#_"Object[]" a (make-array Object 32)
              [#_"ISeq" s #_"int" i]
                (loop-when-recur [s (seq items) i 0] (and (some? s) (< i 32)) [(next s) (inc i)] => [s i]
                    (aset a i (first s))
                )]
            (cond
                (some? s) ;; >32, construct with array directly
                    (into (PersistentVector'new 32, 5, PersistentVector'EMPTY_NODE, a) s)
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

    (defn #_"PersistentVector" PersistentVector'create-1a [& #_"Object..." items]
        (loop-when-recur [#_"TransientVector" v (transient PersistentVector'EMPTY) #_"int" i 0]
                         (< i (alength items))
                         [(conj! v (aget items i)) (inc i)]
                      => (persistent! v)
        )
    )

    (extend-type PersistentVector IEditableCollection
        (#_"TransientVector" IEditableCollection'''asTransient [#_"PersistentVector" this]
            (TransientVector'new this)
        )
    )

    #_method
    (defn #_"int" PersistentVector''tailoff [#_"PersistentVector" this]
        (if (< (:cnt this) 32) 0 (bit-shift-left (unsigned-bit-shift-right (dec (:cnt this)) 5) 5))
    )

    #_method
    (defn #_"Object[]" PersistentVector''arrayFor [#_"PersistentVector" this, #_"int" i]
        (when (< -1 i (:cnt this)) => (throw (IndexOutOfBoundsException.))
            (when (< i (PersistentVector''tailoff this)) => (:tail this)
                (loop-when-recur [#_"VNode" node (:root this) #_"int" level (:shift this)]
                                 (< 0 level)
                                 [(cast VNode (aget (:a node) (bit-and (unsigned-bit-shift-right i level) 0x01f))) (- level 5)]
                              => (:a node)
                )
            )
        )
    )

    (extend-type PersistentVector Indexed
        (#_"Object" Indexed'''nth
            ([#_"PersistentVector" this, #_"int" i]
                (aget (PersistentVector''arrayFor this, i) (bit-and i 0x01f))
            )
            ([#_"PersistentVector" this, #_"int" i, #_"Object" notFound]
                (when (< -1 i (:cnt this)) => notFound
                    (nth this i)
                )
            )
        )
    )

    (defn- #_"VNode" PersistentVector'doAssoc [#_"int" level, #_"VNode" node, #_"int" i, #_"Object" o]
        (let [#_"VNode" v (VNode'new (:edit node), (.clone (:a node)))]
            (if (zero? level)
                (aset (:a v) (bit-and i 0x01f) o)
                (let [#_"int" si (bit-and (unsigned-bit-shift-right i level) 0x01f)]
                    (aset (:a v) si (PersistentVector'doAssoc (- level 5), (cast VNode (aget (:a node) si)), i, o))
                )
            )
            v
        )
    )

    (extend-type PersistentVector IPersistentVector
        (#_"PersistentVector" IPersistentVector'''assocN [#_"PersistentVector" this, #_"int" i, #_"Object" o]
            (if (< -1 i (:cnt this))
                (if (<= (PersistentVector''tailoff this) i)
                    (let [#_"Object[]" tail (make-array Object (alength (:tail this)))]
                        (System/arraycopy (:tail this), 0, tail, 0, (alength (:tail this)))
                        (aset tail (bit-and i 0x01f) o)
                        (PersistentVector'new (meta this), (:cnt this), (:shift this), (:root this), tail)
                    )
                    (PersistentVector'new (meta this), (:cnt this), (:shift this), (PersistentVector'doAssoc (:shift this), (:root this), i, o), (:tail this))
                )
                (when (= i (:cnt this)) => (throw (IndexOutOfBoundsException.))
                    (conj this o)
                )
            )
        )
    )

    (extend-type PersistentVector Counted
        (#_"int" Counted'''count [#_"PersistentVector" this]
            (:cnt this)
        )
    )

    #_method
    (defn- #_"VNode" PersistentVector''pushTail [#_"PersistentVector" this, #_"int" level, #_"VNode" parent, #_"VNode" tailnode]
        ;; if parent is leaf, insert node,
        ;; else does it map to an existing child? -> nodeToInsert = pushNode one more level
        ;; else alloc new path
        ;; return nodeToInsert placed in copy of parent
        (let [#_"int" i (bit-and (unsigned-bit-shift-right (dec (:cnt this)) level) 0x01f)
              #_"VNode" v (VNode'new (:edit parent), (.clone (:a parent)))
              #_"VNode" nodeToInsert
                (when-not (= level 5) => tailnode
                    (let [#_"VNode" child (cast VNode (aget (:a parent) i))]
                        (if (some? child)
                            (PersistentVector''pushTail this, (- level 5), child, tailnode)
                            (VNode'newPath (:edit (:root this)), (- level 5), tailnode)
                        )
                    )
                )]
            (aset (:a v) i nodeToInsert)
            v
        )
    )

    (extend-type PersistentVector IPersistentCollection
        (#_"PersistentVector" IPersistentCollection'''conj [#_"PersistentVector" this, #_"Object" val]
            (let [#_"int" n (:cnt this)]
                (if (< (- n (PersistentVector''tailoff this)) 32) ;; room in tail?
                    (let [#_"int" e (alength (:tail this)) #_"Object[]" tail (make-array Object (inc e))]
                        (System/arraycopy (:tail this), 0, tail, 0, e)
                        (aset tail e val)
                        (PersistentVector'new (meta this), (inc n), (:shift this), (:root this), tail)
                    )
                    ;; full tail, push into tree
                    (let [#_"VNode" tailnode (VNode'new (:edit (:root this)), (:tail this))
                          #_"int" shift (:shift this)
                          [#_"VNode" root shift]
                            (if (< (bit-shift-left 1 shift) (unsigned-bit-shift-right n 5)) ;; overflow root?
                                (let [root (VNode'new (:edit (:root this)))]
                                    (aset (:a root) 0 (:root this))
                                    (aset (:a root) 1 (VNode'newPath (:edit (:root this)), shift, tailnode))
                                    [root (+ shift 5)]
                                )
                                [(PersistentVector''pushTail this, shift, (:root this), tailnode) shift]
                            )]
                        (PersistentVector'new (meta this), (inc n), shift, root, (object-array [ val ]))
                    )
                )
            )
        )

        (#_"PersistentVector" IPersistentCollection'''empty [#_"PersistentVector" this]
            (with-meta PersistentVector'EMPTY (meta this))
        )
    )

    (extend-type PersistentVector IReduce
        (#_"Object" IReduce'''reduce
            ([#_"PersistentVector" this, #_"IFn" f]
                (when (pos? (:cnt this)) => (f)
                    (loop-when [#_"Object" r (aget (PersistentVector''arrayFor this, 0) 0) #_"int" i 0] (< i (:cnt this)) => r
                        (let [#_"Object[]" a (PersistentVector''arrayFor this, i)
                              r (loop-when [r r #_"int" j (if (zero? i) 1 0)] (< j (alength a)) => r
                                    (let [r (f r (aget a j))]
                                        (when-not (reduced? r) => (ß return @r)
                                            (recur r (inc j))
                                        )
                                    )
                                )]
                            (recur r (+ i (alength a)))
                        )
                    )
                )
            )
            ([#_"PersistentVector" this, #_"IFn" f, #_"Object" r]
                (loop-when [r r #_"int" i 0] (< i (:cnt this)) => r
                    (let [#_"Object[]" a (PersistentVector''arrayFor this, i)
                          r (loop-when [r r #_"int" j 0] (< j (alength a)) => r
                                (let [r (f r (aget a j))]
                                    (when-not (reduced? r) => (ß return @r)
                                        (recur r (inc j))
                                    )
                                )
                            )]
                        (recur r (+ i (alength a)))
                    )
                )
            )
        )
    )

    (extend-type PersistentVector IKVReduce
        (#_"Object" IKVReduce'''kvreduce [#_"PersistentVector" this, #_"IFn" f, #_"Object" r]
            (loop-when [r r #_"int" i 0] (< i (:cnt this)) => r
                (let [#_"Object[]" a (PersistentVector''arrayFor this, i)
                      r (loop-when [r r #_"int" j 0] (< j (alength a)) => r
                            (let [r (f r (+ j i) (aget a j))]
                                (when-not (reduced? r) => (ß return @r)
                                    (recur r (inc j))
                                )
                            )
                        )]
                    (recur r (+ i (alength a)))
                )
            )
        )
    )

    #_method
    (defn- #_"VNode" PersistentVector''popTail [#_"PersistentVector" this, #_"int" level, #_"VNode" node]
        (let [#_"int" i (bit-and (unsigned-bit-shift-right (- (:cnt this) 2) level) 0x01f)]
            (cond
                (< 5 level)
                    (let [#_"VNode" child (PersistentVector''popTail this, (- level 5), (cast VNode (aget (:a node) i)))]
                        (when-not (and (nil? child) (zero? i))
                            (let [#_"VNode" v (VNode'new (:edit (:root this)), (.clone (:a node)))]
                                (aset (:a v) i child)
                                v
                            )
                        )
                    )
                (pos? i)
                    (let [#_"VNode" v (VNode'new (:edit (:root this)), (.clone (:a node)))]
                        (aset (:a v) i nil)
                        v
                    )
            )
        )
    )

    (extend-type PersistentVector IPersistentStack
        (#_"PersistentVector" IPersistentStack'''pop [#_"PersistentVector" this]
            (cond
                (zero? (:cnt this))
                    (throw! "can't pop empty vector")
                (= (:cnt this) 1)
                    (with-meta PersistentVector'EMPTY (meta this))
                (< 1 (- (:cnt this) (PersistentVector''tailoff this)))
                    (let [#_"Object[]" tail (make-array Object (dec (alength (:tail this))))]
                        (System/arraycopy (:tail this), 0, tail, 0, (alength tail))
                        (PersistentVector'new (meta this), (dec (:cnt this)), (:shift this), (:root this), tail)
                    )
                :else
                    (let [#_"Object[]" tail (PersistentVector''arrayFor this, (- (:cnt this) 2))
                          #_"int" shift (:shift this)
                          #_"VNode" root (or (PersistentVector''popTail this, shift, (:root this)) PersistentVector'EMPTY_NODE)
                          [shift root]
                            (when (and (< 5 shift) (nil? (aget (:a root) 1))) => [shift root]
                                [(- shift 5) (cast VNode (aget (:a root) 0))]
                            )]
                        (PersistentVector'new (meta this), (dec (:cnt this)), shift, root, tail)
                    )
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

    (defn #_"IPersistentVector" LazilyPersistentVector'create [#_"Object" obj]
        (condp satisfies? obj
            IReduce (PersistentVector'create-1r obj)
            Seqable (PersistentVector'create-1s obj)
                    (LazilyPersistentVector'createOwning (to-array obj))
        )
    )
)
)

(java-ns cloiure.lang.Repeat

(class-ns Repeat
    (def- #_"long" Repeat'INFINITE -1)

    (defn- #_"Repeat" Repeat'new
        ([#_"long" cnt, #_"Object" val] (Repeat'new nil, cnt, val))
        ([#_"IPersistentMap" meta, #_"long" cnt, #_"Object" val]
            (merge (Repeat.) (ASeq'new meta)
                (hash-map
                    #_"long" :cnt cnt ;; always INFINITE or pos?
                    #_"Object" :val val
                )
            )
        )
    )

    (extend-type Repeat IObj
        (#_"Repeat" IObj'''withMeta [#_"Repeat" this, #_"IPersistentMap" meta]
            (Repeat'new meta, (:cnt this), (:val this))
        )
    )

    (defn #_"Repeat|ISeq" Repeat'create
        ([#_"Object" val] (Repeat'new Repeat'INFINITE, val))
        ([#_"long" n, #_"Object" val] (if (pos? n) (Repeat'new n, val) ()))
    )

    (extend-type Repeat ISeq
        (#_"Object" ISeq'''first [#_"Repeat" this]
            (:val this)
        )

        (#_"ISeq" ISeq'''next [#_"Repeat" this]
            (cond
                (< 1 (:cnt this))               (Repeat'new (dec (:cnt this)), (:val this))
                (= (:cnt this) Repeat'INFINITE) this
            )
        )
    )

    (extend-type Repeat IReduce
        (#_"Object" IReduce'''reduce
            ([#_"Repeat" this, #_"IFn" f]
                (let [#_"Object" r (:val this)]
                    (if (= (:cnt this) Repeat'INFINITE)
                        (loop [r r]
                            (let [r (f r (:val this))]
                                (if (reduced? r) @r (recur r))
                            )
                        )
                        (loop-when [r r #_"long" i 1] (< i (:cnt this)) => r
                            (let [r (f r (:val this))]
                                (if (reduced? r) @r (recur r (inc i)))
                            )
                        )
                    )
                )
            )
            ([#_"Repeat" this, #_"IFn" f, #_"Object" r]
                (if (= (:cnt this) Repeat'INFINITE)
                    (loop [r r]
                        (let [r (f r (:val this))]
                            (if (reduced? r) @r (recur r))
                        )
                    )
                    (loop-when [r r #_"long" i 0] (< i (:cnt this)) => r
                        (let [r (f r (:val this))]
                            (if (reduced? r) @r (recur r (inc i)))
                        )
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.Range

;;;
 ; Implements generic numeric (potentially infinite) range.
 ;;
(class-ns Range
    (defn- #_"RangeBoundsCheck" Range'positiveStep [#_"Object" end]
        (reify RangeBoundsCheck
            (#_"boolean" RangeBoundsCheck'''exceededBounds [#_"RangeBoundsCheck" _self, #_"Object" val]
                (<= end val)
            )
        )
    )

    (defn- #_"RangeBoundsCheck" Range'negativeStep [#_"Object" end]
        (reify RangeBoundsCheck
            (#_"boolean" RangeBoundsCheck'''exceededBounds [#_"RangeBoundsCheck" _self, #_"Object" val]
                (<= val end)
            )
        )
    )

    (defn- #_"Range" Range'new
        ([#_"Object" start, #_"Object" end, #_"Object" step, #_"RangeBoundsCheck" boundsCheck]
            (Range'new nil, start, end, step, boundsCheck)
        )
        ([#_"IPersistentMap" meta, #_"Object" start, #_"Object" end, #_"Object" step, #_"RangeBoundsCheck" boundsCheck]
            (merge (Range.) (ASeq'new meta)
                (hash-map
                    ;; Invariants guarantee this is never an "empty" seq
                    #_"Object" :start start
                    #_"Object" :end end
                    #_"Object" :step step
                    #_"RangeBoundsCheck" :boundsCheck boundsCheck
                )
            )
        )
    )

    (extend-type Range IObj
        (#_"Range" IObj'''withMeta [#_"Range" this, #_"IPersistentMap" meta]
            (when-not (= meta (:_meta this)) => this
                (Range'new meta, (:end this), (:start this), (:step this), (:boundsCheck this))
            )
        )
    )

    (defn #_"ISeq" Range'create
        ([#_"Object" end]
            (when (pos? end) => ()
                (Range'new 0, end, 1, (Range'positiveStep end))
            )
        )
        ([#_"Object" start, #_"Object" end]
            (Range'create start, end, 1)
        )
        ([#_"Object" start, #_"Object" end, #_"Object" step]
            (cond
                (or (and (pos? step) (< end start))
                    (and (neg? step) (< start end))
                    (= start end)
                )
                    ()
                (zero? step)
                    (Repeat'create start)
                :else
                    (Range'new start, end, step, (if (pos? step) (Range'positiveStep end) (Range'negativeStep end)))
            )
        )
    )

    (extend-type Range ISeq
        (#_"Object" ISeq'''first [#_"Range" this]
            (:start this)
        )

        (#_"ISeq" ISeq'''next [#_"Range" this]
            (let-when-not [#_"Object" n (+ (:start this) (:step this))] (RangeBoundsCheck'''exceededBounds (:boundsCheck this), n)
                (Range'new n, (:end this), (:step this), (:boundsCheck this))
            )
        )
    )

    (extend-type Range IReduce
        (#_"Object" IReduce'''reduce
            ([#_"Range" this, #_"IFn" f]
                (loop [#_"Object" r (:start this) #_"Number" n r]
                    (let-when-not [n (+ n (:step this))] (RangeBoundsCheck'''exceededBounds (:boundsCheck this), n) => r
                        (let-when-not [r (f r n)] (reduced? r) => @r
                            (recur r n)
                        )
                    )
                )
            )
            ([#_"Range" this, #_"IFn" f, #_"Object" r]
                (loop [r r #_"Object" n (:start this)]
                    (let-when-not [r (f r n)] (reduced? r) => @r
                        (let-when-not [n (+ n (:step this))] (RangeBoundsCheck'''exceededBounds (:boundsCheck this), n) => r
                            (recur r n)
                        )
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
        (merge (Reduced.)
            (hash-map
                #_"Object" :val val
            )
        )
    )

    (extend-type Reduced IDeref
        (#_"Object" IDeref'''deref [#_"Reduced" this]
            (:val this)
        )
    )
)
)

(java-ns cloiure.lang.StringSeq

(class-ns StringSeq
    (defn- #_"StringSeq" StringSeq'new [#_"IPersistentMap" meta, #_"CharSequence" s, #_"int" i]
        (merge (StringSeq.) (ASeq'new meta)
            (hash-map
                #_"CharSequence" :s s
                #_"int" :i i
            )
        )
    )

    (extend-type StringSeq IObj
        (#_"StringSeq" IObj'''withMeta [#_"StringSeq" this, #_"IPersistentMap" meta]
            (when-not (= meta (meta this)) => this
                (StringSeq'new meta, (:s this), (:i this))
            )
        )
    )

    (defn #_"StringSeq" StringSeq'create [#_"CharSequence" s]
        (when (pos? (count s))
            (StringSeq'new nil, s, 0)
        )
    )

    (extend-protocol Seqable CharSequence
        (#_"StringSeq" Seqable'''seq [#_"CharSequence" s] (StringSeq'create s))
    )

    (extend-type StringSeq ISeq
        (#_"Object" ISeq'''first [#_"StringSeq" this]
            (Character/valueOf (nth (:s this) (:i this)))
        )

        (#_"ISeq" ISeq'''next [#_"StringSeq" this]
            (when (< (inc (:i this)) (count (:s this)))
                (StringSeq'new (:_meta this), (:s this), (inc (:i this)))
            )
        )
    )

    (extend-type StringSeq Counted
        (#_"int" Counted'''count [#_"StringSeq" this]
            (- (count (:s this)) (:i this))
        )
    )

    (extend-type StringSeq IReduce
        (#_"Object" IReduce'''reduce
            ([#_"StringSeq" this, #_"IFn" f]
                (let [#_"CharSequence" s (:s this) #_"int" i (:i this) #_"int" n (count s)]
                    (loop-when [#_"Object" r (nth s i) i (inc i)] (< i n) => r
                        (let [r (f r (nth s i))]
                            (if (reduced? r) @r (recur r (inc i)))
                        )
                    )
                )
            )
            ([#_"StringSeq" this, #_"IFn" f, #_"Object" r]
                (let [#_"CharSequence" s (:s this) #_"int" i (:i this) #_"int" n (count s)]
                    (loop-when [r (f r (nth s i)) i (inc i)] (< i n) => (if (reduced? r) @r r)
                        (if (reduced? r) @r (recur (f r (nth s i)) (inc i)))
                    )
                )
            )
        )
    )
)
)

(java-ns cloiure.lang.Tuple

(class-ns Tuple
    (def #_"int" Tuple'MAX_SIZE 6)

    (defn #_"IPersistentVector" Tuple'create
        ([] [])
        ([#_"Object" v0] (vector v0))
        ([#_"Object" v0, #_"Object" v1] (vector v0 v1))
        ([#_"Object" v0, #_"Object" v1, #_"Object" v2] (vector v0 v1 v2))
        ([#_"Object" v0, #_"Object" v1, #_"Object" v2, #_"Object" v3] (vector v0 v1 v2 v3))
        ([#_"Object" v0, #_"Object" v1, #_"Object" v2, #_"Object" v3, #_"Object" v4] (vector v0 v1 v2 v3 v4))
        ([#_"Object" v0, #_"Object" v1, #_"Object" v2, #_"Object" v3, #_"Object" v4, #_"Object" v5] (vector v0 v1 v2 v3 v4 v5))
    )
)
)

(java-ns cloiure.lang.Var

(class-ns Unbound
    (defn #_"Unbound" Unbound'new [#_"Namespace" ns, #_"Symbol" sym]
        (merge (Unbound.) (AFn'new)
            (hash-map
                #_"Namespace" :ns ns
                #_"Symbol" :sym sym
            )
        )
    )

    (declare Var'toString)

    (extend-type Unbound IObject
        (#_"String" IObject'''toString [#_"Unbound" this]
            (str "Unbound: " (Var'toString (:ns this), (:sym this)))
        )
    )

    #_override
    (defn #_"Object" AFn'''throwArity--Unbound [#_"Unbound" this, #_"int" n]
        (throw! (str "attempting to call unbound fn: " (Var'toString (:ns this), (:sym this))))
    )
)

(class-ns Var
    (def #_"ThreadLocal" Var'dvals (ThreadLocal.))

    (defn #_"Var" Var'find [#_"Symbol" sym]
        (when (some? (:ns sym)) => (throw! "symbol must be namespace-qualified")
            (let [#_"Namespace" ns (Namespace'find (Symbol'intern (:ns sym)))]
                (when (some? ns) => (throw! (str "no such namespace: " (:ns sym)))
                    (Namespace''findInternedVar ns, (Symbol'intern (:name sym)))
                )
            )
        )
    )

    (defn #_"Var" Var'new
        ([#_"Namespace" ns, #_"Symbol" sym] (Var'new ns, sym, (Unbound'new ns, sym)))
        ([#_"Namespace" ns, #_"Symbol" sym, #_"Object" root]
            (merge (Var.)
                (hash-map
                    #_"Namespace" :ns ns
                    #_"Symbol" :sym sym
                    #_"Object'" :root (atom root)
                )
            )
        )
    )

    (extend-type Var IMeta
        (#_"IPersistentMap" IMeta'''meta [#_"Var" this]
            (meta (:root this))
        )
    )

    (extend-type Var IReference
        (#_"IPersistentMap" IReference'''alterMeta [#_"Var" this, #_"IFn" f, #_"ISeq" args]
            (apply alter-meta! (:root this) f args)
        )

        (#_"IPersistentMap" IReference'''resetMeta [#_"Var" this, #_"IPersistentMap" m]
            (reset-meta! (:root this) m)
        )
    )

    (defn- #_"String" Var'toString [#_"Namespace" ns, #_"Symbol" sym]
        (if (some? ns)
            (str "#'" (:name ns) "/" sym)
            (str "#<Var: " (or sym "--unnamed--") ">")
        )
    )

    (extend-type Var IObject
        (#_"String" IObject'''toString [#_"Var" this]
            (Var'toString (:ns this), (:sym this))
        )
    )

    #_method
    (defn #_"boolean" Var''hasRoot [#_"Var" this]
        (not (instance? Unbound @(:root this)))
    )

    #_method
    (defn #_"boolean" Var''isBound [#_"Var" this]
        (or (Var''hasRoot this) (contains? (first (.get Var'dvals)) this))
    )

    #_method
    (defn #_"Volatile" Var''getThreadBinding [#_"Var" this]
        (get (first (.get Var'dvals)) this)
    )

    #_method
    (defn #_"Object" Var''get [#_"Var" this]
        @(or (Var''getThreadBinding this) (:root this))
    )

    (extend-type Var IDeref
        (#_"Object" IDeref'''deref [#_"Var" this]
            (Var''get this)
        )
    )

    #_method
    (defn #_"Object" Var''set [#_"Var" this, #_"Object" val]
        (let [#_"Volatile" v (Var''getThreadBinding this)]
            (when (some? v) => (throw! (str "can't change/establish root binding of: " (:sym this) " with var-set/set!"))
                (vreset! v val)
            )
        )
    )

    #_method
    (defn #_"void" Var''setMacro [#_"Var" this]
        (alter-meta! this assoc :macro true)
        nil
    )

    #_method
    (defn #_"boolean" Var''isMacro [#_"Var" this]
        (boolean (:macro (meta this)))
    )

    #_method
    (defn #_"boolean" Var''isPublic [#_"Var" this]
        (not (:private (meta this)))
    )

    #_method
    (defn #_"Object" Var''getRawRoot [#_"Var" this]
        @(:root this)
    )

    #_method
    (defn #_"void" Var''bindRoot [#_"Var" this, #_"Object" root]
        ;; binding root always clears macro flag
        (alter-meta! this dissoc :macro)
        (reset! (:root this) root)
        nil
    )

    #_method
    (defn #_"Object" Var''alterRoot [#_"Var" this, #_"IFn" f, #_"ISeq" args]
        (apply swap! (:root this) f args)
    )

    (defn #_"Var" Var'intern
        ([#_"Namespace" ns, #_"Symbol" sym]
            (Namespace''intern ns, sym)
        )
        ([#_"Namespace" ns, #_"Symbol" sym, #_"Object" root]
            (let [#_"Var" v (Namespace''intern ns, sym)]
                (Var''bindRoot v, root)
                v
            )
        )
    )

    (defn #_"void" Var'pushThreadBindings [#_"{Var Object}" bindings]
        (let [#_"ISeq" l (.get Var'dvals)]
            (loop-when [#_"{Var Volatile}" m (first l) #_"ISeq" s (seq bindings)] (some? s) => (.set Var'dvals, (cons m l))
                (let [#_"IMapEntry" e (first s)]
                    (recur (assoc m (key e) (volatile! (val e))) (next s))
                )
            )
        )
        nil
    )

    (defn #_"void" Var'popThreadBindings []
        (let-when [#_"ISeq" s (.get Var'dvals)] (some? s) => (throw! "pop without matching push")
            (.set Var'dvals, (next s))
        )
        nil
    )

    (defn #_"{Var Object}" Var'getThreadBindings []
        (loop-when [#_"{Var Object}" m (transient {}) #_"ISeq" s (seq (first (.get Var'dvals)))] (some? s) => (persistent! m)
            (let [#_"IMapEntry" e (first s)]
                (recur (assoc! m (key e) @(val e)) (next s))
            )
        )
    )

    (extend-type Var IFn
        (#_"Object" IFn'''invoke
            ([#_"Var" this] (IFn'''invoke (deref this)))
            ([#_"Var" this, #_"Object" arg1] (IFn'''invoke (deref this), arg1))
            ([#_"Var" this, #_"Object" arg1, #_"Object" arg2] (IFn'''invoke (deref this), arg1, arg2))
            ([#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3] (IFn'''invoke (deref this), arg1, arg2, arg3))
            ([#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4] (IFn'''invoke (deref this), arg1, arg2, arg3, arg4))
            ([#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5] (IFn'''invoke (deref this), arg1, arg2, arg3, arg4, arg5))
            ([#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6] (IFn'''invoke (deref this), arg1, arg2, arg3, arg4, arg5, arg6))
            ([#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7] (IFn'''invoke (deref this), arg1, arg2, arg3, arg4, arg5, arg6, arg7))
            ([#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8] (IFn'''invoke (deref this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8))
            ([#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9] (IFn'''invoke (deref this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9))
          #_([#_"Var" this, #_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"Object" arg6, #_"Object" arg7, #_"Object" arg8, #_"Object" arg9 & #_"Object..." args] (IFn'''invoke (deref this), arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, (cast Object'array args)))
        )

        (#_"Object" IFn'''applyTo [#_"Var" this, #_"ISeq" args]
            (IFn'''applyTo (deref this), args)
        )
    )
)
)

(java-ns cloiure.lang.RT

(class-ns RT
    (def #_"Namespace" RT'CLOIURE_NS (§ soon Namespace'findOrCreate 'cloiure.core))

    ;;;
     ; A Namespace object representing the current namespace.
     ;;
    (§ soon def #_"Var" ^:dynamic ^Namespace *ns* RT'CLOIURE_NS)

    (def- #_"int'" RT'ID (atom 0))

    (defn #_"int" RT'nextID [] (swap! RT'ID inc))

    (defn #_"Object" RT'seqOrElse [#_"Object" o]
        (when (some? (seq o))
            o
        )
    )

    (defn #_"int" RT'count [#_"Object" o]
        (cond
            (counted? o)
                (Counted'''count o)
            (nil? o)
                0
            (coll? o)
                (loop-when [#_"int" n 0 #_"ISeq" s (seq o)] (some? s) => n
                    (when (counted? s) => (recur (inc n) (next s))
                        (+ n (Counted'''count s))
                    )
                )
            (instance? CharSequence o)
                (.length ^CharSequence o)
            (map-entry? o)
                2
            (.isArray (class o))
                (Array/getLength o)
            :else
                (throw! (str "count not supported on this type: " (.getName (class o))))
        )
    )

    (defn #_"IPersistentCollection" RT'conj [#_"IPersistentCollection" coll, #_"Object" x]
        (if (some? coll) (ITransientCollection'''conj coll, x) (list x))
    )

    (defn #_"ISeq" RT'cons [#_"Object" x, #_"Seqable" s]
        (cond
            (nil? s) (list x)
            (seq? s) (Cons'new x, s)
            :else    (Cons'new x, (seq s))
        )
    )

    (defn #_"Object" RT'peek [#_"Object" x]
        (when (some? x)
            (IPersistentStack'''peek (cast cloiure.core.IPersistentStack x))
        )
    )

    (defn #_"Object" RT'pop [#_"Object" x]
        (when (some? x)
            (IPersistentStack'''pop (cast cloiure.core.IPersistentStack x))
        )
    )

    (defn #_"Object" RT'get
        ([#_"Object" coll, #_"Object" key]
            (cond
                (satisfies? ILookup coll)
                    (ILookup'''valAt coll, key)
                (nil? coll)
                    nil
                (set? coll)
                    (IPersistentSet'''get coll, key)
                (and (number? key) (or (string? coll) (.isArray (class coll))))
                    (let-when [#_"int" n (.intValue ^Number key)] (< -1 n (count coll))
                        (nth coll n)
                    )
                (satisfies? ITransientSet coll)
                    (ITransientSet'''get coll, key)
            )
        )
        ([#_"Object" coll, #_"Object" key, #_"Object" notFound]
            (cond
                (satisfies? ILookup coll)
                    (ILookup'''valAt coll, key, notFound)
                (nil? coll)
                    notFound
                (set? coll)
                    (if (contains? coll key) (IPersistentSet'''get coll, key) notFound)
                (and (number? key) (or (string? coll) (.isArray (class coll))))
                    (let [#_"int" n (.intValue ^Number key)]
                        (if (< -1 n (count coll)) (nth coll n) notFound)
                    )
                (satisfies? ITransientSet coll)
                    (if (contains? coll key) (ITransientSet'''get coll, key) notFound)
                :else
                    notFound
            )
        )
    )

    (defn #_"Associative" RT'assoc [#_"Object" coll, #_"Object" key, #_"Object" val]
        (if (some? coll)
            (Associative'''assoc (cast cloiure.core.Associative coll), key, val)
            (PersistentArrayMap'new (object-array [ key, val ]))
        )
    )

    (defn #_"Object" RT'contains [#_"Object" coll, #_"Object" key]
        (cond
            (nil? coll)
                false
            (associative? coll)
                (if (Associative'''containsKey coll, key) true false)
            (set? coll)
                (if (IPersistentSet'''contains coll, key) true false)
            (and (number? key) (or (string? coll) (.isArray (class coll))))
                (let [#_"int" n (.intValue ^Number key)]
                    (if (< -1 n (count coll)) true false)
                )
            (satisfies? ITransientSet coll)
                (if (ITransientSet'''contains coll, key) true false)
            (satisfies? ITransientAssociative coll)
                (if (ITransientAssociative'''containsKey coll, key) true false)
            :else
                (throw! (str "contains? not supported on type: " (.getName (class coll))))
        )
    )

    (defn #_"Object" RT'find [#_"Object" coll, #_"Object" key]
        (cond
            (nil? coll)
                nil
            (associative? coll)
                (Associative'''entryAt coll, key)
            (satisfies? ITransientAssociative coll)
                (ITransientAssociative'''entryAt coll, key)
            :else
                (throw! (str "find not supported on type: " (.getName (class coll))))
        )
    )

    ;; takes a seq of key, val, key, val
    ;; returns tail starting at val of matching key if found, else nil

    (defn #_"ISeq" RT'findKey [#_"Keyword" key, #_"ISeq" keyvals]
        (loop-when keyvals (some? keyvals)
            (let-when [#_"ISeq" s (next keyvals)] (some? s) => (throw! "malformed keyword argslist")
                (when-not (= (first keyvals) key) => s
                    (recur (next s))
                )
            )
        )
    )

    (defn #_"Object" RT'dissoc [#_"Object" coll, #_"Object" key]
        (when (some? coll)
            (IPersistentMap'''dissoc (cast cloiure.core.IPersistentMap coll), key)
        )
    )

    (defn #_"Object" RT'nth
        ([#_"Object" coll, #_"int" n]
            (cond
                (indexed? coll)
                    (Indexed'''nth coll, n)
                (nil? coll)
                    nil
                (instance? CharSequence coll)
                    (Character/valueOf (.charAt ^CharSequence coll, n))
                (.isArray (class coll))
                    (Reflector'prepRet (.getComponentType (class coll)), (Array/get coll, n))
                (instance? Matcher coll)
                    (.group ^Matcher coll, n)
                (map-entry? coll)
                    (let [^IMapEntry e coll]
                        (case n 0 (key e) 1 (val e) (throw (IndexOutOfBoundsException.)))
                    )
                (sequential? coll)
                    (loop-when [#_"int" i 0 #_"ISeq" s (seq coll)] (and (<= i n) (some? s)) => (throw (IndexOutOfBoundsException.))
                        (recur-if (< i n) [(inc i) (next s)] => (first s))
                    )
                :else
                    (throw! (str "nth not supported on this type: " (.getName (class coll))))
            )
        )
        ([#_"Object" coll, #_"int" n, #_"Object" notFound]
            (cond
                (indexed? coll)
                    (Indexed'''nth coll, n, notFound)
                (nil? coll)
                    notFound
                (neg? n)
                    notFound
                (instance? CharSequence coll)
                    (let-when [^CharSequence s coll] (< n (.length s)) => notFound
                        (Character/valueOf (.charAt s, n))
                    )
                (.isArray (class coll))
                    (when (< n (Array/getLength coll)) => notFound
                        (Reflector'prepRet (.getComponentType (class coll)), (Array/get coll, n))
                    )
                (instance? Matcher coll)
                    (let-when [^Matcher m coll] (< n (.groupCount m)) => notFound
                        (.group m, n)
                    )
                (map-entry? coll)
                    (let [^IMapEntry e coll]
                        (case n 0 (key e) 1 (val e) notFound)
                    )
                (sequential? coll)
                    (loop-when [#_"int" i 0 #_"ISeq" s (seq coll)] (and (<= i n) (some? s)) => notFound
                        (recur-if (< i n) [(inc i) (next s)] => (first s))
                    )
                :else
                    (throw! (str "nth not supported on this type: " (.getName (class coll))))
            )
        )
    )

    (defn #_"Object"    RT'box [#_"Object"  x] x)
    (defn #_"Character" RT'box-1c [#_"char"    x] (Character/valueOf x))
    (defn #_"Object"    RT'box-1z [#_"boolean" x] (if x true false))
    (defn #_"Object"    RT'box-1Z [#_"Boolean" x] x)
    (defn #_"Number"    RT'box-1b [#_"byte"    x] x)
    (defn #_"Number"    RT'box-1i [#_"int"     x] x)
    (defn #_"Number"    RT'box-1l [#_"long"    x] x)

    (defn #_"boolean" RT'booleanCast-1b [#_"boolean" x]
        x
    )

    (defn #_"boolean" RT'booleanCast [#_"Object" x]
        (if (instance? Boolean x) (.booleanValue ^Boolean x) (some? x))
    )

    (defn #_"int" RT'intCast-1b [#_"byte"  x] x)
    (defn #_"int" RT'intCast-1c [#_"char"  x] x)
    (defn #_"int" RT'intCast-1i [#_"int"   x] x)

    (defn #_"int" RT'intCast-1l [#_"long" x]
        (let [#_"int" i (int x)]
            (when (= i x) => (throw! (str "value out of range for int: " x))
                i
            )
        )
    )

    (defn #_"int" RT'intCast [#_"Object" x]
        (cond
            (instance? Integer x) (.intValue ^Integer x)
            (number? x)           (RT'intCast-1l (long x))
            :else                 (.charValue (cast Character x))
        )
    )

    (defn #_"long" RT'longCast-1b [#_"byte"  x] x)
    (defn #_"long" RT'longCast-1i [#_"int"   x] x)
    (defn #_"long" RT'longCast-1l [#_"long"  x] x)

    (defn #_"long" RT'longCast [#_"Object" x]
        (cond
            (or (instance? Long x) (instance? Integer x) (instance? Byte x))
                (.longValue x)
            (instance? BigInteger x)
                (when (< (.bitLength x) 64) => (throw! (str "value out of range for long: " x))
                    (.longValue x)
                )
            (instance? Ratio x)
                (long (Ratio''bigIntegerValue x))
            (instance? Character x)
                (RT'longCast-1l (.charValue ^Character x))
            :else
                (throw! (str "unexpected value type cast for long: " x))
        )
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
        (when (<= 0 from over (count v)) => (throw (IndexOutOfBoundsException.))
            (if (< from over) (SubVector'new nil, v, from, over) [])
        )
    )

    (defn #_"ISeq" RT'list
        ([] nil)
        ([#_"Object" arg1] (PersistentList'new arg1))
        ([#_"Object" arg1, #_"Object" arg2] (list* arg1 arg2 nil))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3] (list* arg1 arg2 arg3 nil))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4] (list* arg1 arg2 arg3 arg4 nil))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5] (list* arg1 arg2 arg3 arg4 arg5 nil))
    )

    (defn #_"ISeq" RT'list*
        ([#_"Object" arg1, #_"ISeq" args] (cons arg1 args))
        ([#_"Object" arg1, #_"Object" arg2, #_"ISeq" args] (cons arg1 (cons arg2 args)))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"ISeq" args] (cons arg1 (cons arg2 (cons arg3 args))))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"ISeq" args] (cons arg1 (cons arg2 (cons arg3 (cons arg4 args)))))
        ([#_"Object" arg1, #_"Object" arg2, #_"Object" arg3, #_"Object" arg4, #_"Object" arg5, #_"ISeq" args] (cons arg1 (cons arg2 (cons arg3 (cons arg4 (cons arg5 args))))))
    )

    (defn #_"ISeq" RT'arrayToSeq [#_"Object[]" a]
        (loop-when-recur [#_"ISeq" s nil #_"int" i (dec (alength a))] (<= 0 i) [(cons (aget a i) s) (dec i)] => s)
    )

    (defn #_"Object[]" RT'objectArray [#_"Object" sizeOrSeq]
        (if (number? sizeOrSeq)
            (make-array Object (.intValue ^Number sizeOrSeq))
            (let [#_"ISeq" s (seq sizeOrSeq) #_"int" size (count s) #_"Object[]" a (make-array Object size)]
                (loop-when-recur [#_"int" i 0 s s] (and (< i size) (some? s)) [(inc i) (next s)]
                    (aset a i (first s))
                )
                a
            )
        )
    )

    (defn #_"Object[]" RT'seqToArray [#_"ISeq" s]
        (let [#_"Object[]" a (make-array Object (count s))]
            (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (next s)]
                (aset a i (first s))
            )
            a
        )
    )

    (defn #_"?[]" RT'seqToTypedArray [#_"Class" type, #_"ISeq" s]
        (let [#_"?[]" a (make-array (or type (class (first s)) Object) (count s))]
            (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (next s)]
                (aset a i (first s))
            )
            a
        )
    )

    (defn #_"Object[]" RT'toArray [#_"Object" coll]
        (cond
            (nil? coll)
                (make-array Object 0)
            (instance? Object'array coll)
                coll
            (indexed? coll)
                (let [#_"int" n (count coll) #_"Object[]" a (make-array Object n)]
                    (dotimes [#_"int" i n]
                        (aset a i (nth coll i))
                    )
                    a
                )
            (satisfies? Seqable coll)
                (RT'seqToArray (seq coll))
            (string? coll)
                (let [#_"char[]" chars (.toCharArray coll)
                      #_"Object[]" a (make-array Object (alength chars))]
                    (dotimes [#_"int" i (alength chars)]
                        (aset a i (aget chars i))
                    )
                    a
                )
            (.isArray (class coll))
                (let [#_"ISeq" s (seq coll)
                      #_"Object[]" a (make-array Object (count s))]
                    (loop-when-recur [#_"int" i 0 s s] (< i (alength a)) [(inc i) (next s)]
                        (aset a i (first s))
                    )
                    a
                )
            :else
                (throw! (str "unable to convert: " (class coll) " to Object[]"))
        )
    )

    (defn #_"int" RT'length [#_"ISeq" s]
        (loop-when-recur [#_"int" i 0 s s] (some? s) [(inc i) (next s)] => i)
    )

    (defn #_"int" RT'boundedLength [#_"ISeq" s, #_"int" limit]
        (loop-when-recur [#_"int" i 0 s s] (and (some? s) (<= i limit)) [(inc i) (next s)] => i)
    )

    (defn #_"boolean" RT'isReduced [#_"Object" r] (instance? Reduced r))

    (declare LispReader'read)

    (defn #_"Object" RT'readString [#_"String" s]
        (let [#_"PushbackReader" r (PushbackReader. (java.io.StringReader. s))]
            (LispReader'read r)
        )
    )

    (declare pr-on)

    (defn #_"void" RT'print [#_"Object" x, #_"Writer" w]
        (pr-on x w) ;; call multimethod
        nil
    )

    (defn #_"String" RT'printString [#_"Object" x]
        (let [#_"StringWriter" sw (StringWriter.)]
            (RT'print x, sw)
            (.toString sw)
        )
    )

    (defn #_"Class" RT'classForName
        ([#_"String" name] (RT'classForName name, true))
        ([#_"String" name, #_"boolean" load?]
            (let [#_"ClassLoader" loader (Compiler'baseLoader)
                  #_"Class" c
                    (when-not (instance? DynamicClassLoader loader)
                        (DynamicClassLoader'findInMemoryClass name)
                    )]
                (or c (Class/forName name, load?, loader))
            )
        )
    )

    (defn #_"Class" RT'classForNameNonLoading [#_"String" name]
        (RT'classForName name, false)
    )
)
)

(clojure-ns cloiure.core

;;;
 ; Creates a new list containing the items.
 ;;
(§ def list (PersistentList'creator))

;;;
 ; Returns a new seq where x is the first element and seq is the rest.
 ;;
(§ def cons (fn* cons [x seq] (RT'cons x seq)))

;; during bootstrap we don't have destructuring let, loop or fn, will redefine later

(§ def ^:macro let  (fn* let  [&form &env & decl] (cons 'let* decl)))
(§ def ^:macro loop (fn* loop [&form &env & decl] (cons 'loop* decl)))
(§ def ^:macro fn   (fn* fn   [&form &env & decl] (with-meta (cons 'fn* decl) (meta &form))))

;;;
 ; conj[oin].
 ; Returns a new collection with the items 'added'. (conj nil item) returns (item).
 ; The 'addition' may happen at different 'places' depending on the concrete type.
 ;;
(defn conj
    ([] [])
    ([coll] coll)
    ([coll x] (RT'conj coll x))
    ([coll x & s] (recur-if s [(conj coll x) (first s) (next s)] => (conj coll x)))
)

;;;
 ; assoc[iate].
 ; When applied to a map, returns a new map of the same (hashed/sorted) type, that contains the mapping of key(s) to val(s).
 ; When applied to a vector, returns a new vector that contains val at index. Note - index must be <= (count vector).
 ;;
(§ defn assoc
    ([a k v] (RT'assoc a k v))
    ([a k v & kvs]
        (let-when [a (assoc a k v)] kvs => a
            (when (next kvs) => (throw! "assoc expects even number of arguments after map/vector, found odd number")
                (recur a (first kvs) (second kvs) (nnext kvs))
            )
        )
    )
)

(defn- ^:dynamic assert-valid-fdecl [_])

(declare some)
(declare mapv)

(defn- sigs [s]
    (assert-valid-fdecl s)
    (letfn [(sig- [s]
                (let [v (first s) s (next s) v (if (= '&form (first v)) (subvec v 2) v)] ;; elide implicit macro args
                    (let-when [m (first s)] (and (map? m) (next s)) => v
                        (with-meta v (conj (or (meta v) {}) m))
                    )
                )
            )
            (tag- [s]
                (let [v (sig- s) m (meta v) ^Symbol tag (:tag m)]
                    (when (and (symbol? tag) (not (some #{\.} (:name tag))) (not (Interop'maybeSpecialTag tag))) => v
                        (let [c (Interop'maybeClass tag false)]
                            (when c => v
                                (with-meta v (assoc m :tag (symbol (.getName c))))
                            )
                        )
                    )
                )
            )]
        (when (seq? (first s)) => (list (tag- s))
            (seq (mapv tag- s))
        )
    )
)

;;;
 ; Return a seq of all but the last item in coll, in linear time.
 ;;
(defn butlast [s] (loop-when-recur [v [] s s] (next s) [(conj v (first s)) (next s)] => (seq v)))

;;;
 ; Same as (def name (fn [params*] exprs*)) or (def name (fn ([params*] exprs*)+)) with any attrs added to the var metadata.
 ;;
(§ defmacro defn [&form &env fname & s]
    ;; note: cannot delegate this check to def because of the call to (with-meta name ...)
    (when (symbol? fname) => (throw! "first argument to defn must be a symbol")
        (let [m (if (map?    (first s)) (first s)        {})
              s (if (map?    (first s)) (next s)          s)
              s (if (vector? (first s)) (list s)          s)
              m (if (map?    (last  s)) (conj m (last s)) m)
              s (if (map?    (last  s)) (butlast s)       s)
              m (conj {:arglists (list 'quote (sigs s))} m)
              m (let [inline (:inline m) ifn (first inline) iname (second inline)]
                    (when (and (= 'fn ifn) (not (symbol? iname))) => m
                        ;; inserts the same fn name to the inline fn if it does not have one
                        (assoc m :inline (cons ifn (cons (symbol (str (:name fname) "__inliner")) (next inline))))
                    )
                )
              m (conj (or (meta fname) {}) m)]
            (list 'def (with-meta fname m)
                ;; todo - restore propagation of fn name
                ;; must figure out how to convey primitive hints to self calls first
                (with-meta (cons `fn s) {:rettag (:tag m)})
            )
        )
    )
)

;;;
 ; Returns an array of Objects containing the contents of coll.
 ;;
(§ defn ^objects to-array [coll] (RT'toArray coll))

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
    ([a b c d e f & args] (LazilyPersistentVector'create (cons a (cons b (cons c (cons d (cons e (cons f args))))))))
)

;;;
 ; Creates a new vector containing the contents of coll.
 ; Java arrays will be aliased and should not be modified.
 ;;
(§ defn vec [coll]
    (when (and (vector? coll) (satisfies? IObj coll)) => (LazilyPersistentVector'create coll)
        (with-meta coll nil)
    )
)

;;;
 ; keyval => key val
 ; Returns a new hash map with supplied mappings.
 ; If any keys are equal, they are handled as if by repeated uses of assoc.
 ;;
(§ defn hash-map
    ([] {})
    ([& keyvals] (PersistentHashMap'create keyvals))
)

;;;
 ; Returns a new hash set with supplied keys.
 ; Any equal keys are handled as if by repeated uses of conj.
 ;;
(§ defn hash-set
    ([] #{})
    ([& keys] (PersistentHashSet'create keys))
)

;;;
 ; keyval => key val
 ; Returns a new sorted map with supplied mappings.
 ; If any keys are equal, they are handled as if by repeated uses of assoc.
 ;;
(defn sorted-map [& keyvals] (PersistentTreeMap'create keyvals))

;;;
 ; keyval => key val
 ; Returns a new sorted map with supplied mappings, using the supplied comparator.
 ; If any keys are equal, they are handled as if by repeated uses of assoc.
 ;;
(defn sorted-map-by [comp & keyvals] (PersistentTreeMap'create comp keyvals))

;;;
 ; Returns a new sorted set with supplied keys.
 ; Any equal keys are handled as if by repeated uses of conj.
 ;;
(defn sorted-set [& keys] (PersistentTreeSet'create keys))

;;;
 ; Returns a new sorted set with supplied keys, using the supplied comparator.
 ; Any equal keys are handled as if by repeated uses of conj.
 ;;
(defn sorted-set-by [comp & keys] (PersistentTreeSet'create comp keys))

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
 ; Returns a Symbol with the given namespace and name.
 ;;
(defn ^Symbol symbol
    ([name] (if (symbol? name) name (Symbol'intern name)))
    ([ns name] (Symbol'intern ns name))
)

;;;
 ; Returns a new symbol with a unique name. If a prefix string is supplied,
 ; the name is prefix# where # is some unique number.
 ; If prefix is not supplied, the prefix is 'G__'.
 ;;
(defn gensym
    ([] (gensym "G__"))
    ([prefix] (symbol (str prefix (RT'nextID))))
)

;;;
 ; Returns a Keyword with the given namespace and name.
 ; Do not use ":" in the keyword strings, it will be added automatically.
 ;;
(defn ^Keyword keyword
    ([name]
        (cond
            (keyword? name) name
            (symbol? name) (Keyword'intern ^Symbol name)
            (string? name) (Keyword'intern (symbol ^String name))
        )
    )
    ([ns name] (Keyword'intern (symbol ns name)))
)

;;;
 ; Returns a Keyword with the given namespace and name if one already exists.
 ; This function will not intern a new keyword. If the keyword has not already
 ; been interned, it will return nil.
 ; Do not use ":" in the keyword strings, it will be added automatically.
 ;;
(defn ^Keyword find-keyword
    ([name]
        (cond
            (keyword? name) name
            (symbol? name) (Keyword'find ^Symbol name)
            (string? name) (Keyword'find (symbol ^String name))
        )
    )
    ([ns name] (Keyword'find (symbol ns name)))
)

(defn- spread [s]
    (cond
        (nil? s) nil
        (nil? (next s)) (seq (first s))
        :else (cons (first s) (spread (next s)))
    )
)

;;;
 ; Creates a new seq containing the items prepended to the rest,
 ; the last of which will be treated as a sequence.
 ;;
(defn list*
    ([s] (seq s))
    ([a s] (cons a s))
    ([a b s] (cons a (cons b s)))
    ([a b c s] (cons a (cons b (cons c s))))
    ([a b c d & s] (cons a (cons b (cons c (cons d (spread s))))))
)

;;;
 ; Applies fn f to the argument list formed by prepending intervening arguments to args.
 ;;
(defn apply
    ([^cloiure.core.IFn f s] (IFn'''applyTo f (seq s)))
    ([^cloiure.core.IFn f a s] (IFn'''applyTo f (list* a s)))
    ([^cloiure.core.IFn f a b s] (IFn'''applyTo f (list* a b s)))
    ([^cloiure.core.IFn f a b c s] (IFn'''applyTo f (list* a b c s)))
    ([^cloiure.core.IFn f a b c d & s] (IFn'''applyTo f (cons a (cons b (cons c (cons d (spread s)))))))
)

;;;
 ; Takes a body of expressions that returns an ISeq or nil, and yields
 ; a Seqable object that will invoke the body only the first time seq
 ; is called, and will cache the result and return it on all subsequent
 ; seq calls. See also - realized?
 ;;
(defmacro lazy-seq [& body] `(LazySeq'new (^{:once true} fn* [] ~@body)))

;;;
 ; Returns a lazy seq representing the concatenation of the elements in the supplied colls.
 ;;
(defn concat
    ([] (lazy-seq nil))
    ([x] (lazy-seq x))
    ([x y]
        (lazy-seq
            (let-when [s (seq x)] s => y
                (cons (first s) (concat (next s) y))
            )
        )
    )
    ([x y & z]
        (letfn [(cat- [s z]
                    (lazy-seq
                        (let [s (seq s)]
                            (cond
                                s (cons (first s) (cat- (next s) z))
                                z (cat- (first z) (next z))
                            )
                        )
                    )
                )]
            (cat- (concat x y) z)
        )
    )
)

;;;
 ; Takes a body of expressions and yields a Delay object that will invoke
 ; the body only the first time it is forced (with force or deref/@), and
 ; will cache the result and return it on all subsequent force calls.
 ; See also - realized?
 ;;
(defmacro delay [& body] `(Delay'new (^{:once true} fn* [] ~@body)))

;;;
 ; Returns true if x is a Delay created with delay.
 ;;
(defn delay? [x] (instance? Delay x))

;;;
 ; If x is a Delay, returns the (possibly cached) value of its expression, else returns x.
 ;;
(defn force [x] (Delay'force x))

;;;
 ; Returns the number of items in the collection. (count nil) returns 0.
 ; Also works on strings, arrays, collections and maps.
 ;;
(§ defn count [s] (RT'count s))

;;;
 ; Coerce to boolean/int/long.
 ;;
(defn boolean [x] (RT'booleanCast x))
(defn int     [x] (RT'intCast     x))
(defn long    [x] (RT'longCast    x))

;;;
 ; Returns the value at the index.
 ; get returns nil if index out of bounds, nth throws an exception unless not-found is supplied.
 ; nth also works for strings, arrays, regex matchers and lists, and, in O(n) time, for sequences.
 ;;
(defn nth
    ([s i]           (RT'nth s i          ))
    ([s i not-found] (RT'nth s i not-found))
)

;;;
 ; Returns a seq of the items in coll in reverse order. Not lazy.
 ;;
(defn reverse [s] (into () s))

;;;
 ; Takes a fn f and returns a fn that takes the same arguments as f,
 ; has the same effects, if any, and returns the opposite truth value.
 ;;
(defn complement [f]
    (fn
        ([] (not (f)))
        ([x] (not (f x)))
        ([x y] (not (f x y)))
        ([x y & s] (not (apply f x y s)))
    )
)

;;;
 ; For a list or queue, same as first, for a vector, same as, but much
 ; more efficient than, last. If the collection is empty, returns nil.
 ;;
(defn peek [coll] (RT'peek coll))

;;;
 ; For a list or queue, returns a new list/queue without the first item,
 ; for a vector, returns a new vector without the last item.
 ; If the collection is empty, throws an exception.
 ; Note - not the same as next/butlast.
 ;;
(defn pop [coll] (RT'pop coll))

;;;
 ; Returns true if key is present in the given collection, otherwise
 ; returns false. Note that for numerically indexed collections, like
 ; vectors and Java arrays, this tests if the numeric key is within the
 ; range of indexes. 'contains?' operates constant or logarithmic time;
 ; it will not perform a linear search for a value. See also 'some'.
 ;;
(defn contains? [coll key] (RT'contains coll key))

;;;
 ; Returns the value mapped to key, not-found or nil if key not present.
 ;;
(defn get
    ([coll key          ] (RT'get coll key          ))
    ([coll key not-found] (RT'get coll key not-found))
)

;;;
 ; dissoc[iate]. Returns a new map of the same (hashed/sorted) type,
 ; that does not contain a mapping for key(s).
 ;;
(defn dissoc
    ([m] m)
    ([m k] (RT'dissoc m k))
    ([m k & ks] (let [m (dissoc m k)] (recur-if ks [m (first ks) (next ks)] => m)))
)

;;;
 ; disj[oin]. Returns a new set of the same (hashed/sorted) type,
 ; that does not contain key(s).
 ;;
(defn disj
    ([s] s)
    ([^cloiure.core.IPersistentSet s k]
        (when s
            (IPersistentSet'''disj s k)
        )
    )
    ([s k & ks]
        (when s
            (let [s (disj s k)]
                (recur-if ks [s (first ks) (next ks)] => s)
            )
        )
    )
)

;;;
 ; Returns the map entry for k, or nil if key not present.
 ;;
(defn find [m k] (RT'find m k))

;;;
 ; Returns a map containing only those entries in m whose key is in keys.
 ;;
(defn select-keys [m keys] (with-meta (into {} (map #(find m %) keys)) (meta m)))

;;;
 ; Returns the key/value of/in the map entry.
 ;;
(§ defn key [^cloiure.core.IMapEntry e] (IMapEntry'''key e))
(§ defn val [^cloiure.core.IMapEntry e] (IMapEntry'''val e))

;;;
 ; Returns a sequence of the map's keys/values, in the same order as (seq m).
 ;;
(§ defn keys [m] (map key m))
(defn vals [m] (map val m))

;;;
 ; Returns, in constant time, a seq of the items in rev (which can be a vector or sorted-map), in reverse order.
 ; If rev is empty, returns nil.
 ;;
(defn rseq [^cloiure.core.Reversible s] (Reversible'''rseq s))

;;;
 ; Return true if x is a symbol or keyword.
 ;;
(defn ident? [x] (or (symbol? x) (keyword? x)))

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
(defmacro ..
    ([x form] `(. ~x ~form))
    ([x form & s] `(.. (. ~x ~form) ~@s))
)

;;;
 ; Threads the expr through the forms. Inserts x as the second item
 ; in the first form, making a list of it if it is not a list already.
 ; If there are more forms, inserts the first form as the second item
 ; in second form, etc.
 ;;
(§ defmacro -> [x & s]
    (when s => x
        (recur &form &env
            (let-when [f (first s)] (seq? f) => (list f x)
                (with-meta `(~(first f) ~x ~@(next f)) (meta f))
            )
            (next s)
        )
    )
)

;;;
 ; Threads the expr through the forms. Inserts x as the last item
 ; in the first form, making a list of it if it is not a list already.
 ; If there are more forms, inserts the first form as the last item
 ; in second form, etc.
 ;;
(defmacro ->> [x & s]
    (when s => x
        (recur &form &env
            (let-when [f (first s)] (seq? f) => (list f x)
                (with-meta `(~(first f) ~@(next f) ~x) (meta f))
            )
            (next s)
        )
    )
)

(defmacro- assert-args [& pairs]
    (§ soon
        `(when ~(first pairs) => (throw! (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))
            ~(when-some [s (nnext pairs)]
                (list* `assert-args s)
            )
        )
    )
)

;;;
 ; bindings => binding-form test
 ;
 ; If test is true, evaluates then with binding-form bound to the value of test, if not, yields else.
 ;;
(defmacro if-let
    ([v then] `(if-let ~v ~then nil))
    ([v then else & _]
        (assert-args
            (vector? v) "a vector for its binding"
            (nil? _) "1 or 2 forms after binding vector"
            (= 2 (count v)) "exactly 2 forms in binding vector"
        )
        `(let [_# ~(v 1)]
            (if _# (let [~(v 0) _#] ~then) ~else)
        )
    )
)

;;;
 ; bindings => binding-form test
 ;
 ; When test is true, evaluates body with binding-form bound to the value of test.
 ;;
(defmacro when-let [v & body]
    (assert-args
        (vector? v) "a vector for its binding"
        (= 2 (count v)) "exactly 2 forms in binding vector"
    )
    `(let [_# ~(v 1)]
        (when _# (let [~(v 0) _#] ~@body))
    )
)

;;;
 ; bindings => binding-form test
 ;
 ; If test is not nil, evaluates then with binding-form bound to the value of test, if not, yields else.
 ;;
(§ defmacro if-some
    ([v then] `(if-some ~v ~then nil))
    ([v then else & _]
        (assert-args
            (vector? v) "a vector for its binding"
            (nil? _) "1 or 2 forms after binding vector"
            (= 2 (count v)) "exactly 2 forms in binding vector"
        )
        `(let [_# ~(v 1)]
            (if (nil? _#) ~else (let [~(v 0) _#] ~then))
        )
    )
)

;;;
 ; bindings => binding-form test
 ;
 ; When test is not nil, evaluates body with binding-form bound to the value of test.
 ;;
(§ defmacro when-some [v & body]
    (assert-args
        (vector? v) "a vector for its binding"
        (= 2 (count v)) "exactly 2 forms in binding vector"
    )
    `(let [_# ~(v 1)]
        (if (nil? _#) nil (let [~(v 0) _#] ~@body))
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
(defn push-thread-bindings [bindings] (Var'pushThreadBindings bindings))

;;;
 ; Pop one set of bindings pushed with push-binding before.
 ; It is an error to pop bindings without pushing before.
 ;;
(defn pop-thread-bindings [] (Var'popThreadBindings))

;;;
 ; Get a map with the Var/value pairs which is currently in effect for the current thread.
 ;;
(defn get-thread-bindings [] (Var'getThreadBindings))

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
                (loop-when-recur [v [] s (seq var-vals)] s [(conj (conj v `(var ~(first s))) (second s)) (next (next s))] => (seq v))
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
(defn with-bindings* [binding-map f & args]
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
(defmacro with-bindings [binding-map & body]
    `(with-bindings* ~binding-map (fn [] ~@body))
)

;;;
 ; Returns a function, which will install the same bindings in effect as in
 ; the thread at the time bound-fn* was called and then call f with any given
 ; arguments. This may be used to define a helper function which runs on a
 ; different thread, but needs the same bindings in place.
 ;;
(defn- bound-fn* [f]
    (let [bindings (get-thread-bindings)]
        (fn [& args] (apply with-bindings* bindings f args))
    )
)

;;;
 ; Returns a function defined by the given tail, which will install the
 ; same bindings in effect as in the thread at the time bound-fn was called.
 ; This may be used to define a helper function which runs on a different
 ; thread, but needs the same bindings in place.
 ;;
(defmacro bound-fn [& tail] `(bound-fn* (fn ~@tail)))

;;;
 ; Returns the global var named by the namespace-qualified symbol,
 ; or nil if no var with that name.
 ;;
(defn find-var [sym] (Var'find sym))

;;;
 ; Takes a set of functions and returns a fn that is the composition
 ; of those fns. The returned fn takes a variable number of args,
 ; applies the rightmost of fns to the args, the next
 ; fn (right-to-left) to the result, etc.
 ;;
(defn comp
    ([] identity)
    ([f] f)
    ([f g]
        (fn
            ([] (f (g)))
            ([x] (f (g x)))
            ([x y] (f (g x y)))
            ([x y & z] (f (apply g x y z)))
        )
    )
    ([f g & fs] (reduce comp (list* f g fs)))
)

;;;
 ; Takes a set of functions and returns a fn that is the juxtaposition
 ; of those fns. The returned fn takes a variable number of args, and
 ; returns a vector containing the result of applying each fn to the
 ; args (left-to-right).
 ; ((juxt a b c) x) => [(a x) (b x) (c x)]
 ;;
(defn juxt
    ([f]
        (fn
            ([] [(f)])
            ([x] [(f x)])
            ([x y] [(f x y)])
            ([x y & z] [(apply f x y z)])
        )
    )
    ([f g]
        (fn
            ([] [(f) (g)])
            ([x] [(f x) (g x)])
            ([x y] [(f x y) (g x y)])
            ([x y & z] [(apply f x y z) (apply g x y z)])
        )
    )
    ([f g h]
        (fn
            ([] [(f) (g) (h)])
            ([x] [(f x) (g x) (h x)])
            ([x y] [(f x y) (g x y) (h x y)])
            ([x y & z] [(apply f x y z) (apply g x y z) (apply h x y z)])
        )
    )
    ([f g h & fs]
        (let [fs (list* f g h fs)]
            (fn
                ([] (reduce #(conj %1 (%2)) [] fs))
                ([x] (reduce #(conj %1 (%2 x)) [] fs))
                ([x y] (reduce #(conj %1 (%2 x y)) [] fs))
                ([x y & z] (reduce #(conj %1 (apply %2 x y z)) [] fs))
            )
        )
    )
)

;;;
 ; Takes a function f and fewer than the normal arguments to f, and
 ; returns a fn that takes a variable number of additional args. When
 ; called, the returned function calls f with args + additional args.
 ;;
(defn partial
    ([f] f)
    ([f a]
        (fn
            ([] (f a))
            ([x] (f a x))
            ([x y] (f a x y))
            ([x y z] (f a x y z))
            ([x y z & args] (apply f a x y z args))
        )
    )
    ([f a b]
        (fn
            ([] (f a b))
            ([x] (f a b x))
            ([x y] (f a b x y))
            ([x y z] (f a b x y z))
            ([x y z & args] (apply f a b x y z args))
        )
    )
    ([f a b c]
        (fn
            ([] (f a b c))
            ([x] (f a b c x))
            ([x y] (f a b c x y))
            ([x y z] (f a b c x y z))
            ([x y z & args] (apply f a b c x y z args))
        )
    )
    ([f a b c & more]
        (fn [& args] (apply f a b c (concat more args)))
    )
)

;;;
 ; Returns true if (f? x) is logical true for every x in coll, else false.
 ;;
(defn ^Boolean every? [f? s]
    (cond
        (nil? (seq s)) true
        (f? (first s)) (recur f? (next s))
        :else false
    )
)

;;;
 ; Returns false if (f? x) is logical true for every x in coll, else true.
 ;;
(def ^Boolean not-every? (comp not every?))

;;;
 ; Returns the first logical true value of (f? x) for any x in coll,
 ; else nil. One common idiom is to use a set as f?, for example
 ; this will return :fred if :fred is in the sequence, otherwise nil:
 ; (some #{:fred} coll).
 ;;
(defn some [f? s]
    (when (seq s)
        (or (f? (first s)) (recur f? (next s)))
    )
)

;;;
 ; Returns false if (f? x) is logical true for any x in coll, else true.
 ;;
(def ^Boolean not-any? (comp not some))

;;;
 ; Returns a lazy sequence consisting of the result of applying f to
 ; the set of first items of each coll, followed by applying f to the
 ; set of second items in each coll, until any one of the colls is
 ; exhausted. Any remaining items in other colls are ignored. Function
 ; f should accept number-of-colls arguments. Returns a transducer when
 ; no collection is provided.
 ;;
(defn map
    ([f]
        (fn [g]
            (fn
                ([] (g))
                ([x] (g x))
                ([x y] (g x (f y)))
                ([x y & s] (g x (apply f y s)))
            )
        )
    )
    ([f s]
        (lazy-seq
            (when-some [s (seq s)]
                (cons (f (first s)) (map f (next s)))
            )
        )
    )
    ([f s1 s2]
        (lazy-seq
            (let-when [s1 (seq s1) s2 (seq s2)] (and s1 s2)
                (cons (f (first s1) (first s2)) (map f (next s1) (next s2)))
            )
        )
    )
    ([f s1 s2 s3]
        (lazy-seq
            (let-when [s1 (seq s1) s2 (seq s2) s3 (seq s3)] (and s1 s2 s3)
                (cons (f (first s1) (first s2) (first s3)) (map f (next s1) (next s2) (next s3)))
            )
        )
    )
    ([f s1 s2 s3 & z]
        (letfn [(map- [s]
                    (lazy-seq
                        (let-when [s (map seq s)] (every? identity s)
                            (cons (map first s) (map- (map next s)))
                        )
                    )
                )]
            (map #(apply f %) (map- (conj z s3 s2 s1)))
        )
    )
)

(declare cat)

;;;
 ; Returns the result of applying concat to the result of applying map to f and colls.
 ; Thus function f should return a collection.
 ; Returns a transducer when no collections are provided.
 ;;
(defn mapcat
    ([f] (comp (map f) cat))
    ([f & s] (apply concat (apply map f s)))
)

;;;
 ; Returns a lazy sequence of the items in coll for which (f? item) returns logical true.
 ; f? must be free of side-effects.
 ; Returns a transducer when no collection is provided.
 ;;
(defn filter
    ([f?]
        (fn [g]
            (fn
                ([] (g))
                ([x] (g x))
                ([x y] (if (f? y) (g x y) x))
            )
        )
    )
    ([f? s]
        (lazy-seq
            (when-some [s (seq s)]
                (let-when [x (first s) s (next s)] (f? x) => (filter f? s)
                    (cons x (filter f? s))
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of the items in coll for which (f? item) returns logical false.
 ; f? must be free of side-effects.
 ; Returns a transducer when no collection is provided.
 ;;
(defn remove
    ([f?]   (filter (complement f?)  ))
    ([f? s] (filter (complement f?) s))
)

;;;
 ; Wraps x in a way such that a reduce will terminate with the value x.
 ;;
(defn reduced [x] (Reduced'new x))

;;;
 ; Returns true if x is the result of a call to reduced.
 ;;
(defn reduced? [x] (RT'isReduced x))

;;;
 ; If x is already reduced?, returns it, else returns (reduced x).
 ;;
(defn ensure-reduced [x] (if (reduced? x) x (reduced x)))

;;;
 ; If x is reduced?, returns (deref x), else returns x.
 ;;
(defn unreduced [x] (if (reduced? x) (deref x) x))

;;;
 ; Returns a lazy sequence of the first n items in coll, or all items if there are fewer than n.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(defn take
    ([n]
        (fn [g]
            (let [n' (volatile! n)]
                (fn
                    ([] (g))
                    ([x] (g x))
                    ([x y]
                        (let [n @n' m (vswap! n' dec) x (if (pos? n) (g x y) x)]
                            (if (pos? m) x (ensure-reduced x))
                        )
                    )
                )
            )
        )
    )
    ([n s]
        (lazy-seq
            (when (pos? n)
                (when-some [s (seq s)]
                    (cons (first s) (take (dec n) (next s)))
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of successive items from coll while (f? item) returns logical true.
 ; f? must be free of side-effects.
 ; Returns a transducer when no collection is provided.
 ;;
(defn take-while
    ([f?]
        (fn [g]
            (fn
                ([] (g))
                ([x] (g x))
                ([x y] (if (f? y) (g x y) (reduced x)))
            )
        )
    )
    ([f? s]
        (lazy-seq
            (when-some [s (seq s)]
                (when (f? (first s))
                    (cons (first s) (take-while f? (next s)))
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of all but the first n items in coll.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(defn drop
    ([n]
        (fn [g]
            (let [n' (volatile! n)]
                (fn
                    ([] (g))
                    ([x] (g x))
                    ([x y] (if (neg? (vswap! n' dec)) (g x y) x))
                )
            )
        )
    )
    ([n s]
        (letfn [(drop- [n s]
                    (let [s (seq s)]
                        (recur-if (and (pos? n) s) [(dec n) (next s)] => s)
                    )
                )]
            (lazy-seq (drop- n s))
        )
    )
)

;;;
 ; Return a lazy sequence of all but the last n (default 1) items in coll.
 ;;
(defn drop-last
    ([s] (drop-last 1 s))
    ([n s] (map (fn [x _] x) s (drop n s)))
)

;;;
 ; Returns a seq of the last n items in coll. Depending on the type of coll
 ; may be no better than linear time. For vectors, see also subvec.
 ;;
(defn take-last [n coll]
    (loop-when-recur [s (seq coll) z (seq (drop n coll))] z [(next s) (next z)] => s)
)

;;;
 ; Returns a lazy sequence of the items in coll starting from the
 ; first item for which (f? item) returns logical false.
 ; Returns a stateful transducer when no collection is provided.
 ;;
(defn drop-while
    ([f?]
        (fn [g]
            (let [drop? (volatile! true)]
                (fn
                    ([] (g))
                    ([x] (g x))
                    ([x y]
                        (when-not (and @drop? (f? y)) => x
                            (vreset! drop? nil)
                            (g x y)
                        )
                    )
                )
            )
        )
    )
    ([f? s]
        (letfn [(drop- [f? s]
                    (let [s (seq s)]
                        (recur-if (and s (f? (first s))) [f? (next s)] => s)
                    )
                )]
            (lazy-seq (drop- f? s))
        )
    )
)

;;;
 ; Returns a lazy (infinite!) sequence of repetitions of the items in coll.
 ;;
(defn cycle [s] (Cycle'create s))

;;;
 ; Returns a vector of [(take n coll) (drop n coll)].
 ;;
(defn split-at [n s] [(take n s) (drop n s)])

;;;
 ; Returns a vector of [(take-while f? coll) (drop-while f? coll)].
 ;;
(defn split-with [f? s] [(take-while f? s) (drop-while f? s)])

;;;
 ; Returns a lazy (infinite!, or length n if supplied) sequence of xs.
 ;;
(defn repeat
    ([  x] (Repeat'create   x))
    ([n x] (Repeat'create n x))
)

;;;
 ; Returns a lazy sequence of x, (f x), (f (f x)), etc.
 ; f must be free of side-effects.
 ;;
(defn iterate [f x] (Iterate'create f x))

;;;
 ; Returns a lazy seq of nums from start (inclusive) to end (exclusive),
 ; by step, where start defaults to 0, step to 1, and end to infinity.
 ; When step is equal to 0, returns an infinite sequence of start.
 ; When start is equal to end, returns empty list.
 ;;
(defn range
    ([] (iterate inc 0))
    ([end] (Range'create end))
    ([start end] (Range'create start end))
    ([start end step] (Range'create start end step))
)

;;;
 ; Returns a map that consists of the rest of the maps conj-ed onto
 ; the first. If a key occurs in more than one map, the mapping from
 ; the latter (left-to-right) will be the mapping in the result.
 ;;
(§ defn merge [& maps]
    (when (some identity maps)
        (reduce #(conj (or %1 {}) %2) maps)
    )
)

;;;
 ; Returns a map that consists of the rest of the maps conj-ed onto
 ; the first. If a key occurs in more than one map, the mapping(s)
 ; from the latter (left-to-right) will be combined with the mapping in
 ; the result by calling (f val-in-result val-in-latter).
 ;;
(defn merge-with [f & maps]
    (when (some identity maps)
        (letfn [(merge- [m e]
                    (let [k (key e) v (val e)]
                        (assoc m k (if (contains? m k) (f (get m k) v) v))
                    )
                )]
            (reduce #(reduce merge- (or %1 {}) %2) maps)
        )
    )
)

;;;
 ; Returns a map with the keys mapped to the corresponding vals.
 ;;
(defn zipmap [keys vals]
    (loop-when-recur [m (transient {}) ks (seq keys) vs (seq vals)]
                     (and ks vs)
                     [(assoc! m (first ks) (first vs)) (next ks) (next vs)]
                  => (persistent! m)
    )
)

;;;
 ; Returns the lines of text from r as a lazy sequence of strings.
 ; r must implement java.io.BufferedReader.
 ;;
(defn line-seq [^BufferedReader r]
    (when-some [line (.readLine r)]
        (cons line (lazy-seq (line-seq r)))
    )
)

;;;
 ; Returns an implementation of java.util.Comparator based upon f?.
 ;;
(defn comparator [f?]
    (fn [x y]
        (cond (f? x y) -1 (f? y x) 1 :else 0)
    )
)

;;;
 ; Returns a sorted sequence of the items in coll.
 ; If no comparator is supplied, uses compare. comparator must implement java.util.Comparator.
 ; Guaranteed to be stable: equal elements will not be reordered.
 ; If coll is a Java array, it will be modified. To avoid this, sort a copy of the array.
 ;;
(defn sort
    ([s] (sort compare s))
    ([^Comparator comp s]
        (when (seq s) => ()
            (let [a (to-array s)]
                (Arrays/sort a comp)
                (seq a)
            )
        )
    )
)

;;;
 ; Returns a sorted sequence of the items in coll, where the sort order is determined by comparing (keyfn item).
 ; If no comparator is supplied, uses compare. comparator must implement java.util.Comparator.
 ; Guaranteed to be stable: equal elements will not be reordered.
 ; If coll is a Java array, it will be modified. To avoid this, sort a copy of the array.
 ;;
(defn sort-by
    ([f s] (sort-by f compare s))
    ([f ^Comparator comp s] (sort #(.compare comp (f %1) (f %2)) s))
)

;;;
 ; When lazy sequences are produced via functions that have side
 ; effects, any effects other than those needed to produce the first
 ; element in the seq do not occur until the seq is consumed. dorun can
 ; be used to force any effects. Walks through the successive nexts of
 ; the seq, does not retain the head and returns nil.
 ;;
(defn dorun
    ([s]
        (when-some [s (seq s)]
            (recur (next s))
        )
    )
    ([n s]
        (when (pos? n)
            (when-some [s (seq s)]
                (recur (dec n) (next s))
            )
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
(defn doall
    ([s] (dorun s) s)
    ([n s] (dorun n s) s)
)

;;;
 ; Returns the nth next of coll, (seq coll) when n is 0.
 ;;
(defn nthnext [s n]
    (loop-when-recur [n n s (seq s)] (and s (pos? n)) [(dec n) (next s)] => s)
)

;;;
 ; Returns a lazy sequence of lists of n items each, at offsets step apart.
 ; If step is not supplied, defaults to n, i.e. the partitions do not overlap.
 ; If a pad is supplied, use it as necessary to complete the last partition upto n items.
 ; In case there are not enough padding elements, return a partition with less than n items.
 ;;
(defn partition
    ([n s] (partition n n s))
    ([n step s]
        (lazy-seq
            (when-some [s (seq s)]
                (let-when [p (take n s)] (= (count p) n)
                    (cons p (partition n step (nthnext s step)))
                )
            )
        )
    )
    ([n step pad s]
        (lazy-seq
            (when-some [s (seq s)]
                (let-when [p (take n s)] (= (count p) n) => (list (take n (concat p pad)))
                    (cons p (partition n step pad (nthnext s step)))
                )
            )
        )
    )
)

;;;
 ; Returns a lazy sequence of lists like partition, but may include
 ; partitions with fewer than n items at the end. Returns a stateful
 ; transducer when no collection is provided.
 ;;
(defn partition-all
    ([n]
        (fn [g]
            (let [v' (volatile! [])]
                (fn
                    ([] (g))
                    ([x]
                        (let [x (when (seq @v') => x
                                    (let [v @v' _ (vswap! v' empty)]
                                        (unreduced (g x v))
                                    )
                                )]
                            (g x)
                        )
                    )
                    ([x y]
                        (vswap! v' conj y)
                        (when (= (count @v') n) => x
                            (let [v @v' _ (vswap! v' empty)]
                                (g x v)
                            )
                        )
                    )
                )
            )
        )
    )
    ([n s] (partition-all n n s))
    ([n step s]
        (lazy-seq
            (when-some [s (seq s)]
                (let [p (doall (take n s))]
                    (cons p (partition-all n step (nthnext s step)))
                )
            )
        )
    )
)

;;;
 ; Repeatedly executes body (presumably for side-effects) with bindings and filtering as provided by "for".
 ; Does not retain the head of the sequence. Returns nil.
 ;;
(§ defmacro doseq [seq-exprs & body]
    (assert-args
        (vector? seq-exprs) "a vector for its binding"
        (even? (count seq-exprs)) "an even number of forms in binding vector"
    )
    (letfn [(step- [recform exprs]
                (when exprs => [true `(do ~@body)]
                    (let [k (first exprs) v (second exprs)]
                        (if (keyword? k)
                            (let [[recur? subform] (step- recform (nnext exprs))]
                                (case k
                                    :let   [recur? `(let ~v ~subform)]
                                    :while [false `(when ~v ~subform ~@(when recur? [recform]))]
                                    :when  [false `(if ~v (do ~subform ~@(when recur? [recform])) ~recform)]
                                )
                            )
                            (let [s- (gensym "s_") i- (gensym "i_")
                                  recform `(recur (next ~s-) 0)
                                  [recur? subform] (step- recform (nnext exprs))]
                                [true
                                    `(loop [~s- (seq ~v) ~i- 0]
                                        (when-some [~s- (seq ~s-)]
                                            (let [~k (first ~s-)]
                                                ~subform
                                                ~@(when recur? [recform])
                                            )
                                        )
                                    )
                                ]
                            )
                        )
                    )
                )
            )]
        (nth (step- nil (seq seq-exprs)) 1)
    )
)

;;;
 ; bindings => name n
 ;
 ; Repeatedly executes body (presumably for side-effects) with name
 ; bound to integers from 0 through n-1.
 ;;
(§ defmacro dotimes [v & body]
    (assert-args
        (vector? v) "a vector for its binding"
        (= 2 (count v)) "exactly 2 forms in binding vector"
    )
    (let [i (v 0) n (v 1)]
        `(let [n# (long ~n)]
            (loop-when-recur [~i 0] (< ~i n#) [(inc ~i)]
                ~@body
            )
        )
    )
)

;;;
 ; Returns a new, transient version of the collection, in constant time.
 ;;
(defn transient [^cloiure.core.IEditableCollection coll] (IEditableCollection'''asTransient coll))

;;;
 ; Returns a new, persistent version of the transient collection, in
 ; constant time. The transient collection cannot be used after this
 ; call, any such use will throw an exception.
 ;;
(defn persistent! [^cloiure.core.ITransientCollection coll] (ITransientCollection'''persistent coll))

;;;
 ; Adds x to the transient collection, and return coll. The 'addition'
 ; may happen at different 'places' depending on the concrete type.
 ;;
(defn conj!
    ([] (transient []))
    ([coll] coll)
    ([^cloiure.core.ITransientCollection coll x] (ITransientCollection'''conj coll x))
)

;;;
 ; When applied to a transient map, adds mapping of key(s) to val(s).
 ; When applied to a transient vector, sets the val at index.
 ; Note - index must be <= (count vector). Returns coll.
 ;;
(defn assoc!
    ([^cloiure.core.ITransientAssociative a k v] (ITransientAssociative'''assoc a k v))
    ([a k v & kvs]
        (let [a (assoc! a k v)]
            (recur-if kvs [a (first kvs) (second kvs) (nnext kvs)] => a)
        )
    )
)

;;;
 ; Returns a transient map that doesn't contain a mapping for key(s).
 ;;
(defn dissoc!
    ([^cloiure.core.ITransientMap m k] (ITransientMap'''dissoc m k))
    ([m k & ks]
        (let [m (dissoc! m k)]
            (recur-if ks [m (first ks) (next ks)] => m)
        )
    )
)

;;;
 ; Removes the last item from a transient vector.
 ; If the collection is empty, throws an exception. Returns coll.
 ;;
(defn pop! [^cloiure.core.ITransientVector coll] (ITransientVector'''pop coll))

;;;
 ; disj[oin].
 ; Returns a transient set of the same (hashed/sorted) type, that does not contain key(s).
 ;;
(defn disj!
    ([s] s)
    ([^cloiure.core.ITransientSet s k] (ITransientSet'''disj s k))
    ([s k & ks]
        (let [s (disj! s k)]
            (recur-if ks [s (first ks) (next ks)] => s)
        )
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
            ~@(map #(list 'import* %)
                (reduce
                    (fn [v spec]
                        (if (symbol? spec)
                            (conj v (name spec))
                            (let [p (first spec) cs (next spec)] (into v (map #(str p "." %) cs)))
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
 ;;
(defn into-array
    ([s] (into-array nil s))
    ([type s] (RT'seqToTypedArray type (seq s)))
)

(defn array [& s] (into-array s))

;;;
 ; Returns the :type metadata of x, or its Class if none.
 ;;
(defn type [x] (or (:type (meta x)) (class x)))

;;;
 ; Returns true if n is a Ratio.
 ;;
(defn ratio? [n] (instance? Ratio n))

;;;
 ; Returns true if n is a rational number.
 ;;
(defn rational? [n] (or (integer? n) (ratio? n)))

;;;
 ; Coerce to BigInteger.
 ;;
(defn ^BigInteger biginteger [x]
    (cond
        (instance? BigInteger x) x
        (ratio? x)               (Ratio''bigIntegerValue ^Ratio x)
        (number? x)              (BigInteger/valueOf (long x))
        :else                    (BigInteger. x)
    )
)

(§ defmulti print-method (fn [x w] (let [t (get (meta x) :type)] (if (keyword? t) t (class x)))))

(§ defn- pr-on [x w]
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
        (if-some [nmore (next more)]
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
        (LispReader'read stream (boolean eof-error?) eof-value)
    )
)

;;;
 ; Reads one object from the string s.
 ;;
(defn read-string [s] (RT'readString s))

;;;
 ; Returns a persistent vector of the items in vector from start (inclusive) to end (exclusive).
 ; If end is not supplied, defaults to (count vector). This operation is O(1) and very fast, as
 ; the resulting vector shares structure with the original and no trimming is done.
 ;;
(defn subvec
    ([v start] (subvec v start (count v)))
    ([v start end] (RT'subvec v start end))
)

;;;
 ; bindings => [name init ...]
 ;
 ; Evaluates body in a try expression with names bound to the values of the inits,
 ; and a finally clause that calls (.close name) on each name in reverse order.
 ;;
(defmacro with-open [bindings & body]
    (assert-args
        (vector? bindings) "a vector for its binding"
        (even? (count bindings)) "an even number of forms in binding vector"
    )
    (cond
        (zero? (count bindings)) `(do ~@body)
        (symbol? (bindings 0))
            `(let ~(subvec bindings 0 2)
                (try
                    (with-open ~(subvec bindings 2) ~@body)
                    (finally
                        (.close ~(bindings 0))
                    )
                )
            )
        :else (throw! "with-open only allows Symbols in bindings")
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
 ; Evaluates expr and prints the time it took. Returns the value of expr.
 ;;
(defmacro time [expr]
    `(let [start# (System/nanoTime) ret# ~expr]
        (prn (str "Elapsed time: " (- (System/nanoTime) start#) " nsecs"))
        ret#
    )
)

;;;
 ; Returns the length of the Java array. Works on arrays of all types.
 ;;
(§ defn alength [a] (RT'alength a))

;;;
 ; Returns a clone of the Java array. Works on arrays of known types.
 ;;
(§ defn aclone [a] (RT'aclone a))

;;;
 ; Returns the value at the index/indices. Works on Java arrays of all types.
 ;;
(§ defn aget
    {:inline (fn [a i] `(RT'aget ~a (int ~i))) :inline-arities #{2}}
    ([a i]
        (Reflector'prepRet (.getComponentType (class a)) (Array/get a i))
    )
    ([a i & s]
        (apply aget (aget a i) s)
    )
)

;;;
 ; Sets the value at the index/indices.
 ; Works on Java arrays of reference types. Returns value.
 ;;
(§ defn aset
    {:inline (fn [a i v] `(RT'aset ~a (int ~i) ~v)) :inline-arities #{3}}
    ([a i v]
        (Array/set a i v)
        v
    )
    ([a i j & s]
        (apply aset (aget a i) j s)
    )
)

(defmacro- def-aset [name method coerce]
    `(defn ~name
        ([a# i# v#]
            (. Array (~method a# i# (~coerce v#)))
            v#
        )
        ([a# i# j# & s#]
            (apply ~name (aget a# i#) j# s#)
        )
    )
)

;;;
 ; Sets the value at the index/indices. Works on arrays of boolean/byte/char/int/long. Returns value.
 ;;
(def-aset aset-boolean setBoolean boolean)
(§ def-aset aset-byte    setByte    byte   )
(def-aset aset-char    setChar    char   )
(def-aset aset-int     setInt     int    )
(def-aset aset-long    setLong    long   )

;;;
 ; Creates an array of objects.
 ;;
(§ defn object-array ([size-or-seq] (RT'objectArray size-or-seq)))

;;;
 ; Creates and returns an array of instances of the specified class of the specified dimension(s).
 ; Note that a class object is required.
 ; Class objects can be obtained by using their imported or fully-qualified name.
 ; Class objects for the primitive types can be obtained using, e.g. Integer/TYPE.
 ;;
(§ defn make-array
    ([^Class type n] (Array/newInstance type (int n)))
    ([^Class type dim & s]
        (let [dims (cons dim s) ^"[I" a (make-array Integer/TYPE (count dims))]
            (dotimes [i (alength a)]
                (aset-int a i (nth dims i))
            )
            (Array/newInstance type a)
        )
    )
)

;;;
 ; If form represents a macro form, returns its expansion, else returns form.
 ;;
(defn macroexpand-1 [form] (Compiler'macroexpand1 form))

;;;
 ; Repeatedly calls macroexpand-1 on form until it no longer
 ; represents a macro form, then returns it. Note neither
 ; macroexpand-1 nor macroexpand expand macros in subforms.
 ;;
(defn macroexpand [form]
    (let-when [e (macroexpand-1 form)] (identical? e form) => (recur e)
        form
    )
)

;;;
 ; Sequentially read and evaluate the set of forms contained in the stream.
 ;;
(defn load-reader [r] (Compiler'load r))

;;;
 ; Sequentially read and evaluate the set of forms contained in the string.
 ;;
(defn load-string [s] (load-reader (-> s (java.io.StringReader.) (PushbackReader.))))

;;;
 ; Returns a set of the distinct elements of coll.
 ;;
(defn set [s] (if (set? s) (with-meta s nil) (into #{} s)))

(defn- filter-key [f f? m]
    (loop-when-recur [s (seq m) m (transient {})]
                     s
                     [(next s) (let [e (first s)] (if (f? (f e)) (assoc m (key e) (val e)) m))]
                  => (persistent! m)
    )
)

;;;
 ; Returns the namespace named by the symbol or nil if it doesn't exist.
 ;;
(defn find-ns [sym] (Namespace'find sym))

;;;
 ; Create a new namespace named by the symbol if one doesn't already exist,
 ; returns it or the already-existing namespace of the same name.
 ;;
(defn create-ns [sym] (Namespace'findOrCreate sym))

;;;
 ; Removes the namespace named by the symbol. Use with caution.
 ; Cannot be used to remove the cloiure namespace.
 ;;
(defn remove-ns [sym] (Namespace'remove sym))

;;;
 ; Returns a sequence of all namespaces.
 ;;
(defn all-ns [] (Namespace'all))

;;;
 ; If passed a namespace, returns it. Else, when passed a symbol,
 ; returns the namespace named by it, throwing an exception if not found.
 ;;
(§ defn ^Namespace the-ns [x]
    (if (instance? Namespace x)
        x
        (or (find-ns x) (throw! (str "no namespace: " x " found")))
    )
)

;;;
 ; Returns the name of the namespace, a symbol.
 ;;
(defn ns-name [ns] (:name (the-ns ns)))

;;;
 ; Returns a map of all the mappings for the namespace.
 ;;
(§ defn ns-map [ns] (Namespace''getMappings (the-ns ns)))

;;;
 ; Removes the mappings for the symbol from the namespace.
 ;;
(§ defn ns-unmap [ns sym] (Namespace''unmap (the-ns ns) sym))

;;;
 ; Returns a map of the public intern mappings for the namespace.
 ;;
(§ defn ns-publics [ns]
    (let [ns (the-ns ns)]
        (filter-key val
            (fn [^Var v]
                (and (var? v) (= ns (:ns v)) (Var''isPublic v))
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
            (fn [^Var v]
                (and (var? v) (= ns (:ns v)))
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
 ;;
(§ defn refer [ns-sym & filters]
    (let [ns (or (find-ns ns-sym) (throw! (str "no namespace: " ns-sym)))
          fs (apply hash-map filters)
          nspublics (ns-publics ns)
          rename (or (:rename fs) {})
          exclude (set (:exclude fs))
          to-do
            (if (= :all (:refer fs))
                (keys nspublics)
                (or (:refer fs) (:only fs) (keys nspublics))
            )]
        (when (and to-do (not (sequential? to-do)))
            (throw! "the value of :only/:refer must be a sequential collection of symbols")
        )
        (doseq [sym to-do]
            (when-not (exclude sym)
                (let [v (nspublics sym)]
                    (when v => (throw! (str sym (if (get (ns-interns ns) sym) " is not public" " does not exist")))
                        (Namespace''refer *ns* (or (rename sym) sym) v)
                    )
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
            (fn [^Var v]
                (and (var? v) (not= ns (:ns v)))
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
    (Namespace''addAlias *ns* alias (the-ns namespace-sym))
)

;;;
 ; Returns a map of the aliases for the namespace.
 ;;
(§ defn ns-aliases [ns]
    (Namespace''getAliases (the-ns ns))
)

;;;
 ; Removes the alias for the symbol from the namespace.
 ;;
(§ defn ns-unalias [ns sym]
    (Namespace''removeAlias (the-ns ns) sym)
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
            (when-some [s (seq coll)]
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
                    (cons (first s1) (cons (first s2) (interleave (next s1) (next s2))))
                )
            )
        )
    )
    ([c1 c2 & colls]
        (lazy-seq
            (let [ss (map seq (conj colls c2 c1))]
                (when (every? identity ss)
                    (concat (map first ss) (apply interleave (map next ss)))
                )
            )
        )
    )
)

;;;
 ; Gets the value in the var object.
 ;;
(defn var-get [^Var x] (Var''get x))

;;;
 ; Sets the value in the var object to val.
 ; The var must be thread-locally bound.
 ;;
(defn var-set [^Var x val] (Var''set x val))

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
    `(let [~@(interleave (take-nth 2 name-vals-vec) (repeat '(Var'new nil, nil)))]
        (push-thread-bindings (hash-map ~@name-vals-vec))
        (try
            ~@body
            (finally
                (pop-thread-bindings)
            )
        )
    )
)

;;;
 ; Returns the var or Class to which a symbol will be resolved in the namespace
 ; (unless found in the environment), else nil. Note that if the symbol is fully qualified,
 ; the var/Class to which it resolves need not be present in the namespace.
 ;;
(defn ns-resolve
    ([ns sym] (ns-resolve ns nil sym))
    ([ns env sym]
        (when-not (contains? env sym)
            (Compiler'maybeResolveIn (the-ns ns) sym)
        )
    )
)

(defn resolve
    ([    sym] (ns-resolve *ns*     sym))
    ([env sym] (ns-resolve *ns* env sym))
)

;;;
 ; Constructs an array-map.
 ; If any keys are equal, they are handled as if by repeated uses of assoc.
 ;;
(§ defn array-map
    ([] PersistentArrayMap/EMPTY)
    ([& keyvals] (PersistentArrayMap'createAsIfByAssoc (to-array keyvals)))
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
                                                                    (throw! "unsupported binding form, only :as can follow & parameter")
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
                            (let [gmap (gensym "map__") gmapseq (with-meta gmap {:tag 'cloiure.core.ISeq}) defaults (:or b)]
                                (loop [ret (-> (conj bvec gmap v gmap)
                                            (conj `(if (seq? ~gmap) (PersistentHashMap'create ~gmapseq) ~gmap))
                                            ((fn [ret] (if (:as b) (conj ret (:as b) gmap) ret)))
                                        )
                                       bes (let [trafos (reduce
                                                    (fn [trafos mk]
                                                        (if (keyword? mk)
                                                            (let [mkns (namespace mk) mkn (name mk)]
                                                                (case mkn
                                                                    "keys" (assoc trafos mk #(keyword (or mkns (namespace %)) (name %)))
                                                                    "syms" (assoc trafos mk #(list `quote (symbol (or mkns (namespace %)) (name %))))
                                                                    "strs" (assoc trafos mk str)
                                                                           trafos
                                                                )
                                                            )
                                                            trafos
                                                        )
                                                    )
                                                    {} (keys b)
                                                )]
                                            (reduce
                                                (fn [bes entry] (reduce #(assoc %1 %2 ((val entry) %2)) (dissoc bes (key entry)) ((key entry) bes)))
                                                (dissoc b :as :or) trafos
                                            )
                                        )]
                                    (if (seq bes)
                                        (let [bb (key (first bes)) bk (val (first bes))
                                              local (if (satisfies? INamed bb) (with-meta (symbol nil (name bb)) (meta bb)) bb)
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
                        :else (throw! (str "unsupported binding form: " b))
                    )
                )
            )
          process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
        (if (every? symbol? (map first bents))
            bindings
            (reduce process-entry [] bents)
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

(§ defn- maybe-destructured [params body]
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
                    (throw!
                        (if (seq sigs)
                            (str "parameter declaration " (first sigs) " should be a vector")
                            (str "parameter declaration missing")
                        )
                    )
                )
            )
          psig
            (fn* [sig]
                ;; ensure correct type before destructuring sig
                (when (not (seq? sig))
                    (throw! (str "invalid signature " sig " should be a list"))
                )
                (let [[params & body] sig
                      _ (when (not (vector? params))
                            (throw!
                                (if (seq? (first sigs))
                                    (str "parameter declaration " params " should be a vector")
                                    (str "invalid signature " sig " should be a list")
                                )
                            )
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
                  bfs (reduce
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
        `(when-some [xs# (seq ~xs)]
            (let [~x (first xs#)]
                ~@body
            )
        )
    )
)

;;;
 ; Expands to code which yields a lazy sequence of the concatenation of
 ; the supplied colls. Each coll expr is not evaluated until it is needed.
 ;
 ; (lazy-cat xs ys zs) === (concat (lazy-seq xs) (lazy-seq ys) (lazy-seq zs))
 ;;
(§ defmacro lazy-cat [& colls]
    `(concat ~@(map #(list `lazy-seq %) colls))
)

;;;
 ; List comprehension.
 ;
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
                (reduce
                    (fn [groups [k v]]
                        (if (keyword? k)
                            (conj (pop groups) (conj (peek groups) [k v]))
                            (conj groups [k v])
                        )
                    )
                    [] (partition 2 seq-exprs)
                )
            )
          emit-bind
            (fn emit-bind [[[bind expr & mod-pairs] & [[_ next-expr] :as next-groups]]]
                (let [i- (gensym "i__") s- (gensym "s__")
                      do-mod
                        (fn do-mod [[[k v :as pair] & etc]]
                            (cond
                                (= k :let) `(let ~v ~(do-mod etc))
                                (= k :while) `(when ~v ~(do-mod etc))
                                (= k :when) `(if ~v ~(do-mod etc) (recur (next ~s-)))
                                (keyword? k) (throw! (str "Invalid 'for' keyword " k))
                                next-groups
                                    `(let [iterys# ~(emit-bind next-groups) fs# (seq (iterys# ~next-expr))]
                                        (if fs#
                                            (concat fs# (~i- (next ~s-)))
                                            (recur (next ~s-))
                                        )
                                    )
                                :else `(cons ~body-expr (~i- (next ~s-)))
                            )
                        )]
                    (if next-groups
                        #_"not the inner-most loop"
                        `(fn ~i- [~s-]
                            (lazy-seq
                                (loop [~s- ~s-]
                                    (when-first [~bind ~s-]
                                        ~(do-mod mod-pairs)
                                    )
                                )
                            )
                        )
                        #_"inner-most loop"
                        `(fn ~i- [~s-]
                            (lazy-seq
                                (loop [~s- ~s-]
                                    (when-some [~s- (seq ~s-)]
                                        (let [~bind (first ~s-)]
                                            ~(do-mod mod-pairs)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )]
        `(~(emit-bind (to-groups seq-exprs)) ~(second seq-exprs))
    )
)

;;;
 ; Evaluates exprs in a context in which *out* is bound to a fresh StringWriter.
 ; Returns the string created by any nested printing calls.
 ;;
(§ defmacro with-out-str [& body]
    `(let [s# (StringWriter.)]
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
    `(with-open [s# (-> ~s (java.io.StringReader.) (PushbackReader.))]
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

;;;
 ; Returns an instance of java.util.regex.Pattern, for use, e.g. in re-matcher.
 ;;
(§ defn ^Pattern re-pattern [s]
    (if (instance? Pattern s)
        s
        (Pattern/compile s)
    )
)

;;;
 ; Returns an instance of java.util.regex.Matcher, for use, e.g. in re-find.
 ;;
(§ defn ^Matcher re-matcher [^Pattern re s]
    (.matcher re s)
)

;;;
 ; Returns the groups from the most recent match/find. If there are no
 ; nested groups, returns a string of the entire match. If there are
 ; nested groups, returns a vector of the groups, the first element
 ; being the entire match.
 ;;
(§ defn re-groups [^Matcher m]
    (let-when [n (.groupCount m)] (pos? n) => (.group m)
        (into [] (for [i (range (inc n))] (.group m i)))
    )
)

;;;
 ; Returns a lazy sequence of successive matches of pattern in string,
 ; using java.util.regex.Matcher.find(), each such match processed with
 ; re-groups.
 ;;
(§ defn re-seq [^Pattern re s]
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
(§ defn re-matches [^Pattern re s]
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
    ([^Matcher m]
        (when (.find m)
            (re-groups m)
        )
    )
    ([^Pattern re s]
        (let [m (re-matcher re s)]
            (re-find m)
        )
    )
)

;;;
 ; Returns a lazy sequence of the nodes in a tree, via a depth-first walk.
 ; branch? must be a fn of one arg that returns true if passed a node
 ; that can have children (but may not). children must be a fn of one
 ; arg that returns a sequence of the children. Will only be called on
 ; nodes for which branch? returns true. Root is the root node of the
 ; tree.
 ;;
(defn tree-seq [branch? children root]
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
(§ defn special-symbol? [s] (contains? Compiler/specials s))

;;;
 ; Returns true if v is of type Var.
 ;;
(defn var? [v] (instance? Var v))

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
                            (when-some [s (seq xs)]
                                (if (contains? seen f)
                                    (recur (next s) seen)
                                    (cons f (step (next s) (conj seen f)))
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
        (map #(if-some [e (find smap %)] (val e) %))
    )
    ([smap coll]
        (if (vector? coll)
            (reduce
                (fn [v i]
                    (if-some [e (find smap (nth v i))]
                        (assoc v i (val e))
                        v
                    )
                )
                coll (range (count coll))
            )
            (map #(if-some [e (find smap %)] (val e) %) coll)
        )
    )
)

(§ defn- mk-bound-fn [^cloiure.core.Sorted sc test key]
    (fn [e] (test (.compare (Sorted'''comparator sc) (Sorted'''entryKey sc e) key) 0))
)

;;;
 ; sc must be a sorted collection, test(s) one of <, <=, > or >=.
 ; Returns a seq of those entries with keys ek for which
 ; (test (.. sc comparator (compare ek key)) 0) is true.
 ;;
(§ defn subseq
    ([^cloiure.core.Sorted sc test key]
        (let [include (mk-bound-fn sc test key)]
            (if (#{> >=} test)
                (when-some [[e :as s] (Sorted'''seqFrom sc key true)]
                    (if (include e) s (next s))
                )
                (take-while include (Sorted'''seq sc true))
            )
        )
    )
    ([^cloiure.core.Sorted sc start-test start-key end-test end-key]
        (when-some [[e :as s] (Sorted'''seqFrom sc start-key true)]
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
    ([^cloiure.core.Sorted sc test key]
        (let [include (mk-bound-fn sc test key)]
            (if (#{< <=} test)
                (when-some [[e :as s] (Sorted'''seqFrom sc key false)]
                    (if (include e) s (next s))
                )
                (take-while include (Sorted'''seq sc false))
            )
        )
    )
    ([^cloiure.core.Sorted sc start-test start-key end-test end-key]
        (when-some [[e :as s] (Sorted'''seqFrom sc end-key false)]
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
 ; consistent with =, and thus is different from .hashCode for Integer,
 ; Byte and Cloiure collections.
 ;;
(§ defn hash [x] (Util'hasheq x))

;;;
 ; Mix final collection hash for ordered or unordered collections.
 ; hash-basis is the combined collection hash, count is the number
 ; of elements included in the basis. Note this is the hash code
 ; consistent with =, different from .hashCode.
 ; See http://clojure.org/data_structures#hash for full algorithms.
 ;;
(§ defn ^long mix-collection-hash [^long hash-basis ^long n] (Murmur3'mixCollHash hash-basis n))

;;;
 ; Returns the hash code, consistent with =, for an external ordered
 ; collection implementing Seqable.
 ; See http://clojure.org/data_structures#hash for full algorithms.
 ;;
(§ defn ^long hash-ordered-coll [coll] (Murmur3'hashOrdered coll))

;;;
 ; Returns the hash code, consistent with =, for an external, unordered
 ; collection implementing Seqable. For maps, it should return
 ; map entries, whose hash is computed as (hash-ordered-coll [k v]).
 ; See http://clojure.org/data_structures#hash for full algorithms.
 ;;
(§ defn ^long hash-unordered-coll [coll] (Murmur3'hashUnordered coll))

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
(defn empty [coll]
    (when (coll? coll)
        (IPersistentCollection'''empty ^cloiure.core.IPersistentCollection coll)
    )
)

;;;
 ; If coll is empty, returns nil, else coll.
 ;;
(defn not-empty [coll] (when (seq coll) coll))

;;;
 ; Atomically alters the root binding of var v by applying f to its current value plus any args.
 ;;
(§ defn alter-var-root [^Var v f & args] (Var''alterRoot v f args))

;;;
 ; Returns true if all of the vars provided as arguments have any bound value, root or thread-local.
 ; Implies that deref'ing the provided vars will succeed. Returns true if no vars are provided.
 ;;
(defn bound? [& vars] (every? #(Var''isBound ^Var %) vars))

;;;
 ; Returns true if all of the vars provided as arguments have thread-local bindings.
 ; Implies that set!'ing the provided vars will succeed. Returns true if no vars are provided.
 ;;
(defn thread-bound? [& vars] (every? #(Var''getThreadBinding ^Var %) vars))

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
(§ defn supers [^Class c]
    (loop-when [s (set (bases c)) cs s] (seq cs) => (not-empty s)
        (let [c (first cs) bs (bases c)]
            (recur (into s bs) (into (disj cs c) bs))
        )
    )
)

;;;
 ; Returns true if (= child parent), or child is directly or indirectly derived
 ; from parent via Java type inheritance relationship.
 ;;
(defn isa? [child parent]
    (or (= child parent)
        (and (class? parent) (class? child) (.isAssignableFrom ^Class parent child))
        (and (vector? parent) (vector? child) (= (count parent) (count child))
            (loop-when-recur [? true i 0] (and ? (< i (count parent))) [(isa? (child i) (parent i)) (inc i)] => ?)
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
 ; Formats a string using String/format.
 ; See java.util.Formatter for format string syntax.
 ;;
(§ defn ^String format [fmt & args] (String/format fmt (to-array args)))

;;;
 ; Prints formatted output, as per format.
 ;;
(§ defn printf [fmt & args] (print (apply format fmt args)))

(defmacro- with-loading-context [& body]
    `((fn loading# []
        (binding [*class-loader* (.getClassLoader (.getClass ^Object loading#))]
            ~@body
        )
    ))
)

;;;
 ; Sets *ns* to the namespace named by name (unevaluated), creating it if needed.
 ;
 ; references can be zero or more of:
 ; (:refer-cloiure ...) (:import ...)
 ; with the syntax of refer-cloiure/import respectively,
 ; except the arguments are unevaluated and need not be quoted.
 ;
 ; If :refer-cloiure is not used, a default (refer 'cloiure.core) is used.
 ; Use of ns is preferred to individual calls to in-ns/import:
 ;
 ; (ns foo.bar
 ;   (:refer-cloiure :exclude [format printf])
 ;   (:import (java.util Date Timer Random)
 ;            (java.sql Connection Statement)))
 ;;
(§ defmacro ns [name & references]
    (let [process-reference (fn [[kname & args]] `(~(symbol "cloiure.core" (cloiure.core/name kname)) ~@(map #(list 'quote %) args)))
          metadata          (when (map? (first references)) (first references))
          references        (if metadata (next references) references)
          name              (if metadata (vary-meta name merge metadata) name)
          ;; ns-effect (cloiure.core/in-ns name)
          name-metadata (meta name)]
        `(do
            (cloiure.core/in-ns '~name)
            ~@(when name-metadata
                `((reset-meta! (Namespace'find '~name) ~name-metadata))
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
(defmacro defonce [name expr]
    `(let-when-not [v# (def ~name)] (Var''hasRoot v#)
        (def ~name ~expr)
    )
)

;;;
 ; Returns the value in a nested associative structure,
 ; where ks is a sequence of keys. Returns nil if the key
 ; is not present, or the not-found value if supplied.
 ;;
(defn get-in
    ([m ks] (reduce get m ks))
    ([m ks not-found]
        (loop-when [o (Object.) m m ks (seq ks)] ks => m
            (let-when [m (get m (first ks) o)] (identical? o m) => (recur o m (next ks))
                not-found
            )
        )
    )
)

;;;
 ; Associates a value in a nested associative structure, where ks is
 ; a sequence of keys and v is the new value and returns a new nested
 ; structure. If any levels do not exist, hash-maps will be created.
 ;;
(defn assoc-in [m [k & ks] v]
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
(defn update-in [m ks f & args]
    (let [[k & ks] ks]
        (if ks
            (assoc m k (apply update-in (get m k) ks f args))
            (assoc m k (apply f (get m k) args))
        )
    )
)

;;;
 ; 'Updates' a value in an associative structure, where k is a key and f is a function
 ; that will take the old value and any supplied args and return the new value, and
 ; returns a new structure. If the key does not exist, nil is passed as the old value.
 ;;
(defn update
    ([m k f] (assoc m k (f (get m k))))
    ([m k f x] (assoc m k (f (get m k) x)))
    ([m k f x y] (assoc m k (f (get m k) x y)))
    ([m k f x y & z] (assoc m k (apply f (get m k) x y z)))
)

;;;
 ; Returns true if coll has no items - same as (not (seq coll)).
 ; Please use the idiom (seq x) rather than (not (empty? x)).
 ;;
(defn empty? [coll] (not (seq coll)))

;;;
 ; trampoline can be used to convert algorithms requiring mutual recursion without
 ; stack consumption. Calls f with supplied args, if any. If f returns a fn, calls
 ; that fn with no arguments, and continues to repeat, until the return value is
 ; not a fn, then returns that non-fn value. Note that if you want to return a fn
 ; as a final value, you must wrap it in some data structure and unpack it after
 ; trampoline returns.
 ;;
(defn trampoline
    ([f]
        (let-when [r (f)] (fn? r) => r
            (recur r)
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
    ([ns ^Symbol name]
        (let [v (Var'intern (the-ns ns) name)]
            (when-some [m (meta name)]
                (reset-meta! v m)
            )
            v
        )
    )
    ([ns name o]
        (let [v (Var'intern (the-ns ns) name o)]
            (when-some [m (meta name)]
                (reset-meta! v m)
            )
            v
        )
    )
)

;;;
 ; Repeatedly executes body while test expression is true. Presumes
 ; some side-effect will cause test to become false/nil. Returns nil.
 ;;
(§ defmacro while [? & s]
    `(loop [] (when ~? ~@s (recur)))
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
            (if-some [e (find @mem args)]
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
 ; For each clause, (f? test-expr expr) is evaluated. If it returns logical true,
 ; the clause is a match. If a binary clause matches, the result-expr is returned,
 ; if a ternary clause matches, its result-fn, which must be a unary function, is
 ; called with the result of the predicate as its argument, the result of that call
 ; being the return value of condp. A single default expression can follow the clauses,
 ; and its value will be returned if no clause matches. If no default expression
 ; is provided and no clause matches, an IllegalArgumentException is thrown.
 ;;
(§ defmacro condp [f? expr & clauses]
    (let [gpred (gensym "pred__") gexpr (gensym "expr__")
          emit
            (fn emit [f? expr args]
                (let [[[a b c :as clause] more] (split-at (if (= :>> (second args)) 3 2) args) n (count clause)]
                    (cond
                        (= 0 n) `(throw! (str "no matching clause: " ~expr))
                        (= 1 n) a
                        (= 2 n) `(if (~f? ~a ~expr)
                                    ~b
                                    ~(emit f? expr more)
                                )
                        :else   `(if-let [p# (~f? ~a ~expr)]
                                    (~c p#)
                                    ~(emit f? expr more)
                                )
                    )
                )
            )]
        `(let [~gpred ~f? ~gexpr ~expr]
            ~(emit gpred gexpr clauses)
        )
    )
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

(def- max-mask-bits 13)
(def- max-switch-table-size (bit-shift-left 1 max-mask-bits))

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
    (into (sorted-map)
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
                    (recur (update m (IObject'''hashCode (first ks)) (fnil conj []) [(first ks) (first vs)]) (next ks) (next vs))
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
            (reduce
                (fn [m [h bucket]]
                    (if (= (count bucket) 1)
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
                (into #{})
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
    (let [hashcode #(IObject'''hashCode %) hashes (into #{} (map hashcode tests))]
        (if (= (count tests) (count hashes))
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
                        (into #{} (map #(shift-mask shift mask %) skip-check))
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
            (when (odd? (count clauses)) => `(throw! (str "no matching clause: " ~ge))
                (last clauses)
            )]
        (when (<= 2 (count clauses)) => `(let [~ge ~e] ~default)
            (let [pairs (partition 2 clauses)
                  assoc-test
                    (fn [m test expr]
                        (when-not (contains? m test) => (throw! (str "duplicate case test constant: " test))
                            (assoc m test expr)
                        )
                    )
                  pairs
                    (reduce
                        (fn [m [test expr]]
                            (if (seq? test)
                                (reduce #(assoc-test %1 %2 expr) m test)
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

;;;
 ; *print-length* controls how many items of each collection the printer will print.
 ; If it is bound to logical false, there is no limit. Otherwise, it must be bound
 ; to an integer indicating the maximum number of items of each collection to print.
 ; If a collection contains more items, the printer will print items up to the limit
 ; followed by '...' to represent the remaining items. The root binding is nil
 ; indicating no limit.
 ;;
(§ soon def ^:dynamic *print-length* nil)

;;;
 ; *print-level* controls how many levels deep the printer will print nested objects.
 ; If it is bound to logical false, there is no limit. Otherwise, it must be bound
 ; to an integer indicating the maximum level to print. Each argument to print is at
 ; level 0; if an argument is a collection, its items are at level 1; and so on.
 ; If an object is a collection and is at a level greater than or equal to the value
 ; bound to *print-level*, the printer prints '#' to represent it. The root binding
 ; is nil indicating no limit.
 ;;
(def ^:dynamic *print-level* nil)

;;;
 ; *print-namespace-maps* controls whether the printer will print namespace map literal
 ; syntax. It defaults to false, but the REPL binds to true.
 ;;
(def ^:dynamic *print-namespace-maps* false)

(§ defn- print-sequential [^String begin, print-one, ^String sep, ^String end, sequence, ^Writer w]
    (binding [*print-level* (and *print-level* (dec *print-level*))]
        (if (and *print-level* (neg? *print-level*))
            (.write w "#")
            (do
                (.write w begin)
                (when-some [xs (seq sequence)]
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
    (if (satisfies? IObj o)
        (print-method (vary-meta o #(dissoc % :type)) w)
        (print-simple o w)
    )
)

(§ defmethod print-method nil [o, ^Writer w]
    (.write w "nil")
)

(§ defn print-ctor [o print-args ^Writer w]
    (.write w "#=(")
    (.write w (.getName (class o)))
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

(§ defmethod print-method Keyword [o, ^Writer w]
    (.write w (str o))
)

(§ defmethod print-method Number [o, ^Writer w]
    (.write w (str o))
)

(§ defmethod print-method Boolean [o, ^Writer w]
    (.write w (str o))
)

(§ defmethod print-method Symbol [o, ^Writer w]
    (print-simple o w)
)

(§ defmethod print-method Var [o, ^Writer w]
    (print-simple o w)
)

(§ defmethod print-method cloiure.core.ISeq [o, ^Writer w]
    (print-sequential "(" pr-on " " ")" o w)
)

(§ prefer-method print-method cloiure.core.ISeq cloiure.core.IPersistentCollection)

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
                (let [c (nth s n) e (char-escape-string c)]
                    (if e (.write w e) (.append w c))
                )
            )
            (.append w \") ;; oops! "
        )
        (.write w s)
    )
    nil
)

(§ defmethod print-method cloiure.core.IPersistentVector [v, ^Writer w]
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
                        (when-some [new-ns (namespace k)]
                            (recur new-ns entries (assoc lm (strip-ns k) v))
                        )
                    )
                )
                [ns (apply conj (empty m) lm)]
            )
        )
    )
)

(§ defmethod print-method cloiure.core.IPersistentMap [m, ^Writer w]
    (let [[ns lift-map] (lift-ns m)]
        (if ns
            (print-prefix-map (str "#:" ns) lift-map pr-on w)
            (print-map m pr-on w)
        )
    )
)

(§ defmethod print-method cloiure.core.IPersistentSet [s, ^Writer w]
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

(§ defmethod print-method Character [^Character c, ^Writer w]
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

(§ defmethod print-method Class [^Class c, ^Writer w]
    (.write w (.getName c))
)

(§ defmethod print-method Pattern [p ^Writer w]
    (.write w "#\"")
    (loop [[^Character c & r :as s] (seq (.pattern ^Pattern p)) qmode false]
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

(defn- deref-as-map [^cloiure.core.IDeref r]
    (let [pending? (and (satisfies? IPending r) (not (realized? r)))
          [failed? val]
            (when-not pending?
                (try
                    [false (deref r)]
                    (catch Throwable e
                        [true e]
                    )
                )
            )]
        (hash-map
            :status (cond failed? :failed pending? :pending :else :ready)
            :val val
        )
    )
)

(§ defmethod print-method cloiure.core.IDeref [o ^Writer w]
    (print-tagged-object o (deref-as-map o) w)
)

(def- prim->class
     (hash-map
        'boolean  Boolean/TYPE   'booleans (Class/forName "[Z")
        'byte     Byte/TYPE      'bytes    (Class/forName "[B")
        'char     Character/TYPE 'chars    (Class/forName "[C")
        'int      Integer/TYPE   'ints     (Class/forName "[I")
        'long     Long/TYPE      'longs    (Class/forName "[J")
        'void     Void/TYPE
    )
)

(§ defn- ^Class the-class [x]
    (cond
        (class? x) x
        (contains? prim->class x) (prim->class x)
        :else (let [s (str x)] (RT'classForName (if (some #{\. \[} s) s (str "java.lang." s))))
    )
)

;;;
 ; Returns an asm Type object for c, which may be a primitive class (such as Integer/TYPE),
 ; any other class (such as Long), or a fully-qualified class name given as a string or symbol
 ; (such as 'java.lang.String).
 ;;
(§ defn- ^Type asm-type [c]
    (if (or (class? c) (prim->class c))
        (Type/getType (the-class c))
        (let [s (str c)]
            (Type/getObjectType (.replace (if (some #{\. \[} s) s (str "java.lang." s)) "." "/"))
        )
    )
)

(§ defn- generate-interface [{:keys [name extends methods]}]
    (when (some #(-> % first cloiure.core/name (.contains "-")) methods)
        (throw! "interface methods must not contain '-'")
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
 ; referred to by their Java names (int, long, etc.), and classes in the
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
        (.defineClass ^DynamicClassLoader *class-loader* (str (:name options-map)) bytecode)
    )
)

;;;
 ; Convert a Cloiure namespace name to a legal Java package name.
 ;;
(defn- namespace-munge [ns] (.replace (str ns) \- \_))

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

(declare resolve)

(§ defn- parse-opts+specs [opts+specs]
    (let [[opts specs] (parse-opts opts+specs)
          impls (parse-impls specs)
          interfaces
            (-> (map #(if (var? (resolve %)) (:on (deref (resolve %))) %) (keys impls))
                set (disj 'Object 'java.lang.Object) vec
            )
          methods
            (map (fn [[name params & body]] (cons name (maybe-destructured params body))) (apply concat (vals impls)))]
        (when-some [bad-opts (seq (keys opts))]
            (throw! (apply print-str "unsupported option(s) -" bad-opts))
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
 ; => "foo"
 ;
 ; (seq (let [f "foo"]
 ;  (reify Seqable
 ;   (seq [this] (seq f)))))
 ; => (\f \o \o)
 ;
 ; reify always implements IObj and transfers meta data of the form
 ; to the created object.
 ;
 ; (meta ^{:k :v} (reify Object (toString [this] "foo")))
 ; => {:k :v}
 ;;
(§ defmacro reify [& opts+specs]
    (let [[interfaces methods] (parse-opts+specs opts+specs)]
        (with-meta `(reify* ~interfaces ~@methods) (meta &form))
    )
)

(§ defn hash-combine [x y]
    (Util'hashCombine x (IObject'''hashCode y))
)

(§ defn munge [s]
    ((if (symbol? s) symbol str) (Compiler'munge (str s)))
)

(§ defn- validate-fields [fields name]
    (when-not (vector? fields)
        (throw! "No fields vector given.")
    )
    (let-when [specials '#{__meta __extmap __hash __hasheq}] (some specials fields)
        (throw! (str "The names in " specials " cannot be used as field names for types."))
    )
    (let-when [non-syms (remove symbol? fields)] (seq non-syms)
        (throw! (apply str "deftype fields must be symbols, " *ns* "." name " had: " (interpose ", " non-syms)))
    )
)

;;;
 ; Do not use this directly - use deftype.
 ;;
(§ defn- emit-deftype* [tagname cname fields interfaces methods opts]
    (let [classname (with-meta (symbol (str (namespace-munge *ns*) "." cname)) (meta cname)) interfaces (conj interfaces 'cloiure.core.IType)]
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
 ;;
(§ defmacro deftype [name fields & opts+specs]
    (validate-fields fields name)
    (let [gname                     name
          [interfaces methods opts] (parse-opts+specs opts+specs)
          ns-part                   (namespace-munge *ns*)
          classname                 (symbol (str ns-part "." gname))]
        `(let []
            ~(emit-deftype* name gname (vec fields) (vec interfaces) methods opts)
            (import ~classname)
        )
    )
)

(§ defn- expand-method-impl-cache [^MethodImplCache cache c f]
    (if (:map cache)
        (let [cs (assoc (:map cache) c (Entry'new c f))]
            (MethodImplCache'new (:protocol cache) (:methodk cache) cs)
        )
        (let [cs (into {} (remove (fn [[c e]] (nil? e)) (map vec (partition 2 (:table cache)))))
              cs (assoc cs c (Entry'new c f))]
            (if-some [[shift mask] (maybe-min-hash (map hash (keys cs)))]
                (let [table (make-array Object (* 2 (inc mask)))
                      table
                        (reduce
                            (fn [^objects t [c e]]
                                (let [i (* 2 (int (shift-mask shift mask (hash c))))]
                                    (aset t i c)
                                    (aset t (inc i) e)
                                    t
                                )
                            )
                            table cs
                        )]
                    (MethodImplCache'new (:protocol cache) (:methodk cache) shift mask table)
                )
                (MethodImplCache'new (:protocol cache) (:methodk cache) cs)
            )
        )
    )
)

(defn- super-chain [^Class c]
    (when c
        (cons c (super-chain (.getSuperclass c)))
    )
)

(defn- pref
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
                        (when-some [t (reduce pref (filter impl (disj (supers c) Object)))]
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

(§ defn -cache-protocol-fn [^AFunction pf x ^Class c ^cloiure.core.IFn interf]
    (let [cache @(:__methodImplCache pf)
          f (if (instance? c x) interf (find-protocol-method (:protocol cache) (:methodk cache) x))]
        (when-not f
            (throw!
                (str "no implementation of method: " (:methodk cache)
                     " of protocol: " (:var (:protocol cache))
                     " found for class: " (if (some? x) (.getName (class x)) "nil"))
            )
        )
        (vreset! (:__methodImplCache pf) (expand-method-impl-cache cache (class x) f))
        f
    )
)

(§ defn- emit-method-builder [on-interface method on-method arglists]
    (let [methodk (keyword method) gthis (with-meta (gensym) {:tag 'cloiure.core.AFunction}) ginterf (gensym)]
        `(fn [cache#]
            (let [~ginterf
                    (fn ~@(map
                        (fn [args]
                            (let [gargs (map #(gensym (str "gf__" % "__")) args) target (first gargs)]
                                `([~@gargs] (. ~(with-meta target {:tag on-interface}) (~(or on-method method) ~@(next gargs))))
                            )
                        )
                        arglists
                    ))
                  ^AFunction f#
                    (fn ~gthis ~@(map
                        (fn [args]
                            (let [gargs (map #(gensym (str "gf__" % "__")) args) target (first gargs)]
                                `([~@gargs]
                                    (let [cache# @(:__methodImplCache ~gthis)
                                          f# (MethodImplCache''fnFor cache# (Reflector'classOf ~target))]
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
                (vreset! (:__methodImplCache f#) cache#)
                f#
            )
        )
    )
)

(§ defn -reset-methods [protocol]
    (doseq [[^Var v build] (:method-builders protocol)]
        (let [cache (MethodImplCache'new protocol (keyword (:sym v)))]
            (Var''bindRoot v (build cache))
        )
    )
)

(§ defn- assert-same-protocol [protocol-var method-syms]
    (doseq [m method-syms]
        (let [v (resolve m) pv (:protocol (meta v))]
            (when (and v (bound? v) (not= protocol-var pv))
                (binding [*out* *err*]
                    (println "Warning: protocol" protocol-var "is overwriting"
                        (if pv
                            (str "method " (:sym v) " of protocol " (:sym pv))
                            (str "function " (:sym v))
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
                    keyword? (recur (assoc opts (first sigs) (second sigs)) (nnext sigs))
                    [opts sigs]
                )
            )
          sigs
            (when sigs
                (reduce
                    (fn [m s]
                        (let [name-meta (meta (first s))
                              mname (with-meta (first s) nil)
                              arglists
                                (loop [as [] rs (next s)]
                                    (if (vector? (first rs))
                                        (recur (conj as (first rs)) (next rs))
                                        (seq as)
                                    )
                                )]
                            (when (some #{0} (map count arglists))
                                (throw! (str "definition of function " mname " in protocol " name " must take at least one arg."))
                            )
                            (when (m (keyword mname))
                                (throw! (str "function " mname " in protocol " name " was redefined: specify all arities in single definition"))
                            )
                            (assoc m (keyword mname)
                                (merge name-meta {:name (vary-meta mname assoc :arglists arglists) :arglists arglists})
                            )
                        )
                    )
                    {} sigs
                )
            )
          meths
            (mapcat
                (fn [sig]
                    (let [m (munge (:name sig))]
                        (map #(vector m (vec (repeat (dec (count %))'Object)) 'Object) (:arglists sig))
                    )
                )
                (vals sigs)
            )]
        `(do
            (defonce ~name {})
            (gen-interface :name ~iname :methods ~meths)
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
                                    (fn [sig] [(keyword (:name sig)) (keyword (or (:on sig) (:name sig)))])
                                    (vals sigs)
                                )
                            )
                        )
                    :method-builders
                        ~(apply hash-map
                            (mapcat
                                (fn [sig] [
                                    `(intern *ns* (with-meta '~(:name sig) (merge '~sig {:protocol (var ~name)})))
                                    (emit-method-builder (:on-interface opts) (:name sig) (:on sig) (:arglists sig))
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
 ;  ;; method signatures
 ;  (bar [this a b])
 ;  (baz [this a] [this a b] [this a b c]))
 ;
 ; No implementations are provided. The above yields a set of polymorphic
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
            (throw! (str proto " is not a protocol"))
        )
        (when (implements? proto atype)
            (throw! (str atype " already directly implements " (:on-interface proto) " for protocol:" (:var proto)))
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
)

(clojure-ns cloiure.core.protocols

(defn- seq-reduce
    ([s f] (if-some [s (seq s)] (seq-reduce (next s) f (first s)) (f)))
    ([s f r]
        (loop-when [r r s (seq s)] s => r
            (let [r (f r (first s))]
                (if (reduced? r) @r (recur r (next s)))
            )
        )
    )
)

;; redefine reduce with IReduce

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
    ([f s]
        (if (satisfies? IReduce s)
            (IReduce'''reduce ^cloiure.core.IReduce s f)
            (seq-reduce s f)
        )
    )
    ([f r s]
        (if (satisfies? IReduce s)
            (IReduce'''reduce ^cloiure.core.IReduce s f r)
            (seq-reduce s f r)
        )
    )
)

;;;
 ; Protocol for concrete associative types that can reduce themselves
 ; via a function of key and val faster than first/next recursion over
 ; map entries. Called by cloiure.core/reduce-kv, and has same
 ; semantics (just different arg order).
 ;;
(§ defprotocol KVReduce
    (kv-reduce [m f r])
)

(§ extend-protocol KVReduce
    nil
    (kv-reduce [_ _ r] r)

    ;; slow path default
    IPersistentMap
    (kv-reduce [m f r] (reduce (fn [r [k v]] (f r k v)) r m))

    IKVReduce
    (kv-reduce [m f r] (IKVReduce'''kvreduce m f r))
)

;;;
 ; Reduces an associative collection. f should be a function of 3 arguments.
 ; Returns the result of applying f to init, the first key and the first value
 ; in coll, then applying f to that result and the 2nd key and value, etc.
 ; If coll contains no entries, returns init and f is not called. Note that
 ; reduce-kv is supported on vectors, where the keys will be the ordinals.
 ;;
(§ defn reduce-kv [f r m] (kv-reduce m f r))

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
    ([xform f s] (transduce xform f (f) s))
    ([xform f r s] (let [f (xform f)] (f (reduce f r s))))
)

;;;
 ; Returns a new coll consisting of to-coll with all of the items of from-coll
 ; conjoined. A transducer may be supplied.
 ;;
(§ defn into
    ([] [])
    ([to] to)
    ([to from]
        (if (editable? to)
            (with-meta (reduce! conj! to from) (meta to))
            (reduce conj to from)
        )
    )
    ([to xform from]
        (if (editable? to)
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
(defn mapv
    ([f coll] (reduce! #(conj! %1 (f %2)) [] coll))
    ([f c1 c2] (into [] (map f c1 c2)))
    ([f c1 c2 c3] (into [] (map f c1 c2 c3)))
    ([f c1 c2 c3 & colls] (into [] (apply map f c1 c2 c3 colls)))
)

;;;
 ; Returns a vector of the items in coll for which (f? item)
 ; returns logical true. f? must be free of side-effects.
 ;;
(defn filterv [f? coll] (reduce! #(if (f? %2) (conj! %1 %2) %1) [] coll))

;;;
 ; Takes any nested combination of sequential things (lists, vectors, etc.)
 ; and returns their contents as a single, flat sequence.
 ; (flatten nil) returns an empty sequence.
 ;;
(defn flatten [s] (remove sequential? (next (tree-seq sequential? seq s))))

;;;
 ; Returns a map of the elements of coll keyed by the result of
 ; f on each element. The value at each key will be a vector of the
 ; corresponding elements, in the order they appeared in coll.
 ;;
(defn group-by [f coll] (reduce! #(let [k (f %2)] (assoc! %1 k (conj (get %1 k []) %2))) {} coll))

;;;
 ; Applies f to each value in coll, splitting it each time f returns
 ; a new value. Returns a lazy seq of partitions. Returns a stateful
 ; transducer when no collection is provided.
 ;;
(§ defn partition-by
    ([f]
        (fn [rf]
            (let [lv (volatile! []) pv (volatile! ::none)]
                (fn
                    ([] (rf))
                    ([result]
                        (let [result
                                (if (empty? @lv)
                                    result
                                    (let [v @lv]
                                        (vswap! lv empty)
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
                                    (vswap! lv conj input)
                                    result
                                )
                                (let [v @lv]
                                    (vswap! lv empty)
                                    (let [ret (rf result v)]
                                        (when-not (reduced? ret)
                                            (vswap! lv conj input)
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
            (when-some [s (seq coll)]
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
(defn frequencies [s] (reduce! #(assoc! %1 %2 (inc (get %1 %2 0))) {} s))

;;;
 ; Returns a lazy seq of the intermediate values of the reduction (as per reduce)
 ; of coll by f, starting with init.
 ;;
(§ defn reductions
    ([f coll]
        (lazy-seq
            (if-some [s (seq coll)]
                (reductions f (first s) (next s))
                (list (f))
            )
        )
    )
    ([f init coll]
        (if (reduced? init)
            (list @init)
            (cons init
                (lazy-seq
                    (when-some [s (seq coll)]
                        (reductions f (f init (first s)) (next s))
                    )
                )
            )
        )
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
                        (when-some [s (seq coll)]
                            (cons (f idx (first s)) (mapi (inc idx) (next s)))
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
            (when-some [s (seq coll)]
                (let [x (f (first s))]
                    (if (nil? x)
                        (keep f (next s))
                        (cons x (keep f (next s)))
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
                        (when-some [s (seq coll)]
                            (let [x (f idx (first s))]
                                (if (nil? x)
                                    (keepi (inc idx) (next s))
                                    (cons x (keepi (inc idx) (next s)))
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
 ; If coll is counted? returns its count, else will count at most the first m
 ; elements of coll using its seq.
 ;;
(defn bounded-count [m coll]
    (when (counted? coll) => (loop-when-recur [n 0 s (seq coll)] (and s (< n m)) [(inc n) (next s)] => n)
        (count coll)
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
        (throw! "parameter declaration missing")
    )
    (let [argdecls
            (map
                #(if (seq? %)
                    (first %)
                    (throw!
                        (if (seq? (first fdecl))
                            (str "invalid signature \"" % "\" should be a list")
                            (str "parameter declaration \"" % "\" should be a vector")
                        )
                    )
                )
                fdecl
            )
          bad-args (seq (remove #(vector? %) argdecls))]
        (when bad-args
            (throw! (str "parameter declaration \"" (first bad-args) "\" should be a vector"))
        )
    )
)

;;;
 ; Takes an expression and a set of test/form pairs. Threads expr (via ->)
 ; through each form for which the corresponding test expression is true.
 ; Note that, unlike cond branching, cond-> threading does not short circuit
 ; after the first true test expression.
 ;;
(§ defmacro cond-> [expr & clauses]
    (assert-args
        (even? (count clauses)) "an even number of forms as clauses"
    )
    (let [g (gensym)
          steps (map (fn [[test step]] `(if ~test (-> ~g ~step) ~g)) (partition 2 clauses))]
        `(let [~g ~expr ~@(interleave (repeat g) (butlast steps))]
            ~(if (seq steps) (last steps) g)
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
    (assert-args
        (even? (count clauses)) "an even number of forms as clauses"
    )
    (let [g (gensym)
          steps (map (fn [[test step]] `(if ~test (->> ~g ~step) ~g)) (partition 2 clauses))]
        `(let [~g ~expr ~@(interleave (repeat g) (butlast steps))]
            ~(if (seq steps) (last steps) g)
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
        ~(if (seq forms) (last forms) name)
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
            ~(if (seq steps) (last steps) g)
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
            ~(if (seq steps) (last steps) g)
        )
    )
)

(defn- preserving-reduced [rf]
    #(let [r (rf %1 %2)]
        (if (reduced? r) (reduced r) r)
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
 ; Returns a transducer that ends transduction when f? returns true for an input.
 ; When retf is supplied it must be a fn of 2 arguments - it will be passed the
 ; (completed) result so far and the input that triggered the predicate, and its
 ; return value (if it does not throw an exception) will be the return value of the
 ; transducer. If retf is not supplied, the input that triggered the predicate will
 ; be returned. If the predicate never returns true the transduction is unaffected.
 ;;
(§ defn halt-when
    ([f?] (halt-when f? nil))
    ([f? retf]
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
                    (if (f? input)
                        (reduced {::halt (if retf (retf (rf result) input) input)})
                        (rf result input)
                    )
                )
            )
        )
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
)

(clojure-ns cloiure.set

;;;
 ; Move a maximal element of coll according to fn k (which returns a number) to the front of coll.
 ;;
(§ defn- bubble-max-key [k coll]
    (let [m (apply max-key k coll)]
        (cons m (remove #(identical? m %) coll))
    )
)

;;;
 ; Return a set that is the union of the input sets.
 ;;
(§ defn union
    ([] #{})
    ([x] x)
    ([x y] (if (< (count x) (count y)) (into y x) (into x y)))
    ([x y & s]
        (let [[x & y] (bubble-max-key count (into s y x))]
            (reduce into x y)
        )
    )
)

;;;
 ; Return a set that is the intersection of the input sets.
 ;;
(§ defn intersection
    ([x] x)
    ([x y] (recur-if (< (count y) (count x)) [y x] => (reduce #(if (contains? y %2) %1 (disj %1 %2)) x x)))
    ([x y & s]
        (let [[x & y] (bubble-max-key #(- (count %)) (conj s y x))]
            (reduce intersection x y)
        )
    )
)

;;;
 ; Return a set that is the first set without elements of the remaining sets.
 ;;
(§ defn difference
    ([x] x)
    ([x y]
        (if (< (count x) (count y))
            (reduce #(if (contains? y %2) (disj %1 %2) %1) x x)
            (reduce disj x y)
        )
    )
    ([x y & s] (reduce difference x (conj s y)))
)

;;;
 ; Returns a set of the elements for which f? is true.
 ;;
(§ defn select [f? xset]
    (reduce (fn [s k] (if (f? k) s (disj s k))) xset xset)
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
)

(clojure-ns cloiure.data
  #_(:require [cloiure.set :as set])

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
    {:diff-similar (fn [a b] ((if (-> a .getClass .isArray) diff-sequential atom-diff) a b))}

    EqualityPartition
    {:equality-partition (fn [x] (if (-> x .getClass .isArray) :sequential :atom))}
)

(§ extend-protocol EqualityPartition
    nil
    (equality-partition [x] :atom)

    IPersistentSet
    (equality-partition [x] :set)

    Sequential
    (equality-partition [x] :sequential)

    IPersistentMap
    (equality-partition [x] :map)
)

(§ defn- as-set-value [s] (if (set? s) s (into #{} s)))

(§ extend-protocol Diff
    IPersistentSet
    (diff-similar [a b]
        (let [aval (as-set-value a) bval (as-set-value b)]
            [
                (not-empty (set/difference aval bval))
                (not-empty (set/difference bval aval))
                (not-empty (set/intersection aval bval))
            ]
        )
    )

    Sequential
    (diff-similar [a b] (diff-sequential a b))

    IPersistentMap
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
)

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
(clojure-ns cloiure.walk

;;;
 ; Traverses form, an arbitrary data structure. inner and outer are functions.
 ; Applies inner to each element of form, building up a data structure of the
 ; same type, then applies outer to the result. Recognizes all Cloiure data
 ; structures. Consumes seqs as with doall.
 ;;
(§ defn walk [inner outer form]
    (cond
        (list? form)      (outer (apply list (map inner form)))
        (map-entry? form) (outer (vec (map inner form)))
        (seq? form)       (outer (doall (map inner form)))
        (coll? form)      (outer (into (empty form) (map inner form)))
        :else             (outer form)
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
)

(defn -main [& args])
