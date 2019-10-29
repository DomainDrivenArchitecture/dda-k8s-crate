(ns dda.pallet.dda-k8s-crate.infra.apple
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/def Apple {:fqdn s/Str :secret-name s/Str :cluster-issuer s/Str})

(s/defn user-render-apple-yml
  [user :- s/Str
   config :- Apple]
  (actions/remote-file
   (str "/home/" user "/k8s_resources/apple/ingress_apple_https.yml")
   :literal true
   :group user
   :owner user
   :mode "755"
   :content
   (selmer/render-file
    (str "apple/ingress_apple_https.yml.template") config)))

(s/defn apply-apple
  [user :- s/Str apply-with-user]
  (actions/directory
   "/mnt/data"
   :owner user
   :group user
   :mode "777")
  (apply-with-user "apple/apple.yml")
  (apply-with-user "apple/ingress_apple_https.yml"))

(s/defn configure-apple [apply-with-user user config]
  (user-render-apple-yml user config)
  (apply-apple user apply-with-user))
