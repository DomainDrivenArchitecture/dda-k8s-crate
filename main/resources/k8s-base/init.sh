#!/bin/bash

#Install Kubernetes apt repo
apt-get update \
  && apt-get -qqy install apt-transport-https \
  && curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -

echo "deb http://apt.kubernetes.io/ kubernetes-xenial main" \
  | tee -a /etc/apt/sources.list.d/kubernetes.list
