#!/bin/bash

DIR=$(dirname $0)
REPO="$DIR/.."
TBO="$HOME/github/waldbrand/osm-data/emergency-merged.tbo"
BOUNDARY="$REPO/data/Brandenburg.smx"
TMP="$REPO/tmp-hydrants"
RULES="$REPO/mapfile/rules"

~/github/topobyte/mapocado/scripts/mapocado mapfile create \
    --input "$TBO" --boundary "$BOUNDARY" \
    --node-db "$TMP/nodes" --way-db "$TMP/ways" \
    --rules "$RULES" --logs logs --output "$REPO/Brandenburg-hydrants.xmap"
