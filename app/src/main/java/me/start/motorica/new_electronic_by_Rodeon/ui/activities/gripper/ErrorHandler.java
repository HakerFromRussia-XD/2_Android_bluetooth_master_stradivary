package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper;

interface ErrorHandler {
	enum ErrorType {
		BUFFER_CREATION_ERROR
	}
	
	void handleError(ErrorType errorType, String cause);
}