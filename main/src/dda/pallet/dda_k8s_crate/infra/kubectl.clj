(ns dda.pallet.dda-k8s-crate.infra.kubectl
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]))

; should act somewhat as an interface to the kubectl commands

(defn install-kubernetes-apt-repositories
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info
    (str facility "-install system: install kubernetes repository"))
   (actions/package-manager :update)
   (actions/package "apt-transport-https")
   (actions/exec-script
    ("curl" "-s" "https://packages.cloud.google.com/apt/doc/apt-key.gpg"
            "|" "apt-key" "add" "-"))))

(defn install-kubeadm
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info
    (str facility "-install system: apt install kubeadm"))
   (actions/package-manager :update)
   (actions/package ["docker.io" "kubelet" "kubeadm" "kubernetes-cni"])))

(defn deactivate-swap
  "deactivate swap on the server"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: disable swap")))
  (actions/exec-script ("swapoff" "-a"))
  (actions/exec-script ("sed" "-i" "'/swap/d'" "/etc/fstab")))

; TODO: add k8s user with user-crate in app-namespace

(defn kubectl-apply-f
  "apply kubectl config file"
  [facility path-on-server]
  (actions/as-action
   (logging/info
    (str facility "-install system: kubectl apply -f " path-on-server)))
  (actions/exec-script ("kubectl" "apply" "-f" ~path-on-server)))

(defn activate-kubectl-bash-completion
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: activate bash completion")))
  (actions/exec-script
   ("kubectl" "completion" "bash" ">>" "/etc/bash_completion.d/kubernetes")))

(defn initialize-cluster
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: activate bash completion")))
  (actions/exec-script ("systemctl" "enable" "docker.service"))
  (actions/exec-script ("kubeadm" "config" "images" "pull"))
  (actions/exec-script ("kubeadm" "init" "--pod-network-cidr=10.244.0.0/16" "pull"
                                  "--apiserver-advertise-address=127.0.0.1"))
  (actions/exec-script ("mkdir" "-p" "/home/k8s/.kube"))
  (actions/exec-script ("cp" "-i" "/etc/kubernetes/admin.conf"
                             "/home/k8s/.kube/config"))
  (actions/exec-script ("chown" "-R" "k8s:k8s" "/home/k8s/.kube")))

; Reminder: remote file with String as content:
;(actions/remote-file
;     (str user-home "/.bashrc.d/team-pass.sh")
;     :literal true
;     :content "# Load the custom .*-pass I have

; TODO: copy local files to host with template abstraction

; TODO: Execute code beyond line 60 in setup.sh as k8s user 