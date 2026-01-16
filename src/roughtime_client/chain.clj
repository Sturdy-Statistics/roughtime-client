(ns roughtime-client.chain
  (:require
   [clojure.pprint            :as    p]
   [roughtime-protocol.util   :refer [sha512-bytes]]
   [roughtime-protocol.config :refer [ecosystem]]
   [roughtime-protocol.compat :refer [required-nonce-length]]
   [roughtime-client.request  :as    rq]
   [taoensso.truss            :refer [have]]))

;;; chain responses by hashing response to make nonces

(defn- response->nonce
  [^bytes response-bytes
   next-server-map]
  (let [ver   (have integer? (:version-no next-server-map))
        len   (required-nonce-length ver)]
    (sha512-bytes len response-bytes)))

(defn- next-nonce
  [exchange next-server-map]
  (when exchange
    (let [response-bytes (have bytes? (get-in exchange [:resp-bytes :bytes]))]
      (response->nonce response-bytes next-server-map))))

(defn- get-intervals [results]
  (letfn [(interval [el] (let [srep  (get-in el [:exchange :parsed "SREP"])
                               midp  (have integer? (get-in el [:results :midp]))
                               radi' (have integer? (get srep "RADI"))
                               ;; hack: google version uses microseconds
                               ;; the midp value is already converted to seconds
                               radi  (if (>= radi' (* 1000 1000))
                                       (quot radi' (* 1000 1000))
                                       radi')]
                           [(- midp radi) (+ midp radi)]))]
    (mapv interval results)))

(defn- run-chained-requests
  [servers initial-nonce]
  (let [chain (reduce (fn [acc server]
                        (let [prev          (last acc)
                              exch          (when prev (:exchange prev))
                              current-nonce (or (next-nonce exch server) initial-nonce)
                              resp (rq/run-single-request server :nonce current-nonce)]
                          (if (:ok? resp)
                            (conj acc resp)
                            (reduced (conj acc (assoc resp :error "Chain broken"))))))
                      []
                      servers)]
    (if (every? :ok? chain)
      {:ok? true
       :chain chain}
      {:ok? false
       :results chain})))

;;; check timestamp consistency

(defn- check-consistency [results]
  (let [intervals (get-intervals results)
        max-start (apply max (map first intervals))
        min-end   (apply min (map second intervals))]
    (<= max-start min-end)))

(defn- find-culprit [results]
  (for [i (range (count results))
        :let [subset (keep-indexed #(when (not= %1 i) %2) results)]
        :when (check-consistency subset)]
    (nth results i)))

(defn- validate-chain [results]
  (if (check-consistency results)
    {:status :ok}
    (let [culprits (find-culprit results)]
      (cond
        (empty? culprits)
        {:status :failed-multiple
         :server nil}

        (= 1 (count culprits))
        {:status :bad-actor
         :server (first culprits)}

        :else
        (throw (ex-info "ambiguous status" {:possible-culprits culprits}))))))

;;; print a table

(defn- print-chain [chain]
  (letfn [(pubk [el] (have string?
                           (get-in el [:exchange :server-map :public-key])))
          (sname [el] (have string?
                            (get-in el [:exchange :server-map :name])))
          (lt [el] (have integer?
                         (get-in el [:results :local-time])))
          (exp [el] (have string?
                          (get-in el [:results :online-key-expires-in])))]
    (let [intervals (get-intervals chain)
          local-times (mapv lt chain)

          lo-vals (mapv #(- (first %1) %2) intervals local-times)
          hi-vals (mapv #(- (last %1) %2) intervals local-times)
          names (mapv sname chain)
          exps (mapv exp chain)
          pks (mapv pubk chain)]
      (p/print-table
       (map (fn [n p l h e] {:name n
                             :public-key p
                             :lower-limit l
                             :upper-limit h
                             :expires-in e})
            names
            pks
            lo-vals
            hi-vals
            exps)))))

;;; public API

(defn run-chained-request
  [servers initial-nonce]
  (letfn [(pubk [el] (have string?
                           (get-in el [:exchange :server-map :public-key])))
          (sname [el] (have string?
                            (get-in el [:exchange :server-map :name])))
          (throw-shade [server]
            (println (format "Single bad actor found:\n  name: %s\n  public key: %s\n"
                             (sname server)
                             (pubk server))))]
    (let [{:keys [ok? chain]} (run-chained-requests servers initial-nonce)]
      (when-not ok?
        (throw (ex-info "Chain broken" {:error (-> chain last :error)})))
      (let [{:keys [status server]} (validate-chain chain)]
        (case status
          :ok (println "All servers are consistent.")
          :bad-actor (throw-shade server)))
      (print-chain chain))))

(defn run-chained-request-default
  []
  (let [servers (mapv #(get ecosystem %)
                      ["int08h" "Cloudflare" "SturdyStatistics" "roughtime.se" "TXRyan"])]
    (run-chained-request servers nil)))
