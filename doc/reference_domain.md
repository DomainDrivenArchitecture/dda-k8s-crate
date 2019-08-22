```clojure
(def ClearPassword secret/Secret)
(def Ssh user-domain/Ssh)

(def Repository git-domain/Repository)
(def GitCredential git-domain/GitCredential)

(def VhostSettings httpd-domain/VhostSettings)

(def SmeagolPasswdUser
  {:admin s/Bool
   :email s/Str
   :password secret/Secret})

(def SmeagolPasswd
  {s/Keyword SmeagolPasswdUser})

(def SmeagolDomain
  {:server-fqdn s/Str
   :user {:name s/Keyword
          :passwd ClearPassword
          :ssh Ssh}
   :git-credential GitCredential
   :git-content-repo Repository
   :smeagol-users SmeagolPasswd
   (s/optional-key :settings) VhostSettings})

```

Referenced [user-domain elements can be found in dda-user-crate](https://github.com/DomainDrivenArchitecture/dda-user-crate), [httpd-domain elements can be found in dda-httpd-crate](https://github.com/DomainDrivenArchitecture/dda-httpd-crate) and [git-domain elements can be found in dda-git-crate](https://github.com/DomainDrivenArchitecture/dda-git-crate/blob/master/doc/reference_domain.md)
