#!/bin/bash

install --mode=0644 99-loop-back.cfg /etc/network/interfaces.d/
ifup lo:0
