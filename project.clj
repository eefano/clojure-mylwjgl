(defproject mylwjgl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 [org.lwjgl/lwjgl "3.0.0a"]
                 [org.lwjgl/lwjgl-platform "3.0.0a" :classifier "natives-linux" :native-prefix ""]
                 ]
  :main ^:skip-aot mylwjgl.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
