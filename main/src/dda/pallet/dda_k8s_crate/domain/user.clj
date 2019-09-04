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

(ns dda.pallet.dda-k8s-crate.domain.user
  (:require
   [schema.core :as s]
   [dda.pallet.commons.secret :as secret]
   [dda.pallet.dda-user-crate.domain :as user-domain]))

(def ClearPassword secret/Secret)
(def ClearPasswordResolved s/Str)
(def Ssh user-domain/Ssh)
(def SshResolved user-domain/SshResolved)

(s/defn
  domain-configuration :- user-domain/UserDomainConfigResolved
  [name :- s/Keyword
   user-password :- ClearPasswordResolved
   ssh :- SshResolved]
  (let [{:keys [ssh-authorized-keys ssh-key]} ssh]
    {name
     (merge
      {:clear-password user-password
       :settings #{:bashrc-d}}
      (when (contains? ssh :ssh-authorized-keys)
        {:ssh-authorized-keys ssh-authorized-keys})
      (when (contains? ssh :ssh-key)
        {:ssh-key ssh-key}))}))
