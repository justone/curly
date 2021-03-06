(ns curly.run
  (:require
    [clojure.string :as string]
    [clojure.pprint :refer [pprint]]

    [babashka.process :refer [process check destroy-tree]]
    [clojure.tools.cli :refer [parse-opts]]

    [curly.shellescape :as shellescape]
    [curly.core :as core]
    ))

;; Helpers

(defn- perr
  [& args]
  (binding [*out* *err*]
    (apply println args)))

(def #^:private cli-options
  [["-v" "--verbose" "Show information to help with debugging."]
   ["-c" "--curl" "Print curl command."]
   ["-n" "--dry-run" "Don't actually run the command."]
   ["-h" "--help"]])

(defn- print-help
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


;; Public interface

(defn curl!
  [commands hosts command-line-args]
  (let [parsed (parse-opts command-line-args cli-options)
        {:keys [options arguments]} parsed
        [command & opts] arguments
        base (commands (keyword command))
        final-command (core/reduce-opts base opts)
        req (core/req->curl final-command hosts)]
    (when (or (nil? command)
              (:help options))
      (print-help parsed commands hosts)
      (System/exit 0))
    (when (:curl options)
      (perr (string/join " " (map shellescape/quote-str req))))
    (when (:verbose options)
      (perr (with-out-str (pprint final-command))))
    (when-not (:dry-run options)
      (check (process req {:inherit true :shutdown destroy-tree})))
    nil))
