#!/bin/bash

echo -e "\n====================\n"
echo -e "All pods should have status: running"
echo -e "\n====================\n"
kubectl get pods -n metallb-system
