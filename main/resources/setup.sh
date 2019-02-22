ssh user@159.69.207.106 -L 8001:localhost:8001

sudo -i
snap install microk8s --classic
snap alias microk8s.kubectl kubectl
microk8s.start
microk8s.enable dns dashboard storage ingress metrics-server
nano /var/snap/microk8s/current/args/kube-apiserver
# replace "–authorization-mode=AlwaysAllow" with "–authorization-mode=RBAC"
# in line 5
microk8s.stop
microk8s.start
nano dashboard-admin.yml
# Paste yml from resources
kubectl create -f dashboard-admin.yml

# install cert-manager
kubectl create namespace cert-manager
kubectl label namespace cert-manager certmanager.k8s.io/disable-validation=true
## Install the CustomResourceDefinition resources
kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.6/deploy/manifests/00-crds.yaml
## Install cert-manager itself
kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.6/deploy/manifests/cert-manager.yaml

# install cert issuers
kubectl apply -f cert_manager/letsencryp_staging_issuer.yaml
kubectl apply -f cert_manager/selfsigned_issuer.yaml
kubectl create secret tls ca-key-pair \
   --cert=ca.crt --key=ca.key --namespace=default
kubectl apply -f cert_manager/ca_issuer.yaml

# apple & banana
nano apple.yml
# Paste yml from resources
kubectl create -f apple.yml
nano banana.yml
# Paste yml from resources
kubectl create -f banana.yml
nano ingress.yml
# Paste yml from resources
kubectl create -f ingress.yml

# Insepct echo app at: https://[159.69.207.106]/apple

# Access dashboard locally:
kubectl proxy
# http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy
