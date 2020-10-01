#!/bin/bash

echo "http://localhost:8002/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"
echo -e "\n"
echo $(kubectl -n kube-system describe secret admin-user| awk '$1=="token:"{print $2}')
echo -e "\n"

kubectl proxy --port=8002
