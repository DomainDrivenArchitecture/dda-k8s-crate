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

(def domain-input {:user {:name :k8s
                          :password {:plain "xxx"}                                  ; k8s user pwd on os level
                          :ssh
                          {:ssh-authorized-keys [{:plain "ssh-rsa AAAA..LL comment"}] ; ssh authorized keys
                           :ssh-key {:public-key {:plain "ssh-rsa AAAA..LL comment"}  ; ssh-key for git sync
                                     :private-key {:plain "SOME_PRIVATE_SSH_KEY"}}}}})

(deftest app-config
  (testing
   "test plan-def"
    (is (map? (sut/app-configuration domain-input)))))

(deftest plan-def
  (testing
   "test plan-def"
    (is (map? sut/with-k8s))))