#!/bin/bash

set -e
set -o nounset

pod=$(kubectl get pods -o name | grep $1-wowza)

# This one pushes an RTMP stream to Wowza.
kubectl exec -it $pod -- ffmpeg -stream_loop -1 -re -i https://storage.googleapis.com/lp_testharness_assets/official_test_source_2s_keys_24pfs.mp4 -c:v copy -c:a copy -f flv rtmp://localhost/live/camcast

# This one sends an ffmpeg stream directly to a production transcoder
# kubectl exec -it -c ffmpeg $pod -- ffmpeg -re -i https://storage.googleapis.com/lp_testharness_assets/official_test_source_2s_keys_24pfs.mp4 -c:v copy -c:a copy -f hls "$(curl https://livepeer.live/api/broadcaster | jq -r '.[0].address')/live/8dc16bee-41b8-4c5c-80ae-fe5e02de0c7d/bbb/video.m3u8"
