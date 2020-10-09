#!/bin/bash

#apt-get -qy install ifupdown2

#install --mode=0644 99-loopback.cfg /etc/network/interfaces.d/
install --mode=0644 99-loopback.yaml /etc/netplan/
#ifup lo:0
netplan apply
