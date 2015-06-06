package org.mambofish.xmldb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPathConstants;

import org.josql.Query;
import org.mambofish.xmldb.reader.XPathReader;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLDatabase {

	private static String dbPath; 
	private static String dbRoot; 

	private static Collection<String> xmlTables = new ArrayList<String>();
	private static Map<Class, Map<String, XMLDataObject>> data = new HashMap<Class, Map<String, XMLDataObject>>();
	
	private static Logger logger = Logger.getLogger(XMLDatabase.class.getName());
	
	static {
		try {
			Properties props = new Properties();
			ClassLoader cl = XMLDatabase.class.getClassLoader();
			URL url = cl.getResource("xmldb.properties");
			
			logger.setLevel(Level.WARNING);
			logger.info("<xmldb: url=" + url.toString() + "/>");
			
			props.load(new FileInputStream(url.getFile()));
			
			dbRoot = props.getProperty("dbRoot");
			dbPath = props.getProperty("dbPath");
			
			logger.info("<xmldb: dbPath=" + dbPath + "/>");
			logger.info("<xmldb: dbRoot " + dbRoot + "/>");
			
			File dir = new File(dbPath);
			String[] filenames = dir.list();
			for (int i = 0; i < filenames.length; i++) {
				String filename = filenames[i];
				if (filename.endsWith(".xml")) {
					logger.info("<xmldb: loading " + filename + "/>");
					xmlTables.add(filename);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads an entire domain class into memory
	 * @param clazz
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SecurityException
	 * @throws DOMException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 * @throws ParseException 
	 */
	public static Map<String, XMLDataObject> load(Class<? extends XMLDataObject> clazz) throws InstantiationException, IllegalAccessException, SecurityException, DOMException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, ParseException {
		
		Map<String, XMLDataObject> map = data.get(clazz);
		if (map == null || map.isEmpty()) {
			map = new HashMap<String, XMLDataObject>();
			XPathReader reader = reader(clazz.getSimpleName() + ".xml");
			NodeList nodes = (NodeList)reader.read(collectionName(clazz), XPathConstants.NODESET);
			for (int index = 0; index < nodes.getLength(); index++) {
				XMLDataObject obj = (XMLDataObject) clazz.newInstance();
				Node data = nodes.item(index);
				obj.read(data);
				map.put(obj.getId(), obj);
			}
			data.put(clazz, map);
		}
		
		return map;
	}
	
	/**
	 * writes domain model classes into an XML database
	 * @param fileName
	 * @throws FileNotFoundException 
	 */
	public static void write(Class<? extends XMLDataObject> clazz, Collection<XMLDataObject> objects) throws FileNotFoundException {
		String className = clazz.getSimpleName();
		PrintStream p = new PrintStream(dbPath + className + ".xml");
		p.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		p.println("<data>");
		for (XMLDataObject obj : objects) {
			p.println(obj.toXML());
		}
		p.println("</data>");
		p.close();
	}

	@SuppressWarnings("unchecked")
	public static void delete(Class<? extends XMLDataObject> clazz, Collection<String> keys) throws SecurityException, DOMException, IllegalArgumentException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, FileNotFoundException, ParseException {
		
		if (keys.isEmpty())
			return;
		
		/** 
		 * recursively handle all referential integrity constaint violations that would
		 * be caused by the deletion of records. The protocol is simple: objects referencing
		 * deleted objects are themselves deleted. This is too simple, because it assumes
		 * that a reference cannot be null.
		 */
		for (String xmlTable : tableNames()) {
			XPathReader reader = reader(xmlTable);
			Class referencingClass = Class.forName(dbRoot + className(xmlTable));
			Collection<String> referencingIds = new ArrayList<String>();
			for (String key : keys) {
				String expression = nodePath(xmlTable) + "/*[@ref=\"" + key + "\"]";
				NodeList nodes = (NodeList)reader.read(expression, XPathConstants.NODESET);
				if (nodes.getLength() > 0) {
					for (int index = 0; index < nodes.getLength(); index++) {
						Node ref = nodes.item(index);
						Node parent = ref.getParentNode();
						String parentId = parent.getAttributes().getNamedItem("id").getNodeValue();
						referencingIds.add(parentId);
					}
				}
			}
			delete(referencingClass, referencingIds);
		}
		Map<String, XMLDataObject> map = load(clazz);
		for (String key : keys) {
			map.remove(key);
		}
		write(clazz, map.values());
	}
	
	private static String nodePath(String filePath) {
		String className = className(filePath);
		return "/data/" + className;
	}
	
	private static String className(String filePath) {
		return filePath.substring(0,1).toUpperCase() + 
		       filePath.substring(1, filePath.indexOf(".xml"));
	}

	public static String path() {
		return dbPath;
	}

	public static String root() {
		return dbRoot;
	}
	
	public static XMLDataObject load(Class<? extends XMLDataObject> clazz, String code) throws InstantiationException, IllegalAccessException, SecurityException, DOMException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, ParseException {
		return load(clazz).get(code);
	}
	
	private static Collection<String> tableNames() {
		return xmlTables;
	}

	private static XPathReader reader(String xmlTable) {
		return new XPathReader(dbPath + xmlTable);	
	}
	
	private static String collectionName(Class<? extends XMLDataObject> clazz) {
		return "data/" + clazz.getSimpleName();
	}

	public static void save(Class<? extends XMLDataObject> clazz, XMLDataObject object) throws FileNotFoundException, SecurityException, DOMException, IllegalArgumentException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, ParseException {
		Map<String, XMLDataObject> map = load(clazz);
		// new objects must be added
		if (object.getId() == null) { 
			object.setId(nextSequence(clazz));
			logger.info("Saving new object");
			map.put(object.getId(), object);
		} else {
			logger.info("Updating object");
		}
		write(clazz, map.values());
	}

	public static String nextSequence(Class<? extends XMLDataObject> clazz) throws SecurityException, DOMException, IllegalArgumentException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, ParseException {
		Map<String, XMLDataObject> map = load(clazz);
		return String.valueOf(map.size() + 1);
	}
	
	public static List query(Class<? extends XMLDataObject>clazz, String qry) throws Exception {
		Map<String, XMLDataObject> map = load(clazz);
		Query query = new Query();
		String sql = "select * from " + clazz.getName() + " where " + qry;
		logger.info("<xmldb: query='" + sql + "'/>");
		logger.info("<xmldb: records=" + map.values().size() + "/>");
		query.parse(sql);
		return query.execute(map.values()).getResults();
	}

	public static List query(String sql) throws Exception {
		logger.info("<xmldb: query='" + sql + "'/>");
		int from = sql.indexOf("from " + dbRoot);
		int space = sql.substring(from + 5).indexOf(" ");
		String className;
		if (space < from) {
			className = sql.substring(from + 5);
		} else {
			className = sql.substring(from + 5, space + from + 5);
		}
		logger.info("<xmldb: class='" + className + "'/>");
		Class clazz = Class.forName(className);
		Map<String, XMLDataObject> map = load(clazz);
		logger.info("<xmldb: records=" + map.values().size() + "/>");
		Query query = new Query();
		query.parse(sql);
		return query.execute(map.values()).getResults();
	}

	public static List<?> fetch(Class<? extends XMLDataObject>clazz) throws Exception {
		Map<String, XMLDataObject> map = load(clazz);
		Query query = new Query();
		String sql = "select * from " + clazz.getName() + " order by id";
		logger.info("<xmldb: query='" + sql + "'/>");
		logger.info("<xmldb: records=" + map.values().size() + "/>");
		query.parse(sql);
		return query.execute(map.values()).getResults();
	}
}