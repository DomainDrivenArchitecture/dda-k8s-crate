(ns dda.pallet.dda-k8s-crate.infra.kubectl
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [pallet.action :as action]
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
  (actions/packages :aptitude ["docker.io" "kubelet" "kubeadm" "kubernetes-cni"]))

(defn deactivate-swap
  "deactivate swap on the server"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: disable swap")))
  (actions/exec-checked-script "turn swap off" ("swapoff" "-a"))
  (actions/exec-checked-script "remove active swap" ("sed" "-i" "'/swap/d'" "/etc/fstab")))

(defn kubectl-apply-f
  "apply kubectl config file"
  [facility path-on-server]
  (actions/as-action
   (logging/info
    (str facility "-install system: kubectl apply -f " path-on-server)))
  (action/with-action-options
    {:sudo-user "k8s"
     :script-dir "/home/k8s"
     :script-env {:HOME "/home/k8s"}}
    (actions/exec-checked-script "apply config file" ("kubectl" "apply" "-f" ~path-on-server))))

(defn prepare-master-node
  [facility]
  (kubectl-apply-f facility "https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml")
  (kubectl-apply-f facility "https://raw.githubusercontent.com/coreos/flannel/master/Documentation/k8s-manifests/kube-flannel-rbac.yml")
  (action/with-action-options
    {:sudo-user "k8s"
     :script-dir "/home/k8s"
     :script-env {:HOME "/home/k8s"}}
    (actions/exec-script ("kubectl" "taint" "nodes" "--all" "node-role.kubernetes.io/master-" "||" "true"))) ;needs to fail so no checked script
  (kubectl-apply-f facility "/home/k8s/k8s_resources/admin_user.yml")
  (kubectl-apply-f facility "https://raw.githubusercontent.com/kubernetes/dashboard/v1.10.1/src/deploy/recommended/kubernetes-dashboard.yaml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/metallb.yml")
  (kubectl-apply-f facility " /home/k8s/k8s_resources/metallb_config.yml")
  (kubectl-apply-f facility "https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/static/mandatory.yaml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/ingress_using_mettallb.yml"))

(defn install-cert-manager
  [facility]
  (action/with-action-options ;n2
    {:sudo-user "k8s"
     :script-dir "/home/k8s"
     :script-env {:HOME "/home/k8s"}}
    (actions/exec-checked-script "create cert-manager ns" ("kubectl" "create" "namespace" "cert-manager"))
    (actions/exec-checked-script "label cert-manager ns" ("kubectl" "label" "namespace" "cert-manager" "certmanager.k8s.io/disable-validation=true")))
  (kubectl-apply-f facility "https://github.com/jetstack/cert-manager/releases/download/v0.9.1/cert-manager.yaml")
  (action/with-action-options ;n2
    {:sudo-user "k8s"
     :script-dir "/home/k8s"
     :script-env {:HOME "/home/k8s"}}
    (actions/exec-checked-script "create cert key" ("openssl" "genrsa" "-out" "ca.key" "2048"))
    (actions/exec-checked-script "" ("openssl" "req" "-x509" "-new" "-nodes" "-key" "ca.key" "-subj" "'/CN=test.domaindrivenarchitecture.org'"
                                               "-days" "365" "-reqexts" "v3_req" "-extensions" "v3_ca" "-out" "ca.crt"))
    (actions/exec-checked-script "create cert key secret" ("kubectl" "create" "secret" "tls" "test-domaindrivenarchitecture-org-ca-key-pair"
                                                                     "--cert=ca.crt"
                                                                     "--key=ca.key"
                                                                     "--namespace=cert-manager"))))

(defn install-apple-banana
  [facility]
  (kubectl-apply-f facility "/home/k8s/k8s_resources/apple_banana/apple.yml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/apple_banana/banana.yml"))

(defn configure-ingress-and-cert-manager
  [facility]
  (kubectl-apply-f facility "/home/k8s/k8s_resources/apple_banana/ingress_simple_le_prod_https.yml")
  ;(kubectl-apply-f facility "/home/k8s/k8s_resources/cert_manager/letsencrypt_prod_issuer.yml") ;TODO not working
  )

(defn install-nexus
  [facility]
  (actions/directory
   "/mnt/data"
   :owner "k8s"
   :group "k8s"
   :mode "777")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/nexus/nexus-storage.yml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/nexus/nexus.yml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/nexus/ingress_nexus_https.yml"))

(defn kubectl-apply
  "apply needed files and options"
  [facility]
  (prepare-master-node facility)
  (install-cert-manager facility)
  (install-apple-banana facility)
  (configure-ingress-and-cert-manager facility)
  (install-nexus facility))

(defn activate-kubectl-bash-completion
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: activate bash completion")))
  (actions/exec-checked-script "add k8s to bash completion"
                               ("kubectl" "completion" "bash" ">>" "/etc/bash_completion.d/kubernetes")))

(defn initialize-cluster
  "apply kubectl config file"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: init cluster")))
  (actions/exec-checked-script "enable docker.service" ("systemctl" "enable" "docker.service"))
  (actions/exec-checked-script "pull k8s images" ("kubeadm" "config" "images" "pull"))
  (actions/exec-checked-script "init k8s" ("kubeadm" "init" "--pod-network-cidr=10.244.0.0/16"
                                                     "--apiserver-advertise-address=127.0.0.1"))
  (actions/exec-checked-script "mk .kube dir" ("mkdir" "-p" "/home/k8s/.kube"))
  (actions/exec-checked-script "copy admin config for k8s" ("cp" "-i" "/etc/kubernetes/admin.conf"
                                                                 "/home/k8s/.kube/config"))
  (actions/exec-checked-script "change owner of k8s dir" ("chown" "-R" "k8s:k8s" "/home/k8s/.kube")))

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
   "/home/k8s/k8s_resources/cert_manager/letsencrypt_prod_issuer.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "cert_manager/letsencrypt_prod_issuer.yml" {}))
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
   :content (selmer/render-file "apple_banana/ingress_simple_le_prod_https.yml" {}))
  (actions/remote-file
   "/home/k8s/k8s_resources/nexus/ingress_nexus_https.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "nexus/ingress_nexus_https.yml" {}))
  (actions/remote-file
   "/home/k8s/k8s_resources/nexus/nexus-storage.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "nexus/nexus-storage.yml" {}))
  (actions/remote-file
   "/home/k8s/k8s_resources/nexus/nexus.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "nexus/nexus.yml" {})))


; Reminder: remote file with String as content:
;(actions/remote-file
;     (str user-home "/.bashrc.d/team-pass.sh")
;     :literal true
;     :content "# Load the custom .*-pass I have

; TODO: copy local files to host with template abstraction

; TODO: Execute code beyond line 60 in setup.sh as k8s user 