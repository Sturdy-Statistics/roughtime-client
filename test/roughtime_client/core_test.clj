(ns roughtime-client.core-test
  (:require
   [clojure.pprint :refer [pprint]]
   [roughtime-client.core :as c]
   [roughtime-protocol.config   :refer [ecosystem]]))

(def server-names
  ["TXRyan"
   "Cloudflare"
   "Cloudflare-goog"
   "int08h"
   "int08h-goog"
   "roughtime.se"
   "SturdyStatistics"
   "SturdyStatistics-goog"])

(doseq [server server-names]
  (let [server-map (get ecosystem server)]
    (println (format "Testing Server '%s'..." server))
    (pprint (c/process-request nil server-map))))
