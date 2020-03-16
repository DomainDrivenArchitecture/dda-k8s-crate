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

(defn init-kubernetes-apt-repositories
  [facility]
  (actions/as-action
   (logging/info
    (str facility " - init-kubernetes-apt-repositories")))
  (actions/package-manager :update)
  (actions/package "apt-transport-https")
  (actions/package-source
   "kubernetes"
   :aptitude
   {:url "http://apt.kubernetes.io/"
    :release "kubernetes-xenial"
    :scopes ["main"]
    :key-url "https://packages.cloud.google.com/apt/doc/apt-key.gpg"}))

(s/defn user-install-k8s-env
  [facility
   user :- s/Str]
  (actions/as-action (logging/info (str facility " - user-install-k8s-env")))
  (actions/exec-checked-script
   "user-install-k8s-env"
   ("mkdir" "-p" ~(str "/home/" user "/.kube"))
   ("cp" "-i" "/etc/kubernetes/admin.conf"
         ~(str "/home/" user "/.kube/config"))
   ("chown" "-R" ~(str user ":" user) ~(str "/home/" user "/.kube"))))

(defn user-untaint-master
  [facility user]
  (actions/as-action (logging/info (str facility " - system-install-k8s-base-config")))
  (actions/exec-checked-script
   "user-untaint-master"
   ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "taint" "nodes"
           "--all" "node-role.kubernetes.io/master-'")))

(s/defn user-render-metallb-yml
  [user :- s/Str config :- K8s]
  (actions/remote-file
   (str "/home/" user "/k8s_resources/metallb/metallb_config.yml")
   :literal true
   :group user
   :owner user
   :mode "755"
   :content
   (selmer/render-file
    (str "metallb/metallb_config.yml.template") config)))

(s/defn user-install-flannel
  [apply-with-user]
  (apply-with-user "flannel/kube-flannel.yml")
  (apply-with-user "flannel/kube-flannel-rbac.yml"))

(s/defn admin-dash-metal-ingress
  [apply-with-user]
  (apply-with-user "admin/admin_user.yml")
  (apply-with-user "dashboard/kubernetes-dashboard.2.0.b5.yml")
  (apply-with-user "dashboard/admin_dash.2.0.b5.yml")
  (apply-with-user "metallb/metallb.yml")
  (apply-with-user "metallb/metallb_config.yml")
  (apply-with-user "ingress/mandatory.yml")
  (apply-with-user "ingress/ingress_using_mettallb.yml"))

(s/defn init
  [facility
   config :- K8s]
  (actions/as-action
   (logging/info (str facility "-init")))
  (actions/package "ifupdown2")
  (init-kubernetes-apt-repositories facility))

(s/defn system-install
  [facility
   config :- K8s]
  (actions/as-action (logging/info (str facility " - system-install")))
  (let [{:keys [advertise-address]} config]
    (transport/copy-resources-to-tmp-and-exec
     facility 
     "infra-scripts" 
     [:filename "kubeadm-init" :config {:advertise-address advertise-address}])))

(s/defn user-install
  [facility
   user :- s/Str
   config :- K8s
   apply-with-user]
  (actions/as-action (logging/info (str facility " - user-install")))
  (transport/user-copy-resources
   facility user
   ["/k8s_resources"
    "/k8s_resources/flannel"]
   ["flannel/kube-flannel-rbac.yml"
    "flannel/kube-flannel.yml"])
  (user-install-k8s-env facility user)
  (user-install-flannel apply-with-user)
  (user-untaint-master facility user))

(s/defn system-configure
  [facility
   config :- K8s]
  (actions/as-action (logging/info (str facility " - system-configure"))))

(s/defn user-configure
  [facility
   user :- s/Str
   config :- K8s
   apply-with-user]
  (actions/as-action (logging/info (str facility " - user-configure")))
  ; TODO: run cleanup for being able do reaply config??
  (transport/user-copy-resources
   facility user
   ["/k8s_resources/admin"
    "/k8s_resources/dashboard"
    "/k8s_resources/metallb"
    "/k8s_resources/ingress"]
   ["admin/admin_user.yml"
    "admin/pod-running.sh"
    "dashboard/kubernetes-dashboard.1.10.yml"
    "dashboard/admin_dash.1.10.yml"
    "dashboard/kubernetes-dashboard.2.0.b5.yml"
    "dashboard/admin_dash.2.0.b5.yml"
    "metallb/metallb.yml"
    "ingress/mandatory.yml"
    "ingress/ingress_using_mettallb.yml"])
  (user-render-metallb-yml user config)
  (admin-dash-metal-ingress apply-with-user))
