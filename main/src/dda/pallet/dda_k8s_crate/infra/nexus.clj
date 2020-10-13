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
(ns dda.pallet.dda-k8s-crate.infra.nexus
  (:require
   [schema.core :as s]
   [dda.provision :as p]
   [dda.provision.pallet :as pp]))

(s/def Nexus {:fqdn s/Str :secret-name s/Str :cluster-issuer s/Str})

(def nexus "nexus")

(s/defn user-install-nexus
  [facility user config]
  (let [facility-name (name facility)]
    (p/provision-log ::pp/pallet facility-name nexus ::p/info "user-install-nexus")
    (p/copy-resources-to-tmp
     ::pp/pallet facility-name nexus
     [{:filename "create-storage.sh" :config {:user user}}])
    (p/exec-file-on-target-as-root
     ::pp/pallet facility-name nexus "create-storage.sh")))

(s/defn user-configure-nexus
  [facility user config]
  (let [facility-name (name facility)]
    (p/provision-log ::pp/pallet facility-name nexus ::p/info "user-configure-nexus")
    (p/copy-resources-to-user
     ::pp/pallet user facility-name nexus
     [{:filename "ingress_nexus_https.yml" :config config}
      {:filename "nexus-storage.yml"}
      {:filename "nexus.yml"}
      {:filename "remove.sh"}
      {:filename "verify.sh" :config config}
      {:filename "install.sh"}])
    (p/exec-file-on-target-as-user
     ::pp/pallet user facility-name nexus "install.sh")))
