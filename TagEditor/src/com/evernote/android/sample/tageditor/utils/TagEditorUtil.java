package com.evernote.android.sample.tageditor.utils;

import com.evernote.client.oauth.android.EvernoteSession;

/**
 * Convenience class to hold the constants we use to access the Evernote service
 * 
 * @author Juan Gomez
 * @version 1.0.0
 * @since December 2, 2012
 * 
 */
public class TagEditorUtil {

	/***************************************************************************
	 * You MUST change the following values to run this sample application.    *
	 ***************************************************************************/

	// Your Evernote API key. See http://dev.evernote.com/documentation/cloud/
	// Please obfuscate your code to help keep these values secret.
	public static final String CONSUMER_KEY = "Your consumer key";
	public static final String CONSUMER_SECRET = "Your consumer secret";

	/***************************************************************************
	 * Change these values as needed to use this code in your own application. *
	 ***************************************************************************/

	// Initial development is done on Evernote's testing service, the sandbox.
	// Change to HOST_PRODUCTION to use the Evernote production service 
	// once your code is complete, or HOST_CHINA to use the Yinxiang Biji
	// (Evernote China) production service.
	public static final String EVERNOTE_HOST = EvernoteSession.HOST_SANDBOX;

}
