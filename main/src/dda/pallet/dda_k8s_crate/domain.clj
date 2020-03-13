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
   [dda.config.commons.map-utils :as mu]
   [dda.pallet.dda-k8s-crate.infra :as infra]
   [clojure.string :as str]))

(def InfraResult {infra/facility infra/ddaK8sConfig})

(s/def k8sDomain
  {:user s/Keyword
   :k8s {:external-ip s/Str (s/optional-key :external-ipv6) s/Str}
   :cert-manager (s/enum :letsencrypt-prod-issuer :letsencrypt-staging-issuer :selfsigned-issuer)
   (s/optional-key :apple) {:fqdn s/Str}
   (s/optional-key :nexus) {:fqdn s/Str}})

(def k8sDomainResolved (secret/create-resolved-schema k8sDomain))

(def InfraResult {infra/facility infra/ddaK8sConfig})

(defn- rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(s/defn ^:always-validate user-domain-configuration
  [domain-config :- k8sDomainResolved]
  (let [{:keys [user]} domain-config]
    {user
     (merge
      {:clear-password (rand-str 10)
       :settings #{:bashrc-d}})}))

(defn- letsencrypt-configuration [letsencrypt-issuer]
  (if (= letsencrypt-issuer :letsencrypt-prod-issuer)
    {:env-flag "prod" :acme-flag ""}
    {:env-flag "staging" :acme-flag "-staging"}))

(s/defn ^:always-validate
  infra-configuration :- InfraResult
  [domain-config :- k8sDomainResolved]
  (let [{:keys [user k8s cert-manager apple nexus]} domain-config
        {:keys [external-ip external-ipv6]} k8s
        cluster-issuer (name cert-manager)
        cert-config (when (not (= cert-manager :selfsigned-issuer)) (letsencrypt-configuration cert-manager))]
    {infra/facility
     (mu/deep-merge
      {:user user
       :networking {:advertise-ip "192.168.5.1"}
       :k8s (merge {:external-ip (str "-   " external-ip "/32")}
                   {:external-ipv6 (if external-ipv6 (str "-   " external-ipv6 "/64") "")})}
      (if cert-config {:cert-manager cert-config} {:cert-manager {}})
      (when apple {:apple (merge
                           apple {:secret-name (str/replace (:fqdn apple) #"\." "-")
                                  :cluster-issuer cluster-issuer})})
      (when nexus {:nexus (merge
                           nexus {:secret-name (str/replace (:fqdn nexus) #"\." "-")
                                  :cluster-issuer cluster-issuer})}))}))

