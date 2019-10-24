(ns dda.pallet.dda-k8s-crate.infra.kubectl
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [pallet.action :as action]
   [selmer.parser :as selmer]
   [clojure.string :as str]))

; TODO: use hostname
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
   user :- s/Str]
  (let [user-resource-path (str "/home/" user "/k8s_resources/")]
    (actions/remote-file
     (str user-resource-path "metallb/metallb_config.yml")
     :literal true
     :owner user
     :mode "755"
     :content (selmer/render-file "metallb/metallb_config.yml.template" {:external-ip (:external-ip config)}))
    (actions/remote-file
     (str user-resource-path "nexus/ingress_nexus_https.yml")
     :literal true
     :owner user
     :mode "755"
     :content (selmer/render-file "nexus/ingress_nexus_https.yml.template"
                                  {:nexus-host-name (:nexus-host-name config)
                                   :nexus-secret-name (:nexus-secret-name config)
                                   :cluster-issuer (if (:letsencrypt-prod config) 
                                                     "letsencrypt-prod-issuer" 
                                                     "letsencrypt-staging-issuer")}))))

(defn user-configure-untaint-master
  [facility user]
  (actions/as-action (logging/info (str facility " - system-install-k8s-base-config")))
  (actions/exec-checked-script
   "user-configure-untaint-master"
   ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "taint" "nodes" "--all" "node-role.kubernetes.io/master-'")))

(defn kubectl-apply-f
  "apply kubectl config file"
  [facility user path-on-server & [should-sleep?]]
  (actions/as-action
   (logging/info
    (str facility "-install system: kubectl apply -f " path-on-server)))
  (when should-sleep?
    (actions/exec-checked-script "sleep" ("sleep" "180")))
  (actions/exec-checked-script
   "apply config file"
   ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "apply" "-f" ~path-on-server "'")))

(s/defn prepare-master-node
  [apply-with-user
   user-resource-path :- s/Str
   user :- s/Str
   facility]
  (apply-with-user (str user-resource-path "flannel/kube-flannel.yml"))
  (apply-with-user (str user-resource-path "flannel/kube-flannel-rbac.yml"))
  (user-configure-untaint-master facility user)
  (apply-with-user (str user-resource-path "admin/admin_user.yml"))
  (apply-with-user (str user-resource-path "dashboard/kubernetes-dashboard.yaml"))
  (apply-with-user (str user-resource-path "metallb/metallb.yml"))
  (apply-with-user (str user-resource-path "metallb/metallb_config.yml"))
  (apply-with-user (str user-resource-path "ingress/mandatory.yaml"))
  (apply-with-user (str user-resource-path "ingress/ingress_using_mettallb.yml")))

(s/defn install-cert-manager
  [apply-with-user
   user-resource-path :- s/Str
   user :- s/Str]
  (let [user-home-ca (str "home/" user "/ca")]
    (actions/exec-checked-script
     "create cert-manager ns"
     ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "create" "namespace" "cert-manager'"))
    (actions/exec-checked-script
     "label cert-manager ns"
     ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "label" "namespace" "cert-manager" "certmanager.k8s.io/disable-validation=true'"))
    (apply-with-user (str user-resource-path "cert_manager/cert-manager.yaml"))
    (actions/directory
     user-home-ca
     :owner user
     :group user
     :mode "777")
    (actions/exec-checked-script
     "create cert key"
     ("sudo" "-H" "-u" ~user "bash" "-c" "'openssl" "genrsa" "-out" ~(str user-home-ca "/ca.key") "2048'")
     ("sudo" "-H" "-u" ~user "bash" "-c" "'openssl" "req" "-x509" "-new" "-nodes" "-key" ~(str user-home-ca "/ca.key") "-subj" "'/CN=test.domaindrivenarchitecture.org'"
             "-days" "365" "-reqexts" "v3_req" "-extensions" "v3_ca" "-out" ~(str user-home-ca "/ca.crt") "'")
     ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "create" "secret" "tls" "test-domaindrivenarchitecture-org-ca-key-pair"
             ~(str "--cert=" user-home-ca "/ca.crt")
             ~(str "--key=" user-home-ca "/ca.key")
             "--namespace=cert-manager'"))))

;TODO: make optional
(s/defn install-apple-banana
  [apply-with-user
   user-resource-path :- s/Str]
  (apply-with-user (str user-resource-path "apple_banana/apple.yml") true)
  (apply-with-user (str user-resource-path "apple_banana/banana.yml")))

;TODO: check if working
(s/defn configure-ingress-and-cert-manager
  [apply-with-user
   user-resource-path :- s/Str
   letsencrypt-prod]
  (if letsencrypt-prod
    (apply-with-user (str user-resource-path "apple_banana/ingress_simple_le_prod_https.yml"))
    (apply-with-user (str user-resource-path "apple_banana/ingress_simple_le_staging_https.yml")))
  (if letsencrypt-prod
    (apply-with-user (str user-resource-path "cert_manager/letsencrypt_prod_issuer.yml") true)
    (apply-with-user (str user-resource-path "cert_manager/letsencrypt_staging_issuer.yml") true)))

(s/defn install-nexus
  [apply-with-user
   user-resource-path :- s/Str
   user :- s/Str]
  (actions/directory
   "/mnt/data"
   :owner user
   :group user
   :mode "777")
  (apply-with-user (str user-resource-path "nexus/nexus-storage.yml"))
  (apply-with-user (str user-resource-path "nexus/nexus.yml"))
  (apply-with-user (str user-resource-path "nexus/ingress_nexus_https.yml")))

(s/defn kubectl-apply
  "apply needed files and options"
  [facility
   user :- s/Str
   config]
  (let [{:keys [letsencrypt-prod]} config
        apply-with-user (partial kubectl-apply-f facility user)
        user-resource-path (str "/home/" user "/k8s_resources/")]
    (prepare-master-node apply-with-user user-resource-path user facility)
    (install-cert-manager apply-with-user user-resource-path user)
    (install-apple-banana apply-with-user user-resource-path)
    (configure-ingress-and-cert-manager apply-with-user user-resource-path letsencrypt-prod)
    (install-nexus apply-with-user user-resource-path user)))

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
  (actions/as-action (logging/info (str facility " - system-configure"))))

(s/defn user-configure
  [facility
   user :- s/Str
   config :- kubectl-config]
  (actions/as-action (logging/info (str facility " - user-configure")))
  ; TODO: run cleanup for being able do reaply config??
  (user-configure-copy-yml facility user)
  (user-configure-copy-template config user)
  (kubectl-apply facility user config))
