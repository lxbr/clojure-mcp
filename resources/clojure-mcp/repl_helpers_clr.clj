;; REPL Helper Functions for Clojure CLR MCP

(ns clj-mcp.repl-tools-clr
  "Namespace containing helper functions for CLR REPL-driven development"
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]))

;; Namespace exploration (same as JVM)
(defn list-ns
  "List all available namespaces, sorted alphabetically."
  []
  (let [namespaces (sort (map str (all-ns)))]
    (println "Available Namespaces:")
    (doseq [ns-name namespaces]
      (println (str "  " ns-name)))
    (println (str "\nTotal: " (count namespaces) " namespaces"))
    nil))

(defn list-vars
  "List all public vars in the given namespace with their arglists and docstrings.
   ns-name can be a symbol or string."
  [ns-name]
  (let [ns-obj (if (symbol? ns-name)
                 (find-ns ns-name)
                 (find-ns (symbol ns-name)))]
    (if ns-obj
      (let [vars (sort-by first (ns-publics ns-obj))]
        (println (str "Vars in " (ns-name ns-obj) ":"))
        (println (str "-------------------------------------------"))
        (doseq [[sym var] vars]
          (let [m (meta var)]
            (println (str (name sym)))
            (when-let [arglists (:arglists m)]
              (println (str "  " arglists)))
            (when-let [doc (:doc m)]
              (println (str "  " doc)))
            (println)))
        (println (str "Total: " (count vars) " vars")))
      (println (str "Error: Namespace not found: " ns-name)))
    nil))

;; CLR-specific file system helpers
(defn pwd 
  "Get current working directory using CLR APIs."
  []
  (System.Environment/get_CurrentDirectory))

(defn ls
  "List files in directory using CLR APIs."
  ([] (ls "."))
  ([path]
   (try
     (let [files (seq (System.IO.Directory/GetFiles path))
           dirs (seq (System.IO.Directory/GetDirectories path))]
       (println "Directories:")
       (doseq [dir dirs]
         (println (str "  " (System.IO.Path/GetFileName dir) "/")))
       (println "Files:")
       (doseq [file files]
         (println (str "  " (System.IO.Path/GetFileName file))))
       (println (str "\nTotal: " (+ (count dirs) (count files)) " items")))
     (catch Exception e
       (println (str "Error listing directory: " (.Message e)))))))

;; .NET assembly introspection
(defn assemblies
  "List all loaded assemblies in the current AppDomain."
  []
  (let [assemblies (seq (.GetAssemblies (System.AppDomain/get_CurrentDomain)))]
    (println "Loaded Assemblies:")
    (doseq [assembly assemblies]
      (println (str "  " (.GetName assembly))))
    (println (str "\nTotal: " (count assemblies) " assemblies"))
    nil))

(defn assembly-types
  "List all types in the specified assembly."
  [assembly-name]
  (try
    (let [assembly (System.Reflection.Assembly/LoadWithPartialName assembly-name)
          types (seq (.GetTypes assembly))]
      (println (str "Types in " assembly-name ":"))
      (doseq [type types]
        (println (str "  " (.FullName type))))
      (println (str "\nTotal: " (count types) " types")))
    (catch Exception e
      (println (str "Error loading assembly: " (.Message e))))))

;; CLR environment information
(defn clr-info
  "Show CLR environment information."
  []
  (println "CLR Environment Information:")
  (println (str "  CLR Version: " (System.Environment/get_Version)))
  (println (str "  OS Version: " (System.Environment/get_OSVersion)))
  (println (str "  Machine Name: " (System.Environment/get_MachineName)))
  (println (str "  User Name: " (System.Environment/get_UserName)))
  (println (str "  Current Directory: " (System.Environment/get_CurrentDirectory)))
  (println (str "  Working Set: " (System.Environment/get_WorkingSet) " bytes"))
  nil)

;; Symbol exploration (same as JVM)
(defn doc-symbol
  "Show documentation for a symbol. Accepts symbol or string."
  [sym]
  (if-let [v (resolve (if (symbol? sym) sym (symbol sym)))]
    (let [m (meta v)]
      (println (str "-------------------------"))
      (println (str (:name m) " - " (or (:doc m) "No documentation")))
      (println (str "  Defined in: " (:ns m)))
      (when-let [arglists (:arglists m)]
        (println (str "  Arguments: " arglists)))
      (when-let [added (:added m)]
        (println (str "  Added in: " added)))
      (when-let [deprecated (:deprecated m)]
        (println (str "  DEPRECATED: " deprecated)))
      (println (str "-------------------------")))
    (println (str "Error: Symbol not found: " sym)))
  nil)

(defn find-symbols
  "Find symbols matching the given pattern across all namespaces."
  [pattern]
  (let [matches (sort (map str (clojure.repl/apropos pattern)))]
    (println (str "Symbols matching '" pattern "':"))
    (doseq [sym matches]
      (println (str "  " sym)))
    (println (str "\nTotal: " (count matches) " matches"))
    nil))

(defn help
  "Show help for CLR REPL helper functions."
  []
  (println "CLR REPL Helper Functions:")
  (println "  clj-mcp.repl-tools-clr/list-ns           - List all available namespaces")
  (println "  clj-mcp.repl-tools-clr/list-vars         - List all vars in namespace")
  (println "  clj-mcp.repl-tools-clr/doc-symbol        - Show documentation for symbol")
  (println "  clj-mcp.repl-tools-clr/find-symbols      - Find symbols matching pattern")
  (println "  clj-mcp.repl-tools-clr/help              - Show this help message")
  (println)
  (println "CLR-Specific Functions:")
  (println "  clj-mcp.repl-tools-clr/pwd               - Get current working directory")
  (println "  clj-mcp.repl-tools-clr/ls                - List files and directories")
  (println "  clj-mcp.repl-tools-clr/assemblies        - List loaded assemblies")
  (println "  clj-mcp.repl-tools-clr/assembly-types    - List types in assembly")
  (println "  clj-mcp.repl-tools-clr/clr-info          - Show CLR environment info")
  (println)
  (println "Usage Examples:")
  (println "  (clj-mcp.repl-tools-clr/list-ns)                     ; List all namespaces")
  (println "  (clj-mcp.repl-tools-clr/pwd)                         ; Show current directory")
  (println "  (clj-mcp.repl-tools-clr/ls)                          ; List current directory")
  (println "  (clj-mcp.repl-tools-clr/assemblies)                  ; List loaded assemblies")
  (println "  (clj-mcp.repl-tools-clr/clr-info)                    ; Show CLR environment")
  (println "  (clj-mcp.repl-tools-clr/assembly-types \"System\")     ; List types in System assembly")
  (println)
  (println "For convenience, you can require the namespace with an alias:")
  (println "  (require '[clj-mcp.repl-tools-clr :as rtc])")
  (println "  (rtc/pwd)")
  (println)
  (println "To import all functions into the current namespace:")
  (println "  (use 'clj-mcp.repl-tools-clr)"))

;; Print loading message
(println "CLR REPL tools loaded in namespace clj-mcp.repl-tools-clr")
(println "Type (clj-mcp.repl-tools-clr/help) for more information")