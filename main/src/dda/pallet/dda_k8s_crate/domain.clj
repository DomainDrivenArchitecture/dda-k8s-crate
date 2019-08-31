; Licensed to the Apache Software Foundation (ASF) under one
; or more contributor license agreements. See the NOTICE file
; distributed with this work for additional information
; regarding copyright ownership. The ASF licenses this file
; to you under the Apache License, Version 2.0 (the
; "License"); you may not use this file except in compliance
; with the License. You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
(ns dda.pallet.dda-k8s-crate.domain
  (:require
   [schema.core :as s]
   [dda.pallet.commons.secret :as secret]
   [dda.pallet.dda-k8s-crate.infra.kubectl :as kubectl]
   [dda.pallet.dda-k8s-crate.infra :as infra]
   [clojure.java.io :as io]
   [dda.pallet.dda-k8s-crate.domain.templating :as templating]
   [selmer.parser :as selmer]))

(def k8sDomain
  {})

(def k8sDomainResolved (secret/create-resolved-schema k8sDomain))

(def InfraResult {infra/facility infra/k8sInfra})

(s/defn ^:always-validate
  infra-configuration
  [domain-config :- k8sDomainResolved]
  (let [{:keys []} domain-config]
    (infra/facility)))

; Print all yml files, iterate over them and replace with selmer and create 
; mapping between filename and string in sequence

(defn list-files-in-dir
  "Lists all the filenames found in a directory"
  [path-to-dir]
  (file-seq
   (clojure.java.io/file "/home/krj/repo/dda-pallet/dda-k8s-crate/main/resources")))

(defn test
  ; Filters all files without extension
  []
  (for [file-name (filter #(.isFile %) (list-files-in-dir "bla"))]
    (print (keyword (apply str (drop-last 4 (.getName file-name)))))))

(defn create-map-with-path-content-mapping-helper
  [map]
  (for [file-name (filter #(.isFile %) (list-files-in-dir "bla"))]
    (assoc (keyword (apply str (drop-last 4 (.getName file-name)))))))

(defn create-map-with-path-content-mapping
  []
  (create-map-with-path-content-mapping-helper {}))

  ;(selmer/render-file
   ;(.getAbsolutePath file-name)
   ;(get templating/template-associate-map
    ;    (keyword (apply str (drop-last 4 (.getName file-name))))))