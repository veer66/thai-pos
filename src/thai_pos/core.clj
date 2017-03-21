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
  (:require [clojure.java.io :as io]
            [yaito-clj.core :as yaito]
            [clojure.string :as str]))

(load-file (.getPath (io/resource "config.clj")))

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



(tag (tokenize "กาบินมา"))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
