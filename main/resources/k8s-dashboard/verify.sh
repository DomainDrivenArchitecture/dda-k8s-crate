#!/bin/bash

echo -e "\n====================\n"
echo -e "expect some service service/kubernetes-dashboard running"
echo -e "\n====================\n"
kubectl get all --namespace kubernetes-dashboard
