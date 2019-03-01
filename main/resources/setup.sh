ssh-keygen -f "$HOME/.ssh/known_hosts" -R "159.69.207.106"
ssh `(whoami)`@159.69.207.106 -L 8001:localhost:8001

sudo -i

#nano /var/snap/microk8s/current/args/kube-apiserver
# replace "–authorization-mode=AlwaysAllow" with "–authorization-mode=RBAC"
# in line 5
nano dashboard-admin.yml
# Paste yml from resources

# install cert issuers
mkdir cert_manager
#nano cert_manager/letsencryp_staging_issuer.yaml
# Paste yml from resources
nano cert_manager/letsencryp_prod_issuer.yaml
# Paste yml from resources
#nano cert_manager/selfsigned_issuer.yaml
# Paste yml from resources
nano cert_manager/ca_issuer.yaml
# Paste yml from resources

nano apple.yml
# Paste yml from resources
nano banana.yml
# Paste yml from resources
nano nexus/nexus.yml
# Paste yml from resources

nano ingress.yml
# Paste yml from resources


snap install microk8s --classic
snap alias microk8s.kubectl kubectl
microk8s.enable dns dashboard storage ingress metrics-server
microk8s.stop
microk8s.start
#kubectl create -f dashboard-admin.yml

# Access dashboard locally:
kubectl proxy &
# http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy

# install cert-manager
kubectl create namespace cert-manager
kubectl label namespace cert-manager certmanager.k8s.io/disable-validation=true
## Install the CustomResourceDefinition resources
kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.6/deploy/manifests/00-crds.yaml
## Install cert-manager itself
kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.6/deploy/manifests/cert-manager-no-webhook.yaml

# TODO: tut mit rbac noch nicht
kubectl apply secret tls ca-key-pair \
  --cert=ca.crt --key=ca.key --namespace=default
kubectl apply -f cert_manager/ca_issuer.yaml
# kubectl apply -f cert_manager/selfsigned_issuer.yaml
# kubectl apply -f cert_manager/letsencryp_staging_issuer.yaml
kubectl apply -f cert_manager/letsencryp_prod_issuer.yaml

# apple & banana
kubectl apply -f apple.yml
kubectl apply -f banana.yml
# nexus
kubectl apply -f nexus/nexus.yml
# ingress
kubectl apply -f ingress.yml

# Insepct echo app at:
#     https://[159.69.207.106]/apple
#     https://k8stest.domaindrivenarchitecture.org/apple
#     https://k8stest.domaindrivenarchitecture.org/banana
