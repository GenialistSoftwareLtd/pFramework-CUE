/*******************************************************************************
 * Copyright (c) 2017-2026 Genialist Software Ltd.
 * All rights reserved.
 ******************************************************************************/

package tv.genialist.fwrk.document.file;

/**
 * The <code>pCUESheetFileDocumentType</code> class is an implementation of the {@link pFileDocumentType} interface 
 * for cue sheet files.
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Cue_sheet_%28computing%29">Wikipedia</a>: 
 * <i>A cue sheet, or cue file, is a metadata file which describes how the tracks of a CD or DVD are laid out. 
 * Cue sheets are stored as plain text files and commonly have a ".cue" filename extension.</i> 
 * <p>
 * @author Genialist Software Ltd
 * @version 0.9.5
 * @see pCUESheet
 */
public class pCUESheetFileDocumentType extends pFileDocumentTypeImpl {

	/**************************************************************************/
	/***  DEFINITIONS  ********************************************************/
	/**************************************************************************/

	/**************************************************************************/
	/***  SUB-CLASSES  ********************************************************/
	/**************************************************************************/
	
	/**************************************************************************/
	/***  RUNTIME DATA  *******************************************************/
	/**************************************************************************/

	/** The default instance of this object. */
	private static final pCUESheetFileDocumentType DEFAULT = new pCUESheetFileDocumentType(); 
	
	/**************************************************************************/
	/***  CONSTRUCTORS  *******************************************************/
	/**************************************************************************/

	/**
	 * Constructs a new <code>pCUESheetFileDocumentType</code> object.
	 */
	public pCUESheetFileDocumentType() {
		super();
	}
	
	/**************************************************************************/
	/***  METHODS  ************************************************************/
	/**************************************************************************/
	
	/**************************************************************************/
	/***  STATIC METHODS  *****************************************************/
	/**************************************************************************/
	
	/**
	 * Gets a default instance of this class.
	 * <p>
	 * @return The default instance (this cannot be <code>null</code>).
	 */
	public static synchronized pCUESheetFileDocumentType getDefaultInstance() {
    	return DEFAULT;
	}
}

/******************************************************************************/
/***  END OF FILE  ************************************************************/
/******************************************************************************/
