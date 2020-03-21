#!/bin/bash

echo -e "\n====================\n"
echo -e "All pods should have status: running"
echo -e "\n====================\n"
kubectl get all --all-namespaces
echo -e "\n====================\n"
echo -e "there should be no pods"
echo -e "\n====================\n"
kubectl get pods