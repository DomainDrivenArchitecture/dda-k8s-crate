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

(ns dda.pallet.dda-k8s-crate.infra
  (:require
   [schema.core :as s]
   [clojure.tools.logging :as logging]
   [pallet.actions :as actions]
   [dda.pallet.core.infra :as core-infra]
   [selmer.parser :as selmer]
   [dda.pallet.dda-k8s-crate.infra.kubectl :as kubectl]))

(def facility :dda-k8s)

; the infra config
(s/def k8sInfra
  ;TODO: I think we have somewhere a shema excactly for IPs
  {:external-ip s/Str})

(selmer/render-file "metallb_config.yml" {:external-ip "test"})

(defn- install-k8s
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: kubeadm")))
  ;(kubectl/install-kubernetes-apt-repositories)
  (kubectl/move-yaml-to-server {:external-ip "123"} "k8s"))

(s/defmethod core-infra/dda-install facility
  [core-infra config]
  (install-k8s facility))

(def dda-k8s-crate
  (core-infra/make-dda-crate-infra
   :facility facility))

(def with-k8s
  (core-infra/create-infra-plan dda-k8s-crate))
