package com.evgeniiavak.integrationdsl;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

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
        System.out.println("\n\n\nHandling string file: " + Thread.currentThread());

        return file;
    }

    @Async
    public Future executeAsync(String file) {
        System.out.println("\n\n\nThis is async method, file: " + Thread.currentThread());
        try {
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("\n\n\nThis is async method, file: " + Thread.currentThread());
        System.out.println();
        return new SettableListenableFuture();
    }
}
