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
(ns dda.pallet.dda-k8s-crate.infra.networking
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]
   [dda.provision :as p]
   [dda.provision.pallet :as pp]))

(s/def Networking
  {:advertise-ip s/Str
   :os-version (s/enum :20.04 :18.04)})

(def module "networking")

(s/defn init
  [facility
   config :- Networking]
  (let [facility-name (name facility)
        {:keys [advertise-ip os-version]} config]
    (p/provision-log ::pp/pallet facility-name module ::p/info "init")
    (p/copy-resources-to-tmp
     ::pp/pallet facility-name module
     [{:filename "99-loopback.cfg" :config {:ipv4 advertise-ip}}
      {:filename "99-loopback.yaml" :config {:ipv4 advertise-ip}}
      {:filename "init18_04.sh"}
      {:filename "init20_04.sh"}])
    (if (= :20.04 os-version)
      (p/exec-file-on-target-as-root
       ::pp/pallet facility-name module "init20_04.sh")
      (p/exec-file-on-target-as-root
       ::pp/pallet facility-name module "init18_04.sh"))))
