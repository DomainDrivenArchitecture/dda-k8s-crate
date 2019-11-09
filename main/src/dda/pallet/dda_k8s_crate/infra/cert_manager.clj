(ns dda.pallet.dda-k8s-crate.infra.cert-manager
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]
   [dda.pallet.dda-k8s-crate.infra.check :as check]))

(s/def CertManager {(s/optional-key :env-flag) s/Str
                    (s/optional-key :acme-flag) s/Str})

(s/defn user-render-cert-manager-yml
  [user :- s/Str
   config :- CertManager]
  (actions/remote-file
   (str "/home/" user "/k8s_resources/cert_manager/le-issuer.yaml")
   :literal true
   :group user
   :owner user
   :mode "755"
   :content
   (selmer/render-file "cert_manager/le-issuer.yaml.template" config)))

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
    (check/wait-until-pod-running user "webhook" 5 10 20)
    (apply-with-user "cert_manager/selfsigning-issuer.yaml")
    (apply-with-user "cert_manager/le-issuer.yaml")))

(s/defn user-configure-cert-manager
  [facility user config apply-with-user]
  (actions/as-action (logging/info (str facility " - user-configure-cert-manager")))
  (transport/user-copy-resources
   facility user
   ["/k8s_resources"
    "/k8s_resources/cert_manager"]
   ["cert_manager/cert-manager.yaml"
    "cert_manager/selfsigning-issuer.yaml"])
  (user-render-cert-manager-yml user config)
  (apply-cert-manager apply-with-user user))
