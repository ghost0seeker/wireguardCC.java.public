#!/usr/bin/env fish

# Start OpenRC services
openrc
rc-service dbus start
rc-service firewalld start
firewall-cmd --zone=public --add-interface=eth0

# Drop into Fish shell
exec fish
