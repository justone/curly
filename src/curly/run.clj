(ns curly.run
  (:require
    [clojure.edn :as edn]
    [clojure.string :as string]
    [clojure.pprint :refer [pprint]]

    [babashka.process :refer [process check destroy-tree]]
    [babashka.fs :as fs]
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

(defn- print-command-help
  [k base]
  (println "Command:" k)
  (println)
  (println "Base:")
  (pprint base)
  )

(defn- print-help
  [parsed commands hosts]
  (println "Command line for curl requests.")
  (println)
  (println (str "Available commands (run with `" (fs/file-name *file*) " [command] -h` to see specific help):"))
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
        {:keys [command base final curl-command]} (core/interpret-arguments commands hosts arguments)]
    (when (or (nil? base)
              (:help options))
      (if base
        (print-command-help command base)
        (print-help parsed commands hosts))
      (System/exit 0))
    (when (:verbose options)
      (perr "Processing command:" command "\n")
      (perr "Base:")
      (perr (with-out-str (pprint base)))
      (perr "Final:")
      (perr (with-out-str (pprint final))))
    (when (:curl options)
      (perr (string/join " " (map shellescape/quote-str curl-command))))
    (when-not (:dry-run options)
      (check (process curl-command {:inherit true :shutdown destroy-tree})))
    nil))
