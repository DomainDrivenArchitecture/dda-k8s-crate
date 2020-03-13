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
(ns dda.pallet.dda-k8s-crate.infra.check
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]))

(s/defn wait-until-pod-running
  [user :- s/Str pod-selector :- s/Str toleration-time :- s/Int sleep-sec :- s/Int final-sleep :- s/Int]
  (actions/exec-checked-script
   (str "check pod " pod-selector " until ready")
   ("sudo" "-H" "-u" ~user "bash" "-c" "'" ~(str "/home/" user "/k8s_resources/admin/pod-running.sh " pod-selector " " toleration-time " " sleep-sec " " final-sleep) "'")))
