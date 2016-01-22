package de.unidue.ltl.ibes.reader;

/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universit??t Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.NestableRuntimeException;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

@TypeCapability(outputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData", })
public class LinewiseTextReader extends JCasResourceCollectionReader_ImplBase {

	public static final String PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
	@ConfigurationParameter(name = PARAM_SOURCE_ENCODING, mandatory = true, defaultValue = "UTF-8")
	protected String encoding;

	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false)
	private String language;

	public static final String PARAM_ANNOTATE_SENTENCES = "PARAM_ANNOTATE_LINE_AS_SENTENCE";
	@ConfigurationParameter(name = PARAM_ANNOTATE_SENTENCES, mandatory = false, defaultValue = "true")
	private boolean setSentence;

	public static final String PARAM_UNESCAPE_HTML = "PARAM_UNESCAPE_HTML";
	@ConfigurationParameter(name = PARAM_UNESCAPE_HTML, mandatory = false, defaultValue = "true")
	private boolean unescapeHtml;

	public static final String PARAM_UNESCAPE_JAVA = "PARAM_UNESCAPE_JAVA";
	@ConfigurationParameter(name = PARAM_UNESCAPE_JAVA, mandatory = false, defaultValue = "true")
	private boolean unescapeJava;

	public static final String ENCODING_AUTO = "auto";

	private BufferedReader br;

	private List<BufferedReader> bfs = new ArrayList<BufferedReader>();
	private int currentReader = 0;

	private int instanceId = 1;

	private String nextLine = null;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		try {
			for (Resource r : getResources()) {
				String name = r.getResource().getFile().getName();
				InputStreamReader is = null;
				if (name.endsWith(".gz")) {
					is = new InputStreamReader(new GZIPInputStream(
							r.getInputStream()), encoding);
				} else {
					is = new InputStreamReader(r.getInputStream(), encoding);
				}
				br = new BufferedReader(is);
				bfs.add(br);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void getNext(JCas aJCas) throws IOException, CollectionException {

		DocumentMetaData md = new DocumentMetaData(aJCas);
		md.setDocumentTitle("title");
		md.setDocumentId("" + (instanceId++));
		md.setLanguage(language);
		md.addToIndexes();

		String documentText = nextLine;

		documentText = checkUnescapeHtml(documentText);
		documentText = checkUnescapeJava(documentText);

		aJCas.setDocumentText(documentText);

		checkSetSentence(aJCas);

	}

	private String checkUnescapeJava(String documentText) {
		String backup = documentText;
		if (unescapeJava) {
			try {
				documentText = StringEscapeUtils.unescapeJava(documentText);
			} catch (NestableRuntimeException e) {
				documentText = backup;
			}
		}
		return documentText;
	}

	private String checkUnescapeHtml(String documentText) {
		if (unescapeHtml) {
			documentText = StringEscapeUtils.unescapeHtml(documentText);
		}
		return documentText;
	}

	private void checkSetSentence(JCas aJCas) {
		if (setSentence) {
			Sentence sentence = new Sentence(aJCas, 0, aJCas.getDocumentText()
					.length());
			sentence.addToIndexes();
		}
	}

	public boolean hasNext() throws IOException, CollectionException {
		BufferedReader br = getBufferedReader();

		if ((nextLine = br.readLine()) != null) {

			String[] split = nextLine.split("\t");

			// in case we get some broken lines, skip through unti we have
			// something valid again
			while (split.length != 2) {
				nextLine = br.readLine();
				
				if(nextLine==null){
					return closeReaderOpenNext();
				}
				
				split = nextLine.split("\t");
			}

			nextLine = split[1];

			return true;
		}
		return closeReaderOpenNext();

	}

	private boolean closeReaderOpenNext() throws CollectionException,
			IOException {
		try {
			bfs.get(currentReader).close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (currentReader + 1 < bfs.size()) {
			currentReader++;
			return hasNext();
		}
		return false;
	}

	private BufferedReader getBufferedReader() {
		return bfs.get(currentReader);
	}

	public Progress[] getProgress() {
		return null;
	}
}
