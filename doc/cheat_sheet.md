# connect to server & tunnel k8s-proxy
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no `(whoami)`@k8s.test.domaindrivenarchitecture.org -L 8001:localhost:8001
sudo su k8s
cd && whoami
sudo lsof -i -n | egrep '\<ssh\>'

# admin account
kubectl -n kube-system describe secret admin-user| awk '$1=="token:"{print $2}'

# apply yml
kubectl apply -f /home/k8s/k8s_resources/

# inspect
kubectl get all --all-namespaces
kubectl get pods
kubectl -n kube-system describe secret
kubectl -n kubernetes-dashboard describe secret

# test apple
curl https://k8s-apple.test.domaindrivenarchitecture.org/apple

# dashboard-2.0
kubectl proxy &
curl http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy
kubectl -n kubernetes-dashboard describe secret kubernetes-dashboard| awk '$1=="token:"{print $2}'

# nexus
curl --insecure -v  https://k8s-nexus.test.domaindrivenarchitecture.org/
cat /mnt/data/admin.password

# debug with network-tools pod
kubectl run my-shell --rm -i --tty --image nicolaka/netshoot -- bash

# log & attach
kubectl logs -n ingress-nginx nginx-ingress-controller-577c465bb-bszqk | less
kubectl exec -it -n ingress-nginx nginx-ingress-controller-577c465bb-bszqk bash
