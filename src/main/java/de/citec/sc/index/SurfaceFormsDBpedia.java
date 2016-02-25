package de.citec.sc.index;

import de.citec.sc.index.Indexer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import test.TestSearch;

public class SurfaceFormsDBpedia {

    //docDirectory => dbpedia *.nt files
    //luceneIndex => lucene creates indexes 
    private Set<String> properties = new HashSet<String>();
    private Set<String> redirects = new LinkedHashSet<String>();
    private ConcurrentHashMap<String, Integer> frequencies = new ConcurrentHashMap<String, Integer>(50000000);

    private String propertiesFile = "";

    /**
     * sets the file path for loading property that are indexed
     */
    public SurfaceFormsDBpedia(String f) {
        this.propertiesFile = f;
    }

    public SurfaceFormsDBpedia() {
    }

    public void load(String dbpediaFilesDirectory) {

        try {

            //load files
            File folder = new File(dbpediaFilesDirectory);
            File[] listOfFiles = folder.listFiles();

            if (propertiesFile.equals("")) {
                properties = readFile(new File("src/main/resources/propertyList.txt"));
            } else {
                System.out.println("loading file : " + propertiesFile);
                properties = readFile(new File(propertiesFile));
            }

            //get redirects pages only, not actual resources
            //create redirect index before
            //processor = new DBpediaRedirectQueryProcessor(true);
            long start = System.currentTimeMillis();
            System.out.println("Adding 'dbpediaFiles/redirects_en.nt' to memory for indexing");
            redirects = getRedirects(new File("dbpediaFiles/redirects_en.nt"));
            long end = System.currentTimeMillis() - start;
            System.out.println("DONE " + (end) + " ms.");

            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile() && !listOfFiles[i].isHidden()) {
                    String fileExtension = listOfFiles[i].getName().substring(listOfFiles[i].getName().lastIndexOf(".") + 1);
                    if (fileExtension.equals("nt")) {

                        try {
                            System.out.print(dbpediaFilesDirectory + listOfFiles[i].getName() + " ");
                            long startTime = System.currentTimeMillis();
                            indexData(dbpediaFilesDirectory + listOfFiles[i].getName());
                            long endTime = System.currentTimeMillis();
                            System.out.println((endTime - startTime) / 1000 + " sec.");
                            System.out.println(frequencies.keySet().size());

                        } catch (Exception ex) {
                            System.err.println("Problem loading file: " + listOfFiles[i].getName());
                            ex.printStackTrace();
                        }
                    }
                }
            }

            writeListToFile(frequencies, "dbpediaFiles/dbpediaSurfaceForms.ttl");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void writeListToFile(ConcurrentHashMap<String, Integer> map, String fileName) {
        try {
            File file = new File(fileName);

            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            PrintStream p = new PrintStream(file);

            for (String s : map.keySet()) {
                String label = s.substring(s.indexOf("###") + 3);
                String uri = s.substring(0, s.indexOf("###"));
                String freq = map.get(s).toString();

//                try {
//                    label = URLDecoder.decode(label, "UTF-8");
//                } catch (Exception e) {
//
//                }
//                try {
//                    uri = URLDecoder.decode(uri, "UTF-8");
//                } catch (Exception e) {
//
//                }
                p.println(label + "\t" + uri + "\t" + freq + "\n");
                
            }

            p.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // reads chunks of data from filePath
    public void indexData(String filePath) {

        String redirectPatternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)> <http://dbpedia.org/ontology/wikiPageRedirects> <http://dbpedia.org/resource/(.*?)>";
        String patternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)> <(.*?)> \"(.*?)\"@en .";
        Pattern patternLabel = Pattern.compile(patternString);
        Pattern patternRedirect = Pattern.compile(redirectPatternString);

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String line = item.toString();

                Matcher m = patternLabel.matcher(line);
                while (m.find()) {
                    String uri = m.group(2);
                    String property = m.group(3);
                    String label = m.group(4);

                    if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {
                        if (properties.contains(property)) {

                            //add to index
                            if (label.contains(",")) {
                                label = label.substring(0, label.indexOf(","));
                            }
                            if (label.contains("(") && label.contains(")")) {
                                label = label.substring(0, label.indexOf("(")).trim();
                            }

                            label = label.trim();
                            label = label.toLowerCase();

//                            try {
//                                uri = URLDecoder.decode(uri, "UTF-8");
//
//                            } catch (Exception e) {
//
//                            }
//                            try {
//                                label = URLDecoder.decode(label, "UTF-8");
//
//                            } catch (Exception e) {
//
//                            }

                            if (!redirects.contains(uri)) {
                                frequencies.put(uri + "###" + label, frequencies.getOrDefault(uri + "###" + label, 0) + 1);
                            }

                        }
                    }
                }

                Matcher m2 = patternRedirect.matcher(line);
                while (m2.find()) {
                    String s = m2.group(2);
                    String uri = m2.group(3);
                    s = s.replace("_", " ");

                    String label = "";

                    //replace each big character with space and the same character
                    //indexing  accessible computing is better than AccessibleComputing
                    for (int k = 0; k < s.length(); k++) {
                        char c = s.charAt(k);
                        if (Character.isUpperCase(c)) {

                            if (k - 1 >= 0) {
                                String prev = s.charAt(k - 1) + "";
                                if (prev.equals(" ")) {
                                    label += c + "";
                                } else {
                                    //put space between characters
                                    label += " " + c;
                                }
                            } else {
                                label += c + "";
                            }
                        } else {
                            label += c + "";
                        }
                    }

                    s = label.toLowerCase();

                    //remove after comma e.g. AS,_b
                    if (s.contains(",")) {
                        s = s.substring(0, s.indexOf(","));
                    }

                    //remove parantheses e.g. AS_(song)
                    if (s.contains("(") && s.contains(")")) {
                        s = s.substring(0, s.indexOf("(")).trim();
                    }

                    s = s.trim();

                    //add to index
//                    try {
//                        uri = URLDecoder.decode(uri, "UTF-8");
//
//                    } catch (Exception e) {
//
//                    }
//                    try {
//                        s = URLDecoder.decode(s, "UTF-8");
//
//                    } catch (Exception e) {
//
//                    }

                    frequencies.put(uri + "###" + s, frequencies.getOrDefault(uri + "###" + s, 0) + 1);

                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Set<String> getRedirects(File file) {
        //HashMap<String, Set<String>> content = new HashMap<>();

        Set<String> content = new LinkedHashSet<>();

        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            String patternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)>.*<http://dbpedia.org/resource/(.*?)>";
            Pattern pattern = Pattern.compile(patternString);
            while ((line = br.readLine()) != null) {

                Matcher m = pattern.matcher(line);
                while (m.find()) {
                    String s = m.group(2);
//
//                    try {
//                        s = URLDecoder.decode(s, "UTF-8");
//                    } catch (Exception e) {
//                    }

                    //String o = m.group(3);
                    content.add(s);
                }

//                if (!line.startsWith("#")) {
//
//                    //System.out.println(line);
//                    String[] a = line.split(" ");
//
//                    String s = a[0];
//
//                    String p = a[1];
//
//                    String o = a[2];
//
//                    s = s.replace("<", "");
//                    s = s.replace(">", "");
//                    p = p.replace("<", "");
//                    p = p.replace(">", "");
//                    o = o.replace("<", "");
//                    o = o.replace(">", "");
//
//                    content.add(s);
//
//                }
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error reading the file: " + file.getPath() + "\n" + e.getMessage());
        }

        return content;
    }

    public Set<String> readFile(File file) {
        Set<String> content = null;
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                if (content == null) {
                    content = new LinkedHashSet<>();
                }
                content.add(strLine);
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error reading the file: " + file.getPath() + "\n" + e.getMessage());
        }

        return content;
    }

}
