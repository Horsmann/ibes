package de.unidue.ltl.ibes.analysis;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Analyser
    extends JCasAnnotator_ImplBase
{

    public static final String PARAM_CONFIGURATION_FILE = "PARAM_CONFIGURATION_FILE";
    @ConfigurationParameter(name = PARAM_CONFIGURATION_FILE, mandatory = true)
    private File configFile;

    public static final String PARAM_TARGET_FOLDER = "PARAM_TARGET_FILE";
    @ConfigurationParameter(name = PARAM_TARGET_FOLDER, mandatory = true)
    private File targetFolder;

    Map<String, Set<String>> participant2keywords = new HashMap<String, Set<String>>();
    Map<String, Double> participant2score = new HashMap<String, Double>();

    final String LINEBREAK = "\r\n";
    
    @Override
    public void initialize(final UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        loadConfiguration();
        initScoreMap();
    }

    private void initScoreMap()
    {
        for (String participant : participant2keywords.keySet()) {
            participant2score.put(participant, new Double(0));
        }
    }

    private void loadConfiguration()
        throws ResourceInitializationException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("candidate");

            for (int j = 0; j < nList.getLength(); j++) {
                Node nNode = nList.item(j);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    Node item = eElement.getElementsByTagName("name").item(0);
                    // System.out.println(item.getTextContent());
                    // System.out.println("----");;

                    String candidateName = item.getTextContent();
                    Set<String> keywords = new HashSet<String>();

                    NodeList keyList = eElement.getElementsByTagName("key");
                    for (int i = 0; i < keyList.getLength(); i++) {
                        Node key = keyList.item(i);
                        if (key.getNodeType() == Node.ELEMENT_NODE) {
                            // System.out.println(key.getTextContent());
                            keywords.add(key.getTextContent().toLowerCase());
                        }
                    }

                    participant2keywords.put(candidateName, keywords);
                }
            }

        }
        catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        String documentText = aJCas.getDocumentText().toLowerCase();

        Set<String> keySet = participant2keywords.keySet();
        for (String participant : keySet) {
            Set<String> keywords = participant2keywords.get(participant);
            for (String key : keywords) {

                boolean plain = documentText.contains(key);
                boolean hash = documentText.contains("#" + key);
                boolean atmention = documentText.contains("@" + key);

                if (plain || hash || atmention) {
                    scoreParticipant(participant);
                    break;
                }
            }
        }
    }

    private void scoreParticipant(String participant)
    {
        Double score = participant2score.get(participant);
        score++;
        participant2score.put(participant, score);
    }

    public void collectionProcessComplete()
        throws AnalysisEngineProcessException
    {
        Double totalScore = getTotal();

        sysoutToCmd(totalScore);
        writeResultfile(totalScore);

    }

    private void writeResultfile(Double totalScore)
        throws AnalysisEngineProcessException
    {
        StringBuilder sb = new StringBuilder();
        for (String participant : participant2score.keySet()) {
            Double count = participant2score.get(participant);
            sb.append(participant + "\t" + getScore(count, totalScore));
            sb.append(LINEBREAK);
        }

        java.util.Date date = new java.util.Date();
        String timestamp = new Timestamp(date.getTime()).toString().replaceAll(":", "_")
                .replaceAll(" ", "_").replaceAll("-", "_").replaceAll("\\.", "_");

        try {
            FileUtils.writeStringToFile(new File(targetFolder, "ibes" + timestamp+".txt"),
                    sb.toString(), "utf-8");
            FileUtils.writeStringToFile(new File(targetFolder, "ibes_latest.txt"),
                    sb.toString(), "utf-8");
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private String getScore(Double count, Double totalScore)
    {
        return String.format("%.1f", count / totalScore * 100);
    }

    private void sysoutToCmd(Double totalScore)
    {
        for (String participant : participant2score.keySet()) {
            Double count = participant2score.get(participant);
            System.out.println(participant + "\t" + getScore(count, totalScore));
        }
    }

    private Double getTotal()
    {
        Double totalScore = 0.0;
        for (String participant : participant2score.keySet()) {
            totalScore += participant2score.get(participant);
        }

        return totalScore;
    }

}
