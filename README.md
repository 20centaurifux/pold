# pold

**pold** (*partition* and *fold*) is a Clojure library for efficiently dividing
data into any number of partitions and accumulating them into a result.

## Installation

This library can be installed from Clojars:

[![Clojars Project](https://img.shields.io/clojars/v/de.dixieflatline/pold.svg?include_prereleases)](https://clojars.org/de.dixieflatline/pold)

## Usage

### Basic Example

**pold** applies a stateful partition and accumulation closure to each value of
a collection. The closure can be built with **partitioner** which takes a set of
partition functions.

Assume you want to partition numbers based on whether they are even or odd and
return the sum of each partition. In this case, you could use **odd?** as key
function. Whenever the key changes, a new partition is created. The first number
of the partition is used as accumulator, which is initialized with **identity**.
Each subsequent number is added to the accumulator, updating it.

```
(require '[pold.core :refer [pold partitioner part]])

(pold (partitioner (part odd? identity +))
      [1 1 2 4 3 9])
      ; => (2 6 12)
```

When no collection is provided **pold** returns a stateful transducer.

```
(eduction (comp (pold (partitioner (part odd? identity +)))
                (filter #(> % 2)))
          [1 1 2 4 3 9])
          ; => (6 12)
```

### Nested partitions

As described in the example above, **partitioner** accepts a set of partition
functions. This makes it possible to divide and accumulate data into any number
of partitions.

In the example below, an ordered list of artists, albums, and songs is divided
into three partitions, which are then transformed into a list of nested maps.

```
(def songs
  [{:artist "Aphex Twin" :album "On" :track 1 :title "On"}
   {:artist "Aphex Twin" :album "On" :track 2 :title "73-Yips"}
   {:artist "Aphex Twin" :album "On" :track 3 :title "D-Scape"}
   {:artist "Aphex Twin" :album "On" :track 4 :title "Xepha"}
   {:artist "Aphex Twin" :album "Blackbox Life Recorder 21f / In a Room7 F760" :track 1 :title "Blackbox Life Recorder 21f"}
   {:artist "Aphex Twin" :album "Blackbox Life Recorder 21f / In a Room7 F760" :track 2 :title "Zin2 Test5"}
   {:artist "Aphex Twin" :album "Blackbox Life Recorder 21f / In a Room7 F760" :track 3 :title "In a Room7 F760"}
   {:artist "Aphex Twin" :album "Blackbox Life Recorder 21f / In a Room7 F760" :track 4 :title "Blackbox Life Recorder 22 (Parallax mix)"}
   {:artist "Squarepusher" :album "Welcome to Europe" :track 1 :title "Welcome to Europe"}
   {:artist "Squarepusher" :album "Welcome to Europe" :track 2 :title "Hanningfield Window"}
   {:artist "Squarepusher" :album "Welcome to Europe" :track 3 :title "Exciton"}
   {:artist "Autechre" :album "Anti" :track 1 :title "Lost"}
   {:artist "Autechre" :album "Anti" :track 2 :title "Djarum"}
   {:artist "Autechre" :album "Anti" :track 3 :title "Flutter"}])

(defn- make-partitioner
  []
  (partitioner
   (part :artist
         (fn [{:keys [artist]}]
           {:artist artist
            :albums []})
         #(update-in %1 [:albums] conj %2))

   (part :album
         (fn [{:keys [album]}]
           {:title album
            :songs []})
         #(update-in %1 [:songs] conj %2))

   (part :track
         (fn [{:keys [title]}]
           title))))

(pold (make-partitioner) songs)
; => [{:artist "Aphex Twin", :albums [{:title "On" :songs ["On" ...
```