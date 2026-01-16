(ns roughtime-client.core
  (:gen-class)
  (:require
   [clojure.pprint :refer [pprint]]
   [roughtime-protocol.config   :refer [ecosystem]]
   [roughtime-protocol.tlv      :as tlv]
   [roughtime-client.cli        :as cli]
   [roughtime-client.request    :as rq]
   [roughtime-client.chain      :as ch]))

(set! *warn-on-reflection* true)

(defn- normalize-address
  "nest keys `:protocol` and `:address` under `:addresses`"
  [server-map]
  (assoc (dissoc server-map :protocol :address)
         :addresses [(select-keys server-map [:protocol :address])]))

(defn process-request
  [cfg server-map]
  (let [{:keys [ok? exchange error results]} (rq/run-single-request
                                              server-map
                                              {:nonce nil})]

    (when ok?
      (when (:print-request cfg)
        (println "==== REQUEST ====")
        (pprint (tlv/print-rt-message-recursive (:request exchange)))
        (println))
      (when (:print-response cfg)
        (println "==== RESPONSE ====")
        (pprint (tlv/print-rt-message-recursive (:parsed exchange)))
        (println))
      (let [{:keys [print-request print-response]} cfg]
        (when (or print-request print-response)
          (println "==== RESULT ====")))
      (pprint results))

    (when-not ok?
      (println (format "Caught error `%s`:" (ex-message error)))
      (pprint (ex-data error)))))

(defn -main [& argv]

  (let [cfg (cli/parse! (vec argv))
        server-map (if-let [nm (:server-name cfg)]
                     (get ecosystem nm)
                     (normalize-address cfg))]

    (if (:chain cfg)
      (ch/run-chained-request-default)
      (process-request cfg server-map))))
