package com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.gripper_settings;

interface ErrorHandler {
	enum ErrorType {
		BUFFER_CREATION_ERROR
	}
	
	void handleError(ErrorType errorType, String cause);
}