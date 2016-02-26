package de.citec.sc.index;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;

public class AnchorTextLoader implements Loader {

    //docDirectory => dbpedia *.nt files
    //luceneIndex => lucene creates indexes 
    private HashMap<String, String> redirects = new LinkedHashMap<String, String>();

    @Override
    public void load(boolean deleteIndexFiles, String indexDirectory, String anchorFilesDirectory) {

        //delete old indice files
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

            //processor = new DBpediaRedirectQueryProcessor(true);
            long start = System.currentTimeMillis();
            System.out.println("Adding 'dbpediaFiles/redirects_en.nt' to memory for indexing");
            if (redirects.isEmpty()) {
                redirects = getRedirects(new File("dbpediaFiles/redirects_en.nt"));
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("DONE " + (end) + " ms.");

            //load files
            File folder = new File(anchorFilesDirectory);
            File[] listOfFiles = folder.listFiles();

            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile() && !listOfFiles[i].isHidden()) {
                    String fileExtension = listOfFiles[i].getName().substring(listOfFiles[i].getName().lastIndexOf(".") + 1);
                    if (fileExtension.equals("ttl")) {

                        try {
                            AnchorTextIndexer indexer = new AnchorTextIndexer(indexDirectory);

                            long startTime = System.currentTimeMillis();

                            indexData(anchorFilesDirectory + listOfFiles[i].getName(), indexer);

                            indexer.finilize();

                            long endTime = System.currentTimeMillis();
                            System.out.println((endTime - startTime) / 1000 + " sec.");

                        } catch (Exception e) {
                            System.err.println("Problem loading : " + listOfFiles[i].getName());
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void deleteFolder(File folder) {
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
        try {

            AnchorTextIndexer anchorIndexer = (AnchorTextIndexer) indexer;

            BufferedReader wpgk = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
            String line = "";

            while ((line = wpgk.readLine()) != null) {

                String[] data = line.split("\t");

                if (data.length == 3) {
                    
                    try {
                        String label = data[0];

                        String uri = data[1];

                        int freq = Integer.parseInt(data[2]);

                        //replace redirect with its main page
                        if (redirects.containsKey(uri)) {
                            uri = redirects.get(uri);
                        }

                        if (!uri.contains("Category:") && !uri.contains("(disambiguation)") && !uri.contains("File:")) {
                            label = label.toLowerCase();
                            
                            anchorIndexer.addEntity(label, uri, freq);
                        }
                    } catch (Exception e) {
                    }

                }

            }
        } catch (Exception e) {

        }
    }

    public HashMap<String, String> getRedirects(File file) {
        HashMap<String, String> content = new HashMap<>();

        //Set<String> content = new LinkedHashSet<>();
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
                    
                    s = StringEscapeUtils.unescapeJava(s);
                    o = StringEscapeUtils.unescapeJava(o);

                    try {
                        o = URLDecoder.decode(o, "UTF-8");
                    } catch (Exception e) {
                    }
                    try {
                        s = URLDecoder.decode(s, "UTF-8");
                    } catch (Exception e) {
                    }
                    //content.add(s);
                    content.put(s, o);
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

}
