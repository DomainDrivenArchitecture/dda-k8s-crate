(ns dda.pallet.dda-k8s-crate.infra.transport
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/defn user-copy-resources
  [facility
   user :- s/Str]
  (actions/as-action
   (logging/info (str facility " - user-copy-resources")))
  (doseq [path ["/k8s_resources"
                "/k8s_resources/flannel"
                "/k8s_resources/admin"
                "/k8s_resources/dashboard"
                "/k8s_resources/metallb"
                "/k8s_resources/ingress"
                "/k8s_resources/cert_manager"
                "/k8s_resources/apple"
                "/k8s_resources/nexus"]]
    (actions/directory
     (str "/home/" user path)
     :group user
     :owner user))
  (doseq [path ["flannel/kube-flannel-rbac.yml"
                "flannel/kube-flannel.yml"
                "admin/admin_user.yml"
                "dashboard/kubernetes-dashboard.yaml"
                "metallb/metallb.yml"
                "ingress/mandatory.yaml"
                "ingress/ingress_using_mettallb.yml"
                "cert_manager/cert-manager.yaml"
                "apple/apple.yml"
                "nexus/nexus-storage.yml"
                "nexus/nexus.yml"
                "nexus/nexus-ready.sh"]]
    (actions/remote-file
     (str "/home/" user "/k8s_resources/" path)
     :literal true
     :group user
     :owner user
     :mode "755"
     :content (selmer/render-file path {}))))

