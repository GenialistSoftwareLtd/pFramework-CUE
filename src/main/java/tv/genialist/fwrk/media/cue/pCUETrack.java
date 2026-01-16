/*******************************************************************************
 * Copyright (c) 2017-2024 Genialist Software Ltd.
 * All rights reserved.
 ******************************************************************************/

package tv.genialist.fwrk.media.cue;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import tv.genialist.fwrk.media.pMediaUtil;
import tv.genialist.fwrk.media.impl.pHasMetadataImpl;
import tv.genialist.ptools.data.pHasID;
import tv.genialist.ptools.lang.pBaseStringBuilder;
import tv.genialist.ptools.string.pString;

/**
 * The <code>pCUETrack</code> class defines an audio or video track contained inside a cue sheet.
 * The list of tracks is retrieved from a .cue file by the {@link pCUESheet#getTracks()} method.
 * <p>
 * @author Genialist Software Ltd
 * @version 0.9.23
 * @see pCUESheet
 */
public class pCUETrack extends pHasMetadataImpl implements pHasID {

	/**************************************************************************/
	/***  DEFINITIONS  ********************************************************/
	/**************************************************************************/

	/** An empty array of this object that can be used by other classes. */
	public static final pCUETrack[] CUETRACKS_EMPTY_ARRAY = new pCUETrack[]{};
	
	/** @since 0.9.29 */
	private static final Set<String> SUPPORTED_KEYS = new HashSet<>(6);
	
	static {
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_ARTIST);
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_GENRE);
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_TITLE);
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_TRACK);
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_TV_EPISODE);
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_YEAR);
	}

	/**
	 * The list of <code>MNAME</code> properties from {@link pMediaUtil} class supported by this object meta-data interface.
	 */
	private static final Set<String> KEYS = Collections.unmodifiableSet(SUPPORTED_KEYS);
	
	/**************************************************************************/
	/***  RUNTIME DATA  *******************************************************/
	/**************************************************************************/
	
	/** 
	 * The starting time in milliseconds from the beginning of the cue media.
	 * <p>
	 * @see #getStart()
	 * @see #setStart(long)
	 */
	private long m_start = 0;

	/** 
	 * The end time in milliseconds from the beginning of the cue media.
	 * <p>
	 * @see #getEnd()
	 * @see #setEnd(long)
	 */
	private long m_end = -1;
	
	/**************************************************************************/
	/***  CONSTRUCTORS  *******************************************************/
	/**************************************************************************/
	
	/**
	 * Constructs a new <code>pCUETrack</code> object.
	 * <p>
	 * @param p_id The identifier of the track. This is used as {@link pMediaUtil#MNAME_TRACK} meta-data.
	 */
	public pCUETrack(final String p_id) {
		super();
		getMetadata().put(pMediaUtil.MNAME_TRACK, p_id);
	}
	
	/**
	 * Constructs a new <code>pCUETrack</code> object.
	 * <p>
	 * @param p_id The identifier of the track. This is used as {@link pMediaUtil#MNAME_TRACK} meta-data.
	 * @param p_start The starting time in milliseconds from the beginning of the cue media.
	 * @param p_end The end time in milliseconds from the beginning of the cue media.
	 */
	public pCUETrack(final String p_id, final long p_start, final long p_end) {
		super();
		getMetadata().put(pMediaUtil.MNAME_TRACK, p_id);
		setStart(p_start);
		setEnd(p_end);
	}
	
	/**************************************************************************/
	/***  ACCESSOR METHODS  ***************************************************/
	/**************************************************************************/
	
	/**
	 * Sets the starting time in milliseconds from the beginning of the cue media.
	 * <p>
	 * @see #getStart()
	 */
	public void setStart(final long p_index) {
		m_start = p_index;
	}

	/**
	 * Sets the end time in milliseconds from the beginning of the cue media.
	 * <p>
	 * @see #getEnd()
	 */
	public void setEnd(final long p_index) {
		m_end = p_index;
	}
	
	/**
	 * Gets the starting time in milliseconds from the beginning of the cue media.
	 * <p>
	 * @see #setStart(long)
	 */
	public long getStart() {
		return m_start;
	}

	/**
	 * Gets the end time in milliseconds from the beginning of the cue media.
	 * <p>
	 * @see #setEnd(long)
	 */
	public long getEnd() {
		return m_end;
	}
	
	/**
	 * Gets the duration of the track.
	 */
	public long getDuration() {
		return (m_end<0)? 0 : m_end - m_start; 
	}
	
	/**************************************************************************/
	/***  METHODS  ************************************************************/
	/**************************************************************************/
	
	/**
	 * Gets the list of meta-data identifiers supported by this object.
	 * <p>
	 * This implementation returns the same values as {@link #getMetadataKeys()} method.
	 * <p>
	 * @return The list of <code>MNAME</code> properties from {@link pMediaUtil} class supported by this object (can be <code>null</code> or empty).
	 */
	@Override	
	public Set<String> getMetadataSupportedKeys() {
		return getMetadataKeys();
	}
	
	/**
	 * Saves this track into an output stream in the .CUE format.
	 * <p>
	 * @param i_writer The output stream.
	 * @throws IOException In case of failure.
	 */
	public void save(final OutputStreamWriter i_writer) throws IOException {
		
		i_writer.write("  TRACK ");
		i_writer.write(getID());
		i_writer.write(" AUDIO\n");
				
		pCUESheet.writeField(i_writer, getMetadata().getString(pMediaUtil.MNAME_GENRE), "    REM GENRE");
		
		pCUESheet.writeField(i_writer, getMetadata().getString(pMediaUtil.MNAME_YEAR), "    REM DATE");
		
		final String i_value = getMetadata().getString(pMediaUtil.MNAME_TV_EPISODE);
		if (pString.isValid(i_value)) {
			i_writer.write("    REM ");
			i_writer.write(pMediaUtil.MNAME_TV_EPISODE);
			i_writer.write(" \"");
			i_writer.write(i_value);
			i_writer.write("\"\n");
		}
		
		pCUESheet.writeField(i_writer, getMetadata().getString(pMediaUtil.MNAME_ARTIST), "    PERFORMER");
		
		pCUESheet.writeField(i_writer, getMetadata().getString(pMediaUtil.MNAME_TITLE), "    TITLE");

		i_writer.write("    INDEX 01 ");
		i_writer.write(time(getStart()));
		i_writer.write("\n");
	}

	/**
	 * Returns a string representation of this track in the .CUE format.
	 */
	@Override
	public String toString() {
		final pBaseStringBuilder i_result = new pBaseStringBuilder();
		
		i_result.append("  TRACK ");
		i_result.append(getID());
		i_result.append(" AUDIO\n");
		
		String i_value = getMetadata().getString(pMediaUtil.MNAME_GENRE);
		if (pString.isValid(i_value)) {
			i_result.append("    REM GENRE \"");
			i_result.append(i_value);
			//0.9.8
			i_result.append('\"');
			i_result.append('\n');
		}
		
		i_value = getMetadata().getString(pMediaUtil.MNAME_YEAR);
		if (pString.isValid(i_value)) {
			i_result.append("    REM DATE \"");
			i_result.append(i_value);
			//0.9.8
			i_result.append('\"');
			i_result.append('\n');
		}
		
		i_value = getMetadata().getString(pMediaUtil.MNAME_ARTIST);
		if (pString.isValid(i_value)) {
			i_result.append("    PERFORMER \"");
			i_result.append(i_value);
			//0.9.8
			i_result.append('\"');
			i_result.append('\n');
		}
		
		i_value = getMetadata().getString(pMediaUtil.MNAME_TITLE);
		if (pString.isValid(i_value)) {
			i_result.append("    TITLE \"");
			i_result.append(i_value);
			//0.9.8
			i_result.append('\"');
			i_result.append('\n');
		}

		i_result.append("    INDEX 01 ");
		i_result.append(time(getStart()));
		i_result.append('\n');
		
		return i_result.toString();
	}
	
	private String time(final long millis) {
		return String.format("%02d:%02d:00",
			TimeUnit.MILLISECONDS.toMinutes(millis),// - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), 
			TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
		);
	}	
	
	/***************************************************************************/
	/***  pHasID METHODS  ******************************************************/
	/***************************************************************************/
	
	/**
	 * Gets the unique identifier of the object.
	 * <p>
	 * @return The unique identifier (can be <code>null</code>). 
	 */
	public String getID() {
		return getMetadata().getString(pMediaUtil.MNAME_TRACK);
	}

	/***************************************************************************/
	/***  STATIC METHODS  ******************************************************/
	/***************************************************************************/

	/**
	 * Transforms an integer into track ID.
	 * <p>
	 * @param n The integer (must starts with 1).
	 */
	public static String trackID(final int n) {
		return pString.toString2Digits(n);
	}

	/**
	 * Gets the list of meta-data identifiers supported by this class.
	 * <p>
	 * This implementation returns:
	 * <ul>
	 * <li>{@link pMediaUtil#MNAME_ARTIST},
	 * <li>{@link pMediaUtil#MNAME_GENRE},
	 * <li>{@link pMediaUtil#MNAME_TITLE},
	 * <li>{@link pMediaUtil#MNAME_TRACK},
	 * <li>{@link pMediaUtil#MNAME_TV_EPISODE},
	 * <li>{@link pMediaUtil#MNAME_YEAR}.
	 * </ul>
	 * <p>
	 * @return The list of <code>MNAME</code> properties from {@link pMediaUtil} class supported by this object (can be <code>null</code> or empty).
	 */
	public static Set<String> getMetadataKeys() {
		return KEYS;
	}
}

/******************************************************************************/
/***  END OF FILE  ************************************************************/
/******************************************************************************/
