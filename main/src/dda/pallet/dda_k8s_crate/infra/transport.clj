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
   [clojure.string :as string]
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/defn user-copy-resources
 {:deprecated "0.1.4"}   
 [facility :- s/Str
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
  (doseq [file files]
    (actions/remote-file
     (str "/home/" user "/k8s_resources/" file)
     :literal true
     :group user
     :owner user
     :mode "755"
     :content (selmer/render-file file {}))))

(s/def Resource
  {:filename s/Str
   (s/optional-key :config) s/Any
   (s/optional-key :mode) s/Str})

(defn
  ^{:private true}
  copy-resources-to-path
  [user 
   facility-path 
   module 
   files]
  (let [module-path (str facility-path "/" module)]
    (actions/directory
     facility-path
     :group user
     :owner user)
    (actions/directory
     module-path
     :group user
     :owner user)
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
            mode (cond
                   (contains? resource :mode) (:mode resource)
                   (string/ends-with? filename ".sh") "700"
                   :default "600")]
        (actions/remote-file
         filename-on-target
         :literal true
         :group user
         :owner user
         :mode mode
         :content (selmer/render-file filename-on-source config))))))

(defn ^{:private true} user-path
  [user facility]
  (str "/home/" user "/resources/" facility))

(defn ^{:private true} tmp-path
  [facility]
  (str "/tmp/" facility))

(s/defn copy-resources-to-user
  [user :- s/Str
   facility :- s/Str
   module :- s/Str
   files :- [Resource]]
  (copy-resources-to-path user (user-path user facility) module files))

(s/defn copy-resources-to-tmp
  [facility :- s/Str
   module :- s/Str
   files :- [Resource]]
  (copy-resources-to-path "root" (tmp-path facility) module files))

(s/defn exec
  [facility :- s/Str
   module :- s/Str
   filename :-  s/Str]
  (let [facility-path (tmp-path (name facility))
        module-path (str facility-path "/" module)]
    (actions/exec-checked-script
     (str "execute " module "/" filename)
     ("cd" ~module-path)
     ("bash" ~filename))))

(s/defn exec-as-user
  [facility :- s/Str
   module :- s/Str
   user :- s/Str
   filename :-  s/Str]
  (let [facility-path (user-path user (name facility))
        module-path (str facility-path "/" module)]
    (actions/exec-checked-script
     (str "execute " module "/" filename)
     ("cd" ~module-path)
     ("sudo" "-H" "-u" ~user "bash" "-c" ~(str "./" filename)))))
