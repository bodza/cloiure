(ns cloiure.ratio
    (:refer-clojure :only [* *ns* + - -> / < <= = == > >= aclone aget alength and aset assoc bit-and bit-not bit-or bit-shift-left bit-shift-right bit-xor byte byte-array case cast complement cond condp cons dec declare defmacro defn dotimes first identical? if-not import inc instance? int int-array into-array let letfn long long-array loop make-array map max min neg? next nil? not or pos? quot rem second some? symbol? unsigned-bit-shift-right update vary-meta vec vector? while zero?])
)

(defmacro § [& _])
(defmacro ß [& _])

(defmacro java-ns  [_ & s] (cons 'do s))
(defmacro class-ns [_ & s] (cons 'do s))

(clojure.core/doseq [% (clojure.core/keys (clojure.core/ns-imports *ns*))] (clojure.core/ns-unmap *ns* %))

(import
    [java.lang Character Class Integer Long RuntimeException String StringBuilder System]
)

(import
    [java.util Arrays Random]
    [java.util.concurrent ThreadLocalRandom]
)

(defmacro throw! [^String s] `(throw (RuntimeException. ~s)))

(defmacro def-      [x & s] `(def      ~(vary-meta x assoc :private true) ~@s))
(defmacro defn-     [x & s] `(defn     ~(vary-meta x assoc :private true) ~@s))
(defmacro defmacro- [x & s] `(defmacro ~(vary-meta x assoc :private true) ~@s))

(letfn [(=> [s] (if (= '=> (first s)) (next s) (cons nil s)))]
    (defmacro     when       [? & s] (let [[e & s] (=> s)]               `(if     ~? (do ~@s) ~e)))
    (defmacro     when-not   [? & s] (let [[e & s] (=> s)]               `(if-not ~? (do ~@s) ~e)))
    (defmacro let-when     [v ? & s] (let [[e & s] (=> s)] `(let ~(vec v) (if     ~? (do ~@s) ~e))))
    (defmacro let-when-not [v ? & s] (let [[e & s] (=> s)] `(let ~(vec v) (if-not ~? (do ~@s) ~e))))
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

(defmacro aswap [a i f & s] `(aset ~a ~i (~f (aget ~a ~i) ~@s)))

(def % rem)

(def & bit-and)
(def | bit-or)

(def << bit-shift-left)
(def >> bit-shift-right)
(def >>> unsigned-bit-shift-right)

(defn abs [a] (if (neg? a) (- a) a))

(java-ns cloiure.math.BigInteger

;;;
 ; Obtain the value of an int as if it were unsigned.
 ;;
(defn #_"long" long! [#_"int" i] (& i 0xffffffff))

;;;
 ; A class used to represent multiprecision integers that makes efficient
 ; use of allocated space by allowing a number to occupy only part of
 ; an array so that the arrays do not have to be reallocated as often.
 ; When performing an operation with many iterations the array used to
 ; hold a number is only reallocated when necessary and does not have to
 ; be the same size as the number it represents. A mutable number allows
 ; calculations to occur on the same number without having to create
 ; a new number for every step of the calculation as occurs with
 ; BigIntegers.
 ;;

(class-ns MutableBigInteger
    ;;;
     ; Holds the magnitude of this MutableBigInteger in big endian order.
     ; The magnitude may start at an offset into the value array, and it may
     ; end before the length of the value array.
     ;;
    (§ field #_"int[]" :value nil)

    ;;;
     ; The number of ints of the value array that are currently used
     ; to hold the magnitude of this MutableBigInteger. The magnitude starts
     ; at an offset and offset + intLen may be less than value.length.
     ;;
    (§ field #_"int" :intLen 0)

    ;;;
     ; The offset into the value array where the magnitude of this
     ; MutableBigInteger begins.
     ;;
    (§ field #_"int" :offset 0)

    ;; constants

    ;;;
     ; The minimum {@code intLen} for cancelling powers of two before dividing.
     ; If the number of ints is less than this threshold, {@code divideKnuth}
     ; does not eliminate common powers of two from the dividend and divisor.
     ;;
    (def #_"int" MutableBigInteger'KNUTH_POW2_THRESH_LEN 6)

    ;;;
     ; The minimum number of trailing zero ints for cancelling powers of two before dividing.
     ; If the dividend and divisor don't share at least this many zero ints at the end,
     ; {@code divideKnuth} does not eliminate common powers of two from the dividend and divisor.
     ;;
    (def #_"int" MutableBigInteger'KNUTH_POW2_THRESH_ZEROS 3)

    ;; constructors

    ;;;
     ; The default constructor. An empty MutableBigInteger is created with a one word capacity.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'empty []
        (let [this (§ new)
              this (assoc this :value (int-array 1))
              this (assoc this :intLen 0)]
            this
        )
    )

    ;;;
     ; Construct a new MutableBigInteger with a magnitude specified by the int i.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'new-i [#_"int" i]
        (let [this (§ new)
              this (assoc this :value (int-array 1))
              this (assoc this :intLen 1)]
            (aset (:value this) 0 i)
            this
        )
    )

    ;;;
     ; Construct a new MutableBigInteger with the specified value array
     ; up to the length of the array supplied.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'new-a [#_"int[]" a]
        (let [this (§ new)
              this (assoc this :value a)
              this (assoc this :intLen (alength a))]
            this
        )
    )

    ;;;
     ; Construct a new MutableBigInteger with a magnitude equal to the
     ; specified BigInteger.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'fromBigInteger [#_"BigInteger" b]
        (let [this (§ new)
              this (assoc this :value (Arrays/copyOf (:mag b), (alength (:mag b))))]
              this (assoc this :intLen (alength (:mag b)))
            this
        )
    )

    ;;;
     ; Construct a new MutableBigInteger with a magnitude equal to the
     ; specified MutableBigInteger.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'copy [#_"MutableBigInteger" m]
        (let [this (§ new)
              this (assoc this :value (Arrays/copyOfRange (:value m), (:offset m), (+ (:offset m) (:intLen m))))
              this (assoc this :intLen (:intLen m))]
            this
        )
    )

    (declare BigInteger'ZERO)
    (declare BigInteger'new-2ai)

    ;;;
     ; Converts a MutableBigInteger to a BigInteger.
     ;;
    (defn #_"BigInteger" MutableBigInteger'toBigInteger [#_"MutableBigInteger" m, #_"int" sign]
        (when-not (or (zero? (:intLen m)) (zero? sign)) => BigInteger'ZERO
            (let [#_"int[]" a
                    (when (or (pos? (:offset m)) (not (== (alength (:value m)) (:intLen m)))) => (:value m)
                        (Arrays/copyOfRange (:value m), (:offset m), (+ (:offset m) (:intLen m)))
                    )]
                (BigInteger'new-2ai a, sign)
            )
        )
    )

    ;;;
     ; Compare the magnitude of two MutableBigIntegers. Returns -1, 0 or 1 as x
     ; MutableBigInteger is numerically less than, equal to, or greater than y.
     ;;
    (defn #_"int" MutableBigInteger'compare [#_"MutableBigInteger" x, #_"MutableBigInteger" y]
        (let [#_"int" cmp (- (:intLen x) (:intLen y))]
            (cond
                (neg? cmp) -1
                (pos? cmp) 1
                :else ;; add Integer.MIN_VALUE to make the comparison act as unsigned integer comparison
                    (let [#_"int" n (+ (:offset x) (:intLen x))]
                        (loop-when [#_"int" i (:offset x) #_"int" j (:offset y)] (< i n) => 0
                            (let [#_"long" a (long! (aget (:value x) i))
                                  #_"long" b (long! (aget (:value y) j))]
                                (cond
                                    (< a b) -1
                                    (< b a) 1
                                    :else (recur (inc i) (inc j))
                                )
                            )
                        )
                    )
            )
        )
    )

    ;;;
     ; Return the index of the lowest set bit in this MutableBigInteger.
     ; If the magnitude of this MutableBigInteger is zero, -1 is returned.
     ;;
    (defn- #_"int" MutableBigInteger'getLowestSetBit [#_"MutableBigInteger" m]
        (let [#_"int" n (:intLen m)]
            (loop-when [#_"int" i n] (pos? i) => -1
                (let [i (dec i) #_"int" x (aget (:value m) (+ (:offset m) i))]
                    (recur-if (zero? x) [i] => (+ (<< (dec (- n i)) 5) (Integer/numberOfTrailingZeros x)))
                )
            )
        )
    )

    ;;;
     ; Ensure that the MutableBigInteger is in normal form, specifically making sure that
     ; there are no leading zeros, and that if the magnitude is zero, then intLen is zero.
     ;;
    #_method
    (defn #_"MutableBigInteger" MutableBigInteger''normalize [#_"MutableBigInteger" this]
        (when (pos? (:intLen this)) => (assoc this :offset 0)
            (let-when [#_"int" i (:offset this)] (zero? (aget (:value this) i)) => this
                (let [#_"int" n (+ i (:intLen this))
                      i (loop-when-recur [i (inc i)] (and (< i n) (zero? (aget (:value this) i))) [(inc i)] => i)
                      #_"int" n (- i (:offset this))
                      this (update this :intLen - n)
                      this (assoc this :offset (if (pos? (:intLen this)) (+ (:offset this) n) 0))]
                    this
                )
            )
        )
    )

    ;;;
     ; Returns true iff this MutableBigInteger has a value of one.
     ;;
    (defn #_"boolean" MutableBigInteger'isOne [#_"MutableBigInteger" this]
        (and (== (:intLen this) 1) (== (aget (:value this) (:offset this)) 1))
    )

    ;;;
     ; Returns true iff this MutableBigInteger has a value of zero.
     ;;
    (defn #_"boolean" MutableBigInteger'isZero [#_"MutableBigInteger" this]
        (zero? (:intLen this))
    )

    ;;;
     ; Returns true iff this MutableBigInteger is even.
     ;;
    (defn #_"boolean" MutableBigInteger'isEven [#_"MutableBigInteger" this]
        (or (zero? (:intLen this)) (zero? (& (aget (:value this) (dec (+ (:offset this) (:intLen this)))) 1)))
    )

    ;;;
     ; Returns a String representation of this MutableBigInteger in radix 10.
     ;;
    #_foreign
    (defn #_"String" toString---MutableBigInteger [#_"MutableBigInteger" this]
        (.toString (MutableBigInteger'toBigInteger this, 1))
    )

    ;;;
     ; Right shift this MutableBigInteger n bits, where n is less than 32.
     ; Assumes pos? intLen and pos? n for speed.
     ;;
    #_method
    (defn- #_"MutableBigInteger" MutableBigInteger''primitiveRightShift [#_"MutableBigInteger" this, #_"int" n]
        (let [#_"int[]" a (:value this) #_"int" m (- 32 n)]
            (loop-when [#_"int" i (dec (+ (:offset this) (:intLen this))) #_"int" c (aget a i)] (< (:offset this) i)
                (let [#_"int" b c c (aget a (dec i))]
                    (aset a i (| (<< c m) (>>> b n)))
                    (recur (dec i) c)
                )
            )
            (aswap a (:offset this) >>> n)
            this
        )
    )

    ;;;
     ; Left shift this MutableBigInteger n bits, where n is less than 32.
     ; Assumes pos? intLen and pos? n for speed.
     ;;
    #_method
    (defn- #_"MutableBigInteger" MutableBigInteger''primitiveLeftShift [#_"MutableBigInteger" this, #_"int" n]
        (let [#_"int[]" a (:value this) #_"int" m (- 32 n)]
            (loop-when [#_"int" i (:offset this) #_"int" c (aget a i)] (< i (dec (+ (:offset this) (:intLen this))))
                (let [#_"int" b c c (aget a (inc i))]
                    (aset a i (| (<< b n) (>>> c m)))
                    (recur (inc i) c)
                )
            )
            (aswap a (dec (+ (:offset this) (:intLen this))) << n)
            this
        )
    )

    (declare BigInteger'bitLengthForInt)

    ;;;
     ; Right shift this MutableBigInteger shift bits.
     ; The MutableBigInteger is left in normal form.
     ;;
    #_method
    (defn #_"MutableBigInteger" MutableBigInteger''rightShift [#_"MutableBigInteger" this, #_"int" shift]
        (when (pos? (:intLen this)) => this
            (let-when [this (update this :intLen - (>>> shift 5)) #_"int" nBits (& shift 31)] (pos? nBits) => this
                (if (<= (BigInteger'bitLengthForInt (aget (:value this) (:offset this))) nBits)
                    (update (MutableBigInteger''primitiveLeftShift this, (- 32 nBits)) :intLen dec)
                    (MutableBigInteger''primitiveRightShift this, nBits)
                )
            )
        )
    )

    ;;;
     ; Left shift this MutableBigInteger shift bits.
     ;
     ; If there is enough storage space in this MutableBigInteger already the available
     ; space will be used. Space to the right of the used ints in the value array is faster
     ; to utilize, so the extra space will be taken from the right if possible.
     ;;
    #_method
    (defn #_"MutableBigInteger" MutableBigInteger''leftShift [#_"MutableBigInteger" this, #_"int" shift]
        (when (pos? (:intLen this)) => this
            (let [#_"int" nInts (>>> shift 5) #_"int" nBits (& shift 31)
                  #_"int" mBits (BigInteger'bitLengthForInt (aget (:value this) (:offset this)))]
                ;; if shift can be done without moving words, do so
                (when (< (- 32 mBits) shift) => (MutableBigInteger''primitiveLeftShift this, nBits)
                    (let [#_"int[]" a (:value this)
                          #_"int" n (+ (:intLen this) nInts (if (<= nBits (- 32 mBits)) 0 1))
                          this
                            (cond (< (alength a) n) ;; the array must grow
                                (let [#_"int[]" b (int-array n)]
                                    (dotimes [#_"int" i (:intLen this)]
                                        (aset b i (aget a (+ (:offset this) i)))
                                    )
                                    (assoc this :value b :offset 0 :intLen n)
                                )
                                (< (alength a) (+ (:offset this) n)) ;; must use space on left
                                (do
                                    (dotimes [#_"int" i (:intLen this)]
                                        (aset a i (aget a (+ (:offset this) i)))
                                    )
                                    (loop-when-recur [#_"int" i (:intLen this)] (< i n) [(inc i)]
                                        (aset a i 0)
                                    )
                                    (assoc this :value a :offset 0 :intLen n)
                                )
                                :else ;; use space on right
                                (do
                                    (loop-when-recur [#_"int" i (:intLen this)] (< i n) [(inc i)]
                                        (aset a (+ (:offset this) i) 0)
                                    )
                                    (assoc this :intLen n)
                                )
                            )]
                        (when (pos? nBits) => this
                            (if (<= nBits (- 32 mBits))
                                (MutableBigInteger''primitiveLeftShift this, nBits)
                                (MutableBigInteger''primitiveRightShift this, (- 32 nBits))
                            )
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Adds the contents of two MutableBigInteger objects.
     ; The result is placed within this MutableBigInteger.
     ; The contents of 'that' addend are not changed.
     ;;
    #_method
    (defn #_"MutableBigInteger" MutableBigInteger''add [#_"MutableBigInteger" this, #_"MutableBigInteger" that]
        (let [#_"int[]" a (:value this) #_"int" ao (:offset this) #_"int" ai (:intLen this)
              #_"int[]" b (:value that) #_"int" bo (:offset that) #_"int" bi (:intLen that)
              #_"int" n (max ai bi) #_"int[]" c (if (< (alength a) n) (int-array n) a)
              ;; add common parts of both numbers
              [ai bi #_"int" ci #_"long" carry]
                (loop-when [ai ai bi bi ci (dec (alength c)) carry 0] (and (pos? ai) (pos? bi)) => [ai bi ci carry]
                    (let [ai (dec ai) bi (dec bi)
                          #_"long" sum (+ (long! (aget a (+ ao ai))) (long! (aget b (+ bo bi))) carry)]
                        (aset c ci (int sum))
                        (recur ai bi (dec ci) (>>> sum 32))
                    )
                )
              ;; add remainder of the longer number
              [ci carry :as _]
                (loop-when [ai ai ci ci carry carry] (pos? ai) => [ci carry]
                    (let [ai (dec ai)]
                        (when-not (and (zero? carry) (identical? c a) (== ci (+ ao ai))) => nil
                            (let [#_"long" sum (+ (long! (aget a (+ ao ai))) carry)]
                                (aset c ci (int sum))
                                (recur ai (dec ci) (>>> sum 32))
                            )
                        )
                    )
                )]
            (when (some? _) => this
                (let [[ci carry]
                        (loop-when [bi bi ci ci carry carry] (pos? bi) => [ci carry]
                            (let [bi (dec bi)
                                  #_"long" sum (+ (long! (aget b (+ bo bi))) carry)]
                                (aset c ci (int sum))
                                (recur bi (dec ci) (>>> sum 32))
                            )
                        )
                      [c n]
                        (when (pos? carry) => [c n]
                            ;; result must grow in length
                            (let-when [n (inc n)] (< (alength c) n) => (do (aset c ci 1) [c n])
                                (let [#_"int[]" _ (int-array n)]
                                    (System/arraycopy c, 0, _, 1, (alength c))
                                    (aset _ 0 1)
                                    [_ n]
                                )
                            )
                        )]
                    (assoc this :value c :offset (- (alength c) n) :intLen n)
                )
            )
        )
    )

    ;;;
     ; Subtracts the smaller of 'this' and 'that' from the larger and places the result into 'this'.
     ;;
    #_method
    (defn #_"[MutableBigInteger int]" MutableBigInteger''subtract [#_"MutableBigInteger" this, #_"MutableBigInteger" that]
        (let [#_"int" sign (MutableBigInteger'compare this, that)]
            (if (zero? sign)
                [(assoc this :offset 0 :intLen 0) sign]
                (let [[#_"MutableBigInteger" a #_"MutableBigInteger" b] (if (neg? sign) [that this] [this that])
                      #_"int" n (:intLen a) #_"int[]" c (:value this) c (if (< (alength c) n) (int-array n) c)
                      ;; subtract common parts of both numbers
                      [#_"long" diff #_"int" ai #_"int" ci]
                        (loop-when [diff 0 ai (:intLen a) #_"int" bi (:intLen b) ci (dec (alength c))] (pos? bi) => [diff ai ci]
                            (let [ai (dec ai) bi (dec bi)
                                  diff (- (long! (aget (:value a) (+ (:offset a) ai))) (long! (aget (:value b) (+ (:offset b) bi))) (int (- (>> diff 32))))]
                                (aset c ci (int diff))
                                (recur diff ai bi (dec ci))
                            )
                        )
                      ;; subtract remainder of longer number
                      _ (loop-when [diff diff ai ai ci ci] (pos? ai)
                            (let [ai (dec ai)
                                  diff (- (long! (aget (:value a) (+ (:offset a) ai))) (int (- (>> diff 32))))]
                                (aset c ci (int diff))
                                (recur diff ai (dec ci))
                            )
                        )]
                    [(MutableBigInteger''normalize (assoc this :value c :offset (- (alength c) n) :intLen n)) sign]
                )
            )
        )
    )

    ;;;
     ; Subtracts the smaller of x and y from the larger and places the result into the larger.
     ; Returns 1 if the answer is in x, -1 if in y, 0 if no operation was performed.
     ;;
    (defn- #_"[MutableBigInteger MutableBigInteger int]" MutableBigInteger'difference [#_"MutableBigInteger" x, #_"MutableBigInteger" y]
        (let [#_"int" sign (MutableBigInteger'compare x, y)]
            (if (zero? sign)
                [x y sign]
                (let [[x y] (if (neg? sign) [y x] [x y])
                      #_"int[]" a (:value x) #_"int[]" b (:value y) #_"int" ao (:offset x) #_"int" bo (:offset y)
                      ;; subtract common parts of both numbers
                      [#_"long" diff #_"int" ai]
                        (loop-when [diff 0 ai (:intLen x) #_"int" bi (:intLen y)] (pos? bi) => [diff ai]
                            (let [ai (dec ai) bi (dec bi)
                                  diff (- (long! (aget a (+ ao ai))) (long! (aget b (+ bo bi))) (int (- (>> diff 32))))]
                                (aset a (+ ao ai) (int diff))
                                (recur diff ai bi)
                            )
                        )
                      ;; subtract remainder of longer number
                      _ (loop-when [diff diff ai ai] (pos? ai)
                            (let [ai (dec ai)
                                  diff (- (long! (aget a (+ ao ai))) (int (- (>> diff 32))))]
                                (aset a (+ ao ai) (int diff))
                                (recur diff ai)
                            )
                        )
                      x (MutableBigInteger''normalize x)]
                    (if (neg? sign) [y x sign] [x y sign])
                )
            )
        )
    )

    ;;;
     ; Multiply the contents of x by y. The product is placed into z. The contents of x and y are not changed.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'multiply [#_"MutableBigInteger" x, #_"MutableBigInteger" y]
        (let [#_"int[]" x* (:value x) #_"int" xo (:offset x) #_"int" xn (:intLen x)
              #_"int[]" y* (:value y) #_"int" yo (:offset y) #_"int" yn (:intLen y)
              #_"int[]" z* (int-array (+ xn yn))]
            ;; perform the multiplication word by word
            (loop-when-recur [#_"int" xi (dec xn)] (<= 0 xi) [(dec xi)]
                (loop-when [#_"long" carry 0 #_"int" yi (dec yn) #_"int" zi (+ yn xi)] (<= 0 yi) => (aset z* zi (int carry))
                    (let [#_"long" product (+ (* (long! (aget y* (+ yo yi))) (long! (aget x* (+ xo xi)))) (long! (aget z* zi)) carry)]
                        (aset z* zi (int product))
                        (recur (>>> product 32) (dec yi) (dec zi))
                    )
                )
            )
            ;; remove leading zeros from product
            (MutableBigInteger''normalize (MutableBigInteger'new-a z*))
        )
    )

    ;;;
     ; Multiply the contents of MutableBigInteger x by word y. The product is placed into z.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'mul [#_"MutableBigInteger" x, #_"int" y]
        (case y
            0 (MutableBigInteger'empty)
            1 (MutableBigInteger'copy x)
            ;; perform the multiplication word by word
            (let [#_"int" n (:intLen x) #_"int[]" a (int-array (inc n))
                  #_"long" carry
                    (loop-when [carry 0 #_"int" i (dec n)] (<= 0 i) => carry
                        (let [#_"long" product (+ (* (long! y) (long! (aget (:value x) (+ (:offset x) i)))) carry)]
                            (aset a (inc i) (int product))
                            (recur (>>> product 32) (dec i))
                        )
                    )
                  #_"MutableBigInteger" z (MutableBigInteger'new-a a)]
                (if (zero? carry)
                    (assoc z :offset 1 :intLen n)
                    (do (aset a 0 (int carry)) z)
                )
            )
        )
    )

    ;;;
     ; This method divides a long quantity by an int to estimate qhat for two multi
     ; precision numbers. It is used when the signed value of x is less than zero.
     ; Returns long value with high 32 bits remainder and low 32 bits quotient.
     ;;
    (defn- #_"long" MutableBigInteger'divWord [#_"long" x, #_"int" y]
        (let [[#_"long" r #_"long" q]
                (when-not (== y 1) => [0 (int x)]
                    (let [#_"long" y' (long! y)
                          ;; approximate the quotient and remainder
                          q (quot (>>> x 1) (>>> y' 1)) r (- x (* q y'))
                          ;; correct the approximation
                          [r q] (loop-when-recur [r r q q] (neg? r) [(+ r y') (dec q)] => [r q])]
                        ;; when x - q * y' == r, where 0 <= r < y', we're done
                        (loop-when-recur [r r q q] (<= y' r) [(- r y') (inc q)] => [r q])
                    )
                )]
            (| (<< r 32) (long! q))
        )
    )

    ;;;
     ; Divide a multi-word x by a single-word y.
     ;;
    (defn #_"[MutableBigInteger int]" MutableBigInteger'divideOneWord [#_"MutableBigInteger" x, #_"int" y]
        (let [#_"int" n (:intLen x) #_"long" y' (long! y)]
            (case n
                0 [(MutableBigInteger'empty) 0]
                1 ;; special case of one word dividend
                (let [#_"long" x' (long! (aget (:value x) (:offset x))) #_"int" q (int (quot x' y'))]
                    [(if (zero? q) (MutableBigInteger'empty) (MutableBigInteger'new-i q)) (int (- x' (* q y')))]
                )
                (let [#_"int[]" x* (:value x) #_"int[]" q* (int-array n)
                      ;; normalize the divisor
                      #_"int" r (aget x* (:offset x))
                      r (let-when [#_"long" r' (long! r)] (<= y' r') => r
                            (let [#_"int" q (int (quot r' y'))]
                                (aset q* 0 q)
                                (int (- r' (* q y')))
                            )
                        )
                      r (loop-when [r r #_"int" i (dec n)] (pos? i) => r
                            (let [#_"long" x' (| (<< (long! r) 32) (long! (aget x* (+ (:offset x) (- n i)))))
                                  [#_"int" q r]
                                    (if (<= 0 x')
                                        (let [q (int (quot x' y'))]
                                            [q (int (- x' (* q y')))]
                                        )
                                        (let [_ (MutableBigInteger'divWord x', y)]
                                            [(int (long! _)) (int (>>> _ 32))]
                                        )
                                    )]
                                (aset q* (- n i) q)
                                (recur r (dec i))
                            )
                        )
                      ;; unnormalize the divisor
                      r (if (pos? (Integer/numberOfLeadingZeros y)) (% r y) r)]
                    [(MutableBigInteger''normalize (MutableBigInteger'new-a q*)) r]
                )
            )
        )
    )

    (defn- #_"void" MutableBigInteger'copyAndShift [#_"int[]" a, #_"int" ao, #_"int" n, #_"int[]" b, #_"int" bo, #_"int" shift]
        (let [#_"int" shift' (- 32 shift)]
            (loop-when [#_"int" ai ao #_"int" c (aget a ai) #_"int" bi 0] (< bi (dec n)) => (aset b (+ bo bi) (<< c shift))
                (let [#_"int" c' c ai (inc ai) c (aget a ai)]
                    (aset b (+ bo bi) (| (<< c' shift) (>>> c shift')))
                    (recur ai c (inc bi))
                )
            )
        )
        nil
    )

    ;;;
     ; Compare two longs as if they were unsigned.
     ; Returns true iff one is greater than two.
     ;;
    (defn- #_"boolean" MutableBigInteger'unsignedLongGreater [#_"long" one, #_"long" two]
        (> (+ one Long/MIN_VALUE) (+ two Long/MIN_VALUE))
    )

    ;;;
     ; This method is used for division. It multiplies an n word input a by one word input x, and subtracts
     ; the n word product from q. This is needed when subtracting qhat * divisor from dividend.
     ;;
    (defn- #_"int" MutableBigInteger'mulsub [#_"int[]" q, #_"int[]" a, #_"int" x, #_"int" offset]
        (let [#_"long" x' (long! x)]
            (loop-when [offset (+ offset (alength a)) #_"long" carry 0 #_"int" i (dec (alength a))] (<= 0 i) => (int carry)
                (let [#_"long" mul (+ (* (long! (aget a i)) x') carry) #_"long" sub (- (aget q offset) mul)]
                    (aset q offset (int sub))
                    (recur (dec offset) (+ (>>> mul 32) (if (< (long! (bit-not (int mul))) (long! sub)) 1 0)) (dec i))
                )
            )
        )
    )

    ;;;
     ; A primitive used for division. This method adds in one multiple of the divisor a back to the dividend
     ; b at a specified offset. It is used when qhat was estimated too large, and must be adjusted.
     ;;
    (defn- #_"int" MutableBigInteger'divadd [#_"int[]" a, #_"int[]" b, #_"int" offset]
        (loop-when [#_"long" carry 0 #_"int" i (dec (alength a))] (<= 0 i) => (int carry)
            (let [#_"long" sum (+ (long! (aget a i)) (long! (aget b (+ offset i))) carry)]
                (aset b (+ offset i) (int sum))
                (recur (>>> sum 32) (dec i))
            )
        )
    )

    (defn- #_"[MutableBigInteger MutableBigInteger]" MutableBigInteger'divideMagnitude [#_"MutableBigInteger" x, #_"MutableBigInteger" y]
        (when (< 1 (:intLen y)) => (throw! "multi-word divisor expected")
            ;; D1 normalize the divisor
            (let [#_"int" shift (Integer/numberOfLeadingZeros (aget (:value y) (:offset y)))
                  ;; copy divisor to protect it ;; remainder starts as dividend with space for a leading zero
                  [#_"int[]" d* #_"int[]" r*]
                    (if (pos? shift)
                        (let [#_"int[]" d* (int-array (:intLen y))]
                            (MutableBigInteger'copyAndShift (:value y), (:offset y), (:intLen y), d*, 0, shift)
                            (if (<= shift (Integer/numberOfLeadingZeros (aget (:value x) (:offset x))))
                                (let [#_"int" n (:intLen x) #_"int[]" r* (int-array (inc n))]
                                    (MutableBigInteger'copyAndShift (:value x), (:offset x), n, r*, 1, shift)
                                    [d* r*]
                                )
                                (let [#_"int" n (:intLen x) #_"int[]" r* (int-array (+ n 2)) #_"int" shift' (- 32 shift)]
                                    (loop-when [#_"int" i (:offset x) #_"int" c 0 #_"int" j 1] (< j (inc n)) => (aset r* j (<< c shift))
                                        (let [#_"int" c' c c (aget (:value x) i)]
                                            (aset r* j (| (<< c' shift) (>>> c shift')))
                                            (recur (inc i) c (inc j))
                                        )
                                    )
                                    [d* r*]
                                )
                            )
                        )
                        (let [#_"int[]" d* (Arrays/copyOfRange (:value y), (:offset y), (+ (:offset y) (:intLen y)))
                              #_"int" n (:intLen x) #_"int[]" r* (int-array (inc n))]
                            (System/arraycopy (:value x), (:offset x), r*, 1, n)
                            [d* r*]
                        )
                    )
                  ;; set the quotient size
                  #_"int" n (- (alength r*) (alength d*)) #_"int[]" q* (int-array (max 1 n))
                  #_"int" d0 (aget d* 0) #_"long" d0' (long! d0) #_"long" d1' (long! (aget d* 1))]

                ;; D2 initialize i ;; D7 loop on i
                (dotimes [#_"int" i (dec n)]
                    ;; D3 calculate q ;; estimate q
                    (let [#_"int" r0 (aget r* i) #_"long" r0' (long! r0) #_"int" r1 (aget r* (inc i))
                          [#_"int" q #_"int" r #_"boolean" skip?]
                            (if (== r0 d0)
                                (let [q (bit-not 0) r (+ r0 r1)]
                                    [q r (< (long! r) r0')]
                                )
                                (let [#_"long" w (| (<< (long r0) 32) (long! r1))]
                                    (if (<= 0 w)
                                        (let [q (int (quot w d0')) r (int (- w (* q d0')))]
                                            [q r false]
                                        )
                                        (let [_ (MutableBigInteger'divWord w, d0)]
                                            [(int (long! _)) (int (>>> _ 32)) false]
                                        )
                                    )
                                )
                            )]
                        (when-not (zero? q)
                            (let [[q r] ;; correct q
                                    (when-not skip? => [q r]
                                        (let [#_"long" product (* d1' (long! q)) #_"long" r2' (long! (aget r* (+ i 2)))]
                                            (when (MutableBigInteger'unsignedLongGreater product, (| (<< (long! r) 32) r2')) => [q r]
                                                (let [q (dec q) r (int (+ (long! r) d0'))]
                                                    (when (<= d0' (long! r)) => [q r]
                                                        (let [product (- product d1')]
                                                            (when (MutableBigInteger'unsignedLongGreater product, (| (<< (long! r) 32) r2')) => [q r]
                                                                [(dec q) r]
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )]
                                ;; D4 multiply and subtract
                                (aset r* i 0)
                                (let [#_"int" borrow (MutableBigInteger'mulsub r*, d*, q, i)
                                      ;; D5 test remainder
                                      q (when (< r0' (long! borrow)) => q
                                            ;; D6 add back
                                            (MutableBigInteger'divadd d*, r*, (inc i))
                                            (dec q)
                                        )]
                                    ;; store the quotient digit
                                    (aset q* i q)
                                )
                            )
                        )
                    )
                )

                (let [#_"int" i (dec n)]
                    ;; D3 Calculate q ;; estimate q
                    (let [#_"int" r0 (aget r* i) #_"long" r0' (long! r0) #_"int" r1 (aget r* (inc i))
                          [#_"int" q #_"int" r #_"boolean" skip?]
                            (if (== r0 d0)
                                (let [q (bit-not 0) r (+ r0 r1)]
                                    [q r (< (long! r) r0')]
                                )
                                (let [#_"long" w (| (<< (long r0) 32) (long! r1))]
                                    (if (<= 0 w)
                                        (let [q (int (quot w d0')) r (int (- w (* q d0')))]
                                            [q r false]
                                        )
                                        (let [_ (MutableBigInteger'divWord w, d0)]
                                            [(int (long! _)) (int (>>> _ 32)) false]
                                        )
                                    )
                                )
                            )]
                        (when-not (zero? q)
                            (let [[q r] ;; correct q
                                    (when-not skip? => [q r]
                                        (let [#_"long" product (* d1' (long! q)) #_"long" r2' (long! (aget r* (+ i 2)))]
                                            (when (MutableBigInteger'unsignedLongGreater product, (| (<< (long! r) 32) r2')) => [q r]
                                                (let [q (dec q) r (int (+ (long! r) d0'))]
                                                    (when (<= d0' (long! r)) => [q r]
                                                        (let [product (- product d1')]
                                                            (when (MutableBigInteger'unsignedLongGreater product, (| (<< (long! r) 32) r2')) => [q r]
                                                                [(dec q) r]
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )]
                                ;; D4 multiply and subtract
                                (aset r* i 0)
                                (let [#_"int" borrow (MutableBigInteger'mulsub r*, d*, q, i)
                                      ;; D5 test remainder
                                      q (when (< r0' (long! borrow)) => q
                                            ;; D6 add back
                                            (MutableBigInteger'divadd d*, r*, (inc i))
                                            (dec q)
                                        )]
                                    ;; store the quotient digit
                                    (aset q* i q)
                                )
                            )
                        )
                    )
                )

                (let [#_"MutableBigInteger" q (assoc (MutableBigInteger'new-a q*) :intLen n)
                      #_"MutableBigInteger" r (MutableBigInteger'new-a r*)
                      ;; D8 unnormalize
                      r (if (pos? shift) (MutableBigInteger''rightShift r, shift) r)]
                    [(MutableBigInteger''normalize q) (MutableBigInteger''normalize r)]
                )
            )
        )
    )

    ;;;
     ; Calculates the quotient and remainder of x div y.
     ;
     ; Uses Algorithm D in Knuth section 4.3.1.
     ; Many optimizations to that algorithm have been adapted from the Colin Plumb C library.
     ; It special cases one word divisors for speed. The content of divisor is not changed.
     ;;
    (defn #_"[MutableBigInteger MutableBigInteger]" MutableBigInteger'divide [#_"MutableBigInteger" x, #_"MutableBigInteger" y]
        (cond
            (MutableBigInteger'isZero y) (throw! "divide by zero")
            (MutableBigInteger'isZero x) [(MutableBigInteger'empty) (MutableBigInteger'empty)] ;; dividend is zero
            :else
            (let [#_"int" cmp (MutableBigInteger'compare x, y)]
                (cond
                    (neg? cmp) [(MutableBigInteger'empty) (MutableBigInteger'copy x)] ;; dividend less than divisor
                    (zero? cmp) [(MutableBigInteger'new-i 1) (MutableBigInteger'empty)] ;; dividend equal to divisor
                    (== (:intLen y) 1) ;; special case one word divisor
                    (let [[#_"MutableBigInteger" q #_"int" r] (MutableBigInteger'divideOneWord x, (aget (:value y) (:offset y)))]
                        [q (if (zero? r) (MutableBigInteger'empty) (MutableBigInteger'new-i r))]
                    )
                    :else ;; cancel common powers of two if we're above the KNUTH_POW2_* thresholds
                    (when (<= MutableBigInteger'KNUTH_POW2_THRESH_LEN (:intLen x)) => (MutableBigInteger'divideMagnitude x, y)
                        (let [#_"int" shift (min (MutableBigInteger'getLowestSetBit x) (MutableBigInteger'getLowestSetBit y))]
                            (when (<= (<< MutableBigInteger'KNUTH_POW2_THRESH_ZEROS 5) shift) => (MutableBigInteger'divideMagnitude x, y)
                                (let [x (-> x (MutableBigInteger'copy) (MutableBigInteger''rightShift shift))
                                      y (-> y (MutableBigInteger'copy) (MutableBigInteger''rightShift shift))
                                      [#_"MutableBigInteger" q #_"MutableBigInteger" r] (MutableBigInteger'divide x, y)]
                                    [q (MutableBigInteger''leftShift r, shift)]
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Calculate GCD of a and b interpreted as unsigned integers.
     ;;
    (defn- #_"int" MutableBigInteger'binaryGCD-i [#_"int" a, #_"int" b]
        (cond (zero? b) a (zero? a) b
            :else ;; right shift a & b till their last bits equal to 1
            (let [#_"int" n (Integer/numberOfTrailingZeros a) #_"int" m (Integer/numberOfTrailingZeros b)]
                (loop-when [a (>>> a n) b (>>> b m)] (not (== a b)) => (<< a (min n m))
                    (if (> (long! a) (long! b)) ;; a > b as unsigned
                        (recur   (let [a (- a b)] (>>> a (Integer/numberOfTrailingZeros a))) b)
                        (recur a (let [b (- b a)] (>>> b (Integer/numberOfTrailingZeros b)))  )
                    )
                )
            )
        )
    )

    ;;;
     ; Calculate GCD of u and v.
     ; Assumes that u and v are not zero.
     ;
     ; Algorithm B from Knuth section 4.5.2.
     ;;
    (defn- #_"MutableBigInteger" MutableBigInteger'binaryGCD-m [#_"MutableBigInteger" u, #_"MutableBigInteger" v]
        ;; step B1
        (let [#_"int" s1 (MutableBigInteger'getLowestSetBit u) #_"int" s2 (MutableBigInteger'getLowestSetBit v)
              #_"int" k (min s1 s2)
              [u v] (if (zero? k) [u v] [(MutableBigInteger''rightShift u, k) (MutableBigInteger''rightShift v, k)])
              ;; step B2
              [#_"MutableBigInteger" t #_"int" s] (if (== k s1) [v -1] [u 1])]
            (loop [u u v v t t s s]
                (let-when [#_"int" l (MutableBigInteger'getLowestSetBit t)] (<= 0 l) => (if (pos? k) (MutableBigInteger''leftShift u, k) u)
                    ;; steps B3 and B4 ;; step B5
                    (let [t (MutableBigInteger''rightShift t, l) [u v] (if (pos? s) [t v] [u t])]
                        ;; special case one word numbers
                        (if (and (< (:intLen u) 2) (< (:intLen v) 2))
                            (let [#_"int" x (aget (:value u) (:offset u)) #_"int" y (aget (:value v) (:offset v))
                                  #_"MutableBigInteger" r (MutableBigInteger'new-i (MutableBigInteger'binaryGCD-i x, y))]
                                (if (pos? k) (MutableBigInteger''leftShift r, k) r)
                            )
                            ;; step B6
                            (let [[u v s] (MutableBigInteger'difference u, v)]
                                (if (zero? s)
                                    (if (pos? k) (MutableBigInteger''leftShift u, k) u)
                                    (recur u v (if (neg? s) v u) s)
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Calculate GCD of a and b.
     ;
     ; Use Euclid's algorithm until the numbers are approximately the
     ; same length, then use the binary GCD algorithm to find the GCD.
     ;
     ; a and b are changed by the computation!
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'hybridGCD [#_"MutableBigInteger" a, #_"MutableBigInteger" b]
        (when (pos? (:intLen b)) => a
            (if (< (abs (- (:intLen a) (:intLen b))) 2)
                (MutableBigInteger'binaryGCD-m a, b)
                (let [[_ #_"MutableBigInteger" r] (MutableBigInteger'divide a, b)]
                    (recur b r)
                )
            )
        )
    )

    ;;;
     ; Returns the multiplicative inverse of i mod 2^32. Assumes i is odd.
     ;;
    (defn #_"int" MutableBigInteger'inverseMod32 [#_"int" i]
        ;; Newton's iteration!
        (let [#_"int" t i
              t (- (* t 2) (* i t))
              t (- (* t 2) (* i t))
              t (- (* t 2) (* i t))
              t (- (* t 2) (* i t))]
            t
        )
    )

    ;;;
     ; The Fixup Algorithm
     ; Calculates X such that X = C * 2^(-k) (mod P)
     ; Assumes C < P and P is odd.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'fixup [#_"MutableBigInteger" c, #_"MutableBigInteger" p, #_"int" k]
        ;; set r to the multiplicative inverse of p mod 2^32
        (let [#_"int" r (- (MutableBigInteger'inverseMod32 (aget (:value p) (dec (+ (:offset p) (:intLen p))))))
              #_"int" n (>> k 5)
              c (loop-when [c c #_"int" i 0] (< i n) => c
                    (let [#_"int" v (* r (aget (:value c) (dec (+ (:offset c) (:intLen c))))) ;; V = R * c (mod 2^j)
                          c (MutableBigInteger''add c, (MutableBigInteger'mul p, v))          ;; c = c + (v * p)
                          c (update c :intLen dec)]                                           ;; c = c / 2^j
                        (recur c (inc i))
                    )
                )
              #_"int" m (& k 31)
              c (when-not (zero? m) => c
                    (let [#_"int" v (* r (aget (:value c) (dec (+ (:offset c) (:intLen c))))) ;; V = R * c (mod 2^j)
                          v (& v (dec (<< 1 m)))
                          c (MutableBigInteger''add c, (MutableBigInteger'mul p, v))          ;; c = c + (v * p)
                          c (MutableBigInteger''rightShift c, m)]                             ;; c = c / 2^j
                        c
                    )
                )]
            ;; In theory, c may be greater than p at this point (Very rare!)
            (loop-when [c c] (<= 0 (MutableBigInteger'compare c, p)) => c
                (let [[c _] (MutableBigInteger''subtract c, p)]
                    (recur c)
                )
            )
        )
    )

    ;;;
     ; Calculate the multiplicative inverse of a mod b, where b is odd.
     ; a and b are not changed by the calculation.
     ;
     ; This method implements an algorithm due to Richard Schroeppel,
     ; that uses the same intermediate representation as Montgomery Reduction ("Montgomery Form").
     ; The algorithm is described in an unpublished manuscript entitled "Fast Modular Reciprocals".
     ;;
    #_method
    (defn- #_"MutableBigInteger" MutableBigInteger''modInverse [#_"MutableBigInteger" a, #_"MutableBigInteger" b]
        (let [#_"MutableBigInteger" f (MutableBigInteger'copy a)
              #_"MutableBigInteger" g (MutableBigInteger'copy b)
              #_"MutableBigInteger" d (MutableBigInteger'empty)
              ;; right shift f k times until odd, left shift d k times
              [f d #_"int" k]
                (when (MutableBigInteger'isEven f) => [f d 0]
                    (let [#_"int" shift (MutableBigInteger'getLowestSetBit f)]
                        [(MutableBigInteger''rightShift f, shift) (MutableBigInteger''leftShift d, shift) shift]
                    )
                )
              ;; the Almost Inverse Algorithm
              [#_"MutableBigInteger" c #_"int" cSign k]
                (loop-when [f f g g c (MutableBigInteger'new-i 1) cSign 1 d d #_"int" dSign 1 k k] (not (MutableBigInteger'isOne f)) => [c cSign k]
                    ;; if gcd(f, g) != 1, number is not invertible modulo mod
                    (when-not (MutableBigInteger'isZero f) => (throw! "not invertible")
                        ;; if f < g exchange f, g and c, d
                        (let [[f g c cSign d dSign] (if (neg? (MutableBigInteger'compare f, g)) [g f d dSign c cSign] [f g c cSign d dSign])
                              [f c cSign]
                                (if (zero? (& (bit-xor (aget (:value f) (dec (+ (:offset f) (:intLen f)))) (aget (:value g) (dec (+ (:offset g) (:intLen g))))) 3))
                                    (let [[f _] (MutableBigInteger''subtract f, g)] ;; if f == g (mod 4)
                                        (if (== cSign dSign)
                                            (let [[c _] (MutableBigInteger''subtract c, d)]
                                                [f c (* cSign _)]
                                            )
                                            [f (MutableBigInteger''add c, d) cSign]
                                        )
                                    )
                                    (let [f (MutableBigInteger''add f, g)] ;; if f != g (mod 4)
                                        (if (== cSign dSign)
                                            [f (MutableBigInteger''add c, d) cSign]
                                            (let [[c _] (MutableBigInteger''subtract c, d)]
                                                [f c (* cSign _)]
                                            )
                                        )
                                    )
                                )
                              ;; right shift f k times until odd, left shift d k times
                              #_"int" shift (MutableBigInteger'getLowestSetBit f)]
                            (recur (MutableBigInteger''rightShift f, shift) g c cSign (MutableBigInteger''leftShift d, shift) dSign (+ k shift))
                        )
                    )
                )
              c (loop-when [c c cSign cSign] (neg? cSign) => c
                    (if (== cSign 1)
                        (recur (MutableBigInteger''add c, b) cSign)
                        (let [[c _] (MutableBigInteger''subtract c, b)]
                            (recur c (* cSign _))
                        )
                    )
                )]
            (MutableBigInteger'fixup c, b, k)
        )
    )

    ;;;
     ; Extended Euclidean algorithm to compute the multiplicative inverse of a mod 2^k.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'euclidModInverse [#_"MutableBigInteger" a, #_"int" k]
        (let [#_"MutableBigInteger" b (MutableBigInteger''leftShift (MutableBigInteger'new-i 1), k)
              #_"MutableBigInteger" m (MutableBigInteger'copy b)
              [#_"MutableBigInteger" c b] (MutableBigInteger'divide b, a)
              #_"MutableBigInteger" d (MutableBigInteger'new-i 1)]
            (loop-when [a a b b c c d d] (not (MutableBigInteger'isOne b)) => (let [[m _] (MutableBigInteger''subtract m, c)] m)
                (let-when [[#_"MutableBigInteger" q a] (MutableBigInteger'divide a, b)] (pos? (:intLen a)) => (throw! "not invertible")
                    (let [q (if (== (:intLen q) 1)
                                (MutableBigInteger'mul c, (aget (:value q) (:offset q)))
                                (MutableBigInteger'multiply q, c)
                            )
                          d (MutableBigInteger''add d, q)]
                        (when-not (MutableBigInteger'isOne a) => d
                            (let-when [[#_"MutableBigInteger" q b] (MutableBigInteger'divide b, a)] (pos? (:intLen b)) => (throw! "not invertible")
                                (let [q (if (== (:intLen q) 1)
                                            (MutableBigInteger'mul d, (aget (:value q) (:offset q)))
                                            (MutableBigInteger'multiply q, d)
                                        )]
                                    (recur a b (MutableBigInteger''add c, q) d)
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    ;;
     ; Calculate the multiplicative inverse of x mod 2^k.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'modInverseMP2 [#_"MutableBigInteger" x, #_"int" k]
        (cond
            (MutableBigInteger'isEven x) (throw! "not invertible (not= gcd 1)")
            (< 64 k)                     (MutableBigInteger'euclidModInverse x, k)
            :else
            (let [#_"int[]" a (:value x) #_"int" n (:intLen x) #_"int" e (dec (+ (:offset x) n)) #_"int" p (aget a e)
                  #_"int" t (MutableBigInteger'inverseMod32 p)]
                (when (< 32 k) => (MutableBigInteger'new-i (if (== k 32) t (& t (dec (<< 1 k)))))
                    (let [#_"long" p' (long! p) p' (if (< 1 n) (| p' (<< (long (aget a (dec e))) 32)) p')
                          #_"long" t' (long! t) t' (* t' (- 2 (* p' t'))) ;; 1 more Newton iteration step
                          t' (if (== k 64) t' (& t' (dec (<< 1 k))))]
                        (MutableBigInteger''normalize (MutableBigInteger'new-a (int-array [(int (>>> t' 32)) (int t')])))
                    )
                )
            )
        )
    )

    ;;;
     ; Calculate the multiplicative inverse of 2^k mod m, where m is odd.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'modInverseBP2 [#_"MutableBigInteger" m, #_"int" k]
        (MutableBigInteger'fixup (MutableBigInteger'new-i 1), m, k)
    )

    ;;;
     ; Returns the modInverse of x mod p.
     ; x and p are not affected by the operation.
     ;;
    (defn #_"MutableBigInteger" MutableBigInteger'mutableModInverse [#_"MutableBigInteger" x, #_"MutableBigInteger" p]
        ;; when modulus is odd, use Schroeppel's algorithm
        (when (MutableBigInteger'isEven p) => (MutableBigInteger''modInverse x, p)
            ;; when base and modulus are even, throw exception
            (when-not (MutableBigInteger'isEven x) => (throw! "not invertible")
                ;; get even part of modulus expressed as a power of 2
                (let [#_"int" shift (MutableBigInteger'getLowestSetBit p)
                      ;; construct odd part of modulus
                      #_"MutableBigInteger" o (MutableBigInteger''rightShift (MutableBigInteger'copy p), shift)]
                    (if (MutableBigInteger'isOne o)
                        (MutableBigInteger'modInverseMP2 x, shift)
                        ;; calculate 1/a mod oddMod ;; calculate 1/a mod evenMod
                        (let [#_"MutableBigInteger" odd (MutableBigInteger''leftShift (MutableBigInteger''modInverse x, o), shift)
                              #_"MutableBigInteger" even (MutableBigInteger'modInverseMP2 x, shift)
                              ;; combine the results using Chinese Remainder Theorem
                              [_ #_"MutableBigInteger" r]
                                (MutableBigInteger'divide
                                    (MutableBigInteger''add
                                        (MutableBigInteger'multiply odd, (MutableBigInteger'modInverseBP2 o, shift)),
                                        (MutableBigInteger'multiply (MutableBigInteger'multiply even, o), (MutableBigInteger'modInverseMP2 o, shift))
                                    ),
                                    p
                                )]
                            r
                        )
                    )
                )
            )
        )
    )
)

;;;
 ; Immutable arbitrary-precision integers. All operations behave as if BigIntegers were represented
 ; in two's-complement notation (like Java's primitive integer types). BigInteger provides analogues
 ; to all of Java's primitive integer operators, and all relevant methods from java.lang.Math.
 ; Additionally, BigInteger provides operations for modular arithmetic, GCD calculation, primality
 ; testing, prime generation, bit manipulation, and a few other miscellaneous operations.
 ;
 ; Semantics of arithmetic operations exactly mimic those of Java's integer arithmetic operators, as
 ; defined in "The Java Language Specification". For example, division by zero throws an ArithmeticException,
 ; and division of a negative by a positive yields a negative (or zero) remainder. All of the details
 ; in the Spec concerning overflow are ignored, as BigIntegers are made as large as necessary to
 ; accommodate the results of an operation.
 ;
 ; Semantics of shift operations extend those of Java's shift operators to allow for negative shift
 ; distances. A right-shift with a negative shift distance results in a left shift, and vice-versa.
 ; The unsigned right shift operator (>>>) is omitted, as this operation makes little sense in combination
 ; with the "infinite word size" abstraction provided by this class.
 ;
 ; Semantics of bitwise logical operations exactly mimic those of Java's bitwise integer operators.
 ; The binary operators (and, or, xor) implicitly perform sign extension on the shorter of
 ; the two operands prior to performing the operation.
 ;
 ; Comparison operations perform signed integer comparisons, analogous to those
 ; performed by Java's relational and equality operators.
 ;
 ; Modular arithmetic operations are provided to compute residues, perform exponentiation, and compute
 ; multiplicative inverses. These methods always return a non-negative result, between 0 and
 ; (modulus - 1), inclusive.
 ;
 ; Bit operations operate on a single bit of the two's-complement representation of their operand.
 ; If necessary, the operand is sign- extended so that it contains the designated bit. None of the
 ; single-bit operations can produce a BigInteger with a different sign from the BigInteger being
 ; operated on, as they affect only a single bit, and the "infinite word size" abstraction provided by
 ; this class ensures that there are infinitely many "virtual sign bits" preceding each BigInteger.
 ;
 ; All methods and constructors in this class throw NullPointerException when passed a null
 ; object reference for any input parameter.
 ;
 ; BigInteger must support values in the range
 ; -2<sup>Integer.MAX_VALUE</sup> (exclusive) to +2<sup>Integer.MAX_VALUE</sup> (exclusive)
 ; and may support values outside of that range.
 ;
 ; The range of probable prime values is limited and may be less than the full supported positive
 ; range of BigInteger. The range must be at least 1 to 2<sup>500000000</sup>.
 ;
 ; BigInteger constructors and operations throw ArithmeticException when the result is
 ; out of the supported range of
 ; -2<sup>Integer.MAX_VALUE</sup> (exclusive) to +2<sup>Integer.MAX_VALUE</sup> (exclusive).
 ;;

(class-ns BigInteger (§ extends #_"Number") (§ implements #_"Comparable<BigInteger>")
    ;;;
     ; The signum of this BigInteger: -1 for negative, 0 for zero, or 1 for positive.
     ; Note that the BigInteger zero *must* have a signum of 0. This is necessary
     ; to ensure that there is exactly one representation for each BigInteger value.
     ;;
    #_final
    (§ field #_"int" :signum 0)

    ;;;
     ; The magnitude of this BigInteger, in *big-endian* order: the zeroth element
     ; of this array is the most-significant int of the magnitude. The magnitude must be
     ; "minimal" in that the most-significant int (mag[0]) must be non-zero. This is
     ; necessary to ensure that there is exactly one representation for each BigInteger
     ; value. Note that this implies that the BigInteger zero has a zero-length mag array.
     ;;
    #_final
    (§ field #_"int[]" :mag nil)

    ;;;
     ; This constant limits {@code mag.length} of BigIntegers to the supported range.
     ;;
    (def- #_"int" BigInteger'MAX_MAG_LENGTH 64)

    (defn- #_"void" BigInteger'checkRange [#_"int[]" a]
        (when (or (< BigInteger'MAX_MAG_LENGTH (alength a)) (and (== (alength a) BigInteger'MAX_MAG_LENGTH) (neg? (aget a 0))))
            (throw! "magnitude overflow")
        )
        nil
    )

    ;;;
     ; Bit lengths larger than this constant can cause overflow in searchLen
     ; calculation and in BitSieve.singleSearch method.
     ;;
    (def- #_"int" BigInteger'PRIME_SEARCH_BIT_LENGTH_LIMIT 500000000)

    (defn- #_"int" BigInteger'getPrimeSearchLen [#_"int" bitLength]
        (when (< (inc BigInteger'PRIME_SEARCH_BIT_LENGTH_LIMIT) bitLength)
            (throw! "prime search implementation restriction on bitLength")
        )
        (* (quot bitLength 20) 64)
    )

    ;;
     ; The following two arrays are used for fast String conversions. Both are indexed by radix.
     ; The first is the number of digits of the given radix that can fit in a Java long without
     ; "going negative", i.e., the highest integer n such that radix^n < 2^63. The second is the
     ; "long radix" that tears each number into "long digits", each of which consists of the number
     ; of digits in the corresponding element in digitsPerLong (longRadix[i] = i^digitPerLong[i]).
     ; Both arrays have nonsense values in their 0 and 1 elements, as radixes 0 and 1 are not used.
     ;;
    (def- #_"int[]" BigInteger'digitsPerLong
        (int-array [
            0, 0,
            62, 39, 31, 27, 24, 22, 20, 19, 18, 18, 17, 17, 16, 16, 15, 15, 15, 14, 14, 14, 14, 13, 13, 13, 13, 13, 13, 12, 12, 12, 12, 12, 12, 12, 12
        ])
    )

    (declare BigInteger'valueOf-l)

    (def- #_"BigInteger[]" BigInteger'longRadix
        (into-array #_"BigInteger"
            (§ soon map BigInteger'valueOf-l [
                nil, nil,
                0x4000000000000000, 0x383d9170b85ff80b, 0x4000000000000000, 0x6765c793fa10079d, 0x41c21cb8e1000000,
                0x3642798750226111, 0x1000000000000000, 0x12bf307ae81ffd59, 0x0de0b6b3a7640000, 0x4d28cb56c33fa539,
                0x1eca170c00000000, 0x780c7372621bd74d, 0x1e39a5057d810000, 0x5b27ac993df97701, 0x1000000000000000,
                0x27b95e997e21d9f1, 0x5da0e1e53c5c8000, 0x0b16a458ef403f19, 0x16bcc41e90000000, 0x2d04b7fdd9c0ef49,
                0x5658597bcaa24000, 0x06feb266931a75b7, 0x0c29e98000000000, 0x14adf4b7320334b9, 0x226ed36478bfa000,
                0x383d9170b85ff80b, 0x5a3c23e39c000000, 0x04e900abb53e6b71, 0x07600ec618141000, 0x0aee5720ee830681,
                0x1000000000000000, 0x172588ad4f5f0981, 0x211e44f7d02c1000, 0x2ee56725f06e5c71, 0x41c21cb8e1000000
            ])
        )
    )

    ;;
     ; These two arrays are the integer analogue of above.
     ;;
    (def- #_"int[]" BigInteger'digitsPerInt
        (int-array [
            0, 0,
            30, 19, 15, 13, 11, 11, 10, 9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5
        ])
    )

    (def- #_"int[]" BigInteger'intRadix
        (int-array [
            0, 0,
            0x40000000, 0x4546b3db, 0x40000000, 0x48c27395, 0x159fd800,
            0x75db9c97, 0x40000000, 0x17179149, 0x3b9aca00, 0x0cc6db61,
            0x19a10000, 0x309f1021, 0x57f6c100, 0x0a2f1b6f, 0x10000000,
            0x18754571, 0x247dbc80, 0x3547667b, 0x4c4b4000, 0x6b5a6e1d,
            0x06c20a40, 0x08d2d931, 0x0b640000, 0x0e8d4a51, 0x1269ae40,
            0x17179149, 0x1cb91000, 0x23744899, 0x2b73a840, 0x34e63b41,
            0x40000000, 0x4cfa3cc1, 0x5c13d840, 0x6d91b519, 0x039aa400
        ])
    )

    ;; bitsPerDigit in the given radix times 1024
    ;; Rounded up to avoid underallocation.
    (def- #_"long[]" BigInteger'bitsPerDigit
        (long-array [
            0, 0,
            1024, 1624, 2048, 2378, 2648, 2875, 3072,
            3247, 3402, 3543, 3672, 3790, 3899, 4001,
            4096, 4186, 4271, 4350, 4426, 4498, 4567,
            4633, 4696, 4756, 4814, 4870, 4923, 4975,
            5025, 5074, 5120, 5166, 5210, 5253, 5295
        ])
    )

    ;; miscellaneous bit operations

    ;;;
     ; Package private method to return bit length for an integer.
     ;;
    (defn #_"int" BigInteger'bitLengthForInt [#_"int" n]
        (- 32 (Integer/numberOfLeadingZeros n))
    )

    ;;;
     ; Calculate bitlength of contents of the first n elements of an int array,
     ; assuming there are no leading zero ints.
     ;;
    (defn #_"int" BigInteger'bitLength [#_"int[]" a, #_"int" n]
        (if (zero? n) 0 (+ (<< (dec n) 5) (BigInteger'bitLengthForInt (aget a 0))))
    )

    ;;;
     ; Returns the number of bits in the minimal two's-complement representation
     ; of this BigInteger, *excluding* a sign bit. For positive BigIntegers, this
     ; is equivalent to the number of bits in the ordinary binary representation.
     ;
     ; Computes (ceil (log2 (if (neg? this) (- this) (inc this)))).
     ;;
    #_method
    (defn #_"int" BigInteger''bitLength [#_"BigInteger" this]
        (let-when [#_"int[]" a (:mag this) #_"int" n (alength a)] (pos? n) => 0
            ;; calculate the bit length of the magnitude
            (let-when [#_"int" m (+ (<< (dec n) 5) (BigInteger'bitLengthForInt (aget a 0)))] (neg? (:signum this)) => m
                ;; check if magnitude is a power of two
                (loop-when-recur [? (== (Integer/bitCount (aget a 0)) 1) #_"int" i 1]
                                 (and ? (< i n))
                                 [(zero? (aget a i)) (inc i)]
                              => (if ? (dec m) m)
                )
            )
        )
    )

    ;;;
     ; Returns the number of bits in the two's-complement representation
     ; of this BigInteger that differ from its sign bit. This method is
     ; useful when implementing bit-vector style sets atop BigIntegers.
     ;;
    #_method
    (defn #_"int" BigInteger''bitCount [#_"BigInteger" this]
        (let-when [#_"int[]" a (:mag this) #_"int" n (alength a)] (pos? n) => 0
            ;; count the bits in the magnitude
            (let-when [#_"int" c (loop-when-recur [c 0 #_"int" i 0] (< i n) [(+ c (Integer/bitCount (aget a i))) (inc i)] => c)] (neg? (:signum this)) => c
                ;; count the trailing zeros in the magnitude
                (loop-when-recur [c c #_"int" i (dec n)] (zero? (aget a i)) [(+ c 32) (dec i)] => (dec (+ c (Integer/numberOfTrailingZeros (aget a i)))))
            )
        )
    )

    ;; functions providing access to the two's-complement representation of BigIntegers

    ;;;
     ; Returns the length of the two's-complement representation in ints,
     ; including space for at least one sign bit.
     ;;
    #_method
    (defn- #_"int" BigInteger''intLength [#_"BigInteger" this]
        (inc (>>> (BigInteger''bitLength this) 5))
    )

    ;;; Returns sign bit.
    #_method
    (defn- #_"int" BigInteger''signBit [#_"BigInteger" this]
        (if (neg? (:signum this)) 1 0)
    )

    ;;; Returns an int of sign bits.
    #_method
    (defn- #_"int" BigInteger''signInt [#_"BigInteger" this]
        (if (neg? (:signum this)) -1 0)
    )

    ;;;
     ; Returns the index of the first nonzero int in the little-endian binary representation of the magnitude
     ; (int 0 is the least significant). If the magnitude is zero, return value is undefined.
     ;;
    #_method
    (defn- #_"int" BigInteger''firstNonzeroIntNum [#_"BigInteger" this]
        (let [#_"int[]" a (:mag this) #_"int" n (alength a)]
            ;; search for the first nonzero int
            (loop-when-recur [#_"int" i (dec n)] (and (<= 0 i) (zero? (aget a i))) [(dec i)] => (- (dec n) i))
        )
    )

    ;;;
     ; Returns the i'th int of the little-endian two's-complement representation (int 0 is the least significant).
     ; The int number can be arbitrarily high (values are logically preceded by infinitely many sign ints).
     ;;
    #_method
    (defn- #_"int" BigInteger''getInt [#_"BigInteger" this, #_"int" i]
        (let [#_"int[]" a (:mag this) #_"int" n (alength a)]
            (cond
                (neg? i) 0
                (<= n i) (BigInteger''signInt this)
                :else
                (let [#_"int" m (aget a (- (dec n) i))]
                    (cond (<= 0 (:signum this)) m (<= i (BigInteger''firstNonzeroIntNum this)) (- m) :else (bit-not m))
                )
            )
        )
    )

    ;;;
     ; Returns the index of the rightmost (lowest-order) one bit in this BigInteger (the number of zero bits
     ; to the right of the rightmost one bit). Returns -1 if this BigInteger contains no one bits.
     ;
     ; Computes (if (zero? this) -1 (log2 (& this (- this)))).
     ;;
    #_method
    (defn #_"int" BigInteger''getLowestSetBit [#_"BigInteger" this]
        (when-not (zero? (:signum this)) => -1
            ;; search for lowest order nonzero int
            (loop [#_"int" i 0]
                (let-when [#_"int" b (BigInteger''getInt this, i)] (zero? b) => (+ (<< i 5) (Integer/numberOfTrailingZeros b))
                    (recur (inc i))
                )
            )
        )
    )

    ;;;
     ; Returns a copy of the input array stripped of any leading zero bytes.
     ;;
    (defn- #_"int[]" BigInteger'stripLeadingZeroInts [#_"int[]" a]
        (let [#_"int" n (alength a)]
            ;; find first nonzero byte
            (loop-when-recur [#_"int" i 0] (and (< i n) (zero? (aget a i))) [(inc i)] => (Arrays/copyOfRange a, i, n))
        )
    )

    ;;;
     ; Returns the input array stripped of any leading zero bytes.
     ; Since the source is trusted the copying may be skipped.
     ;;
    (defn- #_"int[]" BigInteger'trustedStripLeadingZeroInts [#_"int[]" a]
        (let [#_"int" n (alength a)]
            ;; find first nonzero byte
            (loop-when-recur [#_"int" i 0] (and (< i n) (zero? (aget a i))) [(inc i)] => (if (zero? i) a (Arrays/copyOfRange a, i, n)))
        )
    )

    ;;;
     ; Returns a copy of the input array stripped of any leading zero bytes.
     ;;
    (defn- #_"int[]" BigInteger'stripLeadingZeroBytes [#_"byte[]" b]
        (let [#_"int" m (alength b)
              ;; find first nonzero byte
              #_"int" k (loop-when-recur [k 0] (and (< k m) (zero? (aget b k))) [(inc k)] => k)
              ;; allocate new array and copy relevant part of input array
              #_"int" n (>>> (+ (- m k) 3) 2) #_"int[]" a (int-array n)]
            (loop-when [m (dec m) n (dec n)] (<= 0 n)
                (aset a n (& (aget b m) 0xff))
                (let [#_"int" t (* (min (- m k) 3) 8)
                      m (loop-when-recur [m (dec m) #_"int" i 8] (<= i t) [(dec m) (+ i 8)] => m
                            (aswap a n | (<< (& (aget b m) 0xff) i))
                        )]
                    (recur m (dec n))
                )
            )
            a
        )
    )

    ;;;
     ; Takes an array b representing a negative two's-complement number and
     ; returns the minimal (no leading zero bytes) unsigned whose value is -b.
     ;;
    (defn- #_"int[]" BigInteger'makePositive-b [#_"byte[]" b]
        (let [#_"int" m (alength b)
              ;; find first non-sign (0xff) byte of input
              #_"int" k (loop-when-recur [k 0] (and (< k m) (== (aget b k) -1)) [(inc k)] => k)
              ;; if all non-sign bytes are zero, we must allocate space for one extra output byte
              #_"int" e (loop-when-recur [e k] (and (< e m) (zero? (aget b e))) [(inc e)] => e)
              #_"int" n (>>> (+ (- m k) 3 (if (== e m) 1 0)) 2) #_"int[]" a (int-array n)]
            ;; copy one's complement of input into output leaving extra byte (if it exists) zero
            (loop-when [m (dec m) #_"int" i (dec n)] (<= 0 i)
                (aset a i (& (aget b m) 0xff))
                (let [#_"int" t (* (max 0 (min (- m k) 3)) 8)
                      m (loop-when-recur [m (dec m) #_"int" j 8] (<= j t) [(dec m) (+ j 8)] => m
                            (aswap a i | (<< (& (aget b m) 0xff) j))
                        )]
                    ;; mask indicates which bits must be complemented
                    (aswap a i #(& (bit-not %) (>>> -1 (* 8 (- 3 t)))))
                    (recur m (dec i))
                )
            )
            ;; add one to one's complement to generate two's-complement
            (loop-when [#_"int" i (dec n)] (<= 0 i)
                (aswap a i #(int (inc (long! %))))
                (recur-if (zero? (aget a i)) [(dec i)])
            )
            a
        )
    )

    ;;;
     ; Takes an array a representing a negative two's-complement number and
     ; returns the minimal (no leading zero ints) unsigned whose value is -a.
     ;;
    (defn- #_"int[]" BigInteger'makePositive-i [#_"int[]" a]
        (let [#_"int" n (alength a)
              ;; find first non-sign (0xffffffff) int of input
              #_"int" k (loop-when-recur [k 0] (and (< k n) (== (aget a k) -1)) [(inc k)] => k)
              ;; if all non-sign ints are zero, we must allocate space for one extra output int
              #_"int" e (loop-when-recur [e k] (and (< e n) (zero? (aget a e))) [(inc e)] => e)
              #_"int" n' (+ (- n k) (if (== e n) 1 0)) #_"int[]" a' (int-array n')]
            ;; copy one's complement of input into output leaving extra int (if it exists) zero
            (loop-when-recur [#_"int" i k] (< i n) [(inc i)]
                (aset a' (+ (- i k) (if (== e n) 1 0)) (bit-not (aget a i)))
            )
            ;; add one to one's complement to generate two's-complement
            (loop [#_"int" i (dec n')]
                (aswap a' i inc)
                (recur-if (zero? (aget a' i)) [(dec i)])
            )
            a'
        )
    )

    ;; constructors

    ;;;
     ; Translates a byte array containing the two's-complement binary representation
     ; of a BigInteger into a BigInteger. The input array is assumed to be in
     ; *big-endian* byte-order: the most-significant byte is in the zeroth element.
     ;
     ; @throws NumberFormatException when b is zero bytes long.
     ;;
    (defn #_"BigInteger" BigInteger'new-b [#_"byte[]" b]
        (when (pos? (alength b)) => (throw! "zero length")
            (let [this (§ new)
                  this
                    (if (neg? (aget b 0))
                        (let [#_"int[]" a (BigInteger'makePositive-b b)]
                            (assoc this :mag a :signum -1)
                        )
                        (let [#_"int[]" a (BigInteger'stripLeadingZeroBytes b)]
                            (assoc this :mag a :signum (if (zero? (alength a)) 0 1))
                        )
                    )]
                (BigInteger'checkRange (:mag this))
                this
            )
        )
    )

    ;;;
     ; This private constructor translates an int array containing the two's-complement
     ; binary representation of a BigInteger into a BigInteger. The input array is assumed
     ; to be in *big-endian* int-order: the most-significant int is in the zeroth element.
     ;;
    (defn- #_"BigInteger" BigInteger'new-a [#_"int[]" a]
        (when (pos? (alength a)) => (throw! "zero length")
            (let [this (§ new)
                  this
                    (if (neg? (aget a 0))
                        (let [a (BigInteger'makePositive-i a)]
                            (assoc this :mag a :signum -1)
                        )
                        (let [a (BigInteger'trustedStripLeadingZeroInts a)]
                            (assoc this :mag a :signum (if (zero? (alength a)) 0 1))
                        )
                    )]
                (BigInteger'checkRange (:mag this))
                this
            )
        )
    )

    ;;;
     ; Translates the sign-magnitude representation of a BigInteger into a BigInteger.
     ; The sign is represented as an integer signum value: -1 for negative, 0 for zero,
     ; or 1 for positive. The magnitude is a byte array in *big-endian* byte-order: the
     ; most-significant byte is in the zeroth element. A zero-length magnitude array is
     ; permissible, and will result in a BigInteger value of 0, whether signum is -1, 0 or 1.
     ;
     ; @throws NumberFormatException when signum is not one of the three legal values
     ;         (-1, 0, and 1), or signum is 0 and magnitude contains one or more non-zero bytes.
     ;;
    (defn #_"BigInteger" BigInteger'new-2ib [#_"int" signum, #_"byte[]" magnitude]
        (when (<= -1 signum 1) => (throw! "invalid signum value")
            (let [this (§ new)
                  this (assoc this :mag (BigInteger'stripLeadingZeroBytes magnitude))
                  this
                    (cond
                        (zero? (alength (:mag this))) (assoc this :signum 0)
                        (zero? signum)                (throw! "signum-magnitude mismatch")
                        :else                         (assoc this :signum signum)
                    )]
                (BigInteger'checkRange (:mag this))
                this
            )
        )
    )

    ;;;
     ; A constructor for internal use that translates the sign-magnitude representation
     ; of a BigInteger into a BigInteger. It checks the arguments and copies the magnitude,
     ; so this constructor would be safe for external use.
     ;;
    (defn- #_"BigInteger" BigInteger'new-2ia [#_"int" signum, #_"int[]" magnitude]
        (when (<= -1 signum 1) => (throw! "invalid signum value")
            (let [this (§ new)
                  this (assoc this :mag (BigInteger'stripLeadingZeroInts magnitude))
                  this
                    (cond
                        (zero? (alength (:mag this))) (assoc this :signum 0)
                        (zero? signum)                (throw! "signum-magnitude mismatch")
                        :else                         (assoc this :signum signum)
                    )]
                (BigInteger'checkRange (:mag this))
                this
            )
        )
    )

    ;; multiply x array times word y in place, and add word z
    (defn- #_"void" BigInteger'destructiveMulAdd [#_"int[]" x, #_"int" y, #_"int" z]
        (let [#_"int" n (alength x) #_"long" y' (long! y) #_"long" z' (long! z)]
            ;; perform the multiplication word by word
            (loop-when [#_"long" carry 0 #_"int" i (dec n)] (<= 0 i)
                (let [#_"long" mul (+ (* (long! (aget x i)) y') carry)]
                    (aset x i (int mul))
                    (recur (>>> mul 32) (dec i))
                )
            )
            ;; perform the addition
            (let [#_"long" sum (+ (long! (aget x (dec n))) z')]
                (aset x (dec n) (int sum))
                (loop-when [carry (>>> sum 32) #_"int" i (- n 2)] (<= 0 i)
                    (let [sum (+ (long! (aget x i)) carry)]
                        (aset x i (int sum))
                        (recur (>>> sum 32) (dec i))
                    )
                )
            )
        )
        nil
    )

    ;;;
     ; Translates the String representation of a BigInteger in the specified radix into a BigInteger.
     ; The String representation consists of an optional minus or plus sign followed by a sequence
     ; of one or more digits in the specified radix. The character-to-digit mapping is provided by
     ; Character/digit. The String may not contain any extraneous characters (whitespace, for example).
     ;
     ; @throws NumberFormatException when s is not a valid representation of
     ;         a BigInteger in the given radix, or the radix is outside the range
     ;         from Character/MIN_RADIX to Character/MAX_RADIX, inclusive.
     ;;
    (defn #_"BigInteger" BigInteger'new-s
        ([#_"String" s] (BigInteger'new-s s, 10))
        ([#_"String" s, #_"int" radix]
            (when (<= Character/MIN_RADIX radix Character/MAX_RADIX) => (throw! "radix out of range")
                (let-when [#_"int" n (.length s)] (pos? n) => (throw! "zero length")
                    ;; check for at most one leading sign
                    (let [#_"int" minus (.lastIndexOf s (int \-)) #_"int" plus (.lastIndexOf s (int \+))
                          [#_"int" sign #_"int" i]
                            (cond
                                (<= 0 minus)
                                    (when (and (zero? minus) (neg? plus)) => (throw! "illegal embedded sign character")
                                        [-1 1]
                                    )
                                (<= 0 plus)
                                    (when (zero? plus) => (throw! "illegal embedded sign character")
                                        [1 1]
                                    )
                                :else
                                    [1 0]
                            )]
                        (when (< i n) => (throw! "zero length")
                            ;; skip leading zeros and compute number of digits in magnitude
                            (let [i (loop-when-recur i (and (< i n) (zero? (Character/digit (.charAt s, i), radix))) (inc i) => i)]
                                (when (< i n) => BigInteger'ZERO
                                    ;; Allocate an array of the expected size. May be too large, but should never be too small. Typically exact.
                                    (let [#_"int" nDigits (- n i) #_"long" nBits (inc (>>> (* nDigits (aget BigInteger'bitsPerDigit radix)) 10))]
                                        (when-not (<= (<< 1 32) (+ nBits 31)) => (throw! "magnitude overflow")
                                            (let [#_"int" nWords (>>> (int (+ nBits 31)) 5) #_"int[]" a (int-array nWords)
                                                  ;; process first (potentially short) digit group
                                                  #_"int" dpi (aget BigInteger'digitsPerInt radix)
                                                  #_"int" nGroup (% nDigits dpi) nGroup (if (zero? nGroup) dpi nGroup)
                                                  #_"String" group (.substring s, i, (+ i nGroup)) i (+ i nGroup)]
                                                (when-not (neg? (aset a (dec nWords) (Integer/parseInt group, radix))) => (throw! "illegal digit")
                                                    ;; process remaining digit groups
                                                    (let [#_"int" radix' (aget BigInteger'intRadix radix)]
                                                        (while (< i n)
                                                            (let [#_"String" group (.substring s, i, (+ i dpi)) i (+ i dpi)
                                                                  #_"int" digit (Integer/parseInt group, radix)]
                                                                (when-not (neg? digit) => (throw! "illegal digit")
                                                                    (BigInteger'destructiveMulAdd a, radix', digit)
                                                                )
                                                            )
                                                        )
                                                        ;; required for cases where the array was overallocated
                                                        (let [this (assoc (§ new) :mag (BigInteger'trustedStripLeadingZeroInts a) :signum sign)]
                                                            (BigInteger'checkRange (:mag this))
                                                            this
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

    (defn- #_"byte[]" BigInteger'randomBits [#_"int" nBits, #_"Random" rnd]
        (when-not (neg? nBits) => (throw! "nBits must be non-negative")
            (let [#_"int" n (int (quot (+ (long nBits) 7) 8)) ;; avoid overflow
                  #_"byte[]" b (byte-array n)]
                (when (pos? n) => b
                    ;; generate random bytes and mask out any excess bits
                    (.nextBytes rnd, b)
                    (let [#_"int" e (- (* 8 n) nBits)]
                        (aswap b 0 & (dec (<< 1 (- 8 e))))
                        b
                    )
                )
            )
        )
    )

    ;;;
     ; Constructs a randomly generated BigInteger, uniformly distributed over
     ; the range 0 to 2^nBits - 1, inclusive.
     ; The uniformity of the distribution assumes that a fair source of random
     ; bits is provided in rnd. Note that this constructor always constructs
     ; a non-negative BigInteger.
     ;
     ; @param  nBits maximum bitLength of the new BigInteger.
     ; @param  rnd source of randomness to be used in computing the new BigInteger.
     ; @throws IllegalArgumentException when nBits is negative.
     ;;
    (defn #_"BigInteger" BigInteger'new-2ir [#_"int" nBits, #_"Random" rnd]
        (BigInteger'new-2ib 1, (BigInteger'randomBits nBits, rnd))
    )

    ;;;
     ; This internal constructor differs from its public cousin
     ; with the arguments reversed in two ways: it assumes that its
     ; arguments are correct, and it doesn't copy the magnitude array.
     ;;
    (defn #_"BigInteger" BigInteger'new-2ai [#_"int[]" a, #_"int" signum]
        (let [this (assoc (§ new) :mag a :signum (if (zero? (alength a)) 0 signum))]
            (BigInteger'checkRange (:mag this))
            this
        )
    )

    ;;;
     ; This private constructor is for internal use and assumes that
     ; its arguments are correct.
     ;;
    (defn- #_"BigInteger" BigInteger'new-2bi [#_"byte[]" b, #_"int" signum]
        (let [this (assoc (§ new) :mag (BigInteger'stripLeadingZeroBytes b) :signum (if (zero? (alength b)) 0 signum))]
            (BigInteger'checkRange (:mag this))
            this
        )
    )

    ;; static factory methods

    ;;;
     ; The BigInteger constant zero.
     ;;
    (def #_"BigInteger" BigInteger'ZERO (§ soon BigInteger'new-2ai (int-array 0), 0))

    (def- #_"int" BigInteger'MAX_CONSTANT 16)

    (def- #_"BigInteger[]" BigInteger'posConst (§ soon make-array BigInteger (inc BigInteger'MAX_CONSTANT)))
    (def- #_"BigInteger[]" BigInteger'negConst (§ soon make-array BigInteger (inc BigInteger'MAX_CONSTANT)))

    (§ soon dotimes [#_"int" i BigInteger'MAX_CONSTANT]
        (let [i (inc i) #_"int[]" a (int-array [i])]
            (aset BigInteger'posConst i (BigInteger'new-2ai a,  1))
            (aset BigInteger'negConst i (BigInteger'new-2ai a, -1))
        )
    )

    ;;;
     ; Constructs a BigInteger with the specified value, which may not be zero.
     ;;
    (defn- #_"BigInteger" BigInteger'new-l [#_"long" n]
        (let [[n #_"int" signum] (if (neg? n) [(- n) -1] [n 1]) #_"int" m (int (>>> n 32))]
            (assoc (§ new) :mag (int-array (if (zero? m) [(int n)] [m (int n)])) :signum signum)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is equal to that of the specified long.
     ; This "static factory method" is provided in preference to a (long) constructor,
     ; because it allows for reuse of frequently used BigIntegers.
     ;;
    (defn #_"BigInteger" BigInteger'valueOf-l [#_"long" n]
        (cond
            (zero? n)                              BigInteger'ZERO
            (<= 1 n BigInteger'MAX_CONSTANT)      (BigInteger'posConst (int n))
            (<= (- BigInteger'MAX_CONSTANT) n -1) (BigInteger'negConst (int (- n)))
            :else                                 (BigInteger'new-l n)
        )
    )

    ;;;
     ; Returns a BigInteger with the given two's-complement representation.
     ; Assumes that the input array will not be modified
     ; (the returned BigInteger will reference the input array, if feasible).
     ;;
    (defn- #_"BigInteger" BigInteger'valueOf-a [#_"int[]" a]
        (if (pos? (aget a 0)) (BigInteger'new-2ai a, 1) (BigInteger'new-a a))
    )

    ;; constants

    ;;;
     ; The BigInteger constant one.
     ;;
    (def #_"BigInteger" BigInteger'ONE (§ soon BigInteger'valueOf-l 1))

    ;;;
     ; The BigInteger constant two. (Not exported.)
     ;;
    (def- #_"BigInteger" BigInteger'TWO (§ soon BigInteger'valueOf-l 2))

    ;;;
     ; The BigInteger constant -1. (Not exported.)
     ;;
    (def- #_"BigInteger" BigInteger'MINUS_ONE (§ soon BigInteger'valueOf-l -1))

    ;; comparison operations

    ;;;
     ; Compare the contents of int arrays.
     ;;
    (defn- #_"int" BigInteger'compare-aa [#_"int[]" a*, #_"int[]" b*]
        (let [#_"int" n (alength a*) #_"int" m (alength b*)]
            (cond (< n m) -1 (< m n) 1 :else
                (loop-when [#_"int" i 0] (< i n) => 0
                    (let [#_"int" a (aget a* i) #_"int" b (aget b* i)]
                        (recur-if (== a b) [(inc i)] => (if (< (long! a) (long! b)) -1 1))
                    )
                )
            )
        )
    )

    ;;;
     ; Compare the contents of int array with long value.
     ; xy can't be Long/MIN_VALUE.
     ;;
    (defn- #_"int" BigInteger'compare-al [#_"int[]" a*, #_"long" xy]
        (when-not (== xy Long/MIN_VALUE) => (throw! "cannot compare with Long/MIN_VALUE")
            (let-when [#_"int" n (alength a*)] (<= n 2) => 1
                (let [xy (abs xy) #_"int" x (int (>>> xy 32))]
                    (if (zero? x)
                        (cond (< n 1) -1 (< 1 n) 1 :else
                            (let-when [#_"int" a (aget a* 0) #_"int" y (int xy)] (== a y) => (if (< (long! a) (long! y)) -1 1)
                                0
                            )
                        )
                        (when (== n 2) => -1
                            (let-when [#_"int" a (aget a* 0)                   ] (== a x) => (if (< (long! a) (long! x)) -1 1)
                                (let-when [    a (aget a* 1) #_"int" y (int xy)] (== a y) => (if (< (long! a) (long! y)) -1 1)
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Compares this BigInteger with 'that'.
     ; This method is provided in preference to individual methods for
     ; each of the six boolean comparison operators (<, <=, >, >=, ==, !=).
     ; The suggested idiom for performing these comparisons is:
     ; (op (.compareTo x, y) 0), where op is one of the six ops above.
     ;
     ; @return -1, 0 or 1 as this BigInteger is numerically less than,
     ;         equal to, or greater than 'that'.
     ;;
    #_foreign
    (defn #_"int" compareTo---BigInteger [#_"BigInteger" this, #_"BigInteger" that]
        (let [#_"int" x (:signum this) #_"int" y (:signum that)]
            (when (== x y) => (if (< x y) -1 1)
                (let [#_"int[]" a (:mag this) #_"int[]" b (:mag that)]
                    (case x -1 (BigInteger'compare-aa b, a) 1 (BigInteger'compare-aa a, b) 0)
                )
            )
        )
    )

    ;;;
     ; Compares this BigInteger with 'that' for equality.
     ;
     ; @return true if and only if 'that' is a BigInteger whose
     ;         value is numerically equal to this BigInteger.
     ;;
    #_foreign
    (defn #_"boolean" equals---BigInteger [#_"BigInteger" this, #_"Object" that]
        (or (identical? this that)
            (and (§ soon instance? BigInteger that) (== (:signum this) (:signum that))
                (let [#_"int[]" a (:mag this) #_"int[]" b (:mag that) #_"int" n (alength a)]
                    (and (== n (alength b))
                        (loop-when [#_"int" i 0] (< i n) => true
                            (and (== (aget a i) (aget b i))
                                (recur (inc i))
                            )
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Returns the minimum of a and b.
     ; If they are equal, either may be returned.
     ;;
    (defn #_"BigInteger" BigInteger'min [#_"BigInteger" a, #_"BigInteger" b]
        (if (neg? (.compareTo a, b)) a b)
    )

    ;;;
     ; Returns the maximum of a and b.
     ; If they are equal, either may be returned.
     ;;
    (defn #_"BigInteger" BigInteger'max [#_"BigInteger" a, #_"BigInteger" b]
        (if (pos? (.compareTo a, b)) a b)
    )

    ;; arithmetic operations

    (defn- #_"int[]" BigInteger'subtract-la [#_"long" ab, #_"int[]" c*]
        (int-array
            (let-when [#_"int" a (int (>>> ab 32)) #_"long" c' (long! (aget c* 0))] (not (zero? a)) => [(int (- ab c'))]
                (let [#_"long" b' (long! (int ab))]
                    (case (alength c*)
                        1   (let [#_"long" e' (- b' c')]                  [(if (zero? (>> e' 32)) a (dec a))     (int e')])
                        2   (let [#_"long" e' (- b' (long! (aget c* 1)))] [(int (+ (- (long! a) c') (>> e' 32))) (int e')])
                    )
                )
            )
        )
    )

    ;;;
     ; Subtracts the contents of the second argument (cd) from the first (a*).
     ; The first int array (a*) must represent a larger number than the second.
     ; Assumes (<= 0 cd).
     ;;
    (defn- #_"int[]" BigInteger'subtract-al [#_"int[]" a*, #_"long" cd]
        (let [#_"int" i (alength a*) #_"int[]" e* (int-array i) #_"long" c' (long! (int (>>> cd 32)))
              [i #_"long" e']
                (if (zero? c')
                    (let [i (dec i) e' (- (long! (aget a* i)) cd)]
                        (aset e* i (int e'))
                        [i e']
                    )
                    (let [i (dec i) e' (- (long! (aget a* i)) (long! (int cd)))]
                        (aset e* i (int e'))
                        (let [i (dec i) e' (+ (- (long! (aget a* i)) c') (>> e' 32))]
                            (aset e* i (int e'))
                            [i e']
                        )
                    )
                )
              i (loop-when [i i #_"boolean" borrow (not (zero? (>> e' 32)))] (and (pos? i) borrow) => i
                    (let [i (dec i) #_"int" e (dec (aget a* i))]
                        (aset e* i e)
                        (recur i (== e -1))
                    )
                )]
            (loop-when i (pos? i)
                (let [i (dec i)]
                    (aset e* i (aget a* i))
                    (recur i)
                )
            )
            e*
        )
    )

    ;;;
     ; Subtracts the contents of the second int array (c*) from the first (a*).
     ; The first int array (a*) must represent a larger number than the second.
     ;;
    (defn- #_"int[]" BigInteger'subtract-aa [#_"int[]" a*, #_"int[]" c*]
        (let [#_"int" i (alength a*) #_"int[]" e* (int-array i)
              ;; subtract common parts of both numbers
              [i #_"int" j #_"long" e']
                (loop-when [i i j (alength c*) e' 0] (pos? j) => [i j e']
                    (let [i (dec i) j (dec j) e' (+ (- (long! (aget a* i)) (long! (aget c* j))) (>> e' 32))]
                        (aset e* i (int e'))
                        (recur i j e')
                    )
                )
              ;; subtract remainder of longer number while borrow propagates
              i (loop-when [i i #_"boolean" borrow (not (zero? (>> e' 32)))] (and (pos? i) borrow) => i
                    (let [i (dec i) #_"int" e (dec (aget a* i))]
                        (aset e* i e)
                        (recur i (== e -1))
                    )
                )]
            ;; copy remainder of longer number
            (loop-when i (pos? i)
                (let [i (dec i)]
                    (aset e* i (aget a* i))
                    (recur i)
                )
            )
            e*
        )
    )

    ;;;
     ; Adds the contents of int arrays x* and y*.
     ;;
    (defn- #_"int[]" BigInteger'add-aa [#_"int[]" x*, #_"int[]" y*]
        ;; when the first is shorter, swap the arrays
        (let-when [#_"int" i (alength x*) #_"int" j (alength y*)] (<= j i) => (recur y* x*)
            (let [#_"int" n i #_"int[]" z* (int-array n)
                  ;; add common parts of both numbers
                  [i #_"long" sum]
                    (loop-when [i i j j sum 0] (pos? j) => [i sum]
                        (let [i (dec i) j (dec j) sum (+ (long! (aget x* i)) (long! (aget y* j)) (>>> sum 32))]
                            (aset z* i (int sum))
                            (recur i j sum)
                        )
                    )
                  ;; copy remainder of longer number while carry propagation is required
                  [i #_"boolean" carry]
                    (loop-when [i i carry (not (zero? (>>> sum 32)))] (and (pos? i) carry) => [i carry]
                        (let [i (dec i) #_"int" z  (inc (aget x* i))]
                            (aset z* i z)
                            (recur i (zero? z))
                        )
                    )]
                ;; copy remainder of longer number
                (loop-when i (pos? i)
                    (let [i (dec i)]
                        (aset z* i (aget x* i))
                        (recur i)
                    )
                )
                ;; grow z* if necessary
                (when carry => z*
                    (let [#_"int[]" z+ (int-array (inc n))]
                        (System/arraycopy z*, 0, z+, 1, n)
                        (aset z+ 0 1)
                        z+
                    )
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (+ a b).
     ;;
    (defn #_"BigInteger" BigInteger'add [#_"BigInteger" a, #_"BigInteger" b]
        (let [#_"int" a? (:signum a) #_"int" b? (:signum b)]
            (cond (zero? a?) b (zero? b?) a :else
                (let-when [#_"int[]" a* (:mag a) #_"int[]" b* (:mag b)] (not (== a? b?)) => (BigInteger'new-2ai (BigInteger'add-aa a*, b*), a?)
                    (let-when [#_"int" c? (BigInteger'compare-aa a*, b*)] (not (zero? c?)) => BigInteger'ZERO
                        (let [#_"int[]" c* (if (pos? c?) (BigInteger'subtract-aa a*, b*) (BigInteger'subtract-aa b*, a*))]
                            (BigInteger'new-2ai (BigInteger'trustedStripLeadingZeroInts c*), (if (== c? a?) 1 -1))
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (- x).
     ;;
    (defn #_"BigInteger" BigInteger'negate [#_"BigInteger" x]
        (BigInteger'new-2ai (:mag x), (- (:signum x)))
    )

    ;;;
     ; Returns a BigInteger whose value is (- a b).
     ;;
    (defn #_"BigInteger" BigInteger'subtract [#_"BigInteger" a, #_"BigInteger" b]
        (let [#_"int" a? (:signum a) #_"int" b? (:signum b)]
            (cond (zero? a?) (BigInteger'negate b) (zero? b?) a :else
                (let-when [#_"int[]" a* (:mag a) #_"int[]" b* (:mag b)] (== a? b?) => (BigInteger'new-2ai (BigInteger'add-aa a*, b*), a?)
                    (let-when [#_"int" c? (BigInteger'compare-aa a*, b*)] (not (zero? c?)) => BigInteger'ZERO
                        (let [#_"int[]" c* (if (pos? c?) (BigInteger'subtract-aa a*, b*) (BigInteger'subtract-aa b*, a*))]
                            (BigInteger'new-2ai (BigInteger'trustedStripLeadingZeroInts c*), (if (== c? a?) 1 -1))
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is the absolute value of x.
     ;;
    (defn #_"BigInteger" BigInteger'abs [#_"BigInteger" x]
        (if (neg? (:signum x)) (BigInteger'negate x) x)
    )

    ;;;
     ; Returns the signum of x.
     ;
     ; @return -1, 0 or 1 as the value of x is negative, zero or positive.
     ;;
    (defn #_"int" BigInteger'signum [#_"BigInteger" x]
        (:signum x)
    )

    (declare BigInteger'shiftLeft-a)

    (defn- #_"int[]" BigInteger'multiply-ai [#_"int[]" x*, #_"int" y]
        (case (Integer/bitCount y)
            0   BigInteger'ZERO
            1   (BigInteger'shiftLeft-a x*, (Integer/numberOfTrailingZeros y))
                (let [#_"int" n (alength x*) #_"long" y' (long! y) #_"int[]" z* (int-array (inc n))
                      [#_"int" i #_"long" carry]
                        (loop-when [i (dec n) carry 0] (<= 0 i) => [i carry]
                            (let [#_"long" product (+ (* (long! (aget x* i)) y') carry)]
                                (aset z* (inc i) (int product))
                                (recur (dec i) (>>> product 32))
                            )
                        )]
                    (when-not (zero? carry) => (Arrays/copyOfRange z*, 1, (inc n))
                        (aset z* (inc i) (int carry))
                        z*
                    )
                )
        )
    )

    ;;;
     ; Multiplies int arrays x* and y* to the specified lengths and places the
     ; result into z*. There will be no leading zeros in the resultant array.
     ;;
    (defn- #_"int[]" BigInteger'multiply-aa [#_"int[]" x*, #_"int" n, #_"int[]" y*, #_"int" m]
        (let [#_"int[]" z* (int-array (+ n m))]
            (loop-when-recur [#_"int" i (dec n)] (<= 0 i) [(dec i)]
                (loop-when [#_"int" j (dec m) #_"int" k (+ m i) #_"long" carry 0] (<= 0 j) => (aset z* i (int carry))
                    (let [#_"long" product (+ (* (long! (aget y* j)) (long! (aget x* i))) (long! (aget z* k)) carry)]
                        (aset z* k (int product))
                        (recur (dec j) (dec k) (>>> product 32))
                    )
                )
            )
            z*
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (* x y).
     ;;
    (defn #_"BigInteger" BigInteger'multiply [#_"BigInteger" x, #_"BigInteger" y]
        (let-when [#_"int" x? (:signum x) #_"int" y? (:signum y)] (not (zero? (* x? y?))) => BigInteger'ZERO
            (let [#_"int[]" x* (:mag x) #_"int" n (alength x*)
                  #_"int[]" y* (:mag y) #_"int" m (alength y*)
                  #_"int[]" z*
                    (condp == 1
                        m   (BigInteger'multiply-ai x*, (aget y* 0))
                        n   (BigInteger'multiply-ai y*, (aget x* 0))
                            (BigInteger'trustedStripLeadingZeroInts (BigInteger'multiply-aa x*, n, y*, m))
                    )]
                (BigInteger'new-2ai z*, (if (== x? y?) 1 -1))
            )
        )
    )

    ;; squaring

    ;;;
     ; Multiply int array x* by one word y and add to int array z*, return the carry.
     ;;
    (defn- #_"int" BigInteger'mulAdd [#_"int[]" x*, #_"int" n, #_"int" y, #_"int[]" z*, #_"int" k]
        (let [#_"long" y' (long! y)]
            (loop-when [#_"int" i (dec n) k (- (alength z*) k 1) #_"long" carry 0] (<= 0 i) => (int carry)
                (let [#_"long" product (+ (* (long! (aget x* i)) y') (long! (aget z* k)) carry)]
                    (aset z* k (int product))
                    (recur (dec i) (dec k) (>>> product 32))
                )
            )
        )
    )

    ;;;
     ; Add one word y to int array x* n words at i into x*, return the carry.
     ;;
    (defn- #_"int" BigInteger'addOne [#_"int[]" x*, #_"int" i, #_"int" n, #_"int" y]
        (let [i (- (alength x*) n i 1) #_"long" sum (+ (long! (aget x* i)) (long! y))]
            (aset x* i (int sum))
            (when-not (zero? (>>> sum 32)) => 0
                (loop-when [i (dec i) n (dec n)] (and (<= 0 i) (<= 0 n)) => 1
                    (recur-if (zero? (aswap x* i inc)) [(dec i) (dec n)] => 0)
                )
            )
        )
    )

    ;; right shift int array a up to n ints by shift bits assuming no leading zeros, where 0 < shift < 32
    (defn- #_"void" BigInteger'primitiveRightShift [#_"int[]" a, #_"int" n, #_"int" shift]
        (loop-when [#_"int" i (dec n) #_"int" c (aget a i)] (pos? i) => (aswap a i >>> shift)
            (let [#_"int" b c c (aget a (dec i))]
                (aset a i (| (<< c (- 32 shift)) (>>> b shift)))
                (recur (dec i) c)
            )
        )
        nil
    )

    ;; left shift int array a up to n ints by shift bits assuming no leading zeros, where 0 <= shift < 32
    (defn- #_"void" BigInteger'primitiveLeftShift [#_"int[]" a, #_"int" n, #_"int" shift]
        (when (and (pos? n) (pos? shift))
            (loop-when [#_"int" i 0 #_"int" c (aget a i)] (< i (dec n)) => (aswap a i << shift)
                (let [#_"int" b c c (aget a (inc i))]
                    (aset a i (| (<< b shift) (>>> c (- 32 shift))))
                    (recur (inc i) c)
                )
            )
        )
        nil
    )

    (defn- #_"int[]" BigInteger'leftShift-a [#_"int[]" a, #_"int" shift]
        (let [#_"int" n (alength a) #_"int" nInts (>>> shift 5) #_"int" nBits (& shift 31) #_"int" mBits (BigInteger'bitLengthForInt (aget a 0))]
            ;; if shift can be done without copy, do so
            (if (<= shift (- 32 mBits))
                (do
                    (BigInteger'primitiveLeftShift a, n, nBits)
                    a
                )
                ;; array must be resized
                (if (<= nBits (- 32 mBits))
                    (let [#_"int[]" b (int-array (+ nInts n))]
                        (System/arraycopy a, 0, b, 0, n)
                        (BigInteger'primitiveLeftShift b, (alength b), nBits)
                        b
                    )
                    (let [#_"int[]" b (int-array (+ nInts n 1))]
                        (System/arraycopy a, 0, b, 0, n)
                        (BigInteger'primitiveRightShift b, (alength b), (- 32 nBits))
                        b
                    )
                )
            )
        )
    )

    ;;;
     ; Squares the contents of int array x* into int array z*.
     ; The contents of x* are not changed.
     ;
     ; The algorithm used here is adapted from Colin Plumb's C library.
     ; Technique: Consider the partial products in the multiplication of
     ; abcde by itself:
     ;
     ;               a  b  c  d  e
     ;            *  a  b  c  d  e
     ;          ==================
     ;              ae be ce de ee
     ;           ad bd cd dd de
     ;        ac bc cc cd ce
     ;     ab bb bc bd be
     ;  aa ab ac ad ae
     ;
     ; Note that everything above the main diagonal:
     ;
     ;              ae be ce de = (abcd) * e
     ;           ad bd cd       = (abc) * d
     ;        ac bc             = (ab) * c
     ;     ab                   = (a) * b
     ;
     ; is a copy of everything below the main diagonal:
     ;
     ;                       de
     ;                 cd ce
     ;           bc bd be
     ;     ab ac ad ae
     ;
     ; Thus, the sum is 2 * (off the diagonal) + diagonal.
     ;
     ; This is accumulated beginning with the diagonal (which consist of the
     ; squares of the digits of the input), which is then divided by two, the
     ; off-diagonal added, and multiplied by two again. The low bit is simply
     ; a copy of the low bit of the input, so it doesn't need special care.
     ;;
    (defn- #_"int[]" BigInteger'square-a
        ([#_"int[]" x*] (BigInteger'square-a x*, (alength x*)))
        ([#_"int[]" x*, #_"int" n]
            (let [#_"int" m (<< n 1) #_"int[]" z* (int-array m)]
                ;; store the squares, right shifted one bit (i.e., divided by 2)
                (loop-when [#_"int" i 0 #_"int" carry 0] (< i n)
                    (let [#_"long" x' (long! (aget x* i)) #_"long" square (* x' x') #_"int" k (<< i 1)]
                        (aset z* k (| (<< carry 31) (int (>>> square 33))))
                        (aset z* (inc k) (int (>>> square 1)))
                        (recur (inc i) (int square))
                    )
                )
                ;; add in off-diagonal sums
                (loop-when-recur [#_"int" i (dec n) #_"int" k 0] (<= 0 i) [(dec i) (+ k 2)]
                    (BigInteger'addOne z*, k, (inc i), (BigInteger'mulAdd x*, i, (aget x* i), z*, (inc k)))
                )
                ;; shift back up and set low bit
                (BigInteger'primitiveLeftShift z*, m, 1)
                (aswap z* (dec m) | (& (aget x* (dec n)) 1))
                z*
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (* x x).
     ;;
    (defn #_"BigInteger" BigInteger'square [#_"BigInteger" x]
        (when-not (zero? (:signum x)) => BigInteger'ZERO
            (BigInteger'new-2ai (BigInteger'trustedStripLeadingZeroInts (BigInteger'square-a (:mag x))), 1)
        )
    )

    ;; division

    ;;;
     ; Returns a BigInteger whose value is (/ x y) using an O(n^2) algorithm from Knuth.
     ;
     ; @throws ArithmeticException if y is zero.
     ;;
    (defn #_"BigInteger" BigInteger'divide [#_"BigInteger" x, #_"BigInteger" y]
        (let [[#_"MutableBigInteger" q _] (MutableBigInteger'divide (MutableBigInteger'new-a (:mag x)), (MutableBigInteger'new-a (:mag y)))]
            (MutableBigInteger'toBigInteger q, (* (:signum x) (:signum y)))
        )
    )

    ;;;
     ; Returns a pair of BigIntegers containing (/ x y) followed by (% x y).
     ;
     ; @throws ArithmeticException if y is zero.
     ;;
    (defn #_"[BigInteger BigInteger]" BigInteger'divideAndRemainder [#_"BigInteger" x, #_"BigInteger" y]
        (let [[#_"MutableBigInteger" q #_"MutableBigInteger" r] (MutableBigInteger'divide (MutableBigInteger'new-a (:mag x)), (MutableBigInteger'new-a (:mag y)))]
            [(MutableBigInteger'toBigInteger q, (if (== (:signum x) (:signum y)) 1 -1)) (MutableBigInteger'toBigInteger r, (:signum x))]
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (% x y).
     ;
     ; @throws ArithmeticException if y is zero.
     ;;
    (defn #_"BigInteger" BigInteger'remainder [#_"BigInteger" x, #_"BigInteger" y]
        (let [[_ #_"MutableBigInteger" r] (MutableBigInteger'divide (MutableBigInteger'new-a (:mag x)), (MutableBigInteger'new-a (:mag y)))]
            (MutableBigInteger'toBigInteger r, (:signum x))
        )
    )

    ;; bitwise operations

    ;;;
     ; Returns a BigInteger whose value is (& x y).
     ; (Returns a negative BigInteger if and only if x and y are both negative.)
     ;;
    (defn #_"BigInteger" BigInteger'and [#_"BigInteger" x, #_"BigInteger" y]
        (let [#_"int" n (max (BigInteger''intLength x) (BigInteger''intLength y)) #_"int[]" z* (int-array n)]
            (dotimes [#_"int" i n]
                (aset z* i (& (BigInteger''getInt x, (- n i 1)) (BigInteger''getInt y, (- n i 1))))
            )
            (BigInteger'valueOf-a z*)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (| x y).
     ; (Returns a negative BigInteger if and only if either x or y is negative.)
     ;;
    (defn #_"BigInteger" BigInteger'or [#_"BigInteger" x, #_"BigInteger" y]
        (let [#_"int" n (max (BigInteger''intLength x) (BigInteger''intLength y)) #_"int[]" z* (int-array n)]
            (dotimes [#_"int" i n]
                (aset z* i (| (BigInteger''getInt x, (- n i 1)) (BigInteger''getInt y, (- n i 1))))
            )
            (BigInteger'valueOf-a z*)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (^ x y).
     ; (Returns a negative BigInteger if and only if exactly one of x and y are negative.)
     ;;
    (defn #_"BigInteger" BigInteger'xor [#_"BigInteger" x, #_"BigInteger" y]
        (let [#_"int" n (max (BigInteger''intLength x) (BigInteger''intLength y)) #_"int[]" z* (int-array n)]
            (dotimes [#_"int" i n]
                (aset z* i (bit-xor (BigInteger''getInt x, (- n i 1)) (BigInteger''getInt y, (- n i 1))))
            )
            (BigInteger'valueOf-a z*)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (~ x).
     ; (Returns a negative BigInteger if and only if x is non-negative.)
     ;;
    (defn #_"BigInteger" BigInteger'not [#_"BigInteger" x]
        (let [#_"int" n (BigInteger''intLength x) #_"int[]" z* (int-array n)]
            (dotimes [#_"int" i n]
                (aset z* i (bit-not (BigInteger''getInt x, (- n i 1))))
            )
            (BigInteger'valueOf-a z*)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (& x (~ y)).
     ; This method, which is equivalent to (and x (not y)), is provided as a convenience for masking operations.
     ; (Returns a negative BigInteger if and only if x is negative and y is positive.)
     ;;
    (defn #_"BigInteger" BigInteger'andNot [#_"BigInteger" x, #_"BigInteger" y]
        (let [#_"int" n (max (BigInteger''intLength x) (BigInteger''intLength y)) #_"int[]" z* (int-array n)]
            (dotimes [#_"int" i n]
                (aset z* i (& (BigInteger''getInt x, (- n i 1)) (bit-not (BigInteger''getInt y, (- n i 1)))))
            )
            (BigInteger'valueOf-a z*)
        )
    )

    ;; single bit operations

    ;;;
     ; Returns true if and only if the designated bit is set.
     ; Computes (not (zero? (& x (<< 1 n)))).
     ;
     ; @throws ArithmeticException if n is negative.
     ;;
    (defn #_"boolean" BigInteger'testBit [#_"BigInteger" x, #_"int" n]
        (when-not (neg? n) => (throw! "negative bit address")
            (not (zero? (& (BigInteger''getInt x, (>>> n 5)) (<< 1 (& n 31)))))
        )
    )

    ;;;
     ; Returns a BigInteger whose value is equivalent to x with the designated bit set.
     ; Computes (| x (<< 1 n)).
     ;
     ; @throws ArithmeticException if n is negative.
     ;;
    (defn #_"BigInteger" BigInteger'setBit [#_"BigInteger" x, #_"int" n]
        (when-not (neg? n) => (throw! "negative bit address")
            (let [#_"int" j (>>> n 5) #_"int" m (max (BigInteger''intLength x) (+ j 2)) #_"int[]" a (int-array m)]
                (dotimes [#_"int" i m]
                    (aset a (- m i 1) (BigInteger''getInt x, i))
                )
                (aswap a (- m j 1) | (<< 1 (& n 31)))
                (BigInteger'valueOf-a a)
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is equivalent to x with the designated bit cleared.
     ; Computes (& x (~ (<< 1 n))).
     ;
     ; @throws ArithmeticException if n is negative.
     ;;
    (defn #_"BigInteger" BigInteger'clearBit [#_"BigInteger" x, #_"int" n]
        (when-not (neg? n) => (throw! "negative bit address")
            (let [#_"int" j (>>> n 5) #_"int" m (max (BigInteger''intLength x) (inc (>>> (inc n) 5))) #_"int[]" a (int-array m)]
                (dotimes [#_"int" i m]
                    (aset a (- m i 1) (BigInteger''getInt x, i))
                )
                (aswap a (- m j 1) & (bit-not (<< 1 (& n 31))))
                (BigInteger'valueOf-a a)
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is equivalent to x with the designated bit flipped.
     ; Computes (^ x (<< 1 n)).
     ;
     ; @throws ArithmeticException if n is negative.
     ;;
    (defn #_"BigInteger" BigInteger'flipBit [#_"BigInteger" x, #_"int" n]
        (when-not (neg? n) => (throw! "negative bit address")
            (let [#_"int" j (>>> n 5) #_"int" m (max (BigInteger''intLength x) (+ j 2)) #_"int[]" a (int-array m)]
                (dotimes [#_"int" i m]
                    (aset a (- m i 1) (BigInteger''getInt x, i))
                )
                (aswap a (- m j 1) bit-xor (<< 1 (& n 31)))
                (BigInteger'valueOf-a a)
            )
        )
    )

    ;; shift operations

    (defn- #_"int[]" BigInteger'inc-a [#_"int[]" a]
        (let [#_"int" n (alength a)
              #_"int" x (loop-when-recur [x 0 #_"int" i (dec n)] (and (<= 0 i) (zero? x)) [(aswap a i inc) (dec i)] => x)]
            (when (zero? x) => a
                (let [a (int-array (inc n))]
                    (aset a 0 1)
                    a
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (>> x shift).
     ; The shift distance is considered being unsigned.
     ; Computes (floor (* x (pow 2 (- shift)))).
     ;;
    (defn- #_"BigInteger" BigInteger'shiftRight-i [#_"BigInteger" x, #_"int" shift]
        (let [#_"int[]" a (:mag x) #_"int" n (alength a) #_"int" m (- n (>>> shift 5))]
            ;; special case: entire contents shifted off the end
            (when (pos? m) => (if (neg? (:signum x)) BigInteger'MINUS_ONE BigInteger'ZERO)
                (let [#_"int" bits (& shift 31)
                      #_"int[]" b
                        (when (pos? bits) => (Arrays/copyOf a, m)
                            (let [#_"int" y (>>> (aget a 0) bits)
                                  [b #_"int" j]
                                    (when-not (zero? y) => [(int-array (dec m)) 0]
                                        (let [b (int-array m)]
                                            (aset b 0 y)
                                            [b 1]
                                        )
                                    )]
                                (loop-when-recur [j j #_"int" i 0] (< i (dec m)) [(inc j) (inc i)]
                                    (aset b j (| (<< (aget a i) (- 32 bits)) (>>> (aget a (inc i)) bits)))
                                )
                                b
                            )
                        )
                      b (when (neg? (:signum x)) => b
                            ;; find out whether any one-bits were shifted off the end
                            (let [? (loop-when [#_"int" i (dec n)] (<= m i) => (and (pos? bits) (not (zero? (<< (aget a i) (- 32 bits)))))
                                        (or (not (zero? (aget a i))) (recur (dec i)))
                                    )]
                                (when ? => b
                                    (BigInteger'inc-a b)
                                )
                            )
                        )]
                    (BigInteger'new-2ai b, (:signum x))
                )
            )
        )
    )

    ;;;
     ; Returns a magnitude array whose value is (<< a shift).
     ; The most-significant int of a (aget a 0) must be non-zero.
     ; The shift distance is considered being unsigned.
     ; Computes (* a (pow 2 shift)).
     ;;
    (defn- #_"int[]" BigInteger'shiftLeft-a [#_"int[]" a, #_"int" shift]
        (let [#_"int" n (alength a) #_"int" m (+ n (>>> shift 5)) #_"int" bits (& shift 31)]
            (if (zero? bits)
                (let [#_"int[]" b (int-array m)]
                    (System/arraycopy a, 0, b, 0, n)
                    b
                )
                (let [#_"int" y (>>> (aget a 0) (- 32 bits))
                      [#_"int[]" b #_"int" j]
                        (when-not (zero? y) => [(int-array m) 0]
                            (let [b (int-array (inc m))]
                                (aset b 0 y)
                                [b 1]
                            )
                        )]
                    (loop-when-recur [j j #_"int" i 0] (< i (dec n)) [(inc j) (inc i)] => (aset b j (<< (aget a i) bits))
                        (aset b j (| (<< (aget a i) bits) (>>> (aget a (inc i)) (- 32 bits))))
                    )
                    b
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (<< x shift).
     ; The shift distance may be negative, that means a right shift.
     ; Computes (floor (* x (pow 2 shift))).
     ;;
    (defn #_"BigInteger" BigInteger'shiftLeft [#_"BigInteger" x, #_"int" shift]
        (cond
            (zero? (:signum x)) BigInteger'ZERO
            (pos? shift)        (BigInteger'new-2ai (BigInteger'shiftLeft-a (:mag x), shift), (:signum x))
            (zero? shift)       x
            ;; possible int overflow in (- shift) is harmless, as considered being unsigned
            :else               (BigInteger'shiftRight-i x, (- shift))
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (>> x shift).
     ; Sign extension is performed.
     ; The shift distance may be negative, that means a left shift.
     ; Computes (floor (/ x (pow 2 shift))).
     ;;
    (defn #_"BigInteger" BigInteger'shiftRight [#_"BigInteger" x, #_"int" shift]
        (cond
            (zero? (:signum x)) BigInteger'ZERO
            (pos? shift)        (BigInteger'shiftRight-i x, shift)
            (zero? shift)       x
            ;; possible int overflow in (- shift) is harmless, as considered being unsigned
            :else               (BigInteger'new-2ai (BigInteger'shiftLeft-a (:mag x), (- shift)), (:signum x))
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (pow x e).
     ;
     ; @throws ArithmeticException if exponent is negative, as it would yield a non-integer value.
     ;;
    (defn #_"BigInteger" BigInteger'pow [#_"BigInteger" x, #_"int" e]
        (cond
            (neg? e)            (throw! "negative exponent")
            (zero? (:signum x)) (if (zero? e) BigInteger'ONE x)
            :else
            ;; Factor out powers of two from the base, as the exponentiation of these can be done by left shifts only.
            ;; The remaining part can then be exponentiated faster. The powers of two will be multiplied back at the end.
            (let [#_"BigInteger" a (BigInteger'abs x) #_"int" order (BigInteger''getLowestSetBit a) #_"long" shift (* (long order) e)]
                (when (<= shift Integer/MAX_VALUE) => (throw! "magnitude overflow")
                    ;; Factor the powers of two out quickly by shifting right, if needed.
                    (let [#_"boolean" minus? (and (neg? (:signum x)) (== (& e 1) 1))
                          [a #_"int" b #_"BigInteger" y]
                            (if (pos? order)
                                (let-when [a (BigInteger'shiftRight a, order) b (BigInteger''bitLength a)] (== b 1) => [a b nil]
                                    [a b (BigInteger'shiftLeft (if minus? BigInteger'MINUS_ONE BigInteger'ONE), (* order e))]
                                )
                                (let-when [b (BigInteger''bitLength a)] (== b 1) => [a b nil]
                                    [a b (if minus? BigInteger'MINUS_ONE BigInteger'ONE)]
                                )
                            )]
                        (when (nil? y) => y
                            ;; This is a quick way to approximate the size of the result, similar to doing log2[n] * exponent.
                            ;; This will give an upper bound of how big the result can be, and which algorithm to use.
                            (let [#_"long" scale (* (long b) e)]
                                ;; Use slightly different algorithms for small and large operands.
                                ;; See if the result will safely fit into a long (max 2^63-1).
                                (cond (and (== (alength (:mag a)) 1) (< scale 63))
                                    ;; Small number algorithm. Everything fits into a long.
                                    (let [#_"long" r 1 #_"int" s (if minus? -1 1)
                                          ;; perform exponentiation using repeated squaring trick
                                          r (loop-when [r r #_"int" p e #_"long" q (long! (aget (:mag a) 0))] (not (zero? p)) => r
                                                (let [r (if (== (& p 1) 1) (* r q) r) p (>>> p 1)]
                                                    (recur r p (if (not (zero? p)) (* q q) q))
                                                )
                                            )]
                                        ;; multiply back the powers of two (quickly, by shifting left)
                                        (when (pos? order) => (BigInteger'valueOf-l (* r s))
                                            (if (< (+ shift scale) 63) ;; Fits in long?
                                                (BigInteger'valueOf-l (* (<< r shift) s))
                                                (-> (BigInteger'valueOf-l (* r s)) (BigInteger'shiftLeft (int shift)))
                                            )
                                        )
                                    )
                                    :else
                                    ;; Large number algorithm. This is basically identical to the above, but
                                    ;; calls multiply and square, which may be more efficient for large numbers.
                                    (let [#_"BigInteger" r BigInteger'ONE
                                          ;; perform exponentiation using repeated squaring trick
                                          r (loop-when [r r #_"int" p e #_"BigInteger" q a] (not (zero? p)) => r
                                                (let [r (if (== (& p 1) 1) (BigInteger'multiply r, q) r) p (>>> p 1)]
                                                    (recur r p (if (not (zero? p)) (BigInteger'square q) q))
                                                )
                                            )
                                          ;; multiply back the (exponentiated) powers of two (quickly, by shifting left)
                                          r (if (pos? order) (BigInteger'shiftLeft r, (* order e)) r)]
                                        (if minus? (BigInteger'negate r) r)
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
     ; Returns a BigInteger whose value is the greatest common divisor of (abs x) and (abs y).
     ; Returns 0 if (zero? x) and (zero? y).
     ;;
    (defn #_"BigInteger" BigInteger'gcd [#_"BigInteger" x, #_"BigInteger" y]
        (cond
            (zero? (:signum y)) (BigInteger'abs x)
            (zero? (:signum x)) (BigInteger'abs y)
            :else
            (let [#_"MutableBigInteger" a (MutableBigInteger'fromBigInteger x)
                  #_"MutableBigInteger" b (MutableBigInteger'fromBigInteger y)]
                (MutableBigInteger'toBigInteger (MutableBigInteger'hybridGCD a, b), 1)
            )
        )
    )

    ;; modular arithmetic operations

    ;;;
     ; Returns a BigInteger whose value is (mod x y).
     ; This method differs from remainder, as it always returns a *non-negative* BigInteger.
     ;
     ; @throws ArithmeticException if (<= y 0).
     ;;
    (defn #_"BigInteger" BigInteger'mod [#_"BigInteger" x, #_"BigInteger" y]
        (when (pos? (:signum y)) => (throw! "modulus not positive")
            (let [#_"BigInteger" r (BigInteger'remainder x, y)]
                (if (neg? (:signum r)) (BigInteger'add r, y) r)
            )
        )
    )

    ;;;
     ; Subtracts two numbers of same length, returning borrow.
     ;;
    (defn- #_"int" BigInteger'subN [#_"int[]" a, #_"int[]" b, #_"int" n]
        (loop-when [#_"long" sub 0 n (dec n)] (<= 0 n) => (int (>> sub 32))
            (let [sub (+ (- (long! (aget a n)) (long! (aget b n))) (>> sub 32))]
                (aset a n (int sub))
                (recur sub (dec n))
            )
        )
    )

    ;;
     ; Returns -1, 0 or +1 as big-endian unsigned int array a is less than,
     ; equal to, or greater than b up to length n.
     ;;
    (defn- #_"int" BigInteger'intArrayCmpToLen [#_"int[]" a, #_"int[]" b, #_"int" n]
        (loop-when [#_"int" i 0] (< i n) => 0
            (let [#_"long" a' (long! (aget a i)) #_"long" b' (long! (aget b i))]
                (cond (< a' b') -1 (< b' a') 1 :else (recur (inc i)))
            )
        )
    )

    ;;;
     ; Montgomery reduce a modulo b. This reduces modulo b and divides by (pow 2 (* 32 m)).
     ; Adapted from Colin Plumb's C library.
     ;;
    (defn- #_"int[]" BigInteger'montReduce [#_"int[]" a, #_"int[]" b, #_"int" m, #_"int" inv]
        (let [#_"int" c
                (loop [c 0 #_"int" i 0 #_"int" n m]
                    (let [#_"int" end (aget a (- (alength a) 1 i))
                          #_"int" carry (BigInteger'mulAdd b, m, (* inv end), a, i)
                          c (+ c (BigInteger'addOne a, i, m, carry)) i (inc i) n (dec n)]
                        (recur-if (pos? n) [c i n] => c)
                    )
                )
              _ (loop-when-recur c (pos? c) (+ c (BigInteger'subN a, b, m)))]
            (while (<= 0 (BigInteger'intArrayCmpToLen a, b, m))
                (BigInteger'subN a, b, m)
            )
            a
        )
    )

    (def #_"int[]" BigInteger'bnExpModThreshTable (int-array [7, 25, 81, 241, 673, 1793, Integer/MAX_VALUE])) ;; sentinel

    ;;;
     ; Returns a BigInteger whose value is x to the power of y mod z.
     ; Assumes: (and (odd? z) (< x z)).
     ;
     ; The algorithm is adapted from Colin Plumb's C library.
     ;
     ; The window algorithm:
     ;
     ; the idea is to keep a running product of b1 = n^(high-order bits of exp)
     ; and then keep appending exponent bits to it. The following patterns apply
     ; to a 3-bit window (k = 3):
     ;
     ; to append   0: square
     ; to append   1: square, multiply by n^1
     ; to append  10: square, multiply by n^1, square
     ; to append  11: square, square, multiply by n^3
     ; to append 100: square, multiply by n^1, square, square
     ; to append 101: square, square, square, multiply by n^5
     ; to append 110: square, square, multiply by n^3, square
     ; to append 111: square, square, square, multiply by n^7
     ;
     ; Since each pattern involves only one multiply, the longer the pattern
     ; the better, except that a 0 (no multiplies) can be appended directly.
     ; We precompute a table of odd powers of n, up to 2^k, and can then
     ; multiply k bits of exponent at a time. Actually, assuming random
     ; exponents, there is on average one zero bit between needs to multiply
     ; (1/2 of the time there's none, 1/4 of the time there's 1,
     ; 1/8 of the time, there's 2, 1/32 of the time, there's 3, etc.),
     ; so you have to do one multiply per k+1 bits of exponent.
     ;
     ; The loop walks down the exponent, squaring the result buffer as it goes.
     ; There is a wbits+1 bit lookahead buffer, buf, that is filled with the
     ; upcoming exponent bits. (What is read after the end of the exponent is
     ; unimportant, but it is filled with zero here.) When the most-significant
     ; bit of this buffer becomes set, i.e. (buf & tblmask) != 0, we have to
     ; decide what pattern to multiply by, and when to do it. We decide,
     ; remember to do it in future after a suitable number of squarings have
     ; passed (e.g. a pattern of "100" in the buffer requires that we multiply
     ; by n^1 immediately; a pattern of "110" calls for multiplying by n^3 after
     ; one more squaring), clear the buffer, and continue.
     ;
     ; When we start, there is one more optimization: the result buffer is
     ; implcitly one, so squaring it or multiplying by it can be optimized away.
     ; Further, if we start with a pattern like "100" in the lookahead window,
     ; rather than placing n into the buffer and then starting to square it,
     ; we have already computed n^2 to compute the odd-powers table, so we
     ; can place that into the buffer and save a squaring.
     ;
     ; This means that if you have a k-bit window, to compute n^z, where z is
     ; the high k bits of the exponent, 1/2 of the time it requires no squarings.
     ; 1/4 of the time, it requires 1 squaring, ... 1/2^(k-1) of the time, it
     ; reqires k-2 squarings. And the remaining 1/2^(k-1) of the time, the top
     ; k bits are a 1 followed by k-1 0 bits, so it again only requires k-2
     ; squarings, not k-1. The average of these is 1. Add that to the one
     ; squaring we have to do to compute the table, and you'll see that a k-bit
     ; window saves k-2 squarings as well as reducing the multiplies.
     ; (It actually doesn't hurt in the case k = 1, either.)
     ;;
    (defn- #_"BigInteger" BigInteger'oddModPow [#_"BigInteger" x, #_"BigInteger" y, #_"BigInteger" z]
        (cond
            (.equals y, BigInteger'ONE) x ;; special case for exponent of one
            (zero? (:signum x)) BigInteger'ZERO ;; special case for base of zero
            :else
            (let [#_"int[]" base (aclone (:mag x)) #_"int[]" exp (:mag y) #_"int[]" mod (:mag z) #_"int" modLen (alength mod)
                  ;; select an appropriate window size ;; if exponent is 65537 (0x10001), use minimum window size
                  #_"int" ebits (BigInteger'bitLength exp, (alength exp))
                  #_"int" wbits
                    (when (or (not (== ebits 17)) (not (== (aget exp 0) 65537))) => 0
                        (loop-when-recur [wbits 0] (< (aget BigInteger'bnExpModThreshTable wbits) ebits) [(inc wbits)] => wbits)
                    )
                  ;; calculate appropriate table size
                  #_"int" tblmask (<< 1 wbits)
                  ;; allocate table for precomputed odd powers of base in Montgomery form
                  #_"int[][]" table (make-array (Class/forName "[I") tblmask)
                  _ (dotimes [#_"int" i tblmask]
                        (aset table i (int-array modLen))
                    )
                  ;; compute the modular inverse
                  #_"int" inv (- (MutableBigInteger'inverseMod32 (aget mod (dec modLen))))
                  ;; convert base to Montgomery form
                  [_ #_"MutableBigInteger" r] (MutableBigInteger'divide (MutableBigInteger'new-a (BigInteger'leftShift-a base, (<< modLen 5))), (MutableBigInteger'new-a mod))]
                (aset table 0 (Arrays/copyOfRange (:value r), (:offset r), (+ (:offset r) (:intLen r))))
                ;; pad table[0] with leading zeros so its length is at least modLen
                (when (< (alength (aget table 0)) modLen)
                    (let [#_"int" offset (- modLen (alength (aget table 0)))
                          #_"int[]" a (int-array modLen)]
                        (dotimes [#_"int" i (alength (aget table 0))]
                            (aset a (+ i offset) (aget table 0 i))
                        )
                        (aset table 0 a)
                    )
                )
                ;; set b to the square of the base
                (let [#_"int[]" b (BigInteger'montReduce (BigInteger'square-a (aget table 0), modLen), mod, modLen, inv)
                      ;; set b' to high half of b
                      #_"int[]" b' (Arrays/copyOf b, modLen)
                      ;; fill in the table with odd powers of the base
                      _ (loop-when-recur [#_"int" i 1] (< i tblmask) [(inc i)]
                            (aset table i (BigInteger'montReduce (BigInteger'multiply-aa b', modLen, (aget table (dec i)), modLen), mod, modLen, inv))
                        )
                      ;; pre-load the window that slides over the exponent
                      [#_"int" buf #_"int" bitpos #_"int" eIndex #_"int" elen]
                        (loop-when [buf 0 bitpos (<< 1 (& (dec ebits) (dec 32))) eIndex 0 elen (alength exp) #_"int" i 0] (<= i wbits) => [buf bitpos eIndex elen]
                            (let [buf (| (<< buf 1) (if (zero? (& (aget exp eIndex) bitpos)) 0 1)) bitpos (>>> bitpos 1)
                                  [eIndex bitpos elen]
                                    (when (zero? bitpos) => [eIndex bitpos elen]
                                        [(inc eIndex) (<< 1 (dec 32)) (dec elen)]
                                    )]
                                (recur buf bitpos eIndex elen (inc i))
                            )
                        )
                      ;; the first iteration, which is hoisted out of the main loop
                      #_"int" multpos ebits ebits (dec ebits)
                      [buf multpos] (loop-when-recur [buf buf multpos (- ebits wbits)] (zero? (& buf 1)) [(>>> buf 1) (inc multpos)] => [buf multpos])
                      [#_"int[]" mult buf] [(aget table (>>> buf 1)) 0]
                      #_"boolean" isone? (not (== multpos ebits))
                      ;; the main loop
                      b (loop [b b ebits ebits buf buf bitpos bitpos eIndex eIndex elen elen multpos multpos mult mult isone? isone?]
                            ;; advance the window
                            (let [ebits (dec ebits) buf (<< buf 1)
                                  [buf bitpos eIndex elen]
                                    (when (not (zero? elen)) => [buf bitpos eIndex elen]
                                        (let [buf (| buf (if (zero? (& (aget exp eIndex) bitpos)) 0 1)) bitpos (>>> bitpos 1)
                                              [bitpos eIndex elen]
                                                (when (zero? bitpos) => [bitpos eIndex elen]
                                                    [(inc eIndex) (<< 1 (dec 32)) (dec elen)]
                                                )]
                                            [buf bitpos eIndex elen]
                                        )
                                    )
                                  ;; examine the window for pending multiplies
                                  [multpos mult buf]
                                    (when (not (zero? (& buf tblmask))) => [multpos mult buf]
                                        (let [[buf multpos] (loop-when-recur [buf buf multpos (- ebits wbits)] (zero? (& buf 1)) [(>>> buf 1) (inc multpos)] => [buf multpos])]
                                            [multpos (aget table (>>> buf 1)) 0]
                                        )
                                    )
                                  ;; perform multiply
                                  [b isone?]
                                    (when (== ebits multpos) => [b isone?]
                                        (if isone?
                                            [(aclone mult) false]
                                            [(BigInteger'montReduce (BigInteger'multiply-aa b, modLen, mult, modLen), mod, modLen, inv) isone?]
                                        )
                                    )]
                                ;; check if done
                                (when-not (zero? ebits) => b
                                    ;; square the input
                                    (let [b (if isone? b (BigInteger'montReduce (BigInteger'square-a b, modLen), mod, modLen, inv))]
                                        (recur b ebits buf bitpos eIndex elen multpos mult isone?)
                                    )
                                )
                            )
                        )]
                    ;; convert result out of Montgomery form and return
                    (let [#_"int[]" a (int-array (* 2 modLen))]
                        (System/arraycopy b, 0, a, modLen, modLen)
                        (BigInteger'new-2ia 1, (Arrays/copyOf (BigInteger'montReduce a, mod, modLen, inv), modLen))
                    )
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (mod x (pow 2 p)).
     ; Assumes that (and (<= 0 x) (pos? p)).
     ;;
    (defn- #_"BigInteger" BigInteger'mod2 [#_"BigInteger" x, #_"int" p]
        (when (< p (BigInteger''bitLength x)) => x
            ;; copy remaining ints of mag
            (let [#_"int" n (>>> (+ p 31) 5) #_"int[]" a (int-array n) #_"int" e (- (<< n 5) p)]
                (System/arraycopy (:mag x), (- (alength (:mag x)) n), a, 0, n)
                ;; mask out any excess bits
                (aswap a 0 & (dec (<< 1 (- 32 e))))
                (if (zero? (aget a 0)) (BigInteger'new-2ia 1, a) (BigInteger'new-2ai a, 1))
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (mod (pow x y) (pow 2 p)).
     ;
     ; Perform exponentiation using repeated squaring trick, chopping off
     ; high order bits as indicated by modulus.
     ;;
    (defn- #_"BigInteger" BigInteger'modPow2 [#_"BigInteger" x, #_"BigInteger" y, #_"int" p]
        (let [#_"int" n (BigInteger''bitLength y) n (if (BigInteger'testBit x, 0) (min (dec p) n) n)]
            (loop-when [#_"BigInteger" r BigInteger'ONE #_"BigInteger" m (BigInteger'mod2 x, p) #_"int" i 0] (< i n) => r
                (let [r (if (BigInteger'testBit y, i) (-> (BigInteger'multiply r, m) (BigInteger'mod2 p)) r)
                      i (inc i)
                      m (if (< i n) (-> (BigInteger'square m) (BigInteger'mod2 p)) m)]
                    (recur r m i)
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (mod (pow x -1) m).
     ;
     ; @throws ArithmeticException if (<= m 0), or x has no multiplicative inverse mod m
     ;         (that is, x is not *relatively prime* to m).
     ;;
    (defn #_"BigInteger" BigInteger'modInverse [#_"BigInteger" x, #_"BigInteger" m]
        (when (pos? (:signum m)) => (throw! "modulus not positive")
            (when-not (.equals m, BigInteger'ONE) => BigInteger'ZERO
                ;; calculate (x mod m)
                (let [#_"BigInteger" y (if (or (neg? (:signum x)) (<= 0 (BigInteger'compare-aa (:mag x), (:mag m)))) (BigInteger'mod x, m) x)]
                    (when-not (.equals y, BigInteger'ONE) => BigInteger'ONE
                        (MutableBigInteger'toBigInteger (MutableBigInteger'mutableModInverse (MutableBigInteger'fromBigInteger y), (MutableBigInteger'fromBigInteger m)), 1)
                    )
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (mod (pow x y) m).
     ; (Unlike pow, this method permits negative exponents.)
     ;
     ; @throws ArithmeticException if (<= m 0), or y is negative and x is not *relatively prime* to m.
     ;;
    (defn #_"BigInteger" BigInteger'modPow [#_"BigInteger" x, #_"BigInteger" y, #_"BigInteger" m]
        (when (pos? (:signum m)) => (throw! "modulus not positive")
            ;; trivial cases
            (cond
                (zero? (:signum y))                                                     (if (.equals m, BigInteger'ONE) BigInteger'ZERO BigInteger'ONE)
                (.equals x, BigInteger'ONE)                                             (if (.equals m, BigInteger'ONE) BigInteger'ZERO BigInteger'ONE)
                (and (.equals x, BigInteger'ZERO) (not (neg? (:signum y))))                                             BigInteger'ZERO
                (and (.equals x, BigInteger'MINUS_ONE) (not (BigInteger'testBit y, 0))) (if (.equals m, BigInteger'ONE) BigInteger'ZERO BigInteger'ONE)
                :else
                (let [#_"boolean" invert? (neg? (:signum y)) y (if invert? (BigInteger'negate y) y)
                      #_"BigInteger" base (if (or (neg? (:signum x)) (<= 0 (.compareTo x, m))) (BigInteger'mod x, m) x)
                      #_"BigInteger" r
                        (when-not (BigInteger'testBit m, 0) => (BigInteger'oddModPow base, y, m) ;; odd modulus
                            ;;
                            ; Even modulus. Tear it into an "odd part" (m1) and power of two (m2), exponentiate mod m1,
                            ; manually exponentiate mod m2, and use Chinese Remainder Theorem to combine results.
                            ;;
                            (let [#_"int" p (BigInteger''getLowestSetBit m) ;; max pow of 2 that divides m
                                  #_"BigInteger" m1 (BigInteger'shiftRight m, p) ;; m/2^p
                                  #_"BigInteger" m2 (BigInteger'shiftLeft BigInteger'ONE, p) ;; 2^p
                                  ;; calculate new base from m1
                                  #_"BigInteger" base2 (if (or (neg? (:signum x)) (<= 0 (.compareTo x, m1))) (BigInteger'mod x, m1) x)
                                  ;; caculate (base^y) mod m1
                                  #_"BigInteger" a1 (if (.equals m1, BigInteger'ONE) BigInteger'ZERO (BigInteger'oddModPow base2, y, m1))
                                  ;; calculate (x^y) mod m2
                                  #_"BigInteger" a2 (BigInteger'modPow2 base, y, p)
                                  ;; combine results using Chinese Remainder Theorem
                                  #_"BigInteger" y1 (BigInteger'modInverse m2, m1)
                                  #_"BigInteger" y2 (BigInteger'modInverse m1, m2)]
                                (if (< (alength (:mag m)) (quot BigInteger'MAX_MAG_LENGTH 2))
                                    (-> (BigInteger'multiply a1, m2)
                                        (BigInteger'multiply y1)
                                        (BigInteger'add (-> (BigInteger'multiply a2, m1) (BigInteger'multiply y2)))
                                        (BigInteger'mod m)
                                    )
                                    (let [#_"MutableBigInteger" t1
                                            (-> (MutableBigInteger'fromBigInteger (BigInteger'multiply a1, m2))
                                                (MutableBigInteger'multiply (MutableBigInteger'fromBigInteger y1))
                                            )
                                          #_"MutableBigInteger" t2
                                            (-> (MutableBigInteger'fromBigInteger (BigInteger'multiply a2, m1))
                                                (MutableBigInteger'multiply (MutableBigInteger'fromBigInteger y2))
                                            )
                                          [_ #_"MutableBigInteger" r] (MutableBigInteger'divide (MutableBigInteger''add t1, t2), (MutableBigInteger'fromBigInteger m))
                                          r (MutableBigInteger''normalize r)]
                                        (MutableBigInteger'toBigInteger r, (if (MutableBigInteger'isZero r) 0 1))
                                    )
                                )
                            )
                        )]
                    (if invert? (BigInteger'modInverse r, m) r)
                )
            )
        )
    )

    ;; hash function

    ;;;
     ; Returns the hash code for this BigInteger.
     ;;
    #_foreign
    (defn #_"int" hashCode---BigInteger [#_"BigInteger" this]
        (loop-when-recur [#_"int" hash 0 #_"int" i 0]
                         (< i (alength (:mag this)))
                         [(int (+ (* 31 hash) (long! (aget (:mag this) i)))) (inc i)]
                      => (* hash (:signum this))
        )
    )

    ;; zero[i] is a string of i consecutive zeros
    (def- #_"String[]" BigInteger'zeros
        (let [#_"String[]" a (make-array String 64)]
            (aset a 63 "000000000000000000000000000000000000000000000000000000000000000")
            (dotimes [#_"int" i 63]
                (aset a i (.substring (aget a 63), 0, i))
            )
            a
        )
    )

    ;;;
     ; This method is used to perform toString when arguments are small.
     ;;
    (defn- #_"String" BigInteger'toString [#_"BigInteger" this, #_"int" radix]
        (if (zero? (:signum this))
            "0"
            ;; compute upper bound on number of digit groups and allocate space
            (let [#_"String[]" digits (make-array String (quot (+ (* 4 (alength (:mag this))) 6) 7))
                  ;; translate number to string, a digit group at a time
                  #_"int" n
                    (loop-when [n 0 #_"BigInteger" x (BigInteger'abs this)] (not (zero? (:signum x))) => n
                        (let [#_"BigInteger" y (aget BigInteger'longRadix radix)
                              [#_"MutableBigInteger" q #_"MutableBigInteger" r]
                                (MutableBigInteger'divide (MutableBigInteger'new-a (:mag x)), (MutableBigInteger'new-a (:mag y)))
                              q (MutableBigInteger'toBigInteger q, (* (:signum x) (:signum y)))
                              r (MutableBigInteger'toBigInteger r, (* (:signum x) (:signum y)))]
                            (aset digits n (Long/toString (.longValue r), radix))
                            (recur (inc n) q)
                        )
                    )
                  ;; put sign (if any) and first digit group into result buffer
                  #_"StringBuilder" sb (StringBuilder. (inc (* n (aget BigInteger'digitsPerLong radix))))]
                (when (neg? (:signum this))
                    (.append sb, \-)
                )
                (.append sb, (aget digits (dec n)))
                ;; append remaining digit groups padded with leading zeros
                (loop-when-recur [#_"int" i (- n 2)] (<= 0 i) [(dec i)]
                    ;; prepend (any) leading zeros for this digit group
                    (let [#_"int" m (- (aget BigInteger'digitsPerLong radix) (.length (aget digits i)))]
                        (when-not (zero? m)
                            (.append sb, (aget BigInteger'zeros m))
                        )
                        (.append sb, (aget digits i))
                    )
                )
                (.toString sb)
            )
        )
    )

    ;;;
     ; Returns the String representation of this BigInteger in the given radix.
     ;
     ; If the radix is outside the range from Character/MIN_RADIX to Character/MAX_RADIX inclusive,
     ; it will default to 10 (as is the case for Integer/toString). The digit-to-character mapping
     ; provided by Character/forDigit is used, and a minus sign is prepended if appropriate.
     ;
     ; (This representation is compatible with the BigInteger(String, int) constructor.)
     ;;
    #_method
    (defn #_"String" BigInteger''toString [#_"BigInteger" this, #_"int" radix]
        (if (zero? (:signum this))
            "0"
            (let [radix (if (<= Character/MIN_RADIX radix Character/MAX_RADIX) radix 10)]
                (BigInteger'toString this, radix)
            )
        )
    )

    ;;;
     ; Returns the decimal String representation of this BigInteger.
     ;
     ; The digit-to-character mapping provided by Character/forDigit is used,
     ; and a minus sign is prepended if appropriate.
     ;
     ; (This representation is compatible with the BigInteger(String) constructor.)
     ;;
    #_foreign
    (defn #_"String" toString---BigInteger [#_"BigInteger" this]
        (BigInteger''toString this, 10)
    )

    ;;;
     ; Returns a byte array containing the two's-complement representation of x.
     ;
     ; The byte array will be in *big-endian* byte-order: the most-significant byte is in the
     ; 0th element. The array will contain the minimum number of bytes required to represent x,
     ; including at least one sign bit, which is (ceil (/ (inc (.bitLength x)) 8)).
     ;
     ; (This representation is compatible with the BigInteger(byte[]) constructor.)
     ;;
    (defn #_"byte[]" BigInteger'toByteArray [#_"BigInteger" x]
        (let [#_"int" n (inc (quot (BigInteger''bitLength x) 8)) #_"byte[]" b (byte-array n)]
            (loop-when [#_"int" m 4 #_"int" w 0 #_"int" i 0 #_"int" j (dec n)] (<= 0 j)
                (let [[w i m]
                        (if (== m 4)
                            [(BigInteger''getInt x, i) (inc i) 1]
                            [(>>> w 8) i (inc m)]
                        )]
                    (aset b j (byte w))
                    (recur m w i (dec j))
                )
            )
            b
        )
    )

    ;;;
     ; Converts this BigInteger to an int.
     ;
     ; This conversion is analogous to a *narrowing primitive conversion* from long to
     ; int as defined in section 5.1.3 of "The Java Language Specification": if this
     ; BigInteger is too big to fit in an int, only the low-order 32 bits are returned.
     ;
     ; Note that this conversion can lose information about the overall magnitude
     ; of the BigInteger value as well as return a result with the opposite sign.
     ;;
    #_foreign
    (defn #_"int" intValue---BigInteger [#_"BigInteger" this]
        (BigInteger''getInt this, 0)
    )

    ;;;
     ; Converts this BigInteger to a long.
     ;
     ; This conversion is analogous to a *narrowing primitive conversion* from long to
     ; int as defined in section 5.1.3 of "The Java Language Specification": if this
     ; BigInteger is too big to fit in a long, only the low-order 64 bits are returned.
     ;
     ; Note that this conversion can lose information about the overall magnitude
     ; of the BigInteger value as well as return a result with the opposite sign.
     ;;
    #_foreign
    (defn #_"long" longValue---BigInteger [#_"BigInteger" this]
        (loop-when-recur [#_"long" r 0 #_"int" i 1] (<= 0 i) [(+ (<< r 32) (long! (BigInteger''getInt this, i))) (dec i)] => r)
    )

    #_foreign
    (defn #_"float" floatValue---BigInteger [#_"BigInteger" this]
        (throw! "no floating loathing")
    )

    #_foreign
    (defn #_"double" doubleValue---BigInteger [#_"BigInteger" this]
        (throw! "no double bubble")
    )

    ;; primality testing

    ;;;
     ; Returns true iff x passes the specified number of Miller-Rabin tests.
     ; This test is taken from the DSA spec (NIST FIPS 186-2).
     ;
     ; The following assumptions are made:
     ; x is a positive, odd number greater than 2,
     ; n <= 50.
     ;;
    (defn- #_"boolean" BigInteger'passesMillerRabin [#_"BigInteger" x, #_"int" n, #_"Random" r]
        ;; find a and m such that m is odd and x == 1 + 2^a * m
        (let [r (or r (ThreadLocalRandom/current)) #_"BigInteger" x-- (BigInteger'subtract x, BigInteger'ONE)
              #_"BigInteger" m x-- #_"int" a (BigInteger''getLowestSetBit m) m (BigInteger'shiftRight m, a)]
            (loop-when [#_"int" i 0] (< i n) => true
                ;; generate a uniform random on (1, x)
                (let [#_"BigInteger" b
                        (loop []
                            (let [b (BigInteger'new-2ir (BigInteger''bitLength x), r)]
                                (recur-if (or (<= (.compareTo b, BigInteger'ONE) 0) (<= 0 (.compareTo b, x))) [] => b)
                            )
                        )
                      ? (loop [#_"int" j 0 #_"BigInteger" z (BigInteger'modPow b, m, x)]
                            (or (and (zero? j) (.equals z, BigInteger'ONE))
                                (.equals z, x--)
                                (if (and (pos? j) (.equals z, BigInteger'ONE))
                                    false
                                    (let-when [j (inc j)] (== j a) => (recur j (BigInteger'modPow z, BigInteger'TWO, x))
                                        false
                                    )
                                )
                            )
                        )]
                    (recur-if ? [(inc i)] => ?)
                )
            )
        )
    )

    ;;;
     ; Computes Jacobi(p,n).
     ; Assumes (and (pos? n) (odd? n) (<= 3 n)).
     ; Algorithm adapted from Colin Plumb's C library.
     ;;
    (defn- #_"int" BigInteger'jacobiSymbol [#_"int" p, #_"BigInteger" n]
        (when-not (zero? p) => 0
            (let [#_"int" u (aget (:mag n) (dec (alength (:mag n))))
                  [p #_"int" j]
                    (when (neg? p) => [p 1]
                        (let-when [#_"int" n8 (& u 7)] (or (== n8 3) (== n8 7)) => [(- p) 1]
                            [(- p) -1] ;; 3 (011) or 7 (111) mod 8
                        )
                    )
                  [p j] ;; get rid of factors of 2 in p
                    (let-when [p (loop-when-recur p (zero? (& p 3)) (>> p 2) => p)] (zero? (& p 1)) => [p j]
                        [(>> p 1) (if (zero? (& (bit-xor u (>> u 1)) 2)) j (- j))] ;; 3 (011) or 5 (101) mod 8
                    )]
                (when-not (== p 1) => j
                    ;; apply quadratic reciprocity
                    (let [j (if (zero? (& p u 2)) j (- j)) ;; p = u = 3 (mod 4)?
                          ;; reduce u mod p
                          u (.intValue (BigInteger'mod n, (BigInteger'valueOf-l p)))]
                        ;; compute Jacobi(u,p), u < p
                        (loop-when [j j u u p p] (not (zero? u)) => 0
                            (let [[u j] ;; get rid of factors of 2 in u
                                    (let-when [u (loop-when-recur u (zero? (& u 3)) (>> u 2) => u)] (zero? (& u 1)) => [u j]
                                        [(>> u 1) (if (zero? (& (bit-xor p (>> p 1)) 2)) j (- j))] ;; 3 (011) or 5 (101) mod 8
                                    )]
                                (when-not (== u 1) => j
                                    ;; both u and p are odd, so use quadratic reciprocity
                                    (when (< u p) => (throw! "(not (< u p))")
                                        (let [[u p] [p u]]
                                            ;; u >= p, so it can be reduced
                                            (recur (if (zero? (& u p 2)) j (- j)) (% u p) p) ;; u = p = 3 (mod 4)?
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

    (defn- #_"BigInteger" BigInteger'lucasLehmerSequence [#_"int" z, #_"BigInteger" k, #_"BigInteger" n]
        (let [#_"BigInteger" d (BigInteger'valueOf-l z)]
            (loop-when [#_"BigInteger" u BigInteger'ONE #_"BigInteger" v BigInteger'ONE #_"int" i (- (BigInteger''bitLength k) 2)] (<= 0 i) => u
                (let [[u v]
                        (let [#_"BigInteger" u2 (-> (BigInteger'multiply u, v) (BigInteger'mod n))
                              #_"BigInteger" v2 (-> (BigInteger'square v) (BigInteger'add (BigInteger'multiply d, (BigInteger'square u))) (BigInteger'mod n))
                              v2 (BigInteger'shiftRight (if (BigInteger'testBit v2, 0) (BigInteger'subtract v2, n) v2), 1)]
                            [u2 v2]
                        )
                      [u v]
                        (when (BigInteger'testBit k, i) => [u v]
                            (let [#_"BigInteger" u2 (-> (BigInteger'add u, v) (BigInteger'mod n))
                                  u2 (BigInteger'shiftRight (if (BigInteger'testBit u2, 0) (BigInteger'subtract u2, n) u2), 1)
                                  #_"BigInteger" v2 (-> (BigInteger'add v, (BigInteger'multiply d, u)) (BigInteger'mod n))
                                  v2 (BigInteger'shiftRight (if (BigInteger'testBit v2, 0) (BigInteger'subtract v2, n) v2), 1)]
                                [u2 v2]
                            )
                        )]
                    (recur u v (dec i))
                )
            )
        )
    )

    ;;;
     ; Returns true iff x is a Lucas-Lehmer probable prime.
     ;
     ; The following assumptions are made:
     ; x is a positive, odd number.
     ;;
    (defn- #_"boolean" BigInteger'passesLucasLehmer [#_"BigInteger" x]
        (let [#_"int" d (loop-when-recur [d 5] (not (== (BigInteger'jacobiSymbol d, x) -1)) [(- (if (neg? d) 2 -2) d)] => d)] ;; 5, -7, 9, -11, ...
            (.equals (BigInteger'mod (BigInteger'lucasLehmerSequence d, (BigInteger'add x, BigInteger'ONE), x), x), BigInteger'ZERO)
        )
    )

    ;;;
     ; Returns {@code true} if this BigInteger is probably prime,
     ; {@code false} if it's definitely composite.
     ;
     ; This method assumes bitLength > 2.
     ;
     ; @param  certainty a measure of the uncertainty that the caller is
     ;         willing to tolerate: if the call returns {@code true}
     ;         the probability that this BigInteger is prime exceeds
     ;         {@code (1 - 1/2<sup>certainty</sup>)}. The execution time of
     ;         this method is proportional to the value of this parameter.
     ; @return {@code true} if this BigInteger is probably prime,
     ;         {@code false} if it's definitely composite.
     ;;
    #_method
    (defn #_"boolean" BigInteger''primeToCertainty [#_"BigInteger" this, #_"int" certainty, #_"Random" random]
        (let [
              #_"int" rounds 0
              #_"int" n (quot (inc (min certainty (dec Integer/MAX_VALUE))) 2)
              ;; The relationship between the certainty and the number of rounds we perform is given in the draft
              ;; standard ANSI X9.80, "PRIME NUMBER GENERATION, PRIMALITY TESTING, AND PRIMALITY CERTIFICATES".
              #_"int" sizeInBits (BigInteger''bitLength this)
        ]
            (when (< sizeInBits 100)
                (§ ass rounds 50)
                (§ ass rounds (if (< n rounds) n rounds))
                (§ return (BigInteger'passesMillerRabin this, rounds, random))
            )

            (cond (< sizeInBits 256)
                (do
                    (§ ass rounds 27)
                )
                (< sizeInBits 512)
                (do
                    (§ ass rounds 15)
                )
                (< sizeInBits 768)
                (do
                    (§ ass rounds 8)
                )
                (< sizeInBits 1024)
                (do
                    (§ ass rounds 4)
                )
                :else
                (do
                    (§ ass rounds 2)
                )
            )
            (§ ass rounds (if (< n rounds) n rounds))

            (and (BigInteger'passesMillerRabin this, rounds, random) (BigInteger'passesLucasLehmer this))
        )
    )

    ;;;
     ; Returns {@code true} if this BigInteger is probably prime,
     ; {@code false} if it's definitely composite.
     ; If {@code certainty} is <= 0, {@code true} is returned.
     ;
     ; @param  certainty a measure of the uncertainty that the caller is
     ;         willing to tolerate: if the call returns {@code true}
     ;         the probability that this BigInteger is prime exceeds
     ;         (1 - 1/2<sup>{@code certainty}</sup>). The execution time of
     ;         this method is proportional to the value of this parameter.
     ; @return {@code true} if this BigInteger is probably prime,
     ;         {@code false} if it's definitely composite.
     ;;
    #_method
    (defn #_"boolean" BigInteger''isProbablePrime [#_"BigInteger" this, #_"int" certainty]
        (when (<= certainty 0)
            (§ return true)
        )
        (let [
              #_"BigInteger" w (BigInteger'abs this)
        ]
            (when (.equals w, BigInteger'TWO)
                (§ return true)
            )
            (when (or (not (BigInteger'testBit w, 0)) (.equals w, BigInteger'ONE))
                (§ return false)
            )

            (BigInteger''primeToCertainty w, certainty, nil)
        )
    )

    ;; Minimum size in bits that the requested prime number has
    ;; before we use the large prime number generating algorithms.
    ;; The cutoff of 95 was chosen empirically for best performance.
    (def- #_"int" BigInteger'SMALL_PRIME_THRESHOLD 95)

    ;; certainty required to meet the spec of probablePrime
    (def- #_"int" BigInteger'DEFAULT_PRIME_CERTAINTY 100)

    (def- #_"BigInteger" BigInteger'SMALL_PRIME_PRODUCT (§ soon BigInteger'valueOf-l (* 3 5 7 11 13 17 19 23 29 31 37 41)))

    ;;;
     ; Find a random number of the specified bitLength that is probably prime.
     ; This method is used for smaller primes, its performance degrades on larger bitlengths.
     ;
     ; This method assumes bitLength > 1.
     ;;
    (defn- #_"BigInteger" BigInteger'smallPrime [#_"int" bitLength, #_"int" certainty, #_"Random" rnd]
        (let [
              #_"int" magLen (>>> (+ bitLength 31) 5)
              #_"int[]" temp (int-array magLen)
              #_"int" highBit (<< 1 (& (+ bitLength 31) 31)) ;; high bit of high int
              #_"int" highMask (- (<< highBit 1) 1) ;; bits to keep in high int
        ]
            (§ while true
                ;; construct a candidate
                (loop-when-recur [#_"int" i 0] (< i magLen) [(inc i)]
                    (aset temp i (.nextInt rnd))
                )
                (aswap temp 0 #(| (& % highMask) highBit)) ;; ensure exact length
                (when (< 2 bitLength)
                    (aswap temp (dec magLen) | 1) ;; make odd if bitlen > 2
                )

                (let [
                      #_"BigInteger" p (BigInteger'new-2ai temp, 1)
                ]
                    ;; do cheap "pre-test" if applicable
                    (when (< 6 bitLength)
                        (let [
                              #_"long" r (.longValue (BigInteger'remainder p, BigInteger'SMALL_PRIME_PRODUCT))
                        ]
                            (when (or (zero? (% r 3)) (zero? (% r 5)) (zero? (% r 7)) (zero? (% r 11)) (zero? (% r 13)) (zero? (% r 17)) (zero? (% r 19)) (zero? (% r 23)) (zero? (% r 29)) (zero? (% r 31)) (zero? (% r 37)) (zero? (% r 41)))
                                (§ continue) ;; candidate is composite; try another
                            )
                        )
                    )

                    ;; all candidates of bitLength 2 and 3 are prime by this point
                    (when (< bitLength 4)
                        (§ return p)
                    )

                    ;; do expensive test if we survive pre-test (or it's inapplicable)
                    (when (BigInteger''primeToCertainty p, certainty, rnd)
                        (§ return p)
                    )
                )
            )
        )
    )

    (declare BitSieve'new-2)
    (declare BitSieve''retrieve)

    ;;;
     ; Find a random number of the specified bitLength that is probably prime.
     ; This method is more appropriate for larger bitlengths since it uses
     ; a sieve to eliminate most composites before using a more expensive test.
     ;;
    (defn- #_"BigInteger" BigInteger'largePrime [#_"int" bitLength, #_"int" certainty, #_"Random" rnd]
        (let [
              #_"BigInteger" p (-> (BigInteger'new-2ir bitLength, rnd) (BigInteger'setBit (dec bitLength)))
        ]
            (aswap (:mag p) (dec (alength (:mag p))) & 0xfffffffe)

            ;; use a sieve length likely to contain the next prime number
            (let [
                  #_"int" searchLen (BigInteger'getPrimeSearchLen bitLength)
                  #_"BitSieve" searchSieve (BitSieve'new-2 p, searchLen)
                  #_"BigInteger" candidate (BitSieve''retrieve searchSieve, p, certainty, rnd)
            ]
                (while (or (nil? candidate) (not (== (BigInteger''bitLength candidate) bitLength)))
                    (let [
                    ]
                        (§ ass p (BigInteger'add p, (BigInteger'valueOf-l (* 2 searchLen))))
                        (when (not (== (BigInteger''bitLength p) bitLength))
                            (§ ass p (-> (BigInteger'new-2ir bitLength, rnd) (BigInteger'setBit (dec bitLength))))
                        )
                        (aswap (:mag p) (dec (alength (:mag p))) & 0xfffffffe)
                        (§ ass searchSieve (BitSieve'new-2 p, searchLen))
                        (§ ass candidate (BitSieve''retrieve searchSieve, p, certainty, rnd))
                    )
                )
                candidate
            )
        )
    )

    ;;;
     ; Returns a randomly generated positive BigInteger that is probably prime, with the
     ; specified bitLength. By default, the probability that a BigInteger returned by this
     ; method is composite does not exceed 2<sup>-100</sup>.
     ;
     ; @param  bitLength bitLength of the returned BigInteger.
     ; @param  certainty a measure of the uncertainty that the caller is willing to tolerate.
     ;         The probability that the new BigInteger represents a prime number will exceed
     ;         (1 - 1/2<sup>{@code certainty}</sup>). The execution time of this function
     ;         is proportional to the value of this parameter.
     ; @param  rnd source of random bits used to select candidates to be tested for primality.
     ;
     ; @return a BigInteger of {@code bitLength} bits that is probably prime.
     ; @throws ArithmeticException {@code bitLength < 2} or {@code bitLength} is too large.
     ;;
    (defn #_"BigInteger" BigInteger'probablePrime
        ([#_"int" bitLength, #_"Random" rnd] (BigInteger'probablePrime bitLength, BigInteger'DEFAULT_PRIME_CERTAINTY, rnd))
        ([#_"int" bitLength, #_"int" certainty, #_"Random" rnd]
            (when (< bitLength 2)
                (throw! "(< bitLength 2)")
            )

            (if (< bitLength BigInteger'SMALL_PRIME_THRESHOLD)
                (BigInteger'smallPrime bitLength, certainty, rnd)
                (BigInteger'largePrime bitLength, certainty, rnd)
            )
        )
    )

    ;;;
     ; Returns the first integer greater than this {@code BigInteger} that
     ; is probably prime. The probability that the number returned by this
     ; method is composite does not exceed 2<sup>-100</sup>. This method will
     ; never skip over a prime when searching: if it returns {@code p}, there
     ; is no prime {@code q} such that {@code this < q < p}.
     ;
     ; @return the first integer greater than this {@code BigInteger} that is probably prime.
     ; @throws ArithmeticException {@code this < 0} or {@code this} is too large.
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''nextProbablePrime [#_"BigInteger" this]
        (when (neg? (:signum this))
            (throw! "negative start")
        )

        ;; handle trivial cases
        (when (or (zero? (:signum this)) (.equals this, BigInteger'ONE))
            (§ return BigInteger'TWO)
        )

        (let [
              #_"BigInteger" result (BigInteger'add this, BigInteger'ONE)
        ]
            ;; fastpath for small numbers
            (when (< (BigInteger''bitLength result) BigInteger'SMALL_PRIME_THRESHOLD)
                ;; ensure an odd number
                (when (not (BigInteger'testBit result, 0))
                    (§ ass result (BigInteger'add result, BigInteger'ONE))
                )

                (§ while true
                    ;; do cheap "pre-test" if applicable
                    (when (< 6 (BigInteger''bitLength result))
                        (let [
                              #_"long" r (.longValue (BigInteger'remainder result, BigInteger'SMALL_PRIME_PRODUCT))
                        ]
                            (when (or (zero? (% r 3)) (zero? (% r 5)) (zero? (% r 7)) (zero? (% r 11)) (zero? (% r 13)) (zero? (% r 17)) (zero? (% r 19)) (zero? (% r 23)) (zero? (% r 29)) (zero? (% r 31)) (zero? (% r 37)) (zero? (% r 41)))
                                (§ ass result (BigInteger'add result, BigInteger'TWO))
                                (§ continue) ;; candidate is composite; try another
                            )
                        )
                    )

                    ;; all candidates of bitLength 2 and 3 are prime by this point
                    (when (< (BigInteger''bitLength result) 4)
                        (§ return result)
                    )

                    ;; the expensive test
                    (when (BigInteger''primeToCertainty result, BigInteger'DEFAULT_PRIME_CERTAINTY, nil)
                        (§ return result)
                    )

                    (§ ass result (BigInteger'add result, BigInteger'TWO))
                )
            )

            ;; start at previous even number
            (when (BigInteger'testBit result, 0)
                (§ ass result (BigInteger'subtract result, BigInteger'ONE))
            )

            ;; looking for the next large prime
            (let [
                  #_"int" searchLen (BigInteger'getPrimeSearchLen (BigInteger''bitLength result))
            ]
                (§ while true
                    (let [
                          #_"BitSieve" searchSieve (BitSieve'new-2 result, searchLen)
                          #_"BigInteger" candidate (BitSieve''retrieve searchSieve, result, BigInteger'DEFAULT_PRIME_CERTAINTY, nil)
                    ]
                        (when-not (nil? candidate)
                            (§ return candidate)
                        )
                        (§ ass result (BigInteger'add result, (BigInteger'valueOf-l (* 2 searchLen))))
                    )
                )
            )
        )
    )
)

;;;
 ; A simple bit sieve used for finding prime number candidates. Allows setting
 ; and clearing of bits in a storage array. The size of the sieve is assumed to
 ; be constant to reduce overhead. All the bits of a new bitSieve are zero, and
 ; bits are removed from it by setting them.
 ;
 ; To reduce storage space and increase efficiency, no even numbers are
 ; represented in the sieve (each bit in the sieve represents an odd number).
 ; The relationship between the index of a bit and the number it represents is
 ; given by
 ;
 ; N = offset + (2 * index + 1);
 ;
 ; Where N is the integer represented by a bit in the sieve, offset is some
 ; even integer offset indicating where the sieve begins, and index is the
 ; index of a bit in the sieve array.
 ;;

(class-ns BitSieve
    ;;;
     ; Stores the bits in this bitSieve.
     ;;
    (§ field- #_"long[]" :bits)

    ;;;
     ; Length is how many bits this sieve holds.
     ;;
    (§ field- #_"int" :length)

    ;;;
     ; Get the value of the bit at the specified index.
     ;;
    #_method
    (defn- #_"boolean" BitSieve''get [#_"BitSieve" this, #_"int" i]
        (not (zero? (& (aget (:bits this) (>>> i 6)) (<< 1 (& i 63)))))
    )

    ;;;
     ; Set the bit at the specified index.
     ;;
    #_method
    (defn- #_"void" BitSieve''set [#_"BitSieve" this, #_"int" i]
        (aswap (:bits this) (>>> i 6) | (<< 1 (& i 63)))
        nil
    )

    ;;;
     ; This method returns the index of the first clear bit in the search
     ; array that occurs at or after start. It will not search past the
     ; specified limit. It returns -1 if there is no such clear bit.
     ;;
    #_method
    (defn- #_"int" BitSieve''sieveSearch
        ([#_"BitSieve" this, #_"int" start] (BitSieve''sieveSearch this, start, (:length this)))
        ([#_"BitSieve" this, #_"int" start, #_"int" limit]
            (when (< start limit) => -1
                (loop-when [#_"int" i start] (BitSieve''get this, i) => i
                    (let [i (inc i)]
                        (recur-if (< i (dec limit)) [i] => -1)
                    )
                )
            )
        )
    )

    ;;;
     ; Sieve a single set of multiples out of the sieve. Begin to remove
     ; multiples of the specified step starting at the specified start index,
     ; up to the specified limit.
     ;;
    #_method
    (defn- #_"void" BitSieve''sieveSingle
        ([#_"BitSieve" this, #_"int" start, #_"int" step] (BitSieve''sieveSingle this, start, step, (:length this)))
        ([#_"BitSieve" this, #_"int" start, #_"int" step, #_"int" limit]
            (loop-when-recur start (< start limit) (+ start step)
                (BitSieve''set this, start)
            )
            nil
        )
    )

    ;;;
     ; Construct a "small sieve" with a base of 0. This constructor is
     ; used internally to generate the set of "small primes" whose multiples
     ; are excluded from sieves generated by the main (package private)
     ; constructor, BitSieve(BigInteger base, int searchLen). The length
     ; of the sieve generated by this constructor was chosen for performance;
     ; it controls a tradeoff between how much time is spent constructing
     ; other sieves, and how much time is wasted testing composite candidates
     ; for primality. The length was chosen experimentally to yield good
     ; performance.
     ;;
    (defn- #_"BitSieve" BitSieve'new-0 []
        (let [this (§ new)
              this (assoc this :length (<< 150 6))
              this (assoc this :bits (long-array (inc (>>> (dec (:length this)) 6))))]
            ;; mark 1 as composite
            (BitSieve''set this, 0)
            ;; find primes and remove their multiples from sieve
            (loop [#_"int" i 1 #_"int" p 3]
                (BitSieve''sieveSingle this, (+ i p), p)
                (let [i (BitSieve''sieveSearch this, (inc i)) p (inc (* i 2))]
                    (recur-if (and (pos? i) (< p (:length this))) [i p])
                )
            )
            this
        )
    )

    ;;;
     ; A small sieve used to filter out multiples of small primes in a search sieve.
     ;;
    (def- #_"BitSieve" BitSieve'smallSieve (BitSieve'new-0))

    ;;;
     ; Construct a bit sieve of length bits used for finding prime number candidates.
     ; The new sieve begins at the specified base, which must be even.
     ;
     ; Candidates are indicated by clear bits in the sieve. As a candidates nonprimality
     ; is calculated, a bit is set in the sieve to eliminate it. To reduce storage space
     ; and increase efficiency, no even numbers are represented in the sieve (each bit
     ; in the sieve represents an odd number).
     ;;
    (defn #_"BitSieve" BitSieve'new-2 [#_"BigInteger" base, #_"int" length]
        (let [this (§ new)
              this (assoc this :bits (long-array (inc (>>> (dec length) 6))))
              this (assoc this :length length)
              ;; construct the large sieve at an even offset specified by base
              #_"MutableBigInteger" b (MutableBigInteger'fromBigInteger base)]
            (loop [#_"int" i (BitSieve''sieveSearch BitSieve'smallSieve, 0)]
                (let [#_"int" p (inc (* i 2))
                      ;; calculate base mod p
                      [_ #_"int" r] (MutableBigInteger'divideOneWord b, p)
                      ;; take each multiple of i out of sieve
                      r (- p r) r (if (zero? (% r 2)) (+ r p) r)]
                    (BitSieve''sieveSingle this, (quot (dec r) 2), p)
                    ;; find next prime from small sieve
                    (let [i (BitSieve''sieveSearch BitSieve'smallSieve, (inc i))]
                        (recur-if (pos? i) [i])
                    )
                )
            )
            this
        )
    )

    ;;;
     ; Test probable primes in the sieve and return successful candidates.
     ;;
    #_method
    (defn #_"BigInteger" BitSieve''retrieve [#_"BitSieve" this, #_"BigInteger" base, #_"int" certainty, #_"Random" rnd]
        ;; examine the sieve one long at a time to find possible primes
        (let [#_"int" units (alength (:bits this))]
            (loop-when [#_"int" i 0] (< i units)
                (let [#_"BigInteger" p
                        (loop-when [#_"long" unit (bit-not (aget (:bits this) i)) #_"int" k (inc (* i 64)) #_"int" j 0] (< j 64)
                            (let [p (when (== (& unit 1) 1)
                                        (let [p (BigInteger'add base, (BigInteger'valueOf-l k))]
                                            (when (BigInteger''primeToCertainty p, certainty, rnd)
                                                p
                                            )
                                        )
                                    )]
                                (recur-if (nil? p) [(>>> unit 1) (+ k 2) (inc j)] => p)
                            )
                        )]
                    (recur-if (nil? p) [(inc i)] => p)
                )
            )
        )
    )
)
)
