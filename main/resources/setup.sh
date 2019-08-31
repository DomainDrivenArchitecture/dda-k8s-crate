# optional: set network on remote
cd /etc/netplan
sudo nano 50-cloud-init.yaml
# Under ethernets: enp0s8: dhcp4: true

# connect to your server
ssh-keygen -f "$HOME/.ssh/known_hosts" -R "k8s.test.domaindrivenarchitecture.org"
ssh `(whoami)`@k8s.test.domaindrivenarchitecture.org -L 8001:localhost:8001

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
# bash completion
kubectl completion bash >> /etc/bash_completion.d/kubernetes

#Configure an unprivileged user-account
useradd k8s -G sudo -m -s /bin/bash
passwd k8s

#Initialize your cluster with kubeadm
#kubeadm aims to create a secure cluster out of the box via mechanisms such as RBAC.
systemctl enable docker.service
# TODO: etcd not public. Configure IPv6. Look for systemd on port 68.
kubeadm config images pull
kubeadm init --pod-network-cidr=10.244.0.0/16 \
  --apiserver-advertise-address=127.0.0.1

mkdir -p /home/k8s/.kube
cp -i /etc/kubernetes/admin.conf /home/k8s/.kube/config
chown -R k8s:k8s /home/k8s/.kube

# -------------- on your DevOps system -------------------
# local, needs to be executed without remote part
scp -r main/resources k8s@k8s.test.domaindrivenarchitecture.org:

# -------------- on server -------------------

# TODO: need to make sure that we execute the code in pallet as the correct user

#Configure environmental variables as the new user
su k8s

cd
whoami # k8s !

rm -rf /home/k8s/k8s_resources
mv /home/k8s/resources /home/k8s/k8s_resources

# clean up k8s setup
rm ca*
#kubectl delete namespace ingress-nginx
#kubectl delete namespace cert-manager
#kubectl delete secret my-ca-key-pair echo-cert
#kubectl delete clusterrole cert-manager cert-manager-edit cert-manager-view
#kubectl delete -f /home/k8s/k8s_resources/cert_manager/ca_issuer.yml
#kubectl delete -f /home/k8s/k8s_resources/cert_manager/selfsigned_issuer.yml
#kubectl delete -f /home/k8s/k8s_resources/cert_manager/letsencrypt_staging_issuer.yml
#kubectl delete -f /home/k8s/k8s_resources/cert_manager/letsencrypt_prod_issuer.yml
#kubectl delete -f /home/k8s/k8s_resources/kustomization.yml
#kubectl delete -f /home/k8s/k8s_resources/ingress.yml
#kubectl delete -f /home/k8s/k8s_resources/apple.yml
#kubectl delete -f /home/k8s/k8s_resources/banana.yml
#kubectl delete -f /home/k8s/k8s_resources/nexus/nexus.yml

#Flannel provides a software defined network (SDN) using the Linux kernel's overlay and ipvlan modules.
#Apply your pod network (flannel)
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/k8s-manifests/kube-flannel-rbac.yml

#Allow a single-host cluster
kubectl taint nodes --all node-role.kubernetes.io/master-

#Check it's working
kubectl get all --all-namespaces #All status: running
kubectl get pods #No pods

# create admin_user
kubectl apply -f /home/k8s/k8s_resources/admin_user.yml

# dashboard
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v1.10.1/src/deploy/recommended/kubernetes-dashboard.yaml
kubectl proxy &
# http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy
kubectl -n kube-system describe secret admin-user| awk '$1=="token:"{print $2}'

# MetalLB
kubectl apply -f /home/k8s/k8s_resources/metallb.yml # from https://raw.githubusercontent.com/google/metallb/v0.7.3/manifests/metallb.yaml
vi /home/k8s/k8s_resources/metallb_config.yml # adjust ip to your public
kubectl apply -f /home/k8s/k8s_resources/metallb_config.yml
kubectl get pods -n metallb-system  # should all be running

# Deploy Ingress
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/static/mandatory.yaml
kubectl apply -f /home/k8s/k8s_resources/ingress_using_mettallb.yml
kubectl get all --all-namespaces #ingress-nginx   has type: LoadBalancer & external ip

# install cert-manager
kubectl create namespace cert-manager
kubectl label namespace cert-manager certmanager.k8s.io/disable-validation=true
kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/v0.9.1/cert-manager.yaml        

# apple & banana
kubectl apply -f /home/k8s/k8s_resources/apple_banana/apple.yml
kubectl apply -f /home/k8s/k8s_resources/apple_banana/banana.yml
kubectl apply -f /home/k8s/k8s_resources/apple_banana/ingress_simple_http.yml
# curl http://k8s.test.domaindrivenarchitecture.org/apple

kubectl apply -f /home/k8s/k8s_resources/cert_manager/selfsigned_issuer.yml
kubectl apply -f /home/k8s/k8s_resources/apple_banana/ingress_simple_selfsigned_https.yml
# curl https://k8s-selfsigned.test.domaindrivenarchitecture.org/apple

openssl genrsa -out ca.key 2048
openssl req -x509 -new -nodes -key ca.key -subj "/CN=test.domaindrivenarchitecture.org" \
  -days 365 -reqexts v3_req -extensions v3_ca -out ca.crt
kubectl create secret tls test-domaindrivenarchitecture-org-ca-key-pair \
   --cert=ca.crt \
   --key=ca.key \
   --namespace=cert-manager # If using ClusterIssuer, the secret definitly needs to be in the namespace of the cert-manager controller pod
kubectl apply -f /home/k8s/k8s_resources/cert_manager/ca_cert.yml
kubectl apply -f /home/k8s/k8s_resources/cert_manager/ca_issuer.yml
kubectl apply -f /home/k8s/k8s_resources/apple_banana/ingress_simple_ca_https.yml
# curl https://k8s-ca.test.domaindrivenarchitecture.org/apple

kubectl apply -f /home/k8s/k8s_resources/cert_manager/letsencrypt_staging_issuer.yml
kubectl apply -f /home/k8s/k8s_resources/apple_banana/ingress_simple_le_staging_https.yml
# curl https://k8s-le-staging.test.domaindrivenarchitecture.org/apple

kubectl apply -f /home/k8s/k8s_resources/cert_manager/letsencrypt_prod_issuer.yml
kubectl apply -f /home/k8s/k8s_resources/apple_banana/ingress_simple_le_prod_https.yml
# curl https://k8s-le-prod.test.domaindrivenarchitecture.org/apple

# nexus, takes a few minutes to start
sudo mkdir /mnt/data
sudo chown -R k8s:k8s /mnt/data
sudo chmod -R 777 /mnt/data
kubectl apply -f /home/k8s/k8s_resources/nexus/nexus-storage.yml
kubectl apply -f /home/k8s/k8s_resources/nexus/nexus.yml
kubectl apply -f /home/k8s/k8s_resources/nexus/ingress_nexus_https.yml
# curl https://k8s-nexus.test.domaindrivenarchitecture.org/
# cat /mnt/data/admin.password

######### OPTIONS END #########

# debug
# kubectl run my-shell --rm -i --tty --image nicolaka/netshoot -- bash
