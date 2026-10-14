package com.motorica.gamecontrol;

import com.motorica.gamecontrol.GameControlSnapshot;
import com.motorica.gamecontrol.IGameControlCallback;

interface IGameControlService {
    GameControlSnapshot getLatestSnapshot();
    void registerCallback(IGameControlCallback callback);
    void unregisterCallback(IGameControlCallback callback);
    // Append only: transaction IDs and the v1 Parcelable used by STK remain unchanged.
    // Returns [openThreshold, closeThreshold], or null while MS has no known values.
    int[] getLatestThresholds();
}
