package org.weasis.core.api.telnet;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class CustomTelnetExecutor {

    private TelnetClient telnet;
    private InputStream in;
    private PrintStream out;

    public CustomTelnetExecutor() {

    }

    public CustomTelnetExecutor(String server, Integer port) throws IOException {
        init(server, port);
    }

    public void init(String server, Integer port) throws IOException {
        telnet = new TelnetClient();
        telnet.connect(server, port);

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

    public void disconnect() throws IOException {
        telnet.disconnect();
    }
}