#!/bin/bash

kubectl apply -f mandatory.yml
kubectl apply -f ingress-using-metallb.yml 
