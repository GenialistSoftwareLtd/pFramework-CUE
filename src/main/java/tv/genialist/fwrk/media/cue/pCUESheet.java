/*******************************************************************************
 * Copyright (c) 2017-2025 Genialist Software Ltd.
 * All rights reserved.
 ******************************************************************************/

package tv.genialist.fwrk.media.cue;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import tv.genialist.fwrk.document.file.pCUESheetFileDocumentType;
import tv.genialist.fwrk.media.pMediaUtil;
import tv.genialist.fwrk.media.impl.pHasMetadataImpl;
import tv.genialist.ptools.io.writer.pOutputStreamWriter;
import tv.genialist.ptools.io.pSingleThreadBufferedReader;
import tv.genialist.ptools.lang.pBaseStringBuilder;
import tv.genialist.ptools.lang.util.pStringUtil;
import tv.genialist.ptools.string.pPattern;
import tv.genialist.ptools.string.pString;
import tv.genialist.ptools.trace.pTraceImpl;

/**
 * The <code>pCUESheet</code> class allows reading cue sheet from files or input streams.
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Cue_sheet_%28computing%29">Wikipedia</a>: 
 * <i>A cue sheet, or cue file, is a metadata file which describes how the tracks of a CD or DVD are laid out. 
 * Cue sheets are stored as plain text files and commonly have a ".cue" filename extension.</i> 
 * <p>
 * <b>Example:</b>
 * <dl>
 * <dd><code>
 * pCUESheet i_cue = new pCUESheet(new File(".../cd.cue"), i_default_length);
 * <p>
 * //set default title<br>						
 * i_cue.getMetadata().put(pMediaUtil.MNAME_TITLE, "U2 - The Best Of");<br>
 * <br>
 * for(pCUETrack i_track : i_cue.getTracks()) {<br>
 * &nbsp;&nbsp;System.out.println("Track " + i_track.getID());<br>
 * &nbsp;&nbsp;System.out.println("  Duration: " + i_track.getDuration());<br>
 * &nbsp;&nbsp;System.out.println("  Start: " + i_track.getStart() + " ms");<br>
 * &nbsp;&nbsp;System.out.println("  End: " + i_track.getEnd() + " ms");<br>
 * &nbsp;&nbsp;System.out.println("  Title: " + i_track.getMetadata().get(pMediaUtil.MNAME_TITLE));<br>
 * &nbsp;&nbsp;System.out.println("  Artist: " + i_track.getMetadata().get(pMediaUtil.MNAME_ARTIST));<br>
 * }<br>	
 * </code>
 * </dl>
 * <p>
 * @author Genialist Software Ltd
 * @version 0.9.29
 */
public class pCUESheet extends pHasMetadataImpl {

	/**************************************************************************/
	/***  DEFINITIONS  ********************************************************/
	/**************************************************************************/

	/** The prefix used in trace and log messages. */
	public static final String TRACE_PREFIX = "CUESheet";
	
	/** @since 0.9.29 */
	private static final Set<String> SUPPORTED_KEYS = new HashSet<>(6);
	
	static {
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_ARTIST);
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_GENRE);
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_TITLE);
		SUPPORTED_KEYS.add(pMediaUtil.MNAME_YEAR);
	}
	
	/**
	 * The list of <code>MNAME</code> properties from {@link pMediaUtil} class supported by this object meta-data interface.
	 */
	private static final Set<String> KEYS = Collections.unmodifiableSet(SUPPORTED_KEYS);
	
	/*************************************************************************/
	/***  RUNTIME DATA  ******************************************************/
	/*************************************************************************/

	/** @since 0.9.29 */
	private static final pTraceImpl TRACE = pTraceImpl.getTrace(pCUESheet.class, TRACE_PREFIX);
	
	/** The line regular expression for the cue sheet metadata. */
	private static final pPattern[] m_data_sheet_patterns = new pPattern[] {
		new pPattern(pMediaUtil.MNAME_ARTIST, new String[]{ "PERFORMER\\s+\"([^\"]*)\"", "PERFORMER\\s+([^ \\t\\n]*)" }),
		new pPattern(pMediaUtil.MNAME_GENRE, new String[]{ "REM GENRE\\s+\"([^\"]*)\"", "REM GENRE\\s+([^ \\t\\n]*)" }),
		new pPattern(pMediaUtil.MNAME_TITLE, new String[]{ "TITLE\\s+\"([^\"]*)\"", "TITLE\\s+([^ \\t\\n]*)" }),
		new pPattern(pMediaUtil.MNAME_YEAR, new String[]{ "REM DATE\\s+\"([^\"]*)\"", "REM DATE\\s+([^ \\t\\n]*)" }),
	};

	private static final pPattern[] m_data_track_patterns = new pPattern[] {
		new pPattern(pMediaUtil.MNAME_ARTIST, new String[]{ "PERFORMER\\s+\"([^\"]*)\"", "PERFORMER\\s+([^ \\t\\n]*)" }),
		new pPattern(pMediaUtil.MNAME_GENRE, new String[]{ "REM GENRE\\s+\"([^\"]*)\"", "REM GENRE\\s+([^ \\t\\n]*)" }),
		new pPattern(pMediaUtil.MNAME_TITLE, new String[]{ "TITLE\\s+\"([^\"]*)\"", "TITLE\\s+([^ \\t\\n]*)" }),
		new pPattern(pMediaUtil.MNAME_TV_EPISODE, new String[]{ 
			pStringUtil.concat("REM ", pMediaUtil.MNAME_TV_EPISODE, "\\s+\"([^\"]*)\""), 
			pStringUtil.concat("REM ", pMediaUtil.MNAME_TV_EPISODE, "\\s+([^ \\t\\n]*)")
		}),
		new pPattern(pMediaUtil.MNAME_YEAR, new String[]{ "REM DATE\\s+\"([^\"]*)\"", "REM DATE\\s+([^ \\t\\n]*)" }),
	};
	
	/** The line regular expression for a track index. */
	private static final pPattern m_line_index = new pPattern("INDEX\\s+01\\s+([0-9][0-9]):([0-9][0-9]):([0-9][0-9])");
	
	/**
	 * The list of audio tracks present inside the cue sheet.
	 */
	private ArrayList<pCUETrack> m_data_tracks;
	
	/**
	 * The total length of the cue sheet media, if known.
	 * It is used for the end time of the last track when the sheet is parsed or new tracks are inserted.
	 */
	private long m_total_length = -1;
	
	/** The "FILE..." line read from an existing cue sheet. */
	private String m_data_media;

	/**************************************************************************/
	/***  CONSTRUCTORS  *******************************************************/
	/**************************************************************************/
	
	/**
	 * Constructs a new <code>pCUESheet</code> empty object.
	 * <p>
	 * @param p_total_length The total length of the cue sheet media, if known (in milliseconds).
	 */
	public pCUESheet(final long p_total_length) {
		super();
		m_total_length = p_total_length;
	}
	
	/**
	 * Constructs a new <code>pCUESheet</code> object from an existing .cue file.
	 * <p>
	 * @param p_file The .cue file.
	 * @param p_total_length The total length of the cue sheet media, if known (in milliseconds).
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws Exception If the file reading or parsing failed.
	 */
	public pCUESheet(final File p_file, final long p_total_length) throws IOException {
		this(p_total_length);
		
		try (FileInputStream i_stream = new FileInputStream(p_file)) {
			parse(i_stream);
		}
	}
	
	/**
	 * Constructs a new <code>pCUESheet</code> object from an input stream.
	 * <p>
	 * @param p_stream The input stream.
	 * @param p_total_length The total length of the cue sheet media, if known (in milliseconds).
	 * @throws IOException 
	 * @throws Exception If the stream reading or parsing failed.
	 */
	public pCUESheet(final InputStream p_stream, final long p_total_length) throws IOException {
		this(p_total_length);
		
		parse(p_stream);
	}

	/**************************************************************************/
	/***  METHODS  ************************************************************/
	/**************************************************************************/
	
	/**
	 * Extracts the audio tracks from an input stream. 
	 * <p>
	 * @param p_stream The input stream.
	 * @throws IOException 
	 * @throws Exception If the stream reading or parsing failed.
	 */
	private void parse(final InputStream p_stream) throws IOException {	
		
		String i_line = null;
		
		m_data_tracks = new ArrayList<>();
		
		final pSingleThreadBufferedReader i_reader =new pSingleThreadBufferedReader(new InputStreamReader(p_stream));
		while(true) {
			i_line = i_reader.readLine();
			if (null==i_line) return;
			i_line = i_line.trim();
			if (!pString.isValid(i_line)) continue;
			
			parseLine(i_reader, i_line);
		}
	}
	
	private void parseLine(final pSingleThreadBufferedReader i_reader, String i_line) throws NumberFormatException, IOException {
		Matcher i_matcher;
		
		for(pPattern i_pattern : m_data_sheet_patterns)
			if (null!=(i_matcher = i_pattern.matcher(i_line))) {
				getMetadata().put(i_pattern.getID(), i_matcher.group(1));
				return;
			}
			
			if (i_line.startsWith("FILE ") && i_line.endsWith(" MP3")) {
				//TODO: OPTIMIZE
				i_line = i_line.substring("FILE ".length());
				i_line = i_line.substring(0,  i_line.length()-" MP3".length()).trim();
				m_data_media = i_line;
				if ((m_data_media.charAt(0)=='\"' && m_data_media.charAt(m_data_media.length()-1)=='\"') || 
					(m_data_media.charAt(0)=='\'' && m_data_media.charAt(m_data_media.length()-1)=='\''))
					m_data_media = m_data_media.substring(1, m_data_media.length()-1);
				
				parseFile(i_reader);
			}
	}
	
	/**
	 * Extracts the audio tracks from an input stream after the "FILE" line. 
	 * <p>
	 * @param p_reader The input reader.
	 * @param p_title The default title found in the cue sheet (can be <code>null</code> or empty).
	 * @param p_performer The default artist/performer found in the cue sheet (can be <code>null</code> or empty).
	 * @throws IOException 
	 * @throws NumberFormatException 
	 * @throws Exception If the stream reading or parsing failed.
	 */
	private void parseFile(final pSingleThreadBufferedReader p_reader) throws NumberFormatException, IOException {
			
		String i_line = null;

		while((i_line = p_reader.readLine())!=null) {
			i_line = i_line.trim();
			if (!pString.isValid(i_line)) continue;
			
			if (i_line.startsWith("TRACK ") && i_line.endsWith(" AUDIO")) 
				while(parseTrack(p_reader));
		}
	}
	
	/**
	 * Extracts the audio tracks from an input stream after the "FILE" line. 
	 * <p>
	 * @param p_reader The input reader.
	 * @param p_title The default title found in the cue sheet (can be <code>null</code> or empty).
	 * @param p_performer The default artist/performer found in the cue sheet (can be <code>null</code> or empty).
	 * @return <code>true</code> if there is other tracks to parse, otherwise <code>false</code>.
	 * @throws IOException 
	 * @throws NumberFormatException 
	 * @throws Exception If the stream reading or parsing failed.
	 */
	private boolean parseTrack(final pSingleThreadBufferedReader p_reader) throws NumberFormatException, IOException {
				
		final pCUETrack i_track = new pCUETrack(pCUETrack.trackID(m_data_tracks.size()+1));
		String i_line = null;
		
		for(String i_key : SUPPORTED_KEYS) {
			i_line = getMetadata().getString(i_key);
			if (pString.isValid(i_line)) {
				i_track.getMetadata().put(i_key, i_line);
				i_track.getMetadata().put(pMediaUtil.MNAME_TITLE, pStringUtil.concat(i_line, " (", i_track.getID(), ")"));
			}
		}
		Matcher i_matcher;

		while((i_line = p_reader.readLine())!=null) {
			i_line = i_line.trim();
			if (!pString.isValid(i_line)) continue;
			
			if (i_line.startsWith("TRACK ")) {
				m_data_tracks.add(i_track);
				return true;
			}
				
			if (null!=(i_matcher = m_line_index.matcher(i_line))) {
				long i_frames = Long.parseLong(i_matcher.group(3), 10);
				i_track.setStart(Long.parseLong(i_matcher.group(1), 10)*60000L + Long.parseLong(i_matcher.group(2), 10)*1000L + 1000L*i_frames/75L);
				if (m_data_tracks.size()>0)
					m_data_tracks.get(m_data_tracks.size()-1).setEnd(i_track.getStart());
				
				continue;
			}
			
			for(pPattern i_pattern : m_data_track_patterns)
				if (null!=(i_matcher = i_pattern.matcher(i_line))) {
					i_track.getMetadata().put(i_pattern.getID(), i_matcher.group(1));
					break;
				}
		}
		
		if (i_track.getEnd()<0 && m_total_length>0)
			i_track.setEnd(m_total_length);
			
		m_data_tracks.add(i_track);
		return false;
	}
	
	/**
	 * Gets the "FILE..." line read from an existing cue sheet.
	 */
	public String getMedia() {
		return m_data_media;
	}
	
	/**
	 * Saves this sheet into a file in the .CUE format.
	 * <p>
	 * @param p_file The output file (the parent directory must exist).
	 * @param p_media The name of the media file that is written in the "FILE...MP3" line.
	 * @throws IOException In case of failure.
	 */
	public void save(final File p_file, final String p_media) throws IOException {
		
		try (FileOutputStream i_fos = new FileOutputStream(p_file); pOutputStreamWriter i_writer = new pOutputStreamWriter(i_fos)) {

			//*** SAVE ARTIST
			writeField(i_writer, getMetadata().getString(pMediaUtil.MNAME_ARTIST), "PERFORMER");
			
			//*** SAVE TITLE
			writeField(i_writer, getMetadata().getString(pMediaUtil.MNAME_TITLE), "TITLE");

			//*** SAVE FILENAME
			i_writer.write("FILE \"");
			i_writer.write(p_media);
			i_writer.write("\" MP3\n");
			
			//*** SAVE TRACKS
			if (null!=m_data_tracks) {
				final int i_len = m_data_tracks.size();
				for(int i=0 ; i<i_len ; i++)
					m_data_tracks.get(i).save(i_writer);
			}
			
			i_writer.flush();
			i_fos.flush();
		}
	}
	
	/**************************************************************************/
	/***  ACCESSOR METHODS  ***************************************************/
	/**************************************************************************/
	
	/**
	 * Gets the list of tracks present inside the cue sheet.
	 * <p>
	 * If a track does not define its own artist/performer, it it set to the artist/performer of the cue sheet.
	 * <p>
	 * If a track does not define its own title, it it set to the title of the cue sheet followed by " (&lt;track&gt;)".
	 * <p>
	 * @return The list of audio tracks (can be empty, but not <code>null</code>).
	 */
	public pCUETrack[] getTracks() {
		return (null!=m_data_tracks)? m_data_tracks.toArray(pCUETrack.CUETRACKS_EMPTY_ARRAY) : pCUETrack.CUETRACKS_EMPTY_ARRAY;
	}
	
	/**
	 * Gets one track by its unique identifier.
	 * <p>
	 * @param p_track_id The unique identifier.
	 * @return The track if found, otherwise <code>null</code>.
	 */
	public pCUETrack getTrack(final String p_track_id) {
		if (null==m_data_tracks)
			return null;
		
		//0.9.8
		final int i_len = m_data_tracks.size();
		for(int i=0 ; i<i_len ; i++) {
			pCUETrack i_track = m_data_tracks.get(i);
			if (pString.fastEquals(i_track.getID(), p_track_id))
				return i_track;
		}
		return null;
	}
	
	/**
	 * Inserts a new track in the list of tracks.
	 * <p>
	 * @param p_time The start time of the new track. If it equals zero or the start time of another existing track, nothing is changed.
	 * @return The new track if inserted, or <code>null</code> if nothing has been changed.
	 */
	public pCUETrack insertTrack(final long p_time) {
		pCUETrack i_result = null;
		
		if (p_time<=0)
			return null;
		
		if (null==m_data_tracks) {
			m_data_tracks = new ArrayList<>();
			
			m_data_tracks.add(new pCUETrack("01", 0, p_time));
			i_result = new pCUETrack("02", p_time, m_total_length);
			m_data_tracks.add(i_result);
			return i_result;
		}
		
		int i_len = m_data_tracks.size();
		for(int i=0 ; i<i_len ; i++) {
			final pCUETrack i_track = m_data_tracks.get(i);
			if (i_track.getStart()==p_time)
				return null;
			if (i_track.getStart()>p_time) {
				i_result = new pCUETrack(pCUETrack.trackID(i+1), p_time, i_track.getStart());
				m_data_tracks.add(i, i_result);
				if (i>0)
					m_data_tracks.get(i-1).setEnd(p_time);
				
				i++;
				for( ; i<m_data_tracks.size() ; i++)
					m_data_tracks.get(i).getMetadata().put(pMediaUtil.MNAME_TRACK, pCUETrack.trackID(i+1));
				return i_result;
			}
		}
		
		// insert at the end
		i_result = new pCUETrack(pCUETrack.trackID(m_data_tracks.size()+1), p_time, m_total_length);
		m_data_tracks.add(i_result);
		m_data_tracks.get(m_data_tracks.size()-2).setEnd(p_time);
		return i_result;
	}
	
	/**************************************************************************/
	/***  METHODS  ************************************************************/
	/**************************************************************************/
	
	/**
	 * Gets the list of meta-data identifiers supported by this object.
	 * <p>
	 * This implementation returns:
	 * <ul>
	 * <li>{@link pMediaUtil#MNAME_ARTIST},
	 * <li>{@link pMediaUtil#MNAME_GENRE},
	 * <li>{@link pMediaUtil#MNAME_TITLE},
	 * <li>{@link pMediaUtil#MNAME_YEAR}.
	 * </ul>
	 * <p>
	 * @return The list of <code>MNAME</code> properties from {@link pMediaUtil} class supported by this object (can be <code>null</code> or empty).
	 */
	@Override
	public Set<String> getMetadataSupportedKeys() {
		return KEYS;
	}
	
	/**
	 * Returns a string representation of this sheet in the .CUE format.
	 */
	@Override
	public String toString() {
		final pBaseStringBuilder i_result = new pBaseStringBuilder();
		
		String i_value = getMetadata().getString(pMediaUtil.MNAME_ARTIST);
		if (pString.isValid(i_value)) {
			i_result.append("PERFORMER \"");
			i_result.append(i_value);
			i_result.append("\"\n");
		}
		
		i_value = getMetadata().getString(pMediaUtil.MNAME_TITLE);
		if (pString.isValid(i_value)) {
			i_result.append("TITLE \"");
			i_result.append(i_value);
			i_result.append("\"\n");
		}
		
		i_value = getMetadata().getString(pString.STRING_NAME);
		if (pString.isValid(i_value)) {
			i_result.append("FILE \"");
			i_result.append(i_value);
			i_result.append("\" MP3\n");
		}

		for(pCUETrack i_track : getTracks())
			i_result.append(i_track.toString());
		
		return i_result.toString();
	}

	/**************************************************************************/
	/***  STATIC METHODS  *****************************************************/
	/**************************************************************************/
	
	/**
	 * Changes metadata into an existing CUE Sheet file.
	 * <p>
	 * @param p_file The .cue file. 
	 * @param p_values The list of metadata values to change. 
	 * The list must contain a value for the {@link pMediaUtil#MNAME_TRACK} property, otherwise the method will throw an exception.
	 * Values that have been successfully changed are removed from the list.
	 * @return <code>true</code> if the update succeeded, otherwise <code>false</code>.
	 * @throws IOException In case of error.
	 */
	public static boolean update(final File p_file, final Map<String,Object> p_values) throws IOException {
		if (!p_file.exists())
			return false;
		if (!p_file.canRead())
			return false;
		if (!pCUESheetFileDocumentType.getDefaultInstance().accept(p_file))
			return false;
		
		//0.9.8
		if (null==p_values)
			return false;
		if (p_values.size()<1)
			return false;

		if (TRACE.isDebugEnabled())
			TRACE.debug("Writing CUE Sheet file: ", p_file.getAbsolutePath(), ": changing data...");			

		try {
			final pCUESheet i_sheet = new pCUESheet(p_file, 0);
			boolean i_save = false;
			
			final String i_track_id = pString.valueOf(p_values.get(pMediaUtil.MNAME_TRACK), null);
			final pCUETrack i_track = i_sheet.getTrack(i_track_id);
			if (null==i_track) {
				throw new IOException("Cannot find CUE Sheet track: ".concat(i_track_id));
			}
			
			final ArrayList<String> i_toremove = new ArrayList<>(p_values.size());
			for(Map.Entry<String,Object> i_entry : p_values.entrySet()) {
				final String i_pname = i_entry.getKey();
				final String i_value = Objects.toString(i_entry.getValue(), null);
			
				if (i_track.getMetadataSupportedKeys().contains(i_pname)) {

					if (TRACE.isInfoEnabled())
						TRACE.info("Writing CUE Sheet file: ", p_file.getAbsolutePath(), ": changing data... Setting: ", i_pname, ": ", i_value);

					final String i_ovalue = i_track.getMetadata().getString(i_pname);
					if (!pString.equals(i_ovalue, i_value)) {
						i_track.getMetadata().put(i_pname, i_value);
						i_save = true;
						
						i_toremove.remove(i_pname);
					}
				}
			}
			
			//*** SAVE THE CHANGES
			if (i_save) {
				i_sheet.save(p_file, i_sheet.getMedia());
					
				//*** REMOVE THE VALUES THAT WE SAVED
				final int i_len = i_toremove.size();
				for(int i=0 ; i<i_len ; i++)
					p_values.remove(i_toremove.get(i));
				
				if (TRACE.isDebugEnabled())
					TRACE.debug("Writing CUE Sheet file: ", p_file.getAbsolutePath(), ": changing data... Done.");
				return true;
			}
			
			//TODO: update mp3 file...
		}
		catch (final IOException ex) {
			if (TRACE.isErrorEnabled())
				TRACE.error("Failed to write data into CUE Sheet file: ", p_file.getAbsolutePath(), ex);
			throw ex;
		}
		return false;
	}
	
	/** 
	 * <p>
	 * @throws IOException 
	 * @since 0.9.29 
	 */
	static void writeField(final OutputStreamWriter i_writer, final String p_value, final String p_name) throws IOException {
		if (pString.isValid(p_value)) {
			i_writer.write(p_name);
			i_writer.write(" \"");
			i_writer.write(p_value);
			i_writer.write("\"\n");
		}
	}
	
	/**************************************************************************/
	/***  MAIN METHOD  ********************************************************/
	/**************************************************************************/
}

/******************************************************************************/
/***  END OF FILE  ************************************************************/
/******************************************************************************/
