package org.weasis.base.ui.gui;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

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
    }

    public void write(String value) throws IOException {
        out.println(value);
        out.flush();
        Scanner sc = new Scanner(in);
        while(sc.hasNextLine()){
           sc.nextLine();
        }
    }

    public void sendCommand(String command) throws IOException {
        write(command);
    }

    public void connect() throws IOException {
        telnet.connect(getHost(), getPort());
        in = telnet.getInputStream();
        out = new PrintStream(telnet.getOutputStream());
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