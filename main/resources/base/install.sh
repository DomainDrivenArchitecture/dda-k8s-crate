apt-get update \
  && apt-get -qqy install grep

#deactivate swap
swapoff -a
#remove any swap entry from /etc/fstab.
sed -i '/swap/d' /etc/fstab
