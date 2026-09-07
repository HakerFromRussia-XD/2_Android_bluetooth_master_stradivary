package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3;

public final class V3FingerPositionMapping {
    public static final int THUMB_FIRST_AXIS_MIN_DEGREES = -35;
    public static final int THUMB_FIRST_AXIS_MAX_DEGREES = 49;
    public static final int THUMB_SECOND_AXIS_MIN_DEGREES = -68;
    public static final int THUMB_SECOND_AXIS_MAX_DEGREES = 22;
    public static final int THUMB_SECOND_AXIS_INITIAL_DEGREES = -34;
    public static final int THUMB_SECOND_PHALANX_MIN_DEGREES = -25;
    public static final int THUMB_SECOND_PHALANX_MAX_DEGREES = 20;

    private V3FingerPositionMapping() {
    }

    public static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    public static int thumbFirstAxisAngle(int percent) {
        return angleFromPercent(
                percent,
                THUMB_FIRST_AXIS_MIN_DEGREES,
                THUMB_FIRST_AXIS_MAX_DEGREES
        );
    }

    public static int thumbSecondAxisAngle(int percent) {
        return angleFromPercent(
                percent,
                THUMB_SECOND_AXIS_MIN_DEGREES,
                THUMB_SECOND_AXIS_MAX_DEGREES
        );
    }

    public static int thumbSecondPhalanxAngle(int percent) {
        return angleFromPercent(
                percent,
                THUMB_SECOND_PHALANX_MIN_DEGREES,
                THUMB_SECOND_PHALANX_MAX_DEGREES
        );
    }

    public static int thumbFirstAxisPercent(int angle) {
        return percentFromAngle(
                angle,
                THUMB_FIRST_AXIS_MIN_DEGREES,
                THUMB_FIRST_AXIS_MAX_DEGREES
        );
    }

    public static int thumbSecondAxisPercent(int angle) {
        return percentFromAngle(
                angle,
                THUMB_SECOND_AXIS_MIN_DEGREES,
                THUMB_SECOND_AXIS_MAX_DEGREES
        );
    }

    /** Collection clip percentages are applied directly to Android mesh axes. */
    public static float collectionThumbFirstAxisRotation(int percent, int handSide) {
        return collectionSideSign(handSide) * thumbFirstAxisAngle(percent);
    }

    public static float collectionThumbSecondPhalanxRotation(int percent, int handSide) {
        return collectionSideSign(handSide) * thumbSecondPhalanxAngle(percent);
    }

    public static float collectionThumbSecondAxisRotation(int percent, int handSide) {
        return collectionSideSign(handSide)
                * (thumbSecondAxisAngle(percent)
                - THUMB_SECOND_AXIS_INITIAL_DEGREES);
    }

    private static float collectionSideSign(int handSide) {
        return handSide == 0 ? -1.0f : 1.0f;
    }

    private static int angleFromPercent(int percent, int minAngle, int maxAngle) {
        int clampedPercent = clampPercent(percent);
        return Math.round(maxAngle - clampedPercent * (maxAngle - minAngle) / 100.0f);
    }

    private static int percentFromAngle(int angle, int minAngle, int maxAngle) {
        int clampedAngle = Math.max(minAngle, Math.min(maxAngle, angle));
        return clampPercent(Math.round((maxAngle - clampedAngle) * 100.0f / (maxAngle - minAngle)));
    }
}
