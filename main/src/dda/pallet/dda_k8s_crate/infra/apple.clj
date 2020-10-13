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
   [clojure.spec.test.alpha :as st]
   [schema.core :as sch]
   [dda.provision :as p]
   [dda.provision.pallet :as pp]))

(s/def ::fqdn string?)
(s/def ::secret-name string?)
(s/def ::cluster-issuer string?)

(s/def ::facility keyword?)
(s/def ::user string?)

(s/def ::apple (s/keys :req [::fqdn ::secret-name ::cluster-issuer]))
(sch/def Apple {:fqdn sch/Str :secret-name sch/Str :cluster-issuer sch/Str})

(def apple "apple")

(s/fdef user-configure-apple
  :args (s/cat :facility ::facility :user ::user :config ::apple))

(defn user-configure-apple
  [facility user config]
  (let [facility-name (name facility)]
    (p/provision-log ::pp/pallet facility-name "user-configure-apple" "info" "start")
    (p/copy-resources-to-user
     ::pp/pallet user facility-name apple
     [{:filename "apple.yml"}
      {:filename "ingress_apple_https.yml" :config config}
      {:filename "install.sh"}
      {:filename "remove.sh"}
      {:filename "verify.sh" :config config}])
    (p/exec-file-on-target-as-user
     ::pp/pallet user facility-name apple "install.sh")))
