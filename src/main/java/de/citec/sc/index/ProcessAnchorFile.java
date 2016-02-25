/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.index;

import edu.stanford.nlp.util.ArraySet;
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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author sherzod
 */
public class ProcessAnchorFile {

    public static void run(String filePath) {

        try {
            File file = new File("new.ttl");

            // if file doesnt exists, then create it
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedReader wpkg = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
            String line = "";

            PrintStream ps = new PrintStream("new.ttl", "UTF-8");

            while ((line = wpkg.readLine()) != null) {

                String[] data = line.split("\t");

                if (data.length == 3) {
                    String label = data[0];

                    label = StringEscapeUtils.unescapeJava(label);
                    

                    try {
                        label = URLDecoder.decode(label, "UTF-8");
                    } catch (Exception e) {
                    }

                    String uri = data[1].replace("http://dbpedia.org/resource/", "");
                    uri = StringEscapeUtils.unescapeJava(uri);

                    try {
                        uri = URLDecoder.decode(uri, "UTF-8");
                    } catch (Exception e) {
                    }

                    String f = data[2];
                    
                    label = label.toLowerCase();
                    ps.println(label + "\t" + uri + "\t" + f);

                }

            }
            
            

            wpkg.close();
            ps.close();
            
            File oldFile = new File(filePath);
            oldFile.delete();
            oldFile.createNewFile();

            file.renameTo(oldFile);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
