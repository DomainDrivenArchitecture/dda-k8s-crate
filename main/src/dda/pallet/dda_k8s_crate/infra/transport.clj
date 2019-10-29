(ns dda.pallet.dda-k8s-crate.infra.transport
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]
   [selmer.parser :as selmer]))

(s/defn user-copy-resources
  [facility
   user :- s/Str
   dirs :- [s/Str]
   files :- [s/Str]]
  (actions/as-action
   (logging/info (str facility " - user-copy-resources")))
  (doseq [path dirs]
    (actions/directory
     (str "/home/" user path)
     :group user
     :owner user))
  (doseq [path files]
    (actions/remote-file
     (str "/home/" user "/k8s_resources/" path)
     :literal true
     :group user
     :owner user
     :mode "755"
     :content (selmer/render-file path {}))))

