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
nano kubernetes-admin.yml
# Paste yml from resources
kubectl create -f dashboard-admin.yml
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

kubectl proxy

# Access dashboard locally:
# http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy
