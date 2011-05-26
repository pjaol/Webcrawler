package com.pjaol.crawler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

public class CrawlerDataExtractor {

	//static String url ="http://www.reuters.com/article/politicsNews/idUSTRE51O58220090226";
	//static String url ="http://www.latimes.com/business/la-fi-leno26-2009feb26,0,6199268.story";
	//static String url ="http://www.latimes.com/news/nationworld/nation/la-na-war-dead-photos27-2009feb27,0,1362500.story";
	static String url ="http://voices.washingtonpost.com/economy-watch/2009/02/buffett_releases_annual_shareh.html?hpid=topnews";
	
    
	HttpClient httpclient = new DefaultHttpClient();
	List<TextPriority> textList = new LinkedList<TextPriority>();
	int nodePosition = 0;
	double avgContentLen;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CrawlerDataExtractor tc = new CrawlerDataExtractor();
		try {
			System.out.println(tc.process(url));
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public String getURL(String url) throws ClientProtocolException, IOException{
		System.out.print("Fetching :"+ url);
		long st = System.currentTimeMillis();
		
		HttpGet httpget = new HttpGet(url);
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 500);
		HttpConnectionParams.setSoTimeout(httpParams, 500);
		
		httpclient = new DefaultHttpClient(httpParams);
		//httpclient.getParams().setParameter(, arg1)
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		
        String responseBody = httpclient.execute(httpget, responseHandler);
        httpclient.getConnectionManager().shutdown(); 
       // System.out.println(responseBody);
        long et = System.currentTimeMillis();
        System.out.println(" Taken: "+ (et -st));
		return responseBody;
	}

	
	public String process(String url) throws ClientProtocolException, IOException{
		
		String content = getURL(url);
		
		Tidy tidy = new Tidy(); // obtain a new Tidy instance
		tidy.setXHTML(false); // set desired config options using tidy setters 
									// (equivalent to command line options)

		tidy.setMakeClean(true);
		tidy.setQuiet(true);
		tidy.setShowWarnings(false);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes());
		Document dom = tidy.parseDOM(bais, null); // run tidy, providing an input and output stream
		NodeList nl = dom.getChildNodes();
		
		recurse(nl, 0);
		avgContentLen = avgContentLen / nodePosition;
		
		Collections.sort(textList);
		int i = 0;
		
		List<LinePriority> linePosition = new LinkedList<LinePriority>();
		
		for(TextPriority tp: textList){
			if (tp.score >0){
				//System.out.println(i+":"+tp.position+":"+tp.score+":"+tp.content);
				LinePriority lp = new LinePriority();
				lp.content = tp.content;
				lp.position = tp.position;
				linePosition.add(lp);
			}
			i++;
		}
		//System.out.println("=======================");
		StringBuffer buff = new StringBuffer();
		Collections.sort(linePosition);
		for(LinePriority lp: linePosition){
			buff.append(lp.content+"\n");
		}
		
		return buff.toString();
	}
	
	public void recurse (NodeList nl, int indent){
		
		int len = nl.getLength();
		for (int i =0; i< len; i++){
			Node n = nl.item(i);
			if (n.getNodeName() != null)
				if (n.getNodeType() == Node.COMMENT_NODE || 	
					n.getNodeName().equalsIgnoreCase("script") ||
					n.getNodeName().equalsIgnoreCase("style"))
				continue;
			
			if (n.hasChildNodes()) {
				recurse(n.getChildNodes(), (indent+1));
			}
			
			
			String nText = n.getNodeValue();
			nText = trim(nText);
			String[] words = nText.split("\\s");
			if (nText.length() >0 && words.length > 3){// at least 3 words
				nodePosition++;
				//System.out.println(nText.length()+":"+n.getNodeName() +": "+n.getNodeType()+"-->"+ n.getNodeValue());
				TextPriority tp = new TextPriority();
				tp.content = nText;
				tp.position = nodePosition;
				tp.tcp = this;
				textList.add(tp);
				avgContentLen+= nText.length();
			}
			
		}
		
	}
	 /* remove leading whitespace */
    public static String ltrim(String source) {
        return source.replaceAll("^\\s+", "");
    }

    /* remove trailing whitespace */
    public static String rtrim(String source) {
        return source.replaceAll("\\s+$", "");
    }

    /* replace multiple whitespaces between words with single blank */
    public static String itrim(String source) {
        return source.replaceAll("\\b\\s{2,}\\b", " ");
    }

    /* remove all superfluous whitespaces in source string */
    public static String trim(String source) {
        return itrim(ltrim(rtrim(source)));
    }

    public static String lrtrim(String source){
        return ltrim(rtrim(source));
    }
    
    
	class TextPriority implements Comparable{
	
		int position;
		String content;
		double score;
		CrawlerDataExtractor tcp;
		
		public int compareTo(Object o) {
			
			//System.out.println(position +":"+ tcp.nodePosition);
			TextPriority b = (TextPriority)o;
			if (b.score == 0)
				b.createScore();
			
			if (score == 0)
				createScore();
			
			if (score == b.score)
				return 0;
			if (score < b.score)
				return -1;
			
			return 1;
		}
		
		public void createScore(){
			score = Math.log(tcp.nodePosition / position) *
			 (content.length() / tcp.avgContentLen );
		}
		
		
	}
	
	class LinePriority implements Comparable{
		int position;
		String content;
		
		public int compareTo(Object o){
			
			LinePriority b = (LinePriority)o;
			
			if (position == b.position)
				return 0;
			if (position < b.position)
				return -1;
			return 1;
		}
	}
}
