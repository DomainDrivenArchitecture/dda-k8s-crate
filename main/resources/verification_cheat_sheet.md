# check it's working
kubectl get all --all-namespaces #All status: running

# get admin-user token
kubectl -n kube-system describe secret admin-user| awk '$1=="token:"{print $2}'

# connect to dashboard
http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy

# test apple remote with curl
curl http://192.168.56.101/apple -H 'Host: the.test.host'

# test apple local with curl
Find ClusterIP and port: kubectl get svc --all-namespaces
curl 10.109.169.40:5678

# debug with debug pod
kubectl run my-shell --rm -i --tty --image nicolaka/netshoot -- bash

# ssh with tunnel
ssh `(whoami)`@192.168.56.101 -L 8001:localhost:8001

# check if used services exist
kubectl get svc --all-namespaces

# open inside of pod with bash 
kubectl exec nginx-ingress-1167843297-40nbm -it bash -n ingress-nginx