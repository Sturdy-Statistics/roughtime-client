(ns roughtime-client.request
  (:require
   [roughtime-protocol.client :refer [make-request validate-response process-time]])
  (:import
   (clojure.lang ExceptionInfo)))

(set! *warn-on-reflection* true)

(defn run-single-request
  [server-map & {:keys [nonce]}]
  (let [opts     (cond-> {} nonce (assoc :nonce nonce))
        exchange (make-request server-map opts)]
    (try
      (when (validate-response exchange)
        (let [results (process-time exchange)]
          {:ok? true
           :exchange exchange
           :results results}))
      (catch ExceptionInfo ex
        {:ok? false
         :error ex}))))
