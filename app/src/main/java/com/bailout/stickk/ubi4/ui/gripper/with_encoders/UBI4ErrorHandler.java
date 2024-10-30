package com.bailout.stickk.ubi4.ui.gripper.with_encoders;

interface UBI4ErrorHandler {
	enum ErrorType {
		BUFFER_CREATION_ERROR
	}
	
	void handleError(ErrorType errorType, String cause);
}