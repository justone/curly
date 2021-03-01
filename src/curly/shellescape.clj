(ns curly.shellescape
  (:require
    [clojure.string :as string]
    ))

(def pattern #".*[^\w@%+=:,./-].*")

(defn quote-str
  [s]
  (cond
    (empty? s) "''"
    (re-matches pattern s) (str "'" (string/replace s #"'" "'\"'\"'") "'")
    :else s))
