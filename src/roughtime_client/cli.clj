(ns roughtime-client.cli
  (:require
   [clojure.string :as string]
   [roughtime-protocol.config :refer [ecosystem]]
   [babashka.cli :as cli])
  (:import
   (java.util Base64)))

(set! *warn-on-reflection* true)

(def defaults
  {:protocol "udp"
   :msg-size 1024
   :print-request false
   :print-response false})

(defn- b64? [s]
  (try
    (when (string? s)
      (-> (Base64/getDecoder) (.decode ^String s))
      true)
    (catch Exception _ false)))

(defn- host:port? [s]
  (boolean (and (string? s)
                (re-matches #"^\[?[A-Za-z0-9\-\._:]+\]?:\d{1,5}$" s))))

(defn- protocol? [s]
  (or (nil? s) (#{"udp" "tcp"} (string/lower-case s))))

(defn- pos-long? [x]
  (and (integer? x) (pos? x)))

(defn- in-ecosystem? [name]
  (contains? ecosystem name))

(defn parse-version
  "Parse a version string.
   Accepts decimal (\"0\", \"12\") or hex (\"0x8000000c\").
   Returns a long."
  [s]
  (when-not (string? s)
    (throw (ex-info "Version must be a string" {:value s})))

  (let [s (string/trim s)]
    (cond
      (re-matches #"0[xX][0-9a-fA-F]+" s)
      (Long/parseUnsignedLong (subs s 2) 16)

      (re-matches #"\d+" s)
      (Long/parseLong s)

      :else
      (throw
       (ex-info
        "Invalid version format (expected decimal or hex)"
        {:value s})))))

(def spec
  {:spec
   {:msg-size
    {:coerce :long
     :desc "pad messages to this size in bytes"
     :alias :n
     :validate #(or (nil? %) (pos-long? %))}

    :print-request
    {:coerce :boolean
     :desc "pretty-print the request message"}

    :print-response
    {:coerce :boolean
     :desc "pretty-print the response message"}

    :server-name
    {:desc (format "named server in ecosystem [one of %s]"
                   (string/join
                    ", "
                    (map #(format "\"%s\"" %) (keys ecosystem))))
     :alias :s
     :validate in-ecosystem?}

    :version-no
    {:desc "roughtime version number (e.g. \"0x8000000c\")"
     :coerce parse-version
     :alias :V}

    :public-key
    {:desc "public key for server in base-64"
     :alias :k
     :validate b64?}

    :address
    {:desc "server address, including port number (e.g. \"roughtime.int08h.com:2002\")"
     :alias :a
     :validate host:port?}

    :protocol
    {:desc "server protocol [tcp or udp]"
     :alias :p
     :validate protocol?}

    :chain
    {:coerce :boolean
     :desc "chain requests to all servers in the ecosystem"}}})

(defn usage
  [spec]
  (cli/format-opts (merge spec {:order (vec (keys (:spec spec)))})))

(defn- have-triple? [opts]
  (every? #(some? (get opts %)) [:version-no :public-key :address]))

(defn- expand-server-name
  "If :server-name present, merge its connection details from ecosystem."
  [opts]
  (if-let [nm (:server-name opts)]
    (merge opts (select-keys (get ecosystem nm)
                             [:version-no :public-key :address :protocol :msg-size]))
    opts))

(defn- print-errors! [errors]
  (binding [*out* *err*]
    (doseq [e errors] (println e)))
  (println (usage spec))
  (flush)
  (Thread/sleep 10)
  (System/exit 1))

(defn parse!
  "Parse argv with babashka.cli, apply defaults & validation, check presence rule,
   expand :server-name, and return the final opts map."
  [argv]
  (let [{:keys [opts errors]} (cli/parse-args argv
                                              {:spec (:spec spec)
                                               :exec-args defaults})]
    (when (seq errors)
      (print-errors! errors))

    (when (or (:help opts) (:h opts))
      (println (usage spec))
      (flush)
      (Thread/sleep 10)
      (System/exit 0))

    ;; Presence rule: require either a known server-name OR the raw triple.
    (when (not (or (:chain opts) (:server-name opts) (have-triple? opts)))
      (print-errors!
       ["Error: provide either --server-name or all of --version-no --public-key --address."]))

    (when (:chain opts)
      (println "Running chain script; ignoring all other options.\n"))

    ;; Normalize (prefer the named server’s connection fields)
    (-> opts
        (update :protocol #(some-> % string/lower-case))
        (expand-server-name))))
