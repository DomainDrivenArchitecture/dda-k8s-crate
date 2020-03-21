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
(ns dda.pallet.dda-k8s-crate.infra.k8s
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]))

(s/def K8s
  {:external-ip s/Str :external-ipv6 s/Str :advertise-address s/Str})

(def k8s-base "k8s-base")
(def k8s-flannel "k8s-flannel")
(def k8s-admin "k8s-admin")
(def k8s-metallb "k8s-metallb")
(def k8s-ingress "k8s-ingress")
(def k8s-dashboard "k8s-dashboard")


(s/defn admin-dash-metal-ingress
  [apply-with-user]
  (apply-with-user "dashboard/kubernetes-dashboard.2.0.b5.yml")
  (apply-with-user "dashboard/admin_dash.2.0.b5.yml")
  (apply-with-user "ingress/mandatory.yml")
  (apply-with-user "ingress/ingress_using_mettallb.yml"))

(s/defn init
  [facility :- s/Keyword
   config :- K8s]
  (actions/as-action (logging/info (str facility "-init")))
  (transport/copy-resources-to-tmp
   (name facility)
   k8s-base
   [{:filename "init.sh"}])
  (transport/exec facility k8s-base "init.sh"))

(s/defn system-install
  [facility :- s/Keyword
   config :- K8s]
  (actions/as-action (logging/info (str facility " - system-install")))
  (let [facility-name (name facility)
        {:keys [advertise-address]} config]
    (transport/copy-resources-to-tmp
     facility-name k8s-base
     [{:filename "install-system.sh" :config {:advertise-address advertise-address}}])
    (transport/exec facility-name k8s-base "install-system.sh")))


(s/defn user-install
  [facility :- s/Keyword
   user :- s/Str
   config :- K8s]
  (let [facility-name (name facility)]
    (transport/log-info facility-name "user-install")
    (transport/copy-resources-to-tmp
     facility-name k8s-base
     [{:filename "install-user-as-root.sh" :config {:user user}}])
    (transport/exec 
     facility-name k8s-base "install-user-as-root.sh")
    (transport/copy-resources-to-user
     user facility-name k8s-flannel
     [{:filename "flannel-rbac.yml"}
      {:filename "flannel.yml"}
      {:filename "install-user-as-user.sh"}
      {:filename "verify.sh"}])
    (transport/exec-as-user 
     user facility-name k8s-flannel "install-user-as-user.sh")))

(s/defn system-configure
  [facility :- s/Keyword
   config :- K8s]
  (transport/log-info (name facility) "system-configure"))

(s/defn user-configure
  [facility :- s/Keyword
   user :- s/Str
   config :- K8s]
  (let [facility-name (name facility)]
    (transport/log-info facility-name "user-configure")
    (transport/copy-resources-to-user
     user facility-name k8s-admin
     [{:filename "admin-user.yml"}
      {:filename "remove.sh"}
      {:filename "install-user-as-user.sh"}])
    (transport/exec-as-user
     user facility-name k8s-admin "install-user-as-user.sh")
    (transport/copy-resources-to-user
     user facility-name k8s-metallb
     [{:filename "metallb.yml"}
      {:filename "metallb-config.yml" :config config}
      {:filename "remove.sh"}
      {:filename "verify.sh"}
      {:filename "install.sh"}])
    (transport/exec-as-user
     user facility-name k8s-metallb "install.sh")

    (transport/copy-resources-to-user
     user facility-name k8s-ingress
     [{:filename "mandatory.yml"}
      {:filename "ingress-using-metallb.yml"}
      {:filename "remove.sh"}
      {:filename "verify.sh"}
      {:filename "install.sh"}])
    (transport/exec-as-user
     user facility-name k8s-ingress "install.sh")

    (transport/copy-resources-to-user
     user facility-name k8s-dashboard
     [{:filename "kubernetes-dashboard.2.0.0.rc6.yml"}
      {:filename "admin_dash.2.0.0.rc6.yml"}
      {:filename "install-dashboard-as-user.sh"}
      {:filename "remove.sh"}
      {:filename "proxy.sh"}])
    (transport/exec-as-user
     user facility-name k8s-dashboard "install-dashboard-as-user.sh"))
        

  ; (transport/user-copy-resources
  ;  facility user
  ;  ["/k8s_resources/ingress"]
  ;  ["admin/admin_user.yml"
  ;   "admin/pod-running.sh"
  ;   "ingress/mandatory.yml"
  ;   "ingress/ingress_using_mettallb.yml"])
  )
