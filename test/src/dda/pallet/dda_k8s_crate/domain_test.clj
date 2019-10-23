(ns dda.pallet.dda-k8s-crate.domain-test
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]
   [dda.pallet.dda-k8s-crate.domain :as sut]
   [dda.pallet.commons.secret :as secret]))

(s/def test-domain-conf
  {:user :k8s
   :password "password"                                 ; k8s user pwd on os level
   :ssh {:ssh-authorized-keys ["ssh-rsa AAAA..LL comment"] ; ssh authorized keys
         :ssh-key {:public-key "ssh-rsa AAAA..LL comment"  ; ssh-key for git sync
                   :private-key "SOME_PRIVATE_SSH_KEY"}}
   :kubectl {:external-ip "external-ip"
             :host-name "hostname"
             :letsencrypt-prod true
             :nexus-host-name "nexus-host-name"}})

(deftest test-input-to-domain
  (is (= {:dda-k8s {:user :k8s
                    :kubectl-config {:external-ip "external-ip"
                                     :host-name "hostname"
                                     :letsencrypt-prod true
                                     :nexus-host-name "nexus-host-name"
                                     :nexus-secret-name "nexus-host-name"}}}
         (sut/infra-configuration test-domain-conf))))

(deftest test-input-for-user-domain
  (is (= {:k8s {:clear-password "password"
                :settings #{:bashrc-d}
                :ssh-authorized-keys ["ssh-rsa AAAA..LL comment"]
                :ssh-key {:private-key "SOME_PRIVATE_SSH_KEY", :public-key "ssh-rsa AAAA..LL comment"}}}
         (sut/user-domain-configuration test-domain-conf))))



