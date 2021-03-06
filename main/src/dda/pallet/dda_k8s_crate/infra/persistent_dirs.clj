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
(ns dda.pallet.dda-k8s-crate.infra.persistent-dirs
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [dda.provision :as p]
   [dda.provision.pallet :as pp]))

(def module "persistent-dirs")

(s/defn install-dir 
  [facility :- s/Keyword
   user-str :- s/Str
   persistent-dir :- s/Str]
  (let [facility-name (name facility)]
    (p/copy-resources-to-tmp
     ::pp/pallet facility-name module
     [{:filename "install-as-root.sh" :config {:user user-str :persistent-dir persistent-dir}}])
    (p/exec-file-on-target-as-root
     ::pp/pallet facility-name module "install-as-root.sh")))

(s/defn system-install
  [facility :- s/Keyword
   user-str :- s/Str
   persistent-dirs :- [s/Str]]
  (actions/as-action (logging/info (str facility "-install system")))
  (doseq [dir persistent-dirs]
    (install-dir facility user-str dir)))
