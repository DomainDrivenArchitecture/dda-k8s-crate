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
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]))

(s/def Nexus {:fqdn s/Str :secret-name s/Str :cluster-issuer s/Str})

(def nexus "nexus")

(s/defn user-install-nexus
  [facility user config]
  (let [facility-name (name facility)]
    (transport/log-info facility-name "user-install-nexus")
    (transport/copy-resources-to-tmp
     facility-name nexus
     [{:filename "create-storage.sh" :config {:user user}}])
    (transport/exec
     facility-name nexus "create-storage.sh")))

(s/defn user-configure-nexus
  [facility user config]
  (let [facility-name (name facility)]
    (transport/log-info facility-name "user-configure-nexus")
    (transport/copy-resources-to-user
     user facility-name nexus
     [{:filename "ingress_nexus_https.yml" :config config}
      {:filename "nexus-storage.yml"}
      {:filename "nexus.yml"}
      {:filename "remove.sh"}
      {:filename "verify.sh" :config config}
      {:filename "install.sh"}])
    (transport/exec-as-user
     user facility-name nexus "install.sh")))
