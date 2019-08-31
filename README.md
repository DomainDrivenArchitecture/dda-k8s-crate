# dda-k8s-crate
[![Clojars Project](https://img.shields.io/clojars/v/dda/dda-k8s-crate.svg)](https://clojars.org/dda/dda-k8s-crate)
[![Build Status](https://travis-ci.org/DomainDrivenArchitecture/dda-k8s-crate.svg?branch=master)](https://travis-ci.org/DomainDrivenArchitecture/dda-k8s-crate)

[![Slack](https://img.shields.io/badge/chat-clojurians-green.svg?style=flat)](https://clojurians.slack.com/messages/#dda-pallet/) | [<img src="https://meissa-gmbh.de/img/community/Mastodon_Logotype.svg" width=20 alt="team@social.meissa-gmbh.de"> team@social.meissa-gmbh.de](https://social.meissa-gmbh.de/@team) | [Website & Blog](https://domaindrivenarchitecture.org)

This crate is part of [dda-pallet](https://domaindrivenarchitecture.org/pages/dda-pallet/).

## Kubernetes setup 

This crate uses Kuberentes to initialize and build a single host Kubernetes cluster. 

### Requirements

This crate should fullfill certain requirements for the installed Kubernetes cluster

* a small k8s all in one system for serving one application.
* is compatible with k8s
* providing ingress for app to be installed (replacement of traditional reverse-proxy httpd)
* support letsencrypt (dynamic created by https) for a defined fqdn or injected static https certs.
* exposes dashboard with users defined & disabled annonymous access.

### Ingress and MetalLB
As our services need to be exposed to the outside by a URL we use Ingress to do just that. Ingress requires some sort of load balances attached in order to acquire an external IP. As we do not want to install Kubernetes on a third party cloud platform we require MetalLB to give Ingress an external IP. Ingress and MetalLB is configured in a dedicated file usually annotated with ingress_*.yml. There is a unique Ingress configuration for each type of certificate used and they can all be active at the same time and listen to different URLs. However, the config files are somewhat similar and most important config options are:

* the name given for the ingress instance
* the (cluster-)issuer to be used with that ingress instance
* host(s) name and corresponding seceretName
* the rules for redirecting and which service port to forward to 

MetalLB configuration resiedes in a seperate metallb_config.yml file where the host ip needs to be changed to the actual external if of the host. metallb.yml is responsible for the installation of Metallb but does not need to be adjusted for our installation.
[[Ingress]](https://kubernetes.io/docs/concepts/services-networking/ingress/) 
[[MetalLB]](https://metallb.universe.tf/) 



### Flannel networkking

Kubernetes requires a way for multiple hosts to commuicate with each other. Flanne handles the layer 3 networking of Kubernetes. We do not use any custom configuration of flannel. [[Flannel]](https://github.com/coreos/flannel#flannel) [[Kubernetes Cluster Networking]](https://kubernetes.io/docs/concepts/cluster-administration/networking/) 

### cert-manager and handling of https and certificates

Ingress together with MetalLB is not enough to handle https and ca certificates for our Kubernetes cluster. Additionally, we require cert-manager to issue certificates for us. The workflow for either letsencrypt or ca issuer requires three configs to be applied:

* a secret representing the ca-key pairs needs to be created
* a config file for the actual certificate needs to be created
* an issuer of either self-signed, ca-issuer or letsencrypt issuer

The self-signed issuer does not require a secret or certificate config file. For the letsencrypt issuer we distinguish between an issuer for staging and one for production.

### Nexus repository manager

The nexus repository manager is the first application we deploy on the Kubernetes cluster. It uses the lestsencrypt-prod-issuer and requires an ingress config file on its own. In addition to the ingress config file a nexus-storage.yml and nexus.yml config file are necessary. The storage is of kind PersistenVolume and differentiates the storage and actualy application of nexus in two seperate Pods. 

### Dashboard

The dashboard allows for debugging and overview of the Kubernetes cluster and should later also provide networking. To secure the dasboard, an ssh proxy is required and the dashboard can then be accessed via a http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy in the browser.



## compatability
dda-pallet is compatible to the following versions
* pallet 0.8.x
* clojure 1.9
* (x)ubunutu 18.04

## Features


...

### Watch log for debug reasons
In case of problems you may want to have a look at the log-file:
`less logs/pallet.log`

### Targets

You can define provisioning targets using the [targets-schema](https://github.com/DomainDrivenArchitecture/dda-pallet-commons/blob/master/doc/existing_spec.md)

### Domain API

You can use our conventions as a starting point:
[see domain reference](doc/reference_domain.md)

### Infra API

Or you can build your own conventions using our low level infra API. We will keep this API backward compatible whenever possible:
[see infra reference](doc/reference_infra.md)

## License

Copyright © 2018, 2019 meissa GmbH
Licensed under the [Apache License, Version 2.0](LICENSE) (the "License")
Pls. find licenses of our subcomponents [here](doc/SUBCOMPONENT_LICENSE)
