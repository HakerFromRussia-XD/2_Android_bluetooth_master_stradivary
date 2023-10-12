package com.bailout.stick.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders;

interface ErrorHandler {
	enum ErrorType {
		BUFFER_CREATION_ERROR
	}
	
	void handleError(ErrorType errorType, String cause);
}