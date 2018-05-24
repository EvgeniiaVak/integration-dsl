package com.evgeniiavak.integrationdsl;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.sftp.Sftp;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class IntegrationDslApplication {

    @Autowired
    private SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory;

    @Autowired
    private SessionFactory<FTPFile> ftpSessionFactory;

    @Autowired
    private ApplicationService applicationService;

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

//    @Bean
    public IntegrationFlow sftpFlow(@Value("${sftp.path-in:/tmp/path/in/}") String from,
                                    @Value("${sftp.path-out:/tmp/path/out/}") String to,
                                    @Value("${tmp-path:/tmp/}") String tmpPath) {

        return IntegrationFlows
                .from(s -> s.sftp(sftpSessionFactory)
                                .remoteDirectory(from)
                                .regexFilter(".*\\.xml$")
                                .localDirectory(new File(tmpPath))
                                .localFilter(new RegexPatternFileListFilter(".*\\.xml$")),
                        e -> e
                                .autoStartup(true)
                                .poller(Pollers
                                        .fixedDelay(5000)))
                .log(message -> "\n\nTEST RESULTS (downloaded): \n" + message.getPayload())

                //reading file
                .transform(Transformers.fileToString())

                //some business logic
                .<String>handle((p, h) -> applicationService.execute(p, from))

                //preparing for mv command
                .enrichHeaders(h -> h.headerExpressions(m -> m
                        .put(FileHeaders.RENAME_TO, "'" + to + "' + headers.file_name")
                        .put(FileHeaders.REMOTE_FILE, "'" + from + "' + headers.file_name")
                        .put(FileHeaders.REMOTE_DIRECTORY, "'" + from + "'")
                ))
                //the mv command
                .handle(Sftp.outboundGateway(sftpSessionFactory, AbstractRemoteFileOutboundGateway.Command.MV,
                        "'" + from + "' + headers.file_name")
                        .fileExistsMode(FileExistsMode.REPLACE))
                //delete file from /tmp
                .transform("headers.file_originalFile")
                .transform(File::delete).channel("nullChannel")
                .get();
    }

    @Bean
    public IntegrationFlow generateFileFlow(@Value("${sftp.path-out:/tmp/path/out/}") String to,
                                            @Value("${generate-file-cron}") String cron) {


        return IntegrationFlows
                .from(applicationService, "generateFileContent",
                        p -> p.poller(Pollers.cron(cron)))
                .log(LoggingHandler.Level.INFO)
                .handleWithAdapter(a -> a.sftp(sftpSessionFactory, FileExistsMode.REPLACE)
                        .autoCreateDirectory(true)
                        .fileNameGenerator(f->LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + ".txt")
                        .remoteDirectory(to))
                .get();
    }
}
