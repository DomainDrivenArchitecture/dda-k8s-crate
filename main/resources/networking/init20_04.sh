#!/bin/bash

install --mode=0644 99-loopback.yaml /etc/netplan/
netplan apply
