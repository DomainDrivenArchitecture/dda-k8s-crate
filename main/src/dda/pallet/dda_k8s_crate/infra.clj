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
    [dda.pallet.dda-k8s-crate.infra.k8s :as k8s]))

(def facility :dda-k8s)

(def k8sInfra k8s/k8sInfra)

(defn- install-microk8s
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: microk8s"))))
  ; sudo -if
  ; snap install microk8s --classic)
  ; microk8s.kubectl as kubectl
  ; microk8s.enable dns dashboard storage ingress metrics-server
  ; kubectl -n kube-system edit service kubernetes-dashboard
  ; --> type: ClusterIP to type: NodePort

; view services: microk8s.kubectl get all --all-namespaces
; view dashboard-port: kubectl -n kube-system get service kubernetes-dashboard

; microk8s.stop



(s/defmethod core-infra/dda-install facility
  [core-infra config]
  (install-microk8s facility))

(def dda-k8s-crate
  (core-infra/make-dda-crate-infra
    :facility facility))

(def with-k8s
  (core-infra/create-infra-plan dda-k8s-crate))
