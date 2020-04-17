package com.bqniu.lotterydraw.truerandom;

import java.util.ArrayList;

public class TestClient {
	
	public static SimpleRandomOrgLib random;
	public static ArrayList<Integer> tmpArrayInt;
	public static ArrayList<String> tmpArrayString;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		random = new SimpleRandomOrgLib();
		System.out.println("randomNumberBaseTenInt()");
		try {
			tmpArrayInt = random.randomNumberBaseTenInt(20, 1, 46);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		int arrayCount = tmpArrayInt.size();
		System.out.println("arrayCount="+arrayCount);
		
		for (int i=0;i < arrayCount;i++){
			System.out.println("Array Index " + i + ": " +tmpArrayInt.get(i));
		}
		
		
		System.out.println("randomNumberBaseTenString()");
		try {
			tmpArrayString = random.randomNumberBaseTenString(20, 1, 46);
		} catch (Exception e) {
			e.printStackTrace();
		}
		arrayCount = tmpArrayString.size();
		System.out.println("arrayCount="+arrayCount);
		for (int i=0;i < arrayCount;i++){
			System.out.println("Array Index " + i + ": " +tmpArrayString.get(i));
		}
		

	}

}
