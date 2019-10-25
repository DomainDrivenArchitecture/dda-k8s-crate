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
(ns dda.pallet.dda-k8s-crate.app-test
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]
   [dda.pallet.dda-k8s-crate.app :as sut]))

(s/set-fn-validation! true)

(s/def test-domain-conf-prod
  {:user :k8s
   :password "password"                                 ; k8s user pwd on os level
   :ssh {:ssh-authorized-keys ["ssh-rsa AAAA..LL comment"] ; ssh authorized keys
         :ssh-key {:public-key "ssh-rsa AAAA..LL comment"  ; ssh-key for git sync
                   :private-key "SOME_PRIVATE_SSH_KEY"}}
   :kubectl {:external-ip "external-ip"
             :host-name "hostname"
             :letsencrypt-prod true
             :nexus-host-name "nexus-host-name"}})

(deftest app-config
  (testing
   "test plan-def"
    (is (map? (sut/app-configuration-resolved test-domain-conf-prod)))))

(deftest plan-def
  (testing
   "test plan-def"
    (is (map? sut/with-k8s))))
