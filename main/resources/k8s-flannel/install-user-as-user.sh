#!/bin/bash

kubectl apply -f flannel.yml
kubectl apply -f flannel-rbac.yml

kubectl taint nodes --all node-role.kubernetes.io/master-