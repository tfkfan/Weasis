package org.weasis.core.api.telnet;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class CustomTelnetExecutor {

    private TelnetClient telnet;
    private InputStream in;
    private PrintStream out;

    private String host;
    private Integer port;

    public CustomTelnetExecutor() {

    }

    public CustomTelnetExecutor(String server, Integer port) throws IOException {
        init(server, port);
    }

    public void init(String server, Integer port) throws IOException {
        setHost(host);
        setPort(port);

        telnet = new TelnetClient();
        in = telnet.getInputStream();
        out = new PrintStream(telnet.getOutputStream());
    }

    public void write(String value) {
        out.println(value);
        out.flush();
    }

    public void sendCommand(String command) {
        write(command);
    }

    public void connect() throws IOException {
        telnet.connect(getHost(), getPort());
    }

    public void disconnect() throws IOException {
        telnet.disconnect();
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}