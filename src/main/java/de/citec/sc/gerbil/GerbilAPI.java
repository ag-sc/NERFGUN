package de.citec.sc.gerbil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import spark.Spark;

public class GerbilAPI {

	private static final String NED_BIRE_URL = "http://localhost:8181/bire";
//        private static final String NED_BIRE_URL = "http://purpur-v11:8181/bire";
	private static Logger log = LogManager.getFormatterLogger();

	public static void main(String[] args) {
		run();
	}

	public static void run() {
		log.info("GERBIL NIF-Document disambiguation service started.");
		//Spark.port(8181);
		Spark.post("/NED", "application/x-turtle", (request, response) -> {
			String nifDocument = request.body();
                        System.out.println(nifDocument);
			log.info("NIF-Document for disambiguation received:\n%s", nifDocument);
			String annotatedJsonDocument = onRequest(nifDocument);
			log.info("Returning disambiguated NIF-Document:\n%s", annotatedJsonDocument);
			response.type("application/x-tutle");
			return annotatedJsonDocument;
//                        return nifDocument;
		});
	}

	public static String onRequest(String nifDocument) {
		TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		Document gerbilDocument;
		try {
			gerbilDocument = parser.getDocumentFromNIFString(nifDocument);
		} catch (Exception e) {
			log.error("Exception while reading request.", e);
			return "";
		}
		String jsonDocument = GerbilUtil.gerbil2json(gerbilDocument);
		log.info("Send request to disambiguation service. as JSON document:\n%s", jsonDocument);
		String annotatedNifDocument = "";
		try {
			String annotatedJsonDocument;
			annotatedJsonDocument = sendHTTPRequest(jsonDocument);
			log.info("Disambiguated document in JSON format received:\n%s", annotatedJsonDocument);
			Document annotatedGerbilDocument = GerbilUtil.json2gerbil(annotatedJsonDocument);

			TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
			annotatedNifDocument = creator.getDocumentAsNIFString(annotatedGerbilDocument);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return annotatedNifDocument;
	}

	public static String sendHTTPRequest(String jsonDocument) throws IOException {
		HttpURLConnection connection = null;
		try {
			// Create connection
			URL url = new URL(NED_BIRE_URL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			// connection.setRequestProperty("Content-Length",
			// Integer.toString(urlParameters.getBytes().length));
			// connection.setRequestProperty("Content-Language", "en-US");

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(jsonDocument);
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			StringBuilder response = new StringBuilder();

			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
