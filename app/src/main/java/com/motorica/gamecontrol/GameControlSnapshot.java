package com.motorica.gamecontrol;

import android.os.Parcel;
import android.os.Parcelable;

public class GameControlSnapshot implements Parcelable {
    public int version;
    public long seq;
    public long timestampMs;
    public int openLevel;
    public int closeLevel;
    public boolean connected;

    public GameControlSnapshot() {
        this(1, 0L, 0L, 0, 0, false);
    }

    public GameControlSnapshot(int version, long seq, long timestampMs,
                               int openLevel, int closeLevel, boolean connected) {
        this.version = version;
        this.seq = seq;
        this.timestampMs = timestampMs;
        this.openLevel = openLevel;
        this.closeLevel = closeLevel;
        this.connected = connected;
    }

    protected GameControlSnapshot(Parcel in) {
        version = in.readInt();
        seq = in.readLong();
        timestampMs = in.readLong();
        openLevel = in.readInt();
        closeLevel = in.readInt();
        connected = in.readInt() != 0;
    }

    public static final Creator<GameControlSnapshot> CREATOR =
            new Creator<GameControlSnapshot>() {
                @Override
                public GameControlSnapshot createFromParcel(Parcel in) {
                    return new GameControlSnapshot(in);
                }

                @Override
                public GameControlSnapshot[] newArray(int size) {
                    return new GameControlSnapshot[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(version);
        dest.writeLong(seq);
        dest.writeLong(timestampMs);
        dest.writeInt(openLevel);
        dest.writeInt(closeLevel);
        dest.writeInt(connected ? 1 : 0);
    }
}
