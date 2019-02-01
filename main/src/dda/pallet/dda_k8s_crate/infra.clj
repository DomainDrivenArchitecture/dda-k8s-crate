; Licensed to the Apache Software Foundation (ASF) under one
; or more contributor license agreements. See the NOTICE file
; distributed with this work for additional information
; regarding copyright ownership. The ASF licenses this file
; to you under the Apache License, Version 2.0 (the
; "License"); you may not use this file except in compliance
; with the License. You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns dda.pallet.dda-k8s-crate.infra
  (:require
    [schema.core :as s]
    [clojure.tools.logging :as logging]
    [pallet.actions :as actions]
    [dda.pallet.core.infra :as core-infra]
    [dda.pallet.dda-k8s-crate.infra.k8s :as k8s]))

(def facility :dda-k8s)

(def k8sInfra k8s/k8sInfra)

(defn- install-microk8s
  [facility]
  (actions/as-action
   (logging/info (str facility "-install system: microk8s"))))
  ; # install - see: https://github.com/ubuntu/microk8s
  ; sudo -i
  ; snap install microk8s --classic
  ; snap alias microk8s.kubectl kubectl
  ; microk8s.enable dns dashboard storage ingress metrics-server
  ;
  ; # TODO: Install App-ServiceAccount for dash & app
  ;
  ; # TODO: inject bearer token App-ServiceAccount - see: https://github.com/kubernetes/dashboard/wiki/Access-control#bearer-token
  ; # get bearertoken - see: https://stackoverflow.com/questions/46664104/how-to-sign-in-kubernetes-dashboard
  ; kubectl -n kube-system describe secret $(kubectl -n kube-system get secret | awk '/^deployment-controller-token-/{print $1}') | awk '$1=="token:"{print $2}'
  ;
  ; # TODO: disable anonymous access
  ;
  ; # expose dashboard to outside - see: https://github.com/kubernetes/dashboard/wiki/Accessing-Dashboard---1.7.X-and-above#kubectl-proxy
  ; kubectl -n kube-system edit service kubernetes-dashboard
  ; --> type: ClusterIP to type: NodePort
  ; --> nodePort: 31665
  ;
  ; # inspect namespaces & dashboard port
  ; # kubectl get all --all-namespaces
  ; # kubectl -n kube-system get service kubernetes-dashboard
  ;
  ; # install apple & banana - see: https://matthewpalmer.net/kubernetes-app-developer/articles/kubernetes-ingress-guide-nginx-example.html
  ; install main/resources/apple.yml via dashboard
  ; install main/resources/banana.yml via dashboard
  ; install main/resources/ingress.yml via dashboard
  ; # Insepct echo app at: https://[159.69.207.106]/apple
  ;
  ; # TODO: install letsencrypt - see: https://github.com/jetstack/cert-manager or https://medium.com/google-cloud/kubernetes-w-lets-encrypt-cloud-dns-c888b2ff8c0e or https://akomljen.com/get-automatic-https-with-lets-encrypt-and-kubernetes-ingress/
  ;
  ; # TODO: install an example app

  ; # TODO: till dash is insecure, pls stop after finish development
  ; microk8s.stop



(s/defmethod core-infra/dda-install facility
  [core-infra config]
  (install-microk8s facility))

(def dda-k8s-crate
  (core-infra/make-dda-crate-infra
    :facility facility))

(def with-k8s
  (core-infra/create-infra-plan dda-k8s-crate))
