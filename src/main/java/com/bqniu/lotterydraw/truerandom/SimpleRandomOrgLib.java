package com.bqniu.lotterydraw.truerandom;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;


/**
 * SimmpleRandomOrgLib() is a class to interface with the Random.org http api in a simple manor. It requires apache httpclient httpcore and logging
 * @author Charles Eakins
 *
 */
public class SimpleRandomOrgLib {
	public ArrayList<Integer> tmpArrayListInt;
	public ArrayList<String> tmpArrayListString;
	public String [] tmpArrayString;
	public String url;
	
	/**
	 * httpGet() returns the contents of a get line by line in an ArrayList	
	 * @param url String of url to get from
	 * @return String ArrayList
	 * @throws Exception
	 */
	public ArrayList<String> httpGet(String url) throws Exception{
		tmpArrayListString = new ArrayList();
		
//		HttpParams httpParams = new BasicHttpParams();
//		HttpConnectionParams.setConnectionTimeout(httpParams, 200);
//		HttpConnectionParams.setSoTimeout(httpParams, 200);
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		
		String responseBody = httpclient.execute(httpget, responseHandler);
		httpclient.getConnectionManager().shutdown();
		
		tmpArrayString = responseBody.split("\n");
		int arrayCount = tmpArrayString.length;
		
		for (int i = 0;i < arrayCount;i++){
			tmpArrayListString.add(tmpArrayString[i]);
		}
		return tmpArrayListString;
	}
	/**
	 * randomNumberBaseTenInt() return an integer ArrayList
	 * @param amountOfNumbersToReturn Integer of how many numbers you want generated
	 * @param minNumber Integer of the minimum number to start with
	 * @param maxNumber Integer of the maximum number to start with
	 * @return Return an Integer ArrayList
	 * @throws Exception
	 */
	public ArrayList<Integer> randomNumberBaseTenInt(int amountOfNumbersToReturn,int minNumber, int maxNumber) throws Exception{
		
		tmpArrayListInt = new ArrayList();

		String url = "http://www.random.org/integers/?num="+Integer.toString(amountOfNumbersToReturn)+"&min="+Integer.toString(minNumber)+"&max="+Integer.toString(maxNumber)+"&col=1&base=10&format=plain&rnd=new";

		tmpArrayListString = this.httpGet(url);
		
		int arrayCount = tmpArrayListString.size();
		
		for (int i = 0;i < arrayCount;i++){
			tmpArrayListInt.add(Integer.parseInt(tmpArrayListString.get(i)));
		}
		
		
		
		return tmpArrayListInt;
		
	}
	
	/**
	 * randomNumberBaseTenString() return a String ArrayList
	 * @param amountOfNumbersToReturn Integer of how many numbers you want generated
	 * @param minNumber Integer of the minimum number to start with
	 * @param maxNumber Integer of the maximum number to start with
	 * @return Return an String ArrayList
	 * @throws Exception
	 */
	public ArrayList<String> randomNumberBaseTenString(int amountOfNumbersToReturn,int minNumber, int maxNumber) throws Exception{


		url = "http://www.random.org/integers/?num="+Integer.toString(amountOfNumbersToReturn)+"&min="+Integer.toString(minNumber)+"&max="+Integer.toString(maxNumber)+"&col=1&base=10&format=plain&rnd=new";
		
		return this.httpGet(url);
		
	}
	/**
	 * randomNumberBaseTwoString() return a String ArrayList
	 * @param amountOfNumbersToReturn Integer of how many numbers you want generated
	 * @param minNumber Integer of the minimum number to start with
	 * @param maxNumber Integer of the maximum number to start with
	 * @return Return an String ArrayList
	 * @throws Exception
	 */
	public ArrayList<String> randomNumberBaseTwoString(int amountOfNumbersToReturn,int minNumber, int maxNumber) throws Exception{
		

		url = "http://www.random.org/integers/?num="+Integer.toString(amountOfNumbersToReturn)+"&min="+Integer.toString(minNumber)+"&max="+Integer.toString(maxNumber)+"&col=1&base=2&format=plain&rnd=new";
		return this.httpGet(url);
		
	}
	/**
	 * randomNumberBaseEightString() return a String ArrayList
	 * @param amountOfNumbersToReturn Integer of how many numbers you want generated
	 * @param minNumber Integer of the minimum number to start with
	 * @param maxNumber Integer of the maximum number to start with
	 * @return Return an String ArrayList
	 * @throws Exception
	 */
	public ArrayList<String> randomNumberBaseEightString(int amountOfNumbersToReturn,int minNumber, int maxNumber) throws Exception{
		
		url = "http://www.random.org/integers/?num="+Integer.toString(amountOfNumbersToReturn)+"&min="+Integer.toString(minNumber)+"&max="+Integer.toString(maxNumber)+"&col=1&base=8&format=plain&rnd=new";
		
		return this.httpGet(url);
		
	}
	/**
	 * randomNumberBaseSixteenString() return a String ArrayList
	 * @param amountOfNumbersToReturn Integer of how many numbers you want generated
	 * @param minNumber Integer of the minimum number to start with
	 * @param maxNumber Integer of the maximum number to start with
	 * @return Return an String ArrayList
	 * @throws Exception
	 */
	public ArrayList<String> randomNumberBaseSixteenString(int amountOfNumbersToReturn,int minNumber, int maxNumber) throws Exception{
		
		url = "http://www.random.org/integers/?num="+Integer.toString(amountOfNumbersToReturn)+"&min="+Integer.toString(minNumber)+"&max="+Integer.toString(maxNumber)+"&col=1&base=16&format=plain&rnd=new";
		
		return this.httpGet(url);
		
	}

}
