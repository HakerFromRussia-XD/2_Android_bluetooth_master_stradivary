package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3;

interface UBI4ErrorHandlerV3 {
	enum ErrorType {
		BUFFER_CREATION_ERROR
	}
	
	void handleError(ErrorType errorType, String cause);
}