package org.sg.ehcache.pounder;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SampleObject implements Serializable {
	
	public SampleObject(int min, int max) {		
		this.setID(createUniqueID());
		this.setName(getRandomName());
		this.setNumber((int) getRandomNumber());
		this.setContent(buildValue(min, max));
	}
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public String ID; 
	public String Name;
	public Integer Number; 
	public byte[] content;
	
	public String getID() {
		return ID;
	}
	public void setID(String iD) {
		ID = iD;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public Integer getNumber() {
		return Number;
	}
	public void setNumber(Integer number) {
		Number = number;
	}
	public byte[] getContent() {
		return content;
	}
	public void setContent(byte[] content) {
		this.content = content;
	}
	
	
	private String createUniqueID() {	
		String dateString = getDate();
    	
    	String ID = "EHP" + dateString + getRandomNumber(); 
        return ID.replace(".", "");
	}
	
	private double getRandomNumber() {
		return Math.random();
	}
	
	
	private String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    	java.util.Date date = new java.util.Date();
    	String dateString = dateFormat.format(date);
		return dateString;
	}
	
	private String getRandomName() {
		List<String> names = new LinkedList<String>();
		names.add("Sepp");
		names.add("Hans");
		names.add("Paul");
		names.add("Charles");
		names.add("Dirk");
		names.add("Fabian");
		names.add("Stephan");
		names.add("Steve");
		names.add("Armin");
		names.add("Cindy");
		names.add("Abdul");
		names.add("Bart");
		names.add("Cyrus");
		names.add("Homer");
		names.add("Joe");

		Random rand = new Random();
		int choice = rand.nextInt(names.size());
		return names.get(choice);

	}
	
	
	/**
	 * create an entry of random size within the tests params and add in a basic
	 * check sum in the beginning and end of the value
	 * 
	 * @return byte[] for the value
	 */
	private byte[] buildValue(int min, int max) {
		Random r = new Random();
		int size = r.nextInt(max - min + 10) + max;
		byte[] bytes = new byte[size];
/*		for (int i = 0; i < bytes.length; i++) {
			if (i < 5) {
				bytes[i] = (byte) i;
			} else if ((bytes.length - i) < 5) {
				bytes[i] = (byte) (bytes.length - i);
			} else {
				bytes[i] = (byte) r.nextInt(128);
			}
		}*/
		return bytes;
	}
	

}
