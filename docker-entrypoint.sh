#! /bin/bash

echo "*** Docker Entry Point Is Launching Traveltime Services *** "
exec /usr/bin/java \
    -jar \
    neic-traveltime-service.jar \
    --mode=service