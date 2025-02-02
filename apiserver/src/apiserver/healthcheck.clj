; Copyright 2018-2022 Rahul De
;
; Use of this source code is governed by an MIT-style
; license that can be found in the LICENSE file or at
; https://opensource.org/licenses/MIT.

(ns apiserver.healthcheck
  (:require [failjure.core :as f]
            [xtdb.api :as xt]
            [taoensso.timbre :as log]
            [java-http-clj.core :as http])
  (:import [java.util.concurrent Executors TimeUnit]))

(defn queue
  [{:keys [queue]}]
  (when (not (.isOpen queue))
    (f/fail "Queue is unreachable")))

(defn db
  [{:keys [db]}]
  (when (not (xt/status db))
    (f/fail "DB is unhealthy")))

(defn check-entity
  [{:keys [name url]}]
  (f/try-all [{status :status} (http/get (str url "/ping"))]
    (if (>= status 400)
      (f/fail (format "Error checking %s at %s"
                      name
                      url))
      "Ok")
    (f/when-failed [_]
      (f/fail (format "Error checking %s at %s"
                      name
                      url)))))

(defn check-entities
  [{db :db}]
  (let
    [result
     (xt/q
       (xt/db db)
       '{:find  [(pull entity [:name :url])]
         :where [(or
                   [entity :type :artifact-store]
                   [entity :type :resource-provider])]})]
    (->> result
         (map first)
         (map check-entity)
         (filter f/failed?)
         (into []))))

(defn check
  [opts]
  (let [results (->> [queue db check-entities]
                     (pmap #(% opts))
                     (doall)
                     (flatten)
                     (filter f/failed?))]
    (when (seq results)
      (run! #(log/errorf "Health checks failing: %s" (f/message %)) results)
      (f/fail results))))

(defn schedule
  [queue database health-check-freq]
  (.scheduleAtFixedRate (Executors/newScheduledThreadPool 1)
                        #(check {:queue queue :db database})
                        0
                        health-check-freq
                        TimeUnit/MILLISECONDS))

(comment
  (->> (into (list {:name "a1" :url "https://httpbin.org/get"})
             (list {:name "r1" :url "http://localhost:8000/ping"}))
       (map check-entity)))
