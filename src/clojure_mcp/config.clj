(ns clojure-mcp.config
  (:require
   [clojure.java.io :as io]
   [clojure-mcp.nrepl :as nrepl]
   [clojure.edn :as edn]
   [clojure.tools.logging :as log]))

(defn- relative-to [dir path]
  (try
    (let [f (io/file path)]
      (if (.isAbsolute f)
        (.getCanonicalPath f)
        (.getCanonicalPath (io/file dir path))))
    (catch Exception e
      (log/warn "Bad file paths " (pr-str [dir path]))
      nil)))

(defn process-config [{:keys [allowed-directories emacs-notify write-file-guard cljfmt bash-over-nrepl nrepl-env-type] :as config} user-dir]
  (let [ud (io/file user-dir)]
    (assert (and (.isAbsolute ud) (.isDirectory ud)))
    (when (some? write-file-guard)
      (when-not (contains? #{:full-read :partial-read false} write-file-guard)
        (log/warn "Invalid write-file-guard value:" write-file-guard
                  "- using default :full-read")
        (throw (ex-info (str "Invalid Config: write-file-guard value:  " write-file-guard
                             "- must be one of (:full-read, :partial-read, false)")
                        {:write-file-guard write-file-guard}))))
    (cond-> config
      user-dir (assoc :nrepl-user-dir (.getCanonicalPath ud))
      true
      (assoc :allowed-directories
             (->> (cons user-dir allowed-directories)
                  (keep #(relative-to user-dir %))
                  distinct
                  vec))
      (some? (:emacs-notify config))
      (assoc :emacs-notify (boolean (:emacs-notify config)))
      (some? (:cljfmt config))
      (assoc :cljfmt (boolean (:cljfmt config)))
      (some? (:bash-over-nrepl config))
      (assoc :bash-over-nrepl (boolean (:bash-over-nrepl config)))
      (some? (:nrepl-env-type config))
      (assoc :nrepl-env-type (:nrepl-env-type config)))))

(defn load-config
  "Loads configuration from .clojure-mcp/config.edn in the given directory.
   Reads the file directly from the filesystem."
  [cli-config-file user-dir]
  (let [config-file (if cli-config-file
                      (io/file cli-config-file)
                      (io/file user-dir ".clojure-mcp" "config.edn"))
        config (if (.exists config-file)
                 (try
                   (edn/read-string (slurp config-file))
                   (catch Exception e
                     (log/warn e "Failed to read config file:" (.getPath config-file))
                     {}))
                 {})
        processed-config (process-config config user-dir)]
    (log/info "Config file:" (.getPath config-file) "exists:" (.exists config-file))
    (log/info "Raw config:" config)
    (log/info "Processed config:" processed-config)
    processed-config))

(defn get-config [nrepl-client-map k]
  (get-in nrepl-client-map [::config k]))

(defn get-allowed-directories [nrepl-client-map]
  (get-config nrepl-client-map :allowed-directories))

(defn get-emacs-notify [nrepl-client-map]
  (get-config nrepl-client-map :emacs-notify))

(defn get-nrepl-user-dir [nrepl-client-map]
  (get-config nrepl-client-map :nrepl-user-dir))

(defn get-cljfmt [nrepl-client-map]
  (let [value (get-config nrepl-client-map :cljfmt)]
    (if (nil? value)
      true ; Default to true when not specified
      value)))

(defn get-write-file-guard [nrepl-client-map]
  (let [value (get-config nrepl-client-map :write-file-guard)]
    ;; Validate the value and default to :full-read if invalid
    (cond
      ;; nil means not configured, use default
      (nil? value) :full-read
      ;; Valid values (including false)
      (contains? #{:full-read :partial-read false} value) value
      ;; Invalid values
      :else (do
              (log/warn "Invalid write-file-guard value:" value "- using default :full-read")
              :full-read))))

(defn get-bash-over-nrepl
  "Returns whether bash commands should be executed over nREPL.
   Defaults to true for compatibility."
  [nrepl-client-map]
  (let [value (get-config nrepl-client-map :bash-over-nrepl)]
    (if (nil? value)
      true ; Default to true when not specified
      (boolean value))))

(defn get-nrepl-env-type
  "Returns the nREPL environment type.
   Defaults to :clj if not specified."
  [nrepl-client-map]
  (let [value (get-config nrepl-client-map :nrepl-env-type)]
    (if (nil? value)
      :clj ; Default to :clj when not specified
      value)))

(defn clojure-env?
  "Returns true if the nREPL environment is a Clojure environment."
  [nrepl-client-map]
  (= :clj (get-nrepl-env-type nrepl-client-map)))

(defn write-guard?
  "Returns true if write-file-guard is enabled (not false).
   This means file timestamp checking is active."
  [nrepl-client-map]
  (not= false (get-write-file-guard nrepl-client-map)))

(defn get-scratch-pad-load
  "Returns whether scratch pad persistence is enabled.
   Defaults to false when not specified."
  [nrepl-client-map]
  (let [value (get-config nrepl-client-map :scratch-pad-load)]
    (if (nil? value)
      false                      ; Default to false when not specified
      (boolean value))))

(defn get-scratch-pad-file
  "Returns the scratch pad filename.
   Defaults to 'scratch_pad.edn' when not specified."
  [nrepl-client-map]
  (let [value (get-config nrepl-client-map :scratch-pad-file)]
    (if (nil? value)
      "scratch_pad.edn" ; Default filename
      value)))

(defn set-config*
  "Sets a config value in a map. Returns the updated map.
   This is the core function that set-config! uses."
  [nrepl-client-map k v]
  (assoc-in nrepl-client-map [::config k] v))

(defn set-config!
  "Sets a config value in an atom containing an nrepl-client map.
   Uses set-config* to perform the actual update."
  [nrepl-client-atom k v]
  (swap! nrepl-client-atom set-config* k v))


