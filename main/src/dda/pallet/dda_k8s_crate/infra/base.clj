(ns dda.pallet.dda-k8s-crate.infra.base
  (:require
   [clojure.tools.logging :as logging]
   [schema.core :as s]
   [pallet.actions :as actions]))

(defn deactivate-swap
  "deactivate swap on the server"
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: disable swap")))
  (actions/exec-checked-script "turn swap off" ("swapoff" "-a"))
  (actions/exec-checked-script "remove active swap" ("sed" "-i" "'/swap/d'" "/etc/fstab")))

(defn install-utils [] 
 (actions/package "curl")
 (actions/package "grep")) 

(s/defn install
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system")))
  (deactivate-swap facility)
  (install-utils))
