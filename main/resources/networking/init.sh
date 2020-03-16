#!/bin/bash

#apt-get -qy install ifupdown2

install --mode=0644 99-loop-back.cfg /etc/network/interfaces.d/
ifup lo:0
