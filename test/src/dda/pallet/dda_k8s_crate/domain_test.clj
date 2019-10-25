(ns dda.pallet.dda-k8s-crate.domain-test
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]
   [dda.pallet.dda-k8s-crate.domain :as sut]
   [dda.pallet.commons.secret :as secret]))

; TODO: use data test instead
(s/def test-domain-conf-prod
  {:user :k8s
   :password "password"                                 ; k8s user pwd on os level
   :ssh {:ssh-authorized-keys ["ssh-rsa AAAA..LL comment"] ; ssh authorized keys
         :ssh-key {:public-key "ssh-rsa AAAA..LL comment"  ; ssh-key for git sync
                   :private-key "SOME_PRIVATE_SSH_KEY"}}
   :kubectl {:external-ip "external-ip"
             :host-name "hostname"
             :letsencrypt-prod true
             :nexus-host-name "nexus-host-name"}})

(s/def test-domain-conf-staging
  {:user :k8s
   :password "password"                                 ; k8s user pwd on os level
   :ssh {:ssh-authorized-keys ["ssh-rsa AAAA..LL comment"] ; ssh authorized keys
         :ssh-key {:public-key "ssh-rsa AAAA..LL comment"  ; ssh-key for git sync
                   :private-key "SOME_PRIVATE_SSH_KEY"}}
   :kubectl {:external-ip "external-ip"
             :host-name "hostname"
             :letsencrypt-prod false
             :nexus-host-name "nexus-host-name"}})

(deftest test-input-to-domain-prod
  (is (= {:dda-k8s {:user :k8s
                    :kubectl-config {:external-ip "external-ip"
                                     :host-name "hostname"
                                     :letsencrypt-prod true
                                     :nexus-host-name "nexus-host-name"
                                     :nexus-secret-name "nexus-host-name"
                                     :nexus-cluster-issuer "letsencrypt-prod-issuer"}}}
         (sut/infra-configuration test-domain-conf-prod))))

(deftest test-input-to-domain-staging
  (is (= {:dda-k8s {:user :k8s
                    :kubectl-config {:external-ip "external-ip"
                                     :host-name "hostname"
                                     :letsencrypt-prod false
                                     :nexus-host-name "nexus-host-name"
                                     :nexus-secret-name "nexus-host-name"
                                     :nexus-cluster-issuer "letsencrypt-staging-issuer"}}}
         (sut/infra-configuration test-domain-conf-staging))))

(deftest test-input-for-user-domain
  (is (= {:k8s {:clear-password "password"
                :settings #{:bashrc-d}
                :ssh-authorized-keys ["ssh-rsa AAAA..LL comment"]
                :ssh-key {:private-key "SOME_PRIVATE_SSH_KEY", :public-key "ssh-rsa AAAA..LL comment"}}}
         (sut/user-domain-configuration test-domain-conf-prod))))



