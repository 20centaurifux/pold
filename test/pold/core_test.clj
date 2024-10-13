(ns pold.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [pold.core :refer [pold-by partitioner part]]))

(defonce ^:private flat-songs
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

(defonce ^:private nested-songs
  [{:artist "Aphex Twin",
    :albums [{:title "On"
              :songs ["On"
                      "73-Yips"
                      "D-Scape"
                      "Xepha"]}
             {:title "Blackbox Life Recorder 21f / In a Room7 F760"
              :songs ["Blackbox Life Recorder 21f"
                      "Zin2 Test5"
                      "In a Room7 F760"
                      "Blackbox Life Recorder 22 (Parallax mix)"]}]}
   {:artist "Squarepusher"
    :albums [{:title "Welcome to Europe"
              :songs ["Welcome to Europe"
                      "Hanningfield Window"
                      "Exciton"]}]}
   {:artist "Autechre"
    :albums [{:title "Anti",
              :songs ["Lost"
                      "Djarum"
                      "Flutter"]}]}])

(defn- song-partitioner
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

(deftest test-pold-by
  (testing "collection"
    (let [result (pold-by (song-partitioner) flat-songs)]
      (is (= nested-songs result))))

  (testing "transducer"
    (let [result (eduction (pold-by (song-partitioner))
                           flat-songs)]
      (is (= nested-songs result))))

  (testing "transformation stack"
    (let [[result] (eduction (comp (pold-by (song-partitioner))
                                   (take 1))
                             flat-songs)]
      (is (= (first nested-songs) result)))))