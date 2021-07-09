```clojure
(s/def K8s
  {:external-ip s/Str :external-ipv6 s/Str})

(s/def CertManager {(s/optional-key :env-flag) s/Str
                    (s/optional-key :acme-flag) s/Str})

(s/def Apple {:fqdn s/Str :secret-name s/Str :cluster-issuer s/Str})

(s/def Nexus {:fqdn s/Str :secret-name s/Str :cluster-issuer s/Str})

(def ddaK8sConfig
  {:user s/Keyword
   :k8s k8s/K8s
   :cert-manager cert-manager/CertManager
   (s/optional-key :persistent-dirs) [s/Str]
   (s/optional-key :apple) apple/Apple
   (s/optional-key :nexus) nexus/Nexus})
```
