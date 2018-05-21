package com.evgeniiavak.integrationdsl;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
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
import org.springframework.integration.dsl.ftp.Ftp;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import java.io.File;

@SpringBootApplication
public class IntegrationDslApplication {

    private Logger logger = LoggerFactory.getLogger(IntegrationDslApplication.class);

    @Autowired
    private SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory;

    @Autowired
    private SessionFactory<FTPFile> ftpSessionFactory;

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

    @Bean(name = "ftpSessionFactory")
    public SessionFactory<FTPFile> ftpSessionFactory(
            @Value(value = "${ftp.host:localhost}") String host,
            @Value (value = "${ftp.port:21}") int port,
            @Value (value = "${ftp.user:username}") String user,
            @Value (value = "${ftp.password:12345678}") String password
    ) {
        DefaultFtpSessionFactory factory = new DefaultFtpSessionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
        factory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        return new CachingSessionFactory<>(factory);
    }

    @Bean
    public IntegrationFlow fromSftpToFtpFlow(@Value (value = "${sftp.directory:remote-dir}") String sftpDir,
                                             @Value (value = "${ftp.directory:remote-dir}") String ftpDir) {
        return IntegrationFlows
                .from(s -> s.sftp(sftpSessionFactory)
                                .preserveTimestamp(true)
                                .remoteDirectory(sftpDir)
                                .regexFilter(".*\\.txt$")
                                .filter(new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "sftp-message"))
                                .autoCreateLocalDirectory(true)
                                .localDirectory(new File("tmp")),
                        e -> e.id("sftpInboundAdapter")
                                .autoStartup(true)
                                .poller(Pollers.fixedDelay(5000)))
                .log(message -> "\n\nTEST RESULTS (downloaded): \n" + message.getPayload())
                .handle(Ftp.outboundAdapter(ftpSessionFactory, FileExistsMode.REPLACE)
                                .autoCreateDirectory(true)
                                .remoteDirectory(ftpDir))
                .get();
    }

}
