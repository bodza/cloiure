(ns cloiure.ratio
    (:refer-clojure :only [* *ns* + - -> / < <= = == > >= aclone aget alength and aset assoc bit-and bit-not bit-or bit-shift-left bit-shift-right bit-xor byte byte-array case cast complement cond cons dec declare defmacro defn dotimes first identical? if-not import inc instance? int int-array into-array let letfn long long-array loop make-array map max min neg? next nil? not or pos? quot rem second some? symbol? unsigned-bit-shift-right update vary-meta vec vector? while zero?])
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
     ; *big-endian* byte-order: the most significant byte is in the zeroth element.
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
     ; to be in *big-endian* int-order: the most significant int is in the zeroth element.
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
     ; most significant byte is in the zeroth element. A zero-length magnitude array is
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
    (def- #_"BigInteger" BigInteger'NEGATIVE_ONE (§ soon BigInteger'valueOf-l -1))

    ;; comparison operations

    ;;;
     ; Compares the magnitude array of this BigInteger with of 'that'.
     ; This is the version of compareTo ignoring sign.
     ;
     ; @return -1, 0 or 1 as this magnitude array is less than, equal
     ;         to or greater than the magnitude array for 'that'.
     ;;
    (defn #_"int" BigInteger'compareMagnitude-i [#_"BigInteger" this, #_"BigInteger" that]
        (let [#_"int[]" a* (:mag this) #_"int" n (alength a*)
              #_"int[]" b* (:mag that) #_"int" m (alength b*)]
            (cond (< n m) -1 (< m n) 1
                :else
                (loop-when [#_"int" i 0] (< i n) => 0
                    (let [#_"int" a (aget a* i) #_"int" b (aget b* i)]
                        (recur-if (== a b) [(inc i)] => (if (< (long! a) (long! b)) -1 1))
                    )
                )
            )
        )
    )

    ;;;
     ; Version of compareMagnitude that compares magnitude with long value.
     ; x can't be Long/MIN_VALUE.
     ;;
    (defn #_"int" BigInteger'compareMagnitude-l [#_"BigInteger" this, #_"long" x]
        (when-not (== x Long/MIN_VALUE) => (throw! "(== x Long/MIN_VALUE)")
            (let-when [#_"int[]" a* (:mag this) #_"int" n (alength a*)] (<= n 2) => 1
                (let [x (if (neg? x) (- x) x) #_"int" y (int (>>> x 32))]
                    (if (zero? y)
                        (cond (< n 1) -1 (< 1 n) 1
                            :else
                            (let-when [#_"int" a (aget a* 0) #_"int" b (int x)] (== a b) => (if (< (long! a) (long! b)) -1 1)
                                0
                            )
                        )
                        (when (== n 2) => -1
                            (let-when [#_"int" a (aget a* 0) #_"int" b      y ] (== a b) => (if (< (long! a) (long! b)) -1 1)
                                (let-when [    a (aget a* 1)         b (int x)] (== a b) => (if (< (long! a) (long! b)) -1 1)
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
        (let-when [#_"int" a (:signum this) #_"int" b (:signum that)] (== a b) => (if (< a b) -1 1)
            (case a
                -1 (BigInteger'compareMagnitude-i that, this)
                1 (BigInteger'compareMagnitude-i this, that)
                0
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

    (defn- #_"int[]" BigInteger'subtract-la [#_"long" val, #_"int[]" little]
        (let [
              #_"int" highWord (int (>>> val 32))
        ]
            (cond (zero? highWord)
                (let [
                      #_"int[]" result (int-array 1)
                ]
                    (aset result 0 (int (- val (long! (aget little 0)))))
                    (§ return result)
                )
                :else
                (let [
                      #_"int[]" result (int-array 2)
                ]
                    (cond (== (alength little) 1)
                        (let [
                              #_"long" difference (- (long! (int val)) (long! (aget little 0)))
                        ]
                            (aset result 1 (int difference))
                            ;; subtract remainder of longer number while borrow propagates
                            (let [
                                  #_"boolean" borrow (not (zero? (>> difference 32)))
                            ]
                                (cond borrow
                                    (do
                                        (aset result 0 (dec highWord))
                                    )
                                    :else ;; copy remainder of longer number
                                    (do
                                        (aset result 0 highWord)
                                    )
                                )
                                (§ return result)
                            )
                        )
                        :else ;; little.length == 2
                        (let [
                              #_"long" difference (- (long! (int val)) (long! (aget little 1)))
                        ]
                            (aset result 1 (int difference))
                            (§ ass difference (+ (- (long! highWord) (long! (aget little 0))) (>> difference 32)))
                            (aset result 0 (int difference))
                            (§ return result)
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Subtracts the contents of the second argument (val) from the first (big).
     ; The first int array (big) must represent a larger number than the second.
     ; This method allocates the space necessary to hold the answer.
     ; assumes val >= 0
     ;;
    (defn- #_"int[]" BigInteger'subtract-al [#_"int[]" big, #_"long" val]
        (let [
              #_"int" highWord (int (>>> val 32))
              #_"int" bigIndex (alength big)
              #_"int[]" result (int-array bigIndex)
              #_"long" difference 0
        ]
            (cond (zero? highWord)
                (do
                    (§ ass bigIndex (dec bigIndex))
                    (§ ass difference (- (long! (aget big bigIndex)) val))
                    (aset result bigIndex (int difference))
                )
                :else
                (do
                    (§ ass bigIndex (dec bigIndex))
                    (§ ass difference (- (long! (aget big bigIndex)) (long! val)))
                    (aset result bigIndex (int difference))
                    (§ ass bigIndex (dec bigIndex))
                    (§ ass difference (+ (- (long! (aget big bigIndex)) (long! highWord)) (>> difference 32)))
                    (aset result bigIndex (int difference))
                )
            )

            ;; subtract remainder of longer number while borrow propagates
            (let [
                  #_"boolean" borrow (not (zero? (>> difference 32)))
            ]
                (while (and (pos? bigIndex) borrow)
                    (§ ass bigIndex (dec bigIndex))
                    (aset result bigIndex (dec (aget big bigIndex)))
                    (§ ass borrow (== (aget result bigIndex) -1))
                )

                ;; copy remainder of longer number
                (while (pos? bigIndex)
                    (§ ass bigIndex (dec bigIndex))
                    (aset result bigIndex (aget big bigIndex))
                )

                result
            )
        )
    )

    ;;;
     ; Subtracts the contents of the second int arrays (little) from the first (big).
     ; The first int array (big) must represent a larger number than the second.
     ; This method allocates the space necessary to hold the answer.
     ;;
    (defn- #_"int[]" BigInteger'subtract-aa [#_"int[]" big, #_"int[]" little]
        (let [
              #_"int" bigIndex (alength big)
              #_"int[]" result (int-array bigIndex)
              #_"int" littleIndex (alength little)
              #_"long" difference 0
        ]
            ;; subtract common parts of both numbers
            (while (pos? littleIndex)
                (§ ass bigIndex (dec bigIndex))
                (§ ass littleIndex (dec littleIndex))
                (§ ass difference (+ (- (long! (aget big bigIndex)) (long! (aget little littleIndex))) (>> difference 32)))
                (aset result bigIndex (int difference))
            )

            ;; subtract remainder of longer number while borrow propagates
            (let [
                  #_"boolean" borrow (not (zero? (>> difference 32)))
            ]
                (while (and (pos? bigIndex) borrow)
                    (§ ass bigIndex (dec bigIndex))
                    (aset result bigIndex (dec (aget big bigIndex)))
                    (§ ass borrow (== (aget result bigIndex) -1))
                )

                ;; copy remainder of longer number
                (while (pos? bigIndex)
                    (§ ass bigIndex (dec bigIndex))
                    (aset result bigIndex (aget big bigIndex))
                )

                result
            )
        )
    )

    ;;;
     ; Adds the contents of the int arrays x and y. This method allocates a
     ; new int array to hold the answer and returns a reference to that array.
     ;;
    (defn- #_"int[]" BigInteger'add [#_"int[]" x, #_"int[]" y]
        ;; if x is shorter, swap the two arrays
        (when (< (alength x) (alength y))
            (let [
                  #_"int[]" tmp x
            ]
                (§ ass x y)
                (§ ass y tmp)
            )
        )

        (let [
              #_"int" xIndex (alength x)
              #_"int" yIndex (alength y)
              #_"int[]" result (int-array xIndex)
              #_"long" sum 0
        ]
            (cond (== yIndex 1)
                (do
                    (§ ass xIndex (dec xIndex))
                    (§ ass sum (+ (long! (aget x xIndex)) (long! (aget y 0))))
                    (aset result xIndex (int sum))
                )
                :else
                (do
                    ;; add common parts of both numbers
                    (while (pos? yIndex)
                        (§ ass xIndex (dec xIndex))
                        (§ ass yIndex (dec yIndex))
                        (§ ass sum (+ (long! (aget x xIndex)) (long! (aget y yIndex)) (>>> sum 32)))
                        (aset result xIndex (int sum))
                    )
                )
            )
            ;; copy remainder of longer number while carry propagation is required
            (let [
                  #_"boolean" carry (not (zero? (>>> sum 32)))
            ]
                (while (and (pos? xIndex) carry)
                    (§ ass xIndex (dec xIndex))
                    (aset result xIndex (inc (aget x xIndex)))
                    (§ ass carry (zero? (aget result xIndex)))
                )

                ;; copy remainder of longer number
                (while (pos? xIndex)
                    (§ ass xIndex (dec xIndex))
                    (aset result xIndex (aget x xIndex))
                )

                ;; grow result if necessary
                (when carry
                    (let [
                          #_"int[]" bigger (int-array (inc (alength result)))
                    ]
                        (System/arraycopy result, 0, bigger, 1, (alength result))
                        (aset bigger 0 0x01)
                        (§ return bigger)
                    )
                )
                result
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (+ this val).
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''add [#_"BigInteger" this, #_"BigInteger" val]
        (when (zero? (:signum val))
            (§ return this)
        )
        (when (zero? (:signum this))
            (§ return val)
        )
        (when (== (:signum val) (:signum this))
            (§ return (BigInteger'new-2ai (BigInteger'add (:mag this), (:mag val)), (:signum this)))
        )

        (let [
              #_"int" cmp (BigInteger'compareMagnitude-i this, val)
        ]
            (when (zero? cmp)
                (§ return BigInteger'ZERO)
            )
            (let [
                  #_"int[]" resultMag (if (pos? cmp) (BigInteger'subtract-aa (:mag this), (:mag val)) (BigInteger'subtract-aa (:mag val), (:mag this)))
                  _ (§ ass resultMag (BigInteger'trustedStripLeadingZeroInts resultMag))
            ]
                (BigInteger'new-2ai resultMag, (if (== cmp (:signum this)) 1 -1))
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (- this).
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''negate [#_"BigInteger" this]
        (BigInteger'new-2ai (:mag this), (- (:signum this)))
    )

    ;;;
     ; Returns a BigInteger whose value is (- this val).
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''subtract [#_"BigInteger" this, #_"BigInteger" val]
        (when (zero? (:signum val))
            (§ return this)
        )
        (when (zero? (:signum this))
            (§ return (BigInteger''negate val))
        )
        (when (not (== (:signum val) (:signum this)))
            (§ return (BigInteger'new-2ai (BigInteger'add (:mag this), (:mag val)), (:signum this)))
        )

        (let [
              #_"int" cmp (BigInteger'compareMagnitude-i this, val)
        ]
            (when (zero? cmp)
                (§ return BigInteger'ZERO)
            )
            (let [
                  #_"int[]" resultMag (if (pos? cmp) (BigInteger'subtract-aa (:mag this), (:mag val)) (BigInteger'subtract-aa (:mag val), (:mag this)))
                  _ (§ ass resultMag (BigInteger'trustedStripLeadingZeroInts resultMag))
            ]
                (BigInteger'new-2ai resultMag, (if (== cmp (:signum this)) 1 -1))
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is the absolute value of this BigInteger.
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''abs [#_"BigInteger" this]
        (if (neg? (:signum this)) (BigInteger''negate this) this)
    )

    ;;;
     ; Returns the signum function of this BigInteger.
     ;
     ; @return -1, 0 or 1 as the value of this BigInteger is negative, zero or positive.
     ;;
    #_method
    (defn #_"int" BigInteger''signum [#_"BigInteger" this]
        (:signum this)
    )

    (defn- #_"BigInteger" BigInteger'multiplyByInt [#_"int[]" x, #_"int" y, #_"int" sign]
        (when (== (Integer/bitCount y) 1)
            (§ return (BigInteger'new-2ai (BigInteger'shiftLeft x, (Integer/numberOfTrailingZeros y)), sign))
        )
        (let [
              #_"int" xlen (alength x)
              #_"int[]" rmag (int-array (inc xlen))
              #_"long" carry 0
              #_"long" yl (long! y)
              #_"int" rstart (- (alength rmag) 1)
        ]
            (loop-when-recur [#_"int" i (dec xlen)] (<= 0 i) [(dec i)]
                (let [
                      #_"long" product (+ (* (long! (aget x i)) yl) carry)
                ]
                    (aset rmag rstart (int product))
                    (§ ass rstart (dec rstart))
                    (§ ass carry (>>> product 32))
                )
            )
            (cond (zero? carry)
                (do
                    (§ ass rmag (Arrays/copyOfRange rmag, 1, (alength rmag)))
                )
                :else
                (do
                    (aset rmag rstart (int carry))
                )
            )
            (BigInteger'new-2ai rmag, sign)
        )
    )

    ;;;
     ; Multiplies int arrays x and y to the specified lengths and places the
     ; result into z. There will be no leading zeros in the resultant array.
     ;;
    #_method
    (defn- #_"int[]" BigInteger''multiplyToLen [#_"BigInteger" this, #_"int[]" x, #_"int" xlen, #_"int[]" y, #_"int" ylen, #_"int[]" z]
        (let [
              #_"int" xstart (dec xlen)
              #_"int" ystart (dec ylen)
        ]
            (when (or (nil? z) (< (alength z) (+ xlen ylen)))
                (§ ass z (int-array (+ xlen ylen)))
            )

            (let [
                  #_"long" carry 0
            ]
                (loop-when-recur [#_"int" j ystart #_"int" k (+ ystart 1 xstart)] (<= 0 j) [(dec j) (dec k)]
                    (let [
                          #_"long" product (+ (* (long! (aget y j)) (long! (aget x xstart))) carry)
                    ]
                        (aset z k (int product))
                        (§ ass carry (>>> product 32))
                    )
                )
                (aset z xstart (int carry))

                (loop-when-recur [#_"int" i (dec xstart)] (<= 0 i) [(dec i)]
                    (let [
                    ]
                        (§ ass carry 0)
                        (loop-when-recur [#_"int" j ystart #_"int" k (+ ystart 1 i)] (<= 0 j) [(dec j) (dec k)]
                            (let [
                                  #_"long" product (+ (* (long! (aget y j)) (long! (aget x i))) (long! (aget z k)) carry)
                            ]
                                (aset z k (int product))
                                (§ ass carry (>>> product 32))
                            )
                        )
                        (aset z i (int carry))
                    )
                )
                z
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (* this val).
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''multiply [#_"BigInteger" this, #_"BigInteger" val]
        (when (or (zero? (:signum val)) (zero? (:signum this)))
            (§ return BigInteger'ZERO)
        )

        (let [
              #_"int" xlen (alength (:mag this))
              #_"int" ylen (alength (:mag val))
              #_"int" resultSign (if (== (:signum this) (:signum val)) 1 -1)
        ]
            (when (== (alength (:mag val)) 1)
                (§ return (BigInteger'multiplyByInt (:mag this), (aget (:mag val) 0), resultSign))
            )
            (when (== (alength (:mag this)) 1)
                (§ return (BigInteger'multiplyByInt (:mag val), (aget (:mag this) 0), resultSign))
            )
            (let [
                  #_"int[]" result (BigInteger''multiplyToLen this, (:mag this), xlen, (:mag val), ylen, nil)
                  _ (§ ass result (BigInteger'trustedStripLeadingZeroInts result))
            ]
                (BigInteger'new-2ai result, resultSign)
            )
        )
    )

    ;; squaring

    (declare BigInteger'mulAdd)
    (declare BigInteger'addOne)
    (declare BigInteger'primitiveLeftShift)

    ;;;
     ; Squares the contents of the int array x. The result is placed into the
     ; int array z. The contents of x are not changed.
     ;
     ; The algorithm used here is adapted from Colin Plumb's C library.
     ; Technique: Consider the partial products in the multiplication
     ; of "abcde" by itself:
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
     ;              ae be ce de = (abcd) * e
     ;           ad bd cd       = (abc) * d
     ;        ac bc             = (ab) * c
     ;     ab                   = (a) * b
     ;
     ; is a copy of everything below the main diagonal:
     ;                       de
     ;                 cd ce
     ;           bc bd be
     ;     ab ac ad ae
     ;
     ; Thus, the sum is 2 * (off the diagonal) + diagonal.
     ;
     ; This is accumulated beginning with the diagonal (which
     ; consist of the squares of the digits of the input), which is then
     ; divided by two, the off-diagonal added, and multiplied by two
     ; again. The low bit is simply a copy of the low bit of the
     ; input, so it doesn't need special care.
     ;;
    (defn- #_"int[]" BigInteger'squareToLen [#_"int[]" x, #_"int" len, #_"int[]" z]
        (let [
              #_"int" zlen (<< len 1)
        ]
            (when (or (nil? z) (< (alength z) zlen))
                (§ ass z (int-array zlen))
            )

            ;; store the squares, right shifted one bit (i.e., divided by 2)
            (let [
                  #_"int" lastProductLowWord 0
            ]
                (loop-when-recur [#_"int" i 0 #_"int" j 0] (< j len) [i (inc j)]
                    (let [
                          #_"long" piece (long! (aget x j))
                          #_"long" product (* piece piece)
                    ]
                        (aset z i (| (<< lastProductLowWord 31) (int (>>> product 33))))
                        (§ ass i (inc i))
                        (aset z i (int (>>> product 1)))
                        (§ ass i (inc i))
                        (§ ass lastProductLowWord (int product))
                    )
                )

                ;; add in off-diagonal sums
                (loop-when-recur [#_"int" i len #_"int" offset 1] (pos? i) [(dec i) (+ offset 2)]
                    (let [
                          #_"int" t (aget x (dec i))
                    ]
                        (§ ass t (BigInteger'mulAdd z, x, offset, (dec i), t))
                        (BigInteger'addOne z, (dec offset), i, t)
                    )
                )

                ;; shift back up and set low bit
                (BigInteger'primitiveLeftShift z, zlen, 1)
                (aswap z (dec zlen) | (& (aget x (dec len)) 1))

                z
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this<sup>2</sup>)}.
     ;
     ; @return {@code this<sup>2</sup>}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''square [#_"BigInteger" this]
        (when (zero? (:signum this))
            (§ return BigInteger'ZERO)
        )

        (let [
              #_"int[]" z (BigInteger'squareToLen (:mag this), (alength (:mag this)), nil)
        ]
            (BigInteger'new-2ai (BigInteger'trustedStripLeadingZeroInts z), 1)
        )
    )

    ;; division

    ;;;
     ; Returns a BigInteger whose value is {@code (this / val)} using an O(n^2) algorithm from Knuth.
     ;
     ; @param  val value by which this BigInteger is to be divided.
     ; @return {@code this / val}
     ; @throws ArithmeticException if {@code val} is zero.
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''divide [#_"BigInteger" this, #_"BigInteger" val]
        (let [
              #_"MutableBigInteger" a (MutableBigInteger'new-a (:mag this))
              #_"MutableBigInteger" b (MutableBigInteger'new-a (:mag val))
              [#_"MutableBigInteger" q _] (MutableBigInteger'divide a, b)
        ]
            (MutableBigInteger'toBigInteger q, (* (:signum this) (:signum val)))
        )
    )

    ;;;
     ; Returns an array of two BigIntegers containing {@code (this / val)} followed by {@code (this % val)}.
     ;
     ; @param  val value by which this BigInteger is to be divided, and the remainder computed.
     ; @return an array of two BigIntegers:
     ;         the quotient {@code (this / val)} is the initial element,
     ;         and the remainder {@code (this % val)} is the final element.
     ; @throws ArithmeticException if {@code val} is zero.
     ;;
    #_method
    (defn #_"BigInteger[]" BigInteger''divideAndRemainder [#_"BigInteger" this, #_"BigInteger" val]
        (let [
              #_"MutableBigInteger" a (MutableBigInteger'new-a (:mag this))
              #_"MutableBigInteger" b (MutableBigInteger'new-a (:mag val))
              [#_"MutableBigInteger" q #_"MutableBigInteger" r] (MutableBigInteger'divide a, b)
              #_"BigInteger[]" result (§ soon make-array BigInteger 2)
        ]
            (aset result 0 (MutableBigInteger'toBigInteger q, (if (== (:signum this) (:signum val)) 1 -1)))
            (aset result 1 (MutableBigInteger'toBigInteger r, (:signum this)))
            result
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this % val)}.
     ;
     ; @param  val value by which this BigInteger is to be divided, and the remainder computed.
     ; @return {@code this % val}
     ; @throws ArithmeticException if {@code val} is zero.
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''remainder [#_"BigInteger" this, #_"BigInteger" val]
        (let [
              #_"MutableBigInteger" a (MutableBigInteger'new-a (:mag this))
              #_"MutableBigInteger" b (MutableBigInteger'new-a (:mag val))
              [_ #_"MutableBigInteger" r] (MutableBigInteger'divide a, b)
        ]
            (MutableBigInteger'toBigInteger r, (:signum this))
        )
    )

    ;; bitwise operations

    ;;;
     ; Returns a BigInteger whose value is {@code (this & val)}.
     ; (This method returns a negative BigInteger if and only if this and val are both negative.)
     ;
     ; @param val value to be AND'ed with this BigInteger.
     ; @return {@code this & val}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''and [#_"BigInteger" this, #_"BigInteger" val]
        (let [
              #_"int[]" result (int-array (max (BigInteger''intLength this) (BigInteger''intLength val)))
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength result)) [(inc i)]
                (aset result i (& (BigInteger''getInt this, (- (alength result) i 1)) (BigInteger''getInt val, (- (alength result) i 1))))
            )

            (BigInteger'valueOf-a result)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this | val)}.
     ; (This method returns a negative BigInteger if and only if either this or val is negative.)
     ;
     ; @param val value to be OR'ed with this BigInteger.
     ; @return {@code this | val}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''or [#_"BigInteger" this, #_"BigInteger" val]
        (let [
              #_"int[]" result (int-array (max (BigInteger''intLength this) (BigInteger''intLength val)))
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength result)) [(inc i)]
                (aset result i (| (BigInteger''getInt this, (- (alength result) i 1)) (BigInteger''getInt val, (- (alength result) i 1))))
            )

            (BigInteger'valueOf-a result)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this ^ val)}.
     ; (This method returns a negative BigInteger if and only if exactly one of this and val are negative.)
     ;
     ; @param val value to be XOR'ed with this BigInteger.
     ; @return {@code this ^ val}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''xor [#_"BigInteger" this, #_"BigInteger" val]
        (let [
              #_"int[]" result (int-array (max (BigInteger''intLength this) (BigInteger''intLength val)))
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength result)) [(inc i)]
                (aset result i (bit-xor (BigInteger''getInt this, (- (alength result) i 1)) (BigInteger''getInt val, (- (alength result) i 1))))
            )

            (BigInteger'valueOf-a result)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (~this)}.
     ; (This method returns a negative value if and only if this BigInteger is non-negative.)
     ;
     ; @return {@code ~this}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''not [#_"BigInteger" this]
        (let [
              #_"int[]" result (int-array (BigInteger''intLength this))
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength result)) [(inc i)]
                (aset result i (bit-not (BigInteger''getInt this, (- (alength result) i 1))))
            )

            (BigInteger'valueOf-a result)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this & ~val)}.
     ; This method, which is equivalent to {@code and(val.not())}, is provided as a
     ; convenience for masking operations. (This method returns a negative BigInteger
     ; if and only if {@code this} is negative and {@code val} is positive.)
     ;
     ; @param val value to be complemented and AND'ed with this BigInteger.
     ; @return {@code this & ~val}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''andNot [#_"BigInteger" this, #_"BigInteger" val]
        (let [
              #_"int[]" result (int-array (max (BigInteger''intLength this) (BigInteger''intLength val)))
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength result)) [(inc i)]
                (aset result i (& (BigInteger''getInt this, (- (alength result) i 1)) (bit-not (BigInteger''getInt val, (- (alength result) i 1)))))
            )

            (BigInteger'valueOf-a result)
        )
    )

    ;; single bit operations

    ;;;
     ; Returns {@code true} if and only if the designated bit is set.
     ; (Computes {@code ((this & (1<<n)) != 0)}.)
     ;
     ; @param  n index of bit to test.
     ; @return {@code true} if and only if the designated bit is set.
     ; @throws ArithmeticException {@code n} is negative.
     ;;
    #_method
    (defn #_"boolean" BigInteger''testBit [#_"BigInteger" this, #_"int" n]
        (when (neg? n)
            (throw! "negative bit address")
        )

        (not (zero? (& (BigInteger''getInt this, (>>> n 5)) (<< 1 (& n 31)))))
    )

    ;;;
     ; Returns a BigInteger whose value is equivalent to this BigInteger
     ; with the designated bit set. (Computes {@code (this | (1<<n))}.)
     ;
     ; @param  n index of bit to set.
     ; @return {@code this | (1<<n)}
     ; @throws ArithmeticException {@code n} is negative.
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''setBit [#_"BigInteger" this, #_"int" n]
        (when (neg? n)
            (throw! "negative bit address")
        )

        (let [
              #_"int" intNum (>>> n 5)
              #_"int[]" result (int-array (max (BigInteger''intLength this) (+ intNum 2)))
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength result)) [(inc i)]
                (aset result (- (alength result) i 1) (BigInteger''getInt this, i))
            )

            (aswap result (- (alength result) intNum 1) | (<< 1 (& n 31)))

            (BigInteger'valueOf-a result)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is equivalent to this BigInteger with the designated bit cleared.
     ; (Computes {@code (this & ~(1<<n))}.)
     ;
     ; @param  n index of bit to clear.
     ; @return {@code this & ~(1<<n)}
     ; @throws ArithmeticException {@code n} is negative.
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''clearBit [#_"BigInteger" this, #_"int" n]
        (when (neg? n)
            (throw! "negative bit address")
        )

        (let [
              #_"int" intNum (>>> n 5)
              #_"int[]" result (int-array (max (BigInteger''intLength this) (inc (>>> (inc n) 5))))
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength result)) [(inc i)]
                (aset result (- (alength result) i 1) (BigInteger''getInt this, i))
            )

            (aswap result (- (alength result) intNum 1) & (bit-not (<< 1 (& n 31))))

            (BigInteger'valueOf-a result)
        )
    )

    ;;;
     ; Returns a BigInteger whose value is equivalent to this BigInteger with the designated bit flipped.
     ; (Computes {@code (this ^ (1<<n))}.)
     ;
     ; @param  n index of bit to flip.
     ; @return {@code this ^ (1<<n)}
     ; @throws ArithmeticException {@code n} is negative.
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''flipBit [#_"BigInteger" this, #_"int" n]
        (when (neg? n)
            (throw! "negative bit address")
        )

        (let [
              #_"int" intNum (>>> n 5)
              #_"int[]" result (int-array (max (BigInteger''intLength this) (+ intNum 2)))
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength result)) [(inc i)]
                (aset result (- (alength result) i 1) (BigInteger''getInt this, i))
            )

            (aswap result (- (alength result) intNum 1) bit-xor (<< 1 (& n 31)))

            (BigInteger'valueOf-a result)
        )
    )

    ;; shift operations

    (defn- #_"int[]" BigInteger'javaIncrement [#_"int[]" val]
        (let [
              #_"int" lastSum 0
        ]
            (loop-when-recur [#_"int" i (dec (alength val))] (and (<= 0 i) (zero? lastSum)) [(dec i)]
                (aswap val i inc)
                (§ ass lastSum (aget val i))
            )
            (when (zero? lastSum)
                (§ ass val (int-array (inc (alength val))))
                (aset val 0 1)
            )
            val
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this >> n)}.
     ; The shift distance, {@code n}, is considered unsigned.
     ; (Computes <tt>floor(this * 2<sup>-n</sup>)</tt>.)
     ;
     ; @param  n unsigned shift distance, in bits.
     ; @return {@code this >> n}
     ;;
    #_method
    (defn- #_"BigInteger" BigInteger''shiftRightImpl [#_"BigInteger" this, #_"int" n]
        (let [
              #_"int" nInts (>>> n 5)
              #_"int" nBits (& n 31)
              #_"int" magLen (alength (:mag this))
              #_"int[]" newMag nil
        ]
            ;; special case: entire contents shifted off the end
            (when (<= magLen nInts)
                (§ return (if (<= 0 (:signum this)) BigInteger'ZERO (aget BigInteger'negConst 1)))
            )

            (cond (zero? nBits)
                (let [
                      #_"int" newMagLen (- magLen nInts)
                ]
                    (§ ass newMag (Arrays/copyOf (:mag this), newMagLen))
                )
                :else
                (let [
                      #_"int" i 0
                      #_"int" highBits (>>> (aget (:mag this) 0) nBits)
                ]
                    (cond (not (zero? highBits))
                        (do
                            (§ ass newMag (int-array (- magLen nInts)))
                            (aset newMag i highBits)
                            (§ ass i (inc i))
                        )
                        :else
                        (do
                            (§ ass newMag (int-array (- magLen nInts 1)))
                        )
                    )

                    (let [
                          #_"int" nBits2 (- 32 nBits)
                          #_"int" j 0
                    ]
                        (while (< j (- magLen nInts 1))
                            (aset newMag i (| (<< (aget (:mag this) j) nBits2) (>>> (aget (:mag this) (inc j)) nBits)))
                            (§ ass i (inc i))
                            (§ ass j (inc j))
                        )
                    )
                )
            )

            (when (neg? (:signum this))
                ;; Find out whether any one-bits were shifted off the end.
                (let [
                      #_"int" j (- magLen nInts)
                      #_"boolean" onesLost false
                ]
                    (loop-when-recur [#_"int" i (dec magLen)] (and (<= j i) (not onesLost)) [(dec i)]
                        (§ ass onesLost (not (zero? (aget (:mag this) i))))
                    )
                    (when (and (not onesLost) (not (zero? nBits)))
                        (§ ass onesLost (not (zero? (<< (aget (:mag this) (- magLen nInts 1)) (- 32 nBits)))))
                    )

                    (when onesLost
                        (§ ass newMag (BigInteger'javaIncrement newMag))
                    )
                )
            )

            (BigInteger'new-2ai newMag, (:signum this))
        )
    )

    ;;;
     ; Returns a magnitude array whose value is {@code (mag << n)}.
     ; The shift distance, {@code n}, is considered unnsigned.
     ; (Computes <tt>this * 2<sup>n</sup></tt>.)
     ;
     ; @param mag magnitude, the most-significant int ({@code mag[0]}) must be non-zero.
     ; @param  n unsigned shift distance, in bits.
     ; @return {@code mag << n}
     ;;
    (defn- #_"int[]" BigInteger'shiftLeft [#_"int[]" mag, #_"int" n]
        (let [
              #_"int" nInts (>>> n 5)
              #_"int" nBits (& n 31)
              #_"int" magLen (alength mag)
              #_"int[]" newMag nil
        ]
            (cond (zero? nBits)
                (do
                    (§ ass newMag (int-array (+ magLen nInts)))
                    (System/arraycopy mag, 0, newMag, 0, magLen)
                )
                :else
                (let [
                      #_"int" i 0
                      #_"int" nBits2 (- 32 nBits)
                      #_"int" highBits (>>> (aget mag 0) nBits2)
                ]
                    (cond (not (zero? highBits))
                        (do
                            (§ ass newMag (int-array (+ magLen nInts 1)))
                            (aset newMag i highBits)
                            (§ ass i (inc i))
                        )
                        :else
                        (do
                            (§ ass newMag (int-array (+ magLen nInts)))
                        )
                    )
                    (let [
                          #_"int" j 0
                    ]
                        (while (< j (dec magLen))
                            (aset newMag i (| (<< (aget mag j) nBits) (>>> (aget mag (inc j)) nBits2)))
                            (§ ass i (inc i))
                            (§ ass j (inc j))
                        )
                        (aset newMag i (<< (aget mag j) nBits))
                    )
                )
            )
            newMag
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this << n)}.
     ; The shift distance, {@code n}, may be negative, in which case
     ; this method performs a right shift.
     ; (Computes <tt>floor(this * 2<sup>n</sup>)</tt>.)
     ;
     ; @param  n shift distance, in bits.
     ; @return {@code this << n}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''shiftLeft [#_"BigInteger" this, #_"int" n]
        (when (zero? (:signum this))
            (§ return BigInteger'ZERO)
        )
        (cond (pos? n)
            (do
                (§ return (BigInteger'new-2ai (BigInteger'shiftLeft (:mag this), n), (:signum this)))
            )
            (zero? n)
            (do
                (§ return this)
            )
            :else
            (do
                ;; Possible int overflow in (-n) is not a trouble,
                ;; because shiftRightImpl considers its argument unsigned.
                (§ return (BigInteger''shiftRightImpl this, (- n)))
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this >> n)}. Sign
     ; extension is performed. The shift distance, {@code n}, may be
     ; negative, in which case this method performs a left shift.
     ; (Computes <tt>floor(this / 2<sup>n</sup>)</tt>.)
     ;
     ; @param  n shift distance, in bits.
     ; @return {@code this >> n}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''shiftRight [#_"BigInteger" this, #_"int" n]
        (when (zero? (:signum this))
            (§ return BigInteger'ZERO)
        )
        (cond (pos? n)
            (do
                (BigInteger''shiftRightImpl this, n)
            )
            (zero? n)
            (do
                this
            )
            :else
            (do
                ;; Possible int overflow in {@code -n} is not a trouble,
                ;; because shiftLeft considers its argument unsigned.
                (BigInteger'new-2ai (BigInteger'shiftLeft (:mag this), (- n)), (:signum this))
            )
        )
    )

    ;; shifts a up to len right n bits assumes no leading zeros, 0 < n < 32
    (defn #_"void" BigInteger'primitiveRightShift [#_"int[]" a, #_"int" len, #_"int" n]
        (let [#_"int" m (- 32 n)]
            (loop-when [#_"int" i (dec len) #_"int" c (aget a i)] (pos? i)
                (let [#_"int" b c c (aget a (dec i))]
                    (aset a i (| (<< c m) (>>> b n)))
                    (recur (dec i) c)
                )
            )
            (aswap a 0 >>> n)
        )
        nil
    )

    ;; shifts a up to len left n bits assumes no leading zeros, 0 <= n < 32
    (defn #_"void" BigInteger'primitiveLeftShift [#_"int[]" a, #_"int" len, #_"int" n]
        (when (and (pos? len) (pos? n))
            (let [#_"int" m (- 32 n)]
                (loop-when [#_"int" i 0 #_"int" c (aget a i)] (< i (dec len))
                    (let [#_"int" b c c (aget a (inc i))]
                        (aset a i (| (<< b n) (>>> c m)))
                        (recur (inc i) c)
                    )
                )
                (aswap a (dec len) << n)
            )
        )
        nil
    )

    ;;;
     ; Left shift int array a up to n by shift bits. Returns the array that
     ; results from the shift since space may have to be reallocated.
     ;;
    (defn- #_"int[]" BigInteger'leftShift [#_"int[]" a, #_"int" n, #_"int" shift]
        (let [#_"int" nInts (>>> shift 5) #_"int" nBits (& shift 31) #_"int" mBits (BigInteger'bitLengthForInt (aget a 0))]
            ;; if shift can be done without recopy, do so
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
     ; Returns a BigInteger whose value is <tt>(this<sup>exponent</sup>)</tt>.
     ; Note that {@code exponent} is an integer rather than a BigInteger.
     ;
     ; @param  exponent exponent to which this BigInteger is to be raised.
     ; @return <tt>this<sup>exponent</sup></tt>
     ; @throws ArithmeticException {@code exponent} is negative. (This would
     ;         cause the operation to yield a non-integer value.)
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''pow [#_"BigInteger" this, #_"int" exponent]
        (when (neg? exponent)
            (throw! "negative exponent")
        )
        (when (zero? (:signum this))
            (§ return (if (zero? exponent) BigInteger'ONE this))
        )

        (let [
              #_"BigInteger" partToSquare (BigInteger''abs this)
              ;; Factor out powers of two from the base, as the exponentiation of these can be done by left shifts only.
              ;; The remaining part can then be exponentiated faster.
              ;; The powers of two will be multiplied back at the end.
              #_"int" powersOfTwo (BigInteger''getLowestSetBit partToSquare)
              #_"long" bitsToShift (* (long powersOfTwo) exponent)
        ]
            (when (< Integer/MAX_VALUE bitsToShift)
                (throw! "magnitude overflow")
            )

            (let [
                  #_"int" remainingBits (ß )
            ]

                ;; Factor the powers of two out quickly by shifting right, if needed.
                (cond (pos? powersOfTwo)
                    (let [
                    ]
                        (§ ass partToSquare (BigInteger''shiftRight partToSquare, powersOfTwo))
                        (§ ass remainingBits (BigInteger''bitLength partToSquare))
                        (when (== remainingBits 1) ;; Nothing left but +/- 1?
                            (cond (and (neg? (:signum this)) (== (& exponent 1) 1))
                                (do
                                    (§ return (BigInteger''shiftLeft BigInteger'NEGATIVE_ONE, (* powersOfTwo exponent)))
                                )
                                :else
                                (do
                                    (§ return (BigInteger''shiftLeft BigInteger'ONE, (* powersOfTwo exponent)))
                                )
                            )
                        )
                    )
                    :else
                    (let [
                    ]
                        (§ ass remainingBits (BigInteger''bitLength partToSquare))
                        (when (== remainingBits 1) ;; Nothing left but +/- 1?
                            (cond (and (neg? (:signum this)) (== (& exponent 1) 1))
                                (do
                                    (§ return BigInteger'NEGATIVE_ONE)
                                )
                                :else
                                (do
                                    (§ return BigInteger'ONE)
                                )
                            )
                        )
                    )
                )

                ;; This is a quick way to approximate the size of the result,
                ;; similar to doing log2[n] * exponent. This will give an upper bound
                ;; of how big the result can be, and which algorithm to use.
                (let [
                      #_"long" scaleFactor (* (long remainingBits) exponent)
                ]
                    ;; Use slightly different algorithms for small and large operands.
                    ;; See if the result will safely fit into a long. (Largest 2^63-1)
                    (cond (and (== (alength (:mag partToSquare)) 1) (<= scaleFactor 62))
                        ;; Small number algorithm. Everything fits into a long.
                        (let [
                              #_"int" newSign (if (and (neg? (:signum this)) (== (& exponent 1) 1)) -1 1)
                              #_"long" result 1
                              #_"long" baseToPow2 (long! (aget (:mag partToSquare) 0))
                              #_"int" workingExponent exponent
                        ]
                            ;; perform exponentiation using repeated squaring trick
                            (while (not (zero? workingExponent))
                                (when (== (& workingExponent 1) 1)
                                    (§ ass result (* result baseToPow2))
                                )

                                (§ ass workingExponent (>>> workingExponent 1))
                                (when (not (zero? workingExponent))
                                    (§ ass baseToPow2 (* baseToPow2 baseToPow2))
                                )
                            )

                            ;; multiply back the powers of two (quickly, by shifting left)
                            (cond (pos? powersOfTwo)
                                (do
                                    (cond (<= (+ bitsToShift scaleFactor) 62) ;; Fits in long?
                                        (do
                                            (§ return (BigInteger'valueOf-l (* (<< result bitsToShift) newSign)))
                                        )
                                        :else
                                        (do
                                            (§ return (-> (BigInteger'valueOf-l (* result newSign)) (BigInteger''shiftLeft (int bitsToShift))))
                                        )
                                    )
                                )
                                :else
                                (do
                                    (§ return (BigInteger'valueOf-l (* result newSign)))
                                )
                            )
                        )
                        :else
                        ;; Large number algorithm. This is basically identical to
                        ;; the algorithm above, but calls multiply() and square()
                        ;; which may use more efficient algorithms for large numbers.
                        (let [
                              #_"BigInteger" answer BigInteger'ONE
                              #_"int" workingExponent exponent
                        ]
                            ;; perform exponentiation using repeated squaring trick
                            (while (not (zero? workingExponent))
                                (when (== (& workingExponent 1) 1)
                                    (§ ass answer (BigInteger''multiply answer, partToSquare))
                                )

                                (§ ass workingExponent (>>> workingExponent 1))
                                (when (not (zero? workingExponent))
                                    (§ ass partToSquare (BigInteger''square partToSquare))
                                )
                            )
                            ;; multiply back the (exponentiated) powers of two (quickly, by shifting left)
                            (when (pos? powersOfTwo)
                                (§ ass answer (BigInteger''shiftLeft answer, (* powersOfTwo exponent)))
                            )

                            (cond (and (neg? (:signum this)) (== (& exponent 1) 1))
                                (do
                                    (§ return (BigInteger''negate answer))
                                )
                                :else
                                (do
                                    (§ return answer)
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is the greatest common divisor
     ; of {@code abs(this)} and {@code abs(val)}.
     ; Returns 0 if {@code this == 0 && val == 0}.
     ;
     ; @param  val value with which the GCD is to be computed.
     ; @return {@code GCD(abs(this), abs(val))}
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''gcd [#_"BigInteger" this, #_"BigInteger" val]
        (cond (zero? (:signum val))
            (do
                (§ return (BigInteger''abs this))
            )
            (zero? (:signum this))
            (do
                (§ return (BigInteger''abs val))
            )
        )

        (let [
              #_"MutableBigInteger" a (MutableBigInteger'fromBigInteger this)
              #_"MutableBigInteger" b (MutableBigInteger'fromBigInteger val)
              #_"MutableBigInteger" result (MutableBigInteger'hybridGCD a, b)
        ]
            (MutableBigInteger'toBigInteger result, 1)
        )
    )

    ;; modular arithmetic operations

    ;;;
     ; Returns a BigInteger whose value is {@code (this mod m}).
     ; This method differs from {@code remainder} in that it always returns
     ; a *non-negative* BigInteger.
     ;
     ; @param  m the modulus.
     ; @return {@code this mod m}
     ; @throws ArithmeticException {@code m} <= 0
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''mod [#_"BigInteger" this, #_"BigInteger" m]
        (when (<= (:signum m) 0)
            (throw! "modulus not positive")
        )

        (let [
              #_"BigInteger" result (BigInteger''remainder this, m)
        ]
            (if (<= 0 (:signum result)) result (BigInteger''add result, m))
        )
    )

    ;;;
     ; Multiply an array by one word k and add to result, return the carry.
     ;;
    (defn #_"int" BigInteger'mulAdd [#_"int[]" out, #_"int[]" in, #_"int" offset, #_"int" len, #_"int" k]
        (let [
              #_"long" kLong (long! k)
              #_"long" carry 0
        ]
            (§ ass offset (- (alength out) offset 1))
            (loop-when-recur [#_"int" j (dec len)] (<= 0 j) [(dec j)]
                (let [
                      #_"long" product (+ (* (long! (aget in j)) kLong) (long! (aget out offset)) carry)
                ]
                    (aset out offset (int product))
                    (§ ass offset (dec offset))
                    (§ ass carry (>>> product 32))
                )
            )
            (int carry)
        )
    )

    ;;;
     ; Add one word to the number a mlen words into a.
     ; Return the resulting carry.
     ;;
    (defn #_"int" BigInteger'addOne [#_"int[]" a, #_"int" offset, #_"int" mlen, #_"int" carry]
        (let [
              _ (§ ass offset (- (alength a) 1 mlen offset))
              #_"long" t (+ (long! (aget a offset)) (long! carry))
        ]
            (aset a offset (int t))
            (when (zero? (>>> t 32))
                (§ return 0)
            )
            (loop-when-recur [mlen (dec mlen)] (<= 0 mlen) [(dec mlen)]
                (let [
                ]
                    (§ ass offset (dec offset))
                    (cond (neg? offset) ;; carry out of number
                        (do
                            (§ return 1)
                        )
                        :else
                        (do
                            (aswap a offset inc)
                            (when (not (zero? (aget a offset)))
                                (§ return 0)
                            )
                        )
                    )
                )
            )
            1
        )
    )

    ;;;
     ; Subtracts two numbers of same length, returning borrow.
     ;;
    (defn- #_"int" BigInteger'subN [#_"int[]" a, #_"int[]" b, #_"int" len]
        (let [
              #_"long" sum 0
        ]
            (loop-when-recur [len (dec len)] (<= 0 len) [(dec len)]
                (§ ass sum (+ (- (long! (aget a len)) (long! (aget b len))) (>> sum 32)))
                (aset a len (int sum))
            )

            (int (>> sum 32))
        )
    )

    ;;
     ; Returns -1, 0 or +1 as big-endian unsigned int array arg1 is less than,
     ; equal to, or greater than arg2 up to length len.
     ;;
    (defn- #_"int" BigInteger'intArrayCmpToLen [#_"int[]" arg1, #_"int[]" arg2, #_"int" len]
        (let [
        ]
            (loop-when-recur [#_"int" i 0] (< i len) [(inc i)]
                (let [
                      #_"long" b1 (long! (aget arg1 i))
                      #_"long" b2 (long! (aget arg2 i))
                ]
                    (when (< b1 b2)
                        (§ return -1)
                    )
                    (when (< b2 b1)
                        (§ return 1)
                    )
                )
            )
            0
        )
    )

    ;;;
     ; Montgomery reduce n, modulo mod. This reduces modulo mod and divides
     ; by 2^(32*mlen). Adapted from Colin Plumb's C library.
     ;;
    (defn- #_"int[]" BigInteger'montReduce [#_"int[]" n, #_"int[]" mod, #_"int" mlen, #_"int" inv]
        (let [
              #_"int" c 0
              #_"int" len mlen
              #_"int" offset 0
        ]
            (loop []
                (let [
                      #_"int" nEnd (aget n (- (alength n) 1 offset))
                      #_"int" carry (BigInteger'mulAdd n, mod, offset, mlen, (* inv nEnd))
                ]
                    (§ ass c (+ c (BigInteger'addOne n, offset, mlen, carry)))
                    (§ ass offset (inc offset))
                    (§ ass len (dec len))
                    (recur-if (pos? len) [])
                )
            )

            (while (pos? c)
                (§ ass c (+ c (BigInteger'subN n, mod, mlen)))
            )

            (while (<= 0 (BigInteger'intArrayCmpToLen n, mod, mlen))
                (BigInteger'subN n, mod, mlen)
            )

            n
        )
    )

    (def #_"int[]" BigInteger'bnExpModThreshTable (§ init 7, 25, 81, 241, 673, 1793, Integer/MAX_VALUE )) ;; sentinel

    ;;;
     ; Returns a BigInteger whose value is x to the power of y mod z.
     ; Assumes: z is odd && x < z.
     ;
     ; The algorithm is adapted from Colin Plumb's C library.
     ;
     ; The window algorithm:
     ; The idea is to keep a running product of b1 = n^(high-order bits of exp)
     ; and then keep appending exponent bits to it. The following patterns
     ; apply to a 3-bit window (k = 3):
     ; To append   0: square
     ; To append   1: square, multiply by n^1
     ; To append  10: square, multiply by n^1, square
     ; To append  11: square, square, multiply by n^3
     ; To append 100: square, multiply by n^1, square, square
     ; To append 101: square, square, square, multiply by n^5
     ; To append 110: square, square, multiply by n^3, square
     ; To append 111: square, square, square, multiply by n^7
     ;
     ; Since each pattern involves only one multiply, the longer the pattern
     ; the better, except that a 0 (no multiplies) can be appended directly.
     ; We precompute a table of odd powers of n, up to 2^k, and can then
     ; multiply k bits of exponent at a time. Actually, assuming random
     ; exponents, there is on average one zero bit between needs to
     ; multiply (1/2 of the time there's none, 1/4 of the time there's 1,
     ; 1/8 of the time, there's 2, 1/32 of the time, there's 3, etc.), so
     ; you have to do one multiply per k+1 bits of exponent.
     ;
     ; The loop walks down the exponent, squaring the result buffer as
     ; it goes. There is a wbits+1 bit lookahead buffer, buf, that is
     ; filled with the upcoming exponent bits. (What is read after the
     ; end of the exponent is unimportant, but it is filled with zero here.)
     ; When the most-significant bit of this buffer becomes set, i.e.
     ; (buf & tblmask) != 0, we have to decide what pattern to multiply
     ; by, and when to do it. We decide, remember to do it in future
     ; after a suitable number of squarings have passed (e.g. a pattern
     ; of "100" in the buffer requires that we multiply by n^1 immediately;
     ; a pattern of "110" calls for multiplying by n^3 after one more
     ; squaring), clear the buffer, and continue.
     ;
     ; When we start, there is one more optimization: the result buffer
     ; is implcitly one, so squaring it or multiplying by it can be
     ; optimized away. Further, if we start with a pattern like "100"
     ; in the lookahead window, rather than placing n into the buffer
     ; and then starting to square it, we have already computed n^2
     ; to compute the odd-powers table, so we can place that into
     ; the buffer and save a squaring.
     ;
     ; This means that if you have a k-bit window, to compute n^z,
     ; where z is the high k bits of the exponent, 1/2 of the time
     ; it requires no squarings. 1/4 of the time, it requires 1
     ; squaring, ... 1/2^(k-1) of the time, it reqires k-2 squarings.
     ; And the remaining 1/2^(k-1) of the time, the top k bits are a
     ; 1 followed by k-1 0 bits, so it again only requires k-2
     ; squarings, not k-1. The average of these is 1. Add that
     ; to the one squaring we have to do to compute the table,
     ; and you'll see that a k-bit window saves k-2 squarings
     ; as well as reducing the multiplies. (It actually doesn't
     ; hurt in the case k = 1, either.)
     ;;
    #_method
    (defn- #_"BigInteger" BigInteger''oddModPow [#_"BigInteger" this, #_"BigInteger" y, #_"BigInteger" z]
        ;; special case for exponent of one
        (when (.equals y, BigInteger'ONE)
            (§ return this)
        )

        ;; special case for base of zero
        (when (zero? (:signum this))
            (§ return BigInteger'ZERO)
        )

        (let [
              #_"int[]" base (aclone (:mag this))
              #_"int[]" exp (:mag y)
              #_"int[]" mod (:mag z)
              #_"int" modLen (alength mod)
              ;; select an appropriate window size
              #_"int" wbits 0
              #_"int" ebits (BigInteger'bitLength exp, (alength exp))
        ]
            ;; if exponent is 65537 (0x10001), use minimum window size
            (when (or (not (== ebits 17)) (not (== (aget exp 0) 65537)))
                (while (< (aget BigInteger'bnExpModThreshTable wbits) ebits)
                    (§ ass wbits (inc wbits))
                )
            )

            ;; calculate appropriate table size
            (let [
                  #_"int" tblmask (<< 1 wbits)
                  ;; allocate table for precomputed odd powers of base in Montgomery form
                  #_"int[][]" table (make-array (Class/forName "[I") tblmask)
            ]
                (loop-when-recur [#_"int" i 0] (< i tblmask) [(inc i)]
                    (aset table i (int-array modLen))
                )

                ;; compute the modular inverse
                (let [
                      #_"int" inv (- (MutableBigInteger'inverseMod32 (aget mod (dec modLen))))
                      ;; convert base to Montgomery form
                      #_"int[]" a (BigInteger'leftShift base, (§ soon alength base), (<< modLen 5))
                      #_"MutableBigInteger" a2 (MutableBigInteger'new-a a)
                      #_"MutableBigInteger" b2 (MutableBigInteger'new-a mod)
                      [_ #_"MutableBigInteger" r] (MutableBigInteger'divide a2, b2)
                ]
                    (aset table 0 (Arrays/copyOfRange (:value r), (:offset r), (+ (:offset r) (:intLen r))))

                    ;; pad table[0] with leading zeros so its length is at least modLen
                    (when (< (alength (aget table 0)) modLen)
                        (let [
                              #_"int" offset (- modLen (alength (aget table 0)))
                              #_"int[]" t2 (int-array modLen)
                        ]
                            (loop-when-recur [#_"int" i 0] (< i (alength (aget table 0))) [(inc i)]
                                (aset t2 (+ i offset) (aget table 0 i))
                            )
                            (aset table 0 t2)
                        )
                    )

                    ;; set b to the square of the base
                    (let [
                          #_"int[]" b (BigInteger'squareToLen (aget table 0), modLen, nil)
                          _ (§ ass b (BigInteger'montReduce b, mod, modLen, inv))
                          ;; set t to high half of b
                          #_"int[]" t (Arrays/copyOf b, modLen)
                    ]

                        ;; fill in the table with odd powers of the base
                        (loop-when-recur [#_"int" i 1] (< i tblmask) [(inc i)]
                            (let [
                                  #_"int[]" prod (BigInteger''multiplyToLen this, t, modLen, (aget table (dec i)), modLen, nil)
                            ]
                                (aset table i (BigInteger'montReduce prod, mod, modLen, inv))
                            )
                        )

                        (let [
                              ;; pre load the window that slides over the exponent
                              #_"int" bitpos (<< 1 (& (dec ebits) (dec 32)))
                              #_"int" buf 0
                              #_"int" elen (alength exp)
                              #_"int" eIndex 0
                        ]
                            (loop-when-recur [#_"int" i 0] (<= i wbits) [(inc i)]
                                (let [
                                ]
                                    (§ ass buf (| (<< buf 1) (if (zero? (& (aget exp eIndex) bitpos)) 0 1)))
                                    (§ ass bitpos (>>> bitpos 1))
                                    (when (zero? bitpos)
                                        (§ ass eIndex (inc eIndex))
                                        (§ ass bitpos (<< 1 (dec 32)))
                                        (§ ass elen (dec elen))
                                    )
                                )
                            )

                            (let [
                                  #_"int" multpos ebits
                                  ;; the first iteration, which is hoisted out of the main loop
                                  _ (§ ass ebits (dec ebits))
                                  #_"boolean" isone true
                                  _ (§ ass multpos (- ebits wbits))
                            ]
                                (while (zero? (& buf 1))
                                    (§ ass buf (>>> buf 1))
                                    (§ ass multpos (inc multpos))
                                )

                                (let [
                                      #_"int[]" mult (§ soon aget table (>>> buf 1))
                                ]
                                    (§ ass buf 0)
                                    (when (== multpos ebits)
                                        (§ ass isone false)
                                    )

                                    ;; the main loop
                                    (§ while true
                                        (let [
                                        ]
                                            (§ ass ebits (dec ebits))
                                            ;; advance the window
                                            (§ ass buf (<< buf 1))

                                            (when (not (zero? elen))
                                                (§ ass buf (| buf (if (zero? (& (aget exp eIndex) bitpos)) 0 1)))
                                                (§ ass bitpos (>>> bitpos 1))
                                                (when (zero? bitpos)
                                                    (§ ass eIndex (inc eIndex))
                                                    (§ ass bitpos (<< 1 (dec 32)))
                                                    (§ ass elen (dec elen))
                                                )
                                            )

                                            ;; examine the window for pending multiplies
                                            (when (not (zero? (& buf tblmask)))
                                                (let [
                                                ]
                                                    (§ ass multpos (- ebits wbits))
                                                    (while (zero? (& buf 1))
                                                        (§ ass buf (>>> buf 1))
                                                        (§ ass multpos (inc multpos))
                                                    )
                                                    (§ ass mult (aget table (>>> buf 1)))
                                                    (§ ass buf 0)
                                                )
                                            )

                                            ;; perform multiply
                                            (when (== ebits multpos)
                                                (cond isone
                                                    (do
                                                        (§ ass b (aclone mult))
                                                        (§ ass isone false)
                                                    )
                                                    :else
                                                    (do
                                                        (§ ass t b)
                                                        (§ ass a (BigInteger''multiplyToLen this, t, modLen, mult, modLen, a))
                                                        (§ ass a (BigInteger'montReduce a, mod, modLen, inv))
                                                        (§ ass t a)
                                                        (§ ass a b)
                                                        (§ ass b t)
                                                    )
                                                )
                                            )

                                            ;; check if done
                                            (when (zero? ebits)
                                                (§ break)
                                            )

                                            ;; square the input
                                            (when (not isone)
                                                (§ ass t b)
                                                (§ ass a (BigInteger'squareToLen t, modLen, a))
                                                (§ ass a (BigInteger'montReduce a, mod, modLen, inv))
                                                (§ ass t a)
                                                (§ ass a b)
                                                (§ ass b t)
                                            )
                                        )
                                    )

                                    ;; convert result out of Montgomery form and return
                                    (let [
                                          #_"int[]" t2 (int-array (* 2 modLen))
                                    ]
                                        (System/arraycopy b, 0, t2, modLen, modLen)

                                        (§ ass b (BigInteger'montReduce t2, mod, modLen, inv))

                                        (§ ass t2 (Arrays/copyOf b, modLen))

                                        (BigInteger'new-2ia 1, t2)
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
     ; Returns a BigInteger whose value is this mod(2^p).
     ; Assumes that this {@code BigInteger >= 0} and {@code p > 0}.
     ;;
    #_method
    (defn- #_"BigInteger" BigInteger''mod2 [#_"BigInteger" this, #_"int" p]
        (when (<= (BigInteger''bitLength this) p)
            (§ return this)
        )

        ;; copy remaining ints of mag
        (let [
              #_"int" numInts (>>> (+ p 31) 5)
              #_"int[]" mag (int-array numInts)
        ]
            (System/arraycopy (:mag this), (- (alength (:mag this)) numInts), mag, 0, numInts)

            ;; mask out any excess bits
            (let [
                  #_"int" excessBits (- (<< numInts 5) p)
            ]
                (aswap mag 0 & (dec (<< 1 (- 32 excessBits))))

                (if (zero? (aget mag 0)) (BigInteger'new-2ia 1, mag) (BigInteger'new-2ai mag, 1))
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is (this^exponent) mod (2^p)
     ;;
    #_method
    (defn- #_"BigInteger" BigInteger''modPow2 [#_"BigInteger" this, #_"BigInteger" exponent, #_"int" p]
        ;;
         ; Perform exponentiation using repeated squaring trick, chopping off
         ; high order bits as indicated by modulus.
         ;;
        (let [
              #_"BigInteger" result BigInteger'ONE
              #_"BigInteger" baseToPow2 (BigInteger''mod2 this, p)
              #_"int" expOffset 0
              #_"int" limit (BigInteger''bitLength exponent)
        ]
            (when (BigInteger''testBit this, 0)
                (§ ass limit (min (dec p) limit))
            )

            (while (< expOffset limit)
                (when (BigInteger''testBit exponent, expOffset)
                    (§ ass result (-> (BigInteger''multiply result, baseToPow2) (BigInteger''mod2 p)))
                )
                (§ ass expOffset (inc expOffset))
                (when (< expOffset limit)
                    (§ ass baseToPow2 (-> (BigInteger''square baseToPow2) (BigInteger''mod2 p)))
                )
            )

            result
        )
    )

    ;;;
     ; Returns a BigInteger whose value is {@code (this}<sup>-1</sup> {@code mod m)}.
     ;
     ; @param  m the modulus.
     ; @return {@code this}<sup>-1</sup> {@code mod m}.
     ; @throws ArithmeticException {@code m} <= 0, or this BigInteger
     ;         has no multiplicative inverse mod m (that is, this BigInteger
     ;         is not *relatively prime* to m).
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''modInverse [#_"BigInteger" this, #_"BigInteger" m]
        (when (not (== (:signum m) 1))
            (throw! "modulus not positive")
        )

        (when (.equals m, BigInteger'ONE)
            (§ return BigInteger'ZERO)
        )

        ;; calculate (this mod m)
        (let [
              #_"BigInteger" modVal this
        ]
            (when (or (neg? (:signum this)) (<= 0 (BigInteger'compareMagnitude-i this, m)))
                (§ ass modVal (BigInteger''mod this, m))
            )

            (when (.equals modVal, BigInteger'ONE)
                (§ return BigInteger'ONE)
            )

            (let [
                  #_"MutableBigInteger" a (MutableBigInteger'fromBigInteger modVal)
                  #_"MutableBigInteger" b (MutableBigInteger'fromBigInteger m)
                  #_"MutableBigInteger" result (MutableBigInteger'mutableModInverse a, b)
            ]
                (MutableBigInteger'toBigInteger result, 1)
            )
        )
    )

    ;;;
     ; Returns a BigInteger whose value is <tt>(this<sup>exponent</sup> mod m)</tt>.
     ; (Unlike {@code pow}, this method permits negative exponents.)
     ;
     ; @param  exponent the exponent.
     ; @param  m the modulus.
     ; @return <tt>this<sup>exponent</sup> mod m</tt>
     ; @throws ArithmeticException {@code m} <= 0 or the exponent is negative
     ;         and this BigInteger is not *relatively prime* to {@code m}.
     ;;
    #_method
    (defn #_"BigInteger" BigInteger''modPow [#_"BigInteger" this, #_"BigInteger" exponent, #_"BigInteger" m]
        (when (<= (:signum m) 0)
            (throw! "modulus not positive")
        )

        ;; trivial cases
        (when (zero? (:signum exponent))
            (§ return (if (.equals m, BigInteger'ONE) BigInteger'ZERO BigInteger'ONE))
        )

        (when (.equals this, BigInteger'ONE)
            (§ return (if (.equals m, BigInteger'ONE) BigInteger'ZERO BigInteger'ONE))
        )

        (when (and (.equals this, BigInteger'ZERO) (<= 0 (:signum exponent)))
            (§ return BigInteger'ZERO)
        )

        (when (and (.equals this, (aget BigInteger'negConst 1)) (not (BigInteger''testBit exponent, 0)))
            (§ return (if (.equals m, BigInteger'ONE) BigInteger'ZERO BigInteger'ONE))
        )

        (let [
              #_"boolean" invertResult (neg? (:signum exponent))
        ]
            (when invertResult
                (§ ass exponent (BigInteger''negate exponent))
            )

            (let [
                  #_"BigInteger" base (if (or (neg? (:signum this)) (<= 0 (.compareTo this, m))) (BigInteger''mod this, m) this)
                  #_"BigInteger" result (ß )
            ]
                (cond (BigInteger''testBit m, 0) ;; odd modulus
                    (do
                        (§ ass result (BigInteger''oddModPow base, exponent, m))
                    )
                    :else
                    ;;
                     ; Even modulus. Tear it into an "odd part" (m1) and power of two (m2),
                     ; exponentiate mod m1, manually exponentiate mod m2, and use
                     ; Chinese Remainder Theorem to combine results.
                     ;;
                    (let [
                          ;; tear m apart into odd part (m1) and power of 2 (m2)
                          #_"int" p (BigInteger''getLowestSetBit m) ;; max pow of 2 that divides m
                          #_"BigInteger" m1 (BigInteger''shiftRight m, p) ;; m/2^p
                          #_"BigInteger" m2 (BigInteger''shiftLeft BigInteger'ONE, p) ;; 2^p
                          ;; calculate new base from m1
                          #_"BigInteger" base2 (if (or (neg? (:signum this)) (<= 0 (.compareTo this, m1))) (BigInteger''mod this, m1) this)
                          ;; caculate (base^exponent) mod m1
                          #_"BigInteger" a1 (if (.equals m1, BigInteger'ONE) BigInteger'ZERO (BigInteger''oddModPow base2, exponent, m1))
                          ;; calculate (this^exponent) mod m2
                          #_"BigInteger" a2 (BigInteger''modPow2 base, exponent, p)
                          ;; combine results using Chinese Remainder Theorem
                          #_"BigInteger" y1 (BigInteger''modInverse m2, m1)
                          #_"BigInteger" y2 (BigInteger''modInverse m1, m2)
                    ]
                        (cond (< (alength (:mag m)) (quot BigInteger'MAX_MAG_LENGTH 2))
                            (do
                                (§ ass result (-> (BigInteger''multiply a1, m2) (BigInteger''multiply y1) (BigInteger''add (-> (BigInteger''multiply a2, m1) (BigInteger''multiply y2))) (BigInteger''mod m)))
                            )
                            :else
                            (let [
                                  #_"MutableBigInteger" t1 (-> (MutableBigInteger'fromBigInteger (BigInteger''multiply a1, m2)) (MutableBigInteger'multiply (MutableBigInteger'fromBigInteger y1)))
                                  #_"MutableBigInteger" t2 (-> (MutableBigInteger'fromBigInteger (BigInteger''multiply a2, m1)) (MutableBigInteger'multiply (MutableBigInteger'fromBigInteger y2)))
                                  [_ #_"MutableBigInteger" r] (MutableBigInteger'divide (MutableBigInteger''add t1, t2), (MutableBigInteger'fromBigInteger m))
                                  r (MutableBigInteger''normalize r)
                            ]
                                (§ ass result (MutableBigInteger'toBigInteger r, (if (MutableBigInteger'isZero r) 0 1)))
                            )
                        )
                    )
                )

                (if invertResult (BigInteger''modInverse result, m) result)
            )
        )
    )

    ;; hash function

    ;;;
     ; Returns the hash code for this BigInteger.
     ;
     ; @return hash code for this BigInteger.
     ;;
    #_foreign
    (defn #_"int" hashCode---BigInteger [#_"BigInteger" this]
        (let [
              #_"int" hashCode 0
        ]
            (loop-when-recur [#_"int" i 0] (< i (alength (:mag this))) [(inc i)]
                (§ ass hashCode (int (+ (* 31 hashCode) (long! (aget (:mag this) i)))))
            )

            (* hashCode (:signum this))
        )
    )

    ;; zero[i] is a string of i consecutive zeros
    (def- #_"String[]" BigInteger'zeros (make-array String 64))

    #_static
    (§
        (aset BigInteger'zeros 63 "000000000000000000000000000000000000000000000000000000000000000")
        (loop-when-recur [#_"int" i 0] (< i 63) [(inc i)]
            (aset BigInteger'zeros i (.substring (aget BigInteger'zeros 63), 0, i))
        )
    )

    ;;;
     ; This method is used to perform toString when arguments are small.
     ;;
    (defn- #_"String" BigInteger'toString [#_"BigInteger" this, #_"int" radix]
        (when (zero? (:signum this))
            (§ return "0")
        )

        ;; compute upper bound on number of digit groups and allocate space
        (let [
              #_"int" maxNumDigitGroups (quot (+ (* 4 (alength (:mag this))) 6) 7)
              #_"String[]" digitGroup (make-array String maxNumDigitGroups)

              ;; translate number to string, a digit group at a time
              #_"BigInteger" tmp (BigInteger''abs this)
              #_"int" numGroups 0
        ]
            (while (not (zero? (:signum tmp)))
                (let [
                      #_"BigInteger" d (aget BigInteger'longRadix radix)
                      #_"MutableBigInteger" a (MutableBigInteger'new-a (:mag tmp))
                      #_"MutableBigInteger" b (MutableBigInteger'new-a (:mag d))
                      [#_"MutableBigInteger" q #_"MutableBigInteger" r] (MutableBigInteger'divide a, b)
                      #_"BigInteger" q2 (MutableBigInteger'toBigInteger q, (* (:signum tmp) (:signum d)))
                      #_"BigInteger" r2 (MutableBigInteger'toBigInteger r, (* (:signum tmp) (:signum d)))
                ]
                    (aset digitGroup numGroups (Long/toString (.longValue r2), radix))
                    (§ ass numGroups (inc numGroups))
                    (§ ass tmp q2)
                )
            )

            ;; put sign (if any) and first digit group into result buffer
            (let [
                  #_"StringBuilder" buf (StringBuilder. (inc (* numGroups (aget BigInteger'digitsPerLong radix))))
            ]
                (when (neg? (:signum this))
                    (.append buf, \-)
                )
                (.append buf, (aget digitGroup (dec numGroups)))

                ;; append remaining digit groups padded with leading zeros
                (loop-when-recur [#_"int" i (- numGroups 2)] (<= 0 i) [(dec i)]
                    ;; prepend (any) leading zeros for this digit group
                    (let [
                          #_"int" numLeadingZeros (- (aget BigInteger'digitsPerLong radix) (.length (aget digitGroup i)))
                    ]
                        (when (not (zero? numLeadingZeros))
                            (.append buf, (aget BigInteger'zeros numLeadingZeros))
                        )
                        (.append buf, (aget digitGroup i))
                    )
                )
                (.toString buf)
            )
        )
    )

    ;;;
     ; Returns the String representation of this BigInteger in the given radix.
     ; If the radix is outside the range from {@link Character#MIN_RADIX}
     ; to {@link Character#MAX_RADIX} inclusive, it will default to 10 (as is
     ; the case for {@code Integer.toString}). The digit-to-character mapping
     ; provided by {@code Character.forDigit} is used, and a minus
     ; sign is prepended if appropriate. (This representation is compatible with
     ; the {@link #BigInteger(String, int) (String, int)} constructor.)
     ;
     ; @param  radix  radix of the String representation.
     ; @return String representation of this BigInteger in the given radix.
     ;;
    #_method
    (defn #_"String" BigInteger''toString [#_"BigInteger" this, #_"int" radix]
        (when (zero? (:signum this))
            (§ return "0")
        )
        (when-not (<= Character/MIN_RADIX radix Character/MAX_RADIX)
            (§ ass radix 10)
        )

        (§ return (BigInteger'toString this, radix))
    )

    ;;;
     ; Returns the decimal String representation of this BigInteger.
     ; The digit-to-character mapping provided by {@code Character.forDigit}
     ; is used, and a minus sign is prepended if appropriate.
     ; (This representation is compatible with the {@link #BigInteger(String) (String)}
     ; constructor, and allows for String concatenation with Java's + operator.)
     ;
     ; @return decimal String representation of this BigInteger.
     ;;
    #_foreign
    (defn #_"String" toString---BigInteger [#_"BigInteger" this]
        (BigInteger''toString this, 10)
    )

    ;;;
     ; Returns a byte array containing the two's-complement representation of this BigInteger.
     ; The byte array will be in *big-endian* byte-order: the most significant byte is in the
     ; zeroth element. The array will contain the minimum number of bytes required to represent this
     ; BigInteger, including at least one sign bit, which is {@code (ceil((this.bitLength() + 1)/8))}.
     ; (This representation is compatible with the {@link #BigInteger(byte[]) (byte[])} constructor.)
     ;
     ; @return a byte array containing the two's-complement representation of this BigInteger.
     ;;
    #_method
    (defn #_"byte[]" BigInteger''toByteArray [#_"BigInteger" this]
        (let [
              #_"int" byteLen (inc (quot (BigInteger''bitLength this) 8))
              #_"byte[]" byteArray (byte-array byteLen)
        ]
            (loop-when-recur [#_"int" bytesCopied 4 #_"int" nextInt 0 #_"int" intIndex 0 #_"int" i (dec byteLen)] (<= 0 i) [bytesCopied nextInt intIndex (dec i)]
                (cond (== bytesCopied 4)
                    (do
                        (§ ass nextInt (BigInteger''getInt this, intIndex))
                        (§ ass intIndex (inc intIndex))
                        (§ ass bytesCopied 1)
                    )
                    :else
                    (do
                        (§ ass nextInt (>>> nextInt 8))
                        (§ ass bytesCopied (inc bytesCopied))
                    )
                )
                (aset byteArray i (byte nextInt))
            )
            byteArray
        )
    )

    ;;;
     ; Converts this BigInteger to an {@code int}.
     ; This conversion is analogous to a *narrowing primitive conversion*
     ; from {@code long} to {@code int} as defined in section 5.1.3 of
     ; <cite>The Java Language Specification</cite>: if this BigInteger
     ; is too big to fit in an {@code int}, only the low-order 32 bits are returned.
     ; Note that this conversion can lose information about the overall magnitude
     ; of the BigInteger value as well as return a result with the opposite sign.
     ;
     ; @return this BigInteger converted to an {@code int}.
     ;;
    #_foreign
    (defn #_"int" intValue---BigInteger [#_"BigInteger" this]
        (BigInteger''getInt this, 0)
    )

    ;;;
     ; Converts this BigInteger to a {@code long}.
     ; This conversion is analogous to a *narrowing primitive conversion*
     ; from {@code long} to {@code int} as defined in section 5.1.3 of
     ; <cite>The Java Language Specification</cite>: if this BigInteger
     ; is too big to fit in a {@code long}, only the low-order 64 bits are returned.
     ; Note that this conversion can lose information about the overall magnitude
     ; of the BigInteger value as well as return a result with the opposite sign.
     ;
     ; @return this BigInteger converted to a {@code long}.
     ;;
    #_foreign
    (defn #_"long" longValue---BigInteger [#_"BigInteger" this]
        (let [
              #_"long" result 0
        ]
            (loop-when-recur [#_"int" i 1] (<= 0 i) [(dec i)]
                (§ ass result (+ (<< result 32) (long! (BigInteger''getInt this, i))))
            )
            result
        )
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
     ; Returns true iff this BigInteger passes the specified number of Miller-Rabin tests.
     ; This test is taken from the DSA spec (NIST FIPS 186-2).
     ;
     ; The following assumptions are made:
     ; This BigInteger is a positive, odd number greater than 2.
     ; iterations <= 50.
     ;;
    #_method
    (defn- #_"boolean" BigInteger''passesMillerRabin [#_"BigInteger" this, #_"int" iterations, #_"Random" rnd]
        ;; find a and m such that m is odd and this == 1 + 2^a * m
        (let [
              #_"BigInteger" thisMinusOne (BigInteger''subtract this, BigInteger'ONE)
              #_"BigInteger" m thisMinusOne
              #_"int" a (BigInteger''getLowestSetBit m)
              _ (§ ass m (BigInteger''shiftRight m, a))
        ]
            ;; do the tests
            (when (nil? rnd)
                (§ ass rnd (ThreadLocalRandom/current))
            )
            (loop-when-recur [#_"int" i 0] (< i iterations) [(inc i)]
                ;; generate a uniform random on (1, this)
                (let [
                      #_"BigInteger" b (ß )
                ]
                    (loop []
                        (§ ass b (BigInteger'new-2ir (BigInteger''bitLength this), rnd))
                        (recur-if (or (<= (.compareTo b, BigInteger'ONE) 0) (<= 0 (.compareTo b, this))) [])
                    )

                    (let [
                        #_"int" j 0
                        #_"BigInteger" z (BigInteger''modPow b, m, this)
                    ]
                        (while (not (or (and (zero? j) (.equals z, BigInteger'ONE)) (.equals z, thisMinusOne)))
                            (when (and (pos? j) (.equals z, BigInteger'ONE))
                                (§ return false)
                            )
                            (§ ass j (inc j))
                            (when (== j a)
                                (§ return false)
                            )
                            (§ ass z (BigInteger''modPow z, BigInteger'TWO, this))
                        )
                    )
                )
            )
            true
        )
    )

    ;;;
     ; Computes Jacobi(p,n).
     ; Assumes n positive, odd, n>=3.
     ;;
    (defn- #_"int" BigInteger'jacobiSymbol [#_"int" p, #_"BigInteger" n]
        (when (zero? p)
            (§ return 0)
        )

        ;; Algorithm and comments adapted from Colin Plumb's C library.
        (let [
              #_"int" j 1
              #_"int" u (aget (:mag n) (dec (alength (:mag n))))
        ]
            ;; make p positive
            (when (neg? p)
                (§ ass p (- p))
                (let [
                      #_"int" n8 (& u 7)
                ]
                    (when (or (== n8 3) (== n8 7))
                        (§ ass j (- j)) ;; 3 (011) or 7 (111) mod 8
                    )
                )
            )

            ;; get rid of factors of 2 in p
            (while (zero? (& p 3))
                (§ ass p (>> p 2))
            )
            (when (zero? (& p 1))
                (§ ass p (>> p 1))
                (when (not (zero? (& (bit-xor u (>> u 1)) 2)))
                    (§ ass j (- j)) ;; 3 (011) or 5 (101) mod 8
                )
            )
            (when (== p 1)
                (§ return j)
            )
            ;; then, apply quadratic reciprocity
            (when (not (zero? (& p u 2))) ;; p = u = 3 (mod 4)?
                (§ ass j (- j))
            )
            ;; and reduce u mod p
            (§ ass u (.intValue (BigInteger''mod n, (BigInteger'valueOf-l p))))

            ;; now compute Jacobi(u,p), u < p
            (while (not (zero? u))
                (while (zero? (& u 3))
                    (§ ass u (>> u 2))
                )
                (when (zero? (& u 1))
                    (§ ass u (>> u 1))
                    (when (not (zero? (& (bit-xor p (>> p 1)) 2)))
                        (§ ass j (- j)) ;; 3 (011) or 5 (101) mod 8
                    )
                )
                (when (== u 1)
                    (§ return j)
                )
                ;; now both u and p are odd, so use quadratic reciprocity
                (when-not (< u p)
                    (throw! "(not (< u p))")
                )
                (let [
                      #_"int" t u
                      u p
                      p t
                ]
                    (when (not (zero? (& u p 2))) ;; u = p = 3 (mod 4)?
                        (§ ass j (- j))
                    )
                    ;; now u >= p, so it can be reduced
                    (§ ass u (% u p))
                )
            )
            0
        )
    )

    (defn- #_"BigInteger" BigInteger'lucasLehmerSequence [#_"int" z, #_"BigInteger" k, #_"BigInteger" n]
        (let [
              #_"BigInteger" d (BigInteger'valueOf-l z)
              #_"BigInteger" u BigInteger'ONE
              #_"BigInteger" u2 (ß )
              #_"BigInteger" v BigInteger'ONE
              #_"BigInteger" v2 (ß )
        ]
            (loop-when-recur [#_"int" i (- (BigInteger''bitLength k) 2)] (<= 0 i) [(dec i)]
                (§ ass u2 (-> (BigInteger''multiply u, v) (BigInteger''mod n)))

                (§ ass v2 (-> (BigInteger''square v) (BigInteger''add (BigInteger''multiply d, (BigInteger''square u))) (BigInteger''mod n)))
                (when (BigInteger''testBit v2, 0)
                    (§ ass v2 (BigInteger''subtract v2, n))
                )

                (§ ass v2 (BigInteger''shiftRight v2, 1))

                (§ ass u u2)
                (§ ass v v2)
                (when (BigInteger''testBit k, i)
                    (§ ass u2 (-> (BigInteger''add u, v) (BigInteger''mod n)))
                    (when (BigInteger''testBit u2, 0)
                        (§ ass u2 (BigInteger''subtract u2, n))
                    )

                    (§ ass u2 (BigInteger''shiftRight u2, 1))
                    (§ ass v2 (-> (BigInteger''add v, (BigInteger''multiply d, u)) (BigInteger''mod n)))
                    (when (BigInteger''testBit v2, 0)
                        (§ ass v2 (BigInteger''subtract v2, n))
                    )
                    (§ ass v2 (BigInteger''shiftRight v2, 1))

                    (§ ass u u2)
                    (§ ass v v2)
                )
            )
            u
        )
    )

    ;;;
     ; Returns true iff this BigInteger is a Lucas-Lehmer probable prime.
     ;
     ; The following assumptions are made:
     ; This BigInteger is a positive, odd number.
     ;;
    #_method
    (defn- #_"boolean" BigInteger''passesLucasLehmer [#_"BigInteger" this]
        (let [
              #_"BigInteger" thisPlusOne (BigInteger''add this, BigInteger'ONE)
              ;; step 1
              #_"int" d 5
        ]
            (while (not (== (BigInteger'jacobiSymbol d, this) -1))
                ;; 5, -7, 9, -11, ...
                (§ ass d (if (neg? d) (+ (abs d) 2) (- (+ d 2))))
            )

            ;; step 2
            (let [
                  #_"BigInteger" u (BigInteger'lucasLehmerSequence d, thisPlusOne, this)
            ]
                ;; step 3
                (.equals (BigInteger''mod u, this), BigInteger'ZERO)
            )
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
                (§ return (BigInteger''passesMillerRabin this, rounds, random))
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

            (and (BigInteger''passesMillerRabin this, rounds, random) (BigInteger''passesLucasLehmer this))
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
              #_"BigInteger" w (BigInteger''abs this)
        ]
            (when (.equals w, BigInteger'TWO)
                (§ return true)
            )
            (when (or (not (BigInteger''testBit w, 0)) (.equals w, BigInteger'ONE))
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
                              #_"long" r (.longValue (BigInteger''remainder p, BigInteger'SMALL_PRIME_PRODUCT))
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
              #_"BigInteger" p (-> (BigInteger'new-2ir bitLength, rnd) (BigInteger''setBit (dec bitLength)))
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
                        (§ ass p (BigInteger''add p, (BigInteger'valueOf-l (* 2 searchLen))))
                        (when (not (== (BigInteger''bitLength p) bitLength))
                            (§ ass p (-> (BigInteger'new-2ir bitLength, rnd) (BigInteger''setBit (dec bitLength))))
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
              #_"BigInteger" result (BigInteger''add this, BigInteger'ONE)
        ]
            ;; fastpath for small numbers
            (when (< (BigInteger''bitLength result) BigInteger'SMALL_PRIME_THRESHOLD)
                ;; ensure an odd number
                (when (not (BigInteger''testBit result, 0))
                    (§ ass result (BigInteger''add result, BigInteger'ONE))
                )

                (§ while true
                    ;; do cheap "pre-test" if applicable
                    (when (< 6 (BigInteger''bitLength result))
                        (let [
                              #_"long" r (.longValue (BigInteger''remainder result, BigInteger'SMALL_PRIME_PRODUCT))
                        ]
                            (when (or (zero? (% r 3)) (zero? (% r 5)) (zero? (% r 7)) (zero? (% r 11)) (zero? (% r 13)) (zero? (% r 17)) (zero? (% r 19)) (zero? (% r 23)) (zero? (% r 29)) (zero? (% r 31)) (zero? (% r 37)) (zero? (% r 41)))
                                (§ ass result (BigInteger''add result, BigInteger'TWO))
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

                    (§ ass result (BigInteger''add result, BigInteger'TWO))
                )
            )

            ;; start at previous even number
            (when (BigInteger''testBit result, 0)
                (§ ass result (BigInteger''subtract result, BigInteger'ONE))
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
                        (§ ass result (BigInteger''add result, (BigInteger'valueOf-l (* 2 searchLen))))
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
                                        (let [p (BigInteger''add base, (BigInteger'valueOf-l k))]
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
