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
   [selmer.parser :as selmer]
   [clojure.string :as str]))

(def InfraResult {infra/facility infra/ddaK8sConfig})

(s/def k8sDomain
  {:user s/Keyword
   :password secret/Secret
   (s/optional-key :ssh) {:ssh-authorized-keys [secret/Secret]
                          :ssh-key {:public-key secret/Secret
                                    :private-key secret/Secret}}
   :kubectl {:external-ip s/Str
             :host-name s/Str
             (s/optional-key :letsencrypt-prod) s/Bool
             :nexus-host-name s/Str}})

(def k8sDomainResolved (secret/create-resolved-schema k8sDomain))

(def InfraResult {infra/facility infra/ddaK8sConfig})

(s/defn ^:always-validate user-domain-configuration
  [domain-config :- k8sDomainResolved]
  (let [{:keys [user password ssh]} domain-config]
    {user
     (merge
      {:clear-password password
       :settings #{:bashrc-d}}
      (if ssh {:ssh-key (:ssh-key ssh)})
      (if ssh {:ssh-authorized-keys (:ssh-authorized-keys ssh)}))}))

(s/defn ^:always-validate
  infra-configuration :- InfraResult
  [domain-config :- k8sDomainResolved]
  (let [{:keys [user kubectl]} domain-config
        {:keys [external-ip host-name letsencrypt-prod nexus-host-name]} kubectl]
    {infra/facility
     {:user user
      :kubectl-config   {:external-ip external-ip
                         :host-name host-name
                         :letsencrypt-prod letsencrypt-prod   ; Letsencrypt environment: true -> prod | false -> staging
                         :nexus-host-name nexus-host-name
                         :nexus-secret-name (str/replace nexus-host-name #"\." "-")}}}))
