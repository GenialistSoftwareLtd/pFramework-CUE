/*******************************************************************************
 * Copyright (c) 2017-2020 Genialist Software Ltd.
 * All rights reserved.
 ******************************************************************************/

package tv.genialist.fwrk.media.cue;

import java.io.File;
import java.util.regex.Pattern;

import tv.genialist.fwrk.media.pMediaUtil;
import tv.genialist.ptools.string.pString;

/**
 * <p>
 * @author Genialist Software Ltd
 * @version 0.9.8
 */
public class test_pCUESheet {

	/**************************************************************************/
	/***  MAIN METHOD  ********************************************************/
	/**************************************************************************/
	
	/**
	 * Executes unit-tests against this class and prints out the results.
	 * <p>
	 * @param p_args The command line arguments (ignored).
	 */
	public static void main(final String[] p_args) {
		
		try {
			new pCUESheet(new File("C:\\Music\\=Techno, House & Dance\\DJ Armin van Buuren\\Boundaries Of Imagination\\DJ Armin van Buuren # D1 - Boundaries Of Imagination.cue"), -1);
		} 
		catch (final Exception e) {
			e.printStackTrace();
		}
	
		pCUESheet i_sheet = new pCUESheet(-1);
		i_sheet.getMetadata().put(pMediaUtil.MNAME_TITLE, "This is my song...");
		i_sheet.getMetadata().put(pString.STRING_NAME, "dummy");
		System.out.println(i_sheet.toString());
		
		i_sheet.insertTrack(10000);
		System.out.println(i_sheet.toString());
	
		i_sheet.insertTrack(20000);
		System.out.println(i_sheet.toString());
		
		i_sheet.insertTrack(5000);
		System.out.println(i_sheet.toString());
	
		i_sheet.insertTrack(10000);
		System.out.println(i_sheet.toString());
		
		Pattern i_test = Pattern.compile("foo \"{0,1}([^\"]*)\"{0,1}");
		for(String i_text : new String[]{ "foo \"bar\"", "foo bar", "foo bar xxx" }) {
			System.out.print("match("); System.out.print(i_text); System.out.print("): "); System.out.println(i_test.matcher(i_text).matches());
			if (i_test.matcher(i_text).matches()) {
				System.out.print("match("); System.out.print(i_text); System.out.print("): true: "); System.out.println(i_test.matcher(i_text).group(1));
			}
			else {
				System.out.print("match("); System.out.print(i_text); System.out.println("): false");
			}
		}
	}
}

/******************************************************************************/
/***  END OF FILE  ************************************************************/
/******************************************************************************/
