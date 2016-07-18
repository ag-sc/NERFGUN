package de.citec.sc.similarity.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.citec.sc.exceptions.EmptyIndexException;

/**
 * This class represents a very lite implementation of a file-based database.
 * The class contains three methods: </b>
 * 
 * 1) createIndexFromFile: this method creates the index of a given data-file.
 * Such a data-file must be in the correct format. In this implementation we use
 * '\n' as data split identifier. That means in each line of the data-file there
 * is exactly one data-point. The returned index contains the byte position and
 * length of each data-point matched to its documentID.</b>
 * 
 * 2) query: this method converts a given documentID (In this case a documentID
 * is equal to a Wikipedia pagename) into its corresponding index. The index is
 * then used to read the exact number of bytes in the data-file starting from
 * the right position. </b>
 * 
 * 3) loadIndicies: this method serves as factory of the indix-file. If the
 * indexFile is not in the memory and there is no file in the specified path,
 * the method will create a index-file for the given data.
 * 
 * NOTE: Each index within the index-file stores the filename of the data-file.
 * If you change the name of the data-file. The index needs to be recomputed.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class FileDB {

	/**
	 * Megabyte
	 */
	private static final int MB = 1000000;

	/**
	 * Buffer size to read a file.
	 */
	private static final int BUFFER_SIZE = 16 * MB;

	/**
	 * The index file that contains each index for each datapoint. This method
	 * must be called before querying the database.
	 */
	public static Map<String, Map<String, Index>> indices;

	/**
	 * The path to the file that contains the datapoints.
	 */
	public static String fileDirectory;

	/**
	 * filename of the data to query.
	 */
	public static String queryPrefix;

	/**
	 * Call this method to load the index-file from the hard-drive to the
	 * memory. After successful loading you can start querying using the
	 * query-method.
	 * 
	 * After the index file was stored there is the option to store the index
	 * file to the hard-drive. The next time this method is called it searches
	 * for existing index-file and loads it into memory instead of computing it
	 * again.
	 * 
	 * @param indexFileName
	 *            the filename of the index-file.
	 * @param dataFileName
	 *            the name of the file that contains the actual data.
	 * @param storeIndexFile
	 *            whether to store the computed index file or not.
	 * @throws IOException
	 */
	public static void loadIndicies(final String indexFileName, final String dataFileName, final boolean storeIndexFile)
			throws IOException {

		/*
		 * Load only if the
		 */
		final String file = new File(dataFileName).getName();

		if (indices == null || indices.isEmpty()) {
			fileDirectory = new File(dataFileName).getParent();
			if ((indices = (Map<String, Map<String, Index>>) restoreData(indexFileName)) == null) {
				indices = new HashMap<>();
				indices.put(file, new HashMap());
				createIndexFromFile(file, fileDirectory);
				writeData(indexFileName, indices);
			}
			queryPrefix = new ArrayList<>(indices.keySet()).get(0);
		}
	}

	/**
	 * Query the index file to get the right datapoint from the data-file.
	 * Before you can call this method the index file needs to be loaded. To do
	 * that call loadIndicies first.
	 * 
	 * @param dataID
	 *            the ID of the datapoint in the data-file.
	 * @return the datapoint from the data-file as string.
	 * @throws IOException
	 * @throws EmptyIndexException
	 */

	public static String query(final String dataID) throws IOException, EmptyIndexException {
		if (indices.isEmpty())
			throw new EmptyIndexException(
					"The query could not be executed because the index file is empty. Call load indicies first and make sure to provide the correct indicies file for the data.");

		/*
		 * The requested datapoint.
		 */
		String datapoint = null;

		/*
		 * The file where to find the datapoints.
		 */
		RandomAccessFile raf;

		/*
		 * The datapoint filename
		 */
		final String fileName;

		/*
		 * The index of the queried datapoint.
		 */
		final Index i = indices.get(queryPrefix).get(dataID);
		if (i != null) {
			fileName = fileDirectory + "/" + queryPrefix;
			raf = new RandomAccessFile(new File(fileName), "r");
			raf.seek(i.bytePosition);
			datapoint = raf.readLine();
			raf.close();
		}

		return datapoint;
	}

	/**
	 * Creates the index-file given the data-file.
	 * 
	 * @param filename
	 *            the name of the data-file.
	 * @throws IOException
	 */
	private static void createIndexFromFile(final String fileName, String fileDirectory) throws IOException {

		final BufferedReader br = new BufferedReader(new FileReader(fileDirectory + "/" + fileName), BUFFER_SIZE);
		String line;
		long bytesOfLine = 0;
		long totalBytes = 0;

		int count = 0;
		while ((line = br.readLine()) != null) {
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			count++;
			if (count % 1000 == 0) {
				System.out.println("Lines: " + count);
			}
			bytesOfLine = line.getBytes("UTF-8").length + System.lineSeparator().getBytes().length;
			final String[] data = line.split("\t", 2);
			if (data.length == 2) {
				indices.get(fileName).put(data[0], new Index(totalBytes, bytesOfLine));
			} else {
				System.err.println("Line could not be read + " + line);
			}
			totalBytes += bytesOfLine;
		}
		br.close();
	}

	/**
	 * Writes the index file from memory to hard-drive. The method is implements
	 * a standard java-serialization.
	 * 
	 * @param filename
	 *            the name where to store the index-file.
	 * @param data
	 *            the actual data to store.
	 */
	private static void writeData(final String filename, final Object data) {
		final long t = System.currentTimeMillis();

		FileOutputStream fileOut;
		try {
			System.out.println("Try to write data to filesystem in " + filename + "...");
			fileOut = new FileOutputStream(filename);
			final ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(data);
			out.close();
			fileOut.close();
			System.out.println("Successfully serialized data in " + (System.currentTimeMillis() - t));
		} catch (final Exception e) {
			System.out.println("Could not serialize data: " + e.getMessage());
		}
	}

	/**
	 * Loads the index file from the hard-drive to the memory. The method
	 * implements the standard java-deserialization.
	 * 
	 * @param filename
	 *            the name of the index-file.
	 * @return the loaded data.
	 */
	private static Object restoreData(final String filename) {
		final long t = System.currentTimeMillis();
		Object data = null;
		FileInputStream fileIn;
		System.out.println("Try to restore data from : \"" + filename + "\" ...");
		try {
			fileIn = new FileInputStream(filename);
			ObjectInputStream in;
			in = new ObjectInputStream(fileIn);
			data = in.readObject();
			in.close();
			fileIn.close();
			System.out.println("Successfully restored in " + (System.currentTimeMillis() - t));
		} catch (final Exception e) {
			System.out.println("Could not restored data: " + e.getMessage());
			return null;
		}
		return data;
	}
}
