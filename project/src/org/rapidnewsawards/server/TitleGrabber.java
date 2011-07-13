package org.rapidnewsawards.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleGrabber {

	public static Pattern p = Pattern.compile(".*<[tT][iI][tT][lL][eE]>\\s*(.*?)\\s*</[tT][iI][tT][lL][eE]>.*", Pattern.DOTALL);
	
	public static String getTitle(String urlStr) {

		// &#039; = "
		// &#8221; = ”
		// &#8220; = “
		// &#8217; = ’
        try {
            URL url = new URL(urlStr);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String document = new String();
            String line;
            
            while ((line = reader.readLine()) != null) {
                document = document + line;
                String title = tryGrab(document);
                if (title != null) {
                	return title;
                }
            }
            reader.close();
            return "?";
        } catch (MalformedURLException e) {
            return "Bad URL";
        } catch (IOException e) {
            return "?";
        }
	}
	
	public static String tryGrab(String input) {
		 Matcher m = p.matcher(input);
		if (m.matches()) {
			return m.group(1);
		}
		else {
			return null;
		}
	}
}
