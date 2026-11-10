#!/bin/bash
DISPLAY=:99 import -window root /tmp/_s.png && convert /tmp/_s.png -crop 854x480+213+120 +repage /tmp/$1.png
