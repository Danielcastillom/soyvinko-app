#!/bin/sh
set -eu
/usr/bin/sudo -n /usr/sbin/nginx -t
/usr/bin/sudo -n /usr/bin/systemctl reload nginx
