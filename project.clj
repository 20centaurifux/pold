(defproject pold "0.1.0-SNAPSHOT"
  :description "Partition and fold."
  :url "https://github.com/20centaurifux/pold"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :main ^:skip-aot pold.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})