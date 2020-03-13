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
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]))

(s/def Networking
  {:advertise-ip s/Str})

(s/defn init
  [facility
   config :- Networking]
  (actions/as-action
   (logging/info (str facility "-init")))
  (let [{:keys [advertise-ip]} config]
    (transport/copy-resources-to-tmp
     facility
     "networking"
     [{:filename "99-loop-back.cfg" :config {:ipv4 advertise-ip}}
      {:filename "start.sh"}])
    (actions/exec-checked-script "networking-init" (~(str "/tmp/" facility "/networking/start.sh")))))
