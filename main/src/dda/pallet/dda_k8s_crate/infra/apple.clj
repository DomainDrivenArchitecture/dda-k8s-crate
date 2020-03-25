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
(ns dda.pallet.dda-k8s-crate.infra.apple
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]))

(s/def Apple {:fqdn s/Str :secret-name s/Str :cluster-issuer s/Str})

(def apple "apple")

(s/defn apply-apple
  [user :- s/Str apply-with-user]
  (actions/directory
   "/mnt/data"
   :owner user
   :group user
   :mode "777")
  (apply-with-user "apple/apple.yml")
  (apply-with-user "apple/ingress_apple_https.yml"))

(s/defn user-configure-apple
  [facility user config]
  (let [facility-name (name facility)]
    (transport/log-info facility-name "(s/defn user-configure-apple")
    (transport/copy-resources-to-user
     user facility-name apple
     [{:filename "apple.yml"}
      {:filename "ingress_apple_https.yml" :config config}
      {:filename "install.sh"}
      {:filename "remove.sh"}
      {:filename "verify.sh" :config config}])
    (transport/exec-as-user
     user facility-name apple "install.sh")))