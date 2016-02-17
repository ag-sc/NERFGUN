/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import de.citec.sc.query.Search;

/**
 *
 * @author sherzod
 */
public class TestSearch {
    public static void main(String[] args) {
        Search indexSearch  = new Search(false, "dbpediaIndexAll");
        
        System.out.println(indexSearch.getAllResources("Obama", 10));
    }
}
