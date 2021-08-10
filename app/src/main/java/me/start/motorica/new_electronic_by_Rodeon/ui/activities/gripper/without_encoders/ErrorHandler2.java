package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders;

interface ErrorHandler2 {
	enum ErrorType {
		BUFFER_CREATION_ERROR
	}
	
	void handleError(ErrorType errorType, String cause);
}