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
   [dda.pallet.core.infra :as core-infra]
   [dda.pallet.dda-k8s-crate.infra.base :as base]
   [dda.pallet.dda-k8s-crate.infra.kubectl :as kubectl]))

(def facility :dda-k8s)

; the infra config
(def ddaK8sConfig
  {:kubectl-config kubectl/kubectl-config})

(s/defmethod core-infra/dda-init facility
  [dda-crate config]
  (let [facility (:facility dda-crate)
        {:keys [kubectl-config]} config]
    (kubectl/init facility kubectl-config)))

(s/defmethod core-infra/dda-install facility
  [dda-crate config]
  (let [facility (:facility dda-crate)
        {:keys [kubectl-config dda-user]} config]
    (base/install facility)
    (kubectl/system-install facility kubectl-config)
    (kubectl/user-install facility (:name dda-user) kubectl-config)))

(s/defmethod core-infra/dda-configure facility
  [dda-crate config]
  (let [facility (:facility dda-crate)
        {:keys [kubectl-config dda-user]} config]
    (kubectl/system-configure facility kubectl-config)
    (kubectl/user-configure facility (:name dda-user) kubectl-config)))

(def dda-k8s-crate
  (core-infra/make-dda-crate-infra
   :facility facility
   :infra-schema ddaK8sConfig))

(def with-k8s
  (core-infra/create-infra-plan dda-k8s-crate))
