package com.evgeniiavak.integrationdsl;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class ApplicationService {

    public ApplicationService() {
        System.out.println("\n\n\nSERVICE INITIALIZED");
    }

    public File execute(File file) {
        System.out.println("\n\n\nHandling file: " + file.getName());
        return file;
    }


    public String execute(String file, Object... extraParams) {
//        System.out.println("\n\n\nHandling string file: " + file);
        return file;
    }
}
