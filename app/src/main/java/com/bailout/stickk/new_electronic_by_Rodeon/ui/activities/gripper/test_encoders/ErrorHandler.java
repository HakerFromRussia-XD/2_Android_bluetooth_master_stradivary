package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.test_encoders;

interface ErrorHandler {
	enum ErrorType {
		BUFFER_CREATION_ERROR
	}
	
	void handleError(ErrorType errorType, String cause);
}