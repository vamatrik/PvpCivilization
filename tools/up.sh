#!/bin/bash
# перезапуск сервера и клиента для теста
pkill -f clientRunProgramArgs; pkill -f serverRunProgramArgs; sleep 3; pkill -f "gradle-wrapper.jar run"; sleep 1
pgrep Xvfb >/dev/null || (Xvfb :99 -screen 0 1280x720x24 >/tmp/xvfb.log 2>&1 &); sleep 1
cd /home/claude/civ
: > run/logs/latest.log
(nohup ./gradlew runServer --console=plain > /tmp/server.log 2>&1 &)
for i in $(seq 1 50); do sleep 4; grep -q 'Done (' /tmp/server.log && break; done
(DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 nohup ./gradlew runClient -PautoConnect --console=plain > /tmp/client.log 2>&1 &)
for i in $(seq 1 80); do sleep 4; grep -q "joined the game" run/logs/latest.log && break; done
sleep 12; grep "joined the game" run/logs/latest.log | tail -1
