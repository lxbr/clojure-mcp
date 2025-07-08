(ns clojure-mcp.main-examples.clr-main
  "Example of a custom MCP server that adds Clojure CLR evaluation via dual connection.
   
   This demonstrates the dual connection pattern for Clojure CLR:
   1. Primary connection: JVM Clojure nREPL (for MCP infrastructure)
   2. Secondary connection: CLR Clojure nREPL (for CLR-specific evaluation)
   
   The CLR connection uses the existing dual connection architecture,
   similar to Shadow CLJS dual connections, but targets Clojure CLR instead."
  (:require
   [clojure-mcp.core :as core]
   [clojure-mcp.main :as main]
   [clojure-mcp.tools.eval.tool :as eval-tool]
   [clojure-mcp.dialects :as dialects]
   [clojure.tools.logging :as log]))

(def clr-tool-name "clojure_clr_eval")

(def clr-description
  "Takes a Clojure CLR expression and evaluates it in the current namespace.
   
   **.NET Interop**: Full access to .NET Framework/Core APIs via (System.Console/WriteLine \"Hello\")
   
   **Project File Access**: Can load and use any Clojure CLR file from your project with `(require '[your-namespace.core :as core] :reload)`. Always use `:reload` to ensure you get the latest version of files.
   
   **CLR-Specific Features**:
   - Access to .NET assemblies and types
   - Full CLR interop capabilities
   - System.Environment and System.IO APIs
   - Assembly loading and type inspection
   
   **IMPORTANT**: This REPL is intended for CLOJURE CLR CODE only.")

(defn initialize-clr-connection
  "Initialize the CLR connection with CLR-specific dialect setup."
  [clr-nrepl-client-map]
  (log/info "Initializing CLR connection with CLR dialect")
  (-> clr-nrepl-client-map
      (dialects/initialize-environment :clr)
      (dialects/load-repl-helpers :clr)))

(defn clr-eval-tool 
  "Creates a CLR evaluation tool using a secondary nREPL connection."
  [nrepl-client-atom {:keys [clr-port] :as config}]
  (let [clr-nrepl-client-map (core/create-additional-connection 
                              nrepl-client-atom 
                              {:port clr-port}
                              initialize-clr-connection)
        clr-nrepl-client-atom (atom clr-nrepl-client-map)]
    (-> (eval-tool/eval-code clr-nrepl-client-atom)
        (assoc :name clr-tool-name)
        (assoc :description clr-description))))

(defn make-tools 
  "Creates the tools list, conditionally adding CLR evaluation if dual ports are configured."
  [nrepl-client-atom working-directory & [{:keys [port clr-port] :as config}]]
  (if (and port clr-port (not= port clr-port))
    (conj (main/make-tools nrepl-client-atom working-directory)
          (clr-eval-tool nrepl-client-atom config))
    (main/make-tools nrepl-client-atom working-directory)))

(defn start-mcp-server 
  "Starts the MCP server with CLR dual connection support."
  [opts]
  (core/build-and-start-mcp-server
   opts
   {:make-tools-fn (fn [nrepl-client-atom working-directory]
                     (make-tools nrepl-client-atom working-directory opts))
    :make-prompts-fn main/make-prompts
    :make-resources-fn main/make-resources}))