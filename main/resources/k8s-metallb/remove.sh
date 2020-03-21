#!/bin/bash

kubectl delete -f metallb-config.yml
kubectl delete -f metallb.yml
#kubectl delete secret generic -n metallb-system secret/memberlist
kubectl delete namespace metallb-system