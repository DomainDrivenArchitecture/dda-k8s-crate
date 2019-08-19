kubectl get certificate -o wide --all-namespaces
NAMESPACE   NAME             READY   SECRET           ISSUER      STATUS                                          AGE
default     example-com      True    example-de-tls   ca-issuer   Certificate is up to date and has not expired   22h
default     example-de-tls   True    example-de-tls   ca-issuer   Certificate is up to date and has not expired   22h

## Für Issuer
kubectl get issuer --all-namespaces
No resources found.
## Für ClusterIssuer(!) 
kubectl get clusterissuer --all-namespaces

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




## Fehlersuche ca-issuer not ready

kubectl describe clusterissuer ca-issuer 
Name:         ca-issuer
Namespace:    
Labels:       <none>
Annotations:  kubectl.kubernetes.io/last-applied-configuration:
                {"apiVersion":"certmanager.k8s.io/v1alpha1","kind":"ClusterIssuer","metadata":{"annotations":{},"name":"ca-issuer"},"spec":{"ca":{"secretN...
API Version:  certmanager.k8s.io/v1alpha1
Kind:         ClusterIssuer
Metadata:
  Creation Timestamp:  2019-08-19T12:33:01Z
  Generation:          2
  Resource Version:    7468
  Self Link:           /apis/certmanager.k8s.io/v1alpha1/clusterissuers/ca-issuer
  UID:                 59980bf2-ac55-49fc-b669-d8671a0abe3e
Spec:
  Ca:
    Secret Name:  my-ca-key-pair
Status:
  Conditions:
    Last Transition Time:  2019-08-19T12:33:01Z
    Message:               Error getting keypair for CA issuer: secret "my-ca-key-pair" not found
    Reason:                ErrGetKeyPair
    Status:                False
    Type:                  Ready
Events:
  Type     Reason         Age                  From          Message
  ----     ------         ----                 ----          -------
  Warning  ErrGetKeyPair  4m10s (x9 over 19m)  cert-manager  Error getting keypair for CA issuer: secret "my-ca-key-pair" not found
  Warning  ErrInitIssuer  4m10s (x9 over 19m)  cert-manager  Error initializing issuer: secret "my-ca-key-pair" not found

