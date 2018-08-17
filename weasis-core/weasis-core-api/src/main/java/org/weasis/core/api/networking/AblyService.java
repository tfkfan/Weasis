package org.weasis.core.api.networking;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.types.AblyException;

public class AblyService {
    private final static String API_KEY = "Rzgycw.3FHJ-Q:75oKq_HwuSSeV_Rn";

    protected AblyRealtime ablyRealtime;

    protected String channelName;

    public AblyService() {

    }

    public void init() throws AblyException {
        ablyRealtime = new AblyRealtime(API_KEY);
    }

    public void publish(String name, String text) throws AblyException {
        getAblyChannel().publish(name, text);
    }

    public void setChannelName(String currentChannel) {
        this.channelName = currentChannel;
    }

    public Channel getAblyChannel() {
        return ablyRealtime.channels.get(channelName);
    }

}
