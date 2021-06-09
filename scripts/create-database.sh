#!/bin/bash

DIR=$(dirname $0)
REPO="$DIR/.."
TBO="$HOME/github/waldbrand/osm-data/Brandenburg.tbo"
BOUNDARY="$REPO/data/Brandenburg.smx"
TMP="$REPO/tmp"
CONFIG="$REPO/pois.xml"

~/github/topobyte/nomioc/setup-cli/scripts/NomiocDatabaseCreatorCustom \
    --input "$TBO" --boundary "$BOUNDARY" \
    --node-db "$TMP/nodes" --way-db "$TMP/ways" \
    --config "$CONFIG" --output "$REPO/Brandenburg.sqlite"
