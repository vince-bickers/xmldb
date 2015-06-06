package org.mambofish.xmldb;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLDataObject {

	private String id = null;
	
    protected void read(Node data) throws SecurityException, DOMException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchFieldException, ParseException {
    	setId(data.getAttributes().getNamedItem("id").getNodeValue());
		NodeList fields = data.getChildNodes();
		for (int index = 0; index < fields.getLength(); index++) {
			Node field = fields.item(index);
			if (field.getNodeType() == Node.ELEMENT_NODE) {
				if (field.getAttributes().getNamedItem("ref") != null) {
					String refClassName = XMLDatabase.root() + field.getAttributes().getNamedItem("class").getNodeValue();
					XMLDataObject refObj = (XMLDataObject) Class.forName(refClassName).newInstance();
					refObj = XMLDatabase.load(refObj.getClass(), field.getAttributes().getNamedItem("ref").getNodeValue());
					setField(field.getNodeName(), refObj);
				} else {
					setField(field.getNodeName(), field.getTextContent());
				}
			}
		}
	}
    
    public Object value(String nodeName) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    	Field f = this.getClass().getDeclaredField(nodeName);
		f.setAccessible(true);
		return f.get(this);
    }
    
	public void setField(String nodeName, Object value) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, ParseException {
		Field f = this.getClass().getDeclaredField(nodeName);
		Class clazz = f.getType();
		if (value instanceof String) {
			if (clazz == Integer.class) {
				value = Integer.parseInt((String) value);
			} else if (clazz == Float.class) {
				value = Float.parseFloat((String) value);
			} else if (clazz == java.util.Date.class) {
				value = new SimpleDateFormat("dd MMM yyyy").parse(value.toString());
			} else if (clazz == BigDecimal.class) {
				value = new BigDecimal(value.toString());
			} else if (clazz == Boolean.class) {
				value = Boolean.parseBoolean(value.toString());
			}
		}
		f.setAccessible(true);
		f.set(this, value);
	}

	public void save() throws FileNotFoundException, SecurityException, DOMException, IllegalArgumentException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, ParseException {
		XMLDatabase.save(getClass(), this);
	}
	
	public void delete() throws SecurityException, DOMException, IllegalArgumentException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, FileNotFoundException, ParseException {
		Collection<String> deletions = new ArrayList<String>();
		deletions.add(getId());
		XMLDatabase.delete(getClass(), deletions);
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public List<String> fieldNames() {
		List<String> l = new ArrayList<String>();
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			String fn = f.getName();
			Object o = null;
			try {
				f.setAccessible(true);
				o = f.get(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (o != null) {
				l.add(f.getName());
			}
		}
		return l;
	}

	public String toXML() {
		StringBuffer xml = new StringBuffer();
		xml.append("\t<");
		xml.append(this.getClass().getSimpleName());
		xml.append(" id=\"");
		xml.append(this.getId());
		xml.append("\">\n");
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			String fn = f.getName();
			Object o = null;
			try {
				f.setAccessible(true);
				o = f.get(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (o != null) {
				xml.append("\t\t<");
				xml.append(fn);
				if (o instanceof XMLDataObject) {
					xml.append(" class=\"");
					xml.append(o.getClass().getSimpleName());
					xml.append("\" ref=\"");
					xml.append( ((XMLDataObject)o).getId());
					xml.append("\"/>\n");
				}
				else {
					xml.append(">");
					// TODO: transform to correct format
					// for different types (e.g date)
					xml.append(xmlClean(stringify(o).trim()));
					xml.append("</");
					xml.append(fn);
					xml.append(">\n");
				}
			}
			
		}
		xml.append("\t</");
		xml.append(this.getClass().getSimpleName());
		xml.append(">\n");
		return xml.toString();
	}

	private String stringify(Object o) {
		if (o instanceof Date) {
			return new SimpleDateFormat("dd MMM yyyy").format((Date) o);
		}
		return o.toString();
	}

	private String xmlClean(String xml) {
		xml = xml.replace("&", "&amp;");
		xml = xml.replace("'", "&apos;");
		xml = xml.replace("<", "&lt;");
		xml = xml.replace(">", "&gt;");
		
		for (int i = 0; i < xml.length(); i++) {
			char c = xml.charAt(i);
			if (c < 32 || c > 127) {
				xml = xml.replace(c, ' ');
			}
		}
		return xml;
	}
}