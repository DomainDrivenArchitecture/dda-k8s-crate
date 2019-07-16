#Check it's working
kubectl get all --all-namespaces #All status: running
kubectl get pods #No pods

#Get admin-user token
kubectl -n kube-system describe secret admin-user| awk '$1=="token:"{print $2}'

# connect to dashboard
http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy

# test apple with curl
curl http://192.168.56.101/apple -H 'Host: the.test.host'

# debug with debug pod
kubectl run my-shell --rm -i --tty --image nicolaka/netshoot -- bash
