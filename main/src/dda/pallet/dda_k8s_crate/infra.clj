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
   [dda.pallet.dda-k8s-crate.infra.k8s :as k8s]
   [clojure.tools.logging :as logging]
   [pallet.actions :as actions]))

(def facility :dda-k8s)

; the infra config
(def ddaK8sConfig
  {:user s/Keyword
   :k8s k8s/k8s
   (s/optional-key :apple) {:fqdn s/Str}})

(s/defmethod core-infra/dda-init facility
  [dda-crate config]
  (let [facility (:facility dda-crate)
        {:keys [k8s]} config]
    (k8s/init facility k8s)))

(s/defmethod core-infra/dda-install facility
  [dda-crate config]
  (let [facility (:facility dda-crate)
        {:keys [k8s user]} config]
    (actions/as-action (logging/info (str facility " - core-infra/dda-install")))
    (logging/info config)
    (base/install facility)
    (k8s/system-install facility k8s)
    (k8s/user-install facility (name user) k8s)))

(s/defmethod core-infra/dda-configure facility
  [dda-crate config]
  (let [facility (:facility dda-crate)
        {:keys [k8s user]} config]
    (k8s/system-configure facility k8s)
    (k8s/user-configure facility (name user) k8s)))

(def dda-k8s-crate
  (core-infra/make-dda-crate-infra
   :facility facility
   :infra-schema ddaK8sConfig))

(def with-k8s
  (core-infra/create-infra-plan dda-k8s-crate))
