/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.sc.index;

/**
 *
 * @author sherzod
 */
public interface Loader {
    public void load(boolean deleteIndexFiles, String indexDirectory, String filesDirectory);
    public void indexData(String filePath, Indexer indexer);
    
}
