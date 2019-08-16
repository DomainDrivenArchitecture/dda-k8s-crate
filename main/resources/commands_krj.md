kubectl get certificate -o wide --all-namespaces
NAMESPACE   NAME             READY   SECRET           ISSUER      STATUS                                          AGE
default     example-com      True    example-de-tls   ca-issuer   Certificate is up to date and has not expired   22h
default     example-de-tls   True    example-de-tls   ca-issuer   Certificate is up to date and has not expired   22h

kubectl get issuer --all-namespaces
No resources found.

kubectl describe namespace cert-manager
Name:         cert-manager
Labels:       certmanager.k8s.io/disable-validation=true
Annotations:  kubectl.kubernetes.io/last-applied-configuration:
                {"apiVersion":"v1","kind":"Namespace","metadata":{"annotations":{},"labels":{"certmanager.k8s.io/disable-validation":"true"},"name":"cert-...
Status:       Active
No resource quota.
No resource limits.

kubectl get issuer,certificate --namespace cert-manager
No resources found.

kubectl describe certificate example-de
