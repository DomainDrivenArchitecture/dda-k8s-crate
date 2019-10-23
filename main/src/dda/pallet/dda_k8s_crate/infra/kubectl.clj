(ns dda.pallet.dda-k8s-crate.infra.kubectl
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [pallet.action :as action]
   [selmer.parser :as selmer]
   [clojure.string :as str]))

(s/def kubectl-config
  {:external-ip s/Str
   :host-name s/Str
   :letsencrypt-prod s/Bool   ; Letsencrypt environment: true -> prod | false -> staging
   :nexus-host-name s/Str
   :nexus-secret-name s/Str})

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

(defn system-install-k8s
  [facility]
  (actions/as-action
   (logging/info (str facility " - system-install-k8s")))
  (actions/package-manager :update)
  (actions/packages :aptitude ["docker.io" "kubelet" "kubeadm" "kubernetes-cni"]))

(defn system-install-kubectl-bash-completion
  [facility]
  (actions/as-action
   (logging/info (str facility " -system-install-kubectl-bash-completion")))
  (actions/exec-checked-script 
   "add k8s to bash completion"
   ("kubectl" "completion" "bash" ">>" "/etc/bash_completion.d/kubernetes")))

(defn system-install-k8s-base-config
  [facility]
  (actions/as-action (logging/info (str facility " - system-install-k8s-base-config")))
  (actions/exec-checked-script
   "system-install-k8s-base-config"
   ("systemctl" "enable" "docker.service")
   ("kubeadm" "config" "images" "pull")
   ("kubeadm" "init" "--pod-network-cidr=10.244.0.0/16"
              "--apiserver-advertise-address=127.0.0.1") ;fails here if you have less than 2 cpus
   ;("kubectl" "taint" "nodes" "--all" "node-role.kubernetes.io/master-")
   ))

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

(s/defn user-configure-copy-yml
  [facility
   user :- s/Str]
  (actions/as-action
   (logging/info (str facility " - user-configure-k8s-yml")))
  (doseq [path ["/k8s_resources"
                "/k8s_resources/flannel"
                "/k8s_resources/admin"
                "/k8s_resources/dashboard"
                "/k8s_resources/metallb"
                "/k8s_resources/ingress"
                "/k8s_resources/cert_manager"
                "/k8s_resources/apple_banana"
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
                "cert_manager/letsencrypt_prod_issuer.yml"
                "cert_manager/letsencrypt_staging_issuer.yml"
                "apple_banana/apple.yml"
                "apple_banana/banana.yml"
                "apple_banana/ingress_simple_le_staging_https.yml"
                "apple_banana/ingress_simple_le_prod_https.yml"
                "nexus/nexus-storage.yml"
                "nexus/nexus.yml"]]
    (actions/remote-file
     (str "/home/" user "/k8s_resources/" path)
     :literal true
     :group user
     :owner user
     :mode "755"
     :content (selmer/render-file path {}))))

(s/defn user-configure-copy-template
  [config :- kubectl-config
   owner :- s/Str]
  (actions/remote-file
   "/home/k8s/k8s_resources/metallb_config.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "metallb/metallb_config.yml.template" {:external-ip (:external-ip config)}))
  (actions/remote-file
   "/home/k8s/k8s_resources/nexus/ingress_nexus_https.yml"
   :literal true
   :owner owner
   :mode "755"
   :content (selmer/render-file "nexus/ingress_nexus_https.yml.template"
                                {:nexus-host-name (:nexus-host-name config)
                                 :nexus-secret-name (:nexus-secret-name config)})))

(defn user-configure-untaint-master
  [facility]
  (actions/as-action (logging/info (str facility " - system-install-k8s-base-config")))
  (actions/exec-checked-script
   "user-configure-untaint-master"
   ("kubectl" "taint" "nodes" "--all" "node-role.kubernetes.io/master-")))

(defn kubectl-apply-f
  "apply kubectl config file"
  [facility path-on-server & [should-sleep?]]
  (actions/as-action
   (logging/info
    (str facility "-install system: kubectl apply -f " path-on-server)))
  (when should-sleep?
    (actions/exec-checked-script "sleep" ("sleep" "180")))
  (actions/exec-checked-script
   "apply config file"
   ("sudo" "-H" "-u" "k8s" "bash" "-c" "'kubectl" "apply" "-f" ~path-on-server "'")))

(defn prepare-master-node
  [facility]
  (kubectl-apply-f facility "/home/k8s/k8s_resources/flannel/kube-flannel.yml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/flannel/kube-flannel-rbac.yml")
  (user-configure-untaint-master facility)
  (kubectl-apply-f facility "/home/k8s/k8s_resources/admin/admin_user.yml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/dashboard/kubernetes-dashboard.yaml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/metallb/metallb.yml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/metallb/metallb_config.yml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/ingress/mandatory.yaml")
  (kubectl-apply-f facility "/home/k8s/k8s_resources/ingress/ingress_using_mettallb.yml"))

(defn install-cert-manager
  [facility]
  (actions/exec-checked-script
   "create cert-manager ns"
   ("sudo" "-H" "-u" "k8s" "bash" "-c" "'kubectl" "create" "namespace" "cert-manager'"))
  (actions/exec-checked-script
   "label cert-manager ns"
   ("sudo" "-H" "-u" "k8s" "bash" "-c" "'kubectl" "label" "namespace" "cert-manager" "certmanager.k8s.io/disable-validation=true'"))
  (kubectl-apply-f facility "/home/k8s/k8s_resources/cert_manager/cert-manager.yaml")
  (actions/exec-checked-script
   "create cert key"
   ("sudo" "-H" "-u" "k8s" "bash" "-c" "'openssl" "genrsa" "-out" "ca.key" "2048'")
   ("sudo" "-H" "-u" "k8s" "bash" "-c" "'openssl" "req" "-x509" "-new" "-nodes" "-key" "ca.key" "-subj" "'/CN=test.domaindrivenarchitecture.org'"
           "-days" "365" "-reqexts" "v3_req" "-extensions" "v3_ca" "-out" "ca.crt'")
   ("sudo" "-H" "-u" "k8s" "bash" "-c" "'kubectl" "create" "secret" "tls" "test-domaindrivenarchitecture-org-ca-key-pair"
           "--cert=ca.crt"
           "--key=ca.key"
           "--namespace=cert-manager'")))

;TODO: make optional
(defn install-apple-banana
  [facility]
  (kubectl-apply-f facility "/home/k8s/k8s_resources/apple_banana/apple.yml" true)
  (kubectl-apply-f facility "/home/k8s/k8s_resources/apple_banana/banana.yml"))

;TODO: check if working
(defn configure-ingress-and-cert-manager
  [facility letsencrypt-prod]
  (if letsencrypt-prod
    (kubectl-apply-f facility "/home/k8s/k8s_resources/apple_banana/ingress_simple_le_prod_https.yml")
    (kubectl-apply-f facility "/home/k8s/k8s_resources/apple_banana/ingress_simple_le_staging_https.yml"))
  (if letsencrypt-prod
    (kubectl-apply-f facility "/home/k8s/k8s_resources/cert_manager/letsencrypt_prod_issuer.yml" true)
    (kubectl-apply-f facility "/home/k8s/k8s_resources/cert_manager/letsencrypt_staging_issuer.yml" true)))

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
  [facility config]
  (let [{:keys [letsencrypt-prod]} config]
    (prepare-master-node facility)
    (install-cert-manager facility)
    (install-apple-banana facility)
    (configure-ingress-and-cert-manager facility letsencrypt-prod)
    (install-nexus facility)))

(s/defn init
  [facility
   config :- kubectl-config]
  (actions/as-action
   (logging/info (str facility "-init")))
  (init-kubernetes-apt-repositories facility))

(s/defn system-install
  [facility
   config :- kubectl-config]
  (actions/as-action (logging/info (str facility " - system-install")))
  (system-install-k8s facility)
  (system-install-k8s-base-config facility)
  (system-install-kubectl-bash-completion facility))


(s/defn user-install
  [facility
   user :- s/Str
   config :- kubectl-config]
  (actions/as-action (logging/info (str facility " - user-install")))
  (user-install-k8s-env facility user))

(s/defn system-configure
  [facility
   config :- kubectl-config]
  (actions/as-action (logging/info (str facility " - system-configure")))
  )

(s/defn user-configure
  [facility
   user :- s/Str
   config :- kubectl-config]
  (actions/as-action (logging/info (str facility " - user-configure")))
  ; TODO run cleanup for being able do reaply config??
  (user-configure-copy-yml facility user)
  (user-configure-copy-template config user)
  
  ; TODO: as - user is not working!
  (kubectl-apply facility config))
