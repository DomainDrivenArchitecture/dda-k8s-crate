(ns dda.pallet.dda-k8s-crate.infra.apple
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]
   [dda.pallet.dda-k8s-crate.infra.transport :as transport]))

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

(s/defn user-configure-apple
  [facility user config apply-with-user]
  (actions/as-action (logging/info (str facility " - user-apple-configure")))
  (transport/user-copy-resources
   facility user
   ["/k8s_resources"
    "/k8s_resources/apple"]
   ["apple/apple.yml"])
  (user-render-apple-yml user config)
  (apply-apple user apply-with-user))
