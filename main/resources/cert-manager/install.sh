#!/bin/bash

kubectl create namespace "cert-manager"
kubectl label namespace  "cert-manager" "cert-manager.io/disable-validation=true"
kubectl apply -f cert-manager.yml

./wait-for-pod.sh webhook 5 10 20

kubectl apply -f selfsigning-issuer.yml
kubectl apply -f le-issuer.yml