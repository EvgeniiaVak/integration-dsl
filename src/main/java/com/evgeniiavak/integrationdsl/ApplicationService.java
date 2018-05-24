package com.evgeniiavak.integrationdsl;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApplicationService {

    public ApplicationService() {
        System.out.println("\n\n\nSERVICE INITIALIZED");
    }

    public File execute(File file) {
        System.out.println("\n\n\nHandling file: " + file.getName());
        return file;
    }

    public String generateFileContent() {
        return "this file content: " + Math.random();
    }

    public List<String> generateMultipleFilesContent() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            result.add("this line content: " + Math.random());
        }
        return result;
    }


    public String execute(String file, Object... extraParams) {
        System.out.println("\n\n\nHandling string file: " + file);
        return file;
    }
}
