package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import twitter4j.HashtagEntity;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterObjectFactory;
import twitter4j.json.DataObjectFactory;

public class CrawlerListener implements StatusListener {

	private Date lastTime;
	private String currentDayString;
	private DateFormat df;
	private DateFormat dayFormat;
	
	public CrawlerListener(){
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		this.lastTime=calendar.getTime();
		this.df= new SimpleDateFormat("dd.MM.yyyy_HH:mm:ss");
		this.dayFormat= new SimpleDateFormat("dd_MM_yyyy");
		
		calendar.setTimeInMillis(System.currentTimeMillis());
		this.lastTime=calendar.getTime();
		this.currentDayString= dayFormat.format(lastTime); 
	}
	
	public void onException(Exception ex) {
		System.out.println(ex.getLocalizedMessage());
	}

	public void onStatus(Status status) {
		 String username = status.getUser().getScreenName(); 

         String content = status.getText();
         
         System.out.println("@" +username+"   "+content);
         
         String rawJSON = TwitterObjectFactory.getRawJSON(status);
         String plainText= status.getId()+"\t"+content.replaceAll("\n", " ");
         this.currentDayString=dayFormat.format(status.getCreatedAt());
        
         
         File dayDir = new File("statuses/"+currentDayString);

         // if the directory does not exist, create it
         if (!dayDir.exists()) {
        	 dayDir.mkdirs();
         }
          
         String fileNameJSON = "statuses/"+currentDayString+"/" + getFileName(status.getCreatedAt()) + ".json";
         String fileNamePlain = "statuses/"+currentDayString+"/" + getFileName(status.getCreatedAt()) + ".txt";
         try {
			storeJSON(rawJSON, fileNameJSON);
			storeJSON(plainText, fileNamePlain);
		} catch (IOException e) {
			e.printStackTrace();
		}
         
	}

	private boolean dayFolderexists(String currentDayString2) {
		// TODO Auto-generated method stub
		return false;
	}

	private String getFileName(Date date) {
		long duration = date.getTime() - this.lastTime.getTime();
		
		long maxDuration=15*60*1000;

		if (duration >= maxDuration) {
			this.lastTime=date;
			return df.format(date);
		}
		return df.format(lastTime);
	}

	private void storeJSON(String rawJSON, String fileName) throws IOException {
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
		    out.println(rawJSON);
		}catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
		System.err.println(statusDeletionNotice);
	}

	public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
		System.err.println("track limit "+numberOfLimitedStatuses);
	}

	public void onScrubGeo(long userId, long upToStatusId) {
		
	}

	public void onStallWarning(StallWarning warning) {
		System.err.println("stall warning "+warning.getMessage());
		
	}
	

		

		}

