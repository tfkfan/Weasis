package org.weasis.base.networking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.types.AblyException;

public class AblyService {
    private AblyRealtime ablyRealtime;
    private Channel channel;

    private String key;
    private String channelName;

    public AblyService() {

    }

    public AblyService(String key, String channelName) throws AblyException {
        setKey(key);
        setChannelName(channelName);
        init(getKey(), getChannelName(), null);
    }

    public AblyService(String key, String channelName, Channel.MessageListener listener) throws AblyException {
        setKey(key);
        setChannelName(channelName);
        init(getKey(), getChannelName(), listener);
    }


    public void init(String key, String channelName, Channel.MessageListener listener) throws AblyException {
        ablyRealtime = new AblyRealtime(key);
        channel = ablyRealtime.channels.get(channelName);
        subscribe(listener);
    }

    public void subscribe(Channel.MessageListener listener) throws AblyException {
        if (listener != null)
            channel.subscribe(listener);
    }

    public void publish(String userName, String text) throws AblyException {
        channel.publish(userName, text);
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
