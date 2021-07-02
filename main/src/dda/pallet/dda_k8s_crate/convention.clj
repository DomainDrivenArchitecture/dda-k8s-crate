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
(ns dda.pallet.dda-k8s-crate.convention
  (:require
   [schema.core :as s]
   [dda.pallet.commons.secret :as secret]
   [dda.config.commons.map-utils :as mu]
   [dda.pallet.dda-k8s-crate.infra :as infra]
   [clojure.string :as str]))

(def InfraResult {infra/facility infra/ddaK8sConfig})

(s/def k8sConvention
  {:user s/Keyword
   :k8s {:external-ip s/Str (s/optional-key :external-ipv6) s/Str 
         (s/optional-key :advertise-address) s/Str
         (s/optional-key :u18-04) (s/enum true)}
   :cert-manager (s/enum :letsencrypt-prod-issuer :letsencrypt-staging-issuer :selfsigned-issuer)
   (s/optional-key :persistent-dir) s/Str
   (s/optional-key :apple) {:fqdn s/Str}
   (s/optional-key :nexus) {:fqdn s/Str}})

(def k8sConventionResolved (secret/create-resolved-schema k8sConvention))

(def InfraResult {infra/facility infra/ddaK8sConfig})

(defn- rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(s/defn ^:always-validate user-domain-configuration
  [domain-config :- k8sConventionResolved]
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
  [domain-config :- k8sConventionResolved]
  (let [{:keys [user k8s cert-manager apple nexus persistent-dir]} domain-config
        {:keys [external-ip external-ipv6 advertise-address u18-04]} k8s
        cluster-issuer (name cert-manager)
        cert-config (when (not (= cert-manager :selfsigned-issuer)) 
                      (letsencrypt-configuration cert-manager))
        advertise-address (or advertise-address "192.168.5.1")
        os-version (if u18-04 :18.04 :20.04)]
    {infra/facility
     (mu/deep-merge
      {:user user
       :networking {:advertise-ip advertise-address
                    :os-version os-version}
       :k8s (merge {:external-ip (str "-   " external-ip "/32")}
                   {:external-ipv6 (if external-ipv6 (str "-   " external-ipv6 "/64") "")}
                   {:advertise-address advertise-address})}
      (if cert-config {:cert-manager cert-config} {:cert-manager {}})
      (when persistent-dir {:persistent-dir persistent-dir})
      (when apple {:apple (merge
                           apple {:secret-name (str/replace (:fqdn apple) #"\." "-")
                                  :cluster-issuer cluster-issuer})})
      (when nexus {:nexus (merge
                           nexus {:secret-name (str/replace (:fqdn nexus) #"\." "-")
                                  :cluster-issuer cluster-issuer})}))}))

