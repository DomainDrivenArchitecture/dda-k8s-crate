#!/bin/bash

echo -e "\n====================\n"
echo -e "expect some pods running"
echo -e "\n====================\n"
kubectl get all --namespace cert-manager
