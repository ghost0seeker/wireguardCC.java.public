#!/usr/bin/env fish

# Start OpenRC services
openrc
rc-service dbus start
rc-service firewalld start

# Drop into Fish shell
exec fish
