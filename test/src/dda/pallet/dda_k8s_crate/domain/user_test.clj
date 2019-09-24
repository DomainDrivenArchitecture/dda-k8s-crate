(ns dda.pallet.dda-k8s-crate.domain.user-test
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]
   [dda.pallet.dda-k8s-crate.domain.user :as sut]))

(s/set-fn-validation! true)

(def name :k8s)
(def user-password "xxx")
(def ssh {:ssh-authorized-keys ["ssh-rsa AAAA..LL comment"]
          :ssh-key {:public-key "ssh-rsa AAAA..LL comment"
                    :private-key "SOME_PRIVATE_SSH_KEY"}})

(deftest domain-config
  (testing
   "test user domain config"
    (is (map? (sut/domain-configuration name user-password ssh)))))

(def domain-config-1
  )