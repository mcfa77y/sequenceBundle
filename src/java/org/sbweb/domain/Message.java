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
public class Message implements Serializable {

    private static final long serialVersionUID = -4093981756240899937L;
    String owner;
    String description;
    String filename;

    public Message() {
        super();
    }

    public Message(String owner, String description, String filename) {
        super();
        this.owner = owner;
        this.description = description;
        this.filename = filename;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

}
