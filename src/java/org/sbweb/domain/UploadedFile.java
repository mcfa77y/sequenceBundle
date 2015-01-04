/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sbweb.domain;

import java.io.Serializable;

/**
 *
 * @author joelau
 */
public class UploadedFile implements Serializable {

    private static final long serialVersionUID = -38331060124340967L;
    String name;
    Integer size;
    String url;
    String thumbnail_url;
    String delete_url;
    String delete_type;

    public UploadedFile() {
        super();
    }

    public UploadedFile(String name, Integer size, String url) {
        super();
        this.name = name;
        this.size = size;
        this.url = url;
    }

    public UploadedFile(String name, Integer size, String url,
            String thumbnail_url, String delete_url, String delete_type) {
        super();
        this.name = name;
        this.size = size;
        this.url = url;
        this.thumbnail_url = thumbnail_url;
        this.delete_url = delete_url;
        this.delete_type = delete_type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the size
     */
    public Integer getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the thumbnail_url
     */
    public String getThumbnail_url() {
        return thumbnail_url;
    }

    /**
     * @param thumbnail_url the thumbnail_url to set
     */
    public void setThumbnail_url(String thumbnail_url) {
        this.thumbnail_url = thumbnail_url;
    }

    /**
     * @return the delete_url
     */
    public String getDelete_url() {
        return delete_url;
    }

    /**
     * @param delete_url the delete_url to set
     */
    public void setDelete_url(String delete_url) {
        this.delete_url = delete_url;
    }

    /**
     * @return the delete_type
     */
    public String getDelete_type() {
        return delete_type;
    }

    /**
     * @param delete_type the delete_type to set
     */
    public void setDelete_type(String delete_type) {
        this.delete_type = delete_type;
    }

}
