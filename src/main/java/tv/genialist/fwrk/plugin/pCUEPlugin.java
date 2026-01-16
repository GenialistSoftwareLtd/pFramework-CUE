/*******************************************************************************
 * Copyright (c) 2017-2026 Genialist Software Ltd.
 * All rights reserved.
 ******************************************************************************/

package tv.genialist.fwrk.plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

import tv.genialist.fwrk.document.pFragmentPosition;
import tv.genialist.fwrk.document.pMediaDocument;
import tv.genialist.fwrk.document.file.pCUESheetFileDocumentType;
import tv.genialist.fwrk.document.file.pFileDocument;
import tv.genialist.fwrk.document.file.pMediaFileDocument;
import tv.genialist.fwrk.media.pMediaUtil;
import tv.genialist.fwrk.media.cue.pCUESheet;
import tv.genialist.fwrk.media.cue.pCUETrack;
import tv.genialist.fwrk.swing.util.service.pFileAcceptMetadata;
import tv.genialist.fwrk.swing.util.service.pFileAcceptMetadata.pFileAcceptMetadata_Request;
import tv.genialist.fwrk.swing.util.service.pFileSetMetadata;
import tv.genialist.fwrk.swing.util.service.pFileToPlaylist.pServiceProvider;
import tv.genialist.ptools.lang.util.pStringUtil;
import tv.genialist.ptools.util.pDuration;
import tv.genialist.ptools.util.pFilenameUtil;

/**
 * <p>
 * @author Genialist Software Ltd
 * @since 0.9.29
 * @version 0.9.29
 */
public class pCUEPlugin extends pBasePlugin {

	/** The prefix used in trace and log messages. */
	public static final String TRACE_PREFIX = "CUEPlugin";
	
	/**************************************************************************/
	/***  RUNTIME DATA  *******************************************************/
	/**************************************************************************/
	
	/** @since 0.9.29 */
	//private static final pTraceImpl TRACE = pTraceImpl.getTrace(pCUEPlugin.class, TRACE_PREFIX);
	
	/** The default instance of this object (initialised by the method {@link #getDefaultInstance()}). */
	private static pCUEPlugin DEFAULT; 
	
	/**************************************************************************/
	/***  CONSTRUCTORS  *******************************************************/
	/**************************************************************************/
	
	/**
	 * Constructs a new <code>pPDFPlugin</code> object.
	 */
	private pCUEPlugin() {
		super();
		
		addResource(new pServiceProvider() {

			/**************************************************************************/
			/***  DEFINITIONS  ********************************************************/
			/**************************************************************************/

			public boolean provide(File i_file, List<pMediaDocument> i_result, String p_default_title, long p_default_duration) {
				
				final String i_name = i_file.getName();
				//0.9.20
				final int i_ext = pFilenameUtil.indexOfExtension(i_name);
				if (i_ext<0)
				//if (null==pFilenameUtil.getExtension(i_name))
					return false;
				
				//0.9.20
				final File i_file_cue = new File(i_file.getParent(), pStringUtil.concat(i_name, 0, i_ext, ".cue"));
				//File i_file_cue = new File(i_file.getParent(), pFilenameUtil.removeExtension(i_name)+".cue");
				if (i_file_cue.exists() && i_file_cue.canRead() && i_file_cue.isFile()) {
					try {
						pCUESheet i_cue = new pCUESheet(i_file_cue, p_default_duration);
						
						i_cue.getMetadata().put(pMediaUtil.MNAME_TITLE, (null==p_default_title)? i_file.getName() : p_default_title);
						
						pFileDocument i_first_doc = null;
						for(pCUETrack i_track : i_cue.getTracks()) {
							pMediaFileDocument i_doc = new pMediaFileDocument(i_file);
							
							pFragmentPosition i_position = new pFragmentPosition();
							i_position.setStartPosition(i_track.getStart());
							i_position.setEndPosition(i_track.getEnd());
							
							i_doc.setFragment(i_position);
							i_doc.setMetadata(i_track.getMetadata());
							i_doc.setID(i_track.getID());
							
							pDuration i_duration = pDuration.getInstance(i_track.getDuration());
							i_doc.putValue(pMediaUtil.MNAME_LENGTH, i_duration);
							i_doc.getMetadata().setDuration(i_duration);
							
							i_doc.getMetadata().put(pMediaUtil.MNAME_TRACKS_FILE, i_file_cue.getAbsolutePath());
							
							if (null==i_first_doc)
								i_first_doc = i_doc;
							
							i_result.add(i_doc);
						}
						if (null!=i_first_doc) {
							return true;
						}
					}
					catch (final Exception ex) {
						ex.printStackTrace();
						//TODO:...
					}
				}
				return false;
			}
		});
		
		//*** SERVICE TO CHANGE MEDATA IN CUE FILES
		addResource(new pFileSetMetadata.pAbstractServiceProvider() {

			@Override
			public boolean provide(final File p_source, final Map<String,Object> p_values) throws Exception {
				return pCUESheet.update(p_source, p_values);
			}
		});
		
		addResource(new pFileAcceptMetadata.pAbstractChangeFileMetadata_ServiceProvider(this.getConfigHandler(), "cue.sp.file.accept.metadata", true) {

			@Override
			public boolean invoke(final pFileAcceptMetadata_Request p_request) {
				
				if (pCUESheetFileDocumentType.getDefaultInstance().accept(p_request.getSourceFile())) {
					if (pCUETrack.getMetadataKeys().contains(p_request.getMetadataType()))
						p_request.setResult(false);
					
					return true;
				}
				
				return false;
			}
		});
	}

	/**************************************************************************/
	/***  METHODS  ************************************************************/
	/**************************************************************************/

	/**
	 * Stops this plug-in.
	 * This method must be thread-safe.
	 * In case of overwriting, the method of the parent class should be invoked as well in case of success.
	 */
	public void stop() {
		super.stop();
		
		pCUESheetFileDocumentType.getDefaultInstance().clearCache();
	}
	
	/**************************************************************************/
	/***  STATIC METHODS  *****************************************************/
	/**************************************************************************/
	
	/**
	 * Gets a default instance of this class.
	 * <p>
	 * @return The default instance (this cannot be <code>null</code>).
	 */
	public static synchronized pCUEPlugin getDefaultInstance() {
		if (null==DEFAULT)
			DEFAULT = new pCUEPlugin();
    	return DEFAULT;
	}
}

/******************************************************************************/
/***  END OF FILE  ************************************************************/
/******************************************************************************/	
