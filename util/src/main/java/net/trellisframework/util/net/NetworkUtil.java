package net.trellisframework.util.net;

import net.trellisframework.http.exception.ForbiddenException;
import net.trellisframework.http.exception.GatewayTimeoutException;
import net.trellisframework.core.log.Logger;
import net.trellisframework.core.message.Messages;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;

import java.io.File;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkUtil {

    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            Logger.error("HostException", e.getMessage());
            return StringUtils.EMPTY;
        }
    }

    public static Set<String> getAllIp(String ip, int subnet) {
        ip = ip + "/" + subnet;
        SubnetUtils utils = new SubnetUtils(ip);
        utils.setInclusiveHostCount(true);
        return Set.of(utils.getInfo().getAllAddresses());
    }

    public static NetworkStatus ping(String ip) {
        try {
            InetAddress geek = InetAddress.getByName(ip);
            return geek.isReachable(5000) ? NetworkStatus.CONNECTED : NetworkStatus.FAILED;
        } catch (Exception e) {
            return NetworkStatus.FAILED;
        }
    }

    public static Set<String> getAvailableIp(Set<String> ips) {
        Set<String> response = new HashSet<>();
        ExecutorService es = Executors.newFixedThreadPool(100);
        for (String ip : ips) {
            es.execute(() -> {
                if (NetworkStatus.CONNECTED.equals(NetworkUtil.ping(ip)))
                    response.add(ip);
            });
        }
        es.shutdown();
        while (true) {
            try {
                if (es.awaitTermination(1, TimeUnit.MINUTES)) break;
            } catch (InterruptedException ignored) {

            }
        }
        return response;
    }

    public static void ssh(String host, String username, String password) throws ForbiddenException, GatewayTimeoutException {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.loadKnownHosts();
            ssh.connect(host);
            ssh.authPassword(username, password);
        } catch (Exception e) {
            Logger.error("SshError", host + " : " + e.getMessage());
            if (e instanceof ConnectException)
                throw new GatewayTimeoutException(Messages.SSH_TIMEOUT);
            throw new ForbiddenException(Messages.SSH_AUTHENTICATION_FAILED);
        }
    }

    public static void ssh(String host, String username, File privateKey) throws ForbiddenException, GatewayTimeoutException {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.loadKnownHosts();
            ssh.connect(host);
            ssh.authPublickey(username, privateKey.getAbsolutePath());
        } catch (Exception e) {
            Logger.error("SshError", host + " : " + e.getMessage());
            if (e instanceof ConnectException)
                throw new GatewayTimeoutException(Messages.SSH_TIMEOUT);
            throw new ForbiddenException(Messages.SSH_AUTHENTICATION_FAILED);
        }
    }

    public static void scp(String host, String username, String password, String localPath, String remotePath) throws ForbiddenException, GatewayTimeoutException {
        scp(host, username, password, localPath, remotePath, (String[]) null);
    }

    public static void scp(String host, String username, String password, String localPath, String remotePath, String... commands) throws ForbiddenException, GatewayTimeoutException {
        try(SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.loadKnownHosts();
            ssh.connect(host);
            ssh.authPassword(username, password);
            ssh.useCompression();
            ssh.newSCPFileTransfer().upload(localPath, remotePath);
            if (commands != null) {
                for (String command : commands) {
                    Session session = ssh.startSession();
                    Session.Command cmd = session.exec(command);
                    Logger.info(command, IOUtils.readFully(cmd.getInputStream()).toString());
                    cmd.join(15, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            Logger.error("SshError", host + " : " + e.getMessage());
            if (e instanceof ConnectException)
                throw new GatewayTimeoutException(Messages.SSH_TIMEOUT);
            throw new ForbiddenException(Messages.SSH_AUTHENTICATION_FAILED);
        }
    }

    public static void scp(String host, String username, File privateKey, String localPath, String remotePath) throws ForbiddenException, GatewayTimeoutException {
        scp(host, username, privateKey, localPath, remotePath, (String) null);
    }

    public static void scp(String host, String username, File privateKey, String localPath, String remotePath, String... commands) throws ForbiddenException, GatewayTimeoutException {
        try(SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.loadKnownHosts();
            ssh.connect(host);
            ssh.authPublickey(username, privateKey.getAbsolutePath());
            ssh.useCompression();
            ssh.newSCPFileTransfer().upload(localPath, remotePath);
            if (commands != null) {
                for (String command : commands) {
                    Session session = ssh.startSession();
                    Session.Command cmd = session.exec(command);
                    Logger.info(command, IOUtils.readFully(cmd.getInputStream()).toString());
                    cmd.join(10, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            Logger.error("SshError", host + " : " + e.getMessage());
            if (e instanceof ConnectException)
                throw new GatewayTimeoutException(Messages.SSH_TIMEOUT);
            throw new ForbiddenException(Messages.SSH_AUTHENTICATION_FAILED);
        }
    }

    public static void command(String host, String username, String password, String... commands) throws ForbiddenException, GatewayTimeoutException {
        try(SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.loadKnownHosts();
            ssh.connect(host);
            ssh.authPassword(username, password);
            for (String command : commands) {
                Session session = ssh.startSession();
                Session.Command cmd = session.exec(command);
                Logger.info(command, IOUtils.readFully(cmd.getInputStream()).toString());
                cmd.join(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Logger.error("SshError", host + " : " + e.getMessage());
            if (e instanceof ConnectException)
                throw new GatewayTimeoutException(Messages.SSH_TIMEOUT);
            throw new ForbiddenException(Messages.SSH_AUTHENTICATION_FAILED);
        }
    }

    public static void command(String host, String username, File privateKey, String... commands) throws ForbiddenException, GatewayTimeoutException {
        try(SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.loadKnownHosts();
            ssh.connect(host);
            ssh.authPublickey(username, privateKey.getAbsolutePath());
            for (String command : commands) {
                Session session = ssh.startSession();
                Session.Command cmd = session.exec(command);
                Logger.info(command, IOUtils.readFully(cmd.getInputStream()).toString());
                cmd.join(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Logger.error("SshError", host + " : " + e.getMessage());
            if (e instanceof ConnectException)
                throw new GatewayTimeoutException(Messages.SSH_TIMEOUT);
            throw new ForbiddenException(Messages.SSH_AUTHENTICATION_FAILED);
        }
    }
}
