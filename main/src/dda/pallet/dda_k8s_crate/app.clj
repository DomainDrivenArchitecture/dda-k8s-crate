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

(ns dda.pallet.dda-k8s-crate.app
  (:require
   [schema.core :as s]
   [dda.pallet.commons.secret :as secret]
   [dda.config.commons.map-utils :as mu]
   [dda.pallet.core.app :as core-app]
   [dda.pallet.dda-config-crate.infra :as config-crate]
   [dda.pallet.dda-user-crate.app :as user]
   [dda.pallet.dda-k8s-crate.infra :as infra]
   [dda.pallet.dda-k8s-crate.domain :as domain]))

(def with-k8s infra/with-k8s)

(def k8sDomainConfig domain/k8sDomain)

(def k8sDomainResolvedConfig domain/k8sDomainResolved)

(def InfraResult domain/InfraResult)

(def k8sAppConfig
  {:group-specific-config
   {s/Keyword (merge InfraResult
                     user/InfraResult)}})

(s/defn ^:always-validate
  app-configuration-resolved :- k8sAppConfig
  [resolved-domain-config :- k8sDomainResolvedConfig
   & options]
  (let [{:keys [group-key] :or {group-key infra/facility}} options]
    (mu/deep-merge
     (user/app-configuration-resolved
      (domain/user-domain-configuration resolved-domain-config) :group-key group-key)
     {:group-specific-config
      {group-key
       (domain/infra-configuration resolved-domain-config)}})))

(s/defn ^:always-validate
  app-configuration :- k8sAppConfig
  [domain-config :- k8sDomainConfig
   & options]
  (let [resolved-domain-config (secret/resolve-secrets domain-config k8sDomainConfig)]
    (apply app-configuration-resolved resolved-domain-config options)))

(s/defmethod ^:always-validate
  core-app/group-spec infra/facility
  [crate-app
   domain-config :- k8sDomainResolvedConfig]
  (let [app-config (app-configuration-resolved domain-config)]
    (core-app/pallet-group-spec
     app-config [(config-crate/with-config app-config)
                 user/with-user
                 with-k8s])))

(def crate-app (core-app/make-dda-crate-app
                :facility infra/facility
                :domain-schema k8sDomainConfig
                :domain-schema-resolved k8sDomainResolvedConfig
                :default-domain-file "k8s.edn"))
