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
   [dda.pallet.dda-k8s-crate.app :as sut]
   [dda.pallet.dda-k8s-crate.domain-test :as test-domain]))

(s/set-fn-validation! true)

; TODO: Bad style to use others test definition!
(deftest app-config
  (testing
   "test plan-def"
    (is (map? (sut/app-configuration-resolved test-domain/test-domain-conf-prod)))))

(deftest plan-def
  (testing
   "test plan-def"
    (is (map? sut/with-k8s))))
