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
(ns dda.pallet.dda-k8s-crate.infra.base
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]))

(def module "base")

(s/defn system-install
  [facility]
  (actions/as-action (logging/info (str facility "-install system"))
   (transport/copy-resources-to-tmp
    (name facility)
    module
    [{:filename "install.sh"}])
    (transport/exec facility module "install.sh")))
