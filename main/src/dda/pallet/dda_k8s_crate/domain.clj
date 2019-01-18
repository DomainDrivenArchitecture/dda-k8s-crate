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
    [dda.pallet.dda-k8s-crate.domain.k8s :as k8s]
    [dda.pallet.dda-k8s-crate.domain.git :as git]
    [dda.pallet.dda-k8s-crate.domain.user :as user]
    [dda.pallet.dda-k8s-crate.domain.httpd :as httpd]
    [dda.pallet.dda-k8s-crate.infra :as infra]))

(def k8sDomain
  {})

(def k8sDomainResolved (secret/create-resolved-schema k8sDomain))

(def InfraResult {infra/facility infra/k8sInfra})

(s/defn ^:always-validate
  infra-configuration
  [domain-config :- k8sDomainResolved]
  (let [{:keys []} domain-config]
    (k8s/k8s-infra-configuration
     infra/facility)))
