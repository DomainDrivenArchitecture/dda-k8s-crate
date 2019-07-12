# optional: set network on remote
cd /etc/netplan
sudo nano 50-cloud-init.yaml
# Under ethernets: enp0s8: dhcp4: true

# connect to your server
ssh-keygen -f "$HOME/.ssh/known_hosts" -R "192.168.56.106"
ssh `(whoami)`@192.168.56.106 -L 8001:localhost:8001

#Install Kubernetes apt repo
sudo -i
apt-get update \
  && apt-get install -y apt-transport-https \
  && curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -

echo "deb http://apt.kubernetes.io/ kubernetes-xenial main" \
  | tee -a /etc/apt/sources.list.d/kubernetes.list

#Install kubelet (run containers), kubeadm (convenience utility) and kubernetes-cni (network components)
#CNI stands for Container Networking Interface which is a spec that defines how network drivers should interact with Kubernetes
apt-get update \
  && apt-get install -y \
  docker.io \
  kubelet \
  kubeadm \
  kubernetes-cni

#deactivate swap
swapoff -a
#remove any swap entry from /etc/fstab.
sed -i '/swap/d' /etc/fstab

# TODO
#[preflight] Running pre-flight checks
#	[WARNING IsDockerSystemdCheck]: detected "cgroupfs" as the Docker cgroup driver. The recommended driver is "systemd". Please follow the guide at https://kubernetes.io/docs/setup/cri/

#Configure an unprivileged user-account
useradd k8s -G sudo -m -s /bin/bash
passwd k8s

#Initialize your cluster with kubeadm
#kubeadm aims to create a secure cluster out of the box via mechanisms such as RBAC.
systemctl enable docker.service
# TODO: etcd not public. Configure IPv6. Look for systemd on port 68.
kubeadm config images pull
kubeadm init --pod-network-cidr=10.244.0.0/16 \
  --apiserver-advertise-address=127.0.0.1 --ignore-preflight-errors NumCPU

mkdir -p /home/k8s/.kube
cp -i /etc/kubernetes/admin.conf /home/k8s/.kube/config
chown -R k8s:k8s /home/k8s/.kube

# -------------- on your DevOps system -------------------
# local, needs to be executed without remote part
scp -r main/resources k8s@192.168.56.106:

# -------------- on server -------------------

#Configure environmental variables as the new user
su k8s
cd
whoami

rm -rf k8s_resources
mv resources k8s_resources

# clean up k8s setup
rm ca*
kubectl delete namespace ingress-nginx
kubectl delete namespace cert-manager
kubectl delete secret my-ca-key-pair echo-cert
kubectl delete clusterrole cert-manager cert-manager-edit cert-manager-view
kubectl delete -f /home/k8s/k8s_resources/cert_manager/ca_issuer.yml
kubectl delete -f /home/k8s/k8s_resources/cert_manager/selfsigned_issuer.yml
kubectl delete -f /home/k8s/k8s_resources/cert_manager/letsencrypt_staging_issuer.yml
kubectl delete -f /home/k8s/k8s_resources/cert_manager/letsencrypt_prod_issuer.yml
kubectl delete -f /home/k8s/k8s_resources/kustomization.yml
kubectl delete -f /home/k8s/k8s_resources/ingress.yml
kubectl delete -f /home/k8s/k8s_resources/apple.yml
kubectl delete -f /home/k8s/k8s_resources/banana.yml
kubectl delete -f /home/k8s/k8s_resources/nexus/nexus.yml

#Flannel provides a software defined network (SDN) using the Linux kernel's overlay and ipvlan modules.
#Apply your pod network (flannel)
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/k8s-manifests/kube-flannel-rbac.yml

#Allow a single-host cluster
kubectl taint nodes --all node-role.kubernetes.io/master-

#Check it's working
#All status: running
kubectl get all --all-namespaces
#No pods
kubectl get pods

# create admin_user
kubectl apply -f k8s_resources/admin_user.yml
kubectl -n kube-system describe secret admin-user| awk '$1=="token:"{print $2}'

# dashboard
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v1.10.1/src/deploy/recommended/kubernetes-dashboard.yaml
kubectl proxy &
# http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy

# MetalLB
kubectl apply -f /home/k8s/k8s_resources/metallb.yml # from https://raw.githubusercontent.com/google/metallb/v0.7.3/manifests/metallb.yaml
kubectl get pods -n metallb-system  # should all be running
vi /home/k8s/k8s_resources/metallb_config.yml # adjust ip to your public
kubectl apply -f /home/k8s/k8s_resources/metallb_config.yml

# Deploy Ingress
kubectl apply -f /home/k8s/k8s_resources/ingress_metallb.yml

# apple & banana
kubectl apply -f /home/k8s/k8s_resources/apple.yml
kubectl apply -f /home/k8s/k8s_resources/banana.yml
kubectl apply -f /home/k8s/k8s_resources/ingress_simple.yml



####### shelved stuff ###########



kubectl create namespace ingress-nginx
kubectl apply --kustomize /home/k8s/k8s_resources/


#microk8s.enable dns storage metrics-server

# nexus, takes a few minutes to start
sudo mkdir /mnt/data
sudo chmod -R 777 /mnt/data
kubectl apply -f /home/k8s/k8s_resources/nexus/nexus-storage.yml
kubectl apply -f /home/k8s/k8s_resources/nexus/nexus.yml
# ingress
kubectl apply -f /home/k8s/k8s_resources/ingress.yml


# install cert-manager
kubectl create namespace cert-manager
kubectl label namespace cert-manager certmanager.k8s.io/disable-validation=true
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

kubectl apply -f /home/k8s/k8s_resources/cert_manager/ca_issuer.yml
kubectl apply -f /home/k8s/k8s_resources/cert_manager/selfsigned_issuer.yml
kubectl apply -f /home/k8s/k8s_resources/cert_manager/letsencrypt_staging_issuer.yml
kubectl apply -f /home/k8s/k8s_resources/cert_manager/letsencrypt_prod_issuer.yml

# debug
# kubectl run my-shell --rm -i --tty --image nicolaka/netshoot -- bash


# Insepct echo app at:
#     https://[192.168.56.105]/apple
#     https://k8stest.domaindrivenarchitecture.org/apple
#     https://k8stest.domaindrivenarchitecture.org/banana
