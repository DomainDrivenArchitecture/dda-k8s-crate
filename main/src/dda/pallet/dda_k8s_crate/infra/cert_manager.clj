(ns dda.pallet.dda-k8s-crate.infra.cert-manager
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/def cert-manager {(s/optional-key :env-flag) s/Str
                     (s/optional-key :acme-flag) s/Str})

(s/defn user-render-cert-manager-yml
  [user :- s/Str
   config :- cert-manager]
  (actions/remote-file
   (str "/home/" user "/k8s_resources/cert_manager/cert-issuer.yml")
   :literal true
   :group user
   :owner user
   :mode "755"
   :content
   (selmer/render-file
    (if (= config {}) "cert_manager/selfsigned-issuer.yml" "cert_manager/letsencrypt-issuer.yml.template") config)))

(s/defn apply-cert-manager
  [apply-with-user
   user :- s/Str]
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
    (apply-with-user "cert_manager/cert-issuer.yml" true)))

(s/defn configure-cert-manager [apply-with-user user config]
  (user-render-cert-manager-yml user config)
  (apply-cert-manager apply-with-user user))
