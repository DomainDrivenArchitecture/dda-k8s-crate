(ns dda.pallet.dda-k8s-crate.infra.check
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]))

(s/defn wait-until-pod-running [user :- s/Str pod-selector :- s/Str]
  (actions/exec-checked-script
   (str "check pod " pod-selector " until ready")
   ("sudo" "-H" "-u" ~user "bash" "-c" "'" ~(str "/home/" user "/k8s_resources/admin/pod-running.sh " pod-selector) "'")))
