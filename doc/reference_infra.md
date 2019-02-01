```clojure
(def SmeagolPasswdUser
  {:admin s/Bool
   :email s/Str
   :password s/Str})

(def SmeagolPasswd
  {s/Keyword SmeagolPasswdUser})

; TODO: simplify smeagol-parent-dir and smeagol-dir to one directory
(def SmeagolInfra
  {:smeagol-parent-dir s/Str
   :smeagol-dir s/Str
   :smeagol-passwd SmeagolPasswd
   :smeagol-owner s/Str
   :repo-download-source s/Str
   :resource-locations {s/Keyword {:name s/Str :source s/Str :destination s/Str}}
   :environment-variables [{:name s/Str :value s/Str}]})

```
