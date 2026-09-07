package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3;

import java.util.Arrays;

/** Thread-safe, per-card pose storage for the collection OpenGL renderer. */
public final class CollectionGesturePoseSource
		implements UBI4GripperSettingsWithEncodersRendererV3.FingerPoseSource {
	private static final int CHANNEL_COUNT = 6;
	private final int[] values = new int[CHANNEL_COUNT];

	public CollectionGesturePoseSource(int[] initialValues) {
		setPose(initialValues);
	}

	public synchronized void setPose(int[] pose) {
		if (pose == null || pose.length != values.length) {
			throw new IllegalArgumentException("Collection pose must contain " + values.length + " values");
		}
		System.arraycopy(pose, 0, values, 0, values.length);
	}

	public synchronized int[] copyPose() {
		return Arrays.copyOf(values, values.length);
	}

	@Override public synchronized int fingerPosition(int channel) {
		return values[channel];
	}

	/** Collection poses are absolute. Renderer updates every channel from them. */
	@Override public boolean isAnimating(int channel) {
		return true;
	}

	@Override public int side() {
		return 1;
	}
}
