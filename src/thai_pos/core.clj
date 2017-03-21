(ns thai-pos.core
  (:import eu.danieldk.nlp.jitar.corpus.BrownCorpusReader)
  (:import eu.danieldk.nlp.jitar.training.FrequenciesCollector)
  (:import eu.danieldk.nlp.jitar.data.Model)
  (:import eu.danieldk.nlp.jitar.languagemodel.LanguageModel)
  (:import eu.danieldk.nlp.jitar.languagemodel.LinearInterpolationLM)
  (:import eu.danieldk.nlp.jitar.tagger.HMMTagger)
  (:import eu.danieldk.nlp.jitar.wordhandler.LexiconWordHandler)
  (:import eu.danieldk.nlp.jitar.wordhandler.SuffixWordHandler)
  (:import eu.danieldk.nlp.jitar.wordhandler.WordHandler)
  (:use ring.middleware.resource
        ring.middleware.params
        hiccup.core
        hiccup.page
        hiccup.form
        hiccup.util
        hiccup.element)
  (:require [clojure.java.io :as io]
            [yaito-clj.core :as yaito]
            [clojure.string :as str]
            [bidi.ring :refer (make-handler)]
            [bidi.bidi :refer (url-decode)]))

(def corpus-path (.toString (io/resource "corpus.txt")))

(defn train [path]
  (let [reader (-> path
                   (io/reader)
                   (BrownCorpusReader. false))
        trainer (FrequenciesCollector.)]
    (.process trainer reader)
    (.model trainer)))

(defn make-tagger [model]
  (let [swh (SuffixWordHandler. model 2 2 8 4 10 10)
        wh (LexiconWordHandler. (.lexicon model)
                                (.uniGrams model)
                                swh)
        lm (LinearInterpolationLM. (.uniGrams model)
                                   (.biGrams model)
                                   (.triGrams model))
        tagger (HMMTagger. model wh lm 1000.0)]
    (fn [tokens]
      (-> (.tag tagger tokens)
          (HMMTagger/highestProbabilitySequence model)
          (.sequence)))))

(defn extract-words [corpus-path]
  (let [toks (-> (slurp corpus-path)
                 (str/split #"\s+"))]
    (->> (map (fn [tok]
                (first (str/split tok #"/")))
              toks)
         (filter #(re-seq #"[ก-์]+" %))
         distinct
         sort)))

(def tokenize (yaito/create-tokenizer (into-array (extract-words corpus-path))))
(def tag (make-tagger (train corpus-path)))

(defn response
  [body]
  {:status  200
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body body})

(defn text-form [text]
  (form-to {:id "text-form"} [:post "/"]
           (text-area {:cols 80
                       :rows 20} "text" text)
           [:br ]
           [:input {:type "submit" :value "Tag"}]))

(defn header
  ([] (header "Thai Part-of-speech tagger"))
  ([title] [:head
            (include-css "/css/style.css")
            [:title title]
            [:meta {:charset "utf-8"}]]))


(defn tag-with-text [text]
  (let [toks (tokenize text)
        tags (tag toks)]
    (str/join " " (map #(str %1 "/" %2) toks tags))))

(defn index-handler
  [req]
  (let [text (get (:form-params req) "text")]
    (response
     (html5 {:lang "th"}
            (header)
            [:body
             [:div.text-form (text-form text)]
             (when (some? text)
               [:div.tag-result
                (->> (str/split-lines text)
                     (map #(tag-with-text %))
                     (map #(vector :p %)))])]))))

(def handler
  (make-handler ["/" {"" (wrap-params index-handler)}]))

(def app (-> (make-handler ["/" {""
                                 (wrap-params index-handler)}])
             (wrap-resource "public")))



