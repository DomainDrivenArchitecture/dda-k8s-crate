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
   [clojure.spec.alpha :as s]
   ;[schema.core :as s]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]))

(s/def ::fqdn string?)
(s/def ::secret-name string?)
(s/def ::cluster-issuer string?)

(s/def ::facility keyword?)
(s/def ::user string?)

(s/def ::apple (s/keys :req [::fqdn ::secret-name ::cluster-issuer ]))

(def apple "apple")

(defn user-configure-apple
  [facility user config]
  {:pre [(s/valid? ::facility facility) 
         (s/valid? ::user user) 
         (s/valid? ::apple config)]}
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

(s/fdef myspectest 
  :args (s/cat :facility ::facility :user ::user :config ::apple)
  :ret int?)

(defn myspectest "test"
  [facility user config]
  "123")