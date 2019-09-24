(ns dda.pallet.dda-k8s-crate.domain.domain-test
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]
   [dda.pallet.dda-k8s-crate.domain :as sut]))

(s/def test-conf-user
  {:user {:name :k8s
          :password {:plain "xxx"}                                  ; k8s user pwd on os level
          :ssh {:ssh-authorized-keys [{:plain "ssh-rsa AAAA..LL comment"}] ; ssh authorized keys
                :ssh-key {:public-key {:plain "ssh-rsa AAAA..LL comment"}  ; ssh-key for git sync
                          :private-key {:plain "SOME_PRIVATE_SSH_KEY"}}}}})

(s/def test-conf-kubectl
  {:kubectl {:external-ip "external-ip"
             :host-name "hostname"
             :letsencrypt-prod true
             :nexus-host-name "nexus-host-name"}})

(def test-domain-conf
  (merge
   test-conf-user
   test-conf-kubectl))

(s/validate sut/k8sDomainResolved test-conf-kubectl)

(defn run-my-tests
  ""
  []
  (sut/infra-configuration test-conf-kubectl))


