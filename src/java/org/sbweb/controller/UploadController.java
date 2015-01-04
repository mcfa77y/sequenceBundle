/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sbweb.controller;

import org.sbweb.domain.Message;
import org.sbweb.domain.UploadedFile;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.MultipartConfig;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.sbweb.response.StatusResponse;

/**
 *
 * @author joelau
 */
@Controller
@MultipartConfig
@RequestMapping(value = "/upload")
public class UploadController {

    private static Logger logger = Logger.getLogger("controller");

    @RequestMapping(method = RequestMethod.GET)
    public String form() {
        return "form";
    }

    @RequestMapping(value = "/message", method = RequestMethod.POST)
    public @ResponseBody
    StatusResponse message(@RequestBody Message message) {
// Do custom steps here
// i.e. Persist the message to the database
        logger.debug("Service processing...done");
        return new StatusResponse(true, "Message received");
    }

    @RequestMapping(value = "/file", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody
    List<UploadedFile> upload(
            @RequestParam("file") MultipartFile file) {
// Do custom steps here
// i.e. Save the file to a temporary location or database
        logger.debug("Writing file to disk...");
        List<UploadedFile> uploadedFiles = new ArrayList<>();
        UploadedFile u = new UploadedFile(file.getOriginalFilename(),
                Long.valueOf(file.getSize()).intValue(),
                "http://localhost:8080/spring-fileupload-tutorial/resources/" + file.getOriginalFilename());

        uploadedFiles.add(u);
        return uploadedFiles;
    }
}
