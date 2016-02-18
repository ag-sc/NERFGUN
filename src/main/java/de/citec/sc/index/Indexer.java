/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.sc.index;

/**
 *
 * @author sherzod
 * 
 * 
 */
public interface Indexer {
    public void initIndex(String folderPath);
    public void finilize();
}
