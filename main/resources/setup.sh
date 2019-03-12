# local
ssh-keygen -f "$HOME/.ssh/known_hosts" -R "159.69.207.106"
scp -r main/resources `(whoami)`@159.69.207.106:

# remote
ssh `(whoami)`@159.69.207.106 -L 8001:localhost:8001
sudo rm -rf /tmp/resources
mv resources /tmp/

sudo -i

snap install microk8s --classic
snap alias microk8s.kubectl kubectl
# Wait a moment for snap to really finish
microk8s.enable dns dashboard storage ingress metrics-server
# Wait a minute as PODs are initializing -> all Status:Running with kubectl get pods --all-namespaces

# RBAC
nano /var/snap/microk8s/current/args/kube-apiserver
#To activate RBAC replace "–authorization-mode=AlwaysAllow" with "–authorization-mode=RBAC" in line 5
microk8s.stop
microk8s.start

# ingress
kubectl apply -f /tmp/resources/ingress.yml
# dashboard
kubectl apply -f /tmp/resources/dashboard.yml

# clean up k8s setup?
rm ca*
kubectl delete namespace cert-manager
kubectl delete secret my-ca-key-pair ingress-cert
kubectl delete clusterrole cert-manager cert-manager-edit cert-manager-view
kubectl delete -f /tmp/resources/cert_manager/ca_issuer.yml
kubectl delete -f /tmp/resources/cert_manager/selfsigned_issuer.yml
kubectl delete -f /tmp/resources/cert_manager/letsencrypt_staging_issuer.yml
kubectl delete -f /tmp/resources/cert_manager/letsencrypt_prod_issuer.yml
kubectl delete -f /tmp/resources/ingress.yml
kubectl delete -f /tmp/resources/apple.yml
kubectl delete -f /tmp/resources/banana.yml
kubectl delete -f /tmp/resources/nexus/nexus.yml

# install cert-manager
kubectl create namespace cert-manager
kubectl label namespace cert-manager certmanager.k8s.io/disable-validation=true
## Install the CustomResourceDefinition resources
# Not needed: kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.6/deploy/manifests/00-crds.yaml
## Install cert-manager itself
kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.6/deploy/manifests/cert-manager-no-webhook.yaml

openssl genrsa -out ca.key 2048
openssl req -x509 -new -nodes -key ca.key -subj "/CN=domaindrivenarchitecture.org" -days 3650 -reqexts v3_req -extensions v3_ca -out ca.crt

#kubectl create secret generic ca-key-pair \
#  --from-file=tls.key=./ca.key \
#  --from-file=tls.crt=./ca.crt \
#  --namespace=default
kubectl create secret tls my-ca-key-pair \
   --cert=ca.crt \
   --key=ca.key \
   --namespace=cert-manager

kubectl apply -f /tmp/resources/cert_manager/ca_issuer.yml
kubectl apply -f /tmp/resources/cert_manager/selfsigned_issuer.yml
kubectl apply -f /tmp/resources/cert_manager/letsencrypt_staging_issuer.yml
kubectl apply -f /tmp/resources/cert_manager/letsencrypt_prod_issuer.yml

# apple & banana
kubectl apply -f /tmp/resources/apple.yml
kubectl apply -f /tmp/resources/banana.yml
# nexus, takes a few minutes to start
kubectl apply -f /tmp/resources/nexus/nexus.yml

# Access dashboard locally after waiting a few minutes:
kubectl proxy &
# http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy

# Insepct echo app at:
#     https://[159.69.207.106]/apple
#     https://k8stest.domaindrivenarchitecture.org/apple
#     https://k8stest.domaindrivenarchitecture.org/banana
