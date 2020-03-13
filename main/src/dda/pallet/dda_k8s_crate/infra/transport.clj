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
(ns dda.pallet.dda-k8s-crate.infra.transport
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/defn user-copy-resources
  [facility
   user :- s/Str
   dirs :- [s/Str]
   files :- [s/Str]]
  (actions/as-action
   (logging/info (str facility " - user-copy-resources")))
  (doseq [path dirs]
    (actions/directory
     (str "/home/" user path)
     :group user
     :owner user))
  (doseq [path files]
    (actions/remote-file
     (str "/home/" user "/k8s_resources/" path)
     :literal true
     :group user
     :owner user
     :mode "755"
     :content (selmer/render-file path {}))))

