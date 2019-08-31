(ns dda.pallet.dda-k8s-crate.domain.templating
  (:require
   [schema.core :as s]
   [clojure.java.io :as io]))

(def template-associate-map
  {:metallb_config {:external-ip "SOME_IP"}})