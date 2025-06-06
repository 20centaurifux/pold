(ns pold.core)

(defonce ^:private nothing ::nothing)

(defn- nothing?
  [x]
  (identical? x nothing))

(defonce ^:private anything? (complement nothing?))

(defn- partfn
  ([kf initf f]
   (let [k (volatile! nothing)
         v (volatile! nothing)]
     (fn
       ([]
        (let [oldv @v]
          (vreset! k nothing)
          (vreset! v nothing)
          oldv))
       ([input]
        (let [oldk @k
              newk (kf input)]
          (if (= oldk newk)
            (do
              (vswap! v f input)
              nothing)
            (let [oldv @v]
              (vreset! k newk)
              (vreset! v (initf input))
              oldv)))))))
  ([kf initf f nextf]
   (let [k (volatile! nothing)
         v (volatile! nothing)]
     (fn
       ([]
        (let [oldv @v]
          (if (anything? oldv)
            (let [p (nextf)]
              (vreset! k nothing)
              (vreset! v nothing)
              (f oldv p))
            oldv)))
       ([input]
        (let [oldk @k
              newk (kf input)]
          (cond
            (nothing? oldk) (do
                              (vreset! k newk)
                              (vreset! v (initf input))
                              (nextf input)
                              nothing)
            (= oldk newk) (let [p (nextf input)]
                            (when (anything? p)
                              (vswap! v f p))
                            nothing)
            :else (let [oldv @v
                        p (nextf)]
                    (vreset! k newk)
                    (vreset! v (initf input))
                    (nextf input)
                    (f oldv p)))))))))

(defn partitioner
  "Takes a set of partition functions (kf :: input -> key,
   initf :: input -> accumulator, aggrf :: accumulator -> input -> accumulator)
   and returns a stateful closure that is the composition of those functions.
   
   The closure is defined with 2 arities for different purposes:
   
   Step (arity 1) - applies all key functions to input and splitting it each
   time a new value is returned. If a key changes an accumulator is created by
   applying initf to input. Otherwise the accumulator is updated
   by appyling aggrf. Returns accumulator of first partition if first key
   changes, otherwise :pold.core/nothing.
   
   Completion (artity 0) - used to flush element at the end of the input.
   Returns :pold.core/nothing if no element is left, otherwise the accumulator
   of the first partition."
  [[kf initf aggrf] & more]
  (if more
    (partfn kf initf aggrf (apply partitioner more))
    (partfn kf initf aggrf)))

(defn part
  "Returns a vector of partition functions (kf :: input -> key,
   initf :: input -> accumulator, aggrf :: accumulator -> input -> accumulator).
   Adds a default identity function for aggregation if no aggregation function
   is given."
  ([kf aggrf]
   (part kf aggrf (fn [result _] result)))
  ([kf initf accf]
   [kf initf accf]))

(defn pold
  "Applies partitioner closure f to each value in coll. Returns a lazy seq of
   partitions. Returns a stateful transducer when no collection is given."
  ([f]
   (fn [rf]
     (let [f' (volatile! f)]
       (fn
         ([] (rf))
         ([result]
          (let [ret (@f')
                result' (if (anything? ret)
                          (unreduced (rf result ret))
                          result)]
            (rf result')))
         ([result input]
          (let [ret (@f' input)]
            (if (anything? ret)
              (let [result' (rf result ret)]
                (when (reduced? result')
                  (vreset! f' (constantly nothing)))
                result')
              result)))))))
  ([f coll]
   (lazy-seq
    (if-let [s (seq coll)]
      (let [head (f (first s))]
        (if (anything? head)
          (cons head (pold f (rest s)))
          (pold f (rest s))))
      (let [v (f)]
        (when (anything? v)
          [v]))))))