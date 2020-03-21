#!/bin/bash

echo -e "\n====================\n"
echo -e "ingress-nginx   has type: LoadBalancer & external ip"
echo -e "\n====================\n"
kubectl get all --namespace ingress-nginx
