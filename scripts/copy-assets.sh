#!/bin/bash

DIR=$(dirname $0)
REPO="$DIR/.."
ASSETS="$REPO/../app/app/src/main/assets/"

cp "$REPO/Brandenburg.xmap" "$ASSETS/map.xmap.jet"
cat "$REPO/Brandenburg.sqlite" | gzip > "$ASSETS/map.sqlite.jet"
