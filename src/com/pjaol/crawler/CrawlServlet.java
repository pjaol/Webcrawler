package com.pjaol.crawler;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CrawlServlet extends HttpServlet{

	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
	
		PrintWriter out = resp.getWriter();
	
		String url = req.getParameter("url");
		out.write("<html><head><title>Crawl Data Extractor</title></head>");
		out.write("<body><form><input type='text' name='url' value='http://'><input type='submit'></form>");
		
		if (url != null){
			CrawlerDataExtractor cde = new CrawlerDataExtractor();
			String content = cde.process(url);
			out.write("<pre>"+content+"</pre>");
		}
		
		out.write("</body></html>");
		
	}
}
