#!/bin/bash

kubectl create namespace metallb-system

kubectl apply -f metallb.yml
kubectl apply -f metallb-config.yml
kubectl apply -f proxy.yml

openssl rand -base64 128 | kubectl create secret generic -n metallb-system memberlist --from-literal=secretkey=-
