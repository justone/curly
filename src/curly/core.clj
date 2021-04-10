(ns curly.core
  (:require
    [clojure.edn :as edn]
    [clojure.string :as string]

    [cheshire.core :as json]

    [curly.ext :as ext]
    ))

;; Opt reducing helpers

(defn- sym-str-single
  [v]
  (if (symbol? v) (str v) v))

(defn- desymbolize
  [d]
  (if (sequential? d)
    (map sym-str-single d)
    (sym-str-single d)))

(def #^:private non-body-namespaces
  #{"c" "r"})

(defn- promote-body
  [d]
  (let [first-val (if (sequential? d) (first d) d)]
    (if (or (not (keyword? first-val))
            (not (non-body-namespaces (namespace first-val))))
      (if (sequential? d)
        (cons :r/body d)
        [:r/body d])
      d)))


;; Request building helpers

(defn- ->str
  [v]
  (cond
    (keyword? v) (subs (str v) 1)
    (number? v) (str v)
    (boolean? v) (str v)
    :else (str v)))

(defn encode-body
  [body headers]
  (if (string? body)
    body
    (case (:Content-Type headers)
      "application/json" (json/generate-string body)
      (string/join "&" (map #(str (-> % key ->str) "=" (-> % val ->str)) body)))))


;; Public interface

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

(defn command->curl
  [command hosts]
  (let [{:r/keys [method body host path headers url]
         :c/keys [verbose]} command]
    (cond-> ["curl" "-s"]
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

(defn interpret-arguments
  [commands hosts arguments]
  (let [[command & args] arguments
        command-key (keyword (edn/read-string command))
        base (commands command-key)
        final-command (->> args
                           (ext/munge-arguments :curly/pre)
                           (ext/munge-arguments command-key)
                           (ext/munge-arguments :curly/post)
                           (reduce-opts base)
                           (ext/munge-command :curly/pre)
                           (ext/munge-command command-key)
                           (ext/munge-command :curly/post))
        curl-command (->> (command->curl final-command hosts)
                          (ext/munge-curl-command :curly/pre)
                          (ext/munge-curl-command command-key)
                          (ext/munge-curl-command :curly/post))]
    {:command command-key
     :base base
     :final final-command
     :curl-command curl-command}))
