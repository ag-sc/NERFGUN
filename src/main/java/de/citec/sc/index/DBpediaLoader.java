package de.citec.sc.index;

import de.citec.sc.index.DBpediaLabelIndexer;
import de.citec.sc.index.Indexer;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import test.TestSearch;

public class DBpediaLoader implements Loader {

    //docDirectory => dbpedia *.nt files
    //luceneIndex => lucene creates indexes 
    private Set<String> properties = new HashSet<String>();
    private Set<String> redirects = new LinkedHashSet<String>();
    private HashMap<String, Double> pageRanks = new HashMap<String, Double>();

    private String propertiesFile = "";

    /**
     * sets the file path for loading property that are indexed
     */
    public DBpediaLoader(String f) {
        this.propertiesFile = f;
    }

    public DBpediaLoader() {
    }

    @Override
    public void load(boolean deleteIndexFiles, String indexDirectory, String dbpediaFilesDirectory) {

        try {
            File indexFolder = new File(indexDirectory);
            if (!indexFolder.exists()) {
                indexFolder.mkdir();
                System.out.println(indexDirectory + " directory is created!");
            }
            //delete old indice files        
            if (deleteIndexFiles) {
                File[] listOfIndexFiles = indexFolder.listFiles();

                for (int i = 0; i < listOfIndexFiles.length; i++) {
                    if (listOfIndexFiles[i].isDirectory()) {
                        deleteFolder(listOfIndexFiles[i]);
                    }
                }
            }

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
                            DBpediaLabelIndexer indexer = new DBpediaLabelIndexer(indexDirectory);

                            long startTime = System.currentTimeMillis();
                            indexData(dbpediaFilesDirectory + listOfFiles[i].getName(), indexer);
                            indexer.finilize();
                            long endTime = System.currentTimeMillis();
                            System.out.println((endTime - startTime) / 1000 + " sec.");

                        } catch (Exception ex) {
                            System.err.println("Problem loading file: " + listOfFiles[i].getName());
                        }
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    // reads chunks of data from filePath
    @Override
    public void indexData(String filePath, Indexer indexer) {
        DBpediaLabelIndexer dbpediaIndexer = (DBpediaLabelIndexer) indexer;
        
        String redirectPatternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)> <http://dbpedia.org/ontology/wikiPageRedirects> <http://dbpedia.org/resource/(.*?)>";
        String patternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)> <(.*?)> \"(.*?)\"@en .";
        Pattern pattern = Pattern.compile(patternString);
        
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String line = item.toString();

                Matcher m = pattern.matcher(line);
                while (m.find()) {
                    String uri = m.group(2);
                    String property = m.group(3);
                    String label = m.group(4);

                    if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {
                        try {
                            //counter.incrementAndGet();
                            uri = URLDecoder.decode(uri, "UTF-8");
                        } catch (UnsupportedEncodingException ex) {
                            Logger.getLogger(TestSearch.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        //add to index

                    }

                }

            });


        } catch (IOException e) {
            e.printStackTrace();
        }
//        try {
//
//            DBpediaLabelIndexer dbpediaIndexer = (DBpediaLabelIndexer) indexer;
//            String redirectPatternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)> <http://dbpedia.org/ontology/wikiPageRedirects> <http://dbpedia.org/resource/(.*?)>";
//            String patternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)> <(.*?)> \"(.*?)\"@en .";
//
//            System.out.println("Loading file: " + filePath);
//            RandomAccessFile aFile = new RandomAccessFile(filePath, "r");
//            FileChannel inChannel = aFile.getChannel();
//            ByteBuffer buffer = ByteBuffer.allocate(1024);
//            String line = "";
//            while (inChannel.read(buffer) > 0) {
//                buffer.flip();
//                for (int i = 0; i < buffer.limit(); i++) {
//                    String b = (char) buffer.get() + "";
//                    if (b.equals("\n")) {
//                        if (line.startsWith("#")) {
//                            line = "";
//                        } else {
//
//                            //System.out.println(line);
//                            String s = line.substring(0, line.indexOf(" ")).trim();
//                            line = line.replace(s, "").trim();
//                            String p = line.substring(0, line.indexOf(" ")).trim();
//                            line = line.replace(p, "").trim();
//                            String o = line.trim();
//
//                            s = s.replace("<", "");
//                            s = s.replace(">", "");
//                            p = p.replace("<", "");
//                            p = p.replace(">", "");
//                            o = o.replace("<", "");
//                            o = o.replace(">", "");
//
//                            if (o.lastIndexOf(".") == o.length() - 1) {
//                                o = o.substring(0, o.length() - 1).trim();
//                            }
//                            s = s.trim();
//                            p = p.trim();
//                            o = o.trim();
//
//                            if (!s.equals("") && !p.equals("") && !o.equals("")) {
//                                if (s.contains("http://dbpedia.org/resource/")
//                                        && properties.contains(p) && !s.contains("disambiguation")) {
//                                    // create entity index
//                                    if (o.contains("\"@en")) {
//                                        //remove " and "@en from string
//                                        o = o.substring(1, o.length() - 4);
//                                    }
//
//                                    //remove after comma e.g. AS,_b
//                                    if (o.contains(",")) {
//                                        o = o.substring(0, o.indexOf(","));
//                                    }
//
//                                    //remove parantheses e.g. AS_(song)
//                                    if (o.contains("(") && o.contains(")")) {
//                                        o = o.substring(0, o.indexOf("(")).trim();
//                                    }
//
//                                    o = o.trim();
//                                    o = o.toLowerCase();
//
//                                    //check if Subject is not a redirect page
//                                    //get subject for the given String s
//                                    //empty means this subject is not a redirect page
//                                    if (!redirects.contains(s)) {
//                                        double rank = 0;
//                                        if (pageRanks.get(s) != null) {
//                                            rank = pageRanks.get(s);
//                                        }
//
//                                        s = URLDecoder.decode(s, "UTF-8");
//                                        dbpediaIndexer.addInstance(o, s);
//                                    }
//
//                                } else if (o.contains("http://dbpedia.org/resource/")
//                                        && p.equals("http://dbpedia.org/ontology/wikiPageRedirects") && !o.contains("disambiguation")) {
//                                    // create entity index
//                                    //subject is the redirect page for the object
//                                    //remove  resource/ , underscore, convert to queryable string
//                                    s = s.replace("http://dbpedia.org/resource/", "");
//
//                                    s = s.replace("_", " ");
//
//                                    String label = "";
//
//                                    //replace each big character with space and the same character
//                                    //indexing  accessible computing is better than AccessibleComputing
//                                    for (int k = 0; k < s.length(); k++) {
//                                        char c = s.charAt(k);
//                                        if (Character.isUpperCase(c)) {
//
//                                            if (k - 1 >= 0) {
//                                                String prev = s.charAt(k - 1) + "";
//                                                if (prev.equals(" ")) {
//                                                    label += c + "";
//                                                } else {
//                                                    //put space between characters
//                                                    label += " " + c;
//                                                }
//                                            } else {
//                                                label += c + "";
//                                            }
//                                        } else {
//                                            label += c + "";
//                                        }
//                                    }
//
//                                    s = label.toLowerCase();
//
//                                    //remove after comma e.g. AS,_b
//                                    if (s.contains(",")) {
//                                        s = s.substring(0, s.indexOf(","));
//                                    }
//
//                                    //remove parantheses e.g. AS_(song)
//                                    if (s.contains("(") && s.contains(")")) {
//                                        s = s.substring(0, s.indexOf("(")).trim();
//                                    }
//
//                                    s = s.trim();
//
//                                    double rank = 0;
//                                    if (pageRanks.get(o) != null) {
//                                        rank = pageRanks.get(o);
//                                    }
//
//                                    o = URLDecoder.decode(o, "UTF-8");
//                                    dbpediaIndexer.addInstance(s, o);
//
//                                }
//                            }
//
//                            line = "";
//                        }
//
//                    } else {
//                        line += b;
//
//                    }
//
//                }
//                buffer.clear();
//            }
//            inChannel.close();
//            aFile.close();
//        } catch (Exception e) {
//            System.err.println("Problem with reading from file : " + filePath + " !!!\n" + e.getMessage());
//            e.printStackTrace();
//
//        }

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

                    String o = m.group(3);
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

    public HashMap<String, Double> getPageRanks(File file) {
        //HashMap<String, Set<String>> content = new HashMap<>();

        HashMap<String, Double> ranks = new HashMap<>();

        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;

            while ((line = br.readLine()) != null) {

                if (!line.startsWith("#")) {
                    //System.out.println(line);
                    try {
                        String[] content = line.split("\t");

                        String uri = content[0];
                        uri = uri.substring(1, uri.length() - 1);
                        String value = content[3].replace("^^<http://www.w3.org/2001/XMLSchema#float>] .", "").replace("\"", "");
                        double rank = Double.parseDouble(value);

                        ranks.put(uri, rank);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error reading the file: " + file.getPath() + "\n" + e.getMessage());
        }

        return ranks;
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
