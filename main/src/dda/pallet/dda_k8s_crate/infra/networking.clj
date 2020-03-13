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
   [clojure.string :as string]
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]))

(s/def Networking
  {:advertise-ip s/Str})

(s/def Resource
  {:filename s/Str
   (s/optional-key :config) s/Any})

(s/defn copy-temp-resources
  [facility :- s/Str
   module :- s/Str
   files :- [Resource]]
  (let [facility-path (str "/tmp/" facility)
        module-path (str facility-path "/" module)]
    (actions/directory
     facility-path
     :group "root"
     :owner "root")
    (actions/directory
     module-path
     :group "root"
     :owner "root")
    (doseq [resource files]
      (let [template? (contains? resource :config)
            filename (:filename resource)
            filename-on-target (str module-path "/" filename)
            filename-on-source (if template?
                                 (str module "/" filename ".template")
                                 (str module "/" filename))
            config (if template?
                     (:config resource)
                     {})
            mode (if (string/ends-with? filename ".sh")
                   "700"
                   "600")]
        (actions/remote-file
         filename-on-target
         :literal true
         :group "root"
         :owner "root"
         :mode mode
         :content (selmer/render-file filename-on-source config))))))

(s/defn init
  [facility
   config :- Networking]
  (actions/as-action
   (logging/info (str facility "-init")))
  (let [{:keys [advertise-ip]} config]
    (copy-temp-resources
     facility
     "networking"
     [{:filename "99-loop-back.cfg" :config {:ipv4 advertise-ip}}
      {:filename "start.sh"}])
    (actions/exec-checked-script "networking-init" (~(str "/tmp/" facility "/networking/start.sh")))))
