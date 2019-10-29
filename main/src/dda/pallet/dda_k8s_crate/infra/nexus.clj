(ns dda.pallet.dda-k8s-crate.infra.nexus
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]
   [dda.pallet.dda-k8s-crate.infra.check :as check]))

(s/def Nexus {:fqdn s/Str :secret-name s/Str :cluster-issuer s/Str})

(s/defn user-render-nexus-yml
  [user :- s/Str
   config :- Nexus]
  (actions/remote-file
   (str "/home/" user "/k8s_resources/nexus/ingress_nexus_https.yml")
   :literal true
   :group user
   :owner user
   :mode "755"
   :content
   (selmer/render-file
    (str "nexus/ingress_nexus_https.yml.template") config)))

(s/defn apply-nexus
  [user :- s/Str apply-with-user]
  (actions/directory
   "/mnt/data"
   :owner user
   :group user
   :mode "777")
  (apply-with-user "nexus/nexus-storage.yml")
  (apply-with-user "nexus/nexus.yml")
  (apply-with-user "nexus/ingress_nexus_https.yml"))

(s/defn user-configure-nexus [facility user config apply-with-user]
  (transport/user-copy-resources
   facility user
   ["/k8s_resources"
    "/k8s_resources/nexus"]
   ["nexus/nexus-storage.yml"
    "nexus/nexus.yml"])
  (user-render-nexus-yml user config)
  (apply-nexus user apply-with-user)
  (check/wait-until-pod-running user "nexus" 5 10 20))
