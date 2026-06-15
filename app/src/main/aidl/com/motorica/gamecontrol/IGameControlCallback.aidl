package com.motorica.gamecontrol;

import com.motorica.gamecontrol.GameControlSnapshot;

interface IGameControlCallback {
    void onGameControlSnapshot(in GameControlSnapshot snapshot);
}
