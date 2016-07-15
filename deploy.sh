#!/usr/bin/env bash

SEBASTOPOL_IP=$1
ENVIRONMENT=$2
# using deployment service sebastopol
TAG=git-$(echo $CIRCLE_SHA1 | cut -c1-12)
VPC=sandpit

# we want curl to output something we can use to indicate success/failure
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://$SEBASTOPOL_IP:9501/marathon/whiner-timbre -H "Content-Type: application/json" -H "$SEKRIT_HEADER: 123" --data-binary "@whiner-timbre.json")

echo "HTTP code " $STATUS
if [ $STATUS == "201" ]
then exit 0
else exit 1
fi
