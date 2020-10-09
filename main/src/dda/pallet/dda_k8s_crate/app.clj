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
   [dda.pallet.dda-k8s-crate.convention :as convention]))

(def with-k8s infra/with-k8s)

(def k8sConvention convention/k8sConvention)

(def k8sConventionResolved convention/k8sConventionResolved)

(def InfraResult convention/InfraResult)

(def k8sApp
  {:group-specific-config
   {s/Keyword (merge InfraResult
                     user/InfraResult)}})

(s/defn ^:always-validate
  app-configuration-resolved :- k8sApp
  [resolved-convention-config :- k8sConventionResolved
   & options]
  (let [{:keys [group-key] :or {group-key infra/facility}} options]
    (mu/deep-merge
     (user/app-configuration-resolved
      (convention/user-domain-configuration resolved-convention-config) :group-key group-key)
     {:group-specific-config
      {group-key
       (convention/infra-configuration resolved-convention-config)}})))

(s/defn ^:always-validate
  app-configuration :- k8sApp
  [convention-config :- k8sConvention
   & options]
  (let [resolved-convention-config (secret/resolve-secrets convention-config k8sConvention)]
    (apply app-configuration-resolved resolved-convention-config options)))

(s/defmethod ^:always-validate
  core-app/group-spec infra/facility
  [crate-app
   convention-config :- k8sConventionResolved]
  (let [app-config (app-configuration-resolved convention-config)]
    (core-app/pallet-group-spec
     app-config [(config-crate/with-config app-config)
                 user/with-user
                 with-k8s])))

(def crate-app (core-app/make-dda-crate-app
                :facility infra/facility
                :convention-schema k8sConvention
                :convention-schema-resolved k8sConventionResolved
                :default-convention-file "k8s.edn"))
