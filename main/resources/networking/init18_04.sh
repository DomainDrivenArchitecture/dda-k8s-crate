#!/bin/bash

install --mode=0644 99-loopback.cfg /etc/network/interfaces.d/
ifup lo:0
