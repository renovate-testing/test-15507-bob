; Copyright 2018-2022 Rahul De
;
; Use of this source code is governed by an MIT-style
; license that can be found in the LICENSE file or at
; https://opensource.org/licenses/MIT.

(ns runner.artifact
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as s]
            [taoensso.timbre :as log]
            [java-http-clj.core :as http]
            [xtdb.api :as xt]
            [failjure.core :as f]
            [common.schemas]
            [runner.engine :as eng]))

(defn store-url
  [db-client store]
  (f/try-all [store (xt/entity (xt/db db-client) (keyword (str "bob.artifact-store/" store)))
              _ (when-not (spec/valid? :bob.db/artifact-store store)
                  (f/fail "Invalid artifact store: " store))]
    (:url store)
    (f/when-failed [err]
      err)))

(defn upload-artifact
  "Opens up a stream to the path in a container by id and POSTs it to the artifact store."
  [db-client group name run-id artifact-name container-id path store-name]
  (if-let [url (store-url db-client store-name)]
    (f/try-all [_ (log/debugf "Streaming from container %s on path %s"
                              container-id
                              path)
                stream     (eng/get-container-archive container-id path)
                upload-url (s/join "/" [url "bob_artifact" group name run-id artifact-name])
                _ (log/infof "Uploading artifact %s for pipeline %s run %s to %s"
                             artifact-name
                             name
                             run-id
                             upload-url)
                _ (http/post upload-url
                             {:body stream
                              :as   :input-stream})]
      "Ok"
      (f/when-failed [err]
        (log/errorf "Error in uploading artifact: %s" (f/message err))
        (f/try*
          (eng/delete-container container-id))
        err))
    (do
      (log/error "Error locating Artifact Store")
      (f/try*
        (eng/delete-container container-id))
      (f/fail "No such artifact store registered"))))

(comment
  (http/post "http://localhost:8001/bob_artifact/dev/test/r-1/tar"
             {:body (clojure.java.io/input-stream "test/test.tar")
              :as   :input-stream}))
