/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.helper;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author sherzod
 */
public class DBpediaEndpoint {

    private static ConcurrentHashMap<String, Set<String>> entityToCategories;
    private static ConcurrentHashMap<String, Set<String>> entityToClasses;
    private static ConcurrentHashMap<String, Set<String>> categoryToParentCategories;
    private static HashMap<String, List<String>> queryCache = new HashMap<>();

    public static void init() {
        loadEntityToCategory();

        loadCategoryToCategory();

        loadEntityToClass();

    }

    private static void loadCategoryToCategory() {
        //load entity to category file
        String path = "skos_categories_en.ttl";

        System.out.print("Loading category to categories file ");

        String categoryPatternString = "^(?!(#))<http://dbpedia.org/resource/Category:(.*?)> <http://www.w3.org/2004/02/skos/core#broader> <http://dbpedia.org/resource/Category:(.*?)>";
        Pattern patternCategory = Pattern.compile(categoryPatternString);

        if (categoryToParentCategories == null) {

            categoryToParentCategories = new ConcurrentHashMap<>(1200000);
            try {
                FileInputStream fstream = new FileInputStream(path);
                BufferedReader indexMappingReader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));

                // BufferedReader indexMappingReader = new BufferedReader(new
                // FileReader(new File(keyFiles)));
                String line = "";
                while ((line = indexMappingReader.readLine()) != null) {

                    Matcher m = patternCategory.matcher(line);
                    while (m.find()) {

                        String child = m.group(2);
                        String parent = m.group(3);

                        child = StringEscapeUtils.unescapeJava(child);

                        try {
                            child = URLDecoder.decode(child, "UTF-8");
                        } catch (Exception e) {
                        }

                        parent = StringEscapeUtils.unescapeJava(parent);

                        try {
                            parent = URLDecoder.decode(parent, "UTF-8");
                        } catch (Exception e) {
                        }

                        if (categoryToParentCategories.containsKey(child)) {
                            Set<String> categories = categoryToParentCategories.get(child);
                            categories.add(parent);
                            categoryToParentCategories.put(child, categories);
                        } else {

                            Set<String> categories = new CopyOnWriteArraySet();//Collections.synchronizedSet(new HashSet());
                            categories.add(parent);
                            categoryToParentCategories.put(child, categories);
                        }

                    }

                }
                indexMappingReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(categoryToParentCategories.size() + "  DONE");
        }
    }

    private static void loadEntityToCategory() {
        //load entity to category file
        String path = "article_categories_en.ttl";

        System.out.print("Loading articles to categories file ");

        String categoryPatternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)> <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:(.*?)>";
        Pattern patternCategory = Pattern.compile(categoryPatternString);

        if (entityToCategories == null) {

            entityToCategories = new ConcurrentHashMap<>(5000000);
            try {
                FileInputStream fstream = new FileInputStream(path);
                BufferedReader indexMappingReader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));

                // BufferedReader indexMappingReader = new BufferedReader(new
                // FileReader(new File(keyFiles)));
                String line = "";
                while ((line = indexMappingReader.readLine()) != null) {

                    Matcher m = patternCategory.matcher(line);
                    while (m.find()) {

                        String uri = m.group(2);
                        String category = m.group(3);

                        uri = StringEscapeUtils.unescapeJava(uri);

                        try {
                            uri = URLDecoder.decode(uri, "UTF-8");
                        } catch (Exception e) {
                        }

                        category = StringEscapeUtils.unescapeJava(category);

                        try {
                            category = URLDecoder.decode(category, "UTF-8");
                        } catch (Exception e) {
                        }

                        if (entityToCategories.containsKey(uri)) {
                            Set<String> categories = entityToCategories.get(uri);
                            categories.add(category);
                            entityToCategories.put(uri, categories);
                        } else {
                            Set<String> categories = new CopyOnWriteArraySet();
                            categories.add(category);
                            entityToCategories.put(uri, categories);
                        }

                    }

                }
                indexMappingReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(entityToCategories.size() + "  DONE");
        }
    }

    private static void loadEntityToClass() {
        //load entity to category file
        String path = "instance-types_en.nt";

        System.out.print("Loading entities to classes file ");

        String categoryPatternString = "^(?!(#))<http://dbpedia.org/resource/(.*?)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/(.*?)>";
        Pattern patternCategory = Pattern.compile(categoryPatternString);

        if (entityToClasses == null) {

            entityToClasses = new ConcurrentHashMap<>(4100000);
            try {
                FileInputStream fstream = new FileInputStream(path);
                BufferedReader indexMappingReader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));

                // BufferedReader indexMappingReader = new BufferedReader(new
                // FileReader(new File(keyFiles)));
                String line = "";
                while ((line = indexMappingReader.readLine()) != null) {

                    Matcher m = patternCategory.matcher(line);
                    while (m.find()) {

                        String uri = m.group(2);
                        String classOfURI = m.group(3);

                        uri = StringEscapeUtils.unescapeJava(uri);

                        try {
                            uri = URLDecoder.decode(uri, "UTF-8");
                        } catch (Exception e) {
                        }

                        classOfURI = StringEscapeUtils.unescapeJava(classOfURI);

                        try {
                            classOfURI = URLDecoder.decode(classOfURI, "UTF-8");
                        } catch (Exception e) {
                        }

                        if (entityToClasses.containsKey(uri)) {
                            Set<String> classes = entityToClasses.get(uri);
                            classes.add(classOfURI);
                            entityToClasses.put(uri, classes);
                        } else {
                            Set<String> classes = new CopyOnWriteArraySet();
                            classes.add(classOfURI);
                            entityToClasses.put(uri, classes);
                        }

                    }

                }
                indexMappingReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(entityToClasses.size() + "  DONE");
        }
    }

    public static List<String> runQuery(String query) {

        List<String> results = new ArrayList<>();

        try {
            if (queryCache.containsKey(query)) {
                return queryCache.get(query);
            }
            Query sparqlQuery = QueryFactory.create(query);

            //QueryExecution qq = QueryExecutionFactory.create(query);
            QueryExecution qexec = QueryExecutionFactory.sparqlService("http://purpur-v11:8890/sparql", sparqlQuery);

            // Set the DBpedia specific timeout.
            ((QueryEngineHTTP) qexec).addParam("timeout", "10000");

            // Execute.
            ResultSet rs = qexec.execSelect();

            List<String> vars = rs.getResultVars();

            while (rs.hasNext()) {

                QuerySolution s = rs.next();

                String r = "";
                for (String v : vars) {
                    RDFNode node = s.get(v);

                    r += node.toString() + "\t";
                }

                results.add(r.trim());
            }

            qexec.close();

            queryCache.put(query, results);

        } catch (Exception e) {

        }

        return results;
    }

    public static Set<String> getClasses(String entityLink) {
        Set<String> result = new HashSet<>();

        if (entityToClasses == null) {
            entityToClasses = new ConcurrentHashMap<>();
        }

        //return from cache
        if (entityToClasses.containsKey(entityLink)) {
            Set<String> c = entityToClasses.get(entityLink);
            for (String c1 : c) {
                result.add(c1);
            }
            return result;
        } 
        else {
            String query = "select distinct ?o where {<http://dbpedia.org/resource/" + entityLink + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o . }";

            List<String> resultFromEndpoint = DBpediaEndpoint.runQuery(query);

            for (String c : resultFromEndpoint) {
                if(c.contains("http://dbpedia.org/ontology/")){
                    result.add(c.replace("http://dbpedia.org/ontology/", ""));
                }
            }
        }
        
        return result;
    }

    public static Set<String> getProperties(String entityLink) {
        Set<String> result = new HashSet<>();

        String query = "select distinct ?p where {<http://dbpedia.org/resource/" + entityLink + "> ?p ?o . \n"
                + "\n"
                + "{?p <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#ObjectProperty> .}\n"
                + "UNION {\n"
                + "\n"
                + "?p <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#DatatypeProperty> .\n"
                + "\n"
                + "}\n"
                + " }";

        List<String> r = DBpediaEndpoint.runQuery(query);

        for (String s : r) {
            //to remove properties like http://dbpedia.org/ontology/wikiPageRevisionID etc .
            if (!s.contains("ontology/wiki")) {
                result.add(s);
            }
        }

        return result;
    }

    /**
     * return list of categories belong to the argument String entityLink
     *
     * @param entityLink
     * @return set of categories for the given entityLink
     */
    public static Set<String> getCategories(String entityLink) {
        Set<String> result = new HashSet<>();

        if (entityToCategories == null) {
            entityToCategories = new ConcurrentHashMap<>();
        }

        //return from cache
        if (entityToCategories.containsKey(entityLink)) {
            Set<String> c = entityToCategories.get(entityLink);
            for (String c1 : c) {
                result.add(c1);
            }
            return result;
        } else {
            String query = "select distinct ?c where { \n"
                    + "\n"
                    + "<http://dbpedia.org/resource/" + entityLink + "> <http://purl.org/dc/terms/subject> ?c. \n"
                    + "}";

            List<String> r = DBpediaEndpoint.runQuery(query);

            for (String s : r) {
                if (!result.contains(s)) {
                    s = s.replace("http://dbpedia.org/resource/Category:", "");
                    //add parent category
                    result.add(s);
                }
            }

//            entityToCategories.put(entityLink, result);
        }

        return result;
    }

    /**
     * return list of categories belong to the argument String entityLink
     */
    public static Set<String> getParentCategories(Set<String> childCategories) {
        Set<String> result = new HashSet<>();

        if (categoryToParentCategories == null) {
            categoryToParentCategories = new ConcurrentHashMap<>();
        }

        for (String category : childCategories) {

            if (result.contains(category)) {
                continue;
                //it has already been expanded to it's parent since we add each child category as well, see the end
            }

            //find parent of category
            //look in cache, if there return those
            if (categoryToParentCategories.containsKey(category)) {

                Set<String> parentCats = categoryToParentCategories.get(category);

                CopyOnWriteArraySet c = new CopyOnWriteArraySet();

                for (String s : parentCats) {
                    result.add(s);
                }

            } else {

                String query = "select distinct ?c where { \n"
                        + " <http://dbpedia.org/resource/Category:" + category + "> <http://www.w3.org/2004/02/skos/core#broader> ?c. \n"
                        + "}";

                List<String> r = DBpediaEndpoint.runQuery(query);

                for (String s : r) {
                    if (!result.contains(s)) {
                        s = s.replace("http://dbpedia.org/resource/Category:", "");
                        //add parent category
                        result.add(s);
                    }
                }
                //add to cache
//                categoryToParentCategories.put(category, result);

            }
            //add category itself, if it's not there already
            result.add(category);
        }

        return result;
    }

    public static double normalizedCategorySimilarity(String firstLink, String secondLink) {

        HashMap<String, Integer> lcs = leastCommonSubsummer(firstLink, secondLink);

        int n_0 = 0;
        int n_x = 0;
        int x = 0;
        int min_0 = 0;
        int max_0 = 0;
        int min_x = 0;
        int max_x = 0;
        int maxLevel = 6;

        if (lcs.size() == 7) {
            n_0 = lcs.get("n_0");
            n_x = lcs.get("n_x");
            x = lcs.get("level_x");
            min_0 = lcs.get("min_0");
            max_0 = lcs.get("max_0");
            min_x = lcs.get("min_x");
            max_x = lcs.get("max_x");
        }
        double part1 = (1 - Math.pow((x - 1) / (double) (maxLevel - 1), 2));

        double recall = ((n_0 + n_x) / (double) (min_0 + min_x)) * part1;
        double precision = ((n_0 + n_x) / (double) (max_0 + max_x)) * part1;
        double f1 = (2 * precision * recall) / (precision + recall);
        if (Double.isNaN(f1)) {
            f1 = 0;
        }
        return f1;
    }

    private static HashMap<String, Integer> leastCommonSubsummer(String firstLink, String secondLink) {

        Set<String> firstResult = getCategories(firstLink);
        Set<String> secondResult = getCategories(secondLink);

        int n_0 = commonCategoryCount(firstResult, secondResult);
        int min_0 = Math.min(firstResult.size(), secondResult.size());
        int max_0 = Math.max(firstResult.size(), secondResult.size());

        boolean isCommonCategoryFound = false;
        //if any one doesn't have categories, then continue
        if (firstResult.isEmpty() || secondResult.isEmpty()) {
            isCommonCategoryFound = true;
        }

        int iterationNumber = 0;
        int maxIteration = 6;
        int n_x = 0;
        int min_x = 0;
        int max_x = 0;

        while (!isCommonCategoryFound) {
            //get parent categories, including the initial categories
            firstResult = getParentCategories(firstResult);
            secondResult = getParentCategories(secondResult);

            min_x = Math.min(firstResult.size(), secondResult.size());
            max_x = Math.max(firstResult.size(), secondResult.size());

            //compare two sets if any set contains an element from the other set
            n_x = commonCategoryCount(firstResult, secondResult);
            isCommonCategoryFound = n_x > 0;

            iterationNumber++;

            //if it reaches max iteration , don't continue anymore
            if (iterationNumber == maxIteration) {
                break;
            }
        }

        HashMap<String, Integer> result = new LinkedHashMap<>();
        result.put("n_0", n_0);
        result.put("min_0", min_0);
        result.put("max_0", max_0);
        result.put("n_x", n_x);// number of matches
        result.put("level_x", iterationNumber);
        result.put("max_x", max_x);
        result.put("min_x", min_x);

        return result;
    }

    private static int commonCategoryCount(Set<String> firstList, Set<String> secondList) {

//        List<String> matched = new ArrayList<>();
        int count = 0;

        Set<String> intersection = new HashSet<>(firstList);
        intersection.retainAll(secondList);

//        System.out.println(intersection);
//
//        System.out.println("=============================");
        count = intersection.size();

        return count;
    }

}
