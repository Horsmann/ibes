package main;

import twitter4j.FilterQuery;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterCrawler {
	public static void main(String[] args) {

		ConfigurationBuilder cb = new ConfigurationBuilder();

		cb.setDebugEnabled(true).setOAuthConsumerKey("XXX")
				.setOAuthConsumerSecret("XXX")
				.setOAuthAccessToken("XXX")
				.setOAuthAccessTokenSecret("XXX");
		
		cb.setJSONStoreEnabled(true);
		
		TwitterStream twitterStream= new TwitterStreamFactory(cb.build()).getInstance();
		
		//new query
		FilterQuery fq = new FilterQuery();
		Config config= new Config();

		String[] keywordsArray = config.getHashtags();
//		String[] keywordsArray = {"#bornheim"};
		
		CrawlerListener listener = new CrawlerListener();
		twitterStream.addListener(listener);
	
		fq.track(keywordsArray);
		twitterStream.filter(fq);
	}
}
