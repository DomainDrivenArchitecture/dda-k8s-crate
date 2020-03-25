#!/bin/bash

kubectl delete -f ingress_nexus_https.yml
kubectl delete -f nexus.yml
kubectl delete -f nexus-storage.yml
