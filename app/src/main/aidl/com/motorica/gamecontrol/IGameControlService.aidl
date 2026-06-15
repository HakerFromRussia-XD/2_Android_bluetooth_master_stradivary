package com.motorica.gamecontrol;

import com.motorica.gamecontrol.GameControlSnapshot;
import com.motorica.gamecontrol.IGameControlCallback;

interface IGameControlService {
    GameControlSnapshot getLatestSnapshot();
    void registerCallback(IGameControlCallback callback);
    void unregisterCallback(IGameControlCallback callback);
}
