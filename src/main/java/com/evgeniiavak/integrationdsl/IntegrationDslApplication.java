package com.evgeniiavak.integrationdsl;

import com.jcraft.jsch.ChannelSftp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.sftp.Sftp;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class IntegrationDslApplication {

    private Logger logger = LoggerFactory.getLogger(IntegrationDslApplication.class);

    @Autowired
    SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory;

    public static void main(String[] args) {
        SpringApplication.run(IntegrationDslApplication.class, args);
    }

    @Bean(name = "sftpSessionFactory")
    public SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory(
            @Value(value = "${sftp.host:localhost}") String host,
            @Value (value = "${sftp.port:22}") int port,
            @Value (value = "${sftp.user:username}") String user,
            @Value (value = "${sftp.password:12345678}") String password
    ) {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUser(user);
        factory.setPassword(password);
        factory.setAllowUnknownKeys(true);
        return new CachingSessionFactory<>(factory);
    }

    @Bean
    public IntegrationFlow sftpInboundFlow(@Value (value = "${sftp.directory:remote-dir}") String dir) {
        return IntegrationFlows
                .from(s -> s.sftp(sftpSessionFactory)
                                .preserveTimestamp(true)
                                .remoteDirectory(dir)
                                .regexFilter(".*\\.txt$")
                                .deleteRemoteFiles(true)
                                .autoCreateLocalDirectory(true)
                                .localDirectory(new File("tmp")),
                        e -> e.id("sftpInboundAdapter")
                                .autoStartup(true)
                                .poller(Pollers.fixedDelay(5000)))
                .log(LoggingHandler.Level.INFO)
                .<File,String> transform(new FileToStringTransformer())
                .log(LoggingHandler.Level.INFO)
                .handle(m -> System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Test results: \n" + m.getPayload()))
                .get();
    }

    @Bean
    public IntegrationFlow fileDeletionFlow(@Value (value = "${local.directory:tmp}") String localDir,
                                            @Value (value = "${sftp.directory:remote-dir}") String remoteDir ) {
        return IntegrationFlows
                .from(s -> s.file(new File(localDir))
                                .autoCreateDirectory(true)
                                .regexFilter(".*\\.txt$"),
                        e -> e.id("fileInboundAdapter")
                                .autoStartup(true)
                                .poller(Pollers.fixedDelay(5333)))
                .log(LoggingHandler.Level.INFO)
                .handle(Sftp.outboundAdapter(sftpSessionFactory, FileExistsMode.FAIL)
                                .remoteDirectory(remoteDir)
                                .autoCreateDirectory(true))
                .get();
    }
}
