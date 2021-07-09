```clojure
(s/def k8sConvention
  {:user s/Keyword
   :k8s {:external-ip s/Str 
         (s/optional-key :external-ipv6) s/Str
         (s/optional-key :u18-04) (s/enum true)}
   :cert-manager (s/enum :letsencrypt-prod-issuer :letsencrypt-staging-issuer :selfsigned-issuer)
   (s/optional-key :apple) {:fqdn s/Str}
   (s/optional-key :nexus) {:fqdn s/Str}
   (s/optional-key :persistent-dirs) [s/Str]})
```
