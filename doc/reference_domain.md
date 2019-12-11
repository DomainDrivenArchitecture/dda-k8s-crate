```clojure
(s/def k8sDomain
  {:user s/Keyword
   :k8s {:external-ip s/Str (s/optional-key :external-ipv6) s/Str}
   :cert-manager (s/enum :letsencrypt-prod-issuer :letsencrypt-staging-issuer :selfsigned-issuer)
   (s/optional-key :apple) {:fqdn s/Str}
   (s/optional-key :nexus) {:fqdn s/Str}})
```
