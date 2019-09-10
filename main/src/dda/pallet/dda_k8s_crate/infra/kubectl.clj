(ns dda.pallet.dda-k8s-crate.infra.kubectl
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/def k8s-infra-config
  {:external-ip s/Str
   :host-name s/Str})

; should act somewhat as an interface to the kubectl commands

(defn install-kubernetes-apt-repositories
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info
    (str facility "-install system: install kubernetes repository")))
  (actions/package-manager :update)
  (actions/package "apt-transport-https")
  (actions/package-source 
   "kubernetes"
   :aptitude
   {:url "http://apt.kubernetes.io/"
    :release "kubernetes-xenial"
    :scopes ["main"]
    :key-url "https://packages.cloud.google.com/apt/doc/apt-key.gpg"}))

(defn install-kubeadm
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info
    (str facility "-install system: apt install kubeadm")))
  (actions/package-manager :update)
  (actions/package ["docker.io" "kubelet" "kubeadm" "kubernetes-cni"]))

(defn deactivate-swap
  "deactivate swap on the server"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: disable swap")))
  (actions/exec-checked-script ("swapoff" "-a"))
  (actions/exec-checked-script ("sed" "-i" "'/swap/d'" "/etc/fstab")))

; TODO: add k8s user with user-crate in app-namespace

(defn kubectl-apply-f
  "apply kubectl config file"
  [facility path-on-server]
  (actions/as-action
   (logging/info
    (str facility "-install system: kubectl apply -f " path-on-server)))
  (actions/exec-checked-script ("kubectl" "apply" "-f" ~path-on-server)))

(defn activate-kubectl-bash-completion
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: activate bash completion")))
  (actions/exec-checked-script
   ("kubectl" "completion" "bash" ">>" "/etc/bash_completion.d/kubernetes")))

(defn initialize-cluster
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: activate bash completion")))
  (actions/exec-checked-script ("systemctl" "enable" "docker.service"))
  (actions/exec-checked-script ("kubeadm" "config" "images" "pull"))
  (actions/exec-checked-script ("kubeadm" "init" "--pod-network-cidr=10.244.0.0/16" "pull"
                                  "--apiserver-advertise-address=127.0.0.1"))
  (actions/exec-checked-script ("mkdir" "-p" "/home/k8s/.kube"))
  (actions/exec-checked-script ("cp" "-i" "/etc/kubernetes/admin.conf"
                             "/home/k8s/.kube/config"))
  (actions/exec-checked-script ("chown" "-R" "k8s:k8s" "/home/k8s/.kube")))

(s/defn create-dirs
  [owner :- s/Str]
  (actions/directory
   "/home/k8s/k8s_resources"
   :owner owner)
  (actions/directory
   "/home/k8s/k8s_resources/nexus"
   :owner owner)
  (actions/directory
   "/home/k8s/k8s_resources/cert_manager"
   :owner owner)
  (actions/directory
   "/home/k8s/k8s_resources/apple_banana"
   :owner owner))

(s/defn move-yaml-to-server
  [config :- k8s-infra-config
   owner :- s/Str]
  (create-dirs owner)
  (actions/remote-file
   "/home/k8s/k8s_resources/admin_user.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "admin_user.yml" {}))
  (actions/remote-file
   "/home/k8s/k8s_resources/metallb.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "metallb.yml" {}))
  (actions/remote-file
   "/home/k8s/k8s_resources/metallb_config.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "metallb_config.yml" {:external-ip (:external-ip config)}))
  (actions/remote-file
   "/home/k8s/k8s_resources/ingress_using_mettallb.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "ingress_using_mettallb.yml" {}))
  (actions/remote-file
   "/home/k8s/k8s_resources/apple_banana/apple.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "apple_banana/apple.yml" {}))
  (actions/remote-file
   "/home/k8s/k8s_resources/apple_banana/banana.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "apple_banana/banana.yml" {}))
  (actions/remote-file
   "/home/k8s/k8s_resources/apple_banana/ingress_simple_le_prod_https.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "apple_banana/ingress_simple_le_prod_https.yml" {})))


; Reminder: remote file with String as content:
;(actions/remote-file
;     (str user-home "/.bashrc.d/team-pass.sh")
;     :literal true
;     :content "# Load the custom .*-pass I have

; TODO: copy local files to host with template abstraction

; TODO: Execute code beyond line 60 in setup.sh as k8s user 