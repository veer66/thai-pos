(defproject thai-pos "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [eu.danieldk.nlp.jitar/jitar "0.3.3"]
                 [yaito-clj "0.2.0"]
                 [bidi "2.0.9"]
                 [ring "1.5.0"]
                 [hiccup "1.0.5"]]
  :ring {:handler thai-pos.core/app}
  :plugins [[lein-ring "0.11.0"]]
  :profiles {:uberjar {:aot :all}})
