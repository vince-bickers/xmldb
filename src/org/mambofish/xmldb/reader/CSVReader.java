package org.mambofish.xmldb.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

public class CSVReader {

	private Collection<String[]> records = new ArrayList<String[]>();
	private static final int MAX_FIELDS = 255;
	
	public Collection<String[]> records() {
		return records;
	}
	
	public void readFile(String filename) {
		try {
			BufferedReader r = new BufferedReader(new FileReader(new File(filename)));
			String line;
			while ((line = r.readLine()) != null) {
				String data[] = new String[MAX_FIELDS];
				int index = 0;
				StringTokenizer st = new StringTokenizer(line, ",");
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					if (token.startsWith("\"")) {
						while (st.hasMoreTokens()) {
							token += st.nextToken();
							if (token.endsWith("\""))
								break;
						}
					}
					data[index++] = token;
				}
				records.add(data);
			}
			r.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
