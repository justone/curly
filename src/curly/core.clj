(ns curly.core
  (:require
    [clojure.edn :as edn]
    [clojure.string :as string]
    [clojure.pprint :refer [pprint]]

    [babashka.process :refer [process check destroy-tree]]
    [cheshire.core :as json]
    [clojure.tools.cli :refer [parse-opts]]

    [curly.shellescape :as shellescape]
    ))

(defn sym-str-single
  [v]
  (if (symbol? v) (str v) v))

(defn desymbolize
  [d]
  (if (sequential? d)
    (map sym-str-single d)
    (sym-str-single d)))

(def non-body-namespaces
  #{"c" "r"})

(defn promote-body
  [d]
  (let [first-val (if (sequential? d) (first d) d)]
    (if (or (not (keyword? first-val))
            (not (non-body-namespaces (namespace first-val))))
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

(defn ->str
  [v]
  (cond
    (keyword? v) (subs (str v) 1)
    (number? v) (str v)
    (boolean? v) (str v)
    :else (name v)
    ))

(defn encode-body
  [body headers]
  (if (string? body)
    body
    (case (:Content-Type headers)
      "application/json" (json/generate-string body)
      (string/join "&" (map #(str (->str (key %)) "=" (->str (val %))) body)))))

(defn req->curl
  [req hosts]
  (let [{:r/keys [method body host path headers url]
         :c/keys [verbose]} req]
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

(defn perr
  [& args]
  (binding [*out* *err*]
    (apply println args)))

(def cli-options
  [["-v" "--verbose" "Show information to help with debugging."]
   ["-c" "--curl" "Print curl command."]
   ["-n" "--dry-run" "Don't actually run the command."]
   ["-h" "--help"]])

(defn print-help
  [parsed commands hosts]
  (println "Command line for curl requests.")
  (println)
  (println "Available commands:")
  (println)
  (doseq [id (sort (keys commands))]
    (println (str "  " id)))
  (println)
  (println "Available hosts:")
  (println)
  (doseq [[id host] hosts]
    (println (str "  " id " - " host)))
  (println)
  (println "Options:")
  (println)
  (println (:summary parsed)))

(defn curl!
  [commands hosts command-line-args]
  (let [parsed (parse-opts command-line-args cli-options)
        {:keys [options arguments]} parsed
        [command & opts] arguments
        base (commands (keyword command))
        final-command (reduce-opts base opts)
        req (req->curl final-command hosts)]
    (when (:help options)
      (print-help parsed commands hosts)
      (System/exit 0))
    (when (:curl options)
      (perr (string/join " " (map shellescape/quote-str req))))
    (when (:verbose options)
      (perr (with-out-str (pprint final-command))))
    (when-not (:dry-run options)
      (check (process req {:inherit true :shutdown destroy-tree})))
    nil))
