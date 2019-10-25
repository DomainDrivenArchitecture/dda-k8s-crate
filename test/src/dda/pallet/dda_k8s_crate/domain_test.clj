(ns dda.pallet.dda-k8s-crate.domain-test
  (:require
   [clojure.test :refer :all]
   [data-test :refer :all]
   [dda.pallet.dda-k8s-crate.domain :as sut]))

(defdatatest should-generate-infra-for-domain [input expected]
  (is (= expected
         (sut/infra-configuration input))))

(defdatatest should-generate-user-domain [input expected]
  (is (:k8s (sut/user-domain-configuration input)))
  (is (get-in (sut/user-domain-configuration input)
              [:k8s :clear-password]))
  (is (= expected
         (dissoc
          (:k8s (sut/user-domain-configuration input))
          :clear-password))))
