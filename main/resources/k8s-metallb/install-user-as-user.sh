#!/bin/bash

kubectl apply -f metallb.yml
kubectl apply -f metallb-config.yml
