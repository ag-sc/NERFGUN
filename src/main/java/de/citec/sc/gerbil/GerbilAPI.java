package de.citec.sc.gerbil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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

	private static final String NED_BIRE_URL = "http://localhost:8080/ned/json";
	private static Logger log = LogManager.getFormatterLogger();

	public void run() {
		Spark.post("/ned/gerbil", "application/x-turtle", (request, response) -> {
			String nifDocument = request.body();
			String annotatedJsonDocument = onRequest(nifDocument);

			response.type("application/x-tutle");
			return annotatedJsonDocument;
		});
	}

	public String onRequest(String nifDocument) {
		TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		Document gerbilDocument;
		try {
			gerbilDocument = parser.getDocumentFromNIFString(nifDocument);
		} catch (Exception e) {
			log.error("Exception while reading request.", e);
			return "";
		}
		String annotatedJsonDocument = sendHTTPRequest(GerbilUtil.gerbil2json(gerbilDocument));

		Document annotatedGerbilDocument = GerbilUtil.json2gerbil(annotatedJsonDocument);

		TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
		String annotatedNifDocument = creator.getDocumentAsNIFString(annotatedGerbilDocument);
		return annotatedNifDocument;
	}

	public String sendHTTPRequest(String jsonDocument) {
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
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
