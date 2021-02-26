(ns curly.core
  (:require
    [clojure.edn :as edn]
    [clojure.string :as string]

    [babashka.process :refer [process check destroy-tree]]
    [cheshire.core :as json]
    ))

(defn sym-str-single
  [v]
  (if (symbol? v) (str v) v))

(defn desymbolize
  [d]
  (if (sequential? d)
    (map sym-str-single d)
    (sym-str-single d)))

(defn promote-body
  [d]
  (let [first-val (if (sequential? d) (first d) d)]
    (if (or (not (keyword? first-val))
            (nil? (namespace first-val)))
      (if (sequential? d)
        (cons :r/body d)
        [:r/body d])
      d)))

(defn reduce-opts
  [base opts]
  (reduce
    (fn [result [k v]]
      (let [k-val (-> (try (edn/read-string k) (catch Exception _e k))
                      desymbolize
                      promote-body)
            v-val (-> (try (edn/read-string v) (catch Exception _e v))
                      desymbolize)
            op (if (coll? k-val) assoc-in assoc)]
        (op result k-val v-val)))
    base (partition 2 opts)))

(defn encode-body
  [body headers]
  (case (:Content-Type headers)
    "application/json" (json/generate-string body)))

(defn req->curl
  [req hosts]
  (let [{:r/keys [method body host path headers url]
         :c/keys [verbose]} req]
    (println (pr-str req))
    (cond-> ["curl" #_"-v"]
      verbose
      (into ["-v"])

      (or (and (= :post method)
               (nil? body))
          (and (= :get method)
               (some? body)))
      (into ["-X" (string/upper-case (name method))])

      (some? body)
      (into ["-d" (encode-body body headers)])

      (some? headers)
      (into (mapcat #(vector "-H" (str (name (first %)) ": " (name (second %)))) headers))

      (some? url)
      (into [url])

      (and (nil? url) host path)
      (into [(str (hosts host) path)])

      )))

(defn curl!
  [commands hosts command-line-args]
  (let [[command & opts] command-line-args
        base (commands (keyword command))
        final-command (reduce-opts base opts)
        req (req->curl final-command hosts)]
    (println (string/join " " req))
    (clojure.pprint/pprint final-command)
    (check (process req {:inherit true :shutdown destroy-tree}))
    nil))
