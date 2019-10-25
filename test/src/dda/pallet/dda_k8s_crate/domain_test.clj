(ns dda.pallet.dda-k8s-crate.domain-test
  (:require
   [clojure.test :refer :all]
   [data-test :refer :all]
   [dda.pallet.dda-k8s-crate.domain :as sut]))

(defdatatest should-generate-infra-for-domain [input expected]
  (is (= expected
         (sut/infra-configuration input))))

(defdatatest should-generate-user-domain [input expected]
  (is (= expected
         (sut/user-domain-configuration input))))
