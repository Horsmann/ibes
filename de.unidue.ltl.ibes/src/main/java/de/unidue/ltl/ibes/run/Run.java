package de.unidue.ltl.ibes.run;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import de.unidue.ltl.ibes.analysis.Analyser;
import de.unidue.ltl.ibes.reader.LinewiseTextReader;

public class Run
{
    public static void main(String[] args)
        throws UIMAException, IOException
    {

        String inputFolder = args[0];
        String configFile = args[1];
        String outputFolder = args[2];

        while (true) {

            DateFormat dayFormat = new SimpleDateFormat("dd_MM_yyyy");

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            Date lastTime = calendar.getTime();
            String dayFolder = dayFormat.format(lastTime);

            String currentDay = inputFolder + "/" + dayFolder + "/";

            if (!new File(currentDay).exists()) {
                sleep(900000);
                continue;
            }

            CollectionReader reader = CollectionReaderFactory.createReader(
                    LinewiseTextReader.class, LinewiseTextReader.PARAM_SOURCE_LOCATION, currentDay,
                    LinewiseTextReader.PARAM_PATTERNS, "*.txt", LinewiseTextReader.PARAM_LANGUAGE,
                    "de", LinewiseTextReader.PARAM_ANNOTATE_SENTENCES, false);

            AnalysisEngine analysis = AnalysisEngineFactory.createEngine(Analyser.class,
                    Analyser.PARAM_CONFIGURATION_FILE, configFile, Analyser.PARAM_TARGET_FOLDER,
                    outputFolder);

            SimplePipeline.runPipeline(reader, analysis);

            sleep(900000);
        }

    }

    private static void sleep(long time)
    {
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
