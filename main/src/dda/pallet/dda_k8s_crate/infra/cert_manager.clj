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
(ns dda.pallet.dda-k8s-crate.infra.cert-manager
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]
   [dda.pallet.dda-k8s-crate.infra.check :as check]
   [dda.provision :as p]
   [dda.provision.pallet :as pp]))

(s/def CertManager {(s/optional-key :env-flag) s/Str
                    (s/optional-key :acme-flag) s/Str})

(def cert-manager "cert-manager")

(s/defn apply-cert-manager
  [apply-with-user
   user :- s/Str]
  (let [user-home-ca (str "home/" user "/ca")]
    (actions/exec-checked-script
     "create cert-manager ns"
     ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "create" "namespace" "cert-manager'")
     ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "label" "namespace"
             "cert-manager" "cert-manager.io/disable-validation=true'"))
    (apply-with-user "cert_manager/cert-manager.yml")
    (check/wait-until-pod-running user "webhook" 5 10 20)
    (apply-with-user "cert_manager/selfsigning-issuer.yml")
    (apply-with-user "cert_manager/le-issuer.yml")))

(s/defn user-configure-cert-manager
  [facility user config]
  (let [facility-name (name facility)]
    (p/provision-log ::pp/pallet facility-name cert-manager 
                     ::p/info "user-configure-cert-manager")
    (p/copy-resources-to-user
     ::pp/pallet user facility-name cert-manager
     [{:filename "cert-manager.yml"}
      {:filename "selfsigning-issuer.yml"}
      {:filename "le-issuer.yml" :config config}
      {:filename "install.sh"}
      {:filename "wait-for-pod.sh"}
      {:filename "verify.sh"}])
    (p/exec-file-on-target-as-user
     ::pp/pallet user facility-name cert-manager "install.sh")))
