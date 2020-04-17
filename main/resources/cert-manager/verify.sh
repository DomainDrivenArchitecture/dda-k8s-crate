#!/bin/bash

echo -e "\n====================\n"
echo -e "expect some pods running"
echo -e "\n====================\n"
kubectl get all --namespace cert-manager

echo -e "\n====================\n"
echo -e "expect some cert (see also https://cert-manager.io/docs/faq/acme/ ) \n"
echo -e "  -> kubectl describe certificaterequest <CERT_REQUEST_FROM_ABOVE> \n"
echo -e "  -> kubectl describe order <ORDER_FROM_ABOVE> \n"
echo -e "\n====================\n"
kubectl describe certificate jira-neu-prod-meissa-gmbh-de
