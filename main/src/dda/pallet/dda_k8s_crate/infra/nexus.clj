(ns dda.pallet.dda-k8s-crate.infra.nexus
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/def nexus {:fqdn s/Str :secret-name s/Str :cluster-issuer s/Str})

(s/defn user-render-nexus-yml
  [user :- s/Str
   config :- nexus]
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

(s/defn configure-nexus [apply-with-user user config]
  (user-render-nexus-yml user config)
  (apply-nexus user apply-with-user))
