package de.citec.sc.index;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class DBpediaLabelIndexer implements Indexer {

    private Path instancesIndexPath;
    private Path instancesTokenizedIndexPath;

    private IndexWriter instancesIndexWriter;
    private IndexWriter instancesTokenizedIndexWriter;

    private Document instancesDoc;
    private Document instancesTokenizedIndexDoc;

    public void addInstance(String label, String uri) throws IOException {
        instancesDoc = new Document();
        instancesTokenizedIndexDoc = new Document();

        Field labelField = new StringField("label", label, Field.Store.YES);
        Field tokenized = new TextField("labelTokenized", label, Field.Store.YES);
        Field uriField = new StringField("URI", uri, Field.Store.YES);

        instancesDoc.add(labelField);
        instancesDoc.add(uriField);

        instancesTokenizedIndexDoc.add(tokenized);
        instancesTokenizedIndexDoc.add(uriField);

        instancesIndexWriter.addDocument(instancesDoc);
        instancesTokenizedIndexWriter.addDocument(instancesTokenizedIndexDoc);
    }

    private IndexWriter initIndexWriter(Path path, boolean create) throws IOException {
        Directory dir = FSDirectory.open(path);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        /*
         * if (create) { iwc.setOpenMode(OpenMode.CREATE); } else { // Add new
         * documents to an existing index:
         * iwc.setOpenMode(OpenMode.CREATE_OR_APPEND); }
         */
        IndexWriter writer = new IndexWriter(dir, iwc);
        return writer;
    }

    @Override
    public void initIndex(String folderPath) {
        if (folderPath == null) {
            throw new RuntimeException("The indexes directory path must be specified");
        }

        try {

            instancesIndexPath = Paths.get(folderPath, "resourceIndex");
            instancesIndexWriter = initIndexWriter(instancesIndexPath, true);
            instancesDoc = new Document();

            instancesTokenizedIndexPath = Paths.get(folderPath, "resourceTokenizedIndex");
            instancesTokenizedIndexWriter = initIndexWriter(instancesTokenizedIndexPath, true);
            instancesTokenizedIndexDoc = new Document();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void finilize() {
        try {

            instancesIndexWriter.close();
            instancesTokenizedIndexWriter.close();

        } catch (IOException ex) {
            Logger.getLogger(DBpediaLabelIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public DBpediaLabelIndexer(String filePath) {
        initIndex(filePath);
    }

}
