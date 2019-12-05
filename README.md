# dda-k8s-crate
[![Clojars Project](https://img.shields.io/clojars/v/dda/dda-k8s-crate.svg)](https://clojars.org/dda/dda-k8s-crate)
[![Build Status](https://travis-ci.org/DomainDrivenArchitecture/dda-k8s-crate.svg?branch=master)](https://travis-ci.org/DomainDrivenArchitecture/dda-k8s-crate)

[![Slack](https://img.shields.io/badge/chat-clojurians-green.svg?style=flat)](https://clojurians.slack.com/messages/#dda-pallet/) | [<img src="https://meissa-gmbh.de/img/community/Mastodon_Logotype.svg" width=20 alt="team@social.meissa-gmbh.de"> team@social.meissa-gmbh.de](https://social.meissa-gmbh.de/@team) | [Website & Blog](https://domaindrivenarchitecture.org)

This crate is part of [dda-pallet](https://domaindrivenarchitecture.org/pages/dda-pallet/).

## Kubernetes setup

This crate uses Kubernetes to initialize and build a single host Kubernetes cluster.

### Features

The Kubernetes cluster installed by this crate provides the following features:

* a small k8s all-in-one system for serving one application
* compatible with k8s
* includes Ingress for the app to be installed (replacement of traditional reverse-proxy httpd)
* supports letsencrypt (dynamic created by https) for a defined fqdn or alternatively injected static https certs
* provides a dashboard for defined users with anonymous access disabled

### Ingress and MetalLB
As our services need to be exposed to the outside by a URL, we use Ingress for this. Ingress requires some kind of load balancer to be attached in order to acquire an external IP. As we don't want to install Kubernetes on a third party cloud platform, we require MetalLB to give Ingress an external IP. Ingress and MetalLB are configured in a dedicated file usually named like `ingress_*.yml`. There is a unique Ingress configuration for each type of certificate used and they can all be active at the same time and listen to different URLs. However, the config files are somewhat similar to each other. The most important config options are:

* the name for the Ingress instance
* the (cluster-)issuer to be used with the Ingress instance
* host(s) name and corresponding seceretName
* the rules for redirecting and for which service port to forward to

MetalLB configuration resides in a separate `metallb_config.yml` file, where the host ip needs to be changed to the actual external ip of the host. `metallb.yml` is responsible for the installation of MetalLB but doesn't need to be adjusted for our installation.
[[Ingress]](https://kubernetes.io/docs/concepts/services-networking/Ingress/)
[[MetalLB]](https://metallb.universe.tf/)


### Flannel networking

Kubernetes requires a way for multiple hosts to communicate with each other. Flannel handles the layer 3 networking of Kubernetes. We do not use any custom configuration for Flannel. [[Flannel]](https://github.com/coreos/flannel#flannel) [[Kubernetes Cluster Networking]](https://kubernetes.io/docs/concepts/cluster-administration/networking/)

### Cert-manager and handling of https and certificates

Ingress together with MetalLB aren't sufficient to handle https and CA certificates for our Kubernetes cluster. Additionally, we require cert-manager to issue certificates for us. The workflow for either letsencrypt or CA issuer requires three configs:

* a secret representing the CA-key pairs needs to be created
* a config file for the actual certificate needs to be created
* an issuer of either self-signed, CA-issuer or letsencrypt issuer

The self-signed issuer does not require a secret or certificate config file. For the letsencrypt issuer we distinguish between an issuer for staging and one for production.

### Nexus repository manager

The nexus repository manager is the first application we deploy on the Kubernetes cluster. It uses the lestsencrypt-prod-issuer and requires an Ingress config file on its own. In addition to the Ingress config file a `nexus-storage.yml` and `nexus.yml` config file are necessary. The storage is of kind PersistentVolume and differentiates the storage and the actual application of nexus in two seperate Pods.

### Dashboard

The dashboard allows for debugging and to get an overview of the Kubernetes cluster and should later also provide networking. To secure the dashboard, an ssh proxy is required and the dashboard can then be accessed via a http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy in the browser.

## Compatibility
dda-pallet is compatible with the following versions
* pallet 0.8.x
* clojure 1.9
* (x)ubunutu 18.04



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
