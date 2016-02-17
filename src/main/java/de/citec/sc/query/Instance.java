/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.query;

import java.util.Objects;

/**
 *
 * @author sherzod
 */
public class Instance implements Comparable<Instance> {

    private String uri;
    private double freq;
    private String pos;
    private String onProperty;
    private double pageRank;

    public Instance(String uri, double freq) {
        this.uri = uri;
        this.freq = freq;
    }

    @Override
    public int compareTo(Instance o) {
        if (pageRank > o.pageRank) {
            return -1;
        } else if (pageRank < o.pageRank) {
            return 1;
        }

        return 0;
    }

    @Override
    public String toString() {
        return "Instance{" + "uri=" + uri + ", freq=" + freq + ", pos=" + pos + ", onProperty=" + onProperty + ", pageRank=" + pageRank + '}';
    }

   

    public double getFreq() {
        return freq;
    }

    public void setFreq(double freq) {
        this.freq = freq;
    }

    public double getPageRank() {
        return pageRank;
    }

    public void setPageRank(double pageRank) {
        this.pageRank = pageRank;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.uri);
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.freq) ^ (Double.doubleToLongBits(this.freq) >>> 32));
        hash = 37 * hash + Objects.hashCode(this.pos);
        hash = 37 * hash + Objects.hashCode(this.onProperty);
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.pageRank) ^ (Double.doubleToLongBits(this.pageRank) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Instance other = (Instance) obj;
        if (!Objects.equals(this.uri, other.uri)) {
            return false;
        }
        if (Double.doubleToLongBits(this.freq) != Double.doubleToLongBits(other.freq)) {
            return false;
        }
        if (!Objects.equals(this.pos, other.pos)) {
            return false;
        }
        if (!Objects.equals(this.onProperty, other.onProperty)) {
            return false;
        }
        if (Double.doubleToLongBits(this.pageRank) != Double.doubleToLongBits(other.pageRank)) {
            return false;
        }
        return true;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public String getOnProperty() {
        return onProperty;
    }

    public void setOnProperty(String onProperty) {
        this.onProperty = onProperty;
    }
    
    

}
