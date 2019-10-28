(ns dda.pallet.dda-k8s-crate.infra.k8s
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/def k8s
  {:external-ip s/Str
   :letsencrypt-prod s/Bool   ; Letsencrypt environment: true -> prod | false -> staging
   })

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

(s/defn user-copy-yml
  [facility
   user :- s/Str
   config :- k8s]
  (actions/as-action
   (logging/info (str facility " - user-configure-k8s-yml")))
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
                "cert_manager/letsencrypt_prod_issuer.yml"
                "cert_manager/letsencrypt_staging_issuer.yml"
                "apple/apple.yml"
                "apple/ingress_simple_le_staging_https.yml"
                "apple/ingress_simple_le_prod_https.yml"
                "nexus/nexus-storage.yml"
                "nexus/nexus.yml"]]
    (actions/remote-file
     (str "/home/" user "/k8s_resources/" path)
     :literal true
     :group user
     :owner user
     :mode "755"
     :content (selmer/render-file path {})))
  (actions/remote-file
   (str "/home/" user "/k8s_resources/metallb/metallb_config.yml")
   :literal true
   :group user
   :owner user
   :mode "755"
   :content
   (selmer/render-file
    (str "metallb/metallb_config.yml.template") config)))

(defn user-untaint-master
  [facility user]
  (actions/as-action (logging/info (str facility " - system-install-k8s-base-config")))
  (actions/exec-checked-script
   "user-untaint-master"
   ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "taint" "nodes"
           "--all" "node-role.kubernetes.io/master-'")))

(s/defn user-install-flannel
  [apply-with-user]
  (apply-with-user "flannel/kube-flannel.yml")
  (apply-with-user "flannel/kube-flannel-rbac.yml"))

(s/defn admin-dash-metal-ingress
  [apply-with-user]
  (apply-with-user "admin/admin_user.yml")
  (apply-with-user "dashboard/kubernetes-dashboard.yaml")
  (apply-with-user "metallb/metallb.yml")
  (apply-with-user "metallb/metallb_config.yml")
  (apply-with-user "ingress/mandatory.yaml")
  (apply-with-user "ingress/ingress_using_mettallb.yml"))

(s/defn install-cert-manager
  [apply-with-user
   user :- s/Str
   letsencrypt-prod :- s/Bool]
  (let [user-home-ca (str "home/" user "/ca")]
    (actions/exec-checked-script
     "create cert-manager ns"
     ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "create" "namespace" "cert-manager'")
     ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "label" "namespace"
             "cert-manager" "certmanager.k8s.io/disable-validation=true'"))
    (apply-with-user "cert_manager/cert-manager.yaml")
    (actions/directory
     user-home-ca
     :owner user
     :group user
     :mode "777")
    (actions/exec-checked-script
     "create cert key"
     ("sudo" "-H" "-u" ~user "bash" "-c" "'openssl" "genrsa" "-out"
             ~(str user-home-ca "/ca.key") "2048'")
     ("sudo" "-H" "-u" ~user "bash" "-c" "'openssl" "req" "-x509" "-new" "-nodes"
             "-key" ~(str user-home-ca "/ca.key")
             "-subj" "'/CN=test.domaindrivenarchitecture.org'"
             "-days" "365" "-reqexts" "v3_req"
             "-extensions" "v3_ca"
             "-out" ~(str user-home-ca "/ca.crt") "'")
     ("sudo" "-H" "-u" ~user "bash" "-c" "'kubectl" "create" "secret" "tls"
             "test-domaindrivenarchitecture-org-ca-key-pair"
             ~(str "--cert=" user-home-ca "/ca.crt")
             ~(str "--key=" user-home-ca "/ca.key")
             "--namespace=cert-manager'"))
    (apply-with-user "cert_manager/letsencrypt_staging_issuer.yml" true)
    (when letsencrypt-prod
      (apply-with-user "cert_manager/letsencrypt_prod_issuer.yml" true))))

(s/defn install-apple
  [apply-with-user
   letsencrypt-prod :- s/Bool]
  (apply-with-user "apple/apple.yml" true)
  (apply-with-user "apple/ingress_simple_le_staging_https.yml")
  (when letsencrypt-prod
    (apply-with-user "apple/ingress_simple_le_prod_https.yml")))

(s/defn init
  [facility
   config :- k8s]
  (actions/as-action
   (logging/info (str facility "-init")))
  (init-kubernetes-apt-repositories facility))

(s/defn system-install
  [facility
   config :- k8s]
  (actions/as-action (logging/info (str facility " - system-install")))
  (system-install-k8s facility)
  (system-install-k8s-base-config facility)
  (system-install-kubectl-bash-completion facility))

(s/defn user-install
  [facility
   user :- s/Str
   config :- k8s
   apply-with-user]
  (actions/as-action (logging/info (str facility " - user-install")))
  (user-install-k8s-env facility user)
  (user-copy-yml facility user config)
  (user-install-flannel apply-with-user)
  (user-untaint-master facility user))

(s/defn system-configure
  [facility
   config :- k8s]
  (actions/as-action (logging/info (str facility " - system-configure"))))

(s/defn user-configure
  [facility
   user :- s/Str
   config :- k8s
   apply-with-user]
  (let [{:keys [letsencrypt-prod]} config]
    (actions/as-action (logging/info (str facility " - user-configure")))
  ; TODO: run cleanup for being able do reaply config??
    (user-copy-yml facility user config)
    (admin-dash-metal-ingress apply-with-user)
    (install-cert-manager apply-with-user user letsencrypt-prod)
    ;TODO: make optional
    (install-apple apply-with-user letsencrypt-prod)))