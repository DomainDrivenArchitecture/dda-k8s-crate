#!/bin/bash

kubectl apply -f nexus-storage.yml
kubectl apply -f nexus.yml
kubectl apply -f ingress_nexus_https.yml
